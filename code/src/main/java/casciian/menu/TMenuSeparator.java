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
package casciian.menu;

import casciian.bits.BorderStyle;
import casciian.bits.CellAttributes;
import casciian.bits.GraphicsChars;

/**
 * TMenuSeparator is a special case menu item.
 */
public class TMenuSeparator extends TMenuItem {

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Package private constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     */
    TMenuSeparator(final TMenu parent, final int x, final int y) {
        super(parent, TMenu.MID_UNUSED, x, y, "");
        setEnabled(false);
        setActive(false);
        setWidth(parent.getWidth() - 2);
    }

    // ------------------------------------------------------------------------
    // TMenuItem --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw a menu separator.
     */
    @Override
    public void draw() {
        CellAttributes background = getTheme().getColor("tmenu");

        BorderStyle borderStyle = ((TMenu) getParent()).getBorderStyle();
        int cHSide = GraphicsChars.SINGLE_BAR;
        int left = GraphicsChars.CP437[0xC3];
        int right = GraphicsChars.CP437[0xB4];
        if (borderStyle.getVertical() == GraphicsChars.WINDOW_SIDE_DOUBLE) {
            left = 0x255F;
            right = 0x2562;
        }
        if (borderStyle.equals(BorderStyle.NONE)) {
            left = ' ';
            right = ' ';
        }

        putCharXY(0, 0, left, background);
        putCharXY(getWidth() - 1, 0, right, background);
        hLineXY(1, 0, getWidth() - 2, cHSide, background);
    }

}
