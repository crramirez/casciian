/*
 * Casciian - Java Text User Interface
 *
 * Original work written 2013–2025 by Autumn Lamonte
 * and dedicated to the public domain via CC0.
 *
 * Modifications and maintenance:
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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import casciian.bits.CellAttributes;
import casciian.bits.StringUtils;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import casciian.texteditor.Document;
import casciian.texteditor.Word;
import static casciian.TKeypress.kbDown;
import static casciian.TKeypress.kbEnd;
import static casciian.TKeypress.kbHome;
import static casciian.TKeypress.kbLeft;
import static casciian.TKeypress.kbPgDn;
import static casciian.TKeypress.kbPgUp;
import static casciian.TKeypress.kbRight;
import static casciian.TKeypress.kbUp;

/**
 * TText implements a simple scrollable text area. It reflows automatically on
 * resize.
 *
 * <p>
 * The text is not editable, but it can be selected with the mouse (or with
 * shift + the navigation keys) and copied to the clipboard.  The text model,
 * selection and rendering are provided by {@link TTextBase}.
 * </p>
 */
public class TText extends TTextBase {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Available text justifications.
     */
    public enum Justification {

        /**
         * Not justified at all, use spacing as provided by the client.
         */
        NONE,

        /**
         * Left-justified text.
         */
        LEFT,

        /**
         * Centered text.
         */
        CENTER,

        /**
         * Right-justified text.
         */
        RIGHT,

        /**
         * Fully-justified text.
         */
        FULL,
    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * How to justify the text.
     */
    private Justification justification = Justification.LEFT;

    /**
     * Text to display.
     */
    private String text;

    /**
     * Text converted to lines.
     */
    private List<String> lines;

    /**
     * Text color.
     */
    private String colorKey;

    /**
     * Maximum width of a single line.
     */
    private int maxLineWidth;

    /**
     * Number of lines between each paragraph.
     */
    private int lineSpacing = 1;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param text text on the screen
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     */
    public TText(final TWidget parent, final String text, final int x,
            final int y, final int width, final int height) {

        this(parent, text, x, y, width, height, "ttext");
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param text text on the screen
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     * @param colorKey ColorTheme key color to use for foreground
     * text. Default is "ttext".
     */
    @SuppressWarnings("this-escape")
    public TText(final TWidget parent, final String text, final int x,
            final int y, final int width, final int height,
            final String colorKey) {

        // Set parent and window
        super(parent, text, x, y, width, height, colorKey);

        this.text = (text == null ? "" : text);
        this.colorKey = colorKey;

        setMouseStyle("text");

        lines = new ArrayList<String>();

        vScroller = new TVScroller(this, getWidth() - 1, 0,
            Math.max(1, getHeight() - 1));
        hScroller = new THScroller(this, 0, getHeight() - 1,
            Math.max(1, getWidth() - 1));
        reflowData();
    }

    // ------------------------------------------------------------------------
    // TTextBase --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * TText does not allow the text to be modified.
     *
     * @return false
     */
    @Override
    public boolean isEditable() {
        return false;
    }

    /**
     * The text area excludes the column used by the vertical scrollbar.
     *
     * @return the width of the text area
     */
    @Override
    protected int getTextAreaWidth() {
        return Math.max(0, getWidth() - 1);
    }

    /**
     * The text area excludes the row used by the horizontal scrollbar.
     *
     * @return the height of the text area
     */
    @Override
    protected int getTextAreaHeight() {
        return Math.max(0, getHeight() - 1);
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
            vScroller.setHeight(getHeight() - 1);
        }
    }

    /**
     * Draw the text box.
     */
    @Override
    public void draw() {
        CellAttributes color = getWidgetColor(colorKey);
        if (color != null) {
            // Pick up runtime theme changes.
            setDefaultColor(color);
        }
        syncFromScrollers();
        drawDocument();
    }

    /**
     * The text of a read-only text box is always drawn with the current
     * theme color.
     *
     * @param word the word being drawn
     * @return the color to draw the word with
     */
    @Override
    protected CellAttributes getTextColor(final Word word) {
        return getDefaultColor();
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

        syncFromScrollers();
        super.onMouseDown(mouse);
        syncToScrollers();
    }

    /**
     * Handle mouse motion events.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        syncFromScrollers();
        super.onMouseMotion(mouse);
        syncToScrollers();
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.getKey().isShift()) {
            // Shifted navigation keys extend the selection.
            syncFromScrollers();
            super.onKeypress(keypress);
            syncToScrollers();
            return;
        }

        if (keypress.equals(kbLeft)) {
            hScroller.decrement();
        } else if (keypress.equals(kbRight)) {
            hScroller.increment();
        } else if (keypress.equals(kbUp)) {
            vScroller.decrement();
        } else if (keypress.equals(kbDown)) {
            vScroller.increment();
        } else if (keypress.equals(kbPgUp)) {
            vScroller.bigDecrement();
        } else if (keypress.equals(kbPgDn)) {
            vScroller.bigIncrement();
        } else if (keypress.equals(kbHome)) {
            vScroller.toTop();
        } else if (keypress.equals(kbEnd)) {
            vScroller.toBottom();
        } else {
            // Pass other keys (tab etc.) on
            super.onKeypress(keypress);
            return;
        }
        unsetSelection();
        syncFromScrollers();
    }

    /**
     * Resize text and scrollbars for a new width/height.
     */
    @Override
    public void reflowData() {
        // Reset the lines
        lines.clear();

        // Break up text into paragraphs
        String [] paragraphs = text.split("\n\n");
        for (String p : paragraphs) {
            switch (justification) {
            case NONE:
                lines.addAll(Arrays.asList(p.split("\n")));
                break;
            case LEFT:
                lines.addAll(StringUtils.left(p, getWidth() - 1));
                break;
            case CENTER:
                lines.addAll(StringUtils.center(p, getWidth() - 1));
                break;
            case RIGHT:
                lines.addAll(StringUtils.right(p, getWidth() - 1));
                break;
            case FULL:
                lines.addAll(StringUtils.full(p, getWidth() - 1));
                break;
            }

            for (int i = 0; i < lineSpacing; i++) {
                lines.add("");
            }
        }
        unsetSelection();
        document = new Document(String.join("\n", lines), getDefaultColor());
        computeBounds();
    }

    // ------------------------------------------------------------------------
    // TText ------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Copy the visible area position from the scrollbars.
     */
    private void syncFromScrollers() {
        if (vScroller != null) {
            setTopLine(vScroller.getValue());
        }
        if (hScroller != null) {
            setLeftColumn(hScroller.getValue());
        }
    }

    /**
     * Copy the visible area position to the scrollbars.
     */
    private void syncToScrollers() {
        if (vScroller != null) {
            vScroller.setValue(Math.max(vScroller.getTopValue(),
                    Math.min(getTopLine(), vScroller.getBottomValue())));
        }
        if (hScroller != null) {
            hScroller.setValue(Math.max(hScroller.getLeftValue(),
                    Math.min(getLeftColumn(), hScroller.getRightValue())));
        }
    }

    /**
     * Set the text.
     *
     * @param text new text to display
     */
    @Override
    public void setText(final String text) {
        this.text = (text == null ? "" : text);
        reflowData();
    }

    /**
     * Get the text.
     *
     * @return the text
     */
    @Override
    public String getText() {
        return text;
    }

    /**
     * Get the ColorTheme key color used for the text.
     *
     * @return the color key
     */
    public String getColorKey() {
        return colorKey;
    }

    /**
     * Add one line.
     *
     * @param line new line to add
     */
    public void addLine(final String line) {
        if (StringUtils.width(text) == 0) {
            text = line;
        } else {
            text += "\n\n";
            text += line;
        }
        reflowData();
    }

    /**
     * Recompute the bounds for the scrollbars.
     */
    private void computeBounds() {
        maxLineWidth = 0;
        for (String line : lines) {
            if (StringUtils.width(line) > maxLineWidth) {
                maxLineWidth = StringUtils.width(line);
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

    /**
     * Set justification.
     *
     * @param justification NONE, LEFT, CENTER, RIGHT, or FULL
     */
    public void setJustification(final Justification justification) {
        this.justification = justification;
        reflowData();
    }

    /**
     * Left-justify the text.
     */
    public void leftJustify() {
        justification = Justification.LEFT;
        reflowData();
    }

    /**
     * Center-justify the text.
     */
    public void centerJustify() {
        justification = Justification.CENTER;
        reflowData();
    }

    /**
     * Right-justify the text.
     */
    public void rightJustify() {
        justification = Justification.RIGHT;
        reflowData();
    }

    /**
     * Fully-justify the text.
     */
    public void fullJustify() {
        justification = Justification.FULL;
        reflowData();
    }

    /**
     * Un-justify the text.
     */
    public void unJustify() {
        justification = Justification.NONE;
        reflowData();
    }

    /**
     * Set the number of lines between each paragraph.
     *
     * @param lineSpacing the number of blank lines between paragraphs
     * @return this to allow chaining initialization
     */
    @SuppressWarnings("UnusedReturnValue")
    public TText setLineSpacing(final int lineSpacing) {
        this.lineSpacing = lineSpacing;
        reflowData();

        return this;
    }
}
