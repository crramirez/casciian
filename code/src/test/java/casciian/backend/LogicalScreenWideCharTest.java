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

import casciian.bits.Cell;
import casciian.bits.CellAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for placement of extended grapheme clusters (wide characters) on the
 * LogicalScreen, including right-edge handling and stale-cell cleanup when a
 * wide cell is replaced.
 */
@DisplayName("LogicalScreen wide-character placement")
class LogicalScreenWideCharTest {

    private static class TestableLogicalScreen extends LogicalScreen {
        TestableLogicalScreen(final int width, final int height) {
            super(width, height);
        }

        @Override
        public void flushPhysical() {
            // no-op for tests
        }
    }

    private TestableLogicalScreen screen;
    private CellAttributes attr;

    private static String s(final int... codePoints) {
        StringBuilder sb = new StringBuilder();
        for (int cp : codePoints) {
            sb.appendCodePoint(cp);
        }
        return sb.toString();
    }

    @BeforeEach
    void setUp() {
        screen = new TestableLogicalScreen(5, 3);
        attr = new CellAttributes();
    }

    @Test
    @DisplayName("Wide char placed as LEFT/RIGHT with three columns free")
    void wideCharPlacement() {
        screen.putStringXY(3, 0, s(0x4E2D), attr);
        assertEquals(Cell.Width.LEFT, screen.getCharXY(3, 0).getWidth());
        assertEquals(0x4E2D, screen.getCharXY(3, 0).getChar());
        assertEquals(Cell.Width.RIGHT, screen.getCharXY(4, 0).getWidth());
    }

    @Test
    @DisplayName("Wide char at last column is not partially placed")
    void wideCharRightEdgeNotPlaced() {
        // Fill both cells with a known single-width character first.
        screen.putCharXY(4, 0, 'Z', attr);
        // Attempt to place a wide char at the final column (only 1 free).
        screen.putStringXY(4, 0, s(0x4E2D), attr);
        // The wide char must not be written at all.
        assertEquals(Cell.Width.SINGLE, screen.getCharXY(4, 0).getWidth());
        assertEquals('Z', screen.getCharXY(4, 0).getChar());
    }

    @Test
    @DisplayName("Grapheme string places A, wide LEFT/RIGHT, then B")
    void mixedStringPlacement() {
        // "A中B"
        screen.putStringXY(0, 0, s(0x41, 0x4E2D, 0x42), attr);
        assertEquals('A', screen.getCharXY(0, 0).getChar());
        assertEquals(Cell.Width.SINGLE, screen.getCharXY(0, 0).getWidth());
        assertEquals(Cell.Width.LEFT, screen.getCharXY(1, 0).getWidth());
        assertEquals(Cell.Width.RIGHT, screen.getCharXY(2, 0).getWidth());
        assertEquals('B', screen.getCharXY(3, 0).getChar());
        assertEquals(Cell.Width.SINGLE, screen.getCharXY(3, 0).getWidth());
    }

    @Test
    @DisplayName("Wide replaced by narrow clears the stale RIGHT half")
    void wideReplacedByNarrow() {
        screen.putStringXY(0, 0, s(0x4E2D), attr);
        assertEquals(Cell.Width.LEFT, screen.getCharXY(0, 0).getWidth());
        assertEquals(Cell.Width.RIGHT, screen.getCharXY(1, 0).getWidth());

        // Overwrite the left half with a narrow character.
        screen.putCharXY(0, 0, 'X', attr);
        assertEquals(Cell.Width.SINGLE, screen.getCharXY(0, 0).getWidth());
        // The former RIGHT half must no longer claim to be a right half.
        assertNotEquals(Cell.Width.RIGHT, screen.getCharXY(1, 0).getWidth());
    }

    @Test
    @DisplayName("Wide replaced by another wide keeps a clean LEFT/RIGHT pair")
    void wideReplacedByWide() {
        screen.putStringXY(0, 0, s(0x4E2D), attr);
        screen.putStringXY(0, 0, s(0x4E00), attr);
        assertEquals(0x4E00, screen.getCharXY(0, 0).getChar());
        assertEquals(Cell.Width.LEFT, screen.getCharXY(0, 0).getWidth());
        assertEquals(Cell.Width.RIGHT, screen.getCharXY(1, 0).getWidth());
    }

    @Test
    @DisplayName("Wide char whose right half is clipped at the screen edge is blanked, not stranded")
    void wideCharRightHalfClippedIsBlanked() {
        // Place a wide char directly at the final column: its RIGHT half
        // would fall at x == width, off the screen.  A stranded LEFT half
        // would make the terminal emit an overflowing half glyph, so it must
        // be replaced with a blank single-width cell instead.
        screen.putCharXY(4, 0, 0x4E2D, attr);
        assertEquals(Cell.Width.SINGLE, screen.getCharXY(4, 0).getWidth());
        assertEquals(' ', screen.getCharXY(4, 0).getChar());
    }

    @Test
    @DisplayName("Translucent overlay on the RIGHT half of a wide char repairs the orphaned LEFT half")
    void blendOverlayRepairsOrphanedLeftHalf() {
        // Wide char at columns 2-3 (LEFT/RIGHT).
        screen.putCharXY(2, 0, 0x4E2D, attr);
        assertEquals(Cell.Width.LEFT, screen.getCharXY(2, 0).getWidth());
        assertEquals(Cell.Width.RIGHT, screen.getCharXY(3, 0).getWidth());

        // Overlay a visible glyph over columns 3-4 (only the RIGHT half).
        TestableLogicalScreen over = new TestableLogicalScreen(2, 1);
        over.putCharXY(0, 0, 'X', attr);
        over.putCharXY(1, 0, 'Y', attr);
        screen.blendScreen(over, 3, 0, 2, 1, 128, false);

        // The RIGHT half was overwritten; the now-orphaned LEFT half at
        // column 2 must be blanked so no stale wide glyph is emitted.
        assertEquals('X', screen.getCharXY(3, 0).getChar());
        assertEquals(Cell.Width.SINGLE, screen.getCharXY(2, 0).getWidth());
        assertEquals(' ', screen.getCharXY(2, 0).getChar());
    }

    @Test
    @DisplayName("Translucent overlay on the LEFT half of a wide char repairs the orphaned RIGHT half")
    void blendOverlayRepairsOrphanedRightHalf() {
        // Wide char at columns 2-3 (LEFT/RIGHT).
        screen.putCharXY(2, 0, 0x4E2D, attr);

        // Overlay a visible glyph over columns 1-2 (only the LEFT half).
        TestableLogicalScreen over = new TestableLogicalScreen(2, 1);
        over.putCharXY(0, 0, 'X', attr);
        over.putCharXY(1, 0, 'Z', attr);
        screen.blendScreen(over, 1, 0, 2, 1, 128, false);

        // The LEFT half was overwritten; the orphaned RIGHT half at column 3
        // must be blanked.
        assertEquals('Z', screen.getCharXY(2, 0).getChar());
        assertNotEquals(Cell.Width.RIGHT, screen.getCharXY(3, 0).getWidth());
        assertEquals(' ', screen.getCharXY(3, 0).getChar());
    }

    @Test
    @DisplayName("Translucent overlay straddling a wide char boundary blanks the whole char")
    void blendStraddlingBoundaryBlanksWholeWideChar() {
        // Wide char at columns 1-2 (LEFT/RIGHT).
        screen.putCharXY(1, 0, 0x4E2D, attr);
        assertEquals(Cell.Width.LEFT, screen.getCharXY(1, 0).getWidth());
        assertEquals(Cell.Width.RIGHT, screen.getCharXY(2, 0).getWidth());

        // Overlay two invisible (translucent) space cells over columns 2-3, so
        // only the RIGHT half (column 2) is inside the composited region while
        // the LEFT half (column 1) stays outside.  The wide glyph would spill
        // across the region edge and paint over the overlay; both halves must
        // be blanked instead.
        TestableLogicalScreen over = new TestableLogicalScreen(2, 1);
        screen.blendScreen(over, 2, 0, 2, 1, 128, false);

        assertEquals(Cell.Width.SINGLE, screen.getCharXY(1, 0).getWidth());
        assertEquals(' ', screen.getCharXY(1, 0).getChar());
        assertEquals(Cell.Width.SINGLE, screen.getCharXY(2, 0).getWidth());
        assertEquals(' ', screen.getCharXY(2, 0).getChar());
    }

    @Test
    @DisplayName("Invisible (space) translucent overlay preserves a fully covered wide char")
    void blendInvisibleOverlayPreservesWideChar() {
        // Wide char at columns 2-3 (LEFT/RIGHT).
        screen.putCharXY(2, 0, 0x4E2D, attr);

        // Overlay two invisible space cells fully over the wide char.
        TestableLogicalScreen over = new TestableLogicalScreen(2, 1);
        screen.blendScreen(over, 2, 0, 2, 1, 128, false);

        // Both halves must remain intact so the wide char shows through.
        assertEquals(Cell.Width.LEFT, screen.getCharXY(2, 0).getWidth());
        assertEquals(0x4E2D, screen.getCharXY(2, 0).getChar());
        assertEquals(Cell.Width.RIGHT, screen.getCharXY(3, 0).getWidth());
    }
}
