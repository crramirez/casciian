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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for TList selection and scrolling.
 */
class TListTest {

    /**
     * How long an operation may take before it is considered hung.
     */
    private static final long TIMEOUT_MILLIS = 5000;

    /**
     * Build a list of numbered entries.
     *
     * @param count how many entries
     * @return the entries
     */
    private static List<String> items(final int count) {
        List<String> items = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            items.add("item " + i);
        }
        return items;
    }

    /**
     * Run an action on a daemon thread and fail if it does not finish.  A
     * daemon thread is used so that a regression that reintroduces an
     * endless loop fails the test instead of wedging the test JVM.
     *
     * @param action the action to run
     */
    private static void assertTerminates(final Runnable action) {
        AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        Thread thread = new Thread(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            }
        }, "TListTest");
        thread.setDaemon(true);
        thread.start();
        try {
            thread.join(TIMEOUT_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("interrupted while waiting for TList");
        }
        assertFalse(thread.isAlive(), "TList operation did not terminate");
        if (error.get() != null) {
            fail(error.get());
        }
    }

    /**
     * Shrinking a list that has a selection must not spin forever trying to
     * scroll the selection into view.
     */
    @Test
    void shrinkingHeightWithSelectionTerminates() {
        TList list = new TList(null, items(3), 0, 0, 20, 6);
        list.setSelectedIndex(2);

        assertTerminates(() -> list.setHeight(2));

        assertEquals(2, list.getSelectedIndex());
    }

    /**
     * Shrinking a list to the point where no row fits must still terminate.
     */
    @Test
    void shrinkingHeightBelowOneRowTerminates() {
        TList list = new TList(null, items(10), 0, 0, 20, 8);
        list.setSelectedIndex(9);

        assertTerminates(() -> {
            list.setHeight(1);
            list.setHeight(0);
        });

        assertEquals(9, list.getSelectedIndex());
    }

    /**
     * Resizing step by step, as happens while a window is dragged smaller,
     * must terminate at every step and keep the selection.
     */
    @Test
    void repeatedResizeKeepsSelectionAndTerminates() {
        TList list = new TList(null, items(5), 0, 0, 20, 10);
        list.setSelectedIndex(4);

        assertTerminates(() -> {
            for (int height = 10; height >= 1; height--) {
                list.setHeight(height);
            }
        });

        assertEquals(4, list.getSelectedIndex());
    }

    /**
     * Appending an entry, selecting it, and resizing is the pattern used by
     * log-style windows; it must terminate however small the widget gets.
     */
    @Test
    void appendSelectLastAndResizeTerminates() {
        List<String> history = new ArrayList<String>(Arrays.asList("a", "b"));
        TList list = new TList(null, history, 0, 0, 20, 3);

        assertTerminates(() -> {
            for (int i = 0; i < 10; i++) {
                history.add("entry " + i);
                list.setList(history);
                list.setSelectedIndex(history.size() - 1);
                list.setHeight(2);
            }
        });

        assertEquals(history.size() - 1, list.getSelectedIndex());
    }

    /**
     * Selecting an item below the visible area scrolls just far enough to
     * make it the last visible row.
     */
    @Test
    void selectingBelowTheViewScrollsItIntoView() {
        TList list = new TList(null, items(20), 0, 0, 20, 6);

        list.setSelectedIndex(9);

        assertEquals(9, list.getSelectedIndex());
        // 5 rows are visible (the last row holds the horizontal scrollbar),
        // so item 5 must be at the top for item 9 to be at the bottom.
        assertEquals(5, list.getVerticalValue());
    }

    /**
     * Selecting an item that already fits on screen does not scroll.
     */
    @Test
    void selectingInsideTheViewDoesNotScroll() {
        TList list = new TList(null, items(20), 0, 0, 20, 6);

        list.setSelectedIndex(3);

        assertEquals(3, list.getSelectedIndex());
        assertEquals(0, list.getVerticalValue());
    }

    /**
     * After the widget is made shorter the selection is still on a visible
     * row.
     */
    @Test
    void selectionStaysVisibleAfterShrinking() {
        TList list = new TList(null, items(20), 0, 0, 20, 12);
        list.setSelectedIndex(19);

        list.setHeight(5);

        int firstVisible = list.getVerticalValue();
        assertTrue(list.getSelectedIndex() >= firstVisible,
            "selection scrolled off the top");
        assertTrue(list.getSelectedIndex() < firstVisible + list.getHeight() - 1,
            "selection scrolled off the bottom");
    }

    /**
     * The vertical scrollbar range follows the height: growing the widget
     * must not leave it scrolled past the end of the list.
     */
    @Test
    void growingHeightClampsScrollToTheEndOfTheList() {
        TList list = new TList(null, items(10), 0, 0, 20, 4);
        list.setSelectedIndex(9);
        assertEquals(7, list.getVerticalValue());

        list.setHeight(11);

        assertEquals(0, list.getBottomValue());
        assertEquals(0, list.getVerticalValue());
    }

}
