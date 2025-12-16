/*
 * Casciian - Java Text User Interface
 *
 * Integration tests for backend package
 */
package casciian.backend;

import casciian.bits.BorderStyle;
import casciian.bits.CellAttributes;
import casciian.event.TInputEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for backend components working together.
 * These tests validate the interaction between multiple classes and components.
 */
@DisplayName("Backend Integration Tests")
class BackendIntegrationTest {

    @Test
    @DisplayName("HeadlessBackend complete workflow")
    void testHeadlessBackendWorkflow() {
        // Create a backend
        HeadlessBackend backend = new HeadlessBackend();
        
        // Get session and screen
        SessionInfo session = backend.getSessionInfo();
        Screen screen = backend.getScreen();
        
        // Verify initialization
        assertNotNull(session);
        assertNotNull(screen);
        assertEquals(80, session.getWindowWidth());
        assertEquals(24, session.getWindowHeight());
        
        // Perform drawing operations
        CellAttributes attr = new CellAttributes();
        screen.putStringXY(0, 0, "Test Application", attr);
        screen.drawBox(5, 5, 30, 15, attr, attr, BorderStyle.SINGLE, false);
        screen.hLineXY(0, 10, 40, '-', attr);
        screen.vLineXY(20, 0, 20, '|', attr);
        screen.putCursor(true, 10, 10);
        
        // Flush screen
        assertDoesNotThrow(() -> backend.flushScreen());
        
        // Check events
        assertFalse(backend.hasEvents());
        List<TInputEvent> events = new ArrayList<>();
        backend.getEvents(events);
        assertTrue(events.isEmpty());
        
        // Update session
        session.setIdleTime(100);
        assertEquals(100, session.getIdleTime());
        
        // Clean up
        assertDoesNotThrow(() -> backend.shutdown());
    }

    @Test
    @DisplayName("MultiBackend with multiple HeadlessBackends")
    void testMultiBackendIntegration() {
        // Create first backend
        HeadlessBackend backend1 = new HeadlessBackend();
        
        // Create multi backend
        MultiBackend multiBackend = new MultiBackend(backend1);
        
        // Add more backends
        HeadlessBackend backend2 = new HeadlessBackend();
        HeadlessBackend backend3 = new HeadlessBackend();
        multiBackend.addBackend(backend2);
        multiBackend.addBackend(backend3);
        
        // Get screen and draw
        Screen screen = multiBackend.getScreen();
        CellAttributes attr = new CellAttributes();
        
        screen.putStringXY(0, 0, "Multi Backend Test", attr);
        screen.drawBox(10, 10, 50, 20, attr, attr, BorderStyle.DOUBLE, false);
        
        // Flush to all backends
        assertDoesNotThrow(() -> multiBackend.flushScreen());
        
        // Set properties that should propagate
        multiBackend.setTitle("Test Title");
        multiBackend.setListener(new Object());
        multiBackend.reloadOptions();
        
        // Handle events
        assertFalse(multiBackend.hasEvents());
        List<TInputEvent> events = new ArrayList<>();
        multiBackend.getEvents(events);
        
        // Remove a backend
        multiBackend.removeBackend(backend2);
        
        // Continue operations
        assertDoesNotThrow(() -> multiBackend.flushScreen());
        
        // Clean up
        assertDoesNotThrow(() -> multiBackend.shutdown());
    }

    @Test
    @DisplayName("Screen dimension changes propagate correctly")
    void testDimensionPropagation() {
        HeadlessBackend backend = new HeadlessBackend();
        Screen screen = backend.getScreen();
        SessionInfo session = backend.getSessionInfo();
        
        // Initial dimensions
        assertEquals(80, screen.getWidth());
        assertEquals(24, screen.getHeight());
        
        // Change screen dimensions
        screen.setDimensions(100, 50);
        
        // Verify change
        assertEquals(100, screen.getWidth());
        assertEquals(50, screen.getHeight());
        
        // Drawing should still work
        CellAttributes attr = new CellAttributes();
        assertDoesNotThrow(() -> {
            screen.putStringXY(50, 25, "In new area", attr);
            backend.flushScreen();
        });
        
        backend.shutdown();
    }

    @Test
    @DisplayName("MultiScreen synchronizes drawing operations")
    void testMultiScreenSynchronization() throws InterruptedException {
        // Create test screens that track copy operations
        class TestScreen extends LogicalScreen {
            private volatile boolean copied = false;
            private final Object copyLock = new Object();
            
            public TestScreen(int width, int height) {
                super(width, height);
            }
            
            @Override
            public void copyScreen(Screen screen) {
                synchronized (copyLock) {
                    super.copyScreen(screen);
                    copied = true;
                    copyLock.notifyAll();
                }
            }
            
            @Override
            public void flushPhysical() {
                // No-op for this test
            }
            
            public boolean waitForCopy(long timeoutMs) throws InterruptedException {
                synchronized (copyLock) {
                    long deadline = System.currentTimeMillis() + timeoutMs;
                    while (!copied && System.currentTimeMillis() < deadline) {
                        long remaining = deadline - System.currentTimeMillis();
                        if (remaining > 0) {
                            copyLock.wait(remaining);
                        }
                    }
                    return copied;
                }
            }
        }
        
        TestScreen screen1 = new TestScreen(80, 24);
        TestScreen screen2 = new TestScreen(80, 24);
        
        MultiScreen multiScreen = new MultiScreen(screen1);
        multiScreen.addScreen(screen2);
        
        // Draw on multi screen
        CellAttributes attr = new CellAttributes();
        multiScreen.putStringXY(5, 5, "Synchronized", attr);
        
        // Flush should propagate to all screens via copyScreen
        multiScreen.flushPhysical();
        
        // Wait for copy operations to complete using synchronization
        assertTrue(screen1.waitForCopy(1000), "Screen 1 should receive copy within timeout");
        assertTrue(screen2.waitForCopy(1000), "Screen 2 should receive copy within timeout");
        
        // Verify subsequent flush operations work correctly
        assertDoesNotThrow(() -> multiScreen.flushPhysical());
    }

    @Test
    @DisplayName("Session info updates work across backend lifecycle")
    void testSessionInfoLifecycle() {
        HeadlessBackend backend = new HeadlessBackend();
        SessionInfo session = backend.getSessionInfo();
        
        // Initial state
        long startTime = session.getStartTime();
        assertTrue(startTime > 0);
        
        // Update session properties
        session.setUsername("testuser");
        session.setLanguage("en_GB");
        session.setIdleTime(0);
        
        // Perform backend operations
        backend.flushScreen();
        List<TInputEvent> events = new ArrayList<>();
        backend.getEvents(events);
        
        // Session properties should persist
        assertEquals("testuser", session.getUsername());
        assertEquals("en_GB", session.getLanguage());
        assertEquals(0, session.getIdleTime());
        assertEquals(startTime, session.getStartTime());
        
        // Update idle time
        session.setIdleTime(100);
        assertEquals(100, session.getIdleTime());
        
        backend.shutdown();
    }

    @Test
    @DisplayName("Screen clipping and offset work together")
    void testClippingAndOffset() {
        HeadlessBackend backend = new HeadlessBackend();
        Screen screen = backend.getScreen();
        CellAttributes attr = new CellAttributes();
        
        // Set clipping region
        screen.setClipLeft(10);
        screen.setClipTop(5);
        screen.setClipRight(70);
        screen.setClipBottom(20);
        
        // Set offset
        screen.setOffsetX(5);
        screen.setOffsetY(3);
        
        // Draw within clipped region
        assertDoesNotThrow(() -> {
            screen.putStringXY(15, 10, "Clipped & Offset", attr);
            screen.hLineXY(10, 15, 30, '-', attr);
            backend.flushScreen();
        });
        
        // Verify settings persist
        assertEquals(10, screen.getClipLeft());
        assertEquals(5, screen.getClipTop());
        assertEquals(70, screen.getClipRight());
        assertEquals(20, screen.getClipBottom());
        assertEquals(5, screen.getOffsetX());
        assertEquals(3, screen.getOffsetY());
        
        backend.shutdown();
    }

    @Test
    @DisplayName("Complex drawing operations sequence")
    void testComplexDrawingSequence() {
        HeadlessBackend backend = new HeadlessBackend();
        Screen screen = backend.getScreen();
        CellAttributes attr = new CellAttributes();
        
        assertDoesNotThrow(() -> {
            // Draw border
            screen.drawBox(0, 0, 79, 23, attr, attr, BorderStyle.DOUBLE, false);
            
            // Draw title
            screen.putStringXY(2, 0, "[ Test Application ]", attr);
            
            // Draw menu
            screen.hLineXY(0, 2, 80, '-', attr);
            screen.putStringXY(2, 1, "File  Edit  View  Help", attr);
            
            // Draw content area
            for (int i = 0; i < 10; i++) {
                screen.putStringXY(5, 5 + i, "Line " + i + ": Test content", attr);
            }
            
            // Draw status bar
            screen.hLineXY(0, 21, 80, '-', attr);
            screen.putStringXY(2, 22, "Status: Ready", attr);
            
            // Position cursor
            screen.putCursor(true, 5, 5);
            
            // Flush
            backend.flushScreen();
            
            // Clear and redraw
            screen.clear();
            screen.putStringXY(10, 10, "Cleared and redrawn", attr);
            backend.flushScreen();
        });
        
        backend.shutdown();
    }

    @Test
    @DisplayName("Multiple backends share resources correctly")
    void testResourceSharing() {
        HeadlessBackend backend1 = new HeadlessBackend();
        MultiBackend multiBackend = new MultiBackend(backend1);
        
        HeadlessBackend backend2 = new HeadlessBackend();
        multiBackend.addBackend(backend2);
        
        // Each backend should maintain its own session
        assertNotSame(backend1.getSessionInfo(), backend2.getSessionInfo());
        
        // But multi backend should have a shared screen
        Screen multiScreen = multiBackend.getScreen();
        assertNotNull(multiScreen);
        assertInstanceOf(MultiScreen.class, multiScreen);
        
        // Operations should work on all backends
        CellAttributes attr = new CellAttributes();
        assertDoesNotThrow(() -> {
            multiScreen.putStringXY(10, 10, "Shared", attr);
            multiBackend.flushScreen();
            multiBackend.setTitle("Shared Title");
        });
        
        multiBackend.shutdown();
    }

    @Test
    @DisplayName("Backend recovery from dimension changes")
    void testDimensionChangeRecovery() {
        HeadlessBackend backend = new HeadlessBackend();
        Screen screen = backend.getScreen();
        CellAttributes attr = new CellAttributes();
        
        // Draw content
        screen.putStringXY(10, 10, "Original", attr);
        backend.flushScreen();
        
        // Change dimensions multiple times
        screen.setDimensions(100, 30);
        screen.setDimensions(120, 40);
        screen.setDimensions(80, 24);
        
        // Should still be able to draw
        assertDoesNotThrow(() -> {
            screen.putStringXY(10, 10, "Recovered", attr);
            backend.flushScreen();
        });
        
        assertEquals(80, screen.getWidth());
        assertEquals(24, screen.getHeight());
        
        backend.shutdown();
    }

    @Test
    @DisplayName("TTYSessionInfo integration with backend")
    void testTTYSessionInfoIntegration() {
        // Create session info
        TTYSessionInfo sessionInfo = new TTYSessionInfo();
        
        // Verify it behaves correctly
        assertNotNull(sessionInfo.getUsername());
        assertNotNull(sessionInfo.getLanguage());
        assertTrue(sessionInfo.getWindowWidth() > 0);
        assertTrue(sessionInfo.getWindowHeight() > 0);
        
        // Update properties
        sessionInfo.setUsername("integration_test");
        sessionInfo.setLanguage("en_US");
        sessionInfo.setIdleTime(50);
        
        // Query window size (should not throw)
        assertDoesNotThrow(() -> sessionInfo.queryWindowSize());
        
        // Properties should persist
        assertEquals("integration_test", sessionInfo.getUsername());
        assertEquals("en_US", sessionInfo.getLanguage());
        assertEquals(50, sessionInfo.getIdleTime());
    }

    @Test
    @DisplayName("Large scale drawing stress test")
    void testLargeScaleDrawing() {
        HeadlessBackend backend = new HeadlessBackend();
        Screen screen = backend.getScreen();
        
        // Increase screen size
        screen.setDimensions(200, 100);
        
        CellAttributes attr = new CellAttributes();
        
        assertDoesNotThrow(() -> {
            // Draw many elements
            for (int y = 0; y < 90; y += 5) {
                for (int x = 0; x < 190; x += 10) {
                    screen.putCharXY(x, y, '*', attr);
                }
            }
            
            // Draw boxes
            for (int i = 0; i < 10; i++) {
                screen.drawBox(10 + i * 15, 10 + i * 5, 
                              25 + i * 15, 15 + i * 5, 
                              attr, attr, BorderStyle.SINGLE, false);
            }
            
            // Draw lines
            screen.hLineXY(0, 50, 200, '=', attr);
            screen.vLineXY(100, 0, 100, '|', attr);
            
            backend.flushScreen();
        });
        
        backend.shutdown();
    }

    @Test
    @DisplayName("Backend operations are idempotent")
    void testIdempotentOperations() {
        HeadlessBackend backend = new HeadlessBackend();
        
        // Multiple initializations/operations should be safe
        assertDoesNotThrow(() -> {
            backend.setListener(new Object());
            backend.setListener(new Object());
            backend.setListener(null);
            
            backend.reloadOptions();
            backend.reloadOptions();
            
            backend.flushScreen();
            backend.flushScreen();
            backend.flushScreen();
            
            backend.shutdown();
            backend.shutdown();
        });
    }
}
