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
import java.util.function.IntUnaryOperator;

/**
 * AnsiParser is a utility class that parses text containing ANSI escape
 * sequences (SGR sequences for colors and attributes) and produces a grid
 * of {@link Cell} objects ready for display.
 *
 * <p>
 * It simulates the behavior of the {@code cat} command: text is placed
 * character by character, honoring newline (LF), carriage return (CR),
 * tab (HT), and wrapping at the specified width. ANSI CSI SGR sequences
 * (ESC [ ... m) are interpreted for foreground/background colors, bold,
 * underline, blink, and reverse attributes.
 * </p>
 *
 * <p>
 * This class is extracted from and inspired by the SGR parsing logic in
 * the {@code ECMA48} terminal emulator, but is much simpler since it only
 * handles display rendering without terminal emulation.
 * </p>
 */
public final class AnsiParser {

    /**
     * Result of parsing ANSI text into a RichText model plus the final parser
     * attributes after all escape sequences have been applied.
     */
    public static final class RichTextParseResult {
        /**
         * Parsed styled text.
         */
        private final RichText richText;

        /**
         * Final parser state after the text has been consumed.
         */
        private final CellAttributes finalAttributes;

        /**
         * Public constructor.
         *
         * @param richText the parsed rich text
         * @param finalAttributes the final parser attributes
         */
        public RichTextParseResult(final RichText richText,
                final CellAttributes finalAttributes) {

            this.richText = richText;
            this.finalAttributes = new CellAttributes(finalAttributes);
        }

        /**
         * Get the parsed rich text.
         *
         * @return the parsed rich text
         */
        public RichText getRichText() {
            return richText;
        }

        /**
         * Get the final parser attributes.
         *
         * @return a copy of the final parser attributes
         */
        public CellAttributes getFinalAttributes() {
            return new CellAttributes(finalAttributes);
        }
    }

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Palette lookup using the shared default xterm 256-color palette.
     */
    private static final IntUnaryOperator DEFAULT_PALETTE =
        SgrUtil::getDefaultIndexedColor;

    // ------------------------------------------------------------------------
    // Inner class: parsed line -----------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * A single parsed line containing cells.
     */
    public static final class Line {
        private final List<Cell> cells;

        Line(final List<Cell> cells) {
            this.cells = List.copyOf(cells);
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
    }

    // ------------------------------------------------------------------------
    // Parser state machine ---------------------------------------------------
    // ------------------------------------------------------------------------

    private enum State {
        GROUND,
        ESCAPE,
        CSI_PARAM
    }

    // ------------------------------------------------------------------------
    // Public API --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Private constructor - utility class.
     */
    private AnsiParser() {
    }

    /**
     * Parse a string containing ANSI escape sequences and produce a list
     * of display lines with attributed cells. Lines are wrapped at the
     * given width, simulating the behavior of {@code cat} in a terminal
     * of the specified width.
     *
     * <p>
     * This is a convenience method equivalent to
     * {@code layout(toRichText(text), width)}: the ANSI text is first parsed
     * into a width-independent {@link RichText} model, which is then laid out
     * (wrapped) for the given width.
     * </p>
     *
     * @param text the input text (may contain ANSI escape sequences)
     * @param width the display width for wrapping (columns)
     * @return list of parsed lines
     */
    public static List<Line> parse(final String text, final int width) {
        if (text == null || width <= 0) {
            return Collections.emptyList();
        }
        return layout(toRichText(text), width);
    }

    /**
     * Parse a string containing ANSI escape sequences into a width-independent
     * {@link RichText} model.  SGR sequences (colors and attributes) and OSC 8
     * hyperlinks are interpreted; all other escape sequences are ignored.
     *
     * <p>
     * Control characters that affect layout (LF, CR, HT, BS) are preserved in
     * the run text and interpreted later by {@link #layout(RichText, int)}.
     * </p>
     *
     * @param text the input text (may contain ANSI escape sequences)
     * @return the parsed RichText (never null; empty when text is null)
     */
    public static RichText toRichText(final String text) {
        CellAttributes initialAttr = new CellAttributes();
        initialAttr.setDefaultColor(true, true);
        initialAttr.setDefaultColor(false, true);
        return parseRichText(text, initialAttr).getRichText();
    }

    /**
     * Parse a string containing ANSI escape sequences into a width-independent
     * {@link RichText} model, starting from a given SGR/hyperlink state.  This
     * allows an appended fragment to inherit the attributes that were active at
     * the end of a previously parsed source, so that parsing the fragment is
     * equivalent to concatenating it onto that source.
     *
     * @param text the input text (may contain ANSI escape sequences)
     * @param initialAttr the attributes active at the start of the text; a copy
     * is taken so the caller's instance is not modified (null uses the default
     * attributes)
     * @return the parsed RichText (never null; empty when text is null)
     */
    public static RichText toRichText(final String text,
        final CellAttributes initialAttr) {

        return parseRichText(text, initialAttr).getRichText();
    }

    /**
     * Parse a string containing ANSI escape sequences into a width-independent
     * {@link RichText} model and return the final parser attributes after all
     * escape sequences have been applied.
     *
     * @param text the input text (may contain ANSI escape sequences)
     * @param initialAttr the attributes active at the start of the text; a copy
     * is taken so the caller's instance is not modified (null uses the default
     * attributes)
     * @return the parsed RichText plus the final parser attributes
     */
    public static RichTextParseResult parseRichText(final String text,
            final CellAttributes initialAttr) {

        RichText.Builder builder = RichText.builder();
        if (text == null) {
            CellAttributes defaultAttr = new CellAttributes();
            defaultAttr.setDefaultColor(true, true);
            defaultAttr.setDefaultColor(false, true);
            return new RichTextParseResult(builder.build(),
                initialAttr == null ? defaultAttr : initialAttr);
        }

        CellAttributes currentAttr = new CellAttributes();
        if (initialAttr == null) {
            currentAttr.setDefaultColor(true, true);
            currentAttr.setDefaultColor(false, true);
        } else {
            currentAttr.setTo(initialAttr);
        }

        StringBuilder buffer = new StringBuilder();
        State state = State.GROUND;
        StringBuilder csiParams = new StringBuilder();

        int i = 0;
        while (i < text.length()) {
            int ch = text.codePointAt(i);
            int charCount = Character.charCount(ch);

            switch (state) {
            case GROUND:
                if (ch == 0x1B) {
                    // ESC
                    state = State.ESCAPE;
                } else if (ch == '\n' || ch == '\r' || ch == '\t'
                        || ch == '\b' || ch >= 0x20) {
                    // Printable characters and layout-affecting control
                    // characters are buffered verbatim; the layout stage
                    // interprets them.
                    buffer.appendCodePoint(ch);
                }
                // else: ignore other control characters
                break;

            case ESCAPE:
                if (ch == '[') {
                    // CSI sequence
                    state = State.CSI_PARAM;
                    csiParams.setLength(0);
                } else if (ch == ']') {
                    // OSC sequence: collect until ST (BEL or ESC \)
                    i += charCount;
                    StringBuilder osc = new StringBuilder();
                    while (i < text.length()) {
                        int oscCh = text.codePointAt(i);
                        if (oscCh == 0x07) {
                            break; // BEL terminates
                        }
                        if (oscCh == 0x1B && i + 1 < text.length()
                                && text.charAt(i + 1) == '\\') {
                            i++; // skip the backslash
                            break;
                        }
                        osc.appendCodePoint(oscCh);
                        i += Character.charCount(oscCh);
                    }
                    // The buffered text used the pre-change attributes.
                    flush(builder, buffer, currentAttr);
                    applyOsc(osc.toString(), currentAttr);
                    state = State.GROUND;
                } else {
                    // Unknown escape sequence, ignore and return to ground
                    state = State.GROUND;
                    continue; // re-process this character in GROUND state
                }
                break;

            case CSI_PARAM:
                if ((ch >= 0x30 && ch <= 0x3F)) {
                    // Parameter bytes: 0-9, ;, :, <, =, >, ?
                    csiParams.append((char) ch);
                } else if (ch >= 0x20 && ch <= 0x2F) {
                    // Intermediate bytes - ignore for our purposes
                    csiParams.append((char) ch);
                } else if (ch >= 0x40 && ch <= 0x7E) {
                    // Final byte
                    if (ch == 'm') {
                        // SGR sequence: flush text styled with the old attrs
                        // before mutating them.
                        flush(builder, buffer, currentAttr);
                        applySgr(csiParams.toString(), currentAttr);
                    }
                    // All other CSI sequences are ignored
                    state = State.GROUND;
                } else {
                    // Invalid, abort sequence
                    state = State.GROUND;
                }
                break;
            }

            i += charCount;
        }

        flush(builder, buffer, currentAttr);
        return new RichTextParseResult(builder.build(), currentAttr);
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
        if (richText == null || width <= 0) {
            return Collections.emptyList();
        }

        List<Line> lines = new ArrayList<>();
        List<Cell> currentLine = new ArrayList<>();
        int col = 0;

        for (RichText.Run run : richText.getRuns()) {
            CellAttributes attr = run.attributes();
            String runText = run.getText();
            int i = 0;
            while (i < runText.length()) {
                int ch = runText.codePointAt(i);
                int charCount = Character.charCount(ch);

                if (ch == '\n') {
                    // Line feed: finish current line, start new one
                    lines.add(new Line(currentLine));
                    currentLine = new ArrayList<>();
                    col = 0;
                } else if (ch == '\r') {
                    // Carriage return: move cursor to beginning of line
                    col = 0;
                } else if (ch == '\t') {
                    // Tab: advance to next tab stop (every 8 columns)
                    int nextTab = ((col / 8) + 1) * 8;
                    while (col < nextTab && col < width) {
                        putCell(currentLine, col, ' ', attr);
                        col++;
                    }
                    // Delayed wrap: leave the cursor at column==width and
                    // only wrap when the next printable character arrives.
                } else if (ch == '\b') {
                    // Backspace: move cursor back one column
                    if (col > 0) {
                        col--;
                    }
                } else if (ch >= 0x20) {
                    // Printable character
                    int charWidth = StringUtils.width(ch);
                    if (col > 0 && col + charWidth > width) {
                        // Delayed wrap: the previous character filled the
                        // line, so wrap now that another character arrives.
                        lines.add(new Line(currentLine));
                        currentLine = new ArrayList<>();
                        col = 0;
                    }
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
        if (!currentLine.isEmpty() || col == 0) {
            lines.add(new Line(currentLine));
        }

        return lines;
    }

    /**
     * Flush the buffered text into the builder with the given attributes, then
     * clear the buffer.
     *
     * @param builder the target builder
     * @param buffer the text buffer (cleared afterwards)
     * @param attr the attributes for the buffered text
     */
    private static void flush(final RichText.Builder builder,
            final StringBuilder buffer, final CellAttributes attr) {

        if (buffer.length() > 0) {
            builder.append(buffer.toString(), attr);
            buffer.setLength(0);
        }
    }

    // ------------------------------------------------------------------------
    // Private helpers ---------------------------------------------------------
    // ------------------------------------------------------------------------

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

    /**
     * Apply an OSC (Operating System Command) sequence.  Only OSC 8
     * hyperlinks are interpreted; all other OSC sequences are ignored.
     *
     * <p>
     * OSC 8 has the form {@code 8 ; params ; URI}.  A non-empty URI opens (or
     * changes) the hyperlink applied to subsequent cells; an empty URI closes
     * the current hyperlink.
     * </p>
     *
     * @param osc the OSC body (without the leading ESC ] and trailing ST)
     * @param attr the attributes to modify
     */
    private static void applyOsc(final String osc,
            final CellAttributes attr) {

        // OSC 8 hyperlink.  Do not split on ';' generally, because the URI
        // may itself contain ';'.  Extract the URI as everything after the
        // second ';'.
        if (osc.equals("8") || osc.startsWith("8;")) {
            int firstSemi = osc.indexOf(';');
            int secondSemi = (firstSemi < 0)
                ? -1 : osc.indexOf(';', firstSemi + 1);
            if (secondSemi >= 0) {
                String uri = osc.substring(secondSemi + 1);
                attr.setHyperlink(uri.isEmpty() ? null : uri);
            } else {
                attr.setHyperlink(null);
            }
        }
    }

    /**
     * Apply an SGR (Select Graphic Rendition) parameter string to the
     * given attributes. Delegates to {@link SgrUtil} for the common
     * color/attribute codes.
     *
     * @param params the parameter string (e.g., "1;31" for bold red)
     * @param attr the attributes to modify
     */
    private static void applySgr(final String params,
            final CellAttributes attr) {
        if (params.isEmpty()) {
            // ESC[m (no params) is equivalent to ESC[0m (reset).
            SgrUtil.resetToDefaults(attr);
            return;
        }

        SgrUtil.ExtendedColorState extColor = new SgrUtil.ExtendedColorState();
        String[] groups = params.split(";", -1);

        for (String group : groups) {
            String[] subParams = group.split(":", -1);
            if (subParams.length > 1) {
                int prefix;
                try {
                    prefix = subParams[0].isEmpty() ? 0
                        : Integer.parseInt(subParams[0]);
                } catch (NumberFormatException e) {
                    continue;
                }
                if (prefix == 4) {
                    int style = CellAttributes.UNDERLINE_STYLE_SINGLE;
                    if (subParams.length >= 2 && !subParams[1].isEmpty()) {
                        try {
                            style = Integer.parseInt(subParams[1]);
                        } catch (NumberFormatException e) {
                            style = CellAttributes.UNDERLINE_STYLE_SINGLE;
                        }
                    }
                    if (SgrUtil.applyUnderlineStyleCode(style, attr)) {
                        continue;
                    }
                    attr.setUnderlineStyle(
                        CellAttributes.UNDERLINE_STYLE_SINGLE);
                    continue;
                }
            }
            for (String part : subParams) {
                int value;
                try {
                    value = part.isEmpty() ? 0 : Integer.parseInt(part);
                } catch (NumberFormatException e) {
                    continue;
                }

                // Handle extended color sub-parameters (38;5;n or 38;2;r;g;b)
                if (extColor.isActive()) {
                    extColor.feedValue(value, attr, DEFAULT_PALETTE);
                    continue;
                }

                // Try the shared SGR handler
                if (SgrUtil.applySgrCode(value, attr, DEFAULT_PALETTE)) {
                    continue;
                }

                // Codes 38/48 start an extended color sequence
                if (value == 38 || value == 48) {
                    extColor.begin(value);
                }
            }
        }
    }
}
