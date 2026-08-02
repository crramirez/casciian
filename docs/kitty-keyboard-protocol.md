# Kitty keyboard protocol (CSI u) in Casciian

Status: implemented, enabled by default, as of 1.6.1-SNAPSHOT.

This document exists so that a future agent picking up keyboard work does not
have to re-derive the design decisions, and does not "fix" things that are
deliberate. Read it before touching `KittyKeyboard`, the `CSI_PARAM` state in
`ECMA48Terminal`, or anything that compares `TKeypress` instances.

## What problem this solves

The legacy VT/ASCII encoding is lossy. Ctrl+I and Tab both arrive as `0x09`;
Ctrl+M and Enter both arrive as `0x0D`; Ctrl+[ and Escape both arrive as
`0x1B`. Shift is simply dropped on control characters, so Ctrl+Shift+R is
indistinguishable from Ctrl+R.

The Kitty keyboard protocol ("disambiguated keys") fixes this by having the
terminal report such keys as `CSI keycode ; modifiers u` instead. It is a
progressive enhancement: the application asks for it, and terminals that do
not implement it silently ignore the request and keep sending legacy
sequences. **Both parsers must therefore stay alive, permanently.**

## Wire format

```
CSI unicode-key[:shifted-key[:base-layout-key]] ; modifiers[:event-type] [; text] u
```

- `unicode-key` — the code point the key produces with no modifiers on the
  current layout, or a functional key identifier from the Unicode private use
  area (57344–57454).
- `modifiers` — **1 + bitmask**. Shift 1, Alt 2, Ctrl 4, Super 8, Hyper 16,
  Meta 32, Caps Lock 64, Num Lock 128. So Ctrl = 5, Shift = 2, Ctrl+Shift = 6.
- `event-type` — 1 press, 2 repeat, 3 release.

Examples: Ctrl+I is `\e[105;5u`, Shift+Enter is `\e[13;2u`, Ctrl+Shift+R is
`\e[114;6u` (note the keycode is the *unshifted* `r`).

## Flags: what we push and why

`\e[>{flags}u` pushes a flags entry; `\e[<u` pops it. The number is a bitmask:

| Bit | Name | Pushed? |
|---|---|---|
| 1 | Disambiguate escape codes | **yes** |
| 2 | Report event types (`:1`/`:2`/`:3`) | no |
| 4 | Report alternate keys (`:shifted:base`) | no |
| 8 | Report all keys as escape codes | no |
| 16 | Report associated text | no |

We push **flag 1 only** — `KittyKeyboard.ENABLE = "\033[>1u"`.

The decoder nevertheless *parses* event types and the `:shifted` sub-parameter,
even though flag 1 alone will never produce them. This is intentional:

1. Terminal implementations vary and have shipped bugs; tolerating an
   unexpected sub-parameter costs nothing, mis-parsing one drops a keystroke.
2. Release events must be **dropped**, not mishandled. Without the
   `isRelease()` guard, turning on flag 2 would silently double every
   keystroke (one event for press, one for release).
3. Raising the flags later becomes a one-character change.

`base-layout-key` (flag 4) is parsed by `subParams()` but **deliberately not
used** — see "Known gaps" below.

## Where the code lives

### `casciian/backend/KittyKeyboard.java` (new)

Standalone, state-free decoder. It was extracted rather than inlined into
`ECMA48Terminal` specifically so the wire format is unit-testable without a
terminal or a `Backend` — the same reasoning that produced `AnsiParser` and
`Palette256`, per CLAUDE.md. **Keep it free of terminal state.**

- `ENABLE` / `DISABLE` — the push/pop sequences (`:62`, `:68`).
- `parse(String)` (`:208`) — accepts a whole sequence, with or without the
  leading CSI. Used by tests.
- `parse(List<String>)` (`:237`) — accepts parameters already split on `;` by
  the terminal's state machine, colon sub-parameters left intact. Used in
  production.
- `KeyEvent(TKeypress key, EventType type, int modifiers)` (`:163`) — the
  result. It carries the raw bitmask so Super/Hyper survive, since `TKeypress`
  has no field for them.
- `toKeypress()` (`:355`) / `functionalKey()` (`:414`) — the mapping.

Returns `null` for anything malformed or unrepresentable. It never throws.

### `casciian/backend/ECMA48Terminal.java` (modified)

- `enableKittyKeyboard()` (`:1150`), called from both stream constructors
  (`:738`, `:872`), right after `xtermMetaSendsEscape`.
- `disableKittyKeyboard()` (`:1178`), called from `closeTerminal()` (`:1105`)
  *before* anything can close the writer.
- `case 'u':` in `CSI_PARAM` (`:3507`) → `parseKittyKey()` (`:2907`).
- `if (ch == ':')` in `CSI_PARAM` (`:3424`) — accumulates sub-parameters onto
  the parameter they qualify, instead of terminating the sequence.
- `csiModifiers()` (`:2858`) — see "Behavior changes" below.

### `casciian/backend/SystemProperties.java` (modified)

`casciian.ECMA48.kittyKeyboard`, **default true**. Set it to `false` to
suppress the handshake entirely.

## Design decisions, and why

**Meta folds into Alt; Super and Hyper do not.** `TKeypress` has exactly three
modifier booleans. Meta has always meant Alt in this codebase (see
`xtermMetaSendsEscape`), so folding it is faithful. Folding Super into Alt
would make Super+X collide with Alt+X, so instead the raw bitmask is preserved
on `KeyEvent` and reachable via `isSuper()` / `isHyper()`. **Do not "simplify"
this by folding Super into Alt.**

**Ctrl/Shift force the letter uppercase.** Casciian spells Ctrl-I as
`TKeypress(ch='I', ctrl=true)` — see the `kbCtrl*` constants and the identical
folding already present in `handleModifyOtherKeys()`. Ctrl+Shift+Z therefore
becomes `('Z', ctrl, shift)`, which is *not* equal to `kbCtrlZ` (`shift=false`).
That inequality is the entire point of the feature.

**Plain Backspace stays `kbBackspaceDel`.** Keycode 127 with no modifiers maps
to the legacy `^?` constant rather than the `BACKSPACE` function key, so
existing widgets keep matching. With modifiers it becomes the function key.

**Shift+Tab maps to `kbShiftTab`, not `kbBackTab`.** Widget code checks both
(`TWidget:338`, `TComboBox:217`, `TList:317`, …), so either works; `kbShiftTab`
was chosen because Ctrl+Shift+Tab then differs only in the ctrl flag.

**Key releases are dropped in `parseKittyKey()`, not in `KittyKeyboard`.** The
decoder reports what arrived; the terminal decides Casciian has no use for it.
If release events ever become meaningful, the policy change is one method.

**Unrepresentable keys return `null`.** F13–F35, lock keys, media keys, and the
modifier keys reported as keys themselves have no `TKeypress` equivalent.
Dropping them is correct; inventing keycodes for them would collide with the
existing `TKeypress.F1`–`F12`/`HOME`/… integer space.

**Teardown is belt-and-braces.** `closeTerminal()` pops the flags, and a JVM
shutdown hook (`casciian-kitty-keyboard-restore`) covers death by exception or
abrupt exit. An `AtomicBoolean` makes the pop exactly-once; the hook
deregisters itself on a normal close, guarded against the
`IllegalStateException` you get when the JVM is already shutting down. Leaving
the flags pushed would leave the user's shell in a broken input mode after
Casciian exits — this is the failure mode most worth protecting.

## Behavior changes to existing code

`csiIsShift` / `csiIsAlt` / `csiIsCtrl` used to be string-set membership tests
(`Set.of("2","4","6","8")`). They now decode the bitmask numerically via
`csiModifiers()` and strip any `:sub-parameter`. Results are identical for the
legacy values 2–8. Two things newly work: Super-inclusive values (9–16), and
legacy sequences that carry an event type such as `\e[1;5:1D`.

`java.util.Set` was dropped from the imports as a result.

## Testing

- `KittyKeyboardTest` — 64 cases over the wire format: disambiguation,
  bitmask decoding, event types, functional keys, malformed input, and both
  `parse` entry points. Pure and fast; **this is where new protocol cases
  belong.**
- `ECMA48TerminalKittyKeyboardTest` — 11 cases end-to-end through the real
  parser: handshake emitted at startup, popped at teardown, popped exactly
  once, suppressible by property, CSI u decoded off the wire, releases
  dropped, and — importantly — `legacySequencesStillWork`, which feeds
  `\e[A`, `\e[1;5D`, `\t`, `\e[105;5u` through one terminal and asserts
  Up / Ctrl+Left / Tab / Ctrl+I in order. **Do not delete that one.**

Tests use JUnit 5 assertions, not AssertJ, despite CLAUDE.md's preference —
AssertJ is not on the test classpath (`build.gradle:36-42`).

### Manual testing

Three levels, cheapest first. Level 1 tells you whether the terminal supports
the protocol at all, which you want to know before debugging anything else.

1. **Does the terminal support it?** (Git Bash; `stty` is not in PowerShell)

   ```bash
   stty raw -echo; printf '\033[?u'; timeout 1 cat -v; stty sane; echo
   ```

   `^[[?0u` → supported. Silence → not supported, legacy path forever.

2. **What bytes does it send?**

   ```bash
   stty raw -echo; printf '\033[>1u'; timeout 5 cat -v; printf '\033[<u'; stty sane; echo
   ```

   Press combos during the 5 seconds. Tab should give `^I`, Ctrl+I should give
   `^[[105;5u`. The `timeout` is not optional: with the protocol on, Ctrl+C is
   reported as `^[[99;5u` and no longer raises SIGINT, so a bare `cat -v`
   would be unkillable from that terminal.

3. **What does Casciian decode it into?** Demo → *Keyboard probe…* (Ctrl+K),
   implemented in `demo/DemoKeyboardWindow.java`. It logs each decoded
   `TKeypress` and flips a header to **DETECTED** on the first keystroke the
   legacy encoding could not have produced.

   Caveat: `TApplication` consumes menu accelerators before any window sees
   them (`TApplication.java:1687`), so the probe can never show Ctrl+X, C, V,
   Z, Y, L, W, F1, F5, or Ctrl+K itself. `tools/KeyProbe.java` (outside `src/`,
   not part of the build) drives `ECMA48Terminal` with no `TApplication` above
   it and therefore sees everything.

## Known gaps / next steps

- **`base-layout-key` is parsed but unused.** This is the one real
  outstanding improvement. Enabling flag 4 (`ENABLE` → `"\033[>5u"`, since
  1|4 = 5) and using `keyField[2]` would make shortcuts work on non-Latin
  layouts: on a Cyrillic layout Ctrl+C currently arrives as keycode 1089
  (`с`) and matches no accelerator, so the shortcut is simply dead. The
  correct rule is narrow — **fall back to the base layout key only when the
  primary keycode is outside printable ASCII and the base is inside it.**
  Applying it unconditionally would break Dvorak and AZERTY users, who want
  the logical key, not the physical US position. Enabling flag 4 *without*
  consuming `keyField[2]` is strictly worse than leaving it off.
- **Flag 2 (event types) is not pushed**, and should not be until Casciian has
  a concept of key release. Today it would only produce work to discard.
- **No `kbCtrlShift<letter>` constants.** `TKeypress` ships
  `kbCtrlShift{Home,End,PgUp,PgDn,Up,Down,Left,Right}` but nothing for
  letters, so applications must construct their own:
  `new TKeypress(false, 0, 'R', false, true, true)`.
- **Windows Terminal support is unverified.** Use level 1 above rather than
  guessing.
- **The demo window has not been run interactively.** Layout and colors are
  unconfirmed on a real screen; the build and test suite pass.

## Things that will still eat your keystroke

Worth knowing before debugging a "the protocol is broken" report:

- The terminal itself binds Ctrl+Shift+C/V (copy/paste) and Ctrl+Shift+T/W/N
  (tabs/windows) in GNOME Terminal, Konsole, and Windows Terminal. Those never
  reach the application, protocol or not.
- Casciian's menu accelerators are consumed in `TApplication` before any
  window is reached.
- `TApplication` does *not* intercept Tab, Escape, or the arrows — those are
  handled by `TWidget.onKeypress` focus traversal, which a window can override
  (as `DemoKeyboardWindow` does).
