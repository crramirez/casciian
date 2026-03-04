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

import casciian.bits.ImageRGB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the pseudo image (Unicode half-block) fallback rendering.
 */
@DisplayName("Pseudo Image Tests")
class PseudoImageTest {

    @Test
    @DisplayName("averageColor returns the single color for a uniform region")
    void averageColorUniform() {
        ImageRGB image = new ImageRGB(4, 4);
        image.fillRect(0, 0, 4, 4, 0xFF8040);

        int avg = ECMA48Terminal.averageColor(image, 0, 0, 4, 4);
        assertEquals(0xFF8040, avg);
    }

    @Test
    @DisplayName("averageColor computes correct average of two colors")
    void averageColorTwoColors() {
        // 2x1 image: left pixel = pure red, right pixel = pure blue
        ImageRGB image = new ImageRGB(2, 1);
        image.setRGB(0, 0, 0xFF0000);
        image.setRGB(1, 0, 0x0000FF);

        int avg = ECMA48Terminal.averageColor(image, 0, 0, 2, 1);
        // R: (255+0)/2=127, G: 0, B: (0+255)/2=127
        int r = (avg >>> 16) & 0xFF;
        int g = (avg >>> 8) & 0xFF;
        int b = avg & 0xFF;
        assertEquals(127, r);
        assertEquals(0, g);
        assertEquals(127, b);
    }

    @Test
    @DisplayName("averageColor computes average over a sub-region")
    void averageColorSubregion() {
        ImageRGB image = new ImageRGB(4, 4);
        // Fill entire image with black
        image.fillRect(0, 0, 4, 4, 0x000000);
        // Fill top-left 2x2 with white
        image.fillRect(0, 0, 2, 2, 0xFFFFFF);

        // Average of top-left 2x2 (all white)
        int avgWhite = ECMA48Terminal.averageColor(image, 0, 0, 2, 2);
        assertEquals(0xFFFFFF, avgWhite);

        // Average of bottom-right 2x2 (all black)
        int avgBlack = ECMA48Terminal.averageColor(image, 2, 2, 2, 2);
        assertEquals(0x000000, avgBlack);
    }

    @Test
    @DisplayName("averageColor handles region clamped to image bounds")
    void averageColorClampedBounds() {
        ImageRGB image = new ImageRGB(2, 2);
        image.fillRect(0, 0, 2, 2, 0x804020);

        // Request region larger than image - should clamp
        int avg = ECMA48Terminal.averageColor(image, 0, 0, 10, 10);
        assertEquals(0x804020, avg);
    }

    @Test
    @DisplayName("averageColor returns black for zero-sized region")
    void averageColorEmptyRegion() {
        ImageRGB image = new ImageRGB(4, 4);
        image.fillRect(0, 0, 4, 4, 0xFFFFFF);

        // Region starting beyond image bounds → count=0 → returns 0
        int avg = ECMA48Terminal.averageColor(image, 10, 10, 1, 1);
        assertEquals(0x000000, avg);
    }

    @Test
    @DisplayName("averageColor correctly averages four different colors")
    void averageColorFourColors() {
        ImageRGB image = new ImageRGB(2, 2);
        image.setRGB(0, 0, 0xFF0000); // red
        image.setRGB(1, 0, 0x00FF00); // green
        image.setRGB(0, 1, 0x0000FF); // blue
        image.setRGB(1, 1, 0xFFFFFF); // white

        int avg = ECMA48Terminal.averageColor(image, 0, 0, 2, 2);
        int r = (avg >>> 16) & 0xFF;
        int g = (avg >>> 8) & 0xFF;
        int b = avg & 0xFF;
        // R: (255+0+0+255)/4 = 127
        // G: (0+255+0+255)/4 = 127
        // B: (0+0+255+255)/4 = 127
        assertEquals(127, r);
        assertEquals(127, g);
        assertEquals(127, b);
    }
}
