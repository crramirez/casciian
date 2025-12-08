Terminal Features
=================

Casciian can use the following features of a terminal if available:

* Mouse clicks.

* Mouse motion, including dragging and resizing windows, and
  SGR-Pixels mode (1016).
  [DECRQM/DECRPM](https://vt100.net/docs/vt510-rm/DECRQM.html) is used
  to detect SGR-Pixels support.

* 24-bit ("true color") RGB.

* Image support via sixel or iTerm2.  There is currently [no
  documented means](https://gitlab.com/gnachman/iterm2/issues/8940) of
  detecting iTerm2 image support, so Casciian will emit iTerm2 images
  when casciian.ECMA48.iTerm2Images=true, or when it detects (via
  XTVERSION) WezTerm, mintty, konsole, or iTerm2.

Terminals
---------

Most popular terminals can run Casciian, but only a few support all of
Casciian's features.  The table below lists the terminals last tested
against Casciian:

| Terminal       | Environment        | Mouse Click | Mouse Cursor | Images |
| -------------- | ------------------ | ----------- | ------------ | ------ |
| xterm          | X11                | yes         | yes          | yes    |
| wezterm        | X11, Windows, Mac  | yes         | yes          | yes(7) |
| foot           | Wayland            | yes         | yes          | yes    |
| contour        | X11                | yes         | yes          | yes    |
| mintty         | Windows            | yes         | yes          | yes(7) |
| mlterm         | X11                | yes         | yes          | yes    |
| RLogin         | Windows            | yes         | yes          | yes    |
| st-sx          | X11                | yes         | yes          | yes    |
| xterm.js(8)    | Web                | yes         | yes          | yes    |
| Windows Terminal(6) | Windows       | yes         | yes          | yes    |
| alacritty(3b)  | X11                | yes         | yes          | yes    |
| ghostty        | X11                | yes         | yes          | no     |
| gnome-terminal | X11                | yes         | yes          | no     |
| iTerm2         | Mac                | yes         | yes          | no(5)  |
| kitty          | X11                | yes         | yes          | no     |
| konsole        | X11                | yes         | yes          | no(10) |
| lcxterm        | CLI, Linux console | yes         | yes          | no     |
| putty          | X11, Windows       | yes         | yes          | no(2)  |
| rio            | X11, Windows, Mac  | yes         | yes          | no(10) |
| rxvt-unicode   | X11                | yes         | yes          | no(2)  |
| xfce4-terminal | X11                | yes         | yes          | no     |
| zutty          | X11                | yes         | yes          | no(2)  |
| DomTerm        | Web                | yes         | no           | no(10) |
| darktile       | X11                | yes         | no           | no(5)  |
| yakuake        | X11                | yes         | no           | no     |
| screen         | CLI                | yes(1)      | yes(1)       | no(2)  |
| tmux           | CLI                | yes(1)      | yes(1)       | no     |
| zellij         | CLI                | yes         | yes          | no(9)  |
| qodem(3)       | CLI, Linux console | yes         | yes(4)       | no     |
| qodem-x11      | X11                | yes         | no           | no     |
| yaft           | Linux console (FB) | no          | no           | yes    |
| Linux          | Linux console      | no          | no           | no(2)  |
| MacTerm        | Mac                | no          | no           | no(2)  |

1 - Requires mouse support from host terminal.

2 - Also fails to filter out sixel data, leaving garbage on screen.

3 - Latest in repository.

3b - Latest in repository, using graphics PR branch.

4 - Requires TERM=xterm-1003 before starting.

5 - Sixel images can crash terminal.

6 - Windows Terminal 1.22 Preview, released on August 27, 2024.

7 - Both sixel and iTerm2 images.

8 - Using jerch's xterm-addon-image.

9  - zellij supports sixel, but Casciian's output can overwhelm it,
     resulting in it becoming unresponsive to user input.

10 - Terminal has sixel or iTerm2 support, but shows artifacts.

When running on the raw Linux console,
[LCXterm](https://gitlab.com/AutumnMeowMeow/lcxterm) or
[Qodem](http://qodem.sourceforge.net) are required if one wishes to
use the mouse.  [GPM](https://github.com/telmich/gpm) is also
required.

Terminal Widgets
----------------

The table below lists embeddable widgets tested against Casciian recently:

| Terminal       | Language | Mouse Click | Mouse Cursor | Images | Link |
| -------------- | -------- | ----------- | ------------ | ------ | ---- |
| jexer (ECMA48) | Java     | yes         | yes          | yes    | https://gitlab.com/AutumnMeowMeow/jexer
| upp-components Terminal | C++  | yes    | yes          | yes    | https://github.com/ismail-yilmaz/upp-components/tree/master/CtrlLib/Terminal
| xterm.js(2)    | TypeScript | yes       | yes          | yes    | https://xtermjs.org
| gowid          | Go       | yes         | yes          | no(1)  | https://github.com/gcla/gowid
| urwid          | Python   | no          | no           | no     | https://github.com/urwid/urwid
| JediTerm       | Java     | yes         | no           | no     | https://github.com/JetBrains/jediterm

1 - Also fails to filter out sixel data, leaving garbage on screen.

2 - Using jerch's xterm-addon-image: https://github.com/jerch/xterm-addon-image
