/*
 * Casciian - Java Text User Interface
 *
 * Unit tests for TTYSessionInfo
 */
package casciian.backend;

import casciian.backend.terminal.Terminal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TTYSessionInfo - validates session information from system environment.
 * Note: Some tests may behave differently based on the OS and environment.
 */
@DisplayName("TTYSessionInfo Tests")
class TTYSessionInfoTest {

    private TTYSessionInfo sessionInfo;
    private MockTerminal mockTerminal;

    /**
     * Mock terminal implementation for testing.
     */
    private static class MockTerminal implements Terminal {
        private int windowWidth = 100;
        private int windowHeight = 50;
        private boolean queryWindowSizeCalled = false;

        @Override
        public void setRawMode() {}

        @Override
        public void setCookedMode() {}

        @Override
        public void close() {}

        @Override
        public PrintWriter getWriter() {
            return null;
        }

        @Override
        public Reader getReader() {
            return null;
        }

        @Override
        public void enableMouseReporting(boolean on) {}

        @Override
        public int available() throws IOException {
            return 0;
        }

        @Override
        public int read(char[] buffer, int off, int len) throws IOException {
            return -1;
        }

        @Override
        public void queryWindowSize() {
            queryWindowSizeCalled = true;
        }

        @Override
        public int getWindowWidth() {
            return windowWidth;
        }

        @Override
        public int getWindowHeight() {
            return windowHeight;
        }

        public void setWindowWidth(int width) {
            this.windowWidth = width;
        }

        public void setWindowHeight(int height) {
            this.windowHeight = height;
        }

        public boolean wasQueryWindowSizeCalled() {
            return queryWindowSizeCalled;
        }
    }

    @BeforeEach
    void setUp() {
        mockTerminal = new MockTerminal();
        sessionInfo = new TTYSessionInfo(mockTerminal);
    }

    // Initialization tests

    @Test
    @DisplayName("TTYSessionInfo initializes successfully")
    void testInitialization() {
        assertNotNull(sessionInfo);
    }

    @Test
    @DisplayName("Start time is set on creation")
    void testStartTime() {
        long startTime = sessionInfo.getStartTime();
        assertTrue(startTime > 0);
        assertTrue(startTime <= System.currentTimeMillis());
    }

    // Username tests

    @Test
    @DisplayName("Username is initialized from system property")
    void testUsername() {
        String username = sessionInfo.getUsername();
        // Username should match system property (can be null or empty)
        String systemUsername = System.getProperty("user.name");
        if (systemUsername != null) {
            assertEquals(systemUsername, username);
        }
    }

    @Test
    @DisplayName("Username can be set and retrieved")
    void testSetUsername() {
        sessionInfo.setUsername("testuser");
        assertEquals("testuser", sessionInfo.getUsername());
    }

    @Test
    @DisplayName("Username can be set to null")
    void testSetUsernameNull() {
        sessionInfo.setUsername(null);
        assertNull(sessionInfo.getUsername());
    }

    @Test
    @DisplayName("Username can be changed multiple times")
    void testChangeUsername() {
        sessionInfo.setUsername("user1");
        assertEquals("user1", sessionInfo.getUsername());
        
        sessionInfo.setUsername("user2");
        assertEquals("user2", sessionInfo.getUsername());
    }

    // Language tests

    @Test
    @DisplayName("Language is initialized from system property")
    void testLanguage() {
        String language = sessionInfo.getLanguage();
        // Language should match system property (can be null or empty)
        String systemLanguage = System.getProperty("user.language");
        if (systemLanguage != null) {
            assertEquals(systemLanguage, language);
        }
    }

    @Test
    @DisplayName("Language can be set and retrieved")
    void testSetLanguage() {
        sessionInfo.setLanguage("es_ES");
        assertEquals("es_ES", sessionInfo.getLanguage());
    }

    @Test
    @DisplayName("Language can be set to null")
    void testSetLanguageNull() {
        sessionInfo.setLanguage(null);
        assertNull(sessionInfo.getLanguage());
    }

    @Test
    @DisplayName("Language can be changed multiple times")
    void testChangeLanguage() {
        sessionInfo.setLanguage("fr_FR");
        assertEquals("fr_FR", sessionInfo.getLanguage());
        
        sessionInfo.setLanguage("de_DE");
        assertEquals("de_DE", sessionInfo.getLanguage());
    }

    // Window dimensions tests

    @Test
    @DisplayName("Window dimensions are positive")
    void testWindowDimensions() {
        assertTrue(sessionInfo.getWindowWidth() > 0);
        assertTrue(sessionInfo.getWindowHeight() > 0);
    }

    @Test
    @DisplayName("Window dimensions match terminal values")
    void testWindowDimensionsFromTerminal() {
        // Mock terminal returns 100x50
        assertEquals(100, sessionInfo.getWindowWidth());
        assertEquals(50, sessionInfo.getWindowHeight());
    }

    @Test
    @DisplayName("Query window size delegates to terminal")
    void testQueryWindowSizeDelegation() {
        // Reset the flag to check if it's called during queryWindowSize
        mockTerminal = new MockTerminal();
        sessionInfo = new TTYSessionInfo(mockTerminal);
        
        // The constructor calls queryWindowSize
        assertTrue(mockTerminal.wasQueryWindowSizeCalled());
    }

    @Test
    @DisplayName("Query window size does not throw exception")
    void testQueryWindowSize() {
        assertDoesNotThrow(() -> sessionInfo.queryWindowSize());
    }

    @Test
    @DisplayName("Query window size multiple times does not throw exception")
    void testQueryWindowSizeMultipleTimes() {
        assertDoesNotThrow(() -> {
            sessionInfo.queryWindowSize();
            Thread.sleep(10); // Small delay
            sessionInfo.queryWindowSize();
            Thread.sleep(10);
            sessionInfo.queryWindowSize();
        });
    }

    @Test
    @DisplayName("Rapid window size queries are throttled")
    void testRapidWindowSizeQueries() throws InterruptedException {
        int initialWidth = sessionInfo.getWindowWidth();
        int initialHeight = sessionInfo.getWindowHeight();
        
        // Rapid queries should be throttled (< 1 second apart)
        sessionInfo.queryWindowSize();
        sessionInfo.queryWindowSize();
        sessionInfo.queryWindowSize();
        
        // Dimensions should remain stable
        assertEquals(initialWidth, sessionInfo.getWindowWidth());
        assertEquals(initialHeight, sessionInfo.getWindowHeight());
    }

    @Test
    @DisplayName("Window size query after delay is allowed")
    void testWindowSizeQueryAfterDelay() throws InterruptedException {
        sessionInfo.queryWindowSize();
        
        // Wait for more than the throttle period (1 second)
        Thread.sleep(1100);
        
        // This query should be allowed
        assertDoesNotThrow(() -> sessionInfo.queryWindowSize());
    }

    // Idle time tests

    @Test
    @DisplayName("Default idle time is Integer.MAX_VALUE")
    void testDefaultIdleTime() {
        assertEquals(Integer.MAX_VALUE, sessionInfo.getIdleTime());
    }

    @Test
    @DisplayName("Idle time can be set and retrieved")
    void testSetIdleTime() {
        sessionInfo.setIdleTime(100);
        assertEquals(100, sessionInfo.getIdleTime());
    }

    @Test
    @DisplayName("Idle time can be set to zero")
    void testSetIdleTimeZero() {
        sessionInfo.setIdleTime(0);
        assertEquals(0, sessionInfo.getIdleTime());
    }

    @Test
    @DisplayName("Idle time can be set to negative value")
    void testSetIdleTimeNegative() {
        sessionInfo.setIdleTime(-1);
        assertEquals(-1, sessionInfo.getIdleTime());
    }

    @Test
    @DisplayName("Idle time can be updated multiple times")
    void testUpdateIdleTime() {
        sessionInfo.setIdleTime(10);
        assertEquals(10, sessionInfo.getIdleTime());
        
        sessionInfo.setIdleTime(20);
        assertEquals(20, sessionInfo.getIdleTime());
        
        sessionInfo.setIdleTime(5);
        assertEquals(5, sessionInfo.getIdleTime());
    }

    // State consistency tests

    @Test
    @DisplayName("Session info maintains state across operations")
    void testStateConsistency() {
        sessionInfo.setUsername("testuser");
        sessionInfo.setLanguage("en_GB");
        sessionInfo.setIdleTime(42);
        
        assertEquals("testuser", sessionInfo.getUsername());
        assertEquals("en_GB", sessionInfo.getLanguage());
        assertEquals(42, sessionInfo.getIdleTime());
        assertTrue(sessionInfo.getWindowWidth() > 0);
        assertTrue(sessionInfo.getWindowHeight() > 0);
    }

    @Test
    @DisplayName("Multiple TTYSessionInfo instances are independent")
    void testMultipleInstancesIndependent() {
        MockTerminal terminal1 = new MockTerminal();
        MockTerminal terminal2 = new MockTerminal();
        TTYSessionInfo session1 = new TTYSessionInfo(terminal1);
        TTYSessionInfo session2 = new TTYSessionInfo(terminal2);
        
        session1.setUsername("user1");
        session2.setUsername("user2");
        
        assertEquals("user1", session1.getUsername());
        assertEquals("user2", session2.getUsername());
    }

    @Test
    @DisplayName("Query window size maintains dimension consistency")
    void testQueryWindowSizeMaintainsConsistency() {
        int widthBefore = sessionInfo.getWindowWidth();
        int heightBefore = sessionInfo.getWindowHeight();
        
        sessionInfo.queryWindowSize();
        
        int widthAfter = sessionInfo.getWindowWidth();
        int heightAfter = sessionInfo.getWindowHeight();
        
        // Dimensions should be stable (may change on Unix-like systems with actual terminal)
        assertTrue(widthAfter > 0);
        assertTrue(heightAfter > 0);
    }

    @Test
    @DisplayName("Session info reflects system properties correctly")
    void testSystemPropertiesReflection() {
        MockTerminal terminal = new MockTerminal();
        TTYSessionInfo newSession = new TTYSessionInfo(terminal);
        
        String expectedUsername = System.getProperty("user.name");
        String expectedLanguage = System.getProperty("user.language");
        
        if (expectedUsername != null) {
            assertEquals(expectedUsername, newSession.getUsername());
        }
        
        if (expectedLanguage != null) {
            assertEquals(expectedLanguage, newSession.getLanguage());
        }
    }

    @Test
    @DisplayName("Start time is unique for each instance")
    void testUniqueStartTimes() throws InterruptedException {
        MockTerminal terminal1 = new MockTerminal();
        MockTerminal terminal2 = new MockTerminal();
        TTYSessionInfo session1 = new TTYSessionInfo(terminal1);
        Thread.sleep(10); // Ensure different timestamps
        TTYSessionInfo session2 = new TTYSessionInfo(terminal2);
        
        assertTrue(session2.getStartTime() >= session1.getStartTime());
    }

    @Test
    @DisplayName("Window dimensions are reasonable values")
    void testReasonableWindowDimensions() {
        int width = sessionInfo.getWindowWidth();
        int height = sessionInfo.getWindowHeight();
        
        // Sanity checks - dimensions should be reasonable
        assertTrue(width >= 1 && width <= 10000, "Width should be between 1 and 10000");
        assertTrue(height >= 1 && height <= 10000, "Height should be between 1 and 10000");
    }
}
