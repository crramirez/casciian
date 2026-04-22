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

    /** Color key for TWindow border. */
    public static final String TWINDOW_BORDER = "twindow.border";

    /** Color key for TWindow background. */
    public static final String TWINDOW_BACKGROUND = "twindow.background";

    /** Color key for TWindow border when inactive. */
    public static final String TWINDOW_BORDER_INACTIVE = "twindow.border.inactive";

    /** Color key for TWindow background when inactive. */
    public static final String TWINDOW_BACKGROUND_INACTIVE = "twindow.background.inactive";

    /** Color key for TWindow border in modal mode. */
    public static final String TWINDOW_BORDER_MODAL = "twindow.border.modal";

    /** Color key for TWindow background in modal mode. */
    public static final String TWINDOW_BACKGROUND_MODAL = "twindow.background.modal";

    /** Color key for TWindow border when modal and inactive. */
    public static final String TWINDOW_BORDER_MODAL_INACTIVE = "twindow.border.modal.inactive";

    /** Color key for TWindow background when modal and inactive. */
    public static final String TWINDOW_BACKGROUND_MODAL_INACTIVE = "twindow.background.modal.inactive";

    /** Color key for TWindow border during window movement in modal mode. */
    public static final String TWINDOW_BORDER_MODAL_WINDOWMOVE = "twindow.border.modal.windowmove";

    /** Color key for TWindow border during window movement. */
    public static final String TWINDOW_BORDER_WINDOWMOVE = "twindow.border.windowmove";

    /** Color key for TWindow background during window movement. */
    public static final String TWINDOW_BACKGROUND_WINDOWMOVE = "twindow.background.windowmove";

    /** Color key for TDesktop background. */
    public static final String TDESKTOP_BACKGROUND = "tdesktop.background";

    /** Color key for TButton when inactive. */
    public static final String TBUTTON_INACTIVE = "tbutton.inactive";

    /** Color key for TButton when active. */
    public static final String TBUTTON_ACTIVE = "tbutton.active";

    /** Color key for TButton when disabled. */
    public static final String TBUTTON_DISABLED = "tbutton.disabled";

    /** Color key for TButton mnemonic character. */
    public static final String TBUTTON_MNEMONIC = "tbutton.mnemonic";

    /** Color key for TButton mnemonic character when highlighted. */
    public static final String TBUTTON_MNEMONIC_HIGHLIGHTED = "tbutton.mnemonic.highlighted";

    /** Color key for TButton mnemonic character pulse effect. */
    public static final String TBUTTON_MNEMONIC_PULSE = "tbutton.mnemonic.pulse";

    /** Color key for TButton pulse effect. */
    public static final String TBUTTON_PULSE = "tbutton.pulse";

    /** Color key for TLabel. */
    public static final String TLABEL = "tlabel";

    /** Color key for TLabel when active. */
    public static final String TLABEL_ACTIVE = "tlabel.active";

    /** Color key for TLabel when disabled. */
    public static final String TLABEL_DISABLED = "tlabel.disabled";

    /** Color key for TLabel mnemonic character. */
    public static final String TLABEL_MNEMONIC = "tlabel.mnemonic";

    /** Color key for TLabel mnemonic character when active. */
    public static final String TLABEL_ACTIVE_MNEMONIC = "tlabel.active.mnemonic";

    /** Color key for TLabel mnemonic character when disabled. */
    public static final String TLABEL_DISABLED_MNEMONIC = "tlabel.disabled.mnemonic";

    /** Color key for TLabel in modal mode. */
    public static final String TLABEL_MODAL = "tlabel.modal";

    /** Color key for TLabel when active in modal mode. */
    public static final String TLABEL_ACTIVE_MODAL = "tlabel.active.modal";

    /** Color key for TLabel when disabled in modal mode. */
    public static final String TLABEL_DISABLED_MODAL = "tlabel.disabled.modal";

    /** Color key for TLabel mnemonic character in modal mode. */
    public static final String TLABEL_MNEMONIC_MODAL = "tlabel.mnemonic.modal";

    /** Color key for TLabel mnemonic character when active in modal mode. */
    public static final String TLABEL_ACTIVE_MNEMONIC_MODAL = "tlabel.active.mnemonic.modal";

    /** Color key for TLabel mnemonic character when disabled in modal mode. */
    public static final String TLABEL_DISABLED_MNEMONIC_MODAL = "tlabel.disabled.mnemonic.modal";

    /** Color key for TText. */
    public static final String TTEXT = "ttext";

    /** Color key for TField when inactive. */
    public static final String TFIELD_INACTIVE = "tfield.inactive";

    /** Color key for TField when active. */
    public static final String TFIELD_ACTIVE = "tfield.active";

    /** Color key for TField pulse effect. */
    public static final String TFIELD_PULSE = "tfield.pulse";

    /** Color key for TCheckBox when inactive. */
    public static final String TCHECKBOX_INACTIVE = "tcheckbox.inactive";

    /** Color key for TCheckBox when active. */
    public static final String TCHECKBOX_ACTIVE = "tcheckbox.active";

    /** Color key for TCheckBox mnemonic character. */
    public static final String TCHECKBOX_MNEMONIC = "tcheckbox.mnemonic";

    /** Color key for TCheckBox mnemonic character when highlighted. */
    public static final String TCHECKBOX_MNEMONIC_HIGHLIGHTED = "tcheckbox.mnemonic.highlighted";

    /** Color key for TCheckBox pulse effect. */
    public static final String TCHECKBOX_PULSE = "tcheckbox.pulse";

    /** Color key for TComboBox when inactive. */
    public static final String TCOMBOBOX_INACTIVE = "tcombobox.inactive";

    /** Color key for TComboBox when active. */
    public static final String TCOMBOBOX_ACTIVE = "tcombobox.active";

    /** Color key for TSpinner when inactive. */
    public static final String TSPINNER_INACTIVE = "tspinner.inactive";

    /** Color key for TSpinner when active. */
    public static final String TSPINNER_ACTIVE = "tspinner.active";

    /** Color key for TCalendar background. */
    public static final String TCALENDAR_BACKGROUND = "tcalendar.background";

    /** Color key for TCalendar day. */
    public static final String TCALENDAR_DAY = "tcalendar.day";

    /** Color key for TCalendar selected day. */
    public static final String TCALENDAR_DAY_SELECTED = "tcalendar.day.selected";

    /** Color key for TCalendar navigation arrow. */
    public static final String TCALENDAR_ARROW = "tcalendar.arrow";

    /** Color key for TCalendar title. */
    public static final String TCALENDAR_TITLE = "tcalendar.title";

    /** Color key for TPanel border. */
    public static final String TPANEL_BORDER = "tpanel.border";

    /** Color key for TRadioButton when inactive. */
    public static final String TRADIOBUTTON_INACTIVE = "tradiobutton.inactive";

    /** Color key for TRadioButton when active. */
    public static final String TRADIOBUTTON_ACTIVE = "tradiobutton.active";

    /** Color key for TRadioButton mnemonic character. */
    public static final String TRADIOBUTTON_MNEMONIC = "tradiobutton.mnemonic";

    /** Color key for TRadioButton mnemonic character when highlighted. */
    public static final String TRADIOBUTTON_MNEMONIC_HIGHLIGHTED = "tradiobutton.mnemonic.highlighted";

    /** Color key for TRadioButton pulse effect. */
    public static final String TRADIOBUTTON_PULSE = "tradiobutton.pulse";

    /** Color key for TRadioGroup when inactive. */
    public static final String TRADIOGROUP_INACTIVE = "tradiogroup.inactive";

    /** Color key for TRadioGroup when active. */
    public static final String TRADIOGROUP_ACTIVE = "tradiogroup.active";

    /** Color key for TMenu. */
    public static final String TMENU = "tmenu";

    /** Color key for TMenu when highlighted. */
    public static final String TMENU_HIGHLIGHTED = "tmenu.highlighted";

    /** Color key for TMenu mnemonic character. */
    public static final String TMENU_MNEMONIC = "tmenu.mnemonic";

    /** Color key for TMenu mnemonic character when highlighted. */
    public static final String TMENU_MNEMONIC_HIGHLIGHTED = "tmenu.mnemonic.highlighted";

    /** Color key for TMenu when disabled. */
    public static final String TMENU_DISABLED = "tmenu.disabled";

    /** Color key for TProgressBar completed portion. */
    public static final String TPROGRESSBAR_COMPLETE = "tprogressbar.complete";

    /** Color key for TProgressBar incomplete portion. */
    public static final String TPROGRESSBAR_INCOMPLETE = "tprogressbar.incomplete";

    /** Color key for THScroller and TVScroller bar. */
    public static final String TSCROLLER_BAR = "tscroller.bar";

    /** Color key for THScroller and TVScroller arrows. */
    public static final String TSCROLLER_ARROWS = "tscroller.arrows";

    /** Color key for TTreeView. */
    public static final String TTREEVIEW = "ttreeview";

    /** Color key for TTreeView expand button. */
    public static final String TTREEVIEW_EXPANDBUTTON = "ttreeview.expandbutton";

    /** Color key for TTreeView selected item. */
    public static final String TTREEVIEW_SELECTED = "ttreeview.selected";

    /** Color key for TTreeView unreadable item. */
    public static final String TTREEVIEW_UNREADABLE = "ttreeview.unreadable";

    /** Color key for TTreeView when inactive. */
    public static final String TTREEVIEW_INACTIVE = "ttreeview.inactive";

    /** Color key for TTreeView selected item when inactive. */
    public static final String TTREEVIEW_SELECTED_INACTIVE = "ttreeview.selected.inactive";

    /** Color key for TList. */
    public static final String TLIST = "tlist";

    /** Color key for TList selected item. */
    public static final String TLIST_SELECTED = "tlist.selected";

    /** Color key for TList unreadable item. */
    public static final String TLIST_UNREADABLE = "tlist.unreadable";

    /** Color key for TList when inactive. */
    public static final String TLIST_INACTIVE = "tlist.inactive";

    /** Color key for TList selected item when inactive. */
    public static final String TLIST_SELECTED_INACTIVE = "tlist.selected.inactive";

    /** Color key for TStatusBar text. */
    public static final String TSTATUSBAR_TEXT = "tstatusbar.text";

    /** Color key for TStatusBar button. */
    public static final String TSTATUSBAR_BUTTON = "tstatusbar.button";

    /** Color key for TStatusBar selected item. */
    public static final String TSTATUSBAR_SELECTED = "tstatusbar.selected";

    /** Color key for TEditor. */
    public static final String TEDITOR = "teditor";

    /** Color key for TEditor selected text. */
    public static final String TEDITOR_SELECTED = "teditor.selected";

    /** Color key for TEditor margin. */
    public static final String TEDITOR_MARGIN = "teditor.margin";

    /** Color key for TTable when inactive. */
    public static final String TTABLE_INACTIVE = "ttable.inactive";

    /** Color key for TTable when active. */
    public static final String TTABLE_ACTIVE = "ttable.active";

    /** Color key for TTable selected item. */
    public static final String TTABLE_SELECTED = "ttable.selected";

    /** Color key for TTable label. */
    public static final String TTABLE_LABEL = "ttable.label";

    /** Color key for TTable selected label. */
    public static final String TTABLE_LABEL_SELECTED = "ttable.label.selected";

    /** Color key for TTable border. */
    public static final String TTABLE_BORDER = "ttable.border";

    /** Color key for TSplitPane. */
    public static final String TSPLITPANE = "tsplitpane";

    /** Color key for THelpWindow during window movement. */
    public static final String THELPWINDOW_WINDOWMOVE = "thelpwindow.windowmove";

    /** Color key for THelpWindow border. */
    public static final String THELPWINDOW_BORDER = "thelpwindow.border";

    /** Color key for THelpWindow background. */
    public static final String THELPWINDOW_BACKGROUND = "thelpwindow.background";

    /** Color key for THelpWindow text. */
    public static final String THELPWINDOW_TEXT = "thelpwindow.text";

    /** Color key for THelpWindow link. */
    public static final String THELPWINDOW_LINK = "thelpwindow.link";

    /** Color key for THelpWindow active link. */
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
        colors.put(TLABEL_ACTIVE, color);
        colors.put(TLABEL_DISABLED, color);
        colors.put(TLABEL_MODAL, color);
        colors.put(TLABEL_ACTIVE_MODAL, color);
        colors.put(TLABEL_DISABLED_MODAL, color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TLABEL_MNEMONIC, color);
        colors.put(TLABEL_ACTIVE_MNEMONIC, color);
        colors.put(TLABEL_DISABLED_MNEMONIC, color);
        colors.put(TLABEL_MNEMONIC_MODAL, color);
        colors.put(TLABEL_ACTIVE_MNEMONIC_MODAL, color);
        colors.put(TLABEL_DISABLED_MNEMONIC_MODAL, color);

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
    @SuppressWarnings("DuplicatedCode")
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

    @SuppressWarnings("SameParameterValue")
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
        colors.put(TLABEL_ACTIVE, color);
        colors.put(TLABEL_DISABLED, color);
        colors.put(TLABEL_MODAL, color);
        colors.put(TLABEL_ACTIVE_MODAL, color);
        colors.put(TLABEL_DISABLED_MODAL, color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put(TLABEL_MNEMONIC, color);
        colors.put(TLABEL_ACTIVE_MNEMONIC, color);
        colors.put(TLABEL_DISABLED_MNEMONIC, color);
        colors.put(TLABEL_MNEMONIC_MODAL, color);
        colors.put(TLABEL_ACTIVE_MNEMONIC_MODAL, color);
        colors.put(TLABEL_DISABLED_MNEMONIC_MODAL, color);

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

    // ------------------------------------------------------------------------
    // Additional preset themes ------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Build a CellAttributes with named fore/back colors.
     */
    private static CellAttributes attr(final Color fg, final Color bg,
        final boolean bold) {

        CellAttributes c = new CellAttributes();
        c.setForeColor(fg);
        c.setBackColor(bg);
        c.setBold(bold);
        return c;
    }

    /**
     * Build a CellAttributes with RGB fore/back colors (24-bit).
     */
    private static CellAttributes rgb(final int fgRGB, final int bgRGB) {
        CellAttributes c = new CellAttributes();
        c.setForeColorRGB(fgRGB);
        c.setBackColorRGB(bgRGB);
        return c;
    }

    /**
     * A dark variant of the default Borland-style theme.  The structural
     * feel is preserved (bright borders, coloured buttons, yellow mnemonics)
     * but the usual blue/white backgrounds are replaced with soft dark
     * greys so the UI reads as a modern dark theme on a 24-bit terminal
     * without the stark look of pure black surfaces.
     */
    public void setDarkDefault() {
        setDefaultTheme();

        // Dark palette: softened greys with a navy desktop and Borland-style
        // yellow mnemonics.  Full RGB so the look stays consistent regardless
        // of the terminal's 16-colour palette.
        final int bgDesktop  = 0x00004a; // deep navy desktop
        final int bgWindow   = 0x1e1e1e; // main window surface (was pure black)
        final int bgInactive = 0x2a2a2a; // inactive window (slightly lighter)
        final int bgPanel    = 0x252526; // panel / menu surface
        final int bgModal    = 0x1a3a66; // modal surface (muted blue)
        final int bgField    = 0x0a3a6e; // input field idle
        final int bgFieldHot = 0x0f5aa8; // input field / active selection
        final int bgSelect   = 0x264f78; // list / editor selection
        final int fgText     = 0xe0e0e0;
        final int fgMuted    = 0x909090;
        final int fgYellow   = 0xffcc33; // Borland yellow
        final int fgGreen    = 0x5ef07a;
        final int fgCyan     = 0x55ddee;
        final int fgRed      = 0xff6b6b;
        final int fgWhite    = 0xffffff;
        final int borderDim  = 0x606060;

        // Desktop: solid dark background.
        colors.put(TDESKTOP_BACKGROUND, rgb(fgCyan, bgDesktop));

        // Window borders and backgrounds: bright lines on dark grey.
        colors.put(TWINDOW_BORDER, rgb(fgWhite, bgWindow));
        colors.put(TWINDOW_BACKGROUND, rgb(fgYellow, bgWindow));
        colors.put(TWINDOW_BORDER_INACTIVE, rgb(fgMuted, bgInactive));
        colors.put(TWINDOW_BACKGROUND_INACTIVE, rgb(fgText, bgInactive));
        colors.put(TWINDOW_BORDER_WINDOWMOVE, rgb(fgGreen, bgWindow));
        colors.put(TWINDOW_BACKGROUND_WINDOWMOVE, rgb(fgYellow, bgWindow));

        // Modal windows: use muted blue surface to stand out from the desktop.
        colors.put(TWINDOW_BORDER_MODAL, rgb(fgWhite, bgModal));
        colors.put(TWINDOW_BACKGROUND_MODAL, rgb(fgWhite, bgModal));
        colors.put(TWINDOW_BORDER_MODAL_INACTIVE, rgb(fgText, bgModal));
        colors.put(TWINDOW_BACKGROUND_MODAL_INACTIVE, rgb(fgText, bgModal));
        colors.put(TWINDOW_BORDER_MODAL_WINDOWMOVE, rgb(fgGreen, bgModal));

        // Labels / text on the dark window background.
        colors.put(TLABEL, rgb(fgWhite, bgWindow));
        colors.put(TLABEL_MNEMONIC, rgb(fgYellow, bgWindow));
        colors.put(TTEXT, rgb(fgText, bgWindow));

        // Fields: dark input with a subtle highlight when active.
        colors.put(TFIELD_INACTIVE, rgb(fgText, bgField));
        colors.put(TFIELD_ACTIVE, rgb(fgWhite, bgFieldHot));

        // Check boxes / radio buttons / combos: match dark background.
        colors.put(TCHECKBOX_INACTIVE, rgb(fgText, bgWindow));
        colors.put(TCHECKBOX_ACTIVE, rgb(fgYellow, bgSelect));
        colors.put(TCHECKBOX_MNEMONIC, rgb(fgYellow, bgWindow));
        colors.put(TCHECKBOX_MNEMONIC_HIGHLIGHTED, rgb(fgYellow, bgSelect));
        colors.put(TRADIOBUTTON_INACTIVE, rgb(fgText, bgWindow));
        colors.put(TRADIOBUTTON_ACTIVE, rgb(fgYellow, bgSelect));
        colors.put(TRADIOBUTTON_MNEMONIC, rgb(fgYellow, bgWindow));
        colors.put(TRADIOBUTTON_MNEMONIC_HIGHLIGHTED, rgb(fgYellow, bgSelect));
        colors.put(TRADIOGROUP_INACTIVE, rgb(fgText, bgWindow));
        colors.put(TRADIOGROUP_ACTIVE, rgb(fgYellow, bgWindow));
        colors.put(TCOMBOBOX_INACTIVE, rgb(fgText, bgField));
        colors.put(TCOMBOBOX_ACTIVE, rgb(fgYellow, bgSelect));
        colors.put(TSPINNER_INACTIVE, rgb(fgText, bgField));
        colors.put(TSPINNER_ACTIVE, rgb(fgYellow, bgSelect));

        // Panels / tables / editor / lists / tree: dark surfaces.
        colors.put(TPANEL_BORDER, rgb(fgWhite, bgWindow));
        colors.put(TEDITOR, rgb(fgText, bgWindow));
        colors.put(TEDITOR_SELECTED, rgb(fgWhite, bgSelect));
        colors.put(TEDITOR_MARGIN, rgb(fgMuted, bgField));
        colors.put(TLIST, rgb(fgText, bgWindow));
        colors.put(TLIST_SELECTED, rgb(fgYellow, bgSelect));
        colors.put(TLIST_UNREADABLE, rgb(fgRed, bgWindow));
        colors.put(TLIST_INACTIVE, rgb(fgMuted, bgWindow));
        colors.put(TLIST_SELECTED_INACTIVE, rgb(fgText, bgField));
        colors.put(TTREEVIEW, rgb(fgText, bgWindow));
        colors.put(TTREEVIEW_EXPANDBUTTON, rgb(fgGreen, bgWindow));
        colors.put(TTREEVIEW_SELECTED, rgb(fgYellow, bgSelect));
        colors.put(TTREEVIEW_UNREADABLE, rgb(fgRed, bgWindow));
        colors.put(TTREEVIEW_INACTIVE, rgb(fgMuted, bgWindow));
        colors.put(TTREEVIEW_SELECTED_INACTIVE, rgb(fgText, bgField));
        colors.put(TTABLE_INACTIVE, rgb(fgText, bgWindow));
        colors.put(TTABLE_ACTIVE, rgb(fgYellow, bgSelect));
        colors.put(TTABLE_SELECTED, rgb(fgWhite, bgSelect));
        colors.put(TTABLE_LABEL, rgb(fgCyan, bgWindow));
        colors.put(TTABLE_LABEL_SELECTED, rgb(fgYellow, bgWindow));
        colors.put(TTABLE_BORDER, rgb(fgWhite, bgWindow));
        colors.put(TSPLITPANE, rgb(fgText, bgWindow));

        // Calendar
        colors.put(TCALENDAR_BACKGROUND, rgb(fgText, bgWindow));
        colors.put(TCALENDAR_DAY, rgb(fgText, bgWindow));
        colors.put(TCALENDAR_DAY_SELECTED, rgb(fgYellow, bgSelect));
        colors.put(TCALENDAR_ARROW, rgb(fgGreen, bgWindow));
        colors.put(TCALENDAR_TITLE, rgb(fgYellow, bgWindow));

        // Scrollers
        colors.put(TSCROLLER_BAR, rgb(fgMuted, bgWindow));
        colors.put(TSCROLLER_ARROWS, rgb(fgWhite, bgField));

        // Progress bar
        colors.put(TPROGRESSBAR_COMPLETE, rgb(fgGreen, fgGreen));
        colors.put(TPROGRESSBAR_INCOMPLETE, rgb(fgMuted, bgWindow));

        // Menu: dark panel surface with bright hotkeys.
        colors.put(TMENU, rgb(fgText, bgPanel));
        colors.put(TMENU_HIGHLIGHTED, rgb(fgYellow, bgFieldHot));
        colors.put(TMENU_MNEMONIC, rgb(fgYellow, bgPanel));
        colors.put(TMENU_MNEMONIC_HIGHLIGHTED, rgb(fgRed, bgFieldHot));
        colors.put(TMENU_DISABLED, rgb(borderDim, bgPanel));

        // Status bar
        colors.put(TSTATUSBAR_TEXT, rgb(fgText, bgPanel));
        colors.put(TSTATUSBAR_BUTTON, rgb(fgYellow, bgPanel));
        colors.put(TSTATUSBAR_SELECTED, rgb(fgWhite, bgFieldHot));

        // Help window
        colors.put(THELPWINDOW_BACKGROUND, rgb(fgWhite, bgWindow));
        colors.put(THELPWINDOW_BORDER, rgb(fgGreen, bgWindow));
        colors.put(THELPWINDOW_TEXT, rgb(fgText, bgWindow));
        colors.put(THELPWINDOW_LINK, rgb(fgYellow, bgWindow));
        colors.put(THELPWINDOW_LINK_ACTIVE, rgb(fgYellow, bgSelect));
        colors.put(THELPWINDOW_WINDOWMOVE, rgb(fgGreen, bgWindow));
    }

    /**
     * A theme that mimics the default skin of Midnight Commander: cyan and
     * white on blue panels with yellow selections, and light-grey modal
     * dialogs.
     */
    public void setMidnightCommander() {
        setDefaultTheme();

        // Desktop: Midnight Commander shows the shell behind it; use a dark
        // blue desktop similar to its typical root colour.
        colors.put(TDESKTOP_BACKGROUND, attr(Color.CYAN, Color.BLUE, false));

        // Panels / windows: white on blue, cyan borders.
        colors.put(TWINDOW_BORDER, attr(Color.CYAN, Color.BLUE, true));
        colors.put(TWINDOW_BACKGROUND, attr(Color.WHITE, Color.BLUE, true));
        colors.put(TWINDOW_BORDER_INACTIVE, attr(Color.CYAN, Color.BLUE, false));
        colors.put(TWINDOW_BACKGROUND_INACTIVE, attr(Color.WHITE, Color.BLUE, false));
        colors.put(TWINDOW_BORDER_WINDOWMOVE, attr(Color.YELLOW, Color.BLUE, true));
        colors.put(TWINDOW_BACKGROUND_WINDOWMOVE, attr(Color.WHITE, Color.BLUE, false));

        // Modal dialogs: classic MC "dnormal": black on lightgray.
        colors.put(TWINDOW_BORDER_MODAL, attr(Color.BLACK, Color.WHITE, false));
        colors.put(TWINDOW_BACKGROUND_MODAL, attr(Color.BLACK, Color.WHITE, false));
        colors.put(TWINDOW_BORDER_MODAL_INACTIVE, attr(Color.BLACK, Color.WHITE, false));
        colors.put(TWINDOW_BACKGROUND_MODAL_INACTIVE, attr(Color.BLACK, Color.WHITE, false));
        colors.put(TWINDOW_BORDER_MODAL_WINDOWMOVE, attr(Color.BLUE, Color.WHITE, true));

        // Labels / text.
        colors.put(TLABEL, attr(Color.WHITE, Color.BLUE, true));
        colors.put(TLABEL_MNEMONIC, attr(Color.YELLOW, Color.BLUE, true));
        colors.put(TTEXT, attr(Color.WHITE, Color.BLUE, false));

        // Buttons: MC's "dfocus / dhotfocus" style - black/blue on cyan.
        colors.put(TBUTTON_INACTIVE, attr(Color.BLACK, Color.CYAN, false));
        colors.put(TBUTTON_ACTIVE, attr(Color.WHITE, Color.CYAN, true));
        colors.put(TBUTTON_DISABLED, attr(Color.BLACK, Color.WHITE, true));
        colors.put(TBUTTON_MNEMONIC, attr(Color.BLUE, Color.CYAN, true));
        colors.put(TBUTTON_MNEMONIC_HIGHLIGHTED, attr(Color.YELLOW, Color.CYAN, true));

        // Fields / inputs: MC input is lightgray on blue.
        colors.put(TFIELD_INACTIVE, attr(Color.WHITE, Color.BLUE, false));
        colors.put(TFIELD_ACTIVE, attr(Color.BLACK, Color.CYAN, false));

        // Check / radio / combo.
        colors.put(TCHECKBOX_INACTIVE, attr(Color.WHITE, Color.BLUE, false));
        colors.put(TCHECKBOX_ACTIVE, attr(Color.YELLOW, Color.BLUE, true));
        colors.put(TCHECKBOX_MNEMONIC, attr(Color.YELLOW, Color.BLUE, true));
        colors.put(TCHECKBOX_MNEMONIC_HIGHLIGHTED, attr(Color.YELLOW, Color.CYAN, true));
        colors.put(TRADIOBUTTON_INACTIVE, attr(Color.WHITE, Color.BLUE, false));
        colors.put(TRADIOBUTTON_ACTIVE, attr(Color.YELLOW, Color.BLUE, true));
        colors.put(TRADIOBUTTON_MNEMONIC, attr(Color.YELLOW, Color.BLUE, true));
        colors.put(TRADIOBUTTON_MNEMONIC_HIGHLIGHTED, attr(Color.YELLOW, Color.CYAN, true));
        colors.put(TRADIOGROUP_INACTIVE, attr(Color.WHITE, Color.BLUE, false));
        colors.put(TRADIOGROUP_ACTIVE, attr(Color.YELLOW, Color.BLUE, true));
        colors.put(TCOMBOBOX_INACTIVE, attr(Color.WHITE, Color.BLUE, false));
        colors.put(TCOMBOBOX_ACTIVE, attr(Color.BLACK, Color.CYAN, false));

        // Lists / tree / editor / table: selected = MC "marked" = yellow on
        // blue; focus selected = black on cyan (MC "selected").
        colors.put(TLIST, attr(Color.WHITE, Color.BLUE, false));
        colors.put(TLIST_SELECTED, attr(Color.BLACK, Color.CYAN, false));
        colors.put(TLIST_INACTIVE, attr(Color.WHITE, Color.BLUE, false));
        colors.put(TLIST_SELECTED_INACTIVE, attr(Color.YELLOW, Color.BLUE, true));
        colors.put(TTREEVIEW, attr(Color.WHITE, Color.BLUE, false));
        colors.put(TTREEVIEW_SELECTED, attr(Color.BLACK, Color.CYAN, false));
        colors.put(TTREEVIEW_EXPANDBUTTON, attr(Color.YELLOW, Color.BLUE, true));
        colors.put(TEDITOR, attr(Color.WHITE, Color.BLUE, false));
        colors.put(TEDITOR_SELECTED, attr(Color.BLACK, Color.CYAN, false));
        colors.put(TEDITOR_MARGIN, attr(Color.CYAN, Color.BLUE, true));
        colors.put(TTABLE_INACTIVE, attr(Color.WHITE, Color.BLUE, false));
        colors.put(TTABLE_ACTIVE, attr(Color.BLACK, Color.CYAN, false));
        colors.put(TTABLE_SELECTED, attr(Color.YELLOW, Color.BLUE, true));

        // Panel border
        colors.put(TPANEL_BORDER, attr(Color.CYAN, Color.BLUE, true));

        // Menu: MC menu is lightgray on cyan with yellow hotkeys.
        colors.put(TMENU, attr(Color.BLACK, Color.CYAN, false));
        colors.put(TMENU_HIGHLIGHTED, attr(Color.WHITE, Color.BLACK, true));
        colors.put(TMENU_MNEMONIC, attr(Color.YELLOW, Color.CYAN, true));
        colors.put(TMENU_MNEMONIC_HIGHLIGHTED, attr(Color.YELLOW, Color.BLACK, true));
        colors.put(TMENU_DISABLED, attr(Color.BLUE, Color.CYAN, false));

        // Status bar: black on cyan like MC's "header".
        colors.put(TSTATUSBAR_TEXT, attr(Color.BLACK, Color.CYAN, false));
        colors.put(TSTATUSBAR_BUTTON, attr(Color.YELLOW, Color.CYAN, true));
        colors.put(TSTATUSBAR_SELECTED, attr(Color.WHITE, Color.BLACK, true));

        // Help
        colors.put(THELPWINDOW_BACKGROUND, attr(Color.WHITE, Color.BLUE, true));
        colors.put(THELPWINDOW_BORDER, attr(Color.CYAN, Color.BLUE, true));
        colors.put(THELPWINDOW_TEXT, attr(Color.WHITE, Color.BLUE, false));
        colors.put(THELPWINDOW_LINK, attr(Color.YELLOW, Color.BLUE, true));
        colors.put(THELPWINDOW_LINK_ACTIVE, attr(Color.BLACK, Color.CYAN, false));
    }

    /**
     * A flat, modern terminal-style theme inspired by TUIs like k9s, the
     * GitHub Copilot CLI and Claude Code.  Everything sits on a pure black
     * background with a minimal, muted grey border and vibrant accent
     * colours (cyan, magenta, green, yellow).  The theme is designed to
     * look right even when shadows/translucency are effectively invisible
     * because the desktop and window surfaces are the same black.
     */
    public void setFlatDark() {
        setDefaultTheme();

        // Flat dark palette: everything on pure black with vibrant accents.
        final int bgBlack     = 0x000000; // main surface
        final int bgSubtle    = 0x0a0a0a; // extremely subtle lift for inputs
        final int bgSelection = 0x1f3a5f; // selection blue (muted, readable)
        final int bgAccentDim = 0x1a1a1a; // pressed / disabled surface
        final int fgText      = 0xd0d0d0; // main foreground
        final int fgMuted     = 0x808080; // secondary text / borders
        final int fgBorder    = 0x505050; // minimal window border
        final int fgWhite     = 0xffffff;
        final int accentCyan  = 0x00d7d7; // bright cyan (k9s header-style)
        final int accentGreen = 0x5fff87; // vibrant green
        final int accentMag   = 0xff5fd7; // vibrant magenta / pink
        final int accentYellw = 0xffd75f; // vibrant amber / yellow
        final int accentBlue  = 0x5fafff; // vibrant blue
        final int accentRed   = 0xff5f5f; // vibrant red

        // Desktop: pure black, like a bare terminal.
        colors.put(TDESKTOP_BACKGROUND, rgb(fgMuted, bgBlack));

        // Window borders: thin muted grey on black (active = cyan accent).
        colors.put(TWINDOW_BORDER, rgb(accentCyan, bgBlack));
        colors.put(TWINDOW_BACKGROUND, rgb(fgText, bgBlack));
        colors.put(TWINDOW_BORDER_INACTIVE, rgb(fgBorder, bgBlack));
        colors.put(TWINDOW_BACKGROUND_INACTIVE, rgb(fgMuted, bgBlack));
        colors.put(TWINDOW_BORDER_WINDOWMOVE, rgb(accentGreen, bgBlack));
        colors.put(TWINDOW_BACKGROUND_WINDOWMOVE, rgb(fgText, bgBlack));

        // Modal windows: still black, border accent switches to magenta so
        // modals stand out without introducing a different background.
        colors.put(TWINDOW_BORDER_MODAL, rgb(accentMag, bgBlack));
        colors.put(TWINDOW_BACKGROUND_MODAL, rgb(fgText, bgBlack));
        colors.put(TWINDOW_BORDER_MODAL_INACTIVE, rgb(fgBorder, bgBlack));
        colors.put(TWINDOW_BACKGROUND_MODAL_INACTIVE, rgb(fgMuted, bgBlack));
        colors.put(TWINDOW_BORDER_MODAL_WINDOWMOVE, rgb(accentGreen, bgBlack));

        // Labels / text.
        colors.put(TLABEL, rgb(fgText, bgBlack));
        colors.put(TLABEL_MNEMONIC, rgb(accentYellw, bgBlack));
        colors.put(TTEXT, rgb(fgText, bgBlack));

        // Buttons: flat look - accent text on black, selection-blue when
        // focused, minimal contrast (no fake bevels).
        colors.put(TBUTTON_INACTIVE, rgb(accentCyan, bgBlack));
        colors.put(TBUTTON_ACTIVE, rgb(fgWhite, bgSelection));
        colors.put(TBUTTON_DISABLED, rgb(fgBorder, bgAccentDim));
        colors.put(TBUTTON_MNEMONIC, rgb(accentYellw, bgBlack));
        colors.put(TBUTTON_MNEMONIC_HIGHLIGHTED, rgb(accentYellw, bgSelection));

        // Inputs: barely-lifted surface so the cursor has something to sit on.
        colors.put(TFIELD_INACTIVE, rgb(fgText, bgSubtle));
        colors.put(TFIELD_ACTIVE, rgb(fgWhite, bgSelection));

        // Check boxes / radio buttons / combos.
        colors.put(TCHECKBOX_INACTIVE, rgb(fgText, bgBlack));
        colors.put(TCHECKBOX_ACTIVE, rgb(accentGreen, bgSelection));
        colors.put(TCHECKBOX_MNEMONIC, rgb(accentYellw, bgBlack));
        colors.put(TCHECKBOX_MNEMONIC_HIGHLIGHTED, rgb(accentYellw, bgSelection));
        colors.put(TRADIOBUTTON_INACTIVE, rgb(fgText, bgBlack));
        colors.put(TRADIOBUTTON_ACTIVE, rgb(accentGreen, bgSelection));
        colors.put(TRADIOBUTTON_MNEMONIC, rgb(accentYellw, bgBlack));
        colors.put(TRADIOBUTTON_MNEMONIC_HIGHLIGHTED, rgb(accentYellw, bgSelection));
        colors.put(TRADIOGROUP_INACTIVE, rgb(fgText, bgBlack));
        colors.put(TRADIOGROUP_ACTIVE, rgb(accentCyan, bgBlack));
        colors.put(TCOMBOBOX_INACTIVE, rgb(fgText, bgSubtle));
        colors.put(TCOMBOBOX_ACTIVE, rgb(fgWhite, bgSelection));
        colors.put(TSPINNER_INACTIVE, rgb(fgText, bgSubtle));
        colors.put(TSPINNER_ACTIVE, rgb(fgWhite, bgSelection));

        // Lists / tree / editor / table - k9s-style selection bar.
        colors.put(TLIST, rgb(fgText, bgBlack));
        colors.put(TLIST_SELECTED, rgb(fgWhite, bgSelection));
        colors.put(TLIST_INACTIVE, rgb(fgMuted, bgBlack));
        colors.put(TLIST_SELECTED_INACTIVE, rgb(fgText, bgAccentDim));
        colors.put(TLIST_UNREADABLE, rgb(accentRed, bgBlack));
        colors.put(TTREEVIEW, rgb(fgText, bgBlack));
        colors.put(TTREEVIEW_SELECTED, rgb(fgWhite, bgSelection));
        colors.put(TTREEVIEW_EXPANDBUTTON, rgb(accentCyan, bgBlack));
        colors.put(TTREEVIEW_UNREADABLE, rgb(accentRed, bgBlack));
        colors.put(TTREEVIEW_INACTIVE, rgb(fgMuted, bgBlack));
        colors.put(TTREEVIEW_SELECTED_INACTIVE, rgb(fgText, bgAccentDim));
        colors.put(TEDITOR, rgb(fgText, bgBlack));
        colors.put(TEDITOR_SELECTED, rgb(fgWhite, bgSelection));
        colors.put(TEDITOR_MARGIN, rgb(fgMuted, bgBlack));
        colors.put(TTABLE_INACTIVE, rgb(fgText, bgBlack));
        colors.put(TTABLE_ACTIVE, rgb(fgWhite, bgSelection));
        colors.put(TTABLE_SELECTED, rgb(fgWhite, bgSelection));
        colors.put(TTABLE_LABEL, rgb(accentCyan, bgBlack));
        colors.put(TTABLE_LABEL_SELECTED, rgb(accentYellw, bgBlack));
        colors.put(TTABLE_BORDER, rgb(fgBorder, bgBlack));
        colors.put(TSPLITPANE, rgb(fgText, bgBlack));

        // Calendar
        colors.put(TCALENDAR_BACKGROUND, rgb(fgText, bgBlack));
        colors.put(TCALENDAR_DAY, rgb(fgText, bgBlack));
        colors.put(TCALENDAR_DAY_SELECTED, rgb(fgWhite, bgSelection));
        colors.put(TCALENDAR_ARROW, rgb(accentGreen, bgBlack));
        colors.put(TCALENDAR_TITLE, rgb(accentCyan, bgBlack));

        // Panel border: minimal grey line.
        colors.put(TPANEL_BORDER, rgb(fgBorder, bgBlack));

        // Scrollers
        colors.put(TSCROLLER_BAR, rgb(fgBorder, bgBlack));
        colors.put(TSCROLLER_ARROWS, rgb(accentCyan, bgBlack));

        // Progress bar: bright green on black.
        colors.put(TPROGRESSBAR_COMPLETE, rgb(accentGreen, accentGreen));
        colors.put(TPROGRESSBAR_INCOMPLETE, rgb(fgBorder, bgBlack));

        // Menu: flat black surface, accent-coloured highlights.
        colors.put(TMENU, rgb(fgText, bgBlack));
        colors.put(TMENU_HIGHLIGHTED, rgb(fgWhite, bgSelection));
        colors.put(TMENU_MNEMONIC, rgb(accentYellw, bgBlack));
        colors.put(TMENU_MNEMONIC_HIGHLIGHTED, rgb(accentYellw, bgSelection));
        colors.put(TMENU_DISABLED, rgb(fgBorder, bgBlack));

        // Status bar: k9s-style cyan accent.
        colors.put(TSTATUSBAR_TEXT, rgb(fgMuted, bgBlack));
        colors.put(TSTATUSBAR_BUTTON, rgb(accentCyan, bgBlack));
        colors.put(TSTATUSBAR_SELECTED, rgb(fgWhite, bgSelection));

        // Help window
        colors.put(THELPWINDOW_BACKGROUND, rgb(fgText, bgBlack));
        colors.put(THELPWINDOW_BORDER, rgb(accentCyan, bgBlack));
        colors.put(THELPWINDOW_TEXT, rgb(fgText, bgBlack));
        colors.put(THELPWINDOW_LINK, rgb(accentBlue, bgBlack));
        colors.put(THELPWINDOW_LINK_ACTIVE, rgb(fgWhite, bgSelection));
        colors.put(THELPWINDOW_WINDOWMOVE, rgb(accentGreen, bgBlack));
    }

    /**
     * A modern theme that mimics the default "Dark Modern" colour palette of
     * Visual Studio Code.  Uses 24-bit RGB colours.
     */
    public void setVSCodeDark() {
        setDefaultTheme();

        // VS Code Dark Modern palette.
        final int bgEditor   = 0x1e1e1e; // editor.background
        final int bgPanel    = 0x252526; // sideBar.background
        final int bgChrome   = 0x333333; // activityBar.background / titleBar
        final int bgInput    = 0x3c3c3c; // input.background / border
        final int bgSelect   = 0x264f78; // editor.selectionBackground
        final int bgListSel  = 0x094771; // list.activeSelectionBackground
        final int fgText     = 0xd4d4d4; // editor.foreground
        final int fgChrome   = 0xcccccc; // sideBar.foreground
        final int fgMuted    = 0x858585; // descriptionForeground
        final int accent     = 0x007acc; // statusBar / focusBorder
        final int accentHot  = 0x1177bb; // button.hoverBackground
        final int buttonBg   = 0x0e639c; // button.background
        final int borderDim  = 0x3c3c3c; // editor border

        // Desktop
        colors.put(TDESKTOP_BACKGROUND, rgb(fgMuted, bgEditor));

        // Windows (non-modal): editor surface with a dim border.
        colors.put(TWINDOW_BORDER, rgb(accent, bgPanel));
        colors.put(TWINDOW_BACKGROUND, rgb(fgText, bgPanel));
        colors.put(TWINDOW_BORDER_INACTIVE, rgb(borderDim, bgPanel));
        colors.put(TWINDOW_BACKGROUND_INACTIVE, rgb(fgChrome, bgPanel));
        colors.put(TWINDOW_BORDER_WINDOWMOVE, rgb(accentHot, bgPanel));
        colors.put(TWINDOW_BACKGROUND_WINDOWMOVE, rgb(fgText, bgPanel));

        // Modal dialogs: chrome surface with accent border.
        colors.put(TWINDOW_BORDER_MODAL, rgb(accent, bgChrome));
        colors.put(TWINDOW_BACKGROUND_MODAL, rgb(fgChrome, bgChrome));
        colors.put(TWINDOW_BORDER_MODAL_INACTIVE, rgb(borderDim, bgChrome));
        colors.put(TWINDOW_BACKGROUND_MODAL_INACTIVE, rgb(fgChrome, bgChrome));
        colors.put(TWINDOW_BORDER_MODAL_WINDOWMOVE, rgb(accentHot, bgChrome));

        // Labels / text.
        colors.put(TLABEL, rgb(fgText, bgPanel));
        colors.put(TLABEL_MNEMONIC, rgb(accent, bgPanel));
        colors.put(TTEXT, rgb(fgText, bgPanel));

        // Buttons use the VS Code primary button palette.  Mnemonics use a
        // bright amber so the underlined character is distinguishable from
        // the regular white button label.
        colors.put(TBUTTON_INACTIVE, rgb(0xffffff, buttonBg));
        colors.put(TBUTTON_ACTIVE, rgb(0xffffff, accentHot));
        colors.put(TBUTTON_DISABLED, rgb(fgMuted, bgInput));
        colors.put(TBUTTON_MNEMONIC, rgb(0xffcc00, buttonBg));
        colors.put(TBUTTON_MNEMONIC_HIGHLIGHTED, rgb(0xffcc00, accentHot));

        // Inputs.
        colors.put(TFIELD_INACTIVE, rgb(fgChrome, bgInput));
        colors.put(TFIELD_ACTIVE, rgb(0xffffff, bgSelect));

        // Check / radio / combo.
        colors.put(TCHECKBOX_INACTIVE, rgb(fgText, bgPanel));
        colors.put(TCHECKBOX_ACTIVE, rgb(0xffffff, bgListSel));
        colors.put(TCHECKBOX_MNEMONIC, rgb(accent, bgPanel));
        colors.put(TCHECKBOX_MNEMONIC_HIGHLIGHTED, rgb(0xffffff, bgListSel));
        colors.put(TRADIOBUTTON_INACTIVE, rgb(fgText, bgPanel));
        colors.put(TRADIOBUTTON_ACTIVE, rgb(0xffffff, bgListSel));
        colors.put(TRADIOBUTTON_MNEMONIC, rgb(accent, bgPanel));
        colors.put(TRADIOBUTTON_MNEMONIC_HIGHLIGHTED, rgb(0xffffff, bgListSel));
        colors.put(TRADIOGROUP_INACTIVE, rgb(fgText, bgPanel));
        colors.put(TRADIOGROUP_ACTIVE, rgb(accent, bgPanel));
        colors.put(TCOMBOBOX_INACTIVE, rgb(fgChrome, bgInput));
        colors.put(TCOMBOBOX_ACTIVE, rgb(0xffffff, bgListSel));
        colors.put(TSPINNER_INACTIVE, rgb(fgChrome, bgInput));
        colors.put(TSPINNER_ACTIVE, rgb(0xffffff, bgListSel));

        // Lists / tree / editor / table.
        colors.put(TLIST, rgb(fgText, bgPanel));
        colors.put(TLIST_SELECTED, rgb(0xffffff, bgListSel));
        colors.put(TLIST_INACTIVE, rgb(fgChrome, bgPanel));
        colors.put(TLIST_SELECTED_INACTIVE, rgb(fgChrome, bgInput));
        colors.put(TLIST_UNREADABLE, rgb(0xf14c4c, bgPanel));
        colors.put(TTREEVIEW, rgb(fgText, bgPanel));
        colors.put(TTREEVIEW_SELECTED, rgb(0xffffff, bgListSel));
        colors.put(TTREEVIEW_EXPANDBUTTON, rgb(accent, bgPanel));
        colors.put(TTREEVIEW_UNREADABLE, rgb(0xf14c4c, bgPanel));
        colors.put(TTREEVIEW_INACTIVE, rgb(fgChrome, bgPanel));
        colors.put(TTREEVIEW_SELECTED_INACTIVE, rgb(fgChrome, bgInput));
        colors.put(TEDITOR, rgb(fgText, bgEditor));
        colors.put(TEDITOR_SELECTED, rgb(0xffffff, bgSelect));
        colors.put(TEDITOR_MARGIN, rgb(fgMuted, bgEditor));
        colors.put(TTABLE_INACTIVE, rgb(fgText, bgPanel));
        colors.put(TTABLE_ACTIVE, rgb(0xffffff, bgListSel));
        colors.put(TTABLE_SELECTED, rgb(0xffffff, bgSelect));
        colors.put(TTABLE_LABEL, rgb(fgChrome, bgChrome));
        colors.put(TTABLE_LABEL_SELECTED, rgb(0xffffff, accent));
        colors.put(TTABLE_BORDER, rgb(borderDim, bgPanel));

        // Calendar
        colors.put(TCALENDAR_BACKGROUND, rgb(fgText, bgPanel));
        colors.put(TCALENDAR_DAY, rgb(fgText, bgPanel));
        colors.put(TCALENDAR_DAY_SELECTED, rgb(0xffffff, bgListSel));
        colors.put(TCALENDAR_ARROW, rgb(accent, bgPanel));
        colors.put(TCALENDAR_TITLE, rgb(0xffffff, bgChrome));

        // Scrollers
        colors.put(TSCROLLER_BAR, rgb(fgMuted, bgInput));
        colors.put(TSCROLLER_ARROWS, rgb(0xffffff, bgInput));

        // Panel border
        colors.put(TPANEL_BORDER, rgb(borderDim, bgPanel));

        // Split pane
        colors.put(TSPLITPANE, rgb(fgText, bgPanel));

        // Progress bar
        colors.put(TPROGRESSBAR_COMPLETE, rgb(accent, accent));
        colors.put(TPROGRESSBAR_INCOMPLETE, rgb(fgMuted, bgInput));

        // Menu (command palette-like).
        colors.put(TMENU, rgb(fgChrome, bgPanel));
        colors.put(TMENU_HIGHLIGHTED, rgb(0xffffff, bgListSel));
        colors.put(TMENU_MNEMONIC, rgb(accent, bgPanel));
        colors.put(TMENU_MNEMONIC_HIGHLIGHTED, rgb(0xffffff, bgListSel));
        colors.put(TMENU_DISABLED, rgb(fgMuted, bgPanel));

        // Status bar (VS Code's signature accent blue).  Shortcut-key labels
        // use amber so they stand out against the white status text.
        colors.put(TSTATUSBAR_TEXT, rgb(0xffffff, accent));
        colors.put(TSTATUSBAR_BUTTON, rgb(0xffcc00, accent));
        colors.put(TSTATUSBAR_SELECTED, rgb(0xffcc00, accentHot));

        // Help window
        colors.put(THELPWINDOW_BACKGROUND, rgb(fgText, bgPanel));
        colors.put(THELPWINDOW_BORDER, rgb(accent, bgPanel));
        colors.put(THELPWINDOW_TEXT, rgb(fgText, bgPanel));
        colors.put(THELPWINDOW_LINK, rgb(accent, bgPanel));
        colors.put(THELPWINDOW_LINK_ACTIVE, rgb(0xffffff, bgListSel));
        colors.put(THELPWINDOW_WINDOWMOVE, rgb(accentHot, bgPanel));
    }

    /**
     * A modern theme that mimics the "Light Modern" (2026) colour palette of
     * Visual Studio Code.  Uses 24-bit RGB colours.
     */
    public void setVSCodeLight() {
        setDefaultTheme();

        // VS Code Light Modern palette.
        final int bgEditor   = 0xffffff; // editor.background
        final int bgPanel    = 0xf8f8f8; // sideBar.background
        final int bgChrome   = 0xf0f0f0; // activityBar.background
        final int bgInput    = 0xffffff; // input.background
        final int bgInputBd  = 0xcecece; // input.border
        final int bgSelect   = 0xadd6ff; // editor.selectionBackground
        final int bgListSel  = 0x0060c0; // list.activeSelectionBackground
        final int bgListHov  = 0xe8e8e8; // list.hoverBackground
        final int fgText     = 0x3b3b3b; // editor.foreground
        final int fgChrome   = 0x1f1f1f; // sideBar.foreground
        final int fgMuted    = 0x717171; // descriptionForeground
        final int accent     = 0x0078d4; // statusBar / focusBorder
        final int accentHot  = 0x026ec1; // button.hoverBackground
        final int buttonBg   = 0x005fb8; // button.background
        final int borderDim  = 0xe5e5e5; // editorGroup.border

        // Desktop
        colors.put(TDESKTOP_BACKGROUND, rgb(fgMuted, bgChrome));

        // Windows (non-modal).
        colors.put(TWINDOW_BORDER, rgb(accent, bgPanel));
        colors.put(TWINDOW_BACKGROUND, rgb(fgText, bgPanel));
        colors.put(TWINDOW_BORDER_INACTIVE, rgb(borderDim, bgPanel));
        colors.put(TWINDOW_BACKGROUND_INACTIVE, rgb(fgChrome, bgPanel));
        colors.put(TWINDOW_BORDER_WINDOWMOVE, rgb(accentHot, bgPanel));
        colors.put(TWINDOW_BACKGROUND_WINDOWMOVE, rgb(fgText, bgPanel));

        // Modal dialogs.
        colors.put(TWINDOW_BORDER_MODAL, rgb(accent, bgEditor));
        colors.put(TWINDOW_BACKGROUND_MODAL, rgb(fgChrome, bgEditor));
        colors.put(TWINDOW_BORDER_MODAL_INACTIVE, rgb(borderDim, bgEditor));
        colors.put(TWINDOW_BACKGROUND_MODAL_INACTIVE, rgb(fgChrome, bgEditor));
        colors.put(TWINDOW_BORDER_MODAL_WINDOWMOVE, rgb(accentHot, bgEditor));

        // Labels / text.
        colors.put(TLABEL, rgb(fgChrome, bgPanel));
        colors.put(TLABEL_MNEMONIC, rgb(accent, bgPanel));
        colors.put(TTEXT, rgb(fgText, bgPanel));

        // Buttons.  Mnemonics use a bright amber so the underlined character
        // is distinguishable from the regular white button label.
        colors.put(TBUTTON_INACTIVE, rgb(0xffffff, buttonBg));
        colors.put(TBUTTON_ACTIVE, rgb(0xffffff, accentHot));
        colors.put(TBUTTON_DISABLED, rgb(fgMuted, borderDim));
        colors.put(TBUTTON_MNEMONIC, rgb(0xffcc00, buttonBg));
        colors.put(TBUTTON_MNEMONIC_HIGHLIGHTED, rgb(0xffcc00, accentHot));

        // Inputs.
        colors.put(TFIELD_INACTIVE, rgb(fgChrome, bgInput));
        colors.put(TFIELD_ACTIVE, rgb(fgChrome, bgSelect));

        // Check / radio / combo.
        colors.put(TCHECKBOX_INACTIVE, rgb(fgChrome, bgPanel));
        colors.put(TCHECKBOX_ACTIVE, rgb(0xffffff, bgListSel));
        colors.put(TCHECKBOX_MNEMONIC, rgb(accent, bgPanel));
        colors.put(TCHECKBOX_MNEMONIC_HIGHLIGHTED, rgb(0xffffff, bgListSel));
        colors.put(TRADIOBUTTON_INACTIVE, rgb(fgChrome, bgPanel));
        colors.put(TRADIOBUTTON_ACTIVE, rgb(0xffffff, bgListSel));
        colors.put(TRADIOBUTTON_MNEMONIC, rgb(accent, bgPanel));
        colors.put(TRADIOBUTTON_MNEMONIC_HIGHLIGHTED, rgb(0xffffff, bgListSel));
        colors.put(TRADIOGROUP_INACTIVE, rgb(fgChrome, bgPanel));
        colors.put(TRADIOGROUP_ACTIVE, rgb(accent, bgPanel));
        colors.put(TCOMBOBOX_INACTIVE, rgb(fgChrome, bgInput));
        colors.put(TCOMBOBOX_ACTIVE, rgb(0xffffff, bgListSel));
        colors.put(TSPINNER_INACTIVE, rgb(fgChrome, bgInput));
        colors.put(TSPINNER_ACTIVE, rgb(0xffffff, bgListSel));

        // Lists / tree / editor / table.
        colors.put(TLIST, rgb(fgText, bgPanel));
        colors.put(TLIST_SELECTED, rgb(0xffffff, bgListSel));
        colors.put(TLIST_INACTIVE, rgb(fgText, bgPanel));
        colors.put(TLIST_SELECTED_INACTIVE, rgb(fgChrome, bgListHov));
        colors.put(TLIST_UNREADABLE, rgb(0xe51400, bgPanel));
        colors.put(TTREEVIEW, rgb(fgText, bgPanel));
        colors.put(TTREEVIEW_SELECTED, rgb(0xffffff, bgListSel));
        colors.put(TTREEVIEW_EXPANDBUTTON, rgb(accent, bgPanel));
        colors.put(TTREEVIEW_UNREADABLE, rgb(0xe51400, bgPanel));
        colors.put(TTREEVIEW_INACTIVE, rgb(fgText, bgPanel));
        colors.put(TTREEVIEW_SELECTED_INACTIVE, rgb(fgChrome, bgListHov));
        colors.put(TEDITOR, rgb(fgText, bgEditor));
        colors.put(TEDITOR_SELECTED, rgb(fgChrome, bgSelect));
        colors.put(TEDITOR_MARGIN, rgb(fgMuted, bgEditor));
        colors.put(TTABLE_INACTIVE, rgb(fgText, bgPanel));
        colors.put(TTABLE_ACTIVE, rgb(0xffffff, bgListSel));
        colors.put(TTABLE_SELECTED, rgb(fgChrome, bgSelect));
        colors.put(TTABLE_LABEL, rgb(fgChrome, bgChrome));
        colors.put(TTABLE_LABEL_SELECTED, rgb(0xffffff, accent));
        colors.put(TTABLE_BORDER, rgb(borderDim, bgPanel));

        // Calendar
        colors.put(TCALENDAR_BACKGROUND, rgb(fgText, bgPanel));
        colors.put(TCALENDAR_DAY, rgb(fgText, bgPanel));
        colors.put(TCALENDAR_DAY_SELECTED, rgb(0xffffff, bgListSel));
        colors.put(TCALENDAR_ARROW, rgb(accent, bgPanel));
        colors.put(TCALENDAR_TITLE, rgb(fgChrome, bgChrome));

        // Scrollers
        colors.put(TSCROLLER_BAR, rgb(fgMuted, bgInputBd));
        colors.put(TSCROLLER_ARROWS, rgb(fgChrome, bgChrome));

        // Panel border
        colors.put(TPANEL_BORDER, rgb(borderDim, bgPanel));

        // Split pane
        colors.put(TSPLITPANE, rgb(fgText, bgPanel));

        // Progress bar
        colors.put(TPROGRESSBAR_COMPLETE, rgb(accent, accent));
        colors.put(TPROGRESSBAR_INCOMPLETE, rgb(fgMuted, borderDim));

        // Menu.
        colors.put(TMENU, rgb(fgChrome, bgPanel));
        colors.put(TMENU_HIGHLIGHTED, rgb(0xffffff, bgListSel));
        colors.put(TMENU_MNEMONIC, rgb(accent, bgPanel));
        colors.put(TMENU_MNEMONIC_HIGHLIGHTED, rgb(0xffffff, bgListSel));
        colors.put(TMENU_DISABLED, rgb(fgMuted, bgPanel));

        // Status bar (VS Code's signature accent blue).  Shortcut-key labels
        // use amber so they stand out against the white status text.
        colors.put(TSTATUSBAR_TEXT, rgb(0xffffff, accent));
        colors.put(TSTATUSBAR_BUTTON, rgb(0xffcc00, accent));
        colors.put(TSTATUSBAR_SELECTED, rgb(0xffcc00, accentHot));

        // Help window
        colors.put(THELPWINDOW_BACKGROUND, rgb(fgText, bgPanel));
        colors.put(THELPWINDOW_BORDER, rgb(accent, bgPanel));
        colors.put(THELPWINDOW_TEXT, rgb(fgText, bgPanel));
        colors.put(THELPWINDOW_LINK, rgb(accent, bgPanel));
        colors.put(THELPWINDOW_LINK_ACTIVE, rgb(0xffffff, bgListSel));
        colors.put(THELPWINDOW_WINDOWMOVE, rgb(accentHot, bgPanel));
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
