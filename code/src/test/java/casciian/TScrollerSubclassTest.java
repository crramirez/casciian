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
import casciian.bits.GraphicsChars;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Regression tests for custom {@link TScroller} subclasses.
 */
class TScrollerSubclassTest {

    /**
     * Simple test scroller that exposes the protected base constructor.
     */
    private static final class TestScroller extends TScroller {

        private TestScroller(final Orientation orientation,
            final TWidget parent, final int x, final int y,
            final int width, final int height) {

            super(orientation, parent, x, y, width, height);
        }
    }

    @Test
    void customScrollerSubclassesDrawOnWindowEdges() {
        TApplication application = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(application, "test", 0, 0, 20, 8);
        TestScroller vertical = new TestScroller(TScroller.Orientation.VERTICAL,
            window, window.getWidth() - 2, 0, 1, window.getHeight() - 2);
        TestScroller horizontal = new TestScroller(
            TScroller.Orientation.HORIZONTAL, window, 0,
            window.getHeight() - 2, window.getWidth() - 2, 1);

        window.drawChildren();

        assertEquals(GraphicsChars.CP437[0x1E],
            application.getScreen().getCharXY(vertical.getAbsoluteX(),
                vertical.getAbsoluteY()).getChar());
        assertEquals(GraphicsChars.CP437[0x1F],
            application.getScreen().getCharXY(vertical.getAbsoluteX(),
                vertical.getAbsoluteY() + vertical.getHeight() - 1).getChar());
        assertEquals(GraphicsChars.CP437[0x11],
            application.getScreen().getCharXY(horizontal.getAbsoluteX(),
                horizontal.getAbsoluteY()).getChar());
        assertEquals(GraphicsChars.CP437[0x10],
            application.getScreen().getCharXY(horizontal.getAbsoluteX()
                + horizontal.getWidth() - 1,
                horizontal.getAbsoluteY()).getChar());
    }

    @Test
    void customScrollerSubclassesAreSkippedByFocusManagement() {
        TApplication application = new TApplication(new HeadlessBackend());
        TWindow window = new TWindow(application, "test", 0, 0, 20, 8);
        TField first = new TField(window, 1, 1, 5, false, "one");
        TestScroller scroller = new TestScroller(TScroller.Orientation.VERTICAL,
            window, window.getWidth() - 2, 0, 1, window.getHeight() - 2);
        TField second = new TField(window, 1, 2, 5, false, "two");

        window.activate(first);
        assertSame(first, window.getActiveChild());

        window.activate(scroller);
        assertSame(first, window.getActiveChild());

        window.switchWidget(true);
        assertSame(second, window.getActiveChild());

        window.activate(first);
        first.setEnabled(false);
        assertSame(second, window.getActiveChild());
    }
}
