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

import casciian.event.TResizeEvent;

/**
 * TScrollable is a convenience superclass for widgets that have scrollbars.
 */
public class TScrollable extends TWidget implements Scrollable {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The horizontal scrollbar.
     */
    protected THScroller hScroller = null;

    /**
     * The vertical scrollbar.
     */
    protected TVScroller vScroller = null;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Protected constructor.
     *
     * @param parent parent widget
     */
    protected TScrollable(final TWidget parent) {
        super(parent);
    }

    /**
     * Protected constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of widget
     * @param height height of widget
     */
    protected TScrollable(final TWidget parent, final int x, final int y,
        final int width, final int height) {

        super(parent, x, y, width, height);
    }

    /**
     * Protected constructor used by subclasses that are disabled by default.
     *
     * @param parent parent widget
     * @param enabled if true assume enabled
     */
    protected TScrollable(final TWidget parent, final boolean enabled) {

        super(parent, enabled);
    }

    /**
     * Protected constructor used by subclasses that are disabled by default.
     *
     * @param parent parent widget
     * @param enabled if true assume enabled
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of widget
     * @param height height of widget
     */
    protected TScrollable(final TWidget parent, final boolean enabled,
        final int x, final int y, final int width, final int height) {

        super(parent, enabled, x, y, width, height);
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle window/screen resize events.
     *
     * @param event resize event
     */
    @Override
    public void onResize(final TResizeEvent event) {
        if (event.getType() == TResizeEvent.Type.WIDGET) {
            setWidth(event.getWidth());
            setHeight(event.getHeight());

            reflowData();
            placeScrollbars();
            return;
        } else {
            super.onResize(event);
        }
    }

    // ------------------------------------------------------------------------
    // TScrollable ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Place the scrollbars on the edge of this widget, and adjust bigChange
     * to match the new size.  This is called by onResize().
     */
    protected void placeScrollbars() {
        if (hScroller != null) {
            hScroller.setY(getHeight() - 1);
            hScroller.setWidth(getWidth() - 1);
            hScroller.setBigChange(getWidth() - 1);
        }
        if (vScroller != null) {
            vScroller.setX(getWidth() - 1);
            vScroller.setHeight(getHeight() - 1);
            vScroller.setBigChange(getHeight() - 1);
        }
    }

    /**
     * Recompute whatever data is displayed by this widget.
     */
    public void reflowData() {
        // Default: nothing to do
    }

    /**
     * Get the horizontal scrollbar, or null if this Viewport does not
     * support horizontal scrolling.
     *
     * @return the horizontal scrollbar
     */
    public THScroller getHorizontalScroller() {
        return hScroller;
    }

    /**
     * Get the vertical scrollbar, or null if this Viewport does not support
     * vertical scrolling.
     *
     * @return the vertical scrollbar
     */
    public TVScroller getVerticalScroller() {
        return vScroller;
    }

    /**
     * Get the value that corresponds to being on the top edge of the
     * vertical scroll bar.
     *
     * @return the scroll value
     */
    public int getTopValue() {
        if (vScroller == null) {
            return 0;
        } else {
            return vScroller.getTopValue();
        }
    }

    /**
     * Set the value that corresponds to being on the top edge of the
     * vertical scroll bar.
     *
     * @param topValue the new scroll value
     */
    public void setTopValue(final int topValue) {
        if (vScroller == null) {
            return;
        } else {
            vScroller.setTopValue(topValue);
        }
    }

    /**
     * Get the value that corresponds to being on the bottom edge of the
     * vertical scroll bar.
     *
     * @return the scroll value
     */
    public int getBottomValue() {
        if (vScroller == null) {
            return 0;
        } else {
            return vScroller.getBottomValue();
        }
    }

    /**
     * Set the value that corresponds to being on the bottom edge of the
     * vertical scroll bar.
     *
     * @param bottomValue the new scroll value
     */
    public void setBottomValue(final int bottomValue) {
        if (vScroller == null) {
            return;
        } else {
            vScroller.setBottomValue(bottomValue);
        }
    }

    /**
     * Get current value of the vertical scroll.
     *
     * @return the scroll value
     */
    public int getVerticalValue() {
        if (vScroller == null) {
            return 0;
        } else {
            return vScroller.getValue();
        }
    }

    /**
     * Set current value of the vertical scroll.
     *
     * @param value the new scroll value
     */
    public void setVerticalValue(final int value) {
        if (vScroller == null) {
            return;
        } else {
            vScroller.setValue(value);
        }
    }

    /**
     * Get the increment for clicking on an arrow on the vertical scrollbar.
     *
     * @return the increment value
     */
    public int getVerticalSmallChange() {
        if (vScroller == null) {
            return 0;
        } else {
            return vScroller.getSmallChange();
        }
    }

    /**
     * Set the increment for clicking on an arrow on the vertical scrollbar.
     *
     * @param smallChange the new increment value
     */
    public void setVerticalSmallChange(final int smallChange) {
        if (vScroller == null) {
            return;
        } else {
            vScroller.setSmallChange(smallChange);
        }
    }

    /**
     * Get the increment for clicking in the bar between the box and an
     * arrow on the vertical scrollbar.
     *
     * @return the increment value
     */
    public int getVerticalBigChange() {
        if (vScroller == null) {
            return 0;
        } else {
            return vScroller.getBigChange();
        }
    }

    /**
     * Set the increment for clicking in the bar between the box and an
     * arrow on the vertical scrollbar.
     *
     * @param bigChange the new increment value
     */
    public void setVerticalBigChange(final int bigChange) {
        if (vScroller == null) {
            return;
        } else {
            vScroller.setBigChange(bigChange);
        }
    }

    /**
     * Perform a small step change up.
     */
    public void verticalDecrement() {
        if (vScroller == null) {
            return;
        } else {
            vScroller.decrement();
        }
    }

    /**
     * Perform a small step change down.
     */
    public void verticalIncrement() {
        if (vScroller == null) {
            return;
        } else {
            vScroller.increment();
        }
    }

    /**
     * Perform a big step change up.
     */
    public void bigVerticalDecrement() {
        if (vScroller == null) {
            return;
        } else {
            vScroller.bigDecrement();
        }
    }

    /**
     * Perform a big step change down.
     */
    public void bigVerticalIncrement() {
        if (vScroller == null) {
            return;
        } else {
            vScroller.bigIncrement();
        }
    }

    /**
     * Go to the top edge of the vertical scroller.
     */
    public void toTop() {
        if (vScroller == null) {
            return;
        } else {
            vScroller.toTop();
        }
    }

    /**
     * Go to the bottom edge of the vertical scroller.
     */
    public void toBottom() {
        if (vScroller == null) {
            return;
        } else {
            vScroller.toBottom();
        }
    }

    /**
     * Get the value that corresponds to being on the left edge of the
     * horizontal scroll bar.
     *
     * @return the scroll value
     */
    public int getLeftValue() {
        if (hScroller == null) {
            return 0;
        } else {
            return hScroller.getLeftValue();
        }
    }

    /**
     * Set the value that corresponds to being on the left edge of the
     * horizontal scroll bar.
     *
     * @param leftValue the new scroll value
     */
    public void setLeftValue(final int leftValue) {
        if (hScroller == null) {
            return;
        } else {
            hScroller.setLeftValue(leftValue);
        }
    }

    /**
     * Get the value that corresponds to being on the right edge of the
     * horizontal scroll bar.
     *
     * @return the scroll value
     */
    public int getRightValue() {
        if (hScroller == null) {
            return 0;
        } else {
            return hScroller.getRightValue();
        }
    }

    /**
     * Set the value that corresponds to being on the right edge of the
     * horizontal scroll bar.
     *
     * @param rightValue the new scroll value
     */
    public void setRightValue(final int rightValue) {
        if (hScroller == null) {
            return;
        } else {
            hScroller.setRightValue(rightValue);
        }
    }

    /**
     * Get current value of the horizontal scroll.
     *
     * @return the scroll value
     */
    public int getHorizontalValue() {
        if (hScroller == null) {
            return 0;
        } else {
            return hScroller.getValue();
        }
    }

    /**
     * Set current value of the horizontal scroll.
     *
     * @param value the new scroll value
     */
    public void setHorizontalValue(final int value) {
        if (hScroller == null) {
            return;
        } else {
            hScroller.setValue(value);
        }
    }

    /**
     * Get the increment for clicking on an arrow on the horizontal
     * scrollbar.
     *
     * @return the increment value
     */
    public int getHorizontalSmallChange() {
        if (hScroller == null) {
            return 0;
        } else {
            return hScroller.getSmallChange();
        }
    }

    /**
     * Set the increment for clicking on an arrow on the horizontal
     * scrollbar.
     *
     * @param smallChange the new increment value
     */
    public void setHorizontalSmallChange(final int smallChange) {
        if (hScroller == null) {
            return;
        } else {
            hScroller.setSmallChange(smallChange);
        }
    }

    /**
     * Get the increment for clicking in the bar between the box and an
     * arrow on the horizontal scrollbar.
     *
     * @return the increment value
     */
    public int getHorizontalBigChange() {
        if (hScroller == null) {
            return 0;
        } else {
            return hScroller.getBigChange();
        }
    }

    /**
     * Set the increment for clicking in the bar between the box and an
     * arrow on the horizontal scrollbar.
     *
     * @param bigChange the new increment value
     */
    public void setHorizontalBigChange(final int bigChange) {
        if (hScroller == null) {
            return;
        } else {
            hScroller.setBigChange(bigChange);
        }
    }

    /**
     * Perform a small step change left.
     */
    public void horizontalDecrement() {
        if (hScroller == null) {
            return;
        } else {
            hScroller.decrement();
        }
    }

    /**
     * Perform a small step change right.
     */
    public void horizontalIncrement() {
        if (hScroller == null) {
            return;
        } else {
            hScroller.increment();
        }
    }

    /**
     * Perform a big step change left.
     */
    public void bigHorizontalDecrement() {
        if (hScroller == null) {
            return;
        } else {
            hScroller.bigDecrement();
        }
    }

    /**
     * Perform a big step change right.
     */
    public void bigHorizontalIncrement() {
        if (hScroller == null) {
            return;
        } else {
            hScroller.bigIncrement();
        }
    }

    /**
     * Go to the left edge of the horizontal scroller.
     */
    public void toLeft() {
        if (hScroller == null) {
            return;
        } else {
            hScroller.toLeft();
        }
    }

    /**
     * Go to the right edge of the horizontal scroller.
     */
    public void toRight() {
        if (hScroller == null) {
            return;
        } else {
            hScroller.toRight();
        }
    }

    /**
     * Go to the top-left edge of the horizontal and vertical scrollers.
     */
    public void toHome() {
        if (hScroller != null) {
            hScroller.toLeft();
        }
        if (vScroller != null) {
            vScroller.toTop();
        }
    }

    /**
     * Go to the bottom-right edge of the horizontal and vertical scrollers.
     */
    public void toEnd() {
        if (hScroller != null) {
            hScroller.toRight();
        }
        if (vScroller != null) {
            vScroller.toBottom();
        }
    }

}
