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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.sshd.common.channel.Channel;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.Signal;
import org.apache.sshd.server.SignalListener;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.shell.ShellFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import casciian.TApplication;

/**
 * MINA SSHD {@link ShellFactory} that delegates {@link TApplication}
 * construction to a user-supplied {@link CasciianTApplicationFactory}.
 *
 * <p>A new {@link TApplication} is built for every shell channel, so each SSH
 * session has its own UI state. The application is driven on a dedicated
 * virtual thread so many simultaneous TUI users do not consume platform
 * threads.</p>
 *
 * <p>The SSH {@code InputStream} handed to the factory is wrapped in an
 * {@link SshSessionInfoInputStream} so Casciian's ECMA48 backend can read
 * the PTY width/height advertised at channel start, and pick up subsequent
 * client {@code window-change} requests as {@code TResizeEvent}s. A
 * {@link Signal#WINCH} listener is registered on the SSH
 * {@link Environment} for the lifetime of the shell so the wrapper's size
 * stays in sync with the remote terminal.</p>
 */
public class CasciianShellFactory implements ShellFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CasciianShellFactory.class);

    private final CasciianTApplicationFactory applicationFactory;

    public CasciianShellFactory(final CasciianTApplicationFactory applicationFactory) {
        if (applicationFactory == null) {
            throw new IllegalArgumentException("applicationFactory must not be null");
        }
        this.applicationFactory = applicationFactory;
    }

    @Override
    public Command createShell(final ChannelSession channel) throws IOException {
        return new CasciianShellCommand(channel, applicationFactory);
    }

    /**
     * {@link Command} implementation that runs a {@link TApplication} on a
     * virtual thread for the lifetime of a single SSH shell channel.
     */
    static final class CasciianShellCommand implements Command {

        private final ChannelSession channel;
        private final CasciianTApplicationFactory applicationFactory;

        private InputStream in;
        private OutputStream out;
        private OutputStream err;
        private ExitCallback exitCallback;

        private volatile Thread worker;
        private volatile TApplication application;

        CasciianShellCommand(final ChannelSession channel,
                             final CasciianTApplicationFactory applicationFactory) {
            this.channel = channel;
            this.applicationFactory = applicationFactory;
        }

        @Override
        public void setInputStream(final InputStream in) {
            this.in = in;
        }

        @Override
        public void setOutputStream(final OutputStream out) {
            this.out = out;
        }

        @Override
        public void setErrorStream(final OutputStream err) {
            this.err = err;
        }

        @Override
        public void setExitCallback(final ExitCallback callback) {
            this.exitCallback = callback;
        }

        @Override
        public void start(final ChannelSession ignored, final Environment env) {
            final SshSessionContext context = buildContext(env);
            final SshSessionInfoInputStream sessionInput = new SshSessionInfoInputStream(
                    in, context.username(), context.columns(), context.rows());
            final SignalListener winchListener = newWinchListener(env, sessionInput);
            if (env != null) {
                // Subscribe before launching the worker so a fast resize
                // request that races with the client's first input is not
                // dropped on the floor.
                env.addSignalListener(winchListener, Signal.WINCH);
            }
            final Runnable body = () -> {
                try {
                    runApplication(context, sessionInput);
                } finally {
                    if (env != null) {
                        env.removeSignalListener(winchListener);
                    }
                }
            };
            worker = Thread.ofVirtual()
                    .name("casciian-ssh-" + context.username() + "-"
                            + System.identityHashCode(channel))
                    .start(body);
        }

        /**
         * Build a {@link SignalListener} that mirrors the SSH PTY size into
         * the supplied {@link SshSessionInfoInputStream} every time the
         * client sends a {@code window-change} request.
         *
         * <p>MINA SSHD updates {@link Environment#getEnv()} with the new
         * {@link Environment#ENV_COLUMNS} / {@link Environment#ENV_LINES}
         * values <em>before</em> notifying signal listeners, so the
         * listener simply needs to re-read those entries. Casciian's
         * ECMA48 backend polls the wrapped stream's
         * {@code SessionInfo.getWindowWidth()/getWindowHeight()} once per
         * second and emits a {@code TResizeEvent.Type.SCREEN} as soon as
         * it observes a change.</p>
         */
        SignalListener newWinchListener(final Environment env,
                                        final SshSessionInfoInputStream sessionInput) {
            return new SignalListener() {
                @Override
                public void signal(final Channel ignored, final Signal sig) {
                    if (sig != Signal.WINCH || env == null) {
                        return;
                    }
                    final Map<String, String> envMap = env.getEnv();
                    if (envMap == null) {
                        return;
                    }
                    final int columns = parseInt(envMap.get(Environment.ENV_COLUMNS));
                    final int rows = parseInt(envMap.get(Environment.ENV_LINES));
                    sessionInput.setWindowSize(columns, rows);
                }
            };
        }

        @Override
        public void destroy(final ChannelSession ignored) {
            final Thread t = worker;
            if (t != null) {
                t.interrupt();
            }
        }

        private void runApplication(final SshSessionContext context,
                                    final InputStream sessionInput) {
            int exitCode = 0;
            try {
                application = applicationFactory.create(sessionInput, out, context);
                if (application == null) {
                    throw new IllegalStateException(
                            "CasciianTApplicationFactory returned null");
                }
                application.run();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                exitCode = 130; // conventional 128 + SIGINT
            } catch (Exception e) {
                LOG.warn("Casciian SSH session for user '{}' terminated with error",
                        context.username(), e);
                exitCode = 1;
            } finally {
                final ExitCallback cb = exitCallback;
                if (cb != null) {
                    cb.onExit(exitCode);
                }
            }
        }

        SshSessionContext buildContext(final Environment env) {
            final Map<String, String> envMap = env == null ? Map.of() : env.getEnv();
            final String username = envMap.getOrDefault(Environment.ENV_USER, "");
            final String termType = envMap.get(Environment.ENV_TERM);

            int columns = parseInt(envMap.get(Environment.ENV_COLUMNS));
            int rows = parseInt(envMap.get(Environment.ENV_LINES));

            final Object clientAddress = channel == null || channel.getSession() == null
                    ? null
                    : channel.getSession().getClientAddress();
            final String remote = clientAddress == null ? null : clientAddress.toString();

            return new SshSessionContext(username, remote, termType, columns, rows);
        }

        /**
         * Package-private accessor used by tests to verify that exactly one
         * application was built per shell invocation.
         */
        TApplication getApplicationForTesting() {
            return application;
        }

        private static int parseInt(final String value) {
            if (value == null || value.isBlank()) {
                return 0;
            }
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
}
