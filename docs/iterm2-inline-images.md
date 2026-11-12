# iTerm2 inline images protocol in Casciian

Status: Image cells are drawn with the
[iTerm2 inline images protocol](https://iterm2.com/documentation-images.html)
when the terminal is iTerm2. Pixels are packed into an image file by a
pluggable `ImageEncoder`; the core library ships a PNG encoder (the default)
and an uncompressed 24-bit / 32-bit BMP encoder.

Read this before changing `ITerm2ImageEncoder`, the `casciian.image.encoders`
package, or the iTerm2 parts of `ECMA48Terminal`.

## Detection

iTerm2 has no capability query for this protocol, so Casciian relies on the
XTVERSION reply sent at startup. The protocol is automatically enabled for
iTerm2 3.5 and later, which support `doNotMoveCursor=1`. Any kitty images
already placed are freed first.

## Protocol priority

On iTerm2, the iTerm2 protocol has top priority:

1. iTerm2 inline images, if XTVERSION reports iTerm2.
2. Casciian image protocol (OSC 445), if DA reports it.
3. Kitty graphics, if the query got an `OK` reply.
4. Sixel, if DA reports it.
5. Unicode half-block fallback.

## How image cells are drawn

Contiguous horizontal image cells that changed are joined into one image per
row run, like sixel and OSC 445. The tiles are joined at their original pixel
size and are not rescaled: the terminal stretches the image over the run with

```
ESC ] 1337 ; File=inline=1;size=<bytes>;width=<columns>;height=1;
            preserveAspectRatio=0;doNotMoveCursor=1 : <base64 file> BEL
```

`doNotMoveCursor=1` (iTerm2 3.5+) keeps the cursor in place, so images on the
bottom row never scroll the screen.

iTerm2 stores inline images in the cells they cover. Text written over an
image cell replaces that part of the image, so no explicit delete commands
are needed (unlike kitty). Every redraw sends the file again. To avoid
encoding it again, the escape sequence for each run is kept in an LRU cache
keyed by the cells' images. Runs with inverted images (mouse selection) are
not cached. The cache retains at most 8 MiB of encoded image commands.

## Pixel formats

- `rgb` (default): the encoder is asked for an opaque image. The PNG encoder
  writes 8-bit truecolor; the BMP encoder writes 24-bit pixels with a
  `BITMAPINFOHEADER`.
- `rgba`: alpha is kept if the encoder supports it. The PNG encoder writes
  8-bit truecolor with alpha; the BMP encoder writes 32-bit pixels with a
  `BITMAPV4HEADER` and `BI_BITFIELDS` masks, so that readers honor alpha. Before each image, Casciian resets SGR and sets the
  background to the first cell's background color, so transparent pixels show
  that color.

## Built-in encoders

`PngImageEncoder` (priority 10) is the default. It uses only `java.base`
(`Deflater`, `CRC32`), applies the Paeth filter to every row, and deflates at
the fastest level. The terminal must parse and decode every byte sent, and
uncompressed BMP costs about 4 base64 bytes per pixel: a full 120x40 screen
of 10x20 pixel cells is about 3.8 MB per frame. PNG cuts that by roughly 2x
for noisy photos and by up to about 40x for flat UI-like images, with similar
encoding time.

`BmpImageEncoder` (priority 0) is uncompressed and is the fallback when the
chosen encoder fails. Select it with `casciian.ECMA48.iterm2ImageEncoder=bmp`.

## Pluggable encoders

`casciian.image.encoders.ImageEncoder` is a `ServiceLoader` service, like
`ImageDecoder`. `ImageEncoderRegistry` discovers providers the first time it
is used; you can also register encoders with `registerEncoder(...)` before the
terminal is created.

An add-on module (for example a JPEG encoder) implements `ImageEncoder`
and declares it in `module-info.java`:

```java
provides casciian.image.encoders.ImageEncoder
    with com.example.JpegImageEncoder;
```

or in `META-INF/services/casciian.image.encoders.ImageEncoder` for the
classpath.

Unless an encoder is configured, Casciian uses the registered encoder with the
highest `getPriority()`. The built-in `PngImageEncoder` has priority 10 and
`BmpImageEncoder` has priority 0, so an add-on encoder needs a priority above
10 to take precedence. In `rgba` mode,
encoders whose `supportsAlpha()` is true are preferred. If no encoder is found,
or the chosen encoder throws, Casciian uses BMP.

## Configuration

| System property                        | Values                         | Default |
|----------------------------------------|--------------------------------|---------|
| `casciian.ECMA48.iterm2Images`         | `true`, `false`                | `true`  |
| `casciian.ECMA48.iterm2ImageFormat`    | `rgb`, `rgba`                  | `rgb`   |
| `casciian.ECMA48.iterm2ImageEncoder`   | `auto`, encoder name or MIME   | `auto`  |

`casciian.ECMA48.iterm2ImageEncoder` matches `ImageEncoder.getName()` (for
example `png` or `bmp`) or `getMimeType()` (for example `image/bmp`). With
`casciian.ECMA48.iterm2Images=false`, iTerm2 falls back to the next protocol
(usually kitty graphics). At runtime, `ECMA48Terminal.setHasITerm2Images(boolean)`
turns the protocol on or off, and `hasITerm2Images()` reports whether it is in
use.
