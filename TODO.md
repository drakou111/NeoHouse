# TODO

## Open

- [ ] Back button / logo / "(i)" overlap: the code editor header is fixed; no "(i)" element exists in NeoHouse's own screens, so if it still overlaps somewhere, note which screen.

## Needs checking in game (done in code, can't be tested without Hypixel)

- [ ] Chat inputs: every text field is answered in chat; an anvil (other than list search bars and Custom Sound) stops the run with a "turn on chat inputs" message.
- [ ] Override If Empty: existing functions/commands/events are opened and slot 10 is checked for an action; menus count as empty when every element is a gray glass "Empty Slot".
- [ ] Commands and custom menus lists use the same layout as the functions list (entries in slots 10-34, next-page arrow in slot 53) for the pre-read.
- [ ] Menu loading: a menu counts as loaded once it shows items that stay the same for 4 ticks (fixes "Could not find ..." on slow servers).
- [ ] Custom Sound: playSound with a name that isn't a listed sound clicks "Custom Sound" and types the id.
- [ ] 26.1 / 26.2 / 26.3 builds: compile and pass tests, but haven't been launched in game. 26.3 uses SDL key codes (Keys compat set); default keybinds K and N should be checked there.
- [ ] Team / layout / group check: names are read from item names in slots 10-34 of `/teams`, `/layout` and `/menu` -> "Permissions and Groups"; check those menus really list them there.
- [ ] Editor: horizontal scrollbar, suggestion and parameter-hint behaviour (see Done).

## Done

- Skip the functions list when the script defines no functions (same for commands and menus).
- Commented-out imports are ignored.
- Errors from an imported file name that file; the editor underlines the import line that pulled it in.
- Commands and menus lists are read once up front, like functions.
- Multi-call: copies are assigned correctly (each copy calls its own copies), Error is the default mode, Warn only warns; trig.nh no longer breaks it (cos no longer calls sin).
- Imports are not visible to the files they import (transitive imports still are).
- New override mode "Override If Empty" (default).
- `const function/loop/event/command/menu` removed (value constants stay).
- Back buttons of screens opened by command go to the home page.
- Code editor header: logo no longer under the Back button.
- Discarding unsaved changes no longer brings them back when reopening NeoHouse.
- Hex, binary and underscore number literals (0xFF00, 0b1010, 1_000).
- Empty functions, commands, menus and events are never opened just to be filled.
- At most 20 conditions of one type per conditional (limit stored per condition in AddCondition).
- More reliable menu searches and anvil input.
- Chat error messages: one line per message, offending token highlighted inline.
- Housing limits (300 functions, 50 commands, 200 menus): compile error when a script alone exceeds them, run stops when existing + new would.
- `%` and `%=`.
- Shift+click selection in the editor.
- Find bar no longer steals Ctrl+A and other shortcuts once the code is clicked.
- Cleaned up messages; no em dashes or other non-ASCII punctuation.
- NeoForge dropped; Fabric builds for 1.21.2 to 1.21.11 and 26.1, 26.2, 26.3.
- Custom Sound for playSound.
- Title fade/stay times documented as seconds.
- `ph.x() == p.var` is mirrored to `p.var == ph.x()` instead of using a temp variable.
- Chat inputs assumed for every text field.
- Teleport/location presets (HOUSE_SPAWN, INVOKER, CURRENT) are suggested in the editor.
- Built-in library files are upgraded on existing installs unless the user edited them.
- `!=` comparison (an `==` condition with Invert on).
- Editor: parameter hints no longer appear on lines that aren't part of a call; suggestions only open while typing; find matches follow edits; horizontal scrollbar.
- Before anything else, a run checks that every team, layout and group the script names exists, and stops listing the missing ones.
