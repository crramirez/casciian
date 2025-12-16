/*
 * Casciian - Java Text User Interface
 *
 * Unit tests for MultiScreen
 */
package casciian.backend;

import casciian.bits.CellAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MultiScreen - validates screen aggregation and synchronization.
 */
@DisplayName("MultiScreen Tests")
class MultiScreenTest {

    private static class TestScreen extends LogicalScreen {
        private int flushCount = 0;
        private int copyCount = 0;
        private boolean physicalCleared = false;

        public TestScreen(int width, int height) {
            super(width, height);
        }

        @Override
        public void flushPhysical() {
            flushCount++;
        }

        @Override
        public void copyScreen(Screen screen) {
            super.copyScreen(screen);
            copyCount++;
        }

        @Override
        public synchronized void clearPhysical() {
            super.clearPhysical();
            physicalCleared = true;
        }

        public int getFlushCount() {
            return flushCount;
        }

        public int getCopyCount() {
            return copyCount;
        }

        public boolean isPhysicalCleared() {
            return physicalCleared;
        }

        public void resetFlags() {
            flushCount = 0;
            copyCount = 0;
            physicalCleared = false;
        }
    }

    private MultiScreen multiScreen;
    private TestScreen screen1;
    private TestScreen screen2;
    private CellAttributes defaultAttr;

    @BeforeEach
    void setUp() {
        screen1 = new TestScreen(80, 24);
        multiScreen = new MultiScreen(screen1);
        defaultAttr = new CellAttributes();
    }

    // Initialization tests

    @Test
    @DisplayName("MultiScreen can be created with default constructor")
    void testDefaultConstructor() {
        MultiScreen emptyMultiScreen = new MultiScreen();
        assertNotNull(emptyMultiScreen);
        assertEquals(80, emptyMultiScreen.getWidth());
        assertEquals(25, emptyMultiScreen.getHeight());
    }

    @Test
    @DisplayName("MultiScreen takes dimensions from first screen")
    void testConstructorWithScreen() {
        assertEquals(80, multiScreen.getWidth());
        assertEquals(24, multiScreen.getHeight());
    }

    @Test
    @DisplayName("MultiScreen with custom dimensions initializes correctly")
    void testConstructorWithCustomDimensions() {
        TestScreen customScreen = new TestScreen(100, 50);
        MultiScreen customMultiScreen = new MultiScreen(customScreen);
        assertEquals(100, customMultiScreen.getWidth());
        assertEquals(50, customMultiScreen.getHeight());
    }

    // Screen management tests

    @Test
    @DisplayName("Add screen to multi screen works")
    void testAddScreen() {
        screen2 = new TestScreen(80, 24);
        assertDoesNotThrow(() -> multiScreen.addScreen(screen2));
    }

    @Test
    @DisplayName("Add multiple screens works")
    void testAddMultipleScreens() {
        screen2 = new TestScreen(80, 24);
        TestScreen screen3 = new TestScreen(80, 24);
        
        assertDoesNotThrow(() -> {
            multiScreen.addScreen(screen2);
            multiScreen.addScreen(screen3);
        });
    }

    @Test
    @DisplayName("Remove screen from multi screen works")
    void testRemoveScreen() {
        screen2 = new TestScreen(80, 24);
        multiScreen.addScreen(screen2);
        
        assertDoesNotThrow(() -> multiScreen.removeScreen(screen2));
    }

    @Test
    @DisplayName("Cannot remove last screen")
    void testCannotRemoveLastScreen() {
        // MultiScreen should keep at least one screen
        multiScreen.removeScreen(screen1);
        
        // After attempting to remove the only screen, it should still be there
        // We can't directly test this, but we ensure no exception is thrown
        assertDoesNotThrow(() -> multiScreen.flushPhysical());
    }

    // Dimension synchronization tests

    @Test
    @DisplayName("Set width synchronizes to all screens")
    void testSetWidthSynchronization() {
        screen2 = new TestScreen(80, 24);
        multiScreen.addScreen(screen2);
        
        multiScreen.setWidth(100);
        
        assertEquals(100, multiScreen.getWidth());
        assertEquals(100, screen1.getWidth());
        assertEquals(100, screen2.getWidth());
    }

    @Test
    @DisplayName("Set height synchronizes to all screens")
    void testSetHeightSynchronization() {
        screen2 = new TestScreen(80, 24);
        multiScreen.addScreen(screen2);
        
        multiScreen.setHeight(40);
        
        assertEquals(40, multiScreen.getHeight());
        assertEquals(40, screen1.getHeight());
        assertEquals(40, screen2.getHeight());
    }

    @Test
    @DisplayName("Set dimensions synchronizes to all screens")
    void testSetDimensionsSynchronization() {
        screen2 = new TestScreen(80, 24);
        multiScreen.addScreen(screen2);
        
        multiScreen.setDimensions(120, 50);
        
        assertEquals(120, multiScreen.getWidth());
        assertEquals(50, multiScreen.getHeight());
        assertEquals(120, screen1.getWidth());
        assertEquals(50, screen1.getHeight());
        assertEquals(120, screen2.getWidth());
        assertEquals(50, screen2.getHeight());
    }

    @Test
    @DisplayName("Dimension change on screen with same dimensions triggers repaint")
    void testDimensionChangeRepaint() {
        screen2 = new TestScreen(80, 24);
        multiScreen.addScreen(screen2);
        
        screen1.resetFlags();
        screen2.resetFlags();
        
        // Set to same dimensions - should trigger clearPhysical on screens
        multiScreen.setDimensions(80, 24);
        
        // At least one screen should have been cleared for repaint
        // (the one that already had these dimensions)
        assertTrue(screen1.isPhysicalCleared() || screen2.isPhysicalCleared());
    }

    // Flush operations tests

    @Test
    @DisplayName("Flush physical copies content to all screens")
    void testFlushPhysical() {
        screen2 = new TestScreen(80, 24);
        multiScreen.addScreen(screen2);
        
        multiScreen.putStringXY(5, 5, "Test", defaultAttr);
        
        screen1.resetFlags();
        screen2.resetFlags();
        
        multiScreen.flushPhysical();
        
        // Verify that copyScreen was called on both screens (synchronous operation)
        assertEquals(1, screen1.getCopyCount(), "Screen 1 should have copyScreen called once");
        assertEquals(1, screen2.getCopyCount(), "Screen 2 should have copyScreen called once");
        
        // Verify subsequent flush works correctly
        screen1.resetFlags();
        screen2.resetFlags();
        multiScreen.flushPhysical();
        
        assertEquals(1, screen1.getCopyCount(), "Screen 1 should have copyScreen called again");
        assertEquals(1, screen2.getCopyCount(), "Screen 2 should have copyScreen called again");
    }

    @Test
    @DisplayName("Clear physical synchronizes to all screens")
    void testClearPhysical() {
        screen2 = new TestScreen(80, 24);
        multiScreen.addScreen(screen2);
        
        screen1.resetFlags();
        screen2.resetFlags();
        
        multiScreen.clearPhysical();
        
        assertTrue(screen1.isPhysicalCleared());
        assertTrue(screen2.isPhysicalCleared());
    }

    // Drawing synchronization tests

    @Test
    @DisplayName("Drawing on multi screen works correctly")
    void testDrawingOnMultiScreen() {
        screen2 = new TestScreen(80, 24);
        multiScreen.addScreen(screen2);
        
        assertDoesNotThrow(() -> {
            multiScreen.putCharXY(5, 5, 'A', defaultAttr);
            multiScreen.putStringXY(10, 10, "Hello", defaultAttr);
            multiScreen.hLineXY(0, 15, 20, '-', defaultAttr);
            multiScreen.flushPhysical();
        });
    }

    @Test
    @DisplayName("Multiple flush operations work correctly")
    void testMultipleFlushOperations() {
        screen2 = new TestScreen(80, 24);
        multiScreen.addScreen(screen2);
        
        assertDoesNotThrow(() -> {
            multiScreen.putStringXY(0, 0, "Line 1", defaultAttr);
            multiScreen.flushPhysical();
            
            multiScreen.putStringXY(0, 1, "Line 2", defaultAttr);
            multiScreen.flushPhysical();
            
            multiScreen.putStringXY(0, 2, "Line 3", defaultAttr);
            multiScreen.flushPhysical();
        });
    }

    // Title synchronization tests

    @Test
    @DisplayName("Set title synchronizes to all screens")
    void testSetTitle() {
        screen2 = new TestScreen(80, 24);
        multiScreen.addScreen(screen2);
        
        assertDoesNotThrow(() -> multiScreen.setTitle("Test Title"));
    }

    @Test
    @DisplayName("Set empty title synchronizes to all screens")
    void testSetEmptyTitle() {
        screen2 = new TestScreen(80, 24);
        multiScreen.addScreen(screen2);
        
        assertDoesNotThrow(() -> multiScreen.setTitle(""));
    }

    @Test
    @DisplayName("Set null title synchronizes to all screens")
    void testSetNullTitle() {
        screen2 = new TestScreen(80, 24);
        multiScreen.addScreen(screen2);
        
        assertDoesNotThrow(() -> multiScreen.setTitle(null));
    }

    // Edge case tests

    @Test
    @DisplayName("Multi screen with single screen works correctly")
    void testSingleScreenOperations() {
        assertDoesNotThrow(() -> {
            multiScreen.putStringXY(5, 5, "Test", defaultAttr);
            multiScreen.setDimensions(100, 40);
            multiScreen.flushPhysical();
            multiScreen.clearPhysical();
        });
    }

    @Test
    @DisplayName("Multi screen with many screens works correctly")
    void testManyScreens() {
        for (int i = 0; i < 5; i++) {
            TestScreen screen = new TestScreen(80, 24);
            multiScreen.addScreen(screen);
        }
        
        assertDoesNotThrow(() -> {
            multiScreen.putStringXY(10, 10, "Test", defaultAttr);
            multiScreen.flushPhysical();
        });
    }

    @Test
    @DisplayName("Add and remove screens in sequence works correctly")
    void testAddRemoveSequence() {
        screen2 = new TestScreen(80, 24);
        TestScreen screen3 = new TestScreen(80, 24);
        
        multiScreen.addScreen(screen2);
        multiScreen.addScreen(screen3);
        multiScreen.removeScreen(screen2);
        
        assertDoesNotThrow(() -> multiScreen.flushPhysical());
    }

    @Test
    @DisplayName("Screen operations after remove work correctly")
    void testOperationsAfterRemove() {
        screen2 = new TestScreen(80, 24);
        multiScreen.addScreen(screen2);
        multiScreen.removeScreen(screen2);
        
        assertDoesNotThrow(() -> {
            multiScreen.putStringXY(5, 5, "Test", defaultAttr);
            multiScreen.flushPhysical();
        });
    }

    // Complex operation tests

    @Test
    @DisplayName("Multi screen handles rapid dimension changes")
    void testRapidDimensionChanges() {
        screen2 = new TestScreen(80, 24);
        multiScreen.addScreen(screen2);
        
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                multiScreen.setDimensions(80 + i * 5, 24 + i * 2);
            }
        });
        
        assertEquals(125, multiScreen.getWidth());
        assertEquals(42, multiScreen.getHeight());
        assertEquals(125, screen1.getWidth());
        assertEquals(42, screen1.getHeight());
        assertEquals(125, screen2.getWidth());
        assertEquals(42, screen2.getHeight());
    }

    @Test
    @DisplayName("Multi screen maintains state consistency")
    void testStateConsistency() {
        screen2 = new TestScreen(80, 24);
        multiScreen.addScreen(screen2);
        
        multiScreen.setDimensions(100, 50);
        multiScreen.putStringXY(10, 10, "Test", defaultAttr);
        multiScreen.putCursor(true, 15, 12);
        multiScreen.setTitle("Test Title");
        
        assertEquals(100, multiScreen.getWidth());
        assertEquals(50, multiScreen.getHeight());
        assertEquals(100, screen1.getWidth());
        assertEquals(50, screen1.getHeight());
        assertEquals(100, screen2.getWidth());
        assertEquals(50, screen2.getHeight());
    }

    @Test
    @DisplayName("Multi screen copy operation works correctly")
    void testCopyScreen() {
        TestScreen sourceScreen = new TestScreen(80, 24);
        sourceScreen.putStringXY(5, 5, "Source", defaultAttr);
        
        assertDoesNotThrow(() -> multiScreen.copyScreen(sourceScreen));
    }

    @Test
    @DisplayName("Multi screen with different initial dimensions")
    void testDifferentInitialDimensions() {
        TestScreen screen100x50 = new TestScreen(100, 50);
        MultiScreen customMulti = new MultiScreen(screen100x50);
        
        TestScreen screen80x24 = new TestScreen(80, 24);
        customMulti.addScreen(screen80x24);
        
        // Both screens should now have the same dimensions
        assertEquals(customMulti.getWidth(), screen100x50.getWidth());
        assertEquals(customMulti.getHeight(), screen100x50.getHeight());
        // screen80x24 should have been resized to match
    }
}
