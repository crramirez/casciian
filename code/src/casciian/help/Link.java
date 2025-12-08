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

import java.util.HashSet;
import java.util.Set;
import java.util.ResourceBundle;

/**
 * A Link is a section of text with a reference to a Topic.
 */
public class Link {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The topic id that this link points to.
     */
    private String topic;

    /**
     * The text inside the link tag.
     */
    private String text;

    /**
     * The number of words in this link.
     */
    private int wordCount;

    /**
     * The word number (from the beginning of topic text) that corresponds to
     * the first word of this link.
     */
    private int index;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param topic the topic to point to
     * @param text the text inside the link tag
     * @param index the word count index
     */
    public Link(final String topic, final String text, final int index) {
        this.topic = topic;
        this.text = text;
        this.index = index;
        this.wordCount = text.split("\\s+").length;
    }

    // ------------------------------------------------------------------------
    // Link -------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the topic.
     *
     * @return the topic
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Get the link text.
     *
     * @return the text inside the link tag
     */
    public String getText() {
        return text;
    }

    /**
     * Get the word index for this link.
     *
     * @return the word number (from the beginning of topic text) that
     * corresponds to the first word of this link
     */
    public int getIndex() {
        return index;
    }

    /**
     * Get the number of words in this link.
     *
     * @return the number of words in this link
     */
    public int getWordCount() {
        return wordCount;
    }

    /**
     * Generate a human-readable string for this widget.
     *
     * @return a human-readable string
     */
    @Override
    public String toString() {
        return String.format("%s(%8x) topic %s link text %s word # %d count %d",
            getClass().getName(), hashCode(), topic, text, index, wordCount);
    }

}
