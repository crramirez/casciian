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
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import casciian.bits.BorderStyle;
import casciian.bits.CellAttributes;
import casciian.bits.ControlPadding;
import casciian.event.TKeypressEvent;

/**
 * TEditDesktopStyleWindow provides an easy UI for users to alter the running
 * border styles and button style.
 *
 */
public class TEditDesktopStyleWindow extends TDialog {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The name of the resource bundle for this class.
     */
    public static final String RESOURCE_BUNDLE_NAME = TEditDesktopStyleWindow.class.getName() + "Bundle";

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The left-side list of border names pane.
     */
    private final TList borderNames;

    /**
     * The selected choice for border style.
     */
    private final TComboBox borderStyle;

    /**
     * The style to show for the selected border name.
     */
    private BorderStyle shownBorderStyle = BorderStyle.NONE;

    /**
     * The border styles being edited.
     */
    private final Properties editBorderStyles = new Properties();

    /**
     * The selected choice for button style.
     */
    private final TComboBox buttonStyle;

    /**
     * Example button 1.
     */
    private final TButton button1;

    /**
     * The selected choice for checkbox style.
     */
    private final TComboBox checkBoxStyle;

    /**
     * Example checkbox.
     */
    private final TCheckBox checkBox1;

    /**
     * The selected choice for radio-button style.
     */
    private final TComboBox radioButtonStyle;

    /**
     * Example radio button.
     */
    private final TRadioButton radioButton1;

    /**
     * The selected choice for the controls padding style.
     */
    private final TComboBox controlsPadding;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.  The window will be centered on screen.
     *
     * @param application the TApplication that manages this window
     */
    @SuppressWarnings("this-escape")
    public TEditDesktopStyleWindow(final TApplication application) {

        // Register with the TApplication
        super(application, "", 0, 0, 70, 27, MODAL);

        ResourceBundle i18n = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME,
            getLocale());
        setTitle(i18n.getString("windowTitle"));

        // Initialize with the first border
        List<String> borders = createBorders();
        assert (!borders.isEmpty());
        for (String borderName : borders) {
            editBorderStyles.put(borderName, System.getProperty(borderName,
                "default"));
        }

        int row = 1;

        borderNames = addLabelFor(i18n.getString("borderName"), 2, row,
            addList(borders, 2, row + 1, 43, 7,
                new TAction() {
                    // When the user presses Enter
                    public void DO() {
                        updateShownBorderStyle();
                    }
                },
                new TAction() {
                    // When the user navigates with keyboard
                    public void DO() {
                        updateShownBorderStyle();
                    }
                },
                new TAction() {
                    // When the user navigates with keyboard
                    public void DO() {
                        updateShownBorderStyle();
                    }
                }
            ));
        borderNames.setSelectedIndex(0);

        List<String> borderStyles = BorderStyle.getStyleNames();
        borderStyle = addLabelFor(i18n.getString("borderStyle"), 47, row,
            addComboBox(47, row + 1, 18, borderStyles, 0, 7,
                new TAction() {
                    public void DO() {
                        String borderName = borderNames.getSelected();
                        assert (borderName != null);
                        String newBorderStyle = borderStyle.getText();

                        editBorderStyles.setProperty(borderName, newBorderStyle);
                        updateShownBorderStyle();
                    }
                }));

        updateShownBorderStyle();

        row += 9;

        var buttonStyles = new ArrayList<String>();
        buttonStyles.add("square");
        buttonStyles.add("brackets");
        buttonStyles.add("diamonds");

        buttonStyle = addLabelFor(i18n.getString("buttonStyle"), 2, row,
            addComboBox(2, row + 1, 18, buttonStyles, 0, 6,
                new TAction() {
                    public void DO() {
                        String newButtonStyle = buttonStyle.getText();
                        button1.setStyle(newButtonStyle);
                    }
                }));
        String buttonStyleString = System.getProperty("casciian.TButton.style",
            "square");
        buttonStyle.setText(buttonStyleString);

        button1 = addButton(i18n.getString("button1"), 24, row + 1, () -> {});
        button1.setStyle(buttonStyleString);

        row += 3;

        List<String> checkBoxStyles = TCheckBox.getStyleNames();
        checkBoxStyle = addLabelFor(i18n.getString("checkBoxStyle"), 2, row,
            addComboBox(2, row + 1, 18, checkBoxStyles, 0,
                checkBoxStyles.size() + 2, new TAction() {
                    public void DO() {
                        checkBox1.setStyle(checkBoxStyle.getText());
                    }
                }));
        String checkBoxStyleString = System.getProperty(TCheckBox.PROPERTY_KEY,
            TCheckBox.DEFAULT_STYLE_NAME);
        checkBoxStyle.setText(checkBoxStyleString);

        checkBox1 = addCheckBox(25, row + 1, i18n.getString("checkBox1"), true);
        checkBox1.setStyle(checkBoxStyleString);

        row += 3;

        List<String> radioButtonStyles = TRadioButton.getStyleNames();
        radioButtonStyle = addLabelFor(i18n.getString("radioButtonStyle"), 2,
            row, addComboBox(2, row + 1, 18, radioButtonStyles, 0,
                radioButtonStyles.size() + 2, new TAction() {
                    public void DO() {
                        radioButton1.setStyle(radioButtonStyle.getText());
                    }
                }));
        String radioButtonStyleString = System.getProperty(
            TRadioButton.PROPERTY_KEY, TRadioButton.DEFAULT_STYLE_NAME);
        radioButtonStyle.setText(radioButtonStyleString);

        TRadioGroup radioGroup1 = addRadioGroup(24, row, 28,
            i18n.getString("radioGroupTitle"));
        radioButton1 = radioGroup1.addRadioButton(i18n.getString("radioButton1"),
            true);
        radioButton1.setStyle(radioButtonStyleString);

        // Controls padding combobox
        row += 3;

        List<String> paddingStyles = ControlPadding.getStyleNames();
        String controlsPaddingString = System.getProperty(
            ControlPadding.PROPERTY_KEY,
            ControlPadding.DEFAULT_STYLE_NAME);

        controlsPadding = addLabelFor(i18n.getString("controlsPadding"), 2, row,
            addComboBox(2, row + 1, 18, paddingStyles, 0,
                paddingStyles.size() + 2, (TAction) null));
        controlsPadding.setText(controlsPaddingString);

        setDefaultButton(addButton(i18n.getString("okButton"), 6, getHeight() - 4,
            new TAction() {
                public void DO() {
                    for (String name : editBorderStyles.stringPropertyNames()) {
                        String value = editBorderStyles.getProperty(name);
                        System.setProperty(name, value);
                    }
                    String newButtonStyle = buttonStyle.getText();
                    System.setProperty("casciian.TButton.style", newButtonStyle);
                    String newCheckBoxStyle = checkBoxStyle.getText();
                    System.setProperty(TCheckBox.PROPERTY_KEY,
                        newCheckBoxStyle);
                    String newRadioButtonStyle = radioButtonStyle.getText();
                    System.setProperty(TRadioButton.PROPERTY_KEY,
                        newRadioButtonStyle);
                    String newPadding = controlsPadding.getText();
                    System.setProperty(ControlPadding.PROPERTY_KEY,
                        newPadding);
                    getApplication().closeWindow(TEditDesktopStyleWindow.this);
                }
            }
        ));

        addButton(i18n.getString("cancelButton"), getWidth() - 16,
            getHeight() - 4,
            new TAction() {
                public void DO() {
                    getApplication().closeWindow(TEditDesktopStyleWindow.this);
                }
            }
        );

        // Default to the border list
        activate(borderNames);

        // Add shortcut text
        newStatusBar(i18n.getString("statusBar"));
    }

    private static List<String> createBorders() {
        return Stream.of(
            "casciian.TEditColorTheme.borderStyle",
            "casciian.TEditColorTheme.options.borderStyle",
            "casciian.TEditDesktopStyle.borderStyle",
            "casciian.TMenu.borderStyle",
            "casciian.TPanel.borderStyle",
            "casciian.TRadioGroup.borderStyle",
            "casciian.TScreenOptions.borderStyle",
            "casciian.TScreenOptions.grid.borderStyle",
            "casciian.TScreenOptions.options.borderStyle",
            "casciian.TWindow.borderStyleForeground",
            "casciian.TWindow.borderStyleInactive",
            "casciian.TWindow.borderStyleModal",
            "casciian.TWindow.borderStyleMoving")

            .sorted()
            .toList();
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        // Pass to my parent
        super.onKeypress(keypress);
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw me on screen.
     */
    @Override
    public void draw() {
        super.draw();
        CellAttributes attr = new CellAttributes();

        // Draw the border style example box
        attr.setTo(getTheme().getColor("twindow.background"));
        CellAttributes border = new CellAttributes();
        border.setTo(getTheme().getColor("twindow.border"));
        drawBox(borderNames.getX() + borderNames.getWidth() + 3,
            borderNames.getY() + 3, getWidth() - 3, borderNames.getY() + 8,
            border, attr, shownBorderStyle);
    }

    // ------------------------------------------------------------------------
    // TEditDesktopStyleWindow ------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Update the shown border with the style of the selected border name.
     */
    private void updateShownBorderStyle() {
        String borderName = borderNames.getSelected();
        assert (borderName != null);

        String borderStyleName = editBorderStyles.getProperty(borderName,
            "default");
        BorderStyle style = BorderStyle.getStyle(borderStyleName);

        if (borderStyleName.equalsIgnoreCase("default")) {
            // This is ugly! But we put the default border style of the
            // Casciian widgets here.

            // TWindow
            if (borderName.equals("casciian.TWindow.borderStyleForeground")) {
                style = BorderStyle.DOUBLE;
            }
            if (borderName.equals("casciian.TWindow.borderStyleModal")) {
                style = BorderStyle.DOUBLE;
            }
            if (borderName.equals("casciian.TWindow.borderStyleMoving")) {
                style = BorderStyle.SINGLE;
            }
            if (borderName.equals("casciian.TWindow.borderStyleInactive")) {
                style = BorderStyle.SINGLE;
            }

            // TMenu
            if (borderName.equals("casciian.TMenu.borderStyle")) {
                style = BorderStyle.SINGLE;
            }

            // TEditColorThemeWindow
            if (borderName.equals("casciian.TEditColorTheme.borderStyle")) {
                style = BorderStyle.DOUBLE;
            }
            if (borderName.equals("casciian.TEditColorTheme.options.borderStyle")) {
                style = BorderStyle.SINGLE;
            }

            // TEditDesktopStyleWindow
            if (borderName.equals("casciian.TEditDesktopStyle.borderStyle")) {
                style = BorderStyle.DOUBLE;
            }

            // TPanel
            if (borderName.equals("casciian.TPanel.borderStyle")) {
                style = BorderStyle.NONE;
            }

            // TRadioGroup
            if (borderName.equals("casciian.TRadioGroup.borderStyle")) {
                style = BorderStyle.SINGLE_V_DOUBLE_H;
            }

            // TScreenOptionsWindow
            if (borderName.equals("casciian.TScreenOptions.borderStyle")) {
                style = BorderStyle.SINGLE;
            }
            if (borderName.equals("casciian.TScreenOptions.grid.borderStyle")) {
                style = BorderStyle.SINGLE;
            }
            if (borderName.equals("casciian.TScreenOptions.options.borderStyle")) {
                style = BorderStyle.SINGLE;
            }

        }
        shownBorderStyle = style;

        borderStyle.setText(borderStyleName);
    }

    /**
     * Set the border style for the window when it is the foreground window.
     *
     * @param borderStyle the border style string, one of: "default", "none",
     *                    "single", "double", "singleVdoubleH", "singleHdoubleV", or "round"; or
     *                    null to use the value from casciian.TEditDesktopStyle.borderStyle.
     */
    @Override
    public void setBorderStyleForeground(final String borderStyle) {
        if (borderStyle == null) {
            String style = System.getProperty("casciian.TEditDesktopStyle.borderStyle",
                "double");
            super.setBorderStyleForeground(style);
        } else {
            super.setBorderStyleForeground(borderStyle);
        }
    }

    /**
     * Set the border style for the window when it is the modal window.
     *
     * @param borderStyle the border style string, one of: "default", "none",
     *                    "single", "double", "singleVdoubleH", "singleHdoubleV", or "round"; or
     *                    null to use the value from casciian.TEditDesktopStyle.borderStyle.
     */
    @Override
    public void setBorderStyleModal(final String borderStyle) {
        if (borderStyle == null) {
            String style = System.getProperty("casciian.TEditDesktopStyle.borderStyle",
                "double");
            super.setBorderStyleModal(style);
        } else {
            super.setBorderStyleModal(borderStyle);
        }
    }

    /**
     * Set the border style for the window when it is an inactive/background
     * window.
     *
     * @param borderStyle the border style string, one of: "default", "none",
     *                    "single", "double", "singleVdoubleH", "singleHdoubleV", or "round"; or
     *                    null to use the value from casciian.TEditDesktopStyle.borderStyle.
     */
    @Override
    public void setBorderStyleInactive(final String borderStyle) {
        if (borderStyle == null) {
            String style = System.getProperty("casciian.TEditDesktopStyle.borderStyle",
                "double");
            super.setBorderStyleInactive(style);
        } else {
            super.setBorderStyleInactive(borderStyle);
        }
    }

    /**
     * Set the border style for the window when it is being dragged/resize.
     *
     * @param borderStyle the border style string, one of: "default", "none",
     *                    "single", "double", "singleVdoubleH", "singleHdoubleV", or "round"; or
     *                    null to use the value from casciian.TEditDesktopStyle.borderStyle.
     */
    @Override
    public void setBorderStyleMoving(final String borderStyle) {
        if (borderStyle == null) {
            String style = System.getProperty("casciian.TEditDesktopStyle.borderStyle",
                "double");
            super.setBorderStyleMoving(style);
        } else {
            super.setBorderStyleMoving(borderStyle);
        }
    }

}
