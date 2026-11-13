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

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import casciian.backend.HeadlessBackend;
import casciian.event.TCommandEvent;
import casciian.event.TMouseEvent;
import casciian.menu.TMenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests the hit-tested mouse-motion routing model: ordinary (non-captured)
 * mouse motion is delivered only to the widget under the pointer, and
 * {@link TWidget#onMouseEnter} / {@link TWidget#onMouseExit} transitions are
 * synthesized as the pointer moves between widgets.  Mouse capture continues
 * to own drag/release routing independently of the hover target.
 */
class TMouseHoverTest {

    /** Timeout for event-loop synchronization. */
    private static final long TIMEOUT_MS = 10000;

    /** Sentinel command used to wait for all posted events to dispatch. */
    private static final TCommand SYNC_COMMAND = new TCommand(-4242);

    /** No-op command used to wake the event thread for queued work. */
    private static final TCommand WAKE_COMMAND = new TCommand(-4243);

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

    /**
     * A window that records ordinary motion, used to verify modal routing.
     */
    private static class CountingWindow extends TWindow {
        int motions = 0;

        CountingWindow(final TApplication application, final String title,
            final int x, final int y, final int width, final int height,
            final int flags) {

            super(application, title, x, y, width, height, flags);
        }

        @Override
        public void onMouseMotion(final TMouseEvent mouse) {
            motions++;
            super.onMouseMotion(mouse);
        }
    }

    /**
     * Runs a real application event loop so tests exercise the public event
     * dispatch path instead of calling hover internals directly.
     *
     * <p>Widgets are not thread-safe and the event thread draws continuously,
     * so all widget creation and mutation must go through {@link #call} or
     * {@link #ui}.  Posted
     * events are synchronized with {@link #sync()}, which posts a sentinel
     * command behind them: events are dispatched in FIFO order, so once the
     * sentinel is handled every earlier event has been processed.</p>
     */
    private static class RunningApplication implements AutoCloseable {
        final HeadlessBackend backend;
        final TApplication app;
        private final Thread thread;
        private volatile CountDownLatch synced;

        RunningApplication() {
            backend = new HeadlessBackend();
            backend.setBackend(backend);
            app = new TApplication(backend) {
                @Override
                protected boolean onCommand(final TCommandEvent command) {
                    if (command.equals(SYNC_COMMAND)) {
                        synced.countDown();
                        return true;
                    }
                    if (command.equals(WAKE_COMMAND)) {
                        return true;
                    }
                    return super.onCommand(command);
                }
            };
            thread = new Thread(app::run);
            thread.setDaemon(true);
            thread.start();
            sync();
        }

        /**
         * Wait until every previously posted event has been dispatched by
         * the primary event thread.
         */
        void sync() {
            CountDownLatch latch = new CountDownLatch(1);
            synced = latch;
            app.postEvent(new TCommandEvent(backend, SYNC_COMMAND));
            try {
                assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS),
                    "Application event-dispatch thread did not respond");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(e);
            }
        }

        /**
         * Run an action on the event-dispatch thread and wait for it.
         *
         * @param action the action to run
         * @param <T> the result type
         * @return the action's result
         */
        <T> T call(final Callable<T> action) {
            FutureTask<T> task = new FutureTask<>(action);
            app.invokeLater(task);
            // invokeLater() alone waits for the next idle tick; an event
            // wakes the event thread so the task runs promptly.
            app.postEvent(new TCommandEvent(backend, WAKE_COMMAND));
            try {
                return task.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return fail(e);
            } catch (ExecutionException | TimeoutException e) {
                return fail(e);
            }
        }

        /**
         * Run an action on the event-dispatch thread and wait for it.
         *
         * @param action the action to run
         */
        void ui(final Runnable action) {
            call(() -> {
                action.run();
                return null;
            });
        }

        @Override
        public void close() {
            app.exit();
            try {
                thread.join(TIMEOUT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(e);
            }
            assertFalse(thread.isAlive());
        }
    }

    // ------------------------------------------------------------------------
    // Enter/exit transitions -------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    void movingBetweenSiblingsSendsExitAndEnter() {
        try (RunningApplication running = new RunningApplication()) {
            TApplication app = running.app;
            TWindow window = running.call(() ->
                new TWindow(app, "test", 0, 0, 40, 10));
            CountingButton a = running.call(() ->
                new CountingButton(window, "A", 1, 1));
            CountingButton b = running.call(() ->
                new CountingButton(window, "B", 20, 1));

            // Move the pointer over A.
            hover(running, a);
            assertSame(a, app.getMouseHoverTarget());
            assertEquals(1, a.enters);
            assertEquals(0, a.exits);
            assertEquals(0, b.enters,
                "B must not see the pointer while it is over A");

            // Move the pointer over B.
            hover(running, b);
            assertSame(b, app.getMouseHoverTarget());
            assertEquals(1, a.exits,
                "A should exit when the pointer leaves it");
            assertEquals(1, b.enters, "B should enter when the pointer arrives");
            assertEquals(1, a.enters, "A should not re-enter");
        }
    }

    @Test
    void ordinaryMotionReachesOnlyTheHitWidget() {
        try (RunningApplication running = new RunningApplication()) {
            TApplication app = running.app;
            TWindow window = running.call(() ->
                new TWindow(app, "test", 0, 0, 40, 10));
            CountingButton a = running.call(() ->
                new CountingButton(window, "A", 1, 1));
            CountingButton b = running.call(() ->
                new CountingButton(window, "B", 20, 1));

            // A non-captured motion over A is routed only to A.
            hover(running, a);
            assertEquals(1, a.motions);
            assertEquals(0, b.motions, "the sibling must receive no motion");
            assertSame(a, app.getMouseHoverTarget());
        }
    }

    @Test
    void leavingAllWidgetsSendsExitAndStopsMotion() {
        try (RunningApplication running = new RunningApplication()) {
            TApplication app = running.app;
            TWindow window = running.call(() ->
                new TWindow(app, "test", 0, 0, 40, 10));
            CountingButton a = running.call(() ->
                new CountingButton(window, "A", 1, 1));

            hover(running, a);
            assertSame(a, app.getMouseHoverTarget());

            // Move into empty window space (over the window, not over any child).
            int emptyX = running.call(() -> window.getAbsoluteX() + 30);
            int emptyY = running.call(() -> window.getAbsoluteY() + 5);
            hoverAt(running, emptyX, emptyY);

            assertEquals(1, a.exits,
                "the widget should exit when the pointer leaves it");
            assertFalse(app.getMouseHoverTarget() == a,
                "the widget must no longer be the hover target");

            // Ordinary motion over empty space is not delivered to the widget.
            int before = a.motions;
            motion(running, emptyX, emptyY, false);
            assertEquals(before, a.motions,
                "the widget must not receive motion once the pointer has left it");
        }
    }

    @Test
    void sharedAncestorsAreNotDisturbedWhenMovingBetweenChildren() {
        try (RunningApplication running = new RunningApplication()) {
            TApplication app = running.app;
            TWindow window = running.call(() ->
                new TWindow(app, "test", 0, 0, 40, 12));
            CountingPanel panel = running.call(() ->
                new CountingPanel(window, 1, 1, 30, 8));
            CountingButton a = running.call(() ->
                new CountingButton(panel, "A", 1, 1));
            CountingButton b = running.call(() ->
                new CountingButton(panel, "B", 1, 4));

            hover(running, a);
            assertEquals(1, panel.enters);
            assertEquals(1, a.enters);

            // Move from A to B within the same panel.
            hover(running, b);
            assertEquals(1, a.exits);
            assertEquals(1, b.enters);
            assertEquals(1, panel.enters,
                "the shared panel must not re-enter");
            assertEquals(0, panel.exits, "the shared panel must not exit");
            assertSame(b, app.getMouseHoverTarget());
        }
    }

    @Test
    void overlappingWidgetsRouteToTopmost() {
        try (RunningApplication running = new RunningApplication()) {
            TApplication app = running.app;
            TWindow window = running.call(() ->
                new TWindow(app, "test", 0, 0, 40, 10));
            // A and B occupy the same location; B is created last so it is topmost.
            CountingButton a = running.call(() ->
                new CountingButton(window, "A", 1, 1));
            CountingButton b = running.call(() ->
                new CountingButton(window, "B", 1, 1));

            hover(running, b);
            assertSame(b, app.getMouseHoverTarget(),
                "only the topmost overlapping widget should be the hover target");
            assertEquals(1, b.enters);
            assertEquals(0, a.enters,
                "the covered widget must not receive enter");
        }
    }

    // ------------------------------------------------------------------------
    // Interaction with mouse capture -----------------------------------------
    // ------------------------------------------------------------------------

    @Test
    void captureOwnsDragAndHoverIsSuspended() {
        try (RunningApplication running = new RunningApplication()) {
            TApplication app = running.app;
            TWindow window = running.call(() ->
                new TWindow(app, "test", 0, 0, 40, 10));
            CountingButton a = running.call(() ->
                new CountingButton(window, "A", 1, 1));
            CountingButton b = running.call(() ->
                new CountingButton(window, "B", 20, 1));

            // Hover A, then press it to begin a captured interaction.
            hover(running, a);
            int aMotionsBefore = a.motions;
            mouseDown(running, a, 1, 0);
            assertTrue(app.hasMouseCapture(a), "mouse capture should start");

            // Drag over B: the captured widget A receives the drag, B does not.
            dragOver(running, b);
            assertTrue(a.motions > aMotionsBefore,
                "A should receive the captured drag");
            assertEquals(0, b.motions, "B must not receive the drag as normal motion");
            assertSame(a, app.getMouseHoverTarget(),
                "hover stays suspended on the pre-capture widget");
        }
    }

    @Test
    void hoverIsReconciledAfterCaptureEnds() {
        try (RunningApplication running = new RunningApplication()) {
            TApplication app = running.app;
            TWindow window = running.call(() ->
                new TWindow(app, "test", 0, 0, 40, 10));
            CountingButton a = running.call(() ->
                new CountingButton(window, "A", 1, 1));
            CountingButton b = running.call(() ->
                new CountingButton(window, "B", 20, 1));

            // Hover A, press it, drag over B, release over B.
            hover(running, a);
            mouseDown(running, a, 1, 0);
            assertTrue(app.hasMouseCapture(a), "mouse capture should start");
            dragOver(running, b);
            mouseUp(running, b, 1, 0);

            assertNull(app.getMouseCapture(), "capture must end on release");
            assertSame(b, app.getMouseHoverTarget(),
                "hover should be reconciled to the widget under the pointer");
            assertEquals(1, a.exits,
                "A should exit after the pointer left it");
            assertEquals(1, b.enters,
                "B should enter after the drag released over it");
        }
    }

    @Test
    void capturedWidgetKeepsReceivingMotionOutsideItsBounds() {
        try (RunningApplication running = new RunningApplication()) {
            TApplication app = running.app;
            TWindow window = running.call(() ->
                new TWindow(app, "test", 0, 0, 40, 10));
            CountingButton a = running.call(() ->
                new CountingButton(window, "A", 1, 1));

            mouseDown(running, a, 1, 0);
            assertTrue(app.hasMouseCapture(a), "mouse capture should start");

            // A point well outside A's bounds still reaches A through capture.
            int before = a.motions;
            int outsideX = running.call(() -> window.getAbsoluteX() + 35);
            int outsideY = running.call(() -> window.getAbsoluteY() + 8);
            motion(running, outsideX, outsideY, true);
            assertTrue(a.motions > before,
                "a captured widget keeps receiving motion outside its bounds");
        }
    }

    // ------------------------------------------------------------------------
    // Hyperlink hover --------------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    void hyperlinkHoverFollowsEnterExit() {
        try (RunningApplication running = new RunningApplication()) {
            TApplication app = running.app;
            TWindow window = running.call(() ->
                new TWindow(app, "test", 0, 0, 40, 10));
            THyperLink link = running.call(() ->
                new THyperLink(window, "link", "http://example.com", 2, 2));
            CountingButton other = running.call(() ->
                new CountingButton(window, "B", 20, 1));

            assertFalse(link.isHover(), "hover starts false");

            // Move onto the link.
            hover(running, link);
            assertTrue(link.isHover(),
                "hover becomes true when the pointer arrives");

            // Move off the link onto another widget.
            hover(running, other);
            assertFalse(link.isHover(),
                "hover becomes false when the pointer leaves, without motion broadcast");
            assertSame(other, app.getMouseHoverTarget());
        }
    }

    @Test
    void invisibleWidgetIsIgnoredForHover() {
        try (RunningApplication running = new RunningApplication()) {
            TApplication app = running.app;
            TWindow window = running.call(() ->
                new TWindow(app, "test", 0, 0, 40, 10));
            CountingButton visible = running.call(() ->
                new CountingButton(window, "A", 1, 1));
            CountingButton hidden = running.call(() ->
                new CountingButton(window, "B", 1, 1));
            running.ui(() -> hidden.setVisible(false));

            hover(running, hidden);
            assertSame(visible, app.getMouseHoverTarget(),
                "the visible widget should receive hover through the public path");
            assertEquals(1, visible.enters);
            assertEquals(0, hidden.enters,
                "the hidden widget must not receive hover enter");
        }
    }

    @Test
    void modalHoverDoesNotFallBackToDesktop() {
        try (RunningApplication running = new RunningApplication()) {
            TApplication app = running.app;
            TWindow background = running.call(() ->
                new TWindow(app, "background", 0, 0, 40, 10));
            CountingButton button = running.call(() ->
                new CountingButton(background, "A", 1, 1));
            CountingWindow dialog = running.call(() ->
                new CountingWindow(app, "modal", 10, 2, 20, 6, TWindow.MODAL));
            CountDownLatch modalReturned = new CountDownLatch(1);

            app.invokeLater(() -> {
                app.executeModal(dialog);
                modalReturned.countDown();
            });
            waitFor(app::isModalThreadRunning, "modal thread should start");

            // The modal secondary thread does not route commands to
            // onCommand(), so wait on the modal receiver instead of sync().
            int buttonX = running.call(() -> button.getAbsoluteX() + 1);
            int buttonY = running.call(button::getAbsoluteY);
            app.postEvent(mouseEvent(running.backend,
                TMouseEvent.Type.MOUSE_MOTION, buttonX, buttonY, false));
            waitFor(() -> dialog.motions > 0,
                "modal receiver should still consume the motion");

            assertEquals(0, button.enters,
                "desktop widgets must not become hover targets outside a modal");
            assertNull(app.getMouseHoverTarget(),
                "hover should remain outside the desktop while the modal is active");

            app.invokeLater(() -> app.closeWindow(dialog));
            try {
                assertTrue(modalReturned.await(TIMEOUT_MS, TimeUnit.MILLISECONDS),
                    "modal execution should finish after closing the dialog");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(e);
            }
        }
    }

    // ------------------------------------------------------------------------
    // Lifecycle safety -------------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    void removingHoveredWidgetClearsHoverTarget() {
        try (RunningApplication running = new RunningApplication()) {
            TApplication app = running.app;
            TWindow window = running.call(() ->
                new TWindow(app, "test", 0, 0, 40, 10));
            CountingButton a = running.call(() ->
                new CountingButton(window, "A", 1, 1));
            CountingButton b = running.call(() ->
                new CountingButton(window, "B", 20, 1));

            hover(running, a);
            assertSame(a, app.getMouseHoverTarget());

            // Removing the hovered widget must not leave a stale hover reference.
            running.ui(() -> window.remove(a));
            assertNull(app.getMouseHoverTarget(),
                "the hover target must be cleared when the hovered widget is removed");

            // Subsequent movement still works normally.
            hover(running, b);
            assertSame(b, app.getMouseHoverTarget());
            assertEquals(1, b.enters);
        }
    }

    @Test
    void closingHoveredWindowClearsHoverTarget() {
        try (RunningApplication running = new RunningApplication()) {
            TApplication app = running.app;
            TWindow window = running.call(() ->
                new TWindow(app, "test", 0, 0, 40, 10));
            CountingButton a = running.call(() ->
                new CountingButton(window, "A", 1, 1));

            hover(running, a);
            assertSame(a, app.getMouseHoverTarget());

            running.ui(() -> app.closeWindow(window));
            assertNull(app.getMouseHoverTarget(),
                "closing the window must clear a hover target inside it");
        }
    }

    @Test
    void hidingHoveredWidgetClearsHoverTarget() {
        try (RunningApplication running = new RunningApplication()) {
            TApplication app = running.app;
            TWindow window = running.call(() ->
                new TWindow(app, "test", 0, 0, 40, 10));
            CountingButton a = running.call(() ->
                new CountingButton(window, "A", 1, 1));

            hover(running, a);
            assertSame(a, app.getMouseHoverTarget());

            running.ui(() -> a.setVisible(false));

            assertEquals(1, a.exits,
                "hiding the hovered widget must dispatch exit");
            assertNull(app.getMouseHoverTarget(),
                "hiding the hovered widget must clear hover state");
        }
    }

    @Test
    void disablingHoveredHyperlinkDispatchesExit() {
        try (RunningApplication running = new RunningApplication()) {
            TApplication app = running.app;
            TWindow window = running.call(() ->
                new TWindow(app, "test", 0, 0, 40, 10));
            THyperLink link = running.call(() ->
                new THyperLink(window, "link", "http://example.com", 2, 2));

            hover(running, link);
            assertTrue(link.isHover(), "hover starts true after entering");

            running.ui(() -> link.setEnabled(false));

            assertFalse(link.isHover(),
                "disabling the hovered link must dispatch exit and clear hover");
            assertNull(app.getMouseHoverTarget());
        }
    }

    @Test
    void openingMenuClearsWidgetHover() {
        try (RunningApplication running = new RunningApplication()) {
            TApplication app = running.app;
            TWindow window = running.call(() ->
                new TWindow(app, "test", 0, 0, 40, 10));
            THyperLink link = running.call(() ->
                new THyperLink(window, "link", "http://example.com", 2, 2));
            running.ui(() -> app.addMenu(new TMenu(app, 0, 0, "&File")));

            hover(running, link);
            assertTrue(link.isHover(), "hover starts true after entering");

            app.postEvent(new TCommandEvent(running.backend, TCommand.cmMenu));
            running.sync();
            assertFalse(link.isHover(),
                "opening a menu must clear previous widget hover state");
            assertNull(app.getMouseHoverTarget(),
                "opening a menu must clear the hover target");
        }
    }

    // ------------------------------------------------------------------------
    // Helpers ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Move the pointer over the center-left of a widget.
     *
     * @param running the running application
     * @param widget the widget to hover
     */
    private void hover(final RunningApplication running, final TWidget widget) {
        motionOver(running, widget, false);
    }

    /**
     * Drag the pointer (button 1 held) over the center-left of a widget.
     *
     * @param running the running application
     * @param widget the widget to drag over
     */
    private void dragOver(final RunningApplication running,
        final TWidget widget) {

        motionOver(running, widget, true);
    }

    /**
     * Send a MOUSE_MOTION over the center-left of a widget.
     *
     * @param running the running application
     * @param widget the target widget
     * @param mouse1 whether button 1 is held
     */
    private void motionOver(final RunningApplication running,
        final TWidget widget, final boolean mouse1) {

        int absX = running.call(() -> widget.getAbsoluteX() + 1);
        int absY = running.call(widget::getAbsoluteY);
        motion(running, absX, absY, mouse1);
    }

    /**
     * Move the pointer to an absolute position.
     *
     * @param running the running application
     * @param absX absolute column
     * @param absY absolute row
     */
    private void hoverAt(final RunningApplication running, final int absX,
        final int absY) {

        motion(running, absX, absY, false);
    }

    /**
     * Send a MOUSE_MOTION through the application's public event path.
     *
     * @param running the running application
     * @param absX absolute column
     * @param absY absolute row
     * @param mouse1 whether button 1 is held
     */
    private void motion(final RunningApplication running, final int absX,
        final int absY, final boolean mouse1) {

        dispatchMouse(running, TMouseEvent.Type.MOUSE_MOTION, absX, absY, mouse1);
    }

    /**
     * Send a MOUSE_DOWN through the application's public event path.
     *
     * @param running the running application
     * @param widget the target widget
     * @param x relative column
     * @param y relative row
     */
    private void mouseDown(final RunningApplication running,
        final TWidget widget, final int x, final int y) {

        int absX = running.call(() -> widget.getAbsoluteX() + x);
        int absY = running.call(() -> widget.getAbsoluteY() + y);
        dispatchMouse(running, TMouseEvent.Type.MOUSE_DOWN, absX, absY, true);
    }

    /**
     * Send a button 1 MOUSE_UP through the application's public event path.
     *
     * @param running the running application
     * @param widget the widget under the pointer at release
     * @param x relative column
     * @param y relative row
     */
    private void mouseUp(final RunningApplication running,
        final TWidget widget, final int x, final int y) {

        int absX = running.call(() -> widget.getAbsoluteX() + x);
        int absY = running.call(() -> widget.getAbsoluteY() + y);
        dispatchMouse(running, TMouseEvent.Type.MOUSE_UP, absX, absY, true);
    }

    /**
     * Post one mouse event and wait until the primary event thread has
     * dispatched it.
     *
     * @param running the running application
     * @param type the event type
     * @param absX absolute column
     * @param absY absolute row
     * @param mouse1 whether button 1 is held
     */
    private void dispatchMouse(final RunningApplication running,
        final TMouseEvent.Type type, final int absX, final int absY,
        final boolean mouse1) {

        running.app.postEvent(mouseEvent(running.backend, type, absX, absY,
            mouse1));
        running.sync();
    }

    /**
     * Poll until a condition becomes true.
     *
     * @param condition the condition to poll
     * @param message assertion message on timeout
     */
    private static void waitFor(final BooleanSupplier condition,
        final String message) {

        long deadline = System.nanoTime()
            + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MS);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.yield();
        }
        assertTrue(condition.getAsBoolean(), message);
    }

    /**
     * Build a screen-absolute mouse event for application-level routing.
     *
     * @param backend the backend that produced the event
     * @param type the event type
     * @param absX absolute column
     * @param absY absolute row
     * @param mouse1 whether button 1 is held
     * @return the event
     */
    private TMouseEvent mouseEvent(final HeadlessBackend backend,
        final TMouseEvent.Type type, final int absX, final int absY,
        final boolean mouse1) {

        return new TMouseEvent(backend, type, absX, absY, absX, absY, 0, 0,
            mouse1, false, false, false, false, false, false, false);
    }
}
