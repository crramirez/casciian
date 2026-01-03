/*
 * Casciian - Java Text User Interface
 *
 * Original work written 2013–2025 by Autumn Lamonte
 * and dedicated to the public domain via CC0.
 *
 * Modifications and maintenance:
 * Copyright 2025 Carlos Rafael Ramirez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package casciian.bits;

/**
 * UnicodeWidth provides methods to compute the display width of Unicode
 * codepoints and strings in terminal/text environments.
 * <br>
 * This is based on mk_wcwidth() available at:
 * <a href="http://www.cl.cam.ac.uk/~mgk25/ucs/wcwidth.c">http://www.cl.cam.ac.uk/~mgk25/ucs/wcwidth.c</a>
 * <br>
 * Enhanced to handle:
 * - Zero-width characters (ZWJ, variation selectors, combining marks)
 * - Modern emoji ranges
 * - Variation selectors (text vs emoji presentation)
 * - CJK characters and fullwidth forms
 */
public class UnicodeWidth {

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Private constructor prevents accidental creation of this class.
     */
    private UnicodeWidth() {}

    // ------------------------------------------------------------------------
    // Public Width Calculation Methods --------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Determine the display width of a Unicode code point.
     *
     * @param ch the Unicode code point to measure
     * @return the number of text cell columns required to display this code
     * point, one of 0, 1, or 2
     */
    public static int width(final int ch) {
        if (isControlCharacter(ch) || isZeroWidthCharacter(ch) || isCombiningCharacter(ch)) {
            return 0;
        }

        if (isSingleWidthEmoji(ch)) {
            return 1;
        }

        if (isWideCharacter(ch)) {
            return 2;
        }

        return 1;
    }

    // ------------------------------------------------------------------------
    // Character Classification Methods ---------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Check if a codepoint is a control character (width 0).
     *
     * @param ch the code point
     * @return true if control character
     */
    private static boolean isControlCharacter(final int ch) {
        return (ch < 32) || ((ch >= 0x7f) && (ch < 0xa0));
    }

    /**
     * Check if a codepoint is a zero-width character.
     *
     * @param ch the code point
     * @return true if zero-width
     */
    private static boolean isZeroWidthCharacter(final int ch) {
        return (ch == 0x200b)                          // Zero Width Space
            || (ch == 0x200c)                          // Zero Width Non-Joiner
            || (ch == 0x200d)                          // Zero Width Joiner
            || (ch == 0xfeff)                          // Zero Width No-Break Space
            || ((ch >= 0x180b) && (ch <= 0x180e))      // Mongolian variation selectors
            || ((ch >= 0xfe00) && (ch <= 0xfe0f))      // Variation Selectors 1-16
            || ((ch >= 0xe0100) && (ch <= 0xe01ef));   // Variation Selectors 17-256
    }

    /**
     * Check if a codepoint is a combining character.
     *
     * @param ch the code point
     * @return true if combining character
     */
    private static boolean isCombiningCharacter(final int ch) {
        return ((ch >= 0x0300) && (ch <= 0x036f))      // Combining Diacritical Marks
            || ((ch >= 0x0483) && (ch <= 0x0489))      // Cyrillic combining
            || ((ch >= 0x0591) && (ch <= 0x05bd))      // Hebrew combining
            || ((ch >= 0x05bf) && (ch <= 0x05c7))      // Hebrew combining
            || ((ch >= 0x0610) && (ch <= 0x061a))      // Arabic combining
            || ((ch >= 0x064b) && (ch <= 0x065f))      // Arabic combining
            || (ch == 0x670)                           // Arabic combining
            || ((ch >= 0x06d6) && (ch <= 0x06ed))      // Arabic combining
            || (ch == 0x711)                           // Syriac combining
            || ((ch >= 0x0730) && (ch <= 0x074a))      // Syriac combining
            || ((ch >= 0x07a6) && (ch <= 0x07b0))      // Thaana combining
            || ((ch >= 0x07eb) && (ch <= 0x07f3))      // NKo combining
            || ((ch >= 0x0816) && (ch <= 0x0819))      // Samaritan combining
            || ((ch >= 0x081b) && (ch <= 0x0823))      // Samaritan combining
            || ((ch >= 0x0825) && (ch <= 0x0827))      // Samaritan combining
            || ((ch >= 0x0829) && (ch <= 0x082d))      // Samaritan combining
            || ((ch >= 0x0859) && (ch <= 0x085b))      // Mandaic combining
            || ((ch >= 0x0900) && (ch <= 0x0902))      // Devanagari combining
            || ((ch >= 0x093a) && (ch <= 0x093c))      // Devanagari combining
            || ((ch >= 0x0941) && (ch <= 0x0948))      // Devanagari combining
            || (ch == 0x94D)                           // Devanagari combining
            || ((ch >= 0x0951) && (ch <= 0x0957))      // Devanagari combining
            || ((ch >= 0x0962) && (ch <= 0x0963))      // Devanagari combining
            || ((ch >= 0x1ab0) && (ch <= 0x1ace))      // More combining marks
            || ((ch >= 0x1dc0) && (ch <= 0x1dff))      // Combining Diacritical Marks Supplement
            || ((ch >= 0x20d0) && (ch <= 0x20f0))      // Combining Diacritical Marks for Symbols
            || ((ch >= 0xfe20) && (ch <= 0xfe2f));     // Combining Half Marks
    }

    /**
     * Check if a codepoint is a single-width emoji.
     * <p>
     * Most emoji are treated as wide characters via {@link #isWideCharacter(int)}
     * (see {@code isEmojiRange(int)}). However, a small number of emoji are commonly
     * rendered as single-width glyphs in many terminal/font combinations. Those code
     * points are explicitly whitelisted here so that their width matches actual
     * observed behavior in typical terminal environments.
     * <p>
     * This list is intentionally conservative and is <strong>not</strong> meant to
     * automatically track all future Unicode emoji. If you need to adjust the
     * behavior for a specific environment (e.g., when new emoji are known to be
     * rendered as single-width), extend or modify this set after validating the
     * rendering behavior in your target terminals, or consider wiring this through
     * a configuration layer external to this class.
     *
     * @param ch the code point
     * @return true if single-width emoji
     */
    private static boolean isSingleWidthEmoji(final int ch) {
        return ch == 0x1F5AE  // 📮 Postbox
            || ch == 0x1F5D9  // 🗙 Dismiss/close symbol
            || ch == 0x2B6F   // ⭯ Three-dimensional arrow (clockwise)
            || ch == 0x2B6E   // ⭮ Three-dimensional arrow (anticlockwise)
            || ch == 0x1F5F6  // 🗶 Ballot X
            || ch == 0x1F5D0  // 🗐 Page
            || ch == 0x1F5D7; // 🗗 Window
    }

    /**
     * Check if a codepoint is a wide character (CJK, emoji, etc.).
     *
     * @param ch the code point
     * @return true if wide character
     */
    private static boolean isWideCharacter(final int ch) {
        if (ch < 0x1100) {
            return false;
        }

        return (ch <= 0x115f)                          // Hangul Jamo init. consonants
            || (ch == 0x2329)
            || (ch == 0x232a)
            || isCJKAndRelated(ch)
            || isEmojiRange(ch)
            || isArrowAndSymbol(ch);
    }

    /**
     * Check if a codepoint is in CJK or related ranges.
     *
     * @param ch the code point
     * @return true if in CJK range
     */
    private static boolean isCJKAndRelated(final int ch) {
        return ((ch >= 0x2e80) && (ch <= 0xa4cf) && (ch != 0x303f))    // CJK ... Yi
            || ((ch >= 0xac00) && (ch <= 0xd7a3))                      // Hangul Syllables
            || ((ch >= 0xf900) && (ch <= 0xfaff))                      // CJK Compatibility Ideographs
            || ((ch >= 0xfe10) && (ch <= 0xfe19))                      // Vertical forms
            || ((ch >= 0xfe30) && (ch <= 0xfe6f))                      // CJK Compatibility Forms
            || ((ch >= 0xff00) && (ch <= 0xff60))                      // Fullwidth Forms
            || ((ch >= 0xffe0) && (ch <= 0xffe6))
            || ((ch >= 0x20000) && (ch <= 0x2fffd))
            || ((ch >= 0x30000) && (ch <= 0x3fffd));
    }

    /**
     * Check if a codepoint is in emoji ranges.
     *
     * @param ch the code point
     * @return true if in emoji range
     */
    private static boolean isEmojiRange(final int ch) {
        return ((ch >= 0x1f004) && (ch < 0x1fb00))     // emoji - exclude symbols for legacy computing
            || ((ch >= 0x1fc00) && (ch <= 0x1fffd));
    }

    /**
     * Check if a codepoint is in arrow or symbol ranges.
     *
     * @param ch the code point
     * @return true if in arrow/symbol range
     */
    private static boolean isArrowAndSymbol(final int ch) {
        return isArrowRange(ch)
            || isMiscellaneousSymbol(ch)
            || isDingbat(ch)
            || isMiscSymbolAndArrow(ch);
    }

    /**
     * Check if a codepoint is in arrow ranges.
     *
     * @param ch the code point
     * @return true if in arrow range
     */
    private static boolean isArrowRange(final int ch) {
        return ((ch >= 0x2196) && (ch <= 0x21ff) && (ch != 0x21a8))    // Arrows - but not the ones in CP437
            || ((ch >= 0x2900) && (ch <= 0x297f));                     // Supplemental Arrows
    }

    /**
     * Check if a codepoint is a miscellaneous symbol.
     *
     * @param ch the code point
     * @return true if miscellaneous symbol
     */
    private static boolean isMiscellaneousSymbol(final int ch) {
        return ((ch >= 0x2614) && (ch <= 0x2615))
            || ((ch >= 0x2630) && (ch <= 0x2637))
            || ((ch >= 0x2648) && (ch <= 0x2653))
            || (ch == 0x267f)
            || ((ch >= 0x268a) && (ch <= 0x268f))
            || (ch == 0x2693)
            || (ch == 0x26a0)
            || (ch == 0x26a1)
            || ((ch >= 0x26aa) && (ch <= 0x26ab))
            || ((ch >= 0x26bd) && (ch <= 0x26be))
            || ((ch >= 0x26c4) && (ch <= 0x26c5))
            || (ch == 0x26ce)
            || (ch == 0x26d4)
            || (ch == 0x26ea)
            || ((ch >= 0x26f2) && (ch <= 0x26f3))
            || (ch == 0x26f5)
            || (ch == 0x26fa)
            || (ch == 0x26fd);
    }

    /**
     * Check if a codepoint is a dingbat.
     *
     * @param ch the code point
     * @return true if dingbat
     */
    private static boolean isDingbat(final int ch) {
        return (ch == 0x2705)
            || ((ch >= 0x270a) && (ch <= 0x270b))
            || (ch == 0x2728)
            || (ch == 0x274c)
            || (ch == 0x274e)
            || ((ch >= 0x2753) && (ch <= 0x2755))
            || (ch == 0x2757)
            || ((ch >= 0x2795) && (ch <= 0x2797))
            || (ch == 0x27b0)
            || (ch == 0x27bf);
    }

    /**
     * Check if a codepoint is a miscellaneous symbol or arrow.
     *
     * @param ch the code point
     * @return true if miscellaneous symbol/arrow
     */
    private static boolean isMiscSymbolAndArrow(final int ch) {
        return ((ch >= 0x2b1b) && (ch <= 0x2b1c))
            || (ch == 0x2b50)
            || (ch == 0x2b55)
            || (ch == 0x2b6e)                          // Specific glyphs we use in Casciian
            || (ch == 0x2b6f);
    }
}
