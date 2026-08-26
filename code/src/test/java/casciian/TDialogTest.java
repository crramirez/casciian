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

import org.junit.jupiter.api.Test;

import casciian.backend.HeadlessBackend;
import casciian.event.TKeypressEvent;

import static casciian.TKeypress.kbEnter;
import static casciian.TKeypress.kbEsc;
import static casciian.TWindow.MODAL;
import static casciian.TWindow.NOCLOSEBOX;
import static casciian.TWindow.RESIZABLE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for TDialog default-flags, onCancel(), and migrated dialog classes.
 */
class TDialogTest {

    // -----------------------------------------------------------------------
    // 1. Default constructor produces a modal dialog
    // -----------------------------------------------------------------------

    @Test
    void defaultConstructorProducesModalDialog() {
        TDialog dialog = new TDialog(app(), "test", 40, 10);
        assertTrue(dialog.isModal(),
            "TDialog default constructor must produce a modal dialog");
    }

    // -----------------------------------------------------------------------
    // 2. Explicit flags are respected exactly – RESIZABLE only, not MODAL
    // -----------------------------------------------------------------------

    @Test
    void explicitResizableFlagIsNotForcedModal() {
        TDialog dialog = new TDialog(app(), "test", 40, 10, RESIZABLE);
        assertFalse(dialog.isModal(),
            "Explicit RESIZABLE flag must not silently add MODAL");
        assertTrue(dialog.isResizable());
    }

    // -----------------------------------------------------------------------
    // 3. MODAL | RESIZABLE flags work
    // -----------------------------------------------------------------------

    @Test
    void modalAndResizableFlagsWork() {
        TDialog dialog = new TDialog(app(), "test", 0, 0, 40, 10,
            MODAL | RESIZABLE);
        assertTrue(dialog.isModal());
        assertTrue(dialog.isResizable());
    }

    // -----------------------------------------------------------------------
    // 4. MODAL | NOCLOSEBOX flags work
    // -----------------------------------------------------------------------

    @Test
    void modalAndNoCloseBoxFlagsWork() {
        TDialog dialog = new TDialog(app(), "test", 0, 0, 40, 10,
            MODAL | NOCLOSEBOX);
        assertTrue(dialog.isModal());
        // NOCLOSEBOX flag means no close box, so hasCloseBox() must return false.
        assertFalse(dialog.hasCloseBox());
    }

    // -----------------------------------------------------------------------
    // 5. Modal TDialog + Escape invokes default onCancel() and closes
    // -----------------------------------------------------------------------

    @Test
    void modalDialogEscapeInvokesOnCancelAndCloses() {
        TApplication application = app();
        TDialog dialog = new TDialog(application, "test", 40, 10);
        press(dialog, kbEsc);
        // Window is removed from application after closeWindow()
        assertFalse(application.getAllWindows().contains(dialog),
            "Dialog must be removed after Escape-triggered onCancel()");
    }

    // -----------------------------------------------------------------------
    // 6. Subclass returning false from onCancel() does not close on Escape
    // -----------------------------------------------------------------------

    @Test
    void subclassReturningFalseFromOnCancelPreventsClose() {
        TApplication application = app();
        TDialog dialog = new TDialog(application, "test", 40, 10) {
            @Override
            protected boolean onCancel() {
                return false;
            }
        };
        press(dialog, kbEsc);
        assertTrue(application.getAllWindows().contains(dialog),
            "Dialog must remain open when onCancel() returns false");
    }

    // -----------------------------------------------------------------------
    // 7. Modeless TDialog does not get modal Escape cancellation
    // -----------------------------------------------------------------------

    @Test
    void modelessDialogEscapeDoesNotClose() {
        TApplication application = app();
        TDialog dialog = new TDialog(application, "test", 40, 10, RESIZABLE);
        press(dialog, kbEsc);
        // Non-modal: TWindow guard prevents onCancel() from being invoked.
        assertTrue(application.getAllWindows().contains(dialog),
            "Modeless dialog must not close on Escape");
    }

    // -----------------------------------------------------------------------
    // 8. Default button works unchanged in TDialog
    // -----------------------------------------------------------------------

    @Test
    void defaultButtonWorksInTDialog() {
        TDialog dialog = new TDialog(app(), "test", 40, 10);
        int[] activations = new int[1];
        TButton ok = new TButton(dialog, "O&K", 1, 3, new TAction() {
            public void DO() {
                activations[0]++;
            }
        });
        dialog.setDefaultButton(ok);
        TField field = new TField(dialog, 1, 1, 12, false, "");
        dialog.activate(field);

        press(dialog, kbEnter);

        assertEquals(1, activations[0]);
    }

    // -----------------------------------------------------------------------
    // 9. TMessageBox: Enter->OK, Escape->CLOSED, Cancel->CANCEL
    // -----------------------------------------------------------------------

    @Test
    void messageBoxEnterOnDefaultButtonReturnsOk() {
        TMessageBox box = makeBox(TMessageBox.Type.OK);
        press(box, kbEnter);
        assertEquals(TMessageBox.Result.OK, box.getResult());
    }

    @Test
    void messageBoxEscapeReturnsClosed() {
        TMessageBox box = makeBox(TMessageBox.Type.OKCANCEL);
        press(box, kbEsc);
        assertEquals(TMessageBox.Result.CLOSED, box.getResult());
        assertTrue(box.isClosed());
        assertFalse(box.isCancel());
    }

    @Test
    void messageBoxCancelButtonReturnsCancel() {
        TMessageBox box = makeBox(TMessageBox.Type.OKCANCEL);
        buttons(box).get(1).dispatch();
        assertEquals(TMessageBox.Result.CANCEL, box.getResult());
        assertTrue(box.isCancel());
        assertFalse(box.isClosed());
    }

    @Test
    void messageBoxIsInstanceOfTDialog() {
        assertInstanceOf(TDialog.class, makeBox(TMessageBox.Type.OK));
    }



    // -----------------------------------------------------------------------
    // 11. Migrated configuration dialogs retain modality
    // -----------------------------------------------------------------------

    @Test
    void editColorThemeWindowIsModalDialog() {
        TEditColorThemeWindow w = new TEditColorThemeWindow(app());
        assertTrue(w.isModal());
        assertInstanceOf(TDialog.class, w);
    }

    @Test
    void editDesktopStyleWindowIsModalDialog() {
        TEditDesktopStyleWindow w = new TEditDesktopStyleWindow(app());
        assertTrue(w.isModal());
        assertInstanceOf(TDialog.class, w);
    }

    @Test
    void exceptionDialogIsModalDialog() {
        TExceptionDialog w = new TExceptionDialog(app(),
            new RuntimeException("test"));
        assertTrue(w.isModal());
        assertInstanceOf(TDialog.class, w);
    }

    // -----------------------------------------------------------------------
    // 12. Existing TWindow remains unchanged (TDialog is additive)
    // -----------------------------------------------------------------------

    @Test
    void existingTWindowIsNotATDialog() {
        TWindow window = new TWindow(app(), "test", 0, 0, 40, 10);
        assertFalse(window instanceof TDialog,
            "Plain TWindow must not be a TDialog");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private TApplication app() {
        return new TApplication(new HeadlessBackend());
    }

    private TMessageBox makeBox(final TMessageBox.Type type) {
        return new TMessageBox(app(), "title", "caption", type);
    }

    private void press(final TWidget widget, final TKeypress keypress) {
        widget.onKeypress(new TKeypressEvent(null, keypress));
    }

    private List<TButton> buttons(final TWidget widget) {
        List<TButton> list = new ArrayList<>();
        for (TWidget child : widget.getChildren()) {
            if (child instanceof TButton b) {
                list.add(b);
            }
        }
        return list;
    }
}
