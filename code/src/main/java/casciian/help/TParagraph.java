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
package casciian.help;

import java.util.List;

import casciian.TWidget;

/**
 * TParagraph contains a reflowable collection of TWords, some of which are
 * clickable links.
 */
public class TParagraph extends TWidget {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Topic text and links converted to words.
     */
    private List<TWord> words;

    /**
     * If true, add one row to height as a paragraph separator.  Note package
     * private access.
     */
    boolean separator = true;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param words the pieces of the paragraph to display
     */
    @SuppressWarnings("this-escape")
    public TParagraph(final THelpText parent, final List<TWord> words) {

        // Set parent and window
        super(parent, 0, 0, parent.getWidth() - 1, 1);

        this.words = words;
        for (TWord word: words) {
            word.setParent(this, false);
        }

        reflowData();
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // TParagraph -------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Reposition the words in this paragraph to reflect the new width, and
     * set the paragraph height.
     */
    public void reflowData() {
        int x = 0;
        int y = 0;
        for (TWord word: words) {
            if (x + word.getWidth() >= getWidth()) {
                x = 0;
                y++;
            }
            word.setX(x);
            word.setY(y);
            x += word.getWidth() + 1;
        }
        if (separator) {
            setHeight(y + 2);
        } else {
            setHeight(y + 1);
        }
    }

    /**
     * Try to select a previous link.
     *
     * @return true if there was a previous link in this paragraph to select
     */
    public boolean up() {
        if (words.size() == 0) {
            return false;
        }
        if (getActiveChild() == this) {
            // No selectable links
            return false;
        }
        TWord firstWord = null;
        TWord lastWord = null;
        for (TWord word: words) {
            if (word.isEnabled()) {
                if (firstWord == null) {
                    firstWord = word;
                }
                lastWord = word;
            }
        }
        if (getActiveChild() == firstWord) {
            return false;
        }
        switchWidget(false);
        return true;
    }

    /**
     * Try to select a next link.
     *
     * @return true if there was a next link in this paragraph to select
     */
    public boolean down() {
        if (words.size() == 0) {
            return false;
        }
        if (getActiveChild() == this) {
            // No selectable links
            return false;
        }
        TWord firstWord = null;
        TWord lastWord = null;
        for (TWord word: words) {
            if (word.isEnabled()) {
                if (firstWord == null) {
                    firstWord = word;
                }
                lastWord = word;
            }
        }
        if (getActiveChild() == lastWord) {
            return false;
        }
        switchWidget(true);
        return true;
    }


}
