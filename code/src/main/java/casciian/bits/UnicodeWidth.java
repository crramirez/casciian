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
     * <p>
     * Delegates to {@link Character#isISOControl(int)}, which covers exactly
     * the C0 (U+0000..U+001F) and C1 (U+007F..U+009F) control ranges.
     *
     * @param ch the code point
     * @return true if control character
     */
    private static boolean isControlCharacter(final int ch) {
        return Character.isISOControl(ch);
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
     * Check if a codepoint is a combining character (width 0).
     * <p>
     * Rather than a hand-maintained table of Unicode ranges, this defers to
     * {@link Character#getType(int)} and treats non-spacing marks (Mn) and
     * enclosing marks (Me) as zero-width. This keeps the classification in
     * sync with the Unicode version bundled with the running JDK. Spacing
     * combining marks (Mc) are intentionally excluded because they occupy a
     * display cell.
     *
     * @param ch the code point
     * @return true if combining character
     */
    private static boolean isCombiningCharacter(final int ch) {
        final int type = Character.getType(ch);
        return (type == Character.NON_SPACING_MARK)
            || (type == Character.ENCLOSING_MARK);
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
        return ch == 0x1F5AE
            || ch == 0x1F5D9
            || ch == 0x2B6F
            || ch == 0x2B6E
            || ch == 0x1F5F6
            || ch == 0x1F5D0
            || ch == 0x1F5D7
            || ch == 0x1F5D1
            || ch == 0x1F5BC;
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
