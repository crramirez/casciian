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

import java.util.ArrayList;
import java.util.List;

import casciian.THelpWindow;
import casciian.TScrollable;
import casciian.TVScroller;
import casciian.TWidget;
import casciian.bits.CellAttributes;
import casciian.bits.StringUtils;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import static casciian.TKeypress.*;

/**
 * THelpText displays help text with clickable links in a scrollable text
 * area. It reflows automatically on resize.
 */
public class THelpText extends TScrollable {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The number of lines to scroll on mouse wheel up/down.
     */
    private static final int wheelScrollSize = 3;

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The paragraphs in this text box.
     */
    private List<TParagraph> paragraphs;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param topic the topic to display
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     */
    @SuppressWarnings("this-escape")
    public THelpText(final THelpWindow parent, final Topic topic, final int x,
        final int y, final int width, final int height) {

        // Set parent and window
        super(parent, x, y, width, height);

        vScroller = new TVScroller(this, getWidth() - 1, 0,
            Math.max(1, getHeight()));

        setTopic(topic);
    }

    // ------------------------------------------------------------------------
    // TScrollable ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Override TWidget's width: we need to set child widget widths.
     *
     * @param width new widget width
     */
    @Override
    public void setWidth(final int width) {
        super.setWidth(width);
        if (hScroller != null) {
            hScroller.setWidth(getWidth() - 1);
        }
        if (vScroller != null) {
            vScroller.setX(getWidth() - 1);
        }
    }

    /**
     * Override TWidget's height: we need to set child widget heights.
     * time.
     *
     * @param height new widget height
     */
    @Override
    public void setHeight(final int height) {
        super.setHeight(height);
        if (hScroller != null) {
            hScroller.setY(getHeight() - 1);
        }
        if (vScroller != null) {
            vScroller.setHeight(Math.max(1, getHeight()));
        }
    }

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        // Pass to children
        super.onMouseDown(mouse);

        if (mouse.isMouseWheelUp()) {
            for (int i = 0; i < wheelScrollSize; i++) {
                vScroller.decrement();
            }
            reflowData();
            return;
        }
        if (mouse.isMouseWheelDown()) {
            for (int i = 0; i < wheelScrollSize; i++) {
                vScroller.increment();
            }
            reflowData();
            return;
        }

        // User clicked on a paragraph, update the scrollbar accordingly.
        for (int i = 0; i < paragraphs.size(); i++) {
            if (paragraphs.get(i).isActive()) {
                setVerticalValue(i);
                return;
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
        if (keypress.equals(kbTab)) {
            getParent().switchWidget(true);
        } else if (keypress.equals(kbShiftTab)) {
            getParent().switchWidget(false);
        } else if (keypress.equals(kbUp)) {
            if (!paragraphs.get(getVerticalValue()).up()) {
                vScroller.decrement();
                reflowData();
            }
        } else if (keypress.equals(kbDown)) {
            if (!paragraphs.get(getVerticalValue()).down()) {
                vScroller.increment();
                reflowData();
            }
        } else if (keypress.equals(kbPgUp)) {
            vScroller.bigDecrement();
            reflowData();
        } else if (keypress.equals(kbPgDn)) {
            vScroller.bigIncrement();
            reflowData();
        } else if (keypress.equals(kbHome)) {
            vScroller.toTop();
            reflowData();
        } else if (keypress.equals(kbEnd)) {
            vScroller.toBottom();
            reflowData();
        } else {
            // Pass other keys on
            super.onKeypress(keypress);
        }
    }

    /**
     * Place the scrollbars on the edge of this widget, and adjust bigChange
     * to match the new size.  This is called by onResize().
     */
    protected void placeScrollbars() {
        if (hScroller != null) {
            hScroller.setY(getHeight() - 1);
            hScroller.setWidth(getWidth() - 1);
            hScroller.setBigChange(getWidth() - 1);
        }
        if (vScroller != null) {
            vScroller.setX(getWidth() - 1);
            vScroller.setHeight(getHeight());
            vScroller.setBigChange(getHeight());
        }
    }

    /**
     * Resize text and scrollbars for a new width/height.
     */
    @Override
    public void reflowData() {
        for (TParagraph paragraph: paragraphs) {
            paragraph.setWidth(getWidth() - 1);
            paragraph.reflowData();
        }

        int top = getVerticalValue();
        int paragraphsHeight = 0;
        for (TParagraph paragraph: paragraphs) {
            paragraphsHeight += paragraph.getHeight();
        }
        if (paragraphsHeight <= getHeight()) {
            // All paragraphs fit in the window.
            int y = 0;
            for (int i = 0; i < paragraphs.size(); i++) {
                paragraphs.get(i).setEnabled(true);
                paragraphs.get(i).setVisible(true);
                paragraphs.get(i).setY(y);
                y += paragraphs.get(i).getHeight();
            }
            activate(paragraphs.get(getVerticalValue()));
            return;
        }

        /*
         * Some paragraphs will not fit in the window.  Find the number of
         * rows needed to display from the current vertical position to the
         * end:
         *
         * - If this meets or exceeds the available height, then draw from
         *   the vertical position to the number of visible rows.
         *
         * - If this is less than the available height, back up until
         *   meeting/exceeding the height, and draw from there to the end.
         *
         */
        int rowsNeeded = 0;
        for (int i = getVerticalValue(); i <= getBottomValue(); i++) {
            rowsNeeded += paragraphs.get(i).getHeight();
        }
        while (rowsNeeded < getHeight()) {
            // Decrease top until we meet/exceed the visible display.
            if (top == getTopValue()) {
                break;
            }
            top--;
            rowsNeeded += paragraphs.get(top).getHeight();
        }

        // All set, now disable all paragraphs except the visible ones.
        for (TParagraph paragraph: paragraphs) {
            paragraph.setEnabled(false);
            paragraph.setVisible(false);
            paragraph.setY(-1);
        }
        int y = 0;
        for (int i = top; (i <= getBottomValue()) && (y < getHeight()); i++) {
            paragraphs.get(i).setEnabled(true);
            paragraphs.get(i).setVisible(true);
            paragraphs.get(i).setY(y);
            y += paragraphs.get(i).getHeight();
        }
        activate(paragraphs.get(getVerticalValue()));
    }

    /**
     * Draw the text box.
     */
    @Override
    public void draw() {
        // Setup my color
        CellAttributes color = getTheme().getColor("thelpwindow.text");
        for (int y = 0; y < getHeight(); y++) {
            hLineXY(0, y, getWidth(), ' ', color);
        }
    }

    // ------------------------------------------------------------------------
    // THelpText --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set the topic.
     *
     * @param topic new topic to display
     */
    public void setTopic(final Topic topic) {
        setTopic(topic, true);
    }

    /**
     * Set the topic.
     *
     * @param topic new topic to display
     * @param separator if true, separate paragraphs
     */
    public void setTopic(final Topic topic, final boolean separator) {

        if (paragraphs != null) {
            getChildren().removeAll(paragraphs);
        }
        paragraphs = new ArrayList<TParagraph>();

        // Add title paragraph at top.  We explicitly set the separator to
        // false to achieve the underscore effect.
        List<TWord> title = new ArrayList<TWord>();
        title.add(new TWord(topic.getTitle(), null));
        TParagraph titleParagraph = new TParagraph(this, title);
        titleParagraph.separator = false;
        paragraphs.add(titleParagraph);
        title = new ArrayList<TWord>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < topic.getTitle().length(); i++) {
            sb.append('\u2580');
        }
        title.add(new TWord(sb.toString(), null));
        titleParagraph = new TParagraph(this, title);
        paragraphs.add(titleParagraph);

        // Now add the actual text as paragraphs.
        int wordIndex = 0;

        // Break up text into paragraphs
        String [] blocks = topic.getText().split("\n\n");
        for (String block: blocks) {
            List<TWord> words = new ArrayList<TWord>();
            String [] lines = block.split("\n");
            for (String line: lines) {
                line = line.trim();
                // System.err.println("line: " + line);
                String [] wordTokens = line.split("\\s+");
                for (int i = 0; i < wordTokens.length; i++) {
                    String wordStr = wordTokens[i].trim();
                    Link wordLink = null;
                    for (Link link: topic.getLinks()) {
                        if ((i + wordIndex >= link.getIndex())
                            && (i + wordIndex < link.getIndex() + link.getWordCount())
                        ) {
                            // This word is part of a link.
                            wordLink = link;
                            wordStr = link.getText();
                            i += link.getWordCount() - 1;
                            break;
                        }
                    }
                    TWord word = new TWord(wordStr, wordLink);
                    /*
                    System.err.println("add word at " + (i + wordIndex) + " : "
                        + wordStr + " " + wordLink);
                     */
                    words.add(word);
                } // for (int i = 0; i < words.length; i++)
                wordIndex += wordTokens.length;
            } // for (String line: lines)
            TParagraph paragraph = new TParagraph(this, words);
            paragraph.separator = separator;
            paragraphs.add(paragraph);
        } // for (String block: blocks)

        setBottomValue(paragraphs.size() - 1);
        setVerticalValue(0);
        reflowData();
    }

}
