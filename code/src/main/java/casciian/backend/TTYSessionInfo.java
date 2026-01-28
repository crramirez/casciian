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

import java.io.PrintWriter;

import casciian.backend.terminal.Terminal;
import casciian.backend.terminal.TerminalJlineImpl;

/**
 * TTYSessionInfo queries environment variables and the tty window size for
 * the session information.  The username is taken from user.name, language
 * is taken from user.language, and text window size is delegated to the
 * Terminal implementation.
 */
public class TTYSessionInfo implements SessionInfo {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * User name.
     */
    private String username = "";

    /**
     * Language.
     */
    private String language = "";

    /**
     * Text window width.  Default is 80x24 (same as VT100-ish terminals).
     * Note package private access.
     */
    int windowWidth = 80;

    /**
     * Text window height.  Default is 80x24 (same as VT100-ish terminals).
     * Note package private access.
     */
    int windowHeight = 24;

    /**
     * Time at which the window size was refreshed.
     */
    private long lastQueryWindowTime;

    /**
     * Time at which we last did a fallback direct query for stty terminals
     * with CSI 18 t enabled. This is used as a defensive measure in case
     * CSI 8 t responses are not being received (e.g., due to ptypipe bugs).
     */
    volatile long lastFallbackQueryTime;

    /**
     * The time this session was started.
     */
    private final long startTime = System.currentTimeMillis();

    /**
     * The number of seconds since the last user input event from this
     * session.
     */
    private int idleTime = Integer.MAX_VALUE;

    /**
     * If set, this session can only use CSI 8 t to get window size.  Note
     * package private access.
     */
    @SuppressWarnings("java:S3077")
    volatile PrintWriter output = null;

    /**
     * The terminal to use for querying window size.
     */
    private final Terminal terminal;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor that receives a Terminal for window size queries.
     *
     * @param terminal the terminal to use for querying window size; may be null,
     *                 in which case window size queries will use default values
     */
    @SuppressWarnings("this-escape")
    public TTYSessionInfo(Terminal terminal) {
        this.terminal = terminal;
        // Populate lang and user from the environment
        username = System.getProperty("user.name");
        language = System.getProperty("user.language");
        queryWindowSize();
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
        if (lastQueryWindowTime == 0) {
            lastQueryWindowTime = System.currentTimeMillis();
        } else {
            long nowTime = System.currentTimeMillis();
            if (nowTime - lastQueryWindowTime < 1000) {
                // Don't re-query if it hasn't been a full second since the last time.
                return;
            }
            lastQueryWindowTime = nowTime;
        }

        // Also send CSI 18 t if the terminal supports it.
        // The response (CSI 8 t) will update windowWidth/windowHeight
        // via ECMA48Terminal when it arrives.
        if (output != null) {
            output.write(ECMA48Terminal.xtermQueryWindowSize());
            output.flush();
        }

        // Determine if we should query the terminal directly.
        // JLine tracks SIGWINCH automatically, so getWindowWidth/Height() is cheap.
        // For stty (TerminalShImpl), each query spawns a process, which is expensive.
        // When CSI 18 t mode is enabled (output != null), the terminal supports
        // CSI 18 t queries, and we rely on CSI 8 t responses for size updates.
        // However, when running inside ptypipe, CSI 8 t responses are stripped,
        // so we need to query JLine (which tracks SIGWINCH) to catch those resizes.
        boolean useDirectQuery = false;
        boolean useFallbackQuery = false;
        if (terminal != null) {
            if (terminal instanceof TerminalJlineImpl) {
                // JLine tracks SIGWINCH automatically, always query it (cheap)
                useDirectQuery = true;
            } else if (output == null) {
                // CSI 18 t not enabled, must use direct query
                useDirectQuery = true;
            } else {
                // CSI 18 t mode enabled for stty terminal.
                // As a defensive measure, periodically query via stty anyway
                // in case CSI 8 t responses are not being received (e.g., due to
                // ptypipe bugs that strip the responses).
                long nowTime = System.currentTimeMillis();
                if (nowTime - lastFallbackQueryTime >= 3000) {
                    // Do a fallback stty query every 3 seconds
                    useFallbackQuery = true;
                    lastFallbackQueryTime = nowTime;
                    output = null; // If we don't receive CSI 18 t anymore, we'll again spam stty
                }
            }
        }

        if ((useDirectQuery || useFallbackQuery) && terminal != null) {
            terminal.queryWindowSize();
            int width = terminal.getWindowWidth();
            int height = terminal.getWindowHeight();
            if (width > 0) {
                windowWidth = width;
            }
            if (height > 0) {
                windowHeight = height;
            }
        }
    }

}
