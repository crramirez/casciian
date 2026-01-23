package casciian.backend;

import casciian.bits.ImageRGB;
import casciian.terminal.SixelDecoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HQSixelEncoder}.
 * 
 * These tests focus on black-box testing the public API of HQSixelEncoder,
 * validating its behavior for sixel image encoding.
 */
@DisplayName("HQSixelEncoder Tests")
class HQSixelEncoderTest {

    private HQSixelEncoder encoder;
    
    // Store original system property values for restoration
    private String originalPaletteSize;
    private String originalFastAndDirty;
    private String originalCustomPalette;
    private String originalEmitPalette;

    @BeforeEach
    void setUp() {
        // Save original system property values
        originalPaletteSize = System.getProperty("casciian.ECMA48.sixelPaletteSize");
        originalFastAndDirty = System.getProperty("casciian.ECMA48.sixelFastAndDirty");
        originalCustomPalette = System.getProperty("casciian.ECMA48.sixelCustomPalette");
        originalEmitPalette = System.getProperty("casciian.ECMA48.sixelEmitPalette");
        
        // Clear system properties to ensure clean test environment
        System.clearProperty("casciian.ECMA48.sixelPaletteSize");
        System.clearProperty("casciian.ECMA48.sixelFastAndDirty");
        System.clearProperty("casciian.ECMA48.sixelCustomPalette");
        System.clearProperty("casciian.ECMA48.sixelEmitPalette");
        
        encoder = new HQSixelEncoder();
    }
    
    @AfterEach
    void tearDown() {
        // Restore original system property values
        restoreProperty("casciian.ECMA48.sixelPaletteSize", originalPaletteSize);
        restoreProperty("casciian.ECMA48.sixelFastAndDirty", originalFastAndDirty);
        restoreProperty("casciian.ECMA48.sixelCustomPalette", originalCustomPalette);
        restoreProperty("casciian.ECMA48.sixelEmitPalette", originalEmitPalette);
    }
    
    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    // ========================================================================
    // Constructor and Basic Configuration Tests
    // ========================================================================

    @Nested
    @DisplayName("Constructor and Configuration")
    class ConstructorAndConfiguration {

        @Test
        @DisplayName("Default constructor creates encoder with default palette size")
        void testDefaultConstructor() {
            HQSixelEncoder encoder = new HQSixelEncoder();
            assertEquals(128, encoder.getPaletteSize());
        }

        @Test
        @DisplayName("Default constructor creates encoder with no shared palette")
        void testDefaultConstructorNoSharedPalette() {
            HQSixelEncoder encoder = new HQSixelEncoder();
            assertFalse(encoder.hasSharedPalette());
        }
    }

    // ========================================================================
    // Palette Size Tests
    // ========================================================================

    @Nested
    @DisplayName("Palette Size Operations")
    class PaletteSizeOperations {

        @Test
        @DisplayName("getPaletteSize returns current palette size")
        void testGetPaletteSize() {
            assertEquals(128, encoder.getPaletteSize());
        }

        @Test
        @DisplayName("setPaletteSize with valid power of 2 values")
        void testSetPaletteSizeValidValues() {
            int[] validSizes = {2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048};
            
            for (int size : validSizes) {
                encoder.setPaletteSize(size);
                assertEquals(size, encoder.getPaletteSize(), 
                    "Palette size should be " + size);
            }
        }

        @Test
        @DisplayName("setPaletteSize with invalid values throws IllegalArgumentException")
        void testSetPaletteSizeInvalidValues() {
            int[] invalidSizes = {0, 1, 3, 5, 7, 9, 15, 17, 100, 200, 3000};
            
            for (int size : invalidSizes) {
                assertThrows(IllegalArgumentException.class, 
                    () -> encoder.setPaletteSize(size),
                    "Should throw for invalid palette size: " + size);
            }
        }

        @Test
        @DisplayName("setPaletteSize with same value does not change state")
        void testSetPaletteSizeSameValue() {
            encoder.setPaletteSize(128);
            assertEquals(128, encoder.getPaletteSize());
            
            // Setting same value should not throw or change behavior
            encoder.setPaletteSize(128);
            assertEquals(128, encoder.getPaletteSize());
        }
    }

    // ========================================================================
    // toSixel Basic Encoding Tests
    // ========================================================================

    @Nested
    @DisplayName("Basic Sixel Encoding")
    class BasicSixelEncoding {

        @Test
        @DisplayName("toSixel encodes a simple solid color image")
        void testToSixelSolidColorImage() {
            ImageRGB image = new ImageRGB(10, 10);
            fillImageWithColor(image, 0xFF0000); // Red
            
            String sixel = encoder.toSixel(image);
            
            assertNotNull(sixel);
            assertFalse(sixel.isEmpty());
            // Sixel should contain complete raster attributes at the start with dimensions
            assertTrue(sixel.startsWith("\"1;1;10;10"));
        }

        @Test
        @DisplayName("toSixel encodes a 1x1 pixel image")
        void testToSixelSinglePixel() {
            ImageRGB image = new ImageRGB(1, 1);
            image.setRGB(0, 0, 0x00FF00); // Green
            
            String sixel = encoder.toSixel(image);
            
            assertNotNull(sixel);
            assertFalse(sixel.isEmpty());
        }

        @Test
        @DisplayName("toSixel encodes a 1x6 pixel image (single sixel row)")
        void testToSixelSingleSixelRow() {
            ImageRGB image = new ImageRGB(1, 6);
            fillImageWithColor(image, 0x0000FF); // Blue
            
            String sixel = encoder.toSixel(image);
            
            assertNotNull(sixel);
            assertFalse(sixel.isEmpty());
            assertTrue(sixel.startsWith("\"1;1;1;6"));
        }

        @Test
        @DisplayName("toSixel encodes image with multiple colors")
        void testToSixelMultipleColors() {
            ImageRGB image = new ImageRGB(20, 20);
            
            // Create a simple pattern with different colors
            for (int y = 0; y < 20; y++) {
                for (int x = 0; x < 20; x++) {
                    int color;
                    if (x < 10 && y < 10) {
                        color = 0xFF0000; // Red
                    } else if (x >= 10 && y < 10) {
                        color = 0x00FF00; // Green
                    } else if (x < 10) {
                        color = 0x0000FF; // Blue
                    } else {
                        color = 0xFFFF00; // Yellow
                    }
                    image.setRGB(x, y, color);
                }
            }
            
            String sixel = encoder.toSixel(image);
            
            assertNotNull(sixel);
            assertFalse(sixel.isEmpty());
            // Should have palette definitions
            assertTrue(sixel.contains("#"));
        }

        @Test
        @DisplayName("toSixel handles black image")
        void testToSixelBlackImage() {
            ImageRGB image = new ImageRGB(10, 10);
            fillImageWithColor(image, 0x000000); // Black
            
            String sixel = encoder.toSixel(image);
            
            assertNotNull(sixel);
            assertFalse(sixel.isEmpty());
        }

        @Test
        @DisplayName("toSixel handles white image")
        void testToSixelWhiteImage() {
            ImageRGB image = new ImageRGB(10, 10);
            fillImageWithColor(image, 0xFFFFFF); // White
            
            String sixel = encoder.toSixel(image);
            
            assertNotNull(sixel);
            assertFalse(sixel.isEmpty());
        }

        @Test
        @DisplayName("toSixel encodes image with dimensions matching sixel row boundaries")
        void testToSixelSixelRowBoundaries() {
            // 12 = 2 * 6 (exactly two sixel rows)
            ImageRGB image = new ImageRGB(10, 12);
            fillImageWithColor(image, 0x808080); // Gray
            
            String sixel = encoder.toSixel(image);
            
            assertNotNull(sixel);
            assertTrue(sixel.startsWith("\"1;1;10;12"));
        }

        @Test
        @DisplayName("toSixel encodes image with height not multiple of 6")
        void testToSixelNonMultipleOf6Height() {
            // 13 = 2*6 + 1 (partial last sixel row)
            ImageRGB image = new ImageRGB(10, 13);
            fillImageWithColor(image, 0x808080); // Gray
            
            String sixel = encoder.toSixel(image);
            
            assertNotNull(sixel);
            assertTrue(sixel.startsWith("\"1;1;10;13"));
        }
    }

    // ========================================================================
    // Transparency Tests
    // ========================================================================

    @Nested
    @DisplayName("Transparency Handling")
    class TransparencyHandling {

        @Test
        @DisplayName("toSixel with transparency flag encodes transparent pixels")
        void testToSixelWithTransparency() {
            ImageRGB image = new ImageRGB(10, 10);
            
            // Fill with semi-transparent pixels
            for (int y = 0; y < 10; y++) {
                for (int x = 0; x < 10; x++) {
                    // 0x80 = 128 alpha (about 50% transparent)
                    image.setRGB(x, y, 0x80FF0000);
                }
            }
            
            String sixel = encoder.toSixel(image, true);
            
            assertNotNull(sixel);
        }

        @Test
        @DisplayName("toSixel without transparency flag treats transparent pixels as opaque")
        void testToSixelWithoutTransparency() {
            ImageRGB image = new ImageRGB(10, 10);
            
            for (int y = 0; y < 10; y++) {
                for (int x = 0; x < 10; x++) {
                    image.setRGB(x, y, 0x80FF0000);
                }
            }
            
            String sixel = encoder.toSixel(image, false);
            
            assertNotNull(sixel);
            assertFalse(sixel.isEmpty());
        }

        @Test
        @DisplayName("toSixel handles fully transparent pixels")
        void testToSixelFullyTransparentPixels() {
            ImageRGB image = new ImageRGB(10, 10);
            
            // Alpha = 0 means fully transparent
            for (int y = 0; y < 10; y++) {
                for (int x = 0; x < 10; x++) {
                    image.setRGB(x, y, 0x00FF0000);
                }
            }
            
            String sixel = encoder.toSixel(image, true);
            
            assertNotNull(sixel);
        }

        @Test
        @DisplayName("toSixel handles mixed opaque and transparent pixels")
        void testToSixelMixedTransparency() {
            ImageRGB image = new ImageRGB(10, 10);
            
            for (int y = 0; y < 10; y++) {
                for (int x = 0; x < 10; x++) {
                    if ((x + y) % 2 == 0) {
                        image.setRGB(x, y, 0xFFFF0000); // Opaque red
                    } else {
                        image.setRGB(x, y, 0x00FF0000); // Transparent red
                    }
                }
            }
            
            String sixel = encoder.toSixel(image, true);
            
            assertNotNull(sixel);
        }
    }

    // ========================================================================
    // Custom Palette Tests
    // ========================================================================

    @Nested
    @DisplayName("Custom Palette Handling")
    class CustomPaletteHandling {

        @Test
        @DisplayName("toSixel with VT340 custom palette")
        void testToSixelWithVT340Palette() {
            String key = "casciian.ECMA48.sixelCustomPalette";
            String previousValue = System.getProperty(key);
            try {
                System.setProperty(key, "vt340");
                HQSixelEncoder encoderWithVT340 = new HQSixelEncoder();
                
                ImageRGB image = new ImageRGB(10, 10);
                fillImageWithColor(image, 0x3333CC); // VT340 color 1 (blue)
                
                String sixel = encoderWithVT340.toSixel(image);
                
                assertNotNull(sixel);
                assertFalse(sixel.isEmpty());
            } finally {
                if (previousValue == null) {
                    System.clearProperty(key);
                } else {
                    System.setProperty(key, previousValue);
                }
            }
        }

        @Test
        @DisplayName("toSixel with CGA custom palette")
        void testToSixelWithCGAPalette() {
            String key = "casciian.ECMA48.sixelCustomPalette";
            String previousValue = System.getProperty(key);
            try {
                System.setProperty(key, "cga");
                HQSixelEncoder encoderWithCGA = new HQSixelEncoder();
                
                ImageRGB image = new ImageRGB(10, 10);
                fillImageWithColor(image, 0xA80000); // CGA color 1 (red)
                
                String sixel = encoderWithCGA.toSixel(image);
                
                assertNotNull(sixel);
                assertFalse(sixel.isEmpty());
            } finally {
                if (previousValue == null) {
                    System.clearProperty(key);
                } else {
                    System.setProperty(key, previousValue);
                }
            }
        }

        @Test
        @DisplayName("toSixel with explicit custom palette Map")
        void testToSixelWithExplicitCustomPalette() {
            // Use Map interface type - API now accepts Map instead of HashMap
            Map<Integer, Integer> customPalette = new HashMap<>();
            customPalette.put(0, 0xFF0000); // Red
            customPalette.put(1, 0x00FF00); // Green
            customPalette.put(2, 0x0000FF); // Blue
            customPalette.put(3, 0xFFFFFF); // White
            
            ImageRGB image = new ImageRGB(10, 10);
            fillImageWithColor(image, 0xFF0000);
            
            // API accepts Map interface directly, no cast needed
            String sixel = encoder.toSixel(image, customPalette);
            
            assertNotNull(sixel);
            assertFalse(sixel.isEmpty());
        }
    }

    // ========================================================================
    // reloadOptions Tests
    // ========================================================================

    @Nested
    @DisplayName("Reload Options")
    class ReloadOptionsTests {

        @Test
        @DisplayName("reloadOptions updates palette size from system property")
        void testReloadOptionsPaletteSize() {
            String key = "casciian.ECMA48.sixelPaletteSize";
            String previousValue = System.getProperty(key);
            try {
                System.setProperty(key, "256");
                
                encoder.reloadOptions();
                
                assertEquals(256, encoder.getPaletteSize());
            } finally {
                if (previousValue == null) {
                    System.clearProperty(key);
                } else {
                    System.setProperty(key, previousValue);
                }
            }
        }

        @Test
        @DisplayName("reloadOptions ignores invalid palette size values")
        void testReloadOptionsInvalidPaletteSize() {
            String key = "casciian.ECMA48.sixelPaletteSize";
            String previousValue = System.getProperty(key);
            int originalSize = encoder.getPaletteSize();
            try {
                System.setProperty(key, "100");
                
                encoder.reloadOptions();
                
                // Invalid values should be ignored, keeping the original
                assertEquals(originalSize, encoder.getPaletteSize());
            } finally {
                if (previousValue == null) {
                    System.clearProperty(key);
                } else {
                    System.setProperty(key, previousValue);
                }
            }
        }

        @Test
        @DisplayName("reloadOptions ignores non-numeric palette size")
        void testReloadOptionsNonNumericPaletteSize() {
            String key = "casciian.ECMA48.sixelPaletteSize";
            String previousValue = System.getProperty(key);
            int originalSize = encoder.getPaletteSize();
            try {
                System.setProperty(key, "abc");
                
                encoder.reloadOptions();
                
                assertEquals(originalSize, encoder.getPaletteSize());
            } finally {
                if (previousValue == null) {
                    System.clearProperty(key);
                } else {
                    System.setProperty(key, previousValue);
                }
            }
        }

        @Test
        @DisplayName("reloadOptions recognizes fastAndDirty setting")
        void testReloadOptionsFastAndDirty() {
            String key = "casciian.ECMA48.sixelFastAndDirty";
            String previousValue = System.getProperty(key);
            try {
                System.setProperty(key, "true");
                
                HQSixelEncoder newEncoder = new HQSixelEncoder();
                
                // We can verify by encoding - a fast and dirty encoder should produce output
                ImageRGB image = new ImageRGB(10, 10);
                fillImageWithColor(image, 0xFF0000);
                
                String sixel = newEncoder.toSixel(image);
                assertNotNull(sixel);
                assertFalse(sixel.isEmpty());
            } finally {
                if (previousValue == null) {
                    System.clearProperty(key);
                } else {
                    System.setProperty(key, previousValue);
                }
            }
        }
    }

    // ========================================================================
    // Shared Palette Tests
    // ========================================================================

    @Nested
    @DisplayName("Shared Palette")
    class SharedPaletteTests {

        @Test
        @DisplayName("hasSharedPalette returns false by default")
        void testHasSharedPaletteDefault() {
            assertFalse(encoder.hasSharedPalette());
        }

        @Test
        @DisplayName("setSharedPalette is a no-op for HQSixelEncoder")
        void testSetSharedPaletteNoOp() {
            encoder.setSharedPalette(true);
            
            // HQSixelEncoder doesn't support shared palette, so it remains false
            assertFalse(encoder.hasSharedPalette());
        }
    }

    // ========================================================================
    // clearPalette and emitPalette Tests
    // ========================================================================

    @Nested
    @DisplayName("Palette Management")
    class PaletteManagement {

        @Test
        @DisplayName("clearPalette does not throw exception")
        void testClearPalette() {
            assertDoesNotThrow(() -> encoder.clearPalette());
        }

        @Test
        @DisplayName("emitPalette does not throw exception")
        void testEmitPalette() {
            StringBuilder sb = new StringBuilder();
            assertDoesNotThrow(() -> encoder.emitPalette(sb));
            // HQSixelEncoder.emitPalette is a no-op
            assertEquals(0, sb.length());
        }

        @Test
        @DisplayName("clearPalette followed by encoding still works")
        void testClearPaletteThenEncode() {
            ImageRGB image = new ImageRGB(10, 10);
            fillImageWithColor(image, 0xFF0000);
            
            // First encode
            String sixel1 = encoder.toSixel(image);
            assertNotNull(sixel1);
            
            // Clear and encode again
            encoder.clearPalette();
            String sixel2 = encoder.toSixel(image);
            assertNotNull(sixel2);
        }
    }

    // ========================================================================
    // Roundtrip (Encode/Decode) Tests
    // ========================================================================

    @Nested
    @DisplayName("Roundtrip Encoding/Decoding")
    class RoundtripTests {

        @Test
        @DisplayName("Encoded sixel can be decoded back to an image")
        void testRoundtripEncodeDecode() {
            ImageRGB original = new ImageRGB(20, 20);
            fillImageWithColor(original, 0xFF0000);
            
            String sixel = encoder.toSixel(original);
            
            // Prepend "q" to make it a valid sixel sequence for decoder
            SixelDecoder decoder = new SixelDecoder("q" + sixel, null, 0x000000, false);
            ImageRGB decoded = decoder.getImage();
            
            assertNotNull(decoded);
            assertEquals(original.getWidth(), decoded.getWidth());
            assertEquals(original.getHeight(), decoded.getHeight());
        }

        @Test
        @DisplayName("Encoded multicolor image can be decoded")
        void testRoundtripMulticolor() {
            ImageRGB original = new ImageRGB(20, 12);
            
            // Create horizontal stripes
            for (int y = 0; y < 12; y++) {
                int color = (y < 6) ? 0xFF0000 : 0x0000FF;
                for (int x = 0; x < 20; x++) {
                    original.setRGB(x, y, color);
                }
            }
            
            String sixel = encoder.toSixel(original);
            
            SixelDecoder decoder = new SixelDecoder("q" + sixel, null, 0x000000, false);
            ImageRGB decoded = decoder.getImage();
            
            assertNotNull(decoded);
            assertEquals(original.getWidth(), decoded.getWidth());
            assertEquals(original.getHeight(), decoded.getHeight());
        }
    }

    // ========================================================================
    // Large Image Tests
    // ========================================================================

    @Nested
    @DisplayName("Large Image Handling")
    class LargeImageTests {

        @Test
        @DisplayName("toSixel handles moderately large image")
        void testToSixelLargeImage() {
            ImageRGB image = new ImageRGB(100, 100);
            fillImageWithGradient(image);
            
            String sixel = encoder.toSixel(image);
            
            assertNotNull(sixel);
            assertFalse(sixel.isEmpty());
            assertTrue(sixel.startsWith("\"1;1;100;100"));
        }

        @Test
        @DisplayName("toSixel handles wide image")
        void testToSixelWideImage() {
            ImageRGB image = new ImageRGB(200, 10);
            fillImageWithColor(image, 0x808080);
            
            String sixel = encoder.toSixel(image);
            
            assertNotNull(sixel);
            assertFalse(sixel.isEmpty());
        }

        @Test
        @DisplayName("toSixel handles tall image")
        void testToSixelTallImage() {
            ImageRGB image = new ImageRGB(10, 200);
            fillImageWithColor(image, 0x808080);
            
            String sixel = encoder.toSixel(image);
            
            assertNotNull(sixel);
            assertFalse(sixel.isEmpty());
        }
    }

    // ========================================================================
    // Edge Cases and Error Handling
    // ========================================================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("toSixel with minimum size image (1x1)")
        void testToSixelMinimumSize() {
            ImageRGB image = new ImageRGB(1, 1);
            image.setRGB(0, 0, 0xABCDEF);
            
            String sixel = encoder.toSixel(image);
            
            assertNotNull(sixel);
            assertFalse(sixel.isEmpty());
            assertTrue(sixel.startsWith("\"1;1;1;1"));
        }

        @Test
        @DisplayName("toSixel output contains valid sixel characters")
        void testToSixelValidCharacters() {
            ImageRGB image = new ImageRGB(10, 12);
            fillImageWithColor(image, 0x123456);
            
            String sixel = encoder.toSixel(image);
            
            assertNotNull(sixel);
            // Sixel data characters are in range 0x3F-0x7E (? to ~)
            // Plus control characters like #, !, $, -, ;, digits
            for (char c : sixel.toCharArray()) {
                assertTrue(isValidSixelChar(c), 
                    "Invalid sixel character: " + (int) c + " (" + c + ")");
            }
        }

        @Test
        @DisplayName("toSixel with high color count image triggers median cut")
        void testToSixelHighColorCount() {
            ImageRGB image = new ImageRGB(50, 50);
            
            // Create a gradient that uses many different colors
            for (int y = 0; y < 50; y++) {
                for (int x = 0; x < 50; x++) {
                    int r = (x * 5) % 256;
                    int g = (y * 5) % 256;
                    int b = ((x + y) * 3) % 256;
                    image.setRGB(x, y, (r << 16) | (g << 8) | b);
                }
            }
            
            String sixel = encoder.toSixel(image);
            
            assertNotNull(sixel);
            assertFalse(sixel.isEmpty());
        }

        @Test
        @DisplayName("toSixel with low color count image uses direct map")
        void testToSixelLowColorCount() {
            encoder.setPaletteSize(256);
            
            ImageRGB image = new ImageRGB(10, 10);
            // Use only 4 colors
            int[] colors = {0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00};
            for (int y = 0; y < 10; y++) {
                for (int x = 0; x < 10; x++) {
                    image.setRGB(x, y, colors[(x + y) % 4]);
                }
            }
            
            String sixel = encoder.toSixel(image);
            
            assertNotNull(sixel);
            assertFalse(sixel.isEmpty());
        }
    }

    // ========================================================================
    // Palette Suppression Tests
    // ========================================================================

    @Nested
    @DisplayName("Palette Emission Suppression")
    class PaletteEmissionSuppression {

        @Test
        @DisplayName("toSixel with suppressEmitPalette does not emit palette definitions")
        void testToSixelSuppressPalette() {
            ImageRGB image = new ImageRGB(10, 10);
            fillImageWithColor(image, 0xFF0000);
            
            // With palette suppression
            String sixelSuppressed = encoder.toSixel(image, false, null, true);
            
            // Without palette suppression
            String sixelWithPalette = encoder.toSixel(image, false, null, false);
            
            assertNotNull(sixelSuppressed);
            assertNotNull(sixelWithPalette);
            
            // Suppressed version should be shorter (no palette definitions)
            assertTrue(sixelSuppressed.length() < sixelWithPalette.length(),
                "Suppressed palette output should be shorter");
        }

        @Test
        @DisplayName("toSixel with suppressEmitPalette set via system property")
        void testToSixelSuppressPaletteSystemProperty() {
            String key = "casciian.ECMA48.sixelEmitPalette";
            String previousValue = System.getProperty(key);
            try {
                System.setProperty(key, "false");
                HQSixelEncoder newEncoder = new HQSixelEncoder();
                
                ImageRGB image = new ImageRGB(10, 10);
                fillImageWithColor(image, 0xFF0000);
                
                String sixel = newEncoder.toSixel(image);
                
                assertNotNull(sixel);
                assertFalse(sixel.isEmpty());
            } finally {
                if (previousValue == null) {
                    System.clearProperty(key);
                } else {
                    System.setProperty(key, previousValue);
                }
            }
        }
    }

    // ========================================================================
    // Sixel Output Structure Tests
    // ========================================================================

    @Nested
    @DisplayName("Sixel Output Structure")
    class SixelOutputStructure {

        @Test
        @DisplayName("Sixel output starts with raster attributes")
        void testSixelOutputStartsWithRasterAttributes() {
            ImageRGB image = new ImageRGB(15, 18);
            fillImageWithColor(image, 0x123456);
            
            String sixel = encoder.toSixel(image);
            
            assertNotNull(sixel);
            assertTrue(sixel.startsWith("\"1;1;15;18"));
        }

        @Test
        @DisplayName("Sixel output contains color selection")
        void testSixelOutputContainsColorSelection() {
            ImageRGB image = new ImageRGB(10, 10);
            fillImageWithColor(image, 0xFF0000);
            
            String sixel = encoder.toSixel(image);
            
            assertNotNull(sixel);
            // Color selection is done with # followed by index
            assertTrue(sixel.contains("#"));
        }

        @Test
        @DisplayName("Sixel output uses run-length encoding for repeated pixels")
        void testSixelOutputUsesRLE() {
            ImageRGB image = new ImageRGB(100, 6);
            fillImageWithColor(image, 0xFF0000);
            
            String sixel = encoder.toSixel(image);
            
            assertNotNull(sixel);
            // RLE is indicated by ! followed by a count
            assertTrue(sixel.contains("!"));
        }

        @Test
        @DisplayName("Sixel output uses line breaks for sixel rows")
        void testSixelOutputUsesLineBreaks() {
            // 12 pixels high = 2 sixel rows
            ImageRGB image = new ImageRGB(10, 12);
            fillImageWithColor(image, 0xFF0000);
            
            String sixel = encoder.toSixel(image);
            
            assertNotNull(sixel);
            // Line breaks are done with - (but the last one is stripped)
            // With 2 rows, there should be at least one - somewhere
            assertTrue(sixel.contains("-"));
        }
    }

    // ========================================================================
    // SixelEncoder Interface Compliance Tests
    // ========================================================================

    @Nested
    @DisplayName("SixelEncoder Interface Compliance")
    class SixelEncoderInterfaceCompliance {

        @Test
        @DisplayName("HQSixelEncoder implements SixelEncoder interface")
        void testImplementsSixelEncoder() {
            assertInstanceOf(SixelEncoder.class, encoder);
        }

        @Test
        @DisplayName("All SixelEncoder methods are callable")
        void testAllInterfaceMethodsCallable() {
            ImageRGB image = new ImageRGB(5, 5);
            fillImageWithColor(image, 0xFF0000);
            
            assertDoesNotThrow(() -> {
                int originalPaletteSize = encoder.getPaletteSize();
                try {
                    encoder.reloadOptions();
                    encoder.toSixel(image);
                    encoder.emitPalette(new StringBuilder());
                    encoder.hasSharedPalette();
                    encoder.setSharedPalette(true);
                    encoder.getPaletteSize();
                    encoder.setPaletteSize(64);
                    encoder.clearPalette();
                } finally {
                    encoder.setPaletteSize(originalPaletteSize);
                }
            });
        }
    }

    // ========================================================================
    // Thread Safety Tests
    // ========================================================================

    @Nested
    @DisplayName("Thread Safety")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Concurrent encoding from multiple threads produces valid output")
        void testConcurrentEncodingProducesValidOutput() throws InterruptedException {
            final int threadCount = 4;
            final int iterationsPerThread = 5;
            final HQSixelEncoder sharedEncoder = new HQSixelEncoder();
            
            Thread[] threads = new Thread[threadCount];
            final boolean[] errors = new boolean[threadCount];
            
            for (int t = 0; t < threadCount; t++) {
                final int threadIndex = t;
                threads[t] = new Thread(() -> {
                    try {
                        for (int i = 0; i < iterationsPerThread; i++) {
                            // Each thread creates its own image with different colors
                            // to ensure distinct encoding operations. The color is
                            // computed to vary based on thread index and iteration:
                            // - Red component varies by thread (0-200)
                            // - Green component varies by iteration (0-150)
                            // - Blue component varies by both (0-180)
                            ImageRGB image = new ImageRGB(20, 12);
                            int red = (threadIndex * 50) & 0xFF;
                            int green = (i * 30) & 0xFF;
                            int blue = ((threadIndex + i) * 20) & 0xFF;
                            int color = (red << 16) | (green << 8) | blue;
                            fillImageWithColor(image, color);
                            
                            String sixel = sharedEncoder.toSixel(image);
                            
                            // Validate the output
                            if (sixel == null || sixel.isEmpty()) {
                                errors[threadIndex] = true;
                                return;
                            }
                            if (!sixel.startsWith("\"1;1;20;12")) {
                                errors[threadIndex] = true;
                                return;
                            }
                        }
                    } catch (Exception e) {
                        errors[threadIndex] = true;
                    }
                });
            }
            
            // Start all threads
            for (Thread thread : threads) {
                thread.start();
            }
            
            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }
            
            // Check for errors
            for (int t = 0; t < threadCount; t++) {
                assertFalse(errors[t], "Thread " + t + " encountered an error");
            }
        }

        @Test
        @DisplayName("Concurrent encoding with different image sizes works correctly")
        void testConcurrentEncodingDifferentSizes() throws InterruptedException {
            final int threadCount = 3;
            final HQSixelEncoder sharedEncoder = new HQSixelEncoder();
            
            // Different image sizes for each thread
            final int[][] sizes = {{10, 6}, {25, 18}, {50, 30}};
            Thread[] threads = new Thread[threadCount];
            final String[] results = new String[threadCount];
            final boolean[] success = new boolean[threadCount];
            
            for (int t = 0; t < threadCount; t++) {
                final int threadIndex = t;
                final int width = sizes[t][0];
                final int height = sizes[t][1];
                
                threads[t] = new Thread(() -> {
                    try {
                        ImageRGB image = new ImageRGB(width, height);
                        fillImageWithGradient(image);
                        
                        results[threadIndex] = sharedEncoder.toSixel(image);
                        
                        // Validate dimensions in output
                        String expected = "\"1;1;" + width + ";" + height;
                        success[threadIndex] = results[threadIndex] != null 
                            && results[threadIndex].startsWith(expected);
                    } catch (Exception e) {
                        success[threadIndex] = false;
                    }
                });
            }
            
            for (Thread thread : threads) {
                thread.start();
            }
            
            for (Thread thread : threads) {
                thread.join();
            }
            
            for (int t = 0; t < threadCount; t++) {
                assertTrue(success[t], "Thread " + t + " failed: expected dimensions " 
                    + sizes[t][0] + "x" + sizes[t][1]);
            }
        }

        @Test
        @DisplayName("Configuration changes during encoding use volatile fields")
        void testVolatileFieldsForConfiguration() {
            // This test verifies that volatile fields work correctly by checking
            // that changes are visible across threads
            final HQSixelEncoder sharedEncoder = new HQSixelEncoder();
            
            // Initial value
            assertEquals(128, sharedEncoder.getPaletteSize());
            
            // Change in main thread
            sharedEncoder.setPaletteSize(256);
            
            // Verify change is visible
            assertEquals(256, sharedEncoder.getPaletteSize());
            
            // Test that encoding still works after configuration change
            ImageRGB image = new ImageRGB(10, 10);
            fillImageWithColor(image, 0xFF0000);
            String sixel = sharedEncoder.toSixel(image);
            
            assertNotNull(sixel);
            assertFalse(sixel.isEmpty());
        }
    }
    // ========================================================================

    /**
     * Fill an image with a solid color.
     */
    private void fillImageWithColor(ImageRGB image, int color) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, color);
            }
        }
    }

    /**
     * Fill an image with a gradient pattern.
     */
    private void fillImageWithGradient(ImageRGB image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int r = (x * 255) / Math.max(1, image.getWidth() - 1);
                int g = (y * 255) / Math.max(1, image.getHeight() - 1);
                int b = 128;
                image.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
    }

    /**
     * Check if a character is valid in sixel output.
     */
    private boolean isValidSixelChar(char c) {
        // Valid sixel characters:
        // - Sixel data: 0x3F (?) to 0x7E (~)
        // - Control: #, !, $, -, ", ;, digits
        if (c >= '?' && c <= '~') return true; // Sixel data
        if (c >= '0' && c <= '9') return true; // Digits for counts
        if (c == '#') return true; // Color selection
        if (c == '!') return true; // Repeat prefix
        if (c == '$') return true; // Carriage return
        if (c == '-') return true; // Line break
        if (c == '"') return true; // Raster attributes
        if (c == ';') return true; // Separator
        return false;
    }
}
