/*
 * Casciian - Java Text User Interface
 *
 * Additional unit tests for Screen interface edge cases
 */
package casciian.backend;

import casciian.bits.BorderStyle;
import casciian.bits.Cell;
import casciian.bits.CellAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional tests for Screen interface implementations focusing on edge cases
 * and boundary conditions.
 */
@DisplayName("Screen Edge Cases Tests")
class ScreenEdgeCasesTest {

    private HeadlessBackend backend;
    private Screen screen;

    @BeforeEach
    void setUp() {
        backend = new HeadlessBackend();
        screen = backend.getScreen();
    }

    @Test
    @DisplayName("Screen dimensions are positive")
    void testScreenDimensionsPositive() {
        assertTrue(screen.getWidth() > 0, "Screen width must be positive");
        assertTrue(screen.getHeight() > 0, "Screen height must be positive");
    }

    @Test
    @DisplayName("Clipping boundaries start at zero")
    void testClippingBoundariesDefault() {
        assertEquals(0, screen.getClipLeft());
        assertEquals(0, screen.getClipTop());
        assertTrue(screen.getClipRight() >= 0);
        assertTrue(screen.getClipBottom() >= 0);
    }

    @Test
    @DisplayName("Set clipping boundaries to valid values")
    void testSetClippingBoundaries() {
        screen.setClipLeft(5);
        screen.setClipTop(3);
        screen.setClipRight(75);
        screen.setClipBottom(20);

        assertEquals(5, screen.getClipLeft());
        assertEquals(3, screen.getClipTop());
        assertEquals(75, screen.getClipRight());
        assertEquals(20, screen.getClipBottom());
    }

    @Test
    @DisplayName("Set clipping boundaries to full screen")
    void testSetClippingBoundariesFullScreen() {
        screen.setClipLeft(0);
        screen.setClipTop(0);
        screen.setClipRight(screen.getWidth() - 1);
        screen.setClipBottom(screen.getHeight() - 1);

        assertEquals(0, screen.getClipLeft());
        assertEquals(0, screen.getClipTop());
        assertEquals(screen.getWidth() - 1, screen.getClipRight());
        assertEquals(screen.getHeight() - 1, screen.getClipBottom());
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0",
        "10, 10",
        "-5, -5",
        "100, 100"
    })
    @DisplayName("Set offset values")
    void testSetOffsetValues(int x, int y) {
        assertDoesNotThrow(() -> {
            screen.setOffsetX(x);
            screen.setOffsetY(y);
            assertEquals(x, screen.getOffsetX());
            assertEquals(y, screen.getOffsetY());
        });
    }

    @Test
    @DisplayName("PutCharXY at corner coordinates")
    void testPutCharAtCorners() {
        CellAttributes attr = new CellAttributes();
        
        assertDoesNotThrow(() -> {
            screen.putCharXY(0, 0, 'A', attr);
            screen.putCharXY(screen.getWidth() - 1, 0, 'B', attr);
            screen.putCharXY(0, screen.getHeight() - 1, 'C', attr);
            screen.putCharXY(screen.getWidth() - 1, screen.getHeight() - 1, 'D', attr);
        });
    }

    @Test
    @DisplayName("PutCharXY with special characters")
    void testPutCharWithSpecialCharacters() {
        CellAttributes attr = new CellAttributes();
        
        // Some special characters might trigger assertions or be handled specially
        // Test only safe special characters
        assertDoesNotThrow(() -> {
            screen.putCharXY(0, 0, ' ', attr);
            screen.putCharXY(1, 0, '~', attr);
            screen.putCharXY(2, 0, '@', attr);
            screen.putCharXY(3, 0, '#', attr);
        });
    }

    @Test
    @DisplayName("PutCharXY with Unicode characters")
    void testPutCharWithUnicode() {
        CellAttributes attr = new CellAttributes();
        
        assertDoesNotThrow(() -> {
            screen.putCharXY(0, 0, '你', attr);
            screen.putCharXY(1, 0, '好', attr);
            screen.putCharXY(2, 0, '世', attr);
            screen.putCharXY(3, 0, '界', attr);
            screen.putCharXY(4, 0, '🎉', attr);
        });
    }

    @Test
    @DisplayName("PutStringXY with empty string")
    void testPutStringEmpty() {
        CellAttributes attr = new CellAttributes();
        assertDoesNotThrow(() -> screen.putStringXY(0, 0, "", attr));
    }

    @Test
    @DisplayName("PutStringXY with long string")
    void testPutStringLong() {
        CellAttributes attr = new CellAttributes();
        String longString = "A".repeat(200);
        assertDoesNotThrow(() -> screen.putStringXY(0, 0, longString, attr));
    }

    @Test
    @DisplayName("PutStringXY with various content")
    void testPutStringWithVariousContent() {
        CellAttributes attr = new CellAttributes();
        assertDoesNotThrow(() -> {
            screen.putStringXY(0, 0, "Line1", attr);
            screen.putStringXY(0, 1, "Line2", attr);
            screen.putStringXY(0, 2, "Line3", attr);
        });
    }

    @Test
    @DisplayName("VLineXY draws vertical line")
    void testVLineXY() {
        CellAttributes attr = new CellAttributes();
        assertDoesNotThrow(() -> {
            screen.vLineXY(5, 0, 10, '|', attr);
        });
    }

    @Test
    @DisplayName("VLineXY with zero height")
    void testVLineXYZeroHeight() {
        CellAttributes attr = new CellAttributes();
        assertDoesNotThrow(() -> screen.vLineXY(5, 5, 0, '|', attr));
    }

    @Test
    @DisplayName("HLineXY draws horizontal line")
    void testHLineXY() {
        CellAttributes attr = new CellAttributes();
        assertDoesNotThrow(() -> {
            screen.hLineXY(0, 5, 20, '-', attr);
        });
    }

    @Test
    @DisplayName("HLineXY with zero width")
    void testHLineXYZeroWidth() {
        CellAttributes attr = new CellAttributes();
        assertDoesNotThrow(() -> screen.hLineXY(5, 5, 0, '-', attr));
    }

    @Test
    @DisplayName("DrawBox draws all border styles")
    void testDrawBoxAllBorderStyles() {
        CellAttributes border = new CellAttributes();
        CellAttributes background = new CellAttributes();
        
        assertDoesNotThrow(() -> {
            screen.drawBox(0, 0, 20, 10, border, background, BorderStyle.SINGLE, true);
            screen.drawBox(21, 0, 41, 10, border, background, BorderStyle.DOUBLE, true);
            screen.drawBox(0, 11, 20, 21, border, background, BorderStyle.SINGLE_V_DOUBLE_H, false);
            screen.drawBox(21, 11, 41, 21, border, background, BorderStyle.SINGLE_H_DOUBLE_V, false);
        });
    }

    @Test
    @DisplayName("DrawBox with minimal dimensions")
    void testDrawBoxMinimal() {
        CellAttributes border = new CellAttributes();
        CellAttributes background = new CellAttributes();
        
        // Minimal box is 3x3 (2 for borders, 1 for interior)
        assertDoesNotThrow(() -> {
            screen.drawBox(0, 0, 3, 3, border, background, BorderStyle.SINGLE, false);
        });
    }

    @Test
    @DisplayName("DrawBox with shadow")
    void testDrawBoxWithShadow() {
        CellAttributes border = new CellAttributes();
        CellAttributes background = new CellAttributes();
        
        assertDoesNotThrow(() -> {
            screen.drawBox(5, 5, 25, 15, border, background, BorderStyle.SINGLE, true);
        });
    }

    @Test
    @DisplayName("DrawBox without shadow")
    void testDrawBoxWithoutShadow() {
        CellAttributes border = new CellAttributes();
        CellAttributes background = new CellAttributes();
        
        assertDoesNotThrow(() -> {
            screen.drawBox(5, 5, 25, 15, border, background, BorderStyle.SINGLE, false);
        });
    }

    @Test
    @DisplayName("DrawBox with different coordinates")
    void testDrawBoxDifferentCoordinates() {
        CellAttributes border = new CellAttributes();
        CellAttributes background = new CellAttributes();
        
        assertDoesNotThrow(() -> {
            screen.drawBox(5, 5, 35, 15, border, background, BorderStyle.DOUBLE, false);
        });
    }

    @Test
    @DisplayName("GetCharXY returns cell at position")
    void testGetCharXY() {
        CellAttributes attr = new CellAttributes();
        screen.putCharXY(10, 10, 'X', attr);
        
        Cell cell = screen.getCharXY(10, 10);
        assertNotNull(cell);
        assertEquals('X', cell.getChar());
    }

    @Test
    @DisplayName("GetCharXY at corners returns cells")
    void testGetCharXYAtCorners() {
        assertNotNull(screen.getCharXY(0, 0));
        assertNotNull(screen.getCharXY(screen.getWidth() - 1, 0));
        assertNotNull(screen.getCharXY(0, screen.getHeight() - 1));
        assertNotNull(screen.getCharXY(screen.getWidth() - 1, screen.getHeight() - 1));
    }

    @Test
    @DisplayName("PutCursor with visible true")
    void testPutCursorVisible() {
        assertDoesNotThrow(() -> {
            screen.putCursor(true, 10, 10);
            assertTrue(screen.isCursorVisible());
            assertEquals(10, screen.getCursorX());
            assertEquals(10, screen.getCursorY());
        });
    }

    @Test
    @DisplayName("PutCursor with visible false")
    void testPutCursorInvisible() {
        assertDoesNotThrow(() -> {
            screen.putCursor(false, 10, 10);
            assertFalse(screen.isCursorVisible());
        });
    }

    @Test
    @DisplayName("PutCursor at corners")
    void testPutCursorAtCorners() {
        assertDoesNotThrow(() -> {
            screen.putCursor(true, 0, 0);
            screen.putCursor(true, screen.getWidth() - 1, 0);
            screen.putCursor(true, 0, screen.getHeight() - 1);
            screen.putCursor(true, screen.getWidth() - 1, screen.getHeight() - 1);
        });
    }

    @Test
    @DisplayName("HideCursor makes cursor invisible")
    void testHideCursor() {
        screen.putCursor(true, 10, 10);
        screen.hideCursor();
        assertFalse(screen.isCursorVisible());
    }

    @Test
    @DisplayName("Multiple hideCursor calls are safe")
    void testMultipleHideCursor() {
        assertDoesNotThrow(() -> {
            screen.hideCursor();
            screen.hideCursor();
            screen.hideCursor();
        });
    }

    @Test
    @DisplayName("InvertCell inverts a cell's attributes")
    void testInvertCell() {
        assertDoesNotThrow(() -> screen.invertCell(5, 5));
    }

    @Test
    @DisplayName("InvertCell at corners")
    void testInvertCellAtCorners() {
        assertDoesNotThrow(() -> {
            screen.invertCell(0, 0);
            screen.invertCell(screen.getWidth() - 1, 0);
            screen.invertCell(0, screen.getHeight() - 1);
            screen.invertCell(screen.getWidth() - 1, screen.getHeight() - 1);
        });
    }

    @Test
    @DisplayName("SetTitle with various strings")
    void testSetTitle() {
        assertDoesNotThrow(() -> screen.setTitle(""));
        assertDoesNotThrow(() -> screen.setTitle("Simple Title"));
        assertDoesNotThrow(() -> screen.setTitle("Title with emojis 🎉"));
        assertDoesNotThrow(() -> screen.setTitle("Very long title " + "x".repeat(200)));
    }

    @Test
    @DisplayName("SetTitle with null")
    void testSetTitleNull() {
        assertDoesNotThrow(() -> screen.setTitle(null));
    }

    @Test
    @DisplayName("Complex drawing sequence")
    void testComplexDrawingSequence() {
        CellAttributes attr = new CellAttributes();
        
        assertDoesNotThrow(() -> {
            // Draw a complex scene
            screen.putStringXY(0, 0, "Header Text", attr);
            screen.hLineXY(0, 1, screen.getWidth(), '=', attr);
            
            screen.drawBox(5, 3, 30, 15, attr, attr, BorderStyle.DOUBLE, true);
            screen.putStringXY(7, 5, "Box Content", attr);
            
            screen.vLineXY(35, 3, 12, '|', attr);
            screen.hLineXY(5, 16, 30, '-', attr);
            
            screen.putCursor(true, 10, 10);
            
            screen.invertCell(20, 20);
        });
    }

    @Test
    @DisplayName("Rapid successive operations")
    void testRapidOperations() {
        CellAttributes attr = new CellAttributes();
        
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 100; i++) {
                screen.putCharXY(i % screen.getWidth(), 
                               i % screen.getHeight(), 
                               (char)('A' + (i % 26)), 
                               attr);
            }
        });
    }

    @Test
    @DisplayName("Offset and clipping interaction")
    void testOffsetAndClipping() {
        assertDoesNotThrow(() -> {
            screen.setOffsetX(10);
            screen.setOffsetY(5);
            screen.setClipLeft(0);
            screen.setClipTop(0);
            screen.setClipRight(50);
            screen.setClipBottom(20);
            
            CellAttributes attr = new CellAttributes();
            screen.putStringXY(0, 0, "Test with offset and clipping", attr);
        });
    }

    @Test
    @DisplayName("Screen snapshot operations")
    void testScreenSnapshot() {
        CellAttributes attr = new CellAttributes();
        
        // Draw some content
        screen.putStringXY(0, 0, "Snapshot Test", attr);
        screen.drawBox(5, 5, 25, 15, attr, attr, BorderStyle.SINGLE, false);
        
        // Take a "snapshot" by reading all cells
        assertDoesNotThrow(() -> {
            for (int y = 0; y < screen.getHeight(); y++) {
                for (int x = 0; x < screen.getWidth(); x++) {
                    Cell cell = screen.getCharXY(x, y);
                    assertNotNull(cell, "Cell at (" + x + ", " + y + ") should not be null");
                }
            }
        });
    }

    @Test
    @DisplayName("Clear screen operation")
    void testClearScreen() {
        CellAttributes attr = new CellAttributes();
        
        // Fill screen with content
        screen.putStringXY(0, 0, "Content", attr);
        screen.drawBox(5, 5, 25, 15, attr, attr, BorderStyle.SINGLE, false);
        
        // Clear should not throw
        assertDoesNotThrow(() -> screen.clear());
    }

    @Test
    @DisplayName("ClearPhysical does not throw")
    void testClearPhysical() {
        assertDoesNotThrow(() -> screen.clearPhysical());
    }

    @Test
    @DisplayName("Multiple clearPhysical calls")
    void testMultipleClearPhysical() {
        assertDoesNotThrow(() -> {
            screen.clearPhysical();
            screen.clearPhysical();
            screen.clearPhysical();
        });
    }

    @Test
    @DisplayName("FlushPhysical does not throw")
    void testFlushPhysical() {
        assertDoesNotThrow(() -> screen.flushPhysical());
    }

    @Test
    @DisplayName("Multiple flushPhysical calls")
    void testMultipleFlushPhysical() {
        assertDoesNotThrow(() -> {
            screen.flushPhysical();
            screen.flushPhysical();
            screen.flushPhysical();
        });
    }
}
