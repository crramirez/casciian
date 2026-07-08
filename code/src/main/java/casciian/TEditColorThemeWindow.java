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

import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;

import casciian.bits.BorderStyle;
import casciian.bits.Color;
import casciian.bits.ColorTheme;
import casciian.bits.CellAttributes;
import casciian.bits.GraphicsChars;
import casciian.bits.Palette256;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import static casciian.TKeypress.*;
import static casciian.bits.ColorTheme.TLABEL;
import static casciian.bits.ColorTheme.TLABEL_ACTIVE;

/**
 * TEditColorThemeWindow provides an easy UI for users to alter the running
 * color theme.
 *
 */
public class TEditColorThemeWindow extends TWindow {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The name of the resource bundle for this class.
     */
    public static final String RESOURCE_BUNDLE_NAME = TEditColorThemeWindow.class.getName() + "Bundle";

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * The current editing theme.
     */
    private ColorTheme editTheme;

    /**
     * The left-side list of colors pane.
     */
    private TList colorNames;

    /**
     * The foreground color.
     */
    private final ColorPicker foreground;

    /**
     * The background color.
     */
    private final ColorPicker background;

    /**
     * The foreground color foreground.
     */
    class ColorPicker extends TWidget {

        /**
         * The label associated with this ColorPicker instance.
         * This string is used to describe or identify the color picker,
         * and may appear as a title or label in the user interface.
         */
        private final String label;

        /**
         * The selected color.
         */
        Color color;

        /**
         * The bright flag.
         */
        boolean bright;

        /**
         * The RGB background color.
         */
        TField rgb;

        /**
         * Public constructor.
         *
         * @param parent parent widget
         * @param x column relative to parent
         * @param y row relative to parent
         * @param width width of text area
         * @param height height of text area
         */
        public ColorPicker(final TWidget parent, final int x,
                           final int y, final int width, final int height, final String label) {

            super(parent, x, y, width, height);
            this.label = label;

            rgb = addLabelFor(i18n.getString("rgbHex"), 5, 6,
                addField(7, 6, 6, true, ""));
        }

        /**
         * Get the X grid coordinate for this color.
         *
         * @param color the Color value
         * @return the X coordinate
         */
        private int getXColorPosition(final Color color) {
            return TEditColorThemeWindow.getXColorPosition(color);
        }

        /**
         * Get the Y grid coordinate for this color.
         *
         * @param color the Color value
         * @param bright if true use bright color
         * @return the Y coordinate
         */
        private int getYColorPosition(final Color color, final boolean bright) {
            int dotY = 1;
            if (color.equals(Color.RED)) {
                dotY = 2;
            } else if (color.equals(Color.MAGENTA)) {
                dotY = 2;
            } else if (color.equals(Color.YELLOW)) {
                dotY = 2;
            } else if (color.equals(Color.WHITE)) {
                dotY = 2;
            }
            if (bright) {
                dotY += 2;
            }
            return dotY;
        }

        /**
         * Get the bright value based on Y grid coordinate.
         *
         * @param dotY the Y coordinate
         * @return the bright value
         */
        private boolean getBrightFromPosition(final int dotY) {
            return dotY > 2;
        }

        /**
         * Get the color based on (X, Y) grid coordinate.
         *
         * @param dotX the X coordinate
         * @param dotY the Y coordinate
         * @return the Color value
         */
        private Color getColorFromPosition(final int dotX, final int dotY) {
            int y = dotY;
            if (y > 2) {
                y -= 2;
            }
            if ((1 <= dotX) && (dotX <= 3) && (y == 1)) {
                return Color.BLACK;
            }
            if ((4 <= dotX) && (dotX <= 6) && (y == 1)) {
                return Color.BLUE;
            }
            if ((7 <= dotX) && (dotX <= 9) && (y == 1)) {
                return Color.GREEN;
            }
            if ((10 <= dotX) && (dotX <= 12) && (y == 1)) {
                return Color.CYAN;
            }
            if ((1 <= dotX) && (dotX <= 3) && (y == 2)) {
                return Color.RED;
            }
            if ((4 <= dotX) && (dotX <= 6) && (y == 2)) {
                return Color.MAGENTA;
            }
            if ((7 <= dotX) && (dotX <= 9) && (y == 2)) {
                return Color.YELLOW;
            }
            if ((10 <= dotX) && (dotX <= 12) && (y == 2)) {
                return Color.WHITE;
            }

            throw new IllegalArgumentException("Invalid coordinates: "
                + dotX + ", " + dotY);
        }

        /**
         * Draw the colors grid.
         */
        @Override
        public void draw() {
            CellAttributes border = getWindow().getBorder();
            CellAttributes background = getWindow().getBackground();
            CellAttributes attr = new CellAttributes();

            BorderStyle borderStyle;
            borderStyle = BorderStyle.getStyle(System.getProperty(
                "casciian.TEditColorTheme.options.borderStyle", "single"));

            drawBox(0, 0, getWidth(), getHeight(), border, background,
                borderStyle);

            attr.setTo(getWidgetColor(isActive() ? TLABEL_ACTIVE: TLABEL));
            if (borderStyle.equals(BorderStyle.NONE)) {
                putStringXY(0, 0, this.label, attr);
            } else {
                putStringXY(1, 0, this.label, attr);
            }

            // Have to draw the colors manually because the int value matches
            // SGR, not CGA.
            attr.reset();
            attr.setForeColor(Color.BLACK);
            putStringXY(1, 1, "\u2588\u2588\u2588", attr);
            attr.setForeColor(Color.BLUE);
            putStringXY(4, 1, "\u2588\u2588\u2588", attr);
            attr.setForeColor(Color.GREEN);
            putStringXY(7, 1, "\u2588\u2588\u2588", attr);
            attr.setForeColor(Color.CYAN);
            putStringXY(10, 1, "\u2588\u2588\u2588", attr);
            attr.setForeColor(Color.RED);
            putStringXY(1, 2, "\u2588\u2588\u2588", attr);
            attr.setForeColor(Color.MAGENTA);
            putStringXY(4, 2, "\u2588\u2588\u2588", attr);
            attr.setForeColor(Color.YELLOW);
            putStringXY(7, 2, "\u2588\u2588\u2588", attr);
            attr.setForeColor(Color.WHITE);
            putStringXY(10, 2, "\u2588\u2588\u2588", attr);

            attr.setForeColor(Color.BRIGHT_BLACK);
            putStringXY(1, 3, "\u2588\u2588\u2588", attr);
            attr.setForeColor(Color.BRIGHT_BLUE);
            putStringXY(4, 3, "\u2588\u2588\u2588", attr);
            attr.setForeColor(Color.BRIGHT_GREEN);
            putStringXY(7, 3, "\u2588\u2588\u2588", attr);
            attr.setForeColor(Color.BRIGHT_CYAN);
            putStringXY(10, 3, "\u2588\u2588\u2588", attr);
            attr.setForeColor(Color.BRIGHT_RED);
            putStringXY(1, 4, "\u2588\u2588\u2588", attr);
            attr.setForeColor(Color.BRIGHT_MAGENTA);
            putStringXY(4, 4, "\u2588\u2588\u2588", attr);
            attr.setForeColor(Color.BRIGHT_YELLOW);
            putStringXY(7, 4, "\u2588\u2588\u2588", attr);
            attr.setForeColor(Color.BRIGHT_WHITE);
            putStringXY(10, 4, "\u2588\u2588\u2588", attr);

            // Draw the dot
            int rgbColor = parseColorHex(rgb.text);
            if (rgbColor >= 0) {
                attr.reset();
                attr.setForeColorRGB(rgbColor);
                putStringXY(1, 6, "\u2588\u25D8\u2588", attr);
            } else {
                int dotX = getXColorPosition(color);
                int dotY = getYColorPosition(color, bright);
                if (color.equals(Color.BLACK) && !bright) {
                    // Use white-on-black for black.  All other colors use
                    // black-on-whatever.
                    attr.reset();
                    putCharXY(dotX, dotY, GraphicsChars.CP437[0x07], attr);
                } else {
                    attr.setForeColor(bright ? color.toBright() : color);
                    putCharXY(dotX, dotY, '\u25D8', attr);
                }
            }
        }

        /**
         * Handle keystrokes.
         *
         * @param keypress keystroke event
         */
        @Override
        public void onKeypress(final TKeypressEvent keypress) {
            if (rgb.isActive()) {
                rgb.onKeypress(keypress);
            } else if (keypress.equals(kbRight)) {
                int dotX = getXColorPosition(color);
                int dotY = getYColorPosition(color, bright);
                if (dotX < 10) {
                    dotX += 3;
                }
                color = getColorFromPosition(dotX, dotY);
                rgb.setText("");
            } else if (keypress.equals(kbLeft)) {
                int dotX = getXColorPosition(color);
                int dotY = getYColorPosition(color, bright);
                if (dotX > 3) {
                    dotX -= 3;
                }
                color = getColorFromPosition(dotX, dotY);
                rgb.setText("");
            } else if (keypress.equals(kbUp)) {
                int dotX = getXColorPosition(color);
                int dotY = getYColorPosition(color, bright);
                if (dotY > 1) {
                    dotY--;
                }
                color = getColorFromPosition(dotX, dotY);
                bright = getBrightFromPosition(dotY);
                rgb.setText("");
            } else if (keypress.equals(kbDown)) {
                int dotX = getXColorPosition(color);
                int dotY = getYColorPosition(color, bright);
                if (dotY < 4) {
                    dotY++;
                }
                color = getColorFromPosition(dotX, dotY);
                bright = getBrightFromPosition(dotY);
                rgb.setText("");
            } else {
                // Pass to my parent
                super.onKeypress(keypress);
            }

            // Save this update to the local theme.
            ((TEditColorThemeWindow) getWindow()).saveToEditTheme();
        }

        /**
         * Handle mouse press events.
         *
         * @param mouse mouse button press event
         */
        @Override
        public void onMouseDown(final TMouseEvent mouse) {
            if (mouse.isMouseWheelUp()) {
                // Do this like kbUp
                int dotX = getXColorPosition(color);
                int dotY = getYColorPosition(color, bright);
                if (dotY > 1) {
                    dotY--;
                }
                color = getColorFromPosition(dotX, dotY);
                bright = getBrightFromPosition(dotY);
                rgb.setText("");
            } else if (mouse.isMouseWheelDown()) {
                // Do this like kbDown
                int dotX = getXColorPosition(color);
                int dotY = getYColorPosition(color, bright);
                if (dotY < 4) {
                    dotY++;
                }
                color = getColorFromPosition(dotX, dotY);
                bright = getBrightFromPosition(dotY);
                rgb.setText("");
            } else if (mouse.isMouseWheelLeft()) {
                // Do this like kbLeft
                int dotX = getXColorPosition(color);
                int dotY = getYColorPosition(color, bright);
                if (dotX > 3) {
                    dotX -= 3;
                }
                color = getColorFromPosition(dotX, dotY);
                rgb.setText("");
            } else if (mouse.isMouseWheelRight()) {
                // Do this like kbRight
                int dotX = getXColorPosition(color);
                int dotY = getYColorPosition(color, bright);
                if (dotX < 10) {
                    dotX += 3;
                }
                color = getColorFromPosition(dotX, dotY);
                rgb.setText("");
            } else if ((mouse.getX() > 0)
                && (mouse.getX() < getWidth() - 1)
                && (mouse.getY() > 0)
                && (mouse.getY() < getHeight() - 3)
            ) {
                color = getColorFromPosition(mouse.getX(), mouse.getY());
                bright = getBrightFromPosition(mouse.getY());
                rgb.setText("");
            } else {
                // Let parent class handle it.
                super.onMouseDown(mouse);
            }

            // Save this update to the local theme.
            ((TEditColorThemeWindow) getWindow()).saveToEditTheme();
        }

    }

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.  The window will be centered on screen.
     *
     * @param application the TApplication that manages this window
     */
    @SuppressWarnings("this-escape")
    public TEditColorThemeWindow(final TApplication application) {

        // Register with the TApplication
        super(application, "", 0, 0, 60, 27, MODAL);
        i18n = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME,
            getLocale());
        setTitle(i18n.getString("windowTitle"));

        // Initialize with the first color
        List<String> colors = getTheme().getColorNames();
        assert (colors.size() > 0);
        editTheme = new ColorTheme();
        for (String key: colors) {
            CellAttributes attr = new CellAttributes();
            attr.setTo(getTheme().getColor(key));
            editTheme.setColor(key, attr);
        }

        colorNames = addList(colors, 2, 2, 38, getHeight() - 10,
            new TAction() {
                // When the user presses Enter
                public void DO() {
                    refreshFromTheme(colorNames.getSelected());
                }
            },
            new TAction() {
                // When the user navigates with keyboard
                public void DO() {
                    refreshFromTheme(colorNames.getSelected());
                }
            },
            new TAction() {
                // When the user navigates with keyboard
                public void DO() {
                    refreshFromTheme(colorNames.getSelected());
                }
            }
        );
        addLabel(i18n.getString("colorName"), 2, 1, colorNames);
        foreground = new ColorPicker(this, 42, 1, 14, 8, i18n.getString("foregroundLabel"));
        background = new ColorPicker(this, 42, 9, 14, 8, i18n.getString("backgroundLabel"));
        refreshFromTheme(colors.getFirst());
        colorNames.setSelectedIndex(0);

        TText tText = addText(i18n.getString("casciianrcHint"), 2, getHeight() - 7, getWidth() - 4, 3,
            "twindow.background.modal");
        tText.getHorizontalScroller().setVisible(false);
        tText.getVerticalScroller().setVisible(false);
        tText.setEnabled(false);

        addButton(i18n.getString("okButton"), getWidth() - 53, getHeight() - 4,
            new TAction() {
                public void DO() {
                    ColorTheme global = getTheme();
                    List<String> colors = editTheme.getColorNames();
                    for (String key: colors) {
                        CellAttributes attr = new CellAttributes();
                        attr.setTo(editTheme.getColor(key));
                        global.setColor(key, attr);
                    }
                    getApplication().closeWindow(TEditColorThemeWindow.this);
                }
            }
        );

        addButton(i18n.getString("loadButton"), getWidth() - 41,
            getHeight() - 4,
            new TAction() {
                public void DO() {
                    try {
                        String filename = null;
                        filename = fileOpenBox(".");
                        if (filename != null) {
                            editTheme.load(filename);
                            refreshFromTheme(colorNames.getSelected());
                        }
                    } catch (IOException e) {
                        new TExceptionDialog(getApplication(), e);
                    }
                }
            }
        );

        addButton(i18n.getString("saveButton"), getWidth() - 29,
            getHeight() - 4,
            new TAction() {
                public void DO() {
                    try {
                        String filename = null;
                        filename = fileSaveBox(".");
                        if (filename != null) {
                            editTheme.save(filename);
                        }
                    } catch (IOException e) {
                        new TExceptionDialog(getApplication(), e);
                    }
                }
            }
        );

        addButton(i18n.getString("cancelButton"), getWidth() - 17,
            getHeight() - 4,
            new TAction() {
                public void DO() {
                    getApplication().closeWindow(TEditColorThemeWindow.this);
                }
            }
        );

        // Default to the color list
        activate(colorNames);

        // Add shortcut text
        newStatusBar(i18n.getString("statusBar"));
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        // Escape - behave like cancel
        if (keypress.equals(kbEsc)) {
            getApplication().closeWindow(this);
            return;
        }

        // Pass to my parent
        super.onKeypress(keypress);
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw me on screen.
     */
    @Override
    public void draw() {
        super.draw();
        CellAttributes attr = new CellAttributes();

        // Draw the sample text box
        attr.reset();
        attr.setForeColor(foreground.bright
            ? foreground.color.toBright() : foreground.color);
        try {
            int foreColorRGB = parseColorHex(foreground.rgb.getText());
            if (foreColorRGB >= 0) {
                attr.setForeColorRGB(foreColorRGB);
            }
        } catch (NumberFormatException e) {
            // SQUASH
        }

        attr.setBackColor(background.bright
            ? background.color.toBright() : background.color);
        try {
            int backColorRGB = parseColorHex(background.rgb.getText());
            if (backColorRGB >= 0) {
                attr.setBackColorRGB(backColorRGB);
            }
        } catch (NumberFormatException e) {
            // SQUASH
        }
        putStringXY(getWidth() - 17, getHeight() - 9,
            i18n.getString("textTextText"), attr);
        putStringXY(getWidth() - 17, getHeight() - 8,
            i18n.getString("textTextText"), attr);
    }

    private static int parseColorHex(String text) {
        int color = -1;
        while (text.startsWith("#")) {
            text = text.substring(1);
        }
        if (text.length() == 6) {
            color = Integer.parseInt(text, 16);
        }
        return color;
    }

    // ------------------------------------------------------------------------
    // TEditColorThemeWindow --------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set various widgets/values to the editing theme color.
     *
     * @param colorName name of color from theme
     */
    private void refreshFromTheme(final String colorName) {
        CellAttributes attr = editTheme.getColor(colorName);

        foreground.color = attr.getForeColor().toNormal();

        if (attr.getForeColorRGB() >= 0) {
            foreground.rgb.setText(String.format("%06x",
                    attr.getForeColorRGB()));
        } else if (attr.getForeColorPalette() >= 0) {
            foreground.rgb.setText(String.format("%06x",
                    Palette256.toRgb(attr.getForeColorPalette())));
        } else {
            foreground.rgb.setText("");
        }

        // The "bright" toggle reflects only the actual bright foreground
        // color; this editor does not expose the (unrelated) bold attribute.
        foreground.bright = attr.getForeColor().isBright();

        background.color = attr.getBackColor().toNormal();

        if (attr.getBackColorRGB() >= 0) {
            background.rgb.setText(String.format("%06x",
                    attr.getBackColorRGB()));
        } else if (attr.getBackColorPalette() >= 0) {
            background.rgb.setText(String.format("%06x",
                    Palette256.toRgb(attr.getBackColorPalette())));
        } else {
            background.rgb.setText("");
        }

        // The "bright" toggle reflects only the actual bright background
        // color; this editor does not expose the (unrelated) bold attribute.
        background.bright = attr.getBackColor().isBright();
    }

    /**
     * Examines foreground, background, and colorNames and sets the color in
     * editTheme.
     */
    private void saveToEditTheme() {
        String colorName = colorNames.getSelected();
        if (colorName == null) {
            return;
        }
        CellAttributes attr = editTheme.getColor(colorName);
        attr.setForeColor(foreground.color);
        try {
            String text = foreground.rgb.getText();
            while (text.startsWith("#")) {
                text = text.substring(1);
            }
            if (text.length() > 0) {
                int foreColorRGB = Integer.parseInt(text, 16);
                if (foreColorRGB >= 0) {
                    int paletteIndex = Palette256.findExact(foreColorRGB);
                    if (paletteIndex >= 0) {
                        attr.setForeColorPalette(paletteIndex);
                    } else {
                        attr.setForeColorRGB(foreColorRGB);
                    }
                }
            }
        } catch (NumberFormatException e) {
            // SQUASH
        }
        // A bright selection is stored as the bright foreground color
        // directly, independent of the bold attribute (which this editor
        // does not expose).
        if ((attr.getForeColorRGB() < 0) && (attr.getForeColorPalette() < 0)
                && foreground.bright) {
            attr.setForeColor(foreground.color.toBright());
        }

        attr.setBackColor(background.color);
        try {
            String text = background.rgb.getText();
            while (text.startsWith("#")) {
                text = text.substring(1);
            }
            if (text.length() > 0) {
                int backColorRGB = Integer.parseInt(text, 16);
                if (backColorRGB >= 0) {
                    int paletteIndex = Palette256.findExact(backColorRGB);
                    if (paletteIndex >= 0) {
                        attr.setBackColorPalette(paletteIndex);
                    } else {
                        attr.setBackColorRGB(backColorRGB);
                    }
                }
            }
        } catch (NumberFormatException e) {
            // SQUASH
        }
        // A bright selection is stored as the bright background color
        // directly, independent of the bold attribute (which this editor
        // does not expose).
        if ((attr.getBackColorRGB() < 0) && (attr.getBackColorPalette() < 0)
            && background.bright) {
            attr.setBackColor(background.color.toBright());
        }

        editTheme.setColor(colorName, attr);
    }

    /**
     * Set the border style for the window when it is the foreground window.
     *
     * @param borderStyle the border style string, one of: "default", "none",
     * "single", "double", "singleVdoubleH", "singleHdoubleV", or "round"; or
     * null to use the value from casciian.TEditColorTheme.borderStyle.
     */
    @Override
    public void setBorderStyleForeground(final String borderStyle) {
        if (borderStyle == null) {
            String style = System.getProperty("casciian.TEditColorTheme.borderStyle",
                "double");
            super.setBorderStyleForeground(style);
        } else {
            super.setBorderStyleForeground(borderStyle);
        }
    }

    /**
     * Set the border style for the window when it is the modal window.
     *
     * @param borderStyle the border style string, one of: "default", "none",
     * "single", "double", "singleVdoubleH", "singleHdoubleV", or "round"; or
     * null to use the value from casciian.TEditColorTheme.borderStyle.
     */
    @Override
    public void setBorderStyleModal(final String borderStyle) {
        if (borderStyle == null) {
            String style = System.getProperty("casciian.TEditColorTheme.borderStyle",
                "double");
            super.setBorderStyleModal(style);
        } else {
            super.setBorderStyleModal(borderStyle);
        }
    }

    /**
     * Set the border style for the window when it is an inactive/background
     * window.
     *
     * @param borderStyle the border style string, one of: "default", "none",
     * "single", "double", "singleVdoubleH", "singleHdoubleV", or "round"; or
     * null to use the value from casciian.TEditColorTheme.borderStyle.
     */
    @Override
    public void setBorderStyleInactive(final String borderStyle) {
        if (borderStyle == null) {
            String style = System.getProperty("casciian.TEditColorTheme.borderStyle",
                "double");
            super.setBorderStyleInactive(style);
        } else {
            super.setBorderStyleInactive(borderStyle);
        }
    }

    /**
     * Set the border style for the window when it is being dragged/resize.
     *
     * @param borderStyle the border style string, one of: "default", "none",
     * "single", "double", "singleVdoubleH", "singleHdoubleV", or "round"; or
     * null to use the value from casciian.TEditColorTheme.borderStyle.
     */
    @Override
    public void setBorderStyleMoving(final String borderStyle) {
        if (borderStyle == null) {
            String style = System.getProperty("casciian.TEditColorTheme.borderStyle",
                "double");
            super.setBorderStyleMoving(style);
        } else {
            super.setBorderStyleMoving(borderStyle);
        }
    }

    /**
     * Get the X grid coordinate for this color.
     *
     * @param color the Color value
     * @return the X coordinate
     */
    private static int getXColorPosition(Color color) {
        if (color.equals(Color.BLACK)) {
            return 2;
        } else if (color.equals(Color.BLUE)) {
            return 5;
        } else if (color.equals(Color.GREEN)) {
            return 8;
        } else if (color.equals(Color.CYAN)) {
            return 11;
        } else if (color.equals(Color.RED)) {
            return 2;
        } else if (color.equals(Color.MAGENTA)) {
            return 5;
        } else if (color.equals(Color.YELLOW)) {
            return 8;
        } else if (color.equals(Color.WHITE)) {
            return 11;
        }
        throw new IllegalArgumentException("Invalid color: " + color);
    }

}
