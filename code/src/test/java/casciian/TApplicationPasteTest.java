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
import casciian.event.TCommandEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TPasteEvent;
import org.junit.jupiter.api.Test;

import static casciian.TCommand.cmPaste;
import static casciian.TCommand.cmSystemPaste;
import static casciian.TKeypress.kbCtrlShiftV;
import static casciian.TWindow.MODAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests terminal paste integration with the application clipboard and command
 * path.
 */
class TApplicationPasteTest {

    private static class ClipboardBackend extends HeadlessBackend {

        private volatile int requestCount;

        @Override
        public void requestClipboardText() {
            requestCount++;
        }
    }

    @Test
    void editMenuMapsCtrlShiftVToSystemPaste() {
        ClipboardBackend backend = new ClipboardBackend();
        backend.setBackend(backend);
        TApplication application = new TApplication(backend);

        application.addEditMenu();

        assertEquals(kbCtrlShiftV, application.getMenuItem(
            casciian.menu.TMenu.MID_SYSTEM_PASTE).getKey());
    }

    @Test
    void systemPasteRequestsClipboardWithoutEditingImmediately()
        throws Exception {

        ClipboardBackend backend = new ClipboardBackend();
        backend.setBackend(backend);
        TApplication application = new TApplication(backend);
        TWindow window = new TWindow(application, "paste", 0, 0, 40, 10);
        TField field = window.addField(1, 1, 30, false);
        application.getClipboard().copyText("old");

        Thread applicationThread = new Thread(application::run);
        applicationThread.start();
        try {
            application.postEvent(new TCommandEvent(backend, cmSystemPaste));
            waitForRequests(backend, 1);

            assertEquals("", field.getText());
            assertEquals("old", application.getClipboard().pasteText());
        } finally {
            application.exit();
            applicationThread.join(2000);
        }
    }

    @Test
    void terminalPasteReplacesClipboardAndUsesPasteCommand() throws Exception {
        HeadlessBackend backend = new HeadlessBackend();
        backend.setBackend(backend);
        TApplication application = new TApplication(backend);
        TWindow window = new TWindow(application, "paste", 0, 0, 40, 10);
        TField field = window.addField(1, 1, 30, false);
        application.getClipboard().copyText("old");
        application.lastUserInputTime = 0;

        Thread applicationThread = new Thread(application::run);
        applicationThread.start();
        try {
            TPasteEvent paste = new TPasteEvent(backend, "hello");
            application.postEvent(paste);
            waitForText(field, "hello");

            assertEquals("hello", application.getClipboard().pasteText());
            assertEquals(paste.getTime().getTime(),
                application.lastUserInputTime);

            application.postEvent(new TCommandEvent(backend, cmPaste));
            waitForText(field, "hellohello");
        } finally {
            application.exit();
            applicationThread.join(2000);
        }

        assertFalse(applicationThread.isAlive());
    }

    @Test
    void shortcutAndPasteResponseUseModalClipboardFlow() throws Exception {
        ClipboardBackend backend = new ClipboardBackend();
        backend.setBackend(backend);
        TApplication application = new TApplication(backend);
        TDialog dialog = new TDialog(application, "paste", 40, 10, MODAL);
        TField field = dialog.addField(1, 1, 30, false);

        Thread applicationThread = new Thread(application::run);
        applicationThread.start();
        try {
            application.invokeLater(() -> application.executeModal(dialog));
            waitForModal(application);

            application.postEvent(new TKeypressEvent(backend, kbCtrlShiftV));
            waitForRequests(backend, 1);
            assertEquals("", field.getText());

            application.postEvent(new TPasteEvent(backend, "modal 世界"));
            waitForText(field, "modal 世界");
            assertEquals("modal 世界",
                application.getClipboard().pasteText());

            application.postEvent(new TCommandEvent(backend, cmPaste));
            waitForText(field, "modal 世界modal 世界");
        } finally {
            application.closeWindow(dialog);
            application.exit();
            applicationThread.join(2000);
        }

        assertFalse(applicationThread.isAlive());
    }

    private void waitForText(final TField field, final String expected)
        throws InterruptedException {

        long deadline = System.currentTimeMillis() + 5000;
        while (!field.getText().equals(expected)
            && (System.currentTimeMillis() < deadline)
        ) {
            Thread.sleep(1);
        }
        assertEquals(expected, field.getText());
    }

    private void waitForRequests(final ClipboardBackend backend,
        final int expected) throws InterruptedException {

        long deadline = System.currentTimeMillis() + 5000;
        while ((backend.requestCount < expected)
            && (System.currentTimeMillis() < deadline)
        ) {
            Thread.sleep(1);
        }
        assertEquals(expected, backend.requestCount);
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
