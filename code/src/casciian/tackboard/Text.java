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
package casciian.tackboard;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import casciian.bits.StringUtils;

/**
 * Text is a raw bitmap image.
 */
public class Text extends Bitmap {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The text.
     */
    private String text;

    /**
     * The font.
     */
    private Font font;

    /**
     * The font size in points.
     */
    private int fontSize;

    /**
     * The color.
     */
    private Color color;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param x X pixel coordinate
     * @param y Y pixel coordinate
     * @param z Z coordinate
     * @param text the text string
     * @param fontName the font name
     * @param fontSize the font size in points
     * @param color the color of the text
     */
    public Text(final int x, final int y, final int z,
        final String text, final String fontName, final int fontSize,
        final Color color) {

        super(x, y, z, null);

        this.text = text;
        this.fontSize = fontSize;
        this.color = color;
        font = new Font(fontName, Font.PLAIN, fontSize);
    }

    /**
     * Public constructor.
     *
     * @param x X pixel coordinate
     * @param y Y pixel coordinate
     * @param z Z coordinate
     * @param text the text string
     * @param font the font
     * @param fontSize the font size in points
     * @param color the color of the text
     */
    public Text(final int x, final int y, final int z,
        final String text, final Font font, final int fontSize,
        final Color color) {

        super(x, y, z, null);

        this.text = text;
        this.fontSize = fontSize;
        this.color = color;
        this.font = font;
    }

    // ------------------------------------------------------------------------
    // TackboardItem ----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Comparison check.  All fields must match to return true.
     *
     * @param rhs another Text instance
     * @return true if all fields are equal
     */
    @Override
    public boolean equals(final Object rhs) {
        if (!(rhs instanceof Text)) {
            return false;
        }
        Text that = (Text) rhs;
        return (super.equals(rhs)
            && (this.text.equals(that.text))
            && (this.fontSize == that.fontSize)
            && (this.color.equals(that.color)));
    }

    /**
     * Hashcode uses all fields in equals().
     *
     * @return the hash
     */
    @Override
    public int hashCode() {
        int A = 13;
        int B = 23;
        int hash = A;
        hash = (B * hash) + super.hashCode();
        hash = (B * hash) + text.hashCode();
        hash = (B * hash) + fontSize;
        hash = (B * hash) + color.hashCode();
        return hash;
    }

    /**
     * Make human-readable description of this item.
     *
     * @return displayable String
     */
    @Override
    public String toString() {
        return String.format("(%d, %d, %d) '%s'", getX(), getY(), getZ(),
            text);
    }

    /**
     * Get this item rendered to a bitmap, offset to align on a grid of
     * cells with pixel dimensions (textWidth, textHeight).
     *
     * @param textWidth the width of a text cell
     * @param textHeight the height of a text cell
     * @return the image, or null if this item does not have any pixels to
     * show
     */
    @Override
    public BufferedImage getImage(final int textWidth, final int textHeight) {
        if (dirty) {
            // Estimate the pixels needed to render the text.
            int width = 0;
            int height = 0;
            String [] rawLines = text.split("\n");
            for (int i = 0; i < rawLines.length; i++) {
                int lineWidth = StringUtils.width(rawLines[i]) * textWidth;
                width = Math.max(width, lineWidth);
            }
            width *= (fontSize / 2);
            height = (rawLines.length + 1) * (int) (fontSize * 1.5);

            BufferedImage newImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
            Graphics2D gr = newImage.createGraphics();
            gr.setFont(font);
            gr.setColor(color);

            // Because this is text, let's enable anti-aliasing.
            gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            FontMetrics fm = gr.getFontMetrics();
            int maxDescent = fm.getMaxDescent();
            int maxAscent = fm.getMaxAscent();

            for (int i = 0; i < rawLines.length; i++) {
                gr.drawString(rawLines[i], 0,
                    (fontSize * (i + 1)) + maxAscent - maxDescent);
            }
            gr.dispose();
            setImage(newImage);
        }
        return super.getImage(textWidth, textHeight);
    }

    // ------------------------------------------------------------------------
    // Text -------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
