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

/**
 * Tests for TerminalShImpl class.
 */
@DisplayName("TerminalShImpl Tests")
class TerminalShImplTest {

    private TerminalShImpl terminal;

    @BeforeEach
    void setUp() {
        terminal = new TerminalShImpl(false);
    }

    @AfterEach
    void tearDown() {
        if (terminal != null) {
            terminal.close();
        }
    }

    @Test
    @DisplayName("hasCustomWriter returns false")
    void testHasCustomWriterReturnsFalse() {
        assertFalse(terminal.hasCustomWriter());
    }

    @Test
    @DisplayName("getWriter returns null")
    void testGetWriterReturnsNull() {
        assertNull(terminal.getWriter());
    }

    @Test
    @DisplayName("hasCustomInputStream returns false")
    void testHasCustomInputStreamReturnsFalse() {
        assertFalse(terminal.hasCustomInputStream());
    }

    @Test
    @DisplayName("getInputStream returns null")
    void testGetInputStreamReturnsNull() {
        assertNull(terminal.getInputStream());
    }

    @Test
    @DisplayName("close does not throw exception")
    void testCloseDoesNotThrow() {
        // close() should not throw any exception
        terminal.close();
        // Can call close multiple times
        terminal.close();
    }

    @Test
    @DisplayName("setRawMode does not throw on non-Unix systems")
    void testSetRawModeDoesNotThrow() {
        // setRawMode should not throw even if stty command fails
        // (e.g., on Windows or when /dev/tty is not available)
        terminal.setRawMode();
    }

    @Test
    @DisplayName("setCookedMode does not throw on non-Unix systems")
    void testSetCookedModeDoesNotThrow() {
        // setCookedMode should not throw even if stty command fails
        terminal.setCookedMode();
    }

    @Test
    @DisplayName("constructor with debugToStderr true creates valid terminal")
    void testConstructorWithDebugTrue() {
        TerminalShImpl debugTerminal = new TerminalShImpl(true);
        assertFalse(debugTerminal.hasCustomWriter());
        debugTerminal.close();
    }
}
