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

import java.util.ArrayList;
import java.util.List;

import casciian.bits.CellAttributes;
import casciian.bits.GraphicsChars;
import casciian.bits.StringUtils;
import casciian.event.TCommandEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;

/**
 * TStatusBar implements a status line with clickable buttons.
 */
public class TStatusBar extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Remember mouse state.
     */
    private TMouseEvent mouse;

    /**
     * The text to display on the right side of the shortcut keys.
     */
    private String text = null;

    /**
     * The shortcut keys.
     */
    private List<TStatusBarKey> keys = new ArrayList<TStatusBarKey>();

    /**
     * A single shortcut key.
     */
    private class TStatusBarKey {

        /**
         * The keypress for this action.
         */
        public TKeypress key;

        /**
         * The command to issue.
         */
        public TCommand cmd;

        /**
         * The label text.
         */
        public String label;

        /**
         * If true, the mouse is on this key.
         */
        public boolean selected;

        /**
         * The left edge coordinate to draw this key with.
         */
        public int x = 0;

        /**
         * The width of this key on the screen.
         *
         * @return the number of columns this takes when drawn
         */
        public int width() {
            return StringUtils.width(this.label) +
                StringUtils.width(this.key.toString()) + 3;
        }

        /**
         * Add a key to this status bar.
         *
         * @param key the key to trigger on
         * @param cmd the command event to issue when key is pressed or this
         * item is clicked
         * @param label the label for this action
         */
        public TStatusBarKey(final TKeypress key, final TCommand cmd,
            final String label) {

            this.key    = key;
            this.cmd    = cmd;
            this.label  = label;
        }

    }

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param window the window associated with this status bar
     * @param text text for the bar on the bottom row
     */
    @SuppressWarnings("this-escape")
    public TStatusBar(final TWindow window, final String text) {

        // TStatusBar is a parentless widget, because TApplication handles
        // its drawing and event routing directly.
        super(null, false, 0, 0, StringUtils.width(text), 1);

        this.text = text;
        setWindow(window);
    }

    /**
     * Public constructor.
     *
     * @param window the window associated with this status bar
     */
    public TStatusBar(final TWindow window) {
        this(window, "");
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle keypresses.
     *
     * @param keypress keystroke event
     * @return true if this keypress was consumed
     */
    public boolean statusBarKeypress(final TKeypressEvent keypress) {
        for (TStatusBarKey key: keys) {
            if (keypress.equals(key.key)) {
                getApplication().postMenuEvent(new TCommandEvent(
                    keypress.getBackend(), key.cmd));
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the mouse is currently on the button.
     *
     * @param statusBarKey the status bar item
     * @return if true the mouse is currently on the button
     */
    private boolean mouseOnShortcut(final TStatusBarKey statusBarKey) {
        if ((mouse != null)
            && (mouse.getAbsoluteY() == getApplication().getDesktopBottom())
            && (mouse.getAbsoluteX() >= statusBarKey.x)
            && (mouse.getAbsoluteX() < statusBarKey.x + statusBarKey.width())
        ) {
            return true;
        }
        return false;
    }

    /**
     * Handle mouse button presses.
     *
     * @param mouse mouse button event
     * @return true if this mouse event was consumed
     */
    public boolean statusBarMouseDown(final TMouseEvent mouse) {
        this.mouse = mouse;

        for (TStatusBarKey key: keys) {
            if ((mouseOnShortcut(key)) && (mouse.isMouse1())) {
                key.selected = true;
                return true;
            }
        }
        return false;
    }

    /**
     * Handle mouse button releases.
     *
     * @param mouse mouse button release event
     * @return true if this mouse event was consumed
     */
    public boolean statusBarMouseUp(final TMouseEvent mouse) {
        this.mouse = mouse;

        for (TStatusBarKey key: keys) {
            if (key.selected && mouse.isMouse1()) {
                key.selected = false;

                // Dispatch the event
                getApplication().postMenuEvent(new TCommandEvent(
                    mouse.getBackend(), key.cmd));
                return true;
            }
        }
        return false;
    }

    /**
     * Handle mouse movements.
     *
     * @param mouse mouse motion event
     */
    public void statusBarMouseMotion(final TMouseEvent mouse) {
        this.mouse = mouse;

        for (TStatusBarKey key: keys) {
            if (!mouseOnShortcut(key)) {
                key.selected = false;
            }
        }
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the bar.
     */
    @Override
    public void draw() {
        CellAttributes barColor = new CellAttributes();
        barColor.setTo(getTheme().getColor("tstatusbar.text"));
        CellAttributes keyColor = new CellAttributes();
        keyColor.setTo(getTheme().getColor("tstatusbar.button"));
        CellAttributes selectedColor = new CellAttributes();
        selectedColor.setTo(getTheme().getColor("tstatusbar.selected"));

        // Status bar is weird.  Its draw() method is called directly by
        // TApplication after everything is drawn, and after
        // Screen.resetClipping().  So at this point we are drawing in
        // absolute coordinates, not relative to our TWindow.
        int row = getScreen().getHeight() - 1;
        int width = getScreen().getWidth();

        hLineXY(0, row, width, ' ', barColor);

        int col = 0;
        for (TStatusBarKey key: keys) {
            String keyStr = key.key.toString();
            if (key.selected) {
                putCharXY(col++, row, ' ', selectedColor);
                putStringXY(col, row, keyStr, selectedColor);
                col += StringUtils.width(keyStr);
                putCharXY(col++, row, ' ', selectedColor);
                putStringXY(col, row, key.label, selectedColor);
                col += StringUtils.width(key.label);
                putCharXY(col++, row, ' ', selectedColor);
            } else {
                putCharXY(col++, row, ' ', barColor);
                putStringXY(col, row, keyStr, keyColor);
                col += StringUtils.width(keyStr) + 1;
                putStringXY(col, row, key.label, barColor);
                col += StringUtils.width(key.label);
                putCharXY(col++, row, ' ', barColor);
            }
        }
        if (text.length() > 0) {
            if (keys.size() > 0) {
                putCharXY(col++, row, GraphicsChars.VERTICAL_BAR, barColor);
            }
            putCharXY(col++, row, ' ', barColor);
            putStringXY(col, row, text, barColor);
        }
    }

    // ------------------------------------------------------------------------
    // TStatusBar -------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Add a key to this status bar.
     *
     * @param key the key to trigger on
     * @param cmd the command event to issue when key is pressed or this item
     * is clicked
     * @param label the label for this action
     */
    public void addShortcutKeypress(final TKeypress key, final TCommand cmd,
        final String label) {

        TStatusBarKey newKey = new TStatusBarKey(key, cmd, label);
        if (keys.size() > 0) {
            TStatusBarKey oldKey = keys.get(keys.size() - 1);
            newKey.x = oldKey.x + oldKey.width();
        }
        keys.add(newKey);
    }

    /**
     * Set the text to display on the right side of the shortcut keys.
     *
     * @param text the new text
     */
    public void setText(final String text) {
        this.text = text;
    }

}
