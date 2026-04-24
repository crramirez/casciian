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

/**
 * Read-only metadata about the SSH session backing a Casciian
 * {@link casciian.TApplication} instance.
 *
 * <p>Exposed to {@link CasciianTApplicationFactory} implementations so the
 * application can record who connected, display a per-user greeting, or tune
 * its UI to the reported PTY geometry.</p>
 *
 * @param username      the authenticated SSH username
 * @param remoteAddress the remote host/port of the client (may be {@code null}
 *                      if the channel is not TCP-backed, e.g. in tests)
 * @param terminalType  the reported terminal type (e.g. {@code "xterm-256color"}),
 *                      or {@code null} if no PTY was allocated
 * @param columns       the initial PTY width in columns; {@code 0} if unknown
 * @param rows          the initial PTY height in rows; {@code 0} if unknown
 */
public record SshSessionContext(
        String username,
        String remoteAddress,
        String terminalType,
        int columns,
        int rows) {
}
