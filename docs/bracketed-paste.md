# Bracketed paste

Casciian enables xterm bracketed paste mode for ECMA-48 terminal sessions.
The terminal backend collects each paste as one `TPasteEvent`, preserving its
text without interpreting embedded terminal sequences.

`TApplication` copies that text into Casciian's internal clipboard and
dispatches the existing `cmPaste` command. Widgets therefore use their normal
paste behavior and remain independent of the terminal protocol.
