/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.illinois.cs.osl.aj.collectionsfuzzer;

import collections.Collection;
import collections.Vector;

/**
 *
 * @author minas
 */
public class VectorFuzzer extends ListFuzzer
{

    public VectorFuzzer(Collection<Object> col, Collection<Object> otherCol,
            long seed, boolean allowed)
    {
        super(col, otherCol, seed, allowed);
    }

	protected int getOpCount() {
		return super.getOpCount() + 13;
	}

	@SuppressWarnings("unchecked")
	@Override
    protected void executeOne()
    {
        Vector<Object> vec = (Vector<Object>) getCollection();

        int op = randomizer.nextInt(getOpCount());

        switch (op)
        {
            case 0:
                vec.addElement(getObject());
                break;
            case 1:
                vec.capacity();
                break;
            case 2:
                int size = vec.size();
                vec.copyInto(new Object[size]);
                break;
            case 3:
                vec.elementAt(2);
                break;
            case 4:
                vec.elements();
                break;
            case 5:
                vec.ensureCapacity(200);
                break;
            case 6:
                vec.firstElement();
                break;
            case 7:
                vec.insertElementAt(getObject(), 2);
                break;
            case 8:
                vec.removeAllElements();
                break;
            case 9:
                vec.removeElementAt(2);
                break;
            case 10:
                vec.setElementAt(getObject(), 3);
                break;
            case 11:
                vec.setSize(10);
                break;
            case 12:
                vec.trimToSize();
                break;
            default:
            	super.executeOne();
        }
    }
}
