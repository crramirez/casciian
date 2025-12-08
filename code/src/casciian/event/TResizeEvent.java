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
 * This class encapsulates a screen or window resize event.
 */
public class TResizeEvent extends TInputEvent {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Resize events can be generated for either a total screen resize or a
     * widget/window resize.
     */
    public enum Type {
        /**
         * The entire screen size changed.
         */
        SCREEN,

        /**
         * A widget was resized.
         */
        WIDGET
    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The type of resize.
     */
    private Type type;

    /**
     * New width.
     */
    private int width;

    /**
     * New height.
     */
    private int height;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public contructor.
     *
     * @param backend the backend that generated this event
     * @param type the Type of resize, Screen or Widget
     * @param width the new width
     * @param height the new height
     */
    public TResizeEvent(final Backend backend, final Type type,
        final int width, final int height) {

        super(backend);

        this.type   = type;
        this.width  = width;
        this.height = height;
    }

    // ------------------------------------------------------------------------
    // TResizeEvent -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get resize type.
     *
     * @return SCREEN or WIDGET
     */
    public Type getType() {
        return type;
    }

    /**
     * Get the new width.
     *
     * @return width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Get the new height.
     *
     * @return height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Make human-readable description of this TResizeEvent.
     *
     * @return displayable String
     */
    @Override
    public String toString() {
        return String.format("Resize: %s width = %d height = %d",
            type, width, height);
    }

}
