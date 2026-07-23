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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ColorThemeTest {

    @Test
    void testBoldSetsBoldAttributeOnly() {
        ColorTheme theme = new ColorTheme();
        theme.setColorFromString("test.color", "bold red on blue");

        CellAttributes attr = theme.getColor("test.color");
        assertTrue(attr.isBold());
        assertEquals(Color.RED, attr.getForeColor());
        assertEquals(Color.BLUE, attr.getBackColor());
    }

    @Test
    void testBrightSetsBrightColorDirectly() {
        ColorTheme theme = new ColorTheme();
        theme.setColorFromString("test.color", "bright red on blue");

        CellAttributes attr = theme.getColor("test.color");
        assertFalse(attr.isBold());
        assertEquals(Color.BRIGHT_RED, attr.getForeColor());
        assertEquals(Color.BLUE, attr.getBackColor());
    }

    @Test
    void testBoldAndBrightCombine() {
        ColorTheme theme = new ColorTheme();
        theme.setColorFromString("test.color", "bold bright red on blue");

        CellAttributes attr = theme.getColor("test.color");
        assertTrue(attr.isBold());
        assertEquals(Color.BRIGHT_RED, attr.getForeColor());
    }

    @Test
    void testBlinkWithBright() {
        ColorTheme theme = new ColorTheme();
        theme.setColorFromString("test.color", "bright blink red on blue");

        CellAttributes attr = theme.getColor("test.color");
        assertTrue(attr.isBlink());
        assertEquals(Color.BRIGHT_RED, attr.getForeColor());
    }

    @Test
    void testDarkThemeUsesPaletteColors() {
        ColorTheme theme = new ColorTheme();
        theme.setDarkDefault();

        CellAttributes attr = theme.getColor(ColorTheme.TWINDOW_BORDER);
        assertTrue(attr.isPalette());
        assertTrue(attr.getForeColorPalette() >= 0);
        assertTrue(attr.getBackColorPalette() >= 0);
        assertEquals(-1, attr.getForeColorRGB());
        assertEquals(-1, attr.getBackColorRGB());
    }

    @Test
    void testFemmeThemeUsesRgbColors() {
        ColorTheme theme = new ColorTheme();
        theme.setFemme();

        CellAttributes attr = theme.getColor(ColorTheme.TWINDOW_BACKGROUND);
        assertTrue(attr.getBackColorRGB() >= 0);
        assertEquals(-1, attr.getBackColorPalette());
    }

    @Test
    void testMixedRgbForegroundNamedBackground() {
        ColorTheme theme = new ColorTheme();
        theme.setColorFromString("test.color", "#ffcc00 on blue");

        CellAttributes attr = theme.getColor("test.color");
        assertEquals(0xffcc00, attr.getForeColorRGB());
        assertEquals(-1, attr.getForeColorPalette());
        assertEquals(-1, attr.getBackColorRGB());
        assertEquals(Color.BLUE, attr.getBackColor());
    }

    @Test
    void testMixedNamedForegroundRgbBackground() {
        ColorTheme theme = new ColorTheme();
        theme.setColorFromString("test.color", "red on #112233");

        CellAttributes attr = theme.getColor("test.color");
        assertEquals(Color.RED, attr.getForeColor());
        assertEquals(-1, attr.getForeColorRGB());
        assertEquals(0x112233, attr.getBackColorRGB());
    }

    @Test
    void testExactPaletteRgbLoadsAsPaletteColor() {
        ColorTheme theme = new ColorTheme();
        theme.setColorFromString("test.color", "#ff0000 on #808080");

        CellAttributes attr = theme.getColor("test.color");
        assertEquals(196, attr.getForeColorPalette());
        assertEquals(-1, attr.getForeColorRGB());
        assertEquals(244, attr.getBackColorPalette());
        assertEquals(-1, attr.getBackColorRGB());
    }

    @Test
    void testMixedPaletteForegroundRgbBackground() {
        ColorTheme theme = new ColorTheme();
        CellAttributes attr = CellAttributes.builder()
            .foreColorPalette(220)
            .backColorRGB(0x1e1e1e)
            .build();
        theme.setColor("test.color", attr);

        CellAttributes stored = theme.getColor("test.color");
        assertEquals(220, stored.getForeColorPalette());
        assertEquals(-1, stored.getForeColorRGB());
        assertEquals(0x1e1e1e, stored.getBackColorRGB());
    }

    @Test
    void testMixedNamedForegroundPaletteBackground() {
        ColorTheme theme = new ColorTheme();
        CellAttributes attr = CellAttributes.builder()
            .foreColor(Color.BRIGHT_YELLOW)
            .backColorPalette(236)
            .build();
        theme.setColor("test.color", attr);

        CellAttributes stored = theme.getColor("test.color");
        assertEquals(Color.BRIGHT_YELLOW, stored.getForeColor());
        assertEquals(236, stored.getBackColorPalette());
        assertEquals(-1, stored.getBackColorRGB());
    }

    @Test
    void testBrightBackgroundNamedColor() {
        ColorTheme theme = new ColorTheme();
        theme.setColorFromString("test.color", "white on bright black");

        CellAttributes attr = theme.getColor("test.color");
        assertEquals(Color.WHITE, attr.getForeColor());
        assertEquals(Color.BRIGHT_BLACK, attr.getBackColor());
    }

    @Test
    void testLegacyRgbLineStillLoads() {
        ColorTheme theme = new ColorTheme();
        theme.setColorFromString("test.color", "rgb: #ffcc00 on #112233");

        CellAttributes attr = theme.getColor("test.color");
        assertEquals(0xffcc00, attr.getForeColorRGB());
        assertEquals(0x112233, attr.getBackColorRGB());
    }

    @Test
    void testInvalidLineIsIgnored() {
        ColorTheme theme = new ColorTheme();
        theme.setColorFromString("test.color", "just some text");

        assertNull(theme.getColor("test.color"));
    }

    @Test
    void testSaveConvertsPaletteToRgb(@TempDir Path tempDir) throws IOException {
        ColorTheme theme = new ColorTheme();
        CellAttributes attr = CellAttributes.builder()
            .foreColorPalette(220)
            .backColorPalette(236)
            .build();
        theme.setColor("test.color", attr);

        Path file = tempDir.resolve("theme.dat");
        theme.save(file.toString());
        String content = Files.readString(file);

        // Palette colors must be serialized as their RGB equivalent.
        assertFalse(content.contains("pal:"));
        assertFalse(content.contains("Palette"));
        String expected = "test.color = #%06x on #%06x".formatted(
            Palette256.toRgb(220) & 0xFFFFFF,
            Palette256.toRgb(236) & 0xFFFFFF);
        assertTrue(content.contains(expected),
            "Expected line <" + expected + "> in:\n" + content);
    }

    @Test
    void testSaveLoadRoundTripMixed(@TempDir Path tempDir) throws IOException {
        ColorTheme theme = new ColorTheme();
        theme.setColorFromString("k.rgbOnNamed", "#ffcc00 on blue");
        theme.setColorFromString("k.namedOnRgb", "bold red on #112233");
        theme.setColorFromString("k.brightNamed", "white on bright black");

        Path file = tempDir.resolve("theme.dat");
        theme.save(file.toString());

        ColorTheme loaded = new ColorTheme();
        loaded.load(file.toString());

        CellAttributes a = loaded.getColor("k.rgbOnNamed");
        assertEquals(0xffcc00, a.getForeColorRGB());
        assertEquals(Color.BLUE, a.getBackColor());

        CellAttributes b = loaded.getColor("k.namedOnRgb");
        assertTrue(b.isBold());
        assertEquals(Color.RED, b.getForeColor());
        assertEquals(0x112233, b.getBackColorRGB());

        CellAttributes c = loaded.getColor("k.brightNamed");
        assertEquals(Color.WHITE, c.getForeColor());
        assertEquals(Color.BRIGHT_BLACK, c.getBackColor());
    }

    @Test
    void testSaveLoadRoundTripPaletteBecomesRgb(@TempDir Path tempDir)
        throws IOException {

        ColorTheme theme = new ColorTheme();
        CellAttributes attr = CellAttributes.builder()
            .foreColorPalette(220)
            .backColorPalette(236)
            .build();
        theme.setColor("k.palette", attr);

        Path file = tempDir.resolve("theme.dat");
        theme.save(file.toString());

        ColorTheme loaded = new ColorTheme();
        loaded.load(file.toString());

        CellAttributes a = loaded.getColor("k.palette");
        assertEquals(220, a.getForeColorPalette());
        assertEquals(236, a.getBackColorPalette());
        assertEquals(-1, a.getForeColorRGB());
        assertEquals(-1, a.getBackColorRGB());
    }

    // -------------------------------------------------------------------
    // isDarkTheme()
    // -------------------------------------------------------------------

    @Test
    void testIsDarkThemeDefaultThemeIsDark() {
        ColorTheme theme = new ColorTheme();
        // Default theme is white text on a blue background.
        assertTrue(theme.isDarkTheme());
    }

    @Test
    void testIsDarkThemeDetectsLightBackground() {
        ColorTheme theme = new ColorTheme();
        theme.setColorFromString(ColorTheme.TLABEL, "black on white");
        assertFalse(theme.isDarkTheme());
    }

    @Test
    void testIsDarkThemeDetectsDarkBackground() {
        ColorTheme theme = new ColorTheme();
        theme.setColorFromString(ColorTheme.TLABEL, "white on black");
        assertTrue(theme.isDarkTheme());
    }

    @Test
    void testIsDarkThemeUsesRgbColors() {
        ColorTheme theme = new ColorTheme();
        theme.setColorFromString(ColorTheme.TLABEL, "#eeeeee on #111111");
        assertTrue(theme.isDarkTheme());

        theme.setColorFromString(ColorTheme.TLABEL, "#111111 on #eeeeee");
        assertFalse(theme.isDarkTheme());
    }

    @Test
    void testIsDarkThemeUsesPaletteColors() {
        ColorTheme theme = new ColorTheme();
        CellAttributes attr = CellAttributes.builder()
            .foreColorPalette(15)
            .backColorPalette(0)
            .build();
        theme.setColor(ColorTheme.TLABEL, attr);
        assertTrue(theme.isDarkTheme());

        attr = CellAttributes.builder()
            .foreColorPalette(0)
            .backColorPalette(15)
            .build();
        theme.setColor(ColorTheme.TLABEL, attr);
        assertFalse(theme.isDarkTheme());
    }
}
