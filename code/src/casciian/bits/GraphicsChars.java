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

/**
 * This class contains a collection of special characters used by the
 * windowing system and the mappings from CP437 to Unicode.
 */
public final class GraphicsChars {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The CP437 to Unicode translation map.
     */
    public static final char [] CP437 = {
        '\u2007', '\u263A', '\u263B', '\u2665',
        '\u2666', '\u2663', '\u2660', '\u2022',
        '\u25D8', '\u25CB', '\u25D9', '\u2642',
        '\u2640', '\u266A', '\u266B', '\u263C',
        // Terminus has 25B6 and 25C0 here, which I believe are better
        // Unicode equivalents anyway.
        // '\u25BA', '\u25C4', '\u2195', '\u203C',
        '\u25B6', '\u25C0', '\u2195', '\u203C',
        '\u00B6', '\u00A7', '\u25AC', '\u21A8',
        '\u2191', '\u2193', '\u2192', '\u2190',
        '\u221F', '\u2194', '\u25B2', '\u25BC',
        '\u0020', '\u0021', '\"', '\u0023',
        '\u0024', '\u0025', '\u0026', '\'',
        '\u0028', '\u0029', '\u002a', '\u002b',
        '\u002c', '\u002d', '\u002e', '\u002f',
        '\u0030', '\u0031', '\u0032', '\u0033',
        '\u0034', '\u0035', '\u0036', '\u0037',
        '\u0038', '\u0039', '\u003a', '\u003b',
        '\u003c', '\u003d', '\u003e', '\u003f',
        '\u0040', '\u0041', '\u0042', '\u0043',
        '\u0044', '\u0045', '\u0046', '\u0047',
        '\u0048', '\u0049', '\u004a', '\u004b',
        '\u004c', '\u004d', '\u004e', '\u004f',
        '\u0050', '\u0051', '\u0052', '\u0053',
        '\u0054', '\u0055', '\u0056', '\u0057',
        '\u0058', '\u0059', '\u005a', '\u005b',
        '\\', '\u005d', '\u005e', '\u005f',
        '\u0060', '\u0061', '\u0062', '\u0063',
        '\u0064', '\u0065', '\u0066', '\u0067',
        '\u0068', '\u0069', '\u006a', '\u006b',
        '\u006c', '\u006d', '\u006e', '\u006f',
        '\u0070', '\u0071', '\u0072', '\u0073',
        '\u0074', '\u0075', '\u0076', '\u0077',
        '\u0078', '\u0079', '\u007a', '\u007b',
        '\u007c', '\u007d', '\u007e', '\u2302',
        '\u00c7', '\u00fc', '\u00e9', '\u00e2',
        '\u00e4', '\u00e0', '\u00e5', '\u00e7',
        '\u00ea', '\u00eb', '\u00e8', '\u00ef',
        '\u00ee', '\u00ec', '\u00c4', '\u00c5',
        '\u00c9', '\u00e6', '\u00c6', '\u00f4',
        '\u00f6', '\u00f2', '\u00fb', '\u00f9',
        '\u00ff', '\u00d6', '\u00dc', '\u00a2',
        '\u00a3', '\u00a5', '\u20a7', '\u0192',
        '\u00e1', '\u00ed', '\u00f3', '\u00fa',
        '\u00f1', '\u00d1', '\u00aa', '\u00ba',
        '\u00bf', '\u2310', '\u00ac', '\u00bd',
        '\u00bc', '\u00a1', '\u00ab', '\u00bb',
        '\u2591', '\u2592', '\u2593', '\u2502',
        '\u2524', '\u2561', '\u2562', '\u2556',
        '\u2555', '\u2563', '\u2551', '\u2557',
        '\u255d', '\u255c', '\u255b', '\u2510',
        '\u2514', '\u2534', '\u252c', '\u251c',
        '\u2500', '\u253c', '\u255e', '\u255f',
        '\u255a', '\u2554', '\u2569', '\u2566',
        '\u2560', '\u2550', '\u256c', '\u2567',
        '\u2568', '\u2564', '\u2565', '\u2559',
        '\u2558', '\u2552', '\u2553', '\u256b',
        '\u256a', '\u2518', '\u250c', '\u2588',
        '\u2584', '\u258c', '\u2590', '\u2580',
        '\u03b1', '\u00df', '\u0393', '\u03c0',
        '\u03a3', '\u03c3', '\u00b5', '\u03c4',
        '\u03a6', '\u0398', '\u03a9', '\u03b4',
        '\u221e', '\u03c6', '\u03b5', '\u2229',
        '\u2261', '\u00b1', '\u2265', '\u2264',
        '\u2320', '\u2321', '\u00f7', '\u2248',
        '\u00b0', '\u2219', '\u00b7', '\u221a',
        '\u207f', '\u00b2', '\u25a0', '\u00a0'
    };

    /**
     * ░
     */
    public static final char HATCH                      = CP437[0xB0];

    /**
     * ═
     */
    public static final char DOUBLE_BAR                 = CP437[0xCD];

    /**
     * ■
     */
    public static final char BOX                        = CP437[0xFE];

    /**
     * √
     */
    public static final char CHECK                      = CP437[0xFB];

    /**
     * ≡
     */
    public static final char TRIPLET                    = CP437[0xF0];

    /**
     * Ω
     */
    public static final char OMEGA                      = CP437[0xEA];

    /**
     * π
     */
    public static final char PI                         = CP437[0xE3];

    /**
     * ↑
     */
    public static final char UPARROW                    = CP437[0x18];

    /**
     * ↓
     */
    public static final char DOWNARROW                  = CP437[0x19];

    /**
     * →
     */
    public static final char RIGHTARROW                 = CP437[0x1A];

    /**
     * ←
     */
    public static final char LEFTARROW                  = CP437[0x1B];

    /**
     * ─
     */
    public static final char SINGLE_BAR                 = CP437[0xC4];

    /**
     * ◄
     */
    public static final char BACK_ARROWHEAD             = CP437[0x11];

    /**
     * ┘
     */
    public static final char LRCORNER                   = CP437[0xD9];

    /**
     * ┐
     */
    public static final char URCORNER                   = CP437[0xBF];

    /**
     * └
     */
    public static final char LLCORNER                   = CP437[0xC0];

    /**
     * ┌
     */
    public static final char ULCORNER                   = CP437[0xDA];

    /**
     * °
     */
    public static final char DEGREE                     = CP437[0xF8];

    /**
     * ±
     */
    public static final char PLUSMINUS                  = CP437[0xF1];

    /**
     * ═
     */
    public static final char WINDOW_TOP                 = CP437[0xCD];

    /**
     * ╒
     */
    public static final char WINDOW_LEFT_TOP            = CP437[0xD5];

    /**
     * ╕
     */
    public static final char WINDOW_RIGHT_TOP           = CP437[0xB8];

    /**
     * │
     */
    public static final char WINDOW_SIDE                = CP437[0xB3];

    /**
     * ╘
     */
    public static final char WINDOW_LEFT_BOTTOM         = CP437[0xD4];

    /**
     * ╛
     */
    public static final char WINDOW_RIGHT_BOTTOM        = CP437[0xBE];

    /**
     * ╞
     */
    public static final char WINDOW_LEFT_TEE            = CP437[0xC6];

    /**
     * ╡
     */
    public static final char WINDOW_RIGHT_TEE           = CP437[0xB5];

    /**
     * ║
     */
    public static final char WINDOW_SIDE_DOUBLE         = CP437[0xBA];

    /**
     * ╔
     */
    public static final char WINDOW_LEFT_TOP_DOUBLE     = CP437[0xC9];

    /**
     * ╗
     */
    public static final char WINDOW_RIGHT_TOP_DOUBLE    = CP437[0xBB];

    /**
     * ╚
     */
    public static final char WINDOW_LEFT_BOTTOM_DOUBLE  = CP437[0xC8];

    /**
     * ╝
     */
    public static final char WINDOW_RIGHT_BOTTOM_DOUBLE = CP437[0xBC];

    /**
     * │
     */
    public static final char VERTICAL_BAR               = CP437[0xB3];

    /**
     * ☼
     */
    public static final char OCTOSTAR                   = CP437[0x0F];

    /**
     * ▌
     */
    public static final char DOWNARROWLEFT              = CP437[0xDD];

    /**
     * ▐
     */
    public static final char DOWNARROWRIGHT             = CP437[0xDE];

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Private constructor prevents accidental creation of this class.
     */
    private GraphicsChars() {}

}
