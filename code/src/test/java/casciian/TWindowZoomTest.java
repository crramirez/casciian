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
import casciian.event.TCommandEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TMenuEvent;
import casciian.menu.TMenu;

import static casciian.TCommand.cmWindowZoom;
import static casciian.TKeypress.kbF5;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Tests user-initiated window zoom behavior.
 */
class TWindowZoomTest {

    @Test
    void f5DoesNotZoomNonResizableWindow() {
        TWindow window = window(0);
        int originalWidth = window.getWidth();
        int originalHeight = window.getHeight();

        window.onKeypress(new TKeypressEvent(null, kbF5));

        assertEquals(originalWidth, window.getWidth());
        assertEquals(originalHeight, window.getHeight());
    }

    @Test
    void zoomCommandDoesNotZoomNonResizableWindow() {
        TWindow window = window(0);
        int originalWidth = window.getWidth();
        int originalHeight = window.getHeight();

        window.onCommand(new TCommandEvent(null, cmWindowZoom));

        assertEquals(originalWidth, window.getWidth());
        assertEquals(originalHeight, window.getHeight());
    }

    @Test
    void zoomMenuItemDoesNotZoomNonResizableWindow() {
        TWindow window = window(0);
        int originalWidth = window.getWidth();
        int originalHeight = window.getHeight();

        window.onMenu(new TMenuEvent(null, TMenu.MID_WINDOW_ZOOM));

        assertEquals(originalWidth, window.getWidth());
        assertEquals(originalHeight, window.getHeight());
    }

    @Test
    void f5StillZoomsResizableWindow() {
        TWindow window = window(TWindow.RESIZABLE);
        int originalWidth = window.getWidth();
        int originalHeight = window.getHeight();

        window.onKeypress(new TKeypressEvent(null, kbF5));

        assertNotEquals(originalWidth, window.getWidth());
        assertNotEquals(originalHeight, window.getHeight());
    }

    private TWindow window(final int flags) {
        return new TWindow(new TApplication(new HeadlessBackend()), "zoom",
            1, 1, 40, 10, flags);
    }
}
