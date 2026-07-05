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
import static org.junit.jupiter.api.Assertions.assertTrue;

class Palette256Test {

    @Test
    @DisplayName("The 16 CGA colors map to palette indices 0-15")
    void cgaColorsMapToFirstSixteenIndices() {
        assertEquals(0, Palette256.fromColor(Color.BLACK));
        assertEquals(1, Palette256.fromColor(Color.RED));
        assertEquals(2, Palette256.fromColor(Color.GREEN));
        assertEquals(3, Palette256.fromColor(Color.YELLOW));
        assertEquals(4, Palette256.fromColor(Color.BLUE));
        assertEquals(5, Palette256.fromColor(Color.MAGENTA));
        assertEquals(6, Palette256.fromColor(Color.CYAN));
        assertEquals(7, Palette256.fromColor(Color.WHITE));
        assertEquals(8, Palette256.fromColor(Color.BRIGHT_BLACK));
        assertEquals(15, Palette256.fromColor(Color.BRIGHT_WHITE));
    }

    @Test
    @DisplayName("fromColor can request the bright variant of a normal color")
    void brightVariantAddsEight() {
        assertEquals(10, Palette256.fromColor(Color.GREEN, true));
        assertEquals(2, Palette256.fromColor(Color.GREEN, false));
        // Already-bright colors stay bright regardless of the flag.
        assertEquals(15, Palette256.fromColor(Color.BRIGHT_WHITE, false));
    }

    @Test
    @DisplayName("Exact color-cube colors round-trip to their own index")
    void exactCubeColorRoundTrips() {
        // Index 16 is the cube origin (0,0,0 -> black).
        assertEquals(16, Palette256.fromRgb(0x000000));
        // Index 196 is pure red (5,0,0) -> 0xFF0000.
        int pureRed = Palette256.toRgb(196);
        assertEquals(196, Palette256.fromRgb(pureRed));
        // Index 21 is pure blue (0,0,5) -> 0x0000FF.
        int pureBlue = Palette256.toRgb(21);
        assertEquals(21, Palette256.fromRgb(pureBlue));
    }

    @Test
    @DisplayName("fromRgb picks the grayscale ramp for neutral grays")
    void neutralGrayUsesGrayscaleRamp() {
        // 0x808080 is a mid gray that is closer to the grayscale ramp than to
        // any cube entry.
        int index = Palette256.fromRgb(0x808080);
        assertTrue(index >= 232 && index <= 255,
            "Expected a grayscale-ramp index (232-255) but got " + index);
    }

    @Test
    @DisplayName("fromRgb always returns an in-range palette index")
    void fromRgbAlwaysInRange() {
        for (int rgb = 0; rgb <= 0xFFFFFF; rgb += 7919) {
            int index = Palette256.fromRgb(rgb);
            assertTrue(index >= 16 && index <= 255,
                "Index out of range for #" + Integer.toHexString(rgb)
                    + ": " + index);
        }
    }

    @Test
    @DisplayName("toRgb matches the shared xterm palette definition")
    void toRgbMatchesSharedPalette() {
        for (int i = 0; i < Palette256.SIZE; i++) {
            assertEquals(SgrUtil.getDefaultIndexedColor(i), Palette256.toRgb(i),
                "Mismatch at palette index " + i);
        }
    }
}
