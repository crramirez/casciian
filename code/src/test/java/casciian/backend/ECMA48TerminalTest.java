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
import casciian.event.TInputEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import casciian.event.TPasteEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
    @DisplayName("synchronized output is enabled by default")
    void testSynchronizedOutputEnabledByDefault() {
        terminal = createTerminal();
        assertTrue(terminal.isSynchronizedOutputEnabled());
    }

    @Test
    @DisplayName("flushPhysical wraps frames in CSI ? 2026 h/l by default")
    void testFlushPhysicalWrapsFrameInSynchronizedOutput() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes attr = new CellAttributes();
        terminal.putCharXY(0, 0, 'A', attr);
        outputStream.reset();

        terminal.flushPhysical();

        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(output.startsWith("\033[?2026h"),
            "Frame should start with synchronized output enable: "
            + escapeForDisplay(output));
        assertTrue(output.contains("A"),
            "Frame should contain rendered content: " + escapeForDisplay(output));
        assertTrue(output.endsWith("\033[?2026l"),
            "Frame should end with synchronized output disable: "
            + escapeForDisplay(output));
    }

    @Test
    @DisplayName("setSynchronizedOutputEnabled(false) disables CSI ? 2026 wrapping")
    void testDisableSynchronizedOutputWrapping() {
        terminal = createTerminal();
        assertNotNull(terminal);

        terminal.setSynchronizedOutputEnabled(false);
        CellAttributes attr = new CellAttributes();
        terminal.putCharXY(0, 0, 'A', attr);
        outputStream.reset();

        terminal.flushPhysical();

        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertFalse(output.contains("\033[?2026h"),
            "Disabled synchronized output should not emit begin sequence: "
            + escapeForDisplay(output));
        assertFalse(output.contains("\033[?2026l"),
            "Disabled synchronized output should not emit end sequence: "
            + escapeForDisplay(output));
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
    @DisplayName("setWorkingDirectory emits an OSC 7 file:// sequence")
    void testSetWorkingDirectory() {
        terminal = createTerminal();
        outputStream.reset();

        terminal.setWorkingDirectory("/home/user/my dir");

        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(output.startsWith("\033]7;file://"),
            "Output should start with OSC 7. Output: "
            + escapeForDisplay(output));
        assertTrue(output.contains("/home/user/my%20dir\033\\"),
            "Output should include the percent-encoded path and ST. "
            + "Output: " + escapeForDisplay(output));
    }

    @Test
    @DisplayName("setWorkingDirectory ignores null and empty paths")
    void testSetWorkingDirectoryIgnoresEmpty() {
        terminal = createTerminal();
        outputStream.reset();

        terminal.setWorkingDirectory(null);
        terminal.setWorkingDirectory("");

        assertEquals("", outputStream.toString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("setWorkingDirectory also emits OSC 9 ; 9 on Windows")
    void testSetWorkingDirectoryWindowsTerminalCompatibility() {
        String originalOsName = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Windows 11");
            terminal = createTerminal();
            outputStream.reset();

            terminal.setWorkingDirectory("C:\\Users\\Alice\\My Dir");

            String output = outputStream.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("\033]7;file://"),
                "Output should still include OSC 7. Output: "
                + escapeForDisplay(output));
            assertTrue(output.contains("/C:/Users/Alice/My%20Dir\033\\"),
                "OSC 7 should include the percent-encoded file URI path. "
                + "Output: " + escapeForDisplay(output));
            assertTrue(output.contains("\033]9;9;C:\\Users\\Alice\\My Dir\033\\"),
                "Output should include Windows Terminal OSC 9 ; 9. Output: "
                + escapeForDisplay(output));
        } finally {
            if (originalOsName != null) {
                System.setProperty("os.name", originalOsName);
            }
        }
    }

    @Test
    @DisplayName("setWorkingDirectory also emits OSC 9 ; 9 for WSL in Windows Terminal")
    void testSetWorkingDirectoryWindowsTerminalWslCompatibility() {
        String originalOsName = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Linux");
            terminal = createTerminal(true, "C:\\Users\\Alice\\My Dir");
            outputStream.reset();

            terminal.setWorkingDirectory("/home/alice/My Dir");

            String output = outputStream.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("\033]7;file://"),
                "Output should still include OSC 7. Output: "
                + escapeForDisplay(output));
            assertTrue(output.contains("/home/alice/My%20Dir\033\\"),
                "OSC 7 should keep the Linux path. Output: "
                + escapeForDisplay(output));
            assertTrue(output.contains("\033]9;9;C:\\Users\\Alice\\My Dir\033\\"),
                "Output should include Windows Terminal OSC 9 ; 9 with the "
                + "wslpath-converted Windows path. Output: "
                + escapeForDisplay(output));
        } finally {
            if (originalOsName != null) {
                System.setProperty("os.name", originalOsName);
            }
        }
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
    @DisplayName("Windows SGR repeated button down is treated as drag motion")
    void testSgrRepeatedButtonDownOnWindowsBecomesMotion() throws Exception {
        terminal = createTerminalForMouseParsing(true,
            "\033[<0;1;1M\033[<0;2;1M");
        List<TMouseEvent> mouseEvents = collectMouseEvents(terminal, 2);
        assertEquals(2, mouseEvents.size());
        TMouseEvent firstPress = mouseEvents.get(0);
        TMouseEvent repeatedPress = mouseEvents.get(1);

        assertEquals(TMouseEvent.Type.MOUSE_DOWN, firstPress.getType());
        assertEquals(0, firstPress.getX());
        assertEquals(0, firstPress.getY());
        assertEquals(TMouseEvent.Type.MOUSE_MOTION, repeatedPress.getType());
        assertTrue(repeatedPress.isMouse1());
        assertEquals(1, repeatedPress.getX());
        assertEquals(0, repeatedPress.getY());
    }

    @Test
    @DisplayName("Non-Windows SGR repeated button down stays as button down")
    void testSgrRepeatedButtonDownOffWindowsStaysDown() throws Exception {
        terminal = createTerminalForMouseParsing(false,
            "\033[<0;1;1M\033[<0;2;1M");
        List<TMouseEvent> mouseEvents = collectMouseEvents(terminal, 2);
        assertEquals(2, mouseEvents.size());
        TMouseEvent repeatedPress = mouseEvents.get(1);

        assertEquals(TMouseEvent.Type.MOUSE_DOWN, repeatedPress.getType());
        assertTrue(repeatedPress.isMouse1());
        assertEquals(1, repeatedPress.getX());
        assertEquals(0, repeatedPress.getY());
    }

    @Test
    @DisplayName("SGR code 3 keeps release as MOUSE_UP and is hover motion only on Windows")
    void testSgrCodeThreeReleaseAndHoverBehavior() throws Exception {
        terminal = createTerminalForMouseParsing(true,
            "\033[<0;1;1M\033[<3;1;1m\033[<3;2;1M");
        List<TMouseEvent> windowsEvents = collectMouseEvents(terminal, 3);
        assertEquals(3, windowsEvents.size());
        TMouseEvent release = windowsEvents.get(1);
        assertEquals(TMouseEvent.Type.MOUSE_UP, release.getType());
        assertTrue(release.isMouse1());
        TMouseEvent windowsHover = windowsEvents.get(2);
        assertEquals(TMouseEvent.Type.MOUSE_MOTION, windowsHover.getType());

        terminal.closeTerminal();
        terminal = createTerminalForMouseParsing(false,
            "\033[<0;1;1M\033[<3;1;1m");
        List<TMouseEvent> nonWindowsReleaseEvents = collectMouseEvents(terminal, 2);
        assertEquals(2, nonWindowsReleaseEvents.size());
        TMouseEvent nonWindowsRelease = nonWindowsReleaseEvents.get(1);
        assertEquals(TMouseEvent.Type.MOUSE_UP, nonWindowsRelease.getType());
        assertTrue(nonWindowsRelease.isMouse1());

        terminal.closeTerminal();
        terminal = createTerminalForMouseParsing(false, "\033[<3;2;1M");
        assertTrue(collectMouseEvents(terminal, 1).isEmpty());
    }

    @Test
    @DisplayName("SGR release reports only one button when multiple buttons are tracked")
    void testSgrReleaseReportsOnlyOneButton() throws Exception {
        terminal = createTerminalForMouseParsing(false,
            "\033[<0;1;1M\033[<1;1;1M\033[<3;1;1m");
        List<TMouseEvent> mouseEvents = collectMouseEvents(terminal, 3);
        assertEquals(3, mouseEvents.size());

        TMouseEvent release = mouseEvents.get(2);
        assertEquals(TMouseEvent.Type.MOUSE_UP, release.getType());
        assertEquals(1, (release.isMouse1() ? 1 : 0)
            + (release.isMouse2() ? 1 : 0)
            + (release.isMouse3() ? 1 : 0));
    }

    @Test
    @DisplayName("SGR code 3 release without tracked button is hover motion")
    void testSgrCodeThreeReleaseWithoutTrackedButtonIsMotion()
        throws Exception {
        terminal = createTerminalForMouseParsing(false, "\033[<3;2;1m");
        List<TMouseEvent> mouseEvents = collectMouseEvents(terminal, 1);
        assertEquals(1, mouseEvents.size());

        TMouseEvent event = mouseEvents.get(0);
        assertEquals(TMouseEvent.Type.MOUSE_MOTION, event.getType());
        assertFalse(event.isMouse1());
        assertFalse(event.isMouse2());
        assertFalse(event.isMouse3());
        assertEquals(1, event.getX());
        assertEquals(0, event.getY());
    }

    @Test
    @DisplayName("closeTerminal does not throw exception")
    void testCloseTerminal() {
        terminal = createTerminal();
        // Should not throw exception
        assertDoesNotThrow(() -> terminal.closeTerminal());
    }

    @Test
    @DisplayName("closeTerminal always emits CSI ? 2026 l")
    void testCloseTerminalEmitsSynchronizedOutputEnd() {
        terminal = createTerminal();
        assertNotNull(terminal);
        outputStream.reset();

        terminal.closeTerminal();

        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("\033[?2026l"),
            "Terminal shutdown should always emit synchronized output end: "
            + escapeForDisplay(output));
        terminal = null;
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
    @DisplayName("Cells with a hyperlink emit OSC 8 open and close sequences")
    void shouldEmitOsc8ForHyperlinkCells() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes link = new CellAttributes();
        link.setForeColor(Color.WHITE);
        link.setBackColor(Color.BLACK);
        link.setHyperlink("https://example.com");

        CellAttributes plain = new CellAttributes();
        plain.setForeColor(Color.WHITE);
        plain.setBackColor(Color.BLACK);

        terminal.putCharXY(0, 0, 'A', link);
        terminal.putCharXY(1, 0, 'B', link);
        terminal.putCharXY(2, 0, 'C', plain);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();

        // OSC 8 open with the URI
        assertTrue(output.contains("\033]8;;https://example.com\033\\"),
            "Should open OSC 8 hyperlink. Output: " + escapeForDisplay(output));
        // OSC 8 close (empty URI)
        assertTrue(output.contains("\033]8;;\033\\"),
            "Should close OSC 8 hyperlink. Output: " + escapeForDisplay(output));
        // The open sequence appears only once for two consecutive link cells
        int firstOpen = output.indexOf("\033]8;;https://example.com");
        int lastOpen = output.lastIndexOf("\033]8;;https://example.com");
        assertEquals(firstOpen, lastOpen,
            "Consecutive link cells should share a single OSC 8 open. Output: "
            + escapeForDisplay(output));
    }

    @Test
    @DisplayName("Hyperlink URIs are sanitized of control characters")
    void shouldSanitizeControlCharactersInHyperlink() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes link = new CellAttributes();
        link.setForeColor(Color.WHITE);
        link.setBackColor(Color.BLACK);
        // Attempt to inject an escape/BEL into the URI.
        link.setHyperlink("https://evil\033]0;pwned\007.com");

        terminal.putCharXY(0, 0, 'A', link);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();

        // The injected OSC title change must not survive (ESC and BEL are
        // stripped, so no new escape sequence can start).
        assertFalse(output.contains("\033]0;pwned"),
            "Injected escape must be stripped. Output: "
            + escapeForDisplay(output));
        // The remaining printable characters stay as a harmless part of the
        // URI; the control characters (ESC, BEL) are removed.
        assertTrue(output.contains("\033]8;;https://evil]0;pwned.com\033\\"),
            "Control characters should be stripped from the URI. Output: "
            + escapeForDisplay(output));
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
    @DisplayName("Double underline emits SGR 21")
    void shouldEmitSgr21ForDoubleUnderline() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes attr = new CellAttributes();
        attr.setUnderlineStyle(CellAttributes.UNDERLINE_STYLE_DOUBLE);

        terminal.putCharXY(0, 0, 'A', attr);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();
        assertTrue(output.contains(";21m") || output.contains("\033[21m"),
            "Double underline should emit SGR 21. Output: "
            + escapeForDisplay(output));
    }

    @Test
    @DisplayName("Curly underline emits SGR 4:3")
    void shouldEmitSgr43ForCurlyUnderline() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes attr = new CellAttributes();
        attr.setUnderlineStyle(CellAttributes.UNDERLINE_STYLE_CURLY);

        terminal.putCharXY(0, 0, 'A', attr);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();
        assertTrue(output.contains(";4:3m") || output.contains("\033[4:3m"),
            "Curly underline should emit SGR 4:3. Output: "
            + escapeForDisplay(output));
    }

    @Test
    @DisplayName("Changing underline style resets underline before applying new style")
    void shouldResetBeforeChangingUnderlineStyle() {
        terminal = createTerminal();
        assertNotNull(terminal);

        CellAttributes single = new CellAttributes();
        single.setUnderlineStyle(CellAttributes.UNDERLINE_STYLE_SINGLE);
        CellAttributes dotted = new CellAttributes();
        dotted.setUnderlineStyle(CellAttributes.UNDERLINE_STYLE_DOTTED);

        terminal.putCharXY(0, 0, 'A', single);
        terminal.putCharXY(1, 0, 'B', dotted);

        outputStream.reset();
        terminal.flushPhysical();

        String output = outputStream.toString();
        assertTrue(output.contains("[24;4:4m") || output.contains(";24;4:4m"),
            "Changing underline style should emit SGR 24 before SGR 4:4. Output: "
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


    // ------------------------------------------------------------------------
    // Raw byte input: legacy X10, UTF-8 (1005) and SGR (1006) mouse reports,
    // and UTF-8 keyboard input
    // ------------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(ints = {1, 95, 96, 127, 128, 160, 200, 223})
    @DisplayName("Legacy X10 report decodes raw column byte for every position 1-223")
    void testX10ColumnFromRawByte(final int column) throws Exception {
        terminal = createByteTerminal(250, 250,
            bytes(0x1B, '[', 'M', 32, column + 32, 33));
        List<TMouseEvent> mouseEvents = collectMouseEvents(terminal, 1);
        assertEquals(1, mouseEvents.size(), "column " + column);
        TMouseEvent event = mouseEvents.get(0);
        assertEquals(TMouseEvent.Type.MOUSE_DOWN, event.getType());
        assertTrue(event.isMouse1());
        assertEquals(column - 1, event.getX());
        assertEquals(0, event.getY());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 95, 96, 127, 128, 160, 200, 223})
    @DisplayName("Legacy X10 report decodes raw row byte for every position 1-223")
    void testX10RowFromRawByte(final int row) throws Exception {
        terminal = createByteTerminal(250, 250,
            bytes(0x1B, '[', 'M', 32, 33, row + 32));
        List<TMouseEvent> mouseEvents = collectMouseEvents(terminal, 1);
        assertEquals(1, mouseEvents.size(), "row " + row);
        TMouseEvent event = mouseEvents.get(0);
        assertEquals(TMouseEvent.Type.MOUSE_DOWN, event.getType());
        assertEquals(0, event.getX());
        assertEquals(row - 1, event.getY());
    }

    @Test
    @DisplayName("Legacy X10 report with both coordinates in the ambiguous lead-byte range")
    void testX10AmbiguousReportResolvedAsX10() throws Exception {
        // Column 163 (0xC3) and row 128 (0xA0) also form the UTF-8 character
        // U+00E0.  With no DECRQM answer and nothing following, the report
        // must still be decoded as X10.
        terminal = createByteTerminal(250, 250,
            bytes(0x1B, '[', 'M', 32, 0xC3, 0xA0));
        List<TMouseEvent> mouseEvents = collectMouseEvents(terminal, 1);
        assertEquals(1, mouseEvents.size());
        assertEquals(162, mouseEvents.get(0).getX());
        assertEquals(127, mouseEvents.get(0).getY());
    }

    @Test
    @DisplayName("Legacy X10 press, drag and release at high coordinates keep their semantics")
    void testX10DragAndReleaseAtHighCoordinates() throws Exception {
        terminal = createByteTerminal(250, 250, bytes(
            0x1B, '[', 'M', 32, 200 + 32, 150 + 32,       // press button 1
            0x1B, '[', 'M', 32 + 32, 210 + 32, 160 + 32,  // drag
            0x1B, '[', 'M', 32 + 32, 223 + 32, 223 + 32,  // drag to the limit
            0x1B, '[', 'M', 3 + 32, 223 + 32, 223 + 32)); // release
        List<TMouseEvent> mouseEvents = collectMouseEvents(terminal, 4);
        assertEquals(4, mouseEvents.size());

        assertEquals(TMouseEvent.Type.MOUSE_DOWN, mouseEvents.get(0).getType());
        assertTrue(mouseEvents.get(0).isMouse1());
        assertEquals(199, mouseEvents.get(0).getX());
        assertEquals(149, mouseEvents.get(0).getY());

        assertEquals(TMouseEvent.Type.MOUSE_MOTION, mouseEvents.get(1).getType());
        assertTrue(mouseEvents.get(1).isMouse1());
        assertEquals(209, mouseEvents.get(1).getX());
        assertEquals(159, mouseEvents.get(1).getY());

        assertEquals(TMouseEvent.Type.MOUSE_MOTION, mouseEvents.get(2).getType());
        assertEquals(222, mouseEvents.get(2).getX());
        assertEquals(222, mouseEvents.get(2).getY());

        assertEquals(TMouseEvent.Type.MOUSE_UP, mouseEvents.get(3).getType());
        assertTrue(mouseEvents.get(3).isMouse1());
        assertEquals(222, mouseEvents.get(3).getX());
        assertEquals(222, mouseEvents.get(3).getY());
    }

    @Test
    @DisplayName("Legacy X10 repeated press without motion bit is treated as drag")
    void testX10RepeatedPressIsDrag() throws Exception {
        terminal = createByteTerminal(250, 250, bytes(
            0x1B, '[', 'M', 32, 100 + 32, 100 + 32,
            0x1B, '[', 'M', 32, 101 + 32, 100 + 32));
        List<TMouseEvent> mouseEvents = collectMouseEvents(terminal, 2);
        assertEquals(2, mouseEvents.size());
        assertEquals(TMouseEvent.Type.MOUSE_DOWN, mouseEvents.get(0).getType());
        assertEquals(TMouseEvent.Type.MOUSE_MOTION, mouseEvents.get(1).getType());
        assertTrue(mouseEvents.get(1).isMouse1());
        assertEquals(100, mouseEvents.get(1).getX());
    }

    @Test
    @DisplayName("Legacy X10 coordinates are clamped to the screen")
    void testX10CoordinatesClampedToScreen() throws Exception {
        // NUL means "beyond the encodable range" in xterm.
        terminal = createByteTerminal(100, 50, bytes(
            0x1B, '[', 'M', 32, 200 + 32, 0));
        List<TMouseEvent> mouseEvents = collectMouseEvents(terminal, 1);
        assertEquals(1, mouseEvents.size());
        assertEquals(99, mouseEvents.get(0).getX());
        assertEquals(49, mouseEvents.get(0).getY());
    }

    @Test
    @DisplayName("Legacy X10 reports split across reads at every byte")
    void testX10ReportSplitAcrossReads() throws Exception {
        terminal = createByteTerminal(250, 250,
            bytes(0x1B), bytes('['), bytes('M'), bytes(32), bytes(0xE8),
            bytes(0xC8),
            bytes(0x1B, '[', 'M', 32 + 32), bytes(0xFF, 0xFF));
        List<TMouseEvent> mouseEvents = collectMouseEvents(terminal, 2);
        assertEquals(2, mouseEvents.size());
        assertEquals(TMouseEvent.Type.MOUSE_DOWN, mouseEvents.get(0).getType());
        assertEquals(199, mouseEvents.get(0).getX());
        assertEquals(167, mouseEvents.get(0).getY());
        assertEquals(TMouseEvent.Type.MOUSE_MOTION, mouseEvents.get(1).getType());
        assertEquals(222, mouseEvents.get(1).getX());
        assertEquals(222, mouseEvents.get(1).getY());
    }

    @Test
    @DisplayName("UTF-8 (1005) report with multibyte coordinates, as confirmed by DECRPM")
    void testUtf8MouseReportAfterDecrpm() throws Exception {
        // Column 300 = 332 = U+014C, row 250 = 282 = U+011A.
        byte[] report = concat(bytes(0x1B, '[', 'M', 32),
            "\u014C\u011A".getBytes(StandardCharsets.UTF_8));
        terminal = createByteTerminal(400, 300,
            "\033[?1005;1$y".getBytes(StandardCharsets.US_ASCII),
            report,
            // Same report split inside the multibyte coordinates.
            java.util.Arrays.copyOfRange(report, 0, 5),
            java.util.Arrays.copyOfRange(report, 5, report.length));
        List<TMouseEvent> mouseEvents = collectMouseEvents(terminal, 2);
        assertEquals(2, mouseEvents.size());
        for (TMouseEvent event : mouseEvents) {
            assertEquals(299, event.getX());
            assertEquals(249, event.getY());
        }
        assertEquals(TMouseEvent.Type.MOUSE_DOWN, mouseEvents.get(0).getType());
    }

    @Test
    @DisplayName("UTF-8 (1005) report without DECRPM is recognized from its structure")
    void testUtf8MouseReportWithoutDecrpm() throws Exception {
        // Column 96 = 128 = C2 80, row 200 = 232 = C3 A8.
        terminal = createByteTerminal(250, 250,
            bytes(0x1B, '[', 'M', 32, 0xC2, 0x80, 0xC3, 0xA8),
            bytes(0x1B, '[', 'M', 3 + 32, 0xC2, 0x80, 0xC3, 0xA8));
        List<TMouseEvent> mouseEvents = collectMouseEvents(terminal, 2);
        assertEquals(2, mouseEvents.size());
        assertEquals(TMouseEvent.Type.MOUSE_DOWN, mouseEvents.get(0).getType());
        assertEquals(95, mouseEvents.get(0).getX());
        assertEquals(199, mouseEvents.get(0).getY());
        assertEquals(TMouseEvent.Type.MOUSE_UP, mouseEvents.get(1).getType());
        assertEquals(95, mouseEvents.get(1).getX());
        assertEquals(199, mouseEvents.get(1).getY());
    }

    @Test
    @DisplayName("DECRPM reporting 1005 reset makes ESC [ M reports X10")
    void testX10AfterDecrpmReset() throws Exception {
        terminal = createByteTerminal(250, 250,
            "\033[?1005;2$y".getBytes(StandardCharsets.US_ASCII),
            bytes(0x1B, '[', 'M', 32, 0xC3, 0xA8, 'x'));
        List<TInputEvent> events = collectInputEvents(terminal, 2);
        assertEquals(2, events.size());
        TMouseEvent mouse = (TMouseEvent) events.get(0);
        assertEquals(162, mouse.getX());
        assertEquals(135, mouse.getY());
        assertKeyChar(events.get(1), 'x');
    }

    @Test
    @DisplayName("SGR (1006) reports beyond 223 are unchanged")
    void testSgrLargeCoordinates() throws Exception {
        terminal = createByteTerminal(400, 300,
            "\033[<0;300;250M\033[<0;301;250M\033[<0;301;250m"
                .getBytes(StandardCharsets.US_ASCII));
        List<TMouseEvent> mouseEvents = collectMouseEvents(terminal, 3);
        assertEquals(3, mouseEvents.size());
        assertEquals(TMouseEvent.Type.MOUSE_DOWN, mouseEvents.get(0).getType());
        assertEquals(299, mouseEvents.get(0).getX());
        assertEquals(249, mouseEvents.get(0).getY());
        assertEquals(300, mouseEvents.get(1).getX());
        assertEquals(TMouseEvent.Type.MOUSE_UP, mouseEvents.get(2).getType());
        assertTrue(mouseEvents.get(2).isMouse1());
    }

    @Test
    @DisplayName("Unicode keys before, between and after mouse reports")
    void testUnicodeKeysInterleavedWithMouse() throws Exception {
        byte[] input = concat(
            "aé".getBytes(StandardCharsets.UTF_8),
            bytes(0x1B, '[', 'M', 32, 200 + 32, 0x80),  // X10, raw bytes
            "漢".getBytes(StandardCharsets.UTF_8),
            "\033[<0;300;5M".getBytes(StandardCharsets.US_ASCII),
            "😀ñ".getBytes(StandardCharsets.UTF_8));
        // Split the input in the middle of the CJK character and the emoji.
        int cjk = 2 + 1 + 6 + 1;
        int emoji = cjk + 2 + 12 + 2;
        terminal = createByteTerminal(400, 300,
            java.util.Arrays.copyOfRange(input, 0, cjk),
            java.util.Arrays.copyOfRange(input, cjk, emoji),
            java.util.Arrays.copyOfRange(input, emoji, input.length));
        List<TInputEvent> events = collectInputEvents(terminal, 8);
        assertEquals(8, events.size(), events.toString());
        assertKeyChar(events.get(0), 'a');
        assertKeyChar(events.get(1), 'é');
        TMouseEvent x10 = (TMouseEvent) events.get(2);
        assertEquals(199, x10.getX());
        assertEquals(95, x10.getY());
        assertKeyChar(events.get(3), '漢');
        TMouseEvent sgr = (TMouseEvent) events.get(4);
        assertEquals(299, sgr.getX());
        assertEquals(4, sgr.getY());
        // Supplementary characters arrive as a surrogate pair, exactly as
        // from a UTF-8 Reader.
        assertKeyChar(events.get(5), "😀".charAt(0));
        assertKeyChar(events.get(6), "😀".charAt(1));
        assertKeyChar(events.get(7), 'ñ');
    }

    @Test
    @DisplayName("Malformed UTF-8 keyboard input becomes U+FFFD without losing other keys")
    void testMalformedUtf8Input() throws Exception {
        terminal = createByteTerminal(80, 25,
            bytes(0xC3, 'a', 0xFF, 'b', 0xE6, 0xBC, 'c'));
        List<TInputEvent> events = collectInputEvents(terminal, 6);
        assertEquals(6, events.size(), events.toString());
        assertKeyChar(events.get(0), 0xFFFD);
        assertKeyChar(events.get(1), 'a');
        assertKeyChar(events.get(2), 0xFFFD);
        assertKeyChar(events.get(3), 'b');
        // The unfinished character is replaced when 'c' interrupts it.
        assertKeyChar(events.get(4), 0xFFFD);
        assertKeyChar(events.get(5), 'c');
    }

    @Test
    @DisplayName("UTF-8 character split by a long pause is reassembled, not replaced")
    void testUtf8CharacterSplitByPause() throws Exception {
        ChunkedSessionInput input = new ChunkedSessionInput(80, 25,
            bytes('a', 0xE6, 0xBC), bytes(0xA2, 'b'));
        input.holdFrom(1);
        terminal = new ECMA48Terminal(mockBackend, null, input, outputStream);

        List<TInputEvent> events = collectInputEvents(terminal, 1);
        assertEquals(1, events.size(), events.toString());
        assertKeyChar(events.get(0), 'a');

        // Much longer than the ESC and mouse report timeouts.
        Thread.sleep(400L);
        assertTrue(collectInputEventsFor(terminal, 0L).isEmpty(),
            "incomplete character must not be flushed early");

        input.releaseAll();
        events = collectInputEvents(terminal, 2);
        assertEquals(2, events.size(), events.toString());
        assertKeyChar(events.get(0), '漢');
        assertKeyChar(events.get(1), 'b');
    }

    @Test
    @DisplayName("Bracketed paste keeps a UTF-8 character split by a long pause")
    void testBracketedPasteUtf8CharacterSplitByPause() throws Exception {
        byte[] start = concat("\033[200~x".getBytes(StandardCharsets.US_ASCII),
            bytes(0xE6, 0xBC));
        byte[] end = concat(bytes(0xA2),
            "y\033[201~".getBytes(StandardCharsets.US_ASCII));
        ChunkedSessionInput input = new ChunkedSessionInput(80, 25, start, end);
        input.holdFrom(1);
        terminal = new ECMA48Terminal(mockBackend, null, input, outputStream);

        Thread.sleep(400L);
        input.releaseAll();

        String pasted = null;
        long deadline = System.currentTimeMillis() + 1000L;
        List<TInputEvent> events = new ArrayList<>();
        while ((pasted == null) && (System.currentTimeMillis() < deadline)) {
            events.clear();
            terminal.getEvents(events);
            for (TInputEvent event : events) {
                if (event instanceof TPasteEvent paste) {
                    pasted = paste.getText();
                }
            }
            Thread.sleep(10L);
        }
        assertEquals("x漢y", pasted);
    }

    @Test
    @DisplayName("Incomplete mouse report does not swallow the following input")
    void testIncompleteMouseReport() throws Exception {
        // ESC interrupts the first report; the SGR report must survive.
        terminal = createByteTerminal(80, 25,
            "\033[M\033[<0;5;6M".getBytes(StandardCharsets.US_ASCII));
        List<TMouseEvent> mouseEvents = collectMouseEvents(terminal, 1);
        assertEquals(1, mouseEvents.size());
        assertEquals(4, mouseEvents.get(0).getX());
        assertEquals(5, mouseEvents.get(0).getY());

        // A truncated report times out and its bytes become keystrokes.
        terminal.closeTerminal();
        terminal = createByteTerminal(80, 25,
            "\033[M a".getBytes(StandardCharsets.US_ASCII));
        List<TInputEvent> events = collectInputEvents(terminal, 2);
        assertEquals(2, events.size(), events.toString());
        assertKeyChar(events.get(0), ' ');
        assertKeyChar(events.get(1), 'a');
    }

    @Test
    @DisplayName("Caller-supplied Latin-1 Reader still decodes X10 coordinates")
    void testReaderPathLatin1X10() throws Exception {
        ChunkedSessionInput input = new ChunkedSessionInput(250, 250,
            bytes(0x1B, '[', 'M', 32, 200 + 32, 0xFF));
        terminal = new ECMA48Terminal(mockBackend, null, input,
            new InputStreamReader(input, StandardCharsets.ISO_8859_1),
            new PrintWriter(outputStream));
        List<TMouseEvent> mouseEvents = collectMouseEvents(terminal, 1);
        assertEquals(1, mouseEvents.size());
        assertEquals(199, mouseEvents.get(0).getX());
        assertEquals(222, mouseEvents.get(0).getY());
    }

    @Test
    @DisplayName("Caller-supplied UTF-8 Reader decodes 1005 coordinates and Unicode keys")
    void testReaderPathUtf8() throws Exception {
        ChunkedSessionInput input = new ChunkedSessionInput(400, 300,
            concat(bytes(0x1B, '[', 'M', 32),
                "\u014C\u011Aé".getBytes(StandardCharsets.UTF_8)));
        Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
        terminal = new ECMA48Terminal(mockBackend, null, input, reader,
            new PrintWriter(outputStream));
        List<TInputEvent> events = collectInputEvents(terminal, 2);
        assertEquals(2, events.size(), events.toString());
        TMouseEvent mouse = (TMouseEvent) events.get(0);
        assertEquals(299, mouse.getX());
        assertEquals(249, mouse.getY());
        assertKeyChar(events.get(1), 'é');
    }

    @Test
    @DisplayName("Startup queries whether UTF-8 mouse mode 1005 is active")
    void testQueriesMode1005() {
        terminal = createTerminal();
        terminal.flushPhysical();
        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("\033[?1005$p"), escapeForDisplay(output));
        // Mouse mode selection itself is unchanged: SGR is still preferred.
        assertTrue(output.contains("\033[?1002;1003;1005;1006h"));
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

    private ECMA48Terminal createTerminal(final boolean windowsTerminalSession,
        final String wslWindowsPath) {
        try {
            return new ECMA48Terminal(mockBackend, null, inputStream,
                outputStream) {
                @Override
                protected boolean isWindowsTerminalSession() {
                    return windowsTerminalSession;
                }

                @Override
                protected String wslDirectoryToWindowsTerminalPath(
                    final String directory
                ) {
                    return wslWindowsPath;
                }
            };
        } catch (Exception e) {
            fail("Failed to create terminal: " + e.getMessage());
            return null;
        }
    }

    private ECMA48Terminal createTerminalForMouseParsing(
        final boolean windowsMouseParsing, final String inputSequence) {
        try {
            byte[] bytes = inputSequence.getBytes(StandardCharsets.UTF_8);
            ByteArrayInputStream mouseInputStream = new ByteArrayInputStream(bytes);
            return new ECMA48Terminal(mockBackend, null, mouseInputStream,
                outputStream) {
                @Override
                protected boolean isWindowsForMouseParsing() {
                    return windowsMouseParsing;
                }
            };
        } catch (Exception e) {
            fail("Failed to create terminal: " + e.getMessage());
            return null;
        }
    }

    /**
     * An InputStream that delivers its data in fixed chunks, one chunk per
     * read() call, and that reports a fixed window size.
     */
    private static final class ChunkedSessionInput extends InputStream
        implements SessionInfo {

        private final List<byte[]> chunks = new ArrayList<>();
        private final TSessionInfo sessionInfo;
        private int chunkIndex = 0;
        private int chunkOffset = 0;
        private int releasedChunks = Integer.MAX_VALUE;

        ChunkedSessionInput(final int width, final int height,
            final byte[]... data) {
            sessionInfo = new TSessionInfo(width, height);
            chunks.addAll(List.of(data));
        }

        /**
         * Make chunks from index onwards unavailable until releaseAll().
         */
        synchronized void holdFrom(final int index) {
            releasedChunks = index;
        }

        synchronized void releaseAll() {
            releasedChunks = Integer.MAX_VALUE;
        }

        @Override
        public synchronized int available() {
            if ((chunkIndex >= chunks.size()) || (chunkIndex >= releasedChunks)) {
                return 0;
            }
            return chunks.get(chunkIndex).length - chunkOffset;
        }

        @Override
        public synchronized int read() {
            byte[] one = new byte[1];
            return read(one, 0, 1) == 1 ? (one[0] & 0xFF) : -1;
        }

        @Override
        public synchronized int read(final byte[] b, final int off,
            final int len) {
            if (chunkIndex >= chunks.size()) {
                return -1;
            }
            if (chunkIndex >= releasedChunks) {
                return 0;
            }
            byte[] chunk = chunks.get(chunkIndex);
            int n = Math.min(len, chunk.length - chunkOffset);
            System.arraycopy(chunk, chunkOffset, b, off, n);
            chunkOffset += n;
            if (chunkOffset == chunk.length) {
                chunkIndex++;
                chunkOffset = 0;
            }
            return n;
        }

        @Override
        public long getStartTime() {
            return sessionInfo.getStartTime();
        }

        @Override
        public int getIdleTime() {
            return sessionInfo.getIdleTime();
        }

        @Override
        public void setIdleTime(final int seconds) {
            sessionInfo.setIdleTime(seconds);
        }

        @Override
        public String getUsername() {
            return sessionInfo.getUsername();
        }

        @Override
        public void setUsername(final String username) {
            sessionInfo.setUsername(username);
        }

        @Override
        public String getLanguage() {
            return sessionInfo.getLanguage();
        }

        @Override
        public void setLanguage(final String language) {
            sessionInfo.setLanguage(language);
        }

        @Override
        public int getWindowWidth() {
            return sessionInfo.getWindowWidth();
        }

        @Override
        public int getWindowHeight() {
            return sessionInfo.getWindowHeight();
        }

        @Override
        public void queryWindowSize() {
            // Fixed size
        }
    }

    private static byte[] bytes(final int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }
        return result;
    }

    private static byte[] concat(final byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }

    /**
     * Create a terminal on the raw byte input path, delivering each chunk
     * in a separate read.
     */
    private ECMA48Terminal createByteTerminal(final int width,
        final int height, final byte[]... chunks) throws Exception {
        return new ECMA48Terminal(mockBackend, null,
            new ChunkedSessionInput(width, height, chunks), outputStream);
    }

    /**
     * Collect keypress and mouse events, in order.
     */
    private List<TInputEvent> collectInputEvents(final ECMA48Terminal t,
        final int maxEvents) throws InterruptedException {
        List<TInputEvent> result = new ArrayList<>();
        List<TInputEvent> events = new ArrayList<>();
        long deadline = System.currentTimeMillis() + 1000L;
        while (System.currentTimeMillis() < deadline
            && result.size() < maxEvents) {
            if (t.hasEvents()) {
                events.clear();
                t.getEvents(events);
                for (TInputEvent event : events) {
                    if ((event instanceof TMouseEvent)
                        || (event instanceof TKeypressEvent)) {
                        result.add(event);
                    }
                }
            } else {
                Thread.sleep(10L);
            }
        }
        return result;
    }

    /**
     * Collect keypress and mouse events that are already queued, waiting
     * at most the given time for more.
     */
    private List<TInputEvent> collectInputEventsFor(final ECMA48Terminal t,
        final long millis) throws InterruptedException {
        List<TInputEvent> result = new ArrayList<>();
        List<TInputEvent> events = new ArrayList<>();
        long deadline = System.currentTimeMillis() + millis;
        do {
            events.clear();
            t.getEvents(events);
            for (TInputEvent event : events) {
                if ((event instanceof TMouseEvent)
                    || (event instanceof TKeypressEvent)) {
                    result.add(event);
                }
            }
            if (System.currentTimeMillis() < deadline) {
                Thread.sleep(10L);
            }
        } while (System.currentTimeMillis() < deadline);
        return result;
    }

    private static void assertKeyChar(final TInputEvent event,
        final int expected) {
        assertTrue(event instanceof TKeypressEvent, String.valueOf(event));
        TKeypressEvent keypress = (TKeypressEvent) event;
        assertFalse(keypress.getKey().isFnKey(), keypress.toString());
        assertEquals(expected, keypress.getKey().getChar(), keypress.toString());
    }

    private List<TMouseEvent> collectMouseEvents(final ECMA48Terminal t,
        final int maxEvents) throws InterruptedException {
        List<TMouseEvent> mouseEvents = new ArrayList<>();
        List<casciian.event.TInputEvent> events = new ArrayList<>();
        long deadline = System.currentTimeMillis() + 500L;
        while (System.currentTimeMillis() < deadline
            && mouseEvents.size() < maxEvents) {
            if (t.hasEvents()) {
                events.clear();
                t.getEvents(events);
                for (casciian.event.TInputEvent event : events) {
                    if (event instanceof TMouseEvent mouseEvent) {
                        mouseEvents.add(mouseEvent);
                    }
                }
            } else {
                Thread.sleep(10L);
            }
        }
        return mouseEvents;
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

        String output = outputStream.toString(StandardCharsets.UTF_8);
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

        String output = outputStream.toString(StandardCharsets.UTF_8);
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

        String output = outputStream.toString(StandardCharsets.UTF_8);

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
