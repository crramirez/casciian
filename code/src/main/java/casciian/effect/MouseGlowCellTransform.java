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

import casciian.TWidget;
import casciian.backend.Backend;
import casciian.bits.Cell;
import casciian.bits.CellTransform;
import casciian.bits.ImageUtils;

/**
 * MouseGlowCellTransform smoothly changes foreground and/or background color
 * in an elliptical region around the location of the mouse.
 */
public class MouseGlowCellTransform implements CellTransform {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Layer(s) to apply the mouse glow effect to.
     */
    public enum Layer {
        /**
         * Foreground color only.
         */
        FOREGROUND,

        /**
         * Background color only.
         */
        BACKGROUND,

        /**
         * Both foreground and background colors.
         */
        BOTH,

    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Layer(s) to apply mouse glow effect to.
     */
    private Layer layer = Layer.BACKGROUND;

    /**
     * RGB color to use for the glow effect.
     */
    private int glowColor = -1;

    /*
     * Maximum horizontal cell distance to have the glow
     */
    private int glowDistance = 5;

    // applyTransform() cached values.
    private int mouseX = -1;
    private int mouseY = -1;
    private int absoluteX = -1;
    private int absoluteY = -1;
    private int minX = -1;
    private int maxX = -1;
    private int minY = -1;
    private int maxY = -1;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param layer layer to apply glow effect to
     * @param glowColor RGB color of the glow
     * @param glowDistance maximum horizontal cell distance to have the glow
     * effect
     */
    public MouseGlowCellTransform(final Layer layer, final int glowColor,
        final int glowDistance) {

        this.layer        = layer;
        this.glowColor    = glowColor;
        this.glowDistance = glowDistance;
    }

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
    public void prepareTransform(final Backend backend, final TWidget widget) {
        mouseX = widget.getApplication().getMouseX();
        mouseY = widget.getApplication().getMouseY();
        absoluteX = widget.getAbsoluteX();
        absoluteY = widget.getAbsoluteY();
        minX = mouseX - glowDistance;
        maxX = mouseX + glowDistance;
        minY = mouseY - (glowDistance / 2);
        maxY = mouseY + (glowDistance / 2);
    }

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
    @Override
    public void applyTransform(final Backend backend, final Cell cell,
        final int x, final int y, final TWidget widget) {

        if (glowDistance < 1) {
            return;
        }

        boolean foreground = true;
        boolean background = true;
        switch (layer) {
        case FOREGROUND:
            background = false;
            break;
        case BACKGROUND:
            foreground = false;
            break;
        default:
            break;
        }

        int x0 = x;
        int y0 = y;
        if (widget != null) {
            x0 += absoluteX;
            y0 += absoluteY;
        }

        if ((x0 < minX)
            || (x0 > maxX)
            || (y0 < minY)
            || (y0 > maxY)
        ) {
            return;
        }

        int diffX = mouseX - x0;
        int diffY = mouseY - y0;
        if ((diffX == 0) && (diffY == 0)) {
            return;
        }

        int distance = (int) Math.sqrt((diffX * diffX) +
            (diffY * diffY) * glowDistance);
        if ((Math.abs(diffX) > glowDistance)
            || (Math.abs(diffY) > (glowDistance / 2))
        ) {
            return;
        }

        double brightFraction = 1.0 - ((double) distance / glowDistance);
        int glowRgb = glowColor;

        if (foreground) {
            int foreColorRgb = backend.attrToForegroundColor(cell);
            cell.setForeColorRGB(ImageUtils.rgbMove(foreColorRgb, glowRgb,
                    brightFraction));
        }

        if (background) {
            int backColorRgb = backend.attrToBackgroundColor(cell);
            cell.setBackColorRGB(ImageUtils.rgbMove(backColorRgb, glowRgb,
                    brightFraction));
        }
    }

    // ------------------------------------------------------------------------
    // MouseGlowCellTransform -------------------------------------------------
    // ------------------------------------------------------------------------

}
