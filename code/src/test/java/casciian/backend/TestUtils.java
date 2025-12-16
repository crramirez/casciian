/*
 * Casciian - Java Text User Interface
 *
 * Test utilities for backend package tests
 */
package casciian.backend;

import casciian.bits.Cell;
import casciian.bits.CellAttributes;
import casciian.bits.ComplexCell;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Utility methods for backend tests to reduce code duplication.
 */
public class TestUtils {

    /**
     * Create a basic CellAttributes for testing.
     *
     * @return a default CellAttributes instance
     */
    public static CellAttributes createDefaultCellAttributes() {
        return new CellAttributes();
    }

    /**
     * Create a Cell with default attributes for testing.
     *
     * @param ch the character to put in the cell
     * @return a Cell instance
     */
    public static Cell createCell(char ch) {
        return new Cell(ch);
    }

    /**
     * Assert that two SessionInfo instances have the same state.
     *
     * @param expected the expected session info
     * @param actual the actual session info
     */
    public static void assertSessionInfoEquals(SessionInfo expected, SessionInfo actual) {
        assertEquals(expected.getUsername(), actual.getUsername(), "Username should match");
        assertEquals(expected.getLanguage(), actual.getLanguage(), "Language should match");
        assertEquals(expected.getWindowWidth(), actual.getWindowWidth(), "Window width should match");
        assertEquals(expected.getWindowHeight(), actual.getWindowHeight(), "Window height should match");
    }

    /**
     * Assert that a screen has the expected dimensions.
     *
     * @param screen the screen to check
     * @param expectedWidth the expected width
     * @param expectedHeight the expected height
     */
    public static void assertScreenDimensions(Screen screen, int expectedWidth, int expectedHeight) {
        assertEquals(expectedWidth, screen.getWidth(), "Screen width should match");
        assertEquals(expectedHeight, screen.getHeight(), "Screen height should match");
    }

    /**
     * Assert that a value is within a reasonable range of an expected value.
     * Useful for time-based assertions.
     *
     * @param expected the expected value
     * @param actual the actual value
     * @param tolerance the acceptable difference
     * @param message the message to display on failure
     */
    public static void assertWithinRange(long expected, long actual, long tolerance, String message) {
        long diff = Math.abs(expected - actual);
        assertTrue(diff <= tolerance, message + " (expected: " + expected + ", actual: " + actual + ", tolerance: " + tolerance + ")");
    }
}
