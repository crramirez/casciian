/*
 * Casciian - Java Text User Interface
 *
 * Written 2013-2025 by Autumn Lamonte
 *
 * To the extent possible under law, the author(s) have dedicated all
 * copyright and related and neighboring rights to this software to the
 * public domain worldwide. This software is distributed without any
 * warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along
 * with this software. If not, see
 * <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package casciian.bits;

import java.util.ArrayList;
import java.util.List;

/**
 * A text box border style.
 */
public class BorderStyle {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The "no-border" style.
     */
    public static final BorderStyle NONE;

    /**
     * A single-line border.
     */
    public static final BorderStyle SINGLE;

    /**
     * A double-line border.
     */
    public static final BorderStyle DOUBLE;

    /**
     * A single-line border on the vertical sections, double-line on the
     * horizontal sections.
     */
    public static final BorderStyle SINGLE_V_DOUBLE_H;

    /**
     * A double-line border on the vertical sections, single-line on the
     * horizontal sections.
     */
    public static final BorderStyle SINGLE_H_DOUBLE_V;

    /**
     * A single-line border with round corners.
     */
    public static final BorderStyle SINGLE_ROUND;

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The glyph for horizontal sections.
     */
    private int horizontal;

    /**
     * The glyph for vertical sections.
     */
    private int vertical;

    /**
     * The glyph for the top-left corner.
     */
    private int topLeft;

    /**
     * The glyph for the top-right corner.
     */
    private int topRight;

    /**
     * The glyph for the bottom-left corner.
     */
    private int bottomLeft;

    /**
     * The glyph for the bottom-right corner.
     */
    private int bottomRight;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Static constructor.
     */
    static {

        NONE = new BorderStyle(' ', ' ', ' ', ' ', ' ', ' ');

        SINGLE = new BorderStyle(GraphicsChars.SINGLE_BAR,
            GraphicsChars.WINDOW_SIDE,
            GraphicsChars.ULCORNER,
            GraphicsChars.URCORNER,
            GraphicsChars.LLCORNER,
            GraphicsChars.LRCORNER);

        DOUBLE = new BorderStyle(GraphicsChars.DOUBLE_BAR,
            GraphicsChars.WINDOW_SIDE_DOUBLE,
            GraphicsChars.WINDOW_LEFT_TOP_DOUBLE,
            GraphicsChars.WINDOW_RIGHT_TOP_DOUBLE,
            GraphicsChars.WINDOW_LEFT_BOTTOM_DOUBLE,
            GraphicsChars.WINDOW_RIGHT_BOTTOM_DOUBLE);

        SINGLE_V_DOUBLE_H = new BorderStyle(GraphicsChars.WINDOW_TOP,
            GraphicsChars.WINDOW_SIDE,
            GraphicsChars.WINDOW_LEFT_TOP,
            GraphicsChars.WINDOW_RIGHT_TOP,
            GraphicsChars.WINDOW_LEFT_BOTTOM,
            GraphicsChars.WINDOW_RIGHT_BOTTOM);

        SINGLE_H_DOUBLE_V = new BorderStyle(GraphicsChars.SINGLE_BAR,
            GraphicsChars.WINDOW_SIDE_DOUBLE,
            0x2553,
            0x2556,
            0x2559,
            0x255C);

        SINGLE_ROUND = new BorderStyle(GraphicsChars.SINGLE_BAR,
            GraphicsChars.WINDOW_SIDE,
            0x256D,
            0x256E,
            0x2570,
            0x256F);

    }

    /**
     * Private constructor used to make the static BorderStyle instances.
     *
     * @param horizontal the horizontal section glyph
     * @param vertical the vertical section glyph
     * @param topLeft the top-left corner glyph
     * @param topRight the top-right corner glyph
     * @param bottomLeft the bottom-left corner glyph
     * @param bottomRight the bottom-right corner glyph
     */
    private BorderStyle(final int horizontal, final int vertical,
        final int topLeft, final int topRight,
        final int bottomLeft, final int bottomRight) {

        this.horizontal  = horizontal;
        this.vertical    = vertical;
        this.topLeft     = topLeft;
        this.topRight    = topRight;
        this.bottomLeft  = bottomLeft;
        this.bottomRight = bottomRight;
    }

    // ------------------------------------------------------------------------
    // BorderStyle ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor returns one of the static BorderStyle instances.
     *
     * @return a list of supported border style strings
     */
    public static final List<String> getStyleNames() {
        List<String> result = new ArrayList<String>();
        result.add("none");
        result.add("default");
        result.add("single");
        result.add("double");
        result.add("round");
        result.add("singleVdoubleH");
        result.add("singleHdoubleV");
        return result;
    }

    /**
     * Public constructor returns one of the static BorderStyle instances.
     *
     * @param borderStyle the border style string, one of: "default", "none",
     * "single", "double", "singleVdoubleH", "singleHdoubleV", or "round"
     * @return BorderStyle.SINGLE, BorderStyle.DOUBLE, etc.
     */
    public static final BorderStyle getStyle(final String borderStyle) {
        String str = borderStyle.toLowerCase();

        if (str.equals("none")) {
            return NONE;
        }
        if (str.equals("default")) {
            return SINGLE;
        }
        if (str.equals("single")) {
            return SINGLE;
        }
        if (str.equals("double")) {
            return DOUBLE;
        }
        if (str.equals("round")) {
            return SINGLE_ROUND;
        }
        if (str.equals("singlevdoubleh")) {
            return SINGLE_V_DOUBLE_H;
        }
        if (str.equals("singlehdoublev")) {
            return SINGLE_H_DOUBLE_V;
        }

        // If they didn't get it right, return single.
        return SINGLE;
    }

    /**
     * Get the glyph for horizontal sections.
     *
     * @return the glyph for horizontal sections.
     */
    public final int getHorizontal() {
        return horizontal;
    }

    /**
     *
     * Get the glyph for vertical sections.
     * @return the glyph for vertical sections.
     */
    public final int getVertical() {
        return vertical;
    }

    /**
     * Get the glyph for the top-left corner.
     *
     * @return the glyph for the top-left corner.
     */
    public final int getTopLeft() {
        return topLeft;
    }

    /**
     * Get the glyph for the top-right corner.
     *
     * @return the glyph for the top-right corner.
     */
    public final int getTopRight() {
        return topRight;
    }

    /**
     * Get the glyph for the bottom-left corner.
     *
     * @return the glyph for the bottom-left corner.
     */
    public final int getBottomLeft() {
        return bottomLeft;
    }

    /**
     * Get the glyph for the bottom-right corner.
     *
     * @return the glyph for the bottom-right corner.
     */
    public final int getBottomRight() {
        return bottomRight;
    }

}
