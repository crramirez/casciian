Casciian - Java Text User Interface library
===========================================

This library implements a text-based windowing system loosely
reminiscent of Borland's [Turbo
Vision](http://en.wikipedia.org/wiki/Turbo_Vision) system for
Xterm-like terminals.

Casciian can be run inside its own terminal window, with support for
all of its features including mouse, and of course more terminals.

Casciian has seen inspiration from several other projects:

* Translucent windows were inspired by
  [notcurses](https://github.com/dankamongmen/notcurses).  Translucent
  windows and layered images generally look as one would expect in a
  modern graphical environment...but it's mostly text.

* Pulsing button text, window effects, and desktop effects were
  inspired by [vtm's](https://github.com/netxs-group/vtm) incredibly
  slick game-like aesthetic.

<img width="1612" height="996" alt="image" src="https://github.com/user-attachments/assets/69312af6-36b3-46a2-a4c9-d77a1e804e68" />

<img width="884" height="639" alt="image" src="https://github.com/user-attachments/assets/73f9f917-544b-4662-ad3c-9a925c0c92f4" />

## Getting Started

If you want to start your own Casciian-based application, there is a
[project template](https://github.com/crramirez/casciian-app-template)
ready to use as a repository seed.

## Spring Boot Integration

For Spring Boot users, the
[casciian-spring](https://github.com/crramirez/casciian-spring) project
provides a Spring Boot 3.x auto-configuration starter that exposes a
Casciian text user interface over SSH. This is useful when you want to
ship an admin or operations TUI alongside a regular web application —
for example, a customer-facing web shop served over HTTP plus an admin
console that operates on the same backing database, accessible by
simply `ssh`-ing into the running Spring Boot process. The repository
also includes a runnable `demo-shop` showing this exact use case.

## Micronaut Integration

For Micronaut users, the
[casciian-micronaut](https://github.com/crramirez/casciian-micronaut)
companion project provides Micronaut integration for exposing
Casciian-based TUIs from Micronaut applications. Like
`casciian-spring`, it is intentionally separate from the core Casciian
artifact so the main toolkit stays framework-neutral while still giving
Micronaut applications a practical way to add SSH-accessible operations
or admin terminals.

## Java Desktop Add-on (from Casciian 1.5)

Casciian's core library deliberately avoids any dependency on the JDK's
`java.desktop` module so that applications can be compiled with GraalVM
`native-image`. If your application instead runs on a regular JVM and
you want to opt into `java.desktop` capabilities (for example, decoding
PNG/JPEG images via `javax.imageio.ImageIO`), use the optional
[`casciian-java-desktop`](code-java-desktop/README.md) add-on. It is a
separate Gradle multi-project under `code-java-desktop/` and is
publishable to Maven Central, so users can simply add it as an extra
dependency when they need it. This add-on will be released starting from
Casciian version 1.5.

## Terminal Component (from Casciian 1.6)

The embedded terminal — the ECMA-48 / ANSI X3.64 terminal emulator and the
`TTerminalWindow` widget that runs a shell or child process inside a
Casciian window — lives in the optional
[`casciian-terminal-component`](code-terminal-component/README.md) module
rather than in the core library. This keeps the core small and free of the
terminal-specific logic and dependencies that only matter to applications
embedding a terminal. It is a separate Gradle multi-project under
`code-terminal-component/` and is publishable to Maven Central, so users
can add it as an extra dependency when they need it. This component will be
released starting from Casciian version 1.6.

## License

This project is distributed under the Apache License, Version 2.0.

### Provenance

Casciian is derived from a codebase originally written by Autumn Lamonte
and dedicated to the public domain via CC0. https://codeberg.org/AutumnMeowMeow/casciian

The original public-domain dedication remains in effect for the
historical code. Subsequent modifications, maintenance, and releases
of this distribution are provided under the Apache License, Version 2.0.

Nothing in this license restricts the original public-domain status of
the upstream code.


Copyright Status
----------------

Copyright © 2013–2025 Autumn Lamonte  
Copyright © 2025 Carlos Rafael Ramirez

Maintained by Carlos Rafael Ramirez.

Running The Demo
----------------

src/demo contains official demos showing all of the stock UI controls.
The demos can be run as follows:

**Note for Windows users:** When running on Windows, you may need to add the 
`--enable-native-access=ALL-UNNAMED` flag to enable native terminal support.
For example: `java --enable-native-access=ALL-UNNAMED -jar casciian-demo.jar`

  * `java -jar casciian-demo.jar` .  This will use
    System.in/out with Xterm-like sequences.

  * `java -cp casciian-demo.jar demo.Demo2 PORT` (where PORT is a
    number to run the TCP daemon on).  This will use the Xterm backend
    on a telnet server that will update with screen size changes.

  * `java -cp casciian-demo.jar demo.Demo3` .  This will use
    System.in/out with Xterm-like sequences.  One can see in the code
    how to pass a different InputReader and OutputReader to
    TApplication, permitting a different encoding than UTF-8; in this
    case, code page 437.

  * `java -cp casciian-demo.jar demo.Demo4` .  This demonstrates
    hidden windows and a custom TDesktop.

  * `java -cp casciian-demo.jar demo.Demo7` .  This demonstrates the
    BoxLayoutManager, achieving a similar result as the
    javax.swing.BoxLayout apidocs example.

  * `java -cp casciian-demo.jar demo.Demo8 PORT` (where PORT is a
    number to run the TCP daemon on).  This will use the Xterm backend
    on a telnet server to share one screen to many terminals.

Editing the Wiki
----------------

The GitHub Wiki lives in a separate repository (`crramirez/casciian.wiki`).
Changes made in this source repository do **not** modify the wiki. To propose
wiki updates, clone the wiki repository directly:

```
git clone https://github.com/crramirez/casciian.wiki.git
```

Submit your changes there (or share patches with the maintainer) to update the
published wiki pages.

Configuration
-------------

Casciian's runtime behavior can be tuned through Java system properties (all
prefixed with `casciian.`), either via `-D` JVM options or through a
properties file whose path is provided by the `CASCIIANRC` environment
variable.

### Bold attribute and bright colors (since 1.6.0)

Starting with **1.6.0**, the bold attribute no longer produces bright
(high-intensity) colors on its own. Bold is now emitted as a real SGR bold
(`ESC[1m`); to guarantee the text is shown as bold and *not* brightened —
regardless of how the terminal is configured — Casciian pins a normal
(non-bright) palette color to its exact RGB value. The result is bold text in
its original color on every terminal.

The pin also applies when `casciian.useTerminalPalette` is enabled: Casciian
always queries the terminal's own ANSI colors on connect and reconciles the
response into its internal palette (redrawing the screen once it arrives), so
the pinned RGB reflects the terminal's native theme rather than Casciian's own
defaults, once the terminal has responded to that query.

If you rely on the legacy "bold means bright" behavior — most commonly with
custom, **non-RGB** color themes — enable the following property to restore
it:

| Property | Values | Default | Description |
| --- | --- | --- | --- |
| `casciian.treatBoldAsBright` | `true` / `false` | `false` | When `true`, the bold attribute is rendered using the bright color palette (legacy behavior). |

We recommend enabling `casciian.treatBoldAsBright=true` if you have custom,
non-RGB themes that use the `bold` keyword and want to keep their previous
bright appearance. Themes that use the `bright` keyword are unaffected by
this property: `bright` always selects the bright color directly, while
`bold` sets the bold attribute (whose bright rendering is governed by this
property).

Note that the built-in ECMA-48 terminal emulator always treats a received bold
attribute transparently (as if this property were `false`), so terminal
sessions are reproduced faithfully regardless of the setting.

### 256-color palette (color cube)

In addition to the 16 ANSI colors and 24-bit RGB (`ESC[38;2;r;g;b`), Casciian
can emit colors from the terminal's 256-color palette (the "color cube") using
the compact indexed form `ESC[38;5;n` / `ESC[48;5;n`. Because an indexed color
is a single small number rather than a full RGB triple, it produces shorter
escape sequences and is cheaper to render, which is useful when an approximate
color is acceptable.

To use it, set a palette index on a cell's attributes:

```java
CellAttributes attr = new CellAttributes();
attr.setForeColorPalette(196);   // bright red from the color cube
attr.setBackColorPalette(21);    // blue from the color cube
```

The `casciian.bits.Palette256` helper makes it easy to obtain indices:

* `Palette256.fromColor(Color)` returns the palette index (0–15) for one of
  the 16 CGA/ANSI colors, with an optional `bright` variant.
* `Palette256.fromCgaColor(Color)` maps one of the 16 CGA/ANSI colors to the
  closest fixed color-cube / grayscale index (16–255), giving a
  terminal-independent color even when the terminal's base 16 palette is
  remapped, with an optional `bright` variant.
* `Palette256.fromRgb(int rgb)` returns the closest palette index for an
  arbitrary 24-bit RGB value, searching both the 6×6×6 color cube and the
  grayscale ramp.
* `Palette256.toRgb(int index)` returns the RGB value for a palette index.

Palette, RGB, and named colors are mutually exclusive per channel: setting one
clears the others.

The system property `casciian.ECMA48.paletteColor` (default `false`) forces the
16 named system colors to be emitted as fixed color-cube palette indices,
mirroring `casciian.ECMA48.rgbColor` for 24-bit RGB. Only the 16 named colors
are affected; cells that already carry a true RGB color are left untouched.
This is enabled automatically for terminals such as Konsole whose default
16-color palette has low contrast between normal and bright colors.

On the input side, `AnsiParser` and the ECMA-48 terminal emulator apply
received `38;5;n` / `48;5;n` sequences as a palette index directly (rather
than resolving the index to RGB), so parsed cells preserve the compact
palette representation end-to-end.
