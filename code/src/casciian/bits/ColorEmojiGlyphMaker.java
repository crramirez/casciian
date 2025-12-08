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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import javax.imageio.ImageIO;

import casciian.backend.Backend;

/**
 * ColorEmojiGlyphMaker provides access to the color emoji image files
 * located in "emojis/" on the classpath.
 */
public class ColorEmojiGlyphMaker {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Cache of original-size images by lookup string.
     */
    private static HashMap<String, BufferedImage> emojis = new HashMap<String, BufferedImage>();

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Private constructor prevents accidental creation of this class.
     */
    private ColorEmojiGlyphMaker() {}

    // ------------------------------------------------------------------------
    // ColorEmojiGlyphMaker ---------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Make the lookup key string for a sequence of codepoints.
     *
     * @param codePoints the emoji (sequence of Unicode codepoints)
     * @return the lookup key ("0023-FE0F-20E3.png", "1F1E8-1F1E7.png", etc.)
     */
    private static String makeKey(final int [] codePoints) {
        StringBuilder sb = new StringBuilder();
        sb.append("emoji/");
        sb.append(String.format("%04x", codePoints[0]).toUpperCase());
        if ((codePoints.length == 2) && (codePoints[1] == 0xFE0F)) {
            sb.append(".png");
            return sb.toString();
        }
        for (int i = 1; i < codePoints.length; i++) {
            sb.append("-");
            sb.append(String.format("%04x", codePoints[i]).toUpperCase());
        }
        sb.append(".png");
        return sb.toString();
    }

    /**
     * Checks if an emoji glyph for the specified codepoint is available.
     *
     * @param ch the emoji (single Unicode codepoint) for which a glyph is
     * needed.
     * @return true if a glyph for the character is available; false
     * otherwise.
     */
    public static boolean canDisplay(final int ch) {
        int [] codePoints = new int[1];
        codePoints[0] = ch;
        return canDisplay(codePoints);
    }

    /**
     * Checks if an emoji glyph for the specified codepoint(s) is available.
     *
     * @param codePoints the emoji (sequence of Unicode codepoints) for which
     * a glyph is needed.
     * @return true if a glyph for the character is available; false
     * otherwise.
     */
    public static boolean canDisplay(final int [] codePoints) {
        String key = makeKey(codePoints);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        java.net.URL url = loader.getResource(key);
        if (url == null) {
            return false;
        }
        return true;
    }

    /**
     * Checks if an emoji glyph for the specified codepoint(s) is available.
     *
     * @param cell the cell containing an emoji (sequence of Unicode
     * codepoints) for which a glyph is needed.
     * @return true if a glyph for the character is available; false
     * otherwise.
     */
    public static boolean canDisplay(final Cell cell) {
        int [] codePoints;
        if (cell instanceof ComplexCell) {
            codePoints = ((ComplexCell) cell).getCodePoints();
        } else {
            codePoints = new int[1];
            codePoints[0] = cell.getChar();
        }
        return canDisplay(codePoints);
    }

    /**
     * Checks if an emoji glyph for the specified codepoint(s) is available.
     *
     * @param codePoints the emoji (sequence of Unicode codepoints) for which
     * a glyph is needed.
     * @return true if a glyph for the character is available; false
     * otherwise.
     */
    private static BufferedImage getEmoji(final int [] codePoints) {
        String key = makeKey(codePoints);
        if (emojis.containsKey(key)) {
            return emojis.get(key);
        }

        BufferedImage image = null;
        try {
            ClassLoader loader = Thread.currentThread().
                getContextClassLoader();
            String filename = makeKey(codePoints);
            java.net.URL url = loader.getResource(filename);
            assert (url != null);
            image = ImageIO.read(url);
            emojis.put(key, image);
        } catch (IOException e) {
            // SQUASH
        }
        return image;
    }

    /**
     * Get an emoji image for a complex cell.
     *
     * @param complexCell the emoji to draw
     * @param cellWidth the width of the text cell to draw into
     * @param cellHeight the height of the text cell to draw into
     * @param backend the backend that can obtain the correct background
     * color
     * @param blinkVisible if true, the cell is visible if it is blinking
     * @return the glyph as an image
     */
    public static BufferedImage getImage(final ComplexCell complexCell,
        final int cellWidth, final int cellHeight, final Backend backend,
        final boolean blinkVisible) {

        return getImage(complexCell, cellWidth, cellHeight, backend,
            blinkVisible, Cell.Width.SINGLE);
    }

    /**
     * Get an emoji image for a complex cell.
     *
     * @param complexCell the emoji to draw
     * @param cellWidth the width of the text cell to draw into
     * @param cellHeight the height of the text cell to draw into
     * @param backend the backend that can obtain the correct background
     * color
     * @param blinkVisible if true, the cell is visible if it is blinking
     * @param widthEnum If SINGLE, return the entire image. If LEFT or RIGHT,
     * return only half of the image (either left side or right side).
     * @return the glyph as an image
     */
    public static BufferedImage getImage(final ComplexCell complexCell,
        final int cellWidth, final int cellHeight, final Backend backend,
        final boolean blinkVisible, final Cell.Width widthEnum) {

        // Generate glyph and draw it.
        BufferedImage image = new BufferedImage(cellWidth, cellHeight,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr2 = image.createGraphics();

        Cell cellColor = new Cell(complexCell);

        // Check for reverse
        if (complexCell.isReverse()) {
            if (complexCell.getBackColorRGB() < 0) {
                cellColor.setForeColor(complexCell.getBackColor());
            } else {
                cellColor.setForeColorRGB(complexCell.getBackColorRGB());
            }
            if (complexCell.getForeColorRGB() < 0) {
                cellColor.setBackColor(complexCell.getForeColor());
            } else {
                cellColor.setBackColorRGB(complexCell.getForeColorRGB());
            }
        }

        // Draw the background rectangle.
        gr2.setColor(backend.attrToBackgroundColor(cellColor));
        gr2.fillRect(0, 0, cellWidth, cellHeight);

        BufferedImage emojiImage = getEmoji(complexCell.getCodePoints());

        if (emojiImage != null) {
            // Blit the emoji over the background, vertically aligned to the
            // middle.
            int emojiWidth = emojiImage.getWidth();
            int emojiHeight = emojiImage.getHeight();
            int yOffset = 0;
            int padPixels = 2;
            if (emojiWidth != emojiHeight) {
                yOffset = ((emojiWidth - emojiHeight) / 2);
                yOffset *= (cellWidth / emojiWidth);
                if (yOffset < 0) {
                    yOffset = 0;
                }
            }
            gr2.drawImage(emojiImage, padPixels, yOffset + padPixels,
                cellWidth - (2 * padPixels),
                (cellHeight - (yOffset * 2) - (2 * padPixels)),
                0, 0, emojiWidth, emojiHeight, null);
        }

        // Handle blink and underline
        if (complexCell.isUnderline()
            && (!complexCell.isBlink()
                || (complexCell.isBlink() && blinkVisible))
        ) {
            gr2.setColor(backend.attrToForegroundColor(cellColor));
            gr2.fillRect(0, cellHeight - 2, cellWidth, 2);
        }
        gr2.dispose();

        int leftHalf = cellWidth / 2;
        int rightHalf = cellWidth - leftHalf;

        if (widthEnum == Cell.Width.LEFT) {
            return image.getSubimage(0, 0, leftHalf, image.getHeight());
        }
        if (widthEnum == Cell.Width.RIGHT) {
            return image.getSubimage(leftHalf, 0, rightHalf, image.getHeight());
        }

        return image;
    }

}
