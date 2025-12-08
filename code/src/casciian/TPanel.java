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

import casciian.bits.BorderStyle;
import casciian.bits.CellAttributes;
import casciian.bits.StringUtils;
import casciian.event.TResizeEvent;

/**
 * TPanel is an empty container for other widgets.
 */
public class TPanel extends TWidget {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Available locations to draw the title, if the title is specified.
     */
    public enum Direction {
        /**
         * Top-left.
         */
        TOP_LEFT,

        /**
         * Centered on the top.
         */
        TOP,

        /**
         * Top-right.
         */
        TOP_RIGHT,

        /**
         * Bottom-left.
         */
        BOTTOM_LEFT,

        /**
         * Centered on the bottom.
         */
        BOTTOM,

        /**
         * Bottom-right.
         */
        BOTTOM_RIGHT,
    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Optional panel title.
     */
    private String title = null;

    /**
     * Panel title location.
     */
    private Direction titleDirection = Direction.TOP_LEFT;

    /**
     * Border style.
     */
    private BorderStyle borderStyle = BorderStyle.NONE;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of widget
     * @param height height of widget
     */
    public TPanel(final TWidget parent, final int x, final int y,
        final int width, final int height) {

        super(parent, x, y, width, height);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Method that subclasses can override to handle window/screen resize
     * events.
     *
     * @param resize resize event
     */
    @Override
    public void onResize(final TResizeEvent resize) {
        if (resize.getType() == TResizeEvent.Type.WIDGET) {
            if (getChildren().size() == 1) {
                TWidget child = getChildren().get(0);
                if ((child instanceof TSplitPane)
                    || (child instanceof TPanel)
                ) {
                    child.onResize(new TResizeEvent(resize.getBackend(),
                            TResizeEvent.Type.WIDGET, resize.getWidth(),
                            resize.getHeight()));
                    return;
                }
            }
        }

        // Pass on to TWidget.
        super.onResize(resize);
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw an optional border and title.
     */
    @Override
    public void draw() {
        CellAttributes borderColor;
        borderColor = getTheme().getColor("tpanel.border");

        drawBox(0, 0, getWidth(), getHeight(), borderColor, borderColor,
            borderStyle, false);

        if ((title != null) && (title.length() > 0)) {
            int titleX = 1;
            int titleY = 0;
            int titleLength = StringUtils.width(title) + 2;

            switch (titleDirection) {
            case TOP_LEFT:
                titleX = 1;
                titleY = 0;
                break;
            case TOP:
                titleX = (getWidth() - titleLength) / 2;
                titleY = 0;
                break;
            case TOP_RIGHT:
                titleX = getWidth() - titleLength - 1;
                titleY = 0;
                break;
            case BOTTOM_LEFT:
                titleX = 1;
                titleY = getHeight() - 1;
                break;
            case BOTTOM:
                titleX = (getWidth() - titleLength) / 2;
                titleY = getHeight() - 1;
                break;
            case BOTTOM_RIGHT:
                titleX = getWidth() - titleLength - 1;
                titleY = getHeight() - 1;
                break;
            }

            hLineXY(titleX, titleY, titleLength, ' ', borderColor);
            if (borderStyle.equals(BorderStyle.NONE)) {
                putStringXY(titleX, titleY, title, borderColor);
            } else {
                putStringXY(titleX + 1, titleY, title, borderColor);
            }
        }
    }

    // ------------------------------------------------------------------------
    // TPanel -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get panel title.
     *
     * @return the panel title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set panel title.
     *
     * @param title the new panel title
     */
    public void setTitle(final String title) {
        this.title = title.trim();
    }

    /**
     * Get panel title direction.
     *
     * @return one of Direction.TOP_LEFT, Direction.TOP, etc.
     */
    public Direction getTitleDirection() {
        return titleDirection;
    }

    /**
     * Set panel title direction.
     *
     * @param direction one of Direction.TOP_LEFT, Direction.TOP, etc.
     */
    public void setTitleDirection(final Direction direction) {
        this.titleDirection = direction;
    }

    /**
     * Set the border style for the panel.
     *
     * @param borderStyle the border style string, one of: "default", "none",
     * "single", "double", "singleVdoubleH", "singleHdoubleV", or "round"; or
     * null to use the value from casciian.TPanel.borderStyle.
     */
    public void setBorderStyle(final String borderStyle) {
        if (borderStyle == null) {
            String style = System.getProperty("casciian.TPanel.borderStyle",
                "none");
            this.borderStyle = BorderStyle.getStyle(style);
        } else if (borderStyle.equals("default")) {
            this.borderStyle = BorderStyle.NONE;
        } else {
            this.borderStyle = BorderStyle.getStyle(borderStyle);
        }
    }

    /**
     * Get the border style for the panel.
     *
     * @return the border style
     */
    public BorderStyle getBorderStyle() {
        return borderStyle;
    }

}
