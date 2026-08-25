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

import casciian.bits.CellAttributes;
import casciian.bits.ControlPadding;
import casciian.bits.GraphicsChars;
import casciian.bits.MnemonicString;
import casciian.bits.StringUtils;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import static casciian.TKeypress.*;

/**
 * TRadioButton implements a selectable radio button.
 *
 * If the user clicks or presses space on this button, it is selected.
 *
 * If the user presses escape on this button, it is unselected.
 */
public class TRadioButton extends TWidget {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * System property key that selects the active radio-button style.
     */
    public static final String PROPERTY_KEY = "casciian.TRadioButton.style";

    /**
     * Default radio-button style name.
     */
    public static final String DEFAULT_STYLE_NAME = Style.SMALL_BULLET.styleName;

    /**
     * Available radio-button styles.
     */
    public enum Style {

        /**
         * Use the classic bullet.
         */
        BULLET("bullet", GraphicsChars.CP437[0x07]),

        /**
         * Use the smaller bullet glyph.
         */
        SMALL_BULLET("smallbullet", GraphicsChars.CP437[0xF9]),

        /**
         * Use an asterisk.
         */
        ASTERISK("asterisk", '*');

        /**
         * The canonical style name.
         */
        private final String styleName;

        /**
         * The symbol rendered for the selected state.
         */
        private final char symbol;

        Style(final String styleName, final char symbol) {
            this.styleName = styleName;
            this.symbol = symbol;
        }

        /**
         * Get the selected-state symbol.
         *
         * @return the symbol to render between parentheses
         */
        public char getSymbol() {
            return symbol;
        }

        /**
         * Resolve a style name.
         *
         * @param styleName the style name
         * @return the matching style, or BULLET for unknown values
         */
        public static Style fromStyleName(final String styleName) {
            if (styleName == null) {
                return BULLET;
            }
            String key = styleName.toLowerCase(Locale.ROOT);
            if (key.equals("default")) {
                return BULLET;
            }
            for (Style style: values()) {
                if (style.styleName.equals(key)) {
                    return style;
                }
            }
            return BULLET;
        }

        /**
         * Get style names for the desktop styles dialog.
         *
         * @return supported radio-button style names
         */
        public static List<String> getStyleNames() {
            return Collections.unmodifiableList(Arrays.asList(
                "default", BULLET.styleName, SMALL_BULLET.styleName,
                ASTERISK.styleName));
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
     * RadioButton state, true means selected.  Note package private access.
     */
    boolean selected = false;

    /**
     * The shortcut and radio button label.
     */
    private MnemonicString mnemonic;

    /**
     * ID for this radio button.  Buttons start counting at 1 in the
     * RadioGroup.  Note package private access.
     */
    int id;

    /**
     * If true, use the window's background color.
     */
    private boolean matchWindowBackground = false;

    /**
     * The style used for the selected-state symbol.
     */
    private Style style = Style.BULLET;

    /**
     * Extra left/right padding applied to the control.  The value is
     * resolved once at construction from the active
     * {@link ControlPadding} style (system property
     * {@code casciian.controls.padding}).  The radio button content is
     * drawn offset by this amount from the left edge of the widget.
     */
    private final int padding;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Package private constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param label label to display next to (right of) the radiobutton
     * @param id ID for this radio button
     */
    TRadioButton(final TRadioGroup parent, final int x, final int y,
        final String label, final int id) {

        // Resolve padding once: ControlPadding.current() can be toggled
        // at runtime, but the widget size is fixed at construction, so
        // we only read the style a single time here to avoid any
        // width/padding mismatch.
        this(parent, x, y, label, id, ControlPadding.current().getCells());
    }

    /**
     * Private delegate that receives the pre-resolved padding value so
     * the super(...) width and the cached {@code padding} field are
     * guaranteed to agree.
     */
    @SuppressWarnings("this-escape")
    private TRadioButton(final TRadioGroup parent, final int x, final int y,
        final String label, final int id, final int padding) {

        // Set parent and window
        super(parent, x, y, StringUtils.width(label) + 4 + 2 * padding, 1);

        this.padding = padding;
        mnemonic = new MnemonicString(label);
        this.id = id;
        setStyle((String) null);

        setCursorVisible(true);
        setCursorX(padding + 1);

        parent.addRadioButton(this);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Returns true if the mouse is currently on the radio button.
     *
     * @param mouse mouse event
     * @return if true the mouse is currently on the radio button
     */
    private boolean mouseOnRadioButton(final TMouseEvent mouse) {
        if ((mouse.getY() == 0)
            && (mouse.getX() >= 0)
            && (mouse.getX() < getWidth())
        ) {
            return true;
        }
        return false;
    }

    /**
     * Handle mouse button presses.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if ((mouseOnRadioButton(mouse)) && (mouse.isMouse1())) {
            // Switch state
            ((TRadioGroup) getParent()).setSelected(id);
        }
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {

        if (keypress.equals(kbSpace)) {
            ((TRadioGroup) getParent()).setSelected(id);
            return;
        }

        if (keypress.equals(kbEsc)) {
            TRadioGroup parent = (TRadioGroup) getParent();
            if (parent.requiresSelection == false) {
                selected = false;
                parent.setSelected(0);
            }
            return;
        }

        // Pass to parent for the things we don't care about.
        super.onKeypress(keypress);
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Override TWidget's width: we can only set width at construction time.
     *
     * @param width new widget width (ignored)
     */
    @Override
    public void setWidth(final int width) {
        // Do nothing
    }

    /**
     * Override TWidget's height: we can only set height at construction
     * time.
     *
     * @param height new widget height (ignored)
     */
    @Override
    public void setHeight(final int height) {
        // Do nothing
    }

    /**
     * Draw a radio button with label.
     */
    @Override
    public void draw() {
        CellAttributes radioButtonColor = new CellAttributes();
        CellAttributes mnemonicColor;

        if (isAbsoluteActive()) {
            radioButtonColor.setTo(getWidgetColor("tradiobutton.active"));
            mnemonicColor = getWidgetColor("tradiobutton.mnemonic.highlighted");
        } else {
            radioButtonColor.setTo(getWidgetColor("tradiobutton.inactive"));
            mnemonicColor = getWidgetColor("tradiobutton.mnemonic");
        }

        // Pulse color.
        if (isActive() && getParent().isActive() && getWindow().isActive()
            && getApplication().hasAnimations()
        ) {
            radioButtonColor.setPulse(true, false, 0);
            radioButtonColor.setPulseColorRGB(getScreen().getBackend().
                attrToForegroundColor(getWidgetColor(
                    "tradiobutton.pulse")));
        }

        for (int i = 0; i < getWidth(); i++) {
            if (matchWindowBackground) {
                putForegroundCharXY(i, 0, ' ', radioButtonColor);
            } else {
                putCharXY(i, 0, ' ', radioButtonColor);
            }
        }

        if (matchWindowBackground) {
            putForegroundCharXY(padding, 0, '(', radioButtonColor);
        } else {
            putCharXY(padding, 0, '(', radioButtonColor);
        }
        if (selected) {
            if (matchWindowBackground) {
                putForegroundCharXY(padding + 1, 0, style.getSymbol(),
                    radioButtonColor);
            } else {
                putCharXY(padding + 1, 0, style.getSymbol(), radioButtonColor);
            }
        }
        if (matchWindowBackground) {
            putForegroundCharXY(padding + 2, 0, ')', radioButtonColor);
            putForegroundCharXY(padding + 3, 0, ' ', radioButtonColor);
            putForegroundStringXY(padding + 4, 0, mnemonic.getRawLabel(),
                radioButtonColor);
        } else {
            putCharXY(padding + 2, 0, ')', radioButtonColor);
            putCharXY(padding + 3, 0, ' ', radioButtonColor);
            putStringXY(padding + 4, 0, mnemonic.getRawLabel(),
                radioButtonColor);
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
    // TRadioButton -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get RadioButton state, true means selected.
     *
     * @return if true then this is the one button in the group that is
     * selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Set RadioButton state, true means selected.
     *
     * @param selected if true then this is the one button in the group that
     * is selected
     */
    public void setSelected(final boolean selected) {
        if (selected == true) {
            ((TRadioGroup) getParent()).setSelected(id);
        } else {
            ((TRadioGroup) getParent()).setSelected(0);
        }
    }

    /**
     * Get ID for this radio button.  Buttons start counting at 1 in the
     * RadioGroup.
     *
     * @return the ID
     */
    public int getId() {
        return id;
    }

    /**
     * Get the mnemonic string for this button.
     *
     * @return mnemonic string
     */
    public MnemonicString getMnemonic() {
        return mnemonic;
    }

    /**
     * Get the supported radio-button style names.
     *
     * @return supported radio-button style names
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
     * Set the radio-button style.
     *
     * @param style the radio-button style
     */
    public void setStyle(final Style style) {
        this.style = style;
    }

    /**
     * Set the radio-button style.
     *
     * @param radioButtonStyle the style string, or null to use the value from
     * {@value #PROPERTY_KEY}
     */
    public void setStyle(final String radioButtonStyle) {
        String styleString = System.getProperty(PROPERTY_KEY,
            DEFAULT_STYLE_NAME);
        if (radioButtonStyle != null) {
            styleString = radioButtonStyle;
        }
        style = Style.fromStyleName(styleString);
    }

    /**
     * Internal helper to widen the radio button to the group's content width.
     *
     * @param width the width to use for the widget
     */
    void setDisplayWidth(final int width) {
        super.setWidth(width);
    }

}
