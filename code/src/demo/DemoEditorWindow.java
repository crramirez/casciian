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
import casciian.TEditor;
import casciian.TWidget;
import casciian.TWindow;
import casciian.event.TResizeEvent;
import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * This window demonstates the TEditor widget.
 */
public class DemoEditorWindow extends TWindow {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * Hang onto my TEditor so I can resize it with the window.
     */
    private TEditor editField;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor makes a text window out of any string.
     *
     * @param parent the main application
     * @param title the text string
     * @param text the text string
     */
    @SuppressWarnings("this-escape")
    public DemoEditorWindow(final TApplication parent, final String title,
        final String text) {

        super(parent, title, 0, 0, 44, 22, RESIZABLE);
        i18n = ResourceBundle.getBundle(DemoEditorWindow.class.getName(),
            getLocale());

        editField = addEditor(text, 0, 0, 42, 20);

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
    public DemoEditorWindow(final TApplication parent) {
        this(parent, "",
"This is an example of an editable text field.  Some example text follows.\n" +
"\n" +
"This library implements a text-based windowing system loosely\n" +
"reminiscent of Borland's [Turbo\n" +
"Vision](http://en.wikipedia.org/wiki/Turbo_Vision) library.  For those\n" +
"wishing to use the actual C++ Turbo Vision library, see [Sergio\n" +
"Sigala's updated version](http://tvision.sourceforge.net/) that runs\n" +
"on many more platforms.\n" +
"\n" +
"This library is licensed MIT.  See the file LICENSE for the full license\n" +
"for the details.\n" +
"\n" +
"package demo;\n" +
"\n" +
"import casciian.*;\n" +
"import casciian.event.*;\n" +
"import static casciian.TCommand.*;\n" +
"import static casciian.TKeypress.*;\n" +
"\n" +
"/**\n" +
" * This window demonstates the TText, THScroller, and TVScroller widgets.\n" +
" */\n" +
"public class DemoEditorWindow extends TWindow {\n" +
"\n" +
"1 2 3 123\n" +
"\n"
        );
        i18n = ResourceBundle.getBundle(DemoEditorWindow.class.getName(),
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
            TResizeEvent editSize = new TResizeEvent(event.getBackend(),
                TResizeEvent.Type.WIDGET, event.getWidth() - 2,
                event.getHeight() - 2);
            editField.onResize(editSize);
            return;
        }

        // Pass to children instead
        for (TWidget widget: getChildren()) {
            widget.onResize(event);
        }
    }

}
