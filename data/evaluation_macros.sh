#!/bin/bash
export JAVA_HOME=/opt/java6
export PATH=/opt/java6/bin:${PATH}
export TEMP_PATH=/dev/shm


function projectJarFile() {
    project="${1}"
    echo -n "${project}_instrumented.jar"
}

function projectMetaFile() {
    project="${1}"
    echo -n "${TEMP_PATH}/${project}.meta"
}

function projectLogFile() {
    project="${1}"
    echo -n "${TEMP_PATH}/${project}.log"
}

function instrument() {
    project="${1}"
    # Remove the project name from the argument list so we can use $* below.
    shift

    jarFile="$(projectJarFile "${project}")"
    metaFile="$(projectMetaFile "${project}")"
    logFile="$(projectLogFile "${project}")"

    classpath=""
    for lib in tracing com.ibm.wala.shrike com.ibm.wala.util; do
	classpath="${classpath}${WORKSPACE}/${lib}/bin:"
    done

    echo ">>> Instrumenting..."
    java -cp ${classpath} edu.illinois.cs.osl.aj.Instrumenter -o "${jarFile}" -m "${metaFile}" -l "${logFile}" ${*}
    echo "Done."
}

