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
import casciian.bits.ControlPadding;
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

    /**
     * Extra left/right padding applied to the control.  The value is
     * resolved once at construction from the active
     * {@link ControlPadding} style (system property
     * {@code casciian.controls.padding}).  The checkbox content is drawn
     * offset by this amount from the left edge of the widget, and the
     * widget reserves {@code padding} blank cells on both sides.
     */
    private final int padding;

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

        // Resolve padding once: ControlPadding.current() can be toggled
        // at runtime, but the widget size is fixed at construction, so
        // we only read the style a single time here to avoid any
        // width/padding mismatch.
        this(parent, x, y, label, checked, action,
            ControlPadding.current().getCells());
    }

    /**
     * Private delegate that receives the pre-resolved padding value so
     * the super(...) width and the cached {@code padding} field are
     * guaranteed to agree.
     */
    @SuppressWarnings("this-escape")
    private TCheckBox(final TWidget parent, final int x, final int y,
        final String label, final boolean checked, final TAction action,
        final int padding) {

        // Set parent and window
        super(parent, x, y, StringUtils.width(label) + 4 + 2 * padding, 1);

        this.padding = padding;
        mnemonic = new MnemonicString(label);
        this.checked = checked;
        this.action = action;

        setCursorVisible(true);
        setCursorX(padding + 1);
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
            && (mouse.getX() >= padding)
            && (mouse.getX() <= padding + 2)
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
            checkboxColor.setTo(getWidgetColor("tcheckbox.active"));
            mnemonicColor = getWidgetColor("tcheckbox.mnemonic.highlighted");
        } else {
            checkboxColor.setTo(getWidgetColor("tcheckbox.inactive"));
            mnemonicColor = getWidgetColor("tcheckbox.mnemonic");
        }

        // Pulse color.
        if (isActive() && getWindow().isActive()
            && getApplication().hasAnimations()
        ) {
            checkboxColor.setPulse(true, false, 0);
            checkboxColor.setPulseColorRGB(getScreen().getBackend().
                attrToForegroundColor(getWidgetColor("tcheckbox.pulse")));

        }

        if (padding > 0) {
            // Paint the left and right padding cells with the checkbox
            // background color so they blend with the control.
            if (matchWindowBackground) {
                for (int i = 0; i < padding; i++) {
                    putForegroundCharXY(i, 0, ' ', checkboxColor);
                    putForegroundCharXY(getWidth() - 1 - i, 0, ' ',
                        checkboxColor);
                }
            } else {
                for (int i = 0; i < padding; i++) {
                    putCharXY(i, 0, ' ', checkboxColor);
                    putCharXY(getWidth() - 1 - i, 0, ' ', checkboxColor);
                }
            }
        }

        if (matchWindowBackground) {
            putForegroundCharXY(padding, 0, '[', checkboxColor);
        } else {
            putCharXY(padding, 0, '[', checkboxColor);
        }
        if (checked) {
            if (matchWindowBackground) {
                putForegroundCharXY(padding + 1, 0, GraphicsChars.CHECK,
                    checkboxColor);
            } else {
                putCharXY(padding + 1, 0, GraphicsChars.CHECK, checkboxColor);
            }
        } else {
            if (matchWindowBackground) {
                putForegroundCharXY(padding + 1, 0, ' ', checkboxColor);
            } else {
                putCharXY(padding + 1, 0, ' ', checkboxColor);
            }
        }
        if (matchWindowBackground) {
            putForegroundCharXY(padding + 2, 0, ']', checkboxColor);
            putForegroundStringXY(padding + 4, 0, mnemonic.getRawLabel(),
                checkboxColor);
        } else {
            putCharXY(padding + 2, 0, ']', checkboxColor);
            putStringXY(padding + 4, 0, mnemonic.getRawLabel(), checkboxColor);
        }
        if (mnemonic.getScreenShortcutIdx() >= 0) {
            if (matchWindowBackground) {
                putForegroundCharXY(padding + 4
                    + mnemonic.getScreenShortcutIdx(), 0,
                    mnemonic.getShortcut(), mnemonicColor);
            } else {
                putCharXY(padding + 4 + mnemonic.getScreenShortcutIdx(), 0,
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
