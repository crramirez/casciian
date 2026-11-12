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

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry for image encoder plugins.
 *
 * <p>The first time the registry is queried, it discovers
 * {@link ImageEncoder} implementations with {@link ServiceLoader}.  This
 * picks up the built-in {@link BmpImageEncoder} and any add-on encoders
 * (e.g. PNG or JPEG) on the classpath or module path.  Encoders can also be
 * registered explicitly with {@link #registerEncoder(ImageEncoder)}.</p>
 *
 * <p>{@link #getPreferredEncoder(boolean)} selects the encoder with the
 * highest {@link ImageEncoder#getPriority() priority}; registration order
 * breaks ties.</p>
 */
@SuppressWarnings("java:S6548")
public class ImageEncoderRegistry {

    /**
     * Singleton instance.
     */
    private static final ImageEncoderRegistry INSTANCE =
        new ImageEncoderRegistry();

    /**
     * List of registered encoders.
     */
    private final List<ImageEncoder> encoders = new CopyOnWriteArrayList<>();

    /**
     * If true, the automatic ServiceLoader discovery has run.
     */
    private volatile boolean discovered = false;

    /**
     * Private constructor for singleton.
     */
    private ImageEncoderRegistry() {
    }

    /**
     * Get the singleton instance.
     *
     * @return the registry instance
     */
    public static ImageEncoderRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Register a new image encoder.
     *
     * @param encoder the encoder to register
     */
    public void registerEncoder(final ImageEncoder encoder) {
        if (encoder == null) {
            throw new IllegalArgumentException("Encoder cannot be null");
        }
        encoders.add(encoder);
    }

    /**
     * Unregister an image encoder.
     *
     * @param encoder the encoder to unregister
     * @return true if the encoder was found and removed, false otherwise
     */
    public boolean unregisterEncoder(final ImageEncoder encoder) {
        return encoders.remove(encoder);
    }

    /**
     * Get all registered encoders, discovering them first if needed.
     *
     * @return a list of all registered encoders
     */
    public List<ImageEncoder> getEncoders() {
        discover();
        return encoders.stream().toList();
    }

    /**
     * Find an encoder by its {@link ImageEncoder#getName() name} or
     * {@link ImageEncoder#getMimeType() MIME type}, case-insensitively.
     * If several match, the one with the highest priority is returned.
     *
     * @param nameOrMimeType e.g. "png" or "image/png"
     * @return a matching encoder, or empty if none found
     */
    public Optional<ImageEncoder> findEncoder(final String nameOrMimeType) {
        if (nameOrMimeType == null) {
            return Optional.empty();
        }
        String key = nameOrMimeType.split(";")[0].trim()
            .toLowerCase(Locale.ROOT);
        return byPriority(getEncoders().stream()
            .filter(e -> e.getName().toLowerCase(Locale.ROOT).equals(key)
                || e.getMimeType().toLowerCase(Locale.ROOT).equals(key))
            .toList());
    }

    /**
     * Get the encoder with the highest priority.  If alpha is requested,
     * encoders that support alpha are preferred over those that do not.
     *
     * @param alpha if true, prefer encoders that preserve alpha
     * @return the preferred encoder, or empty if none is registered
     */
    public Optional<ImageEncoder> getPreferredEncoder(final boolean alpha) {
        List<ImageEncoder> all = getEncoders();
        if (alpha) {
            Optional<ImageEncoder> withAlpha = byPriority(all.stream()
                .filter(ImageEncoder::supportsAlpha).toList());
            if (withAlpha.isPresent()) {
                return withAlpha;
            }
        }
        return byPriority(all);
    }

    /**
     * Clear all registered encoders.  Automatic discovery will not run
     * again; call {@link #loadEncoders()} to rediscover.
     */
    public void clear() {
        discovered = true;
        encoders.clear();
    }

    /**
     * Discover and register {@link ImageEncoder} implementations using
     * {@link ServiceLoader} with the current thread's context class loader,
     * falling back to this class's class loader.  Encoders whose class is
     * already registered are skipped.
     *
     * @return the number of encoders that were newly registered
     */
    public int loadEncoders() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = ImageEncoderRegistry.class.getClassLoader();
        }
        return loadEncoders(loader);
    }

    /**
     * Discover and register {@link ImageEncoder} implementations using
     * {@link ServiceLoader} with the supplied {@link ClassLoader}.
     *
     * @param classLoader the class loader to use for service discovery
     * @return the number of encoders that were newly registered
     */
    public int loadEncoders(final ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader cannot be null");
        int count = register(ServiceLoader.load(ImageEncoder.class,
            classLoader));
        discovered = true;
        return count;
    }

    /**
     * Discover and register {@link ImageEncoder} implementations using
     * {@link ServiceLoader} with the supplied {@link ModuleLayer}.
     *
     * @param moduleLayer the module layer to use for service discovery
     * @return the number of encoders that were newly registered
     */
    public int loadEncoders(final ModuleLayer moduleLayer) {
        Objects.requireNonNull(moduleLayer, "moduleLayer cannot be null");
        int count = register(ServiceLoader.load(moduleLayer,
            ImageEncoder.class));
        discovered = true;
        return count;
    }

    /**
     * Run the automatic discovery once.
     */
    private void discover() {
        if (discovered) {
            return;
        }
        synchronized (this) {
            if (!discovered) {
                loadEncoders();
            }
        }
    }

    /**
     * Pick the highest-priority encoder, keeping registration order for
     * ties.
     *
     * @param candidates the encoders to choose from
     * @return the chosen encoder, or empty
     */
    private static Optional<ImageEncoder> byPriority(
        final List<ImageEncoder> candidates) {

        return candidates.stream().reduce((best, next) ->
            Comparator.comparingInt(ImageEncoder::getPriority)
                .compare(next, best) > 0 ? next : best);
    }

    /**
     * Register every provider of a service loader whose class is not
     * registered yet.  Broken providers are skipped.
     *
     * @param serviceLoader the service loader to iterate
     * @return the number of encoders that were newly registered
     */
    private synchronized int register(
        final ServiceLoader<ImageEncoder> serviceLoader) {

        int count = 0;
        Iterator<ServiceLoader.Provider<ImageEncoder>> it =
            serviceLoader.stream().iterator();
        while (true) {
            try {
                if (!it.hasNext()) {
                    break;
                }
                ServiceLoader.Provider<ImageEncoder> provider = it.next();
                Class<? extends ImageEncoder> type = provider.type();
                if (encoders.stream().anyMatch(e -> e.getClass() == type)) {
                    continue;
                }
                encoders.add(provider.get());
                count++;
            } catch (ServiceConfigurationError e) {
                System.err.println(
                    "ImageEncoderRegistry: failed to load ImageEncoder provider: "
                        + e.getMessage());
            }
        }
        return count;
    }
}
