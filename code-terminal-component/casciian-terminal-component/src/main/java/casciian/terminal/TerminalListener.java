/*
 * Casciian - Java Text User Interface
 *
 * Original work written 2013–2025 by Autumn Lamonte
 * and dedicated to the public domain via CC0.
 *
 * Modifications and maintenance:
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
package casciian.terminal;

import java.util.List;

import casciian.bits.Clipboard;

/**
 * TerminalListener is used to callback into external UI when the terminal
 * state has changed.
 */
public interface TerminalListener {

    /**
     * Function to call when the terminal state has updated.
     *
     * @param terminalState the new (now current) terminal state
     */
    public void postUpdate(final TerminalState terminalState);

    /**
     * Function to call to obtain the external UI display width.
     *
     * @return the number of columns in the display
     */
    public int getDisplayWidth();

    /**
     * Function to call to obtain the external UI display height.
     *
     * @return the number of rows in the display
     */
    public int getDisplayHeight();

    /**
     * Get the system clipboard to use for OSC 52.
     *
     * @return the clipboard
     */
    public Clipboard getClipboard();

}
