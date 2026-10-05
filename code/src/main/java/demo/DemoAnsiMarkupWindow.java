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

import java.util.ArrayList;
import java.util.List;

import casciian.TApplication;
import casciian.TEditor;
import casciian.THScroller;
import casciian.TScrollable;
import casciian.TSplitPane;
import casciian.TTextAnsi;
import casciian.TVScroller;
import casciian.TWidget;
import casciian.TWindow;
import casciian.bits.AnsiParser;
import casciian.bits.CasciianMarkupParser;
import casciian.bits.RichText;
import casciian.event.TCommandEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TMenuEvent;
import casciian.event.TMouseEvent;
import casciian.event.TResizeEvent;

/**
 * This window demonstrates the RichText pipeline.  ANSI text is parsed into
 * a RichText model and exported as Casciian markup into an editor on the
 * left.  Every edit re-parses the markup and replaces the rich text shown in
 * the viewer on the right.
 *
 * <p>
 * The two panes scroll together the way IDE markdown previews do: each
 * source line of the markup is anchored to the first wrapped line it
 * produces in the viewer.  Scrolling the editor moves the viewer to the
 * anchor of the editor's top line, and scrolling the viewer moves the editor
 * to the source line that owns the viewer's top line.  The side whose
 * position differs from the last synchronized position is the one the user
 * moved, which avoids feedback loops between the two panes.
 * </p>
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
     * The scrollable pane wrapping the markup editor on the left.
     */
    private final MarkupEditorPane editorPane;

    /**
     * The markup editor.
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

    /**
     * The editor top line (0-based) at the last synchronization.
     */
    private int syncedEditorLine = -1;

    /**
     * The viewer top line (0-based) at the last synchronization.
     */
    private int syncedViewerLine = -1;

    /**
     * If true, the next synchronization moves the viewer to the editor
     * position regardless of which side changed.
     */
    private boolean forceEditorToViewer = true;

    /**
     * The rich text the source line map was computed for.
     */
    private RichText mappedText;

    /**
     * The viewer width the source line map was computed for.
     */
    private int mappedWidth = -1;

    /**
     * For each source line, the index of the first viewer line it produces.
     */
    private int[] sourceLineStarts = {0};

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
        editorPane = new MarkupEditorPane(this, lastMarkup);
        editor = editorPane.editor;
        viewer = new TTextAnsi(this, "", 0, 0, 1, 1);
        viewer.setMarkup(lastMarkup);
        splitPane.setLeft(editorPane);
        splitPane.setRight(viewer);
        editorPane.activate();
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

    /**
     * Synchronize the scroll positions before the panes are drawn.  This
     * runs after every input, including captured scrollbar drags and
     * auto-repeat that bypass this window's event handlers.
     */
    @Override
    public void draw() {
        synchronizeScrolling();
        super.draw();
    }

    // ------------------------------------------------------------------------
    // DemoAnsiMarkupWindow ---------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Re-render the viewer if the editor markup changed.
     */
    private void updateViewer() {
        String markup = editor.getText();
        if (markup.equals(lastMarkup)) {
            return;
        }
        lastMarkup = markup;
        viewer.setMarkup(markup);
        forceEditorToViewer = true;
    }

    /**
     * Synchronize the editor scrollbars with the editor, then the editor
     * with the viewer.
     */
    private void synchronizeScrolling() {
        editorPane.syncScrollbars();

        int[] starts = getSourceLineStarts();
        int editorLine = editor.getVisibleRowNumber() - 1;
        int viewerLine = viewer.getVerticalValue();

        if (forceEditorToViewer || (editorLine != syncedEditorLine)) {
            int source = Math.min(editorLine, starts.length - 1);
            viewer.setVerticalValue(Math.min(starts[source],
                viewer.getBottomValue()));
        } else if (viewerLine != syncedViewerLine) {
            int source = sourceLineAt(starts, viewerLine);
            editor.setVisibleRowNumber(Math.min(source + 1,
                editor.getLineCount()));
            editorPane.syncScrollbars();
        }

        forceEditorToViewer = false;
        syncedEditorLine = editor.getVisibleRowNumber() - 1;
        syncedViewerLine = viewer.getVerticalValue();
    }

    /**
     * Get the map from source line to first viewer line, recomputing it
     * when the viewer text or width changed.
     *
     * @return the viewer line where each source line starts
     */
    private int[] getSourceLineStarts() {
        RichText text = viewer.getRichText();
        int width = Math.max(1, viewer.getWidth() - 1);
        if ((text == mappedText) && (width == mappedWidth)) {
            return sourceLineStarts;
        }
        mappedText = text;
        mappedWidth = width;
        forceEditorToViewer = true;

        // Markup newlines are literal, so source line N of the markup is
        // logical line N of the rich text.  Lay out each logical line at the
        // viewer width to find how many viewer lines it wraps to.
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        int displayLine = 0;
        RichText.Builder line = RichText.builder();
        for (RichText.Run run : text.getRuns()) {
            String runText = run.getText();
            int from = 0;
            int newline = runText.indexOf('\n');
            while (newline >= 0) {
                line.append(runText.substring(from, newline),
                    run.getAttributes());
                displayLine += AnsiParser.layout(line.build(), width).size();
                starts.add(displayLine);
                line = RichText.builder();
                from = newline + 1;
                newline = runText.indexOf('\n', from);
            }
            line.append(runText.substring(from), run.getAttributes());
        }

        sourceLineStarts = starts.stream().mapToInt(Integer::intValue)
            .toArray();
        return sourceLineStarts;
    }

    /**
     * Find the source line that produces the given viewer line.
     *
     * @param starts the viewer line where each source line starts
     * @param viewerLine the viewer line
     * @return the source line containing viewerLine
     */
    private static int sourceLineAt(final int[] starts,
        final int viewerLine) {

        int low = 0;
        int high = starts.length - 1;
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (starts[mid] <= viewerLine) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    /**
     * A TEditor with vertical and horizontal scrollbars kept in sync with
     * the editor's visible area.
     */
    private class MarkupEditorPane extends TScrollable {

        /**
         * The editor.
         */
        private final MarkupEditor editor;

        /**
         * The editor visible row (1-based) at the last synchronization.
         */
        private int syncedRow = 1;

        /**
         * The editor visible column (1-based) at the last synchronization.
         */
        private int syncedColumn = 1;

        /**
         * Public constructor.
         *
         * @param parent parent widget
         * @param text initial text
         */
        MarkupEditorPane(final TWidget parent, final String text) {
            super(parent, 0, 0, 2, 2);
            editor = new MarkupEditor(this, text);
            vScroller = new TVScroller(this, getWidth() - 1, 0,
                Math.max(1, getHeight() - 1));
            hScroller = new THScroller(this, 0, getHeight() - 1,
                Math.max(1, calculateHScrollerWidth()));
            setTopValue(1);
            setLeftValue(1);
            syncScrollbars();
        }

        @Override
        public void onResize(final TResizeEvent event) {
            super.onResize(event);
            if (event.getType() == TResizeEvent.Type.WIDGET) {
                editor.onResize(new TResizeEvent(event.getBackend(),
                    TResizeEvent.Type.WIDGET, Math.max(1, getWidth() - 1),
                    Math.max(1, getHeight() - 1)));
                syncScrollbars();
            }
        }

        /**
         * Synchronize the scrollbars and the editor.  A scrollbar that moved
         * since the last synchronization scrolls the editor; otherwise the
         * scrollbars follow the editor.
         */
        void syncScrollbars() {
            int lineCount = Math.max(1, editor.getLineCount());
            setBottomValue(lineCount);
            setRightValue(Math.max(1, editor.getMaximumColumnNumber() - 2));

            int row = editor.getVisibleRowNumber();
            int barRow = getVerticalValue();
            if ((barRow != syncedRow) && (barRow != row)) {
                editor.setVisibleRowNumber(Math.max(1,
                    Math.min(barRow, lineCount)));
                row = editor.getVisibleRowNumber();
            }
            setVerticalValue(row);
            syncedRow = row;

            int column = editor.getVisibleColumnNumber();
            int barColumn = getHorizontalValue();
            if ((barColumn != syncedColumn) && (barColumn != column)) {
                editor.setVisibleColumnNumber(Math.max(1,
                    Math.min(barColumn, getRightValue())));
                column = editor.getVisibleColumnNumber();
            }
            setHorizontalValue(column);
            syncedColumn = column;
        }
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

            setHighlighting(false);
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
