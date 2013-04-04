import argparse
import collections
import gzip
import logging  
import sys

from contextlib import closing

import replay.fields as fields
import replay.names as names
import replay.operations as operations

import inference
import inference.sourcemodel

import output

from profile import ExecutionRecorder


def main(argv):
    arguments = _parseCommandlineArguments(argv)
    
    # Set up logging
    numericLevel = getattr(logging, arguments.verbosityLevel.upper(), None)
    if not isinstance(numericLevel, int):
        logging.error("Invalid verbosity level: %s", arguments.verbosityLevel)
    logging.basicConfig(level=numericLevel)
    
    # Set up profiling
    recorder = ExecutionRecorder()
    
    with closing(open(arguments.metaFileName, "rt")) as meta:
        methodNameTable, fieldNameTable = parseMetaTables(meta)
        observations = {}   # { method : MethodObservations }

        for logFileName in arguments.logFileNames:
            # Transparently support gzip compression
            o = gzip.open if logFileName.endswith(".gz") else open
            with closing(o(logFileName, "rb")) as log:
                operationStream = operations.operationStream(log, methodNameTable, fieldNameTable)
                updateObservations(observations, replay(operationStream))

        jClassRegistry = analyze(observations)

        for jClassName in sorted(jClassRegistry):
            if arguments.count:
                print(output.countJavaClass(jClassRegistry[jClassName]))
            else:
                print(output.formatJavaClass(jClassRegistry[jClassName]))

    # Output execution profile
    recorder.dump_data()
    

def updateObservations(observations1, observations2):
    for m, o in observations2.items():
        observations1[m] = o if m not in observations1 else (observations1[m] + o)


def replay(operationStream):
    # Create the runtime infrastructure objects
    threadAccessRecorder = fields.ThreadAccessRecorder()
    nameStacks = {}                 # { thread : NameStack }
    fieldAccessStacks = {}          # { thread : FieldAccessStack }
    methodObservations = {}         # { method : MethodObservations }
    
    lastThread = -1
    schedule = collections.deque()
    
    # Replay the operations in the log        
    for op in operationStream:
        t = op._thread
        
        # Record the thread schedule for judging the concurrency in the trace. 
        if t != lastThread:
            schedule.append(t)
            lastThread = t
        
        if t not in nameStacks:
            nameStacks[t] = names.NameStack()
        if t not in fieldAccessStacks:
            fieldAccessStacks[t] = fields.FieldAccessStack(threadAccessRecorder)
        
        op.applyTo( nameStacks[t], fieldAccessStacks[t], methodObservations )
        
    logging.debug("Trace contained {0} threads.".format(len(nameStacks)))
    logging.debug("Schedule: {0}".format(schedule))
    return methodObservations

    
def analyze(methodObservations):
    """Infer the atomic sets and units of work for the given method observations.
    
    Returns a registry of all classes in the observations.  Each class can be
    queried for its atomic sets, as well as its methods (which contain the unit
    of work data).
    """ 
    jClassRegistry = inference.sourcemodel.JavaClassRegistry()

    inference.aggregateObservations(jClassRegistry, methodObservations)
    inference.formAtomicSets(jClassRegistry)
        
    return jClassRegistry


def parseMetaTables(metaFile):
    """Return two dictionaries that map the unique method and field ids
    generated during instrumentation to the respective meta data.
    """
    methodNameTable = dict()        # { id : QualifiedMethodName }
    fieldNameTable = dict()         # { id : QualifiedName }
    
    metaLines = list(map(lambda l: l.strip(), metaFile.read().strip().split("\n")))

    # A single empty line divides the method and field name sections.
    divider = metaLines.index("")
    methodNameRows = metaLines[:divider]
    fieldNameRows = metaLines[divider+1:]
    
    for row in methodNameRows:
        # Ignore comments
        if row.startswith("#"):
            continue
        mId, scopeName, methodName, signature, isStatic, accessFlags = row.split("|")
        # TODO: Split methodName and signature if necessary.
        methodNameTable[ int(mId) ] = names.QualifiedMethodName(scopeName,
                                                                methodName + signature,
                                                                isStatic == "true",
                                                                int(accessFlags))
        
    for row in fieldNameRows:
        if row.startswith("#"):
            continue
        fId, scopeName, fieldName, fieldType = row.split("|")
        fieldNameTable[ int(fId) ] = names.QualifiedName(scopeName, fieldName, fieldType)
        
    return methodNameTable, fieldNameTable


def _parseCommandlineArguments(argv):
    description = """Infer atomic sets from field access traces."""
    argParser = argparse.ArgumentParser(description=description)
    
    argParser.add_argument("metaFileName", type=str, metavar="META_FILE",
                           help="""file containing the method and field
                               meta-data tables generated during
                               instrumentation""")
    argParser.add_argument("logFileNames", nargs="+", type=str, metavar="LOG_FILE",
                           help="""log file containing the field access
                               trace to use.  Can be gzip compressed.""")
    argParser.add_argument("--verbosityLevel", "-v", dest="verbosityLevel",
                           type=str, metavar="LEVEL", default="WARNING",
                           help="""enable printing debug information for events
                               whose severity is at least LEVEL.  Valid levels
                               are DEBUG, INFO, WARNING, ERROR, and CRITICAL.
                               The default is WARNING.""")
    argParser.add_argument("--count", "-c", dest="count",
                           action="store_true", default=False,
                           help="""instead of printing the actual annotations,
                               output a summary of their counts.""")
    
    return argParser.parse_args(argv)


if __name__ == "__main__":
    main(sys.argv[1:])
