/*
 * Casciian - Java Text User Interface
 *
 * Unit tests for LogicalScreen
 */
package casciian.backend;

import casciian.bits.BorderStyle;
import casciian.bits.Cell;
import casciian.bits.CellAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;
import static casciian.backend.TestUtils.*;

/**
 * Tests for LogicalScreen - validates screen operations including drawing,
 * clipping, dimensions, and cell manipulation.
 */
@DisplayName("LogicalScreen Tests")
class LogicalScreenTest {

    private static class TestableLogicalScreen extends LogicalScreen {
        public TestableLogicalScreen() {
            super();
        }

        public TestableLogicalScreen(int width, int height) {
            super(width, height);
        }

        @Override
        public void flushPhysical() {
            // Test implementation - does nothing
        }
    }

    private TestableLogicalScreen screen;
    private CellAttributes defaultAttr;

    @BeforeEach
    void setUp() {
        screen = new TestableLogicalScreen();
        defaultAttr = createDefaultCellAttributes();
    }

    // Dimension tests

    @Test
    @DisplayName("Default screen dimensions are 80x24")
    void testDefaultDimensions() {
        assertEquals(80, screen.getWidth());
        assertEquals(24, screen.getHeight());
    }

    @Test
    @DisplayName("Custom dimensions are set correctly")
    void testCustomDimensions() {
        TestableLogicalScreen customScreen = new TestableLogicalScreen(100, 50);
        assertEquals(100, customScreen.getWidth());
        assertEquals(50, customScreen.getHeight());
    }

    @Test
    @DisplayName("Set width changes screen width")
    void testSetWidth() {
        screen.setWidth(120);
        assertEquals(120, screen.getWidth());
    }

    @Test
    @DisplayName("Set height changes screen height")
    void testSetHeight() {
        screen.setHeight(40);
        assertEquals(40, screen.getHeight());
    }

    @Test
    @DisplayName("Set dimensions changes both width and height")
    void testSetDimensions() {
        screen.setDimensions(100, 30);
        assertEquals(100, screen.getWidth());
        assertEquals(30, screen.getHeight());
    }

    @Test
    @DisplayName("Set dimensions to 1x1 works correctly")
    void testSetDimensionsMinimum() {
        screen.setDimensions(1, 1);
        assertEquals(1, screen.getWidth());
        assertEquals(1, screen.getHeight());
    }

    @Test
    @DisplayName("Set dimensions to large values works correctly")
    void testSetDimensionsLarge() {
        screen.setDimensions(500, 500);
        assertEquals(500, screen.getWidth());
        assertEquals(500, screen.getHeight());
    }

    // Drawing tests

    @Test
    @DisplayName("Put char at valid position does not throw exception")
    void testPutCharXY() {
        assertDoesNotThrow(() -> screen.putCharXY(0, 0, 'A', defaultAttr));
    }

    @Test
    @DisplayName("Put string at valid position does not throw exception")
    void testPutStringXY() {
        assertDoesNotThrow(() -> screen.putStringXY(0, 0, "Hello", defaultAttr));
    }

    @Test
    @DisplayName("Put string with different colors does not throw exception")
    void testPutStringWithColors() {
        String text = "Colored Text";
        assertDoesNotThrow(() -> screen.putStringXY(5, 5, text, defaultAttr));
    }

    @Test
    @DisplayName("Put empty string does not throw exception")
    void testPutEmptyString() {
        assertDoesNotThrow(() -> screen.putStringXY(0, 0, "", defaultAttr));
    }

    @Test
    @DisplayName("Put long string does not throw exception")
    void testPutLongString() {
        String longString = "A".repeat(200);
        assertDoesNotThrow(() -> screen.putStringXY(0, 0, longString, defaultAttr));
    }

    @Test
    @DisplayName("Vertical line draws correctly")
    void testVLineXY() {
        assertDoesNotThrow(() -> screen.vLineXY(5, 0, 10, '|', defaultAttr));
    }

    @Test
    @DisplayName("Horizontal line draws correctly")
    void testHLineXY() {
        assertDoesNotThrow(() -> screen.hLineXY(0, 5, 20, '-', defaultAttr));
    }

    @Test
    @DisplayName("Draw box does not throw exception")
    void testDrawBox() {
        assertDoesNotThrow(() -> screen.drawBox(5, 5, 20, 10, defaultAttr, defaultAttr, BorderStyle.SINGLE, false));
    }

    @Test
    @DisplayName("Draw box with zero width does not throw exception")
    void testDrawBoxZeroWidth() {
        assertDoesNotThrow(() -> screen.drawBox(5, 5, 5, 10, defaultAttr, defaultAttr, BorderStyle.SINGLE, false));
    }

    @Test
    @DisplayName("Draw box with zero height does not throw exception")
    void testDrawBoxZeroHeight() {
        assertDoesNotThrow(() -> screen.drawBox(5, 5, 10, 5, defaultAttr, defaultAttr, BorderStyle.SINGLE, false));
    }

    @Test
    @DisplayName("Draw box at screen edges does not throw exception")
    void testDrawBoxAtEdges() {
        assertDoesNotThrow(() -> screen.drawBox(0, 0, screen.getWidth() - 1, screen.getHeight() - 1, defaultAttr, defaultAttr, BorderStyle.SINGLE, false));
    }

    // Clipping tests

    @Test
    @DisplayName("Default clipping bounds match screen dimensions")
    void testDefaultClippingBounds() {
        assertEquals(0, screen.getClipLeft());
        assertEquals(0, screen.getClipTop());
        assertEquals(screen.getWidth(), screen.getClipRight());
        assertEquals(screen.getHeight(), screen.getClipBottom());
    }

    @Test
    @DisplayName("Set clip left updates correctly")
    void testSetClipLeft() {
        screen.setClipLeft(10);
        assertEquals(10, screen.getClipLeft());
    }

    @Test
    @DisplayName("Set clip top updates correctly")
    void testSetClipTop() {
        screen.setClipTop(5);
        assertEquals(5, screen.getClipTop());
    }

    @Test
    @DisplayName("Set clip right updates correctly")
    void testSetClipRight() {
        screen.setClipRight(70);
        assertEquals(70, screen.getClipRight());
    }

    @Test
    @DisplayName("Set clip bottom updates correctly")
    void testSetClipBottom() {
        screen.setClipBottom(20);
        assertEquals(20, screen.getClipBottom());
    }

    @Test
    @DisplayName("Set all clipping bounds works correctly")
    void testSetAllClippingBounds() {
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
    @DisplayName("Clipping does not affect unclipped drawing")
    void testClippingDoesNotAffectNormalDrawing() {
        screen.setClipLeft(10);
        screen.setClipTop(5);
        screen.setClipRight(70);
        screen.setClipBottom(20);
        
        // Drawing within clipped region should work
        assertDoesNotThrow(() -> screen.putCharXY(15, 10, 'A', defaultAttr));
    }

    // Offset tests

    @Test
    @DisplayName("Default offset is zero")
    void testDefaultOffset() {
        assertEquals(0, screen.getOffsetX());
        assertEquals(0, screen.getOffsetY());
    }

    @Test
    @DisplayName("Set offset X updates correctly")
    void testSetOffsetX() {
        screen.setOffsetX(10);
        assertEquals(10, screen.getOffsetX());
    }

    @Test
    @DisplayName("Set offset Y updates correctly")
    void testSetOffsetY() {
        screen.setOffsetY(5);
        assertEquals(5, screen.getOffsetY());
    }

    @Test
    @DisplayName("Set negative offset works correctly")
    void testSetNegativeOffset() {
        screen.setOffsetX(-10);
        screen.setOffsetY(-5);
        assertEquals(-10, screen.getOffsetX());
        assertEquals(-5, screen.getOffsetY());
    }

    @Test
    @DisplayName("Set large offset works correctly")
    void testSetLargeOffset() {
        screen.setOffsetX(1000);
        screen.setOffsetY(1000);
        assertEquals(1000, screen.getOffsetX());
        assertEquals(1000, screen.getOffsetY());
    }

    // Cursor tests

    @Test
    @DisplayName("Cursor is not visible by default")
    void testCursorNotVisibleByDefault() {
        assertFalse(screen.isCursorVisible());
    }

    @Test
    @DisplayName("Put cursor makes cursor visible")
    void testPutCursor() {
        screen.putCursor(true, 10, 5);
        assertTrue(screen.isCursorVisible());
        assertEquals(10, screen.getCursorX());
        assertEquals(5, screen.getCursorY());
    }

    @Test
    @DisplayName("Hide cursor makes cursor invisible")
    void testHideCursor() {
        screen.putCursor(true, 10, 5);
        assertTrue(screen.isCursorVisible());
        
        screen.putCursor(false, 0, 0);
        assertFalse(screen.isCursorVisible());
    }

    @Test
    @DisplayName("Cursor position can be updated")
    void testUpdateCursorPosition() {
        screen.putCursor(true, 10, 5);
        assertEquals(10, screen.getCursorX());
        assertEquals(5, screen.getCursorY());
        
        screen.putCursor(true, 20, 10);
        assertEquals(20, screen.getCursorX());
        assertEquals(10, screen.getCursorY());
    }

    // Clear tests

    @Test
    @DisplayName("Clear does not throw exception")
    void testClear() {
        assertDoesNotThrow(() -> screen.clear());
    }

    @Test
    @DisplayName("Clear physical does not throw exception")
    void testClearPhysical() {
        assertDoesNotThrow(() -> screen.clearPhysical());
    }

    @Test
    @DisplayName("Clear after drawing does not throw exception")
    void testClearAfterDrawing() {
        screen.putStringXY(5, 5, "Test", defaultAttr);
        assertDoesNotThrow(() -> screen.clear());
    }

    @Test
    @DisplayName("Multiple clears do not cause issues")
    void testMultipleClears() {
        assertDoesNotThrow(() -> {
            screen.clear();
            screen.clear();
            screen.clear();
        });
    }

    // Text dimension tests

    @Test
    @DisplayName("Text width returns positive value")
    void testGetTextWidth() {
        int textWidth = screen.getTextWidth();
        assertTrue(textWidth > 0, "Text width should be positive");
    }

    @Test
    @DisplayName("Text height returns positive value")
    void testGetTextHeight() {
        int textHeight = screen.getTextHeight();
        assertTrue(textHeight > 0, "Text height should be positive");
    }

    // Title tests

    @Test
    @DisplayName("Set title does not throw exception")
    void testSetTitle() {
        assertDoesNotThrow(() -> screen.setTitle("Test Title"));
    }

    @Test
    @DisplayName("Set empty title does not throw exception")
    void testSetEmptyTitle() {
        assertDoesNotThrow(() -> screen.setTitle(""));
    }

    @Test
    @DisplayName("Set null title does not throw exception")
    void testSetNullTitle() {
        assertDoesNotThrow(() -> screen.setTitle(null));
    }

    // Complex operation tests

    @Test
    @DisplayName("Multiple drawing operations in sequence work correctly")
    void testMultipleDrawingOperations() {
        assertDoesNotThrow(() -> {
            screen.putCharXY(0, 0, 'A', defaultAttr);
            screen.putStringXY(5, 5, "Hello", defaultAttr);
            screen.hLineXY(0, 10, 20, '-', defaultAttr);
            screen.vLineXY(10, 0, 10, '|', defaultAttr);
            screen.drawBox(15, 15, 30, 20, defaultAttr, defaultAttr, BorderStyle.SINGLE, false);
        });
    }

    @Test
    @DisplayName("Drawing after dimension change works correctly")
    void testDrawingAfterDimensionChange() {
        screen.putStringXY(5, 5, "Before", defaultAttr);
        screen.setDimensions(120, 40);
        
        assertDoesNotThrow(() -> {
            screen.putStringXY(10, 10, "After", defaultAttr);
        });
    }

    @Test
    @DisplayName("Screen operations with clipping and offset work together")
    void testComplexScreenOperations() {
        screen.setOffsetX(5);
        screen.setOffsetY(3);
        screen.setClipLeft(10);
        screen.setClipTop(5);
        screen.setClipRight(70);
        screen.setClipBottom(20);
        
        assertDoesNotThrow(() -> {
            screen.putStringXY(15, 10, "Test", defaultAttr);
            screen.putCursor(true, 20, 12);
            screen.flushPhysical();
        });
    }

    @Test
    @DisplayName("Screen handles rapid dimension changes")
    void testRapidDimensionChanges() {
        for (int i = 0; i < 10; i++) {
            screen.setDimensions(80 + i * 10, 24 + i * 5);
        }
        assertEquals(170, screen.getWidth());
        assertEquals(69, screen.getHeight());
    }

    @Test
    @DisplayName("Screen state is consistent after multiple operations")
    void testScreenStateConsistency() {
        screen.setDimensions(100, 50);
        screen.setOffsetX(5);
        screen.setOffsetY(3);
        screen.putStringXY(10, 10, "Test", defaultAttr);
        screen.putCursor(true, 15, 12);
        
        assertEquals(100, screen.getWidth());
        assertEquals(50, screen.getHeight());
        assertEquals(5, screen.getOffsetX());
        assertEquals(3, screen.getOffsetY());
        assertTrue(screen.isCursorVisible());
        assertEquals(15, screen.getCursorX());
        assertEquals(12, screen.getCursorY());
    }

    @Test
    @DisplayName("Copy screen operation does not throw exception")
    void testCopyScreen() {
        TestableLogicalScreen sourceScreen = new TestableLogicalScreen(80, 24);
        sourceScreen.putStringXY(5, 5, "Test", defaultAttr);
        
        assertDoesNotThrow(() -> screen.copyScreen(sourceScreen));
    }

    @Test
    @DisplayName("Snapshot operation does not throw exception")
    void testSnapshot() {
        screen.putStringXY(5, 5, "Test", defaultAttr);
        assertDoesNotThrow(() -> screen.snapshot());
    }
}
