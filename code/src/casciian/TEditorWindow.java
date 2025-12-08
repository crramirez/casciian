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

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.Scanner;

import casciian.bits.CellAttributes;
import casciian.bits.GraphicsChars;
import casciian.event.TCommandEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TMenuEvent;
import casciian.event.TMouseEvent;
import casciian.event.TResizeEvent;
import casciian.menu.TMenu;
import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * TEditorWindow is a basic text file editor.
 */
public class TEditorWindow extends TScrollableWindow {

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

    /**
     * The fully-qualified name of the file being edited.
     */
    private String filename = "";

    /**
     * If true, hide the mouse after typing a keystroke.
     */
    private boolean hideMouseWhenTyping = true;

    /**
     * If true, the mouse should not be displayed because a keystroke was
     * typed.
     */
    private boolean typingHidMouse = false;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor sets window title.
     *
     * @param parent the main application
     * @param title the window title
     */
    @SuppressWarnings("this-escape")
    public TEditorWindow(final TApplication parent, final String title) {

        super(parent, title, 0, 0, parent.getScreen().getWidth(),
            parent.getDesktopBottom() - parent.getDesktopTop(), RESIZABLE);
        i18n = ResourceBundle.getBundle(TEditorWindow.class.getName(),
            getLocale());

        editField = addEditor("", 0, 0, getWidth() - 2, getHeight() - 2);
        setupAfterEditor();
    }

    /**
     * Public constructor sets window title and contents.
     *
     * @param parent the main application
     * @param title the window title, usually a filename
     * @param contents the data for the editing window, usually the file data
     */
    @SuppressWarnings("this-escape")
    public TEditorWindow(final TApplication parent, final String title,
        final String contents) {

        super(parent, title, 0, 0, parent.getScreen().getWidth(),
            parent.getDesktopBottom() - parent.getDesktopTop(), RESIZABLE);
        i18n = ResourceBundle.getBundle(TEditorWindow.class.getName(),
            getLocale());

        filename = title;
        editField = addEditor(contents, 0, 0, getWidth() - 2, getHeight() - 2);
        setupAfterEditor();
    }

    /**
     * Public constructor opens a file.
     *
     * @param parent the main application
     * @param file the file to open
     * @throws IOException if a java.io operation throws
     */
    @SuppressWarnings("this-escape")
    public TEditorWindow(final TApplication parent,
        final File file) throws IOException {

        super(parent, file.getName(), 0, 0, parent.getScreen().getWidth(),
            parent.getDesktopBottom() - parent.getDesktopTop(), RESIZABLE);
        i18n = ResourceBundle.getBundle(TEditorWindow.class.getName(),
            getLocale());

        filename = file.getName();
        String contents = readFileData(file);
        editField = addEditor(contents, 0, 0, getWidth() - 2, getHeight() - 2);
        setupAfterEditor();
    }

    /**
     * Public constructor.
     *
     * @param parent the main application
     */
    @SuppressWarnings("this-escape")
    public TEditorWindow(final TApplication parent) {
        this(parent, "");

        setTitle(i18n.getString("newTextDocument"));
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Called by application.switchWindow() when this window gets the
     * focus, and also by application.addWindow().
     */
    public void onFocus() {
        super.onFocus();
        getApplication().enableMenuItem(TMenu.MID_UNDO);
        getApplication().enableMenuItem(TMenu.MID_REDO);
    }

    /**
     * Called by application.switchWindow() when another window gets the
     * focus.
     */
    public void onUnfocus() {
        super.onUnfocus();
        getApplication().disableMenuItem(TMenu.MID_UNDO);
        getApplication().disableMenuItem(TMenu.MID_REDO);
    }

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        // Use TWidget's code to pass the event to the children.
        super.onMouseDown(mouse);

        if (hideMouseWhenTyping) {
            typingHidMouse = false;
        }

        if (mouseOnEditor(mouse)) {
            // The editor might have changed, update the scollbars.
            setBottomValue(editField.getMaximumRowNumber());
            setVerticalValue(editField.getVisibleRowNumber());
            setRightValue(editField.getMaximumColumnNumber());
            setHorizontalValue(editField.getEditingColumnNumber());
        } else {
            if (mouse.isMouseWheelUp() || mouse.isMouseWheelDown()) {
                // Vertical scrollbar actions
                editField.setVisibleRowNumber(getVerticalValue());
            }
        }
    }

    /**
     * Handle mouse release events.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        // Use TWidget's code to pass the event to the children.
        super.onMouseUp(mouse);

        if (hideMouseWhenTyping) {
            typingHidMouse = false;
        }

        if (mouse.isMouse1() && mouseOnVerticalScroller(mouse)) {
            // Clicked on vertical scrollbar
            editField.setVisibleRowNumber(getVerticalValue());
        }
        if (mouse.isMouse1() && mouseOnHorizontalScroller(mouse)) {
            // Clicked on horizontal scrollbar
            editField.setVisibleColumnNumber(getHorizontalValue());
            setHorizontalValue(editField.getVisibleColumnNumber());
        }
    }

    /**
     * Method that subclasses can override to handle mouse movements.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        // Use TWidget's code to pass the event to the children.
        super.onMouseMotion(mouse);

        if (hideMouseWhenTyping) {
            typingHidMouse = false;
        }

        if (mouseOnEditor(mouse) && mouse.isMouse1()) {
            // The editor might have changed, update the scollbars.
            setBottomValue(editField.getMaximumRowNumber());
            setVerticalValue(editField.getVisibleRowNumber());
            setRightValue(editField.getMaximumColumnNumber());
            setHorizontalValue(editField.getEditingColumnNumber());
        } else {
            if (mouse.isMouse1() && mouseOnVerticalScroller(mouse)) {
                // Clicked/dragged on vertical scrollbar
                editField.setVisibleRowNumber(getVerticalValue());
            }
            if (mouse.isMouse1() && mouseOnHorizontalScroller(mouse)) {
                // Clicked/dragged on horizontal scrollbar
                editField.setVisibleColumnNumber(getHorizontalValue());
                setHorizontalValue(editField.getVisibleColumnNumber());
            }
        }

    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (hideMouseWhenTyping) {
            typingHidMouse = true;
        }

        // Use TWidget's code to pass the event to the children.
        super.onKeypress(keypress);

        // The editor might have changed, update the scollbars.
        setBottomValue(editField.getMaximumRowNumber());
        setVerticalValue(editField.getVisibleRowNumber());
        setRightValue(editField.getMaximumColumnNumber());
        setHorizontalValue(editField.getEditingColumnNumber());
    }

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

            // Have TScrollableWindow handle the scrollbars
            super.onResize(event);
            return;
        }

        // Pass to children instead
        for (TWidget widget: getChildren()) {
            widget.onResize(event);
        }
    }

    /**
     * Method that subclasses can override to handle posted command events.
     *
     * @param command command event
     */
    @Override
    public void onCommand(final TCommandEvent command) {
        if (command.equals(cmOpen)) {
            try {
                String filename = fileOpenBox(".");
                if (filename != null) {
                    try {
                        String contents = readFileData(filename);
                        new TEditorWindow(getApplication(), filename, contents);
                    } catch (IOException e) {
                        messageBox(i18n.getString("errorDialogTitle"),
                            MessageFormat.format(i18n.
                                getString("errorReadingFile"), e.getMessage()));
                    }
                }
            } catch (IOException e) {
                messageBox(i18n.getString("errorDialogTitle"),
                    MessageFormat.format(i18n.
                        getString("errorOpeningFileDialog"), e.getMessage()));
            }
            return;
        }

        if (command.equals(cmSave)) {
            try {
                if (filename.length() == 0) {
                    String filename = fileSaveBox(".");
                    if (filename != null) {
                        this.filename = filename;
                        setTitle(filename);
                    }
                }
                if (this.filename.length() > 0) {
                    editField.saveToFilename(this.filename);
                }
            } catch (IOException e) {
                messageBox(i18n.getString("errorDialogTitle"),
                    MessageFormat.format(i18n.
                        getString("errorSavingFile"), e.getMessage()));
            }
            return;
        }

        if (command.equals(cmSaveAs)) {
            try {
                String filename = fileSaveBox(".");
                if (filename != null) {
                    if (filename.length() > 0) {
                        editField.saveToFilename(filename);
                        this.filename = filename;
                        setTitle(this.filename);
                    }
                }
            } catch (IOException e) {
                messageBox(i18n.getString("errorDialogTitle"),
                    MessageFormat.format(i18n.
                        getString("errorSavingFile"), e.getMessage()));
            }
            return;
        }

        // Didn't handle it, let children get it instead
        super.onCommand(command);
    }

    /**
     * Handle posted menu events.
     *
     * @param menu menu event
     */
    @Override
    public void onMenu(final TMenuEvent menu) {
        switch (menu.getId()) {
        case TMenu.MID_UNDO:
            editField.undo();
            return;

        case TMenu.MID_REDO:
            editField.redo();
            return;

        default:
            break;
        }

        super.onMenu(menu);
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the window.
     */
    @Override
    public void draw() {
        // Draw as normal.
        super.draw();

        // Add the row:col on the bottom row
        CellAttributes borderColor = getBorder();
        String location = String.format(" %d:%d ",
            editField.getEditingRowNumber(),
            editField.getEditingColumnNumber());
        int colon = location.indexOf(':');
        putStringXY(10 - colon, getHeight() - 1, location, borderColor);

        if (editField.isDirty()) {
            putCharXY(2, getHeight() - 1, GraphicsChars.OCTOSTAR, borderColor);
        }
    }

    /**
     * Returns true if this window does not want the application-wide mouse
     * pointer drawn over it.
     *
     * @return true if this window does not want the application-wide mouse
     * pointer drawn over it
     */
    @Override
    public boolean hasHiddenMouse() {
        return (super.hasHiddenMouse() || typingHidMouse);
    }

    // ------------------------------------------------------------------------
    // TEditorWindow ----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Setup other fields after the editor is created.
     */
    private void setupAfterEditor() {
        hScroller = new THScroller(this, 17, getHeight() - 2, getWidth() - 20);
        vScroller = new TVScroller(this, getWidth() - 2, 0, getHeight() - 2);
        setMinimumWindowWidth(25);
        setMinimumWindowHeight(10);
        setTopValue(1);
        setBottomValue(editField.getMaximumRowNumber());
        setLeftValue(1);
        setRightValue(editField.getMaximumColumnNumber());

        statusBar = newStatusBar(i18n.getString("statusBar"));
        statusBar.addShortcutKeypress(kbF1, cmHelp,
            i18n.getString("statusBarHelp"));
        statusBar.addShortcutKeypress(kbF2, cmSave,
            i18n.getString("statusBarSave"));
        statusBar.addShortcutKeypress(kbF3, cmSaveAs,
            i18n.getString("statusBarSaveAs"));
        statusBar.addShortcutKeypress(kbF4, cmOpen,
            i18n.getString("statusBarOpen"));
        statusBar.addShortcutKeypress(kbShiftF10, cmMenu,
            i18n.getString("statusBarMenu"));

        // Hide mouse when typing option
        if (System.getProperty("casciian.TEditor.hideMouseWhenTyping",
                "true").equals("false")) {

            hideMouseWhenTyping = false;
        }

        String marginString = System.getProperty("casciian.TEditor.margin");
        if (marginString != null) {
            try {
                int margin = Integer.parseInt(marginString);
                editField.setMargin(margin);
            } catch (NumberFormatException e) {
                // SQUASH
            }
        }
        editField.setAutoWrap(System.getProperty("casciian.TEditor.autoWrap",
                "false").equals("true"));
    }

    /**
     * Read file data into a string.
     *
     * @param file the file to open
     * @return the file contents
     * @throws IOException if a java.io operation throws
     */
    private String readFileData(final File file) throws IOException {
        StringBuilder fileContents = new StringBuilder();
        Scanner scanner = new Scanner(file);
        String EOL = System.getProperty("line.separator");

        try {
            while (scanner.hasNextLine()) {
                fileContents.append(scanner.nextLine() + EOL);
            }
            return fileContents.toString();
        } finally {
            scanner.close();
        }
    }

    /**
     * Read file data into a string.
     *
     * @param filename the file to open
     * @return the file contents
     * @throws IOException if a java.io operation throws
     */
    private String readFileData(final String filename) throws IOException {
        return readFileData(new File(filename));
    }

    /**
     * Check if a mouse press/release/motion event coordinate is over the
     * editor.
     *
     * @param mouse a mouse-based event
     * @return whether or not the mouse is on the editor
     */
    private boolean mouseOnEditor(final TMouseEvent mouse) {
        if ((mouse.getAbsoluteX() >= getAbsoluteX() + 1)
            && (mouse.getAbsoluteX() <  getAbsoluteX() + getWidth() - 1)
            && (mouse.getAbsoluteY() >= getAbsoluteY() + 1)
            && (mouse.getAbsoluteY() <  getAbsoluteY() + getHeight() - 1)
        ) {
            return true;
        }
        return false;
    }

}
