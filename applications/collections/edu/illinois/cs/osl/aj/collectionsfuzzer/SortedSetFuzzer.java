/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.illinois.cs.osl.aj.collectionsfuzzer;

import collections.SortedSet;

/**
 *
 * @author minas
 */
public class SortedSetFuzzer extends SetFuzzer
{

    public SortedSetFuzzer(SortedSet<Object> set, SortedSet<Object> otherSet,
            long seed, boolean allowed)
    {
        super(set, otherSet, seed, allowed);
    }

	protected int getOpCount() {
		return super.getOpCount() + 6;
	}

	@SuppressWarnings("unchecked")
	@Override
    protected void executeOne()
    {
        SortedSet<Object> set = (SortedSet<Object>) getCollection();

        int op = randomizer.nextInt(getOpCount());

        switch (op)
        {
            case 0:
                set.comparator();
                break;
            case 1:
                set.first();
                break;
            case 2:
                set.headSet(getObject());
                break;
            case 3:
                set.last();
                break;
            case 4:
                set.subSet(getObject(), getObject());
                break;
            case 5:
                set.tailSet(getObject());
                break;
            default:
            	super.executeOne();
        }
    }
}
