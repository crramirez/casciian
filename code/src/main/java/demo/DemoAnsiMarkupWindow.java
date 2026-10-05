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
package demo;

import casciian.TApplication;
import casciian.TEditor;
import casciian.TSplitPane;
import casciian.TTextAnsi;
import casciian.TWidget;
import casciian.TWindow;
import casciian.bits.AnsiParser;
import casciian.bits.CasciianMarkupParser;
import casciian.event.TCommandEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TMenuEvent;
import casciian.event.TMouseEvent;
import casciian.event.TResizeEvent;

/**
 * This window demonstrates the RichText pipeline.  ANSI text is parsed into
 * a RichText model and exported as Casciian markup into an editor on the
 * left.  Every edit re-parses the markup and replaces the rich text shown in
 * the viewer on the right, keeping the viewer's vertical scroll position.
 */
public class DemoAnsiMarkupWindow extends TWindow {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The split pane holding the editor and the viewer.
     */
    private final TSplitPane splitPane;

    /**
     * The markup editor on the left.
     */
    private final MarkupEditor editor;

    /**
     * The rich text viewer on the right.
     */
    private final TTextAnsi viewer;

    /**
     * The last markup that was rendered in the viewer.
     */
    private String lastMarkup;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent the main application
     * @param title the window title
     * @param ansiText the text to display (may contain ANSI escape sequences)
     */
    @SuppressWarnings("this-escape")
    public DemoAnsiMarkupWindow(final TApplication parent, final String title,
        final String ansiText) {

        super(parent, title, 0, 0, parent.getScreen().getWidth(),
            parent.getDesktopBottom() - parent.getDesktopTop(), RESIZABLE);

        lastMarkup = CasciianMarkupParser.toMarkup(
            AnsiParser.toRichText(ansiText));

        splitPane = new TSplitPane(this, 0, 0, getWidth() - 2,
            getHeight() - 2, true);
        // Children are created on the window and then moved into the split
        // pane, which requires its left/right links to be set first.
        editor = new MarkupEditor(this, lastMarkup);
        viewer = new TTextAnsi(this, "", 0, 0, 1, 1);
        viewer.setMarkup(lastMarkup);
        splitPane.setLeft(editor);
        splitPane.setRight(viewer);
        editor.activate();
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle window/screen resize events.
     *
     * @param event resize event
     */
    @Override
    public void onResize(final TResizeEvent event) {
        if (event.getType() == TResizeEvent.Type.WIDGET) {
            splitPane.onResize(new TResizeEvent(event.getBackend(),
                TResizeEvent.Type.WIDGET, event.getWidth() - 2,
                event.getHeight() - 2));
            return;
        }

        // Pass to children instead
        for (TWidget widget : getChildren()) {
            widget.onResize(event);
        }
    }

    // ------------------------------------------------------------------------
    // DemoAnsiMarkupWindow ---------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Re-render the viewer if the editor markup changed.  The viewer keeps
     * its vertical scroll position (clamped to the new content).
     */
    private void updateViewer() {
        String markup = editor.getText();
        if (markup.equals(lastMarkup)) {
            return;
        }
        lastMarkup = markup;
        int top = viewer.getVerticalValue();
        viewer.setMarkup(markup);
        viewer.setVerticalValue(Math.min(top, viewer.getBottomValue()));
    }

    /**
     * TEditor that notifies the window after every event that may modify
     * its text.
     */
    private class MarkupEditor extends TEditor {

        /**
         * Public constructor.
         *
         * @param parent parent widget
         * @param text initial text
         */
        MarkupEditor(final TWidget parent, final String text) {
            super(parent, text, 0, 0, 1, 1);
        }

        @Override
        public void onKeypress(final TKeypressEvent keypress) {
            super.onKeypress(keypress);
            updateViewer();
        }

        @Override
        public void onMouseDown(final TMouseEvent mouse) {
            super.onMouseDown(mouse);
            updateViewer();
        }

        @Override
        public void onMouseUp(final TMouseEvent mouse) {
            super.onMouseUp(mouse);
            updateViewer();
        }

        @Override
        public void onCommand(final TCommandEvent command) {
            super.onCommand(command);
            updateViewer();
        }

        @Override
        public void onMenu(final TMenuEvent menu) {
            super.onMenu(menu);
            updateViewer();
        }
    }

}
