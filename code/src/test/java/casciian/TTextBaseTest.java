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
import casciian.bits.Clipboard;
import casciian.event.TCommandEvent;
import casciian.event.TKeypressEvent;
import static casciian.TCommand.cmClear;
import static casciian.TCommand.cmPaste;
import static casciian.TKeypress.kbBackspace;
import static casciian.TKeypress.kbCtrlC;
import static casciian.TKeypress.kbCtrlV;
import static casciian.TKeypress.kbCtrlX;
import static casciian.TKeypress.kbDel;
import static casciian.TKeypress.kbEnter;
import static casciian.TKeypress.kbLeft;
import static casciian.TKeypress.kbRight;
import static casciian.TKeypress.kbShiftLeft;
import static casciian.TKeypress.kbShiftRight;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the behavior shared by the text widgets built on TTextBase: TField,
 * TText and TEditor.
 */
class TTextBaseTest {

    /**
     * Type a printable character into a widget.
     *
     * @param widget the widget
     * @param ch the character
     */
    private void type(final TWidget widget, final char ch) {
        widget.onKeypress(new TKeypressEvent(null, false, 0, ch,
                false, false, false));
    }

    /**
     * Type a string into a widget.
     *
     * @param widget the widget
     * @param text the text
     */
    private void type(final TWidget widget, final String text) {
        for (int i = 0; i < text.length(); i++) {
            type(widget, text.charAt(i));
        }
    }

    /**
     * Press a key on a widget.
     *
     * @param widget the widget
     * @param key the key
     */
    private void press(final TWidget widget, final TKeypress key) {
        widget.onKeypress(new TKeypressEvent(null, key));
    }

    // ------------------------------------------------------------------------
    // TField -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    void fieldTypingAppendsText() {
        TField field = new TField(null, 0, 0, 20, false);
        type(field, "hello");
        assertEquals("hello", field.getText());
        assertTrue(field.isEditable());
    }

    @Test
    void fieldEditingInTheMiddle() {
        TField field = new TField(null, 0, 0, 20, false, "abc");
        field.home();
        press(field, kbRight);
        type(field, 'X');
        assertEquals("aXbc", field.getText());

        press(field, kbBackspace);
        assertEquals("abc", field.getText());

        field.home();
        press(field, kbDel);
        assertEquals("bc", field.getText());
    }

    @Test
    void fieldFixedDoesNotGrowBeyondWidth() {
        TField field = new TField(null, 0, 0, 5, true);
        type(field, "abcdefghij");
        assertTrue(field.getText().length() <= 5,
            "fixed field grew beyond its width: " + field.getText());
    }

    @Test
    void fieldNonFixedScrollsInsteadOfTruncating() {
        TField field = new TField(null, 0, 0, 5, false);
        type(field, "abcdefghij");
        assertEquals("abcdefghij", field.getText());
    }

    @Test
    void fieldEnterAndUpdateActionsFire() {
        final int[] enters = new int[1];
        final int[] updates = new int[1];
        TField field = new TField(null, 0, 0, 20, false, "",
            new TAction() {
                public void DO() {
                    enters[0]++;
                }
            },
            new TAction() {
                public void DO() {
                    updates[0]++;
                }
            });

        type(field, "hi");
        press(field, kbEnter);

        assertEquals(1, enters[0]);
        assertEquals(2, updates[0]);
        // Enter must not add a line to a single-line field.
        assertEquals(1, field.getLineCount());
        assertEquals("hi", field.getText());
    }

    @Test
    void fieldSelectionWithShiftedKeys() {
        TField field = new TField(null, 0, 0, 20, false, "hello world");
        field.home();
        assertFalse(field.hasSelection());
        assertNull(field.getSelection());

        for (int i = 0; i < 5; i++) {
            press(field, kbShiftRight);
        }
        assertTrue(field.hasSelection());
        // The cell under the cursor is part of the selection, as in TEditor.
        assertEquals("hello ", field.getSelection());

        // A plain navigation key clears the selection.
        press(field, kbLeft);
        assertFalse(field.hasSelection());
    }

    @Test
    void fieldTypingReplacesTheSelection() {
        TField field = new TField(null, 0, 0, 20, false, "hello world");
        field.home();
        for (int i = 0; i < 5; i++) {
            press(field, kbShiftRight);
        }
        type(field, 'X');
        assertEquals("Xworld", field.getText());
        assertFalse(field.hasSelection());
    }

    @Test
    void fieldDeleteRemovesTheSelection() {
        TField field = new TField(null, 0, 0, 20, false, "hello world");
        field.end();
        for (int i = 0; i < 5; i++) {
            press(field, kbShiftLeft);
        }
        assertEquals("world", field.getSelection());
        press(field, kbDel);
        assertEquals("hello ", field.getText());
    }

    @Test
    void fieldSetTextResetsSelectionAndPosition() {
        TField field = new TField(null, 0, 0, 20, false, "hello");
        field.end();
        press(field, kbShiftLeft);
        field.setText("new text");
        assertEquals("new text", field.getText());
        assertFalse(field.hasSelection());
        assertEquals(1, field.getEditingColumnNumber());
    }

    @Test
    void fieldKeepsASingleLine() {
        TField field = new TField(null, 0, 0, 20, false);
        field.setText("one\ntwo\tthree");
        assertEquals(1, field.getLineCount());
        assertEquals("one two three", field.getText());
    }

    @Test
    void fieldBackgroundCharacterIsBlankByDefault() {
        TField field = new TField(null, 0, 0, 20, false);
        assertEquals(' ', field.getBackgroundChar());
    }

    @Test
    void passwordFieldStillTracksItsText() {
        TPasswordField field = new TPasswordField(null, 0, 0, 20, false);
        type(field, "secret");
        assertEquals("secret", field.getText());
    }

    // ------------------------------------------------------------------------
    // TEditor ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    void editorTypingAndNewLines() {
        TEditor editor = new TEditor(null, "", 0, 0, 40, 10);
        assertTrue(editor.isEditable());
        type(editor, "one");
        press(editor, kbEnter);
        type(editor, "two");
        assertEquals(2, editor.getLineCount());
        assertTrue(editor.getText().startsWith("one\ntwo"));
    }

    @Test
    void editorSelectionAcrossLines() {
        TEditor editor = new TEditor(null, "one\ntwo", 0, 0, 40, 10);
        editor.setSelection(0, 0, 1, 2);
        assertTrue(editor.hasSelection());
        assertEquals("one\ntwo", editor.getSelection());
        assertEquals(0, editor.getSelectionStartRow());
        assertEquals(1, editor.getSelectionEndRow());
        editor.unsetSelection();
        assertFalse(editor.hasSelection());
        assertEquals(-1, editor.getSelectionStartRow());
    }

    @Test
    void editorUndoRestoresPreviousText() {
        TEditor editor = new TEditor(null, "abc", 0, 0, 40, 10);
        editor.setEditingRowNumber(1);
        type(editor, 'X');
        assertTrue(editor.getText().startsWith("Xabc"));
        editor.undo();
        assertTrue(editor.getText().startsWith("abc"));
    }

    // ------------------------------------------------------------------------
    // TText ------------------------------------------------------------------
    // ------------------------------------------------------------------------

    @Test
    void textIsNotEditable() {
        TText text = new TText(null, "hello world", 0, 0, 40, 10);
        assertFalse(text.isEditable());
        // Editing commands are ignored.
        text.onCommand(new TCommandEvent(null, cmPaste));
        text.onCommand(new TCommandEvent(null, cmClear));
        assertEquals("hello world", text.getText());
        assertFalse(text.isEditMenuCut());
        assertFalse(text.isEditMenuPaste());
        assertFalse(text.isEditMenuClear());
        assertTrue(text.isEditMenuCopy());
    }

    @Test
    void textIsSelectable() {
        TText text = new TText(null, "hello world", 0, 0, 40, 10);
        text.setSelection(0, 0, 0, 4);
        assertTrue(text.hasSelection());
        assertEquals("hello", text.getSelection());
    }

    @Test
    void textSelectAllCoversTheContent() {
        TText text = new TText(null, "hello", 0, 0, 40, 10);
        text.selectAll();
        assertTrue(text.hasSelection());
        assertTrue(text.getSelection().startsWith("hello"));
    }

    @Test
    void textSetTextAndAddLineKeepTheRawText() {
        TText text = new TText(null, "one", 0, 0, 40, 10);
        assertEquals("one", text.getText());
        text.addLine("two");
        assertEquals("one\n\ntwo", text.getText());
        text.setText("three");
        assertEquals("three", text.getText());
        assertFalse(text.hasSelection());
    }

    // ------------------------------------------------------------------------
    // Clipboard --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Create a window to hold a widget.  A widget needs a window to reach the
     * application clipboard.
     *
     * @return the window
     */
    private TWindow makeWindow() {
        return new TWindow(new TApplication(new HeadlessBackend()), "test",
            0, 0, 40, 10);
    }

    /**
     * The clipboard keys work without going through the Edit menu, so that
     * they also work on the modal dialogs that run on the secondary event
     * thread (TFileOpenBox, TMessageBox) and in applications with no menu.
     */
    @Test
    void clipboardKeysWorkWithoutTheEditMenu() {
        TWindow window = makeWindow();
        Clipboard clipboard = window.getApplication().getClipboard();
        TField field = new TField(window, 1, 1, 20, false, "hello world");

        field.home();
        for (int i = 0; i < 5; i++) {
            press(field, kbShiftRight);
        }
        press(field, kbCtrlC);
        assertEquals("hello ", clipboard.pasteText());
        assertEquals("hello world", field.getText());

        press(field, kbCtrlX);
        assertEquals("hello ", clipboard.pasteText());
        assertEquals("world", field.getText());

        field.home();
        press(field, kbCtrlV);
        assertEquals("hello world", field.getText());
    }

    /**
     * A password field never puts its text on the clipboard.
     */
    @Test
    void passwordFieldDoesNotCopyToTheClipboard() {
        TWindow window = makeWindow();
        Clipboard clipboard = window.getApplication().getClipboard();
        clipboard.copyText("other");
        TPasswordField field = new TPasswordField(window, 1, 1, 20, false,
            "secret");

        field.selectAll();
        press(field, kbCtrlC);
        assertEquals("other", clipboard.pasteText());
        press(field, kbCtrlX);
        assertEquals("other", clipboard.pasteText());
        assertEquals("secret", field.getText());

        assertFalse(field.isEditMenuCopy());
        assertFalse(field.isEditMenuCut());

        // Pasting into it still works.
        field.setText("");
        press(field, kbCtrlV);
        assertEquals("other", field.getText());
    }
}
