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
package casciian.javadesktop.bits;

import casciian.bits.ImageRGB;

import java.awt.image.BufferedImage;

/**
 * An {@link ImageRGB} implementation backed by a {@link BufferedImage}
 * from the {@code java.desktop} module.
 *
 * <p>This class lets Casciian image decoders that natively produce a
 * {@link BufferedImage} (for example {@link javax.imageio.ImageIO})
 * expose their result without losing the underlying AWT image. An
 * AWT-aware consumer (such as a future {@code AWTImageEncoder}) can
 * downcast an {@link ImageRGB} to {@code BufferedImageRGB} and obtain
 * the original {@link BufferedImage} via {@link #getBufferedImage()}
 * to avoid an extra round-trip conversion.</p>
 *
 * <p>Like its parent {@link ImageRGB}, this class stores an
 * {@code 0xAARRGGBB} value per pixel. The constructor copies the AWT
 * image's pixels into the parent's row-major storage so that all the
 * inherited operations ({@link #alphaBlendOver}, {@link #getSubimage},
 * {@link #scale}, {@link #fillRect}, {@link #rotate},
 * {@link #resizeCanvas}, etc.) work unchanged. Modifications made
 * through those methods are reflected the next time
 * {@link #getBufferedImage()} is called.</p>
 *
 * <p>This class is part of the optional Casciian Java Desktop add-on
 * and therefore is only available on JVMs that ship the
 * {@code java.desktop} module.</p>
 */
public class BufferedImageRGB extends ImageRGB {

    /**
     * The {@link BufferedImage} this instance was constructed from.
     * It is kept so AWT-aware consumers can access the original AWT
     * image. It is <em>not</em> mutated by inherited {@link ImageRGB}
     * operations: those operate on the parent's row-major storage and
     * {@link #getBufferedImage()} rebuilds an AWT image from that
     * storage when its pixels diverge from {@code source}.
     */
    private final BufferedImage source;

    /**
     * Public constructor.
     *
     * @param image the {@link BufferedImage} to wrap; must not be
     *              {@code null}
     * @throws IllegalArgumentException if {@code image} is {@code null}
     */
    @SuppressWarnings("this-escape")
    public BufferedImageRGB(final BufferedImage image) {
        super(checkNotNull(image).getWidth(), image.getHeight());
        this.source = image;

        final int width = image.getWidth();
        final int height = image.getHeight();
        // Pull all pixels into a single int[] in 0xAARRGGBB layout and
        // bulk-copy them into the parent's row-major storage. This is
        // more efficient than per-pixel BufferedImage#getRGB calls.
        final int[] pixels = image.getRGB(0, 0, width, height,
                null, 0, width);
        setRGB(0, 0, width, height, pixels, 0, width);
    }

    /**
     * Returns a {@link BufferedImage} view of this image.
     *
     * <p>If the current pixels still match those of the
     * {@link BufferedImage} this instance was constructed with (i.e.
     * no inherited mutating operation has been invoked), the original
     * AWT image is returned directly to avoid copying. Otherwise a
     * fresh {@link BufferedImage} of type
     * {@link BufferedImage#TYPE_INT_ARGB} is built from the current
     * row-major storage.</p>
     *
     * @return a {@link BufferedImage} that reflects this image's
     *         current pixels
     */
    public BufferedImage getBufferedImage() {
        final int width = getWidth();
        final int height = getHeight();
        final int[] pixels = getRGB(0, 0, width, height, null, 0, width);

        // Fast path: if the source AWT image still has the same pixels
        // the parent storage holds, return it unchanged so AWT-aware
        // consumers can use it without an extra conversion.
        if (sourceMatches(pixels, width, height)) {
            return source;
        }

        final BufferedImage refreshed = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        refreshed.setRGB(0, 0, width, height, pixels, 0, width);
        return refreshed;
    }

    /**
     * Whether the original {@link BufferedImage} still matches the
     * current pixels held by the parent row-major storage.
     */
    private boolean sourceMatches(final int[] pixels,
                                  final int width, final int height) {
        if (source.getWidth() != width || source.getHeight() != height) {
            return false;
        }
        final int[] sourcePixels = source.getRGB(0, 0, width, height,
                null, 0, width);
        if (sourcePixels.length != pixels.length) {
            return false;
        }
        for (int i = 0; i < pixels.length; i++) {
            if (sourcePixels[i] != pixels[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Helper used in the constructor to validate the {@link BufferedImage}
     * argument before {@code super(...)} is invoked.
     */
    private static BufferedImage checkNotNull(final BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException(
                    "BufferedImage must not be null");
        }
        return image;
    }
}
