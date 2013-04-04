/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.illinois.cs.osl.aj.collectionsfuzzer;

import collections.Iterator;
import collections.Map;
import collections.Set;
import collections.SortedMap;

/**
 *
 * @author minas
 */
public class SortedMapFuzzer extends MapFuzzer
{

	public SortedMapFuzzer(Map<Object, Object> col,
			Map<Object, Object> otherCol, long seed, boolean allowed) {
        super(col, otherCol, seed, allowed);
    }

	protected int getOpCount() {
		return super.getOpCount() + 6;
	}

	@SuppressWarnings("unchecked")
	@Override
    protected void executeOne()
    {
        SortedMap<Object, Object> map = (SortedMap<Object, Object>) getCollection();

        int op = randomizer.nextInt(getOpCount());

        switch (op)
        {
            case 0:
                map.comparator();
                break;
            case 1:
                map.firstKey();
                break;
            case 2:
                mutateEntrySet(map.headMap(getObject()).entrySet());
                break;
            case 3:
                map.lastKey();
                break;
            case 4:
                mutateEntrySet(map.subMap(getObject(), getObject()).entrySet());
                break;
            case 5:
                mutateEntrySet(map.tailMap(getObject()).entrySet());
                break;
            default:
            	super.executeOne();
        }
    }
}
