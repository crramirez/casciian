/*
 * Casciian Java Desktop add-on
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
package casciian.javadesktop.encoders;

import casciian.bits.ImageRGB;
import casciian.image.encoders.ImageEncoder;
import casciian.image.encoders.PngImageEncoder;

import java.io.IOException;

/**
 * Encoder that picks lossless PNG or lossy JPEG for each image.  It is the
 * preferred encoder when the Java Desktop add-on is present.
 *
 * <p>An image is encoded as PNG when any of these is true, and as JPEG
 * otherwise:</p>
 *
 * <ol>
 *   <li>alpha is requested and some pixel is not fully opaque, since JPEG
 *   has no alpha channel;</li>
 *   <li>it has fewer than {@link #getMinJpegPixels()} pixels: JPEG's fixed
 *   headers outweigh its savings on tiny images;</li>
 *   <li>it has at most {@link #getMaxPngColors()} distinct colors, like
 *   text, borders, charts and pixel art;</li>
 *   <li>at least {@link #getMinPngRepeatFraction()} of its pixels equal
 *   their left or upper neighbor.  Flat areas, anti-aliased text and smooth
 *   gradients repeat a lot and compress well with PNG's row filters, while
 *   photographs and renders almost never repeat.</li>
 * </ol>
 *
 * <p>On photographs JPEG is typically 5 to 13 times smaller than PNG and
 * faster to encode; on UI content PNG is smaller and has no artifacts.
 * The scan stops as soon as the outcome is known.</p>
 *
 * <p>PNG files come from the built-in {@link PngImageEncoder}, which is
 * faster and usually smaller than ImageIO's PNG writer for this kind of
 * content; JPEG files come from {@link ImageIOJpegEncoder}.
 * {@link #getMimeType()} reports the generic {@code image/*}; consumers
 * such as the iTerm2 inline images protocol detect the actual format from
 * the file signature.</p>
 */
public class ImageIOAdaptiveEncoder implements ImageEncoder {

    /**
     * The priority: above the built-in PNG encoder (10).
     */
    public static final int PRIORITY = 20;

    /**
     * Default for {@link #getMinJpegPixels()}.
     */
    public static final int DEFAULT_MIN_JPEG_PIXELS = 4096;

    /**
     * Default for {@link #getMaxPngColors()}.
     */
    public static final int DEFAULT_MAX_PNG_COLORS = 256;

    /**
     * Default for {@link #getMinPngRepeatFraction()}.
     */
    public static final double DEFAULT_MIN_PNG_REPEAT_FRACTION = 0.5;

    /**
     * The lossless encoder.
     */
    private final ImageEncoder pngEncoder;

    /**
     * The lossy encoder.
     */
    private final ImageEncoder jpegEncoder;

    /**
     * Images with fewer pixels are always lossless.
     */
    private final int minJpegPixels;

    /**
     * Images with at most this many distinct colors are lossless.
     */
    private final int maxPngColors;

    /**
     * Images where at least this fraction of pixels repeat a neighbor are
     * lossless.
     */
    private final double minPngRepeatFraction;

    /**
     * Create an encoder with the default thresholds, the built-in
     * {@link PngImageEncoder} and an {@link ImageIOJpegEncoder}.
     */
    public ImageIOAdaptiveEncoder() {
        this(new PngImageEncoder(), new ImageIOJpegEncoder(),
            DEFAULT_MIN_JPEG_PIXELS, DEFAULT_MAX_PNG_COLORS,
            DEFAULT_MIN_PNG_REPEAT_FRACTION);
    }

    /**
     * Create an encoder with custom delegates and thresholds.
     *
     * @param pngEncoder the lossless encoder, which must support alpha
     * @param jpegEncoder the lossy encoder
     * @param minJpegPixels images with fewer pixels are always lossless
     * @param maxPngColors images with at most this many distinct colors
     * are lossless
     * @param minPngRepeatFraction images where at least this fraction of
     * pixels equal their left or upper neighbor are lossless; a value above
     * 1 disables this rule
     * @throws IllegalArgumentException if an encoder is null, the lossless
     * encoder does not support alpha, or a threshold is negative
     */
    public ImageIOAdaptiveEncoder(final ImageEncoder pngEncoder,
        final ImageEncoder jpegEncoder, final int minJpegPixels,
        final int maxPngColors, final double minPngRepeatFraction) {

        if (pngEncoder == null || jpegEncoder == null) {
            throw new IllegalArgumentException("encoders must not be null");
        }
        if (!pngEncoder.supportsAlpha()) {
            throw new IllegalArgumentException(
                "the lossless encoder must support alpha");
        }
        if (minJpegPixels < 0 || maxPngColors < 0
            || !(minPngRepeatFraction >= 0)) {
            throw new IllegalArgumentException(
                "thresholds must not be negative");
        }
        this.pngEncoder = pngEncoder;
        this.jpegEncoder = jpegEncoder;
        this.minJpegPixels = minJpegPixels;
        this.maxPngColors = maxPngColors;
        this.minPngRepeatFraction = minPngRepeatFraction;
    }

    /**
     * Get the pixel count below which images are always lossless.
     *
     * @return the pixel threshold
     */
    public int getMinJpegPixels() {
        return minJpegPixels;
    }

    /**
     * Get the distinct color count up to which images are lossless.
     *
     * @return the color threshold
     */
    public int getMaxPngColors() {
        return maxPngColors;
    }

    /**
     * Get the fraction of pixels equal to their left or upper neighbor from
     * which images are lossless.
     *
     * @return the fraction
     */
    public double getMinPngRepeatFraction() {
        return minPngRepeatFraction;
    }

    @Override
    public String getName() {
        return "adaptive";
    }

    @Override
    public String getMimeType() {
        return "image/*";
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean supportsAlpha() {
        return true;
    }

    @Override
    public byte[] encode(final ImageRGB image, final boolean alpha)
        throws IOException {

        return selectEncoder(image, alpha).encode(image, alpha);
    }

    /**
     * Choose the encoder for an image, following the rules in the class
     * documentation.
     *
     * @param image the image
     * @param alpha whether alpha is requested
     * @return the lossless or the lossy encoder
     */
    public ImageEncoder selectEncoder(final ImageRGB image,
        final boolean alpha) {

        if (image == null) {
            throw new IllegalArgumentException("image must not be null");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        long total = (long) width * height;
        if (total < minJpegPixels || total == 0) {
            return pngEncoder;
        }
        if (maxPngColors >= Math.min(total, 0x1000000L)) {
            return pngEncoder;
        }
        double repeatsNeeded = minPngRepeatFraction * total;

        ColorSet colors = new ColorSet(maxPngColors);
        boolean manyColors = false;
        long repeats = 0;
        long remaining = total;
        int[] row = new int[width];
        int[] above = new int[width];
        for (int y = 0; y < height; y++) {
            int[] swap = above;
            above = row;
            row = image.getRGB(0, y, width, 1, swap, 0, width);
            for (int x = 0; x < width; x++) {
                int argb = row[x];
                if (alpha && (argb >>> 24) != 0xFF) {
                    return pngEncoder;
                }
                if ((x > 0 && argb == row[x - 1])
                    || (y > 0 && argb == above[x])) {
                    repeats++;
                } else if (!manyColors) {
                    manyColors = !colors.add(argb | 0xFF000000);
                }
            }
            remaining -= width;
            // Stop early once the remaining rows cannot change the result.
            if (repeats >= repeatsNeeded) {
                return pngEncoder;
            }
            if (!alpha && manyColors && repeats + remaining < repeatsNeeded) {
                return jpegEncoder;
            }
        }
        return (manyColors && repeats < repeatsNeeded)
            ? jpegEncoder : pngEncoder;
    }

    /**
     * A small open-addressing set of opaque colors with a fixed capacity.
     */
    private static final class ColorSet {

        /**
         * Hash slots; 0 marks an empty slot, which is safe because every
         * stored color has its alpha byte set.
         */
        private final int[] slots;

        /**
         * The maximum number of colors.
         */
        private final int limit;

        /**
         * Right shift that maps a 32-bit hash to a slot index.
         */
        private final int shift;

        /**
         * The number of colors stored.
         */
        private int size;

        /**
         * Create a set.
         *
         * @param limit the maximum number of colors
         */
        ColorSet(final int limit) {
            this.limit = limit;
            int capacity = Integer.highestOneBit(Math.max(4, limit * 2)) * 2;
            this.slots = new int[capacity];
            this.shift = Integer.numberOfLeadingZeros(capacity - 1);
        }

        /**
         * Add a color.
         *
         * @param color an opaque color
         * @return false if the color is new and the set is full
         */
        boolean add(final int color) {
            int mask = slots.length - 1;
            int i = (color * 0x9E3779B9) >>> shift;
            while (slots[i] != 0) {
                if (slots[i] == color) {
                    return true;
                }
                i = (i + 1) & mask;
            }
            if (size == limit) {
                return false;
            }
            slots[i] = color;
            size++;
            return true;
        }
    }
}
