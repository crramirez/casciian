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

/**
 * Tests for {@link CasciianMarkupParser}.
 */
class CasciianMarkupParserTest {

    // -----------------------------------------------------------------------
    // Edge cases
    // -----------------------------------------------------------------------

    @Test
    void testNull() {
        assertTrue(CasciianMarkupParser.parse(null).isEmpty());
    }

    @Test
    void testPlainText() {
        RichText text = CasciianMarkupParser.parse("Normal text");
        assertEquals(1, text.getRuns().size());
        assertEquals("Normal text", text.getPlainText());
        assertFalse(text.getRuns().get(0).getAttributes().isBold());
    }

    // -----------------------------------------------------------------------
    // Basic attributes
    // -----------------------------------------------------------------------

    @Test
    void testBold() {
        RichText text = CasciianMarkupParser.parse("[bold]bold text[/]");
        assertEquals(1, text.getRuns().size());
        assertEquals("bold text", text.getRuns().get(0).getText());
        assertTrue(text.getRuns().get(0).getAttributes().isBold());
    }

    @Test
    void testAllBooleanAttributes() {
        RichText text = CasciianMarkupParser.parse(
            "[bold faint italic blink reverse hidden strike]x[/]");
        CellAttributes a = text.getRuns().get(0).getAttributes();
        assertTrue(a.isBold());
        assertTrue(a.isFaint());
        assertTrue(a.isItalic());
        assertTrue(a.isBlink());
        assertTrue(a.isReverse());
        assertTrue(a.isHidden());
        assertTrue(a.isStrikethrough());
    }

    @Test
    void testStrikethroughAlias() {
        RichText text = CasciianMarkupParser.parse("[strikethrough]x[/]");
        assertTrue(text.getRuns().get(0).getAttributes().isStrikethrough());
    }

    @Test
    void testDimAliasForFaint() {
        RichText text = CasciianMarkupParser.parse("[dim]x[/]");
        assertTrue(text.getRuns().get(0).getAttributes().isFaint());
    }

    @Test
    void testMultipleAttributesInOneTag() {
        RichText text = CasciianMarkupParser.parse(
            "[bold italic fg=#ffcc00]Multiple attributes[/]");
        CellAttributes a = text.getRuns().get(0).getAttributes();
        assertTrue(a.isBold());
        assertTrue(a.isItalic());
        assertEquals(0xffcc00, a.getForeColorRGB());
    }

    // -----------------------------------------------------------------------
    // Colors
    // -----------------------------------------------------------------------

    @Test
    void testRgbForeground() {
        RichText text = CasciianMarkupParser.parse("[fg=#ff0000]red[/]");
        CellAttributes a = text.getRuns().get(0).getAttributes();
        assertEquals(0xff0000, a.getForeColorRGB());
        assertFalse(a.isDefaultColor(true));
    }

    @Test
    void testRgbBackground() {
        RichText text = CasciianMarkupParser.parse("[bg=#202020]x[/]");
        CellAttributes a = text.getRuns().get(0).getAttributes();
        assertEquals(0x202020, a.getBackColorRGB());
        assertFalse(a.isDefaultColor(false));
    }

    @Test
    void testPaletteColors() {
        RichText text = CasciianMarkupParser.parse(
            "[fg=palette:214 bg=palette:17]x[/]");
        CellAttributes a = text.getRuns().get(0).getAttributes();
        assertEquals(214, a.getForeColorPalette());
        assertEquals(17, a.getBackColorPalette());
    }

    @Test
    void testNamedColor() {
        RichText text = CasciianMarkupParser.parse("[fg=green]x[/]");
        CellAttributes a = text.getRuns().get(0).getAttributes();
        assertEquals(Color.GREEN, a.getForeColor());
        assertFalse(a.isDefaultColor(true));
    }

    @Test
    void testDefaultColorKeyword() {
        RichText text = CasciianMarkupParser.parse(
            "[fg=red]a[fg=default]b[/][/]");
        // Second run resets foreground to default.
        CellAttributes b = text.getRuns().get(1).getAttributes();
        assertTrue(b.isDefaultColor(true));
        assertEquals(Color.WHITE, b.getForeColor());
        assertEquals(-1, b.getForeColorRGB());
        assertEquals(-1, b.getForeColorPalette());
    }

    @Test
    void testDefaultBackgroundColorKeyword() {
        RichText text = CasciianMarkupParser.parse(
            "[bg=#123456]a[bg=default]b[/][/]");
        CellAttributes b = text.getRuns().get(1).getAttributes();
        assertTrue(b.isDefaultColor(false));
        assertEquals(Color.BLACK, b.getBackColor());
        assertEquals(-1, b.getBackColorRGB());
        assertEquals(-1, b.getBackColorPalette());
    }

    @Test
    void testInvalidHexIgnored() {
        RichText text = CasciianMarkupParser.parse("[fg=#zzzzzz]x[/]");
        CellAttributes a = text.getRuns().get(0).getAttributes();
        assertTrue(a.isDefaultColor(true));
    }

    // -----------------------------------------------------------------------
    // Underline styles
    // -----------------------------------------------------------------------

    @Test
    void testUnderlineShorthand() {
        RichText text = CasciianMarkupParser.parse("[u]x[/]");
        assertEquals(CellAttributes.UNDERLINE_STYLE_SINGLE,
            text.getRuns().get(0).getAttributes().getUnderlineStyle());
    }

    @Test
    void testCurlyUnderline() {
        RichText text = CasciianMarkupParser.parse("[u=curly]Curly[/]");
        assertEquals(CellAttributes.UNDERLINE_STYLE_CURLY,
            text.getRuns().get(0).getAttributes().getUnderlineStyle());
    }

    @Test
    void testUnderlineStyleVariants() {
        assertEquals(CellAttributes.UNDERLINE_STYLE_DOUBLE, styleOf("double"));
        assertEquals(CellAttributes.UNDERLINE_STYLE_DOTTED, styleOf("dotted"));
        assertEquals(CellAttributes.UNDERLINE_STYLE_DASHED, styleOf("dashed"));
        assertEquals(CellAttributes.UNDERLINE_STYLE_NONE, styleOf("none"));
    }

    private static int styleOf(final String name) {
        RichText text = CasciianMarkupParser.parse("[u=" + name + "]x[/]");
        return text.getRuns().get(0).getAttributes().getUnderlineStyle();
    }

    // -----------------------------------------------------------------------
    // Links
    // -----------------------------------------------------------------------

    @Test
    void testLink() {
        RichText text = CasciianMarkupParser.parse(
            "[link=\"https://github.com/crramirez/casciian\"]"
            + "Casciian repository[/]");
        CellAttributes a = text.getRuns().get(0).getAttributes();
        assertEquals("https://github.com/crramirez/casciian",
            a.getHyperlink());
        assertEquals("Casciian repository", text.getRuns().get(0).getText());
    }

    // -----------------------------------------------------------------------
    // Nesting
    // -----------------------------------------------------------------------

    @Test
    void testNesting() {
        RichText text = CasciianMarkupParser.parse(
            "Normal [bold]bold [fg=#ff0000]red[/] bold[/] normal");
        List<RichText.Run> runs = text.getRuns();
        assertEquals("Normal ", runs.get(0).getText());
        assertFalse(runs.get(0).getAttributes().isBold());

        assertEquals("bold ", runs.get(1).getText());
        assertTrue(runs.get(1).getAttributes().isBold());
        assertTrue(runs.get(1).getAttributes().isDefaultColor(true));

        assertEquals("red", runs.get(2).getText());
        assertTrue(runs.get(2).getAttributes().isBold());
        assertEquals(0xff0000, runs.get(2).getAttributes().getForeColorRGB());

        assertEquals(" bold", runs.get(3).getText());
        assertTrue(runs.get(3).getAttributes().isBold());
        assertTrue(runs.get(3).getAttributes().isDefaultColor(true));

        assertEquals(" normal", runs.get(4).getText());
        assertFalse(runs.get(4).getAttributes().isBold());
    }

    @Test
    void testShorthandClosesMostRecentScope() {
        RichText text = CasciianMarkupParser.parse(
            "[bold]a[italic]b[/]c[/]");
        List<RichText.Run> runs = text.getRuns();
        // a: bold
        assertTrue(runs.get(0).getAttributes().isBold());
        assertFalse(runs.get(0).getAttributes().isItalic());
        // b: bold + italic
        assertTrue(runs.get(1).getAttributes().isBold());
        assertTrue(runs.get(1).getAttributes().isItalic());
        // c: bold (italic closed)
        assertTrue(runs.get(2).getAttributes().isBold());
        assertFalse(runs.get(2).getAttributes().isItalic());
    }

    @Test
    void testExplicitClosingTag() {
        RichText text = CasciianMarkupParser.parse("[bold]a[/bold]b");
        assertTrue(text.getRuns().get(0).getAttributes().isBold());
        assertFalse(text.getRuns().get(1).getAttributes().isBold());
    }

    @Test
    void testUnbalancedCloseIsSafe() {
        RichText text = CasciianMarkupParser.parse("a[/]b");
        assertEquals("ab", text.getPlainText());
    }

    // -----------------------------------------------------------------------
    // Escaping
    // -----------------------------------------------------------------------

    @Test
    void testEscapedBracket() {
        RichText text = CasciianMarkupParser.parse("[[bold]");
        assertEquals("[bold]", text.getPlainText());
        assertFalse(text.getRuns().get(0).getAttributes().isBold());
    }

    @Test
    void testUnterminatedTagIsLiteral() {
        RichText text = CasciianMarkupParser.parse("a[bold");
        assertEquals("a[bold", text.getPlainText());
    }

    // -----------------------------------------------------------------------
    // Layout integration
    // -----------------------------------------------------------------------

    @Test
    void testLayoutOfMarkup() {
        RichText text = CasciianMarkupParser.parse("[bold]AB[/]CD");
        List<AnsiParser.Line> lines = AnsiParser.layout(text, 80);
        List<Cell> cells = lines.get(0).getCells();
        assertTrue(cells.get(0).isBold());
        assertTrue(cells.get(1).isBold());
        assertFalse(cells.get(2).isBold());
    }
}
