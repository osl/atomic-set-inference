/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.illinois.cs.osl.aj.collectionsfuzzer;

import collections.ArrayList;
import collections.Collections;
import collections.HashMap;
import collections.HashSet;
import collections.Hashtable;
import collections.IdentityHashMap;
import collections.LinkedHashMap;
import collections.LinkedHashSet;
import collections.LinkedList;
import collections.List;
import collections.Map;
import collections.Set;
import collections.SortedMap;
import collections.SortedSet;
import collections.Stack;
import collections.TreeMap;
import collections.TreeSet;
import collections.Vector;
import collections.WeakHashMap;

/**
 *
 * @author minas
 */
public class FuzzEverything
{
	private static int RANDOM_SEED1 = 23;
	private static int RANDOM_SEED2 = 42;

    public static void main(String[] args) throws Exception
    {
    	try {
	    	if (args.length > 0) {
	    		Fuzzer.REPETITION_COUNT = Integer.parseInt(args[0]);
	    	}
	    	if (args.length > 1) {
	    		RANDOM_SEED1 = Integer.parseInt(args[1]);
	    	}
	    	if (args.length > 2) {
	    		RANDOM_SEED2 = Integer.parseInt(args[2]);
	    	}
    	}
    	catch (NumberFormatException e) {
    		System.err.println("ERROR: Malformed command line argument.");
    		System.err.println();
    		System.err.println("Valid arguments: <REPETITION_COUNT> <RANDOM_SEED1> <RANDOM_SEED2>");
    		System.err.println();
    		System.err.println("The repetition count and the random seeds are integers.");
    		System.exit(1);
    	}
    	
        fuzzList(ArrayList.class);
        fuzzList(LinkedList.class);
        fuzzList(Stack.class);
        fuzzList(Vector.class);

        fuzzMap(HashMap.class);
        fuzzMap(Hashtable.class);
        fuzzMap(IdentityHashMap.class);
        fuzzMap(LinkedHashMap.class);
        fuzzMap(WeakHashMap.class);

        fuzzSet(HashSet.class);
        fuzzSet(LinkedHashSet.class);

        fuzzSortedMap(TreeMap.class);

        fuzzSortedSet(TreeSet.class);
        
        System.out.println();
    }

	@SuppressWarnings("unchecked")
	private static void fuzzSortedSet(@SuppressWarnings("rawtypes") Class c) throws Exception
    {
        SortedSet<Object> set1 = Collections.synchronizedSortedSet((SortedSet<Object>) c.newInstance());
        SortedSet<Object> set2 = Collections.synchronizedSortedSet((SortedSet<Object>) c.newInstance());

        Thread t1 = new SortedSetFuzzer(set1, set2, RANDOM_SEED1, true);
        Thread t2 = new SortedSetFuzzer(set2, set1, RANDOM_SEED2, false);

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }

    @SuppressWarnings("unchecked")
	private static void fuzzSet(@SuppressWarnings("rawtypes") Class c) throws Exception
    {
        Set<Object> set1 = Collections.synchronizedSet((Set<Object>) c.newInstance());
        Set<Object> set2 = Collections.synchronizedSet((Set<Object>) c.newInstance());

        Thread t1 = new SetFuzzer(set1, set2, RANDOM_SEED1, true);
        Thread t2 = new SetFuzzer(set2, set1, RANDOM_SEED2, false);

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }

    @SuppressWarnings("unchecked")
	private static void fuzzList(@SuppressWarnings("rawtypes") Class c) throws Exception
    {
        List<Object> list1 = Collections.synchronizedList((List<Object>) c.newInstance());
        List<Object> list2 = Collections.synchronizedList((List<Object>) c.newInstance());

        Thread t1 = new ListFuzzer(list1, list2, RANDOM_SEED1, true);
        Thread t2 = new ListFuzzer(list2, list1, RANDOM_SEED2, false);

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }

    @SuppressWarnings("unchecked")
	private static void fuzzMap(@SuppressWarnings("rawtypes") Class c) throws Exception
    {
        Map<Object, Object> map1 = Collections.synchronizedMap((Map<Object, Object>) c.newInstance());
        Map<Object, Object> map2 = Collections.synchronizedMap((Map<Object, Object>) c.newInstance());

        Thread t1 = new MapFuzzer(map1, map2, RANDOM_SEED1, true);
        Thread t2 = new MapFuzzer(map2, map1, RANDOM_SEED2, false);

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }

    @SuppressWarnings("unchecked")
	private static void fuzzSortedMap(@SuppressWarnings("rawtypes") Class c) throws Exception
    {
        SortedMap<Object, Object> map1 = Collections.synchronizedSortedMap((SortedMap<Object, Object>) c.newInstance());
        SortedMap<Object, Object> map2 = Collections.synchronizedSortedMap((SortedMap<Object, Object>) c.newInstance());

        Thread t1 = new SortedMapFuzzer(map1, map2, RANDOM_SEED1, true);
        Thread t2 = new SortedMapFuzzer(map2, map1, RANDOM_SEED2, false);

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }
}
