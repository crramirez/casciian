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

import casciian.TApplication;
import casciian.bits.Animation;

/**
 * Bitmap is a raw bitmap image.
 */
public class Bitmap extends TackboardItem {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The image data.
     */
    private BufferedImage image;

    /**
     * The rendered image data.
     */
    private BufferedImage renderedImage;

    /**
     * Animation to display.
     */
    private Animation animation;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param x X pixel coordinate
     * @param y Y pixel coordinate
     * @param z Z coordinate
     * @param image the image
     */
    public Bitmap(final int x, final int y, final int z,
        final BufferedImage image) {

        super(x, y, z);
        this.image = image;
    }

    /**
     * Public constructor.  Due to the use of a timer, the Bitmap needs to
     * see TApplication to start the Animation.
     *
     * @param x X pixel coordinate
     * @param y Y pixel coordinate
     * @param z Z coordinate
     * @param animation the animation to display
     * @param application the application to set the animation timer on
     */
    public Bitmap(final int x, final int y, final int z,
        final Animation animation, final TApplication application) {

        super(x, y, z);
        this.animation = animation;
        image = animation.getFrame();
        animation.start(application);
    }

    // ------------------------------------------------------------------------
    // TackboardItem ----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Comparison check.  All fields must match to return true.
     *
     * @param rhs another Bitmap instance
     * @return true if all fields are equal
     */
    @Override
    public boolean equals(final Object rhs) {
        if (!(rhs instanceof Bitmap)) {
            return false;
        }
        Bitmap that = (Bitmap) rhs;
        return (super.equals(rhs)
            && (this.image.equals(that.image)));
    }

    /**
     * Hashcode uses all fields in equals().
     *
     * @return the hash
     */
    @Override
    public int hashCode() {
        int A = 13;
        int B = 23;
        int hash = A;
        hash = (B * hash) + super.hashCode();
        hash = (B * hash) + image.hashCode();
        return hash;
    }

    /**
     * Make human-readable description of this item.
     *
     * @return displayable String
     */
    @Override
    public String toString() {
        return String.format("(%d, %d, %d) %d X %d", getX(), getY(), getZ(),
            (image != null ? image.getWidth() : "null"),
            (image != null ? image.getHeight() : "null"));
    }

    /**
     * Get this item rendered to a bitmap, offset to align on a grid of
     * cells with pixel dimensions (textWidth, textHeight).
     *
     * @param textWidth the width of a text cell
     * @param textHeight the height of a text cell
     * @return the image, or null if this item does not have any pixels to
     * show
     */
    @Override
    public BufferedImage getImage(final int textWidth, final int textHeight) {
        if (dirty) {
            render(textWidth, textHeight);
            dirty = false;
        }
        return renderedImage;
    }

    /**
     * Remove this item from its board.  Subclasses can use this for cleanup
     * also.
     */
    @Override
    public void remove() {
        super.remove();

        if (animation != null) {
            animation.stop();
        }
    }

    // ------------------------------------------------------------------------
    // Bitmap -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get this item rendered to a bitmap, offset to align on a grid of
     * cells with pixel dimensions (textWidth, textHeight).
     *
     * @param textWidth the width of a text cell
     * @param textHeight the height of a text cell
     */
    private void render(final int textWidth, final int textHeight) {
        if (animation != null) {
            BufferedImage newFrame = animation.getFrame();
            if (newFrame != image) {
                image = newFrame;
                renderedImage = null;
            }
        }
        if (image == null) {
            renderedImage = null;
            return;
        }
        int dx = getX() % textWidth;
        int dy = getY() % textHeight;
        if ((dx == 0) && (dy == 0)) {
            renderedImage = image;
            return;
        }

        int columns = (dx + image.getWidth()) / textWidth;
        if ((dx + image.getWidth()) % textWidth > 0) {
            columns++;
        }
        int rows = (dy + image.getHeight()) / textHeight;
        if ((dy + image.getHeight()) % textHeight > 0) {
            rows++;
        }

        renderedImage = new BufferedImage(columns * textWidth,
            rows * textHeight, BufferedImage.TYPE_INT_ARGB);

        java.awt.Graphics gr = renderedImage.getGraphics();
        gr.setColor(java.awt.Color.BLACK);
        gr.drawImage(image, dx, dy, null, null);
        gr.dispose();
    }

    /**
     * Set the image.
     *
     * @param image the new image
     */
    public void setImage(final BufferedImage image) {
        if (animation != null) {
            animation.stop();
            animation = null;
        }
        this.image = image;
        setDirty();
    }

}
