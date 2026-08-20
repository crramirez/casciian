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
import casciian.event.TMouseEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests mouse interaction with checkbox and radio-button labels.
 */
class TSelectionControlMouseTest {

    @Test
    void clickingCheckboxLabelTogglesTheCheckbox() {
        TWindow window = makeWindow();
        TCheckBox checkBox = new TCheckBox(window, 1, 1, "Enable feature",
            false);

        mouse(window, checkBox.getAbsoluteX() + checkBox.getWidth() - 2,
            checkBox.getAbsoluteY());

        assertTrue(checkBox.isChecked());
    }

    @Test
    void clickingRadioButtonLabelSelectsTheRadioButton() {
        TWindow window = makeWindow();
        TRadioGroup group = new TRadioGroup(window, 1, 1, "Choices");
        group.addRadioButton("First");
        TRadioButton second = group.addRadioButton("Second");

        mouse(window, second.getAbsoluteX() + second.getWidth() - 2,
            second.getAbsoluteY());

        assertEquals(second.getId(), group.getSelected());
    }

    @Test
    void checkboxSeparatorSpaceUsesCheckboxColor() {
        TWindow window = makeWindow();
        TCheckBox checkBox = new TCheckBox(window, 1, 1, "Enable feature",
            false);

        assertSeparatorPainted(checkBox);

        checkBox.setMatchWindowBackground(false);
        assertSeparatorPainted(checkBox);
    }

    @Test
    void radioButtonSeparatorSpaceUsesRadioButtonColor() {
        TWindow window = makeWindow();
        TRadioGroup group = new TRadioGroup(window, 1, 1, "Choices");
        TRadioButton radioButton = group.addRadioButton("Second");

        assertSeparatorPainted(radioButton);

        radioButton.setMatchWindowBackground(false);
        assertSeparatorPainted(radioButton);
    }

    private TWindow makeWindow() {
        return new TWindow(new TApplication(new HeadlessBackend()), "test",
            0, 0, 40, 10);
    }

    private void assertSeparatorPainted(final TWidget control) {

        control.getWindow().drawChildren();

        int padding = ControlPadding.current().getCells();
        int gapX = control.getAbsoluteX() + padding + 3;
        int y = control.getAbsoluteY();

        assertEquals(' ', control.getScreen().getCharXY(gapX, y).getChar());
        assertEquals(control.getScreen().getAttrXY(gapX - 1, y),
            control.getScreen().getAttrXY(gapX, y));
        assertEquals(control.getScreen().getAttrXY(gapX, y),
            control.getScreen().getAttrXY(gapX + 1, y));
    }

    private void mouse(final TWidget widget, final int x, final int y) {
        TMouseEvent event = new TMouseEvent(null, TMouseEvent.Type.MOUSE_DOWN,
            x, y, x, y, 0, 0,
            true, false, false, false, false, false, false, false);
        widget.onMouseDown(event);
    }
}
