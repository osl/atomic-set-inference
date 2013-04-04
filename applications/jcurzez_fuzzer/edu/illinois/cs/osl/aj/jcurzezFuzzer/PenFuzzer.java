/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.illinois.cs.osl.aj.jcurzezFuzzer;

import org.nongnu.savannah.jcurzez.Attribute;
import org.nongnu.savannah.jcurzez.BackgroundColor;
import org.nongnu.savannah.jcurzez.ForegroundColor;
import org.nongnu.savannah.jcurzez.Pen;
import org.nongnu.savannah.jcurzez.Window;

/**
 *
 * @author minas
 */
public abstract class PenFuzzer extends Fuzzer
{

    protected Pen pen;

    public PenFuzzer(Window win, long seed)
    {
        super(win, seed);
    }

    @Override
    protected void chooseOne()
    {
        final int OP_COUNT = 8;

        int op = randomizer.nextInt(OP_COUNT);
        
        switch (op)
        {
            case 0:
                Attribute a = pen.getAttribute();
                a.and(pen.getAttribute());
                a.andNot(pen.getAttribute());
                a.equals(a);
                a.getValue();
                a.hashCode();
                break;
            case 1:
                pen.getBackgroundColor();
                break;
            case 2:
                pen.getForegroundColor();
                break;
            case 3:
                pen.setAttribute(randomizer.nextBoolean() ? Attribute.BLINK : Attribute.NORMAL);
                pen.setAttribute(pen.getAttribute());
                break;
            case 4:
                pen.setBackgroundColor(randomizer.nextBoolean() ? BackgroundColor.BLACK : BackgroundColor.CYAN);
                break;
            case 5:
                pen.setForegroundColor(randomizer.nextBoolean() ? ForegroundColor.MAGENTA : ForegroundColor.YELLOW);
                break;
            case 6:
                pen.toString();
                break;
            case 7:
            	pen.equals(pen);
            	break;
        }
    }
}
