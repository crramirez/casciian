/*
 * Casciian - Java Text User Interface
 *
 * Copyright 2025 Carlos Rafael Ramirez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package casciian;

import java.util.ArrayList;
import java.util.List;

import casciian.backend.Backend;
import casciian.bits.CellAttributes;
import casciian.bits.ColorTheme;
import casciian.bits.ComplexCell;
import casciian.bits.StringUtils;
import casciian.event.TCommandEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import casciian.texteditor.Document;
import casciian.texteditor.Line;
import casciian.texteditor.Word;
import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * TTextBase is the common base class for the text widgets: {@link TField}
 * (single line, editable), {@link TText} (multiple lines, read-only) and
 * {@link TEditor} (multiple lines, editable).
 *
 * <p>
 * It owns a {@link Document} text model and implements the behavior shared by
 * all of them: cursor movement, mouse and keyboard text selection, scrolling
 * of the visible area, clipboard operations, undo/redo, and rendering of the
 * document with the selection highlighted.
 * </p>
 *
 * <p>
 * Subclasses control what they support by overriding {@link #isEditable()}
 * (whether the document can be modified), and the text area geometry methods
 * {@link #getTextAreaX()}, {@link #getTextAreaY()}, {@link #getTextAreaWidth()}
 * and {@link #getTextAreaHeight()} (the region of the widget where the
 * document is drawn).
 * </p>
 */
public abstract class TTextBase extends TScrollable implements EditMenuUser {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The number of lines to scroll on mouse wheel up/down.
     */
    protected static final int wheelScrollSize = 3;

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The document being displayed/edited.
     */
    protected Document document;

    /**
     * The default color for the text.
     */
    private CellAttributes defaultColor = null;

    /**
     * The ColorTheme key used to highlight the selected text.
     */
    private String selectedColorKey = ColorTheme.TEDITOR_SELECTED;

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
     * The saved state for an undo/redo operation.
     */
    private static class SavedState {
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
     * Protected constructor.
     *
     * @param parent parent widget
     * @param text text to display
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     * @param colorKey ColorTheme key color to use for the text
     */
    @SuppressWarnings("this-escape")
    protected TTextBase(final TWidget parent, final String text, final int x,
        final int y, final int width, final int height,
        final String colorKey) {

        super(parent, x, y, width, height);

        defaultColor = getWidgetColor(colorKey);
        if (defaultColor == null) {
            defaultColor = new CellAttributes();
        }
        document = new Document(text == null ? "" : text, defaultColor);
    }

    // ------------------------------------------------------------------------
    // Subclass hooks ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Check if the document can be modified by the user.
     *
     * @return true if the text can be edited
     */
    public boolean isEditable() {
        return false;
    }

    /**
     * Get the column of the widget where the document text begins.
     *
     * @return the X position of the text area, relative to this widget
     */
    protected int getTextAreaX() {
        return 0;
    }

    /**
     * Get the row of the widget where the document text begins.
     *
     * @return the Y position of the text area, relative to this widget
     */
    protected int getTextAreaY() {
        return 0;
    }

    /**
     * Get the number of columns available to display document text.
     *
     * @return the width of the text area, never negative
     */
    protected int getTextAreaWidth() {
        return Math.max(0, getWidth());
    }

    /**
     * Get the number of rows available to display document text.
     *
     * @return the height of the text area, never negative
     */
    protected int getTextAreaHeight() {
        return Math.max(0, getHeight());
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
        if (isSingleLine()
            && (mouse.isMouseWheelUp() || mouse.isMouseWheelDown()
                || mouse.isMouseWheelLeft() || mouse.isMouseWheelRight())
        ) {
            // Single-line widgets do not scroll with the wheel.
            return;
        }
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
        if (mouse.isMouseWheelLeft()) {
            int maxColumn = document.getLineLengthMax() - 1;
            for (int i = 0; i < wheelScrollSize; i++) {
                if (leftColumn < maxColumn) {
                    leftColumn++;
                }
            }
            return;
        }
        if (mouse.isMouseWheelRight()) {
            for (int i = 0; i < wheelScrollSize; i++) {
                if (leftColumn > 0) {
                    leftColumn--;
                }
            }
            return;
        }

        if (mouse.isMouse1() && mouseOnTextArea(mouse)) {
            // Selection.
            int newLine = documentLineFor(mouse);
            int newX = documentColumnFor(mouse);

            inSelection = true;
            selectionLine0 = Math.min(newLine, document.getLineCount() - 1);
            selectionColumn0 = newX;
            selectionColumn0 = Math.max(0, Math.min(selectionColumn0,
                    document.getLine(selectionLine0).getDisplayLength() - 1));
            selectionColumn1 = selectionColumn0;
            selectionLine1 = selectionLine0;

            moveToMousePosition(mouse);
            selectionColumn1 = document.getCursor();
            selectionLine1 = document.getLineNumber();
            return;
        } else if (!mouse.isMouse1()) {
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
            int newLine = documentLineFor(mouse);
            int newSelectionLine0 = Math.min(newLine,
                document.getLineCount() - 1);
            int newSelectionColumn0 = documentColumnFor(mouse);
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

        if (mouse.isMouse1() && (inSelection || mouseOnTextArea(mouse))) {
            // Set the row and column
            int newLine = documentLineFor(mouse);
            int newX = documentColumnFor(mouse);
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

            moveToMousePosition(mouse);
            selectionColumn1 = document.getCursor();
            selectionLine1 = document.getLineNumber();
            return;
        }

        // Pass to children
        super.onMouseMotion(mouse);
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (handleClipboardKeypress(keypress)) {
            return;
        }

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
        } else if (!isSingleLine()
            && (keypress.equals(kbUp) || keypress.equals(kbShiftUp))
        ) {
            document.up();
            alignTopLine(false);
        } else if (!isSingleLine()
            && (keypress.equals(kbDown) || keypress.equals(kbShiftDown))
        ) {
            document.down();
            alignTopLine(true);
        } else if (!isSingleLine()
            && (keypress.equals(kbPgUp) || keypress.equals(kbShiftPgUp))
        ) {
            document.up(getTextAreaHeight() - 1);
            alignTopLine(false);
        } else if (!isSingleLine()
            && (keypress.equals(kbPgDn) || keypress.equals(kbShiftPgDn))
        ) {
            document.down(getTextAreaHeight() - 1);
            alignTopLine(true);
        } else if (keypress.equals(kbHome)
            || keypress.equals(kbShiftHome)
        ) {
            if (document.home()) {
                leftColumn = 0;
                setCursorX(getTextAreaX());
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
            setCursorX(getTextAreaX());
            setCursorY(getTextAreaY());
        } else if (keypress.equals(kbCtrlEnd)
            || keypress.equals(kbCtrlShiftEnd)
        ) {
            document.setLineNumber(document.getLineCount() - 1);
            document.end();
            alignTopLine(false);
        } else if (isEditable() && keypress.equals(kbIns)) {
            document.setOverwrite(!document.isOverwrite());
        } else if (isEditable() && keypress.equals(kbDel)) {
            if (inSelection) {
                deleteSelection();
                alignCursor();
            } else {
                saveUndo();
                document.del();
                alignCursor();
            }
        } else if (isEditable()
            && (keypress.equals(kbBackspace)
                || keypress.equals(kbBackspaceDel))
        ) {
            if (inSelection) {
                deleteSelection();
                alignTopLine(false);
            } else {
                saveUndo();
                document.backspace();
                alignTopLine(false);
            }
        } else if (isEditable() && keypress.equals(kbTab)
            && supportsTab()
        ) {
            deleteSelection();
            saveUndo();
            document.tab();
            alignCursor();
        } else if (isEditable() && keypress.equals(kbShiftTab)
            && supportsTab()
        ) {
            deleteSelection();
            saveUndo();
            document.backTab();
            alignCursor();
        } else if (isEditable() && keypress.equals(kbEnter)
            && supportsNewline()
        ) {
            deleteSelection();
            saveUndo();
            document.enter();
            alignTopLine(true);
        } else if (isEditable()
            && !keypress.getKey().isFnKey()
            && !keypress.getKey().isAlt()
            && !keypress.getKey().isCtrl()
            && !keypress.equals(kbEnter)
            && !keypress.equals(kbTab)
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
     * Handle the clipboard keystrokes: Ctrl-C/Ctrl-Ins (copy),
     * Ctrl-X/Shift-Del (cut) and Ctrl-V/Shift-Ins (paste).
     *
     * <p>
     * TApplication turns these keys into command events when they are bound
     * to the Edit menu, but that only happens on the primary event thread:
     * the modal dialogs that run on the secondary event thread (like
     * {@link TFileOpenBox}), and applications without an Edit menu, never
     * see those command events.  Handling the keys here makes the clipboard
     * work everywhere.
     * </p>
     *
     * @param keypress keystroke event
     * @return true if the keystroke was a clipboard operation
     */
    private boolean handleClipboardKeypress(final TKeypressEvent keypress) {
        TCommand command = null;

        if (keypress.equals(kbCtrlC) || keypress.equals(kbCtrlIns)) {
            command = cmCopy;
        } else if (keypress.equals(kbCtrlX) || keypress.equals(kbShiftDel)) {
            command = cmCut;
        } else if (keypress.equals(kbCtrlV) || keypress.equals(kbShiftIns)) {
            command = cmPaste;
        }
        if (command == null) {
            return false;
        }
        onCommand(new TCommandEvent(keypress.getBackend(), command));
        return true;
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
            if (isEditable()) {
                deleteSelection();
            }
            return;
        }

        if (command.equals(cmCopy)) {
            // Copy text to clipboard.
            copySelection();
            return;
        }

        if (command.equals(cmPaste)) {
            if (!isEditable()) {
                return;
            }
            // Delete selected text, then paste text from clipboard.
            deleteSelection();

            String text = (getClipboard() == null ? null
                : getClipboard().pasteText());
            if (text != null) {
                pasteText(text, command.getBackend());
            }
            return;
        }

        if (command.equals(cmClear)) {
            // Remove text.
            if (isEditable()) {
                deleteSelection();
            }
            return;
        }

    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the text.
     */
    @Override
    public void draw() {
        drawDocument();
    }

    // ------------------------------------------------------------------------
    // TTextBase --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Check if this widget holds a single line of text.  Single-line widgets
     * do not consume the vertical navigation keys (up, down, page up, page
     * down) or the mouse wheel, so that they can be used to move focus
     * between widgets.
     *
     * @return true if this widget holds exactly one line
     */
    protected boolean isSingleLine() {
        return false;
    }

    /**
     * Check if this widget breaks lines when Enter is pressed.  Single-line
     * widgets return false.
     *
     * @return true if Enter inserts a new line
     */
    protected boolean supportsNewline() {
        return true;
    }

    /**
     * Check if this widget inserts tabs when Tab is pressed.
     *
     * @return true if Tab inserts a tab into the document
     */
    protected boolean supportsTab() {
        return true;
    }

    /**
     * Draw the visible part of the document, highlighting the selection.
     */
    protected void drawDocument() {
        CellAttributes selectedColor = getSelectedColor();

        int areaX = getTextAreaX();
        int areaY = getTextAreaY();
        int areaWidth = getTextAreaWidth();
        int areaHeight = getTextAreaHeight();

        boolean drawSelection = hasVisibleSelection();

        int startCol = getSelectionStartColumn();
        int startRow = getSelectionStartRow();
        int endCol = getSelectionEndColumn();
        int endRow = getSelectionEndRow();

        for (int i = 0; i < areaHeight; i++) {
            // Background line
            hLineXY(areaX, areaY + i, areaWidth, getBackgroundChar(),
                defaultColor);

            if (topLine + i >= document.getLineCount()) {
                continue;
            }

            // Now draw document's line
            Line line = document.getLine(topLine + i);
            int x = 0;
            for (Word word: line.getWords()) {
                putTextXY(areaX + x - leftColumn, areaY + i,
                    getDisplayText(word), getTextColor(word));
                x += word.getDisplayLength();
                if (x - leftColumn > areaWidth) {
                    break;
                }
            }

            // Highlight selected region
            if (drawSelection) {
                if (startRow == endRow) {
                    if (topLine + i == startRow) {
                        for (x = startCol; x <= endCol; x++) {
                            putSelectionAttrXY(areaX + x - leftColumn,
                                areaY + i, selectedColor);
                        }
                    }
                } else {
                    if (topLine + i == startRow) {
                        for (x = startCol; x < line.getDisplayLength(); x++) {
                            putSelectionAttrXY(areaX + x - leftColumn,
                                areaY + i, selectedColor);
                        }
                    } else if (topLine + i == endRow) {
                        for (x = 0; x <= endCol; x++) {
                            putSelectionAttrXY(areaX + x - leftColumn,
                                areaY + i, selectedColor);
                        }
                    } else if ((topLine + i >= startRow)
                        && (topLine + i <= endRow)
                    ) {
                        for (x = 0; x < areaWidth; x++) {
                            putSelectionAttrXY(areaX + x, areaY + i,
                                selectedColor);
                        }
                    }
                }
            }
        }
    }

    /**
     * Draw a string of the document, clipped to the text area.
     *
     * @param x column relative to this widget
     * @param y row relative to this widget
     * @param text the text to draw
     * @param color the color to use
     */
    private void putTextXY(final int x, final int y, final String text,
        final CellAttributes color) {

        int areaX = getTextAreaX();
        int areaRight = areaX + getTextAreaWidth();

        int screenX = x;
        for (ComplexCell cell: StringUtils.toComplexCells(text)) {
            int cellWidth = cell.getDisplayWidth();
            if (screenX + cellWidth > areaRight) {
                // Never partially place a two-cell cluster.
                break;
            }
            if (screenX >= areaX) {
                ComplexCell placed = new ComplexCell(cell);
                placed.setAttr(color);
                putCharXY(screenX, y, placed);
            }
            screenX += cellWidth;
        }
    }

    /**
     * Apply the selection color to a cell, if it is inside the text area.
     *
     * @param x column relative to this widget
     * @param y row relative to this widget
     * @param color the color to use
     */
    private void putSelectionAttrXY(final int x, final int y,
        final CellAttributes color) {

        if ((x >= getTextAreaX()) && (x < getTextAreaX() + getTextAreaWidth())
            && (y >= getTextAreaY())
            && (y < getTextAreaY() + getTextAreaHeight())
        ) {
            putAttrXY(x, y, color);
        }
    }

    /**
     * Get the text used to draw a word of the document.  Subclasses can
     * override this to mask the contents of the document, as long as the
     * returned text has the same display width as the word (see
     * {@link TPasswordField}).
     *
     * @param word the word being drawn
     * @return the text to draw
     */
    protected String getDisplayText(final Word word) {
        return word.getText();
    }

    /**
     * Get the color used to draw a word of the document.  Subclasses can
     * override this to draw the whole document with a single color.
     *
     * @param word the word being drawn
     * @return the color to draw the word with
     */
    protected CellAttributes getTextColor(final Word word) {
        return word.getColor();
    }

    /**
     * Get the character used to fill the unused parts of the text area.
     *
     * @return the background character
     */
    protected int getBackgroundChar() {
        return ' ';
    }

    /**
     * Check if the selection covers at least one cell.
     *
     * @return true if a selection is active and not empty
     */
    protected boolean hasVisibleSelection() {
        if (!inSelection) {
            return false;
        }
        return !((selectionColumn0 == selectionColumn1)
            && (selectionLine0 == selectionLine1));
    }

    /**
     * Get the color used to highlight the selection.
     *
     * @return the selection color
     */
    protected CellAttributes getSelectedColor() {
        CellAttributes color = getWidgetColor(selectedColorKey);
        if (color == null) {
            // The theme does not know this key: fall back on the editor's
            // selection color, which every theme defines.
            color = getWidgetColor(ColorTheme.TEDITOR_SELECTED);
        }
        if (color == null) {
            color = new CellAttributes();
        }
        if ((defaultColor != null) && sameColors(color, defaultColor)) {
            // The theme uses the same color for the text and the selection:
            // the selection would be invisible, so reverse the text color.
            color = reverseColor(defaultColor);
        }
        return color;
    }

    /**
     * Check if two attributes paint the same foreground and background
     * colors, regardless of the other attributes.
     *
     * @param first the first attributes
     * @param second the second attributes
     * @return true if both paint the same colors
     */
    private static boolean sameColors(final CellAttributes first,
        final CellAttributes second) {

        return ((first.getForeColor() == second.getForeColor())
            && (first.getBackColor() == second.getBackColor())
            && (first.getForeColorRGB() == second.getForeColorRGB())
            && (first.getBackColorRGB() == second.getBackColorRGB())
            && (first.getForeColorPalette() == second.getForeColorPalette())
            && (first.getBackColorPalette() == second.getBackColorPalette()));
    }

    /**
     * Build the reverse-video version of a color: the foreground and
     * background colors are swapped.
     *
     * @param color the color to reverse
     * @return a new reversed color
     */
    private static CellAttributes reverseColor(final CellAttributes color) {
        CellAttributes result = new CellAttributes();
        result.setTo(color);
        result.setPulse(false, false, 0);

        if (color.getForeColorPalette() >= 0) {
            result.setBackColorPalette(color.getForeColorPalette());
        } else if (color.getForeColorRGB() >= 0) {
            result.setBackColorRGB(color.getForeColorRGB());
        } else {
            result.setBackColor(color.getForeColor());
        }

        if (color.getBackColorPalette() >= 0) {
            result.setForeColorPalette(color.getBackColorPalette());
        } else if (color.getBackColorRGB() >= 0) {
            result.setForeColorRGB(color.getBackColorRGB());
        } else {
            result.setForeColor(color.getBackColor());
        }
        return result;
    }

    /**
     * Set the ColorTheme key used to highlight the selection.
     *
     * @param selectedColorKey the new color key
     */
    public void setSelectedColorKey(final String selectedColorKey) {
        this.selectedColorKey = selectedColorKey;
    }

    /**
     * Get the default color used to draw the text.
     *
     * @return the default color
     */
    protected CellAttributes getDefaultColor() {
        return defaultColor;
    }

    /**
     * Set the default color used to draw the text.
     *
     * @param color the new default color
     */
    protected void setDefaultColor(final CellAttributes color) {
        if (color != null) {
            defaultColor = color;
        }
    }

    /**
     * Check if the mouse event is inside the text area.
     *
     * @param mouse the mouse event
     * @return true if the mouse is over the text area
     */
    protected boolean mouseOnTextArea(final TMouseEvent mouse) {
        return ((mouse.getX() >= getTextAreaX())
            && (mouse.getX() < getTextAreaX() + getTextAreaWidth())
            && (mouse.getY() >= getTextAreaY())
            && (mouse.getY() < getTextAreaY() + getTextAreaHeight()));
    }

    /**
     * Get the document line number the mouse is pointing at.
     *
     * @param mouse the mouse event
     * @return the 0-based document line number
     */
    protected int documentLineFor(final TMouseEvent mouse) {
        return topLine + mouse.getY() - getTextAreaY();
    }

    /**
     * Get the document display column the mouse is pointing at.
     *
     * @param mouse the mouse event
     * @return the 0-based document display column
     */
    protected int documentColumnFor(final TMouseEvent mouse) {
        return leftColumn + mouse.getX() - getTextAreaX();
    }

    /**
     * Move the document cursor to the location pointed at by the mouse.
     *
     * @param mouse the mouse event
     */
    protected void moveToMousePosition(final TMouseEvent mouse) {
        int newLine = documentLineFor(mouse);
        int newX = documentColumnFor(mouse);

        if (newLine > document.getLineCount() - 1) {
            // Go to the end
            document.setLineNumber(document.getLineCount() - 1);
            document.end();
            setCursorY(getTextAreaY()
                + Math.max(0, document.getLineCount() - 1 - topLine));
            alignCursor();
            return;
        }
        if (newLine < 0) {
            return;
        }

        document.setLineNumber(newLine);
        setCursorY(getTextAreaY() + newLine - topLine);
        if (newX >= document.getCurrentLine().getDisplayLength()) {
            document.end();
            alignCursor();
        } else {
            document.setCursor(Math.max(0, newX));
            setCursorX(getTextAreaX() + document.getCursor() - leftColumn);
        }
    }

    /**
     * Align visible area with document current line.
     *
     * @param topLineIsTop if true, make the top visible line the document
     * current line if it was off-screen.  If false, make the bottom visible
     * line the document current line.
     */
    protected void alignTopLine(final boolean topLineIsTop) {
        int line = document.getLineNumber();
        int height = Math.max(1, getTextAreaHeight());

        if ((line < topLine) || (line > topLine + height - 1)) {
            // Need to move topLine to bring document back into view.
            if (topLineIsTop) {
                topLine = line - (height - 1);
                if (topLine < 0) {
                    topLine = 0;
                }
            } else {
                topLine = line;
            }
        }

        setCursorY(getTextAreaY() + line - topLine);
        alignCursor();
    }

    /**
     * Align document current line with visible area.
     *
     * @param topLineIsTop if true, make the top visible line the document
     * current line if it was off-screen.  If false, make the bottom visible
     * line the document current line.
     */
    protected void alignDocument(final boolean topLineIsTop) {
        int line = document.getLineNumber();
        int cursor = document.getCursor();
        int height = Math.max(1, getTextAreaHeight());

        if ((line < topLine) || (line > topLine + height - 1)) {
            // Need to move document to ensure it fits view.
            if (topLineIsTop) {
                document.setLineNumber(topLine);
            } else {
                document.setLineNumber(topLine + (height - 1));
            }
            if (cursor < document.getCurrentLine().getDisplayLength()) {
                document.setCursor(cursor);
            }
        }

        setCursorY(getTextAreaY() + document.getLineNumber() - topLine);
        alignCursor();
    }

    /**
     * Align visible cursor with document cursor.
     */
    protected void alignCursor() {
        int width = Math.max(1, getTextAreaWidth());

        int desiredX = document.getCursor() - leftColumn;
        if (desiredX < 0) {
            // We need to push the screen to the left.
            leftColumn = document.getCursor();
        } else if (desiredX > width - 1) {
            // We need to push the screen to the right.
            leftColumn = document.getCursor() - (width - 1);
        }

        setCursorX(getTextAreaX() + document.getCursor() - leftColumn);
    }

    /**
     * Get the topmost visible line number.  0-based.
     *
     * @return the topmost visible line number
     */
    protected int getTopLine() {
        return topLine;
    }

    /**
     * Set the topmost visible line number.  0-based.
     *
     * @param topLine the new topmost visible line number
     */
    protected void setTopLine(final int topLine) {
        this.topLine = Math.max(0, topLine);
    }

    /**
     * Get the leftmost visible column number.  0-based.
     *
     * @return the leftmost visible column number
     */
    protected int getLeftColumn() {
        return leftColumn;
    }

    /**
     * Set the leftmost visible column number.  0-based.
     *
     * @param leftColumn the new leftmost visible column number
     */
    protected void setLeftColumn(final int leftColumn) {
        this.leftColumn = Math.max(0, leftColumn);
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
     * Get the overwrite value.
     *
     * @return true if new text will overwrite old text
     */
    public boolean isOverwrite() {
        return document.isOverwrite();
    }

    /**
     * Get the entire contents of the widget as one string.
     *
     * @return the contents
     */
    public String getText() {
        return document.getText();
    }

    /**
     * Set the entire contents of the widget from one string.
     *
     * @param text the new contents
     */
    public void setText(final String text) {
        document = new Document(text == null ? "" : text, defaultColor);
        unsetSelection();
        topLine = 0;
        leftColumn = 0;
    }

    /**
     * Insert text at the cursor position, honoring newlines and tabs.
     *
     * @param text the text to insert
     * @param backend the backend to attribute the synthetic keystrokes to,
     * may be null
     */
    protected void pasteText(final String text, final Backend backend) {

        if (!isEditable() || (text == null)) {
            return;
        }
        for (int i = 0; i < text.length(); ) {
            int ch = text.codePointAt(i);
            switch (ch) {
            case '\n':
                onKeypress(new TKeypressEvent(backend, kbEnter));
                break;
            case '\t':
                onKeypress(new TKeypressEvent(backend, kbTab));
                break;
            default:
                if ((ch >= 0x20) && (ch != 0x7F)) {
                    onKeypress(new TKeypressEvent(backend, false, 0, ch,
                            false, false, false));
                }
                break;
            }

            i += Character.charCount(ch);
        }
    }

    // ------------------------------------------------------------------------
    // Selection --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Delete text within the selection bounds.
     */
    protected void deleteSelection() {
        if (!inSelection || !isEditable()) {
            return;
        }

        saveUndo();

        inSelection = false;

        int startCol = selectionColumn0;
        int startRow = selectionLine0;
        int endCol = selectionColumn1;
        int endRow = selectionLine1;

        if (isSelectionInverted()) {
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

        if (endRow >= document.getLineCount()) {
            endRow = document.getLineCount() - 1;
        }
        if (startRow >= document.getLineCount()) {
            startRow = document.getLineCount() - 1;
        }
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
        document.setLineNumber(endRow);
        document.setCursor(endCol + 1);
        while (!((document.getLineNumber() == startRow)
                && (document.getCursor() == startCol))
        ) {
            document.backspace();
        }
        alignTopLine(true);
    }

    /**
     * Copy text within the selection bounds to clipboard.
     */
    protected void copySelection() {
        String textToCopy;
        if (!inSelection) {
            // Copy the entire buffer.
            textToCopy = getText();
        } else {
            // Copy just the selected portion.
            textToCopy = getSelection();
        }
        if (textToCopy == null) {
            return;
        }
        if (getClipboard() != null) {
            getClipboard().copyText(textToCopy);
        }
        if (getApplication() != null) {
            getApplication().getBackend().copyClipboardText(textToCopy);
        }
    }

    /**
     * Check if the selection was made bottom-to-top and/or right-to-left.
     *
     * @return true if the selection coordinates need to be swapped
     */
    private boolean isSelectionInverted() {
        return (((selectionColumn1 < selectionColumn0)
                && (selectionLine1 == selectionLine0))
            || (selectionLine1 < selectionLine0));
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
     * Select all of the text.
     */
    public void selectAll() {
        int lastRow = document.getLineCount() - 1;
        int lastColumn = Math.max(0,
            document.getLine(lastRow).getDisplayLength() - 1);
        setSelection(0, 0, lastRow, lastColumn);
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

        int startCol = getSelectionStartColumn();
        int startRow = getSelectionStartRow();
        int endCol = getSelectionEndColumn();
        int endRow = getSelectionEndRow();

        if (startRow >= document.getLineCount()) {
            return "";
        }
        if (endRow >= document.getLineCount()) {
            endRow = document.getLineCount() - 1;
            endCol = Math.max(0,
                document.getLine(endRow).getDisplayLength() - 1);
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
        return (isSelectionInverted() ? selectionLine1 : selectionLine0);
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
        return (isSelectionInverted() ? selectionColumn1 : selectionColumn0);
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
        return (isSelectionInverted() ? selectionLine0 : selectionLine1);
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
        return (isSelectionInverted() ? selectionColumn0 : selectionColumn1);
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
        if (!inSelection || !isEditable()) {
            return;
        }

        // Delete selected text, then insert the new text.
        deleteSelection();
        pasteText(text, null);
    }

    /**
     * Check if selection is available.
     *
     * @return true if a selection has been made
     */
    public boolean hasSelection() {
        return inSelection;
    }

    // ------------------------------------------------------------------------
    // Undo / redo ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set the undo level.
     *
     * @param undoLevel the maximum number of undo operations
     */
    public void setUndoLevel(final int undoLevel) {
        this.undoLevel = undoLevel;
    }

    /**
     * Save undo state.
     */
    protected void saveUndo() {
        if (!isEditable()) {
            return;
        }
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
        if (!isEditable()) {
            return;
        }
        inSelection = false;
        if ((undoListI >= 0) && (undoListI < undoList.size())) {
            SavedState state = undoList.get(undoListI);
            document = state.document.dup();
            topLine = state.topLine;
            leftColumn = state.leftColumn;
            undoListI--;
            setCursorY(getTextAreaY() + document.getLineNumber() - topLine);
            alignCursor();
        }
    }

    /**
     * Redo an edit.
     */
    public void redo() {
        if (!isEditable()) {
            return;
        }
        inSelection = false;
        if ((undoListI >= 0) && (undoListI < undoList.size())) {
            SavedState state = undoList.get(undoListI);
            document = state.document.dup();
            topLine = state.topLine;
            leftColumn = state.leftColumn;
            undoListI++;
            setCursorY(getTextAreaY() + document.getLineNumber() - topLine);
            alignCursor();
        }
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
        return isEditable();
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
        return isEditable();
    }

    /**
     * Check if the clear menu item should be enabled.
     *
     * @return true if the clear menu item should be enabled
     */
    public boolean isEditMenuClear() {
        return isEditable();
    }

}
