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

import java.io.InputStream;
import java.io.OutputStream;

import casciian.TApplication;

/**
 * Factory that creates a fresh {@link TApplication} for every incoming SSH
 * session.
 *
 * <p>Applications using the starter must publish exactly one bean implementing
 * this interface. Because the factory itself is a Spring singleton, it can
 * capture injected collaborators (repositories, services, security context)
 * and hand them to each new {@link TApplication} instance. The
 * {@link TApplication} instances themselves are intentionally per-connection:
 * a Casciian application owns mutable UI state and cannot be shared across
 * concurrent terminals.</p>
 *
 * <p>The factory is invoked on an SSH channel thread. Implementations should
 * return quickly; heavy initialization should be cached as fields on the
 * factory bean.</p>
 */
@FunctionalInterface
public interface CasciianTApplicationFactory {

    /**
     * Create a new {@link TApplication} bound to the given SSH channel
     * streams.
     *
     * @param input   the channel's input stream (bytes from the remote
     *                terminal)
     * @param output  the channel's output stream (bytes to the remote
     *                terminal)
     * @param session metadata about the SSH session (username, pty
     *                geometry, remote address)
     * @return a new, unstarted {@link TApplication} instance
     * @throws Exception if the application cannot be constructed
     */
    TApplication create(InputStream input, OutputStream output, SshSessionContext session) throws Exception;
}
