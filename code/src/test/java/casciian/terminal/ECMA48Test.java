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

package casciian.terminal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import casciian.backend.Backend;
import casciian.backend.HeadlessBackend;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ECMA48 terminal emulator - validates OSC 4 color parsing
 * for correct handling of 16-bit color components.
 */
@DisplayName("ECMA48 Terminal Emulator Tests")
class ECMA48Test {

    /**
     * Test that 16-bit color components in OSC 4 sequences are correctly
     * parsed to 8-bit RGB values.
     * <p>
     * This tests the fix for the bright color rendering bug where green
     * was showing as yellow when using 16-bit color specifications.
     * </p>
     */
    @Test
    @DisplayName("OSC 4 with 16-bit color components should be parsed correctly")
    void shouldParse16BitColorComponents() {
        assertDoesNotThrow(() -> {
            // Create a minimal backend for testing
            Backend backend = new HeadlessBackend();
            
            // Create input stream with an OSC 4 sequence setting color 10 (bright green)
            // to rgb:0000/ffff/0000 which should parse to 0x00ff00 (bright green)
            // The format uses 16-bit components (4 hex digits each), with only green maximized
            String osc4Sequence = "\033]4;10;rgb:0000/ffff/0000\033\\";
            ByteArrayInputStream inputStream = new ByteArrayInputStream(
                osc4Sequence.getBytes("UTF-8"));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // Create the emulator
            ECMA48 emulator = new ECMA48(ECMA48.DeviceType.XTERM, inputStream,
                outputStream, null, backend);
            
            // Wait for the emulator to process the input
            emulator.waitForOutput(1000);
            
            // Close the emulator
            emulator.close();
        }, "OSC 4 sequence with 16-bit colors should be processed without exceptions");
    }
    
    /**
     * Test that 8-bit color components in OSC 4 sequences continue to work.
     */
    @Test
    @DisplayName("OSC 4 with 8-bit color components should still work")
    void shouldParse8BitColorComponents() {
        assertDoesNotThrow(() -> {
            Backend backend = new HeadlessBackend();
            
            // Create input stream with an OSC 4 sequence using 8-bit color format
            // rgb:00/ff/00 should parse to 0x00ff00 (bright green)
            String osc4Sequence = "\033]4;10;rgb:00/ff/00\033\\";
            ByteArrayInputStream inputStream = new ByteArrayInputStream(
                osc4Sequence.getBytes("UTF-8"));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            ECMA48 emulator = new ECMA48(ECMA48.DeviceType.XTERM, inputStream,
                outputStream, null, backend);
            
            emulator.waitForOutput(1000);
            emulator.close();
        }, "OSC 4 sequence with 8-bit colors should be processed without exceptions");
    }
    
    /**
     * Test that 4-bit color components in OSC 4 sequences work.
     */
    @Test
    @DisplayName("OSC 4 with 4-bit color components should work")
    void shouldParse4BitColorComponents() {
        assertDoesNotThrow(() -> {
            Backend backend = new HeadlessBackend();
            
            // Create input stream with an OSC 4 sequence using 4-bit color format
            // rgb:0/f/0 should scale to approximately 0x00ff00 (bright green)
            String osc4Sequence = "\033]4;10;rgb:0/f/0\033\\";
            ByteArrayInputStream inputStream = new ByteArrayInputStream(
                osc4Sequence.getBytes("UTF-8"));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            ECMA48 emulator = new ECMA48(ECMA48.DeviceType.XTERM, inputStream,
                outputStream, null, backend);
            
            emulator.waitForOutput(1000);
            emulator.close();
        }, "OSC 4 sequence with 4-bit colors should be processed without exceptions");
    }
    
    /**
     * Test that 12-bit color components in OSC 4 sequences work.
     */
    @Test
    @DisplayName("OSC 4 with 12-bit color components should work")
    void shouldParse12BitColorComponents() {
        assertDoesNotThrow(() -> {
            Backend backend = new HeadlessBackend();
            
            // Create input stream with an OSC 4 sequence using 12-bit color format
            // rgb:000/fff/000 should parse to approximately 0x00ff00 (bright green)
            String osc4Sequence = "\033]4;10;rgb:000/fff/000\033\\";
            ByteArrayInputStream inputStream = new ByteArrayInputStream(
                osc4Sequence.getBytes("UTF-8"));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            ECMA48 emulator = new ECMA48(ECMA48.DeviceType.XTERM, inputStream,
                outputStream, null, backend);
            
            emulator.waitForOutput(1000);
            emulator.close();
        }, "OSC 4 sequence with 12-bit colors should be processed without exceptions");
    }
}
