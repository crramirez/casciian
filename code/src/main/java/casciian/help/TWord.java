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

import casciian.THelpWindow;
import casciian.TWidget;
import casciian.bits.CellAttributes;
import casciian.bits.StringUtils;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import static casciian.TKeypress.*;

/**
 * TWord contains either a string to display or a clickable link.
 */
public class TWord extends TWidget {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The word(s) to display.
     */
    private String words;

    /**
     * Link to another Topic.
     */
    private Link link;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param words the words to display
     * @param link link to other topic, or null
     */
    @SuppressWarnings("this-escape")
    public TWord(final String words, final Link link) {

        // TWord is created by THelpText before the TParagraph is belongs to
        // is created, so pass null as parent for now.
        super(null, 0, 0, StringUtils.width(words), 1);

        this.words = words;
        this.link = link;

        // Don't make text-only words "active".
        if (link == null) {
            setEnabled(false);
        }
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if (mouse.isMouse1()) {
            if (link != null) {
                ((THelpWindow) getWindow()).setHelpTopic(link.getTopic());
            }
        }
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.equals(kbEnter)) {
            if (link != null) {
                ((THelpWindow) getWindow()).setHelpTopic(link.getTopic());
            }
        }
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the words.
     */
    @Override
    public void draw() {
        CellAttributes color = getTheme().getColor("thelpwindow.text");
        if (link != null) {
            if (isAbsoluteActive()) {
                color = getTheme().getColor("thelpwindow.link.active");
            } else {
                color = getTheme().getColor("thelpwindow.link");
            }
        }
        putStringXY(0, 0, words, color);
    }

    // ------------------------------------------------------------------------
    // TWord ------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
