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
package casciian.image.encoders;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import casciian.bits.ImageRGB;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests for encoder discovery and selection.
 */
@DisplayName("ImageEncoderRegistry")
class ImageEncoderRegistryTest {

    private final ImageEncoderRegistry registry =
        ImageEncoderRegistry.getInstance();

    private final List<ImageEncoder> registered = new ArrayList<>();

    @AfterEach
    void tearDown() {
        registered.forEach(registry::unregisterEncoder);
    }

    private ImageEncoder register(final String name, final int priority,
        final boolean alpha) {

        ImageEncoder encoder = new FakeEncoder(name, priority, alpha);
        registry.registerEncoder(encoder);
        registered.add(encoder);
        return encoder;
    }

    @Test
    @DisplayName("The built-in PNG encoder is preferred over built-in BMP")
    void discoversBuiltIns() {
        assertThat(registry.getEncoders())
            .hasAtLeastOneElementOfType(BmpImageEncoder.class)
            .hasAtLeastOneElementOfType(PngImageEncoder.class);
        assertThat(registry.getPreferredEncoder(false)).get()
            .isInstanceOf(PngImageEncoder.class);
        assertThat(registry.getPreferredEncoder(true)).get()
            .isInstanceOf(PngImageEncoder.class);
        assertThat(registry.findEncoder("BMP")).get()
            .isInstanceOf(BmpImageEncoder.class);
        assertThat(registry.findEncoder("image/png")).get()
            .isInstanceOf(PngImageEncoder.class);
    }

    @Test
    @DisplayName("Add-on encoders with higher priority win over built-ins")
    void higherPriorityWins() {
        ImageEncoder webp = register("webp", 20, true);

        assertThat(registry.getPreferredEncoder(false)).containsSame(webp);
        assertThat(registry.findEncoder("image/webp; q=1")).containsSame(webp);
        assertThat(registry.findEncoder("bmp")).get()
            .isInstanceOf(BmpImageEncoder.class);
    }

    @Test
    @DisplayName("Alpha requests prefer encoders that keep alpha")
    void alphaPrefersAlphaEncoders() {
        ImageEncoder jpeg = register("jpeg", 20, false);

        assertThat(registry.getPreferredEncoder(false)).containsSame(jpeg);
        assertThat(registry.getPreferredEncoder(true)).get()
            .isInstanceOf(PngImageEncoder.class);
    }

    @Test
    @DisplayName("Loading again does not duplicate discovered encoders")
    void reloadingDoesNotDuplicate() {
        int before = registry.getEncoders().size();

        assertThat(registry.loadEncoders()).isZero();
        assertThat(registry.getEncoders()).hasSize(before);
    }

    @Test
    @DisplayName("Malformed service entries do not stop provider discovery")
    void malformedServiceEntryDoesNotStopDiscovery() throws Exception {
        URL serviceFile = URL.of(URI.create("memory:image-encoders"),
            new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(final URL url) {
                    return new URLConnection(url) {
                        @Override
                        public void connect() {
                        }

                        @Override
                        public java.io.InputStream getInputStream() {
                            String providers = "not a valid provider\n"
                                + BmpImageEncoder.class.getName() + "\n";
                            return new ByteArrayInputStream(providers.getBytes(
                                StandardCharsets.UTF_8));
                        }
                    };
                }
            });
        ClassLoader classLoader = new ClassLoader(
            ImageEncoderRegistryTest.class.getClassLoader()) {
            @Override
            public java.util.Enumeration<URL> getResources(final String name)
                throws IOException {
                if (name.equals("META-INF/services/"
                    + ImageEncoder.class.getName())) {
                    return Collections.enumeration(List.of(serviceFile));
                }
                return super.getResources(name);
            }
        };

        assertThatCode(() -> registry.loadEncoders(classLoader))
            .doesNotThrowAnyException();
        assertThat(registry.getEncoders())
            .hasAtLeastOneElementOfType(BmpImageEncoder.class);
    }

    /**
     * Test encoder that emits its name as bytes.
     */
    static final class FakeEncoder implements ImageEncoder {
        private final String name;
        private final int priority;
        private final boolean alpha;

        FakeEncoder(final String name, final int priority,
            final boolean alpha) {
            this.name = name;
            this.priority = priority;
            this.alpha = alpha;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getMimeType() {
            return "image/" + name;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public boolean supportsAlpha() {
            return alpha;
        }

        @Override
        public byte[] encode(final ImageRGB image, final boolean keepAlpha) {
            return name.getBytes();
        }
    }
}
