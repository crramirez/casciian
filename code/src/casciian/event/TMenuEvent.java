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

import casciian.backend.Backend;

/**
 * This class encapsulates a menu selection event.
 * TApplication.getMenuItem(id) can be used to obtain the TMenuItem itself,
 * say for setting enabled/disabled/checked/etc.
 */
public class TMenuEvent extends TInputEvent {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * MenuItem ID.
     */
    private int id;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public contructor.
     *
     * @param backend the backend that generated this event
     * @param id the MenuItem ID
     */
    public TMenuEvent(final Backend backend, final int id) {
        super(backend);

        this.id = id;
    }

    // ------------------------------------------------------------------------
    // TInputEvent ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Make human-readable description of this TMenuEvent.
     *
     * @return displayable String
     */
    @Override
    public String toString() {
        return String.format("MenuEvent: %d", id);
    }

    // ------------------------------------------------------------------------
    // TMenuEvent -------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the MenuItem ID.
     *
     * @return the ID
     */
    public int getId() {
        return id;
    }

}
