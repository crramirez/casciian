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

import java.util.ArrayList;

import casciian.TWidget;
import casciian.event.TResizeEvent;

/**
 * BoxLayoutManager repositions child widgets based on the order they are
 * added to the parent widget and desired orientation.
 */
public class BoxLayoutManager implements LayoutManager {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * If true, orient vertically.  If false, orient horizontally.
     */
    private boolean vertical = true;

    /**
     * Current width.
     */
    private int width = 0;

    /**
     * Current height.
     */
    private int height = 0;

    /**
     * Widgets being managed.
     */
    private ArrayList<TWidget> children = new ArrayList<TWidget>();

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param width the width of the parent widget
     * @param height the height of the parent widget
     * @param vertical if true, arrange widgets vertically
     */
    public BoxLayoutManager(final int width, final int height,
        final boolean vertical) {

        this.width = width;
        this.height = height;
        this.vertical = vertical;
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
            for (TWidget child: children) {
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
        children.add(child);
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
        // NOP
    }

    // ------------------------------------------------------------------------
    // BoxLayoutManager -------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Resize/reposition child widgets based on horizontal/vertical
     * arrangement.
     */
    private void layoutChildren() {
        if (children.size() == 0) {
            return;
        }
        if (vertical) {
            int widgetHeight = Math.max(1, height / children.size());
            int leftoverHeight = height % children.size();
            for (int i = 0; i < children.size() - 1; i++) {
                TWidget child = children.get(i);
                child.setDimensions(child.getX(), i * widgetHeight,
                    width, widgetHeight);
            }
            TWidget child = children.get(children.size() - 1);
            child.setDimensions(child.getX(),
                (children.size() - 1) * widgetHeight, width,
                widgetHeight + leftoverHeight);
        } else {
            int widgetWidth = Math.max(1, width / children.size());
            int leftoverWidth = width % children.size();
            for (int i = 0; i < children.size() - 1; i++) {
                TWidget child = children.get(i);
                child.setDimensions(i * widgetWidth, child.getY(),
                    widgetWidth, height);
            }
            TWidget child = children.get(children.size() - 1);
            child.setDimensions((children.size() - 1) * widgetWidth,
                child.getY(), widgetWidth + leftoverWidth, height);
        }
    }

}
