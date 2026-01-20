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
package casciian.terminal;

import casciian.bits.ImageRGB;
import java.util.HashMap;
import java.util.Map;

/**
 * SixelDecoder parses a buffer of sixel image data into a ImageRGB.
 */
public class SixelDecoder {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Parser character scan states.
     */
    private enum ScanState {
        INIT,
        GROUND,
        RASTER,
        COLOR,
        REPEAT,
    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * If true, enable debug messages.
     */
    private static boolean DEBUG = false;

    /**
     * Number of pixels to increment when we need more horizontal room.
     */
    private static int WIDTH_INCREASE = 400;

    /**
     * Number of pixels to increment when we need more vertical room.
     */
    private static int HEIGHT_INCREASE = 400;

    /**
     * Maximum width in pixels.  Xterm's max is 1000, but that's pretty
     * limited for today's systems, so we will support up to "4K Ultra HD"
     * width and three times that height.
     */
    private static int MAX_WIDTH = 1 * 3840;

    /**
     * Maximum height in pixels.  Xterm's max is 1000, but that's pretty
     * limited for today's systems, so we will support up to "4K Ultra HD".
     */
    private static int MAX_HEIGHT = 3 * 2160;

    /**
     * Current scanning state.
     */
    private ScanState scanState = ScanState.INIT;

    /**
     * Parameters being collected.
     */
    private int [] params = new int[5];

    /**
     * Current parameter being collected.
     */
    private int paramsI = 0;

    /**
     * The sixel palette colors specified.
     */
    private Map<Integer, Integer> palette;

    /**
     * The buffer to parse.
     */
    private String buffer;

    /**
     * The image being drawn to.
     */
    private ImageRGB image;

    /**
     * The real width of image.
     */
    private int width = 0;

    /**
     * The real height of image.
     */
    private int height = 0;

    /**
     * The width of image provided in the raster attribute.
     */
    private int rasterWidth = 0;

    /**
     * The height of image provided in the raster attribute.
     */
    private int rasterHeight = 0;

    /**
     * The repeat count.
     */
    private int repeatCount = -1;

    /**
     * The current drawing x position.
     */
    private int x = 0;

    /**
     * The maximum y drawn to.  This will set the final image height.
     */
    private int y = 0;

    /**
     * The current drawing color.
     */
    private int color = 0x000000;

    /**
     * The background color.
     */
    private int background = 0x000000;

    /**
     * If set, abort processing this image.
     */
    private boolean abort = false;

    /**
     * If true, transparency will be honored.
     */
    private boolean maybeTransparent = false;

    /**
     * If set, color index 0 will be set to transparent.
     */
    private boolean transparent = false;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param buffer the sixel data to parse
     * @param palette palette to use, or null for a private palette
     * @param background the background color to use
     * @param maybeTransparent if true, transparency in the image will be
     * honored
     */
    public SixelDecoder(final String buffer,
                        final Map<Integer, Integer> palette, final int background,
                        final boolean maybeTransparent) {

        this.buffer = buffer;
        if (palette == null) {
            this.palette = new HashMap<>();
            initializePaletteVT340(this.palette);
        } else {
            this.palette = palette;
        }
        this.background = background;
        this.maybeTransparent = maybeTransparent;
    }

    // ------------------------------------------------------------------------
    // SixelDecoder -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Initialize a palette with VT340 colors.
     *
     * @param palette palette to initialize
     */
    public static void initializePaletteVT340(final Map<Integer, Integer> palette) {
        palette.clear();

        /*
         * Populate bottom 16 colors with VT340 colors. See:
         * https://github.com/hackerb9/vt340test/blob/main/colormap/showcolortable.png
         */
        palette.put(0,  0x000000);
        palette.put(1,  0x3333cc);
        palette.put(2,  0xcc2323);
        palette.put(3,  0x33cc33);
        palette.put(4,  0xcc33cc);
        palette.put(5,  0x33cccc);
        palette.put(6,  0xcccc33);
        palette.put(7,  0x777777);
        palette.put(8,  0x444444);
        palette.put(9,  0x565699);
        palette.put(10, 0x994444);
        palette.put(11, 0x569956);
        palette.put(12, 0x995699);
        palette.put(13, 0x569999);
        palette.put(14, 0x999956);
        palette.put(15, 0xcccccc);
    }

    /**
     * Initialize a palette with CGA colors.
     *
     * @param palette palette to initialize
     */
    public static void initializePaletteCGA(final Map<Integer, Integer> palette) {
        palette.clear();

        /*
         * Populate bottom 16 colors with text-mode 16-color CGA.
         */
        palette.put(0,  0x000000);
        palette.put(1,  0xa80000);
        palette.put(2,  0x00a800);
        palette.put(3,  0xa85400);
        palette.put(4,  0x0000a8);
        palette.put(5,  0xa800a8);
        palette.put(6,  0x00a8a8);
        palette.put(7,  0xa8a8a8);
        palette.put(8,  0x545454);
        palette.put(9,  0xfc5454);
        palette.put(10, 0x54fc54);
        palette.put(11, 0xfcfc54);
        palette.put(12, 0x5454fc);
        palette.put(13, 0xfc54fc);
        palette.put(14, 0x54fcfc);
        palette.put(15, 0xfcfcfc);
    }

    /**
     * If true, this image might have transparent pixels.
     *
     * @return whether this image might have transparent pixels
     */
    public boolean isTransparent() {
        return transparent;
    }

    /**
     * Get the image.
     *
     * @return the sixel data as an image.
     */
    public ImageRGB getImage() {
        if (buffer != null) {
            int bufferLength = buffer.length();
            for (int i = 0; (i < bufferLength) && (abort == false); i++) {
                consume(buffer.charAt(i));
            }
            buffer = null;
        }
        if (abort == true) {
            return null;
        }

        if ((width > 0) && (height > 0) && (image != null)) {
            /*
            System.err.println(String.format("getImage() %d %d %d %d %d %d",
                    width, height, x, y, rasterWidth, rasterHeight));
            */

            if ((rasterWidth > width) || (rasterHeight > y + 1)) {
                resizeImage(Math.max(width, rasterWidth),
                    Math.max(y + 1, rasterHeight));
                return image.getSubimage(0, 0, Math.max(width, rasterWidth),
                    Math.max(y + 1, rasterHeight));
            }
            return image.getSubimage(0, 0, width, y + 1);
        }
        return null;
    }

    /**
     * Resize image to a new size.
     *
     * @param newWidth new width of image
     * @param newHeight new height of image
     */
    private void resizeImage(final int newWidth, final int newHeight) {

        ImageRGB newImage;

        if (DEBUG) {
            System.err.println("resizeImage(); old " +
                (image != null ? image.getWidth() : "null ") + "x " +
                (image != null ? image.getHeight() : "null ") + "y " +
                "new " + newWidth + "x " + newHeight + "y " +
                "transparency: " + transparent);
        }

        if (image != null) {
            newImage = image.getSubimage(0, 0, newWidth, newHeight);
        } else {
            newImage = new ImageRGB(newWidth, newHeight);
        }
        image = newImage;
    }

    /**
     * Clear the parameters and flags.
     */
    private void toGround() {
        paramsI = 0;
        for (int i = 0; i < params.length; i++) {
            params[i] = 0;
        }
        scanState = ScanState.GROUND;
        repeatCount = -1;
    }

    /**
     * Get a color parameter value, with a default.
     *
     * @param position parameter index.  0 is the first parameter.
     * @param defaultValue value to use if colorParams[position] doesn't exist
     * @return parameter value
     */
    private int getParam(final int position, final int defaultValue) {
        if (position > paramsI) {
            return defaultValue;
        }
        return params[position];
    }

    /**
     * Get a color parameter value, clamped to within min/max.
     *
     * @param position parameter index.  0 is the first parameter.
     * @param defaultValue value to use if colorParams[position] doesn't exist
     * @param minValue minimum value inclusive
     * @param maxValue maximum value inclusive
     * @return parameter value
     */
    private int getParam(final int position, final int defaultValue,
        final int minValue, final int maxValue) {

        assert (minValue <= maxValue);
        int value = getParam(position, defaultValue);
        if (value < minValue) {
            value = minValue;
        }
        if (value > maxValue) {
            value = maxValue;
        }
        return value;
    }

    /**
     * Add sixel data to the image.
     *
     * @param ch the character of sixel data
     */
    private void addSixel(final char ch) {
        int n = ((int) ch - 63);

        int rgb = color;
        // As per jerch who has read STD 070 much more than I have, the
        // repeat counter may not exceed 2^15 - 1; and a value of 0 means 1
        // pixel wide.  CVE-2022-24130 shows how to exceed memory / crash if
        // this value is not checked.
        int rep = Math.min(Math.max(1, (repeatCount == -1 ? 1 : repeatCount)),
            32767);
        // Also clamp to the maximum allowed image width, like foot terminal
        // does.
        rep = Math.min(rep, MAX_WIDTH);

        if (DEBUG) {
            System.err.println("addSixel() rep " + rep + " char " +
                Integer.toHexString(n) + " color " + color);
        }

        assert (n >= 0);

        if (image == null) {
            // The raster attributes was not provided.
            resizeImage(WIDTH_INCREASE, HEIGHT_INCREASE);
        }

        if (x + rep > image.getWidth()) {
            // Resize the image, give us another max(rep, WIDTH_INCREASE)
            // pixels of horizontal length.
            resizeImage(image.getWidth() + Math.max(rep, WIDTH_INCREASE),
                image.getHeight());
        }

        // If nothing will be drawn, just advance x.
        if (n == 0) {
            x += rep;
            if (x > width) {
                width = x;
            }
            if (width > MAX_WIDTH) {
                abort = true;
            }
            return;
        }

        int dy = 0;
        for (int i = 0; i < rep; i++) {
            if ((n & 0x01) != 0) {
                dy = 0;
                image.setRGB(x, height + dy, rgb);
            }
            if ((n & 0x02) != 0) {
                dy = 1;
                image.setRGB(x, height + dy, rgb);
            }
            if ((n & 0x04) != 0) {
                dy = 2;
                image.setRGB(x, height + dy, rgb);
            }
            if ((n & 0x08) != 0) {
                dy = 3;
                image.setRGB(x, height + dy, rgb);
            }
            if ((n & 0x10) != 0) {
                dy = 4;
                image.setRGB(x, height + dy, rgb);
            }
            if ((n & 0x20) != 0) {
                dy = 5;
                image.setRGB(x, height + dy, rgb);
            }
            if (height + dy > y) {
                y = height + dy;
            }
            x++;
        }
        if (x > width) {
            width = x;
        }
        if (width > MAX_WIDTH) {
            abort = true;
        }
        if (y + 1 > MAX_HEIGHT) {
            abort = true;
        }
    }

    /**
     * Process a color palette change.
     */
    private void setPalette() {
        int idx = getParam(0, 0);

        if (paramsI == 0) {
            Integer newColor = palette.get(idx);
            if (newColor != null) {
                color = newColor;
            } else {
                if (DEBUG) {
                    System.err.println("COLOR " + idx + " NOT FOUND");
                }
                color = 0x000000;
            }

            if (DEBUG) {
                System.err.println("set color " + idx + " " + color);
            }
            return;
        }

        int type = getParam(1, 0);
        float red   = (float) (getParam(2, 0, 0, 100) / 100.0);
        float green = (float) (getParam(3, 0, 0, 100) / 100.0);
        float blue  = (float) (getParam(4, 0, 0, 100) / 100.0);

        if (type == 2) {
            int newColor = ((int) (red * 255.0) << 16)
                | ((int) (green * 255.0) << 8)
                | ((int) (blue * 255.0));
            palette.put(idx, newColor);
            if (DEBUG) {
                System.err.println("Palette color " + idx + " --> " + newColor);
            }
        } else {
            if (DEBUG) {
                System.err.println("UNKNOWN COLOR TYPE " + type + ": " + type +
                    " " + idx + " R " + red + " G " + green + " B " + blue);
            }
        }
    }

    /**
     * Parse the initializer.
     */
    private void parseInit() {
        int p1 = getParam(0, 0);        // Pixel aspect ratio (ignored)
        int p2 = getParam(1, 0);        // Background color option
        int p3 = getParam(2, 0);        // Horizontal grid size (ignored)

        if (DEBUG) {
            System.err.println("parseInit() " + p1 + " " + p2 + " " + p3);
        }

        switch (p2) {
        case 1:
            /*
             * Pixels that are not specified with a color will be
             * transparent, IF transparency was enabled.
             */
            if (maybeTransparent) {
                transparent = true;
            } else {
                transparent = false;
            }
            break;
        default:
            // Pixels that are not specified with a color will be the current
            // background color.
            transparent = false;
            break;
        }
    }

    /**
     * Parse the raster attributes.
     */
    private void parseRaster() {
        int pan = getParam(0, 0);  // Aspect ratio numerator
        int pad = getParam(1, 0);  // Aspect ratio denominator
        int pah = getParam(2, 0);  // Horizontal width
        int pav = getParam(3, 0);  // Vertical height

        if (DEBUG) {
            System.err.println("parseRaster() " + pan + " " + pad + " " +
                pah + " " + pav);
        }

        if ((pan == pad) && (pah > 0) && (pav > 0)) {
            rasterWidth = pah;
            rasterHeight = pav;
            if ((rasterWidth <= MAX_WIDTH) && (rasterHeight <= MAX_HEIGHT)) {
                resizeImage(rasterWidth, rasterHeight);
            } else {
                abort = true;
            }
        } else {
            abort = true;
        }
    }

    /**
     * Run this input character through the sixel state machine.
     *
     * @param ch character from the remote side
     */
    private void consume(char ch) {

        // DEBUG
        /*
        System.err.printf("SixelDecoder.consume() %c STATE = %s\n", ch,
            scanState);
         */

        if ((ch == 'q') && (scanState == ScanState.INIT)) {
            // This is the normal happy path with the introducer string.
            parseInit();
            toGround();
            return;
        }

        // Between decimal 63 (inclusive) and 127 (exclusive) --> pixels
        if ((ch >= 63) && (ch < 127)) {
            if (scanState == ScanState.COLOR) {
                setPalette();
            }
            if (scanState == ScanState.INIT) {
                parseInit();
                toGround();
            }
            if (scanState == ScanState.RASTER) {
                parseRaster();
                toGround();
            }
            addSixel(ch);
            toGround();
            return;
        }

        if (ch == '#') {
            // Next color is here, parse what we had before.
            if (scanState == ScanState.COLOR) {
                setPalette();
                toGround();
            }
            if (scanState == ScanState.INIT) {
                parseInit();
                toGround();
            }
            if (scanState == ScanState.RASTER) {
                parseRaster();
                toGround();
            }
            scanState = ScanState.COLOR;
            return;
        }

        if (ch == '!') {
            // Repeat count
            if (scanState == ScanState.COLOR) {
                setPalette();
                toGround();
            }
            if (scanState == ScanState.INIT) {
                parseInit();
                toGround();
            }
            if (scanState == ScanState.RASTER) {
                parseRaster();
                toGround();
            }
            scanState = ScanState.REPEAT;
            repeatCount = 0;
            return;
        }

        if (ch == '-') {
            if (scanState == ScanState.COLOR) {
                setPalette();
                toGround();
            }
            if (scanState == ScanState.INIT) {
                parseInit();
                toGround();
            }
            if (scanState == ScanState.RASTER) {
                parseRaster();
                toGround();
            }

            height += 6;
            x = 0;

            if (height + 6 > image.getHeight()) {
                // Resize the image, give us another HEIGHT_INCREASE
                // pixels of vertical length.
                resizeImage(image.getWidth(),
                    image.getHeight() + HEIGHT_INCREASE);
            }
            return;
        }

        if (ch == '$') {
            if (scanState == ScanState.COLOR) {
                setPalette();
                toGround();
            }
            if (scanState == ScanState.INIT) {
                parseInit();
                toGround();
            }
            if (scanState == ScanState.RASTER) {
                parseRaster();
                toGround();
            }
            x = 0;
            return;
        }

        if (ch == '"') {
            if (scanState == ScanState.COLOR) {
                setPalette();
                toGround();
            }
            if (scanState == ScanState.INIT) {
                parseInit();
                toGround();
            }
            scanState = ScanState.RASTER;
            return;
        }

        switch (scanState) {

        case GROUND:
            // Unknown character.
            if (DEBUG) {
                System.err.println("UNKNOWN CHAR: " + ch);
            }
            return;

        case INIT:
            // 30-39, 3B --> param
            if ((ch >= '0') && (ch <= '9')) {
                params[paramsI] *= 10;
                params[paramsI] += (ch - '0');
            }
            if (ch == ';') {
                if (paramsI < params.length - 1) {
                    paramsI++;
                }
            }
            return;

        case RASTER:
            // 30-39, 3B --> param
            if ((ch >= '0') && (ch <= '9')) {
                params[paramsI] *= 10;
                params[paramsI] += (ch - '0');
            }
            if (ch == ';') {
                if (paramsI < params.length - 1) {
                    paramsI++;
                }
            }
            return;

        case COLOR:
            // 30-39, 3B --> param
            if ((ch >= '0') && (ch <= '9')) {
                params[paramsI] *= 10;
                params[paramsI] += (ch - '0');
            }
            if (ch == ';') {
                if (paramsI < params.length - 1) {
                    paramsI++;
                }
            }
            return;

        case REPEAT:
            if ((ch >= '0') && (ch <= '9')) {
                if (repeatCount == -1) {
                    repeatCount = (ch - '0');
                } else {
                    repeatCount *= 10;
                    repeatCount += (ch - '0');
                }
            }
            return;

        }

    }

}
