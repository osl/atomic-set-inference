Atomic Set Inferencer
=====================

This repository contains the tool chain for automatically inferring
atomic sets that is described in the 2013
[UIUC](http://cs.illinois.edu) technical report
[Automated Inference of Atomic Sets for Safe Concurrent Execution][tr]
by Peter Dinges, Minas Charalambides, and Gul Agha.  It furthermore
contains the evaluation data used in the report, as well as the custom
workloads.


Abstract
--------

[Atomic sets][atomic-sets] are a synchronization mechanism in which
the programmer specifies the groups of data that must be accessed as a
unit.  The compiler can check this specification for consistency,
detect deadlocks, and automatically add the primitives to prevent
interleaved access.  Atomic sets relieve the programmer from the
burden of recognizing and pruning execution paths which lead to
interleaved access, thereby reducing the potential for data races.

However, manually converting programs from lock-based synchronization
to atomic sets requires reasoning about the program's concurrency
structure, which can be a challenge even for small programs.  Our
analysis eliminates the challenge by automating the reasoning.  Our
implementation of the analysis allowed us to derive the atomic sets
for large code bases such as the Java `collections` framework in a
matter of minutes.  The analysis is based on execution traces;
assuming all traces reflect intended behavior, our analysis allows
safe concurrency by preventing unobserved interleavings which may
harbor latent _Heisenbugs_.


Overview
--------

The tool chain consists of a Java byte code instrumenter and an
inference tool.  The instrumenter uses [WALA's][wala] Shrike library
to insert calls to the field access tracing library into the input
byte code.  After instrumentation, the target program must be executed
to generate field access traces.  The traces are the input for the
inference tool, which is a [Python][python] implementation of the
algorithm described in the [technical report][tr].

* The `tools/` directory contains the instrumenter (`tools/tracing/`),
  as well as the inference tool (`tools/replay/`).  It furthermore
  contains a set of test cases used to ensure that everything works as
  expected.
* The `applications/` directory contains the fuzzing tools used to
  generate the execution traces for the Java collections framework and
  the [JCurzez library][jcurzez] variations.
* The `data/` directory contains the data used to evaluate the
  approach (`data/aj_benchmarks/`), as well as the expected outputs
  for the test cases.


Usage
-----

The sub-directories of the `data/aj_benchmarks/` directory contain the
data used to evaluate our tool.  Each directory contains a shell
script `run.sh` that automates the process of instrumenting and
running the target application, and analyzing the results.  The
commands in the shell scripts provide documentation on how to execute
each step.


License
-------

The Atomic Set Inferencer tool chain may be used, copied, and modified
for education, research, and non-profit purposes as described in the
`LICENSE` file.  Copyright (c) 2013 The University of Illinois Board
of Trustees.  All Rights Reserved.


Acknowledgments
---------------

This work was supported in part by sponsorships from the Army Research
Office under award number W911NF-09-1-0273, as well as the Air Force
Research Laboratory and the Air Force Office of Scientific Research
under agreement number FA8750-11-2-0084.


[atomic-sets]: http://sss.cs.purdue.edu/projects/aj/ "Data-Centric Concurrency Control project page"
[jcurzez]: http://www.nongnu.org/jcurzez/ "JCurzez, a screen management library"
[python]: http://python.org/ "Homepage of the Python programming language"
[tr]: http://hdl.handle.net/2142/43357 "Technical Report: Automated Inference of Atomic Sets for Safe Concurrent Execution"
[wala]: http://wala.sourceforge.net/ "IBM T.J. Watson Libraries for Analysis"
