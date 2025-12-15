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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import casciian.bits.Cell;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import casciian.event.TResizeEvent;
import casciian.terminal.DisplayLine;
import casciian.terminal.ECMA48;
import casciian.terminal.TerminalListener;
import casciian.terminal.TerminalState;
import static casciian.TKeypress.*;


/**
 * TTextPicture displays a color-and-text canvas, also called "ANSI Art" or
 * "ASCII Art".
 */
public class TTextPicture extends TScrollable implements TerminalListener {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The terminal containing the display.
     */
    private ECMA48 terminal;

    /**
     * If true, the terminal is not reading and is closed.
     */
    private boolean terminalClosed = true;

    /**
     * The last seen terminal state.
     */
    private TerminalState terminalState;

    /**
     * Update(s) from the terminal.
     */
    private List<TerminalState> dirtyQueue = new ArrayList<TerminalState>();

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param filename the file containing the picture data
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     */
    @SuppressWarnings("this-escape")
    public TTextPicture(final TWidget parent, final String filename,
        final int x, final int y, final int width, final int height) {

        // Set parent and window
        super(parent, x, y, width, height);

        try {
            terminal = new ECMA48(ECMA48.DeviceType.XTERM,
                new FileInputStream(filename), new ByteArrayOutputStream(),
                this, getApplication().getBackend());

            terminalClosed = false;
        } catch (FileNotFoundException e) {
            // SQUASH
            terminal = null;
        } catch (UnsupportedEncodingException e) {
            // SQUASH
            terminal = null;
        }

        // We will have scrollers for data fields and mouse event handling,
        // but do not want to draw it.
        vScroller = new TVScroller(null, getWidth(), 0, getHeight());
        vScroller.setVisible(false);
        setBottomValue(0);
        hScroller = new THScroller(null, 0, getHeight() - 1,
            Math.max(1, getWidth() - 1));
        hScroller.setVisible(false);
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

        if (terminal == null) {
            return;
        }

        synchronized (terminal) {
            if (resize.getType() == TResizeEvent.Type.WIDGET) {
                // Resize the scroll bars
                reflowData();
                placeScrollbars();

                // Get out of scrollback
                setVerticalValue(0);

                terminal.setWidth(getWidth());
                terminal.setHeight(getHeight());
            }
        } // synchronized (emulator)
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
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

        super.onKeypress(keypress);
    }

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if (mouse.isMouseWheelUp()) {
            verticalDecrement();
            return;
        }
        if (mouse.isMouseWheelDown()) {
            verticalIncrement();
            return;
        }
        super.onMouseDown(mouse);
    }

    // ------------------------------------------------------------------------
    // TScrollable ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle widget close.
     */
    @Override
    public void close() {
        if (terminal != null) {
            terminal.close();
        }
    }

    /**
     * Resize scrollbars for a new width/height.
     */
    @Override
    public void reflowData() {
        if (terminal == null) {
            return;
        }

        if (terminalState == null) {
            synchronized (terminal) {
                terminalState = terminal.captureState();
            }
        }

        // Vertical scrollbar
        setTopValue(getHeight()
            - (terminalState.getScrollbackBuffer().size()
                + terminalState.getDisplayBuffer().size()));
        setVerticalBigChange(getHeight());
    }

    // ------------------------------------------------------------------------
    // TerminalListener -------------------------------------------------------
    // -----------------------------------------------------------------------

    /**
     * Ensure terminal state is known.
     */
    private void checkTerminalState() {
        assert (terminal != null);
        if (terminalState == null) {
            synchronized (terminal) {
                terminalState = terminal.captureState();
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
        return getWidth();
    }

    /**
     * Function to call to obtain the display height.
     *
     * @return the number of rows in the display
     */
    public int getDisplayHeight() {
        return getHeight();
    }

    // ------------------------------------------------------------------------
    // TTextPicture -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the text box.
     */
    @Override
    public void draw() {
        if (terminal == null) {
            return;
        }

        // Check to see if the shell has died.
        if (!terminalClosed && !terminal.isReading()) {
            try {
                terminal.close();
                terminalClosed = true;
            } catch (IllegalThreadStateException e) {
                // SQUASH
            }
        }

        int width = 80;
        int left = 0;
        List<DisplayLine> display = null;
        checkTerminalState();

        // Update the scroll bars
        reflowData();

        display = terminalState.getVisibleDisplay(getHeight(),
            -getVerticalValue());
        assert (display.size() == getHeight());
        width = terminalState.getWidth();
        left = getHorizontalValue();

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
                Cell ch = line.charAt(i + left);

                Cell newCell = new Cell(ch);
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
     * Draw glyphs for a double-width or double-height VT100 cell to two
     * screen cells.
     *
     * @param line the line this VT100 cell is in
     * @param x the X position to draw the left half to
     * @param y the Y position to draw to
     * @param cell the cell to draw
     */
    private void putDoubleWidthCharXY(final DisplayLine line, final int x,
        final int y, final Cell cell) {

        putCharXY(x, y, cell);
        putCharXY(x + 1, y, ' ', cell);
    }

}
