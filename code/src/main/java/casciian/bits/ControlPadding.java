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
package casciian.bits;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Named styles controlling the extra left/right padding of text and
 * selection controls such as TField, TPasswordField, TCheckBox,
 * TRadioButton, TList, and TTreeView.
 * <p>
 * Styles are selected via the {@code casciian.controls.padding} system
 * property and follow the same pattern as border styles and
 * {@code casciian.TButton.style}: the property is read directly by the
 * widget on each draw, so it can be toggled at runtime (e.g. from the
 * Desktop Styles dialog) without restarting the application.
 * </p>
 */
public enum ControlPadding {

    /**
     * No extra padding: controls are drawn flush against their left and
     * right edges, preserving the classic Casciian look.
     */
    NONE(0, "none"),

    /**
     * One blank cell of padding on the left and right side of each
     * control, matching the Turbo Vision style.  This is the default.
     */
    SINGLE(1, "single");

    /**
     * System property key that selects the active control padding style.
     */
    public static final String PROPERTY_KEY = "casciian.controls.padding";

    /**
     * Default style name used when the property is unset or invalid.
     */
    public static final String DEFAULT_STYLE_NAME = "single";

    /**
     * The number of blank cells reserved on each side of a padded
     * control.
     */
    private final int cells;

    /**
     * The canonical name of this style as used in system properties and
     * the Desktop Styles dialog.
     */
    private final String styleName;

    ControlPadding(final int cells, final String styleName) {
        this.cells = cells;
        this.styleName = styleName;
    }

    /**
     * Get the number of blank cells reserved on each side of the
     * control when this style is active.
     *
     * @return 0 for {@link #NONE}, 1 for {@link #SINGLE}
     */
    public int getCells() {
        return cells;
    }

    /**
     * Get the canonical name of this style.
     *
     * @return "none" or "single"
     */
    public String getStyleName() {
        return styleName;
    }

    /**
     * Resolve a style name into a ControlPadding value.  Unknown or
     * null values fall back to {@link #SINGLE}.
     *
     * @param styleName the style name, e.g. "single", "none", or
     *                  "default"
     * @return the matching ControlPadding, never null
     */
    public static ControlPadding fromStyleName(final String styleName) {
        if (styleName == null) {
            return SINGLE;
        }
        String key = styleName.toLowerCase();
        if (key.equals("default")) {
            return SINGLE;
        }
        for (ControlPadding p : values()) {
            if (p.styleName.equals(key)) {
                return p;
            }
        }
        return SINGLE;
    }

    /**
     * Read the currently active control padding style from the
     * {@link #PROPERTY_KEY} system property.  Callers should invoke
     * this on each draw so the style can be toggled dynamically.
     *
     * @return the active control padding style, never null
     */
    public static ControlPadding current() {
        return fromStyleName(System.getProperty(PROPERTY_KEY,
            DEFAULT_STYLE_NAME));
    }

    /**
     * Get the list of style names available for the Desktop Styles
     * dialog, in UI display order.
     *
     * @return an unmodifiable list of style names
     */
    public static List<String> getStyleNames() {
        return Collections.unmodifiableList(
            Arrays.asList(SINGLE.styleName, NONE.styleName));
    }
}
