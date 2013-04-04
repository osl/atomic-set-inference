/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.illinois.cs.osl.aj.collectionsfuzzer;

import java.util.Random;
import java.util.Vector;

/**
 *
 * @author minas
 */
public abstract class Fuzzer extends Thread
{
    public static int REPETITION_COUNT = 100;

    protected final Random randomizer;
    protected final boolean allowed;

    private Object sharedCollection1;
    private Object sharedCollection2;
    private Vector<Object> objectPool;

    public Fuzzer(Object collection1, Object collection2, long seed, boolean allowed)
    {
    	this.objectPool = new Vector<Object>();
        
        this.randomizer = new Random(seed);
    	this.sharedCollection1 = collection1;
        this.sharedCollection2 = collection2;

        /**
         * This flag signals whether the Fuzzer is allowed to use "dangerous"
         * operations that may lead to deadlocks (if used concurrently).
         */
        this.allowed = allowed;
    }

    abstract protected void executeOne();
    abstract protected int getOpCount();

    @Override
    public void run()
    {
        for (int i = 0; i < REPETITION_COUNT; i++) {
        	try {
        		executeOne();
        		System.out.print(".");
        	}
        	catch (Throwable t) {
        		// Simply ignore exceptions and hope for the best.
        		System.out.print("!");
        	}
        }
    }

    protected Object getObject()
    {
    	if (randomizer.nextBoolean()) {
    		Object o = new Integer(randomizer.nextInt());
    		objectPool.add(o);
    		return o;
    	} else {
    		return objectPool.get(randomizer.nextInt(objectPool.size()));
    	}
    }
    
    protected Object[] getObjectArray()
    {
    	Object[] a = new Object[randomizer.nextInt(5)];

    	for (int i=0; i<a.length; ++i) {
    		a[i] = getObject();
    	}
    	
    	return a;
    }
    
    protected Object getCollection() {
    	return randomizer.nextBoolean() ? sharedCollection1 : sharedCollection2;
    }
}
