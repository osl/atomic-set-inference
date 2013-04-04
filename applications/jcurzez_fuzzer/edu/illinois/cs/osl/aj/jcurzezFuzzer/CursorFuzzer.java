/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.illinois.cs.osl.aj.jcurzezFuzzer;

import org.nongnu.savannah.jcurzez.Cursor;
import org.nongnu.savannah.jcurzez.Window;

/**
 *
 * @author minas
 */
public class CursorFuzzer extends Fuzzer
{

    Cursor cursor;

    public CursorFuzzer(Window win, long seed)
    {
        super(win, seed);
        // AJ-API (jcurzez-aj1) removes direct access to the cursor.  Disable.
        this.cursor = win.getCursor();
    }

    @Override
    protected void chooseOne()
    {
        final int OP_COUNT = 7;

        int op = randomizer.nextInt(OP_COUNT);

        // AJ-API (jcurzez-aj1) removes direct access to the cursor.  Disable.
        switch (op)
        {
            case 0:
                cursor.getCursorVisibility();
                break;
            case 1:
                cursor.getX();
                break;
            case 2:
                cursor.getY();
                break;
            case 3:
                cursor.setCursorVisibility(randomizer.nextBoolean() ? Cursor.VERY_VISIBLE : Cursor.INVISIBLE);
                break;
            case 4:
                cursor.setX(randomizer.nextInt(10));
                break;
            case 5:
                cursor.setY(randomizer.nextInt(10));
                break;
            case 6:
                cursor.setXY(randomizer.nextInt(10), randomizer.nextInt(10));
                break;
        }
    }
}
