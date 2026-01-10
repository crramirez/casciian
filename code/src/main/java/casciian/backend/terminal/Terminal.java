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

import java.io.InputStream;
import java.io.PrintWriter;

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
     * Some implementations (like JLine on Windows) provide their own writer
     * for proper Unicode support.
     *
     * @return the PrintWriter for terminal output, or null if the default should be used
     */
    PrintWriter getWriter();

    /**
     * Check if this terminal provides a custom writer.
     *
     * @return true if getWriter() returns a non-null writer
     */
    boolean hasCustomWriter();

    /**
     * Get the input stream to use for terminal input.
     * Some implementations (like JLine on Windows) provide their own input stream
     * for proper handling.
     *
     * @return the InputStream for terminal input, or null if the default should be used
     */
    InputStream getInputStream();

    /**
     * Check if this terminal provides a custom input stream.
     *
     * @return true if getInputStream() returns a non-null stream
     */
    boolean hasCustomInputStream();
}
