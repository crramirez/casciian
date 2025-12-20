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
}
