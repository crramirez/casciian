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
 * A simple 2D RGB array.
 */
public class ImageRGB {

    /**
     * Threshold for using parallel streams. Operations on images with
     * total pixels (width * height) greater than this will use parallel processing.
     * This is set to balance the overhead of thread creation against the benefits
     * of parallel execution.
     */
    private static final int PARALLEL_THRESHOLD = 10_000;

    /**
     * The 24-bit RGB data.
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
        rgb = new int[width][height];
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
        rgb = new int[width][height];
        // Use System.arraycopy for efficient column-by-column copying
        for (int x = 0; x < width; x++) {
            System.arraycopy(image.rgb[x], 0, this.rgb[x], 0, height);
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
        return rgb[x][y];
    }

    /**
     * Set an RGB value.
     *
     * @param x   the column location
     * @param y   the row location
     * @param rgb the new RGB value
     */
    public void setRGB(final int x, final int y, final int rgb) {
        this.rgb[x][y] = rgb;
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

        @SuppressWarnings("UnnecessaryLocalVariable") // Used a local variable for performance
        int[][] thisRgb = this.rgb;
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                rgbArray[offset + row * scansize + col] = thisRgb[startX + col][startY + row];
            }
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

        @SuppressWarnings("UnnecessaryLocalVariable") // Used a local variable for performance
        int[][] thisRgb = this.rgb;
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                thisRgb[startX + col][startY + row] = rgbArray[offset + row * scanSize + col];
            }
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

        // Use parallel streams for large images to improve performance
        IntStream xStream = IntStream.range(0, width);
        if ((long) width * height > PARALLEL_THRESHOLD) {
            //noinspection DataFlowIssue
            xStream = xStream.parallel();
        }
        
        xStream.forEach(x -> {
            for (int y = 0; y < height; y++) {
                int underRGB = rgb[x][y];
                int overRGB = image.rgb[x][y];
                int newRgb = ImageUtils.blendColors(alpha, underRGB, overRGB);
                this.rgb[x][y] = newRgb;
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
            // Use parallel streams for large subimages
            IntStream colStream = IntStream.range(0, copyWidth);
            if ((long) copyWidth * copyHeight > PARALLEL_THRESHOLD) {
                //noinspection DataFlowIssue
                colStream = colStream.parallel();
            }
            
            colStream.forEach(col ->
                System.arraycopy(this.rgb[x + col], y, subimage.rgb[col], 0, copyHeight)
            );
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
        
        // Use parallel streams for large fill operations
        IntStream colStream = IntStream.range(startX, startX + width);
        if ((long) width * height > PARALLEL_THRESHOLD) {
            //noinspection DataFlowIssue
            colStream = colStream.parallel();
        }
        
        colStream.forEach(col ->
            Arrays.fill(this.rgb[col], startY, startY + height, color)
        );
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
    @SuppressWarnings("UnnecessaryLocalVariable")
    public ImageRGB resizeCanvas(int newWidth, int newHeight, int backgroundColor) {
        if (newWidth <= 0 || newHeight <= 0) {
            throw new IllegalArgumentException("New dimensions must be positive");
        }

        ImageRGB resized = new ImageRGB(newWidth, newHeight);
        int oldWidth = this.width;
        int oldHeight = this.height;
        int[][] oldRgb = this.rgb;
        int[][] newRgb = resized.rgb;
        
        int copyWidth = Math.min(oldWidth, newWidth);
        int copyHeight = Math.min(oldHeight, newHeight);

        // Use parallel streams for large resize operations
        IntStream xStream = IntStream.range(0, newWidth);
        if ((long) newWidth * newHeight > PARALLEL_THRESHOLD) {
            //noinspection DataFlowIssue
            xStream = xStream.parallel();
        }

        xStream.forEach(x -> {
            if (x < copyWidth) {
                // Copy existing data
                System.arraycopy(oldRgb[x], 0, newRgb[x], 0, copyHeight);
                // Fill remaining height with background color
                if (copyHeight < newHeight) {
                    Arrays.fill(newRgb[x], copyHeight, newHeight, backgroundColor);
                }
            } else {
                // Fill entire column with background color
                Arrays.fill(newRgb[x], 0, newHeight, backgroundColor);
            }
        });

        return resized;
    }
}
