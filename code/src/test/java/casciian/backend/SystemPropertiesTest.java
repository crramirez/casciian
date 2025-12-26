/*
 * Casciian - Java Text User Interface
 *
 * Copyright 2025 Carlos Rafael Ramirez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package casciian.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for SystemProperties class.
 */
@DisplayName("SystemProperties Tests")
class SystemPropertiesTest {

    @AfterEach
    void tearDown() {
        // Clear the system properties after each test
        System.clearProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY);
        System.clearProperty(SystemProperties.CASCIIAN_TEXT_MOUSE);
        SystemProperties.reset();
    }

    @Test
    @DisplayName("Get shadow opacity returns default value when not set")
    void testGetShadowOpacityDefault() {
        // When no property is set, should return default value of 60
        assertEquals(60, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Get shadow opacity returns valid value when set")
    void testGetShadowOpacityValidValue() {
        // Set a valid value and verify it's returned
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "75");
        assertEquals(75, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Get shadow opacity handles minimum boundary (0)")
    void testGetShadowOpacityMinBoundary() {
        // Test minimum valid value
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "0");
        assertEquals(0, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Get shadow opacity handles maximum boundary (100)")
    void testGetShadowOpacityMaxBoundary() {
        // Test maximum valid value
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "100");
        assertEquals(100, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Get shadow opacity returns default for negative values")
    void testGetShadowOpacityInvalidNegative() {
        // Negative values should return default
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "-1");
        assertEquals(60, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Get shadow opacity returns default for values over 100")
    void testGetShadowOpacityInvalidTooLarge() {
        // Values over 100 should return default
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "101");
        assertEquals(60, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Set shadow opacity stores valid value")
    void testSetShadowOpacityValidValue() {
        // Set a valid value and verify it's stored
        SystemProperties.setShadowOpacity(50);
        assertEquals(50, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Set shadow opacity stores minimum boundary value (0)")
    void testSetShadowOpacityMinBoundary() {
        // Test minimum valid value
        SystemProperties.setShadowOpacity(0);
        assertEquals(0, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Set shadow opacity stores maximum boundary value (100)")
    void testSetShadowOpacityMaxBoundary() {
        // Test maximum valid value
        SystemProperties.setShadowOpacity(100);
        assertEquals(100, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Set shadow opacity clamps negative values to 0")
    void testSetShadowOpacityClampNegative() {
        // Negative values should be clamped to 0
        SystemProperties.setShadowOpacity(-1);
        assertEquals(0, SystemProperties.getShadowOpacity());
        
        SystemProperties.setShadowOpacity(-100);
        assertEquals(0, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Set shadow opacity clamps values over 100")
    void testSetShadowOpacityClampTooLarge() {
        // Values over 100 should be clamped to 100
        SystemProperties.setShadowOpacity(101);
        assertEquals(100, SystemProperties.getShadowOpacity());
        
        SystemProperties.setShadowOpacity(999);
        assertEquals(100, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Set and get shadow opacity works correctly for various values")
    void testSetShadowOpacityRoundTrip() {
        // Verify setting and getting works correctly for various values
        int[] testValues = {0, 25, 50, 75, 100};
        for (int value : testValues) {
            SystemProperties.setShadowOpacity(value);
            assertEquals(value, SystemProperties.getShadowOpacity());
        }
    }

    // -------------------------------------------------------------------------
    // Text Mouse Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Get text mouse returns default value (true) when not set")
    void testIsTextMouseDefault() {
        // When no property is set, should return default value of true
        assertTrue(SystemProperties.isTextMouse());
    }

    @Test
    @DisplayName("Get text mouse returns true when set to 'true'")
    void testIsTextMouseSetTrue() {
        System.setProperty(SystemProperties.CASCIIAN_TEXT_MOUSE, "true");
        assertTrue(SystemProperties.isTextMouse());
    }

    @Test
    @DisplayName("Get text mouse returns false when set to 'false'")
    void testIsTextMouseSetFalse() {
        System.setProperty(SystemProperties.CASCIIAN_TEXT_MOUSE, "false");
        assertFalse(SystemProperties.isTextMouse());
    }

    @Test
    @DisplayName("Get text mouse returns false for invalid values")
    void testIsTextMouseInvalidValue() {
        // Any value other than "true" should return false
        System.setProperty(SystemProperties.CASCIIAN_TEXT_MOUSE, "invalid");
        assertFalse(SystemProperties.isTextMouse());
    }

    @Test
    @DisplayName("Set text mouse to true")
    void testSetTextMouseTrue() {
        SystemProperties.setTextMouse(true);
        assertTrue(SystemProperties.isTextMouse());
    }

    @Test
    @DisplayName("Set text mouse to false")
    void testSetTextMouseFalse() {
        SystemProperties.setTextMouse(false);
        assertFalse(SystemProperties.isTextMouse());
    }

    @Test
    @DisplayName("Set and get text mouse round trip")
    void testSetTextMouseRoundTrip() {
        // Test toggling the value
        SystemProperties.setTextMouse(false);
        assertFalse(SystemProperties.isTextMouse());
        
        SystemProperties.setTextMouse(true);
        assertTrue(SystemProperties.isTextMouse());
        
        SystemProperties.setTextMouse(false);
        assertFalse(SystemProperties.isTextMouse());
    }

    @Test
    @DisplayName("Reset clears text mouse cached value")
    void testResetClearsTextMouse() {
        // Set text mouse to false
        SystemProperties.setTextMouse(false);
        assertFalse(SystemProperties.isTextMouse());
        
        // Set system property to true and reset
        System.setProperty(SystemProperties.CASCIIAN_TEXT_MOUSE, "true");
        SystemProperties.reset();
        
        // After reset, should read from system property again
        assertTrue(SystemProperties.isTextMouse());
    }
}
