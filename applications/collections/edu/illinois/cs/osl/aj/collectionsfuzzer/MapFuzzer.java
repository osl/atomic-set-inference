/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.illinois.cs.osl.aj.collectionsfuzzer;

import collections.Iterator;
import collections.Map;
import collections.Set;

/**
 *
 * @author minas
 */
public class MapFuzzer extends Fuzzer
{

	public MapFuzzer(Map<Object, Object> col, Map<Object, Object> otherCol,
			long seed, boolean allowed)    {
        super(col, otherCol, seed, allowed);
    }

	protected int getOpCount() {
		return 14;
	}

    @SuppressWarnings("unchecked")
	@Override
    protected void executeOne()
    {
        Map<Object, Object> map = (Map<Object, Object>) getCollection();
        Map<Object, Object> otherMap = (Map<Object, Object>) getCollection();
        
        int op = randomizer.nextInt(getOpCount());

        switch (op)
        {
            case 0:
                map.clear();
                break;
            case 1:
                map.containsKey(getObject());
                break;
            case 2:
                map.containsValue(getObject());
                break;
            case 3:
                mutateEntrySet(map.entrySet());
                break;
            case 4:
                if (allowed)
                    map.equals(otherMap);
                break;
            case 5:
                map.get(getObject());
                break;
            case 6:
                map.hashCode();
                break;
            case 7:
                map.isEmpty();
                break;
            case 8:
                map.keySet();
                break;
            case 9:
                map.put(getObject(), getObject());
                break;
            case 10:
                if (allowed)
                    map.putAll(otherMap);
                break;
            case 11:
                map.remove(getObject());
                break;
            case 12:
                map.size();
                break;
            case 13:
                map.values();
                break;
        }
    }
    
    protected void mutateEntrySet(Set<Map.Entry<Object, Object>> entries) {
        Iterator<Map.Entry<Object, Object>> it = entries.iterator();
        while (it.hasNext() && randomizer.nextInt(4) > 0) {
        	Map.Entry<Object, Object> entry = it.next();
        	entry.getKey();
        	entry.getValue();
        	if (randomizer.nextBoolean()) {
        		entry.setValue(getObject());
        	}
        }
    }
}
