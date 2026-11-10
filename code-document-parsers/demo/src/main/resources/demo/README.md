# Casciian Document Parsers

![Casciian logo](logo.png)

This document was written in **Markdown**, converted to a Casciian
`RichText` by the *pluggable* `MarkdownDocumentParser`, and displayed in a
`TTextAnsi` widget. Resize the window to watch the text ~~stay fixed~~ reflow.

## How it works

1. `DocumentParserRegistry` discovers parsers through `ServiceLoader`.
2. A parser is chosen by **file extension**, **MIME type**, or called
   directly with an `InputStream`.
3. The parser returns a width-independent `RichText` model.

> Tip: use **File > Open document...** to view any Markdown file on disk.
> Relative image paths are resolved against the document's folder.

## Inline styles

You can mix *italic*, **bold**, ***bold italic***, `inline code`,
~~strikethrough~~ and [hyperlinks](https://github.com/crramirez/casciian).
A backslash forces a hard line break:\
like this.

## Lists

- Unordered items
  - Nested items
    - Even deeper
- Task lists:
  - [x] Parse Markdown
  - [x] Display images
  - [ ] Write more parsers

3. Ordered lists keep
4. their start number

## Code

```java
RichText doc = DocumentParserRegistry.getInstance()
        .parse(Path.of("README.md"));
textAnsiWindow.getTextAnsiField().setRichText(doc);
```

## Tables

| Format   | MIME type       | Extensions        |
|:---------|:---------------:|------------------:|
| Markdown | `text/markdown` | `.md` `.markdown` |
| *Future* | `text/...`      | `...`             |

## Images

Images are decoded by the image decoders add-on:

![Sample chart](chart.png)

Relative image paths can also use a subdirectory:

![xyz](./images/xyz.png)

---

Press **F1** for help, or open *Help > About this demo*.
