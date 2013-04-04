/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.illinois.cs.osl.aj.jcurzezFuzzer;

import org.nongnu.savannah.jcurzez.Cell;
import org.nongnu.savannah.jcurzez.Cursor;
import org.nongnu.savannah.jcurzez.Window;
import org.nongnu.savannah.jcurzez.frame.OldStyleFrame;
import org.nongnu.savannah.jcurzez.frame.SingleFrame;

/**
 *
 * @author minas
 */
public class WindowFuzzer extends Fuzzer
{

    public WindowFuzzer(Window win, long seed)
    {
        super(win, seed);
    }

    @Override
    public void run()
    {
        window.insertString("let's start with some words in here");
        window.insertLine();
        window.insertString("text continues");

        super.run();
    }

    @Override
    protected void chooseOne()
    {
        final int OP_COUNT = 23;

        int op = randomizer.nextInt(OP_COUNT);

        switch (op)
        {
            case 0:
                window.beep();
                break;
            case 1:
                window.clear();
                break;
            case 2:
                window.clearToBottom();
                break;
            case 3:
                window.clearToEndOfLine();
                break;
            case 4:
                window.deleteChar();
                break;
            case 5:
                window.deleteLine();
                break;
            case 6:
            	// AJ-API (jcurzez-aj1) removes direct access to the cursor.  Use
            	// indirect access instead.
                window.getCursor();
//            	window.setCursorVisibility(randomizer.nextBoolean() ? Cursor.VERY_VISIBLE : Cursor.INVISIBLE);
//            	window.setCursorXY(randomizer.nextInt(10), randomizer.nextInt(10));
                break;
            case 7:
                window.getId();
                break;
            case 8:
                window.getParent();
                break;
            case 9:
                window.getRectangle();
                break;
            case 10:
                window.hasLinesWrapped();
                break;
            case 11:
                window.insertChar((char) randomizer.nextInt(256));
                break;
            case 12:
                window.insertLine();
                break;
            case 13:
                window.insertString("some text here"
                        + (randomizer.nextBoolean() ? "\n" : ""));
                break;
            case 14:
                window.printChar((char) randomizer.nextInt(256));
                break;
            case 15:
                window.printString("some more text here"
                        + (randomizer.nextBoolean() ? "\n" : ""));
                break;
            case 16:
                window.refresh();
                break;
            case 17:
                window.touch();
                break;
            case 18:
                window.wouldScroll();
                break;
            case 19:
            	window.move(randomizer.nextInt(10), randomizer.nextInt(10));
                break;
            case 20:
                window.gotoXY(randomizer.nextInt(10), randomizer.nextInt(10));
                break;
            case 21:
                window.printCell(randomizer.nextInt(10), randomizer.nextInt(10), Cell.NULL_CELL);
                break;
            case 22:
                window.setFrame(randomizer.nextBoolean()
                        ? new OldStyleFrame() : new SingleFrame());
                break;
        }
    }
}
