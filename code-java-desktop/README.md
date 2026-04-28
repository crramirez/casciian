Casciian Java Desktop Add-on
============================

Optional add-on for the [Casciian](../README.md) text user interface
library that integrates the JDK's `java.desktop` module.

This project lives under `code-java-desktop/` and is **independent** from
the main `code/` project. It is published separately to Maven Central, so
applications can opt in only when they actually need it.

Why a separate add-on?
----------------------

Casciian is designed to be runnable as a GraalVM native image, which means
the core library deliberately avoids any dependency on the
[`java.desktop`](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/module-summary.html)
module — that module is large, has many platform-specific behaviors, and
is awkward to use with `native-image`.

However, when an application is **not** building a native image and runs
on a regular JVM that has full Java Desktop support, it can take
advantage of the rich functionality shipped in `java.desktop` — such as
`javax.imageio.ImageIO` for image decoding. This add-on lets users opt
into those capabilities by simply adding a single dependency. If you
**don't** add this dependency, your application keeps working exactly
as before, and remains compatible with `native-image`.

What's in here?
---------------

* **`casciian-java-desktop`** — the add-on itself, packaged as a JPMS
  module `casciian.java.desktop`. It depends on `casciian` and on
  `java.desktop`. Currently it provides:
    * `casciian.javadesktop.decoders.ImageIORGBDecoder` — an
      [`ImageDecoder`](../code/src/main/java/casciian/image/decoders/ImageDecoder.java)
      backed by `javax.imageio.ImageIO`. Out of the box it decodes PNG and
      JPEG files, but the constructor accepts a custom regex / description
      so applications can register it for any other format ImageIO
      supports on their JVM (BMP, GIF, WBMP, …).
* **`demo`** — a small TUI application demonstrating the add-on. It
  registers `ImageIORGBDecoder` on `ImageDecoderRegistry` and lets the
  user pick a `.png` / `.jpg` file from `File ▸ Open` to display it in a
  `TImageWindow`. Built as a fat JAR via the `jarDemo` task.

Building
--------

The project uses Gradle (wrapper included). At build time it uses a
[Gradle composite build](https://docs.gradle.org/current/userguide/composite_builds.html)
to substitute the published `io.github.crramirez:casciian` artifact with
the sibling project under `../code`, so you don't need to publish a
SNAPSHOT first to build locally:

```sh
cd code-java-desktop
./gradlew build
```

To produce the demo fat JAR:

```sh
./gradlew :demo:jarDemo
java -jar demo/build/libs/casciian-java-desktop-demo-<version>.jar
```

The fat JAR bundles the demo, the add-on, the core casciian library and
all runtime dependencies (including JLine), so it can be run standalone.

Using the add-on in your application
------------------------------------

Once published, add it as a dependency alongside core casciian:

```gradle
dependencies {
    implementation "io.github.crramirez:casciian:<version>"
    implementation "io.github.crramirez:casciian-java-desktop:<version>"
}
```

Then, register the decoder during application startup:

```java
ImageDecoderRegistry.getInstance()
    .registerDecoder(new ImageIORGBDecoder());
```

After that, any `casciian.TImageWindow` (and any other code path going
through `ImageDecoderRegistry`) will be able to open PNG and JPEG files.

Native image users
------------------

If you compile your application with `native-image`, do **not** add
this dependency: `java.desktop` and `ImageIO` are not available (or
require non-trivial reachability metadata). Stick to the pure-Java
decoders shipped with core casciian.

License
-------

Apache License, Version 2.0. See the project [LICENSE](../LICENSE).
