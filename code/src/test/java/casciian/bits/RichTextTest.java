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
package casciian.bits;

import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RichText} and its {@link RichText.Builder}.
 */
class RichTextTest {

    // -----------------------------------------------------------------------
    // Builder basics
    // -----------------------------------------------------------------------

    @Test
    void testEmptyBuilder() {
        RichText text = RichText.builder().build();
        assertTrue(text.getRuns().isEmpty());
        assertTrue(text.isEmpty());
        assertEquals("", text.getPlainText());
    }

    @Test
    void testAppendPlainText() {
        RichText text = RichText.builder()
            .append("Hello ")
            .append("world")
            .build();
        // Same (default) attributes so runs are merged.
        assertEquals(1, text.getRuns().size());
        assertEquals("Hello world", text.getPlainText());
    }

    @Test
    void testAppendEmptyIgnored() {
        RichText text = RichText.builder()
            .append("")
            .append(null)
            .append("x")
            .build();
        assertEquals(1, text.getRuns().size());
        assertEquals("x", text.getPlainText());
    }

    @Test
    void testAdjacentEqualAttributesMerged() {
        CellAttributes bold = CellAttributes.builder().bold(true).build();
        RichText text = RichText.builder()
            .append("a", bold)
            .append("b", bold)
            .build();
        assertEquals(1, text.getRuns().size());
        assertEquals("ab", text.getRuns().get(0).getText());
        assertTrue(text.getRuns().get(0).getAttributes().isBold());
    }

    @Test
    void testDifferentAttributesNotMerged() {
        CellAttributes bold = CellAttributes.builder().bold(true).build();
        RichText text = RichText.builder()
            .append("a")
            .append("b", bold)
            .build();
        assertEquals(2, text.getRuns().size());
        assertFalse(text.getRuns().get(0).getAttributes().isBold());
        assertTrue(text.getRuns().get(1).getAttributes().isBold());
    }

    // -----------------------------------------------------------------------
    // Immutability / defensive copies
    // -----------------------------------------------------------------------

    @Test
    void testRunsListIsReadOnly() {
        RichText text = RichText.builder().append("x").build();
        assertThrows(UnsupportedOperationException.class,
            () -> text.getRuns().clear());
    }

    @Test
    void testAttributesDefensivelyCopiedIn() {
        CellAttributes attr = CellAttributes.builder().bold(true).build();
        RichText text = RichText.builder().append("x", attr).build();
        // Mutating the source after building must not affect the run.
        attr.setBold(false);
        assertTrue(text.getRuns().get(0).getAttributes().isBold());
    }

    @Test
    void testAttributesDefensivelyCopiedOut() {
        RichText text = RichText.builder()
            .append("x", CellAttributes.builder().bold(true).build())
            .build();
        RichText.Run run = text.getRuns().get(0);
        run.getAttributes().setBold(false);
        // The run's own attributes are unchanged.
        assertTrue(run.getAttributes().isBold());
    }

    // -----------------------------------------------------------------------
    // Hyperlinks
    // -----------------------------------------------------------------------

    @Test
    void testLink() {
        RichText text = RichText.builder()
            .link("Casciian repository",
                "https://github.com/crramirez/casciian")
            .build();
        assertEquals(1, text.getRuns().size());
        CellAttributes attr = text.getRuns().get(0).getAttributes();
        assertEquals("https://github.com/crramirez/casciian",
            attr.getHyperlink());
        assertTrue(attr.isHyperlink());
    }

    // -----------------------------------------------------------------------
    // Nested temporary styles
    // -----------------------------------------------------------------------

    @Test
    void testPushPopScopes() {
        CellAttributes bold = CellAttributes.builder().bold(true).build();
        RichText text = RichText.builder()
            .append("normal ")
            .push(bold)
            .append("bold")
            .pop()
            .append(" normal")
            .build();
        List<RichText.Run> runs = text.getRuns();
        assertEquals(3, runs.size());
        assertFalse(runs.get(0).getAttributes().isBold());
        assertTrue(runs.get(1).getAttributes().isBold());
        assertFalse(runs.get(2).getAttributes().isBold());
    }

    @Test
    void testPopOnDefaultScopeIsSafe() {
        RichText text = RichText.builder()
            .pop().pop()
            .append("x")
            .build();
        assertEquals("x", text.getPlainText());
    }

    // -----------------------------------------------------------------------
    // Layout (RichText -> Cells) via RichTextLayout.layout
    // -----------------------------------------------------------------------

    @Test
    void testLayoutWraps() {
        RichText text = RichText.builder().append("ABCDE").build();
        List<RichTextLayout.Line> lines = RichTextLayout.layout(text, 3);
        assertEquals(2, lines.size());
        assertEquals("ABC", cellsToString(lines.get(0)));
        assertEquals("DE", cellsToString(lines.get(1)));
    }

    @Test
    void testLayoutCarriesAttributes() {
        CellAttributes bold = CellAttributes.builder().bold(true).build();
        RichText text = RichText.builder()
            .append("a")
            .append("b", bold)
            .build();
        List<RichTextLayout.Line> lines = RichTextLayout.layout(text, 80);
        List<Cell> cells = lines.get(0).getCells();
        assertFalse(cells.get(0).isBold());
        assertTrue(cells.get(1).isBold());
    }

    @Test
    void testLayoutNewlinesInRunText() {
        RichText text = RichText.builder().append("a\nb").build();
        List<RichTextLayout.Line> lines = RichTextLayout.layout(text, 80);
        assertEquals(2, lines.size());
        assertEquals("a", cellsToString(lines.get(0)));
        assertEquals("b", cellsToString(lines.get(1)));
    }

    @Test
    void testLayoutEmptyAndInvalidInputs() {
        RichText empty = RichText.builder().build();
        assertThat(RichTextLayout.layout(null, 4)).isEmpty();
        assertThat(RichTextLayout.layout(null, 4, run -> null)).isEmpty();
        for (int width : new int[] {0, -1}) {
            assertThat(RichTextLayout.layout(empty, width)).isEmpty();
            assertThat(RichTextLayout.layout(empty, width, run -> null))
                .isEmpty();
        }
        List<RichTextLayout.Line> lines = RichTextLayout.layout(empty, 4);
        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst().getWidth()).isZero();
    }

    @Test
    void testLayoutControlCharactersAcrossStyledRuns() {
        RichText text = RichText.builder().append("abc\t")
            .append("\bX\rY\n", CellAttributes.builder().bold(true).build())
            .build();
        List<RichTextLayout.Line> lines = RichTextLayout.layout(text, 4);
        assertThat(lines).extracting(RichTextTest::cellsToString)
            .containsExactly("YbcX", "");
        assertThat(lines.getFirst().getWidth()).isEqualTo(4);
        assertThat(lines.getFirst().getCells().getFirst().isBold()).isTrue();
        assertThat(lines.getFirst().getCells().get(1).isBold()).isFalse();
        assertThat(lines.getFirst().getCells().getLast().isBold()).isTrue();
    }

    @Test
    void testLayoutWideCharactersAtWidthBoundary() {
        RichText text = RichText.builder().append("a界b").build();
        assertThat(RichTextLayout.layout(text, 2))
            .extracting(RichTextTest::cellsToString)
            .containsExactly("a", "界 ", "b");
        assertThat(RichTextLayout.layout(text, 1))
            .extracting(RichTextTest::cellsToString)
            .containsExactly("a", "界", "b");
    }

    @Test
    void testAnsiParseRetainsCompatibleLines() {
        String source = "ab\033[1;31m界\tX\rY\n";
        List<AnsiParser.Line> parsed = AnsiParser.parse(source, 4);
        List<RichTextLayout.Line> laidOut =
            RichTextLayout.layout(AnsiParser.toRichText(source), 4);
        assertThat(parsed).hasSameSizeAs(laidOut);
        for (int i = 0; i < parsed.size(); i++) {
            assertThat(parsed.get(i).getWidth())
                .isEqualTo(laidOut.get(i).getWidth());
            assertThat(parsed.get(i).getCells())
                .containsExactlyElementsOf(laidOut.get(i).getCells());
        }
    }

    // -----------------------------------------------------------------------
    // equals / hashCode
    // -----------------------------------------------------------------------

    @Test
    void testEqualsAndHashCode() {
        RichText a = RichText.builder().append("x").build();
        RichText b = RichText.builder().append("x").build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String cellsToString(final RichTextLayout.Line line) {
        StringBuilder sb = new StringBuilder();
        for (Cell cell : line.getCells()) {
            sb.appendCodePoint(cell.getChar());
        }
        return sb.toString();
    }
}
