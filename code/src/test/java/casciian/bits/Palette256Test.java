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
    @DisplayName("fromRgb keeps saturated colors on the color cube instead of "
        + "flattening them to gray")
    void saturatedColorDoesNotCollapseToGray() {
        // 0x1f3a5f (a muted navy-blue theme color) has real chroma, but the
        // grayscale ramp used to be a marginally closer numeric RGB match,
        // causing it to render as flat gray instead of blue.
        int index = Palette256.fromRgb(0x1f3a5f);
        assertTrue(index < 232,
            "Expected a color-cube index (16-231) but got grayscale index "
                + index);
    }

    @Test
    @DisplayName("fromCgaColor maps CGA colors into the cube/grayscale range")
    void cgaColorsMapIntoCube() {
        for (Color color : new Color[] {
            Color.BLACK, Color.RED, Color.GREEN, Color.YELLOW,
            Color.BLUE, Color.MAGENTA, Color.CYAN, Color.WHITE}) {

            int index = Palette256.fromCgaColor(color);
            assertTrue(index >= 16 && index <= 255,
                "Expected a cube/grayscale index (16-255) for " + color
                    + " but got " + index);
            // The mapped index must match the nearest cube entry for the
            // CGA color's fixed RGB value.
            int cgaRgb = SgrUtil.getDefaultIndexedColor(
                Palette256.fromColor(color));
            assertEquals(Palette256.fromRgb(cgaRgb), index);
        }
    }

    @Test
    @DisplayName("fromCgaColor can request the bright variant")
    void cgaBrightVariantMapsIntoCube() {
        int normal = Palette256.fromCgaColor(Color.GREEN, false);
        int bright = Palette256.fromCgaColor(Color.GREEN, true);
        assertEquals(Palette256.fromRgb(
            SgrUtil.getDefaultIndexedColor(10)), bright);
        assertEquals(Palette256.fromRgb(
            SgrUtil.getDefaultIndexedColor(2)), normal);
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

    @Test
    @DisplayName("findExact returns the palette index for a known cube color")
    void findExactReturnsCubeIndex() {
        // Index 196 is pure red (0xFF0000) in the 6×6×6 cube.
        assertEquals(196, Palette256.findExact(0xFF0000));
        // Index 21 is pure blue (0x0000FF).
        assertEquals(21, Palette256.findExact(0x0000FF));
    }

    @Test
    @DisplayName("findExact returns -1 for a non-palette RGB value")
    void findExactReturnsMinusOneForNonMatch() {
        // 0x123456 is extremely unlikely to be an exact palette entry.
        assertEquals(-1, Palette256.findExact(0x123456));
    }

    @Test
    @DisplayName("findExact never returns an index below 16")
    void findExactSkipsBaseColors() {
        // Black (0x000000) exists as both index 0 (CGA black) and index 16
        // (cube origin). findExact must return 16, not 0.
        int index = Palette256.findExact(0x000000);
        assertTrue(index >= 16,
            "findExact must not return a base-16 index, got " + index);
        assertEquals(16, index);
    }

    @Test
    @DisplayName("findExact normalizes extra bits before comparing")
    void findExactNormalizesInput() {
        // A value with bits above 0xFFFFFF set should still match index 21.
        assertEquals(21, Palette256.findExact(0xFF0000FF));
    }

    @Test
    @DisplayName("fromRgb returns the same index whether cached or freshly "
        + "computed")
    void fromRgbCacheIsConsistent() {
        // The first call populates the cache; subsequent calls must return
        // the exact same index. Exercise more than the cache capacity so the
        // LRU eviction path is also covered.
        for (int rgb = 0; rgb <= 0xFFFFFF; rgb += 5003) {
            int first = Palette256.fromRgb(rgb);
            int second = Palette256.fromRgb(rgb);
            assertEquals(first, second,
                "Cached result differs for #" + Integer.toHexString(rgb));
        }
    }
}
