/*
 * Casciian - Java Text User Interface
 *
 * To the extent possible under law, the author(s) have dedicated all
 * copyright and related and neighboring rights to this software to the
 * public domain worldwide. This software is distributed without any
 * warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along
 * with this software. If not, see
 * <http://creativecommons.org/publicdomain/zero/1.0/>.
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
public class TTextAnsiWindow extends TScrollableWindow {

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

        textAnsiField = new TTextAnsi(this, text, 0, 0,
            getWidth() - 2, getHeight() - 2);

        setupAfterTextField();
    }

    /**
     * Setup scrollbars after the text field is created.
     */
    private void setupAfterTextField() {
        hScroller = new THScroller(this,
            Math.min(Math.max(0, getWidth() - 17), 17),
            getHeight() - 2,
            getWidth() - Math.min(Math.max(0, getWidth() - 17), 17) - 3);
        vScroller = new TVScroller(this, getWidth() - 2, 0, getHeight() - 2);
        reflowData();
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
            // Resize the text field
            TResizeEvent fieldSize = new TResizeEvent(event.getBackend(),
                TResizeEvent.Type.WIDGET, event.getWidth() - 2,
                event.getHeight() - 2);
            textAnsiField.onResize(fieldSize);

            // Have TScrollableWindow handle the scrollbars
            super.onResize(event);
            return;
        }

        // Pass to children instead
        for (TWidget widget : getChildren()) {
            widget.onResize(event);
        }
    }

    // ------------------------------------------------------------------------
    // TScrollableWindow ------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the window.
     */
    @Override
    public void draw() {
        reflowData();
        super.draw();
    }

    /**
     * Resize scrollbars for a new width/height.
     */
    @Override
    public void reflowData() {
        if (textAnsiField != null) {
            textAnsiField.reflowData();
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
