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
 * UnicodeGlyphImage constructs a single character from the Unicode
 * block-drawing elements ("Symbols For Legacy Computing") from a bitmap
 * image.
 */
public class UnicodeGlyphImage {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The bitmap image this glyph is supposed to represent.
     */
    private ImageRGB image = null;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param cell a Cell to read image data from
     * @throws IllegalArgumentException if cell does not have an image
     */
    public UnicodeGlyphImage(final Cell cell) throws IllegalArgumentException {
        if (!cell.isImage()) {
            throw new IllegalArgumentException("cell does not have an image");
        }
        this.image = cell.getImage();
    }

    /**
     * Public constructor.
     *
     * @param image the bitmap image
     */
    public UnicodeGlyphImage(final ImageRGB image) {
        this.image = image;
    }

    // ------------------------------------------------------------------------
    // UnicodeGlyphImage ------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Create a single glyph using Unicode half-blocks that best represents
     * this entire image.
     *
     * @return a cell with character, foreground, and background color set
     */
    public Cell toHalfBlockGlyph() {
        Cell cell;

        int width = image.getWidth();
        int height = image.getHeight();

        // Try left half, top half, and full block, and whichever has the
        // least relative difference to the image is what we return.
        double bestStdDev;
        int ch = 0x258c;
        int foreColorRGB;
        int backColorRGB;

        // Left half.
        foreColorRGB = ImageUtils.rgbAverage(image, 0, 0, width / 2, height);
        backColorRGB = ImageUtils.rgbAverage(image, width / 2, 0,
            width - width / 2, height);
        bestStdDev = computeRegionStdDev(image, foreColorRGB,
            backColorRGB, true, width, height);

        // Top half
        int newForeColorRGB = ImageUtils.rgbAverage(image, 0, 0, width,
            height / 2);
        int newBackColorRGB = ImageUtils.rgbAverage(image, 0, height / 2,
            width, height - height / 2);
        double newRgbStdDev = computeRegionStdDev(image, newForeColorRGB,
            newBackColorRGB, false, width, height);
        if (newRgbStdDev < bestStdDev) {
            ch = 0x2580;
            foreColorRGB = newForeColorRGB;
            backColorRGB = newBackColorRGB;
            bestStdDev = newRgbStdDev;
        }

        // Full block
        int newColorRGB = ImageUtils.rgbAverage(image, 0, 0, width, height);
        newRgbStdDev = computeFullBlockStdDev(image, newColorRGB,
            width, height);
        if (newRgbStdDev < bestStdDev) {
            ch = 0x2588;
            foreColorRGB = newColorRGB;
            backColorRGB = newColorRGB;
        }

        cell = new Cell(ch);
        cell.setBackColorRGB(backColorRGB);
        cell.setForeColorRGB(foreColorRGB);

        return cell;
    }

    /**
     * Compute the standard deviation between the image and a two-region
     * split (left/right or top/bottom).
     *
     * @param image the source image
     * @param foreColor the average color for the foreground region
     * @param backColor the average color for the background region
     * @param leftRight if true, split left/right; if false, split top/bottom
     * @param width the image width
     * @param height the image height
     * @return the standard deviation
     */
    private double computeRegionStdDev(final ImageRGB image,
        final int foreColor, final int backColor,
        final boolean leftRight, final int width, final int height) {

        long totalDiffSquared = 0;
        int pixelCount = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                int avgColor;
                if (leftRight) {
                    avgColor = (x < width / 2) ? foreColor : backColor;
                } else {
                    avgColor = (y < height / 2) ? foreColor : backColor;
                }
                totalDiffSquared += Rgb.distanceSquared(pixel, avgColor);
                pixelCount++;
            }
        }
        if (pixelCount == 0) {
            return 0;
        }
        return Math.sqrt((double) totalDiffSquared / pixelCount);
    }

    /**
     * Compute the standard deviation between the image and a single color.
     *
     * @param image the source image
     * @param color the uniform color
     * @param width the image width
     * @param height the image height
     * @return the standard deviation
     */
    private double computeFullBlockStdDev(final ImageRGB image,
        final int color, final int width, final int height) {

        long totalDiffSquared = 0;
        int pixelCount = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                totalDiffSquared += Rgb.distanceSquared(pixel, color);
                pixelCount++;
            }
        }
        if (pixelCount == 0) {
            return 0;
        }
        return Math.sqrt((double) totalDiffSquared / pixelCount);
    }
}
