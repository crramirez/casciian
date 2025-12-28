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

import casciian.bits.CellAttributes;
import casciian.bits.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ECMA48Terminal - validates color conversion, RGB color handling,
 * terminal properties, and basic terminal operations.
 */
@DisplayName("ECMA48Terminal Tests")
class ECMA48TerminalTest {

    private ECMA48Terminal terminal;
    private ByteArrayOutputStream outputStream;
    private ByteArrayInputStream inputStream;
    private Backend mockBackend;

    @BeforeEach
    void setUp() {
        // Reset static state for white color adjustment
        ECMA48Terminal.resetWhiteColorAdjustmentState();

        // Create mock backend
        mockBackend = Mockito.mock(Backend.class);

        // Set up streams for terminal I/O
        outputStream = new ByteArrayOutputStream();
        byte[] inputBytes = new byte[0];
        inputStream = new ByteArrayInputStream(inputBytes);
    }

    @AfterEach
    void tearDown() {
        if (terminal != null) {
            terminal.closeTerminal();
        }
    }

    // Color conversion tests - static methods

    @Test
    @DisplayName("attrToForegroundColor returns RGB for custom RGB color")
    void testAttrToForegroundColorCustomRGB() {
        CellAttributes attr = new CellAttributes();
        int customRGB = 0xFF5733; // Orange color
        attr.setForeColorRGB(customRGB);

        assertEquals(customRGB, ECMA48Terminal.attrToForegroundColor(attr));
    }

    @Test
    @DisplayName("attrToForegroundColor returns default for default color")
    void testAttrToForegroundColorDefault() {
        CellAttributes attr = new CellAttributes();
        attr.setDefaultColor(true, true);

        int result = ECMA48Terminal.attrToForegroundColor(attr);
        assertEquals(ECMA48Terminal.getDefaultForeColorRGB(), result);
    }

    @Test
    @DisplayName("attrToForegroundColor handles bold colors correctly")
    void testAttrToForegroundColorBold() {
        CellAttributes attr = new CellAttributes();
        attr.setBold(true);
        attr.setForeColor(Color.RED);

        int result = ECMA48Terminal.attrToForegroundColor(attr);
        assertTrue(result > 0);
    }

    @Test
    @DisplayName("attrToForegroundColor handles all standard colors")
    void testAttrToForegroundColorStandardColors() {
        Color[] colors = {
            Color.BLACK, Color.RED, Color.GREEN, Color.YELLOW,
            Color.BLUE, Color.MAGENTA, Color.CYAN, Color.WHITE
        };

        for (Color color : colors) {
            CellAttributes attr = new CellAttributes();
            attr.setForeColor(color);

            // Should not throw exception
            int result = ECMA48Terminal.attrToForegroundColor(attr);
            assertTrue(result >= 0);
        }
    }

    @Test
    @DisplayName("attrToForegroundColor handles bold standard colors")
    void testAttrToForegroundColorBoldStandardColors() {
        Color[] colors = {
            Color.BLACK, Color.RED, Color.GREEN, Color.YELLOW,
            Color.BLUE, Color.MAGENTA, Color.CYAN, Color.WHITE
        };

        for (Color color : colors) {
            CellAttributes attr = new CellAttributes();
            attr.setForeColor(color);
            attr.setBold(true);

            // Should not throw exception
            int result = ECMA48Terminal.attrToForegroundColor(attr);
            assertTrue(result >= 0);
        }
    }

    @Test
    @DisplayName("attrToBackgroundColor returns RGB for custom RGB color")
    void testAttrToBackgroundColorCustomRGB() {
        CellAttributes attr = new CellAttributes();
        int customRGB = 0x4286F4; // Blue color
        attr.setBackColorRGB(customRGB);

        assertEquals(customRGB, ECMA48Terminal.attrToBackgroundColor(attr));
    }

    @Test
    @DisplayName("attrToBackgroundColor returns default for default color")
    void testAttrToBackgroundColorDefault() {
        CellAttributes attr = new CellAttributes();
        attr.setDefaultColor(false, true);

        int result = ECMA48Terminal.attrToBackgroundColor(attr);
        assertEquals(ECMA48Terminal.getDefaultBackColorRGB(), result);
    }

    @Test
    @DisplayName("attrToBackgroundColor handles all standard colors")
    void testAttrToBackgroundColorStandardColors() {
        Color[] colors = {
            Color.BLACK, Color.RED, Color.GREEN, Color.YELLOW,
            Color.BLUE, Color.MAGENTA, Color.CYAN, Color.WHITE
        };

        for (Color color : colors) {
            CellAttributes attr = new CellAttributes();
            attr.setBackColor(color);

            // Should not throw exception
            int result = ECMA48Terminal.attrToBackgroundColor(attr);
            assertTrue(result >= 0);
        }
    }

    @Test
    @DisplayName("getDefaultForeColorRGB returns valid RGB value")
    void testGetDefaultForeColorRGB() {
        int rgb = ECMA48Terminal.getDefaultForeColorRGB();
        // RGB value should be in valid range (0x000000 to 0xFFFFFF)
        assertTrue(rgb >= 0 && rgb <= 0xFFFFFF);
    }

    @Test
    @DisplayName("getDefaultBackColorRGB returns valid RGB value")
    void testGetDefaultBackColorRGB() {
        int rgb = ECMA48Terminal.getDefaultBackColorRGB();
        // RGB value should be in valid range (0x000000 to 0xFFFFFF)
        assertTrue(rgb >= 0 && rgb <= 0xFFFFFF);
    }

    @Test
    @DisplayName("getDefaultForeColorRGB and getDefaultBackColorRGB are different")
    void testDefaultColorsAreDifferent() {
        int foreRGB = ECMA48Terminal.getDefaultForeColorRGB();
        int backRGB = ECMA48Terminal.getDefaultBackColorRGB();

        // Foreground and background should typically be different
        assertNotEquals(foreRGB, backRGB);
    }

    // Terminal property tests

    @Test
    @DisplayName("getBlinkMillis returns positive value")
    void testGetBlinkMillis() {
        terminal = createTerminal();
        long blinkMillis = terminal.getBlinkMillis();
        assertTrue(blinkMillis > 0);
    }

    @Test
    @DisplayName("setBlinkMillis updates blink rate")
    void testSetBlinkMillis() {
        terminal = createTerminal();
        long newBlinkMillis = 750;
        terminal.setBlinkMillis(newBlinkMillis);
        assertEquals(newBlinkMillis, terminal.getBlinkMillis());
    }

    @Test
    @DisplayName("getBytesPerSecond returns non-negative value")
    void testGetBytesPerSecond() {
        terminal = createTerminal();
        int bps = terminal.getBytesPerSecond();
        assertTrue(bps >= 0);
    }

    @Test
    @DisplayName("getSessionInfo returns non-null session info")
    void testGetSessionInfo() {
        terminal = createTerminal();
        SessionInfo sessionInfo = terminal.getSessionInfo();
        assertNotNull(sessionInfo);
    }

    @Test
    @DisplayName("getOutput returns non-null PrintWriter")
    void testGetOutput() {
        terminal = createTerminal();
        PrintWriter output = terminal.getOutput();
        assertNotNull(output);
    }

    @Test
    @DisplayName("getTextBlinkVisible returns boolean value")
    void testGetTextBlinkVisible() {
        terminal = createTerminal();
        // Should not throw exception
        boolean blinkVisible = terminal.getTextBlinkVisible();
        // Value can be true or false, both are valid
    }

    @Test
    @DisplayName("isFocused returns boolean value")
    void testIsFocused() {
        terminal = createTerminal();
        // Should not throw exception
        boolean focused = terminal.isFocused();
        // Value can be true or false, both are valid
    }

    @Test
    @DisplayName("isRgbColor returns boolean value")
    void testIsRgbColor() {
        terminal = createTerminal();
        boolean isRgb = terminal.isRgbColor();
        // Should return a boolean value
        assertNotNull(isRgb);
    }

    @Test
    @DisplayName("setRgbColor updates RGB color mode")
    void testSetRgbColor() {
        terminal = createTerminal();

        terminal.setRgbColor(true);
        assertTrue(terminal.isRgbColor());

        terminal.setRgbColor(false);
        assertFalse(terminal.isRgbColor());
    }

    @Test
    @DisplayName("setTitle does not throw exception")
    void testSetTitle() {
        terminal = createTerminal();
        // Should not throw exception
        assertDoesNotThrow(() -> terminal.setTitle("Test Title"));
    }

    @Test
    @DisplayName("flush does not throw exception")
    void testFlush() {
        terminal = createTerminal();
        // Should not throw exception
        assertDoesNotThrow(() -> terminal.flush());
    }

    @Test
    @DisplayName("flushPhysical does not throw exception")
    void testFlushPhysical() {
        terminal = createTerminal();
        // Should not throw exception
        assertDoesNotThrow(() -> terminal.flushPhysical());
    }

    @Test
    @DisplayName("resizeToScreen does not throw exception")
    void testResizeToScreen() {
        terminal = createTerminal();
        // Should not throw exception
        assertDoesNotThrow(() -> terminal.resizeToScreen());
    }

    @Test
    @DisplayName("hasEvents returns false initially")
    void testHasEventsInitially() {
        terminal = createTerminal();
        // With no input, should return false
        assertFalse(terminal.hasEvents());
    }

    @Test
    @DisplayName("closeTerminal does not throw exception")
    void testCloseTerminal() {
        terminal = createTerminal();
        // Should not throw exception
        assertDoesNotThrow(() -> terminal.closeTerminal());
    }

    @Test
    @DisplayName("reloadOptions does not throw exception")
    void testReloadOptions() {
        terminal = createTerminal();
        // Should not throw exception
        assertDoesNotThrow(() -> terminal.reloadOptions());
    }

    @Test
    @DisplayName("xtermSetClipboardText does not throw exception")
    void testXtermSetClipboardText() {
        terminal = createTerminal();
        // Should not throw exception
        assertDoesNotThrow(() -> terminal.xtermSetClipboardText("test text"));
    }

    @Test
    @DisplayName("should parse default foreground color correctly")
    void shouldParseDefaultForeColorCorrectly() {
        terminal = createTerminal();
        assertNotNull(terminal);

        terminal.oscResponse("10;rgb:0000/ffff/afaf");

        int defaultForeColor = ECMA48Terminal.getDefaultForeColorRGB();
        assertEquals(65455, defaultForeColor);
    }

    @Test
    @DisplayName("should parse default background color correctly")
    void shouldParseDefaultBackColorCorrectly() {
        terminal = createTerminal();
        assertNotNull(terminal);

        terminal.oscResponse("11;rgb:ffff/0000/0000");

        int defaultBackColor = ECMA48Terminal.getDefaultBackColorRGB();
        assertEquals(16711680, defaultBackColor);
    }

    @Test
    @DisplayName("should parse color palette correctly")
    void shouldParseColorPaletteCorrectly() {
        terminal = createTerminal();
        assertNotNull(terminal);

        terminal.oscResponse("4;2;rgb:0000/cdcd/0000");

        CellAttributes attr = new CellAttributes();
        attr.setForeColor(Color.GREEN);
        int defaultBackColor = ECMA48Terminal.attrToForegroundColor(attr);
        assertEquals(52480, defaultBackColor);
    }

    // White color adjustment tests

    @Test
    @DisplayName("WHITE_COLOR_MINIMUM_THRESHOLD is set to 0xB0B0B0")
    void testWhiteColorMinimumThreshold() {
        assertEquals(0xB0B0B0, ECMA48Terminal.WHITE_COLOR_MINIMUM_THRESHOLD);
    }

    @Test
    @DisplayName("should send DOS palette when white color is brighter than threshold")
    void shouldSendDOSPaletteWhenBrighterThanThreshold() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // Before the OSC response, there should be no output for color adjustment
        String outputBefore = outputStream.toString();
        
        // Simulate receiving a bright white color (0xFFFFFF) from the terminal
        terminal.oscResponse("4;7;rgb:ffff/ffff/ffff");

        // The terminal should have sent OSC commands to set the full DOS palette
        String outputAfter = outputStream.toString();
        
        // The output should contain OSC 4 sequences for multiple colors
        assertTrue(outputAfter.length() > outputBefore.length(),
            "Terminal should send OSC commands for DOS palette");
        // Check for color 0 (black) - 0x000000
        assertTrue(outputAfter.contains("\033]4;0;rgb:0000/0000/0000\033\\"),
            "Terminal should send DOS black color (color 0)");
        // Check for color 7 (white/light gray) - 0xa8a8a8
        assertTrue(outputAfter.contains("\033]4;7;rgb:a8a8/a8a8/a8a8\033\\"),
            "Terminal should send DOS white color (color 7)");
        // Check for color 15 (bright white) - 0xfcfcfc
        assertTrue(outputAfter.contains("\033]4;15;rgb:fcfc/fcfc/fcfc\033\\"),
            "Terminal should send DOS bright white color (color 15)");
    }

    @Test
    @DisplayName("should not send palette when white color is darker than threshold")
    void shouldNotSendPaletteWhenDarkerThanThreshold() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // Get the output before the OSC response
        String outputBefore = outputStream.toString();

        // Simulate receiving a dark white color (0x808080) from the terminal
        terminal.oscResponse("4;7;rgb:8080/8080/8080");

        // The terminal should not send any palette adjustment
        String outputAfter = outputStream.toString();
        String newOutput = outputAfter.substring(outputBefore.length());
        
        // The output should not contain any DOS palette sequences (colors 0-15)
        for (int i = 0; i <= 15; i++) {
            assertFalse(newOutput.contains("\033]4;" + i + ";"),
                "Terminal should not send DOS palette color " + i + 
                " when white color is already dark enough");
        }
    }

    @Test
    @DisplayName("should not send palette when white color equals the threshold")
    void shouldNotSendPaletteWhenEqualsThreshold() {
        terminal = createTerminal();
        assertNotNull(terminal);

        String outputBefore = outputStream.toString();

        // Simulate receiving the exact threshold color from the terminal
        terminal.oscResponse("4;7;rgb:b0b0/b0b0/b0b0");

        String output = outputStream.toString();
        String newOutput = output.substring(outputBefore.length());
        
        // The output should not contain any DOS palette sequences (colors 0-15)
        // because the color is equal to the threshold (not brighter)
        for (int i = 0; i <= 15; i++) {
            assertFalse(newOutput.contains("\033]4;" + i + ";"),
                "Terminal should not send DOS palette color " + i + 
                " when white color equals the threshold");
        }
    }

    @Test
    @DisplayName("should only send palette once per session")
    void shouldOnlySendPaletteOncePerSession() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // First bright white color should trigger palette sending
        terminal.oscResponse("4;7;rgb:ffff/ffff/ffff");
        String outputAfterFirst = outputStream.toString();
        // Check for DOS white color (color 7) - 0xa8a8a8
        assertTrue(outputAfterFirst.contains("\033]4;7;rgb:a8a8/a8a8/a8a8\033\\"),
            "Terminal should send DOS palette on first bright color");

        // Clear the output stream to check for new output
        outputStream.reset();

        // Second bright white color should not trigger another palette send
        terminal.oscResponse("4;7;rgb:ffff/ffff/ffff");
        String outputAfterSecond = outputStream.toString();
        assertFalse(outputAfterSecond.contains("\033]4;0;"),
            "Terminal should not send palette again");
    }

    @Test
    @DisplayName("should restore original white color when terminal is closed")
    void shouldRestoreOriginalWhiteColorWhenTerminalClosed() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // Simulate receiving a bright white color (0xFFFFFF) to trigger adjustment
        terminal.oscResponse("4;7;rgb:ffff/ffff/ffff");
        
        // Clear the output stream to check for restoration output
        outputStream.reset();

        // Close the terminal - should restore the original color
        terminal.closeTerminal();
        terminal = null; // Prevent double close in tearDown

        // The output should contain the OSC 4 sequence to restore the original color
        // Using 16-bit format (4 hex digits per component) for wezterm compatibility
        String output = outputStream.toString();
        assertTrue(output.contains("\033]4;7;rgb:ffff/ffff/ffff\033\\"),
            "Terminal should restore original white color on close using 16-bit format");
    }

    @Test
    @DisplayName("should force full screen repaint after white color adjustment")
    void shouldForceFullScreenRepaintAfterWhiteColorAdjustment() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // Put a character on the screen first
        CellAttributes attr = new CellAttributes();
        attr.setForeColor(Color.WHITE);
        attr.setBackColor(Color.BLACK);
        terminal.putCharXY(0, 0, 'A', attr);
        
        // Flush to sync physical screen with logical screen
        terminal.flushPhysical();
        
        // Clear the output stream to measure new output
        outputStream.reset();
        
        // Now simulate receiving a bright white color which triggers adjustment
        terminal.oscResponse("4;7;rgb:ffff/ffff/ffff");
        
        // The adjustment should have set reallyCleared = true, so the next
        // flushPhysical() should output something even though logical screen
        // hasn't changed. The screen should be fully redrawn.
        terminal.flushPhysical();
        
        String output = outputStream.toString();
        
        // The output should contain position commands and character output
        // because the screen was marked as needing a full redraw
        assertTrue(output.length() > 0,
            "Screen should have been redrawn after white color adjustment. " +
            "Output length: " + output.length());
        
        // Should contain goto XY sequence for repainted cells
        assertTrue(output.contains("\033["),
            "Output should contain escape sequences for screen redraw");
    }

    // Mouse pointer shape tests for xterm
    
    @Test
    @DisplayName("OSC 22 pointer shape response is processed correctly")
    void shouldProcessOsc22PointerShapeResponse() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // Clear the output stream to check for new output
        outputStream.reset();
        
        // OSC 22 should not trigger any action unless we're waiting for a response
        terminal.oscResponse("22;xterm");
        
        // No pointer change should happen since we weren't querying
        String output = outputStream.toString();
        assertFalse(output.contains("\033]22;"),
            "Should not change pointer when not waiting for query response");
    }

    @Test
    @DisplayName("OSC_POINTER_SHAPE constant is defined correctly")
    void shouldHaveCorrectOscPointerShapeConstant() {
        assertEquals("22", ECMA48Terminal.OSC_POINTER_SHAPE,
            "OSC_POINTER_SHAPE should be '22'");
    }

    // Bold color tests
    
    @Test
    @DisplayName("Bold foreground colors use 90-97 range (AIXterm bright colors)")
    void shouldUseBrightColorsForBoldForeground() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // Set up a cell with bold + foreground color (no RGB)
        CellAttributes attr = new CellAttributes();
        attr.setBold(true);
        attr.setForeColor(Color.GREEN);
        attr.setBackColor(Color.BLACK);

        // Draw the character to the terminal
        terminal.putCharXY(0, 0, 'A', attr);
        
        // Clear the output stream to capture only flush output
        outputStream.reset();
        
        // Flush to generate the escape sequences
        terminal.flushPhysical();

        String output = outputStream.toString();
        
        // The output should contain the bright green foreground color (92)
        // instead of just relying on SGR 1 + normal green (32)
        // Bright colors use the 90-97 range where green is 92
        assertTrue(output.contains("\033[92m"),
            "Bold green foreground should use bright color code 92 (not 32). Output: " + 
            escapeForDisplay(output));
    }

    @Test
    @DisplayName("Non-bold foreground colors use 30-37 range")
    void shouldUseNormalColorsForNonBoldForeground() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // Set up a cell with non-bold foreground color
        CellAttributes attr = new CellAttributes();
        attr.setBold(false);
        attr.setForeColor(Color.GREEN);
        attr.setBackColor(Color.BLACK);

        // Draw the character to the terminal
        terminal.putCharXY(0, 0, 'A', attr);
        
        // Clear the output stream to capture only flush output
        outputStream.reset();
        
        // Flush to generate the escape sequences
        terminal.flushPhysical();

        String output = outputStream.toString();
        
        // The output should contain the normal green foreground color (32)
        assertTrue(output.contains("\033[32m"),
            "Non-bold green foreground should use normal color code 32 (not 92). Output: " + 
            escapeForDisplay(output));
        // And should NOT contain the bright green (92)
        assertFalse(output.contains("\033[92m"),
            "Non-bold green foreground should not use bright color code 92. Output: " + 
            escapeForDisplay(output));
    }

    @Test
    @DisplayName("All bold foreground colors use correct bright codes")
    void shouldUseCorrectBrightCodesForAllBoldColors() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // Test all standard colors with bold
        Color[] colors = {
            Color.BLACK, Color.RED, Color.GREEN, Color.YELLOW,
            Color.BLUE, Color.MAGENTA, Color.CYAN, Color.WHITE
        };
        int[] expectedBrightCodes = {90, 91, 92, 93, 94, 95, 96, 97};

        for (int i = 0; i < colors.length; i++) {
            Color color = colors[i];
            int expectedCode = expectedBrightCodes[i];
            
            CellAttributes attr = new CellAttributes();
            attr.setBold(true);
            attr.setForeColor(color);
            attr.setBackColor(Color.BLACK);

            // Reset terminal state
            terminal.clearPhysical();
            terminal.putCharXY(0, 0, 'X', attr);
            
            outputStream.reset();
            terminal.flushPhysical();

            String output = outputStream.toString();
            
            assertTrue(output.contains("\033[" + expectedCode + "m"),
                "Bold " + color + " should use bright code " + expectedCode + ". Output: " + 
                escapeForDisplay(output));
        }
    }
    
    // Helper methods

    private ECMA48Terminal createTerminal() {
        try {
            return new ECMA48Terminal(mockBackend, null, inputStream, outputStream);
        } catch (Exception e) {
            fail("Failed to create terminal: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Helper method to escape control characters for display in error messages.
     */
    private String escapeForDisplay(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '\033') {
                sb.append("\\033");
            } else if (c < 32) {
                sb.append("\\x").append(String.format("%02x", (int)c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    
}
