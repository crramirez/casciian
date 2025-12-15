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

import casciian.TKeypress;
import casciian.backend.Backend;

/**
 * This class encapsulates a keyboard input event.
 */
public class TKeypressEvent extends TInputEvent {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Keystroke received.
     */
    private TKeypress key;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public contructor.
     *
     * @param backend the backend that generated this event
     * @param key the TKeypress received
     */
    public TKeypressEvent(final Backend backend, final TKeypress key) {
        super(backend);

        this.key = key;
    }

    /**
     * Public constructor.
     *
     * @param backend the backend that generated this event
     * @param isKey is true, this is a function key
     * @param fnKey the function key code (only valid if isKey is true)
     * @param ch the character (only valid if fnKey is false)
     * @param alt if true, ALT was pressed with this keystroke
     * @param ctrl if true, CTRL was pressed with this keystroke
     * @param shift if true, SHIFT was pressed with this keystroke
     */
    public TKeypressEvent(final Backend backend, final boolean isKey,
        final int fnKey, final int ch, final boolean alt, final boolean ctrl,
        final boolean shift) {

        super(backend);

        this.key = new TKeypress(isKey, fnKey, ch, alt, ctrl, shift);
    }

    /**
     * Public constructor.
     *
     * @param backend the backend that generated this event
     * @param key the TKeypress received
     * @param alt if true, ALT was pressed with this keystroke
     * @param ctrl if true, CTRL was pressed with this keystroke
     * @param shift if true, SHIFT was pressed with this keystroke
     */
    public TKeypressEvent(final Backend backend, final TKeypress key,
        final boolean alt, final boolean ctrl, final boolean shift) {

        super(backend);

        this.key = new TKeypress(key.isFnKey(), key.getKeyCode(), key.getChar(),
            alt, ctrl, shift);
    }

    // ------------------------------------------------------------------------
    // TInputEvent ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Comparison check.  All fields must match to return true.
     *
     * @param rhs another TKeypressEvent or TKeypress instance
     * @return true if all fields are equal
     */
    @Override
    public boolean equals(final Object rhs) {
        if (!(rhs instanceof TKeypressEvent)
            && !(rhs instanceof TKeypress)
        ) {
            return false;
        }

        if (rhs instanceof TKeypressEvent) {
            TKeypressEvent that = (TKeypressEvent) rhs;
            return (key.equals(that.key)
                && (getTime().equals(that.getTime())));
        }

        TKeypress that = (TKeypress) rhs;
        return (key.equals(that));
    }

    /**
     * Hashcode uses all fields in equals().
     *
     * @return the hash
     */
    @Override
    public int hashCode() {
        int A = 13;
        int B = 23;
        int hash = A;
        hash = (B * hash) + getTime().hashCode();
        hash = (B * hash) + key.hashCode();
        return hash;
    }

    /**
     * Make human-readable description of this TKeypressEvent.
     *
     * @return displayable String
     */
    @Override
    public String toString() {
        return String.format("Keypress: %s", key.toString());
    }

    // ------------------------------------------------------------------------
    // TKeypressEvent ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get keystroke.
     *
     * @return keystroke
     */
    public TKeypress getKey() {
        return key;
    }

    /**
     * Create a duplicate instance.
     *
     * @return duplicate intance
     */
    public TKeypressEvent dup() {
        TKeypressEvent keypress = new TKeypressEvent(getBackend(), key.dup());
        return keypress;
    }

}
