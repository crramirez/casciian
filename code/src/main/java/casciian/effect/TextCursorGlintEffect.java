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

import casciian.backend.Screen;

/**
 * Put a bit of bling around the text cursor, if it is visible.
 */
public class TextCursorGlintEffect implements Effect {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The screen to draw on.
     */
    private Screen screen;

    /**
     * The time first run.
     */
    private long firstBlingTime = -1;

    /**
     * The time last run.
     */
    private long blingTime = -1;

    /**
     * The increment to change bling location, in millis.
     */
    private int stepMillis = 32;

    /**
     * The total time to run the effect for.
     */
    private int maxMillis = 1024;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public contructor.
     *
     * @param screen the screen to draw on
     */
    public TextCursorGlintEffect(final Screen screen) {
        this.screen = screen;
    }

    // ------------------------------------------------------------------------
    // Effect -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Update the effect.
     */
    public void update() {
        if (!screen.isCursorVisible()) {
            return;
        }
        long nowTime = System.currentTimeMillis();
        if (firstBlingTime < 0) {
            firstBlingTime = nowTime;
        }
        if (nowTime - blingTime > stepMillis) {
            blingTime = nowTime;
        }

        long totalMillis = blingTime - firstBlingTime;
        double theta = 2.0 * Math.PI * (totalMillis / 1024.0);
        double r = 9.0 * (maxMillis - totalMillis) / maxMillis;
        r = Math.max(r, 2);

        int x0 = screen.getCursorX();
        int y0 = screen.getCursorY();

        // Invert several cells around the text cursor.  The r distance in
        // the Y direction is half that in the X direction.  Note that the
        // screen Y axis goes top-down, which is reverse of classic trig
        // coordinates, hence the negation on y1.

        double [] piFractions = { 0.0, 0.333, 0.666, 1.0, 1.333, 1.666 };
        for (int i = 0; i < piFractions.length; i++) {
            double angle = theta + (piFractions[i] * Math.PI);
            int x1 = (int) (r * Math.cos(angle));
            int y1 = -(int) (r * Math.sin(angle) / 2.0);
            screen.invertCell(x0 + x1, y0 + y1);
        }
    }

    /**
     * If true, the effect is completed and can be removed.
     *
     * @return true if this effect is finished
     */
    public boolean isCompleted() {
        if (!screen.isCursorVisible()) {
            return true;
        }
        if (firstBlingTime < 0) {
            return false;
        }
        return ((blingTime - firstBlingTime) > maxMillis);
    }

}
