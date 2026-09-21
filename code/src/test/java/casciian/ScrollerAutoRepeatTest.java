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

import casciian.event.TMouseEvent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that the scroll bars act on mouse press rather than on release.
 *
 * <p>These scrollers are built with a null parent, so they have no
 * TApplication and therefore no repeat timer.  That isolates the press
 * behaviour: each press performs exactly one step.</p>
 */
class ScrollerAutoRepeatTest {

    /**
     * A scroller 10 rows tall over 0..100 sitting at 50 puts the box on row
     * 4, leaving rows 1-3 as page-up and rows 5-8 as page-down.
     */
    private TVScroller vScroller() {
        TVScroller scroller = new TVScroller(null, 0, 0, 10);
        scroller.setTopValue(0);
        scroller.setBottomValue(100);
        scroller.setValue(50);
        return scroller;
    }

    /**
     * The horizontal twin of {@link #vScroller()}.
     */
    private THScroller hScroller() {
        THScroller scroller = new THScroller(null, 0, 0, 10);
        scroller.setLeftValue(0);
        scroller.setRightValue(100);
        scroller.setValue(50);
        return scroller;
    }

    /**
     * Build a mouse event of the given type at the given widget-relative
     * position.
     */
    private TMouseEvent mouse(final TMouseEvent.Type type, final int x,
        final int y, final boolean mouse1) {

        return new TMouseEvent(null, type, x, y, x, y, 0, 0,
            mouse1, !mouse1, false, false, false, false, false, false);
    }

    private TMouseEvent down(final int x, final int y) {
        return mouse(TMouseEvent.Type.MOUSE_DOWN, x, y, true);
    }

    private TMouseEvent up(final int x, final int y) {
        return mouse(TMouseEvent.Type.MOUSE_UP, x, y, true);
    }

    /**
     * A press synthesized by the repeat timer, as
     * {@code repeatStep()} sends back through the parent.
     */
    @SuppressWarnings("SameParameterValue")
    private TMouseEvent repeat(final int x, final int y) {
        TMouseEvent event = down(x, y);
        event.setAutoRepeat(true);
        return event;
    }

    // ------------------------------------------------------------------------
    // Vertical ---------------------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    void verticalTopArrowScrollsOnPress() {
        TVScroller scroller = vScroller();

        scroller.onMouseDown(down(0, 0));

        assertEquals(49, scroller.getValue(),
            "pressing the top arrow should scroll up immediately");
    }

    @Test
    void verticalBottomArrowScrollsOnPress() {
        TVScroller scroller = vScroller();

        scroller.onMouseDown(down(0, scroller.getHeight() - 1));

        assertEquals(51, scroller.getValue(),
            "pressing the bottom arrow should scroll down immediately");
    }

    @Test
    void verticalReleaseAloneDoesNotScroll() {
        TVScroller scroller = vScroller();

        scroller.onMouseUp(up(0, 0));
        scroller.onMouseUp(up(0, scroller.getHeight() - 1));

        assertEquals(50, scroller.getValue(),
            "releasing the mouse should not scroll, only stop repeating");
    }

    @Test
    void verticalTroughPagesOnPress() {
        TVScroller scroller = vScroller();

        scroller.onMouseDown(down(0, 2));
        assertEquals(30, scroller.getValue(),
            "pressing above the box should page up");

        scroller.setValue(50);
        scroller.onMouseDown(down(0, 6));
        assertEquals(70, scroller.getValue(),
            "pressing below the box should page down");
    }

    @Test
    void verticalPressingTheBoxDoesNotScroll() {
        TVScroller scroller = vScroller();

        scroller.onMouseDown(down(0, 4));

        assertEquals(50, scroller.getValue(),
            "pressing the box starts a drag, it should not scroll");
    }

    @Test
    void verticalClampsAtTheEnds() {
        TVScroller scroller = vScroller();

        scroller.setValue(0);
        scroller.onMouseDown(down(0, 0));
        assertEquals(0, scroller.getValue(), "should not scroll above the top");

        scroller.setValue(100);
        scroller.onMouseDown(down(0, scroller.getHeight() - 1));
        assertEquals(100, scroller.getValue(),
            "should not scroll below the bottom");
    }

    @Test
    void verticalIgnoresOtherMouseButtons() {
        TVScroller scroller = vScroller();

        scroller.onMouseDown(mouse(TMouseEvent.Type.MOUSE_DOWN, 0, 0, false));

        assertEquals(50, scroller.getValue(),
            "only button 1 should drive the scroll bar");
    }

    @Test
    void verticalEmptyRangeDoesNotScroll() {
        TVScroller scroller = new TVScroller(null, 0, 0, 10);
        scroller.setTopValue(0);
        scroller.setBottomValue(0);
        scroller.setValue(0);

        scroller.onMouseDown(down(0, 0));
        scroller.onMouseDown(down(0, scroller.getHeight() - 1));

        assertEquals(0, scroller.getValue(),
            "an empty range has nothing to scroll");
    }

    @Test
    void verticalRepeatKeepsUsingTheRegionThatWasPressed() {
        TVScroller scroller = vScroller();

        scroller.onMouseDown(down(0, 0));
        assertEquals(49, scroller.getValue());

        // The repeat is re-dispatched by position, but it must keep scrolling
        // the arrow that was originally pressed rather than re-reading the
        // region under those coordinates.
        scroller.onMouseDown(repeat(0, 5));
        scroller.onMouseDown(repeat(0, 5));

        assertEquals(47, scroller.getValue(),
            "each repeat should step the originally pressed arrow");
    }

    @Test
    void verticalRepeatWithoutAPressDoesNothing() {
        TVScroller scroller = vScroller();

        scroller.onMouseDown(repeat(0, 0));

        assertEquals(50, scroller.getValue(),
            "a repeat with no press behind it should not scroll");
    }

    @Test
    void verticalRepeatStopsAfterRelease() {
        TVScroller scroller = vScroller();

        scroller.onMouseDown(down(0, 0));
        scroller.onMouseUp(up(0, 0));
        scroller.onMouseDown(repeat(0, 0));

        assertEquals(49, scroller.getValue(),
            "releasing clears the pressed region, so a late repeat is inert");
    }

    // ------------------------------------------------------------------------
    // Horizontal -------------------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    void horizontalLeftArrowScrollsOnPress() {
        THScroller scroller = hScroller();

        scroller.onMouseDown(down(0, 0));

        assertEquals(49, scroller.getValue(),
            "pressing the left arrow should scroll left immediately");
    }

    @Test
    void horizontalRightArrowScrollsOnPress() {
        THScroller scroller = hScroller();

        scroller.onMouseDown(down(scroller.getWidth() - 1, 0));

        assertEquals(51, scroller.getValue(),
            "pressing the right arrow should scroll right immediately");
    }

    @Test
    void horizontalReleaseAloneDoesNotScroll() {
        THScroller scroller = hScroller();

        scroller.onMouseUp(up(0, 0));
        scroller.onMouseUp(up(scroller.getWidth() - 1, 0));

        assertEquals(50, scroller.getValue(),
            "releasing the mouse should not scroll, only stop repeating");
    }

    @Test
    void horizontalTroughPagesOnPress() {
        THScroller scroller = hScroller();

        scroller.onMouseDown(down(2, 0));
        assertEquals(30, scroller.getValue(),
            "pressing left of the box should page left");

        scroller.setValue(50);
        scroller.onMouseDown(down(6, 0));
        assertEquals(70, scroller.getValue(),
            "pressing right of the box should page right");
    }

    @Test
    void horizontalPressingTheBoxDoesNotScroll() {
        THScroller scroller = hScroller();

        scroller.onMouseDown(down(4, 0));

        assertEquals(50, scroller.getValue(),
            "pressing the box starts a drag, it should not scroll");
    }

    @Test
    void horizontalClampsAtTheEnds() {
        THScroller scroller = hScroller();

        scroller.setValue(0);
        scroller.onMouseDown(down(0, 0));
        assertEquals(0, scroller.getValue(), "should not scroll past the left");

        scroller.setValue(100);
        scroller.onMouseDown(down(scroller.getWidth() - 1, 0));
        assertEquals(100, scroller.getValue(),
            "should not scroll past the right");
    }

    @Test
    void horizontalIgnoresOtherMouseButtons() {
        THScroller scroller = hScroller();

        scroller.onMouseDown(mouse(TMouseEvent.Type.MOUSE_DOWN, 0, 0, false));

        assertEquals(50, scroller.getValue(),
            "only button 1 should drive the scroll bar");
    }

}
