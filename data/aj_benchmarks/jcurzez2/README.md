Building the Project
====================

* Download the JCurzez2 benchmark `jcurzez2.zip` from the
  [Data-Centric Concurrency Control project page](http://sss.cs.purdue.edu/projects/aj/)
* In Eclipse, use `File > Import...` to import the projects contained
  in the `jcurzez2.zip` archive. This generates the projects
  `jcurzez-redo`, `jcurzez-aj2`, and `jcurzez-aj2-Translated`.
* In the `Source` tab of the `Java Build Path` pane of the project
  properties (`Project > Properties`), add a linked source folder
  pointing to the `applications/jcurzez_fuzzer/` directory.
* Add a print statement into the method `_deleteLine_internal()` in
  class `AbstractWindow` to trigger an interleaved thread schedule.
  Otherwise, no aliases for the `buffer` array will be inferred.
* Use `Project > Clean...` on the projects to rebuild them (if
  `Project > Build Automatically` is checked).
