Casciian - Java Text User Interface library
===========================================

This library implements a text-based windowing system loosely
reminiscent of Borland's [Turbo
Vision](http://en.wikipedia.org/wiki/Turbo_Vision) system.  It looks
like this:

![WezTerm, translucent images](/screenshots/wezterm_translucent_images.png?raw=true "WezTerm, translucent images")

...or this:

![Terminal, Image, Table](/screenshots/new_demo1.png?raw=true "Terminal, Image, Table")

...or anything in between.

Casciian works on both Xterm-like terminals and Swing, and supports
images in both Xterm and Swing.  On Swing, images are true color; on
Xterm, images are rendered as sixel, iTerm2, or Casciian images, or as
Unicode half-block glyphs if none of those are available.  Casciian
can be run inside its own terminal window, with support for all of its
features including images and mouse, and of course more terminals.

Casciian has seen inspiration from several other projects:

* Translucent windows were inspired by
  [notcurses](https://github.com/dankamongmen/notcurses).  Translucent
  windows and layered images generally look as one would expect in a
  modern graphical environment...but it's mostly text.

* Casciian's (multithread-safe) "high quality" sixel encoder
  (HQSixelEncoder) -- which supplants its original (single-threaded)
  2018-era design (LegacySixelEncoder) -- was inspired by
  [chafa's](https://hpjansson.org/chafa/) high-performance principal
  component analysis based sixel encoder.  HQSixelEncoder combined
  with Casciian's cell-based images design approaches 20-bit color
  depth!

* Pulsing button text, window effects, and desktop effects were
  inspired by [vtm's](https://github.com/netxs-group/vtm) incredibly
  slick game-like aesthetic.

* Notcurses, chafa, and
  [sixel-tmux](https://github.com/csdvrx/sixel-tmux) were the
  inspiration for adding image rendering to Unicode half-block glyphs.



Screenshots
-----------

![Gradients in translucent windows with images](/screenshots/gradient1.png?raw=true "Gradients in translucent windows with images")

![Color emoji menu icons](/screenshots/emoji1.png?raw=true "Color emoji menu icons")

![Casciian with Spanish-language translations, CJK text in terminal, the libsixel snake image, and an ANSI art screen](/screenshots/casciian_espanol.png?raw=true "Casciian with Spanish-language translations, CJK text in terminal, the libsixel snake image, and an ANSI art screen")



Copyright Status
----------------

To the extent possible under law, the author(s) of Casciian have
dedicated all copyright and related and neighboring rights to Casciian
to the public domain worldwide. This software is distributed without
any warranty.  The COPYING file describes this intent, and provides a
public license fallback for those jurisdictions that do not recognize
the public domain.



Running The Demo
----------------

src/demo contains official demos showing all of the stock UI controls.
The demos can be run as follows:

  * 'java -cp casciian.jar:demo.jar demo.Demo1' .  This will use
    System.in/out with Xterm-like sequences on non-Windows non-Mac
    platforms.  On Windows and Mac it will use a Swing JFrame.

  * 'java -Dcasciian.Swing=true -cp casciian.jar:demo.jar demo.Demo1' .
    This will always use Swing on any platform.

  * 'java -cp casciian.jar:demo.jar demo.Demo2 PORT' (where PORT is a
    number to run the TCP daemon on).  This will use the Xterm backend
    on a telnet server that will update with screen size changes.

  * 'java -cp casciian.jar:demo.jar demo.Demo3' .  This will use
    System.in/out with Xterm-like sequences.  One can see in the code
    how to pass a different InputReader and OutputReader to
    TApplication, permitting a different encoding than UTF-8; in this
    case, code page 437.

  * 'java -cp casciian.jar:demo.jar demo.Demo4' .  This demonstrates
    hidden windows and a custom TDesktop.

  * 'java -cp casciian.jar:demo.jar demo.Demo5' .  This demonstrates two
    demo applications using different fonts in the same Swing frame.

  * 'java -cp casciian.jar:demo.jar demo.Demo6' .  This demonstrates two
    applications performing I/O across three screens: an Xterm screen
    and Swing screen, monitored from a third Swing screen.

  * 'java -cp casciian.jar:demo.jar demo.Demo7' .  This demonstrates the
    BoxLayoutManager, achieving a similar result as the
    javax.swing.BoxLayout apidocs example.

  * 'java -cp casciian.jar:demo.jar demo.Demo8 PORT' (where PORT is a
    number to run the TCP daemon on).  This will use the Xterm backend
    on a telnet server to share one screen to many terminals.



Acknowledgements
----------------

Casciian makes use of the Terminus TrueType font [made available
here](http://files.ax86.net/terminus-ttf/) .

💖
