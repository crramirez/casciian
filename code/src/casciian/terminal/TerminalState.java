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
package casciian.terminal;

import casciian.bits.CellAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * This represents the full displayable state of the ECMA38 terminal.
 */
public class TerminalState {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Current text attributes.
     */
    private CellAttributes attr;

    /**
     * Physical display width.
     */
    private int width = 80;

    /**
     * Physical display height.
     */
    private int height = 24;

    /**
     * The portion of the display that has scrolled out of view.
     */
    private List<DisplayLine> scrollback;

    /**
     * The portion of the display is within view.
     */
    private List<DisplayLine> display;

    /**
     * Visible cursor (DECTCEM).
     */
    private boolean cursorVisible = true;

    /**
     * Current cursor X.
     */
    private int cursorX;

    /**
     * Current cursor Y.
     */
    private int cursorY;

    /**
     * If true, the terminal has requested the mouse pointer be hidden.
     */
    private boolean hideMousePointer;

    /**
     * The mouse protocol, one of MouseProtocol.OFF, MouseProtocol.X10, etc.
     */
    private ECMA48.MouseProtocol mouseProtocol;

    /**
     * The screen title as set by the xterm OSC sequence.  Lots of
     * applications send a screenTitle regardless of whether it is an xterm
     * client or not.
     */
    private String screenTitle = "";

    /**
     * If true, the remote side has requested a synchronized update.
     */
    private boolean withinSynchronizedUpdate = false;

    /**
     * The last display returned from getVisibleDisplay().
     */
    private List<DisplayLine> lastVisibleDisplay;

    /**
     * The last time we returned lastVisibleDisplay.
     */
    private long lastVisibleUpdateTime;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Package private constructor.
     *
     * @param attr current text attributes
     * @param width the display width
     * @param height the display height
     * @param scrollBuffer the portion of the display that has scrolled out
     * of view.
     * @param displayBuffer the portion of the display is within view
     * @param cursorVisible if true, the cursor is visible
     * @param cursorX current cursor X
     * @param cursorY current cursor Y
     * @param hideMousePointer if true, the terminal has requested the mouse
     * pointer be hidden
     * @param mouseProtocol the mouse protocol, one of MouseProtocol.OFF,
     * MouseProtocol.X10, etc.
     * @param screenTitle the screen title as set by the xterm OSC sequence
     * @param withinSynchronizedUpdate true if the remote side has requested
     * a synchronized update
     * @param lastVisibleDisplay the last display returned from
     * getVisibleDisplay()
     * @param lastVisibleUpdateTime the last time we returned
     * lastVisibleDisplay
     */
    TerminalState(final CellAttributes attr, final int width, final int height,
        final List<DisplayLine> scrollbackBuffer,
        final List<DisplayLine> displayBuffer, final boolean cursorVisible,
        final int cursorX, final int cursorY, final boolean hideMousePointer,
        final ECMA48.MouseProtocol mouseProtocol, final String screenTitle,
        final boolean withinSynchronizedUpdate,
        final List<DisplayLine> lastVisibleDisplay,
        final long lastVisibleUpdateTime) {

        this.attr             = new CellAttributes(attr);
        this.width            = width;
        this.height           = height;
        this.scrollback       = copyBuffer(scrollbackBuffer);
        this.display          = copyBuffer(displayBuffer);
        this.cursorVisible    = cursorVisible;
        this.cursorX          = cursorX;
        this.cursorY          = cursorY;
        this.hideMousePointer = hideMousePointer;
        this.mouseProtocol    = mouseProtocol;
        this.screenTitle      = screenTitle;
        this.withinSynchronizedUpdate = withinSynchronizedUpdate;
        this.lastVisibleDisplay       = copyBuffer(lastVisibleDisplay);
        this.lastVisibleUpdateTime    = lastVisibleUpdateTime;
    }

    // ------------------------------------------------------------------------
    // TerminalState ----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the display width.
     *
     * @return the width (usually 80 or 132)
     */
    public final int getWidth() {
        return width;
    }

    /**
     * Get the display height.
     *
     * @return the height (usually 24)
     */
    public final int getHeight() {
        return height;
    }

    /**
     * Get visible cursor flag.
     *
     * @return if true, the cursor is visible
     */
    public final boolean isCursorVisible() {
        return cursorVisible;
    }

    /**
     * Get the portion of the display that has scrolled out of view.
     *
     * @return the scrollback buffer
     */
    public final List<DisplayLine> getScrollbackBuffer() {
        return scrollback;
    }

    /**
     * Get the portion of the display is within view.
     *
     * @return the display buffer
     */
    public final List<DisplayLine> getDisplayBuffer() {
        return display;
    }

    /**
     * Get the visible display + scrollback buffer, offset by a specified
     * number of rows from the bottom.
     *
     * @param visibleHeight the total height of the display to show
     * @param scrollBottom the number of rows from the bottom to scroll back
     * @return a copy of the display + scrollback buffers
     */
    public final List<DisplayLine> getVisibleDisplay(final int visibleHeight,
        final int scrollBottom) {

        assert (visibleHeight >= 0);
        assert (scrollBottom >= 0);

        long now = System.currentTimeMillis();

        if (withinSynchronizedUpdate
            && (lastVisibleDisplay != null)
            && (lastVisibleDisplay.size() == visibleHeight)
            && ((now - lastVisibleUpdateTime) < 125)
        ) {
            // More data is being received, and we have a usable screen from
            // before.  Use it.
            return lastVisibleDisplay;
        }

        int visibleBottom = scrollback.size() + display.size() - scrollBottom;

        List<DisplayLine> preceedingBlankLines = new ArrayList<DisplayLine>();
        int visibleTop = visibleBottom - visibleHeight;
        if (visibleTop < 0) {
            for (int i = visibleTop; i < 0; i++) {
                preceedingBlankLines.add(getBlankDisplayLine());
            }
            visibleTop = 0;
        }
        assert (visibleTop >= 0);

        List<DisplayLine> displayLines = new ArrayList<DisplayLine>();
        displayLines.addAll(scrollback);
        displayLines.addAll(display);

        List<DisplayLine> visibleLines = new ArrayList<DisplayLine>();
        visibleLines.addAll(preceedingBlankLines);
        visibleLines.addAll(displayLines.subList(visibleTop,
                Math.min(visibleBottom, displayLines.size())));

        // Fill in the blank lines on bottom
        int bottomBlankLines = visibleHeight - visibleLines.size();
        assert (bottomBlankLines >= 0);
        for (int i = 0; i < bottomBlankLines; i++) {
            visibleLines.add(getBlankDisplayLine());
        }

        return visibleLines;
    }

    /**
     * Copy a display buffer.
     *
     * @param buffer the buffer to copy
     * @return a deep copy of the buffer's data
     */
    private List<DisplayLine> copyBuffer(final List<DisplayLine> buffer) {
        if (buffer == null) {
            return null;
        }
        List<DisplayLine> result = new ArrayList<DisplayLine>(buffer.size());
        for (DisplayLine line: buffer) {
            result.add(new DisplayLine(line));
        }
        return result;
    }

    /**
     * Expose current cursor X to outside world.
     *
     * @return current cursor X
     */
    public int getCursorX() {
        return cursorX;
    }

    /**
     * Expose current cursor Y to outside world.
     *
     * @return current cursor Y
     */
    public int getCursorY() {
        return cursorY;
    }

    /**
     * Returns true if this terminal has requested the mouse pointer be
     * hidden.
     *
     * @return true if this terminal has requested the mouse pointer be
     * hidden
     */
    public boolean hasHiddenMousePointer() {
        return hideMousePointer;
    }

    /**
     * Get the mouse protocol.
     *
     * @return MouseProtocol.OFF, MouseProtocol.X10, etc.
     */
    public ECMA48.MouseProtocol getMouseProtocol() {
        return mouseProtocol;
    }

    /**
     * Get the screen title as set by the xterm OSC sequence.  Lots of
     * applications send a screenTitle regardless of whether it is an xterm
     * client or not.
     *
     * @return screen title
     */
    public String getScreenTitle() {
        return screenTitle;
    }

    /**
     * Obtain a new blank display line for an external user
     * (e.g. TTerminalWindow).
     *
     * @return new blank line
     */
    private DisplayLine getBlankDisplayLine() {
        return new DisplayLine(attr);
    }

}
