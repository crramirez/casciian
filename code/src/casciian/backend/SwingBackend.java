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

import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import javax.swing.JComponent;

import casciian.bits.CellAttributes;

/**
 * This class uses standard Swing calls to handle screen, keyboard, and mouse
 * I/O.
 */
public class SwingBackend extends GenericBackend {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The system clipboard, or null if it is not available.
     */
    private java.awt.datatransfer.Clipboard systemClipboard = null;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.  The window will be 80x25 with font size 20 pts.
     */
    public SwingBackend() {
        this(null, 80, 25, 20);
    }

    /**
     * Public constructor.  The window will be 80x25 with font size 20 pts.
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
     */
    public SwingBackend(final Object listener) {
        this(listener, 80, 25, 20);
    }

    /**
     * Public constructor will spawn a new JFrame with font size 20 pts.
     *
     * @param windowWidth the number of text columns to start with
     * @param windowHeight the number of text rows to start with
     */
    public SwingBackend(final int windowWidth, final int windowHeight) {
        this(null, windowWidth, windowHeight, 20);
    }

    /**
     * Public constructor will spawn a new JFrame.
     *
     * @param windowWidth the number of text columns to start with
     * @param windowHeight the number of text rows to start with
     * @param fontSize the size in points.  Good values to pick are: 16, 20,
     * 22, and 24.
     */
    public SwingBackend(final int windowWidth, final int windowHeight,
        final int fontSize) {

        this(null, windowWidth, windowHeight, fontSize);
    }

    /**
     * Public constructor will spawn a new JFrame.
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param windowWidth the number of text columns to start with
     * @param windowHeight the number of text rows to start with
     * @param fontSize the size in points.  Good values to pick are: 16, 20,
     * 22, and 24.
     */
    @SuppressWarnings("this-escape")
    public SwingBackend(final Object listener, final int windowWidth,
        final int windowHeight, final int fontSize) {

        // Create a Swing backend using a JFrame
        terminal = new SwingTerminal(this, windowWidth, windowHeight, fontSize,
            listener);
        ((SwingTerminal) terminal).setBackend(this);

        // Hang onto the session info
        this.sessionInfo = ((SwingTerminal) terminal).getSessionInfo();

        // SwingTerminal is the screen too
        screen = (SwingTerminal) terminal;
    }

    /**
     * Public constructor will render onto a JComponent.
     *
     * @param component the Swing component to render to
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param windowWidth the number of text columns to start with
     * @param windowHeight the number of text rows to start with
     * @param fontSize the size in points.  Good values to pick are: 16, 20,
     * 22, and 24.
     */
    @SuppressWarnings("this-escape")
    public SwingBackend(final JComponent component, final Object listener,
        final int windowWidth, final int windowHeight, final int fontSize) {

        // Create a Swing backend using a JComponent
        terminal = new SwingTerminal(this, component, windowWidth, windowHeight,
            fontSize, listener);
        ((SwingTerminal) terminal).setBackend(this);

        // Hang onto the session info
        this.sessionInfo = ((SwingTerminal) terminal).getSessionInfo();

        // SwingTerminal is the screen too
        screen = (SwingTerminal) terminal;
    }

    // ------------------------------------------------------------------------
    // SwingBackend -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set to a new font, and resize the screen to match its dimensions.
     *
     * @param font the new font
     */
    public void setFont(final Font font) {
        ((SwingTerminal) terminal).setFont(font);
    }

    /**
     * Get the number of millis to wait before switching the blink from
     * visible to invisible.
     *
     * @return the number of milli to wait before switching the blink from
     * visible to invisible
     */
    public long getBlinkMillis() {
        return ((SwingTerminal) terminal).getBlinkMillis();
    }

    /**
     * Getter for the underlying Swing component.
     *
     * @return the SwingComponent
     */
    public SwingComponent getSwingComponent() {
        return ((SwingTerminal) terminal).getSwingComponent();
    }

    /**
     * Set the mouse pointer (cursor) style.
     *
     * @param mouseStyle the pointer style string, one of: "default", "none",
     * "hand", "text", "move", or "crosshair"
     */
    @Override
    public void setMouseStyle(final String mouseStyle) {
        ((SwingTerminal) terminal).setMouseStyle(mouseStyle);
    }

    /**
     * Convert a CellAttributes foreground color to an AWT Color.
     *
     * @param attr the text attributes
     * @return the AWT Color
     */
    public java.awt.Color attrToForegroundColor(final CellAttributes attr) {
        // Use Swing colors.
        return SwingTerminal.attrToForegroundColor(attr);
    }

    /**
     * Convert a CellAttributes background color to an AWT Color.
     *
     * @param attr the text attributes
     * @return the AWT Color
     */
    public java.awt.Color attrToBackgroundColor(final CellAttributes attr) {
        // Use Swing colors.
        return SwingTerminal.attrToBackgroundColor(attr);
    }

    /**
     * Copy text to the AWT system clipboard.
     *
     * @param text string to copy
     */
    public void copyClipboardText(final String text) {
        if (systemClipboard == null) {
            try {
                systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            } catch (java.awt.HeadlessException e) {
                // SQUASH
            }
        }

        if (systemClipboard != null) {
            StringSelection stringSelection = new StringSelection(text);
            systemClipboard.setContents(stringSelection, null);
        }
    }

    /**
     * Get window/terminal system focus.
     *
     * @return true if this backend has the mouse/keyboard focus
     */
    public boolean isFocused() {
        return ((SwingTerminal) terminal).isFocused();
    }

}
