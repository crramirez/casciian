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
import casciian.bits.ControlPadding;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
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

    /**
     * A single-line field must not swallow the vertical navigation keys: they
     * move the focus to the next/previous widget, as they did before the
     * refactor.
     */
    @Test
    void fieldVerticalKeysSwitchWidgets() {
        TWindow w = makeWindow();
        TField field1 = new TField(w, 1, 1, 10, false, "one");
        TField field2 = new TField(w, 1, 2, 10, false, "two");
        w.activate(field1);
        assertTrue(field1.isActive());

        field1.onKeypress(new TKeypressEvent(null, TKeypress.kbDown));
        assertTrue(field2.isActive());

        field2.onKeypress(new TKeypressEvent(null, TKeypress.kbUp));
        assertTrue(field1.isActive());
    }

    /**
     * A field does not scroll with the mouse wheel, and a password field can
     * still be drawn afterwards.
     */
    @Test
    void fieldIgnoresMouseWheel() {
        TWindow w = makeWindow();
        TPasswordField field = new TPasswordField(w, 1, 1, 10, false, "abcde");
        for (int i = 0; i < 10; i++) {
            field.onMouseDown(new TMouseEvent(null,
                TMouseEvent.Type.MOUSE_DOWN, 1, 0,
                field.getAbsoluteX() + 1, field.getAbsoluteY(), 0, 0,
                false, false, false, false, false, true, false, false,
                false, false));
        }
        // Must not throw.
        drawWidget(w);
        assertEquals("abcde", field.getText());
    }

    /**
     * On a fixed field the cursor never leaves the text area.
     */
    @Test
    void fixedFieldKeepsCursorInsideTheField() {
        TWindow w = makeWindow();
        TField field = new TField(w, 1, 1, 5, true, "abcde");
        for (int i = 0; i < 10; i++) {
            field.onKeypress(new TKeypressEvent(null, TKeypress.kbRight));
        }
        assertTrue(field.getCursorX() < field.getWidth());
        field.onKeypress(new TKeypressEvent(null, TKeypress.kbEnd));
        assertTrue(field.getCursorX() < field.getWidth());
        assertEquals("abcde", field.getText());
    }

    /**
     * With control padding active a fixed field keeps its text capacity
     * (both paddings reserved), but the cursor may reach the right padding
     * cell after the last character instead of being pulled back onto it.
     */
    @Test
    void fixedFieldCursorReachesRightPaddingCell() {
        String previous = System.getProperty(ControlPadding.PROPERTY_KEY);
        System.setProperty(ControlPadding.PROPERTY_KEY, "single");
        try {
            TWindow w = makeWindow();
            // Width 8, single padding: text area is 8 - 2 = 6 cells.
            TField field = new TField(w, 1, 1, 8, true, "abcdefgh");
            w.activate(field);

            field.onKeypress(new TKeypressEvent(null, TKeypress.kbEnd));
            drawWidget(field);

            // Capacity is unchanged: only six characters are kept.
            assertEquals("abcdef", field.getText());
            // The row shows the left padding, the six characters, and a
            // blank right padding cell.
            assertEquals(" abcdef ",
                screenText(field.getAbsoluteX(), field.getAbsoluteY(), 8));
            // The cursor sits over the right padding cell (the last cell).
            assertEquals(field.getWidth() - 1, field.getCursorX());
        } finally {
            if (previous == null) {
                System.clearProperty(ControlPadding.PROPERTY_KEY);
            } else {
                System.setProperty(ControlPadding.PROPERTY_KEY, previous);
            }
        }
    }

    /**
     * A password field masks its text with stars, whether it is active or
     * not.
     */
    @Test
    void passwordFieldAlwaysDrawsStars() {
        TWindow w = makeWindow();
        TPasswordField field = new TPasswordField(w, 1, 1, 10, false, "secret");
        w.activate(field);

        drawWidget(field);

        int x = field.getAbsoluteX();
        int y = field.getAbsoluteY();
        assertEquals("******", screenText(x, y, 6));
        assertEquals("secret", field.getText());
    }

    /**
     * A field that lost the focus does not highlight its selection anymore,
     * but it does remember it for when the focus comes back.
     */
    @Test
    void fieldDoesNotHighlightTheSelectionWhenUnfocused() {
        TWindow w = makeWindow();
        TField field = new TField(w, 1, 1, 10, false, "abcdef");
        TField other = new TField(w, 1, 2, 10, false, "other");
        w.activate(field);
        field.setSelection(0, 0, 0, 2);

        w.activate(other);
        drawWidget(field);

        int x = field.getAbsoluteX();
        int y = field.getAbsoluteY();
        // Nothing is highlighted: the selected and unselected cells match.
        assertEquals(screenAttr(x, y), screenAttr(x + 5, y));
        assertTrue(field.hasSelection());

        // Focus comes back: the selection shows up again.
        w.activate(field);
        field.setSelection(0, 0, 0, 2);
        drawWidget(field);
        assertNotEquals(screenAttr(x, y), screenAttr(x + 5, y));
    }

    /**
     * A field highlights its selection with the tfield.selected theme color,
     * not with the editor's.
     */
    @Test
    void fieldUsesItsOwnSelectionColor() {
        TWindow w = makeWindow();
        TField field = new TField(w, 1, 1, 10, false, "abcdef");
        w.activate(field);
        field.setSelection(0, 0, 0, 2);

        drawWidget(field);

        CellAttributes expected = field.getWidgetColor("tfield.selected");
        assertEquals(expected,
            screenAttr(field.getAbsoluteX(), field.getAbsoluteY()));
    }

    /**
     * A password field highlights its selection, over the stars.
     */
    @Test
    void passwordFieldHighlightsTheSelection() {
        TWindow w = makeWindow();
        TPasswordField field = new TPasswordField(w, 1, 1, 10, false, "secret");
        field.setSelection(0, 0, 0, 2);

        drawWidget(field);

        int x = field.getAbsoluteX();
        int y = field.getAbsoluteY();
        assertNotEquals(screenAttr(x, y), screenAttr(x + 5, y));
    }
}
