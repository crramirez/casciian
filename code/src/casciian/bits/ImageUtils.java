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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import casciian.backend.Backend;

/**
 * ImageUtils contains methods to:
 *
 *    - Check if an image is fully transparent.
 *
 *    - Scale an image and preserve aspect ratio.
 *
 *    - Break an image up into cells and optionally convert to Unicode glyphs.
 *
 *    - Open an animated image as an Animation.
 *
 *    - Compute the distance between two colors in RGB space.
 *
 *    - Compute the partial movement between two colors in RGB space.
 */
public class ImageUtils {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Selections for fitting an image to the text cells.
     */
    public enum Scale {
        /**
         * Stretch/shrink the image in both directions to fully fill the text
         * area width/height.
         */
        STRETCH,

        /**
         * Scale the image, preserving aspect ratio, to fill the text area
         * width/height (like letterbox).  The background color for the
         * letterboxed area is specified in the backColor argument to
         * scaleImage().
         */
        SCALE,
    }

    /**
     * Selections for approximating an image as text cells.
     */
    public enum DisplayMode {
        /**
         * Bitmap image.
         */
        BITMAP,

        /**
         * Converted to solid space (' ') character blocks.
         */
        BLOCKS,

        /**
         * Converted to Unicode half-block glyphs.
         */
        UNICODE_HALVES,

        /**
         * Converted to Unicode sextant glyphs.
         */
        UNICODE_SEXTANTS,

        /**
         * Converted to Unicode quadrant-block glyphs.
         */
        UNICODE_QUADRANTS,

        /**
         * Converted to Unicode 6-dot Braille glyphs on this window's
         * background color.
         */
        UNICODE_SIXDOT,

        /**
         * Converted to Unicode 6-dot Braille glyphs with
         * foreground/background color.
         */
        UNICODE_SIXDOTSOLID,
    }

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
     * Check if any pixels in an image have not-0% alpha value.
     *
     * @param image the image to check
     * @return true if every pixel is fully transparent
     */
    public static boolean isFullyTransparent(final BufferedImage image) {
        assert (image != null);

        int [] rgbArray = image.getRGB(0, 0,
            image.getWidth(), image.getHeight(), null, 0, image.getWidth());

        if (rgbArray.length == 0) {
            // No image data, fully transparent.
            return true;
        }

        for (int i = 0; i < rgbArray.length; i++) {
            int alpha = (rgbArray[i] >>> 24) & 0xFF;
            if (alpha != 0x00) {
                // A not-fully transparent pixel is found.
                return false;
            }
        }
        // Every pixel was transparent.
        return true;
    }

    /**
     * Check if any pixels in an image have not-100% alpha value.
     *
     * @param image the image to check
     * @return true if every pixel is fully transparent
     */
    public static boolean isFullyOpaque(final BufferedImage image) {
        assert (image != null);

        int [] rgbArray = image.getRGB(0, 0,
            image.getWidth(), image.getHeight(), null, 0, image.getWidth());

        if (rgbArray.length == 0) {
            // No image data, fully transparent.
            return true;
        }

        for (int i = 0; i < rgbArray.length; i++) {
            int alpha = (rgbArray[i] >>> 24) & 0xFF;
            if (alpha != 0xFF) {
                // A partially transparent pixel is found.
                return false;
            }
        }
        // Every pixel was opaque.
        return true;
    }

    /**
     * Scale an image to be scaleFactor size and/or stretch it to fit a
     * target box.
     *
     * @param image the image to scale
     * @param width the width in pixels for the destination image
     * @param height the height in pixels for the destination image
     * @param scale the scaling type
     * @param backColor the background color to use for Scale.SCALE
     * @return the scaled image
     */
    public static BufferedImage scaleImage(final BufferedImage image,
        final int width, final int height,
        final Scale scale, final java.awt.Color backColor) {

        BufferedImage newImage = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);

        int x = 0;
        int y = 0;
        int destWidth = width;
        int destHeight = height;
        switch (scale) {
        case STRETCH:
            break;
        case SCALE:
            double a = (double) image.getWidth() / image.getHeight();
            double b = (double) width / height;
            double h = (double) height / image.getHeight();
            double w = (double) width / image.getWidth();
            assert (a > 0);
            assert (b > 0);

            if (a > b) {
                // Horizontal letterbox
                destHeight = (int) (image.getWidth() / a * w);
                destWidth = (int) (image.getWidth() * w);
                y = (height - destHeight) / 2;
                assert (y >= 0);
            } else {
                // Vertical letterbox
                destHeight = (int) (image.getHeight() * h);
                destWidth = (int) (image.getHeight() * a * h);
                x = (width - destWidth) / 2;
                assert (x >= 0);
            }
            break;
        }

        java.awt.Graphics gr = newImage.createGraphics();
        if (scale == Scale.SCALE) {
            gr.setColor(backColor);
            gr.fillRect(0, 0, newImage.getWidth(), newImage.getHeight());
        }
        gr.drawImage(image, x, y, destWidth, destHeight, null);
        gr.dispose();
        return newImage;
    }

    /**
     * Convert an image to a 2D array of cells.
     *
     * @param image the image to convert
     * @param backend the backend that can obtain the correct background
     * color for a text cell
     * @param textWidth the width of a cell in pixels
     * @param textHeight the height of a cell in pixels
     * @param displayMode option to convert image to Unicode glyphs
     * @param backColor the color to use for the right and bottom padding to
     * the text cell boundary
     * @param bleedThrough if true, then image cells that are fully covered
     * by a single color will be replaced by empty text cells of that color,
     * permitting text "behind" that cell to bleed through if translucence is
     * enabled
     * @return a two-dimensional array of cells (cells[x][y])
     */
    public static Cell [][] imageToCells(final Backend backend,
        final BufferedImage image, final int textWidth, final int textHeight,
        final DisplayMode displayMode, final java.awt.Color backColor,
        final boolean bleedThrough) {

        assert (image != null);

        int cellColumns = image.getWidth() / textWidth;
        if (cellColumns * textWidth < image.getWidth()) {
            cellColumns++;
        }
        int cellRows = image.getHeight() / textHeight;
        if (cellRows * textHeight < image.getHeight()) {
            cellRows++;
        }

        // Break the image up into an array of cells.
        Cell [][] cells = new Cell[cellColumns][cellRows];

        int imageId = System.identityHashCode(ImageUtils.class);
        imageId ^= (int) System.currentTimeMillis();
        for (int x = 0; x < cellColumns; x++) {
            for (int y = 0; y < cellRows; y++) {

                int width = textWidth;
                if ((x + 1) * textWidth > image.getWidth()) {
                    width = image.getWidth() - (x * textWidth);
                }
                int height = textHeight;
                if ((y + 1) * textHeight > image.getHeight()) {
                    height = image.getHeight() - (y * textHeight);
                }

                Cell cell = new Cell();
                cell.setBackColorRGB(backColor.getRGB());

                // Render over a full-cell-size image.
                BufferedImage newImage = ImageUtils.createImage(image,
                    textWidth, textHeight);
                Graphics gr = newImage.getGraphics();
                BufferedImage subImage = image.getSubimage(x * textWidth,
                    y * textHeight, width, height);
                gr.drawImage(subImage, 0, 0, null, null);
                gr.dispose();

                cell.setImage(newImage);
                if (!isFullyTransparent(newImage)) {
                    cell.flattenImage(false, backend);
                } else {
                    cell.setTo(backColor);
                }
                if ((displayMode != DisplayMode.BITMAP)
                    || (cell.checkForSingleColor(!bleedThrough) == false)
                ) {
                    imageId++;
                    cell.setImageId(imageId & 0x7FFFFFFF);
                }
                switch (displayMode) {
                case BITMAP:
                    cells[x][y] = cell;
                    break;
                case BLOCKS:
                    if (cell.isImage()) {
                        int rgb = ImageUtils.rgbAverage(cell.getImage());
                        Cell newCell = new Cell(' ');
                        newCell.setForeColorRGB(rgb);
                        newCell.setBackColorRGB(rgb);
                        cells[x][y] = newCell;
                    } else {
                        cells[x][y] = cell;
                    }
                    break;
                case UNICODE_HALVES:
                    if (cell.isImage()) {
                        UnicodeGlyphImage ch = new UnicodeGlyphImage(cell);
                        cells[x][y] = ch.toHalfBlockGlyph();
                    } else {
                        cells[x][y] = cell;
                    }
                    break;
                case UNICODE_SEXTANTS:
                    if (cell.isImage()) {
                        UnicodeGlyphImage ch = new UnicodeGlyphImage(cell);
                        cells[x][y] = ch.toSextantBlockGlyph();
                    } else {
                        cells[x][y] = cell;
                    }
                    break;
                case UNICODE_QUADRANTS:
                    if (cell.isImage()) {
                        UnicodeGlyphImage ch = new UnicodeGlyphImage(cell);
                        cells[x][y] = ch.toQuadrantBlockGlyph();
                    } else {
                        cells[x][y] = cell;
                    }
                    break;
                case UNICODE_SIXDOT:
                    if (cell.isImage()) {
                        UnicodeGlyphImage ch = new UnicodeGlyphImage(cell);
                        cells[x][y] = ch.toSixDotGlyph();
                        cells[x][y].setBackColorRGB(backColor.getRGB());
                    } else {
                        cells[x][y] = cell;
                    }
                    break;
                case UNICODE_SIXDOTSOLID:
                    if (cell.isImage()) {
                        UnicodeGlyphImage ch = new UnicodeGlyphImage(cell);
                        cells[x][y] = ch.toSixDotSolidGlyph();
                    } else {
                        cells[x][y] = cell;
                    }
                    break;
                }
            }
        }

        return cells;
    }

    /**
     * Open an image as an Animation.
     *
     * @param filename the name of the file that contains an animation
     * @return the animation, or null on error
     */
    public static Animation getAnimation(final String filename) {
        try {
            return getAnimation(new FileInputStream(filename));
        } catch (IOException e) {
            // SQUASH
            return null;
        }
    }

    /**
     * Open an image as an Animation.
     *
     * @param file the file that contains an animation
     * @return the animation, or null on error
     */
    public static Animation getAnimation(final File file) {
        try {
            return getAnimation(new FileInputStream(file));
        } catch (IOException e) {
            // SQUASH
            return null;
        }
    }

    /**
     * Open an image as an Animation.
     *
     * @param url the URK that contains an animation
     * @return the animation, or null on error
     */
    public static Animation getAnimation(final URL url) {
        try {
            return getAnimation(url.openStream());
        } catch (IOException e) {
            // SQUASH
            return null;
        }
    }

    /**
     * Open an image as an Animation.
     *
     * @param inputStream the inputStream that contains an animation
     * @return the animation, or null on error
     */
    public static Animation getAnimation(final InputStream inputStream) {
        try {
            List<BufferedImage> frames = new LinkedList<BufferedImage>();
            List<String> disposals = new LinkedList<String>();
            int delays = 0;

            /*
             * Assume infinite loop.  Finite-count looping in GIFs is an
             * Application Extension made popular by Netscape 2.0: see
             * http://giflib.sourceforge.net/whatsinagif/bits_and_bytes.html
             * .
             *
             * Unfortunately the Sun GIF decoder did not read and expose
             * this.
             */
            int loopCount = 0;

            ImageReader reader = null;
            ImageInputStream stream;
            stream = ImageIO.createImageInputStream(inputStream);
            Iterator<ImageReader> iter = ImageIO.getImageReaders(stream);
            while (iter.hasNext()) {
                reader = iter.next();
                break;
            }
            if (reader == null) {
                return null;
            }
            reader.setInput(stream);

            int width = -1;
            int height = -1;
            java.awt.Color backgroundColor = null;

            IIOMetadata metadata = reader.getStreamMetadata();
            if (metadata != null) {
                IIOMetadataNode gblRoot;
                gblRoot = (IIOMetadataNode) metadata.getAsTree(metadata.
                    getNativeMetadataFormatName());
                NodeList gblScreenDesc;
                gblScreenDesc = gblRoot.getElementsByTagName(
                        "LogicalScreenDescriptor");
                if ((gblScreenDesc != null)
                    && (gblScreenDesc.getLength() > 0)
                ) {
                    IIOMetadataNode screenDescriptor;
                    screenDescriptor = (IIOMetadataNode) gblScreenDesc.item(0);

                    if (screenDescriptor != null) {
                        width = Integer.parseInt(screenDescriptor.
                            getAttribute("logicalScreenWidth"));
                        height = Integer.parseInt(screenDescriptor.
                            getAttribute("logicalScreenHeight"));
                    }
                }
                NodeList gblColorTable = gblRoot.getElementsByTagName(
                        "GlobalColorTable");

                if ((gblColorTable != null)
                    && (gblColorTable.getLength() > 0)
                ) {
                    IIOMetadataNode colorTable = (IIOMetadataNode) gblColorTable.item(0);

                    if (colorTable != null) {
                        String bgIndex = colorTable.getAttribute(
                                "backgroundColorIndex");

                        IIOMetadataNode color;
                        color = (IIOMetadataNode) colorTable.getFirstChild();
                        while (color != null) {
                            if (color.getAttribute("index").equals(bgIndex)) {
                                int red = Integer.parseInt(
                                        color.getAttribute("red"));
                                int green = Integer.parseInt(
                                        color.getAttribute("green"));
                                int blue = Integer.parseInt(
                                        color.getAttribute("blue"));
                                backgroundColor = new java.awt.Color(red,
                                    green, blue);
                                break;
                            }

                            color = (IIOMetadataNode) color.getNextSibling();
                        }
                    }
                }

            }
            BufferedImage master = null;
            Graphics2D masterGraphics = null;
            int lastx = 0;
            int lasty = 0;
            boolean hasBackround = false;

            for (int frameIndex = 0; ; frameIndex++) {
                BufferedImage image;
                try {
                    image = reader.read(frameIndex);
                } catch (IndexOutOfBoundsException io) {
                    break;
                }
                assert (image != null);

                if (width == -1 || height == -1) {
                    width = image.getWidth();
                    height = image.getHeight();
                }
                IIOMetadataNode root;
                root = (IIOMetadataNode) reader.getImageMetadata(frameIndex).
                        getAsTree("javax_imageio_gif_image_1.0");
                IIOMetadataNode gce;
                gce = (IIOMetadataNode) root.getElementsByTagName(
                        "GraphicControlExtension").item(0);
                int delay = Integer.valueOf(gce.getAttribute("delayTime"));
                String disposal = gce.getAttribute("disposalMethod");

                int x = 0;
                int y = 0;

                if (master == null) {
                    master = new BufferedImage(width, height,
                        BufferedImage.TYPE_INT_ARGB);
                    masterGraphics = master.createGraphics();
                    masterGraphics.setBackground(new java.awt.Color(0, 0, 0, 0));
                    if ((image.getWidth() == width)
                        && (image.getHeight() == height)
                    ) {
                        hasBackround = true;
                    }
                } else {
                    NodeList children = root.getChildNodes();
                    for (int nodeIndex = 0; nodeIndex < children.getLength();
                         nodeIndex++) {

                        Node nodeItem = children.item(nodeIndex);
                        if (nodeItem.getNodeName().equals("ImageDescriptor")) {
                            NamedNodeMap map = nodeItem.getAttributes();
                            x = Integer.valueOf(map.getNamedItem(
                                "imageLeftPosition").getNodeValue());
                            y = Integer.valueOf(map.getNamedItem(
                                "imageTopPosition").getNodeValue());
                        }
                    }
                }
                masterGraphics.drawImage(image, x, y, null);
                lastx = x;
                lasty = y;

                BufferedImage copy = new BufferedImage(master.getColorModel(),
                    master.copyData(null), master.isAlphaPremultiplied(), null);
                frames.add(copy);
                disposals.add(disposal);
                delays += delay;

                if (disposal.equals("restoreToPrevious")) {
                    BufferedImage from = null;
                    for (int i = frameIndex - 1; i >= 0; i--) {
                        if (!disposals.get(i).equals("restoreToPrevious")
                            || (frameIndex == 0)
                        ) {
                            from = frames.get(i);
                            break;
                        }
                    }

                    master = new BufferedImage(from.getColorModel(),
                        from.copyData(null), from.isAlphaPremultiplied(), null);
                    masterGraphics = master.createGraphics();
                    masterGraphics.setBackground(new java.awt.Color(0, 0, 0, 0));
                } else if (disposal.equals("restoreToBackgroundColor")
                    && (backgroundColor != null)) {

                    if (!hasBackround || (frameIndex > 1)) {
                        master.createGraphics().fillRect(lastx, lasty,
                            frames.get(frameIndex - 1).getWidth(),
                            frames.get(frameIndex - 1).getHeight());
                    }
                }
            }
            reader.dispose();

            if (frames.size() == 1) {
                loopCount = 1;
            }
            if (frames.size() == 0) {
                return null;
            }
            Animation animation = new Animation(frames,
                (delays * 10 / frames.size()), loopCount);
            return animation;

        } catch (IOException e) {
            // SQUASH
            return null;
        }
    }

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

    /**
     * Compute the average RGB value of an entire image, including pixels
     * that may be partially or fully transparent.
     *
     * @param image the image to check
     * @return the average color
     */
    public static int rgbAverage(final BufferedImage image) {
        return rgbAverage(image, false);
    }

    /**
     * Compute the average RGB value of an entire image.
     *
     * @param image the image to check
     * @param onlyOpaque if true, only count pixels that are fully opaque
     * @return the average color
     */
    public static int rgbAverage(final BufferedImage image,
        final boolean onlyOpaque) {

        assert (image != null);

        int [] rgbArray = image.getRGB(0, 0,
            image.getWidth(), image.getHeight(), null, 0, image.getWidth());

        if (rgbArray.length == 0) {
            // No image data, return black.
            return 0xFF000000;
        }

        // Compute the average color.
        long totalRed = 0;
        long totalGreen = 0;
        long totalBlue = 0;
        long count = 0;
        for (int i = 0; i < rgbArray.length; i++) {
            int argb = rgbArray[i];
            if ((onlyOpaque == true) && (((argb >>> 24) & 0xFF) != 0xFF)) {
                continue;
            }
            count++;
            int red   = (argb >>> 16) & 0xFF;
            int green = (argb >>>  8) & 0xFF;
            int blue  =  argb         & 0xFF;
            totalRed   += red;
            totalGreen += green;
            totalBlue  += blue;
        }
        totalRed   = (int) (totalRed   / count);
        totalGreen = (int) (totalGreen / count);
        totalBlue  = (int) (totalBlue  / count);

        int result = (int) ((0xFF << 24) | (totalRed   << 16)
                                         | (totalGreen <<  8)
                                         |  totalBlue);
        return result;
    }

    /**
     * Compute the standard deviation of RGB values of an entire image.
     *
     * @param image the image to check
     * @param averageImage the image's "average" pixel values
     * @return the average color
     * @throws IllegalArgumentException if the two images are of different
     * dimensions
     */
    public static double rgbStdDev(final BufferedImage image,
        final BufferedImage averageImage) {

        if (image.getWidth() != averageImage.getWidth()) {
            throw new IllegalArgumentException("images have different widths");
        }
        if (image.getHeight() != averageImage.getHeight()) {
            throw new IllegalArgumentException("images have different heights");
        }

        assert (image.getWidth() == averageImage.getWidth());
        assert (image.getHeight() == averageImage.getHeight());

        int [] imageRgbArray = image.getRGB(0, 0,
            image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        int [] averageImageRgbArray = averageImage.getRGB(0, 0,
            image.getWidth(), image.getHeight(), null, 0, image.getWidth());

        assert (imageRgbArray.length == averageImageRgbArray.length);

        double variance = 0.0;
        for (int i = 0; i < imageRgbArray.length; i++) {
            int rgb1 = imageRgbArray[i];
            int rgb2 = averageImageRgbArray[i];
            double distance = rgbDistance(rgb1, rgb2);
            variance += distance;
        }

        return (variance / (double) imageRgbArray.length);
    }

    /**
     * Create a BufferedImage using the same color model as another image.
     *
     * @param image the original image
     * @param width the width of the new image
     * @param height the height of the new image
     * @return the new image
     */
    public static BufferedImage createImage(final BufferedImage image,
        final int width, final int height) {

        if (image.getType() == BufferedImage.TYPE_INT_ARGB) {
            return new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        }

        ColorModel colorModel = image.getColorModel();
        if (colorModel instanceof IndexColorModel) {
            IndexColorModel indexModel = (IndexColorModel) colorModel;
            return new BufferedImage(width, height, image.getType(),
                indexModel);
        }

        // Fallback: ARGB
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Check if a Unicode block-drawing character is drawable by
     * drawUnicodeBlockDrawingChar().
     *
     * @param ch the character to draw
     * @return true if drawUnicodeBlockDrawingChar() can draw that character
     */
    public static boolean canDrawUnicodeBlockDrawingChar(final int ch) {
        switch (ch) {
        case ' ':
            // Space character
        case 0x2580:
            // Full upper half - 0x2580 - ▀
        case 0x258c:
            // Full left half - 0x258c - ▌
        case 0x2590:
            // Full right half - 0x2590 - ▐
        case 0x2584:
            // Full bottom half - 0x2584 - ▄
        case 0x2588:
            // Full foreground block - 0x2588 - █
            return true;
        }
        if ((ch >= 0x1FB00) && (ch <= 0x1FB3B)) {
            // Sextants
            return true;
        }

        return false;
    }

    /**
     * Draw a Unicode block-drawing character to an image.
     *
     * @param ch the character to draw
     * @param foreColor the foreground color
     * @param backColor the background color
     * @param image the image to draw onto
     */
    public static void drawUnicodeBlockDrawingChar(final int ch,
        final java.awt.Color foreColor,  final java.awt.Color backColor,
        final BufferedImage image) {

        Graphics gr = image.getGraphics();
        int width = image.getWidth();
        int height = image.getHeight();
        int halfWidth = width / 2;
        int halfHeight = height / 2;
        int thirdHeight = height / 3;

        switch (ch) {

        // Half blocks --------------------------------------------------------

        case ' ':
            // Space character
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            break;
        case 0x2580:
            // Full upper half - 0x2580 - ▀
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, halfHeight);
            gr.setColor(backColor);
            gr.fillRect(0, halfHeight, width, height - halfHeight);
            break;
        case 0x258c:
            // Full left half - 0x258c - ▌
            gr.setColor(foreColor);
            gr.fillRect(0, 0, halfWidth, height);
            gr.setColor(backColor);
            gr.fillRect(halfWidth, 0, width - halfWidth, height);
            break;
        case 0x2590:
            // Full right half - 0x2590 - ▐
            gr.setColor(backColor);
            gr.fillRect(0, 0, halfWidth, height);
            gr.setColor(foreColor);
            gr.fillRect(halfWidth, 0, width - halfWidth, height);
            break;
        case 0x2584:
            // Full bottom half - 0x2584 - ▄
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, halfHeight);
            gr.setColor(foreColor);
            gr.fillRect(0, halfHeight, width, height - halfHeight);
            break;
        case 0x2588:
            // Full foreground block - 0x2588 - █
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, height);
            break;

        // Sextants -----------------------------------------------------------

        case 0x1FB00:
            // 🬀      Block Sextant-1
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, 0, halfWidth, thirdHeight);
            break;
        case 0x1FB01:
            // 🬁      Block Sextant-2
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(halfWidth, 0, width - halfWidth, thirdHeight);
            break;
        case 0x1FB02:
            // 🬂      Block Sextant-12
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, thirdHeight);
            break;
        case 0x1FB03:
            // 🬃      Block Sextant-3
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, thirdHeight, halfWidth, thirdHeight);
            break;
        case 0x1FB04:
            // 🬄      Block Sextant-13
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, 0, halfWidth, 2 * thirdHeight);
            break;
        case 0x1FB05:
            // 🬅      Block Sextant-23
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(halfWidth, 0, width - halfWidth, thirdHeight);
            gr.fillRect(0, thirdHeight, halfWidth, thirdHeight);
            break;
        case 0x1FB06:
            // 🬆      Block Sextant-123
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, thirdHeight);
            gr.fillRect(0, thirdHeight, halfWidth, thirdHeight);
            break;
        case 0x1FB07:
            // 🬇      Block Sextant-4
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(halfWidth, thirdHeight, width - halfWidth, thirdHeight);
            break;
        case 0x1FB08:
            // 🬈      Block Sextant-14
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, 0, halfWidth, thirdHeight);
            gr.fillRect(halfWidth, thirdHeight, width - halfWidth, thirdHeight);
            break;
        case 0x1FB09:
            // 🬉      Block Sextant-24
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(halfWidth, 0, width - halfWidth, 2 * thirdHeight);
            break;
        case 0x1FB0A:
            // 🬊      Block Sextant-124
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, thirdHeight);
            gr.fillRect(halfWidth, thirdHeight, width - halfWidth, thirdHeight);
            break;
        case 0x1FB0B:
            // 🬋      Block Sextant-34
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, thirdHeight, width, thirdHeight);
            break;
        case 0x1FB0C:
            // 🬌      Block Sextant-134
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, 0, halfWidth, thirdHeight);
            gr.fillRect(0, thirdHeight, width, thirdHeight);
            break;
        case 0x1FB0D:
            // 🬍      Block Sextant-234
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(halfWidth, 0, width - halfWidth, thirdHeight);
            gr.fillRect(0, thirdHeight, width, thirdHeight);
            break;
        case 0x1FB0E:
            // 🬎      Block Sextant-1234
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, 2 * thirdHeight);
            gr.setColor(backColor);
            gr.fillRect(0, 2 * thirdHeight, width, height - (2 * thirdHeight));
            break;
        case 0x1FB0F:
            // 🬏      Block Sextant-5
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, 2 * thirdHeight, halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB10:
            // 🬐      Block Sextant-15
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, 0, halfWidth, thirdHeight);
            gr.fillRect(0, 2 * thirdHeight, halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB11:
            // 🬑      Block Sextant-25
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(halfWidth, 0, width - halfWidth, thirdHeight);
            gr.fillRect(0, 2 * thirdHeight, halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB12:
            // 🬒      Block Sextant-125
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, thirdHeight);
            gr.fillRect(0, 2 * thirdHeight, halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB13:
            // 🬓      Block Sextant-35
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, thirdHeight, halfWidth, height - thirdHeight);
            break;
        case 0x1FB14:
            // 🬔      Block Sextant-235
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(halfWidth, 0, width - halfWidth, thirdHeight);
            gr.fillRect(0, thirdHeight, halfWidth, height - thirdHeight);
            break;
        case 0x1FB15:
            // 🬕      Block Sextant-1235
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(backColor);
            gr.fillRect(halfWidth, thirdHeight, width - halfWidth,
                height - thirdHeight);
            break;
        case 0x1FB16:
            // 🬖      Block Sextant-45
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(halfWidth, thirdHeight, width - halfWidth, thirdHeight);
            gr.fillRect(0, 2 * thirdHeight, halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB17:
            // 🬗      Block Sextant-145
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, 0, halfWidth, thirdHeight);
            gr.fillRect(halfWidth, thirdHeight, width - halfWidth, thirdHeight);
            gr.fillRect(0, 2 * thirdHeight, halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB18:
            // 🬘      Block Sextant-245
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(halfWidth, 0, width - halfWidth, 2 * thirdHeight);
            gr.fillRect(0, 2 * thirdHeight, halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB19:
            // 🬙      Block Sextant-1245
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(backColor);
            gr.fillRect(0, thirdHeight, halfWidth, thirdHeight);
            gr.fillRect(halfWidth, 2 * thirdHeight, width - halfWidth,
                height - 2 * thirdHeight);
            break;
        case 0x1FB1A:
            // 🬚      Block Sextant-345
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, thirdHeight, width, thirdHeight);
            gr.fillRect(0, 2 * thirdHeight, halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB1B:
            // 🬛      Block Sextant-1345
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(backColor);
            gr.fillRect(halfWidth, 0, width - halfWidth, thirdHeight);
            gr.fillRect(halfWidth, 2 * thirdHeight, width - halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB1C:
            // 🬜      Block Sextant-2345
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(backColor);
            gr.fillRect(0, 0, halfWidth, thirdHeight);
            gr.fillRect(halfWidth, 2 * thirdHeight, width - halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB1D:
            // 🬝      Block Sextant-12345
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(backColor);
            gr.fillRect(halfWidth, 2 * thirdHeight, width - halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB1E:
            // 🬞      Block Sextant-6
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(halfWidth, 2 * thirdHeight, width - halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB1F:
            // 🬟      Block Sextant-16
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, 0, halfWidth, thirdHeight);
            gr.fillRect(halfWidth, 2 * thirdHeight, width - halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB20:
            // 🬠      Block Sextant-26
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(halfWidth, 0, width - halfWidth, thirdHeight);
            gr.fillRect(halfWidth, 2 * thirdHeight, width - halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB21:
            // 🬡      Block Sextant-126
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, thirdHeight);
            gr.fillRect(halfWidth, 2 * thirdHeight, width - halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB22:
            // 🬢      Block Sextant-36
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, thirdHeight, halfWidth, thirdHeight);
            gr.fillRect(halfWidth, 2 * thirdHeight, width - halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB23:
            // 🬣      Block Sextant-136
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, 0, halfWidth, 2 * thirdHeight);
            gr.fillRect(halfWidth, 2 * thirdHeight, width - halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB24:
            // 🬤      Block Sextant-236
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(halfWidth, 0, width - halfWidth, thirdHeight);
            gr.fillRect(0, thirdHeight, halfWidth, thirdHeight);
            gr.fillRect(halfWidth, 2 * thirdHeight, width - halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB25:
            // 🬥      Block Sextant-1236
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(backColor);
            gr.fillRect(halfWidth, thirdHeight, width - halfWidth, thirdHeight);
            gr.fillRect(0, 2 * thirdHeight, halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB26:
            // 🬦      Block Sextant-46
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(halfWidth, thirdHeight, width - halfWidth,
                height -thirdHeight);
            break;
        case 0x1FB27:
            // 🬧      Block Sextant-146
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, 0, halfWidth, thirdHeight);
            gr.fillRect(halfWidth, thirdHeight, width - halfWidth,
                height -thirdHeight);
            break;
        case 0x1FB28:
            // 🬨      Block Sextant-1246
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(backColor);
            gr.fillRect(0, thirdHeight, halfWidth, thirdHeight);
            gr.fillRect(0, 2 * thirdHeight, halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB29:
            // 🬩      Block Sextant-346
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, thirdHeight, width, thirdHeight);
            gr.fillRect(halfWidth, 2 * thirdHeight, width - halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB2A:
            // 🬪      Block Sextant-1346
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(backColor);
            gr.fillRect(halfWidth, 0, width - halfWidth, thirdHeight);
            gr.fillRect(0, 2 * thirdHeight, halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB2B:
            // 🬫      Block Sextant-2346
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(backColor);
            gr.fillRect(0, 0, halfWidth, thirdHeight);
            gr.fillRect(0, 2 * thirdHeight, halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB2C:
            // 🬬      Block Sextant-12346
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(backColor);
            gr.fillRect(0, 2 * thirdHeight, halfWidth,
                height - (2 * thirdHeight));
            break;
        case 0x1FB2D:
            // 🬭      Block Sextant-56
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, 2 * thirdHeight, width,
                height - (2 * thirdHeight));
            break;
        case 0x1FB2E:
            // 🬮      Block Sextant-156
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, 0, halfWidth, thirdHeight);
            gr.fillRect(0, 2 * thirdHeight, width,
                height - (2 * thirdHeight));
            break;
        case 0x1FB2F:
            // 🬯      Block Sextant-256
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(halfWidth, 0, width - halfWidth, thirdHeight);
            gr.fillRect(0, 2 * thirdHeight, width,
                height - (2 * thirdHeight));
            break;
        case 0x1FB30:
            // 🬰      Block Sextant-1256
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, thirdHeight);
            gr.fillRect(0, 2 * thirdHeight, width,
                height - (2 * thirdHeight));
            break;
        case 0x1FB31:
            // 🬱      Block Sextant-356
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(foreColor);
            gr.fillRect(0, thirdHeight, halfWidth, thirdHeight);
            gr.fillRect(0, 2 * thirdHeight, width,
                height - (2 * thirdHeight));
            break;
        case 0x1FB32:
            // 🬲      Block Sextant-1356
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(backColor);
            gr.fillRect(halfWidth, 0, width - halfWidth, 2 * thirdHeight);
            break;
        case 0x1FB33:
            // 🬳      Block Sextant-2356
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(backColor);
            gr.fillRect(0, 0, halfWidth, thirdHeight);
            gr.fillRect(halfWidth, thirdHeight, width - halfWidth, thirdHeight);
            break;
        case 0x1FB34:
            // 🬴      Block Sextant-12356
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(backColor);
            gr.fillRect(halfWidth, thirdHeight, width - halfWidth, thirdHeight);
            break;
        case 0x1FB35:
            // 🬵      Block Sextant-456
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, thirdHeight);
            gr.fillRect(0, thirdHeight, halfWidth, thirdHeight);
            break;
        case 0x1FB36:
            // 🬶      Block Sextant-1456
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(backColor);
            gr.fillRect(halfWidth, 0, width - halfWidth, thirdHeight);
            gr.fillRect(0, thirdHeight, halfWidth, thirdHeight);
            break;
        case 0x1FB37:
            // 🬷      Block Sextant-2456
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(backColor);
            gr.fillRect(0, 0, halfWidth, 2 * thirdHeight);
            break;
        case 0x1FB38:
            // 🬸      Block Sextant-12456
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(backColor);
            gr.fillRect(0, thirdHeight, halfWidth, thirdHeight);
            break;
        case 0x1FB39:
            // 🬹      Block Sextant-3456
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(backColor);
            gr.fillRect(0, 0, width, thirdHeight);
            break;
        case 0x1FB3A:
            // 🬺      Block Sextant-13456
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(backColor);
            gr.fillRect(halfWidth, 0, width - halfWidth, thirdHeight);
            break;
        case 0x1FB3B:
            // 🬻      Block Sextant-23456
            gr.setColor(foreColor);
            gr.fillRect(0, 0, width, height);
            gr.setColor(backColor);
            gr.fillRect(0, 0, halfWidth, thirdHeight);
            break;

        }

        gr.dispose();

    }


}
