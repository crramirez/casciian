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

import casciian.bits.Cell;
import casciian.bits.CellAttributes;
import casciian.bits.Color;
import casciian.bits.GraphicsChars;
import casciian.bits.MnemonicString;
import casciian.bits.StringUtils;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import static casciian.TKeypress.kbEnter;
import static casciian.TKeypress.kbSpace;

/**
 * TButton implements a simple button.  To make the button do something, pass
 * a TAction class to its constructor.
 *
 * @see TAction#DO()
 */
public class TButton extends TWidget {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Available styles of the button.
     */
    public static enum Style {

        /**
         * A fully-text square button.  The default style.
         */
        SQUARE,

    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The shortcut and button text.
     */
    private MnemonicString mnemonic;

    /**
     * Remember mouse state.
     */
    private TMouseEvent mouse;

    /**
     * True when the button is being pressed and held down.
     */
    private boolean inButtonPress = false;

    /**
     * The action to perform when the button is clicked.
     */
    private TAction action;

    /**
     * If true, suppress the button shadow.
     */
    private boolean noButtonShadow = false;

    /**
     * The style of button to draw.
     */
    private Style style = Style.SQUARE;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Private constructor.
     *
     * @param parent parent widget
     * @param text label on the button
     * @param x column relative to parent
     * @param y row relative to parent
     */
    @SuppressWarnings("this-escape")
    private TButton(final TWidget parent, final String text,
        final int x, final int y) {

        // Set parent and window
        super(parent);

        mnemonic = new MnemonicString(text);

        setX(x);
        setY(y);
        super.setHeight(2);
        super.setWidth(StringUtils.width(mnemonic.getRawLabel()) + 3);

        setStyle((String) null);

        // Since we set dimensions after TWidget's constructor, we need to
        // update the layout manager.
        if (getParent().getLayoutManager() != null) {
            getParent().getLayoutManager().remove(this);
            getParent().getLayoutManager().add(this);
        }
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param text label on the button
     * @param x column relative to parent
     * @param y row relative to parent
     * @param action to call when button is pressed
     */
    public TButton(final TWidget parent, final String text,
        final int x, final int y, final TAction action) {

        this(parent, text, x, y);
        this.action = action;
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Returns true if the mouse is currently on the button.
     *
     * @return if true the mouse is currently on the button
     */
    private boolean mouseOnButton() {
        int rightEdge = getWidth() - 1;
        if (inButtonPress) {
            rightEdge++;
        }
        if ((mouse != null)
            && (mouse.getY() == 0)
            && (mouse.getX() >= 0)
            && (mouse.getX() < rightEdge)
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

        if ((mouseOnButton()) && (mouse.isMouse1())) {
            // Begin button press
            inButtonPress = true;
        }
    }

    /**
     * Handle mouse button releases.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        this.mouse = mouse;

        if (inButtonPress && mouse.isMouse1()) {
            // Dispatch the event
            dispatch();
        }

    }

    /**
     * Handle mouse movements.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        this.mouse = mouse;

        if (!mouseOnButton()) {
            inButtonPress = false;
        }
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.equals(kbEnter)
            || keypress.equals(kbSpace)
        ) {
            // Dispatch
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
     * Override TWidget.setActive() so that the button ends are redrawn.
     *
     * @param enabled if true, this widget can be tabbed to or receive events
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
    }

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
     * Draw a button with a shadow.
     */
    @Override
    public void draw() {
        CellAttributes buttonColor;
        CellAttributes mnemonicColor;

        if (!isEnabled()) {
            buttonColor = getTheme().getColor("tbutton.disabled");
            mnemonicColor = getTheme().getColor("tbutton.disabled");
        } else if (isAbsoluteActive()) {
            buttonColor = getTheme().getColor("tbutton.active");
            mnemonicColor = getTheme().getColor("tbutton.mnemonic.highlighted");
        } else {
            buttonColor = getTheme().getColor("tbutton.inactive");
            mnemonicColor = getTheme().getColor("tbutton.mnemonic");
        }

        buttonColor = new CellAttributes(buttonColor);
        buttonColor.setForeColorRGB(getScreen().getBackend().
            attrToForegroundColor(buttonColor));
        mnemonicColor = new CellAttributes(mnemonicColor);
        mnemonicColor.setForeColorRGB(getScreen().getBackend().
            attrToForegroundColor(mnemonicColor));

        // Pulse colors.
        if (isActive() && getWindow().isActive()
            && getApplication().hasAnimations()
        ) {
            buttonColor.setPulse(true, false, 0);
            buttonColor.setPulseColorRGB(getScreen().getBackend().
                attrToForegroundColor(getTheme().getColor("tbutton.pulse")));
            mnemonicColor.setPulse(true, false, 0);
            mnemonicColor.setPulseColorRGB(getScreen().getBackend().
                attrToForegroundColor(getTheme().getColor(
                    "tbutton.mnemonic.pulse")));
        }

        Cell leftEdge = getLeftEdge(buttonColor);
        Cell rightEdge = getRightEdge(buttonColor);

        /*
        leftEdge = new Cell('\\', buttonColor);
        rightEdge = new Cell('/', buttonColor);
         */

        if (inButtonPress) {
            putCharXY(1, 0, leftEdge);
            putStringXY(2, 0, mnemonic.getRawLabel(), buttonColor);
            putCharXY(getWidth() - 1, 0, rightEdge);
        } else {
            putCharXY(0, 0, leftEdge);
            putStringXY(1, 0, mnemonic.getRawLabel(), buttonColor);
            putCharXY(getWidth() - 2, 0, rightEdge);

            if (!noButtonShadow) {
                Cell leftBottomShadow = getLeftBottomShadow();
                Cell rightTopShadow = getRightTopShadow();
                Cell rightBottomShadow = getRightBottomShadow();
                putCharXY(1, 1, leftBottomShadow);
                for (int i = 0; i < getWidth() - 3; i++) {
                    Cell bottomShadow = getBottomShadow(buttonColor, i + 2);
                    putCharXY(i + 2, 1, bottomShadow);
                }
                putCharXY(getWidth() - 1, 0, rightTopShadow);
                putCharXY(getWidth() - 1, 1, rightBottomShadow);
            }
        }
        if (mnemonic.getScreenShortcutIdx() >= 0) {
            if (inButtonPress) {
                putCharXY(2 + mnemonic.getScreenShortcutIdx(), 0,
                    mnemonic.getShortcut(), mnemonicColor);
            } else {
                putCharXY(1 + mnemonic.getScreenShortcutIdx(), 0,
                    mnemonic.getShortcut(), mnemonicColor);
            }
        }
    }

    // ------------------------------------------------------------------------
    // TButton ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the mnemonic string for this button.
     *
     * @return mnemonic string
     */
    public MnemonicString getMnemonic() {
        return mnemonic;
    }

    /**
     * Act as though the button was pressed.  This is useful for other UI
     * elements to get the same action as if the user clicked the button.
     */
    public void dispatch() {
        if (action != null) {
            action.DO(this);
            inButtonPress = false;
        }
    }

    /**
     * Set the button style.
     *
     * @param style SQUARE, ROUND, etc.
     */
    public void setStyle(final Style style) {
        this.style = style;
    }

    /**
     * Get the button end on the left side.
     *
     * @param buttonColor the button foreground color
     * @return the left button end
     */
    private Cell getLeftEdge(final CellAttributes buttonColor) {
        if (style == Style.SQUARE) {
            return new Cell(buttonColor);
        }
        throw new IllegalArgumentException("Unsupported button style");
    }

    /**
     * Get the button end on the right side.
     *
     * @param buttonColor the button foreground color
     * @return the right button end
     */
    private Cell getRightEdge(final CellAttributes buttonColor) {
        if (style == Style.SQUARE) {
            return new Cell(buttonColor);
        }
        throw new IllegalArgumentException("Unsupported button style");
    }

    /**
     * Get the shadowed cell for the button end on the leftt side.
     *
     * @return the right button end
     */
    private Cell getLeftBottomShadow() {
        int screenX = getAbsoluteX() + 1;
        int screenY = getAbsoluteY() + 1;
        CellAttributes shadowChar = getAttrXY(screenX, screenY, true);
        shadowChar.setForeColorRGB(0x00);

        if (style == Style.SQUARE) {
            return new Cell(GraphicsChars.CP437[0xDF], shadowChar);
        }
        throw new IllegalArgumentException("Unsupported button style");
    }

    /**
     * Get the shadowed cell for the button end on the right side.
     *
     * @return the right button end's shadow
     */
    private Cell getRightTopShadow() {
        int screenX = getAbsoluteX() + getWidth() - 1;
        int screenY = getAbsoluteY();
        CellAttributes shadowChar = getAttrXY(screenX, screenY, true);
        shadowChar.setForeColorRGB(0x00);

        if (style == Style.SQUARE) {
            return new Cell(GraphicsChars.CP437[0xDC], shadowChar);
        }
        throw new IllegalArgumentException("Unsupported button style");
    }

    /**
     * Get the shadowed cell below the button end on the right side.
     *
     * @return the right button end's shadow
     */
    private Cell getRightBottomShadow() {
        int screenX = getAbsoluteX() + getWidth() - 1;
        int screenY = getAbsoluteY() + 1;
        CellAttributes shadowChar = getAttrXY(screenX, screenY, true);
        shadowChar.setForeColorRGB(0x00);
        if (style == Style.SQUARE) {
            return new Cell(GraphicsChars.CP437[0xDF], shadowChar);
        }
        throw new IllegalArgumentException("Unsupported button style");
    }

    /**
     * Get the shadowed cell below the button text.
     *
     * @param buttonColor the button foreground color
     * @param x the widget x location for this shadowed cell
     * @return the right button end's shadow
     */
    private Cell getBottomShadow(final CellAttributes buttonColor,
        final int x) {

        int screenX = getAbsoluteX() + x;
        int screenY = getAbsoluteY() + 1;
        CellAttributes shadowChar = getAttrXY(screenX, screenY, true);
        shadowChar.setForeColorRGB(0x00);
        if (style == Style.SQUARE) {
            return new Cell(GraphicsChars.CP437[0xDF], shadowChar);
        }
        throw new IllegalArgumentException("Unsupported button style");
    }

    /**
     * Set the button style.
     *
     * @param buttonStyle the button style string: "square"; or null to use
     * the value from casciian.TButton.style.
     */
    public void setStyle(final String buttonStyle) {
        String styleString = System.getProperty("casciian.TButton.style",
            "square");
        if (buttonStyle != null) {
            styleString = buttonStyle.toLowerCase();
        }
        if (styleString.equals("square")) {
            style = Style.SQUARE;
        } else {
            // No other button styles supported.
            style = Style.SQUARE;
        }
    }

}
