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
            .faint(true)
            .italic(true)
            .hidden(true)
            .strikethrough(true)
            .foreColor(Color.RED)
            .backColor(Color.BLUE)
            .build();

        assertTrue(attr.isBold());
        assertTrue(attr.isBlink());
        assertTrue(attr.isReverse());
        assertTrue(attr.isUnderline());
        assertTrue(attr.isProtect());
        assertTrue(attr.isFaint());
        assertTrue(attr.isItalic());
        assertTrue(attr.isHidden());
        assertTrue(attr.isStrikethrough());
        assertEquals(Color.RED, attr.getForeColor());
        assertEquals(Color.BLUE, attr.getBackColor());
    }

    @Test
    void testFaintItalicHiddenStrikethroughDefaultToFalse() {
        CellAttributes attr = new CellAttributes();
        assertFalse(attr.isFaint());
        assertFalse(attr.isItalic());
        assertFalse(attr.isHidden());
        assertFalse(attr.isStrikethrough());
    }

    @Test
    void testFaintItalicHiddenStrikethroughSettersAndReset() {
        CellAttributes attr = new CellAttributes();
        attr.setFaint(true);
        attr.setItalic(true);
        attr.setHidden(true);
        attr.setStrikethrough(true);

        assertTrue(attr.isFaint());
        assertTrue(attr.isItalic());
        assertTrue(attr.isHidden());
        assertTrue(attr.isStrikethrough());

        attr.reset();
        assertFalse(attr.isFaint());
        assertFalse(attr.isItalic());
        assertFalse(attr.isHidden());
        assertFalse(attr.isStrikethrough());
    }

    @Test
    void testFaintItalicHiddenStrikethroughCopiedBySetTo() {
        CellAttributes attr = new CellAttributes();
        attr.setFaint(true);
        attr.setItalic(true);
        attr.setHidden(true);
        attr.setStrikethrough(true);

        CellAttributes copy = new CellAttributes(attr);
        assertEquals(attr, copy);
        assertEquals(attr.hashCode(), copy.hashCode());
        assertTrue(copy.isFaint());
        assertTrue(copy.isItalic());
        assertTrue(copy.isHidden());
        assertTrue(copy.isStrikethrough());
    }

    @Test
    void testHyperlinkDefaultsToNull() {
        CellAttributes attr = new CellAttributes();
        assertNull(attr.getHyperlink());
        assertFalse(attr.isHyperlink());
    }

    @Test
    void testHyperlinkSetterBuilderAndReset() {
        CellAttributes attr = CellAttributes.builder()
            .hyperlink("https://example.com")
            .build();
        assertEquals("https://example.com", attr.getHyperlink());
        assertTrue(attr.isHyperlink());

        attr.reset();
        assertNull(attr.getHyperlink());
    }

    @Test
    void testHyperlinkCopiedBySetToAndAffectsEquality() {
        CellAttributes attr = new CellAttributes();
        attr.setHyperlink("https://example.com");

        CellAttributes copy = new CellAttributes(attr);
        assertEquals("https://example.com", copy.getHyperlink());
        assertEquals(attr, copy);
        assertEquals(attr.hashCode(), copy.hashCode());

        // Differing hyperlinks make cells unequal.
        copy.setHyperlink("https://other.example.com");
        assertNotEquals(attr, copy);

        // Clearing makes them unequal to a linked cell.
        copy.setHyperlink(null);
        assertNotEquals(attr, copy);
    }

    @Test
    void testToStringUsesSingleOnSeparator() {
        CellAttributes attr = new CellAttributes();
        attr.setBold(true);
        attr.setForeColor(Color.RED);
        attr.setBackColor(Color.BLUE);

        assertEquals("bold red on blue", attr.toString());
    }

    @Test
    void testToHtmlHiddenKeepsBackgroundVisible() {
        CellAttributes attr = new CellAttributes();
        attr.setHidden(true);
        attr.setForeColor(Color.RED);
        attr.setBackColor(Color.BLUE);

        String html = attr.toHtml();

        assertTrue(html.contains("color: transparent"),
            "Hidden text should use a transparent foreground.");
        assertTrue(html.contains("background-color: " + Color.BLUE.toRgbString(false)),
            "Hidden text should preserve the background color.");
        assertFalse(html.contains("visibility: hidden"),
            "Hidden text should not hide the entire cell.");
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
