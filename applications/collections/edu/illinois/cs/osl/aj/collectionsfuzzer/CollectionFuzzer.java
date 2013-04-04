/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.illinois.cs.osl.aj.collectionsfuzzer;

import collections.Collection;

/**
 *
 * @author minas
 */
public abstract class CollectionFuzzer extends IterableFuzzer
{

    public CollectionFuzzer(Collection<Object> col, Collection<Object> otherCol,
            long seed, boolean allowed)
    {
        super(col, otherCol, seed, allowed);
    }

    protected int getOpCount() {
    	return super.getOpCount() + 14;
    }
    
    @SuppressWarnings("unchecked")
	@Override
    protected void executeOne()
    {
        Collection<Object> col = (Collection<Object>) getCollection();
        Collection<Object> otherCol = (Collection<Object>) getCollection();

        int op = randomizer.nextInt(getOpCount());

        switch (op)
        {
            case 0:
                col.add(getObject());
                break;
            case 1:
                if (allowed)
                    col.addAll(otherCol);
                break;
            case 2:
                col.clear();
                break;
            case 3:
                col.contains(getObject());
                break;
            case 4:
                if (allowed)
                    col.containsAll(otherCol);
                break;
            case 5:
                if (allowed)
                    col.equals(otherCol);
                break;
            case 6:
                col.hashCode();
                break;
            case 7:
                col.isEmpty();
                break;
            case 8:
                col.remove(getObject());
                break;
            case 9:
                if (allowed)
                    col.removeAll(otherCol);
                break;
            case 10:
                if (allowed)
                    col.retainAll(otherCol);
                break;
            case 11:
                col.size();
                break;
            case 12:
                col.toArray();
                break;
            case 13:
                col.toArray(new Object[1]);
                break;
            default:
            	super.executeOne();
        }
    }
}
