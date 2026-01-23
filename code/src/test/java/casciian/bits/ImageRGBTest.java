package casciian.bits;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ImageRGB}.
 */
@DisplayName("ImageRGB Tests")
class ImageRGBTest {

    @Test
    void testConstructorWithDimensions() {
        ImageRGB image = new ImageRGB(10, 20);
        assertEquals(10, image.getWidth());
        assertEquals(20, image.getHeight());
    }

    @Test
    void testConstructorCopy() {
        ImageRGB original = new ImageRGB(5, 5);
        original.setRGB(2, 3, 0xFF0000);
        
        ImageRGB copy = new ImageRGB(original);
        
        assertEquals(original.getWidth(), copy.getWidth());
        assertEquals(original.getHeight(), copy.getHeight());
        assertEquals(0xFF0000, copy.getRGB(2, 3));
        
        // Verify it's a deep copy
        copy.setRGB(2, 3, 0x00FF00);
        assertEquals(0xFF0000, original.getRGB(2, 3));
        assertEquals(0x00FF00, copy.getRGB(2, 3));
    }

    @Test
    void testGetAndSetRGB() {
        ImageRGB image = new ImageRGB(10, 10);
        
        image.setRGB(5, 7, 0x123456);
        assertEquals(0x123456, image.getRGB(5, 7));
        
        image.setRGB(0, 0, 0xFFFFFF);
        assertEquals(0xFFFFFF, image.getRGB(0, 0));
    }

    @Test
    void testGetRGBRegion() {
        ImageRGB image = new ImageRGB(10, 10);
        
        // Fill a 3x3 region with red
        for (int y = 2; y < 5; y++) {
            for (int x = 3; x < 6; x++) {
                image.setRGB(x, y, 0xFF0000);
            }
        }
        
        int[] rgbArray = image.getRGB(3, 2, 3, 3, null, 0, 3);
        
        assertNotNull(rgbArray);
        assertTrue(rgbArray.length >= 9);
        
        // Verify all pixels are red
        for (int i = 0; i < 9; i++) {
            assertEquals(0xFF0000, rgbArray[i]);
        }
    }

    @Test
    void testGetRGBRegionWithExistingArray() {
        ImageRGB image = new ImageRGB(10, 10);
        image.setRGB(1, 1, 0x00FF00);
        image.setRGB(2, 1, 0x0000FF);
        
        int[] rgbArray = new int[10];
        int[] result = image.getRGB(1, 1, 2, 1, rgbArray, 0, 2);
        
        assertSame(rgbArray, result);
        assertEquals(0x00FF00, rgbArray[0]);
        assertEquals(0x0000FF, rgbArray[1]);
    }

    @Test
    void testGetRGBRegionInvalidDimensions() {
        ImageRGB image = new ImageRGB(10, 10);
        
        Exception e1 = assertThrows(IllegalArgumentException.class, 
            () -> image.getRGB(-1, 0, 5, 5, null, 0, 5));
        assertTrue(e1.getMessage().contains("Invalid region dimensions"));
        
        Exception e2 = assertThrows(IllegalArgumentException.class, 
            () -> image.getRGB(0, 0, 11, 5, null, 0, 11));
        assertTrue(e2.getMessage().contains("Invalid region dimensions"));
        
        Exception e3 = assertThrows(IllegalArgumentException.class, 
            () -> image.getRGB(0, 0, 0, 5, null, 0, 0));
        assertTrue(e3.getMessage().contains("Invalid region dimensions"));
    }

    @Test
    void testSetRGBRegion() {
        ImageRGB image = new ImageRGB(10, 10);
        
        int[] rgbData = new int[9];
        for (int i = 0; i < 9; i++) {
            rgbData[i] = 0xFF0000 + i;
        }
        
        image.setRGB(2, 3, 3, 3, rgbData, 0, 3);
        
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int expected = 0xFF0000 + (row * 3 + col);
                assertEquals(expected, image.getRGB(2 + col, 3 + row));
            }
        }
    }

    @Test
    void testSetRGBRegionInvalidDimensions() {
        ImageRGB image = new ImageRGB(10, 10);
        int[] rgbData = new int[25];
        
        Exception e1 = assertThrows(IllegalArgumentException.class, 
            () -> image.setRGB(-1, 0, 5, 5, rgbData, 0, 5));
        assertTrue(e1.getMessage().contains("Invalid region dimensions"));
        
        Exception e2 = assertThrows(IllegalArgumentException.class, 
            () -> image.setRGB(0, 0, 11, 5, rgbData, 0, 11));
        assertTrue(e2.getMessage().contains("Invalid region dimensions"));
    }

    @Test
    void testAlphaBlendOver() {
        ImageRGB under = new ImageRGB(5, 5);
        ImageRGB over = new ImageRGB(5, 5);
        
        // Fill under with red
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                under.setRGB(x, y, 0xFF0000);
            }
        }
        
        // Fill over with blue
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                over.setRGB(x, y, 0x0000FF);
            }
        }
        
        // Blend 50%
        under.alphaBlendOver(over, 0.5);
        
        // Result should be purple-ish (mix of red and blue)
        int blended = under.getRGB(0, 0);
        int red = (blended >>> 16) & 0xFF;
        int blue = blended & 0xFF;
        
        assertTrue(red > 0);
        assertTrue(blue > 0);
    }

    @Test
    void testAlphaBlendOverNullImage() {
        ImageRGB image = new ImageRGB(5, 5);
        
        Exception e = assertThrows(IllegalArgumentException.class, 
            () -> image.alphaBlendOver(null, 0.5));
        assertTrue(e.getMessage().contains("Image to alpha-blend over cannot be null"));
    }

    @Test
    void testAlphaBlendOverMismatchedDimensions() {
        ImageRGB image1 = new ImageRGB(5, 5);
        ImageRGB image2 = new ImageRGB(10, 10);
        
        Exception e = assertThrows(IllegalArgumentException.class, 
            () -> image1.alphaBlendOver(image2, 0.5));
        assertTrue(e.getMessage().contains("Image dimensions must match for alpha blending"));
    }

    @Test
    void testGetSubimage() {
        ImageRGB image = new ImageRGB(10, 10);
        
        // Set some pixels
        image.setRGB(2, 3, 0xFF0000);
        image.setRGB(3, 3, 0x00FF00);
        image.setRGB(2, 4, 0x0000FF);
        
        ImageRGB subimage = image.getSubimage(2, 3, 3, 3);
        
        assertEquals(3, subimage.getWidth());
        assertEquals(3, subimage.getHeight());
        assertEquals(0xFF0000, subimage.getRGB(0, 0));
        assertEquals(0x00FF00, subimage.getRGB(1, 0));
        assertEquals(0x0000FF, subimage.getRGB(0, 1));
    }

    @Test
    void testGetSubimageInvalidDimensions() {
        ImageRGB image = new ImageRGB(10, 10);
        
        Exception e1 = assertThrows(IllegalArgumentException.class, 
            () -> image.getSubimage(-1, 0, 5, 5));
        assertTrue(e1.getMessage().contains("Invalid subimage dimensions"));
        
        Exception e2 = assertThrows(IllegalArgumentException.class, 
            () -> image.getSubimage(0, 0, 0, 5));
        assertTrue(e2.getMessage().contains("Invalid subimage dimensions"));
    }

    @Test
    void testGetSubimageBeyondBounds() {
        ImageRGB image = new ImageRGB(10, 10);
        image.setRGB(5, 5, 0xFF0000);
        
        // Request subimage that goes beyond bounds
        ImageRGB subimage = image.getSubimage(8, 8, 5, 5);
        
        assertEquals(5, subimage.getWidth());
        assertEquals(5, subimage.getHeight());
        // Pixels beyond bounds should be 0 (black/default)
        assertEquals(0, subimage.getRGB(0, 0));
    }

    @Test
    void testFillRect() {
        ImageRGB image = new ImageRGB(10, 10);
        
        image.fillRect(2, 3, 4, 3, 0xFF0000);
        
        // Check filled area
        for (int y = 3; y < 6; y++) {
            for (int x = 2; x < 6; x++) {
                assertEquals(0xFF0000, image.getRGB(x, y));
            }
        }
        
        // Check unfilled area
        assertEquals(0, image.getRGB(0, 0));
        assertEquals(0, image.getRGB(9, 9));
    }

    @Test
    void testFillRectInvalidDimensions() {
        ImageRGB image = new ImageRGB(10, 10);
        
        Exception e1 = assertThrows(IllegalArgumentException.class, 
            () -> image.fillRect(-1, 0, 5, 5, 0xFF0000));
        assertTrue(e1.getMessage().contains("Invalid fill rectangle dimensions"));
        
        Exception e2 = assertThrows(IllegalArgumentException.class, 
            () -> image.fillRect(0, 0, 11, 5, 0xFF0000));
        assertTrue(e2.getMessage().contains("Invalid fill rectangle dimensions"));
    }

    @Test
    void testResizeCanvasLarger() {
        ImageRGB image = new ImageRGB(5, 5);
        image.setRGB(2, 2, 0xFF0000);
        
        ImageRGB resized = image.resizeCanvas(10, 10, 0x0000FF);
        
        assertEquals(10, resized.getWidth());
        assertEquals(10, resized.getHeight());
        
        // Original pixel should be preserved
        assertEquals(0xFF0000, resized.getRGB(2, 2));
        
        // New area should be filled with background color
        assertEquals(0x0000FF, resized.getRGB(9, 9));
        assertEquals(0x0000FF, resized.getRGB(6, 6));
    }

    @Test
    void testResizeCanvasSmaller() {
        ImageRGB image = new ImageRGB(10, 10);
        image.setRGB(2, 2, 0xFF0000);
        image.setRGB(8, 8, 0x00FF00);
        
        ImageRGB resized = image.resizeCanvas(5, 5, 0x0000FF);
        
        assertEquals(5, resized.getWidth());
        assertEquals(5, resized.getHeight());
        
        // Pixel within bounds should be preserved
        assertEquals(0xFF0000, resized.getRGB(2, 2));
    }

    @Test
    void testResizeCanvasInvalidDimensions() {
        ImageRGB image = new ImageRGB(10, 10);
        
        Exception e1 = assertThrows(IllegalArgumentException.class, 
            () -> image.resizeCanvas(0, 10, 0));
        assertTrue(e1.getMessage().contains("New dimensions must be positive"));
        
        Exception e2 = assertThrows(IllegalArgumentException.class, 
            () -> image.resizeCanvas(10, -1, 0));
        assertTrue(e2.getMessage().contains("New dimensions must be positive"));
    }

    @Test
    void testGetWidthAndHeight() {
        ImageRGB image = new ImageRGB(123, 456);
        
        assertEquals(123, image.getWidth());
        assertEquals(456, image.getHeight());
    }

    // ========================================================================
    // Tests for parallel execution paths (images > 10,000 pixels)
    // These tests verify that parallel stream processing works correctly
    // without race conditions or concurrency issues.
    // ========================================================================

    @Test
    @DisplayName("Copy constructor with large image (parallel path)")
    void testConstructorCopyLargeImage() {
        // 200x100 = 20,000 pixels, exceeds PARALLEL_THRESHOLD of 10,000
        ImageRGB original = new ImageRGB(200, 100);
        
        // Set pixels at various locations to verify correct copying
        original.setRGB(0, 0, 0xFF0000);      // top-left corner
        original.setRGB(199, 99, 0x00FF00);   // bottom-right corner
        original.setRGB(100, 50, 0x0000FF);   // center
        original.setRGB(50, 25, 0xFFFF00);    // quarter point
        
        ImageRGB copy = new ImageRGB(original);
        
        assertEquals(original.getWidth(), copy.getWidth());
        assertEquals(original.getHeight(), copy.getHeight());
        
        // Verify all set pixels are correctly copied
        assertEquals(0xFF0000, copy.getRGB(0, 0));
        assertEquals(0x00FF00, copy.getRGB(199, 99));
        assertEquals(0x0000FF, copy.getRGB(100, 50));
        assertEquals(0xFFFF00, copy.getRGB(50, 25));
        
        // Verify it's a deep copy
        copy.setRGB(100, 50, 0x123456);
        assertEquals(0x0000FF, original.getRGB(100, 50));
        assertEquals(0x123456, copy.getRGB(100, 50));
    }

    @Test
    @DisplayName("alphaBlendOver with large image (parallel path)")
    void testAlphaBlendOverLargeImage() {
        // 150x100 = 15,000 pixels, exceeds PARALLEL_THRESHOLD
        ImageRGB under = new ImageRGB(150, 100);
        ImageRGB over = new ImageRGB(150, 100);
        
        // Fill under with red, over with blue
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 150; x++) {
                under.setRGB(x, y, 0xFF0000);
                over.setRGB(x, y, 0x0000FF);
            }
        }
        
        // Blend 50%
        under.alphaBlendOver(over, 0.5);
        
        // Verify blending at multiple positions to check for race conditions
        int[][] checkPositions = {{0, 0}, {149, 99}, {75, 50}, {25, 25}, {100, 75}};
        for (int[] pos : checkPositions) {
            int blended = under.getRGB(pos[0], pos[1]);
            int red = (blended >>> 16) & 0xFF;
            int blue = blended & 0xFF;
            
            // Both red and blue should be present in the blend
            assertTrue(red > 0, "Red component should be > 0 at (" + pos[0] + "," + pos[1] + ")");
            assertTrue(blue > 0, "Blue component should be > 0 at (" + pos[0] + "," + pos[1] + ")");
        }
    }

    @Test
    @DisplayName("getSubimage with large image (parallel path)")
    void testGetSubimageLargeImage() {
        // 200x100 = 20,000 pixels source, extracting 150x100 = 15,000 pixels
        ImageRGB image = new ImageRGB(200, 100);
        
        // Set pixels at specific positions
        image.setRGB(10, 10, 0xFF0000);
        image.setRGB(100, 50, 0x00FF00);
        image.setRGB(140, 90, 0x0000FF);
        
        ImageRGB subimage = image.getSubimage(5, 5, 150, 100);
        
        assertEquals(150, subimage.getWidth());
        assertEquals(100, subimage.getHeight());
        
        // Verify pixels are at correct offset positions
        assertEquals(0xFF0000, subimage.getRGB(5, 5));   // was at (10,10), now at (5,5)
        assertEquals(0x00FF00, subimage.getRGB(95, 45)); // was at (100,50), now at (95,45)
        assertEquals(0x0000FF, subimage.getRGB(135, 85)); // was at (140,90), now at (135,85)
    }

    @Test
    @DisplayName("fillRect with large area (parallel path)")
    void testFillRectLargeImage() {
        // 200x100 = 20,000 pixels
        ImageRGB image = new ImageRGB(200, 100);
        
        // Fill a large rectangle: 150x80 = 12,000 pixels
        image.fillRect(10, 10, 150, 80, 0xFF0000);
        
        // Verify corners of filled area
        assertEquals(0xFF0000, image.getRGB(10, 10));     // top-left of fill
        assertEquals(0xFF0000, image.getRGB(159, 89));    // bottom-right of fill
        assertEquals(0xFF0000, image.getRGB(85, 50));     // center of fill
        
        // Verify unfilled areas remain at 0
        assertEquals(0, image.getRGB(0, 0));
        assertEquals(0, image.getRGB(199, 99));
        assertEquals(0, image.getRGB(9, 9));
        assertEquals(0, image.getRGB(160, 90));
    }

    @Test
    @DisplayName("resizeCanvas larger with large image (parallel path)")
    void testResizeCanvasLargerLargeImage() {
        // Original: 100x100 = 10,000 pixels
        ImageRGB image = new ImageRGB(100, 100);
        image.setRGB(0, 0, 0xFF0000);
        image.setRGB(99, 99, 0x00FF00);
        image.setRGB(50, 50, 0x0000FF);
        
        // Resize to 200x150 = 30,000 pixels
        ImageRGB resized = image.resizeCanvas(200, 150, 0xABCDEF);
        
        assertEquals(200, resized.getWidth());
        assertEquals(150, resized.getHeight());
        
        // Original pixels should be preserved
        assertEquals(0xFF0000, resized.getRGB(0, 0));
        assertEquals(0x00FF00, resized.getRGB(99, 99));
        assertEquals(0x0000FF, resized.getRGB(50, 50));
        
        // New areas should have background color
        assertEquals(0xABCDEF, resized.getRGB(199, 149));
        assertEquals(0xABCDEF, resized.getRGB(150, 100));
        assertEquals(0xABCDEF, resized.getRGB(100, 0));   // beyond original width
        assertEquals(0xABCDEF, resized.getRGB(0, 100));   // beyond original height
    }

    @Test
    @DisplayName("resizeCanvas smaller with large image (parallel path)")
    void testResizeCanvasSmallerLargeImage() {
        // Original: 200x150 = 30,000 pixels
        ImageRGB image = new ImageRGB(200, 150);
        image.setRGB(0, 0, 0xFF0000);
        image.setRGB(99, 99, 0x00FF00);
        image.setRGB(50, 50, 0x0000FF);
        image.setRGB(199, 149, 0xFFFF00);  // will be cropped
        
        // Resize to 150x100 = 15,000 pixels
        ImageRGB resized = image.resizeCanvas(150, 100, 0xABCDEF);
        
        assertEquals(150, resized.getWidth());
        assertEquals(100, resized.getHeight());
        
        // Pixels within bounds should be preserved
        assertEquals(0xFF0000, resized.getRGB(0, 0));
        assertEquals(0x00FF00, resized.getRGB(99, 99));
        assertEquals(0x0000FF, resized.getRGB(50, 50));
    }
}
