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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for the JLine terminal provider selection used by
 * {@link TerminalJlineImpl}.
 *
 * <p>The provider must be chosen at runtime rather than via a build-time
 * {@code -Dorg.jline.terminal.provider} native-image argument, because those
 * properties are not visible at native-image runtime. In a native image the
 * JNI provider must be avoided entirely so JLine's Windows codepage probe never
 * loads {@code org.jline.nativ.Kernel32}.
 */
@DisplayName("TerminalJlineImpl provider selection")
class TerminalJlineProviderTest {

    private static final String PROP_PROVIDER = "org.jline.terminal.provider";
    private static final String PROP_IMAGE_CODE = "org.graalvm.nativeimage.imagecode";

    private String originalProvider;
    private String originalImageCode;

    private void snapshot() {
        originalProvider = System.getProperty(PROP_PROVIDER);
        originalImageCode = System.getProperty(PROP_IMAGE_CODE);
    }

    private static void set(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    @AfterEach
    void restore() {
        set(PROP_PROVIDER, originalProvider);
        set(PROP_IMAGE_CODE, originalImageCode);
    }

    @Test
    @DisplayName("returns null (JLine default) on a regular JVM")
    void defaultsToNullOnJvm() {
        snapshot();
        System.clearProperty(PROP_PROVIDER);
        System.clearProperty(PROP_IMAGE_CODE);

        assertNull(TerminalJlineImpl.resolveProvider());
    }

    @Test
    @DisplayName("forces the FFM provider when running as a native image")
    void forcesFfmInNativeImage() {
        snapshot();
        System.clearProperty(PROP_PROVIDER);
        System.setProperty(PROP_IMAGE_CODE, "runtime");

        assertEquals("ffm", TerminalJlineImpl.resolveProvider());
    }

    @Test
    @DisplayName("an explicit provider override always wins")
    void explicitOverrideWins() {
        snapshot();
        System.setProperty(PROP_PROVIDER, "jni");
        System.setProperty(PROP_IMAGE_CODE, "runtime");

        assertEquals("jni", TerminalJlineImpl.resolveProvider());
    }

    @Test
    @DisplayName("a blank provider override is ignored")
    void blankOverrideIsIgnored() {
        snapshot();
        System.setProperty(PROP_PROVIDER, "   ");
        System.clearProperty(PROP_IMAGE_CODE);

        assertNull(TerminalJlineImpl.resolveProvider());
    }
}
