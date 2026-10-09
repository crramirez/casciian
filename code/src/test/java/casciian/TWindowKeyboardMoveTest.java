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
import casciian.event.TCommandEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TMenuEvent;
import casciian.layout.LayoutManager;
import casciian.menu.TMenu;

import static casciian.TCommand.cmWindowMove;
import static casciian.TKeypress.kbCtrlF5;
import static casciian.TKeypress.kbDown;
import static casciian.TKeypress.kbEnter;
import static casciian.TKeypress.kbEsc;
import static casciian.TKeypress.kbRight;
import static casciian.TKeypress.kbShiftDown;
import static casciian.TKeypress.kbShiftRight;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests keyboard-driven window movement and sizing.
 */
class TWindowKeyboardMoveTest {

    @Test
    void escapeRestoresOriginalGeometry() {
        TestWindow window = window();
        int originalX = window.getX();
        int originalY = window.getY();
        int originalWidth = window.getWidth();
        int originalHeight = window.getHeight();

        press(window, kbCtrlF5);
        press(window, kbRight);
        press(window, kbDown);
        press(window, kbShiftRight);
        press(window, kbShiftDown);
        press(window, kbEsc);

        assertEquals(originalX, window.getX());
        assertEquals(originalY, window.getY());
        assertEquals(originalWidth, window.getWidth());
        assertEquals(originalHeight, window.getHeight());
        assertEquals(0, window.cancelCount);
    }

    @Test
    void enterKeepsChangedGeometry() {
        TestWindow window = window();
        int originalX = window.getX();
        int originalY = window.getY();
        int originalWidth = window.getWidth();
        int originalHeight = window.getHeight();

        press(window, kbCtrlF5);
        press(window, kbRight);
        press(window, kbDown);
        press(window, kbShiftRight);
        press(window, kbShiftDown);
        press(window, kbEnter);

        assertEquals(originalX + 1, window.getX());
        assertEquals(originalY + 1, window.getY());
        assertEquals(originalWidth + 1, window.getWidth());
        assertEquals(originalHeight + 1, window.getHeight());
        assertEquals(0, window.cancelCount);
    }

    @Test
    void modalWindowShowsMovingFrameDuringKeyboardMove() {
        TestWindow window = window();

        press(window, kbCtrlF5);

        assertEquals(
            window.getTheme().getColor("twindow.border.modal.windowmove"),
            window.getBorder());
        assertNotEquals(window.getBorderStyleModal(),
            window.getBorderStyle());
    }

    @Test
    void escapeRestoresGeometryAfterWindowMoveCommand() {
        TestWindow window = window();
        int originalX = window.getX();

        window.onCommand(new TCommandEvent(null, cmWindowMove));
        press(window, kbRight);
        press(window, kbEsc);

        assertEquals(originalX, window.getX());
    }

    @Test
    void repeatedWindowMoveCommandPreservesOriginalGeometry() {
        TestWindow window = window();
        int originalX = window.getX();

        window.onCommand(new TCommandEvent(null, cmWindowMove));
        press(window, kbRight);
        window.onCommand(new TCommandEvent(null, cmWindowMove));
        press(window, kbRight);
        press(window, kbEsc);

        assertEquals(originalX, window.getX());
    }

    @Test
    void moveOnlyCancellationDoesNotResizeLayout() {
        TestWindow window = window();
        LayoutManager layout = mock(LayoutManager.class);
        window.setLayoutManager(layout);

        press(window, kbCtrlF5);
        press(window, kbRight);
        clearInvocations(layout);
        press(window, kbEsc);

        verifyNoInteractions(layout);
    }

    @Test
    void escapeRestoresGeometryAfterWindowMoveMenuItem() {
        TestWindow window = window();
        int originalX = window.getX();

        window.onMenu(new TMenuEvent(null, TMenu.MID_WINDOW_MOVE));
        press(window, kbRight);
        press(window, kbEsc);

        assertEquals(originalX, window.getX());
    }

    @Test
    void directTreeViewOnKeypressPreservesKeyboardResize() {
        TTreeViewWindow window = new TTreeViewWindow(
            new TApplication(new HeadlessBackend()), "tree", 10, 5, 30, 10,
            TWindow.RESIZABLE);
        window.setTreeRoot(new TTreeItem(window.getTreeView(), "root", true));
        int originalWidth = window.getWidth();

        window.onCommand(new TCommandEvent(null, cmWindowMove));
        window.onKeypress(new TKeypressEvent(null, kbShiftRight));

        assertEquals(originalWidth + 1, window.getWidth());
    }

    @Test
    void subclassOnKeypressDoesNotStealKeysDuringKeyboardMove() {
        KeyStealingWindow window = new KeyStealingWindow(
            new TApplication(new HeadlessBackend()));
        int originalX = window.getX();
        int originalY = window.getY();
        int originalWidth = window.getWidth();
        int originalHeight = window.getHeight();

        dispatch(window, kbCtrlF5);
        dispatch(window, kbRight);
        dispatch(window, kbDown);
        dispatch(window, kbShiftRight);
        dispatch(window, kbShiftDown);
        dispatch(window, kbEnter);

        assertEquals(originalX + 1, window.getX());
        assertEquals(originalY + 1, window.getY());
        assertEquals(originalWidth + 1, window.getWidth());
        assertEquals(originalHeight + 1, window.getHeight());
        assertEquals(1, window.stolenCount);

        dispatch(window, kbRight);
        assertEquals(2, window.stolenCount);
        assertEquals(originalX + 1, window.getX());
    }

    private TestWindow window() {
        return new TestWindow(new TApplication(new HeadlessBackend()));
    }

    private void press(final TWidget widget, final TKeypress keypress) {
        widget.onKeypress(new TKeypressEvent(null, keypress));
    }

    private void dispatch(final TWidget widget, final TKeypress keypress) {
        widget.handleEvent(new TKeypressEvent(null, keypress));
    }

    /**
     * A window whose onKeypress() consumes arrow keys before TWindow sees
     * them, as TImageWindow does.
     */
    private static class KeyStealingWindow extends TWindow {
        private int stolenCount;

        KeyStealingWindow(final TApplication app) {
            super(app, "key-stealing", 40, 10, RESIZABLE);
        }

        @Override
        public void onKeypress(final TKeypressEvent keypress) {
            if (keypress.equals(kbCtrlF5)) {
                stolenCount++;
                super.onKeypress(keypress);
                return;
            }
            if (keypress.equals(kbRight) || keypress.equals(kbDown)
                || keypress.equals(kbShiftRight)
                || keypress.equals(kbShiftDown)
                || keypress.equals(kbEnter)
            ) {
                stolenCount++;
                return;
            }
            super.onKeypress(keypress);
        }
    }

    private static class TestWindow extends TWindow {
        private int cancelCount;

        TestWindow(final TApplication app) {
            super(app, "keyboard-move", 40, 10, MODAL | RESIZABLE);
        }

        @Override
        protected boolean onCancel() {
            cancelCount++;
            return true;
        }
    }
}
