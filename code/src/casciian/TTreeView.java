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

import casciian.TAction;
import casciian.TKeypress;
import casciian.TWidget;
import casciian.event.TKeypressEvent;
import static casciian.TKeypress.*;

/**
 * TTreeView implements a simple tree view.
 */
public class TTreeView extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Root of the tree.
     */
    private TTreeItem treeRoot;

    /**
     * Only one of my children can be selected.
     */
    private TTreeItem selectedItem = null;

    /**
     * The action to perform when the user selects an item.
     */
    private TAction action = null;

    /**
     * The top line currently visible.
     */
    private int topLine = 0;

    /**
     * The left column currently visible.
     */
    private int leftColumn = 0;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of tree view
     * @param height height of tree view
     */
    public TTreeView(final TWidget parent, final int x, final int y,
        final int width, final int height) {

        this(parent, x, y, width, height, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of tree view
     * @param height height of tree view
     * @param action action to perform when an item is selected
     */
    public TTreeView(final TWidget parent, final int x, final int y,
        final int width, final int height, final TAction action) {

        super(parent, x, y, width, height);
        this.action = action;
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
        if (keypress.equals(kbUp)) {
            // Select the previous item
            if (selectedItem != null) {
                if (selectedItem.keyboardPrevious != null) {
                    setSelected(selectedItem.keyboardPrevious, true);
                }
            }
        } else if (keypress.equals(kbDown)) {
            // Select the next item
            if (selectedItem != null) {
                if (selectedItem.keyboardNext != null) {
                    setSelected(selectedItem.keyboardNext, true);
                }
            }
        } else if (keypress.equals(kbPgDn)) {
            for (int i = 0; i < getHeight() - 1; i++) {
                onKeypress(new TKeypressEvent(keypress.getBackend(),
                        TKeypress.kbDown));
            }
        } else if (keypress.equals(kbPgUp)) {
            for (int i = 0; i < getHeight() - 1; i++) {
                onKeypress(new TKeypressEvent(keypress.getBackend(),
                        TKeypress.kbUp));
            }
        } else if (keypress.equals(kbHome)) {
            setSelected((TTreeItem) getChildren().get(0), false);
            setTopLine(0);
        } else if (keypress.equals(kbEnd)) {
            setSelected((TTreeItem) getChildren().get(getChildren().size() - 1),
                true);
        } else {
            if (selectedItem != null) {
                selectedItem.onKeypress(keypress);
            } else {
                // Pass other keys (tab etc.) on to TWidget's handler.
                super.onKeypress(keypress);
            }
        }
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // TTreeView --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the root of the tree.
     *
     * @return the root of the tree
     */
    public final TTreeItem getTreeRoot() {
        return treeRoot;
    }

    /**
     * Set the root of the tree.
     *
     * @param treeRoot the new root of the tree
     */
    public final void setTreeRoot(final TTreeItem treeRoot) {
        this.treeRoot = treeRoot;
        alignTree();
    }

    /**
     * Get the tree view item that was selected.
     *
     * @return the selected item, or null if no item is selected
     */
    public final TTreeItem getSelected() {
        return selectedItem;
    }

    /**
     * Set the new selected tree view item.
     *
     * @param item new item that became selected
     * @param centerWindow if true, move the window to put the selected into
     * view
     */
    public void setSelected(final TTreeItem item, final boolean centerWindow) {
        if (item != null) {
            item.setSelected(true);
        }
        if ((selectedItem != null) && (selectedItem != item)) {
            selectedItem.setSelected(false);
        }
        selectedItem = item;

        if (centerWindow) {
            int y = 0;
            for (TWidget widget: getChildren()) {
                if (widget == selectedItem) {
                    break;
                }
                y++;
            }
            topLine = y - (getHeight() - 1)/2;
            if (topLine > getChildren().size() - getHeight()) {
                topLine = getChildren().size() - getHeight();
            }
            if (topLine < 0) {
                topLine = 0;
            }
        }

        if (selectedItem != null) {
            activate(selectedItem);
        }
    }

    /**
     * Perform user selection action.
     */
    public void dispatch() {
        if (action != null) {
            action.DO(this);
        }
    }

    /**
     * Get the left column value.  0 is the leftmost column.
     *
     * @return the left column
     */
    public int getLeftColumn() {
        return leftColumn;
    }

    /**
     * Set the left column value.  0 is the leftmost column.
     *
     * @param leftColumn the new left column
     */
    public void setLeftColumn(final int leftColumn) {
        this.leftColumn = leftColumn;
    }

    /**
     * Get the top line (row) value.  0 is the topmost line.
     *
     * @return the top line
     */
    public int getTopLine() {
        return topLine;
    }

    /**
     * Set the top line value.  0 is the topmost line.
     *
     * @param topLine the new top line
     */
    public void setTopLine(final int topLine) {
        this.topLine = topLine;
    }

    /**
     * Get the total line (rows) count, based on the items that are visible
     * and expanded.
     *
     * @return the line count
     */
    public int getTotalLineCount() {
        if (treeRoot == null) {
            return 0;
        }
        return getChildren().size();
    }

    /**
     * Get the length of the widest item to display.
     *
     * @return the maximum number of columns for this item or its children
     */
    public int getMaximumColumn() {
        if (treeRoot == null) {
            return 0;
        }
        return treeRoot.getMaximumColumn();
    }

    /**
     * Update the Y positions of all the children items to match the current
     * topLine value.  Note package private access.
     */
    void alignTree() {
        if (treeRoot == null) {
            return;
        }

        // As we walk the list we also adjust next/previous pointers,
        // resulting in a doubly-linked list but only of the expanded items.
        TTreeItem p = null;

        for (int i = 0; i < getChildren().size(); i++) {
            TTreeItem item = (TTreeItem) getChildren().get(i);

            if (p != null) {
                item.keyboardPrevious = p;
                p.keyboardNext = item;
            }
            p = item;

            item.setY(i - topLine);
            item.setWidth(getWidth());
        }

    }

}
