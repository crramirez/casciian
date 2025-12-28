# Casciian wiki (local copy)

> This page is meant to be copied into the GitHub wiki repository
> (`crramirez/casciian.wiki`). It lives in-tree so changes can be reviewed
> alongside code before publishing.

Casciian is a text-based windowing system (ANSI/ECMA-48) with mouse support,
translucent windows, gradients, terminal background exposure, and a polished
control set reminiscent of Turbo Vision. It is suited for building full
screen terminal tools that feel like desktop apps.

![Casciian demo screenshot](https://github.com/user-attachments/assets/69312af6-36b3-46a2-a4c9-d77a1e804e68)

## Requirements

- Java 21 or newer
- An ECMA-48/XTerm-compatible terminal (tmux/screen are supported; mouse
  capture must be enabled in the terminal)

## Using the library

### As a dependency

Add the library to your build (replace the version with the latest tagged
release when available):

```kotlin
// Gradle (Kotlin DSL)
repositories {
    mavenCentral()
    // Or use mavenLocal() after running `./gradlew publishToMavenLocal`
}

dependencies {
    implementation("io.github.crramirez:casciian:1.1-SNAPSHOT")
}
```

### From source

```bash
cd code
./gradlew assemble              # builds casciian-<ver>.jar and casciian-full-<ver>.jar
./gradlew run                   # launches demo.Demo1 with the ECMA-48 backend
java -jar build/libs/casciian-full-<ver>.jar   # run the demo from the fat jar
```

To experiment with the telnet/multi-screen demo:

```bash
java -cp build/libs/casciian-full-<ver>.jar demo.Demo8 2323
telnet localhost 2323
```

## Your first Casciian app

```java
import casciian.TApplication;
import casciian.TAction;
import casciian.TField;
import casciian.TWindow;

public class HelloCasciian extends TApplication {

    public HelloCasciian() throws Exception {
        super(BackendType.XTERM); // ECMA-48 terminal backend

        TWindow window = addWindow("Hello Casciian", 50, 12);
        window.addLabel("Enter your name:", 2, 2);
        TField name = window.addField(2, 3, 30, false, "");

        window.addButton("&Say hi", 2, 5, new TAction() {
            @Override
            public void DO() {
                messageBox("Hi " + name.getText() + "!");
            }
        });
    }

    public static void main(String[] args) throws Exception {
        new HelloCasciian().run();   // starts the event loop
    }
}
```

Run it with `java HelloCasciian`. The `run()` call blocks until the user exits
or `shutdown()` is called.

## Windows, widgets, and layout

- Core widgets include buttons, fields/password fields, check boxes, radio
  groups, combo boxes, lists, tables, tree views, text areas/editors,
  progress bars, spinners, and panels/split panes for composition.
- Widgets live inside `TWindow` (or sub-containers like `TPanel`). Coordinates
  are 0-based and measured in character cells.
- Layout managers (`AnchoredLayoutManager`, `BoxLayoutManager`,
  `StretchLayoutManager`) can reposition children on resize. Set them on any
  `TWidget` container.

Example: stack widgets vertically using `BoxLayoutManager`:

```java
TWindow window = addWindow("Layout demo", 60, 16);
window.setLayoutManager(new BoxLayoutManager(window.getWidth(),
                                             window.getHeight(), true));

window.addLabel("Tasks", 1, 1);
window.addList(1, 2, 25, 6, java.util.List.of("alpha", "beta", "gamma"));
window.addButton("&Close", 1, 9, new TAction() {
    @Override
    public void DO() {
        shutdown();
    }
});
```

## Backends and I/O

- `BackendType.XTERM` (alias `BackendType.ECMA48`) targets modern terminals
  with mouse and resize support.
- You can construct `TApplication` with custom streams (`InputStream`,
  `Reader`, `PrintWriter`) to integrate with telnet/SSH servers, or pass a
  prebuilt `Backend` (e.g., `ECMA48Backend`, `HeadlessBackend`).
- `MultiBackend` + `MultiScreen` let you share one application across several
  clients (see `demo.Demo8`).

## Styling, effects, and runtime switches

Casciian reads system properties to toggle visuals without code changes:

- `casciian.animations` (default: `false`)
- `casciian.shadowOpacity` (0–100, default `60`)
- `casciian.translucence` (default: `false`)
- `casciian.useTerminalPalette` (default: `false`, use CGA palette otherwise)
- `casciian.disablePreTransform` / `casciian.disablePostTransform`
  (default: `false`) to skip gradient and other cell transforms
- `casciian.menuIcons` (default: `true`)
- `casciian.hideMenuBar`, `casciian.hideStatusBar`, `casciian.hideMouseWhenTyping`
  (default: `false`)
- `casciian.textMouse` (default: `false`)
- `casciian.textBlink` (default: `true`), `casciian.blinkMillis` (0–500,
  default `500`), `casciian.blinkDimPercent` (0–100, default `80`)

Example:

```bash
java -Dcasciian.animations=true \
     -Dcasciian.translucence=true \
     -Dcasciian.shadowOpacity=70 \
     -jar build/libs/casciian-full-<ver>.jar
```

## Patterns and tips

- Actions are represented by `TAction`; use anonymous subclasses to react to
  clicks, selection changes, timers, etc.
- `messageBox`, `TInputBox`, `TFileOpenBox`, `TExceptionDialog`, and
  `TTerminalInformationWindow` provide ready-made dialogs.
- Menus/toolbars: `addToolMenu()`, `addFileMenu()`, and `addWindowMenu()` add
  standard menus; override `onMenu(TMenuEvent)` for custom commands.
- For non-UTF-8 encodings, use the constructor that accepts a `Reader`/`Writer`
  (see `demo.Demo3`).

## Where to look next

- `src/main/java/demo/` contains focused examples:
  - `Demo2`: telnet server + ECMA-48 backend
  - `Demo4`: hidden windows/custom desktop
  - `Demo7`: layout manager usage
  - `Demo8`: headless + MultiBackend broadcasting
- `TApplication` and `TWidget` Javadocs describe the full API surface.

Copy any updates from this page into the wiki repository to publish them.
