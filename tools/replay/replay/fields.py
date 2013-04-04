import collections

UNDEFINED_TIMESTAMP = -1


class FieldAccessEnvironment:
    def __init__(self, scope, memoryModel):
        self.scope = scope
        self._memoryModel = memoryModel
        self._accessed = {}             # { objectId : { field : lastWriteTimestamp } }
        self._overWritten = {}          # { objectId : { field : timestampBeforeFirstOverwrite } }
        self._inherited = {}            # { objectId : set([ field ]) }
        self._monitors = {}             # { objectId : set([ field ]) }
        self._enterTimestamp = self._memoryModel.currentTimestamp()
        
    def get(self, thread, objectId, field):
        # Sharing detection is a problem of the memory model and does not depend on the scope
        # Interleaving detection depends on the scope
        
        fieldAccess = self._accessed.setdefault(objectId, {})
        # Always issue a read to keep the sharing information up to date.
        timestamp = self._memoryModel.read(thread, objectId, field)
        
        if field not in fieldAccess:
            # Only store the timestamp of the first read access to make
            # sure we detect all changes from other threads.
            fieldAccess[field] = timestamp
        
        fieldInheritance = self._inherited.get(objectId, {})
        if field in fieldInheritance:
            fieldInheritance.remove(field)
    
    def put(self, thread, objectId, field):
        fieldAccess = self._accessed.setdefault(objectId, {})
        oldTimestamp = self._memoryModel.read(thread, objectId, field)
        newTimestamp = self._memoryModel.write(thread, objectId, field)

        if field not in fieldAccess:
            if oldTimestamp > self._enterTimestamp:
                # Another thread wrote the field since the scope was entered
                fieldAccess[field] = self._enterTimestamp
            else:
                if field not in self._overWritten.setdefault(objectId, {}):
                    # Remember the original timestamp that was overwritten to
                    # allow the enclosing method to detect concurrent writes
                    # before this method was called.  (See __iadd__().)
                    # Only do this once; otherwise, multiple writes destroy
                    # the original timestamp.
                    self._overWritten[objectId][field] = oldTimestamp
                fieldAccess[field] = newTimestamp
        else:
            if fieldAccess[field] < oldTimestamp:
                # Another thread wrote the field
                pass
            else:
                if field not in self._overWritten.setdefault(objectId, {}):
                    self._overWritten[objectId][field] = oldTimestamp
                fieldAccess[field] = newTimestamp

        fieldInheritance = self._inherited.get(objectId, {})
        if field in fieldInheritance:
            fieldInheritance.remove(field)

    
    def __iadd__(self, other):
        # Goal: import and update timestamps without losing interleaving information 
        
        # _accessed dictionary
        # If (object, field) is not in self's dictionary, copy other's timestamp
        # If (object, field) is in self's dictionary:
        #   If the field was written by some other thread in the meantime:
        #     Do not update the timestamp (so the interleaving will be detected)
        #   Otherwise:
        #     If the field was written by other, use the written timestamp
        #     If the field was not written by other, use self's timestamp
        
        # Carry over and merge the access status of objects
        # NOTE: Monitor flags do _not_ transcend their original environment.
        
        for objectId, otherFields in other._accessed.items():
            otherOverWritten = other._overWritten.get(objectId, {})
            selfFields = self._accessed.setdefault(objectId, {})
            selfInherited = self._inherited.setdefault(objectId, set())
            for field in otherFields:
                if field in selfFields:
                    if field in otherOverWritten and otherOverWritten[field] > selfFields[field]:
                        # Concurrent writes from other threads to fields just before a
                        # writing method was called, for example (A, write), (B, write),
                        # (A, enter), (A, write).
                        pass
                    else:
                        selfFields[field] = otherFields[field]
                else:
                    selfFields[field] = otherFields[field]
                    selfInherited.add(field)
        
        return self
    
    def sharedObjects(self, thread):
        return self._memoryModel.sharedObjects(set(self._accessed.keys()), thread)
    
    def atomicFields(self, objectId):
        return self._memoryModel.unwrittenSince(objectId, self._accessed.get(objectId, {}))
    
    def interleavedFields(self, objectId):
        changedFields = self._memoryModel.writtenSince(objectId, self._accessed.get(objectId, {}))
        # FIXME: Fields read by other threads since the current thread wrote them
        #        are also interleaved.  (This affects atomicFields() as well.)
        return changedFields - self._monitors.get(objectId, set())

    def inheritedFields(self, objectId):
        return self._inherited.get(objectId, set())

    def markAsMonitor(self, objectId, field):
        self._monitors.setdefault(objectId, set()).add(field)



# TODO: Make the owning thread a property of the field access stack and its environments.
class FieldAccessStack:
    def __init__(self, threadAccessRecorder):
        self._threadAccessRecorder = threadAccessRecorder
        self._environments = collections.deque()
        
    def pushScope(self, scope):
        fae = FieldAccessEnvironment(scope, self._threadAccessRecorder)
        self._environments.append(fae)
    
    def popScope(self):
        # Merge the popped environment with previous one to simulate
        # simultaneous counting in across all environments.
        oldEnv = self._environments.pop()
        if len(self._environments) > 0:
            self._environments[-1] += oldEnv

    def get(self, thread, objectId, field):
        self._environments[-1].get(thread, objectId, field)

    def put(self, thread, objectId, field):
        self._environments[-1].put(thread, objectId, field)

    def sharedObjects(self, thread):
        if len(self._environments) == 0:
            return set()
        return self._environments[-1].sharedObjects(thread)

    def atomicFields(self, objectId):
        if len(self._environments) == 0:
            return set()
        return self._environments[-1].atomicFields(objectId)

    def interleavedFields(self, objectId):
        if len(self._environments) == 0:
            return set()
        return self._environments[-1].interleavedFields(objectId)
    
    def inheritedFields(self, objectId):
        if len(self._environments) == 0:
            return set()
        return self._environments[-1].inheritedFields(objectId)

    def markAsMonitor(self, objectId, field):
        if len(self._environments) > 0:
            self._environments[-1].markAsMonitor(objectId, field)

    def topScope(self):
        if len(self._environments) == 0:
            return None
        return self._environments[-1].scope


class ThreadAccessRecorder:
#    """A global "memory model" that records which thread accessed an object's field last."""
    """A global "memory model" that timestamps the values store in the fields of objects."""
    def __init__(self):
        self._objects = {}              # { objectId : { field : timestamp } }
        # TODO: Sharing could also be tracked on the field level.
        self._shared = set()            # set([ objectId ])
        self._lastThread = {}           # { objectId : lastThread }
        self._touchedThreads = {}       # { objectId : set([ thread ]) }
        self._currentTimestamp = 0
    
    # TODO: Re-implement the special handling of array indices.
    def read(self, thread, objectId, field):
        timestamp = self._objects.setdefault(objectId, {}).setdefault(field, UNDEFINED_TIMESTAMP)
        
        touchedThreads = self._touchedThreads.setdefault(objectId, set())
        if thread not in touchedThreads:
            # If this is the first time the thread accesses the object, ignore prior accesses to the field from other threads.
            self._lastThread[objectId] = thread
            touchedThreads.add(thread)
        
        elif thread != self._lastThread[objectId]:
            # There was "non-monotonic" access from different threads,
            # so this is a shared object.
            self._lastThread[objectId] = thread
            self._shared.add(objectId)
            
        return timestamp

    def write(self, thread, objectId, field):
        t = self._currentTimestamp
        self._currentTimestamp = t + 1
        self._objects.setdefault(objectId, {})[field] = t
        
        touchedThreads = self._touchedThreads.setdefault(objectId, set())
        if thread not in touchedThreads:
            self._lastThread[objectId] = thread
            touchedThreads.add(thread)
        
        elif thread != self._lastThread[objectId]:
            self._lastThread[objectId] = thread
            self._shared.add(objectId)
        
        return t

    def sharedObjects(self, objectIds, thread):
        for objectId in objectIds - self._shared:
            if self._lastThread[objectId] != thread:
                self._lastThread[objectId] = thread
                self._shared.add(objectId)

        return self._shared.intersection(objectIds)
    
    def unwrittenSince(self, objectId, fieldTimestamps):
        fieldStatus = self._objects[objectId]
        result = set()

        for field, timestamp in fieldTimestamps.items():
            if fieldStatus[field] <= timestamp:
                result.add(field)
        
        return result
        
    def writtenSince(self, objectId, fieldTimestamps):
        fieldStatus = self._objects[objectId]
        result = set()

        for field, timestamp in fieldTimestamps.items():
            if fieldStatus[field] > timestamp:
                result.add(field)
        
        return result

    def currentTimestamp(self):
        return self._currentTimestamp
