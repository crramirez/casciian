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

import org.junit.jupiter.api.Test;

import casciian.backend.HeadlessBackend;
import casciian.event.TMouseEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the global mouse capture mechanism: a widget that begins a stateful
 * mouse interaction owns drag/release events through to the end, even when the
 * pointer leaves its bounds or moves over another widget.
 */
class TMouseCaptureTest {

    /**
     * A TButton that records how many mouse release and motion events it
     * receives, so that tests can verify which widget events are routed to.
     */
    private static class CountingButton extends TButton {
        int ups = 0;
        int motions = 0;

        CountingButton(final TWidget parent, final String text, final int x,
            final int y, final TAction action) {

            super(parent, text, x, y, action);
        }

        @Override
        public void onMouseUp(final TMouseEvent mouse) {
            ups++;
            super.onMouseUp(mouse);
        }

        @Override
        public void onMouseMotion(final TMouseEvent mouse) {
            motions++;
            super.onMouseMotion(mouse);
        }
    }

    // ------------------------------------------------------------------------
    // Capture ownership API --------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    void captureOwnershipIsExclusiveAndReleasableOnlyByOwner() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        TButton a = new TButton(window, "A", 1, 1, doNothing());
        TButton b = new TButton(window, "B", 10, 1, doNothing());

        assertNull(app.getMouseCapture());

        app.captureMouse(a);
        assertTrue(app.hasMouseCapture(a));
        assertFalse(app.hasMouseCapture(b));

        // Requesting capture again by the same widget is safe.
        app.captureMouse(a);
        assertTrue(app.hasMouseCapture(a));

        // Only one owner at a time; requesting replaces the previous owner.
        app.captureMouse(b);
        assertTrue(app.hasMouseCapture(b));
        assertFalse(app.hasMouseCapture(a));

        // Releasing another widget's capture is a no-op.
        app.releaseMouseCapture(a);
        assertTrue(app.hasMouseCapture(b));

        // The owner can release its own capture.
        app.releaseMouseCapture(b);
        assertNull(app.getMouseCapture());
    }

    @Test
    void replacingCaptureClearsPreviousWidgetInteractionState() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        int[] countA = new int[1];
        int[] countB = new int[1];
        TButton a = new TButton(window, "A", 1, 1, counter(countA));
        TButton b = new TButton(window, "B", 10, 1, counter(countB));

        pressInside(a);
        assertTrue(app.hasMouseCapture(a));

        pressInside(b);
        assertTrue(app.hasMouseCapture(b));
        assertFalse(app.hasMouseCapture(a));

        route(app, TMouseEvent.Type.MOUSE_UP,
            b.getAbsoluteX() + 1, b.getAbsoluteY(), true);
        route(app, TMouseEvent.Type.MOUSE_UP,
            a.getAbsoluteX() + 1, a.getAbsoluteY(), true);

        assertEquals(0, countA[0],
            "the previous capturer must not keep a stale pressed state");
        assertEquals(1, countB[0],
            "the replacement capturer should still handle its own release");
    }

    @Test
    void captureOwnershipRejectsForeignOrDetachedWidgets() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        TButton local = new TButton(window, "A", 1, 1, doNothing());

        TApplication otherApp = new TApplication(new HeadlessBackend());
        TWindow otherWindow = new TWindow(otherApp, "other", 0, 0, 40, 10);
        TButton foreign = new TButton(otherWindow, "B", 1, 1, doNothing());

        app.captureMouse(foreign);
        assertNull(app.getMouseCapture(),
            "foreign widgets must not become this application's capture owner");

        window.remove(local);
        app.captureMouse(local);
        assertNull(app.getMouseCapture(),
            "detached widgets must not become the capture owner");
    }

    @Test
    void freshLeftDownClearsPreviousCaptureBeforeNormalDispatch() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        int[] count = new int[1];
        TButton a = new TButton(window, "A", 1, 1, counter(count));

        pressInside(a);
        assertTrue(app.hasMouseCapture(a));

        // A fresh left-button press elsewhere must end A's capture so a new
        // target that does not itself capture is not swallowed by A.
        route(app, TMouseEvent.Type.MOUSE_DOWN,
            a.getAbsoluteX() + 20, a.getAbsoluteY(), true);
        assertNull(app.getMouseCapture(),
            "a fresh left-button press must release the previous capture");

        // A's stale press state must not later fire on an unrelated release.
        route(app, TMouseEvent.Type.MOUSE_UP,
            a.getAbsoluteX() + 1, a.getAbsoluteY(), true);
        assertEquals(0, count[0],
            "the previous capturer must not keep a stale pressed state");
    }

    @Test
    void nonLeftDownDuringCaptureKeepsCapture() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        TButton a = new TButton(window, "A", 1, 1, doNothing());

        pressInside(a);
        assertTrue(app.hasMouseCapture(a));

        // A wheel or other-button press is delivered as MOUSE_DOWN with
        // mouse1 == false; it must not disturb an active capture.
        route(app, TMouseEvent.Type.MOUSE_DOWN,
            a.getAbsoluteX() + 20, a.getAbsoluteY(), false);
        assertTrue(app.hasMouseCapture(a),
            "a non-left mouse press must not release the capture");
    }

    // ------------------------------------------------------------------------
    // Button semantics -------------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    void buttonReleaseOutsideDoesNotDispatch() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        int[] count = new int[1];
        TButton a = new TButton(window, "A", 1, 1, counter(count));

        pressInside(a);
        assertTrue(app.hasMouseCapture(a));

        // Drag the pointer well outside the button, still holding.
        route(app, TMouseEvent.Type.MOUSE_MOTION,
            a.getAbsoluteX() - 5, a.getAbsoluteY(), true);
        assertTrue(app.hasMouseCapture(a),
            "the button must keep the capture while the pointer is outside");

        // Release outside the button.
        route(app, TMouseEvent.Type.MOUSE_UP,
            a.getAbsoluteX() - 5, a.getAbsoluteY(), true);

        assertEquals(0, count[0], "action must not fire on release outside");
        assertNull(app.getMouseCapture(), "capture must be released");
    }

    @Test
    void buttonLeaveAndReenterDispatchesExactlyOnce() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        int[] count = new int[1];
        TButton a = new TButton(window, "A", 1, 1, counter(count));

        pressInside(a);

        // Drag outside then back inside while holding.
        route(app, TMouseEvent.Type.MOUSE_MOTION,
            a.getAbsoluteX() - 5, a.getAbsoluteY(), true);
        route(app, TMouseEvent.Type.MOUSE_MOTION,
            a.getAbsoluteX() + 1, a.getAbsoluteY(), true);
        assertTrue(app.hasMouseCapture(a));

        // Release back inside the button.
        route(app, TMouseEvent.Type.MOUSE_UP,
            a.getAbsoluteX() + 1, a.getAbsoluteY(), true);

        assertEquals(1, count[0], "action must fire exactly once");
        assertNull(app.getMouseCapture());
    }

    @Test
    void releaseOverDifferentWidgetGoesToCapturer() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        int[] countA = new int[1];
        int[] countB = new int[1];
        CountingButton a = new CountingButton(window, "A", 1, 1,
            counter(countA));
        CountingButton b = new CountingButton(window, "B", 12, 1,
            counter(countB));

        pressInside(a);

        // Move over and release over button B.
        route(app, TMouseEvent.Type.MOUSE_MOTION,
            b.getAbsoluteX() + 1, b.getAbsoluteY(), true);
        route(app, TMouseEvent.Type.MOUSE_UP,
            b.getAbsoluteX() + 1, b.getAbsoluteY(), true);

        // Button A received the drag and the release; button B saw neither.
        assertTrue(a.ups >= 1, "capturer A must receive the release");
        assertEquals(0, b.ups, "B must not receive the release");
        assertEquals(0, b.motions, "B must not receive the drag");
        assertEquals(0, countA[0], "A must not fire (released outside A)");
        assertEquals(0, countB[0], "B must not activate");
        assertNull(app.getMouseCapture());
    }

    // ------------------------------------------------------------------------
    // Text selection ---------------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    void textSelectionContinuesOutsideBoundsThenReleases() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 12);
        TText text = new TText(window, "hello world\nsecond line\nthird line",
            1, 1, 20, 6);

        // Press inside the text area to begin a selection.
        mouseDown(text, 3, 0);
        assertTrue(app.hasMouseCapture(text),
            "text selection must capture the mouse");

        // Drag past the left/top border (negative relative coordinates).
        route(app, TMouseEvent.Type.MOUSE_MOTION,
            text.getAbsoluteX() - 4, text.getAbsoluteY() + 2, true);
        assertTrue(app.hasMouseCapture(text),
            "selection must continue while the pointer is outside");

        // Release finalizes the selection and drops the capture.
        route(app, TMouseEvent.Type.MOUSE_UP,
            text.getAbsoluteX() - 4, text.getAbsoluteY() + 2, true);

        assertNull(app.getMouseCapture());
        assertNotNull(text.getSelection());
    }

    @Test
    void strayDragAfterReleaseDoesNotContinueSelection() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 12);
        TText text = new TText(window, "hello world\nsecond line\nthird line",
            1, 1, 20, 6);

        // Press inside the text area and drag to form a real selection.
        mouseDown(text, 3, 0);
        route(app, TMouseEvent.Type.MOUSE_MOTION,
            text.getAbsoluteX() + 8, text.getAbsoluteY() + 1, true);
        route(app, TMouseEvent.Type.MOUSE_UP,
            text.getAbsoluteX() + 8, text.getAbsoluteY() + 1, true);

        assertNull(app.getMouseCapture());
        String selection = text.getSelection();
        assertNotNull(selection);

        // A later press begins elsewhere in the window (not on the text
        // widget) and the resulting motion is broadcast to all children with
        // mouse button 1 held.  Because the text widget is no longer actively
        // selecting, this stray drag must not extend the existing selection.
        TMouseEvent stray = new TMouseEvent(null, TMouseEvent.Type.MOUSE_MOTION,
            text.getWidth() + 5, text.getHeight() + 5,
            text.getAbsoluteX() + text.getWidth() + 5,
            text.getAbsoluteY() + text.getHeight() + 5,
            0, 0, true, false, false, false, false, false, false, false);
        text.onMouseMotion(stray);

        assertNull(app.getMouseCapture());
        assertEquals(selection, text.getSelection(),
            "a stray drag after release must not extend the selection");
    }

    @Test
    void clickingOutsideTextAreaReleasesTextCapture() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 12);
        TText text = new TText(window, "hello world\nsecond line\nthird line",
            1, 1, 20, 6);

        mouseDown(text, 3, 0);
        assertTrue(app.hasMouseCapture(text));

        mouseDown(text, -1, -1);

        assertNull(app.getMouseCapture(),
            "clicking outside the text area must drop the active selection drag");
    }

    // ------------------------------------------------------------------------
    // Split pane divider drag ------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    void splitPaneDividerDragContinuesOutsideAndReleases() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 14);
        TSplitPane split = new TSplitPane(window, 1, 1, 20, 8, true);
        int startSplit = split.getSplit();

        // Press on the divider column to begin moving it.
        mouseDown(split, startSplit, 1);
        assertTrue(app.hasMouseCapture(split),
            "the split pane must own the capture while moving the divider");

        // Drag the pointer well past the right edge of the pane.
        route(app, TMouseEvent.Type.MOUSE_MOTION,
            split.getAbsoluteX() + split.getWidth() + 10,
            split.getAbsoluteY() + 1, true);
        assertTrue(app.hasMouseCapture(split),
            "the split pane stays the owner while the pointer is outside");
        assertEquals(split.getWidth() - 2, split.getSplit(),
            "the divider keeps following the pointer and clamps at the edge");

        // Release ends the drag.
        route(app, TMouseEvent.Type.MOUSE_UP,
            split.getAbsoluteX() + split.getWidth() + 10,
            split.getAbsoluteY() + 1, true);
        assertNull(app.getMouseCapture());
    }

    @Test
    void splitPaneNonLeftDownDoesNotStrandCapture() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 14);
        TSplitPane split = new TSplitPane(window, 1, 1, 20, 8, true);
        int startSplit = split.getSplit();

        // Press on the divider column to begin moving it.
        mouseDown(split, startSplit, 1);
        assertTrue(app.hasMouseCapture(split),
            "the split pane must own the capture while moving the divider");

        // A non-left MOUSE_DOWN (for example a wheel event) arrives during the
        // drag.  It must not clear the active drag state and strand the capture.
        route(app, TMouseEvent.Type.MOUSE_DOWN,
            split.getAbsoluteX() + startSplit, split.getAbsoluteY() + 1, false);
        assertTrue(app.hasMouseCapture(split),
            "the split pane stays captured after a non-left mouse press");

        // The eventual left-button release still ends the drag.
        route(app, TMouseEvent.Type.MOUSE_UP,
            split.getAbsoluteX() + startSplit, split.getAbsoluteY() + 1, true);
        assertNull(app.getMouseCapture());
    }

    // ------------------------------------------------------------------------
    // Scrollbar thumb drag ---------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    void scrollbarThumbDragOutsideScrollsWithoutSelectingText() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 14);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            sb.append("line ").append(i).append("\n");
        }
        TText text = new TText(window, sb.toString(), 1, 1, 20, 8);
        TVScroller vScroller = text.getVerticalScroller();
        assertNotNull(vScroller);

        // Press on the scroll box (at value 0 it sits at relative row 1).
        mouseDown(vScroller, 0, 1);
        assertTrue(app.hasMouseCapture(vScroller),
            "dragging the scroll box must capture the mouse");

        // Drag the pointer down and to the LEFT, into the text area and past
        // the left border of the scrollbar - exactly the "leave the scrollbar
        // by mistake" case.  The scrollbar keeps the capture and scrolls; the
        // text must NOT begin a selection.
        route(app, TMouseEvent.Type.MOUSE_MOTION,
            vScroller.getAbsoluteX() - 6, vScroller.getAbsoluteY() + 4, true);
        assertTrue(app.hasMouseCapture(vScroller),
            "the scrollbar stays the owner while the pointer is outside");
        assertTrue(vScroller.getValue() > 0,
            "dragging the scroll box must scroll the view");
        assertNull(text.getSelection(),
            "leaving the scrollbar must not start a text selection");

        // Release ends the drag and drops the capture.
        route(app, TMouseEvent.Type.MOUSE_UP,
            vScroller.getAbsoluteX() - 6, vScroller.getAbsoluteY() + 4, true);
        assertNull(app.getMouseCapture());
        assertNull(text.getSelection(),
            "no selection should exist after a scrollbar drag");
    }

    @Test
    void collapsedScrollbarRangeDuringDragReleasesCapture() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 14);
        THScroller hScroller = new THScroller(window, 1, 1, 10);
        TVScroller vScroller = new TVScroller(window, 20, 1, 8);

        hScroller.setRightValue(10);
        mouseDown(hScroller, 1, 0);
        assertTrue(app.hasMouseCapture(hScroller));
        hScroller.setRightValue(hScroller.getLeftValue());
        route(app, TMouseEvent.Type.MOUSE_MOTION,
            hScroller.getAbsoluteX() + 4, hScroller.getAbsoluteY(), true);
        assertNull(app.getMouseCapture(),
            "a collapsed horizontal range must stop the thumb drag");

        vScroller.setBottomValue(10);
        mouseDown(vScroller, 0, 1);
        assertTrue(app.hasMouseCapture(vScroller));
        vScroller.setBottomValue(vScroller.getTopValue());
        route(app, TMouseEvent.Type.MOUSE_MOTION,
            vScroller.getAbsoluteX(), vScroller.getAbsoluteY() + 4, true);
        assertNull(app.getMouseCapture(),
            "a collapsed vertical range must stop the thumb drag");
    }

    // ------------------------------------------------------------------------
    // Lifecycle safety -------------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    void removingCapturedWidgetClearsCapture() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        int[] count = new int[1];
        TButton a = new TButton(window, "A", 1, 1, counter(count));

        pressInside(a);
        assertTrue(app.hasMouseCapture(a));

        window.remove(a);

        assertNull(app.getMouseCapture());

        TWindow otherWindow = new TWindow(app, "other", 0, 12, 40, 10);
        a.setParent(otherWindow, false);
        route(app, TMouseEvent.Type.MOUSE_UP,
            a.getAbsoluteX() + 1, a.getAbsoluteY(), true);
        assertEquals(0, count[0],
            "removing a captured widget must clear its pressed state");

        // Subsequent mouse events must not throw or be routed anywhere.
        route(app, TMouseEvent.Type.MOUSE_MOTION, 5, 5, true);
        route(app, TMouseEvent.Type.MOUSE_UP, 5, 5, false);
        assertNull(app.getMouseCapture());
    }

    @Test
    void closingCapturedWindowClearsCapture() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        TButton a = new TButton(window, "A", 1, 1, doNothing());

        pressInside(a);
        assertTrue(app.hasMouseCapture(a));

        app.closeWindow(window);

        assertNull(app.getMouseCapture());
    }

    @Test
    void disablingCapturedWidgetClearsCapture() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        TButton a = new TButton(window, "A", 1, 1, doNothing());

        pressInside(a);
        assertTrue(app.hasMouseCapture(a));

        a.setEnabled(false);

        assertNull(app.getMouseCapture());
    }

    @Test
    void disablingContainerReleasesCapturedDescendant() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        TPanel panel = new TPanel(window, 1, 1, 20, 6);
        int[] count = new int[1];
        TButton a = new TButton(panel, "A", 1, 1, counter(count));

        pressInside(a);
        assertTrue(app.hasMouseCapture(a));

        // Disabling the container, not the button directly, must still notify
        // and release the captured descendant.
        panel.setEnabled(false);
        assertNull(app.getMouseCapture(),
            "disabling a container must release a captured descendant");

        // Re-enabling and releasing normally must not fire the stale click.
        panel.setEnabled(true);
        route(app, TMouseEvent.Type.MOUSE_UP,
            a.getAbsoluteX() + 1, a.getAbsoluteY(), false);
        assertEquals(0, count[0],
            "a released descendant must not resume its old interaction");
    }

    @Test
    void collapsedRangeOnMouseDownReleasesCapture() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 14);
        THScroller hScroller = new THScroller(window, 1, 1, 10);
        TVScroller vScroller = new TVScroller(window, 20, 1, 8);

        hScroller.setRightValue(10);
        mouseDown(hScroller, 1, 0);
        assertTrue(app.hasMouseCapture(hScroller));
        // Range collapses while the thumb is held, then a fresh MOUSE_DOWN is
        // hit-tested to the scrollbar before any motion/up event.
        hScroller.setRightValue(hScroller.getLeftValue());
        mouseDown(hScroller, 1, 0);
        assertNull(app.getMouseCapture(),
            "a collapsed horizontal range on mouse-down must release capture");

        vScroller.setBottomValue(10);
        mouseDown(vScroller, 0, 1);
        assertTrue(app.hasMouseCapture(vScroller));
        vScroller.setBottomValue(vScroller.getTopValue());
        mouseDown(vScroller, 0, 1);
        assertNull(app.getMouseCapture(),
            "a collapsed vertical range on mouse-down must release capture");
    }

    @Test
    void quickClickOnArrowPerformsExactlyOneStep() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 14);
        THScroller hScroller = new THScroller(window, 1, 1, 12);
        hScroller.setRightValue(100);
        hScroller.setValue(50);
        hScroller.setSmallChange(1);

        // Press on the right arrow, then release right away.  The initial
        // press fires a single immediate step; the release must not add more.
        mouseDown(hScroller, hScroller.getWidth() - 1, 0);
        assertEquals(51, hScroller.getValue(),
            "the initial press must fire exactly one step");
        mouseUp(hScroller, hScroller.getWidth() - 1, 0);
        assertEquals(51, hScroller.getValue(),
            "releasing must not fire an extra step");
        assertNull(app.getMouseCapture(),
            "releasing must drop the capture");
    }

    @Test
    void heldArrowRepeatsWhileButtonIsDown() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 14);
        THScroller hScroller = new THScroller(window, 1, 1, 12);
        hScroller.setRightValue(100);
        hScroller.setValue(50);
        hScroller.setSmallChange(1);

        // Initial press: one immediate step.
        mouseDown(hScroller, hScroller.getWidth() - 1, 0);
        assertEquals(51, hScroller.getValue());

        // The repeat timer re-dispatches auto-repeat presses.  Simulate two
        // ticks; each performs another step on the pressed region.
        mouseDownAutoRepeat(hScroller, hScroller.getWidth() - 1, 0);
        mouseDownAutoRepeat(hScroller, hScroller.getWidth() - 1, 0);
        assertEquals(53, hScroller.getValue(),
            "each repeat tick must advance the scroll value");
    }

    @Test
    void repeatStopsWhenPointerLeavesPressedRegion() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 14);
        THScroller hScroller = new THScroller(window, 1, 1, 12);
        hScroller.setRightValue(100);
        hScroller.setValue(50);
        hScroller.setSmallChange(1);

        mouseDown(hScroller, hScroller.getWidth() - 1, 0);
        assertEquals(51, hScroller.getValue());

        // Move off the pressed arrow while still holding the button.  This
        // stops the auto-repeat and clears the pressed region, so a later
        // repeat tick is a no-op.
        mouseMotion(hScroller, hScroller.getWidth() / 2, 0, true);
        mouseDownAutoRepeat(hScroller, hScroller.getWidth() - 1, 0);
        assertEquals(51, hScroller.getValue(),
            "moving off the pressed region must stop the repeat");
    }

    @Test
    void nonLeftReleaseDoesNotStopHeldArrowRepeat() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 14);
        THScroller hScroller = new THScroller(window, 1, 1, 12);
        hScroller.setRightValue(100);
        hScroller.setValue(50);
        hScroller.setSmallChange(1);

        mouseDown(hScroller, hScroller.getWidth() - 1, 0);
        assertEquals(51, hScroller.getValue());
        assertTrue(app.hasMouseCapture(hScroller));

        route(app, TMouseEvent.Type.MOUSE_UP,
            hScroller.getAbsoluteX() + hScroller.getWidth() - 1,
            hScroller.getAbsoluteY(), false);
        assertTrue(app.hasMouseCapture(hScroller),
            "a non-left release must not cancel the held repeat");

        mouseDownAutoRepeat(hScroller, hScroller.getWidth() - 1, 0);
        assertEquals(52, hScroller.getValue(),
            "repeat ticks must continue after a non-left release");

        mouseUp(hScroller, hScroller.getWidth() - 1, 0);
        assertNull(app.getMouseCapture(),
            "the eventual left-button release must still end the interaction");
    }

    @Test
    void nonLeftReleaseDoesNotInterruptScrollbarThumbDrag() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 14);
        TVScroller vScroller = new TVScroller(window, 20, 1, 8);
        vScroller.setBottomValue(10);

        mouseDown(vScroller, 0, 1);
        assertTrue(app.hasMouseCapture(vScroller));

        route(app, TMouseEvent.Type.MOUSE_UP,
            vScroller.getAbsoluteX(), vScroller.getAbsoluteY() + 1, false);
        assertTrue(app.hasMouseCapture(vScroller),
            "a non-left release must not end the thumb drag");

        route(app, TMouseEvent.Type.MOUSE_MOTION,
            vScroller.getAbsoluteX(), vScroller.getAbsoluteY() + 4, true);
        assertTrue(vScroller.getValue() > 0,
            "dragging must continue after a non-left release");

        route(app, TMouseEvent.Type.MOUSE_UP,
            vScroller.getAbsoluteX(), vScroller.getAbsoluteY() + 4, true);
        assertNull(app.getMouseCapture(),
            "the eventual left-button release must end the drag");
    }

    // ------------------------------------------------------------------------
    // Helpers ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    private TAction doNothing() {
        return new TAction() {
            public void DO() {
                // no-op
            }
        };
    }

    private TAction counter(final int[] count) {
        return new TAction() {
            public void DO() {
                count[0]++;
            }
        };
    }

    /**
     * Press mouse button 1 inside a button, at relative position (1, 0).
     *
     * @param button the button to press
     */
    private void pressInside(final TButton button) {
        mouseDown(button, 1, 0);
    }

    /**
     * Send a MOUSE_DOWN directly to a widget with coordinates relative to it.
     *
     * @param widget the target widget
     * @param x relative column
     * @param y relative row
     */
    private void mouseDown(final TWidget widget, final int x, final int y) {
        TMouseEvent event = new TMouseEvent(null, TMouseEvent.Type.MOUSE_DOWN,
            x, y, widget.getAbsoluteX() + x, widget.getAbsoluteY() + y, 0, 0,
            true, false, false, false, false, false, false, false);
        widget.onMouseDown(event);
    }

    /**
     * Send an auto-repeat MOUSE_DOWN directly to a widget, simulating a tick
     * of the press-and-hold repeat timer.
     *
     * @param widget the target widget
     * @param x relative column
     * @param y relative row
     */
    private void mouseDownAutoRepeat(final TWidget widget, final int x,
        final int y) {

        TMouseEvent event = new TMouseEvent(null, TMouseEvent.Type.MOUSE_DOWN,
            x, y, widget.getAbsoluteX() + x, widget.getAbsoluteY() + y, 0, 0,
            true, false, false, false, false, false, false, false);
        event.setAutoRepeat(true);
        widget.onMouseDown(event);
    }

    /**
     * Send a MOUSE_UP directly to a widget with coordinates relative to it.
     *
     * @param widget the target widget
     * @param x relative column
     * @param y relative row
     */
    private void mouseUp(final TWidget widget, final int x, final int y) {
        TMouseEvent event = new TMouseEvent(null, TMouseEvent.Type.MOUSE_UP,
            x, y, widget.getAbsoluteX() + x, widget.getAbsoluteY() + y, 0, 0,
            true, false, false, false, false, false, false, false);
        widget.onMouseUp(event);
    }

    /**
     * Send a MOUSE_MOTION directly to a widget with coordinates relative to
     * it.
     *
     * @param widget the target widget
     * @param x relative column
     * @param y relative row
     * @param mouse1 whether mouse button 1 is held
     */
    private void mouseMotion(final TWidget widget, final int x, final int y,
        final boolean mouse1) {

        TMouseEvent event = new TMouseEvent(null, TMouseEvent.Type.MOUSE_MOTION,
            x, y, widget.getAbsoluteX() + x, widget.getAbsoluteY() + y, 0, 0,
            mouse1, false, false, false, false, false, false, false);
        widget.onMouseMotion(event);
    }

    /**
     * Route a mouse event through the application's capture mechanism.  The
     * coordinates are absolute screen coordinates.
     *
     * @param app the application
     * @param type the event type
     * @param absX absolute column
     * @param absY absolute row
     * @param mouse1 whether mouse button 1 is held
     */
    private void route(final TApplication app, final TMouseEvent.Type type,
        final int absX, final int absY, final boolean mouse1) {

        TMouseEvent event = new TMouseEvent(null, type, absX, absY, absX, absY,
            0, 0, mouse1, false, false, false, false, false, false, false);
        app.handleMouseCapture(event);
    }
}
