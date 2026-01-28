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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for CASCIIANRC environment variable functionality.
 * These tests verify that properties can be loaded from a configuration file
 * specified by the CASCIIANRC environment variable.
 */
@DisplayName("CASCIIANRC Tests")
class CasciianRcTest {

    @TempDir
    Path tempDir;

    private Path rcFilePath;

    @BeforeEach
    void setUp() {
        rcFilePath = tempDir.resolve("casciianrc");
        // Clear all system properties before each test
        clearAllCasciianProperties();
        SystemProperties.reset();
    }

    @AfterEach
    void tearDown() {
        // Clear system properties after each test
        clearAllCasciianProperties();
        SystemProperties.reset();
    }

    private void clearAllCasciianProperties() {
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
        System.clearProperty(SystemProperties.CASCIIAN_USE_JLINE);
        System.clearProperty(SystemProperties.CASCIIAN_USE_TERMINAL_PALETTE);
        System.clearProperty(SystemProperties.CASCIIAN_DISABLE_PRE_TRANSFORM);
        System.clearProperty(SystemProperties.CASCIIAN_DISABLE_POST_TRANSFORM);
    }

    @Test
    @DisplayName("CASCIIANRC_ENV_VAR constant is defined correctly")
    void testCasciianRcEnvVarConstant() {
        assertEquals("CASCIIANRC", SystemProperties.CASCIIANRC_ENV_VAR);
    }

    @Test
    @DisplayName("Properties loaded from RC file")
    void testPropertiesLoadedFromRcFile() throws Exception {
        // Create a temp config file with properties
        String content = """
                casciian.animations=true
                casciian.shadowOpacity=75
                casciian.textMouse=true
                """;
        Files.writeString(rcFilePath, content);

        // Load properties from file
        SystemProperties.loadPropertiesFromFile(rcFilePath.toString());

        // Verify properties were loaded
        assertEquals("true", System.getProperty(SystemProperties.CASCIIAN_ANIMATIONS));
        assertEquals("75", System.getProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY));
        assertEquals("true", System.getProperty(SystemProperties.CASCIIAN_TEXT_MOUSE));
    }

    @Test
    @DisplayName("System property (-D) takes priority over RC file")
    void testSystemPropertyTakesPriorityOverRcFile() throws Exception {
        // Set a system property first (simulating -D option)
        System.setProperty(SystemProperties.CASCIIAN_ANIMATIONS, "false");
        System.setProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY, "50");

        // Create a temp config file with different properties
        String content = """
                casciian.animations=true
                casciian.shadowOpacity=75
                casciian.textMouse=true
                """;
        Files.writeString(rcFilePath, content);

        // Load properties from file
        SystemProperties.loadPropertiesFromFile(rcFilePath.toString());

        // Verify -D properties were preserved (not overwritten)
        assertEquals("false", System.getProperty(SystemProperties.CASCIIAN_ANIMATIONS));
        assertEquals("50", System.getProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY));
        // Property not set via -D should be loaded from file
        assertEquals("true", System.getProperty(SystemProperties.CASCIIAN_TEXT_MOUSE));
    }

    @Test
    @DisplayName("No error when file path is null")
    void testNoErrorWhenFilePathIsNull() {
        // Should not throw any exception
        SystemProperties.loadPropertiesFromFile(null);

        // Default values should still work
        assertFalse(SystemProperties.isAnimations());
        assertEquals(60, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("No error when file path is empty")
    void testNoErrorWhenFilePathIsEmpty() {
        // Should not throw any exception
        SystemProperties.loadPropertiesFromFile("");

        // Default values should still work
        assertFalse(SystemProperties.isAnimations());
        assertEquals(60, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("No error when RC file does not exist")
    void testNoErrorWhenFileDoesNotExist() {
        // Load from a non-existent file
        SystemProperties.loadPropertiesFromFile("/nonexistent/path/to/file");

        // Default values should still work
        assertFalse(SystemProperties.isAnimations());
        assertEquals(60, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Custom properties not defined in SystemProperties class are loaded")
    void testCustomPropertiesLoaded() throws Exception {
        // Create a temp config file with custom properties
        String content = """
                casciian.animations=true
                casciian.custom.property=customValue
                my.app.setting=12345
                """;
        Files.writeString(rcFilePath, content);

        // Load properties from file
        SystemProperties.loadPropertiesFromFile(rcFilePath.toString());

        // Verify all properties were loaded, including custom ones
        assertEquals("true", System.getProperty(SystemProperties.CASCIIAN_ANIMATIONS));
        assertEquals("customValue", System.getProperty("casciian.custom.property"));
        assertEquals("12345", System.getProperty("my.app.setting"));

        // Clean up
        System.clearProperty("casciian.custom.property");
        System.clearProperty("my.app.setting");
    }

    @Test
    @DisplayName("Empty RC file is handled gracefully")
    void testEmptyRcFileHandledGracefully() throws Exception {
        // Create an empty config file
        Files.writeString(rcFilePath, "");

        // Should not throw any exception
        SystemProperties.loadPropertiesFromFile(rcFilePath.toString());

        // Default values should still work
        assertFalse(SystemProperties.isAnimations());
        assertEquals(60, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("RC file with comments is handled correctly")
    void testRcFileWithComments() throws Exception {
        // Create a config file with comments
        String content = """
                # This is a comment
                casciian.animations=true
                # Another comment
                casciian.shadowOpacity=80
                ! This is also a comment (properties format)
                casciian.textMouse=true
                """;
        Files.writeString(rcFilePath, content);

        // Load properties from file
        SystemProperties.loadPropertiesFromFile(rcFilePath.toString());

        // Verify properties were loaded correctly
        assertEquals("true", System.getProperty(SystemProperties.CASCIIAN_ANIMATIONS));
        assertEquals("80", System.getProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY));
        assertEquals("true", System.getProperty(SystemProperties.CASCIIAN_TEXT_MOUSE));
    }

    @Test
    @DisplayName("All property types can be loaded from RC file")
    void testAllPropertyTypesLoaded() throws Exception {
        // Create a config file with all property types
        String content = """
                casciian.animations=true
                casciian.shadowOpacity=75
                casciian.textMouse=true
                casciian.translucence=true
                casciian.hideMouseWhenTyping=true
                casciian.hideStatusBar=true
                casciian.hideMenuBar=true
                casciian.blinkMillis=300
                casciian.blinkDimPercent=90
                casciian.textBlink=false
                casciian.menuIcons=true
                casciian.menuIconsOffset=4
                casciian.useTerminalPalette=true
                casciian.disablePreTransform=true
                casciian.disablePostTransform=true
                casciian.useJline=true
                """;
        Files.writeString(rcFilePath, content);

        // Load properties from file
        SystemProperties.loadPropertiesFromFile(rcFilePath.toString());

        // Verify all properties were loaded
        assertTrue(SystemProperties.isAnimations());
        assertEquals(75, SystemProperties.getShadowOpacity());
        assertTrue(SystemProperties.isTextMouse());
        assertTrue(SystemProperties.isTranslucence());
        assertTrue(SystemProperties.isHideMouseWhenTyping());
        assertTrue(SystemProperties.isHideStatusBar());
        assertTrue(SystemProperties.isHideMenuBar());
        assertEquals(300, SystemProperties.getBlinkMillis());
        assertEquals(90, SystemProperties.getBlinkDimPercent());
        assertFalse(SystemProperties.isTextBlink());
        assertTrue(SystemProperties.isMenuIcons());
        assertEquals(4, SystemProperties.getMenuIconsOffset());
        assertTrue(SystemProperties.isUseTerminalPalette());
        assertTrue(SystemProperties.isDisablePreTransform());
        assertTrue(SystemProperties.isDisablePostTransform());
        assertTrue(SystemProperties.isUseJline());
    }

    @Test
    @DisplayName("Directory path is handled gracefully")
    void testDirectoryPathHandledGracefully() {
        // Load from a directory instead of a file
        SystemProperties.loadPropertiesFromFile(tempDir.toString());

        // Default values should still work
        assertFalse(SystemProperties.isAnimations());
        assertEquals(60, SystemProperties.getShadowOpacity());
    }

    @Test
    @DisplayName("Properties with spaces around equals sign are handled")
    void testPropertiesWithSpaces() throws Exception {
        // Create a config file with various spacing
        String content = """
                casciian.animations = true
                casciian.shadowOpacity=75
                casciian.textMouse =true
                """;
        Files.writeString(rcFilePath, content);

        // Load properties from file
        SystemProperties.loadPropertiesFromFile(rcFilePath.toString());

        // Verify properties were loaded correctly (note: Java Properties handles spaces)
        assertEquals("true", System.getProperty(SystemProperties.CASCIIAN_ANIMATIONS));
        assertEquals("75", System.getProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY));
        assertEquals("true", System.getProperty(SystemProperties.CASCIIAN_TEXT_MOUSE));
    }

    @Test
    @DisplayName("Properties not loaded do not affect unset system properties")
    void testUnloadedPropertiesRemainNull() throws Exception {
        // Create a config file with only one property
        String content = """
                casciian.animations=true
                """;
        Files.writeString(rcFilePath, content);

        // Load properties from file
        SystemProperties.loadPropertiesFromFile(rcFilePath.toString());

        // Verify only the loaded property is set
        assertEquals("true", System.getProperty(SystemProperties.CASCIIAN_ANIMATIONS));
        // Other properties should remain null (not set as system properties)
        assertNull(System.getProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY));
        assertNull(System.getProperty(SystemProperties.CASCIIAN_TEXT_MOUSE));
    }

    @Test
    @DisplayName("Multiple loads do not override existing system properties")
    void testMultipleLoadsDoNotOverride() throws Exception {
        // Create first config file
        Path firstFile = tempDir.resolve("first.properties");
        Files.writeString(firstFile, """
                casciian.animations=true
                casciian.shadowOpacity=80
                """);

        // Create second config file with different values
        Path secondFile = tempDir.resolve("second.properties");
        Files.writeString(secondFile, """
                casciian.animations=false
                casciian.shadowOpacity=20
                casciian.textMouse=true
                """);

        // Load first file
        SystemProperties.loadPropertiesFromFile(firstFile.toString());

        // Load second file
        SystemProperties.loadPropertiesFromFile(secondFile.toString());

        // First file's properties should be preserved
        assertEquals("true", System.getProperty(SystemProperties.CASCIIAN_ANIMATIONS));
        assertEquals("80", System.getProperty(SystemProperties.CASCIIAN_SHADOW_OPACITY));
        // New property from second file should be loaded
        assertEquals("true", System.getProperty(SystemProperties.CASCIIAN_TEXT_MOUSE));
    }
}
