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

import java.util.List;

import org.junit.jupiter.api.Test;

import casciian.backend.HeadlessBackend;
import casciian.event.TKeypressEvent;

import static casciian.TKeypress.kbEnter;
import static casciian.TKeypress.kbEsc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests modeless window numbering and Window List dialog behavior.
 */
class TWindowListTest {

    @Test
    void modelessWindowsReceiveStableReusableNumbers() {
        TApplication application = app();
        TWindow first = window(application, "one");
        TWindow second = window(application, "two");
        TDialog modal = new TDialog(application, "dialog", 20, 8);

        assertEquals(1, first.getWindowNumber());
        assertEquals(2, second.getWindowNumber());
        assertEquals(0, modal.getWindowNumber());

        application.closeWindow(first);
        application.closeWindow(modal);
        TWindow replacement = window(application, "replacement");

        assertEquals(2, second.getWindowNumber());
        assertEquals(1, replacement.getWindowNumber());
    }

    @Test
    void windowNumberIsDrawnLeftOfZoomBox() {
        TApplication application = app();
        TWindow window = window(application, "one");

        window.draw();

        int numberX = window.getWidth() - 7;
        assertEquals('1', window.getScreen().getCharXY(numberX,
                0).getChar());
    }

    @Test
    void enterOnListAcceptsAndActivatesSelectedWindow() {
        TApplication application = app();
        TWindow first = window(application, "one");
        TWindow second = window(application, "two");
        TWindowList dialog = new TWindowList(application);
        dialog.getWindowList().setSelectedIndex(0);

        press(dialog, kbEnter);

        assertEquals(TWindowList.Result.OK, dialog.getResult());
        assertFalse(application.hasWindow(dialog));
        assertSame(first, application.getActiveWindow());
        assertTrue(application.hasWindow(second));
    }

    @Test
    void escapeCancelsAndRestoresPreviouslyActiveWindow() {
        TApplication application = app();
        window(application, "one");
        TWindow active = window(application, "two");
        TWindowList dialog = new TWindowList(application);

        press(dialog, kbEsc);

        assertEquals(TWindowList.Result.CANCEL, dialog.getResult());
        assertFalse(application.hasWindow(dialog));
        assertSame(active, application.getActiveWindow());
        assertEquals(2, application.windowCount());
    }

    @Test
    void closeRefreshesListAndKeepsDeletedIndexSelected() {
        TApplication application = app();
        TWindow first = window(application, "one");
        TWindow second = window(application, "two");
        TWindow third = window(application, "three");
        TWindowList dialog = new TWindowList(application);
        dialog.getWindowList().setSelectedIndex(1);

        buttons(dialog).get(1).dispatch();

        assertFalse(application.hasWindow(second));
        assertTrue(application.hasWindow(first));
        assertTrue(application.hasWindow(third));
        assertEquals(List.of("one", "three"),
            dialog.getWindowList().getList());
        assertEquals(1, dialog.getWindowList().getSelectedIndex());
        assertEquals("three", dialog.getWindowList().getSelected());
    }

    @Test
    void emptyDialogDisablesCloseButtonAndCancelsNormally() {
        TApplication application = app();
        TWindowList dialog = new TWindowList(application);

        assertFalse(buttons(dialog).get(1).isEnabled());
        buttons(dialog).get(2).dispatch();

        assertEquals(TWindowList.Result.CANCEL, dialog.getResult());
        assertFalse(application.hasWindow(dialog));
    }

    private TApplication app() {
        return new TApplication(new HeadlessBackend());
    }

    private TWindow window(final TApplication application,
        final String title) {

        return new TWindow(application, title, 0, 0, 30, 10);
    }

    private void press(final TWidget widget, final TKeypress keypress) {
        widget.onKeypress(new TKeypressEvent(null, keypress));
    }

    private List<TButton> buttons(final TWidget widget) {
        return widget.getChildren().stream()
            .filter(TButton.class::isInstance)
            .map(TButton.class::cast)
            .toList();
    }
}
