/*
 * Casciian Document Parsers add-on
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
package casciian.documentparsers.markdown;

import casciian.bits.RichText;
import casciian.document.parsers.DocumentParserRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Markdown parser is discovered through
 * {@link java.util.ServiceLoader} and located by MIME type and extension.
 */
class MarkdownServiceLoaderTest {

    private final DocumentParserRegistry registry =
        DocumentParserRegistry.getInstance();

    @BeforeEach
    void setUp() {
        registry.clear();
        registry.loadParsers();
    }

    @AfterEach
    void tearDown() {
        registry.clear();
    }

    @Test
    void markdownParserIsFoundByMimeTypeAndExtension() {
        assertThat(registry.findParser("text/markdown; charset=UTF-8"))
            .get().isInstanceOf(MarkdownDocumentParser.class);
        assertThat(registry.findParser("text/x-markdown"))
            .get().isInstanceOf(MarkdownDocumentParser.class);
        for (String name : new String[] { "README.md", "notes.MARKDOWN",
            "a.mkd" }) {
            assertThat(registry.findParser(Path.of(name))).as(name)
                .get().isInstanceOf(MarkdownDocumentParser.class);
        }
        assertThat(registry.findParser(Path.of("notes.txt"))).isEmpty();
    }

    @Test
    void registryParsesAStreamByMimeType() throws IOException {
        RichText text = registry.parse(new ByteArrayInputStream(
            "*hi*".getBytes(StandardCharsets.UTF_8)), "text/markdown");

        assertThat(text.getPlainText()).isEqualTo("hi");
        assertThat(text.getRuns().get(0).getAttributes().isItalic()).isTrue();
    }
}
