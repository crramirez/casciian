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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * Shell-based terminal implementation using stty commands.
 * 
 * <p>This implementation uses /bin/sh and stty commands to set terminal
 * attributes. This is the traditional Unix approach and works on Linux
 * and macOS but will fail silently on Windows (where /bin/sh doesn't exist).
 */
public class TerminalShImpl implements Terminal {

    /**
     * If true, print debug output to stderr.
     */
    private final boolean debugToStderr;

    /**
     * Create a new shell-based terminal implementation.
     *
     * @param debugToStderr if true, print debug output to stderr
     */
    public TerminalShImpl(boolean debugToStderr) {
        this.debugToStderr = debugToStderr;
    }

    @Override
    public void setRawMode() {
        doStty(true);
    }

    @Override
    public void setCookedMode() {
        doStty(false);
    }

    @Override
    public void close() {
        // No resources to release for the shell implementation
    }

    @Override
    public PrintWriter getWriter() {
        // Shell implementation doesn't provide a custom writer
        return null;
    }

    @Override
    public boolean hasCustomWriter() {
        return false;
    }

    @Override
    public InputStream getInputStream() {
        // Shell implementation doesn't provide a custom input stream
        return null;
    }

    @Override
    public boolean hasCustomInputStream() {
        return false;
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
            try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
                 BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"))) {
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
}
