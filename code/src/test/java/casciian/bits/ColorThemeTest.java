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
}
