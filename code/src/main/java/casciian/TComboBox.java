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

import java.util.List;

import casciian.bits.CellAttributes;
import casciian.bits.ColorTheme;
import casciian.bits.GraphicsChars;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import static casciian.TKeypress.*;

/**
 * TComboBox implements a combobox containing a drop-down list and edit
 * field.  Alt-Down can be used to show the drop-down.
 */
public class TComboBox extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The list of items in the drop-down.
     */
    private final TList list;

    /**
     * The edit field containing the value to return.
     */
    private final TField field;

    /**
     * If true, the field cannot be updated to a value not on the list.
     */
    private final boolean limitToListValue;

    /**
     * The maximum height of the values drop-down when it is visible.
     */
    private final int maxValuesHeight;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width visible combobox width, including the down-arrow
     * @param values the possible values for the box, shown in the drop-down
     * @param valuesIndex the initial index in values, or -1 for no default
     * value
     * @param maxValuesHeight the maximum height of the values drop-down when
     * it is visible
     * @param limitToListValue if true, the field cannot be updated to a value
     * not on the list
     * @param updateAction action to call when a new value is selected from
     * the list or enter is pressed in the edit field
     */
    @SuppressWarnings("this-escape")
    public TComboBox(final TWidget parent, final int x, final int y,
        final int width, final List<String> values, final int valuesIndex,
        final int maxValuesHeight, final boolean limitToListValue,
        final TAction updateAction) {

        // Set parent and window
        super(parent, x, y, width, 1);

        if (values == null) {
            throw new IllegalArgumentException("values cannot be null");
        }

        this.maxValuesHeight = maxValuesHeight;
        this.limitToListValue = limitToListValue;

        field = addField(0, 0, width - 3, false, "", updateAction, null);
        if ((valuesIndex >= 0) && (valuesIndex < values.size())) {
            field.setText(values.get(valuesIndex));
        }

        list = addList(values, 0, 1, width,
            getListHeight(values.size()),
            new TAction() {
                public void DO() {
                    field.setText(list.getSelected());
                    list.setEnabled(false);
                    list.setVisible(false);
                    TComboBox.super.setHeight(1);
                    if (!TComboBox.this.limitToListValue) {
                        TComboBox.this.activate(field);
                    }
                    if (updateAction != null) {
                        updateAction.DO(TComboBox.this);
                    }
                }
            }
        );
        if (valuesIndex >= 0) {
            list.setSelectedIndex(valuesIndex);
        }

        list.setEnabled(false);
        list.setVisible(false);
        super.setHeight(1);
        if (limitToListValue) {
            field.setEnabled(false);
        } else {
            activate(field);
        }
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width visible combobox width, including the down-arrow
     * @param values the possible values for the box, shown in the drop-down
     * @param valuesIndex the initial index in values, or -1 for no default
     * value
     * @param maxValuesHeight the maximum height of the values drop-down when
     * it is visible
     * @param updateAction action to call when a new value is selected from
     * the list or enter is pressed in the edit field
     */
    public TComboBox(final TWidget parent, final int x, final int y,
        final int width, final List<String> values, final int valuesIndex,
        final int maxValuesHeight, final TAction updateAction) {

        this(parent, x, y, width, values, valuesIndex, maxValuesHeight, true,
            updateAction);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Returns true if the mouse is currently on the down arrow.
     *
     * @param mouse mouse event
     * @return true if the mouse is currently on the down arrow
     */
    private boolean mouseOnArrow(final TMouseEvent mouse) {
        return (mouse.getY() == 0)
            && (mouse.getX() >= getWidth() - 3)
            && (mouse.getX() <= getWidth() - 1);
    }

    /**
     * Handle mouse down clicks.
     *
     * @param mouse mouse button down event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if ((mouseOnArrow(mouse)) && (mouse.isMouse1())) {
            // Make the list visible or not.
            if (list.isActive()) {
                hideList();
            } else {
                showList();
            }
        }

        // Pass to parent for the things we don't care about.
        super.onMouseDown(mouse);
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.matchesKey(kbEsc)) {
            if (list.isActive()) {
                hideList();
                return;
            }
        }

        if (keypress.matchesKey(kbAltDown)) {
            showList();
            return;
        }

        if (keypress.matchesKey(kbTab)
            || (keypress.matchesKey(kbShiftTab))
            || (keypress.matchesKey(kbBackTab))
        ) {
            if (list.isActive()) {
                hideList();
                return;
            }
        }

        // Pass to parent for the things we don't care about.
        super.onKeypress(keypress);
    }

    /**
     * A combobox with an open drop-down list uses Enter to select the
     * highlighted value, so it must keep the keypress instead of activating the
     * window default button.  (When the edit field has focus, the field's own
     * enter action, if any, already takes precedence.)
     *
     * @param keypress keystroke event
     * @return true if this widget should handle the keypress first
     */
    @Override
    protected boolean receivesKeypressBeforeWindowDefaultButton(
        final TKeypressEvent keypress) {

        return keypress.matchesKey(kbEnter) && list.isActive();
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Override TWidget's width: we need to set child widget widths.
     *
     * @param width new widget width
     */
    @Override
    public void setWidth(final int width) {
        if (field != null) {
            field.setWidth(width - 3);
        }
        if (list != null) {
            list.setWidth(width);
        }
        super.setWidth(width);
    }

    /**
     * Override TWidget's height: we can only set height at construction
     * time.
     *
     * @param height new widget height (ignored)
     */
    @Override
    public void setHeight(final int height) {
        // Do nothing
    }

    /**
     * Draw the combobox down arrow.
     */
    @Override
    public void draw() {
        CellAttributes comboBoxColor;

        if (!isAbsoluteActive()) {
            // We lost focus, turn off the list.
            if (list.isActive()) {
                hideList();
            }
        }

        if (isAbsoluteActive()) {
            comboBoxColor = getWidgetColor(ColorTheme.TCOMBOBOX_ACTIVE);
        } else {
            comboBoxColor = getWidgetColor(ColorTheme.TCOMBOBOX_INACTIVE);
        }

        var borderColor = CellAttributes.builder()
            .foreColor(getWidgetColor(ColorTheme.TWINDOW_BACKGROUND).getBackColor())
            .backColor(comboBoxColor.getBackColor())
            .build();

        putCharXY(getWidth() - 3, 0, GraphicsChars.DOWNARROWLEFT,
            borderColor);
        putCharXY(getWidth() - 2, 0, GraphicsChars.DOWNARROW,
            comboBoxColor);
        putCharXY(getWidth() - 1, 0, GraphicsChars.DOWNARROWRIGHT,
            borderColor);
    }

    // ------------------------------------------------------------------------
    // TComboBox --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Calculate the drop-down list height for the given number of items.
     *
     * @param itemCount number of items in the drop-down list
     * @return the drop-down list height
     */
    @SuppressWarnings("MathClampMigration")
    private int getListHeight(final int itemCount) {
        return Math.max(3, Math.min(itemCount + 1, maxValuesHeight));
    }

    /**
     * Hide the drop-down list.
     */
    public void hideList() {
        list.setEnabled(false);
        list.setVisible(false);
        super.setHeight(1);
        if (!limitToListValue) {
            activate(field);
        }
    }

    /**
     * Show the drop-down list.
     */
    public void showList() {
        list.setEnabled(true);
        list.setVisible(true);
        super.setHeight(list.getHeight() + 1);
        activate(list);
    }

    /**
     * Get combobox text value.
     *
     * @return text in the edit field
     */
    public String getText() {
        return field.getText();
    }

    /**
     * Set combobox text value.
     *
     * @param text the new text in the edit field
     */
    public void setText(final String text) {
        setText(text, true);
    }

    /**
     * Set combobox text value.
     *
     * @param text the new text in the edit field
     * @param caseSensitive if true, perform a case-sensitive search for the
     * list item
     */
    public void setText(final String text, final boolean caseSensitive) {
        field.setText(text);
        for (int i = 0; i <= list.getMaxSelectedIndex(); i++) {
            String item = list.getListItem(i);
            if (caseSensitive) {
                if (item.equals(text)) {
                    list.setSelectedIndex(i);
                    return;
                }
            } else {
                if (item.equalsIgnoreCase(text)) {
                    list.setSelectedIndex(i);
                    return;
                }
            }
        }
        list.setSelectedIndex(-1);
    }

    /**
     * Set combobox text to one of the list values.
     *
     * @param index the index in the list
     */
    public void setIndex(final int index) {
        list.setSelectedIndex(index);
        String value = list.getSelected();
        field.setText(value == null ? "" : value);
    }

    /**
     * Get a copy of the list of strings to display.
     *
     * @return the list of strings
     */
    public final List<String> getList() {
        return list.getList();
    }

    /**
     * Set the new list of strings to display.
     *
     * @param list new list of strings
     */
    public final void setList(final List<String> list) {
        this.list.setList(list);
        this.list.setHeight(getListHeight(list.size()));
        field.setText("");
    }
}
