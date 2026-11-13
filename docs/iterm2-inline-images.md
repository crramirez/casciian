# iTerm2 inline images protocol in Casciian

Status: Image cells are drawn with the
[iTerm2 inline images protocol](https://iterm2.com/documentation-images.html)
when the terminal is iTerm2. Pixels are packed into an image file by a
pluggable `ImageEncoder`. The core library ships one encoder,
`PngImageEncoder`, a pure-Java PNG writer that uses only `java.base`.

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
  writes 8-bit truecolor (color type 2).
- `rgba`: alpha is kept if the encoder supports it. The PNG encoder writes
  8-bit truecolor with alpha (color type 6). The color of fully transparent
  pixels is kept. Before each image, Casciian resets SGR and sets the
  background to the first cell's background color, so transparent pixels show
  that color.

## Built-in PNG encoder

`PngImageEncoder` (name `png`, priority 10) is the default and only built-in
encoder. It uses only `java.base` (`Deflater`, `CRC32`). The default is
**zlib level 1 (`Deflater.BEST_SPEED`) with the `ADAPTIVE` filter
strategy**. Both were chosen from measurements (see below). Interactive
rendering latency took priority over the smallest file.

### Filter strategies

`new PngImageEncoder(level, PngImageEncoder.FilterStrategy)` accepts zlib
levels 0-9 and one of these strategies:

| Strategy       | Behavior                                                                 |
|----------------|--------------------------------------------------------------------------|
| `NONE`, `SUB`, `UP`, `AVERAGE`, `PAETH` | the same PNG filter on every row               |
| `ADAPTIVE`     | default: per row, picks Up or Sub, whichever has the smaller sum of absolute differences (MSAD); a row that Up filters to all zeros stops the search early |
| `ADAPTIVE_ALL` | the same MSAD heuristic over all five filters, with early abandonment    |

The `ADAPTIVE` heuristic is cheap: two passes over a row, using only
additions. Up wins on vertically repeated content (text, borders, flat
fills) and Sub wins on horizontal runs. `ADAPTIVE_ALL` gives files only about
1-2% smaller than `ADAPTIVE` on text, but costs 1.7-2.3x as much time, so it is
not the default.

### Memory

The encoder never builds a raw or filtered copy of the whole image. Each
scanline is extracted from the ARGB `int[]`, filtered into one of two reusable
row buffers, and passed directly to the `Deflater`. Compressed bytes are
written into a short list of blocks. The first block is sized from the raw
image size, and later blocks double up to 64 KiB. The PNG signature, `IHDR`,
`IDAT`, CRCs, and `IEND` are then written once into an exactly sized
`byte[]`. Allocations per encode dropped by 4-12x on runs and large images
compared with the previous encoder. `ITerm2ImageEncoder` base64-encodes straight into the final escape
sequence buffer, without an intermediate base64 `String`.

### Measurements

`./gradlew pngBenchmark` (in `code/`) runs
`casciian.image.encoders.PngEncoderBenchmark`. It covers levels 1, 3, and 6,
all filter strategies, and 12 deterministic synthetic images: flat fills, text
and window borders, an RGBA icon, gradients, photo-like content, and noise.
Each image is tested as a single 8x16 cell, as a 1280x16 horizontal run, and
as a 640x400 image. Each configuration gets 300 ms of warm-up and at least
700 ms or 30 repetitions of measurement. The benchmark reports bytes, base64
bytes, average, median, and p95 time, throughput, and allocated KiB per encode
(through `com.sun.management.ThreadMXBean`). Options:
`--levels=1,3,6`, `--filters=PAETH,ADAPTIVE`, `--images=text-run,photo-large`,
`--warmup-ms=`, `--measure-ms=`, `--min-reps=`, and `--encoder-class=<fqcn>` to
measure any other `ImageEncoder`. For example:

```
./gradlew pngBenchmark --args="--levels=1 --filters=ADAPTIVE,PAETH"
```

These results come from one run on OpenJDK 27, Linux amd64, with 2 shared
vCPUs. Absolute timings are noisy, but the relative ordering was stable
across runs. The "photo" images are synthetic (smooth fields with grain), not
real photographs. Columns show median time in µs / PNG bytes.

| Image (pixels)          | previous: L1 Paeth | L1 PAETH      | **L1 ADAPTIVE** | L1 NONE        | L3 ADAPTIVE   | L6 ADAPTIVE   |
|-------------------------|--------------------|---------------|-----------------|----------------|---------------|---------------|
| flat-run 1280x16        | 177 / 388          | 222 / 388     | **93 / 388**    | 55 / 378       | 93 / 388      | 216 / 183     |
| text-run 1280x16        | 213 / 3385         | 218 / 3385    | **136 / 3158**  | 87 / 3541      | 157 / 2722    | 526 / 2128    |
| text-window 640x400     | 3890 / 38850       | 4068 / 38850  | **2792 / 36203**| 2255 / 39883   | 3344 / 31789  | 8056 / 23821  |
| icon-alpha 64x64 RGBA   | 64 / 1149          | 73 / 1149     | **51 / 1215**   | 138 / 5509     | 55 / 1170     | 153 / 939     |
| gradient-run 1280x16    | 188 / 1169         | 193 / 1169    | **127 / 2182**  | 318 / 20489    | 127 / 1469    | 311 / 985     |
| gradient-large 640x400  | 3106 / 14783       | 3181 / 14783  | **2686 / 30470**| 7340 / 311175  | 2746 / 18782  | 5421 / 13382  |
| photo-run 1280x16       | 1285 / 40820       | 1336 / 40820  | **895 / 40722** | 1186 / 59904   | 1012 / 40690  | 1595 / 39430  |
| photo-large 640x400     | 17258 / 507318     | 17949 / 507318| **12190 / 505899** | 18164 / 747178 | 14336 / 504737 | 23082 / 492366 |
| noise-large 640x400     | 21059 / 768698     | 21616 / 768698| **16397 / 768698** | 15456 / 768698 | 16091 / 768698 | 16631 / 768698 |

Allocated KiB per encode, previous encoder → new encoder (L1 ADAPTIVE):
text-run 158 → 27, text-window 1122 → 93, gradient-large 1052 → 87,
photo-large 3621 → 999, noise-large 5887 → 1512. Single 8x16 cells encode in
about 5-10 µs either way, and their allocations dropped from 2-3 KiB to under
1 KiB.

How the defaults were chosen:

- **Level 1.** Level 3 makes text and UI images 10-15% smaller but takes
  10-30% longer. Level 6 makes them 25-35% smaller but takes 2-4x longer.
  Photo-like and noisy content shrinks by less than 3% at any level. On a
  local terminal, encoding time dominates, so level 1 wins. Over slow remote
  links, a higher level can be worth it (see below).
- **ADAPTIVE over fixed Paeth.** Paeth is the most expensive filter. Adaptive
  Up/Sub is 15-60% faster on every test image and gives similar or
  smaller output on text, windows, and photos. Its known weakness is smooth
  gradients, which come out about 2x larger than with Paeth (still far smaller
  than unfiltered). `NONE` is fastest on flat and text content, but it is 5x
  about 4.5x larger on the RGBA icon and 10x larger on gradients than
  `ADAPTIVE`, so it is unsafe as a default.

To trade CPU for bandwidth (for example over SSH), register a configured
encoder with a higher priority, or replace the default:

```java
ImageEncoderRegistry.getInstance().registerEncoder(
    new PngImageEncoder(6, PngImageEncoder.FilterStrategy.ADAPTIVE_ALL) {
        @Override public int getPriority() { return 11; }
    });
```

### Failure handling

If the configured encoder throws or returns an empty result,
`ITerm2ImageEncoder` encodes the same pixels again with the built-in
`PngImageEncoder` (using the same alpha mode), silently.
If that also fails, for example because the image cannot be read, no escape
sequence is sent. Those cells are drawn as blank cells in the normal
attribute, and the run is not cached. An invalid image is never sent, and the
render loop keeps running.

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
highest `getPriority()`. The built-in `PngImageEncoder` has priority 10, so
an add-on encoder needs a priority above 10 to take precedence. In `rgba`
mode, encoders whose `supportsAlpha()` is true are preferred. If no encoder is
registered at all, Casciian uses a default `PngImageEncoder`. If the chosen
encoder fails, see [Failure handling](#failure-handling).

### Java Desktop add-on: automatic PNG/JPEG

The `casciian-java-desktop` add-on (`code-java-desktop/`) registers two
encoders:

- `ImageIOAdaptiveEncoder` (name `adaptive`, priority 20, supports alpha)
  becomes the default. It chooses a format for each image: PNG (from the
  built-in `PngImageEncoder`) if alpha is requested and some pixel is not
  opaque, if the image has fewer than 4096 pixels, if it has at most 256
  colors, or if at least half of its pixels equal their left or upper
  neighbor. Otherwise it uses JPEG. iTerm2 detects the format from the file
  bytes.
- `ImageIOJpegEncoder` (name `jpeg`, priority 5, quality 0.85) is used by
  the adaptive encoder. You can also force it with
  `casciian.ECMA48.iterm2ImageEncoder=jpeg`.

The repeated-pixel rule came from measurements on the benchmark samples and
on real photos. On 1280x16 row runs and full images, flat UI, text
(including anti-aliased text with 500+ colors), charts, logos and gradient
strips repeat 64–100% of their pixels, and PNG is about the same size as
JPEG or up to 5x smaller. Photos repeat 0–42%, and JPEG is 5–13x smaller
and about 4x faster to encode (for example, 3.6 KB in 0.24 ms instead of
40.7 KB in 0.97 ms). A large 2-D gradient is near the threshold (56%), so it
stays lossless even though JPEG would be about 2x smaller. Color count alone
is not enough: anti-aliased text has many colors but still compresses better
as PNG.

ImageIO's own PNG writer is not used. Compared with the built-in encoder it
was 2–4x slower, and its files were up to 6x larger on gradients and alpha
icons.

## Configuration

| System property                        | Values                         | Default |
|----------------------------------------|--------------------------------|---------|
| `casciian.ECMA48.iterm2Images`         | `true`, `false`                | `true`  |
| `casciian.ECMA48.iterm2ImageFormat`    | `rgb`, `rgba`                  | `rgb`   |
| `casciian.ECMA48.iterm2ImageEncoder`   | `auto`, encoder name or MIME   | `auto`  |

`casciian.ECMA48.iterm2ImageEncoder` matches `ImageEncoder.getName()` (for
example `png`, or `jpeg` for an add-on) or `getMimeType()` (for example
`image/png`). An unknown name falls back to the `auto` selection. With
`casciian.ECMA48.iterm2Images=false`, iTerm2 falls back to the next protocol
(usually kitty graphics). At runtime, `ECMA48Terminal.setHasITerm2Images(boolean)`
turns the protocol on or off, and `hasITerm2Images()` reports whether it is in
use.
