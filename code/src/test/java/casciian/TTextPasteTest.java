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

import casciian.backend.HeadlessBackend;
import casciian.bits.StringUtils;
import casciian.event.TCommandEvent;
import casciian.event.TKeypressEvent;
import org.junit.jupiter.api.Test;

import static casciian.TCommand.cmPaste;
import static casciian.TKeypress.kbIns;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests direct, atomic clipboard insertion in editable text widgets.
 */
class TTextPasteTest {

    @Test
    void editorPasteDirectlyInsertsTextWithoutSyntheticKeypresses() {
        TWindow window = makeWindow();
        CountingEditor editor = new CountingEditor(window, "abc", 0, 0, 40, 5);
        editor.setEditingColumnNumber(4);

        paste(editor, "XYZ");

        assertEquals("abcXYZ\n", editor.getText());
        assertEquals(7, editor.getEditingColumnNumber());
        assertEquals(0, editor.keypressCount);
        assertTrue(editor.isDirty());
    }

    @Test
    void editorPasteReplacesSelectionWithOneUndoState() {
        TWindow window = makeWindow();
        TEditor editor = new TEditor(window, "abc123xyz", 0, 0, 40, 5);
        editor.setSelection(0, 3, 0, 6);

        paste(editor, "HELLO");

        assertEquals("abcHELLOxyz\n", editor.getText());
        assertFalse(editor.hasSelection());
        editor.undo();
        assertEquals("abc123xyz\n", editor.getText());
        editor.undo();
        assertEquals("abc123xyz\n", editor.getText());
    }

    @Test
    void editorPasteIsOneUndoState() {
        TWindow window = makeWindow();
        TEditor editor = new TEditor(window, "abc", 0, 0, 40, 5);
        editor.setEditingColumnNumber(4);

        paste(editor, "HELLO");
        editor.undo();

        assertEquals("abc\n", editor.getText());
    }

    @Test
    void ignoredPasteDoesNotConsumeAnUndoState() {
        TWindow window = makeWindow();
        TEditor editor = new TEditor(window, "a", 0, 0, 40, 5);
        editor.setEditingColumnNumber(2);
        editor.onKeypress(new TKeypressEvent(null, false, 0, 'b',
                false, false, false));

        paste(editor, "\u0001\u007F");
        editor.undo();

        assertEquals("a\n", editor.getText());
    }

    @Test
    void ignoredPasteWithCollapsedSelectionDoesNotConsumeAnUndoState() {
        TWindow window = makeWindow();
        TEditor editor = new TEditor(window, "a", 0, 0, 40, 5);
        editor.setEditingColumnNumber(2);
        editor.onKeypress(new TKeypressEvent(null, false, 0, 'b',
                false, false, false));
        editor.setSelection(0, editor.getEditingColumnNumber(), 0,
            editor.getEditingColumnNumber());

        paste(editor, "\u0001\u007F");
        editor.undo();

        assertEquals("a\n", editor.getText());
        editor.undo();
        assertEquals("a\n", editor.getText());
    }

    @Test
    void editorPastePreservesLinesTabsUnicodeAndFiltersControls() {
        TWindow window = makeWindow();
        TEditor editor = new TEditor(window, "", 0, 0, 40, 5);

        paste(editor, "one\ntwo\tthree\n你好 🌍 café 👨‍👩‍👧‍👦\u0001\u007F");

        assertEquals("one\ntwo     three\n你好 🌍 café 👨‍👩‍👧‍👦\n",
            editor.getText());
        assertEquals(3, editor.getEditingRowNumber());
        String currentLine = editor.document.getLine(
            editor.getEditingRowNumber() - 1).getRawString();
        int expectedColumn = 1;
        for (int i = 0; i < currentLine.length(); ) {
            int ch = currentLine.codePointAt(i);
            expectedColumn += StringUtils.width(ch);
            i += Character.charCount(ch);
        }
        assertEquals(expectedColumn, editor.getEditingColumnNumber());
    }

    @Test
    void editorPasteTreatsCarriageReturnsAsNewlines() {
        TWindow window = makeWindow();

        TEditor crlf = new TEditor(window, "", 0, 0, 40, 5);
        paste(crlf, "one\r\ntwo\r\nthree");
        assertEquals("one\ntwo\nthree\n", crlf.getText());
        assertEquals(3, crlf.getEditingRowNumber());

        TEditor cr = new TEditor(window, "", 0, 0, 40, 5);
        paste(cr, "one\rtwo\rthree");
        assertEquals("one\ntwo\nthree\n", cr.getText());
        assertEquals(3, cr.getEditingRowNumber());
    }

    @Test
    void fieldPasteNormalizesMultilineTextWithoutEnterAction() {
        TWindow window = makeWindow();
        int[] enters = new int[1];
        TField field = new TField(window, 0, 0, 30, false, "",
            new TAction() {
                public void DO() {
                    enters[0]++;
                }
            });

        paste(field, "hello\nworld\tagain");

        assertEquals("hello world again", field.getText());
        assertEquals(0, enters[0]);
    }

    @Test
    void fixedFieldPasteRespectsCapacityAtEveryInsertionPosition() {
        TWindow window = makeWindow();
        TField beginning = new TField(window, 0, 0, 6, true, "bc");
        TField middle = new TField(window, 0, 1, 6, true, "ad");
        TField end = new TField(window, 0, 2, 6, true, "ab");

        beginning.home();
        paste(beginning, "ABCDE");
        middle.setEditingColumnNumber(2);
        paste(middle, "BCDE");
        end.end();
        paste(end, "ABCDE");

        int capacity = beginning.textAreaWidth();
        assertEquals("ABbc", beginning.getText());
        assertEquals("aBCd", middle.getText());
        assertEquals("abAB", end.getText());
        assertTrue(StringUtils.width(beginning.getText()) <= capacity);
        assertTrue(StringUtils.width(middle.getText()) <= capacity);
        assertTrue(StringUtils.width(end.getText()) <= capacity);
    }

    @Test
    void fixedFieldPasteHandlesSelectionAndWideCharacters() {
        TWindow window = makeWindow();
        TField selection = new TField(window, 0, 0, 6, true, "abcd");
        selection.setSelection(0, 1, 0, 3);
        TField wide = new TField(window, 0, 1, 6, true);

        paste(selection, "XYZ");
        paste(wide, "你你你");

        assertEquals("aXYd", selection.getText());
        assertTrue(StringUtils.width(selection.getText())
            <= selection.textAreaWidth());
        assertEquals("你你", wide.getText());
        assertTrue(StringUtils.width(wide.getText()) <= wide.textAreaWidth());
    }

    @Test
    void pasteHonorsOverwriteMode() {
        TWindow window = makeWindow();
        TField field = new TField(window, 0, 0, 20, false, "abcde");
        field.setEditingColumnNumber(3);
        field.onKeypress(new TKeypressEvent(null, kbIns));

        paste(field, "XYZ");

        assertEquals("abXYZ", field.getText());
        assertTrue(field.isOverwrite());
    }

    @Test
    void fieldPasteDispatchesOneCompleteUpdate() {
        TWindow window = makeWindow();
        int[] updates = new int[1];
        TField field = new TField(window, 0, 0, 30, false, "", null,
            new TAction() {
                public void DO() {
                    updates[0]++;
                }
            });

        paste(field, "ten thousand characters");

        assertEquals("ten thousand characters", field.getText());
        assertEquals(1, updates[0]);
    }

    @Test
    void largePasteHasNoKeyEventsAndOneUndoState() {
        TWindow window = makeWindow();
        CountingEditor editor = new CountingEditor(window, "", 0, 0, 40, 5);
        String text = "x".repeat(20_000);

        paste(editor, text);

        assertEquals(text + "\n", editor.getText());
        assertEquals(0, editor.keypressCount);
        editor.undo();
        assertEquals("\n", editor.getText());
    }

    private TWindow makeWindow() {
        return new TWindow(new TApplication(new HeadlessBackend()), "test",
            0, 0, 80, 10);
    }

    private void paste(final TTextBase widget, final String text) {
        widget.getApplication().getClipboard().copyText(text);
        widget.onCommand(new TCommandEvent(null, cmPaste));
    }

    private static final class CountingEditor extends TEditor {
        private int keypressCount;

        CountingEditor(final TWidget parent, final String text, final int x,
            final int y, final int width, final int height) {

            super(parent, text, x, y, width, height);
        }

        @Override
        public void onKeypress(final TKeypressEvent keypress) {
            keypressCount++;
            super.onKeypress(keypress);
        }
    }
}
