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

/**
 * A TAction represents a simple action to perform in response to the user.
 *
 * @see TButton
 */
public abstract class TAction {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The widget that called this action's DO() method.  Note that this
     * field could be null, for example if executed as a timer action.
     */
    public TWidget source;

    /**
     * An optional bit of data associated with this action.
     */
    public Object data;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Default constructor.
     */
    public TAction() {}

    // ------------------------------------------------------------------------
    // TAction ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Call DO() with source widget set.
     *
     * @param source the source widget
     */
    public final void DO(final TWidget source) {
        this.source = source;
        DO();
    }

    /**
     * Call DO() with source widget and data set.
     *
     * @param source the source widget
     * @param data the data
     */
    public final void DO(final TWidget source, final Object data) {
        this.source = source;
        this.data = data;
        DO();
    }

    /**
     * Various classes will call DO() when they are clicked/selected.
     */
    public abstract void DO();
}
