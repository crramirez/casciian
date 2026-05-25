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
 */
package casciian.image.decoders;

import casciian.bits.ImageRGB;
import casciian.bits.ArrayImageRGB;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ImageDecoderRegistry} ServiceLoader-based decoder
 * discovery, including {@link ClassLoader} and {@link ModuleLayer}
 * overloads.
 */
@DisplayName("ImageDecoderRegistry ServiceLoader Tests")
class ImageDecoderRegistryServiceLoaderTest {

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
    @DisplayName("loadDecoders() discovers SixelImageDecoder via default ClassLoader")
    void loadDecodersWithDefaultClassLoader() {
        int count = registry.loadDecoders();

        assertTrue(count >= 1, "expected at least one decoder, got " + count);
        assertTrue(containsType(registry.getDecoders(), SixelImageDecoder.class),
            "SixelImageDecoder should be discovered via ServiceLoader");
    }

    @Test
    @DisplayName("loadDecoders(ClassLoader) discovers SixelImageDecoder via supplied loader")
    void loadDecodersWithExplicitClassLoader() {
        ClassLoader loader = ImageDecoderRegistry.class.getClassLoader();

        int count = registry.loadDecoders(loader);

        assertTrue(count >= 1, "expected at least one decoder, got " + count);
        assertTrue(containsType(registry.getDecoders(), SixelImageDecoder.class));
    }

    @Test
    @DisplayName("loadDecoders(ClassLoader) rejects null ClassLoader")
    void loadDecodersWithNullClassLoaderThrows() {
        NullPointerException ex = assertThrows(NullPointerException.class,
            () -> registry.loadDecoders((ClassLoader) null));
        assertTrue(ex.getMessage() != null && ex.getMessage().contains("classLoader"));
    }

    @Test
    @DisplayName("loadDecoders(ModuleLayer) accepts the boot layer without throwing")
    void loadDecodersWithModuleLayer() {
        ModuleLayer bootLayer = ModuleLayer.boot();

        int count = registry.loadDecoders(bootLayer);

        // When tests run on the module path, the boot layer sees the
        // casciian module's `provides` declaration; on the classpath it does
        // not. Either way the call must succeed and return a non-negative
        // count.
        assertTrue(count >= 0);
    }

    @Test
    @DisplayName("loadDecoders(ModuleLayer) rejects null ModuleLayer")
    void loadDecodersWithNullModuleLayerThrows() {
        NullPointerException ex = assertThrows(NullPointerException.class,
            () -> registry.loadDecoders((ModuleLayer) null));
        assertTrue(ex.getMessage() != null && ex.getMessage().contains("moduleLayer"));
    }

    @Test
    @DisplayName("Decoders loaded via ServiceLoader are usable through findDecoder")
    void loadedDecoderCanBeFoundByExtension() {
        registry.loadDecoders();

        // SixelImageDecoder matches *.six files
        assertTrue(registry.findDecoder(Path.of("sample.six")).isPresent());
        assertFalse(registry.findDecoder(Path.of("sample.unknownext")).isPresent());
    }

    @Test
    @DisplayName("ServiceLoader-loaded decoders coexist with manually registered decoders")
    void serviceLoaderAndManualRegistrationCoexist() {
        registry.registerDecoder(new StubDecoder());
        registry.loadDecoders();

        assertTrue(registry.findDecoder(Path.of("foo.stub")).isPresent());
        assertTrue(registry.findDecoder(Path.of("foo.six")).isPresent());
    }

    private static boolean containsType(List<ImageDecoder> decoders, Class<?> type) {
        return decoders.stream().anyMatch(d -> d.getClass().equals(type));
    }

    /**
     * Trivial decoder used to demonstrate that explicitly-registered decoders
     * coexist with ServiceLoader-loaded ones.
     */
    static final class StubDecoder implements ImageDecoder {
        @Override
        public ImageRGB decode(InputStream inputStream, String mimeType) throws IOException {
            return new ArrayImageRGB(1, 1);
        }

        @Override
        public List<String> getSupportedMimeTypes() {
            return List.of("image/x-stub");
        }

        @Override
        public String getFileExtensionPattern() {
            return "^.*\\.[sS][tT][uU][bB]$";
        }

        @Override
        public String getFormatDescription() {
            return "Stub Image (*.stub)";
        }
    }
}
