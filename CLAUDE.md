# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build (also builds the demo fat JAR)
./gradlew build

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "casciian.bits.Palette256Test"

# Run a single test method
./gradlew test --tests "casciian.bits.Palette256Test.testFromRgb"

# Run demo (after building)
java -jar build/libs/casciian-demo-*.jar
# Windows: add --enable-native-access=ALL-UNNAMED
```

## Architecture

Casciian is a Turbo Vision–inspired Text User Interface library for Java. It targets Java 21 (toolchain) and deliberately avoids `java.desktop` so the library can compile to a GraalVM native image.

### Core widget hierarchy

- **`TApplication`** — the top-level event loop. Owns the `Backend`, color `ColorTheme`, widget tree, menus, and timers. Applications subclass it.
- **`TWidget`** — base class of every drawable, focusable object. Holds a `parent`, a list of `children`, and a `TWindow` reference. Widgets draw themselves via `Screen` draw calls and handle input events (`TKeypressEvent`, `TMouseEvent`, etc.).
- **`TWindow`** extends `TWidget` — a movable/resizable container. `TScrollableWindow` and specializations (editor, table, image…) live in `casciian/`.

### Backend / rendering pipeline

`TApplication` talks to `Backend` (interface). The backend provides both a `Screen` for drawing and a `TerminalReader` for input.

```
TApplication
  └── Backend (interface)
        ├── GenericBackend (abstract) — holds sessionInfo, screen, terminal
        │     ├── ECMA48Backend      — raw/xterm terminal via System.in/out or streams
        │     ├── HeadlessBackend    — no real terminal; used in tests
        │     ├── TWindowBackend     — renders into a TWindow (nested TUI)
        │     └── MultiBackend       — multiplexes several backends in parallel
        └── Screen (interface)      — all draw primitives (putCharXY, drawBox, …)
              └── MultiScreen
```

`ECMA48Backend` wraps an `ECMA48Terminal` which is also the screen. On Windows it always uses JLine for raw-mode; on Unix, JLine is opt-in via `casciian.useJline=true`.

### Color model (bits package)

Every cell carries a `CellAttributes` that holds:
- Named `Color` (16 CGA/ANSI colors)
- 24-bit RGB (`foreColorRGB` / `backColorRGB`, negative = unset)
- 256-color palette index (`foreColorPalette` / `backColorPalette`, negative = unset)

Precedence when emitting SGR: palette index > RGB > named color. Setting one representation clears the others per channel.

`Palette256` maps between the three representations. `ColorTheme` is the named theme map (keys like `"twindow.border"`) loaded from `$CASCIIANRC` or system properties.

`AnsiParser` parses ANSI SGR escape sequences into a `Cell` grid (used for rendering pre-formatted ANSI text, not for full terminal emulation).

### Event model

All input is routed as `TInputEvent` subclasses (`TKeypressEvent`, `TMouseEvent`, `TMenuEvent`, `TResizeEvent`, `TCommandEvent`). `TApplication` dispatches events down the active widget tree. Commands (`TCommand`) are integer constants used for menu actions and inter-widget communication.

### Effects & transforms

`casciian.effect` contains `Effect` (timed screen-level effects like fade/burn/wipe) and `CellTransform` implementations (gradient, mouse glow). `TApplication` runs pending effects each draw cycle.

### Image support

`casciian.image.decoders` uses a `ServiceLoader`-based `ImageDecoderRegistry`. The core library ships only `SixelImageDecoder`; the `casciian-java-desktop` add-on and demo fat JAR add BMP/JPEG/PNG/XPM decoders as separate `ImageDecoder` service implementations.

### Optional modules (sibling Gradle projects, not in this directory)

- `code-java-desktop/` — adds `javax.imageio`-based image decoders
- `code-terminal-component/` — adds `TTerminalWindow` (embedded ECMA-48 terminal emulator)

## License Headers

Every **new** Java source file must start with the Apache 2.0 header:

```java
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
```

When **modifying** a file that originated in the first commit (Autumn Lamonte, CC0), replace the CC0 header with:

```java
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
```

## Unit Testing Guidelines

- **Black-box only** — test the public API, not implementation details.
- **AssertJ and Mockito** are the preferred assertion/mocking libraries.
- **Avoid 1:1 test-to-class mapping** — one test class may cover several chained classes.
- **Test business logic only** — do not test constant values, JDK utilities, or external library behavior.
- When functionality is tightly coupled to UI, extract it into a standalone class first (like `AnsiParser`, `Palette256`) to enable unit testing.
