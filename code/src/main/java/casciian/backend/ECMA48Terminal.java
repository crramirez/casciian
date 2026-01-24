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

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import casciian.backend.terminal.Terminal;
import casciian.backend.terminal.TerminalFactory;
import casciian.bits.Cell;
import casciian.bits.CellAttributes;
import casciian.bits.Color;
import casciian.bits.ComplexCell;
import casciian.bits.ImageRGB;
import casciian.bits.StringUtils;
import casciian.event.TCommandEvent;
import casciian.event.TInputEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import casciian.event.TResizeEvent;

import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * This class reads keystrokes and mouse events and emits output to ANSI
 * X3.64 / ECMA-48 type terminals e.g. xterm, linux, vt100, ansi.sys, etc.
 */
public class ECMA48Terminal extends LogicalScreen
    implements TerminalReader, Runnable {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * OSC sequence identifier for querying/setting the color palette.
     * Used in OSC 4 sequences to query or set individual palette colors (0-255).
     */
    public static final String OSC_PALETTE = "4";

    /**
     * OSC sequence identifier for querying/setting the default foreground color.
     * Used in OSC 10 sequences to query or set the terminal's default text color.
     */
    public static final String OSC_DEFAULT_FORECOLOR = "10";

    /**
     * OSC sequence identifier for querying/setting the default background color.
     * Used in OSC 11 sequences to query or set the terminal's default background color.
     */
    public static final String OSC_DEFAULT_BACKCOLOR = "11";

    /**
     * OSC sequence identifier for querying/setting the mouse pointer shape.
     * Used in OSC 22 sequences to query or set the terminal's mouse pointer.
     * When mouse mode is enabled in xterm, this is used to change the I-beam
     * cursor to an arrow pointer for better visual consistency.
     */
    public static final String OSC_POINTER_SHAPE = "22";

    /**
     * Arrow pointer shape name for xterm OSC 22 sequence.
     */
    private static final String POINTER_SHAPE_LEFT_PTR = "left_ptr";

    /**
     * Default xterm pointer shape to restore
     */
    private static final String POINTER_SHAPE_DEFAULT = "default";

    /**
     * States in the input parser.
     */
    private enum ParseState {
        GROUND,
        ESCAPE,
        ESCAPE_INTERMEDIATE,
        CSI_ENTRY,
        CSI_PARAM,
        XTVERSION,
        OSC,
        MOUSE,
        MOUSE_SGR,
    }

    /**
     * Available Casciian images support.
     */
    private enum JexerImageOption {
        DISABLED,
        RGB,
    }

    /**
     * Available text blink options.
     */
    private enum TextBlinkOption {
        OFF,
        HARD,
        SOFT,
        AUTO,
    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Emit debugging to stderr.
     */
    private boolean debugToStderr = false;

    /**
     * If true, emit T.416-style RGB colors for normal system colors.  This
     * is a) expensive in bandwidth, and b) potentially terrible looking for
     * non-xterms.
     */
    private boolean doRgbColor = false;

    /**
     * The backend that is reading from this terminal.
     */
    private Backend backend;

    /**
     * The session information.
     */
    private SessionInfo sessionInfo;

    /**
     * The event queue, filled up by a thread reading on input.
     */
    private List<TInputEvent> eventQueue;

    /**
     * If true, we want the reader thread to exit gracefully.
     */
    private volatile boolean stopReaderThread;

    /**
     * The reader thread.
     */
    private Thread readerThread;

    /**
     * Parameters being collected.  E.g. if the string is \033[1;3m, then
     * params[0] will be 1 and params[1] will be 3.
     */
    private List<String> params;

    /**
     * Current parsing state.
     */
    private ParseState state;

    /**
     * The time we entered ESCAPE.  If we get a bare escape without a code
     * following it, this is used to return that bare escape.
     */
    private long escapeTime;

    /**
     * The time we last checked the window size.  We try not to spawn stty
     * more than once per second.
     */
    private long windowSizeTime;

    /**
     * true if mouse1 was down.  Used to report mouse1 on the release event.
     */
    private boolean mouse1;

    /**
     * true if mouse2 was down.  Used to report mouse2 on the release event.
     */
    private boolean mouse2;

    /**
     * true if mouse3 was down.  Used to report mouse3 on the release event.
     */
    private boolean mouse3;

    /**
     * Cache the cursor visibility value so we only emit the sequence when we
     * need to.
     */
    private boolean cursorOn = true;

    /**
     * Cache the last window size to figure out if a TResizeEvent needs to be
     * generated.
     */
    private TResizeEvent windowResize = null;

    /**
     * Window width in pixels.  Used for image support.
     */
    private int widthPixels = 640;

    /**
     * Window height in pixels.  Used for image support.
     */
    private int heightPixels = 400;

    /**
     * Text cell width in pixels.
     */
    private int textWidthPixels = -1;

    /**
     * Text cell height in pixels.
     */
    private int textHeightPixels = -1;

    /**
     * Text blink option.
     */
    private TextBlinkOption textBlinkOption = TextBlinkOption.AUTO;

    /**
     * If true, blinking text should be visible right now based on the blink
     * time.
     */
    private boolean textBlinkVisible = true;

    /**
     * The percent of "dimming" to do when blinking text is invisible.  0
     * means no blinking; 100 means the foreground color matches the
     * background color.
     */
    private int blinkDimPercent = 100;

    /**
     * The number of millis to wait before switching the blink from visible
     * to invisible.  Set to 0 or negative to disable blinking.
     */
    private long blinkMillis = 500;

    /**
     * The time that the blink last flipped from visible to invisible or
     * from invisible to visible.
     */
    private long lastBlinkTime = 0;

    /**
     * If true, we are operating on a Genuine(tm) XTerm.
     */
    private boolean isGenuineXTerm = false;

    /**
     * If true, emit image data via sixel.
     */
    private boolean sixel = true;

    /**
     * The sixel encoder.
     */
    private SixelEncoder sixelEncoder = null;

    /**
     * If true, ask sixel to be fast and dirty.
     */
    private boolean sixelFastAndDirty = false;

    /**
     * If true, the terminal supports leaving the terminal cursor to the
     * right of sixel images (DECSET 8452).
     */
    private boolean sixelCursorOnRight = false;

    /**
     * The sixel post-rendered string cache.
     */
    private ImageCache sixelCache = null;

    /**
     * If not DISABLED, emit image data via Casciian image protocol if the
     * terminal supports it.
     */
    private JexerImageOption jexerImageOption = JexerImageOption.RGB;

    /**
     * The Casciian post-rendered string cache.
     */
    private ImageCache jexerCache = null;

    /**
     * The Unicode glyph post-rendered string cache.
     */
    private ImageCache unicodeGlyphCache = null;

    /**
     * The number of threads for image rendering.
     */
    private int imageThreadCount = 2;

    /**
     * If true, we changed the mouse pointer shape and need to restore it.
     */
    private boolean mousePointerShapeChanged = false;

    /**
     * If true, then we changed System.in and need to change it back.
     */
    private boolean setRawMode = false;

    /**
     * The terminal implementation used for setting raw/cooked mode.
     */
    private final Terminal terminal;

    /**
     * If true, the DA response has been seen and options that it affects
     * should not be reset in reloadOptions().
     */
    private boolean daResponseSeen = false;

    /**
     * If true, then we will set modifyOtherKeys.
     */
    private boolean modifyOtherKeys = false;

    /**
     * If true, '?' was seen in terminal response.
     */
    private boolean decPrivateModeFlag = false;

    /**
     * If true, '$' was seen in terminal response.
     */
    private boolean decDollarModeFlag = false;

    /**
     * If true, we are waiting on the XTVERSION response.  (Which might never
     * come if this terminal doesn't support it.  Blech.)
     */
    private boolean xtversionQuery = false;

    /**
     * The string being built by XTVERSION.
     */
    private StringBuilder xtversionResponse = new StringBuilder();

    /**
     * The string returned by XTVERSION.
     */
    private String terminalVersion = "";

    /**
     * The string being built by OSC.
     */
    private StringBuilder oscResponse = new StringBuilder();

    /**
     * If true, this terminal has the mouse/keyboard focus.  We default to
     * true because terminals that lack FOCUS_EVENT_MOUSE mode should act
     * normally.
     */
    private boolean hasFocus = true;

    /**
     * If true, this terminal supports Synchronized Output mode (2026).  See
     * https://gist.github.com/christianparpart/d8a62cc1ab659194337d73e399004036
     * for details of this mode.
     */
    private boolean hasSynchronizedOutput = false;

    /**
     * If true, this terminal requires explicitly overwriting images
     * with black pixels to destroy them.  If false, overwriting
     * images with text will destroy them.  Konsole is the only
     * terminal known at this time that requires
     * explicitlyDestroyImages to be true.
     */
    private boolean explicitlyDestroyImages = false;

    /**
     * The cells containing all-black pixels used to erase images when
     * explicityDestroyImages is true.  Only initialized if needed.
     */
    private ArrayList<Cell> blankImageRow = null;

    /**
     * The time we last flushed output in flushPhysical().
     */
    private long lastFlushTime;

    /**
     * The bytes being written in this second.
     */
    private int bytesPerSecond;

    /**
     * The bytes per second for the last second.
     */
    private int lastBytesPerSecond;

    /**
     * The terminal's input.  If an InputStream is not specified in the
     * constructor, then this InputStreamReader will be bound to System.in
     * with UTF-8 encoding.
     */
    private Reader input;

    /**
     * The terminal's output.  If an OutputStream is not specified in the
     * constructor, then this PrintWriter will be bound to System.out with
     * UTF-8 encoding.
     */
    private PrintWriter output;

    /**
     * The listening object that run() wakes up on new input.
     * This field is volatile to ensure visibility across threads when the
     * listener is changed via setListener() while the reader thread is running.
     */
    private volatile Object listener;

    // RGB colors matching the DOS/CGA colors.
    private static int MYBLACK;
    private static int MYRED;
    private static int MYGREEN;
    private static int MYYELLOW;
    private static int MYBLUE;
    private static int MYMAGENTA;
    private static int MYCYAN;
    private static int MYWHITE;
    private static int MYBOLD_BLACK;
    private static int MYBOLD_RED;
    private static int MYBOLD_GREEN;
    private static int MYBOLD_YELLOW;
    private static int MYBOLD_BLUE;
    private static int MYBOLD_MAGENTA;
    private static int MYBOLD_CYAN;
    private static int MYBOLD_WHITE;
    private static int DEFAULT_FORECOLOR;
    private static int DEFAULT_BACKCOLOR;

    private class ImageCache {

        /**
         * Maximum size of the cache.
         */
        private int maxSize = 100;

        /**
         * The entries stored in the cache.
         */
        private HashMap<String, CacheEntry> cache = null;

        /**
         * CacheEntry is one entry in the cache.
         */
        private class CacheEntry {
            /**
             * The cache key.
             */
            public String key;

            /**
             * The cache data.
             */
            public String data;

            /**
             * The last time this entry was used.
             */
            public long millis = 0;

            /**
             * Public constructor.
             *
             * @param key  the cache entry key
             * @param data the cache entry data
             */
            public CacheEntry(final String key, final String data) {
                this.key = key;
                this.data = data;
                this.millis = System.currentTimeMillis();
            }
        }

        /**
         * Public constructor.
         *
         * @param maxSize the maximum size of the cache
         */
        public ImageCache(final int maxSize) {
            this.maxSize = maxSize;
            cache = new HashMap<String, CacheEntry>();
        }

        /**
         * Make a unique key for a list of cells.
         *
         * @param cells the cells
         * @return the key
         */
        private String makeKey(final ArrayList<Cell> cells) {
            StringBuilder sb = new StringBuilder();
            for (Cell cell : cells) {
                sb.append(cell.hashCode());
            }
            // System.err.println("key: " + sb.toString());
            return sb.toString();
        }

        /**
         * Get an entry from the cache.
         *
         * @param cells the list of cells that are the cache key
         * @return the image string representing these cells, or null if this
         * list of cells is not in the cache
         */
        public synchronized String get(final ArrayList<Cell> cells) {
            CacheEntry entry = cache.get(makeKey(cells));
            if (entry == null) {
                return null;
            }
            entry.millis = System.currentTimeMillis();
            return entry.data;
        }

        /**
         * Put an entry into the cache.
         *
         * @param cells the list of cells that are the cache key
         * @param data  the image string representing these cells
         */
        public synchronized void put(final ArrayList<Cell> cells,
                                     final String data) {

            String key = makeKey(cells);

            // System.err.println("put() " + key + " size " + cache.size());

            assert (!cache.containsKey(key));

            assert (cache.size() <= maxSize);
            if (cache.size() == maxSize) {
                // Cache is at limit, evict oldest entry.
                long oldestTime = Long.MAX_VALUE;
                String keyToRemove = null;
                for (CacheEntry entry : cache.values()) {
                    if ((entry.millis < oldestTime) || (keyToRemove == null)) {
                        keyToRemove = entry.key;
                        oldestTime = entry.millis;
                    }
                }
                /*
                System.err.println("put() remove key = " + keyToRemove +
                    " size " + cache.size());
                 */
                assert (keyToRemove != null);
                cache.remove(keyToRemove);
                /*
                System.err.println("put() removed, size " + cache.size());
                 */
            }
            assert (cache.size() <= maxSize);
            CacheEntry entry = new CacheEntry(key, data);
            assert (key.equals(entry.key));
            cache.put(key, entry);
            /*
            System.err.println("put() added key " + key + " " +
                " size " + cache.size());
             */
        }

        /**
         * Get the number of entries in the cache.
         *
         * @return the number of entries
         */
        public synchronized int size() {
            return cache.size();
        }

    }

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Static constructor.
     */
    static {
        setCGAColors();
    }

    /**
     * Constructor sets up state for getEvent().  If either windowWidth or
     * windowHeight are less than 1, the terminal is not resized.
     *
     * @param backend      the backend that will read from this terminal
     * @param listener     the object this backend needs to wake up when new
     *                     input comes in
     * @param input        an InputStream connected to the remote user, or null for
     *                     System.in.  If System.in is used, then on non-Windows systems it will
     *                     be put in raw mode; closeTerminal() will (blindly!) put System.in in
     *                     cooked mode.  input is always converted to a Reader with UTF-8
     *                     encoding.
     * @param output       an OutputStream connected to the remote user, or null
     *                     for System.out.  output is always converted to a Writer with UTF-8
     *                     encoding.
     * @param windowWidth  the number of text columns to start with
     * @param windowHeight the number of text rows to start with
     * @throws UnsupportedEncodingException if an exception is thrown when
     *                                      creating the InputStreamReader
     */
    @SuppressWarnings("this-escape")
    public ECMA48Terminal(final Backend backend, final Object listener,
                          final InputStream input, final OutputStream output,
                          final int windowWidth,
                          final int windowHeight) throws UnsupportedEncodingException {

        this(backend, listener, input, output);

        // Send dtterm/xterm sequences, which will probably not work because
        // allowWindowOps is defaulted to false.
        if ((windowWidth > 0) && (windowHeight > 0)) {
            if (debugToStderr) {
                System.err.println("ECMA48Terminal() request screen size " +
                    getWidth() + " x " + getHeight());
            }

            String resizeString = String.format("\033[8;%d;%dt", windowHeight,
                windowWidth);
            this.output.write(resizeString);
            this.output.flush();
        }
    }

    /**
     * Constructor sets up state for getEvent().
     *
     * @param backend  the backend that will read from this terminal
     * @param listener the object this backend needs to wake up when new
     *                 input comes in
     * @param input    an InputStream connected to the remote user, or null for
     *                 System.in.  If System.in is used, then on non-Windows systems it will
     *                 be put in raw mode; closeTerminal() will (blindly!) put System.in in
     *                 cooked mode.  input is always converted to a Reader with UTF-8
     *                 encoding.
     * @param output   an OutputStream connected to the remote user, or null
     *                 for System.out.  output is always converted to a Writer with UTF-8
     *                 encoding.
     * @throws UnsupportedEncodingException if an exception is thrown when
     *                                      creating the InputStreamReader
     */
    @SuppressWarnings("this-escape")
    public ECMA48Terminal(final Backend backend, final Object listener,
                          final InputStream input,
                          final OutputStream output) throws UnsupportedEncodingException {

        this.backend = backend;

        resetParser();
        mouse1 = false;
        mouse2 = false;
        mouse3 = false;
        stopReaderThread = false;
        this.listener = listener;

        // Always create a terminal instance - it manages streams and features
        terminal = TerminalFactory.create(input, output, debugToStderr);

        this.input = terminal.getReader();

        // Set raw mode if using system input
        if (input == null) {
            sttyRaw();
            setRawMode = true;
        }

        if (input instanceof SessionInfo inputAsSessionInfo) {
            // This is a TelnetInputStream that exposes window size and
            // environment variables from the telnet layer.
            sessionInfo = inputAsSessionInfo;
        }

        if (sessionInfo == null) {
            if (input == null) {
                // Reading right off the tty
                sessionInfo = new TTYSessionInfo(terminal);
            } else {
                sessionInfo = new TSessionInfo();
            }
        }

        // Get output writer from terminal
        this.output = terminal.getWriter();

        // Request xterm version.  Due to the ambiguity between the response
        // and Alt-P, this must be the first thing to request.
        this.output.printf("%s", xtermReportVersion());

        // Request Device Attributes
        this.output.printf("\033[c");

        // Request xterm report window/cell dimensions in pixels
        this.output.printf("%s", xtermReportPixelDimensions());

        // Enable mouse reporting
        this.terminal.enableMouseReporting(true);

        // Enable metaSendsEscape
        this.output.printf("%s", xtermMetaSendsEscape(true));

        // Request xterm report Synchronized Output support
        this.output.printf("%s", xtermQueryMode(2026));

        // Send CGA palette to terminal (unless using terminal's native palette)
        if (!SystemProperties.isUseTerminalPalette()) {
            sendPalette();
        }

        // Request xterm report its ANSI colors
        this.output.printf("%s", xtermQueryAnsiColors());

        // Request xterm report sixelCursorOnRight support
        this.output.printf("%s", xtermQueryMode(8452));

        // Request xterm report its screen size
        this.output.printf("%s", xtermQueryWindowSize());

        this.output.flush();

        // Query the screen size locally
        sessionInfo.queryWindowSize();
        setDimensions(sessionInfo.getWindowWidth(),
            sessionInfo.getWindowHeight());

        // Hang onto the window size
        windowResize = new TResizeEvent(backend, TResizeEvent.Type.SCREEN,
            sessionInfo.getWindowWidth(), sessionInfo.getWindowHeight());

        reloadOptions();

        if (modifyOtherKeys) {
            // Request modifyOtherKeys
            this.output.printf("\033[>4;2m");
        }

        // Spin up the input reader
        eventQueue = new ArrayList<TInputEvent>();
        readerThread = new Thread(this);
        readerThread.start();

        // Clear the screen
        this.output.write(clearAll());
        this.output.flush();

    }

    /**
     * Constructor sets up state for getEvent().
     *
     * @param backend    the backend that will read from this terminal
     * @param listener   the object this backend needs to wake up when new
     *                   input comes in
     * @param input      the InputStream underlying 'reader'.  Its available()
     *                   method is used to determine if reader.read() will block or not.
     * @param reader     a Reader connected to the remote user.
     * @param writer     a PrintWriter connected to the remote user.
     * @param setRawMode if true, set System.in into raw mode with stty.
     *                   This should in general not be used.  It is here solely for Demo3,
     *                   which uses System.in.
     * @throws IllegalArgumentException if input, reader, or writer are null.
     */
    @SuppressWarnings("this-escape")
    public ECMA48Terminal(final Backend backend, final Object listener,
                          final InputStream input, final Reader reader, final PrintWriter writer,
                          final boolean setRawMode) {

        if (input == null) {
            throw new IllegalArgumentException("InputStream must be specified");
        }
        if (reader == null) {
            throw new IllegalArgumentException("Reader must be specified");
        }
        if (writer == null) {
            throw new IllegalArgumentException("Writer must be specified");
        }

        this.backend = backend;

        resetParser();

        mouse1 = false;
        mouse2 = false;
        mouse3 = false;
        stopReaderThread = false;
        this.listener = listener;

        // Create a terminal instance with the pre-wired streams
        // This allows future delegation of terminal features
        terminal = TerminalFactory.create(input, reader, writer, debugToStderr);

        this.input = reader;

        if (setRawMode == true) {
            sttyRaw();
        }
        this.setRawMode = setRawMode;

        if (input instanceof SessionInfo) {
            // This is a TelnetInputStream that exposes window size and
            // environment variables from the telnet layer.
            sessionInfo = (SessionInfo) input;
        }
        if (sessionInfo == null) {
            if (setRawMode == true) {
                // Reading right off the tty
                sessionInfo = new TTYSessionInfo(terminal);
            } else {
                sessionInfo = new TSessionInfo();
            }
        }

        this.output = writer;

        // Request xterm version.  Due to the ambiguity between the response
        // and Alt-P, this must be the first thing to request.
        this.output.printf("%s", xtermReportVersion());

        // Request Device Attributes
        this.output.printf("\033[c");

        // Request xterm report window/cell dimensions in pixels
        this.output.printf("%s", xtermReportPixelDimensions());

        // Enable mouse reporting
        this.terminal.enableMouseReporting(true);

        // Enable metaSendsEscape
        this.output.printf("%s", xtermMetaSendsEscape(true));

        // Request xterm report Synchronized Output support
        this.output.printf("%s", xtermQueryMode(2026));

        // Send CGA palette to terminal (unless using terminal's native palette)
        if (!SystemProperties.isUseTerminalPalette()) {
            sendPalette();
        }

        // Request xterm report its ANSI colors
        this.output.printf("%s", xtermQueryAnsiColors());

        // Request xterm report its screen size
        this.output.printf("%s", xtermQueryWindowSize());

        this.output.flush();

        // Query the screen size locally
        sessionInfo.queryWindowSize();
        setDimensions(sessionInfo.getWindowWidth(),
            sessionInfo.getWindowHeight());

        // Hang onto the window size
        windowResize = new TResizeEvent(backend, TResizeEvent.Type.SCREEN,
            sessionInfo.getWindowWidth(), sessionInfo.getWindowHeight());

        reloadOptions();

        if (modifyOtherKeys) {
            // Request modifyOtherKeys
            this.output.printf("\033[>4;2m");
        }

        // Spin up the input reader
        eventQueue = new ArrayList<TInputEvent>();
        readerThread = new Thread(this);
        readerThread.start();

        // Clear the screen
        this.output.write(clearAll());
        this.output.flush();
    }

    /**
     * Constructor sets up state for getEvent().
     *
     * @param backend  the backend that will read from this terminal
     * @param listener the object this backend needs to wake up when new
     *                 input comes in
     * @param input    the InputStream underlying 'reader'.  Its available()
     *                 method is used to determine if reader.read() will block or not.
     * @param reader   a Reader connected to the remote user.
     * @param writer   a PrintWriter connected to the remote user.
     * @throws IllegalArgumentException if input, reader, or writer are null.
     */
    public ECMA48Terminal(final Backend backend, final Object listener,
                          final InputStream input, final Reader reader,
                          final PrintWriter writer) {

        this(backend, listener, input, reader, writer, false);
    }

    // ------------------------------------------------------------------------
    // LogicalScreen ----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set the window title.
     *
     * @param title the new title
     */
    @Override
    public void setTitle(final String title) {
        if (output != null) {
            output.write(getSetTitleString(title));
            flush();
        }
    }

    /**
     * Push the logical screen to the physical device.
     */
    @Override
    public void flushPhysical() {
        StringBuilder sb = new StringBuilder();
        if ((cursorVisible)
            && (cursorY >= 0)
            && (cursorX >= 0)
            && (cursorY <= height - 1)
            && (cursorX <= width - 1)
        ) {
            flushString(sb);
            sb.append(cursor(true));
            sb.append(gotoXY(cursorX, cursorY));
        } else {
            sb.append(cursor(false));
            flushString(sb);
        }

        // See if it is time to flip the blink time.
        long nowTime = System.currentTimeMillis();
        if ((blinkMillis > 0)
            && (nowTime >= blinkMillis + lastBlinkTime)
        ) {
            lastBlinkTime = nowTime;
            textBlinkVisible = !textBlinkVisible;
        } else if (blinkMillis <= 0) {
            textBlinkVisible = true;
        }

        if (output != null) {
            if (hasSynchronizedOutput) {
                if (sb.length() > 0) {
                    // Begin Synchronized Update (BSU)
                    output.write("\033[?2026h");
                    if (debugToStderr) {
                        System.err.printf("Writing %d bytes to terminal (sync)\n",
                            sb.length());
                    }
                    output.write(sb.toString());
                    // End Synchronized Update (ESU)
                    output.write("\033[?2026l");
                }
                if (debugToStderr) {
                    System.err.printf("flushPhysical() \033[?2026h%s\033[?2026l\n",
                        sb.toString());
                }
            } else {
                if (sb.length() > 0) {
                    if (debugToStderr) {
                        System.err.printf("Writing %d bytes to terminal\n",
                            sb.length());
                    }
                    output.write(sb.toString());
                }
            }
            output.flush();

            long now = System.currentTimeMillis();
            if ((int) (now / 1000) == (int) (lastFlushTime / 1000)) {
                bytesPerSecond += sb.length();
            } else {
                lastBytesPerSecond = sb.length();
                bytesPerSecond = 0;
            }
            lastFlushTime = now;
        }
    }

    /**
     * Resize the physical screen to match the logical screen dimensions.
     */
    @Override
    public void resizeToScreen() {
        if (backend.isReadOnly()) {
            return;
        }
        if (!daResponseSeen) {
            if (debugToStderr) {
                System.err.println("resizeToScreen() -- ABORT no DA seen --");
            }
            // Do not resize immediately until we have seen device
            // attributes.
            return;
        }

        if (debugToStderr) {
            System.err.println("resizeToScreen() " + getWidth() + " x " +
                getHeight());
        }

        // Send dtterm/xterm sequences, which will probably not work because
        // allowWindowOps is defaulted to false.
        String resizeString = String.format("\033[8;%d;%dt", getHeight(),
            getWidth());
        if (output != null) {
            this.output.write(resizeString);
            this.output.flush();
        }
    }

    // ------------------------------------------------------------------------
    // TerminalReader ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Check if there are events in the queue.
     *
     * @return if true, getEvents() has something to return to the backend
     */
    public boolean hasEvents() {
        synchronized (eventQueue) {
            return (eventQueue.size() > 0);
        }
    }

    /**
     * Return any events in the IO queue.
     *
     * @param queue list to append new events to
     */
    public void getEvents(final List<TInputEvent> queue) {
        synchronized (eventQueue) {
            if (eventQueue.size() > 0) {
                synchronized (queue) {
                    queue.addAll(eventQueue);
                }
                eventQueue.clear();
            }
        }
    }

    /**
     * Restore terminal to normal state.
     */
    public void closeTerminal() {

        // System.err.println("=== closeTerminal() ==="); System.err.flush();

        // Tell the reader thread to stop looking at input
        stopReaderThread = true;
        try {
            readerThread.join();
        } catch (InterruptedException e) {
            if (debugToStderr) {
                e.printStackTrace();
            }
        }

        // Restore original xterm mouse pointer shape if it was changed
        restoreXtermMousePointer();

        restorePalette();

        // Disable mouse reporting and show cursor.  Defensive null check
        // here in case closeTerminal() is called twice.
        if (output != null) {
            this.terminal.enableMouseReporting(false);
            output.printf("%s%s", cursor(true), defaultColor());
            output.printf("\033[>4m");
            output.flush();
        }

        if (setRawMode) {
            sttyCooked();
            setRawMode = false;
            // We don't close System.in/out
        } else {
            // Shut down the streams, this should wake up the reader thread
            // and make it exit.
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    // SQUASH
                }
                input = null;
            }
            if (output != null) {
                output.close();
                output = null;
            }
        }

        closeTerminalImpl();
    }

    /**
     * Restore the terminal's palette to its original state.
     */
    private void restorePalette() {
        if (output == null) {
            return;
        }

        if (SystemProperties.isUseTerminalPalette()) {
            return;
        }

        output.print("\033]104\033\\");
        output.flush();
    }

    /**
     * Set listener to a different Object.
     *
     * @param listener the new listening object that run() wakes up on new
     *                 input
     */
    public void setListener(final Object listener) {
        this.listener = listener;
    }

    /**
     * Reload options from System properties.
     */
    public void reloadOptions() {
        if (debugToStderr) {
            System.err.println("reloadOptions()");
        }

        // Permit RGB colors only if externally requested.
        if (System.getProperty("casciian.ECMA48.modifyOtherKeys",
            "false").equals("true")) {

            modifyOtherKeys = true;
        } else {
            modifyOtherKeys = false;
        }

        // Permit RGB colors only if externally requested.
        if (System.getProperty("casciian.ECMA48.rgbColor",
            "false").equals("true")
        ) {
            doRgbColor = true;
        } else {
            doRgbColor = false;
        }

        // Default to sixel enabled.
        sixel = !System.getProperty("casciian.ECMA48.sixel", "true").equals("false");
        // Default to HQ quantizer.
        sixelEncoder = new HQSixelEncoder();

        if (System.getProperty("casciian.ECMA48.sixelFastAndDirty",
            "false").equals("true")
        ) {
            sixelFastAndDirty = true;
        } else {
            sixelFastAndDirty = false;
        }
        sixelEncoder.reloadOptions();

        // Request xterm use the sixel settings we want
        this.output.printf("%s", xtermSetSixelSettings());

        if (!daResponseSeen) {
            // Default to using JPG Casciian images if terminal supports it.
            String jexerImageStr = System.getProperty("casciian.ECMA48.jexerImages",
                "rgb").toLowerCase();
            if (jexerImageStr.equals("false")) {
                jexerImageOption = JexerImageOption.DISABLED;
            } else if (jexerImageStr.equals("rgb")) {
                jexerImageOption = JexerImageOption.RGB;
            }
        }

        String destroyImagesStr = System.getProperty("casciian.ECMA48.explicitlyDestroyImages",
            "auto").toLowerCase();
        explicitlyDestroyImages = destroyImagesStr.equals("true");

        // Image thread count.
        imageThreadCount = 2;
        try {
            imageThreadCount = Integer.parseInt(System.getProperty(
                "casciian.ECMA48.imageThreadCount", "2"));
            if (imageThreadCount < 1) {
                imageThreadCount = 1;
            }
        } catch (NumberFormatException e) {
            // SQUASH
        }

        // For text blinking, "auto" acts like "hard" except for genuine
        // XTerm where it acts like "soft".
        String textBlinkStr = System.getProperty("casciian.ECMA48.textBlink",
            "auto").toLowerCase();
        if (textBlinkStr.equals("off")) {
            textBlinkOption = TextBlinkOption.OFF;
        } else if (textBlinkStr.equals("hard")) {
            textBlinkOption = TextBlinkOption.HARD;
        } else if (textBlinkStr.equals("soft")) {
            textBlinkOption = TextBlinkOption.SOFT;
        }
        // casciian.textBlink overrides all
        if (!SystemProperties.isTextBlink()) {
            textBlinkOption = TextBlinkOption.OFF;
        }

        long millis = SystemProperties.getBlinkMillis();
        if (millis <= 0) {
            textBlinkOption = TextBlinkOption.OFF;
        } else {
            blinkMillis = millis;
        }

        int dimPercent = SystemProperties.getBlinkDimPercent();
        if ((dimPercent >= 0) && (dimPercent <= 100)) {
            blinkDimPercent = dimPercent;
            if (blinkDimPercent == 0) {
                textBlinkOption = TextBlinkOption.OFF;
            }
        }

        // Set custom colors
        setCustomSystemColors();
    }

    // ------------------------------------------------------------------------
    // Runnable ---------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Read function runs on a separate thread.
     */
    public void run() {
        boolean done = false;
        // available() will often return > 1, so we need to read in chunks to
        // stay caught up.
        char[] readBuffer = new char[128];
        List<TInputEvent> events = new ArrayList<TInputEvent>();

        //boolean debugToStderr = true;

        while (!done && !stopReaderThread) {
            try {
                // We assume that if inputStream has bytes available, then
                // input won't block on read().
                if (debugToStderr) {
                    System.err.printf("Looking for input...");
                }

                int n = terminal.available();

                if (debugToStderr) {
                    if (n == 0) {
                        System.err.println("none.");
                    }
                    if (n < 0) {
                        System.err.printf("WHAT?!  n = %d\n", n);
                    }
                }

                if (n > 0) {
                    if (debugToStderr) {
                        System.err.printf("%d bytes to read.\n", n);
                    }

                    if (readBuffer.length < n) {
                        // The buffer wasn't big enough, make it huger
                        readBuffer = new char[readBuffer.length * 2];
                    }

                    if (debugToStderr) {
                        System.err.printf("B4 read(): readBuffer.length = %d\n",
                            readBuffer.length);
                    }

                    int rc = terminal.read(readBuffer, 0, readBuffer.length);

                    /*
                    System.err.printf("AFTER read() %d\n", rc);
                    System.err.flush();
                    */

                    if (rc == -1) {
                        if (debugToStderr) {
                            System.err.println(" ---- EOF ----");
                        }

                        // This is EOF
                        done = true;
                    } else {
                        if (debugToStderr) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < rc; i++) {
                                sb.append(readBuffer[i]);
                            }
                            System.err.printf("%d rc = %d INPUT: ",
                                System.currentTimeMillis(), rc);
                            System.err.println(sb.toString());
                        }
                        for (int i = 0; i < rc; i++) {
                            int ch = readBuffer[i];
                            processChar(events, (char) ch);
                        }
                        getIdleEvents(events);
                        if (events.size() > 0) {
                            // Add to the queue for the backend thread to
                            // be able to obtain.
                            if (debugToStderr) {
                                System.err.printf("Checking eventQueue...");
                            }

                            synchronized (eventQueue) {
                                eventQueue.addAll(events);
                            }
                            if (debugToStderr) {
                                System.err.printf("done.\n");
                            }

                            if (listener != null) {
                                if (debugToStderr) {
                                    System.err.printf("Waking up listener...");
                                }

                                synchronized (listener) {
                                    listener.notifyAll();
                                }
                                if (debugToStderr) {
                                    System.err.printf("done.\n");
                                }

                            }
                            events.clear();
                        }
                    }
                } else {
                    if (debugToStderr) {
                        System.err.println("Looking for idle events");
                    }
                    getIdleEvents(events);
                    if (events.size() > 0) {
                        if (debugToStderr) {
                            System.err.printf("Checking eventQueue...");
                        }

                        synchronized (eventQueue) {
                            eventQueue.addAll(events);
                        }
                        if (debugToStderr) {
                            System.err.printf("done.\n");
                        }

                        if (listener != null) {
                            if (debugToStderr) {
                                System.err.printf("Waking up listener...");
                            }

                            synchronized (listener) {
                                listener.notifyAll();
                            }
                            if (debugToStderr) {
                                System.err.printf("done.\n");
                            }

                        }
                        events.clear();
                    }

                    if (output != null) {
                        if (output.checkError()) {
                            // This is EOF.
                            done = true;
                        }
                    }

                    // Wait 20 millis for more data
                    Thread.sleep(20);
                }
                // System.err.println("end while loop"); System.err.flush();
            } catch (InterruptedException e) {
                // SQUASH
            } catch (IOException e) {
                e.printStackTrace();
                done = true;
            }
        } // while ((done == false) && (stopReaderThread == false))

        // Pass an event up to TApplication to tell it this Backend is done.
        synchronized (eventQueue) {
            eventQueue.add(new TCommandEvent(backend, cmBackendDisconnect));
        }
        if (listener != null) {
            synchronized (listener) {
                listener.notifyAll();
            }
        }

        // System.err.println("*** run() exiting..."); System.err.flush();
    }

    // ------------------------------------------------------------------------
    // ECMA48Terminal ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the terminal version, as returned by XTVERSION.
     *
     * @return the XTVERSION string, or empty string
     */
    public String getTerminalVersion() {
        return terminalVersion;
    }

    /**
     * Get the number of millis to wait before switching the blink from
     * visible to invisible.
     *
     * @return the number of milliseconds to wait before switching the blink
     * from visible to invisible
     */
    public long getBlinkMillis() {
        return blinkMillis;
    }

    /**
     * Set the number of millis to wait before switching the blink from
     * visible to invisible.
     *
     * @param millis the number of milliseconds to wait before switching the
     *               blink from visible to invisible
     */
    public void setBlinkMillis(final long millis) {
        blinkMillis = millis;
    }

    /**
     * Get the bytes per second from the last second.
     *
     * @return the bytes per second
     */
    public int getBytesPerSecond() {
        return lastBytesPerSecond;
    }

    /**
     * Get the width of a character cell in pixels.
     *
     * @return the width in pixels of a character cell
     */
    @Override
    public int getTextWidth() {
        if (textWidthPixels > 0) {
            return textWidthPixels;
        }
        int windowWidth = sessionInfo.getWindowWidth();
        if (windowWidth > 0) {
            return widthPixels / windowWidth;
        }
        return 10;
    }

    /**
     * Get the height of a character cell in pixels.
     *
     * @return the height in pixels of a character cell
     */
    @Override
    public int getTextHeight() {
        if (textHeightPixels > 0) {
            return textHeightPixels;
        }
        int windowHeight = sessionInfo.getWindowHeight();
        if (windowHeight > 0) {
            return heightPixels / windowHeight;
        }
        return 20;
    }

    /**
     * Getter for sessionInfo.
     *
     * @return the SessionInfo
     */
    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    /**
     * Get the output writer.
     *
     * @return the Writer
     */
    public PrintWriter getOutput() {
        return output;
    }

    /**
     * Get the current status of the text blink flag.
     *
     * @return true if blinking text should be visible
     */
    public boolean getTextBlinkVisible() {
        if (textBlinkOption == TextBlinkOption.OFF) {
            return true;
        }
        return textBlinkVisible;
    }

    /**
     * Set terminal to raw mode.
     *
     * <p>Configures the terminal for character-by-character input without echo.
     */
    private void sttyRaw() {
        if (terminal != null) {
            terminal.setRawMode();
        }
    }

    /**
     * Set terminal to cooked (normal) mode.
     */
    private void sttyCooked() {
        if (terminal != null) {
            terminal.setCookedMode();
        }
    }

    /**
     * Close the terminal if it was opened.
     */
    private void closeTerminalImpl() {
        if (terminal != null) {
            terminal.close();
        }
    }

    /**
     * Flush output.
     */
    public void flush() {
        if (output != null) {
            output.flush();
        }
    }

    /**
     * Perform a somewhat-optimal rendering of a line.
     *
     * @param y        row coordinate.  0 is the top-most row.
     * @param sb       StringBuilder to write escape sequences to
     * @param lastAttr cell attributes from the last call to flushLine
     */
    private void flushLine(final int y, final StringBuilder sb,
                           CellAttributes lastAttr) {

        int lastX = -1;
        int textEnd = 0;
        for (int x = 0; x < width; x++) {
            Cell lCell = logical[x][y];
            if (!lCell.isBlank()
                || lCell.isDefaultColor(true)
                || lCell.isDefaultColor(false)
            ) {
                textEnd = x;
            }
        }
        // Push textEnd to first column beyond the text area
        textEnd++;

        // DEBUG
        // reallyCleared = true;

        final boolean reallyDebug = false;

        for (int x = 0; x < width; x++) {
            ComplexCell lCell = logical[x][y];
            ComplexCell pCell = physical[x][y];

            if (lCell.isImage()) {
                continue;
            }

            if (lCell.isBlink()) {
                switch (textBlinkOption) {
                    case OFF:
                        lCell = new ComplexCell(logical[x][y]);
                        lCell.setBlink(false);
                        break;

                    case SOFT:
                        lCell = new ComplexCell(logical[x][y]);
                        lCell.setBlink(false);
                        if (!textBlinkVisible) {
                            lCell.setDimmedForeColor(backend, blinkDimPercent);
                        }
                        break;

                    case AUTO:
                        // Fall through...
                    case HARD:
                        break;
                }
            }

            if (!lCell.equals(pCell) || lCell.isPulse() || reallyCleared) {

                if (debugToStderr && reallyDebug) {
                    System.err.printf("\n--\n");
                    System.err.printf(" Y: %d X: %d lastX %d textEnd %d\n",
                        y, x, lastX, textEnd);
                    System.err.printf("   lCell: %s\n", lCell);
                    System.err.printf("   pCell: %s\n", pCell);
                    System.err.printf("   lastAttr: %s\n", lastAttr);
                    System.err.printf("    ====    \n");
                }

                if (lastAttr == null) {
                    lastAttr = new CellAttributes();
                    sb.append(normal());
                }

                // Place the cell
                if ((lastX != (x - 1)) || (lastX == -1)) {
                    if (!lCell.isImage()) {
                        if (debugToStderr && reallyDebug) {
                            System.err.println("1 gotoXY() " + x + " " + y +
                                " lastX " + lastX);
                        }
                        // Advancing at least one cell, or the first gotoXY
                        sb.append(gotoXY(x, y));
                    }
                }

                assert (lastAttr != null);

                if ((x == textEnd) && (textEnd < width - 1)) {
                    assert (lCell.isBlank());

                    for (int i = x; i < width; i++) {
                        assert (logical[i][y].isBlank());
                        // Physical is always updated
                        physical[i][y].reset();
                    }

                    // Clear remaining line
                    if (debugToStderr && reallyDebug) {
                        System.err.println("2 gotoXY() " + x + " " + y +
                            " lastX " + lastX);
                        System.err.println("X: " + x + " clearRemainingLine()");
                    }
                    sb.append(gotoXY(x, y));
                    sb.append(clearRemainingLine());
                    lastAttr.reset();
                    return;
                }

                // Image cell: bypass the rest of the loop, it is not
                // rendered here.
                if (lCell.isImage()) {
                    // Save the last rendered cell
                    lastX = x;

                    // Physical is always updated
                    physical[x][y].setTo(lCell);
                    continue;
                }

                assert (!lCell.isImage());

                if (debugToStderr && reallyDebug) {
                    System.err.println("3 gotoXY() " + x + " " + y +
                        " lastX " + lastX);
                }
                if (lastX != (x - 1)) {
                    sb.append(gotoXY(x, y));
                }

                // Now emit only the modified attributes
                // Note: We do NOT emit SGR 1 for bold because casciian uses
                // bright colors (90-97) to indicate bold instead of the SGR 1
                // attribute. This avoids showing bold/thick text on terminals
                // that support it.
                StringBuilder attrSgr = new StringBuilder(8);
                if (lCell.isUnderline() != lastAttr.isUnderline()) {
                    if (lCell.isUnderline()) {
                        attrSgr.append(";4");
                    } else {
                        attrSgr.append(";24");
                    }
                }
                if (lCell.isBlink() != lastAttr.isBlink()) {
                    if (lCell.isBlink()) {
                        attrSgr.append(";5");
                    } else {
                        attrSgr.append(";25");
                    }
                }
                if (lCell.isReverse() != lastAttr.isReverse()) {
                    if (lCell.isReverse()) {
                        attrSgr.append(";7");
                    } else {
                        attrSgr.append(";27");
                    }
                }
                if (attrSgr.length() > 0) {
                    if (debugToStderr && reallyDebug) {
                        System.err.println("2 attr: " + attrSgr.substring(1));
                    }
                    sb.append("\033[");
                    sb.append(attrSgr.substring(1));
                    sb.append("m");
                }

                boolean doForeColorRGB = false;
                int foreColorRGB = lCell.getForeColorRGB();
                long now = System.currentTimeMillis();
                if (lCell.isPulse()) {
                    foreColorRGB = lCell.getForeColorPulseRGB(backend, now);
                    int lastForeColorRGB = lastAttr.getForeColorRGB();
                    if (lastAttr.isPulse()) {
                        lastForeColorRGB = lastAttr.getForeColorRGB();
                    }
                    if ((foreColorRGB != lastForeColorRGB)
                        && !lCell.isDefaultColor(true)
                    ) {
                        doForeColorRGB = true;
                    }
                }
                if (doForeColorRGB
                    || ((lCell.getForeColorRGB() >= 0)
                    && !lCell.isDefaultColor(true)
                    && ((lCell.getForeColorRGB() != lastAttr.getForeColorRGB())
                    || (lastAttr.getForeColorRGB() < 0)))
                ) {
                    if (debugToStderr && reallyDebug) {
                        System.err.println("3a set foreColorRGB");
                    }
                    sb.append(colorRGB(foreColorRGB, true));
                } else if (lCell.isDefaultColor(true)) {
                    if (!lastAttr.isDefaultColor(true)) {
                        if (debugToStderr && reallyDebug) {
                            System.err.println("3b set DEFAULT foreColor");
                        }
                        sb.append("\033[39m");
                    }
                } else {
                    if ((lCell.getForeColorRGB() < 0)
                        && ((lastAttr.getForeColorRGB() >= 0)
                        || !lCell.getForeColor().equals(lastAttr.getForeColor())
                        || lastAttr.isDefaultColor(true)
                        || lCell.isBold() != lastAttr.isBold())
                    ) {
                        if (debugToStderr && reallyDebug) {
                            System.err.println("4 set foreColor");
                        }
                        sb.append(color(lCell.getForeColor(), true, true,
                            lCell.isBold()));
                    }
                }

                if ((lCell.getBackColorRGB() >= 0)
                    && !lCell.isDefaultColor(false)
                    && ((lCell.getBackColorRGB() != lastAttr.getBackColorRGB())
                    || (lastAttr.getBackColorRGB() < 0))
                ) {
                    //noinspection ConstantValue
                    if (debugToStderr && reallyDebug) {
                        System.err.println("5 set backColorRGB");
                    }
                    sb.append(colorRGB(lCell.getBackColorRGB(), false));
                } else if (lCell.isDefaultColor(false)) {
                    if (!lastAttr.isDefaultColor(false)) {
                        //noinspection ConstantValue
                        if (debugToStderr && reallyDebug) {
                            System.err.println("5b set DEFAULT backColor");
                        }
                        sb.append("\033[49m");
                    }
                } else {
                    if ((lCell.getBackColorRGB() < 0)
                        && ((lastAttr.getBackColorRGB() >= 0)
                        || !lCell.getBackColor().equals(lastAttr.getBackColor())
                        || lastAttr.isDefaultColor(false))
                    ) {
                        if (debugToStderr && reallyDebug) {
                            System.err.println("6 set backColor");
                        }
                        sb.append(color(lCell.getBackColor(), false, true));
                    }
                }

                // Emit the character
                if (lCell.getWidth() != Cell.Width.RIGHT) {
                    // Don't emit the right-half of full-width chars.
                    sb.append(lCell.toCharArray());
                }

                // Save the last rendered cell
                lastX = x;
                lastAttr.setTo(lCell);

                // Text cell: update, done.
                physical[x][y].setTo(lCell);

            } // if (!lCell.equals(pCell) || (reallyCleared == true))

        } // for (int x = 0; x < width; x++)
    }

    /**
     * Render the screen to a string that can be emitted to something that
     * knows how to process ECMA-48/ANSI X3.64 escape sequences.
     *
     * @param sb StringBuilder to write escape sequences to
     * @return escape sequences string that provides the updates to the
     * physical screen
     */
    private String flushString(final StringBuilder sb) {
        final boolean reallyDebug = false;

        CellAttributes attr = null;

        if (reallyCleared) {
            attr = new CellAttributes();
            sb.append(clearAll());
        }

        /*
         * For images support, draw all of the image output first, and
         * then draw all the text afterwards.
         *
         * We are deliberately using a font that is four pixels smaller than
         * the cell height.
         */
        int glyphFontSize = Math.max(4, getTextHeight() - 4);
        for (int y = 0; y < height; y++) {
            boolean unsetRow = false;
            boolean eraseImagesOnRow = false;
            for (int x = 0; x < width; x++) {
                ComplexCell lCell = logical[x][y];
                ComplexCell pCell = physical[x][y];

                // If physical has image data that will be overwritten by
                // text, then erase all of the images on this row for
                // terminals that require explicitlyDestroyImages to be true.
                if (pCell.isImage() && !lCell.isImage()) {
                    eraseImagesOnRow = true;
                }

                // If physical had non-image data that is now image data, the
                // entire row must be redrawn.
                if (lCell.isImage() && !pCell.isImage()) {
                    unsetRow = true;
                }
            }

            if (unsetRow) {
                unsetImageRow(y);
            }

            if (explicitlyDestroyImages && eraseImagesOnRow) {
                for (int x = 0; x < width; x++) {
                    physical[x][y].unset();
                }
                if ((blankImageRow == null)
                    || (blankImageRow.size() < width)
                ) {
                    blankImageRow = new ArrayList<Cell>(width);
                    Cell blank = new Cell();
                    ImageRGB newImage = new ImageRGB(textWidthPixels,
                        textHeightPixels);
                    newImage.fillRect(0, 0, newImage.getWidth(),
                        newImage.getHeight(), 0x000000);
                    blank.setImage(newImage);
                    for (int x = 0; x < width; x++) {
                        blankImageRow.add(new Cell(blank));
                    }
                }
                if (jexerImageOption != JexerImageOption.DISABLED) {
                    sb.append(toJexerImage(0, y, blankImageRow));
                } else {
                    sb.append(toSixel(0, y, blankImageRow));
                }
            }

        } //for (int y = 0; y < height; y++) {

        /*
         * Image encoding is expensive, especially when the image is not in
         * cache.  We multithread it.  Since each image contains its own
         * gotoxy(), it doesn't matter in what order they are delivered to
         * the terminal.
         */
        ExecutorService imageExecutor = null;
        List<Future<String>> imageResults = null;

        if (imageThreadCount > 1) {
            imageExecutor = Executors.newFixedThreadPool(imageThreadCount);
            imageResults = new ArrayList<Future<String>>();
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                ComplexCell lCell = logical[x][y];
                ComplexCell pCell = physical[x][y];

                if (!lCell.isImage()) {
                    continue;
                }

                int right = x;
                // This little loop is a *HUGE* bottleneck for image cells
                // when imageId is not set.  Higher layers of code should
                // always aim to set imageId before putting it on the screen.
                while ((right < width)
                    && (logical[right][y].isImage())
                    && (!logical[right][y].equals(physical[right][y])
                    || reallyCleared)
                ) {
                    right++;
                }

                ArrayList<Cell> cellsToDraw = new ArrayList<Cell>();
                for (int i = 0; i < (right - x); i++) {
                    cellsToDraw.add(logical[x + i][y]);

                    // Physical is always updated.
                    physical[x + i][y].setTo(logical[x + i][y]);
                }
                if (!cellsToDraw.isEmpty()) {
                    if (jexerImageOption != JexerImageOption.DISABLED) {
                        if (jexerCache == null) {
                            jexerCache = new ImageCache(height * width * 10);
                        }
                    } else if (sixel) {
                        if (sixelCache == null) {
                            sixelCache = new ImageCache(height * width * 10);
                        }
                    } else {
                        if (unicodeGlyphCache == null) {
                            unicodeGlyphCache = new ImageCache(height * width);
                        }
                    }

                    if (imageThreadCount == 1) {
                        // Single-threaded
                        if (jexerImageOption != JexerImageOption.DISABLED) {
                            sb.append(toJexerImage(x, y, cellsToDraw));
                        } else if (sixel) {
                            sb.append(toSixel(x, y, cellsToDraw));
                        }
                    } else {
                        // Multi-threaded: experimental and likely borken
                        final int callX = x;
                        final int callY = y;

                        // Make a deep copy of the cells to render.
                        final ArrayList<Cell> callCells;
                        callCells = new ArrayList<Cell>(cellsToDraw);
                        imageResults.add(imageExecutor.submit(new Callable<String>() {
                            @Override
                            public String call() {
                                if (jexerImageOption != JexerImageOption.DISABLED) {
                                    return toJexerImage(callX, callY, callCells);
                                } else {
                                    return toSixel(callX, callY, callCells);
                                }
                            }
                        }));
                    }
                }

                x = right;
            }
        }

        if (imageThreadCount > 1) {
            List<String> threadedImages = new ArrayList<String>(imageResults.size());
            // Collect all the encoded images.
            while (imageResults.size() > 0) {
                Future<String> image = imageResults.get(0);
                try {
                    threadedImages.add(image.get());
                } catch (InterruptedException e) {
                    // SQUASH
                    // e.printStackTrace();
                } catch (ExecutionException e) {
                    // SQUASH
                    // e.printStackTrace();
                }
                imageResults.remove(0);
            }
            imageExecutor.shutdown();

            Collections.sort(threadedImages);
            for (String imageString : threadedImages) {
                sb.append(imageString);
            }
        }

        // Draw the text part now.
        for (int y = 0; y < height; y++) {
            flushLine(y, sb, attr);
        }

        reallyCleared = false;

        String result = sb.toString();
        if (debugToStderr && !hasSynchronizedOutput) {
            System.err.printf("flushString(): %s\n", result);
        }
        return result;
    }

    /**
     * Get window/terminal system focus.
     *
     * @return true if this terminal has the mouse/keyboard focus
     */
    public boolean isFocused() {
        return hasFocus;
    }

    /**
     * Set the mouse pointer (cursor) style.
     *
     * @param mouseStyle the pointer style string, one of: "default", "none",
     *                   "hand", "text", "move", or "crosshair"
     */
    public void setMouseStyle(final String mouseStyle) {
        // TODO: For now disregard this.  OSC 22 came out with XTerm 367
        // which can select X11 cursors/pointers, but mintty implemented it
        // against Win32 cursors/pointers.  And neither bothered to implement
        // "really, just hide the damn pointer but still give me events" grr.
    }

    /**
     * Reset keyboard/mouse input parser.
     */
    private void resetParser() {
        state = ParseState.GROUND;
        params = new ArrayList<String>();
        params.clear();
        params.add("");
        decPrivateModeFlag = false;
        decDollarModeFlag = false;
        xtversionResponse.setLength(0);
        oscResponse.setLength(0);
    }

    /**
     * Produce a control character or one of the special ones (ENTER, TAB,
     * etc.).
     *
     * @param ch  Unicode code point
     * @param alt if true, set alt on the TKeypress
     * @return one TKeypress event, either a control character (e.g. isKey ==
     * false, ch == 'A', ctrl == true), or a special key (e.g. isKey == true,
     * fnKey == ESC)
     */
    private TKeypressEvent controlChar(final char ch, final boolean alt) {
        return switch (ch) {
            case 0x0D, 0x0A -> // Carriage return or Linefeed --> ENTER
                new TKeypressEvent(backend, kbEnter, alt, false, false);
            case 0x1B -> // ESC
                new TKeypressEvent(backend, kbEsc, alt, false, false);
            case '\t' -> // TAB
                new TKeypressEvent(backend, kbTab, alt, false, false);
            default ->
                // Make all other control characters come back as the alphabetic
                // character with the ctrl field set.  So SOH would be 'A' + ctrl.
                new TKeypressEvent(backend, false, 0, (char) (ch + 0x40),
                    alt, true, false);
        };
    }

    /**
     * Handle arrow key events. This is a common helper to avoid code duplication
     * between CSI (ESC [) and SS3 (ESC O) arrow key sequences.
     *
     * @param events the list to add events to
     * @param ch     the character identifying the arrow key ('A'=Up, 'B'=Down, 'C'=Right, 'D'=Left)
     * @param alt    true if Alt was pressed
     * @param ctrl   true if Ctrl was pressed
     * @param shift  true if Shift was pressed
     * @return true if the character was an arrow key, false otherwise
     */
    private boolean handleArrowKey(final List<TInputEvent> events, final int ch,
                                   final boolean alt, final boolean ctrl, final boolean shift) {
        TKeypressEvent event = switch (ch) {
            case 'A' -> new TKeypressEvent(backend, kbUp, alt, ctrl, shift);
            case 'B' -> new TKeypressEvent(backend, kbDown, alt, ctrl, shift);
            case 'C' -> new TKeypressEvent(backend, kbRight, alt, ctrl, shift);
            case 'D' -> new TKeypressEvent(backend, kbLeft, alt, ctrl, shift);
            default -> null;
        };
        if (event != null) {
            events.add(event);
            return true;
        }
        return false;
    }

    /**
     * Produce special key from CSI Pn ; Pm ; ... ~
     *
     * @return one KEYPRESS event representing a special key
     */
    private TInputEvent csiFnKey() {
        int key = 0;
        if (!params.isEmpty()) {
            key = Integer.parseInt(params.getFirst());
        }
        boolean alt = false;
        boolean ctrl = false;
        boolean shift = false;

        int otherKey = 0;
        if (params.size() > 1) {
            shift = csiIsShift(params.get(1));
            alt = csiIsAlt(params.get(1));
            ctrl = csiIsCtrl(params.get(1));
        }
        if (params.size() > 2) {
            otherKey = Integer.parseInt(params.get(2));
        }

        return switch (key) {
            case 1 -> new TKeypressEvent(backend, kbHome, alt, ctrl, shift);
            case 2 -> new TKeypressEvent(backend, kbIns, alt, ctrl, shift);
            case 3 -> new TKeypressEvent(backend, kbDel, alt, ctrl, shift);
            case 4 -> new TKeypressEvent(backend, kbEnd, alt, ctrl, shift);
            case 5 -> new TKeypressEvent(backend, kbPgUp, alt, ctrl, shift);
            case 6 -> new TKeypressEvent(backend, kbPgDn, alt, ctrl, shift);
            case 15 -> new TKeypressEvent(backend, kbF5, alt, ctrl, shift);
            case 17 -> new TKeypressEvent(backend, kbF6, alt, ctrl, shift);
            case 18 -> new TKeypressEvent(backend, kbF7, alt, ctrl, shift);
            case 19 -> new TKeypressEvent(backend, kbF8, alt, ctrl, shift);
            case 20 -> new TKeypressEvent(backend, kbF9, alt, ctrl, shift);
            case 21 -> new TKeypressEvent(backend, kbF10, alt, ctrl, shift);
            case 23 -> new TKeypressEvent(backend, kbF11, alt, ctrl, shift);
            case 24 -> new TKeypressEvent(backend, kbF12, alt, ctrl, shift);
            case 27 -> handleModifyOtherKeys(otherKey, alt, ctrl, shift);
            default -> null; // Unknown
        };
    }

    /**
     * Handle the modifyOtherKeys sequence (CSI 27).
     *
     * @param otherKey the key code
     * @param alt      true if alt was pressed
     * @param ctrl     true if ctrl was pressed
     * @param shift    true if shift was pressed
     * @return the corresponding keypress event, or null if unsupported
     */
    private TInputEvent handleModifyOtherKeys(final int otherKey, final boolean alt,
                                               final boolean ctrl, final boolean shift) {
        return switch (otherKey) {
            case 8 -> new TKeypressEvent(backend, kbBackspace, alt, ctrl, shift);
            case 9 -> new TKeypressEvent(backend, kbTab, alt, ctrl, shift);
            case 13 -> new TKeypressEvent(backend, kbEnter, alt, ctrl, shift);
            case 27 -> new TKeypressEvent(backend, kbEsc, alt, ctrl, shift);
            default -> {
                if (otherKey < 32) {
                    yield null;
                }
                if (otherKey >= 'a' && otherKey <= 'z' && ctrl) {
                    // Turn Ctrl-lowercase into Ctrl-uppercase
                    yield new TKeypressEvent(backend, false, 0, (otherKey - 32),
                        alt, ctrl, shift);
                }
                yield new TKeypressEvent(backend, false, 0, otherKey, alt, ctrl, shift);
            }
        };
    }

    /**
     * Produce mouse events based on "Any event tracking" and UTF-8
     * coordinates.  See
     * http://invisible-island.net/xterm/ctlseqs/ctlseqs.html#Mouse%20Tracking
     *
     * @return a MOUSE_MOTION, MOUSE_UP, or MOUSE_DOWN event
     */
    private TInputEvent parseMouse() {
        String firstParam = params.getFirst();
        int buttons = firstParam.charAt(0) - 32;
        int x = firstParam.charAt(1) - 32 - 1;
        int y = firstParam.charAt(2) - 32 - 1;

        // Clamp X and Y to the physical screen coordinates.
        if (x >= windowResize.getWidth()) {
            x = windowResize.getWidth() - 1;
        }
        if (y >= windowResize.getHeight()) {
            y = windowResize.getHeight() - 1;
        }

        TMouseEvent.Type eventType = TMouseEvent.Type.MOUSE_DOWN;
        boolean eventMouse1 = false;
        boolean eventMouse2 = false;
        boolean eventMouse3 = false;
        boolean eventMouseWheelUp = false;
        boolean eventMouseWheelDown = false;
        boolean eventMouseWheelLeft = false;
        boolean eventMouseWheelRight = false;
        boolean eventAlt = false;
        boolean eventCtrl = false;
        boolean eventShift = false;

        // System.err.printf("buttons: %04x\r\n", buttons);

        switch (buttons & 0xE3) {
            case 0:
                eventMouse1 = true;
                // X10 mouse protocol doesn't send the motion bit (32) during drag operations.
                // If mouse1 is already tracked as pressed, treat this as motion instead of
                // a new press event. This also fixes JLine on Windows.
                if (mouse1) {
                    eventType = TMouseEvent.Type.MOUSE_MOTION;
                } else {
                    mouse1 = true;
                }
                break;
            case 1:
                eventMouse2 = true;
                // Same fix for mouse2 dragging
                if (mouse2) {
                    eventType = TMouseEvent.Type.MOUSE_MOTION;
                } else {
                    mouse2 = true;
                }
                break;
            case 2:
                eventMouse3 = true;
                // Same fix for mouse3 dragging
                if (mouse3) {
                    eventType = TMouseEvent.Type.MOUSE_MOTION;
                } else {
                    mouse3 = true;
                }
                break;
            case 3:
                // Release or Move
                if (!mouse1 && !mouse2 && !mouse3) {
                    eventType = TMouseEvent.Type.MOUSE_MOTION;
                } else {
                    eventType = TMouseEvent.Type.MOUSE_UP;
                }
                if (mouse1) {
                    mouse1 = false;
                    eventMouse1 = true;
                }
                if (mouse2) {
                    mouse2 = false;
                    eventMouse2 = true;
                }
                if (mouse3) {
                    mouse3 = false;
                    eventMouse3 = true;
                }
                break;

            case 32:
                // Dragging with mouse1 down
                eventMouse1 = true;
                mouse1 = true;
                eventType = TMouseEvent.Type.MOUSE_MOTION;
                break;

            case 33, // Dragging with mouse2 down
                 96, // Dragging with mouse2 down after wheelUp
                 97: // Dragging with mouse2 down after wheelDown
                eventMouse2 = true;
                mouse2 = true;
                eventType = TMouseEvent.Type.MOUSE_MOTION;
                break;

            case 34:
                // Dragging with mouse3 down
                eventMouse3 = true;
                mouse3 = true;
                eventType = TMouseEvent.Type.MOUSE_MOTION;
                break;

            case 64:
                eventMouseWheelUp = true;
                break;

            case 65:
                eventMouseWheelDown = true;
                break;

            case 66:
                eventMouseWheelRight = true;
                break;

            case 67:
                eventMouseWheelLeft = true;
                break;

            default:
                // Unknown, just make it motion
                eventType = TMouseEvent.Type.MOUSE_MOTION;
                break;
        }

        if ((buttons & 0x04) != 0) {
            eventShift = true;
        }
        if ((buttons & 0x08) != 0) {
            eventAlt = true;
        }
        if ((buttons & 0x10) != 0) {
            eventCtrl = true;
        }

        return new TMouseEvent(backend, eventType, x, y, x, y, 0, 0,
            eventMouse1, eventMouse2, eventMouse3,
            eventMouseWheelUp, eventMouseWheelDown,
            eventMouseWheelLeft, eventMouseWheelRight,
            eventAlt, eventCtrl, eventShift);
    }

    /**
     * Produce mouse events based on "Any event tracking" and SGR
     * coordinates.  See
     * http://invisible-island.net/xterm/ctlseqs/ctlseqs.html#Mouse%20Tracking
     *
     * @param release if true, this was a release ('m')
     * @return a MOUSE_MOTION, MOUSE_UP, or MOUSE_DOWN event
     */
    private TInputEvent parseMouseSGR(final boolean release) {
        // SGR extended coordinates - mode 1006 or 1016
        if (params.size() < 3) {
            // Invalid position, bail out.
            return null;
        }
        int buttons = Integer.parseInt(params.get(0));
        int x = Integer.parseInt(params.get(1)) - 1;
        int y = Integer.parseInt(params.get(2)) - 1;
        int offsetX = 0;
        int offsetY = 0;

        // Clamp X and Y to the physical screen coordinates.
        if (x >= windowResize.getWidth()) {
            x = windowResize.getWidth() - 1;
        }
        if (y >= windowResize.getHeight()) {
            y = windowResize.getHeight() - 1;
        }

        TMouseEvent.Type eventType = TMouseEvent.Type.MOUSE_DOWN;
        boolean eventMouse1 = false;
        boolean eventMouse2 = false;
        boolean eventMouse3 = false;
        boolean eventMouseWheelUp = false;
        boolean eventMouseWheelDown = false;
        boolean eventMouseWheelLeft = false;
        boolean eventMouseWheelRight = false;
        boolean eventAlt = false;
        boolean eventCtrl = false;
        boolean eventShift = false;

        if (release) {
            eventType = TMouseEvent.Type.MOUSE_UP;
        }

        // Check if this is a wheel event - wheel events don't have releases
        int buttonCode = buttons & 0xE3;
        if (release && (buttonCode >= 64 && buttonCode <= 67)) {
            // Ignore release events for wheel buttons (vertical and horizontal)
            // Wheel events are instant actions, not press/release pairs
            return null;
        }

        switch (buttons & 0xE3) {
            case 0:
                eventMouse1 = true;
                break;
            case 1:
                eventMouse2 = true;
                break;
            case 2:
                eventMouse3 = true;
                break;
            case 35:
                // Motion only, no buttons down
                eventType = TMouseEvent.Type.MOUSE_MOTION;
                break;

            case 32:
                // Dragging with mouse1 down
                eventMouse1 = true;
                eventType = TMouseEvent.Type.MOUSE_MOTION;
                break;

            case 33, // Dragging with mouse2 down
                 96, // Dragging with mouse2 down after wheelUp
                 97: // Dragging with mouse2 down after wheelDown
                eventMouse2 = true;
                eventType = TMouseEvent.Type.MOUSE_MOTION;
                break;

            case 34:
                // Dragging with mouse3 down
                eventMouse3 = true;
                eventType = TMouseEvent.Type.MOUSE_MOTION;
                break;

            case 64:
                eventMouseWheelUp = true;
                break;

            case 65:
                eventMouseWheelDown = true;
                break;

            case 66:
                eventMouseWheelRight = true;
                break;

            case 67:
                eventMouseWheelLeft = true;
                break;

            default:
                // Unknown, bail out
                return null;
        }

        if ((buttons & 0x04) != 0) {
            eventShift = true;
        }
        if ((buttons & 0x08) != 0) {
            eventAlt = true;
        }
        if ((buttons & 0x10) != 0) {
            eventCtrl = true;
        }

        return new TMouseEvent(backend, eventType, x, y,
            x, y, offsetX, offsetY,
            eventMouse1, eventMouse2, eventMouse3,
            eventMouseWheelUp, eventMouseWheelDown,
            eventMouseWheelLeft, eventMouseWheelRight,
            eventAlt, eventCtrl, eventShift);
    }

    /**
     * Return any events in the IO queue due to timeout.
     *
     * @param queue list to append new events to
     */
    private void getIdleEvents(final List<TInputEvent> queue) {
        long nowTime = System.currentTimeMillis();

        // Check for new window size
        long windowSizeDelay = nowTime - windowSizeTime;
        if (windowSizeDelay > 1000) {
            int oldTextWidth = getTextWidth();
            int oldTextHeight = getTextHeight();
            boolean useStty = true;

            //noinspection RedundantIfStatement
            if (sessionInfo instanceof TTYSessionInfo ttySessionInfo && ttySessionInfo.output != null) {
                // If we are using CSI 18 t, the new dimensions will come
                // later.
                useStty = false;
            }
            sessionInfo.queryWindowSize();

            if (useStty) {
                int newWidth = sessionInfo.getWindowWidth();
                int newHeight = sessionInfo.getWindowHeight();

                if ((newWidth != windowResize.getWidth())
                    || (newHeight != windowResize.getHeight())
                ) {

                    // Request xterm report window dimensions in pixels
                    // again.  Between now and then, ensure that the reported
                    // text cell size is the same by setting widthPixels and
                    // heightPixels to match the new dimensions.
                    widthPixels = oldTextWidth * newWidth;
                    heightPixels = oldTextHeight * newHeight;

                    if (debugToStderr) {
                        System.err.println("Screen size changed, old size " +
                            windowResize);
                        System.err.println("                     new size " +
                            newWidth + " x " + newHeight);
                        System.err.println("                old cell sixe " +
                            oldTextWidth + " x " + oldTextHeight);
                        System.err.println("                new cell size " +
                            getTextWidth() + " x " + getTextHeight());
                    }

                    if (output != null) {
                        output.printf("%s", xtermReportPixelDimensions());
                        output.flush();
                    }

                    TResizeEvent event = new TResizeEvent(backend,
                        TResizeEvent.Type.SCREEN, newWidth, newHeight);
                    windowResize = new TResizeEvent(backend,
                        TResizeEvent.Type.SCREEN, newWidth, newHeight);
                    queue.add(event);
                }
            }

            windowSizeTime = nowTime;
        }

        // ESCDELAY type timeout
        if (state == ParseState.ESCAPE) {
            long escDelay = nowTime - escapeTime;
            if (escDelay > 100) {
                // After 0.1 seconds, assume a true escape character
                queue.add(controlChar((char) 0x1B, false));
                resetParser();
            }
        }
    }

    /**
     * Set of CSI parameters indicating Shift key was pressed.
     */
    private static final Set<String> CSI_SHIFT_PARAMS = Set.of("2", "4", "6", "8");

    /**
     * Set of CSI parameters indicating Alt key was pressed.
     */
    private static final Set<String> CSI_ALT_PARAMS = Set.of("3", "4", "7", "8");

    /**
     * Set of CSI parameters indicating Ctrl key was pressed.
     */
    private static final Set<String> CSI_CTRL_PARAMS = Set.of("5", "6", "7", "8");

    /**
     * Returns true if the CSI parameter for a keyboard command means that
     * shift was down.
     */
    private boolean csiIsShift(final String x) {
        return CSI_SHIFT_PARAMS.contains(x);
    }

    /**
     * Returns true if the CSI parameter for a keyboard command means that
     * alt was down.
     */
    private boolean csiIsAlt(final String x) {
        return CSI_ALT_PARAMS.contains(x);
    }

    /**
     * Returns true if the CSI parameter for a keyboard command means that
     * ctrl was down.
     */
    private boolean csiIsCtrl(final String x) {
        return CSI_CTRL_PARAMS.contains(x);
    }

    /**
     * Apply heuristics against the version string returned by XTVERSION.
     *
     * @param text the xtversion text string
     */
    private void fingerprintTerminal(final String text) {
        if (debugToStderr) {
            System.err.println("fingerprintTerminal(): '" + text + "'");
        }
        if (text.length() > 1) {
            terminalVersion = text.substring(1);
        }

        if (text.contains("XTerm")) {
            if (debugToStderr) {
                System.err.println("  -- Genuine(tm) XTerm! -- ");
            }
            isGenuineXTerm = true;
            if (sixel && (textBlinkOption == TextBlinkOption.AUTO)) {
                if (debugToStderr) {
                    System.err.println("  -- sixel enabled, so soft blink -- ");
                }
                textBlinkOption = TextBlinkOption.SOFT;
            }
        }

        // Konsole places image data underneath text, such that
        // erasing text will re-expose the image.  We need to
        // explicitly destroy any images being overwritten by text for
        // that case.
        if (text.contains("Konsole")) {
            String str = System.getProperty("casciian.ECMA48.explicitlyDestroyImages");
            if ((str != null) && (str.equals("false"))) {
                if (debugToStderr) {
                    System.err.println("  -- terminal requires " +
                        "explicitlyDestroyImages, but is disabled in config");
                }
                explicitlyDestroyImages = false;
            } else {
                if (debugToStderr) {
                    System.err.println("  -- terminal requires explicitlyDestroyImages");
                }
                //explicitlyDestroyImages = true;
            }

        }

        setXtermMousePointer(POINTER_SHAPE_LEFT_PTR);
    }

    /**
     * Process an OSC response.
     *
     * @param text the OSC response string
     */
    void oscResponse(final String text) {
        if (debugToStderr) {
            System.err.println("oscResponse(): '" + text + "'");
        }

        String[] ps = text.split(";");
        if (ps.length == 0) {
            return;
        }
        final int oscIndex = 0;
        final int colorIndex = 1;
        int rgbIndex = 2;

        boolean isColorPalette = ps[oscIndex].equals(OSC_PALETTE);
        boolean isDefaultForegroundColor = ps[oscIndex].equals(OSC_DEFAULT_FORECOLOR);
        boolean isDefaultBackgroundColor = ps[oscIndex].equals(OSC_DEFAULT_BACKCOLOR);

        if (!isColorPalette && !isDefaultForegroundColor && !isDefaultBackgroundColor) {
            return;
        }

        if (isColorPalette && ps.length != 3) {
            return;
        }

        try {
            int color;
            if ((isDefaultForegroundColor || isDefaultBackgroundColor)) {
                if (ps.length != 2) {
                    return;
                }

                rgbIndex = 1;
                color = isDefaultForegroundColor ? 39 : 49;
            } else {
                color = Integer.parseInt(ps[colorIndex]);
            }

            String rgb = ps[rgbIndex];
            if (!rgb.startsWith("rgb:")) {
                return;
            }
            rgb = rgb.substring(4);
            if (debugToStderr) {
                System.err.println("  Color " + color + " is " + rgb);
            }
            String[] rgbs = rgb.split("/");
            if (rgbs.length != 3) {
                return;
            }
            int red = Integer.parseInt(rgbs[0], 16);
            int green = Integer.parseInt(rgbs[1], 16);
            int blue = Integer.parseInt(rgbs[2], 16);
            if (rgbs[0].length() == 4) {
                red = red >> 8;
            }
            if (rgbs[1].length() == 4) {
                green = green >> 8;
            }
            if (rgbs[2].length() == 4) {
                blue = blue >> 8;
            }
            if (debugToStderr) {
                System.err.printf("    RGB %02x%02x%02x\n",
                    red, green, blue);
            }
            int rgbColor = red << 16 | green << 8 | blue;
            setColorFromOsc(color, rgbColor);

            // We have changed a system color.  Redraw the entire screen.
            clearPhysical();
            reallyCleared = true;
        } catch (NumberFormatException e) {
            // SQUASH
        }
    }

    /**
     * Set a palette color from an OSC response.
     *
     * @param colorIndex the color index (0-15 for palette, 39 for default foreground, 49 for default background)
     * @param rgbColor   the RGB color value
     */
    private void setColorFromOsc(final int colorIndex, final int rgbColor) {
        switch (colorIndex) {
            case 0 -> {
                MYBLACK = rgbColor;
                if (debugToStderr) System.err.println("    Set BLACK");
            }
            case 1 -> {
                MYRED = rgbColor;
                if (debugToStderr) System.err.println("    Set RED");
            }
            case 2 -> {
                MYGREEN = rgbColor;
                if (debugToStderr) System.err.println("    Set GREEN");
            }
            case 3 -> {
                MYYELLOW = rgbColor;
                if (debugToStderr) System.err.println("    Set YELLOW");
            }
            case 4 -> {
                MYBLUE = rgbColor;
                if (debugToStderr) System.err.println("    Set BLUE");
            }
            case 5 -> {
                MYMAGENTA = rgbColor;
                if (debugToStderr) System.err.println("    Set MAGENTA");
            }
            case 6 -> {
                MYCYAN = rgbColor;
                if (debugToStderr) System.err.println("    Set CYAN");
            }
            case 7 -> {
                MYWHITE = rgbColor;
                if (debugToStderr) System.err.println("    Set WHITE");
            }
            case 8 -> {
                MYBOLD_BLACK = rgbColor;
                if (debugToStderr) System.err.println("    Set BOLD BLACK");
            }
            case 9 -> {
                MYBOLD_RED = rgbColor;
                if (debugToStderr) System.err.println("    Set BOLD RED");
            }
            case 10 -> {
                MYBOLD_GREEN = rgbColor;
                if (debugToStderr) System.err.println("    Set BOLD GREEN");
            }
            case 11 -> {
                MYBOLD_YELLOW = rgbColor;
                if (debugToStderr) System.err.println("    Set BOLD YELLOW");
            }
            case 12 -> {
                MYBOLD_BLUE = rgbColor;
                if (debugToStderr) System.err.println("    Set BOLD BLUE");
            }
            case 13 -> {
                MYBOLD_MAGENTA = rgbColor;
                if (debugToStderr) System.err.println("    Set BOLD MAGENTA");
            }
            case 14 -> {
                MYBOLD_CYAN = rgbColor;
                if (debugToStderr) System.err.println("    Set BOLD CYAN");
            }
            case 15 -> {
                MYBOLD_WHITE = rgbColor;
                if (debugToStderr) System.err.println("    Set BOLD WHITE");
            }
            case 39 -> {
                DEFAULT_FORECOLOR = rgbColor;
                if (debugToStderr) System.err.println("    Set DEFAULT FOREGROUND");
            }
            case 49 -> {
                DEFAULT_BACKCOLOR = rgbColor;
                if (debugToStderr) System.err.println("    Set DEFAULT BACKGROUND");
            }
            default -> {
                // Unknown color index, ignore
            }
        }
    }

    /**
     * Parses the next character of input to see if an InputEvent is
     * fully here.
     *
     * @param events list to append new events to
     * @param ch     Unicode code point
     */
    private void processChar(final List<TInputEvent> events, final char ch) {

        // ESCDELAY type timeout
        long nowTime = System.currentTimeMillis();
        if (state == ParseState.ESCAPE) {
            long escDelay = nowTime - escapeTime;
            if (escDelay > 250) {
                // After 0.25 seconds, assume a true escape character
                events.add(controlChar((char) 0x1B, false));
                resetParser();
            }
        }

        // TKeypress fields
        boolean ctrl = false;
        boolean alt = false;
        boolean shift = false;

        if (debugToStderr) {
            System.err.printf("state: %s ch %c\r\n", state, ch);
        }

        switch (state) {
            case GROUND:

                if (ch == 0x1B) {
                    state = ParseState.ESCAPE;
                    escapeTime = nowTime;
                    return;
                }

                if (ch <= 0x1F) {
                    // Control character
                    events.add(controlChar(ch, false));
                    resetParser();
                    return;
                } else {
                    // Normal character
                    events.add(new TKeypressEvent(backend, false, 0, ch,
                        false, false, false));
                    resetParser();
                    return;
                }

            case ESCAPE:
                // 'P', during the XTVERSION query only, goes to XTVERSION.
                // What a fucking mess.
                if ((ch == 'P') && (xtversionQuery == true)) {
                    state = ParseState.XTVERSION;
                    xtversionResponse.setLength(0);
                    xtversionQuery = false;
                    return;
                }
                xtversionQuery = false;

                if (ch == ']') {
                    state = ParseState.OSC;
                    oscResponse.setLength(0);
                    return;
                }

                if (ch <= 0x1F) {
                    // ALT-Control character
                    events.add(controlChar(ch, true));
                    resetParser();
                    return;
                }

                if (ch == 'O') {
                    // This will be one of the function keys
                    state = ParseState.ESCAPE_INTERMEDIATE;
                    return;
                }

                // '[' goes to CSI_ENTRY
                if (ch == '[') {
                    state = ParseState.CSI_ENTRY;
                    return;
                }

                // Everything else is assumed to be Alt-keystroke
                if ((ch >= 'A') && (ch <= 'Z')) {
                    shift = true;
                }
                alt = true;
                events.add(new TKeypressEvent(backend, false, 0, ch,
                    alt, ctrl, shift));
                resetParser();
                return;

            case ESCAPE_INTERMEDIATE:
                if ((ch >= 'P') && (ch <= 'S')) {
                    // Function key
                    switch (ch) {
                        case 'P':
                            events.add(new TKeypressEvent(backend, kbF1));
                            break;
                        case 'Q':
                            events.add(new TKeypressEvent(backend, kbF2));
                            break;
                        case 'R':
                            events.add(new TKeypressEvent(backend, kbF3));
                            break;
                        case 'S':
                            events.add(new TKeypressEvent(backend, kbF4));
                            break;
                        default:
                            break;
                    }
                    resetParser();
                    return;
                }

                // Arrow keys in SS3 (ESC O) format - used by JLine on Windows
                // JLine's Windows terminal sends ESC O A/B/C/D instead of ESC [ A/B/C/D
                if (handleArrowKey(events, ch, alt, ctrl, shift)) {
                    resetParser();
                    return;
                }

                // Unknown keystroke, ignore
                resetParser();
                return;

            case CSI_ENTRY:
                // Numbers - parameter values
                if ((ch >= '0') && (ch <= '9')) {
                    params.set(params.size() - 1,
                        params.get(params.size() - 1) + ch);
                    state = ParseState.CSI_PARAM;
                    return;
                }
                // Parameter separator
                if (ch == ';') {
                    params.add("");
                    return;
                }

                if ((ch >= 0x30) && (ch <= 0x7E)) {
                    // Try arrow keys first using the unified handler
                    if (handleArrowKey(events, ch, alt, ctrl, shift)) {
                        resetParser();
                        return;
                    }

                    switch (ch) {
                        case 'H':
                            // Home
                            events.add(new TKeypressEvent(backend, kbHome));
                            resetParser();
                            return;
                        case 'F':
                            // End
                            events.add(new TKeypressEvent(backend, kbEnd));
                            resetParser();
                            return;
                        case 'Z':
                            // CBT - Cursor backward X tab stops (default 1)
                            events.add(new TKeypressEvent(backend, kbBackTab));
                            resetParser();
                            return;
                        case 'M':
                            // Mouse position
                            state = ParseState.MOUSE;
                            return;
                        case '<':
                            // Mouse position, SGR (1006) coordinates
                            state = ParseState.MOUSE_SGR;
                            return;
                        case '?':
                            // DEC private mode flag
                            decPrivateModeFlag = true;
                            return;
                        case 'I':
                            // Focus in
                            hasFocus = true;
                            resetParser();
                            return;
                        case 'O':
                            // Focus out
                            hasFocus = false;
                            resetParser();
                            return;
                        default:
                            break;
                    }
                }

                // Unknown keystroke, ignore
                resetParser();
                return;

            case MOUSE_SGR:
                // Numbers - parameter values
                if ((ch >= '0') && (ch <= '9')) {
                    params.set(params.size() - 1,
                        params.get(params.size() - 1) + ch);
                    return;
                }
                // Parameter separator
                if (ch == ';') {
                    params.add("");
                    return;
                }

                switch (ch) {
                    case 'M':
                        // Generate a mouse press event
                        TInputEvent event = parseMouseSGR(false);
                        if (event != null) {
                            events.add(event);
                        }
                        resetParser();
                        return;
                    case 'm':
                        // Generate a mouse release event
                        event = parseMouseSGR(true);
                        if (event != null) {
                            events.add(event);
                        }
                        resetParser();
                        return;
                    default:
                        break;
                }

                // Unknown keystroke, ignore
                resetParser();
                return;

            case CSI_PARAM:
                // Numbers - parameter values
                if ((ch >= '0') && (ch <= '9')) {
                    params.set(params.size() - 1,
                        params.get(params.size() - 1) + ch);
                    state = ParseState.CSI_PARAM;
                    return;
                }
                // Parameter separator
                if (ch == ';') {
                    params.add("");
                    return;
                }

                if (ch == '~') {
                    events.add(csiFnKey());
                    resetParser();
                    return;
                }

                if (ch == '$') {
                    // This will be the DECRPM response to a DECRQM mode
                    // query.
                    if (decPrivateModeFlag) {
                        decDollarModeFlag = true;
                        return;
                    }
                }

                if ((ch >= 0x30) && (ch <= 0x7E)) {
                    switch (ch) {
                        case 'A':
                            // Up
                            if (params.size() > 1) {
                                shift = csiIsShift(params.get(1));
                                alt = csiIsAlt(params.get(1));
                                ctrl = csiIsCtrl(params.get(1));
                            }
                            events.add(new TKeypressEvent(backend, kbUp, alt, ctrl, shift));
                            resetParser();
                            return;
                        case 'B':
                            // Down
                            if (params.size() > 1) {
                                shift = csiIsShift(params.get(1));
                                alt = csiIsAlt(params.get(1));
                                ctrl = csiIsCtrl(params.get(1));
                            }
                            events.add(new TKeypressEvent(backend, kbDown, alt, ctrl, shift));
                            resetParser();
                            return;
                        case 'C':
                            // Right
                            if (params.size() > 1) {
                                shift = csiIsShift(params.get(1));
                                alt = csiIsAlt(params.get(1));
                                ctrl = csiIsCtrl(params.get(1));
                            }
                            events.add(new TKeypressEvent(backend, kbRight, alt, ctrl, shift));
                            resetParser();
                            return;
                        case 'D':
                            // Left
                            if (params.size() > 1) {
                                shift = csiIsShift(params.get(1));
                                alt = csiIsAlt(params.get(1));
                                ctrl = csiIsCtrl(params.get(1));
                            }
                            events.add(new TKeypressEvent(backend, kbLeft, alt, ctrl, shift));
                            resetParser();
                            return;
                        case 'H':
                            // Home
                            if (params.size() > 1) {
                                shift = csiIsShift(params.get(1));
                                alt = csiIsAlt(params.get(1));
                                ctrl = csiIsCtrl(params.get(1));
                            }
                            events.add(new TKeypressEvent(backend, kbHome, alt, ctrl, shift));
                            resetParser();
                            return;
                        case 'F':
                            // End
                            if (params.size() > 1) {
                                shift = csiIsShift(params.get(1));
                                alt = csiIsAlt(params.get(1));
                                ctrl = csiIsCtrl(params.get(1));
                            }
                            events.add(new TKeypressEvent(backend, kbEnd, alt, ctrl, shift));
                            resetParser();
                            return;
                        case 'S':
                            // Report graphics property.
                            if (decPrivateModeFlag == false) {
                                break;
                            }

                            if ((params.size() > 2)
                                && (!params.get(1).equals("0"))
                            ) {
                                if (debugToStderr) {
                                    System.err.printf("Graphics query error: " +
                                        params);
                                }
                                break;
                            }

                            if (params.size() > 2) {
                                if (debugToStderr) {
                                    System.err.printf("Graphics result: " +
                                            "status %s Ps %s Pv %s\n", params.get(0),
                                        params.get(1), params.get(2));
                                }
                                if (params.get(0).equals("1")) {
                                    int registers = sixelEncoder.getPaletteSize();
                                    try {
                                        registers = Integer.parseInt(params.get(2));
                                        if (debugToStderr) {
                                            System.err.println("Terminal reports " +
                                                registers + " sixel colors, current " +
                                                "size = " +
                                                sixelEncoder.getPaletteSize());
                                        }
                                        if ((registers >= 2)
                                            && (registers < sixelEncoder.getPaletteSize())
                                        ) {
                                            try {
                                                sixelEncoder.setPaletteSize(Integer.highestOneBit(registers));
                                                if (debugToStderr) {
                                                    System.err.println("New palette size: "
                                                        + sixelEncoder.getPaletteSize());
                                                }
                                            } catch (IllegalArgumentException e) {
                                                if (debugToStderr) {
                                                    System.err.println("Unsupported palette size: "
                                                        + registers);
                                                }
                                            }
                                        }
                                    } catch (NumberFormatException e) {
                                        if (debugToStderr) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                            break;
                        case 'c':
                            // Device Attributes
                            if (decPrivateModeFlag == false) {
                                break;
                            }
                            daResponseSeen = true;

                            boolean reportsJexerImages = false;
                            boolean reportsSixelImages = false;
                            for (String x : params) {
                                if (debugToStderr) {
                                    System.err.println("Device Attributes: x = " + x);
                                }
                                if (x.equals("4")) {
                                    // Terminal reports sixel support
                                    if (debugToStderr) {
                                        System.err.println("Device Attributes: sixel");
                                    }
                                    reportsSixelImages = true;
                                    if (isGenuineXTerm
                                        && (textBlinkOption == TextBlinkOption.AUTO)
                                    ) {
                                        if (debugToStderr) {
                                            System.err.println("  -- GenuineXTerm, so soft blink -- ");
                                        }
                                        textBlinkOption = TextBlinkOption.SOFT;
                                    }
                                }
                                if (x.equals("52")) {
                                    // Terminal reports OSC 52 support
                                    if (debugToStderr) {
                                        System.err.println("Device Attributes: OSC52");
                                    }

                                    /*
                                     * For now, we do not expose the underlying
                                     * terminal capability to TApplication, because
                                     * we provide a per-application clipboard buffer
                                     * already.
                                     */
                                }

                                if (x.equals("444")) {
                                    // Terminal reports Casciian images support
                                    if (debugToStderr) {
                                        System.err.println("Device Attributes: Casciian images");
                                    }
                                    reportsJexerImages = true;
                                }
                            }
                            if (reportsSixelImages == false) {
                                // Terminal does not support Sixel images, disable
                                // them.
                                sixel = false;
                                if (debugToStderr) {
                                    System.err.println("Device Attributes: Disable Sixel images");
                                }
                            }
                            if (reportsJexerImages == false) {
                                // Terminal does not support Casciian images, disable
                                // them.
                                jexerImageOption = JexerImageOption.DISABLED;
                                if (debugToStderr) {
                                    System.err.println("Device Attributes: Disable Casciian images");
                                }
                            }
                            resetParser();
                            return;
                        case 't':
                            // windowOps
                            if ((params.size() > 2) && (params.get(0).equals("4"))) {
                                if (debugToStderr) {
                                    System.err.printf("windowOp 4t pixels: " +
                                            "height %s width %s\n",
                                        params.get(1), params.get(2));
                                }
                                try {
                                    widthPixels = Integer.parseInt(params.get(2));
                                    heightPixels = Integer.parseInt(params.get(1));
                                } catch (NumberFormatException e) {
                                    if (debugToStderr) {
                                        e.printStackTrace();
                                    }
                                }
                                if (widthPixels <= 0) {
                                    widthPixels = 640;
                                }
                                if (heightPixels <= 0) {
                                    heightPixels = 400;
                                }
                                if (debugToStderr) {
                                    System.err.printf("   screen pixels: %d x %d",
                                        widthPixels, heightPixels);
                                    System.err.println("  new cell size: " +
                                        getTextWidth() + " x " + getTextHeight());
                                }
                            }
                            if ((params.size() > 2) && (params.get(0).equals("6"))) {
                                if (debugToStderr) {
                                    System.err.printf("windowOp 6t text cell pixels: " +
                                            "cell height %s cell width %s\n",
                                        params.get(1), params.get(2));
                                    System.err.printf("             old screen size: " +
                                        "%d x %d cells\n", width, height);
                                }
                                try {
                                    textWidthPixels = Integer.parseInt(params.get(2));
                                    textHeightPixels = Integer.parseInt(params.get(1));
                                } catch (NumberFormatException e) {
                                    if (debugToStderr) {
                                        e.printStackTrace();
                                    }
                                }
                                if (debugToStderr) {
                                    System.err.println("  new cell size: " +
                                        textWidthPixels + " x " + textHeightPixels);
                                }
                            }
                            if ((params.size() > 2) && (params.get(0).equals("8"))) {
                                if (debugToStderr) {
                                    System.err.printf("windowOp 8t screen size: " +
                                            "height %s width %s\n",
                                        params.get(1), params.get(2));
                                    System.err.printf("             old screen size: " +
                                        "%d x %d\n", width, height);
                                }

                                // Since the terminal supports CSI 18 t, stop
                                // spawning stty.
                                if (sessionInfo instanceof TTYSessionInfo) {
                                    TTYSessionInfo tty = (TTYSessionInfo) sessionInfo;
                                    tty.output = output;

                                    int newHeight = height;
                                    int newWidth = width;
                                    try {
                                        newWidth = Integer.parseInt(params.get(2));
                                        newHeight = Integer.parseInt(params.get(1));
                                    } catch (NumberFormatException e) {
                                        if (debugToStderr) {
                                            e.printStackTrace();
                                        }
                                    }
                                    if (debugToStderr) {
                                        System.err.println("  reported window size: " +
                                            newWidth + " x " + newHeight);
                                    }
                                    if ((newWidth != width)
                                        || (newHeight != height)
                                    ) {
                                        tty.windowWidth = newWidth;
                                        tty.windowHeight = newHeight;
                                        TResizeEvent event = new TResizeEvent(backend,
                                            TResizeEvent.Type.SCREEN, newWidth,
                                            newHeight);
                                        windowResize = new TResizeEvent(backend,
                                            TResizeEvent.Type.SCREEN,
                                            newWidth, newHeight);
                                        setDimensions(newWidth, newHeight);
                                        synchronized (eventQueue) {
                                            eventQueue.add(event);
                                        }
                                        if (listener != null) {
                                            synchronized (listener) {
                                                listener.notifyAll();
                                            }
                                        }
                                    }
                                }
                            }
                            resetParser();
                            return;
                        case 'y':
                            if ((decPrivateModeFlag == true)
                                && (decDollarModeFlag == true)
                            ) {
                                if (debugToStderr) {
                                    System.err.println("DECRPM: " + params);
                                }
                                // DECRPM response
                                if (params.size() == 2) {
                                    String Pd = params.get(0);
                                    String Ps = params.get(1);
                                    if (Ps.equals("1")          // Set
                                        || Ps.equals("2")       // Reset
                                        || Ps.equals("3")       // Permanently set
                                        || Ps.equals("4")       // Permanently reset
                                    ) {
                                        // This option was recognized, and is in some
                                        // state.
                                        if (Pd.equals("2026")) {
                                            if (debugToStderr) {
                                                System.err.println("DECRPM: " +
                                                    "has Synchronized Output support");
                                            }
                                            hasSynchronizedOutput = true;
                                        }
                                        if (Pd.equals("8452")) {
                                            if (debugToStderr) {
                                                System.err.println("DECRPM: " +
                                                    "has sixelCursorOnRight support");
                                            }
                                            sixelCursorOnRight = true;
                                        }
                                    }
                                }
                                resetParser();
                                return;
                            }
                            // Unknown
                            break;
                        default:
                            break;
                    }
                }

                // Unknown keystroke, ignore
                resetParser();
                return;

            case MOUSE:
                params.set(0, params.get(params.size() - 1) + ch);
                if (params.get(0).length() == 3) {
                    // We have enough to generate a mouse event
                    events.add(parseMouse());
                    resetParser();
                }
                return;

            case XTVERSION:
                if ((ch == '\\') &&
                    (xtversionResponse.length() > 0) &&
                    (xtversionResponse.charAt(xtversionResponse.length() - 1)
                        == 0x1B)
                ) {
                    // This is ST, end of the line.
                    fingerprintTerminal(xtversionResponse.substring(1,
                        xtversionResponse.length() - 1));
                    resetParser();
                    return;
                }

                // Continue collecting until we see ST.
                xtversionResponse.append(ch);
                return;

            case OSC:
                if ((ch == '\\') &&
                    (oscResponse.length() > 0) &&
                    (oscResponse.charAt(oscResponse.length() - 1)
                        == 0x1B)
                ) {
                    // This is ST, end of the line.
                    oscResponse(oscResponse.substring(0, oscResponse.length() - 1));
                    resetParser();
                    return;
                }
                if (ch == 0x07) {
                    // This is BEL, end of the line.
                    oscResponse(oscResponse.toString());
                    resetParser();
                    return;
                }

                // Continue collecting until we see ST.
                oscResponse.append(ch);
                return;

            default:
                break;
        }

        // This "should" be impossible to reach
        return;
    }

    /**
     * Request (u)xterm to use the sixel settings we desire:
     * <p>
     * - disable sixel display mode
     * <p>
     * - disable private color registers (so that we can use one common
     * palette) if sixelSharedPalette is set
     * <p>
     * - query number of color registers
     * <p>
     * - sixel scrolling leaves cursor on right
     *
     * @return the string to emit to xterm
     */
    private String xtermSetSixelSettings() {
        if (sixelEncoder.hasSharedPalette()) {
            return "\033[?1070l\033[?1;1;0S\033[?8452h";
        } else {
            return "\033[?1070h\033[?1;1;0S\033[?8452h";
        }
    }

    /**
     * Restore (u)xterm its default sixel settings:
     * <p>
     * - enable sixel scrolling
     * <p>
     * - enable private color registers
     *
     * @return the string to emit to xterm
     */
    private String xtermResetSixelSettings() {
        return "\033[?1070h";
    }

    /**
     * Request (u)xterm to report its program version (XTVERSION).
     * <p>
     * I am not a fan of fingerprinting terminals in this fashion.  They
     * should instead be reporting their features in DA1 using one of the
     * available ~10000 unused IDs out there.  It is also bad because the
     * string returned looks like "Alt-P | {other text} ST", which is
     * completely valid keyboard input, hence the boolean to bypass Alt-P
     * processing IF the response comes in AND this has to be the FIRST thing
     * we send to the terminal.
     * <p>
     * Alas, fingerprinting is now the path of least resistance.
     * <p>
     * On the brighter side, this does allow us to report the exact terminal
     * and version for end-user troubleshooting.
     *
     * @return the string to emit to xterm
     */
    private String xtermReportVersion() {
        xtversionQuery = true;
        return "\033[>0q";
    }

    /**
     * Request (u)xterm to report the current window and cell size dimensions
     * in pixels.
     *
     * @return the string to emit to xterm
     */
    private String xtermReportPixelDimensions() {
        // We will ask for both text cell and window dimensions (in that
        // order!), and hopefully one of them will work.
        return "\033[16t\033[14t";
    }

    /**
     * Tell (u)xterm that we want alt- keystrokes to send escape + character
     * rather than set the 8th bit.  Anyone who wants UTF8 should want this
     * enabled.
     *
     * @param on if true, enable metaSendsEscape
     * @return the string to emit to xterm
     */
    private String xtermMetaSendsEscape(final boolean on) {
        if (on) {
            return "\033[?1036h\033[?1034l";
        }
        return "\033[?1036l";
    }

    /**
     * Create an xterm OSC sequence to change the window title.
     *
     * @param title the new title
     * @return the string to emit to xterm
     */
    private String getSetTitleString(final String title) {
        return "\033]2;" + title + "\007";
    }

    /**
     * Set sixel output support flag.
     *
     * @param sixel if true, then images will be emitted as sixel
     */
    public void setHasSixel(final boolean sixel) {
        // Don't step on the screen refresh thread.
        synchronized (this) {
            this.sixel = sixel;
            sixelEncoder.clearPalette();
            sixelCache = null;
            clearPhysical();
        }
    }

    /**
     * Get the sixel shared palette option.
     *
     * @return true if all sixel output is using the same palette that is set
     * in one DCS sequence and used in later sequences
     */
    public boolean hasSixelSharedPalette() {
        return sixelEncoder.hasSharedPalette();
    }

    /**
     * Set the sixel shared palette option.
     *
     * @param sharedPalette if true, then all sixel output will use the same
     *                      palette that is set in one DCS sequence and used in later sequences
     */
    public void setSixelSharedPalette(final boolean sharedPalette) {
        // Don't step on the screen refresh thread.
        synchronized (this) {
            sixelEncoder.setSharedPalette(sharedPalette);
            sixelCache = null;
            clearPhysical();
        }
    }

    /**
     * Get the number of colors in the sixel palette.
     *
     * @return the palette size
     */
    public int getSixelPaletteSize() {
        return sixelEncoder.getPaletteSize();
    }

    /**
     * Set the number of colors in the sixel palette.
     *
     * @param paletteSize the new palette size
     */
    public void setSixelPaletteSize(final int paletteSize) {
        // Don't step on the screen refresh thread.
        synchronized (this) {
            sixelEncoder.setPaletteSize(paletteSize);
            sixelCache = null;
            clearPhysical();
        }
    }

    /**
     * Start a sixel string for display one row's worth of bitmap data.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @return the string to emit to an ANSI / ECMA-style terminal
     */
    private String startSixel(final int x, final int y) {

        StringBuilder sb = new StringBuilder();

        assert (sixel == true);

        // Place the cursor.
        sb.append(sortableGotoXY(x, y));

        // DCS
        sb.append("\033Pq");

        // We might need to emit the palette.
        sixelEncoder.emitPalette(sb);

        return sb.toString();
    }

    /**
     * End a sixel string for display one row's worth of bitmap data.
     *
     * @return the string to emit to an ANSI / ECMA-style terminal
     */
    private String endSixel() {
        assert (sixel == true);

        // ST
        return ("\033\\");
    }

    /**
     * Create a sixel string representing a row of several cells containing
     * bitmap data.
     *
     * @param x     column coordinate.  0 is the left-most column.
     * @param y     row coordinate.  0 is the top-most row.
     * @param cells the cells containing the bitmap data
     * @return the string to emit to an ANSI / ECMA-style terminal
     */
    private String toSixel(final int x, final int y,
                           final ArrayList<Cell> cells) {

        StringBuilder sb = new StringBuilder();

        assert (cells != null);
        assert (cells.size() > 0);
        assert (cells.get(0).getImage() != null);

        if (sixel == false) {
            sb.append(normal());
            sb.append(gotoXY(x, y));
            for (int i = 0; i < cells.size(); i++) {
                sb.append(' ');
            }
            return sb.toString();
        }

        if ((y == height - 1) && (!sixelCursorOnRight || isGenuineXTerm)) {
            if (sixelEncoder instanceof HQSixelEncoder) {
                // HQ can emit images with transparency.  We can use that
                // along with DECSDM to get up to 1000 pixel width images on
                // the bottom row. We use this trick only with terminals that
                // do not support 8452 mode and the actual XTerm because it
                // always scrolls off the bottom line no matter what.
                emitSixelOnBottomRow(x, y, cells, sb);
                return sb.toString();
            } else {
                // We are on the bottom row.  If scrolling mode is enabled
                // (default), then VT320/xterm will scroll the entire screen if
                // we draw any pixels here.  Do not draw the image, bail out
                // instead.
                sb.append(normal());
                sb.append(gotoXY(x, y));
                for (int j = 0; j < cells.size(); j++) {
                    sb.append(' ');
                }
                return sb.toString();
            }
        }

        boolean saveInCache = true;
        if (sixelFastAndDirty) {
            saveInCache = false;
        } else {
            // Save and get rows to/from the cache that do NOT have inverted
            // cells.
            for (Cell cell : cells) {
                if (cell.isInvertedImage()) {
                    saveInCache = false;
                    break;
                }
                // Compute the hashcode so that the cell image hash is
                // available for looking up in the image cache.
                cell.hashCode();
            }

            if (saveInCache) {
                String cachedResult = sixelCache.get(cells);
                if (cachedResult != null) {
                    // System.err.println("CACHE HIT");
                    sb.append(startSixel(x, y));
                    sb.append(cachedResult);
                    sb.append(endSixel());
                    return sb.toString();
                }
                // System.err.println("CACHE MISS");
            }
        }

        // If the final image would be larger than 1000 pixels wide, break it
        // up into smaller images, but at least 8 cells wide.  Or if we are
        // using the HQ encoder and will have more than some multiple of the
        // palette size in total pixels.
        int maxChunkLength = 1000;
        if ((sixelEncoder instanceof HQSixelEncoder)
            && (sixelEncoder.getPaletteSize() > 64)
        ) {
            maxChunkLength = Math.max(8 * getTextWidth(),
                Math.min(maxChunkLength,
                    sixelEncoder.getPaletteSize() * 10 / getTextHeight()));
            /*
            System.err.printf("maxChunkLength: %d cache used size %d\n",
                maxChunkLength, sixelCache.size());
             */
        }
        if (cells.size() * getTextWidth() > maxChunkLength) {
            StringBuilder chunkSb = new StringBuilder();
            int chunkStart = 0;
            int chunkSize = maxChunkLength / getTextWidth();
            int remaining = cells.size();
            int chunkX = x;
            ArrayList<Cell> chunk;
            while (remaining > 0) {
                chunk = new ArrayList<Cell>(cells.subList(chunkStart,
                    chunkStart + Math.min(chunkSize, remaining)));
                chunkSb.append(toSixel(chunkX, y, chunk));
                chunkStart += chunkSize;
                remaining -= chunkSize;
                chunkX += chunkSize;
            }
            return chunkSb.toString();
        }

        ImageRGB image = cellsToImage(cells);
        String sixel = sixelEncoder.toSixel(image);

        if (saveInCache) {
            // This row is OK to save into the cache.
            sixelCache.put(cells, sixel);
        }

        return (startSixel(x, y) + sixel + endSixel());
    }

    /**
     * Create a sixel string representing a row of several cells containing
     * bitmap data on the bottom.  This technique may not work on all
     * terminals, and is limited to 1000 pixels from the left edge.
     *
     * @param x     column coordinate.  0 is the left-most column.
     * @param y     row coordinate.  0 is the top-most row.
     * @param cells the cells containing the bitmap data
     * @param sb    the StringBuilder to write to
     */
    private void emitSixelOnBottomRow(final int x, final int y,
                                      final ArrayList<Cell> cells, final StringBuilder sb) {

        int cellWidth = getTextWidth();
        int cellHeight = getTextHeight();
        int pixelX = x * cellWidth;
        int pixelY = y * cellHeight;
        int maxPixelX = pixelX + (cells.size() * cellWidth);
        int maxPixelY = pixelY + cellHeight;
        if ((maxPixelX > 1000) || (maxPixelY > 1000)) {
            // There is no point, xterm will not display this image.
            sb.append(normal());
            sb.append(gotoXY(x, y));
            for (int i = 0; i < cells.size(); i++) {
                sb.append(' ');
            }
            return;
        }

        // The final image will be 1000 x 1000 or less.
        ImageRGB cellsImage = cellsToImage(cells);
        ImageRGB fullImage = cellsImage.resizeCanvas(
            Math.min(maxPixelX, cellsImage.getWidth()),
            Math.min(maxPixelY, cellsImage.getHeight()),
            0x000000);

        // HQSixelEncoder.toSixel() can accept allowTransparent.
        String sixel = ((HQSixelEncoder) sixelEncoder).toSixel(fullImage, false);
        sb.append("\033[?80h\033P0;1;0q");
        sb.append(sixel);
        // System.err.println("SIXEL: " + sixel);
        sb.append("\033\\\033[?80l");
    }

    /**
     * Get the sixel support flag.
     *
     * @return true if this terminal is emitting sixel
     */
    public boolean hasSixel() {
        return sixel;
    }

    /**
     * Convert a horizontal range of cell's image data into a single
     * continuous image, rescaled and anti-aliased to match the current text
     * cell size.
     *
     * @param cells the cells containing image data
     * @return the image resized to the current text cell size
     */
    private ImageRGB cellsToImage(final List<Cell> cells) {
        ImageRGB firstImage = cells.getFirst().getImage();
        int tileWidth = firstImage.getWidth();
        int tileHeight = firstImage.getHeight();

        // Piece cells.get(x).getImage() pieces together into one larger
        // image for final rendering.
        int totalWidth = 0;
        int fullWidth = cells.size() * tileWidth;
        for (Cell cell : cells) {
            totalWidth += cell.getImage().getWidth();
        }

        ImageRGB image = new ImageRGB(fullWidth, tileHeight);

        int[] rgbArray;
        for (int i = 0; i < cells.size() - 1; i++) {

            try {
                rgbArray = cells.get(i).getImage().getRGB(0, 0,
                    tileWidth, tileHeight, null, 0, tileWidth);
            } catch (Exception e) {
                throw new RuntimeException("image " + tileWidth + "x" +
                    tileHeight +
                    " tile " + tileWidth + "x" +
                    tileHeight +
                    " cells.get(i).getImage() " +
                    cells.get(i).getImage() +
                    " i " + i +
                    " fullWidth " + fullWidth +
                    " fullHeight " + tileHeight, e);
            }
            image.setRGB(i * tileWidth, 0, tileWidth, tileHeight,
                rgbArray, 0, tileWidth);
        }
        totalWidth -= ((cells.size() - 1) * tileWidth);
        try {
            rgbArray = cells.get(cells.size() - 1).getImage().getRGB(0, 0,
                totalWidth, tileHeight, null, 0, totalWidth);
        } catch (Exception e) {
            // TODO: Both of these setRGB cases are failing sometimes in
            // the multihead case.  Figure it out.
            return image;
                /*
                throw new RuntimeException("image " + tileWidth + "x" +
                    tileHeight + " cells.get(cells.size() - 1).getImage() " +
                    cells.get(cells.size() - 1).getImage(), e);
                 */
        }

        try {
            image.setRGB((cells.size() - 1) * tileWidth, 0, totalWidth,
                tileHeight, rgbArray, 0, totalWidth);
        } catch (Exception e) {
            // TODO: Both of these setRGB cases are failing sometimes in the
            // multihead case.  Figure it out.
            return image;
            /*
            throw new RuntimeException("image " + tileWidth + "x" +
                tileHeight + " cells.get(cells.size() - 1).getImage() " +
                cells.get(cells.size() - 1).getImage(), e);
             */
        }

        if (totalWidth < tileWidth) {
            int backgroundColor = 0;
            for (int imageX = image.getWidth() - totalWidth;
                 imageX < image.getWidth(); imageX++) {

                for (int imageY = 0; imageY < tileHeight; imageY++) {
                    image.setRGB(imageX, imageY, backgroundColor);
                }
            }
        }

        return image;
    }

    // ------------------------------------------------------------------------
    // End sixel output support -----------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Casciian image output support ---------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Create a Casciian images string representing a row of several cells
     * containing bitmap data.
     *
     * @param x     column coordinate.  0 is the left-most column.
     * @param y     row coordinate.  0 is the top-most row.
     * @param cells the cells containing the bitmap data
     * @return the string to emit to an ANSI / ECMA-style terminal
     */
    private String toJexerImage(final int x, final int y,
                                final ArrayList<Cell> cells) {

        StringBuilder sb = new StringBuilder();

        assert (cells != null);
        assert (cells.size() > 0);
        assert (cells.get(0).getImage() != null);

        if (jexerImageOption == JexerImageOption.DISABLED) {
            sb.append(normal());
            sb.append(sortableGotoXY(x, y));
            for (int i = 0; i < cells.size(); i++) {
                sb.append(' ');
            }
            return sb.toString();
        }

        // Save and get rows to/from the cache that do NOT have inverted
        // cells.
        boolean saveInCache = true;
        for (Cell cell : cells) {
            if (cell.isInvertedImage()) {
                saveInCache = false;
                break;
            }
            // Compute the hashcode so that the cell image hash is available
            // for looking up in the image cache.
            cell.hashCode();
        }
        if (saveInCache) {
            String cachedResult = jexerCache.get(cells);
            if (cachedResult != null) {
                // System.err.println("CACHE HIT");
                sb.append(sortableGotoXY(x, y));
                sb.append(cachedResult);
                return sb.toString();
            }
            // System.err.println("CACHE MISS");
        }

        ImageRGB image = cellsToImage(cells);
        int fullHeight = image.getHeight();

        if (jexerImageOption == JexerImageOption.RGB) {

            // RGB
            sb.append(String.format("\033]444;0;%d;%d;0;", image.getWidth(),
                Math.min(image.getHeight(), fullHeight)));

            byte[] bytes = new byte[image.getWidth() * image.getHeight() * 3];
            int stride = image.getWidth();
            for (int px = 0; px < stride; px++) {
                for (int py = 0; py < image.getHeight(); py++) {
                    int rgb = image.getRGB(px, py);
                    bytes[(py * stride * 3) + (px * 3)] = (byte) ((rgb >>> 16) & 0xFF);
                    bytes[(py * stride * 3) + (px * 3) + 1] = (byte) ((rgb >>> 8) & 0xFF);
                    bytes[(py * stride * 3) + (px * 3) + 2] = (byte) (rgb & 0xFF);
                }
            }
            sb.append(StringUtils.toBase64(bytes));
            sb.append("\007");
        }

        if (saveInCache) {
            // This row is OK to save into the cache.
            jexerCache.put(cells, sb.toString());
        }

        return (gotoXY(x, y) + sb.toString());
    }

    /**
     * Get the Casciian images support flag.
     *
     * @return true if this terminal is emitting Casciian images
     */
    public boolean hasJexerImages() {
        return (jexerImageOption != JexerImageOption.DISABLED);
    }

    // ------------------------------------------------------------------------
    // End Casciian image output support -----------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Setup system colors to match CGA color palette.
     */
    private static void setCGAColors() {
        MYBLACK = 0x000000;
        MYRED = 0xaa0000;
        MYGREEN = 0x00aa00;
        MYYELLOW = 0xaa5500;
        MYBLUE = 0x0000aa;
        MYMAGENTA = 0xaa00aa;
        MYCYAN = 0x00aaaa;
        MYWHITE = 0xaaaaaa;
        MYBOLD_BLACK = 0x555555;
        MYBOLD_RED = 0xff5555;
        MYBOLD_GREEN = 0x55ff55;
        MYBOLD_YELLOW = 0xffff55;
        MYBOLD_BLUE = 0x5555ff;
        MYBOLD_MAGENTA = 0xff55ff;
        MYBOLD_CYAN = 0x55ffff;
        MYBOLD_WHITE = 0xffffff;
        DEFAULT_FORECOLOR = MYWHITE;
        DEFAULT_BACKCOLOR = MYBLACK;
    }

    /**
     * Setup ECMA48 colors to match those provided in system properties.
     */
    private void setCustomSystemColors() {
        String previousCommand = buildSendPaletteCommand();

        MYBLACK = getCustomColor("casciian.ECMA48.color0", MYBLACK);
        MYRED = getCustomColor("casciian.ECMA48.color1", MYRED);
        MYGREEN = getCustomColor("casciian.ECMA48.color2", MYGREEN);
        MYYELLOW = getCustomColor("casciian.ECMA48.color3", MYYELLOW);
        MYBLUE = getCustomColor("casciian.ECMA48.color4", MYBLUE);
        MYMAGENTA = getCustomColor("casciian.ECMA48.color5", MYMAGENTA);
        MYCYAN = getCustomColor("casciian.ECMA48.color6", MYCYAN);
        MYWHITE = getCustomColor("casciian.ECMA48.color7", MYWHITE);
        MYBOLD_BLACK = getCustomColor("casciian.ECMA48.color8", MYBOLD_BLACK);
        MYBOLD_RED = getCustomColor("casciian.ECMA48.color9", MYBOLD_RED);
        MYBOLD_GREEN = getCustomColor("casciian.ECMA48.color10", MYBOLD_GREEN);
        MYBOLD_YELLOW = getCustomColor("casciian.ECMA48.color11", MYBOLD_YELLOW);
        MYBOLD_BLUE = getCustomColor("casciian.ECMA48.color12", MYBOLD_BLUE);
        MYBOLD_MAGENTA = getCustomColor("casciian.ECMA48.color13", MYBOLD_MAGENTA);
        MYBOLD_CYAN = getCustomColor("casciian.ECMA48.color14", MYBOLD_CYAN);
        MYBOLD_WHITE = getCustomColor("casciian.ECMA48.color15", MYBOLD_WHITE);

        DEFAULT_FORECOLOR = getCustomColor("casciian.ECMA48.color39",
            DEFAULT_FORECOLOR);
        DEFAULT_BACKCOLOR = getCustomColor("casciian.ECMA48.color49",
            DEFAULT_BACKCOLOR);

        if (!previousCommand.equals(buildSendPaletteCommand())) {
            sendPalette();
        }
    }

    /**
     * Setup one system color to match the RGB value provided in system
     * properties.
     *
     * @param key          the system property key
     * @param defaultColor the default color to return if key is not set, or
     *                     incorrect
     * @return a color from the RGB string, or defaultColor
     */
    private int getCustomColor(final String key, final int defaultColor) {

        String rgb = System.getProperty(key);
        if (rgb == null) {
            return defaultColor;
        }
        if (rgb.startsWith("#")) {
            rgb = rgb.substring(1);
        }
        int rgbInt = 0;
        try {
            rgbInt = Integer.parseInt(rgb, 16);
        } catch (NumberFormatException e) {
            return defaultColor;
        }
        return rgbInt;
    }

    /**
     * Convert a CellAttributes foreground color to an RGB color.
     *
     * @param attr the text attributes
     * @return the RGB color
     */
    public static int attrToForegroundColor(final CellAttributes attr) {
        if (attr.isDefaultColor(true)) {
            return getDefaultForeColorRGB();
        }

        int rgb = attr.getForeColorRGB();
        if (rgb >= 0) {
            return rgb;
        }

        int colorValue = attr.getForeColor().getValue();
        if (attr.isBold()) {
            return switch (colorValue) {
                case 0 -> MYBOLD_BLACK;  // Color.BLACK
                case 1 -> MYBOLD_RED;    // Color.RED
                case 2 -> MYBOLD_GREEN;  // Color.GREEN
                case 3 -> MYBOLD_YELLOW; // Color.YELLOW
                case 4 -> MYBOLD_BLUE;   // Color.BLUE
                case 5 -> MYBOLD_MAGENTA;// Color.MAGENTA
                case 6 -> MYBOLD_CYAN;   // Color.CYAN
                case 7 -> MYBOLD_WHITE;  // Color.WHITE
                default -> throw new IllegalArgumentException("Invalid color: " + colorValue);
            };
        }
        return switch (colorValue) {
            case 0 -> MYBLACK;   // Color.BLACK
            case 1 -> MYRED;     // Color.RED
            case 2 -> MYGREEN;   // Color.GREEN
            case 3 -> MYYELLOW;  // Color.YELLOW
            case 4 -> MYBLUE;    // Color.BLUE
            case 5 -> MYMAGENTA; // Color.MAGENTA
            case 6 -> MYCYAN;    // Color.CYAN
            case 7 -> MYWHITE;   // Color.WHITE
            default -> throw new IllegalArgumentException("Invalid color: " + colorValue);
        };
    }

    /**
     * Convert a CellAttributes background color to an RGB color.
     *
     * @param attr the text attributes
     * @return the RGB color
     */
    public static int attrToBackgroundColor(final CellAttributes attr) {
        if (attr.isDefaultColor(false)) {
            return getDefaultBackColorRGB();
        }

        int rgb = attr.getBackColorRGB();
        if (rgb >= 0) {
            return rgb;
        }

        return switch (attr.getBackColor().getValue()) {
            case 0 -> MYBLACK;   // Color.BLACK
            case 1 -> MYRED;     // Color.RED
            case 2 -> MYGREEN;   // Color.GREEN
            case 3 -> MYYELLOW;  // Color.YELLOW
            case 4 -> MYBLUE;    // Color.BLUE
            case 5 -> MYMAGENTA; // Color.MAGENTA
            case 6 -> MYCYAN;    // Color.CYAN
            case 7 -> MYWHITE;   // Color.WHITE
            default -> throw new IllegalArgumentException("Invalid color: " + attr.getBackColor().getValue());
        };
    }

    /**
     * Retrieve the default foreground color.
     *
     * @return the RGB color
     */
    public static int getDefaultForeColorRGB() {
        return DEFAULT_FORECOLOR;
    }

    /**
     * Retrieve the default background color.
     *
     * @return the RGB color
     */
    public static int getDefaultBackColorRGB() {
        return DEFAULT_BACKCOLOR;
    }

    /**
     * Create a T.416 RGB parameter sequence for a custom system color.
     *
     * @param colorRGB one of the MYBLACK, MYBOLD_BLUE, etc. colors
     * @return the color portion of the string to emit to an ANSI /
     * ECMA-style terminal
     */
    private String systemColorRGB(final int colorRGB) {
        int colorRed = (colorRGB >>> 16) & 0xFF;
        int colorGreen = (colorRGB >>> 8) & 0xFF;
        int colorBlue = colorRGB & 0xFF;

        return String.format("%d;%d;%d", colorRed, colorGreen, colorBlue);
    }

    /**
     * Create a SGR parameter sequence for a single color change.
     *
     * @param bold       if true, set bold
     * @param color      one of the Color.WHITE, Color.BLUE, etc. constants
     * @param foreground if true, this is a foreground color
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[42m"
     */
    private String color(final boolean bold, final Color color,
                         final boolean foreground) {
        return color(color, foreground, true) +
            rgbColor(bold, color, foreground);
    }

    /**
     * Create a T.416 RGB parameter sequence for a single color change.
     *
     * @param colorRGB   a 24-bit RGB value for foreground color
     * @param foreground if true, this is a foreground color
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[42m"
     */
    private String colorRGB(final int colorRGB, final boolean foreground) {

        int colorRed = (colorRGB >>> 16) & 0xFF;
        int colorGreen = (colorRGB >>> 8) & 0xFF;
        int colorBlue = colorRGB & 0xFF;

        StringBuilder sb = new StringBuilder();
        if (foreground) {
            sb.append("\033[38;2;");
        } else {
            sb.append("\033[48;2;");
        }
        sb.append(String.format("%d;%d;%dm", colorRed, colorGreen, colorBlue));
        return sb.toString();
    }

    /**
     * Create a T.416 RGB parameter sequence for both foreground and
     * background color change.
     *
     * @param foreColorRGB a 24-bit RGB value for foreground color
     * @param backColorRGB a 24-bit RGB value for foreground color
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[42m"
     */
    private String colorRGB(final int foreColorRGB, final int backColorRGB) {
        int foreColorRed = (foreColorRGB >>> 16) & 0xFF;
        int foreColorGreen = (foreColorRGB >>> 8) & 0xFF;
        int foreColorBlue = foreColorRGB & 0xFF;
        int backColorRed = (backColorRGB >>> 16) & 0xFF;
        int backColorGreen = (backColorRGB >>> 8) & 0xFF;
        int backColorBlue = backColorRGB & 0xFF;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\033[38;2;%d;%d;%dm",
            foreColorRed, foreColorGreen, foreColorBlue));
        sb.append(String.format("\033[48;2;%d;%d;%dm",
            backColorRed, backColorGreen, backColorBlue));
        return sb.toString();
    }

    /**
     * Get the palette color for a Color constant.
     *
     * @param color the Color constant
     * @param bold  if true, return the bold/bright variant
     * @return the RGB palette color value
     */
    private static int getPaletteColor(final Color color, final boolean bold) {
        return bold ? switch (color.getValue()) {
            case 0 -> MYBOLD_BLACK;
            case 1 -> MYBOLD_RED;
            case 2 -> MYBOLD_GREEN;
            case 3 -> MYBOLD_YELLOW;
            case 4 -> MYBOLD_BLUE;
            case 5 -> MYBOLD_MAGENTA;
            case 6 -> MYBOLD_CYAN;
            case 7 -> MYBOLD_WHITE;
            default -> MYBOLD_WHITE;
        } : switch (color.getValue()) {
            case 0 -> MYBLACK;
            case 1 -> MYRED;
            case 2 -> MYGREEN;
            case 3 -> MYYELLOW;
            case 4 -> MYBLUE;
            case 5 -> MYMAGENTA;
            case 6 -> MYCYAN;
            case 7 -> MYWHITE;
            default -> MYWHITE;
        };
    }

    /**
     * Create a T.416 RGB parameter sequence for a single color change.
     *
     * @param bold       if true, set bold
     * @param color      one of the Color.WHITE, Color.BLUE, etc. constants
     * @param foreground if true, this is a foreground color
     * @return the string to emit to an xterm terminal with RGB support,
     * e.g. "\033[38;2;RR;GG;BBm"
     */
    private String rgbColor(final boolean bold, final Color color,
                            final boolean foreground) {
        if (!doRgbColor) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\033[");
        if (bold) {
            // Bold implies foreground only
            sb.append("38;2;");
            sb.append(systemColorRGB(getPaletteColor(color, true)));
        } else {
            sb.append(foreground ? "38;2;" : "48;2;");
            sb.append(systemColorRGB(getPaletteColor(color, false)));
        }
        sb.append("m");
        return sb.toString();
    }

    /**
     * Create a T.416 RGB parameter sequence for both foreground and
     * background color change.
     *
     * @param bold      if true, set bold
     * @param foreColor one of the Color.WHITE, Color.BLUE, etc. constants
     * @param backColor one of the Color.WHITE, Color.BLUE, etc. constants
     * @return the string to emit to an xterm terminal with RGB support,
     * e.g. "\033[38;2;RR;GG;BB;48;2;RR;GG;BBm"
     */
    private String rgbColor(final boolean bold, final Color foreColor,
                            final Color backColor) {
        if (!doRgbColor) {
            return "";
        }

        return rgbColor(bold, foreColor, true) +
            rgbColor(false, backColor, false);
    }

    /**
     * Create a SGR parameter sequence for a single color change.
     *
     * @param color      one of the Color.WHITE, Color.BLUE, etc. constants
     * @param foreground if true, this is a foreground color
     * @param header     if true, make the full header, otherwise just emit the
     *                   color parameter e.g. "42;"
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[42m"
     */
    private String color(final Color color, final boolean foreground,
                         final boolean header) {

        return color(color, foreground, header, false);
    }

    /**
     * Create a SGR parameter sequence for a single color change.
     *
     * @param color      one of the Color.WHITE, Color.BLUE, etc. constants
     * @param foreground if true, this is a foreground color
     * @param header     if true, make the full header, otherwise just emit the
     *                   color parameter e.g. "42;"
     * @param bold       if true and foreground is true, use bright colors (90-97)
     *                   instead of normal colors (30-37). This is needed because some terminals
     *                   (e.g., Terminator, gnome-terminal) do not interpret SGR 1 (bold) as
     *                   switching to bright colors.
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[42m" or "\033[92m" for bright green
     */
    private String color(final Color color, final boolean foreground,
                         final boolean header, final boolean bold) {

        int ecmaColor = color.getValue();

        // Convert Color.* values to SGR numerics
        if (foreground) {
            if (bold) {
                // Use bright foreground colors (90-97) for bold text.
                // This is the AIXterm-style bright colors which are widely
                // supported and do not rely on SGR 1 to switch to bright colors.
                ecmaColor += 90;
            } else {
                ecmaColor += 30;
            }
        } else {
            ecmaColor += 40;
        }

        if (header) {
            return String.format("\033[%dm", ecmaColor);
        } else {
            return String.format("%d;", ecmaColor);
        }
    }

    /**
     * Create a SGR parameter sequence for both foreground and background
     * color change.
     *
     * @param bold      if true, set bold
     * @param foreColor one of the Color.WHITE, Color.BLUE, etc. constants
     * @param backColor one of the Color.WHITE, Color.BLUE, etc. constants
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[31;42m"
     */
    private String color(final boolean bold, final Color foreColor,
                         final Color backColor) {
        return color(foreColor, backColor, true) +
            rgbColor(bold, foreColor, backColor);
    }

    /**
     * Create a SGR parameter sequence for both foreground and
     * background color change.
     *
     * @param foreColor one of the Color.WHITE, Color.BLUE, etc. constants
     * @param backColor one of the Color.WHITE, Color.BLUE, etc. constants
     * @param header    if true, make the full header, otherwise just emit the
     *                  color parameter e.g. "31;42;"
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[31;42m"
     */
    private String color(final Color foreColor, final Color backColor,
                         final boolean header) {

        int ecmaForeColor = foreColor.getValue();
        int ecmaBackColor = backColor.getValue();

        // Convert Color.* values to SGR numerics
        ecmaBackColor += 40;
        ecmaForeColor += 30;

        if (header) {
            return String.format("\033[%d;%dm", ecmaForeColor, ecmaBackColor);
        } else {
            return String.format("%d;%d;", ecmaForeColor, ecmaBackColor);
        }
    }

    /**
     * Build the SGR attribute prefix string based on reverse, blink, and underline flags.
     * The returned string includes the ESC[ and ends with either 'm' (if terminated)
     * or a trailing separator for additional parameters.
     *
     * @param reverse    if true, include reverse attribute (7)
     * @param blink      if true, include blink attribute (5)
     * @param underline  if true, include underline attribute (4)
     * @param terminated if true, end with 'm'; otherwise end with ';' for more params
     * @return the SGR prefix string
     */
    private static String buildAttributePrefix(final boolean reverse, final boolean blink,
                                               final boolean underline, final boolean terminated) {
        StringBuilder sb = new StringBuilder("\033[0");
        if (reverse) sb.append(";7");
        if (blink) sb.append(";5");
        if (underline) sb.append(";4");
        sb.append(terminated ? "m" : ";");
        return sb.toString();
    }

    /**
     * Create a SGR parameter sequence for foreground, background, and
     * several attributes.  This sequence first resets all attributes to
     * default, then sets attributes as per the parameters.
     * <p>
     * Note: SGR 1 (bold) is NOT emitted because casciian uses bright colors
     * (90-97) to indicate bold instead. This avoids showing bold/thick text
     * on terminals that support it.
     *
     * @param foreColor one of the Color.WHITE, Color.BLUE, etc. constants
     * @param backColor one of the Color.WHITE, Color.BLUE, etc. constants
     * @param bold      if true, set bold
     * @param reverse   if true, set reverse
     * @param blink     if true, set blink
     * @param underline if true, set underline
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[0;31;42m"
     */
    private String color(final Color foreColor, final Color backColor,
                         final boolean bold, final boolean reverse, final boolean blink,
                         final boolean underline) {

        int ecmaForeColor = foreColor.getValue() + 30;
        int ecmaBackColor = backColor.getValue() + 40;

        return buildAttributePrefix(reverse, blink, underline, false)
            + String.format("%d;%dm", ecmaForeColor, ecmaBackColor)
            + rgbColor(bold, foreColor, backColor);
    }

    /**
     * Create a SGR parameter sequence for several attributes.  This sequence
     * first resets all attributes to default, then sets attributes as per
     * the parameters.
     * <p>
     * Note: SGR 1 (bold) is NOT emitted because casciian uses bright colors
     * (90-97) to indicate bold instead. This avoids showing bold/thick text
     * on terminals that support it.
     *
     * @param bold      if true, set bold
     * @param reverse   if true, set reverse
     * @param blink     if true, set blink
     * @param underline if true, set underline
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[0;5m"
     */
    private String attributes(final boolean bold, final boolean reverse,
                              final boolean blink, final boolean underline) {
        return buildAttributePrefix(reverse, blink, underline, true);
    }

    /**
     * Create a SGR parameter sequence for foreground, background, and
     * several attributes.  This sequence first resets all attributes to
     * default, then sets attributes as per the parameters.
     * <p>
     * Note: SGR 1 (bold) is NOT emitted because casciian uses bright colors
     * (90-97) to indicate bold instead. This avoids showing bold/thick text
     * on terminals that support it.
     *
     * @param foreColorRGB a 24-bit RGB value for foreground color
     * @param backColorRGB a 24-bit RGB value for foreground color
     * @param bold         if true, set bold
     * @param reverse      if true, set reverse
     * @param blink        if true, set blink
     * @param underline    if true, set underline
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[0;31;42m"
     */
    private String colorRGB(final int foreColorRGB, final int backColorRGB,
                            final boolean bold, final boolean reverse, final boolean blink,
                            final boolean underline) {

        int foreColorRed = (foreColorRGB >>> 16) & 0xFF;
        int foreColorGreen = (foreColorRGB >>> 8) & 0xFF;
        int foreColorBlue = foreColorRGB & 0xFF;
        int backColorRed = (backColorRGB >>> 16) & 0xFF;
        int backColorGreen = (backColorRGB >>> 8) & 0xFF;
        int backColorBlue = backColorRGB & 0xFF;

        return buildAttributePrefix(reverse, blink, underline, false)
            + String.format("m\033[38;2;%d;%d;%dm\033[48;2;%d;%d;%dm",
                foreColorRed, foreColorGreen, foreColorBlue,
                backColorRed, backColorGreen, backColorBlue);
    }

    /**
     * Create a SGR parameter sequence to reset to VT100 defaults.
     *
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[0m"
     */
    private String normal() {
        return normal(true) + rgbColor(false, Color.WHITE, Color.BLACK);
    }

    /**
     * Create a SGR parameter sequence to reset to ECMA-48 default
     * foreground/background.
     *
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[0m"
     */
    private String defaultColor() {
        /*
         * VT100 normal.
         * Normal (neither bold nor faint).
         * Not italicized.
         * Not underlined.
         * Steady (not blinking).
         * Positive (not inverse).
         * Visible (not hidden).
         * Not crossed-out.
         * Default foreground color.
         * Default background color.
         */
        return "\033[0;22;23;24;25;27;28;29;39;49m";
    }

    /**
     * Create a SGR parameter sequence to reset to defaults.
     *
     * @param header if true, make the full header, otherwise just emit the
     *               bare parameter e.g. "0;"
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[0m"
     */
    private String normal(final boolean header) {
        if (header) {
            return "\033[0;37;40m";
        }
        return "0;37;40";
    }

    /**
     * Create a SGR parameter sequence for enabling the visible cursor.
     *
     * @param on if true, turn on cursor
     * @return the string to emit to an ANSI / ECMA-style terminal
     */
    private String cursor(final boolean on) {
        if (on && !cursorOn) {
            cursorOn = true;
            return "\033[?25h";
        }
        if (!on && cursorOn) {
            cursorOn = false;
            return "\033[?25l";
        }
        return "";
    }

    /**
     * Clear the entire screen.  Because some terminals use back-color-erase,
     * set the color to white-on-black beforehand.
     *
     * @return the string to emit to an ANSI / ECMA-style terminal
     */
    private String clearAll() {
        return "\033[0;37;40m\033[2J";
    }

    /**
     * Clear the line from the cursor (inclusive) to the end of the screen.
     * Because some terminals use back-color-erase, set the color to
     * white-on-black beforehand.
     *
     * @return the string to emit to an ANSI / ECMA-style terminal
     */
    private String clearRemainingLine() {
        return "\033[0;37;40m\033[K";
    }

    /**
     * Move the cursor to (x, y).
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @return the string to emit to an ANSI / ECMA-style terminal
     */
    private String gotoXY(final int x, final int y) {
        return String.format("\033[%d;%dH", y + 1, x + 1);
    }

    /**
     * Move the cursor to (x, y).
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @return the string to emit to an ANSI / ECMA-style terminal
     */
    private String sortableGotoXY(final int x, final int y) {
        return String.format("\033[%02d;%02dH", y + 1, x + 1);
    }

    /**
     * Request (u)xterm report its screen size in cells.  Note package
     * private access.
     *
     * @return the string to emit to xterm
     */
    static String xtermQueryWindowSize() {
        return "\033[18t";
    }

    /**
     * Request (u)xterm report support for a specific mode.
     *
     * @param mode the mode to query
     * @return the string to emit to xterm
     */
    private String xtermQueryMode(final int mode) {
        if (mode > 0) {
            String str = String.format("\033[?%d$p", mode);
            if (debugToStderr) {
                System.err.printf("Sending DECRQM: %s\n", str);
            }
            return str;
        }
        return "";
    }

    /**
     * Request (u)xterm report the RGB values of its ANSI colors.
     *
     * @return the string to emit to xterm
     */
    private String xtermQueryAnsiColors() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append("\033]4;%d;?\033\\".formatted(i));
        }
        sb.append("\033]10;?\033\\");
        sb.append("\033]11;?\033\\");
        return sb.toString();
    }

    /**
     * Set xterm mouse pointer shape using OSC 22.
     *
     * @param shape the pointer shape name (e.g., "arrow", "xterm")
     */
    private void setXtermMousePointer(final String shape) {
        if (output != null) {
            mousePointerShapeChanged = true;
            // OSC 22 ; shape ST - set pointer shape
            output.printf("\033]%s;%s\033\\", OSC_POINTER_SHAPE, shape);
            output.flush();
            if (debugToStderr) {
                System.err.println("Set pointer shape to: " + shape);
            }
        }
    }

    /**
     * Restore the default xterm mouse pointer shape.
     * This is called when the terminal is closed.
     */
    private void restoreXtermMousePointer() {
        if (mousePointerShapeChanged && output != null) {
            setXtermMousePointer(POINTER_SHAPE_DEFAULT);
            mousePointerShapeChanged = false;
            if (debugToStderr) {
                System.err.println("Restored pointer shape to: " + POINTER_SHAPE_DEFAULT);
            }
        }
    }

    /**
     * Tell (u)xterm that we want to copy text to the system clipboard via
     * OSC 52.
     *
     * @param text string to copy
     */
    public void xtermSetClipboardText(final String text) {
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        String textToCopy = StringUtils.toBase64(textBytes);
        this.output.printf("\033]52;c;%s\033\\", textToCopy);
        this.output.flush();
    }

    /**
     * Get the rgbColor flag.
     *
     * @return true if the standard system colors will be emitted as 24-bit RGB
     */
    public boolean isRgbColor() {
        return doRgbColor;
    }

    /**
     * Set the rgbColor flag.
     *
     * @param rgbColor if true, the standard system colors will be emitted as
     *                 24-bit RGB images
     */
    public void setRgbColor(final boolean rgbColor) {
        doRgbColor = rgbColor;
    }

    /**
     * Send OSC 4 sequences to set the entire terminal palette.
     * Uses the {@code MY*} color constants which are already initialized by
     * {@link #setCGAColors()} or {@link #setCustomSystemColors()}.
     * <p>
     * This method unconditionally emits the palette update when invoked; it does
     * not itself consult any system properties. The decision to call this method
     * is typically controlled by the {@code useTerminalPalette} system property
     * (via {@code SystemProperties.isUseTerminalPalette()}) in the constructors
     * or other callers.
     */
    private void sendPalette() {
        if (output == null) {
            if (debugToStderr) {
                System.err.println("sendPalette(): output is null, skipping palette transmission");
            }
            return;
        }

        String command = buildSendPaletteCommand();

        output.write(command);
        output.flush();

        if (debugToStderr) {
            System.err.println("Sent CGA palette (16 colors) to terminal");
        }
    }

    /**
     * Build the OSC 4 command sequence used to program the terminal's
     * 16-color palette.
     * <p>
     * This method uses the {@code MY*} color constants (which are expected
     * to have been initialized by {@code setCGAColors()} or
     * {@code setCustomSystemColors()}) and generates, for each palette
     * index 0–15, an OSC 4 sequence of the form:
     * </p>
     * <pre>
     *   ESC ] 4 ; index ; rgb:rrrr/gggg/bbbb ESC \
     * </pre>
     * where {@code rrrr}, {@code gggg}, and {@code bbbb} are 16-bit
     * (4-hex-digit) color components. Each 8-bit RGB component in the
     * {@code MY*} constants is expanded to 16 bits by repeating its
     * two-digit hexadecimal value (for example, {@code 0x12} becomes
     * {@code 0x1212}).
     *
     * @return a single {@link String} containing the concatenated OSC 4
     * sequences for all 16 palette entries, ready to be written to
     * the terminal output stream
     */
    private static String buildSendPaletteCommand() {

        // Palette colors using the MY* constants
        int[] colors = {
            MYBLACK,        // 0: Black
            MYRED,          // 1: Red
            MYGREEN,        // 2: Green
            MYYELLOW,       // 3: Yellow (brown)
            MYBLUE,         // 4: Blue
            MYMAGENTA,      // 5: Magenta
            MYCYAN,         // 6: Cyan
            MYWHITE,        // 7: White (light gray)
            MYBOLD_BLACK,   // 8: Bright Black (dark gray)
            MYBOLD_RED,     // 9: Bright Red
            MYBOLD_GREEN,   // 10: Bright Green
            MYBOLD_YELLOW,  // 11: Bright Yellow
            MYBOLD_BLUE,    // 12: Bright Blue
            MYBOLD_MAGENTA, // 13: Bright Magenta
            MYBOLD_CYAN,    // 14: Bright Cyan
            MYBOLD_WHITE    // 15: Bright White
        };

        // Pre-size to avoid internal resizing: ~50 bytes/color * 16 colors ≈ 800 bytes
        StringBuilder sb = new StringBuilder(800);
        for (int i = 0; i < colors.length; i++) {
            sb.append(buildColorOscSequence(i, colors[i]));
        }
        return sb.toString();
    }

    /**
     * Build a single OSC 4 sequence for setting a terminal palette color.
     * <p>
     * The generated sequence has the form:
     * </p>
     * <pre>
     *   ESC ] 4 ; index ; rgb:rrrr/gggg/bbbb ESC \
     * </pre>
     * where {@code rrrr}, {@code gggg}, and {@code bbbb} are 16-bit
     * (4-hex-digit) color components. Each 8-bit RGB component is expanded
     * to 16 bits by repeating its two-digit hexadecimal value (for example,
     * {@code 0x12} becomes {@code 0x1212}).
     *
     * @param index the palette index (0-15)
     * @param color the 24-bit RGB color value
     * @return the OSC 4 sequence string for this color
     */
    private static String buildColorOscSequence(final int index, final int color) {
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;

        // Format: OSC 4 ; color-index ; rgb:rrrr/gggg/bbbb ST
        // Using 16-bit format (4 hex digits per component) for compatibility
        // Using argument_index to send each component only once to the formatter
        return "\033]4;%1$d;rgb:%2$02x%2$02x/%3$02x%3$02x/%4$02x%4$02x\033\\"
            .formatted(index, red, green, blue);
    }

}
