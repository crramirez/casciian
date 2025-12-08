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

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;

import casciian.backend.Backend;
import casciian.bits.Cell;
import casciian.bits.ComplexCell;
import casciian.bits.ExtendedGraphemeClusterUtils;

/**
 * GlyphMakerFont creates glyphs as bitmaps from a font.
 */
class GlyphMakerFont {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * If true, enable debug messages.
     */
    private static boolean DEBUG = false;

    /**
     * If true, we were successful at getting the font dimensions.
     */
    private boolean gotFontDimensions = false;

    /**
     * The currently selected font.
     */
    private Font font = null;

    /**
     * Width of a character cell in pixels.
     */
    private int textWidth = 1;

    /**
     * Height of a character cell in pixels.
     */
    private int textHeight = 1;

    /**
     * Width of a character cell in pixels, as reported by font.
     */
    private int fontTextWidth = 1;

    /**
     * Height of a character cell in pixels, as reported by font.
     */
    private int fontTextHeight = 1;

    /**
     * Descent of a character cell in pixels.
     */
    private int maxDescent = 0;

    /**
     * System-dependent Y adjustment for text in the character cell.
     */
    private int textAdjustY = 0;

    /**
     * System-dependent X adjustment for text in the character cell.
     */
    private int textAdjustX = 0;

    /**
     * System-dependent height adjustment for text in the character cell.
     */
    private int textAdjustHeight = 0;

    /**
     * System-dependent width adjustment for text in the character cell.
     */
    private int textAdjustWidth = 0;

    /**
     * A cache of previously-rendered glyphs for blinking text, when it is
     * not visible.
     */
    private HashMap<Cell, BufferedImage> glyphCacheBlink;

    /**
     * A cache of previously-rendered glyphs for non-blinking, or
     * blinking-and-visible, text.
     */
    private HashMap<Cell, BufferedImage> glyphCache;

    /**
     * If true, this font loaded OK.
     */
    private boolean loaded = false;

    /**
     * The percent of "dimming" to do when blinking text is invisible.  0
     * means no blinking; 100 means the foreground color matches the
     * background color.
     */
    private int blinkDimPercent = 100;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param filename the resource filename of the font to use
     * @param fontSize the size of font to use
     */
    public GlyphMakerFont(final String filename, final int fontSize) {

        int dimPercent = 80;
        try {
            String dimPercentStr = System.getProperty("casciian.blinkDimPercent",
                "80");
            dimPercent = Integer.parseInt(dimPercentStr);
        } catch (NumberFormatException e) {
            // SQUASH
        }
        if ((dimPercent >= 0) && (dimPercent <= 100)) {
            blinkDimPercent = dimPercent;
        }

        if (filename.length() == 0) {
            // Fallback font
            font = new Font(Font.MONOSPACED, Font.PLAIN, fontSize);
            return;
        }

        Font fontRoot = null;
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream in = loader.getResourceAsStream(filename);
            fontRoot = Font.createFont(Font.TRUETYPE_FONT, in);
            font = fontRoot.deriveFont(Font.PLAIN, fontSize);
            loaded = true;
        } catch (FontFormatException e) {
            // Ideally we would report an error here, either via System.err
            // or TExceptionDialog.  However, I do not want GlyphMaker to
            // know about available backends, so we quietly fallback to
            // whatever is available as MONO.
            font = new Font(Font.MONOSPACED, Font.PLAIN, fontSize);
        } catch (IOException e) {
            // See comment above.
            font = new Font(Font.MONOSPACED, Font.PLAIN, fontSize);
        }
    }

    // ------------------------------------------------------------------------
    // GlyphMakerFont ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get a glyph image.
     *
     * @param cell the character to draw
     * @param cellWidth the width of the text cell to draw into
     * @param cellHeight the height of the text cell to draw into
     * @param backend the backend that can obtain the correct background
     * color
     * @return the glyph as an image
     */
    public BufferedImage getImage(final Cell cell, final int cellWidth,
        final int cellHeight, final Backend backend) {

        return getImage(cell, cellWidth, cellHeight, backend, true,
            Cell.Width.SINGLE);
    }

    /**
     * Get a glyph image.
     *
     * @param cell the character to draw
     * @param cellWidth the width of the text cell to draw into
     * @param cellHeight the height of the text cell to draw into
     * @param backend the backend that can obtain the correct background
     * color
     * @param blinkVisible if true, the cell is visible if it is blinking
     * @return the glyph as an image
     */
    public BufferedImage getImage(final Cell cell, final int cellWidth,
        final int cellHeight, final Backend backend,
        final boolean blinkVisible) {

        return getImage(cell, cellWidth, cellHeight, backend, blinkVisible,
            Cell.Width.SINGLE);
    }

    /**
     * Get a glyph image.
     *
     * @param cell the character to draw
     * @param cellWidth the width of the text cell to draw into
     * @param cellHeight the height of the text cell to draw into
     * @param backend the backend that can obtain the correct background
     * color
     * @param blinkVisible if true, the cell is visible if it is blinking
     * @param widthEnum If SINGLE, return the entire image. If LEFT or RIGHT,
     * return only half of the image (either left side or right side).
     * @return the glyph as an image
     */
    public BufferedImage getImage(final Cell cell, final int cellWidth,
        final int cellHeight, final Backend backend,
        final boolean blinkVisible, final Cell.Width widthEnum) {

        if (gotFontDimensions == false) {
            // Lazy-load the text width/height and adjustments.
            getFontDimensions();
        }

        if (DEBUG && !font.canDisplay(cell.getChar())) {
            System.err.println("font " + font + " has no glyph for " +
                String.format("0x%x", cell.getChar()));
        }

        BufferedImage image = null;
        if (cell.isBlink() && !blinkVisible) {
            image = glyphCacheBlink.get(cell);
        } else {
            image = glyphCache.get(cell);
        }
        if ((image != null)
            && (image.getWidth() == cellWidth)
            && (image.getHeight() == cellHeight)
        ) {
            return image;
        }

        // Generate glyph and draw it.
        image = new BufferedImage(cellWidth, cellHeight,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr2 = image.createGraphics();
        gr2.setFont(font);

        Cell cellColor = new Cell(cell);
        if (cell.isPulse()) {
            cellColor.setPulse(false, false, 0);
            cellColor.setForeColorRGB(cell.getForeColorPulseRGB(backend,
                    System.currentTimeMillis()));
        }

        // Check for reverse
        if (cell.isReverse()) {
            if (cell.getBackColorRGB() < 0) {
                cellColor.setForeColor(cell.getBackColor());
            } else {
                cellColor.setForeColorRGB(cell.getBackColorRGB());
            }
            if (cell.getForeColorRGB() < 0) {
                cellColor.setBackColor(cell.getForeColor());
            } else {
                cellColor.setBackColorRGB(cell.getForeColorRGB());
            }
        }

        // Draw the background rectangle, then the foreground character.
        gr2.setColor(backend.attrToBackgroundColor(cellColor));
        gr2.fillRect(0, 0, cellWidth, cellHeight);

        // Handle blink and underline
        boolean showGlyph = true;
        if (cell.isBlink() && !blinkVisible) {
            if (blinkDimPercent <= 0) {
                showGlyph = true;
            } else if (blinkDimPercent >= 100) {
                showGlyph = false;
            } else {
                cellColor.setDimmedForeColor(backend, blinkDimPercent);
            }
        }

        if (showGlyph) {
            if (!cell.isCodePoint(' ')) {
                gr2.setColor(backend.attrToForegroundColor(cellColor));
                char [] chars = null;
                if (cell instanceof ComplexCell) {
                    chars = ((ComplexCell) cell).toCharArray();
                } else {
                    chars = Character.toChars(cell.getChar());
                }
                gr2.drawChars(chars, 0, chars.length, textAdjustX,
                    cellHeight - maxDescent + textAdjustY);
            }
            if (cell.isUnderline()) {
                gr2.fillRect(0, cellHeight - 2, cellWidth, 2);
            }
        }
        gr2.dispose();

        if (cell.isCacheable()) {
            // We need a new key that will not be mutated by invertCell().
            ComplexCell key = new ComplexCell(cell);
            if (cell.isBlink() && !blinkVisible) {
                glyphCacheBlink.put(key, image);
            } else {
                glyphCache.put(key, image);
            }
        }

        /*
        System.err.println("cellWidth " + cellWidth +
            " cellHeight " + cellHeight + " image " + image);
         */

        int centerX = image.getWidth() / 2;
        switch (widthEnum) {
        case LEFT:
            return image.getSubimage(0, 0, centerX, image.getHeight());
        case RIGHT:
            return image.getSubimage(centerX, 0,
                image.getWidth() - centerX, image.getHeight());
        case SINGLE:
            // Fall through...
        default:
            return image;
        }
    }

    /**
     * Figure out my font dimensions.
     */
    private void getFontDimensions() {
        glyphCacheBlink = new HashMap<Cell, BufferedImage>();
        glyphCache = new HashMap<Cell, BufferedImage>();

        BufferedImage image = new BufferedImage(font.getSize() * 2,
            font.getSize() * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = image.createGraphics();
        gr.setFont(font);
        FontMetrics fm = gr.getFontMetrics();
        maxDescent = fm.getMaxDescent();
        Rectangle2D bounds = fm.getMaxCharBounds(gr);
        int leading = fm.getLeading();
        fontTextWidth = (int)Math.round(bounds.getWidth());
        // fontTextHeight = (int)Math.round(bounds.getHeight()) - maxDescent;

        // This produces the same number, but works better for ugly
        // monospace.
        fontTextHeight = fm.getMaxAscent() + maxDescent - leading;
        gr.dispose();

        textHeight = fontTextHeight + textAdjustHeight;
        textWidth = fontTextWidth + textAdjustWidth;
        /*
        System.err.println("font " + font);
        System.err.println("fontTextWidth " + fontTextWidth);
        System.err.println("fontTextHeight " + fontTextHeight);
        System.err.println("textWidth " + textWidth);
        System.err.println("textHeight " + textHeight);
         */

        gotFontDimensions = true;
    }

    /**
     * Checks if this maker's Font has a glyph for the specified character.
     *
     * @param codePoint the character (Unicode code point) for which a glyph
     * is needed.
     * @return true if this Font has a glyph for the character; false
     * otherwise.
     */
    public boolean canDisplay(final int codePoint) {
        return font.canDisplay(codePoint);
    }

    /**
     * See if this font loaded OK.
     *
     * @return true if this font loaded OK, otherwise it is rendering using
     * MONO
     */
    public boolean isLoaded() {
        return loaded;
    }
}

/**
 * GlyphMaker presents unified interface to all of its supported fonts to
 * clients.
 */
public class GlyphMaker {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The mono font resource filename (terminus).
     */
    private static final String MONO = "terminus-ttf-4.49.1/TerminusTTF-Bold-4.49.1.ttf";

    /**
     * The CJK font resource filename.
     */
    private static final String cjkFontFilename = "NotoSansMonoCJKtc-Regular.otf";

    /**
     * The emoji font resource filename.
     */
    private static final String emojiFontFilename = "OpenSansEmoji.ttf";

    /**
     * The fallback font resource filename.
     */
    private static final String fallbackFontFilename = "";

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * If true, enable debug messages.
     */
    private static boolean DEBUG = false;

    /**
     * Cache of font bundles by size.
     */
    private static HashMap<Integer, GlyphMaker> makers = new HashMap<Integer, GlyphMaker>();

    /**
     * The instance that has the mono (default) font.
     */
    private GlyphMakerFont makerMono;

    /**
     * The instance that has the CJK font.
     */
    private GlyphMakerFont makerCjk;

    /**
     * The instance that has the emoji font.
     */
    private GlyphMakerFont makerEmoji;

    /**
     * The instance that has the fallback font.
     */
    private GlyphMakerFont makerFallback;

    /**
     * The system mono font.
     */
    private GlyphMakerFont makerSystemMono;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Create an instance with references to the necessary fonts.
     *
     * @param fontSize the size of these fonts in pixels
     */
    private GlyphMaker(final int fontSize) {
        makerMono = new GlyphMakerFont(MONO, fontSize);
        makerSystemMono = new GlyphMakerFont("", fontSize);

        String fontFilename = null;
        fontFilename = System.getProperty("casciian.cjkFont.filename",
            cjkFontFilename);
        makerCjk = new GlyphMakerFont(fontFilename, fontSize);
        fontFilename = System.getProperty("casciian.emojiFont.filename",
            emojiFontFilename);
        makerEmoji = new GlyphMakerFont(fontFilename, fontSize);
        fontFilename = System.getProperty("casciian.fallbackFont.filename",
            fallbackFontFilename);
        makerFallback = new GlyphMakerFont(fontFilename, fontSize);
    }

    // ------------------------------------------------------------------------
    // GlyphMaker -------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Obtain the GlyphMaker instance for a particular font size.
     *
     * @param fontSize the size of these fonts in pixels
     * @return the instance
     */
    public static GlyphMaker getInstance(final int fontSize) {
        synchronized (GlyphMaker.class) {
            GlyphMaker maker = makers.get(fontSize);
            if (maker == null) {
                maker = new GlyphMaker(fontSize);
                makers.put(fontSize, maker);
            }
            return maker;
        }
    }

    /**
     * Get a glyph image.
     *
     * @param cell the character to draw
     * @param cellWidth the width of the text cell to draw into
     * @param cellHeight the height of the text cell to draw into
     * @param backend the backend that can obtain the correct background
     * color
     * @return the glyph as an image
     */
    public BufferedImage getImage(final Cell cell, final int cellWidth,
        final int cellHeight, final Backend backend) {

        return getImage(cell, cellWidth, cellHeight, backend, true,
            Cell.Width.SINGLE);
    }

    /**
     * Get a glyph image.
     *
     * @param cell the character to draw
     * @param cellWidth the width of the text cell to draw into
     * @param cellHeight the height of the text cell to draw into
     * @param backend the backend that can obtain the correct background
     * color
     * @param blinkVisible if true, the cell is visible if it is blinking
     * @return the glyph as an image
     */
    public BufferedImage getImage(final Cell cell, final int cellWidth,
        final int cellHeight, final Backend backend,
        final boolean blinkVisible) {

        return getImage(cell, cellWidth, cellHeight, backend, blinkVisible,
            Cell.Width.SINGLE);
    }

    /**
     * Get a glyph image.
     *
     * @param cell the character to draw
     * @param cellWidth the width of the text cell to draw into
     * @param cellHeight the height of the text cell to draw into
     * @param backend the backend that can obtain the correct background
     * color
     * @param blinkVisible if true, the cell is visible if it is blinking
     * @param widthEnum If SINGLE, return the entire image. If LEFT or RIGHT,
     * return only half of the image (either left side or right side).
     * @return the glyph as an image
     */
    public BufferedImage getImage(final Cell cell, final int cellWidth,
        final int cellHeight, final Backend backend,
        final boolean blinkVisible, final Cell.Width widthEnum) {

        int ch = cell.getChar();
        if (ExtendedGraphemeClusterUtils.isCjk(ch)) {
            if (makerCjk.canDisplay(ch)) {
                // System.err.println("CJK: " + String.format("0x%x", ch));
                return makerCjk.getImage(cell, cellWidth, cellHeight, backend,
                    blinkVisible, widthEnum);
            }
        }
        if (ExtendedGraphemeClusterUtils.isEmoji(ch)) {
            if (makerEmoji.canDisplay(ch)) {
                // System.err.println("emoji: " + String.format("0x%x", ch));
                return makerEmoji.getImage(cell, cellWidth, cellHeight, backend,
                    blinkVisible, widthEnum);
            }
        }

        if (makerFallback.canDisplay(ch)) {
            // System.err.println("fallback: " + String.format("0x%x", ch));
            return makerFallback.getImage(cell, cellWidth, cellHeight, backend,
                blinkVisible, widthEnum);
        }

        if (makerSystemMono.canDisplay(ch)) {
            // System.err.println("system mono: " + String.format("0x%x", ch));
            return makerSystemMono.getImage(cell, cellWidth, cellHeight,
                backend, blinkVisible, widthEnum);
        }

        // When all else fails, use the default.
        // System.err.println("mono: " + String.format("0x%x", ch));
        return makerMono.getImage(cell, cellWidth, cellHeight, backend,
            blinkVisible, widthEnum);
    }

    /**
     * Check if a CJK font is available.
     *
     * @return true if a CJK font is available
     */
    public boolean isCjk() {
        return makerCjk.isLoaded();
    }

    /**
     * Check if an emoji font is available.
     *
     * @return true if an emoji font is available
     */
    public boolean isEmoji() {
        return makerEmoji.isLoaded();
    }

    /**
     * Check if a fallback font is available.
     *
     * @return true if a fallback font is available
     */
    public boolean isFallback() {
        return makerFallback.isLoaded();
    }

    /**
     * Checks if a fallback font has a glyph for the specified character.
     *
     * @param codePoint the character (Unicode code point) for which a glyph
     * is needed.
     * @return true if this Font has a glyph for the character; false
     * otherwise.
     */
    public boolean canDisplay(final int codePoint) {
        if ((makerFallback.isLoaded() && makerFallback.canDisplay(codePoint))
            || (makerEmoji.isLoaded() && makerEmoji.canDisplay(codePoint))
            || (makerCjk.isLoaded() && makerCjk.canDisplay(codePoint))

            // Put the system mono font ahead of terminus.
            || makerSystemMono.canDisplay(codePoint)
            || (makerMono.isLoaded() && makerMono.canDisplay(codePoint))
        ) {
            return true;
        }
        return false;
    }

}
