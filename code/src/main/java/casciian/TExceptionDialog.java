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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.ResourceBundle;

import casciian.bits.CellAttributes;
import casciian.layout.AnchoredLayoutManager;

/**
 * TExceptionDialog displays an exception and its stack trace to the user,
 * and provides a means to save a troubleshooting report for support.
 */
public class TExceptionDialog extends TWindow {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * The exception.  We will actually make it Throwable, for the unlikely
     * event we catch an Error rather than an Exception.
     */
    private Throwable exception;

    /**
     * The exception's stack trace.
     */
    private TList stackTrace;

    /**
     * The exception string label.
     */
    private TLabel exceptionString;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param application TApplication that manages this window
     * @param exception the exception to display
     */
    @SuppressWarnings("this-escape")
    public TExceptionDialog(final TApplication application,
        final Throwable exception) {

        super(application, "", 1, 1, 78, 22, CENTERED | RESIZABLE | MODAL);
        i18n = ResourceBundle.getBundle(TExceptionDialog.class.getName(),
            getLocale());
        setTitle(i18n.getString("windowTitle"));

        setMinimumWindowWidth(getWidth());
        setMinimumWindowHeight(getHeight());
        AnchoredLayoutManager layout;
        layout = new AnchoredLayoutManager(getWidth() - 2, getHeight() - 2);
        setLayoutManager(layout);

        this.exception = exception;

        TText caption = addText(i18n.getString("captionText"), 2, 1,
            getWidth() - 6, 5,
            "twindow.background.modal");

        exceptionString = addLabel(MessageFormat.format(
            i18n.getString("exceptionString"),
                exception.getClass().getName(), exception.getMessage()),
            2, 8, "ttext", false);

        ArrayList<String> stackTraceStrings = new ArrayList<String>();
        stackTraceStrings.add(exception.getMessage());
        StackTraceElement [] stack = exception.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            stackTraceStrings.add(stack[i].toString());
        }
        stackTrace = addList(stackTraceStrings, 2, 9, getWidth() - 6, 7);
        layout.setAnchor(stackTrace, caption,
            AnchoredLayoutManager.Anchor.TOP);

        layout.setAnchor(exceptionString, stackTrace,
            AnchoredLayoutManager.Anchor.BOTTOM);

        // Buttons
        TButton saveButton = addButton(i18n.getString("saveButton"),
            17, getHeight() - 4,
            new TAction() {
                public void DO() {
                    saveToFile();
                }
            });
        layout.setAnchor(saveButton, null,
            AnchoredLayoutManager.Anchor.BOTTOM_RIGHT);

        TButton closeButton = addButton(i18n.getString("closeButton"),
            41, getHeight() - 4,
            new TAction() {
                public void DO() {
                    // Don't do anything, just close the window.
                    TExceptionDialog.this.close();
                }
            });
        layout.setAnchor(closeButton, null,
            AnchoredLayoutManager.Anchor.BOTTOM_RIGHT);

        // Save this for last: make the close button default action.
        activate(closeButton);
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the exception message background.
     */
    @Override
    public void draw() {
        // Draw window and border.
        super.draw();

        CellAttributes boxColor = getTheme().getColor("ttext");
        hLineXY(exceptionString.getX() + 1, exceptionString.getY() + 1,
            stackTrace.getWidth(), ' ', boxColor);
    }

    // ------------------------------------------------------------------------
    // TExceptionDialog -------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Save a troubleshooting report to file.  Note that we do NOT translate
     * the strings within the error report.
     */
    private void saveToFile() {
        // Prompt for filename.
        PrintWriter writer = null;
        try {
            String filename = fileSaveBox(".");
            if (filename == null) {
                // User cancelled, bail out.
                return;
            }
            writer = new PrintWriter(new FileWriter(filename));
            writer.write("Date: " + new Date(System.currentTimeMillis())
                + "\n");

            // System properties
            writer.write("System properties:\n");
            writer.write("-----------------------------------\n");
            System.getProperties().store(writer, null);
            writer.write("-----------------------------------\n");
            writer.write("\n");

            // The exception we caught
            writer.write("Caught exception:\n");
            writer.write("-----------------------------------\n");
            exception.printStackTrace(writer);
            writer.write("-----------------------------------\n");
            writer.write("\n");
            // The exception's cause, if it was set
            if (exception.getCause() != null) {
                writer.write("Caught exception's cause:\n");
                writer.write("-----------------------------------\n");
                exception.getCause().printStackTrace(writer);
                writer.write("-----------------------------------\n");
            }
            writer.write("\n");

            // The UI stack trace
            writer.write("UI stack trace:\n");
            writer.write("-----------------------------------\n");
            (new Throwable("UI Thread")).printStackTrace(writer);
            writer.write("-----------------------------------\n");
            writer.write("\n");
            writer.close();
        } catch (IOException e) {
            messageBox(i18n.getString("errorDialogTitle"),
                MessageFormat.format(i18n.
                    getString("errorSavingFile"), e.getMessage()));
        } finally {
            if (writer != null) {
                writer.close();
                writer = null;
            }
        }
    }
}
