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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import casciian.backend.ECMA48Terminal;
import casciian.backend.MultiScreen;
import casciian.bits.Clipboard;
import casciian.event.TKeypressEvent;
import static casciian.TKeypress.*;

/**
 * TTerminalInformationWindow displays some details about the running
 * environment and terminal.
 */
public class TTerminalInformationWindow extends TWindow {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * The text to copy to the clipboard.
     */
    private String copyText = "";

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.  The window will be centered on screen.
     *
     * @param application the TApplication that manages this window
     */
    @SuppressWarnings("this-escape")
    public TTerminalInformationWindow(final TApplication application) {

        // Register with the TApplication
        super(application, "", 0, 0, 70, 17, MODAL);
        i18n = ResourceBundle.getBundle(TTerminalInformationWindow.class.getName(),
            getLocale());
        setTitle(i18n.getString("windowTitle"));

        // Add shortcut text
        newStatusBar(i18n.getString("statusBar"));

        final Clipboard clipboard = application.getClipboard();

        ECMA48Terminal ecmaTerminal = null;
        MultiScreen multiScreen = null;

        if (getScreen() instanceof ECMA48Terminal) {
            ecmaTerminal = (ECMA48Terminal) getScreen();
        }
        if (getScreen() instanceof MultiScreen) {
            multiScreen = (MultiScreen) getScreen();
        }

        int labelColumn = 1;
        int infoColumn = 32;
        int row = 1;
        TLabel label = addLabel(i18n.getString("backendType"), labelColumn, row);
        copyText += label.getLabel() + " ";
        if (ecmaTerminal != null) {
            label = addLabel("EMCA48/Xterm", infoColumn, row, "ttext", false);
        }
        if (multiScreen != null) {
            label = addLabel("MultiScreen", infoColumn, row, "ttext", false);
        }
        copyText += label.getLabel() + "\n";

        row++;

        row++;

        label = addLabel(i18n.getString("screenDimensions"), labelColumn, row);
        copyText += label.getLabel() + " ";
        label = addLabel(String.format("%d x %d", getScreen().getWidth(),
                getScreen().getHeight()), infoColumn, row, "ttext", false);
        copyText += label.getLabel() + "\n";
        row++;

        row++;

        label = addLabel(i18n.getString("xtversion"), labelColumn, row);
        copyText += label.getLabel() + " ";
        if (ecmaTerminal != null) {
            label = addLabel(ecmaTerminal.getTerminalVersion(),
                infoColumn, row, "ttext", false);
        }
        copyText += label.getLabel() + "\n";
        row++;

        label = addLabel("TERM:", labelColumn, row);
        copyText += label.getLabel() + " ";
        String envStr = System.getenv("TERM");
        label = addLabel(envStr == null ? "" : envStr,
            infoColumn, row, "ttext", false);
        copyText += label.getLabel() + "\n";
        row++;

        envStr = System.getenv("LANG");
        label = addLabel("LANG:", labelColumn, row);
        copyText += label.getLabel() + " ";
        label = addLabel(envStr == null ? "" : envStr,
            infoColumn, row, "ttext", false);
        copyText += label.getLabel() + "\n";
        row++;

        row++;

        label = addLabel(i18n.getString("javaVirtualMachine"), labelColumn,
            row);
        copyText += label.getLabel() + " ";
        label = addLabel(System.getProperty("java.vm.name"),
            infoColumn, row, "ttext", false);
        copyText += label.getLabel() + "\n";
        row++;

        label = addLabel(i18n.getString("operatingSystem"), labelColumn, row);
        copyText += label.getLabel() + " ";
        label = addLabel(String.format("%s %s", System.getProperty("os.name"),
                System.getProperty("os.version")),
            infoColumn, row, "ttext", false);
        copyText += label.getLabel() + "\n";
        row++;

        addButton(i18n.getString("copyButton"),
            6, getHeight() - 4,
            new TAction() {
                public void DO() {
                    // Copy text to clipboard.
                    clipboard.copyText(copyText);
                }
            });

        TButton closeButton = addButton(i18n.getString("closeButton"),
            getWidth() - 16, getHeight() - 4,
            new TAction() {
                public void DO() {
                    // Close window.
                    TTerminalInformationWindow.this.close();
                }
            });

        // Save this for last: make the close button default action.
        activate(closeButton);

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
        // Escape - behave like close
        if (keypress.equals(kbEsc)) {
            getApplication().closeWindow(this);
            return;
        }

        // Pass to my parent
        super.onKeypress(keypress);
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // TTerminalInformationWindow ---------------------------------------------
    // ------------------------------------------------------------------------

}
