TTerminal
=========

TTerminal is a full-featured ECMA-48/ISO 6429/ANSI X3.64 type console
window, including a scrollback buffer.  It implements nearly all of
VT100, VT102, and VT220 faithfully, and enough of XTERM to support
terminfo-based applications and Casciian itself.  TTerminal can spawn a
login shell or run any arbitrary command in a shell window.  To the
best of my knowledge, TTerminal is also the only terminal widget
available that can produce true double-width/double-height and sixel
images while running itself inside xterm.

ptypipe
-------

[ptypipe](https://gitlab.com/AutumnMeowMeow/ptypipe) executes a shell
inside a new TTY, and can notify the shell when the TTerminal is
resized.  It looks for the dtterm/xterm window size sequence, and if
seen executes ioctl(TIOCSWINSZ).

To use ptypipe, set the System property "casciian.TTerminal.ptypipe" to
"true" before calling TTerminal's constructor.

Examples
--------

```Java
new TTerminal(window, 0, 0, width, height,
    new TAction() {
        public void DO() {
            // This action will be executed when the shell process exits.
        }
    });
```

Screenshots
-----------

There are a lot of screenshots below showing off TTerminal's
capabilities.  Casciian's terminal is quite good, exceeding the
VT100/VT220 faithfulness of many popular terminals.

We start with the classic mc:

![terminal_mc_1](uploads/1246eaae875db6637ad9e9617a472ae5/terminal_mc_1.png)

Next we show a second instance of Casciian running inside a window.  The
Casciian instance in the window has full mouse support including
dragging/resizing windows.

![terminal_jexer_1](uploads/e5e8a2d7bd0b10fc6ff8d0b1fc9221bf/terminal_jexer_1.png)

The terminal expects UTF-8, and when using a font with the glyphs one
will see proper box-drawing characters:

![terminal_utf8_1](uploads/cd1e749c788730d49e94cbaa715c1803/terminal_utf8_1.png)

The terminal's color palette matches CGA/EGA/VGA, with "dark yellow"
showing as brown:

![terminal_vttest_1](uploads/46fec1da96953d53958865e51e9bd46e/terminal_vttest_1.png)

The remaining screens show off Casciian's casciian.tterminal.ECMA48
vttest compliance.  Beginning with the VT100 special graphics
characters correctly translated to Unicode:

![terminal_vttest_2](uploads/1fd7a58c323ce8893f4d7554d7f376b2/terminal_vttest_2.png)

The cursor movements test:

![terminal_vttest_3](uploads/33b776816fd67a77e19fc0e9acb0b9d7/terminal_vttest_3.png)

Bold, underline, blinking, and reverse color.  The bold + reverse
colors behave as they would on CGA/EGA/VGA (where the bold attribute
is only on the foreground color) rather than Xterm (where bold +
reverse will apply the bold to the background color):

![terminal_vttest_4](uploads/b6da46a049ab514c08d1298acb7e0f51/terminal_vttest_4.png)

The save/restore screen:

![terminal_vttest_5](uploads/ad34ae90bd782ad03f7c260c92a648f8/terminal_vttest_5.png)

VT52 sub-mode support.  Casciian is (to the best of my knowledge) the
only terminal multiplexer to support this:

![terminal_vttest_6](uploads/32d9df6af3f1d11d3ffffc5d700da690/terminal_vttest_6.png)

Double-width / double-height support:

![terminal_vttest_7](uploads/76f420ceaace4c65bd4cb1f051e57fe4/terminal_vttest_7.png)


API
---

[TTerminal API](/TTerminal.html)
