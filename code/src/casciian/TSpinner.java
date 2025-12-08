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
package casciian;

import casciian.bits.CellAttributes;
import casciian.bits.GraphicsChars;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import static casciian.TKeypress.*;

/**
 * TSpinner implements a simple up/down spinner.
 */
public class TSpinner extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The action to perform when the user clicks on the up arrow.
     */
    private TAction upAction = null;

    /**
     * The action to perform when the user clicks on the down arrow.
     */
    private TAction downAction = null;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param upAction action to call when the up arrow is clicked or pressed
     * @param downAction action to call when the down arrow is clicked or
     * pressed
     */
    public TSpinner(final TWidget parent, final int x, final int y,
        final TAction upAction, final TAction downAction) {

        // Set parent and window
        super(parent, x, y, 2, 1);

        this.upAction = upAction;
        this.downAction = downAction;
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Returns true if the mouse is currently on the up arrow.
     *
     * @param mouse mouse event
     * @return true if the mouse is currently on the up arrow
     */
    private boolean mouseOnUpArrow(final TMouseEvent mouse) {
        if ((mouse.getY() == 0)
            && (mouse.getX() == getWidth() - 2)
        ) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if the mouse is currently on the down arrow.
     *
     * @param mouse mouse event
     * @return true if the mouse is currently on the down arrow
     */
    private boolean mouseOnDownArrow(final TMouseEvent mouse) {
        if ((mouse.getY() == 0)
            && (mouse.getX() == getWidth() - 1)
        ) {
            return true;
        }
        return false;
    }

    /**
     * Handle mouse checkbox presses.
     *
     * @param mouse mouse button down event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if ((mouseOnUpArrow(mouse)) && (mouse.isMouse1())) {
            up();
        } else if ((mouseOnDownArrow(mouse)) && (mouse.isMouse1())) {
            down();
        }
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.equals(kbUp)) {
            up();
            return;
        }
        if (keypress.equals(kbDown)) {
            down();
            return;
        }

        // Pass to parent for the things we don't care about.
        super.onKeypress(keypress);
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the spinner arrows.
     */
    @Override
    public void draw() {
        CellAttributes spinnerColor;

        if (isAbsoluteActive()) {
            spinnerColor = getTheme().getColor("tspinner.active");
        } else {
            spinnerColor = getTheme().getColor("tspinner.inactive");
        }

        putCharXY(getWidth() - 2, 0, GraphicsChars.UPARROW, spinnerColor);
        putCharXY(getWidth() - 1, 0, GraphicsChars.DOWNARROW, spinnerColor);
    }

    // ------------------------------------------------------------------------
    // TSpinner ---------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Perform the "up" action.
     */
    private void up() {
        if (upAction != null) {
            upAction.DO(this);
        }
    }

    /**
     * Perform the "down" action.
     */
    private void down() {
        if (downAction != null) {
            downAction.DO(this);
        }
    }

}
