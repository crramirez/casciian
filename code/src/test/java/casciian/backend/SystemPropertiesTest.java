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
        System.clearProperty(SystemProperties.CASCIIAN_ANIMATIONS);
        System.clearProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY);
        System.clearProperty(SystemProperties.CASCIIAN_TEXT_MOUSE);
        System.clearProperty(SystemProperties.CASCIIAN_TRANSLUCENCE);
        System.clearProperty(SystemProperties.CASCIIAN_HIDE_MOUSE_WHEN_TYPING);
        System.clearProperty(SystemProperties.CASCIIAN_HIDE_STATUS_BAR);
        System.clearProperty(SystemProperties.CASCIIAN_HIDE_MENU_BAR);
        System.clearProperty(SystemProperties.CASCIIAN_BLINK_MILLIS);
        System.clearProperty(SystemProperties.CASCIIAN_BLINK_DIM_PERCENT);
        System.clearProperty(SystemProperties.CASCIIAN_TEXT_BLINK);
        System.clearProperty(SystemProperties.CASCIIAN_MENU_ICONS);
        System.clearProperty(SystemProperties.CASCIIAN_MENU_ICONS_OFFSET);
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
    @DisplayName("Get text mouse returns default value (false) when not set")
    void testIsTextMouseDefault() {
        // When no property is set, should return default value of false
        assertFalse(SystemProperties.isTextMouse());
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
        SystemProperties.setTextMouse(true);
        assertTrue(SystemProperties.isTextMouse());
        
        SystemProperties.setTextMouse(false);
        assertFalse(SystemProperties.isTextMouse());
        
        SystemProperties.setTextMouse(true);
        assertTrue(SystemProperties.isTextMouse());
    }

    @Test
    @DisplayName("Reset clears text mouse cached value")
    void testResetClearsTextMouse() {
        // Set text mouse to true
        SystemProperties.setTextMouse(true);
        assertTrue(SystemProperties.isTextMouse());
        
        // Set system property to false and reset
        System.setProperty(SystemProperties.CASCIIAN_TEXT_MOUSE, "false");
        SystemProperties.reset();
        
        // After reset, should read from system property again
        assertFalse(SystemProperties.isTextMouse());
    }

    // -------------------------------------------------------------------------
    // Animations Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Get animations returns default value (false) when not set")
    void testIsAnimationsDefault() {
        // When no property is set, should return default value of false
        assertFalse(SystemProperties.isAnimations());
    }

    @Test
    @DisplayName("Get animations returns true when set to 'true'")
    void testIsAnimationsSetTrue() {
        System.setProperty(SystemProperties.CASCIIAN_ANIMATIONS, "true");
        assertTrue(SystemProperties.isAnimations());
    }

    @Test
    @DisplayName("Get animations returns false when set to 'false'")
    void testIsAnimationsSetFalse() {
        System.setProperty(SystemProperties.CASCIIAN_ANIMATIONS, "false");
        assertFalse(SystemProperties.isAnimations());
    }

    @Test
    @DisplayName("Get animations returns false for invalid values")
    void testIsAnimationsInvalidValue() {
        // Any value other than "true" should return false
        System.setProperty(SystemProperties.CASCIIAN_ANIMATIONS, "invalid");
        assertFalse(SystemProperties.isAnimations());
    }

    @Test
    @DisplayName("Set animations to true")
    void testSetAnimationsTrue() {
        SystemProperties.setAnimations(true);
        assertTrue(SystemProperties.isAnimations());
    }

    @Test
    @DisplayName("Set animations to false")
    void testSetAnimationsFalse() {
        SystemProperties.setAnimations(false);
        assertFalse(SystemProperties.isAnimations());
    }

    @Test
    @DisplayName("Set and get animations round trip")
    void testSetAnimationsRoundTrip() {
        // Test toggling the value
        SystemProperties.setAnimations(true);
        assertTrue(SystemProperties.isAnimations());
        
        SystemProperties.setAnimations(false);
        assertFalse(SystemProperties.isAnimations());
        
        SystemProperties.setAnimations(true);
        assertTrue(SystemProperties.isAnimations());
    }

    @Test
    @DisplayName("Reset clears animations cached value")
    void testResetClearsAnimations() {
        // Set animations to true
        SystemProperties.setAnimations(true);
        assertTrue(SystemProperties.isAnimations());
        
        // Set system property to false and reset
        System.setProperty(SystemProperties.CASCIIAN_ANIMATIONS, "false");
        SystemProperties.reset();
        
        // After reset, should read from system property again
        assertFalse(SystemProperties.isAnimations());
    }

    // -------------------------------------------------------------------------
    // Translucence Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Get translucence returns default value (false) when not set")
    void testIsTranslucenceDefault() {
        // When no property is set, should return default value of false
        assertFalse(SystemProperties.isTranslucence());
    }

    @Test
    @DisplayName("Get translucence returns true when set to 'true'")
    void testIsTranslucenceSetTrue() {
        System.setProperty(SystemProperties.CASCIIAN_TRANSLUCENCE, "true");
        assertTrue(SystemProperties.isTranslucence());
    }

    @Test
    @DisplayName("Get translucence returns false when set to 'false'")
    void testIsTranslucenceSetFalse() {
        System.setProperty(SystemProperties.CASCIIAN_TRANSLUCENCE, "false");
        assertFalse(SystemProperties.isTranslucence());
    }

    @Test
    @DisplayName("Get translucence returns false for invalid values")
    void testIsTranslucenceInvalidValue() {
        // Any value other than "true" should return false
        System.setProperty(SystemProperties.CASCIIAN_TRANSLUCENCE, "invalid");
        assertFalse(SystemProperties.isTranslucence());
    }

    @Test
    @DisplayName("Set translucence to true")
    void testSetTranslucenceTrue() {
        SystemProperties.setTranslucence(true);
        assertTrue(SystemProperties.isTranslucence());
    }

    @Test
    @DisplayName("Set translucence to false")
    void testSetTranslucenceFalse() {
        SystemProperties.setTranslucence(false);
        assertFalse(SystemProperties.isTranslucence());
    }

    @Test
    @DisplayName("Set and get translucence round trip")
    void testSetTranslucenceRoundTrip() {
        // Test toggling the value
        SystemProperties.setTranslucence(true);
        assertTrue(SystemProperties.isTranslucence());
        
        SystemProperties.setTranslucence(false);
        assertFalse(SystemProperties.isTranslucence());
        
        SystemProperties.setTranslucence(true);
        assertTrue(SystemProperties.isTranslucence());
    }

    @Test
    @DisplayName("Reset clears translucence cached value")
    void testResetClearsTranslucence() {
        // Set translucence to true
        SystemProperties.setTranslucence(true);
        assertTrue(SystemProperties.isTranslucence());
        
        // Set system property to false and reset
        System.setProperty(SystemProperties.CASCIIAN_TRANSLUCENCE, "false");
        SystemProperties.reset();
        
        // After reset, should read from system property again
        assertFalse(SystemProperties.isTranslucence());
    }

    // -------------------------------------------------------------------------
    // Hide Mouse When Typing Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Get hideMouseWhenTyping returns default value (false) when not set")
    void testIsHideMouseWhenTypingDefault() {
        assertFalse(SystemProperties.isHideMouseWhenTyping());
    }

    @Test
    @DisplayName("Get hideMouseWhenTyping returns true when set to 'true'")
    void testIsHideMouseWhenTypingSetTrue() {
        System.setProperty(SystemProperties.CASCIIAN_HIDE_MOUSE_WHEN_TYPING, "true");
        assertTrue(SystemProperties.isHideMouseWhenTyping());
    }

    @Test
    @DisplayName("Set hideMouseWhenTyping to true")
    void testSetHideMouseWhenTypingTrue() {
        SystemProperties.setHideMouseWhenTyping(true);
        assertTrue(SystemProperties.isHideMouseWhenTyping());
    }

    @Test
    @DisplayName("Set hideMouseWhenTyping to false")
    void testSetHideMouseWhenTypingFalse() {
        SystemProperties.setHideMouseWhenTyping(false);
        assertFalse(SystemProperties.isHideMouseWhenTyping());
    }

    // -------------------------------------------------------------------------
    // Hide Status Bar Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Get hideStatusBar returns default value (false) when not set")
    void testIsHideStatusBarDefault() {
        assertFalse(SystemProperties.isHideStatusBar());
    }

    @Test
    @DisplayName("Get hideStatusBar returns true when set to 'true'")
    void testIsHideStatusBarSetTrue() {
        System.setProperty(SystemProperties.CASCIIAN_HIDE_STATUS_BAR, "true");
        assertTrue(SystemProperties.isHideStatusBar());
    }

    @Test
    @DisplayName("Set hideStatusBar to true")
    void testSetHideStatusBarTrue() {
        SystemProperties.setHideStatusBar(true);
        assertTrue(SystemProperties.isHideStatusBar());
    }

    @Test
    @DisplayName("Set hideStatusBar to false")
    void testSetHideStatusBarFalse() {
        SystemProperties.setHideStatusBar(false);
        assertFalse(SystemProperties.isHideStatusBar());
    }

    // -------------------------------------------------------------------------
    // Hide Menu Bar Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Get hideMenuBar returns default value (false) when not set")
    void testIsHideMenuBarDefault() {
        assertFalse(SystemProperties.isHideMenuBar());
    }

    @Test
    @DisplayName("Get hideMenuBar returns true when set to 'true'")
    void testIsHideMenuBarSetTrue() {
        System.setProperty(SystemProperties.CASCIIAN_HIDE_MENU_BAR, "true");
        assertTrue(SystemProperties.isHideMenuBar());
    }

    @Test
    @DisplayName("Set hideMenuBar to true")
    void testSetHideMenuBarTrue() {
        SystemProperties.setHideMenuBar(true);
        assertTrue(SystemProperties.isHideMenuBar());
    }

    @Test
    @DisplayName("Set hideMenuBar to false")
    void testSetHideMenuBarFalse() {
        SystemProperties.setHideMenuBar(false);
        assertFalse(SystemProperties.isHideMenuBar());
    }

    // -------------------------------------------------------------------------
    // Blink Millis Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Get blinkMillis returns default value (500) when not set")
    void testGetBlinkMillisDefault() {
        assertEquals(500, SystemProperties.getBlinkMillis());
    }

    @Test
    @DisplayName("Get blinkMillis returns valid value when set")
    void testGetBlinkMillisValidValue() {
        System.setProperty(SystemProperties.CASCIIAN_BLINK_MILLIS, "250");
        assertEquals(250, SystemProperties.getBlinkMillis());
    }

    @Test
    @DisplayName("Get blinkMillis handles minimum boundary (0)")
    void testGetBlinkMillisMinBoundary() {
        System.setProperty(SystemProperties.CASCIIAN_BLINK_MILLIS, "0");
        assertEquals(0, SystemProperties.getBlinkMillis());
    }

    @Test
    @DisplayName("Get blinkMillis handles maximum boundary (500)")
    void testGetBlinkMillisMaxBoundary() {
        System.setProperty(SystemProperties.CASCIIAN_BLINK_MILLIS, "500");
        assertEquals(500, SystemProperties.getBlinkMillis());
    }

    @Test
    @DisplayName("Set blinkMillis stores valid value")
    void testSetBlinkMillisValidValue() {
        SystemProperties.setBlinkMillis(300);
        assertEquals(300, SystemProperties.getBlinkMillis());
    }

    @Test
    @DisplayName("Set blinkMillis clamps negative values to 0")
    void testSetBlinkMillisClampNegative() {
        SystemProperties.setBlinkMillis(-1);
        assertEquals(0, SystemProperties.getBlinkMillis());
    }

    @Test
    @DisplayName("Set blinkMillis clamps values over 500")
    void testSetBlinkMillisClampTooLarge() {
        SystemProperties.setBlinkMillis(600);
        assertEquals(500, SystemProperties.getBlinkMillis());
    }

    // -------------------------------------------------------------------------
    // Blink Dim Percent Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Get blinkDimPercent returns default value (80) when not set")
    void testGetBlinkDimPercentDefault() {
        assertEquals(80, SystemProperties.getBlinkDimPercent());
    }

    @Test
    @DisplayName("Get blinkDimPercent returns valid value when set")
    void testGetBlinkDimPercentValidValue() {
        System.setProperty(SystemProperties.CASCIIAN_BLINK_DIM_PERCENT, "50");
        assertEquals(50, SystemProperties.getBlinkDimPercent());
    }

    @Test
    @DisplayName("Set blinkDimPercent stores valid value")
    void testSetBlinkDimPercentValidValue() {
        SystemProperties.setBlinkDimPercent(75);
        assertEquals(75, SystemProperties.getBlinkDimPercent());
    }

    @Test
    @DisplayName("Set blinkDimPercent clamps negative values to 0")
    void testSetBlinkDimPercentClampNegative() {
        SystemProperties.setBlinkDimPercent(-10);
        assertEquals(0, SystemProperties.getBlinkDimPercent());
    }

    @Test
    @DisplayName("Set blinkDimPercent clamps values over 100")
    void testSetBlinkDimPercentClampTooLarge() {
        SystemProperties.setBlinkDimPercent(150);
        assertEquals(100, SystemProperties.getBlinkDimPercent());
    }

    // -------------------------------------------------------------------------
    // Text Blink Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Get textBlink returns default value (true) when not set")
    void testIsTextBlinkDefault() {
        assertTrue(SystemProperties.isTextBlink());
    }

    @Test
    @DisplayName("Get textBlink returns false when set to 'false'")
    void testIsTextBlinkSetFalse() {
        System.setProperty(SystemProperties.CASCIIAN_TEXT_BLINK, "false");
        assertFalse(SystemProperties.isTextBlink());
    }

    @Test
    @DisplayName("Set textBlink to true")
    void testSetTextBlinkTrue() {
        SystemProperties.setTextBlink(true);
        assertTrue(SystemProperties.isTextBlink());
    }

    @Test
    @DisplayName("Set textBlink to false")
    void testSetTextBlinkFalse() {
        SystemProperties.setTextBlink(false);
        assertFalse(SystemProperties.isTextBlink());
    }

    // -------------------------------------------------------------------------
    // Menu Icons Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Get menuIcons returns default value (true) when not set")
    void testIsMenuIconsDefault() {
        assertFalse(SystemProperties.isMenuIcons());
    }

    @Test
    @DisplayName("Get menuIcons returns false when set to 'false'")
    void testIsMenuIconsSetFalse() {
        System.setProperty(SystemProperties.CASCIIAN_MENU_ICONS, "false");
        assertFalse(SystemProperties.isMenuIcons());
    }

    @Test
    @DisplayName("Set menuIcons to true")
    void testSetMenuIconsTrue() {
        SystemProperties.setMenuIcons(true);
        assertTrue(SystemProperties.isMenuIcons());
    }

    @Test
    @DisplayName("Set menuIcons to false")
    void testSetMenuIconsFalse() {
        SystemProperties.setMenuIcons(false);
        assertFalse(SystemProperties.isMenuIcons());
    }

    // -------------------------------------------------------------------------
    // Menu Icons Offset Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Get menuIconsOffset returns default value (3) when not set")
    void testGetMenuIconsOffsetDefault() {
        assertEquals(3, SystemProperties.getMenuIconsOffset());
    }

    @Test
    @DisplayName("Get menuIconsOffset returns valid value when set")
    void testGetMenuIconsOffsetValidValue() {
        System.setProperty(SystemProperties.CASCIIAN_MENU_ICONS_OFFSET, "2");
        assertEquals(2, SystemProperties.getMenuIconsOffset());
    }

    @Test
    @DisplayName("Get menuIconsOffset handles minimum boundary (0)")
    void testGetMenuIconsOffsetMinBoundary() {
        System.setProperty(SystemProperties.CASCIIAN_MENU_ICONS_OFFSET, "0");
        assertEquals(0, SystemProperties.getMenuIconsOffset());
    }

    @Test
    @DisplayName("Get menuIconsOffset handles maximum boundary (5)")
    void testGetMenuIconsOffsetMaxBoundary() {
        System.setProperty(SystemProperties.CASCIIAN_MENU_ICONS_OFFSET, "5");
        assertEquals(5, SystemProperties.getMenuIconsOffset());
    }

    @Test
    @DisplayName("Get menuIconsOffset returns 0 for negative values")
    void testGetMenuIconsOffsetInvalidNegative() {
        System.setProperty(SystemProperties.CASCIIAN_MENU_ICONS_OFFSET, "-1");
        assertEquals(0, SystemProperties.getMenuIconsOffset());
    }

    @Test
    @DisplayName("Get menuIconsOffset returns 5 for values over 5")
    void testGetMenuIconsOffsetInvalidTooLarge() {
        System.setProperty(SystemProperties.CASCIIAN_MENU_ICONS_OFFSET, "6");
        assertEquals(5, SystemProperties.getMenuIconsOffset());
    }

    @Test
    @DisplayName("Get menuIconsOffset returns default for invalid string values")
    void testGetMenuIconsOffsetInvalidString() {
        System.setProperty(SystemProperties.CASCIIAN_MENU_ICONS_OFFSET, "invalid");
        assertEquals(3, SystemProperties.getMenuIconsOffset());
    }

    @Test
    @DisplayName("Set menuIconsOffset stores valid value")
    void testSetMenuIconsOffsetValidValue() {
        SystemProperties.setMenuIconsOffset(4);
        assertEquals(4, SystemProperties.getMenuIconsOffset());
    }

    @Test
    @DisplayName("Set menuIconsOffset stores minimum boundary value (0)")
    void testSetMenuIconsOffsetMinBoundary() {
        SystemProperties.setMenuIconsOffset(0);
        assertEquals(0, SystemProperties.getMenuIconsOffset());
    }

    @Test
    @DisplayName("Set menuIconsOffset stores maximum boundary value (5)")
    void testSetMenuIconsOffsetMaxBoundary() {
        SystemProperties.setMenuIconsOffset(5);
        assertEquals(5, SystemProperties.getMenuIconsOffset());
    }

    @Test
    @DisplayName("Set menuIconsOffset clamps negative values to 0")
    void testSetMenuIconsOffsetClampNegative() {
        SystemProperties.setMenuIconsOffset(-1);
        assertEquals(0, SystemProperties.getMenuIconsOffset());

        SystemProperties.setMenuIconsOffset(-100);
        assertEquals(0, SystemProperties.getMenuIconsOffset());
    }

    @Test
    @DisplayName("Set menuIconsOffset clamps values over 5")
    void testSetMenuIconsOffsetClampTooLarge() {
        SystemProperties.setMenuIconsOffset(6);
        assertEquals(5, SystemProperties.getMenuIconsOffset());

        SystemProperties.setMenuIconsOffset(999);
        assertEquals(5, SystemProperties.getMenuIconsOffset());
    }

    @Test
    @DisplayName("Set and get menuIconsOffset works correctly for all valid values")
    void testSetMenuIconsOffsetRoundTrip() {
        // Verify setting and getting works correctly for all values in range
        int[] testValues = {0, 1, 2, 3, 4, 5};
        for (int value : testValues) {
            SystemProperties.setMenuIconsOffset(value);
            assertEquals(value, SystemProperties.getMenuIconsOffset());
        }
    }

    @Test
    @DisplayName("Reset clears menuIconsOffset cached value")
    void testResetClearsMenuIconsOffset() {
        // Set menuIconsOffset to 5
        SystemProperties.setMenuIconsOffset(5);
        assertEquals(5, SystemProperties.getMenuIconsOffset());

        // Set system property to 1 and reset
        System.setProperty(SystemProperties.CASCIIAN_MENU_ICONS_OFFSET, "1");
        SystemProperties.reset();

        // After reset, should read from system property again
        assertEquals(1, SystemProperties.getMenuIconsOffset());
    }
}
