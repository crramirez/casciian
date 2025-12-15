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
import casciian.bits.BorderStyle;
import casciian.backend.ECMA48Terminal;
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
    private ResourceBundle i18n = ResourceBundle.getBundle(DemoApplication.class.getName());

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
     * @throws Exception if TApplication can't instantiate the Backend.
     */
    @SuppressWarnings("this-escape")
    public DemoApplication(final BackendType backendType) throws Exception {
        super(backendType);
        addAllWidgets();
        getBackend().setTitle(i18n.getString("applicationTitle"));

        // Use custom theme by default.
        onMenu(new TMenuEvent(getBackend(), 10003));

        // Use window gradients by default.
        getMenuItem(10010).setChecked(true);
        onMenu(new TMenuEvent(getBackend(), 10010));
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
        getMenuItem(10010).setChecked(true);
        onMenu(new TMenuEvent(getBackend(), 10010));
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
            setDesktop(null);
            setHideStatusBar(true);
            return true;
        }

        if (menu.getId() == 10002) {
            // Look bland: switch the color theme, window borders, and button
            // styles.
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
            setHideStatusBar(false);
            return true;
        }

        if (menu.getId() == 10003) {
            // Look "custom", sorta vaguely like Qmodem 5.
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
            setHideStatusBar(false);
            return true;
        }

        if (menu.getId() == 10005) {
            setLocale(Locale.forLanguageTag(""));
            i18n = ResourceBundle.getBundle(DemoApplication.class.getName(),
                getLocale());
            getBackend().setTitle(i18n.getString("applicationTitle"));
            clearAllWidgets();
            addAllWidgets();
            return true;
        }

        if (menu.getId() == 10006) {
            setLocale(Locale.forLanguageTag("es"));
            i18n = ResourceBundle.getBundle(DemoApplication.class.getName(),
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

            for (TWindow window: getAllWindows()) {
                if (window instanceof DemoMainWindow) {
                    ((DemoMainWindow) window).setUseGradient(useGradient);
                }
                if (window instanceof DemoCheckBoxWindow) {
                    ((DemoCheckBoxWindow) window).setUseGradient(useGradient);
                }
            }
            return true;
        }

        return super.onMenu(menu);
    }

    /**
     * Show FPS.
     */
    @Override
    protected void onPreDraw() {
        if (getScreen() instanceof ECMA48Terminal) {
            ECMA48Terminal terminal = (ECMA48Terminal) getScreen();
            int bytes = terminal.getBytesPerSecond();
            String bps = "";
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
        boolean oldHasAnimations = getAnimations();
        setAnimations(false);
        DemoMainWindow mainWindow = new DemoMainWindow(this);
        setAnimations(oldHasAnimations);

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
        TMenuItem gradients = demoMenu.addItem(10010,
            i18n.getString("useGradients"));
        gradients.setCheckable(true);
        gradients.setChecked(false);
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
        item = demoMenu.addItem(2010, i18n.getString("normal"));
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
