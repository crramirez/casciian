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
package demo;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.ResourceBundle;

import casciian.TAction;
import casciian.TApplication;
import casciian.TCalendar;
import casciian.TField;
import casciian.TMessageBox;
import casciian.TWindow;
import casciian.bits.StringUtils;
import casciian.layout.StretchLayoutManager;
import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * This window demonstates the TField and TPasswordField widgets.
 */
public class DemoTextFieldWindow extends TWindow {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The name of the resource bundle for this class.
     */
    public static final String RESOURCE_BUNDLE_NAME = DemoTextFieldWindow.class.getName() + "Bundle";
    private static final String LABEL_FORMAT = "%-12s";

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private final ResourceBundle i18n;

    /**
     * Calendar.  Has to be at class scope so that it can be accessed by the
     * anonymous TAction class.
     */
    TCalendar calendar = null;

    /**
     * Day of week label is updated with TSpinner clicks.
     */
    TField dayOfWeekLabel;

    /**
     * Day of week to demonstrate TSpinner.  Has to be at class scope so that
     * it can be accessed by the anonymous TAction class.
     */
    GregorianCalendar dayOfWeekCalendar = new GregorianCalendar();

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Constructor.
     *
     * @param parent the main application
     */
    DemoTextFieldWindow(final TApplication parent) {
        this(parent, TWindow.CENTERED | TWindow.RESIZABLE);
    }

    /**
     * Constructor.
     *
     * @param parent the main application
     * @param flags bitmask of MODAL, CENTERED, or RESIZABLE
     */
    DemoTextFieldWindow(final TApplication parent, final int flags) {
        // Construct a demo window.  X and Y don't matter because it
        // will be centered on screen.
        super(parent, "", 0, 0, 60, 24, flags);
        i18n = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME,
            getLocale());
        setTitle(i18n.getString("windowTitle"));

        setLayoutManager(new StretchLayoutManager(getWidth() - 2,
                getHeight() - 2));

        int row = 1;

        final int fieldWidth = 17;
        TField selected = addLabelFor(i18n.getString("textField1"), 1, row,
            addField(35, row++, fieldWidth, false, i18n.getString("fieldText")));
        row++;
        addLabelFor(i18n.getString("textField2"), 1, row,
            addField(35, row++, fieldWidth, true));
        row++;
        addLabelFor(i18n.getString("textField3"), 1, row,
            addPasswordField(35, row++, fieldWidth, false));
        row++;
        addLabelFor(i18n.getString("textField4"), 1, row,
            addPasswordField(35, row++, fieldWidth, true, "hunter2"));
        row++;
        addLabelFor(i18n.getString("textField5"), 1, row,
            addField(35, row++, 40, false, i18n.getString("textField6")));

        row++;

        calendar = addCalendar(1, row,
            new TAction() {
                public void DO() {
                    getApplication().messageBox(i18n.getString("calendarTitle"),
                        MessageFormat.format(i18n.getString("calendarMessage"),
                            new Date(calendar.getValue().getTimeInMillis())),
                        TMessageBox.Type.OK);
                }
            }
        );

        final int dayOfWeekWidth = getDayOfWeekWidth();
        int fieldPadding = getTheme().getControlPadding().getCells();
        int spinnerOffset = dayOfWeekWidth + 2 * fieldPadding;
        dayOfWeekLabel = addField(35, row, dayOfWeekWidth + 2 * fieldPadding, true);
        dayOfWeekLabel.setEnabled(false);
        dayOfWeekLabel.setText(getDayOfWeekText());

        addSpinner(35 + spinnerOffset, row,
            new TAction() {
                public void DO() {
                    dayOfWeekCalendar.add(Calendar.DAY_OF_WEEK, 1);
                    dayOfWeekLabel.setText(getDayOfWeekText());
                }
            },
            new TAction() {
                public void DO() {
                    dayOfWeekCalendar.add(Calendar.DAY_OF_WEEK, -1);
                    dayOfWeekLabel.setText(getDayOfWeekText());
                }
            }
        );

        row += 2;

        addHyperlink(i18n.getString("hyperlinkText"),
            "https://github.com/crramirez/casciian", 35, row);

        addButton(i18n.getString("closeWindow"),
            (getWidth() - 14) / 2, getHeight() - 4,
            new TAction() {
                public void DO() {
                    getApplication().closeWindow(DemoTextFieldWindow.this);
                }
            }
        );

        activate(selected);

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

    private String getDayOfWeekText() {
        return String.format(LABEL_FORMAT,
            dayOfWeekCalendar.getDisplayName(Calendar.DAY_OF_WEEK,
                Calendar.LONG, Locale.getDefault()));
    }

    private int getDayOfWeekWidth() {
        int width = 0;
        GregorianCalendar calendar = new GregorianCalendar();
        for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
            calendar.set(Calendar.DAY_OF_WEEK, day);
            width = Math.max(width, StringUtils.width(String.format(LABEL_FORMAT,
                    calendar.getDisplayName(Calendar.DAY_OF_WEEK,
                        Calendar.LONG, Locale.getDefault()))));
        }
        return width;
    }

}
