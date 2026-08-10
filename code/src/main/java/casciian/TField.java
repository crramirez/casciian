/*
 * Casciian - Java Text User Interface
 *
 * Original work written 2013–2025 by Autumn Lamonte
 * and dedicated to the public domain via CC0.
 *
 * Modifications and maintenance:
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

import casciian.backend.Backend;
import casciian.bits.CellAttributes;
import casciian.bits.ControlPadding;
import casciian.bits.StringUtils;
import casciian.event.TCommandEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import casciian.texteditor.Word;
import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * TField implements an editable text field.
 *
 * <p>
 * It is a single-line {@link TTextBase}: it behaves like {@link TEditor}
 * restricted to one line, so the text can be selected with the mouse or with
 * shift + the navigation keys, and cut/copied/pasted.
 * </p>
 */
public class TField extends TTextBase {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Background character for unfilled-in text.
     */
    protected int backgroundChar = ' ';

    /**
     * Field text.  This mirrors the contents of the underlying document, and
     * is kept up to date after every change.
     */
    protected String text = "";

    /**
     * If true, only allow enough characters that will fit in the width.  If
     * false, allow the field to scroll to the right.
     */
    protected boolean fixed = false;

    /**
     * Current editing position within text.  This mirrors the underlying
     * document cursor, and is kept up to date after every change.
     */
    protected int position = 0;

    /**
     * Current editing position screen column number.  This mirrors the
     * underlying document cursor, and is kept up to date after every change.
     */
    protected int screenPosition = 0;

    /**
     * Beginning of visible portion.  This mirrors the leftmost visible
     * column, and is kept up to date after every change.
     */
    protected int windowStart = 0;

    /**
     * If true, new characters are inserted at position.
     */
    protected boolean insertMode = true;

    /**
     * Remember mouse state.
     */
    protected TMouseEvent mouse;

    /**
     * The action to perform when the user presses enter.
     */
    protected TAction enterAction;

    /**
     * The action to perform when the text is updated.
     */
    protected TAction updateAction;

    /**
     * The color to use when this field is active.
     */
    private String activeColorKey = "tfield.active";

    /**
     * The color to use when this field is not active.
     */
    private String inactiveColorKey = "tfield.inactive";

    /**
     * The color to use for the selected text.
     */
    private static final String SELECTED_COLOR_KEY = "tfield.selected";

    /**
     * The color used to draw the text on the last draw() call.
     */
    private CellAttributes fieldColor = null;

    /**
     * Extra left/right padding applied to the control.  The value is
     * resolved once at construction from the active
     * {@link ControlPadding} style (system property
     * {@code casciian.controls.padding}).  The editable text area is
     * drawn offset by this amount from the left edge of the widget, and
     * {@code padding} blank cells are reserved on both the left and
     * right edges.
     */
    protected final int padding;

    /**
     * Get the width of the editable text area (excluding the left and
     * right padding).  Clamped to zero in case the widget was
     * constructed narrower than {@code 2 * padding}, so that
     * substring/drawing math never goes negative.
     *
     * @return the visible text area width, never negative
     */
    protected final int textAreaWidth() {
        return Math.max(0, getWidth() - 2 * padding);
    }

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width visible text width
     * @param fixed if true, the text cannot exceed the display width
     */
    public TField(final TWidget parent, final int x, final int y,
        final int width, final boolean fixed) {

        this(parent, x, y, width, fixed, "", null, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width visible text width
     * @param fixed if true, the text cannot exceed the display width
     * @param text initial text, default is empty string
     */
    public TField(final TWidget parent, final int x, final int y,
        final int width, final boolean fixed, final String text) {

        this(parent, x, y, width, fixed, text, null, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width visible text width
     * @param fixed if true, the text cannot exceed the display width
     * @param text initial text, default is empty string
     * @param enterAction function to call when enter key is pressed
     */
    public TField(final TWidget parent, final int x, final int y,
        final int width, final boolean fixed, final String text,
        final TAction enterAction) {

        this(parent, x, y, width, fixed, text, enterAction, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width visible text width
     * @param fixed if true, the text cannot exceed the display width
     * @param text initial text, default is empty string
     * @param enterAction function to call when enter key is pressed
     * @param updateAction function to call when the text is updated
     */
    @SuppressWarnings("this-escape")
    public TField(final TWidget parent, final int x, final int y,
        final int width, final boolean fixed, final String text,
        final TAction enterAction, final TAction updateAction) {

        // Set parent and window
        super(parent, singleLine(text), x, y, width, 1, "tfield.active");

        this.padding = ControlPadding.current().getCells();

        setCursorVisible(true);
        setMouseStyle("text");
        setSelectedColorKey(SELECTED_COLOR_KEY);

        this.fixed = fixed;
        this.enterAction = enterAction;
        this.updateAction = updateAction;

        if (fixed) {
            truncateToWidth();
        }
        syncFields();
    }

    // ------------------------------------------------------------------------
    // TTextBase --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * TField allows the text to be modified.
     *
     * @return true
     */
    @Override
    public boolean isEditable() {
        return true;
    }

    /**
     * A field holds a single line.
     *
     * @return true
     */
    @Override
    protected boolean isSingleLine() {
        return true;
    }

    /**
     * A field holds a single line: Enter does not break the line.
     *
     * @return false
     */
    @Override
    protected boolean supportsNewline() {
        return false;
    }

    /**
     * A field does not insert tabs: Tab moves to the next widget.
     *
     * @return false
     */
    @Override
    protected boolean supportsTab() {
        return false;
    }

    /**
     * The text is drawn after the left padding.
     *
     * @return the X position of the text area
     */
    @Override
    protected int getTextAreaX() {
        return padding;
    }

    /**
     * The text area excludes the left and right padding.
     *
     * @return the width of the text area
     */
    @Override
    protected int getTextAreaWidth() {
        return textAreaWidth();
    }

    /**
     * A field is exactly one row tall.
     *
     * @return 1
     */
    @Override
    protected int getTextAreaHeight() {
        return 1;
    }

    /**
     * A field only shows its selection while it has the focus: an unfocused
     * field draws its text plainly, even though the selection is remembered
     * for when the focus comes back.
     *
     * @return true if the selection must be highlighted
     */
    @Override
    protected boolean hasVisibleSelection() {
        return isAbsoluteActive() && super.hasVisibleSelection();
    }

    /**
     * The text of a field is always drawn with the field color.
     *
     * @param word the word being drawn
     * @return the color to draw the word with
     */
    @Override
    protected CellAttributes getTextColor(final Word word) {
        if (fieldColor != null) {
            return fieldColor;
        }
        return super.getTextColor(word);
    }

    /**
     * Align visible cursor with document cursor.
     */
    @Override
    protected void alignCursor() {
        if (fixed) {
            setLeftColumn(0);
        } else {
            super.alignCursor();
        }
        syncFields();
        updateCursor();
    }

    /**
     * Insert text at the cursor position.  Newlines and tabs are converted
     * to spaces, since a field holds a single line.
     *
     * @param text the text to insert
     * @param backend the backend to attribute the synthetic keystrokes to,
     * may be null
     */
    @Override
    protected void pasteText(final String text, final Backend backend) {
        if (text == null) {
            return;
        }
        super.pasteText(singleLine(text), backend);
        syncFields();
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Select the whole text when the field gains the focus, so that typing
     * replaces the old value.  When the focus came from a mouse click, the
     * click that follows collapses the selection to the clicked position,
     * which is the expected behavior for a mouse.
     */
    @Override
    protected void onActivate() {
        super.onActivate();

        if (document == null) {
            // The parent activated us from TWidget's constructor: there is no
            // document to select yet.
            return;
        }
        selectAll();
    }

    /**
     * Returns true if the mouse is currently on the field.
     *
     * @return if true the mouse is currently on the field
     */
    protected boolean mouseOnField() {
        return ((mouse != null) && mouseOnTextArea(mouse));
    }

    /**
     * Handle mouse button presses.
     *
     * @param mouse mouse button event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        this.mouse = mouse;

        super.onMouseDown(mouse);
        syncFields();
    }

    /**
     * Handle mouse button releases.
     *
     * @param mouse mouse button event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        this.mouse = mouse;

        super.onMouseUp(mouse);
        syncFields();
    }

    /**
     * Handle mouse motion events.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        this.mouse = mouse;

        super.onMouseMotion(mouse);
        syncFields();
    }

    /**
     * Handle mouse double-click events: select all text in the field.
     *
     * @param mouse mouse double-click event
     */
    @Override
    public void onMouseDoubleClick(final TMouseEvent mouse) {
        this.mouse = mouse;

        if (mouse.isMouse1() && mouseOnField()) {
            selectAll();
        }
        syncFields();
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {

        if (keypress.equals(kbEnter)) {
            dispatch(true);
            return;
        }

        if (keypress.equals(kbIns)) {
            insertMode = !insertMode;
            document.setOverwrite(!insertMode);
            return;
        }

        boolean isText = isTextKeypress(keypress);
        if (isText && !canAcceptChar()) {
            // The field is full, nothing to do.
            return;
        }

        boolean modifies = isText
            || keypress.equals(kbDel)
            || keypress.equals(kbBackspace)
            || keypress.equals(kbBackspaceDel);

        super.onKeypress(keypress);
        syncFields();
        updateCursor();

        if (modifies) {
            dispatch(false);
        }
    }

    /**
     * Handle posted command events.
     *
     * @param command command event
     */
    @Override
    public void onCommand(final TCommandEvent command) {
        if (command.equals(cmCut) && !hasSelection()) {
            // Copy the whole field to clipboard, and then remove it.
            copySelection();
            setText("");
            dispatch(false);
            return;
        }

        if (command.equals(cmClear) && !hasSelection()) {
            // Remove all text.
            setText("");
            dispatch(false);
            return;
        }

        super.onCommand(command);
        syncFields();

        if (command.equals(cmCut) || command.equals(cmPaste)
            || command.equals(cmClear)
        ) {
            dispatch(false);
        }
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Override TWidget's height: we can only set height at construction
     * time.
     *
     * @param height new widget height (ignored)
     */
    @Override
    public void setHeight(final int height) {
        // Do nothing
    }

    /**
     * Draw the text field.
     */
    @Override
    public void draw() {
        fieldColor = new CellAttributes();

        if (isAbsoluteActive()) {
            fieldColor.setTo(getWidgetColor(activeColorKey));
        } else {
            fieldColor.setTo(getWidgetColor(inactiveColorKey));
        }
        // Pulse color.
        if (isActive() && (getWindow() != null) && getWindow().isActive()
            && (getApplication() != null) && getApplication().hasAnimations()
        ) {
            fieldColor.setPulse(true, false, 0);
            fieldColor.setPulseColorRGB(getScreen().getBackend().
                attrToForegroundColor(getWidgetColor("tfield.pulse")));
        }
        setDefaultColor(fieldColor);

        if (padding > 0) {
            // Paint the left and right padding cells in the field color.
            for (int i = 0; i < padding; i++) {
                putCharXY(i, 0, ' ', fieldColor);
                putCharXY(getWidth() - 1 - i, 0, ' ', fieldColor);
            }
        }

        drawDocument();

        // Fix the cursor, it will be rendered by TApplication.drawAll().
        updateCursor();
    }

    // ------------------------------------------------------------------------
    // TField -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Collapse a string to a single line: newlines and tabs become spaces.
     *
     * @param text the text, may be null
     * @return the single-line text, never null
     */
    private static String singleLine(final String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[\\r\\n\\t]", " ");
    }

    /**
     * Check if a keypress inserts a character into the field.
     *
     * @param keypress the keypress
     * @return true if this keypress adds text
     */
    private boolean isTextKeypress(final TKeypressEvent keypress) {
        return (!keypress.getKey().isFnKey()
            && !keypress.getKey().isAlt()
            && !keypress.getKey().isCtrl()
            && !keypress.equals(kbEnter)
            && !keypress.equals(kbTab)
            && !keypress.equals(kbShiftTab)
            && !keypress.equals(kbBackTab)
            && !keypress.equals(kbBackspace)
            && !keypress.equals(kbBackspaceDel)
            && !keypress.equals(kbDel)
            && !keypress.equals(kbIns)
            && !keypress.equals(kbHome)
            && !keypress.equals(kbEnd)
            && !keypress.equals(kbLeft)
            && !keypress.equals(kbRight)
            && !keypress.equals(kbUp)
            && !keypress.equals(kbDown)
            && !keypress.equals(kbPgUp)
            && !keypress.equals(kbPgDn));
    }

    /**
     * Check if another character can be added to a fixed-width field.
     *
     * @return true if a character can be inserted
     */
    private boolean canAcceptChar() {
        if (!fixed) {
            return true;
        }
        if (hasSelection()) {
            // Replacing the selection will not grow the field.
            return true;
        }
        if (StringUtils.width(getText()) < textAreaWidth()) {
            return true;
        }
        // The field is full: only an overwrite in the middle is allowed.
        return (document.isOverwrite()
            && (document.getCursor() < StringUtils.width(getText())));
    }

    /**
     * Truncate the text to fit inside a fixed-width field.
     */
    private void truncateToWidth() {
        String current = document.getLine(0).getRawString();
        if (StringUtils.width(current) > textAreaWidth()) {
            int displayWidth = 0;
            int byteIdx = 0;
            int[] codePoints = StringUtils.toCodePoints(current);
            for (int cp : codePoints) {
                int cpWidth = StringUtils.width(cp);
                if (displayWidth + cpWidth > textAreaWidth()) {
                    break;
                }
                displayWidth += cpWidth;
                byteIdx += Character.charCount(cp);
            }
            document.setText(current.substring(0, byteIdx));
        }
    }

    /**
     * Update the mirrored text/position values from the document.
     */
    private void syncFields() {
        if (fixed && (textAreaWidth() > 0)
            && (document.getCursor() > textAreaWidth() - 1)
        ) {
            // A fixed field cannot put the cursor past its last cell.
            document.setCursor(textAreaWidth() - 1);
        }
        text = document.getLine(0).getRawString();
        position = document.getCurrentLine().getRawCursor();
        screenPosition = document.getCursor();
        windowStart = getLeftColumn();
    }

    /**
     * Get field background character.
     *
     * @return background character
     */
    @Override
    public int getBackgroundChar() {
        return backgroundChar;
    }

    /**
     * Set field background character.
     *
     * @param backgroundChar the background character
     */
    public void setBackgroundChar(final int backgroundChar) {
        this.backgroundChar = backgroundChar;
    }

    /**
     * Get field text.
     *
     * @return field text
     */
    @Override
    public final String getText() {
        return document.getLine(0).getRawString();
    }

    /**
     * Set field text.
     *
     * @param text the new field text
     */
    @Override
    public void setText(final String text) {
        assert (text != null);

        super.setText(singleLine(text));
        if (fixed) {
            truncateToWidth();
        }
        setLeftColumn(0);
        syncFields();
        updateCursor();
    }

    /**
     * Dispatch to the action function.
     *
     * @param enter if true, the user pressed Enter, else this was an update
     * to the text.
     */
    protected void dispatch(final boolean enter) {
        if (enter) {
            if (enterAction != null) {
                enterAction.DO(this);
            }
        } else {
            if (updateAction != null) {
                updateAction.DO(this);
            }
        }
    }

    /**
     * Determine string position from screen position.
     *
     * @param screenPosition the position on screen
     * @return the equivalent position in text
     */
    protected int screenToTextPosition(final int screenPosition) {
        if (screenPosition == 0) {
            return 0;
        }

        String text = getText();
        int n = 0;
        for (int i = 0; i < text.length(); i++) {
            n += StringUtils.width(text.codePointAt(i));
            if (n >= screenPosition) {
                return i + 1;
            }
        }
        // screenPosition exceeds the available text length.
        throw new IndexOutOfBoundsException("screenPosition " + screenPosition +
            " exceeds available text length " + text.length());
    }

    /**
     * Update the visible cursor position to match the location of position
     * and windowStart.
     */
    protected void updateCursor() {
        int cursor = document.getCursor();
        int start = getLeftColumn();

        if ((cursor >= textAreaWidth()) && fixed) {
            setCursorX(padding + Math.max(0, textAreaWidth() - 1));
        } else if ((cursor - start >= textAreaWidth()) && !fixed) {
            setCursorX(padding + textAreaWidth() - 1);
        } else {
            setCursorX(padding + cursor - start);
        }
    }

    /**
     * Normalize windowStart such that most of the field data if visible.
     */
    protected void normalizeWindowStart() {
        if (fixed) {
            // windowStart had better be zero, there is nothing to do here.
            setLeftColumn(0);
            windowStart = 0;
            updateCursor();
            return;
        }
        setLeftColumn(document.getCursor() - (textAreaWidth() - 1));
        windowStart = getLeftColumn();

        updateCursor();
    }

    /**
     * Append char to the end of the field.
     *
     * @param ch char to append
     */
    protected void appendChar(final int ch) {
        document.end();
        document.addChar(ch);
        alignCursor();
    }

    /**
     * Insert char somewhere in the middle of the field.
     *
     * @param ch char to append
     */
    protected void insertChar(final int ch) {
        boolean overwrite = document.isOverwrite();
        document.setOverwrite(false);
        document.addChar(ch);
        document.setOverwrite(overwrite);
        alignCursor();
    }

    /**
     * Position the cursor at the first column.  The field may adjust the
     * window start to show as much of the field as possible.
     */
    public void home() {
        document.home();
        setLeftColumn(0);
        syncFields();
        updateCursor();
    }

    /**
     * Set the editing position to the last filled character.  The field may
     * adjust the window start to show as much of the field as possible.
     */
    public void end() {
        document.end();
        if (fixed) {
            setLeftColumn(0);
            if ((document.getCursor() >= textAreaWidth())
                && (document.getCursor() > 0)
            ) {
                document.setCursor(Math.max(0, textAreaWidth() - 1));
            }
        } else {
            setLeftColumn(StringUtils.width(getText()) - textAreaWidth() + 1);
        }
        syncFields();
        updateCursor();
    }

    /**
     * Set the editing position.  The field may adjust the window start to
     * show as much of the field as possible.
     *
     * @param position the new position
     * @throws IndexOutOfBoundsException if position is outside the range of
     * the available text
     */
    public void setPosition(final int position) {
        String text = getText();
        if ((position < 0) || (position >= text.length())) {
            throw new IndexOutOfBoundsException("Max length is " +
                text.length() + ", requested position " + position);
        }
        document.setCursor(StringUtils.width(text.substring(0, position)));
        normalizeWindowStart();
        syncFields();
    }

    /**
     * Set the active color key.
     *
     * @param activeColorKey ColorTheme key color to use when this field is
     * active
     */
    public void setActiveColorKey(final String activeColorKey) {
        this.activeColorKey = activeColorKey;
    }

    /**
     * Set the inactive color key.
     *
     * @param inactiveColorKey ColorTheme key color to use when this field is
     * inactive
     */
    public void setInactiveColorKey(final String inactiveColorKey) {
        this.inactiveColorKey = inactiveColorKey;
    }

    /**
     * Set the action to perform when the user presses enter.
     *
     * @param action the action to perform when the user presses enter
     */
    public void setEnterAction(final TAction action) {
        enterAction = action;
    }

    /**
     * Set the action to perform when the field is updated.
     *
     * @param action the action to perform when the field is updated
     */
    public void setUpdateAction(final TAction action) {
        updateAction = action;
    }

}
