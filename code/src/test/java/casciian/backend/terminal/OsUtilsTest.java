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

package casciian.backend.terminal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for OsUtils class.
 */
@DisplayName("OsUtils Tests")
class OsUtilsTest {

    @Test
    @DisplayName("isWindows returns true when os.name starts with Windows")
    void testIsWindowsReturnsTrue() {
        String originalOsName = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Windows 10");
            assertTrue(OsUtils.isWindows());
            
            System.setProperty("os.name", "Windows 11");
            assertTrue(OsUtils.isWindows());
            
            System.setProperty("os.name", "Windows Server 2019");
            assertTrue(OsUtils.isWindows());
        } finally {
            if (originalOsName != null) {
                System.setProperty("os.name", originalOsName);
            }
        }
    }

    @Test
    @DisplayName("isWindows returns false for Linux")
    void testIsWindowsReturnsFalseForLinux() {
        String originalOsName = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Linux");
            assertFalse(OsUtils.isWindows());
        } finally {
            if (originalOsName != null) {
                System.setProperty("os.name", originalOsName);
            }
        }
    }

    @Test
    @DisplayName("isWindows returns false for Mac OS X")
    void testIsWindowsReturnsFalseForMac() {
        String originalOsName = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Mac OS X");
            assertFalse(OsUtils.isWindows());
        } finally {
            if (originalOsName != null) {
                System.setProperty("os.name", originalOsName);
            }
        }
    }

    @Test
    @DisplayName("isWindows returns false for empty os.name")
    void testIsWindowsReturnsFalseForEmpty() {
        String originalOsName = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "");
            assertFalse(OsUtils.isWindows());
        } finally {
            if (originalOsName != null) {
                System.setProperty("os.name", originalOsName);
            }
        }
    }
}
