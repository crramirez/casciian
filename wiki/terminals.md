Terminal Features
=================

Casciian can use the following features of a terminal if available:

* Mouse clicks.

* Mouse motion, including dragging and resizing windows, and
  SGR-Pixels mode (1016).
  [DECRQM/DECRPM](https://vt100.net/docs/vt510-rm/DECRQM.html) is used
  to detect SGR-Pixels support.

* 24-bit ("true color") RGB.

Terminals
---------

Most popular terminals can run Casciian, but a few might not support
all of Casciian's features.  The table below lists the terminals last
tested against Casciian:

| Terminal       | Environment        | Mouse Click | Mouse Cursor |
| -------------- | ------------------ | ----------- | ------------ |
| xterm          | X11                | yes         | yes          |
| alacritty      | X11                | yes         | yes          |
| contour        | X11                | yes         | yes          |
| foot           | Wayland            | yes         | yes          |
| ghostty        | X11                | yes         | yes          |
| gnome-terminal | X11                | yes         | yes          |
| iTerm2         | Mac                | yes         | yes          |
| kitty          | X11                | yes         | yes          |
| konsole        | X11                | yes         | yes          |
| lcxterm        | CLI, Linux console | yes         | yes          |
| mintty         | Windows            | yes         | yes          |
| mlterm         | X11                | yes         | yes          |
| putty          | X11, Windows       | yes         | yes          |
| rio            | X11, Windows, Mac  | yes         | yes          |
| RLogin         | Windows            | yes         | yes          |
| rxvt-unicode   | X11                | yes         | yes          |
| st-sx          | X11                | yes         | yes          |
| wezterm        | X11, Windows, Mac  | yes         | yes          |
| Windows Terminal | Windows          | yes         | yes          |
| xfce4-terminal | X11                | yes         | yes          |
| xterm.js       | Web                | yes         | yes          |
| zutty          | X11                | yes         | yes          |
| DomTerm        | Web                | yes         | no           |
| darktile       | X11                | yes         | no           |
| yakuake        | X11                | yes         | no           |
| screen         | CLI                | yes(1)      | yes(1)       |
| tmux           | CLI                | yes(1)      | yes(1)       |
| zellij         | CLI                | yes         | yes          |
| qodem(2)       | CLI, Linux console | yes         | yes(3)       |
| qodem-x11      | X11                | yes         | no           |
| yaft           | Linux console (FB) | no          | no           |
| Linux          | Linux console      | no          | no           |
| MacTerm        | Mac                | no          | no           |

1 - Requires mouse support from host terminal.

2 - Latest in repository.

3 - Requires TERM=xterm-1003 before starting.

When running on the raw Linux console,
[LCXterm](https://gitlab.com/AutumnMeowMeow/lcxterm) or
[Qodem](http://qodem.sourceforge.net) are required if one wishes to
use the mouse.  [GPM](https://github.com/telmich/gpm) is also
required.

Terminal Widgets
----------------

The table below lists embeddable widgets tested against Casciian recently:

| Terminal       | Language | Mouse Click | Mouse Cursor | Link |
| -------------- | -------- | ----------- | ------------ | ---- |
| jexer (ECMA48) | Java     | yes         | yes          | https://gitlab.com/AutumnMeowMeow/jexer
| upp-components Terminal | C++  | yes    | yes          | https://github.com/ismail-yilmaz/upp-components/tree/master/CtrlLib/Terminal
| xterm.js(2)    | TypeScript | yes       | yes          | https://xtermjs.org
| gowid          | Go       | yes         | yes          | https://github.com/gcla/gowid
| urwid          | Python   | no          | no           | https://github.com/urwid/urwid
| JediTerm       | Java     | yes         | no           | https://github.com/JetBrains/jediterm
