/*
 * Casciian - Java Text User Interface
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
package casciian.imagedecoders.decoders;

import casciian.image.decoders.ImageDecoderRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link BMP24ImageDecoder} and {@link XPMImageDecoder} are wired
 * up as {@link java.util.ServiceLoader} providers so they are discoverable
 * through {@link ImageDecoderRegistry#loadDecoders()} when the add-on is on the
 * classpath or module path.
 */
class ImageDecodersServiceLoaderTest {

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
    void loadDecodersDiscoversBmpAndXpmDecoders() {
        int count = registry.loadDecoders();

        assertThat(count).isGreaterThanOrEqualTo(2);
        assertThat(registry.getDecoders())
            .extracting(d -> d.getClass().getName())
            .contains(
                BMP24ImageDecoder.class.getName(),
                XPMImageDecoder.class.getName());
    }
}
