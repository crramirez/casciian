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
package casciian.bits;

import casciian.TWidget;
import casciian.backend.Backend;
import casciian.bits.Cell;

/**
 * A CellTransform is a function applied to a cell and its location.
 */
public interface CellTransform {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // CellTransform ----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * This method is called once before applyTransform() is called on every
     * cell of the widget or screen, providing an opportunity to reduce
     * computations in applyTransform().
     *
     * @param backend the backend that will be passed to applyTransform()
     * @param widget the widget that will be passed to applyTransform()
     */
    public void prepareTransform(final Backend backend, final TWidget widget);

    /**
     * Perform some kind of change to a cell, based on its location relative
     * to a widget or the entire screen.
     *
     * @param backend the backend that can obtain the correct foreground or
     * background color of the cell
     * @param cell the cell to alter
     * @param x column of the cell.  0 is the left-most column.
     * @param y row of the cell.  0 is the top-most row.
     * @param widget the widget this cell is on, or null if the transform is
     * relative to the entire screen
     */
    public void applyTransform(final Backend backend, final Cell cell,
        final int x, final int y, final TWidget widget);
    
}
