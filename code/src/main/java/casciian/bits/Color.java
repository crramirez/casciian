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

/**
 * A text cell color.
 */
public final class Color {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * SGR black value = 0.
     */
    private static final int SGRBLACK   = 0;

    /**
     * SGR red value = 1.
     */
    private static final int SGRRED     = 1;

    /**
     * SGR green value = 2.
     */
    private static final int SGRGREEN   = 2;

    /**
     * SGR yellow value = 3.
     */
    private static final int SGRYELLOW  = 3;

    /**
     * SGR blue value = 4.
     */
    private static final int SGRBLUE    = 4;

    /**
     * SGR magenta value = 5.
     */
    private static final int SGRMAGENTA = 5;

    /**
     * SGR cyan value = 6.
     */
    private static final int SGRCYAN    = 6;

    /**
     * SGR white value = 7.
     */
    private static final int SGRWHITE   = 7;

    /**
     * Black.  Bold + black = dark grey
     */
    public static final Color BLACK = new Color(SGRBLACK);

    /**
     * Red.
     */
    public static final Color RED = new Color(SGRRED);

    /**
     * Green.
     */
    public static final Color GREEN  = new Color(SGRGREEN);

    /**
     * Yellow.  Sometimes not-bold yellow is brown.
     */
    public static final Color YELLOW = new Color(SGRYELLOW);

    /**
     * Blue.
     */
    public static final Color BLUE = new Color(SGRBLUE);

    /**
     * Magenta (purple).
     */
    public static final Color MAGENTA = new Color(SGRMAGENTA);

    /**
     * Cyan (blue-green).
     */
    public static final Color CYAN = new Color(SGRCYAN);

    /**
     * White.
     */
    public static final Color WHITE = new Color(SGRWHITE);

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The color value.  Default is SGRWHITE.
     */
    private int value = SGRWHITE;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Private constructor used to make the static Color instances.
     *
     * @param value the integer Color value
     */
    private Color(final int value) {
        this.value = value;
    }

    // ------------------------------------------------------------------------
    // Color ------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get color value.  Note that these deliberately match the color values
     * of the ECMA-48 / ANSI X3.64 / VT100-ish SGR function ("ANSI colors").
     *
     * @return the value
     */
    public int getValue() {
        return value;
    }

    /**
     * Public constructor returns one of the static Color instances.
     *
     * @param colorName "red", "blue", etc.
     * @return Color.RED, Color.BLUE, etc.
     */
    static Color getColor(final String colorName) {
        String str = colorName.toLowerCase();

        if (str.equals("black")) {
            return Color.BLACK;
        } else if (str.equals("white")) {
            return Color.WHITE;
        } else if (str.equals("red")) {
            return Color.RED;
        } else if (str.equals("cyan")) {
            return Color.CYAN;
        } else if (str.equals("green")) {
            return Color.GREEN;
        } else if (str.equals("magenta")) {
            return Color.MAGENTA;
        } else if (str.equals("blue")) {
            return Color.BLUE;
        } else if (str.equals("yellow")) {
            return Color.YELLOW;
        } else if (str.equals("brown")) {
            return Color.YELLOW;
        } else {
            // Let unknown strings become white
            return Color.WHITE;
        }
    }

    /**
     * Invert a color in the same way as (CGA/VGA color XOR 0x7).
     *
     * @return the inverted color
     */
    public Color invert() {
        switch (value) {
        case SGRBLACK:
            return Color.WHITE;
        case SGRWHITE:
            return Color.BLACK;
        case SGRRED:
            return Color.CYAN;
        case SGRCYAN:
            return Color.RED;
        case SGRGREEN:
            return Color.MAGENTA;
        case SGRMAGENTA:
            return Color.GREEN;
        case SGRBLUE:
            return Color.YELLOW;
        case SGRYELLOW:
            return Color.BLUE;
        default:
            throw new IllegalArgumentException("Invalid Color value: " + value);
        }
    }

    /**
     * Comparison check.  All fields must match to return true.
     *
     * @param rhs another Color instance
     * @return true if all fields are equal
     */
    @Override
    public boolean equals(final Object rhs) {
        if (!(rhs instanceof Color)) {
            return false;
        }

        Color that = (Color) rhs;
        return (value == that.value);
    }

    /**
     * Hashcode uses all fields in equals().
     *
     * @return the hash
     */
    @Override
    public int hashCode() {
        return value;
    }

    /**
     * Make human-readable description of this Color.
     *
     * @return displayable String "red", "blue", etc.
     */
    @Override
    public String toString() {
        switch (value) {
        case SGRBLACK:
            return "black";
        case SGRWHITE:
            return "white";
        case SGRRED:
            return "red";
        case SGRCYAN:
            return "cyan";
        case SGRGREEN:
            return "green";
        case SGRMAGENTA:
            return "magenta";
        case SGRBLUE:
            return "blue";
        case SGRYELLOW:
            return "yellow";
        default:
            throw new IllegalArgumentException("Invalid Color value: " + value);
        }
    }

    /**
     * Convert this color to an RGB string.
     *
     * @return the RGB string
     */
    public String toRgbString() {
        return toRgbString(false);
    }

    /**
     * Convert this color to an RGB string.
     *
     * @param bright if true, return the bright/bold color
     * @return the RGB string
     */
    public String toRgbString(final boolean bright) {
        String [] normalColors = {
            "#000000",              // COLOR_BLACK
            "#AB0000",              // COLOR_RED
            "#00AB00",              // COLOR_GREEN
            "#996600",              // COLOR_YELLOW
            "#0000AB",              // COLOR_BLUE
            "#990099",              // COLOR_MAGENTA
            "#009999",              // COLOR_CYAN
            "#ABABAB",              // COLOR_WHITE
        };

        String [] brightColors = {
            "#545454",              // COLOR_BLACK
            "#FF6666",              // COLOR_RED
            "#66FF66",              // COLOR_GREEN
            "#FFFF66",              // COLOR_YELLOW
            "#6666FF",              // COLOR_BLUE
            "#FF66FF",              // COLOR_MAGENTA
            "#66FFFF",              // COLOR_CYAN
            "#FFFFFF",              // COLOR_WHITE
        };

        if (bright) {
            return brightColors[value];
        }
        return normalColors[value];
    }

    /**
     * Public constructor returns one of the static Color instances.
     *
     * @param sgrValue a value between 0 and 15, inclusive, representing an
     * ANSI color
     * @return Color.RED, Color.BLUE, etc.
     */
    public static Color getSgrColor(final int sgrValue) {
        switch (sgrValue) {
        case 0:
            return Color.BLACK;
        case 1:
            return Color.RED;
        case 2:
            return Color.GREEN;
        case 3:
            return Color.YELLOW;
        case 4:
            return Color.BLUE;
        case 5:
            return Color.MAGENTA;
        case 6:
            return Color.CYAN;
        case 7:
            return Color.WHITE;
        default:
            throw new IllegalArgumentException("Invalid Color value: " +
                sgrValue);
        }
    }

}
