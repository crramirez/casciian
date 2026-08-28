# Bracketed paste

Casciian enables xterm bracketed paste mode for ECMA-48 terminal sessions.
The terminal backend collects each paste as one `TPasteEvent`, preserving its
text without interpreting embedded terminal sequences.

`TApplication` copies that text into Casciian's internal clipboard and
dispatches the existing `cmPaste` command. Widgets therefore use their normal
paste behavior and remain independent of the terminal protocol.

The Edit menu distinguishes two paste commands:

- **Paste** (`Ctrl+V`) uses Casciian's internal clipboard.
- **System Paste** (`Ctrl+Shift+V`) asynchronously requests the terminal's
  CLIPBOARD selection with OSC 52. A valid response becomes a `TPasteEvent` and
  follows the same internal clipboard and `cmPaste` path described above.

Terminals may ignore OSC 52 clipboard-read requests. Casciian does not wait,
poll, show an error, or modify the current document unless a valid response
arrives.
