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

import java.util.List;
import java.util.ArrayList;

/**
 * ExtendedGraphemeClusterUtils implements most, but not all, of the grapheme
 * cluster breaking rules of Unicode TR #29 section 3.1.1. Specifically:
 *
 * <ul>
 *   <li>GB3 is deliberately ignored.</li>
 *   <li>GB4 and GB5 will break at all control characters including CR.</li>
 *   <li>GB9c is not implemented.</li>
 *   <li>GB11 and GB12 do not count "evenness" of previous regional
 *       indicator (RI) symbols, instead always joining.</li>
 * </ul>
 */
public class ExtendedGraphemeClusterUtils {

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Private constructor prevents accidental creation of this class.
     */
    private ExtendedGraphemeClusterUtils() {}

    // ------------------------------------------------------------------------
    // ExtendedGraphemeClusterUtils -------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Check if character is in the CJK range.
     *
     * @param ch character to check
     * @return true if this character is in the CJK range
     */
    public static boolean isCjk(final int ch) {
        return ((ch >= 0x2e80) && (ch <= 0x9fff));
    }

    /**
     * Check if character is in the braille range.
     *
     * @param ch character to check
     * @return true if this character is in the braille range
     */
    public static boolean isBraille(final int ch) {
        return ((ch >= 0x2800) && (ch <= 0x28ff));
    }

    /**
     * Check if character is in the emoji range of the Basic Multilingual
     * Plane (Emoji, Emoji_Component, Extended_Pictographic).
     *
     * @param ch character to check
     * @return true if this character is in the emoji range
     */
    public static boolean isEmojiBMP(final int ch) {
        // Emoji Version 16.0 Extended Pictographic range.
        if (false
            /*
             * These codepoints can be displayed as either text style
             * (0xFE0E) or emoji style (0xFE0F), and for UI work we typically
             * will want them text style, so they are omitted from the
             * isEmoji() check.  But for completeness, one should also be
             * able to identify these as emoji.
             */
            || (ch == 0x00A9)
            || (ch == 0x00AE)
            || (ch == 0x203C)
            || (ch == 0x2049)
            || (ch == 0x2122)
            || (ch == 0x2139)
            || ((ch >= 0x2194) && (ch <= 0x2199))
            || ((ch >= 0x21A9) && (ch <= 0x21AA))
            || ((ch >= 0x231A) && (ch <= 0x231B))
            || (ch == 0x2328)
            || (ch == 0x2388)
            || (ch == 0x23CF)
            || ((ch >= 0x23E9) && (ch <= 0x23F3))
            || ((ch >= 0x23F8) && (ch <= 0x23FA))
            || (ch == 0x24C2)
            || ((ch >= 0x25AA) && (ch <= 0x25AB))
            || (ch == 0x25B6)
            || (ch == 0x25C0)
            || ((ch >= 0x25FB) && (ch <= 0x2605))
            || ((ch >= 0x2607) && (ch <= 0x2612))
            || ((ch >= 0x2614) && (ch <= 0x2685))
            || ((ch >= 0x2690) && (ch <= 0x2705))
            || ((ch >= 0x2708) && (ch <= 0x2712))
            || (ch == 0x2714)
            || (ch == 0x2716)
            || (ch == 0x271D)
            || (ch == 0x2721)
            || (ch == 0x2728)
            || ((ch >= 0x2733) && (ch <= 0x2734))
            || (ch == 0x2744)
            || (ch == 0x2747)
            || (ch == 0x274C)
            || (ch == 0x274E)
            || ((ch >= 0x2753) && (ch <= 0x2755))
            || (ch == 0x2757)
            || ((ch >= 0x2763) && (ch <= 0x2767))
            || ((ch >= 0x2795) && (ch <= 0x2797))
            || (ch == 0x27A1)
            || (ch == 0x27B0)
            || (ch == 0x27BF)
            || ((ch >= 0x2934) && (ch <= 0x2935))
            || ((ch >= 0x2B05) && (ch <= 0x2B07))
            || ((ch >= 0x2B1B) && (ch <= 0x2B1C))
            || (ch == 0x2B50)
            || (ch == 0x2B55)
            || (ch == 0x3030)
            || (ch == 0x303D)
            || (ch == 0x3297)
            || (ch == 0x3299)
        ) {
            return true;
        }
        return false;
    }

    /**
     * Check if character is in the emoji range (Emoji, Emoji_Component,
     * Extended_Pictographic) AND not in the Basic Multilingual Plane.  For a
     * full check of ALL emoji, use 'isEmoji(x) || isEmojiBMP(x)'.
     *
     * @param ch character to check
     * @return true if this character is in the emoji range
     */
    public static boolean isEmoji(final int ch) {
        // Emoji Version 16.0 Extended Pictographic range.
        if (false
            /*
             * These codepoints can be displayed as either text style
             * (0xFE0E) or emoji style (0xFE0F), and typically we will want
             * them text style.  They are left out of isEmoji() so that they
             * go through the font mechanism (GlyphEncoder) rather than the
             * PNG blit mechanism (ColorEmojiGlyphMaker).
             */
            /*
            || (ch == 0x00A9)
            || (ch == 0x00AE)
            || (ch == 0x203C)
            || (ch == 0x2049)
            || (ch == 0x2122)
            || (ch == 0x2139)
            || ((ch >= 0x2194) && (ch <= 0x2199))
            || ((ch >= 0x21A9) && (ch <= 0x21AA))
            || ((ch >= 0x231A) && (ch <= 0x231B))
            || (ch == 0x2328)
            || (ch == 0x2388)
            || (ch == 0x23CF)
            || ((ch >= 0x23E9) && (ch <= 0x23F3))
            || ((ch >= 0x23F8) && (ch <= 0x23FA))
            || (ch == 0x24C2)
            || ((ch >= 0x25AA) && (ch <= 0x25AB))
            || (ch == 0x25B6)
            || (ch == 0x25C0)
            || ((ch >= 0x25FB) && (ch <= 0x2605))
            || ((ch >= 0x2607) && (ch <= 0x2612))
            || ((ch >= 0x2614) && (ch <= 0x2685))
            || ((ch >= 0x2690) && (ch <= 0x2705))
            || ((ch >= 0x2708) && (ch <= 0x2712))
            || (ch == 0x2714)
            || (ch == 0x2716)
            || (ch == 0x271D)
            || (ch == 0x2721)
            || (ch == 0x2728)
            || ((ch >= 0x2733) && (ch <= 0x2734))
            || (ch == 0x2744)
            || (ch == 0x2747)
            || (ch == 0x274C)
            || (ch == 0x274E)
            || ((ch >= 0x2753) && (ch <= 0x2755))
            || (ch == 0x2757)
            || ((ch >= 0x2763) && (ch <= 0x2767))
            || ((ch >= 0x2795) && (ch <= 0x2797))
            || (ch == 0x27A1)
            || (ch == 0x27B0)
            || (ch == 0x27BF)
            || ((ch >= 0x2934) && (ch <= 0x2935))
            || ((ch >= 0x2B05) && (ch <= 0x2B07))
            || ((ch >= 0x2B1B) && (ch <= 0x2B1C))
            || (ch == 0x2B50)
            || (ch == 0x2B55)
            || (ch == 0x3030)
            || (ch == 0x303D)
            || (ch == 0x3297)
            || (ch == 0x3299)
             */
            || (ch == 0x26A0)
            || (ch == 0x26D4)
            || (ch == 0x2728)

            || ((ch >= 0x1F000) && (ch <= 0x1F0FF))
            || ((ch >= 0x1F10D) && (ch <= 0x1F10F))
            || (ch == 0x1F12F)
            || ((ch >= 0x1F16C) && (ch <= 0x1F171))
            || ((ch >= 0x1F17E) && (ch <= 0x1F17F))
            || (ch == 0x1F18E)
            || ((ch >= 0x1F191) && (ch <= 0x1F19A))
            || ((ch >= 0x1F1AD) && (ch <= 0x1F1E5))
            || ((ch >= 0x1F1E6) && (ch <= 0x1F1FF))
            || ((ch >= 0x1F201) && (ch <= 0x1F20F))
            || (ch == 0x1F21A)
            || (ch == 0x1F22F)
            || ((ch >= 0x1F232) && (ch <= 0x1F23A))
            || ((ch >= 0x1F23C) && (ch <= 0x1F23F))
            || ((ch >= 0x1F249) && (ch <= 0x1F319))
            || ((ch >= 0x1F31A) && (ch <= 0x1F3FA))
            || ((ch >= 0x1F400) && (ch <= 0x1F53D))
            || ((ch >= 0x1F546) && (ch <= 0x1F64F))
            || ((ch >= 0x1F680) && (ch <= 0x1F6FF))
            || ((ch >= 0x1F774) && (ch <= 0x1F77F))
            || ((ch >= 0x1F7D5) && (ch <= 0x1F7FF))
            || ((ch >= 0x1F80C) && (ch <= 0x1F80F))
            || ((ch >= 0x1F848) && (ch <= 0x1F84F))
            || ((ch >= 0x1F85A) && (ch <= 0x1F85F))
            || ((ch >= 0x1F888) && (ch <= 0x1F88F))
            || ((ch >= 0x1F8AE) && (ch <= 0x1F8FF))
            || ((ch >= 0x1F90C) && (ch <= 0x1F93A))
            || ((ch >= 0x1F93C) && (ch <= 0x1F945))
            || ((ch >= 0x1F947) && (ch <= 0x1FAFF))
            || ((ch >= 0x1FC00) && (ch <= 0x1FFFD))
        ) {
            return true;
        }
        return false;
    }

    /**
     * Check if character is in the Emoji_Component range.  Emoji_Component
     * codepoints are part of larger sequences, but some of them can also
     * stand alone to represent glyphs (Emoji, Extended_Pictographic).
     *
     * @param ch character to check
     * @return true if this character is in the emoji component range
     */
    public static boolean isEmojiComponent(final int ch) {
        if ((ch == 0x0023)
            || (ch == 0x002A)
            || ((ch >= 0x0030) && (ch <= 0x0039))
            || (ch == 0x200D)
            || (ch == 0x20E3)
            || (ch == 0xFE0F)
            || ((ch >= 0x1F1E6) && (ch <= 0x1F1FF))
            || ((ch >= 0x1F3FB) && (ch <= 0x1F3FF))
            || ((ch >= 0x1F9B0) && (ch <= 0x1F9B3))
            || ((ch >= 0xE0020) && (ch <= 0xE007F))
        ) {
            return true;
        }
        return false;
    }

    /**
     * Check if character will always be part of a larger emoji sequence.
     *
     * @param ch character to check
     * @return true if this character is only used to combine/modify emoji
     * codepoints.
     */
    public static boolean isEmojiCombiner(final int ch) {
        if ((ch == 0xFE0E)
            || (ch == 0xFE0F)
            || (ch == 0x200D)
            || (ch == 0x20E3)
            || ((ch >= 0x1F1E6) && (ch <= 0x1F1FF))
            || ((ch >= 0x1F3FB) && (ch <= 0x1F3FF))
            || ((ch >= 0x1F9B0) && (ch <= 0x1F9B3))
            || ((ch >= 0xE0020) && (ch <= 0xE007F))
        ) {
            return true;
        }
        return false;
    }

    /**
     * Check if character is a Regional Indicator (RI) symbol.
     *
     * @param ch character to check
     * @return true if this character is a Regional Indicator (RI) symbol
     */
    public static boolean isRegionalIndicator(final int ch) {
        if ((ch >= 0x1F1E6) && (ch <= 0x1F1FF)) {
            return true;
        }
        return false;
    }

    /**
     * Check if character is in the Symbols for Legacy Computing range.
     *
     * @param ch character to check
     * @return true if this character is in the Symbols for Legacy Computing
     * range
     */
    public static boolean isLegacyComputingSymbol(final int ch) {
        return ((ch >= 0x1fb00) && (ch <= 0x1fbff));
    }

    /**
     * Check if codepoint has the Prepend Grapheme_Cluster_Break property.
     *
     * @param ch character to check
     * @return true if Prepend
     */
    public static boolean isPrepend(final int ch) {
        if (((ch >= 0x0600) && (ch <= 0x0605))
            || (ch == 0x06DD)
            || (ch == 0x070F)
            || ((ch >= 0x0890) && (ch <= 0x0891))
            || (ch == 0x08E2)
            || (ch == 0x0D4E)
            || (ch == 0x110BD)
            || (ch == 0x110CD)
            || ((ch >= 0x111C2) && (ch <= 0x111C3))
            || (ch == 0x113D1)
            || (ch == 0x1193F)
            || (ch == 0x11941)
            || ((ch >= 0x11A84) && (ch <= 0x11A89))
            || (ch == 0x11D46)
            || (ch == 0x11F02)
        ) {
            return true;
        }
        return false;
    }

    /**
     * Check if codepoint has the CR Grapheme_Cluster_Break property.
     *
     * @param ch character to check
     * @return true if CR
     */
    public static boolean isCR(final int ch) {
        return (ch == 0x0D);
    }

    /**
     * Check if codepoint has the LF Grapheme_Cluster_Break property.
     *
     * @param ch character to check
     * @return true if LF
     */
    public static boolean isLF(final int ch) {
        return (ch == 0x0A);
    }

    /**
     * Check if codepoint has the Control Grapheme_Cluster_Break property.
     *
     * @param ch character to check
     * @return true if Control
     */
    public static boolean isControl(final int ch) {
        if (((ch >= 0x0000) && (ch <= 0x0009))
            || ((ch >= 0x000B) && (ch <= 0x000C))
            || ((ch >= 0x000E) && (ch <= 0x001F))
            || ((ch >= 0x007F) && (ch <= 0x009F))
            || (ch == 0x00AD)
            || (ch == 0x061C)
            || (ch == 0x180E)
            || (ch == 0x200B)
            || ((ch >= 0x200E) && (ch <= 0x200F))
            || ((ch >= 0x2028) && (ch <= 0x202E))
            || ((ch >= 0x2060) && (ch <= 0x206F))
            || (ch == 0xFEFF)
            || ((ch >= 0xFFF0) && (ch <= 0xFFFB))
            || ((ch >= 0x13430) && (ch <= 0x1343F))
            || ((ch >= 0x1BCA0) && (ch <= 0x1BCA3))
            || ((ch >= 0x1D173) && (ch <= 0x1D17A))
            || ((ch >= 0xE0000) && (ch <= 0xE001F))
            || ((ch >= 0xE0080) && (ch <= 0xE00FF))
            || ((ch >= 0xE01F0) && (ch <= 0xE0FFF))
        ) {
            return true;
        }
        return false;
    }

    /**
     * Check if codepoint has the Extend Grapheme_Cluster_Break property.
     *
     * @param ch character to check
     * @return true if Extend
     */
    public static boolean isExtend(final int ch) {
        if (((ch >= 0x0300) && (ch <= 0x036F))
            || ((ch >= 0x0483) && (ch <= 0x0487))
            || ((ch >= 0x0488) && (ch <= 0x0489))
            || ((ch >= 0x0591) && (ch <= 0x05BD))
            || (ch == 0x05BF)
            || ((ch >= 0x05C1) && (ch <= 0x05C2))
            || ((ch >= 0x05C4) && (ch <= 0x05C5))
            || (ch == 0x05C7)
            || ((ch >= 0x0610) && (ch <= 0x061A))
            || ((ch >= 0x064B) && (ch <= 0x065F))
            || (ch == 0x0670)
            || ((ch >= 0x06D6) && (ch <= 0x06DC))
            || ((ch >= 0x06DF) && (ch <= 0x06E4))
            || ((ch >= 0x06E7) && (ch <= 0x06E8))
            || ((ch >= 0x06EA) && (ch <= 0x06ED))
            || (ch == 0x0711)
            || ((ch >= 0x0730) && (ch <= 0x074A))
            || ((ch >= 0x07A6) && (ch <= 0x07B0))
            || ((ch >= 0x07EB) && (ch <= 0x07F3))
            || (ch == 0x07FD)
            || ((ch >= 0x0816) && (ch <= 0x0819))
            || ((ch >= 0x081B) && (ch <= 0x0823))
            || ((ch >= 0x0825) && (ch <= 0x0827))
            || ((ch >= 0x0829) && (ch <= 0x082D))
            || ((ch >= 0x0859) && (ch <= 0x085B))
            || ((ch >= 0x0897) && (ch <= 0x089F))
            || ((ch >= 0x08CA) && (ch <= 0x08E1))
            || ((ch >= 0x08E3) && (ch <= 0x0902))
            || (ch == 0x093A)
            || (ch == 0x093C)
            || ((ch >= 0x0941) && (ch <= 0x0948))
            || (ch == 0x094D)
            || ((ch >= 0x0951) && (ch <= 0x0957))
            || ((ch >= 0x0962) && (ch <= 0x0963))
            || (ch == 0x0981)
            || (ch == 0x09BC)
            || (ch == 0x09BE)
            || ((ch >= 0x09C1) && (ch <= 0x09C4))
            || (ch == 0x09CD)
            || (ch == 0x09D7)
            || ((ch >= 0x09E2) && (ch <= 0x09E3))
            || (ch == 0x09FE)
            || ((ch >= 0x0A01) && (ch <= 0x0A02))
            || (ch == 0x0A3C)
            || ((ch >= 0x0A41) && (ch <= 0x0A42))
            || ((ch >= 0x0A47) && (ch <= 0x0A48))
            || ((ch >= 0x0A4B) && (ch <= 0x0A4D))
            || (ch == 0x0A51)
            || ((ch >= 0x0A70) && (ch <= 0x0A71))
            || (ch == 0x0A75)
            || ((ch >= 0x0A81) && (ch <= 0x0A82))
            || (ch == 0x0ABC)
            || ((ch >= 0x0AC1) && (ch <= 0x0AC5))
            || ((ch >= 0x0AC7) && (ch <= 0x0AC8))
            || (ch == 0x0ACD)
            || ((ch >= 0x0AE2) && (ch <= 0x0AE3))
            || ((ch >= 0x0AFA) && (ch <= 0x0AFF))
            || (ch == 0x0B01)
            || (ch == 0x0B3C)
            || (ch == 0x0B3E)
            || (ch == 0x0B3F)
            || ((ch >= 0x0B41) && (ch <= 0x0B44))
            || (ch == 0x0B4D)
            || ((ch >= 0x0B55) && (ch <= 0x0B56))
            || (ch == 0x0B57)
            || ((ch >= 0x0B62) && (ch <= 0x0B63))
            || (ch == 0x0B82)
            || (ch == 0x0BBE)
            || (ch == 0x0BC0)
            || (ch == 0x0BCD)
            || (ch == 0x0BD7)
            || (ch == 0x0C00)
            || (ch == 0x0C04)
            || (ch == 0x0C3C)
            || ((ch >= 0x0C3E) && (ch <= 0x0C40))
            || ((ch >= 0x0C46) && (ch <= 0x0C48))
            || ((ch >= 0x0C4A) && (ch <= 0x0C4D))
            || ((ch >= 0x0C55) && (ch <= 0x0C56))
            || ((ch >= 0x0C62) && (ch <= 0x0C63))
            || (ch == 0x0C81)
            || (ch == 0x0CBC)
            || (ch == 0x0CBF)
            || (ch == 0x0CC0)
            || (ch == 0x0CC2)
            || (ch == 0x0CC6)
            || ((ch >= 0x0CC7) && (ch <= 0x0CC8))
            || ((ch >= 0x0CCA) && (ch <= 0x0CCB))
            || ((ch >= 0x0CCC) && (ch <= 0x0CCD))
            || ((ch >= 0x0CD5) && (ch <= 0x0CD6))
            || ((ch >= 0x0CE2) && (ch <= 0x0CE3))
            || ((ch >= 0x0D00) && (ch <= 0x0D01))
            || ((ch >= 0x0D3B) && (ch <= 0x0D3C))
            || (ch == 0x0D3E)
            || ((ch >= 0x0D41) && (ch <= 0x0D44))
            || (ch == 0x0D4D)
            || (ch == 0x0D57)
            || ((ch >= 0x0D62) && (ch <= 0x0D63))
            || (ch == 0x0D81)
            || (ch == 0x0DCA)
            || (ch == 0x0DCF)
            || ((ch >= 0x0DD2) && (ch <= 0x0DD4))
            || (ch == 0x0DD6)
            || (ch == 0x0DDF)
            || (ch == 0x0E31)
            || ((ch >= 0x0E34) && (ch <= 0x0E3A))
            || ((ch >= 0x0E47) && (ch <= 0x0E4E))
            || (ch == 0x0EB1)
            || ((ch >= 0x0EB4) && (ch <= 0x0EBC))
            || ((ch >= 0x0EC8) && (ch <= 0x0ECE))
            || ((ch >= 0x0F18) && (ch <= 0x0F19))
            || (ch == 0x0F35)
            || (ch == 0x0F37)
            || (ch == 0x0F39)
            || ((ch >= 0x0F71) && (ch <= 0x0F7E))
            || ((ch >= 0x0F80) && (ch <= 0x0F84))
            || ((ch >= 0x0F86) && (ch <= 0x0F87))
            || ((ch >= 0x0F8D) && (ch <= 0x0F97))
            || ((ch >= 0x0F99) && (ch <= 0x0FBC))
            || (ch == 0x0FC6)
            || ((ch >= 0x102D) && (ch <= 0x1030))
            || ((ch >= 0x1032) && (ch <= 0x1037))
            || ((ch >= 0x1039) && (ch <= 0x103A))
            || ((ch >= 0x103D) && (ch <= 0x103E))
            || ((ch >= 0x1058) && (ch <= 0x1059))
            || ((ch >= 0x105E) && (ch <= 0x1060))
            || ((ch >= 0x1071) && (ch <= 0x1074))
            || (ch == 0x1082)
            || ((ch >= 0x1085) && (ch <= 0x1086))
            || (ch == 0x108D)
            || (ch == 0x109D)
            || ((ch >= 0x135D) && (ch <= 0x135F))
            || ((ch >= 0x1712) && (ch <= 0x1714))
            || (ch == 0x1715)
            || ((ch >= 0x1732) && (ch <= 0x1733))
            || (ch == 0x1734)
            || ((ch >= 0x1752) && (ch <= 0x1753))
            || ((ch >= 0x1772) && (ch <= 0x1773))
            || ((ch >= 0x17B4) && (ch <= 0x17B5))
            || ((ch >= 0x17B7) && (ch <= 0x17BD))
            || (ch == 0x17C6)
            || ((ch >= 0x17C9) && (ch <= 0x17D3))
            || (ch == 0x17DD)
            || ((ch >= 0x180B) && (ch <= 0x180D))
            || (ch == 0x180F)
            || ((ch >= 0x1885) && (ch <= 0x1886))
            || (ch == 0x18A9)
            || ((ch >= 0x1920) && (ch <= 0x1922))
            || ((ch >= 0x1927) && (ch <= 0x1928))
            || (ch == 0x1932)
            || ((ch >= 0x1939) && (ch <= 0x193B))
            || ((ch >= 0x1A17) && (ch <= 0x1A18))
            || (ch == 0x1A1B)
            || (ch == 0x1A56)
            || ((ch >= 0x1A58) && (ch <= 0x1A5E))
            || (ch == 0x1A60)
            || (ch == 0x1A62)
            || ((ch >= 0x1A65) && (ch <= 0x1A6C))
            || ((ch >= 0x1A73) && (ch <= 0x1A7C))
            || (ch == 0x1A7F)
            || ((ch >= 0x1AB0) && (ch <= 0x1ABD))
            || (ch == 0x1ABE)
            || ((ch >= 0x1ABF) && (ch <= 0x1ADD))
            || ((ch >= 0x1AE0) && (ch <= 0x1AEB))
            || ((ch >= 0x1B00) && (ch <= 0x1B03))
            || (ch == 0x1B34)
            || (ch == 0x1B35)
            || ((ch >= 0x1B36) && (ch <= 0x1B3A))
            || (ch == 0x1B3B)
            || (ch == 0x1B3C)
            || (ch == 0x1B3D)
            || (ch == 0x1B42)
            || ((ch >= 0x1B43) && (ch <= 0x1B44))
            || ((ch >= 0x1B6B) && (ch <= 0x1B73))
            || ((ch >= 0x1B80) && (ch <= 0x1B81))
            || ((ch >= 0x1BA2) && (ch <= 0x1BA5))
            || ((ch >= 0x1BA8) && (ch <= 0x1BA9))
            || (ch == 0x1BAA)
            || ((ch >= 0x1BAB) && (ch <= 0x1BAD))
            || (ch == 0x1BE6)
            || ((ch >= 0x1BE8) && (ch <= 0x1BE9))
            || (ch == 0x1BED)
            || ((ch >= 0x1BEF) && (ch <= 0x1BF1))
            || ((ch >= 0x1BF2) && (ch <= 0x1BF3))
            || ((ch >= 0x1C2C) && (ch <= 0x1C33))
            || ((ch >= 0x1C36) && (ch <= 0x1C37))
            || ((ch >= 0x1CD0) && (ch <= 0x1CD2))
            || ((ch >= 0x1CD4) && (ch <= 0x1CE0))
            || ((ch >= 0x1CE2) && (ch <= 0x1CE8))
            || (ch == 0x1CED)
            || (ch == 0x1CF4)
            || ((ch >= 0x1CF8) && (ch <= 0x1CF9))
            || ((ch >= 0x1DC0) && (ch <= 0x1DFF))
            || (ch == 0x200C)
            || ((ch >= 0x20D0) && (ch <= 0x20DC))
            || ((ch >= 0x20DD) && (ch <= 0x20E0))
            || (ch == 0x20E1)
            || ((ch >= 0x20E2) && (ch <= 0x20E4))
            || ((ch >= 0x20E5) && (ch <= 0x20F0))
            || ((ch >= 0x2CEF) && (ch <= 0x2CF1))
            || (ch == 0x2D7F)
            || ((ch >= 0x2DE0) && (ch <= 0x2DFF))
            || ((ch >= 0x302A) && (ch <= 0x302D))
            || ((ch >= 0x302E) && (ch <= 0x302F))
            || ((ch >= 0x3099) && (ch <= 0x309A))
            || (ch == 0xA66F)
            || ((ch >= 0xA670) && (ch <= 0xA672))
            || ((ch >= 0xA674) && (ch <= 0xA67D))
            || ((ch >= 0xA69E) && (ch <= 0xA69F))
            || ((ch >= 0xA6F0) && (ch <= 0xA6F1))
            || (ch == 0xA802)
            || (ch == 0xA806)
            || (ch == 0xA80B)
            || ((ch >= 0xA825) && (ch <= 0xA826))
            || (ch == 0xA82C)
            || ((ch >= 0xA8C4) && (ch <= 0xA8C5))
            || ((ch >= 0xA8E0) && (ch <= 0xA8F1))
            || (ch == 0xA8FF)
            || ((ch >= 0xA926) && (ch <= 0xA92D))
            || ((ch >= 0xA947) && (ch <= 0xA951))
            || (ch == 0xA953)
            || ((ch >= 0xA980) && (ch <= 0xA982))
            || (ch == 0xA9B3)
            || ((ch >= 0xA9B6) && (ch <= 0xA9B9))
            || ((ch >= 0xA9BC) && (ch <= 0xA9BD))
            || (ch == 0xA9C0)
            || (ch == 0xA9E5)
            || ((ch >= 0xAA29) && (ch <= 0xAA2E))
            || ((ch >= 0xAA31) && (ch <= 0xAA32))
            || ((ch >= 0xAA35) && (ch <= 0xAA36))
            || (ch == 0xAA43)
            || (ch == 0xAA4C)
            || (ch == 0xAA7C)
            || (ch == 0xAAB0)
            || ((ch >= 0xAAB2) && (ch <= 0xAAB4))
            || ((ch >= 0xAAB7) && (ch <= 0xAAB8))
            || ((ch >= 0xAABE) && (ch <= 0xAABF))
            || (ch == 0xAAC1)
            || ((ch >= 0xAAEC) && (ch <= 0xAAED))
            || (ch == 0xAAF6)
            || (ch == 0xABE5)
            || (ch == 0xABE8)
            || (ch == 0xABED)
            || (ch == 0xFB1E)
            || ((ch >= 0xFE00) && (ch <= 0xFE0F))
            || ((ch >= 0xFE20) && (ch <= 0xFE2F))
            || ((ch >= 0xFF9E) && (ch <= 0xFF9F))
            || (ch == 0x101FD)
            || (ch == 0x102E0)
            || ((ch >= 0x10376) && (ch <= 0x1037A))
            || ((ch >= 0x10A01) && (ch <= 0x10A03))
            || ((ch >= 0x10A05) && (ch <= 0x10A06))
            || ((ch >= 0x10A0C) && (ch <= 0x10A0F))
            || ((ch >= 0x10A38) && (ch <= 0x10A3A))
            || (ch == 0x10A3F)
            || ((ch >= 0x10AE5) && (ch <= 0x10AE6))
            || ((ch >= 0x10D24) && (ch <= 0x10D27))
            || ((ch >= 0x10D69) && (ch <= 0x10D6D))
            || ((ch >= 0x10EAB) && (ch <= 0x10EAC))
            || ((ch >= 0x10EFA) && (ch <= 0x10EFF))
            || ((ch >= 0x10F46) && (ch <= 0x10F50))
            || ((ch >= 0x10F82) && (ch <= 0x10F85))
            || (ch == 0x11001)
            || ((ch >= 0x11038) && (ch <= 0x11046))
            || (ch == 0x11070)
            || ((ch >= 0x11073) && (ch <= 0x11074))
            || ((ch >= 0x1107F) && (ch <= 0x11081))
            || ((ch >= 0x110B3) && (ch <= 0x110B6))
            || ((ch >= 0x110B9) && (ch <= 0x110BA))
            || (ch == 0x110C2)
            || ((ch >= 0x11100) && (ch <= 0x11102))
            || ((ch >= 0x11127) && (ch <= 0x1112B))
            || ((ch >= 0x1112D) && (ch <= 0x11134))
            || (ch == 0x11173)
            || ((ch >= 0x11180) && (ch <= 0x11181))
            || ((ch >= 0x111B6) && (ch <= 0x111BE))
            || (ch == 0x111C0)
            || ((ch >= 0x111C9) && (ch <= 0x111CC))
            || (ch == 0x111CF)
            || ((ch >= 0x1122F) && (ch <= 0x11231))
            || (ch == 0x11234)
            || (ch == 0x11235)
            || ((ch >= 0x11236) && (ch <= 0x11237))
            || (ch == 0x1123E)
            || (ch == 0x11241)
            || (ch == 0x112DF)
            || ((ch >= 0x112E3) && (ch <= 0x112EA))
            || ((ch >= 0x11300) && (ch <= 0x11301))
            || ((ch >= 0x1133B) && (ch <= 0x1133C))
            || (ch == 0x1133E)
            || (ch == 0x11340)
            || (ch == 0x1134D)
            || (ch == 0x11357)
            || ((ch >= 0x11366) && (ch <= 0x1136C))
            || ((ch >= 0x11370) && (ch <= 0x11374))
            || (ch == 0x113B8)
            || ((ch >= 0x113BB) && (ch <= 0x113C0))
            || (ch == 0x113C2)
            || (ch == 0x113C5)
            || ((ch >= 0x113C7) && (ch <= 0x113C9))
            || (ch == 0x113CE)
            || (ch == 0x113CF)
            || (ch == 0x113D0)
            || (ch == 0x113D2)
            || ((ch >= 0x113E1) && (ch <= 0x113E2))
            || ((ch >= 0x11438) && (ch <= 0x1143F))
            || ((ch >= 0x11442) && (ch <= 0x11444))
            || (ch == 0x11446)
            || (ch == 0x1145E)
            || (ch == 0x114B0)
            || ((ch >= 0x114B3) && (ch <= 0x114B8))
            || (ch == 0x114BA)
            || (ch == 0x114BD)
            || ((ch >= 0x114BF) && (ch <= 0x114C0))
            || ((ch >= 0x114C2) && (ch <= 0x114C3))
            || (ch == 0x115AF)
            || ((ch >= 0x115B2) && (ch <= 0x115B5))
            || ((ch >= 0x115BC) && (ch <= 0x115BD))
            || ((ch >= 0x115BF) && (ch <= 0x115C0))
            || ((ch >= 0x115DC) && (ch <= 0x115DD))
            || ((ch >= 0x11633) && (ch <= 0x1163A))
            || (ch == 0x1163D)
            || ((ch >= 0x1163F) && (ch <= 0x11640))
            || (ch == 0x116AB)
            || (ch == 0x116AD)
            || ((ch >= 0x116B0) && (ch <= 0x116B5))
            || (ch == 0x116B6)
            || (ch == 0x116B7)
            || (ch == 0x1171D)
            || (ch == 0x1171F)
            || ((ch >= 0x11722) && (ch <= 0x11725))
            || ((ch >= 0x11727) && (ch <= 0x1172B))
            || ((ch >= 0x1182F) && (ch <= 0x11837))
            || ((ch >= 0x11839) && (ch <= 0x1183A))
            || (ch == 0x11930)
            || ((ch >= 0x1193B) && (ch <= 0x1193C))
            || (ch == 0x1193D)
            || (ch == 0x1193E)
            || (ch == 0x11943)
            || ((ch >= 0x119D4) && (ch <= 0x119D7))
            || ((ch >= 0x119DA) && (ch <= 0x119DB))
            || (ch == 0x119E0)
            || ((ch >= 0x11A01) && (ch <= 0x11A0A))
            || ((ch >= 0x11A33) && (ch <= 0x11A38))
            || ((ch >= 0x11A3B) && (ch <= 0x11A3E))
            || (ch == 0x11A47)
            || ((ch >= 0x11A51) && (ch <= 0x11A56))
            || ((ch >= 0x11A59) && (ch <= 0x11A5B))
            || ((ch >= 0x11A8A) && (ch <= 0x11A96))
            || ((ch >= 0x11A98) && (ch <= 0x11A99))
            || (ch == 0x11B60)
            || ((ch >= 0x11B62) && (ch <= 0x11B64))
            || (ch == 0x11B66)
            || ((ch >= 0x11C30) && (ch <= 0x11C36))
            || ((ch >= 0x11C38) && (ch <= 0x11C3D))
            || (ch == 0x11C3F)
            || ((ch >= 0x11C92) && (ch <= 0x11CA7))
            || ((ch >= 0x11CAA) && (ch <= 0x11CB0))
            || ((ch >= 0x11CB2) && (ch <= 0x11CB3))
            || ((ch >= 0x11CB5) && (ch <= 0x11CB6))
            || ((ch >= 0x11D31) && (ch <= 0x11D36))
            || (ch == 0x11D3A)
            || ((ch >= 0x11D3C) && (ch <= 0x11D3D))
            || ((ch >= 0x11D3F) && (ch <= 0x11D45))
            || (ch == 0x11D47)
            || ((ch >= 0x11D90) && (ch <= 0x11D91))
            || (ch == 0x11D95)
            || (ch == 0x11D97)
            || ((ch >= 0x11EF3) && (ch <= 0x11EF4))
            || ((ch >= 0x11F00) && (ch <= 0x11F01))
            || ((ch >= 0x11F36) && (ch <= 0x11F3A))
            || (ch == 0x11F40)
            || (ch == 0x11F41)
            || (ch == 0x11F42)
            || (ch == 0x11F5A)
            || (ch == 0x13440)
            || ((ch >= 0x13447) && (ch <= 0x13455))
            || ((ch >= 0x1611E) && (ch <= 0x16129))
            || ((ch >= 0x1612D) && (ch <= 0x1612F))
            || ((ch >= 0x16AF0) && (ch <= 0x16AF4))
            || ((ch >= 0x16B30) && (ch <= 0x16B36))
            || (ch == 0x16F4F)
            || ((ch >= 0x16F8F) && (ch <= 0x16F92))
            || (ch == 0x16FE4)
            || ((ch >= 0x16FF0) && (ch <= 0x16FF1))
            || ((ch >= 0x1BC9D) && (ch <= 0x1BC9E))
            || ((ch >= 0x1CF00) && (ch <= 0x1CF2D))
            || ((ch >= 0x1CF30) && (ch <= 0x1CF46))
            || ((ch >= 0x1D165) && (ch <= 0x1D166))
            || ((ch >= 0x1D167) && (ch <= 0x1D169))
            || ((ch >= 0x1D16D) && (ch <= 0x1D172))
            || ((ch >= 0x1D17B) && (ch <= 0x1D182))
            || ((ch >= 0x1D185) && (ch <= 0x1D18B))
            || ((ch >= 0x1D1AA) && (ch <= 0x1D1AD))
            || ((ch >= 0x1D242) && (ch <= 0x1D244))
            || ((ch >= 0x1DA00) && (ch <= 0x1DA36))
            || ((ch >= 0x1DA3B) && (ch <= 0x1DA6C))
            || (ch == 0x1DA75)
            || (ch == 0x1DA84)
            || ((ch >= 0x1DA9B) && (ch <= 0x1DA9F))
            || ((ch >= 0x1DAA1) && (ch <= 0x1DAAF))
            || ((ch >= 0x1E000) && (ch <= 0x1E006))
            || ((ch >= 0x1E008) && (ch <= 0x1E018))
            || ((ch >= 0x1E01B) && (ch <= 0x1E021))
            || ((ch >= 0x1E023) && (ch <= 0x1E024))
            || ((ch >= 0x1E026) && (ch <= 0x1E02A))
            || (ch == 0x1E08F)
            || ((ch >= 0x1E130) && (ch <= 0x1E136))
            || (ch == 0x1E2AE)
            || ((ch >= 0x1E2EC) && (ch <= 0x1E2EF))
            || ((ch >= 0x1E4EC) && (ch <= 0x1E4EF))
            || ((ch >= 0x1E5EE) && (ch <= 0x1E5EF))
            || (ch == 0x1E6E3)
            || (ch == 0x1E6E6)
            || ((ch >= 0x1E6EE) && (ch <= 0x1E6EF))
            || (ch == 0x1E6F5)
            || ((ch >= 0x1E8D0) && (ch <= 0x1E8D6))
            || ((ch >= 0x1E944) && (ch <= 0x1E94A))
            || ((ch >= 0x1F3FB) && (ch <= 0x1F3FF))
            || ((ch >= 0xE0020) && (ch <= 0xE007F))
            || ((ch >= 0xE0100) && (ch <= 0xE01EF))
        ) {
            return true;
        }
        return false;
    }

    /**
     * Check if codepoint has the SpacingMark Grapheme_Cluster_Break
     * property.
     *
     * @param ch character to check
     * @return true if SpacingMark
     */
    public static boolean isSpacingMark(final int ch) {
        if ((ch == 0x0903)
            || (ch == 0x093B)
            || ((ch >= 0x093E) && (ch <= 0x0940))
            || ((ch >= 0x0949) && (ch <= 0x094C))
            || ((ch >= 0x094E) && (ch <= 0x094F))
            || ((ch >= 0x0982) && (ch <= 0x0983))
            || ((ch >= 0x09BF) && (ch <= 0x09C0))
            || ((ch >= 0x09C7) && (ch <= 0x09C8))
            || ((ch >= 0x09CB) && (ch <= 0x09CC))
            || (ch == 0x0A03)
            || ((ch >= 0x0A3E) && (ch <= 0x0A40))
            || (ch == 0x0A83)
            || ((ch >= 0x0ABE) && (ch <= 0x0AC0))
            || (ch == 0x0AC9)
            || ((ch >= 0x0ACB) && (ch <= 0x0ACC))
            || ((ch >= 0x0B02) && (ch <= 0x0B03))
            || (ch == 0x0B40)
            || ((ch >= 0x0B47) && (ch <= 0x0B48))
            || ((ch >= 0x0B4B) && (ch <= 0x0B4C))
            || (ch == 0x0BBF)
            || ((ch >= 0x0BC1) && (ch <= 0x0BC2))
            || ((ch >= 0x0BC6) && (ch <= 0x0BC8))
            || ((ch >= 0x0BCA) && (ch <= 0x0BCC))
            || ((ch >= 0x0C01) && (ch <= 0x0C03))
            || ((ch >= 0x0C41) && (ch <= 0x0C44))
            || ((ch >= 0x0C82) && (ch <= 0x0C83))
            || (ch == 0x0CBE)
            || (ch == 0x0CC1)
            || ((ch >= 0x0CC3) && (ch <= 0x0CC4))
            || (ch == 0x0CF3)
            || ((ch >= 0x0D02) && (ch <= 0x0D03))
            || ((ch >= 0x0D3F) && (ch <= 0x0D40))
            || ((ch >= 0x0D46) && (ch <= 0x0D48))
            || ((ch >= 0x0D4A) && (ch <= 0x0D4C))
            || ((ch >= 0x0D82) && (ch <= 0x0D83))
            || ((ch >= 0x0DD0) && (ch <= 0x0DD1))
            || ((ch >= 0x0DD8) && (ch <= 0x0DDE))
            || ((ch >= 0x0DF2) && (ch <= 0x0DF3))
            || (ch == 0x0E33)
            || (ch == 0x0EB3)
            || ((ch >= 0x0F3E) && (ch <= 0x0F3F))
            || (ch == 0x0F7F)
            || (ch == 0x1031)
            || ((ch >= 0x103B) && (ch <= 0x103C))
            || ((ch >= 0x1056) && (ch <= 0x1057))
            || (ch == 0x1084)
            || (ch == 0x17B6)
            || ((ch >= 0x17BE) && (ch <= 0x17C5))
            || ((ch >= 0x17C7) && (ch <= 0x17C8))
            || ((ch >= 0x1923) && (ch <= 0x1926))
            || ((ch >= 0x1929) && (ch <= 0x192B))
            || ((ch >= 0x1930) && (ch <= 0x1931))
            || ((ch >= 0x1933) && (ch <= 0x1938))
            || ((ch >= 0x1A19) && (ch <= 0x1A1A))
            || (ch == 0x1A55)
            || (ch == 0x1A57)
            || ((ch >= 0x1A6D) && (ch <= 0x1A72))
            || (ch == 0x1B04)
            || ((ch >= 0x1B3E) && (ch <= 0x1B41))
            || (ch == 0x1B82)
            || (ch == 0x1BA1)
            || ((ch >= 0x1BA6) && (ch <= 0x1BA7))
            || (ch == 0x1BE7)
            || ((ch >= 0x1BEA) && (ch <= 0x1BEC))
            || (ch == 0x1BEE)
            || ((ch >= 0x1C24) && (ch <= 0x1C2B))
            || ((ch >= 0x1C34) && (ch <= 0x1C35))
            || (ch == 0x1CE1)
            || (ch == 0x1CF7)
            || ((ch >= 0xA823) && (ch <= 0xA824))
            || (ch == 0xA827)
            || ((ch >= 0xA880) && (ch <= 0xA881))
            || ((ch >= 0xA8B4) && (ch <= 0xA8C3))
            || (ch == 0xA952)
            || (ch == 0xA983)
            || ((ch >= 0xA9B4) && (ch <= 0xA9B5))
            || ((ch >= 0xA9BA) && (ch <= 0xA9BB))
            || ((ch >= 0xA9BE) && (ch <= 0xA9BF))
            || ((ch >= 0xAA2F) && (ch <= 0xAA30))
            || ((ch >= 0xAA33) && (ch <= 0xAA34))
            || (ch == 0xAA4D)
            || (ch == 0xAAEB)
            || ((ch >= 0xAAEE) && (ch <= 0xAAEF))
            || (ch == 0xAAF5)
            || ((ch >= 0xABE3) && (ch <= 0xABE4))
            || ((ch >= 0xABE6) && (ch <= 0xABE7))
            || ((ch >= 0xABE9) && (ch <= 0xABEA))
            || (ch == 0xABEC)
            || (ch == 0x11000)
            || (ch == 0x11002)
            || (ch == 0x11082)
            || ((ch >= 0x110B0) && (ch <= 0x110B2))
            || ((ch >= 0x110B7) && (ch <= 0x110B8))
            || (ch == 0x1112C)
            || ((ch >= 0x11145) && (ch <= 0x11146))
            || (ch == 0x11182)
            || ((ch >= 0x111B3) && (ch <= 0x111B5))
            || (ch == 0x111BF)
            || (ch == 0x111CE)
            || ((ch >= 0x1122C) && (ch <= 0x1122E))
            || ((ch >= 0x11232) && (ch <= 0x11233))
            || ((ch >= 0x112E0) && (ch <= 0x112E2))
            || ((ch >= 0x11302) && (ch <= 0x11303))
            || (ch == 0x1133F)
            || ((ch >= 0x11341) && (ch <= 0x11344))
            || ((ch >= 0x11347) && (ch <= 0x11348))
            || ((ch >= 0x1134B) && (ch <= 0x1134C))
            || ((ch >= 0x11362) && (ch <= 0x11363))
            || ((ch >= 0x113B9) && (ch <= 0x113BA))
            || (ch == 0x113CA)
            || ((ch >= 0x113CC) && (ch <= 0x113CD))
            || ((ch >= 0x11435) && (ch <= 0x11437))
            || ((ch >= 0x11440) && (ch <= 0x11441))
            || (ch == 0x11445)
            || ((ch >= 0x114B1) && (ch <= 0x114B2))
            || (ch == 0x114B9)
            || ((ch >= 0x114BB) && (ch <= 0x114BC))
            || (ch == 0x114BE)
            || (ch == 0x114C1)
            || ((ch >= 0x115B0) && (ch <= 0x115B1))
            || ((ch >= 0x115B8) && (ch <= 0x115BB))
            || (ch == 0x115BE)
            || ((ch >= 0x11630) && (ch <= 0x11632))
            || ((ch >= 0x1163B) && (ch <= 0x1163C))
            || (ch == 0x1163E)
            || (ch == 0x116AC)
            || ((ch >= 0x116AE) && (ch <= 0x116AF))
            || (ch == 0x1171E)
            || (ch == 0x11726)
            || ((ch >= 0x1182C) && (ch <= 0x1182E))
            || (ch == 0x11838)
            || ((ch >= 0x11931) && (ch <= 0x11935))
            || ((ch >= 0x11937) && (ch <= 0x11938))
            || (ch == 0x11940)
            || (ch == 0x11942)
            || ((ch >= 0x119D1) && (ch <= 0x119D3))
            || ((ch >= 0x119DC) && (ch <= 0x119DF))
            || (ch == 0x119E4)
            || (ch == 0x11A39)
            || ((ch >= 0x11A57) && (ch <= 0x11A58))
            || (ch == 0x11A97)
            || (ch == 0x11B61)
            || (ch == 0x11B65)
            || (ch == 0x11B67)
            || (ch == 0x11C2F)
            || (ch == 0x11C3E)
            || (ch == 0x11CA9)
            || (ch == 0x11CB1)
            || (ch == 0x11CB4)
            || ((ch >= 0x11D8A) && (ch <= 0x11D8E))
            || ((ch >= 0x11D93) && (ch <= 0x11D94))
            || (ch == 0x11D96)
            || ((ch >= 0x11EF5) && (ch <= 0x11EF6))
            || (ch == 0x11F03)
            || ((ch >= 0x11F34) && (ch <= 0x11F35))
            || ((ch >= 0x11F3E) && (ch <= 0x11F3F))
            || ((ch >= 0x1612A) && (ch <= 0x1612C))
            || ((ch >= 0x16F51) && (ch <= 0x16F87))
        ) {
            return true;
        }
        return false;
    }

    /**
     * Check if codepoint has the L Grapheme_Cluster_Break property.
     *
     * @param ch character to check
     * @return true if L
     */
    public static boolean isL(final int ch) {
        if (((ch >= 0x1100) && (ch <= 0x115F))
            || ((ch >= 0xA960) && (ch <= 0xA97C))
        ) {
            return true;
        }
        return false;
    }

    /**
     * Check if codepoint has the V Grapheme_Cluster_Break property.
     *
     * @param ch character to check
     * @return true if V
     */
    public static boolean isV(final int ch) {
        if (((ch >= 0x1160) && (ch <= 0x11A7))
            || ((ch >= 0xD7B0) && (ch <= 0xD7C6))
            || (ch == 0x16D63)
            || ((ch >= 0x16D67) && (ch <= 0x16D6A))
        ) {
            return true;
        }
        return false;
    }

    /**
     * Check if codepoint has the T Grapheme_Cluster_Break property.
     *
     * @param ch character to check
     * @return true if T
     */
    public static boolean isT(final int ch) {
        if (((ch >= 0x11A8) && (ch <= 0x11FF))
            || ((ch >= 0xD7CB) && (ch <= 0xD7FB))
        ) {
            return true;
        }
        return false;
    }

    /**
     * Check if codepoint has the LV Grapheme_Cluster_Break property.
     *
     * @param ch character to check
     * @return true if LV
     */
    public static boolean isLV(final int ch) {
        if ((ch == 0xAC00)
            || (ch == 0xAC1C)
            || (ch == 0xAC38)
            || (ch == 0xAC54)
            || (ch == 0xAC70)
            || (ch == 0xAC8C)
            || (ch == 0xACA8)
            || (ch == 0xACC4)
            || (ch == 0xACE0)
            || (ch == 0xACFC)
            || (ch == 0xAD18)
            || (ch == 0xAD34)
            || (ch == 0xAD50)
            || (ch == 0xAD6C)
            || (ch == 0xAD88)
            || (ch == 0xADA4)
            || (ch == 0xADC0)
            || (ch == 0xADDC)
            || (ch == 0xADF8)
            || (ch == 0xAE14)
            || (ch == 0xAE30)
            || (ch == 0xAE4C)
            || (ch == 0xAE68)
            || (ch == 0xAE84)
            || (ch == 0xAEA0)
            || (ch == 0xAEBC)
            || (ch == 0xAED8)
            || (ch == 0xAEF4)
            || (ch == 0xAF10)
            || (ch == 0xAF2C)
            || (ch == 0xAF48)
            || (ch == 0xAF64)
            || (ch == 0xAF80)
            || (ch == 0xAF9C)
            || (ch == 0xAFB8)
            || (ch == 0xAFD4)
            || (ch == 0xAFF0)
            || (ch == 0xB00C)
            || (ch == 0xB028)
            || (ch == 0xB044)
            || (ch == 0xB060)
            || (ch == 0xB07C)
            || (ch == 0xB098)
            || (ch == 0xB0B4)
            || (ch == 0xB0D0)
            || (ch == 0xB0EC)
            || (ch == 0xB108)
            || (ch == 0xB124)
            || (ch == 0xB140)
            || (ch == 0xB15C)
            || (ch == 0xB178)
            || (ch == 0xB194)
            || (ch == 0xB1B0)
            || (ch == 0xB1CC)
            || (ch == 0xB1E8)
            || (ch == 0xB204)
            || (ch == 0xB220)
            || (ch == 0xB23C)
            || (ch == 0xB258)
            || (ch == 0xB274)
            || (ch == 0xB290)
            || (ch == 0xB2AC)
            || (ch == 0xB2C8)
            || (ch == 0xB2E4)
            || (ch == 0xB300)
            || (ch == 0xB31C)
            || (ch == 0xB338)
            || (ch == 0xB354)
            || (ch == 0xB370)
            || (ch == 0xB38C)
            || (ch == 0xB3A8)
            || (ch == 0xB3C4)
            || (ch == 0xB3E0)
            || (ch == 0xB3FC)
            || (ch == 0xB418)
            || (ch == 0xB434)
            || (ch == 0xB450)
            || (ch == 0xB46C)
            || (ch == 0xB488)
            || (ch == 0xB4A4)
            || (ch == 0xB4C0)
            || (ch == 0xB4DC)
            || (ch == 0xB4F8)
            || (ch == 0xB514)
            || (ch == 0xB530)
            || (ch == 0xB54C)
            || (ch == 0xB568)
            || (ch == 0xB584)
            || (ch == 0xB5A0)
            || (ch == 0xB5BC)
            || (ch == 0xB5D8)
            || (ch == 0xB5F4)
            || (ch == 0xB610)
            || (ch == 0xB62C)
            || (ch == 0xB648)
            || (ch == 0xB664)
            || (ch == 0xB680)
            || (ch == 0xB69C)
            || (ch == 0xB6B8)
            || (ch == 0xB6D4)
            || (ch == 0xB6F0)
            || (ch == 0xB70C)
            || (ch == 0xB728)
            || (ch == 0xB744)
            || (ch == 0xB760)
            || (ch == 0xB77C)
            || (ch == 0xB798)
            || (ch == 0xB7B4)
            || (ch == 0xB7D0)
            || (ch == 0xB7EC)
            || (ch == 0xB808)
            || (ch == 0xB824)
            || (ch == 0xB840)
            || (ch == 0xB85C)
            || (ch == 0xB878)
            || (ch == 0xB894)
            || (ch == 0xB8B0)
            || (ch == 0xB8CC)
            || (ch == 0xB8E8)
            || (ch == 0xB904)
            || (ch == 0xB920)
            || (ch == 0xB93C)
            || (ch == 0xB958)
            || (ch == 0xB974)
            || (ch == 0xB990)
            || (ch == 0xB9AC)
            || (ch == 0xB9C8)
            || (ch == 0xB9E4)
            || (ch == 0xBA00)
            || (ch == 0xBA1C)
            || (ch == 0xBA38)
            || (ch == 0xBA54)
            || (ch == 0xBA70)
            || (ch == 0xBA8C)
            || (ch == 0xBAA8)
            || (ch == 0xBAC4)
            || (ch == 0xBAE0)
            || (ch == 0xBAFC)
            || (ch == 0xBB18)
            || (ch == 0xBB34)
            || (ch == 0xBB50)
            || (ch == 0xBB6C)
            || (ch == 0xBB88)
            || (ch == 0xBBA4)
            || (ch == 0xBBC0)
            || (ch == 0xBBDC)
            || (ch == 0xBBF8)
            || (ch == 0xBC14)
            || (ch == 0xBC30)
            || (ch == 0xBC4C)
            || (ch == 0xBC68)
            || (ch == 0xBC84)
            || (ch == 0xBCA0)
            || (ch == 0xBCBC)
            || (ch == 0xBCD8)
            || (ch == 0xBCF4)
            || (ch == 0xBD10)
            || (ch == 0xBD2C)
            || (ch == 0xBD48)
            || (ch == 0xBD64)
            || (ch == 0xBD80)
            || (ch == 0xBD9C)
            || (ch == 0xBDB8)
            || (ch == 0xBDD4)
            || (ch == 0xBDF0)
            || (ch == 0xBE0C)
            || (ch == 0xBE28)
            || (ch == 0xBE44)
            || (ch == 0xBE60)
            || (ch == 0xBE7C)
            || (ch == 0xBE98)
            || (ch == 0xBEB4)
            || (ch == 0xBED0)
            || (ch == 0xBEEC)
            || (ch == 0xBF08)
            || (ch == 0xBF24)
            || (ch == 0xBF40)
            || (ch == 0xBF5C)
            || (ch == 0xBF78)
            || (ch == 0xBF94)
            || (ch == 0xBFB0)
            || (ch == 0xBFCC)
            || (ch == 0xBFE8)
            || (ch == 0xC004)
            || (ch == 0xC020)
            || (ch == 0xC03C)
            || (ch == 0xC058)
            || (ch == 0xC074)
            || (ch == 0xC090)
            || (ch == 0xC0AC)
            || (ch == 0xC0C8)
            || (ch == 0xC0E4)
            || (ch == 0xC100)
            || (ch == 0xC11C)
            || (ch == 0xC138)
            || (ch == 0xC154)
            || (ch == 0xC170)
            || (ch == 0xC18C)
            || (ch == 0xC1A8)
            || (ch == 0xC1C4)
            || (ch == 0xC1E0)
            || (ch == 0xC1FC)
            || (ch == 0xC218)
            || (ch == 0xC234)
            || (ch == 0xC250)
            || (ch == 0xC26C)
            || (ch == 0xC288)
            || (ch == 0xC2A4)
            || (ch == 0xC2C0)
            || (ch == 0xC2DC)
            || (ch == 0xC2F8)
            || (ch == 0xC314)
            || (ch == 0xC330)
            || (ch == 0xC34C)
            || (ch == 0xC368)
            || (ch == 0xC384)
            || (ch == 0xC3A0)
            || (ch == 0xC3BC)
            || (ch == 0xC3D8)
            || (ch == 0xC3F4)
            || (ch == 0xC410)
            || (ch == 0xC42C)
            || (ch == 0xC448)
            || (ch == 0xC464)
            || (ch == 0xC480)
            || (ch == 0xC49C)
            || (ch == 0xC4B8)
            || (ch == 0xC4D4)
            || (ch == 0xC4F0)
            || (ch == 0xC50C)
            || (ch == 0xC528)
            || (ch == 0xC544)
            || (ch == 0xC560)
            || (ch == 0xC57C)
            || (ch == 0xC598)
            || (ch == 0xC5B4)
            || (ch == 0xC5D0)
            || (ch == 0xC5EC)
            || (ch == 0xC608)
            || (ch == 0xC624)
            || (ch == 0xC640)
            || (ch == 0xC65C)
            || (ch == 0xC678)
            || (ch == 0xC694)
            || (ch == 0xC6B0)
            || (ch == 0xC6CC)
            || (ch == 0xC6E8)
            || (ch == 0xC704)
            || (ch == 0xC720)
            || (ch == 0xC73C)
            || (ch == 0xC758)
            || (ch == 0xC774)
            || (ch == 0xC790)
            || (ch == 0xC7AC)
            || (ch == 0xC7C8)
            || (ch == 0xC7E4)
            || (ch == 0xC800)
            || (ch == 0xC81C)
            || (ch == 0xC838)
            || (ch == 0xC854)
            || (ch == 0xC870)
            || (ch == 0xC88C)
            || (ch == 0xC8A8)
            || (ch == 0xC8C4)
            || (ch == 0xC8E0)
            || (ch == 0xC8FC)
            || (ch == 0xC918)
            || (ch == 0xC934)
            || (ch == 0xC950)
            || (ch == 0xC96C)
            || (ch == 0xC988)
            || (ch == 0xC9A4)
            || (ch == 0xC9C0)
            || (ch == 0xC9DC)
            || (ch == 0xC9F8)
            || (ch == 0xCA14)
            || (ch == 0xCA30)
            || (ch == 0xCA4C)
            || (ch == 0xCA68)
            || (ch == 0xCA84)
            || (ch == 0xCAA0)
            || (ch == 0xCABC)
            || (ch == 0xCAD8)
            || (ch == 0xCAF4)
            || (ch == 0xCB10)
            || (ch == 0xCB2C)
            || (ch == 0xCB48)
            || (ch == 0xCB64)
            || (ch == 0xCB80)
            || (ch == 0xCB9C)
            || (ch == 0xCBB8)
            || (ch == 0xCBD4)
            || (ch == 0xCBF0)
            || (ch == 0xCC0C)
            || (ch == 0xCC28)
            || (ch == 0xCC44)
            || (ch == 0xCC60)
            || (ch == 0xCC7C)
            || (ch == 0xCC98)
            || (ch == 0xCCB4)
            || (ch == 0xCCD0)
            || (ch == 0xCCEC)
            || (ch == 0xCD08)
            || (ch == 0xCD24)
            || (ch == 0xCD40)
            || (ch == 0xCD5C)
            || (ch == 0xCD78)
            || (ch == 0xCD94)
            || (ch == 0xCDB0)
            || (ch == 0xCDCC)
            || (ch == 0xCDE8)
            || (ch == 0xCE04)
            || (ch == 0xCE20)
            || (ch == 0xCE3C)
            || (ch == 0xCE58)
            || (ch == 0xCE74)
            || (ch == 0xCE90)
            || (ch == 0xCEAC)
            || (ch == 0xCEC8)
            || (ch == 0xCEE4)
            || (ch == 0xCF00)
            || (ch == 0xCF1C)
            || (ch == 0xCF38)
            || (ch == 0xCF54)
            || (ch == 0xCF70)
            || (ch == 0xCF8C)
            || (ch == 0xCFA8)
            || (ch == 0xCFC4)
            || (ch == 0xCFE0)
            || (ch == 0xCFFC)
            || (ch == 0xD018)
            || (ch == 0xD034)
            || (ch == 0xD050)
            || (ch == 0xD06C)
            || (ch == 0xD088)
            || (ch == 0xD0A4)
            || (ch == 0xD0C0)
            || (ch == 0xD0DC)
            || (ch == 0xD0F8)
            || (ch == 0xD114)
            || (ch == 0xD130)
            || (ch == 0xD14C)
            || (ch == 0xD168)
            || (ch == 0xD184)
            || (ch == 0xD1A0)
            || (ch == 0xD1BC)
            || (ch == 0xD1D8)
            || (ch == 0xD1F4)
            || (ch == 0xD210)
            || (ch == 0xD22C)
            || (ch == 0xD248)
            || (ch == 0xD264)
            || (ch == 0xD280)
            || (ch == 0xD29C)
            || (ch == 0xD2B8)
            || (ch == 0xD2D4)
            || (ch == 0xD2F0)
            || (ch == 0xD30C)
            || (ch == 0xD328)
            || (ch == 0xD344)
            || (ch == 0xD360)
            || (ch == 0xD37C)
            || (ch == 0xD398)
            || (ch == 0xD3B4)
            || (ch == 0xD3D0)
            || (ch == 0xD3EC)
            || (ch == 0xD408)
            || (ch == 0xD424)
            || (ch == 0xD440)
            || (ch == 0xD45C)
            || (ch == 0xD478)
            || (ch == 0xD494)
            || (ch == 0xD4B0)
            || (ch == 0xD4CC)
            || (ch == 0xD4E8)
            || (ch == 0xD504)
            || (ch == 0xD520)
            || (ch == 0xD53C)
            || (ch == 0xD558)
            || (ch == 0xD574)
            || (ch == 0xD590)
            || (ch == 0xD5AC)
            || (ch == 0xD5C8)
            || (ch == 0xD5E4)
            || (ch == 0xD600)
            || (ch == 0xD61C)
            || (ch == 0xD638)
            || (ch == 0xD654)
            || (ch == 0xD670)
            || (ch == 0xD68C)
            || (ch == 0xD6A8)
            || (ch == 0xD6C4)
            || (ch == 0xD6E0)
            || (ch == 0xD6FC)
            || (ch == 0xD718)
            || (ch == 0xD734)
            || (ch == 0xD750)
            || (ch == 0xD76C)
            || (ch == 0xD788)
        ) {
            return true;
        }
        return false;
    }

    /**
     * Check if codepoint has the LVT Grapheme_Cluster_Break property.
     *
     * @param ch character to check
     * @return true if LVT
     */
    public static boolean isLVT(final int ch) {
        if (((ch >= 0xAC01) && (ch <= 0xAC1B))
            || ((ch >= 0xAC1D) && (ch <= 0xAC37))
            || ((ch >= 0xAC39) && (ch <= 0xAC53))
            || ((ch >= 0xAC55) && (ch <= 0xAC6F))
            || ((ch >= 0xAC71) && (ch <= 0xAC8B))
            || ((ch >= 0xAC8D) && (ch <= 0xACA7))
            || ((ch >= 0xACA9) && (ch <= 0xACC3))
            || ((ch >= 0xACC5) && (ch <= 0xACDF))
            || ((ch >= 0xACE1) && (ch <= 0xACFB))
            || ((ch >= 0xACFD) && (ch <= 0xAD17))
            || ((ch >= 0xAD19) && (ch <= 0xAD33))
            || ((ch >= 0xAD35) && (ch <= 0xAD4F))
            || ((ch >= 0xAD51) && (ch <= 0xAD6B))
            || ((ch >= 0xAD6D) && (ch <= 0xAD87))
            || ((ch >= 0xAD89) && (ch <= 0xADA3))
            || ((ch >= 0xADA5) && (ch <= 0xADBF))
            || ((ch >= 0xADC1) && (ch <= 0xADDB))
            || ((ch >= 0xADDD) && (ch <= 0xADF7))
            || ((ch >= 0xADF9) && (ch <= 0xAE13))
            || ((ch >= 0xAE15) && (ch <= 0xAE2F))
            || ((ch >= 0xAE31) && (ch <= 0xAE4B))
            || ((ch >= 0xAE4D) && (ch <= 0xAE67))
            || ((ch >= 0xAE69) && (ch <= 0xAE83))
            || ((ch >= 0xAE85) && (ch <= 0xAE9F))
            || ((ch >= 0xAEA1) && (ch <= 0xAEBB))
            || ((ch >= 0xAEBD) && (ch <= 0xAED7))
            || ((ch >= 0xAED9) && (ch <= 0xAEF3))
            || ((ch >= 0xAEF5) && (ch <= 0xAF0F))
            || ((ch >= 0xAF11) && (ch <= 0xAF2B))
            || ((ch >= 0xAF2D) && (ch <= 0xAF47))
            || ((ch >= 0xAF49) && (ch <= 0xAF63))
            || ((ch >= 0xAF65) && (ch <= 0xAF7F))
            || ((ch >= 0xAF81) && (ch <= 0xAF9B))
            || ((ch >= 0xAF9D) && (ch <= 0xAFB7))
            || ((ch >= 0xAFB9) && (ch <= 0xAFD3))
            || ((ch >= 0xAFD5) && (ch <= 0xAFEF))
            || ((ch >= 0xAFF1) && (ch <= 0xB00B))
            || ((ch >= 0xB00D) && (ch <= 0xB027))
            || ((ch >= 0xB029) && (ch <= 0xB043))
            || ((ch >= 0xB045) && (ch <= 0xB05F))
            || ((ch >= 0xB061) && (ch <= 0xB07B))
            || ((ch >= 0xB07D) && (ch <= 0xB097))
            || ((ch >= 0xB099) && (ch <= 0xB0B3))
            || ((ch >= 0xB0B5) && (ch <= 0xB0CF))
            || ((ch >= 0xB0D1) && (ch <= 0xB0EB))
            || ((ch >= 0xB0ED) && (ch <= 0xB107))
            || ((ch >= 0xB109) && (ch <= 0xB123))
            || ((ch >= 0xB125) && (ch <= 0xB13F))
            || ((ch >= 0xB141) && (ch <= 0xB15B))
            || ((ch >= 0xB15D) && (ch <= 0xB177))
            || ((ch >= 0xB179) && (ch <= 0xB193))
            || ((ch >= 0xB195) && (ch <= 0xB1AF))
            || ((ch >= 0xB1B1) && (ch <= 0xB1CB))
            || ((ch >= 0xB1CD) && (ch <= 0xB1E7))
            || ((ch >= 0xB1E9) && (ch <= 0xB203))
            || ((ch >= 0xB205) && (ch <= 0xB21F))
            || ((ch >= 0xB221) && (ch <= 0xB23B))
            || ((ch >= 0xB23D) && (ch <= 0xB257))
            || ((ch >= 0xB259) && (ch <= 0xB273))
            || ((ch >= 0xB275) && (ch <= 0xB28F))
            || ((ch >= 0xB291) && (ch <= 0xB2AB))
            || ((ch >= 0xB2AD) && (ch <= 0xB2C7))
            || ((ch >= 0xB2C9) && (ch <= 0xB2E3))
            || ((ch >= 0xB2E5) && (ch <= 0xB2FF))
            || ((ch >= 0xB301) && (ch <= 0xB31B))
            || ((ch >= 0xB31D) && (ch <= 0xB337))
            || ((ch >= 0xB339) && (ch <= 0xB353))
            || ((ch >= 0xB355) && (ch <= 0xB36F))
            || ((ch >= 0xB371) && (ch <= 0xB38B))
            || ((ch >= 0xB38D) && (ch <= 0xB3A7))
            || ((ch >= 0xB3A9) && (ch <= 0xB3C3))
            || ((ch >= 0xB3C5) && (ch <= 0xB3DF))
            || ((ch >= 0xB3E1) && (ch <= 0xB3FB))
            || ((ch >= 0xB3FD) && (ch <= 0xB417))
            || ((ch >= 0xB419) && (ch <= 0xB433))
            || ((ch >= 0xB435) && (ch <= 0xB44F))
            || ((ch >= 0xB451) && (ch <= 0xB46B))
            || ((ch >= 0xB46D) && (ch <= 0xB487))
            || ((ch >= 0xB489) && (ch <= 0xB4A3))
            || ((ch >= 0xB4A5) && (ch <= 0xB4BF))
            || ((ch >= 0xB4C1) && (ch <= 0xB4DB))
            || ((ch >= 0xB4DD) && (ch <= 0xB4F7))
            || ((ch >= 0xB4F9) && (ch <= 0xB513))
            || ((ch >= 0xB515) && (ch <= 0xB52F))
            || ((ch >= 0xB531) && (ch <= 0xB54B))
            || ((ch >= 0xB54D) && (ch <= 0xB567))
            || ((ch >= 0xB569) && (ch <= 0xB583))
            || ((ch >= 0xB585) && (ch <= 0xB59F))
            || ((ch >= 0xB5A1) && (ch <= 0xB5BB))
            || ((ch >= 0xB5BD) && (ch <= 0xB5D7))
            || ((ch >= 0xB5D9) && (ch <= 0xB5F3))
            || ((ch >= 0xB5F5) && (ch <= 0xB60F))
            || ((ch >= 0xB611) && (ch <= 0xB62B))
            || ((ch >= 0xB62D) && (ch <= 0xB647))
            || ((ch >= 0xB649) && (ch <= 0xB663))
            || ((ch >= 0xB665) && (ch <= 0xB67F))
            || ((ch >= 0xB681) && (ch <= 0xB69B))
            || ((ch >= 0xB69D) && (ch <= 0xB6B7))
            || ((ch >= 0xB6B9) && (ch <= 0xB6D3))
            || ((ch >= 0xB6D5) && (ch <= 0xB6EF))
            || ((ch >= 0xB6F1) && (ch <= 0xB70B))
            || ((ch >= 0xB70D) && (ch <= 0xB727))
            || ((ch >= 0xB729) && (ch <= 0xB743))
            || ((ch >= 0xB745) && (ch <= 0xB75F))
            || ((ch >= 0xB761) && (ch <= 0xB77B))
            || ((ch >= 0xB77D) && (ch <= 0xB797))
            || ((ch >= 0xB799) && (ch <= 0xB7B3))
            || ((ch >= 0xB7B5) && (ch <= 0xB7CF))
            || ((ch >= 0xB7D1) && (ch <= 0xB7EB))
            || ((ch >= 0xB7ED) && (ch <= 0xB807))
            || ((ch >= 0xB809) && (ch <= 0xB823))
            || ((ch >= 0xB825) && (ch <= 0xB83F))
            || ((ch >= 0xB841) && (ch <= 0xB85B))
            || ((ch >= 0xB85D) && (ch <= 0xB877))
            || ((ch >= 0xB879) && (ch <= 0xB893))
            || ((ch >= 0xB895) && (ch <= 0xB8AF))
            || ((ch >= 0xB8B1) && (ch <= 0xB8CB))
            || ((ch >= 0xB8CD) && (ch <= 0xB8E7))
            || ((ch >= 0xB8E9) && (ch <= 0xB903))
            || ((ch >= 0xB905) && (ch <= 0xB91F))
            || ((ch >= 0xB921) && (ch <= 0xB93B))
            || ((ch >= 0xB93D) && (ch <= 0xB957))
            || ((ch >= 0xB959) && (ch <= 0xB973))
            || ((ch >= 0xB975) && (ch <= 0xB98F))
            || ((ch >= 0xB991) && (ch <= 0xB9AB))
            || ((ch >= 0xB9AD) && (ch <= 0xB9C7))
            || ((ch >= 0xB9C9) && (ch <= 0xB9E3))
            || ((ch >= 0xB9E5) && (ch <= 0xB9FF))
            || ((ch >= 0xBA01) && (ch <= 0xBA1B))
            || ((ch >= 0xBA1D) && (ch <= 0xBA37))
            || ((ch >= 0xBA39) && (ch <= 0xBA53))
            || ((ch >= 0xBA55) && (ch <= 0xBA6F))
            || ((ch >= 0xBA71) && (ch <= 0xBA8B))
            || ((ch >= 0xBA8D) && (ch <= 0xBAA7))
            || ((ch >= 0xBAA9) && (ch <= 0xBAC3))
            || ((ch >= 0xBAC5) && (ch <= 0xBADF))
            || ((ch >= 0xBAE1) && (ch <= 0xBAFB))
            || ((ch >= 0xBAFD) && (ch <= 0xBB17))
            || ((ch >= 0xBB19) && (ch <= 0xBB33))
            || ((ch >= 0xBB35) && (ch <= 0xBB4F))
            || ((ch >= 0xBB51) && (ch <= 0xBB6B))
            || ((ch >= 0xBB6D) && (ch <= 0xBB87))
            || ((ch >= 0xBB89) && (ch <= 0xBBA3))
            || ((ch >= 0xBBA5) && (ch <= 0xBBBF))
            || ((ch >= 0xBBC1) && (ch <= 0xBBDB))
            || ((ch >= 0xBBDD) && (ch <= 0xBBF7))
            || ((ch >= 0xBBF9) && (ch <= 0xBC13))
            || ((ch >= 0xBC15) && (ch <= 0xBC2F))
            || ((ch >= 0xBC31) && (ch <= 0xBC4B))
            || ((ch >= 0xBC4D) && (ch <= 0xBC67))
            || ((ch >= 0xBC69) && (ch <= 0xBC83))
            || ((ch >= 0xBC85) && (ch <= 0xBC9F))
            || ((ch >= 0xBCA1) && (ch <= 0xBCBB))
            || ((ch >= 0xBCBD) && (ch <= 0xBCD7))
            || ((ch >= 0xBCD9) && (ch <= 0xBCF3))
            || ((ch >= 0xBCF5) && (ch <= 0xBD0F))
            || ((ch >= 0xBD11) && (ch <= 0xBD2B))
            || ((ch >= 0xBD2D) && (ch <= 0xBD47))
            || ((ch >= 0xBD49) && (ch <= 0xBD63))
            || ((ch >= 0xBD65) && (ch <= 0xBD7F))
            || ((ch >= 0xBD81) && (ch <= 0xBD9B))
            || ((ch >= 0xBD9D) && (ch <= 0xBDB7))
            || ((ch >= 0xBDB9) && (ch <= 0xBDD3))
            || ((ch >= 0xBDD5) && (ch <= 0xBDEF))
            || ((ch >= 0xBDF1) && (ch <= 0xBE0B))
            || ((ch >= 0xBE0D) && (ch <= 0xBE27))
            || ((ch >= 0xBE29) && (ch <= 0xBE43))
            || ((ch >= 0xBE45) && (ch <= 0xBE5F))
            || ((ch >= 0xBE61) && (ch <= 0xBE7B))
            || ((ch >= 0xBE7D) && (ch <= 0xBE97))
            || ((ch >= 0xBE99) && (ch <= 0xBEB3))
            || ((ch >= 0xBEB5) && (ch <= 0xBECF))
            || ((ch >= 0xBED1) && (ch <= 0xBEEB))
            || ((ch >= 0xBEED) && (ch <= 0xBF07))
            || ((ch >= 0xBF09) && (ch <= 0xBF23))
            || ((ch >= 0xBF25) && (ch <= 0xBF3F))
            || ((ch >= 0xBF41) && (ch <= 0xBF5B))
            || ((ch >= 0xBF5D) && (ch <= 0xBF77))
            || ((ch >= 0xBF79) && (ch <= 0xBF93))
            || ((ch >= 0xBF95) && (ch <= 0xBFAF))
            || ((ch >= 0xBFB1) && (ch <= 0xBFCB))
            || ((ch >= 0xBFCD) && (ch <= 0xBFE7))
            || ((ch >= 0xBFE9) && (ch <= 0xC003))
            || ((ch >= 0xC005) && (ch <= 0xC01F))
            || ((ch >= 0xC021) && (ch <= 0xC03B))
            || ((ch >= 0xC03D) && (ch <= 0xC057))
            || ((ch >= 0xC059) && (ch <= 0xC073))
            || ((ch >= 0xC075) && (ch <= 0xC08F))
            || ((ch >= 0xC091) && (ch <= 0xC0AB))
            || ((ch >= 0xC0AD) && (ch <= 0xC0C7))
            || ((ch >= 0xC0C9) && (ch <= 0xC0E3))
            || ((ch >= 0xC0E5) && (ch <= 0xC0FF))
            || ((ch >= 0xC101) && (ch <= 0xC11B))
            || ((ch >= 0xC11D) && (ch <= 0xC137))
            || ((ch >= 0xC139) && (ch <= 0xC153))
            || ((ch >= 0xC155) && (ch <= 0xC16F))
            || ((ch >= 0xC171) && (ch <= 0xC18B))
            || ((ch >= 0xC18D) && (ch <= 0xC1A7))
            || ((ch >= 0xC1A9) && (ch <= 0xC1C3))
            || ((ch >= 0xC1C5) && (ch <= 0xC1DF))
            || ((ch >= 0xC1E1) && (ch <= 0xC1FB))
            || ((ch >= 0xC1FD) && (ch <= 0xC217))
            || ((ch >= 0xC219) && (ch <= 0xC233))
            || ((ch >= 0xC235) && (ch <= 0xC24F))
            || ((ch >= 0xC251) && (ch <= 0xC26B))
            || ((ch >= 0xC26D) && (ch <= 0xC287))
            || ((ch >= 0xC289) && (ch <= 0xC2A3))
            || ((ch >= 0xC2A5) && (ch <= 0xC2BF))
            || ((ch >= 0xC2C1) && (ch <= 0xC2DB))
            || ((ch >= 0xC2DD) && (ch <= 0xC2F7))
            || ((ch >= 0xC2F9) && (ch <= 0xC313))
            || ((ch >= 0xC315) && (ch <= 0xC32F))
            || ((ch >= 0xC331) && (ch <= 0xC34B))
            || ((ch >= 0xC34D) && (ch <= 0xC367))
            || ((ch >= 0xC369) && (ch <= 0xC383))
            || ((ch >= 0xC385) && (ch <= 0xC39F))
            || ((ch >= 0xC3A1) && (ch <= 0xC3BB))
            || ((ch >= 0xC3BD) && (ch <= 0xC3D7))
            || ((ch >= 0xC3D9) && (ch <= 0xC3F3))
            || ((ch >= 0xC3F5) && (ch <= 0xC40F))
            || ((ch >= 0xC411) && (ch <= 0xC42B))
            || ((ch >= 0xC42D) && (ch <= 0xC447))
            || ((ch >= 0xC449) && (ch <= 0xC463))
            || ((ch >= 0xC465) && (ch <= 0xC47F))
            || ((ch >= 0xC481) && (ch <= 0xC49B))
            || ((ch >= 0xC49D) && (ch <= 0xC4B7))
            || ((ch >= 0xC4B9) && (ch <= 0xC4D3))
            || ((ch >= 0xC4D5) && (ch <= 0xC4EF))
            || ((ch >= 0xC4F1) && (ch <= 0xC50B))
            || ((ch >= 0xC50D) && (ch <= 0xC527))
            || ((ch >= 0xC529) && (ch <= 0xC543))
            || ((ch >= 0xC545) && (ch <= 0xC55F))
            || ((ch >= 0xC561) && (ch <= 0xC57B))
            || ((ch >= 0xC57D) && (ch <= 0xC597))
            || ((ch >= 0xC599) && (ch <= 0xC5B3))
            || ((ch >= 0xC5B5) && (ch <= 0xC5CF))
            || ((ch >= 0xC5D1) && (ch <= 0xC5EB))
            || ((ch >= 0xC5ED) && (ch <= 0xC607))
            || ((ch >= 0xC609) && (ch <= 0xC623))
            || ((ch >= 0xC625) && (ch <= 0xC63F))
            || ((ch >= 0xC641) && (ch <= 0xC65B))
            || ((ch >= 0xC65D) && (ch <= 0xC677))
            || ((ch >= 0xC679) && (ch <= 0xC693))
            || ((ch >= 0xC695) && (ch <= 0xC6AF))
            || ((ch >= 0xC6B1) && (ch <= 0xC6CB))
            || ((ch >= 0xC6CD) && (ch <= 0xC6E7))
            || ((ch >= 0xC6E9) && (ch <= 0xC703))
            || ((ch >= 0xC705) && (ch <= 0xC71F))
            || ((ch >= 0xC721) && (ch <= 0xC73B))
            || ((ch >= 0xC73D) && (ch <= 0xC757))
            || ((ch >= 0xC759) && (ch <= 0xC773))
            || ((ch >= 0xC775) && (ch <= 0xC78F))
            || ((ch >= 0xC791) && (ch <= 0xC7AB))
            || ((ch >= 0xC7AD) && (ch <= 0xC7C7))
            || ((ch >= 0xC7C9) && (ch <= 0xC7E3))
            || ((ch >= 0xC7E5) && (ch <= 0xC7FF))
            || ((ch >= 0xC801) && (ch <= 0xC81B))
            || ((ch >= 0xC81D) && (ch <= 0xC837))
            || ((ch >= 0xC839) && (ch <= 0xC853))
            || ((ch >= 0xC855) && (ch <= 0xC86F))
            || ((ch >= 0xC871) && (ch <= 0xC88B))
            || ((ch >= 0xC88D) && (ch <= 0xC8A7))
            || ((ch >= 0xC8A9) && (ch <= 0xC8C3))
            || ((ch >= 0xC8C5) && (ch <= 0xC8DF))
            || ((ch >= 0xC8E1) && (ch <= 0xC8FB))
            || ((ch >= 0xC8FD) && (ch <= 0xC917))
            || ((ch >= 0xC919) && (ch <= 0xC933))
            || ((ch >= 0xC935) && (ch <= 0xC94F))
            || ((ch >= 0xC951) && (ch <= 0xC96B))
            || ((ch >= 0xC96D) && (ch <= 0xC987))
            || ((ch >= 0xC989) && (ch <= 0xC9A3))
            || ((ch >= 0xC9A5) && (ch <= 0xC9BF))
            || ((ch >= 0xC9C1) && (ch <= 0xC9DB))
            || ((ch >= 0xC9DD) && (ch <= 0xC9F7))
            || ((ch >= 0xC9F9) && (ch <= 0xCA13))
            || ((ch >= 0xCA15) && (ch <= 0xCA2F))
            || ((ch >= 0xCA31) && (ch <= 0xCA4B))
            || ((ch >= 0xCA4D) && (ch <= 0xCA67))
            || ((ch >= 0xCA69) && (ch <= 0xCA83))
            || ((ch >= 0xCA85) && (ch <= 0xCA9F))
            || ((ch >= 0xCAA1) && (ch <= 0xCABB))
            || ((ch >= 0xCABD) && (ch <= 0xCAD7))
            || ((ch >= 0xCAD9) && (ch <= 0xCAF3))
            || ((ch >= 0xCAF5) && (ch <= 0xCB0F))
            || ((ch >= 0xCB11) && (ch <= 0xCB2B))
            || ((ch >= 0xCB2D) && (ch <= 0xCB47))
            || ((ch >= 0xCB49) && (ch <= 0xCB63))
            || ((ch >= 0xCB65) && (ch <= 0xCB7F))
            || ((ch >= 0xCB81) && (ch <= 0xCB9B))
            || ((ch >= 0xCB9D) && (ch <= 0xCBB7))
            || ((ch >= 0xCBB9) && (ch <= 0xCBD3))
            || ((ch >= 0xCBD5) && (ch <= 0xCBEF))
            || ((ch >= 0xCBF1) && (ch <= 0xCC0B))
            || ((ch >= 0xCC0D) && (ch <= 0xCC27))
            || ((ch >= 0xCC29) && (ch <= 0xCC43))
            || ((ch >= 0xCC45) && (ch <= 0xCC5F))
            || ((ch >= 0xCC61) && (ch <= 0xCC7B))
            || ((ch >= 0xCC7D) && (ch <= 0xCC97))
            || ((ch >= 0xCC99) && (ch <= 0xCCB3))
            || ((ch >= 0xCCB5) && (ch <= 0xCCCF))
            || ((ch >= 0xCCD1) && (ch <= 0xCCEB))
            || ((ch >= 0xCCED) && (ch <= 0xCD07))
            || ((ch >= 0xCD09) && (ch <= 0xCD23))
            || ((ch >= 0xCD25) && (ch <= 0xCD3F))
            || ((ch >= 0xCD41) && (ch <= 0xCD5B))
            || ((ch >= 0xCD5D) && (ch <= 0xCD77))
            || ((ch >= 0xCD79) && (ch <= 0xCD93))
            || ((ch >= 0xCD95) && (ch <= 0xCDAF))
            || ((ch >= 0xCDB1) && (ch <= 0xCDCB))
            || ((ch >= 0xCDCD) && (ch <= 0xCDE7))
            || ((ch >= 0xCDE9) && (ch <= 0xCE03))
            || ((ch >= 0xCE05) && (ch <= 0xCE1F))
            || ((ch >= 0xCE21) && (ch <= 0xCE3B))
            || ((ch >= 0xCE3D) && (ch <= 0xCE57))
            || ((ch >= 0xCE59) && (ch <= 0xCE73))
            || ((ch >= 0xCE75) && (ch <= 0xCE8F))
            || ((ch >= 0xCE91) && (ch <= 0xCEAB))
            || ((ch >= 0xCEAD) && (ch <= 0xCEC7))
            || ((ch >= 0xCEC9) && (ch <= 0xCEE3))
            || ((ch >= 0xCEE5) && (ch <= 0xCEFF))
            || ((ch >= 0xCF01) && (ch <= 0xCF1B))
            || ((ch >= 0xCF1D) && (ch <= 0xCF37))
            || ((ch >= 0xCF39) && (ch <= 0xCF53))
            || ((ch >= 0xCF55) && (ch <= 0xCF6F))
            || ((ch >= 0xCF71) && (ch <= 0xCF8B))
            || ((ch >= 0xCF8D) && (ch <= 0xCFA7))
            || ((ch >= 0xCFA9) && (ch <= 0xCFC3))
            || ((ch >= 0xCFC5) && (ch <= 0xCFDF))
            || ((ch >= 0xCFE1) && (ch <= 0xCFFB))
            || ((ch >= 0xCFFD) && (ch <= 0xD017))
            || ((ch >= 0xD019) && (ch <= 0xD033))
            || ((ch >= 0xD035) && (ch <= 0xD04F))
            || ((ch >= 0xD051) && (ch <= 0xD06B))
            || ((ch >= 0xD06D) && (ch <= 0xD087))
            || ((ch >= 0xD089) && (ch <= 0xD0A3))
            || ((ch >= 0xD0A5) && (ch <= 0xD0BF))
            || ((ch >= 0xD0C1) && (ch <= 0xD0DB))
            || ((ch >= 0xD0DD) && (ch <= 0xD0F7))
            || ((ch >= 0xD0F9) && (ch <= 0xD113))
            || ((ch >= 0xD115) && (ch <= 0xD12F))
            || ((ch >= 0xD131) && (ch <= 0xD14B))
            || ((ch >= 0xD14D) && (ch <= 0xD167))
            || ((ch >= 0xD169) && (ch <= 0xD183))
            || ((ch >= 0xD185) && (ch <= 0xD19F))
            || ((ch >= 0xD1A1) && (ch <= 0xD1BB))
            || ((ch >= 0xD1BD) && (ch <= 0xD1D7))
            || ((ch >= 0xD1D9) && (ch <= 0xD1F3))
            || ((ch >= 0xD1F5) && (ch <= 0xD20F))
            || ((ch >= 0xD211) && (ch <= 0xD22B))
            || ((ch >= 0xD22D) && (ch <= 0xD247))
            || ((ch >= 0xD249) && (ch <= 0xD263))
            || ((ch >= 0xD265) && (ch <= 0xD27F))
            || ((ch >= 0xD281) && (ch <= 0xD29B))
            || ((ch >= 0xD29D) && (ch <= 0xD2B7))
            || ((ch >= 0xD2B9) && (ch <= 0xD2D3))
            || ((ch >= 0xD2D5) && (ch <= 0xD2EF))
            || ((ch >= 0xD2F1) && (ch <= 0xD30B))
            || ((ch >= 0xD30D) && (ch <= 0xD327))
            || ((ch >= 0xD329) && (ch <= 0xD343))
            || ((ch >= 0xD345) && (ch <= 0xD35F))
            || ((ch >= 0xD361) && (ch <= 0xD37B))
            || ((ch >= 0xD37D) && (ch <= 0xD397))
            || ((ch >= 0xD399) && (ch <= 0xD3B3))
            || ((ch >= 0xD3B5) && (ch <= 0xD3CF))
            || ((ch >= 0xD3D1) && (ch <= 0xD3EB))
            || ((ch >= 0xD3ED) && (ch <= 0xD407))
            || ((ch >= 0xD409) && (ch <= 0xD423))
            || ((ch >= 0xD425) && (ch <= 0xD43F))
            || ((ch >= 0xD441) && (ch <= 0xD45B))
            || ((ch >= 0xD45D) && (ch <= 0xD477))
            || ((ch >= 0xD479) && (ch <= 0xD493))
            || ((ch >= 0xD495) && (ch <= 0xD4AF))
            || ((ch >= 0xD4B1) && (ch <= 0xD4CB))
            || ((ch >= 0xD4CD) && (ch <= 0xD4E7))
            || ((ch >= 0xD4E9) && (ch <= 0xD503))
            || ((ch >= 0xD505) && (ch <= 0xD51F))
            || ((ch >= 0xD521) && (ch <= 0xD53B))
            || ((ch >= 0xD53D) && (ch <= 0xD557))
            || ((ch >= 0xD559) && (ch <= 0xD573))
            || ((ch >= 0xD575) && (ch <= 0xD58F))
            || ((ch >= 0xD591) && (ch <= 0xD5AB))
            || ((ch >= 0xD5AD) && (ch <= 0xD5C7))
            || ((ch >= 0xD5C9) && (ch <= 0xD5E3))
            || ((ch >= 0xD5E5) && (ch <= 0xD5FF))
            || ((ch >= 0xD601) && (ch <= 0xD61B))
            || ((ch >= 0xD61D) && (ch <= 0xD637))
            || ((ch >= 0xD639) && (ch <= 0xD653))
            || ((ch >= 0xD655) && (ch <= 0xD66F))
            || ((ch >= 0xD671) && (ch <= 0xD68B))
            || ((ch >= 0xD68D) && (ch <= 0xD6A7))
            || ((ch >= 0xD6A9) && (ch <= 0xD6C3))
            || ((ch >= 0xD6C5) && (ch <= 0xD6DF))
            || ((ch >= 0xD6E1) && (ch <= 0xD6FB))
            || ((ch >= 0xD6FD) && (ch <= 0xD717))
            || ((ch >= 0xD719) && (ch <= 0xD733))
            || ((ch >= 0xD735) && (ch <= 0xD74F))
            || ((ch >= 0xD751) && (ch <= 0xD76B))
            || ((ch >= 0xD76D) && (ch <= 0xD787))
            || ((ch >= 0xD789) && (ch <= 0xD7A3))
        ) {
            return true;
        }
        return false;
    }

    /**
     * Check if codepoint has the ZWJ Grapheme_Cluster_Break property.
     *
     * @param ch character to check
     * @return true if ZWJ
     */
    public static boolean isZWJ(final int ch) {
        return (ch == 0x200D);
    }

    /**
     * Check if codepoint has the Other Grapheme_Cluster_Break property.
     *
     * @param ch character to check
     * @return true if Other
     */
    public static boolean isOther(final int ch) {
        if (isPrepend(ch)
            || isCR(ch)
            || isLF(ch)
            || isControl(ch)
            || isExtend(ch)
            || isSpacingMark(ch)
            || isL(ch)
            || isV(ch)
            || isT(ch)
            || isLV(ch)
            || isLVT(ch)
            || isZWJ(ch)
        ) {
            return false;
        }
        return true;
    }

    /**
     * Make a sub-copy of a codepoint array.
     *
     * @param codePoints an array of codepoints
     * @param start starting location to copy
     * @param length number of codepoints to copy
     * @return a new array containing codePoints[start .. (length - 1)]
     */
    private static int [] slice(final int [] codePoints, final int start,
        final int length) {

        int [] result = new int[length];
        System.arraycopy(codePoints, start, result, 0, length);
        return result;
    }

    /**
     * See if a grapheme cluster break should occur between two codepoints,
     * following most of the rules of Unicode TR #29 section 3.1.1.
     *
     * @param firstCh the first codepoint in the sequence
     * @param secondCh the second codepoint in the sequence
     * @return true if a break should be between these codepoints
     */
    public static boolean shouldBreak(final int firstCh, final int secondCh) {
        /*
        System.err.println("shouldBreak: " + (char) firstCh + " "
            + (char) secondCh);
         */

        if (isL(firstCh)
            && (isL(secondCh) || isV(secondCh) || isLV(secondCh) || isLVT(secondCh))
        ) {
            // GB6
            // System.err.println("GB6 false");
            return false;
        }

        if ((isLV(firstCh) || isV(firstCh))
            && (isV(secondCh) || isT(secondCh))
        ) {
            // GB7
            // System.err.println("GB7 false");
            return false;
        }

        if ((isLVT(firstCh) || isT(firstCh))
            && isT(secondCh)
        ) {
            // GB8
            // System.err.println("GB8 false");
            return false;
        }

        if (isExtend(secondCh) || isZWJ(secondCh)) {
            // GB9
            // System.err.println("GB9 false");
            return false;
        }

        if (isSpacingMark(secondCh) || isPrepend(firstCh)) {
            // GB9a
            // GB9b
            // System.err.println("GB9a GB9b false");
            return false;
        }

        // GB3, GB4, GB9c, GB12, and GB13 - Will not implement

        if ((isEmoji(firstCh)
                || isEmojiBMP(firstCh)
                || isEmojiCombiner(secondCh))
            && (isEmojiCombiner(secondCh) || isEmojiComponent(secondCh))
            && !isRegionalIndicator(firstCh)
        ) {
            // GB11
            // System.err.println("GB11 false");
            return false;
        }

        // GB999 - all others break
        return true;
    }

    /**
     * Converts a string into a sequence of grapheme clusters following most
     * of the rules of Unicode TR #29 section 3.1.1.
     *
     * @param input a string of codepoints
     * @return a sequence of grapheme clusters
     */
    public static List<ComplexCell> toComplexCells(final String input) {
        List<ComplexCell> result = new ArrayList<ComplexCell>();
        int [] codePoints = StringUtils.toCodePoints(input);

        int [] grapheme = null;
        int begin = 0;
        int lastCh = 0;
        if (codePoints.length > 0) {
            lastCh = codePoints[0];
        }

        // As per GB1, any codepoint after the start of text (sot) begins a
        // new grapheme. So we start the loop at 1 instead of 0.
        for (int i = 1; i < codePoints.length; i++) {
            int ch = codePoints[i];

            if (shouldBreak(lastCh, ch)) {
                // Everything before this is one grapheme.
                int n = i - begin;
                assert (n > 0);
                grapheme = slice(codePoints, begin, n);
                ComplexCell cell = new ComplexCell(grapheme);
                result.add(cell);
                begin = i;
            }

            lastCh = ch;

        } // for (int i = 1; i < codePoints.length; i++)

        int n = codePoints.length - begin;
        if (n > 0) {
            // GB2
            grapheme = slice(codePoints, begin, n);
            ComplexCell cell = new ComplexCell(grapheme);
            result.add(cell);
            begin = codePoints.length;
        }

        return result;
    }

    /**
     * Test the extended grapheme cluster boundary code.
     *
     * @param args command line arguments
     */
    public static void main(final String [] args) {
        List<ComplexCell> result;

        System.out.println(toComplexCells(""));
        System.out.println(toComplexCells("Hello"));
        System.out.println(toComplexCells("He\nllo"));
        System.out.println(toComplexCells("H\te\nl\nlo\n"));
        System.out.println(toComplexCells("He\u0308\u0306llo\u0301\u0302\u0303"));
        System.out.println(toComplexCells("H\u070Fe\u0306\nl\nlo\n"));
        System.out.println(toComplexCells("\uD83C\uDFD6\uFE0F\ud83e\udec2__"));
    }

}
