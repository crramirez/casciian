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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * Registry for image decoder plugins.
 * Allows registering decoders for different image formats and finding
 * the appropriate decoder for a given file.
 */
@SuppressWarnings("java:S6548")
public class ImageDecoderRegistry {

    /**
     * Singleton instance.
     */
    private static final ImageDecoderRegistry INSTANCE = new ImageDecoderRegistry();

    /**
     * List of registered decoders.
     */
    private final List<ImageDecoder> decoders = new CopyOnWriteArrayList<>();

    /**
     * Private constructor for singleton.
     */
    private ImageDecoderRegistry() {
    }

    /**
     * Get the singleton instance.
     *
     * @return the registry instance
     */
    public static ImageDecoderRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Register a new image decoder.
     *
     * @param decoder the decoder to register
     */
    public void registerDecoder(ImageDecoder decoder) {
        if (decoder == null) {
            throw new IllegalArgumentException("Decoder cannot be null");
        }
        decoders.add(decoder);
    }

    /**
     * Unregister an image decoder.
     *
     * @param decoder the decoder to unregister
     * @return true if the decoder was found and removed, false otherwise
     */
    public boolean unregisterDecoder(ImageDecoder decoder) {
        return decoders.remove(decoder);
    }

    /**
     * Get all registered decoders.
     *
     * @return a list of all registered decoders
     */
    public List<ImageDecoder> getDecoders() {
        return decoders.stream().toList();
    }

    /**
     * Get a list of all file extension patterns from registered decoders.
     * This can be used for file open dialogs.
     *
     * @return a list of regex patterns
     */
    public List<String> getFileExtensionPatterns() {
        return decoders.stream()
            .map(ImageDecoder::getFileExtensionPattern)
            .toList();
    }

    /**
     * Find a decoder that can handle the given file based on its name.
     *
     * @param path the path to the file
     * @return a matching decoder, or null if none found
     */
    public Optional<ImageDecoder> findDecoder(Path path) {
        String filename = path.getFileName().toString();
        for (ImageDecoder decoder : decoders) {
            Pattern pattern = Pattern.compile(decoder.getFileExtensionPattern());
            if (pattern.matcher(filename).matches()) {
                return Optional.of(decoder);
            }
        }
        return Optional.empty();
    }

    /**
     * Decode an image using the appropriate registered decoder.
     *
     * @param path the path to the image file
     * @return the decoded ImageRGB object
     * @throws IOException if an error occurs during decoding
     * @throws IllegalArgumentException if no decoder is found for the file
     */
    public ImageRGB decodeImage(Path path) throws IOException {
        ImageDecoder decoder = findDecoder(path).orElseThrow(
            () -> new IllegalArgumentException("No decoder found for file: " + path.getFileName()));
        return decoder.decode(path);
    }

    /**
     * Clear all registered decoders.
     * Useful for testing or resetting the registry.
     */
    public void clear() {
        decoders.clear();
    }
}
