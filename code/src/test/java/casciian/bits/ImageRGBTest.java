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

    @Test
    @DisplayName("getRGB region with large image (parallel path)")
    void testGetRGBRegionLargeImage() {
        // 200x100 = 20,000 pixels, exceeds PARALLEL_THRESHOLD of 10,000
        ImageRGB image = new ImageRGB(200, 100);

        // Set pixels at known positions within the region to extract
        image.setRGB(10, 10, 0xFF0000);
        image.setRGB(150, 50, 0x00FF00);
        image.setRGB(190, 90, 0x0000FF);

        // Extract a large region: 180x80 = 14,400 pixels
        int[] rgbArray = image.getRGB(10, 10, 180, 80, null, 0, 180);

        assertNotNull(rgbArray);
        // (10,10) maps to row=0, col=0
        assertEquals(0xFF0000, rgbArray[0]);
        // (150,50) maps to row=40, col=140
        assertEquals(0x00FF00, rgbArray[40 * 180 + 140]);
        // Verify the last pixel in the region at position (189,89)
        int lastRow = 79;
        int lastCol = 179;
        image.setRGB(189, 89, 0xABCDEF);
        rgbArray = image.getRGB(10, 10, 180, 80, null, 0, 180);
        assertEquals(0xABCDEF, rgbArray[lastRow * 180 + lastCol]);
    }

    @Test
    @DisplayName("setRGB region with large image (parallel path)")
    void testSetRGBRegionLargeImage() {
        // 200x100 = 20,000 pixels, exceeds PARALLEL_THRESHOLD of 10,000
        ImageRGB image = new ImageRGB(200, 100);

        // Create a large RGB data array: 150x80 = 12,000 pixels
        int w = 150;
        int h = 80;
        int[] rgbData = new int[w * h];
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                rgbData[row * w + col] = 0x010000 * (row % 256) + col % 256;
            }
        }

        image.setRGB(10, 5, w, h, rgbData, 0, w);

        // Verify pixels at various positions to check for race conditions
        assertEquals(rgbData[0], image.getRGB(10, 5));           // first pixel
        assertEquals(rgbData[79 * w + 149], image.getRGB(159, 84)); // last pixel
        assertEquals(rgbData[40 * w + 75], image.getRGB(85, 45));   // center pixel

        // Verify pixels outside the set region are unchanged
        assertEquals(0, image.getRGB(0, 0));
        assertEquals(0, image.getRGB(199, 99));
    }

    @Test
    @DisplayName("getRGB with offset and scansize larger than width")
    void testGetRGBRegionWithOffsetAndScansize() {
        ImageRGB image = new ImageRGB(10, 10);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 4; x++) {
                image.setRGB(x + 2, y + 3, 0x100 * y + x + 1);
            }
        }

        // Use offset=5 and scansize=6 (larger than w=4)
        int[] rgbArray = new int[25];
        image.getRGB(2, 3, 4, 3, rgbArray, 5, 6);

        // Row 0 starts at index 5
        assertEquals(0x001, rgbArray[5]);
        assertEquals(0x002, rgbArray[6]);
        assertEquals(0x003, rgbArray[7]);
        assertEquals(0x004, rgbArray[8]);
        // Row 1 starts at index 5 + 6 = 11
        assertEquals(0x101, rgbArray[11]);
        assertEquals(0x104, rgbArray[14]);
        // Row 2 starts at index 5 + 12 = 17
        assertEquals(0x201, rgbArray[17]);
    }

    @Test
    @DisplayName("setRGB with offset and scansize larger than width")
    void testSetRGBRegionWithOffsetAndScansize() {
        ImageRGB image = new ImageRGB(10, 10);

        // Source array with offset=3, scansize=5 (larger than w=3)
        int[] rgbData = new int[20];
        rgbData[3] = 0xAA;
        rgbData[4] = 0xBB;
        rgbData[5] = 0xCC;
        rgbData[8] = 0xDD;  // offset 3 + scansize 5 = 8
        rgbData[9] = 0xEE;
        rgbData[10] = 0xFF;

        image.setRGB(1, 2, 3, 2, rgbData, 3, 5);

        assertEquals(0xAA, image.getRGB(1, 2));
        assertEquals(0xBB, image.getRGB(2, 2));
        assertEquals(0xCC, image.getRGB(3, 2));
        assertEquals(0xDD, image.getRGB(1, 3));
        assertEquals(0xEE, image.getRGB(2, 3));
        assertEquals(0xFF, image.getRGB(3, 3));

        // Verify surrounding pixels are unchanged
        assertEquals(0, image.getRGB(0, 2));
        assertEquals(0, image.getRGB(4, 2));
    }

    @Test
    @DisplayName("getRGB and setRGB with single-pixel region boundary")
    void testGetSetRGBSinglePixelRegion() {
        ImageRGB image = new ImageRGB(5, 5);
        image.setRGB(4, 4, 0xABCDEF);

        // Single pixel at corner
        int[] result = image.getRGB(4, 4, 1, 1, null, 0, 1);
        assertEquals(0xABCDEF, result[0]);

        // Set single pixel via region
        int[] data = {0x123456};
        image.setRGB(0, 0, 1, 1, data, 0, 1);
        assertEquals(0x123456, image.getRGB(0, 0));
    }

    // --- scale() tests ---

    @Test
    @DisplayName("scale: invalid dimensions throw IllegalArgumentException")
    void testScaleInvalidDimensions() {
        ImageRGB image = new ImageRGB(10, 10);
        assertThrows(IllegalArgumentException.class, () -> image.scale(0, 10));
        assertThrows(IllegalArgumentException.class, () -> image.scale(10, 0));
        assertThrows(IllegalArgumentException.class, () -> image.scale(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> image.scale(10, -5));
    }

    @Test
    @DisplayName("scale: result has correct dimensions")
    void testScaleDimensions() {
        ImageRGB image = new ImageRGB(20, 30);
        ImageRGB scaled = image.scale(40, 60);
        assertEquals(40, scaled.getWidth());
        assertEquals(60, scaled.getHeight());
    }

    @Test
    @DisplayName("scale: uniform color is preserved")
    void testScaleUniformColor() {
        int color = 0x336699;
        ImageRGB image = new ImageRGB(10, 10);
        image.fillRect(0, 0, 10, 10, color);

        ImageRGB up = image.scale(20, 20);
        for (int y = 0; y < up.getHeight(); y++) {
            for (int x = 0; x < up.getWidth(); x++) {
                assertEquals(color, up.getRGB(x, y),
                        "Pixel (" + x + "," + y + ") should match uniform color");
            }
        }

        ImageRGB down = image.scale(5, 5);
        for (int y = 0; y < down.getHeight(); y++) {
            for (int x = 0; x < down.getWidth(); x++) {
                assertEquals(color, down.getRGB(x, y),
                        "Pixel (" + x + "," + y + ") should match uniform color");
            }
        }
    }

    @Test
    @DisplayName("scale: 1x1 image scales to uniform output")
    void testScale1x1() {
        ImageRGB image = new ImageRGB(1, 1);
        image.setRGB(0, 0, 0xAABBCC);

        ImageRGB scaled = image.scale(5, 5);
        assertEquals(5, scaled.getWidth());
        assertEquals(5, scaled.getHeight());
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                assertEquals(0xAABBCC, scaled.getRGB(x, y));
            }
        }
    }

    @Test
    @DisplayName("scale: to same size preserves interior pixels of linear gradient")
    void testScaleSameSize() {
        // Mitchell-Netravali reproduces linear functions exactly at
        // interior pixels in theory, but two-pass floating-point
        // arithmetic may introduce ±1 rounding per channel.
        int w = 20, h = 20;
        ImageRGB image = new ImageRGB(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // Keep values safely within [0, 255]: max = 19*5 + 19*3 = 152
                int g = x * 5 + y * 3;
                image.setRGB(x, y, g << 8);
            }
        }

        ImageRGB scaled = image.scale(w, h);
        // Check only interior pixels (skip 2-pixel border = support radius)
        for (int y = 2; y < h - 2; y++) {
            for (int x = 2; x < w - 2; x++) {
                int expected = (image.getRGB(x, y) >>> 8) & 0xFF;
                int actual = (scaled.getRGB(x, y) >>> 8) & 0xFF;
                assertTrue(Math.abs(expected - actual) <= 1,
                        "Interior pixel (" + x + "," + y + ") green channel "
                        + "expected ~" + expected + " but was " + actual);
            }
        }
    }

    @Test
    @DisplayName("scale: scale down reduces dimensions correctly")
    void testScaleDown() {
        ImageRGB image = new ImageRGB(100, 100);
        image.fillRect(0, 0, 100, 100, 0x112233);

        ImageRGB scaled = image.scale(25, 25);
        assertEquals(25, scaled.getWidth());
        assertEquals(25, scaled.getHeight());
        for (int y = 0; y < 25; y++) {
            for (int x = 0; x < 25; x++) {
                assertEquals(0x112233, scaled.getRGB(x, y));
            }
        }
    }

    @Test
    @DisplayName("scale: non-square scaling")
    void testScaleNonSquare() {
        ImageRGB image = new ImageRGB(20, 10);
        image.fillRect(0, 0, 20, 10, 0xFFFFFF);

        ImageRGB scaled = image.scale(40, 5);
        assertEquals(40, scaled.getWidth());
        assertEquals(5, scaled.getHeight());
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 40; x++) {
                assertEquals(0xFFFFFF, scaled.getRGB(x, y));
            }
        }
    }

    @Test
    @DisplayName("scale: large image uses parallel path")
    void testScaleLargeImage() {
        // 200*200 = 40,000 pixels exceeds PARALLEL_THRESHOLD (10,000)
        ImageRGB image = new ImageRGB(200, 200);
        image.fillRect(0, 0, 200, 200, 0x445566);

        ImageRGB scaled = image.scale(100, 100);
        assertEquals(100, scaled.getWidth());
        assertEquals(100, scaled.getHeight());
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                assertEquals(0x445566, scaled.getRGB(x, y));
            }
        }
    }

    @Test
    @DisplayName("scale: preserves channel independence")
    void testScaleChannelIndependence() {
        // Pure red image
        ImageRGB red = new ImageRGB(10, 10);
        red.fillRect(0, 0, 10, 10, 0xFF0000);
        ImageRGB scaledRed = red.scale(5, 5);
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                int pixel = scaledRed.getRGB(x, y);
                assertEquals(0xFF, (pixel >>> 16) & 0xFF, "Red channel");
                assertEquals(0, (pixel >>> 8) & 0xFF, "Green channel");
                assertEquals(0, pixel & 0xFF, "Blue channel");
            }
        }
    }

    // ── rotate ──────────────────────────────────────────────────

    @Test
    @DisplayName("rotate: clockwise swaps dimensions")
    void testRotateClockwiseDimensions() {
        ImageRGB image = new ImageRGB(10, 20);
        ImageRGB rotated = image.rotate(1);
        assertEquals(20, rotated.getWidth());
        assertEquals(10, rotated.getHeight());
    }

    @Test
    @DisplayName("rotate: counter-clockwise swaps dimensions")
    void testRotateCounterClockwiseDimensions() {
        ImageRGB image = new ImageRGB(10, 20);
        ImageRGB rotated = image.rotate(3);
        assertEquals(20, rotated.getWidth());
        assertEquals(10, rotated.getHeight());
    }

    @Test
    @DisplayName("rotate: clockwise pixel mapping is correct")
    void testRotateClockwisePixels() {
        // 3x2 image (W=3, H=2)
        ImageRGB image = new ImageRGB(3, 2);
        // Row 0: 1  2  3
        // Row 1: 4  5  6
        image.setRGB(0, 0, 1);
        image.setRGB(1, 0, 2);
        image.setRGB(2, 0, 3);
        image.setRGB(0, 1, 4);
        image.setRGB(1, 1, 5);
        image.setRGB(2, 1, 6);

        // After 90° clockwise (new W=2, H=3):
        // Row 0: 4  1
        // Row 1: 5  2
        // Row 2: 6  3
        ImageRGB rotated = image.rotate(1);
        assertEquals(2, rotated.getWidth());
        assertEquals(3, rotated.getHeight());
        assertEquals(4, rotated.getRGB(0, 0));
        assertEquals(1, rotated.getRGB(1, 0));
        assertEquals(5, rotated.getRGB(0, 1));
        assertEquals(2, rotated.getRGB(1, 1));
        assertEquals(6, rotated.getRGB(0, 2));
        assertEquals(3, rotated.getRGB(1, 2));
    }

    @Test
    @DisplayName("rotate: counter-clockwise pixel mapping is correct")
    void testRotateCounterClockwisePixels() {
        // 3x2 image (W=3, H=2)
        ImageRGB image = new ImageRGB(3, 2);
        image.setRGB(0, 0, 1);
        image.setRGB(1, 0, 2);
        image.setRGB(2, 0, 3);
        image.setRGB(0, 1, 4);
        image.setRGB(1, 1, 5);
        image.setRGB(2, 1, 6);

        // After 90° counter-clockwise (new W=2, H=3):
        // Row 0: 3  6
        // Row 1: 2  5
        // Row 2: 1  4
        ImageRGB rotated = image.rotate(3);
        assertEquals(2, rotated.getWidth());
        assertEquals(3, rotated.getHeight());
        assertEquals(3, rotated.getRGB(0, 0));
        assertEquals(6, rotated.getRGB(1, 0));
        assertEquals(2, rotated.getRGB(0, 1));
        assertEquals(5, rotated.getRGB(1, 1));
        assertEquals(1, rotated.getRGB(0, 2));
        assertEquals(4, rotated.getRGB(1, 2));
    }

    @Test
    @DisplayName("rotate: four clockwise rotations return to original")
    void testRotateFourTimesIdentity() {
        ImageRGB image = new ImageRGB(4, 3);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 4; x++) {
                image.setRGB(x, y, x * 100 + y);
            }
        }

        ImageRGB result = image;
        for (int i = 0; i < 4; i++) {
            result = result.rotate(1);
        }

        assertEquals(image.getWidth(), result.getWidth());
        assertEquals(image.getHeight(), result.getHeight());
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                assertEquals(image.getRGB(x, y), result.getRGB(x, y),
                        "Pixel (" + x + "," + y + ") should match original after 4 rotations");
            }
        }
    }

    @Test
    @DisplayName("rotate: clockwise then counter-clockwise is identity")
    void testRotateClockwiseThenCounterClockwise() {
        ImageRGB image = new ImageRGB(5, 3);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 5; x++) {
                image.setRGB(x, y, x + y * 10);
            }
        }

        ImageRGB result = image.rotate(1).rotate(3);

        assertEquals(image.getWidth(), result.getWidth());
        assertEquals(image.getHeight(), result.getHeight());
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                assertEquals(image.getRGB(x, y), result.getRGB(x, y));
            }
        }
    }

    @Test
    @DisplayName("rotate: 1x1 image stays the same")
    void testRotate1x1() {
        ImageRGB image = new ImageRGB(1, 1);
        image.setRGB(0, 0, 0xABCDEF);

        ImageRGB cw = image.rotate(1);
        assertEquals(1, cw.getWidth());
        assertEquals(1, cw.getHeight());
        assertEquals(0xABCDEF, cw.getRGB(0, 0));

        ImageRGB ccw = image.rotate(3);
        assertEquals(1, ccw.getWidth());
        assertEquals(1, ccw.getHeight());
        assertEquals(0xABCDEF, ccw.getRGB(0, 0));
    }

    @Test
    @DisplayName("rotate: square image clockwise")
    void testRotateSquareClockwise() {
        ImageRGB image = new ImageRGB(3, 3);
        // 1 2 3
        // 4 5 6
        // 7 8 9
        image.setRGB(0, 0, 1);
        image.setRGB(1, 0, 2);
        image.setRGB(2, 0, 3);
        image.setRGB(0, 1, 4);
        image.setRGB(1, 1, 5);
        image.setRGB(2, 1, 6);
        image.setRGB(0, 2, 7);
        image.setRGB(1, 2, 8);
        image.setRGB(2, 2, 9);

        // After 90° CW:
        // 7 4 1
        // 8 5 2
        // 9 6 3
        ImageRGB rotated = image.rotate(1);
        assertEquals(3, rotated.getWidth());
        assertEquals(3, rotated.getHeight());
        assertEquals(7, rotated.getRGB(0, 0));
        assertEquals(4, rotated.getRGB(1, 0));
        assertEquals(1, rotated.getRGB(2, 0));
        assertEquals(8, rotated.getRGB(0, 1));
        assertEquals(5, rotated.getRGB(1, 1));
        assertEquals(2, rotated.getRGB(2, 1));
        assertEquals(9, rotated.getRGB(0, 2));
        assertEquals(6, rotated.getRGB(1, 2));
        assertEquals(3, rotated.getRGB(2, 2));
    }

    @Test
    @DisplayName("rotate: 180 degrees preserves dimensions and maps pixels correctly")
    void testRotate180Degrees() {
        // 3x2 image (W=3, H=2)
        ImageRGB image = new ImageRGB(3, 2);
        // Row 0: 1  2  3
        // Row 1: 4  5  6
        image.setRGB(0, 0, 1);
        image.setRGB(1, 0, 2);
        image.setRGB(2, 0, 3);
        image.setRGB(0, 1, 4);
        image.setRGB(1, 1, 5);
        image.setRGB(2, 1, 6);

        // After 180° rotation (new W=3, H=2):
        // Row 0: 6  5  4
        // Row 1: 3  2  1
        ImageRGB rotated = image.rotate(2);
        assertEquals(3, rotated.getWidth());
        assertEquals(2, rotated.getHeight());
        assertEquals(6, rotated.getRGB(0, 0));
        assertEquals(5, rotated.getRGB(1, 0));
        assertEquals(4, rotated.getRGB(2, 0));
        assertEquals(3, rotated.getRGB(0, 1));
        assertEquals(2, rotated.getRGB(1, 1));
        assertEquals(1, rotated.getRGB(2, 1));
    }

    @Test
    @DisplayName("rotate: negative value is equivalent to counter-clockwise")
    void testRotateNegativeValue() {
        // A single 90° counter-clockwise rotation (rotate(3)) should equal rotate(-1)
        ImageRGB image = new ImageRGB(3, 2);
        image.setRGB(0, 0, 1);
        image.setRGB(1, 0, 2);
        image.setRGB(2, 0, 3);
        image.setRGB(0, 1, 4);
        image.setRGB(1, 1, 5);
        image.setRGB(2, 1, 6);

        ImageRGB expected = image.rotate(3);
        ImageRGB actual = image.rotate(-1);

        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                assertEquals(expected.getRGB(x, y), actual.getRGB(x, y),
                        "Pixel (" + x + "," + y + ") should match");
            }
        }
    }
}
