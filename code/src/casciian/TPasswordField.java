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
import casciian.bits.StringUtils;

/**
 * TPasswordField implements an editable text field that displays
 * stars/asterisks when it is not active.
 */
public class TPasswordField extends TField {

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width visible text width
     * @param fixed if true, the text cannot exceed the display width
     */
    public TPasswordField(final TWidget parent, final int x, final int y,
        final int width, final boolean fixed) {

        this(parent, x, y, width, fixed, "", null, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width visible text width
     * @param fixed if true, the text cannot exceed the display width
     * @param text initial text, default is empty string
     */
    public TPasswordField(final TWidget parent, final int x, final int y,
        final int width, final boolean fixed, final String text) {

        this(parent, x, y, width, fixed, text, null, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width visible text width
     * @param fixed if true, the text cannot exceed the display width
     * @param text initial text, default is empty string
     * @param enterAction function to call when enter key is pressed
     * @param updateAction function to call when the text is updated
     */
    public TPasswordField(final TWidget parent, final int x, final int y,
        final int width, final boolean fixed, final String text,
        final TAction enterAction, final TAction updateAction) {

        // Set parent and window
        super(parent, x, y, width, fixed, text, enterAction, updateAction);
    }

    // ------------------------------------------------------------------------
    // TField -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the text field.
     */
    @Override
    public void draw() {
        CellAttributes fieldColor;

        boolean showStars = false;
        if (isAbsoluteActive()) {
            fieldColor = getTheme().getColor("tfield.active");
        } else {
            fieldColor = getTheme().getColor("tfield.inactive");
            showStars = true;
        }

        int end = windowStart + getWidth();
        if (end > StringUtils.width(text)) {
            end = StringUtils.width(text);
        }

        hLineXY(0, 0, getWidth(), backgroundChar, fieldColor);
        if (showStars) {
            hLineXY(0, 0, getWidth() - 2, '*', fieldColor);
        } else {
            putStringXY(0, 0, text.substring(screenToTextPosition(windowStart),
                    screenToTextPosition(end)), fieldColor);
        }

        // Fix the cursor, it will be rendered by TApplication.drawAll().
        updateCursor();
    }

}
