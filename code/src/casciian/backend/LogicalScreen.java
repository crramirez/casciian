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
package casciian.backend;

import casciian.TWidget;
import casciian.bits.BorderStyle;
import casciian.bits.Cell;
import casciian.bits.CellAttributes;
import casciian.bits.CellTransform;
import casciian.bits.ComplexCell;
import casciian.bits.Clipboard;
import casciian.bits.GraphicsChars;
import casciian.bits.ImageUtils;
import casciian.bits.StringUtils;

/**
 * A logical screen composed of a 2D array of Cells.
 */
public class LogicalScreen implements Screen {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * A simple 2D RGB array.
     */
    private class BufferedImage {

        /**
         * The 24-bit RGB data.
         */
        private int [][] rgb;

        /**
         * The width of this image.
         */
        private int width;

        /**
         * The height of this image.
         */
        private int height;

        /**
         * Public constructor.
         *
         * @param width the number of pixels in width
         * @param height the number of pixels in height
         */
        public BufferedImage(final int width, final int height) {
            this.width = width;
            this.height = height;
            rgb = new int[width][height];
        }

        /**
         * Public constructor.
         *
         * @param image another BufferedImage that the RGB data will be
         * copied from
         */
        public BufferedImage(final BufferedImage image) {
            width = image.width;
            height = image.height;
            rgb = new int[width][height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    this.rgb[x][y] = image.rgb[x][y];
                }
            }
        }

        /**
         * Get an RGB value.
         *
         * @param x the column location
         * @param y the row location
         * @return the RGB value
         */
        public int getRGB(final int x, final int y) {
            return rgb[x][y];
        }

        /**
         * Set an RGB value.
         *
         * @param x the column location
         * @param y the row location
         * @param rgb the new RGB value
         */
        public void setRGB(final int x, final int y, final int rgb) {
            this.rgb[x][y] = rgb;
        }

        /**
         * Alpha-blend another image over this one.
         *
         * @param image the other image
         * @param alpha a number between 0 and 1
         */
        public void alphaBlendOver(final BufferedImage image,
            final double alpha) {

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int underRGB = rgb[x][y];
                    int overRGB = image.rgb[x][y];
                    int underRed   = (underRGB >>> 16) & 0xFF;
                    int underGreen = (underRGB >>>  8) & 0xFF;
                    int underBlue  =  underRGB         & 0xFF;
                    int overRed    = (overRGB >>> 16) & 0xFF;
                    int overGreen  = (overRGB >>>  8) & 0xFF;
                    int overBlue   =  overRGB         & 0xFF;
                    int red   = (int) ((  underRed * (1.0 - alpha)) + (  overRed * alpha));
                    int green = (int) ((underGreen * (1.0 - alpha)) + (overGreen * alpha));
                    int blue  = (int) (( underBlue * (1.0 - alpha)) + ( overBlue * alpha));
                    int newRgb = (red << 16) | (green << 8) | blue;
                    this.rgb[x][y] = newRgb;
                }
            }
        }

    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The backend associated with this screen.
     */
    private Backend backend;

    /**
     * Width of the visible window.
     */
    protected int width;

    /**
     * Height of the visible window.
     */
    protected int height;

    /**
     * Drawing offset for x.
     */
    private int offsetX;

    /**
     * Drawing offset for y.
     */
    private int offsetY;

    /**
     * Ignore anything drawn right of clipRight.
     */
    private int clipRight;

    /**
     * Ignore anything drawn below clipBottom.
     */
    private int clipBottom;

    /**
     * Ignore anything drawn left of clipLeft.
     */
    private int clipLeft;

    /**
     * Ignore anything drawn above clipTop.
     */
    private int clipTop;

    /**
     * Ignore anything drawn left of relativeClipLeft minus offsetX.
     */
    private int relativeClipLeft;

    /**
     * Ignore anything drawn above relativeClipTop minus offsetY.
     */
    private int relativeClipTop;

    /**
     * The physical screen last sent out on flush().
     */
    protected ComplexCell [][] physical;

    /**
     * The logical screen being rendered to.
     */
    protected ComplexCell [][] logical;

    /**
     * Set if the user explicitly wants to redraw everything starting with a
     * ECMATerminal.clearAll().
     */
    protected boolean reallyCleared;

    /**
     * If true, the cursor is visible and should be placed onscreen at
     * (cursorX, cursorY) during a call to flushPhysical().
     */
    protected boolean cursorVisible;

    /**
     * Cursor X position if visible.
     */
    protected int cursorX;

    /**
     * Cursor Y position if visible.
     */
    protected int cursorY;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.  Sets everything to not-bold, white-on-black.
     */
    protected LogicalScreen() {
        this(80, 24);
    }

    /**
     * Public constructor.  Sets everything to not-bold, white-on-black.
     *
     * @param width width in cells
     * @param height height in cells
     */
    protected LogicalScreen(final int width, final int height) {
        offsetX     = 0;
        offsetY     = 0;
        this.width  = 80;
        this.height = 24;
        logical     = null;
        physical    = null;
        reallocate(width, height);
    }

    // ------------------------------------------------------------------------
    // Screen -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the width of a character cell in pixels.
     *
     * @return the width in pixels of a character cell
     */
    public int getTextWidth() {
        // Default width is 16 pixels.
        return 16;
    }

    /**
     * Get the height of a character cell in pixels.
     *
     * @return the height in pixels of a character cell
     */
    public int getTextHeight() {
        // Default height is 20 pixels.
        return 20;
    }

    /**
     * Set drawing offset for x.
     *
     * @param offsetX new drawing offset
     */
    public final void setOffsetX(final int offsetX) {
        this.offsetX = offsetX;
    }

    /**
     * Get drawing offset for x.
     *
     * @return the drawing offset
     */
    public int getOffsetX() {
        return offsetX;
    }

    /**
     * Set drawing offset for y.
     *
     * @param offsetY new drawing offset
     */
    public final void setOffsetY(final int offsetY) {
        this.offsetY = offsetY;
    }

    /**
     * Get drawing offset for y.
     *
     * @return the drawing offset
     */
    public int getOffsetY() {
        return offsetY;
    }

    /**
     * Get right drawing clipping boundary.
     *
     * @return drawing boundary
     */
    public final int getClipRight() {
        return clipRight;
    }

    /**
     * Set right drawing clipping boundary.
     *
     * @param clipRight new boundary
     */
    public final void setClipRight(final int clipRight) {
        this.clipRight = clipRight;
    }

    /**
     * Get bottom drawing clipping boundary.
     *
     * @return drawing boundary
     */
    public final int getClipBottom() {
        return clipBottom;
    }

    /**
     * Set bottom drawing clipping boundary.
     *
     * @param clipBottom new boundary
     */
    public final void setClipBottom(final int clipBottom) {
        this.clipBottom = clipBottom;
    }

    /**
     * Get left drawing clipping boundary.
     *
     * @return drawing boundary
     */
    public final int getClipLeft() {
        return clipLeft;
    }

    /**
     * Set left drawing clipping boundary.
     *
     * @param clipLeft new boundary
     */
    public final void setClipLeft(final int clipLeft) {
        this.clipLeft = clipLeft;
    }

    /**
     * Get top drawing clipping boundary.
     *
     * @return drawing boundary
     */
    public final int getClipTop() {
        return clipTop;
    }

    /**
     * Set top drawing clipping boundary.
     *
     * @param clipTop new boundary
     */
    public final void setClipTop(final int clipTop) {
        this.clipTop = clipTop;
    }

    /**
     * Set left drawing clipping boundary relative to offsetX.
     *
     * @param relativeClipLeft new boundary
     */
    public final void setRelativeClipLeft(final int relativeClipLeft) {
        this.relativeClipLeft = relativeClipLeft;
    }

    /**
     * Set top drawing clipping boundary relative to offsetY.
     *
     * @param relativeClipTop new boundary
     */
    public final void setRelativeClipTop(final int relativeClipTop) {
        this.relativeClipTop = relativeClipTop;
    }

    /**
     * Get dirty flag.
     *
     * @return if true, the logical screen is not in sync with the physical
     * screen
     */
    public final boolean isDirty() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (logical[x][y].isBlink()) {
                    // Blinking screens are always dirty.  There is
                    // opportunity for a Netscape blink tag joke here...
                    return true;
                }
                synchronized (this) {
                    if (!logical[x][y].equals(physical[x][y])) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Get the attributes at one location.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @return attributes at (x, y)
     */
    public final CellAttributes getAttrXY(final int x, final int y) {
        CellAttributes attr = new CellAttributes();
        if ((x >= 0) && (x < width) && (y >= 0) && (y < height)) {
            attr.setTo(logical[x][y]);
        }
        return attr;
    }

    /**
     * Get the cell at one location in absolute coordinates.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @return the character + attributes
     */
    public Cell getCharXY(final int x, final int y) {
        ComplexCell cell = new ComplexCell();
        if ((x >= 0) && (x < width) && (y >= 0) && (y < height)) {
            cell.setTo(logical[x][y]);
        }
        return cell;
    }

    /**
     * Get the cell at one location, in either absolute or clipped
     * coordinates.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param clip if true, honor clipping/offset
     *
     * @return the character + attributes, or null if this position is
     * outside the clipping/offset region
     */
    public Cell getCharXY(final int x, final int y, final boolean clip) {
        int X = x;
        int Y = y;

        if (clip) {
            if ((x < clipLeft)
                || (x >= clipRight)
                || (y < clipTop)
                || (y >= clipBottom)
                || (x + offsetX < relativeClipLeft)
                || (y + offsetY < relativeClipTop)
            ) {
                return null;
            }
            X += offsetX;
            Y += offsetY;
        }

        if ((X >= 0) && (X < width) && (Y >= 0) && (Y < height)) {
            ComplexCell cell = new ComplexCell();
            cell.setTo(logical[X][Y]);
            return cell;
        }
        return null;
    }

    /**
     * Set the attributes at one location.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public final void putAttrXY(final int x, final int y,
        final CellAttributes attr) {

        putAttrXY(x, y, attr, true);
    }

    /**
     * Set the attributes at one location.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param attr attributes to use (bold, foreColor, backColor)
     * @param clip if true, honor clipping/offset
     */
    public final void putAttrXY(final int x, final int y,
        final CellAttributes attr, final boolean clip) {

        int X = x;
        int Y = y;

        if (clip) {
            if ((x < clipLeft)
                || (x >= clipRight)
                || (y < clipTop)
                || (y >= clipBottom)
                || (x + offsetX < relativeClipLeft)
                || (y + offsetY < relativeClipTop)
            ) {
                return;
            }
            X += offsetX;
            Y += offsetY;
        }

        if ((X >= 0) && (X < width) && (Y >= 0) && (Y < height)) {
            logical[X][Y].setAttr(attr);

            // If this happens to be the cursor position, make the position
            // dirty.
            if ((cursorX == X) && (cursorY == Y)) {
                synchronized (this) {
                    physical[cursorX][cursorY].unset();
                }
            }
        }
    }

    /**
     * Change the background color only of a box/region of the screen.
     *
     * @param left left column of the box.  0 is the left-most row.
     * @param top top row of the box.  0 is the top-most row.
     * @param right right column of box
     * @param bottom bottom row of the box
     * @param attr the background color to use
     */
    public void putBackgroundAttrBox(final int left, final int top,
        final int right, final int bottom, final CellAttributes attr) {

        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                putBackgroundAttrXY(x, y, attr, false);
            }
        }
    }

    /**
     * Set the background color only at one location.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param attr the background color to use
     */
    public void putBackgroundAttrXY(final int x, final int y,
        final CellAttributes attr) {

        putBackgroundAttrXY(x, y, attr, true);
    }

    /**
     * Set the background color only at one location.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param attr the background color to use
     * @param clip if true, honor clipping/offset
     */
    public void putBackgroundAttrXY(final int x, final int y,
        final CellAttributes attr, final boolean clip) {

        int X = x;
        int Y = y;

        if (clip) {
            if ((x < clipLeft)
                || (x >= clipRight)
                || (y < clipTop)
                || (y >= clipBottom)
                || (x + offsetX < relativeClipLeft)
                || (y + offsetY < relativeClipTop)
            ) {
                return;
            }
            X += offsetX;
            Y += offsetY;
        }

        if ((X >= 0) && (X < width) && (Y >= 0) && (Y < height)) {
            if (attr.getBackColorRGB() != -1) {
                logical[X][Y].setBackColorRGB(attr.getBackColorRGB());
            } else {
                logical[X][Y].setBackColor(attr.getBackColor());
            }

            // If this happens to be the cursor position, make the position
            // dirty.
            if ((cursorX == X) && (cursorY == Y)) {
                synchronized (this) {
                    physical[cursorX][cursorY].unset();
                }
            }
        }
    }

    /**
     * Fill the entire screen with one character with attributes.
     *
     * @param ch character to draw
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public final void putAll(final int ch, final CellAttributes attr) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                putCharXY(x, y, ch, attr);
            }
        }
    }

    /**
     * Fill the entire screen with one cell.
     *
     * @param ch the character to draw
     */
    public final void putAll(final Cell ch) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                putCharXY(x, y, ch);
            }
        }
    }

    /**
     * Render one character with attributes.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param ch character + attributes to draw
     */
    public final void putCharXY(final int x, final int y, final Cell ch) {
        putCharXY(x, y, ch, false);
    }

    /**
     * Render one character, using only the foreground attributes.  The
     * background color will not be changed.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param ch character to draw
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public void putForegroundCharXY(final int x, final int y, final int ch,
        final CellAttributes attr) {

       if ((x < clipLeft)
            || (x >= clipRight)
            || (y < clipTop)
            || (y >= clipBottom)
            || (x + offsetX < relativeClipLeft)
            || (y + offsetY < relativeClipTop)
        ) {
            return;
        }

        int X = x + offsetX;
        int Y = y + offsetY;

        if ((X >= 0) && (X < width) && (Y >= 0) && (Y < height)) {
            Cell cell = new Cell(ch, attr);
            CellAttributes backAttr = logical[X][Y];

            if (backAttr.getBackColorRGB() >= 0) {
                cell.setBackColorRGB(backAttr.getBackColorRGB());
            } else {
                cell.setBackColor(backAttr.getBackColor());
            }
            putCharXY(x, y, cell, false);
        }
    }

    /**
     * Render one character with attributes.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param ch character + attributes to draw
     */
    private final void putCharXY(final int x, final int y, final Cell ch,
        final boolean direct) {

        if ((x < clipLeft)
            || (x >= clipRight)
            || (y < clipTop)
            || (y >= clipBottom)
            || (x + offsetX < relativeClipLeft)
            || (y + offsetY < relativeClipTop)
        ) {
            return;
        }

        if ((ch.getDisplayWidth() == 2) && !direct) {
            if (ch instanceof ComplexCell) {
                putFullwidthCharXY(x, y, (ComplexCell) ch);
            } else {
                putFullwidthCharXY(x, y, new ComplexCell(ch));
            }
            return;
        }

        int X = x + offsetX;
        int Y = y + offsetY;

        // System.err.printf("putCharXY: %d, %d, %c\n", X, Y, ch);

        if ((X >= 0) && (X < width) && (Y >= 0) && (Y < height)) {

            // Do not put control characters on the display
            assert (ch.getChar() >= 0x20);
            assert (ch.getChar() != 0x7F);
            logical[X][Y].setTo(ch);

            // If this happens to be the cursor position, make the position
            // dirty.
            if ((cursorX == X) && (cursorY == Y)) {
                synchronized (this) {
                    physical[cursorX][cursorY].unset();
                }
            }
        }
    }

    /**
     * Render one character with attributes.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param ch character to draw
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public final void putCharXY(final int x, final int y, final int ch,
        final CellAttributes attr) {

        if ((x < clipLeft)
            || (x >= clipRight)
            || (y < clipTop)
            || (y >= clipBottom)
            || (x + offsetX < relativeClipLeft)
            || (y + offsetY < relativeClipTop)
        ) {
            return;
        }

        if (StringUtils.width(ch) == 2) {
            putFullwidthCharXY(x, y, ch, attr);
            return;
        }

        int X = x + offsetX;
        int Y = y + offsetY;

        // System.err.printf("putCharXY: %d, %d, %c\n", X, Y, ch);

        if ((X >= 0) && (X < width) && (Y >= 0) && (Y < height)) {

            // Do not put control characters on the display
            assert (ch >= 0x20);
            assert (ch != 0x7F);

            logical[X][Y].setTo(attr);
            logical[X][Y].setChar(ch);

            // If this happens to be the cursor position, make the position
            // dirty.
            if ((cursorX == X) && (cursorY == Y)) {
                synchronized (this) {
                    physical[cursorX][cursorY].unset();
                }
            }
        }
    }

    /**
     * Render one character without changing the underlying attributes.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param ch character to draw
     */
    public final void putCharXY(final int x, final int y, final int ch) {
        if ((x < clipLeft)
            || (x >= clipRight)
            || (y < clipTop)
            || (y >= clipBottom)
            || (x + offsetX < relativeClipLeft)
            || (y + offsetY < relativeClipTop)
        ) {
            return;
        }

        if (StringUtils.width(ch) == 2) {
            putFullwidthCharXY(x, y, ch);
            return;
        }

        int X = x + offsetX;
        int Y = y + offsetY;

        // System.err.printf("putCharXY: %d, %d, %c\n", X, Y, ch);

        if ((X >= 0) && (X < width) && (Y >= 0) && (Y < height)) {
            logical[X][Y].setChar(ch);

            // If this happens to be the cursor position, make the position
            // dirty.
            if ((cursorX == X) && (cursorY == Y)) {
                synchronized (this) {
                    physical[cursorX][cursorY].unset();
                }
            }
        }
    }

    /**
     * Render a string with attributes.  Does not wrap if the string exceeds
     * the line.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param str string to draw
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public final void putStringXY(final int x, final int y, final String str,
        final CellAttributes attr) {

        int i = x;
        for (int j = 0; j < str.length();) {
            int ch = str.codePointAt(j);
            j += Character.charCount(ch);
            putCharXY(i, y, ch, attr);
            i += StringUtils.width(ch);
            if (i == width) {
                break;
            }
        }
    }

    /**
     * Render a string, using only the foreground attributes.  The background
     * color will not be changed.  Does not wrap if the string exceeds the
     * line.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param str string to draw
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public void putForegroundStringXY(final int x, final int y,
        final String str, final CellAttributes attr) {

        int i = x;
        for (int j = 0; j < str.length();) {
            int ch = str.codePointAt(j);
            j += Character.charCount(ch);
            putForegroundCharXY(i, y, ch, attr);
            i += StringUtils.width(ch);
            if (i == width) {
                break;
            }
        }
    }

    /**
     * Render a string without changing the underlying attribute.  Does not
     * wrap if the string exceeds the line.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param str string to draw
     */
    public final void putStringXY(final int x, final int y, final String str) {

        int i = x;
        for (int j = 0; j < str.length();) {
            int ch = str.codePointAt(j);
            j += Character.charCount(ch);
            putCharXY(i, y, ch);
            i += StringUtils.width(ch);
            if (i == width) {
                break;
            }
        }
    }

    /**
     * Draw a vertical line from (x, y) to (x, y + n).
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param n number of characters to draw
     * @param ch character to draw
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public final void vLineXY(final int x, final int y, final int n,
        final int ch, final CellAttributes attr) {

        for (int i = y; i < y + n; i++) {
            putCharXY(x, i, ch, attr);
        }
    }

    /**
     * Draw a vertical line from (x, y) to (x, y + n), using only the
     * foreground attributes.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param n number of characters to draw
     * @param ch character to draw
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public void vForegroundLineXY(final int x, final int y, final int n,
        final int ch, final CellAttributes attr) {

        for (int i = y; i < y + n; i++) {
            putForegroundCharXY(x, i, ch, attr);
        }
    }

    /**
     * Draw a vertical line from (x, y) to (x, y + n).
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param n number of characters to draw
     * @param ch character to draw
     */
    public void vLineXY(final int x, final int y, final int n,
        final Cell ch) {

        for (int i = y; i < y + n; i++) {
            putCharXY(x, i, ch);
        }
    }

    /**
     * Draw a horizontal line from (x, y) to (x + n, y).
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param n number of characters to draw
     * @param ch character to draw
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public final void hLineXY(final int x, final int y, final int n,
        final int ch, final CellAttributes attr) {

        for (int i = x; i < x + n; i++) {
            putCharXY(i, y, ch, attr);
        }
    }

    /**
     * Draw a horizontal line from (x, y) to (x + n, y), using only the
     * foreground attributes.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param n number of characters to draw
     * @param ch character to draw
     * @param attr attributes to use (bold, foreColor)
     */
    public void hForegroundLineXY(final int x, final int y, final int n,
        final int ch, final CellAttributes attr) {

        for (int i = x; i < x + n; i++) {
            putForegroundCharXY(i, y, ch, attr);
        }
    }

    /**
     * Draw a horizontal line from (x, y) to (x + n, y).
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param n number of characters to draw
     * @param ch character to draw
     */
    public void hLineXY(final int x, final int y, final int n,
        final Cell ch) {

        for (int i = x; i < x + n; i++) {
            putCharXY(i, y, ch);
        }
    }

    /**
     * Change the width.  Everything on-screen will be destroyed and must be
     * redrawn.
     *
     * @param width new screen width
     */
    public synchronized void setWidth(final int width) {
        reallocate(width, this.height);
    }

    /**
     * Change the height.  Everything on-screen will be destroyed and must be
     * redrawn.
     *
     * @param height new screen height
     */
    public synchronized void setHeight(final int height) {
        reallocate(this.width, height);
    }

    /**
     * Change the width and height.  Everything on-screen will be destroyed
     * and must be redrawn.
     *
     * @param width new screen width
     * @param height new screen height
     */
    public void setDimensions(final int width, final int height) {
        if ((this.width == width) && (this.height == height)) {
            return;
        }
        reallocate(width, height);
        resizeToScreen();
    }

    /**
     * Resize the physical screen to match the logical screen dimensions.
     */
    public void resizeToScreen() {
        // Subclasses are expected to override this.
    }

    /**
     * Get the height.
     *
     * @return current screen height
     */
    public final synchronized int getHeight() {
        return this.height;
    }

    /**
     * Get the width.
     *
     * @return current screen width
     */
    public final synchronized int getWidth() {
        return this.width;
    }

    /**
     * Reset screen to not-bold, white-on-black.  Also flushes the offset and
     * clip variables.
     */
    public final synchronized void reset() {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                logical[col][row].reset();
            }
        }
        resetClipping();
    }

    /**
     * Flush the offset and clip variables.
     */
    public final void resetClipping() {
        offsetX    = 0;
        offsetY    = 0;
        clipLeft   = 0;
        clipTop    = 0;
        clipRight  = width;
        clipBottom = height;
        relativeClipLeft = 0;
        relativeClipTop  = 0;
    }

    /**
     * Clear the logical screen.
     */
    public final void clear() {
        reset();
    }

    /**
     * Draw a box with a border and empty background.
     *
     * @param left left column of box.  0 is the left-most column.
     * @param top top row of the box.  0 is the top-most row.
     * @param right right column of box
     * @param bottom bottom row of the box
     * @param border attributes to use for the border
     * @param background attributes to use for the background
     * @param borderStyle style of border
     * @param shadow if true, draw a "shadow" on the box
     */
    public void drawBox(final int left, final int top,
        final int right, final int bottom,
        final CellAttributes border, final CellAttributes background,
        final BorderStyle borderStyle, final boolean shadow) {

        int boxWidth = right - left;
        int boxHeight = bottom - top;

        int cTopLeft     = borderStyle.getTopLeft();
        int cTopRight    = borderStyle.getTopRight();
        int cBottomLeft  = borderStyle.getBottomLeft();
        int cBottomRight = borderStyle.getBottomRight();
        int cHSide       = borderStyle.getHorizontal();
        int cVSide       = borderStyle.getVertical();

        // Place the corner characters
        putCharXY(left, top, cTopLeft, border);
        putCharXY(left + boxWidth - 1, top, cTopRight, border);
        putCharXY(left, top + boxHeight - 1, cBottomLeft, border);
        putCharXY(left + boxWidth - 1, top + boxHeight - 1, cBottomRight,
            border);

        // Draw the box lines
        hLineXY(left + 1, top, boxWidth - 2, cHSide, border);
        vLineXY(left, top + 1, boxHeight - 2, cVSide, border);
        hLineXY(left + 1, top + boxHeight - 1, boxWidth - 2, cHSide, border);
        vLineXY(left + boxWidth - 1, top + 1, boxHeight - 2, cVSide, border);

        // Fill in the interior background
        for (int i = 1; i < boxHeight - 1; i++) {
            hLineXY(1 + left, i + top, boxWidth - 2, ' ', background);
        }

        if (shadow) {
            // Draw a shadow
            drawBoxShadow(left, top, right, bottom);
        }
    }

    /**
     * Draw a box with a border and empty background, using only the
     * foreground attributes.
     *
     * @param left left column of box.  0 is the left-most row.
     * @param top top row of the box.  0 is the top-most row.
     * @param right right column of box
     * @param bottom bottom row of the box
     * @param border attributes to use for the border
     * @param background attributes to use for the background
     * @param borderStyle style of border
     * @param shadow if true, draw a "shadow" on the box
     */
    public final void drawForegroundBox(final int left, final int top,
        final int right, final int bottom,
        final CellAttributes border, final CellAttributes background,
        final BorderStyle borderStyle, final boolean shadow) {

        int boxWidth = right - left;
        int boxHeight = bottom - top;

        int cTopLeft     = borderStyle.getTopLeft();
        int cTopRight    = borderStyle.getTopRight();
        int cBottomLeft  = borderStyle.getBottomLeft();
        int cBottomRight = borderStyle.getBottomRight();
        int cHSide       = borderStyle.getHorizontal();
        int cVSide       = borderStyle.getVertical();

        // Place the corner characters
        putForegroundCharXY(left, top, cTopLeft, border);
        putForegroundCharXY(left + boxWidth - 1, top, cTopRight, border);
        putForegroundCharXY(left, top + boxHeight - 1, cBottomLeft, border);
        putForegroundCharXY(left + boxWidth - 1, top + boxHeight - 1,
            cBottomRight, border);

        // Draw the box lines
        hForegroundLineXY(left + 1, top, boxWidth - 2, cHSide, border);
        vForegroundLineXY(left, top + 1, boxHeight - 2, cVSide, border);
        hForegroundLineXY(left + 1, top + boxHeight - 1, boxWidth - 2, cHSide,
            border);
        vForegroundLineXY(left + boxWidth - 1, top + 1, boxHeight - 2, cVSide,
            border);

        // Fill in the interior background
        for (int i = 1; i < boxHeight - 1; i++) {
            hForegroundLineXY(1 + left, i + top, boxWidth - 2, ' ', background);
        }

        if (shadow) {
            // Draw a shadow
            drawBoxShadow(left, top, right, bottom);
        }
    }

    /**
     * Draw a box shadow.
     *
     * @param left left column of box.  0 is the left-most column.
     * @param top top row of the box.  0 is the top-most row.
     * @param right right column of box
     * @param bottom bottom row of the box
     */
    public final void drawBoxShadow(final int left, final int top,
        final int right, final int bottom) {

        int boxTop = top;
        int boxLeft = left;
        int boxWidth = right - left;
        int boxHeight = bottom - top;
        CellAttributes shadowAttr = new CellAttributes();

        // Shadows do not honor clipping but they DO honor offset.
        int oldClipRight = clipRight;
        int oldClipBottom = clipBottom;
        // When offsetX or offsetY go negative, we need to increase the clip
        // bounds.
        clipRight = width - offsetX;
        clipBottom = height - offsetY;

        for (int i = 0; i < boxHeight; i++) {
            Cell cell = getCharXY(offsetX + boxLeft + boxWidth,
                offsetY + boxTop + 1 + i);
            if (cell.getWidth() == Cell.Width.SINGLE) {
                putAttrXY(boxLeft + boxWidth, boxTop + 1 + i, shadowAttr);
            } else {
                putCharXY(boxLeft + boxWidth, boxTop + 1 + i, ' ', shadowAttr);
            }
            cell = getCharXY(offsetX + boxLeft + boxWidth + 1,
                offsetY + boxTop + 1 + i);
            if (cell.getWidth() == Cell.Width.SINGLE) {
                putAttrXY(boxLeft + boxWidth + 1, boxTop + 1 + i, shadowAttr);
            } else {
                putCharXY(boxLeft + boxWidth + 1, boxTop + 1 + i, ' ',
                    shadowAttr);
            }
        }
        for (int i = 0; i < boxWidth; i++) {
            Cell cell = getCharXY(offsetX + boxLeft + 2 + i,
                offsetY + boxTop + boxHeight);
            if (cell.getWidth() == Cell.Width.SINGLE) {
                putAttrXY(boxLeft + 2 + i, boxTop + boxHeight, shadowAttr);
            } else {
                putCharXY(boxLeft + 2 + i, boxTop + boxHeight, ' ', shadowAttr);
            }
        }
        clipRight = oldClipRight;
        clipBottom = oldClipBottom;
    }

    /**
     * Default implementation does nothing.
     */
    public void flushPhysical() {}

    /**
     * Put the cursor at (x,y).
     *
     * @param visible if true, the cursor should be visible
     * @param x column coordinate to put the cursor on
     * @param y row coordinate to put the cursor on
     */
    public void putCursor(final boolean visible, final int x, final int y) {
        if ((cursorY >= 0)
            && (cursorX >= 0)
            && (cursorY <= height - 1)
            && (cursorX <= width - 1)
        ) {
            // Make the current cursor position dirty
            synchronized (this) {
                physical[cursorX][cursorY].unset();
            }
        }

        cursorVisible = visible;
        cursorX = x;
        cursorY = y;
    }

    /**
     * Hide the cursor.
     */
    public final void hideCursor() {
        cursorVisible = false;
    }

    /**
     * Get the cursor visibility.
     *
     * @return true if the cursor is visible
     */
    public boolean isCursorVisible() {
        return cursorVisible;
    }

    /**
     * Get the cursor X position.
     *
     * @return the cursor x column position
     */
    public int getCursorX() {
        return cursorX;
    }

    /**
     * Get the cursor Y position.
     *
     * @return the cursor y row position
     */
    public int getCursorY() {
        return cursorY;
    }

    /**
     * Set the window title.  Default implementation does nothing.
     *
     * @param title the new title
     */
    public void setTitle(final String title) {}

    // ------------------------------------------------------------------------
    // LogicalScreen ----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Reallocate screen buffers.
     *
     * @param width new width
     * @param height new height
     */
    private synchronized void reallocate(final int width, final int height) {
        if (logical != null) {
            for (int row = 0; row < this.height; row++) {
                for (int col = 0; col < this.width; col++) {
                    logical[col][row] = null;
                }
            }
            logical = null;
        }
        logical = new ComplexCell[width][height];
        if (physical != null) {
            for (int row = 0; row < this.height; row++) {
                for (int col = 0; col < this.width; col++) {
                    physical[col][row] = null;
                }
            }
            physical = null;
        }
        physical = new ComplexCell[width][height];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                physical[col][row] = new ComplexCell();
                logical[col][row] = new ComplexCell();
            }
        }

        this.width = width;
        this.height = height;

        clipLeft = 0;
        clipTop = 0;
        clipRight = width;
        clipBottom = height;
        relativeClipLeft = 0;
        relativeClipTop = 0;

        reallyCleared = true;
    }

    /**
     * Clear the physical screen.
     */
    public synchronized void clearPhysical() {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                physical[col][row].unset();
            }
        }
    }

    /**
     * Render one fullwidth cell.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param cell the cell to draw
     */
    private final void putFullwidthCharXY(final int x, final int y,
        final ComplexCell cell) {

        ComplexCell left = new ComplexCell(cell);
        left.setWidth(Cell.Width.LEFT);
        putCharXY(x, y, left, true);

        ComplexCell right = new ComplexCell(cell);
        right.setWidth(Cell.Width.RIGHT);
        putCharXY(x + 1, y, right, true);
    }

    /**
     * Render one fullwidth character with attributes.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param ch character to draw
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public final void putFullwidthCharXY(final int x, final int y,
        final int ch, final CellAttributes attr) {

        ComplexCell cell = new ComplexCell(ch, attr);
        putFullwidthCharXY(x, y, cell);
    }

    /**
     * Render one fullwidth character with attributes.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param ch character to draw
     */
    public final void putFullwidthCharXY(final int x, final int y,
        final int ch) {

        ComplexCell cell = new ComplexCell(ch);
        cell.setAttr(getAttrXY(x, y));
        putFullwidthCharXY(x, y, cell);
    }

    /**
     * Invert the cell color at a position, including both halves of a
     * double-width cell.
     *
     * @param x column position
     * @param y row position
     */
    public void invertCell(final int x, final int y) {
        invertCell(x, y, false);
    }

    /**
     * Invert the cell color at a position.
     *
     * @param x column position
     * @param y row position
     * @param onlyThisCell if true, only invert this cell, otherwise invert
     * both halves of a double-width cell if necessary
     */
    public void invertCell(final int x, final int y,
        final boolean onlyThisCell) {

        Cell cell = getCharXY(x, y);
        if (cell.getForeColorRGB() < 0) {
            cell.setForeColor(cell.getForeColor().invert());
        } else {
            cell.setForeColorRGB(cell.getForeColorRGB() ^ 0x00ffffff);
        }
        if (cell.getBackColorRGB() < 0) {
            cell.setBackColor(cell.getBackColor().invert());
        } else {
            cell.setBackColorRGB(cell.getBackColorRGB() ^ 0x00ffffff);
        }

        cell.setDefaultColor(true, false);
        cell.setDefaultColor(false, false);

        putCharXY(x, y, cell, true);
        if ((onlyThisCell == true) || (cell.getWidth() == Cell.Width.SINGLE)) {
            return;
        }

        // This cell is one half of a fullwidth glyph.  Invert the other
        // half.
        if (cell.getWidth() == Cell.Width.LEFT) {
            if (x < width - 1) {
                Cell rightHalf = getCharXY(x + 1, y);
                if (rightHalf.getWidth() == Cell.Width.RIGHT) {
                    invertCell(x + 1, y, true);
                    return;
                }
            }
        }
        if (cell.getWidth() == Cell.Width.RIGHT) {
            if (x > 0) {
                Cell leftHalf = getCharXY(x - 1, y);
                if (leftHalf.getWidth() == Cell.Width.LEFT) {
                    invertCell(x - 1, y, true);
                }
            }
        }
    }

    /**
     * Set a selection area on the screen.
     *
     * @param x0 the starting X position of the selection
     * @param y0 the starting Y position of the selection
     * @param x1 the ending X position of the selection
     * @param y1 the ending Y position of the selection
     * @param rectangle if true, this is a rectangle select
     */
    public void setSelection(final int x0, final int y0,
        final int x1, final int y1, final boolean rectangle) {

        int startX = x0;
        int startY = y0;
        int endX = x1;
        int endY = y1;

        if (((x1 < x0) && (y1 == y0))
            || (y1 < y0)
        ) {
            // The user dragged from bottom-to-top and/or right-to-left.
            // Reverse the coordinates for the inverted section.
            startX = x1;
            startY = y1;
            endX = x0;
            endY = y0;
        }
        if (rectangle) {
            for (int y = startY; y <= endY; y++) {
                for (int x = startX; x <= endX; x++) {
                    invertCell(x, y);
                }
            }
        } else {
            if (endY > startY) {
                for (int x = startX; x < width; x++) {
                    invertCell(x, startY);
                }
                for (int y = startY + 1; y < endY; y++) {
                    for (int x = 0; x < width; x++) {
                        invertCell(x, y);
                    }
                }
                for (int x = 0; x <= endX; x++) {
                    invertCell(x, endY);
                }
            } else {
                assert (startY == endY);
                for (int x = startX; x <= endX; x++) {
                    invertCell(x, startY);
                }
            }
        }
    }

    /**
     * Copy the screen selection area to the clipboard.
     *
     * @param clipboard the clipboard to use
     * @param x0 the starting X position of the selection
     * @param y0 the starting Y position of the selection
     * @param x1 the ending X position of the selection
     * @param y1 the ending Y position of the selection
     * @param rectangle if true, this is a rectangle select
     */
    public void copySelection(final Clipboard clipboard,
        final int x0, final int y0, final int x1, final int y1,
        final boolean rectangle) {

        StringBuilder sb = new StringBuilder();

        int startX = x0;
        int startY = y0;
        int endX = x1;
        int endY = y1;

        if (((x1 < x0) && (y1 == y0))
            || (y1 < y0)
        ) {
            // The user dragged from bottom-to-top and/or right-to-left.
            // Reverse the coordinates for the inverted section.
            startX = x1;
            startY = y1;
            endX = x0;
            endY = y0;
        }
        if (rectangle) {
            for (int y = startY; y <= endY; y++) {
                for (int x = startX; x <= endX; x++) {
                    sb.append(Character.toChars(getCharXY(x, y).getChar()));
                }
                sb.append("\n");
            }
        } else {
            if (endY > startY) {
                for (int x = startX; x < width; x++) {
                    sb.append(Character.toChars(getCharXY(x, startY).getChar()));
                }
                sb.append("\n");
                for (int y = startY + 1; y < endY; y++) {
                    for (int x = 0; x < width; x++) {
                        sb.append(Character.toChars(getCharXY(x, y).getChar()));
                    }
                    sb.append("\n");
                }
                for (int x = 0; x <= endX; x++) {
                    sb.append(Character.toChars(getCharXY(x, endY).getChar()));
                }
            } else {
                assert (startY == endY);
                for (int x = startX; x <= endX; x++) {
                    sb.append(Character.toChars(getCharXY(x, startY).getChar()));
                }
            }
        }
        clipboard.copyText(sb.toString());
    }

    /**
     * Obtain a snapshot copy of the screen.
     *
     * @return a copy of the screen's data
     */
    public Screen snapshot() {
        LogicalScreen other = null;
        synchronized (this) {
            other = new LogicalScreen(width, height);
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    other.logical[col][row] = new ComplexCell(logical[col][row]);
                }
            }
        }
        return other;
    }

    /**
     * Obtain a snapshot copy of a rectangular portion of the screen.
     *
     * @param x left column of rectangle.  0 is the left-most column.
     * @param y top row of the rectangle.  0 is the top-most row.
     * @param width number of columns to copy
     * @param height number of rows to copy
     * @return a copy of the screen's data from this rectangle.  Any cells
     * outside the actual screen dimensions will be blank.
     */
    public Screen snapshot(final int x, final int y, final int width,
        final int height) {

        LogicalScreen other = null;
        synchronized (this) {
            other = new LogicalScreen(width, height);
            for (int row = y; (row < y + height) && (row < this.height); row++) {
                if (row < 0) {
                    continue;
                }
                for (int col = x; (col < x + width) && (col < this.width); col++) {
                    if (col < 0) {
                        continue;
                    }
                    other.logical[col - x][row - y] = new ComplexCell(logical[col][row]);
                }
            }
        }
        return other;
    }

    /**
     * Obtain a snapshot copy of a rectangular portion of the screen of the
     * PHYSICAL screen - what was LAST emitted.
     *
     * @param x left column of rectangle.  0 is the left-most column.
     * @param y top row of the rectangle.  0 is the top-most row.
     * @param width number of columns to copy
     * @param height number of rows to copy
     * @return a copy of the screen's data from this rectangle.  Any cells
     * outside the actual screen dimensions will be blank.
     */
    public Screen snapshotPhysical(final int x, final int y, final int width,
        final int height) {

        LogicalScreen other = null;
        synchronized (this) {
            other = new LogicalScreen(width, height);
            for (int row = y; (row < y + height) && (row < this.height); row++) {
                if (row < 0) {
                    continue;
                }
                for (int col = x; (col < x + width) && (col < this.width); col++) {
                    if (col < 0) {
                        continue;
                    }
                    other.logical[col - x][row - y] = new ComplexCell(physical[col][row]);
                }
            }
        }
        return other;
    }

    /**
     * Copy all of screen's data to this screen.
     *
     * @param other the other screen
     */
    public void copyScreen(final Screen other) {
        synchronized (this) {
            if ((other.getWidth() != width) || (other.getHeight() != height)) {
                setDimensions(other.getWidth(), other.getHeight());
            }
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    logical[col][row] = new ComplexCell(other.getCharXY(col, row));
                }
            }
        }
    }

    /**
     * Copy a rectangular portion of another screen to this one.  Any cells
     * outside this screen's dimensions will be ignored.
     *
     * @param other the other screen
     * @param x left column of rectangle.  0 is the left-most column.
     * @param y top row of the rectangle.  0 is the top-most row.
     * @param width number of columns to copy
     * @param height number of rows to copy
     */
    public void copyScreen(final Screen other, final int x, final int y,
        final int width, final int height) {

        synchronized (this) {
            for (int row = y; (row < y + height) && (row < this.height); row++) {
                if (row < 0) {
                    continue;
                }
                for (int col = x; (col < x + width) && (col < this.width); col++) {
                    if (col < 0) {
                        continue;
                    }
                    logical[col][row] = new ComplexCell(other.getCharXY(col - x, row - y));
                }
            }
        }
    }

    /**
     * Set the backend to associated with this screen.
     *
     * @param backend the backend
     */
    public final void setBackend(final Backend backend) {
        this.backend = backend;
    }

    /**
     * Get the backend that instantiated this screen.
     *
     * @return the backend
     */
    public final Backend getBackend() {
        return backend;
    }

    /**
     * Alpha-blend a rectangle with a specified color and alpha onto this
     * screen.  Any cells outside this screen's dimensions will be ignored.
     *
     * @param x left column of rectangle.  0 is the left-most column.
     * @param y top row of the rectangle.  0 is the top-most row.
     * @param width number of columns to copy
     * @param height number of rows to copy
     * @param color the RGB color to blend
     * @param alpha the alpha transparency level (0 - 255) to use for cells
     * from the other screen
     */
    public void blendRectangle(final int x, final int y,
        final int width, final int height, final int color, final int alpha) {

        // We just create a new blank screen and blend it.
        LogicalScreen rectangle = new LogicalScreen(width, height);
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                rectangle.logical[col][row].setBackColorRGB(color);
            }
        }

        blendScreen(rectangle, x, y, width, height, alpha, false);
    }

    /**
     * Alpha-blend a rectangular portion of another screen onto this one.
     * Any cells outside this screen's dimensions will be ignored.
     *
     * @param otherScreen the other screen
     * @param x left column of rectangle.  0 is the left-most column.
     * @param y top row of the rectangle.  0 is the top-most row.
     * @param width number of columns to copy
     * @param height number of rows to copy
     * @param alpha the alpha transparency level (0 - 255) to use for cells
     * from the other screen
     * @param filterHatch if true, prevent hatch-like characters from
     * showing through
     */
    public void blendScreen(final Screen otherScreen, final int x, final int y,
        final int width, final int height, final int alpha,
        final boolean filterHatch) {

        if (alpha == 255) {
            // This is a raw copy.
            copyScreen(otherScreen, x, y, width, height);
            synchronized (this) {
                for (int row = y; (row < y + height) && (row < this.height); row++) {
                    if (row < 0) {
                        continue;
                    }
                    for (int col = x; (col < x + width) && (col < this.width); col++) {
                        if (col < 0) {
                            continue;
                        }
                        Cell thisCell = logical[col][row];
                        // Overlapped cells will not have DEFAULT (SGR 39/49)
                        // colors set.
                        thisCell.setDefaultColor(true, false);
                        thisCell.setDefaultColor(false, false);
                    }
                }
            }
            return;
        }

        long now = System.currentTimeMillis();

        /*
         * We need to blend the background colors of other's cells over the
         * cells of this screen (foreground and background), honoring our
         * alpha.  We will create a bitmap of one pixel per cell, blend that
         * via AWT, and then set the cell RGBs and char's.
         */
        synchronized (this) {

            BufferedImage thisForeground = new BufferedImage(width, height);
            BufferedImage thisBackground = new BufferedImage(width, height);
            BufferedImage overForeground = new BufferedImage(width, height);
            BufferedImage overBackground = new BufferedImage(width, height);
            BufferedImage thisOldBackground = new BufferedImage(width, height);

            final int OPAQUE = 0xFF000000;

            for (int row = y; (row < y + height) && (row < this.height); row++) {
                if (row < 0) {
                    continue;
                }
                for (int col = x; (col < x + width) && (col < this.width); col++) {
                    if (col < 0) {
                        continue;
                    }

                    Cell cell = logical[col][row];
                    int thisBg = cell.getBackColorRGB();
                    if (thisBg < 0) {
                        if (backend != null) {
                            thisBg = backend.attrToBackgroundColor(cell);
                        } else {
                            thisBg = ECMA48Terminal.attrToBackgroundColor(cell);
                        }
                    }
                    int thisFg = cell.getForeColorRGB();
                    if (thisFg < 0) {
                        if (backend != null) {
                            thisFg = backend.attrToForegroundColor(cell);
                        } else {
                            thisFg = ECMA48Terminal.attrToForegroundColor(cell);
                        }
                    }

                    Cell over = otherScreen.getCharXY(col - x, row - y);
                    int overFg = over.getForeColorRGB();
                    if (over.isPulse()) {
                        overFg = over.getForeColorPulseRGB(backend, now);
                    } else if (overFg < 0) {
                        if (backend != null) {
                            overFg = backend.attrToForegroundColor(over);
                        } else {
                            overFg = ECMA48Terminal.attrToForegroundColor(over);
                        }
                    }
                    int overBg = over.getBackColorRGB();
                    if (overBg < 0) {
                        if (backend != null) {
                            overBg = backend.attrToBackgroundColor(over);
                        } else {
                            overBg = ECMA48Terminal.attrToBackgroundColor(over);
                        }
                    }
                    thisFg |= OPAQUE;
                    thisBg |= OPAQUE;
                    overBg |= OPAQUE;
                    overFg |= OPAQUE;

                    thisForeground.setRGB(col - x, row - y, thisFg);
                    thisBackground.setRGB(col - x, row - y, thisBg);
                    thisOldBackground.setRGB(col - x, row - y, thisBg);
                    overForeground.setRGB(col - x, row - y, overFg);
                    overBackground.setRGB(col - x, row - y, overBg);
                }
            }

            // The four bitmaps are ready.  We have skipped over cells/pixels
            // that cannot overlap.  Now blit overBackground over both
            // thisForeground and thisBackground, and then assign cell colors
            // and cell chars/images.
            //
            // Also blit overForeground over thisBackground to handle the new
            // layer's glyph opacity.
            float fAlpha = (float) (alpha / 255.0);
            thisForeground.alphaBlendOver(overBackground, fAlpha);
            thisBackground.alphaBlendOver(overBackground, fAlpha);
            BufferedImage glyphForeground = new BufferedImage(thisBackground);
            glyphForeground.alphaBlendOver(overForeground, fAlpha);

            for (int row = y; (row < y + height) && (row < this.height); row++) {
                if (row < 0) {
                    continue;
                }
                for (int col = x; (col < x + width) && (col < this.width); col++) {
                    if (col < 0) {
                        continue;
                    }
                    Cell thisCell = logical[col][row];
                    Cell overCell = otherScreen.getCharXY(col - x, row - y);
                    int thisFg = thisForeground.getRGB(col - x, row - y);
                    int thisBg = thisBackground.getRGB(col - x, row - y);
                    int thisOldBg = thisOldBackground.getRGB(col - x, row - y);
                    int overBg = overBackground.getRGB(col - x, row - y);
                    int overFg = glyphForeground.getRGB(col - x, row - y);

                    thisCell.setBackColorRGB(thisBg | OPAQUE);
                    thisCell.setForeColorRGB(thisFg | OPAQUE);

                    // Overlapped cells will not have DEFAULT (SGR 39/49)
                    // colors set.
                    thisCell.setDefaultColor(true, false);
                    thisCell.setDefaultColor(false, false);

                    if (overCell.isSpaceChar() && !overCell.isUnderline()) {
                        // The overlaying cell is invisible.

                        // Our character will show through.  If the contrast
                        // between our foreground and background is small,
                        // then drop the character.
                        if (ImageUtils.rgbDistance(thisFg, thisBg) < 5) {
                            thisCell.setChar(' ');
                            thisCell.setWidth(Cell.Width.SINGLE);
                        }

                        if (filterHatch) {
                            // Special case: the hatch characters are not
                            // allowed to show through.
                            if (thisCell.isCodePoint(0x2591)
                                || thisCell.isCodePoint(0x2592)
                                || thisCell.isCodePoint(0x2593)
                            ) {
                                thisCell.setChar(' ');
                                thisCell.setWidth(Cell.Width.SINGLE);
                            }
                        }
                        if (cursorVisible &&
                            (col == cursorX) &&
                            (row == cursorY)
                        ) {
                            // Don't surface the character behind the cursor.
                            thisCell.setForeColorRGB(overFg);
                            thisCell.setChar(' ');
                            thisCell.setWidth(Cell.Width.SINGLE);
                        }
                        continue;
                    }

                    // The overlaying cell has a character, use it.
                    thisCell.setChar(overCell);
                    thisCell.setForeColorRGB(overFg);
                    thisCell.setBold(overCell.isBold());
                    thisCell.setBlink(overCell.isBlink());
                    thisCell.setUnderline(overCell.isUnderline());
                    thisCell.setProtect(overCell.isProtect());
                    thisCell.setAnimations(overCell.getAnimations());
                    thisCell.setPulse(false, false, 0);
                    thisCell.setWidth(overCell.getWidth());
                }
            }
        }
    }

    /**
     * Perform some kind of change to a cell, based on its location relative
     * to a widget or the entire screen.
     *
     * @param backend the backend that can obtain the correct foreground or
     * background color of the cell
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of area to be modified
     * @param height height of area to be modified
     * @param cellTransform the cell transform to apply
     * @param widget the widget this cell is on, or null if the transform is
     * relative to the entire screen
     */
    public final void applyCellTransform(final Backend backend,
        final int x, final int y, final int width, final int height,
        final CellTransform cellTransform, TWidget widget) {

        assert (backend != null);
        assert (cellTransform != null);

        cellTransform.prepareTransform(backend, widget);
        for (int row = y; row < y + height; row++) {
            for (int col = x; col < x + width; col++) {

                int X = col;
                int Y = row;

                if ((X < clipLeft)
                    || (X >= clipRight)
                    || (Y < clipTop)
                    || (Y >= clipBottom)
                ) {
                    continue;
                }

                X += offsetX;
                Y += offsetY;

                if ((X >= 0) && (X < this.width)
                    && (Y >= 0) && (Y < this.height)
                ) {
                    ComplexCell cell = logical[X][Y];
                    if (widget == null) {
                        cellTransform.applyTransform(backend, cell, X, Y, null);
                    } else {
                        cellTransform.applyTransform(backend, cell, col - x,
                            row - y, widget);
                    }
                }
            }
        }
    }

}
