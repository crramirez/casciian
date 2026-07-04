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
     * Offset added to a normal SGR color value (0-7) to produce its bright
     * variant (8-15).  Bright colors map to the AIXterm-style SGR foreground
     * codes 90-97 and background codes 100-107.
     */
    private static final int BRIGHT_OFFSET = 8;

    /**
     * SGR bright black value = 8.
     */
    private static final int SGRBRIGHTBLACK   = SGRBLACK + BRIGHT_OFFSET;

    /**
     * SGR bright red value = 9.
     */
    private static final int SGRBRIGHTRED     = SGRRED + BRIGHT_OFFSET;

    /**
     * SGR bright green value = 10.
     */
    private static final int SGRBRIGHTGREEN   = SGRGREEN + BRIGHT_OFFSET;

    /**
     * SGR bright yellow value = 11.
     */
    private static final int SGRBRIGHTYELLOW  = SGRYELLOW + BRIGHT_OFFSET;

    /**
     * SGR bright blue value = 12.
     */
    private static final int SGRBRIGHTBLUE    = SGRBLUE + BRIGHT_OFFSET;

    /**
     * SGR bright magenta value = 13.
     */
    private static final int SGRBRIGHTMAGENTA = SGRMAGENTA + BRIGHT_OFFSET;

    /**
     * SGR bright cyan value = 14.
     */
    private static final int SGRBRIGHTCYAN    = SGRCYAN + BRIGHT_OFFSET;

    /**
     * SGR bright white value = 15.
     */
    private static final int SGRBRIGHTWHITE   = SGRWHITE + BRIGHT_OFFSET;

    /**
     * Black.  See {@link #BRIGHT_BLACK} for the dark-grey, high-intensity
     * variant.
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

    /**
     * Bright black (dark grey).  Maps to SGR foreground 90 / background 100.
     */
    public static final Color BRIGHT_BLACK = new Color(SGRBRIGHTBLACK);

    /**
     * Bright red.  Maps to SGR foreground 91 / background 101.
     */
    public static final Color BRIGHT_RED = new Color(SGRBRIGHTRED);

    /**
     * Bright green.  Maps to SGR foreground 92 / background 102.
     */
    public static final Color BRIGHT_GREEN = new Color(SGRBRIGHTGREEN);

    /**
     * Bright yellow.  Maps to SGR foreground 93 / background 103.
     */
    public static final Color BRIGHT_YELLOW = new Color(SGRBRIGHTYELLOW);

    /**
     * Bright blue.  Maps to SGR foreground 94 / background 104.
     */
    public static final Color BRIGHT_BLUE = new Color(SGRBRIGHTBLUE);

    /**
     * Bright magenta.  Maps to SGR foreground 95 / background 105.
     */
    public static final Color BRIGHT_MAGENTA = new Color(SGRBRIGHTMAGENTA);

    /**
     * Bright cyan.  Maps to SGR foreground 96 / background 106.
     */
    public static final Color BRIGHT_CYAN = new Color(SGRBRIGHTCYAN);

    /**
     * Bright white.  Maps to SGR foreground 97 / background 107.
     */
    public static final Color BRIGHT_WHITE = new Color(SGRBRIGHTWHITE);

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The color value.
     */
    private final int value;

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
     * Determine whether this is a bright (high-intensity) color.
     *
     * @return true if this is one of the BRIGHT_* colors (SGR value 8-15)
     */
    public boolean isBright() {
        return value >= BRIGHT_OFFSET;
    }

    /**
     * Get the bright (high-intensity) variant of this color.
     *
     * @return the matching BRIGHT_* color, or this color if it is already
     * bright
     */
    public Color toBright() {
        if (isBright()) {
            return this;
        }
        return getSgrColor(value + BRIGHT_OFFSET);
    }

    /**
     * Get the normal (non-bright) variant of this color.
     *
     * @return the matching normal color, or this color if it is already
     * normal
     */
    public Color toNormal() {
        if (isBright()) {
            return getSgrColor(value - BRIGHT_OFFSET);
        }
        return this;
    }

    /**
     * Public constructor returns one of the static Color instances.
     *
     * @param colorName "red", "blue", etc.
     * @return Color.RED, Color.BLUE, etc.
     */
    static Color getColor(final String colorName) {
        String str = colorName.toLowerCase();

        return switch (str) {
            case "black" -> Color.BLACK;
            case "white" -> Color.WHITE;
            case "red" -> Color.RED;
            case "cyan" -> Color.CYAN;
            case "green" -> Color.GREEN;
            case "magenta" -> Color.MAGENTA;
            case "blue" -> Color.BLUE;
            case "yellow" -> Color.YELLOW;
            case "brown" -> Color.YELLOW;
            default ->
                // Let unknown strings become white
                Color.WHITE;
        };
    }

    /**
     * Invert a color in the same way as (CGA/VGA color XOR 0x7).
     *
     * @return the inverted color
     */
    public Color invert() {
        Color base = toNormal();
        Color inverted = switch (base.value) {
            case SGRBLACK -> Color.WHITE;
            case SGRWHITE -> Color.BLACK;
            case SGRRED -> Color.CYAN;
            case SGRCYAN -> Color.RED;
            case SGRGREEN -> Color.MAGENTA;
            case SGRMAGENTA -> Color.GREEN;
            case SGRBLUE -> Color.YELLOW;
            case SGRYELLOW -> Color.BLUE;
            default -> throw new IllegalArgumentException("Invalid Color value: " + value);
        };
        return isBright() ? inverted.toBright() : inverted;
    }

    /**
     * Comparison check.  All fields must match to return true.
     *
     * @param rhs another Color instance
     * @return true if all fields are equal
     */
    @Override
    public boolean equals(final Object rhs) {
        if (!(rhs instanceof Color that)) {
            return false;
        }

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
        String prefix = isBright() ? "bright " : "";
        return prefix + switch (toNormal().value) {
            case SGRBLACK -> "black";
            case SGRWHITE -> "white";
            case SGRRED -> "red";
            case SGRCYAN -> "cyan";
            case SGRGREEN -> "green";
            case SGRMAGENTA -> "magenta";
            case SGRBLUE -> "blue";
            case SGRYELLOW -> "yellow";
            default -> throw new IllegalArgumentException("Invalid Color value: " + value);
        };
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
     * @param bright if true, return the bright color
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

        if (bright || isBright()) {
            return brightColors[value & 0x07];
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
        return switch (sgrValue) {
            case 0 -> Color.BLACK;
            case 1 -> Color.RED;
            case 2 -> Color.GREEN;
            case 3 -> Color.YELLOW;
            case 4 -> Color.BLUE;
            case 5 -> Color.MAGENTA;
            case 6 -> Color.CYAN;
            case 7 -> Color.WHITE;
            case 8 -> Color.BRIGHT_BLACK;
            case 9 -> Color.BRIGHT_RED;
            case 10 -> Color.BRIGHT_GREEN;
            case 11 -> Color.BRIGHT_YELLOW;
            case 12 -> Color.BRIGHT_BLUE;
            case 13 -> Color.BRIGHT_MAGENTA;
            case 14 -> Color.BRIGHT_CYAN;
            case 15 -> Color.BRIGHT_WHITE;
            default -> throw new IllegalArgumentException("Invalid Color value: " +
                sgrValue);
        };
    }

}
