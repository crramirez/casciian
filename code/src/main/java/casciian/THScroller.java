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
 * THScroller implements a simple horizontal scroll bar.
 */
public class THScroller extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Value that corresponds to being on the left edge of the scroll bar.
     */
    private int leftValue = 0;

    /**
     * Value that corresponds to being on the right edge of the scroll bar.
     */
    private int rightValue = 100;

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
     * @param width height of scroll bar
     */
    public THScroller(final TWidget parent, final int x, final int y,
        final int width) {

        // Set parent and window
        super(parent, x, y, width, 1);
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

        if (inScroll) {
            inScroll = false;
            return;
        }

        if (rightValue == leftValue) {
            return;
        }

        if ((mouse.getX() == 0)
            && (mouse.getY() == 0)
        ) {
            // Clicked on the left arrow
            decrement();
            return;
        }

        if ((mouse.getY() == 0)
            && (mouse.getX() == getWidth() - 1)
        ) {
            // Clicked on the right arrow
            increment();
            return;
        }

        if ((mouse.getY() == 0)
            && (mouse.getX() > 0)
            && (mouse.getX() < boxPosition())
        ) {
            // Clicked between the left arrow and the box
            value -= bigChange;
            if (value < leftValue) {
                value = leftValue;
            }
            return;
        }

        if ((mouse.getY() == 0)
            && (mouse.getX() > boxPosition())
            && (mouse.getX() < getWidth() - 1)
        ) {
            // Clicked between the box and the right arrow
            value += bigChange;
            if (value > rightValue) {
                value = rightValue;
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

        if (rightValue == leftValue) {
            inScroll = false;
            return;
        }

        if ((mouse.isMouse1())
            && (inScroll)
            && (mouse.getX() > 0)
            && (mouse.getX() < getWidth() - 1)
        ) {
            // Recompute value based on new box position
            value = (rightValue - leftValue)
                * (mouse.getX()) / (getWidth() - 3) + leftValue;
            if (value > rightValue) {
                value = rightValue;
            }
            if (value < leftValue) {
                value = leftValue;
            }
            return;
        }
        inScroll = false;
    }

    /**
     * Handle mouse button press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if (rightValue == leftValue) {
            inScroll = false;
            return;
        }

        if ((mouse.getY() == 0)
            && (mouse.getX() == boxPosition())
        ) {
            inScroll = true;
            return;
        }

    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw a horizontal scroll bar.
     */
    @Override
    public void draw() {
        CellAttributes arrowColor = getTheme().getColor("tscroller.arrows");
        CellAttributes barColor = getTheme().getColor("tscroller.bar");
        putCharXY(0, 0, GraphicsChars.CP437[0x11], arrowColor);
        putCharXY(getWidth() - 1, 0, GraphicsChars.CP437[0x10], arrowColor);

        // Place the box
        if (rightValue > leftValue) {
            hLineXY(1, 0, getWidth() - 2, GraphicsChars.CP437[0xB1], barColor);
            putCharXY(boxPosition(), 0, GraphicsChars.BOX, arrowColor);
        } else {
            hLineXY(1, 0, getWidth() - 2, GraphicsChars.HATCH, barColor);
        }

    }

    // ------------------------------------------------------------------------
    // THScroller -------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the value that corresponds to being on the left edge of the scroll
     * bar.
     *
     * @return the scroll value
     */
    public int getLeftValue() {
        return leftValue;
    }

    /**
     * Set the value that corresponds to being on the left edge of the
     * scroll bar.
     *
     * @param leftValue the new scroll value
     */
    public void setLeftValue(final int leftValue) {
        this.leftValue = leftValue;
    }

    /**
     * Get the value that corresponds to being on the right edge of the
     * scroll bar.
     *
     * @return the scroll value
     */
    public int getRightValue() {
        return rightValue;
    }

    /**
     * Set the value that corresponds to being on the right edge of the
     * scroll bar.
     *
     * @param rightValue the new scroll value
     */
    public void setRightValue(final int rightValue) {
        this.rightValue = rightValue;
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
     * @return Y position of the box, between 1 and width - 2
     */
    private int boxPosition() {
        return (getWidth() - 3) * (value - leftValue) / (rightValue - leftValue) + 1;
    }

    /**
     * Perform a small step change left.
     */
    public void decrement() {
        if (leftValue == rightValue) {
            return;
        }
        value -= smallChange;
        if (value < leftValue) {
            value = leftValue;
        }
    }

    /**
     * Perform a small step change right.
     */
    public void increment() {
        if (leftValue == rightValue) {
            return;
        }
        value += smallChange;
        if (value > rightValue) {
            value = rightValue;
        }
    }

    /**
     * Perform a big step change left.
     */
    public void bigDecrement() {
        if (leftValue == rightValue) {
            return;
        }
        value -= bigChange;
        if (value < leftValue) {
            value = leftValue;
        }
    }

    /**
     * Perform a big step change right.
     */
    public void bigIncrement() {
        if (rightValue == leftValue) {
            return;
        }
        value += bigChange;
        if (value > rightValue) {
            value = rightValue;
        }
    }

    /**
     * Go to the left edge of the scroller.
     */
    public void toLeft() {
        value = leftValue;
    }

    /**
     * Go to the right edge of the scroller.
     */
    public void toRight() {
        value = rightValue;
    }

}
