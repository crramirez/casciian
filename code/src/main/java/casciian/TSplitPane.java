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
import casciian.event.TMouseEvent;
import casciian.event.TResizeEvent;

/**
 * TSplitPane contains two widgets with a draggable horizontal or vertical
 * bar between them.
 */
public class TSplitPane extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * If true, split vertically.  If false, split horizontally.
     */
    private boolean vertical = true;

    /**
     * The location of the split bar, either as a column number for vertical
     * split or a row number for horizontal split.
     */
    private int split = 0;

    /**
     * The widget on the left side.
     */
    private TWidget left;

    /**
     * The widget on the right side.
     */
    private TWidget right;

    /**
     * The widget on the top side.
     */
    private TWidget top;

    /**
     * The widget on the bottom side.
     */
    private TWidget bottom;

    /**
     * If true, we are in the middle of a split move.
     */
    private boolean inSplitMove = false;

    /**
     * The last seen mouse position.
     */
    private TMouseEvent mouse;

    /**
     * If true, focus will follow mouse.
     */
    private boolean focusFollowsMouse = false;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of widget
     * @param height height of widget
     * @param vertical if true, split vertically
     */
    @SuppressWarnings("this-escape")
    public TSplitPane(final TWidget parent, final int x, final int y,
        final int width, final int height, final boolean vertical) {

        super(parent, x, y, width, height);

        this.vertical = vertical;
        center();
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle window/screen resize events.
     *
     * @param event resize event
     */
    @Override
    public void onResize(final TResizeEvent event) {
        if (event.getType() == TResizeEvent.Type.WIDGET) {
            // Resize me
            super.onResize(event);

            if (vertical && (split >= getWidth() - 2)) {
                center();
            } else if (!vertical && (split >= getHeight() - 2)) {
                center();
            } else {
                layoutChildren();
            }
        }
    }

    /**
     * Handle mouse button presses.
     *
     * @param mouse mouse button event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        this.mouse = mouse;

        inSplitMove = false;

        if (mouse.isMouse1()) {
            if (vertical) {
                inSplitMove = (mouse.getAbsoluteX() - getAbsoluteX() == split);
            } else {
                inSplitMove = (mouse.getAbsoluteY() - getAbsoluteY() == split);
            }
            if (inSplitMove) {
                return;
            }
        }

        // I didn't take it, pass it on to my children
        super.onMouseDown(mouse);
    }

    /**
     * Handle mouse button releases.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        this.mouse = mouse;

        if (inSplitMove && mouse.isMouse1()) {
            // Stop moving split
            inSplitMove = false;
            return;
        }

        // I didn't take it, pass it on to my children
        super.onMouseUp(mouse);
    }

    /**
     * Handle mouse movements.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        this.mouse = mouse;

        if ((mouse.getAbsoluteX() - getAbsoluteX() < 0)
            || (mouse.getAbsoluteX() - getAbsoluteX() >= getWidth())
            || (mouse.getAbsoluteY() - getAbsoluteY() < 0)
            || (mouse.getAbsoluteY() - getAbsoluteY() >= getHeight())
        ) {
            // Mouse has travelled out of my window.
            inSplitMove = false;
        }

        if (focusFollowsMouse) {
            if ((top != null) && (top.mouseWouldHit(mouse))) {
                activate(top);
            } else if ((bottom != null) && (bottom.mouseWouldHit(mouse))) {
                activate(bottom);
            } else if ((left != null) && (left.mouseWouldHit(mouse))) {
                activate(left);
            } else if ((right != null) && (right.mouseWouldHit(mouse))) {
                activate(right);
            }
        }

        if (inSplitMove) {
            if (vertical) {
                split = mouse.getAbsoluteX() - getAbsoluteX();
                split = Math.min(Math.max(1, split), getWidth() - 2);
            } else {
                split = mouse.getAbsoluteY() - getAbsoluteY();
                split = Math.min(Math.max(1, split), getHeight() - 2);
            }
            layoutChildren();
            return;
        }

        // I didn't take it, pass it on to my children
        super.onMouseMotion(mouse);
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw me on screen.
     */
    @Override
    public void draw() {
        CellAttributes attr = getTheme().getColor("tsplitpane");
        if (vertical) {
            vLineXY(split, 0, getHeight(), GraphicsChars.WINDOW_SIDE, attr);

            // Draw intersections of children
            if ((left instanceof TSplitPane)
                && (((TSplitPane) left).vertical == false)
                && (right instanceof TSplitPane)
                && (((TSplitPane) right).vertical == false)
                && (((TSplitPane) left).split == ((TSplitPane) right).split)
            ) {
                putCharXY(split, ((TSplitPane) left).split, '\u253C', attr);
            } else {
                if ((left instanceof TSplitPane)
                    && (((TSplitPane) left).vertical == false)
                ) {
                    putCharXY(split, ((TSplitPane) left).split, '\u2524', attr);
                }
                if ((right instanceof TSplitPane)
                    && (((TSplitPane) right).vertical == false)
                ) {
                    putCharXY(split, ((TSplitPane) right).split, '\u251C',
                        attr);
                }
            }

            if ((mouse != null)
                && (mouse.getAbsoluteX() == getAbsoluteX() + split)
                && (mouse.getAbsoluteY() >= getAbsoluteY()) &&
                (mouse.getAbsoluteY() < getAbsoluteY() + getHeight())
            ) {
                putCharXY(split, mouse.getAbsoluteY() - getAbsoluteY(),
                    '\u2194', attr);
            }
        } else {
            hLineXY(0, split, getWidth(), GraphicsChars.SINGLE_BAR, attr);

            // Draw intersections of children
            if ((top instanceof TSplitPane)
                && (((TSplitPane) top).vertical == true)
                && (bottom instanceof TSplitPane)
                && (((TSplitPane) bottom).vertical == true)
                && (((TSplitPane) top).split == ((TSplitPane) bottom).split)
            ) {
                putCharXY(((TSplitPane) top).split, split, '\u253C', attr);
            } else {
                if ((top instanceof TSplitPane)
                    && (((TSplitPane) top).vertical == true)
                ) {
                    putCharXY(((TSplitPane) top).split, split, '\u2534', attr);
                }
                if ((bottom instanceof TSplitPane)
                    && (((TSplitPane) bottom).vertical == true)
                ) {
                    putCharXY(((TSplitPane) bottom).split, split, '\u252C',
                        attr);
                }
            }

            if ((mouse != null)
                && (mouse.getAbsoluteY() == getAbsoluteY() + split)
                && (mouse.getAbsoluteX() >= getAbsoluteX()) &&
                (mouse.getAbsoluteX() < getAbsoluteX() + getWidth())
            ) {
                putCharXY(mouse.getAbsoluteX() - getAbsoluteX(), split,
                    '\u2195', attr);
            }
        }

    }

    /**
     * Generate a human-readable string for this widget.
     *
     * @return a human-readable string
     */
    @Override
    public String toString() {
        return String.format("%s(%8x) %s position (%d, %d) geometry %dx%d " +
            "split %d left %s(%8x) right %s(%8x) top %s(%8x) bottom %s(%8x) " +
            "active %s enabled %s visible %s", getClass().getName(),
            hashCode(), (vertical ? "VERTICAL" : "HORIZONTAL"),
            getX(), getY(), getWidth(), getHeight(), split,
            (left == null ? "null" : left.getClass().getName()),
            (left == null ? 0 : left.hashCode()),
            (right == null ? "null" : right.getClass().getName()),
            (right == null ? 0 : right.hashCode()),
            (top == null ? "null" : top.getClass().getName()),
            (top == null ? 0 : top.hashCode()),
            (bottom == null ? "null" : bottom.getClass().getName()),
            (bottom == null ? 0 : bottom.hashCode()),
            isActive(), isEnabled(), isVisible());
    }

    /**
     * Reset the tab order of children to match their position in the list.
     * Available so that subclasses can re-order their widgets if needed.
     */
    @Override
    protected void resetTabOrder() {
        if (getChildren().size() < 2) {
            super.resetTabOrder();
            return;
        }
        if (vertical) {
            left.tabOrder = 0;
            right.tabOrder = 1;
        } else {
            top.tabOrder = 0;
            bottom.tabOrder = 1;
        }
    }

    // ------------------------------------------------------------------------
    // TSplitPane -------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get focusFollowsMouse flag.
     *
     * @return true if focus follows mouse: widgets automatically activate
     * if the mouse passes over them
     */
    public boolean getFocusFollowsMouse() {
        return focusFollowsMouse;
    }

    /**
     * Set focusFollowsMouse flag.
     *
     * @param focusFollowsMouse if true, focus follows mouse: widgets are
     * automatically activated if the mouse passes over them
     */
    public void setFocusFollowsMouse(final boolean focusFollowsMouse) {
        this.focusFollowsMouse = focusFollowsMouse;
    }

    /**
     * Set focusFollowsMouse flag.
     *
     * @param focusFollowsMouse if true, focus follows mouse: widgets are
     * automatically activated if the mouse passes over them
     * @param recursive if true, set the focusFollowsMouse flag of all child
     * TSplitPane's recursively
     */
    public void setFocusFollowsMouse(final boolean focusFollowsMouse,
        final boolean recursive) {

        this.focusFollowsMouse = focusFollowsMouse;
        if (recursive) {
            if (left instanceof TSplitPane) {
                ((TSplitPane) left).setFocusFollowsMouse(recursive);
            }
            if (right instanceof TSplitPane) {
                ((TSplitPane) right).setFocusFollowsMouse(recursive);
            }
            if (top instanceof TSplitPane) {
                ((TSplitPane) top).setFocusFollowsMouse(recursive);
            }
            if (bottom instanceof TSplitPane) {
                ((TSplitPane) bottom).setFocusFollowsMouse(recursive);
            }
        }
    }

    /**
     * Get the widget on the left side.
     *
     * @return the widget on the left, or null if not set
     */
    public TWidget getLeft() {
        return left;
    }

    /**
     * Set the widget on the left side.
     *
     * @param left the widget to set, or null to remove
     */
    public void setLeft(final TWidget left) {
        if (!vertical) {
            throw new IllegalArgumentException("cannot set left on " +
                "horizontal split pane");
        }
        if (left == null) {
            if (this.left != null) {
                remove(this.left);
            }
            this.left = null;
            return;
        }
        this.left = left;
        left.setParent(this, false);
        onResize(new TResizeEvent(null, TResizeEvent.Type.WIDGET, getWidth(),
                getHeight()));
    }

    /**
     * Get the widget on the right side.
     *
     * @return the widget on the right, or null if not set
     */
    public TWidget getRight() {
        return right;
    }

    /**
     * Set the widget on the right side.
     *
     * @param right the widget to set, or null to remove
     */
    public void setRight(final TWidget right) {
        if (!vertical) {
            throw new IllegalArgumentException("cannot set right on " +
                "horizontal split pane");
        }
        if (right == null) {
            if (this.right != null) {
                remove(this.right);
            }
            this.right = null;
            return;
        }
        this.right = right;
        right.setParent(this, false);
        onResize(new TResizeEvent(null, TResizeEvent.Type.WIDGET, getWidth(),
                getHeight()));
    }

    /**
     * Get the widget on the top side.
     *
     * @return the widget on the top, or null if not set
     */
    public TWidget getTop() {
        return top;
    }

    /**
     * Set the widget on the top side.
     *
     * @param top the widget to set, or null to remove
     */
    public void setTop(final TWidget top) {
        if (vertical) {
            throw new IllegalArgumentException("cannot set top on vertical " +
                "split pane");
        }
        if (top == null) {
            if (this.top != null) {
                remove(this.top);
            }
            this.top = null;
            return;
        }
        this.top = top;
        top.setParent(this, false);
        onResize(new TResizeEvent(null, TResizeEvent.Type.WIDGET, getWidth(),
                getHeight()));
    }

    /**
     * Get the widget on the bottom side.
     *
     * @return the widget on the bottom, or null if not set
     */
    public TWidget getBottom() {
        return bottom;
    }

    /**
     * Set the widget on the bottom side.
     *
     * @param bottom the widget to set, or null to remove
     */
    public void setBottom(final TWidget bottom) {
        if (vertical) {
            throw new IllegalArgumentException("cannot set bottom on " +
                "vertical split pane");
        }
        if (bottom == null) {
            if (this.bottom != null) {
                remove(this.bottom);
            }
            this.bottom = null;
            return;
        }
        this.bottom = bottom;
        bottom.setParent(this, false);
        onResize(new TResizeEvent(null, TResizeEvent.Type.WIDGET, getWidth(),
                getHeight()));
    }

    /**
     * Remove a widget, regardless of what pane it is on.
     *
     * @param widget the widget to remove
     */
    public void removeWidget(final TWidget widget) {
        if (widget == null) {
            throw new IllegalArgumentException("cannot remove null widget");
        }
        if (left == widget) {
            left = null;
            assert(right != widget);
            assert(top != widget);
            assert(bottom != widget);
            return;
        }
        if (right == widget) {
            right = null;
            assert(left != widget);
            assert(top != widget);
            assert(bottom != widget);
            return;
        }
        if (top == widget) {
            top = null;
            assert(left != widget);
            assert(right != widget);
            assert(bottom != widget);
            return;
        }
        if (bottom == widget) {
            bottom = null;
            assert(left != widget);
            assert(right != widget);
            assert(top != widget);
            return;
        }
        throw new IllegalArgumentException("widget " + widget +
            " not in this split");
    }

    /**
     * Replace a widget, regardless of what pane it is on, with another
     * widget.
     *
     * @param oldWidget the widget to remove
     * @param newWidget the widget to replace it with
     */
    public void replaceWidget(final TWidget oldWidget,
        final TWidget newWidget) {

        if (oldWidget == null) {
            throw new IllegalArgumentException("cannot remove null oldWidget");
        }
        if (left == oldWidget) {
            setLeft(newWidget);
            assert(right != newWidget);
            assert(top != newWidget);
            assert(bottom != newWidget);
            return;
        }
        if (right == oldWidget) {
            setRight(newWidget);
            assert(left != newWidget);
            assert(top != newWidget);
            assert(bottom != newWidget);
            return;
        }
        if (top == oldWidget) {
            setTop(newWidget);
            assert(left != newWidget);
            assert(right != newWidget);
            assert(bottom != newWidget);
            return;
        }
        if (bottom == oldWidget) {
            setBottom(newWidget);
            assert(left != newWidget);
            assert(right != newWidget);
            assert(top != newWidget);
            return;
        }
        throw new IllegalArgumentException("oldWidget " + oldWidget +
            " not in this split");
    }

    /**
     * Layout the two child widgets.
     */
    private void layoutChildren() {
        if (vertical) {
            if (left != null) {
                left.setDimensions(0, 0, split, getHeight());
                left.onResize(new TResizeEvent(null, TResizeEvent.Type.WIDGET,
                        left.getWidth(), left.getHeight()));
            }
            if (right != null) {
                right.setDimensions(split + 1, 0, getWidth() - split - 1,
                    getHeight());
                right.onResize(new TResizeEvent(null, TResizeEvent.Type.WIDGET,
                        right.getWidth(), right.getHeight()));
            }
        } else {
            if (top != null) {
                top.setDimensions(0, 0, getWidth(), split);
                top.onResize(new TResizeEvent(null, TResizeEvent.Type.WIDGET,
                        top.getWidth(), top.getHeight()));
            }
            if (bottom != null) {
                bottom.setDimensions(0, split + 1, getWidth(),
                    getHeight() - split - 1);
                bottom.onResize(new TResizeEvent(null, TResizeEvent.Type.WIDGET,
                        bottom.getWidth(), bottom.getHeight()));
            }
        }
    }

    /**
     * Get whether or not this is a vertical split.
     *
     * @return if true, this is a vertical split
     */
    public boolean isVertical() {
        return vertical;
    }

    /**
     * Get whether or not this is a horizontal split.
     *
     * @return if true, this is a horizontal split
     */
    public boolean isHorizontal() {
        return !vertical;
    }

    /**
     * Get the split location.
     *
     * @return the row of the divider for a horizontal, or the column for a
     * vertical split
     */
    public int getSplit() {
        return split;
    }

    /**
     * Set the split location.
     *
     * @param split the row of the divider for a horizontal, or the column
     * for a vertical split
     */
    public void setSplit(final int split) {
        this.split = split;
        layoutChildren();
    }

    /**
     * Recenter the split to the middle of this split pane.
     */
    public void center() {
        if (vertical) {
            split = getWidth() / 2;
        } else {
            split = getHeight() / 2;
        }
        layoutChildren();
    }

    /**
     * Remove this split, removing the widget specified.
     *
     * @param widgetToRemove the widget to remove
     * @param doClose if true, call the close() method before removing the
     * child
     * @return the pane that remains, or null if nothing is retained
     */
    public TWidget removeSplit(final TWidget widgetToRemove,
        final boolean doClose) {

        TWidget keep = null;
        if (vertical) {
            if ((widgetToRemove != left) && (widgetToRemove != right)) {
                throw new IllegalArgumentException("widget to remove is not " +
                    "either of the panes in this splitpane");
            }
            if (widgetToRemove == left) {
                keep = right;
            } else {
                keep = left;
            }

        } else {
            if ((widgetToRemove != top) && (widgetToRemove != bottom)) {
                throw new IllegalArgumentException("widget to remove is not " +
                    "either of the panes in this splitpane");
            }
            if (widgetToRemove == top) {
                keep = bottom;
            } else {
                keep = top;
            }
        }

        // Remove me from my parent widget.
        TWidget myParent = getParent();
        remove(false);

        if (keep == null) {
            if (myParent instanceof TSplitPane) {
                // TSplitPane has a left/right/top/bottom link to me
                // somewhere, remove it.
                ((TSplitPane) myParent).removeWidget(this);
            }

            // Nothing is left of either pane.  Remove me and bail out.
            return null;
        }

        if (myParent instanceof TSplitPane) {
            // TSplitPane has a left/right/top/bottom link to me
            // somewhere, replace me with keep.
            ((TSplitPane) myParent).replaceWidget(this, keep);
        } else {
            keep.setParent(myParent, false);
            keep.setDimensions(getX(), getY(), getWidth(), getHeight());
            keep.onResize(new TResizeEvent(null, TResizeEvent.Type.WIDGET,
                    getWidth(), getHeight()));
        }

        return keep;
    }

}
