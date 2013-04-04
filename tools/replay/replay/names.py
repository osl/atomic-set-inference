import collections

QualifiedName = collections.namedtuple("QualifiedName", "scopeName name type_")
QualifiedMethodName = collections.namedtuple("QualifiedMethodName", "scopeName name isStatic accessFlags")


class NamePath:
    __slots__ = ["_segments"]
    
    def __init__(self, segments):
        self._segments = tuple(segments)
        
    def append(self, segment):
        self._segments += (segment,)

    def __add__(self, segment):
        return self.__class__(self._segments + (segment,))

    def __getitem__(self, index):
        return self._segments[index]

    def __iter__(self):
        return iter(self._segments)

    def __len__(self):
        return len(self._segments)
        
    def __eq__(self, other):
        return self._segments == other._segments
    
    def __str__(self):
        return ".".join(map(lambda s: s.name, self._segments))
    
    def __hash__(self):
        return hash(self._segments)


class NameEnvironment:
    def __init__(self):
        # { objectId -> (name, ownerObjectId) }
        self._names = {}
        # { (name, ownerObjectId) -> objectId }
        self._values = {}
        
    def __getitem__(self, objectId):
        return self._names.get(objectId, list())
             
    def add(self, objectId, name, ownerId = None):
        key = (name, ownerId)
        if key in self._values:
            # Update
            oldObjectId = self._values[key]
            if objectId == objectId:
                return
            self._names[oldObjectId] = [ k for k in self._names[oldObjectId] if k != key ]
            
        self._values[key] = objectId
        self._names.setdefault(objectId, list()).append( (name, ownerId) )


# Stack of name environments that gives all the names of an object in the top frame.

class NameStack:
    def __init__(self):
        self._environments = collections.deque()
        
    def pushScope(self):
        self._environments.append( NameEnvironment() )
    
    def popScope(self):
        self._environments.pop()
    
    def namePathsTo(self, objectId, _seen=None):
        result = set()

        if len(self._environments) == 0:
            return result

        if _seen is None:
            _seen = set()
        _seen.add(objectId)

        nameEnv = self._environments[-1]
        
        # TODO: Use BFS instead of the DFS + seen-set combination below.
        for name, ownerId in nameEnv[objectId]:
            # No owner means that the name refers to a parameter.
            # Note that "this" is also just a parameter.
            if ownerId is None:
                result.add(NamePath([name]))
            elif ownerId not in _seen:
                def appendName(ownerName):
                    return ownerName + name
                result.update( map(appendName, self.namePathsTo(ownerId, _seen)) )
            # If the ownerId has been seen before, the path is a circle and
            # should not be added to the result set at all.
        
        return result        
    
    def addName(self, objectId, name, ownerId = None):
        self._environments[-1].add(objectId, name, ownerId)

    def __getitem__(self, index):
        return self._environments[-1][index]
