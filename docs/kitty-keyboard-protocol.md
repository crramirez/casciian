# Keyboard protocol negotiation in Casciian

Status: Kitty CSI u with automatic xterm modifyOtherKeys level 2 fallback.

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

## Protocol selection

Casciian selects keyboard input in this order:

1. Kitty CSI u.
2. xterm modifyOtherKeys level 2.
3. Legacy VT input.

The startup order remains Kitty query followed by Device Attributes (DA).
When Kitty replies before DA, Kitty is selected and modifyOtherKeys is not
requested. When DA arrives while Kitty support is still unknown, Kitty is
marked unsupported and Casciian sends XTQMODKEYS (`CSI ? 4 m`) followed
immediately by the level-2 request (`CSI > 4 ; 2 m`). The request is not
blocked on the XTQMODKEYS reply: terminals may support setting the mode without
answering the query, and unsupported terminals safely ignore both sequences.

Level 2 is required because level 1 does not provide the complete,
unambiguous modified-key input Casciian needs. The legacy parser remains
active so ignored requests naturally fall back without timeouts or terminal
name detection.

`casciian.ECMA48.modifyOtherKeys` accepts:

- absent or `auto` (default): attempt level 2 only after Kitty is unavailable;
- `true`: explicitly enable the same fallback, while retaining Kitty priority;
- `false`: never request modifyOtherKeys; use legacy VT if Kitty is unavailable.

Kitty always has priority. If a valid Kitty response arrives unexpectedly
late, Casciian immediately restores modifyOtherKeys before selecting Kitty.

## modifyOtherKeys state and restoration

`ECMA48Terminal` separately records whether level 2 was requested, whether
XTQMODKEYS confirmed support, the original level (`-1` when unknown), and
whether Casciian changed terminal state. The authoritative effective protocol
is exposed by `getActiveKeyboardProtocol()`; a silent modifyOtherKeys request
is reported separately and does not falsely become confirmed support.

An XTQMODKEYS reply (`CSI > 4 ; level m`) records the previous level. On exit,
Casciian restores exactly level 0 or 1 when reported. A reported level 2 is
recognized as pre-existing state and is not reset. If Casciian requested
level 2 but the original state is unknown, cleanup uses xterm's reset-to-initial
form (`CSI > 4 m`) rather than assuming level 0. Atomic guards make normal
close, JVM shutdown-hook cleanup, and repeated shutdown paths restore state at
most once, before the output stream closes.

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

## ⚠ The push MUST happen after switching to the alternate screen

This has already caused a real regression in this repo (details below) and will bite anyone who reorders the constructor without knowing it: **the spec requires terminals to keep independent keyboard mode flag stacks for the main screen and the alternate screen.** `TWindow`/`ECMA48Terminal` sessions run entirely on the alternate screen (`terminal.enableMouseReporting(true)` emits `\e[?1049h`, "smcup", as a side effect). If `KittyKeyboard.ENABLE` is written *before* that switch, the push lands on the **main screen's** stack — the one nothing ever reads from — and the alternate screen starts with an empty stack. The terminal is not lying, not broken, and not misconfigured; Casciian just pushed the flag onto the wrong stack, so every keystroke for the rest of the session falls back to legacy encoding, indistinguishable from a terminal with no protocol support at all.

This happened for real: the query/DA-sentinel detection logic (see "Support detection" below) was added by moving `enableKittyKeyboard()` earlier in the constructor, ahead of `enableMouseReporting(true)`, to satisfy an ordering requirement of its own (query before the Device Attributes sentinel). That silently broke real disambiguation on WezTerm and Windows Terminal 1.25, which had been confirmed working immediately beforehand. The fix was **not** to abandon the detection logic, but to keep `enableKittyKeyboard()` positioned after `enableMouseReporting(true)`, and instead move the Device Attributes request (`\e[c`) to immediately follow it, so both ordering constraints hold at once:

```
xtermReportVersion()
xtermReportPixelDimensions()
enableMouseReporting(true)      // switches to the alternate screen (smcup)
enableKittyKeyboard()           // push + query — now on the correct stack
"\e[c"                          // DA request — the sentinel, sent right after
xtermMetaSendsEscape(true)
...
```

**If you ever touch the order of statements in either `ECMA48Terminal` constructor, re-verify this constraint by hand** — there is no automated test for it, since `HeadlessBackend`-based unit tests never talk to a screen-buffer-aware real terminal. The only way to catch a regression here is the manual test in "Manual testing" below, against a real terminal that actually implements the protocol (WezTerm with `enable_kitty_keyboard = true`, or kitty itself).

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

- `ENABLE` / `DISABLE` (`:62`, `:68`) — the push/pop sequences.
- `QUERY = "\033[?u"` (`:81`) — asks the terminal to report its current
  flags; used for support detection, see below.
- `SupportState` (`:89`) — `UNKNOWN` / `SUPPORTED` / `UNSUPPORTED`. This is
  what an application checks before deciding whether to advertise a
  disambiguation-dependent shortcut in its own UI.
- `parse(String)` (`:251`) — accepts a whole sequence, with or without the
  leading CSI. Used by tests.
- `parse(List<String>)` (`:280`) — accepts parameters already split on `;` by
  the terminal's state machine, colon sub-parameters left intact. Used in
  production.
- `KeyEvent(TKeypress key, EventType type, int modifiers)` (`:206`) — the
  result. It carries the raw bitmask so Super/Hyper survive, since `TKeypress`
  has no field for them.
- `toKeypress()` (`:398`) / `functionalKey()` (`:457`) — the mapping.

Returns `null` for anything malformed or unrepresentable. It never throws.

### `casciian/backend/ECMA48Terminal.java` (modified)

- `enableKittyKeyboard()` (`:1180`), called from both stream constructors
  right after `terminal.enableMouseReporting(true)` — see the alternate-screen
  warning above for why that position is load-bearing. It sends `ENABLE` and
  `QUERY` together, then registers the JVM shutdown-hook fallback.
- `disableKittyKeyboard()` (`:1219`), called from `closeTerminal()` (`:1118`,
  the call site is `:1135`) *before* `enableMouseReporting(false)` switches
  back to the main screen, and before anything can close the writer.
- `getKittyKeyboardSupport()` (`:2407`) — the public accessor for
  `SupportState`.
- `case 'u':` in `CSI_PARAM` (`:3565`) — branches on `decPrivateModeFlag`
  (set when a `?` was seen in `CSI_ENTRY`): a `?`-flagged `u` is the
  terminal's reply to `QUERY` and sets `SUPPORTED`; otherwise it's a real
  keystroke and goes to `parseKittyKey()`.
- `case 'c':` in `CSI_PARAM` (`:3641`) — the existing Device Attributes
  handler, now doubling as the support-detection sentinel: if
  `kittyKeyboardSupport` is still `UNKNOWN` when DA's reply arrives (DA is
  answered by every terminal), it's set to `UNSUPPORTED`. This can never
  clobber an already-`SUPPORTED` state, and a late-arriving `QUERY` reply
  always overwrites `UNSUPPORTED` back to `SUPPORTED` unconditionally, so the
  detection is correct regardless of which reply physically arrives first.
- `if (ch == ':')` in `CSI_PARAM` (`:3482`) — accumulates sub-parameters onto
  the parameter they qualify, instead of terminating the sequence.
- `csiModifiers()` (`:2916`) — see "Behavior changes" below.

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

**Support detection exists because silence is ambiguous.** A terminal that
does not implement the protocol, and a terminal that implements it but has it
turned off in its own config (WezTerm ships `enable_kitty_keyboard` **off by
default** — see "Real-world findings" below), both respond to `QUERY`
identically: not at all. There is no error, no negative acknowledgment,
nothing to parse. The only way to turn that silence into a definite answer is
a sentinel — something every terminal is guaranteed to answer, sent right
after `QUERY`. Device Attributes (`\e[c`) was already being requested for
other reasons, so it was repurposed rather than inventing a new one. This is
why the DA request had to move next to `enableKittyKeyboard()`: the sentinel
technique only works if the two requests are adjacent in the outgoing stream.

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
- `ECMA48TerminalKittyKeyboardTest` — 17 cases end-to-end through the real
  parser: handshake emitted at startup (including the query), popped at
  teardown, popped exactly once, suppressible by property, CSI u decoded off
  the wire, releases dropped, the support-detection sentinel logic
  (`queryReplyMeansSupported`, `daWithoutQueryReplyMeansUnsupported`,
  `queryReplyBeforeDaSentinelWins`, `disabledPropertyIsUnsupportedImmediately`),
  and — importantly — `legacySequencesStillWork`, which feeds `\e[A`,
  `\e[1;5D`, `\t`, `\e[105;5u` through one terminal and asserts
  Up / Ctrl+Left / Tab / Ctrl+I in order. **Do not delete that one.**

None of these tests can catch the alternate-screen ordering bug described
above, because `HeadlessBackend`/the test harness never models screen-buffer
switching or per-screen flag stacks — they only assert that the *bytes we
write* are correct, not that a real terminal would apply them to the screen
Casciian actually runs on. That gap is inherent to unit-testing a wire
protocol without a real terminal on the other end; the manual test below is
the only thing that exercises it.

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
   implemented in `demo/DemoKeyboardWindow.java`. It reads the effective
   protocol, Kitty support, and modifyOtherKeys request/support state live from
   `ECMA48Terminal`. The keystroke log below the status still shows the decoded
   `TKeypress` fields for whatever you press.

   Caveat: `TApplication` consumes menu accelerators before any window sees
   them (`TApplication.java:1687`), so the probe can never show Ctrl+X, C, V,
   Z, Y, L, W, F1, F5, or Ctrl+K itself. `tools/KeyProbe.java` (outside `src/`,
   not part of the build) drives `ECMA48Terminal` with no `TApplication` above
   it and therefore sees everything.

### Real-world findings from testing this against actual terminals

- **WezTerm ships `enable_kitty_keyboard` off by default.** A completely
  correct implementation on Casciian's side will still show legacy-only
  behavior against a stock WezTerm config; the raw-byte test in step 2 above
  will show plain `^I` for Ctrl+I even with `ENABLE` pushed. The user has to
  add `config.enable_kitty_keyboard = true` to `wezterm.lua` (inside the
  config table if using the `return { ... }` style, or as `config.xxx = ...`
  if using `wezterm.config_builder()`). Multiple other terminals gate this
  behind a similar opt-in; do not assume a terminal that "supports" the
  protocol in its changelog has it active by default. This is exactly the
  `UNSUPPORTED` case the sentinel above is designed to detect and report.
- **The alternate-screen ordering bug (see the warning above) looks
  identical to "terminal doesn't support it."** Both present as: `ENABLE` is
  sent, no crash, but every keystroke still arrives as legacy VT. The only
  way to tell them apart is the raw-byte test in step 2, run independently of
  Casciian: if a terminal known to support the protocol (WezTerm with the
  config flag on, or kitty itself) still fails that test, suspect Casciian's
  handshake ordering before suspecting the terminal.

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
- **No automated test covers the alternate-screen ordering constraint.** See
  the warning above. `HeadlessBackend` has no concept of a screen buffer
  switch, so a regression here can only be caught by the manual test against
  a real terminal — it will not fail `./gradlew test`.
- **The demo window's layout and colors have not been visually confirmed on
  a real screen**, only that it compiles and the underlying detection logic
  is unit-tested. Windows Terminal 1.25 and WezTerm have both been confirmed,
  by the person who filed this note, to correctly disambiguate Ctrl+I once
  the alternate-screen ordering bug above was fixed and (for WezTerm)
  `enable_kitty_keyboard = true` was set.

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
