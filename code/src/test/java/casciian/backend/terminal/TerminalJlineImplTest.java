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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    @DisplayName("getWindowWidth returns non-negative value")
    void testGetWindowWidthReturnsNonNegative() {
        // JLine may return 0 in non-interactive environments
        assertTrue(terminal.getWindowWidth() >= 0);
    }

    @Test
    @DisplayName("getWindowHeight returns non-negative value")
    void testGetWindowHeightReturnsNonNegative() {
        // JLine may return 0 in non-interactive environments
        assertTrue(terminal.getWindowHeight() >= 0);
    }

    @Test
    @DisplayName("queryWindowSize does not throw")
    void testQueryWindowSizeDoesNotThrow() {
        assertDoesNotThrow(() -> terminal.queryWindowSize());
    }

    @Test
    @DisplayName("window dimensions are non-negative after queryWindowSize")
    void testWindowDimensionsAfterQuery() {
        terminal.queryWindowSize();
        int width = terminal.getWindowWidth();
        int height = terminal.getWindowHeight();
        
        // JLine may return 0 in non-interactive environments, but never negative
        assertTrue(width >= 0 && width <= 10000, "Width should be between 0 and 10000");
        assertTrue(height >= 0 && height <= 10000, "Height should be between 0 and 10000");
    }

    @Test
    @org.junit.jupiter.api.Timeout(10)
    @DisplayName("readBytes delivers raw bytes above 0x7F from a POSIX-style JLine terminal")
    void testReadBytesPreservesRawLegacyMouseBytes() throws Exception {
        // ESC [ M with X10 coordinate bytes 0x80 (column 96) and 0xFF
        // (row 223): neither is valid standalone UTF-8.
        byte[] report = {0x1B, '[', 'M', 0x20, (byte) 0x80, (byte) 0xFF};
        // Keep the input open: JLine treats end-of-stream as a closed tty.
        java.io.PipedOutputStream feed = new java.io.PipedOutputStream();
        java.io.PipedInputStream pipe = new java.io.PipedInputStream(feed);
        feed.write(report);
        feed.flush();
        org.jline.terminal.Terminal jline = new org.jline.terminal.impl.ExternalTerminal(
            "test", "xterm", pipe, new java.io.ByteArrayOutputStream(),
            java.nio.charset.StandardCharsets.UTF_8);
        TerminalJlineImpl jlineImpl = new TerminalJlineImpl(jline, false);
        try {
            jlineImpl.setRawMode();
            assertTrue(jlineImpl.isByteInputSupported());
            byte[] buffer = new byte[16];
            int total = 0;
            long deadline = System.currentTimeMillis() + 2000;
            while (total < report.length && System.currentTimeMillis() < deadline) {
                int rc = jlineImpl.readBytes(buffer, total, buffer.length - total);
                if (rc < 0) {
                    break;
                }
                total += rc;
            }
            assertEquals(report.length, total);
            for (int i = 0; i < report.length; i++) {
                assertEquals(report[i], buffer[i], "byte " + i);
            }
        } finally {
            feed.close();
            jlineImpl.close();
        }
    }
}
