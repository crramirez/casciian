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

import casciian.backend.Backend;
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

    /**
     * Regions of the scroll bar that respond to a mouse press.
     */
    private enum Region {
        NONE,
        ARROW_UP,
        ARROW_DOWN,
        PAGE_UP,
        PAGE_DOWN,
        BOX,
    }

    /**
     * The region the mouse was pressed on, used to stop auto-repeat once the
     * mouse moves off it.
     */
    private Region pressedRegion = Region.NONE;

    /**
     * The row the mouse was pressed on, used to stop a page scroll once the
     * box reaches the mouse.
     */
    private int pressedY = 0;

    /**
     * The backend of the press being repeated, reused for the synthesized
     * repeat events.
     */
    private Backend pressedBackend = null;

    /**
     * Repeats the press action while the mouse button is held down.
     */
    private final MouseAutoRepeat autoRepeat = new MouseAutoRepeat();

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
        autoRepeat.stop();
        pressedRegion = Region.NONE;
        inScroll = false;
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

        // Motion is broadcast to every widget, so this also fires once the
        // mouse has left the scroll bar entirely.  That is what stops the
        // repeat when the button is released somewhere else, since onMouseUp
        // only reaches widgets the mouse is still over.
        if (!mouse.isMouse1()
            || (regionAt(mouse.getX(), mouse.getY()) != pressedRegion)
        ) {
            autoRepeat.stop();
            pressedRegion = Region.NONE;
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
        if (!mouse.isMouse1()) {
            return;
        }

        if (mouse.isAutoRepeat()) {
            // Re-dispatched by our own repeat timer; the pressed region is
            // already known and the timer is already running.
            performStep();
            return;
        }

        pressedRegion = regionAt(mouse.getX(), mouse.getY());
        pressedY = mouse.getY();
        pressedBackend = mouse.getBackend();

        if (pressedRegion == Region.BOX) {
            inScroll = true;
            return;
        }
        if (pressedRegion == Region.NONE) {
            return;
        }

        autoRepeat.start(this, new TAction() {
            @Override
            public void DO() {
                repeatStep();
            }
        });
    }

    /**
     * Release the repeat timer when this widget goes away.
     */
    @Override
    public void close() {
        autoRepeat.stop();
        super.close();
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

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
     * Determine which region of the scroll bar a point is on.
     *
     * @param x column relative to this widget
     * @param y row relative to this widget
     * @return the region, or NONE if the point is off the bar
     */
    private Region regionAt(final int x, final int y) {
        if ((x != 0) || (bottomValue == topValue)) {
            return Region.NONE;
        }
        if (y == 0) {
            return Region.ARROW_UP;
        }
        if (y == getHeight() - 1) {
            return Region.ARROW_DOWN;
        }
        if ((y < 0) || (y > getHeight() - 1)) {
            return Region.NONE;
        }
        int box = boxPosition();
        if (y == box) {
            return Region.BOX;
        }
        return (y < box ? Region.PAGE_UP : Region.PAGE_DOWN);
    }

    /**
     * Apply one scroll step for the region the mouse was pressed on.
     */
    private void performStep() {
        switch (pressedRegion) {
            case ARROW_UP:
                stepBy(-smallChange);
                break;
            case ARROW_DOWN:
                stepBy(smallChange);
                break;
            case PAGE_UP:
                pageBy(-bigChange);
                break;
            case PAGE_DOWN:
                pageBy(bigChange);
                break;
            default:
                break;
        }
    }

    /**
     * Perform one repeat by re-dispatching the press through the parent,
     * rather than scrolling directly.
     *
     * <p>Containers such as TTreeViewScrollable and TText copy the scroll bar
     * value into their view and reflow only inside their own mouse handlers.
     * Going back through the parent runs those handlers, so the content
     * scrolls with the scroll bar instead of lagging until the next real
     * mouse event.</p>
     */
    private void repeatStep() {
        TWidget parent = getParent();
        if (parent == null) {
            performStep();
            return;
        }

        int absoluteX = getAbsoluteX();
        int absoluteY = getAbsoluteY() + pressedY;
        TMouseEvent event = new TMouseEvent(pressedBackend,
            TMouseEvent.Type.MOUSE_DOWN, absoluteX, absoluteY,
            absoluteX, absoluteY, 0, 0,
            true, false, false, false, false, false, false, false);
        event.setAutoRepeat(true);
        parent.onMouseDown(event);
    }

    /**
     * Change value by delta, stopping the auto-repeat once it can go no
     * further.
     *
     * @param delta amount to add to value
     */
    private void stepBy(final int delta) {
        if (bottomValue == topValue) {
            autoRepeat.stop();
            return;
        }
        int oldValue = value;
        value = Math.clamp((long) value + delta, topValue, bottomValue);
        if (value == oldValue) {
            // Already against the end, nothing left to repeat.
            autoRepeat.stop();
        }
    }

    /**
     * Change value by delta, stopping the auto-repeat once it can go no
     * further or the box has reached the mouse.
     *
     * @param delta amount to add to value
     */
    private void pageBy(final int delta) {
        stepBy(delta);
        if (bottomValue == topValue) {
            return;
        }
        // Stop before the box jumps past the mouse, the way Swing does.
        int box = boxPosition();
        if ((delta < 0 && box <= pressedY) || (delta > 0 && box >= pressedY)) {
            autoRepeat.stop();
        }
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
