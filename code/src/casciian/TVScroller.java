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

/**
 * TVScroller implements a simple vertical scroll bar.
 */
public class TVScroller extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Value that corresponds to being on the top edge of the scroll bar.
     */
    private int topValue = 0;

    /**
     * Value that corresponds to being on the bottom edge of the scroll bar.
     */
    private int bottomValue = 100;

    /**
     * Current value of the scroll.
     */
    private int value = 0;

    /**
     * The increment for clicking on an arrow.
     */
    private int smallChange = 1;

    /**
     * The increment for clicking in the bar between the box and an arrow.
     */
    private int bigChange = 20;

    /**
     * When true, the user is dragging the scroll box.
     */
    private boolean inScroll = false;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param height height of scroll bar
     */
    public TVScroller(final TWidget parent, final int x, final int y,
        final int height) {

        // Set parent and window
        super(parent, x, y, 1, height);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle mouse button releases.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        if (bottomValue == topValue) {
            return;
        }

        if (inScroll) {
            inScroll = false;
            return;
        }

        if ((mouse.getX() == 0)
            && (mouse.getY() == 0)
        ) {
            // Clicked on the top arrow
            decrement();
            return;
        }

        if ((mouse.getX() == 0)
            && (mouse.getY() == getHeight() - 1)
        ) {
            // Clicked on the bottom arrow
            increment();
            return;
        }

        if ((mouse.getX() == 0)
            && (mouse.getY() > 0)
            && (mouse.getY() < boxPosition())
        ) {
            // Clicked between the top arrow and the box
            value -= bigChange;
            if (value < topValue) {
                value = topValue;
            }
            return;
        }

        if ((mouse.getX() == 0)
            && (mouse.getY() > boxPosition())
            && (mouse.getY() < getHeight() - 1)
        ) {
            // Clicked between the box and the bottom arrow
            value += bigChange;
            if (value > bottomValue) {
                value = bottomValue;
            }
            return;
        }
    }

    /**
     * Handle mouse movement events.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        if (bottomValue == topValue) {
            return;
        }

        if ((mouse.isMouse1())
            && (inScroll)
            && (mouse.getY() > 0)
            && (mouse.getY() < getHeight() - 1)
        ) {
            // Recompute value based on new box position
            value = (bottomValue - topValue)
                * (mouse.getY()) / (getHeight() - 3) + topValue;
            if (value > bottomValue) {
                value = bottomValue;
            }
            if (value < topValue) {
                value = topValue;
            }
            return;
        }

        inScroll = false;
    }

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if (bottomValue == topValue) {
            return;
        }

        if ((mouse.getX() == 0)
            && (mouse.getY() == boxPosition())
        ) {
            inScroll = true;
            return;
        }
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw a vertical scroll bar.
     */
    @Override
    public void draw() {
        CellAttributes arrowColor = getTheme().getColor("tscroller.arrows");
        CellAttributes barColor = getTheme().getColor("tscroller.bar");
        putCharXY(0, 0, GraphicsChars.CP437[0x1E], arrowColor);
        putCharXY(0, getHeight() - 1, GraphicsChars.CP437[0x1F], arrowColor);

        // Place the box
        if (bottomValue > topValue) {
            vLineXY(0, 1, getHeight() - 2, GraphicsChars.CP437[0xB1], barColor);
            putCharXY(0, boxPosition(), GraphicsChars.BOX, arrowColor);
        } else {
            vLineXY(0, 1, getHeight() - 2, GraphicsChars.HATCH, barColor);
        }
    }

    // ------------------------------------------------------------------------
    // TVScroller -------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the value that corresponds to being on the top edge of the scroll
     * bar.
     *
     * @return the scroll value
     */
    public int getTopValue() {
        return topValue;
    }

    /**
     * Set the value that corresponds to being on the top edge of the scroll
     * bar.
     *
     * @param topValue the new scroll value
     */
    public void setTopValue(final int topValue) {
        this.topValue = topValue;
    }

    /**
     * Get the value that corresponds to being on the bottom edge of the
     * scroll bar.
     *
     * @return the scroll value
     */
    public int getBottomValue() {
        return bottomValue;
    }

    /**
     * Set the value that corresponds to being on the bottom edge of the
     * scroll bar.
     *
     * @param bottomValue the new scroll value
     */
    public void setBottomValue(final int bottomValue) {
        this.bottomValue = bottomValue;
    }

    /**
     * Get current value of the scroll.
     *
     * @return the scroll value
     */
    public int getValue() {
        return value;
    }

    /**
     * Set current value of the scroll.
     *
     * @param value the new scroll value
     */
    public void setValue(final int value) {
        this.value = value;
    }

    /**
     * Get the increment for clicking on an arrow.
     *
     * @return the increment value
     */
    public int getSmallChange() {
        return smallChange;
    }

    /**
     * Set the increment for clicking on an arrow.
     *
     * @param smallChange the new increment value
     */
    public void setSmallChange(final int smallChange) {
        this.smallChange = smallChange;
    }

    /**
     * Set the increment for clicking in the bar between the box and an
     * arrow.
     *
     * @return the increment value
     */
    public int getBigChange() {
        return bigChange;
    }

    /**
     * Set the increment for clicking in the bar between the box and an
     * arrow.
     *
     * @param bigChange the new increment value
     */
    public void setBigChange(final int bigChange) {
        this.bigChange = bigChange;
    }

    /**
     * Compute the position of the scroll box (a.k.a. grip, thumb).
     *
     * @return Y position of the box, between 1 and height - 2
     */
    private int boxPosition() {
        return (getHeight() - 3) * (value - topValue) / (bottomValue - topValue) + 1;
    }

    /**
     * Perform a small step change up.
     */
    public void decrement() {
        if (bottomValue == topValue) {
            return;
        }
        value -= smallChange;
        if (value < topValue) {
            value = topValue;
        }
    }

    /**
     * Perform a small step change down.
     */
    public void increment() {
        if (bottomValue == topValue) {
            return;
        }
        value += smallChange;
        if (value > bottomValue) {
            value = bottomValue;
        }
    }

    /**
     * Perform a big step change up.
     */
    public void bigDecrement() {
        if (bottomValue == topValue) {
            return;
        }
        value -= bigChange;
        if (value < topValue) {
            value = topValue;
        }
    }

    /**
     * Perform a big step change down.
     */
    public void bigIncrement() {
        if (bottomValue == topValue) {
            return;
        }
        value += bigChange;
        if (value > bottomValue) {
            value = bottomValue;
        }
    }

    /**
     * Go to the top edge of the scroller.
     */
    public void toTop() {
        value = topValue;
    }

    /**
     * Go to the bottom edge of the scroller.
     */
    public void toBottom() {
        value = bottomValue;
    }

}
