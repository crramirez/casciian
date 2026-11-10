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
package casciian.bits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lays out width-independent {@link RichText} as display lines of attributed
 * cells, independently of the source text format.
 */
public final class RichTextLayout {

    /**
     * A single laid-out line containing cells.
     */
    public static final class Line {
        private final List<Cell> cells;
        private final int sourceLine;
        private final RichText.Run imageRun;

        Line(final List<Cell> cells, final int sourceLine) {
            this(cells, sourceLine, null);
        }

        Line(final List<Cell> cells, final int sourceLine,
                final RichText.Run imageRun) {
            this.cells = List.copyOf(cells);
            this.sourceLine = sourceLine;
            this.imageRun = imageRun;
        }

        /**
         * Get the cells for this line.
         *
         * @return unmodifiable list of cells
         */
        public List<Cell> getCells() {
            return cells;
        }

        /**
         * Get the display width of this line (number of cells).
         *
         * @return number of cell columns
         */
        public int getWidth() {
            return cells.size();
        }

        /**
         * Get the zero-based logical source line (counting line feeds in the
         * rich text, including image alternative text and line feeds absorbed
         * at image block boundaries) on which this display line starts.
         *
         * @return the logical source line
         */
        public int getSourceLine() {
            return sourceLine;
        }

        /**
         * Get the image run rendered on this line.
         *
         * @return the image run whose rendered cells occupy this line, or
         * null for text lines (including alternative text shown in place of
         * an image that could not be rendered)
         */
        public RichText.Run getImageRun() {
            return imageRun;
        }
    }

    /**
     * Private constructor - utility class.
     */
    private RichTextLayout() {
    }

    /**
     * Lay out a {@link RichText} into a list of display lines with attributed
     * cells, wrapping at the given width and simulating the behavior of
     * {@code cat} in a terminal of the specified width.
     *
     * <p>
     * Run text is interpreted character by character, honoring newline (LF),
     * carriage return (CR), tab (HT), backspace (BS), wide (double-width)
     * characters, and delayed wrapping at the width boundary.
     * </p>
     *
     * @param richText the styled text to lay out
     * @param width the display width for wrapping (columns)
     * @return list of laid-out lines
     */
    public static List<Line> layout(final RichText richText, final int width) {
        return layout(richText, width, run -> null);
    }

    /**
     * Lay out text and standalone image blocks. The renderer receives an image
     * run and returns rows of cells already fitted to {@code width}; returning
     * null displays the alternative text instead. This callback permits screen
     * pixel metrics and image protocols to remain outside the text model.
     * One adjacent newline is absorbed at each block boundary; additional
     * newlines retain their blank rows. Without a renderer, the two-argument
     * overload always uses alternative text.
     *
     * @param richText model to lay out
     * @param width maximum number of cell columns
     * @param imageRenderer renderer for image runs (must not be null)
     * @return laid-out rows
     */
    public static List<Line> layout(final RichText richText, final int width,
            final java.util.function.Function<RichText.Run,
                List<List<Cell>>> imageRenderer) {
        return layout(richText, width, false, imageRenderer);
    }

    /**
     * Lay out text and standalone image blocks, optionally wrapping at word
     * boundaries.
     *
     * <p>
     * When {@code wordWrap} is true, a line that overflows is broken after
     * the last space or tab on it, so whole words move to the next line; the
     * breaking whitespace is dropped. Words longer than the width, or lines
     * without any break opportunity, still wrap at the character boundary.
     * When false, this behaves like {@link #layout(RichText, int,
     * java.util.function.Function)}.
     * </p>
     *
     * @param richText model to lay out
     * @param width maximum number of cell columns
     * @param wordWrap if true, wrap at word boundaries
     * @param imageRenderer renderer for image runs (must not be null)
     * @return laid-out rows
     */
    public static List<Line> layout(final RichText richText, final int width,
            final boolean wordWrap,
            final java.util.function.Function<RichText.Run,
                List<List<Cell>>> imageRenderer) {
        if (richText == null || width <= 0) {
            return Collections.emptyList();
        }

        List<Line> lines = new ArrayList<>();
        List<Cell> currentLine = new ArrayList<>();
        int col = 0;
        int source = 0;
        boolean afterImage = false;
        boolean skipImageNewline = false;
        boolean pendingTabBoundary = false;

        for (RichText.Run run : richText.getRuns()) {
            CellAttributes attr = run.attributes();
            if (run.isImage()) {
                if (!currentLine.isEmpty()) {
                    lines.add(new Line(currentLine, source));
                    currentLine = new ArrayList<>();
                }
                List<List<Cell>> imageRows = imageRenderer.apply(run);
                if (imageRows == null || imageRows.isEmpty()) {
                    for (Line line : layout(RichText.builder()
                            .append(run.getText(), attr).build(), width,
                            wordWrap, r -> null)) {
                        lines.add(new Line(line.getCells(),
                            source + line.getSourceLine()));
                    }
                } else {
                    for (List<Cell> row : imageRows) {
                        lines.add(new Line(row, source, run));
                    }
                }
                source += (int) run.getText().chars()
                    .filter(c -> c == '\n').count();
                col = 0;
                afterImage = true;
                skipImageNewline = true;
                continue;
            }
            String runText = run.getText();
            int i = 0;
            while (i < runText.length()) {
                int ch = runText.codePointAt(i);
                int charCount = Character.charCount(ch);
                if (skipImageNewline) {
                    if (ch == '\r') {
                        i += charCount;
                        continue;
                    }
                    skipImageNewline = false;
                    if (ch == '\n') {
                        source++;
                        i += charCount;
                        continue;
                    }
                }
                afterImage = false;

                if (ch == '\n') {
                    // Line feed: finish current line, start new one
                    lines.add(new Line(currentLine, source));
                    currentLine = new ArrayList<>();
                    col = 0;
                    source++;
                    pendingTabBoundary = false;
                } else if (ch == '\r') {
                    // Carriage return: move cursor to beginning of line
                    col = 0;
                    pendingTabBoundary = false;
                } else if (ch == '\t') {
                    // Tab: advance to next tab stop (every 8 columns)
                    int previousCol = col;
                    int nextTab = ((col / 8) + 1) * 8;
                    while (col < nextTab && col < width) {
                        putCell(currentLine, col, ' ', attr);
                        col++;
                    }
                    pendingTabBoundary = wordWrap && previousCol == width;
                    // Delayed wrap: leave the cursor at column==width and
                    // only wrap when the next printable character arrives.
                } else if (ch == '\b') {
                    // Backspace: move cursor back one column
                    if (col > 0) {
                        col--;
                    }
                    pendingTabBoundary = false;
                } else if (ch >= 0x20) {
                    // Printable character
                    int charWidth = StringUtils.width(ch);
                    if (col > 0 && col + charWidth > width) {
                        // Delayed wrap: the previous character filled the
                        // line, so wrap now that another character arrives.
                        List<Cell> carried = new ArrayList<>();
                        int carriedCol = 0;
                        if (wordWrap && ch != ' ' && !pendingTabBoundary) {
                            int brk = lastBreak(currentLine, col);
                            if (brk > 0) {
                                carried.addAll(currentLine.subList(brk + 1,
                                        currentLine.size()));
                                currentLine = new ArrayList<>(
                                    currentLine.subList(0, brk));
                                carriedCol = col - brk - 1;
                            }
                        }
                        lines.add(new Line(currentLine, source));
                        currentLine = carried;
                        col = carriedCol;
                        if (wordWrap && ch == ' ') {
                            // The space is the break itself: drop it.
                            pendingTabBoundary = false;
                            i += charCount;
                            continue;
                        }
                    }
                    pendingTabBoundary = false;
                    putCell(currentLine, col, ch, attr);
                    col++;
                    if (charWidth == 2 && col < width) {
                        // Wide character takes two columns; put a padding
                        // space for the right half (unless it would overflow
                        // the configured width, e.g. width == 1).
                        putCell(currentLine, col, ' ', attr);
                        col++;
                    }
                }
                // else: ignore other control characters

                i += charCount;
            }
        }

        // Add the last line if it still has buffered content, or if the
        // cursor is at column 0 with nothing buffered (empty input or a
        // trailing newline) so a final empty line is emitted.
        if (!currentLine.isEmpty() || (col == 0 && !afterImage)) {
            lines.add(new Line(currentLine, source));
        }

        return lines;
    }

    /**
     * Find the last whitespace column before {@code col} that is a word
     * break opportunity, skipping the padding half of wide characters.
     *
     * @return the column index, or -1 if there is none
     */
    private static int lastBreak(final List<Cell> line, final int col) {
        for (int i = Math.min(col, line.size()) - 1; i >= 0; i--) {
            if (line.get(i).isImage() || line.get(i).getChar() != ' ') {
                continue;
            }
            if (i > 0 && StringUtils.width(line.get(i - 1).getChar()) == 2) {
                continue;
            }
            return i;
        }
        return -1;
    }

    /**
     * Place a character cell at the given column in the line, extending
     * the line if necessary.
     */
    private static void putCell(final List<Cell> line, final int col,
            final int ch, final CellAttributes attr) {
        // Extend line to reach the column
        while (line.size() <= col) {
            Cell pad = new Cell();
            pad.setDefaultColor(true, true);
            pad.setDefaultColor(false, true);
            line.add(pad);
        }
        Cell cell = new Cell();
        cell.setTo(attr);
        cell.setChar(ch);
        line.set(col, cell);
    }
}
