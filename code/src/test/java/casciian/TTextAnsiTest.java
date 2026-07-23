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
package casciian;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import casciian.bits.CellAttributes;
import casciian.bits.Color;

/**
 * Tests for {@link TTextAnsi}.
 */
class TTextAnsiTest {

    // -----------------------------------------------------------------------
    // Constructor and basic properties
    // -----------------------------------------------------------------------

    @Test
    void testConstructorSetsText() {
        TTextAnsi widget = new TTextAnsi(null, "Hello", 0, 0, 40, 10);
        assertEquals("Hello", widget.getText());
    }

    @Test
    void testSetText() {
        TTextAnsi widget = new TTextAnsi(null, "original", 0, 0, 40, 10);
        widget.setText("updated");
        assertEquals("updated", widget.getText());
    }

    @Test
    void testGetText() {
        TTextAnsi widget = new TTextAnsi(null, "test data", 0, 0, 40, 10);
        assertEquals("test data", widget.getText());
    }

    @Test
    void testAppendText() {
        TTextAnsi widget = new TTextAnsi(null, "Hello", 0, 0, 40, 10);
        widget.appendText(" World");
        assertEquals("Hello World", widget.getText());
    }

    @Test
    void testAppendTextToEmpty() {
        TTextAnsi widget = new TTextAnsi(null, "", 0, 0, 40, 10);
        widget.appendText("Content");
        assertEquals("Content", widget.getText());
    }

    @Test
    void testAppendTextToNull() {
        TTextAnsi widget = new TTextAnsi(null, null, 0, 0, 40, 10);
        widget.appendText("Content");
        assertEquals("Content", widget.getText());
    }

    // -----------------------------------------------------------------------
    // Widget dimensions
    // -----------------------------------------------------------------------

    @Test
    void testWidgetDimensions() {
        TTextAnsi widget = new TTextAnsi(null, "text", 5, 3, 40, 10);
        assertEquals(40, widget.getWidth());
        assertEquals(10, widget.getHeight());
        assertEquals(5, widget.getX());
        assertEquals(3, widget.getY());
    }

    // -----------------------------------------------------------------------
    // ANSI content handling
    // -----------------------------------------------------------------------

    @Test
    void testAnsiContentPreservedInGetText() {
        String ansiText = "\033[1;31mBold Red\033[0m Normal";
        TTextAnsi widget = new TTextAnsi(null, ansiText, 0, 0, 40, 10);
        assertEquals(ansiText, widget.getText());
    }

    @Test
    void testSetTextReflowsData() {
        TTextAnsi widget = new TTextAnsi(null, "Short", 0, 0, 40, 10);
        // Set a longer text; reflowData should be called internally
        String longText = "This is a much longer text that should "
            + "trigger wrapping at the widget width boundary";
        widget.setText(longText);
        assertEquals(longText, widget.getText());
    }

    // -----------------------------------------------------------------------
    // setWidth / setHeight
    // -----------------------------------------------------------------------

    @Test
    void testSetWidthUpdatesWidth() {
        TTextAnsi widget = new TTextAnsi(null, "text", 0, 0, 40, 10);
        widget.setWidth(60);
        assertEquals(60, widget.getWidth());
    }

    @Test
    void testSetHeightUpdatesHeight() {
        TTextAnsi widget = new TTextAnsi(null, "text", 0, 0, 40, 10);
        widget.setHeight(20);
        assertEquals(20, widget.getHeight());
    }

    // -----------------------------------------------------------------------
    // applyBoldAsBright(): treat bold as bright on dark themes
    // -----------------------------------------------------------------------

    @Test
    void testApplyBoldAsBrightBrightensOnDarkTheme() {
        CellAttributes attrs = new CellAttributes();
        attrs.setForeColor(Color.RED);
        attrs.setBold(true);

        TTextAnsi.applyBoldAsBright(attrs, true);

        assertEquals(Color.BRIGHT_RED, attrs.getForeColor());
        assertFalse(attrs.isBold());
    }

    @Test
    void testApplyBoldAsBrightLeavesLightThemeUnchanged() {
        CellAttributes attrs = new CellAttributes();
        attrs.setForeColor(Color.RED);
        attrs.setBold(true);

        TTextAnsi.applyBoldAsBright(attrs, false);

        assertEquals(Color.RED, attrs.getForeColor());
        assertTrue(attrs.isBold());
    }

    @Test
    void testApplyBoldAsBrightIgnoresNonBoldCells() {
        CellAttributes attrs = new CellAttributes();
        attrs.setForeColor(Color.RED);
        attrs.setBold(false);

        TTextAnsi.applyBoldAsBright(attrs, true);

        assertEquals(Color.RED, attrs.getForeColor());
        assertFalse(attrs.isBold());
    }

    @Test
    void testApplyBoldAsBrightIgnoresRgbForeground() {
        CellAttributes attrs = new CellAttributes();
        attrs.setForeColorRGB(0xff0000);
        attrs.setBold(true);

        TTextAnsi.applyBoldAsBright(attrs, true);

        assertEquals(0xff0000, attrs.getForeColorRGB());
        assertTrue(attrs.isBold());
    }

    @Test
    void testApplyBoldAsBrightIgnoresPaletteForeground() {
        CellAttributes attrs = new CellAttributes();
        attrs.setForeColorPalette(196);
        attrs.setBold(true);

        TTextAnsi.applyBoldAsBright(attrs, true);

        assertEquals(196, attrs.getForeColorPalette());
        assertTrue(attrs.isBold());
    }
}
