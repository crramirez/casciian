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

/**
 * Tests for SystemProperties class.
 */
@DisplayName("SystemProperties Tests")
class SystemPropertiesTest {

    @AfterEach
    void tearDown() {
        // Clear the system property after each test
        System.clearProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY);
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
}
