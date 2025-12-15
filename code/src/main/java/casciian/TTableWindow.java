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

import casciian.event.TCommandEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TMenuEvent;
import casciian.event.TMouseEvent;
import casciian.event.TResizeEvent;
import casciian.menu.TMenu;
import casciian.menu.TMenuItem;
import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * TTableWindow is used to display and edit regular two-dimensional tables of
 * cells.
 */
public class TTableWindow extends TScrollableWindow {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * The table widget.
     */
    private TTable tableField;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent the main application
     * @param title window title
     * @param width width of window
     * @param height height of window
     */
    public TTableWindow(final TApplication parent, final String title,
        final int width, final int height) {

        this(parent, title, 0, 0, width, height, RESIZABLE);
    }

    /**
     * Public constructor.
     *
     * @param parent the main application
     * @param title window title, will be centered along the top border
     * @param width width of window
     * @param height height of window
     * @param flags bitmask of RESIZABLE, CENTERED, or MODAL
     */
    public TTableWindow(final TApplication parent, final String title,
        final int width, final int height, final int flags) {

        this(parent, title, 0, 0, width, height, flags);
    }

    /**
     * Public constructor.
     *
     * @param parent the main application
     * @param title window title, will be centered along the top border
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of window
     * @param height height of window
     */
    public TTableWindow(final TApplication parent, final String title,
        final int x, final int y, final int width, final int height) {

        this(parent, title, x, y, width, height, RESIZABLE);
    }

    /**
     * Public constructor.
     *
     * @param parent the main application
     * @param title window title
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of window
     * @param height height of window
     * @param flags mask of RESIZABLE, CENTERED, or MODAL
     */
    @SuppressWarnings("this-escape")
    public TTableWindow(final TApplication parent, final String title,
        final int x, final int y, final int width, final int height,
        final int flags) {

        super(parent, title, x, y, width, height, flags);
        i18n = ResourceBundle.getBundle(TTableWindow.class.getName(),
            getLocale());

        tableField = addTable(0, 0, getWidth() - 2, getHeight() - 2);
        setupAfterTable();
    }

    /**
     * Public constructor sets window title.
     *
     * @param parent the main application
     * @param title the window title
     */
    @SuppressWarnings("this-escape")
    public TTableWindow(final TApplication parent, final String title) {
        this(parent, title, 0, 0, parent.getScreen().getWidth() / 2,
            parent.getScreen().getHeight() / 2 - 2, RESIZABLE | CENTERED);
    }

    /**
     * Public constructor loads a grid from a RFC4180 CSV file.
     *
     * @param parent the main application
     * @param csvFile a File referencing the CSV data
     * @throws IOException if a java.io operation throws
     */
    @SuppressWarnings("this-escape")
    public TTableWindow(final TApplication parent,
        final File csvFile) throws IOException {

        super(parent, csvFile.getName(), 0, 0,
            parent.getScreen().getWidth() * 4 / 5,
            parent.getScreen().getHeight() / 2 - 2,
            RESIZABLE | CENTERED);
        i18n = ResourceBundle.getBundle(TTableWindow.class.getName(),
            getLocale());

        tableField = addTable(0, 0, getWidth() - 2, getHeight() - 2, 1, 1);
        setupAfterTable();
        tableField.loadCsvFile(csvFile);
    }

    /**
     * Public constructor loads a grid from a RFC4180 CSV file.
     *
     * @param parent the main application
     * @param csvFile a File referencing the CSV data
     * @param width width of window
     * @param height height of window
     * @throws IOException if a java.io operation throws
     */
    public TTableWindow(final TApplication parent, final File csvFile,
        final int width, final int height) throws IOException {

        this(parent, csvFile, 0, 0, width, height, RESIZABLE);
    }

    /**
     * Public constructor loads a grid from a RFC4180 CSV file.
     *
     * @param parent the main application
     * @param csvFile a File referencing the CSV data
     * @param width width of window
     * @param height height of window
     * @param flags bitmask of RESIZABLE, CENTERED, or MODAL
     * @throws IOException if a java.io operation throws
     */
    public TTableWindow(final TApplication parent, final File csvFile,
        final int width, final int height, final int flags) throws IOException {

        this(parent, csvFile, 0, 0, width, height, flags);
    }

    /**
     * Public constructor loads a grid from a RFC4180 CSV file.
     *
     * @param parent the main application
     * @param csvFile a File referencing the CSV data
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of window
     * @param height height of window
     * @throws IOException if a java.io operation throws
     */
    public TTableWindow(final TApplication parent, final File csvFile,
        final int x, final int y,
        final int width, final int height) throws IOException {

        this(parent, csvFile, x, y, width, height, RESIZABLE);
    }

    /**
     * Public constructor loads a grid from a RFC4180 CSV file.
     *
     * @param parent the main application
     * @param csvFile a File referencing the CSV data
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of window
     * @param height height of window
     * @param flags mask of RESIZABLE, CENTERED, or MODAL
     * @throws IOException if a java.io operation throws
     */
    @SuppressWarnings("this-escape")
    public TTableWindow(final TApplication parent, final File csvFile,
        final int x, final int y, final int width, final int height,
        final int flags) throws IOException {

        super(parent, csvFile.getName(), x, y, width, height, flags);
        i18n = ResourceBundle.getBundle(TTableWindow.class.getName(),
            getLocale());

        tableField = addTable(0, 0, getWidth() - 2, getHeight() - 2, 1, 1);
        setupAfterTable();
        tableField.loadCsvFile(csvFile);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Called by application.switchWindow() when this window gets the
     * focus, and also by application.addWindow().
     */
    public void onFocus() {
        // Enable the table menu items.
        getApplication().enableMenuItem(TMenu.MID_TABLE_RENAME_COLUMN);
        getApplication().enableMenuItem(TMenu.MID_TABLE_RENAME_ROW);
        getApplication().enableMenuItem(TMenu.MID_TABLE_VIEW_ROW_LABELS);
        getApplication().enableMenuItem(TMenu.MID_TABLE_VIEW_COLUMN_LABELS);
        getApplication().enableMenuItem(TMenu.MID_TABLE_VIEW_HIGHLIGHT_ROW);
        getApplication().enableMenuItem(TMenu.MID_TABLE_VIEW_HIGHLIGHT_COLUMN);
        getApplication().enableMenuItem(TMenu.MID_TABLE_BORDER_NONE);
        getApplication().enableMenuItem(TMenu.MID_TABLE_BORDER_ALL);
        getApplication().enableMenuItem(TMenu.MID_TABLE_BORDER_CELL_NONE);
        getApplication().enableMenuItem(TMenu.MID_TABLE_BORDER_CELL_ALL);
        getApplication().enableMenuItem(TMenu.MID_TABLE_BORDER_RIGHT);
        getApplication().enableMenuItem(TMenu.MID_TABLE_BORDER_LEFT);
        getApplication().enableMenuItem(TMenu.MID_TABLE_BORDER_TOP);
        getApplication().enableMenuItem(TMenu.MID_TABLE_BORDER_BOTTOM);
        getApplication().enableMenuItem(TMenu.MID_TABLE_BORDER_DOUBLE_BOTTOM);
        getApplication().enableMenuItem(TMenu.MID_TABLE_BORDER_THICK_BOTTOM);
        getApplication().enableMenuItem(TMenu.MID_TABLE_DELETE_LEFT);
        getApplication().enableMenuItem(TMenu.MID_TABLE_DELETE_UP);
        getApplication().enableMenuItem(TMenu.MID_TABLE_DELETE_ROW);
        getApplication().enableMenuItem(TMenu.MID_TABLE_DELETE_COLUMN);
        getApplication().enableMenuItem(TMenu.MID_TABLE_INSERT_LEFT);
        getApplication().enableMenuItem(TMenu.MID_TABLE_INSERT_RIGHT);
        getApplication().enableMenuItem(TMenu.MID_TABLE_INSERT_ABOVE);
        getApplication().enableMenuItem(TMenu.MID_TABLE_INSERT_BELOW);
        getApplication().enableMenuItem(TMenu.MID_TABLE_COLUMN_NARROW);
        getApplication().enableMenuItem(TMenu.MID_TABLE_COLUMN_WIDEN);
        getApplication().enableMenuItem(TMenu.MID_TABLE_FILE_OPEN_CSV);
        getApplication().enableMenuItem(TMenu.MID_TABLE_FILE_SAVE_CSV);
        getApplication().enableMenuItem(TMenu.MID_TABLE_FILE_SAVE_TEXT);

        if (tableField != null) {

            // Set the menu to match the flags.
            TMenuItem menuItem = getApplication().getMenuItem(TMenu.MID_TABLE_VIEW_ROW_LABELS);
            if (menuItem != null) {
                menuItem.setChecked(tableField.getShowRowLabels());
            }
            menuItem = getApplication().getMenuItem(TMenu.MID_TABLE_VIEW_COLUMN_LABELS);
            if (menuItem != null) {
                menuItem.setChecked(tableField.getShowColumnLabels());
            }
            menuItem = getApplication().getMenuItem(TMenu.MID_TABLE_VIEW_HIGHLIGHT_ROW);
            if (menuItem != null) {
                menuItem.setChecked(tableField.getHighlightRow());
            }
            menuItem = getApplication().getMenuItem(TMenu.MID_TABLE_VIEW_HIGHLIGHT_COLUMN);
            if (menuItem != null) {
                menuItem.setChecked(tableField.getHighlightColumn());
            }
        }
    }

    /**
     * Called by application.switchWindow() when another window gets the
     * focus.
     */
    public void onUnfocus() {
        // Disable the table menu items.
        getApplication().disableMenuItem(TMenu.MID_TABLE_RENAME_COLUMN);
        getApplication().disableMenuItem(TMenu.MID_TABLE_RENAME_ROW);
        getApplication().disableMenuItem(TMenu.MID_TABLE_VIEW_ROW_LABELS);
        getApplication().disableMenuItem(TMenu.MID_TABLE_VIEW_COLUMN_LABELS);
        getApplication().disableMenuItem(TMenu.MID_TABLE_VIEW_HIGHLIGHT_ROW);
        getApplication().disableMenuItem(TMenu.MID_TABLE_VIEW_HIGHLIGHT_COLUMN);
        getApplication().disableMenuItem(TMenu.MID_TABLE_BORDER_NONE);
        getApplication().disableMenuItem(TMenu.MID_TABLE_BORDER_ALL);
        getApplication().disableMenuItem(TMenu.MID_TABLE_BORDER_CELL_NONE);
        getApplication().disableMenuItem(TMenu.MID_TABLE_BORDER_CELL_ALL);
        getApplication().disableMenuItem(TMenu.MID_TABLE_BORDER_RIGHT);
        getApplication().disableMenuItem(TMenu.MID_TABLE_BORDER_LEFT);
        getApplication().disableMenuItem(TMenu.MID_TABLE_BORDER_TOP);
        getApplication().disableMenuItem(TMenu.MID_TABLE_BORDER_BOTTOM);
        getApplication().disableMenuItem(TMenu.MID_TABLE_BORDER_DOUBLE_BOTTOM);
        getApplication().disableMenuItem(TMenu.MID_TABLE_BORDER_THICK_BOTTOM);
        getApplication().disableMenuItem(TMenu.MID_TABLE_DELETE_LEFT);
        getApplication().disableMenuItem(TMenu.MID_TABLE_DELETE_UP);
        getApplication().disableMenuItem(TMenu.MID_TABLE_DELETE_ROW);
        getApplication().disableMenuItem(TMenu.MID_TABLE_DELETE_COLUMN);
        getApplication().disableMenuItem(TMenu.MID_TABLE_INSERT_LEFT);
        getApplication().disableMenuItem(TMenu.MID_TABLE_INSERT_RIGHT);
        getApplication().disableMenuItem(TMenu.MID_TABLE_INSERT_ABOVE);
        getApplication().disableMenuItem(TMenu.MID_TABLE_INSERT_BELOW);
        getApplication().disableMenuItem(TMenu.MID_TABLE_COLUMN_NARROW);
        getApplication().disableMenuItem(TMenu.MID_TABLE_COLUMN_WIDEN);
        getApplication().disableMenuItem(TMenu.MID_TABLE_FILE_OPEN_CSV);
        getApplication().disableMenuItem(TMenu.MID_TABLE_FILE_SAVE_CSV);
        getApplication().disableMenuItem(TMenu.MID_TABLE_FILE_SAVE_TEXT);
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        // Use TWidget's code to pass the event to the children.
        super.onMouseDown(mouse);

        if (mouseOnTable(mouse)) {
            // The table might have changed, update the scollbars.
            setBottomValue(tableField.getRowCount() - 1);
            setVerticalValue(tableField.getSelectedRowNumber());
            setRightValue(tableField.getColumnCount() - 1);
            setHorizontalValue(tableField.getSelectedColumnNumber());
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

        if (mouse.isMouse1() && mouseOnVerticalScroller(mouse)) {
            // Clicked/dragged on vertical scrollbar.
            tableField.setSelectedRowNumber(getVerticalValue());
        }
        if (mouse.isMouse1() && mouseOnHorizontalScroller(mouse)) {
            // Clicked/dragged on horizontal scrollbar.
            tableField.setSelectedColumnNumber(getHorizontalValue());
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

        if (mouseOnTable(mouse) && mouse.isMouse1()) {
            // The table might have changed, update the scollbars.
            setBottomValue(tableField.getRowCount() - 1);
            setVerticalValue(tableField.getSelectedRowNumber());
            setRightValue(tableField.getColumnCount() - 1);
            setHorizontalValue(tableField.getSelectedColumnNumber());
        } else {
            if (mouse.isMouse1() && mouseOnVerticalScroller(mouse)) {
                // Clicked/dragged on vertical scrollbar.
                tableField.setSelectedRowNumber(getVerticalValue());
            }
            if (mouse.isMouse1() && mouseOnHorizontalScroller(mouse)) {
                // Clicked/dragged on horizontal scrollbar.
                tableField.setSelectedColumnNumber(getHorizontalValue());
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
        // Use TWidget's code to pass the event to the children.
        super.onKeypress(keypress);

        // The table might have changed, update the scollbars.
        setBottomValue(tableField.getRowCount() - 1);
        setVerticalValue(tableField.getSelectedRowNumber());
        setRightValue(tableField.getColumnCount() - 1);
        setHorizontalValue(tableField.getSelectedColumnNumber());
    }

    /**
     * Handle window/screen resize events.
     *
     * @param event resize event
     */
    @Override
    public void onResize(final TResizeEvent event) {
        if (event.getType() == TResizeEvent.Type.WIDGET) {
            // Resize the table
            TResizeEvent tableSize = new TResizeEvent(event.getBackend(),
                TResizeEvent.Type.WIDGET, event.getWidth() - 2,
                event.getHeight() - 2);
            tableField.onResize(tableSize);

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
                        new TTableWindow(getApplication(), new File(filename));
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
                String filename = fileSaveBox(".");
                if (filename != null) {
                    tableField.saveToCsvFilename(filename);
                }
            } catch (IOException e) {
                messageBox(i18n.getString("errorDialogTitle"),
                    MessageFormat.format(i18n.
                        getString("errorWritingFile"), e.getMessage()));
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
        TInputBox inputBox = null;
        String filename = null;

        switch (menu.getId()) {
        case TMenu.MID_TABLE_RENAME_COLUMN:
            inputBox = inputBox(i18n.getString("renameColumnInputTitle"),
                i18n.getString("renameColumnInputCaption"),
                tableField.getColumnLabel(tableField.getSelectedColumnNumber()),
                TMessageBox.Type.OKCANCEL);
            if (inputBox.isOk()) {
                tableField.setColumnLabel(tableField.getSelectedColumnNumber(),
                    inputBox.getText());
            }
            return;
        case TMenu.MID_TABLE_RENAME_ROW:
            inputBox = inputBox(i18n.getString("renameRowInputTitle"),
                i18n.getString("renameRowInputCaption"),
                tableField.getRowLabel(tableField.getSelectedRowNumber()),
                TMessageBox.Type.OKCANCEL);
            if (inputBox.isOk()) {
                tableField.setRowLabel(tableField.getSelectedRowNumber(),
                    inputBox.getText());
            }
            return;
        case TMenu.MID_TABLE_VIEW_ROW_LABELS:
            tableField.setShowRowLabels(getApplication().getMenuItem(
                menu.getId()).isChecked());
            return;
        case TMenu.MID_TABLE_VIEW_COLUMN_LABELS:
            tableField.setShowColumnLabels(getApplication().getMenuItem(
                menu.getId()).isChecked());
            return;
        case TMenu.MID_TABLE_VIEW_HIGHLIGHT_ROW:
            tableField.setHighlightRow(getApplication().getMenuItem(
                menu.getId()).isChecked());
            return;
        case TMenu.MID_TABLE_VIEW_HIGHLIGHT_COLUMN:
            tableField.setHighlightColumn(getApplication().getMenuItem(
                menu.getId()).isChecked());
            return;
        case TMenu.MID_TABLE_BORDER_NONE:
            tableField.setBorderAllNone();
            return;
        case TMenu.MID_TABLE_BORDER_ALL:
            tableField.setBorderAllSingle();
            return;
        case TMenu.MID_TABLE_BORDER_CELL_NONE:
            tableField.setBorderCellNone();
            return;
        case TMenu.MID_TABLE_BORDER_CELL_ALL:
            tableField.setBorderCellSingle();
            return;
        case TMenu.MID_TABLE_BORDER_RIGHT:
            tableField.setBorderColumnRightSingle();
            return;
        case TMenu.MID_TABLE_BORDER_LEFT:
            tableField.setBorderColumnLeftSingle();
            return;
        case TMenu.MID_TABLE_BORDER_TOP:
            tableField.setBorderRowAboveSingle();
            return;
        case TMenu.MID_TABLE_BORDER_BOTTOM:
            tableField.setBorderRowBelowSingle();
            return;
        case TMenu.MID_TABLE_BORDER_DOUBLE_BOTTOM:
            tableField.setBorderRowBelowDouble();
            return;
        case TMenu.MID_TABLE_BORDER_THICK_BOTTOM:
            tableField.setBorderRowBelowThick();
            return;
        case TMenu.MID_TABLE_DELETE_LEFT:
            tableField.deleteCellShiftLeft();
            return;
        case TMenu.MID_TABLE_DELETE_UP:
            tableField.deleteCellShiftUp();
            return;
        case TMenu.MID_TABLE_DELETE_ROW:
            tableField.deleteRow(tableField.getSelectedRowNumber());
            return;
        case TMenu.MID_TABLE_DELETE_COLUMN:
            tableField.deleteColumn(tableField.getSelectedColumnNumber());
            return;
        case TMenu.MID_TABLE_INSERT_LEFT:
            tableField.insertColumnLeft(tableField.getSelectedColumnNumber());
            return;
        case TMenu.MID_TABLE_INSERT_RIGHT:
            tableField.insertColumnRight(tableField.getSelectedColumnNumber());
            return;
        case TMenu.MID_TABLE_INSERT_ABOVE:
            tableField.insertRowAbove(tableField.getSelectedColumnNumber());
            return;
        case TMenu.MID_TABLE_INSERT_BELOW:
            tableField.insertRowBelow(tableField.getSelectedColumnNumber());
            return;
        case TMenu.MID_TABLE_COLUMN_NARROW:
            tableField.setColumnWidth(tableField.getSelectedColumnNumber(),
                tableField.getColumnWidth(tableField.getSelectedColumnNumber()) - 1);
            return;
        case TMenu.MID_TABLE_COLUMN_WIDEN:
            tableField.setColumnWidth(tableField.getSelectedColumnNumber(),
                tableField.getColumnWidth(tableField.getSelectedColumnNumber()) + 1);
            return;
        case TMenu.MID_TABLE_FILE_OPEN_CSV:
            try {
                filename = fileOpenBox(".");
                if (filename != null) {
                    try {
                        new TTableWindow(getApplication(), new File(filename));
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
        case TMenu.MID_TABLE_FILE_SAVE_CSV:
            try {
                filename = fileSaveBox(".");
                if (filename != null) {
                    tableField.saveToCsvFilename(filename);
                }
            } catch (IOException e) {
                messageBox(i18n.getString("errorDialogTitle"),
                    MessageFormat.format(i18n.
                        getString("errorWritingFile"), e.getMessage()));
            }
            return;
        case TMenu.MID_TABLE_FILE_SAVE_TEXT:
            try {
                filename = fileSaveBox(".");
                if (filename != null) {
                    tableField.saveToTextFilename(filename);
                }
            } catch (IOException e) {
                messageBox(i18n.getString("errorDialogTitle"),
                    MessageFormat.format(i18n.
                        getString("errorWritingFile"), e.getMessage()));
            }
            return;
        default:
            break;
        }

        super.onMenu(menu);
    }

    // ------------------------------------------------------------------------
    // TTableWindow -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Setup other fields after the table is created.
     */
    private void setupAfterTable() {
        hScroller = new THScroller(this, 17, getHeight() - 2, getWidth() - 20);
        vScroller = new TVScroller(this, getWidth() - 2, 0, getHeight() - 2);
        setMinimumWindowWidth(25);
        setMinimumWindowHeight(10);
        setTopValue(tableField.getSelectedRowNumber());
        setBottomValue(tableField.getRowCount() - 1);
        setLeftValue(tableField.getSelectedColumnNumber());
        setRightValue(tableField.getColumnCount() - 1);

        statusBar = newStatusBar(i18n.getString("statusBar"));
        statusBar.addShortcutKeypress(kbF1, cmHelp,
            i18n.getString("statusBarHelp"));

        statusBar.addShortcutKeypress(kbF2, cmSave,
            i18n.getString("statusBarSave"));
        statusBar.addShortcutKeypress(kbF3, cmOpen,
            i18n.getString("statusBarOpen"));
        statusBar.addShortcutKeypress(kbShiftF10, cmMenu,
            i18n.getString("statusBarMenu"));

        // Synchronize the menu with tableField's flags.
        onFocus();
    }

    /**
     * Check if a mouse press/release/motion event coordinate is over the
     * table.
     *
     * @param mouse a mouse-based event
     * @return whether or not the mouse is on the table
     */
    private boolean mouseOnTable(final TMouseEvent mouse) {
        if ((mouse.getAbsoluteX() >= getAbsoluteX() + 1)
            && (mouse.getAbsoluteX() <  getAbsoluteX() + getWidth() - 1)
            && (mouse.getAbsoluteY() >= getAbsoluteY() + 1)
            && (mouse.getAbsoluteY() <  getAbsoluteY() + getHeight() - 1)
        ) {
            return true;
        }
        return false;
    }

    /**
     * Retrieve the underlying TTable.
     *
     * @return the table widget
     */
    public TTable getTableWidget() {
        return tableField;
    }

}
