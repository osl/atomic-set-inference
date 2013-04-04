Building the Project
====================

* Download the Weblech benchmark `weblech.zip` from the
  [Data-Centric Concurrency Control project page](http://sss.cs.purdue.edu/projects/aj/)
* In Eclipse, use `File > Import...` to import the projects contained
  in the `weblech.zip` archive. This generates the projects
  `weblech-0.0.3`, and `weblech-AJ-Translated`.  Answer _No_ if
  asked whether to overwrite the project file.
* The `weblech-0.0.3/` directory is the AJ version, so rename it
  to `weblech-0.0.3-AJ`.
* Download the original from http://weblech.sourceforge.net/ and build
  it with `build.sh`
* Edit `weblech-0.0.3/config/Spider.properties`.  Collect from a
  fast-enough host (for example, `localhost:8000`) with 5 threads.
  Watch for Spider-Thread-n messages and make sure that n changes.


Collecting Data
===============

* The program collected files from a host-local webserver for 4
  minutes.
