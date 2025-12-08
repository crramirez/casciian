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
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import static casciian.TKeypress.*;

/**
 * TTreeItem is a single item in a tree view.
 */
public class TTreeItem extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Hang onto reference to my parent TTreeView so I can call its reflow()
     * when I add a child node.
     */
    private TTreeView view;

    /**
     * Displayable text for this item.
     */
    private String text;

    /**
     * If true, this item is expanded in the tree view.
     */
    private boolean expanded = true;

    /**
     * If true, this item can be expanded in the tree view.
     */
    private boolean expandable = false;

    /**
     * The vertical bars and such along the left side.
     */
    private String prefix = "";

    /**
     * Tree level.
     */
    protected int level = 0;

    /**
     * True means selected.
     */
    private boolean selected = false;

    /**
     * True means select-able.
     */
    private boolean selectable = true;

    /**
     * Whether or not this item is last in its parent's list of children.
     */
    private boolean last = false;

    /**
     * Pointer to the previous keyboard-navigable item (kbUp).  Note package
     * private access.
     */
    TTreeItem keyboardPrevious = null;

    /**
     * Pointer to the next keyboard-navigable item (kbDown).  Note package
     * private access.
     */
    TTreeItem keyboardNext = null;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param view root TTreeView
     * @param text text for this item
     * @param expanded if true, have it expanded immediately
     */
    @SuppressWarnings("this-escape")
    public TTreeItem(final TTreeView view, final String text,
        final boolean expanded) {

        super(view, 0, 0, view.getWidth() - 3, 1);

        this.text = text;
        this.expanded = expanded;
        this.view = view;

        if (view.getTreeRoot() == null) {
            view.setTreeRoot(this);
        } else {
            view.alignTree();
        }
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle mouse release events.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        if ((mouse.getX() == (getExpanderX() - view.getLeftColumn()))
            && (mouse.getY() == 0)
        ) {
            if (level == 0) {
                // Root node can't switch.
                return;
            }
            if (selectable) {
                // Flip expanded flag
                expanded = !expanded;
                if (expanded == false) {
                    // Unselect children that became invisible
                    unselect();
                }
                view.setSelected(this, false);
            }
            // Let subclasses do something with this
            onExpand();

            // Update the screen after any thing has expanded/contracted
            view.alignTree();
        } else if (mouse.getY() == 0) {
            // Do the action associated with this item.
            view.setSelected(this, false);
            view.dispatch();
        }
    }

    /**
     * Called when this item is expanded or collapsed.  this.expanded will be
     * true if this item was just expanded from a mouse click or keypress.
     */
    public void onExpand() {
        // Default: do nothing.
        if (!expandable) {
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
        if (keypress.equals(kbLeft)
            || keypress.equals(kbRight)
            || keypress.equals(kbSpace)
        ) {
            if (level == 0) {
                // Root node can't switch.
                return;
            }
            if (selectable) {
                // Flip expanded flag
                expanded = !expanded;
                if (expanded == false) {
                    // Unselect children that became invisible
                    unselect();
                }
                view.setSelected(this, false);
            }
            // Let subclasses do something with this
            onExpand();
        } else if (keypress.equals(kbEnter)) {
            // Do the action associated with this item.
            view.dispatch();
        } else {
            // Pass other keys (tab etc.) on to TWidget's handler.
            super.onKeypress(keypress);
        }
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw this item to a window.
     */
    @Override
    public void draw() {
        if ((getY() < 0) || (getY() > getParent().getHeight() - 1)) {
            return;
        }

        int offset = -view.getLeftColumn();

        CellAttributes color = getTheme().getColor("ttreeview");
        CellAttributes textColor = getTheme().getColor("ttreeview");
        CellAttributes expanderColor = getTheme().getColor("ttreeview.expandbutton");
        CellAttributes selectedColor = getTheme().getColor("ttreeview.selected");

        if (!getParent().isAbsoluteActive()) {
            color = getTheme().getColor("ttreeview.inactive");
            textColor = getTheme().getColor("ttreeview.inactive");
            selectedColor = getTheme().getColor("ttreeview.selected.inactive");
        }

        if (!selectable) {
            textColor = getTheme().getColor("ttreeview.unreadable");
        }

        // Blank out the background
        hLineXY(0, 0, getWidth(), ' ', color);

        String line = prefix;
        if (level > 0) {
            if (last) {
                line += GraphicsChars.CP437[0xC0];
            } else {
                line += GraphicsChars.CP437[0xC3];
            }
            line += GraphicsChars.CP437[0xC4];
            if (expandable) {
                line += "[ ] ";
            } else {
                line += " ";
            }
        }
        putStringXY(offset, 0, line, color);
        if (selected) {
            putStringXY(offset + StringUtils.width(line), 0, text, selectedColor);
        } else {
            putStringXY(offset + StringUtils.width(line), 0, text, textColor);
        }
        if ((level > 0) && (expandable)) {
            if (expanded) {
                putCharXY(offset + getExpanderX(), 0, '-', expanderColor);
            } else {
                putCharXY(offset + getExpanderX(), 0, '+', expanderColor);
            }
        }
    }

    // ------------------------------------------------------------------------
    // TTreeItem --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the parent TTreeView.
     *
     * @return the parent TTreeView
     */
    public final TTreeView getTreeView() {
        return view;
    }

    /**
     * Get the displayable text for this item.
     *
     * @return the displayable text for this item
     */
    public final String getText() {
        return text;
    }

    /**
     * Set the displayable text for this item.
     *
     * @param text the displayable text for this item
     */
    public final void setText(final String text) {
        this.text = text;
    }

    /**
     * Get expanded value.
     *
     * @return if true, this item is expanded
     */
    public final boolean isExpanded() {
        return expanded;
    }

    /**
     * Set expanded value.
     *
     * @param expanded new value
     */
    public final void setExpanded(final boolean expanded) {
        if (level == 0) {
            // Root node can't be unexpanded, ever.
            this.expanded = true;
            return;
        }
        if (level > 0) {
            this.expanded = expanded;
        }
    }

    /**
     * Get expandable value.
     *
     * @return if true, this item is expandable
     */
    public final boolean isExpandable() {
        return expandable;
    }

    /**
     * Set expandable value.
     *
     * @param expandable new value
     */
    public final void setExpandable(final boolean expandable) {
        if (level == 0) {
            // Root node can't be unexpanded, ever.
            this.expandable = true;
            return;
        }
        if (level > 0) {
            this.expandable = expandable;
        }
    }

    /**
     * Get the vertical bars and such along the left side.
     *
     * @return the vertical bars and such along the left side
     */
    public final String getPrefix() {
        return prefix;
    }

    /**
     * Get selected value.
     *
     * @return if true, this item is selected
     */
    public final boolean isSelected() {
        return selected;
    }

    /**
     * Set selected value.
     *
     * @param selected new value
     */
    public final void setSelected(final boolean selected) {
        this.selected = selected;
    }

    /**
     * Set selectable value.
     *
     * @param selectable new value
     */
    public final void setSelectable(final boolean selectable) {
        this.selectable = selectable;
    }

    /**
     * Get the length of the widest item to display.
     *
     * @return the maximum number of columns for this item or its children
     */
    public int getMaximumColumn() {
        int max = prefix.length() + 4 + StringUtils.width(text);
        for (TWidget widget: getChildren()) {
            TTreeItem item = (TTreeItem) widget;
            int n = item.prefix.length() + 4 + StringUtils.width(item.text);
            if (n > max) {
                max = n;
            }
        }
        return max;
    }

    /**
     * Recursively expand the tree into a linear array of items.
     *
     * @param prefix vertical bar of parent levels and such that is set on
     * each child
     * @param last if true, this is the "last" leaf node of a tree
     * @return additional items to add to the array
     */
    public List<TTreeItem> expandTree(final String prefix, final boolean last) {
        List<TTreeItem> array = new ArrayList<TTreeItem>();
        this.last = last;
        this.prefix = prefix;
        array.add(this);

        if ((getChildren().size() == 0) || !expanded) {
            return array;
        }

        String newPrefix = prefix;
        if (level > 0) {
            if (last) {
                newPrefix += "  ";
            } else {
                newPrefix += GraphicsChars.CP437[0xB3];
                newPrefix += ' ';
            }
        }
        for (int i = 0; i < getChildren().size(); i++) {
            TTreeItem item = (TTreeItem) getChildren().get(i);
            if (i == getChildren().size() - 1) {
                array.addAll(item.expandTree(newPrefix, true));
            } else {
                array.addAll(item.expandTree(newPrefix, false));
            }
        }
        return array;
    }

    /**
     * Get the x spot for the + or - to expand/collapse.
     *
     * @return column of the expand/collapse button
     */
    private int getExpanderX() {
        if ((level == 0) || (!expandable)) {
            return 0;
        }
        return prefix.length() + 3;
    }

    /**
     * Recursively unselect me and my children.
     */
    public void unselect() {
        if (selected == true) {
            selected = false;
            view.setSelected(null, false);
        }
        for (TWidget widget: getChildren()) {
            if (widget instanceof TTreeItem) {
                TTreeItem item = (TTreeItem) widget;
                item.unselect();
            }
        }
    }

}
