/*
 * Copyright 2026 Carlos Rafael Ramirez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.crramirez.casciian.spring;

import java.io.FilterInputStream;
import java.io.InputStream;

import casciian.backend.SessionInfo;

/**
 * {@link InputStream} adapter that also implements Casciian's
 * {@link SessionInfo} so the ECMA48 backend can read the SSH-supplied PTY
 * dimensions (and pick up subsequent {@code window-change} requests as
 * resize events).
 *
 * <p>Casciian's {@code ECMA48Terminal} polls
 * {@link SessionInfo#queryWindowSize()} once per second and synthesizes a
 * {@code TResizeEvent} whenever the reported width or height changes. This
 * adapter therefore only has to surface the latest values supplied by the
 * SSH layer; the rest of the resize plumbing is already in place.</p>
 *
 * <p>All accessors are safe to call from any thread: the SSH I/O thread
 * updates the size via {@link #setWindowSize(int, int)} when a
 * {@code SIGWINCH} signal arrives, while the Casciian reader thread reads
 * it on its idle tick.</p>
 */
final class SshSessionInfoInputStream extends FilterInputStream implements SessionInfo {

    /** Default fallback width when the client never advertises a PTY size. */
    static final int DEFAULT_WINDOW_WIDTH = 80;

    /** Default fallback height when the client never advertises a PTY size. */
    static final int DEFAULT_WINDOW_HEIGHT = 24;

    private final long startTime = System.currentTimeMillis();

    private volatile String username;
    private volatile String language = "en_US";
    private volatile int windowWidth;
    private volatile int windowHeight;
    private volatile int idleTime = Integer.MAX_VALUE;

    /**
     * Wrap an existing SSH channel input stream.
     *
     * @param delegate the SSH channel input stream; must not be null
     * @param username the authenticated SSH username; may be empty but not
     *                 null
     * @param columns  the initial PTY width in character cells; values
     *                 less than 1 fall back to {@value #DEFAULT_WINDOW_WIDTH}
     *                 so Casciian's backend never gets a zero-sized screen
     * @param rows     the initial PTY height in character cells; values
     *                 less than 1 fall back to {@value #DEFAULT_WINDOW_HEIGHT}
     */
    SshSessionInfoInputStream(final InputStream delegate, final String username,
                              final int columns, final int rows) {
        super(delegate);
        if (delegate == null) {
            throw new IllegalArgumentException("delegate must not be null");
        }
        this.username = username == null ? "" : username;
        this.windowWidth = columns > 0 ? columns : DEFAULT_WINDOW_WIDTH;
        this.windowHeight = rows > 0 ? rows : DEFAULT_WINDOW_HEIGHT;
    }

    /**
     * Update the cached PTY geometry. Called by the SSH WINCH listener
     * after the client resizes its terminal. Non-positive values are
     * ignored so a malformed {@code window-change} request cannot collapse
     * the screen.
     *
     * @param columns the new PTY width in character cells
     * @param rows    the new PTY height in character cells
     */
    void setWindowSize(final int columns, final int rows) {
        if (columns > 0) {
            this.windowWidth = columns;
        }
        if (rows > 0) {
            this.windowHeight = rows;
        }
    }

    // ------------------------------------------------------------------
    // SessionInfo
    // ------------------------------------------------------------------

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public int getIdleTime() {
        return idleTime;
    }

    @Override
    public void setIdleTime(final int seconds) {
        this.idleTime = seconds;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(final String username) {
        this.username = username == null ? "" : username;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public void setLanguage(final String language) {
        this.language = language == null ? "en_US" : language;
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
     * No-op: the SSH layer pushes new sizes to {@link #setWindowSize(int, int)}
     * via its WINCH listener, so the cached values are always current and
     * there is nothing to query on demand.
     */
    @Override
    public void queryWindowSize() {
        // Intentionally empty: size is push-updated from the SSH layer.
    }
}
