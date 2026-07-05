Casciian Terminal Component
===========================

Optional component for the [Casciian](../README.md) text user interface
library that provides an ECMA-48 / ANSI X3.64 style terminal emulator and
the widgets built on top of it.

This project lives under `code-terminal-component/` and is **independent**
from the main `code/` project. It is published separately to Maven
Central, so applications can opt in only when they actually need it.

Why a separate component?
-------------------------

The terminal emulator (`casciian.terminal.ECMA48`) and the
`TTerminalWindow` widget represent a specific use case: embedding a
terminal that runs a shell or child process, or rendering "ANSI Art"
through a full terminal emulator. That code — and the dependencies it
tends to pull in for spawning and driving child processes — is not useful
for the majority of applications, and it noticeably increases the size of
the core library.

Keeping this functionality in a separate component means the core stays
small and focused. Applications that need an embedded terminal add a
single extra dependency; everyone else keeps a leaner core.

What's in here?
---------------

* **`casciian-terminal-component`** — the component itself, packaged as a
  JPMS module `casciian.terminal.component`. It depends on `casciian`.
  It provides:
    * `casciian.terminal` — the terminal emulator and its supporting
      types (`ECMA48`, `DisplayLine`, `TerminalListener`,
      `TerminalState`, `DECCharacterSets`).
    * `casciian.terminal.widget` — the Casciian widgets that expose the
      emulator:
        * `TTerminal` / `TTerminalWindow` — an embedded terminal running a
          shell or a custom command line.
        * `TTextPicture` / `TTextPictureWindow` — *(deprecated)* display
          "ANSI Art" using the full terminal emulator. Prefer
          `casciian.TTextAnsi` / `casciian.TTextAnsiWindow` in core for
          simpler ANSI rendering.
* **`demo`** — a small TUI application demonstrating the component. Its
  **File → OS Shell** item opens a `TTerminalWindow` running your shell.
  Built as a fat JAR via the `jarDemo` task.

Building
--------

The project uses Gradle (wrapper included). At build time it uses a
[Gradle composite build](https://docs.gradle.org/current/userguide/composite_builds.html)
to substitute the published `io.github.crramirez:casciian` artifact with
the sibling project under `../code`, so you don't need to publish a
SNAPSHOT first to build locally:

```sh
cd code-terminal-component
./gradlew build
```

To produce the demo fat JAR:

```sh
./gradlew :demo:jarDemo
java -jar demo/build/libs/casciian-terminal-component-demo-<version>.jar
```

The fat JAR bundles the demo, the component, the core casciian library and
all runtime dependencies (including JLine), so it can be run standalone.

Using the component in your application
---------------------------------------

Once published, add it as a dependency alongside core casciian:

```gradle
dependencies {
    implementation "io.github.crramirez:casciian:<version>"
    implementation "io.github.crramirez:casciian-terminal-component:<version>"
}
```

Then open a terminal window from your `TApplication`:

```java
import casciian.TWindow;
import casciian.terminal.widget.TTerminalWindow;

// Spawn the user's shell in a resizable window at (0, 0).
new TTerminalWindow(application, 0, 0, TWindow.RESIZABLE);
```

License
-------

Apache License, Version 2.0. See the project [LICENSE](../LICENSE).
