/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.illinois.cs.osl.aj.collectionsfuzzer;

import collections.Collection;
import collections.List;
import collections.ListIterator;

/**
 *
 * @author minas
 */
public class ListFuzzer extends CollectionFuzzer
{

    public ListFuzzer(Collection<Object> col, Collection<Object> otherCol,
            long seed, boolean allowed)
    {
        super(col, otherCol, seed, allowed);
    }

	protected int getOpCount() {
		return super.getOpCount() + 10;
	}

	@SuppressWarnings("unchecked")
	@Override
    protected void executeOne()
    {
        List<Object> list = (List<Object>) getCollection();
        List<Object> otherList = (List<Object>) getCollection();

        int op = randomizer.nextInt(getOpCount());

        switch (op)
        {
            case 0:
                list.add(randomizer.nextInt(list.size()), getObject());
                break;
            case 1:
                list.addAll(randomizer.nextInt(list.size()), otherList);
                break;
            case 2:
                list.get(randomizer.nextInt(list.size()));
                break;
            case 3:
                list.indexOf(getObject());
                break;
            case 4:
                list.lastIndexOf(getObject());
                break;
            case 5:
            	ListIterator<Object> it1 = list.listIterator();
            	while (it1.hasNext() && randomizer.nextInt(4) > 0) {
            		it1.next();
            	}
                break;
            case 6:
            	ListIterator<Object> it2 = list.listIterator(randomizer.nextInt(list.size()));
              	while (it2.hasNext() && randomizer.nextInt(4) > 0) {
            		it2.next();
            	}
                break;
            case 7:
            	list.remove(randomizer.nextInt(list.size()));
            	break;
            case 8:
                list.set(randomizer.nextInt(list.size()), getObject());
                break;
            case 9:
                list.subList(randomizer.nextInt(list.size()), randomizer.nextInt(list.size()));
                break;
            default:
            	super.executeOne();
        }
    }
}
