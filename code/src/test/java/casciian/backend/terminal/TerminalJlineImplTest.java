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

package casciian.backend.terminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for TerminalJlineImpl class.
 */
@DisplayName("TerminalJlineImpl Tests")
class TerminalJlineImplTest {

    private TerminalJlineImpl terminal;
    private String originalOsName;

    @BeforeEach
    void setUp() {
        originalOsName = System.getProperty("os.name");
        terminal = new TerminalJlineImpl(false);
    }

    @AfterEach
    void tearDown() {
        if (terminal != null) {
            terminal.close();
        }
        if (originalOsName != null) {
            System.setProperty("os.name", originalOsName);
        }
    }

    @Test
    @DisplayName("hasCustomWriter returns false before setRawMode is called")
    void testHasCustomWriterReturnsFalseBeforeRawMode() {
        // Before setRawMode is called, jlineTerminal is null
        assertFalse(terminal.hasCustomWriter());
    }

    @Test
    @DisplayName("getWriter returns null before setRawMode is called")
    void testGetWriterReturnsNullBeforeRawMode() {
        // Before setRawMode is called, jlineTerminal is null
        assertNull(terminal.getWriter());
    }

    @Test
    @DisplayName("hasCustomInputStream returns false before setRawMode is called")
    void testHasCustomInputStreamReturnsFalseBeforeRawMode() {
        // Before setRawMode is called, jlineTerminal is null
        assertFalse(terminal.hasCustomInputStream());
    }

    @Test
    @DisplayName("getInputStream returns null before setRawMode is called")
    void testGetInputStreamReturnsNullBeforeRawMode() {
        // Before setRawMode is called, jlineTerminal is null
        assertNull(terminal.getInputStream());
    }

    @Test
    @DisplayName("close does not throw exception when jlineTerminal is null")
    void testCloseDoesNotThrowWhenNotInitialized() {
        // close() should not throw when jlineTerminal is null
        terminal.close();
        // Can call close multiple times
        terminal.close();
    }

    @Test
    @DisplayName("setCookedMode does not throw when not in raw mode")
    void testSetCookedModeDoesNotThrowWhenNotInRawMode() {
        // setCookedMode should not throw when not in raw mode
        terminal.setCookedMode();
    }

    @Test
    @DisplayName("constructor with debugToStderr true creates valid terminal")
    void testConstructorWithDebugTrue() {
        TerminalJlineImpl debugTerminal = new TerminalJlineImpl(true);
        assertFalse(debugTerminal.hasCustomWriter());
        debugTerminal.close();
    }

    @Test
    @DisplayName("hasCustomWriter returns false on non-Windows after setRawMode")
    void testHasCustomWriterReturnsFalseOnNonWindows() {
        System.setProperty("os.name", "Linux");
        TerminalJlineImpl linuxTerminal = new TerminalJlineImpl(false);
        try {
            // This will try to initialize jline terminal
            // On a headless environment, it may not fully work, but it shouldn't throw
            linuxTerminal.setRawMode();
            // Even if jlineTerminal is created, hasCustomWriter should be false on Linux
            // because it only returns true for Windows
            assertFalse(linuxTerminal.hasCustomWriter());
        } finally {
            linuxTerminal.close();
        }
    }

    @Test
    @DisplayName("hasCustomInputStream returns false on non-Windows after setRawMode")
    void testHasCustomInputStreamReturnsFalseOnNonWindows() {
        System.setProperty("os.name", "Linux");
        TerminalJlineImpl linuxTerminal = new TerminalJlineImpl(false);
        try {
            linuxTerminal.setRawMode();
            // Even if jlineTerminal is created, hasCustomInputStream should be false on Linux
            assertFalse(linuxTerminal.hasCustomInputStream());
        } finally {
            linuxTerminal.close();
        }
    }
}
