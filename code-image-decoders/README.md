Casciian Image Decoders Add-on
==============================

Optional add-on for the [Casciian](../README.md) text user interface
library that provides extra **pure-Java** image decoders on top of the
Sixel decoder shipped with the core library.

This project lives under `code-image-decoders/` and is **independent**
from the main `code/` project. It is published separately to Maven
Central, so applications can opt in only when they actually need it.

Why a separate add-on?
----------------------

Core Casciian ships a single image decoder — `SixelImageDecoder` — so
the library stays small. Rather than growing the core with every image
format, additional decoders live here as an opt-in add-on. Unlike the
[`casciian-java-desktop`](../code-java-desktop/README.md) add-on (which
delegates to `javax.imageio.ImageIO` and therefore pulls in
`java.desktop`), the decoders in this module are implemented in **pure
Java** with no `java.desktop` dependency, so they remain compatible with
a GraalVM `native-image`.

What's in here?
---------------

* **`casciian-image-decoders`** — the add-on itself, packaged as a JPMS
  module `casciian.image.decoders`. It depends only on `casciian`.
  Currently it provides:
    * `casciian.imagedecoders.decoders.BMP24ImageDecoder` — an
      [`ImageDecoder`](../code/src/main/java/casciian/image/decoders/ImageDecoder.java)
      for uncompressed 24-bit Windows Bitmap (`.bmp`) files.
    * `casciian.imagedecoders.decoders.XPMImageDecoder` — an
      `ImageDecoder` for X PixMap (`.xpm`) ASCII image files.

  More decoders (for example a PNG decoder) are planned and will be added
  here.
* **`demo`** — a small image-viewer TUI application. It relies on
  `ServiceLoader` auto-discovery so that **all** available decoders — the
  BMP and XPM decoders from this add-on plus the Sixel decoder from core
  casciian — are registered, and lets the user open a `.bmp`, `.xpm` or
  Sixel file from `Tool ▸ Open image` to display it in a `TImageWindow`.
  Built either as a standalone fat JAR (`jarDemo`) or as a JPMS app
  layout (`zipDemoJpms` / `installDemoJpms`).

Building
--------

The project uses Gradle (wrapper included). At build time it uses a
[Gradle composite build](https://docs.gradle.org/current/userguide/composite_builds.html)
to substitute the published `io.github.crramirez:casciian` artifact with
the sibling project under `../code`, so you don't need to publish a
SNAPSHOT first to build locally:

```sh
cd code-image-decoders
./gradlew build
```

To produce the demo fat JAR:

```sh
./gradlew :demo:jarDemo
java -jar demo/build/libs/casciian-image-decoders-demo-<version>.jar
```

The fat JAR bundles the demo, the add-on, the core casciian library and
all runtime dependencies (including JLine), so it can be run standalone.

To produce a JPMS demo app layout and run it as a named module:

```sh
./gradlew :demo:zipDemoJpms
unzip demo/build/distributions/casciian-image-decoders-demo-jpms-app-<version>.zip
./casciian-image-decoders-demo-jpms-app-<version>/bin/casciian-image-decoders-demo-jpms
```

Using the add-on in your application
------------------------------------

Once published, add it as a dependency alongside core casciian:

```gradle
dependencies {
    implementation "io.github.crramirez:casciian:<version>"
    implementation "io.github.crramirez:casciian-image-decoders:<version>"
}
```

Then, no further wiring is required: the BMP and XPM decoders are
registered as
[`java.util.ServiceLoader`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ServiceLoader.html)
providers (declared in the add-on's `module-info.java` and
`META-INF/services/casciian.image.decoders.ImageDecoder`). They are
automatically picked up by `TApplication`'s constructor via
`ImageDecoderRegistry.getInstance().loadDecoders()`, so any
`casciian.TImageWindow` (and any other code path going through
`ImageDecoderRegistry`) can open BMP and XPM files out of the box.

License
-------

Apache License, Version 2.0. See the project [LICENSE](../LICENSE).
