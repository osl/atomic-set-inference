/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.illinois.cs.osl.aj.jcurzezFuzzer;

import org.nongnu.savannah.jcurzez.Window;

/**
 *
 * @author minas
 */
public class DrawingPenFuzzer extends PenFuzzer
{

    public DrawingPenFuzzer(Window win, long seed)
    {
        super(win, seed);
        pen = win.getRectangle().getDrawingPen();
    }
}
