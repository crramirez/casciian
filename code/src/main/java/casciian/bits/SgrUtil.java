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
 * {@link casciian.terminal.ECMA48} (full terminal emulator). Both
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

    /**
     * Apply a single SGR code to the given attributes. Handles the standard
     * attribute codes (bold, underline, blink, reverse), standard 8
     * foreground/background colors (30–37, 40–47), default color restore
     * (39, 49), and high-intensity colors (90–97, 100–107).
     *
     * <p>
     * For high-intensity and indexed colors, the provided {@code palette}
     * function maps a color index (0–255) to an RGB value. This allows
     * callers with custom/mutable palettes (e.g., terminal emulators) to
     * supply their own lookup.
     * </p>
     *
     * @param code the SGR parameter value
     * @param attr the attributes to modify
     * @param palette maps a color index (0–255) to an RGB int; used for
     *        high-intensity colors (indices 8–15). May be {@code null},
     *        in which case the default xterm palette is used.
     * @return {@code true} if the code was handled, {@code false} if it
     *         was not recognized (caller may handle it specially)
     */
    public static boolean applySgrCode(final int code,
            final CellAttributes attr, final IntUnaryOperator palette) {

        IntUnaryOperator pal = (palette != null)
            ? palette : SgrUtil::getDefaultIndexedColor;

        switch (code) {
        // --- Attribute toggles ---
        case 0:
            resetToDefaults(attr);
            return true;
        case 1:
            attr.setBold(true);
            return true;
        case 2:
            // Dim/faint — treat as not-bold
            attr.setBold(false);
            return true;
        case 3:
            // Italic — map to underline (no native italic support)
            attr.setUnderline(true);
            return true;
        case 4:
            attr.setUnderline(true);
            return true;
        case 5:
        case 6:
            attr.setBlink(true);
            return true;
        case 7:
            attr.setReverse(true);
            return true;
        case 8:
            // Hidden/invisible — not supported, but recognized
            return true;
        case 9:
            // Strikethrough — not supported, but recognized
            return true;
        case 22:
            attr.setBold(false);
            return true;
        case 23:
            // Not italic
            attr.setUnderline(false);
            return true;
        case 24:
            attr.setUnderline(false);
            return true;
        case 25:
            attr.setBlink(false);
            return true;
        case 27:
            attr.setReverse(false);
            return true;
        case 28:
            // Not hidden
            return true;
        case 29:
            // Not strikethrough
            return true;

        // --- Standard foreground colors (30–37) ---
        case 30:
        case 31:
        case 32:
        case 33:
        case 34:
        case 35:
        case 36:
        case 37:
            attr.setForeColor(Color.getSgrColor(code - 30));
            attr.setDefaultColor(true, false);
            return true;

        // --- Extended foreground (38) handled by caller's state machine ---
        case 38:
            return false;

        // --- Default foreground (39) ---
        case 39:
            attr.setForeColor(Color.WHITE);
            attr.setDefaultColor(true, true);
            return true;

        // --- Standard background colors (40–47) ---
        case 40:
        case 41:
        case 42:
        case 43:
        case 44:
        case 45:
        case 46:
        case 47:
            attr.setBackColor(Color.getSgrColor(code - 40));
            attr.setDefaultColor(false, false);
            return true;

        // --- Extended background (48) handled by caller's state machine ---
        case 48:
            return false;

        // --- Default background (49) ---
        case 49:
            attr.setBackColor(Color.BLACK);
            attr.setDefaultColor(false, true);
            return true;

        // --- High-intensity foreground (90–97) ---
        case 90:
        case 91:
        case 92:
        case 93:
        case 94:
        case 95:
        case 96:
        case 97:
            attr.setForeColorRGB(pal.applyAsInt(code - 90 + 8));
            attr.setDefaultColor(true, false);
            return true;

        // --- High-intensity background (100–107) ---
        case 100:
        case 101:
        case 102:
        case 103:
        case 104:
        case 105:
        case 106:
        case 107:
            attr.setBackColorRGB(pal.applyAsInt(code - 100 + 8));
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
         * Feed the next sub-parameter value. Returns {@code true} if the
         * value was consumed (caller should continue to next param),
         * {@code false} if the sequence has ended or is invalid.
         *
         * @param value the sub-parameter value
         * @param attr the attributes to apply color to when complete
         * @param palette color index lookup (index→RGB)
         * @return {@code true} if the value was consumed
         */
        public boolean feedValue(final int value,
                final CellAttributes attr,
                final IntUnaryOperator palette) {

            if (mode == -1) {
                return false;
            }

            if (indexed) {
                // We have the color index
                int rgbVal = palette.applyAsInt(value);
                applyColor(rgbVal, attr);
                reset();
                return true;
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
                return true;
            }

            // Expecting sub-mode selector (5 = indexed, 2 = RGB)
            if (value == 5) {
                indexed = true;
                return true;
            } else if (value == 2) {
                rgb = true;
                return true;
            } else {
                // Unknown sub-mode, abort
                reset();
                return true;
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

        private void reset() {
            mode = -1;
            indexed = false;
            rgb = false;
            rgbRed = -1;
            rgbGreen = -1;
        }
    }
}
