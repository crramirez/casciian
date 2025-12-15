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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import casciian.bits.Cell;
import casciian.bits.CellAttributes;
import casciian.bits.Color;
import casciian.bits.ComplexCell;
import casciian.bits.ExtendedGraphemeClusterUtils;
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
    private boolean stopReaderThread;

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
     * If true, then we changed System.in and need to change it back.
     */
    private boolean setRawMode = false;

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
     * The terminal's raw InputStream.  If an InputStream is not specified in
     * the constructor, then this InputReader will be bound to System.in.
     * This is used by run() to see if bytes are available() before calling
     * (Reader)input.read().
     */
    private InputStream inputStream;

    /**
     * The terminal's output.  If an OutputStream is not specified in the
     * constructor, then this PrintWriter will be bound to System.out with
     * UTF-8 encoding.
     */
    private PrintWriter output;

    /**
     * The listening object that run() wakes up on new input.
     */
    private Object listener;

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

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Static constructor.
     */
    static {
        setDOSColors();
    }

    /**
     * Constructor sets up state for getEvent().  If either windowWidth or
     * windowHeight are less than 1, the terminal is not resized.
     *
     * @param backend the backend that will read from this terminal
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param input an InputStream connected to the remote user, or null for
     * System.in.  If System.in is used, then on non-Windows systems it will
     * be put in raw mode; closeTerminal() will (blindly!) put System.in in
     * cooked mode.  input is always converted to a Reader with UTF-8
     * encoding.
     * @param output an OutputStream connected to the remote user, or null
     * for System.out.  output is always converted to a Writer with UTF-8
     * encoding.
     * @param windowWidth the number of text columns to start with
     * @param windowHeight the number of text rows to start with
     * @throws UnsupportedEncodingException if an exception is thrown when
     * creating the InputStreamReader
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
     * @param backend the backend that will read from this terminal
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param input an InputStream connected to the remote user, or null for
     * System.in.  If System.in is used, then on non-Windows systems it will
     * be put in raw mode; closeTerminal() will (blindly!) put System.in in
     * cooked mode.  input is always converted to a Reader with UTF-8
     * encoding.
     * @param output an OutputStream connected to the remote user, or null
     * for System.out.  output is always converted to a Writer with UTF-8
     * encoding.
     * @throws UnsupportedEncodingException if an exception is thrown when
     * creating the InputStreamReader
     */
    @SuppressWarnings("this-escape")
    public ECMA48Terminal(final Backend backend, final Object listener,
        final InputStream input,
        final OutputStream output) throws UnsupportedEncodingException {

        this.backend     = backend;

        resetParser();
        mouse1           = false;
        mouse2           = false;
        mouse3           = false;
        stopReaderThread = false;
        this.listener    = listener;

        if (input == null) {
            // inputStream = System.in;
            inputStream = new FileInputStream(FileDescriptor.in);
            sttyRaw();
            setRawMode = true;
        } else {
            inputStream = input;
        }
        this.input = new InputStreamReader(inputStream, "UTF-8");

        if (input instanceof SessionInfo) {
            // This is a TelnetInputStream that exposes window size and
            // environment variables from the telnet layer.
            sessionInfo = (SessionInfo) input;
        }
        if (sessionInfo == null) {
            if (input == null) {
                // Reading right off the tty
                sessionInfo = new TTYSessionInfo();
            } else {
                sessionInfo = new TSessionInfo();
            }
        }

        if (output == null) {
            this.output = new PrintWriter(new OutputStreamWriter(System.out,
                    "UTF-8"));
        } else {
            this.output = new PrintWriter(new OutputStreamWriter(output,
                    "UTF-8"));
        }

        // Request xterm version.  Due to the ambiguity between the response
        // and Alt-P, this must be the first thing to request.
        this.output.printf("%s", xtermReportVersion());

        // Request Device Attributes
        this.output.printf("\033[c");

        // Enable mouse reporting and metaSendsEscape
        this.output.printf("%s%s", mouse(true), xtermMetaSendsEscape(true));

        // Request xterm report Synchronized Output support
        this.output.printf("%s", xtermQueryMode(2026));

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
     * @param backend the backend that will read from this terminal
     * @param listener the object this backend needs to wake up when new
     * input comes in
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

        this.backend     = backend;

        resetParser();

        mouse1           = false;
        mouse2           = false;
        mouse3           = false;
        stopReaderThread = false;
        this.listener    = listener;

        inputStream = input;
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
                sessionInfo = new TTYSessionInfo();
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

        // Enable mouse reporting and metaSendsEscape
        this.output.printf("%s%s", mouse(true), xtermMetaSendsEscape(true));

        // Request xterm report Synchronized Output support
        this.output.printf("%s", xtermQueryMode(2026));

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
     * @param backend the backend that will read from this terminal
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param input the InputStream underlying 'reader'.  Its available()
     * method is used to determine if reader.read() will block or not.
     * @param reader a Reader connected to the remote user.
     * @param writer a PrintWriter connected to the remote user.
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

        // Disable mouse reporting and show cursor.  Defensive null check
        // here in case closeTerminal() is called twice.
        if (output != null) {
            output.printf("%s%s%s", mouse(false), cursor(true),
                defaultColor());
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
     * Reload options from System properties.
     */
    public void reloadOptions() {
        if (debugToStderr) {
            System.err.println("reloadOptions()");
        }

        // Permit RGB colors only if externally requested.
        if (System.getProperty("casciian.ECMA48.modifyOtherKeys",
                "false").equals("true")
        ) {
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
        if (System.getProperty("casciian.textBlink",
                "on").toLowerCase().equals("off")
        ) {
            textBlinkOption = TextBlinkOption.OFF;
        }

        long millis = 500;
        try {
            String milliStr = System.getProperty("casciian.blinkMillis", "500");
            millis = Integer.parseInt(milliStr);
        } catch (NumberFormatException e) {
            // SQUASH
        }
        if (millis <= 0) {
            textBlinkOption = TextBlinkOption.OFF;
        } else {
            blinkMillis = millis;
        }

        int dimPercent = 80;
        try {
            String dimPercentStr = System.getProperty("casciian.blinkDimPercent",
                "80");
            dimPercent = Integer.parseInt(dimPercentStr);
        } catch (NumberFormatException e) {
            // SQUASH
        }
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
        char [] readBuffer = new char[128];
        List<TInputEvent> events = new ArrayList<TInputEvent>();

        // boolean debugToStderr = true;

        while (!done && !stopReaderThread) {
            try {
                // We assume that if inputStream has bytes available, then
                // input won't block on read().
                if (debugToStderr) {
                    System.err.printf("Looking for input...");
                }

                int n = inputStream.available();

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

                    int rc = input.read(readBuffer, 0, readBuffer.length);

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
                            processChar(events, (char)ch);
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
     * blink from visible to invisible
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
     * Call 'stty' to set cooked mode.
     *
     * <p>Actually executes '/bin/sh -c stty sane cooked &lt; /dev/tty'
     */
    private void sttyCooked() {
        doStty(false);
    }

    /**
     * Call 'stty' to set raw mode.
     *
     * <p>Actually executes '/bin/sh -c stty -ignbrk -brkint -parmrk -istrip
     * -inlcr -igncr -icrnl -ixon -opost -echo -echonl -icanon -isig -iexten
     * -parenb cs8 min 1 &lt; /dev/tty'
     */
    private void sttyRaw() {
        doStty(true);
    }

    /**
     * Call 'stty' to set raw or cooked mode.
     *
     * @param mode if true, set raw mode, otherwise set cooked mode
     */
    private void doStty(final boolean mode) {
        String [] cmdRaw = {
            "/bin/sh", "-c", "stty -ignbrk -brkint -parmrk -istrip -inlcr -igncr -icrnl -ixon -opost -echo -echonl -icanon -isig -iexten -parenb cs8 min 1 < /dev/tty"
        };
        String [] cmdCooked = {
            "/bin/sh", "-c", "stty sane cooked < /dev/tty"
        };
        try {
            Process process;
            if (mode) {
                process = Runtime.getRuntime().exec(cmdRaw);
            } else {
                process = Runtime.getRuntime().exec(cmdCooked);
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line = in.readLine();
            if ((line != null) && (line.length() > 0)) {
                System.err.println("WEIRD?! Normal output from stty: " + line);
            }
            while (true) {
                BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"));
                line = err.readLine();
                if ((line != null) && (line.length() > 0)) {
                    System.err.println("Error output from stty: " + line);
                }
                try {
                    process.waitFor();
                    break;
                } catch (InterruptedException e) {
                    if (debugToStderr) {
                        e.printStackTrace();
                    }
                }
            }
            int rc = process.exitValue();
            if (rc != 0) {
                System.err.println("stty returned error code: " + rc);
            }
        } catch (IOException e) {
            e.printStackTrace();
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
     * @param y row coordinate.  0 is the top-most row.
     * @param sb StringBuilder to write escape sequences to
     * @param lastAttr cell attributes from the last call to flushLine
     */
    private void flushLine(final int y, final StringBuilder sb,
        CellAttributes lastAttr) {

        int lastX = -1;
        int textEnd = 0;
        for (int x = 0; x < width; x++) {
            Cell lCell = logical[x][y];
            if (!lCell.isBlank()) {
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
                    if (debugToStderr && reallyDebug) {
                        System.err.println("1 gotoXY() " + x + " " + y +
                            " lastX " + lastX);
                    }
                    // Advancing at least one cell, or the first gotoXY
                    sb.append(gotoXY(x, y));
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

                if (debugToStderr && reallyDebug) {
                    System.err.println("3 gotoXY() " + x + " " + y +
                        " lastX " + lastX);
                }
                sb.append(gotoXY(x, y));

                // Now emit only the modified attributes
                StringBuilder attrSgr = new StringBuilder(8);
                if (lCell.isBold() != lastAttr.isBold()) {
                    if (lCell.isBold()) {
                        attrSgr.append(";1");
                    } else {
                        attrSgr.append(";22");
                    }
                }
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
                    if (foreColorRGB != lastForeColorRGB) {
                        doForeColorRGB = true;
                    }
                }
                if (doForeColorRGB
                    || ((lCell.getForeColorRGB() >= 0)
                        && ((lCell.getForeColorRGB() != lastAttr.getForeColorRGB())
                            || (lastAttr.getForeColorRGB() < 0)))
                ) {
                    if (debugToStderr && reallyDebug) {
                        System.err.println("3 set foreColorRGB");
                    }
                    sb.append(colorRGB(foreColorRGB, true));
                } else {
                    if ((lCell.getForeColorRGB() < 0)
                        && ((lastAttr.getForeColorRGB() >= 0)
                            || !lCell.getForeColor().equals(lastAttr.getForeColor()))
                    ) {
                        if (debugToStderr && reallyDebug) {
                            System.err.println("4 set foreColor");
                        }
                        sb.append(color(lCell.getForeColor(), true, true));
                    }
                }

                if ((lCell.getBackColorRGB() >= 0)
                    && ((lCell.getBackColorRGB() != lastAttr.getBackColorRGB())
                        || (lastAttr.getBackColorRGB() < 0))
                ) {
                    if (debugToStderr && reallyDebug) {
                        System.err.println("5 set backColorRGB");
                    }
                    sb.append(colorRGB(lCell.getBackColorRGB(), false));
                } else {
                    if ((lCell.getBackColorRGB() < 0)
                        && ((lastAttr.getBackColorRGB() >= 0)
                            || !lCell.getBackColor().equals(lastAttr.getBackColor()))
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
     * @param ch Unicode code point
     * @param alt if true, set alt on the TKeypress
     * @return one TKeypress event, either a control character (e.g. isKey ==
     * false, ch == 'A', ctrl == true), or a special key (e.g. isKey == true,
     * fnKey == ESC)
     */
    private TKeypressEvent controlChar(final char ch, final boolean alt) {
        // System.err.printf("controlChar: %02x\n", ch);

        switch (ch) {
        case 0x0D:
            // Carriage return --> ENTER
            return new TKeypressEvent(backend, kbEnter, alt, false, false);
        case 0x0A:
            // Linefeed --> ENTER
            return new TKeypressEvent(backend, kbEnter, alt, false, false);
        case 0x1B:
            // ESC
            return new TKeypressEvent(backend, kbEsc, alt, false, false);
        case '\t':
            // TAB
            return new TKeypressEvent(backend, kbTab, alt, false, false);
        default:
            // Make all other control characters come back as the alphabetic
            // character with the ctrl field set.  So SOH would be 'A' +
            // ctrl.
            return new TKeypressEvent(backend, false, 0, (char)(ch + 0x40),
                alt, true, false);
        }
    }

    /**
     * Produce special key from CSI Pn ; Pm ; ... ~
     *
     * @return one KEYPRESS event representing a special key
     */
    private TInputEvent csiFnKey() {
        int key = 0;
        if (params.size() > 0) {
            key = Integer.parseInt(params.get(0));
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

        switch (key) {
        case 1:
            return new TKeypressEvent(backend, kbHome, alt, ctrl, shift);
        case 2:
            return new TKeypressEvent(backend, kbIns, alt, ctrl, shift);
        case 3:
            return new TKeypressEvent(backend, kbDel, alt, ctrl, shift);
        case 4:
            return new TKeypressEvent(backend, kbEnd, alt, ctrl, shift);
        case 5:
            return new TKeypressEvent(backend, kbPgUp, alt, ctrl, shift);
        case 6:
            return new TKeypressEvent(backend, kbPgDn, alt, ctrl, shift);
        case 15:
            return new TKeypressEvent(backend, kbF5, alt, ctrl, shift);
        case 17:
            return new TKeypressEvent(backend, kbF6, alt, ctrl, shift);
        case 18:
            return new TKeypressEvent(backend, kbF7, alt, ctrl, shift);
        case 19:
            return new TKeypressEvent(backend, kbF8, alt, ctrl, shift);
        case 20:
            return new TKeypressEvent(backend, kbF9, alt, ctrl, shift);
        case 21:
            return new TKeypressEvent(backend, kbF10, alt, ctrl, shift);
        case 23:
            return new TKeypressEvent(backend, kbF11, alt, ctrl, shift);
        case 24:
            return new TKeypressEvent(backend, kbF12, alt, ctrl, shift);

        case 27:
            // modifyOtherKeys sequence
            switch (otherKey) {
            case 8:
                return new TKeypressEvent(backend, kbBackspace, alt, ctrl, shift);
            case 9:
                return new TKeypressEvent(backend, kbTab, alt, ctrl, shift);
            case 13:
                return new TKeypressEvent(backend, kbEnter, alt, ctrl, shift);
            case 27:
                return new TKeypressEvent(backend, kbEsc, alt, ctrl, shift);
            default:
                if (otherKey < 32) {
                    break;
                }
                if ((otherKey >= 'a') && (otherKey <= 'z') && ctrl) {
                    // Turn Ctrl-lowercase into Ctrl-uppercase
                    return new TKeypressEvent(backend, false, 0, (otherKey - 32),
                        alt, ctrl, shift);
                }
                return new TKeypressEvent(backend, false, 0, otherKey,
                    alt, ctrl, shift);
            }

            // Unsupported other key
            return null;

        default:
            // Unknown
            return null;
        }
    }

    /**
     * Produce mouse events based on "Any event tracking" and UTF-8
     * coordinates.  See
     * http://invisible-island.net/xterm/ctlseqs/ctlseqs.html#Mouse%20Tracking
     *
     * @return a MOUSE_MOTION, MOUSE_UP, or MOUSE_DOWN event
     */
    private TInputEvent parseMouse() {
        int buttons = params.get(0).charAt(0) - 32;
        int x = params.get(0).charAt(1) - 32 - 1;
        int y = params.get(0).charAt(2) - 32 - 1;

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
        boolean eventAlt = false;
        boolean eventCtrl = false;
        boolean eventShift = false;

        // System.err.printf("buttons: %04x\r\n", buttons);

        switch (buttons & 0xE3) {
        case 0:
            eventMouse1 = true;
            mouse1 = true;
            break;
        case 1:
            eventMouse2 = true;
            mouse2 = true;
            break;
        case 2:
            eventMouse3 = true;
            mouse3 = true;
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

        case 33:
            // Dragging with mouse2 down
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

        case 96:
            // Dragging with mouse2 down after wheelUp
            eventMouse2 = true;
            mouse2 = true;
            eventType = TMouseEvent.Type.MOUSE_MOTION;
            break;

        case 97:
            // Dragging with mouse2 down after wheelDown
            eventMouse2 = true;
            mouse2 = true;
            eventType = TMouseEvent.Type.MOUSE_MOTION;
            break;

        case 64:
            eventMouseWheelUp = true;
            break;

        case 65:
            eventMouseWheelDown = true;
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

        return new TMouseEvent(backend, eventType, x, y, x, y,
            eventMouse1, eventMouse2, eventMouse3,
            eventMouseWheelUp, eventMouseWheelDown,
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
        boolean eventAlt = false;
        boolean eventCtrl = false;
        boolean eventShift = false;

        if (release) {
            eventType = TMouseEvent.Type.MOUSE_UP;
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

        case 33:
            // Dragging with mouse2 down
            eventMouse2 = true;
            eventType = TMouseEvent.Type.MOUSE_MOTION;
            break;

        case 34:
            // Dragging with mouse3 down
            eventMouse3 = true;
            eventType = TMouseEvent.Type.MOUSE_MOTION;
            break;

        case 96:
            // Dragging with mouse2 down after wheelUp
            eventMouse2 = true;
            eventType = TMouseEvent.Type.MOUSE_MOTION;
            break;

        case 97:
            // Dragging with mouse2 down after wheelDown
            eventMouse2 = true;
            eventType = TMouseEvent.Type.MOUSE_MOTION;
            break;

        case 64:
            eventMouseWheelUp = true;
            break;

        case 65:
            eventMouseWheelDown = true;
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

            if (sessionInfo instanceof TTYSessionInfo) {
                if (((TTYSessionInfo) sessionInfo).output != null) {
                    // If we are using CSI 18 t, the new dimensions will come
                    // later.
                    useStty = false;
                }
            }
            sessionInfo.queryWindowSize();

            if (useStty) {
                int newWidth = sessionInfo.getWindowWidth();
                int newHeight = sessionInfo.getWindowHeight();

                if ((newWidth != windowResize.getWidth())
                    || (newHeight != windowResize.getHeight())
                ) {
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
                queue.add(controlChar((char)0x1B, false));
                resetParser();
            }
        }
    }

    /**
     * Returns true if the CSI parameter for a keyboard command means that
     * shift was down.
     */
    private boolean csiIsShift(final String x) {
        if ((x.equals("2"))
            || (x.equals("4"))
            || (x.equals("6"))
            || (x.equals("8"))
        ) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if the CSI parameter for a keyboard command means that
     * alt was down.
     */
    private boolean csiIsAlt(final String x) {
        if ((x.equals("3"))
            || (x.equals("4"))
            || (x.equals("7"))
            || (x.equals("8"))
        ) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if the CSI parameter for a keyboard command means that
     * ctrl was down.
     */
    private boolean csiIsCtrl(final String x) {
        if ((x.equals("5"))
            || (x.equals("6"))
            || (x.equals("7"))
            || (x.equals("8"))
        ) {
            return true;
        }
        return false;
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
        }

    }

    /**
     * Process an OSC response.
     *
     * @param text the OSC response string
     */
    private void oscResponse(final String text) {
        if (debugToStderr) {
            System.err.println("oscResponse(): '" + text + "'");
        }

        String [] Ps = text.split(";");
        if (Ps.length == 0) {
            return;
        }
        if (Ps[0].equals("4")) {
            // RGB response
            if (Ps.length != 3) {
                return;
            }
            try {
                int color = Integer.parseInt(Ps[1]);
                String rgb = Ps[2];
                if (!rgb.startsWith("rgb:")) {
                    return;
                }
                rgb = rgb.substring(4);
                if (debugToStderr) {
                    System.err.println("  Color " + color + " is " + rgb);
                }
                String [] rgbs = rgb.split("/");
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
                switch (color) {
                case 0:
                    MYBLACK   = rgbColor;
                    if (debugToStderr) {
                        System.err.println("    Set BLACK");
                    }
                    break;
                case 1:
                    MYRED     = rgbColor;
                    if (debugToStderr) {
                        System.err.println("    Set RED");
                    }
                    break;
                case 2:
                    MYGREEN   = rgbColor;
                    if (debugToStderr) {
                        System.err.println("    Set GREEN");
                    }
                    break;
                case 3:
                    MYYELLOW  = rgbColor;
                    if (debugToStderr) {
                        System.err.println("    Set YELLOW");
                    }
                    break;
                case 4:
                    MYBLUE    = rgbColor;
                    if (debugToStderr) {
                        System.err.println("    Set BLUE");
                    }
                    break;
                case 5:
                    MYMAGENTA = rgbColor;
                    if (debugToStderr) {
                        System.err.println("    Set MAGENTA");
                    }
                    break;
                case 6:
                    MYCYAN    = rgbColor;
                    if (debugToStderr) {
                        System.err.println("    Set CYAN");
                    }
                    break;
                case 7:
                    MYWHITE   = rgbColor;
                    if (debugToStderr) {
                        System.err.println("    Set WHITE");
                    }
                    break;
                case 8:
                    MYBOLD_BLACK   = rgbColor;
                    if (debugToStderr) {
                        System.err.println("    Set BOLD BLACK");
                    }
                    break;
                case 9:
                    MYBOLD_RED     = rgbColor;
                    if (debugToStderr) {
                        System.err.println("    Set BOLD RED");
                    }
                    break;
                case 10:
                    MYBOLD_GREEN   = rgbColor;
                    if (debugToStderr) {
                        System.err.println("    Set BOLD GREEN");
                    }
                    break;
                case 11:
                    MYBOLD_YELLOW  = rgbColor;
                    if (debugToStderr) {
                        System.err.println("    Set BOLD YELLOW");
                    }
                    break;
                case 12:
                    MYBOLD_BLUE    = rgbColor;
                    if (debugToStderr) {
                        System.err.println("    Set BOLD BLUE");
                    }
                    break;
                case 13:
                    MYBOLD_MAGENTA = rgbColor;
                    if (debugToStderr) {
                        System.err.println("    Set BOLD MAGENTA");
                    }
                    break;
                case 14:
                    MYBOLD_CYAN    = rgbColor;
                    if (debugToStderr) {
                        System.err.println("    Set BOLD CYAN");
                    }
                    break;
                case 15:
                    MYBOLD_WHITE   = rgbColor;
                    if (debugToStderr) {
                        System.err.println("    Set BOLD WHITE");
                    }
                    break;
                default:
                    break;
                }

                // We have changed a system color.  Redraw the entire screen.
                clearPhysical();
                reallyCleared = true;
            } catch (NumberFormatException e) {
                return;
            }
        }

    }

    /**
     * Parses the next character of input to see if an InputEvent is
     * fully here.
     *
     * @param events list to append new events to
     * @param ch Unicode code point
     */
    private void processChar(final List<TInputEvent> events, final char ch) {

        // ESCDELAY type timeout
        long nowTime = System.currentTimeMillis();
        if (state == ParseState.ESCAPE) {
            long escDelay = nowTime - escapeTime;
            if (escDelay > 250) {
                // After 0.25 seconds, assume a true escape character
                events.add(controlChar((char)0x1B, false));
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
            }

            if (ch >= 0x20) {
                // Normal character
                events.add(new TKeypressEvent(backend, false, 0, ch,
                        false, false, false));
                resetParser();
                return;
            }

            break;

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
                switch (ch) {
                case 'A':
                    // Up
                    events.add(new TKeypressEvent(backend, kbUp, alt, ctrl, shift));
                    resetParser();
                    return;
                case 'B':
                    // Down
                    events.add(new TKeypressEvent(backend, kbDown, alt, ctrl, shift));
                    resetParser();
                    return;
                case 'C':
                    // Right
                    events.add(new TKeypressEvent(backend, kbRight, alt, ctrl, shift));
                    resetParser();
                    return;
                case 'D':
                    // Left
                    events.add(new TKeypressEvent(backend, kbLeft, alt, ctrl, shift));
                    resetParser();
                    return;
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
                    break;
                case 'c':
                    // Device Attributes
                    if (decPrivateModeFlag == false) {
                        break;
                    }
                    daResponseSeen = true;

                    for (String x: params) {
                        if (debugToStderr) {
                            System.err.println("Device Attributes: x = " + x);
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

                    }
                    resetParser();
                    return;
                case 't':
                    // windowOps
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
     * Request (u)xterm to report its program version (XTVERSION).
     *
     * I am not a fan of fingerprinting terminals in this fashion.  They
     * should instead be reporting their features in DA1 using one of the
     * available ~10000 unused IDs out there.  It is also bad because the
     * string returned looks like "Alt-P | {other text} ST", which is
     * completely valid keyboard input, hence the boolean to bypass Alt-P
     * processing IF the response comes in AND this has to be the FIRST thing
     * we send to the terminal.
     *
     * Alas, fingerprinting is now the path of least resistance.
     *
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
     * Setup system colors to match DOS color palette.
     */
    private static void setDOSColors() {
        MYBLACK         = 0x000000;
        MYRED           = 0xa80000;
        MYGREEN         = 0x00a800;
        MYYELLOW        = 0xa85400;
        MYBLUE          = 0x0000a8;
        MYMAGENTA       = 0xa800a8;
        MYCYAN          = 0x00a8a8;
        MYWHITE         = 0xa8a8a8;
        MYBOLD_BLACK    = 0x545454;
        MYBOLD_RED      = 0xfc5454;
        MYBOLD_GREEN    = 0x54fc54;
        MYBOLD_YELLOW   = 0xfcfc54;
        MYBOLD_BLUE     = 0x5454fc;
        MYBOLD_MAGENTA  = 0xfc54fc;
        MYBOLD_CYAN     = 0x54fcfc;
        MYBOLD_WHITE    = 0xfcfcfc;
    }

    /**
     * Setup ECMA48 colors to match those provided in system properties.
     */
    private void setCustomSystemColors() {
        MYBLACK   = getCustomColor("casciian.ECMA48.color0", MYBLACK);
        MYRED     = getCustomColor("casciian.ECMA48.color1", MYRED);
        MYGREEN   = getCustomColor("casciian.ECMA48.color2", MYGREEN);
        MYYELLOW  = getCustomColor("casciian.ECMA48.color3", MYYELLOW);
        MYBLUE    = getCustomColor("casciian.ECMA48.color4", MYBLUE);
        MYMAGENTA = getCustomColor("casciian.ECMA48.color5", MYMAGENTA);
        MYCYAN    = getCustomColor("casciian.ECMA48.color6", MYCYAN);
        MYWHITE   = getCustomColor("casciian.ECMA48.color7", MYWHITE);
        MYBOLD_BLACK   = getCustomColor("casciian.ECMA48.color8", MYBOLD_BLACK);
        MYBOLD_RED     = getCustomColor("casciian.ECMA48.color9", MYBOLD_RED);
        MYBOLD_GREEN   = getCustomColor("casciian.ECMA48.color10", MYBOLD_GREEN);
        MYBOLD_YELLOW  = getCustomColor("casciian.ECMA48.color11", MYBOLD_YELLOW);
        MYBOLD_BLUE    = getCustomColor("casciian.ECMA48.color12", MYBOLD_BLUE);
        MYBOLD_MAGENTA = getCustomColor("casciian.ECMA48.color13", MYBOLD_MAGENTA);
        MYBOLD_CYAN    = getCustomColor("casciian.ECMA48.color14", MYBOLD_CYAN);
        MYBOLD_WHITE   = getCustomColor("casciian.ECMA48.color15", MYBOLD_WHITE);
    }

    /**
     * Setup one system color to match the RGB value provided in system
     * properties.
     *
     * @param key the system property key
     * @param defaultColor the default color to return if key is not set, or
     * incorrect
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
        int rgb = attr.getForeColorRGB();
        if (rgb >= 0) {
            return rgb;
        }

        if (attr.isBold()) {
            if (attr.getForeColor().equals(Color.BLACK)) {
                return MYBOLD_BLACK;
            } else if (attr.getForeColor().equals(Color.RED)) {
                return MYBOLD_RED;
            } else if (attr.getForeColor().equals(Color.BLUE)) {
                return MYBOLD_BLUE;
            } else if (attr.getForeColor().equals(Color.GREEN)) {
                return MYBOLD_GREEN;
            } else if (attr.getForeColor().equals(Color.YELLOW)) {
                return MYBOLD_YELLOW;
            } else if (attr.getForeColor().equals(Color.CYAN)) {
                return MYBOLD_CYAN;
            } else if (attr.getForeColor().equals(Color.MAGENTA)) {
                return MYBOLD_MAGENTA;
            } else if (attr.getForeColor().equals(Color.WHITE)) {
                return MYBOLD_WHITE;
            }
        } else {
            if (attr.getForeColor().equals(Color.BLACK)) {
                return MYBLACK;
            } else if (attr.getForeColor().equals(Color.RED)) {
                return MYRED;
            } else if (attr.getForeColor().equals(Color.BLUE)) {
                return MYBLUE;
            } else if (attr.getForeColor().equals(Color.GREEN)) {
                return MYGREEN;
            } else if (attr.getForeColor().equals(Color.YELLOW)) {
                return MYYELLOW;
            } else if (attr.getForeColor().equals(Color.CYAN)) {
                return MYCYAN;
            } else if (attr.getForeColor().equals(Color.MAGENTA)) {
                return MYMAGENTA;
            } else if (attr.getForeColor().equals(Color.WHITE)) {
                return MYWHITE;
            }
        }
        throw new IllegalArgumentException("Invalid color: " +
            attr.getForeColor().getValue());
    }

    /**
     * Convert a CellAttributes background color to an RGB color.
     *
     * @param attr the text attributes
     * @return the RGB color
     */
    public static int attrToBackgroundColor(final CellAttributes attr) {
        int rgb = attr.getBackColorRGB();
        if (rgb >= 0) {
            return rgb;
        }

        if (attr.getBackColor().equals(Color.BLACK)) {
            return MYBLACK;
        } else if (attr.getBackColor().equals(Color.RED)) {
            return MYRED;
        } else if (attr.getBackColor().equals(Color.BLUE)) {
            return MYBLUE;
        } else if (attr.getBackColor().equals(Color.GREEN)) {
            return MYGREEN;
        } else if (attr.getBackColor().equals(Color.YELLOW)) {
            return MYYELLOW;
        } else if (attr.getBackColor().equals(Color.CYAN)) {
            return MYCYAN;
        } else if (attr.getBackColor().equals(Color.MAGENTA)) {
            return MYMAGENTA;
        } else if (attr.getBackColor().equals(Color.WHITE)) {
            return MYWHITE;
        }
        throw new IllegalArgumentException("Invalid color: " +
            attr.getBackColor().getValue());
    }

    /**
     * Create a T.416 RGB parameter sequence for a custom system color.
     *
     * @param colorRGB one of the MYBLACK, MYBOLD_BLUE, etc. colors
     * @return the color portion of the string to emit to an ANSI /
     * ECMA-style terminal
     */
    private String systemColorRGB(final int colorRGB) {
        int colorRed     = (colorRGB >>> 16) & 0xFF;
        int colorGreen   = (colorRGB >>>  8) & 0xFF;
        int colorBlue    =  colorRGB         & 0xFF;

        return String.format("%d;%d;%d", colorRed, colorGreen, colorBlue);
    }

    /**
     * Create a SGR parameter sequence for a single color change.
     *
     * @param bold if true, set bold
     * @param color one of the Color.WHITE, Color.BLUE, etc. constants
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
     * @param colorRGB a 24-bit RGB value for foreground color
     * @param foreground if true, this is a foreground color
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[42m"
     */
    private String colorRGB(final int colorRGB, final boolean foreground) {

        int colorRed     = (colorRGB >>> 16) & 0xFF;
        int colorGreen   = (colorRGB >>>  8) & 0xFF;
        int colorBlue    =  colorRGB         & 0xFF;

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
        int foreColorRed     = (foreColorRGB >>> 16) & 0xFF;
        int foreColorGreen   = (foreColorRGB >>>  8) & 0xFF;
        int foreColorBlue    =  foreColorRGB         & 0xFF;
        int backColorRed     = (backColorRGB >>> 16) & 0xFF;
        int backColorGreen   = (backColorRGB >>>  8) & 0xFF;
        int backColorBlue    =  backColorRGB         & 0xFF;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\033[38;2;%d;%d;%dm",
                foreColorRed, foreColorGreen, foreColorBlue));
        sb.append(String.format("\033[48;2;%d;%d;%dm",
                backColorRed, backColorGreen, backColorBlue));
        return sb.toString();
    }

    /**
     * Create a T.416 RGB parameter sequence for a single color change.
     *
     * @param bold if true, set bold
     * @param color one of the Color.WHITE, Color.BLUE, etc. constants
     * @param foreground if true, this is a foreground color
     * @return the string to emit to an xterm terminal with RGB support,
     * e.g. "\033[38;2;RR;GG;BBm"
     */
    private String rgbColor(final boolean bold, final Color color,
        final boolean foreground) {
        if (doRgbColor == false) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\033[");
        if (bold) {
            // Bold implies foreground only
            sb.append("38;2;");
            if (color.equals(Color.BLACK)) {
                sb.append(systemColorRGB(MYBOLD_BLACK));
            } else if (color.equals(Color.RED)) {
                sb.append(systemColorRGB(MYBOLD_RED));
            } else if (color.equals(Color.GREEN)) {
                sb.append(systemColorRGB(MYBOLD_GREEN));
            } else if (color.equals(Color.YELLOW)) {
                sb.append(systemColorRGB(MYBOLD_YELLOW));
            } else if (color.equals(Color.BLUE)) {
                sb.append(systemColorRGB(MYBOLD_BLUE));
            } else if (color.equals(Color.MAGENTA)) {
                sb.append(systemColorRGB(MYBOLD_MAGENTA));
            } else if (color.equals(Color.CYAN)) {
                sb.append(systemColorRGB(MYBOLD_CYAN));
            } else if (color.equals(Color.WHITE)) {
                sb.append(systemColorRGB(MYBOLD_WHITE));
            }
        } else {
            if (foreground) {
                sb.append("38;2;");
            } else {
                sb.append("48;2;");
            }
            if (color.equals(Color.BLACK)) {
                sb.append(systemColorRGB(MYBLACK));
            } else if (color.equals(Color.RED)) {
                sb.append(systemColorRGB(MYRED));
            } else if (color.equals(Color.GREEN)) {
                sb.append(systemColorRGB(MYGREEN));
            } else if (color.equals(Color.YELLOW)) {
                sb.append(systemColorRGB(MYYELLOW));
            } else if (color.equals(Color.BLUE)) {
                sb.append(systemColorRGB(MYBLUE));
            } else if (color.equals(Color.MAGENTA)) {
                sb.append(systemColorRGB(MYMAGENTA));
            } else if (color.equals(Color.CYAN)) {
                sb.append(systemColorRGB(MYCYAN));
            } else if (color.equals(Color.WHITE)) {
                sb.append(systemColorRGB(MYWHITE));
            }
        }
        sb.append("m");
        return sb.toString();
    }

    /**
     * Create a T.416 RGB parameter sequence for both foreground and
     * background color change.
     *
     * @param bold if true, set bold
     * @param foreColor one of the Color.WHITE, Color.BLUE, etc. constants
     * @param backColor one of the Color.WHITE, Color.BLUE, etc. constants
     * @return the string to emit to an xterm terminal with RGB support,
     * e.g. "\033[38;2;RR;GG;BB;48;2;RR;GG;BBm"
     */
    private String rgbColor(final boolean bold, final Color foreColor,
        final Color backColor) {
        if (doRgbColor == false) {
            return "";
        }

        return rgbColor(bold, foreColor, true) +
                rgbColor(false, backColor, false);
    }

    /**
     * Create a SGR parameter sequence for a single color change.
     *
     * @param color one of the Color.WHITE, Color.BLUE, etc. constants
     * @param foreground if true, this is a foreground color
     * @param header if true, make the full header, otherwise just emit the
     * color parameter e.g. "42;"
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[42m"
     */
    private String color(final Color color, final boolean foreground,
        final boolean header) {

        int ecmaColor = color.getValue();

        // Convert Color.* values to SGR numerics
        if (foreground) {
            ecmaColor += 30;
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
     * @param bold if true, set bold
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
     * @param header if true, make the full header, otherwise just emit the
     * color parameter e.g. "31;42;"
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
     * Create a SGR parameter sequence for foreground, background, and
     * several attributes.  This sequence first resets all attributes to
     * default, then sets attributes as per the parameters.
     *
     * @param foreColor one of the Color.WHITE, Color.BLUE, etc. constants
     * @param backColor one of the Color.WHITE, Color.BLUE, etc. constants
     * @param bold if true, set bold
     * @param reverse if true, set reverse
     * @param blink if true, set blink
     * @param underline if true, set underline
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[0;1;31;42m"
     */
    private String color(final Color foreColor, final Color backColor,
        final boolean bold, final boolean reverse, final boolean blink,
        final boolean underline) {

        int ecmaForeColor = foreColor.getValue();
        int ecmaBackColor = backColor.getValue();

        // Convert Color.* values to SGR numerics
        ecmaBackColor += 40;
        ecmaForeColor += 30;

        StringBuilder sb = new StringBuilder();
        if        (  bold &&  reverse &&  blink && !underline ) {
            sb.append("\033[0;1;7;5;");
        } else if (  bold &&  reverse && !blink && !underline ) {
            sb.append("\033[0;1;7;");
        } else if ( !bold &&  reverse &&  blink && !underline ) {
            sb.append("\033[0;7;5;");
        } else if (  bold && !reverse &&  blink && !underline ) {
            sb.append("\033[0;1;5;");
        } else if (  bold && !reverse && !blink && !underline ) {
            sb.append("\033[0;1;");
        } else if ( !bold &&  reverse && !blink && !underline ) {
            sb.append("\033[0;7;");
        } else if ( !bold && !reverse &&  blink && !underline) {
            sb.append("\033[0;5;");
        } else if (  bold &&  reverse &&  blink &&  underline ) {
            sb.append("\033[0;1;7;5;4;");
        } else if (  bold &&  reverse && !blink &&  underline ) {
            sb.append("\033[0;1;7;4;");
        } else if ( !bold &&  reverse &&  blink &&  underline ) {
            sb.append("\033[0;7;5;4;");
        } else if (  bold && !reverse &&  blink &&  underline ) {
            sb.append("\033[0;1;5;4;");
        } else if (  bold && !reverse && !blink &&  underline ) {
            sb.append("\033[0;1;4;");
        } else if ( !bold &&  reverse && !blink &&  underline ) {
            sb.append("\033[0;7;4;");
        } else if ( !bold && !reverse &&  blink &&  underline) {
            sb.append("\033[0;5;4;");
        } else if ( !bold && !reverse && !blink &&  underline) {
            sb.append("\033[0;4;");
        } else {
            assert (!bold && !reverse && !blink && !underline);
            sb.append("\033[0;");
        }
        sb.append(String.format("%d;%dm", ecmaForeColor, ecmaBackColor));
        sb.append(rgbColor(bold, foreColor, backColor));
        return sb.toString();
    }

    /**
     * Create a SGR parameter sequence for several attributes.  This sequence
     * first resets all attributes to default, then sets attributes as per
     * the parameters.
     *
     * @param bold if true, set bold
     * @param reverse if true, set reverse
     * @param blink if true, set blink
     * @param underline if true, set underline
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[0;1;5m"
     */
    private String attributes(final boolean bold, final boolean reverse,
        final boolean blink, final boolean underline) {

        StringBuilder sb = new StringBuilder();
        if        (  bold &&  reverse &&  blink && !underline ) {
            sb.append("\033[0;1;7;5m");
        } else if (  bold &&  reverse && !blink && !underline ) {
            sb.append("\033[0;1;7m");
        } else if ( !bold &&  reverse &&  blink && !underline ) {
            sb.append("\033[0;7;5m");
        } else if (  bold && !reverse &&  blink && !underline ) {
            sb.append("\033[0;1;5m");
        } else if (  bold && !reverse && !blink && !underline ) {
            sb.append("\033[0;1m");
        } else if ( !bold &&  reverse && !blink && !underline ) {
            sb.append("\033[0;7m");
        } else if ( !bold && !reverse &&  blink && !underline) {
            sb.append("\033[0;5m");
        } else if (  bold &&  reverse &&  blink &&  underline ) {
            sb.append("\033[0;1;7;5;4m");
        } else if (  bold &&  reverse && !blink &&  underline ) {
            sb.append("\033[0;1;7;4m");
        } else if ( !bold &&  reverse &&  blink &&  underline ) {
            sb.append("\033[0;7;5;4m");
        } else if (  bold && !reverse &&  blink &&  underline ) {
            sb.append("\033[0;1;5;4m");
        } else if (  bold && !reverse && !blink &&  underline ) {
            sb.append("\033[0;1;4m");
        } else if ( !bold &&  reverse && !blink &&  underline ) {
            sb.append("\033[0;7;4m");
        } else if ( !bold && !reverse &&  blink &&  underline) {
            sb.append("\033[0;5;4m");
        } else if ( !bold && !reverse && !blink &&  underline) {
            sb.append("\033[0;4m");
        } else {
            assert (!bold && !reverse && !blink && !underline);
            sb.append("\033[0m");
        }
        return sb.toString();
    }

    /**
     * Create a SGR parameter sequence for foreground, background, and
     * several attributes.  This sequence first resets all attributes to
     * default, then sets attributes as per the parameters.
     *
     * @param foreColorRGB a 24-bit RGB value for foreground color
     * @param backColorRGB a 24-bit RGB value for foreground color
     * @param bold if true, set bold
     * @param reverse if true, set reverse
     * @param blink if true, set blink
     * @param underline if true, set underline
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[0;1;31;42m"
     */
    private String colorRGB(final int foreColorRGB, final int backColorRGB,
        final boolean bold, final boolean reverse, final boolean blink,
        final boolean underline) {

        int foreColorRed     = (foreColorRGB >>> 16) & 0xFF;
        int foreColorGreen   = (foreColorRGB >>>  8) & 0xFF;
        int foreColorBlue    =  foreColorRGB         & 0xFF;
        int backColorRed     = (backColorRGB >>> 16) & 0xFF;
        int backColorGreen   = (backColorRGB >>>  8) & 0xFF;
        int backColorBlue    =  backColorRGB         & 0xFF;

        StringBuilder sb = new StringBuilder();
        if        (  bold &&  reverse &&  blink && !underline ) {
            sb.append("\033[0;1;7;5;");
        } else if (  bold &&  reverse && !blink && !underline ) {
            sb.append("\033[0;1;7;");
        } else if ( !bold &&  reverse &&  blink && !underline ) {
            sb.append("\033[0;7;5;");
        } else if (  bold && !reverse &&  blink && !underline ) {
            sb.append("\033[0;1;5;");
        } else if (  bold && !reverse && !blink && !underline ) {
            sb.append("\033[0;1;");
        } else if ( !bold &&  reverse && !blink && !underline ) {
            sb.append("\033[0;7;");
        } else if ( !bold && !reverse &&  blink && !underline) {
            sb.append("\033[0;5;");
        } else if (  bold &&  reverse &&  blink &&  underline ) {
            sb.append("\033[0;1;7;5;4;");
        } else if (  bold &&  reverse && !blink &&  underline ) {
            sb.append("\033[0;1;7;4;");
        } else if ( !bold &&  reverse &&  blink &&  underline ) {
            sb.append("\033[0;7;5;4;");
        } else if (  bold && !reverse &&  blink &&  underline ) {
            sb.append("\033[0;1;5;4;");
        } else if (  bold && !reverse && !blink &&  underline ) {
            sb.append("\033[0;1;4;");
        } else if ( !bold &&  reverse && !blink &&  underline ) {
            sb.append("\033[0;7;4;");
        } else if ( !bold && !reverse &&  blink &&  underline) {
            sb.append("\033[0;5;4;");
        } else if ( !bold && !reverse && !blink &&  underline) {
            sb.append("\033[0;4;");
        } else {
            assert (!bold && !reverse && !blink && !underline);
            sb.append("\033[0;");
        }

        sb.append("m\033[38;2;");
        sb.append(String.format("%d;%d;%d", foreColorRed, foreColorGreen,
                foreColorBlue));
        sb.append("m\033[48;2;");
        sb.append(String.format("%d;%d;%d", backColorRed, backColorGreen,
                backColorBlue));
        sb.append("m");
        return sb.toString();
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
     * bare parameter e.g. "0;"
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
     * Tell (u)xterm that we want to receive mouse events based on "Any event
     * tracking", UTF-8 coordinates, and then SGR coordinates.  Ideally we
     * will end up with SGR coordinates with UTF-8 coordinates as a fallback.
     * See
     * http://invisible-island.net/xterm/ctlseqs/ctlseqs.html#Mouse%20Tracking
     *
     * Note that this also sets the alternate/primary screen buffer and
     * requests focus in/out sequences.
     *
     * Finally, also emit a Privacy Message sequence that Casciian recognizes to
     * mean "hide the mouse pointer."  We have to use our own sequence to do
     * this because there is no standard in xterm for unilaterally hiding the
     * pointer all the time (regardless of typing).
     *
     * @param on If true, enable mouse report and use the alternate screen
     * buffer.  If false disable mouse reporting and use the primary screen
     * buffer.
     * @return the string to emit to xterm
     */
    private String mouse(final boolean on) {
        if (on) {
            return "\033[?1004h\033[?1002;1003;1005;1006h\033[?1049h\033^hideMousePointer\033\\";
        }
        return "\033[?1004l\033[?1002;1003;1006;1005l\033[?1049l\033^showMousePointer\033\\";
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
            sb.append(String.format("\033]4;%d;?\033\\", i));
        }
        return sb.toString();
    }

    /**
     * Tell (u)xterm that we want to copy text to the system clipboard via
     * OSC 52.
     *
     * @param text string to copy
     */
    public void xtermSetClipboardText(final String text) {
        try {
            byte [] textBytes = text.getBytes("UTF-8");
            String textToCopy = StringUtils.toBase64(textBytes);
            // Remove CRLF from base64 result
            textToCopy = textToCopy.replaceAll("[\\r\\n]", "");
            this.output.printf("\033]52;c;%s\033\\", textToCopy);
            this.output.flush();
        } catch (UnsupportedEncodingException e) {
            // SQUASH
        }
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
     * 24-bit RGB images
     */
    public void setRgbColor(final boolean rgbColor) {
        doRgbColor = rgbColor;
    }

}
