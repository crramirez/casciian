/*
 * Casciian - Java Text User Interface
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
package casciian;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import casciian.backend.Screen;
import casciian.backend.SystemProperties;
import casciian.bits.AnsiParser;
import casciian.bits.CasciianMarkupParser;
import casciian.bits.Cell;
import casciian.bits.CellAttributes;
import casciian.bits.ColorTheme;
import casciian.bits.ImageRGB;
import casciian.bits.RichText;
import casciian.bits.RichTextLayout;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import casciian.image.decoders.ImageDecoderRegistry;
import static casciian.TKeypress.*;

/**
 * TTextAnsi displays text containing ANSI escape sequences (colors and
 * attributes) as a scrollable widget. It behaves like the {@code cat}
 * command: text is rendered character by character, honoring escape
 * sequences for SGR (Select Graphic Rendition) styling, and wrapping at
 * the widget width.
 *
 * <p>
 * This component is designed to display the output of commands like
 * {@code pandoc file.md -t ansi} within a casciian application.
 * </p>
 */
public class TTextAnsi extends TScrollable {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The raw text (may contain ANSI escape sequences).  Kept so
     * {@link #getText()} can return the source verbatim.
     */
    private String text;

    /**
     * The canonical styled-text model.  Parsed once from the source text (or
     * supplied directly) and laid out for the current width on each reflow, so
     * changing the widget width no longer requires re-parsing ANSI.
     */
    private RichText richText;

    /**
     * The ANSI parser state that should apply to the next appended fragment.
     */
    private CellAttributes appendState;

    /**
     * Parsed lines of cells.
     */
    private List<RichTextLayout.Line> lines;

    /**
     * Maximum width of any single line in cells.
     */
    private int maxLineWidth;

    private final Map<String, Optional<ImageRGB>> images = new HashMap<>();
    private Map<RenderedImageKey, List<List<Cell>>> renderedImages =
        new HashMap<>();
    private int imageTextWidth;
    private int imageTextHeight;
    private TImage.DisplayMode imageMode;
    private int layoutWidth;
    private CellAttributes imageBackground;
    private int imageBackgroundColor;
    private int[] sourceLineStarts;

    /**
     * Everything that determines the rendered cell rows of an image block, so
     * unchanged images keep their tiles (and image IDs) across reflows.
     */
    private record RenderedImageKey(String path, int contentWidth,
        int textWidth, int textHeight, CellAttributes background,
        int backgroundColor, TImage.DisplayMode mode) {
    }

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param text text to display (may contain ANSI escape sequences)
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     */
    @SuppressWarnings("this-escape")
    public TTextAnsi(final TWidget parent, final String text, final int x,
            final int y, final int width, final int height) {

        // Set parent and window
        super(parent, x, y, width, height);

        this.text = text;
        setAnsiSource(text);

        vScroller = new TVScroller(this, getWidth() - 1, 0,
            Math.max(1, getHeight() - 1));
        hScroller = new THScroller(this, 0, getHeight() - 1,
            Math.max(1, calculateHScrollerWidth()));
        reflowData();
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
            hScroller.setWidth(calculateHScrollerWidth());
        }
        if (vScroller != null) {
            vScroller.setX(getWidth() - 1);
        }
    }

    /**
     * Override TWidget's height: we need to set child widget heights.
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
            vScroller.setHeight(getHeight() - 1);
        }
    }

    /**
     * Draw the text area.
     */
    @Override
    public void draw() {
        reflowIfStale();
        CellAttributes defaultColor = getWidgetColor(ColorTheme.TTEXT);
        CellAttributes defaultColorBright = getWidgetColor(ColorTheme.TTEXT_BOLD);

        int begin = vScroller.getValue();
        int hOffset = hScroller.getValue();
        int topY = 0;

        for (int i = begin; i < lines.size(); i++) {
            RichTextLayout.Line line = lines.get(i);
            List<Cell> cells = line.getCells();

            // Draw cells for this line
            int drawWidth = getWidth() - 1;
            int col = 0;
            while (col < drawWidth) {
                int srcCol = col + hOffset;
                if (srcCol >= cells.size()) {
                    putCharXY(col, topY, ' ', defaultColor);
                    col++;
                    continue;
                }

                Cell cell = cells.get(srcCol);
                boolean wide = (cell.getDisplayWidth() == 2) && !cell.isImage();

                // Never partially place a two-cell cluster: if the right
                // half would fall outside the visible area, draw a blank
                // instead so the wide glyph cannot overflow the widget.
                if (wide && (col + 1 >= drawWidth)) {
                    putCharXY(col, topY, ' ', defaultColor);
                    col++;
                    continue;
                }

                drawCell(cell, col, topY, defaultColor, defaultColorBright);

                if (wide) {
                    // AnsiParser stores a padding cell for the right half of
                    // a wide glyph.  putCharXY already rendered both halves,
                    // so skip the padding cell to avoid overwriting the
                    // right half.
                    col += 2;
                } else {
                    col++;
                }
            }
            topY++;

            if (topY >= (getHeight() - 1)) {
                break;
            }
        }

        // Pad remaining rows with blank lines
        for (int i = topY; i < (getHeight() - 1); i++) {
            hLineXY(0, i, getWidth() - 1, ' ', defaultColor);
        }
    }

    /**
     * Draw a single parsed cell at the given position, replacing default
     * terminal colors with the widget's theme colors.
     *
     * @param cell              the parsed cell to draw
     * @param col               column relative to the widget
     * @param topY              row relative to the widget
     * @param defaultColor      the theme's default color
     * @param defaultColorBright the theme's default bright color
     */
    private void drawCell(final Cell cell, final int col, final int topY,
            final CellAttributes defaultColor,
            final CellAttributes defaultColorBright) {

        if (cell.isImage()) {
            putCharXY(col, topY, cell);
            return;
        }
        TWindow w = getWindow();
        boolean modal = (w != null) && w.isModal();
        boolean darkTheme = getTheme().isDarkTheme(modal);

        // Replace default terminal colors with theme colors
        if (cell.isDefaultColor(true)
                && cell.isDefaultColor(false)) {
            CellAttributes base = cell.isBoldAsBright()
                && defaultColor.getForeColorRGB() >= 0
                ? defaultColorBright : defaultColor;
            CellAttributes themed = new CellAttributes();
            themed.setTo(base);
            themed.setBold(cell.isBold());
            themed.setUnderlineStyle(cell.getUnderlineStyle());
            themed.setBlink(cell.isBlink());
            themed.setReverse(cell.isReverse());
            themed.setFaint(cell.isFaint());
            themed.setItalic(cell.isItalic());
            themed.setHidden(cell.isHidden());
            themed.setStrikethrough(cell.isStrikethrough());
            themed.setHyperlink(cell.getHyperlink());
            applyBoldAsBright(themed, darkTheme, defaultColorBright);
            putCharXY(col, topY, cell.getChar(), themed);
        } else if (cell.isDefaultColor(true)) {
            CellAttributes base = cell.isBoldAsBright()
                && defaultColor.getForeColorRGB() >= 0
                ? defaultColorBright : defaultColor;
            CellAttributes themed = new CellAttributes();
            themed.setTo(cell);
            if (base.getForeColorRGB() >= 0) {
                themed.setForeColorRGB(
                    base.getForeColorRGB());
            } else {
                themed.setForeColor(base.getForeColor());
            }
            themed.setDefaultColor(true, false);
            applyBoldAsBright(themed, darkTheme, defaultColorBright);
            putCharXY(col, topY, cell.getChar(), themed);
        } else if (cell.isDefaultColor(false)) {
            CellAttributes themed = new CellAttributes();
            themed.setTo(cell);
            if (defaultColor.getBackColorRGB() >= 0) {
                themed.setBackColorRGB(
                    defaultColor.getBackColorRGB());
            } else {
                themed.setBackColor(defaultColor.getBackColor());
            }
            themed.setDefaultColor(false, false);
            applyBoldAsBright(themed, darkTheme, defaultColorBright);
            putCharXY(col, topY, cell.getChar(), themed);
        } else {
            CellAttributes themed = new CellAttributes();
            themed.setTo(cell);
            applyBoldAsBright(themed, darkTheme, defaultColorBright);
            putCharXY(col, topY, cell.getChar(), themed);
        }
    }

    /**
     * Treat the bold attribute as a bright color on dark themes, matching
     * the classic terminal convention where bold text is rendered using the
     * high-intensity color palette rather than an actual bold font weight.
     *
     * <p>On a dark theme with a bold cell:
     * <ul>
     *   <li>Named (non-RGB, non-palette) foreground: brightened via
     *       {@link casciian.bits.Color#toBright()}, bold cleared.</li>
     *   <li>RGB or palette foreground: replaced with the foreground of
     *       {@code defaultColorBright} (the theme's bold-text color),
     *       bold cleared.</li>
     * </ul>
     * Non-dark themes are left unchanged.
     * </p>
     *
     * @param attrs             the attributes to potentially modify in place
     * @param darkTheme         true if the current theme is a dark theme, as
     *                          determined by {@link ColorTheme#isDarkTheme()}
     * @param defaultColorBright the theme's bold-text color, used as the
     *                          bright substitution for RGB/palette foregrounds
     */
    static void applyBoldAsBright(final CellAttributes attrs,
            final boolean darkTheme,
            final CellAttributes defaultColorBright) {

        if (!darkTheme || !attrs.isBold()) {
            return;
        }
        if ((attrs.getForeColorRGB() >= 0)
                || (attrs.getForeColorPalette() >= 0)) {
            // For RGB/palette foregrounds there is no natural "bright"
            // variant, so use the theme's bold-text foreground instead.
            if (defaultColorBright.getForeColorPalette() >= 0) {
                attrs.setForeColorPalette(
                    defaultColorBright.getForeColorPalette());
            } else if (defaultColorBright.getForeColorRGB() >= 0) {
                attrs.setForeColorRGB(defaultColorBright.getForeColorRGB());
            } else {
                attrs.setForeColor(defaultColorBright.getForeColor());
            }
            attrs.setBold(false);
            return;
        }
        attrs.setForeColor(attrs.getForeColor().toBright());
        attrs.setBold(false);
    }

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if (mouse.isMouseWheelUp()) {
            vScroller.decrement();
            return;
        }
        if (mouse.isMouseWheelDown()) {
            vScroller.increment();
            return;
        }
        if (mouse.isMouseWheelLeft()) {
            hScroller.increment();
            return;
        }
        if (mouse.isMouseWheelRight()) {
            hScroller.decrement();
            return;
        }

        // Pass to children
        super.onMouseDown(mouse);
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.matchesKey(kbLeft)) {
            hScroller.decrement();
        } else if (keypress.matchesKey(kbRight)) {
            hScroller.increment();
        } else if (keypress.matchesKey(kbUp)) {
            vScroller.decrement();
        } else if (keypress.matchesKey(kbDown)) {
            vScroller.increment();
        } else if (keypress.matchesKey(kbPgUp)) {
            vScroller.bigDecrement();
        } else if (keypress.matchesKey(kbPgDn)) {
            vScroller.bigIncrement();
        } else if (keypress.matchesKey(kbHome)) {
            vScroller.toTop();
        } else if (keypress.matchesKey(kbEnd)) {
            vScroller.toBottom();
        } else {
            // Pass other keys (tab etc.) on
            super.onKeypress(keypress);
        }
    }

    /**
     * Resize text and scrollbars for a new width/height.
     */
    @Override
    public void reflowData() {
        // Lay out the styled-text model at the current width.  The RichText
        // model is width-independent, so no ANSI re-parsing is required here.
        int displayWidth = getWidth() - 1;
        if (displayWidth < 1) {
            displayWidth = 1;
        }
        imageTextWidth = getScreen() == null ? 0 : getScreen().getTextWidth();
        imageTextHeight = getScreen() == null ? 0 : getScreen().getTextHeight();
        imageMode = imageDisplayMode();
        imageBackground = new CellAttributes(getWidgetColor(ColorTheme.TTEXT));
        imageBackgroundColor = resolveImageBackgroundColor(imageBackground);
        layoutWidth = getWidth();
        final int contentWidth = displayWidth;
        Map<RenderedImageKey, List<List<Cell>>> previous = renderedImages;
        renderedImages = new HashMap<>();
        lines = RichTextLayout.layout(richText, displayWidth,
            run -> renderImage(run, contentWidth, previous));
        sourceLineStarts = null;
        computeBounds();
    }

    /**
     * Reflow if the width, screen cell pixel metrics, or effective image
     * display mode changed since the last layout.
     */
    private void reflowIfStale() {
        Screen screen = getScreen();
        int textWidth = screen == null ? 0 : screen.getTextWidth();
        int textHeight = screen == null ? 0 : screen.getTextHeight();
        CellAttributes background = new CellAttributes(
            getWidgetColor(ColorTheme.TTEXT));
        int backgroundColor = resolveImageBackgroundColor(background);
        if (layoutWidth != getWidth()
                || imageTextWidth != textWidth
                || imageTextHeight != textHeight
                || imageMode != imageDisplayMode()
                || !imageBackground.equals(background)
                || imageBackgroundColor != backgroundColor) {
            reflowData();
        }
    }

    private int resolveImageBackgroundColor(final CellAttributes background) {
        TApplication application = getApplication();
        if (application == null || application.getBackend() == null) {
            return 0;
        }
        return application.getBackend().attrToBackgroundColor(background);
    }

    /**
     * Get the effective image display mode for the current backend.
     *
     * @return the display mode, or null if there is no backend
     */
    private TImage.DisplayMode imageDisplayMode() {
        TApplication application = getApplication();
        if (application == null || application.getBackend() == null) {
            return null;
        }
        if (application.getBackend().isImageProtocolSupported()) {
            return TImage.DisplayMode.BITMAP;
        }
        return "solid".equals(SystemProperties.getImageFallbackDisplayMode())
            ? TImage.DisplayMode.BLOCKS : TImage.DisplayMode.UNICODE_HALVES;
    }

    private List<List<Cell>> renderImage(final RichText.Run run,
            final int contentWidth,
            final Map<RenderedImageKey, List<List<Cell>>> previous) {
        if (imageMode == null || imageTextWidth <= 0 || imageTextHeight <= 0
                || imageTextWidth == Integer.MAX_VALUE
                || imageTextHeight == Integer.MAX_VALUE
                || imageTextWidth > Integer.MAX_VALUE / contentWidth) {
            return null;
        }
        Optional<ImageRGB> decoded = images.computeIfAbsent(run.getImagePath(),
            path -> {
                try {
                    Path file = Path.of(path);
                    if (file.getFileName() == null) {
                        return Optional.empty();
                    }
                    return Optional.ofNullable(ImageDecoderRegistry.getInstance()
                        .decodeImage(file)).filter(image ->
                            image.getWidth() > 0 && image.getHeight() > 0);
                } catch (IOException | RuntimeException e) {
                    return Optional.empty();
                }
            });
        if (decoded.isEmpty()) {
            return null;
        }
        RenderedImageKey key = new RenderedImageKey(run.getImagePath(),
            contentWidth, imageTextWidth, imageTextHeight,
            new CellAttributes(imageBackground), imageBackgroundColor,
            imageMode);
        List<List<Cell>> rows = renderedImages.get(key);
        if (rows == null) {
            rows = previous.get(key);
        }
        if (rows == null) {
            ImageRGB image = ImageCellRenderer.fit(decoded.get(),
                contentWidth * imageTextWidth, Integer.MAX_VALUE, false);
            image = ImageCellRenderer.pad(image, imageTextWidth,
                imageTextHeight, imageBackgroundColor);
            Cell[][] cells = ImageCellRenderer.render(image, imageTextWidth,
                imageTextHeight, imageBackground, imageMode, true);
            rows = new ArrayList<>();
            for (int y = 0; y < cells[0].length; y++) {
                List<Cell> row = new ArrayList<>();
                for (Cell[] column : cells) {
                    row.add(column[y]);
                }
                rows.add(row);
            }
        }
        renderedImages.put(key, rows);
        return rows;
    }

    // ------------------------------------------------------------------------
    // TTextAnsi --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set the text.
     *
     * @param text new text to display (may contain ANSI escape sequences)
     */
    public void setText(final String text) {
        setAnsiSource(text);
        reflowData();
    }

    /**
     * Get the text.
     *
     * @return the raw text
     */
    public String getText() {
        return text;
    }

    /**
     * Set the styled text directly from a {@link RichText} model.  This makes
     * the given model canonical; {@link #getText()} will then return its plain
     * text (including image alternative text). Image blocks are loaded through
     * {@link ImageDecoderRegistry}, proportionally shrunk only when wider than
     * the content area, and occupy their own rows. Unsupported or unreadable
     * files display alternative text. Terminal image fallback matches TImage.
     *
     * @param richText the styled text to display
     */
    public void setRichText(final RichText richText) {
        this.richText = (richText == null)
            ? RichText.builder().build() : richText;
        images.clear();
        renderedImages.clear();
        this.text = this.richText.getPlainText();
        resetAppendState();
        reflowData();
    }

    /**
     * Get the canonical styled-text model currently displayed.
     *
     * @return the RichText model
     */
    public RichText getRichText() {
        return richText;
    }

    /**
     * Get the display line on which each logical source line starts.  Source
     * lines are separated by line feeds in the rich text (for markup, these
     * are the markup's own lines).  Image blocks count their rendered rows,
     * so this reflects the current image-aware layout.
     *
     * @return the first display line for each source line, in order
     */
    public int[] getSourceLineStarts() {
        reflowIfStale();
        if (sourceLineStarts == null) {
            int count = (int) richText.getPlainText().chars()
                .filter(c -> c == '\n').count() + 1;
            int[] starts = new int[count];
            int display = 0;
            for (int source = 0; source < count; source++) {
                while (display < lines.size() - 1
                        && lines.get(display).getSourceLine() < source) {
                    display++;
                }
                starts[source] = display;
            }
            sourceLineStarts = starts;
        }
        return sourceLineStarts.clone();
    }

    /**
     * Set the styled text from Casciian markup.
     *
     * @param markup the markup text
     * @see CasciianMarkupParser
     */
    public void setMarkup(final String markup) {
        setRichText(CasciianMarkupParser.parse(markup));
    }

    /**
     * Append text.
     *
     * @param newText text to append
     */
    public void appendText(final String newText) {
        if (text == null || text.isEmpty()) {
            text = newText;
        } else {
            text += newText;
        }
        RichText.Builder builder = RichText.builder();
        builder.appendRichText(richText);
        CellAttributes activeAttr = null;
        for (RichText.Run run : richText.getRuns()) {
            activeAttr = run.getAttributes();
        }
        AnsiParser.RichTextParseResult appended = AnsiParser.parseRichText(
            newText, appendState == null ? activeAttr : appendState);
        for (RichText.Run run : appended.getRichText().getRuns()) {
            builder.append(run.getText(), run.getAttributes());
        }
        richText = builder.build();
        appendState = appended.getFinalAttributes();
        reflowData();
    }

    /**
     * Parse ANSI source text into the canonical rich-text model and store the
     * trailing parser state for later append operations.
     *
     * @param source the raw ANSI source
     */
    private void setAnsiSource(final String source) {
        text = source;
        AnsiParser.RichTextParseResult parsed = AnsiParser.parseRichText(
            source, null);
        richText = parsed.getRichText();
        images.clear();
        renderedImages.clear();
        appendState = parsed.getFinalAttributes();
    }

    /**
     * Reset the append parser state to default terminal attributes.
     */
    private void resetAppendState() {
        appendState = new CellAttributes();
        appendState.setDefaultColor(true, true);
        appendState.setDefaultColor(false, true);
    }

    /**
     * Recompute the bounds for the scrollbars.
     */
    private void computeBounds() {
        maxLineWidth = 0;
        for (RichTextLayout.Line line : lines) {
            if (line.getWidth() > maxLineWidth) {
                maxLineWidth = line.getWidth();
            }
        }

        vScroller.setTopValue(0);
        vScroller.setBottomValue((lines.size() - getHeight()) + 1);
        if (vScroller.getBottomValue() < 0) {
            vScroller.setBottomValue(0);
        }
        if (vScroller.getValue() > vScroller.getBottomValue()) {
            vScroller.setValue(vScroller.getBottomValue());
        }

        hScroller.setLeftValue(0);
        hScroller.setRightValue((maxLineWidth - getWidth()) + 1);
        if (hScroller.getRightValue() < 0) {
            hScroller.setRightValue(0);
        }
        if (hScroller.getValue() > hScroller.getRightValue()) {
            hScroller.setValue(hScroller.getRightValue());
        }
    }
}
