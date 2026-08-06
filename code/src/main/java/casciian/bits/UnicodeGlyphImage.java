/*
 * Casciian - Java Text User Interface
 *
 * Original work written 2013–2025 by Autumn Lamonte
 * and dedicated to the public domain via CC0.
 *
 * Modifications and maintenance:
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

/**
 * UnicodeGlyphImage constructs a single character from the Unicode
 * block-drawing elements ("Symbols For Legacy Computing") from a bitmap
 * image.
 */
public class UnicodeGlyphImage {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The bitmap image this glyph is supposed to represent.
     */
    private ImageRGB image = null;

    /**
     * Scratch buffers reused across stddev computations to avoid per-call
     * allocations in hot rendering paths.
     */
    private int[] forePixels = new int[0];
    private int[] backPixels = new int[0];
    private int[] fullPixels = new int[0];

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param cell a Cell to read image data from
     * @throws IllegalArgumentException if cell does not have an image
     */
    public UnicodeGlyphImage(final Cell cell) throws IllegalArgumentException {
        if (!cell.isImage()) {
            throw new IllegalArgumentException("cell does not have an image");
        }
        this.image = cell.getImage();
    }

    /**
     * Public constructor.
     *
     * @param image the bitmap image
     */
    public UnicodeGlyphImage(final ImageRGB image) {
        this.image = image;
    }

    // ------------------------------------------------------------------------
    // UnicodeGlyphImage ------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Create a single glyph using Unicode half-blocks that best represents
     * this entire image.
     *
     * @return a cell with character, foreground, and background color set
     */
    public Cell toHalfBlockGlyph() {
        Cell cell;

        int width = image.getWidth();
        int height = image.getHeight();

        // Try left half, top half, and full block, and whichever has the
        // least relative difference to the image is what we return.
        double bestStdDev;
        int ch = 0x258c;
        int foreColorRGB;
        int backColorRGB;

        // Left half.
        foreColorRGB = ImageUtils.rgbAverage(image, 0, 0, width / 2, height);
        backColorRGB = ImageUtils.rgbAverage(image, width / 2, 0,
            width - width / 2, height);
        bestStdDev = computeRegionStdDev(image, foreColorRGB,
            backColorRGB, true, width, height);

        // Top half
        int newForeColorRGB = ImageUtils.rgbAverage(image, 0, 0, width,
            height / 2);
        int newBackColorRGB = ImageUtils.rgbAverage(image, 0, height / 2,
            width, height - height / 2);
        double newRgbStdDev = computeRegionStdDev(image, newForeColorRGB,
            newBackColorRGB, false, width, height);
        if (newRgbStdDev < bestStdDev) {
            ch = 0x2580;
            foreColorRGB = newForeColorRGB;
            backColorRGB = newBackColorRGB;
            bestStdDev = newRgbStdDev;
        }

        // Full block
        int newColorRGB = ImageUtils.rgbAverage(image, 0, 0, width, height);
        newRgbStdDev = computeFullBlockStdDev(image, newColorRGB,
            width, height);
        if (newRgbStdDev < bestStdDev) {
            ch = 0x2588;
            foreColorRGB = newColorRGB;
            backColorRGB = newColorRGB;
        }

        cell = new Cell(ch);
        cell.setBackColorRGB(backColorRGB);
        cell.setForeColorRGB(foreColorRGB);

        return cell;
    }

    /**
     * Compute the standard deviation between the image and a two-region
     * split (left/right or top/bottom).
     *
     * @param image the source image
     * @param foreColor the average color for the foreground region
     * @param backColor the average color for the background region
     * @param leftRight if true, split left/right; if false, split top/bottom
     * @param width the image width
     * @param height the image height
     * @return the standard deviation
     */
    private double computeRegionStdDev(final ImageRGB image,
        final int foreColor, final int backColor,
        final boolean leftRight, final int width, final int height) {

        int pixelCount = width * height;
        if (pixelCount == 0) {
            return 0;
        }

        // Separate the pixels into two regions so we can call the vectorized
        // distanceSquaredSum on each contiguous array.
        int splitFore = leftRight ? (width / 2) * height : width * (height / 2);
        ensureRegionBufferCapacity(splitFore, pixelCount - splitFore);
        int fi = 0, bi = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean isFore = leftRight ? (x < width / 2) : (y < height / 2);
                int pixel = image.getRGB(x, y);
                if (isFore) {
                    forePixels[fi++] = pixel;
                } else {
                    backPixels[bi++] = pixel;
                }
            }
        }

        long totalDiffSquared = Rgb.distanceSquaredSum(forePixels, fi, foreColor)
                              + Rgb.distanceSquaredSum(backPixels, bi, backColor);
        return Math.sqrt((double) totalDiffSquared / pixelCount);
    }

    /**
     * Compute the standard deviation between the image and a single color.
     *
     * @param image the source image
     * @param color the uniform color
     * @param width the image width
     * @param height the image height
     * @return the standard deviation
     */
    private double computeFullBlockStdDev(final ImageRGB image,
        final int color, final int width, final int height) {

        int pixelCount = width * height;
        if (pixelCount == 0) {
            return 0;
        }

        // Flatten the image into a contiguous array for SIMD processing.
        ensureFullBufferCapacity(pixelCount);
        int idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                fullPixels[idx++] = image.getRGB(x, y);
            }
        }

        long totalDiffSquared = Rgb.distanceSquaredSum(fullPixels, pixelCount, color);
        return Math.sqrt((double) totalDiffSquared / pixelCount);
    }

    private void ensureRegionBufferCapacity(final int foreSize,
        final int backSize) {

        if (forePixels.length < foreSize) {
            forePixels = new int[foreSize];
        }
        if (backPixels.length < backSize) {
            backPixels = new int[backSize];
        }
    }

    private void ensureFullBufferCapacity(final int size) {
        if (fullPixels.length < size) {
            fullPixels = new int[size];
        }
    }
}
