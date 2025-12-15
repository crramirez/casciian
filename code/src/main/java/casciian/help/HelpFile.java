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

import java.io.InputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A HelpFile is a collection of Topics with a table of contents and index of
 * relevant terms.
 */
public class HelpFile {

    /**
     * Translated strings.
     */
    private static final ResourceBundle i18n = ResourceBundle.getBundle(HelpFile.class.getName());

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The XML factory.
     */
    private DocumentBuilder domBuilder;

    /**
     * The map of topics by title.
     */
    private HashMap<String, Topic> topicsByTitle;

    /**
     * The map of topics by index key term.
     */
    private HashMap<String, Topic> topicsByTerm;

    /**
     * The special "table of contents" topic.
     */
    private Topic tableOfContents;

    /**
     * The special "index" topic.
     */
    private Topic index;

    /**
     * The name of this help file.
     */
    private String name = "";

    /**
     * The version of this help file.
     */
    private String version = "";

    /**
     * The help file author.
     */
    private String author = "";

    /**
     * The help file copyright/written by date.
     */
    private String date = "";

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Default constructor.
     */
    public HelpFile() {}

    // ------------------------------------------------------------------------
    // HelpFile ---------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Load a help file from an input stream.
     *
     * @param input the input strem
     * @throws IOException if an I/O error occurs
     * @throws ParserConfigurationException if no XML parser is available
     * @throws SAXException if XML parsing fails
     */
    public void load(final InputStream input) throws IOException,
                                ParserConfigurationException, SAXException {

        topicsByTitle = new HashMap<String, Topic>();
        topicsByTerm = new HashMap<String, Topic>();

        try {
            // Null check, in case input was loaded from ClassLoader from a
            // file not actually on the classpath.
            if (input != null) {
                loadTopics(input);
            }
        } finally {
            // Always generate the TOC and Index from what was read.
            generateTableOfContents();
            generateIndex();
        }
    }

    /**
     * Get a topic by title.
     *
     * @param title the title for the topic
     * @return the topic, or the "not found" topic if title is not found
     */
    public Topic getTopic(final String title) {
        Topic topic = topicsByTitle.get(title);
        if (topic == null) {
            return Topic.NOT_FOUND;
        }
        return topic;
    }

    /**
     * Get the special "search results" topic.
     *
     * @param searchString a regular expression search string
     * @return an index topic containing topics with text that matches the
     * search string
     */
    public Topic getSearchResults(final String searchString) {
        List<Topic> allTopics = new ArrayList<Topic>();
        allTopics.addAll(topicsByTitle.values());
        Collections.sort(allTopics);

        List<Topic> results = new ArrayList<Topic>();
        Pattern pattern = Pattern.compile(searchString);
        Pattern patternLower = Pattern.compile(searchString.toLowerCase());

        for (Topic topic: allTopics) {
            Matcher match = pattern.matcher(topic.getText().toLowerCase());
            if (match.find()) {
                results.add(topic);
                continue;
            }
            match = pattern.matcher(topic.getTitle().toLowerCase());
            if (match.find()) {
                results.add(topic);
                continue;
            }
            match = patternLower.matcher(topic.getText().toLowerCase());
            if (match.find()) {
                results.add(topic);
                continue;
            }
            match = patternLower.matcher(topic.getTitle().toLowerCase());
            if (match.find()) {
                results.add(topic);
                continue;
            }
        }

        StringBuilder text = new StringBuilder();
        int wordIndex = 0;
        List<Link> links = new ArrayList<Link>();
        for (Topic topic: results) {
            text.append(topic.getTitle());
            text.append("\n\n");

            Link link = new Link(topic.getTitle(), topic.getTitle(), wordIndex);
            wordIndex += link.getWordCount();
            links.add(link);
        }

        return new Topic(MessageFormat.format(i18n.getString("searchResults"),
                searchString), text.toString(), links);
    }

    /**
     * Get the special "table of contents" topic.
     *
     * @return the table of contents topic
     */
    public Topic getTableOfContents() {
        return tableOfContents;
    }

    /**
     * Get the special "index" topic.
     *
     * @return the index topic
     */
    public Topic getIndex() {
        return index;
    }

    /**
     * Generate the table of contents topic.
     */
    private void generateTableOfContents() {
        List<Topic> allTopics = new ArrayList<Topic>();
        allTopics.addAll(topicsByTitle.values());
        Collections.sort(allTopics);

        StringBuilder text = new StringBuilder();
        int wordIndex = 0;
        List<Link> links = new ArrayList<Link>();
        for (Topic topic: allTopics) {
            text.append(topic.getTitle());
            text.append("\n\n");

            Link link = new Link(topic.getTitle(), topic.getTitle(), wordIndex);
            wordIndex += link.getWordCount();
            links.add(link);
        }

        tableOfContents = new Topic(i18n.getString("tableOfContents"),
            text.toString(), links);
    }

    /**
     * Generate the index topic.
     */
    private void generateIndex() {
        List<Topic> allTopics = new ArrayList<Topic>();
        allTopics.addAll(topicsByTitle.values());

        HashMap<String, ArrayList<Topic>> allKeys;
        allKeys = new HashMap<String, ArrayList<Topic>>();
        for (Topic topic: allTopics) {
            for (String key: topic.getIndexKeys()) {
                key = key.toLowerCase();
                ArrayList<Topic> topics = allKeys.get(key);
                if (topics == null) {
                    topics = new ArrayList<Topic>();
                    allKeys.put(key, topics);
                }
                topics.add(topic);
            }
        }
        List<String> keys = new ArrayList<String>();
        keys.addAll(allKeys.keySet());
        Collections.sort(keys);

        StringBuilder text = new StringBuilder();
        int wordIndex = 0;
        List<Link> links = new ArrayList<Link>();

        for (String key: keys) {
            List<Topic> topics = allKeys.get(key);
            assert (topics != null);
            for (Topic topic: topics) {
                String line = String.format("%15s %15s", key, topic.getTitle());
                text.append(line);
                text.append("\n\n");

                wordIndex += key.split("\\s+").length;
                Link link = new Link(topic.getTitle(), topic.getTitle(), wordIndex);
                wordIndex += link.getWordCount();
                links.add(link);
            }
        }

        index = new Topic(i18n.getString("index"), text.toString(), links);
    }

    /**
     * Load topics from a help file into the topics pool.
     *
     * @param input the input strem
     * @throws IOException if an I/O error occurs
     * @throws ParserConfigurationException if no XML parser is available
     * @throws SAXException if XML parsing fails
     */
    private void loadTopics(final InputStream input) throws IOException,
                                ParserConfigurationException, SAXException {

        if (domBuilder == null) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.
                                                                newInstance();
            domBuilder = dbFactory.newDocumentBuilder();
        }
        Document doc = domBuilder.parse(input);

        // Get the document's root XML node
        Node root = doc.getChildNodes().item(0);
        NodeList level1 = root.getChildNodes();
        for (int i = 0; i < level1.getLength(); i++) {
            Node node = level1.item(i);
            String name = node.getNodeName();
            String value = node.getTextContent();

            if (name.equals("name")) {
                this.name = value;
            }
            if (name.equals("version")) {
                this.version = value;
            }
            if (name.equals("author")) {
                this.author = value;
            }
            if (name.equals("date")) {
                this.date = value;
            }
            if (name.equals("topics")) {
                NodeList topics = node.getChildNodes();
                for (int j = 0; j < topics.getLength(); j++) {
                    Node topic = topics.item(j);
                    addTopic(topic);
                }
            }
        }
    }

    /**
     * Add a topic to this help file.
     *
     * @param xmlNode the topic XML node
     * @throws IOException if a java.io operation throws
     */
    private void addTopic(final Node xmlNode) throws IOException {
        String title = "";
        String text = "";

        NamedNodeMap attributes = xmlNode.getAttributes();
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attr = attributes.item(i);
                if (attr.getNodeName().equals("title")) {
                    title = attr.getNodeValue().trim();
                }
            }
        }
        NodeList level2 = xmlNode.getChildNodes();
        for (int i = 0; i < level2.getLength(); i++) {
            Node node = level2.item(i);
            String nodeName = node.getNodeName();
            String nodeValue = node.getTextContent();
            if (nodeName.equals("text")) {
                text = nodeValue.trim();
            }
        }
        if (title.length() > 0) {
            Topic topic = new Topic(title, text);
            topicsByTitle.put(title, topic);
        }
    }

}
