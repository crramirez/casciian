/*
 * Casciian - Java Text User Interface
 *
 * Written 2013-2025 by Autumn Lamonte
 *
 * To the extent possible under law, the author(s) have dedicated all
 * copyright and related and neighboring rights to this software to the
 * public domain worldwide. This software is distributed without any
 * warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along
 * with this software. If not, see
 * <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package casciian.effect;

import casciian.TWindow;
import casciian.backend.Screen;
import casciian.bits.CellAttributes;

/**
 * Make the window look like it was burned out with plasma fire.
 */
public class WindowBurnOutEffect extends WindowBurnInEffect {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public contructor.
     *
     * @param window the window to burn out
     */
    public WindowBurnOutEffect(final TWindow window) {
        super(window);

        fadeTime = 32;
    }

    // ------------------------------------------------------------------------
    // Effect -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // WindowTransitionEffect -------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The draw function called by the DummyScreen.
     */
    @Override
    protected void drawEffect() {
        CellAttributes attr = new CellAttributes();

        int x0 = dummyWindow.getEffectedWindow().getX();
        int y0 = dummyWindow.getEffectedWindow().getY();
        Screen oldScreen = dummyWindow.getOldScreen();
        Screen screen = dummyWindow.getScreen();

        long now = System.currentTimeMillis();
        if (startTime < 0) {
            startTime = now;
        }

        boolean fade = false;
        if (now - startTime > fadeTime) {
            fade = true;
        }

        screen.resetClipping();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                int idx = PALETTE[plasma[x][y]];
                attr.setForeColorRGB(idx);
                attr.setBackColorRGB(idx);
                int ch = 0x2588;
                if (idx > 3) {
                    screen.putCharXY(x + x0, y + y0, ch, attr);
                } else if (!fade) {
                    screen.putCharXY(x + x0, y + y0,
                        oldScreen.getCharXY(x + x0, y + y0));
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // WindowBurnOutEffect ----------------------------------------------------
    // ------------------------------------------------------------------------

}
