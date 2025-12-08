Casciian Escape Sequences
=========================

Goals
-----

Casciian's original project goal has been to expose the capabilities
of stock xterm to applications.  If a way to achieve its aims exists
in xterm's [defined
sequences](https://invisible-island.net/xterm/ctlseqs/ctlseqs.html),
then Casciian will use that.

Casciian's secondary goals have been:

* To provide feature parity between its ECMA48 and Swing backends, so
  that applications can seamlessly be used in either environment.

* To be able to run Casciian-inside-Casciian smoothly.

This document outlines the places where Casciian goes outside xterm's
defined sequences in order to achieve its secondary goals.



Images
------

xterm provides sixel for bitmap images, but does not provide a
non-palettized 24-bit image format.  Casciian can use both iTerm2 and
Jexer image protocols to transmit basic 24-bit bitmap images.



Nested Text Mouse Pointer
-------------------------

Running a ECMA8Terminal Casciian application inside a Casciian ECMA48
terminal window creates an issue with the text mouse pointer: both
"host" and "guest" application invert the cell to represent the mouse,
resulting in the mouse being invisible.  Casciian's solution is for
ECMA48Terminal backend to issue a Privacy Message containing the text
"hideMousePointer":

```
ESC ^ hideMousePointer ESC \
```

When ECMA48 terminal (TTerminalWindow) sees this message, it requests
its TApplication to not draw the mouse when that mouse is over the
window.  This results in the "guest" inverting the mouse cell, and all
subsequent hosts not inverting it, such that mouse pointer remains
visible to the end user.

On exit, ECMA48Terminal emits another Privacy Message containing
"showMousePointer":

```
ESC ^ showMousePointer ESC \
```

A non-Casciian terminal should quietly consume these Privacy Messages as
per the [VT320
specification](https://vt100.net/docs/vt320-uu/appendixe.html).
