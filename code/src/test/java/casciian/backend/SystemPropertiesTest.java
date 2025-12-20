package casciian.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for SystemProperties class.
 */
@DisplayName("SystemProperties Tests")
public class SystemPropertiesTest {

    @AfterEach
    public void tearDown() {
        // Clear the system property after each test
        System.clearProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY);
    }

    @Test
    @DisplayName("Get shadow opacity returns default value when not set")
    public void testGetShadowOpacityDefault() {
        // When no property is set, should return default value of 60
        assertEquals(60, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Get shadow opacity returns valid value when set")
    public void testGetShadowOpacityValidValue() {
        // Set a valid value and verify it's returned
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "75");
        assertEquals(75, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Get shadow opacity handles minimum boundary (0)")
    public void testGetShadowOpacityMinBoundary() {
        // Test minimum valid value
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "0");
        assertEquals(0, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Get shadow opacity handles maximum boundary (100)")
    public void testGetShadowOpacityMaxBoundary() {
        // Test maximum valid value
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "100");
        assertEquals(100, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Get shadow opacity returns default for negative values")
    public void testGetShadowOpacityInvalidNegative() {
        // Negative values should return default
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "-1");
        assertEquals(60, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Get shadow opacity returns default for values over 100")
    public void testGetShadowOpacityInvalidTooLarge() {
        // Values over 100 should return default
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "101");
        assertEquals(60, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Set shadow opacity stores valid value")
    public void testSetShadowOpacityValidValue() {
        // Set a valid value and verify it's stored
        SystemProperties.setShadowOpacity(50);
        assertEquals(50, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Set shadow opacity stores minimum boundary value (0)")
    public void testSetShadowOpacityMinBoundary() {
        // Test minimum valid value
        SystemProperties.setShadowOpacity(0);
        assertEquals(0, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Set shadow opacity stores maximum boundary value (100)")
    public void testSetShadowOpacityMaxBoundary() {
        // Test maximum valid value
        SystemProperties.setShadowOpacity(100);
        assertEquals(100, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Set shadow opacity clamps negative values to 0")
    public void testSetShadowOpacityClampNegative() {
        // Negative values should be clamped to 0
        SystemProperties.setShadowOpacity(-1);
        assertEquals(0, SystemProperties.getShadowOpacity());
        
        SystemProperties.setShadowOpacity(-100);
        assertEquals(0, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Set shadow opacity clamps values over 100")
    public void testSetShadowOpacityClampTooLarge() {
        // Values over 100 should be clamped to 100
        SystemProperties.setShadowOpacity(101);
        assertEquals(100, SystemProperties.getShadowOpacity());
        
        SystemProperties.setShadowOpacity(999);
        assertEquals(100, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Set and get shadow opacity works correctly for various values")
    public void testSetShadowOpacityRoundTrip() {
        // Verify setting and getting works correctly for various values
        int[] testValues = {0, 25, 50, 75, 100};
        for (int value : testValues) {
            SystemProperties.setShadowOpacity(value);
            assertEquals(value, SystemProperties.getShadowOpacity());
        }
    }
}
