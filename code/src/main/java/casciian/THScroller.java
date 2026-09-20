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

    /**
     * Regions of the scroll bar that respond to a mouse press.
     */
    private enum Region {
        NONE,
        ARROW_LEFT,
        ARROW_RIGHT,
        PAGE_LEFT,
        PAGE_RIGHT,
        BOX,
    }

    /**
     * The region the mouse was pressed on, used to stop auto-repeat once the
     * mouse moves off it.
     */
    private Region pressedRegion = Region.NONE;

    /**
     * The column the mouse was pressed on, used to stop a page scroll once
     * the box reaches the mouse.
     */
    private int pressedX = 0;

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
        autoRepeat.stop();
        pressedRegion = Region.NONE;
        // Handle an in-progress thumb drag before the equal-range early
        // return: if the range collapsed while the thumb was held, we still
        // must clear inScroll and release the capture, otherwise this
        // scrollbar stays captured and swallows later events.
        if (inScroll) {
            // Only a left-button release ends the thumb drag.  A non-left
            // release (mouse1 == false) is routed here while the left button
            // is still held; consume it without dropping the capture.
            if (mouse.isMouse1()) {
                inScroll = false;
                releaseMouseCapture();
            }
        }
    }

    /**
     * Handle mouse movement events.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        if (inScroll && (rightValue == leftValue)) {
            inScroll = false;
            releaseMouseCapture();
            return;
        }

        if (rightValue == leftValue) {
            return;
        }

        if (mouse.isMouse1()
            && inScroll && pressedRegion == Region.BOX
        ) {
            // Dragging the scroll box.  This scrollbar owns the mouse
            // capture, so the pointer may be anywhere - including outside the
            // scrollbar's bounds.  Clamp the box position to the track so the
            // drag keeps working when the pointer leaves the scrollbar.
            int boxX = mouse.getX();
            if (boxX < 1) {
                boxX = 1;
            }
            if (boxX > getWidth() - 2) {
                boxX = getWidth() - 2;
            }
            // Recompute value based on new box position
            value = (rightValue - leftValue)
                * (boxX) / (getWidth() - 3) + leftValue;
            if (value > rightValue) {
                value = rightValue;
            }
            if (value < leftValue) {
                value = leftValue;
            }
            TWidget parent = getParent();
            if (parent != null) {
                parent.onScrollerChange();
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
    }

    /**
     * Handle mouse button press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if (rightValue == leftValue) {
            // If the range collapsed while the thumb was held, release the
            // capture here too; otherwise this scrollbar stays captured and
            // swallows later events.  Only a left-button event ends the drag.
            if (inScroll && mouse.isMouse1()) {
                inScroll = false;
                releaseMouseCapture();
            }
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
        pressedX = mouse.getX();
        pressedBackend = mouse.getBackend();

        if (pressedRegion == Region.NONE) {
            return;
        }

        if (pressedRegion != Region.BOX) {
        autoRepeat.start(this, new TAction() {
            @Override
            public void DO() {
                repeatStep();
            }
        });
    }

        inScroll = true;
        captureMouse();
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
     * Stop an in-progress thumb drag when the mouse capture is taken away (for
     * example when this scrollbar is disabled mid-drag).
     */
    @Override
    protected void onCaptureLost() {
        inScroll = false;
    }

    /**
     * Draw a horizontal scroll bar.
     */
    @Override
    public void draw() {
        CellAttributes arrowColor = getWidgetColor("tscroller.arrows");
        CellAttributes barColor = getWidgetColor("tscroller.bar");
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
     * Determine which region of the scroll bar a point is on.
     *
     * @param x column relative to this widget
     * @param y row relative to this widget
     * @return the region, or NONE if the point is off the bar
     */
    private Region regionAt(final int x, final int y) {
        if ((y != 0) || (rightValue == leftValue)) {
            return Region.NONE;
        }
        if (x == 0) {
            return Region.ARROW_LEFT;
        }
        if (x == getWidth() - 1) {
            return Region.ARROW_RIGHT;
        }
        if ((x < 0) || (x > getWidth() - 1)) {
            return Region.NONE;
        }
        int box = boxPosition();
        if (x == box) {
            return Region.BOX;
        }
        return (x < box ? Region.PAGE_LEFT : Region.PAGE_RIGHT);
    }

    /**
     * Apply one scroll step for the region the mouse was pressed on.
     */
    private void performStep() {
        switch (pressedRegion) {
            case ARROW_LEFT:
                stepBy(-smallChange);
                break;
            case ARROW_RIGHT:
                stepBy(smallChange);
                break;
            case PAGE_LEFT:
                pageBy(-bigChange);
                break;
            case PAGE_RIGHT:
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

        int absoluteX = getAbsoluteX() + pressedX;
        int absoluteY = getAbsoluteY();
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
        if (rightValue == leftValue) {
            autoRepeat.stop();
            return;
        }
        int oldValue = value;
        value = Math.clamp((long) value + delta, leftValue, rightValue);
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
        if (rightValue == leftValue) {
            return;
        }
        // Stop before the box jumps past the mouse, the way Swing does.
        int box = boxPosition();
        if ((delta < 0 && box <= pressedX) || (delta > 0 && box >= pressedX)) {
            autoRepeat.stop();
        }
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
