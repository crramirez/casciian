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

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * A simple 2D RGB array stored in row-major order for efficient
 * bulk copy operations via {@link System#arraycopy}.
 */
public class ImageRGB {

    /**
     * Threshold for using parallel streams. Only compute-bound operations
     * (like alpha blending) benefit from parallelization. Memory-bandwidth-bound
     * operations (arraycopy, Arrays.fill) already saturate memory bandwidth
     * on a single core, so parallelization only adds overhead.
     */
    private static final int PARALLEL_THRESHOLD = 10_000;

    /**
     * The 24-bit RGB data stored in row-major order: rgb[row][col].
     */
    private final int[][] rgb;

    /**
     * The width of this image.
     */
    private final int width;

    /**
     * The height of this image.
     */
    private final int height;

    /**
     * Public constructor.
     *
     * @param width  the number of pixels in width
     * @param height the number of pixels in height
     */
    public ImageRGB(final int width, final int height) {
        this.width = width;
        this.height = height;
        rgb = new int[height][width];
    }

    /**
     * Public constructor.
     *
     * @param image another ImageRGB that the RGB data will be
     *              copied from
     */
    public ImageRGB(final ImageRGB image) {
        width = image.width;
        height = image.height;
        rgb = new int[height][width];
        // Use System.arraycopy for efficient row-by-row copying
        for (int y = 0; y < height; y++) {
            System.arraycopy(image.rgb[y], 0, this.rgb[y], 0, width);
        }
    }

    /**
     * Get an RGB value.
     *
     * @param x the column location
     * @param y the row location
     * @return the RGB value
     */
    public int getRGB(final int x, final int y) {
        return rgb[y][x];
    }

    /**
     * Set an RGB value.
     *
     * @param x   the column location
     * @param y   the row location
     * @param rgb the new RGB value
     */
    public void setRGB(final int x, final int y, final int rgb) {
        this.rgb[y][x] = rgb;
    }

    /**
     * Retrieves the RGB values of a rectangular region of the image.
     * The RGB values are returned in an integer array, where each integer
     * represents a combination of alpha, red, green, and blue components.
     * If an existing array is provided, the values are stored in that array;
     * otherwise, a new array is created.
     *
     * @param startX   the starting X-coordinate of the region
     * @param startY   the starting Y-coordinate of the region
     * @param w        the width of the region
     * @param h        the height of the region
     * @param rgbArray an optional pre-existing array to store the RGB values,
     *                 or {@code null} to create a new array
     * @param offset   the starting index in the array at which to write RGB values
     * @param scansize the number of array entries per row of the rectangular region
     * @return an integer array containing the RGB values of the specified region
     * @throws IllegalArgumentException if the specified region dimensions are invalid
     *                                  or extend beyond the bounds of the image
     */
    public int [] getRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {
        if (startX < 0 || startY < 0 || w <= 0 || h <= 0 || startX + w > width || startY + h > height) {
            throw new IllegalArgumentException("Invalid region dimensions");
        }

        if (rgbArray == null) {
            rgbArray = new int[offset + h * scansize];
        }

        int[][] thisRgb = this.rgb;
        for (int row = 0; row < h; row++) {
            System.arraycopy(thisRgb[startY + row], startX, rgbArray, offset + row * scansize, w);
        }

        return rgbArray;
    }

    /**
     * Sets the RGB values for a rectangular region of the image. The method copies the RGB data
     * from a portion of the given array into the specified region of the image.
     *
     * @param startX   the starting X-coordinate of the region to be updated
     * @param startY   the starting Y-coordinate of the region to be updated
     * @param w        the width of the region
     * @param h        the height of the region
     * @param rgbArray an array containing the RGB values that will be set in the region
     * @param offset   the starting index in the array from which RGB values should be read
     * @param scanSize the number of array entries per row of the region
     * @throws IllegalArgumentException if the specified region dimensions are invalid or extend
     *                                  beyond the bounds of the image
     */
    public void setRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scanSize) {
        if (startX < 0 || startY < 0 || w <= 0 || h <= 0 || startX + w > width || startY + h > height) {
            throw new IllegalArgumentException("Invalid region dimensions");
        }

        int[][] thisRgb = this.rgb;
        for (int row = 0; row < h; row++) {
            System.arraycopy(rgbArray, offset + row * scanSize, thisRgb[startY + row], startX, w);
        }
    }

    /**
     * Alpha-blend another image over this one.
     *
     * @param image the other image
     * @param alpha a number between 0 and 1
     */
    public void alphaBlendOver(final ImageRGB image,
                               final double alpha) {

        if (image == null) {
            throw new IllegalArgumentException("Image to alpha-blend over cannot be null");
        }
        if (image.getWidth() != this.width || image.getHeight() != this.height) {
            throw new IllegalArgumentException("Image dimensions must match for alpha blending");
        }

        // Precompute alpha factor as a 0-256 integer to replace per-pixel
        // floating-point arithmetic with integer multiply + shift.
        final int a = (int) (alpha * 256);
        final int oneMinusA = 256 - a;

        // Use parallel streams for large images: alpha blending is
        // compute-bound (bit shifts + multiplications per pixel),
        // so multiple cores provide a genuine speedup.
        IntStream rowStream = IntStream.range(0, height);
        if ((long) width * height > PARALLEL_THRESHOLD) {
            //noinspection DataFlowIssue
            rowStream = rowStream.parallel();
        }

        rowStream.forEach(y -> {
            int[] thisRow = rgb[y];
            int[] overRow = image.rgb[y];
            for (int x = 0; x < width; x++) {
                int under = thisRow[x];
                int over = overRow[x];
                // Max intermediate per component: 255*256 = 65 280 (no int overflow)
                int red   = (((under >>> 16) & 0xFF) * oneMinusA + ((over >>> 16) & 0xFF) * a) >> 8;
                int green = (((under >>>  8) & 0xFF) * oneMinusA + ((over >>>  8) & 0xFF) * a) >> 8;
                int blue  = ((under          & 0xFF) * oneMinusA + ( over         & 0xFF) * a) >> 8;
                thisRow[x] = 0xFF000000 | (red << 16) | (green << 8) | blue;
            }
        });
    }

    /**
     * Extracts a subimage of the specified dimensions from the current image.
     *
     * @param x the x-coordinate of the upper-left corner of the subimage
     * @param y the y-coordinate of the upper-left corner of the subimage
     * @param w the width of the subimage in pixels
     * @param h the height of the subimage in pixels
     * @return a new ImageRGB containing the specified subimage
     * @throws IllegalArgumentException if the specified dimensions are invalid
     */
    public ImageRGB getSubimage(int x, int y, int w, int h) {
        if (x < 0 || y < 0 || w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Invalid subimage dimensions");
        }
        ImageRGB subimage = new ImageRGB(w, h);

        // Calculate the actual copy bounds to avoid checking in the loop
        int copyWidth = Math.min(w, width - x);
        int copyHeight = Math.min(h, height - y);

        if (copyWidth > 0 && copyHeight > 0) {
            // System.arraycopy is memory-bandwidth-bound and already
            // saturates the memory bus on a single core, so a simple
            // sequential loop is faster than parallel streams here.
            for (int row = 0; row < copyHeight; row++) {
                System.arraycopy(this.rgb[y + row], x, subimage.rgb[row], 0, copyWidth);
            }
        }
        return subimage;
    }

    /**
     * Retrieves the width of the image in pixels.
     *
     * @return the width of the image as an integer
     */
    public int getWidth() {
        return width;
    }

    /**
     * Retrieves the height of the image in pixels.
     *
     * @return the height of the image as an integer
     */
    public int getHeight() {
        return height;
    }

    /**
     * Fills a rectangular area of the image with a specified color.
     *
     * @param startX the starting x-coordinate of the fill area
     * @param startY the starting y-coordinate of the fill area
     * @param width the width of the fill area
     * @param height the height of the fill area
     * @param color the color to fill the area with
     */
    public void fillRect(int startX, int startY, int width, int height, int color) {
        if (startX < 0 || startY < 0 || startX + width > this.width || startY + height > this.height) {
            throw new IllegalArgumentException("Invalid fill rectangle dimensions");
        }

        // Arrays.fill is a JVM intrinsic (vectorized memset) that
        // saturates memory bandwidth on a single core. Parallel streams
        // would only add thread-management overhead here.
        for (int row = startY; row < startY + height; row++) {
            Arrays.fill(this.rgb[row], startX, startX + width, color);
        }
    }

    /**
     * Scales this image to the specified dimensions using Mitchell–Netravali
     * bicubic interpolation (B = C = 1/3) with a support radius of 2.
     * Uses separable convolution: a horizontal pass followed by a vertical
     * pass. Border pixels are handled by clamping coordinates at the edges.
     *
     * @param newWidth  the target width in pixels
     * @param newHeight the target height in pixels
     * @return a new ImageRGB with the specified dimensions
     * @throws IllegalArgumentException if dimensions are not positive
     */
    public ImageRGB scale(int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) {
            throw new IllegalArgumentException("New dimensions must be positive");
        }

        // Horizontal pass: width changes, height unchanged
        int[][] temp = new int[height][newWidth];
        ScaleImageUtils.resampleHorizontal(this.rgb, temp, width, newWidth, height);

        // Vertical pass: height changes, width unchanged
        ImageRGB result = new ImageRGB(newWidth, newHeight);
        ScaleImageUtils.resampleVertical(temp, result.rgb, height, newHeight, newWidth);

        return result;
    }

    /**
     * Rotates the image 90 degrees clockwise or counter-clockwise.
     *
     * @param clockwise number of turns clockwise
     * @return a new ImageRGB containing the rotated image
     */
    public ImageRGB rotate(final int clockwise) {
        // Normalize to [0, 3] so negative values and values >= 4 are handled.
        int turns = ((clockwise % 4) + 4) % 4;

        if (turns == 0) {
            return getSubimage(0, 0, width, height);
        }

        // Pixel shuffling is memory-bandwidth-bound (no arithmetic per
        // pixel beyond index computation), so a sequential loop avoids
        // parallel-stream overhead while still saturating the memory bus.
        if (turns == 1) {
            //noinspection SuspiciousNameCombination
            ImageRGB rotated = new ImageRGB(height, width);
            for (int y = 0; y < height; y++) {
                int[] srcRow = rgb[y];
                for (int x = 0; x < width; x++) {
                    rotated.rgb[x][height - 1 - y] = srcRow[x];
                }
            }
            return rotated;
        } else if (turns == 2) {
            ImageRGB rotated = new ImageRGB(width, height);
            for (int y = 0; y < height; y++) {
                int[] srcRow = rgb[y];
                for (int x = 0; x < width; x++) {
                    rotated.rgb[height - 1 - y][width - 1 - x] = srcRow[x];
                }
            }
            return rotated;
        } else {
            //noinspection SuspiciousNameCombination
            ImageRGB rotated = new ImageRGB(height, width);
            for (int y = 0; y < height; y++) {
                int[] srcRow = rgb[y];
                for (int x = 0; x < width; x++) {
                    rotated.rgb[width - 1 - x][y] = srcRow[x];
                }
            }
            return rotated;
        }
    }

    /**
     * Resizes the canvas to the specified dimensions. If the new dimensions are smaller
     * than the current image, it will crop the image. If the new dimensions are larger,
     * it will fill the extra space with the specified background color.
     *
     * @param newWidth the new width in pixels
     * @param newHeight the new height in pixels
     * @param backgroundColor the RGB color to use for filling extra space
     * @return a new ImageRGB with the specified dimensions
     */
    public ImageRGB resizeCanvas(int newWidth, int newHeight, int backgroundColor) {
        if (newWidth <= 0 || newHeight <= 0) {
            throw new IllegalArgumentException("New dimensions must be positive");
        }

        ImageRGB resized = new ImageRGB(newWidth, newHeight);
        int[][] oldRgb = this.rgb;
        int[][] newRgb = resized.rgb;

        int copyWidth = Math.min(this.width, newWidth);
        int copyHeight = Math.min(this.height, newHeight);

        // Both System.arraycopy and Arrays.fill are memory-bandwidth-bound
        // JVM intrinsics. A sequential loop avoids parallel-stream overhead
        // while still saturating the memory bus.

        // Copy existing rows, filling any extra width with background color
        for (int y = 0; y < copyHeight; y++) {
            System.arraycopy(oldRgb[y], 0, newRgb[y], 0, copyWidth);
            if (copyWidth < newWidth) {
                Arrays.fill(newRgb[y], copyWidth, newWidth, backgroundColor);
            }
        }

        // Fill entirely new rows with background color
        for (int y = copyHeight; y < newHeight; y++) {
            Arrays.fill(newRgb[y], 0, newWidth, backgroundColor);
        }

        return resized;
    }
}
