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

import casciian.backend.HeadlessBackend;
import casciian.backend.Screen;
import casciian.bits.CellAttributes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rendering tests for the text widgets: what ends up on the screen when a
 * TField, a TText or a TEditor is drawn.
 */
class TTextBaseRenderTest {

    /**
     * The application under test.
     */
    private TApplication application;

    /**
     * The window holding the widget under test.
     */
    private TWindow window;

    /**
     * Create a headless application and a window to draw into.
     */
    private TWindow makeWindow() {
        application = new TApplication(new HeadlessBackend());
        window = new TWindow(application, "test", 0, 0, 40, 10);
        return window;
    }

    /**
     * Draw a widget onto the screen.
     *
     * @param widget the widget to draw
     */
    private void drawWidget(final TWidget widget) {
        Screen screen = application.getScreen();
        screen.clearPhysical();
        widget.drawChildren();
    }

    /**
     * Read a row of the screen as a string.
     *
     * @param x the starting column, absolute
     * @param y the row, absolute
     * @param width the number of cells to read
     * @return the text of those cells
     */
    private String screenText(final int x, final int y, final int width) {
        Screen screen = application.getScreen();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width; i++) {
            sb.append(Character.toChars(screen.getCharXY(x + i, y).getChar()));
        }
        return sb.toString();
    }

    /**
     * Read the attributes of one screen cell.
     *
     * @param x the column, absolute
     * @param y the row, absolute
     * @return the cell attributes
     */
    private CellAttributes screenAttr(final int x, final int y) {
        return application.getScreen().getAttrXY(x, y);
    }

    @Test
    void fieldDrawsItsTextWithoutHatch() {
        TWindow window = makeWindow();
        TField field = new TField(window, 1, 1, 10, false, "abc");

        drawWidget(field);

        String row = screenText(field.getAbsoluteX(), field.getAbsoluteY(), 10);
        assertTrue(row.contains("abc"), "field text not drawn: '" + row + "'");
        assertEquals(-1, row.indexOf('\u2592'), "field still draws a hatch");
        assertEquals(row.trim(), "abc");
    }

    @Test
    void fieldHighlightsTheSelection() {
        TWindow window = makeWindow();
        TField field = new TField(window, 1, 1, 10, false, "abcdef");
        field.setSelection(0, 0, 0, 2);

        drawWidget(field);

        int x = field.getAbsoluteX() + 0;
        int y = field.getAbsoluteY();
        // The selected cells use a different color than the unselected ones.
        assertNotEquals(screenAttr(x, y), screenAttr(x + 5, y));
    }

    @Test
    void editorDrawsAllItsLines() {
        TWindow window = makeWindow();
        TEditor editor = new TEditor(window, "one\ntwo\nthree", 1, 1, 20, 5);

        drawWidget(editor);

        int x = editor.getAbsoluteX();
        int y = editor.getAbsoluteY();
        assertEquals("one", screenText(x, y, 3));
        assertEquals("two", screenText(x, y + 1, 3));
        assertEquals("three", screenText(x, y + 2, 5));
    }

    @Test
    void editorHighlightsTheSelection() {
        TWindow window = makeWindow();
        TEditor editor = new TEditor(window, "one\ntwo", 1, 1, 20, 5);
        editor.setSelection(0, 0, 0, 1);

        drawWidget(editor);

        int x = editor.getAbsoluteX();
        int y = editor.getAbsoluteY();
        assertNotEquals(screenAttr(x, y), screenAttr(x, y + 1));
    }

    @Test
    void textDrawsItsLines() {
        TWindow window = makeWindow();
        TText text = new TText(window, "hello world", 1, 1, 20, 5);

        drawWidget(text);

        int x = text.getAbsoluteX();
        int y = text.getAbsoluteY();
        assertEquals("hello world", screenText(x, y, 11));
    }

    @Test
    void textHighlightsTheSelection() {
        TWindow window = makeWindow();
        TText text = new TText(window, "hello world", 1, 1, 20, 5);
        text.setSelection(0, 0, 0, 4);

        drawWidget(text);

        int x = text.getAbsoluteX();
        int y = text.getAbsoluteY();
        assertNotEquals(screenAttr(x, y), screenAttr(x + 6, y));
    }
}
