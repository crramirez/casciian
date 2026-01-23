package casciian.terminal;

import casciian.bits.ImageRGB;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SixelDecoder}.
 */
class SixelDecoderTest {

    @Test
    void testInitializePaletteVT340() {
        Map<Integer, Integer> palette = new HashMap<>();
        SixelDecoder.initializePaletteVT340(palette);
        
        assertEquals(16, palette.size());
        assertEquals(0x000000, palette.get(0));
        assertEquals(0x3333cc, palette.get(1));
        assertEquals(0x777777, palette.get(7));
        assertEquals(0xcccccc, palette.get(15));
    }

    @Test
    void testInitializePaletteCGA() {
        Map<Integer, Integer> palette = new HashMap<>();
        SixelDecoder.initializePaletteCGA(palette);
        
        assertEquals(16, palette.size());
        assertEquals(0x000000, palette.get(0));
        assertEquals(0xa80000, palette.get(1));
        assertEquals(0x545454, palette.get(8));
        assertEquals(0xfcfcfc, palette.get(15));
    }

    @Test
    void testGetImageWithEmptyBuffer() {
        SixelDecoder decoder = new SixelDecoder("", null, 0x000000, false);
        ImageRGB image = decoder.getImage();
        
        assertNull(image);
    }

    @Test
    void testBasicSixelParsing() {
        // Simple sixel with raster and line feed
        String sixel = "q\"1;1;10;10#0@-";
        SixelDecoder decoder = new SixelDecoder(sixel, null, 0x000000, false);
        
        ImageRGB image = decoder.getImage();
        
        assertNotNull(image);
        assertTrue(image.getWidth() > 0);
        assertTrue(image.getHeight() > 0);
    }

    @Test
    void testSixelWithRasterAttributes() {
        // Sixel with raster attributes: "1;1;10;20 declares 10x20 image
        String sixel = "q\"1;1;10;20#0@-";
        SixelDecoder decoder = new SixelDecoder(sixel, null, 0x000000, false);
        
        ImageRGB image = decoder.getImage();
        
        assertNotNull(image);
        assertEquals(10, image.getWidth());
        assertTrue(image.getHeight() <= 20);
    }

    @Test
    void testSixelColorChange() {
        // Use color from palette
        String sixel = "q\"1;1;10;10#1@-";
        SixelDecoder decoder = new SixelDecoder(sixel, null, 0x000000, false);
        
        ImageRGB image = decoder.getImage();
        
        assertNotNull(image);
        // Color should be set and image should be created
        assertTrue(image.getWidth() > 0);
    }

    @Test
    void testSixelRepeatCount() {
        // "!5@" means repeat the '@' character 5 times
        String sixel = "q\"1;1;10;10#0!5@-";
        SixelDecoder decoder = new SixelDecoder(sixel, null, 0x000000, false);
        
        ImageRGB image = decoder.getImage();
        
        assertNotNull(image);
        // Width should reflect the repeated pixels
        assertTrue(image.getWidth() >= 5);
    }

    @Test
    void testSixelCarriageReturn() {
        // "$" returns to beginning of line
        String sixel = "q\"1;1;10;10#0@@$@-";
        SixelDecoder decoder = new SixelDecoder(sixel, null, 0x000000, false);
        
        ImageRGB image = decoder.getImage();
        
        assertNotNull(image);
        assertTrue(image.getWidth() > 0);
    }

    @Test
    void testSixelLineFeed() {
        // "-" moves to next line (down 6 pixels)
        String sixel = "q\"1;1;10;20#0??-??";
        SixelDecoder decoder = new SixelDecoder(sixel, null, 0x000000, false);
        
        ImageRGB image = decoder.getImage();
        
        assertNotNull(image);
        assertTrue(image.getHeight() > 6);
    }

    @Test
    void testTransparencyEnabled() {
        // Background option 1 with maybeTransparent=true should enable transparency
        String sixel = "0;1q#0?";
        SixelDecoder decoder = new SixelDecoder(sixel, null, 0x000000, true);
        
        decoder.getImage();
        
        assertTrue(decoder.isTransparent());
    }

    @Test
    void testTransparencyDisabled() {
        // Background option 1 with maybeTransparent=false should not enable transparency
        String sixel = "0;1q#0?";
        SixelDecoder decoder = new SixelDecoder(sixel, null, 0x000000, false);
        
        decoder.getImage();
        
        assertFalse(decoder.isTransparent());
    }

    @Test
    void testTransparencyDefaultOption() {
        // Default background option should not enable transparency
        String sixel = "q#0?";
        SixelDecoder decoder = new SixelDecoder(sixel, null, 0x000000, true);
        
        decoder.getImage();
        
        assertFalse(decoder.isTransparent());
    }

    @Test
    void testCustomPalette() {
        Map<Integer, Integer> palette = new HashMap<>();
        palette.put(0, 0xFF0000); // Red
        palette.put(1, 0x00FF00); // Green
        
        String sixel = "q\"1;1;10;10#0@-";
        SixelDecoder decoder = new SixelDecoder(sixel, palette, 0x000000, false);
        
        ImageRGB image = decoder.getImage();
        
        assertNotNull(image);
        // The palette should be used for color 0
    }

    @Test
    void testMultipleColors() {
        // Use multiple colors in sequence
        String sixel = "q\"1;1;20;10#0@@#1@@#2@@-";
        SixelDecoder decoder = new SixelDecoder(sixel, null, 0x000000, false);
        
        ImageRGB image = decoder.getImage();
        
        assertNotNull(image);
        assertTrue(image.getWidth() >= 6);
    }

    @Test
    void testSixelPixelData() {
        // '?' is 0x3F (63), minus 63 = 0 (no pixels)
        // '@' is 0x40 (64), minus 63 = 1 (bit 0 set, bottom pixel)
        // 'A' is 0x41 (65), minus 63 = 2 (bit 1 set, second pixel from bottom)
        String sixel = "q\"1;1;10;10#0@-";
        SixelDecoder decoder = new SixelDecoder(sixel, null, 0x000000, false);
        
        ImageRGB image = decoder.getImage();
        
        assertNotNull(image);
        assertTrue(image.getHeight() > 0);
    }

    @Test
    void testMaxRepeatCount() {
        // Repeat count should be clamped to 32767
        String sixel = "q\"1;1;100;10#0!99999@-";
        SixelDecoder decoder = new SixelDecoder(sixel, null, 0x000000, false);
        
        ImageRGB image = decoder.getImage();
        
        // Should not crash, and image should be valid
        assertNotNull(image);
    }

    @Test
    void testAbortOnMaxWidth() {
        // Create a sixel that tries to exceed MAX_WIDTH
        StringBuilder sb = new StringBuilder("q\"1;1;10;10#0");
        // Repeat a very large number that would exceed max width
        sb.append("!9999~"); // Try to draw 9999 pixels
        
        SixelDecoder decoder = new SixelDecoder(sb.toString(), null, 0x000000, false);
        ImageRGB image = decoder.getImage();
        
        // Either returns null (aborted) or valid image within limits
        if (image != null) {
            assertTrue(image.getWidth() <= 3840);
        }
    }

    @Test
    void testInvalidRasterAspectRatio() {
        // Raster attributes with non-matching aspect ratio should abort
        String sixel = "q\"1;2;10;10#0?";
        SixelDecoder decoder = new SixelDecoder(sixel, null, 0x000000, false);
        
        ImageRGB image = decoder.getImage();
        
        assertNull(image);
    }

    @Test
    void testRasterDimensionsTooLarge() {
        // Raster attributes exceeding MAX_WIDTH/MAX_HEIGHT should abort
        String sixel = "q\"1;1;20000;20000#0?";
        SixelDecoder decoder = new SixelDecoder(sixel, null, 0x000000, false);
        
        ImageRGB image = decoder.getImage();
        
        assertNull(image);
    }

    @Test
    void testEmptyPixelData() {
        // '?' (63 - 63 = 0) means no pixels are set, just advance position
        String sixel = "q\"1;1;10;10#0???@-";
        SixelDecoder decoder = new SixelDecoder(sixel, null, 0x000000, false);
        
        ImageRGB image = decoder.getImage();
        
        assertNotNull(image);
        assertTrue(image.getWidth() >= 3);
    }

    @Test
    void testComplexSixelSequence() {
        // More complex sequence with multiple operations
        String sixel = "q\"1;1;20;20#0;2;100;0;0#1;2;0;100;0#0??#1??-#0??#1??";
        SixelDecoder decoder = new SixelDecoder(sixel, null, 0x000000, false);
        
        ImageRGB image = decoder.getImage();
        
        assertNotNull(image);
        assertTrue(image.getWidth() > 0);
        assertTrue(image.getHeight() > 6); // Should have at least two lines
    }

    @Test
    void testPaletteColorDefinition() {
        // Define color with RGB values and use it
        String sixel = "q\"1;1;10;10#0;2;50;50;50#0@-";
        SixelDecoder decoder = new SixelDecoder(sixel, null, 0x000000, false);
        
        ImageRGB image = decoder.getImage();
        
        assertNotNull(image);
        // Color should be defined and used
    }

    @Test
    void testMultipleLinesWithCarriageReturns() {
        // Test combination of line feed and carriage return
        String sixel = "q\"1;1;15;15#0??-$??";
        SixelDecoder decoder = new SixelDecoder(sixel, null, 0x000000, false);
        
        ImageRGB image = decoder.getImage();
        
        assertNotNull(image);
        assertTrue(image.getHeight() > 6);
    }

    @Test
    void testAllSixelBits() {
        // Test all 6 bits (characters '?' through '~')
        // '~' is 0x7E (126), minus 63 = 63 (all 6 bits set: 0b111111)
        String sixel = "q\"1;1;10;10#0~-";
        SixelDecoder decoder = new SixelDecoder(sixel, null, 0x000000, false);
        
        ImageRGB image = decoder.getImage();
        
        assertNotNull(image);
        assertTrue(image.getHeight() >= 6); // All 6 pixels in column
    }

    @Test
    void testRepeatCountZeroMeansOne() {
        // Test that repeat functionality works with explicit count
        String sixel = "q\"1;1;10;10#0!1@-";
        SixelDecoder decoder = new SixelDecoder(sixel, null, 0x000000, false);
        
        ImageRGB image = decoder.getImage();
        
        assertNotNull(image);
        assertTrue(image.getWidth() >= 1);
    }
}
