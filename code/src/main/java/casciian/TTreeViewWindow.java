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
import casciian.TApplication;
import casciian.THScroller;
import casciian.TScrollableWindow;
import casciian.TVScroller;
import casciian.TWidget;
import casciian.bits.StringUtils;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import casciian.event.TResizeEvent;
import static casciian.TKeypress.*;

/**
 * TTreeViewWindow wraps a tree view with horizontal and vertical scrollbars
 * in a standalone window.
 */
public class TTreeViewWindow extends TScrollableWindow {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The TTreeView
     */
    private TTreeView treeView;

    /**
     * If true, move the window to put the selected item in view.  This
     * normally only happens once after setting treeRoot.
     */
    private boolean centerWindow = false;

    /**
     * Maximum width of a single line.
     */
    private int maxLineWidth;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent the main application
     * @param title the window title
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of tree view
     * @param flags bitmask of RESIZABLE, CENTERED, or MODAL
     * @param height height of tree view
     */
    public TTreeViewWindow(final TApplication parent, final String title,
        final int x, final int y, final int width, final int height,
        final int flags) {

        this(parent, title, x, y, width, height, flags, null);
    }

    /**
     * Public constructor.
     *
     * @param parent the main application
     * @param title the window title
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of tree view
     * @param height height of tree view
     * @param flags bitmask of RESIZABLE, CENTERED, or MODAL
     * @param action action to perform when an item is selected
     */
    @SuppressWarnings("this-escape")
    public TTreeViewWindow(final TApplication parent, final String title,
        final int x, final int y, final int width, final int height,
        final int flags, final TAction action) {

        super(parent, title, x, y, width, height, flags);

        treeView = new TTreeView(this, 0, 0, getWidth() - 2, getHeight() - 2,
            action);

        hScroller = new THScroller(this, 17, getHeight() - 2, getWidth() - 20);
        vScroller = new TVScroller(this, getWidth() - 2, 0, getHeight() - 2);

        /*
        System.err.println("TTreeViewWindow()");
        for (TWidget w: getChildren()) {
            System.err.println("    " + w + " " + w.isActive());
        }
        */
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if (mouse.isMouseWheelUp()) {
            verticalDecrement();
        } else if (mouse.isMouseWheelDown()) {
            verticalIncrement();
        } else {
            // Pass to the TreeView or scrollbars
            super.onMouseDown(mouse);
        }

        // Update the view to reflect the new scrollbar positions
        treeView.setTopLine(getVerticalValue());
        treeView.setLeftColumn(getHorizontalValue());
        reflowData();
    }

    /**
     * Handle mouse release events.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        // Pass to the TreeView or scrollbars
        super.onMouseUp(mouse);

        // Update the view to reflect the new scrollbar positions
        treeView.setTopLine(getVerticalValue());
        treeView.setLeftColumn(getHorizontalValue());
        reflowData();
    }

    /**
     * Handle mouse motion events.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        // Pass to the TreeView or scrollbars
        super.onMouseMotion(mouse);

        // Update the view to reflect the new scrollbar positions
        treeView.setTopLine(getVerticalValue());
        treeView.setLeftColumn(getHorizontalValue());
        reflowData();
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (inKeyboardResize) {
            // Let TWindow do its job.
            super.onKeypress(keypress);
            return;
        }

        // Give the shortcut bar a shot at this.
        if (statusBar != null) {
            if (statusBar.statusBarKeypress(keypress)) {
                return;
            }
        }

        if (keypress.equals(kbShiftLeft)
            || keypress.equals(kbCtrlLeft)
            || keypress.equals(kbAltLeft)
        ) {
            horizontalDecrement();
        } else if (keypress.equals(kbShiftRight)
            || keypress.equals(kbCtrlRight)
            || keypress.equals(kbAltRight)
        ) {
            horizontalIncrement();
        } else if (keypress.equals(kbShiftUp)
            || keypress.equals(kbCtrlUp)
            || keypress.equals(kbAltUp)
        ) {
            verticalDecrement();
        } else if (keypress.equals(kbShiftDown)
            || keypress.equals(kbCtrlDown)
            || keypress.equals(kbAltDown)
        ) {
            verticalIncrement();
        } else if (keypress.equals(kbShiftPgUp)
            || keypress.equals(kbCtrlPgUp)
            || keypress.equals(kbAltPgUp)
        ) {
            bigVerticalDecrement();
        } else if (keypress.equals(kbShiftPgDn)
            || keypress.equals(kbCtrlPgDn)
            || keypress.equals(kbAltPgDn)
        ) {
            bigVerticalIncrement();
        } else {
            treeView.onKeypress(keypress);

            // Update the scrollbars to reflect the new data position
            reflowData();
            return;
        }

        // Update the view to reflect the new scrollbar position
        treeView.setTopLine(getVerticalValue());
        treeView.setLeftColumn(getHorizontalValue());
        reflowData();
    }

    // ------------------------------------------------------------------------
    // TScrollableWindow ------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle window/screen resize events.
     *
     * @param resize resize event
     */
    @Override
    public void onResize(final TResizeEvent resize) {
        if (resize.getType() == TResizeEvent.Type.WIDGET) {
            // Resize the treeView field.
            TResizeEvent treeSize = new TResizeEvent(resize.getBackend(),
                TResizeEvent.Type.WIDGET, resize.getWidth() - 2,
                resize.getHeight() - 2);
            treeView.onResize(treeSize);

            // Have TScrollableWindow handle the scrollbars.
            super.onResize(resize);

            // Now re-center the treeView field.
            if (treeView.getSelected() != null) {
                treeView.setSelected(treeView.getSelected(), true);
            }
            reflowData();
            return;
        }
    }

    /**
     * Resize text and scrollbars for a new width/height.
     */
    @Override
    public void reflowData() {
        int selectedRow = 0;
        boolean foundSelectedRow = false;

        // Reset the keyboard list, expandTree() will recreate it.
        for (TWidget widget: treeView.getChildren()) {
            TTreeItem item = (TTreeItem) widget;
            item.keyboardPrevious = null;
            item.keyboardNext = null;
        }

        // Expand the tree into a linear list
        treeView.getChildren().clear();
        treeView.getChildren().addAll(treeView.getTreeRoot().expandTree("",
                true));

        // Locate the selected row and maximum line width
        for (TWidget widget: treeView.getChildren()) {
            TTreeItem item = (TTreeItem) widget;

            if (item == treeView.getSelected()) {
                foundSelectedRow = true;
            }
            if (!foundSelectedRow) {
                selectedRow++;
            }

            int lineWidth = StringUtils.width(item.getText())
                + item.getPrefix().length() + 4;
            if (lineWidth > maxLineWidth) {
                maxLineWidth = lineWidth;
            }
        }

        if ((centerWindow) && (foundSelectedRow)) {
            if ((selectedRow < getVerticalValue())
                || (selectedRow > getVerticalValue() + getHeight() - 3)
            ) {
                treeView.setTopLine(selectedRow);
                centerWindow = false;
            }
        }
        treeView.alignTree();

        // Rescale the scroll bars
        setVerticalValue(treeView.getTopLine());
        setBottomValue(treeView.getTotalLineCount() - (getHeight() - 2));
        if (getBottomValue() < getTopValue()) {
            setBottomValue(getTopValue());
        }
        if (getVerticalValue() > getBottomValue()) {
            setVerticalValue(getBottomValue());
        }
        setRightValue(maxLineWidth - 4);
        if (getHorizontalValue() > getRightValue()) {
            setHorizontalValue(getRightValue());
        }
    }

    // ------------------------------------------------------------------------
    // TTreeView --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the underlying TTreeView.
     *
     * @return the TTreeView
     */
    public TTreeView getTreeView() {
        return treeView;
    }

    /**
     * Get the root of the tree.
     *
     * @return the root of the tree
     */
    public final TTreeItem getTreeRoot() {
        return treeView.getTreeRoot();
    }

    /**
     * Set the root of the tree.
     *
     * @param treeRoot the new root of the tree
     */
    public final void setTreeRoot(final TTreeItem treeRoot) {
        treeView.setTreeRoot(treeRoot);
    }

    /**
     * Set treeRoot.
     *
     * @param treeRoot ultimate root of tree
     * @param centerWindow if true, move the window to put the root in view
     */
    public void setTreeRoot(final TTreeItem treeRoot,
        final boolean centerWindow) {

        treeView.setTreeRoot(treeRoot);
        this.centerWindow = centerWindow;
    }

    /**
     * Get the tree view item that was selected.
     *
     * @return the selected item, or null if no item is selected
     */
    public final TTreeItem getSelected() {
        return treeView.getSelected();
    }

    /**
     * Set the new selected tree view item.
     *
     * @param item new item that became selected
     * @param centerWindow if true, move the window to put the selected into
     * view
     */
    public void setSelected(final TTreeItem item, final boolean centerWindow) {
        treeView.setSelected(item, centerWindow);
    }

    /**
     * Perform user selection action.
     */
    public void dispatch() {
        treeView.dispatch();
    }

}
