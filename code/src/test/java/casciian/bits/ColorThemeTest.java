package casciian.bits;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColorThemeTest {

    @Test
    void testBoldSetsBoldAttributeOnly() {
        ColorTheme theme = new ColorTheme();
        theme.setColorFromString("test.color", "bold red on blue");

        CellAttributes attr = theme.getColor("test.color");
        assertTrue(attr.isBold());
        assertEquals(Color.RED, attr.getForeColor());
        assertEquals(Color.BLUE, attr.getBackColor());
    }

    @Test
    void testBrightSetsBrightColorDirectly() {
        ColorTheme theme = new ColorTheme();
        theme.setColorFromString("test.color", "bright red on blue");

        CellAttributes attr = theme.getColor("test.color");
        assertFalse(attr.isBold());
        assertEquals(Color.BRIGHT_RED, attr.getForeColor());
        assertEquals(Color.BLUE, attr.getBackColor());
    }

    @Test
    void testBoldAndBrightCombine() {
        ColorTheme theme = new ColorTheme();
        theme.setColorFromString("test.color", "bold bright red on blue");

        CellAttributes attr = theme.getColor("test.color");
        assertTrue(attr.isBold());
        assertEquals(Color.BRIGHT_RED, attr.getForeColor());
    }

    @Test
    void testBlinkWithBright() {
        ColorTheme theme = new ColorTheme();
        theme.setColorFromString("test.color", "bright blink red on blue");

        CellAttributes attr = theme.getColor("test.color");
        assertTrue(attr.isBlink());
        assertEquals(Color.BRIGHT_RED, attr.getForeColor());
    }
}
