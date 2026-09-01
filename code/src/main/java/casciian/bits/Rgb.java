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

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * A record to hold RGB color components with utility methods for color
 * manipulation and conversion.
 *
 * <p>This record provides methods for:
 * <ul>
 *   <li>Extracting components from packed RGB integers</li>
 *   <li>Combining components into packed integers</li>
 *   <li>Computing color distances</li>
 *   <li>Converting to sixel color space</li>
 *   <li>Alpha/transparency handling</li>
 * </ul>
 *
 * @param r the red component (0-255 for RGB, 0-100 for sixel)
 * @param g the green component (0-255 for RGB, 0-100 for sixel)
 * @param b the blue component (0-255 for RGB, 0-100 for sixel)
 */
public record Rgb(int r, int g, int b) {

    // ========================================================================
    // Constants
    // ========================================================================

    /** Alpha value (0 - 255) above which to consider the pixel opaque (~40%). */
    public static final int ALPHA_OPAQUE = 102;

    /** Sixel white color value (100, 100, 100). */
    public static final int SIXEL_WHITE = 0xFF646464;

    /** Sixel black color value (0, 0, 0). */
    public static final int SIXEL_BLACK = 0xFF000000;

    /**
     * Largest possible squared RGB distance between two colors:
     * {@code 3 * 255^2}.
     */
    private static final int MAX_DISTANCE_SQUARED = 3 * 255 * 255;

    // ========================================================================
    // Factory Methods
    // ========================================================================

    /**
     * Extract RGB components from a packed integer.
     *
     * @param rgb the packed RGB integer
     * @return a record containing the red, green, and blue components
     */
    public static Rgb fromPackedRgb(final int rgb) {
        int red = (rgb >>> 16) & 0xFF;
        int green = (rgb >>> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return new Rgb(red, green, blue);
    }

    // ========================================================================
    // Instance Methods
    // ========================================================================

    /**
     * Combine the RGB components into a packed opaque (alpha=0xFF) integer.
     *
     * @return the packed ARGB integer
     */
    public int toPackedRgb() {
        return (0xFF << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /**
     * Combine the RGB components into a packed integer without alpha.
     *
     * @return the packed RGB integer (no alpha)
     */
    public int toPackedRgbNoAlpha() {
        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /**
     * Calculate the squared Euclidean distance to another color.
     *
     * @param other the other color
     * @return the squared distance
     */
    public int distanceSquaredTo(final Rgb other) {
        int dr = this.r - other.r;
        int dg = this.g - other.g;
        int db = this.b - other.b;
        return dr * dr + dg * dg + db * db;
    }

    /**
     * Calculate the squared Euclidean distance to RGB components.
     *
     * @param red the red component
     * @param green the green component
     * @param blue the blue component
     * @return the squared distance
     */
    public int distanceSquaredTo(final int red, final int green, final int blue) {
        int dr = this.r - red;
        int dg = this.g - green;
        int db = this.b - blue;
        return dr * dr + dg * dg + db * db;
    }

    /**
     * Convert to sixel color space (0-100 per component).
     *
     * @return a new Rgb with components scaled to 0-100
     */
    public Rgb toSixelSpace() {
        return new Rgb(r * 100 / 255, g * 100 / 255, b * 100 / 255);
    }

    /**
     * Clamp components to sixel range [0, 100].
     *
     * @return a new Rgb with clamped components
     */
    public Rgb clampSixel() {
        return new Rgb(
            Math.clamp(r, 0, 100),
            Math.clamp(g, 0, 100),
            Math.clamp(b, 0, 100)
        );
    }

    /**
     * Add error values to components (for dithering).
     *
     * @param redError the red error to add
     * @param greenError the green error to add
     * @param blueError the blue error to add
     * @return a new Rgb with error added and clamped to [0, 100]
     */
    public Rgb addErrorAndClamp(final int redError, final int greenError,
            final int blueError) {
        return new Rgb(
            Math.clamp((long)r + redError, 0, 100),
            Math.clamp((long)g + greenError, 0, 100),
            Math.clamp((long)b + blueError, 0, 100)
        );
    }

    /**
     * Check if this color is near black (in sixel space).
     *
     * @param threshold the distance threshold
     * @return true if near black
     */
    public boolean isNearBlack(final int threshold) {
        return (r * r + g * g + b * b) < threshold;
    }

    /**
     * Check if this color is near white (in sixel space, where white is 100,100,100).
     *
     * @param threshold the distance threshold
     * @return true if near white
     */
    public boolean isNearWhite(final int threshold) {
        int dr = 100 - r;
        int dg = 100 - g;
        int db = 100 - b;
        return (dr * dr + dg * dg + db * db) < threshold;
    }

    // ========================================================================
    // Static Utility Methods
    // ========================================================================

    /**
     * Extract the red component from a packed RGB integer.
     *
     * @param rgb the packed RGB integer
     * @return the red component (0-255)
     */
    public static int getRed(final int rgb) {
        return (rgb >>> 16) & 0xFF;
    }

    /**
     * Extract the green component from a packed RGB integer.
     *
     * @param rgb the packed RGB integer
     * @return the green component (0-255)
     */
    public static int getGreen(final int rgb) {
        return (rgb >>> 8) & 0xFF;
    }

    /**
     * Extract the blue component from a packed RGB integer.
     *
     * @param rgb the packed RGB integer
     * @return the blue component (0-255)
     */
    public static int getBlue(final int rgb) {
        return rgb & 0xFF;
    }

    /**
     * Extract the alpha component from a packed ARGB integer.
     *
     * @param argb the packed ARGB integer
     * @return the alpha component (0-255)
     */
    public static int getAlpha(final int argb) {
        return (argb >>> 24) & 0xFF;
    }

    /**
     * Combine RGB components into a packed opaque integer.
     *
     * @param red the red component (0-255)
     * @param green the green component (0-255)
     * @param blue the blue component (0-255)
     * @return the packed ARGB integer with alpha=0xFF
     */
    public static int combineRgb(final int red, final int green, final int blue) {
        return (0xFF << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    /**
     * Combine ARGB components into a packed integer.
     *
     * @param alpha the alpha component (0-255)
     * @param red the red component (0-255)
     * @param green the green component (0-255)
     * @param blue the blue component (0-255)
     * @return the packed ARGB integer
     */
    public static int combineArgb(final int alpha, final int red,
            final int green, final int blue) {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16)
               | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    /**
     * Check if a pixel is fully opaque (alpha = 0xFF).
     *
     * @param argb the packed ARGB integer
     * @return true if opaque
     */
    public static boolean isOpaque(final int argb) {
        return (argb & 0xFF000000) == 0xFF000000;
    }

    /**
     * Check if a pixel is transparent (alpha below ALPHA_OPAQUE threshold).
     *
     * @param argb the packed ARGB integer
     * @return true if transparent
     */
    public static boolean isTransparent(final int argb) {
        return getAlpha(argb) < ALPHA_OPAQUE;
    }

    /**
     * Calculate the squared Euclidean distance between two packed RGB colors.
     *
     * @param rgb1 the first color
     * @param rgb2 the second color
     * @return the squared distance
     */
    public static int distanceSquared(final int rgb1, final int rgb2) {
        int dr = getRed(rgb1) - getRed(rgb2);
        int dg = getGreen(rgb1) - getGreen(rgb2);
        int db = getBlue(rgb1) - getBlue(rgb2);
        return dr * dr + dg * dg + db * db;
    }

    /**
     * Calculate the squared distance between a packed RGB color and components.
     *
     * @param rgb the packed RGB color
     * @param red the red component
     * @param green the green component
     * @param blue the blue component
     * @return the squared distance
     */
    public static int distanceSquared(final int rgb, final int red,
            final int green, final int blue) {
        int dr = getRed(rgb) - red;
        int dg = getGreen(rgb) - green;
        int db = getBlue(rgb) - blue;
        return dr * dr + dg * dg + db * db;
    }

    /**
     * Clamp a value to the sixel range [0, 100].
     *
     * @param value the value to clamp
     * @return the clamped value
     */
    public static int clampSixelValue(final int value) {
        return Math.clamp(value, 0, 100);
    }

    /**
     * Compute the sum of squared Euclidean distances between each pixel in
     * {@code pixels[0..count)} and the reference color {@code color}, using
     * the Java Vector API for SIMD acceleration.
     *
     * <p>This is the inner-loop kernel for standard-deviation calculations in
     * {@link UnicodeGlyphImage}. Processing pixels in bulk via wide SIMD lanes
     * (8 {@code int}s per cycle on AVX2, 4 on NEON) is substantially faster
     * than calling {@link #distanceSquared(int, int)} per pixel.</p>
     *
     * <p>Each pixel is a packed 24-bit RGB value; the alpha byte (bits 31:24)
     * is ignored.</p>
     *
     * @param pixels the array of packed RGB pixels
     * @param count  the number of pixels to process (starting at index 0)
     * @param color  the reference packed RGB color
     * @return the sum of per-pixel squared distances
     */
    public static long distanceSquaredSum(final int[] pixels,
                                          final int count,
                                          final int color) {
        return distanceSquaredSum(pixels, 0, count, color);
    }

    /**
     * Compute the sum of squared Euclidean distances between each pixel in
     * {@code pixels[offset..offset + count)} and the reference color
     * {@code color}, using the Java Vector API for SIMD acceleration.
     *
     * @param pixels the array of packed RGB pixels
     * @param offset the index of the first pixel to process
     * @param count  the number of pixels to process
     * @param color  the reference packed RGB color
     * @return the sum of per-pixel squared distances
     */
    public static long distanceSquaredSum(final int[] pixels,
                                          final int offset,
                                          final int count,
                                          final int color) {
        final VectorSpecies<Integer> species = IntVector.SPECIES_PREFERRED;
        final int laneCount = species.length();

        // Broadcast the three reference channels into separate vectors.
        final IntVector vRefR = IntVector.broadcast(species, (color >>> 16) & 0xFF);
        final IntVector vRefG = IntVector.broadcast(species, (color >>>  8) & 0xFF);
        final IntVector vRefB = IntVector.broadcast(species,  color         & 0xFF);
        final IntVector vMask = IntVector.broadcast(species, 0xFF);

        // A horizontal reduction per vector iteration would dominate the
        // kernel, so accumulate into an int vector and reduce only once per
        // block. Each per-lane term is bounded by 3 * 255^2 = 195_075 and the
        // horizontal reduction adds all lanes together, so a block may span
        // at most Integer.MAX_VALUE / (195_075 * laneCount) iterations before
        // the accumulator has to be flushed into the long total.
        final int flushInterval = Math.max(1,
            (Integer.MAX_VALUE / MAX_DISTANCE_SQUARED) / laneCount);

        long sum = 0;
        int i = offset;
        final int end = offset + count;
        final int vectorLimit = end - laneCount;
        while (i <= vectorLimit) {
            IntVector acc = IntVector.zero(species);
            int iterations = 0;
            for (; (i <= vectorLimit) && (iterations < flushInterval);
                 i += laneCount, iterations++) {

                IntVector px = IntVector.fromArray(species, pixels, i);

                IntVector r = px.lanewise(VectorOperators.LSHR, 16).and(vMask);
                IntVector g = px.lanewise(VectorOperators.LSHR,  8).and(vMask);
                IntVector b = px.and(vMask);

                IntVector dr = r.sub(vRefR);
                IntVector dg = g.sub(vRefG);
                IntVector db = b.sub(vRefB);

                acc = acc.add(dr.mul(dr).add(dg.mul(dg)).add(db.mul(db)));
            }
            sum += acc.reduceLanes(VectorOperators.ADD);
        }

        // Scalar tail.
        final int refR = (color >>> 16) & 0xFF;
        final int refG = (color >>>  8) & 0xFF;
        final int refB =  color         & 0xFF;
        for (; i < end; i++) {
            int px = pixels[i];
            int dr = ((px >>> 16) & 0xFF) - refR;
            int dg = ((px >>>  8) & 0xFF) - refG;
            int db = ( px         & 0xFF) - refB;
            sum += dr * dr + dg * dg + db * db;
        }

        return sum;
    }

    /**
     * Clamp a value to the RGB range [0, 255].
     *
     * @param value the value to clamp
     * @return the clamped value
     */
    public static int clampRgbValue(final int value) {
        return Math.clamp(value, 0, 255);
    }

    /**
     * Convert a 24-bit RGB color to sixel color space.
     *
     * @param rgb the 24-bit RGB color
     * @return the sixel color (components 0-100)
     */
    public static int toSixelColor(final int rgb) {
        int r = getRed(rgb) * 100 / 255;
        int g = getGreen(rgb) * 100 / 255;
        int b = getBlue(rgb) * 100 / 255;
        return combineRgb(r, g, b);
    }

    /**
     * Convert a 24-bit RGB color to sixel color space, with optional
     * black/white mapping for colors close to those extremes.
     *
     * @param rgb the 24-bit RGB color
     * @param checkBlackWhite if true, map near-black to black and near-white to white
     * @return the sixel color
     */
    public static int toSixelColor(final int rgb, final boolean checkBlackWhite) {
        int r = getRed(rgb) * 100 / 255;
        int g = getGreen(rgb) * 100 / 255;
        int b = getBlue(rgb) * 100 / 255;

        if (!checkBlackWhite) {
            return combineRgb(r, g, b);
        }

        // Black threshold: sum of squared components must be < 10.
        // This catches very dark colors and maps them to pure black.
        final int blackThreshold = 10;
        if ((r * r + g * g + b * b) < blackThreshold) {
            return SIXEL_BLACK;
        }

        // White threshold is 0, effectively disabling white mapping.
        // This is intentional - the original code found that white mapping
        // caused more image quality issues than it solved.

        return combineRgb(r, g, b);
    }
}
