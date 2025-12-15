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

import java.util.ResourceBundle;

import casciian.TApplication;
import casciian.TKeypress;
import casciian.TWidget;
import casciian.TWindow;
import casciian.bits.BorderStyle;
import casciian.bits.CellAttributes;
import casciian.bits.GraphicsChars;
import casciian.bits.MnemonicString;
import casciian.bits.StringUtils;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import static casciian.TKeypress.*;

/**
 * TMenu is a top-level collection of TMenuItems.
 */
public class TMenu extends TWindow {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // Reserved menu item IDs -------------------------------------------------

    /**
     * A menu item that will not generate a TMenuEvent.
     */
    public static final int MID_UNUSED          = -1;

    // Tools menu -------------------------------------------------------------

    /**
     * Repaint desktop.
     */
    public static final int MID_REPAINT         = 1;

    /**
     * View ASCII/ANSI file.
     */
    public static final int MID_VIEW_ANSI       = 3;

    /**
     * Trigger text cursor glint effect.
     */
    public static final int MID_TEXT_CURSOR_GLINT       = 5;

    // File menu --------------------------------------------------------------

    /**
     * New file/document
     */
    public static final int MID_NEW             = 10;

    /**
     * Exit application.
     */
    public static final int MID_EXIT            = 11;

    /**
     * Exit application.
     */
    public static final int MID_QUIT            = MID_EXIT;

    /**
     * File open dialog.
     */
    public static final int MID_OPEN_FILE       = 12;

    /**
     * File save dialog.
     */
    public static final int MID_SAVE_FILE       = 13;

    /**
     * File save as dialog.
     */
    public static final int MID_SAVE_AS_FILE    = 14;

    /**
     * OS shell.
     */
    public static final int MID_SHELL           = 15;

    // Edit menu --------------------------------------------------------------

    /**
     * Undo last edit.
     */
    public static final int MID_UNDO            = 20;

    /**
     * Redo last undone edit.
     */
    public static final int MID_REDO            = 21;

    /**
     * Cut selected text and copy to the clipboard.
     */
    public static final int MID_CUT             = 22;

    /**
     * Copy selected text to clipboard.
     */
    public static final int MID_COPY            = 23;

    /**
     * Paste from clipboard.
     */
    public static final int MID_PASTE           = 24;

    /**
     * Clear selected text without copying it to the clipboard.
     */
    public static final int MID_CLEAR           = 25;

    // Search menu ------------------------------------------------------------

    /**
     * Find.
     */
    public static final int MID_FIND            = 30;

    /**
     * Replace.
     */
    public static final int MID_REPLACE         = 31;

    /**
     * Search again.
     */
    public static final int MID_SEARCH_AGAIN    = 32;

    /**
     * Go to line number.
     */
    public static final int MID_GOTO_LINE       = 33;

    // Window menu ------------------------------------------------------------

    /**
     * Tile windows.
     */
    public static final int MID_TILE            = 40;

    /**
     * Cascade windows.
     */
    public static final int MID_CASCADE         = 41;

    /**
     * Close all windows.
     */
    public static final int MID_CLOSE_ALL       = 42;

    /**
     * Size/move window.
     */
    public static final int MID_WINDOW_MOVE     = 43;

    /**
     * Move (move/resize) window.
     */
    public static final int MID_WINDOW_ZOOM     = 44;

    /**
     * Next window (like Alt-TAB).
     */
    public static final int MID_WINDOW_NEXT     = 45;

    /**
     * Previous window (like Shift-Alt-TAB).
     */
    public static final int MID_WINDOW_PREVIOUS = 46;

    /**
     * Close window.
     */
    public static final int MID_WINDOW_CLOSE    = 47;

    // Help menu --------------------------------------------------------------

    /**
     * Help system - Table of Contents.
     */
    public static final int MID_HELP_CONTENTS           = 50;

    /**
     * Help system - Index.
     */
    public static final int MID_HELP_INDEX              = 51;

    /**
     * Help system - Topic search.
     */
    public static final int MID_HELP_SEARCH             = 52;

    /**
     * Help system - Previous topic.
     */
    public static final int MID_HELP_PREVIOUS           = 53;

    /**
     * Help system - Help on help.
     */
    public static final int MID_HELP_HELP               = 54;

    /**
     * Help system - Change active help file.
     */
    public static final int MID_HELP_ACTIVE_FILE        = 55;

    /**
     * Show "Terminal Information" dialog.
     */
    public static final int MID_TERMINAL_INFORMATION    = 56;

    /**
     * Show "About Application" dialog.
     */
    public static final int MID_ABOUT                   = 57;

    // Table menu -------------------------------------------------------------

    /**
     * Rename row.
     */
    public static final int MID_TABLE_RENAME_ROW                = 60;

    /**
     * Rename column.
     */
    public static final int MID_TABLE_RENAME_COLUMN             = 61;

    /**
     * Show/hide row labels.
     */
    public static final int MID_TABLE_VIEW_ROW_LABELS           = 70;

    /**
     * Show/hide column labels.
     */
    public static final int MID_TABLE_VIEW_COLUMN_LABELS        = 71;

    /**
     * Enable/disable highlight selected row.
     */
    public static final int MID_TABLE_VIEW_HIGHLIGHT_ROW        = 72;

    /**
     * Enable/disable highlight selected column.
     */
    public static final int MID_TABLE_VIEW_HIGHLIGHT_COLUMN     = 73;

    /**
     * Remove all borders for the entire table.
     */
    public static final int MID_TABLE_BORDER_NONE               = 80;

    /**
     * Set all borders for the entire table.
     */
    public static final int MID_TABLE_BORDER_ALL                = 81;

    /**
     * Remove all borders for selected cell.
     */
    public static final int MID_TABLE_BORDER_CELL_NONE          = 82;

    /**
     * Set all borders for selected cell.
     */
    public static final int MID_TABLE_BORDER_CELL_ALL           = 83;

    /**
     * Set right border for selected cell.
     */
    public static final int MID_TABLE_BORDER_RIGHT              = 84;

    /**
     * Set left border for selected cell.
     */
    public static final int MID_TABLE_BORDER_LEFT               = 85;

    /**
     * Set top border for selected cell.
     */
    public static final int MID_TABLE_BORDER_TOP                = 86;

    /**
     * Set bottom border for selected cell.
     */
    public static final int MID_TABLE_BORDER_BOTTOM             = 87;

    /**
     * Set double-line bottom border for selected cell.
     */
    public static final int MID_TABLE_BORDER_DOUBLE_BOTTOM      = 88;

    /**
     * Set thick bottom border for selected cell.
     */
    public static final int MID_TABLE_BORDER_THICK_BOTTOM       = 89;

    /**
     * Delete selected cell, shifting cells left.
     */
    public static final int MID_TABLE_DELETE_LEFT               = 100;

    /**
     * Delete selected cell, shifting cells up.
     */
    public static final int MID_TABLE_DELETE_UP                 = 101;

    /**
     * Delete selected row.
     */
    public static final int MID_TABLE_DELETE_ROW                = 102;

    /**
     * Delete selected column.
     */
    public static final int MID_TABLE_DELETE_COLUMN             = 103;

    /**
     * Insert column to the left of selected cell.
     */
    public static final int MID_TABLE_INSERT_LEFT               = 104;

    /**
     * Insert column to the right of selected cell.
     */
    public static final int MID_TABLE_INSERT_RIGHT              = 105;

    /**
     * Insert row above selected cell.
     */
    public static final int MID_TABLE_INSERT_ABOVE              = 106;

    /**
     * Insert row below selected cell.
     */
    public static final int MID_TABLE_INSERT_BELOW              = 107;

    /**
     * Narrow column width of selected cell.
     */
    public static final int MID_TABLE_COLUMN_NARROW             = 110;

    /**
     * Expand column width of selected cell.
     */
    public static final int MID_TABLE_COLUMN_WIDEN              = 111;

    /**
     * Open comma-separated-values (CSV) file as new table window.
     */
    public static final int MID_TABLE_FILE_OPEN_CSV             = 115;

    /**
     * Save entire table to comma-separated-values (CSV) file.
     */
    public static final int MID_TABLE_FILE_SAVE_CSV             = 116;

    /**
     * Save entire table to text file.
     */
    public static final int MID_TABLE_FILE_SAVE_TEXT            = 117;

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * If true, this is a sub-menu.  Note package private access.
     */
    boolean isSubMenu = false;

    /**
     * The X position of the menu's title.
     */
    private int titleX;

    /**
     * The shortcut and title.
     */
    private MnemonicString mnemonic;

    /**
     * If true, draw icons with menu items.  Note package private access.
     */
    boolean useIcons = false;

    /**
     * If true, this is a context menu.
     */
    private boolean context = false;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent application
     * @param x column relative to parent
     * @param y row relative to parent
     * @param label mnemonic menu title.  Label must contain a keyboard
     * shortcut (mnemonic), denoted by prefixing a letter with "&amp;",
     * e.g. "&amp;File"
     */
    @SuppressWarnings("this-escape")
    public TMenu(final TApplication parent, final int x, final int y,
        final String label) {

        super(parent, label, x, y, parent.getScreen().getWidth(),
            parent.getScreen().getHeight());

        i18n = ResourceBundle.getBundle(TMenu.class.getName(), getLocale());

        // Setup the menu shortcut
        mnemonic = new MnemonicString(label);
        setTitle(mnemonic.getRawLabel());
        assert (mnemonic.getShortcutIdx() >= 0);

        // Recompute width and height to reflect an empty menu
        setWidth(StringUtils.width(getTitle()) + 4);
        setHeight(2);

        setActive(false);

        if (System.getProperty("casciian.menuIcons", "true").equals("false")) {
            useIcons = false;
        }

        // Set the border style from the system properties
        setBorderStyleForeground(null);
        setBorderStyleInactive(null);
        setBorderStyleModal(null);
        setBorderStyleMoving(null);

        int opacity = 95;
        try {
            opacity = Integer.parseInt(System.getProperty(
                "casciian.TMenu.opacity", "95"));
            opacity = Math.max(opacity, 10);
            opacity = Math.min(opacity, 100);
        } catch (NumberFormatException e) {
            // SQUASH
        }
        setAlpha(opacity * 255 / 100);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle mouse button presses.
     *
     * @param mouse mouse button event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        this.mouse = mouse;
        super.onMouseDown(mouse);

        // Pass to children
        for (TWidget widget: getChildren()) {
            if (widget.mouseWouldHit(mouse)) {
                // Dispatch to this child, also activate it
                activate(widget);

                // Set x and y relative to the child's coordinates
                mouse.setX(mouse.getAbsoluteX() - widget.getAbsoluteX());
                mouse.setY(mouse.getAbsoluteY() - widget.getAbsoluteY());
                widget.handleEvent(mouse);
                return;
            }
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

        // Pass to children
        for (TWidget widget: getChildren()) {
            if (widget.mouseWouldHit(mouse)) {
                // Dispatch to this child, also activate it
                activate(widget);

                // Set x and y relative to the child's coordinates
                mouse.setX(mouse.getAbsoluteX() - widget.getAbsoluteX());
                mouse.setY(mouse.getAbsoluteY() - widget.getAbsoluteY());
                widget.handleEvent(mouse);
                return;
            }
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

        // See if we should activate a different menu item
        for (TWidget widget: getChildren()) {
            if ((mouse.isMouse1())
                && (widget.mouseWouldHit(mouse))
            ) {
                // Activate this menu item
                activate(widget);
                if (widget instanceof TSubMenu) {
                    ((TSubMenu) widget).dispatch(mouse.getBackend());
                }
                return;
            }
        }
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {

        /*
        System.err.printf("keypress: %s active child: %s\n", keypress,
            getActiveChild());
        */

        if (getActiveChild() != this) {
            if (getActiveChild() instanceof TMenu) {
                getActiveChild().onKeypress(keypress);
                return;
            }

            if (getActiveChild() instanceof TSubMenu) {
                TSubMenu subMenu = (TSubMenu) getActiveChild();
                if (subMenu.menu.isActive()) {
                    subMenu.onKeypress(keypress);
                    return;
                }
            }
        }

        if (keypress.equals(kbEsc)) {
            getApplication().closeMenu();
            return;
        }
        if (keypress.equals(kbDown)) {
            switchWidget(true);
            return;
        }
        if (keypress.equals(kbUp)) {
            switchWidget(false);
            return;
        }
        if (keypress.equals(kbRight)) {
            if (!context) {
                getApplication().switchMenu(true);
            }
            return;
        }
        if (keypress.equals(kbLeft)) {
            if (isSubMenu) {
                getApplication().closeSubMenu();
            } else {
                if (!context) {
                    getApplication().switchMenu(false);
                }
            }
            return;
        }

        // Switch to a menuItem if it has an mnemonic
        if (!keypress.getKey().isFnKey()
            && !keypress.getKey().isAlt()
            && !keypress.getKey().isCtrl()) {

            // System.err.println("Checking children for mnemonic...");

            for (TWidget widget: getChildren()) {
                TMenuItem item = (TMenuItem) widget;
                if ((item.isEnabled() == true)
                    && (item.getMnemonic() != null)
                    && (Character.toLowerCase(item.getMnemonic().getShortcut())
                        == Character.toLowerCase(keypress.getKey().getChar()))
                ) {
                    // System.err.println("activate: " + item);

                    // Send an enter keystroke to it
                    activate(item);
                    item.handleEvent(new TKeypressEvent(keypress.getBackend(),
                            kbEnter));
                    return;
                }
            }
        }

        // Dispatch the keypress to an active widget
        for (TWidget widget: getChildren()) {
            if (widget.isActive()) {
                widget.handleEvent(keypress);
                return;
            }
        }
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw a top-level menu with title and menu items.
     */
    @Override
    public void draw() {
        CellAttributes background = getTheme().getColor("tmenu");

        assert (isAbsoluteActive());

        // Fill in the interior background
        for (int i = 0; i < getHeight(); i++) {
            hLineXY(0, i, getWidth(), ' ', background);
        }

        // Draw the box
        BorderStyle borderStyle = getBorderStyle();
        int cTopLeft     = borderStyle.getTopLeft();
        int cTopRight    = borderStyle.getTopRight();
        int cBottomLeft  = borderStyle.getBottomLeft();
        int cBottomRight = borderStyle.getBottomRight();
        int cHSide       = borderStyle.getHorizontal();
        int cVSide       = borderStyle.getVertical();

        // Place the corner characters
        putCharXY(1, 0, cTopLeft, background);
        putCharXY(getWidth() - 2, 0, cTopRight, background);
        putCharXY(1, getHeight() - 1, cBottomLeft, background);
        putCharXY(getWidth() - 2, getHeight() - 1, cBottomRight, background);

        // Draw the box lines
        hLineXY(1 + 1, 0, getWidth() - 4, cHSide, background);
        hLineXY(1 + 1, getHeight() - 1, getWidth() - 4, cHSide, background);

        // Draw a shadow
        if (!getApplication().hasTranslucence()) {
            drawBoxShadow(0, 0, getWidth(), getHeight());
        }
    }

    // ------------------------------------------------------------------------
    // TMenu ------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set the menu title X position.
     *
     * @param titleX the position
     */
    public void setTitleX(final int titleX) {
        this.titleX = titleX;
    }

    /**
     * Get the menu title X position.
     *
     * @return the position
     */
    public int getTitleX() {
        return titleX;
    }

    /**
     * Get the mnemonic string.
     *
     * @return the full mnemonic string
     */
    public MnemonicString getMnemonic() {
        return mnemonic;
    }

    /**
     * Get the context flag.
     *
     * @return true if this menu is a right-click context menu
     */
    public boolean isContext() {
        return context;
    }

    /**
     * Set the context flag, used to open a context menu at a specific screen
     * position.
     *
     * @param context if true, this is a context menu
     * @param x the screen X position
     * @param y the screen Y position
     */
    public void setContext(final boolean context, final int x, final int y) {
        this.context = context;
        setX(x);
        setY(y);

        while (getX() + getWidth() > getScreen().getWidth()) {
            setX(getX() - 1);
        }
        while (getY() + getHeight() > getApplication().getDesktopBottom()) {
            setY(getY() - 1);
        }
    }

    /**
     * Set the context flag.
     *
     * @param context if true, this is a context menu
     */
    public void setContext(final boolean context) {
        if (context == false) {
            setX(0);
            setY(1);
            getApplication().recomputeMenuX();
        }
        this.context = context;
    }

    /**
     * Convenience function to add a menu item.
     *
     * @param id menu item ID.  Must be greater than 1024.
     * @param label menu item label
     * @return the new menu item
     */
    public TMenuItem addItem(final int id, final String label) {
        return addItemInternal(id, label, null);
    }

    /**
     * Convenience function to add a menu item.
     *
     * @param id menu item ID.  Must be greater than 1024.
     * @param label menu item label
     * @param enabled default state for enabled
     * @return the new menu item
     */
    public TMenuItem addItem(final int id, final String label,
        final boolean enabled) {

        return addItemInternal(id, label, null, enabled, -1);
    }

    /**
     * Convenience function to add a custom menu item.
     *
     * @param id menu item ID.  Must be greater than 1024.
     * @param label menu item label
     * @param key global keyboard accelerator
     * @return the new menu item
     */
    public TMenuItem addItem(final int id, final String label,
        final TKeypress key) {

        return addItemInternal(id, label, key);
    }

    /**
     * Convenience function to add a custom menu item.
     *
     * @param id menu item ID.  Must be greater than 1024.
     * @param label menu item label
     * @param key global keyboard accelerator
     * @param enabled default state for enabled
     * @return the new menu item
     */
    public TMenuItem addItem(final int id, final String label,
        final TKeypress key, final boolean enabled) {

        TMenuItem item = addItem(id, label, key);
        item.setEnabled(enabled);
        return item;
    }

    /**
     * Convenience function to add a custom menu item.
     *
     * @param id menu item ID.  Must be greater than 1024.
     * @param label menu item label
     * @param key global keyboard accelerator
     * @return the new menu item
     */
    private TMenuItem addItemInternal(final int id, final String label,
        final TKeypress key) {

        return addItemInternal(id, label, key, true, -1);
    }

    /**
     * Convenience function to add a custom menu item.
     *
     * @param id menu item ID.  Must be greater than 1024.
     * @param label menu item label
     * @param key global keyboard accelerator
     * @param enabled default state for enabled
     * @param icon icon picture/emoji
     * @return the new menu item
     */
    private TMenuItem addItemInternal(final int id, final String label,
        final TKeypress key, final boolean enabled, final int icon) {

        int newY = getChildren().size() + 1;
        assert (newY < getHeight());

        TMenuItem menuItem = new TMenuItem(this, id, 1, newY, label, icon);
        menuItem.setKey(key);
        menuItem.setEnabled(enabled);
        setHeight(getHeight() + 1);
        if (menuItem.getWidth() + 2 > getWidth()) {
            setWidth(menuItem.getWidth() + 2);
        }
        for (TWidget widget: getChildren()) {
            widget.setWidth(getWidth() - 2);
        }
        getApplication().addMenuItem(menuItem);
        getApplication().recomputeMenuX();
        activate(0);
        return menuItem;
    }

    /**
     * Convenience function to add one of the default menu items.
     *
     * @param id menu item ID.  Must be between 0 (inclusive) and 1023
     * (inclusive).
     * @return the new menu item
     */
    public TMenuItem addDefaultItem(final int id) {
        return addDefaultItem(id, true);
    }

    /**
     * Convenience function to add one of the default menu items.
     *
     * @param id menu item ID.  Must be between 0 (inclusive) and 1023
     * (inclusive).
     * @param enabled default state for enabled
     * @return the new menu item
     */
    public TMenuItem addDefaultItem(final int id, final boolean enabled) {
        assert (id >= 0);
        assert (id < 1024);

        String label;
        TKeypress key = null;
        int icon = -1;
        boolean checkable = false;
        boolean checked = false;

        switch (id) {

        case MID_REPAINT:
            label = i18n.getString("menuRepaintDesktop");
            icon = 0x1F3A8;
            break;

        case MID_VIEW_ANSI:
            label = i18n.getString("menuViewAnsiArt");
            icon = 0x1F4F0;
            break;

        case MID_TEXT_CURSOR_GLINT:
            label = i18n.getString("menuTextCursorGlint");
            key = kbAltF12;
            icon = 0x2728;
            break;

        case MID_NEW:
            label = i18n.getString("menuNew");
            icon = 0x1F5CE;
            break;

        case MID_EXIT:
            label = i18n.getString("menuExit");
            key = kbAltX;
            icon = 0x1F5D9;
            break;

        case MID_SHELL:
            label = i18n.getString("menuShell");
            icon = 0x1F5AE;
            break;

        case MID_OPEN_FILE:
            label = i18n.getString("menuOpen");
            key = kbF3;
            icon = 0x1F5C1;
            break;

        case MID_SAVE_FILE:
            label = i18n.getString("menuSave");
            icon = 0x1F4BE;
            break;

        case MID_SAVE_AS_FILE:
            label = i18n.getString("menuSaveAs");
            break;

        case MID_UNDO:
            label = i18n.getString("menuUndo");
            key = kbCtrlZ;
            icon = 0x2B6F;
            break;
        case MID_REDO:
            label = i18n.getString("menuRedo");
            key = kbCtrlY;
            icon = 0x2B6E;
            break;
        case MID_CUT:
            label = i18n.getString("menuCut");
            key = kbCtrlX;
            icon = 0x1F5F6;
            break;
        case MID_COPY:
            label = i18n.getString("menuCopy");
            key = kbCtrlC;
            icon = 0x1F5D0;
            break;
        case MID_PASTE:
            label = i18n.getString("menuPaste");
            key = kbCtrlV;
            icon = 0x1F4CB;
            break;
        case MID_CLEAR:
            label = i18n.getString("menuClear");
            break;

        case MID_FIND:
            label = i18n.getString("menuFind");
            icon = 0x1F50D;
            break;
        case MID_REPLACE:
            label = i18n.getString("menuReplace");
            break;
        case MID_SEARCH_AGAIN:
            label = i18n.getString("menuSearchAgain");
            key = kbCtrlL;
            break;
        case MID_GOTO_LINE:
            label = i18n.getString("menuGotoLine");
            break;

        case MID_TILE:
            label = i18n.getString("menuWindowTile");
            icon = 0x229E;
            break;
        case MID_CASCADE:
            label = i18n.getString("menuWindowCascade");
            icon = 0x1F5D7;
            break;
        case MID_CLOSE_ALL:
            label = i18n.getString("menuWindowCloseAll");
            break;
        case MID_WINDOW_MOVE:
            label = i18n.getString("menuWindowMove");
            key = kbCtrlF5;
            icon = 0x261D;
            break;
        case MID_WINDOW_ZOOM:
            label = i18n.getString("menuWindowZoom");
            key = kbF5;
            icon = 0x2195;
            break;
        case MID_WINDOW_NEXT:
            label = i18n.getString("menuWindowNext");
            key = kbF6;
            icon = 0x2192;
            break;
        case MID_WINDOW_PREVIOUS:
            label = i18n.getString("menuWindowPrevious");
            key = kbShiftF6;
            icon = 0x2190;
            break;
        case MID_WINDOW_CLOSE:
            label = i18n.getString("menuWindowClose");
            key = kbCtrlW;
            icon = 0x263C;
            break;

        case MID_HELP_CONTENTS:
            label = i18n.getString("menuHelpContents");
            break;
        case MID_HELP_INDEX:
            label = i18n.getString("menuHelpIndex");
            key = kbShiftF1;
            break;
        case MID_HELP_SEARCH:
            label = i18n.getString("menuHelpSearch");
            key = kbCtrlF1;
            break;
        case MID_HELP_PREVIOUS:
            label = i18n.getString("menuHelpPrevious");
            key = kbAltF1;
            break;
        case MID_HELP_HELP:
            label = i18n.getString("menuHelpHelp");
            break;
        case MID_HELP_ACTIVE_FILE:
            label = i18n.getString("menuHelpActive");
            break;
        case MID_TERMINAL_INFORMATION:
            label = i18n.getString("menuHelpTerminalInformation");
            break;
        case MID_ABOUT:
            label = i18n.getString("menuHelpAbout");
            break;

        case MID_TABLE_RENAME_COLUMN:
            label = i18n.getString("menuTableRenameColumn");
            break;
        case MID_TABLE_RENAME_ROW:
            label = i18n.getString("menuTableRenameRow");
            break;
        case MID_TABLE_VIEW_ROW_LABELS:
            label = i18n.getString("menuTableViewRowLabels");
            checkable = true;
            checked = true;
            break;
        case MID_TABLE_VIEW_COLUMN_LABELS:
            label = i18n.getString("menuTableViewColumnLabels");
            checkable = true;
            checked = true;
            break;
        case MID_TABLE_VIEW_HIGHLIGHT_ROW:
            label = i18n.getString("menuTableViewHighlightRow");
            checkable = true;
            checked = true;
            break;
        case MID_TABLE_VIEW_HIGHLIGHT_COLUMN:
            label = i18n.getString("menuTableViewHighlightColumn");
            checkable = true;
            checked = true;
            break;

        case MID_TABLE_BORDER_NONE:
            label = i18n.getString("menuTableBorderNone");
            break;
        case MID_TABLE_BORDER_ALL:
            label = i18n.getString("menuTableBorderAll");
            break;
        case MID_TABLE_BORDER_CELL_NONE:
            label = i18n.getString("menuTableBorderCellNone");
            break;
        case MID_TABLE_BORDER_CELL_ALL:
            label = i18n.getString("menuTableBorderCellAll");
            break;
        case MID_TABLE_BORDER_RIGHT:
            label = i18n.getString("menuTableBorderRight");
            break;
        case MID_TABLE_BORDER_LEFT:
            label = i18n.getString("menuTableBorderLeft");
            break;
        case MID_TABLE_BORDER_TOP:
            label = i18n.getString("menuTableBorderTop");
            break;
        case MID_TABLE_BORDER_BOTTOM:
            label = i18n.getString("menuTableBorderBottom");
            break;
        case MID_TABLE_BORDER_DOUBLE_BOTTOM:
            label = i18n.getString("menuTableBorderDoubleBottom");
            break;
        case MID_TABLE_BORDER_THICK_BOTTOM:
            label = i18n.getString("menuTableBorderThickBottom");
            break;
        case MID_TABLE_DELETE_LEFT:
            label = i18n.getString("menuTableDeleteLeft");
            break;
        case MID_TABLE_DELETE_UP:
            label = i18n.getString("menuTableDeleteUp");
            break;
        case MID_TABLE_DELETE_ROW:
            label = i18n.getString("menuTableDeleteRow");
            break;
        case MID_TABLE_DELETE_COLUMN:
            label = i18n.getString("menuTableDeleteColumn");
            break;
        case MID_TABLE_INSERT_LEFT:
            label = i18n.getString("menuTableInsertLeft");
            break;
        case MID_TABLE_INSERT_RIGHT:
            label = i18n.getString("menuTableInsertRight");
            break;
        case MID_TABLE_INSERT_ABOVE:
            label = i18n.getString("menuTableInsertAbove");
            break;
        case MID_TABLE_INSERT_BELOW:
            label = i18n.getString("menuTableInsertBelow");
            break;
        case MID_TABLE_COLUMN_NARROW:
            label = i18n.getString("menuTableColumnNarrow");
            key = kbShiftLeft;
            break;
        case MID_TABLE_COLUMN_WIDEN:
            label = i18n.getString("menuTableColumnWiden");
            key = kbShiftRight;
            break;
        case MID_TABLE_FILE_OPEN_CSV:
            label = i18n.getString("menuTableFileOpenCsv");
            break;
        case MID_TABLE_FILE_SAVE_CSV:
            label = i18n.getString("menuTableFileSaveCsv");
            break;
        case MID_TABLE_FILE_SAVE_TEXT:
            label = i18n.getString("menuTableFileSaveText");
            break;

        default:
            throw new IllegalArgumentException("Invalid menu ID: " + id);
        }

        TMenuItem item = addItemInternal(id, label, key, enabled, icon);
        item.setCheckable(checkable);
        return item;
    }

    /**
     * Convenience function to add a menu separator.
     */
    public void addSeparator() {
        int newY = getChildren().size() + 1;
        assert (newY < getHeight());

        // We just have to construct it, don't need to hang onto what it
        // makes.
        new TMenuSeparator(this, 1, newY);
        setHeight(getHeight() + 1);
    }

    /**
     * Convenience function to add a sub-menu.
     *
     * @param title menu title.  Title must contain a keyboard shortcut,
     * denoted by prefixing a letter with "&amp;", e.g. "&amp;File"
     * @return the new sub-menu
     */
    public TSubMenu addSubMenu(final String title) {
        int newY = getChildren().size() + 1;
        assert (newY < getHeight());

        TSubMenu subMenu = new TSubMenu(this, title, 1, newY);
        setHeight(getHeight() + 1);
        if (subMenu.getWidth() + 2 > getWidth()) {
            setWidth(subMenu.getWidth() + 2);
        }
        for (TWidget widget: getChildren()) {
            widget.setWidth(getWidth() - 2);
        }
        getApplication().recomputeMenuX();
        activate(0);
        subMenu.menu.setX(getX() + getWidth() - 2);

        return subMenu;
    }

    /**
     * Reset the tab order of children to match their position in the list.
     * Available so that subclasses can re-order their widgets if needed.
     */
    protected void resetTabOrder() {
        super.resetTabOrder();
    }

    /**
     * Set the border style for the window when it is the foreground window.
     *
     * @param borderStyle the border style string, one of: "default", "none",
     * "single", "double", "singleVdoubleH", "singleHdoubleV", or "round"; or
     * null to use the value from casciian.TMenu.borderStyle.
     */
    @Override
    public void setBorderStyleForeground(final String borderStyle) {
        if (borderStyle == null) {
            String style = System.getProperty("casciian.TMenu.borderStyle",
                "single");
            super.setBorderStyleForeground(style);
        } else {
            super.setBorderStyleForeground(borderStyle);
        }
    }

    /**
     * Set the border style for the window when it is the modal window.
     *
     * @param borderStyle the border style string, one of: "default", "none",
     * "single", "double", "singleVdoubleH", "singleHdoubleV", or "round"; or
     * null to use the value from casciian.TMenu.borderStyle.
     */
    @Override
    public void setBorderStyleModal(final String borderStyle) {
        if (borderStyle == null) {
            String style = System.getProperty("casciian.TMenu.borderStyle",
                "single");
            super.setBorderStyleModal(style);
        } else {
            super.setBorderStyleModal(borderStyle);
        }
    }

    /**
     * Set the border style for the window when it is an inactive/background
     * window.
     *
     * @param borderStyle the border style string, one of: "default", "none",
     * "single", "double", "singleVdoubleH", "singleHdoubleV", or "round"; or
     * null to use the value from casciian.TMenu.borderStyle.
     */
    @Override
    public void setBorderStyleInactive(final String borderStyle) {
        if (borderStyle == null) {
            String style = System.getProperty("casciian.TMenu.borderStyle",
                "single");
            super.setBorderStyleInactive(style);
        } else {
            super.setBorderStyleInactive(borderStyle);
        }
    }

    /**
     * Set the border style for the window when it is being dragged/resize.
     *
     * @param borderStyle the border style string, one of: "default", "none",
     * "single", "double", "singleVdoubleH", "singleHdoubleV", or "round"; or
     * null to use the value from casciian.TMenu.borderStyle.
     */
    @Override
    public void setBorderStyleMoving(final String borderStyle) {
        if (borderStyle == null) {
            String style = System.getProperty("casciian.TMenu.borderStyle",
                "single");
            super.setBorderStyleMoving(style);
        } else {
            super.setBorderStyleMoving(borderStyle);
        }
    }

}
