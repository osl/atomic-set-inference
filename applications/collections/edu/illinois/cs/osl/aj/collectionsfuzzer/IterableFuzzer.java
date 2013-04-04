/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.illinois.cs.osl.aj.collectionsfuzzer;

/**
 *
 * @author minas
 */
public abstract class IterableFuzzer extends Fuzzer
{

	public IterableFuzzer(Iterable<Object> collection1, Iterable<Object> collection2,
			long seed, boolean allowed) {
		super(collection1, collection2, seed, allowed);
	}
	
	protected int getOpCount() {
		return 1;
	}

    @SuppressWarnings("unchecked")
    @Override
    protected void executeOne()
    {
		Iterable<Object> iter = (Iterable<Object>) getCollection();

    	// NOTE: We use java.util.Iterator below because the Iterable
    	//       interface returns this type.  However, the classes
    	//       in the collections package return collections.Iterator,
    	//       which is a subtype of java.util.Iterator.
    	
    	java.util.Iterator<Object> it = iter.iterator();
    	while (it.hasNext() && randomizer.nextInt(4) > 0) {
    		it.next();
    	}
    }
}
