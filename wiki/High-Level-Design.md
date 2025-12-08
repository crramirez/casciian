Casciian's High-Level Design
============================

Casciian has three major areas of function: user-facing input/output
(events and screen), window management (TApplication), and widgets
(TWidget) and windows (TWindow).  This document describes in a general
manner what these pieces do and how they communicate.

The Big Picture
---------------

```mermaid
graph RL;
  X(Physical Keyboard) --> T(ECMA48Terminal / SwingTerminal)
  Y(Physical Mouse) --> T
  T -- flushPhysical --> Z(Physical Screen);
  T -- getEvents --> B(Backend);
  B -- TInputEvent --> C(TApplication);
  C --> TH1((Primary Thread));
  C --> TH2((Secondary Thread));
  TH1 -- onKeyboard --> W1[Window 1];
  TH1 -- onMouseDown --> W2[Window 2];
  TH1 -- onMouseMotion --> W3[ ... ... ... ];
  TH1 -- onResize --> W4[ Window N ];
  TH1 -- drawAll --> DA{Draw To Screen}
  DA --> W1;
  DA --> W2;
  DA --> W3;
  DA --> W4;
  W1 -- draw --> E(Screen);
  W2 -- draw --> E;
  W3 -- draw --> E;
  W4 -- draw --> E;
  E --> T;
  B -- getScreen --> C;
```

Events And Screen
-----------------

Input and output to the "physical" devices occur via received *events*
and updates to the *screen*:

* Events are received by calling casciian.backend.Backend.getEvents(),
  which puts any events into a list for higher layers to process.

* Backend.getScreen() returns a casciian.backend.Screen interface that
  has functions for drawing characters, strings, and lines to a
  logical screen buffer.  When it is time to push that logical buffer
  to the physical screen, Screen.flushPhysical() is called.  The
  SwingTerminal and ECMA48Terminal (Xterm) backends have logic to
  reduce the amount of actual data that must be drawn or emitted to
  the terminal.

Pictorially, the data flow looks like this:

```mermaid
graph RL;
  X(Physical Keyboard) --> T(ECMA48Terminal / SwingTerminal)
  Y(Physical Mouse) --> T
  T -- flushPhysical --> Z(Physical Screen);
  T -- getEvents --> B(Backend);
  W1(Client) -- draw --> E(Screen);
  B -- getEvents --> W1(Client);
  B -- getScreen --> W1(Client);
  E -- flushPhysical --> T
```

The casciian.backend package can be used standalone to provide keyboard,
mouse, and screen support for any kind of application.

Events are always concrete subclasses of abstract class
casciian.event.TInputEvent.  In addition to mouse press/release/movement
and keyboard presses, events are also generated for resizes (screen
and widget), menu selection, and general-purpose "command" events.

Screens encapsulate a two-dimensional grid of cells.  Any number of
actions can be performed on the screen, but none will be visible to
the end user until flushPhysical() is called.

Screen and Backend are both interfaces, so interesting things can be
done by composition:

* MultiBackend and MultiScreen multiplex a single TApplication to
  multiple terminals.  This is demonstrated by casciian.demos.Demo6: a
  single TApplication has *three* screens that can manipulate it: one
  Swing screen, one ECMA48 screen, and another Swing screen showing
  the application running inside a window.  Keyboard and mouse events
  from any of these screens will update all of them at once.  One
  could use MultiBackend/MultiScreen to build a detachable
  long-running application.

* TWindowBackend performs user I/O of a Backend within the window of a
  different TApplication.

Widgets And Windows
-------------------

Above the backend layer, Casciian organizes things into *widgets*
(TWidget subclasses) and *windows* (TWindow subclasses).  The key
principles of these objects are:

* TWidgets receive events and draw things to a TWindow.  A TWidget can
  have zero or many "child" widgets.  Every widget has a "parent"
  TWidget.  This hierarchy is used to route input events to the
  lowest-level widget to handle it.  On the drawing side, the
  hierarchy is drawn in a top-down manner, such that a parent widget
  draws what it needs first, and then its children are drawn, with
  each of those drawing its children, and so on.
  TWidget.drawChildren() contains code that sets up offset and
  clipping, so that the TWidget.draw() methods can assume that
  coordinate (0, 0) is the top-left corner.

* TWindows organize TWidgets into layered panels that can be moved,
  resized, and hidden.  TApplication manages the TWindows, routing
  user events to the active window, and drawing them in reverse Z
  order so that the active window gets drawn last (painter's
  algorithm, with positive Z pointing away from the user: Casciian's
  coordinate system is right-handed).

Both TWidget and TWindow are legitimate "God classes", they each have
code to do far more than a typical Java object should.  This is rather
common for a windowing system, as there are a great many corner cases
to a smoothly functioning mouse-supporting "graphical" user interface.

Most of Casciian's user interface elements are TWidget subclasses,
intended to be composed to make bigger applications.  A few more
complex elements are full windows: the operating shell terminal window
(a vttest-passing terminal emulator in its own right, capable of
running Casciian inside itself), font chooser, color theme editor, Java
exception dialog, modal message box, and modal file chooser.  Some
elements come in both flavors -- as a widget that can be put anywhere,
and as a window that provides a good out-of-the-box experience: a text
editor, terminal, and data table.

Window Management
-----------------

A significant part of TApplication's code is managing multiple
TWindows.

Text Cells
----------

The text cell model is this:

* A Cell can have text that is one or more codepoints.

* The logical screen is built up on every frame by having every
  widget/window draw in reverse-Z-order (the coordinate space is
  right-handed, so positive Z is away from the user).  Any Cell can
  cover any other Cell during this.

* For a GUI/Swing screen, Casciian has to build the whole frame (Swing by
  default is triple-buffered) and then push the frame to the actual
  OS.

* For an Xterm screen, ideally only the changed Cells will be output.
  Casciian will use Synchronized Output when it detects support for
  that.

