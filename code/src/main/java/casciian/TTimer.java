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

import java.util.Date;

/**
 * TTimer implements a simple timer.
 */
public class TTimer {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * If true, re-schedule after every tick.  Note package private access.
     */
    boolean recurring = false;

    /**
     * Duration (in millis) between ticks if this is a recurring timer.
     */
    private long duration = 0;

    /**
     * The next time this timer needs to be ticked.
     */
    private Date nextTick;

    /**
     * The action to perfom on a tick.
     */
    private TAction action;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Package private constructor.
     *
     * @param duration number of milliseconds to wait between ticks
     * @param recurring if true, re-schedule this timer after every tick
     * @param action to perform on next tick
     */
    TTimer(final long duration, final boolean recurring, final TAction action) {

        this.recurring = recurring;
        this.duration  = duration;
        this.action    = action;

        Date now = new Date();
        nextTick = new Date(now.getTime() + duration);
    }

    // ------------------------------------------------------------------------
    // TTimer -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the next time this timer needs to be ticked.  Note package private
     * access.
     *
     * @return time at which action should be called
     */
    Date getNextTick() {
        return nextTick;
    }

    /**
     * Set the recurring flag.
     *
     * @param recurring if true, re-schedule this timer after every tick
     */
    public void setRecurring(final boolean recurring) {
        this.recurring = recurring;
    }

    /**
     * Tick this timer.  Note package private access.
     */
    void tick() {
        if (action != null) {
            action.DO();
        }
        // Set next tick
        Date ticked = new Date();
        if (recurring) {
            nextTick = new Date(ticked.getTime() + duration);
        }
    }

}
