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
import casciian.bits.MnemonicString;
import casciian.bits.StringUtils;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import static casciian.TKeypress.*;

/**
 * TRadioButton implements a selectable radio button.
 *
 * If the user clicks or presses space on this button, it is selected.
 *
 * If the user presses escape on this button, it is unselected.
 */
public class TRadioButton extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * RadioButton state, true means selected.  Note package private access.
     */
    boolean selected = false;

    /**
     * The shortcut and radio button label.
     */
    private MnemonicString mnemonic;

    /**
     * ID for this radio button.  Buttons start counting at 1 in the
     * RadioGroup.  Note package private access.
     */
    int id;

    /**
     * If true, use the window's background color.
     */
    private boolean matchWindowBackground = true;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Package private constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param label label to display next to (right of) the radiobutton
     * @param id ID for this radio button
     */
    TRadioButton(final TRadioGroup parent, final int x, final int y,
        final String label, final int id) {

        // Set parent and window
        super(parent, x, y, StringUtils.width(label) + 4, 1);

        mnemonic = new MnemonicString(label);
        this.id = id;

        setCursorVisible(true);
        setCursorX(1);

        parent.addRadioButton(this);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Returns true if the mouse is currently on the radio button.
     *
     * @param mouse mouse event
     * @return if true the mouse is currently on the radio button
     */
    private boolean mouseOnRadioButton(final TMouseEvent mouse) {
        if ((mouse.getY() == 0)
            && (mouse.getX() >= 0)
            && (mouse.getX() <= 2)
        ) {
            return true;
        }
        return false;
    }

    /**
     * Handle mouse button presses.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if ((mouseOnRadioButton(mouse)) && (mouse.isMouse1())) {
            // Switch state
            ((TRadioGroup) getParent()).setSelected(id);
        }
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {

        if (keypress.equals(kbSpace)) {
            ((TRadioGroup) getParent()).setSelected(id);
            return;
        }

        if (keypress.equals(kbEsc)) {
            TRadioGroup parent = (TRadioGroup) getParent();
            if (parent.requiresSelection == false) {
                selected = false;
                parent.setSelected(0);
            }
            return;
        }

        // Pass to parent for the things we don't care about.
        super.onKeypress(keypress);
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Override TWidget's width: we can only set width at construction time.
     *
     * @param width new widget width (ignored)
     */
    @Override
    public void setWidth(final int width) {
        // Do nothing
    }

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
     * Draw a radio button with label.
     */
    @Override
    public void draw() {
        CellAttributes radioButtonColor = new CellAttributes();
        CellAttributes mnemonicColor;

        if (isAbsoluteActive()) {
            radioButtonColor.setTo(getTheme().getColor("tradiobutton.active"));
            mnemonicColor = getTheme().getColor("tradiobutton.mnemonic.highlighted");
        } else {
            radioButtonColor.setTo(getTheme().getColor("tradiobutton.inactive"));
            mnemonicColor = getTheme().getColor("tradiobutton.mnemonic");
        }

        // Pulse color.
        if (isActive() && getParent().isActive() && getWindow().isActive()
            && getApplication().hasAnimations()
        ) {
            radioButtonColor.setPulse(true, false, 0);
            radioButtonColor.setPulseColorRGB(getScreen().getBackend().
                attrToForegroundColor(getTheme().getColor(
                    "tradiobutton.pulse")));
        }

        if (matchWindowBackground) {
            putForegroundCharXY(0, 0, '(', radioButtonColor);
        } else {
            putCharXY(0, 0, '(', radioButtonColor);
        }
        if (selected) {
            if (matchWindowBackground) {
                putForegroundCharXY(1, 0, GraphicsChars.CP437[0x07],
                    radioButtonColor);
            } else {
                putCharXY(1, 0, GraphicsChars.CP437[0x07], radioButtonColor);
            }
        } else {
            if (matchWindowBackground) {
                putForegroundCharXY(1, 0, ' ', radioButtonColor);
            } else {
                putCharXY(1, 0, ' ', radioButtonColor);
            }
        }
        if (matchWindowBackground) {
            putForegroundCharXY(2, 0, ')', radioButtonColor);
            putForegroundStringXY(4, 0, mnemonic.getRawLabel(),
                radioButtonColor);
        } else {
            putCharXY(2, 0, ')', radioButtonColor);
            putStringXY(4, 0, mnemonic.getRawLabel(), radioButtonColor);
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
    // TRadioButton -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get RadioButton state, true means selected.
     *
     * @return if true then this is the one button in the group that is
     * selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Set RadioButton state, true means selected.
     *
     * @param selected if true then this is the one button in the group that
     * is selected
     */
    public void setSelected(final boolean selected) {
        if (selected == true) {
            ((TRadioGroup) getParent()).setSelected(id);
        } else {
            ((TRadioGroup) getParent()).setSelected(0);
        }
    }

    /**
     * Get ID for this radio button.  Buttons start counting at 1 in the
     * RadioGroup.
     *
     * @return the ID
     */
    public int getId() {
        return id;
    }

    /**
     * Get the mnemonic string for this button.
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

}
