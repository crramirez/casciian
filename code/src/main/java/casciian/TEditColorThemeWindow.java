/*
 * Casciian - Java Text User Interface
 *
 * Written 2013-2025 by Autumn Lamonte
 *
 * To the extent possible under law, the author(s) have dedicated all
 * copyright and related and neighboring rights to this software to the
 * public domain worldwide. This software is distributed without any
 * warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along
 * with this software. If not, see
 * <http://creativecommons.org/publicdomain/zero/1.0/>.
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
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import static casciian.TKeypress.*;

/**
 * TEditColorThemeWindow provides an easy UI for users to alter the running
 * color theme.
 *
 */
public class TEditColorThemeWindow extends TWindow {

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
    private ForegroundPicker foreground;

    /**
     * The background color.
     */
    private BackgroundPicker background;

    /**
     * The foreground color picker.
     */
    class ForegroundPicker extends TWidget {

        /**
         * The selected color.
         */
        Color color;

        /**
         * The bold flag.
         */
        boolean bold;

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
        public ForegroundPicker(final TWidget parent, final int x,
            final int y, final int width, final int height) {

            super(parent, x, y, width, height);

            addLabel(i18n.getString("rgbHex"), 1, 6,
                "twindow.background.modal");

            rgb = addField(6, 6, 7, true, "");
        }

        /**
         * Get the X grid coordinate for this color.
         *
         * @param color the Color value
         * @return the X coordinate
         */
        private int getXColorPosition(final Color color) {
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

        /**
         * Get the Y grid coordinate for this color.
         *
         * @param color the Color value
         * @param bold if true use bold color
         * @return the Y coordinate
         */
        private int getYColorPosition(final Color color, final boolean bold) {
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
            if (bold) {
                dotY += 2;
            }
            return dotY;
        }

        /**
         * Get the bold value based on Y grid coordinate.
         *
         * @param dotY the Y coordinate
         * @return the bold value
         */
        private boolean getBoldFromPosition(final int dotY) {
            if (dotY > 2) {
                return true;
            }
            return false;
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
         * Draw the foreground colors grid.
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
                borderStyle, false);

            attr.setTo(getTheme().getColor("twindow.background.modal"));
            if (isActive()) {
                attr.setForeColor(getTheme().getColor("tlabel").getForeColor());
                attr.setBold(getTheme().getColor("tlabel").isBold());
            }
            if (borderStyle.equals(BorderStyle.NONE)) {
                putStringXY(0, 0, i18n.getString("foregroundLabel"), attr);
            } else {
                putStringXY(1, 0, i18n.getString("foregroundLabel"), attr);
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

            attr.setBold(true);
            attr.setForeColor(Color.BLACK);
            putStringXY(1, 3, "\u2588\u2588\u2588", attr);
            attr.setForeColor(Color.BLUE);
            putStringXY(4, 3, "\u2588\u2588\u2588", attr);
            attr.setForeColor(Color.GREEN);
            putStringXY(7, 3, "\u2588\u2588\u2588", attr);
            attr.setForeColor(Color.CYAN);
            putStringXY(10, 3, "\u2588\u2588\u2588", attr);
            attr.setForeColor(Color.RED);
            putStringXY(1, 4, "\u2588\u2588\u2588", attr);
            attr.setForeColor(Color.MAGENTA);
            putStringXY(4, 4, "\u2588\u2588\u2588", attr);
            attr.setForeColor(Color.YELLOW);
            putStringXY(7, 4, "\u2588\u2588\u2588", attr);
            attr.setForeColor(Color.WHITE);
            putStringXY(10, 4, "\u2588\u2588\u2588", attr);

            // Draw the dot
            int dotX = getXColorPosition(color);
            int dotY = getYColorPosition(color, bold);
            if (color.equals(Color.BLACK) && !bold) {
                // Use white-on-black for black.  All other colors use
                // black-on-whatever.
                attr.reset();
                putCharXY(dotX, dotY, GraphicsChars.CP437[0x07], attr);
            } else {
                attr.setForeColor(color);
                attr.setBold(bold);
                putCharXY(dotX, dotY, '\u25D8', attr);
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
                int dotY = getYColorPosition(color, bold);
                if (dotX < 10) {
                    dotX += 3;
                }
                color = getColorFromPosition(dotX, dotY);
                rgb.setText("");
            } else if (keypress.equals(kbLeft)) {
                int dotX = getXColorPosition(color);
                int dotY = getYColorPosition(color, bold);
                if (dotX > 3) {
                    dotX -= 3;
                }
                color = getColorFromPosition(dotX, dotY);
                rgb.setText("");
            } else if (keypress.equals(kbUp)) {
                int dotX = getXColorPosition(color);
                int dotY = getYColorPosition(color, bold);
                if (dotY > 1) {
                    dotY--;
                }
                color = getColorFromPosition(dotX, dotY);
                bold = getBoldFromPosition(dotY);
                rgb.setText("");
            } else if (keypress.equals(kbDown)) {
                int dotX = getXColorPosition(color);
                int dotY = getYColorPosition(color, bold);
                if (dotY < 4) {
                    dotY++;
                }
                color = getColorFromPosition(dotX, dotY);
                bold = getBoldFromPosition(dotY);
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
                int dotY = getYColorPosition(color, bold);
                if (dotY > 1) {
                    dotY--;
                }
                color = getColorFromPosition(dotX, dotY);
                bold = getBoldFromPosition(dotY);
                rgb.setText("");
            } else if (mouse.isMouseWheelDown()) {
                // Do this like kbDown
                int dotX = getXColorPosition(color);
                int dotY = getYColorPosition(color, bold);
                if (dotY < 4) {
                    dotY++;
                }
                color = getColorFromPosition(dotX, dotY);
                bold = getBoldFromPosition(dotY);
                rgb.setText("");
            } else if ((mouse.getX() > 0)
                && (mouse.getX() < getWidth() - 1)
                && (mouse.getY() > 0)
                && (mouse.getY() < getHeight() - 3)
            ) {
                color = getColorFromPosition(mouse.getX(), mouse.getY());
                bold = getBoldFromPosition(mouse.getY());
                rgb.setText("");
            } else {
                // Let parent class handle it.
                super.onMouseDown(mouse);
            }

            // Save this update to the local theme.
            ((TEditColorThemeWindow) getWindow()).saveToEditTheme();
        }

    }

    /**
     * The background color picker.
     */
    class BackgroundPicker extends TWidget {

        /**
         * The selected color.
         */
        Color color;

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
        public BackgroundPicker(final TWidget parent, final int x,
            final int y, final int width, final int height) {

            super(parent, x, y, width, height);

            addLabel(i18n.getString("rgbHex"), 1, 4,
                "twindow.background.modal");
            rgb = addField(6, 4, 7, true, "");
        }

        /**
         * Get the X grid coordinate for this color.
         *
         * @param color the Color value
         * @return the X coordinate
         */
        private int getXColorPosition(final Color color) {
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

        /**
         * Get the Y grid coordinate for this color.
         *
         * @param color the Color value
         * @return the Y coordinate
         */
        private int getYColorPosition(final Color color) {
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
            return dotY;
        }

        /**
         * Get the color based on (X, Y) grid coordinate.
         *
         * @param dotX the X coordinate
         * @param dotY the Y coordinate
         * @return the Color value
         */
        private Color getColorFromPosition(final int dotX, final int dotY) {
            if ((1 <= dotX) && (dotX <= 3) && (dotY == 1)) {
                return Color.BLACK;
            }
            if ((4 <= dotX) && (dotX <= 6) && (dotY == 1)) {
                return Color.BLUE;
            }
            if ((7 <= dotX) && (dotX <= 9) && (dotY == 1)) {
                return Color.GREEN;
            }
            if ((10 <= dotX) && (dotX <= 12) && (dotY == 1)) {
                return Color.CYAN;
            }
            if ((1 <= dotX) && (dotX <= 3) && (dotY == 2)) {
                return Color.RED;
            }
            if ((4 <= dotX) && (dotX <= 6) && (dotY == 2)) {
                return Color.MAGENTA;
            }
            if ((7 <= dotX) && (dotX <= 9) && (dotY == 2)) {
                return Color.YELLOW;
            }
            if ((10 <= dotX) && (dotX <= 12) && (dotY == 2)) {
                return Color.WHITE;
            }

            throw new IllegalArgumentException("Invalid coordinates: "
                + dotX + ", " + dotY);
        }

        /**
         * Draw the background colors grid.
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
                borderStyle, false);

            attr.setTo(getTheme().getColor("twindow.background.modal"));
            if (isActive()) {
                attr.setForeColor(getTheme().getColor("tlabel").getForeColor());
                attr.setBold(getTheme().getColor("tlabel").isBold());
            }
            if (borderStyle.equals(BorderStyle.NONE)) {
                putStringXY(0, 0, i18n.getString("backgroundLabel"), attr);
            } else {
                putStringXY(1, 0, i18n.getString("backgroundLabel"), attr);
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

            // Draw the dot
            int dotX = getXColorPosition(color);
            int dotY = getYColorPosition(color);
            if (color.equals(Color.BLACK)) {
                // Use white-on-black for black.  All other colors use
                // black-on-whatever.
                attr.reset();
                putCharXY(dotX, dotY, GraphicsChars.CP437[0x07], attr);
            } else {
                attr.setForeColor(color);
                putCharXY(dotX, dotY, '\u25D8', attr);
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
                int dotY = getYColorPosition(color);
                if (dotX < 10) {
                    dotX += 3;
                }
                color = getColorFromPosition(dotX, dotY);
                rgb.setText("");
            } else if (keypress.equals(kbLeft)) {
                int dotX = getXColorPosition(color);
                int dotY = getYColorPosition(color);
                if (dotX > 3) {
                    dotX -= 3;
                }
                color = getColorFromPosition(dotX, dotY);
                rgb.setText("");
            } else if (keypress.equals(kbUp)) {
                int dotX = getXColorPosition(color);
                int dotY = getYColorPosition(color);
                if (dotY == 2) {
                    dotY--;
                }
                color = getColorFromPosition(dotX, dotY);
                rgb.setText("");
            } else if (keypress.equals(kbDown)) {
                int dotX = getXColorPosition(color);
                int dotY = getYColorPosition(color);
                if (dotY == 1) {
                    dotY++;
                }
                color = getColorFromPosition(dotX, dotY);
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
                int dotY = getYColorPosition(color);
                if (dotY == 2) {
                    dotY--;
                }
                color = getColorFromPosition(dotX, dotY);
                rgb.setText("");
            } else if (mouse.isMouseWheelDown()) {
                // Do this like kbDown
                int dotX = getXColorPosition(color);
                int dotY = getYColorPosition(color);
                if (dotY == 1) {
                    dotY++;
                }
                color = getColorFromPosition(dotX, dotY);
                rgb.setText("");
            } else if ((mouse.getX() > 0)
                && (mouse.getX() < getWidth() - 1)
                && (mouse.getY() > 0)
                && (mouse.getY() < getHeight() - 3)
            ) {
                color = getColorFromPosition(mouse.getX(), mouse.getY());
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
        super(application, "", 0, 0, 60, 22, MODAL);
        i18n = ResourceBundle.getBundle(TEditColorThemeWindow.class.getName(),
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

        colorNames = addList(colors, 2, 2, 38, getHeight() - 7,
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
        foreground = new ForegroundPicker(this, 42, 1, 14, 8);
        background = new BackgroundPicker(this, 42, 9, 14, 6);
        refreshFromTheme(colors.get(0));
        colorNames.setSelectedIndex(0);

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

        // Draw the label on colorNames
        attr.setTo(getTheme().getColor("twindow.background.modal"));
        if (colorNames.isActive()) {
            attr.setForeColor(getTheme().getColor("tlabel").getForeColor());
            attr.setBold(getTheme().getColor("tlabel").isBold());
        }
        putStringXY(3, 2, i18n.getString("colorName"), attr);

        // Draw the sample text box
        attr.reset();
        attr.setBold(foreground.bold);
        attr.setForeColor(foreground.color);
        try {
            String text = foreground.rgb.getText();
            while (text.startsWith("#")) {
                text = text.substring(1);
            }
            if (text.length() > 0) {
                int foreColorRGB = Integer.parseInt(text, 16);
                if (foreColorRGB >= 0) {
                    attr.setForeColorRGB(foreColorRGB);
                }
            }
        } catch (NumberFormatException e) {
            // SQUASH
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
                    attr.setBackColorRGB(backColorRGB);
                }
            }
        } catch (NumberFormatException e) {
            // SQUASH
        }
        putStringXY(getWidth() - 17, getHeight() - 6,
            i18n.getString("textTextText"), attr);
        putStringXY(getWidth() - 17, getHeight() - 5,
            i18n.getString("textTextText"), attr);
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

        foreground.color = attr.getForeColor();

        if (attr.getForeColorRGB() >= 0) {
            foreground.rgb.setText(String.format("%06x",
                    attr.getForeColorRGB()));
        } else {
            foreground.rgb.setText("");
        }

        foreground.bold = attr.isBold();

        background.color = attr.getBackColor();

        if (attr.getBackColorRGB() >= 0) {
            background.rgb.setText(String.format("%06x",
                    attr.getBackColorRGB()));
        } else {
            foreground.rgb.setText("");
        }
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
                    attr.setForeColorRGB(foreColorRGB);
                }
            }
        } catch (NumberFormatException e) {
            // SQUASH
        }
        attr.setBold(foreground.bold);

        attr.setBackColor(background.color);
        try {
            String text = background.rgb.getText();
            while (text.startsWith("#")) {
                text = text.substring(1);
            }
            if (text.length() > 0) {
                int backColorRGB = Integer.parseInt(text, 16);
                if (backColorRGB >= 0) {
                    attr.setBackColorRGB(backColorRGB);
                }
            }
        } catch (NumberFormatException e) {
            // SQUASH
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

}
