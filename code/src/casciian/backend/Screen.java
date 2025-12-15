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
import casciian.bits.Clipboard;

/**
 * Drawing operations API.
 */
public interface Screen {

    /**
     * Set drawing offset for x.
     *
     * @param offsetX new drawing offset
     */
    public void setOffsetX(final int offsetX);

    /**
     * Get drawing offset for x.
     *
     * @return the drawing offset
     */
    public int getOffsetX();

    /**
     * Set drawing offset for y.
     *
     * @param offsetY new drawing offset
     */
    public void setOffsetY(final int offsetY);

    /**
     * Get drawing offset for y.
     *
     * @return the drawing offset
     */
    public int getOffsetY();

    /**
     * Get right drawing clipping boundary.
     *
     * @return drawing boundary
     */
    public int getClipRight();

    /**
     * Set right drawing clipping boundary.
     *
     * @param clipRight new boundary
     */
    public void setClipRight(final int clipRight);

    /**
     * Get bottom drawing clipping boundary.
     *
     * @return drawing boundary
     */
    public int getClipBottom();

    /**
     * Set bottom drawing clipping boundary.
     *
     * @param clipBottom new boundary
     */
    public void setClipBottom(final int clipBottom);

    /**
     * Get left drawing clipping boundary.
     *
     * @return drawing boundary
     */
    public int getClipLeft();

    /**
     * Set left drawing clipping boundary.
     *
     * @param clipLeft new boundary
     */
    public void setClipLeft(final int clipLeft);

    /**
     * Get top drawing clipping boundary.
     *
     * @return drawing boundary
     */
    public int getClipTop();

    /**
     * Set top drawing clipping boundary.
     *
     * @param clipTop new boundary
     */
    public void setClipTop(final int clipTop);

    /**
     * Set left drawing clipping boundary relative to offsetX.
     *
     * @param relativeClipLeft new boundary
     */
    public void setRelativeClipLeft(final int relativeClipLeft);

    /**
     * Set top drawing clipping boundary relative to offsetY.
     *
     * @param relativeClipTop new boundary
     */
    public void setRelativeClipTop(final int relativeClipTop);

    /**
     * Get dirty flag.
     *
     * @return if true, the logical screen is not in sync with the physical
     * screen
     */
    public boolean isDirty();

    /**
     * Get the attributes at one location.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @return attributes at (x, y)
     */
    public CellAttributes getAttrXY(final int x, final int y);

    /**
     * Get the cell at one location in absolute coordinates.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @return the character + attributes
     */
    public Cell getCharXY(final int x, final int y);

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
    public Cell getCharXY(final int x, final int y, final boolean clip);

    /**
     * Set the attributes at one location.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public void putAttrXY(final int x, final int y,
        final CellAttributes attr);

    /**
     * Set the attributes at one location.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param attr attributes to use (bold, foreColor, backColor)
     * @param clip if true, honor clipping/offset
     */
    public void putAttrXY(final int x, final int y,
        final CellAttributes attr, final boolean clip);

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
        final int right, final int bottom, final CellAttributes attr);

    /**
     * Set the background color only at one location.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param attr the background color to use
     */
    public void putBackgroundAttrXY(final int x, final int y,
        final CellAttributes attr);

    /**
     * Set the background color only at one location.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param attr the background color to use
     * @param clip if true, honor clipping/offset
     */
    public void putBackgroundAttrXY(final int x, final int y,
        final CellAttributes attr, final boolean clip);

    /**
     * Fill the entire screen with one character with attributes.
     *
     * @param ch character to draw
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public void putAll(final int ch, final CellAttributes attr);

    /**
     * Fill the entire screen with one cell.
     *
     * @param ch the character to draw
     */
    public void putAll(final Cell ch);

    /**
     * Render one character with attributes.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param ch character + attributes to draw
     */
    public void putCharXY(final int x, final int y, final Cell ch);

    /**
     * Render one character with attributes.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param ch character to draw
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public void putCharXY(final int x, final int y, final int ch,
        final CellAttributes attr);

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
        final CellAttributes attr);

    /**
     * Render one character without changing the underlying attributes.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param ch character to draw
     */
    public void putCharXY(final int x, final int y, final int ch);

    /**
     * Render a string with attributes.  Does not wrap if the string exceeds
     * the line.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param str string to draw
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public void putStringXY(final int x, final int y, final String str,
        final CellAttributes attr);

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
        final String str, final CellAttributes attr);

    /**
     * Render a string without changing the underlying attribute.  Does not
     * wrap if the string exceeds the line.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param str string to draw
     */
    public void putStringXY(final int x, final int y, final String str);

    /**
     * Draw a vertical line from (x, y) to (x, y + n).
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param n number of characters to draw
     * @param ch character to draw
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public void vLineXY(final int x, final int y, final int n,
        final int ch, final CellAttributes attr);

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
        final int ch, final CellAttributes attr);

    /**
     * Draw a vertical line from (x, y) to (x, y + n).
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param n number of characters to draw
     * @param ch character to draw
     */
    public void vLineXY(final int x, final int y, final int n,
        final Cell ch);

    /**
     * Draw a horizontal line from (x, y) to (x + n, y).
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param n number of characters to draw
     * @param ch character to draw
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public void hLineXY(final int x, final int y, final int n,
        final int ch, final CellAttributes attr);

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
        final int ch, final CellAttributes attr);

    /**
     * Draw a horizontal line from (x, y) to (x + n, y).
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param n number of characters to draw
     * @param ch character to draw
     */
    public void hLineXY(final int x, final int y, final int n,
        final Cell ch);

    /**
     * Change the width.  Everything on-screen will be destroyed and must be
     * redrawn.
     *
     * @param width new screen width
     */
    public void setWidth(final int width);

    /**
     * Change the height.  Everything on-screen will be destroyed and must be
     * redrawn.
     *
     * @param height new screen height
     */
    public void setHeight(final int height);

    /**
     * Change the width and height.  Everything on-screen will be destroyed
     * and must be redrawn.
     *
     * @param width new screen width
     * @param height new screen height
     */
    public void setDimensions(final int width, final int height);

    /**
     * Get the height.
     *
     * @return current screen height
     */
    public int getHeight();

    /**
     * Get the width.
     *
     * @return current screen width
     */
    public int getWidth();

    /**
     * Reset screen to not-bold, white-on-black.  Also flushes the offset and
     * clip variables.
     */
    public void reset();

    /**
     * Flush the offset and clip variables.
     */
    public void resetClipping();

    /**
     * Clear the logical screen.
     */
    public void clear();

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
        final BorderStyle borderStyle, final boolean shadow);

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
    public void drawForegroundBox(final int left, final int top,
        final int right, final int bottom,
        final CellAttributes border, final CellAttributes background,
        final BorderStyle borderStyle, final boolean shadow);

    /**
     * Draw a box shadow.
     *
     * @param left left column of box.  0 is the left-most column.
     * @param top top row of the box.  0 is the top-most row.
     * @param right right column of box
     * @param bottom bottom row of the box
     */
    public void drawBoxShadow(final int left, final int top,
        final int right, final int bottom);

    /**
     * Clear the physical screen.
     */
    public void clearPhysical();

    /**
     * Classes must provide an implementation to push the logical screen to
     * the physical device.
     */
    public void flushPhysical();

    /**
     * Put the cursor at (x,y).
     *
     * @param visible if true, the cursor should be visible
     * @param x column coordinate to put the cursor on
     * @param y row coordinate to put the cursor on
     */
    public void putCursor(final boolean visible, final int x, final int y);

    /**
     * Hide the cursor.
     */
    public void hideCursor();

    /**
     * Get the cursor visibility.
     *
     * @return true if the cursor is visible
     */
    public boolean isCursorVisible();

    /**
     * Get the cursor X position.
     *
     * @return the cursor x column position
     */
    public int getCursorX();

    /**
     * Get the cursor Y position.
     *
     * @return the cursor y row position
     */
    public int getCursorY();

    /**
     * Set the window title.
     *
     * @param title the new title
     */
    public void setTitle(final String title);

    /**
     * Invert the cell color at a position, including both halves of a
     * double-width cell.
     *
     * @param x column position
     * @param y row position
     */
    public void invertCell(final int x, final int y);

    /**
     * Invert the cell color at a position.
     *
     * @param x column position
     * @param y row position
     * @param onlyThisCell if true, only invert this cell, otherwise invert
     * both halves of a double-width cell if necessary
     */
    public void invertCell(final int x, final int y,
        final boolean onlyThisCell);

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
        final int x1, final int y1, final boolean rectangle);

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
        final boolean rectangle);

    /**
     * Obtain a snapshot copy of the screen.
     *
     * @return a copy of the screen's data
     */
    public Screen snapshot();

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
        final int height);

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
        final int height);

    /**
     * Copy all of screen's data to this screen.
     *
     * @param other the other screen
     */
    public void copyScreen(final Screen other);

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
        final int width, final int height);

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
        final boolean filterHatch);

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
        final int width, final int height, final int color, final int alpha);

    /**
     * Get the backend that instantiated this screen.
     *
     * @return the backend
     */
    public Backend getBackend();

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
    public void applyCellTransform(final Backend backend,
        final int x, final int y, final int width, final int height,
        final CellTransform cellTransform, final TWidget widget);

}
