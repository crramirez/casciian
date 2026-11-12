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
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import casciian.bits.ImageRGB;

/**
 * Encodes images as PNG files: 8-bit truecolor (RGB), or truecolor with
 * alpha (RGBA).  Every row uses the Paeth filter and the data is deflated
 * with the fastest compression level, which already shrinks typical
 * screen content many times compared to BMP.  Only {@code java.base} is
 * used, so this works in native images and without {@code java.desktop}.
 */
public class PngImageEncoder implements ImageEncoder {

    /**
     * The PNG file signature.
     */
    private static final byte[] SIGNATURE = {
        (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'
    };

    /**
     * IHDR color type for truecolor.
     */
    private static final int COLOR_TYPE_RGB = 2;

    /**
     * IHDR color type for truecolor with alpha.
     */
    private static final int COLOR_TYPE_RGBA = 6;

    /**
     * Row filter type for Paeth.
     */
    private static final int FILTER_PAETH = 4;

    /**
     * Public constructor.
     */
    public PngImageEncoder() {
        // Nothing to set up.
    }

    @Override
    public String getName() {
        return "png";
    }

    @Override
    public String getMimeType() {
        return "image/png";
    }

    /**
     * Get the priority of this encoder.  PNG is preferred over the
     * built-in BMP encoder (priority 0).
     *
     * @return 10
     */
    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public boolean supportsAlpha() {
        return true;
    }

    @Override
    public byte[] encode(final ImageRGB image, final boolean alpha) {
        int width = image.getWidth();
        int height = image.getHeight();
        if ((width <= 0) || (height <= 0)) {
            throw new IllegalArgumentException("Empty image: " + width
                + "x" + height);
        }
        int bytesPerPixel = alpha ? 4 : 3;
        long rawSize = (long) height * ((long) width * bytesPerPixel + 1);
        if (rawSize > Integer.MAX_VALUE - 8) {
            throw new IllegalArgumentException("Image too large for PNG: "
                + width + "x" + height);
        }
        int stride = width * bytesPerPixel;
        byte[] raw = new byte[(int) rawSize];
        byte[] prev = new byte[stride];
        byte[] cur = new byte[stride];
        int[] row = new int[width];
        int out = 0;
        for (int y = 0; y < height; y++) {
            image.getRGB(0, y, width, 1, row, 0, width);
            int i = 0;
            for (int x = 0; x < width; x++) {
                int pixel = row[x];
                cur[i++] = (byte) (pixel >>> 16);
                cur[i++] = (byte) (pixel >>> 8);
                cur[i++] = (byte) pixel;
                if (alpha) {
                    cur[i++] = (byte) (pixel >>> 24);
                }
            }
            raw[out++] = FILTER_PAETH;
            for (int k = 0; k < stride; k++) {
                int left = (k >= bytesPerPixel)
                    ? (cur[k - bytesPerPixel] & 0xFF) : 0;
                int up = prev[k] & 0xFF;
                int upLeft = (k >= bytesPerPixel)
                    ? (prev[k - bytesPerPixel] & 0xFF) : 0;
                raw[out++] = (byte) (cur[k] - paeth(left, up, upLeft));
            }
            byte[] swap = prev;
            prev = cur;
            cur = swap;
        }

        byte[] ihdr = new byte[13];
        putInt(ihdr, 0, width);
        putInt(ihdr, 4, height);
        ihdr[8] = 8;
        ihdr[9] = (byte) (alpha ? COLOR_TYPE_RGBA : COLOR_TYPE_RGB);
        // Compression, filter, and interlace methods are all 0.

        byte[] idat = deflate(raw);
        ByteArrayOutputStream file = new ByteArrayOutputStream(
            idat.length + 64);
        file.writeBytes(SIGNATURE);
        writeChunk(file, "IHDR", ihdr);
        writeChunk(file, "IDAT", idat);
        writeChunk(file, "IEND", new byte[0]);
        return file.toByteArray();
    }

    /**
     * Compress data with zlib at the fastest level.
     *
     * @param data the data
     * @return the zlib stream
     */
    private static byte[] deflate(final byte[] data) {
        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        try {
            deflater.setInput(data);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(
                Math.max(64, data.length / 4));
            byte[] buffer = new byte[Math.min(65536,
                Math.max(1024, data.length))];
            while (!deflater.finished()) {
                int n = deflater.deflate(buffer);
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    /**
     * The Paeth predictor from the PNG specification.
     *
     * @param a the byte to the left
     * @param b the byte above
     * @param c the byte above and to the left
     * @return the predicted value
     */
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

    /**
     * Write a PNG chunk: length, type, data, and CRC.
     *
     * @param out the destination
     * @param type the four-letter chunk type
     * @param data the chunk data
     */
    private static void writeChunk(final ByteArrayOutputStream out,
        final String type, final byte[] data) {

        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        byte[] word = new byte[4];
        putInt(word, 0, data.length);
        out.writeBytes(word);
        out.writeBytes(typeBytes);
        out.writeBytes(data);
        putInt(word, 0, (int) crc.getValue());
        out.writeBytes(word);
    }

    /**
     * Write a big-endian 32-bit integer.
     *
     * @param out the destination
     * @param offset the index of the first byte
     * @param value the value
     */
    private static void putInt(final byte[] out, final int offset,
        final int value) {

        out[offset] = (byte) (value >>> 24);
        out[offset + 1] = (byte) (value >>> 16);
        out[offset + 2] = (byte) (value >>> 8);
        out[offset + 3] = (byte) value;
    }
}
