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
package demo;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.ResourceBundle;

import casciian.TApplication;
import casciian.TButton;
import casciian.TDesktop;
import casciian.TEditColorThemeWindow;
import casciian.TEditorWindow;
import casciian.TWidget;
import casciian.TWindow;
import casciian.backend.ECMA48Terminal;
import casciian.backend.SystemProperties;
import casciian.event.TMenuEvent;
import casciian.menu.TMenu;
import casciian.menu.TMenuItem;
import casciian.menu.TSubMenu;
import casciian.backend.Backend;

/**
 * The demo application itself.
 */
public class DemoApplication extends TApplication {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = ResourceBundle.getBundle(DemoApplication.class.getName() + "Bundle");

    /**
     * The desktop visible before selecting "Expose terminal background image".
     */
    private TDesktop oldDesktop = null;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param input an InputStream connected to the remote user, or null for
     * System.in.  If System.in is used, then on non-Windows systems it will
     * be put in raw mode; shutdown() will (blindly!) put System.in in cooked
     * mode.  input is always converted to a Reader with UTF-8 encoding.
     * @param output an OutputStream connected to the remote user, or null
     * for System.out.  output is always converted to a Writer with UTF-8
     * encoding.
     * @throws UnsupportedEncodingException if an exception is thrown when
     * creating the InputStreamReader
     */
    @SuppressWarnings("this-escape")
    public DemoApplication(final InputStream input,
        final OutputStream output) throws UnsupportedEncodingException {
        super(input, output);

        addAllWidgets();

        getBackend().setTitle(i18n.getString("applicationTitle"));
    }

    /**
     * Public constructor.
     *
     * @param input the InputStream underlying 'reader'.  Its available()
     * method is used to determine if reader.read() will block or not.
     * @param reader a Reader connected to the remote user.
     * @param writer a PrintWriter connected to the remote user.
     * @param setRawMode if true, set System.in into raw mode with stty.
     * This should in general not be used.  It is here solely for Demo3,
     * which uses System.in.
     * @throws IllegalArgumentException if input, reader, or writer are null.
     */
    @SuppressWarnings("this-escape")
    public DemoApplication(final InputStream input, final Reader reader,
        final PrintWriter writer, final boolean setRawMode) {
        super(input, reader, writer, setRawMode);

        addAllWidgets();

        getBackend().setTitle(i18n.getString("applicationTitle"));
    }

    /**
     * Public constructor.
     *
     * @param input the InputStream underlying 'reader'.  Its available()
     * method is used to determine if reader.read() will block or not.
     * @param reader a Reader connected to the remote user.
     * @param writer a PrintWriter connected to the remote user.
     * @throws IllegalArgumentException if input, reader, or writer are null.
     */
    public DemoApplication(final InputStream input, final Reader reader,
        final PrintWriter writer) {

        this(input, reader, writer, false);
    }

    /**
     * Public constructor.
     *
     * @param backend a Backend that is already ready to go.
     */
    @SuppressWarnings("this-escape")
    public DemoApplication(final Backend backend) {
        super(backend);

        addAllWidgets();
    }

    /**
     * Public constructor.
     *
     * @param backendType one of the TApplication.BackendType values
     * @param defaults if true, apply Casciian default settings; if false, apply custom theme and visual enhancements
     * @throws UnsupportedEncodingException if TApplication can't instantiate the Backend.
     */
    @SuppressWarnings("this-escape")
    public DemoApplication(final BackendType backendType, final boolean defaults) throws UnsupportedEncodingException {
        super(backendType);

        addAllWidgets();
        getBackend().setTitle(i18n.getString("applicationTitle"));

        if (!defaults) {
            // Use the custom theme by default.
            onMenu(new TMenuEvent(getBackend(), 10003));

            // Use window gradients by default.
            setMenuItemChecked(10010, true);
            onMenu(new TMenuEvent(getBackend(), 10010));

            // Expose terminal background image by default.
            setMenuItemChecked(10011, true);
            onMenu(new TMenuEvent(getBackend(), 10011));
        }

    }

    /**
     * Public constructor.
     *
     * @param backendType one of the TApplication.BackendType values
     * @param windowWidth the number of text columns to start with
     * @param windowHeight the number of text rows to start with
     * @param fontSize the size in points
     * @throws Exception if TApplication can't instantiate the Backend.
     */
    @SuppressWarnings("this-escape")
    public DemoApplication(final BackendType backendType, final int windowWidth,
        final int windowHeight, final int fontSize) throws Exception {

        super(backendType, windowWidth, windowHeight, fontSize);

        addAllWidgets();
        getBackend().setTitle(i18n.getString("applicationTitle"));

        // Use custom theme by default.
        onMenu(new TMenuEvent(getBackend(), 10003));

        // Use window gradients by default.
        setMenuItemChecked(10010, true);
        onMenu(new TMenuEvent(getBackend(), 10010));

        // Expose terminal background image by default.
        // getMenuItem(10011).setChecked(true);
        // onMenu(new TMenuEvent(getBackend(), 10011));
    }

    // ------------------------------------------------------------------------
    // TApplication -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle menu events.
     *
     * @param menu menu event
     * @return if true, the event was processed and should not be passed onto
     * a window
     */
    @Override
    public boolean onMenu(final TMenuEvent menu) {

        if (menu.getId() == 2050) {
            new TEditColorThemeWindow(this);
            return true;
        }

        if (menu.getId() == TMenu.MID_OPEN_FILE) {
            try {
                String filename = fileOpenBox(".");
                 if (filename != null) {
                     try {
                         new TEditorWindow(this, new File(filename));
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                 }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }

        if (menu.getId() == 10000) {
            DemoMainWindow window = new DemoMainWindow(this);
            TMenuItem menuItem = getMenuItem(10010);
            window.setUseGradient(menuItem.isChecked());
            return true;
        }

        if (menu.getId() == 10001) {
            // Look cute: switch the color theme, window borders, and button
            // styles.
            applyRoundBorders();
            System.setProperty("casciian.TWindow.opacity", "80");
            System.setProperty("casciian.TImage.opacity", "80");
            System.setProperty("casciian.TTerminal.opacity", "80");

            getTheme().setFemme();
            for (TWindow window: getAllWindows()) {
                window.setBorderStyleForeground("round");
                window.setBorderStyleModal("round");
                window.setBorderStyleMoving("round");
                window.setBorderStyleInactive("round");
                window.setAlpha(80 * 255 / 100);
            }
            for (TMenu m: getAllMenus()) {
                m.setBorderStyleForeground("round");
                m.setBorderStyleModal("round");
                m.setBorderStyleMoving("round");
                m.setBorderStyleInactive("round");
                m.setAlpha(90 * 255 / 100);
            }

            oldDesktop = getDesktop();
            TDesktop newDesktop = new TDesktop(this);
            setDesktop(newDesktop);
            newDesktop.setBackgroundCell(null);
            setHideStatusBar(true);

            TMenuItem menuItem = getMenuItem(10011);
            menuItem.setChecked(true);

            onMenu(new TMenuEvent(getBackend(), 10011));
            return true;
        }

        if (menu.getId() == 10002) {
            // Look bland: switch the color theme, window borders, and button
            // styles.
            return applyBlandLook();
        }

        if (menu.getId() == 10003) {
            // Look "custom", sorta vaguely like Qmodem 5.
            applyRoundBorders();
            System.setProperty("casciian.TWindow.opacity", "90");
            System.setProperty("casciian.TImage.opacity", "90");
            System.setProperty("casciian.TTerminal.opacity", "90");

            getTheme().setQmodem5();
            for (TWindow window: getAllWindows()) {
                window.setBorderStyleForeground("round");
                window.setBorderStyleModal("round");
                window.setBorderStyleMoving("round");
                window.setBorderStyleInactive("round");
                window.setAlpha(90 * 255 / 100);
            }
            for (TMenu m: getAllMenus()) {
                m.setBorderStyleForeground("single");
                m.setBorderStyleModal("single");
                m.setBorderStyleMoving("single");
                m.setBorderStyleInactive("single");
                m.setAlpha(95 * 255 / 100);
            }
            setDesktop(new TDesktop(this));
            oldDesktop = getDesktop();
            setHideStatusBar(false);
            onMenu(new TMenuEvent(getBackend(), 10011));
            return true;
        }

        if (menu.getId() == 10004) {
            // Apply Casciian defaults: set all boolean properties to false,
            // disable gradients, and apply bland look
            SystemProperties.setAnimations(false);
            animationsChanged();

            SystemProperties.setTextMouse(false);
            SystemProperties.setTranslucence(false);
            SystemProperties.setShadowOpacity(60);

            // Update menu checkboxes
            setMenuItemChecked(10013, false);  // textMouse
            setMenuItemChecked(10014, false);  // animations
            setMenuItemChecked(10015, false);  // translucence
            setMenuItemChecked(10010, false);  // gradients

            // Disable gradients for all windows
            setUseGradientAllSupportedWindows(false);

            // Apply bland look
            return applyBlandLook();
        }

        if (menu.getId() == 10005) {
            setLocale(Locale.forLanguageTag(""));
            i18n = ResourceBundle.getBundle(DemoApplication.class.getName() + "Bundle",
                getLocale());
            getBackend().setTitle(i18n.getString("applicationTitle"));
            clearAllWidgets();
            addAllWidgets();
            return true;
        }

        if (menu.getId() == 10006) {
            setLocale(Locale.forLanguageTag("es"));
            i18n = ResourceBundle.getBundle(DemoApplication.class.getName() + "Bundle",
                getLocale());
            getBackend().setTitle(i18n.getString("applicationTitle"));
            clearAllWidgets();
            addAllWidgets();
            return true;
        }

        if (menu.getId() == 10010) {
            // Enable/disable gradients.
            TMenuItem menuItem = getMenuItem(menu.getId());
            boolean useGradient = menuItem.isChecked();

            setUseGradientAllSupportedWindows(useGradient);
            return true;
        }

        if (menu.getId() == 10013) {
            // Enable/disable text mouse.
            SystemProperties.setTextMouse(isMenuItemChecked(menu.getId()));
            boolean useGradient = isMenuItemChecked(10010);
            if (useGradient) {
                setUseGradientAllSupportedWindows(true);
            }
            return true;
        }

        if (menu.getId() == 10014) {
            // Enable/disable animations.
            TMenuItem menuItem = getMenuItem(menu.getId());
            SystemProperties.setAnimations(menuItem.isChecked());
            animationsChanged();

            return true;
        }

        if (menu.getId() == 10015) {
            // Enable/disable translucence.
            TMenuItem menuItem = getMenuItem(menu.getId());
            SystemProperties.setTranslucence(menuItem.isChecked());
            return true;
        }

        if (menu.getId() == 10011) {
            // Expose/cover terminal background.
            TMenuItem menuItem = getMenuItem(menu.getId());
            boolean exposeBackground = menuItem.isChecked();
            if (exposeBackground) {
                oldDesktop = getDesktop();
                TDesktop newDesktop = new TDesktop(this);
                setDesktop(newDesktop);
                newDesktop.setBackgroundCell(null);
            } else {
                setDesktop(oldDesktop);
            }
            return true;
        }

        if (menu.getId() == 10012) {
            // Shadow opacity dialog.
            new DemoShadowOpacityDialog(this);
            return true;
        }

        return super.onMenu(menu);
    }

    private void setUseGradientAllSupportedWindows(boolean useGradient) {
        for (TWindow window: getAllWindows()) {
            if (window instanceof DemoMainWindow windowMain) {
                windowMain.setUseGradient(useGradient);
            }
            if (window instanceof DemoCheckBoxWindow windowCheckBox) {
                windowCheckBox.setUseGradient(useGradient);
            }
        }
    }

    private void applyRoundBorders() {
        System.setProperty("casciian.TWindow.borderStyleForeground", "round");
        System.setProperty("casciian.TWindow.borderStyleModal", "round");
        System.setProperty("casciian.TWindow.borderStyleMoving", "round");
        System.setProperty("casciian.TWindow.borderStyleInactive", "round");
        System.setProperty("casciian.TEditColorTheme.borderStyle", "round");
        System.setProperty("casciian.TEditColorTheme.options.borderStyle", "round");
        System.setProperty("casciian.TEditDesktopStyle.borderStyle", "round");
        System.setProperty("casciian.TPanel.borderStyle", "round");
        System.setProperty("casciian.TRadioGroup.borderStyle", "round");
        System.setProperty("casciian.TScreenOptions.borderStyle", "round");
        System.setProperty("casciian.TScreenOptions.grid.borderStyle", "round");
        System.setProperty("casciian.TScreenOptions.options.borderStyle", "round");
    }

    private boolean applyBlandLook() {
        System.clearProperty("casciian.TWindow.borderStyleForeground");
        System.clearProperty("casciian.TWindow.borderStyleModal");
        System.clearProperty("casciian.TWindow.borderStyleMoving");
        System.clearProperty("casciian.TWindow.borderStyleInactive");
        System.clearProperty("casciian.TEditColorTheme.borderStyle");
        System.clearProperty("casciian.TEditColorTheme.options.borderStyle");
        System.clearProperty("casciian.TEditDesktopStyle.borderStyle");
        System.clearProperty("casciian.TPanel.borderStyle");
        System.clearProperty("casciian.TRadioGroup.borderStyle");
        System.clearProperty("casciian.TScreenOptions.borderStyle");
        System.clearProperty("casciian.TScreenOptions.grid.borderStyle");
        System.clearProperty("casciian.TScreenOptions.options.borderStyle");
        System.clearProperty("casciian.TWindow.opacity");
        System.clearProperty("casciian.TImage.opacity");
        System.clearProperty("casciian.TTerminal.opacity");
        System.clearProperty("casciian.TButton.style");

        getTheme().setDefaultTheme();
        for (TWindow window: getAllWindows()) {
            window.setBorderStyleForeground(null);
            window.setBorderStyleModal(null);
            window.setBorderStyleMoving(null);
            window.setBorderStyleInactive(null);
            window.setAlpha(90 * 255 / 100);

            for (TWidget widget: window.getChildren()) {
                if (widget instanceof TButton) {
                    ((TButton) widget).setStyle(TButton.Style.SQUARE);
                }
            }
        }
        for (TMenu m: getAllMenus()) {
            m.setBorderStyleForeground(null);
            m.setBorderStyleModal(null);
            m.setBorderStyleMoving(null);
            m.setBorderStyleInactive(null);
            m.setAlpha(90 * 255 / 100);
        }
        setDesktop(new TDesktop(this));
        oldDesktop = getDesktop();
        setHideStatusBar(false);
        onMenu(new TMenuEvent(getBackend(), 10011));
        return true;
    }

    /**
     * Show FPS.
     */
    @Override
    protected void onPreDraw() {
        if (getScreen() instanceof ECMA48Terminal terminal) {
            int bytes = terminal.getBytesPerSecond();
            String bps;
            if (bytes > 1024 * 1024 * 1024) {
                bps = String.format("%4.2f GB/s",
                    ((double) bytes / (1024 * 1024 * 1024)));
            } else if (bytes > 1024 * 1024) {
                bps = String.format("%4.2f MB/s",
                    ((double) bytes / (1024 * 1024)));
            } else if (bytes > 1024) {
                bps = String.format("%4.2f KB/s",
                    ((double) bytes / 1024));
            } else {
                bps = String.format("%d bytes/s", bytes);
            }
            menuTrayText = String.format("%s FPS %d", bps,
                getFramesPerSecond());
        } else {
            menuTrayText = String.format("FPS %d", getFramesPerSecond());
        }
    }

    // ------------------------------------------------------------------------
    // DemoApplication --------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Add all the widgets of the demo.
     */
    private void addAllWidgets() {
        // Temporarily disable animations so that this specific window does
        // not have an open effect.
        boolean oldHasAnimations = SystemProperties.isAnimations();
        SystemProperties.setAnimations(false);

        new DemoMainWindow(this);
        SystemProperties.setAnimations(oldHasAnimations);

        // Add the menus
        addToolMenu();
        addFileMenu();
        addEditMenu();

        TMenu demoMenu = addMenu(i18n.getString("demo"));
        demoMenu.addItem(10000, i18n.getString("mainWindow"));
        demoMenu.addSeparator();
        demoMenu.addItem(10001, i18n.getString("lookCute"));
        demoMenu.addItem(10002, i18n.getString("lookBland"));
        demoMenu.addItem(10003, i18n.getString("lookCustom"));
        demoMenu.addItem(10004, i18n.getString("applyCasciianDefaults"));
        TMenuItem gradients = demoMenu.addItem(10010,
            i18n.getString("useGradients"));
        gradients.setCheckable(true);
        gradients.setChecked(false);
        TMenuItem textMouseItem = demoMenu.addItem(10013,
            i18n.getString("textMouse"));
        textMouseItem.setCheckable(true);
        textMouseItem.setChecked(SystemProperties.isTextMouse());
        TMenuItem animationsItem = demoMenu.addItem(10014,
            i18n.getString("animations"));
        animationsItem.setCheckable(true);
        animationsItem.setChecked(SystemProperties.isAnimations());
        TMenuItem translucenceItem = demoMenu.addItem(10015,
            i18n.getString("translucence"));
        translucenceItem.setCheckable(true);
        translucenceItem.setChecked(SystemProperties.isTranslucence());
        TMenuItem backgroundImage = demoMenu.addItem(10011,
            i18n.getString("exposeBackground"));
        backgroundImage.setCheckable(true);
        backgroundImage.setChecked(false);
        demoMenu.addItem(10012, i18n.getString("shadowOpacity"));
        TSubMenu languageMenu = demoMenu.addSubMenu(i18n.getString("selectLanguage"));
        TMenuItem en = languageMenu.addItem(10005, i18n.getString("english"));
        TMenuItem es = languageMenu.addItem(10006, i18n.getString("espanol"));

        demoMenu.addSeparator();
        TMenuItem item = demoMenu.addItem(2000, i18n.getString("checkable"));
        item.setCheckable(true);
        item = demoMenu.addItem(2001, i18n.getString("disabled"));
        item.setEnabled(false);
        item = demoMenu.addItem(2002, i18n.getString("normal"));
        TSubMenu subMenu = demoMenu.addSubMenu(i18n.getString("subMenu"));
        item = demoMenu.addItem(2010, i18n.getString("normalAD"));
        item = demoMenu.addItem(2050, i18n.getString("colors"));

        item = subMenu.addItem(2000, i18n.getString("checkableSub"));
        item.setCheckable(true);
        item = subMenu.addItem(2001, i18n.getString("disabledSub"));
        item.setEnabled(false);
        item = subMenu.addItem(2002, i18n.getString("normalSub"));

        subMenu = subMenu.addSubMenu(i18n.getString("subMenu"));
        item = subMenu.addItem(2000, i18n.getString("checkableSub"));
        item.setCheckable(true);
        item = subMenu.addItem(2001, i18n.getString("disabledSub"));
        item.setEnabled(false);
        item = subMenu.addItem(2002, i18n.getString("normalSub"));

        addTableMenu();
        addWindowMenu();
        addHelpMenu();
    }

    /**
     * Clear all the widgets of the demo.
     */
    private void clearAllWidgets() {
        closeMenu();
        for (TMenu menu: getAllMenus()) {
            removeMenu(menu);
        }
        for (TWindow window: getAllWindows()) {
            closeWindow(window);
        }
    }

}
