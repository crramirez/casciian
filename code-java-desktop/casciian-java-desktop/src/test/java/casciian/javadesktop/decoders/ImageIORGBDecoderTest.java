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
package casciian.javadesktop.decoders;

import casciian.bits.ImageRGB;
import casciian.image.decoders.ImageDecoder;
import casciian.image.decoders.ImageDecoderRegistry;
import casciian.javadesktop.bits.BufferedImageRGB;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Black-box tests for {@link ImageIORGBDecoder}. Exercises the decoder both
 * directly and through {@link ImageDecoderRegistry} to verify the API
 * contract without inspecting implementation details.
 */
class ImageIORGBDecoderTest {

    @Test
    void decodesPngFileIntoMatchingImageRGB(@TempDir Path tmp) throws IOException {
        Path png = writeSampleImage(tmp.resolve("sample.png"), "png");

        ImageRGB image = new ImageIORGBDecoder().decode(png);

        // The decoder returns a BufferedImageRGB so AWT-aware consumers
        // can downcast and reach the underlying BufferedImage directly.
        assertThat(image).isInstanceOf(BufferedImageRGB.class);
        assertThat(image.getWidth()).isEqualTo(2);
        assertThat(image.getHeight()).isEqualTo(2);
        // PNG is lossless: pixels are preserved exactly. ImageIO returns
        // pixels in 0xAARRGGBB; opaque PNGs come back with alpha=0xFF.
        assertThat(image.getRGB(0, 0) & 0x00FFFFFF).isEqualTo(0xFF0000);
        assertThat(image.getRGB(1, 0) & 0x00FFFFFF).isEqualTo(0x00FF00);
        assertThat(image.getRGB(0, 1) & 0x00FFFFFF).isEqualTo(0x0000FF);
        assertThat(image.getRGB(1, 1) & 0x00FFFFFF).isEqualTo(0xFFFFFF);
    }

    @Test
    void decodesJpegFileWithMatchingDimensions(@TempDir Path tmp) throws IOException {
        Path jpg = writeSampleImage(tmp.resolve("sample.jpg"), "jpg");

        ImageRGB image = new ImageIORGBDecoder().decode(jpg);

        // JPEG is lossy so we don't compare individual pixel values, but the
        // dimensions and successful decode are part of the API contract.
        assertThat(image.getWidth()).isEqualTo(2);
        assertThat(image.getHeight()).isEqualTo(2);
    }

    @Test
    void defaultExtensionPatternMatchesPngJpgAndJpeg() {
        ImageDecoder decoder = new ImageIORGBDecoder();
        String pattern = decoder.getFileExtensionPattern();

        assertThat("photo.png").matches(pattern);
        assertThat("PHOTO.PNG").matches(pattern);
        assertThat("photo.jpg").matches(pattern);
        assertThat("photo.JPEG").matches(pattern);
        assertThat("photo.bmp").doesNotMatch(pattern);
        assertThat(decoder.getFormatDescription()).isNotBlank();
    }

    @Test
    void integratesWithImageDecoderRegistry(@TempDir Path tmp) throws IOException {
        Path png = writeSampleImage(tmp.resolve("via-registry.png"), "png");

        ImageDecoderRegistry registry = ImageDecoderRegistry.getInstance();
        ImageDecoder decoder = new ImageIORGBDecoder();
        registry.registerDecoder(decoder);
        try {
            ImageRGB image = registry.decodeImage(png);
            assertThat(image.getWidth()).isEqualTo(2);
            assertThat(image.getHeight()).isEqualTo(2);
        } finally {
            registry.unregisterDecoder(decoder);
        }
    }

    @Test
    void decodeThrowsWhenFileIsNotAnImage(@TempDir Path tmp) throws IOException {
        Path bogus = tmp.resolve("bogus.png");
        Files.writeString(bogus, "this is not an image");

        assertThatThrownBy(() -> new ImageIORGBDecoder().decode(bogus))
                .isInstanceOf(IOException.class);
    }

    @Test
    void rejectsNullArguments() {
        assertThatThrownBy(() -> new ImageIORGBDecoder(null, "desc"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ImageIORGBDecoder("pat", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ImageIORGBDecoder().decode(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void customConfigurationIsHonored() {
        ImageDecoder decoder = new ImageIORGBDecoder(
                "^.*\\.[gG][iI][fF]$", "GIF Image Files (*.gif)");

        assertThat(decoder.getFileExtensionPattern()).isEqualTo("^.*\\.[gG][iI][fF]$");
        assertThat(decoder.getFormatDescription()).isEqualTo("GIF Image Files (*.gif)");
    }

    /**
     * Writes a tiny 2x2 image with four distinct corners to {@code path}
     * using the given ImageIO format name.
     */
    private static Path writeSampleImage(Path path, String formatName) throws IOException {
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, 0xFF0000);
        img.setRGB(1, 0, 0x00FF00);
        img.setRGB(0, 1, 0x0000FF);
        img.setRGB(1, 1, 0xFFFFFF);
        if (!ImageIO.write(img, formatName, path.toFile())) {
            throw new IOException("No writer for format " + formatName);
        }
        return path;
    }
}
