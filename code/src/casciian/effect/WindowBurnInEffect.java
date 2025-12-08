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
 * Make the window look like it was burned in with plasma fire.
 */
public class WindowBurnInEffect  extends WindowTransitionEffect {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The plasma colors, from dimmest to brightest.
     */
    protected final int [] PALETTE = {
        0x000000,
        // 0x700000,
        0xC00000,
        0xFE0000,
        // 0xFF0141,
        0xFF4001,
        0xFF7F00,
        0xFFC000,
        0xFFFF01,
        0xFFFF80,
    };

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The plasma fire field.
     */
    protected int [][] plasma;

    /**
     * The number of rows in the plasma field.
     */
    protected int rows = 0;

    /**
     * The number of columns in the plasma field.
     */
    protected int columns = 0;

    /**
     * Time since effect began.
     */
    protected long startTime = -1;

    /**
     * Maximum time before fading.
     */
    protected int fadeTime = 96;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public contructor.
     *
     * @param window the window to burn in
     */
    public WindowBurnInEffect(final TWindow window) {
        super(window);

        rows = window.getHeight();
        columns = window.getWidth();
        plasma = new int[columns][rows];

        // Place random hotspots.
        int ODDS = 20;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                int hotspot = (int) (Math.random() * ODDS);
                if (hotspot > (ODDS - 2)) {
                    plasma[x][y] = PALETTE.length - 1;
                }
            }
        }
    }


    // ------------------------------------------------------------------------
    // Effect -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Update the effect.
     */
    public void update() {
        if (dummyWindow == null) {
            return;
        }

        rows = dummyWindow.getEffectedWindow().getHeight();
        columns = dummyWindow.getEffectedWindow().getWidth();

        long now = System.currentTimeMillis();
        if (startTime < 0) {
            startTime = now;
        }

        boolean fade = false;
        if (now - startTime > fadeTime) {
            fade = true;
        }

        if (fade) {
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < columns; x++) {
                    plasma[x][y] = Math.max(0, plasma[x][y] - 1);
                }
            }
            int alpha = dummyWindow.getAlpha();
            alpha = Math.max(0, alpha - 2);
            dummyWindow.setAlpha(alpha);
        } else {
            int [][] newPlasma = new int[columns][rows];
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < columns; x++) {
                    newPlasma[x][y] = burn(x, y);
                }
            }
            plasma = newPlasma;
        }
    }

    /**
     * Perform "burn" operation on plasma at (x, y).
     *
     * @param x the column coordinate
     * @param y the row coordinate
     * @return the new value for plasma[x][y]
     */
    private int burn(final int x, final int y) {
        int sum = 0;
        if (x > 0) {
            if (y > 0) {
                sum += plasma[x - 1][y - 1];
            }
            sum += plasma[x - 1][y];
            if (y < rows - 1) {
                sum += plasma[x - 1][y + 1];
            }
        }
        sum += plasma[x][y] - 1;
        if (x < columns - 1) {
            if (y > 0) {
                sum += plasma[x + 1][y - 1];
            }
            sum += plasma[x + 1][y];
            if (y < rows - 1) {
                sum += plasma[x + 1][y + 1];
            }
        }
        sum /= 2;
        if (sum >= PALETTE.length - 2) {
            sum = Math.max(0, Math.min(sum - 2, plasma[x][y]));
        }
        sum = Math.min(sum, PALETTE.length - 1);
        return sum;
    }

    /**
     * If true, the effect is completed and can be removed.
     *
     * @return true if this effect is finished
     */
    public boolean isCompleted() {
        boolean done = true;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                if (plasma[x][y] > 1) {
                    done = false;
                }
            }
        }
        if (done) {
            if (dummyWindow != null) {
                dummyWindow.close();
                dummyWindow = null;
            }
        }
        return done;
    }

    // ------------------------------------------------------------------------
    // WindowTransitionEffect -------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The draw function called by the DummyScreen.
     */
    @Override
    protected void drawEffect() {
        assert (dummyWindow != null);
        assert (!dummyWindow.getApplication().isModalThreadRunning());

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
                if (idx < PALETTE.length / 2) {
                    ch = ' ';
                }
                if (idx > 3) {
                    screen.putCharXY(x + x0, y + y0, ch, attr);
                } else if (!fade) {
                    screen.putCharXY(x + x0, y + y0,
                        oldScreen.getCharXY(x + x0, y + y0));
                }
            }
        }
    }

}
