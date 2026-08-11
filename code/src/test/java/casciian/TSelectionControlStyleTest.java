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

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
