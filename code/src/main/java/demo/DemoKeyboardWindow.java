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
package demo;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import casciian.TApplication;
import casciian.TKeypress;
import casciian.TWindow;
import casciian.bits.CellAttributes;
import casciian.bits.ColorTheme;
import casciian.event.TKeypressEvent;

/**
 * This window echoes every keystroke it receives, showing the decoded
 * TKeypress fields.  It exists to make keyboard handling visible: in
 * particular whether the terminal is speaking the Kitty keyboard protocol
 * (CSI u), which is what lets Ctrl+I arrive as something other than Tab.
 *
 * <p>Unlike other windows, this one deliberately swallows Tab and the arrow
 * keys instead of letting them move focus, so that they can be observed.</p>
 */
public class DemoKeyboardWindow extends TWindow {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The name of the resource bundle for this class.
     */
    public static final String RESOURCE_BUNDLE_NAME = DemoKeyboardWindow.class.getName() + "Bundle";

    /**
     * How many keystrokes to keep on screen.
     */
    private static final int HISTORY_SIZE = 12;

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * The most recent keystrokes, oldest first.
     */
    private final List<String> history = new ArrayList<String>();

    /**
     * Set once we see a keystroke that the legacy encoding could not have
     * produced, which proves the terminal honored the CSI u request.
     */
    private boolean disambiguationSeen = false;

    /**
     * True if the previous keystroke was Escape, used for the
     * Escape-Escape-to-close shortcut.
     */
    private boolean lastWasEscape = false;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Constructor.
     *
     * @param parent the main application
     */
    DemoKeyboardWindow(final TApplication parent) {
        super(parent, "", 0, 0, 68, HISTORY_SIZE + 6, CENTERED | RESIZABLE);

        i18n = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME, getLocale());
        setTitle(i18n.getString("windowTitle"));
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Record every keystroke rather than acting on it.  Tab, the arrow keys,
     * and Enter are intentionally not passed to super, so that they show up
     * in the log instead of moving focus or closing the window.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        TKeypress key = keypress.getKey();

        // Escape twice in a row closes, since this window eats everything
        // else.
        if (key.equals(TKeypress.kbEsc)) {
            if (lastWasEscape) {
                getApplication().closeWindow(this);
                return;
            }
            lastWasEscape = true;
        } else {
            lastWasEscape = false;
        }

        if (isDisambiguated(key)) {
            disambiguationSeen = true;
        }

        history.add(describe(key));
        while (history.size() > HISTORY_SIZE) {
            history.removeFirst();
        }
    }

    /**
     * Draw the keystroke log.
     */
    @Override
    public void draw() {
        super.draw();

        CellAttributes text = getTheme().getColor(ColorTheme.TLABEL);
        CellAttributes heading = getTheme().getColor(ColorTheme.TTEXT);

        int row = 1;
        putStringXY(2, row++, i18n.getString("instructions"), heading);
        putStringXY(2, row++, disambiguationSeen
            ? i18n.getString("protocolDetected")
            : i18n.getString("protocolUnknown"), heading);
        row++;

        for (String line : history) {
            putStringXY(2, row++, line, text);
        }
    }

    // ------------------------------------------------------------------------
    // DemoKeyboardWindow -----------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Render one keystroke, showing every field that key matching depends
     * on, plus whether it collides with Tab or Enter.
     *
     * @param key the keystroke
     * @return a one-line description
     */
    private String describe(final TKeypress key) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-20s", key.toString()));

        if (key.isFnKey()) {
            sb.append(String.format(" fn=%-4d      ", key.getKeyCode()));
        } else {
            int ch = key.getChar();
            String printable = ((ch >= 0x20) && (ch < 0x7F))
                ? "'" + (char) ch + "'" : "";
            sb.append(String.format(" ch=%-5d %-4s ", ch, printable));
        }

        sb.append(key.isCtrl() ? "Ctrl " : "     ");
        sb.append(key.isAlt() ? "Alt " : "    ");
        sb.append(key.isShift() ? "Shift " : "      ");

        if (key.equals(TKeypress.kbTab)) {
            sb.append("= kbTab");
        } else if (key.equals(TKeypress.kbCtrlI)) {
            sb.append("= kbCtrlI");
        } else if (key.equals(TKeypress.kbEnter)) {
            sb.append("= kbEnter");
        } else if (key.equals(TKeypress.kbCtrlM)) {
            sb.append("= kbCtrlM");
        }
        return sb.toString();
    }

    /**
     * Whether this keystroke is one the legacy VT encoding cannot express,
     * and so proves the terminal is speaking the Kitty keyboard protocol.
     *
     * @param key the keystroke
     * @return true if the keystroke could only have come from a CSI u
     * sequence
     */
    private static boolean isDisambiguated(final TKeypress key) {
        if (key.isFnKey()) {
            // Legacy sends a bare 0x0D for Enter and 0x09 for Tab, with no
            // room for modifiers.
            if ((key.getKeyCode() == TKeypress.ENTER)
                && (key.isCtrl() || key.isShift())
            ) {
                return true;
            }
            return (key.getKeyCode() == TKeypress.TAB) && key.isCtrl();
        }

        if (key.isCtrl() && key.isShift()) {
            // Legacy collapses Ctrl+Shift+X onto Ctrl+X.
            return true;
        }

        // Ctrl+I, Ctrl+M and Ctrl+[ collapse onto Tab, Enter and Esc in the
        // legacy encoding, so seeing them at all is proof.
        return key.isCtrl()
            && ((key.getChar() == 'I')
                || (key.getChar() == 'M')
                || (key.getChar() == '['));
    }

}
