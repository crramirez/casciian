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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Topic is a page of help text with a title and possibly links to other
 * Topics.
 */
public class Topic implements Comparable<Topic> {

    /**
     * Translated strings.
     */
    private static final ResourceBundle i18n = ResourceBundle.getBundle(Topic.class.getName());

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The "not found" topic to display when a key or index term does not
     * have an associated topic.  Note package private access.
     */
    static Topic NOT_FOUND = null;

    /**
     * The regex for identifying index tags.
     */
    private static final String INDEX_REGEX_STR = "\\#\\{([^\\}]*)\\}";

    /**
     * The regex for identifying link tags.
     */
    private static final String LINK_REGEX_STR = "\\[([^\\]]*)\\]\\(([^\\)]*)\\)";

    /**
     * The regex for identifying words.
     */
    private static final String WORD_REGEX_STR = "[ \\t]+";

    /**
     * The index match regex.
     */
    private static Pattern INDEX_REGEX;

    /**
     * The link match regex.
     */
    private static Pattern LINK_REGEX;

    /**
     * The word match regex.
     */
    private static Pattern WORD_REGEX;

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The title for this topic.
     */
    private String title;

    /**
     * The text for this topic.
     */
    private String text;

    /**
     * The index keys in this topic.
     */
    private Set<String> indexKeys = new HashSet<String>();

    /**
     * The links in this topic.
     */
    private List<Link> links = new ArrayList<Link>();

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Static constructor.
     */
    static {
        try {
            INDEX_REGEX = Pattern.compile(INDEX_REGEX_STR);
            LINK_REGEX = Pattern.compile(LINK_REGEX_STR);
            WORD_REGEX = Pattern.compile(WORD_REGEX_STR);

            NOT_FOUND = new Topic(i18n.getString("topicNotFoundTitle"),
                i18n.getString("topicNotFoundText"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Public constructor.
     *
     * @param title the topic title
     * @param text the topic text
     */
    public Topic(final String title, final String text) {
        this.title = title;
        processText(text);
    }

    /**
     * Package private constructor.
     *
     * @param title the topic title
     * @param text the topic text
     * @param links links to add after processing text
     */
    Topic(final String title, final String text, final List<Link> links) {
        this.title = title;
        processText(text);
        this.links.addAll(links);
    }

    // ------------------------------------------------------------------------
    // Topic ------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the topic title.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Get the topic text.
     *
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * Get the index keys.
     *
     * @return the keys
     */
    public Set<String> getIndexKeys() {
        return indexKeys;
    }

    /**
     * Get the links.
     *
     * @return the links
     */
    public List<Link> getLinks() {
        return links;
    }

    /**
     * Comparison operator.
     *
     * @param that another Topic instance
     * @return comparison by topic title
     */
    public int compareTo(final Topic that) {
        return title.compareTo(that.title);
    }

    /**
     * Generate a human-readable string for this widget.
     *
     * @return a human-readable string
     */
    @Override
    public String toString() {
        return String.format("%s(%8x) topic %s text %s links %s indexKeys %s",
            getClass().getName(), hashCode(), title, text, links, indexKeys);
    }

    /**
     * Process a string through the regexes, building up the indexes and
     * links.
     *
     * @param text the text to process
     */
    private void processText(final String text) {
        StringBuilder sb = new StringBuilder();
        String [] lines = text.split("\n");
        int wordIndex = 0;
        for (String line: lines) {
            line = line.trim();

            String cleanLine = "";

            // System.err.println("LINE " + wordIndex + " : '" + line + "'");

            Matcher index = INDEX_REGEX.matcher(line);
            int start = 0;
            while (index.find()) {
                cleanLine += line.substring(start, index.start());
                String key = index.group(1);
                cleanLine += key;
                start = index.end();
                // System.err.println("ADD KEY: " + key);
                indexKeys.add(key);
            }
            cleanLine += line.substring(start);

            line = cleanLine;
            cleanLine = "";

            /*
            System.err.println("line after removing #{index} tags: " +
                wordIndex + " '" + line + "'");
            */

            Matcher link = LINK_REGEX.matcher(line);
            start = 0;

            boolean hasLink = link.find();

            // System.err.println("hasLink " + hasLink);

            while (true) {

                if (hasLink == false) {
                    cleanLine += line.substring(start);

                    String remaining = line.substring(start).trim();
                    Matcher word = WORD_REGEX.matcher(remaining);
                    while (word.find()) {
                        // System.err.println("word.find() true");
                        wordIndex++;
                    }
                    if (remaining.length() > 0) {
                        // The last word on the line.
                        wordIndex++;
                    }
                    break;
                }

                assert (hasLink == true);

                int linkWordIndex = link.start();
                int cleanLineStart = cleanLine.length();
                cleanLine += line.substring(start, linkWordIndex);
                String linkText = link.group(1);
                String topic = link.group(2);
                cleanLine += linkText;
                start = link.end();

                // Increment wordIndex until we reach the first word of
                // the link text.
                Matcher word = WORD_REGEX.matcher(cleanLine.
                    substring(cleanLineStart));
                while (word.find()) {
                    if (word.end() <= linkWordIndex) {
                        wordIndex++;
                    } else {
                        // We have found the word that matches the first
                        // word of link text, bail out.
                        break;
                    }
                }
                /*
                System.err.println("ADD LINK --> " + topic + ": '" +
                    linkText + "' word index " + wordIndex);
                */
                links.add(new Link(topic, linkText, wordIndex));

                // The rest of the words in the link text.
                while (word.find()) {
                    wordIndex++;
                }
                // The final word after the last whitespace.
                wordIndex++;

                hasLink = link.find();
                if (hasLink) {
                    wordIndex += 3;
                }
            }


            /*
            System.err.println("line after removing [link](...) tags: '" +
                cleanLine + "'");
            */

            // Append the entire line.
            sb.append(cleanLine);
            sb.append("\n");

            this.text = sb.toString();

        } // for (String line: lines)

    }

}
