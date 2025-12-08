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

import java.util.ResourceBundle;

import casciian.bits.CellAttributes;
import casciian.event.TResizeEvent;
import casciian.help.THelpText;
import casciian.help.Topic;

/**
 * THelpWindow
 */
public class THelpWindow extends TWindow {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // Default help topic keys.

    /**
     * "Help On Help".
     */
    public static String HELP_HELP                      = "Help On Help";

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * The help text window.
     */
    private THelpText helpText;

    /**
     * The "Contents" button.
     */
    private TButton contentsButton;

    /**
     * The "Index" button.
     */
    private TButton indexButton;

    /**
     * The "Previous" button.
     */
    private TButton previousButton;

    /**
     * The "Close" button.
     */
    private TButton closeButton;

    /**
     * The X position for the buttons.
     */
    private int buttonOffset = 14;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param application TApplication that manages this window
     * @param topic the topic to start on
     */
    public THelpWindow(final TApplication application, final String topic) {
        this(application, application.helpFile.getTopic(topic));
    }

    /**
     * Public constructor.
     *
     * @param application TApplication that manages this window
     * @param topic the topic to start on
     */
    @SuppressWarnings("this-escape")
    public THelpWindow(final TApplication application, final Topic topic) {
        super(application, "", 1, 1, 78, 22, CENTERED | RESIZABLE);

        i18n = ResourceBundle.getBundle(THelpWindow.class.getName(),
            getLocale());
        setTitle(i18n.getString("windowTitle"));

        setMinimumWindowHeight(16);
        setMinimumWindowWidth(30);

        helpText = new THelpText(this, topic, 1, 1,
            getWidth() - buttonOffset - 4, getHeight() - 4);

        setHelpTopic(topic);

        // Buttons
        previousButton = addButton(i18n.getString("previousButton"),
            getWidth() - buttonOffset, 4,
            new TAction() {
                public void DO() {
                    if (application.helpTopics.size() > 1) {
                        Topic previous = application.helpTopics.remove(
                            application.helpTopics.size() - 2);
                        application.helpTopics.remove(application.
                            helpTopics.size() - 1);
                        setHelpTopic(previous);
                    }
                }
            });

        contentsButton = addButton(i18n.getString("contentsButton"),
            getWidth() - buttonOffset, 6,
            new TAction() {
                public void DO() {
                    setHelpTopic(application.helpFile.getTableOfContents());
                }
            });

        indexButton = addButton(i18n.getString("indexButton"),
            getWidth() - buttonOffset, 8,
            new TAction() {
                public void DO() {
                    setHelpTopic(application.helpFile.getIndex());
                }
            });

        closeButton = addButton(i18n.getString("closeButton"),
            getWidth() - buttonOffset, 10,
            new TAction() {
                public void DO() {
                    // Don't copy anything, just close the window.
                    THelpWindow.this.close();
                }
            });

        // Save this for last: make the close button default action.
        activate(closeButton);

    }

    /**
     * Public constructor.
     *
     * @param application TApplication that manages this window
     */
    public THelpWindow(final TApplication application) {
        this(application, HELP_HELP);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle window/screen resize events.
     *
     * @param event resize event
     */
    @Override
    public void onResize(final TResizeEvent event) {
        if (event.getType() == TResizeEvent.Type.WIDGET) {

            previousButton.setX(getWidth() - buttonOffset);
            contentsButton.setX(getWidth() - buttonOffset);
            indexButton.setX(getWidth() - buttonOffset);
            closeButton.setX(getWidth() - buttonOffset);

            helpText.setDimensions(1, 1, getWidth() - buttonOffset - 4,
                getHeight() - 4);
            helpText.onResize(new TResizeEvent(event.getBackend(),
                    TResizeEvent.Type.WIDGET, helpText.getWidth(),
                    helpText.getHeight()));

            return;
        } else {
            super.onResize(event);
        }
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Retrieve the background color.
     *
     * @return the background color
     */
    @Override
    public final CellAttributes getBackground() {
        return getTheme().getColor("thelpwindow.background");
    }

    /**
     * Retrieve the border color.
     *
     * @return the border color
     */
    @Override
    public CellAttributes getBorder() {
        if (inWindowMove) {
            return getTheme().getColor("thelpwindow.windowmove");
        }
        return getTheme().getColor("thelpwindow.background");
    }

    /**
     * Retrieve the color used by the window movement/sizing controls.
     *
     * @return the color used by the zoom box, resize bar, and close box
     */
    @Override
    public CellAttributes getBorderControls() {
        return getTheme().getColor("thelpwindow.border");
    }

    // ------------------------------------------------------------------------
    // THelpWindow ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set the topic to display.
     *
     * @param topic the topic to display
     */
    public void setHelpTopic(final String topic) {
        setHelpTopic(getApplication().helpFile.getTopic(topic));
    }

    /**
     * Set the topic to display.
     *
     * @param topic the topic to display
     */
    private void setHelpTopic(final Topic topic) {
        boolean separator = true;
        if ((topic == getApplication().helpFile.getTableOfContents())
            || (topic == getApplication().helpFile.getIndex())
        ) {
            separator = false;
        }

        getApplication().helpTopics.add(topic);
        helpText.setTopic(topic, separator);
    }

}
