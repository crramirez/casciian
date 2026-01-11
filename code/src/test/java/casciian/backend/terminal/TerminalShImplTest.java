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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for TerminalShImpl class.
 */
@DisplayName("TerminalShImpl Tests")
class TerminalShImplTest {

    private TerminalShImpl terminal;

    @BeforeEach
    void setUp() {
        terminal = new TerminalShImpl(null, null, false);
    }

    @AfterEach
    void tearDown() {
        if (terminal != null) {
            terminal.close();
        }
    }

    @Test
    @DisplayName("getWriter returns non-null for system output")
    void testGetWriterReturnsNotNull() {
        assertNotNull(terminal.getWriter());
    }

    @Test
    @DisplayName("getInputStream returns non-null for system input")
    void testGetInputStreamReturnsNotNull() {
        assertNotNull(terminal.getInputStream());
    }

    @Test
    @DisplayName("getReader returns non-null for system input")
    void testGetReaderReturnsNotNull() {
        assertNotNull(terminal.getReader());
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
        TerminalShImpl debugTerminal = new TerminalShImpl(null, null, true);
        assertNotNull(debugTerminal.getWriter());
        debugTerminal.close();
    }

    @Test
    @DisplayName("constructor with non-null input creates terminal with that input")
    void testConstructorWithNonNullInput() {
        ByteArrayInputStream input = new ByteArrayInputStream("test".getBytes());
        TerminalShImpl customTerminal = new TerminalShImpl(input, null, false);
        assertNotNull(customTerminal.getInputStream());
        assertNotNull(customTerminal.getReader());
        customTerminal.close();
    }

    @Test
    @DisplayName("constructor with non-null output creates terminal with that output")
    void testConstructorWithNonNullOutput() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        TerminalShImpl customTerminal = new TerminalShImpl(null, output, false);
        assertNotNull(customTerminal.getWriter());
        customTerminal.close();
    }

    @Test
    @DisplayName("constructor with pre-wired Reader and PrintWriter uses them directly")
    void testConstructorWithPreWiredStreams() {
        ByteArrayInputStream input = new ByteArrayInputStream("test".getBytes());
        java.io.Reader reader = new java.io.StringReader("test");
        java.io.PrintWriter writer = new java.io.PrintWriter(new ByteArrayOutputStream());
        
        TerminalShImpl customTerminal = new TerminalShImpl(input, reader, writer, false);
        
        // Verify the terminal returns the exact same streams that were passed in
        assertEquals(input, customTerminal.getInputStream());
        assertEquals(reader, customTerminal.getReader());
        assertEquals(writer, customTerminal.getWriter());
        customTerminal.close();
    }

    @Test
    @DisplayName("hasInput does not throw exception")
    void testHasInputDoesNotThrow() {
        // hasInput should not throw on any system
        assertDoesNotThrow(() -> terminal.hasInput());
    }

    @Test
    @DisplayName("hasInput returns false for empty input stream")
    void testHasInputReturnsFalseForEmptyStream() throws IOException {
        ByteArrayInputStream emptyInput = new ByteArrayInputStream(new byte[0]);
        TerminalShImpl emptyTerminal = new TerminalShImpl(emptyInput, null, false);
        
        assertFalse(emptyTerminal.hasInput());
        emptyTerminal.close();
    }

    @Test
    @DisplayName("hasInput returns true when input is available")
    void testHasInputReturnsTrueWhenInputAvailable() throws IOException {
        ByteArrayInputStream inputWithData = new ByteArrayInputStream("test".getBytes());
        TerminalShImpl terminalWithInput = new TerminalShImpl(inputWithData, null, false);
        
        assertTrue(terminalWithInput.hasInput());
        terminalWithInput.close();
    }
}
