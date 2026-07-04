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
import casciian.terminal.widget.TTerminalInformationWindow;
import casciian.terminal.widget.TTerminalWindow;

import java.io.UnsupportedEncodingException;

/**
 * Demo TUI application showcasing the Casciian Terminal component.
 *
 * <p>The terminal component provides the {@link TTerminalWindow} widget, which
 * embeds an ECMA-48 / ANSI X3.64 terminal emulator running a shell (or a
 * custom command) inside a Casciian window. It also ships
 * {@link TTerminalInformationWindow}, which reports the capabilities of the
 * outer terminal Casciian is running on.</p>
 *
 * <p>Neither widget lives in the core library: applications opt into them by
 * putting this component on the classpath / module path, keeping the core
 * free of terminal-specific logic and its dependencies.</p>
 */
public final class TerminalComponentDemoApplication extends TApplication {

    /**
     * Menu id: open a new embedded terminal window.
     */
    private static final int MID_OPEN_TERMINAL = 2000;

    /**
     * Public constructor.
     *
     * @param backendType the desired backend type
     * @throws UnsupportedEncodingException on backend errors
     */
    public TerminalComponentDemoApplication(final BackendType backendType)
        throws UnsupportedEncodingException {

        super(backendType);

        TMenu terminalMenu = addMenu("&Terminal");
        terminalMenu.addItem(MID_OPEN_TERMINAL, "&Open terminal");
        terminalMenu.addSeparator();
        // MID_TERMINAL_INFORMATION is defined by core Casciian; the terminal
        // component supplies the window that answers it.
        terminalMenu.addItem(TMenu.MID_TERMINAL_INFORMATION,
            "Terminal &information");

        addFileMenu();
        addWindowMenu();
        addHelpMenu();

        getBackend().setTitle("Casciian Terminal Component Demo");
    }

    /**
     * Handle the terminal menu items that this component contributes.
     *
     * @param menu the menu event
     * @return true if the event was consumed
     */
    @Override
    protected boolean onMenu(final TMenuEvent menu) {
        if (menu.getId() == MID_OPEN_TERMINAL) {
            new TTerminalWindow(this, 0, 0, TWindow.RESIZABLE);
            return true;
        }
        if (menu.getId() == TMenu.MID_TERMINAL_INFORMATION) {
            new TTerminalInformationWindow(this);
            return true;
        }
        return super.onMenu(menu);
    }
}
