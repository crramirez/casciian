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
package casciian.terminal;

import java.util.ArrayList;

import casciian.bits.CellAttributes;
import casciian.bits.ComplexCell;

/**
 * This represents a single line of the display buffer.
 */
public class DisplayLine {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The characters/attributes of the line.
     */
    private ArrayList<ComplexCell> chars = new ArrayList<ComplexCell>();

    /**
     * Double-width line flag.
     */
    private boolean doubleWidth = false;

    /**
     * Double height line flag.  Valid values are:
     *
     * <p><pre>
     *   0 = single height
     *   1 = top half double height
     *   2 = bottom half double height
     * </pre>
     */
    private int doubleHeight = 0;

    /**
     * DECSCNM - reverse video.  We copy the flag to the line so that
     * reverse-mode scrollback lines still show inverted colors correctly.
     */
    private boolean reverseColor = false;

    /**
     * The initial attributes for this line.
     */
    private CellAttributes attr;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor makes a duplicate (deep copy).
     *
     * @param line the line to duplicate
     */
    public DisplayLine(final DisplayLine line) {
        for (ComplexCell cell: line.chars) {
            chars.add(new ComplexCell(cell));
        }
        attr = new CellAttributes(line.attr);
        doubleWidth = line.doubleWidth;
        doubleHeight = line.doubleHeight;
        reverseColor = line.reverseColor;
    }

    /**
     * Public constructor sets everything to drawing attributes.
     *
     * @param attr current drawing attributes
     */
    public DisplayLine(final CellAttributes attr) {
        this.attr = new CellAttributes(attr);
    }

    // ------------------------------------------------------------------------
    // DisplayLine ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the Cell at a specific column.
     *
     * @param idx the character index
     * @return the Cell
     */
    public ComplexCell charAt(final int idx) {
        while (idx >= chars.size()) {
            chars.add(new ComplexCell(attr));
        }
        return new ComplexCell(chars.get(idx));
    }

    /**
     * Get the length of this line.
     *
     * @return line length
     */
    public int length() {
        return chars.size();
    }

    /**
     * Get double width flag.
     *
     * @return double width
     */
    public boolean isDoubleWidth() {
        return doubleWidth;
    }

    /**
     * Set double width flag.
     *
     * @param doubleWidth new value for double width flag
     */
    public void setDoubleWidth(final boolean doubleWidth) {
        this.doubleWidth = doubleWidth;
    }

    /**
     * Get double height flag.
     *
     * @return double height
     */
    public int getDoubleHeight() {
        return doubleHeight;
    }

    /**
     * Set double height flag.
     *
     * @param doubleHeight new value for double height flag
     */
    public void setDoubleHeight(final int doubleHeight) {
        this.doubleHeight = doubleHeight;
    }

    /**
     * Get reverse video flag.
     *
     * @return reverse video
     */
    public boolean isReverseColor() {
        return reverseColor;
    }

    /**
     * Set double-height flag.
     *
     * @param reverseColor new value for reverse video flag
     */
    public void setReverseColor(final boolean reverseColor) {
        this.reverseColor = reverseColor;
    }

    /**
     * Insert a character at the specified position.
     *
     * @param idx the character index
     * @param newCell the new ComplexCell
     */
    public void insert(final int idx, final ComplexCell newCell) {
        while (idx >= chars.size()) {
            chars.add(new ComplexCell(attr));
        }
        chars.add(idx, new ComplexCell(newCell));
    }

    /**
     * Replace character at the specified position.
     *
     * @param idx the character index
     * @param newCell the new ComplexCell
     */
    public void replace(final int idx, final ComplexCell newCell) {
        while (idx >= chars.size()) {
            chars.add(new ComplexCell(attr));
        }
        chars.get(idx).setTo(newCell);
    }

    /**
     * Set the Cell at the specified position to the blank (reset).
     *
     * @param idx the character index
     */
    public void setBlank(final int idx) {
        while (idx >= chars.size()) {
            chars.add(new ComplexCell(attr));
        }
        chars.get(idx).reset();
    }

    /**
     * Set the character (just the char, not the attributes) at the specified
     * position to ch.
     *
     * @param idx the character index
     * @param ch the new char
     */
    public void setChar(final int idx, final int ch) {
        while (idx >= chars.size()) {
            chars.add(new ComplexCell(attr));
        }
        chars.get(idx).setChar(ch);
    }

    /**
     * Set the attributes (just the attributes, not the char) at the
     * specified position to attr.
     *
     * @param idx the character index
     * @param attr the new attributes
     */
    public void setAttr(final int idx, final CellAttributes attr) {
        while (idx >= chars.size()) {
            chars.add(new ComplexCell(attr));
        }
        chars.get(idx).setAttr(attr);
    }

    /**
     * Delete character at the specified position, filling in the new
     * character on the right with newCell.
     *
     * @param idx the character index
     * @param newCell the new ComplexCell
     */
    public void delete(final int idx, final ComplexCell newCell) {
        while (idx >= chars.size()) {
            chars.add(new ComplexCell(attr));
        }
        chars.remove(idx);
    }

}
