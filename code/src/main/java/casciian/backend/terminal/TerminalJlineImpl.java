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
import java.nio.charset.StandardCharsets;

import org.jline.terminal.Attributes;
import org.jline.terminal.TerminalBuilder;

/**
 * JLine-based terminal implementation for raw/cooked mode handling.
 * 
 * <p>This implementation uses JLine's terminal abstraction to set terminal
 * attributes in a platform-agnostic way. On Windows, JLine uses native
 * Windows Console APIs (WriteConsoleW) for proper Unicode support.
 */
public class TerminalJlineImpl implements Terminal {

    /**
     * The JLine terminal instance.
     */
    private org.jline.terminal.Terminal jlineTerminal;

    /**
     * The original terminal attributes to restore when switching back to cooked mode.
     */
    private Attributes originalAttributes;

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
        try {
            jlineTerminal = TerminalBuilder.builder()
                .system(true)
                .encoding(StandardCharsets.UTF_8)
                .build();
            // Save original attributes for later restoration
            originalAttributes = new Attributes(jlineTerminal.getAttributes());
        } catch (IOException e) {
            if (debugToStderr) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setRawMode() {
        if (jlineTerminal == null || originalAttributes == null) {
            return;
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

    @Override
    public void setCookedMode() {
        if (jlineTerminal != null && originalAttributes != null) {
            jlineTerminal.setAttributes(originalAttributes);
        }
    }

    @Override
    public void close() {
        if (jlineTerminal != null) {
            try {
                jlineTerminal.close();
            } catch (IOException e) {
                if (debugToStderr) {
                    e.printStackTrace();
                }
            }
            jlineTerminal = null;
            originalAttributes = null;
        }
    }

    @Override
    public PrintWriter getWriter() {
        if (jlineTerminal != null) {
            return jlineTerminal.writer();
        }
        return null;
    }

    @Override
    public InputStream getInputStream() {
        if (jlineTerminal != null) {
            return jlineTerminal.input();
        }
        return null;
    }

    @Override
    public Reader getReader() {
        if (jlineTerminal != null) {
            return jlineTerminal.reader();
        }
        return null;
    }
}
