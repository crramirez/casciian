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
import java.util.List;

import casciian.TKeypress;
import casciian.event.TInputEvent;
import casciian.event.TKeypressEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static casciian.TKeypress.kbCtrlI;
import static casciian.TKeypress.kbCtrlLeft;
import static casciian.TKeypress.kbShiftEnter;
import static casciian.TKeypress.kbTab;
import static casciian.TKeypress.kbUp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests that ECMA48Terminal negotiates the Kitty keyboard protocol, decodes
 * CSI u keystrokes off the wire, and keeps the legacy escape sequence parser
 * working for terminals that ignore the request.
 */
@DisplayName("ECMA48Terminal Kitty keyboard protocol")
class ECMA48TerminalKittyKeyboardTest {

    /**
     * How long to wait for the reader thread to turn input bytes into
     * events before giving up.
     */
    private static final long EVENT_TIMEOUT_MILLIS = 5000;

    private ECMA48Terminal terminal;
    private ByteArrayOutputStream outputStream;
    private Backend mockBackend;

    @BeforeEach
    void setUp() {
        System.clearProperty(SystemProperties.CASCIIAN_ECMA48_KITTY_KEYBOARD);
        SystemProperties.reset();
        mockBackend = Mockito.mock(Backend.class);
        outputStream = new ByteArrayOutputStream();
    }

    @AfterEach
    void tearDown() {
        if (terminal != null) {
            terminal.closeTerminal();
            terminal = null;
        }
        System.clearProperty(SystemProperties.CASCIIAN_ECMA48_KITTY_KEYBOARD);
        SystemProperties.reset();
    }

    // ------------------------------------------------------------------------
    // Handshake --------------------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Startup pushes the disambiguate-keys flag")
    void startupEnablesProtocol() {
        terminal = createTerminal("");

        assertTrue(written().contains("\033[>1u"),
            "startup should push the Kitty keyboard flags");
    }

    @Test
    @DisplayName("Teardown pops the flag so the host terminal is restored")
    void teardownDisablesProtocol() {
        terminal = createTerminal("");
        int startupLength = written().length();

        terminal.closeTerminal();
        terminal = null;

        String teardown = written().substring(startupLength);
        assertTrue(teardown.contains("\033[<u"),
            "teardown should pop the Kitty keyboard flags");
    }

    @Test
    @DisplayName("Teardown pops the flag exactly once when closed twice")
    void teardownIsIdempotent() {
        terminal = createTerminal("");
        terminal.closeTerminal();
        String afterFirstClose = written();
        terminal.closeTerminal();
        terminal = null;

        assertEquals(1, countOccurrences(afterFirstClose, "\033[<u"),
            "the flags should be popped once");
        assertEquals(afterFirstClose, written(),
            "a second close should not emit the pop again");
    }

    @Test
    @DisplayName("The protocol can be turned off by system property")
    void protocolCanBeDisabled() {
        SystemProperties.setEcma48KittyKeyboard(false);

        terminal = createTerminal("");
        String startup = written();
        terminal.closeTerminal();
        terminal = null;

        assertFalse(startup.contains("\033[>1u"),
            "the flags should not be pushed when disabled");
        assertFalse(written().contains("\033[<u"),
            "nothing should be popped when nothing was pushed");
    }

    // ------------------------------------------------------------------------
    // CSI u input ------------------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("CSI u yields Ctrl+I, not Tab")
    void ctrlIIsNotTab() {
        terminal = createTerminal("\033[105;5u");

        TKeypress key = nextKey();
        assertEquals(kbCtrlI, key);
        assertFalse(kbTab.equals(key), "Ctrl+I must not collapse into Tab");
    }

    @Test
    @DisplayName("CSI u yields Shift+Enter")
    void shiftEnter() {
        terminal = createTerminal("\033[13;2u");

        assertEquals(kbShiftEnter, nextKey());
    }

    @Test
    @DisplayName("CSI u with an event type sub-parameter is decoded")
    void eventTypeSubParameter() {
        terminal = createTerminal("\033[105;5:1u");

        assertEquals(kbCtrlI, nextKey());
    }

    @Test
    @DisplayName("Key release events are dropped, key presses around them are not")
    void releaseEventsAreDropped() {
        // press, release, press.
        terminal = createTerminal("\033[105;5:1u\033[105;5:3u\033[13;2u");

        List<TKeypress> keys = keys(2);
        assertEquals(kbCtrlI, keys.get(0));
        assertEquals(kbShiftEnter, keys.get(1));
    }

    @Test
    @DisplayName("A malformed CSI u sequence does not disturb the keys around it")
    void malformedSequenceIsSkipped() {
        terminal = createTerminal("\033[;u\033[105;5u");

        assertEquals(kbCtrlI, nextKey());
    }

    // ------------------------------------------------------------------------
    // Legacy fallback --------------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Legacy sequences still parse alongside the CSI u parser")
    void legacySequencesStillWork() {
        // Legacy Up, legacy Ctrl+Left, a bare Tab, then a CSI u Ctrl+I.
        terminal = createTerminal("\033[A\033[1;5D\t\033[105;5u");

        List<TKeypress> keys = keys(4);
        assertEquals(kbUp, keys.get(0));
        assertEquals(kbCtrlLeft, keys.get(1));
        assertEquals(kbTab, keys.get(2));
        assertEquals(kbCtrlI, keys.get(3));
    }

    @Test
    @DisplayName("Legacy modifier parameters carrying a sub-parameter are decoded")
    void legacyModifierWithSubParameter() {
        // Some terminals append an event type to legacy sequences too.
        terminal = createTerminal("\033[1;5:1D");

        assertEquals(kbCtrlLeft, nextKey());
    }

    // ------------------------------------------------------------------------
    // Helpers ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Build a terminal whose input is the given text.
     */
    private ECMA48Terminal createTerminal(final String input) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(
                input.getBytes(StandardCharsets.UTF_8));
            return new ECMA48Terminal(mockBackend, null, inputStream,
                outputStream);
        } catch (Exception e) {
            fail("Failed to create terminal: " + e.getMessage());
            return null;
        }
    }

    /**
     * Everything the terminal has written so far.
     */
    private String written() {
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    /**
     * Wait for exactly one keystroke and return it.
     */
    private TKeypress nextKey() {
        return keys(1).getFirst();
    }

    /**
     * Wait for at least count keystrokes and return them.
     */
    private List<TKeypress> keys(final int count) {
        List<TKeypress> keys = new ArrayList<TKeypress>();
        List<TInputEvent> events = new ArrayList<TInputEvent>();
        long deadline = System.currentTimeMillis() + EVENT_TIMEOUT_MILLIS;

        while ((keys.size() < count)
            && (System.currentTimeMillis() < deadline)
        ) {
            events.clear();
            terminal.getEvents(events);
            for (TInputEvent event : events) {
                if (event instanceof TKeypressEvent keypress) {
                    keys.add(keypress.getKey());
                }
            }
            if (keys.size() < count) {
                Thread.yield();
            }
        }

        if (keys.size() < count) {
            fail("Expected " + count + " keystrokes, got " + keys);
        }
        return keys;
    }

    /**
     * Count non-overlapping occurrences of needle in haystack.
     */
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
