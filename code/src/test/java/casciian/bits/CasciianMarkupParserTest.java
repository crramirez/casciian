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
    // Serialization
    // -----------------------------------------------------------------------

    @Test
    void testToMarkupEmptyAndPlainText() {
        assertEquals("", CasciianMarkupParser.toMarkup(null));
        assertEquals("", CasciianMarkupParser.toMarkup(RichText.builder().build()));
        String plain = "literal [bold] [/], [[, ] \"quotes\" \\ path\n日本語 😀";
        RichText text = RichText.builder().append(plain).build();
        assertEquals(plain.replace("[", "[["),
            CasciianMarkupParser.toMarkup(text));
        assertMarkupRoundTrip(text);
    }

    @Test
    void testToMarkupBooleanStyles() {
        for (String token : new String[] {
                "bold", "faint", "italic", "blink", "reverse", "hidden",
                "strike", "dim", "strikethrough"}) {
            assertMarkupRoundTrip(CasciianMarkupParser.parse(
                "[" + token + "]styled[/]plain"));
        }
        CellAttributes attr = CellAttributes.builder()
            .bold(true).faint(true).italic(true).blink(true)
            .reverse(true).hidden(true).strikethrough(true).build();
        RichText text = RichText.builder().append("all", attr).append("plain")
            .build();
        assertMarkupRoundTrip(text);
        assertEquals("[bold faint italic blink reverse hidden strike "
            + "fg=white bg=black]all[/]plain",
            CasciianMarkupParser.toMarkup(text));
    }

    @Test
    void testToMarkupUnderlineStylesWithAndWithoutLinks() {
        for (String style : new String[] {
                "none", "single", "double", "curly", "dotted", "dashed"}) {
            for (String link : new String[] {"", "link=\"https://example.com\" "}) {
                RichText text = CasciianMarkupParser.parse(
                    "[" + link + "u=" + style + "]x[/]plain");
                assertMarkupRoundTrip(text);
            }
        }
        RichText text = RichText.builder()
            .link("unadorned", "https://example.com", new CellAttributes())
            .link("underlined", "https://example.org")
            .append("plain").build();
        assertMarkupRoundTrip(text);
        assertTrue(CasciianMarkupParser.toMarkup(text)
            .contains("link=\"https://example.com\" u=none"));
    }

    @Test
    void testToMarkupColorChannels() {
        String[] colors = {
            "default", "black", "red", "green", "yellow", "blue", "magenta",
            "cyan", "white", "brightblack", "brightred", "brightgreen",
            "brightyellow", "brightblue", "brightmagenta", "brightcyan",
            "brightwhite", "gray", "grey", "#000000", "#00000f", "#abcdef",
            "#ffffff", "palette:0", "palette:17", "palette:255"
        };
        for (String foreground : colors) {
            for (String background : colors) {
                assertMarkupRoundTrip(CasciianMarkupParser.parse(
                    "[fg=" + foreground + " bg=" + background + "]x[/]plain"));
            }
        }
        RichText explicit = RichText.builder()
            .append("explicit", new CellAttributes()).append("default").build();
        assertEquals("[fg=white bg=black]explicit[/]default",
            CasciianMarkupParser.toMarkup(explicit));
        assertMarkupRoundTrip(explicit);
        RichText rgbPalette = RichText.builder().append("colors",
            CellAttributes.builder().foreColorRGB(0x001234)
                .backColorPalette(255).build()).build();
        assertMarkupRoundTrip(rgbPalette);
        RichText paletteRgb = RichText.builder().append("colors",
            CellAttributes.builder().foreColorPalette(0)
                .backColorRGB(0x00000f).build()).build();
        assertMarkupRoundTrip(paletteRgb);
    }

    @Test
    void testToMarkupFlattensNestedScopesWithoutStyleLeakage() {
        RichText text = CasciianMarkupParser.parse(
            "plain [bold fg=brightred]outer [italic bg=palette:17]"
            + "inner [fg=default bg=#012345 u=dotted]default fg[/]"
            + " inner[/] outer[/] plain");
        assertMarkupRoundTrip(text);
        assertEquals("plain [bold]a[/][bold italic]b[/][bold]c[/] plain",
            CasciianMarkupParser.toMarkup(CasciianMarkupParser.parse(
                "plain [bold]a[italic]b[/]c[/] plain")));
        assertMarkupRoundTrip(CasciianMarkupParser.parse(
            "[fg=red bg=blue]a[fg=default bg=default]b[/]c[/]d"));
    }

    @Test
    void testToMarkupHyperlinksWithQuotedCharacters() {
        String[] links = {
            "https://example.com/a b?q=[bold]&x=\"quoted\"",
            "C:\\folder\\file", "\\\\server\\share", "trailing\\",
            "quote\\\" followed by ] and spaces",
            "\"", "\\", "[]", "spaces\tand\nnewlines"
        };
        for (String link : links) {
            RichText text = RichText.builder()
                .link("literal [link] \"text\"\\", link)
                .append("plain").build();
            assertMarkupRoundTrip(text);
        }
        RichText text = RichText.builder()
            .link("x", "a\"b\\c] d", CellAttributes.builder()
                .defaultColor(true, true).defaultColor(false, true).build())
            .build();
        assertEquals("[link=\"a\\\"b\\\\c] d\" u=none]x[/]",
            CasciianMarkupParser.toMarkup(text));
        assertMarkupRoundTrip(text);
    }

    @Test
    void testQuotedEscapesAndOrdinaryBackslashes() {
        RichText escaped = CasciianMarkupParser.parse(
            "[link=\"a\\\"b\\\\c] d\" bold u=none]x[/]plain");
        CellAttributes attr = escaped.getRuns().get(0).getAttributes();
        assertEquals("a\"b\\c] d", attr.getHyperlink());
        assertTrue(attr.isBold());
        assertEquals(CellAttributes.UNDERLINE_STYLE_NONE, attr.getUnderlineStyle());
        assertEquals("xplain", escaped.getPlainText());
        assertMarkupRoundTrip(escaped);

        RichText quoted = CasciianMarkupParser.parse(
            "[link=\"C:\\folder\\file name\\n\\t\" italic]x[/]");
        assertEquals("C:\\folder\\file name\\n\\t",
            quoted.getRuns().get(0).getAttributes().getHyperlink());
        assertTrue(quoted.getRuns().get(0).getAttributes().isItalic());
        assertMarkupRoundTrip(quoted);

        RichText unquoted = CasciianMarkupParser.parse(
            "[link=C:\\folder\\\\file bold]x[/]");
        assertEquals("C:\\folder\\\\file",
            unquoted.getRuns().get(0).getAttributes().getHyperlink());
        assertTrue(unquoted.getRuns().get(0).getAttributes().isBold());
        assertMarkupRoundTrip(unquoted);
    }

    @Test
    void testToMarkupIgnoresUnsupportedAttributes() {
        CellAttributes attr = new CellAttributes();
        attr.setBold(true);
        attr.setBoldTransparent(true);
        attr.setProtect(true);
        attr.setPulse(true, false, 0);
        RichText text = RichText.builder().append("supported [text]", attr).build();
        String markup = CasciianMarkupParser.toMarkup(text);
        assertEquals("[bold fg=white bg=black]supported [[text][/]", markup);
        RichText parsed = CasciianMarkupParser.parse(markup);
        assertEquals(text.getPlainText(), parsed.getPlainText());
        CellAttributes parsedAttr = parsed.getRuns().get(0).getAttributes();
        assertTrue(parsedAttr.isBold());
        assertFalse(parsedAttr.isBoldTransparent());
        assertFalse(parsedAttr.isProtect());
        assertEquals(0, parsedAttr.getAnimations());
    }

    private static void assertMarkupRoundTrip(final RichText text) {
        String markup = CasciianMarkupParser.toMarkup(text);
        assertEquals(text, CasciianMarkupParser.parse(markup), markup);
        assertEquals(markup, CasciianMarkupParser.toMarkup(
            CasciianMarkupParser.parse(markup)));
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
