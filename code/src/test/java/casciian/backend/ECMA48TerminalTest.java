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
        // Reset system properties.  reset() only clears the cache, so also
        // clear the backing properties to keep tests isolated from each
        // other (several tests in this class set these without resetting).
        System.clearProperty(SystemProperties.CASCIIAN_TREAT_BOLD_AS_BRIGHT);
        System.clearProperty(SystemProperties.CASCIIAN_ECMA48_RGB_COLOR);
        System.clearProperty(SystemProperties.CASCIIAN_USE_TERMINAL_PALETTE);
        SystemProperties.reset();

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
        // Reset system properties to default
        System.clearProperty(SystemProperties.CASCIIAN_TREAT_BOLD_AS_BRIGHT);
        System.clearProperty(SystemProperties.CASCIIAN_ECMA48_RGB_COLOR);
        System.clearProperty(SystemProperties.CASCIIAN_USE_TERMINAL_PALETTE);
        SystemProperties.reset();
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
    @DisplayName("isRgbColor returns default value (false)")
    void testIsRgbColor() {
        terminal = createTerminal();
        // Default RGB color mode is expected to be false
        assertFalse(SystemProperties.isRgbColor());
    }

    @Test
    @DisplayName("setRgbColor updates RGB color mode")
    void testSetRgbColor() {
        terminal = createTerminal();

        SystemProperties.setRgbColor(true);
        assertTrue(SystemProperties.isRgbColor());

        SystemProperties.setRgbColor(false);
        assertFalse(SystemProperties.isRgbColor());
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

    // CGA palette tests

    @Test
    @DisplayName("should send CGA palette to terminal on startup")
    void shouldSendPaletteOnStartup() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // The terminal constructor should have sent the CGA palette
        String output = outputStream.toString();
        
        // Check for CGA palette colors (using MY* constant values)
        // Color 0 (black) - 0x000000
        assertTrue(output.contains("\033]4;0;rgb:0000/0000/0000\033\\"),
            "Terminal should send CGA black color (color 0)");
        // Color 1 (red) - 0xa80000
        assertTrue(output.contains("\033]4;1;rgb:aaaa/0000/0000\033\\"),
            "Terminal should send CGA red color (color 1)");
        // Color 7 (white/light gray) - 0xaaaaaa
        assertTrue(output.contains("\033]4;7;rgb:aaaa/aaaa/aaaa\033\\"),
            "Terminal should send CGA white color (color 7)");
        // Color 8 (bright black/dark gray) - 0x555555
        assertTrue(output.contains("\033]4;8;rgb:5555/5555/5555\033\\"),
            "Terminal should send CGA bright black color (color 8)");
        // Color 15 (bright white) - 0xffffff
        assertTrue(output.contains("\033]4;15;rgb:ffff/ffff/ffff\033\\"),
            "Terminal should send CGA bright white color (color 15)");
    }

    @Test
    @DisplayName("should not send CGA palette when useTerminalPalette is true")
    void shouldNotSendPaletteWhenUseTerminalPaletteIsTrue() {
        // Set the property to use terminal's native palette
        SystemProperties.setUseTerminalPalette(true);
        
        try {
            terminal = createTerminal();
            assertNotNull(terminal);

            // The terminal constructor should NOT have sent the CGA palette
            String output = outputStream.toString();
            
            // Check that CGA palette colors are not in the output
            assertFalse(output.contains("\033]4;0;rgb:0000/0000/0000\033\\"),
                "Terminal should not send CGA palette when useTerminalPalette is true");
        } finally {
            // Reset the property
            SystemProperties.setUseTerminalPalette(false);
        }
    }

    @Test
    @DisplayName("isUseTerminalPalette defaults to false")
    void testUseTerminalPaletteDefaultIsFalse() {
        // Reset all properties
        SystemProperties.reset();
        
        // Default should be false
        assertFalse(SystemProperties.isUseTerminalPalette(),
            "useTerminalPalette should default to false");
    }

    @Test
    @DisplayName("setUseTerminalPalette updates the property value")
    void testSetUseTerminalPalette() {
        try {
            // Set to true
            SystemProperties.setUseTerminalPalette(true);
            assertTrue(SystemProperties.isUseTerminalPalette());
            
            // Set to false
            SystemProperties.setUseTerminalPalette(false);
            assertFalse(SystemProperties.isUseTerminalPalette());
        } finally {
            // Reset to default
            SystemProperties.setUseTerminalPalette(false);
        }
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
    @DisplayName("Bold foreground colors use 90-97 range when treatBoldAsBright enabled")
    void shouldUseBrightColorsForBoldForeground() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // Legacy "bold means bright" behavior requires the compatibility
        // property to be enabled.
        SystemProperties.setTreatBoldAsBright(true);

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
    @DisplayName("All bold foreground colors use correct bright codes when treatBoldAsBright enabled")
    void shouldUseCorrectBrightCodesForAllBoldColors() {
        terminal = createTerminal();
        assertNotNull(terminal);

        SystemProperties.setTreatBoldAsBright(true);

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
    
    @Test
    @DisplayName("By default, bold foreground emits real SGR bold and a normal color")
    void boldForegroundEmitsRealSgrBoldByDefault() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // Default (treatBoldAsBright disabled): bold must be emitted as a real
        // SGR 1 and the color left normal so the terminal decides how to show
        // the bold text.
        CellAttributes attr = new CellAttributes();
        attr.setBold(true);
        attr.setForeColor(Color.GREEN);
        attr.setBackColor(Color.BLACK);

        terminal.putCharXY(0, 0, 'A', attr);
        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();

        assertTrue(output.contains("\033[1m"),
            "Bold cell should emit a real SGR bold (\\033[1m) by default. Output: "
            + escapeForDisplay(output));
        assertTrue(output.contains("\033[38;2;"),
            "Bold non-bright color should be pinned to its normal RGB so it "
            + "cannot be brightened. Output: " + escapeForDisplay(output));
        assertFalse(output.contains("\033[92m"),
            "Bold green should NOT use bright code 92 by default. Output: "
            + escapeForDisplay(output));
    }

    @Test
    @DisplayName("Bold-transparent cell is never brightened, even when treatBoldAsBright enabled")
    void boldTransparentCellNotBrightenedWhenPropertyEnabled() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // A cell marked bold-transparent (e.g. produced by the ECMA48 terminal
        // emulator) must reproduce bold faithfully even when the legacy
        // treatBoldAsBright behavior is enabled globally.
        SystemProperties.setTreatBoldAsBright(true);

        CellAttributes attr = new CellAttributes();
        attr.setBold(true);
        attr.setBoldTransparent(true);
        attr.setForeColor(Color.GREEN);
        attr.setBackColor(Color.BLACK);

        terminal.putCharXY(0, 0, 'A', attr);
        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();

        assertTrue(output.contains("\033[1m"),
            "Bold-transparent cell should emit a real SGR bold (\\033[1m). Output: "
            + escapeForDisplay(output));
        assertTrue(output.contains("\033[38;2;"),
            "Bold-transparent non-bright color should be pinned to its normal "
            + "RGB. Output: " + escapeForDisplay(output));
        assertFalse(output.contains("\033[92m"),
            "Bold-transparent green should NOT use bright code 92. Output: "
            + escapeForDisplay(output));
    }

    @Test
    @DisplayName("When useTerminalPalette enabled, bold foreground pin reflects the terminal's reported palette")
    void boldForegroundPinUsesReconciledPaletteWhenUseTerminalPaletteEnabled() {
        // With useTerminalPalette, Casciian does not send its own CGA
        // palette, but it always queries the terminal's ANSI colors
        // (xtermQueryAnsiColors()) and reconciles the response into the
        // internal palette (setColorFromOsc()), forcing a full redraw.  Once
        // that response arrives, the bold-not-bright pin must use the
        // terminal's reported color, not Casciian's own CGA default.
        SystemProperties.setUseTerminalPalette(true);
        try {
            terminal = createTerminal();
            assertNotNull(terminal);

            // Terminal reports its own green (index 2) as RGB(0, 205, 0),
            // distinct from Casciian's CGA default green RGB(0, 170, 0).
            terminal.oscResponse("4;2;rgb:0000/cdcd/0000");

            CellAttributes attr = new CellAttributes();
            attr.setBold(true);
            attr.setForeColor(Color.GREEN);
            attr.setBackColor(Color.BLACK);

            terminal.putCharXY(0, 0, 'A', attr);
            outputStream.reset();
            terminal.flushPhysical();

            String output = outputStream.toString();

            assertTrue(output.contains("\033[1m"),
                "Bold cell should emit a real SGR bold (\\033[1m). Output: "
                + escapeForDisplay(output));
            assertTrue(output.contains("\033[38;2;0;205;0m"),
                "Bold green pin should use the terminal's reported RGB "
                + "(0,205,0), not Casciian's CGA default. Output: "
                + escapeForDisplay(output));
        } finally {
            SystemProperties.setUseTerminalPalette(false);
        }
    }

    @Test
    @DisplayName("Bright foreground color (bold off) matches legacy bold color")
    void shouldUseBrightForegroundForBrightColor() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // New model: bright color + bold off should render identically to the
        // legacy bold + normal color.
        CellAttributes attr = new CellAttributes();
        attr.setBold(false);
        attr.setForeColor(Color.BRIGHT_GREEN);
        attr.setBackColor(Color.BLACK);

        terminal.putCharXY(0, 0, 'A', attr);
        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();
        assertTrue(output.contains("\033[92m"),
            "Bright green foreground should use bright color code 92. Output: " +
            escapeForDisplay(output));
    }

    @Test
    @DisplayName("Bright background color (bold off) uses 100-107 range")
    void shouldUseBrightBackgroundForBrightColor() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes attr = new CellAttributes();
        attr.setBold(false);
        attr.setForeColor(Color.WHITE);
        attr.setBackColor(Color.BRIGHT_RED);

        terminal.putCharXY(0, 0, 'A', attr);
        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();
        // Bright red background = 101.
        assertTrue(output.contains("101"),
            "Bright red background should use bright background code 101. Output: " +
            escapeForDisplay(output));
    }

    @Test
    @DisplayName("All bright background colors use correct 100-107 codes")
    void shouldUseCorrectBrightBackgroundCodes() {
        terminal = createTerminal();
        assertNotNull(terminal);

        Color[] colors = {
            Color.BRIGHT_BLACK, Color.BRIGHT_RED, Color.BRIGHT_GREEN,
            Color.BRIGHT_YELLOW, Color.BRIGHT_BLUE, Color.BRIGHT_MAGENTA,
            Color.BRIGHT_CYAN, Color.BRIGHT_WHITE
        };
        int[] expectedCodes = {100, 101, 102, 103, 104, 105, 106, 107};

        for (int i = 0; i < colors.length; i++) {
            CellAttributes attr = new CellAttributes();
            attr.setForeColor(Color.WHITE);
            attr.setBackColor(colors[i]);

            terminal.clearPhysical();
            terminal.putCharXY(0, 0, 'X', attr);
            outputStream.reset();
            terminal.flushPhysical();

            String output = outputStream.toString();
            assertTrue(output.contains(String.valueOf(expectedCodes[i])),
                colors[i] + " background should use code " + expectedCodes[i]
                + ". Output: " + escapeForDisplay(output));
        }
    }

    @Test
    @DisplayName("Bright foreground RGB matches legacy bold foreground RGB")
    void brightForegroundMatchesLegacyBoldRgb() {
        Color[] base = {
            Color.BLACK, Color.RED, Color.GREEN, Color.YELLOW,
            Color.BLUE, Color.MAGENTA, Color.CYAN, Color.WHITE
        };
        Color[] bright = {
            Color.BRIGHT_BLACK, Color.BRIGHT_RED, Color.BRIGHT_GREEN,
            Color.BRIGHT_YELLOW, Color.BRIGHT_BLUE, Color.BRIGHT_MAGENTA,
            Color.BRIGHT_CYAN, Color.BRIGHT_WHITE
        };

        // The legacy bold rendering path is only active when the
        // compatibility property is enabled.
        SystemProperties.setTreatBoldAsBright(true);

        for (int i = 0; i < base.length; i++) {
            CellAttributes legacy = new CellAttributes();
            legacy.setForeColor(base[i]);
            legacy.setBold(true);

            CellAttributes updated = new CellAttributes();
            updated.setForeColor(bright[i]);
            updated.setBold(false);

            assertEquals(ECMA48Terminal.attrToForegroundColor(legacy),
                ECMA48Terminal.attrToForegroundColor(updated),
                "Bright color " + bright[i] + " should match legacy bold "
                + base[i]);
        }
    }

    // RGB color mode tests (doRgbColor flag)

    @Test
    @DisplayName("When rgbColor enabled, palette foreground colors emit RGB sequences")
    void shouldEmitRgbSequenceForPaletteForegroundWhenRgbColorEnabled() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // Enable RGB color mode
        SystemProperties.setRgbColor(true);

        // Set up a cell with palette foreground color (no explicit RGB)
        CellAttributes attr = new CellAttributes();
        attr.setBold(false);
        attr.setForeColor(Color.GREEN);
        attr.setBackColor(Color.BLACK);

        terminal.putCharXY(0, 0, 'A', attr);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();

        // Should contain T.416 RGB foreground sequence (38;2;R;G;B)
        assertTrue(output.contains("\033[38;2;"),
            "With rgbColor enabled, palette foreground should emit RGB sequence. Output: " +
            escapeForDisplay(output));
    }

    @Test
    @DisplayName("When rgbColor enabled, palette background colors emit RGB sequences")
    void shouldEmitRgbSequenceForPaletteBackgroundWhenRgbColorEnabled() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // Enable RGB color mode
        SystemProperties.setRgbColor(true);

        // Set up a cell with non-default palette background color (no explicit RGB)
        CellAttributes attr = new CellAttributes();
        attr.setBold(false);
        attr.setForeColor(Color.WHITE);
        attr.setBackColor(Color.BLUE);

        terminal.putCharXY(0, 0, 'A', attr);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();

        // Should contain T.416 RGB background sequence (48;2;R;G;B)
        assertTrue(output.contains("\033[48;2;"),
            "With rgbColor enabled, palette background should emit RGB sequence. Output: " +
            escapeForDisplay(output));
    }

    @Test
    @DisplayName("When rgbColor disabled, palette colors do NOT emit RGB sequences")
    void shouldNotEmitRgbSequenceForPaletteColorsWhenRgbColorDisabled() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // Ensure RGB color mode is disabled
        SystemProperties.setRgbColor(false);

        CellAttributes attr = new CellAttributes();
        attr.setBold(false);
        attr.setForeColor(Color.GREEN);
        attr.setBackColor(Color.BLUE);

        terminal.putCharXY(0, 0, 'A', attr);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();

        // Should NOT contain T.416 RGB sequences
        assertFalse(output.contains("38;2;"),
            "With rgbColor disabled, palette foreground should not emit RGB sequence. Output: " +
            escapeForDisplay(output));
        assertFalse(output.contains("48;2;"),
            "With rgbColor disabled, palette background should not emit RGB sequence. Output: " +
            escapeForDisplay(output));
    }

    @Test
    @DisplayName("When rgbColor enabled, bold foreground also emits RGB sequences")
    void shouldEmitRgbSequenceForBoldForegroundWhenRgbColorEnabled() {
        terminal = createTerminal();
        assertNotNull(terminal);

        SystemProperties.setRgbColor(true);

        CellAttributes attr = new CellAttributes();
        attr.setBold(true);
        attr.setForeColor(Color.GREEN);
        attr.setBackColor(Color.BLACK);

        terminal.putCharXY(0, 0, 'A', attr);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();

        // Should contain T.416 RGB foreground sequence (38;2;R;G;B)
        assertTrue(output.contains("\033[38;2;"),
            "With rgbColor enabled, bold palette foreground should emit RGB sequence. Output: " +
            escapeForDisplay(output));
    }

    // 256-color palette tests

    @Test
    @DisplayName("Palette foreground color emits an indexed (38;5;n) sequence")
    void shouldEmitIndexedSequenceForPaletteForeground() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes attr = new CellAttributes();
        attr.setForeColorPalette(196);
        attr.setBackColor(Color.BLACK);

        terminal.putCharXY(0, 0, 'A', attr);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();

        assertTrue(output.contains("\033[38;5;196m"),
            "Palette foreground should emit indexed sequence. Output: " +
            escapeForDisplay(output));
    }

    @Test
    @DisplayName("Palette background color emits an indexed (48;5;n) sequence")
    void shouldEmitIndexedSequenceForPaletteBackground() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes attr = new CellAttributes();
        attr.setForeColor(Color.WHITE);
        attr.setBackColorPalette(21);

        terminal.putCharXY(0, 0, 'A', attr);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();

        assertTrue(output.contains("\033[48;5;21m"),
            "Palette background should emit indexed sequence. Output: " +
            escapeForDisplay(output));
    }

    @Test
    @DisplayName("Palette colors do not emit RGB (38;2/48;2) sequences")
    void paletteColorsDoNotEmitRgbSequences() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes attr = new CellAttributes();
        attr.setForeColorPalette(200);
        attr.setBackColorPalette(20);

        terminal.putCharXY(0, 0, 'A', attr);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();

        assertFalse(output.contains("38;2;"),
            "Palette foreground should not emit RGB sequence. Output: " +
            escapeForDisplay(output));
        assertFalse(output.contains("48;2;"),
            "Palette background should not emit RGB sequence. Output: " +
            escapeForDisplay(output));
        assertTrue(output.contains("\033[38;5;200m"),
            "Expected indexed foreground. Output: " + escapeForDisplay(output));
        assertTrue(output.contains("\033[48;5;20m"),
            "Expected indexed background. Output: " + escapeForDisplay(output));
    }

    @Test
    @DisplayName("A run of identical palette cells keeps the palette color and is not reset to a named placeholder")
    void adjacentIdenticalPaletteCellsDoNotResetColor() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // A themed window background is a run of many identical palette cells.
        CellAttributes attr = new CellAttributes();
        attr.setForeColorPalette(221);
        attr.setBackColorPalette(234);

        terminal.putCharXY(0, 0, 'P', attr);
        terminal.putCharXY(1, 0, 'Q', attr);
        terminal.putCharXY(2, 0, 'R', attr);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();

        // The palette color must be emitted once and remain active for the
        // whole run; the subsequent identical cells must not fall through to
        // the named-color branch and reset to the Color.WHITE / Color.BLACK
        // placeholder left by the palette setters.
        assertTrue(output.contains("\033[38;5;221m"),
            "Expected indexed foreground. Output: " + escapeForDisplay(output));
        assertTrue(output.contains("\033[48;5;234m"),
            "Expected indexed background. Output: " + escapeForDisplay(output));
        // SGR 37 (named white) / SGR 40 (named black) would be the placeholder
        // reset emitted by the bug for cells after the first.
        assertFalse(output.contains("\033[37m"),
            "Identical palette cells must not reset foreground to named white. "
            + "Output: " + escapeForDisplay(output));
        assertFalse(output.contains("\033[40m"),
            "Identical palette cells must not reset background to named black. "
            + "Output: " + escapeForDisplay(output));
    }

    @Test
    @DisplayName("A run of blank palette-background cells is painted, not erased to the default background")
    void trailingBlankPaletteBackgroundCellsAreNotErasedToDefault() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // A themed window/desktop background is a run of blank (space) cells
        // that carry a palette background.  These must be painted with the
        // palette background rather than being treated as empty trailing
        // cells and erased to the terminal default (which shows as black).
        CellAttributes attr = new CellAttributes();
        attr.setForeColorPalette(221);
        attr.setBackColorPalette(234);

        // A single non-blank cell followed by a run of blank palette cells.
        terminal.putCharXY(0, 0, 'X', attr);
        for (int x = 1; x <= 9; x++) {
            terminal.putCharXY(x, 0, ' ', attr);
        }

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();

        // The palette background must be emitted, and the trailing blank
        // cells must be painted as spaces under that background.  Before the
        // fix, isBlank() considered a palette space cell "blank", so the run
        // was dropped and clearRemainingLine() erased it to the default
        // background.
        assertTrue(output.contains("\033[48;5;234m"),
            "Expected indexed background. Output: " + escapeForDisplay(output));
        assertTrue(output.matches("(?s).*\\033\\[48;5;234m.* {9}.*"),
            "Trailing blank palette cells must be painted with the palette "
            + "background, not erased to default. Output: "
            + escapeForDisplay(output));
    }

    @Test
    @DisplayName("putBackgroundAttrXY preserves a palette background instead of erasing it to black")
    void putBackgroundAttrXYPreservesPaletteBackground() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // Themed window/widget backgrounds are applied with
        // putBackgroundAttrXY.  A palette background must be carried through;
        // before the fix it fell through to the Color.BLACK placeholder left
        // by setBackColorPalette and rendered as a black background.
        CellAttributes attr = new CellAttributes();
        attr.setForeColor(Color.MAGENTA);
        attr.setBackColorPalette(234);

        for (int x = 0; x < 5; x++) {
            terminal.putBackgroundAttrXY(x, 0, attr);
        }

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();

        assertTrue(output.contains("\033[48;5;234m"),
            "putBackgroundAttrXY must preserve the palette background. "
            + "Output: " + escapeForDisplay(output));
    }

    @Test
    @DisplayName("putForegroundCharXY preserves an existing palette background under drawn text")
    void putForegroundCharXYPreservesPaletteBackground() {
        terminal = createTerminal();
        assertNotNull(terminal);

        // Label text is drawn with putForegroundCharXY, which keeps the
        // background of the cell already on screen.  When that background is a
        // palette color it must be preserved; before the fix it fell through
        // to the Color.BLACK placeholder and the text got a black background.
        CellAttributes bg = new CellAttributes();
        bg.setForeColor(Color.MAGENTA);
        bg.setBackColorPalette(234);
        for (int x = 0; x < 5; x++) {
            terminal.putCharXY(x, 0, ' ', bg);
        }

        CellAttributes fg = new CellAttributes();
        fg.setForeColor(Color.WHITE);
        terminal.putForegroundCharXY(1, 0, 'H', fg);
        terminal.putForegroundCharXY(2, 0, 'i', fg);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();

        assertTrue(output.contains("\033[48;5;234m"),
            "putForegroundCharXY must keep the underlying palette background. "
            + "Output: " + escapeForDisplay(output));
    }

    // -----------------------------------------------------------------------
    // Faint / italic / hidden / strikethrough attribute rendering tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Faint attribute emits SGR 2")
    void shouldEmitSgr2ForFaint() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes attr = new CellAttributes();
        attr.setFaint(true);

        terminal.putCharXY(0, 0, 'A', attr);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();
        assertTrue(output.contains("\033[2m") || output.contains(";2m"),
            "Faint cell should emit SGR 2. Output: "
            + escapeForDisplay(output));
    }

    @Test
    @DisplayName("Faint attribute is cleared with SGR 22")
    void shouldEmitSgr22WhenFaintCleared() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes faint = new CellAttributes();
        faint.setFaint(true);
        CellAttributes normal = new CellAttributes();

        terminal.putCharXY(0, 0, 'A', faint);
        terminal.putCharXY(1, 0, 'B', normal);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();
        assertTrue(output.contains("\033[22m") || output.contains(";22m"),
            "Clearing faint should emit SGR 22. Output: "
            + escapeForDisplay(output));
    }

    @Test
    @DisplayName("Bold and faint are mutually exclusive at the SGR level")
    void boldToFaintTransitionEmitsCorrectSgr() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes bold = new CellAttributes();
        bold.setBold(true);
        terminal.putCharXY(0, 0, 'A', bold);
        CellAttributes faint = new CellAttributes();
        faint.setFaint(true);
        terminal.putCharXY(1, 0, 'B', faint);

        terminal.flushPhysical();

        String output = outputStream.toString();
        assertTrue(output.contains("[22;2m") || output.contains(";22;2m"),
            "Transition from bold to faint should reset intensity before "
            + "emitting faint. Output: "
            + escapeForDisplay(output));
    }

    @Test
    @DisplayName("Italic attribute emits SGR 3")
    void shouldEmitSgr3ForItalic() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes attr = new CellAttributes();
        attr.setItalic(true);

        terminal.putCharXY(0, 0, 'A', attr);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();
        assertTrue(output.contains(";3m") || output.contains("\033[3m"),
            "Italic cell should emit SGR 3. Output: "
            + escapeForDisplay(output));
    }

    @Test
    @DisplayName("Italic attribute is cleared with SGR 23")
    void shouldEmitSgr23WhenItalicCleared() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes italic = new CellAttributes();
        italic.setItalic(true);
        CellAttributes normal = new CellAttributes();

        terminal.putCharXY(0, 0, 'A', italic);
        terminal.putCharXY(1, 0, 'B', normal);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();
        assertTrue(output.contains(";23m") || output.contains("\033[23m"),
            "Clearing italic should emit SGR 23. Output: "
            + escapeForDisplay(output));
    }

    @Test
    @DisplayName("Hidden attribute emits SGR 8")
    void shouldEmitSgr8ForHidden() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes attr = new CellAttributes();
        attr.setHidden(true);

        terminal.putCharXY(0, 0, 'A', attr);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();
        assertTrue(output.contains(";8m") || output.contains("\033[8m"),
            "Hidden cell should emit SGR 8. Output: "
            + escapeForDisplay(output));
    }

    @Test
    @DisplayName("Hidden attribute is cleared with SGR 28")
    void shouldEmitSgr28WhenHiddenCleared() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes hidden = new CellAttributes();
        hidden.setHidden(true);
        CellAttributes normal = new CellAttributes();

        terminal.putCharXY(0, 0, 'A', hidden);
        terminal.putCharXY(1, 0, 'B', normal);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();
        assertTrue(output.contains(";28m") || output.contains("\033[28m"),
            "Clearing hidden should emit SGR 28. Output: "
            + escapeForDisplay(output));
    }

    @Test
    @DisplayName("Strikethrough attribute emits SGR 9")
    void shouldEmitSgr9ForStrikethrough() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes attr = new CellAttributes();
        attr.setStrikethrough(true);

        terminal.putCharXY(0, 0, 'A', attr);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();
        assertTrue(output.contains(";9m") || output.contains("\033[9m"),
            "Strikethrough cell should emit SGR 9. Output: "
            + escapeForDisplay(output));
    }

    @Test
    @DisplayName("Strikethrough attribute is cleared with SGR 29")
    void shouldEmitSgr29WhenStrikethroughCleared() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes strike = new CellAttributes();
        strike.setStrikethrough(true);
        CellAttributes normal = new CellAttributes();

        terminal.putCharXY(0, 0, 'A', strike);
        terminal.putCharXY(1, 0, 'B', normal);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();
        assertTrue(output.contains(";29m") || output.contains("\033[29m"),
            "Clearing strikethrough should emit SGR 29. Output: "
            + escapeForDisplay(output));
    }

    @Test
    @DisplayName("All new text styles can be combined in a single cell")
    void allNewStylesCanBeCombined() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes attr = new CellAttributes();
        attr.setFaint(true);
        attr.setItalic(true);
        attr.setHidden(true);
        attr.setStrikethrough(true);

        terminal.putCharXY(0, 0, 'A', attr);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();
        assertTrue(output.contains(";2") || output.contains("\033[2"),
            "Output should contain faint code. Output: "
            + escapeForDisplay(output));
        assertTrue(output.contains(";3") || output.contains("\033[3"),
            "Output should contain italic code. Output: "
            + escapeForDisplay(output));
        assertTrue(output.contains(";8") || output.contains("\033[8"),
            "Output should contain hidden code. Output: "
            + escapeForDisplay(output));
        assertTrue(output.contains(";9") || output.contains("\033[9"),
            "Output should contain strikethrough code. Output: "
            + escapeForDisplay(output));
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

    private static int countOccurrences(final String haystack,
            final String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    // Grapheme-cluster / wide-character output tests

    @Test
    @DisplayName("Wide CJK char is emitted once, right half suppressed")
    void wideCharEmittedOnce() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes attr = new CellAttributes();
        // 中 is a full-width CJK ideograph.
        terminal.putStringXY(0, 0, "\u4E2D", attr);
        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();
        assertEquals(1, countOccurrences(output, "\u4E2D"),
            "CJK char should be emitted exactly once (right half suppressed)."
            + " Output: " + escapeForDisplay(output));
    }

    @Test
    @DisplayName("ZWJ emoji grapheme is emitted as one contiguous sequence once")
    void zwjEmojiEmittedContiguously() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes attr = new CellAttributes();
        // 👩‍💻 = woman + ZWJ + laptop.
        String zwj = "\uD83D\uDC69\u200D\uD83D\uDCBB";
        terminal.putStringXY(0, 0, "A" + zwj + "B", attr);
        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();
        assertTrue(output.contains(zwj),
            "ZWJ emoji should be emitted as one contiguous sequence. Output: "
            + escapeForDisplay(output));
        assertEquals(1, countOccurrences(output, zwj),
            "ZWJ emoji should be emitted exactly once.");
        assertEquals(1, countOccurrences(output, "\u200D"),
            "ZWJ codepoint should appear exactly once (no duplicated half).");
    }
    
    // Thread safety tests
    
    @Test
    @DisplayName("setSixelPaletteSize does not throw when called concurrently with flushPhysical")
    void testConcurrentSixelPaletteSizeChangeWithFlush() throws InterruptedException {
        terminal = createTerminal();
        assertNotNull(terminal);
        
        // Track any exceptions from threads
        final java.util.concurrent.atomic.AtomicReference<Throwable> threadException = 
            new java.util.concurrent.atomic.AtomicReference<>();
        
        // Run flushPhysical in one thread
        Thread flushThread = new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    terminal.flushPhysical();
                }
            } catch (Throwable t) {
                threadException.compareAndSet(null, t);
            }
        });
        
        // Run setSixelPaletteSize in another thread
        Thread setterThread = new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    terminal.setSixelPaletteSize((i % 2 == 0) ? 256 : 16);
                }
            } catch (Throwable t) {
                threadException.compareAndSet(null, t);
            }
        });
        
        flushThread.start();
        setterThread.start();
        
        flushThread.join(5000);
        setterThread.join(5000);
        
        assertNull(threadException.get(), 
            "Concurrent access should not throw: " + threadException.get());
    }

    @Test
    @DisplayName("setSixelSharedPalette does not throw when called concurrently with flushPhysical")
    void testConcurrentSixelSharedPaletteChangeWithFlush() throws InterruptedException {
        terminal = createTerminal();
        assertNotNull(terminal);
        
        final java.util.concurrent.atomic.AtomicReference<Throwable> threadException = 
            new java.util.concurrent.atomic.AtomicReference<>();
        
        Thread flushThread = new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    terminal.flushPhysical();
                }
            } catch (Throwable t) {
                threadException.compareAndSet(null, t);
            }
        });
        
        Thread setterThread = new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    terminal.setSixelSharedPalette(i % 2 == 0);
                }
            } catch (Throwable t) {
                threadException.compareAndSet(null, t);
            }
        });
        
        flushThread.start();
        setterThread.start();
        
        flushThread.join(5000);
        setterThread.join(5000);
        
        assertNull(threadException.get(), 
            "Concurrent access should not throw: " + threadException.get());
    }

    @Test
    @DisplayName("setHasSixel does not throw when called concurrently with flushPhysical")
    void testConcurrentSetHasSixelWithFlush() throws InterruptedException {
        terminal = createTerminal();
        assertNotNull(terminal);
        
        final java.util.concurrent.atomic.AtomicReference<Throwable> threadException = 
            new java.util.concurrent.atomic.AtomicReference<>();
        
        Thread flushThread = new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    terminal.flushPhysical();
                }
            } catch (Throwable t) {
                threadException.compareAndSet(null, t);
            }
        });
        
        Thread setterThread = new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    terminal.setHasSixel(i % 2 == 0);
                }
            } catch (Throwable t) {
                threadException.compareAndSet(null, t);
            }
        });
        
        flushThread.start();
        setterThread.start();
        
        flushThread.join(5000);
        setterThread.join(5000);
        
        assertNull(threadException.get(), 
            "Concurrent access should not throw: " + threadException.get());
    }

    @Test
    @DisplayName("Wide glyph re-anchors on its LEFT half when only the RIGHT "
        + "half is dirtied")
    void testWideCharPairedRedrawOnRightHalfChange() {
        terminal = createTerminal();

        CellAttributes attr = new CellAttributes();
        attr.setForeColor(Color.WHITE);
        attr.setBackColor(Color.BLUE);

        // Place a double-width CJK glyph.  The LEFT half occupies column 0
        // and the RIGHT half occupies column 1.
        String wide = "\uF900"; // CJK Compatibility Ideograph, width 2
        terminal.putStringXY(0, 0, wide, attr);
        terminal.flushPhysical();

        // Dirty only the RIGHT half of the glyph (its background), leaving
        // the LEFT half unchanged.  This mimics an effect (e.g. the mouse
        // glow gradient) that touches a single cell of a wide glyph.
        outputStream.reset();
        CellAttributes redBg = new CellAttributes();
        redBg.setBackColor(Color.RED);
        terminal.putBackgroundAttrXY(1, 0, redBg);
        terminal.flushPhysical();

        String output = outputStream.toString();

        // The glyph must be re-emitted, anchored on its LEFT column.  Before
        // the fix, only the RIGHT half changed, so the terminal was
        // positioned onto the right-half column (\033[1;2H) and emitted no
        // glyph, which Windows Terminal renders with a one-column drift.
        assertTrue(output.contains(wide),
            "Wide glyph should be re-emitted as a unit: " + output);
        assertTrue(output.contains("\033[1;1H"),
            "Cursor should be positioned on the LEFT half column: " + output);
        assertFalse(output.contains("\033[1;2H"),
            "Cursor must not be positioned onto the RIGHT half column: "
            + output);
    }

}
