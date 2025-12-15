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

import java.io.IOException;

/**
 * Clipboard provides convenience methods to copy text to and from a shared
 * clipboard.
 */
public class Clipboard {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The text string last copied to the clipboard.
     */
    private String text = null;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     */
    public Clipboard() {
    }

    // ------------------------------------------------------------------------
    // Clipboard --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Copy a text string to the clipboard.
     *
     * @param text string to copy
     */
    public void copyText(final String text) {
        this.text = text;
    }

    /**
     * Obtain a text string from the clipboard.
     *
     * @return text string from the clipboard, or null if no text is
     * available
     */
    public String pasteText() {
        return text;
    }

    /**
     * Returns true if the clipboard has a text string.
     *
     * @return true if a text string is available from the clipboard
     */
    public boolean isText() {
        return (text != null);
    }

    /**
     * Returns true if the clipboard is empty.
     *
     * @return true if the clipboard is empty
     */
    public boolean isEmpty() {
        return (isText() == false);
    }

    /**
     * Clear whatever is on the local clipboard.
     */
    public void clear() {
        text = null;
    }

}
