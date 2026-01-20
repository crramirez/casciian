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

import casciian.bits.Cell;
import casciian.bits.ImageRGB;
import casciian.bits.ImageUtils;
import casciian.event.TCommandEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import casciian.event.TResizeEvent;
import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * TImage renders a piece of a bitmap image or an animated image on screen.
 */
public class TImage extends TWidget implements EditMenuUser {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The action to perform when the user clicks on the image.
     */
    private TAction clickAction;

    /**
     * The image to display.
     */
    private ImageRGB image;

    /**
     * Left column of the image.  0 is the left-most column.
     */
    private int left;

    /**
     * Top row of the image.  0 is the top-most row.
     */
    private int top;

    /**
     * The cells containing the broken up image pieces.
     */
    private Cell[][] cells;

    /**
     * The number of rows in cells[].
     */
    private int cellRows;

    /**
     * The number of columns in cells[].
     */
    private int cellColumns;

    /**
     * Last text width value.
     */
    private int lastTextWidth = -1;

    /**
     * Last text height value.
     */
    private int lastTextHeight = -1;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width number of text cells for width of the image
     * @param height number of text cells for height of the image
     * @param image the image to display
     * @param left left column of the image.  0 is the left-most column.
     * @param top top row of the image.  0 is the top-most row.
     */
    public TImage(final TWidget parent, final int x, final int y,
        final int width, final int height, final ImageRGB image,
        final int left, final int top) {

        this(parent, x, y, width, height, image, left, top, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width number of text cells for width of the image
     * @param height number of text cells for height of the image
     * @param image the image to display
     * @param left left column of the image.  0 is the left-most column.
     * @param top top row of the image.  0 is the top-most row.
     * @param clickAction function to call when mouse is pressed
     */
    @SuppressWarnings("this-escape")
    public TImage(final TWidget parent, final int x, final int y,
        final int width, final int height, final ImageRGB image,
        final int left, final int top, final TAction clickAction) {

        // Set parent and window
        super(parent, x, y, width, height);

        this.image = image;
        this.left = left;
        this.top = top;
        this.clickAction = clickAction;

        sizeToImage(true);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width number of text cells for width of the image
     * @param height number of text cells for height of the image
     * @param left left column of the image.  0 is the left-most column.
     * @param top top row of the image.  0 is the top-most row.
     */
    public TImage(final TWidget parent, final int x, final int y,
        final int width, final int height,
        final int left, final int top) {

        this(parent, x, y, width, height, left, top, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width number of text cells for width of the image
     * @param height number of text cells for height of the image
     * @param left left column of the image.  0 is the left-most column.
     * @param top top row of the image.  0 is the top-most row.
     * @param clickAction function to call when mouse is pressed
     */
    @SuppressWarnings("this-escape")
    public TImage(final TWidget parent, final int x, final int y,
        final int width, final int height,
        final int left, final int top, final TAction clickAction) {

        // Set parent and window
        super(parent, x, y, width, height);

        this.left = left;
        this.top = top;
        this.clickAction = clickAction;

        sizeToImage(true);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if (clickAction != null) {
            clickAction.DO(this);
            return;
        }
    }

    /**
     * Handle posted command events.
     *
     * @param command command event
     */
    @Override
    public void onCommand(final TCommandEvent command) {
        if (command.getCmd() == cmCopy) {
            // Copy image to clipboard.
            //getClipboard().copyImage(image);
            return;
        }
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the image.
     */
    @Override
    public void draw() {
        sizeToImage(false);

        // We have already broken the image up, just draw the previously
        // created set of cells.
        for (int x = 0; (x < getWidth()) && (x + left < cellColumns); x++) {
            if ((left + x) * lastTextWidth > image.getWidth()) {
                continue;
            }

            for (int y = 0; (y < getHeight()) && (y + top < cellRows); y++) {
                if ((top + y) * lastTextHeight > image.getHeight()) {
                    continue;
                }
                assert (x + left < cellColumns);
                assert (y + top < cellRows);

                putCharXY(x, y, cells[x + left][y + top]);
            }
        }
    }

    // ------------------------------------------------------------------------
    // TImage -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Size cells[][] according to the screen font size.
     *
     * @param always if true, always resize the cells
     */
    private void sizeToImage(final boolean always) {

        if ((getApplication() == null)
            || (getApplication().getBackend() == null)
        ) {
            return;
        }

        int textWidth = getScreen().getTextWidth();
        int textHeight = getScreen().getTextHeight();

        if (always || (textWidth > 0
                && (textWidth != lastTextWidth)
                && (textHeight > 0)
                && (textHeight != lastTextHeight))
        ) {
            cellColumns = image.getWidth() / textWidth;
            if (cellColumns * textWidth < image.getWidth()) {
                cellColumns++;
            }
            cellRows = image.getHeight() / textHeight;
            if (cellRows * textHeight < image.getHeight()) {
                cellRows++;
            }

            // Break the image up into an array of cells.
            cells = new Cell[cellColumns][cellRows];

            for (int x = 0; x < cellColumns; x++) {
                for (int y = 0; y < cellRows; y++) {

                    int width = textWidth;
                    if ((x + 1) * textWidth > image.getWidth()) {
                        width = image.getWidth() - (x * textWidth);
                    }
                    int height = textHeight;
                    if ((y + 1) * textHeight > image.getHeight()) {
                        height = image.getHeight() - (y * textHeight);
                    }

                    Cell cell = new Cell();
                    cell.setTo(getWindow().getBackground());

                    // Render over a full-cell-size image.
                    ImageRGB subImage = image.getSubimage(x * textWidth,
                        y * textHeight, width, height);

                    cell.setImage(subImage);
                        cells[x][y] = cell;
                }
            }

            lastTextWidth = textWidth;
            lastTextHeight = textHeight;
        }

        if ((left + getWidth()) > cellColumns) {
            left = cellColumns - getWidth();
        }
        if (left < 0) {
            left = 0;
        }
        if ((top + getHeight()) > cellRows) {
            top = cellRows - getHeight();
        }
        if (top < 0) {
            top = 0;
        }
    }

    /**
     * Get the top corner to render.
     *
     * @return the top row
     */
    public int getTop() {
        return top;
    }

    /**
     * Set the top corner to render.
     *
     * @param top the new top row
     */
    public void setTop(final int top) {
        this.top = top;
        if (this.top > cellRows - getHeight()) {
            this.top = cellRows - getHeight();
        }
        if (this.top < 0) {
            this.top = 0;
        }
    }

    /**
     * Get the left corner to render.
     *
     * @return the left column
     */
    public int getLeft() {
        return left;
    }

    /**
     * Set the left corner to render.
     *
     * @param left the new left column
     */
    public void setLeft(final int left) {
        this.left = left;
        if (this.left > cellColumns - getWidth()) {
            this.left = cellColumns - getWidth();
        }
        if (this.left < 0) {
            this.left = 0;
        }
    }

    /**
     * Get the number of text cell rows for this image.
     *
     * @return the number of rows
     */
    public int getRows() {
        return cellRows;
    }

    /**
     * Get the number of text cell columns for this image.
     *
     * @return the number of columns
     */
    public int getColumns() {
        return cellColumns;
    }

    /**
     * Get the raw (unprocessed) image.
     *
     * @return the image
     */
    public ImageRGB getImage() {
        return image;
    }

    /**
     * Set the raw image, and reprocess to make the visible image.
     *
     * @param image the new image
     */
    public void setImage(final ImageRGB image) {
        this.image = image;
    }

    /**
     * Get the visible (processed) image.
     *
     * @return the image that is currently on screen
     */
    public ImageRGB getVisibleImage() {
        return image;
    }

    // ------------------------------------------------------------------------
    // EditMenuUser -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Check if the cut menu item should be enabled.
     *
     * @return true if the cut menu item should be enabled
     */
    public boolean isEditMenuCut() {
        return false;
    }

    /**
     * Check if the copy menu item should be enabled.
     *
     * @return true if the copy menu item should be enabled
     */
    public boolean isEditMenuCopy() {
        return true;
    }

    /**
     * Check if the paste menu item should be enabled.
     *
     * @return true if the paste menu item should be enabled
     */
    public boolean isEditMenuPaste() {
        return false;
    }

    /**
     * Check if the clear menu item should be enabled.
     *
     * @return true if the clear menu item should be enabled
     */
    public boolean isEditMenuClear() {
        return false;
    }

}
