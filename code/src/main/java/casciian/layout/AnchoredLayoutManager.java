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
package casciian.layout;

import java.util.HashMap;

import casciian.TButton;
import casciian.TWidget;
import casciian.event.TResizeEvent;

/**
 * AnchoredLayoutManager repositions child widgets based on their coordinates
 * when added, an anchor point, and the current widget size.
 */
public class AnchoredLayoutManager implements LayoutManager {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Available relative corners of another widget to anchor to.
     */
    public enum Anchor {
        /**
         * Anchor to the top-left corner of the container.
         */
        TOP_LEFT,

        /**
         * Anchor to the top-right corner of the container.
         */
        TOP_RIGHT,

        /**
         * Anchor to the bottom-left corner of the container.
         */
        BOTTOM_LEFT,

        /**
         * Anchor to the bottom-right corner of the container.
         */
        BOTTOM_RIGHT,

        /**
         * Anchor relative to a widget above this one.
         */
        TOP,

        /**
         * Anchor relative to a widget to the left of this one.
         */
        LEFT,

        /**
         * Anchor relative to a widget below this one.
         */
        BOTTOM,

        /**
         * Anchor relative to a widget to the right of this one.
         */
        RIGHT,
    }

    /**
     * Original anchor information for a managed widget.
     */
    private class AnchorData {
        private TWidget widget;
        private Anchor anchor;
        private int padX;
        private int padY;

        /**
         * Constructor.
         *
         * @param widget widget to anchor relative to, or null to anchor to
         * the containing widget
         * @param anchor one of Anchor.TOP_LEFT, Anchor.TOP_RIGHT, etc.
         * @param padX number of columns to pad
         * @param padY number of rows to pad
         */
        public AnchorData(final TWidget widget, final Anchor anchor,
            final int padX, final int padY) {

            this.widget = widget;
            this.anchor = anchor;
            this.padX = padX;
            this.padY = padY;
        }

    }

    /**
     * A simple rectangle class.
     */
    private class Rectangle {
        double x;
        double y;
        double width;
        double height;

        Rectangle(final int x, final int y, final int width, final int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Current width.
     */
    private int width = 0;

    /**
     * Current height.
     */
    private int height = 0;

    /**
     * Original width.
     */
    private int originalWidth = 0;

    /**
     * Original height.
     */
    private int originalHeight = 0;

    /**
     * Map of widget to original dimensions.
     */
    private HashMap<TWidget, Rectangle> children = new HashMap<TWidget, Rectangle>();

    /**
     * Map of widget to anchor.
     */
    private HashMap<TWidget, AnchorData> anchors = new HashMap<TWidget, AnchorData>();

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param width the width of the parent widget
     * @param height the height of the parent widget
     */
    public AnchoredLayoutManager(final int width, final int height) {
        originalWidth = width;
        originalHeight = height;
        this.width = width;
        this.height = height;
    }

    // ------------------------------------------------------------------------
    // LayoutManager ----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Process the parent widget's resize event, and resize/reposition child
     * widgets.
     *
     * @param resize resize event
     */
    public void onResize(final TResizeEvent resize) {
        if (resize.getType() == TResizeEvent.Type.WIDGET) {
            width = resize.getWidth();
            height = resize.getHeight();
            layoutChildren();

            for (TWidget child: children.keySet()) {
                child.onResize(new TResizeEvent(resize.getBackend(),
                        TResizeEvent.Type.WIDGET,
                        child.getWidth(), child.getHeight()));
            }
        }
    }

    /**
     * Add a child widget to manage.
     *
     * @param child the widget to manage
     */
    public void add(final TWidget child) {
        Rectangle rect = new Rectangle(child.getX(), child.getY(),
            child.getWidth(), child.getHeight());
        children.put(child, rect);

        // Default behavior is to anchor to the top-left corner.
        AnchorData anchorData = new AnchorData(null, Anchor.TOP_LEFT,
            child.getX(), child.getY());
        anchors.put(child, anchorData);

        layoutChildren();
    }

    /**
     * Remove a child widget from those managed by this LayoutManager.
     *
     * @param child the widget to remove
     */
    public void remove(final TWidget child) {
        children.remove(child);
        anchors.remove(child);
        layoutChildren();
    }

    /**
     * Reset a child widget's original/preferred size.
     *
     * @param child the widget to manage
     */
    public void resetSize(final TWidget child) {
        // For this layout, adding is the same as replacing.
        add(child);
    }

    // ------------------------------------------------------------------------
    // AnchoredLayoutManager --------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Resize/reposition child widgets based on difference between current
     * dimensions and the original dimensions.
     */
    private void layoutChildren() {
        double widthRatio = (double) width / originalWidth;
        if (Math.abs(widthRatio) > Double.MAX_VALUE) {
            widthRatio = 1;
        }
        double heightRatio = (double) height / originalHeight;
        if (Math.abs(heightRatio) > Double.MAX_VALUE) {
            heightRatio = 1;
        }

        // AnchoredLayoutManager never makes it smaller than the original,
        // only bigger.
        if (widthRatio < 1) {
            widthRatio = 1;
        }

        for (TWidget child: children.keySet()) {
            Rectangle rect = children.get(child);
            AnchorData anchorData = anchors.get(child);

            int oldX = (int) (rect.x * widthRatio);
            int oldY = (int) (rect.y * heightRatio);
            int oldWidth = (int) (rect.width * widthRatio);
            int oldHeight = (int) (rect.height * heightRatio);
            if (child instanceof TButton) {
                // Buttons have fixed width
                oldWidth = (int) rect.width;
                oldHeight = (int) rect.height;
            }

            int newX = oldX;
            int newY = oldY;
            int newWidth = oldWidth;
            int newHeight = oldHeight;

            if (anchorData.widget == null) {
                switch (anchorData.anchor) {
                case TOP_LEFT:
                    newX = anchorData.padX;
                    newY = anchorData.padY;
                    break;
                case TOP_RIGHT:
                    newX = width - newWidth - anchorData.padX;
                    newY = anchorData.padY;
                    break;
                case BOTTOM_LEFT:
                    newX = anchorData.padX;
                    newY = height - newHeight - anchorData.padY;
                    break;
                case BOTTOM_RIGHT:
                    newX = width - newWidth - anchorData.padX;
                    newY = height - newHeight - anchorData.padY;
                    break;
                }
                newHeight = oldHeight + oldY - newY + 1;
                newWidth = oldWidth + oldX - newX + 1;
            } else {
                TWidget relativeWidget = anchorData.widget;
                switch (anchorData.anchor) {
                case TOP:
                    newX = relativeWidget.getX() + anchorData.padX;
                    newY = relativeWidget.getY() + relativeWidget.getHeight() + anchorData.padY;
                    break;
                case LEFT:
                    newX = relativeWidget.getX() + relativeWidget.getWidth() + anchorData.padX;
                    newY = relativeWidget.getY() + anchorData.padY;
                    break;
                case BOTTOM:
                    newX = relativeWidget.getX() + anchorData.padX;
                    newY = relativeWidget.getY() - (oldHeight + anchorData.padY);
                    break;
                case RIGHT:
                    newX = relativeWidget.getX() - (oldWidth + anchorData.padX);
                    newY = relativeWidget.getY() + anchorData.padY;
                    break;
                }
                newHeight = oldHeight + oldY - newY + 1;
                newWidth = oldWidth + oldX - newX + 1;
            }

            child.setDimensions(newX, newY, newWidth, newHeight);
        }
    }

    /**
     * Set a child widget's anchor.
     *
     * @param child the widget to manage
     * @param relativeWidget widget to anchor relative to, or null to anchor
     * to the containing widget
     * @param anchor one of Anchor.TOP_LEFT, Anchor.TOP_RIGHT, etc.
     */
    public void setAnchor(final TWidget child, final TWidget relativeWidget,
        final Anchor anchor) {

        Rectangle rect = children.get(child);
        AnchorData anchorData = anchors.get(child);

        anchorData.anchor = anchor;
        anchorData.widget = relativeWidget;
        anchorData.padX = (int) rect.x;
        anchorData.padY = (int) rect.y;

        if (anchorData.widget == null) {
            switch (anchorData.anchor) {
            case TOP_LEFT:
                anchorData.padX = (int) rect.x;
                anchorData.padY = (int) rect.y;
                break;
            case TOP_RIGHT:
                anchorData.padX = width - (int) (rect.x + rect.width);
                anchorData.padY = (int) rect.y;
                break;
            case BOTTOM_LEFT:
                anchorData.padX = (int) rect.x;
                anchorData.padY = height - (int) (rect.y + rect.height);
                break;
            case BOTTOM_RIGHT:
                anchorData.padX = width - (int) (rect.x + rect.width);
                anchorData.padY = height - (int) (rect.y + rect.height);
                break;
            default:
                throw new IllegalArgumentException("Only TOP_LEFT, TOP_RIGHT, "
                    + "BOTTOM_LEFT, BOTTOM_RIGHT supported for container "
                    + "anchors");
            }

        } else {
            switch (anchorData.anchor) {
            case TOP:
                anchorData.padX = (int) rect.x - relativeWidget.getX();
                anchorData.padY = (int) rect.y - (relativeWidget.getY() + relativeWidget.getHeight());
                break;
            case LEFT:
                anchorData.padX = (int) rect.x - (relativeWidget.getX() + relativeWidget.getWidth());
                anchorData.padY = (int) rect.y - relativeWidget.getY();
                break;
            case BOTTOM:
                anchorData.padX = (int) rect.x - relativeWidget.getX();
                anchorData.padY = relativeWidget.getY() - (int) (rect.y + rect.height);
                break;
            case RIGHT:
                anchorData.padX = relativeWidget.getX() - (int) (rect.x + rect.width);
                anchorData.padY = (int) rect.y - relativeWidget.getY();
                break;
            default:
                throw new IllegalArgumentException("Only TOP, LEFT, BOTTOM, "
                    + "RIGHT supported for relative anchors");
            }
        }

        layoutChildren();
    }

}
