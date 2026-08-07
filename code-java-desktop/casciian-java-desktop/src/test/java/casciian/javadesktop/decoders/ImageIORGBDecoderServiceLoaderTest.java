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

import casciian.image.decoders.ImageDecoder;
import casciian.image.decoders.ImageDecoderRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link ImageIORGBDecoder} is wired up as a
 * {@link java.util.ServiceLoader} provider so it is discoverable through
 * {@link ImageDecoderRegistry#loadDecoders()} when the add-on is on the
 * classpath or module path.
 */
class ImageIORGBDecoderServiceLoaderTest {

    private ImageDecoderRegistry registry;

    @BeforeEach
    void setUp() {
        registry = ImageDecoderRegistry.getInstance();
        registry.clear();
    }

    @AfterEach
    void tearDown() {
        registry.clear();
    }

    @Test
    void loadDecodersDiscoversImageIORGBDecoder() {
        int count = registry.loadDecoders();

        assertThat(count).isGreaterThanOrEqualTo(1);
        assertThat(registry.getDecoders())
            .extracting(d -> d.getClass().getName())
            .contains(ImageIORGBDecoder.class.getName());
    }

    @Test
    void loadedImageIODecoderHandlesPngFiles() {
        registry.loadDecoders();

        ImageDecoder discovered = registry.getDecoders().stream()
            .filter(ImageIORGBDecoder.class::isInstance)
            .findFirst()
            .orElseThrow();

        assertThat("photo.png").matches(discovered.getFileExtensionPattern());
        assertThat("photo.jpg").matches(discovered.getFileExtensionPattern());
    }
}
