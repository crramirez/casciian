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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import casciian.backend.Backend;
import casciian.backend.ECMA48Backend;
import casciian.backend.MultiBackend;
import casciian.backend.Screen;
import casciian.backend.TWindowBackend;
import casciian.bits.Cell;
import casciian.bits.CellAttributes;
import casciian.bits.Clipboard;
import casciian.bits.ColorTheme;
import casciian.bits.StringUtils;
import casciian.effect.Effect;
import casciian.effect.TextCursorGlintEffect;
import casciian.effect.WindowBurnInEffect;
import casciian.effect.WindowBurnOutEffect;
import casciian.effect.WindowFadeInEffect;
import casciian.effect.WindowFadeOutEffect;
import casciian.effect.WindowWipeInEffect;
import casciian.effect.WindowWipeOutEffect;
import casciian.event.TCommandEvent;
import casciian.event.TInputEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TMenuEvent;
import casciian.event.TMouseEvent;
import casciian.event.TResizeEvent;
import casciian.help.HelpFile;
import casciian.help.Topic;
import casciian.menu.TMenu;
import casciian.menu.TMenuItem;
import casciian.menu.TSubMenu;
import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * TApplication is the main driver class for a full Text User Interface
 * application.  It manages windows, provides a menu bar and status bar, and
 * processes events received from the user.
 */
public class TApplication implements Runnable {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * If true, emit thread stuff to System.err.
     */
    private static final boolean debugThreads = false;

    /**
     * If true, emit events being processed to System.err.
     */
    private static final boolean debugEvents = false;

    /**
     * If true, do "smart placement" on new windows that are not specified to
     * be centered.
     */
    protected boolean smartWindowPlacement = true;

    /**
     * Two backend types are available.
     */
    public static enum BackendType {
        /**
         * An ECMA48 / ANSI X3.64 / XTERM style terminal.
         */
        ECMA48,

        /**
         * Synonym for ECMA48.
         */
        XTERM
    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = ResourceBundle.getBundle(TApplication.class.getName());

    /**
     * The primary event handler thread.
     */
    private volatile WidgetEventHandler primaryEventHandler;

    /**
     * The secondary event handler thread.
     */
    private volatile WidgetEventHandler secondaryEventHandler;

    /**
     * The screen handler thread.
     */
    private volatile ScreenHandler screenHandler;

    /**
     * The widget receiving events from the secondary event handler thread.
     */
    private volatile TWidget secondaryEventReceiver;

    /**
     * Access to the physical screen, keyboard, and mouse.
     */
    private Backend backend;

    /**
     * The Locale used for producing user-facing strings.
     */
    private Locale locale = Locale.getDefault();

    /**
     * The clipboard for copy and paste.
     */
    private Clipboard clipboard = new Clipboard();

    /**
     * Actual mouse coordinate X.
     */
    private int mouseX;

    /**
     * Actual mouse coordinate Y.
     */
    private int mouseY;

    /**
     * Old drawn version of mouse coordinate X.
     */
    private int oldDrawnMouseX;

    /**
     * Old drawn version mouse coordinate Y.
     */
    private int oldDrawnMouseY;

    /**
     * Old drawn version mouse cell.
     */
    private Cell oldDrawnMouseCell = new Cell();

    /**
     * The last mouse up click time, used to determine if this is a mouse
     * double-click.
     */
    private long lastMouseUpTime;

    /**
     * The amount of millis between mouse up events to assume a double-click.
     */
    private long doubleClickTime = 250;

    /**
     * Event queue that is filled by run().
     */
    private List<TInputEvent> fillEventQueue;

    /**
     * Event queue that will be drained by either primary or secondary
     * Thread.
     */
    private List<TInputEvent> drainEventQueue;

    /**
     * Top-level menus in this application.
     */
    private List<TMenu> menus;

    /**
     * Stack of activated sub-menus in this application.
     */
    private List<TMenu> subMenus;

    /**
     * The currently active menu.
     */
    private TMenu activeMenu = null;

    /**
     * Active keyboard accelerators.
     */
    private Map<TKeypress, TMenuItem> accelerators;

    /**
     * All menu items.
     */
    private List<TMenuItem> menuItems;

    /**
     * Windows and widgets pull colors from this ColorTheme.
     */
    private ColorTheme theme;

    /**
     * The top-level windows (but not menus).
     */
    private List<TWindow> windows;

    /**
     * Timers that are being ticked.
     */
    private List<TTimer> timers;

    /**
     * Temporary timer for effects, when animations are disabled.
     */
    private TTimer tempEffectTimer = null;

    /**
     * When true, the application has been started.
     */
    private volatile boolean started = false;

    /**
     * When true, exit the application.
     */
    private volatile boolean quit = false;

    /**
     * When true, repaint the entire screen.
     */
    private volatile boolean repaint = true;

    /**
     * Y coordinate of the top edge of the desktop.  For now this is a
     * constant.  Someday it would be nice to have a multi-line menu or
     * toolbars.
     */
    private int desktopTop = 1;

    /**
     * Y coordinate of the bottom edge of the desktop.
     */
    private int desktopBottom;

    /**
     * An optional TDesktop background window that is drawn underneath
     * everything else.
     */
    private TDesktop desktop;

    /**
     * If true, focus follows mouse: windows automatically raised if the
     * mouse passes over them.
     */
    private boolean focusFollowsMouse = false;

    /**
     * If true, display a text-based mouse cursor.
     */
    protected boolean textMouse = true;

    /**
     * If true, hide the mouse after typing a keystroke.
     */
    private boolean hideMouseWhenTyping = false;

    /**
     * If true, the mouse should not be displayed because a keystroke was
     * typed.
     */
    private boolean typingHidMouse = false;

    /**
     * If true, hide the status bar.
     */
    private boolean hideStatusBar = false;

    /**
     * If true, hide the menu bar.
     */
    private boolean hideMenuBar = false;

    /**
     * If true, the desktop can have the cursor blinking/visible even if the
     * top-most window does not have the cursor active.  This can lead to a
     * visible artifact where the desktop's cursor is "showing through" a
     * window over it.
     */
    protected boolean desktopCanHaveCursor = false;

    /**
     * Optional text to display at the top right of the menu.
     */
    protected String menuTrayText = "";

    /**
     * The list of commands to run before the next I/O check.
     */
    private List<Runnable> invokeLaters = new LinkedList<Runnable>();

    /**
     * The last time the screen was resized.
     */
    private long screenResizeTime = 0;

    /**
     * If true, screen selection is a rectangle.
     */
    private boolean screenSelectionRectangle = false;

    /**
     * If true, the mouse is dragging a screen selection.
     */
    private boolean inScreenSelection = false;

    /**
     * Screen selection starting X.
     */
    private int screenSelectionX0;

    /**
     * Screen selection starting Y.
     */
    private int screenSelectionY0;

    /**
     * Screen selection ending X.
     */
    private int screenSelectionX1;

    /**
     * Screen selection ending Y.
     */
    private int screenSelectionY1;

    /**
     * The help file data.
     */
    protected HelpFile helpFile;

    /**
     * The stack of help topics.
     */
    protected ArrayList<Topic> helpTopics = new ArrayList<Topic>();

    /**
     * The last time user input (mouse or keyboard) was received.
     */
    protected long lastUserInputTime = System.currentTimeMillis();

    /**
     * If true. enable translucency.
     */
    protected boolean translucence = true;

    /**
     * If true, enable animations.
     */
    protected boolean animationsEnabled = true;

    /**
     * The list of window effects to run.
     */
    private List<Effect> windowEffects = new LinkedList<Effect>();

    /**
     * If true, a new window has been added but runWindowEffects() has not
     * yet been called.
     */
    private boolean needToRunWindowEffects = false;

    /**
     * The list of desktop/screen effects to run.
     */
    private List<Effect> screenEffects = new LinkedList<Effect>();

    /**
     * If true, a new desktop/screen effect has been added but
     * runScreenEffects() has not yet been called.
     */
    private boolean needToRunScreenEffects = false;

    /**
     * WidgetEventHandler is the main event consumer loop.  There are at most
     * two such threads in existence: the primary for normal case and a
     * secondary that is used for TMessageBox, TInputBox, and similar.
     */
    private class WidgetEventHandler implements Runnable {
        /**
         * The main application.
         */
        private TApplication application;

        /**
         * Whether or not this WidgetEventHandler is the primary or secondary
         * thread.
         */
        private boolean primary = true;

        /**
         * Public constructor.
         *
         * @param application the main application
         * @param primary if true, this is the primary event handler thread
         */
        public WidgetEventHandler(final TApplication application,
            final boolean primary) {

            this.application = application;
            this.primary = primary;
        }

        /**
         * The consumer loop.
         */
        public void run() {
            // Wrap everything in a try, so that if we go belly up we can let
            // the user have their terminal back.
            try {
                runImpl();
            } catch (Throwable t) {
                this.application.restoreConsole();
                t.printStackTrace();
                this.application.exit();
            }
        }

        /**
         * The consumer loop.
         */
        private void runImpl() {
            boolean first = true;

            // Loop forever
            while (!application.quit) {

                // Wait until application notifies me
                while (!application.quit) {
                    try {
                        synchronized (application.drainEventQueue) {
                            if (application.drainEventQueue.size() > 0) {
                                break;
                            }
                        }

                        long timeout = 0;
                        if (first) {
                            first = false;
                        } else {
                            timeout = application.getSleepTime(1000);
                        }

                        if (timeout == 0) {
                            // A timer needs to fire, break out.
                            break;
                        }

                        if (debugThreads) {
                            System.err.printf("%d %s %s %s sleep %d millis\n",
                                System.currentTimeMillis(), this,
                                primary ? "primary" : "secondary",
                                Thread.currentThread(), timeout);
                        }

                        synchronized (this) {
                            this.wait(timeout);
                        }

                        if (debugThreads) {
                            System.err.printf("%d %s %s %s AWAKE\n",
                                System.currentTimeMillis(), this,
                                primary ? "primary" : "secondary",
                                Thread.currentThread());
                        }

                        if ((!primary)
                            && (application.secondaryEventReceiver == null)
                        ) {
                            // Secondary thread, emergency exit.  If we got
                            // here then something went wrong with the
                            // handoff between yield() and closeWindow().
                            synchronized (application.primaryEventHandler) {
                                application.primaryEventHandler.notify();
                            }
                            application.secondaryEventHandler = null;
                            throw new RuntimeException("secondary exited " +
                                "at wrong time");
                        }
                        break;
                    } catch (InterruptedException e) {
                        // SQUASH
                    }
                } // while (!application.quit)

                // Pull all events off the queue
                for (;;) {
                    TInputEvent event = null;
                    synchronized (application.drainEventQueue) {
                        if (application.drainEventQueue.size() == 0) {
                            break;
                        }
                        event = application.drainEventQueue.remove(0);
                    }

                    // We will have an event to process, so repaint the
                    // screen at the end.
                    application.repaint = true;

                    if ((event instanceof TMouseEvent)
                        || (event instanceof TKeypressEvent)
                    ) {
                        lastUserInputTime = event.getTime().getTime();
                    }

                    if (primary) {
                        primaryHandleEvent(event);
                    } else {
                        secondaryHandleEvent(event);
                    }
                    if ((!primary)
                        && (application.secondaryEventReceiver == null)
                    ) {
                        // Secondary thread, time to exit.

                        // Eliminate my reference so that wakeEventHandler()
                        // resumes working on the primary.
                        application.secondaryEventHandler = null;

                        // We are ready to exit, wake up the primary thread.
                        // Remember that it is currently sleeping inside its
                        // primaryHandleEvent().
                        synchronized (application.primaryEventHandler) {
                            application.primaryEventHandler.notify();
                        }

                        // All done!
                        return;
                    }

                } // for (;;)

                // Fire timers, update screen.
                if (!quit) {
                    application.finishEventProcessing();
                }

            } // while (true) (main runnable loop)
        }
    }

    /**
     * ScreenHandler pushes screen updates to the physical device.
     */
    private class ScreenHandler implements Runnable {
        /**
         * The main application.
         */
        private TApplication application;

        /**
         * The dirty flag.
         */
        private ArrayList<String> dirtyQueue = new ArrayList<String>();

        /**
         * The number of updates pushed out in this second.
         */
        private int framesPerSecond = 0;

        /**
         * The last time a frame was rendered.
         */
        private long lastFlushTime = 0;

        /**
         * How long it took to render the last time in millis.
         */
        private long lastFrameTime = 0;

        /**
         * Public constructor.
         *
         * @param application the main application
         */
        public ScreenHandler(final TApplication application) {
            this.application = application;
        }

        /**
         * The screen update loop.
         */
        public void run() {
            // Wrap everything in a try, so that if we go belly up we can let
            // the user have their terminal back.
            try {
                runImpl();
            } catch (Throwable t) {
                this.application.restoreConsole();
                t.printStackTrace();
                this.application.exit();
            }
        }

        /**
         * The update loop.
         */
        private void runImpl() {
            int frameCount = 0;

            // Loop forever
            while (!application.quit) {

                // Wait until application notifies me
                while (!application.quit) {
                    synchronized (dirtyQueue) {
                        if (dirtyQueue.size() > 0) {
                            // Collapse all the dirty requests into one
                            // refresh.
                            while (dirtyQueue.size() > 0) {
                                dirtyQueue.remove(dirtyQueue.size() - 1);
                            }
                            break;
                        }
                    }
                    try {
                        synchronized (this) {
                            // Always check once a second, but this is
                            // redundant.
                            this.wait(1000);
                        }
                    } catch (InterruptedException e) {
                        // SQUASH
                    }
                } // while (!application.quit)

                // Flush the screen contents
                if (debugThreads) {
                    System.err.printf("%d %s backend.flushScreen()\n",
                        System.currentTimeMillis(), Thread.currentThread());
                }
                synchronized (getScreen()) {
                    long before = System.currentTimeMillis();
                    backend.flushScreen();
                    long now = System.currentTimeMillis();
                    lastFrameTime = now - before;
                    if ((int) (now / 1000) == (int) (lastFlushTime / 1000)) {
                        frameCount++;
                    } else {
                        framesPerSecond = frameCount;
                        frameCount = 0;
                    }
                    lastFlushTime = now;
                }
            } // while (true) (main runnable loop)

            // Shutdown the user I/O thread(s)
            backend.shutdown();
        }

        /**
         * Set the dirty flag.
         */
        public void setDirty() {
            synchronized (dirtyQueue) {
                if (dirtyQueue.size() == 0) {
                    dirtyQueue.add("dirty");
                    synchronized (this) {
                        notify();
                    }
                }
            }
        }

    }

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param backendType BackendType.XTERM or BackendType.ECMA48
     * @param windowWidth the number of text columns to start with
     * @param windowHeight the number of text rows to start with
     * @param fontSize the size in points
     * @throws UnsupportedEncodingException if an exception is thrown when
     * creating the InputStreamReader
     */
    @SuppressWarnings("this-escape")
    public TApplication(final BackendType backendType, final int windowWidth,
        final int windowHeight, final int fontSize)
        throws UnsupportedEncodingException {

        switch (backendType) {
        case XTERM:
            // Fall through...
        case ECMA48:
            backend = new ECMA48Backend(this, null, null, windowWidth,
                windowHeight, fontSize);
            break;
        default:
            throw new IllegalArgumentException("Invalid backend type: "
                + backendType);
        }
        TApplicationImpl();
    }

    /**
     * Public constructor.
     *
     * @param backendType BackendType.XTERM or BackendType.ECMA48
     * @throws UnsupportedEncodingException if an exception is thrown when
     * creating the InputStreamReader
     */
    @SuppressWarnings("this-escape")
    public TApplication(final BackendType backendType)
        throws UnsupportedEncodingException {

        switch (backendType) {
        case XTERM:
            // Fall through...
        case ECMA48:
            backend = new ECMA48Backend(this, null, null);
            break;
        default:
            throw new IllegalArgumentException("Invalid backend type: "
                + backendType);
        }
        TApplicationImpl();
    }

    /**
     * Public constructor.  The backend type will be BackendType.ECMA48.
     *
     * @param input an InputStream connected to the remote user, or null for
     * System.in.  If System.in is used, then on non-Windows systems it will
     * be put in raw mode; shutdown() will (blindly!) put System.in in cooked
     * mode.  input is always converted to a Reader with UTF-8 encoding.
     * @param output an OutputStream connected to the remote user, or null
     * for System.out.  output is always converted to a Writer with UTF-8
     * encoding.
     * @throws UnsupportedEncodingException if an exception is thrown when
     * creating the InputStreamReader
     */
    @SuppressWarnings("this-escape")
    public TApplication(final InputStream input,
        final OutputStream output) throws UnsupportedEncodingException {

        backend = new ECMA48Backend(this, input, output);
        TApplicationImpl();
    }

    /**
     * Public constructor.  The backend type will be BackendType.ECMA48.
     *
     * @param input the InputStream underlying 'reader'.  Its available()
     * method is used to determine if reader.read() will block or not.
     * @param reader a Reader connected to the remote user.
     * @param writer a PrintWriter connected to the remote user.
     * @param setRawMode if true, set System.in into raw mode with stty.
     * This should in general not be used.  It is here solely for Demo3,
     * which uses System.in.
     * @throws IllegalArgumentException if input, reader, or writer are null.
     */
    @SuppressWarnings("this-escape")
    public TApplication(final InputStream input, final Reader reader,
        final PrintWriter writer, final boolean setRawMode) {

        backend = new ECMA48Backend(this, input, reader, writer, setRawMode);
        TApplicationImpl();
    }

    /**
     * Public constructor.  The backend type will be BackendType.ECMA48.
     *
     * @param input the InputStream underlying 'reader'.  Its available()
     * method is used to determine if reader.read() will block or not.
     * @param reader a Reader connected to the remote user.
     * @param writer a PrintWriter connected to the remote user.
     * @throws IllegalArgumentException if input, reader, or writer are null.
     */
    public TApplication(final InputStream input, final Reader reader,
        final PrintWriter writer) {

        this(input, reader, writer, false);
    }

    /**
     * Public constructor.  This hook enables use with new non-Casciian
     * backends.
     *
     * @param backend a Backend that is already ready to go.
     */
    @SuppressWarnings("this-escape")
    public TApplication(final Backend backend) {
        this.backend = backend;
        backend.setListener(this);
        TApplicationImpl();
    }

    /**
     * Finish construction once the backend is set.
     */
    private void TApplicationImpl() {
        // Text block mouse option
        if (System.getProperty("casciian.textMouse", "true").equals("false")) {
            textMouse = false;
        }

        // Hide mouse when typing option
        if (System.getProperty("casciian.hideMouseWhenTyping",
                "false").equals("true")) {

            hideMouseWhenTyping = true;
        }

        // Hide status bar option
        if (System.getProperty("casciian.hideStatusBar",
                "false").equals("true")) {
            hideStatusBar = true;
        }

        // Hide menu bar option
        if (System.getProperty("casciian.hideMenuBar", "false").equals("true")) {
            hideMenuBar = true;
        }

        // Translucent windows (!) option
        if (System.getProperty("casciian.translucence", "true").equals("false")) {
            translucence = false;
        }

        // Overall animations
        if (System.getProperty("casciian.animations", "true").equals("false")) {
            animationsEnabled = false;
        }

        theme           = new ColorTheme();
        desktopTop      = (hideMenuBar ? 0 : 1);
        desktopBottom   = getScreen().getHeight() - 1 + (hideStatusBar ? 1 : 0);
        fillEventQueue  = new LinkedList<TInputEvent>();
        drainEventQueue = new LinkedList<TInputEvent>();
        windows         = new LinkedList<TWindow>();
        menus           = new ArrayList<TMenu>();
        subMenus        = new ArrayList<TMenu>();
        timers          = new LinkedList<TTimer>();
        accelerators    = new HashMap<TKeypress, TMenuItem>();
        menuItems       = new LinkedList<TMenuItem>();
        desktop         = new TDesktop(this);

        if (animationsEnabled) {
            // Animations always check every 1/32 of a second.
            final int ANIMATION_FPS = 32;
            TTimer animationTimer = addTimer(1000 / ANIMATION_FPS, true,
                new TAction() {
                    public void DO() {
                        doRepaint();
                    }
                }
            );

        } else {
            // If animations are disabled, we need additional timers.

            // Default to 500 millis for blinking.
            long millis = 500;

            try {
                String milliStr = System.getProperty("casciian.blinkMillis",
                    "500");
                millis = Integer.parseInt(milliStr);
            } catch (NumberFormatException e) {
                // SQUASH
            }

            millis = Math.max(0, millis);
            millis = Math.min(millis, 500);

            // All the backends need to have a timer to drive blink state,
            // but MultiBackend specifically also needs to do idle checks.
            if (backend instanceof MultiBackend) {
                addTimer(millis, true,
                    new TAction() {
                        public void DO() {
                            // Update idle checks.
                            getBackend().hasEvents();
                            doRepaint();
                        }
                    }
                );
            } else {
                addTimer(millis, true,
                    new TAction() {
                        public void DO() {
                            doRepaint();
                        }
                    }
                );
            }
        }

        // Load the help system
        invokeLater(new Runnable() {
            /*
             * This isn't the best solution.  But basically if a TApplication
             * subclass constructor throws and needs to use TExceptionDialog,
             * it may end up at the bottom of the window stack with a bunch
             * of modal windows on top of it if said constructors spawn their
             * windows also via invokeLater().  But if they don't do that,
             * and instead just conventionally construct their windows, then
             * this exception dialog will end up on top where it should be.
             */
            public void run() {
                try {
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    helpFile = new HelpFile();
                    helpFile.load(loader.getResourceAsStream("help.xml"));
                } catch (Exception e) {
                    new TExceptionDialog(TApplication.this, e);
                }
            }
        });
    }

    // ------------------------------------------------------------------------
    // Runnable ---------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Run this application until it exits.
     */
    public void run() {
        // System.err.println("*** TApplication.run() begins ***");

        // Start the screen updater thread
        screenHandler = new ScreenHandler(this);
        (new Thread(screenHandler)).start();

        // Start the main consumer thread
        primaryEventHandler = new WidgetEventHandler(this, true);
        (new Thread(primaryEventHandler)).start();

        started = true;

        while (!quit) {
            synchronized (this) {
                boolean doWait = false;

                if (!backend.hasEvents()) {
                    synchronized (fillEventQueue) {
                        if (fillEventQueue.size() == 0) {
                            doWait = true;
                        }
                    }
                }

                if (doWait) {
                    // No I/O to dispatch, so wait until the backend
                    // provides new I/O.
                    try {
                        if (debugThreads) {
                            System.err.println(System.currentTimeMillis() +
                                " " + Thread.currentThread() + " MAIN sleep");
                        }

                        this.wait();

                        if (debugThreads) {
                            System.err.println(System.currentTimeMillis() +
                                " " + Thread.currentThread() + " MAIN AWAKE");
                        }
                    } catch (InterruptedException e) {
                        // I'm awake and don't care why, let's see what's
                        // going on out there.
                    }
                }

            } // synchronized (this)

            synchronized (fillEventQueue) {
                // Pull any pending I/O events
                backend.getEvents(fillEventQueue);
            }

            // Dispatch each event to the appropriate handler, one at a time.
            for (;;) {
                TInputEvent event = null;
                synchronized (fillEventQueue) {
                    if (fillEventQueue.size() == 0) {
                        break;
                    }
                    event = fillEventQueue.remove(0);
                }
                metaHandleEvent(event);
            }

            // Wake a consumer thread if we have any pending events.
            if (drainEventQueue.size() > 0) {
                wakeEventHandler();
            }

        } // while (!quit)

        // Shutdown the event consumer threads
        if (secondaryEventHandler != null) {
            synchronized (secondaryEventHandler) {
                secondaryEventHandler.notify();
            }
        }
        if (primaryEventHandler != null) {
            synchronized (primaryEventHandler) {
                primaryEventHandler.notify();
            }
        }

        // Close all the windows.  This gives them an opportunity to release
        // resources.
        closeAllWindows();

        // Close the desktop.
        if (desktop != null) {
            setDesktop(null);
        }

        // Give the overarching application an opportunity to release
        // resources.
        onExit();

        // System.err.println("*** TApplication.run() exits ***");
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Method that TApplication subclasses can override to handle menu or
     * posted command events.
     *
     * @param command command event
     * @return if true, this event was consumed
     */
    protected boolean onCommand(final TCommandEvent command) {
        // Default: handle cmExit
        if (command.equals(cmExit)) {
            if (messageBox(i18n.getString("exitDialogTitle"),
                    i18n.getString("exitDialogText"),
                    TMessageBox.Type.YESNO).isYes()) {

                exit();
            }
            return true;
        }

        if (command.equals(cmHelp)) {
            if (getActiveWindow() != null) {
                new THelpWindow(this, getActiveWindow().getHelpTopic());
            } else {
                new THelpWindow(this);
            }
            return true;
        }

        if (command.equals(cmShell)) {
            openTerminal(0, 0, TWindow.RESIZABLE);
            return true;
        }

        if (command.equals(cmTile)) {
            tileWindows();
            return true;
        }
        if (command.equals(cmCascade)) {
            cascadeWindows();
            return true;
        }
        if (command.equals(cmCloseAll)) {
            closeAllWindows();
            return true;
        }

        if (command.equals(cmMenu) && (hideMenuBar == false)) {
            if (!modalWindowActive() && (activeMenu == null)) {
                if (menus.size() > 0) {
                    menus.get(0).setActive(true);
                    activeMenu = menus.get(0);
                    return true;
                }
            }
        }

        if (command.equals(cmTextCursorGlint)) {
            synchronized (screenEffects) {
                screenEffects.add(new TextCursorGlintEffect(getScreen()));
                needToRunScreenEffects = true;
            }
            if (!animationsEnabled) {
                // Put a timer on at 32 fps until the effect is finished.
                if (tempEffectTimer != null) {
                    tempEffectTimer.setRecurring(false);
                }
                tempEffectTimer = addTimer(1000/32, true,
                    new TAction() {
                        private int timerI = 0;
                        public void DO() {
                            if (timerI < 64) {
                                timerI++;
                            } else {
                                tempEffectTimer.setRecurring(false);
                            }
                        }
                    });
            }
            return true;
        }

        return false;
    }

    /**
     * Method that TApplication subclasses can override to handle menu
     * events.
     *
     * @param menu menu event
     * @return if true, this event was consumed
     */
    protected boolean onMenu(final TMenuEvent menu) {

        // Default: handle MID_EXIT
        if (menu.getId() == TMenu.MID_EXIT) {
            if (messageBox(i18n.getString("exitDialogTitle"),
                    i18n.getString("exitDialogText"),
                    TMessageBox.Type.YESNO).isYes()) {

                exit();
            }
            return true;
        }

        if (menu.getId() == TMenu.MID_HELP_HELP) {
            new THelpWindow(this, THelpWindow.HELP_HELP);
            return true;
        }

        if (menu.getId() == TMenu.MID_HELP_CONTENTS) {
            new THelpWindow(this, helpFile.getTableOfContents());
            return true;
        }

        if (menu.getId() == TMenu.MID_HELP_INDEX) {
            new THelpWindow(this, helpFile.getIndex());
            return true;
        }

        if (menu.getId() == TMenu.MID_HELP_SEARCH) {
            TInputBox inputBox = inputBox(i18n.
                getString("searchHelpInputBoxTitle"),
                i18n.getString("searchHelpInputBoxCaption"), "",
                TInputBox.Type.OKCANCEL);
            if (inputBox.isOk()) {
                new THelpWindow(this,
                    helpFile.getSearchResults(inputBox.getText()));
            }
            return true;
        }

        if (menu.getId() == TMenu.MID_HELP_PREVIOUS) {
            if (helpTopics.size() > 1) {
                Topic previous = helpTopics.remove(helpTopics.size() - 2);
                helpTopics.remove(helpTopics.size() - 1);
                new THelpWindow(this, previous);
            } else {
                new THelpWindow(this, helpFile.getTableOfContents());
            }
            return true;
        }

        if (menu.getId() == TMenu.MID_HELP_ACTIVE_FILE) {
            try {
                List<String> filters = new ArrayList<String>();
                filters.add("^.*\\.[Xx][Mm][Ll]$");
                String filename = fileOpenBox(".", TFileOpenBox.Type.OPEN,
                    filters);
                if (filename != null) {
                    helpTopics = new ArrayList<Topic>();
                    helpFile = new HelpFile();
                    helpFile.load(new FileInputStream(filename));
                }
            } catch (Exception e) {
                // Show this exception to the user.
                new TExceptionDialog(this, e);
            }
            return true;
        }

        if (menu.getId() == TMenu.MID_SHELL) {
            openTerminal(0, 0, TWindow.RESIZABLE);
            return true;
        }

        if (menu.getId() == TMenu.MID_TILE) {
            tileWindows();
            return true;
        }
        if (menu.getId() == TMenu.MID_CASCADE) {
            cascadeWindows();
            return true;
        }
        if (menu.getId() == TMenu.MID_CLOSE_ALL) {
            closeAllWindows();
            return true;
        }
        if (menu.getId() == TMenu.MID_TERMINAL_INFORMATION) {
            new TTerminalInformationWindow(this);
            return true;
        }
        if (menu.getId() == TMenu.MID_ABOUT) {
            showAboutDialog();
            return true;
        }
        if (menu.getId() == TMenu.MID_REPAINT) {
            getScreen().clearPhysical();
            doRepaint();
            return true;
        }
        if (menu.getId() == TMenu.MID_VIEW_ANSI) {
            openAnsiFile();
            return true;
        }

        if (menu.getId() ==TMenu.MID_TEXT_CURSOR_GLINT) {
            synchronized (screenEffects) {
                screenEffects.add(new TextCursorGlintEffect(getScreen()));
                needToRunScreenEffects = true;
            }
            if (!animationsEnabled) {
                // Put a timer on at 32 fps until the effect is finished.
                if (tempEffectTimer != null) {
                    tempEffectTimer.setRecurring(false);
                }
                tempEffectTimer = addTimer(1000/32, true,
                    new TAction() {
                        private int timerI = 0;
                        public void DO() {
                            if (timerI < 64) {
                                timerI++;
                            } else {
                                tempEffectTimer.setRecurring(false);
                            }
                        }
                    });
            }
            return true;
        }

        if (menu.getId() == TMenu.MID_CUT) {
            postMenuEvent(new TCommandEvent(menu.getBackend(), cmCut));
            return true;
        }
        if (menu.getId() == TMenu.MID_COPY) {
            postMenuEvent(new TCommandEvent(menu.getBackend(), cmCopy));
            return true;
        }
        if (menu.getId() == TMenu.MID_PASTE) {
            postMenuEvent(new TCommandEvent(menu.getBackend(), cmPaste));
            return true;
        }
        if (menu.getId() == TMenu.MID_CLEAR) {
            postMenuEvent(new TCommandEvent(menu.getBackend(), cmClear));
            return true;
        }

        return false;
    }

    /**
     * Method that TApplication subclasses can override to handle keystrokes.
     *
     * @param keypress keystroke event
     * @return if true, this event was consumed
     */
    protected boolean onKeypress(final TKeypressEvent keypress) {
        // Default: only menu shortcuts

        // Process Alt-F, Alt-E, etc. menu shortcut keys
        if (!keypress.getKey().isFnKey()
            && keypress.getKey().isAlt()
            && !keypress.getKey().isCtrl()
            && (activeMenu == null)
            && !modalWindowActive()
            && (hideMenuBar == false)
        ) {

            assert (subMenus.size() == 0);

            for (TMenu menu: menus) {
                if (Character.toLowerCase(menu.getMnemonic().getShortcut())
                    == Character.toLowerCase(keypress.getKey().getChar())
                ) {
                    activeMenu = menu;
                    menu.setActive(true);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Process background events, and update the screen.
     */
    private void finishEventProcessing() {
        if (debugThreads) {
            System.err.printf(System.currentTimeMillis() + " " +
                Thread.currentThread() + " finishEventProcessing()\n");
        }

        // See if we need to enable/disable the edit menu.
        EditMenuUser widget = null;
        if (activeMenu == null) {
            TWindow activeWindow = getActiveWindow();
            if (activeWindow != null) {
                if (activeWindow.getActiveChild() instanceof EditMenuUser) {
                    widget = (EditMenuUser) activeWindow.getActiveChild();
                }
            } else if (desktop != null) {
                if (desktop.getActiveChild() instanceof EditMenuUser) {
                    widget = (EditMenuUser) desktop.getActiveChild();
                }
            }
            if (widget == null) {
                disableMenuItem(TMenu.MID_CUT);
                disableMenuItem(TMenu.MID_COPY);
                disableMenuItem(TMenu.MID_PASTE);
                disableMenuItem(TMenu.MID_CLEAR);
            } else {
                if (widget.isEditMenuCut()) {
                    enableMenuItem(TMenu.MID_CUT);
                } else {
                    disableMenuItem(TMenu.MID_CUT);
                }
                if (widget.isEditMenuCopy()) {
                    enableMenuItem(TMenu.MID_COPY);
                } else {
                    disableMenuItem(TMenu.MID_COPY);
                }
                if (widget.isEditMenuPaste()) {
                    enableMenuItem(TMenu.MID_PASTE);
                } else {
                    disableMenuItem(TMenu.MID_PASTE);
                }
                if (widget.isEditMenuClear()) {
                    enableMenuItem(TMenu.MID_CLEAR);
                } else {
                    disableMenuItem(TMenu.MID_CLEAR);
                }
            }
        }

        // Process timers and call doIdle()'s
        doIdle();

        // Give subclass TApplications a chance to update something before
        // the screen is drawn.
        onPreDraw();

        // Allow window effects to run before the screen is updated.
        boolean doRunWindowEffects = false;
        synchronized (windowEffects) {
            doRunWindowEffects = needToRunWindowEffects;
        }
        if (doRunWindowEffects) {
            runWindowEffects();
        }

        // Update the screen
        synchronized (getScreen()) {
            drawAll();

            // Run screen effects after everything is drawn, but before it is
            // posted to the physical buffer(s).
            boolean doRunScreenEffects = false;
            synchronized (screenEffects) {
                doRunScreenEffects = needToRunScreenEffects;
            }
            if (doRunScreenEffects) {
                runScreenEffects();
            }
        }

        // Wake up the screen repainter
        screenHandler.setDirty();
        wakeScreenHandler();

        if (debugThreads) {
            System.err.printf(System.currentTimeMillis() + " " +
                Thread.currentThread() + " finishEventProcessing() END\n");
        }
    }

    /**
     * Peek at certain application-level events, add to eventQueue, and wake
     * up the consuming Thread.
     *
     * @param event the input event to consume
     */
    private void metaHandleEvent(final TInputEvent event) {

        if (debugEvents) {
            System.err.printf(String.format("metaHandleEvents event: %s\n",
                    event)); System.err.flush();
        }

        if (quit) {
            // Do no more processing if the application is already trying
            // to exit.
            return;
        }

        // Special application-wide events -------------------------------

        // Abort everything
        if (event instanceof TCommandEvent) {
            TCommandEvent command = (TCommandEvent) event;
            if (command.equals(cmAbort)) {
                exit();
                return;
            }
        }

        // Screen resize
        if (event instanceof TResizeEvent) {
            TResizeEvent resize = (TResizeEvent) event;
            assert (resize.getType() == TResizeEvent.Type.SCREEN);

            synchronized (getScreen()) {
                if ((System.currentTimeMillis() - screenResizeTime >= 15)
                    || (resize.getWidth() < getScreen().getWidth())
                    || (resize.getHeight() < getScreen().getHeight())
                ) {
                    getScreen().setDimensions(resize.getWidth(),
                        resize.getHeight());
                    screenResizeTime = System.currentTimeMillis();
                }
                desktopBottom = getScreen().getHeight() - 1;
                if (hideStatusBar) {
                    desktopBottom++;
                }
                mouseX = 0;
                mouseY = 0;

                if (desktop != null) {
                    desktop.setDimensions(0, desktopTop, resize.getWidth(),
                        (desktopBottom - desktopTop));
                    desktop.onResize(resize);
                }
                List<TWindow> resizeWindows = new ArrayList<TWindow>(windows);
                for (TWindow window: resizeWindows) {
                    window.onResize(resize);
                }

                // Change menu edges if needed.
                recomputeMenuX();
            }

            // We are dirty, redraw the screen.
            doRepaint();

            /*
             System.err.println("New screen: " + resize.getWidth() +
                 " x " + resize.getHeight());
             */
            return;
        }

        synchronized (drainEventQueue) {
            // Put into the main queue
            drainEventQueue.add(event);
        }
    }

    /**
     * Dispatch one event to the appropriate widget or application-level
     * event handler.  This is the primary event handler, it has the normal
     * application-wide event handling.
     *
     * @param event the input event to consume
     * @see #secondaryHandleEvent(TInputEvent event)
     */
    private void primaryHandleEvent(final TInputEvent event) {
        assert (event != null);

        if (debugEvents) {
            System.err.printf("%s primaryHandleEvent: %s\n",
                Thread.currentThread(), event);
        }
        TMouseEvent doubleClick = null;

        // Special application-wide events -----------------------------------

        if (event instanceof TKeypressEvent) {
            if (hideMouseWhenTyping) {
                typingHidMouse = true;
            }
        }

        // Peek at the mouse position
        if (event instanceof TMouseEvent) {
            typingHidMouse = false;

            TMouseEvent mouse = (TMouseEvent) event;

            if (mouse.isMouse1() && (mouse.isShift() || mouse.isCtrl())) {
                // Screen selection.
                if (inScreenSelection) {
                    screenSelectionX1 = mouse.getX();
                    screenSelectionY1 = mouse.getY();
                } else {
                    inScreenSelection = true;
                    screenSelectionX0 = mouse.getX();
                    screenSelectionY0 = mouse.getY();
                    screenSelectionX1 = mouse.getX();
                    screenSelectionY1 = mouse.getY();
                    screenSelectionRectangle = mouse.isCtrl();
                }
            } else {
                if (inScreenSelection) {
                    getScreen().copySelection(clipboard, screenSelectionX0,
                        screenSelectionY0, screenSelectionX1, screenSelectionY1,
                        screenSelectionRectangle);
                }
                inScreenSelection = false;
            }

            if ((mouseX != mouse.getX()) || (mouseY != mouse.getY())) {
                mouseX = mouse.getX();
                mouseY = mouse.getY();
            } else {
                if ((mouse.getType() == TMouseEvent.Type.MOUSE_DOWN)
                    && (!mouse.isMouseWheelUp())
                    && (!mouse.isMouseWheelDown())
                ) {
                    if ((mouse.getTime().getTime() - lastMouseUpTime) <
                        doubleClickTime) {

                        // This is a double-click.
                        doubleClick = new TMouseEvent(mouse.getBackend(),
                            TMouseEvent.Type.MOUSE_DOUBLE_CLICK,
                            mouse.getX(), mouse.getY(),
                            mouse.getAbsoluteX(), mouse.getAbsoluteY(),
                            mouse.getPixelOffsetX(), mouse.getPixelOffsetY(),
                            mouse.isMouse1(), mouse.isMouse2(),
                            mouse.isMouse3(),
                            mouse.isMouseWheelUp(), mouse.isMouseWheelDown(),
                            mouse.isAlt(), mouse.isCtrl(), mouse.isShift());

                    } else {
                        // The first click of a potential double-click.
                        lastMouseUpTime = mouse.getTime().getTime();
                    }
                }
            }

            // See if we need to switch focus to another window or the menu
            checkSwitchFocus((TMouseEvent) event);
        }

        // Handle menu events
        if ((activeMenu != null) && !(event instanceof TCommandEvent)) {
            TMenu menu = activeMenu;

            if (event instanceof TMouseEvent) {
                TMouseEvent mouse = (TMouseEvent) event;

                while (subMenus.size() > 0) {
                    TMenu subMenu = subMenus.get(subMenus.size() - 1);
                    if (subMenu.mouseWouldHit(mouse)) {
                        break;
                    }
                    if ((mouse.getType() == TMouseEvent.Type.MOUSE_MOTION)
                        && (!mouse.isMouse1())
                        && (!mouse.isMouse2())
                        && (!mouse.isMouse3())
                        && (!mouse.isMouseWheelUp())
                        && (!mouse.isMouseWheelDown())
                    ) {
                        break;
                    }
                    // We navigated away from a sub-menu, so close it
                    closeSubMenu();
                }

                // Convert the mouse relative x/y to menu coordinates
                assert (mouse.getX() == mouse.getAbsoluteX());
                assert (mouse.getY() == mouse.getAbsoluteY());
                if (subMenus.size() > 0) {
                    menu = subMenus.get(subMenus.size() - 1);
                }
                mouse.setX(mouse.getX() - menu.getX());
                mouse.setY(mouse.getY() - menu.getY());
            }
            menu.handleEvent(event);
            return;
        }

        if (event instanceof TKeypressEvent) {
            TKeypressEvent keypress = (TKeypressEvent) event;

            // See if this key matches an accelerator, and is not being
            // shortcutted by the active window, and if so dispatch the menu
            // event.
            boolean windowWillShortcut = false;
            TWindow activeWindow = getActiveWindow();
            if (activeWindow != null) {
                assert (activeWindow.isShown());
                if (activeWindow.isShortcutKeypress(keypress.getKey())) {
                    // We do not process this key, it will be passed to the
                    // window instead.
                    windowWillShortcut = true;
                }
            } else if (desktop != null) {
                if (desktop.isShortcutKeypress(keypress.getKey())) {
                    // We do not process this key, it will be passed to the
                    // desktop instead.
                    windowWillShortcut = true;
                }
            }

            if (!windowWillShortcut && !modalWindowActive()) {
                TKeypress keypressLowercase = keypress.getKey().toLowerCase();
                TMenuItem item = null;
                synchronized (accelerators) {
                    item = accelerators.get(keypressLowercase);
                }
                if (item != null) {
                    if (item.isEnabled()) {
                        // Let the menu item dispatch
                        item.dispatch(keypress.getBackend());
                        return;
                    }
                }

                // Handle the keypress
                if (onKeypress(keypress)) {
                    return;
                }
            }
        }

        if (event instanceof TCommandEvent) {
            if (onCommand((TCommandEvent) event)) {
                return;
            }
        }

        if (event instanceof TMenuEvent) {
            if (onMenu((TMenuEvent) event)) {
                return;
            }
        }

        // Dispatch events to the active window -------------------------------
        boolean dispatchToDesktop = true;
        TWindow window = getActiveWindow();
        if (window != null) {
            assert (window.isActive());
            assert (window.isShown());

            TMouseEvent mouse = null;
            if (event instanceof TMouseEvent) {
                mouse = (TMouseEvent) event;
                // Convert the mouse relative x/y to window coordinates
                assert (mouse.getX() == mouse.getAbsoluteX());
                assert (mouse.getY() == mouse.getAbsoluteY());
                mouse.setX(mouse.getX() - window.getX());
                mouse.setY(mouse.getY() - window.getY());

                if (doubleClick != null) {
                    doubleClick.setX(doubleClick.getX() - window.getX());
                    doubleClick.setY(doubleClick.getY() - window.getY());
                }

                if (window.mouseWouldHit(mouse)) {
                    dispatchToDesktop = false;
                }
            } else if (event instanceof TKeypressEvent) {
                dispatchToDesktop = false;
            } else if (event instanceof TMenuEvent) {
                dispatchToDesktop = false;
            }

            if (debugEvents) {
                System.err.printf("TApplication dispatch event: %s\n",
                    event);
                System.err.printf("   Routed to: %s\n", window);
                System.err.flush();
            }
            window.handleEvent(event);
            if (doubleClick != null) {
                if (debugEvents) {
                    System.err.printf("  -- DOUBLE CLICK --\n");
                }
                window.handleEvent(doubleClick);
            }
            if (mouse != null) {
                if (window.mouseWouldHit(mouse)) {
                    // If we are dragging the window, the new coordinates
                    // might fully capture the mouse again.  If so, do not
                    // let the desktop see it.
                    dispatchToDesktop = false;
                }
            }
        }
        if (dispatchToDesktop) {
            // This event is fair game for the desktop to process.
            if (desktop != null) {
                desktop.handleEvent(event);
                if (doubleClick != null) {
                    desktop.handleEvent(doubleClick);
                }
            }
        }
    }

    /**
     * Dispatch one event to the appropriate widget or application-level
     * event handler.  This is the secondary event handler used by certain
     * special dialogs (currently TMessageBox and TFileOpenBox).
     *
     * @param event the input event to consume
     * @see #primaryHandleEvent(TInputEvent event)
     */
    private void secondaryHandleEvent(final TInputEvent event) {
        assert (event != null);

        TMouseEvent doubleClick = null;

        if (debugEvents) {
            System.err.printf("%s secondaryHandleEvent: %s\n",
                Thread.currentThread(), event);
        }

        // Peek at the mouse position
        if (event instanceof TMouseEvent) {
            typingHidMouse = false;

            TMouseEvent mouse = (TMouseEvent) event;

            if ((mouseX != mouse.getX()) || (mouseY != mouse.getY())) {
                mouseX = mouse.getX();
                mouseY = mouse.getY();
            } else {
                if ((mouse.getType() == TMouseEvent.Type.MOUSE_DOWN)
                    && (!mouse.isMouseWheelUp())
                    && (!mouse.isMouseWheelDown())
                ) {
                    if ((mouse.getTime().getTime() - lastMouseUpTime) <
                        doubleClickTime) {

                        // This is a double-click.
                        doubleClick = new TMouseEvent(mouse.getBackend(),
                            TMouseEvent.Type.MOUSE_DOUBLE_CLICK,
                            mouse.getX(), mouse.getY(),
                            mouse.getAbsoluteX(), mouse.getAbsoluteY(),
                            mouse.getPixelOffsetX(), mouse.getPixelOffsetY(),
                            mouse.isMouse1(), mouse.isMouse2(),
                            mouse.isMouse3(),
                            mouse.isMouseWheelUp(), mouse.isMouseWheelDown(),
                            mouse.isAlt(), mouse.isCtrl(), mouse.isShift());

                    } else {
                        // The first click of a potential double-click.
                        lastMouseUpTime = mouse.getTime().getTime();
                    }
                }
            }
        }

        secondaryEventReceiver.handleEvent(event);
        // Note that it is possible for secondaryEventReceiver to be null
        // now, because its handleEvent() might have finished out on the
        // secondary thread.  So put any extra processing inside a null
        // check.
        if (secondaryEventReceiver != null) {
            if (doubleClick != null) {
                secondaryEventReceiver.handleEvent(doubleClick);
            }
        }
    }

    /**
     * Enable a widget to override the primary event thread.
     *
     * @param widget widget that will receive events
     */
    public final void enableSecondaryEventReceiver(final TWidget widget) {
        if (debugThreads) {
            System.err.println(System.currentTimeMillis() +
                " enableSecondaryEventReceiver()");
        }

        assert (secondaryEventReceiver == null);
        assert (secondaryEventHandler == null);
        assert ((widget instanceof TMessageBox)
            || (widget instanceof TFileOpenBox));
        secondaryEventReceiver = widget;
        secondaryEventHandler = new WidgetEventHandler(this, false);

        (new Thread(secondaryEventHandler)).start();
    }

    /**
     * Yield to the secondary thread.
     */
    public final void yield() {
        if (debugThreads) {
            System.err.printf(System.currentTimeMillis() + " " +
                Thread.currentThread() + " yield()\n");
        }

        assert (secondaryEventReceiver != null);

        while (secondaryEventReceiver != null) {
            synchronized (primaryEventHandler) {
                try {
                    primaryEventHandler.wait();
                } catch (InterruptedException e) {
                    // SQUASH
                }
            }
        }
    }

    /**
     * Run the window effects.
     */
    private void runWindowEffects() {
        synchronized (windowEffects) {
            if (windowEffects.size() == 0) {
                return;
            }
        }
        List<Effect> effectsToRun = new ArrayList<Effect>();
        List<Effect> effectsToRemove = new ArrayList<Effect>();
        synchronized (windowEffects) {
            effectsToRun.addAll(windowEffects);
        }

        while (effectsToRun.size() > 0) {
            Effect effect = effectsToRun.remove(0);
            if (effect.isCompleted()) {
                effectsToRemove.add(effect);
            }
            effect.update();
        }
        if (effectsToRemove.size() > 0) {
            synchronized (windowEffects) {
                windowEffects.removeAll(effectsToRemove);
                needToRunWindowEffects = false;
            }
        }
    }

    /**
     * Run the screen effects.
     */
    private void runScreenEffects() {
        synchronized (screenEffects) {
            if (screenEffects.size() == 0) {
                return;
            }
        }
        List<Effect> effectsToRun = new ArrayList<Effect>();
        List<Effect> effectsToRemove = new ArrayList<Effect>();
        synchronized (screenEffects) {
            effectsToRun.addAll(screenEffects);
        }

        while (effectsToRun.size() > 0) {
            Effect effect = effectsToRun.remove(0);
            if (effect.isCompleted()) {
                effectsToRemove.add(effect);
            }
            effect.update();
        }
        if (effectsToRemove.size() > 0) {
            synchronized (screenEffects) {
                screenEffects.removeAll(effectsToRemove);
                needToRunScreenEffects = false;
            }
        }
    }

    /**
     * Do stuff when there is no user input.
     */
    private void doIdle() {
        if (debugThreads) {
            System.err.printf(System.currentTimeMillis() + " " +
                Thread.currentThread() + " doIdle()\n");
        }

        synchronized (timers) {

            if (debugThreads) {
                System.err.printf(System.currentTimeMillis() + " " +
                    Thread.currentThread() + " doIdle() 2\n");
            }

            // Run any timers that have timed out
            Date now = new Date();
            List<TTimer> keepTimers = new LinkedList<TTimer>();
            for (TTimer timer: timers) {
                if (timer.getNextTick().getTime() <= now.getTime()) {
                    // Something might change, so repaint the screen.
                    repaint = true;
                    timer.tick();
                    if (timer.recurring) {
                        keepTimers.add(timer);
                    }
                } else {
                    keepTimers.add(timer);
                }
            }
            timers.clear();
            timers.addAll(keepTimers);
        }

        if (debugThreads) {
            System.err.printf(System.currentTimeMillis() + " " +
                Thread.currentThread() + " doIdle() 3\n");
        }

        // Call onIdle's
        for (TWindow window: windows) {
            window.onIdle();
        }
        if (desktop != null) {
            desktop.onIdle();
        }

        // Run any invokeLaters.  We make a copy, and run that, because one
        // of these Runnables might add call TApplication.invokeLater().
        List<Runnable> invokes = new ArrayList<Runnable>();
        synchronized (invokeLaters) {
            invokes.addAll(invokeLaters);
            invokeLaters.clear();
        }
        for (Runnable invoke: invokes) {
            invoke.run();
        }
        doRepaint();

        if (debugThreads) {
            System.err.printf(System.currentTimeMillis() + " " +
                Thread.currentThread() + " doIdle() - exit\n");
        }

    }

    /**
     * Wake the sleeping active event handler.
     */
    private void wakeEventHandler() {
        if (!started) {
            return;
        }

        if (secondaryEventHandler != null) {
            synchronized (secondaryEventHandler) {
                secondaryEventHandler.notify();
            }
        } else {
            assert (primaryEventHandler != null);
            synchronized (primaryEventHandler) {
                primaryEventHandler.notify();
            }
        }
    }

    /**
     * Wake the sleeping screen handler.
     */
    private void wakeScreenHandler() {
        if (!started) {
            return;
        }

        synchronized (screenHandler) {
            screenHandler.notify();
        }
    }

    // ------------------------------------------------------------------------
    // TApplication -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Place a command on the run queue, and run it before the next round of
     * checking I/O.
     *
     * @param command the command to run later
     */
    public void invokeLater(final Runnable command) {
        synchronized (invokeLaters) {
            invokeLaters.add(command);
        }
        doRepaint();
    }

    /**
     * Restore the console to sane defaults.  This is meant to be used for
     * improper exits (e.g. a caught exception in main()), and should not be
     * necessary for normal program termination.
     */
    public void restoreConsole() {
        if (backend != null) {
            if (backend instanceof ECMA48Backend) {
                backend.shutdown();
            }
        }
    }

    /**
     * Get the Backend.
     *
     * @return the Backend
     */
    public final Backend getBackend() {
        return backend;
    }

    /**
     * Get the Screen.
     *
     * @return the Screen
     */
    public final Screen getScreen() {
        if (backend instanceof TWindowBackend) {
            // We are being rendered to a TWindow.  We can't use its
            // getScreen() method because that is how it is rendering to a
            // hardware backend somewhere.  Instead use its getOtherScreen()
            // method.
            return ((TWindowBackend) backend).getOtherScreen();
        } else {
            return backend.getScreen();
        }
    }

    /**
     * Get the Locale used for producing user-facing strings.
     *
     * @return the locale
     */
    public final Locale getLocale() {
        assert (locale != null);
        return locale;
    }

    /**
     * Set the Locale used for producing user-facing strings.
     *
     * @param locale the locale. If null, reset to the default JVM Locale.
     */
    public final void setLocale(final Locale locale) {
        if (locale != null) {
            this.locale = locale;
        } else {
            this.locale = Locale.getDefault();
        }
        i18n = ResourceBundle.getBundle(TApplication.class.getName(),
            getLocale());
    }

    /**
     * Get the global color theme.
     *
     * @return the theme
     */
    public final ColorTheme getTheme() {
        return theme;
    }

    /**
     * Get the animations are enabled at this moment.
     *
     * @return true if animations are enabled and the window is focued
     */
    public boolean hasAnimations() {
        return (animationsEnabled && backend.isFocused());
    }

    /**
     * Get the animations option.
     *
     * @return true if animations are enabled
     */
    public boolean getAnimations() {
        return animationsEnabled;
    }

    /**
     * Set the animations option.
     *
     * @param enabled if true, animations will be enabled
     */
    public void setAnimations(final boolean enabled) {
        animationsEnabled = enabled;
    }

    /**
     * Get the translucence option.
     *
     * @return true if translucency is enabled
     */
    public boolean hasTranslucence() {
        return translucence;
    }

    /**
     * Set the translucence option.
     *
     * @param enabled if true, windows will be translucent
     */
    public void setTranslucence(final boolean enabled) {
        translucence = enabled;
    }

    /**
     * Set the opacity of all windows.  If opacity is 100, translucence is
     * also disabled for performance.
     *
     * @param opacity a number between 10 (nearly transparent) and 100 (fully
     * opaque)
     */
    public void setWindowOpacity(final int opacity) {
        if ((opacity < 10) || (opacity > 100)) {
            return;
        }
        if (opacity == 100) {
            translucence = false;
        } else {
            translucence = true;
        }

        int alpha = opacity * 255 / 100;
        for (TWindow window: windows) {
            window.setAlpha(alpha);
        }
    }

    /**
     * Get the number of frames that were emitted to output on the last
     * second.
     *
     * @return the frames per second
     */
    public int getFramesPerSecond() {
        if (screenHandler != null) {
            return screenHandler.framesPerSecond;
        }
        return 0;
    }

    /**
     * Get the clipboard.
     *
     * @return the clipboard
     */
    public final Clipboard getClipboard() {
        return clipboard;
    }

    /**
     * Repaint the screen on the next update.
     */
    public void doRepaint() {
        repaint = true;
        boolean wakeAndReturn = false;
        synchronized (drainEventQueue) {
            if (fillEventQueue.size() > 0) {
                // User input is waiting, that will update the screen.  Wake
                // the backend reader.
                if (debugEvents) {
                    System.err.printf("Drop: input waiting in backend\n");
                }
                wakeAndReturn = true;
            }
        }
        if (wakeAndReturn) {
            synchronized (this) {
                this.notify();
            }
            return;
        }
        if (screenHandler != null) {
            long now = System.currentTimeMillis();
            if (now - screenHandler.lastFlushTime < screenHandler.lastFrameTime) {
                // We cannot update the screen this quickly.  Drop this
                // request.
                if (debugEvents) {
                    System.err.printf("Drop: %d millis to render, %d since\n",
                        screenHandler.lastFrameTime,
                        now - screenHandler.lastFlushTime);
                }
                return;
            }
            screenHandler.setDirty();
        }

        // It's been enough time since the last frame, and no user input is
        // around, so allow a screen repaint.
        wakeEventHandler();
    }

    /**
     * Get mouse coordinate X.
     *
     * @return mouse X location on screen
     */
    public final int getMouseX() {
        return mouseX;
    }

    /**
     * Get mouse coordinate Y.
     *
     * @return mouse Y location on screen
     */
    public final int getMouseY() {
        return mouseY;
    }

    /**
     * Get Y coordinate of the top edge of the desktop.
     *
     * @return Y coordinate of the top edge of the desktop
     */
    public final int getDesktopTop() {
        return desktopTop;
    }

    /**
     * Get Y coordinate of the bottom edge of the desktop.
     *
     * @return Y coordinate of the bottom edge of the desktop
     */
    public final int getDesktopBottom() {
        return desktopBottom;
    }

    /**
     * Set the TDesktop instance.
     *
     * @param desktop a TDesktop instance, or null to remove the one that is
     * set
     */
    public final void setDesktop(final TDesktop desktop) {
        setDesktop(desktop, true);
    }

    /**
     * Set the TDesktop instance.
     *
     * @param desktop a TDesktop instance, or null to remove the one that is
     * set
     * @param close if true, close the previous desktop
     */
    public final void setDesktop(final TDesktop desktop, final boolean close) {
        if (this.desktop != null) {
            if (close) {
                this.desktop.onPreClose();
                this.desktop.onUnfocus();
                this.desktop.onClose();
            }
        }
        this.desktop = desktop;

        desktopTop = (hideMenuBar ? 0 : 1);
        desktopBottom = getScreen().getHeight() - 1 + (hideStatusBar ?
            1 : 0);

        if (desktop != null) {
            desktop.setDimensions(0, desktopTop, getScreen().getWidth(),
                (desktopBottom - desktopTop));
            TResizeEvent resize = new TResizeEvent(null,
                TResizeEvent.Type.WIDGET, getScreen().getWidth(),
                desktop.getHeight());
            desktop.onResize(resize);
        }

    }

    /**
     * Get the TDesktop instance.
     *
     * @return the desktop, or null if it is not set
     */
    public final TDesktop getDesktop() {
        return desktop;
    }

    /**
     * Get the current active window.
     *
     * @return the active window, or null if it is not set
     */
    public final TWindow getActiveWindow() {
        for (TWindow window: windows) {
            if (window.isShown() && window.isActive()) {
                return window;
            }
        }
        return null;
    }

    /**
     * Get a (shallow) copy of the window list.
     *
     * @return a copy of the list of windows for this application
     */
    public final List<TWindow> getAllWindows() {
        List<TWindow> result = new ArrayList<TWindow>();
        result.addAll(windows);
        return result;
    }

    /**
     * Get focusFollowsMouse flag.
     *
     * @return true if focus follows mouse: windows automatically raised if
     * the mouse passes over them
     */
    public boolean getFocusFollowsMouse() {
        return focusFollowsMouse;
    }

    /**
     * Set focusFollowsMouse flag.
     *
     * @param focusFollowsMouse if true, focus follows mouse: windows
     * automatically raised if the mouse passes over them
     */
    public void setFocusFollowsMouse(final boolean focusFollowsMouse) {
        this.focusFollowsMouse = focusFollowsMouse;
    }

    /**
     * Set hideMenuBar flag.
     *
     * @param hideMenuBar if true, the menu bar will not be visible, and the
     * desktop will cover the top line
     */
    public void setHideMenuBar(final boolean hideMenuBar) {
        this.hideMenuBar = hideMenuBar;
        desktopTop = (hideMenuBar ? 0 : 1);
        if (desktop != null) {
            desktop.setDimensions(0, desktopTop, getScreen().getWidth(),
                (desktopBottom - desktopTop));
            TResizeEvent resize = new TResizeEvent(null,
                TResizeEvent.Type.WIDGET, getScreen().getWidth(),
                desktop.getHeight());
            desktop.onResize(resize);
        }
        if (hideMenuBar == false) {
            // Push any windows that are on the top line down.
            for (TWindow window: windows) {
                if (window.getY() == 0) {
                    window.setY(1);
                }
            }
        }
    }

    /**
     * Set hideStatusBar flag.
     *
     * @param hideStatusBar if true, the status bar will not be visible, and
     * the desktop will cover the bottom line
     */
    public void setHideStatusBar(final boolean hideStatusBar) {
        this.hideStatusBar = hideStatusBar;
        desktopBottom = getScreen().getHeight() - 1 + (hideStatusBar ?
            1 : 0);
        if (desktop != null) {
            desktop.setDimensions(0, desktopTop, getScreen().getWidth(),
                (desktopBottom - desktopTop));
            TResizeEvent resize = new TResizeEvent(null,
                TResizeEvent.Type.WIDGET, getScreen().getWidth(),
                desktop.getHeight());
            desktop.onResize(resize);
        }
    }

    /**
     * Display the about dialog.
     */
    protected void showAboutDialog() {
        String version = getClass().getPackage().getImplementationVersion();
        if (version == null) {
            // This is Java 9+, use a hardcoded string here.
            version = "0.4";
        }
        messageBox(i18n.getString("aboutDialogTitle"),
            MessageFormat.format(i18n.getString("aboutDialogText"), version),
            TMessageBox.Type.OK);
    }

    /**
     * Handle the Tool | Open ANSI menu item.
     */
    private void openAnsiFile() {
        try {
            String filename = fileOpenBox(".", TFileOpenBox.Type.OPEN);
            if (filename != null) {
                new TTextPictureWindow(this, filename);
            }
        } catch (IOException e) {
            // Show this exception to the user.
            new TExceptionDialog(this, e);
        }
    }

    /**
     * Check if application is still running.
     *
     * @return true if the application is running
     */
    public final boolean isRunning() {
        if (quit == true) {
            return false;
        }
        return true;
    }

    /**
     * Check if a system-wide modal thread is running.  This thread is used
     * to drive a few special dialog boxes such as TMessageBox, TInputBox,
     * and TFileOpenBox.
     *
     * @return true if the modal (secondary) thread is running
     */
    public boolean isModalThreadRunning() {
        if (secondaryEventReceiver != null) {
            return true;
        }
        return false;
    }

    /**
     * Get the visible widget under the mouse position.
     *
     * @param mouse the mouse position
     * @return the widget, or null if the mouse is over the menu
     */
    private TWidget getWidgetUnderMouse(final TMouseEvent mouse) {
        TWidget activeWidget = null;
        for (TMenu menu: menus) {
            if (menu.isActive()) {
                return null;
            }
        }
        TWindow window = getActiveWindow();
        if (window == null) {
            return null;
        }
        return window.getWidgetUnderMouse(mouse);
    }

    // ------------------------------------------------------------------------
    // Screen refresh loop ----------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Function called immediately before the screen is drawn.  This can be
     * used by subclasses of TApplication to update things, for example to
     * set menuTrayText.
     */
    protected void onPreDraw() {
        // Default does nothing
    }

    /**
     * Function called immediately after the screen is drawn, while the
     * screen is still synchronized/locked.  This can be used by subclasses
     * of TApplication to alter the final post-rendered screen before it goes
     * out -- or even replace the entire thing such as a screensaver.
     */
    protected void onPostDraw() {
        // Default does nothing
    }

    /**
     * Draw the text mouse at position.
     *
     * @param x column position
     * @param y row position
     */
    private void drawTextMouse(final int x, final int y) {
        TWindow activeWindow = getActiveWindow();

        if (debugThreads) {
            System.err.printf("%d %s drawTextMouse() %d %d\n",
                System.currentTimeMillis(), Thread.currentThread(), x, y);

            if (activeWindow != null) {
                System.err.println("activeWindow.hasHiddenMouse() " +
                    activeWindow.hasHiddenMouse());
            }
        }

        // If this cell is on top of a visible window that has requested a
        // hidden mouse, bail out.
        if ((activeWindow != null) && (activeMenu == null)) {
            if ((activeWindow.hasHiddenMouse() == true)
                && (x > activeWindow.getX())
                && (x < activeWindow.getX() + activeWindow.getWidth() - 1)
                && (y > activeWindow.getY())
                && (y < activeWindow.getY() + activeWindow.getHeight() - 1)
            ) {
                return;
            }
        }

        // If this cell is on top of the desktop, and the desktop has
        // requested a hidden mouse, bail out.
        if ((desktop != null) && (activeWindow == null) && (activeMenu == null)) {
            if ((desktop.hasHiddenMouse() == true)
                && (x > desktop.getX())
                && (x < desktop.getX() + desktop.getWidth() - 1)
                && (y > desktop.getY())
                && (y < desktop.getY() + desktop.getHeight() - 1)
            ) {
                return;
            }
        }

        getScreen().invertCell(x, y);
    }

    /**
     * Draw a translucent window with a shadow on the screen.
     *
     * @param screen the screen
     * @param window the window
     */
    private void drawTranslucentWindow(final Screen screen,
        final TWindow window) {

        // Alpha blending: have the window draw to a snapshot of the screen
        // without alpha, and then merge it on the screen with alpha.
        int windowX = window.getX();
        int windowY = window.getY();
        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();
        Screen oldSnapshot = screen.snapshot(windowX, windowY,
            windowWidth, windowHeight);
        window.drawChildren();
        Screen newSnapshot = screen.snapshot(windowX, windowY,
            windowWidth, windowHeight);
        screen.copyScreen(oldSnapshot, windowX, windowY,
            windowWidth, windowHeight);
        screen.blendScreen(newSnapshot, windowX, windowY,
            windowWidth, windowHeight, window.getAlpha(), true);
        screen.resetClipping();

        if (window instanceof casciian.effect.WindowTransitionEffect.DummyWindow) {
            // Don't draw the shadow on a window effect dummy window.
            return;
        }

        // Recreate the shadow effect by blending a black rectangle over just
        // the shadow region.
        final int shadowOpacity = 30;
        final int shadowAlpha = shadowOpacity * window.getAlpha() / 100;
        screen.blendRectangle(windowX + windowWidth, windowY + 1,
            2, windowHeight - 1, 0x000000, shadowAlpha);
        screen.blendRectangle(windowX + 2, windowY + windowHeight,
            windowWidth, 1, 0x000000, shadowAlpha);
    }

    /**
     * Draw everything.
     */
    private void drawAll() {
        boolean menuIsActive = false;

        if (debugThreads) {
            System.err.printf("%d %s drawAll() enter\n",
                System.currentTimeMillis(), Thread.currentThread());
        }

        // I don't think this does anything useful anymore...
        if (!repaint) {
            if (debugThreads) {
                System.err.printf("%d %s drawAll() !repaint\n",
                    System.currentTimeMillis(), Thread.currentThread());
            }
            if ((oldDrawnMouseX != mouseX) || (oldDrawnMouseY != mouseY)) {
                if (debugThreads) {
                    System.err.printf("%d %s drawAll() !repaint MOUSE\n",
                        System.currentTimeMillis(), Thread.currentThread());
                }

                // The only thing that has happened is the mouse moved.

                // Redraw the old cell at that position, and save the cell at
                // the new mouse position.
                if (debugThreads) {
                    System.err.printf("%d %s restoreImage() %d %d\n",
                        System.currentTimeMillis(), Thread.currentThread(),
                        oldDrawnMouseX, oldDrawnMouseY);
                }
                getScreen().putCharXY(oldDrawnMouseX, oldDrawnMouseY,
                    oldDrawnMouseCell);
                oldDrawnMouseCell = getScreen().getCharXY(mouseX, mouseY);

                if (inScreenSelection) {
                    getScreen().setSelection(screenSelectionX0,
                        screenSelectionY0, screenSelectionX1, screenSelectionY1,
                        screenSelectionRectangle);
                }

                if ((textMouse == true) && (typingHidMouse == false)) {
                    // Draw mouse at the new position.
                    drawTextMouse(mouseX, mouseY);
                }

                oldDrawnMouseX = mouseX;
                oldDrawnMouseY = mouseY;
            }

            if (getScreen().isDirty()) {
                // Give subclass TApplications a chance to update the
                // post-rendered screen.
                onPostDraw();

                screenHandler.setDirty();
            }
            return;
        }

        if (debugThreads) {
            System.err.printf("%d %s drawAll() REDRAW\n",
                System.currentTimeMillis(), Thread.currentThread());
        }

        // If true, the cursor is not visible
        boolean cursor = false;

        // Start with a clean screen
        getScreen().clear();

        // Draw the desktop
        if (desktop != null) {
            desktop.drawChildren();
        }

        // Draw each window in reverse Z order
        List<TWindow> sorted = new ArrayList<TWindow>(windows);
        Collections.sort(sorted);
        TWindow topLevel = null;
        if (sorted.size() > 0) {
            topLevel = sorted.get(0);
        }
        Collections.reverse(sorted);
        for (TWindow window: sorted) {
            if (window.isShown()) {
                // Reset the screen clipping so we can draw the next window.
                getScreen().resetClipping();

                if (translucence) {
                    drawTranslucentWindow(getScreen(), window);
                } else {
                    window.drawChildren();
                }
            }
        }

        if (hideMenuBar == false) {
            // Draw the blank menubar line - reset the screen clipping first
            // so it won't trim it out.
            getScreen().resetClipping();
            getScreen().hLineXY(0, 0, getScreen().getWidth(), ' ',
                theme.getColor("tmenu"));
        }

        // Now draw the menus.
        int x = 1;
        for (TMenu menu: menus) {
            CellAttributes menuColor;
            CellAttributes menuMnemonicColor;
            if (menu.isActive()) {
                menuIsActive = true;
                if (!menu.isContext()) {
                    menuColor = theme.getColor("tmenu.highlighted");
                    menuMnemonicColor = theme.getColor("tmenu.mnemonic.highlighted");
                } else {
                    menuColor = theme.getColor("tmenu");
                    menuMnemonicColor = theme.getColor("tmenu.mnemonic");
                }
                topLevel = menu;
            } else {
                menuColor = theme.getColor("tmenu");
                menuMnemonicColor = theme.getColor("tmenu.mnemonic");
            }

            if (hideMenuBar == false) {
                // Draw the menu title
                getScreen().hLineXY(x, 0,
                    StringUtils.width(menu.getTitle()) + 2, ' ', menuColor);
                getScreen().putStringXY(x + 1, 0, menu.getTitle(), menuColor);
                // Draw the highlight character
                getScreen().putCharXY(x + 1 +
                    menu.getMnemonic().getScreenShortcutIdx(),
                    0, menu.getMnemonic().getShortcut(), menuMnemonicColor);
            }

            if (menu.isActive()) {
                if (translucence) {
                    drawTranslucentWindow(getScreen(), menu);
                } else {
                    ((TWindow) menu).drawChildren();
                }

                // Reset the screen clipping so we can draw the next title.
                getScreen().resetClipping();
            }
            x += StringUtils.width(menu.getTitle()) + 2;
        }

        for (TMenu menu: subMenus) {
            // Reset the screen clipping so we can draw the next sub-menu.
            getScreen().resetClipping();
            if (translucence) {
                drawTranslucentWindow(getScreen(), menu);
            } else {
                ((TWindow) menu).drawChildren();
            }
        }

        if (hideMenuBar == false) {
            if ((menuTrayText != null) && (menuTrayText.length() > 0)) {
                getScreen().resetClipping();
                getScreen().putStringXY(getScreen().getWidth() -
                    StringUtils.width(menuTrayText), 0, menuTrayText,
                    theme.getColor("tmenu"));
            }
        }

        getScreen().resetClipping();

        if (hideStatusBar == false) {
            // Draw the status bar of the top-level window
            TStatusBar statusBar = null;
            if (topLevel != null) {
                if (topLevel.isShown()) {
                    statusBar = topLevel.getStatusBar();
                }
            } else {
                if (desktop != null) {
                    statusBar = desktop.getStatusBar();
                }
            }

            if (statusBar != null) {
                getScreen().resetClipping();
                statusBar.setWidth(getScreen().getWidth());
                if (topLevel != null) {
                    statusBar.setY(getScreen().getHeight() - topLevel.getY());
                } else {
                    statusBar.setY(desktopBottom);
                }
                statusBar.draw();
            } else {
                CellAttributes barColor = new CellAttributes();
                barColor.setTo(getTheme().getColor("tstatusbar.text"));
                getScreen().hLineXY(0, desktopBottom, getScreen().getWidth(),
                    ' ', barColor);
            }
        }

        // Draw the mouse pointer
        if (debugThreads) {
            System.err.printf("%d %s restoreImage() %d %d\n",
                System.currentTimeMillis(), Thread.currentThread(),
                oldDrawnMouseX, oldDrawnMouseY);
        }
        oldDrawnMouseCell = getScreen().getCharXY(mouseX, mouseY);

        if (inScreenSelection) {
            getScreen().setSelection(screenSelectionX0, screenSelectionY0,
                screenSelectionX1, screenSelectionY1, screenSelectionRectangle);
        }

        // The active widget
        TWidget activeWidget = null;

        // Place the cursor if it is visible
        if (!menuIsActive) {

            int visibleWindowCount = 0;
            for (TWindow window: sorted) {
                if (window.isShown()) {
                    visibleWindowCount++;
                }
            }
            if ((visibleWindowCount == 0) || desktopCanHaveCursor) {
                // No windows are visible, only the desktop.  Allow it to
                // have the cursor.
                if (desktop != null) {
                    sorted.add(desktop);
                }
            }

            if (sorted.size() > 0) {
                activeWidget = sorted.get(sorted.size() - 1).getActiveChild();

                if (!(activeWidget instanceof TDesktop)) {
                    assert (activeWidget.isActive());
                    assert (activeWidget.isEnabled());
                }

                int cursorClipTop = desktopTop;
                int cursorClipBottom = desktopBottom;
                if (activeWidget.isCursorVisible()) {
                    if ((activeWidget.getCursorAbsoluteY() <= cursorClipBottom)
                        && (activeWidget.getCursorAbsoluteY() >= cursorClipTop)
                    ) {
                        getScreen().putCursor(true,
                            activeWidget.getCursorAbsoluteX(),
                            activeWidget.getCursorAbsoluteY());
                        cursor = true;
                    } else {
                        // Turn off the cursor.  Also place it at 0,0.
                        getScreen().putCursor(false, 0, 0);
                        cursor = false;
                    }
                }
            }
        }
        // Kill the cursor
        if (!cursor) {
            getScreen().hideCursor();
        }

        // Draw the mouse.
        getScreen().resetClipping();
        if ((textMouse == true) && (typingHidMouse == false)) {
            drawTextMouse(mouseX, mouseY);
        }
        oldDrawnMouseX = mouseX;
        oldDrawnMouseY = mouseY;

        if (getScreen().isDirty()) {
            // Give subclass TApplications a chance to update the
            // post-rendered screen.
            onPostDraw();

            screenHandler.setDirty();
        }
        repaint = false;
    }

    /**
     * Force this application to exit.
     */
    public void exit() {
        quit = true;
        synchronized (this) {
            this.notify();
        }
        if (screenHandler != null) {
            screenHandler.setDirty();
        }
    }

    /**
     * Subclasses can use this hook to cleanup resources.  Called as the last
     * step of TApplication.run().
     */
    public void onExit() {
        // Default does nothing.
    }

    // ------------------------------------------------------------------------
    // TWindow management -----------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Return the total number of windows.
     *
     * @return the total number of windows
     */
    public final int windowCount() {
        return windows.size();
    }

    /**
     * Return the number of windows that are showing.
     *
     * @return the number of windows that are showing on screen
     */
    public final int shownWindowCount() {
        int n = 0;
        for (TWindow w: windows) {
            if (w.isShown()) {
                n++;
            }
        }
        return n;
    }

    /**
     * Return the number of windows that are hidden.
     *
     * @return the number of windows that are hidden
     */
    public final int hiddenWindowCount() {
        int n = 0;
        for (TWindow w: windows) {
            if (w.isHidden()) {
                n++;
            }
        }
        return n;
    }

    /**
     * Check if a window instance is in this application's window list.
     *
     * @param window window to look for
     * @return true if this window is in the list
     */
    public final boolean hasWindow(final TWindow window) {
        if (windows.size() == 0) {
            return false;
        }
        for (TWindow w: windows) {
            if (w == window) {
                assert (window.getApplication() == this);
                return true;
            }
        }
        return false;
    }

    /**
     * Activate a window: bring it to the top and have it receive events.
     *
     * @param window the window to become the new active window
     */
    public final void activateWindow(final TWindow window) {
        if (hasWindow(window) == false) {
            /*
             * Someone has a handle to a window I don't have.  Ignore this
             * request.
             */
            return;
        }

        if (modalWindowActive() && !window.isModal()) {
            // Do not activate a non-modal on top of a modal.
            return;
        }

        synchronized (windows) {
            // Whatever window might be moving/dragging, stop it now.
            for (TWindow w: windows) {
                if (w.inMovements()) {
                    w.stopMovements();
                }
            }

            assert (windows.size() > 0);

            if (window.isHidden()) {
                // Unhiding will also activate.
                showWindow(window);
                return;
            }
            assert (window.isShown());

            if (windows.size() == 1) {
                assert (window == windows.get(0));
                window.setZ(0);
                window.setActive(true);
                window.onFocus();
                return;
            }

            if (getActiveWindow() == window) {
                assert (window.isActive());

                // Window is already active, do nothing.
                return;
            }

            assert (!window.isActive());

            window.setZ(-1);
            Collections.sort(windows);
            int newZ = 0;
            for (TWindow w: windows) {
                w.setZ(newZ);
                newZ++;
                if ((w != window) && w.isActive()) {
                    w.onUnfocus();
                }
                w.setActive(false);
            }
            window.setActive(true);
            window.onFocus();

        } // synchronized (windows)

        return;
    }

    /**
     * Hide a window.
     *
     * @param window the window to hide
     */
    public void hideWindow(final TWindow window) {
        if (hasWindow(window) == false) {
            /*
             * Someone has a handle to a window I don't have.  Ignore this
             * request.
             */
            return;
        }

        synchronized (windows) {

            // Whatever window might be moving/dragging, stop it now.
            for (TWindow w: windows) {
                if (w.inMovements()) {
                    w.stopMovements();
                }
            }

            assert (windows.size() > 0);

            if (window.hidden) {
                return;
            }

            window.setActive(false);
            window.hidden = true;
            window.onHide();

            TWindow activeWindow = null;
            for (TWindow w: windows) {
                if (w.isShown()) {
                    activeWindow = w;
                    break;
                }
            }
            assert (activeWindow != window);
            if (activeWindow != null) {
                activateWindow(activeWindow);
            }

        } // synchronized (windows)

    }

    /**
     * Show a window.
     *
     * @param window the window to show
     */
    public void showWindow(final TWindow window) {
        if (hasWindow(window) == false) {
            /*
             * Someone has a handle to a window I don't have.  Ignore this
             * request.
             */
            return;
        }

        if (window.hidden) {
            window.hidden = false;
            window.onShow();
            activateWindow(window);
        }

    }

    /**
     * Close window.
     *
     * @param window the window to remove
     */
    public final void closeWindow(final TWindow window) {
        if (hasWindow(window) == false) {
            /*
             * Someone has a handle to a window I don't have.  Ignore this
             * request.
             */
            return;
        }

        // Let window know that it is about to be closed, while it is still
        // visible on screen.
        window.onPreClose();

        // If the window has a close effect, kick that off.
        if (!window.disableCloseEffect() && animationsEnabled) {
            String windowCloseEffect = System.getProperty("casciian.effect.windowClose",
                "none").toLowerCase();

            if (windowCloseEffect.equals("fade")) {
                synchronized (windowEffects) {
                    windowEffects.add(new WindowFadeOutEffect(window));
                    needToRunWindowEffects = true;
                }
            }
            if (windowCloseEffect.equals("burn")) {
                synchronized (windowEffects) {
                    windowEffects.add(new WindowBurnOutEffect(window));
                    needToRunWindowEffects = true;
                }
            }
            if (windowCloseEffect.equals("wipeup")) {
                synchronized (windowEffects) {
                    windowEffects.add(new WindowWipeOutEffect(window,
                        WindowWipeOutEffect.Direction.UP));
                    needToRunWindowEffects = true;
                }
            }
            if (windowCloseEffect.equals("wipedown")) {
                synchronized (windowEffects) {
                    windowEffects.add(new WindowWipeOutEffect(window,
                        WindowWipeOutEffect.Direction.DOWN));
                    needToRunWindowEffects = true;
                }
            }
            if (windowCloseEffect.equals("wipeleft")) {
                synchronized (windowEffects) {
                    windowEffects.add(new WindowWipeOutEffect(window,
                        WindowWipeOutEffect.Direction.LEFT));
                    needToRunWindowEffects = true;
                }
            }
            if (windowCloseEffect.equals("wiperight")) {
                synchronized (windowEffects) {
                    windowEffects.add(new WindowWipeOutEffect(window,
                        WindowWipeOutEffect.Direction.RIGHT));
                    needToRunWindowEffects = true;
                }
            }
        }

        synchronized (windows) {

            window.stopMovements();
            window.onUnfocus();
            windows.remove(window);
            Collections.sort(windows);

            TWindow nextWindow = null;
            int newZ = 0;
            for (TWindow w: windows) {
                w.stopMovements();
                w.setZ(newZ);
                newZ++;

                // Do not activate a hidden window.
                if (w.isHidden()) {
                    continue;
                }
                if (nextWindow == null) {
                    nextWindow = w;
                } else {
                    if (w.isActive()) {
                        w.setActive(false);
                        w.onUnfocus();
                    }
                }
            }

            if (nextWindow != null) {
                nextWindow.setActive(true);
                nextWindow.onFocus();
            }

        } // synchronized (windows)

        // Perform window cleanup
        window.onClose();

        // Check if we are closing a TMessageBox or similar
        if (secondaryEventReceiver != null) {
            assert (secondaryEventHandler != null);

            // Do not send events to the secondaryEventReceiver anymore, the
            // window is closed.
            secondaryEventReceiver = null;

            // Wake the secondary thread, it will wake the primary as it
            // exits.
            synchronized (secondaryEventHandler) {
                secondaryEventHandler.notify();
            }

        } // synchronized (windows)

        // Permit desktop to be active if it is the only thing left.
        if (desktop != null) {
            if (windows.size() == 0) {
                desktop.setActive(true);
            }
        }
    }

    /**
     * Switch to the next window.
     *
     * @param forward if true, then switch to the next window in the list,
     * otherwise switch to the previous window in the list
     */
    public final void switchWindow(final boolean forward) {
        // Only switch if there are multiple visible windows
        if (shownWindowCount() < 2) {
            return;
        }

        if (modalWindowActive()) {
            // Do not switch if a window is modal
            return;
        }

        synchronized (windows) {

            TWindow window = windows.get(0);
            do {
                assert (window != null);
                if (forward) {
                    window.setZ(windows.size());
                } else {
                    TWindow lastWindow = windows.get(windows.size() - 1);
                    lastWindow.setZ(-1);
                }

                Collections.sort(windows);
                int newZ = 0;
                for (TWindow w: windows) {
                    w.setZ(newZ);
                    newZ++;
                }

                window = windows.get(0);
            } while (!window.isShown());

            // The next visible window is now on top.  Renumber the list.
            for (TWindow w: windows) {
                w.stopMovements();
                if ((w != window) && w.isActive()) {
                    assert (w.isShown());
                    w.setActive(false);
                    w.onUnfocus();
                }
            }

            // Next visible window is on top.
            assert (window.isShown());
            window.setActive(true);
            window.onFocus();

        } // synchronized (windows)
    }

    /**
     * Add a window to my window list and make it active.  Note package
     * private access.
     *
     * @param window new window to add
     */
    final void addWindowToApplication(final TWindow window) {

        // Do not add menu windows to the window list.
        if (window instanceof TMenu) {
            return;
        }

        // Do not add the desktop to the window list.
        if (window instanceof TDesktop) {
            return;
        }

        synchronized (windows) {
            if (windows.contains(window)) {
                throw new IllegalArgumentException("Window " + window +
                    " is already in window list");
            }

            // Whatever window might be moving/dragging, stop it now.
            for (TWindow w: windows) {
                if (w.inMovements()) {
                    w.stopMovements();
                }
            }

            // Do not allow a modal window to spawn a non-modal window.  If a
            // modal window is active, then this window will become modal
            // too.
            if (modalWindowActive()) {
                window.flags |= TWindow.MODAL;
                window.flags |= TWindow.CENTERED;
                window.hidden = false;
            }
            if (window.isShown()) {
                for (TWindow w: windows) {
                    if (w.isActive()) {
                        w.setActive(false);
                        w.onUnfocus();
                    }
                    w.setZ(w.getZ() + 1);
                }
                window.setZ(0);
                window.setActive(true);
                window.onFocus();
                windows.add(0, window);
            } else {
                window.setZ(windows.size());
                windows.add(window);
            }

            if (((window.flags & TWindow.CENTERED) == 0)
                && ((window.flags & TWindow.ABSOLUTEXY) == 0)
                && (smartWindowPlacement == true)
                && (!(window instanceof TDesktop))
            ) {

                doSmartPlacement(window);
            }
        }

        // Desktop cannot be active over any other window.
        if (desktop != null) {
            desktop.setActive(false);
        }

        if (!window.disableOpenEffect() && animationsEnabled) {
            // If the window has an open effect, kick that off.
            String windowOpenEffect = System.getProperty(
                "casciian.effect.windowOpen", "none").toLowerCase();

            if (windowOpenEffect.equals("fade")) {
                synchronized (windowEffects) {
                    windowEffects.add(new WindowFadeInEffect(window));
                    needToRunWindowEffects = true;
                }
            }
            if (windowOpenEffect.equals("burn")) {
                synchronized (windowEffects) {
                    windowEffects.add(new WindowBurnInEffect(window));
                    needToRunWindowEffects = true;
                }
            }
            if (windowOpenEffect.equals("wipeup")) {
                synchronized (windowEffects) {
                    windowEffects.add(new WindowWipeInEffect(window,
                        WindowWipeInEffect.Direction.UP));
                    needToRunWindowEffects = true;
                }
            }
            if (windowOpenEffect.equals("wipedown")) {
                synchronized (windowEffects) {
                    windowEffects.add(new WindowWipeInEffect(window,
                        WindowWipeInEffect.Direction.DOWN));
                    needToRunWindowEffects = true;
                }
            }
            if (windowOpenEffect.equals("wipeleft")) {
                synchronized (windowEffects) {
                    windowEffects.add(new WindowWipeInEffect(window,
                        WindowWipeInEffect.Direction.LEFT));
                    needToRunWindowEffects = true;
                }
            }
            if (windowOpenEffect.equals("wiperight")) {
                synchronized (windowEffects) {
                    windowEffects.add(new WindowWipeInEffect(window,
                        WindowWipeInEffect.Direction.RIGHT));
                    needToRunWindowEffects = true;
                }
            }
        }
    }

    /**
     * Check if there is a system-modal window on top.
     *
     * @return true if the active window is modal
     */
    private boolean modalWindowActive() {
        if (windows.size() == 0) {
            return false;
        }

        for (TWindow w: windows) {
            if (w.isModal()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if there is a window with overridden menu flag on top.
     *
     * @return true if the active window is overriding the menu
     */
    private boolean overrideMenuWindowActive() {
        TWindow activeWindow = getActiveWindow();
        if (activeWindow != null) {
            if (activeWindow.hasOverriddenMenu()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Close all open windows.
     */
    private void closeAllWindows() {
        // Don't do anything if we are in the menu
        if (activeMenu != null) {
            return;
        }
        while (windows.size() > 0) {
            closeWindow(windows.get(0));
        }
    }

    /**
     * Re-layout the open windows as non-overlapping tiles.  This produces
     * almost the same results as Turbo Pascal 7.0's IDE.
     */
    private void tileWindows() {
        synchronized (windows) {
            // Don't do anything if we are in the menu
            if (activeMenu != null) {
                return;
            }
            casciian.bits.WidgetUtils.tileWidgets(windows, getScreen().getWidth(),
                desktopBottom - desktopTop, desktopTop);
        }
    }

    /**
     * Re-layout the open windows as overlapping cascaded windows.
     */
    private void cascadeWindows() {
        synchronized (windows) {
            // Don't do anything if we are in the menu
            if (activeMenu != null) {
                return;
            }
            int x = 0;
            int y = 1;
            List<TWindow> sorted = new ArrayList<TWindow>(windows);
            Collections.sort(sorted);
            Collections.reverse(sorted);
            for (TWindow window: sorted) {
                window.setX(x);
                window.setY(y);
                x++;
                y++;
                if (x > getScreen().getWidth()) {
                    x = 0;
                }
                if (y >= getScreen().getHeight()) {
                    y = 1;
                }
            }
        }
    }

    /**
     * Place a window to minimize its overlap with other windows.
     *
     * @param window the window to place
     */
    public final void doSmartPlacement(final TWindow window) {
        // This is a pretty dumb algorithm, but seems to work.  The hardest
        // part is computing these "overlap" values seeking a minimum average
        // overlap.
        int xMin = 0;
        int yMin = desktopTop;
        int xMax = getScreen().getWidth() - window.getWidth() + 1;
        int yMax = desktopBottom  - window.getHeight() + 1;
        if (xMax < xMin) {
            xMax = xMin;
        }
        if (yMax < yMin) {
            yMax = yMin;
        }

        if ((xMin == xMax) && (yMin == yMax)) {
            // No work to do, bail out.
            return;
        }

        // Compute the overlap matrix without the new window.
        int width = getScreen().getWidth();
        int height = getScreen().getHeight();
        int overlapMatrix[][] = new int[width][height];
        for (TWindow w: windows) {
            if (window == w) {
                continue;
            }
            for (int x = w.getX(); x < w.getX() + w.getWidth(); x++) {
                if (x < 0) {
                    continue;
                }
                if (x >= width) {
                    continue;
                }
                for (int y = w.getY(); y < w.getY() + w.getHeight(); y++) {
                    if (y < 0) {
                        continue;
                    }
                    if (y >= height) {
                        continue;
                    }
                    overlapMatrix[x][y]++;
                }
            }
        }

        long oldOverlapTotal = 0;
        long oldOverlapN = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                oldOverlapTotal += overlapMatrix[x][y];
                if (overlapMatrix[x][y] > 0) {
                    oldOverlapN++;
                }
            }
        }


        double oldOverlapAvg = (double) oldOverlapTotal / (double) oldOverlapN;
        boolean first = true;
        int windowX = window.getX();
        int windowY = window.getY();

        // For each possible (x, y) position for the new window, compute a
        // new overlap matrix.
        for (int x = xMin; x < xMax; x++) {
            for (int y = yMin; y < yMax; y++) {

                // Start with the matrix minus this window.
                int newMatrix[][] = new int[width][height];
                for (int mx = 0; mx < width; mx++) {
                    for (int my = 0; my < height; my++) {
                        newMatrix[mx][my] = overlapMatrix[mx][my];
                    }
                }

                // Add this window's values to the new overlap matrix.
                long newOverlapTotal = 0;
                long newOverlapN = 0;
                // Start by adding each new cell.
                for (int wx = x; wx < x + window.getWidth(); wx++) {
                    if (wx >= width) {
                        continue;
                    }
                    for (int wy = y; wy < y + window.getHeight(); wy++) {
                        if (wy >= height) {
                            continue;
                        }
                        newMatrix[wx][wy]++;
                    }
                }
                // Now figure out the new value for total coverage.
                for (int mx = 0; mx < width; mx++) {
                    for (int my = 0; my < height; my++) {
                        newOverlapTotal += newMatrix[x][y];
                        if (newMatrix[mx][my] > 0) {
                            newOverlapN++;
                        }
                    }
                }
                double newOverlapAvg = (double) newOverlapTotal / (double) newOverlapN;

                if (first) {
                    // First time: just record what we got.
                    oldOverlapAvg = newOverlapAvg;
                    first = false;
                } else {
                    // All other times: pick a new best (x, y) and save the
                    // overlap value.
                    if (newOverlapAvg < oldOverlapAvg) {
                        windowX = x;
                        windowY = y;
                        oldOverlapAvg = newOverlapAvg;
                    }
                }

            } // for (int x = xMin; x < xMax; x++)

        } // for (int y = yMin; y < yMax; y++)

        // Finally, set the window's new coordinates.
        window.setX(windowX);
        window.setY(windowY);
    }

    // ------------------------------------------------------------------------
    // TMenu management -------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Check if a mouse event would hit either the active menu or any open
     * sub-menus.
     *
     * @param mouse mouse event
     * @return true if the mouse would hit the active menu or an open
     * sub-menu
     */
    private boolean mouseOnMenu(final TMouseEvent mouse) {
        assert (activeMenu != null);
        List<TMenu> menus = new ArrayList<TMenu>(subMenus);
        Collections.reverse(menus);
        for (TMenu menu: menus) {
            if (menu.mouseWouldHit(mouse)) {
                return true;
            }
        }
        return activeMenu.mouseWouldHit(mouse);
    }

    /**
     * See if we need to switch window or activate the menu based on
     * a mouse click.
     *
     * @param mouse mouse event
     */
    private void checkSwitchFocus(final TMouseEvent mouse) {

        if ((mouse.getType() == TMouseEvent.Type.MOUSE_DOWN)
            && (activeMenu != null)
            && (activeMenu.isContext())
            && (!mouseOnMenu(mouse))
        ) {
            // They clicked outside the active context menu, turn it off
            activeMenu.setActive(false);
            activeMenu.setContext(false);
            activeMenu = null;
            for (TMenu menu: subMenus) {
                menu.setActive(false);
                menu.setContext(false);
            }
            subMenus.clear();
            // Continue checks
        }

        if ((mouse.getType() == TMouseEvent.Type.MOUSE_DOWN)
            && (activeMenu != null)
            && (mouse.getAbsoluteY() != 0)
            && (!mouseOnMenu(mouse))
        ) {
            // They clicked outside the active non-context menu, turn it off
            activeMenu.setActive(false);
            assert (activeMenu.isContext() == false);
            activeMenu = null;
            for (TMenu menu: subMenus) {
                menu.setActive(false);
                menu.setContext(false);
            }
            subMenus.clear();
            // Continue checks
        }

        // See if they hit the menu bar
        if ((mouse.getType() == TMouseEvent.Type.MOUSE_DOWN)
            && (mouse.isMouse1())
            && (!modalWindowActive())
            && (!overrideMenuWindowActive())
            && (mouse.getAbsoluteY() == 0)
            && (hideMenuBar == false)
        ) {

            for (TMenu menu: subMenus) {
                menu.setActive(false);
                assert (menu.isContext() == false);
            }
            subMenus.clear();

            // They selected the menu, go activate it
            for (TMenu menu: menus) {
                if ((mouse.getAbsoluteX() >= menu.getTitleX())
                    && (mouse.getAbsoluteX() < menu.getTitleX()
                        + StringUtils.width(menu.getTitle()) + 2)
                ) {
                    menu.setActive(true);
                    assert (menu.isContext() == false);
                    activeMenu = menu;
                } else {
                    menu.setActive(false);
                }
            }
            return;
        }

        // See if they hit the menu bar
        if ((mouse.getType() == TMouseEvent.Type.MOUSE_MOTION)
            && (mouse.isMouse1())
            && (activeMenu != null)
            && (mouse.getAbsoluteY() == 0)
            && (hideMenuBar == false)
        ) {

            TMenu oldMenu = activeMenu;
            for (TMenu menu: subMenus) {
                menu.setActive(false);
                assert (menu.isContext() == false);
            }
            subMenus.clear();

            // See if we should switch menus
            for (TMenu menu: menus) {
                if ((mouse.getAbsoluteX() >= menu.getTitleX())
                    && (mouse.getAbsoluteX() < menu.getTitleX()
                        + StringUtils.width(menu.getTitle()) + 2)
                ) {
                    menu.setActive(true);
                    assert (menu.isContext() == false);
                    activeMenu = menu;
                }
            }
            if (oldMenu != activeMenu) {
                // They switched menus
                oldMenu.setActive(false);
            }
            return;
        }

        // If a menu is still active, don't switch windows
        if (activeMenu != null) {
            return;
        }

        // Only switch if there are multiple windows
        if (windows.size() < 2) {
            return;
        }

        // If the mouse is on the status bar, do not switch focus
        if ((hideStatusBar == false)
            && (mouse.getAbsoluteY() == getDesktopBottom())
        ) {
            return;
        }

        if (((focusFollowsMouse == true)
                && (mouse.getType() == TMouseEvent.Type.MOUSE_MOTION))
            || (mouse.getType() == TMouseEvent.Type.MOUSE_DOWN)
        ) {
            synchronized (windows) {
                if (windows.get(0).isModal()) {
                    // Modal windows don't switch
                    return;
                }

                if (windows.get(0).inMovements()) {
                    // Moving/resizing windows don't switch
                    return;
                }

                for (TWindow window: windows) {
                    assert (!window.isModal());

                    if (window.isHidden()) {
                        assert (!window.isActive());
                        continue;
                    }

                    if (window.mouseWouldHit(mouse)) {
                        activateWindow(window);
                        return;
                    }
                }
            }

            // Clicked on the background, nothing to do
            return;
        }

        // Nothing to do: this isn't a mouse up, or focus isn't following
        // mouse.
        return;
    }

    /**
     * Turn off the menu.
     */
    public final void closeMenu() {
        if (activeMenu != null) {
            activeMenu.setActive(false);
            if (activeMenu.isContext()) {
                activeMenu.setY(0);
                activeMenu.setContext(false);
            }
            activeMenu = null;
            for (TMenu menu: subMenus) {
                menu.setActive(false);
                menu.setContext(false);
            }
            subMenus.clear();
        }
    }

    /**
     * Get a (shallow) copy of the menu list.
     *
     * @return a copy of the menu list
     */
    public final List<TMenu> getAllMenus() {
        return new ArrayList<TMenu>(menus);
    }

    /**
     * Open a previously created menu as a context menu at a specific
     * location.
     *
     * @param menu the menu to open
     * @param x the context menu X position
     * @param y the context menu Y position
     * @throws IllegalArgumentException if the menu is already used in
     * another TApplication
     */
    public final void openContextMenu(final TMenu menu, final int x,
        final int y) {

        if ((menu.getApplication() != null)
            && (menu.getApplication() != this)
        ) {
            throw new IllegalArgumentException("Menu " + menu + " is already " +
                "part of application " + menu.getApplication());
        }

        assert (activeMenu == null);

        menu.setContext(true, x, y);
        menu.setActive(true);
        activeMenu = menu;
    }


    /**
     * Add a top-level menu to the list.
     *
     * @param menu the menu to add
     * @throws IllegalArgumentException if the menu is already used in
     * another TApplication
     */
    public final void addMenu(final TMenu menu) {
        if ((menu.getApplication() != null)
            && (menu.getApplication() != this)
        ) {
            throw new IllegalArgumentException("Menu " + menu + " is already " +
                "part of application " + menu.getApplication());
        }
        closeMenu();
        menus.add(menu);
        recomputeMenuX();
    }

    /**
     * Remove the keyboard accelerators of a menu item.  Sub-menu's will be
     * searched recursively.
     *
     * @param item the item with the keyboard accelerator to remove
     */
    private final void removeMenuAccelerator(final TMenuItem item) {
        if (item instanceof TSubMenu) {
            for (TMenuItem subMenuItem: ((TSubMenu) item).getMenuItems()) {
                removeMenuAccelerator(subMenuItem);
            }
            return;
        }

        TKeypress key = item.getKey();
        if (key != null) {
            synchronized (accelerators) {
                accelerators.remove(key.toLowerCase());
            }
        }
    }

    /**
     * Remove a top-level menu from the list.
     *
     * @param menu the menu to remove
     * @throws IllegalArgumentException if the menu is already used in
     * another TApplication
     */
    public final void removeMenu(final TMenu menu) {
        if ((menu.getApplication() != null)
            && (menu.getApplication() != this)
        ) {
            throw new IllegalArgumentException("Menu " + menu + " is already " +
                "part of application " + menu.getApplication());
        }
        closeMenu();
        menus.remove(menu);

        // Remove keyboard accelerators, recursively.
        for (TWidget w: menu.getChildren()) {
            if (w instanceof TMenuItem) {
                if (w instanceof TMenuItem) {
                    TMenuItem thisItem = (TMenuItem) w;
                    removeMenuAccelerator(thisItem);
                    List<TMenuItem> newMenuItems = new ArrayList<TMenuItem>();
                    for (TMenuItem item: menuItems) {
                        if (item.getId() != thisItem.getId()) {
                            newMenuItems.add(item);
                        }
                    }
                    menuItems = newMenuItems;
                }
            }
        }

        recomputeMenuX();
    }

    /**
     * Turn off a sub-menu.
     */
    public final void closeSubMenu() {
        assert (activeMenu != null);
        TMenu item = subMenus.get(subMenus.size() - 1);
        assert (item != null);
        item.setActive(false);
        subMenus.remove(subMenus.size() - 1);
    }

    /**
     * Switch to the next menu.
     *
     * @param forward if true, then switch to the next menu in the list,
     * otherwise switch to the previous menu in the list
     */
    public final void switchMenu(final boolean forward) {
        assert (activeMenu != null);
        assert (hideMenuBar == false);

        for (TMenu menu: subMenus) {
            menu.setActive(false);
        }
        subMenus.clear();

        for (int i = 0; i < menus.size(); i++) {
            if (activeMenu == menus.get(i)) {
                if (forward) {
                    if (i < menus.size() - 1) {
                        i++;
                    } else {
                        i = 0;
                    }
                } else {
                    if (i > 0) {
                        i--;
                    } else {
                        i = menus.size() - 1;
                    }
                }
                activeMenu.setActive(false);
                activeMenu = menus.get(i);
                activeMenu.setActive(true);
                return;
            }
        }
    }

    /**
     * Add a menu item to the global list.  If it has a keyboard accelerator,
     * that will be added the global hash.
     *
     * @param item the menu item
     */
    public final void addMenuItem(final TMenuItem item) {
        menuItems.add(item);

        TKeypress key = item.getKey();
        if (key != null) {
            synchronized (accelerators) {
                assert (accelerators.get(key) == null);
                accelerators.put(key.toLowerCase(), item);
            }
        }
    }

    /**
     * Disable one menu item.
     *
     * @param id the menu item ID
     */
    public final void disableMenuItem(final int id) {
        for (TMenuItem item: menuItems) {
            if (item.getId() == id) {
                item.setEnabled(false);
            }
        }
    }

    /**
     * Disable the range of menu items with ID's between lower and upper,
     * inclusive.
     *
     * @param lower the lowest menu item ID
     * @param upper the highest menu item ID
     */
    public final void disableMenuItems(final int lower, final int upper) {
        for (TMenuItem item: menuItems) {
            if ((item.getId() >= lower) && (item.getId() <= upper)) {
                item.setEnabled(false);
                item.getParent().activate(0);
            }
        }
    }

    /**
     * Enable one menu item.
     *
     * @param id the menu item ID
     */
    public final void enableMenuItem(final int id) {
        for (TMenuItem item: menuItems) {
            if (item.getId() == id) {
                item.setEnabled(true);
                item.getParent().activate(0);
            }
        }
    }

    /**
     * Enable the range of menu items with ID's between lower and upper,
     * inclusive.
     *
     * @param lower the lowest menu item ID
     * @param upper the highest menu item ID
     */
    public final void enableMenuItems(final int lower, final int upper) {
        for (TMenuItem item: menuItems) {
            if ((item.getId() >= lower) && (item.getId() <= upper)) {
                item.setEnabled(true);
                item.getParent().activate(0);
            }
        }
    }

    /**
     * Get the menu item associated with this ID.
     *
     * @param id the menu item ID
     * @return the menu item, or null if not found
     */
    public final TMenuItem getMenuItem(final int id) {
        for (TMenuItem item: menuItems) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }

    /**
     * Recompute menu x positions based on their title length.
     */
    public final void recomputeMenuX() {
        int x = 0;
        for (TMenu menu: menus) {
            menu.setX(x);
            menu.setTitleX(x);
            x += StringUtils.width(menu.getTitle()) + 2;

            // Don't let the menu window exceed the screen width
            int rightEdge = menu.getX() + menu.getWidth();
            if (rightEdge > getScreen().getWidth()) {
                menu.setX(getScreen().getWidth() - menu.getWidth());
            }
        }
    }

    /**
     * Post an event to process.
     *
     * @param event new event to add to the queue
     */
    public final void postEvent(final TInputEvent event) {
        synchronized (this) {
            synchronized (fillEventQueue) {
                fillEventQueue.add(event);
            }
            if (debugThreads) {
                System.err.println(System.currentTimeMillis() + " " +
                    Thread.currentThread() + " postEvent() wake up main");
            }
            this.notify();
        }
    }

    /**
     * Post an event to process and turn off the menu.
     *
     * @param event new event to add to the queue
     */
    public final void postMenuEvent(final TInputEvent event) {
        synchronized (this) {
            synchronized (fillEventQueue) {
                fillEventQueue.add(event);
            }
            if (debugThreads) {
                System.err.println(System.currentTimeMillis() + " " +
                    Thread.currentThread() + " postMenuEvent() wake up main");
            }
            closeMenu();
            this.notify();
        }
    }

    /**
     * Add a sub-menu to the list of open sub-menus.
     *
     * @param menu sub-menu
     */
    public final void addSubMenu(final TMenu menu) {
        subMenus.add(menu);
    }

    /**
     * Convenience function to add a top-level menu.
     *
     * @param title menu title
     * @return the new menu
     */
    public final TMenu addMenu(final String title) {
        int x = 0;
        int y = 0;
        TMenu menu = new TMenu(this, x, y, title);
        menus.add(menu);
        recomputeMenuX();
        return menu;
    }

    /**
     * Convenience function to add a default tools (hamburger) menu.
     *
     * @return the new menu
     */
    public final TMenu addToolMenu() {
        TMenu toolMenu = addMenu(i18n.getString("toolMenuTitle"));
        toolMenu.addDefaultItem(TMenu.MID_REPAINT);
        toolMenu.addSeparator();
        toolMenu.addDefaultItem(TMenu.MID_VIEW_ANSI);
        toolMenu.addSeparator();
        toolMenu.addDefaultItem(TMenu.MID_TEXT_CURSOR_GLINT);
        TStatusBar toolStatusBar = toolMenu.newStatusBar(i18n.
            getString("toolMenuStatus"));
        toolStatusBar.addShortcutKeypress(kbF1, cmHelp, i18n.getString("Help"));
        return toolMenu;
    }

    /**
     * Convenience function to add a default "File" menu.
     *
     * @return the new menu
     */
    public final TMenu addFileMenu() {
        TMenu fileMenu = addMenu(i18n.getString("fileMenuTitle"));
        fileMenu.addDefaultItem(TMenu.MID_SHELL);
        fileMenu.addSeparator();
        fileMenu.addDefaultItem(TMenu.MID_EXIT);
        TStatusBar statusBar = fileMenu.newStatusBar(i18n.
            getString("fileMenuStatus"));
        statusBar.addShortcutKeypress(kbF1, cmHelp, i18n.getString("Help"));
        return fileMenu;
    }

    /**
     * Convenience function to add a default "Edit" menu.
     *
     * @return the new menu
     */
    public final TMenu addEditMenu() {
        TMenu editMenu = addMenu(i18n.getString("editMenuTitle"));
        editMenu.addDefaultItem(TMenu.MID_UNDO, false);
        editMenu.addDefaultItem(TMenu.MID_REDO, false);
        editMenu.addSeparator();
        editMenu.addDefaultItem(TMenu.MID_CUT, false);
        editMenu.addDefaultItem(TMenu.MID_COPY, false);
        editMenu.addDefaultItem(TMenu.MID_PASTE, false);
        editMenu.addDefaultItem(TMenu.MID_CLEAR, false);
        TStatusBar statusBar = editMenu.newStatusBar(i18n.
            getString("editMenuStatus"));
        statusBar.addShortcutKeypress(kbF1, cmHelp, i18n.getString("Help"));
        return editMenu;
    }

    /**
     * Convenience function to add a default "Window" menu.
     *
     * @return the new menu
     */
    public final TMenu addWindowMenu() {
        TMenu windowMenu = addMenu(i18n.getString("windowMenuTitle"));
        windowMenu.addDefaultItem(TMenu.MID_TILE);
        windowMenu.addDefaultItem(TMenu.MID_CASCADE);
        windowMenu.addDefaultItem(TMenu.MID_CLOSE_ALL);
        windowMenu.addSeparator();
        windowMenu.addDefaultItem(TMenu.MID_WINDOW_MOVE);
        windowMenu.addDefaultItem(TMenu.MID_WINDOW_ZOOM);
        windowMenu.addDefaultItem(TMenu.MID_WINDOW_NEXT);
        windowMenu.addDefaultItem(TMenu.MID_WINDOW_PREVIOUS);
        windowMenu.addDefaultItem(TMenu.MID_WINDOW_CLOSE);
        TStatusBar statusBar = windowMenu.newStatusBar(i18n.
            getString("windowMenuStatus"));
        statusBar.addShortcutKeypress(kbF1, cmHelp, i18n.getString("Help"));
        return windowMenu;
    }

    /**
     * Convenience function to add a default "Help" menu.
     *
     * @return the new menu
     */
    public final TMenu addHelpMenu() {
        TMenu helpMenu = addMenu(i18n.getString("helpMenuTitle"));
        helpMenu.addDefaultItem(TMenu.MID_HELP_CONTENTS);
        helpMenu.addDefaultItem(TMenu.MID_HELP_INDEX);
        helpMenu.addDefaultItem(TMenu.MID_HELP_SEARCH);
        helpMenu.addDefaultItem(TMenu.MID_HELP_PREVIOUS);
        helpMenu.addDefaultItem(TMenu.MID_HELP_HELP);
        helpMenu.addDefaultItem(TMenu.MID_HELP_ACTIVE_FILE);
        helpMenu.addSeparator();
        helpMenu.addDefaultItem(TMenu.MID_TERMINAL_INFORMATION);
        helpMenu.addDefaultItem(TMenu.MID_ABOUT);
        TStatusBar statusBar = helpMenu.newStatusBar(i18n.
            getString("helpMenuStatus"));
        statusBar.addShortcutKeypress(kbF1, cmHelp, i18n.getString("Help"));
        return helpMenu;
    }

    /**
     * Convenience function to add a default "Table" menu.
     *
     * @return the new menu
     */
    public final TMenu addTableMenu() {
        TMenu tableMenu = addMenu(i18n.getString("tableMenuTitle"));
        tableMenu.addDefaultItem(TMenu.MID_TABLE_RENAME_COLUMN, false);
        tableMenu.addDefaultItem(TMenu.MID_TABLE_RENAME_ROW, false);
        tableMenu.addSeparator();

        TSubMenu viewMenu = tableMenu.addSubMenu(i18n.
            getString("tableSubMenuView"));
        viewMenu.addDefaultItem(TMenu.MID_TABLE_VIEW_ROW_LABELS, false);
        viewMenu.addDefaultItem(TMenu.MID_TABLE_VIEW_COLUMN_LABELS, false);
        viewMenu.addDefaultItem(TMenu.MID_TABLE_VIEW_HIGHLIGHT_ROW, false);
        viewMenu.addDefaultItem(TMenu.MID_TABLE_VIEW_HIGHLIGHT_COLUMN, false);

        TSubMenu borderMenu = tableMenu.addSubMenu(i18n.
            getString("tableSubMenuBorders"));
        borderMenu.addDefaultItem(TMenu.MID_TABLE_BORDER_NONE, false);
        borderMenu.addDefaultItem(TMenu.MID_TABLE_BORDER_ALL, false);
        borderMenu.addDefaultItem(TMenu.MID_TABLE_BORDER_CELL_NONE, false);
        borderMenu.addDefaultItem(TMenu.MID_TABLE_BORDER_CELL_ALL, false);
        borderMenu.addDefaultItem(TMenu.MID_TABLE_BORDER_RIGHT, false);
        borderMenu.addDefaultItem(TMenu.MID_TABLE_BORDER_LEFT, false);
        borderMenu.addDefaultItem(TMenu.MID_TABLE_BORDER_TOP, false);
        borderMenu.addDefaultItem(TMenu.MID_TABLE_BORDER_BOTTOM, false);
        borderMenu.addDefaultItem(TMenu.MID_TABLE_BORDER_DOUBLE_BOTTOM, false);
        borderMenu.addDefaultItem(TMenu.MID_TABLE_BORDER_THICK_BOTTOM, false);
        TSubMenu deleteMenu = tableMenu.addSubMenu(i18n.
            getString("tableSubMenuDelete"));
        deleteMenu.addDefaultItem(TMenu.MID_TABLE_DELETE_LEFT, false);
        deleteMenu.addDefaultItem(TMenu.MID_TABLE_DELETE_UP, false);
        deleteMenu.addDefaultItem(TMenu.MID_TABLE_DELETE_ROW, false);
        deleteMenu.addDefaultItem(TMenu.MID_TABLE_DELETE_COLUMN, false);
        TSubMenu insertMenu = tableMenu.addSubMenu(i18n.
            getString("tableSubMenuInsert"));
        insertMenu.addDefaultItem(TMenu.MID_TABLE_INSERT_LEFT, false);
        insertMenu.addDefaultItem(TMenu.MID_TABLE_INSERT_RIGHT, false);
        insertMenu.addDefaultItem(TMenu.MID_TABLE_INSERT_ABOVE, false);
        insertMenu.addDefaultItem(TMenu.MID_TABLE_INSERT_BELOW, false);
        TSubMenu columnMenu = tableMenu.addSubMenu(i18n.
            getString("tableSubMenuColumn"));
        columnMenu.addDefaultItem(TMenu.MID_TABLE_COLUMN_NARROW, false);
        columnMenu.addDefaultItem(TMenu.MID_TABLE_COLUMN_WIDEN, false);
        TSubMenu fileMenu = tableMenu.addSubMenu(i18n.
            getString("tableSubMenuFile"));
        fileMenu.addDefaultItem(TMenu.MID_TABLE_FILE_OPEN_CSV, false);
        fileMenu.addDefaultItem(TMenu.MID_TABLE_FILE_SAVE_CSV, false);
        fileMenu.addDefaultItem(TMenu.MID_TABLE_FILE_SAVE_TEXT, false);

        TStatusBar statusBar = tableMenu.newStatusBar(i18n.
            getString("tableMenuStatus"));
        statusBar.addShortcutKeypress(kbF1, cmHelp, i18n.getString("Help"));
        return tableMenu;
    }

    // ------------------------------------------------------------------------
    // TTimer management ------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the amount of time I can sleep before missing a Timer tick.
     *
     * @param timeout = initial (maximum) timeout in millis
     * @return number of milliseconds between now and the next timer event
     */
    private long getSleepTime(final long timeout) {
        Date now = new Date();
        long nowTime = now.getTime();
        long sleepTime = timeout;

        synchronized (timers) {
            for (TTimer timer: timers) {
                long nextTickTime = timer.getNextTick().getTime();
                if (nextTickTime < nowTime) {
                    return 0;
                }

                long timeDifference = nextTickTime - nowTime;
                if (timeDifference < sleepTime) {
                    sleepTime = timeDifference;
                }
            }
        }

        assert (sleepTime >= 0);
        assert (sleepTime <= timeout);
        return sleepTime;
    }

    /**
     * Convenience function to add a timer.
     *
     * @param duration number of milliseconds to wait between ticks
     * @param recurring if true, re-schedule this timer after every tick
     * @param action function to call when button is pressed
     * @return the timer
     */
    public final TTimer addTimer(final long duration, final boolean recurring,
        final TAction action) {

        TTimer timer = new TTimer(duration, recurring, action);
        synchronized (timers) {
            timers.add(timer);
        }
        return timer;
    }

    /**
     * Convenience function to remove a timer.
     *
     * @param timer timer to remove
     */
    public final void removeTimer(final TTimer timer) {
        synchronized (timers) {
            timers.remove(timer);
        }
    }

    // ------------------------------------------------------------------------
    // Other TWindow constructors ---------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Convenience function to spawn a message box.
     *
     * @param title window title, will be centered along the top border
     * @param caption message to display.  Use embedded newlines to get a
     * multi-line box.
     * @return the new message box
     */
    public final TMessageBox messageBox(final String title,
        final String caption) {

        return new TMessageBox(this, title, caption, TMessageBox.Type.OK);
    }

    /**
     * Convenience function to spawn a message box.
     *
     * @param title window title, will be centered along the top border
     * @param caption message to display.  Use embedded newlines to get a
     * multi-line box.
     * @param type one of the TMessageBox.Type constants.  Default is
     * Type.OK.
     * @return the new message box
     */
    public final TMessageBox messageBox(final String title,
        final String caption, final TMessageBox.Type type) {

        return new TMessageBox(this, title, caption, type);
    }

    /**
     * Convenience function to spawn an input box.
     *
     * @param title window title, will be centered along the top border
     * @param caption message to display.  Use embedded newlines to get a
     * multi-line box.
     * @return the new input box
     */
    public final TInputBox inputBox(final String title, final String caption) {

        return new TInputBox(this, title, caption);
    }

    /**
     * Convenience function to spawn an input box.
     *
     * @param title window title, will be centered along the top border
     * @param caption message to display.  Use embedded newlines to get a
     * multi-line box.
     * @param text initial text to seed the field with
     * @return the new input box
     */
    public final TInputBox inputBox(final String title, final String caption,
        final String text) {

        return new TInputBox(this, title, caption, text);
    }

    /**
     * Convenience function to spawn an input box.
     *
     * @param title window title, will be centered along the top border
     * @param caption message to display.  Use embedded newlines to get a
     * multi-line box.
     * @param text initial text to seed the field with
     * @param type one of the Type constants.  Default is Type.OK.
     * @return the new input box
     */
    public final TInputBox inputBox(final String title, final String caption,
        final String text, final TInputBox.Type type) {

        return new TInputBox(this, title, caption, text, type);
    }

    /**
     * Convenience function to open a terminal window.
     *
     * @param x column relative to parent
     * @param y row relative to parent
     * @return the terminal new window
     */
    public final TTerminalWindow openTerminal(final int x, final int y) {
        return openTerminal(x, y, TWindow.RESIZABLE);
    }

    /**
     * Convenience function to open a terminal window.
     *
     * @param x column relative to parent
     * @param y row relative to parent
     * @param closeOnExit if true, close the window when the command exits
     * @return the terminal new window
     */
    public final TTerminalWindow openTerminal(final int x, final int y,
        final boolean closeOnExit) {

        return openTerminal(x, y, TWindow.RESIZABLE, closeOnExit);
    }

    /**
     * Convenience function to open a terminal window.
     *
     * @param x column relative to parent
     * @param y row relative to parent
     * @param flags mask of CENTERED, MODAL, or RESIZABLE
     * @return the terminal new window
     */
    public final TTerminalWindow openTerminal(final int x, final int y,
        final int flags) {

        return new TTerminalWindow(this, x, y, flags);
    }

    /**
     * Convenience function to open a terminal window.
     *
     * @param x column relative to parent
     * @param y row relative to parent
     * @param flags mask of CENTERED, MODAL, or RESIZABLE
     * @param closeOnExit if true, close the window when the command exits
     * @return the terminal new window
     */
    public final TTerminalWindow openTerminal(final int x, final int y,
        final int flags, final boolean closeOnExit) {

        return new TTerminalWindow(this, x, y, flags, closeOnExit);
    }

    /**
     * Convenience function to open a terminal window and execute a custom
     * command line inside it.
     *
     * @param x column relative to parent
     * @param y row relative to parent
     * @param commandLine the command line to execute
     * @return the terminal new window
     */
    public final TTerminalWindow openTerminal(final int x, final int y,
        final String commandLine) {

        return openTerminal(x, y, TWindow.RESIZABLE, commandLine);
    }

    /**
     * Convenience function to open a terminal window and execute a custom
     * command line inside it.
     *
     * @param x column relative to parent
     * @param y row relative to parent
     * @param commandLine the command line to execute
     * @param closeOnExit if true, close the window when the command exits
     * @return the terminal new window
     */
    public final TTerminalWindow openTerminal(final int x, final int y,
        final String commandLine, final boolean closeOnExit) {

        return openTerminal(x, y, TWindow.RESIZABLE, commandLine, closeOnExit);
    }

    /**
     * Convenience function to open a terminal window and execute a custom
     * command line inside it.
     *
     * @param x column relative to parent
     * @param y row relative to parent
     * @param flags mask of CENTERED, MODAL, or RESIZABLE
     * @param command the command line to execute, as an array of strings
     * which signifies the external program file to be invoked (command[0])
     * and its arguments, if any (command[1], command[2], ...). Refer also to
     * java.lang.ProcessBuilder for further operating-system specific
     * details.
     * @return the terminal new window
     */
    public final TTerminalWindow openTerminal(final int x, final int y,
        final int flags, final String [] command) {

        if (command.length == 0) {
            return new TTerminalWindow(this, x, y, flags);
        }
        return new TTerminalWindow(this, x, y, flags, command);
    }

    /**
     * Convenience function to open a terminal window and execute a custom
     * command line inside it.
     *
     * @param x column relative to parent
     * @param y row relative to parent
     * @param flags mask of CENTERED, MODAL, or RESIZABLE
     * @param command the command line to execute, as an array of strings
     * which signifies the external program file to be invoked (command[0])
     * and its arguments, if any (command[1], command[2], ...). Refer also to
     * java.lang.ProcessBuilder for further operating-system specific
     * details.
     * @param closeOnExit if true, close the window when the command exits
     * @return the terminal new window
     */
    public final TTerminalWindow openTerminal(final int x, final int y,
        final int flags, final String [] command, final boolean closeOnExit) {

        return new TTerminalWindow(this, x, y, flags, command, closeOnExit);
    }

    /**
     * Convenience function to open a terminal window and execute a custom
     * command line inside it.
     *
     * @param x column relative to parent
     * @param y row relative to parent
     * @param flags mask of CENTERED, MODAL, or RESIZABLE
     * @param commandLine the command line to execute
     * @return the terminal new window
     */
    public final TTerminalWindow openTerminal(final int x, final int y,
        final int flags, final String commandLine) {

        return new TTerminalWindow(this, x, y, flags, commandLine.split("\\s+"));
    }

    /**
     * Convenience function to open a terminal window and execute a custom
     * command line inside it.
     *
     * @param x column relative to parent
     * @param y row relative to parent
     * @param flags mask of CENTERED, MODAL, or RESIZABLE
     * @param commandLine the command line to execute
     * @param closeOnExit if true, close the window when the command exits
     * @return the terminal new window
     */
    public final TTerminalWindow openTerminal(final int x, final int y,
        final int flags, final String commandLine, final boolean closeOnExit) {

        return new TTerminalWindow(this, x, y, flags, commandLine.split("\\s+"),
            closeOnExit);
    }

    /**
     * Convenience function to spawn an file open box.
     *
     * @param path path of selected file
     * @return the result of the new file open box
     * @throws IOException if java.io operation throws
     */
    public final String fileOpenBox(final String path) throws IOException {

        TFileOpenBox box = new TFileOpenBox(this, path, TFileOpenBox.Type.OPEN);
        return box.getFilename();
    }

    /**
     * Convenience function to spawn an file open box.
     *
     * @param path path of selected file
     * @param type one of the Type constants
     * @return the result of the new file open box
     * @throws IOException if java.io operation throws
     */
    public final String fileOpenBox(final String path,
        final TFileOpenBox.Type type) throws IOException {

        TFileOpenBox box = new TFileOpenBox(this, path, type);
        return box.getFilename();
    }

    /**
     * Convenience function to spawn a file open box.
     *
     * @param path path of selected file
     * @param type one of the Type constants
     * @param filter a string that files must match to be displayed
     * @return the result of the new file open box
     * @throws IOException of a java.io operation throws
     */
    public final String fileOpenBox(final String path,
        final TFileOpenBox.Type type, final String filter) throws IOException {

        ArrayList<String> filters = new ArrayList<String>();
        filters.add(filter);

        TFileOpenBox box = new TFileOpenBox(this, path, type, filters);
        return box.getFilename();
    }

    /**
     * Convenience function to spawn a file open box.
     *
     * @param path path of selected file
     * @param type one of the Type constants
     * @param filters a list of strings that files must match to be displayed
     * @return the result of the new file open box
     * @throws IOException of a java.io operation throws
     */
    public final String fileOpenBox(final String path,
        final TFileOpenBox.Type type,
        final List<String> filters) throws IOException {

        TFileOpenBox box = new TFileOpenBox(this, path, type, filters);
        return box.getFilename();
    }

    /**
     * Convenience function to spawn a file save box.
     *
     * @param path path of selected file
     * @return the result of the new file open box
     * @throws IOException if a java.io operation throws
     */
    public final String fileSaveBox(final String path) throws IOException {
        return fileOpenBox(path, TFileOpenBox.Type.SAVE);
    }

    /**
     * Convenience function to create a new window and make it active.
     * Window will be located at (0, 0).
     *
     * @param title window title, will be centered along the top border
     * @param width width of window
     * @param height height of window
     * @return the new window
     */
    public final TWindow addWindow(final String title, final int width,
        final int height) {

        TWindow window = new TWindow(this, title, 0, 0, width, height);
        return window;
    }

    /**
     * Convenience function to create a new window and make it active.
     * Window will be located at (0, 0).
     *
     * @param title window title, will be centered along the top border
     * @param width width of window
     * @param height height of window
     * @param flags bitmask of RESIZABLE, CENTERED, or MODAL
     * @return the new window
     */
    public final TWindow addWindow(final String title,
        final int width, final int height, final int flags) {

        TWindow window = new TWindow(this, title, 0, 0, width, height, flags);
        return window;
    }

    /**
     * Convenience function to create a new window and make it active.
     *
     * @param title window title, will be centered along the top border
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of window
     * @param height height of window
     * @return the new window
     */
    public final TWindow addWindow(final String title,
        final int x, final int y, final int width, final int height) {

        TWindow window = new TWindow(this, title, x, y, width, height);
        return window;
    }

    /**
     * Convenience function to create a new window and make it active.
     *
     * @param title window title, will be centered along the top border
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of window
     * @param height height of window
     * @param flags mask of RESIZABLE, CENTERED, or MODAL
     * @return the new window
     */
    public final TWindow addWindow(final String title,
        final int x, final int y, final int width, final int height,
        final int flags) {

        TWindow window = new TWindow(this, title, x, y, width, height, flags);
        return window;
    }

}
