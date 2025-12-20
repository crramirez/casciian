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

import casciian.TAction;
import casciian.TApplication;
import casciian.TField;
import casciian.THScroller;
import casciian.TWindow;
import casciian.backend.SystemProperties;

/**
 * Dialog to change shadow opacity.
 */
public class DemoShadowOpacityDialog extends TWindow {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The horizontal scrollbar.
     */
    private final THScroller hScroller;

    /**
     * The text field to display and edit the value.
     */
    private final TField valueField;

    /**
     * The previous value of the shadow opacity.
     */
    private final int previousValue;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Constructor.
     *
     * @param parent the main application
     */
    public DemoShadowOpacityDialog(final TApplication parent) {
        super(parent, "Shadow Opacity", 0, 0, 35, 9, MODAL | CENTERED);

        int opacity = SystemProperties.getShadowOpacity();
        previousValue = opacity;

        addLabel("Shadow Opacity (0-100):", 2, 1);

        // Add the text field
        valueField = addField(26, 1, 5, false, String.valueOf(opacity));
        valueField.setUpdateAction(new TAction() {
            public void DO() {
                updateFromTextField();
            }
        });

        // Add horizontal scrollbar
        hScroller = new THScroller(this, 2, 3, getWidth() - 6);
        hScroller.setLeftValue(0);
        hScroller.setRightValue(100);
        hScroller.setValue(opacity);

        // Add the Done button
        addButton("&Done", getWidth() / 2 - 8, 5, new TAction() {
            public void DO() {
                applyChanges();
                getApplication().closeWindow(DemoShadowOpacityDialog.this);
            }
        });

        // Add Reset button
        addButton("&Reset", getWidth() / 2 + 3, 5, new TAction() {
            public void DO() {
                hScroller.setValue(previousValue);
                applyChanges();
            }
        });

        // Focus on the text field
        activate(valueField);
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the scrollbar and update the text field when scrollbar changes.
     */
    @Override
    public void draw() {
        super.draw();

        // Sync text field with scrollbar value if scrollbar changed
        int scrollValue = hScroller.getValue();
        String fieldText = valueField.getText();
        try {
            int fieldValue = Integer.parseInt(fieldText);
            if (fieldValue != scrollValue) {
                valueField.setText(String.valueOf(scrollValue));
                applyChanges();
            }
        } catch (NumberFormatException e) {
            valueField.setText(String.valueOf(scrollValue));
        }
    }

    // ------------------------------------------------------------------------
    // ShadowOpacityDialog ----------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Update scrollbar based on text field value.
     */
    private void updateFromTextField() {
        try {
            String text = valueField.getText().trim();
            if (text.isEmpty()) {
                hScroller.setValue(0);
                return;
            }
            int value = Integer.parseInt(text);
            if (value < 0) {
                value = 0;
                valueField.setText("0");
            }
            if (value > 100) {
                value = 100;
                valueField.setText("100");
            }
            hScroller.setValue(value);
        } catch (NumberFormatException e) {
            // Invalid input, restore from scrollbar
            valueField.setText(String.valueOf(hScroller.getValue()));
        }
    }

    /**
     * Apply the changes to the system property.
     */
    private void applyChanges() {
        int value = hScroller.getValue();
        SystemProperties.setShadowOpacity(value);
    }
}
