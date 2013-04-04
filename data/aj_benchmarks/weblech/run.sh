#!/bin/bash

######################################################################
# This script automates the inference of atomic sets of an
# application.  Its purpose is not only to speed up the evaluation of
# the inference algorithm, but also to document how the evaluation
# data was derived.
#
# The script instruments the application, executes it to generate a
# field access trace, runs the inference tool on the trace, and
# finally compares the derived atomic sets against a reference.
#
######################################################################

cd $(dirname ${0})
source ../../evaluation_macros.sh

### Project Parameters ###############################################

WORKSPACE=../../../workspace

PROJECT_NAME="weblech"
PROJECT_PATH="${WORKSPACE}/${PROJECT_NAME}-0.0.3"

CONFIG_FILE=Spider.properties

RESULTS_NAME="${PROJECT_NAME}-$(basename ${CONFIG_FILE})"


### Derived Variables ################################################

jarFileName="$(projectJarFile ${PROJECT_NAME})"
metaFileName="$(projectMetaFile ${PROJECT_NAME})"
logFileName="$(projectLogFile ${PROJECT_NAME})"


### Execution ########################################################

#-- Instrumentation --------------------------------------------------

instrument ${PROJECT_NAME} ${PROJECT_PATH}/classes
mv "${metaFileName}" ./${RESULTS_NAME}.meta


#-- Trace Collection -------------------------------------------------

echo
echo ">>> Running... (Hit Ctrl-C after some time)"
java -cp ${jarFileName}:${PROJECT_PATH}/lib/log4j-1.1.3.jar:${WORKSPACE}/tracing/bin/ weblech.ui.TextSpider ${CONFIG_FILE}
echo "Done."

echo
echo ">>> Copying log..."
gzip "${logFileName}"
mv "${logFileName}.gz" ./${RESULTS_NAME}.log.gz
echo "Done."


#-- Inference --------------------------------------------------------

echo
echo ">>> Inferring atomic sets..."
python3 -OO ${WORKSPACE}/../tools/replay/main.py -v DEBUG ${RESULTS_NAME}.meta ${RESULTS_NAME}.log.gz > ${RESULTS_NAME}.atomic_sets.txt 2> ${RESULTS_NAME}.debug_log.txt
echo "Done."


### Verification #####################################################

if [ -f ${RESULTS_NAME}.expected.txt ]; then
    diff ${RESULTS_NAME}.atomic_sets.txt ${RESULTS_NAME}.expected.txt
    if [[ $? != 0 ]]; then
	echo "!!! Something changed."
    fi
fi
