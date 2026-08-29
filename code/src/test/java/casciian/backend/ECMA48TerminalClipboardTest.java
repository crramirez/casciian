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
package casciian.backend;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import casciian.event.TInputEvent;
import casciian.event.TPasteEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests OSC 52 clipboard queries and responses.
 */
class ECMA48TerminalClipboardTest {

    private static final long EVENT_TIMEOUT_MILLIS = 5000;
    private static final long NO_EVENT_TIMEOUT_MILLIS = 500;

    private final Backend backend = Mockito.mock(Backend.class);
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private ECMA48Terminal terminal;

    @AfterEach
    void tearDown() {
        if (terminal != null) {
            terminal.closeTerminal();
        }
    }

    @Test
    void requestsClipboardSelectionWithOsc52() throws Exception {
        terminal = createTerminal("");

        terminal.xtermRequestClipboardText();

        assertTrue(written().contains("\033]52;c;?\033\\"));
    }

    @Test
    void decodesUnicodeMultilineClipboardResponseTerminatedBySt()
        throws Exception {

        String text = "first line\n世界 🎉\tlast";
        terminal = createTerminal(response(text, "\033\\"));

        assertSinglePaste(text);
    }

    @Test
    void decodesClipboardResponseTerminatedByBel() throws Exception {
        terminal = createTerminal(response("BEL response", "\007"));

        assertSinglePaste("BEL response");
    }

    @Test
    void acceptsEmptyClipboardResponse() throws Exception {
        terminal = createTerminal("\033]52;c;\033\\");

        assertSinglePaste("");
    }

    @Test
    void ignoresMalformedBase64ClipboardResponse() throws Exception {
        terminal = createTerminal("\033]52;c;not base64!\033\\");
        assertNoEvents();
    }

    @Test
    void ignoresNonClipboardSelection() throws Exception {
        terminal = createTerminal("\033]52;p;SGVsbG8=\033\\");
        assertNoEvents();
    }

    private ECMA48Terminal createTerminal(final String input)
        throws Exception {

        return new ECMA48Terminal(backend, null,
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
            output);
    }

    private String response(final String text, final String terminator) {
        String payload = Base64.getEncoder().encodeToString(
            text.getBytes(StandardCharsets.UTF_8));
        return "\033]52;c;" + payload + terminator;
    }

    private void assertSinglePaste(final String expected) {
        List<TInputEvent> events = new ArrayList<>();
        long deadline = System.currentTimeMillis() + EVENT_TIMEOUT_MILLIS;
        while (events.isEmpty() && (System.currentTimeMillis() < deadline)) {
            terminal.getEvents(events);
            if (events.isEmpty()) {
                Thread.yield();
            }
        }

        assertEquals(1, events.size());
        TPasteEvent paste = assertInstanceOf(TPasteEvent.class,
            events.getFirst());
        assertEquals(expected, paste.getText());
    }

    private void assertNoEvents() {
        long deadline = System.currentTimeMillis() + NO_EVENT_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            List<TInputEvent> events = new ArrayList<>();
            terminal.getEvents(events);
            if (!events.isEmpty()) {
                fail("Unexpected event emitted: " + events);
            }
            Thread.yield();
        }
    }

    private String written() {
        return output.toString(StandardCharsets.UTF_8);
    }
}
