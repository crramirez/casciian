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
package casciian.bits;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import static casciian.backend.SystemProperties.CASCIAN_PROPERTY_PREFIX;
import static casciian.backend.SystemProperties.CASCIIANRC_ENV_VAR;

/**
 * ColorTheme is a collection of colors keyed by string.  A default theme is
 * also provided that matches the blue-and-white theme used by Turbo Vision.
 */
public class ColorTheme {

    // Color key constants
    public static final String TWINDOW_BORDER = "twindow.border";
    public static final String TWINDOW_BACKGROUND = "twindow.background";
    public static final String TWINDOW_BORDER_INACTIVE = "twindow.border.inactive";
    public static final String TWINDOW_BACKGROUND_INACTIVE = "twindow.background.inactive";
    public static final String TWINDOW_BORDER_MODAL = "twindow.border.modal";
    public static final String TWINDOW_BACKGROUND_MODAL = "twindow.background.modal";
    public static final String TWINDOW_BORDER_MODAL_INACTIVE = "twindow.border.modal.inactive";
    public static final String TWINDOW_BACKGROUND_MODAL_INACTIVE = "twindow.background.modal.inactive";
    public static final String TWINDOW_BORDER_MODAL_WINDOWMOVE = "twindow.border.modal.windowmove";
    public static final String TWINDOW_BORDER_WINDOWMOVE = "twindow.border.windowmove";
    public static final String TWINDOW_BACKGROUND_WINDOWMOVE = "twindow.background.windowmove";
    public static final String TDESKTOP_BACKGROUND = "tdesktop.background";
    public static final String TBUTTON_INACTIVE = "tbutton.inactive";
    public static final String TBUTTON_ACTIVE = "tbutton.active";
    public static final String TBUTTON_DISABLED = "tbutton.disabled";
    public static final String TBUTTON_MNEMONIC = "tbutton.mnemonic";
    public static final String TBUTTON_MNEMONIC_HIGHLIGHTED = "tbutton.mnemonic.highlighted";
    public static final String TBUTTON_MNEMONIC_PULSE = "tbutton.mnemonic.pulse";
    public static final String TBUTTON_PULSE = "tbutton.pulse";
    public static final String TLABEL = "tlabel";
    public static final String TLABEL_MNEMONIC = "tlabel.mnemonic";
    public static final String TTEXT = "ttext";
    public static final String TFIELD_INACTIVE = "tfield.inactive";
    public static final String TFIELD_ACTIVE = "tfield.active";
    public static final String TFIELD_PULSE = "tfield.pulse";
    public static final String TCHECKBOX_INACTIVE = "tcheckbox.inactive";
    public static final String TCHECKBOX_ACTIVE = "tcheckbox.active";
    public static final String TCHECKBOX_MNEMONIC = "tcheckbox.mnemonic";
    public static final String TCHECKBOX_MNEMONIC_HIGHLIGHTED = "tcheckbox.mnemonic.highlighted";
    public static final String TCHECKBOX_PULSE = "tcheckbox.pulse";
    public static final String TCOMBOBOX_INACTIVE = "tcombobox.inactive";
    public static final String TCOMBOBOX_ACTIVE = "tcombobox.active";
    public static final String TSPINNER_INACTIVE = "tspinner.inactive";
    public static final String TSPINNER_ACTIVE = "tspinner.active";
    public static final String TCALENDAR_BACKGROUND = "tcalendar.background";
    public static final String TCALENDAR_DAY = "tcalendar.day";
    public static final String TCALENDAR_DAY_SELECTED = "tcalendar.day.selected";
    public static final String TCALENDAR_ARROW = "tcalendar.arrow";
    public static final String TCALENDAR_TITLE = "tcalendar.title";
    public static final String TPANEL_BORDER = "tpanel.border";
    public static final String TRADIOBUTTON_INACTIVE = "tradiobutton.inactive";
    public static final String TRADIOBUTTON_ACTIVE = "tradiobutton.active";
    public static final String TRADIOBUTTON_MNEMONIC = "tradiobutton.mnemonic";
    public static final String TRADIOBUTTON_MNEMONIC_HIGHLIGHTED = "tradiobutton.mnemonic.highlighted";
    public static final String TRADIOBUTTON_PULSE = "tradiobutton.pulse";
    public static final String TRADIOGROUP_INACTIVE = "tradiogroup.inactive";
    public static final String TRADIOGROUP_ACTIVE = "tradiogroup.active";
    public static final String TMENU = "tmenu";
    public static final String TMENU_HIGHLIGHTED = "tmenu.highlighted";
    public static final String TMENU_MNEMONIC = "tmenu.mnemonic";
    public static final String TMENU_MNEMONIC_HIGHLIGHTED = "tmenu.mnemonic.highlighted";
    public static final String TMENU_DISABLED = "tmenu.disabled";
    public static final String TPROGRESSBAR_COMPLETE = "tprogressbar.complete";
    public static final String TPROGRESSBAR_INCOMPLETE = "tprogressbar.incomplete";
    public static final String TSCROLLER_BAR = "tscroller.bar";
    public static final String TSCROLLER_ARROWS = "tscroller.arrows";
    public static final String TTREEVIEW = "ttreeview";
    public static final String TTREEVIEW_EXPANDBUTTON = "ttreeview.expandbutton";
    public static final String TTREEVIEW_SELECTED = "ttreeview.selected";
    public static final String TTREEVIEW_UNREADABLE = "ttreeview.unreadable";
    public static final String TTREEVIEW_INACTIVE = "ttreeview.inactive";
    public static final String TTREEVIEW_SELECTED_INACTIVE = "ttreeview.selected.inactive";
    public static final String TLIST = "tlist";
    public static final String TLIST_SELECTED = "tlist.selected";
    public static final String TLIST_UNREADABLE = "tlist.unreadable";
    public static final String TLIST_INACTIVE = "tlist.inactive";
    public static final String TLIST_SELECTED_INACTIVE = "tlist.selected.inactive";
    public static final String TSTATUSBAR_TEXT = "tstatusbar.text";
    public static final String TSTATUSBAR_BUTTON = "tstatusbar.button";
    public static final String TSTATUSBAR_SELECTED = "tstatusbar.selected";
    public static final String TEDITOR = "teditor";
    public static final String TEDITOR_SELECTED = "teditor.selected";
    public static final String TEDITOR_MARGIN = "teditor.margin";
    public static final String TTABLE_INACTIVE = "ttable.inactive";
    public static final String TTABLE_ACTIVE = "ttable.active";
    public static final String TTABLE_SELECTED = "ttable.selected";
    public static final String TTABLE_LABEL = "ttable.label";
    public static final String TTABLE_LABEL_SELECTED = "ttable.label.selected";
    public static final String TTABLE_BORDER = "ttable.border";
    public static final String TSPLITPANE = "tsplitpane";
    public static final String THELPWINDOW_WINDOWMOVE = "thelpwindow.windowmove";
    public static final String THELPWINDOW_BORDER = "thelpwindow.border";
    public static final String THELPWINDOW_BACKGROUND = "thelpwindow.background";
    public static final String THELPWINDOW_TEXT = "thelpwindow.text";
    public static final String THELPWINDOW_LINK = "thelpwindow.link";
    public static final String THELPWINDOW_LINK_ACTIVE = "thelpwindow.link.active";

    /**
     * The current theme colors.
     */
    private final SortedMap<String, CellAttributes> colors;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor sets the theme to the default.
     */
    @SuppressWarnings("this-escape")
    public ColorTheme() {
        colors = new TreeMap<>();
        setDefaultTheme();
        loadThemeFromCasciianRcFile();
    }


    /**
     * Loads properties from the CASCIIANRC file if defined.
     * Properties from the file are only set if they are not already defined
     * via -D JVM options (i.e., -D options take priority).
     * <p>
     * This method is called during class initialization and can also be called
     * after reset() for testing purposes.
     */
    private void loadThemeFromCasciianRcFile() {
        String rcFilePath = System.getenv(CASCIIANRC_ENV_VAR);
        if (rcFilePath == null || rcFilePath.isEmpty()) {
            return;
        }

        try {
            Path path = Paths.get(rcFilePath);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return;
            }

            load(rcFilePath);
        } catch (Exception e) {
            // Silently ignore any errors (including invalid paths) - similar to how dialog handles DIALOGRC
        }
    }

    // ------------------------------------------------------------------------
    // ColorTheme -------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Retrieve the CellAttributes for a named theme color.
     *
     * @param name theme color name, e.g. "twindow.border"
     * @return color associated with name, e.g. bold yellow on blue
     */
    public CellAttributes getColor(final String name) {
        return colors.get(name);
    }

    /**
     * Retrieve all the names in the theme.
     *
     * @return a list of names
     */
    public List<String> getColorNames() {
        Set<String> keys = colors.keySet();
        List<String> names = new ArrayList<>(keys.size());
        names.addAll(keys);
        return names;
    }

    /**
     * Set the color for a named theme color.
     *
     * @param name  theme color name, e.g. "twindow.border"
     * @param color the new color to associate with name, e.g. bold yellow on
     *              blue
     */
    public void setColor(final String name, final CellAttributes color) {
        colors.put(name, color);
    }

    /**
     * Save the color theme mappings to an ASCII file.
     *
     * @param filename file to write to
     * @throws IOException if the I/O fails
     */
    public void save(final String filename) throws IOException {
        try (FileWriter file = new FileWriter(filename)) {
            for (String key : colors.keySet()) {
                CellAttributes color = getColor(key);
                file.write("%s = %s%n".formatted(key, color));
            }
        }
    }

    /**
     * Read color theme mappings from an ASCII file.
     *
     * @param filename file to read from
     * @throws IOException if the I/O fails
     */
    public void load(final String filename) throws IOException {
        load(new FileReader(filename));
    }

    /**
     * Set a color based on a text string.  Color text string is of the form:
     * <code>[ bold ] [ blink ] { foreground on background }</code>
     *
     * @param key  the color key string
     * @param text the text string
     */
    public void setColorFromString(final String key, final String text) {
        boolean bold = false;
        boolean blink = false;
        String foreColor;
        String backColor;
        String token;

        StringTokenizer tokenizer = new StringTokenizer(text);
        token = tokenizer.nextToken();

        if (token.equalsIgnoreCase("rgb:")) {
            setRgbColorFromString(key, tokenizer);
            return;
        }

        while (token.equals("bold")
            || token.equals("bright")
            || token.equals("blink")
        ) {
            if (token.equals("bold") || token.equals("bright")) {
                bold = true;
                token = tokenizer.nextToken();
            }
            if (token.equals("blink")) {
                blink = true;
                token = tokenizer.nextToken();
            }
        }

        // What's left is "blah on blah"
        foreColor = token.toLowerCase();

        if (!tokenizer.nextToken().equalsIgnoreCase("on")) {
            // Invalid line.
            return;
        }
        backColor = tokenizer.nextToken().toLowerCase();

        CellAttributes color = new CellAttributes();
        if (bold) {
            color.setBold(true);
        }
        if (blink) {
            color.setBlink(true);
        }
        color.setForeColor(Color.getColor(foreColor));
        color.setBackColor(Color.getColor(backColor));
        colors.put(key, color);
    }

    private void setRgbColorFromString(String key, StringTokenizer tokenizer) {
        // Foreground
        int foreColorRGB;
        try {
            String rgbText = tokenizer.nextToken();
            while (rgbText.startsWith("#")) {
                rgbText = rgbText.substring(1);
            }
            foreColorRGB = Integer.parseInt(rgbText, 16);
        } catch (NumberFormatException e) {
            // Default to white on black
            foreColorRGB = 0xFFFFFF;
        }

        // "on"
        if (!tokenizer.nextToken().equalsIgnoreCase("on")) {
            // Invalid line.
            return;
        }

        // Background
        int backColorRGB;
        try {
            String rgbText = tokenizer.nextToken();
            while (rgbText.startsWith("#")) {
                rgbText = rgbText.substring(1);
            }
            backColorRGB = Integer.parseInt(rgbText, 16);
        } catch (NumberFormatException e) {
            backColorRGB = 0;
        }

        CellAttributes color = new CellAttributes();
        color.setForeColorRGB(foreColorRGB);
        color.setBackColorRGB(backColorRGB);
        colors.put(key, color);
    }

    /**
     * Read color theme mappings from a Reader.  The reader is closed at the
     * end.
     *
     * @param reader the reader to read from
     * @throws IOException if the I/O fails
     */
    public void load(final Reader reader) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line = bufferedReader.readLine();
            for (; line != null; line = bufferedReader.readLine()) {
                // Look for lines that resemble:
                //     "key = blah on blah"
                //     "key = bold blah on blah"
                //     "key = blink bold blah on blah"
                //     "key = bold blink blah on blah"
                //     "key = blink blah on blah"
                if (line.indexOf('=') == -1) {
                    // Invalid line.
                    continue;
                }
                String key = line.substring(0, line.indexOf('=')).trim();
                String text = line.substring(line.indexOf('=') + 1);

                if (!key.startsWith(CASCIAN_PROPERTY_PREFIX)) {
                    setColorFromString(key, text);
                }
            }
        }
    }

    /**
     * Sets to defaults that resemble the Borland IDE colors.
     */
    public void setDefaultTheme() {
        CellAttributes color;

        // TWindow border
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TWINDOW_BORDER, color);

        // TWindow background
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TWINDOW_BACKGROUND, color);

        // TWindow border - inactive
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TWINDOW_BORDER_INACTIVE, color);

        // TWindow background - inactive
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TWINDOW_BACKGROUND_INACTIVE, color);

        // TWindow border - modal
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.WHITE);
        color.setBold(true);
        colors.put(TWINDOW_BORDER_MODAL, color);

        // TWindow background - modal
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TWINDOW_BACKGROUND_MODAL, color);

        // TWindow border - modal + inactive
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(true);
        colors.put(TWINDOW_BORDER_MODAL_INACTIVE, color);

        // TWindow background - modal + inactive
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TWINDOW_BACKGROUND_MODAL_INACTIVE, color);

        // TWindow border - during window movement - modal
        color = new CellAttributes();
        color.setForeColor(Color.GREEN);
        color.setBackColor(Color.WHITE);
        color.setBold(true);
        colors.put(TWINDOW_BORDER_MODAL_WINDOWMOVE, color);

        // TWindow border - during window movement
        color = new CellAttributes();
        color.setForeColor(Color.GREEN);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TWINDOW_BORDER_WINDOWMOVE, color);

        // TWindow background - during window movement
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TWINDOW_BACKGROUND_WINDOWMOVE, color);

        // TDesktop background
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TDESKTOP_BACKGROUND, color);

        // TButton text
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.GREEN);
        color.setBold(false);
        colors.put(TBUTTON_INACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.CYAN);
        color.setBackColor(Color.GREEN);
        color.setBold(true);
        colors.put(TBUTTON_ACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(true);
        colors.put(TBUTTON_DISABLED, color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.GREEN);
        color.setBold(true);
        colors.put(TBUTTON_MNEMONIC, color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.GREEN);
        color.setBold(true);
        colors.put(TBUTTON_MNEMONIC_HIGHLIGHTED, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBold(true);
        colors.put(TBUTTON_MNEMONIC_PULSE, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBold(true);
        colors.put(TBUTTON_PULSE, color);

        // TLabel text
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TLABEL, color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TLABEL_MNEMONIC, color);

        // TText text
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TTEXT, color);

        // TField text
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TFIELD_INACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TFIELD_ACTIVE, color);
        color = new CellAttributes();
        // Just a small bit of amber.
        color.setForeColorRGB(0x8A610D);
        colors.put(TFIELD_PULSE, color);

        // TCheckBox
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TCHECKBOX_INACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put(TCHECKBOX_ACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TCHECKBOX_MNEMONIC, color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put(TCHECKBOX_MNEMONIC_HIGHLIGHTED, color);
        color = new CellAttributes();
        // Just a small bit of amber.
        color.setForeColorRGB(0x8A610D);
        colors.put(TCHECKBOX_PULSE, color);

        // TComboBox
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TCOMBOBOX_INACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TCOMBOBOX_ACTIVE, color);

        // TSpinner
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TSPINNER_INACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TSPINNER_ACTIVE, color);

        // TCalendar
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TCALENDAR_BACKGROUND, color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TCALENDAR_DAY, color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TCALENDAR_DAY_SELECTED, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TCALENDAR_ARROW, color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TCALENDAR_TITLE, color);

        // TPanel border
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        colors.put(TPANEL_BORDER, color);

        // TRadioButton
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TRADIOBUTTON_INACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put(TRADIOBUTTON_ACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TRADIOBUTTON_MNEMONIC, color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put(TRADIOBUTTON_MNEMONIC_HIGHLIGHTED, color);
        color = new CellAttributes();
        // Just a small bit of amber.
        color.setForeColorRGB(0x8A610D);
        colors.put(TRADIOBUTTON_PULSE, color);

        // TRadioGroup
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TRADIOGROUP_INACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TRADIOGROUP_ACTIVE, color);

        // TMenu
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TMENU, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.GREEN);
        color.setBold(false);
        colors.put(TMENU_HIGHLIGHTED, color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TMENU_MNEMONIC, color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.GREEN);
        color.setBold(false);
        colors.put(TMENU_MNEMONIC_HIGHLIGHTED, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(true);
        colors.put(TMENU_DISABLED, color);

        // TProgressBar
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TPROGRESSBAR_COMPLETE, color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TPROGRESSBAR_INCOMPLETE, color);

        // THScroller / TVScroller
        color = new CellAttributes();
        color.setForeColor(Color.CYAN);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TSCROLLER_BAR, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TSCROLLER_ARROWS, color);

        // TTreeView
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TTREEVIEW, color);
        color = new CellAttributes();
        color.setForeColor(Color.GREEN);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TTREEVIEW_EXPANDBUTTON, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TTREEVIEW_SELECTED, color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TTREEVIEW_UNREADABLE, color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TTREEVIEW_INACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TTREEVIEW_SELECTED_INACTIVE, color);

        // TList
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TLIST, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TLIST_SELECTED, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TLIST_UNREADABLE, color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TLIST_INACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TLIST_SELECTED_INACTIVE, color);

        // TStatusBar
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TSTATUSBAR_TEXT, color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TSTATUSBAR_BUTTON, color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TSTATUSBAR_SELECTED, color);

        // TEditor
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TEDITOR, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TEDITOR_SELECTED, color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put(TEDITOR_MARGIN, color);

        // TTable
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TTABLE_INACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TTABLE_ACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put(TTABLE_SELECTED, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TTABLE_LABEL, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TTABLE_LABEL_SELECTED, color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TTABLE_BORDER, color);

        // TSplitPane
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TSPLITPANE, color);

        // THelpWindow border - during window movement
        color = new CellAttributes();
        color.setForeColor(Color.GREEN);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put(THELPWINDOW_WINDOWMOVE, color);

        // THelpWindow border
        color = new CellAttributes();
        color.setForeColor(Color.GREEN);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put(THELPWINDOW_BORDER, color);

        // THelpWindow background
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put(THELPWINDOW_BACKGROUND, color);

        // THelpWindow text
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(THELPWINDOW_TEXT, color);

        // THelpWindow link
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(THELPWINDOW_LINK, color);

        // THelpWindow link - active
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put(THELPWINDOW_LINK_ACTIVE, color);
    }

    /**
     * Set the theme to femme.  I love pink.  You can too!  💗
     */
    public void setFemme() {
        setDefaultTheme();
        final int pink = 0xf7a8b8;
        final int blue = 0x55cdfc;
        final int pink2 = 0xd77888;

        for (var entry : colors.entrySet()) {
            var key = entry.getKey();
            var color = getFemmeCellAttributes(entry.getValue(), pink, blue);

            colors.put(key, color);
        }

        CellAttributes color;
        color = new CellAttributes();
        color.setForeColor(Color.MAGENTA);
        color.setBackColorRGB(pink2);
        color.setBold(false);
        colors.put(TWINDOW_BACKGROUND, color);
        colors.put(TWINDOW_BACKGROUND_INACTIVE, color);
        colors.put(TWINDOW_BACKGROUND_MODAL, color);
        colors.put(TWINDOW_BACKGROUND_MODAL_INACTIVE, color);
        colors.put(TWINDOW_BACKGROUND_WINDOWMOVE, color);

        color = new CellAttributes();
        color.setForeColor(Color.MAGENTA);
        color.setBold(true);
        colors.put(TWINDOW_BORDER, color);
        colors.put(TWINDOW_BORDER_INACTIVE, color);
        colors.put(TWINDOW_BORDER_MODAL, color);
        colors.put(TWINDOW_BORDER_MODAL_INACTIVE, color);

        color = new CellAttributes();
        color.setForeColorRGB(blue);
        colors.put(TWINDOW_BORDER_WINDOWMOVE, color);
        colors.put(TWINDOW_BORDER_MODAL_WINDOWMOVE, color);

        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBold(true);
        color.setBackColorRGB(pink);
        colors.put(TBUTTON_ACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColorRGB(pink);
        colors.put(TBUTTON_INACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBold(true);
        color = new CellAttributes();
        color.setForeColorRGB(blue);
        color.setBold(true);
        color.setBackColorRGB(pink);
        colors.put(TBUTTON_MNEMONIC_HIGHLIGHTED, color);
        color = new CellAttributes();
        color.setForeColorRGB(blue);
        color.setBold(true);
        color.setBackColorRGB(pink);
        colors.put(TBUTTON_MNEMONIC, color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        colors.put(TBUTTON_PULSE, color);
        color = new CellAttributes();
        color.setForeColorRGB(blue);
        color.setBold(true);
        colors.put(TBUTTON_MNEMONIC_PULSE, color);

        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColorRGB(pink2);
        color.setBold(true);
        colors.put(TPROGRESSBAR_COMPLETE, color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColorRGB(pink2);
        color.setBold(false);
        colors.put(TPROGRESSBAR_INCOMPLETE, color);

        color = new CellAttributes();
        color.setForeColor(Color.MAGENTA);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put(TPANEL_BORDER, color);

        color = new CellAttributes();
        color.setForeColor(Color.MAGENTA);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put(TRADIOGROUP_ACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.MAGENTA);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put(TRADIOBUTTON_ACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.MAGENTA);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put(TCHECKBOX_ACTIVE, color);
    }

    private static CellAttributes getFemmeCellAttributes(CellAttributes color, int pink, int blue) {
        Color fg = color.getForeColor();
        Color bg = color.getBackColor();
        boolean bold = color.isBold();
        if (bg.equals(Color.WHITE) && fg.equals(Color.BLACK)) {
            color.setForeColor(Color.MAGENTA);
            color.setBackColorRGB(pink);
        } else if (bg.equals(Color.WHITE) && fg.equals(Color.WHITE)) {
            color.setForeColor(Color.MAGENTA);
            color.setBackColorRGB(pink);
            color.setBold(true);
        } else if (bg.equals(Color.WHITE) && fg.equals(Color.GREEN)) {
            color.setForeColor(Color.BLUE);
            color.setBackColor(Color.BLACK);
            color.setBold(true);
        } else if (bg.equals(Color.WHITE) && fg.equals(Color.RED)) {
            color.setForeColorRGB(blue);
            color.setBackColorRGB(pink);
            color.setBold(true);
        } else if (bg.equals(Color.BLUE) && fg.equals(Color.CYAN)) {
            color.setForeColor(Color.RED);
            color.setBackColor(Color.MAGENTA);
            color.setBold(true);
        } else if (fg.equals(Color.BLUE) && bg.equals(Color.CYAN)) {
            color.setForeColor(Color.MAGENTA);
            color.setBackColor(Color.RED);
            color.setBold(true);
        } else if (bg.equals(Color.BLUE)) {
            color.setBackColor(Color.BLACK);
        } else if (bg.equals(Color.GREEN)) {
            color.setBackColor(Color.CYAN);
        } else if (fg.equals(Color.WHITE) && bold) {
            color.setForeColor(Color.RED);
        }
        return color;
    }

    /**
     * Sets to colors that resemble the "Custom" colors of Qmodem 5.0.
     */
    public void setQmodem5() {
        CellAttributes color;

        // TWindow border
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put(TWINDOW_BORDER, color);

        // TWindow background
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.CYAN);
        colors.put(TWINDOW_BACKGROUND, color);

        // TWindow border - inactive
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put(TWINDOW_BORDER_INACTIVE, color);

        // TWindow background - inactive
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.CYAN);
        colors.put(TWINDOW_BACKGROUND_INACTIVE, color);

        // TWindow border - modal
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.WHITE);
        color.setBold(true);
        colors.put(TWINDOW_BORDER_MODAL, color);

        // TWindow background - modal
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TWINDOW_BACKGROUND_MODAL, color);

        // TWindow border - modal + inactive
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(true);
        colors.put(TWINDOW_BORDER_MODAL_INACTIVE, color);

        // TWindow background - modal + inactive
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TWINDOW_BACKGROUND_MODAL_INACTIVE, color);

        // TWindow border - during window movement - modal
        color = new CellAttributes();
        color.setForeColor(Color.GREEN);
        color.setBackColor(Color.WHITE);
        color.setBold(true);
        colors.put(TWINDOW_BORDER_MODAL_WINDOWMOVE, color);

        // TWindow border - during window movement
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put(TWINDOW_BORDER_WINDOWMOVE, color);

        // TWindow background - during window movement
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TWINDOW_BACKGROUND_WINDOWMOVE, color);

        // TDesktop background
        color = new CellAttributes();
        color.setForeColor(Color.CYAN);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TDESKTOP_BACKGROUND, color);

        // TButton text
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.GREEN);
        color.setBold(false);
        colors.put(TBUTTON_INACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.CYAN);
        color.setBackColor(Color.GREEN);
        color.setBold(true);
        colors.put(TBUTTON_ACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(true);
        colors.put(TBUTTON_DISABLED, color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.GREEN);
        color.setBold(true);
        colors.put(TBUTTON_MNEMONIC, color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.GREEN);
        color.setBold(true);
        colors.put(TBUTTON_MNEMONIC_HIGHLIGHTED, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBold(true);
        colors.put(TBUTTON_MNEMONIC_PULSE, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBold(true);
        colors.put(TBUTTON_PULSE, color);

        // TLabel text
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TLABEL, color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TLABEL_MNEMONIC, color);

        // TText text
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TTEXT, color);

        // TField text
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TFIELD_INACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TFIELD_ACTIVE, color);
        color = new CellAttributes();
        // Just a small bit of amber.
        color.setForeColorRGB(0x8A610D);
        colors.put(TFIELD_PULSE, color);

        // TCheckBox
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TCHECKBOX_INACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put(TCHECKBOX_ACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TCHECKBOX_MNEMONIC, color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put(TCHECKBOX_MNEMONIC_HIGHLIGHTED, color);
        color = new CellAttributes();
        // Just a small bit of amber.
        color.setForeColorRGB(0x8A610D);
        colors.put(TCHECKBOX_PULSE, color);

        // TComboBox
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TCOMBOBOX_INACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TCOMBOBOX_ACTIVE, color);

        // TSpinner
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TSPINNER_INACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TSPINNER_ACTIVE, color);

        // TCalendar
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TCALENDAR_BACKGROUND, color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TCALENDAR_DAY, color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TCALENDAR_DAY_SELECTED, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TCALENDAR_ARROW, color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TCALENDAR_TITLE, color);

        // TPanel border
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put(TPANEL_BORDER, color);

        // TRadioButton
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TRADIOBUTTON_INACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put(TRADIOBUTTON_ACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TRADIOBUTTON_MNEMONIC, color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put(TRADIOBUTTON_MNEMONIC_HIGHLIGHTED, color);
        color = new CellAttributes();
        // Just a small bit of amber.
        color.setForeColorRGB(0x8A610D);
        colors.put(TRADIOBUTTON_PULSE, color);

        // TRadioGroup
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TRADIOGROUP_INACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TRADIOGROUP_ACTIVE, color);

        // TMenu
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TMENU, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.GREEN);
        color.setBold(false);
        colors.put(TMENU_HIGHLIGHTED, color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TMENU_MNEMONIC, color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.GREEN);
        color.setBold(false);
        colors.put(TMENU_MNEMONIC_HIGHLIGHTED, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(true);
        colors.put(TMENU_DISABLED, color);

        // TProgressBar
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put(TPROGRESSBAR_COMPLETE, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TPROGRESSBAR_INCOMPLETE, color);

        // THScroller / TVScroller
        color = new CellAttributes();
        color.setForeColor(Color.CYAN);
        color.setBackColor(Color.BLACK);
        color.setBold(false);
        colors.put(TSCROLLER_BAR, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TSCROLLER_ARROWS, color);

        // TTreeView
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TTREEVIEW, color);
        color = new CellAttributes();
        color.setForeColor(Color.GREEN);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TTREEVIEW_EXPANDBUTTON, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TTREEVIEW_SELECTED, color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TTREEVIEW_UNREADABLE, color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TTREEVIEW_INACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TTREEVIEW_SELECTED_INACTIVE, color);

        // TList
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TLIST, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TLIST_SELECTED, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TLIST_UNREADABLE, color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TLIST_INACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TLIST_SELECTED_INACTIVE, color);

        // TStatusBar
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TSTATUSBAR_TEXT, color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TSTATUSBAR_BUTTON, color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TSTATUSBAR_SELECTED, color);

        // TEditor
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TEDITOR, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TEDITOR_SELECTED, color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put(TEDITOR_MARGIN, color);

        // TTable
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TTABLE_INACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put(TTABLE_ACTIVE, color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put(TTABLE_SELECTED, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TTABLE_LABEL, color);
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put(TTABLE_LABEL_SELECTED, color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TTABLE_BORDER, color);

        // TSplitPane
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(TSPLITPANE, color);

        // THelpWindow border - during window movement
        color = new CellAttributes();
        color.setForeColor(Color.GREEN);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put(THELPWINDOW_WINDOWMOVE, color);

        // THelpWindow border
        color = new CellAttributes();
        color.setForeColor(Color.GREEN);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put(THELPWINDOW_BORDER, color);

        // THelpWindow background
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put(THELPWINDOW_BACKGROUND, color);

        // THelpWindow text
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put(THELPWINDOW_TEXT, color);

        // THelpWindow link
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(THELPWINDOW_LINK, color);

        // THelpWindow link - active
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put(THELPWINDOW_LINK_ACTIVE, color);

    }

    /**
     * Make human-readable description of this Cell.
     *
     * @return displayable String
     */
    @Override
    public String toString() {
        return colors.toString();
    }

}
