/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.illinois.cs.osl.aj.collectionsfuzzer;

import collections.NavigableSet;

/**
 *
 * @author minas
 */
public class NavigableSetFuzzer extends SortedSetFuzzer
{

    public NavigableSetFuzzer(NavigableSet<Object> set,
            NavigableSet<Object> otherSet, long seed, boolean allowed)
    {
        super(set, otherSet, seed, allowed);
    }

	protected int getOpCount() {
		return super.getOpCount() + 10;
	}

	@SuppressWarnings("unchecked")
	@Override
    protected void executeOne()
    {
        NavigableSet<Object> set = (NavigableSet<Object>) getCollection();

        int op = randomizer.nextInt(getOpCount());

        switch (op)
        {
            case 0:
                set.ceiling(getObject());
                break;
            case 1:
                set.descendingIterator();
                break;
            case 2:
                set.descendingSet();
                break;
            case 3:
                set.floor(getObject());
                break;
            case 4:
                set.higher(getObject());
                break;
            case 5:
                set.lower(getObject());
                break;
            case 6:
                set.pollFirst();
                break;
            case 7:
                set.pollLast();
                break;
            case 8:
                set.subSet(getObject(), true, getObject(), true);
                break;
            case 9:
                set.tailSet(getObject(), true);
                break;
            default:
            	super.executeOne();
        }
    }
}
