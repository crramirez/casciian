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
 * {@link casciian.terminal.ECMA48}, but is much simpler since it only
 * handles display rendering without terminal emulation.
 * </p>
 */
public final class AnsiParser {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The default 256-color palette matching xterm defaults.
     */
    private static final int[] COLORS_256;

    static {
        COLORS_256 = new int[256];
        // Standard 8 colors (normal)
        COLORS_256[0] = 0x000000;
        COLORS_256[1] = 0xa80000;
        COLORS_256[2] = 0x00a800;
        COLORS_256[3] = 0xa85400;
        COLORS_256[4] = 0x0000a8;
        COLORS_256[5] = 0xa800a8;
        COLORS_256[6] = 0x00a8a8;
        COLORS_256[7] = 0xa8a8a8;
        // High-intensity colors
        COLORS_256[8] = 0x545454;
        COLORS_256[9] = 0xfc5454;
        COLORS_256[10] = 0x54fc54;
        COLORS_256[11] = 0xfcfc54;
        COLORS_256[12] = 0x5454fc;
        COLORS_256[13] = 0xfc54fc;
        COLORS_256[14] = 0x54fcfc;
        COLORS_256[15] = 0xfcfcfc;
        // 216-color cube (6x6x6)
        for (int i = 0; i < 216; i++) {
            int ri = i / 36;
            int gi = (i / 6) % 6;
            int bi = i % 6;
            int r = (ri == 0) ? 0 : 55 + ri * 40;
            int g = (gi == 0) ? 0 : 55 + gi * 40;
            int b = (bi == 0) ? 0 : 55 + bi * 40;
            COLORS_256[16 + i] = (r << 16) | (g << 8) | b;
        }
        // 24 grayscale colors
        for (int i = 0; i < 24; i++) {
            int v = 8 + i * 10;
            COLORS_256[232 + i] = (v << 16) | (v << 8) | v;
        }
    }

    // ------------------------------------------------------------------------
    // Inner class: parsed line -----------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * A single parsed line containing cells.
     */
    public static final class Line {
        private final List<Cell> cells;

        Line(final List<Cell> cells) {
            this.cells = Collections.unmodifiableList(
                new ArrayList<>(cells));
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
     * @param text the input text (may contain ANSI escape sequences)
     * @param width the display width for wrapping (columns)
     * @return list of parsed lines
     */
    public static List<Line> parse(final String text, final int width) {
        if (text == null || width <= 0) {
            return Collections.emptyList();
        }

        List<Line> lines = new ArrayList<>();
        List<Cell> currentLine = new ArrayList<>();
        CellAttributes currentAttr = new CellAttributes();
        int col = 0;

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
                } else if (ch == '\n') {
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
                        putCell(currentLine, col, ' ', currentAttr);
                        col++;
                    }
                    if (col >= width) {
                        lines.add(new Line(currentLine));
                        currentLine = new ArrayList<>();
                        col = 0;
                    }
                } else if (ch == '\b') {
                    // Backspace: move cursor back one column
                    if (col > 0) {
                        col--;
                    }
                } else if (ch >= 0x20) {
                    // Printable character
                    int charWidth = StringUtils.width(ch);
                    if (col + charWidth > width) {
                        // Wrap
                        lines.add(new Line(currentLine));
                        currentLine = new ArrayList<>();
                        col = 0;
                    }
                    putCell(currentLine, col, ch, currentAttr);
                    col++;
                    if (charWidth == 2) {
                        // Wide character takes two columns; put a padding
                        // space for the right half
                        putCell(currentLine, col, ' ', currentAttr);
                        col++;
                    }
                    if (col >= width) {
                        lines.add(new Line(currentLine));
                        currentLine = new ArrayList<>();
                        col = 0;
                    }
                }
                // else: ignore other control characters
                break;

            case ESCAPE:
                if (ch == '[') {
                    // CSI sequence
                    state = State.CSI_PARAM;
                    csiParams.setLength(0);
                } else if (ch == ']') {
                    // OSC sequence: skip until ST (BEL or ESC \)
                    i += charCount;
                    while (i < text.length()) {
                        int osc = text.codePointAt(i);
                        if (osc == 0x07) {
                            break; // BEL terminates
                        }
                        if (osc == 0x1B && i + 1 < text.length()
                                && text.charAt(i + 1) == '\\') {
                            i++; // skip the backslash
                            break;
                        }
                        i += Character.charCount(osc);
                    }
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
                        // SGR sequence
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

        // Add the last line if it has content or if text ended without a
        // trailing newline
        if (!currentLine.isEmpty() || col == 0) {
            lines.add(new Line(currentLine));
        }

        return lines;
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
            line.add(new Cell());
        }
        Cell cell = new Cell();
        cell.setTo(attr);
        cell.setChar(ch);
        line.set(col, cell);
    }

    /**
     * Apply an SGR (Select Graphic Rendition) parameter string to the
     * given attributes. This handles the common SGR codes used by tools
     * like pandoc, bat, less, etc.
     *
     * @param params the parameter string (e.g., "1;31" for bold red)
     * @param attr the attributes to modify
     */
    private static void applySgr(final String params,
            final CellAttributes attr) {
        if (params.isEmpty()) {
            attr.reset();
            return;
        }

        // Split by ; or : (colon is used in some SGR subparameters)
        String[] parts = params.split("[;:]");
        int sgrColorMode = -1;
        boolean idx256Color = false;
        boolean rgbColor = false;
        int rgbRed = -1;
        int rgbGreen = -1;

        for (String part : parts) {
            int value;
            try {
                value = part.isEmpty() ? 0 : Integer.parseInt(part);
            } catch (NumberFormatException e) {
                continue;
            }

            // Handle extended color sub-parameters
            if (sgrColorMode == 38 || sgrColorMode == 48) {
                if (idx256Color) {
                    int rgb = getIndexedColor(value);
                    if (sgrColorMode == 38) {
                        attr.setForeColorRGB(rgb);
                    } else {
                        attr.setBackColorRGB(rgb);
                    }
                    sgrColorMode = -1;
                    idx256Color = false;
                    continue;
                }
                if (rgbColor) {
                    if (rgbRed == -1) {
                        rgbRed = value & 0xFF;
                    } else if (rgbGreen == -1) {
                        rgbGreen = value & 0xFF;
                    } else {
                        int rgb = (rgbRed << 16) | (rgbGreen << 8)
                            | (value & 0xFF);
                        if (sgrColorMode == 38) {
                            attr.setForeColorRGB(rgb);
                        } else {
                            attr.setBackColorRGB(rgb);
                        }
                        rgbRed = -1;
                        rgbGreen = -1;
                        sgrColorMode = -1;
                        rgbColor = false;
                    }
                    continue;
                }
                if (value == 5) {
                    idx256Color = true;
                    continue;
                } else if (value == 2) {
                    rgbColor = true;
                    continue;
                } else {
                    // Unknown sub-mode, bail out
                    sgrColorMode = -1;
                    continue;
                }
            }

            switch (value) {
            case 0:
                attr.reset();
                break;
            case 1:
                attr.setBold(true);
                break;
            case 2:
                // Dim/faint - treat as no-bold
                attr.setBold(false);
                break;
            case 3:
                // Italic - not directly supported, map to underline
                attr.setUnderline(true);
                break;
            case 4:
                attr.setUnderline(true);
                break;
            case 5:
            case 6:
                attr.setBlink(true);
                break;
            case 7:
                attr.setReverse(true);
                break;
            case 8:
                // Hidden/invisible - not supported
                break;
            case 9:
                // Strikethrough - not supported
                break;
            case 22:
                attr.setBold(false);
                break;
            case 23:
                // Not italic
                attr.setUnderline(false);
                break;
            case 24:
                attr.setUnderline(false);
                break;
            case 25:
                attr.setBlink(false);
                break;
            case 27:
                attr.setReverse(false);
                break;
            case 28:
                // Not hidden
                break;
            case 29:
                // Not strikethrough
                break;
            case 30:
                attr.setForeColor(Color.BLACK);
                break;
            case 31:
                attr.setForeColor(Color.RED);
                break;
            case 32:
                attr.setForeColor(Color.GREEN);
                break;
            case 33:
                attr.setForeColor(Color.YELLOW);
                break;
            case 34:
                attr.setForeColor(Color.BLUE);
                break;
            case 35:
                attr.setForeColor(Color.MAGENTA);
                break;
            case 36:
                attr.setForeColor(Color.CYAN);
                break;
            case 37:
                attr.setForeColor(Color.WHITE);
                break;
            case 38:
                sgrColorMode = 38;
                break;
            case 39:
                // Default foreground
                attr.setForeColor(Color.WHITE);
                attr.setDefaultColor(true, true);
                break;
            case 40:
                attr.setBackColor(Color.BLACK);
                break;
            case 41:
                attr.setBackColor(Color.RED);
                break;
            case 42:
                attr.setBackColor(Color.GREEN);
                break;
            case 43:
                attr.setBackColor(Color.YELLOW);
                break;
            case 44:
                attr.setBackColor(Color.BLUE);
                break;
            case 45:
                attr.setBackColor(Color.MAGENTA);
                break;
            case 46:
                attr.setBackColor(Color.CYAN);
                break;
            case 47:
                attr.setBackColor(Color.WHITE);
                break;
            case 48:
                sgrColorMode = 48;
                break;
            case 49:
                // Default background
                attr.setBackColor(Color.BLACK);
                attr.setDefaultColor(false, true);
                break;
            case 90:
                attr.setForeColorRGB(COLORS_256[8]);
                break;
            case 91:
                attr.setForeColorRGB(COLORS_256[9]);
                break;
            case 92:
                attr.setForeColorRGB(COLORS_256[10]);
                break;
            case 93:
                attr.setForeColorRGB(COLORS_256[11]);
                break;
            case 94:
                attr.setForeColorRGB(COLORS_256[12]);
                break;
            case 95:
                attr.setForeColorRGB(COLORS_256[13]);
                break;
            case 96:
                attr.setForeColorRGB(COLORS_256[14]);
                break;
            case 97:
                attr.setForeColorRGB(COLORS_256[15]);
                break;
            case 100:
                attr.setBackColorRGB(COLORS_256[8]);
                break;
            case 101:
                attr.setBackColorRGB(COLORS_256[9]);
                break;
            case 102:
                attr.setBackColorRGB(COLORS_256[10]);
                break;
            case 103:
                attr.setBackColorRGB(COLORS_256[11]);
                break;
            case 104:
                attr.setBackColorRGB(COLORS_256[12]);
                break;
            case 105:
                attr.setBackColorRGB(COLORS_256[13]);
                break;
            case 106:
                attr.setBackColorRGB(COLORS_256[14]);
                break;
            case 107:
                attr.setBackColorRGB(COLORS_256[15]);
                break;
            default:
                break;
            }
        }
    }

    /**
     * Get the RGB value for a 256-color index.
     *
     * @param index the color index (0-255)
     * @return the RGB value
     */
    private static int getIndexedColor(final int index) {
        if (index < 0 || index > 255) {
            return 0;
        }
        return COLORS_256[index];
    }
}
