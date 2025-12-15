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
import java.util.ResourceBundle;

import casciian.TAction;
import casciian.TApplication;
import casciian.TInputBox;
import casciian.TMessageBox;
import casciian.TWindow;
import casciian.layout.StretchLayoutManager;
import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * This window demonstates the TMessageBox and TInputBox widgets.
 */
public class DemoMsgBoxWindow extends TWindow {

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * Constructor.
     *
     * @param parent the main application
     */
    DemoMsgBoxWindow(final TApplication parent) {
        this(parent, TWindow.CENTERED | TWindow.RESIZABLE);
    }

    /**
     * Constructor.
     *
     * @param parent the main application
     * @param flags bitmask of MODAL, CENTERED, or RESIZABLE
     */
    DemoMsgBoxWindow(final TApplication parent, final int flags) {
        // Construct a demo window.  X and Y don't matter because it
        // will be centered on screen.
        super(parent, "", 0, 0, 79, 18, flags);
        i18n = ResourceBundle.getBundle(DemoMsgBoxWindow.class.getName(),
            getLocale());
        setTitle(i18n.getString("windowTitle"));

        setLayoutManager(new StretchLayoutManager(getWidth() - 2,
                getHeight() - 2));

        int row = 1;
        int col = 45;

        // Add some widgets
        addLabel(i18n.getString("messageBoxLabel1"), 1, row);
        addButton(i18n.getString("messageBoxButton1"), col, row,
            new TAction() {
                public void DO() {
                    getApplication().messageBox(i18n.
                        getString("messageBoxTitle1"),
                        i18n.getString("messageBoxPrompt1"),
                        TMessageBox.Type.OK);
                }
            }
        );
        row += 2;

        addLabel(i18n.getString("messageBoxLabel2"), 1, row);
        addButton(i18n.getString("messageBoxButton2"), col, row,
            new TAction() {
                public void DO() {
                    getApplication().messageBox(i18n.
                        getString("messageBoxTitle2"),
                        i18n.getString("messageBoxPrompt2"),
                        TMessageBox.Type.OKCANCEL);
                }
            }
        );
        row += 2;

        addLabel(i18n.getString("messageBoxLabel3"), 1, row);
        addButton(i18n.getString("messageBoxButton3"), col, row,
            new TAction() {
                public void DO() {
                    getApplication().messageBox(i18n.
                        getString("messageBoxTitle3"),
                        i18n.getString("messageBoxPrompt3"),
                        TMessageBox.Type.YESNO);
                }
            }
        );
        row += 2;

        addLabel(i18n.getString("messageBoxLabel4"), 1, row);
        addButton(i18n.getString("messageBoxButton4"), col, row,
            new TAction() {
                public void DO() {
                    getApplication().messageBox(i18n.
                        getString("messageBoxTitle4"),
                        i18n.getString("messageBoxPrompt4"),
                        TMessageBox.Type.YESNOCANCEL);
                }
            }
        );
        row += 2;

        addLabel(i18n.getString("inputBoxLabel1"), 1, row);
        addButton(i18n.getString("inputBoxButton1"), col, row,
            new TAction() {
                public void DO() {
                    TInputBox in = getApplication().inputBox(i18n.
                        getString("inputBoxTitle1"),
                        i18n.getString("inputBoxPrompt1"),
                        i18n.getString("inputBoxInput1"));
                    getApplication().messageBox(i18n.
                        getString("inputBoxAnswerTitle1"),
                        MessageFormat.format(i18n.
                            getString("inputBoxAnswerPrompt1"), in.getText()));
                }
            }
        );
        row += 2;

        addLabel(i18n.getString("inputBoxLabel2"), 1, row);
        addButton(i18n.getString("inputBoxButton2"), col, row,
            new TAction() {
                public void DO() {
                    TInputBox in = getApplication().inputBox(i18n.
                        getString("inputBoxTitle2"),
                        i18n.getString("inputBoxPrompt2"),
                        i18n.getString("inputBoxInput2"),
                        TInputBox.Type.OKCANCEL);
                    getApplication().messageBox(i18n.
                        getString("inputBoxAnswerTitle2"),
                        MessageFormat.format(i18n.
                            getString("inputBoxAnswerPrompt2"), in.getText(),
                            in.getResult()));
                }
            }
        );
        row += 2;

        addButton(i18n.getString("closeWindow"),
            (getWidth() - 14) / 2, getHeight() - 4,
            new TAction() {
                public void DO() {
                    getApplication().closeWindow(DemoMsgBoxWindow.this);
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
}
