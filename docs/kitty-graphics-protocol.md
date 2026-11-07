# Kitty graphics protocol in Casciian

Status: Image cells are drawn with the
[kitty graphics protocol](https://sw.kovidgoyal.net/kitty/graphics-protocol/)
when the terminal supports it. Pixels are sent as raw RGB or RGBA,
zlib-compressed by default.

Read this before changing `KittyImageEncoder`, `KittyImageManager`, or the
kitty parts of `ECMA48Terminal`.

## Detection

At startup Casciian sends a support query
(`ESC _ G i=31,s=1,v=1,a=q,t=d,f=24;AAAA ESC \`) **before** the Device
Attributes (DA) request. A terminal that supports the protocol replies
`ESC _ G i=31;OK ESC \` before it answers DA. If DA arrives first, kitty
graphics stay off.

`ESC _` is treated as the start of an APC reply only while the query is
outstanding. After DA arrives, `ESC _` is parsed as Alt-\_ again.

## Protocol priority

1. Casciian image protocol (OSC 445), if DA reports it.
2. Kitty graphics, if the query got an `OK` reply.
3. Sixel, if DA reports it.
4. Unicode half-block fallback.

## How image cells are drawn

Each image cell is sent as its own kitty image and placed over exactly one
text cell (`c=1,r=1`). Tiles are scaled to the destination cell's pixel
dimensions before transmission. Placing the image uses `C=1`, so the cursor
does not move. This means the bottom row
never scrolls, unlike sixel.

**`Cell.getImageId()` is normally used as the kitty image id.** Casciian
compares pixels before reuse and assigns a distinct kitty id if cell ids
collide. It sends each distinct image only once
(`a=T`). If the image moves, is uncovered, or repeats, Casciian sends only a
short placement command (`a=p`) that reuses the image the terminal already
has. Cell ids are positive `int`s. Kitty ids are unsigned 32-bit numbers.

Some cells need a single-use id from the upper half of the id range
(`0x80000000` and up), which cell ids never use:

- cells with no image id
- inverted images, which are used for mouse selection

Colliding persistent images also use this range, but remain cached for reuse.
Single-use images are deleted (`a=d,d=I`) as soon as they leave the screen.

Placement ids come from the cell position (`y * width + x + 1`). When an image
cell turns into text or another image, Casciian deletes only the old placement
(`a=d,d=i,i=<id>,p=<pid>`) and keeps the image data so it can be reused.

`KittyImageManager` keeps a least-recently-used list of the images it has sent.
If their total size goes over the budget (128 MB of decoded RGBA, below kitty's
320 MB limit), Casciian deletes images that are not currently on screen. This
keeps the list in sync with the terminal's memory. When the screen is cleared
or resized, the protocol is turned off, or the terminal is closed, Casciian
deletes every image it has sent and forgets all placements.

Every command uses `q=2`, so the terminal sends no replies that could mix with
keyboard input. Payloads are base64-encoded and split into chunks of at most
4096 bytes (`m=1` … `m=0`).

## Pixel formats

- `rgb` (`f=24`, default): 3 bytes per pixel. Alpha is discarded. This matches
  the Casciian image protocol, so transparent padding shows the color stored
  in the pixel.
- `rgba` (`f=32`): 4 bytes per pixel. Alpha is kept. Before placing each image,
  Casciian paints the text cell under it with the cell's background color, so
  old text does not show through transparent pixels.

zlib compression (`o=z`) is on by default.

## Configuration

| System property                          | Values                   | Default |
|------------------------------------------|--------------------------|---------|
| `casciian.ECMA48.kittyImages`            | `true`, `false`          | `true`  |
| `casciian.ECMA48.kittyImageFormat`       | `rgb`, `rgba`            | `rgb`   |
| `casciian.ECMA48.kittyImageCompression`  | `true`, `false`          | `true`  |

Setting `casciian.ECMA48.kittyImages=false` skips the query entirely. At
runtime, `ECMA48Terminal.setHasKittyImages(boolean)` turns the protocol on or
off, and `hasKittyImages()` reports whether it is in use.
