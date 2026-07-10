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
 * Shared utility methods for SGR (Select Graphic Rendition) parsing.
 *
 * <p>
 * This class extracts the common SGR color and attribute handling logic
 * used by both {@link AnsiParser} (lightweight static renderer) and
 * the {@code ECMA48} terminal emulator (full terminal emulator). Both
 * components interpret the same SGR codes; this class provides the
 * shared mapping and application logic so that changes to color handling
 * only need to be made in one place.
 * </p>
 *
 * <p>
 * Callers that need custom behavior (e.g., ECMA48's backend-aware
 * default color handling for SGR 39/49) can check the return value of
 * {@link #applySgrCode} and handle unprocessed codes themselves.
 * </p>
 */
public final class SgrUtil {

    // ------------------------------------------------------------------------
    // Default 256-color palette (xterm) -------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The default xterm 256-color palette. Indices 0–15 are the standard
     * and high-intensity terminal colors; 16–231 are the 6×6×6 color cube;
     * 232–255 are the grayscale ramp.
     */
    private static final int[] XTERM_256;

    static {
        XTERM_256 = new int[256];
        // Standard 8 colors (DOS-compatible defaults)
        XTERM_256[0] = 0x000000;
        XTERM_256[1] = 0xa80000;
        XTERM_256[2] = 0x00a800;
        XTERM_256[3] = 0xa85400;
        XTERM_256[4] = 0x0000a8;
        XTERM_256[5] = 0xa800a8;
        XTERM_256[6] = 0x00a8a8;
        XTERM_256[7] = 0xa8a8a8;
        // High-intensity colors
        XTERM_256[8] = 0x545454;
        XTERM_256[9] = 0xfc5454;
        XTERM_256[10] = 0x54fc54;
        XTERM_256[11] = 0xfcfc54;
        XTERM_256[12] = 0x5454fc;
        XTERM_256[13] = 0xfc54fc;
        XTERM_256[14] = 0x54fcfc;
        XTERM_256[15] = 0xfcfcfc;
        // 216-color cube (6×6×6)
        for (int i = 0; i < 216; i++) {
            int ri = i / 36;
            int gi = (i / 6) % 6;
            int bi = i % 6;
            int r = (ri == 0) ? 0 : 55 + ri * 40;
            int g = (gi == 0) ? 0 : 55 + gi * 40;
            int b = (bi == 0) ? 0 : 55 + bi * 40;
            XTERM_256[16 + i] = (r << 16) | (g << 8) | b;
        }
        // 24 grayscale colors
        for (int i = 0; i < 24; i++) {
            int v = 8 + i * 10;
            XTERM_256[232 + i] = (v << 16) | (v << 8) | v;
        }
    }

    // ------------------------------------------------------------------------
    // Constructor (utility class) -------------------------------------------
    // ------------------------------------------------------------------------

    private SgrUtil() {
    }

    // ------------------------------------------------------------------------
    // Palette access ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the RGB value for a color index from the default xterm 256-color
     * palette.
     *
     * @param index the color index (0–255)
     * @return the RGB value, or 0 for out-of-range indices
     */
    public static int getDefaultIndexedColor(final int index) {
        if (index < 0 || index > 255) {
            return 0;
        }
        return XTERM_256[index];
    }

    /**
     * Build a mutable list initialized with the default 256-color palette.
     * This is useful for terminal emulators that allow dynamic palette
     * modification via OSC sequences.
     *
     * @return a new mutable {@link List} of 256 RGB color values
     */
    public static List<Integer> buildDefaultPalette() {
        List<Integer> palette = new ArrayList<>(Collections.nCopies(256, 0));
        for (int i = 0; i < 256; i++) {
            palette.set(i, XTERM_256[i]);
        }
        return palette;
    }

    // ------------------------------------------------------------------------
    // Attribute reset --------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Reset attributes to defaults and set both foreground and background
     * default-color flags. This is the standard behavior for SGR 0 and
     * for "ESC[m" (no parameters).
     *
     * @param attr the attributes to reset
     */
    public static void resetToDefaults(final CellAttributes attr) {
        attr.reset();
        attr.setDefaultColor(true, true);
        attr.setDefaultColor(false, true);
    }

    // ------------------------------------------------------------------------
    // Standard SGR application -----------------------------------------------
    // ------------------------------------------------------------------------

    // --- Attribute toggles ---
    private static final int SGR_RESET             = 0;
    private static final int SGR_BOLD               = 1;
    private static final int SGR_DIM                = 2;
    private static final int SGR_ITALIC             = 3;
    private static final int SGR_UNDERLINE          = 4;
    private static final int SGR_BLINK_SLOW         = 5;
    private static final int SGR_BLINK_RAPID        = 6;
    private static final int SGR_REVERSE            = 7;
    private static final int SGR_HIDDEN             = 8;
    private static final int SGR_STRIKETHROUGH      = 9;
    private static final int SGR_NORMAL_INTENSITY   = 22; // cancels bold/dim
    private static final int SGR_ITALIC_OFF         = 23;
    private static final int SGR_UNDERLINE_OFF      = 24;
    private static final int SGR_BLINK_OFF          = 25;
    private static final int SGR_REVERSE_OFF        = 27;
    private static final int SGR_HIDDEN_OFF         = 28;
    private static final int SGR_STRIKETHROUGH_OFF  = 29;

    // --- Foreground colors ---
    private static final int SGR_FG_BASE            = 30; // 30-37
    private static final int SGR_FG_EXTENDED        = 38; // indexed/RGB, sub-params
    private static final int SGR_FG_DEFAULT         = 39;
    private static final int SGR_FG_BRIGHT_BASE     = 90; // 90-97

    // --- Background colors ---
    private static final int SGR_BG_BASE            = 40; // 40-47
    private static final int SGR_BG_EXTENDED        = 48; // indexed/RGB, sub-params
    private static final int SGR_BG_DEFAULT         = 49;
    private static final int SGR_BG_BRIGHT_BASE     = 100; // 100-107

    /**
     * Apply a single SGR code to the given attributes. Handles the standard
     * attribute codes (bold, faint, italic, underline, blink, reverse,
     * hidden, strikethrough), standard 8 foreground/background colors
     * (30–37, 40–47), default color restore (39, 49), and high-intensity
     * colors (90–97, 100–107).
     *
     * <p>
     * High-intensity colors (90–97, 100–107) are now applied directly as
     * the corresponding BRIGHT_* named color (SGR indices 8–15), since
     * {@link CellAttributes} natively supports bright colors. There is no
     * need to resolve them to RGB, so the {@code palette} parameter is not
     * used for those codes; it is retained for signature compatibility with
     * callers that also drive indexed/RGB extended color sequences.
     * </p>
     *
     * @param code the SGR parameter value
     * @param attr the attributes to modify
     * @param palette unused by this method; retained for shared
     *        signature/API compatibility with callers
     * @return {@code true} if the code was handled, {@code false} if it
     *         was not recognized (caller may handle it specially)
     */
    @SuppressWarnings("java:S1871")
    public static boolean applySgrCode(final int code,
            final CellAttributes attr, final IntUnaryOperator palette) {


        switch (code) {
        // --- Attribute toggles ---
        case SGR_RESET:
            resetToDefaults(attr);
            return true;
        case SGR_BOLD:
            attr.setBold(true);
            return true;
        case SGR_DIM:
            // Faint / decreased intensity.
            attr.setFaint(true);
            return true;
        case SGR_ITALIC:
            attr.setItalic(true);
            return true;
        case SGR_UNDERLINE:
            attr.setUnderline(true);
            return true;
        case SGR_BLINK_SLOW, SGR_BLINK_RAPID:
            attr.setBlink(true);
            return true;
        case SGR_REVERSE:
            attr.setReverse(true);
            return true;
        case SGR_HIDDEN:
            attr.setHidden(true);
            return true;
        case SGR_STRIKETHROUGH:
            attr.setStrikethrough(true);
            return true;
        case SGR_NORMAL_INTENSITY:
            // Cancels both bold and faint.
            attr.setBold(false);
            attr.setFaint(false);
            return true;
        case SGR_ITALIC_OFF:
            attr.setItalic(false);
            return true;
        case SGR_UNDERLINE_OFF:
            attr.setUnderline(false);
            return true;
        case SGR_BLINK_OFF:
            attr.setBlink(false);
            return true;
        case SGR_REVERSE_OFF:
            attr.setReverse(false);
            return true;
        case SGR_HIDDEN_OFF:
            attr.setHidden(false);
            return true;
        case SGR_STRIKETHROUGH_OFF:
            attr.setStrikethrough(false);
            return true;

        // --- Standard foreground colors (30–37) ---
        case SGR_FG_BASE,
             SGR_FG_BASE + 1,
             SGR_FG_BASE + 2,
             SGR_FG_BASE + 3,
             SGR_FG_BASE + 4,
             SGR_FG_BASE + 5,
             SGR_FG_BASE + 6,
             SGR_FG_BASE + 7:
            attr.setForeColor(Color.getSgrColor(code - SGR_FG_BASE));
            attr.setDefaultColor(true, false);
            return true;

        // --- Extended foreground (38) handled by caller's state machine ---
        case SGR_FG_EXTENDED:
            return false;

        // --- Default foreground (39) ---
        case SGR_FG_DEFAULT:
            attr.setForeColor(Color.WHITE);
            attr.setDefaultColor(true, true);
            return true;

        // --- Standard background colors (40–47) ---
        case SGR_BG_BASE,
             SGR_BG_BASE + 1,
             SGR_BG_BASE + 2,
             SGR_BG_BASE + 3,
             SGR_BG_BASE + 4,
             SGR_BG_BASE + 5,
             SGR_BG_BASE + 6,
             SGR_BG_BASE + 7:
            attr.setBackColor(Color.getSgrColor(code - SGR_BG_BASE));
            attr.setDefaultColor(false, false);
            return true;

        // --- Extended background (48) handled by caller's state machine ---
        case SGR_BG_EXTENDED:
            return false;

        // --- Default background (49) ---
        case SGR_BG_DEFAULT:
            attr.setBackColor(Color.BLACK);
            attr.setDefaultColor(false, true);
            return true;

        // --- High-intensity foreground (90–97) ---
        case SGR_FG_BRIGHT_BASE,
             SGR_FG_BRIGHT_BASE + 1,
             SGR_FG_BRIGHT_BASE + 2,
             SGR_FG_BRIGHT_BASE + 3,
             SGR_FG_BRIGHT_BASE + 4,
             SGR_FG_BRIGHT_BASE + 5,
             SGR_FG_BRIGHT_BASE + 6,
             SGR_FG_BRIGHT_BASE + 7:
            attr.setForeColor(Color.getSgrColor(code - SGR_FG_BRIGHT_BASE + 8));
            attr.setDefaultColor(true, false);
            return true;

        // --- High-intensity background (100–107) ---
        case SGR_BG_BRIGHT_BASE,
             SGR_BG_BRIGHT_BASE + 1,
             SGR_BG_BRIGHT_BASE + 2,
             SGR_BG_BRIGHT_BASE + 3,
             SGR_BG_BRIGHT_BASE + 4,
             SGR_BG_BRIGHT_BASE + 5,
             SGR_BG_BRIGHT_BASE + 6,
             SGR_BG_BRIGHT_BASE + 7:
            attr.setBackColor(Color.getSgrColor(code - SGR_BG_BRIGHT_BASE + 8));
            attr.setDefaultColor(false, false);
            return true;

        default:
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // Extended color (38/48) sub-parameter handling --------------------------
    // ------------------------------------------------------------------------

    /**
     * Tracks the state of an in-progress extended color sequence (SGR 38 or
     * 48 with sub-parameters for indexed or RGB color). Instances are
     * reusable across multiple SGR sequences.
     */
    public static final class ExtendedColorState {
        private int mode = -1; // 38 or 48, -1 if inactive
        private boolean indexed = false;
        private boolean rgb = false;
        private int rgbRed = -1;
        private int rgbGreen = -1;

        /**
         * Create a new extended color state tracker.
         */
        public ExtendedColorState() {
            // No initialization needed; fields carry their default values.
        }

        /**
         * Check whether an extended color sequence is in progress.
         *
         * @return {@code true} if currently collecting sub-parameters
         */
        public boolean isActive() {
            return mode != -1;
        }

        /**
         * Begin a new extended color sequence.
         *
         * @param sgrCode 38 (foreground) or 48 (background)
         */
        public void begin(final int sgrCode) {
            mode = sgrCode;
            indexed = false;
            rgb = false;
            rgbRed = -1;
            rgbGreen = -1;
        }

        /**
         * Feed the next sub-parameter value.
         *
         * @param value the sub-parameter value
         * @param attr the attributes to apply color to when complete
         * @param palette color index lookup (index→RGB), unused for
         *        indexed (38/48;5;n) sequences since those are now applied
         *        directly as a 256-color palette index; retained for RGB
         *        sub-sequences' shared method signature
         */
        public void feedValue(final int value,
                final CellAttributes attr,
                final IntUnaryOperator palette) {

            if (mode == -1) {
                return;
            }

            if (indexed) {
                // Apply the 256-color palette index directly instead of
                // resolving it to RGB, so downstream rendering can emit a
                // compact "38;5;n"/"48;5;n" sequence.
                applyPaletteColor(value & 0xFF, attr);
                reset();
                return;
            }

            if (rgb) {
                if (rgbRed == -1) {
                    rgbRed = value & 0xFF;
                } else if (rgbGreen == -1) {
                    rgbGreen = value & 0xFF;
                } else {
                    int rgbVal = (rgbRed << 16) | (rgbGreen << 8)
                        | (value & 0xFF);
                    applyColor(rgbVal, attr);
                    reset();
                }
                return;
            }

            // Expecting sub-mode selector (5 = indexed, 2 = RGB)
            switch (value) {
                case 5 -> indexed = true;
                case 2 -> rgb = true;
                default -> reset(); // Unknown sub-mode, abort
            }
        }

        private void applyColor(final int rgbVal,
                final CellAttributes attr) {
            if (mode == 38) {
                attr.setForeColorRGB(rgbVal);
                attr.setDefaultColor(true, false);
            } else {
                attr.setBackColorRGB(rgbVal);
                attr.setDefaultColor(false, false);
            }
        }

        private void applyPaletteColor(final int index,
                final CellAttributes attr) {
            if (mode == 38) {
                attr.setForeColorPalette(index);
                attr.setDefaultColor(true, false);
            } else {
                attr.setBackColorPalette(index);
                attr.setDefaultColor(false, false);
            }
        }

        private void reset() {
            mode = -1;
            indexed = false;
            rgb = false;
            rgbRed = -1;
            rgbGreen = -1;
        }
    }
}
