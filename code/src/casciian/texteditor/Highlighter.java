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
package casciian.texteditor;

import java.util.SortedMap;
import java.util.TreeMap;

import casciian.bits.CellAttributes;
import casciian.bits.Color;

/**
 * Highlighter provides color choices for certain text strings.
 */
public class Highlighter {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The highlighter colors.
     */
    private SortedMap<String, CellAttributes> colors;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor sets the theme to the default.
     */
    public Highlighter() {
        // NOP
    }

    // ------------------------------------------------------------------------
    // Highlighter ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set keyword highlighting.
     *
     * @param enabled if true, enable keyword highlighting
     */
    public void setEnabled(final boolean enabled) {
        if (enabled) {
            setJavaColors();
        } else {
            colors = null;
        }
    }

    /**
     * Set my field values to that's field.
     *
     * @param rhs an instance of Highlighter
     */
    public void setTo(final Highlighter rhs) {
        if (rhs.colors != null) {
            colors = new TreeMap<String, CellAttributes>();
            colors.putAll(rhs.colors);
        } else {
            colors = null;
        }
    }

    /**
     * See if this is a character that should split a word.
     *
     * @param ch the character
     * @return true if the word should be split
     */
    public boolean shouldSplit(final int ch) {
        // For now, split on punctuation
        String punctuation = "'\"\\<>{}[]!@#$%^&*();:.,-+/*?";
        if (ch < 0x100) {
            if (punctuation.indexOf((char) ch) != -1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieve the CellAttributes for a named theme color.
     *
     * @param name theme color name, e.g. "twindow.border"
     * @return color associated with name, e.g. bold yellow on blue
     */
    public CellAttributes getColor(final String name) {
        if (colors == null) {
            return null;
        }
        CellAttributes attr = colors.get(name);
        return attr;
    }

    /**
     * Sets to defaults that resemble the Borland IDE colors.
     */
    public void setJavaColors() {
        colors = new TreeMap<String, CellAttributes>();

        CellAttributes color;

        String [] types = {
            "boolean", "byte", "short", "int", "long", "char", "float",
            "double", "void",
        };
        color = new CellAttributes();
        color.setForeColor(Color.GREEN);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        for (String str: types) {
            colors.put(str, color);
        }

        String [] modifiers = {
            "abstract", "final", "native", "private", "protected", "public",
            "static", "strictfp", "synchronized", "transient", "volatile",
        };
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        for (String str: modifiers) {
            colors.put(str, color);
        }

        String [] keywords = {
            "new", "class", "interface", "extends", "implements",
            "if", "else", "do", "while", "for", "break", "continue",
            "switch", "case", "default",
        };
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        for (String str: keywords) {
            colors.put(str, color);
        }

        String [] operators = {
            "[", "]", "(", ")", "{", "}",
            "*", "-", "+", "/", "=", "%",
            "^", "&", "!", "<<", ">>", "<<<", ">>>",
            "&&", "||",
            ">", "<", ">=", "<=", "!=", "==",
            ",", ";", ".", "?", ":",
        };
        color = new CellAttributes();
        color.setForeColor(Color.CYAN);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        for (String str: operators) {
            colors.put(str, color);
        }

        String [] packageKeywords = {
            "package", "import",
        };
        color = new CellAttributes();
        color.setForeColor(Color.GREEN);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        for (String str: packageKeywords) {
            colors.put(str, color);
        }

    }

}
