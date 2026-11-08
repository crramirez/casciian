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

import casciian.bits.Cell;
import casciian.bits.CellAttributes;
import casciian.bits.RichText;
import casciian.bits.RichTextLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Black-box tests for {@link MarkdownDocumentParser}: Markdown is parsed into
 * {@link RichText} and checked through its runs and its laid-out lines.
 */
class MarkdownDocumentParserTest {

    private final MarkdownDocumentParser parser = new MarkdownDocumentParser();

    @TempDir
    Path tempDir;

    @Test
    void inlineStylesBecomeCellAttributes() {
        RichText text = parser.parse(
            "**bold** *italic* ~~gone~~ `code` [link](https://example.com)",
            null);

        assertThat(attributesOf(text, "bold").isBold()).isTrue();
        assertThat(attributesOf(text, "italic").isItalic()).isTrue();
        assertThat(attributesOf(text, "gone").isStrikethrough()).isTrue();
        CellAttributes code = attributesOf(text, "code");
        assertThat(code.isDefaultColor(false)).isFalse();
        CellAttributes link = attributesOf(text, "link");
        assertThat(link.getHyperlink()).isEqualTo("https://example.com");
        assertThat(link.isUnderline()).isTrue();
        assertThat(text.getPlainText())
            .isEqualTo("bold italic gone code link");
    }

    @Test
    void headingsAreBoldAndTopLevelsAreUnderlined() {
        RichText text = parser.parse("# Title\n\n## Sub\n\n### Third\n\nBody",
            null);

        assertThat(lines(text)).containsExactly("Title", "\u2550".repeat(5),
            "", "Sub", "\u2500".repeat(3), "", "Third", "", "Body");
        assertThat(attributesOf(text, "Third").isBold()).isTrue();
    }

    @Test
    void lineBreaksFollowMarkdownSemantics() {
        RichText text = parser.parse("soft\nbreak\\\nhard", null);

        assertThat(lines(text)).containsExactly("soft break", "hard");
    }

    @Test
    void nestedAndOrderedListsAreIndented() {
        String markdown = """
            - one
            - two
              - nested
                1. deep
            - [x] done
            - [ ] todo

            3. three
            4. four
            """;

        assertThat(lines(parser.parse(markdown, null))).containsExactly(
            "\u2022 one",
            "\u2022 two",
            "  \u25e6 nested",
            "    1. deep",
            "\u2022 [x] done",
            "\u2022 [ ] todo",
            "",
            "3. three",
            "4. four");
    }

    @Test
    void looseListItemsAreSeparatedByBlankLines() {
        assertThat(lines(parser.parse("- a\n\n- b\n", null)))
            .containsExactly("\u2022 a", "", "\u2022 b");
    }

    @Test
    void blockQuotesArePrefixedWithABar() {
        String markdown = "before\n\n> first\n>\n> second\n\nafter";

        assertThat(lines(parser.parse(markdown, null))).containsExactly(
            "before", "", "\u2502 first", "\u2502", "\u2502 second", "",
            "after");
    }

    @Test
    void codeBlocksArePaddedPanelsWithExpandedTabs() {
        RichText text = parser.parse("```\nab\n\tc\n```", null);

        assertThat(lines(text)).containsExactly(" ab    ", "     c ");
    }

    @Test
    void tablesAreDrawnWithBordersAndAlignment() {
        String markdown = """
            | Name | Qty | Mid |
            |:-----|----:|:---:|
            | apple | 3 | x |
            | kiwi | 12 | yy |
            """;

        RichText text = parser.parse(markdown, null);

        assertThat(lines(text)).containsExactly(
            "\u250c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u252c"
                + "\u2500\u2500\u2500\u2500\u2500\u252c"
                + "\u2500\u2500\u2500\u2500\u2500\u2510",
            "\u2502 Name  \u2502 Qty \u2502 Mid \u2502",
            "\u251c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253c"
                + "\u2500\u2500\u2500\u2500\u2500\u253c"
                + "\u2500\u2500\u2500\u2500\u2500\u2524",
            "\u2502 apple \u2502   3 \u2502  x  \u2502",
            "\u2502 kiwi  \u2502  12 \u2502 yy  \u2502",
            "\u2514\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2534"
                + "\u2500\u2500\u2500\u2500\u2500\u2534"
                + "\u2500\u2500\u2500\u2500\u2500\u2518");
        assertThat(attributesOf(text, "Name").isBold()).isTrue();
        assertThat(attributesOf(text, "apple").isBold()).isFalse();
    }

    @Test
    void imagesAreResolvedAgainstTheBaseDirectory() {
        Path base = tempDir.toAbsolutePath();
        RichText text = parser.parse("Intro\n\n"
            + "![The logo](img/my%20logo.png \"title\")\n\n"
            + "![](/abs/pic.png)\n\nAfter", base);

        assertThat(images(text)).containsExactly(
            base.resolve("img/my logo.png").toString(),
            Path.of("/abs/pic.png").toString());
        assertThat(text.getRuns().stream().filter(RichText.Run::isImage)
            .map(RichText.Run::getText))
            .containsExactly("[The logo]", "[pic.png]");
        assertThat(linesWithImages(text)).containsExactly(
            "Intro", "", "<image>", "", "<image>", "", "After");
    }

    @Test
    void htmlImagesAreSupportedAndOtherTagsDropped() {
        RichText text = parser.parse(
            "<p align=\"center\"><img src='badge.png' alt=\"Badge\"> "
                + "a &amp; b<br>c</p>", Path.of("/docs"));

        assertThat(images(text)).containsExactly(
            Path.of("/docs/badge.png").toString());
        assertThat(linesWithImages(text))
            .containsExactly("<image>", "a & b", "c");
    }

    @Test
    void remoteImagesBecomeHyperlinkedAlternativeText() {
        RichText text = parser.parse("![chart](https://example.com/c.png)",
            null);

        assertThat(images(text)).isEmpty();
        assertThat(text.getPlainText()).isEqualTo("[chart]");
        assertThat(attributesOf(text, "[chart]").getHyperlink())
            .isEqualTo("https://example.com/c.png");
    }

    @Test
    void parsingAFileResolvesImagesNextToIt() throws IOException {
        Path docs = Files.createDirectories(tempDir.resolve("docs"));
        Path file = docs.resolve("README.md");
        Files.writeString(file, "# Doc\n\n![shot](shot.png)\n");

        RichText text = parser.parse(file);

        assertThat(images(text)).containsExactly(
            docs.toAbsolutePath().resolve("shot.png").toString());
    }

    @Test
    void streamsHonorTheMimeTypeCharset() throws IOException {
        byte[] latin1 = "caf\u00e9".getBytes(StandardCharsets.ISO_8859_1);

        RichText text = parser.parse(new ByteArrayInputStream(latin1),
            "text/markdown; charset=ISO-8859-1");

        assertThat(text.getPlainText()).isEqualTo("caf\u00e9");
    }

    @Test
    void leadingByteOrderMarkIsIgnored() {
        assertThat(lines(parser.parse("\ufeff# T", null)))
            .containsExactly("T", "\u2550");
    }

    // ------------------------------------------------------------------------

    private static CellAttributes attributesOf(final RichText text,
        final String fragment) {

        return text.getRuns().stream()
            .filter(run -> run.getText().contains(fragment))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No run with " + fragment))
            .getAttributes();
    }

    private static List<String> images(final RichText text) {
        return text.getRuns().stream().filter(RichText.Run::isImage)
            .map(RichText.Run::getImagePath).toList();
    }

    private static List<String> lines(final RichText text) {
        return toStrings(RichTextLayout.layout(text, 80));
    }

    private static List<String> linesWithImages(final RichText text) {
        return toStrings(RichTextLayout.layout(text, 80, run -> {
            List<Cell> row = new ArrayList<>();
            for (char ch : "<image>".toCharArray()) {
                row.add(new Cell(ch));
            }
            return List.of(row);
        }));
    }

    private static List<String> toStrings(
        final List<RichTextLayout.Line> layout) {

        List<String> result = new ArrayList<>();
        for (RichTextLayout.Line line : layout) {
            StringBuilder sb = new StringBuilder();
            for (Cell cell : line.getCells()) {
                sb.appendCodePoint(cell.getChar());
            }
            result.add(sb.toString());
        }
        return result;
    }
}
