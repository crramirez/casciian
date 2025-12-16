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
import java.util.Base64;

/**
 * StringUtils contains methods to:
 *    - Convert one or more long lines of strings into justified text
 *      paragraphs.
 *    - Unescape C0 control codes.
 *    - Read/write a line of RFC4180 comma-separated values strings to/from a
 *      list of strings.
 *    - Compute number of visible text cells for a given Unicode codepoint or
 *      string.
 *    - Convert bytes to and from base-64 encoding.
 */
public class StringUtils {

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Private constructor prevents accidental creation of this class.
     */
    private StringUtils() {}

    // ------------------------------------------------------------------------
    // StringUtils -------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Left-justify a string into a list of lines.
     *
     * @param str the string
     * @param n the maximum number of characters in a line
     * @return the list of lines
     */
    public static List<String> left(final String str, final int n) {
        List<String> result = new ArrayList<>();

        /*
         * General procedure:
         *
         *   1. Split on '\n' into paragraphs.
         *
         *   2. Scan each line, noting the position of the last
         *      beginning-of-a-word.
         *
         *   3. Chop at the last #2 if the next beginning-of-a-word exceeds
         *      n.
         *
         *   4. Return the lines.
         */

        String [] rawLines = str.split("\n");
        for (String rawLine : rawLines) {
            StringBuilder line = new StringBuilder();
            StringBuilder word = new StringBuilder();
            boolean inWord = false;
            for (int j = 0; j < rawLine.length(); j++) {
                char ch = rawLine.charAt(j);
                if ((ch == ' ') || (ch == '\t')) {
                    if (inWord) {
                        // We have just transitioned from a word to
                        // whitespace.  See if we have enough space to add
                        // the word to the line.
                        if (width(word.toString()) + width(line.toString()) > n) {
                            // This word will exceed the line length.  Wrap
                            // at it instead.
                            result.add(line.toString());
                            line = new StringBuilder();
                        }
                        if ((word.toString().startsWith(" "))
                                && (width(line.toString()) == 0)
                        ) {
                            line.append(word.substring(1));
                        } else {
                            line.append(word);
                        }
                        word = new StringBuilder();
                        word.append(ch);
                        inWord = false;
                    }
                } else {
                    if (inWord) {
                        // We are appending to a word.
                        word.append(ch);
                    } else {
                        // We have transitioned from whitespace to a word.
                        word.append(ch);
                        inWord = true;
                    }
                }
            } // for (int j = 0; j < rawLines[i].length(); j++)

            if (width(word.toString()) + width(line.toString()) > n) {
                // This word will exceed the line length.  Wrap at it
                // instead.
                result.add(line.toString());
                line = new StringBuilder();
            }
            if ((word.toString().startsWith(" "))
                    && (width(line.toString()) == 0)
            ) {
                line.append(word.substring(1));
            } else {
                line.append(word);
            }
            result.add(line.toString());
        } // for (int i = 0; i < rawLines.length; i++) {

        return result;
    }

    /**
     * Right-justify a string into a list of lines.
     *
     * @param str the string
     * @param n the maximum number of characters in a line
     * @return the list of lines
     */
    public static List<String> right(final String str, final int n) {
        List<String> result = new ArrayList<>();

        /*
         * Same as left(), but preceed each line with spaces to make it n
         * chars long.
         */
        List<String> lines = left(str, n);
        for (String line: lines) {
            String rightAlignedLine = " ".repeat(Math.max(0, n - width(line))) + line;
            result.add(rightAlignedLine);
        }

        return result;
    }

    /**
     * Center a string into a list of lines.
     *
     * @param str the string
     * @param n the maximum number of characters in a line
     * @return the list of lines
     */
    public static List<String> center(final String str, final int n) {
        List<String> result = new ArrayList<>();

        /*
         * Same as left(), but preceed/succeed each line with spaces to make
         * it n chars long.
         */
        List<String> lines = left(str, n);
        for (String line: lines) {
            StringBuilder sb = new StringBuilder();
            int l = (n - width(line)) / 2;
            int r = n - width(line) - l;
            sb.append(" ".repeat(Math.max(0, l)));
            sb.append(line);
            sb.append(" ".repeat(Math.max(0, r)));
            result.add(sb.toString());
        }

        return result;
    }

    /**
     * Fully-justify a string into a list of lines.
     *
     * @param str the string
     * @param n the maximum number of characters in a line
     * @return the list of lines
     */
    public static List<String> full(final String str, final int n) {
        List<String> result = new ArrayList<>();

        /*
         * Same as left(), but insert spaces between words to make each line
         * n chars long.  The "algorithm" here is pretty dumb: it performs a
         * split on space and then re-inserts multiples of n between words.
         */
        List<String> lines = left(str, n);
        for (int lineI = 0; lineI < lines.size() - 1; lineI++) {
            String line = lines.get(lineI);
            String [] words = line.split(" ");
            if (words.length > 1) {
                int charCount = 0;
                for (String word : words) {
                    charCount += word.length();
                }
                int spaceCount = n - charCount;
                int q = spaceCount / (words.length - 1);
                int r = spaceCount % (words.length - 1);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < words.length - 1; i++) {
                    sb.append(words[i]);
                    sb.append(" ".repeat(Math.max(0, q)));
                    if (r > 0) {
                        sb.append(' ');
                        r--;
                    }
                }
                sb.append(" ".repeat(Math.max(0, r)));
                sb.append(words[words.length - 1]);
                result.add(sb.toString());
            } else {
                result.add(line);
            }
        }
        if (!lines.isEmpty()) {
            result.add(lines.getLast());
        }

        return result;
    }

    /**
     * Convert raw strings into escaped strings that be splatted on the
     * screen.
     *
     * @param str the string
     * @return a string that can be passed into Screen.putStringXY()
     */
    public static String unescape(final String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if ((ch < 0x20) || (ch == 0x7F)) {
                switch (ch) {
                case '\b':
                    sb.append("\\b");
                    continue;
                case '\f':
                    sb.append("\\f");
                    continue;
                case '\n':
                    sb.append("\\n");
                    continue;
                case '\r':
                    sb.append("\\r");
                    continue;
                case '\t':
                    sb.append("\\t");
                    continue;
                case 0x7f:
                    sb.append("^?");
                    continue;
                default:
                    sb.append(' ');
                    continue;
                }
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    /**
     * Read a line of RFC4180 comma-separated values (CSV) into a list of
     * strings.
     *
     * @param line the CSV line, with or without without line terminators
     * @return the list of strings
     */
    public static List<String> fromCsv(final String line) {
        List<String> result = new ArrayList<>();

        StringBuilder str = new StringBuilder();
        boolean quoted = false;
        boolean fieldQuoted = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            /*
            System.err.println("ch '" + ch + "' str '" + str + "' " +
                " fieldQuoted " + fieldQuoted + " quoted " + quoted);
             */

            if (ch == ',') {
                if (fieldQuoted && quoted) {
                    // Terminating a quoted field.
                    result.add(str.toString());
                    str = new StringBuilder();
                    quoted = false;
                    fieldQuoted = false;
                } else if (fieldQuoted) {
                    // Still waiting to see the terminating quote for this
                    // field.
                    str.append(ch);
                } else if (quoted) {
                    // An unmatched double-quote and comma.  This should be
                    // an invalid sequence.  We will treat it as a quote
                    // terminating the field.
                    str.append('\"');
                    result.add(str.toString());
                    str = new StringBuilder();
                    quoted = false;
                } else {
                    // A field separator.
                    result.add(str.toString());
                    str = new StringBuilder();
                }
                continue;
            }

            if (ch == '\"') {
                if ((str.isEmpty()) && (!fieldQuoted)) {
                    // The opening quote to a quoted field.
                    fieldQuoted = true;
                } else if (quoted) {
                    // This is a double-quote.
                    str.append('\"');
                    quoted = false;
                } else {
                    // This is the beginning of a quote.
                    quoted = true;
                }
                continue;
            }

            // Normal character, pass it on.
            str.append(ch);
        }

        // Include the final field.
        result.add(str.toString());

        return result;
    }

    /**
     * Write a list of strings to on line of RFC4180 comma-separated values
     * (CSV).
     *
     * @param list the list of strings
     * @return the CSV line, without any line terminators
     */
    public static String toCsv(final List<String> list) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        for (String str: list) {

            if (!str.contains("\"") && !str.contains(",")) {
                // Just append the string with a comma.
                result.append(str);
            } else if (!str.contains("\"") && str.contains(",")) {
                // Contains commas, but no quotes.  Just double-quote it.
                result.append("\"");
                result.append(str);
                result.append("\"");
            } else if (str.contains("\"")) {
                // Contains quotes and maybe commas.  Double-quote it and
                // replace quotes inside.
                result.append("\"");
                for (int j = 0; j < str.length(); j++) {
                    char ch = str.charAt(j);
                    result.append(ch);
                    if (ch == '\"') {
                        result.append("\"");
                    }
                }
                result.append("\"");
            }

            if (i < list.size() - 1) {
                result.append(",");
            }
            i++;
        }
        return result.toString();
    }

    /**
     * Determine display width of a Unicode code point.
     *
     * @param ch the code point, can be char
     * @return the number of text cell columns required to display this code
     * point, one of 0, 1, or 2
     */
    public static int width(final int ch) {
        /*
         * This routine is a modified version of mk_wcwidth() available
         * at: http://www.cl.cam.ac.uk/~mgk25/ucs/wcwidth.c
         *
         * The combining characters list has been omitted from this
         * implementation.  Hopefully no users will be impacted.
         */

        // 8-bit control characters: width 0
        if (ch == 0) {
            return 0;
        }
        if ((ch < 32) || ((ch >= 0x7f) && (ch < 0xa0))) {
            return 0;
        }

        // All others: either 1 or 2
        if ((ch >= 0x1100)
            && ((ch <= 0x115f)
                // Hangul Jamo init. consonants
                || (ch == 0x2329)
                || (ch == 0x232a)
                // CJK ... Yi
                || ((ch >= 0x2e80) && (ch <= 0xa4cf) && (ch != 0x303f))
                // Hangul Syllables
                || ((ch >= 0xac00) && (ch <= 0xd7a3))
                // CJK Compatibility Ideographs
                || ((ch >= 0xf900) && (ch <= 0xfaff))
                // Vertical forms
                || ((ch >= 0xfe10) && (ch <= 0xfe19))
                // CJK Compatibility Forms
                || ((ch >= 0xfe30) && (ch <= 0xfe6f))
                // Fullwidth Forms
                || ((ch >= 0xff00) && (ch <= 0xff60))
                || ((ch >= 0xffe0) && (ch <= 0xffe6))
                || ((ch >= 0x20000) && (ch <= 0x2fffd))
                || ((ch >= 0x30000) && (ch <= 0x3fffd))
                // emoji - exclude symbols for legacy computing
                || ((ch >= 0x1f004) && (ch < 0x1fb00))
                // Symbols for Legacy Computing, 1 or 2?
                // || ((ch >= 0x1fb00) && (ch <= 0x1fbff))
                || ((ch >= 0x1fc00) && (ch <= 0x1fffd))

                // Arrows - but not the ones in CP437
                // || ((ch >= 0x2190) && (ch <= 0x21ff))
                || ((ch >= 0x2196) && (ch <= 0x21ff) && (ch != 0x21a8))

                // Supplemental Arrows
                || ((ch >= 0x2900) && (ch <= 0x297f))

                // Miscellaneous Symbols - a random smattering
                // || ((ch >= 0x2600) && (ch <= 0x26ff))
                || (ch >= 0x2614) && (ch <= 0x2615)
                || (ch >= 0x2630) && (ch <= 0x2637)
                || (ch >= 0x2648) && (ch <= 0x2653)
                || (ch == 0x267f)
                || (ch >= 0x268a) && (ch <= 0x268f)
                || (ch == 0x2693)
                || (ch == 0x26a0)
                || (ch == 0x26a1)
                || (ch >= 0x26aa) && (ch <= 0x26ab)
                || (ch >= 0x26bd) && (ch <= 0x26be)
                || (ch >= 0x26c4) && (ch <= 0x26c5)
                || (ch == 0x26ce)
                || (ch == 0x26d4)
                || (ch == 0x26ea)
                || (ch >= 0x26f2) && (ch <= 0x26f3)
                || (ch == 0x26f5)
                || (ch == 0x26fa)
                || (ch == 0x26fd)

                // Dingbats - a smattering
                // || ((ch >= 0x2700) && (ch <= 0x27bf))
                || (ch == 0x2705)
                || (ch >= 0x270a) && (ch <= 0x270b)
                || (ch == 0x2728)
                || (ch == 0x274c)
                || (ch == 0x274e)
                || (ch >= 0x2753) && (ch <= 0x2755)
                || (ch == 0x2757)
                || (ch >= 0x2795) && (ch <= 0x2797)
                || (ch == 0x27b0)
                || (ch == 0x27bf)

                // Miscellaneous Symbols and Arrows - a smattering
                // || ((ch >= 0x2b00) && (ch <= 0x2bff))
                || (ch >= 0x2b1b) && (ch <= 0x2b1c)
                || (ch == 0x2b50)
                || (ch == 0x2b55)

                // Specific glyphs we use in Casciian
                || (ch == 0x2b6e)
                || (ch == 0x2b6f)

            )
        ) {
            return 2;
        }
        return 1;
    }

    /**
     * Determine display width of a string.  This ASSUMES that no characters
     * are combining.  Hopefully no users will be impacted.
     *
     * @param str the string
     * @return the number of text cell columns required to display this string
     */
    public static int width(final String str) {
        if (str == null) {
            return 0;
        }

        int n = 0;
        for (int i = 0; i < str.length();) {
            int ch = str.codePointAt(i);
            n += width(ch);
            i += Character.charCount(ch);
        }
        return n;
    }

    /**
     * Determine display width of a list of codepoints that will be displayed
     * as a single extended grapheme cluster.
     *
     * @param codePoints the codepoints
     * @return the number of text cell columns required to display this
     * grapheme, either 1 or 2
     */
    public static int width(final List<Integer> codePoints) {
        // Special case: 2-cell Regional Indicator codes return 2, not 1.
        if ((codePoints.size() == 2)
            && ExtendedGraphemeClusterUtils.isRegionalIndicator(codePoints.get(0))
            && ExtendedGraphemeClusterUtils.isRegionalIndicator(codePoints.get(1))
        ) {
            return 2;
        }

        int n = 0;
        for (Integer ch: codePoints) {
            n = Math.max(n, width(ch));
        }
        return n;
    }

    /**
     * Determine display width of an array of codepoints that will be
     * displayed as a single extended grapheme cluster.
     *
     * @param codePoints the codepoints
     * @return the number of text cell columns required to display this
     * grapheme, either 1 or 2
     */
    public static int width(final int [] codePoints) {
        // Special case: 2-cell Regional Indicator codes return 2, not 1.
        if ((codePoints.length == 2)
            && ExtendedGraphemeClusterUtils.isRegionalIndicator(codePoints[0])
            && ExtendedGraphemeClusterUtils.isRegionalIndicator(codePoints[1])
        ) {
            return 2;
        }

        int n = 0;
        for (int codePoint : codePoints) {
            n = Math.max(n, width(codePoint));
        }
        return n;
    }

    /**
     * Determine display width of a Cell that will be displayed as a single
     * extended grapheme cluster.
     *
     * @param cell the cell
     * @return the number of text cell columns required to display this
     * grapheme, either 1 or 2
     */
    public static int width(final Cell cell) {
        if (cell instanceof ComplexCell) {
            int [] codePoints = ((ComplexCell) cell).getCodePoints();
            int n = 0;
            for (int codePoint : codePoints) {
                n = Math.max(n, width(codePoint));
            }
            return n;
        }
        return width(cell.getChar());
    }

    /**
     * Converts a string into an array of Unicode codepoints.
     *
     * @param input a string of codepoints
     * @return an array of codepoints
     */
    public static int [] toCodePoints(final String input) {
        int n = input.length();
        int [] codePoints = new int[input.codePointCount(0, n)];

        int idx = 0;

        for (int i = 0; i < codePoints.length; i++) {
            int ch = input.codePointAt(idx);
            codePoints[i] = ch;
            idx += Character.charCount(ch);
        }

        return codePoints;
    }

    /**
     * Converts a string into a sequence of grapheme clusters following most
     * of the rules of Unicode TR #29 section 3.1.1.
     *
     * @param input a string of codepoints
     * @return a sequence of grapheme clusters
     */
    public static List<ComplexCell> toComplexCells(final String input) {
        return ExtendedGraphemeClusterUtils.toComplexCells(input);
    }

    // ------------------------------------------------------------------------
    // Base64 -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Encodes a raw byte array into a BASE64 <code>String</code>
     * representation in accordance with RFC 4648.
     * @param sArr The bytes to convert. If <code>null</code> or length 0
     * an empty string will be returned.
     * @return A BASE64 encoded String. Never <code>null</code>.
     */
    public static String toBase64(byte[] sArr) {
        int sLen = sArr != null ? sArr.length : 0;
        if (sLen == 0) {
            return "";
        }
        // Use JDK's built-in Basic Base64 encoder for RFC 4648 compliance (no LF or whitespaces)
        return Base64.getEncoder().encodeToString(sArr);
    }

    /**
     * Decodes a BASE64 encoded byte array. All illegal characters will
     * be ignored and can handle both arrays with and without line
     * separators.
     * @param sArr The source array. Length 0 will return an empty
     * array. <code>null</code> will throw an exception.
     * @return The decoded array of bytes. May be of length 0. Will be
     * <code>null</code> if the legal characters (including '=') isn't
     * divideable by 4. (I.e. definitely corrupted).
     */
    public static byte[] fromBase64(byte[] sArr) {
        // Use JDK's built-in MIME decoder which ignores line separators and
        // non-base64 characters.
        try {
            return Base64.getMimeDecoder().decode(sArr);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

}
