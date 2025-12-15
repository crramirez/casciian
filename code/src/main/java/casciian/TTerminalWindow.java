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

import java.util.ResourceBundle;

import casciian.menu.TMenu;
import casciian.event.TCommandEvent;
import casciian.event.TKeypressEvent;
import casciian.event.TMenuEvent;
import casciian.event.TMouseEvent;
import casciian.event.TResizeEvent;
import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * TTerminalWindow exposes a ECMA-48 / ANSI X3.64 style terminal in a window.
 */
public class TTerminalWindow extends TScrollableWindow {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * The terminal.
     */
    protected TTerminal terminal;

    /**
     * If true, close the window when the shell exits.
     */
    private boolean closeOnExit = false;

    /**
     * If true, setTitle() was called and this title will be honored.
     */
    private boolean titleOverride = false;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor spawns a custom command line.
     *
     * @param application TApplication that manages this window
     * @param x column relative to parent
     * @param y row relative to parent
     * @param commandLine the command line to execute
     */
    public TTerminalWindow(final TApplication application, final int x,
        final int y, final String commandLine) {

        this(application, x, y, RESIZABLE, commandLine.split("\\s+"),
            System.getProperty("casciian.TTerminal.closeOnExit",
                "false").equals("true"));
    }

    /**
     * Public constructor spawns a custom command line.
     *
     * @param application TApplication that manages this window
     * @param x column relative to parent
     * @param y row relative to parent
     * @param commandLine the command line to execute
     * @param closeOnExit if true, close the window when the command exits
     */
    public TTerminalWindow(final TApplication application, final int x,
        final int y, final String commandLine, final boolean closeOnExit) {

        this(application, x, y, RESIZABLE, commandLine.split("\\s+"),
            closeOnExit);
    }

    /**
     * Public constructor spawns a custom command line.
     *
     * @param application TApplication that manages this window
     * @param x column relative to parent
     * @param y row relative to parent
     * @param flags mask of CENTERED, MODAL, or RESIZABLE
     * @param command the command line to execute, as an array of strings
     * which signifies the external program file to be invoked (command[0])
     * and its arguments, if any (command[1], command[2], ...). Refer also to
     * java.lang.ProcessBuilder for further operating-system specific
     * details.
     */
    public TTerminalWindow(final TApplication application, final int x,
        final int y, final int flags, final String [] command) {

        this(application, x, y, flags, command,
            System.getProperty("casciian.TTerminal.closeOnExit",
                "false").equals("true"));
    }

    /**
     * Public constructor spawns a custom command line.
     *
     * @param application TApplication that manages this window
     * @param x column relative to parent
     * @param y row relative to parent
     * @param flags mask of CENTERED, MODAL, or RESIZABLE
     * @param command the command line to execute, as an array of strings
     * which signifies the external program file to be invoked (command[0])
     * and its arguments, if any (command[1], command[2], ...). Refer also to
     * java.lang.ProcessBuilder for further operating-system specific
     * details.
     * @param closeOnExit if true, close the window when the command exits
     */
    @SuppressWarnings("this-escape")
    public TTerminalWindow(final TApplication application, final int x,
        final int y, final int flags, final String [] command,
        final boolean closeOnExit) {

        super(application, "", x, y, 80 + 2, 24 + 2, flags);

        i18n = ResourceBundle.getBundle(TTerminalWindow.class.getName(),
            getLocale());
        setTitle(i18n.getString("windowTitle"));

        // Require at least one line for the display.
        setMinimumWindowHeight(3);

        this.closeOnExit = closeOnExit;
        vScroller = new TVScroller(this, getWidth() - 2, 0, getHeight() - 2);

        // Claim the keystrokes the emulator will need.
        addShortcutKeys();

        // Add shortcut text
        TStatusBar statusBar = newStatusBar(i18n.getString("statusBarRunning"));
        statusBar.addShortcutKeypress(kbF1, cmHelp,
            i18n.getString("statusBarHelp"));
        statusBar.addShortcutKeypress(kbShiftF10, cmMenu,
            i18n.getString("statusBarMenu"));

        // Spin it up
        terminal = new TTerminal(this, 0, 0, command, new TAction() {
            public void DO() {
                onShellExit();
            }
        });

        int opacity = 95;
        try {
            opacity = Integer.parseInt(System.getProperty(
                "casciian.TTerminal.opacity", "95"));
            opacity = Math.max(opacity, 10);
            opacity = Math.min(opacity, 100);
        } catch (NumberFormatException e) {
            // SQUASH
        }
        setAlpha(opacity * 255 / 100);
    }

    /**
     * Public constructor spawns a shell.
     *
     * @param application TApplication that manages this window
     * @param x column relative to parent
     * @param y row relative to parent
     * @param flags mask of CENTERED, MODAL, or RESIZABLE
     */
    public TTerminalWindow(final TApplication application, final int x,
        final int y, final int flags) {

        this(application, x, y, flags,
            System.getProperty("casciian.TTerminal.closeOnExit",
                "false").equals("true"));

    }

    /**
     * Public constructor spawns a shell.
     *
     * @param application TApplication that manages this window
     * @param x column relative to parent
     * @param y row relative to parent
     * @param flags mask of CENTERED, MODAL, or RESIZABLE
     * @param closeOnExit if true, close the window when the shell exits
     */
    @SuppressWarnings("this-escape")
    public TTerminalWindow(final TApplication application, final int x,
        final int y, final int flags, final boolean closeOnExit) {

        super(application, "", x, y, 80 + 2, 24 + 2, flags);
        i18n = ResourceBundle.getBundle(TTerminalWindow.class.getName(),
            getLocale());
        setTitle(i18n.getString("windowTitle"));

        // Require at least one line for the display.
        setMinimumWindowHeight(3);

        this.closeOnExit = closeOnExit;
        vScroller = new TVScroller(this, getWidth() - 2, 0, getHeight() - 2);

        // Claim the keystrokes the emulator will need.
        addShortcutKeys();

        // Add shortcut text
        TStatusBar statusBar = newStatusBar(i18n.getString("statusBarRunning"));
        statusBar.addShortcutKeypress(kbF1, cmHelp,
            i18n.getString("statusBarHelp"));
        statusBar.addShortcutKeypress(kbShiftF10, cmMenu,
            i18n.getString("statusBarMenu"));

        // Spin it up
        terminal = new TTerminal(this, 0, 0, new TAction() {
            public void DO() {
                onShellExit();
            }
        });

        int opacity = 95;
        try {
            opacity = Integer.parseInt(System.getProperty(
                "casciian.TTerminal.opacity", "95"));
            opacity = Math.max(opacity, 10);
            opacity = Math.min(opacity, 100);
        } catch (NumberFormatException e) {
            // SQUASH
        }
        setAlpha(opacity * 255 / 100);
    }

    // ------------------------------------------------------------------------
    // TScrollableWindow ------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the display buffer.
     */
    @Override
    public void draw() {
        if ((terminal != null) && (titleOverride == false)) {
            setTitle(terminal.getTitle());
        }
        reflowData();
        super.draw();
    }

    /**
     * Handle window/screen resize events.
     *
     * @param resize resize event
     */
    @Override
    public void onResize(final TResizeEvent resize) {
        if (resize.getType() == TResizeEvent.Type.WIDGET) {
            if (terminal != null) {
                terminal.onResize(new TResizeEvent(resize.getBackend(),
                        TResizeEvent.Type.WIDGET, getWidth() - 2,
                        getHeight() - 2));
            }

            // Resize the scroll bars
            reflowData();
            placeScrollbars();
        }
        return;
    }

    /**
     * Resize scrollbars for a new width/height.
     */
    @Override
    public void reflowData() {
        // Vertical scrollbar
        if (terminal != null) {
            terminal.reflowData();
            setTopValue(terminal.getTopValue());
            setBottomValue(terminal.getBottomValue());
            setVerticalBigChange(terminal.getVerticalBigChange());
            setVerticalValue(terminal.getVerticalValue());
        }
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        // We have to match the keystroke on the status bar here, because
        // otherwise the emulator will get it.
        if (keypress.equals(kbShiftF10)) {
            getApplication().postEvent(new TCommandEvent(
                keypress.getBackend(), cmMenu));
            return;
        }

        if ((terminal != null)
            && (terminal.isReading())
            && (!inKeyboardResize)
        ) {
            terminal.onKeypress(keypress);
        } else {
            super.onKeypress(keypress);
        }
    }

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if (inWindowMove || inWindowResize) {
            // TWindow needs to deal with this.
            super.onMouseDown(mouse);
            return;
        }

        super.onMouseDown(mouse);
    }

    /**
     * Handle mouse release events.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        if (inWindowMove || inWindowResize) {
            // TWindow needs to deal with this.
            super.onMouseUp(mouse);
            return;
        }

        super.onMouseUp(mouse);

        if (mouse.isMouse1() && mouseOnVerticalScroller(mouse)) {
            // Clicked on vertical scrollbar
            if (terminal != null) {
                terminal.setVerticalValue(getVerticalValue());
            }
        }
    }

    /**
     * Handle mouse motion events.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        if (inWindowMove || inWindowResize) {
            // TWindow needs to deal with this.
            super.onMouseMotion(mouse);
            return;
        }

        super.onMouseMotion(mouse);

        if (mouse.isMouse1() && mouseOnVerticalScroller(mouse)) {
            // Clicked/dragged on vertical scrollbar
            if (terminal != null) {
                terminal.setVerticalValue(getVerticalValue());
            }
        }
    }

    /**
     * Get this window's help topic to load.
     *
     * @return the topic name
     */
    @Override
    public String getHelpTopic() {
        return "Terminal Window";
    }

    /**
     * Called by application.switchWindow() when this window gets the
     * focus.
     */
    @Override
    protected void onFocus() {
        if (terminal != null) {
            terminal.onFocus();
        }
    }

    /**
     * Called by application.switchWindow() when another window gets the
     * focus.
     */
    @Override
    protected void onUnfocus() {
        if (terminal != null) {
            terminal.onUnfocus();
        }
    }

    // ------------------------------------------------------------------------
    // TTerminalWindow --------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the full command line that spawned the shell.
     *
     * @return the command line
     */
    public String [] getCommandLine() {
        return terminal.getCommandLine();
    }

    /**
     * Returns true if this window does not want the application-wide mouse
     * pointer drawn over it.
     *
     * @return true if this window does not want the application-wide mouse
     * pointer drawn over it
     */
    @Override
    public boolean hasHiddenMouse() {
        if (terminal != null) {
            return terminal.hasHiddenMouse();
        }
        return false;
    }

    /**
     * Claim the keystrokes the emulator will need.
     */
    private void addShortcutKeys() {
        addShortcutKeypress(kbCtrlA);
        addShortcutKeypress(kbCtrlB);
        addShortcutKeypress(kbCtrlC);
        addShortcutKeypress(kbCtrlD);
        addShortcutKeypress(kbCtrlE);
        addShortcutKeypress(kbCtrlF);
        addShortcutKeypress(kbCtrlG);
        addShortcutKeypress(kbCtrlH);
        addShortcutKeypress(kbCtrlU);
        addShortcutKeypress(kbCtrlJ);
        addShortcutKeypress(kbCtrlK);
        addShortcutKeypress(kbCtrlL);
        addShortcutKeypress(kbCtrlM);
        addShortcutKeypress(kbCtrlN);
        addShortcutKeypress(kbCtrlO);
        addShortcutKeypress(kbCtrlP);
        addShortcutKeypress(kbCtrlQ);
        addShortcutKeypress(kbCtrlR);
        addShortcutKeypress(kbCtrlS);
        addShortcutKeypress(kbCtrlT);
        addShortcutKeypress(kbCtrlU);
        addShortcutKeypress(kbCtrlV);
        addShortcutKeypress(kbCtrlW);
        addShortcutKeypress(kbCtrlX);
        addShortcutKeypress(kbCtrlY);
        addShortcutKeypress(kbCtrlZ);
        addShortcutKeypress(kbF1);
        addShortcutKeypress(kbF2);
        addShortcutKeypress(kbF3);
        addShortcutKeypress(kbF4);
        addShortcutKeypress(kbF5);
        addShortcutKeypress(kbF6);
        addShortcutKeypress(kbF7);
        addShortcutKeypress(kbF8);
        addShortcutKeypress(kbF9);
        addShortcutKeypress(kbF10);
        addShortcutKeypress(kbF11);
        addShortcutKeypress(kbF12);
        addShortcutKeypress(kbAltA);
        addShortcutKeypress(kbAltB);
        addShortcutKeypress(kbAltC);
        addShortcutKeypress(kbAltD);
        addShortcutKeypress(kbAltE);
        addShortcutKeypress(kbAltF);
        addShortcutKeypress(kbAltG);
        addShortcutKeypress(kbAltH);
        addShortcutKeypress(kbAltU);
        addShortcutKeypress(kbAltJ);
        addShortcutKeypress(kbAltK);
        addShortcutKeypress(kbAltL);
        addShortcutKeypress(kbAltM);
        addShortcutKeypress(kbAltN);
        addShortcutKeypress(kbAltO);
        addShortcutKeypress(kbAltP);
        addShortcutKeypress(kbAltQ);
        addShortcutKeypress(kbAltR);
        addShortcutKeypress(kbAltS);
        addShortcutKeypress(kbAltT);
        addShortcutKeypress(kbAltU);
        addShortcutKeypress(kbAltV);
        addShortcutKeypress(kbAltW);
        addShortcutKeypress(kbAltX);
        addShortcutKeypress(kbAltY);
        addShortcutKeypress(kbAltZ);
    }

    /**
     * Hook for subclasses to be notified of the shell termination.
     */
    public void onShellExit() {
        if (closeOnExit) {
            close();
        }
        clearShortcutKeypresses();
        getApplication().postEvent(new TMenuEvent(null, TMenu.MID_REPAINT));
    }

    /**
     * Wait for a period of time to get output from the launched process.
     *
     * @param millis millis to wait for, or 0 to wait forever
     * @return true if the launched process has emitted something
     */
    public boolean waitForOutput(final int millis) {
        if (terminal == null) {
            return false;
        }
        return terminal.waitForOutput(millis);
    }

    /**
     * Get the exit value for the emulator.
     *
     * @return exit value
     */
    public int getExitValue() {
        if (terminal == null) {
            return -1;
        }
        return terminal.getExitValue();
    }

    /**
     * Set window title.
     *
     * @param title new window title
     * @param override if true, force the title to never change regardless of
     * the desired title from the running terminal
     */
    public final void setTitle(final String title, final boolean override) {
        super.setTitle(title);
        this.titleOverride = override;
    }

}
