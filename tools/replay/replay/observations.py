class MethodObservations:
    def __init__(self, method):
        self._method = method
        self._atomicSet = set()
        self._interleavingWitnesses = set()
    
    def addAtomicSet(self, namePaths):
        # Every method can only suggest a single atomic set per parameter.
        # To store the candidates, take the union of all suggested name
        # paths and later split them by prefix.
        self._atomicSet.update(namePaths)

    def atomicSet(self):
        return self._atomicSet
    
    def addInterleavingWitnesses(self, names):
        self._interleavingWitnesses.update(names)

    def interleavingWitnesses(self):
        return self._interleavingWitnesses

    def __add__(self, other):
        assert isinstance(other, self.__class__)
        if self._method != other._method:
            raise ValueError("Observations belong to different methods")
        
        m = self.__class__(self._method)
        m._atomicSet = set.union(self._atomicSet, other._atomicSet)
        m._interleavingWitnesses = set.union(self._interleavingWitnesses, other._interleavingWitnesses) 
        
        return m