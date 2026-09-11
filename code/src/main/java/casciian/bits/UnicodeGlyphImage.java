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
     * Scratch buffer reused across stddev computations to avoid per-call
     * allocations in hot rendering paths. Holds the whole image in
     * row-major order.
     */
    private int[] pixels = new int[0];

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
        int pixelCount = width * height;

        // Read the whole image once, in bulk, into a contiguous buffer.
        // Per-pixel getRGB() calls cost more than the distance kernels that
        // consume the data, and this method needs the same pixels three
        // times.
        if (pixels.length < pixelCount) {
            pixels = new int[pixelCount];
        }
        if (pixelCount > 0) {
            image.getRGB(0, 0, width, height, pixels, 0, width);
        }

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
        bestStdDev = computeLeftRightStdDev(foreColorRGB, backColorRGB,
            width, height);

        // Top half
        int newForeColorRGB = ImageUtils.rgbAverage(image, 0, 0, width,
            height / 2);
        int newBackColorRGB = ImageUtils.rgbAverage(image, 0, height / 2,
            width, height - height / 2);
        double newRgbStdDev = computeTopBottomStdDev(newForeColorRGB,
            newBackColorRGB, width, height);
        if (newRgbStdDev < bestStdDev) {
            ch = 0x2580;
            foreColorRGB = newForeColorRGB;
            backColorRGB = newBackColorRGB;
            bestStdDev = newRgbStdDev;
        }

        // Full block
        int newColorRGB = ImageUtils.rgbAverage(image, 0, 0, width, height);
        newRgbStdDev = computeFullBlockStdDev(newColorRGB, width, height);
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
     * Compute the standard deviation between the image and a left/right
     * two-color split.
     *
     * @param foreColor the average color of the left region
     * @param backColor the average color of the right region
     * @param width the image width
     * @param height the image height
     * @return the standard deviation
     */
    private double computeLeftRightStdDev(final int foreColor,
        final int backColor, final int width, final int height) {

        int pixelCount = width * height;
        if (pixelCount == 0) {
            return 0;
        }

        // Each row contributes a left run and a right run of the flattened
        // buffer, so the kernel can run over them in place without copying
        // pixels into separate region buffers.
        int leftWidth = width / 2;
        int rightWidth = width - leftWidth;
        long totalDiffSquared = 0;
        for (int y = 0; y < height; y++) {
            int rowStart = y * width;
            totalDiffSquared += Rgb.distanceSquaredSum(pixels, rowStart,
                leftWidth, foreColor);
            totalDiffSquared += Rgb.distanceSquaredSum(pixels,
                rowStart + leftWidth, rightWidth, backColor);
        }
        return Math.sqrt((double) totalDiffSquared / pixelCount);
    }

    /**
     * Compute the standard deviation between the image and a top/bottom
     * two-color split.
     *
     * @param foreColor the average color of the top region
     * @param backColor the average color of the bottom region
     * @param width the image width
     * @param height the image height
     * @return the standard deviation
     */
    private double computeTopBottomStdDev(final int foreColor,
        final int backColor, final int width, final int height) {

        int pixelCount = width * height;
        if (pixelCount == 0) {
            return 0;
        }

        // Both regions are contiguous in the flattened buffer.
        int splitFore = width * (height / 2);
        long totalDiffSquared =
            Rgb.distanceSquaredSum(pixels, 0, splitFore, foreColor)
            + Rgb.distanceSquaredSum(pixels, splitFore,
                pixelCount - splitFore, backColor);
        return Math.sqrt((double) totalDiffSquared / pixelCount);
    }

    /**
     * Compute the standard deviation between the image and a single color.
     *
     * @param color the uniform color
     * @param width the image width
     * @param height the image height
     * @return the standard deviation
     */
    private double computeFullBlockStdDev(final int color, final int width,
        final int height) {

        int pixelCount = width * height;
        if (pixelCount == 0) {
            return 0;
        }

        long totalDiffSquared = Rgb.distanceSquaredSum(pixels, 0, pixelCount,
            color);
        return Math.sqrt((double) totalDiffSquared / pixelCount);
    }
}
