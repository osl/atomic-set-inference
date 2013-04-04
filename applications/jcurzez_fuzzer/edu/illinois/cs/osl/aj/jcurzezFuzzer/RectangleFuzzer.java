/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.illinois.cs.osl.aj.jcurzezFuzzer;

import org.nongnu.savannah.jcurzez.Rectangle;
import org.nongnu.savannah.jcurzez.Window;

/**
 *
 * @author minas
 */
public class RectangleFuzzer extends Fuzzer
{

    Rectangle rectangle;

    public RectangleFuzzer(Window win, long seed)
    {
        super(win, seed);
        rectangle = win.getRectangle();
    }

    @Override
    protected void chooseOne()
    {
        final int OP_COUNT = 11;

        int op = randomizer.nextInt(OP_COUNT);

        switch (op)
        {
            case 0:
                rectangle.getBottom();
                break;
            case 1:
                rectangle.getHeight();
                break;
            case 2:
                rectangle.getLeft();
                break;
            case 3:
                rectangle.getRight();
                break;
            case 4:
                rectangle.getTop();
                break;
            case 6:
            	// AJ-API (jcurzez-aj*)
//                    rec.getTopLeft();
                break;
            case 7:
                rectangle.getWidth();
                break;
            case 8:
            	rectangle.getDrawingPen();
            	break;
            case 9:
            	rectangle.getFillingPen();
            	break;
            case 10:
            	rectangle.toString();
            	break;
        }
    }
}
