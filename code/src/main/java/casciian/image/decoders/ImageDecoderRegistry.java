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
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
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
     * Find a decoder that can handle the given MIME type.
     *
     * @param mimeType the MIME type of the image (e.g., {@code "image/png"})
     * @return a matching decoder, or empty if none found
     */
    public Optional<ImageDecoder> findDecoder(String mimeType) {
        if (mimeType == null) {
            return Optional.empty();
        }
        for (ImageDecoder decoder : decoders) {
            if (decoder.getSupportedMimeTypes().contains(mimeType)) {
                return Optional.of(decoder);
            }
        }
        return Optional.empty();
    }

    /**
     * Decode an image from an {@link InputStream} using the appropriate registered decoder.
     *
     * <p>The decoder is selected by matching the supplied {@code mimeType} against the
     * MIME types declared by each registered decoder via
     * {@link ImageDecoder#getSupportedMimeTypes()}. If no decoder claims the MIME type,
     * an {@link IllegalArgumentException} is thrown.</p>
     *
     * @param inputStream the input stream containing image data; must not be {@code null}
     * @param mimeType    the MIME type of the image (e.g., {@code "image/png"}); must not
     *                    be {@code null}
     * @return the decoded {@link ImageRGB} object
     * @throws IOException              if an error occurs during decoding
     * @throws IllegalArgumentException if no decoder is found for the given MIME type
     * @throws NullPointerException     if {@code inputStream} or {@code mimeType} is
     *                                  {@code null}
     */
    public ImageRGB decodeImage(InputStream inputStream, String mimeType) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream cannot be null");
        Objects.requireNonNull(mimeType, "mimeType cannot be null");
        ImageDecoder decoder = findDecoder(mimeType).orElseThrow(
            () -> new IllegalArgumentException("No decoder found for MIME type: " + mimeType));
        return decoder.decode(inputStream, mimeType);
    }

    /**
     * Decode an image from a {@link URL} using the appropriate registered decoder.
     *
     * <p>The MIME type is determined from the URL connection's content type and used
     * to select the appropriate decoder. If the content type cannot be determined or
     * no decoder is registered for it, an {@link IllegalArgumentException} is thrown.</p>
     *
     * @param url the URL of the image resource; must not be {@code null}
     * @return the decoded {@link ImageRGB} object
     * @throws IOException              if an error occurs while opening the connection
     *                                  or decoding
     * @throws IllegalArgumentException if no decoder is found for the content type
     * @throws NullPointerException     if {@code url} is {@code null}
     */
    public ImageRGB decodeImage(URL url) throws IOException {
        Objects.requireNonNull(url, "url cannot be null");
        java.net.URLConnection connection = url.openConnection();
        // Extract only the base MIME type, stripping any parameters such as
        // "charset=utf-8" (e.g., "image/png; charset=utf-8" -> "image/png").
        String rawContentType = connection.getContentType();
        String mimeType = rawContentType != null
            ? rawContentType.split(";")[0].trim()
            : null;
        ImageDecoder decoder = findDecoder(mimeType).orElseThrow(
            () -> new IllegalArgumentException(
                "No decoder found for MIME type: " + mimeType + " (URL: " + url + ")"));
        try (InputStream is = connection.getInputStream()) {
            return decoder.decode(is, mimeType);
        }
    }

    /**
     * Clear all registered decoders.
     * Useful for testing or resetting the registry.
     */
    public void clear() {
        decoders.clear();
    }

    /**
     * Discover and register {@link ImageDecoder} implementations using
     * {@link ServiceLoader} with the current thread's context class loader,
     * falling back to this class's class loader if no context class loader
     * is available.
     *
     * <p>This is the most convenient overload for typical applications. For
     * environments where a specific class loader must be used (e.g. OSGi or
     * isolated plugin loaders), use {@link #loadDecoders(ClassLoader)}. For
     * modular runtimes with custom layers, use
     * {@link #loadDecoders(ModuleLayer)}.</p>
     *
     * @return the number of decoders that were newly registered
     */
    public int loadDecoders() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = ImageDecoderRegistry.class.getClassLoader();
        }
        return loadDecoders(loader);
    }

    /**
     * Discover and register {@link ImageDecoder} implementations using
     * {@link ServiceLoader} with the supplied {@link ClassLoader}.
     *
     * <p>This overload is useful for environments such as OSGi frameworks,
     * application servers, or plugin systems where the default class loader
     * may not see all service providers.</p>
     *
     * @param classLoader the class loader to use for service discovery; must
     *     not be {@code null}
     * @return the number of decoders that were newly registered
     * @throws NullPointerException if {@code classLoader} is {@code null}
     */
    public int loadDecoders(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader cannot be null");
        ServiceLoader<ImageDecoder> serviceLoader =
            ServiceLoader.load(ImageDecoder.class, classLoader);
        return registerFromServiceLoader(serviceLoader);
    }

    /**
     * Discover and register {@link ImageDecoder} implementations using
     * {@link ServiceLoader} with the supplied {@link ModuleLayer}.
     *
     * <p>This overload is intended for applications using the Java Platform
     * Module System (JPMS) with custom module layers, where service
     * providers are declared in {@code module-info.java} via
     * {@code provides casciian.image.decoders.ImageDecoder with ...}.</p>
     *
     * @param moduleLayer the module layer to use for service discovery; must
     *     not be {@code null}
     * @return the number of decoders that were newly registered
     * @throws NullPointerException if {@code moduleLayer} is {@code null}
     */
    public int loadDecoders(ModuleLayer moduleLayer) {
        Objects.requireNonNull(moduleLayer, "moduleLayer cannot be null");
        ServiceLoader<ImageDecoder> serviceLoader =
            ServiceLoader.load(moduleLayer, ImageDecoder.class);
        return registerFromServiceLoader(serviceLoader);
    }

    /**
     * Register all decoders provided by the given {@link ServiceLoader}.
     * Decoders that fail to instantiate are skipped, so that a single broken
     * provider does not prevent the rest from being registered.
     *
     * @param serviceLoader the service loader to iterate
     * @return the number of decoders that were newly registered
     */
    private int registerFromServiceLoader(ServiceLoader<ImageDecoder> serviceLoader) {
        int count = 0;
        // Iterate lazily so each provider is loaded and validated incrementally;
        // a failure in one provider does not prevent later ones from loading.
        java.util.Iterator<ServiceLoader.Provider<ImageDecoder>> it =
            serviceLoader.stream().iterator();
        while (it.hasNext()) {
            try {
                ServiceLoader.Provider<ImageDecoder> provider = it.next();
                decoders.add(provider.get());
                count++;
            } catch (ServiceConfigurationError e) {
                // Skip broken providers so discovery of the rest can continue,
                // but surface the failure so it can be diagnosed.
                System.err.println(
                    "ImageDecoderRegistry: failed to load ImageDecoder provider: "
                        + e.getMessage());
            }
        }
        return count;
    }
}
