/*
 * Casciian - Java Text User Interface
 *
 * Copyright 2025 Carlos Rafael Ramirez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package casciian.backend.terminal;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.jline.terminal.Attributes;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

/**
 * JLine-based terminal implementation for raw/cooked mode handling.
 *
 * <p>This implementation uses JLine's terminal abstraction to set terminal
 * attributes in a platform-agnostic way. On Windows, JLine uses native
 * Windows Console APIs (WriteConsoleW) for proper Unicode support.
 */
@SuppressWarnings("CallToPrintStackTrace")
public class TerminalJlineImpl implements Terminal {

    /**
     * The timeout for non-blocking reads in milliseconds.
     */
    public static final long TIMEOUT = 20L;

    /**
     * The JLine terminal instance.
     */
    private final org.jline.terminal.Terminal jlineTerminal;

    /**
     * The original terminal attributes to restore when switching back to cooked mode.
     */
    private final Attributes originalAttributes;

    /**
     * If true, print debug output to stderr.
     */
    private final boolean debugToStderr;

    /**
     * Create a new JLine terminal implementation.
     * The JLine terminal is created immediately in the constructor.
     *
     * @param debugToStderr if true, print debug output to stderr
     */
    public TerminalJlineImpl(boolean debugToStderr) {
        this.debugToStderr = debugToStderr;
        org.jline.terminal.Terminal tempTerminal = null;
        try {
            tempTerminal = TerminalBuilder.builder()
                .system(true)
                .encoding(StandardCharsets.UTF_8)
                .build();

            // Save original attributes for later restoration
            originalAttributes = new Attributes(tempTerminal.getAttributes());
            
            // Only assign to field after all initialization succeeds
            jlineTerminal = tempTerminal;
        } catch (IOException | RuntimeException e) {
            // Clean up partially initialized terminal on failure
            if (tempTerminal != null) {
                try {
                    tempTerminal.close();
                } catch (IOException closeEx) {
                    // Ignore close exception
                }
            }
            if (debugToStderr) {
                e.printStackTrace();
            }

            throw new RuntimeException("Failed to initialize JLine terminal", e);
        }
    }

    /**
     * Set the terminal to raw mode.
     * In raw mode, input is available character-by-character, echoing is disabled,
     * and special processing of input and output is disabled.
     *
     * @throws IllegalStateException if the terminal is not initialized
     */
    @Override
    public void setRawMode() {
        if (jlineTerminal == null || originalAttributes == null) {
            throw new IllegalStateException("Terminal not initialized");
        }

        // Create raw mode attributes based on the saved original attributes
        Attributes rawAttrs = new Attributes(originalAttributes);

        // Disable input flags: -ignbrk -brkint -parmrk -istrip -inlcr -igncr -icrnl -ixon
        rawAttrs.setInputFlag(Attributes.InputFlag.IGNBRK, false);
        rawAttrs.setInputFlag(Attributes.InputFlag.BRKINT, false);
        rawAttrs.setInputFlag(Attributes.InputFlag.PARMRK, false);
        rawAttrs.setInputFlag(Attributes.InputFlag.ISTRIP, false);
        rawAttrs.setInputFlag(Attributes.InputFlag.INLCR, false);
        rawAttrs.setInputFlag(Attributes.InputFlag.IGNCR, false);
        rawAttrs.setInputFlag(Attributes.InputFlag.ICRNL, false);
        rawAttrs.setInputFlag(Attributes.InputFlag.IXON, false);

        // Disable output flags: -opost
        rawAttrs.setOutputFlag(Attributes.OutputFlag.OPOST, false);

        // Disable local flags: -echo -echonl -icanon -isig -iexten
        rawAttrs.setLocalFlag(Attributes.LocalFlag.ECHO, false);
        rawAttrs.setLocalFlag(Attributes.LocalFlag.ECHONL, false);
        rawAttrs.setLocalFlag(Attributes.LocalFlag.ICANON, false);
        rawAttrs.setLocalFlag(Attributes.LocalFlag.ISIG, false);
        rawAttrs.setLocalFlag(Attributes.LocalFlag.IEXTEN, false);

        // Control flags: -parenb cs8
        rawAttrs.setControlFlag(Attributes.ControlFlag.PARENB, false);
        rawAttrs.setControlFlag(Attributes.ControlFlag.CS8, true);

        // Set VMIN=1 and VTIME=0 for immediate character-by-character input
        rawAttrs.setControlChar(Attributes.ControlChar.VMIN, 1);
        rawAttrs.setControlChar(Attributes.ControlChar.VTIME, 0);

        jlineTerminal.setAttributes(rawAttrs);
    }

    /**
     * Restore the terminal to cooked mode.
     * Cooked mode restores the original terminal attributes, including
     * line buffering, echoing, and special character processing.
     *
     * @throws IllegalStateException if the terminal is not initialized
     */
    @Override
    public void setCookedMode() {
        if (jlineTerminal == null || originalAttributes == null) {
            throw new IllegalStateException("Terminal not initialized");
        }

        jlineTerminal.setAttributes(originalAttributes);
    }

    /**
     * Close the terminal and restore its original state.
     * Clears the screen and releases any resources held by the terminal.
     */
    @Override
    public void close() {
        if (jlineTerminal != null) {
            try {
                jlineTerminal.puts(InfoCmp.Capability.clear_screen);
                jlineTerminal.close();
            } catch (IOException e) {
                if (debugToStderr) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Get the writer for writing to the terminal output.
     *
     * @return a PrintWriter for terminal output
     * @throws IllegalStateException if the terminal is not initialized
     */
    @Override
    public PrintWriter getWriter() {
        if (jlineTerminal == null) {
            throw new IllegalStateException("Terminal not initialized");
        }

        return jlineTerminal.writer();
    }

    /**
     * Get the reader for reading from the terminal input.
     *
     * @return a Reader for terminal input
     * @throws IllegalStateException if the terminal is not initialized
     */
    @Override
    public Reader getReader() {
        if (jlineTerminal == null) {
            throw new IllegalStateException("Terminal not initialized");
        }

        return jlineTerminal.reader();
    }

    /**
     * Tell (u)xterm that we want to receive mouse events based on "Any event
     * tracking", UTF-8 coordinates, and then SGR coordinates.  Ideally we
     * will end up with SGR coordinates with UTF-8 coordinates as a fallback.
     * See
     * <a href="http://invisible-island.net/xterm/ctlseqs/ctlseqs.html#Mouse%20Tracking">...</a>
     * <br>
     * Note that this also sets the alternate/primary screen buffer and
     * requests focus in/out sequences.
     * <br>
     * Finally, also emit a Privacy Message sequence that Casciian recognizes to
     * mean "hide the mouse pointer."  We have to use our own sequence to do
     * this because there is no standard in xterm for unilaterally hiding the
     * pointer all the time (regardless of typing).
     *
     * @param on If true, enable mouse report and use the alternate screen
     *           buffer.  If false disable mouse reporting and use the primary screen
     *           buffer.
     * @return the string to emit to xterm
     */
    private String mouse(final boolean on) {
        if (on) {
            return "\033[?1049h\033^hideMousePointer\033\\";
        }
        return "\033[?1049l\033^showMousePointer\033\\";
    }

    /**
     * Enable or disable mouse event reporting.
     * When enabled, the terminal will report mouse events and track focus changes.
     *
     * @param on if true, enable mouse reporting; if false, disable it
     * @throws IllegalStateException if the terminal is not initialized
     */
    @Override
    public void enableMouseReporting(boolean on) {
        if (jlineTerminal == null) {
            throw new IllegalStateException("Terminal not initialized");
        }

        if (on) {
            jlineTerminal.trackFocus(true);
            jlineTerminal.trackMouse(org.jline.terminal.Terminal.MouseTracking.Any);
        } else {
            jlineTerminal.trackFocus(false);
            jlineTerminal.trackMouse(org.jline.terminal.Terminal.MouseTracking.Off);
        }
        jlineTerminal.writer().printf("%s", mouse(on));
    }

    /**
     * Return the number of bytes available to be read from the input stream.
     * This method performs a non-blocking peek operation to determine if data is available.
     *
     * @return the number of bytes available, or 0 if none are available
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if the terminal is not initialized
     */
    @Override
    public int available() throws IOException {
        if (jlineTerminal == null) {
            throw new IllegalStateException("Terminal not initialized");
        }

        var reader = jlineTerminal.reader();
        int amount = reader.available();
        if (amount > 0) {
            return amount;
        }

        int ch = reader.peek(TIMEOUT);
        if (ch < 0) {
            return 0;
        }

        return 1;
    }

    /**
     * Read characters from the terminal input into a buffer.
     * This method performs a buffered read operation with a timeout.
     *
     * @param buffer the character array to read data into
     * @param off the starting offset in the buffer
     * @param len the maximum number of characters to read
     * @return the number of characters read, or -1 if end of stream is reached
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(char[] buffer, int off, int len) throws IOException {
        return jlineTerminal.reader().readBuffered(buffer, off, len, TIMEOUT);
    }
}
