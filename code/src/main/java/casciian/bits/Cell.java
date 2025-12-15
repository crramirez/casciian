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
    }

    /**
     * UNset this cell.  It will not be equal to any other cell until it has
     * been assigned attributes and a character.
     */
    public void unset() {
        super.reset();
        ch = UNSET_VALUE;
        width = Width.SINGLE;
    }

    /**
     * Check to see if this cell has default attributes: white foreground,
     * black background, no bold/blink/reverse/underline/protect, and a
     * character value of ' ' (space).
     *
     * @return true if this cell has default attributes.
     */
    public boolean isBlank() {
        if (ch == UNSET_VALUE) {
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
        if (getBackColorRGB() != -1) {
            return false;
        }
        if (isCodePoint(' ')
            // Full upper half - 0x2580 - ▀
            || isCodePoint(0x2580)
            // Full left half - 0x258c - ▌
            || isCodePoint(0x258c)
            // Full right half - 0x2590 - ▐
            || isCodePoint(0x2590)
            // Full bottom half - 0x2584 - ▄
            || isCodePoint(0x2584)
            // Full foreground block - 0x2588 - █
            || isCodePoint(0x2588)
        ) {
            return false;
        }
        if (isPulse()) {
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

        // Normal case: character and attributes must match.
        if ((ch == that.ch) && (width == that.width)) {
            return super.equals(rhs);
        }
        return false;
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
        } else {
            this.width = Width.SINGLE;
        }
    }

    /**
     * Set my field attr values to that's field.
     *
     * @param that a CellAttributes instance
     */
    public void setAttr(final CellAttributes that) {
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
