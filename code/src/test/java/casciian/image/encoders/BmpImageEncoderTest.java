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
package casciian.image.encoders;

import casciian.bits.ArrayImageRGB;
import casciian.bits.ImageRGB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the built-in BMP encoder.
 */
@DisplayName("BmpImageEncoder")
public class BmpImageEncoderTest {

    /**
     * A decoded BMP.
     *
     * @param bitsPerPixel 24 or 32
     * @param headerSize the DIB header size
     * @param compression the DIB compression value
     * @param pixels ARGB pixels, indexed [y][x] from the top row
     */
    public record Bmp(int bitsPerPixel, int headerSize, int compression,
        int[][] pixels) {

        public int width() {
            return pixels[0].length;
        }

        public int height() {
            return pixels.length;
        }
    }

    /**
     * Decode an uncompressed bottom-up 24-bit or 32-bit (BI_BITFIELDS,
     * BGRA masks) BMP.
     *
     * @param bmp the file bytes
     * @return the decoded image
     */
    public static Bmp decode(final byte[] bmp) {
        assertThat(bmp[0]).isEqualTo((byte) 'B');
        assertThat(bmp[1]).isEqualTo((byte) 'M');
        assertThat(readInt(bmp, 2)).isEqualTo(bmp.length);
        int offset = readInt(bmp, 10);
        int headerSize = readInt(bmp, 14);
        int width = readInt(bmp, 18);
        int height = readInt(bmp, 22);
        assertThat(height).as("bottom-up").isPositive();
        int bpp = readShort(bmp, 28);
        int compression = readInt(bmp, 30);
        int rowSize = ((width * bpp + 31) / 32) * 4;
        assertThat(bmp).hasSize(offset + rowSize * height);
        int[][] pixels = new int[height][width];
        for (int y = 0; y < height; y++) {
            int row = offset + (height - 1 - y) * rowSize;
            for (int x = 0; x < width; x++) {
                int p = row + x * (bpp / 8);
                int b = bmp[p] & 0xFF;
                int g = bmp[p + 1] & 0xFF;
                int r = bmp[p + 2] & 0xFF;
                int a = bpp == 32 ? bmp[p + 3] & 0xFF : 0xFF;
                pixels[y][x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return new Bmp(bpp, headerSize, compression, pixels);
    }

    private static int readInt(final byte[] b, final int i) {
        return (b[i] & 0xFF) | ((b[i + 1] & 0xFF) << 8)
            | ((b[i + 2] & 0xFF) << 16) | ((b[i + 3] & 0xFF) << 24);
    }

    private static int readShort(final byte[] b, final int i) {
        return (b[i] & 0xFF) | ((b[i + 1] & 0xFF) << 8);
    }

    private static ImageRGB sample() {
        // Width 3 needs row padding for 24-bit pixels (9 -> 12 bytes).
        ImageRGB image = new ArrayImageRGB(3, 2);
        image.setRGB(0, 0, 0xFF112233);
        image.setRGB(1, 0, 0x80445566);
        image.setRGB(2, 0, 0x00778899);
        image.setRGB(0, 1, 0xFFAABBCC);
        image.setRGB(1, 1, 0xFFDDEEFF);
        image.setRGB(2, 1, 0x40102030);
        return image;
    }

    @Test
    @DisplayName("Opaque images are 24-bit, padded, bottom-up BMPs")
    void encodes24Bit() {
        byte[] bytes = new BmpImageEncoder().encode(sample(), false);

        Bmp bmp = decode(bytes);
        assertThat(bmp.bitsPerPixel()).isEqualTo(24);
        assertThat(bmp.headerSize()).isEqualTo(40);
        assertThat(bmp.compression()).isZero();
        assertThat(bmp.pixels()).isDeepEqualTo(new int[][] {
            {0xFF112233, 0xFF445566, 0xFF778899},
            {0xFFAABBCC, 0xFFDDEEFF, 0xFF102030},
        });
        // The first stored row is the bottom row (BGR order).
        assertThat(bytes[54]).isEqualTo((byte) 0xCC);
        assertThat(bytes[55]).isEqualTo((byte) 0xBB);
        assertThat(bytes[56]).isEqualTo((byte) 0xAA);
    }

    @Test
    @DisplayName("Alpha images are 32-bit BMPs with a V4 header and alpha mask")
    void encodes32BitWithAlpha() {
        byte[] bytes = new BmpImageEncoder().encode(sample(), true);

        Bmp bmp = decode(bytes);
        assertThat(bmp.bitsPerPixel()).isEqualTo(32);
        assertThat(bmp.headerSize()).isEqualTo(108);
        assertThat(bmp.compression()).as("BI_BITFIELDS").isEqualTo(3);
        assertThat(readInt(bytes, 54)).isEqualTo(0x00FF0000);
        assertThat(readInt(bytes, 58)).isEqualTo(0x0000FF00);
        assertThat(readInt(bytes, 62)).isEqualTo(0x000000FF);
        assertThat(readInt(bytes, 66)).isEqualTo(0xFF000000);
        assertThat(bmp.pixels()).isDeepEqualTo(new int[][] {
            {0xFF112233, 0x80445566, 0x00778899},
            {0xFFAABBCC, 0xFFDDEEFF, 0x40102030},
        });
    }
}
