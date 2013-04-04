import textwrap

def formatJavaClass(jclass):
    headerTemplate = textwrap.dedent("""
    === {0}
    """)
    
    atomicSetsTemplate = textwrap.dedent("""
    * Atomic sets:
    {0}
    """)
    atomicSetTemplate = """  - {{ {0} }}"""
    
    witnessTemplate = textwrap.dedent("""
    * Witnessed non-atomic fields: {{ {0} }}
    """)
    
    unitsOfWorkTemplate = textwrap.dedent("""
    * Units of work:
    {0}
    """)
    
    atomicSets = list()
    for fieldNames in jclass.atomicSets():
        fields = list()
        for fieldName in sorted(fieldNames):
            # FIXME: Use the list of suggested field aliases until the part for
            # resolving aliases has been implemented.
            fieldAliases = jclass.suggestedFieldAliases()
            if fieldName in fieldAliases:
                fields.append("{0}={1}".format(fieldName, sorted(fieldAliases[fieldName])))
            else:
                fields.append(fieldName)
        atomicSets.append( atomicSetTemplate.format(", ".join(fields)))
    
    # Only display unit of work annotations for methods that have such annotations
    uow = textwrap.indent("\n".join(sorted([formatJavaMethod(jm) for jm in jclass.methods()
                                            if jm.parameterAliases() ])), "  ")
    
    witnesses = ", ".join(sorted(jclass.nonAtomicFields()))
    
    return "".join([headerTemplate.format(jclass.name),
                    atomicSetsTemplate.format("\n".join(sorted(atomicSets))) if atomicSets else "",
                    witnessTemplate.format(witnesses) if witnesses else "",
                    unitsOfWorkTemplate.format(uow) if uow else "" ])


def formatJavaMethod(jmethod):
    uowTemplate = """{0}  [\n{1}\n]"""
    parameterTemplate = """  {0}: unitfor( {1} )"""
    
    # TODO: Resolve the respective atomic sets considering the witnesses.
    parameters = [ parameterTemplate.format(p.name, sorted(a))
                    for p, a in sorted(jmethod.parameterAliases().items()) ]

    # The parameters will be sorted by index (because of their name)
    # FIXME: This only works for up to nine parameters.
    return uowTemplate.format(jmethod.name, "\n".join(sorted(parameters)))


def countJavaClass(jclass):
    lineTemplate = """{jclass}:{atomicFieldCount}:{aliasCount}:{unitforCount}"""
    
    atomicFieldCount = 0
    aliasCount = 0
    unitforCount = 0
    
    for fieldNames in jclass.atomicSets():
        atomicFieldCount += len(fieldNames)
        
        fieldAliases = jclass.suggestedFieldAliases()
        aliasCount += len([ f for f in fieldNames if f in fieldAliases ])
    
    for jmethod in jclass.methods():
        unitforCount += len([ p for p, a in jmethod.parameterAliases().items() if a ]) 
    
    return lineTemplate.format(jclass=jclass.name,
                               atomicFieldCount=atomicFieldCount,
                               aliasCount=aliasCount,
                               unitforCount=unitforCount)
