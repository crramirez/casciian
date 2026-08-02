# Casciian design notes

Longer-form notes on subsystems where the "why" is not obvious from the code,
aimed at anyone — human or agent — picking the work back up later. These
complement `CLAUDE.md` at the repo root, which covers build commands, the
widget hierarchy, and house style.

| Document | Covers |
|---|---|
| [kitty-keyboard-protocol.md](kitty-keyboard-protocol.md) | Unambiguous key reporting (CSI u): the handshake, the decoder, why Ctrl+I no longer collapses into Tab, and how to test it against a real terminal |
