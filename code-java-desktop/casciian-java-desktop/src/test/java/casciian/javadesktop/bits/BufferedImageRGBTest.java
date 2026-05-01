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
 */
package casciian.javadesktop.bits;

import casciian.bits.ImageRGB;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Black-box tests for {@link BufferedImageRGB}.
 */
class BufferedImageRGBTest {

    @Test
    void wrapsBufferedImageAndPreservesPixels() {
        BufferedImage src = sample2x2();

        BufferedImageRGB image = new BufferedImageRGB(src);

        assertThat(image.getWidth()).isEqualTo(2);
        assertThat(image.getHeight()).isEqualTo(2);
        assertThat(image.getRGB(0, 0) & 0x00FFFFFF).isEqualTo(0xFF0000);
        assertThat(image.getRGB(1, 0) & 0x00FFFFFF).isEqualTo(0x00FF00);
        assertThat(image.getRGB(0, 1) & 0x00FFFFFF).isEqualTo(0x0000FF);
        assertThat(image.getRGB(1, 1) & 0x00FFFFFF).isEqualTo(0xFFFFFF);
    }

    @Test
    void isAnImageRGBSoExistingApisKeepWorking() {
        ImageRGB image = new BufferedImageRGB(sample2x2());

        // Inherited mutating operations must work transparently.
        ImageRGB sub = image.getSubimage(0, 0, 1, 1);
        assertThat(sub.getRGB(0, 0) & 0x00FFFFFF).isEqualTo(0xFF0000);

        image.fillRect(0, 0, 2, 2, 0xFF123456);
        assertThat(image.getRGB(0, 0)).isEqualTo(0xFF123456);
        assertThat(image.getRGB(1, 1)).isEqualTo(0xFF123456);
    }

    @Test
    void getBufferedImageReturnsOriginalWhenUnmodified() {
        BufferedImage src = sample2x2();
        BufferedImageRGB image = new BufferedImageRGB(src);

        // Fast path: no mutation, the wrapper returns the very AWT image
        // it was constructed with so AWT-aware consumers can use it
        // without an extra conversion.
        assertThat(image.getBufferedImage()).isSameAs(src);
    }

    @Test
    void getBufferedImageReflectsMutations() {
        BufferedImageRGB image = new BufferedImageRGB(sample2x2());

        image.fillRect(0, 0, 2, 2, 0xFFABCDEF);

        BufferedImage out = image.getBufferedImage();
        assertThat(out.getWidth()).isEqualTo(2);
        assertThat(out.getHeight()).isEqualTo(2);
        // Each pixel must mirror the mutated state.
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                assertThat(out.getRGB(x, y)).isEqualTo(0xFFABCDEF);
            }
        }
    }

    @Test
    void rejectsNullBufferedImage() {
        assertThatThrownBy(() -> new BufferedImageRGB(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * A 2x2 image with four distinct corners.
     */
    private static BufferedImage sample2x2() {
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, 0xFFFF0000);
        img.setRGB(1, 0, 0xFF00FF00);
        img.setRGB(0, 1, 0xFF0000FF);
        img.setRGB(1, 1, 0xFFFFFFFF);
        return img;
    }
}
