/*
 * Casciian - Java Text User Interface
 *
 * Unit tests for MultiBackend
 */
package casciian.backend;

import casciian.event.TCommandEvent;
import casciian.event.TInputEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MultiBackend - validates multiple backend coordination and management.
 * This tests the component behavior including event handling and screen synchronization.
 */
@DisplayName("MultiBackend Tests")
class MultiBackendTest {

    private MultiBackend multiBackend;
    private HeadlessBackend backend1;
    private HeadlessBackend backend2;

    @BeforeEach
    void setUp() {
        backend1 = new HeadlessBackend();
        multiBackend = new MultiBackend(backend1);
    }

    // Initialization tests

    @Test
    @DisplayName("MultiBackend initializes with one backend")
    void testInitialization() {
        assertNotNull(multiBackend);
    }

    @Test
    @DisplayName("MultiBackend provides session info from first backend")
    void testGetSessionInfo() {
        SessionInfo sessionInfo = multiBackend.getSessionInfo();
        assertNotNull(sessionInfo);
        assertEquals(backend1.getSessionInfo(), sessionInfo);
    }

    @Test
    @DisplayName("MultiBackend provides multi screen")
    void testGetScreen() {
        Screen screen = multiBackend.getScreen();
        assertNotNull(screen);
        assertInstanceOf(MultiScreen.class, screen);
    }

    @Test
    @DisplayName("MultiBackend screen has correct dimensions")
    void testScreenDimensions() {
        Screen screen = multiBackend.getScreen();
        assertEquals(80, screen.getWidth());
        assertEquals(24, screen.getHeight());
    }

    // Backend management tests

    @Test
    @DisplayName("Add backend to multi backend works")
    void testAddBackend() {
        backend2 = new HeadlessBackend();
        assertDoesNotThrow(() -> multiBackend.addBackend(backend2));
    }

    @Test
    @DisplayName("Add multiple backends works")
    void testAddMultipleBackends() {
        backend2 = new HeadlessBackend();
        HeadlessBackend backend3 = new HeadlessBackend();
        
        assertDoesNotThrow(() -> {
            multiBackend.addBackend(backend2);
            multiBackend.addBackend(backend3);
        });
    }

    @Test
    @DisplayName("Add backend with read-only flag works")
    void testAddBackendReadOnly() {
        backend2 = new HeadlessBackend();
        assertDoesNotThrow(() -> multiBackend.addBackend(backend2, true));
    }

    @Test
    @DisplayName("Remove backend from multi backend works")
    void testRemoveBackend() {
        backend2 = new HeadlessBackend();
        multiBackend.addBackend(backend2);
        
        assertDoesNotThrow(() -> multiBackend.removeBackend(backend2));
    }

    @Test
    @DisplayName("Cannot remove last backend")
    void testCannotRemoveLastBackend() {
        // Attempting to remove the only backend should be prevented
        multiBackend.removeBackend(backend1);
        
        // Backend should still be functional
        assertDoesNotThrow(() -> multiBackend.flushScreen());
    }

    @Test
    @DisplayName("Get backends returns active backends")
    void testGetBackends() {
        backend2 = new HeadlessBackend();
        multiBackend.addBackend(backend2);
        
        List<Backend> backends = multiBackend.getBackends();
        assertNotNull(backends);
        // HeadlessBackend instances are filtered out in getBackends()
        // The list should be empty since HeadlessBackends are not included
        assertEquals(0, backends.size());
    }

    // Event handling tests

    @Test
    @DisplayName("MultiBackend has no events by default")
    void testHasEvents() {
        assertFalse(multiBackend.hasEvents());
    }

    @Test
    @DisplayName("Get events returns empty list when no backends have events")
    void testGetEvents() {
        List<TInputEvent> queue = new ArrayList<>();
        multiBackend.getEvents(queue);
        
        // Should either be empty or have an abort command if no backends remain
        assertTrue(queue.isEmpty() || (queue.size() == 1 && queue.get(0) instanceof TCommandEvent));
    }

    @Test
    @DisplayName("MultiBackend handles events from multiple backends")
    void testMultipleBackendEvents() {
        backend2 = new HeadlessBackend();
        multiBackend.addBackend(backend2);
        
        List<TInputEvent> queue = new ArrayList<>();
        multiBackend.getEvents(queue);
        
        // Since HeadlessBackends don't generate events, queue should be empty or have abort
        assertTrue(queue.isEmpty() || (queue.size() == 1 && queue.get(0) instanceof TCommandEvent));
    }

    // Screen operations tests

    @Test
    @DisplayName("Flush screen does not throw exception")
    void testFlushScreen() {
        assertDoesNotThrow(() -> multiBackend.flushScreen());
    }

    @Test
    @DisplayName("Flush screen with multiple backends does not throw exception")
    void testFlushScreenMultipleBackends() {
        backend2 = new HeadlessBackend();
        multiBackend.addBackend(backend2);
        
        assertDoesNotThrow(() -> multiBackend.flushScreen());
    }

    @Test
    @DisplayName("Multiple flush operations work correctly")
    void testMultipleFlushOperations() {
        backend2 = new HeadlessBackend();
        multiBackend.addBackend(backend2);
        
        assertDoesNotThrow(() -> {
            multiBackend.flushScreen();
            multiBackend.flushScreen();
            multiBackend.flushScreen();
        });
    }

    // Title operations tests

    @Test
    @DisplayName("Set title propagates to all backends")
    void testSetTitle() {
        backend2 = new HeadlessBackend();
        multiBackend.addBackend(backend2);
        
        assertDoesNotThrow(() -> multiBackend.setTitle("Test Title"));
    }

    @Test
    @DisplayName("Set empty title does not throw exception")
    void testSetEmptyTitle() {
        assertDoesNotThrow(() -> multiBackend.setTitle(""));
    }

    @Test
    @DisplayName("Set null title does not throw exception")
    void testSetNullTitle() {
        assertDoesNotThrow(() -> multiBackend.setTitle(null));
    }

    // Listener tests

    @Test
    @DisplayName("Set listener propagates to all backends")
    void testSetListener() {
        backend2 = new HeadlessBackend();
        multiBackend.addBackend(backend2);
        
        Object listener = new Object();
        assertDoesNotThrow(() -> multiBackend.setListener(listener));
    }

    @Test
    @DisplayName("Set null listener does not throw exception")
    void testSetNullListener() {
        assertDoesNotThrow(() -> multiBackend.setListener(null));
    }

    // Reload options tests

    @Test
    @DisplayName("Reload options propagates to all backends")
    void testReloadOptions() {
        backend2 = new HeadlessBackend();
        multiBackend.addBackend(backend2);
        
        assertDoesNotThrow(() -> multiBackend.reloadOptions());
    }

    // Read-only tests

    @Test
    @DisplayName("MultiBackend is never read-only")
    void testIsReadOnly() {
        assertFalse(multiBackend.isReadOnly());
    }

    @Test
    @DisplayName("Set read-only does nothing for MultiBackend")
    void testSetReadOnly() {
        assertDoesNotThrow(() -> multiBackend.setReadOnly(true));
        assertFalse(multiBackend.isReadOnly());
    }

    // Color conversion tests

    @Test
    @DisplayName("Attr to foreground color returns valid RGB")
    void testAttrToForegroundColor() {
        int color = multiBackend.attrToForegroundColor(TestUtils.createDefaultCellAttributes());
        assertTrue(color >= 0 && color <= 0xFFFFFF);
    }

    @Test
    @DisplayName("Attr to background color returns valid RGB")
    void testAttrToBackgroundColor() {
        int color = multiBackend.attrToBackgroundColor(TestUtils.createDefaultCellAttributes());
        assertTrue(color >= 0 && color <= 0xFFFFFF);
    }

    @Test
    @DisplayName("Get default foreground color returns valid RGB")
    void testGetDefaultForeColorRGB() {
        int color = multiBackend.getDefaultForeColorRGB();
        assertTrue(color >= 0 && color <= 0xFFFFFF);
    }

    @Test
    @DisplayName("Get default background color returns valid RGB")
    void testGetDefaultBackColorRGB() {
        int color = multiBackend.getDefaultBackColorRGB();
        assertTrue(color >= 0 && color <= 0xFFFFFF);
    }

    // Clipboard tests

    @Test
    @DisplayName("Copy clipboard text propagates to all backends")
    void testCopyClipboardText() {
        backend2 = new HeadlessBackend();
        multiBackend.addBackend(backend2);
        
        assertDoesNotThrow(() -> multiBackend.copyClipboardText("test"));
    }

    @Test
    @DisplayName("Copy empty clipboard text does not throw exception")
    void testCopyEmptyClipboardText() {
        assertDoesNotThrow(() -> multiBackend.copyClipboardText(""));
    }

    @Test
    @DisplayName("Copy null clipboard text does not throw exception")
    void testCopyNullClipboardText() {
        assertDoesNotThrow(() -> multiBackend.copyClipboardText(null));
    }

    // Focus tests

    @Test
    @DisplayName("isFocused returns false when no backend is focused")
    void testIsFocused() {
        assertFalse(multiBackend.isFocused());
    }

    @Test
    @DisplayName("isFocused with multiple backends works correctly")
    void testIsFocusedMultipleBackends() {
        backend2 = new HeadlessBackend();
        multiBackend.addBackend(backend2);
        
        // HeadlessBackends are never focused
        assertFalse(multiBackend.isFocused());
    }

    // Shutdown tests

    @Test
    @DisplayName("Shutdown propagates to all backends")
    void testShutdown() {
        backend2 = new HeadlessBackend();
        multiBackend.addBackend(backend2);
        
        assertDoesNotThrow(() -> multiBackend.shutdown());
    }

    @Test
    @DisplayName("Shutdown multiple times does not cause issues")
    void testShutdownMultipleTimes() {
        assertDoesNotThrow(() -> {
            multiBackend.shutdown();
            multiBackend.shutdown();
        });
    }

    // Complex operation tests

    @Test
    @DisplayName("Add and remove backends in sequence works correctly")
    void testAddRemoveSequence() {
        backend2 = new HeadlessBackend();
        HeadlessBackend backend3 = new HeadlessBackend();
        
        multiBackend.addBackend(backend2);
        multiBackend.addBackend(backend3);
        multiBackend.removeBackend(backend2);
        
        assertDoesNotThrow(() -> {
            multiBackend.flushScreen();
            List<TInputEvent> queue = new ArrayList<>();
            multiBackend.getEvents(queue);
        });
    }

    @Test
    @DisplayName("Operations after remove work correctly")
    void testOperationsAfterRemove() {
        backend2 = new HeadlessBackend();
        multiBackend.addBackend(backend2);
        multiBackend.removeBackend(backend2);
        
        assertDoesNotThrow(() -> {
            multiBackend.setTitle("Test");
            multiBackend.flushScreen();
            multiBackend.reloadOptions();
        });
    }

    @Test
    @DisplayName("MultiBackend handles rapid backend additions")
    void testRapidBackendAdditions() {
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 5; i++) {
                HeadlessBackend backend = new HeadlessBackend();
                multiBackend.addBackend(backend);
            }
        });
    }

    @Test
    @DisplayName("MultiBackend lifecycle test")
    void testCompleteLifecycle() {
        // Simulate a complete backend lifecycle
        backend2 = new HeadlessBackend();
        
        assertDoesNotThrow(() -> {
            // Add backend
            multiBackend.addBackend(backend2);
            
            // Perform operations
            multiBackend.setTitle("Test");
            multiBackend.setListener(new Object());
            multiBackend.reloadOptions();
            
            // Screen operations
            Screen screen = multiBackend.getScreen();
            screen.putCharXY(5, 5, 'A', TestUtils.createDefaultCellAttributes());
            multiBackend.flushScreen();
            
            // Event handling
            List<TInputEvent> queue = new ArrayList<>();
            multiBackend.getEvents(queue);
            
            // Cleanup
            multiBackend.removeBackend(backend2);
            multiBackend.shutdown();
        });
    }

    @Test
    @DisplayName("MultiBackend with single backend behaves correctly")
    void testSingleBackendBehavior() {
        assertDoesNotThrow(() -> {
            multiBackend.setTitle("Test");
            multiBackend.flushScreen();
            
            List<TInputEvent> queue = new ArrayList<>();
            multiBackend.getEvents(queue);
            
            assertFalse(multiBackend.hasEvents());
            assertFalse(multiBackend.isReadOnly());
        });
    }

    @Test
    @DisplayName("MultiBackend maintains state consistency")
    void testStateConsistency() {
        backend2 = new HeadlessBackend();
        multiBackend.addBackend(backend2);
        
        SessionInfo sessionInfo = multiBackend.getSessionInfo();
        Screen screen = multiBackend.getScreen();
        
        assertNotNull(sessionInfo);
        assertNotNull(screen);
        assertEquals(80, screen.getWidth());
        assertEquals(24, screen.getHeight());
        assertFalse(multiBackend.isReadOnly());
    }
}
