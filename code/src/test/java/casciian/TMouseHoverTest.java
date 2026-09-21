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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the hit-tested mouse-motion routing model: ordinary (non-captured)
 * mouse motion is delivered only to the widget under the pointer, and
 * {@link TWidget#onMouseEnter} / {@link TWidget#onMouseExit} transitions are
 * synthesized as the pointer moves between widgets.  Mouse capture continues
 * to own drag/release routing independently of the hover target.
 */
class TMouseHoverTest {

    /**
     * A TButton that records how many enter, exit, and motion events it
     * receives, so that tests can verify which widget events are routed to.
     */
    private static class CountingButton extends TButton {
        int enters = 0;
        int exits = 0;
        int motions = 0;

        CountingButton(final TWidget parent, final String text, final int x,
            final int y) {

            super(parent, text, x, y, new TAction() {
                public void DO() {
                    // no-op
                }
            });
        }

        @Override
        public void onMouseEnter(final TMouseEvent mouse) {
            enters++;
            super.onMouseEnter(mouse);
        }

        @Override
        public void onMouseExit(final TMouseEvent mouse) {
            exits++;
            super.onMouseExit(mouse);
        }

        @Override
        public void onMouseMotion(final TMouseEvent mouse) {
            motions++;
            super.onMouseMotion(mouse);
        }
    }

    /**
     * A plain container widget that records enter/exit transitions, used to
     * verify that shared ancestors are not disturbed when the pointer moves
     * between their descendants.
     */
    private static class CountingPanel extends TPanel {
        int enters = 0;
        int exits = 0;

        CountingPanel(final TWidget parent, final int x, final int y,
            final int width, final int height) {

            super(parent, x, y, width, height);
        }

        @Override
        public void onMouseEnter(final TMouseEvent mouse) {
            enters++;
            super.onMouseEnter(mouse);
        }

        @Override
        public void onMouseExit(final TMouseEvent mouse) {
            exits++;
            super.onMouseExit(mouse);
        }
    }

    // ------------------------------------------------------------------------
    // Enter/exit transitions -------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    void movingBetweenSiblingsSendsExitAndEnter() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        CountingButton a = new CountingButton(window, "A", 1, 1);
        CountingButton b = new CountingButton(window, "B", 20, 1);

        // Move the pointer over A.
        hover(app, a);
        assertSame(a, app.getMouseHoverTarget());
        assertEquals(1, a.enters);
        assertEquals(0, a.exits);
        assertEquals(0, b.enters, "B must not see the pointer while it is over A");

        // Move the pointer over B.
        hover(app, b);
        assertSame(b, app.getMouseHoverTarget());
        assertEquals(1, a.exits, "A should exit when the pointer leaves it");
        assertEquals(1, b.enters, "B should enter when the pointer arrives");
        assertEquals(1, a.enters, "A should not re-enter");
    }

    @Test
    void ordinaryMotionReachesOnlyTheHitWidget() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        CountingButton a = new CountingButton(window, "A", 1, 1);
        CountingButton b = new CountingButton(window, "B", 20, 1);

        // A non-captured motion over A is routed only to A.
        motion(window, a.getAbsoluteX() + 1, a.getAbsoluteY(), false);
        assertEquals(1, a.motions);
        assertEquals(0, b.motions, "the sibling must receive no motion");
    }

    @Test
    void leavingAllWidgetsSendsExitAndStopsMotion() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        CountingButton a = new CountingButton(window, "A", 1, 1);

        hover(app, a);
        assertSame(a, app.getMouseHoverTarget());

        // Move into empty window space (over the window, not over any child).
        int emptyX = window.getAbsoluteX() + 30;
        int emptyY = window.getAbsoluteY() + 5;
        hoverAt(app, emptyX, emptyY);

        assertEquals(1, a.exits, "the widget should exit when the pointer leaves it");
        assertFalse(app.getMouseHoverTarget() == a,
            "the widget must no longer be the hover target");

        // Ordinary motion over empty space is not delivered to the widget.
        int before = a.motions;
        motion(window, emptyX, emptyY, false);
        assertEquals(before, a.motions,
            "the widget must not receive motion once the pointer has left it");
    }

    @Test
    void sharedAncestorsAreNotDisturbedWhenMovingBetweenChildren() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 12);
        CountingPanel panel = new CountingPanel(window, 1, 1, 30, 8);
        CountingButton a = new CountingButton(panel, "A", 1, 1);
        CountingButton b = new CountingButton(panel, "B", 1, 4);

        hover(app, a);
        assertEquals(1, panel.enters);
        assertEquals(1, a.enters);

        // Move from A to B within the same panel.
        hover(app, b);
        assertEquals(1, a.exits);
        assertEquals(1, b.enters);
        assertEquals(1, panel.enters, "the shared panel must not re-enter");
        assertEquals(0, panel.exits, "the shared panel must not exit");
    }

    @Test
    void overlappingWidgetsRouteToTopmost() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        // A and B occupy the same location; B is created last so it is topmost.
        CountingButton a = new CountingButton(window, "A", 1, 1);
        CountingButton b = new CountingButton(window, "B", 1, 1);

        hoverAt(app, b.getAbsoluteX() + 1, b.getAbsoluteY());
        assertSame(b, app.getMouseHoverTarget(),
            "only the topmost overlapping widget should be the hover target");
        assertEquals(1, b.enters);
        assertEquals(0, a.enters, "the covered widget must not receive enter");
    }

    // ------------------------------------------------------------------------
    // Interaction with mouse capture -----------------------------------------
    // ------------------------------------------------------------------------

    @Test
    void captureOwnsDragAndHoverIsSuspended() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        CountingButton a = new CountingButton(window, "A", 1, 1);
        CountingButton b = new CountingButton(window, "B", 20, 1);

        // Hover A, then press it to begin a captured interaction.
        hover(app, a);
        int aMotionsBefore = a.motions;
        mouseDown(a, 1, 0);
        assertTrue(app.hasMouseCapture(a));

        // Drag over B: the captured widget A receives the drag, B does not.
        app.handleMouseCapture(mouseEvent(TMouseEvent.Type.MOUSE_MOTION,
            b.getAbsoluteX() + 1, b.getAbsoluteY(), true));
        assertTrue(a.motions > aMotionsBefore, "A should receive the captured drag");
        assertEquals(0, b.motions, "B must not receive the drag as normal motion");
    }

    @Test
    void hoverIsReconciledAfterCaptureEnds() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        CountingButton a = new CountingButton(window, "A", 1, 1);
        CountingButton b = new CountingButton(window, "B", 20, 1);

        // Hover A, press it, drag over B, release over B.
        hover(app, a);
        mouseDown(a, 1, 0);
        app.handleMouseCapture(mouseEvent(TMouseEvent.Type.MOUSE_MOTION,
            b.getAbsoluteX() + 1, b.getAbsoluteY(), true));

        TMouseEvent release = mouseEvent(TMouseEvent.Type.MOUSE_UP,
            b.getAbsoluteX() + 1, b.getAbsoluteY(), true);
        boolean consumed = app.handleMouseCapture(release);
        assertTrue(consumed, "the release must be consumed by the captured widget");
        assertNull(app.getMouseCapture(), "capture must end on release");

        // The primary handler reconciles hover with the same release event.
        app.updateMouseHover(release);
        assertSame(b, app.getMouseHoverTarget(),
            "hover should be reconciled to the widget under the pointer");
        assertEquals(1, a.exits, "A should exit after the pointer left it");
        assertEquals(1, b.enters, "B should enter after the drag released over it");
    }

    @Test
    void capturedWidgetKeepsReceivingMotionOutsideItsBounds() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        CountingButton a = new CountingButton(window, "A", 1, 1);

        mouseDown(a, 1, 0);
        assertTrue(app.hasMouseCapture(a));

        // A point well outside A's bounds still reaches A through capture.
        int before = a.motions;
        app.handleMouseCapture(mouseEvent(TMouseEvent.Type.MOUSE_MOTION,
            window.getAbsoluteX() + 35, window.getAbsoluteY() + 8, true));
        assertTrue(a.motions > before,
            "a captured widget keeps receiving motion outside its bounds");
    }

    // ------------------------------------------------------------------------
    // Hyperlink hover --------------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    void hyperlinkHoverFollowsEnterExit() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        THyperLink link = new THyperLink(window, "link", "http://example.com",
            2, 2);
        CountingButton other = new CountingButton(window, "B", 20, 1);

        assertFalse(link.isHover(), "hover starts false");

        // Move onto the link.
        hoverAt(app, link.getAbsoluteX() + 1, link.getAbsoluteY());
        assertTrue(link.isHover(), "hover becomes true when the pointer arrives");

        // Move off the link onto another widget.
        hover(app, other);
        assertFalse(link.isHover(),
            "hover becomes false when the pointer leaves, without motion broadcast");
    }

    // ------------------------------------------------------------------------
    // Lifecycle safety -------------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    void removingHoveredWidgetClearsHoverTarget() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        CountingButton a = new CountingButton(window, "A", 1, 1);
        CountingButton b = new CountingButton(window, "B", 20, 1);

        hover(app, a);
        assertSame(a, app.getMouseHoverTarget());

        // Removing the hovered widget must not leave a stale hover reference.
        window.remove(a);
        assertNull(app.getMouseHoverTarget(),
            "the hover target must be cleared when the hovered widget is removed");

        // Subsequent movement still works normally.
        hover(app, b);
        assertSame(b, app.getMouseHoverTarget());
        assertEquals(1, b.enters);
    }

    @Test
    void closingHoveredWindowClearsHoverTarget() {
        TApplication app = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(app, "test", 0, 0, 40, 10);
        CountingButton a = new CountingButton(window, "A", 1, 1);

        hover(app, a);
        assertSame(a, app.getMouseHoverTarget());

        app.closeWindow(window);
        assertNull(app.getMouseHoverTarget(),
            "closing the window must clear a hover target inside it");
    }

    // ------------------------------------------------------------------------
    // Helpers ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Move the pointer over the center-left of a widget and update hover.
     *
     * @param app the application
     * @param widget the widget to hover
     */
    private void hover(final TApplication app, final TWidget widget) {
        hoverAt(app, widget.getAbsoluteX() + 1, widget.getAbsoluteY());
    }

    /**
     * Move the pointer to an absolute position and update hover.
     *
     * @param app the application
     * @param absX absolute column
     * @param absY absolute row
     */
    private void hoverAt(final TApplication app, final int absX, final int absY) {
        app.updateMouseHover(mouseEvent(TMouseEvent.Type.MOUSE_MOTION,
            absX, absY, false));
    }

    /**
     * Send a MOUSE_MOTION through a container's tree routing.
     *
     * @param widget the container
     * @param absX absolute column
     * @param absY absolute row
     * @param mouse1 whether button 1 is held
     */
    private void motion(final TWidget widget, final int absX, final int absY,
        final boolean mouse1) {

        widget.onMouseMotion(mouseEvent(TMouseEvent.Type.MOUSE_MOTION,
            absX, absY, mouse1));
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
     * Build a mouse event whose relative and absolute coordinates are equal
     * (screen-absolute), as delivered to the application-level routing.
     *
     * @param type the event type
     * @param absX absolute column
     * @param absY absolute row
     * @param mouse1 whether button 1 is held
     * @return the event
     */
    private TMouseEvent mouseEvent(final TMouseEvent.Type type, final int absX,
        final int absY, final boolean mouse1) {

        return new TMouseEvent(null, type, absX, absY, absX, absY, 0, 0,
            mouse1, false, false, false, false, false, false, false);
    }
}
