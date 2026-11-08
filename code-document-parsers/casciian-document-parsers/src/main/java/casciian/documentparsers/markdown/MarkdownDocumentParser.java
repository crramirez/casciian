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
import casciian.document.parsers.DocumentParser;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * A {@link DocumentParser} for Markdown, built on
 * <a href="https://github.com/commonmark/commonmark-java">commonmark-java</a>.
 *
 * <p>It supports CommonMark plus the GitHub Flavored Markdown tables,
 * strikethrough and task list extensions.  The resulting
 * {@link RichText} uses:</p>
 * <ul>
 *   <li>bold, italic, strikethrough and colored text for inline styles and
 *       headings;</li>
 *   <li>OSC 8 hyperlinks for links;</li>
 *   <li>indentation and bullets for lists, bars for block quotes, solid
 *       panels for code, and box-drawing borders for tables;</li>
 *   <li>image runs for {@code ![alt](path)} and HTML {@code <img>} tags.
 *       Relative image paths are resolved against the base directory (the
 *       folder of the Markdown file when parsing a {@link Path}); the image
 *       files are decoded by the displaying widget through
 *       {@link casciian.image.decoders.ImageDecoderRegistry}.  Remote images
 *       are shown as hyperlinks with their alternative text.</li>
 * </ul>
 *
 * <p>Instances are thread-safe and registered automatically through
 * {@link java.util.ServiceLoader}.</p>
 */
public class MarkdownDocumentParser implements DocumentParser {

    /**
     * MIME types handled by this parser.
     */
    private static final List<String> MIME_TYPES = List.of("text/markdown",
        "text/x-markdown");

    /**
     * The commonmark parser.  Parser instances are thread-safe.
     */
    private final Parser parser;

    /**
     * Public constructor, required by {@link java.util.ServiceLoader}.
     */
    public MarkdownDocumentParser() {
        List<Extension> extensions = List.of(TablesExtension.create(),
            StrikethroughExtension.create(), TaskListItemsExtension.create());
        parser = Parser.builder().extensions(extensions).build();
    }

    /**
     * Parse Markdown from a stream.  The stream is decoded with the charset
     * declared in {@code mimeType}, or UTF-8.
     *
     * @param inputStream the Markdown source; not closed by this method
     * @param mimeType the MIME type, or null
     * @param baseDirectory directory for relative image paths, or null
     * @return the rich text
     * @throws IOException if the stream cannot be read
     */
    @Override
    public RichText parse(final InputStream inputStream, final String mimeType,
        final Path baseDirectory) throws IOException {

        Objects.requireNonNull(inputStream, "inputStream cannot be null");
        String markdown = new String(inputStream.readAllBytes(),
            DocumentParser.charsetOf(mimeType, StandardCharsets.UTF_8));
        return parse(markdown, baseDirectory);
    }

    /**
     * Parse Markdown text.
     *
     * @param markdown the Markdown source
     * @param baseDirectory directory for relative image paths, or null
     * @return the rich text
     */
    public RichText parse(final String markdown, final Path baseDirectory) {
        Objects.requireNonNull(markdown, "markdown cannot be null");
        String source = markdown.startsWith("\ufeff")
            ? markdown.substring(1) : markdown;
        Node document = parser.parse(source);
        return new MarkdownRichTextRenderer(baseDirectory,
            MarkdownRichTextRenderer.defaultAttributes(), false)
            .render(document);
    }

    @Override
    public List<String> getSupportedMimeTypes() {
        return MIME_TYPES;
    }

    @Override
    public String getFileExtensionPattern() {
        return "^.*\\.(?i:md|markdown|mdown|mkdn|mkd)$";
    }

    @Override
    public String getFormatDescription() {
        return "Markdown Files (*.md, *.markdown)";
    }
}
