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
package casciian;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import casciian.bits.CellAttributes;
import casciian.bits.StringUtils;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
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
 */
public class TText extends TScrollable {

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
        super(parent, x, y, width, height);

        this.text = text;
        this.colorKey = colorKey;

        lines = new ArrayList<String>();

        vScroller = new TVScroller(this, getWidth() - 1, 0,
            Math.max(1, getHeight() - 1));
        hScroller = new THScroller(this, 0, getHeight() - 1,
            Math.max(1, getWidth() - 1));
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
        // Setup my color
        CellAttributes color = getTheme().getColor(colorKey);

        int begin = vScroller.getValue();
        int topY = 0;
        for (int i = begin; i < lines.size(); i++) {
            String line = lines.get(i);
            if (hScroller.getValue() < StringUtils.width(line)) {
                line = line.substring(hScroller.getValue());
            } else {
                line = "";
            }
            if (getWidth() > 3) {
                String formatString = "%-" + Integer.toString(getWidth() - 1) + "s";
                putStringXY(0, topY, String.format(formatString, line), color);
            }
            topY++;

            if (topY >= (getHeight() - 1)) {
                break;
            }
        }

        // Pad the rest with blank lines
        for (int i = topY; i < (getHeight() - 1); i++) {
            hLineXY(0, i, getWidth() - 1, ' ', color);
        }

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
        }
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
        computeBounds();
    }

    // ------------------------------------------------------------------------
    // TText ------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set the text.
     *
     * @param text new text to display
     */
    public void setText(final String text) {
        this.text = text;
        reflowData();
    }

    /**
     * Get the text.
     *
     * @return the text
     */
    public String getText() {
        return text;
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
     */
    public void setLineSpacing(final int lineSpacing) {
        this.lineSpacing = lineSpacing;
        reflowData();
    }

}
