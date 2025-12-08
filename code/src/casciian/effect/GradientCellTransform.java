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

import java.awt.Color;
import casciian.TWidget;
import casciian.backend.Backend;
import casciian.bits.Cell;
import casciian.bits.CellTransform;

/**
 * GradientCellTransform smoothly changes foreground and/or background color
 * in a rectangular region.
 */
public class GradientCellTransform implements CellTransform {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Layer(s) to apply the gradient effect to.
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
     * Layer(s) to apply gradient effect to.
     */
    private Layer layer = Layer.BACKGROUND;

    /**
     * Color at the top-left corner.
     */
    private Color topLeft = null;

    /**
     * Color at the top-right corner.
     */
    private Color topRight = null;

    /**
     * Color at the bottom-left corner.
     */
    private Color bottomLeft = null;

    /**
     * Color at the bottom-right corner.
     */
    private Color bottomRight = null;

    // applyTransform() cached values.
    private int width = 1;
    private int height = 1;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param layer layer to apply gradient effect to
     * @param topLeft color at the top-left corner
     * @param topRight color at the top-right corner
     * @param bottomLeft color at the bottom-left corner
     * @param bottomRight color at the bottom-right corner
     */
    public GradientCellTransform(final Layer layer,
        final Color topLeft, final Color topRight,
        final Color bottomLeft, final Color bottomRight) {

        this.layer       = layer;
        this.topLeft     = topLeft;
        this.topRight    = topRight;
        this.bottomLeft  = bottomLeft;
        this.bottomRight = bottomRight;

        processCorners();
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
        if (widget != null) {
            width = widget.getWidth();
            height = widget.getHeight();
        } else {
            width = backend.getScreen().getWidth();
            height = backend.getScreen().getHeight();
        }
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

        double rightWeight = (double) x / width;
        double bottomWeight = (double) y / height;

        double topLeftWeight = (1.0 - rightWeight) * (1.0 - bottomWeight);
        double topRightWeight = rightWeight * (1.0 - bottomWeight);
        double bottomLeftWeight = (1.0 - rightWeight) * bottomWeight;
        double bottomRightWeight = rightWeight * bottomWeight;

        int   red = (int) ((topLeft.getRed() * topLeftWeight)
                         + (topRight.getRed() * topRightWeight)
                         + (bottomLeft.getRed() * bottomLeftWeight)
                         + (bottomRight.getRed() * bottomRightWeight));
        int green = (int) ((topLeft.getGreen() * topLeftWeight)
                         + (topRight.getGreen() * topRightWeight)
                         + (bottomLeft.getGreen() * bottomLeftWeight)
                         + (bottomRight.getGreen() * bottomRightWeight));
        int  blue = (int) ((topLeft.getBlue() * topLeftWeight)
                         + (topRight.getBlue() * topRightWeight)
                         + (bottomLeft.getBlue() * bottomLeftWeight)
                         + (bottomRight.getBlue() * bottomRightWeight));

        int rgb = (0xFF << 24) | (red << 16) | (green << 8) | blue;
        if (foreground) {
            cell.setForeColorRGB(rgb);
        }
        if (background) {
            cell.setBackColorRGB(rgb);
        }
    }

    // ------------------------------------------------------------------------
    // GradientCellTransform --------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set any missing corner colors based on provided corner colors.
     */
    private void processCorners() {
        // If only one color is set, all four corners will be that color.
        Color oneColor = null;
        int colorCount = 0;
        if (topLeft != null) {
            oneColor = topLeft;
            colorCount++;
        }
        if (topRight != null) {
            oneColor = topRight;
            colorCount++;
        }
        if (bottomLeft != null) {
            oneColor = bottomLeft;
            colorCount++;
        }
        if (bottomRight != null) {
            oneColor = bottomRight;
            colorCount++;
        }

        if (colorCount == 4) {
            // All colors specified: OK
            return;
        }
        assert (colorCount > 0);

        if (colorCount == 1) {
            // One color specified: no gradient
            topLeft = oneColor;
            topRight = oneColor;
            bottomLeft = oneColor;
            bottomRight = oneColor;
            return;
        }
        if (colorCount == 3) {
            if (topLeft == null) {
                topLeft = colorAverage(bottomLeft, topRight);
            }
            if (topRight == null) {
                topRight = colorAverage(bottomLeft, topLeft);
            }
            if (bottomLeft == null) {
                bottomLeft = colorAverage(topLeft, bottomRight);
            }
            if (bottomRight == null) {
                bottomRight = colorAverage(bottomLeft, topLeft);
            }
            return;
        }
        assert (colorCount == 2);

        if ((topLeft == null) && (topRight == null)) {
            topLeft = bottomLeft;
            topRight = bottomRight;
            return;
        }
        if ((bottomLeft == null) && (bottomRight == null)) {
            bottomLeft = topLeft;
            bottomRight = topRight;
            return;
        }
        if ((topLeft == null) && (bottomLeft == null)) {
            topLeft = topRight;
            bottomLeft = bottomRight;
            return;
        }
        if ((topRight == null) && (bottomRight == null)) {
            topRight = topLeft;
            bottomRight = bottomLeft;
            return;
        }
        if ((topLeft == null) && (bottomRight == null)) {
            topLeft = colorAverage(topRight, bottomLeft);
            bottomRight = topLeft;
            return;
        }
        if ((topRight == null) && (bottomLeft == null)) {
            topRight = colorAverage(topLeft, bottomRight);
            bottomLeft = topRight;
            return;
        }
    }

    /**
     * Compute an average color from two colors.
     *
     * @param color1 the first color
     * @param color2 the second color
     */
    private Color colorAverage(final Color color1, final Color color2) {
        return new Color((color1.getRed() + color2.getRed() / 2),
            (color1.getGreen() + color2.getGreen() / 2),
            (color1.getBlue() + color2.getBlue() / 2));
    }

}
