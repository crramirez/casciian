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

import casciian.backend.Backend;

/**
 * This class represents a single text cell on the screen.
 */
public class Cell extends CellAttributes {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * How this cell needs to be displayed if it is part of a larger glyph.
     */
    public enum Width {
        /**
         * This cell is an entire glyph on its own.
         */
        SINGLE,

        /**
         * This cell is the left half of a wide glyph.
         */
        LEFT,

        /**
         * This cell is the right half of a wide glyph.
         */
        RIGHT,
    }

    /**
     * The special "this cell is unset" (null) value.  This is the Unicode
     * "not a character" value.
     */
    private static final char UNSET_VALUE = (char) 65535;

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The character at this cell.
     */
    private int ch = ' ';

    /**
     * The display width of this cell.
     */
    private Width width = Width.SINGLE;

    /**
     * The image at this cell.
     */
    private ImageRGB image = null;

    /**
     * The image at this cell, inverted.
     */
    private ImageRGB invertedImage = null;

    /**
     * hashCode() needs to call makeImageHashCode(), which can get quite
     * expensive.
     */
    private int imageHashCode = 0;

    /**
     * The image ID, a positive integer.  This is NOT like a the hashcode.
     * Instead is an ID assigned by the logical layer that created the image,
     * so that as this image cell is passed down to the user-facing screen it
     * can be quickly be determined if it is different from another image
     * cell.
     */
    private int imageId = 0;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor sets default values of the cell to blank.
     *
     * @see #isBlank()
     * @see #reset()
     */
    public Cell() {
        // NOP
    }

    /**
     * Public constructor sets the character.  Attributes are the same as
     * default.
     *
     * @param ch character to set to
     * @see #reset()
     */
    public Cell(final int ch) {
        this.ch = ch;
    }

    /**
     * Public constructor sets the attributes.
     *
     * @param attr attributes to use
     */
    public Cell(final CellAttributes attr) {
        super(attr);
    }

    /**
     * Public constructor sets the character and attributes.
     *
     * @param ch character to set to
     * @param attr attributes to use
     */
    public Cell(final int ch, final CellAttributes attr) {
        super(attr);
        this.ch = ch;
    }

    /**
     * Public constructor creates a duplicate.
     *
     * @param cell the instance to copy
     */
    @SuppressWarnings("this-escape")
    public Cell(final Cell cell) {
        setTo(cell);
    }

    // ------------------------------------------------------------------------
    // Cell -------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set the image data for this cell.
     *
     * @param image the image for this cell
     */
    public void setImage(final ImageRGB image) {
        this.image = image;
        imageHashCode = 0;
        width = Width.SINGLE;
        this.imageId = 0;
    }

    /**
     * Set the image data for this cell.
     *
     * @param image the image for this cell
     * @param imageId the ID for this image
     */
    public void setImage(final ImageRGB image, final int imageId) {
        setImage(image);
        assert (imageId > 0);
        this.imageId = imageId;
    }

    /**
     * Get the image data for this cell.
     *
     * @return the image for this cell
     */
    public ImageRGB getImage() {
        if (invertedImage != null) {
            return invertedImage;
        }
        return image;
    }

    /**
     * Get the image data for this cell.
     *
     * @param copy if true, return a copy of the image
     * @return the image for this cell
     */
    public ImageRGB getImage(final boolean copy) {
        if (!copy) {
            return getImage();
        }
        if (image == null) {
            return null;
        }

        if (invertedImage != null) {
            return new ImageRGB(invertedImage);
        } else {
            return new ImageRGB(image);
        }
    }

    /**
     * Set the image ID.
     *
     * @param imageId the ID, a positive integer
     */
    public void setImageId(final int imageId) {
        if (imageId > 0) {
            this.imageId = imageId;
        }
    }

    /**
     * Get the image ID.
     *
     * @return the ID, or 0 if not set
     */
    public int getImageId() {
        return imageId;
    }

    /**
     * "Mix" the imageId of another Cell into this cell.  When two cells both
     * have imageId's set, the mixture of them should be a deterministic
     * combination such that one can compare a sequence of "mixed" cells and
     * know (within a high degree of likelihood) that they produced the same
     * final image.
     *
     * @param other the other cell
     */
    public void mixImageId(final Cell other) {
        if (other.imageId <= 0) {
            this.imageId = 0;
            return;
        }
        assert (other.isImage());
        this.imageId = ((this.imageId << 4) ^ other.imageId) & 0x7FFFFFFF;
    }

    /**
     * "Mix" the imageId of another operation into this cell.  When a cell
     * has its imageId set, the mixture of it and other operations should be
     * a deterministic combination such that one can compare a sequence of
     * cell + operations and know (within a high degree of likelihood) that
     * they produced the same final image.
     *
     * @param operation the operation to mix in, typically a color
     * translucent RGB that was blitted over or under this image
     */
    public void mixImageId(final int operation) {
        imageId = ((imageId << 4) ^ operation) & 0x7FFFFFFF;
    }

    /**
     * If true, this cell has image data.
     *
     * @return true if this cell is an image rather than a character with
     * attributes
     */
    public boolean isImage() {
        return image != null;
    }

    /**
     * Restore the image in this cell to its normal version, if it has one.
     */
    public void restoreImage() {
        invertedImage = null;
    }

    /**
     * If true, this cell has image data, and that data is inverted.
     *
     * @return true if this cell is an image rather than a character with
     * attributes, and the data is inverted
     */
    public boolean isInvertedImage() {
        return (image != null) && (invertedImage != null);
    }

    /**
     * Invert the image in this cell, if it has one.
     */
    public void invertImage() {
        if (image == null) {
            return;
        }
        if (invertedImage == null) {
            invertedImage = new ImageRGB(image.getWidth(), image.getHeight());

            int [] rgbArray = image.getRGB(0, 0,
                image.getWidth(), image.getHeight(), null, 0, image.getWidth());

            for (int i = 0; i < rgbArray.length; i++) {
                // Set the colors to fully inverted.
                if (rgbArray[i] != 0x00FFFFFF) {
                    rgbArray[i] ^= 0x00FFFFFF;
                }
            }
            invertedImage.setRGB(0, 0, image.getWidth(), image.getHeight(),
                rgbArray, 0, image.getWidth());
        }
    }

    /**
     * If this cell is fully covered by a single color with no transparency,
     * remove the image and set the foreground/background to that color
     * instead.
     *
     * @param opaque if true, replace with full foreground block 0x2588 (█),
     * otherwise replace with space (' ')
     * @return true if the image was a single color (and has now been erased)
     */
    public boolean checkForSingleColor(final boolean opaque) {
        if (image == null) {
            if (opaque) {
                ch = 0x2588;
                setInvisibleForeColor();
            } else {
                ch = ' ';
            }
            return true;
        }

        // Either all of the pixels are opaque (hasTransparentPixels == 2),
        // or the scan has never occurred (hasTransparentPixels == 0).  Scan
        // now.
        int [] rgbArray = image.getRGB(0, 0,
            image.getWidth(), image.getHeight(), null, 0, image.getWidth());

        if (rgbArray.length == 0) {
            return false;
        }

        int rgb = rgbArray[0];
        for (int j : rgbArray) {
            if ((rgb & 0xFFFFFF) != (j & 0xFFFFFF)) {
                return false;
            }
        }
        // No transparent pixels, and all are the same color.  No need to set
        // hasTransparentPixels = 2, because the image is going to be erased.
        unset();
        if (opaque) {
            ch = 0x2588;
        } else {
            ch = ' ';
        }
        setForeColorRGB(rgb);
        setBackColorRGB(rgb);
        return true;
    }

    /**
     * Getter for cell character.
     *
     * @return cell character
     */
    public int getChar() {
        return ch;
    }

    /**
     * Setter for cell character.
     *
     * @param ch new cell character
     */
    public void setChar(final int ch) {
        this.ch = ch;
    }

    /**
     * Set the cell character to another cell's character.
     *
     * @param other the other cell
     */
    public void setChar(final Cell other) {
        this.ch = other.ch;
    }

    /**
     * Getter for cell width.
     *
     * @return Width.SINGLE, Width.LEFT, or Width.RIGHT
     */
    public final Width getWidth() {
        return width;
    }

    /**
     * Setter for cell width.
     *
     * @param width new cell width, one of Width.SINGLE, Width.LEFT, or
     * Width.RIGHT
     */
    public final void setWidth(final Width width) {
        this.width = width;
    }

    /**
     * Get for number of display cells required to show this cell's text.
     *
     * @return 1 or 2
     */
    public int getDisplayWidth() {
        return StringUtils.width(ch);
    }

    /**
     * Compare this cell's codepoint to another codepoint.
     *
     * @param codePoint codepoint to compare to
     * @return true if codepoints are equal
     */
    public boolean isCodePoint(final int codePoint) {
        return (this.ch == codePoint);
    }

    /**
     * See if this cell is the space (' ') character.
     *
     * @return true if this cell is the space character
     */
    public boolean isSpaceChar() {
        return (ch == ' ');
    }

    /**
     * See if this cell is an emoji.
     *
     * @return true if this cell is an emoji
     */
    public boolean isEmoji() {
        return ExtendedGraphemeClusterUtils.isEmoji(ch);
    }

    /**
     * Reset this cell to a blank.
     */
    @Override
    public void reset() {
        super.reset();
        ch = ' ';
        width = Width.SINGLE;
        image = null;
        imageHashCode = 0;
        invertedImage = null;
        imageId = 0;
    }

    /**
     * UNset this cell.  It will not be equal to any other cell until it has
     * been assigned attributes and a character.
     */
    public void unset() {
        super.reset();
        ch = UNSET_VALUE;
        width = Width.SINGLE;
        image = null;
        imageHashCode = 0;
        invertedImage = null;
        imageId = 0;
    }

    /**
     * Check to see if this cell has default attributes: white foreground,
     * black background, no bold/blink/reverse/underline/protect, and a
     * character value of ' ' (space).
     *
     * @return true if this cell has default attributes.
     */
    public boolean isBlank() {
        if ((ch == UNSET_VALUE) || (image != null)) {
            return false;
        }
        if ((getForeColor().equals(casciian.bits.Color.WHITE))
            && (getBackColor().equals(casciian.bits.Color.BLACK))
            && !isBold()
            && !isBlink()
            && !isReverse()
            && !isUnderline()
            && !isProtect()
            && !isRGB()
            && !isImage()
            && (width == Width.SINGLE)
            && (ch == ' ')
        ) {
            return true;
        }
        return false;
    }

    /**
     * If true, this cell can be placed in a glyph cache somewhere so that it
     * does not have to be re-rendered many times.
     *
     * @return true if this cell can be placed in a cache
     */
    public boolean isCacheable() {
        /*
         * Heuristics, omit cells that:
         *
         *   - Are text-only and have RGB background.
         *
         *   - Are text-only Unicode block drawing characters, or space.
         *
         *   - Are image over a glyph.
         *
         *   - Are animated text cells.
         */
        if ((image == null) && (getBackColorRGB() != -1)) {
            return false;
        }
        if ((image == null) &&
            (isCodePoint(' ')
                // Full upper half - 0x2580 - ▀
                || isCodePoint(0x2580)
                // Full left half - 0x258c - ▌
                || isCodePoint(0x258c)
                // Full right half - 0x2590 - ▐
                || isCodePoint(0x2590)
                // Full bottom half - 0x2584 - ▄
                || isCodePoint(0x2584)
                // Full foreground block - 0x2588 - █
                || isCodePoint(0x2588))
        ) {
            return false;
        }
        if ((image != null) && !isCodePoint(' ')) {
            return false;
        }
        if ((image == null) && isPulse()) {
            return false;
        }
        return true;
    }

    /**
     * Comparison check.  All fields must match to return true.
     *
     * @param rhs another Cell instance
     * @return true if all fields are equal
     */
    @Override
    public boolean equals(final Object rhs) {
        if (!(rhs instanceof Cell)) {
            return false;
        }

        Cell that = (Cell) rhs;

        // Unsetted cells can never be equal.
        if ((ch == UNSET_VALUE) || (that.ch == UNSET_VALUE)) {
            return false;
        }

        // If this or rhs has an image and the other doesn't, these are not
        // equal.
        if ((image != null) && (that.image == null)) {
            return false;
        }
        if ((image == null) && (that.image != null)) {
            return false;
        }
        // If this and rhs have images, both must match.
        if ((image != null) && (that.image != null)) {
            if ((invertedImage == null) && (that.invertedImage != null)) {
                return false;
            }
            if ((invertedImage != null) && (that.invertedImage == null)) {
                return false;
            }
            // Either both objects have their image inverted, or neither do.
            if ((imageId != 0) && (that.imageId != 0)) {
                return (imageId == that.imageId);
            }
            if ((imageHashCode != 0) && (that.imageHashCode != 0)) {
                return (imageHashCode == that.imageHashCode);
            }
            return compareCellImages(this, that);
        }

        // Normal case: character and attributes must match.
        if ((ch == that.ch) && (width == that.width)) {
            return super.equals(rhs);
        }
        return false;
    }

    /**
     * Make a hashcode based on the data in image.  This is needed because
     * two visibly identical ImageRGB's can return different hash codes,
     * which breaks caching.  And we really really need caching here.
     */
    private int makeImageHashCode() {
        if (image == null) {
            return 0;
        }
        return java.util.Arrays.hashCode(image.getRGB(0, 0,
                image.getWidth(), image.getHeight(), null, 0,
                image.getWidth()));
    }

    /**
     * Compare two Cell's images for equality.  If the images are equal, then
     * the imageHashCode on both is set.
     *
     * @param first the first Cell
     * @param second the second Cell
     */
    private boolean compareCellImages(final Cell first,
        final Cell second) {

        if (first == null || second == null) {
            return false;
        }
        if (first.image == null || second.image == null) {
            return false;
        }

        int width = first.image.getWidth();
        int height = first.image.getHeight();
        if (width != second.image.getWidth()) {
            return false;
        }
        if (height != second.image.getHeight()) {
            return false;
        }

        int [] firstRgbArray = first.image.getRGB(0, 0, width, height,
            null, 0, width);
        int [] secondRgbArray = second.image.getRGB(0, 0, width, height,
            null, 0, width);

        // This should be impossible, but check anyway.
        if (firstRgbArray.length != secondRgbArray.length) {
            return false;
        }

        int hashCode = 1;
        for (int i = 0; i < firstRgbArray.length; i++) {
            if (firstRgbArray[i] != secondRgbArray[i]) {
                return false;
            }

            // Integer.hashCode() was introduced in Java 1.8.  It breaks the
            // original Casciian 1.0 dev goal for Java 1.6 compatibility.
            hashCode = 31 * hashCode + Integer.hashCode(firstRgbArray[i]);
        }
        first.imageHashCode = hashCode;
        second.imageHashCode = hashCode;
        return true;
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
        hash = (B * hash) + ch;
        hash = (B * hash) + width.hashCode();
        if (image != null) {
            if (imageHashCode == 0) {
                // Lazy-load hash code.
                imageHashCode = makeImageHashCode();
            }
            hash = (B * hash) + imageHashCode;
            hash = (B * hash) + imageId;
        }
        if (invertedImage != null) {
            hash = (B * hash) + invertedImage.hashCode();
        }
        return hash;
    }

    /**
     * Set my field values to rhs's field.
     *
     * @param rhs an instance of either Cell or CellAttributes
     */
    @Override
    public void setTo(final Object rhs) {
        super.setTo((CellAttributes) rhs);
        if (rhs instanceof Cell) {
            Cell that = (Cell) rhs;
            this.ch = that.ch;
            this.width = that.width;
            this.image = that.image;
            this.invertedImage = that.invertedImage;
            this.imageHashCode = that.imageHashCode;
            this.imageId = that.imageId;
        } else {
            this.image = null;
            this.imageHashCode = 0;
            this.imageId = 0;
            this.width = Width.SINGLE;
        }
    }

    /**
     * Set my field attr values to that's field.
     *
     * @param that a CellAttributes instance
     */
    public void setAttr(final CellAttributes that) {
        image = null;
        imageHashCode = 0;
        imageId = 0;
        super.setTo(that);
    }

    /**
     * Set my field attr values to that's field.
     *
     * @param that a CellAttributes instance
     * @param keepImage if true, retain the image data
     */
    public void setAttr(final CellAttributes that, final boolean keepImage) {
        if (!keepImage) {
            image = null;
            imageHashCode = 0;
            imageId = 0;
        }
        super.setTo(that);
    }

    /**
     * Make human-readable description of this Cell.
     *
     * @return displayable String
     */
    @Override
    public String toString() {
        return String.format("fore: %s RGB %06x back: %s RGB %06x bold: %s blink: %s ch %c",
            getForeColor(), getForeColorRGB(),
            getBackColor(), getBackColorRGB(),
            isBold(), isBlink(), ch);
    }

    /**
     * Convert this cell into an HTML entity inside a &lt;font&gt; tag.
     *
     * @return the HTML string
     */
    public String toHtml() {
        StringBuilder sb = new StringBuilder("<font ");
        sb.append(super.toHtml());
        sb.append('>');
        if (ch == ' ') {
            sb.append("&nbsp;");
        } else if (ch == '<') {
            sb.append("&lt;");
        } else if (ch == '>') {
            sb.append("&gt;");
        } else if (ch < 0x7F) {
            sb.append((char) ch);
        } else {
            sb.append(String.format("&#%d;", ch));
        }
        sb.append("</font>");
        return sb.toString();
    }

}
