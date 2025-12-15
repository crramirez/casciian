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
    private int topLeft = -1;

    /**
     * Color at the top-right corner.
     */
    private int topRight = -1;

    /**
     * Color at the bottom-left corner.
     */
    private int bottomLeft = -1;

    /**
     * Color at the bottom-right corner.
     */
    private int bottomRight = -1;

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
        final int topLeft, final int topRight,
        final int bottomLeft, final int bottomRight) {

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

        int topLeftRed     = (    topLeft >>> 16) & 0xFF;
        int topRightRed    = (   topRight >>> 16) & 0xFF;
        int bottomLeftRed  = ( bottomLeft >>> 16) & 0xFF;
        int bottomRightRed = (bottomRight >>> 16) & 0xFF;
        int topLeftGreen     = (    topLeft >>> 8) & 0xFF;
        int topRightGreen    = (   topRight >>> 8) & 0xFF;
        int bottomLeftGreen  = ( bottomLeft >>> 8) & 0xFF;
        int bottomRightGreen = (bottomRight >>> 8) & 0xFF;
        int topLeftBlue     =     topLeft & 0xFF;
        int topRightBlue    =    topRight & 0xFF;
        int bottomLeftBlue  =  bottomLeft & 0xFF;
        int bottomRightBlue = bottomRight & 0xFF;

        int   red = (int) ((topLeftRed * topLeftWeight)
                         + (topRightRed * topRightWeight)
                         + (bottomLeftRed * bottomLeftWeight)
                         + (bottomRightRed * bottomRightWeight));
        int green = (int) ((topLeftGreen * topLeftWeight)
                         + (topRightGreen * topRightWeight)
                         + (bottomLeftGreen * bottomLeftWeight)
                         + (bottomRightGreen * bottomRightWeight));
        int  blue = (int) ((topLeftBlue * topLeftWeight)
                         + (topRightBlue * topRightWeight)
                         + (bottomLeftBlue * bottomLeftWeight)
                         + (bottomRightBlue * bottomRightWeight));

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
        int oneColor = -1;
        int colorCount = 0;
        if (topLeft != -1) {
            oneColor = topLeft;
            colorCount++;
        }
        if (topRight != -1) {
            oneColor = topRight;
            colorCount++;
        }
        if (bottomLeft != -1) {
            oneColor = bottomLeft;
            colorCount++;
        }
        if (bottomRight != -1) {
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
            if (topLeft == -1) {
                topLeft = colorAverage(bottomLeft, topRight);
            }
            if (topRight == -1) {
                topRight = colorAverage(bottomLeft, topLeft);
            }
            if (bottomLeft == -1) {
                bottomLeft = colorAverage(topLeft, bottomRight);
            }
            if (bottomRight == -1) {
                bottomRight = colorAverage(bottomLeft, topLeft);
            }
            return;
        }
        assert (colorCount == 2);

        if ((topLeft == -1) && (topRight == -1)) {
            topLeft = bottomLeft;
            topRight = bottomRight;
            return;
        }
        if ((bottomLeft == -1) && (bottomRight == -1)) {
            bottomLeft = topLeft;
            bottomRight = topRight;
            return;
        }
        if ((topLeft == -1) && (bottomLeft == -1)) {
            topLeft = topRight;
            bottomLeft = bottomRight;
            return;
        }
        if ((topRight == -1) && (bottomRight == -1)) {
            topRight = topLeft;
            bottomRight = bottomLeft;
            return;
        }
        if ((topLeft == -1) && (bottomRight == -1)) {
            topLeft = colorAverage(topRight, bottomLeft);
            bottomRight = topLeft;
            return;
        }
        if ((topRight == -1) && (bottomLeft == -1)) {
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
    private int colorAverage(final int color1, final int color2) {
        int color1Red   = (    color1 >>> 16) & 0xFF;
        int color2Red   = (    color2 >>> 16) & 0xFF;
        int color1Green = (    color1 >>> 8) & 0xFF;
        int color2Green = (    color2 >>> 8) & 0xFF;
        int color1Blue  =      color1 & 0xFF;
        int color2Blue  =      color2 & 0xFF;


        int red   = (color1Red + color2Red) / 2;
        int green = (color1Green + color2Green) / 2;
        int blue  = (color1Blue + color2Blue) / 2;
        int rgb = (red << 16) | (green << 8) | blue;
        return rgb;
    }

}
