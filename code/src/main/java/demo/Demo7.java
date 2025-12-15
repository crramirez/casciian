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
package demo;

import java.util.ResourceBundle;

import casciian.TApplication;
import casciian.TPanel;
import casciian.TText;
import casciian.TWindow;
import casciian.event.TResizeEvent;
import casciian.layout.BoxLayoutManager;

/**
 * This class shows off BoxLayout and TPanel.
 */
public class Demo7 {

    /**
     * Translated strings.
     */
    private static final ResourceBundle i18n = ResourceBundle.getBundle(Demo7.class.getName());

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Default constructor.
     */
    public Demo7() {}

    // ------------------------------------------------------------------------
    // Demo7 ------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Main entry point.
     *
     * @param args Command line arguments
     * @throws Exception on error
     */
    public static void main(final String [] args) throws Exception {
        // This demo will build everything "from the outside".

        TApplication.BackendType backendType = TApplication.BackendType.XTERM;
        TApplication app = new TApplication(backendType);
        app.addToolMenu();
        app.addFileMenu();
        TWindow window = new TWindow(app, i18n.getString("windowTitle"),
            60, 22);
        window.setLayoutManager(new BoxLayoutManager(window.getWidth() - 2,
                window.getHeight() - 2, false));

        TPanel right = window.addPanel(0, 0, 10, 10);
        right.setBorderStyle("round");
        right.setTitle(i18n.getString("right"));
        TPanel left = window.addPanel(0, 0, 10, 10);
        left.setBorderStyle("singleVdoubleH");
        left.setTitle(i18n.getString("left"));
        left.setTitleDirection(TPanel.Direction.BOTTOM_RIGHT);

        right.setLayoutManager(new BoxLayoutManager(right.getWidth(),
                right.getHeight(), true));
        left.setLayoutManager(new BoxLayoutManager(left.getWidth(),
                left.getHeight(), true));

        left.addText("C1", 0, 0, left.getWidth(), left.getHeight());
        left.addText("C2", 0, 0, left.getWidth(), left.getHeight());
        left.addText("C3", 0, 0, left.getWidth(), left.getHeight());
        right.addText("C4", 0, 0, right.getWidth(), right.getHeight());
        right.addText("C5", 0, 0, right.getWidth(), right.getHeight());
        right.addText("C6", 0, 0, right.getWidth(), right.getHeight());
        window.onResize(new TResizeEvent(app.getBackend(),
                TResizeEvent.Type.WIDGET, window.getWidth(),
                window.getHeight()));

        app.run();
    }

}
