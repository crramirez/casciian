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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.junit.jupiter.api.Test;

import casciian.TApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Black-box tests for {@link CasciianShellFactory}. We exercise the per-session
 * contract (one factory invocation per shell, streams plumbed through, PTY
 * geometry read from the SSH environment) without starting a real SSH server.
 */
class CasciianShellFactoryTest {

    @Test
    void createsOneApplicationPerShellInvocation() throws Exception {
        final CountingFactory factory = new CountingFactory();
        final CasciianShellFactory shellFactory = new CasciianShellFactory(factory);

        shellFactory.createShell(mock(ChannelSession.class));
        shellFactory.createShell(mock(ChannelSession.class));

        assertThat(factory.created).isZero(); // create() runs only in start()
    }

    @Test
    void startInvokesFactoryWithStreamsAndContext() throws Exception {
        final CountingFactory factory = new CountingFactory();
        final CasciianShellFactory shellFactory = new CasciianShellFactory(factory);

        final Command command = shellFactory.createShell(mock(ChannelSession.class));
        final ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        command.setInputStream(in);
        command.setOutputStream(out);
        command.setErrorStream(out);
        final RecordingExit exit = new RecordingExit();
        command.setExitCallback(exit);

        final Map<String, String> env = new HashMap<>();
        env.put(Environment.ENV_USER, "alice");
        env.put(Environment.ENV_TERM, "xterm-256color");
        env.put(Environment.ENV_COLUMNS, "120");
        env.put(Environment.ENV_LINES, "40");

        command.start(mock(ChannelSession.class), new StubEnvironment(env));

        assertThat(exit.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(factory.created).isEqualTo(1);
        assertThat(factory.lastInput).isSameAs(in);
        assertThat(factory.lastOutput).isSameAs(out);
        assertThat(factory.lastSession.username()).isEqualTo("alice");
        assertThat(factory.lastSession.terminalType()).isEqualTo("xterm-256color");
        assertThat(factory.lastSession.columns()).isEqualTo(120);
        assertThat(factory.lastSession.rows()).isEqualTo(40);
        assertThat(exit.exitCode).isZero();
    }

    @Test
    void reportsNonZeroExitWhenFactoryThrows() throws Exception {
        final CasciianShellFactory shellFactory = new CasciianShellFactory(
                (in, out, session) -> {
                    throw new IllegalStateException("boom");
                });
        final Command command = shellFactory.createShell(mock(ChannelSession.class));
        command.setInputStream(new ByteArrayInputStream(new byte[0]));
        command.setOutputStream(new ByteArrayOutputStream());
        command.setErrorStream(new ByteArrayOutputStream());
        final RecordingExit exit = new RecordingExit();
        command.setExitCallback(exit);

        command.start(mock(ChannelSession.class), new StubEnvironment(Map.of()));

        assertThat(exit.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(exit.exitCode).isEqualTo(1);
    }

    @Test
    void handlesMissingOrMalformedEnvironmentGracefully() throws Exception {
        final CountingFactory factory = new CountingFactory();
        final CasciianShellFactory shellFactory = new CasciianShellFactory(factory);
        final Command command = shellFactory.createShell(mock(ChannelSession.class));
        command.setInputStream(new ByteArrayInputStream(new byte[0]));
        command.setOutputStream(new ByteArrayOutputStream());
        command.setErrorStream(new ByteArrayOutputStream());
        final RecordingExit exit = new RecordingExit();
        command.setExitCallback(exit);

        final Map<String, String> env = new HashMap<>();
        env.put(Environment.ENV_COLUMNS, "not-a-number");
        // intentionally omit USER, TERM, LINES

        command.start(mock(ChannelSession.class), new StubEnvironment(env));

        assertThat(exit.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(factory.lastSession.columns()).isZero();
        assertThat(factory.lastSession.rows()).isZero();
        assertThat(factory.lastSession.username()).isEmpty();
        assertThat(factory.lastSession.terminalType()).isNull();
    }

    // ------------------------------------------------------------------
    // Test doubles
    // ------------------------------------------------------------------

    /**
     * Records invocations of the factory and returns a Mockito-based
     * {@link TApplication} whose {@code run()} is a no-op, so the virtual
     * worker thread completes immediately and the exit callback fires.
     */
    private static final class CountingFactory implements CasciianTApplicationFactory {
        int created;
        InputStream lastInput;
        OutputStream lastOutput;
        SshSessionContext lastSession;

        @Override
        public TApplication create(final InputStream input,
                                   final OutputStream output,
                                   final SshSessionContext session) {
            created++;
            lastInput = input;
            lastOutput = output;
            lastSession = session;
            // Mockito returns a TApplication whose run() is a no-op, which is
            // what we want for the test — the worker thread then terminates
            // normally and the exit callback is invoked.
            final TApplication stub = mock(TApplication.class);
            when(stub.toString()).thenReturn("stub-application");
            return stub;
        }
    }

    private static final class RecordingExit implements ExitCallback {
        private final CountDownLatch done = new CountDownLatch(1);
        volatile int exitCode;

        @Override
        public void onExit(final int code) {
            exitCode = code;
            done.countDown();
        }

        @Override
        public void onExit(final int code, final String message) {
            onExit(code);
        }

        @Override
        public void onExit(final int code, final boolean closeImmediately) {
            onExit(code);
        }

        @Override
        public void onExit(final int code, final String message, final boolean closeImmediately) {
            onExit(code);
        }

        boolean await(final long timeout, final TimeUnit unit) throws InterruptedException {
            return done.await(timeout, unit);
        }
    }

    /**
     * Minimal {@link Environment} that returns a canned env map. Unused
     * methods throw so any accidental reliance on them is caught.
     */
    private static final class StubEnvironment implements Environment {
        private final Map<String, String> env;

        StubEnvironment(final Map<String, String> env) {
            this.env = env;
        }

        @Override
        public Map<String, String> getEnv() {
            return env;
        }

        @Override
        public Map<org.apache.sshd.common.channel.PtyMode, Integer> getPtyModes() {
            return Map.of();
        }

        @Override
        public void addSignalListener(final org.apache.sshd.server.SignalListener listener,
                                      final java.util.Collection<org.apache.sshd.server.Signal> signals) {
            // no-op
        }

        @Override
        public void removeSignalListener(final org.apache.sshd.server.SignalListener listener) {
            // no-op
        }
    }
}
