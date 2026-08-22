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

import static casciian.TKeypress.kbAltC;
import static casciian.TKeypress.kbAltN;
import static casciian.TKeypress.kbAltO;
import static casciian.TKeypress.kbAltY;
import static casciian.TKeypress.kbC;
import static casciian.TKeypress.kbEnter;
import static casciian.TKeypress.kbEsc;
import static casciian.TKeypress.kbN;
import static casciian.TKeypress.kbO;
import static casciian.TKeypress.kbY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for generic Escape/cancel semantics on modal windows and
 * TMessageBox.Result.CLOSED result.
 */
class TWindowCancelTest {

    // -----------------------------------------------------------------------
    // 1. Non-modal TWindow + Escape -> no cancellation
    // -----------------------------------------------------------------------

    @Test
    void nonModalWindowEscapeDoesNotCancel() {
        int[] cancelCalls = {0};
        TWindow window = new TWindow(app(), "test", 0, 0, 40, 10) {
            @Override
            protected boolean onCancel() {
                cancelCalls[0]++;
                return true;
            }
        };
        press(window, kbEsc);
        assertEquals(0, cancelCalls[0], "onCancel must not fire on non-modal window");
    }

    // -----------------------------------------------------------------------
    // 2. Modal TWindow with default onCancel() + Escape -> no close
    // -----------------------------------------------------------------------

    @Test
    void modalWindowDefaultOnCancelDoesNothing() {
        TWindow window = makeModalWindow();
        // Default onCancel() returns false; pressing Escape should not throw.
        press(window, kbEsc);
        assertNotNull(window);
    }

    // -----------------------------------------------------------------------
    // 3. Modal subclass overriding onCancel() + Escape -> onCancel() invoked
    // -----------------------------------------------------------------------

    @Test
    void modalWindowOverridingOnCancelIsInvokedOnEscape() {
        int[] cancelCalls = {0};
        TWindow window = new TWindow(app(), "test", 0, 0, 40, 10,
            TWindow.MODAL) {
            @Override
            protected boolean onCancel() {
                cancelCalls[0]++;
                return true;
            }
        };
        press(window, kbEsc);
        assertEquals(1, cancelCalls[0], "onCancel should fire once for modal Escape");
    }

    // -----------------------------------------------------------------------
    // 4. Focused widget that claims Escape -> widget receives it, onCancel()
    //    not invoked
    // -----------------------------------------------------------------------

    @Test
    void focusedWidgetClaimingEscapeBlocksWindowCancel() {
        int[] cancelCalls = {0};
        TWindow window = new TWindow(app(), "test", 0, 0, 40, 10,
            TWindow.MODAL) {
            @Override
            protected boolean onCancel() {
                cancelCalls[0]++;
                return true;
            }
        };
        int[] widgetEscapes = {0};
        TWidget claimingWidget = new TWidget(window) {
            @Override
            protected boolean receivesKeypressBeforeWindowCancel(
                final TKeypressEvent keypress) {
                return keypress.equals(kbEsc);
            }

            @Override
            public void onKeypress(final TKeypressEvent keypress) {
                if (keypress.equals(kbEsc)) {
                    widgetEscapes[0]++;
                }
            }
        };
        window.activate(claimingWidget);
        press(window, kbEsc);
        assertEquals(0, cancelCalls[0], "onCancel must NOT fire when widget claims Escape");
        assertEquals(1, widgetEscapes[0], "widget must receive Escape");
    }

    // -----------------------------------------------------------------------
    // 5. Escape during window move -> exits move mode, onCancel() not invoked
    // -----------------------------------------------------------------------

    @Test
    void escapeDuringMoveDoesNotInvokeOnCancel() {
        MoveTestWindow moveWindow = new MoveTestWindow(app());
        moveWindow.startMove();
        press(moveWindow, kbEsc);
        assertEquals(0, moveWindow.cancelCount,
            "onCancel must NOT fire when exiting move mode");
        assertFalse(moveWindow.inWindowMoveState(),
            "move mode should be cleared by Escape");
    }

    // -----------------------------------------------------------------------
    // 6. TMessageBox OK
    // -----------------------------------------------------------------------

    @Test
    void messageBoxOkButtonReturnsOk() {
        TMessageBox box = makeBox(TMessageBox.Type.OK);
        activateButton(box, 0);
        assertEquals(TMessageBox.Result.OK, box.getResult());
    }

    @Test
    void messageBoxOkEscapeReturnsClosed() {
        TMessageBox box = makeBox(TMessageBox.Type.OK);
        press(box, kbEsc);
        assertEquals(TMessageBox.Result.CLOSED, box.getResult());
    }

    @Test
    void messageBoxOkInitialResultIsClosed() {
        TMessageBox box = makeBox(TMessageBox.Type.OK);
        assertEquals(TMessageBox.Result.CLOSED, box.getResult());
    }

    // -----------------------------------------------------------------------
    // 7. TMessageBox OKCANCEL
    // -----------------------------------------------------------------------

    @Test
    void messageBoxOkCancelOkButtonReturnsOk() {
        TMessageBox box = makeBox(TMessageBox.Type.OKCANCEL);
        activateButton(box, 0);
        assertEquals(TMessageBox.Result.OK, box.getResult());
    }

    @Test
    void messageBoxOkCancelCancelButtonReturnsCancel() {
        TMessageBox box = makeBox(TMessageBox.Type.OKCANCEL);
        activateButton(box, 1);
        assertEquals(TMessageBox.Result.CANCEL, box.getResult());
    }

    @Test
    void messageBoxOkCancelEscapeReturnsClosed() {
        TMessageBox box = makeBox(TMessageBox.Type.OKCANCEL);
        press(box, kbEsc);
        assertEquals(TMessageBox.Result.CLOSED, box.getResult());
    }

    @Test
    void messageBoxOkCancelInitialResultIsClosed() {
        TMessageBox box = makeBox(TMessageBox.Type.OKCANCEL);
        assertEquals(TMessageBox.Result.CLOSED, box.getResult());
    }

    // -----------------------------------------------------------------------
    // 8. TMessageBox YESNO
    // -----------------------------------------------------------------------

    @Test
    void messageBoxYesNoYesButtonReturnsYes() {
        TMessageBox box = makeBox(TMessageBox.Type.YESNO);
        activateButton(box, 0);
        assertEquals(TMessageBox.Result.YES, box.getResult());
    }

    @Test
    void messageBoxYesNoNoButtonReturnsNo() {
        TMessageBox box = makeBox(TMessageBox.Type.YESNO);
        activateButton(box, 1);
        assertEquals(TMessageBox.Result.NO, box.getResult());
    }

    @Test
    void messageBoxYesNoEscapeReturnsClosed() {
        TMessageBox box = makeBox(TMessageBox.Type.YESNO);
        press(box, kbEsc);
        assertEquals(TMessageBox.Result.CLOSED, box.getResult());
    }

    @Test
    void messageBoxYesNoInitialResultIsClosed() {
        TMessageBox box = makeBox(TMessageBox.Type.YESNO);
        assertEquals(TMessageBox.Result.CLOSED, box.getResult());
    }

    // -----------------------------------------------------------------------
    // 9. TMessageBox YESNOCANCEL
    // -----------------------------------------------------------------------

    @Test
    void messageBoxYesNoCancelYesButtonReturnsYes() {
        TMessageBox box = makeBox(TMessageBox.Type.YESNOCANCEL);
        activateButton(box, 0);
        assertEquals(TMessageBox.Result.YES, box.getResult());
    }

    @Test
    void messageBoxYesNoCancelNoButtonReturnsNo() {
        TMessageBox box = makeBox(TMessageBox.Type.YESNOCANCEL);
        activateButton(box, 1);
        assertEquals(TMessageBox.Result.NO, box.getResult());
    }

    @Test
    void messageBoxYesNoCancelCancelButtonReturnsCancel() {
        TMessageBox box = makeBox(TMessageBox.Type.YESNOCANCEL);
        activateButton(box, 2);
        assertEquals(TMessageBox.Result.CANCEL, box.getResult());
    }

    @Test
    void messageBoxYesNoCancelEscapeReturnsClosed() {
        TMessageBox box = makeBox(TMessageBox.Type.YESNOCANCEL);
        press(box, kbEsc);
        assertEquals(TMessageBox.Result.CLOSED, box.getResult());
    }

    @Test
    void messageBoxYesNoCancelInitialResultIsClosed() {
        TMessageBox box = makeBox(TMessageBox.Type.YESNOCANCEL);
        assertEquals(TMessageBox.Result.CLOSED, box.getResult());
    }

    // -----------------------------------------------------------------------
    // 10. Existing letter/Alt shortcuts return their explicit results
    // -----------------------------------------------------------------------

    @Test
    void shortcutOreturnsOk() {
        TMessageBox box = makeBox(TMessageBox.Type.OK);
        press(box, kbO);
        assertEquals(TMessageBox.Result.OK, box.getResult());
    }

    @Test
    void shortcutAltOreturnsOk() {
        TMessageBox box = makeBox(TMessageBox.Type.OKCANCEL);
        press(box, kbAltO);
        assertEquals(TMessageBox.Result.OK, box.getResult());
    }

    @Test
    void shortcutCreturnsCancel() {
        TMessageBox box = makeBox(TMessageBox.Type.OKCANCEL);
        press(box, kbC);
        assertEquals(TMessageBox.Result.CANCEL, box.getResult());
    }

    @Test
    void shortcutAltCreturnsCancel() {
        TMessageBox box = makeBox(TMessageBox.Type.YESNOCANCEL);
        press(box, kbAltC);
        assertEquals(TMessageBox.Result.CANCEL, box.getResult());
    }

    @Test
    void shortcutYreturnsYes() {
        TMessageBox box = makeBox(TMessageBox.Type.YESNO);
        press(box, kbY);
        assertEquals(TMessageBox.Result.YES, box.getResult());
    }

    @Test
    void shortcutAltYreturnsYes() {
        TMessageBox box = makeBox(TMessageBox.Type.YESNOCANCEL);
        press(box, kbAltY);
        assertEquals(TMessageBox.Result.YES, box.getResult());
    }

    @Test
    void shortcutNreturnsNo() {
        TMessageBox box = makeBox(TMessageBox.Type.YESNO);
        press(box, kbN);
        assertEquals(TMessageBox.Result.NO, box.getResult());
    }

    @Test
    void shortcutAltNreturnsNo() {
        TMessageBox box = makeBox(TMessageBox.Type.YESNOCANCEL);
        press(box, kbAltN);
        assertEquals(TMessageBox.Result.NO, box.getResult());
    }

    // -----------------------------------------------------------------------
    // 11. Explicit Cancel is distinguishable from CLOSED
    // -----------------------------------------------------------------------

    @Test
    void cancelButtonResultIsDistinctFromClosed() {
        TMessageBox cancel = makeBox(TMessageBox.Type.OKCANCEL);
        activateButton(cancel, 1);
        assertEquals(TMessageBox.Result.CANCEL, cancel.getResult());
        assertFalse(cancel.isClosed());
        assertTrue(cancel.isCancel());

        TMessageBox escaped = makeBox(TMessageBox.Type.OKCANCEL);
        press(escaped, kbEsc);
        assertEquals(TMessageBox.Result.CLOSED, escaped.getResult());
        assertTrue(escaped.isClosed());
        assertFalse(escaped.isCancel());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private TApplication app() {
        return new TApplication(new HeadlessBackend());
    }

    private TWindow makeModalWindow() {
        return new TWindow(app(), "test", 0, 0, 40, 10, TWindow.MODAL);
    }

    private TMessageBox makeBox(final TMessageBox.Type type) {
        return new TMessageBox(app(), "title", "caption", type, false);
    }

    private void press(final TWidget widget, final TKeypress keypress) {
        widget.onKeypress(new TKeypressEvent(null, keypress));
    }

    private void activateButton(final TMessageBox box, final int index) {
        List<TButton> buttons = new ArrayList<>();
        for (TWidget child : box.getChildren()) {
            if (child instanceof TButton b) {
                buttons.add(b);
            }
        }
        buttons.get(index).dispatch();
    }

    /**
     * Subclass used to test that move-mode Escape exits move mode without
     * invoking onCancel().
     */
    private static class MoveTestWindow extends TWindow {
        int cancelCount = 0;

        MoveTestWindow(final TApplication app) {
            super(app, "move-test", 0, 0, 40, 10, MODAL);
        }

        void startMove() {
            inWindowMove = true;
        }

        boolean inWindowMoveState() {
            return inWindowMove;
        }

        @Override
        protected boolean onCancel() {
            cancelCount++;
            return true;
        }
    }
}
