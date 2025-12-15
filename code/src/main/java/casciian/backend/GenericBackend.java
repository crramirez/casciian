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

import casciian.event.TInputEvent;
import casciian.event.TCommandEvent;
import static casciian.TCommand.*;

/**
 * This abstract class provides a screen, keyboard, and mouse to
 * TApplication.  It also exposes session information as gleaned from lower
 * levels of the communication stack.
 */
public abstract class GenericBackend implements Backend {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The session information.
     */
    protected SessionInfo sessionInfo;

    /**
     * The screen to draw on.
     */
    protected Screen screen;

    /**
     * Input events are processed by this Terminal.
     */
    protected TerminalReader terminal;

    /**
     * By default, GenericBackend adds a cmAbort after it sees
     * cmBackendDisconnect, so that TApplication will exit when the user
     * closes the Swing window or disconnects the ECMA48 streams.  But
     * MultiBackend wraps multiple Backends, and needs to decide when to send
     * cmAbort differently.  Setting this to false is how it manages that.
     * Note package private access.
     */
    boolean abortOnDisconnect = true;

    /**
     * The last time user input (mouse or keyboard) was received.
     */
    protected long lastUserInputTime = System.currentTimeMillis();

    /**
     * Whether or not this backend is read-only.
     */
    protected boolean readOnly = false;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Default constructor used by subclasses.
     */
    protected GenericBackend() {}

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
     * Getter for screen.
     *
     * @return the Screen
     */
    public final Screen getScreen() {
        return screen;
    }

    /**
     * Sync the logical screen to the physical device.
     */
    public void flushScreen() {
        screen.flushPhysical();
    }

    /**
     * Check if there are events in the queue.
     *
     * @return if true, getEvents() has something to return to the application
     */
    public boolean hasEvents() {
        if (terminal.hasEvents()) {
            return true;
        }
        long now = System.currentTimeMillis();
        sessionInfo.setIdleTime((int) (now - lastUserInputTime) / 1000);
        /*
        System.err.println(sessionInfo + " idle " +
            sessionInfo.getIdleTime());
         */
        return false;
    }

    /**
     * Get keyboard, mouse, and screen resize events.
     *
     * @param queue list to append new events to
     */
    public void getEvents(final List<TInputEvent> queue) {
        if (terminal.hasEvents()) {
            terminal.getEvents(queue);

            long now = System.currentTimeMillis();
            TCommandEvent backendDisconnect = null;
            boolean disconnect = false;

            if (queue.size() > 0) {
                lastUserInputTime = now;

                TInputEvent event = queue.get(queue.size() - 1);
                if (event instanceof TCommandEvent) {
                    TCommandEvent command = (TCommandEvent) event;
                    if (command.equals(cmBackendDisconnect)) {
                        backendDisconnect = command;
                        // This default backend assumes a single user, and if
                        // that user becomes disconnected we should terminate
                        // the application.
                        if (abortOnDisconnect == true) {
                            disconnect = true;
                        }
                    }
                }
            }

            sessionInfo.setIdleTime((int) (now - lastUserInputTime) / 1000);

            if (readOnly) {
                queue.clear();
                if (backendDisconnect != null) {
                    queue.add(backendDisconnect);
                }
            }

            if (disconnect) {
                assert (backendDisconnect != null);
                assert (queue.size() > 0);
                assert (queue.get(queue.size() - 1).equals(backendDisconnect));
                queue.add(new TCommandEvent(backendDisconnect.getBackend(),
                        cmAbort));
            }
        }
    }

    /**
     * Close the I/O, restore the console, etc.
     */
    public void shutdown() {
        terminal.closeTerminal();
    }

    /**
     * Set the window title.
     *
     * @param title the new title
     */
    public void setTitle(final String title) {
        screen.setTitle(title);
    }

    /**
     * Set listener to a different Object.
     *
     * @param listener the new listening object that run() wakes up on new
     * input
     */
    public void setListener(final Object listener) {
        terminal.setListener(listener);
    }

    /**
     * Reload backend options from System properties.
     */
    public void reloadOptions() {
        terminal.reloadOptions();
    }

    /**
     * Check if backend is read-only.
     *
     * @return true if user input events from the backend are discarded
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Set read-only flag.
     *
     * @param readOnly if true, then input events will be discarded
     */
    public void setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
    }

}
