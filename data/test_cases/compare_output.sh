#!/bin/bash
for expectedOutput in expected_output/*.txt; do
    testCase=$(basename ${expectedOutput} | sed 's/\.txt$//')
    tempOutput=$(mktemp)
    python ../../tools/replay/main.py all_test_cases.meta ${testCase}.log.gz > ${tempOutput}
    # diff has exit code 0 if the files are identical, and 1 if they differ.
    diff ${expectedOutput} ${tempOutput} > /dev/null
    if [[ $? == 1 ]]; then
	echo "! ${testCase}"
	mv ${tempOutput} ${testCase}.unexpected.txt
    else
	rm -f ${tempOutput}
    fi
done
