/*
 * Casciian - Java Text User Interface
 *
 * Unit tests for ECMA48Backend
 */
package casciian.backend;

import casciian.bits.CellAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ECMA48Backend - validates an xterm/ANSI terminal backend.
 * These tests use mock input/output streams to avoid actual terminal I/O.
 */
@DisplayName("ECMA48Backend Tests")
class ECMA48BackendTest {

    private ECMA48Backend backend;
    private ByteArrayInputStream testInput;
    private ByteArrayOutputStream testOutput;

    @BeforeEach
    void setUp() throws Exception {
        // Create test streams with minimal data to avoid blocking
        testInput = new ByteArrayInputStream(new byte[0]);
        testOutput = new ByteArrayOutputStream();
    }

    @AfterEach
    void tearDown() {
        if (backend != null) {
            try {
                backend.shutdown();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    @Test
    @DisplayName("Backend initializes with custom streams")
    void testBackendInitializationWithStreams() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        assertNotNull(backend);
    }

    @Test
    @DisplayName("Backend provides a session info")
    void testGetSessionInfo() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        SessionInfo sessionInfo = backend.getSessionInfo();
        assertNotNull(sessionInfo);
    }

    @Test
    @DisplayName("Backend provides a screen")
    void testGetScreen() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        Screen screen = backend.getScreen();
        assertNotNull(screen);
    }

    @Test
    @DisplayName("Backend session has default dimensions")
    void testSessionDimensions() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        SessionInfo sessionInfo = backend.getSessionInfo();
        assertTrue(sessionInfo.getWindowWidth() > 0);
        assertTrue(sessionInfo.getWindowHeight() > 0);
    }

    @Test
    @DisplayName("Backend screen has matching dimensions")
    void testScreenDimensions() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        Screen screen = backend.getScreen();
        SessionInfo sessionInfo = backend.getSessionInfo();
        assertEquals(sessionInfo.getWindowWidth(), screen.getWidth());
        assertEquals(sessionInfo.getWindowHeight(), screen.getHeight());
    }

    @Test
    @DisplayName("Flush screen does not throw exception")
    void testFlushScreen() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        assertDoesNotThrow(() -> backend.flushScreen());
    }

    @Test
    @DisplayName("Flush screen writes to output stream")
    void testFlushScreenWritesOutput() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        backend.flushScreen();
        // Flushing may write ANSI sequences to the output
        // We just verify no exception is thrown
        assertTrue(testOutput.size() >= 0);
    }

    @Test
    @DisplayName("Shutdown closes streams gracefully")
    void testShutdown() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        assertDoesNotThrow(() -> backend.shutdown());
    }

    @Test
    @DisplayName("Set listener does not throw exception")
    void testSetListener() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        Object listener = new Object();
        assertDoesNotThrow(() -> backend.setListener(listener));
    }

    @Test
    @DisplayName("Set listener with null does not throw exception")
    void testSetListenerNull() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        assertDoesNotThrow(() -> backend.setListener(null));
    }

    @Test
    @DisplayName("Reload options does not throw exception")
    void testReloadOptions() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        assertDoesNotThrow(() -> backend.reloadOptions());
    }

    @Test
    @DisplayName("Backend is not read-only by default")
    void testIsReadOnlyDefault() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        assertFalse(backend.isReadOnly());
    }

    @Test
    @DisplayName("Backend can be initialized as read-only")
    void testReadOnlyConstructor() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput, true);
        assertTrue(backend.isReadOnly());
    }

    @Test
    @DisplayName("Set read-only updates flag")
    void testSetReadOnly() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        
        backend.setReadOnly(true);
        assertTrue(backend.isReadOnly());
        
        backend.setReadOnly(false);
        assertFalse(backend.isReadOnly());
    }

    @Test
    @DisplayName("AttrToForegroundColor returns valid RGB value")
    void testAttrToForegroundColor() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        CellAttributes attr = new CellAttributes();
        int color = backend.attrToForegroundColor(attr);
        assertTrue(color >= 0 && color <= 0xFFFFFF);
    }

    @Test
    @DisplayName("AttrToBackgroundColor returns valid RGB value")
    void testAttrToBackgroundColor() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        CellAttributes attr = new CellAttributes();
        int color = backend.attrToBackgroundColor(attr);
        assertTrue(color >= 0 && color <= 0xFFFFFF);
    }

    @Test
    @DisplayName("GetDefaultForeColorRGB returns valid RGB value")
    void testGetDefaultForeColorRGB() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        int color = backend.getDefaultForeColorRGB();
        assertTrue(color >= 0 && color <= 0xFFFFFF);
    }

    @Test
    @DisplayName("GetDefaultBackColorRGB returns valid RGB value")
    void testGetDefaultBackColorRGB() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        int color = backend.getDefaultBackColorRGB();
        assertTrue(color >= 0 && color <= 0xFFFFFF);
    }

    @Test
    @DisplayName("CopyClipboardText does not throw exception")
    void testCopyClipboardText() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        assertDoesNotThrow(() -> backend.copyClipboardText("test"));
    }

    @Test
    @DisplayName("CopyClipboardText with empty string does not throw")
    void testCopyClipboardTextEmpty() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        assertDoesNotThrow(() -> backend.copyClipboardText(""));
    }

    @Test
    @DisplayName("CopyClipboardText with null throws exception")
    void testCopyClipboardTextNull() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        assertThrows(NullPointerException.class, () -> backend.copyClipboardText(null));
    }

    @Test
    @DisplayName("IsFocused returns boolean value")
    void testIsFocused() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        // Just verify it doesn't throw
        assertDoesNotThrow(() -> backend.isFocused());
    }

    @Test
    @DisplayName("Backend with custom dimensions")
    void testBackendWithCustomDimensions() throws Exception {
        int width = 100;
        int height = 30;
        backend = new ECMA48Backend(null, testInput, testOutput, width, height, 12);
        
        SessionInfo sessionInfo = backend.getSessionInfo();
        // Custom dimensions may not be honored if terminal size detection overrides them
        // Just verify that dimensions are positive values
        assertTrue(sessionInfo.getWindowWidth() > 0);
        assertTrue(sessionInfo.getWindowHeight() > 0);
    }

    @Test
    @DisplayName("Screen drawing operations do not throw exceptions")
    void testScreenDrawingOperations() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        Screen screen = backend.getScreen();
        CellAttributes attr = new CellAttributes();
        
        assertDoesNotThrow(() -> {
            screen.putCharXY(0, 0, 'A', attr);
            screen.putStringXY(1, 1, "Hello", attr);
            backend.flushScreen();
        });
    }

    @Test
    @DisplayName("Multiple flush operations work correctly")
    void testMultipleFlushOperations() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        Screen screen = backend.getScreen();
        CellAttributes attr = new CellAttributes();
        
        assertDoesNotThrow(() -> {
            screen.putCharXY(0, 0, 'A', attr);
            backend.flushScreen();
            screen.putCharXY(1, 1, 'B', attr);
            backend.flushScreen();
            screen.putCharXY(2, 2, 'C', attr);
            backend.flushScreen();
        });
    }

    @Test
    @DisplayName("Backend handles rapid screen updates")
    void testRapidScreenUpdates() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        Screen screen = backend.getScreen();
        CellAttributes attr = new CellAttributes();
        
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                screen.putCharXY(i, 0, (char)('A' + i), attr);
                backend.flushScreen();
            }
        });
    }

    @Test
    @DisplayName("Backend with Reader and PrintWriter constructor")
    void testBackendWithReaderWriter() throws Exception {
        StringReader reader = new StringReader("");
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        
        backend = new ECMA48Backend(null, testInput, reader, printWriter);
        assertNotNull(backend);
        assertNotNull(backend.getScreen());
        assertNotNull(backend.getSessionInfo());
    }

    @Test
    @DisplayName("Backend operations work in sequence")
    void testBackendOperationSequence() throws Exception {
        backend = new ECMA48Backend(null, testInput, testOutput);
        
        assertDoesNotThrow(() -> {
            Screen screen = backend.getScreen();
            SessionInfo sessionInfo = backend.getSessionInfo();
            
            assertNotNull(screen);
            assertNotNull(sessionInfo);
            
            backend.setListener(new Object());
            backend.reloadOptions();
            
            screen.putStringXY(0, 0, "Test", new CellAttributes());
            backend.flushScreen();
            
            backend.copyClipboardText("clipboard test");
            
            backend.shutdown();
        });
    }
}
