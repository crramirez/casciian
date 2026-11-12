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
package casciian.javadesktop.encoders;

import casciian.bits.ArrayImageRGB;
import casciian.bits.ImageRGB;
import casciian.image.encoders.ImageEncoderRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class ImageIOAdaptiveEncoderTest {

    private static final byte[] PNG_SIGNATURE = {
        (byte) 0x89, 'P', 'N', 'G'
    };

    private static final byte[] JPEG_SIGNATURE = {
        (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
    };

    private final ImageIOAdaptiveEncoder encoder =
        new ImageIOAdaptiveEncoder();

    @AfterEach
    void tearDown() {
        ImageEncoderRegistry.getInstance().clear();
    }

    @Test
    void photoLikeImageIsEncodedAsJpeg() throws IOException {
        byte[] data = encoder.encode(photo(1280, 16, 0xFF), false);

        assertThat(data).startsWith(JPEG_SIGNATURE);
        BufferedImage decoded = decode(data);
        assertThat(decoded.getWidth()).isEqualTo(1280);
        assertThat(decoded.getHeight()).isEqualTo(16);
    }

    @Test
    void textLikeImageIsEncodedAsPng() throws IOException {
        ImageRGB text = text(1280, 16);

        byte[] data = encoder.encode(text, false);

        assertThat(data).startsWith(PNG_SIGNATURE);
        assertThat(decode(data).getRGB(5, 5)).isEqualTo(text.getRGB(5, 5));
    }

    @Test
    void smoothGradientWithManyColorsIsEncodedAsPng() throws IOException {
        ImageRGB gradient = new ArrayImageRGB(1024, 16);
        for (int x = 0; x < 1024; x++) {
            gradient.fillRect(x, 0, 1, 16, 0xFF000000 | (x << 6));
        }

        assertThat(encoder.encode(gradient, false)).startsWith(PNG_SIGNATURE);
    }

    @Test
    void tinyPhotoIsEncodedAsPng() throws IOException {
        assertThat(encoder.encode(photo(8, 16, 0xFF), false))
            .startsWith(PNG_SIGNATURE);
    }

    @Test
    void transparentPhotoKeepsAlphaAsPng() throws IOException {
        ImageRGB image = photo(1280, 16, 0x80);

        byte[] data = encoder.encode(image, true);

        assertThat(data).startsWith(PNG_SIGNATURE);
        assertThat(decode(data).getRGB(3, 3) >>> 24).isEqualTo(0x80);
    }

    @Test
    void transparentPhotoWithoutAlphaRequestIsEncodedAsJpeg()
        throws IOException {

        assertThat(encoder.encode(photo(1280, 16, 0x80), false))
            .startsWith(JPEG_SIGNATURE);
    }

    @Test
    void opaquePhotoWithAlphaRequestIsEncodedAsJpeg() throws IOException {
        assertThat(encoder.encode(photo(1280, 16, 0xFF), true))
            .startsWith(JPEG_SIGNATURE);
    }

    @Test
    void jpegEncoderAcceptsTranslucentPixels() throws IOException {
        byte[] data = new ImageIOJpegEncoder()
            .encode(photo(64, 64, 0x40), true);

        assertThat(data).startsWith(JPEG_SIGNATURE);
        assertThat(decode(data).getWidth()).isEqualTo(64);
    }

    @Test
    void serviceLoaderMakesAdaptiveEncoderPreferred() {
        ImageEncoderRegistry registry = ImageEncoderRegistry.getInstance();
        registry.clear();
        registry.loadEncoders();

        assertThat(registry.getPreferredEncoder(false))
            .containsInstanceOf(ImageIOAdaptiveEncoder.class);
        assertThat(registry.getPreferredEncoder(true))
            .containsInstanceOf(ImageIOAdaptiveEncoder.class);
        assertThat(registry.findEncoder("jpeg"))
            .containsInstanceOf(ImageIOJpegEncoder.class);
    }

    private static BufferedImage decode(final byte[] data)
        throws IOException {

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
        assertThat(image).isNotNull();
        return image;
    }

    private static ImageRGB photo(final int w, final int h,
        final int alpha) {

        Random random = new Random(42);
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = clamp(128 + 60 * Math.sin(x / 37.0)
                    + random.nextGaussian() * 6);
                int g = clamp(128 + 40 * Math.sin(y / 5.0 + x / 50.0)
                    + random.nextGaussian() * 6);
                int b = clamp(128 + 25 * Math.sin((x + y) / 11.0)
                    + random.nextGaussian() * 6);
                px[y * w + x] = (alpha << 24) | (r << 16) | (g << 8) | b;
            }
        }
        ImageRGB image = new ArrayImageRGB(w, h);
        image.setRGB(0, 0, w, h, px, 0, w);
        return image;
    }

    private static ImageRGB text(final int w, final int h) {
        ImageRGB image = new ArrayImageRGB(w, h);
        image.fillRect(0, 0, w, h, 0xFF0000AA);
        for (int x = 1; x < w; x += 8) {
            image.fillRect(x, 3, 1, 9, 0xFFFFFFFF);
            image.fillRect(x + 1, 7, 4, 1, 0xFFFFFF55);
        }
        return image;
    }

    private static int clamp(final double value) {
        return Math.max(0, Math.min(255, (int) value));
    }
}
