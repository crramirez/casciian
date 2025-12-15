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
 * MnemonicString is used to render a string like "&amp;File" into a
 * highlighted 'F' and the rest of 'ile'.  To insert a literal '&amp;', use
 * two '&amp;&amp;' characters, e.g. "&amp;File &amp;&amp; Stuff" would be
 * "File &amp; Stuff" with the first 'F' highlighted.
 */
public class MnemonicString {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Keyboard shortcut to activate this item.
     */
    private int shortcut;

    /**
     * Location of the highlighted character.
     */
    private int shortcutIdx = -1;

    /**
     * Screen location of the highlighted character (number of text cells
     * required to display from the beginning to shortcutIdx).
     */
    private int screenShortcutIdx = -1;

    /**
     * The raw (uncolored) string.
     */
    private String rawLabel;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param label widget label or title.  Label must contain a keyboard
     * shortcut, denoted by prefixing a letter with "&amp;", e.g. "&amp;File"
     */
    public MnemonicString(final String label) {

        // Setup the menu shortcut
        StringBuilder newLabel = new StringBuilder();
        boolean foundAmp = false;
        boolean foundShortcut = false;
        int scanShortcutIdx = 0;
        int scanScreenShortcutIdx = 0;
        for (int i = 0; i < label.length();) {
            int c = label.codePointAt(i);
            i += Character.charCount(c);

            if (c == '&') {
                if (foundAmp) {
                    newLabel.append('&');
                    scanShortcutIdx++;
                    scanScreenShortcutIdx++;
                    foundAmp = false;
                } else {
                    foundAmp = true;
                }
            } else {
                newLabel.append(Character.toChars(c));
                if (foundAmp) {
                    if (!foundShortcut) {
                        shortcut = c;
                        foundAmp = false;
                        foundShortcut = true;
                        shortcutIdx = scanShortcutIdx;
                        screenShortcutIdx = scanScreenShortcutIdx;
                    }
                } else {
                    scanShortcutIdx++;
                    scanScreenShortcutIdx += StringUtils.width(c);
                }
            }
        }
        this.rawLabel = newLabel.toString();
    }

    // ------------------------------------------------------------------------
    // MnemonicString ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the keyboard shortcut character.
     *
     * @return the highlighted character
     */
    public int getShortcut() {
        return shortcut;
    }

    /**
     * Get location of the highlighted character.
     *
     * @return location of the highlighted character
     */
    public int getShortcutIdx() {
        return shortcutIdx;
    }

    /**
     * Get the screen location of the highlighted character.
     *
     * @return the number of text cells required to display from the
     * beginning of the label to shortcutIdx
     */
    public int getScreenShortcutIdx() {
        return screenShortcutIdx;
    }

    /**
     * Get the raw (uncolored) string.
     *
     * @return the raw (uncolored) string
     */
    public String getRawLabel() {
        return rawLabel;
    }

}
