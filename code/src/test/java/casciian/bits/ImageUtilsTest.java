package casciian.bits;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ImageUtils#rgbAverage}.
 */
@DisplayName("ImageUtils Tests")
class ImageUtilsTest {

    @Test
    @DisplayName("rgbAverage of uniform color image returns that color")
    void testRgbAverageUniformColor() {
        ImageRGB image = new ByteArrayImageRGB(10, 10);
        int color = 0xFF804020;
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                image.setRGB(x, y, color);
            }
        }

        int avg = ImageUtils.rgbAverage(image, 0, 0, 10, 10);
        // The average of a uniform color should be that color
        assertEquals(0x80, (avg >>> 16) & 0xFF, "Red component");
        assertEquals(0x40, (avg >>> 8) & 0xFF, "Green component");
        assertEquals(0x20, avg & 0xFF, "Blue component");
    }

    @Test
    @DisplayName("rgbAverage of two-color image returns midpoint")
    void testRgbAverageTwoColors() {
        ImageRGB image = new ByteArrayImageRGB(2, 1);
        image.setRGB(0, 0, 0xFF000000); // black
        image.setRGB(1, 0, 0xFFFFFFFF); // white

        int avg = ImageUtils.rgbAverage(image, 0, 0, 2, 1);
        // Average of black and white should be ~(127, 127, 127)
        int r = (avg >>> 16) & 0xFF;
        int g = (avg >>> 8) & 0xFF;
        int b = avg & 0xFF;
        assertEquals(127, r, "Red component");
        assertEquals(127, g, "Green component");
        assertEquals(127, b, "Blue component");
    }

    @Test
    @DisplayName("rgbAverage with subregion returns average of subregion only")
    void testRgbAverageSubregion() {
        ImageRGB image = new ByteArrayImageRGB(4, 4);
        // Fill entire image with black
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                image.setRGB(x, y, 0xFF000000);
            }
        }
        // Fill top-left 2x2 with red
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                image.setRGB(x, y, 0xFFFF0000);
            }
        }

        // Average of just the red subregion
        int avg = ImageUtils.rgbAverage(image, 0, 0, 2, 2);
        assertEquals(0xFF, (avg >>> 16) & 0xFF, "Red component should be 255");
        assertEquals(0x00, (avg >>> 8) & 0xFF, "Green component should be 0");
        assertEquals(0x00, avg & 0xFF, "Blue component should be 0");
    }

    @Test
    @DisplayName("rgbAverage with zero dimensions returns 0")
    void testRgbAverageZeroDimensions() {
        ImageRGB image = new ByteArrayImageRGB(10, 10);
        assertEquals(0, ImageUtils.rgbAverage(image, 0, 0, 0, 5));
        assertEquals(0, ImageUtils.rgbAverage(image, 0, 0, 5, 0));
    }
}
