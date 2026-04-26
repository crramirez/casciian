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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.sshd.common.channel.Channel;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.Signal;
import org.apache.sshd.server.SignalListener;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.junit.jupiter.api.Test;

import casciian.TApplication;
import casciian.backend.SessionInfo;

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
        // The factory receives the SSH input stream wrapped in an
        // SshSessionInfoInputStream so Casciian's ECMA48 backend can read
        // the PTY size as a SessionInfo. The wrapper must still delegate
        // reads to the original SSH stream.
        assertThat(factory.lastInput).isInstanceOf(SessionInfo.class);
        assertThat(factory.lastInput).isNotSameAs(in);
        final SessionInfo sessionInfo = (SessionInfo) factory.lastInput;
        assertThat(sessionInfo.getWindowWidth()).isEqualTo(120);
        assertThat(sessionInfo.getWindowHeight()).isEqualTo(40);
        assertThat(sessionInfo.getUsername()).isEqualTo("alice");
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

    @Test
    void winchSignalUpdatesWrappedInputStreamSize() throws Exception {
        final CountingFactory factory = new CountingFactory();
        final CasciianShellFactory shellFactory = new CasciianShellFactory(factory);
        final Command command = shellFactory.createShell(mock(ChannelSession.class));
        final ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        command.setInputStream(in);
        command.setOutputStream(new ByteArrayOutputStream());
        command.setErrorStream(new ByteArrayOutputStream());
        final RecordingExit exit = new RecordingExit();
        command.setExitCallback(exit);

        final Map<String, String> env = new HashMap<>();
        env.put(Environment.ENV_USER, "bob");
        env.put(Environment.ENV_COLUMNS, "80");
        env.put(Environment.ENV_LINES, "24");
        final RecordingStubEnvironment recording = new RecordingStubEnvironment(env);

        command.start(mock(ChannelSession.class), recording);
        assertThat(exit.await(2, TimeUnit.SECONDS)).isTrue();

        // The shell registered exactly one listener for WINCH, and removed
        // it again when the worker thread exited.
        assertThat(recording.listeners).hasSize(1);
        assertThat(recording.registeredSignals).singleElement()
                .satisfies(s -> assertThat(s).containsExactly(Signal.WINCH));
        assertThat(recording.removedCount).isEqualTo(1);
        final SignalListener listener = recording.listeners.get(0);

        // Wrapper started at the SSH-advertised geometry.
        final SessionInfo sessionInfo = (SessionInfo) factory.lastInput;
        assertThat(sessionInfo.getWindowWidth()).isEqualTo(80);
        assertThat(sessionInfo.getWindowHeight()).isEqualTo(24);

        // Simulate a window-change request: MINA updates ENV_COLUMNS /
        // ENV_LINES first and then notifies the WINCH listener.
        env.put(Environment.ENV_COLUMNS, "132");
        env.put(Environment.ENV_LINES, "50");
        listener.signal(mock(Channel.class), Signal.WINCH);

        // The wrapped SessionInfo must now report the new size so
        // Casciian's ECMA48 backend will pick it up on its next idle tick
        // and emit a SCREEN-type TResizeEvent.
        assertThat(sessionInfo.getWindowWidth()).isEqualTo(132);
        assertThat(sessionInfo.getWindowHeight()).isEqualTo(50);

        // Non-WINCH signals must not change the geometry.
        env.put(Environment.ENV_COLUMNS, "1");
        env.put(Environment.ENV_LINES, "1");
        listener.signal(mock(Channel.class), Signal.INT);
        assertThat(sessionInfo.getWindowWidth()).isEqualTo(132);
        assertThat(sessionInfo.getWindowHeight()).isEqualTo(50);

        // A WINCH carrying garbage values must be ignored, not collapse
        // the screen.
        env.put(Environment.ENV_COLUMNS, "not-a-number");
        env.put(Environment.ENV_LINES, "0");
        listener.signal(mock(Channel.class), Signal.WINCH);
        assertThat(sessionInfo.getWindowWidth()).isEqualTo(132);
        assertThat(sessionInfo.getWindowHeight()).isEqualTo(50);
    }

    @Test
    void wrappedInputStreamDelegatesReadsToTheSshStream() throws Exception {
        final CountingFactory factory = new CountingFactory();
        final CasciianShellFactory shellFactory = new CasciianShellFactory(factory);
        final Command command = shellFactory.createShell(mock(ChannelSession.class));
        final byte[] payload = new byte[] { 'h', 'i' };
        final ByteArrayInputStream in = new ByteArrayInputStream(payload);
        command.setInputStream(in);
        command.setOutputStream(new ByteArrayOutputStream());
        command.setErrorStream(new ByteArrayOutputStream());
        final RecordingExit exit = new RecordingExit();
        command.setExitCallback(exit);

        command.start(mock(ChannelSession.class), new StubEnvironment(Map.of()));
        assertThat(exit.await(2, TimeUnit.SECONDS)).isTrue();

        // Reading through the wrapper must observe the original stream's
        // bytes; otherwise Casciian would never see SSH input.
        final InputStream wrapped = factory.lastInput;
        assertThat(wrapped.read()).isEqualTo((int) 'h');
        assertThat(wrapped.read()).isEqualTo((int) 'i');
        assertThat(wrapped.read()).isEqualTo(-1);
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
    private static class StubEnvironment implements Environment {
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
        public void addSignalListener(final SignalListener listener,
                                      final Collection<Signal> signals) {
            // no-op
        }

        @Override
        public void removeSignalListener(final SignalListener listener) {
            // no-op
        }
    }

    /**
     * {@link StubEnvironment} that records every signal listener registered
     * by the production code, so a test can drive the WINCH callback by
     * hand without standing up a real SSH server.
     *
     * <p>The {@link #listeners} list is append-only and intentionally not
     * pruned on {@link #removeSignalListener(SignalListener)}, because the
     * shell command removes its listener as soon as the worker thread
     * exits and the test still needs to invoke it afterwards.</p>
     */
    private static final class RecordingStubEnvironment extends StubEnvironment {
        final List<SignalListener> listeners = new ArrayList<>();
        final List<Collection<Signal>> registeredSignals = new ArrayList<>();
        int removedCount;

        RecordingStubEnvironment(final Map<String, String> env) {
            super(env);
        }

        @Override
        public void addSignalListener(final SignalListener listener,
                                      final Collection<Signal> signals) {
            listeners.add(listener);
            registeredSignals.add(new ArrayList<>(signals));
        }

        @Override
        public void removeSignalListener(final SignalListener listener) {
            removedCount++;
        }
    }
}
