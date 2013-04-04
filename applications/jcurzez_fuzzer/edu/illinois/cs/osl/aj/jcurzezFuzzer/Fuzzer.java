/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.illinois.cs.osl.aj.jcurzezFuzzer;

import java.util.Random;
import org.nongnu.savannah.jcurzez.Window;

/**
 *
 * @author minas
 */
public abstract class Fuzzer extends Thread
{

    public static int REPETITION_COUNT = 75;
    Window window;
    Random randomizer;

    public Fuzzer(Window win, long seed)
    {
        this.window = win;
        randomizer = new Random(seed);
    }

    @Override
    public void run()
    {
        for (int i = 0; i < REPETITION_COUNT; i++) {
        	try {
        		chooseOne();
        	}
        	catch (Exception e) {
        		// Silently ignore exceptions.
        	}
        }
            
    }

    protected abstract void chooseOne();
}
