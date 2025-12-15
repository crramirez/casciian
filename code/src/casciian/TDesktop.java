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

import casciian.bits.Cell;
import casciian.bits.CellAttributes;
import casciian.bits.ComplexCell;
import casciian.bits.GraphicsChars;
import casciian.event.TKeypressEvent;
import casciian.event.TMenuEvent;
import casciian.event.TMouseEvent;
import casciian.event.TResizeEvent;

/**
 * TDesktop is a special-class window that is drawn underneath everything
 * else.  Like a TWindow, it can contain widgets and perform "background"
 * processing via onIdle().  But unlike a TWindow, it cannot be hidden,
 * moved, or resized.
 *
 * <p>
 * Events are passed to TDesktop as follows:
 * <ul>
 * <li>Mouse events are seen if they do not cover any other windows.</li>
 * <li>Keypress events are seen if no other windows are open.</li>
 * <li>Menu events are seen if no other windows are open.</li>
 * <li>Command events are seen if no other windows are open.</li>
 * </ul>
 */
public class TDesktop extends TWindow {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The background character to use, or null to use the default background
     * color (SGR 49).
     */
    private ComplexCell backgroundCell = null;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent application
     */
    @SuppressWarnings("this-escape")
    public TDesktop(final TApplication parent) {
        super(parent, "", 0, 0, parent.getScreen().getWidth(),
            parent.getDesktopBottom() - parent.getDesktopTop());

        setActive(false);

        backgroundCell = new ComplexCell(GraphicsChars.HATCH);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle window/screen resize events.
     *
     * @param resize resize event
     */
    @Override
    public void onResize(final TResizeEvent resize) {
        if (getChildren().size() == 1) {
            TWidget child = getChildren().get(0);
            if (!(child instanceof TWindow)) {
                // Only one child, resize it to match my size.
                child.onResize(new TResizeEvent(resize.getBackend(),
                        TResizeEvent.Type.WIDGET, getWidth(), getHeight()));
            }
        }
        if (resize.getType() == TResizeEvent.Type.SCREEN) {
            // Let children see the screen resize
            for (TWidget widget: getChildren()) {
                widget.onResize(resize);
            }
        }
    }

    /**
     * Handle mouse button presses.
     *
     * @param mouse mouse button event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        this.mouse = mouse;

        // Give the shortcut bar a shot at this.
        if (statusBar != null) {
            if (statusBar.statusBarMouseDown(mouse)) {
                return;
            }
        }

        // Pass to children
        for (TWidget widget: getChildren()) {
            if (widget.mouseWouldHit(mouse)) {
                // Dispatch to this child, also activate it
                activate(widget);

                // Set x and y relative to the child's coordinates
                mouse.setX(mouse.getAbsoluteX() - widget.getAbsoluteX());
                mouse.setY(mouse.getAbsoluteY() - widget.getAbsoluteY());
                widget.handleEvent(mouse);
                return;
            }
        }
    }

    /**
     * Handle mouse button releases.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        this.mouse = mouse;

        // Give the shortcut bar a shot at this.
        if (statusBar != null) {
            if (statusBar.statusBarMouseUp(mouse)) {
                return;
            }
        }

        // Pass to children
        for (TWidget widget: getChildren()) {
            if (widget.mouseWouldHit(mouse)) {
                // Dispatch to this child, also activate it
                activate(widget);

                // Set x and y relative to the child's coordinates
                mouse.setX(mouse.getAbsoluteX() - widget.getAbsoluteX());
                mouse.setY(mouse.getAbsoluteY() - widget.getAbsoluteY());
                widget.handleEvent(mouse);
                return;
            }
        }
    }

    /**
     * Handle mouse movements.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        this.mouse = mouse;

        // Give the shortcut bar a shot at this.
        if (statusBar != null) {
            statusBar.statusBarMouseMotion(mouse);
        }

        // Default: do nothing, pass to children instead
        super.onMouseMotion(mouse);
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        // Default: do nothing, pass to children instead
        super.onKeypress(keypress);
    }

    /**
     * Handle posted menu events.
     *
     * @param menu menu event
     */
    @Override
    public void onMenu(final TMenuEvent menu) {
        // Default: do nothing, pass to children instead
        super.onMenu(menu);
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The default TDesktop draws a hatch character across everything.
     */
    @Override
    public void draw() {
        assert (backgroundCell != null);

        if (!backgroundCell.isDefaultColor(false)) {
            backgroundCell.setTo(getTheme().getColor("tdesktop.background"));
        }
        putAll(backgroundCell);

        /*
        // For debugging, let's see where the desktop bounds really are.
        putCharXY(0, 0, '0', background);
        putCharXY(getWidth() - 1, 0, '1', background);
        putCharXY(0, getHeight() - 1, '2', background);
        putCharXY(getWidth() - 1, getHeight() - 1, '3', background);
         */
    }

    /**
     * Hide window.  This is a NOP for TDesktop.
     */
    @Override
    public final void hide() {}

    /**
     * Show window.  This is a NOP for TDesktop.
     */
    @Override
    public final void show() {}

    /**
     * Called by hide().  This is a NOP for TDesktop.
     */
    @Override
    public final void onHide() {}

    /**
     * Called by show().  This is a NOP for TDesktop.
     */
    @Override
    public final void onShow() {}

    /**
     * Returns true if the mouse is currently on the close button.
     *
     * @return true if mouse is currently on the close button
     */
    @Override
    protected final boolean mouseOnClose() {
        return false;
    }

    /**
     * Returns true if the mouse is currently on the maximize/restore button.
     *
     * @return true if the mouse is currently on the maximize/restore button
     */
    @Override
    protected final boolean mouseOnMaximize() {
        return false;
    }

    /**
     * Returns true if the mouse is currently on the resizable lower right
     * corner.
     *
     * @return true if the mouse is currently on the resizable lower right
     * corner
     */
    @Override
    protected final boolean mouseOnResize() {
        return false;
    }

    // ------------------------------------------------------------------------
    // TDesktop ---------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set the background cell.
     *
     * @param cell the cell, or null to use the terminal's default foreground
     * and background colors.
     */
    public void setBackgroundCell(final Cell cell) {
        if (cell != null) {
            backgroundCell = new ComplexCell(cell);
        } else {
            backgroundCell = new ComplexCell();
            backgroundCell.setDefaultColor(true, true);
            backgroundCell.setDefaultColor(false, true);
        }
    }

}
