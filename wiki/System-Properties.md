System Properties
=================

This document outlines the system properties that can be set to
customize Casciian.  The table below summarizes the properties and
default values, below which is a more detailed outline.

| Property                  | Default | Description                            |
| ------------------------- | ------- | -------------------------------------- |
| casciian.animations          | true    | Animations enabled                  |
| casciian.blinkMillis         | 500     | Blinking interval                   |
| casciian.blinkDimPercent     | 80      | Percent to dim when blinked         |
| casciian.textMouse           | true    | Show text mouse pointer             |
| casciian.hideMouseWhenTyping | false   | Hide mouse on keystroke everywhere  |
| casciian.hideMenuBar         | false   | Hide the pull-down menu             |
| casciian.hideStatusBar       | false   | Hide the status bar                 |
| casciian.menuIcons           | true    | Use emoji icons in menu             |
| casciian.cursorBlink         | on      | Allow blinking cursor               |
| casciian.textBlink           | on      | Allow blinking text                 |
| casciian.translucence        | true    | Translucent windows                 |
| casciian.Swing               |         | Demo: select backend                |
| casciian.Swing.cursorBlink   | on      | Swing: Allow blinking cursor        |
| casciian.Swing.textBlink     | on      | Swing: Allow blinking text          |
| casciian.Swing.fontSize      | 20      | Demo: font size of Swing window     |
| casciian.Swing.cursorStyle   | underline | Swing: cursor style               |
| casciian.Swing.mouseStyle    | none    | Swing: mouse pointer selection      |
| casciian.Swing.mouseImage    |         | Swing: image to use for mouse icon  |
| casciian.Swing.tripleBuffer  | true    | Swing: use triple-buffering         |
| casciian.Swing.imagesOverText | false  | Swing: transparent image pixels     |
| casciian.Swing.color0        | #000000 | Swing: color for black              |
| casciian.Swing.color1        | #a80000 | Swing: color for red                |
| casciian.Swing.color2        | #00a800 | Swing: color for green              |
| casciian.Swing.color3        | #a85400 | Swing: color for yellow             |
| casciian.Swing.color4        | #0000a8 | Swing: color for blue               |
| casciian.Swing.color5        | #a800a8 | Swing: color for magenta            |
| casciian.Swing.color6        | #00a8a8 | Swing: color for cyan               |
| casciian.Swing.color7        | #a8a8a8 | Swing: color for white              |
| casciian.Swing.color8        | #545454 | Swing: color for black + bold       |
| casciian.Swing.color9        | #fc5454 | Swing: color for red + bold         |
| casciian.Swing.color10       | #54fc54 | Swing: color for green + bold       |
| casciian.Swing.color11       | #fcfc54 | Swing: color for yellow + bold      |
| casciian.Swing.color12       | #5454fc | Swing: color for blue + bold        |
| casciian.Swing.color13       | #fc54fc | Swing: color for magenta + bold     |
| casciian.Swing.color14       | #54fcfc | Swing: color for cyan + bold        |
| casciian.Swing.color15       | #fcfcfc | Swing: color for white + bold       |
| casciian.TEditor.hideMouseWhenTyping | true | Hide mouse on keystroke in text editor windows |
| casciian.TEditor.margin      | 0       | Right column margin to highlight    |
| casciian.TEditor.autoWrap    | false   | Automatically wrap text to margin   |
| casciian.TImage.bleedThrough | true    | Text "bleeds through" image holes   |
| casciian.TTerminal.closeOnExit | false | Close terminal window when shell exits    |
| casciian.TTerminal.hideMouseWhenTyping | true | Hide mouse on keystroke in terminal windows |
| casciian.TTerminal.ptypipe   | auto    | Use 'ptypipe' for terminal shell    |
| casciian.TTerminal.setsid    | true    | Run 'setsid script' for terminal shell    |
| casciian.TTerminal.shell     |    | Command to use for the terminal shell    |
| casciian.TTerminal.cmdHack   | true    | For Windows, append Ctrl-J after Enter    |
| casciian.TTerminal.scrollbackMax | 2000 | Number of lines in scrollback buffer     |
| casciian.TTerminal.TERM     | xterm-direct | TERM to use for the terminal shell    |
| casciian.ECMA48.modifyOtherKeys | false  | ECMA48: detect other modifiers    |
| casciian.ECMA48.rgbColor     | false   | ECMA48: emit 24-bit RGB for system colors    |
| casciian.ECMA48.textBlink    | auto    | ECMA48: Allow/simulate blinking text|
| casciian.ECMA48.wideCharImages | true  | ECMA48: draw CJK/emoji as images    |
| casciian.ECMA48.sixel        | true    | ECMA48: draw images using sixel     |
| casciian.ECMA48.sixelEncoder | hq      | ECMA48: sixel encoder to use        |
| casciian.ECMA48.sixelPaletteSize | 1024 (legacy), 256 (hq) | ECMA48: number of colors for sixel images |
| casciian.ECMA48.sixelCustomPalette | none | ECMA48: option to use CGA or VT340 colors |
| casciian.ECMA48.sixelEmitPalette | true | ECMA48: emit sixel palette colors  |
| casciian.ECMA48.sixelSharedPalette | true | ECMA48: shared palette for sixel images (legacy only) |
| casciian.ECMA48.iTerm2Images | false   | ECMA48: draw images using iTerm2 protocol    |
| casciian.ECMA48.jexerImages  | png     | ECMA48: draw images using Jexer protocol     |
| casciian.ECMA48.imagesOverText | false | ECMA48: transparent image pixels    |
| casciian.ECMA48.imageThreadCount | 2   | ECMA48: number of threads for image rendering |
| casciian.ECMA48.explicitlyDestroyImages  | false | ECMA48: overwrite old image pixels with black |
| casciian.ECMA48.imageFallbackDisplayMode | halves | ECMA48: image to text fallback mode |
| casciian.ECMA48.color0       | #000000 | ECMA48: color for black             |
| casciian.ECMA48.color1       | #a80000 | ECMA48: color for red               |
| casciian.ECMA48.color2       | #00a800 | ECMA48: color for green             |
| casciian.ECMA48.color3       | #a85400 | ECMA48: color for yellow            |
| casciian.ECMA48.color4       | #0000a8 | ECMA48: color for blue              |
| casciian.ECMA48.color5       | #a800a8 | ECMA48: color for magenta           |
| casciian.ECMA48.color6       | #00a8a8 | ECMA48: color for cyan              |
| casciian.ECMA48.color7       | #a8a8a8 | ECMA48: color for white             |
| casciian.ECMA48.color8       | #545454 | ECMA48: color for black + bold      |
| casciian.ECMA48.color9       | #fc5454 | ECMA48: color for red + bold        |
| casciian.ECMA48.color10      | #54fc54 | ECMA48: color for green + bold      |
| casciian.ECMA48.color11      | #fcfc54 | ECMA48: color for yellow + bold     |
| casciian.ECMA48.color12      | #5454fc | ECMA48: color for blue + bold       |
| casciian.ECMA48.color13      | #fc54fc | ECMA48: color for magenta + bold    |
| casciian.ECMA48.color14      | #54fcfc | ECMA48: color for cyan + bold       |
| casciian.ECMA48.color15      | #fcfcfc | ECMA48: color for white + bold      |
| casciian.cjkFont.filename    | NotoSansMonoCJKtc-Regular.otf | Font for CJK characters |
| casciian.emojiFont.filename  | OpenSansEmoji.ttf | Font for emojis           |
| casciian.fallbackFont.filename |       | Font to use when no other available font has a codepoint |
| casciian.TButton.style       | square  | Button style                        |
| casciian.TWindow.borderStyleForeground | double | Window border style        |
| casciian.TWindow.borderStyleModal      | double | Window border style        |
| casciian.TWindow.borderStyleMoving     | single | Window border style        |
| casciian.TWindow.borderStyleInactive   | single | Window border style        |
| casciian.TMenu.borderStyle             | single | Menu border style          |
| casciian.TEditColorTheme.borderStyle   | double | Color theme window border style     |
| casciian.TEditColorTheme.options.borderStyle   | single | Interior boxes border style |
| casciian.TPanel.borderStyle  | none    | TPanel border style                 |
| casciian.TRadioGroup.borderStyle       | singleVdoubleH | Radio group border style    |
| casciian.TScreenOptions.borderStyle    | single | Screen options window border style  |
| casciian.TScreenOptions.grid.borderStyle    | single | Border style around the grid   |
| casciian.TScreenOptions.options.borderStyle | single | Interior boxes border style    |
| casciian.TWindow.opacity     | 75      | Default window opacity (10 - 100)   |
| casciian.TTerminal.opacity   | 85      | Terminal window opacity (10 - 100)  |
| casciian.TMenu.opacity       | 85      | Menu window opacity (10 - 100)      |
| casciian.effect.windowOpen   | none    | Animation for opening new windows   |
| casciian.effect.windowClose  | none    | Animation for closing windows       |

casciian.animations
-------------------

Used by casciian.TApplication.  If true, allow high-speed animations in
Casciian widgets that support it (currently TButton, TCheckBox, TField,
and TRadioButton), and window open/close effects.  Note that most
animations require 24-bit RGB ("truecolor") support from the terminal.
Default: true.

casciian.blinkMillis
--------------------

Used by casciian.backend.ECMA48Terminal and casciian.backend.SwingTerminal.
The number of millis to wait before switching the blink from visible
to invisible.  If 0 or negative, blinking is disabled.  Default: 500.

casciian.blinkDimPercent
------------------------

Used by casciian.backend.ECMA48Terminal and casciian.backend.SwingTerminal.
The percent of "dimming" to do when blinking text is invisible.  0
means no blinking; 100 means the foreground color matches the
background color (like CGA/EGA/VGA hardware).  Values less than 100
requires 24-bit RGB ("truecolor") support from the terminal to look
correct.  Default: 80.

casciian.textMouse
------------------

Used by casciian.TApplication and casciian.backend.SwingTerminal.  If true,
display a text-based mouse pointer, otherwise do not.  (Also, the
Swing backend will display the "hand" mouse cursor when textMouse is
false.)  Default: true.

casciian.hideMouseWhenTyping
----------------------------

Used by casciian.TApplication.  If true, suppress the text-based mouse
pointer after a user presses a key.  Mouse motion will restore the
pointer.  Default: false.

casciian.hideMenuBar
--------------------

Used by casciian.TApplication.  If true, do not display the pull-down
menu on the top row.  Menu keyboard accelerators will still work.
Default: false.

casciian.hideStatusBar
----------------------

Used by casciian.TApplication.  If true, do not display the status bar on
the bottom row.  Status bar keyboard accelerators will still work.
Default: false.

casciian.menuIcons
------------------

Used by casciian.TApplication.  If true, support emoji icons next to
drop-down menu items.  Default: true.

casciian.cursorBlink
--------------------

Used by casciian.backend.SwingTerminal.  If on, a blinking cursor style
may blink.  If off, the cursor will never blink.  Default: on.

casciian.textBlink
------------------

Used by casciian.backend.ECMA48Terminal and casciian.backend.SwingTerminal.
If on, blinking text may blink.  If off, blinking text will always be
visible without actually blinking.  Default: on.

casciian.translucence
---------------------

Used by casciian.TApplication.  If true, alpha-blend windows during
drawing to achieve a glass-like translucence effect.  Note that this
requires 24-bit RGB ("truecolor") support from the terminal.  Default:
true.

casciian.Swing
--------------

Used by casciian.demos.Demo1 and casciian.demos.Demo4.  If true, use the
Swing interface for the demo application.  Default: true on Windows
(os.name starts with "Windows") and Mac (os.name starts with "Mac"),
false on non-Windows and non-Mac platforms.

casciian.Swing.cursorBlink
--------------------------

Used by casciian.backend.SwingTerminal.  If on, a blinking cursor style
will actually blink.  If off, the cursor will never blink.
(Overridden by casciian.cursorBlink.)  Default: on.

casciian.Swing.textBlink
------------------------

Used by casciian.backend.SwingTerminal.  If on, blinking text will
actually blink.  If off, blinking text will always be visible without
actually blinking.  (Overridden by casciian.textBlink.)  Default: on.

casciian.Swing.fontSize
-----------------------

Used by casciian.demos.Demo1.  The font size to use with the Swing
interface for the demo application.  Default: 20.

casciian.Swing.cursorStyle
--------------------------

Used by casciian.backend.SwingTerminal.  Selects the cursor style to
draw.  Valid values are: underline, block, outline, and verticalBar.
Default: underline.

casciian.Swing.mouseStyle
-------------------------

Used by casciian.backend.SwingTerminal.  Selects the mouse pointer
(java.awt.Cursor) style to use.  Valid values are: none, default,
hand, crosshair, move, and text.  Default: none.

casciian.Swing.mouseImage
-------------------------

Used by casciian.backend.SwingTerminal.  Filename containing an image to
use as the mouse pointer.  Filename must be on the classpath.  The hot
spot will be in the middle of the image.

casciian.Swing.tripleBuffer
---------------------------

Used by casciian.backend.SwingTerminal.  If true, use triple-buffering
which reduces screen tearing but may also be slower to draw on slower
systems.  If false, use naive Swing thread drawing, which may be
faster on slower systems but also more likely to have screen tearing.
Default: true.

casciian.Swing.imagesOverText
-----------------------------

Used by casciian.backend.SwingTerminal.  If true, render text glyphs
underneath images, allowing the using of text and images to both show
in one cell.  This is _very_ cool looking, but currently hideously
expensive when covering large parts of the screen or doing animations
with partially-transparent images.  (And I have no shame in admitting
that the notcurses project was the inspiration for me to finally try
this out.  If you want to mix a lot of text-and-images, check out
notcurses, it's really wicked.)  Default: false.

casciian.TEditor.hideMouseWhenTyping
------------------------------------

Used by casciian.TEditorWindow.  If true, suppress the text-based mouse
pointer after a user presses a key within a text editor window.  Mouse
motion will restore the pointer.  Default: true.

casciian.TEditor.margin
-----------------------

Used by casciian.TEditorWindow.  If a positive integer, highlight the
column in the text editor window.  Default: 0.

casciian.TEditor.autoWrap
-------------------------

Used by casciian.TEditorWindow.  If true, automatically wrap the text to
fit inside the margin.  Default: false.

casciian.TImage.bleedThrough
----------------------------

Used by casciian.TImage.  If true, then image cells that are fully
covered by a single color will be replaced by empty text cells of that
color.  This will permit text "behind" that cell to bleed through if
translucence is enabled and opacity for the window the image is on is
less than 100.  Default: true.

casciian.TTerminal.closeOnExit
------------------------------

Used by casciian.TTerminalWindow.  If true, close the window when the
spawned shell exits.  Default: false.

casciian.TTerminal.hideMouseWhenTyping
--------------------------------------

Used by casciian.TTerminalWindow.  If true, suppress the text-based mouse
pointer after a user presses a key within a terminal window.  Mouse
motion will restore the pointer.  Default: true.

casciian.TTerminal.ptypipe
--------------------------

Used by casciian.TTerminalWindow.  If 'true', or if 'auto' and 'ptypipe'
is on the PATH, then spawn shell using the 'ptypipe' utility rather
than 'script'.  This permits terminals to resize with the window.
ptypipe is a separate C language utility, available at
https://gitlab.com/AutumnMeowMeow/ptypipe.  Default: auto.

When casciian.TTerminal.ptypipe is true, and casciian.TTerminal.shell is not
set, then the command used for the terminal shell is
`ptypipe /bin/bash --login`

casciian.TTerminal.setsid
-------------------------

Used by casciian.TTerminalWindow.  If true, and os.name does not start
with "Windows" or "Mac", spawn shell using the 'setsid' utility to run
'script'.  This runs 'script' in a new process group, permitting
closing the terminal window without crashing the JVM.  Default: true.

When casciian.TTerminal.setsid is true, and casciian.TTerminal.shell is not
set, and os.name does not start with "Windows" or "Mac", then the
command used for the terminal shell is `setsid script -fqe /dev/null`

casciian.TTerminal.shell
------------------------

Used by casciian.TTerminalWindow.  If set, use this value to spawn the
shell.  If not set, the following commands are used based on the value
of the os.name system property:

| os.name starts with | Command used for terminal shell |
| ------------------- | ------------------------------- |
| Windows             | cmd.exe                         |
| Mac                 | script -q -F /dev/null          |
| Anything else       | script -fqe /dev/null           |

casciian.TTerminal.cmdHack
--------------------------

Used by casciian.TTerminalWindow.  If true, append a line feed (Ctrl-J,
hex 0x0a) after every enter/return keystroke (carriage return, Ctrl-M,
hex 0x0d).  This is needed for cmd.exe, but might not be for other
shells.  Default: true.

casciian.TTerminal.scrollbackMax
--------------------------------

Used by casciian.TTerminalWindow.  The number of lines in the scrollback
(offscreen) buffer.  If 0, scrollback is unlimited.  Default: 2000.

casciian.TTerminal.TERM
-----------------------

Used by casciian.TTerminalWindow.  The value to use for the TERM
environment variable.  Default: xterm-direct.

casciian.ECMA48.modifyOtherKeys
-------------------------------

Used by casciian.backend.ECMA48Terminal.  If true, enable the
"modifyOtherKeys" feature of Xterm to detect things like Ctrl-Enter,
Ctrl-Tab, Ctrl-Shift-A, and so on.  Default: false.

casciian.ECMA48.rgbColor
------------------------

Used by casciian.backend.ECMA48Terminal.  If true, emit T.416-style RGB
colors for normal system colors.  This is expensive in bandwidth, and
potentially terrible looking for non-xterms.  Default: false.

casciian.ECMA48.textBlink
-------------------------

Used by casciian.backend.ECMA48Terminal.  Controls whether text specified
with the blinking attribute will never actually blink, be simulated
without sending the blink attribute to the host terminal, or allow the
blink attribute to be sent to the host terminal.  (Overridden by
casciian.textBlink.)

Value can be one of the following:

| Value | Description                                                        |
| ----- | ------------------------------------------------------------------ |
| off   | Do not blink text at all                                           |
| soft  | Simulate blinking without sending blink attribute to host terminal |
| hard  | Send blink attribute to host terminal                              |
| auto  | Choose soft or hard based on terminal identification               |

Default: auto.

casciian.ECMA48.wideCharImages
------------------------------

Used by casciian.backend.ECMA48Terminal.  If true, draw wide characters
(fullwidth characters) as used by CJK and emoji as images.  This looks
better on terminals loaded without a CJK font, but requires sixel
support.  Default: true.

casciian.ECMA48.sixel
---------------------

Used by casciian.backend.ECMA48Terminal.  If true, emit image data using
sixel, otherwise show blank cells where images could be.  This is
expensive in bandwidth, very expensive in CPU (especially for large
images), and will leave artifacts on the screen if the terminal does
not support sixel.  Default: true.

casciian.ECMA48.sixelEncoder
----------------------------

Used by casciian.backend.ECMA48Terminal.  Two sixel encoders are
available: legacy and hq.

The legacy encoder is a uniform color quantizer with
nearly-equal-sized segments in HSL colorspace.  All images are mapped
to a single palette.  Bandwidth can be saved if the terminal supports
DECRST 1070 -- see casciian.ECMA48.sixelSharedPalette.  This encoder is
the original, it looks more retro, but might also work better for some
terminals.  Also, this encoder is not thread-safe, so specifying it
will disable multithreaded image rendering (see
casciian.ECMA48.imageThreadCount).

The hq encoder generates custom palettes via several methods, with the
most advanced being a median cut.  It is much faster, has higher
picture quality, but requires the terminal support individual
registers per image (DECSET 1070).  It looks a bit like VHS tape
quality, with minor grain artifacts, and depending on the picture and
palette size might show horizontal banding.

Default: hq.

casciian.ECMA48.sixelPaletteSize
--------------------------------

Used by casciian.backend.ECMA48Terminal.  Number of colors to use for
sixel output.

For the legacy encoder, values are: 2 (black-and-white), 256, 512,
1024, or 2048; default is 1024.

For the hq encoder, values are: 2 (black-and-white), 4, 8, 16, 32, 64,
128, 256, 512, 1024, or 2048; default is 256.

casciian.ECMA48.sixelCustomPalette
----------------------------------

Used by casciian.backend.ECMA48Terminal, only for the hq sixel encoder.
Default: none.

Value can be one of the following:

| Value | Description                                             |
| ----- | ------------------------------------------------------- |
| none  | Generate custom high-quality colors for every image     |
| cga   | Use only 16 colors, set to match CGA / DOS              |
| vt340 | Use only 16 colors, set to match the DEC VT340 terminal |

casciian.ECMA48.sixelEmitPalette
--------------------------------

Used by casciian.backend.ECMA48Terminal, only for the hq sixel encoder.
If true, do not emit sixel palette color definitions.  Setting this to
false is intended to support hardware terminals, and will likely only
look good when casciian.ECMA48.sixelCustomPalette=vt340.  Default: true.

casciian.ECMA48.sixelSharedPalette (legacy encoder only)
--------------------------------------------------------

Used by casciian.backend.ECMA48Terminal, only for the legacy sixel
encoder.  If true, use a single shared palette for sixel images,
emitting the palette once on the first image; this feature requires
terminal support for DECRST 1070 (the ability to disable "use private
color registers for each graphic" flag).  If false, emit a palette
with the used colors on every sixel image.  Default: true.

casciian.ECMA48.iTerm2Images
----------------------------

Used by casciian.backend.ECMA48Terminal.  If true, emit image data using
iTerm2 image protocol, otherwise show blank cells where images could
be.  This is expensive in bandwidth and will leave artifacts on the
screen if the terminal does not support iTerm2 images.  If both
casciian.ECMA48.sixel and casciian.ECMA48.iTerm2Images are true, images will
be displayed in iTerm2 style only.  Default: false.

casciian.ECMA48.jexerImages
---------------------------

Used by casciian.backend.ECMA48Terminal.  If not false, and the
terminal reports support for Casciian images, then emit image data
using the Jexer image protocol, otherwise fallback to sixel or iTerm2
images if either of those is enabled.

Value can be one of the following:

| Value | Description                        |
| ----- | ---------------------------------- |
| false | Do not use Jexer image protocol    |
| jpg   | Use Jexer protocol with JPG images |
| png   | Use Jexer protocol with PNG images |
| rgb   | Use Jexer protocol with RGB images |

Default: png.

casciian.ECMA48.imagesOverText
------------------------------

Used by casciian.backend.ECMATerminal.  If true, render text glyphs
underneath images, allowing the using of text and images to both show
in one cell.  Casciian cannot know the terminal's font, so glyphs
rendered under images may look odd, even if still usable.  If false,
then the text background color (or what Casciian thinks is the background
color) is emitted underneath the image, which is much faster and looks
better.  Default: false.

casciian.ECMA48.imageThreadCount
---------------------------------

Used by casciian.backend.ECMATerminal.  The number of threads used to
generate images per frame.  Note that the legacy sixel encoder is not
thread-safe, so if casciian.ECMA48.sixelEncoder is 'legacy' then
multi-threaded image rendering will be disabled. Default: 2.

casciian.ECMA48.explicitlyDestroyImages
---------------------------------------

Used by casciian.backend.ECMATerminal.  If true, this terminal requires
explicitly overwriting images with black pixels to destroy them.  If
false, overwriting images with text will destroy them.  Konsole is the
only terminal known at this time that requires explicitlyDestroyImages
to be true.  Default: false.

casciian.ECMA48.imageFallbackDisplayMode
----------------------------------------

Used by casciian.backend.ECMA48Terminal.  How to convert bitmap images to
text on terminals that do not report support for sixel or iTerm2
protocol.  Default: halves.

Value can be one of the following:

| Value     | Description                            |
| --------- | -------------------------------------- |
| solid     | Space (' '), averaged color            |
| halves    | Unicode half block glyphs              |
| sextants  | Unicode sextant block glyphs           |
| quadrants | Unicode quadrant block glyphs          |
| 6dot      | Unicode 6-dot Braille glyphs, average color on black background |
| 6dotsolid | Unicode 6-dot Braille glyphs, foreground and background colors  |

casciian.cjkFont.filename
-------------------------

Used by casciian.backend.GlyphMaker.  Filename containing the font to use
for CJK.  Filename must be on the classpath.  Default:
NotoSansMonoCJKtc-Regular.otf

Note that if the font is not found, NO ERROR IS REPORTED.

casciian.emojiFont.filename
---------------------------

Used by casciian.backend.GlyphMaker.  Filename containing the font to use
for emojis.  Filename must be on the classpath.  Default:
OpenSansEmoji.ttf

Note that if the font is not found, NO ERROR IS REPORTED.

casciian.fallbackFont.filename
------------------------------

Used by casciian.backend.GlyphMaker.  Filename containing the font to use
as a last resort when no other font has the codepoint.  Filename must
be on the classpath.  Default: ""

Note that if the font is not found, NO ERROR IS REPORTED.

casciian.TButton.style
----------------------

Used by casciian.TButton.  The style to draw the button ends and shadow.
Default: square.

Value can be one of the following:

| Value      | Description                       |
| ---------- | --------------------------------- |
| square     | Square edges                      |
| round      | Semicircle edges using images     |
| diamond    | Diamond-angled edges using images |
| arrowleft  | Arrow pointing left using images  |
| arrowright | Arrow pointing right using images |
| leftarrow  | Synonym for arrowleft             |
| rightarrow | Synonym for arrowright            |


casciian.TWindow.borderStyleForeground
--------------------------------------

Used by casciian.TWindow.  The glyphs to be used for the window border
and corners when it is the foreground window, not modal or being
moved/resized.  Default: double.

Value can be one of the following:

| Value   | Description                            |
| ------- | -------------------------------------- |
| default | Default for widget                     |
| none    | No border                              |
| single  | Single-line border with sharp corners  |
| double  | Double-line border with sharp corners  |
| singleVdoubleH | Single-line border on the vertical sections, double-line on the horizontal sections |
| singleHdoubleV | Double-line border on the vertical sections, single-line on the horizontal sections |
| round   | Single-line border with rounded corners |

casciian.TWindow.borderStyleModal
---------------------------------

Used by casciian.TWindow.  The glyphs to be used for the window border
and corners when it is a foreground model window.  See
casciian.TWindow.borderStyleForeground for the available values.
Default: double.

casciian.TWindow.borderStyleMoving
----------------------------------

Used by casciian.TWindow.  The glyphs to be used for the window border
and corners when it is being moved/resized.  See
casciian.TWindow.borderStyleForeground for the available values.
Default: single.

casciian.TWindow.borderStyleInactive
------------------------------------

Used by casciian.TWindow.  The glyphs to be used for the window border
and corners when it is not the foreground window.  See
casciian.TWindow.borderStyleForeground for the available values.
Default: single.

casciian.TMenu.borderStyle
--------------------------

Used by casciian.TMenu.  The glyphs to be used for a menu window border
and corners.  See casciian.TWindow.borderStyleForeground for the
available values.  Default: single.

casciian.TEditColorTheme.borderStyle
------------------------------------

Used by casciian.TEditColorThemeWindow.  The glyphs to be used for the
color theme editor window border and corners.  See
casciian.TWindow.borderStyleForeground for the available values.
Default: double.

casciian.TEditColorTheme.options.borderStyle
--------------------------------------------

Used by casciian.TEditColorThemeWindow.  The glyphs to be used for the
color theme editor interior boxes border and corners.  See
casciian.TWindow.borderStyleForeground for the available values.
Default: single.

casciian.TPanel.borderStyle
---------------------------

Used by casciian.TPanel.  The glyphs to be used for a drawn border and
corners.  See casciian.TWindow.borderStyleForeground for the available
values.  Default: none.

casciian.TRadioGroup.borderStyle
--------------------------------

Used by casciian.TRadioGroup.  The glyphs to be used for the box border
and corners around the radio button options.  See
casciian.TWindow.borderStyleForeground for the available values.
Default: singleVdoubleH.

casciian.TScreenOptions.borderStyle
-----------------------------------

Used by casciian.TScreenOptionsWindow.  The glyphs to be used for the
screen options window border and corners.  See
casciian.TWindow.borderStyleForeground for the available values.
Default: single.

casciian.TScreenOptions.grid.borderStyle
----------------------------------------

Used by casciian.TScreenOptionsWindow.  The glyphs to be used for the
sample window grid border and corners.  See
casciian.TWindow.borderStyleForeground for the available values.
Default: single.

casciian.TScreenOptions.options.borderStyle
-------------------------------------------

Used by casciian.TScreenOptionsWindow.  The glyphs to be used for border
and corners drawn on the interior boxes.  See
casciian.TWindow.borderStyleForeground for the available values.
Default: single.

casciian.translucence
---------------------

Used by casciian.TApplication.  If true, alpha-blend windows during
drawing to achieve a glass-like translucence effect.  Note that this
requires 24-bit RGB ("truecolor") support from the terminal.  Default:
true.

casciian.TWindow.opacity
------------------------

Used by casciian.TWindow.  The opacity to set for new windows, in
general.  A value of 0 will make the window entirely invisible; a
value of 100 will be fully opaque (no alpha-blending the area
underneath it).  Default: 75.

casciian.TTerminal.opacity
--------------------------

Used by casciian.TTerminalWindow.  The opacity to set for new terminal
windows.  A value of 0 will make the window entirely invisible; a
value of 100 will be fully opaque (no alpha-blending the area
underneath it).  Default: 85.

casciian.TMenu.opacity
----------------------

Used by casciian.TMenu.  The opacity to set for menus.  A value of 0 will
make the menu entirely invisible; a value of 100 will be fully opaque
(no alpha-blending the area underneath it).  Default: 85.


casciian.effect.windowOpen
--------------------------

Used by TApplication.  An optional animation shown when opening new
windows.  Default: none.

Value can be one of the following:

| Value     | Description                                          |
| --------- | ---------------------------------------------------- |
| none      | No animation                                         |
| fade      | Fade the window by changing its opacity              |
| burn      | Cover the window area with a plasma fire-type effect |
| wipeUp    | Sweep the window contents from bottom-to-top         |
| wipeDown  | Sweep the window contents from top-to-bottom         |
| wipeLeft  | Sweep the window contents from right-to-left         |
| wipeRight | Sweep the window contents from left-to-right         |

casciian.effect.windowClose
---------------------------

Used by TApplication.  An optional animation shown when a window is
closed.  See casciian.effect.windowOpen for the available values.
Default: none.
