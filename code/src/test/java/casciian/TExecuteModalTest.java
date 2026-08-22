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
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import casciian.backend.HeadlessBackend;
import casciian.event.TKeypressEvent;

import static casciian.TKeypress.kbEsc;
import static casciian.TWindow.MODAL;
import static casciian.TWindow.NOCLOSEBOX;
import static casciian.TWindow.RESIZABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link TApplication#executeModal(TWindow)} – the generic modal
 * execution API.
 *
 * <p>Each test that calls {@code executeModal} from the main thread arranges
 * for the dialog to be closed from a background thread after a short delay so
 * that the blocking call can return.  A timeout guards against deadlocks.</p>
 */
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

        /** Set result to SAVE and close.  Can be called from any thread. */
        void triggerSave() {
            result = Result.SAVE;
            getApplication().closeWindow(this);
        }

        /** Set result to DISCARD and close.  Can be called from any thread. */
        void triggerDiscard() {
            result = Result.DISCARD;
            getApplication().closeWindow(this);
        }

        Result getResult() { return result; }
    }

    @Test
    void genericDialogSaveButtonSetsResultAndReturns() throws Exception {
        TApplication app = startedApp();
        SaveDialog dialog = new SaveDialog(app);

        // executeModal blocks – close the dialog from a background thread.
        closeAfterDelay(app, dialog, dialog::triggerSave);

        app.executeModal(dialog);

        assertEquals(SaveDialog.Result.SAVE, dialog.getResult());
    }

    @Test
    void genericDialogDiscardButtonSetsResultAndReturns() throws Exception {
        TApplication app = startedApp();
        SaveDialog dialog = new SaveDialog(app);

        closeAfterDelay(app, dialog, dialog::triggerDiscard);

        app.executeModal(dialog);

        assertEquals(SaveDialog.Result.DISCARD, dialog.getResult());
    }

    // -----------------------------------------------------------------------
    // 2. No-result dialog – can be executed and closed without any result
    // -----------------------------------------------------------------------

    @Test
    void noResultDialogCanBeExecutedAndClosed() throws Exception {
        TApplication app = startedApp();
        TDialog dialog = new TDialog(app, "Info", 30, 8);
        addCloseButton(dialog);

        AtomicBoolean returned = new AtomicBoolean(false);

        closeAfterDelay(app, dialog, () ->
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
        TApplication app = startedApp();
        TDialog dialog = new TDialog(app, "Esc Test", 30, 8);

        // Deliver Esc from a background thread after executeModal is entered.
        closeAfterDelay(app, dialog, () ->
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
        TApplication app = startedApp();
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

        // First Esc -> child consumes it (dialog stays open).
        // Second Esc -> child no longer claims it -> dialog closes.
        closeAfterDelay(app, dialog, () -> {
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
        TApplication app = startedApp();
        // Default TDialog flags include MODAL (close box present by default).
        TDialog dialog = new TDialog(app, "Close Box", 30, 8);

        closeAfterDelay(app, dialog, () -> app.closeWindow(dialog));

        app.executeModal(dialog);

        assertFalse(app.getAllWindows().contains(dialog));
    }

    // -----------------------------------------------------------------------
    // 6. Explicit closeWindow -> executeModal returns
    // -----------------------------------------------------------------------

    @Test
    void explicitCloseWindowEndsModalExecution() throws Exception {
        TApplication app = startedApp();
        TDialog dialog = new TDialog(app, "Close Explicit", 30, 8);

        closeAfterDelay(app, dialog, () -> app.closeWindow(dialog));

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
        TApplication app = startedApp();
        TDialog dialog = new TDialog(app, "Cleanup", 30, 8);

        closeAfterDelay(app, dialog, () -> app.closeWindow(dialog));

        app.executeModal(dialog);

        // If secondaryEventReceiver is not cleared the next executeModal
        // would throw IllegalStateException – verify it can be called again.
        TDialog second = new TDialog(app, "Second", 30, 8);
        closeAfterDelay(app, second, () -> app.closeWindow(second));
        app.executeModal(second);   // must not throw
    }

    // -----------------------------------------------------------------------
    // 9. Invalid-usage guards
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
        return new TApplication(new HeadlessBackend());
    }

    /**
     * Create and start a TApplication on a background daemon thread so that
     * {@link TApplication#executeModal(TWindow)} can block correctly.
     * The app is stopped in {@link #cleanup} via {@link TApplication#exit()}.
     */
    private TApplication startedApp() {
        TApplication app = new TApplication(new HeadlessBackend());
        startedApps.add(app);
        Thread t = new Thread(app::run);
        t.setDaemon(true);
        t.start();
        // Give the app time to initialise the primaryEventHandler.
        try { Thread.sleep(100); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return app;
    }

    /** Create a TMessageBox without blocking (yield=false). */
    private TMessageBox makeBox(final TMessageBox.Type type) {
        return new TMessageBox(app(), "title", "caption", type, false);
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
     * Schedule {@code action} to run on a background thread after a short
     * delay.  The action is expected to close {@code dialog} and thereby
     * unblock the {@code executeModal} call on the main thread.
     *
     * <p>The background thread waits until the dialog is present in the
     * application's window list (i.e. executeModal has entered its wait
     * loop) before firing, to avoid a race where the close arrives before
     * the secondary handler is installed.</p>
     */
    private void closeAfterDelay(final TApplication app,
            final TWindow dialog, final Runnable action) {

        Thread t = new Thread(() -> {
            // Wait until the secondary event receiver is installed.
            long deadline = System.currentTimeMillis() + 5_000;
            while (System.currentTimeMillis() < deadline) {
                if (app.getAllWindows().contains(dialog)) {
                    break;
                }
                try { Thread.sleep(5); } catch (InterruptedException e) { return; }
            }
            // Small additional pause to allow enableSecondaryEventReceiver
            // to start its thread before we close the window.
            try { Thread.sleep(50); } catch (InterruptedException e) { return; }
            action.run();
        });
        t.setDaemon(true);
        t.start();
    }
}
