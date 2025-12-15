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
 *
 *    - Check if an image is fully transparent.
 *
 *    - Compute the distance between two colors in RGB space.
 *
 *    - Compute the partial movement between two colors in RGB space.
 */
public class ImageUtils {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Private constructor prevents accidental creation of this class.
     */
    private ImageUtils() {}

    // ------------------------------------------------------------------------
    // ImageUtils -------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Report the absolute distance in RGB space between two RGB colors.
     *
     * @param first the first color
     * @param second the second color
     * @return the distance
     */
    public static int rgbDistance(final int first, final int second) {
        int red   = (first >>> 16) & 0xFF;
        int green = (first >>>  8) & 0xFF;
        int blue  =  first         & 0xFF;
        int red2   = (second >>> 16) & 0xFF;
        int green2 = (second >>>  8) & 0xFF;
        int blue2  =  second         & 0xFF;
        double diff = Math.pow(red2 - red, 2);
        diff += Math.pow(green2 - green, 2);
        diff += Math.pow(blue2 - blue, 2);
        return (int) Math.sqrt(diff);
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

        int red   = (start >>> 16) & 0xFF;
        int green = (start >>>  8) & 0xFF;
        int blue  =  start         & 0xFF;
        int red2   = (end >>> 16) & 0xFF;
        int green2 = (end >>>  8) & 0xFF;
        int blue2  =  end         & 0xFF;

        int rgbRed   =   red + (int) (fraction * (  red2 - red));
        int rgbGreen = green + (int) (fraction * (green2 - green));
        int rgbBlue  =  blue + (int) (fraction * ( blue2 - blue));

        rgbRed   = Math.min(Math.max(  rgbRed, 0), 255);
        rgbGreen = Math.min(Math.max(rgbGreen, 0), 255);
        rgbBlue  = Math.min(Math.max( rgbBlue, 0), 255);

        return (rgbRed << 16) | (rgbGreen << 8) | rgbBlue;
    }

}
