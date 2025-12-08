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

/**
 * TInputBox is a system-modal dialog with an OK button and a text input
 * field.  Call it like:
 *
 * <pre>
 * {@code
 *     box = inputBox(title, caption);
 *     if (box.getText().equals("yes")) {
 *         ... the user entered "yes", do stuff ...
 *     }
 * }
 * </pre>
 *
 */
public class TInputBox extends TMessageBox {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The input field.
     */
    private TField field;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.  The input box will be centered on screen.
     *
     * @param application TApplication that manages this window
     * @param title window title, will be centered along the top border
     * @param caption message to display.  Use embedded newlines to get a
     * multi-line box.
     */
    public TInputBox(final TApplication application, final String title,
        final String caption) {

        this(application, title, caption, "", Type.OK);
    }

    /**
     * Public constructor.  The input box will be centered on screen.
     *
     * @param application TApplication that manages this window
     * @param title window title, will be centered along the top border
     * @param caption message to display.  Use embedded newlines to get a
     * multi-line box.
     * @param text initial text to seed the field with
     */
    public TInputBox(final TApplication application, final String title,
        final String caption, final String text) {

        this(application, title, caption, text, Type.OK);
    }

    /**
     * Public constructor.  The input box will be centered on screen.
     *
     * @param application TApplication that manages this window
     * @param title window title, will be centered along the top border
     * @param caption message to display.  Use embedded newlines to get a
     * multi-line box.
     * @param text initial text to seed the field with
     * @param type one of the Type constants.  Default is Type.OK.
     */
    @SuppressWarnings("this-escape")
    public TInputBox(final TApplication application, final String title,
        final String caption, final String text, final Type type) {

        super(application, title, caption, type, false);

        for (TWidget widget: getChildren()) {
            if (widget instanceof TButton) {
                widget.setY(widget.getY() + 2);
            }
        }

        setHeight(getHeight() + 2);
        field = addField(1, getHeight() - 6, getWidth() - 4, false, text,
            new TAction() {
                public void DO() {
                    switch (type) {
                    case OK:
                        result = Result.OK;
                        getApplication().closeWindow(TInputBox.this);
                        return;

                    case OKCANCEL:
                        result = Result.OK;
                        getApplication().closeWindow(TInputBox.this);
                        return;

                    case YESNO:
                        result = Result.YES;
                        getApplication().closeWindow(TInputBox.this);
                        return;

                    case YESNOCANCEL:
                        result = Result.YES;
                        getApplication().closeWindow(TInputBox.this);
                        return;
                    }
                }
            }, null);

        // Set the secondaryThread to run me
        getApplication().enableSecondaryEventReceiver(this);

        // Yield to the secondary thread.  When I come back from the
        // constructor response will already be set.
        getApplication().yield();
    }

    // ------------------------------------------------------------------------
    // TMessageBox ------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // TInputBox --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Retrieve the answer text.
     *
     * @return the answer text
     */
    public String getText() {
        return field.getText();
    }

}
