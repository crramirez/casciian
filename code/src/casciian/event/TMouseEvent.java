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
 * This class encapsulates several kinds of mouse input events.  Note that
 * the relative (x,y) ARE MUTABLE: TWidget's onMouse() handlers perform that
 * update during event dispatching.
 */
public class TMouseEvent extends TInputEvent {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The type of event generated.
     */
    public enum Type {
        /**
         * Mouse motion.  X and Y will have screen coordinates.
         */
        MOUSE_MOTION,

        /**
         * Mouse button down.  X and Y will have screen coordinates.
         */
        MOUSE_DOWN,

        /**
         * Mouse button up.  X and Y will have screen coordinates.
         */
        MOUSE_UP,

        /**
         * Mouse double-click.  X and Y will have screen coordinates.
         */
        MOUSE_DOUBLE_CLICK
    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Type of event, one of MOUSE_MOTION, MOUSE_UP, or MOUSE_DOWN.
     */
    private Type type;

    /**
     * Mouse X - relative coordinates.
     */
    private int x;

    /**
     * Mouse Y - relative coordinates.
     */
    private int y;

    /**
     * Mouse X - absolute screen coordinates.
     */
    private int absoluteX;

    /**
     * Mouse Y - absolute screen coordinate.
     */
    private int absoluteY;

    /**
     * Mouse X pixel offset relative to its text cell position.
     */
    private int pixelOffsetX;

    /**
     * Mouse Y pixel offset relative to its text cell position.
     */
    private int pixelOffsetY;

    /**
     * Mouse button 1 (left button).
     */
    private boolean mouse1;

    /**
     * Mouse button 2 (right button).
     */
    private boolean mouse2;

    /**
     * Mouse button 3 (middle button).
     */
    private boolean mouse3;

    /**
     * Mouse wheel UP (button 4).
     */
    private boolean mouseWheelUp;

    /**
     * Mouse wheel DOWN (button 5).
     */
    private boolean mouseWheelDown;

    /**
     * Keyboard modifier ALT.
     */
    private boolean alt;

    /**
     * Keyboard modifier CTRL.
     */
    private boolean ctrl;

    /**
     * Keyboard modifier SHIFT.
     */
    private boolean shift;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public contructor.
     *
     * @param backend the backend that generated this event
     * @param type the type of event, MOUSE_MOTION, MOUSE_DOWN, or MOUSE_UP
     * @param x relative column
     * @param y relative row
     * @param absoluteX absolute column
     * @param absoluteY absolute row
     * @param mouse1 if true, left button is down
     * @param mouse2 if true, right button is down
     * @param mouse3 if true, middle button is down
     * @param mouseWheelUp if true, mouse wheel (button 4) is down
     * @param mouseWheelDown if true, mouse wheel (button 5) is down
     * @param alt if true, ALT was pressed with this mouse event
     * @param ctrl if true, CTRL was pressed with this mouse event
     * @param shift if true, SHIFT was pressed with this mouse event
     */
    public TMouseEvent(final Backend backend, final Type type,
        final int x, final int y, final int absoluteX, final int absoluteY,
        final boolean mouse1, final boolean mouse2, final boolean mouse3,
        final boolean mouseWheelUp, final boolean mouseWheelDown,
        final boolean alt, final boolean ctrl, final boolean shift) {

        this(backend, type, x, y, absoluteX, absoluteY, 0, 0,
            mouse1, mouse2, mouse3, mouseWheelUp, mouseWheelDown,
            alt, ctrl, shift);
    }

    /**
     * Public contructor.
     *
     * @param backend the backend that generated this event
     * @param type the type of event, MOUSE_MOTION, MOUSE_DOWN, or MOUSE_UP
     * @param x relative column
     * @param y relative row
     * @param absoluteX absolute column
     * @param absoluteY absolute row
     * @param pixelOffsetX X pixel offset relative to text cell
     * @param pixelOffsetY Y pixel offset relative to text cell
     * @param mouse1 if true, left button is down
     * @param mouse2 if true, right button is down
     * @param mouse3 if true, middle button is down
     * @param mouseWheelUp if true, mouse wheel (button 4) is down
     * @param mouseWheelDown if true, mouse wheel (button 5) is down
     * @param alt if true, ALT was pressed with this mouse event
     * @param ctrl if true, CTRL was pressed with this mouse event
     * @param shift if true, SHIFT was pressed with this mouse event
     */
    public TMouseEvent(final Backend backend, final Type type,
        final int x, final int y, final int absoluteX, final int absoluteY,
        final int pixelOffsetX, final int pixelOffsetY,
        final boolean mouse1, final boolean mouse2, final boolean mouse3,
        final boolean mouseWheelUp, final boolean mouseWheelDown,
        final boolean alt, final boolean ctrl, final boolean shift) {

        super(backend);

        this.type               = type;
        this.x                  = x;
        this.y                  = y;
        this.absoluteX          = absoluteX;
        this.absoluteY          = absoluteY;
        this.pixelOffsetX       = pixelOffsetX;
        this.pixelOffsetY       = pixelOffsetY;
        this.mouse1             = mouse1;
        this.mouse2             = mouse2;
        this.mouse3             = mouse3;
        this.mouseWheelUp       = mouseWheelUp;
        this.mouseWheelDown     = mouseWheelDown;
        this.alt                = alt;
        this.ctrl               = ctrl;
        this.shift              = shift;
    }

    // ------------------------------------------------------------------------
    // TMouseEvent ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get type.
     *
     * @return type
     */
    public Type getType() {
        return type;
    }

    /**
     * Get x.
     *
     * @return x
     */
    public int getX() {
        return x;
    }

    /**
     * Set x.
     *
     * @param x new relative X value
     * @see casciian.TWidget#onMouseDown(TMouseEvent mouse)
     * @see casciian.TWidget#onMouseDown(TMouseEvent mouse)
     * @see casciian.TWidget#onMouseMotion(TMouseEvent mouse)
     */
    public void setX(final int x) {
        this.x = x;
    }

    /**
     * Get y.
     *
     * @return y
     */
    public int getY() {
        return y;
    }

    /**
     * Set y.
     *
     * @param y new relative Y value
     * @see casciian.TWidget#onMouseDown(TMouseEvent mouse)
     * @see casciian.TWidget#onMouseDown(TMouseEvent mouse)
     * @see casciian.TWidget#onMouseMotion(TMouseEvent mouse)
     */
    public void setY(final int y) {
        this.y = y;
    }

    /**
     * Get absoluteX.
     *
     * @return absoluteX
     */
    public int getAbsoluteX() {
        return absoluteX;
    }

    /**
     * Set absoluteX.
     *
     * @param absoluteX the new value
     */
    public void setAbsoluteX(final int absoluteX) {
        this.absoluteX = absoluteX;
    }

    /**
     * Get absoluteY.
     *
     * @return absoluteY
     */
    public int getAbsoluteY() {
        return absoluteY;
    }

    /**
     * Set absoluteY.
     *
     * @param absoluteY the new value
     */
    public void setAbsoluteY(final int absoluteY) {
        this.absoluteY = absoluteY;
    }

    /**
     * Get pixelOffsetX.
     *
     * @return pixelOffsetX
     */
    public int getPixelOffsetX() {
        return pixelOffsetX;
    }

    /**
     * Set pixelOffsetX.
     *
     * @param pixelOffsetX the new value
     */
    public void setPixelOffsetX(final int pixelOffsetX) {
        this.pixelOffsetX = pixelOffsetX;
    }

    /**
     * Get pixelOffsetY.
     *
     * @return pixelOffsetY
     */
    public int getPixelOffsetY() {
        return pixelOffsetY;
    }

    /**
     * Set pixelOffsetY.
     *
     * @param pixelOffsetY the new value
     */
    public void setPixelOffsetY(final int pixelOffsetY) {
        this.pixelOffsetY = pixelOffsetY;
    }

    /**
     * Get mouse1.
     *
     * @return mouse1
     */
    public boolean isMouse1() {
        return mouse1;
    }

    /**
     * Get mouse2.
     *
     * @return mouse2
     */
    public boolean isMouse2() {
        return mouse2;
    }

    /**
     * Get mouse3.
     *
     * @return mouse3
     */
    public boolean isMouse3() {
        return mouse3;
    }

    /**
     * Get mouseWheelUp.
     *
     * @return mouseWheelUp
     */
    public boolean isMouseWheelUp() {
        return mouseWheelUp;
    }

    /**
     * Get mouseWheelDown.
     *
     * @return mouseWheelDown
     */
    public boolean isMouseWheelDown() {
        return mouseWheelDown;
    }

    /**
     * Getter for ALT.
     *
     * @return alt value
     */
    public boolean isAlt() {
        return alt;
    }

    /**
     * Getter for CTRL.
     *
     * @return ctrl value
     */
    public boolean isCtrl() {
        return ctrl;
    }

    /**
     * Getter for SHIFT.
     *
     * @return shift value
     */
    public boolean isShift() {
        return shift;
    }

    /**
     * Create a duplicate instance.
     *
     * @return duplicate intance
     */
    public TMouseEvent dup() {
        TMouseEvent mouse = new TMouseEvent(getBackend(), type, x, y,
            absoluteX, absoluteY, pixelOffsetX, pixelOffsetY,
            mouse1, mouse2, mouse3,
            mouseWheelUp, mouseWheelDown, alt, ctrl, shift);

        return mouse;
    }

    /**
     * Make human-readable description of this TMouseEvent.
     *
     * @return displayable String
     */
    @Override
    public String toString() {
        return String.format("Mouse: %s x %d y %d absoluteX %d absoluteY %d pixelX %d pixelY %d 1 %s 2 %s 3 %s DOWN %s UP %s ALT %s CTRL %s SHIFT %s",
            type,
            x, y,
            absoluteX, absoluteY,
            pixelOffsetX, pixelOffsetY,
            mouse1,
            mouse2,
            mouse3,
            mouseWheelUp,
            mouseWheelDown,
            alt, ctrl, shift);
    }

}
