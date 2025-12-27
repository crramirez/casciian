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
    @DisplayName("should adjust white color when it is brighter than threshold")
    void shouldAdjustWhiteColorWhenBrighterThanThreshold() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // Before the OSC response, there should be no output for color adjustment
        String outputBefore = outputStream.toString();
        
        // Simulate receiving a bright white color (0xFFFFFF) from the terminal
        terminal.oscResponse("4;7;rgb:ffff/ffff/ffff");

        // The terminal should have sent an OSC command to adjust the color
        String outputAfter = outputStream.toString();
        
        // The output should contain the OSC 4 sequence to set color 7
        assertTrue(outputAfter.length() > outputBefore.length(),
            "Terminal should send OSC command to adjust white color");
        assertTrue(outputAfter.contains("\033]4;7;rgb:b0/b0/b0\033\\"),
            "Terminal should adjust white color to #b0b0b0");
    }

    @Test
    @DisplayName("should not adjust white color when it is darker than threshold")
    void shouldNotAdjustWhiteColorWhenDarkerThanThreshold() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // Get the output before the OSC response
        String outputBefore = outputStream.toString();

        // Simulate receiving a dark white color (0x808080) from the terminal
        terminal.oscResponse("4;7;rgb:8080/8080/8080");

        // The terminal should not send any additional output
        String outputAfter = outputStream.toString();
        
        // The output should not contain an adjustment sequence
        // (only the normal screen clear/redraw that oscResponse triggers)
        assertFalse(outputAfter.contains("\033]4;7;rgb:b0/b0/b0\033\\"),
            "Terminal should not adjust white color when it is already dark enough");
    }

    @Test
    @DisplayName("should not adjust white color when it equals the threshold")
    void shouldNotAdjustWhiteColorWhenEqualsThreshold() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // Simulate receiving the exact threshold color from the terminal
        terminal.oscResponse("4;7;rgb:b0b0/b0b0/b0b0");

        String output = outputStream.toString();
        
        // The output should not contain an adjustment sequence because the color
        // is equal to the threshold (not brighter)
        assertFalse(output.contains("\033]4;7;rgb:b0/b0/b0\033\\"),
            "Terminal should not adjust white color when it equals the threshold");
    }

    @Test
    @DisplayName("should only adjust white color once per session")
    void shouldOnlyAdjustWhiteColorOncePerSession() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // First bright white color should trigger adjustment
        terminal.oscResponse("4;7;rgb:ffff/ffff/ffff");
        String outputAfterFirst = outputStream.toString();
        assertTrue(outputAfterFirst.contains("\033]4;7;rgb:b0/b0/b0\033\\"),
            "Terminal should adjust white color on first bright color");

        // Clear the output stream to check for new output
        outputStream.reset();

        // Second bright white color should not trigger another adjustment
        terminal.oscResponse("4;7;rgb:ffff/ffff/ffff");
        String outputAfterSecond = outputStream.toString();
        assertFalse(outputAfterSecond.contains("\033]4;7;rgb:b0/b0/b0\033\\"),
            "Terminal should not adjust white color again");
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
        String output = outputStream.toString();
        assertTrue(output.contains("\033]4;7;rgb:ff/ff/ff\033\\"),
            "Terminal should restore original white color on close");
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

    // Foreground color tests
    
    @Test
    @DisplayName("Foreground colors use 90-97 range (AIXterm bright colors)")
    void shouldUseBrightColorsForForeground() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // Set up a cell with foreground color (no RGB)
        CellAttributes attr = new CellAttributes();
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
        // Bright colors use the 90-97 range where green is 92
        assertTrue(output.contains("\033[92m"),
            "Green foreground should use bright color code 92 (not 32). Output: " + 
            escapeForDisplay(output));
    }

    @Test
    @DisplayName("All foreground colors use correct bright codes")
    void shouldUseCorrectBrightCodesForAllForegroundColors() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // Test all standard colors except WHITE (which is the default and may not 
        // trigger a separate color change command due to optimization)
        Color[] colors = {
            Color.BLACK, Color.RED, Color.GREEN, Color.YELLOW,
            Color.BLUE, Color.MAGENTA, Color.CYAN
        };
        int[] expectedBrightCodes = {90, 91, 92, 93, 94, 95, 96};

        for (int i = 0; i < colors.length; i++) {
            Color color = colors[i];
            int expectedCode = expectedBrightCodes[i];
            
            CellAttributes attr = new CellAttributes();
            attr.setForeColor(color);
            attr.setBackColor(Color.BLACK);

            // Reset terminal state
            terminal.clearPhysical();
            terminal.putCharXY(0, 0, 'X', attr);
            
            outputStream.reset();
            terminal.flushPhysical();

            String output = outputStream.toString();
            
            assertTrue(output.contains("\033[" + expectedCode + "m"),
                color + " foreground should use bright code " + expectedCode + ". Output: " + 
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
