# Changelog

## Unreleased

### Compiler
- Menus can have a quoted title with spaces and symbols: `menu "Item Shop"(3) { ... }`. Menu titles can't be colored, so the editor shows `&` codes in them as plain text.
- New setting Unknown calls (Warn by default, or Error or Off): a script can call a function, or `displayMenu` a menu, that it doesn't define but the house already has. Before this it was always an error. A run checks that those functions and menus exist before changing anything, whatever the setting. Calls to such functions can't pass arguments.

- Two functions, commands or menus with the same name are now an error (the second used to replace the first in Housing). A loop can't share a function's name either, since loops are functions in Housing.
- In strings, `\"` now means `"` and `\\` means `\`; before, the backslash was sent to Housing too. Other backslashes (like `\n`) are kept as written. Item data in `({...})` is unchanged.
- Running a script says in chat what it imports, for example "Importing 3 functions (5 once compiled), 1 loop, 2 menus".

### Editor
- `&l`, `&m`, `&n` and `&o` in strings show as bold, strikethrough, underline and italic, adding up until a color code or `&r`, as in Minecraft.
- The script's contents (functions, how many functions they compile into, loops, commands, menus, events) are shown under the file name.
- Autocomplete ignores capitalisation: `i` suggests `INVOKER`.
- Pasting inside a string escapes the pasted `"` and `\`, so `"some |"` plus `"text"` becomes `"some \"text\""`. Text that already has escapes gets one more level.
- Fixed: typing, deleting or pressing Enter past the right edge didn't scroll the editor to the cursor.

### Settings
- Options are colored by how strict they are, in the dropdowns and in the tooltips: Off and Allow green, Warn yellow, Error red, Fix aqua. Override modes go from Never Override (green) through Override If Empty (yellow) and Append (aqua) to Always Override (red).

### Music
- New stop variable field in the music editor (for example `p.stop`). When it is set, the exported song puts every block of notes in `if (p.stop?0 != 1) { ... } else { return; }`, so setting the variable to 1 cuts the song off. The "1 = stop" / "1 = play" button flips the check, so the song plays only while the variable is 1. Leave the field empty to export as before.
- With a stop variable, the export lays the song out itself (blocks within Housing's limits, and extra functions `<name>_part2`, `<name>_part3`, ... when one function is full) instead of leaving the splitting to the compiler.

## 1.1.2

### Running scripts
- New override mode Append: adds the script's actions after the ones already in existing functions, loops, commands and events. It reads every page of the existing actions first and skips that part with a warning if an action type would go over its Housing limit. Existing menus only get their empty slots filled, and a smaller menu is grown to the script's size.
- When a run ends (done, stopped by an error or cancelled) after warnings, chat shows a Show warnings link that lists them all again; `/nh list warnings` does the same.
- Fixed: setting a loop's tick delay stopped the run, because Housing asks for it in an anvil even with chat inputs on.

### Compiler
- New setting Never-set variables (Warn by default, or Error or Off): warns when the script uses a variable (in a calculation, a condition, an action argument, or with `+=` and the like) that it never gives a value with `=` and that has no fallback, since any condition on an unset variable is false.
- Fixed: a fallback on the compared variable of a condition (`if (p.lives?3 > 0)`) was dropped; it now fills the condition's Fallback Value field.

### Interface
- The editor's bottom bar shows a warning only while the cursor is on it; errors still always show.
- Settings tooltips put each option on its own line.

## 1.1.1

### Changes that can affect existing scripts
- NeoForge is no longer supported; NeoHouse is Fabric only.
- `const function` (and `const loop/event/command/menu`) is removed; value constants stay.
- Multi-call mode now defaults to Error: calling a function twice in one execution stops compiling instead of misbehaving in Housing.
- `$` can no longer be used in your own variable, parameter or function names (it marks compiler-made names).
- `%`, `-x`, `x++` and `x--` on an untyped `var` are errors; cast with `(long)` or `(double)` first.
- An import no longer sees the other imports of the file that imported it (imports of imports still work).
- The built-in library's working variables are renamed and declared (`p.trigTemp`, `p.doubleTemp`, `p.longTemp`, ...).
- `speed` and `top` are reserved command names.

### Language
- `!=` comparisons.
- `%` and `%=`.
- Hexadecimal, binary and underscore-separated numbers: `0xFF00`, `0b1010`, `1_000`.
- `1d` / `1D` makes a whole number a double.
- `ph.x() == p.var` is compiled without a temporary variable.
- `playSound` accepts any Minecraft sound id through Custom Sound.
- Location presets (`HOUSE_SPAWN`, `INVOKER`, `CURRENT`) are suggested in the editor.

### Compiler
- Arithmetic on constants is computed while compiling: `p.x = 1.0 / 2.0` becomes `p.x = 0.5`.
- Fewer actions are spent on compiler-made variables; your own variables keep every write.
- Compile errors are reported for more than 20 conditions of one type in an `if`, and for scripts over Housing's limits (300 functions, 50 commands, 200 menus).
- Errors in imported files name the file; the editor underlines the import that pulled it in.
- Commented-out imports are ignored.
- Readable error messages in chat.
- Fixed: a function called inside an expression could overwrite the caller's partial result (`p.a * 2 + sq(p.b)` gave a wrong value).
- Fixed: weighted random (`10*{...}`) only added a weighted branch once.
- Fixed: `return (a + b) * c;` failed to parse.
- Fixed: functions with long names and parameters failed with "variable is too long".

### Running scripts
- New override mode Override If Empty (the default): existing functions, events, commands and menus are only filled when they're still empty.
- Lists of functions, commands and menus are read once up front, and a run stops early if the house would go over its limits.
- Before anything else, a run checks that the teams, layouts and groups the script uses exist.
- Chat inputs are used for every text field; a run stops with a message if Housing opens an anvil instead.
- Waits for Housing menus to load, and waits longer on a laggy server, instead of failing with "Could not find ...".
- Fixed: under lag a page turn could look finished too early, so searches missed existing items and list reads stopped after the first page.
- Empty functions, commands, menus and events are no longer opened just to be filled.

### Editor
- Ctrl+/ comments lines, Ctrl+D duplicates them, Alt+Up/Down moves them.
- Ctrl+click or F12 on a function name goes to its definition.
- Parameter hints appear as soon as `,` or `(` is typed, and on multi-line calls.
- Every event is suggested right after typing `event `.
- Shift+click selects; horizontal scrollbar for long lines.
- Fixed: the find bar stole shortcuts like Ctrl+A, and hints showed on unrelated lines.

### Help and settings
- New help: guide and reference topics in a sidebar, search across everything, and a Copy button on every example. The reference lists each action's and condition's arguments, allowed values and where it can be used.
- New settings: Undeclared variables (Allow / Warn / Error) and Ban the var type.
- The settings screen scrolls when it doesn't fit.

### Files and music
- One file browser for scripts (.nh), music (.nhm) and files to import (.mid, .midi, .nbs), with an icon per type. Files from the old music folder are moved in.
- Browse Folder opens the current folder in your system's file manager.
- Note Block Studio (.nbs) import.
- Music editor: click or drag the ruler above the notes to choose where playback starts.
- Export writes the song as a .nh function next to it.

### Library
- `getFacingVector()` in `trig.nh`.
- Fixed: `atan2` used a wrong value of pi.
- Library files are updated on existing installs unless you edited them.

### Minecraft versions
- Supports 1.21.2 to 1.21.11 and 26.1, 26.2 and 26.3.
- Fixed: a crash on 26.2+ when the script mob preview was shown.

### Interface
- Back buttons on screens opened by command lead to the home screen; the red X buttons are gone.
- Discarding unsaved changes no longer brings them back.
