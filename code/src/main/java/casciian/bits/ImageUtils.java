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
 * ImageUtils contains methods to:
 * <p>
 *    - Check if an image is fully transparent.
 * <p>
 *    - Compute the distance between two colors in RGB space.
 * <p>
 *    - Compute the partial movement between two colors in RGB space.
 *
 * <p>Note: Most RGB manipulation methods are now available in the
 * {@link Rgb} record class.
 */
public class ImageUtils {

    /**
     * Private constructor prevents accidental creation of this class.
     */
    private ImageUtils() {}

    /**
     * Report the absolute distance in RGB space between two RGB colors.
     *
     * @param first the first color
     * @param second the second color
     * @return the distance
     */
    public static int rgbDistance(final int first, final int second) {
        return (int) Math.sqrt(Rgb.distanceSquared(first, second));
    }

    /**
     * Move from one point in RGB space to another, by a certain fraction.
     *
     * @param start the starting point color
     * @param end the ending point color
     * @param fraction the amount of movement between start and end, between
     * 0.0 (start) and 1.0 (end).
     * @return the final color
     */
    public static int rgbMove(final int start, final int end,
            final double fraction) {

        if (fraction <= 0) {
            return start;
        }
        if (fraction >= 1) {
            return end;
        }

        Rgb color1 = Rgb.fromPackedRgb(start);
        Rgb color2 = Rgb.fromPackedRgb(end);

        int rgbRed   = Rgb.clampRgbValue(color1.r() + (int) (fraction * (color2.r() - color1.r())));
        int rgbGreen = Rgb.clampRgbValue(color1.g() + (int) (fraction * (color2.g() - color1.g())));
        int rgbBlue  = Rgb.clampRgbValue(color1.b() + (int) (fraction * (color2.b() - color1.b())));

        return Rgb.combineRgb(rgbRed, rgbGreen, rgbBlue);
    }

    /**
     * Blend two RGB colors using alpha compositing.
     * <p>
     * The result is computed as: result = (under * (1 - alpha)) + (over * alpha)
     * where alpha is between 0.0 (fully under color) and 1.0 (fully over color).
     *
     * @param alpha the blend factor between 0.0 and 1.0, where 0.0 returns
     * the under color and 1.0 returns the over color
     * @param underRGB the RGB color underneath (background)
     * @param overRGB the RGB color on top (foreground)
     * @return the blended RGB color as a packed integer
     */
    public static int blendColors(final double alpha, final int underRGB,
            final int overRGB) {
        Rgb under = Rgb.fromPackedRgb(underRGB);
        Rgb over = Rgb.fromPackedRgb(overRGB);

        int red = (int) ((under.r() * (1.0 - alpha)) + (over.r() * alpha));
        int green = (int) ((under.g() * (1.0 - alpha)) + (over.g() * alpha));
        int blue = (int) ((under.b() * (1.0 - alpha)) + (over.b() * alpha));
        return Rgb.combineRgb(red, green, blue);
    }

    /**
     * Combine RGB components into a single integer.
     *
     * @param red the red component (0-255)
     * @param green the green component (0-255)
     * @param blue the blue component (0-255)
     * @return the packed RGB integer
     * @deprecated Use {@link Rgb#combineRgb(int, int, int)} instead
     */
    @Deprecated
    public static int combineRgb(final int red, final int green, final int blue) {
        return Rgb.combineRgb(red, green, blue);
    }

    /**
     * Extract RGB components from a packed integer.
     *
     * @param rgb the packed RGB integer
     * @return a record containing the red, green, and blue components
     * @deprecated Use {@link Rgb#fromPackedRgb(int)} instead
     */
    @Deprecated
    public static Rgb extractComponents(final int rgb) {
        return Rgb.fromPackedRgb(rgb);
    }
}
