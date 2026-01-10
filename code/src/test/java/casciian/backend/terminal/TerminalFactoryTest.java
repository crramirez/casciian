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

import casciian.backend.SystemProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for TerminalFactory class.
 */
@DisplayName("TerminalFactory Tests")
class TerminalFactoryTest {

    private String originalOsName;

    @BeforeEach
    void setUp() {
        originalOsName = System.getProperty("os.name");
        SystemProperties.reset();
    }

    @AfterEach
    void tearDown() {
        if (originalOsName != null) {
            System.setProperty("os.name", originalOsName);
        }
        System.clearProperty(SystemProperties.CASCIIAN_USE_JLINE);
        SystemProperties.reset();
    }

    @Test
    @DisplayName("create returns TerminalJlineImpl on Windows")
    void testCreateReturnsJlineOnWindows() {
        System.setProperty("os.name", "Windows 10");
        SystemProperties.reset();
        
        Terminal terminal = TerminalFactory.create(false);
        assertNotNull(terminal);
        assertInstanceOf(TerminalJlineImpl.class, terminal);
        terminal.close();
    }

    @Test
    @DisplayName("create returns TerminalShImpl on Linux by default")
    void testCreateReturnsShImplOnLinuxByDefault() {
        System.setProperty("os.name", "Linux");
        SystemProperties.reset();
        
        Terminal terminal = TerminalFactory.create(false);
        assertNotNull(terminal);
        assertInstanceOf(TerminalShImpl.class, terminal);
        terminal.close();
    }

    @Test
    @DisplayName("create returns TerminalShImpl on Mac OS X by default")
    void testCreateReturnsShImplOnMacByDefault() {
        System.setProperty("os.name", "Mac OS X");
        SystemProperties.reset();
        
        Terminal terminal = TerminalFactory.create(false);
        assertNotNull(terminal);
        assertInstanceOf(TerminalShImpl.class, terminal);
        terminal.close();
    }

    @Test
    @DisplayName("create returns TerminalJlineImpl when casciian.useJline is true")
    void testCreateReturnsJlineWhenPropertySet() {
        System.setProperty("os.name", "Linux");
        System.setProperty(SystemProperties.CASCIIAN_USE_JLINE, "true");
        SystemProperties.reset();
        
        Terminal terminal = TerminalFactory.create(false);
        assertNotNull(terminal);
        assertInstanceOf(TerminalJlineImpl.class, terminal);
        terminal.close();
    }

    @Test
    @DisplayName("create returns TerminalShImpl when casciian.useJline is false on Linux")
    void testCreateReturnsShImplWhenPropertyFalse() {
        System.setProperty("os.name", "Linux");
        System.setProperty(SystemProperties.CASCIIAN_USE_JLINE, "false");
        SystemProperties.reset();
        
        Terminal terminal = TerminalFactory.create(false);
        assertNotNull(terminal);
        assertInstanceOf(TerminalShImpl.class, terminal);
        terminal.close();
    }

    @Test
    @DisplayName("create with debugToStderr true returns valid terminal")
    void testCreateWithDebugToStderrTrue() {
        System.setProperty("os.name", "Linux");
        SystemProperties.reset();
        
        Terminal terminal = TerminalFactory.create(true);
        assertNotNull(terminal);
        terminal.close();
    }
}
