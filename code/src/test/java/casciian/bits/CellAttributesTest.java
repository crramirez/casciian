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
}
