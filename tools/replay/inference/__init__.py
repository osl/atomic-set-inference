import collections
import logging
import random


#- Observation Aggregation ---------------------------------------------------- 

def aggregateObservations(classRegistry, methodObservations):
    """Annotate the classes in the class registry with the atomicity
    observations of all methods.
    """
    for method, observations in methodObservations.items():
        
        logging.debug(">>> %s.%s", method.scopeName, method.name)

        # Every method appears exactly once in the set of observations,
        # so this is the first time we see it.
        javaMethod = classRegistry[method.scopeName].addMethod(method)
        
        # Note: the roots of the prefix tree are the method parameters.
        at = prefixTree(mergeArrayIndices(observations.atomicSet()))
        
        # Remove all paths that have a witness as a prefix.
        mergedWitnesses = mergeArrayIndices(observations.interleavingWitnesses())
        pruneWitnesses(at, mergedWitnesses)
        
        # Do not treat constructors as internal units of work.  Because they
        # typically initialize all fields and execute atomically, this leads
        # to unnecessary merges of atomic sets.  Instead, treat them as
        # external units of work so that their suggestions can be overwritten
        # (but are not completely lost).
        if not javaMethod.isStatic() and not javaMethod.isConstructor():
            thisParameter = list(filter(lambda p: p.name == "0", at.keys()))
            
            if thisParameter:
                thisParameter = thisParameter[0]

                # Only handle direct children of "this" as internal.
                directChildren = dict([ (c, {}) for c in at[thisParameter] ])
                suggestAsAtomic(directChildren, classRegistry, False)
                for ct in at[thisParameter].values():
                    suggestAsAtomic(ct, classRegistry, True)
                
                suggestAliases(at[thisParameter], classRegistry)
            
        for parameter, branch in at.items():
            if javaMethod.isStatic() or javaMethod.isConstructor() or parameter.name != "0":
                suggestAsAtomic(branch, classRegistry, True)
                suggestAliases(branch, classRegistry)

        
        for witness in mergedWitnesses:
            if not javaMethod.isStatic() and witness[0].name == "0" and len(witness) == 2:
                # witness looks like "this.foo"; foo cannot be member of an atomic set.
                classRegistry[witness[1].scopeName].markNonAtomicField(witness[1].name)
                logging.debug("Internal witness to: %s", str(witness[1].name))
    
            else:
                # The method is "external" for the witness
                logging.debug("External witness to: %s", str(witness))
                javaMethod.refuseAtomicityOf(witness)

        # Record aliasing information for methods.
        for parameter, successors in at.items():
            pas = map(lambda s: s.name, successors.keys())
            javaMethod.setParameterAliases(parameter, pas)


def mergeArrayIndices(atomicSet):
    """Aggregate the access to all the entries of array "foo"
    as access to the virtual field "foo[]"."""
    result = set()
    
    for namePath in atomicSet:
        mergedPath = collections.deque(namePath[0:1])
        for i in range(1, len(namePath)):
            s = namePath[i]
            if s.type_ == "ARRAY_ENTRY":
                mergedPath.append(s.__class__(mergedPath[-1].scopeName, mergedPath[-1].name + "[]", mergedPath[-1].type_[1:]))
            else:
                mergedPath.append(s)
        result.add(namePath.__class__(mergedPath))
    
    return result


def prefixTree(segmentLists):
    """Create a prefix tree from the given list of paths in the tree.
    
    The tree is modeled using nested dictionaries: each node is a dictionary
    whose keys are the labels of the edges to its children (which are the
    values).  In this construction, the dictionary returned by the method can
    be seen as a "virtual root node" whose children are the different initial
    path segments in the given paths.
    
    >>> pt1 = prefixTree([ [1, 2], [1, 3, 4] ])
    >>> pt1 == {1: {2: {}, 3: {4: {}} } }
    True
    
    >>> pt2 = prefixTree([ [1, 2, 3], [1], [1, 2], [8] ])
    >>> pt2 == {1: {2: {3: {}}}, 8: {}}
    True
    """
    root = {}
    for sl in segmentLists:
        node = root
        for s in sl:
            node = node.setdefault(s, {})

    return root


#SuggestionCandidate = collections.namedtuple("SuggestionCandidate", "name scopeName isAtomic")

def pruneWitnesses(atomicTree, witnesses):
    # Prune everything beneath an actual witness.
    
    for witness in witnesses:
        # Descend to the actual witness field's parent in the atomic tree.
        currentNode = atomicTree
        for name in witness[:-1]:
            if name not in currentNode:
                break
            currentNode = currentNode[name]
        else:
            # Prune the witness and all its descendants.
            if witness[-1] in currentNode:
                del currentNode[witness[-1]]


# For a node in the tree built from proposed atomic sets
# - propose the names of the children as atomic set of fields for the node's type
# - for each child name, propose its children as aliases

def suggestAsAtomic(pt, classRegistry, external=False):
    candidates = pt.keys()
    
    # TODO: Add support for array parameters when AJ supports them.
    def nonArrayParameter(c):
        return not (c.name.endswith("[]") and type(c.scopeName) is not str)
    candidates = list(filter(nonArrayParameter, candidates))
    
    if candidates:
        # Candidates can belong to different scopes because of inheritance.
        suggestions = dict()
        for candidate in candidates:
            suggestions.setdefault(candidate.scopeName, set()).add(candidate)
        
        for scope, candidates in suggestions.items():
            candidateNames = map(lambda c: c.name, candidates)
            classRegistry[scope].suggestAtomicSet(candidateNames, external)
        
    for successors in pt.values():
        # FIXME: Recurse for all?  Even for ones that were reached via methods?
        suggestAsAtomic(successors, classRegistry, external)


def suggestAliases(pt, classRegistry):
    for candidate, aliases in pt.items():
        if aliases:
            aliasFields = map(lambda s: s.name, aliases)
            classRegistry[candidate.scopeName].suggestFieldAliases(candidate, aliasFields)
            
            suggestAliases(aliases, classRegistry)


#- Atomic Set Formation ------------------------------------------------------- 

def formAtomicSets(classRegistry):
    for jclass in classRegistry.classes():
        if len(jclass.suggestedAtomicSets(False)) == 0 \
            and len(jclass.suggestedAtomicSets(True)) == 0:
            continue
        
        # Get the atomic sets and remove the blacklisted fields.
        nonAtomic = jclass.nonAtomicFields()
        atomicSets = [ atomic - nonAtomic for atomic in jclass.suggestedAtomicSets(False) ]
        atomicSets = [ atomic for atomic in atomicSets if len(atomic) > 0 ]
        internalAtomicSets = mergeOverlapping(atomicSets)
        
        atomicSets = [ atomic - nonAtomic for atomic in jclass.suggestedAtomicSets(True) ]
        atomicSets = [ atomic for atomic in atomicSets if len(atomic) > 0 ]
        externalAtomicSets = mergeOverlapping(atomicSets)
        
        jclass.setAtomicSets(extend(internalAtomicSets, externalAtomicSets))
    

def mergeOverlapping(sets):
    """Merge overlapping sets (through a randomized fixpoint iteration).
    
       >>> m = mergeOverlapping([ {1, 2}, {3, 4}, {6, 7} ])
       >>> {1, 2} in m and {6, 7} in m and {3, 4} in m and len(m) == 3
       True
    
       >>> m = mergeOverlapping([ {1, 2, 3}, {3, 4}, {4, 6, 7} ])
       >>> {1, 2, 3, 4, 6, 7} in m and len(m) == 1
       True
    """
    if len(sets) < 2:
        return sets
    
    results = collections.deque()
    work = collections.deque(sets)
    
    random.shuffle(work)
    
    while work:
        head = work.popleft()
        rejects = collections.deque()
        changed = False

        for item in work:
            if head.isdisjoint(item):
                rejects.append(item)
            else:
                head.update(item)
                changed = True
        
        if changed:
            work = rejects
            work.append(head)
            random.shuffle(work)
        else:
            results.append(head)
                        
    return results


def extend(partition, extensionPartition):
    """Extend the elements of the given partition (set of disjoint sets)
    by -- partially -- merging them with elements from the extension partition
    while maintaining a partition.  (Thus, avoid creating overlaps.)
    
    >>> partition = set([ frozenset([1, 2]), frozenset([4, 5]) ])
    >>> extensionPartition = set([ frozenset([2, 3]) ])
    >>> r = extend(partition, extensionPartition)
    >>> frozenset([1, 2, 3]) in r
    True
    >>> len( r.difference(set([ frozenset([1, 2, 3]), frozenset([4, 5]) ])) )
    0

    Combine extensions with the best matching partition element.

    >>> extensionPartition2 = set([ frozenset([1, 2, 3, 4]) ])
    >>> r = extend(partition, extensionPartition2)
    >>> frozenset([1, 2, 3]) in r
    True
    
    If no element matches, include the extension as new partition element.

    >>> extensionPartition3 = set([ frozenset([7, 8]) ])
    >>> r = extend(partition, extensionPartition3)
    >>> frozenset([7, 8]) in r
    True
    """
    result = set([ frozenset(e) for e in partition ])
    for extension in extensionPartition:
        # Find best matching element and update the partition.
        matches = [ (len(e.intersection(extension)), e) for e in result ]
        rankedMatches = list(sorted(filter(lambda m: m[0] > 0, matches), reverse=True))
        if rankedMatches:
            best = rankedMatches[0]
            result.remove(best[1])
            result.add(best[1].union(extension.difference(*result)))
        elif extension:
            result.add(frozenset(extension))
    
    return result



# TODO: Resolving aliases requires access to the field's type.

# def formAliasesAndUnitsOfWork(classRegistry):
#
#    for jclass in classRegistry.classes():
#        atomicFields = set().union(*jclass.atomicSets())
#
#        for field, aliases in jclass.fieldAliases().items():
#            if field.name in atomicFields:
#                mergedSets = set([ classRegistry[field.scopeName].atomicSetNameOf(a) for a in aliases ])
#                if len(mergedSets) > 1:
#                    print("##### Multiple merges for field {0}".format(field))
#                elif mergedSets != set([None]):
#                    jclass.includeAtomicSetFor(field.name, mergedSets.pop())
        
    # For each parameter of each method
    # get class with parameter's type
    # ask for the atomic set that contains the requested field
    # check whether it collides with a witness(??)

