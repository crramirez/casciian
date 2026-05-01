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
 * A 2D RGB image abstraction.
 *
 * <p>Implementations expose pixel data as 24-bit (or 32-bit ARGB) integer
 * values via {@link #getRGB(int, int)} / {@link #setRGB(int, int, int)}
 * and bulk region accessors. The default implementation is
 * {@link ByteArrayImageRGB}, which stores the data in a row-major
 * {@code int[][]} array; alternative implementations may wrap a native
 * image type (for example a Java AWT {@code BufferedImage}) so that
 * decoders that already produce that type can avoid an intermediate copy.
 *
 * <p>Methods that take another {@link ImageRGB} (for example
 * {@link #alphaBlendOver(ImageRGB, double)}) may detect that the argument
 * shares the same underlying representation and use a fast path; otherwise
 * they fall back to the public {@code getRGB} API.
 */
public interface ImageRGB {

    /**
     * Get an RGB value.
     *
     * @param x the column location
     * @param y the row location
     * @return the RGB value
     */
    int getRGB(int x, int y);

    /**
     * Set an RGB value.
     *
     * @param x   the column location
     * @param y   the row location
     * @param rgb the new RGB value
     */
    void setRGB(int x, int y, int rgb);

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
    int[] getRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize);

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
    void setRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scanSize);

    /**
     * Alpha-blend another image over this one.
     *
     * @param image the other image
     * @param alpha a number between 0 and 1
     */
    void alphaBlendOver(ImageRGB image, double alpha);

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
    ImageRGB getSubimage(int x, int y, int w, int h);

    /**
     * Retrieves the width of the image in pixels.
     *
     * @return the width of the image as an integer
     */
    int getWidth();

    /**
     * Retrieves the height of the image in pixels.
     *
     * @return the height of the image as an integer
     */
    int getHeight();

    /**
     * Fills a rectangular area of the image with a specified color.
     *
     * @param startX the starting x-coordinate of the fill area
     * @param startY the starting y-coordinate of the fill area
     * @param width the width of the fill area
     * @param height the height of the fill area
     * @param color the color to fill the area with
     */
    void fillRect(int startX, int startY, int width, int height, int color);

    /**
     * Scales this image to the specified dimensions.
     *
     * <p>The default contract is to throw {@link UnsupportedOperationException}
     * for implementations that do not provide a scaling routine. The default
     * implementation ({@link ByteArrayImageRGB}) uses Mitchell–Netravali
     * bicubic interpolation.
     *
     * @param newWidth  the target width in pixels
     * @param newHeight the target height in pixels
     * @return a new ImageRGB with the specified dimensions
     * @throws IllegalArgumentException      if dimensions are not positive
     * @throws UnsupportedOperationException if the implementation does not support scaling
     */
    default ImageRGB scale(int newWidth, int newHeight) {
        throw new UnsupportedOperationException(
            "scale is not supported by " + getClass().getName());
    }

    /**
     * Rotates the image 90 degrees clockwise or counter-clockwise.
     *
     * @param clockwise number of turns clockwise
     * @return a new ImageRGB containing the rotated image
     */
    ImageRGB rotate(int clockwise);

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
    ImageRGB resizeCanvas(int newWidth, int newHeight, int backgroundColor);
}
