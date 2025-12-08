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
package casciian.bits;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import casciian.TWidget;
import casciian.TWindow;
import casciian.event.TResizeEvent;

/**
 * WidgetUtils contains methods to:
 *
 *    - Tile windows.
 */
public class WidgetUtils {

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Private constructor prevents accidental creation of this class.
     */
    private WidgetUtils() {}

    // ------------------------------------------------------------------------
    // WidgetUtils ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Re-layout a list of widgets as non-overlapping tiles into a
     * rectangular space.  This produces almost the same results as Turbo
     * Pascal 7.0's IDE.
     *
     * @param widgets the list of widgets
     * @param width the width of the rectangle to fit the widgets in
     * @param height the height of the rectangle to fit the widgets in
     * @param topLine the Y top use for a top-line widget
     */
    public static void tileWidgets(final List<? extends TWidget> widgets,
        final int width, final int height, final int topLine) {

        int z = widgets.size();
        for (TWidget w: widgets) {
            if (w instanceof TWindow) {
                if (((TWindow) w).isHidden()) {
                    z--;
                }
                continue;
            }
            if (!w.isVisible()) {
                z--;
            }
        }
        if (z == 0) {
            return;
        }
        assert (z > 0);

        int a = 0;
        int b = 0;
        a = (int)(Math.sqrt(z));
        int c = 0;
        while (c < a) {
            b = (z - c) / a;
            if (((a * b) + c) == z) {
                break;
            }
            c++;
        }
        assert (a > 0);
        assert (b > 0);
        assert (c < a);
        int newWidth = width / a;
        int newHeight1 = height / b;
        int newHeight2 = height / (b + c);

        List<TWidget> sorted = new ArrayList<TWidget>();
        for (TWidget w: widgets) {
            if (w instanceof TWindow) {
                if (((TWindow) w).isShown()) {
                    sorted.add(w);
                }
                continue;
            }
            if (w.isVisible()) {
                sorted.add(w);
            }
        }

        Collections.sort(sorted);
        if (sorted.get(0) instanceof TWindow) {
            Collections.reverse(sorted);
        }
        for (int i = 0; i < sorted.size(); i++) {
            int logicalX = i / b;
            int logicalY = i % b;
            if (i >= ((a - 1) * b)) {
                logicalX = a - 1;
                logicalY = i - ((a - 1) * b);
            }

            TWidget w = sorted.get(i);
            int oldWidth = w.getWidth();
            int oldHeight = w.getHeight();

            w.setX(logicalX * newWidth);
            w.setWidth(newWidth);
            if (i >= ((a - 1) * b)) {
                w.setY((logicalY * newHeight2) + topLine);
                w.setHeight(newHeight2);
            } else {
                w.setY((logicalY * newHeight1) + topLine);
                w.setHeight(newHeight1);
            }
            // Always call onResize so that a LayoutManager can have a chance
            // to do its work.
            w.onResize(new TResizeEvent(null, TResizeEvent.Type.WIDGET,
                    w.getWidth(), w.getHeight()));
        }
    }

}
