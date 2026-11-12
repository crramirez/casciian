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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import casciian.bits.ArrayImageRGB;
import casciian.bits.ImageRGB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the built-in PNG encoder.  Output is checked with an
 * independent decoder that follows the PNG specification.
 */
@DisplayName("PngImageEncoder")
public class PngImageEncoderTest {

    private static final byte[] SIGNATURE = {
        (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'
    };

    /**
     * A decoded PNG image.
     *
     * @param bitsPerPixel 24 for RGB, 32 for RGBA
     * @param pixels ARGB pixels indexed [y][x]; opaque for RGB
     */
    public record Png(int bitsPerPixel, int[][] pixels) {

        public int width() {
            return pixels[0].length;
        }

        public int height() {
            return pixels.length;
        }
    }

    @Test
    @DisplayName("Opaque images round-trip as 8-bit RGB without alpha")
    void opaqueRoundTrip() {
        int[] argb = randomPixels(13, 7, 1);
        Png png = decode(new PngImageEncoder().encode(image(13, 7, argb),
            false));

        assertThat(png.bitsPerPixel()).isEqualTo(24);
        assertThat(png.width()).isEqualTo(13);
        assertThat(png.height()).isEqualTo(7);
        for (int y = 0; y < 7; y++) {
            for (int x = 0; x < 13; x++) {
                assertThat(png.pixels()[y][x])
                    .isEqualTo(0xFF000000 | argb[y * 13 + x]);
            }
        }
    }

    @Test
    @DisplayName("Alpha images round-trip as 8-bit RGBA")
    void alphaRoundTrip() {
        int[] argb = randomPixels(5, 9, 2);
        Png png = decode(new PngImageEncoder().encode(image(5, 9, argb),
            true));

        assertThat(png.bitsPerPixel()).isEqualTo(32);
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 5; x++) {
                assertThat(png.pixels()[y][x]).isEqualTo(argb[y * 5 + x]);
            }
        }
    }

    @Test
    @DisplayName("Flat images compress far below the BMP size")
    void flatImagesCompress() {
        ImageRGB flat = new ArrayImageRGB(200, 100);
        flat.fillRect(0, 0, 200, 100, 0xFF336699);

        byte[] png = new PngImageEncoder().encode(flat, false);
        byte[] bmp = new BmpImageEncoder().encode(flat, false);

        assertThat(decode(png).pixels()[99][199]).isEqualTo(0xFF336699);
        assertThat(png.length * 50).isLessThan(bmp.length);
    }

    @Test
    @DisplayName("Empty images are rejected")
    void emptyImageRejected() {
        ImageRGB empty = org.mockito.Mockito.mock(ImageRGB.class);

        assertThatThrownBy(() -> new PngImageEncoder().encode(empty, false))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static int[] randomPixels(final int width, final int height,
        final long seed) {

        Random random = new Random(seed);
        int[] argb = new int[width * height];
        for (int i = 0; i < argb.length; i++) {
            argb[i] = random.nextInt();
        }
        return argb;
    }

    private static ImageRGB image(final int width, final int height,
        final int[] argb) {

        ImageRGB image = new ArrayImageRGB(width, height);
        image.setRGB(0, 0, width, height, argb, 0, width);
        return image;
    }

    /**
     * Decode an 8-bit RGB or RGBA, non-interlaced PNG, verifying the
     * signature and chunk CRCs.  All five row filter types are supported.
     *
     * @param data the PNG file
     * @return the decoded image
     */
    public static Png decode(final byte[] data) {
        ByteBuffer in = ByteBuffer.wrap(data);
        byte[] signature = new byte[8];
        in.get(signature);
        assertThat(signature).isEqualTo(SIGNATURE);

        int width = 0;
        int height = 0;
        int colorType = -1;
        ByteArrayOutputStream idat = new ByteArrayOutputStream();
        boolean ended = false;
        while (!ended) {
            int length = in.getInt();
            byte[] typeBytes = new byte[4];
            in.get(typeBytes);
            byte[] chunk = new byte[length];
            in.get(chunk);
            CRC32 crc = new CRC32();
            crc.update(typeBytes);
            crc.update(chunk);
            assertThat(in.getInt()).as("CRC").isEqualTo((int) crc.getValue());
            String type = new String(typeBytes, StandardCharsets.US_ASCII);
            switch (type) {
            case "IHDR" -> {
                ByteBuffer ihdr = ByteBuffer.wrap(chunk);
                width = ihdr.getInt();
                height = ihdr.getInt();
                assertThat(ihdr.get()).as("bit depth").isEqualTo((byte) 8);
                colorType = ihdr.get();
                assertThat(ihdr.get()).as("compression").isZero();
                assertThat(ihdr.get()).as("filter method").isZero();
                assertThat(ihdr.get()).as("interlace").isZero();
            }
            case "IDAT" -> idat.writeBytes(chunk);
            case "IEND" -> ended = true;
            default -> { }
            }
        }
        assertThat(in.hasRemaining()).isFalse();
        assertThat(colorType).isIn(2, 6);

        int bpp = (colorType == 6) ? 4 : 3;
        int stride = width * bpp;
        byte[] raw = inflate(idat.toByteArray(), height * (stride + 1));
        int[][] pixels = new int[height][width];
        byte[] prev = new byte[stride];
        for (int y = 0; y < height; y++) {
            int base = y * (stride + 1);
            int filter = raw[base];
            byte[] cur = new byte[stride];
            for (int k = 0; k < stride; k++) {
                int a = (k >= bpp) ? (cur[k - bpp] & 0xFF) : 0;
                int b = prev[k] & 0xFF;
                int c = (k >= bpp) ? (prev[k - bpp] & 0xFF) : 0;
                int predictor = switch (filter) {
                case 0 -> 0;
                case 1 -> a;
                case 2 -> b;
                case 3 -> (a + b) / 2;
                case 4 -> paeth(a, b, c);
                default -> throw new AssertionError("filter " + filter);
                };
                cur[k] = (byte) (raw[base + 1 + k] + predictor);
            }
            for (int x = 0; x < width; x++) {
                int i = x * bpp;
                int alpha = (bpp == 4) ? (cur[i + 3] & 0xFF) : 0xFF;
                pixels[y][x] = (alpha << 24) | ((cur[i] & 0xFF) << 16)
                    | ((cur[i + 1] & 0xFF) << 8) | (cur[i + 2] & 0xFF);
            }
            prev = cur;
        }
        return new Png(bpp * 8, pixels);
    }

    private static byte[] inflate(final byte[] zlib, final int size) {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(zlib);
            byte[] out = new byte[size];
            int n = 0;
            while (!inflater.finished() && n < size) {
                n += inflater.inflate(out, n, size - n);
            }
            assertThat(n).isEqualTo(size);
            assertThat(inflater.finished()).isTrue();
            return out;
        } catch (DataFormatException e) {
            throw new AssertionError(e);
        } finally {
            inflater.end();
        }
    }

    private static int paeth(final int a, final int b, final int c) {
        int p = a + b - c;
        int pa = Math.abs(p - a);
        int pb = Math.abs(p - b);
        int pc = Math.abs(p - c);
        if ((pa <= pb) && (pa <= pc)) {
            return a;
        }
        return (pb <= pc) ? b : c;
    }
}
