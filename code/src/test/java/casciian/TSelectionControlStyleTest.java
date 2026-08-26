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
import casciian.bits.ControlPadding;
import casciian.event.TKeypressEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests checkbox and radio-button style rendering.
 */
class TSelectionControlStyleTest {

    @Test
    void checkboxUsesConfiguredCheckedSymbol() {
        withProperty(TCheckBox.PROPERTY_KEY, "times", () -> {
            TWindow window = makeWindow();
            TCheckBox checkBox = new TCheckBox(window, 1, 1, "Enable feature",
                true);

            window.drawChildren();

            int symbolX = checkBox.getAbsoluteX()
                + ControlPadding.current().getCells() + 1;
            assertEquals('\u00D7',
                window.getScreen().getCharXY(symbolX, checkBox.getAbsoluteY())
                    .getChar());
        });
    }

    @Test
    void widenedCheckboxPaintsTrailingCellsWithCheckboxColor() {
        TWindow window = makeWindow();
        TCheckBox checkBox = new TCheckBox(window, 1, 1, "Enable feature",
            true);
        checkBox.setWidth(checkBox.getWidth() + 4);

        window.drawChildren();

        int padding = ControlPadding.current().getCells();
        int separatorX = checkBox.getAbsoluteX() + padding + 3;
        int trailingX = checkBox.getAbsoluteX() + checkBox.getWidth() - 1;
        int y = checkBox.getAbsoluteY();

        assertEquals(' ', window.getScreen().getCharXY(trailingX, y).getChar());
        assertEquals(window.getScreen().getAttrXY(separatorX, y),
            window.getScreen().getAttrXY(trailingX, y));
    }

    @Test
    void checkboxPaddingTracksThemeChanges() {
        withProperty(ControlPadding.PROPERTY_KEY, null, () -> {
            TWindow window = makeWindow();
            TCheckBox checkBox = new TCheckBox(window, 1, 1, "Choice", true);
            int x = checkBox.getAbsoluteX();
            int y = checkBox.getAbsoluteY();

            window.drawChildren();
            assertEquals('[', window.getScreen().getCharXY(x + 1, y).getChar());

            window.getTheme().setFemme();
            window.drawChildren();
            assertEquals('[', window.getScreen().getCharXY(x, y).getChar());

            window.getTheme().setDarkDefault();
            window.drawChildren();
            assertEquals('[', window.getScreen().getCharXY(x + 1, y).getChar());

            window.getTheme().setQmodem5();
            window.drawChildren();
            assertEquals('[', window.getScreen().getCharXY(x, y).getChar());
        });
    }

    @Test
    void checkboxExpandsWhenThemePaddingIncreases() {
        withProperty(ControlPadding.PROPERTY_KEY, null, () -> {
            TWindow window = makeWindow();
            window.getTheme().setFemme();
            TCheckBox checkBox = new TCheckBox(window, 1, 1, "Choice", true);
            int initialWidth = checkBox.getWidth();
            int x = checkBox.getAbsoluteX();
            int y = checkBox.getAbsoluteY();

            window.drawChildren();
            assertEquals('[', window.getScreen().getCharXY(x, y).getChar());

            window.getTheme().setDarkDefault();
            window.drawChildren();
            assertEquals('[', window.getScreen().getCharXY(x + 1, y).getChar());
            assertEquals(initialWidth + 2, checkBox.getWidth());
        });
    }

    @Test
    void radioButtonUsesConfiguredSelectedSymbol() {
        withProperty(TRadioButton.PROPERTY_KEY, "asterisk", () -> {
            TWindow window = makeWindow();
            TRadioGroup group = new TRadioGroup(window, 1, 1, 20, "Choices");
            TRadioButton radioButton = group.addRadioButton("Second", true);

            window.drawChildren();

            int symbolX = radioButton.getAbsoluteX()
                + ControlPadding.current().getCells() + 1;
            assertEquals('*',
                window.getScreen().getCharXY(symbolX,
                    radioButton.getAbsoluteY()).getChar());
        });
    }

    @Test
    void wideRadioGroupRowsPaintTrailingCellsWithRadioButtonColor() {
        TWindow window = makeWindow();
        TRadioGroup group = new TRadioGroup(window, 1, 1, 20, "Choices");
        TRadioButton radioButton = group.addRadioButton("A", true);

        window.drawChildren();

        int padding = ControlPadding.current().getCells();
        int separatorX = radioButton.getAbsoluteX() + padding + 3;
        int trailingX = group.getAbsoluteX() + group.getWidth() - 2;
        int y = radioButton.getAbsoluteY();

        assertEquals(' ', window.getScreen().getCharXY(trailingX, y).getChar());
        assertEquals(window.getScreen().getAttrXY(separatorX, y),
            window.getScreen().getAttrXY(trailingX, y));
    }

    @Test
    void checkboxMnemonicTogglesState() {
        TWindow window = makeWindow();
        TCheckBox checkBox = new TCheckBox(window, 1, 1, "&Enable feature",
            false);

        pressMnemonic(window, 'e');
        assertTrue(checkBox.isChecked());

        pressMnemonic(window, 'e');
        assertFalse(checkBox.isChecked());
    }

    @Test
    void radioGroupMnemonicActivatesSelectedButton() {
        TWindow window = makeWindow();
        TCheckBox checkBox = new TCheckBox(window, 1, 1, "&Enable feature",
            false);
        TRadioGroup group = new TRadioGroup(window, 1, 3, 20, "&Choices");
        group.addRadioButton("&First");
        TRadioButton second = group.addRadioButton("&Second");
        group.setSelected(second.getId());
        window.activate(checkBox);

        pressMnemonic(window, 'c');

        assertTrue(group.isActive());
        assertTrue(second.isActive());
        assertEquals(second.getId(), group.getSelected());
    }

    @Test
    void radioGroupDrawsMnemonicUsingGroupMnemonicColor() {
        TWindow window = makeWindow();
        TCheckBox checkBox = new TCheckBox(window, 1, 1, "Other", false);
        TRadioGroup group = new TRadioGroup(window, 1, 3, 20, "&Choices");
        window.activate(checkBox);

        window.drawChildren();

        assertEquals(window.getTheme().getColor("tradiogroup.mnemonic"),
            window.getScreen().getAttrXY(group.getAbsoluteX() + 1,
                group.getAbsoluteY()));
        assertEquals('C',
            window.getScreen().getCharXY(group.getAbsoluteX() + 1,
                group.getAbsoluteY()).getChar());
    }

    @Test
    void radioGroupHighlightedMnemonicDefaultsMirrorNonHighlighted() {
        TWindow window = makeWindow();
        new TRadioGroup(window, 1, 1, 20, "&Choices");

        assertEquals(window.getTheme().getColor("tradiogroup.mnemonic"),
            window.getTheme().getColor("tradiogroup.mnemonic.highlighted"));
        assertEquals(window.getTheme().getColor("tradiogroup.mnemonic.modal"),
            window.getTheme().getColor("tradiogroup.mnemonic.highlighted.modal"));
    }

    private TWindow makeWindow() {
        return new TWindow(new TApplication(new HeadlessBackend()), "test",
            0, 0, 40, 10);
    }

    private void withProperty(final String key, final String value,
        final Runnable test) {

        String oldValue = System.getProperty(key);
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
        try {
            test.run();
        } finally {
            if (oldValue == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, oldValue);
            }
        }
    }

    private void pressMnemonic(final TWidget widget, final char ch) {
        widget.onKeypress(new TKeypressEvent(null,
            new TKeypress(false, 0, ch, true, false, false)));
    }
}
