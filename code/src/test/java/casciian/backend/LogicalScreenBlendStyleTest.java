/*
 * Casciian - Java Text User Interface
 *
 * Copyright 2025 Carlos Rafael Ramirez
 *
 * Licensed under the License, Version 2.0 (the "License");
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that {@link LogicalScreen#blendScreen} preserves text style flags
 * (italic, faint, strikethrough, reverse, in addition to bold/blink/
 * underline/protect) when compositing a translucent overlay over an
 * underlying screen.
 */
@DisplayName("LogicalScreen.blendScreen preserves text styles")
class LogicalScreenBlendStyleTest {

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
    private CellAttributes baseAttr;

    @BeforeEach
    void setUp() {
        screen = new TestableLogicalScreen(5, 3);
        baseAttr = new CellAttributes();
    }

    @Test
    @DisplayName("Translucent blend keeps italic, faint, strikethrough and reverse from the overlay")
    void blendPreservesNewStyleFlags() {
        screen.putCharXY(0, 0, ' ', baseAttr);

        CellAttributes overAttr = new CellAttributes();
        overAttr.setItalic(true);
        overAttr.setFaint(true);
        overAttr.setStrikethrough(true);
        overAttr.setReverse(true);
        overAttr.setBold(true);
        overAttr.setBlink(true);
        overAttr.setUnderline(true);
        overAttr.setProtect(true);

        TestableLogicalScreen over = new TestableLogicalScreen(1, 1);
        over.putCharXY(0, 0, 'X', overAttr);

        // Translucent (alpha < 255) blend must not drop the new style flags.
        screen.blendScreen(over, 0, 0, 1, 1, 128, false);

        assertEquals('X', screen.getCharXY(0, 0).getChar());
        assertTrue(screen.getCharXY(0, 0).isItalic(), "italic should survive blend");
        assertTrue(screen.getCharXY(0, 0).isFaint(), "faint should survive blend");
        assertTrue(screen.getCharXY(0, 0).isStrikethrough(),
            "strikethrough should survive blend");
        assertTrue(screen.getCharXY(0, 0).isReverse(), "reverse should survive blend");
        assertTrue(screen.getCharXY(0, 0).isBold(), "bold should survive blend");
        assertTrue(screen.getCharXY(0, 0).isBlink(), "blink should survive blend");
        assertTrue(screen.getCharXY(0, 0).isUnderline(), "underline should survive blend");
        assertTrue(screen.getCharXY(0, 0).isProtect(), "protect should survive blend");
    }
}
