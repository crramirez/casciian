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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link Color} class, in particular the bright (high-intensity)
 * colors that replace the historical "bold == bright" behavior.
 */
@DisplayName("Color Tests")
class ColorTest {

    private static final Color[] BASE = {
        Color.BLACK, Color.RED, Color.GREEN, Color.YELLOW,
        Color.BLUE, Color.MAGENTA, Color.CYAN, Color.WHITE
    };

    private static final Color[] BRIGHT = {
        Color.BRIGHT_BLACK, Color.BRIGHT_RED, Color.BRIGHT_GREEN,
        Color.BRIGHT_YELLOW, Color.BRIGHT_BLUE, Color.BRIGHT_MAGENTA,
        Color.BRIGHT_CYAN, Color.BRIGHT_WHITE
    };

    @Test
    @DisplayName("Bright colors have SGR values 8-15")
    void brightValues() {
        for (int i = 0; i < BASE.length; i++) {
            assertEquals(i, BASE[i].getValue());
            assertEquals(i + 8, BRIGHT[i].getValue());
        }
    }

    @Test
    @DisplayName("isBright distinguishes normal and bright colors")
    void isBright() {
        for (Color c : BASE) {
            assertFalse(c.isBright(), c + " should not be bright");
        }
        for (Color c : BRIGHT) {
            assertTrue(c.isBright(), c + " should be bright");
        }
    }

    @Test
    @DisplayName("toBright/toNormal round-trip between matching colors")
    void brightNormalRoundTrip() {
        for (int i = 0; i < BASE.length; i++) {
            assertEquals(BRIGHT[i], BASE[i].toBright());
            assertEquals(BASE[i], BRIGHT[i].toNormal());
            // Idempotent on already-converted colors.
            assertEquals(BRIGHT[i], BRIGHT[i].toBright());
            assertEquals(BASE[i], BASE[i].toNormal());
        }
    }

    @Test
    @DisplayName("getSgrColor maps 0-15 to the matching color")
    void getSgrColor() {
        for (int i = 0; i < 8; i++) {
            assertEquals(BASE[i], Color.getSgrColor(i));
            assertEquals(BRIGHT[i], Color.getSgrColor(i + 8));
        }
    }

    @Test
    @DisplayName("A bright color renders the same RGB as the legacy bold color")
    void brightRgbMatchesLegacyBold() {
        // Before the change, a bold normal color produced the bright RGB via
        // toRgbString(true). After the change, the bright color produces the
        // same RGB directly.
        for (int i = 0; i < BASE.length; i++) {
            assertEquals(BASE[i].toRgbString(true), BRIGHT[i].toRgbString(false),
                "Bright " + BRIGHT[i] + " should match bold " + BASE[i]);
            assertEquals(BASE[i].toRgbString(true), BRIGHT[i].toRgbString(true));
        }
    }

    @Test
    @DisplayName("toString prefixes bright colors with \"bright\"")
    void toStringBright() {
        assertEquals("red", Color.RED.toString());
        assertEquals("bright red", Color.BRIGHT_RED.toString());
        assertEquals("bright white", Color.BRIGHT_WHITE.toString());
    }

    @Test
    @DisplayName("invert preserves brightness")
    void invertPreservesBrightness() {
        assertEquals(Color.CYAN, Color.RED.invert());
        assertEquals(Color.BRIGHT_CYAN, Color.BRIGHT_RED.invert());
        assertEquals(Color.BRIGHT_WHITE, Color.BRIGHT_BLACK.invert());
    }
}
