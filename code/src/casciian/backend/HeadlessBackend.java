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
 * HeadlessBackend
 */
public class HeadlessBackend extends LogicalScreen implements Backend {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The session information.
     */
    private SessionInfo sessionInfo;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     */
    public HeadlessBackend() {
        sessionInfo = new TSessionInfo(width, height);
    }

    // ------------------------------------------------------------------------
    // Backend ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Getter for sessionInfo.
     *
     * @return the SessionInfo
     */
    public final SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    /**
     * Get a Screen, which displays the text cells to the user.
     *
     * @return the Screen
     */
    public Screen getScreen() {
        return this;
    }

    /**
     * Subclasses must provide an implementation that syncs the logical
     * screen to the physical device.
     */
    public void flushScreen() {
        // NOP
    }

    /**
     * Check if there are events in the queue.
     *
     * @return if true, getEvents() has something to return to the application
     */
    public boolean hasEvents() {
        return false;
    }

    /**
     * Subclasses must provide an implementation to get keyboard, mouse, and
     * screen resize events.
     *
     * @param queue list to append new events to
     */
    public void getEvents(List<TInputEvent> queue) {
        // NOP
    }

    /**
     * Subclasses must provide an implementation that closes sockets,
     * restores console, etc.
     */
    public void shutdown() {
        // NOP
    }

    /**
     * Set listener to a different Object.
     *
     * @param listener the new listening object that run() wakes up on new
     * input
     */
    public void setListener(final Object listener) {
        // NOP
    }

    /**
     * Reload backend options from System properties.
     */
    public void reloadOptions() {
        // NOP
    }

    /**
     * Check if backend is read-only.  For a HeadlessBackend, this is always
     * false.
     *
     * @return true
     */
    public boolean isReadOnly() {
        return true;
    }

    /**
     * Set read-only flag.  This does nothing for HeadlessBackend.
     *
     * @param readOnly ignored
     */
    public void setReadOnly(final boolean readOnly) {
        // NOP
    }

    /**
     * Check if backend will support incomplete image fragments over text
     * display.
     *
     * @return true if images can partially obscure text
     */
    public boolean isImagesOverText() {
        return false;
    }

    /**
     * Check if backend is reporting pixel-based mouse position.
     *
     * @return true if single-pixel mouse movements are reported
     */
    public boolean isPixelMouse() {
        return false;
    }

    /**
     * Set request for backend to report pixel-based mouse position.
     *
     * @param pixelMouse if true, single-pixel mouse movements will be
     * reported, if the backend supports it
     */
    public void setPixelMouse(final boolean pixelMouse) {
        // NOP
    }

    /**
     * Convert a CellAttributes foreground color to an RGB color.
     *
     * @param attr the text attributes
     * @return the RGB color
     */
    public int attrToForegroundColor(final CellAttributes attr) {
        // Use ECMA48 colors.
        return ECMA48Terminal.attrToForegroundColor(attr);
    }

    /**
     * Convert a CellAttributes background color to an RGB color.
     *
     * @param attr the text attributes
     * @return the RGB color
     */
    public int attrToBackgroundColor(final CellAttributes attr) {
        // Use ECMA48 colors.
        return ECMA48Terminal.attrToBackgroundColor(attr);
    }

    /**
     * Retrieve the default foreground color.
     *
     * @return the RGB color
     */
    public int getDefaultForeColorRGB() {
        // Use ECMA48 colors.
        return ECMA48Terminal.getDefaultForeColorRGB();
    }

    /**
     * Retrieve the default background color.
     *
     * @return the RGB color
     */
    public int getDefaultBackColorRGB() {
        // Use ECMA48 colors.
        return ECMA48Terminal.getDefaultBackColorRGB();
    }

    /**
     * Copy text to the system clipboard of the terminal on the backend.
     *
     * @param text string to copy
     */
    public void copyClipboardText(final String text) {
        // NOP
    }

    /**
     * Get window/terminal system focus.
     *
     * @return true if this backend has the mouse/keyboard focus
     */
    public boolean isFocused() {
        return false;
    }

}
