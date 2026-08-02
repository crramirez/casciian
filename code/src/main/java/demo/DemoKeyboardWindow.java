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
import casciian.TList;
import casciian.TPanel;
import casciian.TText;
import casciian.TWindow;
import casciian.backend.ECMA48Terminal;
import casciian.backend.KittyKeyboard;
import casciian.event.TKeypressEvent;
import casciian.event.TResizeEvent;
import casciian.layout.BoxLayoutManager;

/**
 * This window echoes every keystroke it receives, showing the decoded
 * TKeypress fields.  It exists to make keyboard handling visible: in
 * particular whether the terminal is speaking the Kitty keyboard protocol
 * (CSI u), which is what lets Ctrl+I arrive as something other than Tab.
 *
 * <p>The support line is read live from
 * {@link ECMA48Terminal#getKittyKeyboardSupport()} rather than inferred from
 * what the user happens to type: an application deciding whether to
 * advertise a Ctrl+I-style shortcut needs the answer before the user has
 * pressed anything, not after.</p>
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

    private static final int INFO_ROWS = 6;
    private static final int MIN_HISTORY_ROWS = 4;

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private final ResourceBundle i18n;

    /**
     * The most recent keystrokes, oldest first.
     */
    private final List<String> history = new ArrayList<>();

    private final TPanel textPanel;
    private final TText statusText;
    private final TText hintText;
    private final TList historyList;

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
        super(parent, "", 0, 0, 68, 18, CENTERED | RESIZABLE);

        i18n = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME, getLocale());
        setTitle(i18n.getString("windowTitle"));

        textPanel = addPanel(1, 1, getWidth() - 2, INFO_ROWS);
        textPanel.setBorderStyle("none");
        textPanel.setLayoutManager(new BoxLayoutManager(textPanel.getWidth(),
            textPanel.getHeight(), true));
        textPanel.addText(i18n.getString("instructions"), 0, 0,
            textPanel.getWidth(), 2);
        statusText = textPanel.addText("", 0, 0, textPanel.getWidth(), 2);
        hintText = textPanel.addText("", 0, 0, textPanel.getWidth(), 2);

        historyList = addList(history, 1, 1 + INFO_ROWS, getWidth() - 2,
            getHeight() - 2 - INFO_ROWS);
        layoutWidgets(null);
        refreshStatusText();
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

        history.add(describe(key));
        historyList.setList(history);
        historyList.setSelectedIndex(history.size() - 1);
        refreshStatusText();
    }

    @Override
    public void onResize(final TResizeEvent event) {
        super.onResize(event);
        layoutWidgets(event);
        refreshStatusText();
    }

    @Override
    public void draw() {
        refreshStatusText();
        super.draw();
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
     * The live Kitty keyboard support determination for this window's
     * screen, or null if this window is not backed by an ECMA-48 terminal
     * (for example under the HeadlessBackend used in tests).
     *
     * @return the support state, or null if not applicable
     */
    private KittyKeyboard.SupportState support() {
        if (getScreen() instanceof ECMA48Terminal terminal) {
            return terminal.getKittyKeyboardSupport();
        }
        return null;
    }

    /**
     * One line summarizing whether Ctrl+I-style shortcuts can be trusted on
     * this terminal right now.  This is the check an application would make
     * before deciding whether to advertise such a shortcut in its own UI.
     *
     * @return the status line to display
     */
    private String statusLine() {
        KittyKeyboard.SupportState support = support();
        if (support == null) {
            return i18n.getString("protocolNotApplicable");
        }
        return switch (support) {
            case SUPPORTED -> i18n.getString("protocolSupported");
            case UNSUPPORTED -> i18n.getString("protocolUnsupported");
            case UNKNOWN -> i18n.getString("protocolDetecting");
        };
    }

    private void refreshStatusText() {
        String status = statusLine();
        if (!status.equals(statusText.getText())) {
            statusText.setText(status);
        }

        String hint = "";
        if (support() == KittyKeyboard.SupportState.UNSUPPORTED) {
            hint = i18n.getString("unsupportedHint");
        }
        if (!hint.equals(hintText.getText())) {
            hintText.setText(hint);
        }
    }

    private void layoutWidgets(final TResizeEvent event) {
        int clientWidth = getWidth() - 2;
        int clientHeight = getHeight() - 2;
        int infoRows = Math.clamp(clientHeight - MIN_HISTORY_ROWS, 2, INFO_ROWS);
        int historyRows = Math.max(1, clientHeight - infoRows);

        textPanel.setDimensions(1, 1, clientWidth, infoRows);
        if (event != null) {
            textPanel.onResize(new TResizeEvent(event.getBackend(),
                TResizeEvent.Type.WIDGET, textPanel.getWidth(),
                textPanel.getHeight()));
        }
        historyList.setDimensions(1, 1 + infoRows, clientWidth, historyRows);
    }

}
