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
package casciian.javadesktop.image;

import casciian.bits.ImageRGB;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * {@link ImageRGB} implementation backed by a {@link BufferedImage} from the
 * {@code java.desktop} module.
 *
 * <p>This implementation leverages native AWT capabilities for performance:
 * <ul>
 *   <li>Pixel data is stored in a {@link BufferedImage#TYPE_INT_ARGB}
 *       {@code BufferedImage}, whose underlying {@code int[]} raster matches
 *       the 0xAARRGGBB layout used by Casciian's {@link ImageRGB} API. As a
 *       result, the bulk {@code getRGB}/{@code setRGB} array operations
 *       become straight memory copies inside AWT.</li>
 *   <li>{@link #fillRect(int, int, int, int, int) fillRect},
 *       {@link #alphaBlendOver(ImageRGB, double) alphaBlendOver},
 *       {@link #scale(int, int) scale},
 *       {@link #rotate(int) rotate},
 *       {@link #getSubimage(int, int, int, int) getSubimage} and
 *       {@link #resizeCanvas(int, int, int) resizeCanvas} delegate to
 *       {@link Graphics2D} operations such as {@link Graphics2D#fillRect},
 *       {@link Graphics2D#drawImage} with {@link AlphaComposite} and
 *       {@link AffineTransform}, instead of manually iterating over every
 *       pixel.</li>
 * </ul>
 *
 * <p>This class is intended for applications running on a JVM with full
 * {@code java.desktop} support; pure-JVM/GraalVM-native scenarios should use
 * {@link casciian.bits.ArrayImageRGB} from the core Casciian library.
 */
public class BufferedImageRGB implements ImageRGB {

    /**
     * The backing {@link BufferedImage}. Always non-{@code null}.
     */
    private final BufferedImage image;

    /**
     * The width of this image, in pixels.
     */
    private final int width;

    /**
     * The height of this image, in pixels.
     */
    private final int height;

    /**
     * Creates a new opaque-black {@code TYPE_INT_ARGB} image of the given
     * dimensions.
     *
     * @param width  the number of pixels in width (must be positive)
     * @param height the number of pixels in height (must be positive)
     * @throws IllegalArgumentException if {@code width} or {@code height} is
     *                                  not positive
     */
    public BufferedImageRGB(final int width, final int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Dimensions must be positive");
        }
        this.width = width;
        this.height = height;
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Wraps the supplied {@link BufferedImage} in an {@code ImageRGB} view.
     *
     * <p>The supplied image is used as-is (no defensive copy): subsequent
     * mutations through this {@code BufferedImageRGB} are reflected on the
     * underlying {@code BufferedImage}, and vice versa. Callers that need
     * isolation should pass a copy.
     *
     * @param image the BufferedImage to wrap (must be non-{@code null})
     * @throws IllegalArgumentException if {@code image} is {@code null}
     */
    public BufferedImageRGB(final BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("image must not be null");
        }
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
    }

    /**
     * Copy-style constructor that creates a new {@code BufferedImageRGB}
     * containing the same pixel data as the supplied {@link ImageRGB}.
     *
     * <p>If {@code source} is itself a {@code BufferedImageRGB}, the data is
     * transferred via {@link Graphics2D#drawImage} with {@link AlphaComposite#Src}
     * (single AWT call). Otherwise the copy goes through the public bulk
     * {@code getRGB} API in a single allocation.
     *
     * @param source the image whose pixels will be copied (must be non-{@code null})
     * @throws IllegalArgumentException if {@code source} is {@code null}
     */
    public BufferedImageRGB(final ImageRGB source) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        this.width = source.getWidth();
        this.height = source.getHeight();
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        if (source instanceof BufferedImageRGB other) {
            // Use Graphics2D.drawImage with Src composite to copy the pixels
            // verbatim (preserving alpha) rather than alpha-blending on top.
            Graphics2D g = image.createGraphics();
            try {
                g.setComposite(AlphaComposite.Src);
                g.drawImage(other.image, 0, 0, null);
            } finally {
                g.dispose();
            }
        } else {
            int[] pixels = source.getRGB(0, 0, width, height, null, 0, width);
            image.setRGB(0, 0, width, height, pixels, 0, width);
        }
    }

    /**
     * Returns the backing {@link BufferedImage}.
     *
     * <p>The returned reference is the live image: mutating it changes the
     * pixels seen by this {@code BufferedImageRGB}.
     *
     * @return the underlying {@code BufferedImage}
     */
    public BufferedImage getBufferedImage() {
        return image;
    }

    @Override
    public int getRGB(final int x, final int y) {
        return image.getRGB(x, y);
    }

    @Override
    public void setRGB(final int x, final int y, final int rgb) {
        image.setRGB(x, y, rgb);
    }

    @Override
    public int[] getRGB(final int startX, final int startY, final int w, final int h,
                        final int[] rgbArray, final int offset, final int scansize) {
        if (startX < 0 || startY < 0 || w <= 0 || h <= 0
                || startX + w > width || startY + h > height) {
            throw new IllegalArgumentException("Invalid region dimensions");
        }
        // BufferedImage.getRGB is internally backed by an int[] raster for
        // TYPE_INT_ARGB and performs a single bulk copy.
        return image.getRGB(startX, startY, w, h, rgbArray, offset, scansize);
    }

    @Override
    public void setRGB(final int startX, final int startY, final int w, final int h,
                       final int[] rgbArray, final int offset, final int scanSize) {
        if (startX < 0 || startY < 0 || w <= 0 || h <= 0
                || startX + w > width || startY + h > height) {
            throw new IllegalArgumentException("Invalid region dimensions");
        }
        image.setRGB(startX, startY, w, h, rgbArray, offset, scanSize);
    }

    @Override
    public void alphaBlendOver(final ImageRGB other, final double alpha) {
        if (other == null) {
            throw new IllegalArgumentException("Image to alpha-blend over cannot be null");
        }
        if (other.getWidth() != width || other.getHeight() != height) {
            throw new IllegalArgumentException("Image dimensions must match for alpha blending");
        }

        // Use AWT's accelerated SrcOver composite. This delegates to native
        // 2D rendering (often hardware-accelerated) and avoids per-pixel
        // arithmetic in Java.
        BufferedImage overlay;
        if (other instanceof BufferedImageRGB b) {
            overlay = b.image;
        } else {
            // Wrap the foreign ImageRGB into a temporary BufferedImage so
            // Graphics2D can blit it in a single drawImage call.
            overlay = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int[] pixels = other.getRGB(0, 0, width, height, null, 0, width);
            overlay.setRGB(0, 0, width, height, pixels, 0, width);
        }

        Graphics2D g = image.createGraphics();
        try {
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, (float) alpha));
            g.drawImage(overlay, 0, 0, null);
        } finally {
            g.dispose();
        }
    }

    @Override
    public ImageRGB getSubimage(final int x, final int y, final int w, final int h) {
        if (x < 0 || y < 0 || w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Invalid subimage dimensions");
        }

        // Mirror ArrayImageRGB#getSubimage semantics: clamp to source bounds
        // and leave any out-of-bounds region zero-initialised.
        int copyWidth = Math.min(w, width - x);
        int copyHeight = Math.min(h, height - y);

        BufferedImageRGB result = new BufferedImageRGB(w, h);
        if (copyWidth > 0 && copyHeight > 0) {
            // BufferedImage.getSubimage returns a view that shares the data
            // buffer; drawing it with the Src composite produces an
            // independent deep copy via a single AWT call.
            BufferedImage view = image.getSubimage(x, y, copyWidth, copyHeight);
            Graphics2D g = result.image.createGraphics();
            try {
                g.setComposite(AlphaComposite.Src);
                g.drawImage(view, 0, 0, null);
            } finally {
                g.dispose();
            }
        }
        return result;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void fillRect(final int startX, final int startY,
                         final int rectWidth, final int rectHeight,
                         final int color) {
        if (startX < 0 || startY < 0 || rectWidth <= 0 || rectHeight <= 0
                || startX + rectWidth > width || startY + rectHeight > height) {
            throw new IllegalArgumentException("Invalid fill rectangle dimensions");
        }

        Graphics2D g = image.createGraphics();
        try {
            // Use Src composite so the supplied ARGB value is written
            // verbatim, matching the contract of
            // {@link casciian.bits.ArrayImageRGB#fillRect} which simply
            // stores the integer in the underlying buffer.
            g.setComposite(AlphaComposite.Src);
            g.setColor(new Color(color, true));
            g.fillRect(startX, startY, rectWidth, rectHeight);
        } finally {
            g.dispose();
        }
    }

    @Override
    public ImageRGB scale(final int newWidth, final int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) {
            throw new IllegalArgumentException("New dimensions must be positive");
        }

        BufferedImageRGB result = new BufferedImageRGB(newWidth, newHeight);
        Graphics2D g = result.image.createGraphics();
        try {
            // Bicubic interpolation matches the Mitchell-Netravali default
            // used by ArrayImageRGB#scale and is implemented natively by
            // Java 2D for good performance.
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.setComposite(AlphaComposite.Src);
            g.drawImage(image, 0, 0, newWidth, newHeight, null);
        } finally {
            g.dispose();
        }
        return result;
    }

    @Override
    public ImageRGB rotate(final int clockwise) {
        // Normalise to [0, 3] so negative values and values >= 4 work like
        // ArrayImageRGB#rotate.
        int turns = ((clockwise % 4) + 4) % 4;

        if (turns == 0) {
            return new BufferedImageRGB(this);
        }

        // Swap dimensions for 90-degree and 270-degree rotations.
        int newWidth = (turns == 2) ? width : height;
        int newHeight = (turns == 2) ? height : width;

        BufferedImageRGB result = new BufferedImageRGB(newWidth, newHeight);

        // Build the affine transform that takes the source image's
        // bounding box to the rotated bounding box and apply it through
        // Graphics2D.drawImage so AWT handles the pixel relocation
        // natively.
        AffineTransform transform = new AffineTransform();
        switch (turns) {
            case 1 -> {
                // 90 degrees clockwise
                transform.translate(height, 0);
                transform.rotate(Math.PI / 2);
            }
            case 2 -> {
                // 180 degrees
                transform.translate(width, height);
                transform.rotate(Math.PI);
            }
            case 3 -> {
                // 270 degrees clockwise (equivalent to 90 counter-clockwise)
                transform.translate(0, width);
                transform.rotate(-Math.PI / 2);
            }
            default -> {
                // Unreachable: turns has been normalised to [0, 3] above.
            }
        }

        Graphics2D g = result.image.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            g.drawImage(image, transform, null);
        } finally {
            g.dispose();
        }
        return result;
    }

    @Override
    public ImageRGB resizeCanvas(final int newWidth, final int newHeight,
                                 final int backgroundColor) {
        if (newWidth <= 0 || newHeight <= 0) {
            throw new IllegalArgumentException("New dimensions must be positive");
        }

        BufferedImageRGB result = new BufferedImageRGB(newWidth, newHeight);
        Graphics2D g = result.image.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            // Paint the entire new canvas with the background colour first,
            // then copy the existing pixels (cropped if necessary) on top.
            g.setColor(new Color(backgroundColor, true));
            g.fillRect(0, 0, newWidth, newHeight);

            int copyWidth = Math.min(width, newWidth);
            int copyHeight = Math.min(height, newHeight);
            if (copyWidth > 0 && copyHeight > 0) {
                BufferedImage view = (copyWidth == width && copyHeight == height)
                        ? image
                        : image.getSubimage(0, 0, copyWidth, copyHeight);
                g.drawImage(view, 0, 0, null);
            }
        } finally {
            g.dispose();
        }
        return result;
    }
}
