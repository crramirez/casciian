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
package casciian.bits;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * ColorTheme is a collection of colors keyed by string.  A default theme is
 * also provided that matches the blue-and-white theme used by Turbo Vision.
 */
public class ColorTheme {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The current theme colors.
     */
    private SortedMap<String, CellAttributes> colors;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor sets the theme to the default.
     */
    @SuppressWarnings("this-escape")
    public ColorTheme() {
        colors = new TreeMap<String, CellAttributes>();
        setDefaultTheme();
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
        CellAttributes attr = colors.get(name);
        return attr;
    }

    /**
     * Retrieve all the names in the theme.
     *
     * @return a list of names
     */
    public List<String> getColorNames() {
        Set<String> keys = colors.keySet();
        List<String> names = new ArrayList<String>(keys.size());
        names.addAll(keys);
        return names;
    }

    /**
     * Set the color for a named theme color.
     *
     * @param name theme color name, e.g. "twindow.border"
     * @param color the new color to associate with name, e.g. bold yellow on
     * blue
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
        FileWriter file = new FileWriter(filename);
        for (String key: colors.keySet()) {
            CellAttributes color = getColor(key);
            file.write(String.format("%s = %s\n", key, color));
        }
        file.close();
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
     * @param key the color key string
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

        if (token.toLowerCase().equals("rgb:")) {
            // Foreground
            int foreColorRGB = -1;
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
            if (!tokenizer.nextToken().toLowerCase().equals("on")) {
                // Invalid line.
                return;
            }

            // Background
            int backColorRGB = -1;
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

        if (!tokenizer.nextToken().toLowerCase().equals("on")) {
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

    /**
     * Read color theme mappings from a Reader.  The reader is closed at the
     * end.
     *
     * @param reader the reader to read from
     * @throws IOException if the I/O fails
     */
    public void load(final Reader reader) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
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
            setColorFromString(key, text);
        }
        // All done.
        bufferedReader.close();
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
        colors.put("twindow.border", color);

        // TWindow background
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put("twindow.background", color);

        // TWindow border - inactive
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put("twindow.border.inactive", color);

        // TWindow background - inactive
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put("twindow.background.inactive", color);

        // TWindow border - modal
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.WHITE);
        color.setBold(true);
        colors.put("twindow.border.modal", color);

        // TWindow background - modal
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("twindow.background.modal", color);

        // TWindow border - modal + inactive
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(true);
        colors.put("twindow.border.modal.inactive", color);

        // TWindow background - modal + inactive
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("twindow.background.modal.inactive", color);

        // TWindow border - during window movement - modal
        color = new CellAttributes();
        color.setForeColor(Color.GREEN);
        color.setBackColor(Color.WHITE);
        color.setBold(true);
        colors.put("twindow.border.modal.windowmove", color);

        // TWindow border - during window movement
        color = new CellAttributes();
        color.setForeColor(Color.GREEN);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put("twindow.border.windowmove", color);

        // TWindow background - during window movement
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("twindow.background.windowmove", color);

        // TDesktop background
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("tdesktop.background", color);

        // TButton text
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.GREEN);
        color.setBold(false);
        colors.put("tbutton.inactive", color);
        color = new CellAttributes();
        color.setForeColor(Color.CYAN);
        color.setBackColor(Color.GREEN);
        color.setBold(true);
        colors.put("tbutton.active", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(true);
        colors.put("tbutton.disabled", color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.GREEN);
        color.setBold(true);
        colors.put("tbutton.mnemonic", color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.GREEN);
        color.setBold(true);
        colors.put("tbutton.mnemonic.highlighted", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBold(true);
        colors.put("tbutton.mnemonic.pulse", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBold(true);
        colors.put("tbutton.pulse", color);

        // TLabel text
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put("tlabel", color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put("tlabel.mnemonic", color);

        // TText text
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("ttext", color);

        // TField text
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("tfield.inactive", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("tfield.active", color);
        color = new CellAttributes();
        // Just a small bit of amber.
        color.setForeColorRGB(0x8A610D);
        colors.put("tfield.pulse", color);

        // TCheckBox
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("tcheckbox.inactive", color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put("tcheckbox.active", color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put("tcheckbox.mnemonic", color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put("tcheckbox.mnemonic.highlighted", color);
        color = new CellAttributes();
        // Just a small bit of amber.
        color.setForeColorRGB(0x8A610D);
        colors.put("tcheckbox.pulse", color);

        // TComboBox
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("tcombobox.inactive", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("tcombobox.active", color);

        // TSpinner
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("tspinner.inactive", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("tspinner.active", color);

        // TCalendar
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("tcalendar.background", color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("tcalendar.day", color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("tcalendar.day.selected", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("tcalendar.arrow", color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put("tcalendar.title", color);

        // TPanel border
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        colors.put("tpanel.border", color);

        // TRadioButton
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("tradiobutton.inactive", color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put("tradiobutton.active", color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put("tradiobutton.mnemonic", color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put("tradiobutton.mnemonic.highlighted", color);
        color = new CellAttributes();
        // Just a small bit of amber.
        color.setForeColorRGB(0x8A610D);
        colors.put("tradiobutton.pulse", color);

        // TRadioGroup
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("tradiogroup.inactive", color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put("tradiogroup.active", color);

        // TMenu
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("tmenu", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.GREEN);
        color.setBold(false);
        colors.put("tmenu.highlighted", color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("tmenu.mnemonic", color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.GREEN);
        color.setBold(false);
        colors.put("tmenu.mnemonic.highlighted", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(true);
        colors.put("tmenu.disabled", color);

        // TProgressBar
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put("tprogressbar.complete", color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("tprogressbar.incomplete", color);

        // THScroller / TVScroller
        color = new CellAttributes();
        color.setForeColor(Color.CYAN);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("tscroller.bar", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("tscroller.arrows", color);

        // TTreeView
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("ttreeview", color);
        color = new CellAttributes();
        color.setForeColor(Color.GREEN);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put("ttreeview.expandbutton", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("ttreeview.selected", color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("ttreeview.unreadable", color);
        color = new CellAttributes();
        // color.setForeColor(Color.BLACK);
        // color.setBackColor(Color.BLUE);
        // color.setBold(true);
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("ttreeview.inactive", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("ttreeview.selected.inactive", color);

        // TList
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("tlist", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("tlist.selected", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("tlist.unreadable", color);
        color = new CellAttributes();
        // color.setForeColor(Color.BLACK);
        // color.setBackColor(Color.BLUE);
        // color.setBold(true);
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("tlist.inactive", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("tlist.selected.inactive", color);

        // TStatusBar
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("tstatusbar.text", color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("tstatusbar.button", color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("tstatusbar.selected", color);

        // TEditor
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("teditor", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("teditor.selected", color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put("teditor.margin", color);

        // TTable
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("ttable.inactive", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("ttable.active", color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put("ttable.selected", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("ttable.label", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("ttable.label.selected", color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("ttable.border", color);

        // TSplitPane
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("tsplitpane", color);

        // THelpWindow border - during window movement
        color = new CellAttributes();
        color.setForeColor(Color.GREEN);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put("thelpwindow.windowmove", color);

        // THelpWindow border
        color = new CellAttributes();
        color.setForeColor(Color.GREEN);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put("thelpwindow.border", color);

        // THelpWindow background
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put("thelpwindow.background", color);

        // THelpWindow text
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("thelpwindow.text", color);

        // THelpWindow link
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put("thelpwindow.link", color);

        // THelpWindow link - active
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put("thelpwindow.link.active", color);
    }

    /**
     * Set the theme to femme.  I love pink.  You can too!  💗
     */
    public void setFemme() {
        setDefaultTheme();
        final int pink = 0xf7a8b8;
        final int blue = 0x55cdfc;
        final int pink2 = 0xd77888;

        for (String key: colors.keySet()) {
            CellAttributes color = colors.get(key);

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
            } else if (bg.equals(Color.WHITE) && fg.equals(Color.WHITE)) {
                color.setForeColorRGB(blue);
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

            colors.put(key, color);
        }

        CellAttributes color;
        color = new CellAttributes();
        color.setForeColor(Color.MAGENTA);
        color.setBackColorRGB(pink2);
        color.setBold(false);
        colors.put("twindow.background", color);
        colors.put("twindow.background.inactive", color);
        colors.put("twindow.background.modal", color);
        colors.put("twindow.background.modal.inactive", color);
        colors.put("twindow.background.windowmove", color);

        color = new CellAttributes();
        color.setForeColor(Color.MAGENTA);
        color.setBold(true);
        colors.put("twindow.border", color);
        colors.put("twindow.border.inactive", color);
        colors.put("twindow.border.modal", color);
        colors.put("twindow.border.modal.inactive", color);

        color = new CellAttributes();
        color.setForeColorRGB(blue);
        colors.put("twindow.border.windowmove", color);
        colors.put("twindow.border.modal.windowmove", color);

        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBold(true);
        color.setBackColorRGB(pink);
        colors.put("tbutton.active", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColorRGB(pink);
        colors.put("tbutton.inactive", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBold(true);
        color = new CellAttributes();
        color.setForeColorRGB(blue);
        color.setBold(true);
        color.setBackColorRGB(pink);
        colors.put("tbutton.mnemonic.highlighted", color);
        color = new CellAttributes();
        color.setForeColorRGB(blue);
        color.setBold(true);
        color.setBackColorRGB(pink);
        colors.put("tbutton.mnemonic", color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        colors.put("tbutton.pulse", color);
        color = new CellAttributes();
        color.setForeColorRGB(blue);
        color.setBold(true);
        colors.put("tbutton.mnemonic.pulse", color);

        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColorRGB(pink2);
        color.setBold(true);
        colors.put("tprogressbar.complete", color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColorRGB(pink2);
        color.setBold(false);
        colors.put("tprogressbar.incomplete", color);

        color = new CellAttributes();
        color.setForeColor(Color.MAGENTA);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put("tpanel.border", color);

        color = new CellAttributes();
        color.setForeColor(Color.MAGENTA);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put("tradiogroup.active", color);
        color = new CellAttributes();
        color.setForeColor(Color.MAGENTA);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put("tradiobutton.active", color);
        color = new CellAttributes();
        color.setForeColor(Color.MAGENTA);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put("tcheckbox.active", color);
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
        colors.put("twindow.border", color);

        // TWindow background
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.CYAN);
        colors.put("twindow.background", color);

        // TWindow border - inactive
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put("twindow.border.inactive", color);

        // TWindow background - inactive
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.CYAN);
        colors.put("twindow.background.inactive", color);

        // TWindow border - modal
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.WHITE);
        color.setBold(true);
        colors.put("twindow.border.modal", color);

        // TWindow background - modal
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("twindow.background.modal", color);

        // TWindow border - modal + inactive
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(true);
        colors.put("twindow.border.modal.inactive", color);

        // TWindow background - modal + inactive
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("twindow.background.modal.inactive", color);

        // TWindow border - during window movement - modal
        color = new CellAttributes();
        color.setForeColor(Color.GREEN);
        color.setBackColor(Color.WHITE);
        color.setBold(true);
        colors.put("twindow.border.modal.windowmove", color);

        // TWindow border - during window movement
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put("twindow.border.windowmove", color);

        // TWindow background - during window movement
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("twindow.background.windowmove", color);

        // TDesktop background
        color = new CellAttributes();
        color.setForeColor(Color.CYAN);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("tdesktop.background", color);

        // TButton text
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.GREEN);
        color.setBold(false);
        colors.put("tbutton.inactive", color);
        color = new CellAttributes();
        color.setForeColor(Color.CYAN);
        color.setBackColor(Color.GREEN);
        color.setBold(true);
        colors.put("tbutton.active", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(true);
        colors.put("tbutton.disabled", color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.GREEN);
        color.setBold(true);
        colors.put("tbutton.mnemonic", color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.GREEN);
        color.setBold(true);
        colors.put("tbutton.mnemonic.highlighted", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBold(true);
        colors.put("tbutton.mnemonic.pulse", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBold(true);
        colors.put("tbutton.pulse", color);

        // TLabel text
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put("tlabel", color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put("tlabel.mnemonic", color);

        // TText text
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("ttext", color);

        // TField text
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("tfield.inactive", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("tfield.active", color);
        color = new CellAttributes();
        // Just a small bit of amber.
        color.setForeColorRGB(0x8A610D);
        colors.put("tfield.pulse", color);

        // TCheckBox
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("tcheckbox.inactive", color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put("tcheckbox.active", color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put("tcheckbox.mnemonic", color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put("tcheckbox.mnemonic.highlighted", color);
        color = new CellAttributes();
        // Just a small bit of amber.
        color.setForeColorRGB(0x8A610D);
        colors.put("tcheckbox.pulse", color);

        // TComboBox
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("tcombobox.inactive", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("tcombobox.active", color);

        // TSpinner
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("tspinner.inactive", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("tspinner.active", color);

        // TCalendar
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("tcalendar.background", color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("tcalendar.day", color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("tcalendar.day.selected", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("tcalendar.arrow", color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put("tcalendar.title", color);

        // TPanel border
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put("tpanel.border", color);

        // TRadioButton
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("tradiobutton.inactive", color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put("tradiobutton.active", color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put("tradiobutton.mnemonic", color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.BLACK);
        color.setBold(true);
        colors.put("tradiobutton.mnemonic.highlighted", color);
        color = new CellAttributes();
        // Just a small bit of amber.
        color.setForeColorRGB(0x8A610D);
        colors.put("tradiobutton.pulse", color);

        // TRadioGroup
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("tradiogroup.inactive", color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put("tradiogroup.active", color);

        // TMenu
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("tmenu", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.GREEN);
        color.setBold(false);
        colors.put("tmenu.highlighted", color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("tmenu.mnemonic", color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.GREEN);
        color.setBold(false);
        colors.put("tmenu.mnemonic.highlighted", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(true);
        colors.put("tmenu.disabled", color);

        // TProgressBar
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put("tprogressbar.complete", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("tprogressbar.incomplete", color);

        // THScroller / TVScroller
        color = new CellAttributes();
        color.setForeColor(Color.CYAN);
        color.setBackColor(Color.BLACK);
        color.setBold(false);
        colors.put("tscroller.bar", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("tscroller.arrows", color);

        // TTreeView
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("ttreeview", color);
        color = new CellAttributes();
        color.setForeColor(Color.GREEN);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put("ttreeview.expandbutton", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("ttreeview.selected", color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("ttreeview.unreadable", color);
        color = new CellAttributes();
        // color.setForeColor(Color.BLACK);
        // color.setBackColor(Color.BLUE);
        // color.setBold(true);
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("ttreeview.inactive", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("ttreeview.selected.inactive", color);

        // TList
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("tlist", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("tlist.selected", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("tlist.unreadable", color);
        color = new CellAttributes();
        // color.setForeColor(Color.BLACK);
        // color.setBackColor(Color.BLUE);
        // color.setBold(true);
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("tlist.inactive", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("tlist.selected.inactive", color);

        // TStatusBar
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("tstatusbar.text", color);
        color = new CellAttributes();
        color.setForeColor(Color.RED);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("tstatusbar.button", color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("tstatusbar.selected", color);

        // TEditor
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("teditor", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("teditor.selected", color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put("teditor.margin", color);

        // TTable
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("ttable.inactive", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.CYAN);
        color.setBold(false);
        colors.put("ttable.active", color);
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put("ttable.selected", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLACK);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("ttable.label", color);
        color = new CellAttributes();
        color.setForeColor(Color.BLUE);
        color.setBackColor(Color.WHITE);
        color.setBold(false);
        colors.put("ttable.label.selected", color);
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("ttable.border", color);

        // TSplitPane
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("tsplitpane", color);

        // THelpWindow border - during window movement
        color = new CellAttributes();
        color.setForeColor(Color.GREEN);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put("thelpwindow.windowmove", color);

        // THelpWindow border
        color = new CellAttributes();
        color.setForeColor(Color.GREEN);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put("thelpwindow.border", color);

        // THelpWindow background
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put("thelpwindow.background", color);

        // THelpWindow text
        color = new CellAttributes();
        color.setForeColor(Color.WHITE);
        color.setBackColor(Color.BLUE);
        color.setBold(false);
        colors.put("thelpwindow.text", color);

        // THelpWindow link
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.BLUE);
        color.setBold(true);
        colors.put("thelpwindow.link", color);

        // THelpWindow link - active
        color = new CellAttributes();
        color.setForeColor(Color.YELLOW);
        color.setBackColor(Color.CYAN);
        color.setBold(true);
        colors.put("thelpwindow.link.active", color);

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
