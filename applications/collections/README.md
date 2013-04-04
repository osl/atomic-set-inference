Partial Java Collections Framework
==================================

This directory contains a manual for extracting the collections
framework from OpenJDK6 (build b27) and a fuzzing tool for generating
traces of the synchronized versions of the collections.


Extraction Procedure
--------------------

* Create a directory `collections/`.
* Download the sources for build b27 from http://download.java.net/openjdk/jdk6/
* Extract the `util` package and remove all sub-packages.

~~~~
$ tar xzf openjdk-6-src-b27-26_oct_2012.tar.gz
$ cp -r jdk/src/share/classes/java/util util.unused
$ find util.unused/ -mindepth 1 -type d -exec rm -r {} \;
~~~~

* Copy the following classes into the `collections/` directory:

~~~~
AbstractCollection.java
AbstractList.java
AbstractMap.java
AbstractQueue.java
AbstractSequentialList.java
AbstractSet.java
ArrayDeque.java
ArrayList.java
Arrays.java
BitSet.java
Collection.java
Collections.java
Comparator.java
ConcurrentModificationException.java
Deque.java
Dictionary.java
EmptyStackException.java
Enumeration.java
HashMap.java
HashSet.java
Hashtable.java
IdentityHashMap.java
Iterator.java
LinkedHashMap.java
LinkedHashSet.java
LinkedList.java
List.java
ListIterator.java
ListResourceBundle.java
Map.java
NavigableMap.java
NavigableSet.java
NoSuchElementException.java
PriorityQueue.java
Queue.java
RandomAccess.java
Set.java
SortedMap.java
SortedSet.java
Stack.java
TreeMap.java
TreeSet.java
Vector.java
WeakHashMap.java
~~~~

* Adapt the package name.
~~~~
$ sed 's/^package java.util;/package collections;/g' -i collections/*.java
$ sed 's/^import java.util./import collections./g' -i collections/*.java
~~~~

* Fix `Collections.java` by including
~~~~
import java.util.Random;
import java.util.RandomAccess;
~~~~

* Fix `Iterator.java` by having it extend the actual `java.util.Iterator`:
~~~~
public interface Iterator<E> extends java.util.Iterator<E> {
~~~~

* Compile with `javac` from JDK 6.
