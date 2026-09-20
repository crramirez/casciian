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
package casciian.menu;

import org.junit.jupiter.api.Test;

import casciian.TApplication;
import casciian.backend.HeadlessBackend;
import casciian.backend.Screen;
import casciian.bits.GraphicsChars;
import static casciian.TKeypress.kbCtrlF1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TSubMenuTest {

    @Test
    void submenuAcceleratorsAreIgnoredForWidthAndRendering() {
        TApplication application = new TApplication(new HeadlessBackend());
        TMenu menu = application.addMenu("&File");
        TSubMenu subMenu = menu.addSubMenu("&Recent");
        int width = subMenu.getWidth();

        subMenu.setKey(kbCtrlF1);

        assertEquals(width, subMenu.getWidth());

        subMenu.recomputeWidth();

        assertEquals(width, subMenu.getWidth());

        Screen screen = application.getScreen();
        screen.clearPhysical();
        menu.setActive(true);
        menu.draw();
        menu.drawChildren();

        String row = rowText(subMenu);
        assertTrue(row.contains("Recent"));
        assertFalse(row.contains(kbCtrlF1.toString()));
        assertEquals(' ', charAt(subMenu, subMenu.getWidth() - 4));
        assertEquals(GraphicsChars.CP437[0x10],
            charAt(subMenu, subMenu.getWidth() - 3));
    }

    private String rowText(final TSubMenu subMenu) {
        StringBuilder row = new StringBuilder();
        for (int x = 0; x < subMenu.getWidth(); x++) {
            row.appendCodePoint(charAt(subMenu, x));
        }
        return row.toString();
    }

    private int charAt(final TSubMenu subMenu, final int x) {
        return subMenu.getScreen().getCharXY(subMenu.getAbsoluteX() + x,
            subMenu.getAbsoluteY()).getChar();
    }
}
