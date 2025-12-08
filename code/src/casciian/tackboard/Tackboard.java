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
package casciian.tackboard;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import casciian.backend.Screen;
import casciian.bits.Cell;
import casciian.bits.GlyphMaker;
import casciian.bits.ImageUtils;

/**
 * Tackboard maintains a collection of TackboardItems to draw on a Screen.
 *
 * <p>Each item has a set of X, Y, Z pixel (not text cell) coordinates.  The
 * coordinate system is right-handed: (0, 0, 0) is the top-left pixel on the
 * screen, and positive Z points away from the user.</p>
 *
 * <p>When draw() is called, all the items will be rendered in descending Z,
 * ascending Y, ascending X order (painter's algorithm) onto the cell grid,
 * and using transparent pixels.  If the Screen's backend does not support
 * imagesOverText, then the text of the Cell under transparent images will be
 * rendered via GlyphMaker, which might not look ideal if the internal font
 * is quite different from the terminal's.</p>
 *
 * <p>Tackboards were directly inspired by the Visuals (ncvisuals) of <a
 * href="https://github.com/dankamongmen/notcurses">notcurses</a>. Casciian's
 * performance is unlikely to come close to notcurses, so users requiring
 * low-latency pixel-based rendering are recommended to check out
 * notcurses.</p>
 */
public class Tackboard {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The items on this board.
     */
    private ArrayList<TackboardItem> items = new ArrayList<TackboardItem>();

    /**
     * Last text width value.
     */
    private int lastTextWidth = -1;

    /**
     * Last text height value.
     */
    private int lastTextHeight = -1;

    /**
     * Dirty flag, if true then getImage() needs to generate a rendering
     * aligned to the text cells.
     */
    private boolean dirty = true;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     */
    public Tackboard() {
        // NOP
    }

    // ------------------------------------------------------------------------
    // Tackboard --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set dirty flag.
     */
    public final void setDirty() {
        dirty = true;
    }

    /**
     * Add an item to the board.
     *
     * @param item the item to add
     */
    public void addItem(final TackboardItem item) {
        item.setTackboard(this);
        items.add(item);
        dirty = true;
    }

    /**
     * Get the list of items.
     *
     * @return the list of items
     */
    public List<TackboardItem> getItems() {
        return items;
    }

    /**
     * Get the number of items on this board.
     *
     * @return the number of items
     */
    public int size() {
        return items.size();
    }

    /**
     * Remove everything on this board.
     */
    public void clear() {
        while (items.size() > 0) {
            // Give every item a shot to cleanup if it needs to.
            TackboardItem item = items.get(0);
            item.remove();
        }
        dirty = false;
    }

    /**
     * Draw everything to the screen.
     *
     * @param screen the screen to render to
     * @param transparent if true, allow partially transparent images to be
     * drawn to the screen
     */
    public void draw(final Screen screen, final boolean transparent) {
        Collections.sort(items);
        int cellWidth = screen.getTextWidth();
        int cellHeight = screen.getTextHeight();
        boolean redraw = dirty;

        if ((lastTextWidth == -1)
            || (lastTextWidth != cellWidth)
            || (lastTextHeight != cellHeight)
        ) {
            // We need to force a redraw because the cell grid dimensions
            // have changed.
            redraw = true;
            lastTextWidth = cellWidth;
            lastTextHeight = cellHeight;
        }

        int imageId = System.identityHashCode(this);
        imageId ^= (int) System.currentTimeMillis();

        for (TackboardItem item: items) {
            if (redraw) {
                item.setDirty();
            }
            BufferedImage image = item.getImage(cellWidth, cellHeight);
            if (image == null) {
                continue;
            }

            int x = item.getX();
            int y = item.getY();
            int textX = x / cellWidth;
            int textY = y / cellHeight;
            int width = image.getWidth();
            int height = image.getHeight();

            if ((width % cellWidth != 0) || (height % cellHeight != 0)) {
                // These should have lined up, that was the whole point of
                // the redraw.  Why didn't they?
                /*
                System.err.println("HUH? width " + width +
                    " cellWidth " + cellWidth +
                    " height " + height +
                    " cellHeight " + cellHeight);
                 */
            } else {
                // This should be impossible, right?
                assert (width % cellWidth == 0);
                assert (height % cellHeight == 0);
            }

            int columns = width / cellWidth;
            int rows = height / cellHeight;

            int screenWidth = screen.getWidth() * screen.getTextWidth();
            int screenHeight = screen.getHeight() * screen.getTextHeight();
            if ((textX + columns < 0)
                || (textY + rows < 0)
                || (textX >= screen.getWidth())
                || (textY >= screen.getHeight())
            ) {
                // No cells of this item will be visible on the screen.
                continue;
            }

            int dx = x % cellWidth;
            int dy = y % cellHeight;

            // I had thought that with the offsets there might be a
            // discontinuity around +/- 0, but there isn't.  Still, leaving
            // these here in case I'm wrong later on.
            final int left = 0;
            final int top = 0;

            for (int sy = 0; sy < rows; sy++) {
                if ((sy + textY + top < 0)
                    || (sy + textY + top >= screen.getHeight())
                ) {
                    // This row of cells is off-screen, skip it.
                    continue;
                }
                for (int sx = 0; sx < columns; sx++) {
                    while (sx + textX + left < 0) {
                        // This cell is off-screen, advance.
                        sx++;
                    }
                    if (sx + textX + left >= screen.getWidth()) {
                        // This cell is off-screen, done with this entire row.
                        break;
                    }

                    Cell oldCell = screen.getCharXY(sx + textX + left,
                        sy + textY + top, true);
                    if (oldCell == null) {
                        // This image fragment would not be visible on the
                        // screen.
                        continue;
                    }

                    BufferedImage newImage = image.getSubimage(sx * cellWidth,
                        sy * cellHeight, cellWidth, cellHeight);

                    if (ImageUtils.isFullyTransparent(newImage)) {
                        // Skip this cell.
                        continue;
                    }

                    // newImage has the image that needs to be overlaid on
                    // (sx + textX + left, sy + textY + top)

                    if (oldCell.isImage()) {
                        // Blit this image over that one.
                        BufferedImage oldImage = oldCell.getImage(true);
                        java.awt.Graphics gr = oldImage.getGraphics();
                        gr.setColor(screen.getBackend().
                            attrToBackgroundColor(oldCell));
                        gr.drawImage(newImage, 0, 0, null, null);
                        gr.dispose();
                        imageId++;
                        oldCell.setImage(oldImage, imageId & 0x7FFFFFFF);
                    } else {
                        // Old cell is text only, just add the image.
                        if (!transparent) {
                            BufferedImage backImage;
                            backImage = new BufferedImage(cellWidth,
                                cellHeight, BufferedImage.TYPE_INT_ARGB);
                            java.awt.Graphics gr = backImage.getGraphics();

                            java.awt.Color oldColor = screen.getBackend().
                                    attrToBackgroundColor(oldCell);
                            gr.setColor(oldColor);
                            gr.fillRect(0, 0, backImage.getWidth(),
                                backImage.getHeight());
                            gr.drawImage(newImage, 0, 0, null, null);
                            gr.dispose();
                            imageId++;
                            oldCell.setImage(backImage, imageId & 0x7FFFFFFF);
                        } else {
                            imageId++;
                            oldCell.setImage(newImage, imageId & 0x7FFFFFFF);
                        }
                    }
                    screen.putCharXY(sx + textX + left, sy + textY + top,
                        oldCell);

                } // for (int sx = 0; sx < columns; sx++)

            } // for (int sy = 0; sy < rows; sy++)

        } // for (TackboardItem item: items)

        dirty = false;
    }

}
