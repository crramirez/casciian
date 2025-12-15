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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import casciian.TAction;
import casciian.TApplication;
import casciian.TCheckBox;
import casciian.TComboBox;
import casciian.TMessageBox;
import casciian.TRadioGroup;
import casciian.TWindow;
import casciian.effect.GradientCellTransform;
import casciian.layout.StretchLayoutManager;
import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * This window demonstates the TRadioGroup, TRadioButton, and TCheckBox
 * widgets.
 */
public class DemoCheckBoxWindow extends TWindow {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * Combo box.  Has to be at class scope so that it can be accessed by the
     * anonymous TAction class.
     */
    TComboBox comboBox = null;

    /**
     * CheckBox 1.  When window gradient background is set, the checkbox is
     * set to match window background.
     */
    TCheckBox checkBox1;

    /**
     * CheckBox 2.  When window gradient background is set, the checkbox is
     * set to match window background.
     */
    TCheckBox checkBox2;

    /**
     * RadioGroup.  When window gradient background is set, the radio group
     * is set to match window background.
     */
    TRadioGroup radioGroup;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Constructor.
     *
     * @param parent the main application
     */
    DemoCheckBoxWindow(final TApplication parent) {
        this(parent, CENTERED | RESIZABLE);
    }

    /**
     * Constructor.
     *
     * @param parent the main application
     * @param flags bitmask of MODAL, CENTERED, or RESIZABLE
     */
    DemoCheckBoxWindow(final TApplication parent, final int flags) {
        // Construct a demo window.  X and Y don't matter because it will be
        // centered on screen.
        super(parent, "", 0, 0, 60, 17, flags);
        i18n = ResourceBundle.getBundle(DemoCheckBoxWindow.class.getName(),
            getLocale());
        setTitle(i18n.getString("windowTitle"));

        setLayoutManager(new StretchLayoutManager(getWidth() - 2,
                getHeight() - 2));

        int row = 1;

        // Add some widgets
        addLabel(i18n.getString("checkBoxLabel1"), 1, row);
        checkBox1 = addCheckBox(40, row++, i18n.getString("checkBoxText1"),
            false);
        addLabel(i18n.getString("checkBoxLabel2"), 1, row);
        checkBox2 = addCheckBox(40, row++, i18n.getString("checkBoxText2"),
            true);
        row += 2;

        radioGroup = addRadioGroup(1, row,
            i18n.getString("radioGroupTitle"));
        radioGroup.addRadioButton(i18n.getString("radioOption1"));
        radioGroup.addRadioButton(i18n.getString("radioOption2"), true);
        radioGroup.addRadioButton(i18n.getString("radioOption3"));
        radioGroup.setRequiresSelection(true);

        List<String> comboValues = new ArrayList<String>();
        comboValues.add(i18n.getString("comboBoxString0"));
        comboValues.add(i18n.getString("comboBoxString1"));
        comboValues.add(i18n.getString("comboBoxString2"));
        comboValues.add(i18n.getString("comboBoxString3"));
        comboValues.add(i18n.getString("comboBoxString4"));
        comboValues.add(i18n.getString("comboBoxString5"));
        comboValues.add(i18n.getString("comboBoxString6"));
        comboValues.add(i18n.getString("comboBoxString7"));
        comboValues.add(i18n.getString("comboBoxString8"));
        comboValues.add(i18n.getString("comboBoxString9"));
        comboValues.add(i18n.getString("comboBoxString10"));

        comboBox = addComboBox(40, row, 12, comboValues, 2, 6,
            new TAction() {
                public void DO() {
                    getApplication().messageBox(i18n.getString("messageBoxTitle"),
                        MessageFormat.format(i18n.getString("messageBoxPrompt"),
                            comboBox.getText()),
                        TMessageBox.Type.OK);
                }
            }
        );

        addButton(i18n.getString("closeWindow"),
            (getWidth() - 14) / 2, getHeight() - 4,
            new TAction() {
                public void DO() {
                    DemoCheckBoxWindow.this.getApplication()
                        .closeWindow(DemoCheckBoxWindow.this);
                }
            }
        );

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
     * Enable or disable a pre-defined gradient for this window's color.
     *
     * @param useGradient if true, paint this window with a gradient
     */
    public void setUseGradient(final boolean useGradient) {
        if (useGradient) {
            int PINK = 0xac7580;
            int BLUE = 0x3b8fb0;
            int YELLOW = 0x7c7c00;
            setDrawPreTransform(new GradientCellTransform(
                GradientCellTransform.Layer.BACKGROUND, PINK,
                BLUE, YELLOW, BLUE), true);
        } else {
            setDrawPreTransform(null);
        }
        checkBox1.setMatchWindowBackground(useGradient);
        checkBox2.setMatchWindowBackground(useGradient);
        radioGroup.setMatchWindowBackground(useGradient);
    }

}
