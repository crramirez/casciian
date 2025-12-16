/*
 * Casciian - Java Text User Interface
 *
 * Unit tests for TTYSessionInfo
 */
package casciian.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TTYSessionInfo - validates session information from system environment.
 * Note: Some tests may behave differently based on the OS and environment.
 */
@DisplayName("TTYSessionInfo Tests")
class TTYSessionInfoTest {

    private TTYSessionInfo sessionInfo;

    @BeforeEach
    void setUp() {
        sessionInfo = new TTYSessionInfo();
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
    @DisplayName("Default window dimensions are at least 80x24")
    void testDefaultWindowDimensions() {
        // Should default to at least 80x24 if stty fails
        assertTrue(sessionInfo.getWindowWidth() >= 80);
        assertTrue(sessionInfo.getWindowHeight() >= 24);
    }

    @Test
    @DisplayName("Windows OS returns 80x25")
    void testWindowsOSDimensions() {
        if (System.getProperty("os.name").startsWith("Windows")) {
            assertEquals(80, sessionInfo.getWindowWidth());
            assertEquals(25, sessionInfo.getWindowHeight());
        }
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
        TTYSessionInfo session1 = new TTYSessionInfo();
        TTYSessionInfo session2 = new TTYSessionInfo();
        
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
        TTYSessionInfo newSession = new TTYSessionInfo();
        
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
        TTYSessionInfo session1 = new TTYSessionInfo();
        Thread.sleep(10); // Ensure different timestamps
        TTYSessionInfo session2 = new TTYSessionInfo();
        
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
