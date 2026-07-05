/*
 * Casciian Terminal component - demo
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

import casciian.TApplication;
import casciian.TWindow;
import casciian.event.TMenuEvent;
import casciian.menu.TMenu;
import casciian.TStatusBar;
import casciian.terminal.widget.TTerminalWindow;

import java.io.UnsupportedEncodingException;
import java.util.ResourceBundle;

import static casciian.TKeypress.kbF1;
import static casciian.TCommand.cmHelp;

/**
 * Demo TUI application showcasing the Casciian Terminal component.
 *
 * <p>The terminal component provides the {@link TTerminalWindow} widget, which
 * embeds an ECMA-48 / ANSI X3.64 terminal emulator running a shell (or a
 * custom command) inside a Casciian window.  This demo wires it up behind the
 * standard "OS Shell" File-menu item.</p>
 *
 * <p>The widget does not live in the core library: applications opt into it by
 * putting this component on the classpath / module path, keeping the core
 * free of terminal-specific logic and its dependencies.</p>
 */
public final class TerminalComponentDemoApplication extends TApplication {

    /**
     * The name of the resource bundle for this class.
     */
    public static final String RESOURCE_BUNDLE_NAME =
        TerminalComponentDemoApplication.class.getName() + "Bundle";

    /**
     * Translated strings.
     */
    private ResourceBundle i18n =
        ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME);

    /**
     * Public constructor.
     *
     * @param backendType the desired backend type
     * @throws UnsupportedEncodingException on backend errors
     */
    public TerminalComponentDemoApplication(final BackendType backendType)
        throws UnsupportedEncodingException {

        super(backendType);

        addToolMenu();
        // File menu: the "OS Shell" item opens an embedded terminal window.
        // MID_SHELL is defined by core Casciian; the terminal component
        // supplies the window that answers it.
        TMenu fileMenu = addMenu(i18n.getString("fileMenuTitle"));
        fileMenu.addDefaultItem(TMenu.MID_CHANGE_DIR);
        fileMenu.addDefaultItem(TMenu.MID_SHELL);
        fileMenu.addSeparator();
        fileMenu.addDefaultItem(TMenu.MID_EXIT);
        TStatusBar statusBar = fileMenu.newStatusBar(
            i18n.getString("fileMenuStatus"));
        statusBar.addShortcutKeypress(kbF1, cmHelp, i18n.getString("Help"));

        addEditMenu();
        addWindowMenu();
        addHelpMenu();

        getBackend().setTitle(i18n.getString("applicationTitle"));

        new TTerminalWindow(this, 0, 0, TWindow.RESIZABLE);
    }

    /**
     * Handle the menu items that this demo contributes.
     *
     * @param menu the menu event
     * @return true if the event was consumed
     */
    @Override
    protected boolean onMenu(final TMenuEvent menu) {
        if (menu.getId() == TMenu.MID_SHELL) {
            new TTerminalWindow(this, 0, 0, TWindow.RESIZABLE);
            return true;
        }
        return super.onMenu(menu);
    }
}
