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

/**
 * Interface for terminal implementations that handle raw/cooked mode switching.
 * This abstraction allows for different terminal implementations (e.g., JLine, stty)
 * to be used interchangeably.
 */
public interface Terminal {

    /**
     * Set the terminal to raw mode for character-by-character input without echo.
     */
    void setRawMode();

    /**
     * Restore the terminal to cooked (normal) mode.
     */
    void setCookedMode();

    /**
     * Close the terminal and release any resources.
     */
    void close();

    /**
     * Get the writer to use for terminal output.
     *
     * @return the PrintWriter for terminal output, never null
     */
    PrintWriter getWriter();

    /**
     * Get the reader to use for terminal input.
     *
     * @return the Reader for terminal input, never null
     */
    Reader getReader();

    /**
     * Enable or disable mouse reporting in the terminal.
     *
     * @param on If true, enable mouse reporting; if false, disable mouse reporting.
     */
    void enableMouseReporting(boolean on);

    /**
     * Returns the number of bytes that can be read from the input without blocking.
     *
     * @return the number of bytes available to read
     * @throws IOException if an I/O error occurs while determining the availability
     */
    int available() throws IOException;

    /**
     * Read characters from the terminal input into the provided buffer.
     *
     * @param buffer the buffer to read characters into
     * @param off the offset in the buffer to start writing characters
     * @param len the maximum number of characters to read
     * @return the number of characters read, or -1 if end of stream is reached
     * @throws IOException if an I/O error occurs while reading
     */
    int read(char[] buffer, int off, int len) throws IOException;

    /**
     * Whether this terminal can deliver its input as raw, undecoded bytes
     * via {@link #readBytes(byte[], int, int)}.
     *
     * <p>Raw bytes are required to decode legacy X10 mouse reports
     * ({@code ESC [ M Cb Cx Cy}), whose coordinate bytes 0x80-0xFF are not
     * valid standalone UTF-8 and would be replaced by U+FFFD if the input
     * were decoded by a UTF-8 {@link Reader} first.  When this returns true
     * the caller is expected to read <em>only</em> through
     * {@link #readBytes(byte[], int, int)} and to do its own UTF-8 decoding;
     * it must not also read from {@link #getReader()}, since both views are
     * backed by the same underlying stream.
     *
     * @return true if {@link #readBytes(byte[], int, int)} is supported
     */
    default boolean isByteInputSupported() {
        return false;
    }

    /**
     * Read raw, undecoded bytes from the terminal input.
     *
     * @param buffer the buffer to read bytes into
     * @param off the offset in the buffer to start writing bytes
     * @param len the maximum number of bytes to read
     * @return the number of bytes read (possibly 0 if a read timed out), or
     * -1 if end of stream is reached
     * @throws IOException if an I/O error occurs while reading
     * @throws UnsupportedOperationException if
     * {@link #isByteInputSupported()} returns false
     */
    default int readBytes(byte[] buffer, int off, int len) throws IOException {
        throw new UnsupportedOperationException("Raw byte input is not supported");
    }

    /**
     * Query the terminal window size.
     * This method updates the internal width and height values.
     */
    void queryWindowSize();

    /**
     * Get the terminal window width in characters.
     *
     * @return the window width, or a default value (typically 80) if unknown
     */
    int getWindowWidth();

    /**
     * Get the terminal window height in characters.
     *
     * @return the window height, or a default value (typically 24) if unknown
     */
    int getWindowHeight();
}
