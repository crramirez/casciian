# Casciian design notes

Longer-form notes on subsystems where the "why" is not obvious from the code,
aimed at anyone — human or agent — picking the work back up later. These
complement `CLAUDE.md` at the repo root, which covers build commands, the
widget hierarchy, and house style.

| Document | Covers |
|---|---|
| [kitty-keyboard-protocol.md](kitty-keyboard-protocol.md) | Unambiguous key reporting (CSI u): the handshake, the decoder, why Ctrl+I no longer collapses into Tab, and how to test it against a real terminal |
| [bracketed-paste.md](bracketed-paste.md) | Terminal paste detection, `TPasteEvent`, and integration with Casciian's internal clipboard and `cmPaste` command |
| [osc7-working-directory.md](osc7-working-directory.md) | Reporting the current working directory to the hosting terminal (OSC 7): where it lives across the `Screen`/`Backend` hierarchy, and how it auto-syncs with `SystemProperties` |
| [vector-api-performance.md](vector-api-performance.md) | Which image kernels use the Java Vector API and which deliberately do not, how the demo benchmark window measures them, and the native-image build flags that affect the results |
