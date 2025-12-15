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
import casciian.TTable;
import casciian.TWidget;
import casciian.TWindow;
import casciian.event.TResizeEvent;
import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * This window demonstates the TTable widget.
 */
public class DemoTableWindow extends TWindow {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * Hang onto my TTable so I can resize it with the window.
     */
    private TTable tableField;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor makes a text window out of any string.
     *
     * @param parent the main application
     * @param title the text string
     */
    @SuppressWarnings("this-escape")
    public DemoTableWindow(final TApplication parent, final String title) {

        super(parent, title, 0, 0, 44, 22, RESIZABLE);
        i18n = ResourceBundle.getBundle(DemoTableWindow.class.getName(),
            getLocale());

        tableField = new TTable(this, 0, 0, 42, 20);

        statusBar = newStatusBar(i18n.getString("statusBar"));
        statusBar.addShortcutKeypress(kbF1, cmHelp,
            i18n.getString("statusBarHelp"));
        statusBar.addShortcutKeypress(kbF2, cmShell,
            i18n.getString("statusBarShell"));
        statusBar.addShortcutKeypress(kbF10, cmExit,
            i18n.getString("statusBarExit"));
    }

    /**
     * Public constructor.
     *
     * @param parent the main application
     */
    @SuppressWarnings("this-escape")
    public DemoTableWindow(final TApplication parent) {
        this(parent, "");
        i18n = ResourceBundle.getBundle(DemoTableWindow.class.getName(),
            getLocale());
        setTitle(i18n.getString("windowTitle"));
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle window/screen resize events.
     *
     * @param event resize event
     */
    @Override
    public void onResize(final TResizeEvent event) {
        if (event.getType() == TResizeEvent.Type.WIDGET) {
            // Resize the text field
            TResizeEvent tableSize = new TResizeEvent(event.getBackend(),
                TResizeEvent.Type.WIDGET, event.getWidth() - 2,
                event.getHeight() - 2);
            tableField.onResize(tableSize);
            return;
        }

        // Pass to children instead
        for (TWidget widget: getChildren()) {
            widget.onResize(event);
        }
    }

}
