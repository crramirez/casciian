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
    @DisplayName("getWriter returns non-null after construction")
    void testGetWriterReturnsNotNull() {
        // JLine terminal is created in constructor now
        assertNotNull(terminal.getWriter());
    }

    @Test
    @DisplayName("getReader returns non-null after construction")
    void testGetReaderReturnsNotNull() {
        // JLine terminal is created in constructor now
        assertNotNull(terminal.getReader());
    }

    @Test
    @DisplayName("close does not throw exception")
    void testCloseDoesNotThrow() {
        // close() should not throw
        terminal.close();
        // Can call close multiple times
        terminal.close();
    }

    @Test
    @DisplayName("setCookedMode does not throw")
    void testSetCookedModeDoesNotThrow() {
        // setCookedMode should not throw
        terminal.setCookedMode();
    }

    @Test
    @DisplayName("setRawMode does not throw")
    void testSetRawModeDoesNotThrow() {
        // setRawMode should not throw
        terminal.setRawMode();
    }

    @Test
    @DisplayName("constructor with debugToStderr true creates valid terminal")
    void testConstructorWithDebugTrue() {
        TerminalJlineImpl debugTerminal = new TerminalJlineImpl(true);
        assertNotNull(debugTerminal.getWriter());
        debugTerminal.close();
    }

    @Test
    @DisplayName("setRawMode followed by setCookedMode works correctly")
    void testRawModeThenCookedMode() {
        terminal.setRawMode();
        terminal.setCookedMode();
        // Should still have valid streams
        assertNotNull(terminal.getWriter());
    }
}
