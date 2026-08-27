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
import casciian.event.TPasteEvent;
import org.junit.jupiter.api.Test;

import static casciian.TCommand.cmPaste;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests terminal paste integration with the application clipboard and command
 * path.
 */
class TApplicationPasteTest {

    @Test
    void terminalPasteReplacesClipboardAndUsesPasteCommand() throws Exception {
        HeadlessBackend backend = new HeadlessBackend();
        backend.setBackend(backend);
        TApplication application = new TApplication(backend);
        TWindow window = new TWindow(application, "paste", 0, 0, 40, 10);
        TField field = window.addField(1, 1, 30, false);
        application.getClipboard().copyText("old");

        Thread applicationThread = new Thread(application::run);
        applicationThread.start();
        try {
            application.postEvent(new TPasteEvent(backend, "hello"));
            waitForText(field, "hello");

            assertEquals("hello", application.getClipboard().pasteText());

            application.postEvent(new TCommandEvent(backend, cmPaste));
            waitForText(field, "hellohello");
        } finally {
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
}
