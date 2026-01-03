/*
 * Casciian - Java Text User Interface
 *
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
package demo;

import java.util.ResourceBundle;

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
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The name of the resource bundle for this class.
     */
    public static final String RESOURCE_BUNDLE_NAME = DemoShadowOpacityDialog.class.getName() + "Bundle";

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
        super(parent, "", 0, 0, 50, 9, MODAL | CENTERED);

        ResourceBundle i18n = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME, getLocale());
        setTitle(i18n.getString("title"));

        int opacity = SystemProperties.getShadowOpacity();
        previousValue = opacity;

        String label = i18n.getString("label");
        setWidth(label.length() + 12);

        addLabel(label, 2, 1);

        // Add the text field
        valueField = addField(label.length() + 3, 1, 5, false, String.valueOf(opacity));
        valueField.setUpdateAction(new TAction() {
            @Override
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
        String buttonLabel = i18n.getString("done");
        addButton(buttonLabel, getWidth() / 2 - (buttonLabel.length() + 4), 5, () -> {
            applyChanges();
            getApplication().closeWindow(DemoShadowOpacityDialog.this);
        });

        // Add Reset button
        addButton(i18n.getString("reset"), getWidth() / 2 + 1, 5, () -> {
            hScroller.setValue(previousValue);
            applyChanges();
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
    // DemoShadowOpacityDialog -----------------------------------------------
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
