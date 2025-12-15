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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import casciian.bits.CellAttributes;
import casciian.bits.StringUtils;
import casciian.event.TCommandEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import casciian.event.TResizeEvent;
import casciian.texteditor.Document;
import casciian.texteditor.Line;
import casciian.texteditor.Word;
import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * TEditor displays an editable text document.  It is unaware of
 * scrolling behavior, but can respond to mouse and keyboard events.
 */
public class TEditor extends TWidget implements EditMenuUser {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The number of lines to scroll on mouse wheel up/down.
     */
    private static final int wheelScrollSize = 3;

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The document being edited.
     */
    protected Document document;

    /**
     * The default color for the editable text.
     */
    private CellAttributes defaultColor = null;

    /**
     * The topmost line number in the visible area.  0-based.
     */
    private int topLine = 0;

    /**
     * The leftmost column number in the visible area.  0-based.
     */
    private int leftColumn = 0;

    /**
     * If true, the mouse is dragging a selection.
     */
    private boolean inSelection = false;

    /**
     * Selection starting column.
     */
    private int selectionColumn0;

    /**
     * Selection starting line.
     */
    private int selectionLine0;

    /**
     * Selection ending column.
     */
    private int selectionColumn1;

    /**
     * Selection ending line.
     */
    private int selectionLine1;

    /**
     * The list of undo/redo states.
     */
    private List<SavedState> undoList = new ArrayList<SavedState>();

    /**
     * The position in undoList for undo/redo.
     */
    private int undoListI = 0;

    /**
     * The maximum size of the undo list.
     */
    private int undoLevel = 50;

    /**
     * An optional margin to display, or 0 for no margin.
     */
    private int margin = 0;

    /**
     * If true, automatically reflow text to fit the margin.
     */
    private boolean autoWrap = false;

    /**
     * The saved state for an undo/redo operation.
     */
    private class SavedState {
        /**
         * The Document state.
         */
        public Document document;

        /**
         * The topmost line number in the visible area.  0-based.
         */
        public int topLine = 0;

        /**
         * The leftmost column number in the visible area.  0-based.
         */
        public int leftColumn = 0;

    }

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param text text on the screen
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     */
    @SuppressWarnings("this-escape")
    public TEditor(final TWidget parent, final String text, final int x,
        final int y, final int width, final int height) {

        // Set parent and window
        super(parent, x, y, width, height);

        setCursorVisible(true);
        setMouseStyle("text");

        defaultColor = getTheme().getColor("teditor");
        document = new Document(text, defaultColor);
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
        if (mouse.isMouseWheelUp()) {
            for (int i = 0; i < wheelScrollSize; i++) {
                if (topLine > 0) {
                    topLine--;
                    alignDocument(false);
                }
            }
            return;
        }
        if (mouse.isMouseWheelDown()) {
            for (int i = 0; i < wheelScrollSize; i++) {
                if (topLine < document.getLineCount() - 1) {
                    topLine++;
                    alignDocument(true);
                }
            }
            return;
        }

        if (mouse.isMouse1()) {
            // Selection.
            int newLine = topLine + mouse.getY();
            int newX = leftColumn + mouse.getX();

            inSelection = true;
            if (newLine > document.getLineCount() - 1) {
                selectionLine0 = document.getLineCount() - 1;
            } else {
                selectionLine0 = topLine + mouse.getY();
            }
            selectionColumn0 = leftColumn + mouse.getX();
            selectionColumn0 = Math.max(0, Math.min(selectionColumn0,
                    document.getLine(selectionLine0).getDisplayLength() - 1));
            selectionColumn1 = selectionColumn0;
            selectionLine1 = selectionLine0;

            // Set the row and column
            if (newLine > document.getLineCount() - 1) {
                // Go to the end
                document.setLineNumber(document.getLineCount() - 1);
                document.end();
                if (newLine > document.getLineCount() - 1) {
                    setCursorY(document.getLineCount() - 1 - topLine);
                } else {
                    setCursorY(mouse.getY());
                }
                alignCursor();
                if (inSelection) {
                    selectionColumn1 = document.getCursor();
                    selectionLine1 = document.getLineNumber();
                }
                return;
            }

            document.setLineNumber(newLine);
            setCursorY(mouse.getY());
            if (newX >= document.getCurrentLine().getDisplayLength()) {
                document.end();
                alignCursor();
            } else {
                document.setCursor(newX);
                setCursorX(mouse.getX());
            }
            if (inSelection) {
                selectionColumn1 = document.getCursor();
                selectionLine1 = document.getLineNumber();
            }
            return;
        } else {
            inSelection = false;
        }

        // Pass to children
        super.onMouseDown(mouse);
    }

    /**
     * Handle mouse release events.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        if (mouse.isMouse1() && inSelection) {
            int newLine = topLine + mouse.getY();
            int newX = leftColumn + mouse.getX();
            int newSelectionLine0 = selectionLine0;
            int newSelectionColumn0 = selectionColumn0;

            if (newLine > document.getLineCount() - 1) {
                newSelectionLine0 = document.getLineCount() - 1;
            } else {
                newSelectionLine0 = topLine + mouse.getY();
            }
            newSelectionColumn0 = leftColumn + mouse.getX();
            newSelectionColumn0 = Math.max(0, Math.min(newSelectionColumn0,
                    document.getLine(newSelectionLine0).getDisplayLength() - 1));
            if ((newSelectionLine0 == selectionLine0)
                && (newSelectionColumn0 == selectionColumn0)
            ) {
                // The mouse clicked on a cell, but did not continue
                // selecting.
                inSelection = false;
                return;
            }
        }
        // Didn't handle the event, pass on.
        super.onMouseUp(mouse);
    }

    /**
     * Handle mouse motion events.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {

        if (mouse.isMouse1()) {
            // Set the row and column
            int newLine = topLine + mouse.getY();
            int newX = leftColumn + mouse.getX();
            if ((newLine < 0) || (newX < 0)) {
                return;
            }

            // Selection.
            if (inSelection) {
                selectionColumn1 = newX;
                selectionLine1 = newLine;
            } else {
                inSelection = true;
                selectionColumn0 = newX;
                selectionLine0 = newLine;
                selectionColumn1 = selectionColumn0;
                selectionLine1 = selectionLine0;
            }

            if (newLine > document.getLineCount() - 1) {
                // Go to the end
                document.setLineNumber(document.getLineCount() - 1);
                document.end();
                if (newLine > document.getLineCount() - 1) {
                    setCursorY(document.getLineCount() - 1 - topLine);
                } else {
                    setCursorY(mouse.getY());
                }
                alignCursor();
                if (inSelection) {
                    selectionColumn1 = document.getCursor();
                    selectionLine1 = document.getLineNumber();
                }
                return;
            }
            document.setLineNumber(newLine);
            setCursorY(mouse.getY());
            if (newX >= document.getCurrentLine().getDisplayLength()) {
                document.end();
                alignCursor();
            } else {
                document.setCursor(newX);
                setCursorX(mouse.getX());
            }
            if (inSelection) {
                selectionColumn1 = document.getCursor();
                selectionLine1 = document.getLineNumber();
            }
            return;
        }

        // Pass to children
        super.onMouseDown(mouse);
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.getKey().isShift()) {
            if (keypress.equals(kbShiftLeft)
                || keypress.equals(kbShiftRight)
                || keypress.equals(kbShiftUp)
                || keypress.equals(kbShiftDown)
                || keypress.equals(kbShiftPgDn)
                || keypress.equals(kbShiftPgUp)
                || keypress.equals(kbShiftHome)
                || keypress.equals(kbShiftEnd)
            ) {
                // Shifted navigation keys enable selection
                if (!inSelection) {
                    inSelection = true;
                    selectionColumn0 = document.getCursor();
                    selectionLine0 = document.getLineNumber();
                    selectionColumn1 = selectionColumn0;
                    selectionLine1 = selectionLine0;
                }
            }
        } else {
            if (keypress.equals(kbLeft)
                || keypress.equals(kbRight)
                || keypress.equals(kbUp)
                || keypress.equals(kbDown)
                || keypress.equals(kbPgDn)
                || keypress.equals(kbPgUp)
                || keypress.equals(kbHome)
                || keypress.equals(kbEnd)
            ) {
                // Non-shifted navigation keys disable selection.
                inSelection = false;
            }
            if ((selectionColumn0 == selectionColumn1)
                && (selectionLine0 == selectionLine1)
            ) {
                // The user clicked a spot and started typing.
                inSelection = false;
            }
        }

        if (keypress.equals(kbLeft)
            || keypress.equals(kbShiftLeft)
        ) {
            document.left();
            alignTopLine(false);
        } else if (keypress.equals(kbRight)
            || keypress.equals(kbShiftRight)
        ) {
            document.right();
            alignTopLine(true);
        } else if (keypress.equals(kbAltLeft)
            || keypress.equals(kbCtrlLeft)
            || keypress.equals(kbAltShiftLeft)
            || keypress.equals(kbCtrlShiftLeft)
        ) {
            document.backwardsWord();
            alignTopLine(false);
        } else if (keypress.equals(kbAltRight)
            || keypress.equals(kbCtrlRight)
            || keypress.equals(kbAltShiftRight)
            || keypress.equals(kbCtrlShiftRight)
        ) {
            document.forwardsWord();
            alignTopLine(true);
        } else if (keypress.equals(kbUp)
            || keypress.equals(kbShiftUp)
        ) {
            document.up();
            alignTopLine(false);
        } else if (keypress.equals(kbDown)
            || keypress.equals(kbShiftDown)
        ) {
            document.down();
            alignTopLine(true);
        } else if (keypress.equals(kbPgUp)
            || keypress.equals(kbShiftPgUp)
        ) {
            document.up(getHeight() - 1);
            alignTopLine(false);
        } else if (keypress.equals(kbPgDn)
            || keypress.equals(kbShiftPgDn)
        ) {
            document.down(getHeight() - 1);
            alignTopLine(true);
        } else if (keypress.equals(kbHome)
            || keypress.equals(kbShiftHome)
        ) {
            if (document.home()) {
                leftColumn = 0;
                if (leftColumn < 0) {
                    leftColumn = 0;
                }
                setCursorX(0);
            }
        } else if (keypress.equals(kbEnd)
            || keypress.equals(kbShiftEnd)
        ) {
            if (document.end()) {
                alignCursor();
            }
        } else if (keypress.equals(kbCtrlHome)
            || keypress.equals(kbCtrlShiftHome)
        ) {
            document.setLineNumber(0);
            document.home();
            topLine = 0;
            leftColumn = 0;
            setCursorX(0);
            setCursorY(0);
        } else if (keypress.equals(kbCtrlEnd)
            || keypress.equals(kbCtrlShiftEnd)
        ) {
            document.setLineNumber(document.getLineCount() - 1);
            document.end();
            alignTopLine(false);
        } else if (keypress.equals(kbIns)) {
            document.setOverwrite(!document.isOverwrite());
        } else if (keypress.equals(kbDel)) {
            if (inSelection) {
                deleteSelection();
                alignCursor();
            } else {
                saveUndo();
                document.del();
                alignCursor();
            }
        } else if (keypress.equals(kbBackspace)
            || keypress.equals(kbBackspaceDel)
        ) {
            if (inSelection) {
                deleteSelection();
                alignTopLine(false);
            } else {
                saveUndo();
                document.backspace();
                alignTopLine(false);
            }
        } else if (keypress.equals(kbTab)) {
            deleteSelection();
            saveUndo();
            document.tab();
            alignCursor();
        } else if (keypress.equals(kbShiftTab)) {
            deleteSelection();
            saveUndo();
            document.backTab();
            alignCursor();
        } else if (keypress.equals(kbEnter)) {
            deleteSelection();
            saveUndo();
            document.enter();
            alignTopLine(true);
        } else if (!keypress.getKey().isFnKey()
            && !keypress.getKey().isAlt()
            && !keypress.getKey().isCtrl()
        ) {
            // Plain old keystroke, process it
            deleteSelection();
            saveUndo();
            document.addChar(keypress.getKey().getChar());
            alignCursor();
        } else {
            // Pass other keys (tab etc.) on to TWidget
            super.onKeypress(keypress);
        }

        if (inSelection) {
            selectionColumn1 = document.getCursor();
            selectionLine1 = document.getLineNumber();
        }
    }

    /**
     * Method that subclasses can override to handle window/screen resize
     * events.
     *
     * @param resize resize event
     */
    @Override
    public void onResize(final TResizeEvent resize) {
        // Change my width/height, and pull the cursor in as needed.
        if (resize.getType() == TResizeEvent.Type.WIDGET) {
            setWidth(resize.getWidth());
            setHeight(resize.getHeight());
            // See if the cursor is now outside the window, and if so move
            // things.
            if (getCursorX() >= getWidth()) {
                leftColumn += getCursorX() - (getWidth() - 1);
                setCursorX(getWidth() - 1);
            }
            if (getCursorY() >= getHeight()) {
                topLine += getCursorY() - (getHeight() - 1);
                setCursorY(getHeight() - 1);
            }
        } else {
            // Let superclass handle it
            super.onResize(resize);
        }
    }

    /**
     * Handle posted command events.
     *
     * @param command command event
     */
    @Override
    public void onCommand(final TCommandEvent command) {
        if (command.equals(cmCut)) {
            // Copy text to clipboard, and then remove it.
            copySelection();
            deleteSelection();
            return;
        }

        if (command.equals(cmCopy)) {
            // Copy text to clipboard.
            copySelection();
            return;
        }

        if (command.equals(cmPaste)) {
            // Delete selected text, then paste text from clipboard.
            deleteSelection();

            String text = getClipboard().pasteText();
            if (text != null) {
                for (int i = 0; i < text.length(); ) {
                    int ch = text.codePointAt(i);
                    switch (ch) {
                    case '\n':
                        onKeypress(new TKeypressEvent(command.getBackend(),
                                kbEnter));
                        break;
                    case '\t':
                        onKeypress(new TKeypressEvent(command.getBackend(),
                                kbTab));
                        break;
                    default:
                        if ((ch >= 0x20) && (ch != 0x7F)) {
                            onKeypress(new TKeypressEvent(command.getBackend(),
                                    false, 0, ch, false, false, false));
                        }
                        break;
                    }

                    i += Character.charCount(ch);
                }
            }
            return;
        }

        if (command.equals(cmClear)) {
            // Remove text.
            deleteSelection();
            return;
        }

    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the text box.
     */
    @Override
    public void draw() {
        CellAttributes selectedColor = getTheme().getColor("teditor.selected");
        CellAttributes marginColor = getTheme().getColor("teditor.margin");

        boolean drawSelection = true;

        int startCol = selectionColumn0;
        int startRow = selectionLine0;
        int endCol = selectionColumn1;
        int endRow = selectionLine1;

        if (((selectionColumn1 < selectionColumn0)
                && (selectionLine1 == selectionLine0))
            || (selectionLine1 < selectionLine0)
        ) {
            // The user selected from bottom-to-top and/or right-to-left.
            // Reverse the coordinates for the inverted section.
            startCol = selectionColumn1;
            startRow = selectionLine1;
            endCol = selectionColumn0;
            endRow = selectionLine0;
        }
        if ((startCol == endCol) && (startRow == endRow)) {
            drawSelection = false;
        }

        for (int i = 0; i < getHeight(); i++) {
            // Background line
            getScreen().hLineXY(0, i, getWidth(), ' ', defaultColor);

            // Now draw document's line
            if (topLine + i < document.getLineCount()) {
                Line line = document.getLine(topLine + i);
                int x = 0;
                for (Word word: line.getWords()) {
                    // For now, we are cheating: draw outside the left region
                    // if needed and let screen do the clipping.
                    getScreen().putStringXY(x - leftColumn, i, word.getText(),
                        word.getColor());
                    x += word.getDisplayLength();
                    if (x - leftColumn > getWidth()) {
                        break;
                    }
                }

                // Highlight selected region
                if (inSelection && drawSelection) {
                    if (startRow == endRow) {
                        if (topLine + i == startRow) {
                            for (x = startCol; x <= endCol; x++) {
                                putAttrXY(x - leftColumn, i, selectedColor);
                            }
                        }
                    } else {
                        if (topLine + i == startRow) {
                            for (x = startCol; x < line.getDisplayLength(); x++) {
                                putAttrXY(x - leftColumn, i, selectedColor);
                            }
                        } else if (topLine + i == endRow) {
                            for (x = 0; x <= endCol; x++) {
                                putAttrXY(x - leftColumn, i, selectedColor);
                            }
                        } else if ((topLine + i >= startRow)
                            && (topLine + i <= endRow)
                        ) {
                            for (x = 0; x < getWidth(); x++) {
                                putAttrXY(x, i, selectedColor);
                            }
                        }
                    }
                }

            }
            if (margin > 0) {
                putAttrXY(margin - 1 - leftColumn, i, marginColor);
            }
        } // for (int i = 0; i < getHeight(); i++)
    }

    // ------------------------------------------------------------------------
    // TEditor ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the wrapping behavior.
     *
     * @return true if the editor automatically wraps text to fit in the
     * margin
     */
    public boolean isAutoWrap() {
        return autoWrap;
    }

    /**
     * Set the wrapping behavior.
     *
     * @param autoWrap if true, automatically wrap text to fit in the margin
     */
    public void setAutoWrap(final boolean autoWrap) {
        this.autoWrap = autoWrap;
    }

    /**
     * Set the undo level.
     *
     * @param undoLevel the maximum number of undo operations
     */
    public void setUndoLevel(final int undoLevel) {
        this.undoLevel = undoLevel;
    }

    /**
     * Set the right margin.
     *
     * @param margin column number, or 0 to disable
     */
    public void setMargin(final int margin) {
        this.margin = margin;
        if (autoWrap) {
            wrapText();
        }
    }

    /**
     * Align visible area with document current line.
     *
     * @param topLineIsTop if true, make the top visible line the document
     * current line if it was off-screen.  If false, make the bottom visible
     * line the document current line.
     */
    private void alignTopLine(final boolean topLineIsTop) {
        int line = document.getLineNumber();

        if ((line < topLine) || (line > topLine + getHeight() - 1)) {
            // Need to move topLine to bring document back into view.
            if (topLineIsTop) {
                topLine = line - (getHeight() - 1);
                if (topLine < 0) {
                    topLine = 0;
                }
                assert (topLine >= 0);
            } else {
                topLine = line;
                assert (topLine >= 0);
            }
        }

        /*
        System.err.println("line " + line + " topLine " + topLine);
        */

        // Document is in view, let's set cursorY
        assert (line >= topLine);
        setCursorY(line - topLine);
        alignCursor();
    }

    /**
     * Align document current line with visible area.
     *
     * @param topLineIsTop if true, make the top visible line the document
     * current line if it was off-screen.  If false, make the bottom visible
     * line the document current line.
     */
    private void alignDocument(final boolean topLineIsTop) {
        int line = document.getLineNumber();
        int cursor = document.getCursor();

        if ((line < topLine) || (line > topLine + getHeight() - 1)) {
            // Need to move document to ensure it fits view.
            if (topLineIsTop) {
                document.setLineNumber(topLine);
            } else {
                document.setLineNumber(topLine + (getHeight() - 1));
            }
            if (cursor < document.getCurrentLine().getDisplayLength()) {
                document.setCursor(cursor);
            }
        }

        /*
        System.err.println("getLineNumber() " + document.getLineNumber() +
            " topLine " + topLine);
        */

        // Document is in view, let's set cursorY
        setCursorY(document.getLineNumber() - topLine);
        alignCursor();
    }

    /**
     * Align visible cursor with document cursor.
     */
    private void alignCursor() {
        int width = getWidth();

        int desiredX = document.getCursor() - leftColumn;
        if (desiredX < 0) {
            // We need to push the screen to the left.
            leftColumn = document.getCursor();
        } else if (desiredX > width - 1) {
            // We need to push the screen to the right.
            leftColumn = document.getCursor() - (width - 1);
        }

        /*
        System.err.println("document cursor " + document.getCursor() +
            " leftColumn " + leftColumn);
        */


        setCursorX(document.getCursor() - leftColumn);
    }

    /**
     * Get the number of lines in the underlying Document.
     *
     * @return the number of lines
     */
    public int getLineCount() {
        return document.getLineCount();
    }

    /**
     * Get the current visible top row number.  1-based.
     *
     * @return the visible top row number.  Row 1 is the first row.
     */
    public int getVisibleRowNumber() {
        return topLine + 1;
    }

    /**
     * Set the current visible row number.  1-based.
     *
     * @param row the new visible row number.  Row 1 is the first row.
     */
    public void setVisibleRowNumber(final int row) {
        assert (row > 0);
        if ((row > 0) && (row < document.getLineCount())) {
            topLine = row - 1;
            alignDocument(true);
        }
    }

    /**
     * Get the current editing row number.  1-based.
     *
     * @return the editing row number.  Row 1 is the first row.
     */
    public int getEditingRowNumber() {
        return document.getLineNumber() + 1;
    }

    /**
     * Set the current editing row number.  1-based.
     *
     * @param row the new editing row number.  Row 1 is the first row.
     */
    public void setEditingRowNumber(final int row) {
        assert (row > 0);
        if ((row > 0) && (row < document.getLineCount())) {
            document.setLineNumber(row - 1);
            alignTopLine(true);
        }
    }

    /**
     * Set the current visible column number.  1-based.
     *
     * @return the visible column number.  Column 1 is the first column.
     */
    public int getVisibleColumnNumber() {
        return leftColumn + 1;
    }

    /**
     * Set the current visible column number.  1-based.
     *
     * @param column the new visible column number.  Column 1 is the first
     * column.
     */
    public void setVisibleColumnNumber(final int column) {
        assert (column > 0);
        if ((column > 0) && (column < document.getLineLengthMax())) {
            leftColumn = column - 1;
            alignDocument(true);
        }
    }

    /**
     * Get the current editing column number.  1-based.
     *
     * @return the editing column number.  Column 1 is the first column.
     */
    public int getEditingColumnNumber() {
        return document.getCursor() + 1;
    }

    /**
     * Set the current editing column number.  1-based.
     *
     * @param column the new editing column number.  Column 1 is the first
     * column.
     */
    public void setEditingColumnNumber(final int column) {
        if ((column > 0) && (column < document.getLineLength())) {
            document.setCursor(column - 1);
            alignCursor();
        }
    }

    /**
     * Get the maximum possible row number.  1-based.
     *
     * @return the maximum row number.  Row 1 is the first row.
     */
    public int getMaximumRowNumber() {
        return document.getLineCount() + 1;
    }

    /**
     * Get the maximum possible column number.  1-based.
     *
     * @return the maximum column number.  Column 1 is the first column.
     */
    public int getMaximumColumnNumber() {
        return document.getLineLengthMax() + 1;
    }

    /**
     * Get the current editing row plain text.  1-based.
     *
     * @param row the editing row number.  Row 1 is the first row.
     * @return the plain text of the row
     */
    public String getEditingRawLine(final int row) {
        Line line  = document.getLine(row - 1);
        return line.getRawString();
    }

    /**
     * Get the dirty value.
     *
     * @return true if the buffer is dirty
     */
    public boolean isDirty() {
        return document.isDirty();
    }

    /**
     * Unset the dirty flag.
     */
    public void setNotDirty() {
        document.setNotDirty();
    }

    /**
     * Get the overwrite value.
     *
     * @return true if new text will overwrite old text
     */
    public boolean isOverwrite() {
        return document.isOverwrite();
    }

    /**
     * Save contents to file.
     *
     * @param filename file to save to
     * @throws IOException if a java.io operation throws
     */
    public void saveToFilename(final String filename) throws IOException {
        document.saveToFilename(filename);
    }

    /**
     * Reflow the text to fit inside the margin.
     */
    public void wrapText() {
        if (margin > 0) {
            document.wrapText(margin);
            alignDocument(true);
        }
    }

    /**
     * Delete text within the selection bounds.
     */
    private void deleteSelection() {
        if (!inSelection) {
            return;
        }

        saveUndo();

        inSelection = false;

        int startCol = selectionColumn0;
        int startRow = selectionLine0;
        int endCol = selectionColumn1;
        int endRow = selectionLine1;

        /*
        System.err.println("INITIAL: " + startRow + " " + startCol + " " +
            endRow + " " + endCol + " " +
            document.getLineNumber() + " " + document.getCursor());
         */

        if (((selectionColumn1 < selectionColumn0)
                && (selectionLine1 == selectionLine0))
            || (selectionLine1 < selectionLine0)
        ) {
            // The user selected from bottom-to-top and/or right-to-left.
            // Reverse the coordinates for the inverted section.
            startCol = selectionColumn1;
            startRow = selectionLine1;
            endCol = selectionColumn0;
            endRow = selectionLine0;

            if (endRow >= document.getLineCount()) {
                // The selection started beyond EOF, trim it to EOF.
                endRow = document.getLineCount() - 1;
                endCol = document.getLine(endRow).getDisplayLength();
            } else if (endRow == document.getLineCount() - 1) {
                // The selection started beyond EOF, trim it to EOF.
                if (endCol >= document.getLine(endRow).getDisplayLength()) {
                    endCol = document.getLine(endRow).getDisplayLength() - 1;
                }
            }
        }
        /*
        System.err.println("FLIP: " + startRow + " " + startCol + " " +
            endRow + " " + endCol + " " +
            document.getLineNumber() + " " + document.getCursor());
        System.err.println(" --END: " + endRow + " " + document.getLineCount() +
            " " + document.getLine(endRow).getDisplayLength());
         */

        assert (endRow < document.getLineCount());
        if (endCol >= document.getLine(endRow).getDisplayLength()) {
            endCol = document.getLine(endRow).getDisplayLength() - 1;
        }
        if (endCol < 0) {
            endCol = 0;
        }
        if (startCol >= document.getLine(startRow).getDisplayLength()) {
            startCol = document.getLine(startRow).getDisplayLength() - 1;
        }
        if (startCol < 0) {
            startCol = 0;
        }

        // Place the cursor on the selection end, and "press backspace" until
        // the cursor matches the selection start.
        /*
        System.err.println("BEFORE: " + startRow + " " + startCol + " " +
            endRow + " " + endCol + " " +
            document.getLineNumber() + " " + document.getCursor());
         */
        document.setLineNumber(endRow);
        document.setCursor(endCol + 1);
        while (!((document.getLineNumber() == startRow)
                && (document.getCursor() == startCol))
        ) {
            /*
            System.err.println("DURING: " + startRow + " " + startCol + " " +
                endRow + " " + endCol + " " +
                document.getLineNumber() + " " + document.getCursor());
             */

            document.backspace();
        }
        alignTopLine(true);
    }

    /**
     * Copy text within the selection bounds to clipboard.
     */
    private void copySelection() {
        if (!inSelection) {
            // Copy the entire buffer.
            String textToCopy = getText();
            getClipboard().copyText(textToCopy);
            getApplication().getBackend().copyClipboardText(textToCopy);
        } else {
            // Copy just the selected portion.
            String textToCopy = getSelection();
            getClipboard().copyText(textToCopy);
            getApplication().getBackend().copyClipboardText(textToCopy);
        }
    }

    /**
     * Set the selection.
     *
     * @param startRow the starting row number.  0-based: row 0 is the first
     * row.
     * @param startColumn the starting column number.  0-based: column 0 is
     * the first column.
     * @param endRow the ending row number.  0-based: row 0 is the first row.
     * @param endColumn the ending column number.  0-based: column 0 is the
     * first column.
     */
    public void setSelection(final int startRow, final int startColumn,
        final int endRow, final int endColumn) {

        inSelection = true;
        selectionLine0 = startRow;
        selectionColumn0 = startColumn;
        selectionLine1 = endRow;
        selectionColumn1 = endColumn;
    }

    /**
     * Copy text within the selection bounds to a string.
     *
     * @return the selection as a string, or null if there is no selection
     */
    public String getSelection() {
        if (!inSelection) {
            return null;
        }

        int startCol = selectionColumn0;
        int startRow = selectionLine0;
        int endCol = selectionColumn1;
        int endRow = selectionLine1;

        if (((selectionColumn1 < selectionColumn0)
                && (selectionLine1 == selectionLine0))
            || (selectionLine1 < selectionLine0)
        ) {
            // The user selected from bottom-to-top and/or right-to-left.
            // Reverse the coordinates for the inverted section.
            startCol = selectionColumn1;
            startRow = selectionLine1;
            endCol = selectionColumn0;
            endRow = selectionLine0;
        }

        StringBuilder sb = new StringBuilder();

        if (endRow > startRow) {
            // First line
            String line = document.getLine(startRow).getRawString();
            int x = 0;
            for (int i = 0; i < line.length(); ) {
                int ch = line.codePointAt(i);

                if (x >= startCol) {
                    sb.append(Character.toChars(ch));
                }
                x += StringUtils.width(ch);
                i += Character.charCount(ch);
            }
            sb.append("\n");

            // Middle lines
            for (int y = startRow + 1; y < endRow; y++) {
                sb.append(document.getLine(y).getRawString());
                sb.append("\n");
            }

            // Final line
            line = document.getLine(endRow).getRawString();
            x = 0;
            for (int i = 0; i < line.length(); ) {
                int ch = line.codePointAt(i);

                if (x > endCol) {
                    break;
                }

                sb.append(Character.toChars(ch));
                x += StringUtils.width(ch);
                i += Character.charCount(ch);
            }
        } else {
            assert (startRow == endRow);

            // Only one line
            String line = document.getLine(startRow).getRawString();
            int x = 0;
            for (int i = 0; i < line.length(); ) {
                int ch = line.codePointAt(i);

                if ((x >= startCol) && (x <= endCol)) {
                    sb.append(Character.toChars(ch));
                }

                x += StringUtils.width(ch);
                i += Character.charCount(ch);
            }
        }
        return sb.toString();
    }

    /**
     * Get the selection starting row number.
     *
     * @return the starting row number, or -1 if there is no selection.
     * 0-based: row 0 is the first row.
     */
    public int getSelectionStartRow() {
        if (!inSelection) {
            return -1;
        }

        int startCol = selectionColumn0;
        int startRow = selectionLine0;
        int endCol = selectionColumn1;
        int endRow = selectionLine1;

        if (((selectionColumn1 < selectionColumn0)
                && (selectionLine1 == selectionLine0))
            || (selectionLine1 < selectionLine0)
        ) {
            // The user selected from bottom-to-top and/or right-to-left.
            // Reverse the coordinates for the inverted section.
            startCol = selectionColumn1;
            startRow = selectionLine1;
            endCol = selectionColumn0;
            endRow = selectionLine0;
        }
        return startRow;
    }

    /**
     * Get the selection starting column number.
     *
     * @return the starting column number, or -1 if there is no selection.
     * 0-based: column 0 is the first column.
     */
    public int getSelectionStartColumn() {
        if (!inSelection) {
            return -1;
        }

        int startCol = selectionColumn0;
        int startRow = selectionLine0;
        int endCol = selectionColumn1;
        int endRow = selectionLine1;

        if (((selectionColumn1 < selectionColumn0)
                && (selectionLine1 == selectionLine0))
            || (selectionLine1 < selectionLine0)
        ) {
            // The user selected from bottom-to-top and/or right-to-left.
            // Reverse the coordinates for the inverted section.
            startCol = selectionColumn1;
            startRow = selectionLine1;
            endCol = selectionColumn0;
            endRow = selectionLine0;
        }
        return startCol;
    }

    /**
     * Get the selection ending row number.
     *
     * @return the ending row number, or -1 if there is no selection.
     * 0-based: row 0 is the first row.
     */
    public int getSelectionEndRow() {
        if (!inSelection) {
            return -1;
        }

        int startCol = selectionColumn0;
        int startRow = selectionLine0;
        int endCol = selectionColumn1;
        int endRow = selectionLine1;

        if (((selectionColumn1 < selectionColumn0)
                && (selectionLine1 == selectionLine0))
            || (selectionLine1 < selectionLine0)
        ) {
            // The user selected from bottom-to-top and/or right-to-left.
            // Reverse the coordinates for the inverted section.
            startCol = selectionColumn1;
            startRow = selectionLine1;
            endCol = selectionColumn0;
            endRow = selectionLine0;
        }
        return endRow;
    }

    /**
     * Get the selection ending column number.
     *
     * @return the ending column number, or -1 if there is no selection.
     * 0-based: column 0 is the first column.
     */
    public int getSelectionEndColumn() {
        if (!inSelection) {
            return -1;
        }

        int startCol = selectionColumn0;
        int startRow = selectionLine0;
        int endCol = selectionColumn1;
        int endRow = selectionLine1;

        if (((selectionColumn1 < selectionColumn0)
                && (selectionLine1 == selectionLine0))
            || (selectionLine1 < selectionLine0)
        ) {
            // The user selected from bottom-to-top and/or right-to-left.
            // Reverse the coordinates for the inverted section.
            startCol = selectionColumn1;
            startRow = selectionLine1;
            endCol = selectionColumn0;
            endRow = selectionLine0;
        }
        return endCol;
    }

    /**
     * Unset the selection.
     */
    public void unsetSelection() {
        inSelection = false;
    }

    /**
     * Replace whatever is being selected with new text.  If not in
     * selection, nothing is replaced.
     *
     * @param text the new replacement text
     */
    public void replaceSelection(final String text) {
        if (!inSelection) {
            return;
        }

        // Delete selected text, then paste text from clipboard.
        deleteSelection();

        for (int i = 0; i < text.length(); ) {
            int ch = text.codePointAt(i);
            switch (ch) {
            case '\n':
                onKeypress(new TKeypressEvent(null, kbEnter));
                break;
            case '\t':
                onKeypress(new TKeypressEvent(null, kbTab));
                break;
            default:
                if ((ch >= 0x20) && (ch != 0x7F)) {
                    onKeypress(new TKeypressEvent(null, false, 0, ch,
                            false, false, false));
                }
                break;
            }
            i += Character.charCount(ch);
        }
    }

    /**
     * Check if selection is available.
     *
     * @return true if a selection has been made
     */
    public boolean hasSelection() {
        return inSelection;
    }

    /**
     * Get the entire contents of the editor as one string.
     *
     * @return the editor contents
     */
    public String getText() {
        return document.getText();
    }

    /**
     * Set the entire contents of the editor from one string.
     *
     * @param text the new contents
     */
    public void setText(final String text) {
        document = new Document(text, defaultColor);
        unsetSelection();
        topLine = 0;
        leftColumn = 0;
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
        return true;
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
        return true;
    }

    /**
     * Check if the clear menu item should be enabled.
     *
     * @return true if the clear menu item should be enabled
     */
    public boolean isEditMenuClear() {
        return true;
    }

    /**
     * Save undo state.
     */
    private void saveUndo() {
        SavedState state = new SavedState();
        state.document = document.dup();
        state.topLine = topLine;
        state.leftColumn = leftColumn;
        if (undoLevel > 0) {
            while (undoList.size() > undoLevel) {
                undoList.remove(0);
            }
        }
        undoList.add(state);
        undoListI = undoList.size() - 1;
    }

    /**
     * Undo an edit.
     */
    public void undo() {
        inSelection = false;
        if ((undoListI >= 0) && (undoListI < undoList.size())) {
            SavedState state = undoList.get(undoListI);
            document = state.document.dup();
            topLine = state.topLine;
            leftColumn = state.leftColumn;
            undoListI--;
            setCursorY(document.getLineNumber() - topLine);
            alignCursor();
        }
    }

    /**
     * Redo an edit.
     */
    public void redo() {
        inSelection = false;
        if ((undoListI >= 0) && (undoListI < undoList.size())) {
            SavedState state = undoList.get(undoListI);
            document = state.document.dup();
            topLine = state.topLine;
            leftColumn = state.leftColumn;
            undoListI++;
            setCursorY(document.getLineNumber() - topLine);
            alignCursor();
        }
    }

    /**
     * Trim trailing whitespace from lines and trailing empty
     * lines from the document.
     */
    public void cleanWhitespace() {
        document.cleanWhitespace();
        setCursorY(document.getLineNumber() - topLine);
        alignCursor();
    }

    /**
     * Set keyword highlighting.
     *
     * @param enabled if true, enable keyword highlighting
     */
    public void setHighlighting(final boolean enabled) {
        document.setHighlighting(enabled);
    }

}
