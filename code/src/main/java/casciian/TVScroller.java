/*
 * Casciian - Java Text User Interface
 *
 * Original work written 2013–2025 by Autumn Lamonte
 * and dedicated to the public domain via CC0.
 *
 * Modifications and maintenance:
 * Copyright 2025 Carlos Rafael Ramirez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
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
        // Handle an in-progress thumb drag before the equal-range early
        // return: if the range collapsed while the thumb was held, we still
        // must clear inScroll and release the capture, otherwise this
        // scrollbar stays captured and swallows later events.
        if (inScroll) {
            inScroll = false;
            releaseMouseCapture();
            return;
        }

        if (bottomValue == topValue) {
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
        if (inScroll && (bottomValue == topValue)) {
            inScroll = false;
            releaseMouseCapture();
            return;
        }

        if (bottomValue == topValue) {
            return;
        }

        if ((mouse.isMouse1())
            && (inScroll)
        ) {
            // Dragging the scroll box.  This scrollbar owns the mouse
            // capture, so the pointer may be anywhere - including outside the
            // scrollbar's bounds.  Clamp the box position to the track so the
            // drag keeps working when the pointer leaves the scrollbar.
            int boxY = mouse.getY();
            if (boxY < 1) {
                boxY = 1;
            }
            if (boxY > getHeight() - 2) {
                boxY = getHeight() - 2;
            }
            // Recompute value based on new box position
            value = (bottomValue - topValue)
                * (boxY) / (getHeight() - 3) + topValue;
            if (value > bottomValue) {
                value = bottomValue;
            }
            if (value < topValue) {
                value = topValue;
            }
            TWidget parent = getParent();
            if (parent != null) {
                parent.onScrollerChange();
            }
            return;
        }
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

        if ((mouse.isMouse1())
            && (mouse.getX() == 0)
            && (mouse.getY() == boxPosition())
        ) {
            inScroll = true;
            captureMouse();
            return;
        }
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Stop an in-progress thumb drag when the mouse capture is taken away (for
     * example when this scrollbar is disabled mid-drag).
     */
    @Override
    protected void onCaptureLost() {
        inScroll = false;
    }

    /**
     * Draw a vertical scroll bar.
     */
    @Override
    public void draw() {
        CellAttributes arrowColor = getWidgetColor("tscroller.arrows");
        CellAttributes barColor = getWidgetColor("tscroller.bar");
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
