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
package casciian.backend;

import casciian.bits.ImageRGB;

/**
 * SixelEncoder turns a ImageRGB into String of sixel image data.
 */
public interface SixelEncoder {

    // ------------------------------------------------------------------------
    // SixelEncoder -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Reload options from System properties.
     */
    public void reloadOptions();

    /**
     * Create a sixel string representing a bitmap.  The returned string does
     * NOT include the DCS start or ST end sequences.
     *
     * @param bitmap the bitmap data
     * @return the string to emit to an ANSI / ECMA-style terminal
     */
    public String toSixel(final ImageRGB bitmap);

    /**
     * If the palette is shared for the entire terminal, emit it to a
     * StringBuilder.
     *
     * @param sb the StringBuilder to write the shared palette to
     */
    public void emitPalette(final StringBuilder sb);

    /**
     * Get the sixel shared palette option.
     *
     * @return true if all sixel output is using the same palette that is set
     * in one DCS sequence and used in later sequences
     */
    public boolean hasSharedPalette();

    /**
     * Set the sixel shared palette option.
     *
     * @param sharedPalette if true, then all sixel output will use the same
     * palette that is set in one DCS sequence and used in later sequences
     */
    public void setSharedPalette(final boolean sharedPalette);

    /**
     * Get the number of colors in the sixel palette.
     *
     * @return the palette size
     */
    public int getPaletteSize();

    /**
     * Set the number of colors in the sixel palette.
     *
     * @param paletteSize the new palette size
     */
    public void setPaletteSize(final int paletteSize);

    /**
     * Clear the sixel palette.  It will be regenerated on the next image
     * encode.
     */
    public void clearPalette();

}
