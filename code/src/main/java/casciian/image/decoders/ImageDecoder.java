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
package casciian.image.decoders;

import casciian.bits.ImageRGB;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Interface for image decoder plugins.
 * Implementations can decode various image formats into ImageRGB objects.
 *
 * <p>The primary decoding method is {@link #decode(InputStream, String)}, which accepts
 * an {@link InputStream} and an optional MIME type hint. Default implementations of
 * {@link #decode(Path)} and {@link #decode(URL)} open a stream from the source and
 * delegate to the primary method.</p>
 */
public interface ImageDecoder {

    /**
     * Decode an image from an {@link InputStream}.
     *
     * <p>The {@code mimeType} parameter is an optional hint about the content type of
     * the stream (e.g., {@code "image/png"}). Implementations that can auto-detect the
     * format may ignore this parameter; others may require it.</p>
     *
     * @param inputStream the input stream containing image data; must not be {@code null}
     * @param mimeType    the MIME type of the image (e.g., {@code "image/png"}), or
     *                    {@code null} if unknown
     * @return the decoded {@link ImageRGB} object
     * @throws IOException if an error occurs during decoding
     */
    ImageRGB decode(InputStream inputStream, String mimeType) throws IOException;

    /**
     * Decode an image from a file path.
     *
     * <p>The default implementation opens an {@link InputStream} from the path, probes
     * the MIME type using {@link Files#probeContentType(Path)}, and delegates to
     * {@link #decode(InputStream, String)}.</p>
     *
     * @param path the path to the image file
     * @return the decoded ImageRGB object
     * @throws IOException if an error occurs during decoding
     */
    default ImageRGB decode(Path path) throws IOException {
        String mimeType = Files.probeContentType(path);
        try (InputStream is = Files.newInputStream(path)) {
            return decode(is, mimeType);
        }
    }

    /**
     * Decode an image from a {@link URL}.
     *
     * <p>The default implementation opens a {@link URLConnection}, reads the content
     * type from the connection, and delegates to {@link #decode(InputStream, String)}.</p>
     *
     * @param url the URL of the image resource; must not be {@code null}
     * @return the decoded {@link ImageRGB} object
     * @throws IOException if an error occurs while opening the connection or decoding
     */
    default ImageRGB decode(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        String mimeType = connection.getContentType();
        try (InputStream is = connection.getInputStream()) {
            return decode(is, mimeType);
        }
    }

    /**
     * Get the MIME types supported by this decoder.
     *
     * <p>These are used by {@link ImageDecoderRegistry} to locate a decoder for a given
     * MIME type. Return an empty list if the decoder does not declare MIME type support
     * and relies solely on file extension matching.</p>
     *
     * @return an immutable list of supported MIME type strings (e.g.,
     *         {@code ["image/png", "image/jpeg"]}); never {@code null}
     */
    List<String> getSupportedMimeTypes();

    /**
     * Get the regex pattern that matches file extensions this decoder supports.
     * The pattern will be used to filter files in file open dialogs.
     *
     * @return a regex pattern string (e.g., "^.*\\.[sS][iI][xX]$" for .six files)
     */
    String getFileExtensionPattern();

    /**
     * Get a human-readable description of the supported format.
     *
     * @return a description string (e.g., "Sixel Image Files (*.six)")
     */
    String getFormatDescription();
}
