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

import casciian.TAction;
import casciian.TApplication;
import casciian.TText;
import casciian.TWidget;
import casciian.TWindow;
import casciian.event.TResizeEvent;
import casciian.menu.TMenu;
import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * This window demonstates the TText, THScroller, and TVScroller widgets.
 */
public class DemoTextWindow extends TWindow {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * Hang onto my TText so I can resize it with the window.
     */
    private TText textField;

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
    public DemoTextWindow(final TApplication parent, final String title,
        final String text) {

        super(parent, title, 0, 0, 44, 22, RESIZABLE);
        i18n = ResourceBundle.getBundle(DemoTextWindow.class.getName(),
            getLocale());

        textField = addText(text, 1, 3, 40, 16);

        TWidget button = null;
        button = addButton(i18n.getString("left"), 1, 1, new TAction() {
                public void DO() {
                    textField.leftJustify();
                }
        });

        button = addButton(i18n.getString("center"),
            button.getX() + button.getWidth() + 2, 1, new TAction() {
                public void DO() {
                    textField.centerJustify();
                }
        });

        button = addButton(i18n.getString("right"),
            button.getX() + button.getWidth() + 2, 1, new TAction() {
                public void DO() {
                    textField.rightJustify();
                }
        });

        button = addButton(i18n.getString("full"),
            button.getX() + button.getWidth() + 2, 1, new TAction() {
                public void DO() {
                    textField.fullJustify();
                }
        });

        setWidth(button.getX() + button.getWidth() + 4);
        onResize(new TResizeEvent(getApplication().getBackend(),
                TResizeEvent.Type.WIDGET, getWidth(), getHeight()));

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

    /**
     * Public constructor.
     *
     * @param parent the main application
     */
    @SuppressWarnings("this-escape")
    public DemoTextWindow(final TApplication parent) {
        this(parent, "",
"This is an example of a reflowable text field.  Some example text follows.\n" +
"\n" +
"Notice that some menu items should be disabled when this window has focus.\n" +
"\n" +
"This library implements a text-based windowing system loosely " +
"reminiscent of Borland's [Turbo " +
"Vision](http://en.wikipedia.org/wiki/Turbo_Vision) library.  For those " +
"wishing to use the actual C++ Turbo Vision library, see [Sergio " +
"Sigala's updated version](http://tvision.sourceforge.net/) that runs " +
"on many more platforms.\n" +
"\n" +
"This library is licensed MIT.  See the file LICENSE for the full license " +
"for the details.\n");

        i18n = ResourceBundle.getBundle(DemoTextWindow.class.getName(),
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
            TResizeEvent textSize = new TResizeEvent(event.getBackend(),
                TResizeEvent.Type.WIDGET, event.getWidth() - 4,
                event.getHeight() - 6);
            textField.onResize(textSize);
            return;
        }

        // Pass to children instead
        for (TWidget widget: getChildren()) {
            widget.onResize(event);
        }
    }

    /**
     * Play with menu items.
     */
    public void onFocus() {
        getApplication().enableMenuItem(2001);
        getApplication().disableMenuItem(TMenu.MID_SHELL);
        getApplication().disableMenuItem(TMenu.MID_EXIT);
    }

    /**
     * Called by application.switchWindow() when another window gets the
     * focus.
     */
    public void onUnfocus() {
        getApplication().disableMenuItem(2001);
        getApplication().enableMenuItem(TMenu.MID_SHELL);
        getApplication().enableMenuItem(TMenu.MID_EXIT);
    }

}
