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
import java.io.InputStream;
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
     * Get the input stream to use for terminal input.
     *
     * @return the InputStream for terminal input, never null
     */
    InputStream getInputStream();

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
     * Check if there is input available to read without blocking.
     * 
     * <p>This method provides a platform-agnostic way to check for available input.
     * On Windows with JLine, this uses Reader.ready() which properly detects
     * console input including arrow keys. On Unix systems, this typically uses
     * InputStream.available() which works reliably for terminal input.
     *
     * @return true if there is input available, false otherwise
     * @throws IOException if an I/O error occurs
     */
    boolean hasInput() throws IOException;

    /**
     * Read a single character with a timeout.
     * 
     * <p>This method provides a way to read input that doesn't rely on checking
     * availability first. On platforms where availability checking doesn't work
     * correctly (like Windows with JLine), this uses a timed read to wait for
     * input without blocking indefinitely.
     *
     * @param timeout the timeout in milliseconds; 0 means wait forever
     * @return the character read, -1 for EOF, or -2 if the timeout expired
     * @throws IOException if an I/O error occurs
     */
    int readWithTimeout(long timeout) throws IOException;
}
