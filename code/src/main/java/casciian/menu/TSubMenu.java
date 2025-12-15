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

import java.util.ArrayList;
import java.util.List;

import casciian.TKeypress;
import casciian.TWidget;
import casciian.backend.Backend;
import casciian.bits.CellAttributes;
import casciian.bits.GraphicsChars;
import casciian.event.TKeypressEvent;
import static casciian.TKeypress.*;

/**
 * TSubMenu is a special case menu item that wraps another TMenu.
 */
public class TSubMenu extends TMenuItem {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The menu window.  Note package private access.
     */
    TMenu menu;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Package private constructor.
     *
     * @param parent parent widget
     * @param title menu title.  Title must contain a keyboard shortcut,
     * denoted by prefixing a letter with "&amp;", e.g. "&amp;File"
     * @param x column relative to parent
     * @param y row relative to parent
     */
    TSubMenu(final TMenu parent, final String title, final int x, final int y) {
        super(parent, TMenu.MID_UNUSED, x, y, title);

        setActive(false);
        setEnabled(true);

        this.menu = new TMenu(parent.getApplication(), x, getAbsoluteY() - 1,
            title);
        setWidth(menu.getWidth() + 2);

        this.menu.isSubMenu = true;
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {

        // Open me if they hit my mnemonic.
        if (!keypress.getKey().isFnKey()
            && !keypress.getKey().isAlt()
            && !keypress.getKey().isCtrl()
            && (getMnemonic() != null)
            && (Character.toLowerCase(getMnemonic().getShortcut())
                == Character.toLowerCase(keypress.getKey().getChar()))
        ) {
            dispatch(keypress.getBackend());
            return;
        }

        if (menu.isActive()) {
            menu.onKeypress(keypress);
            return;
        }

        if (keypress.equals(kbEnter)) {
            dispatch(keypress.getBackend());
            return;
        }

        if (keypress.equals(kbRight)) {
            dispatch(keypress.getBackend());
            return;
        }

        if (keypress.equals(kbDown)) {
            getParent().switchWidget(true);
            return;
        }

        if (keypress.equals(kbUp)) {
            getParent().switchWidget(false);
            return;
        }

        if (keypress.equals(kbLeft)) {
            TMenu parentMenu = (TMenu) getParent();
            if (parentMenu.isSubMenu) {
                getApplication().closeSubMenu();
            } else {
                if (!parentMenu.isContext()) {
                    getApplication().switchMenu(false);
                }
            }
            return;
        }

        if (keypress.equals(kbEsc)) {
            getApplication().closeMenu();
            return;
        }
    }

    // ------------------------------------------------------------------------
    // TMenuItem --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the menu title.
     */
    @Override
    public void draw() {
        super.draw();

        CellAttributes menuColor;
        if (isAbsoluteActive()) {
            menuColor = getTheme().getColor("tmenu.highlighted");
        } else {
            if (isEnabled()) {
                menuColor = getTheme().getColor("tmenu");
            } else {
                menuColor = getTheme().getColor("tmenu.disabled");
            }
        }

        // Add the arrow
        putCharXY(getWidth() - 2, 0, GraphicsChars.CP437[0x10], menuColor);
    }

    /**
     * Override dispatch() to do nothing.
     *
     * @param backend the backend that generated the user input
     */
    @Override
    public void dispatch(final Backend backend) {
        assert (isEnabled());
        if (isAbsoluteActive()) {
            if (!menu.isActive()) {
                menu.setX(getAbsoluteX() + getWidth() - 1);
                menu.setY(getAbsoluteY());
                while (menu.getX() + menu.getWidth() > getScreen().getWidth()) {
                    menu.setX(menu.getX() - 1);
                }
                while (menu.getY() + menu.getHeight() > getApplication().
                    getDesktopBottom()
                ) {
                    menu.setY(menu.getY() - 1);
                }

                getApplication().addSubMenu(menu);
                menu.setActive(true);
                TMenu parentMenu = (TMenu) getParent();
                if (parentMenu.isContext()) {
                    menu.setContext(true, menu.getX(), menu.getY());
                }
            }
        }
    }

    /**
     * Returns my active widget.
     *
     * @return widget that is active, or this if no children
     */
    @Override
    public TWidget getActiveChild() {
        if (menu.isActive()) {
            return menu;
        }
        // Menu not active, return me
        return this;
    }

    // ------------------------------------------------------------------------
    // TSubMenu ---------------------------------------------------------------
    // ------------------------------------------------------------------------

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

        return menu.addItem(id, label, key);
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

        return menu.addItem(id, label, key, enabled);
    }

    /**
     * Convenience function to add a menu item.
     *
     * @param id menu item ID.  Must be greater than 1024.
     * @param label menu item label
     * @return the new menu item
     */
    public TMenuItem addItem(final int id, final String label) {
        return menu.addItem(id, label);
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

        return menu.addItem(id, label, enabled);
    }

    /**
     * Convenience function to add one of the default menu items.
     *
     * @param id menu item ID.  Must be between 0 (inclusive) and 1023
     * (inclusive).
     * @return the new menu item
     */
    public TMenuItem addDefaultItem(final int id) {
        return menu.addDefaultItem(id);
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
        return menu.addDefaultItem(id, enabled);
    }

    /**
     * Convenience function to add a menu separator.
     */
    public void addSeparator() {
        menu.addSeparator();
    }

    /**
     * Convenience function to add a sub-menu.
     *
     * @param title menu title.  Title must contain a keyboard shortcut,
     * denoted by prefixing a letter with "&amp;", e.g. "&amp;File"
     * @return the new sub-menu
     */
    public TSubMenu addSubMenu(final String title) {
        return menu.addSubMenu(title);
    }

    /**
     * Sort the entries in this menu.
     */
    public void sort() {
        sort(Integer.MIN_VALUE);
    }

    /**
     * Sort the entries in this menu by label.
     *
     * @param cutoff any menu ID's less than this value will be placed first
     * in the list, and stay in the previous order to each other
     */
    public void sort(final int cutoff) {
        int n = menu.getChildren().size();
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                TMenuItem a = (TMenuItem) menu.getChildren().get(i);
                TMenuItem b = (TMenuItem) menu.getChildren().get(j);
                if ((a.getId() < cutoff) && (b.getId() < cutoff)) {
                    continue;
                }
                if ((a.getId() >= cutoff) && (b.getId() < cutoff)) {
                    menu.getChildren().set(i, b);
                    menu.getChildren().set(j, a);
                    continue;
                }
                if ((a.getId() >= cutoff) && (b.getId() >= cutoff)) {
                    String aLabel = a.getMnemonic().getRawLabel();
                    String bLabel = b.getMnemonic().getRawLabel();
                    if (aLabel.compareTo(bLabel) > 0) {
                        menu.getChildren().set(i, b);
                        menu.getChildren().set(j, a);
                    }
                    continue;
                }
            }
        }
        for (int i = 0; i < n; i++) {
            menu.getChildren().get(i).setY(i + 1);
        }
        menu.resetTabOrder();
    }

    /**
     * Get the menu items.
     *
     * @return the menu items
     */
    public List<TMenuItem> getMenuItems() {
        List<TMenuItem> items = new ArrayList<TMenuItem>();
        for (TWidget w: menu.getChildren()) {
            if (w instanceof TMenuItem) {
                items.add((TMenuItem) w);
            }
        }
        return items;
    }
}
