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

import java.util.ArrayList;
import java.util.List;

import casciian.TApplication;
import casciian.TWindow;
import casciian.bits.Cell;
import casciian.bits.CellAttributes;
import casciian.event.TCommandEvent;
import casciian.event.TInputEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import casciian.event.TResizeEvent;
import static casciian.TCommand.*;

/**
 * TWindowBackend uses a window in one TApplication to provide a backend for
 * another TApplication.
 *
 * Note that TWindow has its own getScreen() and setTitle() functions.
 * Clients in TWindowBackend's application won't be able to use it to get at
 * the other application's screen.  getOtherScreen() has been provided.
 */
public class TWindowBackend extends TWindow implements Backend {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The listening object that run() wakes up on new input.
     */
    private Object listener;

    /**
     * The object to sync on in draw().  This is normally otherScreen, but it
     * could also be a MultiScreen.
     */
    private Object drawLock;

    /**
     * The event queue, filled up by a thread reading on input.
     */
    private List<TInputEvent> eventQueue;

    /**
     * The screen this window is monitoring.
     */
    private Screen otherScreen;

    /**
     * The application associated with otherScreen.
     */
    private TApplication otherApplication;

    /**
     * The session information.
     */
    private SessionInfo sessionInfo;

    /**
     * The last time user input (mouse or keyboard) was received.
     */
    private long lastUserInputTime = System.currentTimeMillis();

    /**
     * Whether or not this backend is read-only.
     */
    private boolean readOnly = false;

    /**
     * If true, this backend has the mouse/keyboard focus.
     */
    private boolean hasFocus = false;

    /**
     * OtherScreen provides a hook to notify TWindowBackend of screen size
     * changes.
     */
    private class OtherScreen extends LogicalScreen {

        /**
         * The TWindowBackend to notify.
         */
        private TWindowBackend window;

        /**
         * Public constructor.
         */
        public OtherScreen(final TWindowBackend window) {
            this.window = window;
        }

        /**
         * Resize the physical screen to match the logical screen dimensions.
         */
        @Override
        public void resizeToScreen() {
            window.setWidth(getWidth() + 2);
            window.setHeight(getHeight() + 2);
        }

    }


    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.  Window will be located at (0, 0).
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param application TApplication that manages this window
     * @param title window title, will be centered along the top border
     * @param width width of window
     * @param height height of window
     */
    @SuppressWarnings("this-escape")
    public TWindowBackend(final Object listener,
        final TApplication application, final String title,
        final int width, final int height) {

        super(application, title, width, height);

        this.listener = listener;
        eventQueue = new ArrayList<TInputEvent>();
        sessionInfo = new TSessionInfo(width, height);
        otherScreen = new OtherScreen(this);
        otherScreen.setDimensions(width - 2, height - 2);
        drawLock = otherScreen;
        setHiddenMouse(!readOnly);
    }

    /**
     * Public constructor.  Window will be located at (0, 0).
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param application TApplication that manages this window
     * @param title window title, will be centered along the top border
     * @param width width of window
     * @param height height of window
     * @param flags bitmask of RESIZABLE, CENTERED, or MODAL
     */
    @SuppressWarnings("this-escape")
    public TWindowBackend(final Object listener,
        final TApplication application, final String title,
        final int width, final int height, final int flags) {

        super(application, title, width, height, flags);

        this.listener = listener;
        eventQueue = new ArrayList<TInputEvent>();
        sessionInfo = new TSessionInfo(width, height);
        otherScreen = new OtherScreen(this);
        otherScreen.setDimensions(width - 2, height - 2);
        drawLock = otherScreen;
        setHiddenMouse(!readOnly);
    }

    /**
     * Public constructor.
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param application TApplication that manages this window
     * @param title window title, will be centered along the top border
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of window
     * @param height height of window
     */
    @SuppressWarnings("this-escape")
    public TWindowBackend(final Object listener,
        final TApplication application, final String title,
        final int x, final int y, final int width, final int height) {

        super(application, title, x, y, width, height);

        this.listener = listener;
        eventQueue = new ArrayList<TInputEvent>();
        sessionInfo = new TSessionInfo(width, height);
        otherScreen = new OtherScreen(this);
        otherScreen.setDimensions(width - 2, height - 2);
        drawLock = otherScreen;
        setHiddenMouse(!readOnly);
    }

    /**
     * Public constructor.
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param application TApplication that manages this window
     * @param title window title, will be centered along the top border
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of window
     * @param height height of window
     * @param flags mask of RESIZABLE, CENTERED, or MODAL
     */
    @SuppressWarnings("this-escape")
    public TWindowBackend(final Object listener,
        final TApplication application, final String title,
        final int x, final int y, final int width, final int height,
        final int flags) {

        super(application, title, x, y, width, height, flags);

        this.listener = listener;
        eventQueue = new ArrayList<TInputEvent>();
        sessionInfo = new TSessionInfo(width, height);
        otherScreen = new OtherScreen(this);
        otherScreen.setDimensions(width - 2, height - 2);
        drawLock = otherScreen;
        setHiddenMouse(!readOnly);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle window/screen resize events.
     *
     * @param event resize event
     */
    @Override
    public void onResize(final TResizeEvent event) {
        if (event.getType() == TResizeEvent.Type.WIDGET) {
            int newWidth = event.getWidth() - 2;
            int newHeight = event.getHeight() - 2;
            if ((newWidth != otherScreen.getWidth())
                || (newHeight != otherScreen.getHeight())
            ) {
                // I was resized, notify the screen I am watching to match my
                // new size.
                synchronized (eventQueue) {
                    eventQueue.add(new TResizeEvent(this,
                            TResizeEvent.Type.SCREEN, newWidth, newHeight));
                }
                synchronized (listener) {
                    listener.notifyAll();
                }
            }
            return;
        } else {
            super.onResize(event);
        }
    }

    /**
     * Returns true if the mouse is currently in the otherScreen window.
     *
     * @param mouse mouse event
     * @return true if mouse is currently in the otherScreen window.
     */
    protected boolean mouseOnOtherScreen(final TMouseEvent mouse) {
        if ((mouse.getY() >= 1)
            && (mouse.getY() <= otherScreen.getHeight())
            && (mouse.getX() >= 1)
            && (mouse.getX() <= otherScreen.getWidth())
        ) {
            return true;
        }
        return false;
    }

    /**
     * Handle mouse button presses.
     *
     * @param mouse mouse button event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if (mouseOnOtherScreen(mouse)) {
            TMouseEvent event = mouse.dup();
            event.setX(mouse.getX() - 1);
            event.setY(mouse.getY() - 1);
            event.setAbsoluteX(event.getX());
            event.setAbsoluteY(event.getY());
            synchronized (eventQueue) {
                eventQueue.add(event);
            }
            synchronized (listener) {
                listener.notifyAll();
            }
        }
        super.onMouseDown(mouse);
    }

    /**
     * Handle mouse button releases.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        if (mouseOnOtherScreen(mouse)) {
            TMouseEvent event = mouse.dup();
            event.setX(mouse.getX() - 1);
            event.setY(mouse.getY() - 1);
            event.setAbsoluteX(event.getX());
            event.setAbsoluteY(event.getY());
            synchronized (eventQueue) {
                eventQueue.add(event);
            }
            synchronized (listener) {
                listener.notifyAll();
            }
        }
        super.onMouseUp(mouse);
    }

    /**
     * Handle mouse movements.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        if (mouseOnOtherScreen(mouse)) {
            TMouseEvent event = mouse.dup();
            event.setX(mouse.getX() - 1);
            event.setY(mouse.getY() - 1);
            event.setAbsoluteX(event.getX());
            event.setAbsoluteY(event.getY());
            synchronized (eventQueue) {
                eventQueue.add(event);
            }
            synchronized (listener) {
                listener.notifyAll();
            }
        }
        super.onMouseMotion(mouse);
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        TKeypressEvent event = keypress.dup();
        synchronized (eventQueue) {
            eventQueue.add(event);
        }
        synchronized (listener) {
            listener.notifyAll();
        }
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the foreground colors grid.
     */
    @Override
    public void draw() {

        // Sync on other screen, so that we do not draw in the middle of
        // their screen update.
        synchronized (drawLock) {
            // Draw the box
            super.draw();

            // Draw every cell of the other screen
            for (int y = 0; y < otherScreen.getHeight(); y++) {
                for (int x = 0; x < otherScreen.getWidth(); x++) {
                    Cell cell = otherScreen.getCharXY(x, y);
                    putCharXY(x + 1, y + 1, cell);
                    if (cell.getDisplayWidth() == 2) {
                        x++;
                    }
                }
            }

            // If their cursor is visible, draw that here too.
            if (otherScreen.isCursorVisible()) {
                setCursorX(otherScreen.getCursorX() + 1);
                setCursorY(otherScreen.getCursorY() + 1);
                setCursorVisible(true);
            } else {
                setCursorVisible(false);
            }
        }

        // Check if the other application has died.  If so, unset hidden
        // mouse.
        if (otherApplication != null) {
            if (otherApplication.isRunning() == false) {
                setHiddenMouse(false);
            }
        }

    }

    /**
     * Subclasses should override this method to cleanup resources.  This is
     * called by application.closeWindow().
     */
    @Override
    public void onClose() {
        synchronized (eventQueue) {
            eventQueue.add(new TCommandEvent(this, cmBackendDisconnect));
        }
    }

    /**
     * Called by application.switchWindow() when this window gets the
     * focus, and also by application.addWindow().
     */
    @Override
    protected void onFocus() {
        hasFocus = true;
    }

    /**
     * Called by application.switchWindow() when another window gets the
     * focus.
     */
    @Override
    protected void onUnfocus() {
        hasFocus = false;
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
     * Subclasses must provide an implementation that syncs the logical
     * screen to the physical device.
     */
    public void flushScreen() {
        getApplication().doRepaint();
    }

    /**
     * Check if there are events in the queue.
     *
     * @return if true, getEvents() has something to return to the application
     */
    public boolean hasEvents() {
        synchronized (eventQueue) {
            if (eventQueue.size() > 0) {
                return true;
            }
            long now = System.currentTimeMillis();
            sessionInfo.setIdleTime((int) (now - lastUserInputTime) / 1000);
            return false;
        }
    }

    /**
     * Subclasses must provide an implementation to get keyboard, mouse, and
     * screen resize events.
     *
     * @param queue list to append new events to
     */
    public void getEvents(List<TInputEvent> queue) {
        synchronized (eventQueue) {
            long now = System.currentTimeMillis();
            if (eventQueue.size() > 0) {
                lastUserInputTime = now;
                if (!readOnly) {
                    synchronized (queue) {
                        queue.addAll(eventQueue);
                    }
                }
                eventQueue.clear();
            }
            sessionInfo.setIdleTime((int) (now - lastUserInputTime) / 1000);
        }
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
        this.listener = listener;
    }

    /**
     * Reload backend options from System properties.
     */
    public void reloadOptions() {
        // NOP
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
        setHiddenMouse(!readOnly);
    }

    /**
     * Convert a CellAttributes foreground color to an RGB color.
     *
     * @param attr the text attributes
     * @return the RGB color
     */
    public int attrToForegroundColor(final CellAttributes attr) {
        return getApplication().getBackend().attrToForegroundColor(attr);
    }

    /**
     * Convert a CellAttributes background color to an RGB color.
     *
     * @param attr the text attributes
     * @return the RGB color
     */
    public int attrToBackgroundColor(final CellAttributes attr) {
        return getApplication().getBackend().attrToBackgroundColor(attr);
    }

    /**
     * Retrieve the default foreground color.
     *
     * @return the RGB color
     */
    public int getDefaultForeColorRGB() {
        return getApplication().getBackend().getDefaultForeColorRGB();
    }

    /**
     * Retrieve the default background color.
     *
     * @return the RGB color
     */
    public int getDefaultBackColorRGB() {
        return getApplication().getBackend().getDefaultForeColorRGB();
    }

    /**
     * Copy text to the Casciian clipboard.
     *
     * @param text string to copy
     */
    public void copyClipboardText(final String text) {
        getApplication().getClipboard().copyText(text);
    }

    /**
     * Get window/terminal system focus.
     *
     * @return true if this backend has the mouse/keyboard focus
     */
    public boolean isFocused() {
        return hasFocus;
    }

    // ------------------------------------------------------------------------
    // TWindowBackend ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set the object to sync to in draw().
     *
     * @param drawLock the object to synchronize on
     */
    public void setDrawLock(final Object drawLock) {
        this.drawLock = drawLock;
    }

    /**
     * Getter for the other application's screen.
     *
     * @return the Screen
     */
    public Screen getOtherScreen() {
        return otherScreen;
    }

    /**
     * Set the other screen's application.
     *
     * @param application the application driving the other screen
     */
    public void setOtherApplication(final TApplication application) {
        this.otherApplication = application;
    }

}
