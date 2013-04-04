Building the Project
====================

* Download the JCurzez1 benchmark `jcurzez1.zip` from the
  [Data-Centric Concurrency Control project page](http://sss.cs.purdue.edu/projects/aj/)
* In Eclipse, use `File > Import...` to import the projects contained
  in the `jcurzez1.zip` archive. This generates the projects
  `jcurzez-redo-simple`, `jcurzez-aj1`, and `jcurzez-aj1-Translated`.
* In the `Source` tab of the `Java Build Path` pane of the project
  properties (`Project > Properties`), add a linked source folder
  pointing to the `applications/jcurzez_fuzzer/` directory.
* Comment out the commands in the fuzzer that use invalid (in
  `jcurzez-aj1-Translated`) classes.  Search for `AJ-API` in the
  fuzzer sources.
* Add a print statement into the method `_deleteLine_internal()` in
  class `AbstractWindow` to trigger an interleaved thread schedule.
  Otherwise, no aliases for the `buffer` array will be inferred.
* Use `Project > Clean...` on the projects to rebuild them (if
  `Project > Build Automatically` is checked).
