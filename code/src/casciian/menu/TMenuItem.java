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
package casciian.menu;

import casciian.TKeypress;
import casciian.TWidget;
import casciian.backend.Backend;
import casciian.bits.BorderStyle;
import casciian.bits.CellAttributes;
import casciian.bits.ComplexCell;
import casciian.bits.GraphicsChars;
import casciian.bits.MnemonicString;
import casciian.bits.StringUtils;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import casciian.event.TMenuEvent;
import static casciian.TKeypress.*;

/**
 * TMenuItem implements a menu item.
 */
public class TMenuItem extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Label for this menu item.
     */
    private String label;

    /**
     * Menu ID.  IDs less than 1024 are reserved for common system
     * functions.  Existing ones are defined in TMenu, i.e. TMenu.MID_EXIT.
     */
    private int id = TMenu.MID_UNUSED;

    /**
     * When true, this item can be checked or unchecked.
     */
    private boolean checkable = false;

    /**
     * When true, this item is checked.
     */
    private boolean checked = false;

    /**
     * Global shortcut key.
     */
    private TKeypress key;

    /**
     * The title string.  Use '&' to specify a mnemonic, i.e. "&File" will
     * highlight the 'F' and allow 'f' or 'F' to select it.
     */
    private MnemonicString mnemonic;

    /**
     * An optional 2-cell-wide picture/icon for this item.
     */
    private int [] icon = null;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Package private constructor.
     *
     * @param parent parent widget
     * @param id menu id
     * @param x column relative to parent
     * @param y row relative to parent
     * @param label menu item title
     */
    TMenuItem(final TMenu parent, final int id, final int x, final int y,
        final String label) {

        this(parent, id, x, y, label, null);
    }

    /**
     * Package private constructor.
     *
     * @param parent parent widget
     * @param id menu id
     * @param x column relative to parent
     * @param y row relative to parent
     * @param label menu item title
     * @param icon icon picture/emoji
     */
    TMenuItem(final TMenu parent, final int id, final int x, final int y,
        final String label, final int icon) {

        this(parent, id, x, y, label, null);
        if (icon != -1) {
            this.icon = new int[1];
            this.icon[0] = icon;
        }
    }

    /**
     * Package private constructor.
     *
     * @param parent parent widget
     * @param id menu id
     * @param x column relative to parent
     * @param y row relative to parent
     * @param label menu item title
     * @param icon icon picture/emoji
     */
    TMenuItem(final TMenu parent, final int id, final int x, final int y,
        final String label, final int [] icon) {

        // Set parent and window
        super(parent);

        mnemonic = new MnemonicString(label);

        setX(x);
        setY(y);
        setHeight(1);
        this.label = mnemonic.getRawLabel();
        if (parent.useIcons) {
            setWidth(StringUtils.width(label) + 6);
        } else {
            setWidth(StringUtils.width(label) + 4);
        }
        this.id = id;
        this.icon = icon;

        // Default state for some known menu items
        switch (id) {

        case TMenu.MID_CUT:
            setEnabled(false);
            break;
        case TMenu.MID_COPY:
            setEnabled(false);
            break;
        case TMenu.MID_PASTE:
            setEnabled(false);
            break;
        case TMenu.MID_CLEAR:
            setEnabled(false);
            break;

        case TMenu.MID_TILE:
            break;
        case TMenu.MID_CASCADE:
            break;
        case TMenu.MID_CLOSE_ALL:
            break;
        case TMenu.MID_WINDOW_MOVE:
            break;
        case TMenu.MID_WINDOW_ZOOM:
            break;
        case TMenu.MID_WINDOW_NEXT:
            break;
        case TMenu.MID_WINDOW_PREVIOUS:
            break;
        case TMenu.MID_WINDOW_CLOSE:
            break;
        default:
            break;
        }

    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Returns true if the mouse is currently on the menu item.
     *
     * @param mouse mouse event
     * @return if true then the mouse is currently on this item
     */
    private boolean mouseOnMenuItem(final TMouseEvent mouse) {
        if ((mouse.getY() == 0)
            && (mouse.getX() >= 0)
            && (mouse.getX() < getWidth())
        ) {
            return true;
        }
        return false;
    }

    /**
     * Handle mouse button releases.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        if ((mouseOnMenuItem(mouse)) && (mouse.isMouse1())) {
            dispatch(mouse.getBackend());
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
        if (keypress.equals(kbEnter)) {
            dispatch(keypress.getBackend());
            return;
        }

        // Pass to parent for the things we don't care about.
        super.onKeypress(keypress);
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw a menu item with label.
     */
    @Override
    public void draw() {
        CellAttributes background = getTheme().getColor("tmenu");
        CellAttributes menuColor;
        CellAttributes menuMnemonicColor;
        if (isAbsoluteActive()) {
            menuColor = getTheme().getColor("tmenu.highlighted");
            menuMnemonicColor = getTheme().getColor("tmenu.mnemonic.highlighted");
        } else {
            if (isEnabled()) {
                menuColor = getTheme().getColor("tmenu");
                menuMnemonicColor = getTheme().getColor("tmenu.mnemonic");
            } else {
                menuColor = getTheme().getColor("tmenu.disabled");
                menuMnemonicColor = getTheme().getColor("tmenu.disabled");
            }
        }

        boolean useIcons = ((TMenu) getParent()).useIcons;

        BorderStyle borderStyle = ((TMenu) getParent()).getBorderStyle();
        int cVSide = borderStyle.getVertical();
        vLineXY(0, 0, 1, cVSide, background);
        vLineXY(getWidth() - 1, 0, 1, cVSide, background);

        hLineXY(1, 0, getWidth() - 2, ' ', menuColor);
        putStringXY(2 + (useIcons ? 2 : 0), 0, mnemonic.getRawLabel(),
            menuColor);
        if (key != null) {
            String keyLabel = key.toString();
            putStringXY((getWidth() - StringUtils.width(keyLabel) - 2), 0,
                keyLabel, menuColor);
        }
        if (mnemonic.getScreenShortcutIdx() >= 0) {
            putCharXY(2 + (useIcons ? 2 : 0) + mnemonic.getScreenShortcutIdx(),
                0, mnemonic.getShortcut(), menuMnemonicColor);
        }
        if (checked) {
            assert (checkable);
            putCharXY(1, 0, GraphicsChars.CHECK, menuColor);
        }
        if ((useIcons == true) && (icon != null)) {
            putCharXY(2, 0, new ComplexCell(icon, menuColor));
        }
    }

    // ------------------------------------------------------------------------
    // TMenuItem --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the menu item ID.
     *
     * @return the id
     */
    public final int getId() {
        return id;
    }

    /**
     * Set checkable flag.
     *
     * @param checkable if true, this menu item can be checked/unchecked
     */
    public final void setCheckable(final boolean checkable) {
        this.checkable = checkable;
    }

    /**
     * Get checkable flag.
     *
     * @return true if this menu item is both checkable and checked
     */
    public final boolean isChecked() {
        return ((checkable == true) && (checked == true));
    }

    /**
     * Set checked flag.  Note that setting checked on an item checkable will
     * do nothing.
     *
     * @param checked if true, and if this menu item is checkable, then
     * getChecked() will return true
     */
    public final void setChecked(final boolean checked) {
        if (checkable) {
            this.checked = checked;
        } else {
            this.checked = false;
        }
    }

    /**
     * Get the mnemonic string for this menu item.
     *
     * @return mnemonic string
     */
    public final MnemonicString getMnemonic() {
        return mnemonic;
    }

    /**
     * Get a global accelerator key for this menu item.
     *
     * @return global keyboard accelerator, or null if no key is associated
     * with this item
     */
    public final TKeypress getKey() {
        return key;
    }

    /**
     * Set a global accelerator key for this menu item.
     *
     * @param key global keyboard accelerator
     */
    public final void setKey(final TKeypress key) {
        this.key = key;

        if (key != null) {
            int newWidth = (StringUtils.width(label) + 4 +
                StringUtils.width(key.toString()) + 2);
            if (((TMenu) getParent()).useIcons) {
                newWidth += 2;
            }
            if (newWidth > getWidth()) {
                setWidth(newWidth);
            }
        }
    }

    /**
     * Get a picture/emoji icon for this menu item.
     *
     * @return the codepoints, or null if no icon is specified for this menu
     * item
     */
    public final int [] getIcon() {
        return icon;
    }

    /**
     * Set a picture/emoji icon for this menu item.
     *
     * @param icon a codepoint, or -1 to unset the icon
     */
    public final void setIcon(final int icon) {
        if (icon == -1) {
            this.icon = null;
            return;
        }
        this.icon = new int[1];
        this.icon[0] = icon;
    }

    /**
     * Set a picture/emoji icon for this menu item.
     *
     * @param icon a list of codepoints, or null to unset the icon
     */
    public final void setIcon(final int [] icon) {
        this.icon = new int[icon.length];
        System.arraycopy(icon, 0, this.icon, 0, icon.length);
    }

    /**
     * Dispatch event(s) due to selection or click.
     *
     * @param backend the backend that generated the user input
     */
    public void dispatch(final Backend backend) {
        assert (isEnabled());

        getApplication().postMenuEvent(new TMenuEvent(backend, id));
        if (checkable) {
            checked = !checked;
        }
    }

}
