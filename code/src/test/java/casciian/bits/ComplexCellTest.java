/*
 * Casciian - Java Text User Interface
 *
 * Unit tests for ComplexCell
 */
package casciian.bits;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ComplexCell - validates complex cell functionality.
 */
@DisplayName("ComplexCell Tests")
class ComplexCellTest {

    @Test
    @DisplayName("add() should append a codepoint to the existing codepoints")
    void testAddCodePointAppendsToExisting() {
        // Create a ComplexCell with initial codepoint 'A' (65)
        ComplexCell cell = new ComplexCell('A');
        
        // Add another codepoint 'B' (66)
        cell.add('B');
        
        // Verify the codepoints
        int[] codePoints = cell.getCodePoints();
        assertEquals(2, codePoints.length, "Should have 2 codepoints");
        assertEquals('A', codePoints[0], "First codepoint should be 'A'");
        assertEquals('B', codePoints[1], "Second codepoint should be 'B'");
    }

    @Test
    @DisplayName("add() should preserve original codepoints when adding")
    void testAddCodePointPreservesOriginal() {
        // Create a ComplexCell with initial codepoints
        int[] initial = new int[] {'H', 'e', 'l', 'l', 'o'};
        ComplexCell cell = new ComplexCell(initial);
        
        // Add another codepoint
        cell.add('!');
        
        // Verify all codepoints are present
        int[] codePoints = cell.getCodePoints();
        assertEquals(6, codePoints.length, "Should have 6 codepoints");
        assertEquals('H', codePoints[0]);
        assertEquals('e', codePoints[1]);
        assertEquals('l', codePoints[2]);
        assertEquals('l', codePoints[3]);
        assertEquals('o', codePoints[4]);
        assertEquals('!', codePoints[5]);
    }

    @Test
    @DisplayName("add() should work with emoji codepoints")
    void testAddCodePointWithEmoji() {
        // Create a ComplexCell with a base emoji
        int baseEmoji = 0x1F600; // 😀
        ComplexCell cell = new ComplexCell(baseEmoji);
        
        // Add a variation selector
        int variationSelector = 0xFE0F;
        cell.add(variationSelector);
        
        // Verify both codepoints are present
        int[] codePoints = cell.getCodePoints();
        assertEquals(2, codePoints.length, "Should have 2 codepoints");
        assertEquals(baseEmoji, codePoints[0], "First codepoint should be the emoji");
        assertEquals(variationSelector, codePoints[1], "Second codepoint should be variation selector");
    }

    @Test
    @DisplayName("add() should work multiple times")
    void testAddCodePointMultipleTimes() {
        ComplexCell cell = new ComplexCell('A');
        
        cell.add('B');
        cell.add('C');
        cell.add('D');
        
        int[] codePoints = cell.getCodePoints();
        assertEquals(4, codePoints.length, "Should have 4 codepoints");
        assertEquals('A', codePoints[0]);
        assertEquals('B', codePoints[1]);
        assertEquals('C', codePoints[2]);
        assertEquals('D', codePoints[3]);
    }

    @Test
    @DisplayName("Constructor with single codepoint initializes correctly")
    void testConstructorWithSingleCodepoint() {
        ComplexCell cell = new ComplexCell('X');
        
        int[] codePoints = cell.getCodePoints();
        assertEquals(1, codePoints.length);
        assertEquals('X', codePoints[0]);
        assertEquals('X', cell.getChar());
    }

    @Test
    @DisplayName("Constructor with codepoint array initializes correctly")
    void testConstructorWithCodepointArray() {
        int[] input = new int[] {'A', 'B', 'C'};
        ComplexCell cell = new ComplexCell(input);
        
        int[] codePoints = cell.getCodePoints();
        assertEquals(3, codePoints.length);
        assertEquals('A', codePoints[0]);
        assertEquals('B', codePoints[1]);
        assertEquals('C', codePoints[2]);
    }

    @Test
    @DisplayName("setChar() replaces all codepoints with single codepoint")
    void testSetCharReplacesAllCodepoints() {
        int[] input = new int[] {'A', 'B', 'C'};
        ComplexCell cell = new ComplexCell(input);
        
        cell.setChar('Z');
        
        int[] codePoints = cell.getCodePoints();
        assertEquals(1, codePoints.length, "Should have only 1 codepoint after setChar");
        assertEquals('Z', codePoints[0]);
    }

    @Test
    @DisplayName("getCodePointCount() returns correct count")
    void testGetCodePointCount() {
        int[] input = new int[] {'A', 'B', 'C', 'D'};
        ComplexCell cell = new ComplexCell(input);
        
        assertEquals(4, cell.getCodePointCount());
    }

    @Test
    @DisplayName("toString() returns correct string representation")
    void testToString() {
        int[] input = new int[] {'H', 'i', '!'};
        ComplexCell cell = new ComplexCell(input);
        
        assertEquals("Hi!", cell.toString());
    }

    @Test
    @DisplayName("equals() returns true for identical cells")
    void testEqualsIdenticalCells() {
        int[] input = new int[] {'A', 'B'};
        ComplexCell cell1 = new ComplexCell(input);
        ComplexCell cell2 = new ComplexCell(input);
        
        assertEquals(cell1, cell2);
    }

    @Test
    @DisplayName("equals() returns false for different codepoints")
    void testEqualsDifferentCodepoints() {
        ComplexCell cell1 = new ComplexCell(new int[] {'A', 'B'});
        ComplexCell cell2 = new ComplexCell(new int[] {'A', 'C'});
        
        assertNotEquals(cell1, cell2);
    }

    @Test
    @DisplayName("reset() resets to blank space")
    void testReset() {
        int[] input = new int[] {'H', 'e', 'l', 'l', 'o'};
        ComplexCell cell = new ComplexCell(input);
        
        cell.reset();
        
        int[] codePoints = cell.getCodePoints();
        assertEquals(1, codePoints.length);
        assertEquals(' ', codePoints[0]);
    }
}
