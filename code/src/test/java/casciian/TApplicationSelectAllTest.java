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

import casciian.backend.HeadlessBackend;
import casciian.event.TKeypressEvent;
import casciian.menu.TMenu;
import casciian.menu.TMenuItem;
import org.junit.jupiter.api.Test;

import static casciian.TKeypress.kbCtrlA;
import static casciian.TWindow.MODAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests Select All integration with the standard Edit menu and text widgets.
 */
class TApplicationSelectAllTest {

    @Test
    void editMenuPlacesSelectAllAfterSystemPaste() {
        HeadlessBackend backend = new HeadlessBackend();
        backend.setBackend(backend);
        TApplication application = new TApplication(backend);

        TMenu editMenu = application.addEditMenu();
        TMenuItem systemPaste = application.getMenuItem(TMenu.MID_SYSTEM_PASTE);
        TMenuItem selectAll = application.getMenuItem(TMenu.MID_SELECT_ALL);

        assertEquals(kbCtrlA, selectAll.getKey());
        assertTrue(selectAll.getY() > systemPaste.getY());
        assertEquals(editMenu.getChildren().size() - 1,
            editMenu.getChildren().indexOf(selectAll));
    }

    @Test
    void selectAllMenuTracksTheActiveEditMenuUser() throws Exception {
        HeadlessBackend backend = new HeadlessBackend();
        backend.setBackend(backend);
        TApplication application = new TApplication(backend);
        application.addEditMenu();
        TWindow window = new TWindow(application, "select", 0, 0, 40, 10);
        TField field = window.addField(1, 1, 30, false, "hello");
        TMenuItem selectAll = application.getMenuItem(TMenu.MID_SELECT_ALL);

        Thread applicationThread = new Thread(application::run);
        applicationThread.start();
        try {
            waitForEnabled(selectAll, true);

            field.setEnabled(false);
            application.postEvent(new TKeypressEvent(backend,
                TKeypress.kbRight));
            waitForEnabled(selectAll, false);
        } finally {
            application.exit();
            applicationThread.join(2000);
        }

        assertFalse(applicationThread.isAlive());
    }

    @Test
    void ctrlASelectsTextInModalWindow() throws Exception {
        HeadlessBackend backend = new HeadlessBackend();
        backend.setBackend(backend);
        TApplication application = new TApplication(backend);
        application.addEditMenu();
        TDialog dialog = new TDialog(application, "select", 40, 10, MODAL);
        TField field = dialog.addField(1, 1, 30, false, "modal text");

        Thread applicationThread = new Thread(application::run);
        applicationThread.start();
        try {
            application.invokeLater(() -> application.executeModal(dialog));
            waitForModal(application);

            application.postEvent(new TKeypressEvent(backend, kbCtrlA));
            waitForSelection(field, "modal text");
        } finally {
            application.closeWindow(dialog);
            application.exit();
            applicationThread.join(2000);
        }

        assertFalse(applicationThread.isAlive());
    }

    private void waitForEnabled(final TMenuItem item, final boolean enabled)
        throws InterruptedException {

        long deadline = System.currentTimeMillis() + 5000;
        while ((item.isEnabled() != enabled)
            && (System.currentTimeMillis() < deadline)
        ) {
            Thread.sleep(1);
        }
        assertEquals(enabled, item.isEnabled());
    }

    private void waitForSelection(final TTextBase field,
        final String expected) throws InterruptedException {

        long deadline = System.currentTimeMillis() + 5000;
        while (!expected.equals(field.getSelection())
            && (System.currentTimeMillis() < deadline)
        ) {
            Thread.sleep(1);
        }
        assertEquals(expected, field.getSelection());
    }

    private void waitForModal(final TApplication application)
        throws InterruptedException {

        long deadline = System.currentTimeMillis() + 5000;
        while (!application.isModalThreadRunning()
            && (System.currentTimeMillis() < deadline)
        ) {
            Thread.sleep(1);
        }
        assertTrue(application.isModalThreadRunning());
    }
}
