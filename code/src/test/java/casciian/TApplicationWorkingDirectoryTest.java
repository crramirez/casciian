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
package casciian;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import casciian.backend.HeadlessBackend;
import casciian.backend.SystemProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for TApplication's working-directory synchronization.
 */
@DisplayName("TApplication working directory tests")
class TApplicationWorkingDirectoryTest {

    private final String originalUserDir = SystemProperties.getUserDir();

    @AfterEach
    void tearDown() {
        SystemProperties.setUserDir(originalUserDir);
        SystemProperties.reset();
    }

    @Test
    @DisplayName("Constructor pushes the initial cached working directory to the backend")
    void testConstructorPushesInitialWorkingDirectory() throws Exception {
        RecordingBackend backend = new RecordingBackend(false);
        TApplication application = new TApplication(backend);

        assertEquals(List.of(SystemProperties.getUserDir()),
            backend.workingDirectories);

        runAndExit(application, backend);
    }

    @Test
    @DisplayName("Listener is removed even when backend shutdown throws")
    void testListenerRemovedWhenShutdownThrows() throws Exception {
        RecordingBackend backend = new RecordingBackend(true);
        TApplication application = new TApplication(backend);

        runAndExit(application, backend);

        int callsBefore = backend.workingDirectories.size();
        SystemProperties.setUserDir(System.getProperty("java.io.tmpdir")
            + File.separator + "after-shutdown");

        assertEquals(callsBefore, backend.workingDirectories.size());
    }

    private void runAndExit(final TApplication application,
        final RecordingBackend backend) throws Exception {

        Thread thread = new Thread(application::run);
        thread.start();
        application.exit();
        thread.join(2000);

        assertFalse(thread.isAlive());
        assertTrue(backend.shutdownCalled.await(2, TimeUnit.SECONDS));
    }

    private static class RecordingBackend extends HeadlessBackend {
        private final boolean failOnShutdown;
        private final List<String> workingDirectories =
            new CopyOnWriteArrayList<>();
        private final CountDownLatch shutdownCalled = new CountDownLatch(1);

        RecordingBackend(final boolean failOnShutdown) {
            this.failOnShutdown = failOnShutdown;
        }

        @Override
        public void setWorkingDirectory(final String directory) {
            workingDirectories.add(directory);
        }

        @Override
        public void shutdown() {
            shutdownCalled.countDown();
            if (failOnShutdown) {
                throw new IllegalStateException("expected test failure");
            }
        }
    }
}
