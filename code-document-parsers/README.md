Casciian Document Parsers Add-on
================================

Optional add-on for the [Casciian](../README.md) text user interface
library that converts documents into Casciian's `RichText` model, ready to
be shown in a `TTextAnsi` widget (for example inside a `TTextAnsiWindow`).

This project lives under `code-document-parsers/` and is **independent**
from the main `code/` project. It is published separately to Maven
Central, so applications can opt in only when they actually need it.

Pluggable parsers
-----------------

The service interface lives in core casciian, in the
`casciian.document.parsers` package, and follows the same design as the
image decoders:

* [`DocumentParser`](../code/src/main/java/casciian/document/parsers/DocumentParser.java)
  converts an `InputStream` (plus an optional MIME type and base directory
  used to resolve relative images and links) into a width-independent
  `RichText`. Each parser advertises its supported **MIME types**, a
  **file extension** pattern and a format description.
* [`DocumentParserRegistry`](../code/src/main/java/casciian/document/parsers/DocumentParserRegistry.java)
  discovers parsers through `java.util.ServiceLoader` (`TApplication`'s
  constructor calls `loadParsers()` automatically) and chooses a parser by
  file extension, by MIME type, or by URL content type.

Any library can contribute a new format by implementing `DocumentParser`
and declaring it as a provider in its `module-info.java`
(`provides casciian.document.parsers.DocumentParser with ...`) and in
`META-INF/services/casciian.document.parsers.DocumentParser`.

What's in here?
---------------

* **`casciian-document-parsers`** — the add-on itself, packaged as the JPMS
  module `casciian.document.parsers`. It currently provides:
    * `casciian.documentparsers.markdown.MarkdownDocumentParser` — a
      CommonMark parser based on
      [commonmark-java](https://github.com/commonmark/commonmark-java)
      with the GitHub-flavored tables, strikethrough and task-list
      extensions. It handles `text/markdown` / `text/x-markdown` and the
      `.md`, `.markdown`, `.mdown`, `.mkdn` and `.mkd` extensions.
      Headings, emphasis, inline code, fenced/indented code blocks, block
      quotes, nested and task lists, tables, thematic breaks, links (as
      OSC 8 hyperlinks), basic inline HTML (`<img>`, `<br>`) and
      **images** are supported. Local images are resolved against the
      document's directory and displayed through the
      `ImageDecoderRegistry`; remote images fall back to their alt text.
* **`demo`** — a small Markdown viewer TUI. It opens a bundled sample
  (with PNG images, decoded by the
  [image decoders add-on](../code-image-decoders/README.md)), lets the user
  open any document from `File ▸ Open document...`, and shows a
  `Help ▸ About this demo` page parsed from a stream by MIME type. A path
  to a document can also be passed on the command line.

Building
--------

The project uses Gradle (wrapper included) and a
[Gradle composite build](https://docs.gradle.org/current/userguide/composite_builds.html)
that substitutes `io.github.crramirez:casciian` and
`io.github.crramirez:casciian-image-decoders` with the sibling projects
under `../code` and `../code-image-decoders`:

```sh
cd code-document-parsers
./gradlew build
```

To produce and run the demo fat JAR:

```sh
./gradlew :demo:jarDemo
java -jar demo/build/libs/casciian-document-parsers-demo-<version>.jar [document.md]
```

To produce a JPMS demo app layout and run it as a named module:

```sh
./gradlew :demo:zipDemoJpms
unzip demo/build/distributions/casciian-document-parsers-demo-jpms-app-<version>.zip
./casciian-document-parsers-demo-jpms-app-<version>/bin/casciian-document-parsers-demo-jpms
```

Using the add-on in your application
------------------------------------

```gradle
dependencies {
    implementation "io.github.crramirez:casciian:<version>"
    implementation "io.github.crramirez:casciian-document-parsers:<version>"
    // Optional, to display PNG/BMP/XPM images referenced by documents:
    implementation "io.github.crramirez:casciian-image-decoders:<version>"
}
```

Then parse and display a document:

```java
DocumentParserRegistry registry = DocumentParserRegistry.getInstance();

// By file extension (relative images resolve against the file's folder)
RichText doc = registry.parse(Path.of("README.md"));

// By MIME type, from any stream
RichText help = registry.parse(stream, "text/markdown");

// Or call a parser directly
RichText direct = new MarkdownDocumentParser().parse(stream, null, baseDir);

TTextAnsiWindow window = new TTextAnsiWindow(app, "README", "");
window.getTextAnsiField().setRichText(doc);
```

License
-------

Apache License, Version 2.0. See the project [LICENSE](../LICENSE).
