/*
 * Casciian - Java Text User Interface
 *
 * Unit tests for GenericBackend
 */
package casciian.backend;

import casciian.bits.CellAttributes;
import casciian.event.TInputEvent;
import casciian.event.TCommandEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static casciian.TCommand.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for GenericBackend - validates the abstract backend base class
 * that provides common functionality for backend implementations.
 */
@DisplayName("GenericBackend Tests")
class GenericBackendTest {

    private TestableGenericBackend backend;
    private TerminalReader mockTerminal;
    private Screen mockScreen;
    private SessionInfo mockSessionInfo;

    /**
     * Concrete implementation of GenericBackend for testing purposes.
     */
    private static class TestableGenericBackend extends GenericBackend {
        
        public TestableGenericBackend(TerminalReader terminal, Screen screen, SessionInfo sessionInfo) {
            this.terminal = terminal;
            this.screen = screen;
            this.sessionInfo = sessionInfo;
        }

        @Override
        public int attrToForegroundColor(CellAttributes attr) {
            return 0xFFFFFF;
        }

        @Override
        public int attrToBackgroundColor(CellAttributes attr) {
            return 0x000000;
        }

        @Override
        public void copyClipboardText(String text) {
            // No-op for testing
        }

        @Override
        public boolean isFocused() {
            return false;
        }

        @Override
        public int getDefaultForeColorRGB() {
            return 0xFFFFFF;
        }

        @Override
        public int getDefaultBackColorRGB() {
            return 0x000000;
        }
    }

    @BeforeEach
    void setUp() {
        mockTerminal = Mockito.mock(TerminalReader.class);
        mockScreen = Mockito.mock(Screen.class);
        mockSessionInfo = Mockito.mock(SessionInfo.class);
        
        backend = new TestableGenericBackend(mockTerminal, mockScreen, mockSessionInfo);
    }

    @Test
    @DisplayName("Backend initializes with provided components")
    void testBackendInitialization() {
        assertNotNull(backend);
        assertSame(mockScreen, backend.getScreen());
        assertSame(mockSessionInfo, backend.getSessionInfo());
    }

    @Test
    @DisplayName("GetSessionInfo returns the session info")
    void testGetSessionInfo() {
        SessionInfo sessionInfo = backend.getSessionInfo();
        assertNotNull(sessionInfo);
        assertSame(mockSessionInfo, sessionInfo);
    }

    @Test
    @DisplayName("GetScreen returns the screen")
    void testGetScreen() {
        Screen screen = backend.getScreen();
        assertNotNull(screen);
        assertSame(mockScreen, screen);
    }

    @Test
    @DisplayName("FlushScreen delegates to screen flushPhysical")
    void testFlushScreen() {
        backend.flushScreen();
        verify(mockScreen, times(1)).flushPhysical();
    }

    @Test
    @DisplayName("HasEvents returns false when terminal has no events")
    void testHasEventsWhenNoEvents() {
        when(mockTerminal.hasEvents()).thenReturn(false);
        
        assertFalse(backend.hasEvents());
        verify(mockTerminal, times(1)).hasEvents();
        verify(mockSessionInfo, times(1)).setIdleTime(anyInt());
    }

    @Test
    @DisplayName("HasEvents returns true when terminal has events")
    void testHasEventsWhenEventsPresent() {
        when(mockTerminal.hasEvents()).thenReturn(true);
        
        assertTrue(backend.hasEvents());
        verify(mockTerminal, times(1)).hasEvents();
        verify(mockSessionInfo, never()).setIdleTime(anyInt());
    }

    @Test
    @DisplayName("GetEvents retrieves events from terminal")
    void testGetEvents() {
        List<TInputEvent> queue = new ArrayList<>();
        when(mockTerminal.hasEvents()).thenReturn(true);
        
        backend.getEvents(queue);
        
        verify(mockTerminal, times(1)).hasEvents();
        verify(mockTerminal, times(1)).getEvents(queue);
        verify(mockSessionInfo, times(1)).setIdleTime(anyInt());
    }

    @Test
    @DisplayName("GetEvents does nothing when terminal has no events")
    void testGetEventsWhenNoEvents() {
        List<TInputEvent> queue = new ArrayList<>();
        when(mockTerminal.hasEvents()).thenReturn(false);
        
        backend.getEvents(queue);
        
        verify(mockTerminal, times(1)).hasEvents();
        verify(mockTerminal, never()).getEvents(any());
    }

    @Test
    @DisplayName("GetEvents clears queue when read-only and no disconnect event")
    void testGetEventsReadOnlyMode() {
        backend.setReadOnly(true);
        
        List<TInputEvent> queue = new ArrayList<>();
        TInputEvent mockEvent = Mockito.mock(TInputEvent.class);
        queue.add(mockEvent);
        
        when(mockTerminal.hasEvents()).thenReturn(true);
        doAnswer(invocation -> {
            // Terminal doesn't modify the queue in this test
            return null;
        }).when(mockTerminal).getEvents(any());
        
        backend.getEvents(queue);
        
        assertTrue(queue.isEmpty(), "Queue should be cleared in read-only mode");
    }

    @Test
    @DisplayName("GetEvents adds cmAbort after cmBackendDisconnect when abortOnDisconnect is true")
    void testGetEventsAddsAbortOnDisconnect() {
        backend.abortOnDisconnect = true;
        
        List<TInputEvent> queue = new ArrayList<>();
        TCommandEvent disconnectEvent = new TCommandEvent(backend, cmBackendDisconnect);
        
        when(mockTerminal.hasEvents()).thenReturn(true);
        doAnswer(invocation -> {
            List<TInputEvent> q = invocation.getArgument(0);
            q.add(disconnectEvent);
            return null;
        }).when(mockTerminal).getEvents(any());
        
        backend.getEvents(queue);
        
        assertEquals(2, queue.size(), "Should have disconnect and abort events");
        assertEquals(cmBackendDisconnect, ((TCommandEvent)queue.get(0)).getCmd());
        assertEquals(cmAbort, ((TCommandEvent)queue.get(1)).getCmd());
    }

    @Test
    @DisplayName("GetEvents does not add cmAbort when abortOnDisconnect is false")
    void testGetEventsNoAbortWhenFlagIsFalse() {
        backend.abortOnDisconnect = false;
        
        List<TInputEvent> queue = new ArrayList<>();
        TCommandEvent disconnectEvent = new TCommandEvent(backend, cmBackendDisconnect);
        
        when(mockTerminal.hasEvents()).thenReturn(true);
        doAnswer(invocation -> {
            List<TInputEvent> q = invocation.getArgument(0);
            q.add(disconnectEvent);
            return null;
        }).when(mockTerminal).getEvents(any());
        
        backend.getEvents(queue);
        
        assertEquals(1, queue.size(), "Should only have disconnect event");
        assertEquals(cmBackendDisconnect, ((TCommandEvent)queue.get(0)).getCmd());
    }

    @Test
    @DisplayName("Shutdown closes the terminal")
    void testShutdown() {
        backend.shutdown();
        verify(mockTerminal, times(1)).closeTerminal();
    }

    @Test
    @DisplayName("SetTitle delegates to screen")
    void testSetTitle() {
        String title = "Test Title";
        backend.setTitle(title);
        verify(mockScreen, times(1)).setTitle(title);
    }

    @Test
    @DisplayName("SetListener delegates to terminal")
    void testSetListener() {
        Object listener = new Object();
        backend.setListener(listener);
        verify(mockTerminal, times(1)).setListener(listener);
    }

    @Test
    @DisplayName("ReloadOptions delegates to terminal")
    void testReloadOptions() {
        backend.reloadOptions();
        verify(mockTerminal, times(1)).reloadOptions();
    }

    @Test
    @DisplayName("Backend is not read-only by default")
    void testIsReadOnlyDefault() {
        assertFalse(backend.isReadOnly());
    }

    @Test
    @DisplayName("SetReadOnly updates read-only flag")
    void testSetReadOnly() {
        assertFalse(backend.isReadOnly());
        
        backend.setReadOnly(true);
        assertTrue(backend.isReadOnly());
        
        backend.setReadOnly(false);
        assertFalse(backend.isReadOnly());
    }

    @Test
    @DisplayName("AttrToForegroundColor returns valid RGB value")
    void testAttrToForegroundColor() {
        CellAttributes attr = new CellAttributes();
        int color = backend.attrToForegroundColor(attr);
        assertTrue(color >= 0 && color <= 0xFFFFFF);
    }

    @Test
    @DisplayName("AttrToBackgroundColor returns valid RGB value")
    void testAttrToBackgroundColor() {
        CellAttributes attr = new CellAttributes();
        int color = backend.attrToBackgroundColor(attr);
        assertTrue(color >= 0 && color <= 0xFFFFFF);
    }

    @Test
    @DisplayName("GetDefaultForeColorRGB returns valid RGB value")
    void testGetDefaultForeColorRGB() {
        int color = backend.getDefaultForeColorRGB();
        assertTrue(color >= 0 && color <= 0xFFFFFF);
    }

    @Test
    @DisplayName("GetDefaultBackColorRGB returns valid RGB value")
    void testGetDefaultBackColorRGB() {
        int color = backend.getDefaultBackColorRGB();
        assertTrue(color >= 0 && color <= 0xFFFFFF);
    }

    @Test
    @DisplayName("CopyClipboardText does not throw exception")
    void testCopyClipboardText() {
        assertDoesNotThrow(() -> backend.copyClipboardText("test"));
        assertDoesNotThrow(() -> backend.copyClipboardText(""));
        assertDoesNotThrow(() -> backend.copyClipboardText(null));
    }

    @Test
    @DisplayName("IsFocused returns false by default")
    void testIsFocused() {
        assertFalse(backend.isFocused());
    }

    @Test
    @DisplayName("LastUserInputTime is initialized")
    void testLastUserInputTimeInitialized() {
        // Access through behavior: hasEvents should update idle time
        when(mockTerminal.hasEvents()).thenReturn(false);
        backend.hasEvents();
        
        // Verify that setIdleTime was called with a non-negative value
        verify(mockSessionInfo, times(1)).setIdleTime(intThat(time -> time >= 0));
    }

    @Test
    @DisplayName("Multiple getEvents calls update idle time")
    void testIdleTimeUpdates() {
        when(mockTerminal.hasEvents()).thenReturn(false);
        
        backend.hasEvents();
        backend.hasEvents();
        
        verify(mockSessionInfo, atLeast(2)).setIdleTime(anyInt());
    }

    @Test
    @DisplayName("GetEvents updates lastUserInputTime when events are present")
    void testLastUserInputTimeUpdatedOnEvents() {
        List<TInputEvent> queue = new ArrayList<>();
        TInputEvent mockEvent = Mockito.mock(TInputEvent.class);
        
        when(mockTerminal.hasEvents()).thenReturn(true);
        doAnswer(invocation -> {
            List<TInputEvent> q = invocation.getArgument(0);
            q.add(mockEvent);
            return null;
        }).when(mockTerminal).getEvents(any());
        
        backend.getEvents(queue);
        
        // After getting events, idle time should be set to a very small value (near 0)
        verify(mockSessionInfo, times(1)).setIdleTime(intThat(time -> time >= 0 && time < 2));
    }
}
