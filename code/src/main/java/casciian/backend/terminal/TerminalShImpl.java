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

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

/**
 * Shell-based terminal implementation using stty commands.
 * 
 * <p>This implementation uses /bin/sh and stty commands to set terminal
 * attributes. This is the traditional Unix approach and works on Linux
 * and macOS but will fail silently on Windows (where /bin/sh doesn't exist).
 */
public class TerminalShImpl implements Terminal {

    /**
     * Default window width.
     */
    private static final int DEFAULT_WIDTH = 80;

    /**
     * Default window height.
     */
    private static final int DEFAULT_HEIGHT = 24;

    /**
     * If true, print debug output to stderr.
     */
    private final boolean debugToStderr;

    /**
     * The input stream for this terminal.
     */
    private final InputStream inputStream;

    /**
     * The reader for this terminal.
     */
    private final Reader reader;

    /**
     * The writer for this terminal.
     */
    private final PrintWriter writer;

    /**
     * Text window width.
     */
    private int windowWidth = DEFAULT_WIDTH;

    /**
     * Text window height.
     */
    private int windowHeight = DEFAULT_HEIGHT;

    /**
     * Create a new shell-based terminal implementation.
     *
     * @param input the input stream, or null for system input (FileDescriptor.in)
     * @param output the output stream, or null for system output (System.out)
     * @param debugToStderr if true, print debug output to stderr
     */
    public TerminalShImpl(InputStream input, OutputStream output, boolean debugToStderr) {
        this.debugToStderr = debugToStderr;

        // Set up input stream
        if (input == null) {
            this.inputStream = new FileInputStream(FileDescriptor.in);
        } else {
            this.inputStream = input;
        }
        this.reader = new InputStreamReader(this.inputStream, StandardCharsets.UTF_8);

        // Set up output writer
        if (output == null) {
            this.writer = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
        } else {
            this.writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
        }
    }

    /**
     * Create a new shell-based terminal implementation with pre-wired Reader and PrintWriter.
     * 
     * <p>This constructor is used when the caller already has Reader and PrintWriter
     * objects (e.g., from a telnet connection) and wants to use them directly.
     *
     * @param input the input stream (must not be null)
     * @param reader the reader (must not be null)
     * @param writer the print writer (must not be null)
     * @param debugToStderr if true, print debug output to stderr
     */
    public TerminalShImpl(InputStream input, Reader reader, PrintWriter writer, boolean debugToStderr) {
        this.debugToStderr = debugToStderr;
        this.inputStream = input;
        this.reader = reader;
        this.writer = writer;
    }

    /**
     * Set the terminal to raw mode using stty commands.
     * In raw mode, input is available character-by-character, echoing is disabled,
     * and special processing of input and output is disabled.
     */
    @Override
    public void setRawMode() {
        doStty(true);
    }

    /**
     * Restore the terminal to cooked mode using stty commands.
     * Cooked mode restores sane terminal settings, including
     * line buffering, echoing, and special character processing.
     */
    @Override
    public void setCookedMode() {
        doStty(false);
    }

    /**
     * Close the terminal.
     * This implementation does not close System.in/out streams as the caller
     * is responsible for managing their lifecycle.
     */
    @Override
    public void close() {
        // We don't close System.in/out if we created them from null
        // The caller is responsible for managing the lifecycle
    }

    /**
     * Get the writer for writing to the terminal output.
     *
     * @return a PrintWriter for terminal output
     */
    @Override
    public PrintWriter getWriter() {
        return writer;
    }

    /**
     * Get the reader for reading from the terminal input.
     *
     * @return a Reader for terminal input
     */
    @Override
    public Reader getReader() {
        return reader;
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
     * Enable or disable mouse event reporting.
     * When enabled, the terminal will report mouse events using xterm control sequences.
     *
     * @param on if true, enable mouse reporting; if false, disable it
     */
    @Override
    public void enableMouseReporting(boolean on) {
        if (writer != null) {
            writer.printf("%s", mouse(on));
        }
    }

    /**
     * Return the number of bytes available to be read from the input stream.
     *
     * @return the number of bytes available, or 0 if none are available
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    /**
     * Read characters from the terminal input into a buffer.
     *
     * @param buffer the character array to read data into
     * @param off the starting offset in the buffer
     * @param len the maximum number of characters to read
     * @return the number of characters read, or -1 if end of stream is reached
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(char[] buffer, int off, int len) throws IOException {
        return reader.read(buffer, off, len);
    }

    /**
     * Call stty to set raw or cooked mode.
     *
     * @param mode if true, set raw mode, otherwise set cooked mode
     */
    private void doStty(final boolean mode) {
        String[] cmdRaw = {
            "/bin/sh", "-c", "stty -ignbrk -brkint -parmrk -istrip -inlcr -igncr -icrnl -ixon -opost -echo -echonl -icanon -isig -iexten -parenb cs8 min 1 < /dev/tty"
        };
        String[] cmdCooked = {
            "/bin/sh", "-c", "stty sane cooked < /dev/tty"
        };
        try {
            Process process;
            if (mode) {
                process = Runtime.getRuntime().exec(cmdRaw);
            } else {
                process = Runtime.getRuntime().exec(cmdCooked);
            }
            try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                 BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line = in.readLine();
                if ((line != null) && (line.length() > 0)) {
                    System.err.println("WEIRD?! Normal output from stty: " + line);
                }
                while (true) {
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
            }
            int rc = process.exitValue();
            if (rc != 0) {
                System.err.println("stty returned error code: " + rc);
            }
        } catch (IOException e) {
            if (debugToStderr) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Query the terminal window size using 'stty size'.
     * This method calls the stty command to get the current terminal dimensions.
     */
    @Override
    public void queryWindowSize() {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Linux")
            || osName.startsWith("Mac OS X")
            || osName.startsWith("Darwin")
            || osName.startsWith("SunOS")
            || osName.startsWith("FreeBSD")
        ) {
            sttyWindowSize();
        }
    }

    /**
     * Get the terminal window width in characters.
     *
     * @return the window width
     */
    @Override
    public int getWindowWidth() {
        return windowWidth;
    }

    /**
     * Get the terminal window height in characters.
     *
     * @return the window height
     */
    @Override
    public int getWindowHeight() {
        return windowHeight;
    }

    /**
     * Call 'stty size' to obtain the tty window size.
     * windowWidth and windowHeight are set automatically.
     */
    private void sttyWindowSize() {
        String[] cmd = {
            "/bin/sh", "-c", "stty size < /dev/tty"
        };
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = in.readLine();
                if ((line != null) && (line.length() > 0)) {
                    StringTokenizer tokenizer = new StringTokenizer(line);
                    int rc = Integer.parseInt(tokenizer.nextToken());
                    if (rc > 0) {
                        windowHeight = rc;
                    }
                    rc = Integer.parseInt(tokenizer.nextToken());
                    if (rc > 0) {
                        windowWidth = rc;
                    }
                }
            }
            try (BufferedReader err = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = err.readLine()) != null) {
                    if (debugToStderr && line.length() > 0) {
                        System.err.println("Error output from stty: " + line);
                    }
                }
            }
            while (true) {
                try {
                    process.waitFor();
                    break;
                } catch (InterruptedException e) {
                    // SQUASH
                }
            }
            int rc = process.exitValue();
            if (debugToStderr && rc != 0) {
                System.err.println("stty returned error code: " + rc);
            }
        } catch (IOException e) {
            if (debugToStderr) {
                e.printStackTrace();
            }
        }
    }
}
