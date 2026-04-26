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

    /**
     * Window width and height packed into a single 64-bit value (width in
     * the high 32 bits, height in the low 32 bits) so that updates from
     * the SSH I/O thread and reads from Casciian's reader thread always
     * observe a consistent (width, height) pair. Without this, two
     * independent {@code volatile int}s could surface a half-updated size
     * (new width, old height) on the next idle tick and trigger spurious
     * intermediate resize events.
     */
    private volatile long windowSize;

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
        final int initialWidth = columns > 0 ? columns : DEFAULT_WINDOW_WIDTH;
        final int initialHeight = rows > 0 ? rows : DEFAULT_WINDOW_HEIGHT;
        this.windowSize = pack(initialWidth, initialHeight);
    }

    /**
     * Update the cached PTY geometry. Called by the SSH WINCH listener
     * after the client resizes its terminal. Non-positive values are
     * ignored so a malformed {@code window-change} request cannot collapse
     * the screen.
     *
     * <p>Width and height are written together as a single {@code volatile}
     * long, so concurrent readers always observe a consistent
     * (width, height) pair.</p>
     *
     * @param columns the new PTY width in character cells
     * @param rows    the new PTY height in character cells
     */
    void setWindowSize(final int columns, final int rows) {
        synchronized (this) {
            final long current = this.windowSize;
            final int newWidth = columns > 0 ? columns : unpackWidth(current);
            final int newHeight = rows > 0 ? rows : unpackHeight(current);
            this.windowSize = pack(newWidth, newHeight);
        }
    }

    private static long pack(final int width, final int height) {
        return ((long) width << 32) | ((long) height & 0xFFFFFFFFL);
    }

    private static int unpackWidth(final long packed) {
        return (int) (packed >>> 32);
    }

    private static int unpackHeight(final long packed) {
        return (int) packed;
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
        return unpackWidth(windowSize);
    }

    @Override
    public int getWindowHeight() {
        return unpackHeight(windowSize);
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
