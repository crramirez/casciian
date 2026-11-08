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
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Interface for document parser plugins.  Implementations convert a document
 * format (for example Markdown) into the canonical Casciian {@link RichText}
 * model, which can then be displayed by widgets such as
 * {@link casciian.TTextAnsi#setRichText(RichText)}.
 *
 * <p>The primary parsing method is
 * {@link #parse(InputStream, String, Path)}, which accepts an
 * {@link InputStream}, an optional MIME type hint, and an optional base
 * directory used to resolve relative references (such as image paths) found
 * in the document.  Default implementations of {@link #parse(Path)} and
 * {@link #parse(URL)} open a stream from the source and delegate to the
 * primary method.</p>
 *
 * <p>Parsers are discovered with {@link java.util.ServiceLoader} by
 * {@link DocumentParserRegistry#loadParsers()} and can be located by MIME type
 * or by file name extension.</p>
 */
public interface DocumentParser {

    /**
     * Parse a document from an {@link InputStream}.
     *
     * @param inputStream the input stream containing the document; must not
     *     be {@code null}.  The stream is not closed by this method.
     * @param mimeType the MIME type of the document (for example
     *     {@code "text/markdown; charset=utf-8"}), or {@code null} if unknown.
     *     Implementations should honor a {@code charset} parameter when
     *     present.
     * @param baseDirectory the directory against which relative references
     *     (for example image paths) are resolved, or {@code null} to leave
     *     them relative to the current working directory
     * @return the parsed document
     * @throws IOException if an error occurs while reading or parsing
     */
    RichText parse(InputStream inputStream, String mimeType,
        Path baseDirectory) throws IOException;

    /**
     * Parse a document from an {@link InputStream} without a base directory.
     *
     * @param inputStream the input stream containing the document; must not
     *     be {@code null}.  The stream is not closed by this method.
     * @param mimeType the MIME type of the document, or {@code null}
     * @return the parsed document
     * @throws IOException if an error occurs while reading or parsing
     */
    default RichText parse(final InputStream inputStream,
        final String mimeType) throws IOException {

        return parse(inputStream, mimeType, null);
    }

    /**
     * Parse a document from a file.  Relative references in the document are
     * resolved against the file's directory.
     *
     * @param path the path to the document file; must not be {@code null}
     * @return the parsed document
     * @throws IOException if an error occurs while reading or parsing
     */
    default RichText parse(final Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        String mimeType = Files.probeContentType(path);
        Path baseDirectory = path.toAbsolutePath().getParent();
        try (InputStream is = Files.newInputStream(path)) {
            return parse(is, mimeType, baseDirectory);
        }
    }

    /**
     * Parse a document from a {@link URL}.  For {@code file:} URLs, relative
     * references are resolved against the file's directory.
     *
     * @param url the URL of the document; must not be {@code null}
     * @return the parsed document
     * @throws IOException if an error occurs while reading or parsing
     */
    default RichText parse(final URL url) throws IOException {
        URLConnection connection = url.openConnection();
        String mimeType = connection.getContentType();
        try (InputStream is = connection.getInputStream()) {
            return parse(is, mimeType, baseDirectoryOf(url));
        }
    }

    /**
     * Get the MIME types supported by this parser.
     *
     * @return an immutable list of supported MIME type strings (for example
     *     {@code ["text/markdown"]}); never {@code null}
     */
    List<String> getSupportedMimeTypes();

    /**
     * Get the regex pattern that matches file names this parser supports.
     * The pattern is also used to filter files in file open dialogs.
     *
     * @return a regex pattern string (for example
     *     {@code "^.*\\.[mM][dD]$"} for .md files)
     */
    String getFileExtensionPattern();

    /**
     * Get a human-readable description of the supported format.
     *
     * @return a description string (for example
     *     {@code "Markdown Files (*.md)"})
     */
    String getFormatDescription();

    /**
     * Get the charset declared by a MIME type's {@code charset} parameter.
     *
     * @param mimeType a MIME type such as
     *     {@code "text/markdown; charset=ISO-8859-1"}, or {@code null}
     * @param defaultCharset the charset to return when none is declared or
     *     the declared charset is not supported
     * @return the declared charset, or {@code defaultCharset}
     */
    static Charset charsetOf(final String mimeType,
        final Charset defaultCharset) {

        if (mimeType != null) {
            String[] parts = mimeType.split(";");
            for (int i = 1; i < parts.length; i++) {
                String param = parts[i].trim();
                int eq = param.indexOf('=');
                if (eq > 0 && param.substring(0, eq).trim()
                        .toLowerCase(Locale.ROOT).equals("charset")) {
                    String name = param.substring(eq + 1).trim()
                        .replace("\"", "");
                    try {
                        return Charset.forName(name);
                    } catch (IllegalCharsetNameException
                        | UnsupportedCharsetException e) {
                        return defaultCharset;
                    }
                }
            }
        }
        return defaultCharset;
    }

    /**
     * Get the directory of a {@code file:} URL.
     *
     * @param url the URL
     * @return the parent directory, or {@code null} for non-file URLs
     */
    private static Path baseDirectoryOf(final URL url) {
        if (!"file".equalsIgnoreCase(url.getProtocol())) {
            return null;
        }
        try {
            return Path.of(url.toURI()).toAbsolutePath().getParent();
        } catch (URISyntaxException | IllegalArgumentException e) {
            return null;
        }
    }
}
