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
 * A record to hold RGB color components with utility methods for color
 * manipulation and conversion.
 *
 * <p>This record provides methods for:
 * <ul>
 *   <li>Extracting components from packed RGB integers</li>
 *   <li>Combining components into packed integers</li>
 *   <li>Computing color distances</li>
 *   <li>Converting to sixel color space</li>
 *   <li>Alpha/transparency handling</li>
 * </ul>
 *
 * @param r the red component (0-255 for RGB, 0-100 for sixel)
 * @param g the green component (0-255 for RGB, 0-100 for sixel)
 * @param b the blue component (0-255 for RGB, 0-100 for sixel)
 */
public record Rgb(int r, int g, int b) {

    // ========================================================================
    // Constants
    // ========================================================================

    /** Alpha value (0 - 255) above which to consider the pixel opaque (~40%). */
    public static final int ALPHA_OPAQUE = 102;

    /** Sixel white color value (100, 100, 100). */
    public static final int SIXEL_WHITE = 0xFF646464;

    /** Sixel black color value (0, 0, 0). */
    public static final int SIXEL_BLACK = 0xFF000000;

    // ========================================================================
    // Factory Methods
    // ========================================================================

    /**
     * Extract RGB components from a packed integer.
     *
     * @param rgb the packed RGB integer
     * @return a record containing the red, green, and blue components
     */
    public static Rgb fromPackedRgb(final int rgb) {
        int red = (rgb >>> 16) & 0xFF;
        int green = (rgb >>> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return new Rgb(red, green, blue);
    }

    // ========================================================================
    // Instance Methods
    // ========================================================================

    /**
     * Combine the RGB components into a packed opaque (alpha=0xFF) integer.
     *
     * @return the packed ARGB integer
     */
    public int toPackedRgb() {
        return (0xFF << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /**
     * Combine the RGB components into a packed integer without alpha.
     *
     * @return the packed RGB integer (no alpha)
     */
    public int toPackedRgbNoAlpha() {
        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /**
     * Calculate the squared Euclidean distance to another color.
     *
     * @param other the other color
     * @return the squared distance
     */
    public int distanceSquaredTo(final Rgb other) {
        int dr = this.r - other.r;
        int dg = this.g - other.g;
        int db = this.b - other.b;
        return dr * dr + dg * dg + db * db;
    }

    /**
     * Calculate the squared Euclidean distance to RGB components.
     *
     * @param red the red component
     * @param green the green component
     * @param blue the blue component
     * @return the squared distance
     */
    public int distanceSquaredTo(final int red, final int green, final int blue) {
        int dr = this.r - red;
        int dg = this.g - green;
        int db = this.b - blue;
        return dr * dr + dg * dg + db * db;
    }

    /**
     * Convert to sixel color space (0-100 per component).
     *
     * @return a new Rgb with components scaled to 0-100
     */
    public Rgb toSixelSpace() {
        return new Rgb(r * 100 / 255, g * 100 / 255, b * 100 / 255);
    }

    /**
     * Clamp components to sixel range [0, 100].
     *
     * @return a new Rgb with clamped components
     */
    public Rgb clampSixel() {
        return new Rgb(
            Math.max(0, Math.min(r, 100)),
            Math.max(0, Math.min(g, 100)),
            Math.max(0, Math.min(b, 100))
        );
    }

    /**
     * Add error values to components (for dithering).
     *
     * @param redError the red error to add
     * @param greenError the green error to add
     * @param blueError the blue error to add
     * @return a new Rgb with error added and clamped to [0, 100]
     */
    public Rgb addErrorAndClamp(final int redError, final int greenError,
            final int blueError) {
        return new Rgb(
            Math.max(0, Math.min(r + redError, 100)),
            Math.max(0, Math.min(g + greenError, 100)),
            Math.max(0, Math.min(b + blueError, 100))
        );
    }

    /**
     * Check if this color is near black (in sixel space).
     *
     * @param threshold the distance threshold
     * @return true if near black
     */
    public boolean isNearBlack(final int threshold) {
        return (r * r + g * g + b * b) < threshold;
    }

    /**
     * Check if this color is near white (in sixel space, where white is 100,100,100).
     *
     * @param threshold the distance threshold
     * @return true if near white
     */
    public boolean isNearWhite(final int threshold) {
        int dr = 100 - r;
        int dg = 100 - g;
        int db = 100 - b;
        return (dr * dr + dg * dg + db * db) < threshold;
    }

    // ========================================================================
    // Static Utility Methods
    // ========================================================================

    /**
     * Extract the red component from a packed RGB integer.
     *
     * @param rgb the packed RGB integer
     * @return the red component (0-255)
     */
    public static int getRed(final int rgb) {
        return (rgb >>> 16) & 0xFF;
    }

    /**
     * Extract the green component from a packed RGB integer.
     *
     * @param rgb the packed RGB integer
     * @return the green component (0-255)
     */
    public static int getGreen(final int rgb) {
        return (rgb >>> 8) & 0xFF;
    }

    /**
     * Extract the blue component from a packed RGB integer.
     *
     * @param rgb the packed RGB integer
     * @return the blue component (0-255)
     */
    public static int getBlue(final int rgb) {
        return rgb & 0xFF;
    }

    /**
     * Extract the alpha component from a packed ARGB integer.
     *
     * @param argb the packed ARGB integer
     * @return the alpha component (0-255)
     */
    public static int getAlpha(final int argb) {
        return (argb >>> 24) & 0xFF;
    }

    /**
     * Combine RGB components into a packed opaque integer.
     *
     * @param red the red component (0-255)
     * @param green the green component (0-255)
     * @param blue the blue component (0-255)
     * @return the packed ARGB integer with alpha=0xFF
     */
    public static int combineRgb(final int red, final int green, final int blue) {
        return (0xFF << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    /**
     * Combine ARGB components into a packed integer.
     *
     * @param alpha the alpha component (0-255)
     * @param red the red component (0-255)
     * @param green the green component (0-255)
     * @param blue the blue component (0-255)
     * @return the packed ARGB integer
     */
    public static int combineArgb(final int alpha, final int red,
            final int green, final int blue) {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16)
               | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    /**
     * Check if a pixel is fully opaque (alpha = 0xFF).
     *
     * @param argb the packed ARGB integer
     * @return true if opaque
     */
    public static boolean isOpaque(final int argb) {
        return (argb & 0xFF000000) == 0xFF000000;
    }

    /**
     * Check if a pixel is transparent (alpha below ALPHA_OPAQUE threshold).
     *
     * @param argb the packed ARGB integer
     * @return true if transparent
     */
    public static boolean isTransparent(final int argb) {
        return getAlpha(argb) < ALPHA_OPAQUE;
    }

    /**
     * Calculate the squared Euclidean distance between two packed RGB colors.
     *
     * @param rgb1 the first color
     * @param rgb2 the second color
     * @return the squared distance
     */
    public static int distanceSquared(final int rgb1, final int rgb2) {
        int dr = getRed(rgb1) - getRed(rgb2);
        int dg = getGreen(rgb1) - getGreen(rgb2);
        int db = getBlue(rgb1) - getBlue(rgb2);
        return dr * dr + dg * dg + db * db;
    }

    /**
     * Calculate the squared distance between a packed RGB color and components.
     *
     * @param rgb the packed RGB color
     * @param red the red component
     * @param green the green component
     * @param blue the blue component
     * @return the squared distance
     */
    public static int distanceSquared(final int rgb, final int red,
            final int green, final int blue) {
        int dr = getRed(rgb) - red;
        int dg = getGreen(rgb) - green;
        int db = getBlue(rgb) - blue;
        return dr * dr + dg * dg + db * db;
    }

    /**
     * Clamp a value to the sixel range [0, 100].
     *
     * @param value the value to clamp
     * @return the clamped value
     */
    public static int clampSixelValue(final int value) {
        return Math.max(0, Math.min(value, 100));
    }

    /**
     * Convert a 24-bit RGB color to sixel color space.
     *
     * @param rgb the 24-bit RGB color
     * @return the sixel color (components 0-100)
     */
    public static int toSixelColor(final int rgb) {
        int r = getRed(rgb) * 100 / 255;
        int g = getGreen(rgb) * 100 / 255;
        int b = getBlue(rgb) * 100 / 255;
        return combineRgb(r, g, b);
    }

    /**
     * Convert a 24-bit RGB color to sixel color space, with optional
     * black/white mapping for colors close to those extremes.
     *
     * @param rgb the 24-bit RGB color
     * @param checkBlackWhite if true, map near-black to black and near-white to white
     * @return the sixel color
     */
    public static int toSixelColor(final int rgb, final boolean checkBlackWhite) {
        int r = getRed(rgb) * 100 / 255;
        int g = getGreen(rgb) * 100 / 255;
        int b = getBlue(rgb) * 100 / 255;

        if (!checkBlackWhite) {
            return combineRgb(r, g, b);
        }

        // Thresholds for black/white mapping
        final int blackThreshold = 10;
        final int whiteThreshold = 0;

        if ((r * r + g * g + b * b) < blackThreshold) {
            return SIXEL_BLACK;
        }

        int dr = 100 - r;
        int dg = 100 - g;
        int db = 100 - b;
        if ((dr * dr + dg * dg + db * db) < whiteThreshold) {
            return SIXEL_WHITE;
        }

        return combineRgb(r, g, b);
    }
}
