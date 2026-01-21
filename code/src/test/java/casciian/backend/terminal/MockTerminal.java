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
 * Mock terminal implementation for testing purposes.
 * Provides a configurable terminal that can be used in unit tests.
 */
public class MockTerminal implements Terminal {
    private int windowWidth = 100;
    private int windowHeight = 50;
    private boolean queryWindowSizeCalled = false;

    @Override
    public void setRawMode() {}

    @Override
    public void setCookedMode() {}

    @Override
    public void close() {}

    @Override
    public PrintWriter getWriter() {
        return null;
    }

    @Override
    public Reader getReader() {
        return null;
    }

    @Override
    public void enableMouseReporting(boolean on) {}

    @Override
    public int available() throws IOException {
        return 0;
    }

    @Override
    public int read(char[] buffer, int off, int len) throws IOException {
        return -1;
    }

    @Override
    public void queryWindowSize() {
        queryWindowSizeCalled = true;
    }

    @Override
    public int getWindowWidth() {
        return windowWidth;
    }

    @Override
    public int getWindowHeight() {
        return windowHeight;
    }

    /**
     * Set the mock window width.
     *
     * @param width the width to set
     */
    public void setWindowWidth(int width) {
        this.windowWidth = width;
    }

    /**
     * Set the mock window height.
     *
     * @param height the height to set
     */
    public void setWindowHeight(int height) {
        this.windowHeight = height;
    }

    /**
     * Check if queryWindowSize was called.
     *
     * @return true if queryWindowSize was called, false otherwise
     */
    public boolean wasQueryWindowSizeCalled() {
        return queryWindowSizeCalled;
    }

    /**
     * Reset the queryWindowSizeCalled flag.
     */
    public void resetQueryWindowSizeCalled() {
        queryWindowSizeCalled = false;
    }
}
