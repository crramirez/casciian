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

import static casciian.TKeypress.kbEnter;
import static casciian.TKeypress.kbEsc;
import static casciian.TKeypress.kbSpace;
import casciian.bits.CellAttributes;
import casciian.bits.GraphicsChars;
import casciian.bits.MnemonicString;
import casciian.bits.StringUtils;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;

/**
 * TCheckBox implements an on/off checkbox.
 */
public class TCheckBox extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * CheckBox state, true means checked.
     */
    private boolean checked = false;

    /**
     * The shortcut and checkbox label.
     */
    private MnemonicString mnemonic;

    /**
     * The action to perform when the checkbox is toggled.
     */
    private TAction action;

    /**
     * If true, use the window's background color.
     */
    private boolean matchWindowBackground = true;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param label label to display next to (right of) the checkbox
     * @param checked initial check state
     */
    public TCheckBox(final TWidget parent, final int x, final int y,
        final String label, final boolean checked) {

        this(parent, x, y, label, checked, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param label label to display next to (right of) the checkbox
     * @param checked initial check state
     * @param action the action to perform when the checkbox is toggled
     */
    @SuppressWarnings("this-escape")
    public TCheckBox(final TWidget parent, final int x, final int y,
        final String label, final boolean checked, final TAction action) {

        // Set parent and window
        super(parent, x, y, StringUtils.width(label) + 4, 1);

        mnemonic = new MnemonicString(label);
        this.checked = checked;
        this.action = action;

        setCursorVisible(true);
        setCursorX(1);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Returns true if the mouse is currently on the checkbox.
     *
     * @param mouse mouse event
     * @return true if the mouse is currently on the checkbox
     */
    private boolean mouseOnCheckBox(final TMouseEvent mouse) {
        if ((mouse.getY() == 0)
            && (mouse.getX() >= 0)
            && (mouse.getX() <= 2)
        ) {
            return true;
        }
        return false;
    }

    /**
     * Handle mouse checkbox presses.
     *
     * @param mouse mouse button down event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if ((mouseOnCheckBox(mouse)) && (mouse.isMouse1())) {
            // Switch state
            checked = !checked;
            dispatch();
        }
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.equals(kbSpace)
            || keypress.equals(kbEnter)
        ) {
            checked = !checked;
            dispatch();
            return;
        }

        if (keypress.equals(kbEsc)) {
            checked = false;
            dispatch();
            return;
        }

        // Pass to parent for the things we don't care about.
        super.onKeypress(keypress);
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw a checkbox with label.
     */
    @Override
    public void draw() {
        CellAttributes checkboxColor = new CellAttributes();
        CellAttributes mnemonicColor;

        if (isAbsoluteActive()) {
            checkboxColor.setTo(getTheme().getColor("tcheckbox.active"));
            mnemonicColor = getTheme().getColor("tcheckbox.mnemonic.highlighted");
        } else {
            checkboxColor.setTo(getTheme().getColor("tcheckbox.inactive"));
            mnemonicColor = getTheme().getColor("tcheckbox.mnemonic");
        }

        // Pulse color.
        if (isActive() && getWindow().isActive()
            && getApplication().hasAnimations()
        ) {
            checkboxColor.setPulse(true, false, 0);
            checkboxColor.setPulseColorRGB(getScreen().getBackend().
                attrToForegroundColor(getTheme().getColor("tcheckbox.pulse")));

        }

        if (matchWindowBackground) {
            putForegroundCharXY(0, 0, '[', checkboxColor);
        } else {
            putCharXY(0, 0, '[', checkboxColor);
        }
        if (checked) {
            if (matchWindowBackground) {
                putForegroundCharXY(1, 0, GraphicsChars.CHECK, checkboxColor);
            } else {
                putCharXY(1, 0, GraphicsChars.CHECK, checkboxColor);
            }
        } else {
            if (matchWindowBackground) {
                putForegroundCharXY(1, 0, ' ', checkboxColor);
            } else {
                putCharXY(1, 0, ' ', checkboxColor);
            }
        }
        if (matchWindowBackground) {
            putForegroundCharXY(2, 0, ']', checkboxColor);
            putForegroundStringXY(4, 0, mnemonic.getRawLabel(), checkboxColor);
        } else {
            putCharXY(2, 0, ']', checkboxColor);
            putStringXY(4, 0, mnemonic.getRawLabel(), checkboxColor);
        }
        if (mnemonic.getScreenShortcutIdx() >= 0) {
            if (matchWindowBackground) {
                putForegroundCharXY(4 + mnemonic.getScreenShortcutIdx(), 0,
                    mnemonic.getShortcut(), mnemonicColor);
            } else {
                putCharXY(4 + mnemonic.getScreenShortcutIdx(), 0,
                    mnemonic.getShortcut(), mnemonicColor);
            }
        }
    }

    // ------------------------------------------------------------------------
    // TCheckBox --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get checked value.
     *
     * @return if true, this is checked
     */
    public boolean isChecked() {
        return checked;
    }

    /**
     * Set checked value.
     *
     * @param checked new checked value.
     */
    public void setChecked(final boolean checked) {
        this.checked = checked;
    }

    /**
     * Get the mnemonic string for this checkbox.
     *
     * @return mnemonic string
     */
    public MnemonicString getMnemonic() {
        return mnemonic;
    }

    /**
     * Get the window background option.
     *
     * @return true if the window's background color will be used
     */
    public boolean isMatchWindowBackground() {
        return matchWindowBackground;
    }

    /**
     * Set the window background option.
     *
     * @param matchWindowBackground if true, the window's background color
     * will be used
     */
    public void setMatchWindowBackground(final boolean matchWindowBackground) {
        this.matchWindowBackground = matchWindowBackground;
    }

    /**
     * Act as though the checkbox was pressed.  This is useful for other UI
     * elements to get the same action as if the user clicked the checkbox.
     */
    public void dispatch() {
        if (action != null) {
            action.DO(this);
        }
    }

}
