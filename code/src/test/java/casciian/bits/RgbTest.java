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

import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Rgb Tests")
class RgbTest {

    @Test
    @DisplayName("distanceSquaredSum matches scalar baseline for multiple lengths")
    void distanceSquaredSumMatchesScalarForMultipleLengths() {
        Random random = new Random(0xC45C11AAL);
        int[] pixels = new int[80];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = random.nextInt() & 0x00FFFFFF;
        }
        int[] colors = {
            0x00000000, 0x00112233, 0x0080A0C0, 0x00FFFFFF
        };

        for (int color : colors) {
            for (int count = 0; count <= pixels.length; count++) {
                long expected = scalarDistanceSquaredSum(pixels, count, color);
                long actual = Rgb.distanceSquaredSum(pixels, count, color);
                assertEquals(expected, actual,
                    "Mismatch for color=0x" + Integer.toHexString(color)
                    + ", count=" + count);
            }
        }
    }

    @Test
    @DisplayName("distanceSquaredSum uses only the requested prefix")
    void distanceSquaredSumUsesOnlyRequestedPrefix() {
        int[] pixels = {
            0x00000000, 0x00112233, 0x00445566, 0x00778899,
            0x00AABBCC, 0x00DDEEFF
        };
        int color = 0x00102030;

        long expected = scalarDistanceSquaredSum(pixels, 4, color);
        long actual = Rgb.distanceSquaredSum(pixels, 4, color);

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("distanceSquaredSum with offset matches scalar baseline across lane boundaries")
    void distanceSquaredSumWithOffsetMatchesScalarBaseline() {
        Random random = new Random(0x5EED1234L);
        int[] pixels = new int[96];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = random.nextInt() & 0x00FFFFFF;
        }
        int color = 0x00335577;

        for (int offset = 0; offset < 20; offset++) {
            for (int count = 0; count <= pixels.length - offset; count++) {
                long expected = scalarDistanceSquaredSum(pixels, offset,
                    count, color);
                long actual = Rgb.distanceSquaredSum(pixels, offset, count,
                    color);
                assertEquals(expected, actual,
                    "Mismatch for offset=" + offset + ", count=" + count);
            }
        }
    }

    @Test
    @DisplayName("distanceSquaredSum does not overflow for large pixel counts")
    void distanceSquaredSumDoesNotOverflowForLargeCounts() {
        // Every pixel is at the maximum possible distance (3 * 255^2) from
        // the reference color, so the running total leaves int range well
        // before the end of the array.
        int count = 200_000;
        int[] pixels = new int[count];
        java.util.Arrays.fill(pixels, 0x00FFFFFF);

        long expected = (long) count * 3 * 255 * 255;

        assertEquals(expected, Rgb.distanceSquaredSum(pixels, count, 0));
    }

    private long scalarDistanceSquaredSum(final int[] pixels,
        final int count, final int color) {

        return scalarDistanceSquaredSum(pixels, 0, count, color);
    }

    private long scalarDistanceSquaredSum(final int[] pixels,
        final int offset, final int count, final int color) {

        long sum = 0;
        for (int i = offset; i < offset + count; i++) {
            int px = pixels[i];
            int dr = ((px >>> 16) & 0xFF) - ((color >>> 16) & 0xFF);
            int dg = ((px >>>  8) & 0xFF) - ((color >>>  8) & 0xFF);
            int db = ( px         & 0xFF) - ( color         & 0xFF);
            sum += dr * dr + dg * dg + db * db;
        }
        return sum;
    }
}
