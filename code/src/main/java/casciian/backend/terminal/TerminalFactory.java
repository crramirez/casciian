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

import casciian.backend.SystemProperties;

/**
 * Factory for creating Terminal instances.
 * 
 * <p>The factory determines which implementation to use based on:
 * <ul>
 *   <li>If OS is Windows: uses JLine implementation for proper Unicode support</li>
 *   <li>If casciian.useJline system property is true: uses JLine implementation</li>
 *   <li>Otherwise: uses the stty-based shell implementation (default)</li>
 * </ul>
 */
public final class TerminalFactory {

    private TerminalFactory() {
        // Utility class, prevent instantiation
    }

    /**
     * Create a Terminal instance based on the current platform and configuration.
     *
     * @param debugToStderr if true, print debug output to stderr
     * @return a Terminal instance appropriate for the current environment
     */
    public static Terminal create(boolean debugToStderr) {
        // Use JLine on Windows (required for proper Unicode) or if explicitly requested
        if (OsUtils.isWindows() || SystemProperties.isUseJline()) {
            return new TerminalJlineImpl(debugToStderr);
        }
        // Default to stty-based implementation on Unix-like systems
        return new TerminalShImpl(debugToStderr);
    }
}
