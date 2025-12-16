/*
 * Casciian - Java Text User Interface
 *
 * Unit tests for TSessionInfo
 */
package casciian.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;
import static casciian.backend.TestUtils.*;

/**
 * Tests for TSessionInfo - validates session information management.
 * This tests the component behavior rather than implementation details.
 */
@DisplayName("TSessionInfo Tests")
class TSessionInfoTest {

    private TSessionInfo sessionInfo;

    @BeforeEach
    void setUp() {
        sessionInfo = new TSessionInfo();
    }

    @Test
    @DisplayName("Default constructor initializes with 80x24 dimensions")
    void testDefaultConstructor() {
        assertEquals(80, sessionInfo.getWindowWidth());
        assertEquals(24, sessionInfo.getWindowHeight());
    }

    @Test
    @DisplayName("Constructor with dimensions sets correct width and height")
    void testConstructorWithDimensions() {
        TSessionInfo customSession = new TSessionInfo(100, 50);
        assertEquals(100, customSession.getWindowWidth());
        assertEquals(50, customSession.getWindowHeight());
    }

    @Test
    @DisplayName("Constructor with small dimensions handles edge cases")
    void testConstructorWithSmallDimensions() {
        TSessionInfo smallSession = new TSessionInfo(1, 1);
        assertEquals(1, smallSession.getWindowWidth());
        assertEquals(1, smallSession.getWindowHeight());
    }

    @Test
    @DisplayName("Constructor with large dimensions handles edge cases")
    void testConstructorWithLargeDimensions() {
        TSessionInfo largeSession = new TSessionInfo(1000, 1000);
        assertEquals(1000, largeSession.getWindowWidth());
        assertEquals(1000, largeSession.getWindowHeight());
    }

    @Test
    @DisplayName("Start time is set to current time on creation")
    void testStartTimeIsSet() {
        long beforeCreation = System.currentTimeMillis();
        TSessionInfo newSession = new TSessionInfo();
        long afterCreation = System.currentTimeMillis();
        
        long startTime = newSession.getStartTime();
        assertTrue(startTime >= beforeCreation, "Start time should be after or equal to before creation time");
        assertTrue(startTime <= afterCreation, "Start time should be before or equal to after creation time");
    }

    @Test
    @DisplayName("Default username is empty string")
    void testDefaultUsername() {
        assertEquals("", sessionInfo.getUsername());
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
    @DisplayName("Username can be set multiple times")
    void testSetUsernameMultipleTimes() {
        sessionInfo.setUsername("user1");
        assertEquals("user1", sessionInfo.getUsername());
        sessionInfo.setUsername("user2");
        assertEquals("user2", sessionInfo.getUsername());
    }

    @Test
    @DisplayName("Default language is en_US")
    void testDefaultLanguage() {
        assertEquals("en_US", sessionInfo.getLanguage());
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
    @DisplayName("Language can be set multiple times")
    void testSetLanguageMultipleTimes() {
        sessionInfo.setLanguage("fr_FR");
        assertEquals("fr_FR", sessionInfo.getLanguage());
        sessionInfo.setLanguage("de_DE");
        assertEquals("de_DE", sessionInfo.getLanguage());
    }

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
    @DisplayName("Idle time can be set to large value")
    void testSetIdleTimeLarge() {
        sessionInfo.setIdleTime(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, sessionInfo.getIdleTime());
    }

    @Test
    @DisplayName("Idle time updates correctly across multiple sets")
    void testIdleTimeUpdatesCorrectly() {
        sessionInfo.setIdleTime(10);
        assertEquals(10, sessionInfo.getIdleTime());
        
        sessionInfo.setIdleTime(20);
        assertEquals(20, sessionInfo.getIdleTime());
        
        sessionInfo.setIdleTime(5);
        assertEquals(5, sessionInfo.getIdleTime());
    }

    @Test
    @DisplayName("Query window size does not throw exception")
    void testQueryWindowSize() {
        assertDoesNotThrow(() -> sessionInfo.queryWindowSize());
    }

    @Test
    @DisplayName("Query window size multiple times does not cause issues")
    void testQueryWindowSizeMultipleTimes() {
        assertDoesNotThrow(() -> {
            sessionInfo.queryWindowSize();
            sessionInfo.queryWindowSize();
            sessionInfo.queryWindowSize();
        });
    }

    @Test
    @DisplayName("Session info maintains state across operations")
    void testSessionInfoStateConsistency() {
        sessionInfo.setUsername("testuser");
        sessionInfo.setLanguage("en_GB");
        sessionInfo.setIdleTime(42);
        
        assertEquals("testuser", sessionInfo.getUsername());
        assertEquals("en_GB", sessionInfo.getLanguage());
        assertEquals(42, sessionInfo.getIdleTime());
        assertEquals(80, sessionInfo.getWindowWidth());
        assertEquals(24, sessionInfo.getWindowHeight());
    }

    @Test
    @DisplayName("Multiple session instances are independent")
    void testMultipleSessionsIndependent() {
        TSessionInfo session1 = new TSessionInfo(80, 24);
        TSessionInfo session2 = new TSessionInfo(100, 30);
        
        session1.setUsername("user1");
        session2.setUsername("user2");
        
        assertEquals("user1", session1.getUsername());
        assertEquals("user2", session2.getUsername());
        assertEquals(80, session1.getWindowWidth());
        assertEquals(100, session2.getWindowWidth());
    }
}
