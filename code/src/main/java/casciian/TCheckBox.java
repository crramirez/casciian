/*
 * Casciian - Java Text User Interface
 *
 * Original work written 2013–2025 by Autumn Lamonte
 * and dedicated to the public domain via CC0.
 *
 * Modifications and maintenance:
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
package casciian;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static casciian.TKeypress.kbEnter;
import static casciian.TKeypress.kbEsc;
import static casciian.TKeypress.kbSpace;
import casciian.bits.CellAttributes;
import casciian.bits.GraphicsChars;
import casciian.bits.MnemonicString;
import casciian.bits.StringUtils;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;

/**
 * TCheckBox implements an on/off checkbox.
 */
public class TCheckBox extends TWidget {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * System property key that selects the active checkbox style.
     */
    public static final String PROPERTY_KEY = "casciian.TCheckBox.style";

    /**
     * Default checkbox style name.
     */
    public static final String DEFAULT_STYLE_NAME = Style.UPPER_X.styleName;

    /**
     * Available checkbox styles.
     */
    public enum Style {

        /**
         * Use the classic square-root check mark.
         */
        CHECK("check", GraphicsChars.CHECK),

        /**
         * Use an uppercase X.
         */
        UPPER_X("upperx", 'X'),

        /**
         * Use a lowercase x.
         */
        LOWER_X("lowerx", 'x'),

        /**
         * Use a multiplication sign.
         */
        TIMES("times", '\u00D7');

        /**
         * The canonical style name.
         */
        private final String styleName;

        /**
         * The symbol rendered for the checked state.
         */
        private final char symbol;

        Style(final String styleName, final char symbol) {
            this.styleName = styleName;
            this.symbol = symbol;
        }

        /**
         * Get the checked-state symbol.
         *
         * @return the symbol to render between brackets
         */
        public char getSymbol() {
            return symbol;
        }

        /**
         * Resolve a style name.
         *
         * @param styleName the style name
         * @return the matching style, or CHECK for unknown values
         */
        public static Style fromStyleName(final String styleName) {
            if (styleName == null) {
                return CHECK;
            }
            String key = styleName.toLowerCase(Locale.ROOT);
            if (key.equals("default")) {
                return CHECK;
            }
            for (Style style: values()) {
                if (style.styleName.equals(key)) {
                    return style;
                }
            }
            return CHECK;
        }

        /**
         * Get style names for the desktop styles dialog.
         *
         * @return supported checkbox style names
         */
        public static List<String> getStyleNames() {
            return Collections.unmodifiableList(Arrays.asList(
                "default", CHECK.styleName, UPPER_X.styleName,
                LOWER_X.styleName, TIMES.styleName));
        }

        /**
         * Get the serialized style name.
         *
         * @return style name used for preferences and configuration
         */
        public String getStyleName() {
            return styleName;
        }
    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * CheckBox state, true means checked.
     */
    private boolean checked = false;

    /**
     * The shortcut and checkbox label.
     */
    private MnemonicString mnemonic;

    /**
     * The action to perform when the checkbox is toggled.
     */
    private TAction action;

    /**
     * If true, use the window's background color.
     */
    private boolean matchWindowBackground = false;

    /**
     * The style used for the checked-state symbol.
     */
    private Style style = Style.CHECK;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param label label to display next to (right of) the checkbox
     * @param checked initial check state
     */
    public TCheckBox(final TWidget parent, final int x, final int y,
        final String label, final boolean checked) {

        this(parent, x, y, label, checked, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param label label to display next to (right of) the checkbox
     * @param checked initial check state
     * @param action the action to perform when the checkbox is toggled
     */
    @SuppressWarnings("this-escape")
    public TCheckBox(final TWidget parent, final int x, final int y,
        final String label, final boolean checked, final TAction action) {

        this(parent, x, y, label, checked, action,
            parent.getTheme().getControlPadding().getCells());
    }

    /**
     * Private delegate that receives the pre-resolved padding value so
     * the super(...) width and the cached {@code padding} field are
     * guaranteed to agree.
     */
    @SuppressWarnings("this-escape")
    private TCheckBox(final TWidget parent, final int x, final int y,
        final String label, final boolean checked, final TAction action,
        final int padding) {

        // Set parent and window
        super(parent, x, y, StringUtils.width(label) + 4 + 2 * padding, 1);

        mnemonic = new MnemonicString(label);
        this.checked = checked;
        this.action = action;
        setStyle((String) null);

        setCursorVisible(true);
        setCursorX(padding + 1);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Returns true if the mouse is currently on the checkbox.
     *
     * @param mouse mouse event
     * @return true if the mouse is currently on the checkbox
     */
    private boolean mouseOnCheckBox(final TMouseEvent mouse) {
        return (mouse.getY() == 0)
            && (mouse.getX() >= 0)
            && (mouse.getX() < getWidth());
    }

    /**
     * Handle mouse checkbox presses.
     *
     * @param mouse mouse button down event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if ((mouseOnCheckBox(mouse)) && (mouse.isMouse1())) {
            // Switch state
            checked = !checked;
            dispatch();
        }
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.equals(kbSpace)
            || (keypress.equals(kbEnter) && !hasWindowDefaultButton())
        ) {
            checked = !checked;
            dispatch();
            return;
        }

        if (keypress.equals(kbEsc)) {
            checked = false;
            dispatch();
            return;
        }

        // Pass to parent for the things we don't care about.
        super.onKeypress(keypress);
    }

    /**
     * A checkbox toggles on Enter (like Space) only when the window has no
     * default button.  When a default button is set, Enter activates that
     * button and the checkbox is toggled with Space instead, so the keypress
     * must not be kept here.
     *
     * @param keypress keystroke event
     * @return true if this widget should handle the keypress first
     */
    @Override
    protected boolean receivesKeypressBeforeWindowDefaultButton(
        final TKeypressEvent keypress) {

        return keypress.equals(kbEnter) && !hasWindowDefaultButton();
    }

    /**
     * Returns true if this checkbox's window has a default button.
     *
     * @return true if a window default button is set
     */
    private boolean hasWindowDefaultButton() {
        TWindow window = getWindow();
        return (window != null) && (window.getDefaultButton() != null);
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw a checkbox with label.
     */
    @Override
    public void draw() {
        CellAttributes checkboxColor = new CellAttributes();
        CellAttributes mnemonicColor;
        int padding = getTheme().getControlPadding().getCells();
        int minimumWidth = StringUtils.width(mnemonic.getRawLabel()) + 4
            + 2 * padding;
        if (getWidth() < minimumWidth) {
            setWidth(minimumWidth);
        }
        setCursorX(padding + 1);

        if (isAbsoluteActive()) {
            checkboxColor.setTo(getWidgetColor("tcheckbox.active"));
            mnemonicColor = getWidgetColor("tcheckbox.mnemonic.highlighted");
        } else {
            checkboxColor.setTo(getWidgetColor("tcheckbox.inactive"));
            mnemonicColor = getWidgetColor("tcheckbox.mnemonic");
        }

        // Pulse color.
        if (isActive() && getWindow().isActive()
            && getApplication().hasAnimations()
        ) {
            checkboxColor.setPulse(true, false, 0);
            checkboxColor.setPulseColorRGB(getScreen().getBackend().
                attrToForegroundColor(getWidgetColor("tcheckbox.pulse")));

        }

        for (int i = 0; i < getWidth(); i++) {
            if (matchWindowBackground) {
                putForegroundCharXY(i, 0, ' ', checkboxColor);
            } else {
                putCharXY(i, 0, ' ', checkboxColor);
            }
        }

        if (matchWindowBackground) {
            putForegroundCharXY(padding, 0, '[', checkboxColor);
        } else {
            putCharXY(padding, 0, '[', checkboxColor);
        }
        if (checked) {
            if (matchWindowBackground) {
                putForegroundCharXY(padding + 1, 0, style.getSymbol(),
                    checkboxColor);
            } else {
                putCharXY(padding + 1, 0, style.getSymbol(), checkboxColor);
            }
        }
        if (matchWindowBackground) {
            putForegroundCharXY(padding + 2, 0, ']', checkboxColor);
            putForegroundCharXY(padding + 3, 0, ' ', checkboxColor);
            putForegroundStringXY(padding + 4, 0, mnemonic.getRawLabel(),
                checkboxColor);
        } else {
            putCharXY(padding + 2, 0, ']', checkboxColor);
            putCharXY(padding + 3, 0, ' ', checkboxColor);
            putStringXY(padding + 4, 0, mnemonic.getRawLabel(), checkboxColor);
        }
        if (mnemonic.getScreenShortcutIdx() >= 0) {
            if (matchWindowBackground) {
                putForegroundCharXY(padding + 4
                    + mnemonic.getScreenShortcutIdx(), 0,
                    mnemonic.getShortcut(), mnemonicColor);
            } else {
                putCharXY(padding + 4 + mnemonic.getScreenShortcutIdx(), 0,
                    mnemonic.getShortcut(), mnemonicColor);
            }
        }
    }

    // ------------------------------------------------------------------------
    // TCheckBox --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get checked value.
     *
     * @return if true, this is checked
     */
    public boolean isChecked() {
        return checked;
    }

    /**
     * Set checked value.
     *
     * @param checked new checked value.
     */
    public void setChecked(final boolean checked) {
        this.checked = checked;
    }

    /**
     * Get the mnemonic string for this checkbox.
     *
     * @return mnemonic string
     */
    public MnemonicString getMnemonic() {
        return mnemonic;
    }

    /**
     * Get the supported checkbox style names.
     *
     * @return supported checkbox style names
     */
    public static List<String> getStyleNames() {
        return Style.getStyleNames();
    }

    /**
     * Get the window background option.
     *
     * @return true if the window's background color will be used
     */
    public boolean isMatchWindowBackground() {
        return matchWindowBackground;
    }

    /**
     * Set the window background option.
     *
     * @param matchWindowBackground if true, the window's background color
     * will be used
     */
    public void setMatchWindowBackground(final boolean matchWindowBackground) {
        this.matchWindowBackground = matchWindowBackground;
    }

    /**
     * Set the checkbox style.
     *
     * @param style the checkbox style
     */
    public void setStyle(final Style style) {
        this.style = style;
    }

    /**
     * Set the checkbox style.
     *
     * @param checkboxStyle the checkbox style string, or null to use the
     * value from {@value #PROPERTY_KEY}
     */
    public void setStyle(final String checkboxStyle) {
        String styleString = System.getProperty(PROPERTY_KEY,
            DEFAULT_STYLE_NAME);
        if (checkboxStyle != null) {
            styleString = checkboxStyle;
        }
        style = Style.fromStyleName(styleString);
    }

    /**
     * Act as though the checkbox was pressed.  This is useful for other UI
     * elements to get the same action as if the user clicked the checkbox.
     */
    public void dispatch() {
        if (action != null) {
            action.DO(this);
        }
    }

}
