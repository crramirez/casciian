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

import casciian.bits.ArrayImageRGB;
import casciian.bits.ImageRGB;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ImageDecoder#decode(InputStream, String)},
 * {@link ImageDecoderRegistry#findDecoder(String)}, and
 * {@link ImageDecoderRegistry#decodeImage(InputStream, String)}.
 */
@DisplayName("ImageDecoder InputStream Support Tests")
class ImageDecoderInputStreamTest {

    private ImageDecoderRegistry registry;

    @BeforeEach
    void setUp() {
        registry = ImageDecoderRegistry.getInstance();
        registry.clear();
        registry.registerDecoder(new StubMimeDecoder());
    }

    @AfterEach
    void tearDown() {
        registry.clear();
    }

    // -------------------------------------------------------------------------
    // ImageDecoder interface: decode(InputStream, String)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("decode(InputStream, mimeType) invokes decoder with supplied stream")
    void decodeInputStreamInvokesDecoder() throws IOException {
        StubMimeDecoder decoder = new StubMimeDecoder();
        InputStream is = new ByteArrayInputStream(new byte[0]);

        ImageRGB result = decoder.decode(is, "image/x-stub");

        assertNotNull(result);
    }

    // -------------------------------------------------------------------------
    // ImageDecoder interface: default decode(Path) delegates to decode(InputStream)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("default decode(Path) opens a stream and delegates to decode(InputStream, mimeType)")
    void defaultDecodePathDelegatesToInputStream() throws Exception {
        // Use a tracking decoder to verify that the default decode(Path) implementation
        // opens a stream and delegates to decode(InputStream, mimeType).
        TrackingDecoder decoder = new TrackingDecoder();
        java.nio.file.Path tmp = java.nio.file.Files.createTempFile("test", ".stub");
        try {
            decoder.decode(tmp);
            assertTrue(decoder.decodeStreamWasCalled,
                "default decode(Path) should delegate to decode(InputStream, mimeType)");
        } finally {
            java.nio.file.Files.deleteIfExists(tmp);
        }
    }

    // -------------------------------------------------------------------------
    // ImageDecoder#getSupportedMimeTypes
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getSupportedMimeTypes returns non-null list")
    void getSupportedMimeTypesReturnsNonNull() {
        StubMimeDecoder decoder = new StubMimeDecoder();
        assertNotNull(decoder.getSupportedMimeTypes());
    }

    @Test
    @DisplayName("SixelImageDecoder declares image/x-sixel MIME type")
    void sixelDecoderDeclaresSixelMimeType() {
        SixelImageDecoder decoder = new SixelImageDecoder();
        assertTrue(decoder.getSupportedMimeTypes().contains("image/x-sixel"),
            "SixelImageDecoder should declare image/x-sixel");
    }

    // -------------------------------------------------------------------------
    // ImageDecoderRegistry#findDecoder(String mimeType)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findDecoder(mimeType) returns decoder matching the MIME type")
    void findDecoderByMimeTypeReturnsMatch() {
        assertTrue(registry.findDecoder("image/x-stub").isPresent());
    }

    @Test
    @DisplayName("findDecoder(mimeType) returns empty when no decoder matches")
    void findDecoderByMimeTypeReturnsEmptyForUnknown() {
        assertTrue(registry.findDecoder("image/unknown-format").isEmpty());
    }

    @Test
    @DisplayName("findDecoder(null) returns empty")
    void findDecoderByNullMimeTypeReturnsEmpty() {
        assertTrue(registry.findDecoder((String) null).isEmpty());
    }

    // -------------------------------------------------------------------------
    // ImageDecoderRegistry#decodeImage(InputStream, String)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("decodeImage(InputStream, mimeType) decodes using matching decoder")
    void decodeImageFromInputStreamUsesMatchingDecoder() throws IOException {
        InputStream is = new ByteArrayInputStream(new byte[0]);

        ImageRGB result = registry.decodeImage(is, "image/x-stub");

        assertNotNull(result);
    }

    @Test
    @DisplayName("decodeImage(InputStream, mimeType) throws when no decoder found")
    void decodeImageFromInputStreamThrowsWhenNoDecoder() {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        assertThrows(IllegalArgumentException.class,
            () -> registry.decodeImage(is, "image/unknown-format"));
    }

    @Test
    @DisplayName("decodeImage(InputStream, null) throws NullPointerException")
    void decodeImageFromInputStreamThrowsOnNullMimeType() {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        assertThrows(NullPointerException.class,
            () -> registry.decodeImage(is, null));
    }

    @Test
    @DisplayName("decodeImage(null, mimeType) throws NullPointerException")
    void decodeImageFromInputStreamThrowsOnNullStream() {
        assertThrows(NullPointerException.class,
            () -> registry.decodeImage(null, "image/x-stub"));
    }

    @Test
    @DisplayName("decodeImage(URL, null) throws NullPointerException")
    void decodeImageFromUrlThrowsOnNullUrl() {
        assertThrows(NullPointerException.class,
            () -> registry.decodeImage((java.net.URL) null));
    }

    // -------------------------------------------------------------------------
    // Stub helpers
    // -------------------------------------------------------------------------

    /**
     * Minimal decoder used to test MIME-type-based dispatch.
     */
    static final class StubMimeDecoder implements ImageDecoder {

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
            return "^.*\\.stub$";
        }

        @Override
        public String getFormatDescription() {
            return "Stub Image (*.stub)";
        }
    }

    /**
     * Decoder that tracks whether {@link #decode(InputStream, String)} was called.
     */
    static final class TrackingDecoder implements ImageDecoder {
        boolean decodeStreamWasCalled = false;

        @Override
        public ImageRGB decode(InputStream inputStream, String mimeType) throws IOException {
            decodeStreamWasCalled = true;
            return new ArrayImageRGB(1, 1);
        }

        @Override
        public List<String> getSupportedMimeTypes() {
            return List.of();
        }

        @Override
        public String getFileExtensionPattern() {
            return "^.*\\.stub$";
        }

        @Override
        public String getFormatDescription() {
            return "Tracking decoder";
        }
    }
}
