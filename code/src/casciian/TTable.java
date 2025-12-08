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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import casciian.bits.CellAttributes;
import casciian.bits.StringUtils;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import casciian.event.TResizeEvent;
import static casciian.TKeypress.*;

/**
 * TTable is used to display and edit regular two-dimensional tables of
 * cells.
 *
 * This class was inspired by a TTable implementation originally developed by
 * David "Niki" ROULET [niki@nikiroo.be], made available under MIT at
 * https://github.com/nikiroo/jexer/tree/ttable_pull.
 */
public class TTable extends TWidget {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Available borders for cells.
     */
    public enum Border {
        /**
         * No border.
         */
        NONE,

        /**
         * Single bar: \u2502 (vertical) and \u2500 (horizontal).
         */
        SINGLE,

        /**
         * Double bar: \u2551 (vertical) and \u2550 (horizontal).
         */
        DOUBLE,

        /**
         * Thick bar: \u2503 (vertical heavy) and \u2501 (horizontal heavy).
         */
        THICK,
    }

    /**
     * If true, put a grid of numbers in the cells.
     */
    private static final boolean DEBUG = false;

    /**
     * Column label height.
     */
    private static final int COLUMN_LABEL_HEIGHT = 1;

    /**
     * Column default width.
     */
    private static final int COLUMN_DEFAULT_WIDTH = 8;

    /**
     * Extra rows to add.
     */
    private static final int EXTRA_ROWS = (DEBUG ? 10 : 0);

    /**
     * Extra columns to add.
     */
    private static final int EXTRA_COLUMNS = (DEBUG ? 3 : 0);

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The underlying data, organized as columns.
     */
    private ArrayList<Column> columns = new ArrayList<Column>();

    /**
     * The underlying data, organized as rows.
     */
    private ArrayList<Row> rows = new ArrayList<Row>();

    /**
     * The row in model corresponding to the top-left visible cell.
     */
    private int top = 0;

    /**
     * The column in model corresponding to the top-left visible cell.
     */
    private int left = 0;

    /**
     * The row in model corresponding to the currently selected cell.
     */
    private int selectedRow = 0;

    /**
     * The column in model corresponding to the currently selected cell.
     */
    private int selectedColumn = 0;

    /**
     * If true, highlight the entire row of the currently-selected cell.
     */
    private boolean highlightRow = false;

    /**
     * If true, highlight the entire column of the currently-selected cell.
     */
    private boolean highlightColumn = false;

    /**
     * If true, show the row labels as the first column.
     */
    private boolean showRowLabels = true;

    /**
     * If true, show the column labels as the first row.
     */
    private boolean showColumnLabels = true;

    /**
     * Row label width.
     */
    private int rowLabelWidth = 8;

    /**
     * The top border for the first row.
     */
    private Border topBorder = Border.NONE;

    /**
     * The left border for the first column.
     */
    private Border leftBorder = Border.NONE;

    /**
     * Column represents a column of cells.
     */
    public class Column {

        /**
         * X position of this column.
         */
        private int x = 0;

        /**
         * Width of column.
         */
        private int width = COLUMN_DEFAULT_WIDTH;

        /**
         * The cells of this column.
         */
        private ArrayList<Cell> cells = new ArrayList<Cell>();

        /**
         * Column label.
         */
        private String label = "";

        /**
         * The right border for this column.
         */
        private Border rightBorder = Border.NONE;

        /**
         * Constructor sets label to lettered column.
         *
         * @param col column number to use for this column.  Column 0 will be
         * "A", column 1 will be "B", column 26 will be "AA", and so on.
         */
        Column(int col) {
            label = makeColumnLabel(col);
        }

        /**
         * Add an entry to this column.
         *
         * @param cell the cell to add
         */
        public void add(final Cell cell) {
            cells.add(cell);
        }

        /**
         * Get an entry from this column.
         *
         * @param row the entry index to get
         * @return the cell at row
         */
        public Cell get(final int row) {
            return cells.get(row);
        }

        /**
         * Get the X position of the cells in this column.
         *
         * @return the position
         */
        public int getX() {
            return x;
        }

        /**
         * Set the X position of the cells in this column.
         *
         * @param x the position
         */
        public void setX(final int x) {
            for (Cell cell: cells) {
                cell.setX(x);
            }
            this.x = x;
        }

    }

    /**
     * Row represents a row of cells.
     */
    public class Row {

        /**
         * Y position of this row.
         */
        private int y = 0;

        /**
         * Height of row.
         */
        private int height = 1;

        /**
         * The cells of this row.
         */
        private ArrayList<Cell> cells = new ArrayList<Cell>();

        /**
         * Row label.
         */
        private String label = "";

        /**
         * The bottom border for this row.
         */
        private Border bottomBorder = Border.NONE;

        /**
         * Constructor sets label to numbered row.
         *
         * @param row row number to use for this row
         */
        Row(final int row) {
            label = Integer.toString(row);
        }

        /**
         * Add an entry to this column.
         *
         * @param cell the cell to add
         */
        public void add(final Cell cell) {
            cells.add(cell);
        }

        /**
         * Get an entry from this row.
         *
         * @param column the entry index to get
         * @return the cell at column
         */
        public Cell get(final int column) {
            return cells.get(column);
        }
        /**
         * Get the Y position of the cells in this column.
         *
         * @return the position
         */
        public int getY() {
            return y;
        }

        /**
         * Set the Y position of the cells in this column.
         *
         * @param y the position
         */
        public void setY(final int y) {
            for (Cell cell: cells) {
                cell.setY(y);
            }
            this.y = y;
        }

    }

    /**
     * Cell represents an editable cell in the table.  Normally, navigation
     * to a cell only highlights it; pressing Enter or F2 will switch to
     * editing mode.
     */
    public class Cell extends TWidget {

        // --------------------------------------------------------------------
        // Variables ----------------------------------------------------------
        // --------------------------------------------------------------------

        /**
         * The field containing the cell's data.
         */
        private TField field;

        /**
         * The column of this cell.
         */
        private int column;

        /**
         * The row of this cell.
         */
        private int row;

        /**
         * If true, the cell is being edited.
         */
        private boolean isEditing = false;

        /**
         * If true, the cell is read-only (non-editable).
         */
        private boolean readOnly = false;

        /**
         * Text of field before editing.
         */
        private String fieldText;

        // --------------------------------------------------------------------
        // Constructors -------------------------------------------------------
        // --------------------------------------------------------------------

        /**
         * Public constructor.
         *
         * @param parent parent widget
         * @param x column relative to parent
         * @param y row relative to parent
         * @param width width of widget
         * @param height height of widget
         * @param column column index of this cell
         * @param row row index of this cell
         */
        @SuppressWarnings("this-escape")
        public Cell(final TTable parent, final int x, final int y,
            final int width, final int height, final int column,
            final int row) {

            super(parent, x, y, width, height);
            this.column = column;
            this.row = row;

            field = addField(0, 0, width, false);
            field.setEnabled(false);
            field.setBackgroundChar(' ');
        }

        // --------------------------------------------------------------------
        // Event handlers -----------------------------------------------------
        // --------------------------------------------------------------------

        /**
         * Handle mouse double-click events.
         *
         * @param mouse mouse double-click event
         */
        @Override
        public void onMouseDoubleClick(final TMouseEvent mouse) {
            // Use TWidget's code to pass the event to the children.
            super.onMouseDoubleClick(mouse);

            // Double-click means to start editing.
            fieldText = field.getText();
            isEditing = true;
            field.setEnabled(true);
            activate(field);

            if (isActive()) {
                // Let the table know that I was activated.
                ((TTable) getParent()).setSelectedRowNumber(row);
                ((TTable) getParent()).setSelectedColumnNumber(column);
                ((TTable) getParent()).alignGrid();
            }
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

            if (isActive()) {
                // Let the table know that I was activated.
                ((TTable) getParent()).setSelectedRowNumber(row);
                ((TTable) getParent()).setSelectedColumnNumber(column);
                ((TTable) getParent()).alignGrid();
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

            if (isActive()) {
                // Let the table know that I was activated.
                ((TTable) getParent()).setSelectedRowNumber(row);
                ((TTable) getParent()).setSelectedColumnNumber(column);
                ((TTable) getParent()).alignGrid();
            }
        }

        /**
         * Handle keystrokes.
         *
         * @param keypress keystroke event
         */
        @Override
        public void onKeypress(final TKeypressEvent keypress) {
            // System.err.println("Cell onKeypress: " + keypress);

            if (isEditing) {
                if (keypress.equals(kbEsc)) {
                    // ESC cancels the edit.
                    cancelEdit();
                    return;
                }

                if (readOnly) {
                    // Read only: only send position keys.
                    if (keypress.equals(kbLeft)
                        || keypress.equals(kbRight)
                        || keypress.equals(kbHome)
                        || keypress.equals(kbEnd)
                    ) {
                        // Pass down to field.
                        field.onKeypress(keypress);
                    }
                    return;

                } else {
                    if (keypress.equals(kbEnter)) {
                        // Enter ends editing.

                        // Pass down to field first so that it can execute
                        // enterAction if specified.
                        super.onKeypress(keypress);

                        fieldText = field.getText();
                        isEditing = false;
                        field.setEnabled(false);
                        return;
                    }
                }
                // Pass down to field.
                super.onKeypress(keypress);
            }

            if (keypress.equals(kbEnter) || keypress.equals(kbF2)) {
                // Enter or F2 starts editing.
                fieldText = field.getText();
                isEditing = true;
                field.setEnabled(true);
                activate(field);
                return;
            }
        }

        // --------------------------------------------------------------------
        // TWidget ------------------------------------------------------------
        // --------------------------------------------------------------------

        /**
         * Draw this cell.
         */
        @Override
        public void draw() {
            TTable table = (TTable) getParent();

            if (isAbsoluteActive()) {
                if (isEditing) {
                    field.setActiveColorKey("tfield.active");
                    field.setInactiveColorKey("tfield.inactive");
                } else {
                    field.setActiveColorKey("ttable.selected");
                    field.setInactiveColorKey("ttable.selected");
                }
            } else if (((table.selectedColumn == column)
                    && ((table.selectedRow == row)
                        || (table.highlightColumn == true)))
                || ((table.selectedRow == row)
                    && ((table.selectedColumn == column)
                        || (table.highlightRow == true)))
            ) {
                field.setActiveColorKey("ttable.active");
                field.setInactiveColorKey("ttable.active");
            } else {
                field.setActiveColorKey("ttable.active");
                field.setInactiveColorKey("ttable.inactive");
            }

            assert (isVisible() == true);

            super.draw();
        }

        // --------------------------------------------------------------------
        // TTable.Cell --------------------------------------------------------
        // --------------------------------------------------------------------

        /**
         * Get field text.
         *
         * @return field text
         */
        public final String getText() {
            return field.getText();
        }

        /**
         * Set field text.
         *
         * @param text the new field text
         */
        public void setText(final String text) {
            field.setText(text);
        }

        /**
         * Cancel any pending edit.
         */
        public void cancelEdit() {
            // Cancel any pending edit.
            if (fieldText != null) {
                field.setText(fieldText);
            }
            isEditing = false;
            field.setEnabled(false);
        }

        /**
         * Set an entire column of cells read-only (non-editable) or not.
         *
         * @param readOnly if true, the cells will be non-editable
         */
        public void setReadOnly(final boolean readOnly) {
            cancelEdit();
            this.readOnly = readOnly;
        }

    }

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of widget
     * @param height height of widget
     * @param gridColumns number of columns in grid
     * @param gridRows number of rows in grid
     */
    @SuppressWarnings("this-escape")
    public TTable(final TWidget parent, final int x, final int y,
        final int width, final int height, final int gridColumns,
        final int gridRows) {

        super(parent, x, y, width, height);
        setGridSize(gridColumns, gridRows);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of widget
     * @param height height of widget
     */
    public TTable(final TWidget parent, final int x, final int y,
        final int width, final int height) {

        this(parent, x, y, width, height,
            width / (COLUMN_DEFAULT_WIDTH + 1) + EXTRA_COLUMNS,
            height + EXTRA_ROWS);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if (mouse.isMouseWheelUp() || mouse.isMouseWheelDown()) {
            // Treat wheel up/down as 3 up/down
            TKeypressEvent keyEvent;
            if (mouse.isMouseWheelUp()) {
                keyEvent = new TKeypressEvent(mouse.getBackend(), kbUp);
            } else {
                keyEvent = new TKeypressEvent(mouse.getBackend(), kbDown);
            }
            for (int i = 0; i < 3; i++) {
                onKeypress(keyEvent);
            }
            return;
        }

        // Use TWidget's code to pass the event to the children.
        super.onMouseDown(mouse);
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.equals(kbTab)
            || keypress.equals(kbShiftTab)
        ) {
            // Squash tab and back-tab.  They don't make sense in the TTable
            // grid context.
            return;
        }

        // If editing, pass to that cell and do nothing else.
        if (getSelectedCell().isEditing) {
            super.onKeypress(keypress);
            return;
        }

        if (keypress.equals(kbLeft)) {
            // Left
            if (selectedColumn > 0) {
                selectedColumn--;
            }
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbRight)) {
            // Right
            if (selectedColumn < columns.size() - 1) {
                selectedColumn++;
            }
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbUp)) {
            // Up
            if (selectedRow > 0) {
                selectedRow--;
            }
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbDown)) {
            // Down
            if (selectedRow < rows.size() - 1) {
                selectedRow++;
            }
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbHome)) {
            // Home - leftmost column
            selectedColumn = 0;
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbEnd)) {
            // End - rightmost column
            selectedColumn = columns.size() - 1;
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbPgUp)) {
            // PgUp - Treat like multiple up
            for (int i = 0; i < getHeight() - 2; i++) {
                if (selectedRow > 0) {
                    selectedRow--;
                }
            }
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbPgDn)) {
            // PgDn - Treat like multiple up
            for (int i = 0; i < getHeight() - 2; i++) {
                if (selectedRow < rows.size() - 1) {
                    selectedRow++;
                }
            }
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbCtrlHome)) {
            // Ctrl-Home - go to top-left
            selectedRow = 0;
            selectedColumn = 0;
            activate(columns.get(selectedColumn).get(selectedRow));
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbCtrlEnd)) {
            // Ctrl-End - go to bottom-right
            selectedRow = rows.size() - 1;
            selectedColumn = columns.size() - 1;
            activate(columns.get(selectedColumn).get(selectedRow));
            activate(columns.get(selectedColumn).get(selectedRow));
        } else {
            // Pass to the Cell.
            super.onKeypress(keypress);
        }

        // We may have scrolled off screen.  Reset positions as needed to
        // make the newly selected cell visible.
        alignGrid();
    }

    /**
     * Handle widget resize events.
     *
     * @param event resize event
     */
    @Override
    public void onResize(final TResizeEvent event) {
        super.onResize(event);

        bottomRightCorner();
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the table row/column labels, and borders.
     */
    @Override
    public void draw() {
        CellAttributes labelColor = getTheme().getColor("ttable.label");
        CellAttributes labelColorSelected = getTheme().getColor("ttable.label.selected");
        CellAttributes borderColor = getTheme().getColor("ttable.border");

        int myWidth = getWidth();

        // Column labels.
        if (showColumnLabels == true) {
            for (int i = left; i < columns.size(); i++) {
                if (columns.get(i).get(top).isVisible() == false) {
                    break;
                }
                putStringXY(columns.get(i).get(top).getX(), 0,
                    String.format(" %-" +
                        (columns.get(i).width - 2)
                        + "s ", columns.get(i).label),
                    (i == selectedColumn ? labelColorSelected : labelColor));
            }
        }

        // Row labels.
        if (showRowLabels == true) {
            for (int i = top; i < rows.size(); i++) {
                if (rows.get(i).get(left).isVisible() == false) {
                    break;
                }
                putStringXY(0, rows.get(i).get(left).getY(),
                    String.format(" %-" + (rowLabelWidth - 2) + "s ",
                        rows.get(i).label),
                    (i == selectedRow ? labelColorSelected : labelColor));
            }
        }

        // Draw vertical borders.
        if (leftBorder == Border.SINGLE) {
            vLineXY((showRowLabels ? rowLabelWidth : 0),
                (topBorder == Border.NONE ? 0 : 1) +
                    (showColumnLabels ? COLUMN_LABEL_HEIGHT : 0),
                getHeight(), '\u2502', borderColor);
        }
        for (int i = left; i < columns.size(); i++) {
            if (columns.get(i).get(top).isVisible() == false) {
                break;
            }
            if (columns.get(i).rightBorder == Border.SINGLE) {
                vLineXY(columns.get(i).getX() + columns.get(i).width,
                    (topBorder == Border.NONE ? 0 : 1) +
                        (showColumnLabels ? COLUMN_LABEL_HEIGHT : 0),
                    getHeight(), '\u2502', borderColor);
            }
        }

        // Draw horizontal borders.
        if (topBorder == Border.SINGLE) {
            hLineXY((showRowLabels ? rowLabelWidth : 0),
                (showColumnLabels ? COLUMN_LABEL_HEIGHT : 0),
                myWidth, '\u2500', borderColor);
        }
        for (int i = top; i < rows.size(); i++) {
            if (rows.get(i).get(left).isVisible() == false) {
                break;
            }
            if (rows.get(i).bottomBorder == Border.SINGLE) {
                hLineXY((leftBorder == Border.NONE ? 0 : 1) +
                        (showRowLabels ? rowLabelWidth : 0),
                    rows.get(i).getY() + rows.get(i).height - 1,
                    myWidth, '\u2500', borderColor);
            } else if (rows.get(i).bottomBorder == Border.DOUBLE) {
                hLineXY((leftBorder == Border.NONE ? 0 : 1) +
                        (showRowLabels ? rowLabelWidth : 0),
                    rows.get(i).getY() + rows.get(i).height - 1,
                    myWidth, '\u2550', borderColor);
            } else if (rows.get(i).bottomBorder == Border.THICK) {
                hLineXY((leftBorder == Border.NONE ? 0 : 1) +
                        (showRowLabels ? rowLabelWidth : 0),
                    rows.get(i).getY() + rows.get(i).height - 1,
                    myWidth, '\u2501', borderColor);
            }
        }
        // Top-left corner if needed
        if ((topBorder == Border.SINGLE) && (leftBorder == Border.SINGLE)) {
            putCharXY((showRowLabels ? rowLabelWidth : 0),
                (showColumnLabels ? COLUMN_LABEL_HEIGHT : 0),
                '\u250c', borderColor);
        }

        // Now draw the correct corners
        for (int i = top; i < rows.size(); i++) {
            if (rows.get(i).get(left).isVisible() == false) {
                break;
            }
            for (int j = left; j < columns.size(); j++) {
                if (columns.get(j).get(i).isVisible() == false) {
                    break;
                }
                if ((i == top) && (topBorder == Border.SINGLE)
                    && (columns.get(j).rightBorder == Border.SINGLE)
                ) {
                    // Top tee
                    putCharXY(columns.get(j).getX() + columns.get(j).width,
                        (showColumnLabels ? COLUMN_LABEL_HEIGHT : 0),
                        '\u252c', borderColor);
                }
                if ((j == left) && (leftBorder == Border.SINGLE)
                    && (rows.get(i).bottomBorder == Border.SINGLE)
                ) {
                    // Left tee
                    putCharXY((showRowLabels ? rowLabelWidth : 0),
                        rows.get(i).getY() + rows.get(i).height - 1,
                        '\u251c', borderColor);
                }
                if ((columns.get(j).rightBorder == Border.SINGLE)
                    && (rows.get(i).bottomBorder == Border.SINGLE)
                ) {
                    // Intersection of single bars
                    putCharXY(columns.get(j).getX() + columns.get(j).width,
                        rows.get(i).getY() + rows.get(i).height - 1,
                        '\u253c', borderColor);
                }
                if ((j == left) && (leftBorder == Border.SINGLE)
                    && (rows.get(i).bottomBorder == Border.DOUBLE)
                ) {
                    // Left tee: single bar vertical, double bar horizontal
                    putCharXY((showRowLabels ? rowLabelWidth : 0),
                        rows.get(i).getY() + rows.get(i).height - 1,
                        '\u255e', borderColor);
                }
                if ((j == left) && (leftBorder == Border.SINGLE)
                    && (rows.get(i).bottomBorder == Border.THICK)
                ) {
                    // Left tee: single bar vertical, thick bar horizontal
                    putCharXY((showRowLabels ? rowLabelWidth : 0),
                        rows.get(i).getY() + rows.get(i).height - 1,
                        '\u251d', borderColor);
                }
                if ((columns.get(j).rightBorder == Border.SINGLE)
                    && (rows.get(i).bottomBorder == Border.DOUBLE)
                ) {
                    // Intersection: single bar vertical, double bar
                    // horizontal
                    putCharXY(columns.get(j).getX() + columns.get(j).width,
                        rows.get(i).getY() + rows.get(i).height - 1,
                        '\u256a', borderColor);
                }
                if ((columns.get(j).rightBorder == Border.SINGLE)
                    && (rows.get(i).bottomBorder == Border.THICK)
                ) {
                    // Intersection: single bar vertical, thick bar
                    // horizontal
                    putCharXY(columns.get(j).getX() + columns.get(j).width,
                        rows.get(i).getY() + rows.get(i).height - 1,
                        '\u253f', borderColor);
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // TTable -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set the size of the table grid.  All cells are reset to blank.
     *
     * @param gridColumns number of columns in grid
     * @param gridRows number of rows in grid
     */
    public void setGridSize(final int gridColumns, final int gridRows) {

        /*
        System.err.println("gridColumns " + gridColumns +
            " gridRows " + gridRows);
         */

        if (gridColumns < 1) {
            throw new IllegalArgumentException("Column count cannot be less " +
                "than 1");
        }
        if (gridRows < 1) {
            throw new IllegalArgumentException("Row count cannot be less " +
                "than 1");
        }

        // Remove old TFields from the window.
        for (Row row: rows) {
            for (Cell cell: row.cells) {
                cell.cancelEdit();
                cell.setText("");
                cell.remove();
            }
        }

        // Now redo with new cells.
        columns = new ArrayList<Column>();
        rows = new ArrayList<Row>();
        top = 0;
        left = 0;
        selectedRow = 0;
        selectedColumn = 0;
        highlightRow = false;
        highlightColumn = false;

        // Initialize the starting row and column.
        rows.add(new Row(0));
        columns.add(new Column(0));
        assert (rows.get(0).height == 1);

        // Place a grid of cells that fit in this space.
        for (int row = 0; row < gridRows; row++) {
            for (int column = 0; column < gridColumns; column++) {
                Cell cell = new Cell(this, 0, 0, COLUMN_DEFAULT_WIDTH, 1,
                    column, row);

                if (DEBUG) {
                    // For debugging: set a grid of cell index labels.
                    cell.setText("" + row + " " + column);
                }
                rows.get(row).add(cell);
                columns.get(column).add(cell);

                if (columns.size() < gridColumns) {
                    columns.add(new Column(column + 1));
                }
            }
            if (row < gridRows - 1) {
                rows.add(new Row(row + 1));
            }
        }
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setY(i + (showColumnLabels ? COLUMN_LABEL_HEIGHT : 0));
        }
        for (int j = 0; j < columns.size(); j++) {
            columns.get(j).setX((j * (COLUMN_DEFAULT_WIDTH + 1)) +
                (showRowLabels ? rowLabelWidth : 0));
        }
        activate(columns.get(selectedColumn).get(selectedRow));

        alignGrid();
    }

    /**
     * Reset all cells to blanks.
     */
    public void clearAll() {
        for (Row row: rows) {
            for (Cell cell: row.cells) {
                cell.cancelEdit();
                cell.setText("");
            }
        }
    }

    /**
     * Generate the default letter name for a column number.
     *
     * @param col column number to use for this column.  Column 0 will be
     * "A", column 1 will be "B", column 26 will be "AA", and so on.
     */
    private String makeColumnLabel(int col) {
        StringBuilder sb = new StringBuilder();
        for (;;) {
            sb.append((char) ('A' + (col % 26)));
            if (col < 26) {
                break;
            }
            col /= 26;
        }
        return sb.reverse().toString();
    }

    /**
     * Get the currently-selected cell.
     *
     * @return the selected cell
     */
    public Cell getSelectedCell() {
        assert (rows.get(selectedRow) != null);
        assert (rows.get(selectedRow).get(selectedColumn) != null);
        assert (columns.get(selectedColumn) != null);
        assert (columns.get(selectedColumn).get(selectedRow) != null);
        assert (rows.get(selectedRow).get(selectedColumn) ==
            columns.get(selectedColumn).get(selectedRow));

        return (columns.get(selectedColumn).get(selectedRow));
    }

    /**
     * Get the currently-selected column.
     *
     * @return the selected column
     */
    public Column getSelectedColumn() {
        assert (selectedColumn >= 0);
        assert (columns.size() > selectedColumn);
        assert (columns.get(selectedColumn) != null);
        return columns.get(selectedColumn);
    }

    /**
     * Get the currently-selected row.
     *
     * @return the selected row
     */
    public Row getSelectedRow() {
        assert (selectedRow >= 0);
        assert (rows.size() > selectedRow);
        assert (rows.get(selectedRow) != null);
        return rows.get(selectedRow);
    }

    /**
     * Get the currently-selected column number.  0 is the left-most column.
     *
     * @return the selected column number
     */
    public int getSelectedColumnNumber() {
        return selectedColumn;
    }

    /**
     * Set the currently-selected column number.  0 is the left-most column.
     *
     * @param column the column number to select
     */
    public void setSelectedColumnNumber(final int column) {
        if ((column < 0) || (column > columns.size() - 1)) {
            throw new IndexOutOfBoundsException("Column count is " +
                columns.size() + ", requested index " + column);
        }
        selectedColumn = column;
        activate(columns.get(selectedColumn).get(selectedRow));
        alignGrid();
    }

    /**
     * Get the currently-selected row number.  0 is the top-most row.
     *
     * @return the selected row number
     */
    public int getSelectedRowNumber() {
        return selectedRow;
    }

    /**
     * Set the currently-selected row number.  0 is the left-most column.
     *
     * @param row the row number to select
     */
    public void setSelectedRowNumber(final int row) {
        if ((row < 0) || (row > rows.size() - 1)) {
            throw new IndexOutOfBoundsException("Row count is " +
                rows.size() + ", requested index " + row);
        }
        selectedRow = row;
        activate(columns.get(selectedColumn).get(selectedRow));
        alignGrid();
    }

    /**
     * Get the highlight row flag.
     *
     * @return true if the selected row is highlighted
     */
    public boolean getHighlightRow() {
        return highlightRow;
    }

    /**
     * Set the highlight row flag.
     *
     * @param highlightRow if true, the selected row will be highlighted
     */
    public void setHighlightRow(final boolean highlightRow) {
        this.highlightRow = highlightRow;
    }

    /**
     * Get the highlight column flag.
     *
     * @return true if the selected column is highlighted
     */
    public boolean getHighlightColumn() {
        return highlightColumn;
    }

    /**
     * Set the highlight column flag.
     *
     * @param highlightColumn if true, the selected column will be highlighted
     */
    public void setHighlightColumn(final boolean highlightColumn) {
        this.highlightColumn = highlightColumn;
    }

    /**
     * Get the show row labels flag.
     *
     * @return true if row labels are shown
     */
    public boolean getShowRowLabels() {
        return showRowLabels;
    }

    /**
     * Set the show row labels flag.
     *
     * @param showRowLabels if true, the row labels will be shown
     */
    public void setShowRowLabels(final boolean showRowLabels) {
        this.showRowLabels = showRowLabels;
    }

    /**
     * Get the show column labels flag.
     *
     * @return true if column labels are shown
     */
    public boolean getShowColumnLabels() {
        return showColumnLabels;
    }

    /**
     * Set the show column labels flag.
     *
     * @param showColumnLabels if true, the column labels will be shown
     */
    public void setShowColumnLabels(final boolean showColumnLabels) {
        this.showColumnLabels = showColumnLabels;
    }

    /**
     * Get the width of the column used to show the row labels.
     *
     * @return the row label width
     */
    public int getRowLabelWidth() {
        return rowLabelWidth;
    }

    /**
     * Set the width of the column used to show the row labels.
     *
     * @param rowLabelWidth the new row label width
     */
    public void setRowLabelWidth(final int rowLabelWidth) {
        this.rowLabelWidth = rowLabelWidth;
    }

    /**
     * Automatically set the width of the column used to show the row labels
     * to either the minimum required to display the data, or the maximum
     * supplied width, whichever is smaller.
     *
     * @param maxWidth the maximum width to size to
     */
    public void setRowLabelWidthAuto(final int maxWidth) {
        // Row label column may not be smaller than 4 cells wide.
        int newWidth = 4;

        for (int i = 0; i < rows.size(); i++) {
            newWidth = Math.max(StringUtils.width(getRowLabel(i)), newWidth);
        }
        newWidth = Math.min(newWidth + 2, maxWidth);
        setRowLabelWidth(newWidth);
    }

    /**
     * Get the number of columns.
     *
     * @return the number of columns
     */
    public int getColumnCount() {
        return columns.size();
    }

    /**
     * Get the number of rows.
     *
     * @return the number of rows
     */
    public int getRowCount() {
        return rows.size();
    }


    /**
     * Push top and left to the bottom-most right corner of the available
     * grid.
     */
    private void bottomRightCorner() {
        int viewColumns = getWidth();
        if (showRowLabels == true) {
            viewColumns -= rowLabelWidth;
        }

        // Set left and top such that the table stays on screen if possible.
        top = Math.min(rows.size() - 1,
            Math.max(0, rows.size() - getHeight()));
        int visibleColumnWidth = Math.max(1,
            (viewColumns / (COLUMN_DEFAULT_WIDTH + 1)));
        left = Math.min(columns.size() - 1,
            Math.max(0, columns.size() - (getWidth() / visibleColumnWidth)));
        // Now ensure the selection is visible.
        alignGrid();
    }

    /**
     * Align the grid so that the selected cell is fully visible.
     */
    private void alignGrid() {

        /*
        System.err.println("alignGrid() # columns " + columns.size() +
            " # rows " + rows.size());
         */

        int viewColumns = getWidth();
        if (showRowLabels == true) {
            viewColumns -= rowLabelWidth;
        }
        if (leftBorder != Border.NONE) {
            viewColumns--;
        }
        int viewRows = getHeight();
        if (showColumnLabels == true) {
            viewRows -= COLUMN_LABEL_HEIGHT;
        }
        if (topBorder != Border.NONE) {
            viewRows--;
        }

        // If we pushed left or right, adjust the box to include the new
        // selected cell.
        if (selectedColumn < left) {
            left = selectedColumn - 1;
        }
        if (left < 0) {
            left = 0;
        }
        if (selectedRow < top) {
            top = selectedRow - 1;
        }
        if (top < 0) {
            top = 0;
        }

        /*
         * viewColumns and viewRows now contain the available columns and
         * rows available to view the selected cell.  We adjust left and top
         * to ensure the selected cell is within view, and then make all
         * cells outside the box between (left, top) and (right, bottom)
         * invisible.
         *
         * We need to calculate right and bottom now.
         */
        int right = left;

        boolean done = false;
        while (!done) {
            int rightCellX = (showRowLabels ? rowLabelWidth : 0);
            if (leftBorder != Border.NONE) {
                rightCellX++;
            }
            int maxCellX = rightCellX + viewColumns;
            right = left;
            boolean selectedIsVisible = false;
            int selectedX = 0;
            for (int x = left; x < columns.size(); x++) {
                if (x == selectedColumn) {
                    selectedX = rightCellX;
                    if (selectedX + columns.get(x).width +
                        (x == columns.size() - 1 ? 0 : 1) <= maxCellX) {
                        selectedIsVisible = true;
                    }
                }
                rightCellX += columns.get(x).width + 1;
                if (rightCellX >= maxCellX) {
                    break;
                }
                right++;
            }
            if (right < selectedColumn) {
                // selectedColumn is outside the view range.  Push left over,
                // and calculate again.
                left++;
            } else if (left == selectedColumn) {
                // selectedColumn doesn't fit inside the view range, but we
                // can't go over any further either.  Bail out.
                done = true;
            } else if (selectedIsVisible == false) {
                // selectedColumn doesn't fit inside the view range, continue
                // on.
                left++;
            } else {
                // selectedColumn is fully visible, all done.
                assert (selectedIsVisible == true);
                done = true;
            }

        } // while (!done)

        // We have the left/right range correct, set cell visibility and
        // column X positions.
        int leftCellX = showRowLabels ? rowLabelWidth : 0;
        if (leftBorder != Border.NONE) {
            leftCellX++;
        }
        for (int x = 0; x < columns.size(); x++) {
            if ((x < left) || (x > right)) {
                for (int i = 0; i < rows.size(); i++) {
                    columns.get(x).get(i).setVisible(false);
                    // Just need a value guaranteed to be outside TTable's
                    // visible width.
                    columns.get(x).setX(getWidth() + rowLabelWidth + 2);
                }
                continue;
            }
            for (int i = 0; i < rows.size(); i++) {
                columns.get(x).get(i).setVisible(true);
            }
            columns.get(x).setX(leftCellX);
            leftCellX += columns.get(x).width + 1;
        }

        int bottom = top;

        done = false;
        while (!done) {
            int bottomCellY = (showColumnLabels ? COLUMN_LABEL_HEIGHT : 0);
            if (topBorder != Border.NONE) {
                bottomCellY++;
            }
            int maxCellY = bottomCellY + viewRows;
            bottom = top;
            for (int y = top; y < rows.size(); y++) {
                bottomCellY += rows.get(y).height;
                if (bottomCellY >= maxCellY) {
                    break;
                }
                bottom++;
            }
            if (bottom < selectedRow) {
                // selectedRow is outside the view range.  Push top down, and
                // calculate again.
                top++;
            } else {
                // selectedRow is inside the view range, done.
                done = true;
            }
        } // while (!done)

        // We have the top/bottom range correct, set cell visibility and
        // row Y positions.
        int topCellY = showColumnLabels ? COLUMN_LABEL_HEIGHT : 0;
        if (topBorder != Border.NONE) {
            topCellY++;
        }
        for (int y = 0; y < rows.size(); y++) {
            if ((y < top) || (y > bottom)) {
                for (int i = 0; i < columns.size(); i++) {
                    rows.get(y).get(i).setVisible(false);
                }
                rows.get(y).setY(getHeight() + 1);
                continue;
            }
            for (int i = 0; i < columns.size(); i++) {
                rows.get(y).get(i).setVisible(true);
            }
            rows.get(y).setY(topCellY);
            topCellY += rows.get(y).height;
        }

        // Last thing: cancel any edits that are not the selected cell.
        for (int y = 0; y < rows.size(); y++) {
            for (int x = 0; x < columns.size(); x++) {
                if ((x == selectedColumn) && (y == selectedRow)) {
                    continue;
                }
                rows.get(y).get(x).cancelEdit();
            }
        }
    }

    /**
     * Load contents from file in CSV format.
     *
     * @param csvFile a File referencing the CSV data
     * @throws IOException if a java.io operation throws
     */
    public void loadCsvFile(final File csvFile) throws IOException {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(csvFile));

            String line = null;
            boolean first = true;
            for (line = reader.readLine(); line != null;
                 line = reader.readLine()) {

                List<String> list = StringUtils.fromCsv(line);
                if (list.size() == 0) {
                    continue;
                }

                if (list.size() > columns.size()) {
                    int n = list.size() - columns.size();
                    for (int i = 0; i < n; i++) {
                        selectedColumn = columns.size() - 1;
                        insertColumnRight(selectedColumn);
                    }
                }
                assert (list.size() == columns.size());

                if (first) {
                    // First row: just replace what is here.
                    selectedRow = 0;
                    first = false;
                } else {
                    // All other rows: append to the end.
                    selectedRow = rows.size() - 1;
                    insertRowBelow(selectedRow);
                    selectedRow = rows.size() - 1;
                }
                for (int i = 0; i < list.size(); i++) {
                    rows.get(selectedRow).get(i).setText(list.get(i));
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        left = 0;
        top = 0;
        selectedRow = 0;
        selectedColumn = 0;
        for (int i = 0; i < columns.size(); i++) {
            setColumnWidthAuto(i, getWidth() / 2);
        }
        alignGrid();
        activate(columns.get(selectedColumn).get(selectedRow));
    }

    /**
     * Save contents to file in CSV format.
     *
     * @param filename file to save to
     * @throws IOException if a java.io operation throws
     */
    public void saveToCsvFilename(final String filename) throws IOException {
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(filename));

            List<String> list = new ArrayList<String>();
            if (showColumnLabels) {
                list.add("");
                for (int i = 0; i < columns.size(); i++) {
                    list.add(getColumnLabel(i));
                }
                writer.write(StringUtils.toCsv(list));
                writer.write("\n");
            }

            for (int i = 0; i < rows.size(); i++) {
                Row row = rows.get(i);
                list = new ArrayList<String>(row.cells.size());
                if (showRowLabels) {
                    list.add(getRowLabel(i));
                }
                for (Cell cell: row.cells) {
                    list.add(cell.getText());
                }
                writer.write(StringUtils.toCsv(list));
                writer.write("\n");
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Save contents to file in text format with lines.
     *
     * @param filename file to save to
     * @throws IOException if a java.io operation throws
     */
    public void saveToTextFilename(final String filename) throws IOException {
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(filename));

            if ((topBorder == Border.SINGLE) && (leftBorder == Border.SINGLE)) {
                // Emit top-left corner.
                writer.write("\u250c");
            }

            if (topBorder == Border.SINGLE) {
                int cellI = 0;
                for (Cell cell: rows.get(0).cells) {
                    for (int i = 0; i < columns.get(cellI).width; i++) {
                        writer.write("\u2500");
                    }

                    if (columns.get(cellI).rightBorder == Border.SINGLE) {
                        if (cellI < columns.size() - 1) {
                            // Emit top tee.
                            writer.write("\u252c");
                        } else {
                            // Emit top-right corner.
                            writer.write("\u2510");
                        }
                    }
                    cellI++;
                }
            }
            writer.write("\n");

            int rowI = 0;
            for (Row row: rows) {

                if (leftBorder == Border.SINGLE) {
                    // Emit left border.
                    writer.write("\u2502");
                }

                int cellI = 0;
                for (Cell cell: row.cells) {
                    writer.write(String.format("%" +
                            columns.get(cellI).width + "s", cell.getText()));

                    if (columns.get(cellI).rightBorder == Border.SINGLE) {
                        // Emit right border.
                        writer.write("\u2502");
                    }
                    cellI++;
                }
                writer.write("\n");

                if (row.bottomBorder == Border.NONE) {
                    // All done, move on to the next row.
                    continue;
                }

                // Emit the bottom borders and intersections.
                if ((leftBorder == Border.SINGLE)
                    && (row.bottomBorder != Border.NONE)
                ) {
                    if (rowI < rows.size() - 1) {
                        if (row.bottomBorder == Border.SINGLE) {
                            // Emit left tee.
                            writer.write("\u251c");
                        } else if (row.bottomBorder == Border.DOUBLE) {
                            // Emit left tee (double).
                            writer.write("\u255e");
                        } else if (row.bottomBorder == Border.THICK) {
                            // Emit left tee (thick).
                            writer.write("\u251d");
                        }
                    }

                    if (rowI == rows.size() - 1) {
                        if (row.bottomBorder == Border.SINGLE) {
                            // Emit left bottom corner.
                            writer.write("\u2514");
                        } else if (row.bottomBorder == Border.DOUBLE) {
                            // Emit left bottom corner (double).
                            writer.write("\u2558");
                        } else if (row.bottomBorder == Border.THICK) {
                            // Emit left bottom corner (thick).
                            writer.write("\u2515");
                        }
                    }
                }

                cellI = 0;
                for (Cell cell: row.cells) {

                    for (int i = 0; i < columns.get(cellI).width; i++) {
                        if (row.bottomBorder == Border.SINGLE) {
                            writer.write("\u2500");
                        }
                        if (row.bottomBorder == Border.DOUBLE) {
                            writer.write("\u2550");
                        }
                        if (row.bottomBorder == Border.THICK) {
                            writer.write("\u2501");
                        }
                    }

                    if ((rowI < rows.size() - 1)
                        && (cellI == columns.size() - 1)
                        && (row.bottomBorder == Border.SINGLE)
                        && (columns.get(cellI).rightBorder == Border.SINGLE)
                    ) {
                        // Emit right tee.
                        writer.write("\u2524");
                    }
                    if ((rowI < rows.size() - 1)
                        && (cellI == columns.size() - 1)
                        && (row.bottomBorder == Border.DOUBLE)
                        && (columns.get(cellI).rightBorder == Border.SINGLE)
                    ) {
                        // Emit right tee (double).
                        writer.write("\u2561");
                    }
                    if ((rowI < rows.size() - 1)
                        && (cellI == columns.size() - 1)
                        && (row.bottomBorder == Border.THICK)
                        && (columns.get(cellI).rightBorder == Border.SINGLE)
                    ) {
                        // Emit right tee (thick).
                        writer.write("\u2525");
                    }
                    if ((rowI == rows.size() - 1)
                        && (cellI == columns.size() - 1)
                        && (row.bottomBorder == Border.SINGLE)
                        && (columns.get(cellI).rightBorder == Border.SINGLE)
                    ) {
                        // Emit right bottom corner.
                        writer.write("\u2518");
                    }
                    if ((rowI == rows.size() - 1)
                        && (cellI == columns.size() - 1)
                        && (row.bottomBorder == Border.DOUBLE)
                        && (columns.get(cellI).rightBorder == Border.SINGLE)
                    ) {
                        // Emit right bottom corner (double).
                        writer.write("\u255b");
                    }
                    if ((rowI == rows.size() - 1)
                        && (cellI == columns.size() - 1)
                        && (row.bottomBorder == Border.THICK)
                        && (columns.get(cellI).rightBorder == Border.SINGLE)
                    ) {
                        // Emit right bottom corner (thick).
                        writer.write("\u2519");
                    }
                    if ((rowI < rows.size() - 1)
                        && (cellI < columns.size() - 1)
                        && (row.bottomBorder == Border.SINGLE)
                        && (columns.get(cellI).rightBorder == Border.SINGLE)
                    ) {
                        // Emit intersection.
                        writer.write("\u253c");
                    }
                    if ((rowI < rows.size() - 1)
                        && (cellI < columns.size() - 1)
                        && (row.bottomBorder == Border.DOUBLE)
                        && (columns.get(cellI).rightBorder == Border.SINGLE)
                    ) {
                        // Emit intersection (double).
                        writer.write("\u256a");
                    }
                    if ((rowI < rows.size() - 1)
                        && (cellI < columns.size() - 1)
                        && (row.bottomBorder == Border.THICK)
                        && (columns.get(cellI).rightBorder == Border.SINGLE)
                    ) {
                        // Emit intersection (thick).
                        writer.write("\u253f");
                    }
                    if ((rowI == rows.size() - 1)
                        && (cellI < columns.size() - 1)
                        && (row.bottomBorder == Border.SINGLE)
                        && (columns.get(cellI).rightBorder == Border.SINGLE)
                    ) {
                        // Emit bottom tee.
                        writer.write("\u2534");
                    }
                    if ((rowI == rows.size() - 1)
                        && (cellI < columns.size() - 1)
                        && (row.bottomBorder == Border.DOUBLE)
                        && (columns.get(cellI).rightBorder == Border.SINGLE)
                    ) {
                        // Emit bottom tee (double).
                        writer.write("\u2567");
                    }
                    if ((rowI == rows.size() - 1)
                        && (cellI < columns.size() - 1)
                        && (row.bottomBorder == Border.THICK)
                        && (columns.get(cellI).rightBorder == Border.SINGLE)
                    ) {
                        // Emit bottom tee (thick).
                        writer.write("\u2537");
                    }

                    cellI++;
                }

                writer.write("\n");
                rowI++;
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Set the selected cell location.
     *
     * @param column the selected cell location column
     * @param row the selected cell location row
     */
    public void setSelectedCell(final int column, final int row) {
        if ((column < 0) || (column > columns.size() - 1)) {
            throw new IndexOutOfBoundsException("Column count is " +
                columns.size() + ", requested index " + column);
        }
        if ((row < 0) || (row > rows.size() - 1)) {
            throw new IndexOutOfBoundsException("Row count is " +
                rows.size() + ", requested index " + row);
        }
        selectedColumn = column;
        selectedRow = row;
        alignGrid();
    }

    /**
     * Get a particular cell.
     *
     * @param column the cell column
     * @param row the cell row
     * @return the cell
     */
    public Cell getCell(final int column, final int row) {
        if ((column < 0) || (column > columns.size() - 1)) {
            throw new IndexOutOfBoundsException("Column count is " +
                columns.size() + ", requested index " + column);
        }
        if ((row < 0) || (row > rows.size() - 1)) {
            throw new IndexOutOfBoundsException("Row count is " +
                rows.size() + ", requested index " + row);
        }
        return rows.get(row).get(column);
    }

    /**
     * Get the text of a particular cell.
     *
     * @param column the cell column
     * @param row the cell row
     * @return the text in the cell
     */
    public String getCellText(final int column, final int row) {
        if ((column < 0) || (column > columns.size() - 1)) {
            throw new IndexOutOfBoundsException("Column count is " +
                columns.size() + ", requested index " + column);
        }
        if ((row < 0) || (row > rows.size() - 1)) {
            throw new IndexOutOfBoundsException("Row count is " +
                rows.size() + ", requested index " + row);
        }
        return rows.get(row).get(column).getText();
    }

    /**
     * Set the text of a particular cell.
     *
     * @param column the cell column
     * @param row the cell row
     * @param text the text to put into the cell
     */
    public void setCellText(final int column, final int row,
        final String text) {

        if ((column < 0) || (column > columns.size() - 1)) {
            throw new IndexOutOfBoundsException("Column count is " +
                columns.size() + ", requested index " + column);
        }
        if ((row < 0) || (row > rows.size() - 1)) {
            throw new IndexOutOfBoundsException("Row count is " +
                rows.size() + ", requested index " + row);
        }
        rows.get(row).get(column).setText(text);
    }

    /**
     * Set the action to perform when the user presses enter on a particular
     * cell.
     *
     * @param column the cell column
     * @param row the cell row
     * @param action the action to perform when the user presses enter on the
     * cell
     */
    public void setCellEnterAction(final int column, final int row,
        final TAction action) {

        if ((column < 0) || (column > columns.size() - 1)) {
            throw new IndexOutOfBoundsException("Column count is " +
                columns.size() + ", requested index " + column);
        }
        if ((row < 0) || (row > rows.size() - 1)) {
            throw new IndexOutOfBoundsException("Row count is " +
                rows.size() + ", requested index " + row);
        }
        rows.get(row).get(column).field.setEnterAction(action);
    }

    /**
     * Set the action to perform when the user updates a particular cell.
     *
     * @param column the cell column
     * @param row the cell row
     * @param action the action to perform when the user updates the cell
     */
    public void setCellUpdateAction(final int column, final int row,
        final TAction action) {

        if ((column < 0) || (column > columns.size() - 1)) {
            throw new IndexOutOfBoundsException("Column count is " +
                columns.size() + ", requested index " + column);
        }
        if ((row < 0) || (row > rows.size() - 1)) {
            throw new IndexOutOfBoundsException("Row count is " +
                rows.size() + ", requested index " + row);
        }
        rows.get(row).get(column).field.setUpdateAction(action);
    }

    /**
     * Get the width of a column.
     *
     * @param column the column number
     * @return the width of the column
     */
    public int getColumnWidth(final int column) {
        if ((column < 0) || (column > columns.size() - 1)) {
            throw new IndexOutOfBoundsException("Column count is " +
                columns.size() + ", requested index " + column);
        }
        return columns.get(column).width;
    }

    /**
     * Set the width of a column.
     *
     * @param column the column number
     * @param width the new width of the column
     */
    public void setColumnWidth(final int column, final int width) {
        if ((column < 0) || (column > columns.size() - 1)) {
            throw new IndexOutOfBoundsException("Column count is " +
                columns.size() + ", requested index " + column);
        }

        // Columns may not be smaller than 4 cells wide.
        int newWidth = Math.max(4, width);

        columns.get(column).width = newWidth;
        for (Cell cell: columns.get(column).cells) {
            cell.setWidth(columns.get(column).width);
            cell.field.setWidth(columns.get(column).width);
        }

        int leftX = (leftBorder == Border.NONE ? 0 : 1);
        leftX += (showRowLabels ? rowLabelWidth : 0);
        for (int i = 0; i < columns.size(); i++) {
            columns.get(i).setX(leftX);
            leftX += columns.get(i).width + 1;
        }
        alignGrid();
    }

    /**
     * Automatically set the width of a column to either the minimum required
     * to display the data, or the maximum supplied width, whichever is
     * smaller.
     *
     * @param column the column number
     * @param maxWidth the maximum width to size to
     */
    public void setColumnWidthAuto(final int column, final int maxWidth) {
        // Columns may not be smaller than 4 cells wide.
        int newWidth = 4;

        for (Cell cell: columns.get(column).cells) {
            newWidth = Math.max(StringUtils.width(cell.getText()), newWidth);
        }
        if (showColumnLabels) {
            newWidth = Math.max(StringUtils.width(getColumnLabel(column)),
                newWidth);
        }
        newWidth = Math.min(newWidth + 2, maxWidth);
        setColumnWidth(column, newWidth);
    }

    /**
     * Get the label of a column.
     *
     * @param column the column number
     * @return the label of the column
     */
    public String getColumnLabel(final int column) {
        if ((column < 0) || (column > columns.size() - 1)) {
            throw new IndexOutOfBoundsException("Column count is " +
                columns.size() + ", requested index " + column);
        }
        return columns.get(column).label;
    }

    /**
     * Set the label of a column.
     *
     * @param column the column number
     * @param label the new label of the column
     */
    public void setColumnLabel(final int column, final String label) {
        if ((column < 0) || (column > columns.size() - 1)) {
            throw new IndexOutOfBoundsException("Column count is " +
                columns.size() + ", requested index " + column);
        }
        columns.get(column).label = label;
    }

    /**
     * Get the label of a row.
     *
     * @param row the row number
     * @return the label of the row
     */
    public String getRowLabel(final int row) {
        if ((row < 0) || (row > rows.size() - 1)) {
            throw new IndexOutOfBoundsException("Row count is " +
                rows.size() + ", requested index " + row);
        }
        return rows.get(row).label;
    }

    /**
     * Set the label of a row.
     *
     * @param row the row number
     * @param label the new label of the row
     */
    public void setRowLabel(final int row, final String label) {
        if ((row < 0) || (row > rows.size() - 1)) {
            throw new IndexOutOfBoundsException("Row count is " +
                rows.size() + ", requested index " + row);
        }
        rows.get(row).label = label;
        rowLabelWidth = Math.max(StringUtils.width(label) + 2, rowLabelWidth);
    }

    /**
     * Insert one row at a particular index.
     *
     * @param idx the row number
     */
    private void insertRowAt(final int idx) {
        Row newRow = new Row(idx);
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = new Cell(this, columns.get(i).getX(),
                rows.get(idx).getY(), COLUMN_DEFAULT_WIDTH, 1, i, idx);
            newRow.add(cell);
            columns.get(i).cells.add(idx, cell);
        }
        rows.add(idx, newRow);

        for (int x = 0; x < columns.size(); x++) {
            for (int y = idx; y < rows.size(); y++) {
                columns.get(x).get(y).row = y;
                columns.get(x).get(y).column = x;
            }
        }
        for (int i = idx + 1; i < rows.size(); i++) {
            String oldRowLabel = Integer.toString(i - 1);
            if (rows.get(i).label.equals(oldRowLabel)) {
                rows.get(i).label = Integer.toString(i);
            }
        }
        alignGrid();
    }

    /**
     * Insert one row above a particular row.
     *
     * @param row the row number
     */
    public void insertRowAbove(final int row) {
        if ((row < 0) || (row > rows.size() - 1)) {
            throw new IndexOutOfBoundsException("Row count is " +
                rows.size() + ", requested index " + row);
        }
        insertRowAt(row);
        selectedRow++;
        activate(columns.get(selectedColumn).get(selectedRow));
    }

    /**
     * Insert one row below a particular row.
     *
     * @param row the row number
     */
    public void insertRowBelow(final int row) {
        if ((row < 0) || (row > rows.size() - 1)) {
            throw new IndexOutOfBoundsException("Row count is " +
                rows.size() + ", requested index " + row);
        }
        int idx = row + 1;
        if (idx < rows.size()) {
            insertRowAt(idx);
            activate(columns.get(selectedColumn).get(selectedRow));
            return;
        }

        // row is the last row, we need to perform an append.
        Row newRow = new Row(idx);
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = new Cell(this, columns.get(i).getX(),
                rows.get(row).getY(), COLUMN_DEFAULT_WIDTH, 1, i, idx);
            newRow.add(cell);
            columns.get(i).cells.add(cell);
        }
        rows.add(newRow);
        alignGrid();
        activate(columns.get(selectedColumn).get(selectedRow));
    }

    /**
     * Delete a particular row.
     *
     * @param row the row number
     */
    public void deleteRow(final int row) {
        if ((row < 0) || (row > rows.size() - 1)) {
            throw new IndexOutOfBoundsException("Row count is " +
                rows.size() + ", requested index " + row);
        }
        if (rows.size() == 1) {
            // Don't delete the last row.
            return;
        }
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = columns.get(i).cells.remove(row);
            getChildren().remove(cell);
        }
        rows.remove(row);

        for (int x = 0; x < columns.size(); x++) {
            for (int y = row; y < rows.size(); y++) {
                columns.get(x).get(y).row = y;
                columns.get(x).get(y).column = x;
            }
        }
        for (int i = row; i < rows.size(); i++) {
            String oldRowLabel = Integer.toString(i + 1);
            if (rows.get(i).label.equals(oldRowLabel)) {
                rows.get(i).label = Integer.toString(i);
            }
        }
        if (selectedRow == rows.size()) {
            selectedRow--;
        }
        activate(columns.get(selectedColumn).get(selectedRow));
        bottomRightCorner();
    }

    /**
     * Insert one column at a particular index.
     *
     * @param idx the column number
     */
    private void insertColumnAt(final int idx) {
        Column newColumn = new Column(idx);
        for (int i = 0; i < rows.size(); i++) {
            Cell cell = new Cell(this, columns.get(idx).getX(),
                rows.get(i).getY(), COLUMN_DEFAULT_WIDTH, 1, idx, i);
            newColumn.add(cell);
            rows.get(i).cells.add(idx, cell);
        }
        columns.add(idx, newColumn);

        for (int x = idx; x < columns.size(); x++) {
            for (int y = 0; y < rows.size(); y++) {
                columns.get(x).get(y).row = y;
                columns.get(x).get(y).column = x;
            }
        }
        for (int i = idx + 1; i < columns.size(); i++) {
            String oldColumnLabel = makeColumnLabel(i - 1);
            if (columns.get(i).label.equals(oldColumnLabel)) {
                columns.get(i).label = makeColumnLabel(i);
            }
        }
        alignGrid();
    }

    /**
     * Insert one column to the left of a particular column.
     *
     * @param column the column number
     */
    public void insertColumnLeft(final int column) {
        if ((column < 0) || (column > columns.size() - 1)) {
            throw new IndexOutOfBoundsException("Column count is " +
                columns.size() + ", requested index " + column);
        }
        insertColumnAt(column);
        selectedColumn++;
        activate(columns.get(selectedColumn).get(selectedRow));
    }

    /**
     * Insert one column to the right of a particular column.
     *
     * @param column the column number
     */
    public void insertColumnRight(final int column) {
        if ((column < 0) || (column > columns.size() - 1)) {
            throw new IndexOutOfBoundsException("Column count is " +
                columns.size() + ", requested index " + column);
        }
        int idx = column + 1;
        if (idx < columns.size()) {
            insertColumnAt(idx);
            activate(columns.get(selectedColumn).get(selectedRow));
            return;
        }

        // column is the last column, we need to perform an append.
        Column newColumn = new Column(idx);
        for (int i = 0; i < rows.size(); i++) {
            Cell cell = new Cell(this, columns.get(column).getX(),
                rows.get(i).getY(), COLUMN_DEFAULT_WIDTH, 1, idx, i);
            newColumn.add(cell);
            rows.get(i).cells.add(cell);
        }
        columns.add(newColumn);
        alignGrid();
        activate(columns.get(selectedColumn).get(selectedRow));
    }

    /**
     * Delete a particular column.
     *
     * @param column the column number
     */
    public void deleteColumn(final int column) {
        if ((column < 0) || (column > columns.size() - 1)) {
            throw new IndexOutOfBoundsException("Column count is " +
                columns.size() + ", requested index " + column);
        }
        if (columns.size() == 1) {
            // Don't delete the last column.
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            Cell cell = rows.get(i).cells.remove(column);
            getChildren().remove(cell);
        }
        columns.remove(column);

        for (int x = column; x < columns.size(); x++) {
            for (int y = 0; y < rows.size(); y++) {
                columns.get(x).get(y).row = y;
                columns.get(x).get(y).column = x;
            }
        }
        for (int i = column; i < columns.size(); i++) {
            String oldColumnLabel = makeColumnLabel(i + 1);
            if (columns.get(i).label.equals(oldColumnLabel)) {
                columns.get(i).label = makeColumnLabel(i);
            }
        }
        if (selectedColumn == columns.size()) {
            selectedColumn--;
        }
        activate(columns.get(selectedColumn).get(selectedRow));
        bottomRightCorner();
    }

    /**
     * Delete the selected cell, shifting cells over to the left.
     */
    public void deleteCellShiftLeft() {
        // All we do is copy the text from every cell in this row over.
        for (int i = selectedColumn + 1; i < columns.size(); i++) {
            setCellText(i - 1, selectedRow, getCellText(i, selectedRow));
        }
        setCellText(columns.size() - 1, selectedRow, "");
    }

    /**
     * Delete the selected cell, shifting cells from below up.
     */
    public void deleteCellShiftUp() {
        // All we do is copy the text from every cell in this column up.
        for (int i = selectedRow + 1; i < rows.size(); i++) {
            setCellText(selectedColumn, i - 1, getCellText(selectedColumn, i));
        }
        setCellText(selectedColumn, rows.size() - 1, "");
    }

    /**
     * Set a particular cell read-only (non-editable) or not.
     *
     * @param column the cell column
     * @param row the cell row
     * @param readOnly if true, the cell will be non-editable
     */
    public void setCellReadOnly(final int column, final int row,
        final boolean readOnly) {

        if ((column < 0) || (column > columns.size() - 1)) {
            throw new IndexOutOfBoundsException("Column count is " +
                columns.size() + ", requested index " + column);
        }
        if ((row < 0) || (row > rows.size() - 1)) {
            throw new IndexOutOfBoundsException("Row count is " +
                rows.size() + ", requested index " + row);
        }
        rows.get(row).get(column).setReadOnly(readOnly);
    }

    /**
     * Set an entire row of cells read-only (non-editable) or not.
     *
     * @param row the row number
     * @param readOnly if true, the cells will be non-editable
     */
    public void setRowReadOnly(final int row, final boolean readOnly) {
        if ((row < 0) || (row > rows.size() - 1)) {
            throw new IndexOutOfBoundsException("Row count is " +
                rows.size() + ", requested index " + row);
        }
        for (Cell cell: rows.get(row).cells) {
            cell.setReadOnly(readOnly);
        }
    }

    /**
     * Set an entire column of cells read-only (non-editable) or not.
     *
     * @param column the column number
     * @param readOnly if true, the cells will be non-editable
     */
    public void setColumnReadOnly(final int column, final boolean readOnly) {
        if ((column < 0) || (column > columns.size() - 1)) {
            throw new IndexOutOfBoundsException("Column count is " +
                columns.size() + ", requested index " + column);
        }
        for (Cell cell: columns.get(column).cells) {
            cell.setReadOnly(readOnly);
        }
    }

    /**
     * Set all borders across the entire table to Border.NONE.
     */
    public void setBorderAllNone() {
        topBorder = Border.NONE;
        leftBorder = Border.NONE;
        for (int i = 0; i < columns.size(); i++) {
            columns.get(i).rightBorder = Border.NONE;
        }
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).bottomBorder = Border.NONE;
            rows.get(i).height = 1;
        }
        bottomRightCorner();
    }

    /**
     * Set all borders across the entire table to Border.SINGLE.
     */
    public void setBorderAllSingle() {
        topBorder = Border.SINGLE;
        leftBorder = Border.SINGLE;
        for (int i = 0; i < columns.size(); i++) {
            columns.get(i).rightBorder = Border.SINGLE;
        }
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).bottomBorder = Border.SINGLE;
            rows.get(i).height = 2;
        }
        alignGrid();
    }

    /**
     * Set all borders around the selected cell to Border.NONE.
     */
    public void setBorderCellNone() {
        if (selectedRow == 0) {
            topBorder = Border.NONE;
        }
        if (selectedColumn == 0) {
            leftBorder = Border.NONE;
        }
        if (selectedColumn > 0) {
            columns.get(selectedColumn - 1).rightBorder = Border.NONE;
        }
        columns.get(selectedColumn).rightBorder = Border.NONE;
        if (selectedRow > 0) {
            rows.get(selectedRow - 1).bottomBorder = Border.NONE;
            rows.get(selectedRow - 1).height = 1;
        }
        rows.get(selectedRow).bottomBorder = Border.NONE;
        rows.get(selectedRow).height = 1;
        bottomRightCorner();
    }

    /**
     * Set all borders around the selected cell to Border.SINGLE.
     */
    public void setBorderCellSingle() {
        if (selectedRow == 0) {
            topBorder = Border.SINGLE;
        }
        if (selectedColumn == 0) {
            leftBorder = Border.SINGLE;
        }
        if (selectedColumn > 0) {
            columns.get(selectedColumn - 1).rightBorder = Border.SINGLE;
        }
        columns.get(selectedColumn).rightBorder = Border.SINGLE;
        if (selectedRow > 0) {
            rows.get(selectedRow - 1).bottomBorder = Border.SINGLE;
            rows.get(selectedRow - 1).height = 2;
        }
        rows.get(selectedRow).bottomBorder = Border.SINGLE;
        rows.get(selectedRow).height = 2;
        alignGrid();
    }

    /**
     * Set the column border to the right of the selected cell to
     * Border.SINGLE.
     */
    public void setBorderColumnRightSingle() {
        columns.get(selectedColumn).rightBorder = Border.SINGLE;
        alignGrid();
    }

    /**
     * Set the column border to the right of the selected cell to
     * Border.SINGLE.
     */
    public void setBorderColumnLeftSingle() {
        if (selectedColumn == 0) {
            leftBorder = Border.SINGLE;
        } else {
            columns.get(selectedColumn - 1).rightBorder = Border.SINGLE;
        }
        alignGrid();
    }

    /**
     * Set the row border above the selected cell to Border.SINGLE.
     */
    public void setBorderRowAboveSingle() {
        if (selectedRow == 0) {
            topBorder = Border.SINGLE;
        } else {
            rows.get(selectedRow - 1).bottomBorder = Border.SINGLE;
            rows.get(selectedRow - 1).height = 2;
        }
        alignGrid();
    }

    /**
     * Set the row border below the selected cell to Border.SINGLE.
     */
    public void setBorderRowBelowSingle() {
        rows.get(selectedRow).bottomBorder = Border.SINGLE;
        rows.get(selectedRow).height = 2;
        alignGrid();
    }

    /**
     * Set the row border below the selected cell to Border.DOUBLE.
     */
    public void setBorderRowBelowDouble() {
        rows.get(selectedRow).bottomBorder = Border.DOUBLE;
        rows.get(selectedRow).height = 2;
        alignGrid();
    }

    /**
     * Set the row border below the selected cell to Border.THICK.
     */
    public void setBorderRowBelowThick() {
        rows.get(selectedRow).bottomBorder = Border.THICK;
        rows.get(selectedRow).height = 2;
        alignGrid();
    }

}
