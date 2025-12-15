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
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import casciian.bits.Cell;
import casciian.bits.Clipboard;
import casciian.bits.ComplexCell;
import casciian.event.TCommandEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TMenuEvent;
import casciian.event.TMouseEvent;
import casciian.event.TResizeEvent;
import casciian.menu.TMenu;
import casciian.terminal.DisplayLine;
import casciian.terminal.ECMA48;
import casciian.terminal.TerminalListener;
import casciian.terminal.TerminalState;
import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * TTerminal exposes a ECMA-48 / ANSI X3.64 style terminal in a widget.
 */
public class TTerminal extends TScrollable
                       implements TerminalListener, EditMenuUser {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * The emulator.
     */
    private ECMA48 emulator;

    /**
     * The Process created by the shell spawning constructor.
     */
    private Process shell;

    /**
     * The command line for the shell.
     */
    private String [] commandLine;

    /**
     * If true, something called 'ptypipe' is on the PATH and executable.
     */
    private static boolean ptypipeOnPath = false;

    /**
     * If true, we are using the ptypipe utility to support dynamic window
     * resizing.  ptypipe is available at
     * https://gitlab.com/AutumnMeowMeow/ptypipe .
     */
    private boolean ptypipe = false;

    /**
     * The last seen terminal state.
     */
    private TerminalState terminalState;

    /**
     * Update(s) from the terminal.
     */
    private List<TerminalState> dirtyQueue = new ArrayList<TerminalState>();

    /**
     * Current visible display.
     */
    List<DisplayLine> currentDisplay = null;

    /**
     * If true, hide the mouse after typing a keystroke.
     */
    private boolean hideMouseWhenTyping = true;

    /**
     * If true, the mouse should not be displayed because a keystroke was
     * typed.
     */
    private boolean typingHidMouse = false;

    /**
     * The return value from the emulator.
     */
    private int exitValue = -1;

    /**
     * Title to expose to a window.
     */
    private String title = "";

    /**
     * Action to perform when the terminal exits.
     */
    private TAction closeAction = null;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Static constructor.
     */
    static {
        checkForPtypipe();
    }

    /**
     * Public constructor spawns a custom command line.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param commandLine the command line to execute
     */
    public TTerminal(final TWidget parent, final int x, final int y,
        final String commandLine) {

        this(parent, x, y, commandLine.split("\\s+"));
    }

    /**
     * Public constructor spawns a custom command line.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param command the command line to execute, as an array of strings
     * which signifies the external program file to be invoked (command[0])
     * and its arguments, if any (command[1], command[2], ...). Refer also to
     * java.lang.ProcessBuilder for further operating-system specific
     * details.
     */
    public TTerminal(final TWidget parent, final int x, final int y,
        final String [] command) {

        this(parent, x, y, command, null);
    }

    /**
     * Public constructor spawns a custom command line.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param command the command line to execute, as an array of strings
     * which signifies the external program file to be invoked (command[0])
     * and its arguments, if any (command[1], command[2], ...). Refer also to
     * java.lang.ProcessBuilder for further operating-system specific
     * details.
     * @param closeAction action to perform when the shell exits
     */
    public TTerminal(final TWidget parent, final int x, final int y,
        final String [] command, final TAction closeAction) {

        this(parent, x, y, 80, 24, command, closeAction);
    }

    /**
     * Public constructor spawns a custom command line.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of widget
     * @param height height of widget
     * @param command the command line to execute, as an array of strings
     * which signifies the external program file to be invoked (command[0])
     * and its arguments, if any (command[1], command[2], ...). Refer also to
     * java.lang.ProcessBuilder for further operating-system specific
     * details.
     * @param closeAction action to perform when the shell exits
     */
    @SuppressWarnings("this-escape")
    public TTerminal(final TWidget parent, final int x, final int y,
        final int width, final int height, final String [] command,
        final TAction closeAction) {

        super(parent, x, y, width, height);
        i18n = ResourceBundle.getBundle(TTerminal.class.getName(),
            getLocale());

        setMouseStyle("text");
        this.closeAction = closeAction;

        // Save the external command line that can be used to recreate this
        // terminal, not the fully-processed command line.
        commandLine = command;

        String [] fullCommand;

        // Spawn a shell and pass its I/O to the other constructor.
        if ((System.getProperty("casciian.TTerminal.ptypipe") != null)
            && (System.getProperty("casciian.TTerminal.ptypipe").
                equals("true"))
        ) {
            ptypipe = true;
            fullCommand = new String[command.length + 1];
            fullCommand[0] = "ptypipe";
            System.arraycopy(command, 0, fullCommand, 1, command.length);
        } else if (System.getProperty("casciian.TTerminal.ptypipe",
                "auto").equals("auto")
            && (ptypipeOnPath == true)
        ) {
            ptypipe = true;
            fullCommand = new String[command.length + 1];
            fullCommand[0] = "ptypipe";
            System.arraycopy(command, 0, fullCommand, 1, command.length);
        } else if (System.getProperty("os.name").startsWith("Windows")) {
            fullCommand = new String[3];
            fullCommand[0] = "cmd";
            fullCommand[1] = "/c";
            fullCommand[2] = stringArrayToString(command);
        } else if (System.getProperty("os.name").startsWith("Mac")) {
            fullCommand = new String[6];
            fullCommand[0] = "script";
            fullCommand[1] = "-q";
            fullCommand[2] = "-F";
            fullCommand[3] = "/dev/null";
            fullCommand[4] = "-c";
            fullCommand[5] = stringArrayToString(command);
        } else {
            // Default: behave like Linux
            if (System.getProperty("casciian.TTerminal.setsid",
                    "true").equals("false")
            ) {
                fullCommand = new String[5];
                fullCommand[0] = "script";
                fullCommand[1] = "-fqe";
                fullCommand[2] = "/dev/null";
                fullCommand[3] = "-c";
                fullCommand[4] = stringArrayToString(command);
            } else {
                fullCommand = new String[6];
                fullCommand[0] = "setsid";
                fullCommand[1] = "script";
                fullCommand[2] = "-fqe";
                fullCommand[3] = "/dev/null";
                fullCommand[4] = "-c";
                fullCommand[5] = stringArrayToString(command);
            }
        }
        spawnShell(fullCommand);
    }

    /**
     * Public constructor spawns a shell.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     */
    public TTerminal(final TWidget parent, final int x, final int y) {
        this(parent, x, y, (TAction) null);
    }

    /**
     * Public constructor spawns a shell.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param closeAction action to perform when the shell exits
     */
    public TTerminal(final TWidget parent, final int x, final int y,
        final TAction closeAction) {

        this(parent, x, y, 80, 24, closeAction);
    }

    /**
     * Public constructor spawns a shell.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of widget
     * @param height height of widget
     * @param closeAction action to perform when the shell exits
     */
    @SuppressWarnings("this-escape")
    public TTerminal(final TWidget parent, final int x, final int y,
        final int width, final int height, final TAction closeAction) {

        super(parent, x, y, width, height);
        i18n = ResourceBundle.getBundle(TTerminal.class.getName(),
            getLocale());

        setMouseStyle("text");
        this.closeAction = closeAction;

        if (System.getProperty("casciian.TTerminal.shell") != null) {
            String shell = System.getProperty("casciian.TTerminal.shell");
            if (shell.trim().startsWith("ptypipe")) {
                ptypipe = true;
            }
            spawnShell(shell.split("\\s+"));
            return;
        }

        // Save an empty command line.
        commandLine = new String[0];

        String cmdShellWindows = "cmd.exe";

        // You cannot run a login shell in a bare Process interactively, due
        // to libc's behavior of buffering when stdin/stdout aren't a tty.
        // Use 'script' instead to run a shell in a pty.  And because BSD and
        // GNU differ on the '-f' vs '-F' flags, we need two different
        // commands.  Lovely.
        String cmdShellGNU = "script -fqe /dev/null";
        String cmdShellGNUSetsid = "setsid script -fqe /dev/null";
        String cmdShellBSD = "script -q -F /dev/null";

        // ptypipe is another solution that permits dynamic window resizing.
        String cmdShellPtypipe = "ptypipe /bin/bash --login";

        // Spawn a shell and pass its I/O to the other constructor.
        if ((System.getProperty("casciian.TTerminal.ptypipe") != null)
            && (System.getProperty("casciian.TTerminal.ptypipe").
                equals("true"))
        ) {
            ptypipe = true;
            spawnShell(cmdShellPtypipe.split("\\s+"));
        } else if (System.getProperty("casciian.TTerminal.ptypipe",
                "auto").equals("auto")
            && (ptypipeOnPath == true)
        ) {
            ptypipe = true;
            spawnShell(cmdShellPtypipe.split("\\s+"));
        } else if (System.getProperty("os.name").startsWith("Windows")) {
            spawnShell(cmdShellWindows.split("\\s+"));
        } else if (System.getProperty("os.name").startsWith("Mac")) {
            spawnShell(cmdShellBSD.split("\\s+"));
        } else if (System.getProperty("os.name").startsWith("Linux")) {
            if (System.getProperty("casciian.TTerminal.setsid",
                    "true").equals("false")
            ) {
                spawnShell(cmdShellGNU.split("\\s+"));
            } else {
                spawnShell(cmdShellGNUSetsid.split("\\s+"));
            }
        } else {
            // When all else fails, assume GNU.
            spawnShell(cmdShellGNU.split("\\s+"));
        }
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
        // Let TWidget set my size.
        super.onResize(resize);

        if (emulator == null) {
            return;
        }

        // Synchronize against the emulator so we don't stomp on its reader
        // thread.
        synchronized (emulator) {

            if (resize.getType() == TResizeEvent.Type.WIDGET) {
                // Resize the scroll bars
                reflowData();
                placeScrollbars();

                // Get out of scrollback
                setVerticalValue(0);

                if (ptypipe) {
                    emulator.setWidth(getWidth());
                    emulator.setHeight(getHeight());

                    emulator.writeRemote("\033[8;" + getHeight() + ";" +
                        getWidth() + "t");
                }
            }
            return;

        } // synchronized (emulator)
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (hideMouseWhenTyping) {
            typingHidMouse = true;
        }

        // Scrollback up/down/home/end
        if (keypress.equals(kbShiftHome)
            || keypress.equals(kbCtrlHome)
            || keypress.equals(kbAltHome)
        ) {
            toTop();
            return;
        }
        if (keypress.equals(kbShiftEnd)
            || keypress.equals(kbCtrlEnd)
            || keypress.equals(kbAltEnd)
        ) {
            toBottom();
            return;
        }
        if (keypress.equals(kbShiftPgUp)
            || keypress.equals(kbCtrlPgUp)
            || keypress.equals(kbAltPgUp)
        ) {
            bigVerticalDecrement();
            return;
        }
        if (keypress.equals(kbShiftPgDn)
            || keypress.equals(kbCtrlPgDn)
            || keypress.equals(kbAltPgDn)
        ) {
            bigVerticalIncrement();
            return;
        }

        if ((emulator != null) && (emulator.isReading())) {
            // Get out of scrollback
            setVerticalValue(0);
            emulator.addUserEvent(keypress);

            // UGLY HACK TIME!  cmd.exe needs CRLF, not just CR, so if
            // this is kBEnter then also send kbCtrlJ.
            if (keypress.equals(kbEnter)) {
                if (System.getProperty("os.name").startsWith("Windows")
                    && (System.getProperty("casciian.TTerminal.cmdHack",
                            "true").equals("true"))
                ) {
                    emulator.addUserEvent(new TKeypressEvent(
                        keypress.getBackend(), kbCtrlJ));
                }
            }
            return;
        }

        // Process is closed, honor "normal" TUI keystrokes
        super.onKeypress(keypress);
    }

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if (hideMouseWhenTyping) {
            typingHidMouse = false;
        }

        if (emulator != null) {
            // If the emulator is tracking mouse buttons, it needs to see
            // wheel events.
            if (terminalState.getMouseProtocol() == ECMA48.MouseProtocol.OFF) {
                if (mouse.isMouseWheelUp()) {
                    verticalDecrement();
                    return;
                }
                if (mouse.isMouseWheelDown()) {
                    verticalIncrement();
                    return;
                }
            }
            if (mouseOnEmulator(mouse)) {
                emulator.addUserEvent(mouse);
                return;
            }
        }

        // Emulator didn't consume it, pass it on
        super.onMouseDown(mouse);
    }

    /**
     * Handle mouse release events.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        if (hideMouseWhenTyping) {
            typingHidMouse = false;
        }

        if ((emulator != null) && (mouseOnEmulator(mouse))) {
            emulator.addUserEvent(mouse);
            return;
        }

        // Emulator didn't consume it, pass it on
        super.onMouseUp(mouse);
    }

    /**
     * Handle mouse motion events.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        if (hideMouseWhenTyping) {
            typingHidMouse = false;
        }

        if ((emulator != null) && (mouseOnEmulator(mouse))) {
            emulator.addUserEvent(mouse);
            return;
        }

        // Emulator didn't consume it, pass it on
        super.onMouseMotion(mouse);
    }

    /**
     * Handle posted command events.
     *
     * @param command command event
     */
    @Override
    public void onCommand(final TCommandEvent command) {
        if (emulator == null) {
            return;
        }

        if (command.equals(cmPaste)) {
            // Paste text from clipboard.
            String text = getClipboard().pasteText();
            if (text != null) {
                for (int i = 0; i < text.length(); ) {
                    int ch = text.codePointAt(i);
                    emulator.addUserEvent(new TKeypressEvent(
                        command.getBackend(), false, 0, ch,
                        false, false, false));
                    i += Character.charCount(ch);
                }
            }
            return;
        }
    }

    // ------------------------------------------------------------------------
    // TScrollable ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the display buffer.
     */
    @Override
    public void draw() {
        if (emulator == null) {
            return;
        }

        checkTerminalState();

        processTerminalState();
        if (currentDisplay == null) {
            return;
        }
        List<DisplayLine> display = currentDisplay;

        int width = getDisplayWidth();

        // Draw the emulator screen.
        int row = 0;
        for (DisplayLine line: display) {
            int widthMax = width;
            if (line.isDoubleWidth()) {
                widthMax /= 2;
            }
            if (widthMax > getWidth()) {
                widthMax = getWidth();
            }
            for (int i = 0; i < widthMax; i++) {
                ComplexCell ch = line.charAt(i);
                ComplexCell newCell = new ComplexCell(ch);
                boolean reverse = line.isReverseColor() ^ ch.isReverse();
                newCell.setReverse(false);
                if (reverse) {
                    if (ch.getForeColorRGB() < 0) {
                        newCell.setBackColor(ch.getForeColor());
                    } else {
                        newCell.setBackColorRGB(ch.getForeColorRGB());
                    }
                    if (ch.getBackColorRGB() < 0) {
                        newCell.setForeColor(ch.getBackColor());
                    } else {
                        newCell.setForeColorRGB(ch.getBackColorRGB());
                    }
                }
                if (line.isDoubleWidth()) {
                    putDoubleWidthCharXY(line, (i * 2), row, newCell);
                } else {
                    putCharXY(i, row, newCell);
                }
            }
            row++;
        }
    }

    /**
     * Handle widget close.
     */
    @Override
    public void close() {
        if (emulator != null) {
            emulator.close();
        }
        if (shell != null) {
            terminateShellChildProcess();
            shell.destroy();
            shell = null;
        }
    }

    /**
     * Resize scrollbars for a new width/height.
     */
    @Override
    public void reflowData() {
        if (emulator == null) {
            return;
        }

        if (terminalState == null) {
            synchronized (emulator) {
                terminalState = emulator.captureState();
            }
        }

        // Vertical scrollbar
        setTopValue(getHeight()
            - (terminalState.getScrollbackBuffer().size()
                + terminalState.getDisplayBuffer().size()));
        setVerticalBigChange(getHeight());

    }

    // ------------------------------------------------------------------------
    // TTerminal --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Ensure terminal state is known.
     */
    private void checkTerminalState() {
        assert (emulator != null);
        if (terminalState == null) {
            synchronized (emulator) {
                terminalState = emulator.captureState();
            }
        } else {
            // If the emulator posted updated, sync.
            synchronized (dirtyQueue) {
                if (dirtyQueue.size() > 0) {
                    // We will be dropping frames to keep up.
                    terminalState = dirtyQueue.remove(dirtyQueue.size() - 1);
                    dirtyQueue.clear();
                }
            }
        }
    }

    /**
     * Check for 'ptypipe' on the path.  If available, set ptypipeOnPath.
     */
    private static void checkForPtypipe() {
        String systemPath = System.getenv("PATH");
        if (systemPath == null) {
            return;
        }

        String [] paths = systemPath.split(File.pathSeparator);
        if (paths == null) {
            return;
        }
        if (paths.length == 0) {
            return;
        }
        for (int i = 0; i < paths.length; i++) {
            File path = new File(paths[i]);
            if (path.exists() && path.isDirectory()) {
                File [] files = path.listFiles();
                if (files == null) {
                    continue;
                }
                if (files.length == 0) {
                    continue;
                }
                for (int j = 0; j < files.length; j++) {
                    File file = files[j];
                    if (file.canExecute() && file.getName().equals("ptypipe")) {
                        ptypipeOnPath = true;
                        return;
                    }
                }
            }
        }
    }

    /**
     * Get the desired window title.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Get the full command line that spawned the shell.
     *
     * @return the command line
     */
    public String [] getCommandLine() {
        return commandLine;
    }

    /**
     * Returns true if this widget does not want the application-wide mouse
     * cursor drawn over it.
     *
     * @return true if this widget does not want the application-wide mouse
     * cursor drawn over it
     */
    public boolean hasHiddenMouse() {
        if (emulator != null) {
            if (!emulator.isReading()) {
                typingHidMouse = false;
            }
            checkTerminalState();
            boolean hiddenMouse = (terminalState.hasHiddenMousePointer()
                || typingHidMouse);
            if (hiddenMouse) {
                setMouseStyle("none");
            } else {
                setMouseStyle("text");
            }
            return hiddenMouse;
        }
        return false;
    }

    /**
     * See if the terminal is still running.
     *
     * @return if true, we are still connected to / reading from the remote
     * side
     */
    public boolean isReading() {
        if (emulator == null) {
            return false;
        }
        return emulator.isReading();
    }

    /**
     * Let the remote application know this terminal gained focus.
     */
    public void onFocus() {
        if (emulator != null) {
            emulator.onFocus();
        }
    }

    /**
     * Let the remote application know this terminal lost focus.
     */
    public void onUnfocus() {
        if (emulator != null) {
            emulator.onUnfocus();
        }
    }

    /**
     * Convert a string array to a whitespace-separated string.
     *
     * @param array the string array
     * @return a single string
     */
    private String stringArrayToString(final String [] array) {
        StringBuilder sb = new StringBuilder(array[0].length());
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    /**
     * Spawn the shell.
     *
     * @param command the command line to execute
     */
    private void spawnShell(final String [] command) {
        /*
        System.err.printf("spawnShell(): '%s'\n",
            stringArrayToString(command));
        */

        // We will have vScroller for its data fields and mouse event
        // handling, but do not want to draw it.
        vScroller = new TVScroller(null, getWidth(), 0, getHeight());
        vScroller.setVisible(false);
        setBottomValue(0);

        title = i18n.getString("windowTitle");

        // Assume XTERM
        ECMA48.DeviceType deviceType = ECMA48.DeviceType.XTERM;

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            Map<String, String> env = pb.environment();
            String langString = System.getenv().get("LANG");
            if (langString == null) {
                Locale locale = Locale.getDefault();
                langString = locale.getLanguage();
                if (locale.getCountry().length() > 0) {
                    langString += "_" + locale.getCountry();
                }
            } else {
                int dotIndex = langString.indexOf(".");
                if (dotIndex > 0) {
                    langString = langString.substring(0, dotIndex);
                }
            }
            String termString = System.getProperty("casciian.TTerminal.TERM",
                "xterm-direct");
            env.put("TERM", termString);
            env.put("LANG", ECMA48.deviceTypeLang(deviceType, langString));
            env.put("COLUMNS", "80");
            env.put("LINES", "24");
            pb.redirectErrorStream(true);
            shell = pb.start();
            emulator = new ECMA48(deviceType, shell.getInputStream(),
                shell.getOutputStream(), this, getApplication().getBackend());
        } catch (IOException e) {
            messageBox(i18n.getString("errorLaunchingShellTitle"),
                MessageFormat.format(i18n.getString("errorLaunchingShellText"),
                    e.getMessage()));
        }

        // Setup the scroll bars
        onResize(new TResizeEvent(null, TResizeEvent.Type.WIDGET, getWidth(),
                getHeight()));

        // Hide mouse when typing option
        if (System.getProperty("casciian.TTerminal.hideMouseWhenTyping",
                "true").equals("false")) {

            hideMouseWhenTyping = false;
        }

        try {
            int scrollbackMax = Integer.parseInt(System.getProperty(
                "casciian.TTerminal.scrollbackMax", "2000"));
            if (emulator != null) {
                emulator.setScrollbackMax(scrollbackMax);
            }
        } catch (NumberFormatException e) {
            // SQUASH
        }

    }

    /**
     * Terminate the child of the 'script' process used on POSIX.  This may
     * or may not work.
     */
    private void terminateShellChildProcess() {
        long pid = getPid();
        if (pid != -1) {
            // shell.destroy() works successfully at killing this side of
            // 'script'.  But we need to make sure the other side (child
            // process) is also killed.
            String [] cmdKillIt = {
                "pkill", "-P", Long.toString(pid), "--signal", "KILL"
            };
            try {
                Runtime.getRuntime().exec(cmdKillIt);
            } catch (Throwable e) {
                // SQUASH, this didn't work.  Just bail out quietly.
                return;
            }
        }
    }

    /**
     * Get the PID of the child process.
     *
     * @return the pid, or -1 if it cannot be determined
     */
    public long getPid() {
        try {
            // Java 9 or later, public access to Process.pid().
            Method method = Process.class.getMethod("pid");
            if (Modifier.isPublic(method.getModifiers())) {
                return ((Long) method.invoke(shell)).longValue();
            } else {
                // This is probably related to JDK-4283544: a public
                // interface method on a private implementation class.  Fall
                // through to a pre-Java 9 attempt.
            }
        } catch (NoSuchMethodException e) {
            // This will be before Java 9, fall through.
        } catch (Throwable e) {
            // SQUASH, this didn't work.  Just bail out quietly.
            return -1;
        }

        if (shell.getClass().getName().equals("java.lang.UNIXProcess")) {
            // Java 1.6 or earlier.  Should work smoothly.
            try {
                Field field = shell.getClass().getDeclaredField("pid");
                field.setAccessible(true);
                return field.getInt(shell);
            } catch (Throwable e) {
                // SQUASH, this didn't work.  Just bail out quietly.
                return -1;
            }
        }

        if (shell.getClass().getName().equals("java.lang.ProcessImpl")) {
            // Java 1.7 and 1.8.  If this is actually running on a Java 9+
            // there will be nasty errors in stderr from the setAccessible()
            // call.
            try {
                Field field = shell.getClass().getDeclaredField("pid");
                field.setAccessible(true);
                return field.getInt(shell);
            } catch (Throwable e) {
                // SQUASH, this didn't work.  Just bail out quietly.
                return -1;
            }
        }

        // Don't know how to get the PID.
        return -1;
    }

    /**
     * Send a signal to the the child of the 'script' or 'ptypipe' process
     * used on POSIX.  This may or may not work.
     *
     * @param signal the signal number
     */
    public void signalShellChildProcess(final int signal) {
        long pid = getPid();

        if (pid != -1) {
            String [] cmdSendSignal = {
                "kill", Long.toString(pid), "--signal",
                Integer.toString(signal)
            };
            try {
                Runtime.getRuntime().exec(cmdSendSignal);
            } catch (Throwable e) {
                // SQUASH, this didn't work.  Just bail out quietly.
                return;
            }
        }
    }

    /**
     * Send a signal to the the child of the 'script' or 'ptypipe' process
     * used on POSIX.  This may or may not work.
     *
     * @param signal the signal name
     */
    public void signalShellChildProcess(final String signal) {
        long pid = getPid();

        if (pid != -1) {
            String [] cmdSendSignal = {
                "kill", Long.toString(pid), "--signal", signal
            };
            try {
                Runtime.getRuntime().exec(cmdSendSignal);
            } catch (Throwable e) {
                // SQUASH, this didn't work.  Just bail out quietly.
                return;
            }
        }
    }

    /**
     * Hook for subclasses to be notified of the shell termination.
     */
    public void onShellExit() {
        TApplication app = getApplication();
        if (app != null) {
            if (closeAction != null) {
                // We have to put this action inside invokeLater() because it
                // could be executed during draw() when syncing with ECMA48.
                app.invokeLater(new Runnable() {
                    public void run() {
                        closeAction.DO(TTerminal.this);
                    }
                });
            }
            app.doRepaint();
        }
    }

    /**
     * Copy out variables from the emulator that TTerminal has to expose on
     * screen.
     */
    private void processTerminalState() {
        if (terminalState == null) {
            return;
        }
        boolean emulatorIsReading = true;
        if (emulator != null) {
            synchronized (emulator) {
                emulatorIsReading = emulator.isReading();
            }
        }

        setCursorX(terminalState.getCursorX());
        setCursorY(terminalState.getCursorY()
            + (getHeight() - terminalState.getHeight())
            - getVerticalValue());
        setCursorVisible(terminalState.isCursorVisible());
        if (getCursorX() > getWidth()) {
            setCursorVisible(false);
        }
        if ((getCursorY() >= getHeight()) || (getCursorY() < 0)) {
            setCursorVisible(false);
        }
        if (terminalState.getScreenTitle().length() > 0) {
            // Only update the title if the shell is still alive
            if (shell != null) {
                title = terminalState.getScreenTitle();
            }
        }

        // Update the scroll bars
        reflowData();

        if (!isDrawable()) {
            // We lost the connection, onShellExit() called an action that
            // ultimately removed this widget from the UI hierarchy, so no
            // one cares if we update the display.  Bail out.
            return;
        }

        if (emulatorIsReading) {
            currentDisplay = terminalState.getVisibleDisplay(getHeight(),
                -getVerticalValue());
            assert (currentDisplay.size() == getHeight());
        }

        // Check to see if the shell has died.
        if (!emulatorIsReading && (shell != null)) {
            try {
                int rc = shell.exitValue();
                // The emulator exited on its own, all is fine
                title = MessageFormat.format(i18n.
                    getString("windowTitleCompleted"), title, rc);
                exitValue = rc;
                shell = null;
                emulator.close();
                onShellExit();
            } catch (IllegalThreadStateException e) {
                // The emulator thread has exited, but the shell Process
                // hasn't figured that out yet.  Do nothing, we will see
                // this in a future tick.
            }
        } else if (emulatorIsReading && (shell != null)) {
            // The shell might be dead, let's check
            try {
                int rc = shell.exitValue();
                // If we got here, the shell died.
                title = MessageFormat.format(i18n.
                    getString("windowTitleCompleted"), title, rc);
                exitValue = rc;
                shell = null;
                emulator.close();
                onShellExit();
            } catch (IllegalThreadStateException e) {
                // The shell is still running, do nothing.
            }
        }
    }

    /**
     * Wait for a period of time to get output from the launched process.
     *
     * @param millis millis to wait for, or 0 to wait forever
     * @return true if the launched process has emitted something
     */
    public boolean waitForOutput(final int millis) {
        if (emulator == null) {
            return false;
        }
        return emulator.waitForOutput(millis);
    }

    /**
     * Check if a mouse press/release/motion event coordinate is over the
     * emulator.
     *
     * @param mouse a mouse-based event
     * @return whether or not the mouse is on the emulator
     */
    private boolean mouseOnEmulator(final TMouseEvent mouse) {
        if (emulator == null) {
            return false;
        }

        if (!emulator.isReading()) {
            return false;
        }

        if ((mouse.getX() >= 0)
            && (mouse.getX() < getWidth() - 1)
            && (mouse.getY() >= 0)
            && (mouse.getY() < getHeight())
        ) {
            return true;
        }
        return false;
    }

    /**
     * Draw glyphs for a double-width or double-height VT100 cell to two
     * screen cells.
     *
     * @param line the line this VT100 cell is in
     * @param x the X position to draw the left half to
     * @param y the Y position to draw to
     * @param cell the cell to draw
     */
    private void putDoubleWidthCharXY(final DisplayLine line, final int x,
        final int y, final ComplexCell cell) {

        putCharXY(x, y, cell);
        putCharXY(x + 1, y, ' ', cell);
    }

    /**
     * Get the exit value for the emulator.
     *
     * @return exit value
     */
    public int getExitValue() {
        return exitValue;
    }

    /**
     * Write the entire session (scrollback and display buffers) as plain
     * text to a writer.
     *
     * @param writer the output writer
     * @throws IOException of a java.io operation throws
     */
    public void writeSessionAsText(final Writer writer) throws IOException {
        checkTerminalState();
        for (DisplayLine line: terminalState.getScrollbackBuffer()) {
            for (int i = 0; i < line.length(); i++) {
                writer.write(new String(Character.toChars(
                        line.charAt(i).getChar())));
            }
            writer.write("\n");
        }
        for (DisplayLine line: terminalState.getDisplayBuffer()) {
            for (int i = 0; i < line.length(); i++) {
                writer.write(new String(Character.toChars(
                        line.charAt(i).getChar())));
            }
            writer.write("\n");
        }
    }

    /**
     * Write the entire session (scrollback and display buffers) as colorized
     * HTML to a writer.  This method does not write the HTML header/body
     * tags.
     *
     * @param writer the output writer
     * @throws IOException of a java.io operation throws
     */
    public void writeSessionAsHtml(final Writer writer) throws IOException {
        checkTerminalState();
        for (DisplayLine line: terminalState.getScrollbackBuffer()) {
            for (int i = 0; i < line.length(); i++) {
                writer.write(line.charAt(i).toHtml());
            }
            writer.write("\n");
        }
        for (DisplayLine line: terminalState.getDisplayBuffer()) {
            for (int i = 0; i < line.length(); i++) {
                writer.write(line.charAt(i).toHtml());
            }
            writer.write("\n");
        }
    }

    // ------------------------------------------------------------------------
    // TerminalListener -------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Called by emulator when fresh data has come in.
     *
     * @param terminalState the new terminal state
     */
    public void postUpdate(final TerminalState terminalState) {
        synchronized (dirtyQueue) {
            dirtyQueue.add(terminalState);
        }
        TApplication app = getApplication();
        if (app != null) {
            app.doRepaint();
        }
    }

    /**
     * Function to call to obtain the display width.
     *
     * @return the number of columns in the display
     */
    public int getDisplayWidth() {
        if (ptypipe) {
            return getWidth();
        }
        return 80;
    }

    /**
     * Function to call to obtain the display height.
     *
     * @return the number of rows in the display
     */
    public int getDisplayHeight() {
        if (ptypipe) {
            return getHeight();
        }
        return 24;
    }

    /**
     * Get the system clipboard to use for OSC 52.
     *
     * @return the clipboard
     */
    public Clipboard getClipboard() {
        return getApplication().getClipboard();
    }

    // ------------------------------------------------------------------------
    // EditMenuUser -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Check if the cut menu item should be enabled.
     *
     * @return true if the cut menu item should be enabled
     */
    public boolean isEditMenuCut() {
        return false;
    }

    /**
     * Check if the copy menu item should be enabled.
     *
     * @return true if the copy menu item should be enabled
     */
    public boolean isEditMenuCopy() {
        return false;
    }

    /**
     * Check if the paste menu item should be enabled.
     *
     * @return true if the paste menu item should be enabled
     */
    public boolean isEditMenuPaste() {
        return true;
    }

    /**
     * Check if the clear menu item should be enabled.
     *
     * @return true if the clear menu item should be enabled
     */
    public boolean isEditMenuClear() {
        return false;
    }

}
