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

import java.io.IOException;
import java.util.ResourceBundle;

import casciian.TApplication;
import casciian.TDirectoryTreeItem;
import casciian.TTreeViewScrollable;
import casciian.TWidget;
import casciian.TWindow;
import casciian.event.TResizeEvent;
import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * This window demonstates the TTreeView widget.
 */
public class DemoTreeViewWindow extends TWindow {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * Hang onto my TTreeView so I can resize it with the window.
     */
    private TTreeViewScrollable treeView;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent the main application
     * @throws IOException if a java.io operation throws
     */
    @SuppressWarnings("this-escape")
    public DemoTreeViewWindow(final TApplication parent) throws IOException {
        super(parent, "", 0, 0, 44, 16, TWindow.RESIZABLE);
        i18n = ResourceBundle.getBundle(DemoTreeViewWindow.class.getName(),
            getLocale());
        setTitle(i18n.getString("windowTitle"));

        // Load the treeview with "stuff"
        treeView = addTreeViewWidget(1, 1, 40, 12);
        new TDirectoryTreeItem(treeView, ".", true);

        statusBar = newStatusBar(i18n.getString("statusBar"));
        statusBar.addShortcutKeypress(kbF1, cmHelp,
            i18n.getString("statusBarHelp"));
        statusBar.addShortcutKeypress(kbF2, cmShell,
            i18n.getString("statusBarShell"));
        statusBar.addShortcutKeypress(kbF3, cmOpen,
            i18n.getString("statusBarOpen"));
        statusBar.addShortcutKeypress(kbF10, cmExit,
            i18n.getString("statusBarExit"));
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle window/screen resize events.
     *
     * @param resize resize event
     */
    @Override
    public void onResize(final TResizeEvent resize) {
        if (resize.getType() == TResizeEvent.Type.WIDGET) {
            // Resize the treeView field
            TResizeEvent treeSize = new TResizeEvent(resize.getBackend(),
                TResizeEvent.Type.WIDGET, resize.getWidth() - 4,
                resize.getHeight() - 4);
            treeView.onResize(treeSize);
            return;
        }

        // Pass to children instead
        for (TWidget widget: getChildren()) {
            widget.onResize(resize);
        }
    }

}
