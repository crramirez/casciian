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
import java.io.OutputStream;

import casciian.backend.SystemProperties;

/**
 * Factory for creating Terminal instances.
 * 
 * <p>The factory determines which implementation to use based on:
 * <ul>
 *   <li>If input and output are both null AND (OS is Windows OR casciian.useJline is true):
 *       uses JLine implementation for proper Unicode support and raw mode handling</li>
 *   <li>Otherwise: uses the stty-based shell implementation</li>
 * </ul>
 * 
 * <p>A Terminal instance is always created regardless of input/output streams,
 * allowing future delegation of terminal features like mouse tracking.
 */
public final class TerminalFactory {

    private TerminalFactory() {
        // Utility class, prevent instantiation
    }

    /**
     * Create a Terminal instance based on the current platform and configuration.
     *
     * @param input the input stream, or null for system input
     * @param output the output stream, or null for system output
     * @param debugToStderr if true, print debug output to stderr
     * @return a Terminal instance appropriate for the current environment
     */
    public static Terminal create(InputStream input, OutputStream output, boolean debugToStderr) {
        // Only use JLine when input and output are both null (i.e., using system streams)
        // AND either on Windows (required for proper Unicode) or if explicitly requested
        if (input == null && output == null && (OsUtils.isWindows() || SystemProperties.isUseJline())) {
            return new TerminalJlineImpl(debugToStderr);
        }
        // Default to stty-based implementation
        return new TerminalShImpl(debugToStderr);
    }

    /**
     * Create a Terminal instance for system input/output.
     * This is equivalent to calling create(null, null, debugToStderr).
     *
     * @param debugToStderr if true, print debug output to stderr
     * @return a Terminal instance appropriate for the current environment
     */
    public static Terminal create(boolean debugToStderr) {
        return create(null, null, debugToStderr);
    }
}
