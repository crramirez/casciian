/*
 * Casciian - Java Text User Interface
 *
 * Unit tests for HeadlessBackend
 */
package casciian.backend;

import casciian.bits.CellAttributes;
import casciian.event.TInputEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HeadlessBackend - validates a complete backend component
 * that combines screen and session functionality without actual I/O.
 */
@DisplayName("HeadlessBackend Tests")
class HeadlessBackendTest {

    private HeadlessBackend backend;

    @BeforeEach
    void setUp() {
        backend = new HeadlessBackend();
    }

    @Test
    @DisplayName("Backend initializes successfully")
    void testBackendInitialization() {
        assertNotNull(backend);
    }

    @Test
    @DisplayName("Backend provides a session info")
    void testGetSessionInfo() {
        SessionInfo sessionInfo = backend.getSessionInfo();
        assertNotNull(sessionInfo);
        assertInstanceOf(TSessionInfo.class, sessionInfo);
    }

    @Test
    @DisplayName("Backend provides a screen")
    void testGetScreen() {
        Screen screen = backend.getScreen();
        assertNotNull(screen);
        assertSame(backend, screen, "HeadlessBackend acts as its own screen");
    }

    @Test
    @DisplayName("Backend session has default 80x24 dimensions")
    void testSessionDimensions() {
        SessionInfo sessionInfo = backend.getSessionInfo();
        assertEquals(80, sessionInfo.getWindowWidth());
        assertEquals(24, sessionInfo.getWindowHeight());
    }

    @Test
    @DisplayName("Backend screen has default 80x24 dimensions")
    void testScreenDimensions() {
        Screen screen = backend.getScreen();
        assertEquals(80, screen.getWidth());
        assertEquals(24, screen.getHeight());
    }

    @Test
    @DisplayName("Flush screen does not throw exception")
    void testFlushScreen() {
        assertDoesNotThrow(() -> backend.flushScreen());
    }

    @Test
    @DisplayName("Flush screen multiple times does not cause issues")
    void testFlushScreenMultipleTimes() {
        assertDoesNotThrow(() -> {
            backend.flushScreen();
            backend.flushScreen();
            backend.flushScreen();
        });
    }

    @Test
    @DisplayName("Backend has no events by default")
    void testHasEvents() {
        assertFalse(backend.hasEvents());
    }

    @Test
    @DisplayName("Get events returns empty list")
    void testGetEvents() {
        List<TInputEvent> queue = new ArrayList<>();
        backend.getEvents(queue);
        assertTrue(queue.isEmpty());
    }

    @Test
    @DisplayName("Get events does not modify pre-filled list")
    void testGetEventsDoesNotClearExistingEvents() {
        List<TInputEvent> queue = new ArrayList<>();
        // Add a dummy event to test that getEvents doesn't clear it
        queue.add(null);
        int initialSize = queue.size();
        
        backend.getEvents(queue);
        assertEquals(initialSize, queue.size(), "getEvents should not clear or add to existing queue");
    }

    @Test
    @DisplayName("Shutdown does not throw exception")
    void testShutdown() {
        assertDoesNotThrow(() -> backend.shutdown());
    }

    @Test
    @DisplayName("Shutdown multiple times does not cause issues")
    void testShutdownMultipleTimes() {
        assertDoesNotThrow(() -> {
            backend.shutdown();
            backend.shutdown();
        });
    }

    @Test
    @DisplayName("Set listener does not throw exception")
    void testSetListener() {
        Object listener = new Object();
        assertDoesNotThrow(() -> backend.setListener(listener));
    }

    @Test
    @DisplayName("Set listener with null does not throw exception")
    void testSetListenerNull() {
        assertDoesNotThrow(() -> backend.setListener(null));
    }

    @Test
    @DisplayName("Reload options does not throw exception")
    void testReloadOptions() {
        assertDoesNotThrow(() -> backend.reloadOptions());
    }

    @Test
    @DisplayName("Backend is read-only by default")
    void testIsReadOnly() {
        assertTrue(backend.isReadOnly());
    }

    @Test
    @DisplayName("Set read-only does not throw exception")
    void testSetReadOnly() {
        assertDoesNotThrow(() -> backend.setReadOnly(false));
        assertDoesNotThrow(() -> backend.setReadOnly(true));
    }

    @Test
    @DisplayName("Backend does not support images over text")
    void testIsImagesOverText() {
        assertFalse(backend.isImagesOverText());
    }

    @Test
    @DisplayName("Backend does not support pixel mouse")
    void testIsPixelMouse() {
        assertFalse(backend.isPixelMouse());
    }

    @Test
    @DisplayName("Set pixel mouse does not throw exception")
    void testSetPixelMouse() {
        assertDoesNotThrow(() -> backend.setPixelMouse(true));
        assertDoesNotThrow(() -> backend.setPixelMouse(false));
    }

    @Test
    @DisplayName("Attr to foreground color returns a valid RGB value")
    void testAttrToForegroundColor() {
        CellAttributes attr = new CellAttributes();
        int color = backend.attrToForegroundColor(attr);
        // RGB color should be a valid 24-bit value (0x000000 to 0xFFFFFF)
        assertTrue(color >= 0 && color <= 0xFFFFFF);
    }

    @Test
    @DisplayName("Attr to background color returns a valid RGB value")
    void testAttrToBackgroundColor() {
        CellAttributes attr = new CellAttributes();
        int color = backend.attrToBackgroundColor(attr);
        // RGB color should be a valid 24-bit value
        assertTrue(color >= 0 && color <= 0xFFFFFF);
    }

    @Test
    @DisplayName("Get default foreground color returns a valid RGB value")
    void testGetDefaultForeColorRGB() {
        int color = backend.getDefaultForeColorRGB();
        assertTrue(color >= 0 && color <= 0xFFFFFF);
    }

    @Test
    @DisplayName("Get default background color returns a valid RGB value")
    void testGetDefaultBackColorRGB() {
        int color = backend.getDefaultBackColorRGB();
        assertTrue(color >= 0 && color <= 0xFFFFFF);
    }

    @Test
    @DisplayName("Copy clipboard text does not throw exception")
    void testCopyClipboardText() {
        assertDoesNotThrow(() -> backend.copyClipboardText("test"));
    }

    @Test
    @DisplayName("Copy clipboard text with empty string does not throw exception")
    void testCopyClipboardTextEmpty() {
        assertDoesNotThrow(() -> backend.copyClipboardText(""));
    }

    @Test
    @DisplayName("Copy clipboard text with null does not throw exception")
    void testCopyClipboardTextNull() {
        assertDoesNotThrow(() -> backend.copyClipboardText(null));
    }

    @Test
    @DisplayName("Backend is not focused by default")
    void testIsFocused() {
        assertFalse(backend.isFocused());
    }

    @Test
    @DisplayName("Backend operations work in sequence")
    void testBackendOperationSequence() {
        // Simulate a typical backend lifecycle
        assertDoesNotThrow(() -> {
            Screen screen = backend.getScreen();
            assertNotNull(screen);
            
            backend.setListener(new Object());
            backend.reloadOptions();
            backend.flushScreen();
            
            List<TInputEvent> queue = new ArrayList<>();
            backend.getEvents(queue);
            assertTrue(queue.isEmpty());
            
            assertFalse(backend.hasEvents());
            
            backend.shutdown();
        });
    }

    @Test
    @DisplayName("Screen drawing operations do not throw exceptions")
    void testScreenDrawingOperations() {
        Screen screen = backend.getScreen();
        CellAttributes attr = new CellAttributes();
        
        assertDoesNotThrow(() -> {
            screen.putCharXY(0, 0, 'A', attr);
            screen.putStringXY(1, 1, "Hello", attr);
            screen.vLineXY(5, 0, 5, '|', attr);
            screen.hLineXY(0, 5, 10, '-', attr);
        });
        
        backend.flushScreen();
    }

    @Test
    @DisplayName("Screen clipping operations work correctly")
    void testScreenClippingOperations() {
        Screen screen = backend.getScreen();
        
        assertDoesNotThrow(() -> {
            screen.setClipLeft(0);
            screen.setClipTop(0);
            screen.setClipRight(79);
            screen.setClipBottom(23);
            
            assertEquals(0, screen.getClipLeft());
            assertEquals(0, screen.getClipTop());
            assertEquals(79, screen.getClipRight());
            assertEquals(23, screen.getClipBottom());
        });
    }

    @Test
    @DisplayName("Screen offset operations work correctly")
    void testScreenOffsetOperations() {
        Screen screen = backend.getScreen();
        
        assertDoesNotThrow(() -> {
            screen.setOffsetX(10);
            screen.setOffsetY(5);
            
            assertEquals(10, screen.getOffsetX());
            assertEquals(5, screen.getOffsetY());
        });
    }

    @Test
    @DisplayName("Backend handles multiple screen operations")
    void testMultipleScreenOperations() {
        Screen screen = backend.getScreen();
        CellAttributes attr = new CellAttributes();
        
        for (int i = 0; i < 10; i++) {
            screen.putCharXY(i, 0, (char)('A' + i), attr);
        }
        
        backend.flushScreen();
        assertFalse(backend.hasEvents());
    }
}
