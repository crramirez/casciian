/*
 * Casciian - Java Text User Interface
 *
 * Original work written 2013–2025 by Autumn Lamonte
 * and dedicated to the public domain via CC0.
 *
 * Modifications and maintenance:
 * Copyright 2025 Carlos Rafael Ramirez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package casciian;

import java.util.ArrayList;
import java.util.List;

import casciian.bits.CellAttributes;
import casciian.bits.ControlPadding;
import casciian.bits.StringUtils;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import casciian.event.TResizeEvent;
import static casciian.TKeypress.*;

/**
 * TList shows a list of strings, and lets the user select one.
 */
public class TList extends TScrollable {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The list of strings to display.
     */
    private final List<String> strings;

    /**
     * Selected string.
     */
    private int selectedString = -1;

    /**
     * The action to perform when the user selects an item (double-clicks or
     * enter).
     */
    protected TAction enterAction;

    /**
     * The action to perform when the user selects an item (single-click).
     */
    protected TAction singleClickAction;

    /**
     * The action to perform when the user navigates with keyboard.
     */
    protected TAction moveAction;

    /**
     * Extra left/right padding applied to each list row.  The value is
     * resolved once at construction from the active
     * {@link ControlPadding} style (system property
     * {@code casciian.controls.padding}).  The row text is drawn offset
     * by this amount from the left edge of the widget, and 1 blank cell
     * is reserved on the right (before the vertical scrollbar) as well.
     */
    protected int padding;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param strings list of strings to show
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     */
    public TList(final TWidget parent, final List<String> strings, final int x,
        final int y, final int width, final int height) {

        this(parent, strings, x, y, width, height, null, null, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param strings list of strings to show.  This is allowed to be null
     * and set later with setList() or by subclasses.
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     * @param enterAction action to perform when an item is selected
     */
    public TList(final TWidget parent, final List<String> strings, final int x,
        final int y, final int width, final int height,
        final TAction enterAction) {

        this(parent, strings, x, y, width, height, enterAction, null, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param strings list of strings to show.  This is allowed to be null
     * and set later with setList() or by subclasses.
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     * @param enterAction action to perform when an item is selected
     * @param moveAction action to perform when the user navigates to a new
     * item with arrow/page keys
     */
    public TList(final TWidget parent, final List<String> strings, final int x,
        final int y, final int width, final int height,
        final TAction enterAction, final TAction moveAction) {

        this(parent, strings, x, y, width, height, enterAction, moveAction,
            null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param strings list of strings to show.  This is allowed to be null
     * and set later with setList() or by subclasses.
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     * @param enterAction action to perform when an item is selected
     * @param moveAction action to perform when the user navigates to a new
     * item with arrow/page keys
     * @param singleClickAction action to perform when the user clicks on an
     * item
     */
    @SuppressWarnings("this-escape")
    public TList(final TWidget parent, final List<String> strings, final int x,
        final int y, final int width, final int height,
        final TAction enterAction, final TAction moveAction,
        final TAction singleClickAction) {

        super(parent, x, y, width, height);
        this.padding = ControlPadding.current().getCells();
        this.enterAction = enterAction;
        this.moveAction = moveAction;
        this.singleClickAction = singleClickAction;
        this.strings = new ArrayList<>();
        if (strings != null) {
            this.strings.addAll(strings);
        }

        hScroller = new THScroller(this, 0, getHeight() - 1, calculateHScrollerWidth());
        vScroller = new TVScroller(this, getWidth() - 1, 0, getHeight() - 1);
        reflowData();
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
        if (mouse.isMouseWheelUp()) {
            verticalDecrement();
            return;
        }
        if (mouse.isMouseWheelDown()) {
            verticalIncrement();
            return;
        }
        if (mouse.isMouseWheelLeft()) {
            horizontalIncrement();
            return;
        }
        if (mouse.isMouseWheelRight()) {
            horizontalDecrement();
            return;
        }

        if ((mouse.getX() < getWidth() - 1)
            && (mouse.getY() < getHeight() - 1)
        ) {
            if (getVerticalValue() + mouse.getY() < strings.size()) {
                selectedString = getVerticalValue() + mouse.getY();
                dispatchSingleClick();
            }
            return;
        }

        // Pass to children
        super.onMouseDown(mouse);
    }

    /**
     * Handle mouse double click.
     *
     * @param mouse mouse double click event
     */
    @Override
    public void onMouseDoubleClick(final TMouseEvent mouse) {
        if ((mouse.getX() < getWidth() - 1)
            && (mouse.getY() < getHeight() - 1)
        ) {
            if (getVerticalValue() + mouse.getY() < strings.size()) {
                selectedString = getVerticalValue() + mouse.getY();
                dispatchEnter();
            }
            return;
        }

        // Pass to children
        super.onMouseDoubleClick(mouse);
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.matchesKey(kbLeft)) {
            horizontalDecrement();
        } else if (keypress.matchesKey(kbRight)) {
            horizontalIncrement();
        } else if (keypress.matchesKey(kbUp)) {
            if (!strings.isEmpty()) {
                if (selectedString >= 0) {
                    if (selectedString > 0) {
                        if (selectedString - getVerticalValue() == 0) {
                            verticalDecrement();
                        }
                        selectedString--;
                    }
                } else {
                    selectedString = strings.size() - 1;
                }
            }
            if (selectedString >= 0) {
                dispatchMove();
            }
        } else if (keypress.matchesKey(kbDown)) {
            if (!strings.isEmpty()) {
                if (selectedString >= 0) {
                    if (selectedString < strings.size() - 1) {
                        selectedString++;
                        if (selectedString - getVerticalValue() == getHeight() - 1) {
                            verticalIncrement();
                        }
                    }
                } else {
                    selectedString = 0;
                }
            }
            if (selectedString >= 0) {
                dispatchMove();
            }
        } else if (keypress.matchesKey(kbPgUp)) {
            bigVerticalDecrement();
            if (selectedString >= 0) {
                selectedString -= getHeight() - 1;
                if (selectedString < 0) {
                    selectedString = 0;
                }
            }
            if (selectedString >= 0) {
                dispatchMove();
            }
        } else if (keypress.matchesKey(kbPgDn)) {
            bigVerticalIncrement();
            if (selectedString >= 0) {
                selectedString += getHeight() - 1;
                if (selectedString > strings.size() - 1) {
                    selectedString = strings.size() - 1;
                }
            }
            if (selectedString >= 0) {
                dispatchMove();
            }
        } else if (keypress.matchesKey(kbHome)) {
            toTop();
            if (!strings.isEmpty()) {
                selectedString = 0;
            }
            if (selectedString >= 0) {
                dispatchMove();
            }
        } else if (keypress.matchesKey(kbEnd)) {
            toBottom();
            if (!strings.isEmpty()) {
                selectedString = strings.size() - 1;
            }
            if (selectedString >= 0) {
                dispatchMove();
            }
        } else if (keypress.matchesKey(kbTab)) {
            getParent().switchWidget(true);
        } else if (keypress.matchesKey(kbShiftTab) || keypress.matchesKey(kbBackTab)) {
            getParent().switchWidget(false);
        } else if (keypress.matchesKey(kbEnter)) {
            if (selectedString >= 0) {
                dispatchEnter();
            }
        } else {
            // Pass other keys (tab etc.) on
            super.onKeypress(keypress);
        }
    }

    /**
     * The list uses Enter to trigger its selection action, so it must keep the
     * keypress instead of activating the window default button.
     *
     * @param keypress keystroke event
     * @return true if this widget should handle the keypress first
     */
    @Override
    protected boolean receivesKeypressBeforeWindowDefaultButton(
        final TKeypressEvent keypress) {

        return keypress.matchesKey(kbEnter);
    }

    /**
     * Handle window/screen resize events.
     *
     * @param event resize event
     */
    @Override
    public void onResize(final TResizeEvent event) {
        // Retain selection through list resizes.
        int selectedIndex = getSelectedIndex();
        super.onResize(event);
        setSelectedIndex(selectedIndex);
    }

    // ------------------------------------------------------------------------
    // TScrollable ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Override TWidget's width: we need to set child widget widths.
     *
     * @param width new widget width
     */
    @Override
    public void setWidth(final int width) {
        super.setWidth(width);
        if (hScroller != null) {
            hScroller.setWidth(calculateHScrollerWidth());
        }
        if (vScroller != null) {
            vScroller.setX(getWidth() - 1);
        }
    }

    /**
     * Override TWidget's height: we need to set child widget heights.
     *
     * @param height new widget height
     */
    @Override
    public void setHeight(final int height) {
        super.setHeight(height);
        if (hScroller != null) {
            hScroller.setY(getHeight() - 1);
        }
        if (vScroller != null) {
            vScroller.setHeight(getHeight() - 1);
        }

        // The scrollbar range depends on the height, so it has to be
        // recomputed before the selection is scrolled back into view.
        updateScrollRange();
        setSelectedIndex(selectedString);
    }

    /**
     * Resize for a new width/height.
     */
    @Override
    public void reflowData() {

        // Reset the lines
        selectedString = -1;
        updateScrollRange();
    }

    /**
     * Recompute the scrollbar ranges from the list contents and the current
     * width/height.  Unlike reflowData(), this leaves the selection alone.
     */
    private void updateScrollRange() {
        if (strings == null) {
            return;
        }

        int maxLineWidth = 0;
        for (String line : strings) {
            int lineLength = StringUtils.width(line);
            if (lineLength > maxLineWidth) {
                maxLineWidth = lineLength;
            }
        }

        setBottomValue(Math.max(0, strings.size() - getVisibleRows()));
        if (getVerticalValue() > getBottomValue()) {
            setVerticalValue(getBottomValue());
        }

        setRightValue(Math.max(0, maxLineWidth - getWidth() + 1 + 2 * padding));
        if (getHorizontalValue() > getRightValue()) {
            setHorizontalValue(getRightValue());
        }
    }

    /**
     * The number of list rows that fit inside this widget.  The last row is
     * taken by the horizontal scrollbar.
     *
     * @return the number of visible rows, at least 1
     */
    private int getVisibleRows() {
        return Math.max(1, getHeight() - 1);
    }

    /**
     * Draw the list.
     */
    @Override
    public void draw() {
        CellAttributes color;
        int begin = getVerticalValue();
        int topY = 0;
        // Visible row width excludes the vertical scrollbar (1 cell) and
        // the optional left and right padding cells.
        int rowWidth = Math.max(0, getWidth() - 1 - 2 * padding);
        for (int i = begin; i < strings.size(); i++) {
            String line = strings.get(i);
            if (line == null) {
                line = "";
            }
            if (getHorizontalValue() < line.length()) {
                line = line.substring(getHorizontalValue());
            } else {
                line = "";
            }
            if (i == selectedString) {
                if (isAbsoluteActive()) {
                    color = getWidgetColor("tlist.selected");
                } else {
                    color = getWidgetColor("tlist.selected.inactive");
                }
            } else if (isAbsoluteActive()) {
                color = getWidgetColor("tlist");
            } else {
                color = getWidgetColor("tlist.inactive");
            }
            if (padding > 0) {
                // Paint left and right padding cells for this row.
                for (int p = 0; p < padding; p++) {
                    putCharXY(p, topY, ' ', color);
                    putCharXY(getWidth() - 2 - p, topY, ' ', color);
                }
            }
            String formatString = "%-" + rowWidth + "s";
            putStringXY(padding, topY, String.format(formatString, line),
                color);
            topY++;
            if (topY >= getHeight() - 1) {
                break;
            }
        }

        if (isAbsoluteActive()) {
            color = getWidgetColor("tlist");
        } else {
            color = getWidgetColor("tlist.inactive");
        }

        // Pad the rest with blank lines
        for (int i = topY; i < getHeight() - 1; i++) {
            hLineXY(0, i, getWidth() - 1, ' ', color);
        }
    }

    // ------------------------------------------------------------------------
    // TList ------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the selection index.
     *
     * @return -1 if nothing is selected, otherwise the index into the list
     */
    public final int getSelectedIndex() {
        return selectedString;
    }

    /**
     * Set the selected string index.
     *
     * @param index -1 to unselect, otherwise the index into the list
     */
    public final void setSelectedIndex(final int index) {
        if ((strings == null) || (strings.isEmpty()) || (index < 0)) {
            toTop();
            selectedString = -1;
            return;
        }
        if (index > strings.size() - 1) {
            toBottom();
            selectedString = strings.size() - 1;
            return;
        }

        selectedString = index;

        // Scroll down just far enough to bring the selection into view.
        // This is computed directly rather than by repeatedly calling
        // verticalIncrement(): the scrollbar silently refuses to move once
        // it is at its bottom value, so a loop that waits for it to move
        // never ends when the range is smaller than the selection (which
        // happens whenever the widget is made shorter).
        toTop();
        int firstVisible = index - getVisibleRows() + 1;
        if (firstVisible > getVerticalValue()) {
            setVerticalValue(Math.max(getTopValue(),
                    Math.min(getBottomValue(), firstVisible)));
        }
    }

    /**
     * Get a selectable string by index.
     *
     * @param idx index into list
     * @return the string at idx in the list
     */
    public final String getListItem(final int idx) {
        return strings.get(idx);
    }

    /**
     * Set a selectable string by index.
     *
     * @param idx index into list
     * @param str the new string to use at idx in the list
     */
    public final void setListItem(final int idx, final String str) {
        strings.set(idx, str);
    }

    /**
     * Get the selected string.
     *
     * @return the selected string, or null of nothing is selected yet
     */
    public final String getSelected() {
        if ((selectedString >= 0) && (selectedString <= strings.size() - 1)) {
            return strings.get(selectedString);
        }
        return null;
    }

    /**
     * Get the maximum selection index value.
     *
     * @return -1 if the list is empty
     */
    public final int getMaxSelectedIndex() {
        return strings.size() - 1;
    }

    /**
     * Get a copy of the list of strings to display.
     *
     * @return the list of strings
     */
    public final List<String> getList() {
        return new ArrayList<>(strings);
    }

    /**
     * Set the new list of strings to display.
     *
     * @param list new list of strings
     */
    public final void setList(final List<String> list) {
        strings.clear();
        strings.addAll(list);
        reflowData();
    }

    /**
     * Sets the dimensions of the widget and adjusts its layout.
     *
     * @param x the absolute X position of the top-left corner
     * @param y the absolute Y position of the top-left corner
     * @param width the new width of the widget
     * @param height the new height of the widget
     */
    @Override
    public void setDimensions(int x, int y, int width, int height) {
        super.setDimensions(x, y, width, height);

        placeScrollbars();
    }

    /**
     * Perform user selection action.
     */
    public void dispatchEnter() {
        assert (selectedString >= 0);
        assert (selectedString < strings.size());
        if (enterAction != null) {
            enterAction.DO(this);
        }
    }

    /**
     * Perform list movement action.
     */
    public void dispatchMove() {
        assert (selectedString >= 0);
        assert (selectedString < strings.size());
        if (moveAction != null) {
            moveAction.DO(this);
        }
    }

    /**
     * Perform single-click action.
     */
    public void dispatchSingleClick() {
        assert (selectedString >= 0);
        assert (selectedString < strings.size());
        if (singleClickAction != null) {
            singleClickAction.DO(this);
        }
    }

}
