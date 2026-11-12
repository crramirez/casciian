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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import casciian.bits.ImageRGB;

/**
 * Encodes images as PNG files: 8-bit truecolor (RGB), or truecolor with
 * alpha (RGBA).  This is the built-in, default encoder for the iTerm2
 * inline images protocol.  Only {@code java.base} is used, so it works in
 * native images and without {@code java.desktop}.
 *
 * <p>The defaults favor interactive rendering latency over maximum
 * compression: zlib level {@value #DEFAULT_COMPRESSION_LEVEL} and
 * {@link FilterStrategy#ADAPTIVE adaptive} row filtering.  See
 * {@code docs/iterm2-inline-images.md} for the measurements behind this
 * choice and {@code PngEncoderBenchmark} to reproduce them.</p>
 *
 * <p>Scanlines are unpacked, filtered, and fed to the {@link Deflater} one
 * at a time, so no full-size copy of the raw or filtered image is ever
 * allocated.  The deflater writes straight into output blocks that are
 * copied once into the exactly sized PNG file.  Instances are immutable
 * and thread-safe.</p>
 */
public class PngImageEncoder implements ImageEncoder {

    /**
     * How scanlines are filtered before compression.
     */
    public enum FilterStrategy {
        /**
         * Filter type 0 on every row: the bytes are stored unchanged.
         */
        NONE(0),

        /**
         * Filter type 1 on every row: difference from the pixel to the
         * left.
         */
        SUB(1),

        /**
         * Filter type 2 on every row: difference from the pixel above.
         */
        UP(2),

        /**
         * Filter type 3 on every row: difference from the average of the
         * left and upper pixels.
         */
        AVERAGE(3),

        /**
         * Filter type 4 on every row: difference from the Paeth predictor.
         */
        PAETH(4),

        /**
         * Pick Up or Sub per row, whichever has the smaller sum of
         * absolute differences (the "MSAD" heuristic recommended by the
         * PNG specification).  Both filters are cheap, and a row identical
         * to the one above stops the search after Up.  This is the
         * default: it is faster than fixed {@link #PAETH} on every
         * non-trivial image measured, and produces equal or smaller output
         * on text, UI, icon, and photo content.  Smooth 2-D gradients
         * compress better with {@link #PAETH} or {@link #ADAPTIVE_ALL}.
         */
        ADAPTIVE(-1),

        /**
         * Pick any of the five filters per row with the MSAD heuristic.
         * Gives the smallest output of all strategies, but evaluating
         * Average and Paeth on most rows makes it the slowest.  Useful
         * together with a higher compression level on slow links.
         */
        ADAPTIVE_ALL(-2);

        /**
         * The PNG filter type byte, or -1 for adaptive.
         */
        private final int type;

        FilterStrategy(final int type) {
            this.type = type;
        }
    }

    /**
     * The default zlib compression level.
     */
    public static final int DEFAULT_COMPRESSION_LEVEL = 1;

    /**
     * The default row filter strategy.
     */
    public static final FilterStrategy DEFAULT_FILTER_STRATEGY =
        FilterStrategy.ADAPTIVE;

    /**
     * The PNG file signature.
     */
    private static final byte[] SIGNATURE = {
        (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'
    };

    /**
     * The complete, constant IEND chunk: zero length, type, and CRC.
     */
    private static final byte[] IEND = {
        0, 0, 0, 0, 'I', 'E', 'N', 'D', (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    /**
     * Bytes before the IDAT data: the signature, the 25-byte IHDR chunk,
     * and the IDAT length and type.
     */
    private static final int HEADER_SIZE = 8 + 25 + 8;

    /**
     * Bytes after the IDAT data: the IDAT CRC and the IEND chunk.
     */
    private static final int TRAILER_SIZE = 4 + 12;

    /**
     * The largest array the JVM can reliably allocate.
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * IHDR color type for truecolor.
     */
    private static final int COLOR_TYPE_RGB = 2;

    /**
     * IHDR color type for truecolor with alpha.
     */
    private static final int COLOR_TYPE_RGBA = 6;

    /**
     * Filters tried by {@link FilterStrategy#ADAPTIVE}, Up first so that
     * rows equal to the one above stop the search immediately.
     */
    private static final int[] ADAPTIVE_FILTERS = {2, 1};

    /**
     * Filters tried by {@link FilterStrategy#ADAPTIVE_ALL}: cheapest first,
     * so that the early exit prunes the expensive ones.
     */
    private static final int[] ADAPTIVE_ALL_FILTERS = {2, 1, 0, 4, 3};

    /**
     * The zlib compression level.
     */
    private final int compressionLevel;

    /**
     * The row filter strategy.
     */
    private final FilterStrategy filterStrategy;

    /**
     * Public constructor using the default compression level and filter
     * strategy.  This is the constructor used by {@link
     * java.util.ServiceLoader}.
     */
    public PngImageEncoder() {
        this(DEFAULT_COMPRESSION_LEVEL, DEFAULT_FILTER_STRATEGY);
    }

    /**
     * Public constructor.
     *
     * @param compressionLevel zlib level, 0 (store) to 9 (smallest)
     * @param filterStrategy the row filter strategy
     * @throws IllegalArgumentException if the level is out of range
     */
    public PngImageEncoder(final int compressionLevel,
        final FilterStrategy filterStrategy) {

        if ((compressionLevel < Deflater.NO_COMPRESSION)
            || (compressionLevel > Deflater.BEST_COMPRESSION)) {
            throw new IllegalArgumentException("Invalid compression level: "
                + compressionLevel);
        }
        this.compressionLevel = compressionLevel;
        this.filterStrategy = Objects.requireNonNull(filterStrategy,
            "filterStrategy");
    }

    /**
     * Get the zlib compression level.
     *
     * @return 0 to 9
     */
    public int getCompressionLevel() {
        return compressionLevel;
    }

    /**
     * Get the row filter strategy.
     *
     * @return the strategy
     */
    public FilterStrategy getFilterStrategy() {
        return filterStrategy;
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
     * Get the priority of this encoder.  An add-on encoder needs a
     * priority above this to be preferred.
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

    /**
     * Encode an image as a PNG file.
     *
     * @param image the image to encode
     * @param alpha if true, write RGBA; otherwise write RGB and ignore
     *              the alpha channel
     * @return the PNG file bytes
     * @throws IllegalArgumentException if the image is empty or too large
     *                                  for a single PNG byte array
     */
    @Override
    public byte[] encode(final ImageRGB image, final boolean alpha) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        if ((width <= 0) || (height <= 0)) {
            throw new IllegalArgumentException("Empty image: " + width
                + "x" + height);
        }
        final int bpp = alpha ? 4 : 3;
        final long rowSize = (long) width * bpp + 1;
        if (rowSize * height > MAX_ARRAY_SIZE - HEADER_SIZE - TRAILER_SIZE) {
            throw new IllegalArgumentException("Image too large for PNG: "
                + width + "x" + height);
        }
        final int stride = (int) rowSize - 1;

        Output out = new Output(rowSize * height);

        int[] argb = new int[width];
        byte[] prev = new byte[stride];
        byte[] cur = new byte[stride];
        // Filtered rows carry the filter type byte at index 0.
        byte[] filtered = new byte[stride + 1];
        int[] candidates = switch (filterStrategy) {
        case ADAPTIVE -> ADAPTIVE_FILTERS;
        case ADAPTIVE_ALL -> ADAPTIVE_ALL_FILTERS;
        default -> null;
        };
        byte[] trial = (candidates != null)
            ? new byte[stride + 1] : null;

        Deflater deflater = new Deflater(compressionLevel);
        try {
            for (int y = 0; y < height; y++) {
                image.getRGB(0, y, width, 1, argb, 0, width);
                unpack(argb, cur, alpha);
                if (trial == null) {
                    filter(filterStrategy.type, cur, prev, filtered, bpp, false,
                        Long.MAX_VALUE);
                } else {
                    long best = Long.MAX_VALUE;
                    for (int type : candidates) {
                        long sum = filter(type, cur, prev, trial, bpp, true,
                            best);
                        if (sum < best) {
                            best = sum;
                            byte[] swap = filtered;
                            filtered = trial;
                            trial = swap;
                            if (best == 0) {
                                break;
                            }
                        }
                    }
                }
                deflater.setInput(filtered, 0, stride + 1);
                while (!deflater.needsInput()) {
                    out.deflate(deflater);
                }
                byte[] swap = prev;
                prev = cur;
                cur = swap;
            }
            deflater.finish();
            while (!deflater.finished()) {
                out.deflate(deflater);
            }
        } finally {
            deflater.end();
        }

        return out.toPng(width, height,
            alpha ? COLOR_TYPE_RGBA : COLOR_TYPE_RGB);
    }

    /**
     * Convert ARGB pixels to PNG RGB or RGBA bytes.
     *
     * @param argb the pixels
     * @param out the destination
     * @param alpha if true, also write the alpha byte
     */
    private static void unpack(final int[] argb, final byte[] out,
        final boolean alpha) {

        int i = 0;
        if (alpha) {
            for (int pixel : argb) {
                out[i] = (byte) (pixel >>> 16);
                out[i + 1] = (byte) (pixel >>> 8);
                out[i + 2] = (byte) pixel;
                out[i + 3] = (byte) (pixel >>> 24);
                i += 4;
            }
        } else {
            for (int pixel : argb) {
                out[i] = (byte) (pixel >>> 16);
                out[i + 1] = (byte) (pixel >>> 8);
                out[i + 2] = (byte) pixel;
                i += 3;
            }
        }
    }

    /**
     * Filter one scanline.
     *
     * @param type the PNG filter type, 0 to 4
     * @param cur the raw scanline
     * @param prev the raw previous scanline (all zero for the first row)
     * @param out the destination: the type byte, then the filtered bytes
     * @param bpp bytes per pixel
     * @param measure if true, compute the sum of absolute differences
     * @param limit when measuring, the filter may stop early once the sum
     *              exceeds this
     * @return the sum of absolute (signed byte) differences, a value
     * greater than limit if the filter was abandoned, or 0 if not measuring
     */
    private static long filter(final int type, final byte[] cur,
        final byte[] prev, final byte[] out, final int bpp,
        final boolean measure, final long limit) {

        out[0] = (byte) type;
        final int n = cur.length;
        long sum = 0;
        switch (type) {
        case 0 -> {
            System.arraycopy(cur, 0, out, 1, n);
            if (measure) {
                for (int k = 0; k < n; k++) {
                    sum += Math.abs(cur[k]);
                }
            }
        }
        case 1 -> {
            for (int k = 0; k < bpp; k++) {
                out[k + 1] = cur[k];
            }
            for (int k = bpp; k < n; k++) {
                out[k + 1] = (byte) (cur[k] - cur[k - bpp]);
            }
            if (measure) {
                sum = sumAbs(out, n);
            }
        }
        case 2 -> {
            for (int k = 0; k < n; k++) {
                out[k + 1] = (byte) (cur[k] - prev[k]);
            }
            if (measure) {
                sum = sumAbs(out, n);
            }
        }
        case 3 -> {
            for (int k = 0; k < bpp; k++) {
                out[k + 1] = (byte) (cur[k] - ((prev[k] & 0xFF) >>> 1));
            }
            if (measure) {
                sum = sumAbs(out, bpp);
            }
            for (int k = bpp; k < n; k++) {
                int avg = ((cur[k - bpp] & 0xFF) + (prev[k] & 0xFF)) >>> 1;
                byte residual = (byte) (cur[k] - avg);
                out[k + 1] = residual;
                if (measure) {
                    sum += Math.abs(residual);
                    if (sum > limit) {
                        return sum;
                    }
                }
            }
        }
        default -> {
            for (int k = 0; k < bpp; k++) {
                // With no left pixel, the Paeth predictor is the one above.
                out[k + 1] = (byte) (cur[k] - prev[k]);
            }
            if (measure) {
                sum = sumAbs(out, bpp);
            }
            for (int k = bpp; k < n; k++) {
                byte residual = (byte) (cur[k] - paeth(cur[k - bpp] & 0xFF,
                    prev[k] & 0xFF, prev[k - bpp] & 0xFF));
                out[k + 1] = residual;
                if (measure) {
                    sum += Math.abs(residual);
                    if (sum > limit) {
                        return sum;
                    }
                }
            }
        }
        }
        return sum;
    }

    /**
     * Sum the absolute signed values of a filtered row.
     *
     * @param row the type byte followed by the filtered bytes
     * @param n number of filtered bytes
     * @return the sum
     */
    private static long sumAbs(final byte[] row, final int n) {
        long sum = 0;
        for (int k = 1; k <= n; k++) {
            sum += Math.abs(row[k]);
        }
        return sum;
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

    /**
     * Collects the zlib stream in a list of blocks that the deflater
     * writes into directly, then assembles the PNG file in one exactly
     * sized array.  Unlike a doubling buffer, this never copies the
     * compressed data more than once.
     */
    private static final class Output {

        /**
         * The smallest block.
         */
        private static final int MIN_BLOCK = 256;

        /**
         * The largest block.  Blocks double in size up to this.
         */
        private static final int MAX_BLOCK = 64 * 1024;

        /**
         * Blocks that are completely filled.
         */
        private final List<byte[]> full = new ArrayList<>();

        /**
         * Total bytes in the full blocks.
         */
        private long fullSize;

        /**
         * The block being filled.
         */
        private byte[] block;

        /**
         * Bytes written to the current block.
         */
        private int used;

        /**
         * Create the output.  Screen content usually compresses to a
         * small fraction of the raw size, so the first block is sized
         * from that.
         *
         * @param rawSize the size of the filtered image data
         */
        Output(final long rawSize) {
            block = new byte[(int) Math.max(MIN_BLOCK,
                Math.min(MAX_BLOCK, rawSize / 16))];
        }

        /**
         * Run the deflater once into the free space, starting a new block
         * first if the current one is full.
         *
         * @param deflater the deflater
         */
        void deflate(final Deflater deflater) {
            if (used == block.length) {
                fullSize += used;
                if (fullSize + HEADER_SIZE + TRAILER_SIZE > MAX_ARRAY_SIZE) {
                    throw new IllegalArgumentException(
                        "Encoded PNG is too large");
                }
                full.add(block);
                block = new byte[Math.min(MAX_BLOCK, block.length * 2)];
                used = 0;
            }
            used += deflater.deflate(block, used, block.length - used);
        }

        /**
         * Assemble the PNG file around the collected zlib stream.
         *
         * @param width the image width
         * @param height the image height
         * @param colorType the IHDR color type
         * @return the PNG file bytes
         */
        byte[] toPng(final int width, final int height, final int colorType) {
            long idatLength = fullSize + used;
            if (idatLength + HEADER_SIZE + TRAILER_SIZE > MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException("Encoded PNG is too large");
            }
            byte[] png = new byte[(int) idatLength + HEADER_SIZE
                + TRAILER_SIZE];
            System.arraycopy(SIGNATURE, 0, png, 0, SIGNATURE.length);
            putInt(png, 8, 13);
            putAscii(png, 12, "IHDR");
            putInt(png, 16, width);
            putInt(png, 20, height);
            png[24] = 8;
            png[25] = (byte) colorType;
            // Compression, filter, and interlace methods are all 0.
            CRC32 crc = new CRC32();
            crc.update(png, 12, 17);
            putInt(png, 29, (int) crc.getValue());
            putInt(png, 33, (int) idatLength);
            putAscii(png, 37, "IDAT");
            int at = HEADER_SIZE;
            for (byte[] b : full) {
                System.arraycopy(b, 0, png, at, b.length);
                at += b.length;
            }
            System.arraycopy(block, 0, png, at, used);
            at += used;
            crc.reset();
            crc.update(png, 37, at - 37);
            putInt(png, at, (int) crc.getValue());
            System.arraycopy(IEND, 0, png, at + 4, IEND.length);
            return png;
        }

        /**
         * Write a four-letter chunk type.
         *
         * @param out the destination
         * @param offset the index of the first byte
         * @param type the chunk type
         */
        private static void putAscii(final byte[] out, final int offset,
            final String type) {

            for (int i = 0; i < 4; i++) {
                out[offset + i] = (byte) type.charAt(i);
            }
        }
    }
}
