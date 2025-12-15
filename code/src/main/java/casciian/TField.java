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

import casciian.bits.CellAttributes;
import casciian.bits.GraphicsChars;
import casciian.bits.StringUtils;
import casciian.event.TCommandEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * TField implements an editable text field.
 */
public class TField extends TWidget implements EditMenuUser {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Background character for unfilled-in text.
     */
    protected int backgroundChar = GraphicsChars.HATCH;

    /**
     * Field text.
     */
    protected String text = "";

    /**
     * If true, only allow enough characters that will fit in the width.  If
     * false, allow the field to scroll to the right.
     */
    protected boolean fixed = false;

    /**
     * Current editing position within text.
     */
    protected int position = 0;

    /**
     * Current editing position screen column number.
     */
    protected int screenPosition = 0;

    /**
     * Beginning of visible portion.
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
        super(parent, x, y, width, 1);

        setCursorVisible(true);
        setMouseStyle("text");

        this.fixed = fixed;
        this.text = text;
        this.enterAction = enterAction;
        this.updateAction = updateAction;
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Returns true if the mouse is currently on the field.
     *
     * @return if true the mouse is currently on the field
     */
    protected boolean mouseOnField() {
        int rightEdge = getWidth() - 1;
        if ((mouse != null)
            && (mouse.getY() == 0)
            && (mouse.getX() >= 0)
            && (mouse.getX() <= rightEdge)
        ) {
            return true;
        }
        return false;
    }

    /**
     * Handle mouse button presses.
     *
     * @param mouse mouse button event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        this.mouse = mouse;

        if ((mouseOnField()) && (mouse.isMouse1())) {
            // Move cursor
            int deltaX = mouse.getX() - getCursorX();
            screenPosition += deltaX;
            if (screenPosition > StringUtils.width(text)) {
                screenPosition = StringUtils.width(text);
            }
            position = screenToTextPosition(screenPosition);
            updateCursor();
            return;
        }
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {

        if (keypress.equals(kbLeft)) {
            if (position > 0) {
                screenPosition -= StringUtils.width(text.codePointBefore(position));
                position -= Character.charCount(text.codePointBefore(position));
            }
            if (fixed == false) {
                if ((screenPosition == windowStart) && (windowStart > 0)) {
                    windowStart -= StringUtils.width(text.codePointAt(
                        screenToTextPosition(windowStart)));
                }
            }
            normalizeWindowStart();
            return;
        }

        if (keypress.equals(kbRight)) {
            if (position < text.length()) {
                int lastPosition = position;
                screenPosition += StringUtils.width(text.codePointAt(position));
                position += Character.charCount(text.codePointAt(position));
                if (fixed == true) {
                    if (screenPosition == getWidth()) {
                        screenPosition--;
                        position -= Character.charCount(text.codePointAt(lastPosition));
                    }
                } else {
                    while ((screenPosition - windowStart +
                            StringUtils.width(text.codePointAt(text.length() - 1)))
                        > getWidth()
                    ) {
                        windowStart += StringUtils.width(text.codePointAt(
                            screenToTextPosition(windowStart)));
                    }
                }
            }
            assert (position <= text.length());
            return;
        }

        if (keypress.equals(kbEnter)) {
            dispatch(true);
            return;
        }

        if (keypress.equals(kbIns)) {
            insertMode = !insertMode;
            return;
        }
        if (keypress.equals(kbHome)) {
            home();
            return;
        }

        if (keypress.equals(kbEnd)) {
            end();
            return;
        }

        if (keypress.equals(kbDel)) {
            if ((text.length() > 0) && (position < text.length())) {
                text = text.substring(0, position)
                        + text.substring(position + 1);
                screenPosition = StringUtils.width(text.substring(0, position));
            }
            dispatch(false);
            return;
        }

        if (keypress.equals(kbBackspace) || keypress.equals(kbBackspaceDel)) {
            if (position > 0) {
                position -= Character.charCount(text.codePointBefore(position));
                text = text.substring(0, position)
                        + text.substring(position + 1);
                screenPosition = StringUtils.width(text.substring(0, position));
            }
            if (fixed == false) {
                if ((screenPosition >= windowStart)
                    && (windowStart > 0)
                ) {
                    windowStart -= StringUtils.width(text.codePointAt(
                        screenToTextPosition(windowStart)));
                }
            }
            dispatch(false);
            normalizeWindowStart();
            return;
        }

        if (!keypress.getKey().isFnKey()
            && !keypress.getKey().isAlt()
            && !keypress.getKey().isCtrl()
        ) {
            // Plain old keystroke, process it
            if ((position == text.length())
                && (StringUtils.width(text) < getWidth())) {

                // Append case
                appendChar(keypress.getKey().getChar());
            } else if ((position < text.length())
                && (StringUtils.width(text) < getWidth())) {

                // Overwrite or insert a character
                if (insertMode == false) {
                    // Replace character
                    text = text.substring(0, position)
                            + codePointString(keypress.getKey().getChar())
                            + text.substring(position + 1);
                    screenPosition += StringUtils.width(text.codePointAt(position));
                    position += Character.charCount(keypress.getKey().getChar());
                } else {
                    // Insert character
                    insertChar(keypress.getKey().getChar());
                }
            } else if ((position < text.length())
                && (StringUtils.width(text) >= getWidth())) {

                // Multiple cases here
                if ((fixed == true) && (insertMode == true)) {
                    // Buffer is full, do nothing
                } else if ((fixed == true) && (insertMode == false)) {
                    // Overwrite the last character, maybe move position
                    text = text.substring(0, position)
                            + codePointString(keypress.getKey().getChar())
                            + text.substring(position + 1);
                    if (screenPosition < getWidth() - 1) {
                        screenPosition += StringUtils.width(text.codePointAt(position));
                        position += Character.charCount(keypress.getKey().getChar());
                    }
                } else if ((fixed == false) && (insertMode == false)) {
                    // Overwrite the last character, definitely move position
                    text = text.substring(0, position)
                            + codePointString(keypress.getKey().getChar())
                            + text.substring(position + 1);
                    screenPosition += StringUtils.width(text.codePointAt(position));
                    position += Character.charCount(keypress.getKey().getChar());
                } else {
                    if (position == text.length()) {
                        // Append this character
                        appendChar(keypress.getKey().getChar());
                    } else {
                        // Insert this character
                        insertChar(keypress.getKey().getChar());
                    }
                }
            } else {
                assert (!fixed);

                // Append this character
                appendChar(keypress.getKey().getChar());
            }
            dispatch(false);
            return;
        }

        // Pass to parent for the things we don't care about.
        super.onKeypress(keypress);
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
            getClipboard().copyText(text);
            getApplication().getBackend().copyClipboardText(text);
            setText("");
            return;
        }

        if (command.equals(cmCopy)) {
            // Copy text to clipboard.
            getClipboard().copyText(text);
            getApplication().getBackend().copyClipboardText(text);
            return;
        }

        if (command.equals(cmPaste)) {
            // Paste text from clipboard.
            String newText = getClipboard().pasteText();
            if (newText != null) {
                setText(newText);
            }
            return;
        }

        if (command.equals(cmClear)) {
            // Remove text.
            setText("");
            return;
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
        CellAttributes fieldColor;

        if (isAbsoluteActive()) {
            fieldColor = getTheme().getColor(activeColorKey);
        } else {
            fieldColor = getTheme().getColor(inactiveColorKey);
        }
        // Pulse color.
        if (isActive() && getWindow().isActive()
            && getApplication().hasAnimations()
        ) {
            fieldColor.setPulse(true, false, 0);
            fieldColor.setPulseColorRGB(getScreen().getBackend().
                attrToForegroundColor(getTheme().getColor("tfield.pulse")));
        }

        int end = windowStart + getWidth();
        if (end > StringUtils.width(text)) {
            end = StringUtils.width(text);
        }
        hLineXY(0, 0, getWidth(), backgroundChar, fieldColor);
        putStringXY(0, 0, text.substring(screenToTextPosition(windowStart),
                screenToTextPosition(end)), fieldColor);

        // Fix the cursor, it will be rendered by TApplication.drawAll().
        updateCursor();
    }

    // ------------------------------------------------------------------------
    // TField -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Convert a char (codepoint) to a string.
     *
     * @param ch the char
     * @return the string
     */
    private String codePointString(final int ch) {
        StringBuilder sb = new StringBuilder(1);
        sb.append(Character.toChars(ch));
        assert (Character.charCount(ch) == sb.length());
        return sb.toString();
    }

    /**
     * Get field background character.
     *
     * @return background character
     */
    public final int getBackgroundChar() {
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
    public final String getText() {
        return text;
    }

    /**
     * Set field text.
     *
     * @param text the new field text
     */
    public void setText(final String text) {
        assert (text != null);
        this.text = text;
        position = 0;
        screenPosition = 0;
        windowStart = 0;
        if ((fixed == true) && (this.text.length() > getWidth())) {
            this.text = this.text.substring(0, getWidth());
        }
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
        if ((screenPosition > getWidth()) && fixed) {
            setCursorX(getWidth());
        } else if ((screenPosition - windowStart >= getWidth()) && !fixed) {
            setCursorX(getWidth() - 1);
        } else {
            setCursorX(screenPosition - windowStart);
        }
    }

    /**
     * Normalize windowStart such that most of the field data if visible.
     */
    protected void normalizeWindowStart() {
        if (fixed) {
            // windowStart had better be zero, there is nothing to do here.
            assert (windowStart == 0);
            return;
        }
        windowStart = screenPosition - (getWidth() - 1);
        if (windowStart < 0) {
            windowStart = 0;
        }

        updateCursor();
    }

    /**
     * Append char to the end of the field.
     *
     * @param ch char to append
     */
    protected void appendChar(final int ch) {
        // Append the LAST character
        text += codePointString(ch);
        position += Character.charCount(ch);
        screenPosition += StringUtils.width(ch);

        assert (position == text.length());

        if (fixed) {
            if (screenPosition >= getWidth()) {
                position -= Character.charCount(ch);
                screenPosition -= StringUtils.width(ch);
            }
        } else {
            if ((screenPosition - windowStart) >= getWidth()) {
                windowStart++;
            }
        }
    }

    /**
     * Insert char somewhere in the middle of the field.
     *
     * @param ch char to append
     */
    protected void insertChar(final int ch) {
        text = text.substring(0, position) + codePointString(ch)
                + text.substring(position);
        position += Character.charCount(ch);
        screenPosition += StringUtils.width(ch);
        if ((screenPosition - windowStart) == getWidth()) {
            assert (!fixed);
            windowStart++;
        }
    }

    /**
     * Position the cursor at the first column.  The field may adjust the
     * window start to show as much of the field as possible.
     */
    public void home() {
        position = 0;
        screenPosition = 0;
        windowStart = 0;
    }

    /**
     * Set the editing position to the last filled character.  The field may
     * adjust the window start to show as much of the field as possible.
     */
    public void end() {
        position = text.length();
        screenPosition = StringUtils.width(text);
        if (fixed == true) {
            if (screenPosition >= getWidth()) {
                position -= Character.charCount(text.codePointBefore(position));
                screenPosition = StringUtils.width(text) - 1;
             }
        } else {
            windowStart = StringUtils.width(text) - getWidth() + 1;
            if (windowStart < 0) {
                windowStart = 0;
            }
        }
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
        if ((position < 0) || (position >= text.length())) {
            throw new IndexOutOfBoundsException("Max length is " +
                text.length() + ", requested position " + position);
        }
        this.position = position;
        normalizeWindowStart();
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

}
