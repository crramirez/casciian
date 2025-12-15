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
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import casciian.bits.CellAttributes;

/**
 * This class uses an xterm/ANSI X3.64/ECMA-48 type terminal to provide a
 * screen, keyboard, and mouse to TApplication.
 */
public class ECMA48Backend extends GenericBackend {

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor will use System.in and System.out and UTF-8
     * encoding. On non-Windows systems System.in will be put in raw mode;
     * shutdown() will (blindly!) put System.in in cooked mode.
     *
     * @throws UnsupportedEncodingException if an exception is thrown when
     * creating the InputStreamReader
     */
    public ECMA48Backend() throws UnsupportedEncodingException {
        this(null, null, null);
    }

    /**
     * Public constructor.
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param input an InputStream connected to the remote user, or null for
     * System.in.  If System.in is used, then on non-Windows systems it will
     * be put in raw mode; shutdown() will (blindly!) put System.in in cooked
     * mode.  input is always converted to a Reader with UTF-8 encoding.
     * @param output an OutputStream connected to the remote user, or null
     * for System.out.  output is always converted to a Writer with UTF-8
     * encoding.
     * @param windowWidth the number of text columns to start with
     * @param windowHeight the number of text rows to start with
     * @param fontSize the size in points.  ECMA48 cannot set it, but it is
     * here to match the Swing API.
     * @throws UnsupportedEncodingException if an exception is thrown when
     * creating the InputStreamReader
     */
    @SuppressWarnings("this-escape")
    public ECMA48Backend(final Object listener, final InputStream input,
        final OutputStream output, final int windowWidth,
        final int windowHeight, final int fontSize)
        throws UnsupportedEncodingException {

        // Create a terminal and explicitly set stdin into raw mode
        terminal = new ECMA48Terminal(this, listener, input, output,
            windowWidth, windowHeight);
        ((ECMA48Terminal) terminal).setBackend(this);

        // Keep the terminal's sessionInfo so that TApplication can see it
        sessionInfo = ((ECMA48Terminal) terminal).getSessionInfo();

        // ECMA48Terminal is the screen too
        screen = (ECMA48Terminal) terminal;
    }

    /**
     * Public constructor.
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
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
    public ECMA48Backend(final Object listener, final InputStream input,
        final OutputStream output) throws UnsupportedEncodingException {

        this(listener, input, output, false);
    }

    /**
     * Public constructor.
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param input an InputStream connected to the remote user, or null for
     * System.in.  If System.in is used, then on non-Windows systems it will
     * be put in raw mode; shutdown() will (blindly!) put System.in in cooked
     * mode.  input is always converted to a Reader with UTF-8 encoding.
     * @param output an OutputStream connected to the remote user, or null
     * for System.out.  output is always converted to a Writer with UTF-8
     * encoding.
     * @param readOnly if true, start this backend as read-only
     * @throws UnsupportedEncodingException if an exception is thrown when
     * creating the InputStreamReader
     */
    @SuppressWarnings("this-escape")
    public ECMA48Backend(final Object listener, final InputStream input,
        final OutputStream output,
        final boolean readOnly) throws UnsupportedEncodingException {

        this.readOnly = readOnly;

        // Create a terminal and explicitly set stdin into raw mode
        terminal = new ECMA48Terminal(this, listener, input, output);
        ((ECMA48Terminal) terminal).setBackend(this);

        // Keep the terminal's sessionInfo so that TApplication can see it
        sessionInfo = ((ECMA48Terminal) terminal).getSessionInfo();

        // ECMA48Terminal is the screen too
        screen = (ECMA48Terminal) terminal;
    }

    /**
     * Public constructor.
     *
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
    public ECMA48Backend(final Object listener, final InputStream input,
        final Reader reader, final PrintWriter writer,
        final boolean setRawMode) {

        // Create a terminal and explicitly set stdin into raw mode
        terminal = new ECMA48Terminal(this, listener, input, reader, writer,
            setRawMode);
        ((ECMA48Terminal) terminal).setBackend(this);

        // Keep the terminal's sessionInfo so that TApplication can see it
        sessionInfo = ((ECMA48Terminal) terminal).getSessionInfo();

        // ECMA48Terminal is the screen too
        screen = (ECMA48Terminal) terminal;
    }

    /**
     * Public constructor.
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param input the InputStream underlying 'reader'.  Its available()
     * method is used to determine if reader.read() will block or not.
     * @param reader a Reader connected to the remote user.
     * @param writer a PrintWriter connected to the remote user.
     * @throws IllegalArgumentException if input, reader, or writer are null.
     */
    public ECMA48Backend(final Object listener, final InputStream input,
        final Reader reader, final PrintWriter writer) {

        this(listener, input, reader, writer, false);
    }

    /**
     * Convert a CellAttributes foreground color to an RGB color.
     *
     * @param attr the text attributes
     * @return the RGB color
     */
    public int attrToForegroundColor(final CellAttributes attr) {
        return ECMA48Terminal.attrToForegroundColor(attr);
    }

    /**
     * Convert a CellAttributes background color to an RGB color.
     *
     * @param attr the text attributes
     * @return the RGB color
     */
    public int attrToBackgroundColor(final CellAttributes attr) {
        return ECMA48Terminal.attrToBackgroundColor(attr);
    }

    /**
     * Copy text to the system clipboard of the terminal on the backend.  Not
     * all terminals support this (OSC 52).
     *
     * @param text string to copy
     */
    public void copyClipboardText(final String text) {
        ((ECMA48Terminal) terminal).xtermSetClipboardText(text);
    }

    /**
     * Get window/terminal system focus.
     *
     * @return true if this backend has the mouse/keyboard focus
     */
    public boolean isFocused() {
        return ((ECMA48Terminal) terminal).isFocused();
    }

    /**
     * Retrieve the default foreground color.
     *
     * @return the RGB color
     */
    public int getDefaultForeColorRGB() {
        return ((ECMA48Terminal) terminal).getDefaultForeColorRGB();
    }

    /**
     * Retrieve the default background color.
     *
     * @return the RGB color
     */
    public int getDefaultBackColorRGB() {
        return ((ECMA48Terminal) terminal).getDefaultBackColorRGB();
    }

}
