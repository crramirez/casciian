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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ShellFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/**
 * Spring {@link SmartLifecycle} bean that owns the embedded MINA
 * {@link SshServer}. It is started after the rest of the application context
 * (including any web server) has finished bootstrapping, and stopped in
 * reverse order during shutdown.
 */
public class CasciianSshServer implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(CasciianSshServer.class);

    private final CasciianSshProperties properties;
    private final ShellFactory shellFactory;
    private final PasswordAuthenticator passwordAuthenticator;

    private volatile SshServer sshServer;
    private volatile boolean running;

    public CasciianSshServer(final CasciianSshProperties properties,
                             final ShellFactory shellFactory,
                             final PasswordAuthenticator passwordAuthenticator) {
        this.properties = properties;
        this.shellFactory = shellFactory;
        this.passwordAuthenticator = passwordAuthenticator;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        final SshServer server = SshServer.setUpDefaultServer();
        server.setHost(properties.getHost());
        server.setPort(properties.getPort());
        server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(resolveHostKeyPath()));
        server.setShellFactory(shellFactory);
        server.setPasswordAuthenticator(passwordAuthenticator);
        final String banner = properties.getBanner();
        if (banner != null && !banner.isEmpty()) {
            // Welcome banner sent during user-auth (RFC 4252 §5.4). Stored via
            // the generic property map so this module does not pin a specific
            // MINA SSHD constant name across versions.
            server.getProperties().put("welcome-banner", banner);
        }
        try {
            server.start();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to start Casciian SSH server on "
                            + properties.getHost() + ":" + properties.getPort(), e);
        }
        sshServer = server;
        running = true;
        LOG.info("Casciian SSH server listening on {}:{}",
                properties.getHost(), properties.getPort());
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }
        final SshServer server = sshServer;
        if (server != null) {
            try {
                server.stop(true);
            } catch (IOException e) {
                LOG.warn("Error stopping Casciian SSH server", e);
            }
        }
        sshServer = null;
        running = false;
        LOG.info("Casciian SSH server stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Start after the main web server (Boot's web server uses phase {@code
     * Integer.MAX_VALUE}); we pick a slightly earlier phase so our server is
     * stopped first on shutdown and a slightly later number than most
     * application beans so we start after them.
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 1024;
    }

    /**
     * Resolve the configured host-key path, expanding a leading {@code ~} to
     * the user's home directory and ensuring parent directories exist.
     */
    Path resolveHostKeyPath() {
        final String raw = properties.getHostKeyPath() == null
                ? CasciianSshProperties.DEFAULT_HOST_KEY_PATH
                : properties.getHostKeyPath();
        final String expanded;
        if (raw.startsWith("~/") || raw.equals("~")) {
            expanded = System.getProperty("user.home")
                    + raw.substring(1);
        } else {
            expanded = raw;
        }
        final Path path = Paths.get(expanded).toAbsolutePath();
        final Path parent = path.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Unable to create host-key directory " + parent, e);
            }
        }
        return path;
    }

    /** Package-private accessor for tests. */
    SshServer getSshServerForTesting() {
        return sshServer;
    }
}
