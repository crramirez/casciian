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

import casciian.event.TResizeEvent;

/**
 * TTextAnsiWindow shows text containing ANSI escape sequences with
 * scrollbars in a resizable window.
 *
 * <p>
 * This is designed to display the output of commands like
 * {@code pandoc file.md -t ansi} in a standalone window.
 * </p>
 */
public class TTextAnsiWindow extends TWindow {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The TTextAnsi widget inside this window.
     */
    private TTextAnsi textAnsiField;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent the main application
     * @param title the window title
     * @param text the text to display (may contain ANSI escape sequences)
     */
    public TTextAnsiWindow(final TApplication parent, final String title,
            final String text) {

        this(parent, title, text, 0, 0, 82, 27);
    }

    /**
     * Public constructor.
     *
     * @param parent the main application
     * @param title the window title
     * @param text the text to display (may contain ANSI escape sequences)
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of window
     * @param height height of window
     */
    public TTextAnsiWindow(final TApplication parent, final String title,
            final String text, final int x, final int y,
            final int width, final int height) {

        this(parent, title, text, x, y, width, height, RESIZABLE);
    }

    /**
     * Public constructor.
     *
     * @param parent the main application
     * @param title the window title
     * @param text the text to display (may contain ANSI escape sequences)
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of window
     * @param height height of window
     * @param flags bitmask of RESIZABLE, CENTERED, or MODAL
     */
    @SuppressWarnings("this-escape")
    public TTextAnsiWindow(final TApplication parent, final String title,
            final String text, final int x, final int y,
            final int width, final int height, final int flags) {

        super(parent, title, x, y, width, height, flags);

        // Position TTextAnsi so its scrollbars overlay the window border
        textAnsiField = new TTextAnsi(this, text, 0, 0,
            getWidth() - 1, getHeight() - 1);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle window/screen resize events.
     *
     * @param event resize event
     */
    @Override
    public void onResize(final TResizeEvent event) {
        if (event.getType() == TResizeEvent.Type.WIDGET) {
            // Resize the text field so its scrollbars remain on the border
            TResizeEvent fieldSize = new TResizeEvent(event.getBackend(),
                TResizeEvent.Type.WIDGET, event.getWidth() - 1,
                event.getHeight() - 1);
            textAnsiField.onResize(fieldSize);
            return;
        }

        // Pass to children instead
        for (TWidget widget : getChildren()) {
            widget.onResize(event);
        }
    }

    // ------------------------------------------------------------------------
    // TTextAnsiWindow --------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the TTextAnsi widget.
     *
     * @return the TTextAnsi widget
     */
    public TTextAnsi getTextAnsiField() {
        return textAnsiField;
    }

    /**
     * Set the text content.
     *
     * @param text new text to display (may contain ANSI escape sequences)
     */
    public void setText(final String text) {
        textAnsiField.setText(text);
    }

    /**
     * Get the text content.
     *
     * @return the raw text
     */
    public String getText() {
        return textAnsiField.getText();
    }
}
