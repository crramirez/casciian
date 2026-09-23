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

import casciian.bits.ArrayImageRGB;
import casciian.bits.ImageRGB;
import casciian.image.decoders.ImageDecoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * High-performance, pure-Java PNG image decoder.
 *
 * <p>This decoder implements the baseline (non-interlaced) subset of the PNG
 * specification without relying on {@code java.desktop} or
 * {@code javax.imageio}, so it remains compatible with a GraalVM native image.
 * It parses the {@code IHDR}, {@code PLTE}, {@code tRNS}, {@code IDAT} and
 * {@code IEND} chunks and silently skips every other (ancillary) chunk.</p>
 *
 * <p>Supported color types:</p>
 * <ul>
 *   <li>Type 0 — Grayscale (1, 2, 4, 8 bits per sample)</li>
 *   <li>Type 2 — Truecolor RGB (8 bits per channel)</li>
 *   <li>Type 3 — Indexed / palette (1, 2, 4, 8 bits per sample)</li>
 *   <li>Type 6 — Truecolor RGBA (8 bits per channel)</li>
 * </ul>
 *
 * <p>Transparency is fully supported: alpha lookup tables for palette images
 * ({@code tRNS} on type 3) and single key-color chroma transparency for
 * grayscale and truecolor images ({@code tRNS} on types 0 and 2).</p>
 *
 * <p>Adam7 interlacing and 16-bit sample depth are intentionally not supported
 * and cause an {@link UnsupportedOperationException}.</p>
 *
 * <p>The scanline unfiltering and color-expansion loops are written as flat,
 * branch-free primitive-array loops so the HotSpot C2 JIT can auto-vectorize
 * them (SuperWord / SIMD). Filter-type and color-type dispatch happens once per
 * scanline, outside the per-pixel loops, and all buffers are pre-allocated
 * before row processing begins.</p>
 */
public class PNGImageDecoder implements ImageDecoder {

    /**
     * The 8-byte PNG file signature.
     */
    private static final byte[] SIGNATURE = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    // Chunk type identifiers, encoded as big-endian 32-bit integers.
    private static final int IHDR = 0x49484452;
    private static final int PLTE = 0x504C5445;
    private static final int TRNS = 0x74524E53;
    private static final int IDAT = 0x49444154;
    private static final int IEND = 0x49454E44;

    /**
     * Public constructor.
     */
    public PNGImageDecoder() {
        // Explicit no-arg constructor for ServiceLoader.
    }

    @Override
    public ImageRGB decode(final InputStream inputStream, final String mimeType)
        throws IOException {

        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream cannot be null");
        }

        byte[] data = inputStream.readAllBytes();
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);

        readSignature(buffer);

        int width = 0;
        int height = 0;
        int bitDepth = 0;
        int colorType = 0;
        byte[] palette = null;
        byte[] transparency = null;
        boolean sawIhdr = false;

        // Accumulate the (possibly split) compressed IDAT payload.
        ByteArrayOutputStream idat = new ByteArrayOutputStream();

        boolean sawIend = false;
        while (buffer.remaining() >= 8 && !sawIend) {
            int length = buffer.getInt();
            if (length < 0 || (long) buffer.remaining() < (long) length + 8L) {
                throw new IOException("Corrupt PNG: chunk length out of bounds");
            }
            int type = buffer.getInt();
            int dataStart = buffer.position();

            switch (type) {
            case IHDR:
                if (length != 13) {
                    throw new IOException("Corrupt PNG: invalid IHDR length");
                }
                width = buffer.getInt();
                height = buffer.getInt();
                bitDepth = buffer.get() & 0xFF;
                colorType = buffer.get() & 0xFF;
                int compressionMethod = buffer.get() & 0xFF;
                int filterMethod = buffer.get() & 0xFF;
                int interlaceMethod = buffer.get() & 0xFF;

                if (width <= 0 || height <= 0) {
                    throw new IOException("Corrupt PNG: invalid image dimensions");
                }
                if (compressionMethod != 0) {
                    throw new IOException(
                        "Unsupported PNG: compression method " + compressionMethod);
                }
                if (filterMethod != 0) {
                    throw new IOException(
                        "Unsupported PNG: filter method " + filterMethod);
                }
                if (interlaceMethod != 0) {
                    throw new UnsupportedOperationException(
                        "Adam7 interlaced PNG images are not supported");
                }
                if (bitDepth == 16) {
                    throw new UnsupportedOperationException(
                        "16-bit PNG images are not supported");
                }
                validateColorType(colorType, bitDepth);
                sawIhdr = true;
                break;

            case PLTE:
                if (length < 3 || length > 768 || length % 3 != 0) {
                    throw new IOException("Corrupt PNG: invalid PLTE length");
                }
                palette = new byte[length];
                buffer.get(palette);
                break;

            case TRNS:
                transparency = new byte[length];
                buffer.get(transparency);
                break;

            case IDAT:
                byte[] chunk = new byte[length];
                buffer.get(chunk);
                idat.write(chunk, 0, length);
                break;

            case IEND:
                if (length != 0) {
                    throw new IOException("Corrupt PNG: non-empty IEND chunk");
                }
                sawIend = true;
                break;

            default:
                if (isCriticalChunk(type)) {
                    throw new IOException("Unsupported PNG: unknown critical chunk");
                }
                break;
            }

            // Reposition to the chunk end and skip the 4-byte CRC.
            buffer.position(dataStart + length + 4);
        }

        if (!sawIhdr) {
            throw new IOException("Corrupt PNG: missing IHDR chunk");
        }
        if (!sawIend) {
            throw new IOException("Corrupt PNG: missing IEND chunk");
        }
        if (colorType == 3 && palette == null) {
            throw new IOException("Corrupt PNG: indexed image without PLTE");
        }

        int channels = channelCount(colorType);
        int bitsPerPixel = channels * bitDepth;
        int bytesPerPixel = Math.max(1, bitsPerPixel / 8);
        int stride = checkedArrayLength(
            (((long) width * bitsPerPixel) + 7L) / 8L);
        int pixelCount = checkedArrayLength((long) width * height);
        int rawSize = checkedArrayLength((long) height * (stride + 1L));

        byte[] raw = inflate(idat.toByteArray(), rawSize);

        int[] rgba = expandImage(raw, width, height, stride, pixelCount,
            bytesPerPixel,
            bitDepth, colorType, palette, transparency);

        ImageRGB image = new ArrayImageRGB(width, height);
        image.setRGB(0, 0, width, height, rgba, 0, width);
        return image;
    }

    /**
     * Read and validate the 8-byte PNG signature.
     */
    private static void readSignature(final ByteBuffer buffer) throws IOException {
        if (buffer.remaining() < SIGNATURE.length) {
            throw new IOException("Not a PNG file: truncated signature");
        }
        for (byte b : SIGNATURE) {
            if (buffer.get() != b) {
                throw new IOException("Not a PNG file: bad signature");
            }
        }
    }

    /**
     * Validate that the bit depth is allowed for the given color type.
     */
    private static void validateColorType(final int colorType, final int bitDepth)
        throws IOException {

        boolean valid = switch (colorType) {
            case 0 -> bitDepth == 1 || bitDepth == 2 || bitDepth == 4
                || bitDepth == 8;
            case 3 -> bitDepth == 1 || bitDepth == 2 || bitDepth == 4
                || bitDepth == 8;
            case 2, 6 -> bitDepth == 8;
            default -> false;
        };
        if (!valid) {
            throw new IOException("Unsupported PNG: color type " + colorType
                + " with bit depth " + bitDepth);
        }
    }

    /**
     * Number of samples per pixel for the given color type.
     */
    private static int channelCount(final int colorType) throws IOException {
        return switch (colorType) {
            case 0 -> 1; // grayscale
            case 2 -> 3; // truecolor
            case 3 -> 1; // indexed
            case 6 -> 4; // truecolor + alpha
            default -> throw new IOException(
                "Unsupported PNG: color type " + colorType);
        };
    }

    /**
     * Validate that an array-backed PNG buffer size fits in a Java array.
     */
    private static int checkedArrayLength(final long length) throws IOException {
        if (length < 0 || length > Integer.MAX_VALUE) {
            throw new IOException("Unsupported PNG: image too large");
        }
        return (int) length;
    }

    /**
     * PNG critical chunks have an uppercase first type byte.
     */
    private static boolean isCriticalChunk(final int type) {
        return (type & 0x20000000) == 0;
    }

    /**
     * Inflate the concatenated IDAT payload into the raw filtered scanlines.
     * The {@link Inflater} is always released via {@code end()}.
     */
    private static byte[] inflate(final byte[] compressed, final int expectedSize)
        throws IOException {

        byte[] raw = new byte[expectedSize];
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(compressed);
            int offset = 0;
            while (offset < expectedSize && !inflater.finished()) {
                int n = inflater.inflate(raw, offset, expectedSize - offset);
                if (n == 0) {
                    if (inflater.finished() || inflater.needsDictionary()) {
                        break;
                    }
                    if (inflater.needsInput()) {
                        // No more input available but output incomplete.
                        break;
                    }
                }
                offset += n;
            }
            if (offset != expectedSize) {
                throw new IOException(
                    "Corrupt PNG: decompressed size mismatch (expected "
                        + expectedSize + ", got " + offset + ")");
            }
            byte[] probe = new byte[1];
            if (inflater.inflate(probe, 0, 1) > 0 || !inflater.finished()) {
                throw new IOException("Corrupt PNG: invalid zlib stream size");
            }
            return raw;
        } catch (DataFormatException e) {
            throw new IOException("Corrupt PNG: invalid zlib stream", e);
        } finally {
            inflater.end();
        }
    }

    /**
     * Unfilter every scanline and expand the samples into a flat packed-ARGB
     * {@code int[]} array. All working buffers are pre-allocated here, outside
     * the per-row and per-pixel loops.
     */
    private static int[] expandImage(final byte[] raw, final int width,
        final int height, final int stride, final int pixelCount,
        final int bpp, final int bitDepth, final int colorType,
        final byte[] palette, final byte[] transparency) throws IOException {

        int[] rgba = new int[pixelCount];
        byte[] curr = new byte[stride];
        byte[] prev = new byte[stride];

        // Pre-compute color lookup tables (branchless expansion).
        int[] grayTable = (colorType == 0)
            ? buildGrayTable(bitDepth, transparency) : null;
        int[] paletteTable = (colorType == 3)
            ? buildPaletteTable(palette, transparency) : null;
        int paletteEntries = (colorType == 3) ? (palette.length / 3) : 0;
        int[] sampleRow = (colorType == 0 || colorType == 3)
            ? new int[width] : null;

        // Key-color transparency for truecolor RGB (color type 2).
        boolean rgbKeyed = (colorType == 2 && transparency != null
            && transparency.length >= 6);
        int keyR = rgbKeyed ? (transparency[1] & 0xFF) : 0;
        int keyG = rgbKeyed ? (transparency[3] & 0xFF) : 0;
        int keyB = rgbKeyed ? (transparency[5] & 0xFF) : 0;

        int rawPos = 0;
        for (int y = 0; y < height; y++) {
            int filterType = raw[rawPos++] & 0xFF;
            System.arraycopy(raw, rawPos, curr, 0, stride);
            rawPos += stride;

            unfilter(filterType, curr, prev, bpp, stride);

            int off = y * width;
            switch (colorType) {
            case 0:
                unpackSamples(curr, sampleRow, width, bitDepth);
                for (int x = 0; x < width; x++) {
                    rgba[off + x] = grayTable[sampleRow[x]];
                }
                break;
            case 3:
                unpackSamples(curr, sampleRow, width, bitDepth);
                for (int x = 0; x < width; x++) {
                    int index = sampleRow[x];
                    if (index >= paletteEntries) {
                        throw new IOException(
                            "Corrupt PNG: palette index out of bounds");
                    }
                    rgba[off + x] = paletteTable[index];
                }
                break;
            case 2:
                if (rgbKeyed) {
                    for (int x = 0; x < width; x++) {
                        int p = x * 3;
                        int r = curr[p] & 0xFF;
                        int g = curr[p + 1] & 0xFF;
                        int b = curr[p + 2] & 0xFF;
                        int a = ((r == keyR) & (g == keyG) & (b == keyB))
                            ? 0 : 0xFF;
                        rgba[off + x] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                } else {
                    for (int x = 0; x < width; x++) {
                        int p = x * 3;
                        int r = curr[p] & 0xFF;
                        int g = curr[p + 1] & 0xFF;
                        int b = curr[p + 2] & 0xFF;
                        rgba[off + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
                    }
                }
                break;
            case 6:
                for (int x = 0; x < width; x++) {
                    int p = x * 4;
                    int r = curr[p] & 0xFF;
                    int g = curr[p + 1] & 0xFF;
                    int b = curr[p + 2] & 0xFF;
                    int a = curr[p + 3] & 0xFF;
                    rgba[off + x] = (a << 24) | (r << 16) | (g << 8) | b;
                }
                break;
            default:
                throw new IOException("Unsupported PNG: color type " + colorType);
            }

            // Swap buffers: the row just decoded becomes the previous row.
            byte[] tmp = prev;
            prev = curr;
            curr = tmp;
        }

        return rgba;
    }

    /**
     * Reverse the PNG scanline filter for a single row. The filter type is
     * dispatched here, once per row; each case is a flat, branch-free loop over
     * the scanline bytes.
     */
    private static void unfilter(final int filterType, final byte[] curr,
        final byte[] prev, final int bpp, final int stride) throws IOException {

        switch (filterType) {
        case 0:
            // None: bytes are already unfiltered.
            break;
        case 1:
            // Sub: add the byte bpp positions to the left.
            for (int i = bpp; i < stride; i++) {
                curr[i] = (byte) (curr[i] + curr[i - bpp]);
            }
            break;
        case 2:
            // Up: add the byte directly above.
            for (int i = 0; i < stride; i++) {
                curr[i] = (byte) (curr[i] + prev[i]);
            }
            break;
        case 3:
            // Average: add floor((left + above) / 2).
            for (int i = 0; i < bpp; i++) {
                curr[i] = (byte) (curr[i] + ((prev[i] & 0xFF) >> 1));
            }
            for (int i = bpp; i < stride; i++) {
                curr[i] = (byte) (curr[i]
                    + (((curr[i - bpp] & 0xFF) + (prev[i] & 0xFF)) >> 1));
            }
            break;
        case 4:
            // Paeth: add the Paeth predictor of left, above, upper-left.
            for (int i = 0; i < bpp; i++) {
                curr[i] = (byte) (curr[i] + (prev[i] & 0xFF));
            }
            for (int i = bpp; i < stride; i++) {
                int a = curr[i - bpp] & 0xFF;
                int b = prev[i] & 0xFF;
                int c = prev[i - bpp] & 0xFF;
                int p = a + b - c;
                int pa = Math.abs(p - a);
                int pb = Math.abs(p - b);
                int pc = Math.abs(p - c);
                int pred = (pa <= pb && pa <= pc) ? a : (pb <= pc ? b : c);
                curr[i] = (byte) (curr[i] + pred);
            }
            break;
        default:
            throw new IOException("Corrupt PNG: unknown filter type " + filterType);
        }
    }

    /**
     * Unpack {@code width} samples of {@code bitDepth} bits each (MSB-first)
     * from a scanline into {@code sampleRow}. For 8-bit depth this is a plain
     * byte copy.
     */
    private static void unpackSamples(final byte[] curr, final int[] sampleRow,
        final int width, final int bitDepth) {

        if (bitDepth == 8) {
            for (int x = 0; x < width; x++) {
                sampleRow[x] = curr[x] & 0xFF;
            }
            return;
        }
        int mask = (1 << bitDepth) - 1;
        for (int x = 0; x < width; x++) {
            int bitPos = x * bitDepth;
            int bytePos = bitPos >> 3;
            int shift = 8 - bitDepth - (bitPos & 7);
            sampleRow[x] = (curr[bytePos] >> shift) & mask;
        }
    }

    /**
     * Build the grayscale sample -&gt; packed-ARGB lookup table, scaling each
     * sample to the full 0-255 range and applying key-color transparency.
     */
    private static int[] buildGrayTable(final int bitDepth,
        final byte[] transparency) {

        int maxValue = (1 << bitDepth) - 1;
        int[] table = new int[maxValue + 1];
        for (int v = 0; v <= maxValue; v++) {
            int gray = v * 255 / maxValue;
            table[v] = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
        }
        if (transparency != null && transparency.length >= 2) {
            int key = ((transparency[0] & 0xFF) << 8) | (transparency[1] & 0xFF);
            if (key <= maxValue) {
                table[key] = table[key] & 0x00FFFFFF;
            }
        }
        return table;
    }

    /**
     * Build the palette index -&gt; packed-ARGB lookup table, applying the
     * optional {@code tRNS} alpha values. Indices without an explicit alpha
     * entry are fully opaque.
     */
    private static int[] buildPaletteTable(final byte[] palette,
        final byte[] transparency) {

        int entries = palette.length / 3;
        // Size the table to at least 256 so any in-range index is safe.
        int size = Math.max(256, entries);
        int[] table = new int[size];
        for (int i = 0; i < entries; i++) {
            int r = palette[i * 3] & 0xFF;
            int g = palette[i * 3 + 1] & 0xFF;
            int b = palette[i * 3 + 2] & 0xFF;
            table[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        if (transparency != null) {
            int n = Math.min(transparency.length, entries);
            for (int i = 0; i < n; i++) {
                int a = transparency[i] & 0xFF;
                table[i] = (table[i] & 0x00FFFFFF) | (a << 24);
            }
        }
        return table;
    }

    @Override
    public List<String> getSupportedMimeTypes() {
        return List.of("image/png");
    }

    @Override
    public String getFileExtensionPattern() {
        return "^.*\\.[pP][nN][gG]$";
    }

    @Override
    public String getFormatDescription() {
        return "PNG Image Files (*.png)";
    }
}
