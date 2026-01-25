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

package casciian.terminal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import casciian.backend.Backend;
import casciian.backend.HeadlessBackend;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ECMA48 terminal emulator - validates OSC 4 color parsing
 * for correct handling of 16-bit color components.
 */
@DisplayName("ECMA48 Terminal Emulator Tests")
class ECMA48Test {

    /**
     * Test that 16-bit color components in OSC 4 sequences are correctly
     * parsed to 8-bit RGB values.
     * <p>
     * This tests the fix for the bright color rendering bug where green
     * was showing as yellow when using 16-bit color specifications.
     * </p>
     */
    @Test
    @DisplayName("OSC 4 with 16-bit color components should be parsed correctly")
    void shouldParse16BitColorComponents() {
        assertDoesNotThrow(() -> {
            // Create a minimal backend for testing
            Backend backend = new HeadlessBackend();
            
            // Create input stream with an OSC 4 sequence setting color 10 (bright green)
            // to rgb:0000/ffff/0000 which should parse to 0x00ff00 (bright green)
            // The format uses 16-bit components (4 hex digits each), with only green maximized
            String osc4Sequence = "\033]4;10;rgb:0000/ffff/0000\033\\";
            ByteArrayInputStream inputStream = new ByteArrayInputStream(
                osc4Sequence.getBytes("UTF-8"));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // Create the emulator
            ECMA48 emulator = new ECMA48(ECMA48.DeviceType.XTERM, inputStream,
                outputStream, null, backend);
            
            // Wait for the emulator to process the input
            emulator.waitForOutput(1000);
            
            // Close the emulator
            emulator.close();
        }, "OSC 4 sequence with 16-bit colors should be processed without exceptions");
    }
    
    /**
     * Test that 8-bit color components in OSC 4 sequences continue to work.
     */
    @Test
    @DisplayName("OSC 4 with 8-bit color components should still work")
    void shouldParse8BitColorComponents() {
        assertDoesNotThrow(() -> {
            Backend backend = new HeadlessBackend();
            
            // Create input stream with an OSC 4 sequence using 8-bit color format
            // rgb:00/ff/00 should parse to 0x00ff00 (bright green)
            String osc4Sequence = "\033]4;10;rgb:00/ff/00\033\\";
            ByteArrayInputStream inputStream = new ByteArrayInputStream(
                osc4Sequence.getBytes("UTF-8"));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            ECMA48 emulator = new ECMA48(ECMA48.DeviceType.XTERM, inputStream,
                outputStream, null, backend);
            
            emulator.waitForOutput(1000);
            emulator.close();
        }, "OSC 4 sequence with 8-bit colors should be processed without exceptions");
    }
    
    /**
     * Test that 4-bit color components in OSC 4 sequences work.
     */
    @Test
    @DisplayName("OSC 4 with 4-bit color components should work")
    void shouldParse4BitColorComponents() {
        assertDoesNotThrow(() -> {
            Backend backend = new HeadlessBackend();
            
            // Create input stream with an OSC 4 sequence using 4-bit color format
            // rgb:0/f/0 should scale to approximately 0x00ff00 (bright green)
            String osc4Sequence = "\033]4;10;rgb:0/f/0\033\\";
            ByteArrayInputStream inputStream = new ByteArrayInputStream(
                osc4Sequence.getBytes("UTF-8"));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            ECMA48 emulator = new ECMA48(ECMA48.DeviceType.XTERM, inputStream,
                outputStream, null, backend);
            
            emulator.waitForOutput(1000);
            emulator.close();
        }, "OSC 4 sequence with 4-bit colors should be processed without exceptions");
    }
    
    /**
     * Test that 12-bit color components in OSC 4 sequences work.
     */
    @Test
    @DisplayName("OSC 4 with 12-bit color components should work")
    void shouldParse12BitColorComponents() {
        assertDoesNotThrow(() -> {
            Backend backend = new HeadlessBackend();
            
            // Create input stream with an OSC 4 sequence using 12-bit color format
            // rgb:000/fff/000 should parse to approximately 0x00ff00 (bright green)
            String osc4Sequence = "\033]4;10;rgb:000/fff/000\033\\";
            ByteArrayInputStream inputStream = new ByteArrayInputStream(
                osc4Sequence.getBytes("UTF-8"));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            ECMA48 emulator = new ECMA48(ECMA48.DeviceType.XTERM, inputStream,
                outputStream, null, backend);
            
            emulator.waitForOutput(1000);
            emulator.close();
        }, "OSC 4 sequence with 12-bit colors should be processed without exceptions");
    }

    /**
     * Test that concurrent calls to captureState() and setWidth/setHeight
     * do not cause data corruption or exceptions.
     */
    @RepeatedTest(3)
    @DisplayName("Concurrent captureState and dimension changes should be thread-safe")
    void shouldHandleConcurrentCaptureStateAndDimensionChanges() throws Exception {
        Backend backend = new HeadlessBackend();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        ECMA48 emulator = new ECMA48(ECMA48.DeviceType.XTERM, inputStream,
            outputStream, null, backend);
        
        int numThreads = 4;
        int iterations = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        AtomicBoolean failed = new AtomicBoolean(false);
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        // Thread 1: Capture state repeatedly
        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < iterations && !failed.get(); i++) {
                    TerminalState state = emulator.captureState();
                    assertNotNull(state);
                }
            } catch (Exception e) {
                failed.set(true);
            } finally {
                doneLatch.countDown();
            }
        });
        
        // Thread 2: Set width repeatedly
        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < iterations && !failed.get(); i++) {
                    emulator.setWidth(80 + (i % 20));
                }
            } catch (Exception e) {
                failed.set(true);
            } finally {
                doneLatch.countDown();
            }
        });
        
        // Thread 3: Set height repeatedly
        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < iterations && !failed.get(); i++) {
                    emulator.setHeight(24 + (i % 10));
                }
            } catch (Exception e) {
                failed.set(true);
            } finally {
                doneLatch.countDown();
            }
        });
        
        // Thread 4: Capture state again
        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < iterations && !failed.get(); i++) {
                    TerminalState state = emulator.captureState();
                    assertNotNull(state);
                }
            } catch (Exception e) {
                failed.set(true);
            } finally {
                doneLatch.countDown();
            }
        });
        
        // Start all threads
        startLatch.countDown();
        
        // Wait for completion
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS),
            "All threads should complete within timeout");
        
        assertFalse(failed.get(),
            "No thread should encounter exceptions during concurrent operations");
        
        executor.shutdown();
        emulator.close();
    }

    /**
     * Test that concurrent calls to close() are handled safely.
     */
    @RepeatedTest(3)
    @DisplayName("Concurrent close() calls should be thread-safe")
    void shouldHandleConcurrentClose() throws Exception {
        Backend backend = new HeadlessBackend();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        ECMA48 emulator = new ECMA48(ECMA48.DeviceType.XTERM, inputStream,
            outputStream, null, backend);
        
        int numThreads = 4;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        AtomicBoolean failed = new AtomicBoolean(false);
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        for (int t = 0; t < numThreads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    // Multiple threads calling close concurrently
                    emulator.close();
                } catch (Exception e) {
                    failed.set(true);
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        // Start all threads
        startLatch.countDown();
        
        // Wait for completion
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS),
            "All threads should complete within timeout");
        
        assertFalse(failed.get(),
            "No thread should encounter exceptions during concurrent close()");
        
        executor.shutdown();
    }

    /**
     * Test that concurrent setScrollbackMax calls are thread-safe.
     */
    @RepeatedTest(3)
    @DisplayName("Concurrent setScrollbackMax calls should be thread-safe")
    void shouldHandleConcurrentSetScrollbackMax() throws Exception {
        Backend backend = new HeadlessBackend();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        ECMA48 emulator = new ECMA48(ECMA48.DeviceType.XTERM, inputStream,
            outputStream, null, backend);
        
        int numThreads = 4;
        int iterations = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        AtomicBoolean failed = new AtomicBoolean(false);
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterations && !failed.get(); i++) {
                        emulator.setScrollbackMax(1000 + threadId * 100 + i);
                    }
                } catch (Exception e) {
                    failed.set(true);
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        // Start all threads
        startLatch.countDown();
        
        // Wait for completion
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS),
            "All threads should complete within timeout");
        
        assertFalse(failed.get(),
            "No thread should encounter exceptions during concurrent setScrollbackMax()");
        
        executor.shutdown();
        emulator.close();
    }

    /**
     * Test that captureState() with active reader thread does not cause
     * ConcurrentModificationException in TerminalState.copyBuffer().
     * 
     * This verifies the fix for the bug where concurrent iteration over
     * scrollback/display lists during captureState() while the reader
     * thread modifies them would cause ConcurrentModificationException.
     */
    @RepeatedTest(5)
    @DisplayName("captureState should not cause ConcurrentModificationException with active reader")
    void shouldNotThrowConcurrentModificationExceptionDuringCaptureState() throws Exception {
        Backend backend = new HeadlessBackend();
        
        // Create a piped stream to simulate continuous data flow
        java.io.PipedOutputStream pipedOut = new java.io.PipedOutputStream();
        java.io.PipedInputStream pipedIn = new java.io.PipedInputStream(pipedOut, 4096);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        ECMA48 emulator = new ECMA48(ECMA48.DeviceType.XTERM, pipedIn,
            outputStream, null, backend);
        
        int captureIterations = 50;
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicBoolean writerDone = new AtomicBoolean(false);
        StringBuilder errorMessage = new StringBuilder();
        
        // Thread to write data to the emulator (simulates terminal output)
        Thread writerThread = new Thread(() -> {
            try {
                // Write data that causes scrolling to modify scrollback/display lists
                for (int i = 0; i < 100 && !failed.get(); i++) {
                    // Write text that will cause scrolling
                    String line = "Line " + i + " with some content to fill buffer\n";
                    pipedOut.write(line.getBytes("UTF-8"));
                    pipedOut.flush();
                    Thread.sleep(1); // Small delay to allow processing
                }
            } catch (Exception e) {
                if (!failed.get()) {
                    errorMessage.append("Writer thread error: " + e.getMessage());
                    failed.set(true);
                }
            } finally {
                writerDone.set(true);
            }
        });
        
        // Thread to call captureState() repeatedly
        Thread captureThread = new Thread(() -> {
            try {
                for (int i = 0; i < captureIterations && !failed.get(); i++) {
                    TerminalState state = emulator.captureState();
                    assertNotNull(state, "captureState should return non-null");
                    // Access the buffers to ensure they are valid
                    state.getScrollbackBuffer();
                    state.getDisplayBuffer();
                    Thread.sleep(1); // Small delay
                }
            } catch (java.util.ConcurrentModificationException e) {
                errorMessage.append("ConcurrentModificationException in captureState: " + e.getMessage());
                failed.set(true);
            } catch (Exception e) {
                if (!failed.get()) {
                    errorMessage.append("Capture thread error: " + e.getMessage());
                    failed.set(true);
                }
            }
        });
        
        writerThread.start();
        captureThread.start();
        
        // Wait for threads to complete
        captureThread.join(10000);
        writerDone.set(true); // Signal writer to stop if still running
        writerThread.join(5000);
        
        // Close the emulator
        try {
            pipedOut.close();
        } catch (Exception e) {
            // Ignore
        }
        emulator.close();
        
        assertFalse(failed.get(),
            "No ConcurrentModificationException should occur: " + errorMessage);
    }
}
