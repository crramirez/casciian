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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import casciian.backend.HeadlessBackend;
import casciian.event.TKeypressEvent;

import static casciian.TKeypress.kbAltC;
import static casciian.TKeypress.kbAltN;
import static casciian.TKeypress.kbAltO;
import static casciian.TKeypress.kbAltY;
import static casciian.TKeypress.kbC;
import static casciian.TKeypress.kbEnter;
import static casciian.TKeypress.kbN;
import static casciian.TKeypress.kbO;
import static casciian.TKeypress.kbSpace;
import static casciian.TKeypress.kbY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests window-level default button behavior.
 */
class TWindowDefaultButtonTest {

    @Test
    void enterActivatesDefaultButtonEvenWhenAnotherButtonHasFocus() {
        TWindow window = makeWindow();
        TField field = new TField(window, 1, 1, 12, false, "");
        int[] activations = new int[2];
        TButton cancel = button(window, "&Cancel", 1, 3, activations, 0);
        TButton ok = button(window, "&OK", 12, 3, activations, 1);
        window.setDefaultButton(ok);

        window.activate(cancel);
        press(window, kbEnter);

        assertEquals(0, activations[0]);
        assertEquals(1, activations[1]);

        window.activate(ok);
        press(window, kbEnter);

        assertEquals(0, activations[0]);
        assertEquals(2, activations[1]);

        window.activate(cancel);
        press(window, kbSpace);

        assertEquals(1, activations[0]);
        assertEquals(2, activations[1]);
        assertEquals("", field.getText());
    }

    @Test
    void noDefaultButtonPreservesExistingEnterBehavior() {
        TWindow window = makeWindow();
        int[] fieldEnters = new int[1];
        TField field = new TField(window, 1, 1, 12, false, "",
            new TAction() {
                public void DO() {
                    fieldEnters[0]++;
                }
            }, null);
        int[] activations = new int[1];
        button(window, "&OK", 1, 3, activations, 0);
        window.activate(field);

        press(window, kbEnter);

        assertEquals(1, fieldEnters[0]);
        assertEquals(0, activations[0]);
        assertNull(window.getDefaultButton());
    }

    @Test
    void disabledOrInvisibleDefaultButtonIsNotActivated() {
        TWindow window = makeWindow();
        int[] fieldEnters = new int[1];
        TField field = new TField(window, 1, 1, 12, false, "",
            new TAction() {
                public void DO() {
                    fieldEnters[0]++;
                }
            }, null);
        int[] activations = new int[1];
        TButton ok = button(window, "&OK", 1, 3, activations, 0);
        window.setDefaultButton(ok);
        window.activate(field);

        ok.setEnabled(false);
        press(window, kbEnter);
        assertEquals(1, fieldEnters[0]);
        assertEquals(0, activations[0]);

        ok.setEnabled(true);
        ok.setVisible(false);
        press(window, kbEnter);
        assertEquals(2, fieldEnters[0]);
        assertEquals(0, activations[0]);
    }

    @Test
    void fieldEnterActionTakesPrecedenceOverDefaultButton() {
        TWindow window = makeWindow();
        int[] fieldEnters = new int[1];
        TField field = new TField(window, 1, 1, 12, false, "",
            new TAction() {
                public void DO() {
                    fieldEnters[0]++;
                }
            }, null);
        int[] activations = new int[1];
        TButton ok = button(window, "&OK", 1, 3, activations, 0);
        window.setDefaultButton(ok);
        window.activate(field);

        press(window, kbEnter);

        assertEquals(1, fieldEnters[0]);
        assertEquals(0, activations[0]);
    }

    @Test
    void defaultButtonCanChangeOrBeRemovedDynamically() {
        TWindow window = makeWindow();
        int[] fieldEnters = new int[1];
        TField field = new TField(window, 1, 1, 12, false, "",
            new TAction() {
                public void DO() {
                    fieldEnters[0]++;
                }
            }, null);
        int[] activations = new int[2];
        TButton first = button(window, "&First", 1, 3, activations, 0);
        TButton second = button(window, "&Second", 12, 3, activations, 1);
        window.activate(second);

        window.setDefaultButton(first);
        press(window, kbEnter);
        assertEquals(1, activations[0]);
        assertEquals(0, activations[1]);

        window.setDefaultButton(second);
        press(window, kbEnter);
        assertEquals(1, activations[0]);
        assertEquals(1, activations[1]);

        window.setDefaultButton(null);
        window.activate(field);
        press(window, kbEnter);
        assertEquals(1, activations[0]);
        assertEquals(1, activations[1]);
        assertEquals(1, fieldEnters[0]);
        assertNull(window.getDefaultButton());
    }

    @Test
    void removingDefaultButtonClearsWindowReference() {
        TWindow window = makeWindow();
        TWidget panel = new TPanel(window, 1, 1, 20, 5);
        TButton ok = button(panel, "&OK", 1, 1, new int[1], 0);
        window.setDefaultButton(ok);

        panel.remove(ok, false);

        assertNull(window.getDefaultButton());
    }

    @Test
    void messageBoxOkEnterActivatesOk() {
        TMessageBox box = makeMessageBox(TMessageBox.Type.OK);

        press(box, kbEnter);

        assertEquals(TMessageBox.Result.OK, box.getResult());
    }

    @Test
    void messageBoxOkCancelEnterActivatesOkEvenWhenCancelHasFocus() {
        TMessageBox box = makeMessageBox(TMessageBox.Type.OKCANCEL);
        activateMessageBoxButton(box, 1);

        press(box, kbEnter);

        assertEquals(TMessageBox.Result.OK, box.getResult());
    }

    @Test
    void messageBoxYesNoEnterActivatesYesEvenWhenNoHasFocus() {
        TMessageBox box = makeMessageBox(TMessageBox.Type.YESNO);
        activateMessageBoxButton(box, 1);

        press(box, kbEnter);

        assertEquals(TMessageBox.Result.YES, box.getResult());
    }

    @Test
    void messageBoxYesNoCancelEnterActivatesYesEvenWhenAnotherButtonHasFocus() {
        TMessageBox box = makeMessageBox(TMessageBox.Type.YESNOCANCEL);
        activateMessageBoxButton(box, 2);

        press(box, kbEnter);

        assertEquals(TMessageBox.Result.YES, box.getResult());
    }

    @Test
    void messageBoxConvenienceShortcutsStillWork() {
        assertEquals(TMessageBox.Result.OK,
            pressMessageBoxShortcut(TMessageBox.Type.OK, kbO));
        assertEquals(TMessageBox.Result.OK,
            pressMessageBoxShortcut(TMessageBox.Type.OK, kbAltO));

        assertEquals(TMessageBox.Result.OK,
            pressMessageBoxShortcut(TMessageBox.Type.OKCANCEL, kbO));
        assertEquals(TMessageBox.Result.OK,
            pressMessageBoxShortcut(TMessageBox.Type.OKCANCEL, kbAltO));
        assertEquals(TMessageBox.Result.CANCEL,
            pressMessageBoxShortcut(TMessageBox.Type.OKCANCEL, kbC));
        assertEquals(TMessageBox.Result.CANCEL,
            pressMessageBoxShortcut(TMessageBox.Type.OKCANCEL, kbAltC));

        assertEquals(TMessageBox.Result.YES,
            pressMessageBoxShortcut(TMessageBox.Type.YESNO, kbY));
        assertEquals(TMessageBox.Result.YES,
            pressMessageBoxShortcut(TMessageBox.Type.YESNO, kbAltY));
        assertEquals(TMessageBox.Result.NO,
            pressMessageBoxShortcut(TMessageBox.Type.YESNO, kbN));
        assertEquals(TMessageBox.Result.NO,
            pressMessageBoxShortcut(TMessageBox.Type.YESNO, kbAltN));

        assertEquals(TMessageBox.Result.YES,
            pressMessageBoxShortcut(TMessageBox.Type.YESNOCANCEL, kbY));
        assertEquals(TMessageBox.Result.YES,
            pressMessageBoxShortcut(TMessageBox.Type.YESNOCANCEL, kbAltY));
        assertEquals(TMessageBox.Result.NO,
            pressMessageBoxShortcut(TMessageBox.Type.YESNOCANCEL, kbN));
        assertEquals(TMessageBox.Result.NO,
            pressMessageBoxShortcut(TMessageBox.Type.YESNOCANCEL, kbAltN));
        assertEquals(TMessageBox.Result.CANCEL,
            pressMessageBoxShortcut(TMessageBox.Type.YESNOCANCEL, kbC));
        assertEquals(TMessageBox.Result.CANCEL,
            pressMessageBoxShortcut(TMessageBox.Type.YESNOCANCEL, kbAltC));
    }

    @Test
    void editorEnterInsertsNewlineInsteadOfActivatingDefaultButton() {
        TWindow window = makeWindow();
        TEditor editor = new TEditor(window, "line", 1, 1, 20, 5);
        int[] activations = new int[1];
        TButton ok = button(window, "&OK", 1, 7, activations, 0);
        window.setDefaultButton(ok);
        window.activate(editor);

        assertEquals(1, editor.getLineCount());
        press(window, kbEnter);

        assertEquals(0, activations[0]);
        assertEquals(2, editor.getLineCount());
    }

    @Test
    void tableKeepsEnterInsteadOfActivatingDefaultButton() {
        TWindow window = makeWindow();
        TTable table = new TTable(window, 1, 1, 30, 6, 2, 2);
        int[] activations = new int[1];
        TButton ok = button(window, "&OK", 1, 8, activations, 0);
        window.setDefaultButton(ok);
        window.activate(table);

        press(window, kbEnter);

        assertEquals(0, activations[0]);
    }

    @Test
    void listKeepsEnterInsteadOfActivatingDefaultButton() {
        TWindow window = makeWindow();
        List<String> values = new ArrayList<>();
        values.add("one");
        values.add("two");
        int[] listEnters = new int[1];
        TList list = new TList(window, values, 1, 1, 20, 4,
            new TAction() {
                public void DO() {
                    listEnters[0]++;
                }
            });
        list.setSelectedIndex(0);
        int[] activations = new int[1];
        TButton ok = button(window, "&OK", 1, 6, activations, 0);
        window.setDefaultButton(ok);
        window.activate(list);

        press(window, kbEnter);

        assertEquals(1, listEnters[0]);
        assertEquals(0, activations[0]);
    }

    @Test
    void calendarKeepsEnterInsteadOfActivatingDefaultButton() {
        TWindow window = makeWindow();
        int[] calendarEnters = new int[1];
        TCalendar calendar = new TCalendar(window, 1, 1,
            new TAction() {
                public void DO() {
                    calendarEnters[0]++;
                }
            });
        int[] activations = new int[1];
        TButton ok = button(window, "&OK", 1, 9, activations, 0);
        window.setDefaultButton(ok);
        window.activate(calendar);

        press(window, kbEnter);

        assertEquals(1, calendarEnters[0]);
        assertEquals(0, activations[0]);
    }

    @Test
    void checkBoxTogglesOnEnterInsteadOfActivatingDefaultButton() {
        TWindow window = makeWindow();
        TCheckBox checkBox = new TCheckBox(window, 1, 1, "Check", false);
        int[] activations = new int[1];
        TButton ok = button(window, "&OK", 1, 3, activations, 0);
        window.setDefaultButton(ok);
        window.activate(checkBox);

        assertFalse(checkBox.isChecked());
        press(window, kbEnter);

        assertTrue(checkBox.isChecked());
        assertEquals(0, activations[0]);
    }

    @Test
    void treeViewKeepsEnterInsteadOfActivatingDefaultButton() {
        TWindow window = makeWindow();
        int[] treeEnters = new int[1];
        TTreeView tree = new TTreeView(window, 1, 1, 20, 5,
            new TAction() {
                public void DO() {
                    treeEnters[0]++;
                }
            });
        TTreeItem root = new TTreeItem(tree, "root", true);
        tree.setSelected(root, false);
        int[] activations = new int[1];
        TButton ok = button(window, "&OK", 1, 7, activations, 0);
        window.setDefaultButton(ok);
        window.activate(tree);

        press(window, kbEnter);

        assertEquals(1, treeEnters[0]);
        assertEquals(0, activations[0]);
    }

    private TWindow makeWindow() {
        return new TWindow(new TApplication(new HeadlessBackend()), "test",
            0, 0, 40, 10);
    }

    private TMessageBox makeMessageBox(final TMessageBox.Type type) {
        return new TMessageBox(new TApplication(new HeadlessBackend()), "test",
            "caption", type, false);
    }

    private TButton button(final TWidget parent, final String text, final int x,
        final int y, final int[] activations, final int index) {

        return new TButton(parent, text, x, y, new TAction() {
            public void DO() {
                activations[index]++;
            }
        });
    }

    private void press(final TWidget widget, final TKeypress keypress) {
        widget.onKeypress(new TKeypressEvent(null, keypress));
    }

    private void activateMessageBoxButton(final TMessageBox box,
        final int buttonIndex) {

        box.activate(getButtons(box).get(buttonIndex));
    }

    private TMessageBox.Result pressMessageBoxShortcut(
        final TMessageBox.Type type, final TKeypress keypress) {

        TMessageBox box = makeMessageBox(type);
        press(box, keypress);
        return box.getResult();
    }

    private List<TButton> getButtons(final TWidget widget) {
        List<TButton> buttons = new ArrayList<>();
        for (TWidget child: widget.getChildren()) {
            if (child instanceof TButton button) {
                buttons.add(button);
            }
        }
        return buttons;
    }
}
