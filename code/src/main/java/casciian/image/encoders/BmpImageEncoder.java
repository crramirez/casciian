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

import casciian.bits.ImageRGB;

/**
 * Built-in, dependency-free encoder for uncompressed Windows BMP files.
 *
 * <p>Opaque images are written as 24-bit BMPs with a
 * {@code BITMAPINFOHEADER}.  When alpha is requested, images are written
 * as 32-bit BMPs with a {@code BITMAPV4HEADER} and {@code BI_BITFIELDS}
 * masks, so that readers honor the alpha channel.  Rows are stored
 * bottom-up as the format recommends.</p>
 *
 * <p>It has the lowest priority (0), so any add-on encoder (e.g. PNG) is
 * preferred when present.</p>
 */
public class BmpImageEncoder implements ImageEncoder {

    /**
     * Size of the BITMAPFILEHEADER.
     */
    private static final int FILE_HEADER_SIZE = 14;

    /**
     * Size of the BITMAPINFOHEADER used for 24-bit images.
     */
    private static final int INFO_HEADER_SIZE = 40;

    /**
     * Size of the BITMAPV4HEADER used for 32-bit images.
     */
    private static final int V4_HEADER_SIZE = 108;

    /**
     * BI_RGB compression: uncompressed.
     */
    private static final int BI_RGB = 0;

    /**
     * BI_BITFIELDS compression: uncompressed with explicit channel masks.
     */
    private static final int BI_BITFIELDS = 3;

    /**
     * LCS_sRGB color space tag ('sRGB').
     */
    private static final int LCS_SRGB = 0x73524742;

    /**
     * 72 DPI in pixels per meter.
     */
    private static final int PIXELS_PER_METER = 2835;

    /**
     * Public constructor.
     */
    public BmpImageEncoder() {
        // Nothing to set up.
    }

    @Override
    public String getName() {
        return "bmp";
    }

    @Override
    public String getMimeType() {
        return "image/bmp";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public boolean supportsAlpha() {
        return true;
    }

    @Override
    public byte[] encode(final ImageRGB image, final boolean alpha) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] argb = image.getRGB(0, 0, width, height, null, 0, width);
        int headerSize = alpha ? V4_HEADER_SIZE : INFO_HEADER_SIZE;
        int bitsPerPixel = alpha ? 32 : 24;
        int rowSize = ((width * bitsPerPixel + 31) / 32) * 4;
        int pixelOffset = FILE_HEADER_SIZE + headerSize;
        long fileSize = (long) pixelOffset + (long) rowSize * height;
        if (fileSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Image too large for BMP: "
                + width + "x" + height);
        }
        byte[] out = new byte[(int) fileSize];

        // BITMAPFILEHEADER
        out[0] = 'B';
        out[1] = 'M';
        putInt(out, 2, (int) fileSize);
        putInt(out, 10, pixelOffset);

        // BITMAPINFOHEADER / BITMAPV4HEADER
        int h = FILE_HEADER_SIZE;
        putInt(out, h, headerSize);
        putInt(out, h + 4, width);
        putInt(out, h + 8, height);
        putShort(out, h + 12, 1);
        putShort(out, h + 14, bitsPerPixel);
        putInt(out, h + 16, alpha ? BI_BITFIELDS : BI_RGB);
        putInt(out, h + 20, rowSize * height);
        putInt(out, h + 24, PIXELS_PER_METER);
        putInt(out, h + 28, PIXELS_PER_METER);
        if (alpha) {
            putInt(out, h + 40, 0x00FF0000);
            putInt(out, h + 44, 0x0000FF00);
            putInt(out, h + 48, 0x000000FF);
            putInt(out, h + 52, 0xFF000000);
            putInt(out, h + 56, LCS_SRGB);
        }

        for (int y = 0; y < height; y++) {
            int offset = pixelOffset + (height - 1 - y) * rowSize;
            int rowStart = y * width;
            for (int x = 0; x < width; x++) {
                int pixel = argb[rowStart + x];
                out[offset++] = (byte) pixel;
                out[offset++] = (byte) (pixel >>> 8);
                out[offset++] = (byte) (pixel >>> 16);
                if (alpha) {
                    out[offset++] = (byte) (pixel >>> 24);
                }
            }
        }
        return out;
    }

    /**
     * Write a little-endian 32-bit integer.
     *
     * @param out the destination
     * @param offset the index of the first byte
     * @param value the value
     */
    private static void putInt(final byte[] out, final int offset,
        final int value) {

        out[offset] = (byte) value;
        out[offset + 1] = (byte) (value >>> 8);
        out[offset + 2] = (byte) (value >>> 16);
        out[offset + 3] = (byte) (value >>> 24);
    }

    /**
     * Write a little-endian 16-bit integer.
     *
     * @param out the destination
     * @param offset the index of the first byte
     * @param value the value
     */
    private static void putShort(final byte[] out, final int offset,
        final int value) {

        out[offset] = (byte) value;
        out[offset + 1] = (byte) (value >>> 8);
    }
}
