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
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for grapheme-cluster-aware segmentation and display width, covering
 * combining marks, East Asian Wide characters, emoji ZWJ/modifier/flag/keycap
 * sequences, and variation selectors.
 */
@DisplayName("Grapheme cluster width and segmentation")
class GraphemeWidthTest {

    private static String s(final int... codePoints) {
        StringBuilder sb = new StringBuilder();
        for (int cp : codePoints) {
            sb.appendCodePoint(cp);
        }
        return sb.toString();
    }

    private static List<ComplexCell> cells(final String input) {
        return ExtendedGraphemeClusterUtils.toComplexCells(input);
    }

    // Segmentation: each sample below is exactly one grapheme cluster.

    @Test
    @DisplayName("Single-cluster samples segment into one cluster")
    void singleClusterSamples() {
        // Á (A + combining acute)
        assertEquals(1, cells(s(0x41, 0x0301)).size());
        // 中
        assertEquals(1, cells(s(0x4E2D)).size());
        // Supplementary CJK U+20000
        assertEquals(1, cells(s(0x20000)).size());
        // 👍🏽 emoji + skin-tone modifier
        assertEquals(1, cells(s(0x1F44D, 0x1F3FD)).size());
        // 👩‍💻 emoji ZWJ sequence
        assertEquals(1, cells(s(0x1F469, 0x200D, 0x1F4BB)).size());
        // 👨‍👩‍👧‍👦 family ZWJ sequence
        assertEquals(1, cells(s(0x1F468, 0x200D, 0x1F469, 0x200D,
            0x1F467, 0x200D, 0x1F466)).size());
        // 🇺🇸 regional-indicator flag
        assertEquals(1, cells(s(0x1F1FA, 0x1F1F8)).size());
        // 1️⃣ keycap sequence
        assertEquals(1, cells(s(0x31, 0xFE0F, 0x20E3)).size());
        // ©️ base + VS16
        assertEquals(1, cells(s(0x00A9, 0xFE0F)).size());
        // ⚙︎ base + VS15
        assertEquals(1, cells(s(0x2699, 0xFE0E)).size());
        // ⚙️ base + VS16
        assertEquals(1, cells(s(0x2699, 0xFE0F)).size());
    }

    // Width: authoritative grapheme-cluster width.

    @Test
    @DisplayName("Combining sequence has base width 1")
    void combiningWidth() {
        assertEquals(1, StringUtils.width(s(0x41, 0x0301)));
    }

    @Test
    @DisplayName("East Asian Wide characters are width 2")
    void eastAsianWideWidth() {
        assertEquals(2, StringUtils.width(s(0x4E2D)));
        assertEquals(2, StringUtils.width(s(0x20000)));
    }

    @Test
    @DisplayName("Emoji modifier/ZWJ/flag/keycap sequences are width 2")
    void emojiSequenceWidth() {
        assertEquals(2, StringUtils.width(s(0x1F44D, 0x1F3FD)));
        assertEquals(2, StringUtils.width(s(0x1F469, 0x200D, 0x1F4BB)));
        assertEquals(2, StringUtils.width(s(0x1F468, 0x200D, 0x1F469, 0x200D,
            0x1F467, 0x200D, 0x1F466)));
        assertEquals(2, StringUtils.width(s(0x1F1FA, 0x1F1F8)));
        assertEquals(2, StringUtils.width(s(0x31, 0xFE0F, 0x20E3)));
    }

    @Test
    @DisplayName("VS16 forces emoji width 2, VS15 keeps text width")
    void variationSelectorWidth() {
        // © alone is a narrow text glyph.
        assertEquals(1, StringUtils.width(s(0x00A9)));
        // ©️ with VS16 is emoji-wide.
        assertEquals(2, StringUtils.width(s(0x00A9, 0xFE0F)));
        // ⚙︎ with VS15 stays text width.
        assertEquals(1, StringUtils.width(s(0x2699, 0xFE0E)));
        // ⚙️ with VS16 is emoji-wide.
        assertEquals(2, StringUtils.width(s(0x2699, 0xFE0F)));
    }

    @Test
    @DisplayName("ComplexCell.getDisplayWidth matches StringUtils.width")
    void complexCellWidthMatches() {
        for (ComplexCell cell : cells(s(0x1F469, 0x200D, 0x1F4BB))) {
            assertEquals(2, cell.getDisplayWidth());
            assertEquals(StringUtils.width(cell.getCodePoints()),
                cell.getDisplayWidth());
        }
    }

    // Whole-string width sums cluster widths, not codepoint widths.

    @Test
    @DisplayName("String width sums grapheme-cluster widths")
    void stringWidthSumsClusters() {
        // "ÁB" -> 1 + 1 = 2
        assertEquals(2, StringUtils.width(s(0x41, 0x0301, 0x42)));
        // "A👩‍💻B" -> 1 + 2 + 1 = 4
        assertEquals(4, StringUtils.width(s(0x41, 0x1F469, 0x200D,
            0x1F4BB, 0x42)));
        // "🇺🇸X" -> 2 + 1 = 3
        assertEquals(3, StringUtils.width(s(0x1F1FA, 0x1F1F8, 0x58)));
    }
}
