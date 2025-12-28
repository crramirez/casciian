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

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import casciian.TAction;
import casciian.TApplication;
import casciian.TButton;
import casciian.TEditColorThemeWindow;
import casciian.TEditDesktopStyleWindow;
import casciian.TEditorWindow;
import casciian.TLabel;
import casciian.TProgressBar;
import casciian.TTableWindow;
import casciian.TTimer;
import casciian.TWindow;
import casciian.backend.SystemProperties;
import casciian.effect.GradientCellTransform;
import casciian.effect.MouseGlowCellTransform;
import casciian.event.TCommandEvent;
import casciian.layout.StretchLayoutManager;
import casciian.menu.TMenuItem;
import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * This is the main "demo" application window.  It makes use of the TTimer,
 * TProgressBox, TLabel, TButton, and TField widgets.
 */
public class DemoMainWindow extends TWindow {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * Timer that increments a number.
     */
    private TTimer timer1;

    /**
     * Timer that increments a number.
     */
    private TTimer timer2;

    /**
     * Timer label is updated with timer ticks.
     */
    TLabel timerLabel;

    /**
     * Timer increment used by the timer loop.  Has to be at class scope so
     * that it can be accessed by the anonymous TAction class.
     */
    int timer1I = 0;

    /**
     * Timer increment used by the timer loop.  Has to be at class scope so
     * that it can be accessed by the anonymous TAction class.
     */
    int timer2I = 0;

    /**
     * Progress bar used by the timer loop.  Has to be at class scope so that
     * it can be accessed by the anonymous TAction class.
     */
    TProgressBar progressBar1;

    /**
     * Progress bar used by the timer loop.  Has to be at class scope so that
     * it can be accessed by the anonymous TAction class.
     */
    TProgressBar progressBar2;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Construct demo window.  It will be centered on screen.
     *
     * @param parent the main application
     */
    @SuppressWarnings("this-escape")
    public DemoMainWindow(final TApplication parent) {
        this(parent, CENTERED | RESIZABLE);
    }

    /**
     * Constructor.
     *
     * @param parent the main application
     * @param flags bitmask of MODAL, CENTERED, or RESIZABLE
     */
    @SuppressWarnings("this-escape")
    private DemoMainWindow(final TApplication parent, final int flags) {
        // Construct a demo window.  X and Y don't matter because it will be
        // centered on screen.
        super(parent, "", 0, 0, 66, 25, flags);
        i18n = ResourceBundle.getBundle(DemoMainWindow.class.getName(),
            getLocale());
        setTitle(i18n.getString("windowTitle"));

        setLayoutManager(new StretchLayoutManager(getWidth() - 2,
                getHeight() - 2));

        int row = 1;
        int col = 37;

        // Add some widgets
        addLabel(i18n.getString("messageBoxLabel"), 1, row);
        TButton first = addButton(i18n.getString("messageBoxButton"), col, row,
            new TAction() {
                public void DO() {
                    new DemoMsgBoxWindow(getApplication());
                }
            }
        );
        row += 2;

        addLabel(i18n.getString("openModalLabel"), 1, row);
        addButton(i18n.getString("openModalButton"), col, row,
            new TAction() {
                public void DO() {
                    new DemoMainWindow(getApplication(), MODAL);
                }
            }
        );
        row += 2;

        addLabel(i18n.getString("textFieldLabel"), 1, row);
        addButton(i18n.getString("textFieldButton"), col, row,
            new TAction() {
                public void DO() {
                    new DemoTextFieldWindow(getApplication());
                }
            }
        );
        row += 2;

        addLabel(i18n.getString("radioButtonLabel"), 1, row);
        addButton(i18n.getString("radioButtonButton"), col, row,
            new TAction() {
                public void DO() {
                    DemoCheckBoxWindow window;
                    window = new DemoCheckBoxWindow(getApplication());
                    TMenuItem menuItem = getApplication().getMenuItem(10010);
                    window.setUseGradient(menuItem.isChecked());
                }
            }
        );
        row += 2;

        addLabel(i18n.getString("editorLabel"), 1, row);
        addButton(i18n.getString("editorButton1"), col, row,
            new TAction() {
                public void DO() {
                    new DemoEditorWindow(getApplication());
                }
            }
        );
        addButton(i18n.getString("editorButton2"), col + 13, row,
            new TAction() {
                public void DO() {
                    new TEditorWindow(getApplication());
                }
            }
        );
        row += 2;

        addLabel(i18n.getString("textAreaLabel"), 1, row);
        addButton(i18n.getString("textAreaButton"), col, row,
            new TAction() {
                public void DO() {
                    new DemoTextWindow(getApplication());
                }
            }
        );
        row += 2;

        addLabel(i18n.getString("ttableLabel"), 1, row);
        addButton(i18n.getString("ttableButton1"), col, row,
            new TAction() {
                public void DO() {
                    new DemoTableWindow(getApplication(),
                        i18n.getString("tableWidgetDemo"));
                }
            }
        );
        addButton(i18n.getString("ttableButton2"), col + 13, row,
            new TAction() {
                public void DO() {
                    new TTableWindow(getApplication(),
                        i18n.getString("tableDemo"));
                }
            }
        );
        row += 2;

        addLabel(i18n.getString("treeViewLabel"), 1, row);
        addButton(i18n.getString("treeViewButton"), col, row,
            new TAction() {
                public void DO() {
                    try {
                        new DemoTreeViewWindow(getApplication());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        );
        row += 2;

        addLabel(i18n.getString("terminalLabel"), 1, row);
        addButton(i18n.getString("terminalButton"), col, row,
            () -> getApplication().openTerminal(0, 0));
        row += 2;

        addLabel(i18n.getString("colorAndStyleEditorLabel"), 1, row);
        addButton(i18n.getString("colorEditorButton"), col, row,
            () -> new TEditColorThemeWindow(getApplication()));
        addButton(i18n.getString("styleEditorButton"), col + 13, row,
            () -> new TEditDesktopStyleWindow(getApplication()));

        row = 15;
        progressBar1 = addProgressBar(col + 13, row, 12, 0, true);
        row++;
        timerLabel = addLabel(i18n.getString("timerLabel"), col + 13, row);
        timer1 = getApplication().addTimer(250, true,
            new TAction() {

                public void DO() {
                    timerLabel.setLabel(String.format(i18n.
                            getString("timerText"), timer1I));
                    timerLabel.setWidth(timerLabel.getLabel().length());
                    if (timer1I < 100) {
                        timer1I++;
                    } else {
                        timer1.setRecurring(false);
                    }
                    progressBar1.setValue(timer1I);
                }
            }
        );

        row++;
        progressBar2 = addProgressBar(col + 13, row, 12, 0, true);
        progressBar2.setLeftBorderChar('\u255e');
        progressBar2.setRightBorderChar('\u2561');
        progressBar2.setCompletedChar('\u2592');
        progressBar2.setRemainingChar('\u2550');
        row += 2;
        timer2 = getApplication().addTimer(125, true,
            new TAction() {

                public void DO() {
                    if (timer2I < 100) {
                        timer2I++;
                    } else {
                        timer2.setRecurring(false);
                    }
                    progressBar2.setValue(timer2I);
                }
            }
        );

        row += 2;

        addLabel("Exception management", 1, row);
        addButton("Exception", col, row,
            new TAction() {
                public void DO() {
                    try {
                        throw new RuntimeException("FUBAR'd!");
                    } catch (Exception e) {
                        new casciian.TExceptionDialog(getApplication(), e);
                    }
                }
            }
        );

        activate(first);

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

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * We need to override onClose so that the timer will no longer be called
     * after we close the window.  TTimers currently are completely unaware
     * of the rest of the UI classes.
     */
    @Override
    public void onClose() {
        getApplication().removeTimer(timer1);
        getApplication().removeTimer(timer2);
    }

    /**
     * Method that subclasses can override to handle posted command events.
     *
     * @param command command event
     */
    @Override
    public void onCommand(final TCommandEvent command) {
        if (command.equals(cmOpen)) {
            try {
                String filename = fileOpenBox(".");
                if (filename != null) {
                    try {
                        new TEditorWindow(getApplication(),
                            new File(filename));
                    } catch (IOException e) {
                        messageBox(i18n.getString("errorTitle"),
                            MessageFormat.format(i18n.
                                getString("errorReadingFile"), e.getMessage()));
                    }
                }
            } catch (IOException e) {
                        messageBox(i18n.getString("errorTitle"),
                            MessageFormat.format(i18n.
                                getString("errorOpeningFile"), e.getMessage()));
            }
            return;
        }

        // Didn't handle it, let children get it instead
        super.onCommand(command);
    }

    /**
     * Enable or disable a pre-defined gradient for this window's color.
     *
     * @param useGradient if true, paint this window with a gradient
     */
    public void setUseGradient(final boolean useGradient) {
        if (useGradient) {
            int pink = 0xf7a8b8;
            int blue = 0x55cdfc;
            int yellow = 0xffff00;
            int orange = 0xffc800;

            setDrawPreTransform(new GradientCellTransform(
                GradientCellTransform.Layer.BACKGROUND, blue, pink,
                yellow, blue), true);

            if (SystemProperties.isTextMouse()) {
                setDrawPostTransform(new MouseGlowCellTransform(
                    MouseGlowCellTransform.Layer.BOTH, orange, 7));
            } else {
                setDrawPostTransform(null);
            }
        } else {
            setDrawPreTransform(null);
            setDrawPostTransform(null);
        }
    }

}
