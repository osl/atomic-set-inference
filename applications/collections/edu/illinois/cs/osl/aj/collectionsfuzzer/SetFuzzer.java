/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.illinois.cs.osl.aj.collectionsfuzzer;

import collections.Set;

/**
 *
 * @author minas
 */
public class SetFuzzer extends CollectionFuzzer
{

    public SetFuzzer(Set<Object> set, Set<Object> otherSet, long seed,
            boolean allowed)
    {
        super(set, otherSet, seed, allowed);
    }

    @Override
    protected void executeOne()
    {
        super.executeOne();
    }
}
