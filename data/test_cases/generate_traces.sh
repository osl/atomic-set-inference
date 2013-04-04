#!/bin/bash
export JAVA_HOME=/opt/java6
export PATH=/opt/java6/bin:${PATH}

PROJECT_WORKSPACE=../../workspace

function instrument() {
  java -cp ${PROJECT_WORKSPACE}/tracing/bin:${PROJECT_WORKSPACE}/com.ibm.wala.shrike/bin:${PROJECT_WORKSPACE}/com.ibm.wala.util/bin edu.illinois.cs.osl.aj.Instrumenter ${*}
}

echo ">>> Instrumenting test cases."

instrument ${PROJECT_WORKSPACE}/test_cases/bin -o test_cases_instrumented.jar -m all_test_cases.meta -l /dev/shm/test_case.log

echo "Done."
echo

for case in ${PROJECT_WORKSPACE}/../tools/test_cases/edu/illinois/cs/osl/aj/testcases/*.java; do
   class=$(echo ${case} | sed -e 's/^.*\/test_cases\///' -e 's/\.java$//' -e 's/\//./g')
   echo ">>> Running ${class}"
   java -cp test_cases_instrumented.jar:${PROJECT_WORKSPACE}/tracing/bin ${class}
   gzip /dev/shm/test_case.log
   mv /dev/shm/test_case.log.gz $(echo ${class} | sed 's/^.*\.\([^.]\+\)$/\1/').log.gz
done
