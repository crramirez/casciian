/*
 * Casciian - Java Text User Interface
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

import java.util.List;

import casciian.bits.AnsiParser;
import casciian.bits.Cell;
import casciian.bits.CellAttributes;
import casciian.bits.StringUtils;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
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
     * The raw text (may contain ANSI escape sequences).
     */
    private String text;

    /**
     * Parsed lines of cells.
     */
    private List<AnsiParser.Line> lines;

    /**
     * Maximum width of any single line in cells.
     */
    private int maxLineWidth;

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
        CellAttributes defaultColor = getWidgetColor("ttext");

        int begin = vScroller.getValue();
        int hOffset = hScroller.getValue();
        int topY = 0;

        for (int i = begin; i < lines.size(); i++) {
            AnsiParser.Line line = lines.get(i);
            List<Cell> cells = line.getCells();

            // Draw cells for this line
            int drawWidth = getWidth() - 1;
            for (int col = 0; col < drawWidth; col++) {
                int srcCol = col + hOffset;
                if (srcCol < cells.size()) {
                    putCharXY(col, topY, cells.get(srcCol));
                } else {
                    putCharXY(col, topY, ' ', defaultColor);
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
        // Re-parse with the current width
        int displayWidth = getWidth() - 1;
        if (displayWidth < 1) {
            displayWidth = 1;
        }
        lines = AnsiParser.parse(text, displayWidth);
        computeBounds();
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
        this.text = text;
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
        reflowData();
    }

    /**
     * Recompute the bounds for the scrollbars.
     */
    private void computeBounds() {
        maxLineWidth = 0;
        for (AnsiParser.Line line : lines) {
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
