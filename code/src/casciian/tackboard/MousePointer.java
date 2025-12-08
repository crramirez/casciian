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

/**
 * MousePointer is a Bitmap with a hotspot location to represent the "tip" of
 * a mouse icon.
 */
public class MousePointer extends Bitmap implements Pointer {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the hotspot X location relative to the X location of the icon.
     */
    private int hotspotX = 0;

    /**
     * Get the hotspot Y location relative to the Y location of the icon.
     */
    private int hotspotY = 0;

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
     * @param hotspotX the hotspot X location relative to x
     * @param hotspotY the hotspot Y location relative to y
     */
    public MousePointer(final int x, final int y, final int z,
        final BufferedImage image, final int hotspotX, final int hotspotY) {

        super(x, y, z, image);
        this.hotspotX = hotspotX;
        this.hotspotY = hotspotY;
    }

    // ------------------------------------------------------------------------
    // TackboardItem ----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Comparison check.  All fields must match to return true.
     *
     * @param rhs another MousePointer instance
     * @return true if all fields are equal
     */
    @Override
    public boolean equals(final Object rhs) {
        if (!(rhs instanceof MousePointer)) {
            return false;
        }
        MousePointer that = (MousePointer) rhs;
        return (super.equals(rhs)
            && (this.hotspotX == that.hotspotX)
            && (this.hotspotY == that.hotspotY));
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
        hash = (B * hash) + hotspotX;
        hash = (B * hash) + hotspotY;
        return hash;
    }

    /**
     * Make human-readable description of this item.
     *
     * @return displayable String
     */
    @Override
    public String toString() {
        return String.format("MousePointer[%d, %d]: %s",
            hotspotX, hotspotY, super.toString());
    }

    // ------------------------------------------------------------------------
    // Pointer ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the hotspot X location relative to the X location of the icon.
     */
    public int getHotspotX() {
        return hotspotX;
    }

    /**
     * Get the hotspot Y location relative to the Y location of the icon.
     */
    public int getHotspotY() {
        return hotspotY;
    }

}
