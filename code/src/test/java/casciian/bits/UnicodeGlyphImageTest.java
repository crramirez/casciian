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
    }

}
