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
import casciian.THScroller;
import casciian.TScrollable;
import casciian.TSplitPane;
import casciian.TTextAnsi;
import casciian.TVScroller;
import casciian.TWidget;
import casciian.TWindow;
import casciian.bits.RichText;
import casciian.document.parsers.DocumentParserRegistry;
import casciian.event.TCommandEvent;
import casciian.event.TImageDoubleClickEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TMenuEvent;
import casciian.event.TMouseEvent;
import casciian.event.TResizeEvent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * A live Markdown editor: a split pane with a text editor on the left and
 * the rendered document on the right.  Every edit re-parses the Markdown
 * through the {@link DocumentParserRegistry} by MIME type and replaces the
 * rich text shown in the viewer.
 *
 * <p>Rendered Markdown lines do not map one-to-one to source lines, so the
 * panes scroll together proportionally: the side whose position differs
 * from the last synchronized position is the one the user moved, and the
 * other side follows it at the same relative position.</p>
 */
public class MarkdownEditorWindow extends TWindow {

    /**
     * MIME type used to select the Markdown parser.
     */
    private static final String MIME_TYPE = "text/markdown; charset=UTF-8";

    /**
     * The split pane holding the editor and the viewer.
     */
    private final TSplitPane splitPane;

    /**
     * The scrollable pane wrapping the Markdown editor on the left.
     */
    private final EditorPane editorPane;

    /**
     * The Markdown editor.
     */
    private final MarkdownEditor editor;

    /**
     * The rendered document viewer on the right.
     */
    private final TTextAnsi viewer;

    /**
     * Directory against which relative image paths are resolved, or null.
     */
    private final Path baseDirectory;

    /**
     * The last Markdown that was rendered in the viewer.
     */
    private String lastMarkdown;

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
     * Public constructor.
     *
     * @param parent the main application
     * @param title the window title
     * @param markdown the initial Markdown text
     * @param baseDirectory directory used to resolve relative image paths,
     * or null
     * @param imageListener called when an image in the viewer is
     * double-clicked, or null
     */
    @SuppressWarnings("this-escape")
    public MarkdownEditorWindow(final TApplication parent, final String title,
            final String markdown, final Path baseDirectory,
            final Consumer<TImageDoubleClickEvent> imageListener) {

        super(parent, title, 0, 0, parent.getScreen().getWidth(),
                parent.getDesktopBottom() - parent.getDesktopTop(),
                RESIZABLE);

        this.baseDirectory = baseDirectory;
        lastMarkdown = markdown;

        splitPane = new TSplitPane(this, 0, 0, getWidth() - 2,
                getHeight() - 2, true);
        // Children are created on the window and then moved into the split
        // pane, which requires its left/right links to be set first.
        editorPane = new EditorPane(this, markdown);
        editor = editorPane.editor;
        viewer = new TTextAnsi(this, "", 0, 0, 1, 1);
        if (imageListener != null) {
            viewer.addImageDoubleClickListener(imageListener);
        }
        render(markdown);
        splitPane.setLeft(editorPane);
        splitPane.setRight(viewer);
        splitPane.activate();
        editorPane.activate();
        editor.activate();
    }

    @Override
    public void onResize(final TResizeEvent event) {
        if (event.getType() == TResizeEvent.Type.WIDGET) {
            splitPane.onResize(new TResizeEvent(event.getBackend(),
                    TResizeEvent.Type.WIDGET, event.getWidth() - 2,
                    event.getHeight() - 2));
            return;
        }

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

    /**
     * Re-render the viewer if the editor text changed.
     */
    private void updateViewer() {
        String markdown = editor.getText();
        if (markdown.equals(lastMarkdown)) {
            return;
        }
        lastMarkdown = markdown;
        render(markdown);
        forceEditorToViewer = true;
    }

    /**
     * Parse Markdown and show it in the viewer.  On a parse error the
     * previous rendering is kept.
     *
     * @param markdown the Markdown text
     */
    private void render(final String markdown) {
        try {
            RichText text = DocumentParserRegistry.getInstance().parse(
                    new ByteArrayInputStream(
                            markdown.getBytes(StandardCharsets.UTF_8)),
                    MIME_TYPE, baseDirectory);
            viewer.setRichText(text);
        } catch (IOException | RuntimeException e) {
            // Keep the last good rendering while the user is typing.
        }
    }

    /**
     * Synchronize the editor scrollbars with the editor, then the editor
     * with the viewer.
     */
    private void synchronizeScrolling() {
        editorPane.syncScrollbars();

        int editorLine = editor.getVisibleRowNumber() - 1;
        int editorMax = Math.max(0, editor.getLineCount() - 1);
        int viewerLine = viewer.getVerticalValue();
        int viewerMax = Math.max(0, viewer.getBottomValue());

        if (forceEditorToViewer || (editorLine != syncedEditorLine)) {
            viewer.setVerticalValue(scale(editorLine, editorMax, viewerMax));
        } else if (viewerLine != syncedViewerLine) {
            editor.setVisibleRowNumber(scale(viewerLine, viewerMax,
                    editorMax) + 1);
            editorPane.syncScrollbars();
        }

        forceEditorToViewer = false;
        syncedEditorLine = editor.getVisibleRowNumber() - 1;
        syncedViewerLine = viewer.getVerticalValue();
    }

    /**
     * Map a position in [0, fromMax] to the same relative position in
     * [0, toMax].
     *
     * @param value the position
     * @param fromMax the maximum of the source range
     * @param toMax the maximum of the target range
     * @return the scaled position
     */
    private static int scale(final int value, final int fromMax,
            final int toMax) {

        if (fromMax <= 0 || toMax <= 0) {
            return 0;
        }
        long scaled = Math.round((double) value * toMax / fromMax);
        return (int) Math.max(0, Math.min(toMax, scaled));
    }

    /**
     * A TEditor with vertical and horizontal scrollbars kept in sync with
     * the editor's visible area.
     */
    private class EditorPane extends TScrollable {

        /**
         * The editor.
         */
        private final MarkdownEditor editor;

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
        EditorPane(final TWidget parent, final String text) {
            super(parent, 0, 0, 2, 2);
            editor = new MarkdownEditor(this, text);
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
                        TResizeEvent.Type.WIDGET,
                        Math.max(1, getWidth() - 1),
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
    private class MarkdownEditor extends TEditor {

        /**
         * Public constructor.
         *
         * @param parent parent widget
         * @param text initial text
         */
        MarkdownEditor(final TWidget parent, final String text) {
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
