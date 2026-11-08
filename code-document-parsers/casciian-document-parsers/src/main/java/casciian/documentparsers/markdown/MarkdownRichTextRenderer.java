/*
 * Casciian Document Parsers add-on
 *
 * Copyright 2025 Carlos Rafael Ramirez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package casciian.documentparsers.markdown;

import casciian.bits.CellAttributes;
import casciian.bits.Color;
import casciian.bits.RichText;
import casciian.bits.StringUtils;
import org.commonmark.ext.gfm.strikethrough.Strikethrough;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.task.list.items.TaskListItemMarker;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Block;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.CustomBlock;
import org.commonmark.node.CustomNode;
import org.commonmark.node.Emphasis;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Image;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.ListBlock;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.node.ThematicBreak;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts a commonmark-java AST into a {@link RichText}.  Block structure is
 * expressed with line feeds and line prefixes (list indentation, block quote
 * bars), inline styles with {@link CellAttributes}, and images with
 * {@link RichText.Builder#image(String, String, CellAttributes)} runs whose
 * paths are resolved against the document's base directory.
 */
final class MarkdownRichTextRenderer extends AbstractVisitor {

    /**
     * Width of a thematic break, in columns.
     */
    private static final int RULE_WIDTH = 40;

    /**
     * Bullet glyphs, cycled by list nesting depth.
     */
    private static final String[] BULLETS = { "\u2022", "\u25e6", "\u25aa" };

    private static final Pattern HTML_IMG = Pattern.compile(
        "<img\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_BR = Pattern.compile(
        "<br\\s*/?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_COMMENT = Pattern.compile(
        "<!--.*?-->", Pattern.DOTALL);
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");

    /**
     * A line prefix applied after every line feed (list indentation, block
     * quote bar).
     *
     * @param text the prefix text
     * @param attributes the prefix attributes
     */
    private record Prefix(String text, CellAttributes attributes) {
    }

    private final RichText.Builder out = RichText.builder();
    private final Path baseDirectory;

    /**
     * When true (table cells), images are rendered as their alternative text
     * and line breaks as spaces.
     */
    private final boolean inlineOnly;

    private final Deque<CellAttributes> styles = new ArrayDeque<>();
    private final Deque<Integer> orderedListNumbers = new ArrayDeque<>();
    private final List<Prefix> prefixes = new ArrayList<>();
    private boolean lineStart = true;
    private boolean afterImage = false;
    private boolean anyOutput = false;
    private int pendingNewlines = 0;
    private int listDepth = 0;

    /**
     * Construct a renderer.
     *
     * @param baseDirectory directory for relative image paths, or null
     * @param base the base attributes for all text
     * @param inlineOnly true to render images as text and breaks as spaces
     */
    MarkdownRichTextRenderer(final Path baseDirectory,
        final CellAttributes base, final boolean inlineOnly) {

        this.baseDirectory = baseDirectory;
        this.inlineOnly = inlineOnly;
        styles.push(new CellAttributes(base));
    }

    /**
     * Render a node tree.
     *
     * @param node the root node (usually a Document)
     * @return the rich text
     */
    RichText render(final Node node) {
        node.accept(this);
        return out.build();
    }

    /**
     * Create the default base attributes: the widget's default colors.
     *
     * @return default attributes
     */
    static CellAttributes defaultAttributes() {
        CellAttributes attr = new CellAttributes();
        attr.setDefaultColor(true, true);
        attr.setDefaultColor(false, true);
        return attr;
    }

    // ------------------------------------------------------------------------
    // Block nodes ------------------------------------------------------------
    // ------------------------------------------------------------------------

    @Override
    public void visit(final Paragraph paragraph) {
        visitChildren(paragraph);
        endBlock(paragraph);
    }

    @Override
    public void visit(final Heading heading) {
        int level = heading.getLevel();
        withStyle(attr -> {
            attr.setBold(true);
            switch (level) {
            case 1 -> foreColor(attr, Color.YELLOW);
            case 2 -> foreColor(attr, Color.CYAN);
            case 3 -> foreColor(attr, Color.GREEN);
            default -> {
                // Lower levels are bold only.
            }
            }
        }, () -> visitChildren(heading));
        if (level <= 2) {
            int width = Math.max(1, StringUtils.width(plainText(heading)));
            newline();
            CellAttributes rule = new CellAttributes(styles.peek());
            foreColor(rule, level == 1 ? Color.YELLOW : Color.CYAN);
            text((level == 1 ? "\u2550" : "\u2500").repeat(width), rule);
        }
        endBlock(heading);
    }

    @Override
    public void visit(final ThematicBreak thematicBreak) {
        CellAttributes rule = new CellAttributes(styles.peek());
        foreColor(rule, Color.CYAN);
        text("\u2500".repeat(RULE_WIDTH), rule);
        endBlock(thematicBreak);
    }

    @Override
    public void visit(final BlockQuote blockQuote) {
        // The separator before the quote must not carry the quote bar.
        flushPending();
        CellAttributes bar = new CellAttributes(styles.peek());
        bar.setBold(true);
        foreColor(bar, Color.MAGENTA);
        prefixes.add(new Prefix("\u2502 ", bar));
        withStyle(attr -> attr.setItalic(true),
            () -> visitChildren(blockQuote));
        prefixes.remove(prefixes.size() - 1);
        endBlock(blockQuote);
    }

    @Override
    public void visit(final BulletList bulletList) {
        visitList(bulletList);
    }

    @Override
    public void visit(final OrderedList orderedList) {
        visitList(orderedList);
    }

    @Override
    public void visit(final ListItem listItem) {
        String marker = listMarker(listItem);
        CellAttributes markerAttr = new CellAttributes(styles.peek());
        markerAttr.setBold(true);
        foreColor(markerAttr, Color.YELLOW);
        text(marker, markerAttr);
        prefixes.add(new Prefix(" ".repeat(StringUtils.width(marker)),
            styles.peek()));
        visitChildren(listItem);
        prefixes.remove(prefixes.size() - 1);
        endBlock(listItem);
    }

    @Override
    public void visit(final FencedCodeBlock codeBlock) {
        renderCode(codeBlock.getLiteral(), codeBlock);
    }

    @Override
    public void visit(final IndentedCodeBlock codeBlock) {
        renderCode(codeBlock.getLiteral(), codeBlock);
    }

    @Override
    public void visit(final HtmlBlock htmlBlock) {
        renderHtml(htmlBlock.getLiteral(), true);
        endBlock(htmlBlock);
    }

    @Override
    public void visit(final CustomBlock customBlock) {
        if (customBlock instanceof TableBlock table) {
            renderTable(table);
            endBlock(table);
        } else {
            visitChildren(customBlock);
            endBlock(customBlock);
        }
    }

    // ------------------------------------------------------------------------
    // Inline nodes -----------------------------------------------------------
    // ------------------------------------------------------------------------

    @Override
    public void visit(final Text textNode) {
        text(textNode.getLiteral(), styles.peek());
    }

    @Override
    public void visit(final Emphasis emphasis) {
        withStyle(attr -> attr.setItalic(true),
            () -> visitChildren(emphasis));
    }

    @Override
    public void visit(final StrongEmphasis strongEmphasis) {
        withStyle(attr -> attr.setBold(true),
            () -> visitChildren(strongEmphasis));
    }

    @Override
    public void visit(final Code code) {
        CellAttributes attr = new CellAttributes(styles.peek());
        codeColors(attr);
        text(code.getLiteral(), attr);
    }

    @Override
    public void visit(final Link link) {
        String destination = link.getDestination();
        withStyle(attr -> {
            attr.setUnderlineStyle(CellAttributes.UNDERLINE_STYLE_SINGLE);
            foreColor(attr, Color.CYAN);
            if (destination != null && !destination.isEmpty()) {
                attr.setHyperlink(resolveLinkDestination(destination));
            }
        }, () -> visitChildren(link));
    }

    @Override
    public void visit(final Image image) {
        image(image.getDestination(), plainText(image));
    }

    @Override
    public void visit(final SoftLineBreak softLineBreak) {
        text(" ", styles.peek());
    }

    @Override
    public void visit(final HardLineBreak hardLineBreak) {
        if (inlineOnly) {
            text(" ", styles.peek());
        } else {
            newline();
        }
    }

    @Override
    public void visit(final HtmlInline htmlInline) {
        renderHtml(htmlInline.getLiteral(), false);
    }

    @Override
    public void visit(final CustomNode customNode) {
        if (customNode instanceof Strikethrough) {
            withStyle(attr -> attr.setStrikethrough(true),
                () -> visitChildren(customNode));
        } else if (customNode instanceof TaskListItemMarker marker) {
            CellAttributes attr = new CellAttributes(styles.peek());
            attr.setBold(true);
            foreColor(attr, marker.isChecked() ? Color.GREEN : Color.WHITE);
            text(marker.isChecked() ? "[x] " : "[ ] ", attr);
        } else {
            visitChildren(customNode);
        }
    }

    // ------------------------------------------------------------------------
    // Output primitives ------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Append text, first emitting pending block separators and the line
     * prefixes when at the start of a line.
     *
     * @param text the text (should not contain line feeds)
     * @param attr the attributes
     */
    private void text(final String text, final CellAttributes attr) {
        if (text == null || text.isEmpty()) {
            return;
        }
        flushPending();
        if (lineStart) {
            for (Prefix prefix : prefixes) {
                out.append(prefix.text(), prefix.attributes());
            }
            lineStart = false;
        }
        afterImage = false;
        out.append(text, attr);
        anyOutput = true;
    }

    /**
     * End the current line.  Blank lines inside prefixed blocks keep the
     * visible part of the prefixes (e.g. block quote bars).
     */
    private void newline() {
        if (inlineOnly) {
            return;
        }
        if (afterImage) {
            // The layout already ends the row after an image block and
            // absorbs exactly one line feed there.
            out.append("\n", styles.getLast());
            afterImage = false;
            return;
        }
        if (lineStart) {
            for (Prefix prefix : prefixes) {
                String visible = prefix.text().stripTrailing();
                if (!visible.isEmpty()) {
                    out.append(visible, prefix.attributes());
                }
            }
        }
        out.append("\n", styles.getLast());
        lineStart = true;
    }

    /**
     * Emit the line feeds requested by the previous block.
     */
    private void flushPending() {
        int count = pendingNewlines;
        pendingNewlines = 0;
        for (int i = 0; i < count; i++) {
            newline();
        }
    }

    /**
     * Request the separator that follows a block: one line feed inside tight
     * lists, otherwise a blank line.  The separator is only written if more
     * content follows.
     *
     * @param block the block that just ended
     */
    private void endBlock(final Node block) {
        if (anyOutput) {
            pendingNewlines = isTight(block) ? 1 : 2;
        }
    }

    /**
     * Whether a block belongs to a tight list.
     *
     * @param block the block
     * @return true if separated by a single line feed
     */
    private static boolean isTight(final Node block) {
        Node parent = block.getParent();
        if (block instanceof ListItem && parent instanceof ListBlock list) {
            return list.isTight();
        }
        if (parent instanceof ListItem
            && parent.getParent() instanceof ListBlock list) {
            return list.isTight();
        }
        return false;
    }

    /**
     * Apply a style modification for the duration of an action.
     *
     * @param modifier changes applied to a copy of the current style
     * @param action the action to run with the new style
     */
    private void withStyle(final Consumer<CellAttributes> modifier,
        final Runnable action) {

        CellAttributes attr = new CellAttributes(styles.peek());
        modifier.accept(attr);
        styles.push(attr);
        try {
            action.run();
        } finally {
            styles.pop();
        }
    }

    /**
     * Set a named foreground color, overriding the widget default.
     *
     * @param attr the attributes to change
     * @param color the color
     */
    private static void foreColor(final CellAttributes attr,
        final Color color) {

        attr.setForeColor(color);
        attr.setDefaultColor(true, false);
    }

    /**
     * Apply the colors used for code spans and code blocks.
     *
     * @param attr the attributes to change
     */
    private static void codeColors(final CellAttributes attr) {
        attr.setForeColor(Color.WHITE);
        attr.setBackColor(Color.BLACK);
        attr.setDefaultColor(true, false);
        attr.setDefaultColor(false, false);
    }

    /**
     * Resolve a relative link destination against the document directory.
     *
     * @param destination the link destination
     * @return the destination as an absolute file URI when it is relative
     */
    private String resolveLinkDestination(final String destination) {
        try {
            URI uri = new URI(destination);
            if (uri.isAbsolute() || uri.getRawAuthority() != null
                || baseDirectory == null) {
                return destination;
            }
            URI directory = baseDirectory.toAbsolutePath().normalize().toUri();
            if (!directory.getPath().endsWith("/")) {
                directory = new URI(directory.getScheme(),
                    directory.getAuthority(), directory.getPath() + "/",
                    null, null);
            }
            URI resolved = directory.resolve(uri).normalize();
            String fileUri = Path.of(resolved.getPath()).toAbsolutePath()
                .normalize().toUri().toString();
            if (resolved.getRawQuery() != null) {
                fileUri += "?" + resolved.getRawQuery();
            }
            if (resolved.getRawFragment() != null) {
                fileUri += "#" + resolved.getRawFragment();
            }
            return fileUri;
        } catch (URISyntaxException | IllegalArgumentException e) {
            return destination;
        }
    }

    // ------------------------------------------------------------------------
    // Lists ------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Render a bullet or ordered list.
     *
     * @param list the list
     */
    private void visitList(final ListBlock list) {
        listDepth++;
        if (list instanceof OrderedList ordered) {
            orderedListNumbers.push(ordered.getMarkerStartNumber() == null
                ? 1 : ordered.getMarkerStartNumber());
        }
        visitChildren(list);
        if (list instanceof OrderedList) {
            orderedListNumbers.pop();
        }
        listDepth--;
        endBlock(list);
    }

    /**
     * Compute the marker of a list item, including the trailing space.
     *
     * @param item the list item
     * @return the marker text
     */
    private String listMarker(final ListItem item) {
        if (item.getParent() instanceof OrderedList ordered) {
            int number = orderedListNumbers.pop();
            orderedListNumbers.push(number + 1);
            String delimiter = ordered.getMarkerDelimiter() == null
                ? "." : ordered.getMarkerDelimiter();
            return number + delimiter + " ";
        }
        int depth = Math.max(0, listDepth - 1);
        return BULLETS[depth % BULLETS.length] + " ";
    }

    // ------------------------------------------------------------------------
    // Code blocks ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Render a code block as a solid panel, padding every line to the width
     * of the longest one.
     *
     * @param literal the code
     * @param block the code block node
     */
    private void renderCode(final String literal, final Block block) {
        String code = literal == null ? "" : literal.replace("\t", "    ");
        if (code.endsWith("\n")) {
            code = code.substring(0, code.length() - 1);
        }
        String[] lines = code.split("\n", -1);
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, StringUtils.width(line));
        }
        CellAttributes attr = new CellAttributes(styles.getLast());
        codeColors(attr);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                newline();
            }
            String line = lines[i];
            text(" " + line + " ".repeat(width - StringUtils.width(line) + 1),
                attr);
        }
        endBlock(block);
    }

    // ------------------------------------------------------------------------
    // Images -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Render an image.  Local images become image runs; remote images (which
     * the widgets cannot load) become a hyperlink showing the alternative
     * text.
     *
     * @param destination the image destination from the document
     * @param alt the alternative text
     */
    private void image(final String destination, final String alt) {
        String path = resolveImagePath(destination);
        String label = alt == null || alt.isBlank()
            ? fileName(destination) : alt;
        CellAttributes attr = new CellAttributes(styles.peek());
        attr.setItalic(true);
        foreColor(attr, Color.MAGENTA);
        if (path == null || inlineOnly) {
            if (destination != null && !destination.isBlank()
                && path == null) {
                attr.setUnderlineStyle(CellAttributes.UNDERLINE_STYLE_SINGLE);
                attr.setHyperlink(destination);
            }
            text("[" + label + "]", attr);
            return;
        }
        flushPending();
        out.image(path, "[" + label + "]", attr);
        anyOutput = true;
        lineStart = true;
        afterImage = true;
    }

    /**
     * Resolve an image destination to a local file path.
     *
     * @param destination the destination (relative path, absolute path,
     *     percent-encoded path, or {@code file:} URI)
     * @return the resolved path, or null for remote or invalid destinations
     */
    String resolveImagePath(final String destination) {
        if (destination == null || destination.isBlank()) {
            return null;
        }
        String location = destination.trim();
        try {
            URI uri = new URI(location);
            String scheme = uri.getScheme();
            // A single-letter "scheme" is a Windows drive letter.
            if (scheme != null && scheme.length() > 1) {
                if (!"file".equalsIgnoreCase(scheme)) {
                    return null;
                }
                return Path.of(uri).normalize().toString();
            }
            if (scheme == null && uri.getRawAuthority() != null) {
                return null;
            }
            if (scheme == null && uri.getPath() != null
                && !uri.getPath().isEmpty()) {
                location = uri.getPath();
            }
        } catch (URISyntaxException | IllegalArgumentException e) {
            // Not a URI (e.g. contains spaces): treat as a plain path.
        }
        try {
            Path path = Path.of(location);
            if (!path.isAbsolute() && baseDirectory != null) {
                path = baseDirectory.resolve(path);
            }
            return path.normalize().toString();
        } catch (InvalidPathException e) {
            return null;
        }
    }

    /**
     * Get the last path segment of a destination, for unlabeled images.
     *
     * @param destination the destination
     * @return the file name, or "image"
     */
    private static String fileName(final String destination) {
        if (destination == null || destination.isBlank()) {
            return "image";
        }
        String trimmed = destination.trim();
        int query = trimmed.indexOf('?');
        if (query >= 0) {
            trimmed = trimmed.substring(0, query);
        }
        String name = trimmed.substring(Math.max(trimmed.lastIndexOf('/'),
            trimmed.lastIndexOf('\\')) + 1);
        return name.isEmpty() ? "image" : name;
    }

    // ------------------------------------------------------------------------
    // HTML -------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Render raw HTML: {@code <img>} tags become images, {@code <br>} tags
     * line breaks, other tags are dropped and remaining text is kept.
     *
     * @param html the HTML literal
     * @param block true for an HTML block (keeps its line structure)
     */
    private void renderHtml(final String html, final boolean block) {
        if (html == null) {
            return;
        }
        String source = HTML_COMMENT.matcher(html).replaceAll("");
        Matcher img = HTML_IMG.matcher(source);
        int last = 0;
        while (img.find()) {
            renderHtmlText(source.substring(last, img.start()), block);
            String tag = img.group();
            image(htmlAttribute(tag, "src"), htmlAttribute(tag, "alt"));
            last = img.end();
        }
        renderHtmlText(source.substring(last), block);
    }

    /**
     * Render the text content of an HTML fragment.
     *
     * @param html the fragment, without {@code <img>} tags
     * @param block true to keep non-blank lines of an HTML block
     */
    private void renderHtmlText(final String html, final boolean block) {
        String[] parts = HTML_BR.split(html, -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                if (inlineOnly) {
                    text(" ", styles.peek());
                } else {
                    newline();
                }
            }
            String content = decodeEntities(
                HTML_TAG.matcher(parts[i]).replaceAll(""));
            if (block) {
                boolean first = true;
                for (String line : content.split("\n")) {
                    if (line.isBlank()) {
                        continue;
                    }
                    if (!first) {
                        newline();
                    }
                    text(line.strip(), styles.peek());
                    first = false;
                }
            } else {
                text(content, styles.peek());
            }
        }
    }

    /**
     * Get an attribute value from an HTML start tag.
     *
     * @param tag the tag
     * @param name the attribute name
     * @return the decoded value, or null
     */
    private static String htmlAttribute(final String tag, final String name) {
        Matcher m = Pattern.compile("(?:^|\\s)" + name
            + "\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))",
            Pattern.CASE_INSENSITIVE).matcher(tag);
        if (!m.find()) {
            return null;
        }
        for (int g = 1; g <= 3; g++) {
            if (m.group(g) != null) {
                return decodeEntities(m.group(g));
            }
        }
        return null;
    }

    /**
     * Decode the most common HTML entities.
     *
     * @param text the text
     * @return the decoded text
     */
    private static String decodeEntities(final String text) {
        return text.replace("&nbsp;", "\u00a0")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&amp;", "&");
    }

    // ------------------------------------------------------------------------
    // Tables -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Render a GFM table with box-drawing borders.
     *
     * @param table the table block
     */
    private void renderTable(final TableBlock table) {
        List<List<TableCell>> rows = new ArrayList<>();
        List<List<RichText>> contents = new ArrayList<>();
        int headerRows = 0;
        for (Node section = table.getFirstChild(); section != null;
             section = section.getNext()) {
            for (Node row = section.getFirstChild(); row != null;
                 row = row.getNext()) {
                if (!(row instanceof TableRow)) {
                    continue;
                }
                List<TableCell> cells = new ArrayList<>();
                List<RichText> texts = new ArrayList<>();
                for (Node cell = row.getFirstChild(); cell != null;
                     cell = cell.getNext()) {
                    if (cell instanceof TableCell tableCell) {
                        cells.add(tableCell);
                        texts.add(renderCell(tableCell));
                    }
                }
                if (!cells.isEmpty() && cells.get(0).isHeader()) {
                    headerRows++;
                }
                rows.add(cells);
                contents.add(texts);
            }
        }
        int columns = 0;
        for (List<TableCell> row : rows) {
            columns = Math.max(columns, row.size());
        }
        if (columns == 0) {
            return;
        }
        int[] widths = new int[columns];
        for (List<RichText> row : contents) {
            for (int c = 0; c < row.size(); c++) {
                widths[c] = Math.max(widths[c],
                    StringUtils.width(row.get(c).getPlainText()));
            }
        }

        CellAttributes border = new CellAttributes(styles.getLast());
        foreColor(border, Color.CYAN);
        borderLine("\u250c", "\u252c", "\u2510", widths, border);
        for (int r = 0; r < rows.size(); r++) {
            newline();
            text("\u2502", border);
            for (int c = 0; c < columns; c++) {
                RichText cell = c < contents.get(r).size()
                    ? contents.get(r).get(c) : RichText.builder().build();
                TableCell.Alignment alignment = c < rows.get(r).size()
                    ? rows.get(r).get(c).getAlignment() : null;
                int space = widths[c] - StringUtils.width(cell.getPlainText());
                int left = alignment == TableCell.Alignment.RIGHT ? space
                    : alignment == TableCell.Alignment.CENTER ? space / 2 : 0;
                text(" " + " ".repeat(left), styles.getLast());
                for (RichText.Run run : cell.getRuns()) {
                    text(run.getText(), run.getAttributes());
                }
                text(" ".repeat(space - left) + " ", styles.getLast());
                text("\u2502", border);
            }
            if (r == headerRows - 1 && r < rows.size() - 1) {
                newline();
                borderLine("\u251c", "\u253c", "\u2524", widths, border);
            }
        }
        newline();
        borderLine("\u2514", "\u2534", "\u2518", widths, border);
    }

    /**
     * Render one horizontal table border.
     */
    private void borderLine(final String left, final String middle,
        final String right, final int[] widths, final CellAttributes attr) {

        StringBuilder sb = new StringBuilder(left);
        for (int c = 0; c < widths.length; c++) {
            if (c > 0) {
                sb.append(middle);
            }
            sb.append("\u2500".repeat(widths[c] + 2));
        }
        sb.append(right);
        text(sb.toString(), attr);
    }

    /**
     * Render the inline content of a table cell on its own.
     *
     * @param cell the cell
     * @return the cell text on a single line
     */
    private RichText renderCell(final TableCell cell) {
        CellAttributes base = new CellAttributes(styles.getLast());
        if (cell.isHeader()) {
            base.setBold(true);
        }
        MarkdownRichTextRenderer renderer = new MarkdownRichTextRenderer(
            baseDirectory, base, true);
        for (Node child = cell.getFirstChild(); child != null;
             child = child.getNext()) {
            child.accept(renderer);
        }
        return renderer.out.build();
    }

    // ------------------------------------------------------------------------
    // Helpers ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Collect the plain text of a node's inline content.
     *
     * @param node the node
     * @return the text
     */
    private static String plainText(final Node node) {
        StringBuilder sb = new StringBuilder();
        node.accept(new AbstractVisitor() {
            @Override
            public void visit(final Text text) {
                sb.append(text.getLiteral());
            }

            @Override
            public void visit(final Code code) {
                sb.append(code.getLiteral());
            }

            @Override
            public void visit(final SoftLineBreak softLineBreak) {
                sb.append(' ');
            }

            @Override
            public void visit(final HardLineBreak hardLineBreak) {
                sb.append(' ');
            }
        });
        return sb.toString();
    }
}
