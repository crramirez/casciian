/*
 * Casciian - Java Text User Interface
 *
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
 * TScroller is the common base class of the vertical ({@link TVScroller}) and
 * horizontal ({@link THScroller}) scroll bars.  It contains all of the scroll
 * logic; the only difference between the two subclasses is the
 * {@link Orientation} they run along.
 */
public abstract class TScroller extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The axis a scroll bar runs along.
     */
    public enum Orientation {
        /**
         * A vertical scroll bar, one column wide, running top to bottom.
         */
        VERTICAL,

        /**
         * A horizontal scroll bar, one row tall, running left to right.
         */
        HORIZONTAL,
    }

    /**
     * The axis this scroll bar runs along.
     */
    private final Orientation orientation;

    /**
     * Value that corresponds to being on the low (top/left) edge of the
     * scroll bar.
     */
    private int minValue = 0;

    /**
     * Value that corresponds to being on the high (bottom/right) edge of the
     * scroll bar.
     */
    private int maxValue = 100;

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
        ARROW_DECREASE,
        ARROW_INCREASE,
        PAGE_DECREASE,
        PAGE_INCREASE,
        BOX,
    }

    /**
     * The region the mouse was pressed on, used to stop auto-repeat once the
     * mouse moves off it.
     */
    private Region pressedRegion = Region.NONE;

    /**
     * The position (row for vertical, column for horizontal) the mouse was
     * pressed on, used to stop a page scroll once the box reaches the mouse.
     */
    private int pressedPosition = 0;

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
     * Protected constructor.
     *
     * @param orientation the axis this scroll bar runs along
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of scroll bar
     * @param height height of scroll bar
     */
    protected TScroller(final Orientation orientation, final TWidget parent,
        final int x, final int y, final int width, final int height) {

        // Set parent and window
        super(parent, x, y, width, height);
        this.orientation = orientation;
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
        if (!mouse.isMouse1()) {
            return;
        }
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
        if (inScroll && (maxValue == minValue)) {
            inScroll = false;
            releaseMouseCapture();
            return;
        }

        if (maxValue == minValue) {
            return;
        }

        if (mouse.isMouse1()
            && inScroll && pressedRegion == Region.BOX
        ) {
            // Dragging the scroll box.  This scrollbar owns the mouse
            // capture, so the pointer may be anywhere - including outside the
            // scrollbar's bounds.  Clamp the box position to the track so the
            // drag keeps working when the pointer leaves the scrollbar.
            int boxPos = mainCoordinate(mouse);
            if (boxPos < 1) {
                boxPos = 1;
            }
            if (boxPos > getSize() - 2) {
                boxPos = getSize() - 2;
            }
            // Recompute value based on new box position
            value = (maxValue - minValue)
                * (boxPos) / (getSize() - 3) + minValue;
            if (value > maxValue) {
                value = maxValue;
            }
            if (value < minValue) {
                value = minValue;
            }
            TWidget parent = getParent();
            if (parent != null) {
                parent.onScrollerChange();
            }
            return;
        }

        // Motion is broadcast to every widget, so this also fires once the
        // mouse leaves the pressed region or the left button is no longer
        // held.  That is what stops the repeat before the eventual captured
        // button-up event arrives.
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
        if (maxValue == minValue) {
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
        pressedPosition = mainCoordinate(mouse);
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
     * Draw the scroll bar.
     */
    @Override
    public void draw() {
        CellAttributes arrowColor = getWidgetColor("tscroller.arrows");
        CellAttributes barColor = getWidgetColor("tscroller.bar");

        if (orientation == Orientation.VERTICAL) {
            putCharXY(0, 0, GraphicsChars.CP437[0x1E], arrowColor);
            putCharXY(0, getHeight() - 1, GraphicsChars.CP437[0x1F],
                arrowColor);

            // Place the box
            if (maxValue > minValue) {
                vLineXY(0, 1, getHeight() - 2, GraphicsChars.CP437[0xB1],
                    barColor);
                putCharXY(0, boxPosition(), GraphicsChars.BOX, arrowColor);
            } else {
                vLineXY(0, 1, getHeight() - 2, GraphicsChars.HATCH, barColor);
            }
        } else {
            putCharXY(0, 0, GraphicsChars.CP437[0x11], arrowColor);
            putCharXY(getWidth() - 1, 0, GraphicsChars.CP437[0x10],
                arrowColor);

            // Place the box
            if (maxValue > minValue) {
                hLineXY(1, 0, getWidth() - 2, GraphicsChars.CP437[0xB1],
                    barColor);
                putCharXY(boxPosition(), 0, GraphicsChars.BOX, arrowColor);
            } else {
                hLineXY(1, 0, getWidth() - 2, GraphicsChars.HATCH, barColor);
            }
        }
    }

    // ------------------------------------------------------------------------
    // TScroller --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the axis this scroll bar runs along.
     *
     * @return the orientation
     */
    public final Orientation getOrientation() {
        return orientation;
    }

    /**
     * Get the length of the scroll bar along its axis.
     *
     * @return the height for a vertical bar, the width for a horizontal bar
     */
    private int getSize() {
        return (orientation == Orientation.VERTICAL) ? getHeight() : getWidth();
    }

    /**
     * Get the mouse coordinate along the scroll bar's axis.
     *
     * @param mouse the mouse event
     * @return the Y coordinate for a vertical bar, the X for a horizontal bar
     */
    private int mainCoordinate(final TMouseEvent mouse) {
        return (orientation == Orientation.VERTICAL)
            ? mouse.getY() : mouse.getX();
    }

    /**
     * Get the value that corresponds to being on the low (top/left) edge of
     * the scroll bar.
     *
     * @return the scroll value
     */
    public int getMinValue() {
        return minValue;
    }

    /**
     * Set the value that corresponds to being on the low (top/left) edge of
     * the scroll bar.
     *
     * @param minValue the new scroll value
     */
    public void setMinValue(final int minValue) {
        this.minValue = minValue;
    }

    /**
     * Get the value that corresponds to being on the high (bottom/right) edge
     * of the scroll bar.
     *
     * @return the scroll value
     */
    public int getMaxValue() {
        return maxValue;
    }

    /**
     * Set the value that corresponds to being on the high (bottom/right) edge
     * of the scroll bar.
     *
     * @param maxValue the new scroll value
     */
    public void setMaxValue(final int maxValue) {
        this.maxValue = maxValue;
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
        int cross = (orientation == Orientation.VERTICAL) ? x : y;
        int main = (orientation == Orientation.VERTICAL) ? y : x;

        if ((cross != 0) || (maxValue == minValue)) {
            return Region.NONE;
        }
        if (main == 0) {
            return Region.ARROW_DECREASE;
        }
        if (main == getSize() - 1) {
            return Region.ARROW_INCREASE;
        }
        if ((main < 0) || (main > getSize() - 1)) {
            return Region.NONE;
        }
        int box = boxPosition();
        if (main == box) {
            return Region.BOX;
        }
        return (main < box ? Region.PAGE_DECREASE : Region.PAGE_INCREASE);
    }

    /**
     * Apply one scroll step for the region the mouse was pressed on.
     */
    private void performStep() {
        switch (pressedRegion) {
            case ARROW_DECREASE:
                stepBy(-smallChange);
                break;
            case ARROW_INCREASE:
                stepBy(smallChange);
                break;
            case PAGE_DECREASE:
                pageBy(-bigChange);
                break;
            case PAGE_INCREASE:
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

        int absoluteX;
        int absoluteY;
        if (orientation == Orientation.VERTICAL) {
            absoluteX = getAbsoluteX();
            absoluteY = getAbsoluteY() + pressedPosition;
        } else {
            absoluteX = getAbsoluteX() + pressedPosition;
            absoluteY = getAbsoluteY();
        }
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
        if (maxValue == minValue) {
            autoRepeat.stop();
            return;
        }
        int oldValue = value;
        value = Math.clamp((long) value + delta, minValue, maxValue);
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
        if (maxValue == minValue) {
            return;
        }
        // Stop before the box jumps past the mouse, the way Swing does.
        int box = boxPosition();
        if ((delta < 0 && box <= pressedPosition)
            || (delta > 0 && box >= pressedPosition)
        ) {
            autoRepeat.stop();
        }
    }

    /**
     * Compute the position of the scroll box (a.k.a. grip, thumb).
     *
     * @return position of the box, between 1 and size - 2
     */
    private int boxPosition() {
        return (getSize() - 3) * (value - minValue) / (maxValue - minValue) + 1;
    }

    /**
     * Perform a small step change toward the low (top/left) edge.
     */
    public void decrement() {
        if (maxValue == minValue) {
            return;
        }
        value -= smallChange;
        if (value < minValue) {
            value = minValue;
        }
    }

    /**
     * Perform a small step change toward the high (bottom/right) edge.
     */
    public void increment() {
        if (maxValue == minValue) {
            return;
        }
        value += smallChange;
        if (value > maxValue) {
            value = maxValue;
        }
    }

    /**
     * Perform a big step change toward the low (top/left) edge.
     */
    public void bigDecrement() {
        if (maxValue == minValue) {
            return;
        }
        value -= bigChange;
        if (value < minValue) {
            value = minValue;
        }
    }

    /**
     * Perform a big step change toward the high (bottom/right) edge.
     */
    public void bigIncrement() {
        if (maxValue == minValue) {
            return;
        }
        value += bigChange;
        if (value > maxValue) {
            value = maxValue;
        }
    }

    /**
     * Go to the low (top/left) edge of the scroller.
     */
    public void toMin() {
        value = minValue;
    }

    /**
     * Go to the high (bottom/right) edge of the scroller.
     */
    public void toMax() {
        value = maxValue;
    }

}
