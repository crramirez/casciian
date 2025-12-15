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

import java.util.HashSet;
import java.util.Set;

import casciian.backend.Screen;
import casciian.bits.BorderStyle;
import casciian.bits.CellAttributes;
import casciian.bits.CellTransform;
import casciian.bits.ColorTheme;
import casciian.bits.GraphicsChars;
import casciian.bits.StringUtils;
import casciian.event.TCommandEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TMenuEvent;
import casciian.event.TMouseEvent;
import casciian.event.TResizeEvent;
import casciian.menu.TMenu;
import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * TWindow is the top-level container and drawing surface for other widgets.
 */
public class TWindow extends TWidget {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Window is resizable (default yes).
     */
    public static final int RESIZABLE   = 0x01;

    /**
     * Window is modal (default no).
     */
    public static final int MODAL       = 0x02;

    /**
     * Window is centered (default no).
     */
    public static final int CENTERED    = 0x04;

    /**
     * Window has no close box (default no).  Window can still be closed via
     * TApplication.closeWindow() and TWindow.close().
     */
    public static final int NOCLOSEBOX  = 0x08;

    /**
     * Window has no maximize box (default no).
     */
    public static final int NOZOOMBOX   = 0x10;

    /**
     * Window is placed at absolute position (no smart placement) (default
     * no).
     */
    public static final int ABSOLUTEXY  = 0x20;

    /**
     * Hitting the closebox with the mouse calls TApplication.hideWindow()
     * rather than TApplication.closeWindow() (default no).
     */
    public static final int HIDEONCLOSE = 0x40;

    /**
     * Menus cannot be used when this window is active (default no).
     */
    public static final int OVERRIDEMENU        = 0x80;

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Window flags.  Note package private access.
     */
    int flags = RESIZABLE;

    /**
     * Window title.
     */
    private String title = null;

    /**
     * Window's parent TApplication.
     */
    private TApplication application;

    /**
     * Z order.  Lower number means more in-front.
     */
    private int z = 0;

    /**
     * Window's keyboard shortcuts.  Any key in this set will be passed to
     * the window directly rather than processed through the menu
     * accelerators.
     */
    private Set<TKeypress> keyboardShortcuts = new HashSet<TKeypress>();

    /**
     * If true, then the user clicked on the title bar and is moving the
     * window.
     */
    protected boolean inWindowMove = false;

    /**
     * If true, then the user clicked on the bottom right corner and is
     * resizing the window.
     */
    protected boolean inWindowResize = false;

    /**
     * If true, then the user selected "Size/Move" (or hit Ctrl-F5) and is
     * resizing/moving the window via the keyboard.
     */
    protected boolean inKeyboardResize = false;

    /**
     * If true, this window is maximized.
     */
    private boolean maximized = false;

    /**
     * Remember mouse state.
     */
    protected TMouseEvent mouse;

    // For moving the window.  resizing also uses moveWindowMouseX/Y
    private int moveWindowMouseX;
    private int moveWindowMouseY;
    private int oldWindowX;
    private int oldWindowY;

    // Resizing
    private int resizeWindowWidth;
    private int resizeWindowHeight;
    private int minimumWindowWidth = 10;
    private int minimumWindowHeight = 2;
    private int maximumWindowWidth = -1;
    private int maximumWindowHeight = -1;

    // For maximize/restore
    private int restoreWindowWidth;
    private int restoreWindowHeight;
    private int restoreWindowX;
    private int restoreWindowY;

    /**
     * Hidden flag.  A hidden window will still have its onIdle() called, and
     * will also have onClose() called at application exit.  Note package
     * private access: TApplication will force hidden false if a modal window
     * is active.
     */
    boolean hidden = false;

    /**
     * Widgets on this window can pull colors from this ColorTheme.  Note
     * package private access: if null, TWidget.getTheme() will pull from
     * TApplication.
     */
    ColorTheme theme;

    /**
     * A window may have a status bar associated with it.  TApplication will
     * draw this status bar last, and will also route events to it first
     * before the window.
     */
    protected TStatusBar statusBar = null;

    /**
     * A window may request that TApplication NOT draw the mouse cursor over
     * it by setting this to true.  This is currently only used within
     * Casciian by TTerminalWindow so that only the bottom-most instance of
     * nested Casciian's draws the mouse within its application window.  But
     * perhaps other applications can use it, so public getter/setter is
     * provided.
     */
    private boolean hideMouse = false;

    /**
     * The help topic for this window.
     */
    protected String helpTopic = "Help";

    /**
     * The border style for an active window.
     */
    protected BorderStyle borderStyleActive = BorderStyle.DOUBLE;

    /**
     * The border style for an active modal window.
     */
    protected BorderStyle borderStyleActiveModal = BorderStyle.DOUBLE;

    /**
     * The border style for an inactive window.
     */
    protected BorderStyle borderStyleInactive = BorderStyle.SINGLE;

    /**
     * The border style for a window being dragged/resized.
     */
    protected BorderStyle borderStyleMoving = BorderStyle.SINGLE;

    /**
     * The alpha blending value.
     */
    private int alpha = 255;

    /**
     * The window open effect timer.
     */
    private TTimer openEffectTimer = null;

    /**
     * Cell transform applied after the window background is drawn, but
     * before widgets are drawn.
     */
    private CellTransform drawPreTransform = null;

    /**
     * Widget to apply drawPreTransform relative to. If null, base it on
     * screen coordinates. Note package private access.
     */
    TWidget drawPreTransformWidget = null;

    /**
     * Cell transform applied after the widgets are drawn.
     */
    private CellTransform drawPostTransform = null;

    /*
     * Widget to apply drawPostTransform relative to. If null, base it on
     * screen coordinates. Note package private access.
     */
    TWidget drawPostTransformWidget = null;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.  Window will be located at (0, 0).
     *
     * @param application TApplication that manages this window
     * @param title window title, will be centered along the top border
     * @param width width of window
     * @param height height of window
     */
    public TWindow(final TApplication application, final String title,
        final int width, final int height) {

        this(application, title, 0, 0, width, height, RESIZABLE);
    }

    /**
     * Public constructor.  Window will be located at (0, 0).
     *
     * @param application TApplication that manages this window
     * @param title window title, will be centered along the top border
     * @param width width of window
     * @param height height of window
     * @param flags bitmask of RESIZABLE, CENTERED, or MODAL
     */
    public TWindow(final TApplication application, final String title,
        final int width, final int height, final int flags) {

        this(application, title, 0, 0, width, height, flags);
    }

    /**
     * Public constructor.
     *
     * @param application TApplication that manages this window
     * @param title window title, will be centered along the top border
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of window
     * @param height height of window
     */
    public TWindow(final TApplication application, final String title,
        final int x, final int y, final int width, final int height) {

        this(application, title, x, y, width, height, RESIZABLE);
    }

    /**
     * Public constructor.
     *
     * @param application TApplication that manages this window
     * @param title window title, will be centered along the top border
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of window
     * @param height height of window
     * @param flags mask of RESIZABLE, CENTERED, or MODAL
     */
    @SuppressWarnings("this-escape")
    public TWindow(final TApplication application, final String title,
        final int x, final int y, final int width, final int height,
        final int flags) {

        super();

        // I am my own window and parent
        setupForTWindow(this, x, y + application.getDesktopTop(),
            width, height);

        // Save fields
        this.title       = title;
        this.application = application;
        this.flags       = flags;

        // Minimum width/height are 10 and 2
        assert (width >= 10);
        assert (getHeight() >= 2);

        // MODAL implies CENTERED
        if (isModal()) {
            this.flags |= CENTERED;
        }

        // Center window if specified
        center();

        // Set default borders
        setBorderStyleForeground(null);
        setBorderStyleInactive(null);
        setBorderStyleModal(null);
        setBorderStyleMoving(null);

        int opacity = 95;
        try {
            opacity = Integer.parseInt(System.getProperty(
                "casciian.TWindow.opacity", "95"));
            opacity = Math.max(opacity, 10);
            opacity = Math.min(opacity, 100);
        } catch (NumberFormatException e) {
            // SQUASH
        }
        setAlpha(opacity * 255 / 100);

        // Add me to the application
        application.addWindowToApplication(this);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Returns true if the mouse is currently on the close button.
     *
     * @return true if mouse is currently on the close button
     */
    protected boolean mouseOnClose() {
        if ((flags & NOCLOSEBOX) != 0) {
            return false;
        }
        if ((mouse != null)
            && (mouse.getAbsoluteY() == getY())
            && (mouse.getAbsoluteX() == getX() + 3)
        ) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if the mouse is currently on the maximize/restore button.
     *
     * @return true if the mouse is currently on the maximize/restore button
     */
    protected boolean mouseOnMaximize() {
        if ((flags & NOZOOMBOX) != 0) {
            return false;
        }
        if ((mouse != null)
            && !isModal()
            && (mouse.getAbsoluteY() == getY())
            && (mouse.getAbsoluteX() == getX() + getWidth() - 4)
        ) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if the mouse is currently on the resizable lower right
     * corner.
     *
     * @return true if the mouse is currently on the resizable lower right
     * corner
     */
    protected boolean mouseOnResize() {
        if (((flags & RESIZABLE) != 0)
            && (getBorderStyle() != BorderStyle.NONE)
            && (mouse != null)
            && (mouse.getAbsoluteY() == getY() + getHeight() - 1)
            && ((mouse.getAbsoluteX() == getX() + getWidth() - 1)
                || (mouse.getAbsoluteX() == getX() + getWidth() - 2))
        ) {
            return true;
        }
        return false;
    }

    /**
     * Subclasses should override this method to perform any user prompting
     * before they are offscreen.  Note that unlike other windowing toolkits,
     * windows can NOT use this function in some manner to avoid being
     * closed.  This is called by application.closeWindow().
     */
    protected void onPreClose() {
        // Default: do nothing.
    }

    /**
     * Subclasses should override this method to cleanup resources.  This is
     * called by application.closeWindow().
     */
    protected void onClose() {
        // Default: perform widget-specific cleanup.
        while (getChildren().size() > 0) {
            TWidget w = getChildren().get(0);
            w.close();
            if (getChildren().contains(w)) {
                getChildren().remove(w);
            }
        }
    }

    /**
     * Called by application.switchWindow() when this window gets the
     * focus, and also by application.addWindow().
     */
    protected void onFocus() {
        // Default: do nothing
    }

    /**
     * Called by application.switchWindow() when another window gets the
     * focus.
     */
    protected void onUnfocus() {
        // Default: do nothing
    }

    /**
     * Called by application.hideWindow().
     */
    protected void onHide() {
        // Default: do nothing
    }

    /**
     * Called by application.showWindow().
     */
    protected void onShow() {
        // Default: do nothing
    }

    /**
     * Handle mouse button presses.
     *
     * @param mouse mouse button event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        this.mouse = mouse;

        inKeyboardResize = false;
        inWindowMove = false;
        inWindowResize = false;

        if ((mouse.getAbsoluteY() == getY())
            && mouse.isMouse1()
            && (getX() <= mouse.getAbsoluteX())
            && (mouse.getAbsoluteX() < getX() + getWidth())
            && !mouseOnClose()
            && !mouseOnMaximize()
        ) {
            // Begin moving window
            inWindowMove = true;
            moveWindowMouseX = mouse.getAbsoluteX();
            moveWindowMouseY = mouse.getAbsoluteY();
            oldWindowX = getX();
            oldWindowY = getY();
            if (maximized) {
                maximized = false;
            }
            return;
        }
        if (mouseOnResize()) {
            // Begin window resize
            inWindowResize = true;
            moveWindowMouseX = mouse.getAbsoluteX();
            moveWindowMouseY = mouse.getAbsoluteY();
            resizeWindowWidth = getWidth();
            resizeWindowHeight = getHeight();
            if (maximized) {
                maximized = false;
            }
            return;
        }

        // Give the shortcut bar a shot at this.
        if (statusBar != null) {
            if (statusBar.statusBarMouseDown(mouse)) {
                return;
            }
        }

        // I didn't take it, pass it on to my children
        super.onMouseDown(mouse);
    }

    /**
     * Handle mouse button releases.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        this.mouse = mouse;

        if ((inWindowMove) && (mouse.isMouse1())) {
            // Stop moving window
            inWindowMove = false;
            return;
        }

        if ((inWindowResize) && (mouse.isMouse1())) {
            // Stop resizing window
            inWindowResize = false;
            return;
        }

        if (mouse.isMouse1() && mouseOnClose()) {
            if ((flags & HIDEONCLOSE) == 0) {
                // Close window
                application.closeWindow(this);
            } else {
                // Hide window
                application.hideWindow(this);
            }
            return;
        }

        if ((mouse.getAbsoluteY() == getY())
            && mouse.isMouse1()
            && mouseOnMaximize()) {
            if (maximized) {
                // Restore
                restore();
            } else {
                // Maximize
                maximize();
            }
            // Pass a resize event to my children
            onResize(new TResizeEvent(mouse.getBackend(),
                    TResizeEvent.Type.WIDGET, getWidth(), getHeight()));
            return;
        }

        // Give the shortcut bar a shot at this.
        if (statusBar != null) {
            if (statusBar.statusBarMouseUp(mouse)) {
                return;
            }
        }

        // I didn't take it, pass it on to my children
        super.onMouseUp(mouse);
    }

    /**
     * Handle mouse movements.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        this.mouse = mouse;

        if (inWindowMove) {
            // Move window over
            setX(oldWindowX + (mouse.getAbsoluteX() - moveWindowMouseX));
            setY(oldWindowY + (mouse.getAbsoluteY() - moveWindowMouseY));
            // Don't cover up the menu bar
            if (getY() < application.getDesktopTop()) {
                setY(application.getDesktopTop());
            }
            // Don't go below the status bar
            if (getY() >= application.getDesktopBottom()) {
                setY(application.getDesktopBottom() - 1);
            }
            return;
        }

        if (inWindowResize) {
            // Move window over
            int newWidth = resizeWindowWidth + (mouse.getAbsoluteX()
                    - moveWindowMouseX);
            int newHeight = resizeWindowHeight + (mouse.getAbsoluteY()
                    - moveWindowMouseY);
            if (getX() + newWidth > getScreen().getWidth()) {
                newWidth = getScreen().getWidth() - getX();
            }
            if (newHeight + getY() >= getApplication().getDesktopBottom()) {
                newHeight = getApplication().getDesktopBottom() - getY();
            }

            // Keep within min/max bounds
            newWidth = Math.max(newWidth, minimumWindowWidth);
            if (maximumWindowWidth > minimumWindowWidth) {
                newWidth = Math.min(newWidth, maximumWindowWidth);
            }
            newHeight = Math.max(newHeight, minimumWindowHeight);
            if (maximumWindowHeight > minimumWindowHeight) {
                newHeight = Math.min(newHeight, maximumWindowHeight);
            }

            setWidth(newWidth);
            setHeight(newHeight);

            // Don't cover up the status bar
            if (getY() + newHeight > application.getDesktopBottom()) {
                setY(application.getDesktopBottom() - newHeight + 1);
            }
            // Don't cover up the menu bar
            if (getY() < application.getDesktopTop()) {
                setY(application.getDesktopTop());
            }

            // Pass a resize event to my children
            onResize(new TResizeEvent(mouse.getBackend(),
                    TResizeEvent.Type.WIDGET, getWidth(), getHeight()));
            return;
        }

        // Give the shortcut bar a shot at this.
        if (statusBar != null) {
            statusBar.statusBarMouseMotion(mouse);
        }

        // I didn't take it, pass it on to my children
        super.onMouseMotion(mouse);
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {

        if (inWindowMove || inWindowResize) {
            // ESC or ENTER - Exit size/move
            if (keypress.equals(kbEsc) || keypress.equals(kbEnter)) {
                inWindowMove = false;
                inWindowResize = false;
                return;
            }
        }

        if (inKeyboardResize) {

            // ESC or ENTER - Exit size/move
            if (keypress.equals(kbEsc) || keypress.equals(kbEnter)) {
                inKeyboardResize = false;
            }

            if (keypress.equals(kbLeft)) {
                if (getX() > 0) {
                    setX(getX() - 1);
                }
            }
            if (keypress.equals(kbRight)) {
                if (getX() < getScreen().getWidth() - 1) {
                    setX(getX() + 1);
                }
            }
            if (keypress.equals(kbDown)) {
                if (getY() < application.getDesktopBottom() - 1) {
                    setY(getY() + 1);
                }
            }
            if (keypress.equals(kbUp)) {
                if (getY() > 1) {
                    setY(getY() - 1);
                }
            }

            /*
             * Only permit keyboard resizing if the window was RESIZABLE and
             * there is a window border.
             */
            if (((flags & RESIZABLE) != 0)
                && (getBorderStyle() != BorderStyle.NONE)
            ) {

                if (keypress.equals(kbShiftLeft)) {
                    if ((getWidth() > minimumWindowWidth)
                        || (minimumWindowWidth <= 0)
                    ) {
                        setWidth(getWidth() - 1);
                    }
                }
                if (keypress.equals(kbShiftRight)) {
                    if ((getWidth() < maximumWindowWidth)
                        || (maximumWindowWidth <= 0)
                    ) {
                        setWidth(getWidth() + 1);
                    }
                }
                if (keypress.equals(kbShiftUp)) {
                    if ((getHeight() > minimumWindowHeight)
                        || (minimumWindowHeight <= 0)
                    ) {
                        setHeight(getHeight() - 1);
                    }
                }
                if (keypress.equals(kbShiftDown)) {
                    if ((getHeight() < maximumWindowHeight)
                        || (maximumWindowHeight <= 0)
                    ) {
                        setHeight(getHeight() + 1);
                    }
                }

                // Pass a resize event to my children
                onResize(new TResizeEvent(keypress.getBackend(),
                        TResizeEvent.Type.WIDGET, getWidth(), getHeight()));

            } // if ((flags & RESIZABLE) != 0)

            return;
        }

        // Give the shortcut bar a shot at this.
        if (statusBar != null) {
            if (statusBar.statusBarKeypress(keypress)) {
                return;
            }
        }

        // These keystrokes will typically not be seen unless a subclass
        // overrides onMenu() due to how TApplication dispatches
        // accelerators.

        if (!(this instanceof TDesktop)) {

            // Ctrl-W - close window
            if (keypress.equals(kbCtrlW)) {
                if ((flags & NOCLOSEBOX) == 0) {
                    if ((flags & HIDEONCLOSE) == 0) {
                        // Close window
                        application.closeWindow(this);
                    } else {
                        // Hide window
                        application.hideWindow(this);
                    }
                }
                return;
            }

            // F6 - behave like Alt-TAB
            if (keypress.equals(kbF6)) {
                application.switchWindow(true);
                return;
            }

            // Shift-F6 - behave like Shift-Alt-TAB
            if (keypress.equals(kbShiftF6)) {
                application.switchWindow(false);
                return;
            }

            // F5 - zoom
            if (keypress.equals(kbF5) && ((flags & NOZOOMBOX) == 0)) {
                if (maximized) {
                    restore();
                } else {
                    maximize();
                }
            }

            // Ctrl-F5 - size/move
            if (keypress.equals(kbCtrlF5)) {
                inKeyboardResize = !inKeyboardResize;
            }

        } // if (!(this instanceof TDesktop))

        // I didn't take it, pass it on to my children
        super.onKeypress(keypress);
    }

    /**
     * Handle posted command events.
     *
     * @param command command event
     */
    @Override
    public void onCommand(final TCommandEvent command) {

        // These commands will typically not be seen unless a subclass
        // overrides onMenu() due to how TApplication dispatches
        // accelerators.

        if (!(this instanceof TDesktop)) {

            if (command.equals(cmWindowClose)) {
                if ((flags & NOCLOSEBOX) == 0) {
                    if ((flags & HIDEONCLOSE) == 0) {
                        // Close window
                        application.closeWindow(this);
                    } else {
                        // Hide window
                        application.hideWindow(this);
                    }
                }
                return;
            }

            if (command.equals(cmWindowNext)) {
                application.switchWindow(true);
                return;
            }

            if (command.equals(cmWindowPrevious)) {
                application.switchWindow(false);
                return;
            }

            if (command.equals(cmWindowMove)) {
                inKeyboardResize = true;
                return;
            }

            if (command.equals(cmWindowZoom) && ((flags & NOZOOMBOX) == 0)) {
                if (maximized) {
                    restore();
                } else {
                    maximize();
                }
            }

        } // if (!(this instanceof TDesktop))

        // I didn't take it, pass it on to my children
        super.onCommand(command);
    }

    /**
     * Handle posted menu events.
     *
     * @param menu menu event
     */
    @Override
    public void onMenu(final TMenuEvent menu) {

        if (!(this instanceof TDesktop)) {

            if (menu.getId() == TMenu.MID_WINDOW_CLOSE) {
                if ((flags & NOCLOSEBOX) == 0) {
                    if ((flags & HIDEONCLOSE) == 0) {
                        // Close window
                        application.closeWindow(this);
                    } else {
                        // Hide window
                        application.hideWindow(this);
                    }
                }
                return;
            }

            if (menu.getId() == TMenu.MID_WINDOW_NEXT) {
                application.switchWindow(true);
                return;
            }

            if (menu.getId() == TMenu.MID_WINDOW_PREVIOUS) {
                application.switchWindow(false);
                return;
            }

            if (menu.getId() == TMenu.MID_WINDOW_MOVE) {
                inKeyboardResize = true;
                return;
            }

            if ((menu.getId() == TMenu.MID_WINDOW_ZOOM)
                && ((flags & NOZOOMBOX) == 0)
            ) {
                if (maximized) {
                    restore();
                } else {
                    maximize();
                }
                return;
            }

        } // if (!(this instanceof TDesktop))

        // I didn't take it, pass it on to my children
        super.onMenu(menu);
    }

    /**
     * Method that subclasses can override to handle window/screen resize
     * events.
     *
     * @param resize resize event
     */
    @Override
    public void onResize(final TResizeEvent resize) {
        if (resize.getType() == TResizeEvent.Type.WIDGET) {
            if (getChildren().size() == 1) {
                TWidget child = getChildren().get(0);
                if ((child instanceof TSplitPane)
                    || (child instanceof TPanel)
                ) {
                    if (this instanceof TDesktop) {
                        child.onResize(new TResizeEvent(resize.getBackend(),
                            TResizeEvent.Type.WIDGET,
                            resize.getWidth(), resize.getHeight()));
                    } else {
                        child.onResize(new TResizeEvent(resize.getBackend(),
                            TResizeEvent.Type.WIDGET,
                            resize.getWidth() - 2, resize.getHeight() - 2));
                    }
                }
                return;
            }
        }

        // Pass on to TWidget.
        super.onResize(resize);
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get this TWindow's parent TApplication.
     *
     * @return this TWindow's parent TApplication
     */
    @Override
    public final TApplication getApplication() {
        return application;
    }

    /**
     * Get the Screen.
     *
     * @return the Screen
     */
    @Override
    public final Screen getScreen() {
        return application.getScreen();
    }

    /**
     * Called by TApplication.drawChildren() to render on screen.
     */
    @Override
    public void draw() {
        // Draw the box and background first.
        CellAttributes border = getBorder();
        CellAttributes background = getBackground();
        BorderStyle borderStyle = getBorderStyle();
        drawBox(0, 0, getWidth(), getHeight(), border, background, borderStyle,
            !getApplication().hasTranslucence());

        if ((title != null) && (title.length() > 0)) {
            // Draw the title
            int titleLength = StringUtils.width(title);
            int titleLeft = (getWidth() - titleLength - 2) / 2;
            putCharXY(titleLeft, 0, ' ', border);
            putStringXY(titleLeft + 1, 0, title, border);
            putCharXY(titleLeft + titleLength + 1, 0, ' ', border);
        }

        if (isActive()) {
            int lBracket = '[';
            int rBracket = ']';
            if (borderStyle.equals(BorderStyle.SINGLE_ROUND)) {
                lBracket = '(';
                rBracket = ')';
            }

            // Draw the close button
            if ((flags & NOCLOSEBOX) == 0) {
                putCharXY(2, 0, lBracket, border);
                putCharXY(4, 0, rBracket, border);
                if (mouseOnClose() && mouse.isMouse1()) {
                    putCharXY(3, 0, GraphicsChars.CP437[0x0F],
                        getBorderControls());
                } else {
                    putCharXY(3, 0, GraphicsChars.CP437[0xFE],
                        getBorderControls());
                }
            }

            // Draw the maximize button
            if (!isModal() && ((flags & NOZOOMBOX) == 0)) {

                putCharXY(getWidth() - 5, 0, lBracket, border);
                putCharXY(getWidth() - 3, 0, rBracket, border);
                if (mouseOnMaximize() && mouse.isMouse1()) {
                    putCharXY(getWidth() - 4, 0, GraphicsChars.CP437[0x0F],
                        getBorderControls());
                } else {
                    if (maximized) {
                        putCharXY(getWidth() - 4, 0, GraphicsChars.CP437[0x12],
                            getBorderControls());
                    } else {
                        putCharXY(getWidth() - 4, 0, GraphicsChars.UPARROW,
                            getBorderControls());
                    }
                }
            }

            // Draw the resize corner
            if (((flags & RESIZABLE) != 0)
                && (getBorderStyle() != BorderStyle.NONE)
            ) {
                if ((flags & RESIZABLE) != 0) {
                    putCharXY(getWidth() - 2, getHeight() - 1,
                        getBorderStyle().getHorizontal(), getBorderControls());
                    putCharXY(getWidth() - 1, getHeight() - 1,
                        getBorderStyle().getBottomRight(), getBorderControls());
                }
            }
        }

        if (drawPreTransform != null) {
            applyCellTransform(0, 0, getWidth(), getHeight(), drawPreTransform,
                drawPreTransformWidget);
        }

    }

    /**
     * Function called after the window is drawn, its post-draw cell
     * transform applied.  This can be used by subclasses of TWindow to alter
     * the final post-rendered window screen area.
     */
    protected void onPostDraw() {
        // Default does nothing
    }

    /**
     * Function called immediately after the window is drawn.  Note package
     * private access.
     */
    void onPostDrawCellTransform() {
        if (drawPostTransform != null) {
            applyCellTransform(0, 0, getWidth(), getHeight(),
                drawPostTransform, drawPostTransformWidget);
        }
    }

    /**
     * Get the mouse pointer (cursor) style.
     *
     * @return the pointer style string, one of: "default", "none",
     * "hand", "text", "move", or "crosshair"
     */
    @Override
    public String getMouseStyle() {
        return System.getProperty("casciian.Swing.mouseStyle", "default");
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get window title.
     *
     * @return window title
     */
    public final String getTitle() {
        return title;
    }

    /**
     * Set window title.
     *
     * @param title new window title
     */
    public final void setTitle(final String title) {
        this.title = title;
    }

    /**
     * Get Z order.  Lower number means more in-front.
     *
     * @return Z value.  Lower number means more in-front.
     */
    public final int getZ() {
        return z;
    }

    /**
     * Set Z order.  Lower number means more in-front.
     *
     * @param z the new Z value.  Lower number means more in-front.
     */
    public final void setZ(final int z) {
        this.z = z;
    }

    /**
     * Add a keypress to be overridden for this window.
     *
     * @param key the key to start taking control of
     */
    public void addShortcutKeypress(final TKeypress key) {
        keyboardShortcuts.add(key);
    }

    /**
     * Remove a keypress to be overridden for this window.
     *
     * @param key the key to stop taking control of
     */
    public void removeShortcutKeypress(final TKeypress key) {
        keyboardShortcuts.remove(key);
    }

    /**
     * Remove all keypresses to be overridden for this window.
     */
    protected void clearShortcutKeypresses() {
        keyboardShortcuts.clear();
    }

    /**
     * Determine if a keypress is overridden for this window.
     *
     * @param key the key to check
     * @return true if this window wants to process this key on its own
     */
    public boolean isShortcutKeypress(final TKeypress key) {
        return keyboardShortcuts.contains(key);
    }

    /**
     * Get the window's status bar, or null if it does not have one.
     *
     * @return the status bar, or null
     */
    public TStatusBar getStatusBar() {
        return statusBar;
    }

    /**
     * Set the window's status bar to a new one.
     *
     * @param text the status bar text
     * @return the status bar
     */
    public TStatusBar newStatusBar(final String text) {
        statusBar = new TStatusBar(this, text);
        return statusBar;
    }

    /**
     * Get the maximum width for this window.
     *
     * @return the maximum width
     */
    public final int getMaximumWindowWidth() {
        return maximumWindowWidth;
    }

    /**
     * Set the maximum width for this window.
     *
     * @param maximumWindowWidth new maximum width
     */
    public final void setMaximumWindowWidth(final int maximumWindowWidth) {
        if ((maximumWindowWidth != -1)
            && (maximumWindowWidth < minimumWindowWidth + 1)
        ) {
            throw new IllegalArgumentException("Maximum window width cannot " +
                "be smaller than minimum window width + 1");
        }
        this.maximumWindowWidth = maximumWindowWidth;
    }

    /**
     * Get the minimum width for this window.
     *
     * @return the minimum width
     */
    public final int getMinimumWindowWidth() {
        return minimumWindowWidth;
    }

    /**
     * Set the minimum width for this window.
     *
     * @param minimumWindowWidth new minimum width
     */
    public final void setMinimumWindowWidth(final int minimumWindowWidth) {
        if ((maximumWindowWidth != -1)
            && (minimumWindowWidth > maximumWindowWidth - 1)
        ) {
            throw new IllegalArgumentException("Minimum window width cannot " +
                "be larger than maximum window width - 1");
        }
        this.minimumWindowWidth = minimumWindowWidth;
    }

    /**
     * Get the maximum height for this window.
     *
     * @return the maximum height
     */
    public final int getMaximumWindowHeight() {
        return maximumWindowHeight;
    }

    /**
     * Set the maximum height for this window.
     *
     * @param maximumWindowHeight new maximum height
     */
    public final void setMaximumWindowHeight(final int maximumWindowHeight) {
        if ((maximumWindowHeight != -1)
            && (maximumWindowHeight < minimumWindowHeight + 1)
        ) {
            throw new IllegalArgumentException("Maximum window height cannot " +
                "be smaller than minimum window height + 1");
        }
        this.maximumWindowHeight = maximumWindowHeight;
    }

    /**
     * Get the minimum height for this window.
     *
     * @return the minimum height
     */
    public final int getMinimumWindowHeight() {
        return minimumWindowHeight;
    }

    /**
     * Set the minimum height for this window.
     *
     * @param minimumWindowHeight new minimum height
     */
    public final void setMinimumWindowHeight(final int minimumWindowHeight) {
        if ((maximumWindowHeight != -1)
            && (minimumWindowHeight > maximumWindowHeight - 1)
        ) {
            throw new IllegalArgumentException("Minimum window height cannot " +
                "be larger than maximum window height - 1");
        }
        this.minimumWindowHeight = minimumWindowHeight;
    }

    /**
     * Recenter the window on-screen.
     */
    public final void center() {
        if ((flags & CENTERED) != 0) {
            if (getWidth() < getScreen().getWidth()) {
                setX((getScreen().getWidth() - getWidth()) / 2);
            } else {
                setX(0);
            }
            setY(((application.getDesktopBottom()
                    - application.getDesktopTop()) - getHeight()) / 2);
            if (getY() < 0) {
                setY(0);
            }
            setY(getY() + application.getDesktopTop());
        }
    }

    /**
     * Move the window as needed to ensure it is visible on screen.
     */
    public void ensureOnScreen() {
        if (getX() < 0) {
            setX(0);
        }
        if (getY() < getApplication().getDesktopTop()) {
            setY(getApplication().getDesktopTop());
        }
        while (getX() + getWidth() > getScreen().getWidth()) {
            setX(getX() - 1);
        }
        while (getY() + getHeight() > getApplication().getDesktopBottom()) {
            setY(getY() - 1);
        }
    }

    /**
     * Maximize window.
     */
    public void maximize() {
        if (maximized) {
            return;
        }

        restoreWindowWidth = getWidth();
        restoreWindowHeight = getHeight();
        restoreWindowX = getX();
        restoreWindowY = getY();
        setWidth(getScreen().getWidth());
        setHeight(application.getDesktopBottom() - application.getDesktopTop());
        setX(0);
        setY(application.getDesktopTop());
        maximized = true;

        onResize(new TResizeEvent(null, TResizeEvent.Type.WIDGET, getWidth(),
                getHeight()));
    }

    /**
     * Restore (unmaximize) window.
     */
    public void restore() {
        if (!maximized) {
            return;
        }

        setWidth(restoreWindowWidth);
        setHeight(restoreWindowHeight);
        setX(restoreWindowX);
        setY(restoreWindowY);
        maximized = false;

        onResize(new TResizeEvent(null, TResizeEvent.Type.WIDGET, getWidth(),
                getHeight()));
    }

    /**
     * Returns true if this window is hidden.
     *
     * @return true if this window is hidden, false if the window is shown
     */
    public final boolean isHidden() {
        return hidden;
    }

    /**
     * Returns true if this window is shown.
     *
     * @return true if this window is shown, false if the window is hidden
     */
    public final boolean isShown() {
        return !hidden;
    }

    /**
     * Hide window.  A hidden window will still have its onIdle() called, and
     * will also have onClose() called at application exit.  Hidden windows
     * will not receive any other events.
     */
    public void hide() {
        application.hideWindow(this);
    }

    /**
     * Show window.
     */
    public void show() {
        application.showWindow(this);
    }

    /**
     * Activate window (bring to top and receive events).
     */
    @Override
    public void activate() {
        application.activateWindow(this);
    }

    /**
     * Close window.  Note that windows without a close box can still be
     * closed by calling the close() method.
     */
    @Override
    public void close() {
        application.closeWindow(this);
    }

    /**
     * See if this window is undergoing any movement/resize/etc.
     *
     * @return true if the window is moving
     */
    public boolean inMovements() {
        if (inWindowResize || inWindowMove || inKeyboardResize) {
            return true;
        }
        return false;
    }

    /**
     * Stop any pending movement/resize/etc.
     */
    public void stopMovements() {
        inWindowResize = false;
        inWindowMove = false;
        inKeyboardResize = false;
    }

    /**
     * Returns true if this window is modal.
     *
     * @return true if this window is modal
     */
    public final boolean isModal() {
        if ((flags & MODAL) == 0) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if this window has a close box.
     *
     * @return true if this window has a close box
     */
    public final boolean hasCloseBox() {
        if ((flags & NOCLOSEBOX) != 0) {
            return true;
        }
        return false;
    }

    /**
     * Set this window's close box flag.
     *
     * @param closeBox if true, this window will have a close box
     */
    public final void setCloseBox(final boolean closeBox) {
        if (closeBox) {
            flags &= ~NOCLOSEBOX;
        } else {
            flags |= NOCLOSEBOX;
        }
    }

    /**
     * Returns true if this window has a maximize/zoom box.
     *
     * @return true if this window has a maximize/zoom box
     */
    public final boolean hasZoomBox() {
        if ((flags & NOZOOMBOX) != 0) {
            return true;
        }
        return false;
    }

    /**
     * Set this window's maximize/zoom box flag.
     *
     * @param zoomBox if true, this window will have a maximize/zoom box
     */
    public final void setZoomBox(final boolean zoomBox) {
        if (zoomBox) {
            flags &= ~NOZOOMBOX;
        } else {
            flags |= NOZOOMBOX;
        }
    }

    /**
     * Returns true if this window does not want menus to work while it is
     * visible.
     *
     * @return true if this window does not want menus to work while it is
     * visible
     */
    public final boolean hasOverriddenMenu() {
        if ((flags & OVERRIDEMENU) != 0) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if this window is resizable.
     *
     * @return true if this window is resizable
     */
    public final boolean isResizable() {
        if (((flags & RESIZABLE) == 0)
            || (getBorderStyle() == BorderStyle.NONE)
        ) {
            return false;
        }
        return true;
    }

    /**
     * Set this window's resizable flag.
     *
     * @param resizable if true, this window will be resizable
     */
    public final void setResizable(final boolean resizable) {
        if (resizable) {
            flags |= RESIZABLE;
        } else {
            flags &= ~RESIZABLE;
        }
    }

    /**
     * Retrieve the background color.
     *
     * @return the background color
     */
    public CellAttributes getBackground() {
        if (!isModal()
            && (inWindowMove || inWindowResize || inKeyboardResize)
        ) {
            assert (isActive());
            return getTheme().getColor("twindow.background.windowmove");
        } else if (isModal() && (inWindowMove || inWindowResize)) {
            assert (isActive());
            return getTheme().getColor("twindow.background.modal");
        } else if (isModal()) {
            if (isActive()) {
                return getTheme().getColor("twindow.background.modal");
            }
            return getTheme().getColor("twindow.background.modal.inactive");
        } else if (isActive()) {
            assert (!isModal());
            return getTheme().getColor("twindow.background");
        } else {
            assert (!isModal());
            return getTheme().getColor("twindow.background.inactive");
        }
    }

    /**
     * Retrieve the border color.
     *
     * @return the border color
     */
    public CellAttributes getBorder() {
        if (!isModal()
            && (inWindowMove || inWindowResize || inKeyboardResize)
        ) {
            if (!isActive()) {
                // The user's terminal never passed a mouse up event, and now
                // another window is active but we never finished a drag.
                inWindowMove = false;
                inWindowResize = false;
                inKeyboardResize = false;
                return getTheme().getColor("twindow.border.inactive");
            }

            return getTheme().getColor("twindow.border.windowmove");
        } else if (isModal() && (inWindowMove || inWindowResize)) {
            assert (isActive());
            return getTheme().getColor("twindow.border.modal.windowmove");
        } else if (isModal()) {
            if (isActive()) {
                return getTheme().getColor("twindow.border.modal");
            } else {
                return getTheme().getColor("twindow.border.modal.inactive");
            }
        } else if (isActive()) {
            assert (!isModal());
            return getTheme().getColor("twindow.border");
        } else {
            assert (!isModal());
            return getTheme().getColor("twindow.border.inactive");
        }
    }

    /**
     * Retrieve the color used by the window movement/sizing controls.
     *
     * @return the color used by the zoom box, resize bar, and close box
     */
    public CellAttributes getBorderControls() {
        if (isModal()) {
            return getTheme().getColor("twindow.border.modal.windowmove");
        }
        return getTheme().getColor("twindow.border.windowmove");
    }

    /**
     * Retrieve the border line style.
     *
     * @return the border line style
     */
    public BorderStyle getBorderStyle() {
        if (!isModal()
            && (inWindowMove || inWindowResize || inKeyboardResize)
        ) {
            assert (isActive());
            return borderStyleMoving;
        } else if (isModal() && (inWindowMove || inWindowResize)) {
            assert (isActive());
            // Modals cannot be resized via keyboard, hence the separate
            // check.
            return borderStyleMoving;
        } else if (isModal()) {
            if (isActive()) {
                return borderStyleActiveModal;
            } else {
                // One can stack modals, so an inactive but modal window is
                // possible.
                return borderStyleInactive;
            }
        } else if (isActive()) {
            return borderStyleActive;
        } else {
            return borderStyleInactive;
        }
    }

    /**
     * Returns true if this window does not want the application-wide mouse
     * pointer drawn over it.
     *
     * @return true if this window does not want the application-wide mouse
     * pointer drawn over it
     */
    public boolean hasHiddenMouse() {
        return hideMouse;
    }

    /**
     * Set request to prevent the application-wide mouse pointer from being
     * drawn over this window.
     *
     * @param hideMouse if true, this window does not want the
     * application-wide mouse pointer drawn over it
     */
    public final void setHiddenMouse(final boolean hideMouse) {
        this.hideMouse = hideMouse;
    }

    /**
     * Get this window's help topic to load.
     *
     * @return the topic name
     */
    public String getHelpTopic() {
        return helpTopic;
    }

    /**
     * Generate a human-readable string for this window.
     *
     * @return a human-readable string
     */
    @Override
    public String toString() {
        return String.format("%s(%8x) \'%s\' Z %d position (%d, %d) " +
            "geometry %dx%d  hidden %s modal %s",
            getClass().getName(), hashCode(), title, getZ(),
            getX(), getY(), getWidth(), getHeight(), hidden, isModal());
    }

    /**
     * Set the border style for the window when it is the foreground window.
     *
     * @param borderStyle the border style string, one of: "default", "none",
     * "single", "double", "singleVdoubleH", "singleHdoubleV", or "round"; or
     * null to use the value from casciian.TWindow.borderStyleForeground.
     */
    public void setBorderStyleForeground(final String borderStyle) {
        if (borderStyle == null) {
            String style = System.getProperty("casciian.TWindow.borderStyleForeground",
                "double");
            borderStyleActive = BorderStyle.getStyle(style);
        } else if (borderStyle.equals("default")) {
            borderStyleActive = BorderStyle.DOUBLE;
        } else {
            borderStyleActive = BorderStyle.getStyle(borderStyle);
        }
    }

    /**
     * Get the border style for the window when it is the foreground window.
     *
     * @return the border style
     */
    public BorderStyle getBorderStyleForeground() {
        return borderStyleActive;
    }

    /**
     * Set the border style for the window when it is the modal window.
     *
     * @param borderStyle the border style string, one of: "default", "none",
     * "single", "double", "singleVdoubleH", "singleHdoubleV", or "round"; or
     * null to use the value from casciian.TWindow.borderStyleModal.
     */
    public void setBorderStyleModal(final String borderStyle) {
        if (borderStyle == null) {
            String style = System.getProperty("casciian.TWindow.borderStyleModal",
                "double");
            borderStyleActiveModal = BorderStyle.getStyle(style);
        } else if (borderStyle.equals("default")) {
            borderStyleActiveModal = BorderStyle.DOUBLE;
        } else {
            borderStyleActiveModal = BorderStyle.getStyle(borderStyle);
        }
    }

    /**
     * Get the border style for the window when it is the modal window.
     *
     * @return the border style
     */
    public BorderStyle getBorderStyleModal() {
        return borderStyleActiveModal;
    }

    /**
     * Set the border style for the window when it is an inactive/background
     * window.
     *
     * @param borderStyle the border style string, one of: "default", "none",
     * "single", "double", "singleVdoubleH", "singleHdoubleV", or "round"; or
     * null to use the value from casciian.TWindow.borderStyleInactive.
     */
    public void setBorderStyleInactive(final String borderStyle) {
        if (borderStyle == null) {
            String style = System.getProperty("casciian.TWindow.borderStyleInactive",
                "single");
            borderStyleInactive = BorderStyle.getStyle(style);
        } else if (borderStyle.equals("default")) {
            borderStyleInactive = BorderStyle.SINGLE;
        } else {
            borderStyleInactive = BorderStyle.getStyle(borderStyle);
        }
    }

    /**
     * Get the border style for the window when it is an inactive/background
     * window.
     *
     * @return the border style
     */
    public BorderStyle getBorderStyleInactive() {
        return borderStyleInactive;
    }

    /**
     * Set the border style for the window when it is being dragged/resize.
     *
     * @param borderStyle the border style string, one of: "default", "none",
     * "single", "double", "singleVdoubleH", "singleHdoubleV", or "round"; or
     * null to use the value from casciian.TWindow.borderStyleMoving.
     */
    public void setBorderStyleMoving(final String borderStyle) {
        if (borderStyle == null) {
            String style = System.getProperty("casciian.TWindow.borderStyleMoving",
                "single");
            borderStyleMoving = BorderStyle.getStyle(style);
        } else if (borderStyle.equals("default")) {
            borderStyleMoving = BorderStyle.SINGLE;
        } else {
            borderStyleMoving = BorderStyle.getStyle(borderStyle);
        }
    }

    /**
     * Get the border style for the window when it is being dragged/resize.
     *
     * @return the border style
     */
    public BorderStyle getBorderStyleMoving() {
        return borderStyleMoving;
    }

    /**
     * Get the custom color theme for this window.
     *
     * @return the color theme, or null if the window does not have a custom
     * color theme
     */
    public final ColorTheme getWindowTheme() {
        return theme;
    }

    /**
     * Set a custom color theme for this window.
     *
     * @param theme the custom theme, or null to use the application-level
     * color theme
     */
    public final void setWindowTheme(final ColorTheme theme) {
        this.theme = theme;
    }

    /**
     * Set the alpha level for this window.
     *
     * @param alpha a value between 0 (fully transparent) and 255 (fully
     * opaque)
     */
    public void setAlpha(final int alpha) {
        assert ((alpha >= 0) && (alpha <= 255));
        this.alpha = alpha;
    }

    /**
     * Get the alpha level for this window.
     *
     * @return a value between 0 (fully transparent) and 255 (fully opaque)
     */
    public int getAlpha() {
        return alpha;
    }

    /**
     * If true, disable any window opening effect.  This is used by the
     * window closing effects themselves so that they are not themselves
     * opened.
     *
     * @return true if the window open effect should be disabled
     */
    public boolean disableOpenEffect() {
        return false;
    }

    /**
     * If true, disable any window closing effect.  This is used by the
     * window closing effects themselves so that they can be closed when
     * finished.
     *
     * @return true if the window close effect should be disabled
     */
    public boolean disableCloseEffect() {
        return false;
    }

    /**
     * Set a cell transform to be applied after the window background is drawn,
     * but before widgets are drawn.
     *
     * @param transform the cell transform to be applied, or null to remove
     * the transform effect
     */
    public void setDrawPreTransform(final CellTransform transform) {
        setDrawPreTransform(transform, false);
    }

    /**
     * Set a cell transform to be applied after the window background is drawn,
     * but before widgets are drawn.
     *
     * @param transform the cell transform to be applied, or null to remove
     * the transform effect
     * @param absolute if true, apply based on screen coordinates
     */
    public void setDrawPreTransform(final CellTransform transform,
        final boolean absolute) {

        drawPreTransform = transform;
        if (absolute) {
            drawPreTransformWidget = null;
        } else {
            drawPreTransformWidget = this;
        }
    }

    /**
     * Set a cell transform to be applied after the widgets are drawn.
     *
     * @param transform the cell transform to be applied, or null to remove
     * the transform effect
     */
    public void setDrawPostTransform(final CellTransform transform) {
        setDrawPostTransform(transform, false);
    }

    /**
     * Set a cell transform to be applied after the widgets are drawn.
     *
     * @param transform the cell transform to be applied, or null to remove
     * the transform effect
     * @param absolute if true, apply based on screen coordinates
     */
    public void setDrawPostTransform(final CellTransform transform,
        final boolean absolute) {

        drawPostTransform = transform;
        if (absolute) {
            drawPostTransformWidget = null;
        } else {
            drawPostTransformWidget = this;
        }
    }

}
