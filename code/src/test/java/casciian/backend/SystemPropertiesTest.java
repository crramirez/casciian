/*
 * Casciian - Java Text User Interface
 *
 * Unit tests for SystemProperties
 */
package casciian.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SystemProperties - validates system property management.
 * This tests the get/set behavior, boundary validation, and default value handling.
 */
@DisplayName("SystemProperties Tests")
class SystemPropertiesTest {

    private String originalShadowOpacity;

    @BeforeEach
    void setUp() {
        // Save the original value to restore later
        originalShadowOpacity = System.getProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY);
    }

    @AfterEach
    void tearDown() {
        // Restore the original value or clear if it wasn't set
        if (originalShadowOpacity == null) {
            System.clearProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY);
        } else {
            System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, originalShadowOpacity);
        }
    }

    @Test
    @DisplayName("getShadowOpacity returns default value of 60 when property not set")
    void testGetShadowOpacityDefault() {
        System.clearProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY);
        assertEquals(60, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("setShadowOpacity stores value in system properties")
    void testSetShadowOpacity() {
        SystemProperties.setShadowOpacity(75);
        assertEquals("75", System.getProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY));
    }

    @Test
    @DisplayName("getShadowOpacity retrieves value set by setShadowOpacity")
    void testGetSetRoundTrip() {
        SystemProperties.setShadowOpacity(80);
        assertEquals(80, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("getShadowOpacity accepts minimum boundary value of 0")
    void testGetShadowOpacityMinimumBoundary() {
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "0");
        assertEquals(0, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("getShadowOpacity accepts maximum boundary value of 100")
    void testGetShadowOpacityMaximumBoundary() {
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "100");
        assertEquals(100, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("getShadowOpacity returns default when value is negative")
    void testGetShadowOpacityNegativeValue() {
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "-1");
        assertEquals(60, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("getShadowOpacity returns default when value exceeds 100")
    void testGetShadowOpacityExceedsMaximum() {
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "101");
        assertEquals(60, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("getShadowOpacity returns default when value is far below minimum")
    void testGetShadowOpacityFarBelowMinimum() {
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "-999");
        assertEquals(60, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("getShadowOpacity returns default when value is far above maximum")
    void testGetShadowOpacityFarAboveMaximum() {
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "999");
        assertEquals(60, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("setShadowOpacity and getShadowOpacity work with minimum boundary")
    void testSetGetMinimumBoundary() {
        SystemProperties.setShadowOpacity(0);
        assertEquals(0, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("setShadowOpacity and getShadowOpacity work with maximum boundary")
    void testSetGetMaximumBoundary() {
        SystemProperties.setShadowOpacity(100);
        assertEquals(100, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("setShadowOpacity can be called multiple times")
    void testSetShadowOpacityMultipleTimes() {
        SystemProperties.setShadowOpacity(25);
        assertEquals(25, SystemProperties.getShadowOpacity());
        
        SystemProperties.setShadowOpacity(50);
        assertEquals(50, SystemProperties.getShadowOpacity());
        
        SystemProperties.setShadowOpacity(75);
        assertEquals(75, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("getShadowOpacity handles mid-range values correctly")
    void testGetShadowOpacityMidRange() {
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "50");
        assertEquals(50, SystemProperties.getShadowOpacity());
        
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "33");
        assertEquals(33, SystemProperties.getShadowOpacity());
        
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "67");
        assertEquals(67, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("getShadowOpacity returns default for invalid string values")
    void testGetShadowOpacityInvalidString() {
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "not_a_number");
        // Integer.getInteger returns null for invalid strings, which is handled by the default
        assertEquals(60, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("System property key constant is correctly defined")
    void testSystemPropertyKeyConstant() {
        assertEquals("casciian.shadowOpacity", SystemProperties.CASCIIAN_SHADOW_OPACITY);
    }

    @Test
    @DisplayName("getShadowOpacity handles edge case at boundary + 1")
    void testGetShadowOpacityJustAboveMaximum() {
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "101");
        assertEquals(60, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("getShadowOpacity handles edge case at boundary - 1")
    void testGetShadowOpacityJustBelowMinimum() {
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "-1");
        assertEquals(60, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("setShadowOpacity stores negative values without validation")
    void testSetShadowOpacityNegative() {
        // setShadowOpacity doesn't validate, it just stores
        SystemProperties.setShadowOpacity(-10);
        // getShadowOpacity should return default for out-of-range values
        assertEquals(60, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("setShadowOpacity stores values over 100 without validation")
    void testSetShadowOpacityOver100() {
        // setShadowOpacity doesn't validate, it just stores
        SystemProperties.setShadowOpacity(150);
        // getShadowOpacity should return default for out-of-range values
        assertEquals(60, SystemProperties.getShadowOpacity());
    }
}
