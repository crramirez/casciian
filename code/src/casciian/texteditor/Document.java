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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import casciian.bits.CellAttributes;

/**
 * A Document represents a text file, as a collection of lines.
 */
public class Document {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The list of lines.
     */
    private ArrayList<Line> lines = new ArrayList<Line>();

    /**
     * The current line number being edited.  Note that this is 0-based, the
     * first line is line number 0.
     */
    private int lineNumber = 0;

    /**
     * The overwrite flag.  When true, characters overwrite data.
     */
    private boolean overwrite = false;

    /**
     * If true, the document has been edited.
     */
    private boolean dirty = false;

    /**
     * The default color for the TEditor class.
     */
    private CellAttributes defaultColor = null;

    /**
     * The text highlighter to use.
     */
    private Highlighter highlighter = new Highlighter();

    /**
     * The tab stop size.
     */
    private int tabSize = 8;

    /**
     * If true, backspace at an indent level goes back a full indent level.
     * If false, backspace always goes back one column.
     */
    private boolean backspaceUnindents = false;

    /**
     * If true, save files with tab characters.  If false, convert tabs to
     * spaces when saving files.
     */
    private boolean saveWithTabs = false;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Construct a new Document from an existing text string.
     *
     * @param str the text string
     * @param defaultColor the color for unhighlighted text
     */
    @SuppressWarnings("this-escape")
    public Document(final String str, final CellAttributes defaultColor) {
        this.defaultColor = defaultColor;

        // Set colors to resemble the Borland IDE colors, but for Java
        // language keywords.
        highlighter.setJavaColors();

        setText(str);
    }

    /**
     * Private constructor used by dup().
     */
    private Document() {
        // NOP
    }

    // ------------------------------------------------------------------------
    // Document ---------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set the entire contents of the document from one string.
     *
     * @param text the new contents
     */
    public void setText(final String text) {
        lines.clear();
        String [] rawLines = text.split("\n");
        for (int i = 0; i < rawLines.length; i++) {
            lines.add(new Line(rawLines[i], defaultColor, highlighter));
        }
    }

    /**
     * Create a duplicate instance.
     *
     * @return duplicate intance
     */
    public Document dup() {
        Document other = new Document();
        for (Line line: lines) {
            other.lines.add(line.dup());
        }
        other.lineNumber = lineNumber;
        other.overwrite = overwrite;
        other.dirty = dirty;
        other.defaultColor = defaultColor;
        if ((other.highlighter != null) && (this.highlighter != null)) {
            other.highlighter.setTo(highlighter);
        } else if (this.highlighter == null) {
            other.highlighter = this.highlighter;
        } else {
            assert (other.highlighter == null);
            other.highlighter = new Highlighter();
            other.highlighter.setTo(this.highlighter);
        }
        return other;
    }

    /**
     * Get the overwrite flag.
     *
     * @return true if addChar() overwrites data, false if it inserts
     */
    public boolean isOverwrite() {
        return overwrite;
    }

    /**
     * Get the dirty value.
     *
     * @return true if the buffer is dirty
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Unset the dirty flag.
     */
    public void setNotDirty() {
        dirty = false;
    }

    /**
     * Save contents to file.
     *
     * @param filename file to save to
     * @throws IOException if a java.io operation throws
     */
    public void saveToFilename(final String filename) throws IOException {
        OutputStreamWriter output = null;
        try {
            output = new OutputStreamWriter(new FileOutputStream(filename),
                "UTF-8");

            for (Line line: lines) {
                if (saveWithTabs) {
                    output.write(convertSpacesToTabs(line.getRawString()));
                } else {
                    output.write(line.getRawString());
                }
                output.write("\n");
            }

            dirty = false;
        }
        finally {
            if (output != null) {
                output.close();
            }
        }
    }

    /**
     * Set the overwrite flag.
     *
     * @param overwrite true if addChar() should overwrite data, false if it
     * should insert
     */
    public void setOverwrite(final boolean overwrite) {
        this.overwrite = overwrite;
    }

    /**
     * Get the current line number being edited.
     *
     * @return the line number.  Note that this is 0-based: 0 is the first
     * line.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Get the current editing line.
     *
     * @return the line
     */
    public Line getCurrentLine() {
        return lines.get(lineNumber);
    }

    /**
     * Get a specific line by number.
     *
     * @param lineNumber the line number.  Note that this is 0-based: 0 is
     * the first line.
     * @return the line
     */
    public Line getLine(final int lineNumber) {
        return lines.get(lineNumber);
    }

    /**
     * Set the current line number being edited.
     *
     * @param n the line number.  Note that this is 0-based: 0 is the first
     * line.
     */
    public void setLineNumber(final int n) {
        if ((n < 0) || (n > lines.size())) {
            throw new IndexOutOfBoundsException("Lines array size is " +
                lines.size() + ", requested index " + n);
        }
        lineNumber = n;
    }

    /**
     * Get the current cursor position of the editing line.
     *
     * @return the cursor position
     */
    public int getCursor() {
        return lines.get(lineNumber).getCursor();
    }

    /**
     * Get the character at the current cursor position in the text.
     *
     * @return the character, or -1 if the cursor is at the end of the line
     */
    public int getChar() {
        return lines.get(lineNumber).getChar();
    }

    /**
     * Set the current cursor position of the editing line.  0-based.
     *
     * @param cursor the new cursor position
     */
    public void setCursor(final int cursor) {
        if (cursor >= lines.get(lineNumber).getDisplayLength()) {
            lines.get(lineNumber).end();
        } else {
            lines.get(lineNumber).setCursor(cursor);
        }
    }

    /**
     * Increment the line number by one.  If at the last line, do nothing.
     *
     * @return true if the editing line changed
     */
    public boolean down() {
        if (lineNumber < lines.size() - 1) {
            int x = lines.get(lineNumber).getCursor();
            lineNumber++;
            if (x >= lines.get(lineNumber).getDisplayLength()) {
                lines.get(lineNumber).end();
            } else {
                lines.get(lineNumber).setCursor(x);
            }
            return true;
        }

        // Bottom line: treat down() like end().
        return end();
    }

    /**
     * Increment the line number by n.  If n would go past the last line,
     * increment only to the last line.
     *
     * @param n the number of lines to increment by
     * @return true if the editing line changed
     */
    public boolean down(final int n) {
        if (lineNumber < lines.size() - 1) {
            int x = lines.get(lineNumber).getCursor();
            lineNumber += n;
            if (lineNumber > lines.size() - 1) {
                lineNumber = lines.size() - 1;
            }
            if (x >= lines.get(lineNumber).getDisplayLength()) {
                lines.get(lineNumber).end();
            } else {
                lines.get(lineNumber).setCursor(x);
            }
            return true;
        }
        return false;
    }

    /**
     * Decrement the line number by one.  If at the first line, do nothing.
     *
     * @return true if the editing line changed
     */
    public boolean up() {
        if (lineNumber > 0) {
            int x = lines.get(lineNumber).getCursor();
            lineNumber--;
            if (x >= lines.get(lineNumber).getDisplayLength()) {
                lines.get(lineNumber).end();
            } else {
                lines.get(lineNumber).setCursor(x);
            }
            return true;
        }

        // Top line: treat up() like home().
        return home();
    }

    /**
     * Decrement the line number by n.  If n would go past the first line,
     * decrement only to the first line.
     *
     * @param n the number of lines to decrement by
     * @return true if the editing line changed
     */
    public boolean up(final int n) {
        if (lineNumber > 0) {
            int x = lines.get(lineNumber).getCursor();
            lineNumber -= n;
            if (lineNumber < 0) {
                lineNumber = 0;
            }
            if (x >= lines.get(lineNumber).getDisplayLength()) {
                lines.get(lineNumber).end();
            } else {
                lines.get(lineNumber).setCursor(x);
            }
            return true;
        }
        return false;
    }

    /**
     * Decrement the cursor by one.  If at the first column on the first
     * line, do nothing.
     *
     * @return true if the cursor position changed
     */
    public boolean left() {
        if (!lines.get(lineNumber).left()) {
            // We are on the leftmost column, wrap
            if (up()) {
                end();
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Increment the cursor by one.  If at the last column on the last line,
     * do nothing.
     *
     * @return true if the cursor position changed
     */
    public boolean right() {
        if (!lines.get(lineNumber).right()) {
            // We are on the rightmost column, wrap
            if (down()) {
                home();
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Go back to the beginning of this word if in the middle, or the
     * beginning of the previous word.
     */
    public void backwardsWord() {

        // If at the beginning of a word already, push past it.
        if ((getChar() != -1)
            && (getRawLine().length() > 0)
            && !Character.isWhitespace((char) getChar())
        ) {
            left();
        }

        // int line = lineNumber;
        while ((getChar() == -1)
            || (getRawLine().length() == 0)
            || Character.isWhitespace((char) getChar())
        ) {
            if (left() == false) {
                return;
            }
        }


        assert (getChar() != -1);

        if (!Character.isWhitespace((char) getChar())
            && (getRawLine().length() > 0)
        ) {
            // Advance until at the beginning of the document or a whitespace
            // is encountered.
            while (!Character.isWhitespace((char) getChar())) {
                int line = lineNumber;
                if (left() == false) {
                    // End of document, bail out.
                    return;
                }
                if (lineNumber != line) {
                    // We wrapped a line.  Here that counts as whitespace.
                    right();
                    return;
                }
            }
        }

        // We went one past the word, push back to the first character of
        // that word.
        right();
        return;
    }

    /**
     * Go to the beginning of the next word.
     */
    public void forwardsWord() {
        int line = lineNumber;
        while ((getChar() == -1)
            || (getRawLine().length() == 0)
        ) {
            if (right() == false) {
                return;
            }
            if (lineNumber != line) {
                // We wrapped a line.  Here that counts as whitespace.
                if (!Character.isWhitespace((char) getChar())) {
                    // We found a character immediately after the line.
                    // Done!
                    return;
                }
                // Still looking...
                line = lineNumber;
            }
        }
        assert (getChar() != -1);

        if (!Character.isWhitespace((char) getChar())
            && (getRawLine().length() > 0)
        ) {
            // Advance until at the end of the document or a whitespace is
            // encountered.
            while (!Character.isWhitespace((char) getChar())) {
                line = lineNumber;
                if (right() == false) {
                    // End of document, bail out.
                    return;
                }
                if (lineNumber != line) {
                    // We wrapped a line.  Here that counts as whitespace.
                    if (!Character.isWhitespace((char) getChar())
                        && (getRawLine().length() > 0)
                    ) {
                        // We found a character immediately after the line.
                        // Done!
                        return;
                    }
                    break;
                }
            }
        }

        while ((getChar() == -1)
            || (getRawLine().length() == 0)
        ) {
            if (right() == false) {
                return;
            }
            if (lineNumber != line) {
                // We wrapped a line.  Here that counts as whitespace.
                if (!Character.isWhitespace((char) getChar())) {
                    // We found a character immediately after the line.
                    // Done!
                    return;
                }
                // Still looking...
                line = lineNumber;
            }
        }
        assert (getChar() != -1);

        if (Character.isWhitespace((char) getChar())) {
            // Advance until at the end of the document or a non-whitespace
            // is encountered.
            while (Character.isWhitespace((char) getChar())) {
                if (right() == false) {
                    // End of document, bail out.
                    return;
                }
            }
            return;
        }

        // We wrapped the line to get here.
        return;
    }

    /**
     * Get the raw string that matches this line.
     *
     * @return the string
     */
    public String getRawLine() {
        return lines.get(lineNumber).getRawString();
    }

    /**
     * Go to the first column of this line.
     *
     * @return true if the cursor position changed
     */
    public boolean home() {
        return lines.get(lineNumber).home();
    }

    /**
     * Go to the last column of this line.
     *
     * @return true if the cursor position changed
     */
    public boolean end() {
        return lines.get(lineNumber).end();
    }

    /**
     * Delete the character under the cursor.
     */
    public void del() {
        dirty = true;
        int cursor = lines.get(lineNumber).getCursor();
        if (cursor < lines.get(lineNumber).getDisplayLength() - 1) {
            lines.get(lineNumber).del();
        } else if (lineNumber < lines.size() - 2) {
            // Join two lines
            StringBuilder newLine = new StringBuilder(lines.
                get(lineNumber).getRawString());
            newLine.append(lines.get(lineNumber + 1).getRawString());
            lines.set(lineNumber, new Line(newLine.toString(),
                    defaultColor, highlighter));
            lines.get(lineNumber).setCursor(cursor);
            lines.remove(lineNumber + 1);
        }
    }

    /**
     * Delete the character immediately preceeding the cursor.
     */
    public void backspace() {
        dirty = true;
        int cursor = lines.get(lineNumber).getCursor();
        if (cursor > 0) {
            lines.get(lineNumber).backspace(tabSize, backspaceUnindents);
        } else if (lineNumber > 0) {
            // Join two lines
            lineNumber--;
            String firstLine = lines.get(lineNumber).getRawString();
            if (firstLine.length() > 0) {
                // Backspacing combining two lines
                StringBuilder newLine = new StringBuilder(firstLine);
                newLine.append(lines.get(lineNumber + 1).getRawString());
                lines.set(lineNumber, new Line(newLine.toString(),
                        defaultColor, highlighter));
                lines.get(lineNumber).setCursor(firstLine.length());
                lines.remove(lineNumber + 1);
            } else {
                // Backspacing an empty line
                lines.remove(lineNumber);
                lines.get(lineNumber).setCursor(0);
            }
        }
    }

    /**
     * Split the current line into two, like pressing the enter key.
     */
    public void enter() {
        dirty = true;
        int cursor = lines.get(lineNumber).getRawCursor();
        String original = lines.get(lineNumber).getRawString();
        String firstLine = original.substring(0, cursor);
        String secondLine = original.substring(cursor);
        lines.add(lineNumber + 1, new Line(secondLine, defaultColor,
                highlighter));
        lines.set(lineNumber, new Line(firstLine, defaultColor, highlighter));
        lineNumber++;
        lines.get(lineNumber).home();
    }

    /**
     * Replace or insert a character at the cursor, depending on overwrite
     * flag.
     *
     * @param ch the character to replace or insert
     */
    public void addChar(final int ch) {
        dirty = true;
        if (overwrite) {
            lines.get(lineNumber).replaceChar(ch);
        } else {
            lines.get(lineNumber).addChar(ch);
        }
    }

    /**
     * Get the tab stop size.
     *
     * @return the tab stop size
     */
    public int getTabSize() {
        return tabSize;
    }

    /**
     * Set the tab stop size.
     *
     * @param tabSize the new tab stop size
     */
    public void setTabSize(final int tabSize) {
        this.tabSize = tabSize;
    }

    /**
     * Set the backspace unindent option.
     *
     * @param backspaceUnindents If true, backspace at an indent level goes
     * back a full indent level.  If false, backspace always goes back one
     * column.
     */
    public void setBackspaceUnindents(final boolean backspaceUnindents) {
        this.backspaceUnindents = backspaceUnindents;
    }

    /**
     * Set the save with tabs option.
     *
     * @param saveWithTabs If true, save files with tab characters.  If
     * false, convert tabs to spaces when saving files.
     */
    public void setSaveWithTabs(final boolean saveWithTabs) {
        this.saveWithTabs = saveWithTabs;
    }

    /**
     * Handle the tab character.
     */
    public void tab() {
        if (overwrite) {
            del();
        }
        lines.get(lineNumber).tab(tabSize);
    }

    /**
     * Handle the backtab (shift-tab) character.
     */
    public void backTab() {
        lines.get(lineNumber).backTab(tabSize);
    }

    /**
     * Get a (shallow) copy of the list of lines.
     *
     * @return the list of lines
     */
    public List<Line> getLines() {
        return new ArrayList<Line>(lines);
    }

    /**
     * Get the number of lines.
     *
     * @return the number of lines
     */
    public int getLineCount() {
        return lines.size();
    }

    /**
     * Compute the maximum line length for this document.
     *
     * @return the number of cells needed to display the longest line
     */
    public int getLineLengthMax() {
        int n = 0;
        for (Line line : lines) {
            if (line.getDisplayLength() > n) {
                n = line.getDisplayLength();
            }
        }
        return n;
    }

    /**
     * Get the current line length.
     *
     * @return the number of cells needed to display the current line
     */
    public int getLineLength() {
        return lines.get(lineNumber).getDisplayLength();
    }

    /**
     * Get the entire contents of the document as one string.
     *
     * @return the document contents
     */
    public String getText() {
        StringBuilder sb = new StringBuilder();
        for (Line line: getLines()) {
            sb.append(line.getRawString());
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Trim trailing whitespace from lines and trailing empty
     * lines from the document.
     */
    public void cleanWhitespace() {
        for (Line line: getLines()) {
            line.trimRight();
        }
        if (lines.size() == 0) {
            return;
        }
        while (lines.get(lines.size() - 1).length() == 0) {
            lines.remove(lines.size() - 1);
        }
        if (lineNumber > lines.size() - 1) {
            lineNumber = lines.size() - 1;
        }
    }

    /**
     * Set keyword highlighting.
     *
     * @param enabled if true, enable keyword highlighting
     */
    public void setHighlighting(final boolean enabled) {
        highlighter.setEnabled(enabled);
        for (Line line: getLines()) {
            line.scanLine();
        }
    }

    /**
     * Convert a string with leading spaces to a mix of tabs and spaces.
     *
     * @param string the string to convert
     */
    private String convertSpacesToTabs(final String string) {
        if (string.length() == 0) {
            return string;
        }

        int start = 0;
        while (string.charAt(start) == ' ') {
            start++;
        }
        int tabCount = start / 8;
        if (tabCount == 0) {
            return string;
        }

        StringBuilder sb = new StringBuilder(string.length());

        for (int i = 0; i < tabCount; i++) {
            sb.append('\t');
        }
        sb.append(string.substring(tabCount * 8));
        return sb.toString();
    }

    /**
     * Wrap the document text to fit in a new width.
     *
     * @param width the width to fit to
     */
    public void wrapText(final int width) {
        /*
         * Procedure:
         *
         * 1. Record the position in the string where the cursor is at as N.
         *
         * 2. Reflow the text same as TText with LEFT justification.
         *
         * 3. setLineNumber(0), home(), right() by N times.
         *
         * This is hideously inefficient, but it works.
         */

        int N = 0;
        while (left()) {
            N++;
        }

        String text = getText();
        String parBreakToken = "$$$PARAGRAPH$BREAK$$$";
        text = text.replace("\n\n", parBreakToken);
        text = text.replace("\n", " ");
        text = text.replace(parBreakToken, "\n\n");

        String [] paragraphs = text.split("\n\n");
        List<String> newLines = new ArrayList<String>();
        for (String p : paragraphs) {
            newLines.addAll(casciian.bits.StringUtils.left(p, width - 1));
            newLines.add("");
        }
        StringBuilder newText = new StringBuilder();
        for (String line: newLines) {
            newText.append(line);
            newText.append("\n");
        }
        setText(newText.toString());

        setLineNumber(0);
        home();
        for (int i = 0; i < N; i++) {
            right();
        }
    }

}
