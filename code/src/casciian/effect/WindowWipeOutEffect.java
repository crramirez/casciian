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
import casciian.event.TInputEvent;

/**
 * Make the window wipe out from a side.
 */
public class WindowWipeOutEffect extends WindowTransitionEffect {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Direction of wipe.
     */
    public enum Direction {

        /**
         * Clear the window from bottom-to-top.
         */
        UP,

        /**
         * Clear the window from top-to-bottom.
         */
        DOWN,

        /**
         * Clear the window from right-to-left.
         */
        LEFT,

        /**
         * Clear the window from left-to-right.
         */
        RIGHT,

    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The direction to wipe out.
     */
    private Direction direction;

    /**
     * The current column or row for the wipe effect.
     */
    private int start = 0;

    /**
     * The end column or row for the wipe effect.
     */
    private int end = 0;

    /**
     * Step size between updates.
     */
    private int step = 1;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public contructor.
     *
     * @param window the window to wipe out
     * @param direction the direction of the wipe effect
     */
    public WindowWipeOutEffect(final TWindow window,
        final Direction direction) {

        super(window);

        this.direction = direction;

        switch (direction) {
        case UP:
        case DOWN:
            end = window.getHeight() + 1;
            step = 6;
            break;
        case LEFT:
        case RIGHT:
            end = window.getWidth() + 2;
            step = 12;
            break;
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
            start = end;
            return;
        }
        start = Math.min(start + step, end);
    }

    /**
     * If true, the effect is completed and can be removed.
     *
     * @return true if this effect is finished
     */
    public boolean isCompleted() {
        if (start >= end) {
            if (dummyWindow != null) {
                dummyWindow.close();
                dummyWindow = null;
            }
            return true;
        }
        return false;
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

        int width = dummyWindow.getEffectedWindow().getWidth() + 2;
        int height = dummyWindow.getEffectedWindow().getHeight() + 1;
        Screen oldScreen = dummyWindow.getOldScreen();
        Screen screen = dummyWindow.getScreen();

        int xStart = 0;
        int yStart = 0;
        int xEnd = width;
        int yEnd = height;
        int x0 = dummyWindow.getEffectedWindow().getX();
        int y0 = dummyWindow.getEffectedWindow().getY();

        switch (direction) {
        case UP:
            yEnd = (height - start);
            break;
        case DOWN:
            yStart = start;
            break;
        case LEFT:
            xEnd = (width - start);
            break;
        case RIGHT:
            xStart = start;
            break;
        }

        screen.resetClipping();
        for (int y = yStart; y < yEnd; y++) {
            for (int x = xStart; x < xEnd; x++) {
                screen.putCharXY(x + x0, y + y0,
                    oldScreen.getCharXY(x + x0, y + y0));
            }
        }
    }

}
