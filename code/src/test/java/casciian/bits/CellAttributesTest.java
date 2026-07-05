package casciian.bits;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CellAttributesTest {

    @Test
    void testBuilder() {
        CellAttributes attr = CellAttributes.builder()
            .bold(true)
            .blink(true)
            .reverse(true)
            .underline(true)
            .protect(true)
            .foreColor(Color.RED)
            .backColor(Color.BLUE)
            .build();

        assertTrue(attr.isBold());
        assertTrue(attr.isBlink());
        assertTrue(attr.isReverse());
        assertTrue(attr.isUnderline());
        assertTrue(attr.isProtect());
        assertEquals(Color.RED, attr.getForeColor());
        assertEquals(Color.BLUE, attr.getBackColor());
    }

    @Test
    void testBuilderRGB() {
        CellAttributes attr = CellAttributes.builder()
            .foreColorRGB(0x123456)
            .backColorRGB(0x654321)
            .build();

        assertEquals(0x123456, attr.getForeColorRGB());
        assertEquals(0x654321, attr.getBackColorRGB());
        assertTrue(attr.isRGB());
    }

    @Test
    void testBuilderPalette() {
        CellAttributes attr = CellAttributes.builder()
            .foreColorPalette(196)
            .backColorPalette(21)
            .build();

        assertEquals(196, attr.getForeColorPalette());
        assertEquals(21, attr.getBackColorPalette());
        assertTrue(attr.isPalette());
        assertFalse(attr.isRGB());
    }

    @Test
    void testPaletteAndRgbAreMutuallyExclusive() {
        CellAttributes attr = new CellAttributes();

        attr.setForeColorRGB(0x123456);
        assertEquals(0x123456, attr.getForeColorRGB());

        // Setting a palette index clears the RGB value.
        attr.setForeColorPalette(200);
        assertEquals(200, attr.getForeColorPalette());
        assertEquals(-1, attr.getForeColorRGB());

        // Setting an RGB value clears the palette index.
        attr.setForeColorRGB(0xABCDEF);
        assertEquals(-1, attr.getForeColorPalette());

        // Setting a named color clears both.
        attr.setForeColorPalette(100);
        attr.setForeColor(Color.CYAN);
        assertEquals(-1, attr.getForeColorPalette());
        assertEquals(-1, attr.getForeColorRGB());
    }

    @Test
    void testPaletteResetAndCopy() {
        CellAttributes attr = new CellAttributes();
        attr.setForeColorPalette(50);
        attr.setBackColorPalette(60);

        CellAttributes copy = new CellAttributes(attr);
        assertEquals(attr, copy);
        assertEquals(attr.hashCode(), copy.hashCode());
        assertEquals(50, copy.getForeColorPalette());
        assertEquals(60, copy.getBackColorPalette());

        attr.reset();
        assertEquals(-1, attr.getForeColorPalette());
        assertEquals(-1, attr.getBackColorPalette());
        assertFalse(attr.isPalette());
    }
}
