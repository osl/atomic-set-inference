import logging


# Taken from com.ibm.wala.shrikeBT.Constants
ACC_PUBLIC = 0x1
ACC_PRIVATE = 0x2
ACC_PROTECTED = 0x4
ACC_STATIC = 0x8
ACC_FINAL = 0x10
ACC_SYNCHRONIZED = 0x20
ACC_SUPER = 0x20
ACC_VOLATILE = 0x40
ACC_TRANSIENT = 0x80
ACC_NATIVE = 0x100
ACC_INTERFACE = 0x200
ACC_ABSTRACT = 0x400
ACC_STRICT = 0x800


class JavaMethod:
    def __init__(self, javaClass, name, accessFlags):
        self.name = name
        self.javaClass = javaClass
        self.accessFlags = accessFlags
        
        self._parameterAliases = dict()         # { parameter: set(aliases) }
        self._interleavingWitnesses = set()     # set([ name paths ])
    
    def setParameterAliases(self, parameter, aliases):
        # Ignore aliases for "this"; these should be aliases of the field itself.
        # Example: an alias "foo.bar" for "this" means that "bar" should be an
        # alias of field "foo" in the class that contains this method.
        if not self.isStatic() and parameter.name == "0":
            return
        
        self._parameterAliases[parameter] = list(aliases)
        logging.debug("Set parameter %s aliases in %s.%s: %s", parameter.name,
                        self.javaClass.name, self.name, self._parameterAliases[parameter])
        
    def parameterAliases(self):
        return self._parameterAliases
    
    def refuseAtomicityOf(self, witness):
        self._interleavingWitnesses.add(witness)

    def isStatic(self):
        return self.accessFlags & ACC_STATIC != 0
    
    def isPrivate(self):
        return self.accessFlags & ACC_PRIVATE != 0

    def isConstructor(self):
        return self.name.startswith("<init>") or self.name.startswith("<clinit>")

    def __str__(self):
        return "{0}.{1}".format(str(self.javaClass), self.name)


class JavaClass:
    def __init__(self, name, registry):
        self.name = name
        self.registry = registry
        self._methods = dict()                  # { name: java method }

        # Suggestions; these are collected during the method observation
        # aggregation phase.
        self._internallySuggestedAtomicSets = set()
        self._externallySuggestedAtomicSets = set()
        self._suggestedFieldAliases = dict()    # { field name: field names }
        self._nonAtomicFields = set()

        # Final atomic sets; these are generated during the atomic set
        # formation phase. 
        self._atomicSets = list()
    
    
    def addMethod(self, qualifiedName):
        if qualifiedName.name in self._methods:
            msg = "Method {0} already exists in class {1}"
            raise ValueError(msg.format(qualifiedName.name, self.name))
        
        jm = JavaMethod(self, qualifiedName.name, qualifiedName.accessFlags)
        self._methods[qualifiedName.name] = jm
        
        return jm

    def methods(self):
        return self._methods.values()
    
    def method(self, name):
        return self._methods[name]
    

    def suggestAtomicSet(self, fields, external=False):
        if not fields:
            return
        frozenFields = frozenset(fields)
        if external:
            logging.debug("Externally suggested atomic in %s: %s", self.name, frozenFields)
            self._externallySuggestedAtomicSets.add(frozenFields)
        else:
            logging.debug("Internally suggested atomic in %s: %s", self.name, frozenFields)
            self._internallySuggestedAtomicSets.add(frozenFields)
    
    def suggestedAtomicSets(self, external=False):
        if external:
            atomicSets = self._externallySuggestedAtomicSets
        else:
            atomicSets = self._internallySuggestedAtomicSets
        return [ set(aset) for aset in atomicSets ]
    
    def markNonAtomicField(self, field):
        logging.debug("Marked non-atomic in %s: %s", self.name, field)
        self._nonAtomicFields.add(field)
        
    def nonAtomicFields(self):
        return self._nonAtomicFields


    def suggestFieldAliases(self, field, aliases):
        fieldAliases = list(aliases)
        logging.debug("Suggested aliases for field %s.%s: %s", self.name, field.name, fieldAliases)
        # FIXME: This should actually store the field object, not just the name
        #        to resolve the atomic set later on.
        self._suggestedFieldAliases.setdefault(field.name, set()).update(fieldAliases)
    
    def suggestedFieldAliases(self):
        return self._suggestedFieldAliases
        

    def setAtomicSets(self, atomicSets):
        self._atomicSets = atomicSets
        
    def atomicSets(self):
        return self._atomicSets
    
    
    def atomicSetNameOf(self, fieldName):
        candidates = [ str(i) for i, aset in enumerate(self._atomicSets) if fieldName in aset ]
        if len(candidates) >= 2:
            logging.error("Atomic field %s.%s is in several atomic sets", self.name, fieldName)
            return None
        elif len(candidates) == 0:
            return None
        else:
            return candidates[0]
    
    def __str__(self):
        return "{0}({1})".format(self.__class__.__name__, self.name)


class JavaClassRegistry:
    def __init__(self): 
        self._classes = dict()  # { class name: JavaClass }
    
    def __getitem__(self, name):
        return self._classes.setdefault(name, JavaClass(name, self))

    def __iter__(self):
        return iter(self._classes)

    def classes(self):
        return self._classes.values()
