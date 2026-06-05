/*
 * Casciian - Java Text User Interface
 *
 * Original work written 2013–2025 by Autumn Lamonte
 * and dedicated to the public domain via CC0.
 *
 * Modifications and maintenance:
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
import casciian.backend.Backend;
import casciian.bits.CellAttributes;
import casciian.bits.ColorTheme;

/**
 * Helper that derives the corner colors used by the demo window gradients
 * from the colors of the currently active {@link ColorTheme}.  This keeps the
 * gradient backgrounds harmonic with whatever theme the user has selected,
 * instead of using a single hard-coded palette.
 */
final class DemoGradientColors {

    /**
     * Utility class; do not instantiate.
     */
    private DemoGradientColors() {
    }

    /**
     * Resolve the four gradient corner colors (24-bit RGB) for the active
     * theme, ordered as topLeft, topRight, bottomLeft, bottomRight.
     *
     * @param application the running application providing the theme and
     * backend used to resolve colors
     * @return an array of four RGB ints
     */
    static int[] cornerColors(final TApplication application) {
        ColorTheme theme = application.getTheme();
        Backend backend = application.getBackend();

        int windowBackground = backColor(backend, theme,
            ColorTheme.TWINDOW_BORDER);
        int accent = backColor(backend, theme, ColorTheme.TBUTTON_ACTIVE);
        int border = foreColor(backend, theme, ColorTheme.TLABEL);

        // Sweep from the window background, through the two accent colors,
        // and back to the window background so the gradient blends into the
        // theme on opposite corners.
        return new int[] {
            windowBackground,
            accent,
            border,
            windowBackground,
        };
    }

    /**
     * Resolve the color used for the mouse glow effect for the active theme.
     *
     * @param application the running application providing the theme and
     * backend used to resolve colors
     * @return an RGB int
     */
    static int glowColor(final TApplication application) {
        ColorTheme theme = application.getTheme();
        Backend backend = application.getBackend();
        return foreColor(backend, theme, ColorTheme.TBUTTON_ACTIVE);
    }

    /**
     * Resolve the background RGB color of a named theme color, falling back to
     * black when the color is not registered.
     */
    private static int backColor(final Backend backend,
        final ColorTheme theme, final String name) {

        CellAttributes color = theme.getColor(name);
        if (color == null) {
            return 0;
        }
        return backend.attrToBackgroundColor(color) & 0xFFFFFF;
    }

    /**
     * Resolve the foreground RGB color of a named theme color, falling back to
     * white when the color is not registered.
     */
    private static int foreColor(final Backend backend,
        final ColorTheme theme, final String name) {

        CellAttributes color = theme.getColor(name);
        if (color == null) {
            return 0xFFFFFF;
        }
        return backend.attrToForegroundColor(color) & 0xFFFFFF;
    }
}
