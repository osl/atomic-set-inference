/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.illinois.cs.osl.aj.collectionsfuzzer;

import collections.Collection;
import collections.Queue;

/**
 *
 * @author minas
 */
public class QueueFuzzer extends CollectionFuzzer
{

    public QueueFuzzer(Collection<Object> col, Collection<Object> otherCol,
            long seed, boolean allowed)
    {
        super(col, otherCol, seed, allowed);
    }

	protected int getOpCount() {
		return super.getOpCount() + 5;
	}

	@SuppressWarnings("unchecked")
	@Override
    protected void executeOne()
    {
        Queue<Object> qu = (Queue<Object>) getCollection();

        int op = randomizer.nextInt(getOpCount());

        switch (op)
        {
            case 0:
                qu.element();
                break;
            case 1:
                qu.offer(getObject());
                break;
            case 2:
                qu.peek();
                break;
            case 3:
                qu.poll();
                break;
            case 4:
                qu.remove();
                break;
            default:
            	super.executeOne();
        }
    }
}
