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
        System.clearProperty(
            SystemProperties.CASCIIAN_ECMA48_MODIFY_OTHER_KEYS);
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
        System.clearProperty(
            SystemProperties.CASCIIAN_ECMA48_MODIFY_OTHER_KEYS);
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
    // Support detection --------------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Startup sends both the push and the capability query")
    void startupSendsQuery() {
        terminal = createTerminal("");

        assertTrue(written().contains("\033[>1u"),
            "startup should push the Kitty keyboard flags");
        assertTrue(written().contains("\033[?u"),
            "startup should query the terminal's current flags");
    }

    @Test
    @DisplayName("Support starts UNKNOWN before any response arrives")
    void supportStartsUnknown() {
        terminal = createTerminal("");

        assertEquals(KittyKeyboard.SupportState.UNKNOWN,
            terminal.getKittyKeyboardSupport());
    }

    @Test
    @DisplayName("A reply to the capability query means SUPPORTED")
    void queryReplyMeansSupported() {
        // The terminal answers "CSI ? u" with "CSI ? flags u".
        terminal = createTerminal("\033[?1u");

        assertEquals(KittyKeyboard.SupportState.SUPPORTED,
            waitForSupport());
    }

    @Test
    @DisplayName("A Device Attributes reply with no prior query reply means UNSUPPORTED")
    void daWithoutQueryReplyMeansUnsupported() {
        // The terminal ignores "CSI ? u" (as WezTerm does with
        // enable_kitty_keyboard left at its default) but still answers
        // Device Attributes, as every terminal does.
        terminal = createTerminal("\033[?1c");

        assertEquals(KittyKeyboard.SupportState.UNSUPPORTED,
            waitForSupport());
    }

    @Test
    @DisplayName("A query reply arriving before the DA sentinel is not overwritten")
    void queryReplyBeforeDaSentinelWins() {
        terminal = createTerminal("\033[?1u\033[?1c");

        assertEquals(KittyKeyboard.SupportState.SUPPORTED,
            waitForSupport());
        assertEquals(ECMA48Terminal.KeyboardProtocol.KITTY,
            terminal.getActiveKeyboardProtocol());
        assertFalse(terminal.isModifyOtherKeysRequested());
    }

    @Test
    @DisplayName("Disabling the property reports UNSUPPORTED immediately")
    void disabledPropertyIsUnsupportedImmediately() {
        SystemProperties.setEcma48KittyKeyboard(false);

        terminal = createTerminal("");

        assertEquals(KittyKeyboard.SupportState.UNSUPPORTED,
            terminal.getKittyKeyboardSupport());
    }

    // ------------------------------------------------------------------------
    // modifyOtherKeys fallback ------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("DA starts XTQMODKEYS and level-2 fallback exactly once")
    void daStartsModifyOtherKeysFallback() {
        terminal = createTerminal("\033[?1c\033[?1c");

        waitForModifyOtherKeysRequest();
        assertEquals(1, countOccurrences(written(), "\033[?4m"));
        assertEquals(1, countOccurrences(written(), "\033[>4;2m"));
        assertEquals(ECMA48Terminal.ModifyOtherKeysSupport.UNKNOWN,
            terminal.getModifyOtherKeysSupport());
        assertEquals(ECMA48Terminal.KeyboardProtocol.LEGACY,
            terminal.getActiveKeyboardProtocol());
    }

    @Test
    @DisplayName("XTQMODKEYS level 0 confirms level 2 and records ownership")
    void originalLevelZero() {
        terminal = createTerminal("\033[?1c\033[>4;0m");

        waitForModifyOtherKeysSupport();
        assertEquals(0, terminal.getModifyOtherKeysOriginalLevel());
        assertTrue(terminal.isModifyOtherKeysChangedByUs());
        assertEquals(ECMA48Terminal.KeyboardProtocol.MODIFY_OTHER_KEYS,
            terminal.getActiveKeyboardProtocol());
    }

    @Test
    @DisplayName("Shutdown restores an original modifyOtherKeys level 1")
    void originalLevelOneIsRestored() {
        terminal = createTerminal("\033[?1c\033[>4;1m");
        waitForModifyOtherKeysSupport();
        int beforeClose = written().length();

        terminal.closeTerminal();
        terminal = null;

        assertTrue(written().substring(beforeClose).contains("\033[>4;1m"));
    }

    @Test
    @DisplayName("A pre-existing level 2 is not owned or reset")
    void originalLevelTwoIsNotChanged() {
        terminal = createTerminal("\033[?1c\033[>4;2m");
        waitForModifyOtherKeysSupport();
        assertFalse(terminal.isModifyOtherKeysChangedByUs());
        int beforeClose = written().length();

        terminal.closeTerminal();
        terminal = null;

        String teardown = written().substring(beforeClose);
        assertFalse(teardown.contains("\033[>4m"));
        assertFalse(teardown.contains("\033[>4;0m"));
    }

    @Test
    @DisplayName("An unanswered XTQMODKEYS query remains a safe request")
    void unansweredQueryRemainsUnknown() {
        terminal = createTerminal("\033[?1c");

        waitForModifyOtherKeysRequest();
        assertEquals(ECMA48Terminal.ModifyOtherKeysSupport.UNKNOWN,
            terminal.getModifyOtherKeysSupport());
        assertEquals(-1, terminal.getModifyOtherKeysOriginalLevel());
        assertEquals(ECMA48Terminal.KeyboardProtocol.LEGACY,
            terminal.getActiveKeyboardProtocol());
    }

    @Test
    @DisplayName("Explicit false disables automatic modifyOtherKeys fallback")
    void explicitFalseDisablesFallback() {
        SystemProperties.setEcma48ModifyOtherKeys(false);

        terminal = createTerminal("\033[?1c");
        assertEquals(KittyKeyboard.SupportState.UNSUPPORTED, waitForSupport());

        assertFalse(terminal.isModifyOtherKeysRequested());
        assertFalse(written().contains("\033[?4m"));
        assertFalse(written().contains("\033[>4;2m"));
    }

    @Test
    @DisplayName("Explicit true keeps Kitty priority and enables fallback")
    void explicitTrueKeepsKittyPriority() {
        SystemProperties.setEcma48ModifyOtherKeys(true);
        terminal = createTerminal("\033[?1u\033[?1c");

        assertEquals(KittyKeyboard.SupportState.SUPPORTED, waitForSupport());
        assertFalse(terminal.isModifyOtherKeysRequested());
        assertEquals(ECMA48Terminal.KeyboardProtocol.KITTY,
            terminal.getActiveKeyboardProtocol());
    }

    @Test
    @DisplayName("Explicit true falls back when Kitty is disabled")
    void explicitTrueFallsBackWhenKittyDisabled() {
        SystemProperties.setEcma48KittyKeyboard(false);
        SystemProperties.setEcma48ModifyOtherKeys(true);

        terminal = createTerminal("\033[?1c");
        waitForModifyOtherKeysRequest();

        assertEquals(KittyKeyboard.SupportState.UNSUPPORTED,
            terminal.getKittyKeyboardSupport());
        assertEquals(1, countOccurrences(written(), "\033[>4;2m"));
    }

    @Test
    @DisplayName("A late Kitty reply restores modifyOtherKeys and wins")
    void lateKittyReplyWins() {
        terminal = createTerminal(
            "\033[?1c\033[>4;1m\033[?1u");

        waitForProtocol(ECMA48Terminal.KeyboardProtocol.KITTY);
        assertEquals(KittyKeyboard.SupportState.SUPPORTED,
            terminal.getKittyKeyboardSupport());
        assertFalse(terminal.isModifyOtherKeysChangedByUs());
        assertTrue(written().contains("\033[>4;1m"),
            "late Kitty should restore the previous level immediately");
    }

    @Test
    @DisplayName("Unknown original state uses reset-to-initial exactly once")
    void unknownOriginalStateRestoresOnce() {
        terminal = createTerminal("\033[?1c");
        waitForModifyOtherKeysRequest();

        terminal.closeTerminal();
        String afterFirstClose = written();
        terminal.closeTerminal();
        terminal = null;

        assertEquals(1, countOccurrences(afterFirstClose, "\033[>4m"));
        assertEquals(afterFirstClose, written());
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

    @Test
    @DisplayName("modifyOtherKeys level 2 preserves ambiguous control keys")
    void modifyOtherKeysPreservesAmbiguousControlKeys() {
        terminal = createTerminal(
            "\033[27;5;105~\033[27;5;109~\033[27;5;91~"
            + "\033[27;6;118~\033[27;5;118~");

        List<TKeypress> keys = keys(5);
        assertEquals(TKeypress.kbCtrlI, keys.get(0));
        assertEquals(TKeypress.kbCtrlM, keys.get(1));
        assertEquals(new TKeypress(false, 0, '[', false, true, false),
            keys.get(2));
        assertEquals(new TKeypress(false, 0, 'V', false, true, true),
            keys.get(3));
        assertEquals(new TKeypress(false, 0, 'V', false, true, false),
            keys.get(4));
        assertFalse(keys.get(3).equals(keys.get(4)));
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
     * Wait for the Kitty keyboard support determination to leave UNKNOWN,
     * and return whatever it settled to.
     */
    private KittyKeyboard.SupportState waitForSupport() {
        long deadline = System.currentTimeMillis() + EVENT_TIMEOUT_MILLIS;

        while (System.currentTimeMillis() < deadline) {
            KittyKeyboard.SupportState support =
                terminal.getKittyKeyboardSupport();
            if (support != KittyKeyboard.SupportState.UNKNOWN) {
                return support;
            }
            Thread.yield();
        }

        fail("Kitty keyboard support determination did not settle");
        return null;
    }

    private void waitForModifyOtherKeysRequest() {
        long deadline = System.currentTimeMillis() + EVENT_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            if (terminal.isModifyOtherKeysRequested()
                && written().contains("\033[>4;2m")
            ) {
                return;
            }
            Thread.yield();
        }
        fail("modifyOtherKeys was not requested");
    }

    private void waitForModifyOtherKeysSupport() {
        long deadline = System.currentTimeMillis() + EVENT_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            if (terminal.getModifyOtherKeysSupport()
                == ECMA48Terminal.ModifyOtherKeysSupport.SUPPORTED
            ) {
                return;
            }
            Thread.yield();
        }
        fail("modifyOtherKeys support was not confirmed");
    }

    private void waitForProtocol(
        final ECMA48Terminal.KeyboardProtocol protocol) {

        long deadline = System.currentTimeMillis() + EVENT_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            if (terminal.getActiveKeyboardProtocol() == protocol) {
                return;
            }
            Thread.yield();
        }
        fail("Keyboard protocol did not become " + protocol);
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
