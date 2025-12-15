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

import casciian.TWidget;
import casciian.event.TResizeEvent;

/**
 * StretchLayoutManager repositions child widgets based on their coordinates
 * when added and the current widget size.
 */
public class StretchLayoutManager implements LayoutManager {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

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

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param width the width of the parent widget
     * @param height the height of the parent widget
     */
    public StretchLayoutManager(final int width, final int height) {
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
        layoutChildren();
    }

    /**
     * Remove a child widget from those managed by this LayoutManager.
     *
     * @param child the widget to remove
     */
    public void remove(final TWidget child) {
        children.remove(child);
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
    // StretchLayoutManager ---------------------------------------------------
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
        for (TWidget child: children.keySet()) {
            Rectangle rect = children.get(child);
            child.setDimensions((int) (rect.x * widthRatio),
                (int) (rect.y * heightRatio),
                (int) (rect.width * widthRatio),
                (int) (rect.height * heightRatio));
        }
    }

}
