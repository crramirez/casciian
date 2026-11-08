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
package casciian.document.parsers;

import casciian.bits.RichText;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * Registry for {@link DocumentParser} plugins.  Parsers are registered
 * explicitly or discovered with {@link ServiceLoader}, and can then be located
 * by MIME type or by file name extension.
 *
 * <pre>{@code
 * DocumentParserRegistry registry = DocumentParserRegistry.getInstance();
 * registry.loadParsers();
 *
 * // By file extension (relative images resolve against the file's folder)
 * RichText doc = registry.parse(Path.of("README.md"));
 *
 * // By MIME type, from any InputStream
 * RichText doc2 = registry.parse(stream, "text/markdown");
 * }</pre>
 */
@SuppressWarnings("java:S6548")
public class DocumentParserRegistry {

    /**
     * Singleton instance.
     */
    private static final DocumentParserRegistry INSTANCE =
        new DocumentParserRegistry();

    /**
     * List of registered parsers.
     */
    private final List<DocumentParser> parsers = new CopyOnWriteArrayList<>();

    /**
     * Private constructor for singleton.
     */
    private DocumentParserRegistry() {
    }

    /**
     * Get the singleton instance.
     *
     * @return the registry instance
     */
    public static DocumentParserRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Register a new document parser.
     *
     * @param parser the parser to register
     */
    public void registerParser(final DocumentParser parser) {
        if (parser == null) {
            throw new IllegalArgumentException("Parser cannot be null");
        }
        parsers.add(parser);
    }

    /**
     * Unregister a document parser.
     *
     * @param parser the parser to unregister
     * @return true if the parser was found and removed, false otherwise
     */
    public boolean unregisterParser(final DocumentParser parser) {
        return parsers.remove(parser);
    }

    /**
     * Get all registered parsers.
     *
     * @return a list of all registered parsers
     */
    public List<DocumentParser> getParsers() {
        return List.copyOf(parsers);
    }

    /**
     * Get a list of all file name patterns from registered parsers.  This can
     * be used for file open dialogs.
     *
     * @return a list of regex patterns
     */
    public List<String> getFileExtensionPatterns() {
        return parsers.stream()
            .map(DocumentParser::getFileExtensionPattern)
            .toList();
    }

    /**
     * Find a parser that can handle the given file based on its name.
     *
     * @param path the path to the file
     * @return a matching parser, or empty if none found
     */
    public Optional<DocumentParser> findParser(final Path path) {
        Objects.requireNonNull(path, "path cannot be null");
        Path fileName = path.getFileName();
        if (fileName == null) {
            return Optional.empty();
        }
        String name = fileName.toString();
        for (DocumentParser parser : parsers) {
            if (Pattern.compile(parser.getFileExtensionPattern())
                    .matcher(name).matches()) {
                return Optional.of(parser);
            }
        }
        return Optional.empty();
    }

    /**
     * Find a parser that can handle the given MIME type.  The lookup is
     * case-insensitive and ignores parameters such as {@code charset}.
     *
     * @param mimeType the MIME type (for example {@code "text/markdown"})
     * @return a matching parser, or empty if none found
     */
    public Optional<DocumentParser> findParser(final String mimeType) {
        if (mimeType == null) {
            return Optional.empty();
        }
        String normalized = normalizeMimeType(mimeType);
        for (DocumentParser parser : parsers) {
            for (String supported : parser.getSupportedMimeTypes()) {
                if (normalizeMimeType(supported).equals(normalized)) {
                    return Optional.of(parser);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Parse a document file using the parser registered for its file name
     * extension.  Relative references are resolved against the file's
     * directory.
     *
     * @param path the path to the document
     * @return the parsed document
     * @throws IOException if an error occurs while reading or parsing
     * @throws IllegalArgumentException if no parser is found for the file
     */
    public RichText parse(final Path path) throws IOException {
        DocumentParser parser = findParser(path).orElseThrow(() ->
            new IllegalArgumentException("No document parser found for file: "
                + path.getFileName()));
        return parser.parse(path);
    }

    /**
     * Parse a document from an {@link InputStream} using the parser
     * registered for the given MIME type.
     *
     * @param inputStream the input stream; must not be {@code null}
     * @param mimeType the MIME type; must not be {@code null}
     * @return the parsed document
     * @throws IOException if an error occurs while reading or parsing
     * @throws IllegalArgumentException if no parser is found for the MIME type
     */
    public RichText parse(final InputStream inputStream,
        final String mimeType) throws IOException {

        return parse(inputStream, mimeType, null);
    }

    /**
     * Parse a document from an {@link InputStream} using the parser
     * registered for the given MIME type.
     *
     * @param inputStream the input stream; must not be {@code null}
     * @param mimeType the MIME type; must not be {@code null}
     * @param baseDirectory directory used to resolve relative references, or
     *     {@code null}
     * @return the parsed document
     * @throws IOException if an error occurs while reading or parsing
     * @throws IllegalArgumentException if no parser is found for the MIME type
     */
    public RichText parse(final InputStream inputStream, final String mimeType,
        final Path baseDirectory) throws IOException {

        Objects.requireNonNull(inputStream, "inputStream cannot be null");
        Objects.requireNonNull(mimeType, "mimeType cannot be null");
        DocumentParser parser = findParser(mimeType).orElseThrow(() ->
            new IllegalArgumentException(
                "No document parser found for MIME type: " + mimeType));
        return parser.parse(inputStream, mimeType, baseDirectory);
    }

    /**
     * Parse a document from a {@link URL}.  The parser is selected by the
     * connection's content type, falling back to the file name in the URL
     * path when the content type is unknown or unsupported.
     *
     * @param url the URL of the document; must not be {@code null}
     * @return the parsed document
     * @throws IOException if an error occurs while reading or parsing
     * @throws IllegalArgumentException if no parser is found
     */
    public RichText parse(final URL url) throws IOException {
        Objects.requireNonNull(url, "url cannot be null");
        URLConnection connection = url.openConnection();
        String contentType = connection.getContentType();
        Optional<DocumentParser> parser = findParser(contentType);
        if (parser.isEmpty()) {
            String file = url.getPath();
            String name = file.substring(file.lastIndexOf('/') + 1);
            if (!name.isEmpty()) {
                parser = findParser(Path.of(name));
            }
        }
        DocumentParser found = parser.orElseThrow(() ->
            new IllegalArgumentException(
                "No document parser found for URL: " + url));
        Path baseDirectory = null;
        if ("file".equalsIgnoreCase(url.getProtocol())) {
            try {
                baseDirectory = Path.of(url.toURI()).toAbsolutePath()
                    .getParent();
            } catch (URISyntaxException | IllegalArgumentException e) {
                baseDirectory = null;
            }
        }
        try (InputStream is = connection.getInputStream()) {
            return found.parse(is, contentType, baseDirectory);
        }
    }

    /**
     * Clear all registered parsers.  Useful for testing or resetting the
     * registry.
     */
    public void clear() {
        parsers.clear();
    }

    /**
     * Discover and register {@link DocumentParser} implementations using
     * {@link ServiceLoader} with the current thread's context class loader,
     * falling back to this class's class loader.  Providers whose class is
     * already registered are skipped, so calling this repeatedly is safe.
     *
     * @return the number of parsers that were newly registered
     */
    public int loadParsers() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = DocumentParserRegistry.class.getClassLoader();
        }
        return loadParsers(loader);
    }

    /**
     * Discover and register {@link DocumentParser} implementations using
     * {@link ServiceLoader} with the supplied {@link ClassLoader}.
     *
     * @param classLoader the class loader to use; must not be {@code null}
     * @return the number of parsers that were newly registered
     */
    public int loadParsers(final ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader cannot be null");
        return registerFromServiceLoader(
            ServiceLoader.load(DocumentParser.class, classLoader));
    }

    /**
     * Discover and register {@link DocumentParser} implementations using
     * {@link ServiceLoader} with the supplied {@link ModuleLayer}.
     *
     * @param moduleLayer the module layer to use; must not be {@code null}
     * @return the number of parsers that were newly registered
     */
    public int loadParsers(final ModuleLayer moduleLayer) {
        Objects.requireNonNull(moduleLayer, "moduleLayer cannot be null");
        return registerFromServiceLoader(
            ServiceLoader.load(moduleLayer, DocumentParser.class));
    }

    /**
     * Register every provider of a {@link ServiceLoader} whose class is not
     * already registered.  Broken providers are skipped.
     *
     * @param serviceLoader the service loader to iterate
     * @return the number of parsers that were newly registered
     */
    private int registerFromServiceLoader(
        final ServiceLoader<DocumentParser> serviceLoader) {

        int count = 0;
        Iterator<ServiceLoader.Provider<DocumentParser>> it =
            serviceLoader.stream().iterator();
        while (true) {
            try {
                if (!it.hasNext()) {
                    break;
                }
                ServiceLoader.Provider<DocumentParser> provider = it.next();
                if (isRegistered(provider.type())) {
                    continue;
                }
                parsers.add(provider.get());
                count++;
            } catch (ServiceConfigurationError e) {
                System.err.println("DocumentParserRegistry: failed to load "
                    + "DocumentParser provider: " + e.getMessage());
            }
        }
        return count;
    }

    /**
     * Whether a parser of exactly this class is registered.
     *
     * @param type the parser class
     * @return true if registered
     */
    private boolean isRegistered(final Class<?> type) {
        for (DocumentParser parser : parsers) {
            if (parser.getClass() == type) {
                return true;
            }
        }
        return false;
    }

    /**
     * Strip MIME type parameters and lower-case the result.
     *
     * @param mimeType the MIME type
     * @return the normalized MIME type
     */
    private static String normalizeMimeType(final String mimeType) {
        return mimeType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }
}
