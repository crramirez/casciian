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

/**
 * Helpers for the xterm 256-color palette (the "color cube").
 *
 * <p>
 * The 256-color palette is laid out as follows:
 * </p>
 * <ul>
 *   <li>Indices 0–15: the standard 16 CGA/ANSI colors (0–7 normal, 8–15
 *       bright).</li>
 *   <li>Indices 16–231: a 6×6×6 RGB color cube.</li>
 *   <li>Indices 232–255: a 24-step grayscale ramp.</li>
 * </ul>
 *
 * <p>
 * Emitting a palette index (SGR {@code 38;5;n} / {@code 48;5;n}) is much
 * cheaper than emitting a full 24-bit RGB triple (SGR {@code 38;2;r;g;b} /
 * {@code 48;2;r;g;b}), so callers that only need an approximate color can use
 * this class to trade a small amount of color precision for a smaller,
 * faster-to-render escape sequence.
 * </p>
 */
public final class Palette256 {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The number of entries in the palette.
     */
    public static final int SIZE = 256;

    /**
     * First index of the 6×6×6 color cube.
     */
    private static final int CUBE_START = 16;

    /**
     * First index of the grayscale ramp.
     */
    private static final int GRAY_START = 232;

    /**
     * The six intensity levels used along each axis of the color cube.
     */
    private static final int[] CUBE_LEVELS = {0, 95, 135, 175, 215, 255};

    /**
     * Precomputed lookup table mapping every 8-bit channel value (0–255) to
     * the index (0–5) of the nearest {@link #CUBE_LEVELS} entry.
     *
     * <p>
     * Since the cube levels are fixed, the nearest level for any channel
     * value can be computed once at class-load time and then reused, turning
     * the per-lookup search into a single array access.
     * </p>
     */
    private static final int[] CUBE_LEVEL_INDEX = new int[256];

    static {
        for (int value = 0; value < CUBE_LEVEL_INDEX.length; value++) {
            int best = 0;
            int bestDelta = Integer.MAX_VALUE;
            for (int i = 0; i < CUBE_LEVELS.length; i++) {
                int delta = Math.abs(CUBE_LEVELS[i] - value);
                if (delta < bestDelta) {
                    bestDelta = delta;
                    best = i;
                }
            }
            CUBE_LEVEL_INDEX[value] = best;
        }
    }

    /**
     * Precomputed lookup table mapping each of the 16 CGA/ANSI color indices
     * (0–15) to the nearest 6×6×6 cube / grayscale palette index (16–255).
     *
     * <p>
     * There are only 16 CGA colors with fixed RGB values, so their nearest
     * cube/grayscale entry can be computed once at class-load time and reused,
     * turning {@link #fromCgaColor(Color)} into a single array access.
     * </p>
     */
    private static final int[] CGA_TO_CUBE = new int[16];

    static {
        for (int i = 0; i < CGA_TO_CUBE.length; i++) {
            CGA_TO_CUBE[i] = fromRgb(SgrUtil.getDefaultIndexedColor(i));
        }
    }

    // ------------------------------------------------------------------------
    // Constructor (utility class) -------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Private constructor: this is a utility class.
     */
    private Palette256() {
        // NOP
    }

    // ------------------------------------------------------------------------
    // Palette256 -------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the RGB value for a palette index.
     *
     * @param index the palette index (0–255)
     * @return the 24-bit RGB value, or 0 for out-of-range indices
     */
    public static int toRgb(final int index) {
        return SgrUtil.getDefaultIndexedColor(index);
    }

    /**
     * Get the 256-color palette index for one of the 16 CGA/ANSI colors.
     *
     * <p>
     * The first 16 entries of the palette are exactly the 16 CGA colors, so
     * this simply returns the color's SGR value (0–15).
     * </p>
     *
     * @param color one of the Color.WHITE, Color.BLUE, etc. constants
     * @return the palette index (0–15) that matches this color
     */
    public static int fromColor(final Color color) {
        return color.getValue() & 0x0F;
    }

    /**
     * Get the 256-color palette index for one of the 16 CGA/ANSI colors,
     * optionally selecting the bright (high-intensity) variant.
     *
     * @param color one of the Color.WHITE, Color.BLUE, etc. constants
     * @param bright if true, return the bright (8–15) variant of the color
     * @return the palette index (0–15) that matches this color
     */
    public static int fromColor(final Color color, final boolean bright) {
        int index = color.getValue() & 0x07;
        if (bright || color.isBright()) {
            index += 8;
        }
        return index;
    }

    /**
     * Get the closest color-cube / grayscale palette index for one of the 16
     * CGA/ANSI colors.
     *
     * <p>
     * Unlike {@link #fromColor(Color)}, which returns one of the base indices
     * 0–15, this maps the CGA color to its nearest entry in the 6×6×6 color
     * cube or the grayscale ramp (indices 16–255).  The base 16 indices are
     * terminal-dependent (themes/OSC can remap them), so mapping into the
     * cube yields a fixed, well-defined RGB color that renders consistently
     * across terminals.
     * </p>
     *
     * @param color one of the Color.WHITE, Color.BLUE, etc. constants
     * @return the palette index (16–255) whose color is closest to this CGA
     *         color
     */
    public static int fromCgaColor(final Color color) {
        return CGA_TO_CUBE[fromColor(color)];
    }

    /**
     * Get the closest color-cube / grayscale palette index for one of the 16
     * CGA/ANSI colors, optionally selecting the bright (high-intensity)
     * variant.
     *
     * <p>
     * Unlike {@link #fromColor(Color, boolean)}, which returns one of the
     * base indices 0–15, this maps the CGA color to its nearest entry in the
     * 6×6×6 color cube or the grayscale ramp (indices 16–255).  The base 16
     * indices are terminal-dependent (themes/OSC can remap them), so mapping
     * into the cube yields a fixed, well-defined RGB color that renders
     * consistently across terminals.
     * </p>
     *
     * @param color one of the Color.WHITE, Color.BLUE, etc. constants
     * @param bright if true, use the bright (8–15) variant of the color
     * @return the palette index (16–255) whose color is closest to this CGA
     *         color
     */
    public static int fromCgaColor(final Color color, final boolean bright) {
        return CGA_TO_CUBE[fromColor(color, bright)];
    }

    /**
     * Find the closest 256-color palette index for a 24-bit RGB value.
     *
     * <p>
     * Both the 6×6×6 color cube and the 24-step grayscale ramp are
     * considered; the entry with the smallest squared Euclidean distance in
     * RGB space is returned.  The 16 base colors are intentionally excluded
     * because their exact RGB values are terminal-dependent (they can be
     * remapped by themes/OSC), whereas the cube and grayscale entries have
     * well-defined RGB values.
     * </p>
     *
     * @param rgb a 24-bit RGB value (0xRRGGBB)
     * @return the palette index (16–255) whose color is closest to {@code rgb}
     */
    public static int fromRgb(final int rgb) {
        int red = (rgb >>> 16) & 0xFF;
        int green = (rgb >>> 8) & 0xFF;
        int blue = rgb & 0xFF;

        // Best matching entry in the 6×6×6 color cube.
        int rIndex = nearestCubeLevel(red);
        int gIndex = nearestCubeLevel(green);
        int bIndex = nearestCubeLevel(blue);
        int cubeIndex = CUBE_START + (36 * rIndex) + (6 * gIndex) + bIndex;
        int cubeDistance = distance(rgb,
            (CUBE_LEVELS[rIndex] << 16)
                | (CUBE_LEVELS[gIndex] << 8)
                | CUBE_LEVELS[bIndex]);

        // Best matching entry in the grayscale ramp.
        int grayIndex = nearestGray(red, green, blue);
        int grayDistance = distance(rgb, toRgb(grayIndex));

        return (grayDistance < cubeDistance) ? grayIndex : cubeIndex;
    }

    /**
     * Find the color cube axis index (0–5) whose level is closest to the
     * given 8-bit channel value.
     *
     * @param value an 8-bit color channel value (0–255)
     * @return the closest cube level index (0–5)
     */
    private static int nearestCubeLevel(final int value) {
        return CUBE_LEVEL_INDEX[value];
    }

    /**
     * Find the grayscale-ramp palette index (232–255) closest to the average
     * intensity of the given RGB channels.
     *
     * @param red the red channel (0–255)
     * @param green the green channel (0–255)
     * @param blue the blue channel (0–255)
     * @return the closest grayscale palette index (232–255)
     */
    private static int nearestGray(final int red, final int green,
        final int blue) {

        int average = (red + green + blue) / 3;
        // Grayscale entry i has intensity 8 + i * 10, for i in 0..23.
        int step = Math.round((average - 8) / 10.0f);
        if (step < 0) {
            step = 0;
        } else if (step > 23) {
            step = 23;
        }
        return GRAY_START + step;
    }

    /**
     * Squared Euclidean distance between two RGB values.
     *
     * @param rgb1 the first RGB value
     * @param rgb2 the second RGB value
     * @return the squared distance in RGB space
     */
    private static int distance(final int rgb1, final int rgb2) {
        int dr = ((rgb1 >>> 16) & 0xFF) - ((rgb2 >>> 16) & 0xFF);
        int dg = ((rgb1 >>> 8) & 0xFF) - ((rgb2 >>> 8) & 0xFF);
        int db = (rgb1 & 0xFF) - (rgb2 & 0xFF);
        return (dr * dr) + (dg * dg) + (db * db);
    }

}
