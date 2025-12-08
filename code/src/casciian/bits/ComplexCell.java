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
 * ComplexCell represents a multi-codepoint glyph, as commonly used in color
 * emojis, accented characters, and more.
 */
public class ComplexCell extends Cell {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The codepoints at this cell.
     */
    private int [] codePoints = new int[1];

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor sets default values of the cell to blank.
     *
     * @see #isBlank()
     * @see #reset()
     */
    public ComplexCell() {
        this(' ');
    }

    /**
     * Public constructor sets the attributes.
     *
     * @param attr attributes to use
     */
    public ComplexCell(final CellAttributes attr) {
        super(attr);
        codePoints[0] = ' ';
    }

    /**
     * Public constructor sets a single codepoint and attributes.
     *
     * @param codePoint the codepoint to set to
     * @param attr attributes to use
     */
    public ComplexCell(final int codePoint, final CellAttributes attr) {
        super(codePoint, attr);
        codePoints[0] = codePoint;
    }

    /**
     * Public constructor sets a single codepoint.  Attributes are the same
     * as default.
     *
     * @param codePoint the codepoint to set to
     * @see #reset()
     */
    public ComplexCell(final int codePoint) {
        super(codePoint);
        codePoints[0] = codePoint;
    }

    /**
     * Public constructor sets multiple codepoints.  Attributes are the same
     * as default.
     *
     * @param codePoints the codepoints to set to
     * @see #reset()
     */
    public ComplexCell(final int [] codePoints) {
        super(codePoints[0]);
        this.codePoints = new int[codePoints.length];
        System.arraycopy(codePoints, 0, this.codePoints, 0, codePoints.length);
    }

    /**
     * Public constructor sets multiple codepoints and attributes.
     * as default.
     *
     * @param codePoints the codepoints to set to
     * @param attr attributes to use
     */
    public ComplexCell(final int [] codePoints, final CellAttributes attr) {
        super(codePoints[0], attr);
        this.codePoints = new int[codePoints.length];
        System.arraycopy(codePoints, 0, this.codePoints, 0, codePoints.length);
    }

    /**
     * Public constructor creates a duplicate.
     *
     * @param cell the instance to copy
     */
    @SuppressWarnings("this-escape")
    public ComplexCell(final Cell cell) {
        super(cell);

        codePoints = new int[1];
        codePoints[0] = cell.getChar();

        if (cell instanceof ComplexCell) {
            int [] otherCodePoints = ((ComplexCell) cell).codePoints;
            codePoints = new int[otherCodePoints.length];
            System.arraycopy(otherCodePoints, 0, codePoints, 0,
                otherCodePoints.length);
        }
    }

    // ------------------------------------------------------------------------
    // Cell -------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Getter for cell character.
     *
     * @return cell character
     */
    @Override
    public int getChar() {
        return codePoints[0];
    }

    /**
     * Setter for cell character.
     *
     * @param ch new cell character
     */
    @Override
    public void setChar(final int ch) {
        super.setChar(ch);
        codePoints = new int[1];
        codePoints[0] = ch;
    }

    /**
     * Set the cell character to another cell's character.
     *
     * @param other the other cell
     */
    @Override
    public void setChar(final Cell other) {
        super.setChar(other);

        codePoints = new int[1];
        codePoints[0] = other.getChar();

        if (other instanceof ComplexCell) {
            int [] otherCodePoints = ((ComplexCell) other).codePoints;
            codePoints = new int[otherCodePoints.length];
            System.arraycopy(otherCodePoints, 0, codePoints, 0,
                otherCodePoints.length);
        }
    }

    /**
     * Reset this cell to a blank.
     */
    @Override
    public void reset() {
        super.reset();
        codePoints = new int[1];
        codePoints[0] = ' ';
    }

    /**
     * UNset this cell.  It will not be equal to any other cell until it has
     * been assigned attributes and a character.
     */
    @Override
    public void unset() {
        super.reset();
        codePoints = new int[1];
        codePoints[0] = super.getChar();
    }

    /**
     * Comparison check.  All fields must match to return true.
     *
     * @param rhs another Cell instance
     * @return true if all fields are equal
     */
    @Override
    public boolean equals(final Object rhs) {
        if (!(rhs instanceof ComplexCell)) {
            return false;
        }

        ComplexCell that = (ComplexCell) rhs;
        if (this.codePoints.length != that.codePoints.length) {
            return false;
        }
        for (int i = 0; i < codePoints.length; i++) {
            if (this.codePoints[i] != that.codePoints[i]) {
                return false;
            }
        }

        return super.equals(rhs);
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
        for (int i = 0; i < codePoints.length; i++) {
            hash = (B * hash) + codePoints[i];
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
        super.setTo(rhs);

        if (rhs instanceof Cell) {
            Cell that = (Cell) rhs;
            codePoints = new int[1];
            codePoints[0] = that.getChar();
        }

        if (rhs instanceof ComplexCell) {
            ComplexCell that = (ComplexCell) rhs;
            this.codePoints = new int[that.codePoints.length];
            System.arraycopy(that.codePoints, 0, this.codePoints, 0,
                codePoints.length);
        }
    }

    /**
     * Make human-readable description of this Cell.
     *
     * @return displayable String
     */
    @Override
    public String toString() {
        if (codePoints.length == 1) {
            return new String(Character.toChars(codePoints[0]));
        }
        StringBuilder sb = new StringBuilder(codePoints.length);
        for (int i = 0; i < codePoints.length; i++) {
            sb.append(Character.toChars(codePoints[i]));
        }
        return sb.toString();
    }

    /**
     * Convert this cell into an HTML entity inside a &lt;font&gt; tag.
     *
     * @return the HTML string
     */
    @Override
    public String toHtml() {
        StringBuilder sb = new StringBuilder("<font ");
        sb.append(super.toHtml());
        sb.append('>');
        for (int i = 0; i < codePoints.length; i++) {
            int ch = codePoints[i];
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
        }
        sb.append("</font>");
        return sb.toString();
    }

    /**
     * Get for number of display cells required to show this cell's text.
     *
     * @return 1 or 2
     */
    @Override
    public int getDisplayWidth() {
        return StringUtils.width(codePoints);
    }

    /**
     * Compare this complex cell's codepoint(s) to another single codepoint.
     *
     * @param codePoint codepoint to compare to
     * @return true if this cell has one codepoint, and it is equal
     */
    @Override
    public boolean isCodePoint(final int codePoint) {
        if (codePoints.length == 1) {
            return (codePoints[0] == codePoint);
        }
        return false;
    }

    /**
     * See if this cell is an emoji.
     *
     * @return true if this cell is an emoji
     */
    @Override
    public boolean isEmoji() {
        return ExtendedGraphemeClusterUtils.isEmoji(codePoints[0]);
    }

    // ------------------------------------------------------------------------
    // ComplexCell ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set all cell codepoints.
     *
     * @param codePoints the codepoints to set to
     */
    public void setCodePoints(final int [] codePoints) {
        this.codePoints = new int[codePoints.length];
        System.arraycopy(codePoints, 0, this.codePoints, 0, codePoints.length);
    }

    /**
     * Getter for cell codepoints.
     *
     * @return a copy of the codepoints
     */
    public int [] getCodePoints() {
        int [] result = new int[codePoints.length];
        System.arraycopy(codePoints, 0, result, 0, codePoints.length);
        return result;
    }

    /**
     * Getter for number of codepoints in this cell.
     *
     * @return the number of codepoints
     */
    public int getCodePointCount() {
        return codePoints.length;
    }

    /**
     * Append one codepoint to the end of the sequence.
     *
     * @param codePoint the codepoint to add
     */
    public void add(final int codePoint) {
        int [] oldCodePoints = codePoints;
        codePoints = new int[oldCodePoints.length + 1];
        System.arraycopy(codePoints, 0, oldCodePoints, 0, oldCodePoints.length);
        codePoints[oldCodePoints.length] = codePoint;
    }

    /**
     * Convert the codepoints to a character array.
     *
     * @return an array of all of the codepoints
     */
    public char [] toCharArray() {
        int n = 0;
        for (int i = 0; i < codePoints.length; i++) {
            n += Character.charCount(codePoints[i]);
        }
        char [] result = new char[n];
        int idx = 0;
        for (int i = 0; i < codePoints.length; i++) {
            idx += Character.toChars(codePoints[i], result, idx);
        }
        return result;
    }

}
