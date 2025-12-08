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

/**
 * SessionInfo is used to store per-session properties that are determined at
 * different layers of the communication stack.
 */
public interface SessionInfo {

    /**
     * Get the time this session was started.
     *
     * @return the number of millis since midnight, January 1, 1970 UTC
     */
    public long getStartTime();

    /**
     * Get the time this session was idle.
     *
     * @return the number of seconds since the last user input event from
     * this session
     */
    public int getIdleTime();

    /**
     * Set the time this session was idle.
     *
     * @param seconds the number of seconds since the last user input event
     * from this session
     */
    public void setIdleTime(final int seconds);

    /**
     * Username getter.
     *
     * @return the username
     */
    public String getUsername();

    /**
     * Username setter.
     *
     * @param username the value
     */
    public void setUsername(String username);

    /**
     * Language getter.
     *
     * @return the language
     */
    public String getLanguage();

    /**
     * Language setter.
     *
     * @param language the value
     */
    public void setLanguage(String language);

    /**
     * Text window width getter.
     *
     * @return the window width
     */
    public int getWindowWidth();

    /**
     * Text window height getter.
     *
     * @return the window height
     */
    public int getWindowHeight();

    /**
     * Re-query the text window size.
     */
    public void queryWindowSize();
}
