# OSC 7 working directory reporting

Status: implemented, as of 1.6.1-SNAPSHOT.

This document exists so that a future agent picking up working-directory or
`TChangeDirBox` work does not have to re-derive the design, and does not
"fix" things that are deliberate.

## What problem this solves

xterm-compatible terminal emulators support OSC 7
(`\033]7;file://host/path\033\\`) as a way for the running program to tell
the terminal emulator "this is my current working directory." Terminals
that understand it use this to open new tabs/splits/windows in the same
directory the user just navigated to inside the TUI, even though the
terminal itself has no other way of knowing that Casciian changed its
notion of "current directory" (Casciian never calls `chdir(2)`; there is no
real per-process working directory to inspect).

Casciian already tracked an application-level working directory via
`SystemProperties.getUserDir()` / `setUserDir()` (used by file dialogs such
as `TChangeDirBox` and `TFileOpenBox`). This session wired that cached value
to OSC 7 so hosting terminals stay in sync automatically.

## Where it lives

- `Screen.setWorkingDirectory(String)` — new method on the `Screen`
  interface (`code/src/main/java/casciian/backend/Screen.java`).
- `LogicalScreen.setWorkingDirectory(String)` — default no-op
  implementation, since most `Screen`s (e.g. in-memory backends used by
  tests) have nothing to report to.
- `ECMA48Terminal.setWorkingDirectory(String)` — the only backend that
  actually emits the escape sequence, via `getSetWorkingDirectoryString()`.
  It always emits the xterm/freedesktop-style OSC 7 `file://` URI
  (`directoryToFileUri()`), percent-encoding anything outside
  `[A-Za-z0-9/.\-_~:]` and prefixing it with a best-effort local hostname
  (`getHostname()`, falling back to `"localhost"`). On Windows it also emits
  Windows Terminal / ConEmu's OSC `9;9` extension using the native Windows
  path, because Microsoft's own same-directory documentation currently
  documents OSC `9;9` rather than OSC 7 for native Windows shells. Emitting
  is skipped entirely for a `null`/empty directory.
- `Backend.setWorkingDirectory(String)` / `GenericBackend` — forwards to
  the backend's `Screen`.
- `MultiBackend` / `MultiScreen` — fan the call out to every
  sub-backend/sub-screen, so multiplexed sessions (e.g. multiple attached
  terminals) all get the update.
- `TWindowBackend` — a backend that renders into a `TWindow` (nested TUI);
  reporting the working directory is meaningless there since there is no
  real terminal underneath, so it is a no-op like `LogicalScreen`'s
  default.

## Auto-sync with `SystemProperties`

Rather than have every call site that changes the working directory (e.g.
`TChangeDirBox.okButton()`) remember to also call
`backend.setWorkingDirectory(...)`, `SystemProperties` now supports
listeners:

- `SystemProperties.addUserDirListener(Consumer<String>)` /
  `removeUserDirListener(Consumer<String>)` — register/unregister a
  callback invoked whenever `setUserDir(String)` changes the cached
  directory. Listeners are stored in a `CopyOnWriteArrayList` since they
  may be added/removed from a different thread than the one calling
  `setUserDir`.
- `TApplication` registers a listener (`userDirListener`) during
  `TApplicationImpl()` construction that forwards the new path to
  `backend.setWorkingDirectory(path)`, immediately pushes the initial
  cached directory to the backend, and removes the listener in the screen
  handler's shutdown `finally` block when the backend is torn down.

This means `TChangeDirBox` only needs to call
`SystemProperties.setUserDir(canonical)`; it no longer needs a direct
reference to the application's backend to keep OSC 7 in sync. Any other
future call site that changes the working directory gets OSC 7 reporting
for free.

## Things to be careful about

- `SystemProperties.userDir` is a static, process-wide `AtomicReference`.
  Multiple `TApplication` instances (e.g. `TWindowBackend`-nested
  applications) would all be notified by any one of them changing the
  directory — this is consistent with the existing single, static "current
  directory" model in `SystemProperties`, not a new limitation introduced
  here.
- `ECMA48Terminal.directoryToFileUri()` deliberately does *not* validate
  that `directory` is an absolute, real filesystem path; it trusts the
  caller. `SystemProperties.setUserDir()` is normally only called with a
  canonicalized path (see `TChangeDirBox.okButton()`), so this should hold
  in practice.
- The hostname lookup (`getHostname()`) tries `$HOSTNAME`, then
  `%COMPUTERNAME%` (Windows), then a reverse DNS lookup, before falling
  back to `"localhost"`. This can be slow or fail on systems without proper
  DNS; failures are swallowed and do not prevent the OSC 7 sequence from
  being emitted.
- OSC 7 is the cross-terminal standard, but Windows Terminal's
  same-directory feature has historically documented OSC `9;9` for native
  Windows shells. Casciian therefore emits both on Windows: OSC 7 for
  standards-compatible terminals, plus OSC `9;9` for Windows Terminal /
  ConEmu compatibility.
