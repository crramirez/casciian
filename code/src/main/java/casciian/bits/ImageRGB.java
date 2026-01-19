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
 * A simple 2D RGB array.
 */
public class ImageRGB {

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
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                this.rgb[x][y] = image.rgb[x][y];
            }
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

        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                rgbArray[offset + row * scansize + col] = this.rgb[startX + col][startY + row];
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

        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                this.rgb[startX + col][startY + row] = rgbArray[offset + row * scanSize + col];
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

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int underRGB = rgb[x][y];
                int overRGB = image.rgb[x][y];
                int underRed = (underRGB >>> 16) & 0xFF;
                int underGreen = (underRGB >>> 8) & 0xFF;
                int underBlue = underRGB & 0xFF;
                int overRed = (overRGB >>> 16) & 0xFF;
                int overGreen = (overRGB >>> 8) & 0xFF;
                int overBlue = overRGB & 0xFF;
                int red = (int) ((underRed * (1.0 - alpha)) + (overRed * alpha));
                int green = (int) ((underGreen * (1.0 - alpha)) + (overGreen * alpha));
                int blue = (int) ((underBlue * (1.0 - alpha)) + (overBlue * alpha));
                int newRgb = (red << 16) | (green << 8) | blue;
                this.rgb[x][y] = newRgb;
            }
        }
    }

    /**
     * Extracts a subimage of the specified dimensions from the current image.
     *
     * @param x the x-coordinate of the upper-left corner of the subimage
     * @param y the y-coordinate of the upper-left corner of the subimage
     * @param w the width of the subimage in pixels
     * @param h the height of the subimage in pixels
     * @return a new ImageRGB containing the specified subimage
     * @throws IllegalArgumentException if the specified dimensions are invalid or extend beyond the bounds of the image
     */
    public ImageRGB getSubimage(int x, int y, int w, int h) {
        if (x < 0 || y < 0 || w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Invalid subimage dimensions");
        }
        ImageRGB subimage = new ImageRGB(w, h);
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                if (col < width && row < height) {
                    subimage.rgb[col][row] = this.rgb[x + col][y + row];
                }
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
        for (int row = startY; row < startY + height; row++) {
            for (int col = startX; col < startX + width; col++) {
                this.rgb[col][row] = color;
            }
        }
    }
}
