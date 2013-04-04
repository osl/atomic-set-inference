/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.illinois.cs.osl.aj.jcurzezFuzzer;

import org.nongnu.savannah.jcurzez.Attribute;
import org.nongnu.savannah.jcurzez.BackgroundColor;
import org.nongnu.savannah.jcurzez.ForegroundColor;
import org.nongnu.savannah.jcurzez.Pen;
import org.nongnu.savannah.jcurzez.Rectangle;
import org.nongnu.savannah.jcurzez.Window;

/**
 *
 * @author minas
 */
public class FuzzEverything
{
	private static int RANDOM_SEED = 17;

    public static void main(String[] args) throws Exception
    {
    	try {
	    	if (args.length > 0) {
	    		Fuzzer.REPETITION_COUNT = Integer.parseInt(args[0]);
	    	}
	    	if (args.length > 1) {
	    		RANDOM_SEED = Integer.parseInt(args[1]);
	    	}
    	}
    	catch (NumberFormatException e) {
    		System.err.println("ERROR: Malformed command line argument.");
    		System.err.println();
    		System.err.println("Valid arguments: <REPETITION_COUNT> <RANDOM_SEED>");
    		System.err.println();
    		System.err.println("The repetition count and the random seed are integers.");
    		System.exit(1);
    	}
    	
    	// TODO: Fuzz copy constructors.

    	// NOTE: Use the object constructor for the original API (in projects
    	// jcurzez-redo and jcurzez-redo-simple) and the factory method for
    	// the AJ-refactored API (jcurzez-aj*).
    	//
    	// Search for the 'AJ-API' marker to find all locations in the fuzzer
    	// that need to be adapted.
    	
    	// Original API (jcurzez-redo and jcurzez-redo-simple)
    	Window win1 = new Window(new Rectangle(10, 10, 50, 20,
    	// AJ-API (jcurzez-aj*)
//    	Window win1 = /*new*/ Window.getWindow(new Rectangle(10, 10, 50, 20,
                new Pen(ForegroundColor.YELLOW,
                BackgroundColor.BLUE,
                Attribute.BOLD),
                new Pen(ForegroundColor.WHITE,
                BackgroundColor.BLUE,
                Attribute.UNDERLINE)));

    	// Original API (jcurzez-redo and jcurzez-redo-simple)
        Window win2 = new Window(win1,
    	// AJ-API (jcurzez-aj*)
//    	Window win2 = /*new*/ Window.getWindow(win1,
                new Rectangle(12, 12, 45, 15,
                new Pen(ForegroundColor.YELLOW,
                BackgroundColor.RED,
                Attribute.NORMAL),
                new Pen(ForegroundColor.YELLOW,
                BackgroundColor.RED,
                Attribute.BOLD)));

        Thread t1 = new WindowFuzzer(win1, RANDOM_SEED);
        Thread t2 = new WindowFuzzer(win2, RANDOM_SEED+1);
        Thread t3 = new CursorFuzzer(win1, RANDOM_SEED+2);
        Thread t4 = new CursorFuzzer(win2, RANDOM_SEED+3);
        Thread t5 = new DrawingPenFuzzer(win1, RANDOM_SEED+4);
        Thread t6 = new DrawingPenFuzzer(win2, RANDOM_SEED+5);
        Thread t7 = new FillingPenFuzzer(win1, RANDOM_SEED+6);
        Thread t8 = new FillingPenFuzzer(win2, RANDOM_SEED+7);
        Thread t9 = new RectangleFuzzer(win1, RANDOM_SEED+8);
        Thread t10 = new RectangleFuzzer(win2, RANDOM_SEED+9);

        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();
        t6.start();
        t7.start();
        t8.start();
        t9.start();
        t10.start();

        t1.join();
        t2.join();
        t3.join();
        t4.join();
        t5.join();
        t6.join();
        t7.join();
        t8.join();
        t9.join();
        t10.join();
    }
}
