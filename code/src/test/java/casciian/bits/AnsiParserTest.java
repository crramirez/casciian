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
package casciian.bits;

import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AnsiParser}.
 */
class AnsiParserTest {

    // -----------------------------------------------------------------------
    // Helper to extract text from parsed lines
    // -----------------------------------------------------------------------

    private static String lineText(final AnsiParser.Line line) {
        StringBuilder sb = new StringBuilder();
        for (Cell cell : line.getCells()) {
            sb.appendCodePoint(cell.getChar());
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Null / empty / edge cases
    // -----------------------------------------------------------------------

    @Test
    void testNullInput() {
        List<AnsiParser.Line> lines = AnsiParser.parse(null, 80);
        assertTrue(lines.isEmpty());
    }

    @Test
    void testEmptyInput() {
        List<AnsiParser.Line> lines = AnsiParser.parse("", 80);
        // Empty string yields a single empty line (trailing newline behavior)
        assertEquals(1, lines.size());
        assertEquals(0, lines.get(0).getWidth());
    }

    @Test
    void testZeroWidth() {
        List<AnsiParser.Line> lines = AnsiParser.parse("hello", 0);
        assertTrue(lines.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Plain text
    // -----------------------------------------------------------------------

    @Test
    void testPlainText() {
        List<AnsiParser.Line> lines = AnsiParser.parse("Hello", 80);
        assertEquals(1, lines.size());
        assertEquals("Hello", lineText(lines.get(0)));
    }

    @Test
    void testPlainTextWithNewlines() {
        List<AnsiParser.Line> lines = AnsiParser.parse("Line1\nLine2\nLine3", 80);
        assertEquals(3, lines.size());
        assertEquals("Line1", lineText(lines.get(0)));
        assertEquals("Line2", lineText(lines.get(1)));
        assertEquals("Line3", lineText(lines.get(2)));
    }

    @Test
    void testTrailingNewline() {
        List<AnsiParser.Line> lines = AnsiParser.parse("Hello\n", 80);
        assertEquals(2, lines.size());
        assertEquals("Hello", lineText(lines.get(0)));
        assertEquals(0, lines.get(1).getWidth());
    }

    // -----------------------------------------------------------------------
    // Wrapping
    // -----------------------------------------------------------------------

    @Test
    void testWrappingAtWidth() {
        List<AnsiParser.Line> lines = AnsiParser.parse("ABCDE", 3);
        assertEquals(2, lines.size());
        assertEquals("ABC", lineText(lines.get(0)));
        assertEquals("DE", lineText(lines.get(1)));
    }

    @Test
    void testExactWidthNoWrap() {
        List<AnsiParser.Line> lines = AnsiParser.parse("ABC", 3);
        // Exactly fills one line; wrapping is delayed (ECMA-48/VT100) so no
        // trailing empty line is produced when the input ends on the boundary.
        assertEquals(1, lines.size());
        assertEquals("ABC", lineText(lines.get(0)));
    }

    // -----------------------------------------------------------------------
    // Tabs
    // -----------------------------------------------------------------------

    @Test
    void testTabExpansion() {
        List<AnsiParser.Line> lines = AnsiParser.parse("A\tB", 80);
        assertEquals(1, lines.size());
        // 'A' at col 0, tab to col 8, 'B' at col 8
        assertEquals(9, lines.get(0).getWidth());
        assertEquals('A', lines.get(0).getCells().get(0).getChar());
        assertEquals('B', lines.get(0).getCells().get(8).getChar());
    }

    // -----------------------------------------------------------------------
    // Carriage return
    // -----------------------------------------------------------------------

    @Test
    void testCarriageReturn() {
        List<AnsiParser.Line> lines = AnsiParser.parse("AB\rX", 80);
        assertEquals(1, lines.size());
        // CR resets to column 0, 'X' overwrites 'A'
        assertEquals("XB", lineText(lines.get(0)));
    }

    // -----------------------------------------------------------------------
    // Backspace
    // -----------------------------------------------------------------------

    @Test
    void testBackspace() {
        List<AnsiParser.Line> lines = AnsiParser.parse("AB\bC", 80);
        assertEquals(1, lines.size());
        // BS moves cursor back, 'C' overwrites 'B'
        assertEquals("AC", lineText(lines.get(0)));
    }

    @Test
    void testBackspaceAtBeginning() {
        List<AnsiParser.Line> lines = AnsiParser.parse("\bA", 80);
        assertEquals(1, lines.size());
        assertEquals("A", lineText(lines.get(0)));
    }

    // -----------------------------------------------------------------------
    // Default color tracking
    // -----------------------------------------------------------------------

    @Test
    void testDefaultColorFlagsOnPlainText() {
        List<AnsiParser.Line> lines = AnsiParser.parse("A", 80);
        Cell cell = lines.get(0).getCells().get(0);
        assertTrue(cell.isDefaultColor(true), "foreground should be default");
        assertTrue(cell.isDefaultColor(false), "background should be default");
    }

    @Test
    void testDefaultColorClearedByExplicitColor() {
        // ESC[31m sets foreground to red
        List<AnsiParser.Line> lines = AnsiParser.parse("\033[31mA", 80);
        Cell cell = lines.get(0).getCells().get(0);
        assertFalse(cell.isDefaultColor(true),
            "foreground should not be default after ESC[31m");
        assertTrue(cell.isDefaultColor(false),
            "background should still be default");
        assertEquals(Color.RED, cell.getForeColor());
    }

    @Test
    void testResetRestoresDefaultColor() {
        // Set red, then reset, then character
        List<AnsiParser.Line> lines = AnsiParser.parse(
            "\033[31mR\033[0mA", 80);
        Cell redCell = lines.get(0).getCells().get(0);
        assertFalse(redCell.isDefaultColor(true));
        assertEquals(Color.RED, redCell.getForeColor());

        Cell resetCell = lines.get(0).getCells().get(1);
        assertTrue(resetCell.isDefaultColor(true),
            "foreground should be default after ESC[0m");
        assertTrue(resetCell.isDefaultColor(false),
            "background should be default after ESC[0m");
    }

    @Test
    void testEscMNoParamsResetsToDefault() {
        // ESC[m (no params) should reset like ESC[0m
        List<AnsiParser.Line> lines = AnsiParser.parse(
            "\033[31mR\033[mA", 80);
        Cell resetCell = lines.get(0).getCells().get(1);
        assertTrue(resetCell.isDefaultColor(true));
        assertTrue(resetCell.isDefaultColor(false));
    }

    @Test
    void testSgr39RestoresForegroundDefault() {
        List<AnsiParser.Line> lines = AnsiParser.parse(
            "\033[31mR\033[39mA", 80);
        Cell cell = lines.get(0).getCells().get(1);
        assertTrue(cell.isDefaultColor(true),
            "foreground should be default after ESC[39m");
    }

    @Test
    void testSgr49RestoresBackgroundDefault() {
        List<AnsiParser.Line> lines = AnsiParser.parse(
            "\033[41mR\033[49mA", 80);
        Cell cell = lines.get(0).getCells().get(1);
        assertTrue(cell.isDefaultColor(false),
            "background should be default after ESC[49m");
    }

    // -----------------------------------------------------------------------
    // SGR attributes: bold, underline, blink, reverse
    // -----------------------------------------------------------------------

    @Test
    void testBold() {
        List<AnsiParser.Line> lines = AnsiParser.parse("\033[1mA", 80);
        assertTrue(lines.get(0).getCells().get(0).isBold());
    }

    @Test
    void testBoldNotTransparentInWidget() {
        // The ANSI viewer widget must NOT mark bold as transparent (that is
        // reserved for the ECMA48 terminal emulator), so the bold attribute
        // remains subject to the treatBoldAsBright system property.
        List<AnsiParser.Line> lines = AnsiParser.parse("\033[1mA", 80);
        assertFalse(lines.get(0).getCells().get(0).isBoldTransparent());
    }

    @Test
    void testBoldReset() {
        List<AnsiParser.Line> lines = AnsiParser.parse(
            "\033[1mA\033[22mB", 80);
        assertTrue(lines.get(0).getCells().get(0).isBold());
        assertFalse(lines.get(0).getCells().get(1).isBold());
    }

    @Test
    void testUnderline() {
        List<AnsiParser.Line> lines = AnsiParser.parse("\033[4mA", 80);
        assertTrue(lines.get(0).getCells().get(0).isUnderline());
    }

    @Test
    void testBlink() {
        List<AnsiParser.Line> lines = AnsiParser.parse("\033[5mA", 80);
        assertTrue(lines.get(0).getCells().get(0).isBlink());
    }

    @Test
    void testReverse() {
        List<AnsiParser.Line> lines = AnsiParser.parse("\033[7mA", 80);
        assertTrue(lines.get(0).getCells().get(0).isReverse());
    }

    // -----------------------------------------------------------------------
    // SGR foreground and background colors (standard 8)
    // -----------------------------------------------------------------------

    @Test
    void testStandardForegroundColors() {
        Color[] expected = { Color.BLACK, Color.RED, Color.GREEN,
            Color.YELLOW, Color.BLUE, Color.MAGENTA, Color.CYAN,
            Color.WHITE };
        for (int i = 0; i < expected.length; i++) {
            int code = 30 + i;
            List<AnsiParser.Line> lines = AnsiParser.parse(
                "\033[" + code + "mA", 80);
            assertEquals(expected[i],
                lines.get(0).getCells().get(0).getForeColor(),
                "SGR " + code + " should set foreground to " + expected[i]);
        }
    }

    @Test
    void testStandardBackgroundColors() {
        Color[] expected = { Color.BLACK, Color.RED, Color.GREEN,
            Color.YELLOW, Color.BLUE, Color.MAGENTA, Color.CYAN,
            Color.WHITE };
        for (int i = 0; i < expected.length; i++) {
            int code = 40 + i;
            List<AnsiParser.Line> lines = AnsiParser.parse(
                "\033[" + code + "mA", 80);
            assertEquals(expected[i],
                lines.get(0).getCells().get(0).getBackColor(),
                "SGR " + code + " should set background to " + expected[i]);
        }
    }

    // -----------------------------------------------------------------------
    // Combined SGR parameters
    // -----------------------------------------------------------------------

    @Test
    void testCombinedSgrParams() {
        // ESC[1;31;42m = bold + red foreground + green background
        List<AnsiParser.Line> lines = AnsiParser.parse(
            "\033[1;31;42mA", 80);
        Cell cell = lines.get(0).getCells().get(0);
        assertTrue(cell.isBold());
        assertEquals(Color.RED, cell.getForeColor());
        assertEquals(Color.GREEN, cell.getBackColor());
    }

    // -----------------------------------------------------------------------
    // 256-color support
    // -----------------------------------------------------------------------

    @Test
    void testForeground256Color() {
        // ESC[38;5;196m = 256-color foreground index 196
        List<AnsiParser.Line> lines = AnsiParser.parse(
            "\033[38;5;196mA", 80);
        Cell cell = lines.get(0).getCells().get(0);
        assertFalse(cell.isDefaultColor(true));
        assertTrue(cell.getForeColorPalette() >= 0);
        assertEquals(196, cell.getForeColorPalette());
        assertEquals(-1, cell.getForeColorRGB());
    }

    @Test
    void testBackground256Color() {
        // ESC[48;5;21m = 256-color background index 21
        List<AnsiParser.Line> lines = AnsiParser.parse(
            "\033[48;5;21mA", 80);
        Cell cell = lines.get(0).getCells().get(0);
        assertFalse(cell.isDefaultColor(false));
        assertTrue(cell.getBackColorPalette() >= 0);
        assertEquals(21, cell.getBackColorPalette());
        assertEquals(-1, cell.getBackColorRGB());
    }

    // -----------------------------------------------------------------------
    // RGB (24-bit) color support
    // -----------------------------------------------------------------------

    @Test
    void testForegroundRGB() {
        // ESC[38;2;255;128;0m = RGB foreground
        List<AnsiParser.Line> lines = AnsiParser.parse(
            "\033[38;2;255;128;0mA", 80);
        Cell cell = lines.get(0).getCells().get(0);
        assertFalse(cell.isDefaultColor(true));
        assertEquals(0xFF8000, cell.getForeColorRGB());
    }

    @Test
    void testBackgroundRGB() {
        // ESC[48;2;0;128;255m = RGB background
        List<AnsiParser.Line> lines = AnsiParser.parse(
            "\033[48;2;0;128;255mA", 80);
        Cell cell = lines.get(0).getCells().get(0);
        assertFalse(cell.isDefaultColor(false));
        assertEquals(0x0080FF, cell.getBackColorRGB());
    }

    // -----------------------------------------------------------------------
    // High-intensity colors (90-97, 100-107)
    // -----------------------------------------------------------------------

    @Test
    void testHighIntensityForeground() {
        // ESC[91m = bright red foreground
        List<AnsiParser.Line> lines = AnsiParser.parse("\033[91mA", 80);
        Cell cell = lines.get(0).getCells().get(0);
        assertFalse(cell.isDefaultColor(true));
        assertEquals(casciian.bits.Color.BRIGHT_RED, cell.getForeColor());
        assertEquals(-1, cell.getForeColorRGB());
    }

    @Test
    void testHighIntensityBackground() {
        // ESC[101m = bright red background
        List<AnsiParser.Line> lines = AnsiParser.parse("\033[101mA", 80);
        Cell cell = lines.get(0).getCells().get(0);
        assertFalse(cell.isDefaultColor(false));
        assertEquals(casciian.bits.Color.BRIGHT_RED, cell.getBackColor());
        assertEquals(-1, cell.getBackColorRGB());
    }

    // -----------------------------------------------------------------------
    // OSC sequences should be skipped
    // -----------------------------------------------------------------------

    @Test
    void testOscSequenceSkipped() {
        // OSC sequence terminated by BEL
        List<AnsiParser.Line> lines = AnsiParser.parse(
            "\033]0;title\007Hello", 80);
        assertEquals(1, lines.size());
        assertEquals("Hello", lineText(lines.get(0)));
    }

    @Test
    void testOscSequenceSkippedST() {
        // OSC sequence terminated by ST (ESC \)
        List<AnsiParser.Line> lines = AnsiParser.parse(
            "\033]0;title\033\\Hello", 80);
        assertEquals(1, lines.size());
        assertEquals("Hello", lineText(lines.get(0)));
    }

    // -----------------------------------------------------------------------
    // Non-SGR CSI sequences should be ignored
    // -----------------------------------------------------------------------

    @Test
    void testNonSgrCsiIgnored() {
        // ESC[2J (clear screen) should be silently ignored
        List<AnsiParser.Line> lines = AnsiParser.parse("\033[2JHello", 80);
        assertEquals(1, lines.size());
        assertEquals("Hello", lineText(lines.get(0)));
    }

    // -----------------------------------------------------------------------
    // Padding cells have default color
    // -----------------------------------------------------------------------

    @Test
    void testPaddingCellsHaveDefaultColor() {
        // Start writing at column 3 by using CR to overwrite
        // Simpler: use tab which fills gaps
        List<AnsiParser.Line> lines = AnsiParser.parse("\tA", 80);
        // Padding cells (columns 0-6) should have default color
        Cell padCell = lines.get(0).getCells().get(0);
        assertTrue(padCell.isDefaultColor(true));
        assertTrue(padCell.isDefaultColor(false));
    }

    // -----------------------------------------------------------------------
    // Line width
    // -----------------------------------------------------------------------

    @Test
    void testLineWidthReturnsCorrectCount() {
        List<AnsiParser.Line> lines = AnsiParser.parse("ABCDE", 80);
        assertEquals(5, lines.get(0).getWidth());
    }

    // -----------------------------------------------------------------------
    // Multiple lines with color state carried across lines
    // -----------------------------------------------------------------------

    @Test
    void testColorStatePersistsAcrossLines() {
        List<AnsiParser.Line> lines = AnsiParser.parse(
            "\033[31mLine1\nLine2", 80);
        assertEquals(2, lines.size());
        // Line2 should still be red
        Cell cell = lines.get(1).getCells().get(0);
        assertEquals(Color.RED, cell.getForeColor());
        assertFalse(cell.isDefaultColor(true));
    }

    // -----------------------------------------------------------------------
    // Bold with default color preserved
    // -----------------------------------------------------------------------

    @Test
    void testBoldWithDefaultColor() {
        List<AnsiParser.Line> lines = AnsiParser.parse("\033[1mA", 80);
        Cell cell = lines.get(0).getCells().get(0);
        assertTrue(cell.isBold());
        assertTrue(cell.isDefaultColor(true),
            "bold should keep default foreground flag");
        assertTrue(cell.isDefaultColor(false),
            "bold should keep default background flag");
    }
}
