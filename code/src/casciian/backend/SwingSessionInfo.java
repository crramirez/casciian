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
package casciian.backend;

import java.awt.Insets;

/**
 * SwingSessionInfo provides a session implementation with a callback into
 * Swing to support queryWindowSize().  The username is blank, language is
 * "en_US", with a 80x25 text window.
 */
public class SwingSessionInfo implements SessionInfo {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The Swing JFrame or JComponent.
     */
    private SwingComponent swing;

    /**
     * The width of a text cell in pixels.
     */
    private int textWidth = 10;

    /**
     * The height of a text cell in pixels.
     */
    private int textHeight = 10;

    /**
     * User name.
     */
    private String username = "";

    /**
     * Language.
     */
    private String language = "en_US";

    /**
     * Text window width.
     */
    private int windowWidth = 80;

    /**
     * Text window height.
     */
    private int windowHeight = 25;

    /**
     * The time this session was started.
     */
    private long startTime = System.currentTimeMillis();

    /**
     * The number of seconds since the last user input event from this
     * session.
     */
    private int idleTime = Integer.MAX_VALUE;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param swing the Swing JFrame or JComponent
     * @param textWidth the width of a cell in pixels
     * @param textHeight the height of a cell in pixels
     */
    public SwingSessionInfo(final SwingComponent swing, final int textWidth,
        final int textHeight) {

        this.swing      = swing;
        this.textWidth  = textWidth;
        this.textHeight = textHeight;
    }

    /**
     * Public constructor.
     *
     * @param swing the Swing JFrame or JComponent
     * @param textWidth the width of a cell in pixels
     * @param textHeight the height of a cell in pixels
     * @param width the number of columns
     * @param height the number of rows
     */
    public SwingSessionInfo(final SwingComponent swing, final int textWidth,
        final int textHeight, final int width, final int height) {

        this.swing              = swing;
        this.textWidth          = textWidth;
        this.textHeight         = textHeight;
        this.windowWidth        = width;
        this.windowHeight       = height;
    }

    // ------------------------------------------------------------------------
    // SessionInfo ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the time this session was started.
     *
     * @return the number of millis since midnight, January 1, 1970 UTC
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Get the time this session was idle.
     *
     * @return the number of seconds since the last user input event from
     * this session
     */
    public int getIdleTime() {
        return idleTime;
    }

    /**
     * Set the time this session was idle.
     *
     * @param seconds the number of seconds since the last user input event
     * from this session
     */
    public void setIdleTime(final int seconds) {
        idleTime = seconds;
    }

    /**
     * Username getter.
     *
     * @return the username
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Username setter.
     *
     * @param username the value
     */
    public void setUsername(final String username) {
        this.username = username;
    }

    /**
     * Language getter.
     *
     * @return the language
     */
    public String getLanguage() {
        return this.language;
    }

    /**
     * Language setter.
     *
     * @param language the value
     */
    public void setLanguage(final String language) {
        this.language = language;
    }

    /**
     * Text window width getter.
     *
     * @return the window width
     */
    public int getWindowWidth() {
        return windowWidth;
    }

    /**
     * Text window height getter.
     *
     * @return the window height
     */
    public int getWindowHeight() {
        return windowHeight;
    }

    /**
     * Re-query the text window size.
     */
    public void queryWindowSize() {
        Insets insets = swing.getInsets();
        int width = swing.getWidth() - insets.left - insets.right;
        int height = swing.getHeight() - insets.top - insets.bottom;
        // In theory, if Java reported pixel-perfect dimensions, the
        // expressions above would precisely line up with the requested
        // window size from SwingComponent.setDimensions().  In practice,
        // there appears to be a small difference.  Add half a text cell in
        // both directions before the division to hopefully reach the same
        // result as setDimensions() was supposed to give us.
        width += (textWidth / 2);
        height += (textHeight / 2);
        windowWidth = width / textWidth;
        windowHeight = height / textHeight;

        /*
        System.err.printf("queryWindowSize(): frame %d %d window %d %d\n",
            swing.getWidth(), swing.getHeight(),
            windowWidth, windowHeight);
        */
    }

    // ------------------------------------------------------------------------
    // SwingSessionInfo -------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set the dimensions of a single text cell.
     *
     * @param textWidth the width of a cell in pixels
     * @param textHeight the height of a cell in pixels
     */
    public void setTextCellDimensions(final int textWidth,
        final int textHeight) {

        this.textWidth  = textWidth;
        this.textHeight = textHeight;
    }

    /**
     * Getter for the underlying Swing component.
     *
     * @return the SwingComponent
     */
    public SwingComponent getSwingComponent() {
        return swing;
    }

}
