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
package casciian.imagedecoders.decoders;

import casciian.bits.ImageRGB;
import casciian.image.decoders.ImageDecoder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Black-box tests for {@link PNGImageDecoder}. Exercises the public
 * {@link ImageDecoder} API against synthetic PNG streams built in-memory,
 * covering every supported color type, transparency mode and scanline filter,
 * without inspecting decoder internals.
 */
class PNGImageDecoderTest {

    private static final int NONE = 0;
    private static final int SUB = 1;
    private static final int UP = 2;
    private static final int AVERAGE = 3;
    private static final int PAETH = 4;

    // ------------------------------------------------------------------
    // Color type 2 — Truecolor RGB
    // ------------------------------------------------------------------

    @Test
    void decodesTruecolorRgbWithNoneFilter() throws IOException {
        int[][] pixels = {
            {0xFF0000, 0x00FF00},
            {0x0000FF, 0xFFFFFF},
        };
        byte[] png = buildPng(2, 2, 8, 2, null, null,
            rgbScanlines(pixels, NONE));

        ImageRGB image = decode(png);

        assertThat(image.getWidth()).isEqualTo(2);
        assertThat(image.getHeight()).isEqualTo(2);
        assertThat(image.getRGB(0, 0)).isEqualTo(0xFFFF0000);
        assertThat(image.getRGB(1, 0)).isEqualTo(0xFF00FF00);
        assertThat(image.getRGB(0, 1)).isEqualTo(0xFF0000FF);
        assertThat(image.getRGB(1, 1)).isEqualTo(0xFFFFFFFF);
    }

    @Test
    void appliesRgbKeyColorTransparency() throws IOException {
        int[][] pixels = {
            {0xFF0000, 0x00FF00},
        };
        // tRNS key = 0x00FF00 (green) -> that pixel becomes transparent.
        byte[] trns = {0x00, 0x00, 0x00, (byte) 0xFF, 0x00, 0x00};
        byte[] png = buildPng(2, 1, 8, 2, null, trns,
            rgbScanlines(pixels, NONE));

        ImageRGB image = decode(png);

        assertThat(image.getRGB(0, 0)).isEqualTo(0xFFFF0000);
        assertThat(image.getRGB(1, 0) >>> 24).isEqualTo(0); // transparent
        assertThat(image.getRGB(1, 0) & 0x00FFFFFF).isEqualTo(0x00FF00);
    }

    // ------------------------------------------------------------------
    // Color type 6 — Truecolor RGBA
    // ------------------------------------------------------------------

    @Test
    void decodesTruecolorRgba() throws IOException {
        // Rows of {r,g,b,a}.
        byte[][] rows = {
            {(byte) 0x10, 0x20, 0x30, (byte) 0x80,
             (byte) 0x40, 0x50, 0x60, (byte) 0xFF},
        };
        byte[] png = buildPng(2, 1, 8, 6, null, null,
            filterRows(rows, NONE, 4));

        ImageRGB image = decode(png);

        assertThat(image.getRGB(0, 0)).isEqualTo(0x80102030);
        assertThat(image.getRGB(1, 0)).isEqualTo(0xFF405060);
    }

    // ------------------------------------------------------------------
    // Color type 0 — Grayscale
    // ------------------------------------------------------------------

    @Test
    void decodesEightBitGrayscale() throws IOException {
        byte[][] rows = {{0x00, (byte) 0x80, (byte) 0xFF}};
        byte[] png = buildPng(3, 1, 8, 0, null, null,
            filterRows(rows, NONE, 1));

        ImageRGB image = decode(png);

        assertThat(image.getRGB(0, 0)).isEqualTo(0xFF000000);
        assertThat(image.getRGB(1, 0)).isEqualTo(0xFF808080);
        assertThat(image.getRGB(2, 0)).isEqualTo(0xFFFFFFFF);
    }

    @Test
    void decodesOneBitGrayscaleWithScaling() throws IOException {
        // Two pixels packed MSB-first into one byte: 1,0,... -> 0b10000000.
        byte[][] rows = {{(byte) 0x80}};
        byte[] png = buildPng(2, 1, 1, 0, null, null,
            filterRows(rows, NONE, 1));

        ImageRGB image = decode(png);

        assertThat(image.getRGB(0, 0)).isEqualTo(0xFFFFFFFF); // sample 1 -> white
        assertThat(image.getRGB(1, 0)).isEqualTo(0xFF000000); // sample 0 -> black
    }

    @Test
    void appliesGrayscaleKeyColorTransparency() throws IOException {
        byte[][] rows = {{0x00, (byte) 0xFF}};
        // Key gray sample = 0x00 -> transparent.
        byte[] trns = {0x00, 0x00};
        byte[] png = buildPng(2, 1, 8, 0, null, trns,
            filterRows(rows, NONE, 1));

        ImageRGB image = decode(png);

        assertThat(image.getRGB(0, 0) >>> 24).isEqualTo(0); // transparent black
        assertThat(image.getRGB(1, 0)).isEqualTo(0xFFFFFFFF);
    }

    // ------------------------------------------------------------------
    // Color type 3 — Indexed / palette
    // ------------------------------------------------------------------

    @Test
    void decodesIndexedWithPaletteAndAlpha() throws IOException {
        // Palette: index 0 red, index 1 green, index 2 blue.
        byte[] palette = {
            (byte) 0xFF, 0x00, 0x00,
            0x00, (byte) 0xFF, 0x00,
            0x00, 0x00, (byte) 0xFF,
        };
        // tRNS: index 0 fully transparent, index 1 semi-transparent.
        byte[] trns = {0x00, (byte) 0x80};
        // Four pixels of 4-bit indices packed MSB-first: 0,1,2,1.
        byte[][] rows = {{0x01, 0x21}};
        byte[] png = buildPng(4, 1, 4, 3, palette, trns,
            filterRows(rows, NONE, 1));

        ImageRGB image = decode(png);

        assertThat(image.getRGB(0, 0)).isEqualTo(0x00FF0000); // index 0, alpha 0
        assertThat(image.getRGB(1, 0)).isEqualTo(0x8000FF00); // index 1, alpha 0x80
        assertThat(image.getRGB(2, 0)).isEqualTo(0xFF0000FF); // index 2, opaque
        assertThat(image.getRGB(3, 0)).isEqualTo(0x8000FF00); // index 1 again
    }

    // ------------------------------------------------------------------
    // Scanline filters
    // ------------------------------------------------------------------

    @Test
    void roundTripsAllFilterTypesOnRgba() throws IOException {
        int width = 6;
        int height = 5;
        int channels = 4;
        // Deterministic gradient image.
        byte[][] pixels = new byte[height][width * channels];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int p = x * channels;
                pixels[y][p] = (byte) (x * 40 + y);
                pixels[y][p + 1] = (byte) (x * 7 + y * 11);
                pixels[y][p + 2] = (byte) (x * 3 + y * 5);
                pixels[y][p + 3] = (byte) (200 + x + y);
            }
        }
        // Use a different filter for each row to exercise all five.
        int[] filters = {NONE, SUB, UP, AVERAGE, PAETH};
        byte[] filtered = encodeWithFilters(pixels, filters, channels);
        byte[] png = buildPng(width, height, 8, 6, null, null, filtered);

        ImageRGB image = decode(png);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int p = x * channels;
                int expected = ((pixels[y][p + 3] & 0xFF) << 24)
                    | ((pixels[y][p] & 0xFF) << 16)
                    | ((pixels[y][p + 1] & 0xFF) << 8)
                    | (pixels[y][p + 2] & 0xFF);
                assertThat(image.getRGB(x, y))
                    .as("pixel (%d,%d)", x, y)
                    .isEqualTo(expected);
            }
        }
    }

    // ------------------------------------------------------------------
    // Unsupported / invalid inputs
    // ------------------------------------------------------------------

    @Test
    void rejectsInterlacedImages() throws IOException {
        byte[] png = buildPngWithInterlace(2, 1, 8, 2, 1,
            rgbScanlines(new int[][]{{0, 0}}, NONE));

        assertThatThrownBy(() -> decode(png))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("interlaced");
    }

    @Test
    void rejectsSixteenBitImages() throws IOException {
        // IHDR with bit depth 16 must be rejected before pixel processing.
        byte[] png = buildPngRawIhdr(1, 1, 16, 0, 0,
            new byte[]{0x00, 0x00, 0x00});

        assertThatThrownBy(() -> decode(png))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("16-bit");
    }

    @Test
    void rejectsBadSignature() {
        byte[] notPng = "this is not a png".getBytes();

        assertThatThrownBy(() -> decode(notPng))
            .isInstanceOf(IOException.class);
    }

    @Test
    void rejectsUnknownCriticalChunks() throws IOException {
        byte[] png = buildPngWithExtraChunk(1, 1, 8, 2, "ABCD", new byte[] {0x00},
            rgbScanlines(new int[][]{{0x000000}}, NONE));

        assertThatThrownBy(() -> decode(png))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("unknown critical chunk");
    }

    @Test
    void rejectsMissingIendChunk() throws IOException {
        byte[] png = buildPng(1, 1, 8, 2, null, null,
            rgbScanlines(new int[][]{{0x000000}}, NONE));
        byte[] truncated = new byte[png.length - 12];
        System.arraycopy(png, 0, truncated, 0, truncated.length);

        assertThatThrownBy(() -> decode(truncated))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("missing IEND");
    }

    @Test
    void rejectsChunkLengthThatWouldRunPastTypeAndCrc() throws IOException {
        byte[] png = buildPng(1, 1, 8, 2, null, null,
            rgbScanlines(new int[][]{{0x000000}}, NONE));
        ByteBuffer.wrap(png).order(ByteOrder.BIG_ENDIAN)
            .putInt(33, png.length);

        assertThatThrownBy(() -> decode(png))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("chunk length out of bounds");
    }

    @Test
    void rejectsImagesWhoseScanlineBuffersOverflowJavaArrays() throws IOException {
        byte[] png = buildPng(Integer.MAX_VALUE, 1, 8, 2, null, null,
            new byte[0]);

        assertThatThrownBy(() -> decode(png))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("image too large");
    }

    @Test
    void rejectsImagesWhosePixelCountOverflowsJavaArrays() throws IOException {
        byte[] png = buildPng(1_000_000, 3_000, 1, 0, null, null, new byte[0]);

        assertThatThrownBy(() -> decode(png))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("image too large");
    }

    @Test
    void extensionPatternAndMetadata() {
        ImageDecoder decoder = new PNGImageDecoder();

        assertThat("picture.png").matches(decoder.getFileExtensionPattern());
        assertThat("PICTURE.PNG").matches(decoder.getFileExtensionPattern());
        assertThat("picture.bmp").doesNotMatch(decoder.getFileExtensionPattern());
        assertThat(decoder.getSupportedMimeTypes()).contains("image/png");
        assertThat(decoder.getFormatDescription()).isNotBlank();
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private static ImageRGB decode(byte[] png) throws IOException {
        return new PNGImageDecoder().decode(new ByteArrayInputStream(png), "image/png");
    }

    /**
     * Turn rows of 0xRRGGBB pixels into filtered scanline bytes (RGB, 3 bytes
     * per pixel) with the given single filter applied to every row.
     */
    private static byte[] rgbScanlines(int[][] pixels, int filter) {
        int height = pixels.length;
        int width = pixels[0].length;
        byte[][] rows = new byte[height][width * 3];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = pixels[y][x];
                rows[y][x * 3] = (byte) ((rgb >> 16) & 0xFF);
                rows[y][x * 3 + 1] = (byte) ((rgb >> 8) & 0xFF);
                rows[y][x * 3 + 2] = (byte) (rgb & 0xFF);
            }
        }
        return filterRows(rows, filter, 3);
    }

    /**
     * Prepend the given (single) filter-type byte to each raw row. Only used
     * with NONE, so no forward filtering math is required.
     */
    private static byte[] filterRows(byte[][] rows, int filter, int bpp) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] row : rows) {
            out.write(filter);
            out.write(row, 0, row.length);
        }
        return out.toByteArray();
    }

    /**
     * Forward-filter each raw row with its designated filter type so the
     * decoder's unfiltering can be verified by round-trip.
     */
    private static byte[] encodeWithFilters(byte[][] pixels, int[] filters, int bpp) {
        int height = pixels.length;
        int stride = pixels[0].length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] prev = new byte[stride];
        for (int y = 0; y < height; y++) {
            byte[] curr = pixels[y];
            int filter = filters[y];
            byte[] filtered = new byte[stride];
            for (int i = 0; i < stride; i++) {
                int a = (i >= bpp) ? (curr[i - bpp] & 0xFF) : 0;
                int b = prev[i] & 0xFF;
                int c = (i >= bpp) ? (prev[i - bpp] & 0xFF) : 0;
                int x = curr[i] & 0xFF;
                int predictor = switch (filter) {
                    case SUB -> a;
                    case UP -> b;
                    case AVERAGE -> (a + b) >> 1;
                    case PAETH -> paeth(a, b, c);
                    default -> 0;
                };
                filtered[i] = (byte) (x - predictor);
            }
            out.write(filter);
            out.write(filtered, 0, stride);
            prev = curr;
        }
        return out.toByteArray();
    }

    private static int paeth(int a, int b, int c) {
        int p = a + b - c;
        int pa = Math.abs(p - a);
        int pb = Math.abs(p - b);
        int pc = Math.abs(p - c);
        if (pa <= pb && pa <= pc) {
            return a;
        }
        return (pb <= pc) ? b : c;
    }

    private static byte[] buildPng(int width, int height, int bitDepth,
        int colorType, byte[] palette, byte[] trns, byte[] rawScanlines)
        throws IOException {
        return buildPngWithInterlace(width, height, bitDepth, colorType, 0,
            palette, trns, rawScanlines);
    }

    private static byte[] buildPngWithInterlace(int width, int height,
        int bitDepth, int colorType, int interlace, byte[] rawScanlines)
        throws IOException {
        return buildPngWithInterlace(width, height, bitDepth, colorType,
            interlace, null, null, rawScanlines);
    }

    private static byte[] buildPngWithInterlace(int width, int height,
        int bitDepth, int colorType, int interlace, byte[] palette,
        byte[] trns, byte[] rawScanlines) throws IOException {
        return buildPngWithExtraChunk(width, height, bitDepth, colorType,
            interlace, palette, trns, null, null, rawScanlines);
    }

    private static byte[] buildPngWithExtraChunk(int width, int height,
        int bitDepth, int colorType, String extraChunkType,
        byte[] extraChunkData, byte[] rawScanlines) throws IOException {
        return buildPngWithExtraChunk(width, height, bitDepth, colorType, 0,
            null, null, extraChunkType, extraChunkData, rawScanlines);
    }

    private static byte[] buildPngWithExtraChunk(int width, int height,
        int bitDepth, int colorType, int interlace, byte[] palette,
        byte[] trns, String extraChunkType, byte[] extraChunkData,
        byte[] rawScanlines) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

        ByteBuffer ihdr = ByteBuffer.allocate(13).order(ByteOrder.BIG_ENDIAN);
        ihdr.putInt(width);
        ihdr.putInt(height);
        ihdr.put((byte) bitDepth);
        ihdr.put((byte) colorType);
        ihdr.put((byte) 0); // compression
        ihdr.put((byte) 0); // filter method
        ihdr.put((byte) interlace);
        writeChunk(out, "IHDR", ihdr.array());

        if (palette != null) {
            writeChunk(out, "PLTE", palette);
        }
        if (trns != null) {
            writeChunk(out, "tRNS", trns);
        }
        if (extraChunkType != null) {
            writeChunk(out, extraChunkType, extraChunkData);
        }

        Deflater deflater = new Deflater();
        deflater.setInput(rawScanlines);
        deflater.finish();
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        while (!deflater.finished()) {
            int n = deflater.deflate(buf);
            compressed.write(buf, 0, n);
        }
        deflater.end();
        writeChunk(out, "IDAT", compressed.toByteArray());

        writeChunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    /**
     * Build a PNG whose IHDR is written verbatim (used to inject an unsupported
     * bit depth) followed by a valid but never-reached IDAT.
     */
    private static byte[] buildPngRawIhdr(int width, int height, int bitDepth,
        int colorType, int interlace, byte[] rawScanlines) throws IOException {
        return buildPngWithInterlace(width, height, bitDepth, colorType,
            interlace, null, null, rawScanlines);
    }

    private static void writeChunk(ByteArrayOutputStream out, String type,
        byte[] data) throws IOException {

        byte[] typeBytes = type.getBytes("US-ASCII");
        ByteBuffer len = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        len.putInt(data.length);
        out.write(len.array());
        out.write(typeBytes);
        out.write(data);

        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        ByteBuffer crcBuf = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        crcBuf.putInt((int) crc.getValue());
        out.write(crcBuf.array());
    }
}
