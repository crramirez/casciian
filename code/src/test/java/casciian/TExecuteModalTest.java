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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import casciian.backend.HeadlessBackend;
import casciian.event.TKeypressEvent;

import static casciian.TKeypress.kbEsc;
import static casciian.TWindow.MODAL;
import static casciian.TWindow.NOCLOSEBOX;
import static casciian.TWindow.RESIZABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link TApplication#executeModal(TWindow)} – the generic modal
 * execution API.
 *
 * <p>Each test that calls {@code executeModal} from the main thread schedules
 * its simulated UI action on the modal event-dispatch thread so that the
 * blocking call can return.  A timeout guards against deadlocks.</p>
 */
@Timeout(10)
class TExecuteModalTest {

    /** Apps started via {@link #startedApp()} are stopped in {@link #cleanup}. */
    private final List<TApplication> startedApps = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (TApplication app : startedApps) {
            app.exit();
        }
        startedApps.clear();
    }

    // -----------------------------------------------------------------------
    // 1. Generic custom dialog with its own result enum
    // -----------------------------------------------------------------------

    /**
     * A minimal custom dialog that owns its own result type.
     * It is deliberately not a subclass of TMessageBox.
     */
    private static class SaveDialog extends TDialog {

        enum Result { SAVE, DISCARD, CLOSED }

        private Result result = Result.CLOSED;

        SaveDialog(final TApplication app) {
            super(app, "Save?", 30, 8, MODAL | NOCLOSEBOX);
            TButton save = addButton("&Save", 2, 4, this::triggerSave);
            addButton("&Discard", 10, 4, this::triggerDiscard);
            setDefaultButton(save);
        }

        /** Set result to SAVE and close. */
        void triggerSave() {
            result = Result.SAVE;
            getApplication().closeWindow(this);
        }

        /** Set result to DISCARD and close. */
        void triggerDiscard() {
            result = Result.DISCARD;
            getApplication().closeWindow(this);
        }

        Result getResult() { return result; }
    }

    @Test
    void genericDialogSaveButtonSetsResultAndReturns() throws Exception {
        TApplication app = app();
        SaveDialog dialog = new SaveDialog(app);
        startApp(app);

        runWhenModal(app, dialog::triggerSave);

        app.executeModal(dialog);

        assertEquals(SaveDialog.Result.SAVE, dialog.getResult());
    }

    @Test
    void genericDialogDiscardButtonSetsResultAndReturns() throws Exception {
        TApplication app = app();
        SaveDialog dialog = new SaveDialog(app);
        startApp(app);

        runWhenModal(app, dialog::triggerDiscard);

        app.executeModal(dialog);

        assertEquals(SaveDialog.Result.DISCARD, dialog.getResult());
    }

    // -----------------------------------------------------------------------
    // 2. No-result dialog – can be executed and closed without any result
    // -----------------------------------------------------------------------

    @Test
    void noResultDialogCanBeExecutedAndClosed() throws Exception {
        TApplication app = app();
        TDialog dialog = new TDialog(app, "Info", 30, 8);
        addCloseButton(dialog);
        startApp(app);

        AtomicBoolean returned = new AtomicBoolean(false);

        runWhenModal(app, () ->
            app.closeWindow(dialog));

        app.executeModal(dialog);
        returned.set(true);

        assertTrue(returned.get(), "executeModal must return after dialog closes");
        assertFalse(app.getAllWindows().contains(dialog));
    }

    // -----------------------------------------------------------------------
    // 3. Escape -> onCancel() -> dialog closes -> executeModal returns
    // -----------------------------------------------------------------------

    @Test
    void escapeTriggersOnCancelAndModalReturns() throws Exception {
        TApplication app = app();
        TDialog dialog = new TDialog(app, "Esc Test", 30, 8);
        startApp(app);

        runWhenModal(app, () ->
            dialog.onKeypress(new TKeypressEvent(null, kbEsc)));

        app.executeModal(dialog);

        assertFalse(app.getAllWindows().contains(dialog),
            "Dialog must be closed after Esc");
    }

    // -----------------------------------------------------------------------
    // 4. Child widget that claims Esc keeps dialog open; a second Esc closes
    // -----------------------------------------------------------------------

    @Test
    void childConsumingEscKeepsDialogOpen() throws Exception {
        TApplication app = app();
        TDialog dialog = new TDialog(app, "Esc Child", 30, 8);

        int[] childEscs = {0};
        TWidget claimingChild = new TWidget(dialog) {
            @Override
            protected boolean receivesKeypressBeforeWindowCancel(
                    final TKeypressEvent kp) {
                return kp.equals(kbEsc) && childEscs[0] < 1;
            }
            @Override
            public void onKeypress(final TKeypressEvent kp) {
                if (kp.equals(kbEsc)) {
                    childEscs[0]++;
                }
            }
        };
        dialog.activate(claimingChild);
        startApp(app);

        // First Esc -> child consumes it (dialog stays open).
        // Second Esc -> child no longer claims it -> dialog closes.
        runWhenModal(app, () -> {
            dialog.onKeypress(new TKeypressEvent(null, kbEsc)); // child claims
            dialog.onKeypress(new TKeypressEvent(null, kbEsc)); // dialog closes
        });

        app.executeModal(dialog);

        assertEquals(1, childEscs[0], "Child must have received exactly one Esc");
        assertFalse(app.getAllWindows().contains(dialog),
            "Dialog must be closed after second Esc");
    }

    // -----------------------------------------------------------------------
    // 5. Close box -> dialog closes -> executeModal returns
    // -----------------------------------------------------------------------

    @Test
    void closeBoxEndsModalExecution() throws Exception {
        TApplication app = app();
        // Default TDialog flags include MODAL (close box present by default).
        TDialog dialog = new TDialog(app, "Close Box", 30, 8);
        startApp(app);

        runWhenModal(app, () -> app.closeWindow(dialog));

        app.executeModal(dialog);

        assertFalse(app.getAllWindows().contains(dialog));
    }

    // -----------------------------------------------------------------------
    // 6. Explicit closeWindow -> executeModal returns
    // -----------------------------------------------------------------------

    @Test
    void explicitCloseWindowEndsModalExecution() throws Exception {
        TApplication app = app();
        TDialog dialog = new TDialog(app, "Close Explicit", 30, 8);
        startApp(app);

        runWhenModal(app, () -> app.closeWindow(dialog));

        app.executeModal(dialog);

        assertFalse(app.getAllWindows().contains(dialog));
    }

    // -----------------------------------------------------------------------
    // 7. TMessageBox semantics preserved through executeModal
    // -----------------------------------------------------------------------

    @Test
    void messageBoxOkButtonReturnsOk() {
        TMessageBox box = makeBox(TMessageBox.Type.OK);
        buttons(box).get(0).dispatch();
        assertEquals(TMessageBox.Result.OK, box.getResult());
    }

    @Test
    void messageBoxCancelButtonReturnsCancel() {
        TMessageBox box = makeBox(TMessageBox.Type.OKCANCEL);
        buttons(box).get(1).dispatch();
        assertEquals(TMessageBox.Result.CANCEL, box.getResult());
    }

    @Test
    void messageBoxYesButtonReturnsYes() {
        TMessageBox box = makeBox(TMessageBox.Type.YESNO);
        buttons(box).get(0).dispatch();
        assertEquals(TMessageBox.Result.YES, box.getResult());
    }

    @Test
    void messageBoxNoButtonReturnsNo() {
        TMessageBox box = makeBox(TMessageBox.Type.YESNO);
        buttons(box).get(1).dispatch();
        assertEquals(TMessageBox.Result.NO, box.getResult());
    }

    @Test
    void messageBoxEscapeReturnsClosed() {
        TMessageBox box = makeBox(TMessageBox.Type.OKCANCEL);
        box.onKeypress(new TKeypressEvent(null, kbEsc));
        assertEquals(TMessageBox.Result.CLOSED, box.getResult());
    }

    @Test
    void messageBoxInitialResultIsClosed() {
        TMessageBox box = makeBox(TMessageBox.Type.OKCANCEL);
        assertEquals(TMessageBox.Result.CLOSED, box.getResult());
    }

    @Test
    void cancelAndClosedAreDistinct() {
        TMessageBox escaped = makeBox(TMessageBox.Type.OKCANCEL);
        escaped.onKeypress(new TKeypressEvent(null, kbEsc));
        assertEquals(TMessageBox.Result.CLOSED, escaped.getResult());
        assertTrue(escaped.isClosed());
        assertFalse(escaped.isCancel());

        TMessageBox cancelled = makeBox(TMessageBox.Type.OKCANCEL);
        buttons(cancelled).get(1).dispatch();
        assertEquals(TMessageBox.Result.CANCEL, cancelled.getResult());
        assertTrue(cancelled.isCancel());
        assertFalse(cancelled.isClosed());
    }

    // -----------------------------------------------------------------------
    // 8. State cleanup after modal execution
    // -----------------------------------------------------------------------

    @Test
    void secondaryEventReceiverClearedAfterModalReturns() throws Exception {
        TApplication app = app();
        TDialog dialog = new TDialog(app, "Cleanup", 30, 8);
        TDialog second = new TDialog(app, "Second", 30, 8);
        startApp(app);

        AtomicReference<Thread> primaryThread = new AtomicReference<>();
        app.invokeAndWait(() -> primaryThread.set(Thread.currentThread()));
        runWhenModal(app, () -> app.closeWindow(dialog));

        app.executeModal(dialog);

        // If secondaryEventReceiver is not cleared the next executeModal
        // would throw IllegalStateException – verify it can be called again.
        runWhenModal(app, () -> app.closeWindow(second));
        app.executeModal(second);   // must not throw

        AtomicReference<Thread> resumedThread = new AtomicReference<>();
        app.invokeAndWait(() -> resumedThread.set(Thread.currentThread()));
        assertSame(primaryThread.get(), resumedThread.get(),
            "Primary event-dispatch thread must resume after modal cleanup");
    }

    // -----------------------------------------------------------------------
    // 9. Event-dispatch threading
    // -----------------------------------------------------------------------

    @Test
    void invokeLaterRunsOnEventDispatchThread() throws Exception {
        TApplication app = startedApp();
        CountDownLatch completed = new CountDownLatch(1);
        AtomicBoolean eventDispatchThread = new AtomicBoolean(false);

        app.invokeLater(() -> {
            eventDispatchThread.set(app.isEventDispatchThread());
            completed.countDown();
        });

        assertTrue(completed.await(2, TimeUnit.SECONDS));
        assertTrue(eventDispatchThread.get());
    }

    @Test
    void invokeAndWaitFromForeignThreadBlocksUntilCommandCompletes()
            throws Exception {

        TApplication app = startedApp();
        CountDownLatch commandStarted = new CountDownLatch(1);
        CountDownLatch releaseCommand = new CountDownLatch(1);
        CountDownLatch callerReturned = new CountDownLatch(1);
        AtomicBoolean eventDispatchThread = new AtomicBoolean(false);

        Thread caller = new Thread(() -> {
            app.invokeAndWait(() -> {
                eventDispatchThread.set(app.isEventDispatchThread());
                commandStarted.countDown();
                try {
                    releaseCommand.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            callerReturned.countDown();
        });
        caller.setDaemon(true);
        caller.start();

        assertTrue(commandStarted.await(2, TimeUnit.SECONDS));
        assertFalse(callerReturned.await(100, TimeUnit.MILLISECONDS));
        releaseCommand.countDown();
        assertTrue(callerReturned.await(2, TimeUnit.SECONDS));
        assertTrue(eventDispatchThread.get());
    }

    @Test
    void invokeAndWaitFromEventDispatchThreadExecutesDirectly()
            throws Exception {

        TApplication app = startedApp();
        AtomicBoolean nestedCommandRan = new AtomicBoolean(false);

        app.invokeAndWait(() -> {
            assertTrue(app.isEventDispatchThread());
            app.invokeAndWait(() -> nestedCommandRan.set(true));
        });

        assertTrue(nestedCommandRan.get());
    }

    @Test
    void closeWindowFromForeignThreadIsSynchronous() throws Exception {
        TApplication app = app();
        AtomicBoolean closeOnEventDispatchThread = new AtomicBoolean(false);
        TWindow window = new TWindow(app, "Close", 0, 0, 20, 5) {
            @Override
            public void onClose() {
                closeOnEventDispatchThread.set(
                    app.isEventDispatchThread());
            }
        };
        startApp(app);

        app.closeWindow(window);

        assertFalse(app.getAllWindows().contains(window));
        assertTrue(closeOnEventDispatchThread.get(),
            "onClose must complete on an event-dispatch thread");
    }

    @Test
    void executeModalFromForeignThreadUsesModalEventThread()
            throws Exception {

        TApplication app = app();
        TDialog dialog = new TDialog(app, "Foreign", 30, 8);
        startApp(app);
        AtomicBoolean actionOnEventDispatchThread = new AtomicBoolean(false);

        runWhenModal(app, () -> {
            actionOnEventDispatchThread.set(app.isEventDispatchThread());
            app.closeWindow(dialog);
        });
        app.executeModal(dialog);

        assertTrue(actionOnEventDispatchThread.get());
        assertFalse(app.isModalThreadRunning());
        assertFalse(app.getAllWindows().contains(dialog));
    }

    @Test
    void queuedCloseRemainsAvailableToModalEventThread() throws Exception {
        TApplication app = app();
        TDialog dialog = new TDialog(app, "Queued Close", 30, 8);
        startApp(app);
        CountDownLatch dispatchBlocked = new CountDownLatch(1);
        CountDownLatch releaseDispatch = new CountDownLatch(1);
        CountDownLatch modalReturned = new CountDownLatch(1);

        app.invokeLater(() -> {
            dispatchBlocked.countDown();
            try {
                releaseDispatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        assertTrue(dispatchBlocked.await(2, TimeUnit.SECONDS));

        app.invokeLater(() -> {
            app.executeModal(dialog);
            modalReturned.countDown();
        });
        app.invokeLater(() -> app.closeWindow(dialog));
        releaseDispatch.countDown();

        assertTrue(modalReturned.await(2, TimeUnit.SECONDS));
        assertFalse(app.getAllWindows().contains(dialog));
    }

    @Test
    @Timeout(40)
    void repeatedForeignThreadModalExecutionRemainsStable() throws Exception {
        TApplication app = startedApp();

        for (int i = 0; i < 25; i++) {
            AtomicReference<TDialog> dialogReference = new AtomicReference<>();
            app.invokeAndWait(() -> dialogReference.set(
                new TDialog(app, "Stress", 30, 8)));
            TDialog dialog = dialogReference.get();

            runWhenModal(app, () -> app.closeWindow(dialog));
            app.executeModal(dialog);

            assertFalse(app.isModalThreadRunning());
            assertFalse(app.getAllWindows().contains(dialog));
            assertTrue(app.isRunning(),
                "Event-handler exception stopped the application at iteration "
                    + i);
        }
    }

    // -----------------------------------------------------------------------
    // 10. Invalid-usage guards
    // -----------------------------------------------------------------------

    @Test
    void executeModalNullThrows() {
        TApplication app = app();
        assertThrows(IllegalArgumentException.class,
            () -> app.executeModal(null));
    }

    @Test
    void executeModalModelessWindowThrows() {
        TApplication app = app();
        TDialog modeless = new TDialog(app, "Modeless", 30, 8, RESIZABLE);
        assertFalse(modeless.isModal());
        assertThrows(IllegalArgumentException.class,
            () -> app.executeModal(modeless));
    }

    @Test
    void executeModalWindowFromOtherAppThrows() {
        TApplication app1 = app();
        TApplication app2 = app();
        TDialog fromApp1 = new TDialog(app1, "Foreign", 30, 8);
        assertThrows(IllegalArgumentException.class,
            () -> app2.executeModal(fromApp1));
    }

    @Test
    void executeModalAlreadyClosedWindowThrows() {
        TApplication app = app();
        TDialog dialog = new TDialog(app, "Closed", 30, 8);
        app.closeWindow(dialog);
        assertFalse(app.getAllWindows().contains(dialog));
        assertThrows(IllegalArgumentException.class,
            () -> app.executeModal(dialog));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Create a TApplication without starting its event loop. */
    private TApplication app() {
        HeadlessBackend backend = new HeadlessBackend();
        backend.setBackend(backend);
        return new TApplication(backend);
    }

    /**
     * Create and start a TApplication.
     */
    private TApplication startedApp() throws InterruptedException {
        return startApp(app());
    }

    /**
     * Start a TApplication on a background daemon thread and wait until its
     * event-dispatch thread has processed a command.
     */
    private TApplication startApp(final TApplication app)
            throws InterruptedException {

        startedApps.add(app);
        Thread t = new Thread(app::run);
        t.setDaemon(true);
        t.start();
        CountDownLatch started = new CountDownLatch(1);
        app.invokeLater(started::countDown);
        assertTrue(started.await(2, TimeUnit.SECONDS),
            "Application event-dispatch thread did not start");
        return app;
    }

    /** Create a TMessageBox without executing the modal loop. */
    private TMessageBox makeBox(final TMessageBox.Type type) {
        return new TMessageBox(app(), "title", "caption", type);
    }

    private java.util.List<TButton> buttons(final TWidget widget) {
        java.util.List<TButton> list = new java.util.ArrayList<>();
        for (TWidget child : widget.getChildren()) {
            if (child instanceof TButton b) {
                list.add(b);
            }
        }
        return list;
    }

    /** Add a simple close button to a dialog. */
    private void addCloseButton(final TDialog dialog) {
        dialog.addButton("&Close", 2, 4, () ->
            dialog.getApplication().closeWindow(dialog));
    }

    /**
     * Schedule {@code action} to run after the secondary modal handler starts.
     * If the primary handler sees the command first, it requeues the command;
     * the primary then yields and the secondary handler runs it.
     */
    private void runWhenModal(final TApplication app, final Runnable action) {
        app.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (app.isModalThreadRunning()) {
                    action.run();
                } else {
                    app.invokeLater(this);
                }
            }
        });
    }
}
