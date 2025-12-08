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
package casciian.event;

import java.util.Date;

import casciian.backend.Backend;

/**
 * This is the parent class of all events dispatched to the UI.
 */
public abstract class TInputEvent {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Time at which event was generated.
     */
    private Date time;

    /**
     * The backend that generated this event.
     */
    private Backend backend;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Protected contructor.
     *
     * @param backend the backend that generated this event
     */
    protected TInputEvent(final Backend backend) {
        this.backend = backend;

        // Save the current time
        time = new Date();
    }

    // ------------------------------------------------------------------------
    // TInputEvent ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get time.
     *
     * @return the time that this event was generated
     */
    public final Date getTime() {
        return time;
    }

    /**
     * Get the backend that generated this event.
     *
     * @return the backend that generated this event, or null if this event
     * was generated internally
     */
    public final Backend getBackend() {
        return backend;
    }

}
