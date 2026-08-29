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
package casciian.backend;

import java.util.List;

import casciian.bits.CellAttributes;
import casciian.event.TInputEvent;

/**
 * This interface provides a screen, keyboard, and mouse to TApplication.  It
 * also exposes session information as gleaned from lower levels of the
 * communication stack.
 */
public interface Backend {

    /**
     * Get a SessionInfo, which exposes text width/height, language,
     * username, and other information from the communication stack.
     *
     * @return the SessionInfo
     */
    public SessionInfo getSessionInfo();

    /**
     * Get a Screen, which displays the text cells to the user.
     *
     * @return the Screen
     */
    public Screen getScreen();

    /**
     * Classes must provide an implementation that syncs the logical screen
     * to the physical device.
     */
    public void flushScreen();

    /**
     * Check if there are events in the queue.
     *
     * @return if true, getEvents() has something to return to the application
     */
    public boolean hasEvents();

    /**
     * Classes must provide an implementation to get keyboard, mouse, and
     * screen resize events.
     *
     * @param queue list to append new events to
     */
    public void getEvents(List<TInputEvent> queue);

    /**
     * Classes must provide an implementation that closes sockets, restores
     * console, etc.
     */
    public void shutdown();

    /**
     * Classes must provide an implementation that sets the window title.
     *
     * @param title the new title
     */
    public void setTitle(final String title);

    /**
     * Classes must provide an implementation that reports the current
     * working directory to the terminal (OSC 7).
     *
     * @param directory the new working directory
     */
    public void setWorkingDirectory(final String directory);

    /**
     * Set listener to a different Object.
     *
     * @param listener the new listening object that run() wakes up on new
     * input
     */
    public void setListener(final Object listener);

    /**
     * Reload backend options from System properties.
     */
    public void reloadOptions();

    /**
     * Check if backend is read-only.
     *
     * @return true if user input events from the backend are discarded
     */
    public boolean isReadOnly();

    /**
     * Set read-only flag.
     *
     * @param readOnly if true, then input events will be discarded
     */
    public void setReadOnly(final boolean readOnly);

    /**
     * Check if backend will support incomplete image fragments over text
     * display.
     *
     * @return true if images can partially obscure text
     */
    public boolean isImagesOverText();

    /**
     * Check if the backend supports an image protocol (e.g. sixel or
     * Casciian/Jexer image protocol) that can render bitmap image cells.
     *
     * @return true if bitmap image cells can be rendered natively
     */
    public boolean isImageProtocolSupported();

    /**
     * Set the mouse pointer (cursor) style.
     *
     * @param mouseStyle the pointer style string, one of: "default", "none",
     * "hand", "text", "move", or "crosshair"
     */
    public void setMouseStyle(final String mouseStyle);

    /**
     * Convert a CellAttributes foreground color to an RGB color.
     *
     * @param attr the text attributes
     * @return the RGB color
     */
    public int attrToForegroundColor(final CellAttributes attr);

    /**
     * Convert a CellAttributes background color to an RGB color.
     *
     * @param attr the text attributes
     * @return the RGB color
     */
    public int attrToBackgroundColor(final CellAttributes attr);

    /**
     * Copy text to the system clipboard of the terminal on the backend.  Not
     * all terminals support this (OSC 52).
     *
     * @param text string to copy
     */
    public void copyClipboardText(final String text);

    /**
     * Request text from the system clipboard of the terminal on the backend.
     * The request is asynchronous; unsupported terminals may ignore it.
     */
    public default void requestClipboardText() {
        // NOP
    }

    /**
     * Get window/terminal system focus.
     *
     * @return true if this backend has the mouse/keyboard focus
     */
    public boolean isFocused();

    /**
     * Retrieve the default foreground color.
     *
     * @return the RGB color
     */
    public int getDefaultForeColorRGB();

    /**
     * Retrieve the default background color.
     *
     * @return the RGB color
     */
    public int getDefaultBackColorRGB();

}
