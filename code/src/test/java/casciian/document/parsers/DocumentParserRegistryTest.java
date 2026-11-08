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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link DocumentParserRegistry} and the default methods of
 * {@link DocumentParser}.
 */
class DocumentParserRegistryTest {

    private DocumentParserRegistry registry;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        registry = DocumentParserRegistry.getInstance();
        registry.clear();
    }

    @AfterEach
    void tearDown() {
        registry.clear();
    }

    @Test
    void findsParserByMimeTypeIgnoringCaseAndParameters() {
        EchoParser parser = new EchoParser();
        registry.registerParser(parser);

        assertThat(registry.findParser("Text/X-Echo; charset=UTF-8"))
            .containsSame(parser);
        assertThat(registry.findParser("text/plain")).isEmpty();
        assertThat(registry.findParser((String) null)).isEmpty();
    }

    @Test
    void findsParserByFileExtension() {
        EchoParser parser = new EchoParser();
        registry.registerParser(parser);

        assertThat(registry.findParser(Path.of("dir", "notes.ECHO")))
            .containsSame(parser);
        assertThat(registry.findParser(Path.of("notes.txt"))).isEmpty();
        assertThat(registry.getFileExtensionPatterns())
            .containsExactly(parser.getFileExtensionPattern());
    }

    @Test
    void parsesInputStreamByMimeTypeHonoringCharset() throws IOException {
        registry.registerParser(new EchoParser());
        byte[] bytes = "caf\u00e9".getBytes(StandardCharsets.ISO_8859_1);

        RichText text = registry.parse(new ByteArrayInputStream(bytes),
            "text/x-echo; charset=ISO-8859-1");

        assertThat(text.getPlainText()).isEqualTo("caf\u00e9|null");
    }

    @Test
    void parsingFileResolvesBaseDirectoryToTheFileFolder() throws IOException {
        registry.registerParser(new EchoParser());
        Path file = tempDir.resolve("doc.echo");
        Files.writeString(file, "hello");

        RichText fromPath = registry.parse(file);
        RichText fromUrl = registry.parse(file.toUri().toURL());

        String expected = "hello|" + tempDir.toAbsolutePath();
        assertThat(fromPath.getPlainText()).isEqualTo(expected);
        assertThat(fromUrl.getPlainText()).isEqualTo(expected);
    }

    @Test
    void missingParserIsReported() {
        assertThatThrownBy(() -> registry.parse(Path.of("x.unknown")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("x.unknown");
        assertThatThrownBy(() -> registry.parse(
            new ByteArrayInputStream(new byte[0]), "text/unknown"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("text/unknown");
    }

    @Test
    void serviceLoaderDiscoveryRegistersEachProviderOnce() throws IOException {
        Path services = tempDir.resolve("META-INF/services");
        Files.createDirectories(services);
        Files.writeString(services.resolve(DocumentParser.class.getName()),
            EchoParser.class.getName() + "\n");
        try (URLClassLoader loader = new URLClassLoader(
                new URL[] { tempDir.toUri().toURL() },
                getClass().getClassLoader())) {

            assertThat(registry.loadParsers(loader)).isEqualTo(1);
            assertThat(registry.loadParsers(loader)).isZero();
        }
        assertThat(registry.getParsers()).hasSize(1)
            .first().isInstanceOf(EchoParser.class);
        assertThat(registry.unregisterParser(registry.getParsers().get(0)))
            .isTrue();
        assertThat(registry.getParsers()).isEmpty();
    }

    /**
     * Parser that echoes the decoded text followed by "|" and the base
     * directory, so tests can observe how the registry invoked it.
     */
    public static class EchoParser implements DocumentParser {

        @Override
        public RichText parse(final InputStream inputStream,
            final String mimeType, final Path baseDirectory)
            throws IOException {

            String body = new String(inputStream.readAllBytes(),
                DocumentParser.charsetOf(mimeType, StandardCharsets.UTF_8));
            return RichText.builder().append(body + "|" + baseDirectory)
                .build();
        }

        @Override
        public List<String> getSupportedMimeTypes() {
            return List.of("text/x-echo");
        }

        @Override
        public String getFileExtensionPattern() {
            return "^.*\\.[eE][cC][hH][oO]$";
        }

        @Override
        public String getFormatDescription() {
            return "Echo files (*.echo)";
        }
    }
}
