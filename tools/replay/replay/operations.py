import collections
import logging
import sys

from struct import unpack

import replay.names as names
import replay.observations as observations


#- Constants ------------------------------------------------------------------ 

# Operation codes matching the definitions in FieldAccessLog.java
OP_ENTER = 0x1
OP_VALUE = 0x2
OP_GET_PRIMITIVE = 0x4
OP_GET_REFERENCE = 0x14
OP_GET_ARRAY = 0x24
OP_PUT_PRIMITIVE = 0x8
OP_PUT_REFERENCE = 0x18
OP_PUT_ARRAY = 0x28
OP_MONITOR_ENTRY = 0x30
OP_MONITOR_EXIT = 0x31
OP_EXIT = 0x40



#- Operations (effect-carrying objects) --------------------------------------- 

class Operation:
    def __init__(self, thread):
        self._thread = thread
    
    def applyTo(self, nameStack, fieldAccessStack, methodObservations):
        pass
    
    
class EnterOperation(Operation):
    def __init__(self, thread, method):
        super().__init__(thread)
        self._method = method
    
    def applyTo(self, nameStack, fieldAccessStack, methodObservations):
        nameStack.pushScope()
        fieldAccessStack.pushScope(self._method)


class ExitOperation(Operation):
    def applyTo(self, nameStack, fieldAccessStack, methodObservations):
        # Collect the name paths to all accessed atomic and non-atomic fields.
        atomicPaths, witnessPaths, inheritedPaths = set(), set(), set()
        for objectId in fieldAccessStack.sharedObjects(self._thread):
            objectNamePaths = nameStack.namePathsTo(objectId)
            
            for field in fieldAccessStack.atomicFields(objectId):
                atomicPaths.update(map(lambda p: p + field, objectNamePaths))
        
            for field in fieldAccessStack.interleavedFields(objectId):
                witnessPaths.update(map(lambda p: p + field, objectNamePaths))

            for field in fieldAccessStack.inheritedFields(objectId):
                inheritedPaths.update(map(lambda p: p + field, objectNamePaths))
        
        # Store the atomic paths so their segments can later be proposed as
        # atomic fields of the respective classes.  Also store the witnesses
        # against atomicity.
        m = fieldAccessStack.topScope()
        
        # Remove witnesses against local fields that were not actually accessed,
        # but whose access was inherited from a sub-scope.
        if not m.isStatic:
            nonWitnesses = set(filter(lambda p: len(p) == 2 and p[0].name == "0", inheritedPaths))
        else:
            nonWitnesses = set()
        
        obs = methodObservations.setdefault(m, observations.MethodObservations(m))
        obs.addAtomicSet(atomicPaths)
        obs.addInterleavingWitnesses(witnessPaths - nonWitnesses)

        # Leave the current method.
        nameStack.popScope()
        fieldAccessStack.popScope()
    

class ValueOperation(Operation):
    def __init__(self, thread, parameter, value):
        super().__init__(thread)
        self._parameter = parameter
        self._value = value
        
    def applyTo(self, nameStack, fieldAccessStack, methodObservations):
        m = fieldAccessStack.topScope()
        pt = ValueOperation.parameterTypes(m)
        # FIXME: m is a QualifiedMethodName; for fields, the scope is just a string.
        nameStack.addName(self._value, names.QualifiedName(m, str(self._parameter), pt[self._parameter]))

    @staticmethod
    def parameterTypes(m):
        n = m.name
        signature = n[ n.index("(")+1 : n.index(")") ]    # Ignore the return type
        types, i = collections.deque(), 0
        while i < len(signature):
            t, i = ValueOperation._typeAt(signature, i)
            types.append(t)
        
        # "this" is the implicit 0th parameter to non-static methods.  Its
        # type is the method's containing class (the scope).
        if not m.isStatic:
            types.appendleft(m.scopeName)
        
        return types

    @staticmethod
    def _typeAt(signature, index):
        """Return the JVM type name that starts at the given index in the given
        signature if it is a non-primitive type; otherwise return None.  Also,
        return the index of the next type in the signature.
        
        For arrays, the type name will only be returned if the element type is
        non-primitive.
        
        >>> ValueOperation._typeAt("Lfoo/Foo;I[[Lbar/Bar;", 0)
        ('Lfoo/Foo;', 9)
        >>> ValueOperation._typeAt("Lfoo/Foo;I[[Lbar/Bar;", 9)
        (None, 10)
        >>> ValueOperation._typeAt("Lfoo/Foo;I[[Lbar/Bar;", 10)
        ('[[Lbar/Bar;', 21)
        """
        s = signature[index]
        if s == "L":
            t = signature[index : signature.index(";", index+1)+1]
            nextIndex = index + len(t)
        elif s == "[":
            # Find the array element type by recursing
            t, nextIndex = ValueOperation._typeAt(signature, index+1)
            if t is not None:
                # Array consists of non-primitive elements
                t = "[" + t
        else:
            t, nextIndex = None, index+1
            
        return t, nextIndex


class MonitorEnterOperation(Operation):
    def __init__(self, thread, monitor, instructionNumber):
        super().__init__(thread)
        self._monitor = monitor
        self._instructionNumber = instructionNumber
    
    def applyTo(self, nameStack, fieldAccessStack, methodObservations):
        scope = fieldAccessStack.topScope()
        if scope.name.endswith("$synchronized"):
            # Cut off the "$n$synchronized" suffix.
            name = "{0}${1}$synchronized".format("$".join(scope.name.split("$")[:-2]), self._instructionNumber)
        else:
            name = "{0}${1}$synchronized".format(scope.name, self._instructionNumber) 
        synchronizedBlock = names.QualifiedMethodName(scope.scopeName, name, scope.isStatic, scope.accessFlags)
        
        for field, owner in nameStack[self._monitor]:
            fieldAccessStack.markAsMonitor(owner, field)

        fieldAccessStack.pushScope(synchronizedBlock)


class MonitorExitOperation(Operation):
    def applyTo(self, nameStack, fieldAccessStack, methodObservations):
        # FIXME: This is just copy-and-paste from ExitOperation.  Unify the methods.
        # Collect the name paths to all accessed atomic and non-atomic fields.
        atomicPaths, witnessPaths, inheritedPaths = set(), set(), set()
        for objectId in fieldAccessStack.sharedObjects(self._thread):
            objectNamePaths = nameStack.namePathsTo(objectId)
            
            for field in fieldAccessStack.atomicFields(objectId):
                atomicPaths.update(map(lambda p: p + field, objectNamePaths))
        
            for field in fieldAccessStack.interleavedFields(objectId):
                witnessPaths.update(map(lambda p: p + field, objectNamePaths))

        # Store the atomic paths so their segments can later be proposed as
        # atomic fields of the respective classes.  Also store the witnesses
        # against atomicity.
        sb = fieldAccessStack.topScope()

        if not sb.isStatic:
            nonWitnesses = set(filter(lambda p: len(p) == 2 and p[0].name == "0", inheritedPaths))
        else:
            nonWitnesses = set()
            
        obs = methodObservations.setdefault(sb, observations.MethodObservations(sb))
        obs.addAtomicSet(atomicPaths)
        obs.addInterleavingWitnesses(witnessPaths - nonWitnesses)

        fieldAccessStack.popScope()


class PrimitiveGetOperation(Operation):
    def __init__(self, thread, owner, field):
        super().__init__(thread)
        self._owner = owner if owner != 0 else field.scopeName
        self._field = field
        
    def applyTo(self, nameStack, fieldAccessStack, methodObservations):
        fieldAccessStack.get(self._thread, self._owner, self._field)

    
class ReferenceGetOperation(PrimitiveGetOperation):
    def __init__(self, thread, owner, field, value):
        super().__init__(thread, owner, field)
        self._value = value
        
    def applyTo(self, nameStack, fieldAccessStack, methodObservations):
        nameStack.addName(self._value, self._field, self._owner)
        super().applyTo(nameStack, fieldAccessStack, methodObservations)


class ArrayGetOperation(PrimitiveGetOperation):
    def __init__(self, thread, owner, field, value):
        # field is really the (integer) index.  Since array access operations
        # do not carry (relevant) type or scope information, mock values are
        # used.  The function mergeArrayIndices() in the inference module
        # resolves these mock values to actual values. 
        qf = names.QualifiedName("ARRAY", str(field), "ARRAY_ENTRY")
        super().__init__(thread, owner, qf)
        self._value = value
        
    def applyTo(self, nameStack, fieldAccessStack, methodObservations):
        nameStack.addName(self._value, self._field, self._owner)
        super().applyTo(nameStack, fieldAccessStack, methodObservations)


class PrimitivePutOperation(Operation):
    def __init__(self, thread, owner, field):
        super().__init__(thread)
        self._owner = owner if owner != 0 else field.scopeName
        self._field = field
        
    def applyTo(self, nameStack, fieldAccessStack, methodObservations):
        fieldAccessStack.put(self._thread, self._owner, self._field)

    
class ReferencePutOperation(PrimitivePutOperation):
    def __init__(self, thread, owner, field, value):
        super().__init__(thread, owner, field)
        self._value = value
        
    def applyTo(self, nameStack, fieldAccessStack, methodObservations):
        nameStack.addName(self._value, self._field, self._owner)
        super().applyTo(nameStack, fieldAccessStack, methodObservations)


class ArrayPutOperation(PrimitivePutOperation):
    def __init__(self, thread, owner, field, value):
        # See ArrayGetOperation.__init__()
        qf = names.QualifiedName("ARRAY", str(field), "ARRAY_ENTRY")
        super().__init__(thread, owner, qf)
        self._value = value
        
    def applyTo(self, nameStack, fieldAccessStack, methodObservations):
        nameStack.addName(self._value, self._field, self._owner)
        super().applyTo(nameStack, fieldAccessStack, methodObservations)


    
#- Operation Stream Generation ------------------------------------------------

def operationStream(log, methodNameTable, fieldNameTable):
    while True:
        b = log.read(9)
        
        if len(b) < 9:
            break
        operation, t = unpack("!bq", b)
        
        if operation == OP_ENTER:
            methodId = unpack("!i", log.read(4))[0]             # unpack() returns a tuple
            yield EnterOperation(t, methodNameTable[methodId])
        
        elif operation == OP_EXIT:
            yield ExitOperation(t)
        
        elif operation == OP_VALUE:
            parameter, valueObjectId = unpack("!Bi", log.read(5))
            yield ValueOperation(t, parameter, valueObjectId)
        
        elif operation == OP_MONITOR_ENTRY:
            monitorObjectId, instructionNumber = unpack("!ii", log.read(8))
            yield MonitorEnterOperation(t, monitorObjectId, instructionNumber)
            
        elif operation == OP_MONITOR_EXIT:
            yield MonitorExitOperation(t)
        
        elif operation == OP_GET_PRIMITIVE:
            ownerObjectId, fieldId = unpack("!ii", log.read(8))
            yield PrimitiveGetOperation(t, ownerObjectId, fieldNameTable[fieldId])
        
        elif operation == OP_GET_REFERENCE:
            ownerObjectId, fieldId, valueObjectId = unpack("!iii", log.read(12))
            yield ReferenceGetOperation(t, ownerObjectId, fieldNameTable[fieldId], valueObjectId)
        
        elif operation == OP_GET_ARRAY:
            arrayObjectId, index, valueObjectId = unpack("!iii", log.read(12))
            # NOTE: the index is an integer, not a qualified name as in the case of fields
            yield ArrayGetOperation(t, arrayObjectId, index, valueObjectId)
        
        elif operation == OP_PUT_PRIMITIVE:
            ownerObjectId, fieldId = unpack("!ii", log.read(8))
            yield PrimitivePutOperation(t, ownerObjectId, fieldNameTable[fieldId])
        
        elif operation == OP_PUT_REFERENCE:
            ownerObjectId, fieldId, valueObjectId = unpack("!iii", log.read(12))
            yield ReferencePutOperation(t, ownerObjectId, fieldNameTable[fieldId], valueObjectId)
        
        elif operation == OP_PUT_ARRAY:
            arrayObjectId, index, valueObjectId = unpack("!iii", log.read(12))
            # NOTE: the index is an integer, not a qualified name as in the case of fields
            yield ArrayPutOperation(t, arrayObjectId, index, valueObjectId)
            
        else:
            # TODO: Implement more sensible error reporting and handling.
            logging.critical("Could not parse entry at position %s.  Aborting.", log.tell()-len(b))
            sys.exit(1)
