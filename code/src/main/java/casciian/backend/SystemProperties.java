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

import java.util.concurrent.atomic.AtomicReference;

/**
 * System properties used by Casciian.
 */
public class SystemProperties {

    /**
     * System property key for animations.
     * Valid values: "true" or "false"
     * Default: false
     */
    public static final String CASCIIAN_ANIMATIONS = "casciian.animations";

    /**
     * System property key for shadow opacity.
     * Valid values: 0-100 (percentage)
     * Default: 60
     */
    public static final String CASCIIAN_SHADOW_OPACITY = "casciian.shadowOpacity";

    /**
     * System property key for text mouse.
     * Valid values: "true" or "false"
     * Default: false
     */
    public static final String CASCIIAN_TEXT_MOUSE = "casciian.textMouse";

    /**
     * System property key for translucence.
     * Valid values: "true" or "false"
     * Default: false
     */
    public static final String CASCIIAN_TRANSLUCENCE = "casciian.translucence";

    /**
     * System property key for hiding mouse when typing.
     * Valid values: "true" or "false"
     * Default: false
     */
    public static final String CASCIIAN_HIDE_MOUSE_WHEN_TYPING = "casciian.hideMouseWhenTyping";

    /**
     * System property key for hiding the status bar.
     * Valid values: "true" or "false"
     * Default: false
     */
    public static final String CASCIIAN_HIDE_STATUS_BAR = "casciian.hideStatusBar";

    /**
     * System property key for hiding the menu bar.
     * Valid values: "true" or "false"
     * Default: false
     */
    public static final String CASCIIAN_HIDE_MENU_BAR = "casciian.hideMenuBar";

    /**
     * System property key for blink interval in milliseconds.
     * Valid values: 0-500 (milliseconds)
     * Default: 500
     */
    public static final String CASCIIAN_BLINK_MILLIS = "casciian.blinkMillis";

    /**
     * System property key for blink dim percentage.
     * Valid values: 0-100 (percentage)
     * Default: 80
     */
    public static final String CASCIIAN_BLINK_DIM_PERCENT = "casciian.blinkDimPercent";

    /**
     * System property key for text blink.
     * Valid values: "true" or "false"
     * Default: true
     */
    public static final String CASCIIAN_TEXT_BLINK = "casciian.textBlink";

    /**
     * System property key for menu icons.
     * Valid values: "true" or "false"
     * Default: true
     */
    public static final String CASCIIAN_MENU_ICONS = "casciian.menuIcons";

    /**
     * System property key for menu icons offset.
     * Valid values: 0-5
     * Default: 3
     */
    public static final String CASCIIAN_MENU_ICONS_OFFSET = "casciian.menuIconsOffset";

    /**
     * System property key for using terminal's native palette instead of CGA colors.
     * Valid values: "true" or "false"
     * Default: false (use CGA colors)
     */
    public static final String CASCIIAN_USE_TERMINAL_PALETTE = "casciian.useTerminalPalette";

    /**
     * System property key for disabling pre-transform cell effects (like gradients).
     * Valid values: "true" or "false"
     * Default: false (pre-transforms enabled)
     */
    public static final String CASCIIAN_DISABLE_PRE_TRANSFORM = "casciian.disablePreTransform";

    /**
     * System property key for disabling post-transform cell effects.
     * Valid values: "true" or "false"
     * Default: false (post-transforms enabled)
     */
    public static final String CASCIIAN_DISABLE_POST_TRANSFORM = "casciian.disablePostTransform";

    /**
     * Atomic reference representing the animations setting.
     * When true, enable animations.
     * The default value is false if not explicitly set.
     * A null value signals the property has not been read yet.
     */
    private static final AtomicReference<Boolean> animations = new AtomicReference<>(null);

    /**
     * Atomic reference representing the shadow opacity setting.
     * This value is expected to be a percentage ranging from 0 to 100, which determines
     * the opacity level of a shadow effect. The default value is 60 if not explicitly set.
     * Internal updates to this variable should ensure it stays within the valid range.
     * A null value signals the property has not been read yet.
     */
    private static final AtomicReference<Integer> shadowOpacity = new AtomicReference<>(null);

    /**
     * Atomic reference representing the text mouse setting.
     * When true, display a text-based mouse cursor.
     * The default value is false if not explicitly set.
     * A null value signals the property has not been read yet.
     */
    private static final AtomicReference<Boolean> textMouse = new AtomicReference<>(null);

    /**
     * Atomic reference representing the translucence setting.
     * When true, enable window translucency effects.
     * The default value is false if not explicitly set.
     * A null value signals the property has not been read yet.
     */
    private static final AtomicReference<Boolean> translucence = new AtomicReference<>(null);

    /**
     * Atomic reference representing the hide mouse when typing setting.
     * When true, hide mouse cursor when typing.
     * The default value is false if not explicitly set.
     * A null value signals the property has not been read yet.
     */
    private static final AtomicReference<Boolean> hideMouseWhenTyping = new AtomicReference<>(null);

    /**
     * Atomic reference representing the hide status bar setting.
     * When true, hide the status bar.
     * The default value is false if not explicitly set.
     * A null value signals the property has not been read yet.
     */
    private static final AtomicReference<Boolean> hideStatusBar = new AtomicReference<>(null);

    /**
     * Atomic reference representing the hide menu bar setting.
     * When true, hide the menu bar.
     * The default value is false if not explicitly set.
     * A null value signals the property has not been read yet.
     */
    private static final AtomicReference<Boolean> hideMenuBar = new AtomicReference<>(null);

    /**
     * Atomic reference representing the blink interval in milliseconds.
     * This value is expected to range from 0 to 500 milliseconds.
     * The default value is 500 if not explicitly set.
     * A null value signals the property has not been read yet.
     */
    private static final AtomicReference<Integer> blinkMillis = new AtomicReference<>(null);

    /**
     * Atomic reference representing the blink dim percentage.
     * This value is expected to be a percentage ranging from 0 to 100.
     * The default value is 80 if not explicitly set.
     * A null value signals the property has not been read yet.
     */
    private static final AtomicReference<Integer> blinkDimPercent = new AtomicReference<>(null);

    /**
     * Atomic reference representing the text blink setting.
     * When true, enable text blinking effects.
     * The default value is true if not explicitly set.
     * A null value signals the property has not been read yet.
     */
    private static final AtomicReference<Boolean> textBlink = new AtomicReference<>(null);

    /**
     * Atomic reference representing the menu icons setting.
     * When true, show icons in menus.
     * The default value is true if not explicitly set.
     * A null value signals the property has not been read yet.
     */
    private static final AtomicReference<Boolean> menuIcons = new AtomicReference<>(null);

    /**
     * Atomic reference representing the menu icons offset setting.
     * This value is expected to range from 0 to 5.
     * The default value is 3 if not explicitly set.
     * A null value signals the property has not been read yet.
     */
    private static final AtomicReference<Integer> menuIconsOffset = new AtomicReference<>(null);

    /**
     * Atomic reference representing the use terminal palette setting.
     * When true, use the terminal's native palette instead of CGA colors.
     * The default value is false if not explicitly set.
     * A null value signals the property has not been read yet.
     */
    private static final AtomicReference<Boolean> useTerminalPalette = new AtomicReference<>(null);

    /**
     * Atomic reference representing the disable pre-transform setting.
     * When true, disable pre-transform cell effects (like gradients).
     * The default value is false if not explicitly set.
     * A null value signals the property has not been read yet.
     */
    private static final AtomicReference<Boolean> disablePreTransform = new AtomicReference<>(null);

    /**
     * Atomic reference representing the disable post-transform setting.
     * When true, disable post-transform cell effects.
     * The default value is false if not explicitly set.
     * A null value signals the property has not been read yet.
     */
    private static final AtomicReference<Boolean> disablePostTransform = new AtomicReference<>(null);

    private SystemProperties() {
    }

    /**
     * Get a boolean property value from system properties with caching.
     *
     * @param cache the AtomicReference cache for the property
     * @param propertyKey the system property key
     * @param defaultValue the default value if not set
     * @return the boolean value from cache or system property
     */
    private static boolean getBooleanProperty(AtomicReference<Boolean> cache,
            String propertyKey, boolean defaultValue) {
        if (cache.get() == null) {
            cache.set(Boolean.parseBoolean(System.getProperty(propertyKey,
                String.valueOf(defaultValue))));
        }
        return cache.get();
    }

    /**
     * Set a boolean property value in system properties and cache.
     *
     * @param cache the AtomicReference cache for the property
     * @param propertyKey the system property key
     * @param value the value to set
     */
    private static void setBooleanProperty(AtomicReference<Boolean> cache,
            String propertyKey, boolean value) {
        System.setProperty(propertyKey, String.valueOf(value));
        cache.set(value);
    }

    /**
     * Get the animations value from system properties.
     *
     * @return true if animations are enabled, false otherwise. Default is false.
     */
    public static boolean isAnimations() {
        return getBooleanProperty(animations, CASCIIAN_ANIMATIONS, false);
    }

    /**
     * Set the animations value in system properties.
     *
     * @param value true to enable animations, false to disable
     */
    public static void setAnimations(boolean value) {
        setBooleanProperty(animations, CASCIIAN_ANIMATIONS, value);
    }

    /**
     * Get the shadow opacity value from system properties.
     *
     * @return shadow opacity value between 0 and 100, or default value of 60
     */
    public static int getShadowOpacity() {
        if (shadowOpacity.get() == null) {
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
     * @return true if text mouse is enabled, false otherwise. Default is false.
     */
    public static boolean isTextMouse() {
        return getBooleanProperty(textMouse, CASCIIAN_TEXT_MOUSE, false);
    }

    /**
     * Set the text mouse value in system properties.
     *
     * @param value true to enable text mouse, false to disable
     */
    public static void setTextMouse(boolean value) {
        setBooleanProperty(textMouse, CASCIIAN_TEXT_MOUSE, value);
    }

    /**
     * Get the translucence value from system properties.
     *
     * @return true if translucence is enabled, false otherwise. Default is false.
     */
    public static boolean isTranslucence() {
        return getBooleanProperty(translucence, CASCIIAN_TRANSLUCENCE, false);
    }

    /**
     * Set the translucence value in system properties.
     *
     * @param value true to enable translucence, false to disable
     */
    public static void setTranslucence(boolean value) {
        setBooleanProperty(translucence, CASCIIAN_TRANSLUCENCE, value);
    }

    /**
     * Get the hide mouse when typing value from system properties.
     *
     * @return true if hide mouse when typing is enabled, false otherwise. Default is false.
     */
    public static boolean isHideMouseWhenTyping() {
        return getBooleanProperty(hideMouseWhenTyping, CASCIIAN_HIDE_MOUSE_WHEN_TYPING, false);
    }

    /**
     * Set the hide mouse when typing value in system properties.
     *
     * @param value true to enable hide mouse when typing, false to disable
     */
    public static void setHideMouseWhenTyping(boolean value) {
        setBooleanProperty(hideMouseWhenTyping, CASCIIAN_HIDE_MOUSE_WHEN_TYPING, value);
    }

    /**
     * Get the hide status bar value from system properties.
     *
     * @return true if hide status bar is enabled, false otherwise. Default is false.
     */
    public static boolean isHideStatusBar() {
        return getBooleanProperty(hideStatusBar, CASCIIAN_HIDE_STATUS_BAR, false);
    }

    /**
     * Set the hide status bar value in system properties.
     *
     * @param value true to enable hide status bar, false to disable
     */
    public static void setHideStatusBar(boolean value) {
        setBooleanProperty(hideStatusBar, CASCIIAN_HIDE_STATUS_BAR, value);
    }

    /**
     * Get the hide menu bar value from system properties.
     *
     * @return true if hide menu bar is enabled, false otherwise. Default is false.
     */
    public static boolean isHideMenuBar() {
        return getBooleanProperty(hideMenuBar, CASCIIAN_HIDE_MENU_BAR, false);
    }

    /**
     * Set the hide menu bar value in system properties.
     *
     * @param value true to enable hide menu bar, false to disable
     */
    public static void setHideMenuBar(boolean value) {
        setBooleanProperty(hideMenuBar, CASCIIAN_HIDE_MENU_BAR, value);
    }

    /**
     * Get the blink interval in milliseconds from system properties.
     *
     * @return blink interval in milliseconds between 0 and 500, or default value of 500
     */
    public static int getBlinkMillis() {
        if (blinkMillis.get() == null) {
            final int defaultBlinkMillis = 500;
            int value = defaultBlinkMillis;
            try {
                value = Integer.parseInt(System.getProperty(CASCIIAN_BLINK_MILLIS,
                    String.valueOf(defaultBlinkMillis)));
            } catch (NumberFormatException e) {
                // SQUASH
            }
            value = Math.max(0, value);
            value = Math.min(value, 500);
            blinkMillis.set(value);
        }

        return blinkMillis.get();
    }

    /**
     * Set the blink interval in milliseconds in system properties.
     * Values outside the valid range (0-500) will be clamped to the nearest valid value.
     *
     * @param value blink interval in milliseconds (will be clamped to 0-500)
     */
    public static void setBlinkMillis(int value) {
        int clampedValue = Math.clamp(value, 0, 500);
        System.setProperty(CASCIIAN_BLINK_MILLIS, String.valueOf(clampedValue));
        blinkMillis.set(clampedValue);
    }

    /**
     * Get the blink dim percentage from system properties.
     *
     * @return blink dim percentage between 0 and 100, or default value of 80
     */
    public static int getBlinkDimPercent() {
        if (blinkDimPercent.get() == null) {
            final int defaultBlinkDimPercent = 80;
            int value = defaultBlinkDimPercent;
            try {
                value = Integer.parseInt(System.getProperty(CASCIIAN_BLINK_DIM_PERCENT,
                    String.valueOf(defaultBlinkDimPercent)));
            } catch (NumberFormatException e) {
                // SQUASH
            }
            value = Math.max(0, value);
            value = Math.min(value, 100);
            blinkDimPercent.set(value);
        }

        return blinkDimPercent.get();
    }

    /**
     * Set the blink dim percentage in system properties.
     * Values outside the valid range (0-100) will be clamped to the nearest valid value.
     *
     * @param value blink dim percentage (will be clamped to 0-100)
     */
    public static void setBlinkDimPercent(int value) {
        int clampedValue = Math.clamp(value, 0, 100);
        System.setProperty(CASCIIAN_BLINK_DIM_PERCENT, String.valueOf(clampedValue));
        blinkDimPercent.set(clampedValue);
    }

    /**
     * Get the text blink value from system properties.
     *
     * @return true if text blink is enabled, false otherwise. Default is true.
     */
    public static boolean isTextBlink() {
        return getBooleanProperty(textBlink, CASCIIAN_TEXT_BLINK, true);
    }

    /**
     * Set the text blink value in system properties.
     *
     * @param value true to enable text blink, false to disable
     */
    public static void setTextBlink(boolean value) {
        setBooleanProperty(textBlink, CASCIIAN_TEXT_BLINK, value);
    }

    /**
     * Get the menu icons value from system properties.
     *
     * @return true if menu icons are shown, false otherwise. Default is false.
     */
    public static boolean isMenuIcons() {
        return getBooleanProperty(menuIcons, CASCIIAN_MENU_ICONS, false);
    }

    /**
     * Set the menu icons value in system properties.
     *
     * @param value true to show menu icons, false to hide
     */
    public static void setMenuIcons(boolean value) {
        setBooleanProperty(menuIcons, CASCIIAN_MENU_ICONS, value);
    }

    /**
     * Get the menu icons offset value from system properties.
     *
     * @return menu icons offset between 0 and 5, or default value of 3
     */
    public static int getMenuIconsOffset() {
        if (menuIconsOffset.get() == null) {
            final int defaultMenuIconsOffset = 3;
            int value = defaultMenuIconsOffset;
            try {
                value = Integer.parseInt(System.getProperty(CASCIIAN_MENU_ICONS_OFFSET,
                    String.valueOf(defaultMenuIconsOffset)));
            } catch (NumberFormatException e) {
                // SQUASH
            }
            value = Math.max(0, value);
            value = Math.min(value, 5);
            menuIconsOffset.set(value);
        }

        return menuIconsOffset.get();
    }

    /**
     * Set the menu icons offset value in system properties.
     * Values outside the valid range (0-5) will be clamped to the nearest valid value.
     *
     * @param value menu icons offset (will be clamped to 0-5)
     */
    public static void setMenuIconsOffset(int value) {
        int clampedValue = value;
        clampedValue = Math.max(0, clampedValue);
        clampedValue = Math.min(clampedValue, 5);
        System.setProperty(CASCIIAN_MENU_ICONS_OFFSET, String.valueOf(clampedValue));
        menuIconsOffset.set(clampedValue);
    }

    /**
     * Get the use terminal palette value from system properties.
     *
     * @return true if terminal's native palette should be used, false to use CGA colors.
     *         Default is false.
     */
    public static boolean isUseTerminalPalette() {
        return getBooleanProperty(useTerminalPalette, CASCIIAN_USE_TERMINAL_PALETTE, false);
    }

    /**
     * Set the use terminal palette value in system properties.
     *
     * @param value true to use terminal's native palette, false to use CGA colors
     */
    public static void setUseTerminalPalette(boolean value) {
        setBooleanProperty(useTerminalPalette, CASCIIAN_USE_TERMINAL_PALETTE, value);
    }

    /**
     * Get the disable pre-transform value from system properties.
     *
     * @return true if pre-transform cell effects (like gradients) are disabled,
     *         false otherwise. Default is false.
     */
    public static boolean isDisablePreTransform() {
        return getBooleanProperty(disablePreTransform, CASCIIAN_DISABLE_PRE_TRANSFORM, false);
    }

    /**
     * Set the disable pre-transform value in system properties.
     *
     * @param value true to disable pre-transform cell effects, false to enable
     */
    public static void setDisablePreTransform(boolean value) {
        setBooleanProperty(disablePreTransform, CASCIIAN_DISABLE_PRE_TRANSFORM, value);
    }

    /**
     * Get the disable post-transform value from system properties.
     *
     * @return true if post-transform cell effects are disabled,
     *         false otherwise. Default is false.
     */
    public static boolean isDisablePostTransform() {
        return getBooleanProperty(disablePostTransform, CASCIIAN_DISABLE_POST_TRANSFORM, false);
    }

    /**
     * Set the disable post-transform value in system properties.
     *
     * @param value true to disable post-transform cell effects, false to enable
     */
    public static void setDisablePostTransform(boolean value) {
        setBooleanProperty(disablePostTransform, CASCIIAN_DISABLE_POST_TRANSFORM, value);
    }

    /**
     * Reset all cached system property values to their unset state.
     * This will force values to be re-read from system properties on the next access.
     */
    public static void reset() {
        animations.set(null);
        shadowOpacity.set(null);
        textMouse.set(null);
        translucence.set(null);
        hideMouseWhenTyping.set(null);
        hideStatusBar.set(null);
        hideMenuBar.set(null);
        blinkMillis.set(null);
        blinkDimPercent.set(null);
        textBlink.set(null);
        menuIcons.set(null);
        menuIconsOffset.set(null);
        useTerminalPalette.set(null);
        disablePreTransform.set(null);
        disablePostTransform.set(null);
    }
}
