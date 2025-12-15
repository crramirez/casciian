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
import java.util.List;
import java.util.ResourceBundle;

import casciian.bits.StringUtils;
import casciian.event.TKeypressEvent;
import static casciian.TKeypress.*;

/**
 * TMessageBox is a system-modal dialog with buttons for OK, Cancel, Yes, or
 * No.  Call it like:
 *
 * <pre>
 * {@code
 *     box = messageBox(title, caption,
 *         TMessageBox.Type.OK | TMessageBox.Type.CANCEL);
 *
 *     if (box.getResult() == TMessageBox.OK) {
 *         ... the user pressed OK, do stuff ...
 *     }
 * }
 * </pre>
 *
 */
public class TMessageBox extends TWindow {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Message boxes have these supported types.
     */
    public enum Type {
        /**
         * Show an OK button.
         */
        OK,

        /**
         * Show both OK and Cancel buttons.
         */
        OKCANCEL,

        /**
         * Show both Yes and No buttons.
         */
        YESNO,

        /**
         * Show Yes, No, and Cancel buttons.
         */
        YESNOCANCEL
    };

    /**
     * Message boxes have these possible results.
     */
    public enum Result {
        /**
         * User clicked "OK".
         */
        OK,

        /**
         * User clicked "Cancel".
         */
        CANCEL,

        /**
         * User clicked "Yes".
         */
        YES,

        /**
         * User clicked "No".
         */
        NO
    };

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * The type of this message box.
     */
    private Type type;

    /**
     * My buttons.
     */
    private List<TButton> buttons;

    /**
     * Which button was clicked: OK, CANCEL, YES, or NO.
     */
    protected Result result = Result.OK;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.  The message box will be centered on screen.
     *
     * @param application TApplication that manages this window
     * @param title window title, will be centered along the top border
     * @param caption message to display.  Use embedded newlines to get a
     * multi-line box.
     */
    public TMessageBox(final TApplication application, final String title,
        final String caption) {

        this(application, title, caption, Type.OK, true);
    }

    /**
     * Public constructor.  The message box will be centered on screen.
     *
     * @param application TApplication that manages this window
     * @param title window title, will be centered along the top border
     * @param caption message to display.  Use embedded newlines to get a
     * multi-line box.
     * @param type one of the Type constants.  Default is Type.OK.
     */
    public TMessageBox(final TApplication application, final String title,
        final String caption, final Type type) {

        this(application, title, caption, type, true);
    }

    /**
     * Public constructor.  The message box will be centered on screen.
     *
     * @param application TApplication that manages this window
     * @param title window title, will be centered along the top border
     * @param caption message to display.  Use embedded newlines to get a
     * multi-line box.
     * @param type one of the Type constants.  Default is Type.OK.
     * @param yield if true, yield this Thread.  Subclasses need to set this
     * to false and yield at their end of their constructor intead.
     */
    @SuppressWarnings("this-escape")
    protected TMessageBox(final TApplication application, final String title,
        final String caption, final Type type, final boolean yield) {

        // Start as 100x100 at (1, 1).  These will be changed later.
        super(application, title, 1, 1, 100, 100, CENTERED | MODAL);

        i18n = ResourceBundle.getBundle(TMessageBox.class.getName(),
            getLocale());

        // Hang onto type so that we can provide more convenience in
        // onKeypress().
        this.type = type;

        // Determine width and height
        String [] lines = caption.split("\n");
        int width = StringUtils.width(title) + 12;
        setHeight(6 + lines.length);
        for (String line: lines) {
            if (StringUtils.width(line) + 4 > width) {
                width = StringUtils.width(line) + 4;
            }
        }
        setWidth(width);
        if (getWidth() > getScreen().getWidth()) {
            setWidth(getScreen().getWidth());
        }
        // Re-center window to get an appropriate (x, y)
        center();

        // Now add my elements
        int lineI = 1;
        for (String line: lines) {
            addLabel(line, 1, lineI, "twindow.background.modal");
            lineI++;
        }

        // The button line
        lineI++;
        buttons = new ArrayList<TButton>();

        int buttonX = 0;

        // Setup button actions
        switch (type) {

        case OK:
            result = Result.OK;
            if (getWidth() < 15) {
                setWidth(15);
            }
            buttonX = (getWidth() - 11) / 2;
            buttons.add(addButton(i18n.getString("okButton"), buttonX, lineI,
                    new TAction() {
                        public void DO() {
                            result = Result.OK;
                            getApplication().closeWindow(TMessageBox.this);
                        }
                    }
                )
            );
            break;

        case OKCANCEL:
            result = Result.CANCEL;
            if (getWidth() < 26) {
                setWidth(26);
            }
            buttonX = (getWidth() - 22) / 2;
            buttons.add(addButton(i18n.getString("okButton"), buttonX, lineI,
                    new TAction() {
                        public void DO() {
                            result = Result.OK;
                            getApplication().closeWindow(TMessageBox.this);
                        }
                    }
                )
            );
            buttonX += 8 + 4;
            buttons.add(addButton(i18n.getString("cancelButton"), buttonX, lineI,
                    new TAction() {
                        public void DO() {
                            result = Result.CANCEL;
                            getApplication().closeWindow(TMessageBox.this);
                        }
                    }
                )
            );
            break;

        case YESNO:
            result = Result.NO;
            if (getWidth() < 20) {
                setWidth(20);
            }
            buttonX = (getWidth() - 16) / 2;
            buttons.add(addButton(i18n.getString("yesButton"), buttonX, lineI,
                    new TAction() {
                        public void DO() {
                            result = Result.YES;
                            getApplication().closeWindow(TMessageBox.this);
                        }
                    }
                )
            );
            buttonX += 5 + 4;
            buttons.add(addButton(i18n.getString("noButton"), buttonX, lineI,
                    new TAction() {
                        public void DO() {
                            result = Result.NO;
                            getApplication().closeWindow(TMessageBox.this);
                        }
                    }
                )
            );
            break;

        case YESNOCANCEL:
            result = Result.CANCEL;
            if (getWidth() < 31) {
                setWidth(31);
            }
            buttonX = (getWidth() - 27) / 2;
            buttons.add(addButton(i18n.getString("yesButton"), buttonX, lineI,
                    new TAction() {
                        public void DO() {
                            result = Result.YES;
                            getApplication().closeWindow(TMessageBox.this);
                        }
                    }
                )
            );
            buttonX += 5 + 4;
            buttons.add(addButton(i18n.getString("noButton"), buttonX, lineI,
                    new TAction() {
                        public void DO() {
                            result = Result.NO;
                            getApplication().closeWindow(TMessageBox.this);
                        }
                    }
                )
            );
            buttonX += 4 + 4;
            buttons.add(addButton(i18n.getString("cancelButton"), buttonX,
                    lineI,
                    new TAction() {
                        public void DO() {
                            result = Result.CANCEL;
                            getApplication().closeWindow(TMessageBox.this);
                        }
                    }
                )
            );
            break;

        default:
            throw new IllegalArgumentException("Invalid message box type: " +
                type);
        }

        if (yield) {
            // Set the secondaryThread to run me
            getApplication().enableSecondaryEventReceiver(this);

            // Yield to the secondary thread.  When I come back from the
            // constructor response will already be set.
            getApplication().yield();
        }
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {

        if (this instanceof TInputBox) {
            super.onKeypress(keypress);
            return;
        }

        String keyStr = keypress.getKey().toString().toLowerCase();

        // Some convenience for message boxes: Alt is optional for the
        // buttons.
        switch (type) {

        case OK:
            if (keyStr.equals(i18n.getString("kbO"))
                || keyStr.equals(i18n.getString("kbAltO"))
            ) {
                buttons.get(0).dispatch();
                return;
            }
            break;

        case OKCANCEL:
            if (keyStr.equals(i18n.getString("kbO"))
                || keyStr.equals(i18n.getString("kbAltO"))
            ) {
                buttons.get(0).dispatch();
                return;
            } else if (keyStr.equals(i18n.getString("kbC"))
                || keyStr.equals(i18n.getString("kbAltC"))
            ) {
                buttons.get(1).dispatch();
                return;
            }
            break;

        case YESNO:
            if (keyStr.equals(i18n.getString("kbY"))
                || keyStr.equals(i18n.getString("kbAltY"))
            ) {
                buttons.get(0).dispatch();
                return;
            } else if (keyStr.equals(i18n.getString("kbN"))
                || keyStr.equals(i18n.getString("kbAltN"))
            ) {
                buttons.get(1).dispatch();
                return;
            }
            break;

        case YESNOCANCEL:
            if (keyStr.equals(i18n.getString("kbY"))
                || keyStr.equals(i18n.getString("kbAltY"))
            ) {
                buttons.get(0).dispatch();
                return;
            } else if (keyStr.equals(i18n.getString("kbN"))
                || keyStr.equals(i18n.getString("kbAltN"))
            ) {
                buttons.get(1).dispatch();
                return;
            } else if (keyStr.equals(i18n.getString("kbC"))
                || keyStr.equals(i18n.getString("kbAltC"))
            ) {
                buttons.get(2).dispatch();
                return;
            }
            break;

        default:
            throw new IllegalArgumentException("Invalid message box type: " +
                type);
        }

        super.onKeypress(keypress);
    }

    // ------------------------------------------------------------------------
    // TMessageBox ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the result.
     *
     * @return the result: OK, CANCEL, YES, or NO.
     */
    public final Result getResult() {
        return result;
    }

    /**
     * See if the user clicked YES.
     *
     * @return true if the user clicked YES
     */
    public final boolean isYes() {
        return (result == Result.YES);
    }

    /**
     * See if the user clicked NO.
     *
     * @return true if the user clicked NO
     */
    public final boolean isNo() {
        return (result == Result.NO);
    }

    /**
     * See if the user clicked OK.
     *
     * @return true if the user clicked OK
     */
    public final boolean isOk() {
        return (result == Result.OK);
    }

    /**
     * See if the user clicked CANCEL.
     *
     * @return true if the user clicked CANCEL
     */
    public final boolean isCancel() {
        return (result == Result.CANCEL);
    }

}
