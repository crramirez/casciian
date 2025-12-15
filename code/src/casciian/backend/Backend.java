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

import java.util.List;

import casciian.bits.CellAttributes;
import casciian.event.TInputEvent;

/**
 * This interface provides a screen, keyboard, and mouse to TApplication.  It
 * also exposes session information as gleaned from lower levels of the
 * communication stack.
 */
public interface Backend {

    /**
     * Get a SessionInfo, which exposes text width/height, language,
     * username, and other information from the communication stack.
     *
     * @return the SessionInfo
     */
    public SessionInfo getSessionInfo();

    /**
     * Get a Screen, which displays the text cells to the user.
     *
     * @return the Screen
     */
    public Screen getScreen();

    /**
     * Classes must provide an implementation that syncs the logical screen
     * to the physical device.
     */
    public void flushScreen();

    /**
     * Check if there are events in the queue.
     *
     * @return if true, getEvents() has something to return to the application
     */
    public boolean hasEvents();

    /**
     * Classes must provide an implementation to get keyboard, mouse, and
     * screen resize events.
     *
     * @param queue list to append new events to
     */
    public void getEvents(List<TInputEvent> queue);

    /**
     * Classes must provide an implementation that closes sockets, restores
     * console, etc.
     */
    public void shutdown();

    /**
     * Classes must provide an implementation that sets the window title.
     *
     * @param title the new title
     */
    public void setTitle(final String title);

    /**
     * Set listener to a different Object.
     *
     * @param listener the new listening object that run() wakes up on new
     * input
     */
    public void setListener(final Object listener);

    /**
     * Reload backend options from System properties.
     */
    public void reloadOptions();

    /**
     * Check if backend is read-only.
     *
     * @return true if user input events from the backend are discarded
     */
    public boolean isReadOnly();

    /**
     * Set read-only flag.
     *
     * @param readOnly if true, then input events will be discarded
     */
    public void setReadOnly(final boolean readOnly);

    /**
     * Convert a CellAttributes foreground color to an RGB color.
     *
     * @param attr the text attributes
     * @return the RGB color
     */
    public int attrToForegroundColor(final CellAttributes attr);

    /**
     * Convert a CellAttributes background color to an RGB color.
     *
     * @param attr the text attributes
     * @return the RGB color
     */
    public int attrToBackgroundColor(final CellAttributes attr);

    /**
     * Copy text to the system clipboard of the terminal on the backend.  Not
     * all terminals support this (OSC 52).
     *
     * @param text string to copy
     */
    public void copyClipboardText(final String text);

    /**
     * Get window/terminal system focus.
     *
     * @return true if this backend has the mouse/keyboard focus
     */
    public boolean isFocused();

    /**
     * Retrieve the default foreground color.
     *
     * @return the RGB color
     */
    public int getDefaultForeColorRGB();

    /**
     * Retrieve the default background color.
     *
     * @return the RGB color
     */
    public int getDefaultBackColorRGB();

}
