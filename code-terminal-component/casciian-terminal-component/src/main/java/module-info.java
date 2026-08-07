/*
 * Casciian Terminal component
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

/**
 * Casciian Terminal component.
 *
 * <p>Optional component that provides an ECMA-48 / ANSI X3.64 style terminal
 * emulator ({@code casciian.terminal.ECMA48}) together with the widgets that
 * expose it: {@code TTerminalWindow} embeds a terminal running a shell or a
 * child process, and {@code TTextPicture} renders "ANSI Art" through the same
 * emulator.</p>
 *
 * <p>Use this component when your application needs to embed a terminal or
 * render ANSI art with a full terminal emulator. Keeping this code out of the
 * core library avoids pulling terminal-specific logic into applications that
 * do not need it.</p>
 */
module casciian.terminal.component {
    requires transitive casciian;
    requires org.jline.terminal;

    exports casciian.terminal;
    exports casciian.terminal.widget;
}
