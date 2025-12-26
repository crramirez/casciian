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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * System properties used by Casciian.
 */
public class SystemProperties {

    /**
     * System property key for shadow opacity.
     * Valid values: 0-100 (percentage)
     * Default: 60
     */
    public static final String CASCIIAN_SHADOW_OPACITY = "casciian.shadowOpacity";

    /**
     * System property key for text mouse.
     * Valid values: "true" or "false"
     * Default: true
     */
    public static final String CASCIIAN_TEXT_MOUSE = "casciian.textMouse";

    /**
     * System property key for animations.
     * Valid values: "true" or "false"
     * Default: true
     */
    public static final String CASCIIAN_ANIMATIONS = "casciian.animations";

    /**
     * System property key for translucence.
     * Valid values: "true" or "false"
     * Default: true
     */
    public static final String CASCIIAN_TRANSLUCENCE = "casciian.translucence";

    /**
     * The default value for a property to signal was not set.
     */
    public static final int UNSET_PROPERTY = -1;

    /**
     * Atomic integer representing the shadow opacity setting.
     * This value is expected to be a percentage ranging from 0 to 100, which determines
     * the opacity level of a shadow effect. The default value is 60 if not explicitly set.
     * Internal updates to this variable should ensure it stays within the valid range.
     */
    private static final AtomicInteger shadowOpacity = new AtomicInteger(UNSET_PROPERTY);

    /**
     * Atomic reference representing the text mouse setting.
     * When true, display a text-based mouse cursor.
     * The default value is true if not explicitly set.
     * A null value signals the property has not been read yet.
     */
    private static final AtomicReference<Boolean> textMouse = new AtomicReference<>(null);

    /**
     * Atomic reference representing the animations setting.
     * When true, enable animations.
     * The default value is true if not explicitly set.
     * A null value signals the property has not been read yet.
     */
    private static final AtomicReference<Boolean> animations = new AtomicReference<>(null);

    /**
     * Atomic reference representing the translucence setting.
     * When true, enable window translucency effects.
     * The default value is true if not explicitly set.
     * A null value signals the property has not been read yet.
     */
    private static final AtomicReference<Boolean> translucence = new AtomicReference<>(null);

    private SystemProperties() {
    }

    /**
     * Get the shadow opacity value from system properties.
     *
     * @return shadow opacity value between 0 and 100, or default value of 60
     */
    public static int getShadowOpacity() {
        if (shadowOpacity.get() == UNSET_PROPERTY) {
            final int defaultShadowOpacity = 60;
            final int shadowOpacityValue = Integer.getInteger(CASCIIAN_SHADOW_OPACITY, defaultShadowOpacity);

            shadowOpacity.set(
                shadowOpacityValue >= 0 && shadowOpacityValue <= 100 ? shadowOpacityValue : defaultShadowOpacity);
        }

        return shadowOpacity.get();
    }

    /**
     * Set the shadow opacity value in system properties.
     * Values outside the valid range (0-100) will be clamped to the nearest valid value.
     *
     * @param value shadow opacity value (will be clamped to 0-100)
     */
    public static void setShadowOpacity(int value) {
        int clampedValue = Math.clamp(value, 0, 100);
        System.setProperty(CASCIIAN_SHADOW_OPACITY, String.valueOf(clampedValue));
        shadowOpacity.set(clampedValue);
    }

    /**
     * Get the text mouse value from system properties.
     *
     * @return true if text mouse is enabled, false otherwise. Default is true.
     */
    public static boolean isTextMouse() {
        if (textMouse.get() == null) {
            textMouse.set(Boolean.parseBoolean(System.getProperty(CASCIIAN_TEXT_MOUSE, "true")));
        }

        return textMouse.get();
    }

    /**
     * Set the text mouse value in system properties.
     *
     * @param value true to enable text mouse, false to disable
     */
    public static void setTextMouse(boolean value) {
        System.setProperty(CASCIIAN_TEXT_MOUSE, String.valueOf(value));
        textMouse.set(value);
    }

    /**
     * Get the animations value from system properties.
     *
     * @return true if animations are enabled, false otherwise. Default is true.
     */
    public static boolean isAnimations() {
        if (animations.get() == null) {
            animations.set(Boolean.parseBoolean(System.getProperty(CASCIIAN_ANIMATIONS, "true")));
        }

        return animations.get();
    }

    /**
     * Set the animations value in system properties.
     *
     * @param value true to enable animations, false to disable
     */
    public static void setAnimations(boolean value) {
        System.setProperty(CASCIIAN_ANIMATIONS, String.valueOf(value));
        animations.set(value);
    }

    /**
     * Get the translucence value from system properties.
     *
     * @return true if translucence is enabled, false otherwise. Default is true.
     */
    public static boolean isTranslucence() {
        if (translucence.get() == null) {
            translucence.set(Boolean.parseBoolean(System.getProperty(CASCIIAN_TRANSLUCENCE, "true")));
        }

        return translucence.get();
    }

    /**
     * Set the translucence value in system properties.
     *
     * @param value true to enable translucence, false to disable
     */
    public static void setTranslucence(boolean value) {
        System.setProperty(CASCIIAN_TRANSLUCENCE, String.valueOf(value));
        translucence.set(value);
    }

    /**
     * Reset all cached system property values to their unset state.
     * This will force values to be re-read from system properties on the next access.
     */
    public static void reset() {
        shadowOpacity.set(UNSET_PROPERTY);
        textMouse.set(null);
        animations.set(null);
        translucence.set(null);
    }
}
