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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.ResourceBundle;

import casciian.TAction;
import casciian.TApplication;
import casciian.TCalendar;
import casciian.TField;
import casciian.TLabel;
import casciian.TMessageBox;
import casciian.TWindow;
import casciian.layout.StretchLayoutManager;
import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * This window demonstates the TField and TPasswordField widgets.
 */
public class DemoTextFieldWindow extends TWindow {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * Calendar.  Has to be at class scope so that it can be accessed by the
     * anonymous TAction class.
     */
    TCalendar calendar = null;

    /**
     * Day of week label is updated with TSpinner clicks.
     */
    TLabel dayOfWeekLabel;

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
        super(parent, "", 0, 0, 60, 20, flags);
        i18n = ResourceBundle.getBundle(DemoTextFieldWindow.class.getName(),
            getLocale());
        setTitle(i18n.getString("windowTitle"));

        setLayoutManager(new StretchLayoutManager(getWidth() - 2,
                getHeight() - 2));

        int row = 1;

        addLabel(i18n.getString("textField1"), 1, row);
        addField(35, row++, 15, false, i18n.getString("fieldText"));
        addLabel(i18n.getString("textField2"), 1, row);
        addField(35, row++, 15, true);
        addLabel(i18n.getString("textField3"), 1, row);
        addPasswordField(35, row++, 15, false);
        addLabel(i18n.getString("textField4"), 1, row);
        addPasswordField(35, row++, 15, true, "hunter2");
        addLabel(i18n.getString("textField5"), 1, row);
        TField selected = addField(35, row++, 40, false,
            i18n.getString("textField6"));
        row += 1;

        calendar = addCalendar(1, row++,
            new TAction() {
                public void DO() {
                    getApplication().messageBox(i18n.getString("calendarTitle"),
                        MessageFormat.format(i18n.getString("calendarMessage"),
                            new Date(calendar.getValue().getTimeInMillis())),
                        TMessageBox.Type.OK);
                }
            }
        );

        dayOfWeekLabel = addLabel("Wednesday-", 35, row - 1, "tmenu", false);
        dayOfWeekLabel.setLabel(String.format("%-10s",
                dayOfWeekCalendar.getDisplayName(Calendar.DAY_OF_WEEK,
                    Calendar.LONG, Locale.getDefault())));

        addSpinner(35 + dayOfWeekLabel.getWidth(), row - 1,
            new TAction() {
                public void DO() {
                    dayOfWeekCalendar.add(Calendar.DAY_OF_WEEK, 1);
                    dayOfWeekLabel.setLabel(String.format("%-10s",
                            dayOfWeekCalendar.getDisplayName(
                            Calendar.DAY_OF_WEEK, Calendar.LONG,
                            Locale.getDefault())));
                }
            },
            new TAction() {
                public void DO() {
                    dayOfWeekCalendar.add(Calendar.DAY_OF_WEEK, -1);
                    dayOfWeekLabel.setLabel(String.format("%-10s",
                            dayOfWeekCalendar.getDisplayName(
                            Calendar.DAY_OF_WEEK, Calendar.LONG,
                            Locale.getDefault())));
                }
            }
        );


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

}
