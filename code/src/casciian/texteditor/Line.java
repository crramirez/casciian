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
package casciian.texteditor;

import java.util.ArrayList;
import java.util.List;

import casciian.bits.CellAttributes;
import casciian.bits.GraphicsChars;
import casciian.bits.StringUtils;

/**
 * A Line represents a single line of text on the screen, as a collection of
 * words.
 */
public class Line {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The list of words.
     */
    private ArrayList<Word> words = new ArrayList<Word>();

    /**
     * The default color for the TEditor class.
     */
    private CellAttributes defaultColor = null;

    /**
     * The text highlighter to use.
     */
    private Highlighter highlighter = null;

    /**
     * The current edition position on this line.
     */
    private int position = 0;

    /**
     * The current editing position screen column number.
     */
    private int screenPosition = 0;

    /**
     * The raw text of this line, what is passed to Word to determine
     * highlighting behavior.
     */
    private StringBuilder rawText;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Construct a new Line from an existing text string, and highlight
     * certain strings.
     *
     * @param str the text string
     * @param defaultColor the color for unhighlighted text
     * @param highlighter the highlighter to use
     */
    @SuppressWarnings("this-escape")
    public Line(final String str, final CellAttributes defaultColor,
        final Highlighter highlighter) {

        this.defaultColor = defaultColor;
        this.highlighter = highlighter;

        this.rawText = new StringBuilder();
        int col = 0;
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch == '\r') {
                // Strip carriage returns
                continue;
            }

            if (ch == '\t') {
                // Expand tabs
                int j = col % 8;
                do {
                    rawText.append(' ');
                    j++;
                    col++;
                } while ((j % 8) != 0);
                continue;
            }
            if ((ch <= 0x20) || (ch == 0x7F)) {
                // Replace all other C0 bytes with CP437 glyphs.
                rawText.append(GraphicsChars.CP437[(int) ch]);
                col++;
                continue;
            }

            rawText.append(ch);
            col++;
        }

        scanLine();
    }

    /**
     * Construct a new Line from an existing text string.
     *
     * @param str the text string
     * @param defaultColor the color for unhighlighted text
     */
    public Line(final String str, final CellAttributes defaultColor) {
        this(str, defaultColor, null);
    }

    /**
     * Private constructor used by dup().
     */
    private Line() {
        // NOP
    }

    // ------------------------------------------------------------------------
    // Line -------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Create a duplicate instance.
     *
     * @return duplicate intance
     */
    public Line dup() {
        Line other = new Line();
        other.defaultColor = defaultColor;
        other.highlighter = highlighter;
        other.position = position;
        other.screenPosition = screenPosition;
        other.rawText = new StringBuilder(rawText);
        other.scanLine();
        return other;
    }

    /**
     * Get a (shallow) copy of the words in this line.
     *
     * @return a copy of the word list
     */
    public List<Word> getWords() {
        return new ArrayList<Word>(words);
    }

    /**
     * Get the current cursor position in the text.
     *
     * @return the cursor position
     */
    public int getRawCursor() {
        return position;
    }

    /**
     * Get the current cursor position on screen.
     *
     * @return the cursor position
     */
    public int getCursor() {
        return screenPosition;
    }

    /**
     * Set the current cursor position.
     *
     * @param cursor the new cursor position
     */
    public void setCursor(final int cursor) {
        if ((cursor < 0)
            || ((cursor >= getDisplayLength())
                && (getDisplayLength() > 0))
        ) {
            throw new IndexOutOfBoundsException("Max length is " +
                getDisplayLength() + ", requested position " + cursor);
        }
        screenPosition = cursor;
        position = screenToTextPosition(screenPosition);
    }

    /**
     * Get the character at the current cursor position in the text.
     *
     * @return the character, or -1 if the cursor is at the end of the line
     */
    public int getChar() {
        if (position == rawText.length()) {
            return -1;
        }
        return rawText.codePointAt(position);
    }

    /**
     * Get the on-screen display length.
     *
     * @return the number of cells needed to display this line
     */
    public int getDisplayLength() {
        int n = StringUtils.width(rawText.toString());

        if (n > 0) {
            // If we have any visible characters, add one to the display so
            // that the position is immediately after the data.
            return n + 1;
        }
        return n;
    }

    /**
     * Get the raw string that matches this line.
     *
     * @return the string
     */
    public String getRawString() {
        return rawText.toString();
    }

    /**
     * Get the raw length of this line.
     *
     * @return the length of this line in characters, which may be different
     * from the number of cells needed to display it
     */
    public int length() {
        return rawText.length();
    }

    /**
     * Scan rawText and make words out of it.  Note package private access.
     */
    void scanLine() {
        words.clear();
        Word word = new Word(this.defaultColor, this.highlighter);
        words.add(word);
        for (int i = 0; i < rawText.length();) {
            int ch = rawText.codePointAt(i);
            i += Character.charCount(ch);
            Word newWord = word.addChar(ch);
            if (newWord != word) {
                words.add(newWord);
                word = newWord;
            }
        }
        for (Word w: words) {
            w.applyHighlight();
        }
    }

    /**
     * Decrement the cursor by one.  If at the first column, do nothing.
     *
     * @return true if the cursor position changed
     */
    public boolean left() {
        if (position == 0) {
            return false;
        }
        screenPosition -= StringUtils.width(rawText.codePointBefore(position));
        position -= Character.charCount(rawText.codePointBefore(position));
        return true;
    }

    /**
     * Increment the cursor by one.  If at the last column, do nothing.
     *
     * @return true if the cursor position changed
     */
    public boolean right() {
        if (getDisplayLength() == 0) {
            return false;
        }
        if (screenPosition == getDisplayLength() - 1) {
            return false;
        }
        if (position < rawText.length()) {
            screenPosition += StringUtils.width(rawText.codePointAt(position));
            position += Character.charCount(rawText.codePointAt(position));
        }
        assert (position <= rawText.length());
        return true;
    }

    /**
     * Go to the first column of this line.
     *
     * @return true if the cursor position changed
     */
    public boolean home() {
        if (position > 0) {
            position = 0;
            screenPosition = 0;
            return true;
        }
        return false;
    }

    /**
     * Go to the last column of this line.
     *
     * @return true if the cursor position changed
     */
    public boolean end() {
        if (screenPosition != getDisplayLength() - 1) {
            position = rawText.length();
            screenPosition = StringUtils.width(rawText.toString());
            return true;
        }
        return false;
    }

    /**
     * Delete the character under the cursor.
     */
    public void del() {
        assert (words.size() > 0);

        if (screenPosition < getDisplayLength()) {
            int n = Character.charCount(rawText.codePointAt(position));
            for (int i = 0; i < n; i++) {
                rawText.deleteCharAt(position);
            }
        }

        // Re-scan the line to determine the new word boundaries.
        scanLine();
    }

    /**
     * Delete the character immediately preceeding the cursor.
     *
     * @param tabSize the tab stop size
     * @param backspaceUnindents If true, backspace at an indent level goes
     * back a full indent level.  If false, backspace always goes back one
     * column.
     */
    public void backspace(final int tabSize, final boolean backspaceUnindents) {
        if ((backspaceUnindents == true)
            && (tabSize > 0)
            && (screenPosition > 0)
            && (rawText.charAt(position - 1) == ' ')
            && ((screenPosition % tabSize) == 0)
        ) {
            boolean doBackTab = true;
            for (int i = 0; i < position; i++) {
                if (rawText.charAt(i) != ' ') {
                    doBackTab = false;
                    break;
                }
            }
            if (doBackTab) {
                backTab(tabSize);
                return;
            }
        }

        if (left()) {
            del();
        }
    }

    /**
     * Insert a character at the cursor.
     *
     * @param ch the character to insert
     */
    public void addChar(final int ch) {
        if (screenPosition < getDisplayLength() - 1) {
            rawText.insert(position, Character.toChars(ch));
        } else {
            rawText.append(Character.toChars(ch));
        }
        position += Character.charCount(ch);
        screenPosition += StringUtils.width(ch);
        scanLine();
    }

    /**
     * Replace a character at the cursor.
     *
     * @param ch the character to replace
     */
    public void replaceChar(final int ch) {
        if (screenPosition < getDisplayLength() - 1) {
            // Replace character
            String oldText = rawText.toString();
            rawText = new StringBuilder(oldText.substring(0, position));
            rawText.append(Character.toChars(ch));
            rawText.append(oldText.substring(position + 1));
            screenPosition += StringUtils.width(rawText.codePointAt(position));
            position += Character.charCount(ch);
        } else {
            rawText.append(Character.toChars(ch));
            position += Character.charCount(ch);
            screenPosition += StringUtils.width(ch);
        }
        scanLine();
    }

    /**
     * Determine string position from screen position.
     *
     * @param screenPosition the position on screen
     * @return the equivalent position in text
     */
    private int screenToTextPosition(final int screenPosition) {
        if (screenPosition == 0) {
            return 0;
        }

        int n = 0;
        for (int i = 0; i < rawText.length(); i++) {
            n += StringUtils.width(rawText.codePointAt(i));
            if (n >= screenPosition) {
                return i + 1;
            }
        }
        // screenPosition exceeds the available text length.
        throw new IndexOutOfBoundsException("screenPosition " + screenPosition +
            " exceeds available text length " + rawText.length());
    }

    /**
     * Trim trailing whitespace from line, repositioning cursor if needed.
     */
    public void trimRight() {
        if (rawText.length() == 0) {
            return;
        }
        if (!Character.isWhitespace(rawText.charAt(rawText.length() - 1))) {
            return;
        }
        while ((rawText.length() > 0)
            && Character.isWhitespace(rawText.charAt(rawText.length() - 1))
        ) {
            rawText.deleteCharAt(rawText.length() - 1);
        }
        if (position >= rawText.length()) {
            end();
        }
        scanLine();
    }

    /**
     * Handle the tab character.
     *
     * @param tabSize the tab stop size
     */
    public void tab(final int tabSize) {
        if (tabSize > 0) {
            do {
                addChar(' ');
            } while ((screenPosition % tabSize) != 0);
        }
    }

    /**
     * Handle the backtab (shift-tab) character.
     *
     * @param tabSize the tab stop size
     */
    public void backTab(final int tabSize) {
        if ((tabSize > 0) && (screenPosition > 0)
            && (rawText.charAt(position - 1) == ' ')
        ) {
            do {
                backspace(tabSize, false);
            } while (((screenPosition % tabSize) != 0)
                && (screenPosition > 0)
                && (rawText.charAt(position - 1) == ' '));
        }
    }

}
