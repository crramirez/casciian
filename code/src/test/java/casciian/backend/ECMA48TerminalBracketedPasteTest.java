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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import casciian.event.TInputEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TPasteEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests ECMA-48 bracketed paste parsing and terminal lifecycle.
 */
@DisplayName("ECMA48Terminal bracketed paste")
class ECMA48TerminalBracketedPasteTest {

    private static final long EVENT_TIMEOUT_MILLIS = 5000;

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
    @DisplayName("A simple paste produces one paste event and no keypresses")
    void simplePasteProducesOneEvent() {
        terminal = createTerminal("\033[200~hello world\033[201~");

        List<TInputEvent> events = waitForEvents(1);

        assertEquals(1, events.size());
        TPasteEvent paste = assertInstanceOf(TPasteEvent.class,
            events.getFirst());
        assertEquals("hello world", paste.getText());
        assertFalse(events.stream().anyMatch(TKeypressEvent.class::isInstance));
    }

    @Test
    @DisplayName("Multiline Unicode and escape sequences are preserved literally")
    void pastePayloadIsPreserved() {
        String text = "hello\n世界 \033[31mred\033[0m\n\n";
        terminal = createTerminal("\033[200~" + text + "\033[201~");

        TPasteEvent paste = assertInstanceOf(TPasteEvent.class,
            waitForEvents(1).getFirst());

        assertEquals(text, paste.getText());
    }

    @Test
    @DisplayName("A terminator fragmented across reads ends the paste")
    void fragmentedTerminatorEndsPaste() throws Exception {
        PipedInputStream input = new PipedInputStream();
        try (PipedOutputStream source = new PipedOutputStream(input)) {
            terminal = new ECMA48Terminal(backend, null, input, output);

            source.write("\033[200~fragmented\033[20"
                .getBytes(StandardCharsets.UTF_8));
            source.flush();
            waitForNoEvents();

            source.write("1~".getBytes(StandardCharsets.UTF_8));
            source.flush();

            TPasteEvent paste = assertInstanceOf(TPasteEvent.class,
                waitForEvents(1).getFirst());
            assertEquals("fragmented", paste.getText());
        }
    }

    @Test
    @DisplayName("Startup enables and cleanup disables bracketed paste once")
    void lifecycleRestoresBracketedPasteMode() {
        terminal = createTerminal("");
        assertTrue(written().contains("\033[?2004h"));

        terminal.closeTerminal();
        terminal.closeTerminal();
        terminal = null;

        assertEquals(1, countOccurrences(written(), "\033[?2004h"));
        assertEquals(1, countOccurrences(written(), "\033[?2004l"));
    }

    private ECMA48Terminal createTerminal(final String input) {
        try {
            return new ECMA48Terminal(backend, null,
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                output);
        } catch (Exception e) {
            fail("Failed to create terminal: " + e.getMessage());
            return null;
        }
    }

    private List<TInputEvent> waitForEvents(final int count) {
        List<TInputEvent> events = new ArrayList<>();
        long deadline = System.currentTimeMillis() + EVENT_TIMEOUT_MILLIS;
        while ((events.size() < count)
            && (System.currentTimeMillis() < deadline)
        ) {
            terminal.getEvents(events);
            if (events.size() < count) {
                Thread.yield();
            }
        }
        if (events.size() < count) {
            fail("Expected " + count + " events, got " + events);
        }
        return events;
    }

    private void waitForNoEvents() throws InterruptedException {
        Thread.sleep(100);
        List<TInputEvent> events = new ArrayList<>();
        terminal.getEvents(events);
        assertTrue(events.isEmpty(), "an incomplete paste must emit no events");
    }

    private String written() {
        return output.toString(StandardCharsets.UTF_8);
    }

    private static int countOccurrences(final String haystack,
        final String needle) {

        int count = 0;
        int index = haystack.indexOf(needle);
        while (index >= 0) {
            count++;
            index = haystack.indexOf(needle, index + needle.length());
        }
        return count;
    }
}
