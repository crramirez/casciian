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

import casciian.bits.BorderStyle;
import casciian.bits.CellAttributes;
import casciian.bits.StringUtils;

/**
 * TRadioGroup is a collection of TRadioButtons with a box and label.
 */
public class TRadioGroup extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Label for this radio button group.
     */
    private String label;

    /**
     * Only one of my children can be selected.
     */
    private TRadioButton selectedButton = null;

    /**
     * If true, one of the children MUST be selected.  Note package private
     * access.
     */
    boolean requiresSelection = false;

    /**
     * If true, use the window's background color.
     */
    private boolean matchWindowBackground = true;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of group
     * @param label label to display on the group box
     */
    public TRadioGroup(final TWidget parent, final int x, final int y,
        final int width, final String label) {

        // Set parent and window
        super(parent, x, y, width, 2);

        this.label = label;
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param label label to display on the group box
     */
    public TRadioGroup(final TWidget parent, final int x, final int y,
        final String label) {

        // Set parent and window
        super(parent, x, y, StringUtils.width(label) + 4, 2);

        this.label = label;
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Override TWidget's width: we can only set width at construction time.
     *
     * @param width new widget width (ignored)
     */
    @Override
    public void setWidth(final int width) {
        // Do nothing
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
     * Draw a radio button with label.
     */
    @Override
    public void draw() {
        CellAttributes radioGroupColor;

        if (isAbsoluteActive()) {
            radioGroupColor = getTheme().getColor("tradiogroup.active");
        } else {
            radioGroupColor = getTheme().getColor("tradiogroup.inactive");
        }

        BorderStyle borderStyle;
        borderStyle = BorderStyle.getStyle(System.getProperty(
            "casciian.TRadioGroup.borderStyle", "singleVdoubleH"));

        if (matchWindowBackground) {
            drawForegroundBox(0, 0, getWidth(), getHeight(), radioGroupColor,
                radioGroupColor, borderStyle, false);
        } else {
            drawBox(0, 0, getWidth(), getHeight(), radioGroupColor,
                radioGroupColor, borderStyle, false);
        }

        if (matchWindowBackground) {
            hForegroundLineXY(1, 0, StringUtils.width(label) + 2, ' ',
                radioGroupColor);
        } else {
            hLineXY(1, 0, StringUtils.width(label) + 2, ' ', radioGroupColor);
        }
        if (borderStyle.equals(BorderStyle.NONE)) {
            if (matchWindowBackground) {
                putForegroundStringXY(1, 0, label, radioGroupColor);
            } else {
                putStringXY(1, 0, label, radioGroupColor);
            }
        } else {
            if (matchWindowBackground) {
                putForegroundStringXY(2, 0, label, radioGroupColor);
            } else {
                putStringXY(2, 0, label, radioGroupColor);
            }
        }
    }

    // ------------------------------------------------------------------------
    // TRadioGroup ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the radio button ID that was selected.
     *
     * @return ID of the selected button, or 0 if no button is selected
     */
    public int getSelected() {
        if (selectedButton == null) {
            return 0;
        }
        return selectedButton.getId();
    }

    /**
     * Set the new selected radio button.  1-based.
     *
     * @param id ID of the selected button, or 0 to unselect
     */
    public void setSelected(final int id) {
        if ((id < 0) || (id > getChildren().size())) {
            return;
        }

        for (TWidget widget: getChildren()) {
            ((TRadioButton) widget).selected = false;
        }
        if (id == 0) {
            selectedButton = null;
            return;
        }
        assert ((id > 0) && (id <= getChildren().size()));
        TRadioButton button = (TRadioButton) (getChildren().get(id - 1));
        button.selected = true;
        selectedButton = button;
    }

    /**
     * Get the radio button that was selected.
     *
     * @return the selected button, or null if no button is selected
     */
    public TRadioButton getSelectedButton() {
        return selectedButton;
    }

    /**
     * Convenience function to add a radio button to this group.
     *
     * @param label label to display next to (right of) the radiobutton
     * @param selected if true, this will be the selected radiobutton
     * @return the new radio button
     */
    public TRadioButton addRadioButton(final String label,
        final boolean selected) {

        TRadioButton button = addRadioButton(label);
        setSelected(button.id);
        return button;
    }

    /**
     * Convenience function to add a radio button to this group.
     *
     * @param label label to display next to (right of) the radiobutton
     * @return the new radio button
     */
    public TRadioButton addRadioButton(final String label) {
        return new TRadioButton(this, 0, 0, label, 0);
    }

    /**
     * Package private method for RadioButton to add itself to a RadioGroup
     * container.
     *
     * @param button the button to add
     */
    void addRadioButton(final TRadioButton button) {
        super.setHeight(getChildren().size() + 2);
        button.setX(1);
        button.setY(getChildren().size());
        button.id = getChildren().size();
        String label = button.getMnemonic().getRawLabel();

        if (StringUtils.width(label) + 4 > getWidth()) {
            super.setWidth(StringUtils.width(label) + 7);
        }

        if (getParent().getLayoutManager() != null) {
            getParent().getLayoutManager().resetSize(this);
        }

        // Default to the first item on the list.
        activate(getChildren().get(0));
    }

    /**
     * Get the requires selection flag.
     *
     * @return true if this radiogroup requires that one of the buttons be
     * selected
     */
    public boolean getRequiresSelection() {
        return requiresSelection;
    }

    /**
     * Set the requires selection flag.
     *
     * @param requiresSelection if true, then this radiogroup requires that
     * one of the buttons be selected
     */
    public void setRequiresSelection(final boolean requiresSelection) {
        this.requiresSelection = requiresSelection;
        if (requiresSelection) {
            if ((getChildren().size() > 0) && (selectedButton == null)) {
                setSelected(1);
            }
        }
    }

    /**
     * Get the window background option.
     *
     * @return true if the window's background color will be used
     */
    public boolean isMatchWindowBackground() {
        return matchWindowBackground;
    }

    /**
     * Set the window background option.
     *
     * @param matchWindowBackground if true, the window's background color
     * will be used
     */
    public void setMatchWindowBackground(final boolean matchWindowBackground) {
        this.matchWindowBackground = matchWindowBackground;

        for (TWidget w: getChildren()) {
            if (w instanceof TRadioButton) {
                TRadioButton button = (TRadioButton) w;
                button.setMatchWindowBackground(matchWindowBackground);
            }
        }
    }

}
