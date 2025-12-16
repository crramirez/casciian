/*
 * Casciian - Java Text User Interface
 *
 * Unit tests for Backend interface through implementations
 */
package casciian.backend;

import casciian.bits.CellAttributes;
import casciian.event.TInputEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Backend interface - validates the contract through concrete implementations.
 * These tests ensure that all backend implementations adhere to the Backend interface contract.
 */
@DisplayName("Backend Interface Tests")
class BackendTest {

    private HeadlessBackend backend;

    @BeforeEach
    void setUp() {
        backend = new HeadlessBackend();
    }

    @Test
    @DisplayName("Backend interface contract: getSessionInfo never returns null")
    void testGetSessionInfoContract() {
        assertNotNull(backend.getSessionInfo(), "getSessionInfo must never return null");
    }

    @Test
    @DisplayName("Backend interface contract: getScreen never returns null")
    void testGetScreenContract() {
        assertNotNull(backend.getScreen(), "getScreen must never return null");
    }

    @Test
    @DisplayName("Backend interface contract: flushScreen is idempotent")
    void testFlushScreenIdempotent() {
        assertDoesNotThrow(() -> {
            backend.flushScreen();
            backend.flushScreen();
            backend.flushScreen();
        }, "Multiple flushScreen calls should be safe");
    }

    @Test
    @DisplayName("Backend interface contract: hasEvents is consistent")
    void testHasEventsConsistent() {
        // For HeadlessBackend, should consistently return false
        assertFalse(backend.hasEvents());
        assertFalse(backend.hasEvents());
        assertFalse(backend.hasEvents());
    }

    @Test
    @DisplayName("Backend interface contract: getEvents with empty queue")
    void testGetEventsWithEmptyQueue() {
        List<TInputEvent> queue = new ArrayList<>();
        backend.getEvents(queue);
        assertTrue(queue.isEmpty(), "Queue should remain empty when backend has no events");
    }

    @Test
    @DisplayName("Backend interface contract: getEvents with pre-filled queue")
    void testGetEventsWithPreFilledQueue() {
        List<TInputEvent> queue = new ArrayList<>();
        queue.add(null); // Add a dummy entry
        int initialSize = queue.size();
        
        backend.getEvents(queue);
        
        // Queue size should not decrease (backend should only append)
        assertTrue(queue.size() >= initialSize, "Backend should not remove existing events");
    }

    @Test
    @DisplayName("Backend interface contract: shutdown is idempotent")
    void testShutdownIdempotent() {
        assertDoesNotThrow(() -> {
            backend.shutdown();
            backend.shutdown();
        }, "Multiple shutdown calls should be safe");
    }

    @Test
    @DisplayName("Backend interface contract: setTitle with various strings")
    void testSetTitleContract() {
        assertDoesNotThrow(() -> backend.setTitle(""));
        assertDoesNotThrow(() -> backend.setTitle("Normal Title"));
        assertDoesNotThrow(() -> backend.setTitle("Title with 特殊字符"));
        assertDoesNotThrow(() -> backend.setTitle("Very long title ".repeat(10)));
    }

    @Test
    @DisplayName("Backend interface contract: setTitle with null")
    void testSetTitleWithNull() {
        assertDoesNotThrow(() -> backend.setTitle(null), 
            "setTitle should handle null gracefully");
    }

    @Test
    @DisplayName("Backend interface contract: setListener with various objects")
    void testSetListenerContract() {
        assertDoesNotThrow(() -> backend.setListener(new Object()));
        assertDoesNotThrow(() -> backend.setListener(new Thread()));
        assertDoesNotThrow(() -> backend.setListener(null));
    }

    @Test
    @DisplayName("Backend interface contract: reloadOptions can be called multiple times")
    void testReloadOptionsMultipleTimes() {
        assertDoesNotThrow(() -> {
            backend.reloadOptions();
            backend.reloadOptions();
            backend.reloadOptions();
        });
    }

    @Test
    @DisplayName("Backend interface contract: setReadOnly toggles flag")
    void testReadOnlyFlagToggle() {
        // Note: HeadlessBackend's setReadOnly is a no-op and always returns true
        // This test uses GenericBackend implementation to properly test the feature
        
        // Create a testable backend that actually implements setReadOnly
        TerminalReader mockTerminal = Mockito.mock(TerminalReader.class);
        Screen mockScreen = Mockito.mock(Screen.class);
        SessionInfo mockSessionInfo = Mockito.mock(SessionInfo.class);
        
        GenericBackend testBackend = new GenericBackend() {
            @Override
            public int attrToForegroundColor(CellAttributes attr) { return 0xFFFFFF; }
            @Override
            public int attrToBackgroundColor(CellAttributes attr) { return 0x000000; }
            @Override
            public void copyClipboardText(String text) { }
            @Override
            public boolean isFocused() { return false; }
            @Override
            public int getDefaultForeColorRGB() { return 0xFFFFFF; }
            @Override
            public int getDefaultBackColorRGB() { return 0x000000; }
        };
        testBackend.terminal = mockTerminal;
        testBackend.screen = mockScreen;
        testBackend.sessionInfo = mockSessionInfo;
        
        // Test that setReadOnly actually changes the value
        assertFalse(testBackend.isReadOnly(), "Should start as not read-only");
        
        testBackend.setReadOnly(true);
        assertTrue(testBackend.isReadOnly(), "Should be read-only after setting to true");
        
        testBackend.setReadOnly(false);
        assertFalse(testBackend.isReadOnly(), "Should not be read-only after setting to false");
    }

    @Test
    @DisplayName("Backend interface contract: attrToForegroundColor returns valid RGB")
    void testAttrToForegroundColorValidRange() {
        CellAttributes attr = new CellAttributes();
        int color = backend.attrToForegroundColor(attr);
        
        assertTrue(color >= 0, "RGB color must be non-negative");
        assertTrue(color <= 0xFFFFFF, "RGB color must be within 24-bit range");
    }

    @Test
    @DisplayName("Backend interface contract: attrToBackgroundColor returns valid RGB")
    void testAttrToBackgroundColorValidRange() {
        CellAttributes attr = new CellAttributes();
        int color = backend.attrToBackgroundColor(attr);
        
        assertTrue(color >= 0, "RGB color must be non-negative");
        assertTrue(color <= 0xFFFFFF, "RGB color must be within 24-bit range");
    }

    @Test
    @DisplayName("Backend interface contract: copyClipboardText with various strings")
    void testCopyClipboardTextContract() {
        assertDoesNotThrow(() -> backend.copyClipboardText(""));
        assertDoesNotThrow(() -> backend.copyClipboardText("Simple text"));
        assertDoesNotThrow(() -> backend.copyClipboardText("Text with\nnewlines\n"));
        assertDoesNotThrow(() -> backend.copyClipboardText("Unicode: 你好世界 🎉"));
    }

    @Test
    @DisplayName("Backend interface contract: isFocused returns boolean")
    void testIsFocusedReturnType() {
        assertDoesNotThrow(() -> {
            boolean focused = backend.isFocused();
            // Just verify it returns without exception
            assertNotNull(Boolean.valueOf(focused));
        });
    }

    @Test
    @DisplayName("Backend interface contract: getDefaultForeColorRGB returns valid RGB")
    void testGetDefaultForeColorRGBValidRange() {
        int color = backend.getDefaultForeColorRGB();
        
        assertTrue(color >= 0, "Default foreground RGB must be non-negative");
        assertTrue(color <= 0xFFFFFF, "Default foreground RGB must be within 24-bit range");
    }

    @Test
    @DisplayName("Backend interface contract: getDefaultBackColorRGB returns valid RGB")
    void testGetDefaultBackColorRGBValidRange() {
        int color = backend.getDefaultBackColorRGB();
        
        assertTrue(color >= 0, "Default background RGB must be non-negative");
        assertTrue(color <= 0xFFFFFF, "Default background RGB must be within 24-bit range");
    }

    @Test
    @DisplayName("Backend interface contract: operations work in typical sequence")
    void testTypicalOperationSequence() {
        assertDoesNotThrow(() -> {
            // Initialization phase
            SessionInfo session = backend.getSessionInfo();
            Screen screen = backend.getScreen();
            assertNotNull(session);
            assertNotNull(screen);
            
            // Configuration phase
            backend.setTitle("Test Window");
            backend.setListener(new Object());
            backend.setReadOnly(false);
            backend.reloadOptions();
            
            // Operation phase
            CellAttributes attr = new CellAttributes();
            screen.putStringXY(0, 0, "Hello", attr);
            backend.flushScreen();
            
            // Event phase
            List<TInputEvent> events = new ArrayList<>();
            backend.hasEvents();
            backend.getEvents(events);
            
            // Cleanup phase
            backend.copyClipboardText("test");
            backend.shutdown();
        });
    }

    @Test
    @DisplayName("Backend interface contract: screen dimensions match session")
    void testScreenDimensionsMatchSession() {
        SessionInfo session = backend.getSessionInfo();
        Screen screen = backend.getScreen();
        
        assertEquals(session.getWindowWidth(), screen.getWidth(),
            "Screen width should match session window width");
        assertEquals(session.getWindowHeight(), screen.getHeight(),
            "Screen height should match session window height");
    }

    @Test
    @DisplayName("Backend interface contract: color methods are consistent")
    void testColorMethodsConsistent() {
        CellAttributes attr = new CellAttributes();
        
        // Call multiple times - should return consistent results
        int fg1 = backend.attrToForegroundColor(attr);
        int fg2 = backend.attrToForegroundColor(attr);
        int bg1 = backend.attrToBackgroundColor(attr);
        int bg2 = backend.attrToBackgroundColor(attr);
        
        assertEquals(fg1, fg2, "attrToForegroundColor should be deterministic");
        assertEquals(bg1, bg2, "attrToBackgroundColor should be deterministic");
    }

    @Test
    @DisplayName("Backend interface contract: default colors are consistent")
    void testDefaultColorsConsistent() {
        int defaultFg1 = backend.getDefaultForeColorRGB();
        int defaultFg2 = backend.getDefaultForeColorRGB();
        int defaultBg1 = backend.getDefaultBackColorRGB();
        int defaultBg2 = backend.getDefaultBackColorRGB();
        
        assertEquals(defaultFg1, defaultFg2, "Default foreground color should be consistent");
        assertEquals(defaultBg1, defaultBg2, "Default background color should be consistent");
    }

    @Test
    @DisplayName("Backend interface contract: operations after shutdown don't crash")
    void testOperationsAfterShutdown() {
        backend.shutdown();
        
        // These operations should not throw exceptions after shutdown
        assertDoesNotThrow(() -> backend.getSessionInfo());
        assertDoesNotThrow(() -> backend.getScreen());
        assertDoesNotThrow(() -> backend.hasEvents());
        assertDoesNotThrow(() -> backend.isReadOnly());
        assertDoesNotThrow(() -> backend.isFocused());
    }
}
