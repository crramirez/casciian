package casciian.bits;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UnicodeGlyphImage}.
 */
@DisplayName("UnicodeGlyphImage Tests")
class UnicodeGlyphImageTest {

    /**
     * Create a solid-color ImageRGB of the given dimensions.
     */
    private ImageRGB createSolidImage(int width, int height, int rgb) {
        ImageRGB image = new ImageRGB(width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, rgb);
            }
        }
        return image;
    }

    /**
     * Create an ImageRGB with the left half one color and right half another.
     */
    private ImageRGB createLeftRightImage(int width, int height,
        int leftRgb, int rightRgb) {
        ImageRGB image = new ImageRGB(width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (x < width / 2) {
                    image.setRGB(x, y, leftRgb);
                } else {
                    image.setRGB(x, y, rightRgb);
                }
            }
        }
        return image;
    }

    /**
     * Create an ImageRGB with the top half one color and bottom half another.
     */
    private ImageRGB createTopBottomImage(int width, int height,
        int topRgb, int bottomRgb) {
        ImageRGB image = new ImageRGB(width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (y < height / 2) {
                    image.setRGB(x, y, topRgb);
                } else {
                    image.setRGB(x, y, bottomRgb);
                }
            }
        }
        return image;
    }

    // ------------------------------------------------------------------------
    // Constructor tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Constructor with ImageRGB should succeed")
    void testConstructorWithImageRGB() {
        ImageRGB image = createSolidImage(10, 10, 0xFF0000);
        assertDoesNotThrow(() -> new UnicodeGlyphImage(image));
    }

    @Test
    @DisplayName("Constructor with Cell should succeed when cell has image")
    void testConstructorWithCell() {
        ImageRGB image = createSolidImage(10, 10, 0xFF0000);
        Cell cell = new Cell();
        cell.setImage(image);

        assertDoesNotThrow(() -> new UnicodeGlyphImage(cell));
    }

    @Test
    @DisplayName("Constructor with Cell should throw when cell has no image")
    void testConstructorWithCellNoImage() {
        Cell cell = new Cell('A');
        assertThrows(IllegalArgumentException.class,
            () -> new UnicodeGlyphImage(cell));
    }

    // ------------------------------------------------------------------------
    // toHalfBlockGlyph tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("toHalfBlockGlyph should return a cell with block character")
    void testToHalfBlockGlyph() {
        ImageRGB image = createTopBottomImage(10, 10,
            0xFFFF0000, 0xFF0000FF);
        UnicodeGlyphImage glyph = new UnicodeGlyphImage(image);

        Cell result = glyph.toHalfBlockGlyph();
        assertNotNull(result);
        // Should be a block-drawing character (space, upper half, lower half,
        // left half, right half, or full block)
        int ch = result.getChar();
        assertTrue(ch == ' ' || ch == 0x2580 || ch == 0x2584
            || ch == 0x258c || ch == 0x2590 || ch == 0x2588,
            "Expected a block-drawing character, got: 0x"
                + Integer.toHexString(ch));
    }

    @Test
    @DisplayName("toHalfBlockGlyph with solid image returns block character with matching colors")
    void testToHalfBlockGlyphSolid() {
        ImageRGB image = createSolidImage(10, 10, 0xFFFF0000);
        UnicodeGlyphImage glyph = new UnicodeGlyphImage(image);

        Cell result = glyph.toHalfBlockGlyph();
        // A solid single-color image: fore and back colors should be
        // the same (or very close), regardless of which block char is chosen
        int foreR = (result.getForeColorRGB() >>> 16) & 0xFF;
        int foreG = (result.getForeColorRGB() >>> 8) & 0xFF;
        int foreB = result.getForeColorRGB() & 0xFF;
        int backR = (result.getBackColorRGB() >>> 16) & 0xFF;
        int backG = (result.getBackColorRGB() >>> 8) & 0xFF;
        int backB = result.getBackColorRGB() & 0xFF;
        assertTrue(Math.abs(foreR - backR) <= 1
            && Math.abs(foreG - backG) <= 1
            && Math.abs(foreB - backB) <= 1,
            "Fore and back colors should match for solid image");
    }

    // ------------------------------------------------------------------------
    // toQuadrantBlockGlyph tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("toQuadrantBlockGlyph should return a cell with quadrant character")
    void testToQuadrantBlockGlyph() {
        ImageRGB image = createLeftRightImage(10, 10,
            0xFFFFFFFF, 0xFF000000);
        UnicodeGlyphImage glyph = new UnicodeGlyphImage(image);

        Cell result = glyph.toQuadrantBlockGlyph();
        assertNotNull(result);
        int ch = result.getChar();
        // Should be one of the quadrant/block characters
        assertTrue(ch == ' ' || (ch >= 0x2580 && ch <= 0x259f)
            || ch == 0x2588,
            "Expected a quadrant-block character, got: 0x"
                + Integer.toHexString(ch));
    }

    @Test
    @DisplayName("toQuadrantBlockGlyph with solid image returns full block or space")
    void testToQuadrantBlockGlyphSolid() {
        ImageRGB image = createSolidImage(10, 10, 0xFF00FF00);
        UnicodeGlyphImage glyph = new UnicodeGlyphImage(image);

        Cell result = glyph.toQuadrantBlockGlyph();
        int ch = result.getChar();
        // Solid image: all quadrants are the same, so either full block or space
        assertTrue(ch == ' ' || ch == 0x2588,
            "Expected full block or space for solid image, got: 0x"
                + Integer.toHexString(ch));
    }

    // ------------------------------------------------------------------------
    // toSixDotGlyph tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("toSixDotGlyph should return a cell with Braille character")
    void testToSixDotGlyph() {
        ImageRGB image = createLeftRightImage(10, 12,
            0xFFFFFFFF, 0xFF000000);
        UnicodeGlyphImage glyph = new UnicodeGlyphImage(image);

        Cell result = glyph.toSixDotGlyph();
        assertNotNull(result);
        int ch = result.getChar();
        // Braille patterns range from U+2800 to U+283F (6-dot: 64 patterns)
        assertTrue(ch >= 0x2800 && ch <= 0x283F,
            "Expected a Braille character, got: 0x"
                + Integer.toHexString(ch));
    }

    // ------------------------------------------------------------------------
    // toSixDotSolidGlyph tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("toSixDotSolidGlyph should return a cell with Braille character and colors")
    void testToSixDotSolidGlyph() {
        ImageRGB image = createTopBottomImage(10, 12,
            0xFFFF0000, 0xFF0000FF);
        UnicodeGlyphImage glyph = new UnicodeGlyphImage(image);

        Cell result = glyph.toSixDotSolidGlyph();
        assertNotNull(result);
        int ch = result.getChar();
        assertTrue(ch >= 0x2800 && ch <= 0x283F,
            "Expected a Braille character, got: 0x"
                + Integer.toHexString(ch));
        // Should have both fore and back colors set
        assertTrue(result.getForeColorRGB() >= 0);
        assertTrue(result.getBackColorRGB() >= 0);
    }

    // ------------------------------------------------------------------------
    // toSextantBlockGlyph tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("toSextantBlockGlyph should return a cell with sextant character")
    void testToSextantBlockGlyph() {
        ImageRGB image = createLeftRightImage(10, 12,
            0xFFFFFFFF, 0xFF000000);
        UnicodeGlyphImage glyph = new UnicodeGlyphImage(image);

        Cell result = glyph.toSextantBlockGlyph();
        assertNotNull(result);
        int ch = result.getChar();
        // Sextant blocks are in range U+1FB00-U+1FB3B, plus space, ▌, ▐, █
        assertTrue(ch == ' ' || ch == 0x258c || ch == 0x2590
            || ch == 0x2588 || (ch >= 0x1FB00 && ch <= 0x1FB3B),
            "Expected a sextant character, got: 0x"
                + Integer.toHexString(ch));
    }

    // ------------------------------------------------------------------------
    // Color preservation tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Foreground and background colors should be set on output cells")
    void testColorPreservation() {
        ImageRGB image = createLeftRightImage(10, 10,
            0xFFFF0000, 0xFF0000FF);
        UnicodeGlyphImage glyph = new UnicodeGlyphImage(image);

        Cell halfBlock = glyph.toHalfBlockGlyph();
        assertTrue(halfBlock.getForeColorRGB() >= 0,
            "Foreground color should be set");
        assertTrue(halfBlock.getBackColorRGB() >= 0,
            "Background color should be set");

        Cell quadrant = glyph.toQuadrantBlockGlyph();
        assertTrue(quadrant.getForeColorRGB() >= 0);
        assertTrue(quadrant.getBackColorRGB() >= 0);

        Cell sextant = glyph.toSextantBlockGlyph();
        assertTrue(sextant.getForeColorRGB() >= 0);
        assertTrue(sextant.getBackColorRGB() >= 0);
    }
}
