# NeoHouse — Diagnostic Report

---

## 1. Code Smells

### `Config.java` is a dead file
`Config.java` registers an empty `ModConfigSpec` with NeoForge but never declares any config fields. All actual configuration lives in `NeoHouseConfig.java` (manual JSON). The two systems coexist without interacting. `Config.java` should be deleted; it currently registers a no-op `ModConfigEvent` listener and introduces a misleading parallel to `NeoHouseConfig`.

### `ScriptRunner.sourceNeedsCreativeMode` — text scanning instead of AST
`ScriptRunner` determines whether a script needs creative mode by doing `source.contains(action.name + "(")` on the raw source string after compilation. This means:
- It can false-positive on comments or string literals containing the action name
- It runs against the original source, not the preprocessed+expanded version
- It fires before imports are resolved

The right fix: check which `AddAction` values with `ITEM_LITERAL` params are actually emitted by the `CodeGenerator`, not which keywords appear in text.

### Silent config load failure in `NeoHouseConfig`
`NeoHouseConfig.load()` wraps everything in a try-catch that discards all exceptions (including `JsonParseException`, `IOException`, and `ClassCastException`). A corrupt or outdated config file silently falls back to defaults with no user notification. At minimum the catch should log a warning.

### Implicit global auto-promotion in `ScopeCheckingVisitor`
When a variable reference is not found in any scope, `ScopeCheckingVisitor.resolveOrDeclareGlobal` silently promotes it to a global `VAR`. A typo in a variable name creates a brand-new undeclared global with no warning. This should at least emit a warning, and ideally be an error unless the variable has an explicit `var` type declaration.

### Reflection in `AnvilAction`
`AnvilAction` accesses `AnvilScreen`'s private `name` field via `setAccessible(true)`. This breaks silently on obfuscated builds or mapping updates and throws a wrapped `RuntimeException` that drops the original cause. If this field is accessible another way (e.g. mixin or accessor interface), that would be far more robust.

### Duplicate enum-to-enum conversion in `CodeGenerator.toHolder`
`CodeGenerator.toHolder` converts `compiler.enums.Holder` to `constants.Holder` with a switch that maps each variant 1:1. Two enums exist for the same concept because one lives in the compiler layer and one in the game-mapping layer. Sharing a single `Holder` enum (or making `CodeGenerator` import the compiler one directly) would eliminate this boilerplate.

### `ConditionCallNode` missing from `AddCondition` list
`AddCondition` lists 22 conditions, but one (`VARIABLE`) and one (`PLACEHOLDER`) are handled as `BooleanExpressionNode` rather than `ConditionCallNode` inside `CodeGenerator.generateConditionCall`. The `default` fallthrough throws `IllegalStateException` at code-generation time rather than at parse time — a late error for something that could be caught earlier.

### `LinkedHashMap<WaitForAction, Object>` as a set
`ScreenEvents` uses `LinkedHashMap<WaitForAction, Object>` with null values to emulate an ordered set. Use `LinkedHashSet<WaitForAction>` instead.

### `VoidReturnNode` in `generateStatement` emits an Exit action inconsistently
In `generateStatement`, a `VoidReturnNode` inside a non-`NONE` context generates an Exit action. But `FunctionParamLoweringVisitor` and `ReturnPlacementVisitor` should already have normalized all returns before code generation. The runtime context-switch at generation time is a workaround for possibly-incomplete normalization and makes the statement generator non-uniform.

---

## 2. Features That Should Be Removed

### `Config.java`
As noted above — completely empty, no purpose.

### Commented-out trig functions in `BNF.bnf`
Lines 63–72 of `BNF.bnf` have `sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `sqrt`, `nroot`, `pow`, and `|x|` commented out. These are in `const_expr`, which is only evaluated at compile time. Either implement them (they're useful for computing spawn coordinates, angles, etc.) or remove the comment block. Dead commented-out grammar is confusing.

### TODO comments in `HomeScreen.java`
Two TODO comments reference e4mc and Wurst detection (`//TODO You know e4mc exists, right?` and `//TODO You know, maybe don't use cheats...`). These are half-formed ideas that have been sitting in the source tree. Either implement them as hints or delete them.

---

## 3. Features That Should Be Added

### Find & Replace in `CodeEditorScreen`
The editor has no search capability. Ctrl+F for find and Ctrl+H for replace are standard and would be heavily used when working with long scripts. The `SyntaxEditorWidget` would need a search overlay.

### Go-to-line shortcut
Ctrl+G to jump to a specific line number would complement the line-number gutter already present.

### Parser error recovery
The parser currently stops at the first error. Adding basic panic-mode recovery (skip to the next statement or top-level declaration boundary) would allow reporting multiple errors per compile, which is a significant quality-of-life improvement for iterative development.

### `cancel` keyword restricted to events
`cancel` maps to Hypixel's `CANCEL_EVENT` which is only meaningful inside an event handler. The `ActionRestrictionVisitor` probably already enforces this at code-generation time, but the parser and scope checker don't reject it in function/command bodies early, leading to a late error message.

### Undo history in the editor
`SyntaxEditorWidget` inherits from Minecraft's `EditBox`, which has no undo stack. Users can lose work if they accidentally overwrite a large block. A simple circular buffer of text snapshots triggered on edit would suffice.

### Runtime loop construct
The language has `repeat(N)` which is compile-time unrolled. There is no runtime loop. Hypixel Housing supports "loop" functions (periodic tick execution), but the language has no way to express "do this N times at runtime" — all loops must be manually unrolled at compile time or modeled as loop functions. A `while`-style construct that desugars into trigger-function recursion would be useful, though Hypixel's action limits constrain how deep it can go.

### Warning system alongside errors
Currently `CompilingException` is thrown on the first problem. A warning list (non-fatal) would let the compiler report auto-promoted globals, unreachable code, or deprecated usage without aborting compilation.

### File browser: rename and create new file
`FileBrowserScreen` can browse and open files, but there is no in-UI way to rename or create a new `.nh` file without going to the OS file manager.

### Syntax theme hot-reload
Changing the syntax theme in `ConfigScreen` requires closing and re-opening the editor. The editor could listen for config changes and re-tokenize on theme switch.

---

## 4. Features That Should Be Modified

### `ScriptRunner` creative-mode detection
Replace the raw-string scan with a post-compilation AST walk (or a flag emitted by `CodeGenerator`) that records whether any item-related action was actually emitted. See §1 for details.

### `NeoHouseConfig` error handling
Silent failure on config load should at minimum push a message to the Minecraft chat (via `MessageUtils.errorToChat`) when the JSON is malformed, so users know their settings were not applied.

### `WaitForAction` timeout
The 200-tick (10 second) timeout is hardcoded. Heavy scripts navigating deep menus can legitimately exceed this. Expose it in `NeoHouseConfig`.

### `overrideExisting` config description
The `ConfigScreen` tooltip says "When running a script, recreate existing functions, events, commands and menus from scratch. Disable to skip items that already exist." The distinction between what "already exists" means for events (has content in slot 10) vs. functions (name search returns a result) is not surfaced. The tooltip should note that this affects code generation strategy, not just whether items are skipped.

### `ActionCountSplittingVisitor` naming
Functions it creates to absorb overflow actions are named with internal conventions that may conflict with user-defined names. The naming scheme for generated helper functions should be clearly separate (e.g. use a prefix like `__split_` that the parser rejects as a user name).

### `deriveConditionalActionCtx` in `CodeGenerator`
This method manually manipulates enum name strings (`name.replace("EVENT_", "EVENT_CONDITION_")`). This is fragile and couples `CodeGenerator` to the naming convention of `RestrictionAction`. Encoding the nested-context equivalent directly in the `RestrictionAction` enum (e.g. a `conditionalVariant()` method) would be cleaner.

### `BNF.bnf` — `name` production excludes digits
The `name` production only allows `[a-z] | [A-Z] | '_'`, meaning identifiers cannot contain digits. This is more restrictive than Housing's actual naming rules (which allow digits after the first character). At minimum the grammar should match what the lexer actually accepts.

### `TypedFunctionNode` is unused in code generation
`TypedFunctionNode` (functions that return a value) exists in the AST and is handled by `ScopeCheckingVisitor` and return checkers, but `CodeGenerator.generate` only handles `VoidFunctionNode`. Typed functions are lowered/inlined earlier in the pipeline, which means any typed function that survives to code generation would silently be skipped. This should be explicitly verified (assert or error) rather than left as a silent gap.

---

## 5. Refactor Areas

### `DefaultVisitor.java` — boilerplate tree transformation
`DefaultVisitor` must manually implement identity-transform visit methods for every AST node type (currently 28+ methods). Every time a new node type is added, `DefaultVisitor` must be updated or it silently returns null. Consider generating this class from the AST node hierarchy via annotation processing, or switching to a record-based AST where structural copying is derivable.

### `CodeGenerator` two-pass override branching
The `generate` method has an `if (override)` block for Pass 1 followed by a second loop for Pass 2. The `generateXxx` vs `generateXxxIfAbsent` method pairs duplicate the structure of every top-level entity type. Extracting a `TopLevelStrategy` interface (with `createShell()` and `fillBody()` methods) would centralize this duplication.

### Visitor chain in `NHCompiler` — hard-coded linear sequence
The 16-visitor pipeline in `NHCompiler.compile` is written as a sequence of local variable declarations and method calls. Adding, removing, or reordering a visitor requires editing this method and being careful about what state each visitor depends on. Expressing the chain as a `List<VisitorPass>` with named steps would make the pipeline easier to test in isolation and easier to document.

### `AddAction` / `AddCondition` enums — data in code
These enums encode 45 actions and 22 conditions with per-entry field data (max counts, param kinds, display names). As the action list grows, the enums grow unwieldy. A data-driven approach (JSON or a dedicated resource file read at startup) would separate the Housing API surface from compiler logic.

### `RestrictionAction` / `RestrictionCondition` — combinatorial enum
`RestrictionAction` encodes 40+ variants covering every event type, their conditional sub-contexts, menu contexts, etc. The naming convention (`EVENT_CONDITION_DEATH`, etc.) is load-bearing but brittle. Replacing this with a composite context object (`ExecutionContext { EventType event; boolean inConditional; boolean inMenu; }`) would make the restriction rules explicit and testable without string manipulation.

### `NeoHouseConfig` — manual JSON instead of NeoForge config system
`NeoHouseConfig` manually reads/writes a JSON file in `.minecraft/neohouse/`. The mod already pulls in NeoForge which provides a robust config system (TOML-based, with hot-reload support, validation, and UI integration). Migrating to NeoForge config would eliminate `NeoHouseConfig.load()`, `NeoHouseConfig.save()`, and the silent-failure trap.

### `Preprocessor` — `define` at both preprocessor and AST level
The `#define NAME = value` directive is processed by `Preprocessor` as text substitution, but there is also a `DefinitionNode` in the AST (with its own `DefinitionNode` class and `DeclarationCollectorVisitor` handling). It's unclear whether these are the same feature at different stages or two separate mechanisms. The duplication should be resolved: either definitions are purely preprocessor-level (and `DefinitionNode` is removed), or they are purely AST-level (and the preprocessor text substitution is removed).

---

## 6. Unhandled / Mishandled Exceptions

| Location | Issue |
|---|---|
| `NeoHouseConfig.load()` | Catches all exceptions and discards them silently. Malformed config files reset to defaults with no feedback. |
| `AnvilAction` | Catches `Exception` from reflection and rethrows as `RuntimeException`, losing the original cause type and stack frame. |
| `ScriptRunner.runScript(Path)` | `CompilingException` and `PermissionException` are declared, but the callers in `CodeEditorScreen.saveAndRun()` catch generic `Exception` and route to `MessageUtils.errorToChat`. This hides the structured error data (token, line, column) that `CompilingException` carries. |
| `CodeEditorScreen.checkErrors()` | The outer `catch (Exception e)` after `catch (CompilingException e)` swallows unexpected runtime exceptions as `"Internal error: " + e.getMessage()`. `getMessage()` can return null, resulting in `"Internal error: null"` with no further context. Should use `e.toString()` or include the class name. |
| `WaitForAction` | Times out after 200 ticks and throws `InterruptActionException`. The exception is caught inside `SequenceManager` but callers that invoke `SequenceManager.startAction` have no way to react to a timeout. The timeout should surface as a user-visible message. |
| `ScopeCheckingVisitor.resolveOrDeclareGlobal` | Silently auto-promotes unknown identifiers to global VAR (see §1). This is a form of silent semantic failure that should be a warning or error. |
| `CodeGenerator.generateStatement` | `default -> throw new IllegalStateException(...)` is correct, but this exception is unchecked and will bubble up to `CodeEditorScreen.checkErrors()` as the generic `catch (Exception e)` case, producing an unhelpful "Internal error" message. |
| `Preprocessor` | Circular import detection throws a `CompilingException` with a null token. The error message reads "Circular import detected", which doesn't tell the user which file is the cycle root. |
| `Parser` | Throws `ParsingException` on first syntax error with no recovery. All subsequent errors are lost. |

---

## 7. Grammar of the Language

### Overview
NeoHouse scripts compile to Hypixel Housing action sequences. The language is **statically-typed** with four primitive types (`long`, `double`, `string`, `bool`) plus `var` (dynamic / Housing's untyped variable). The type system is shallow: types influence operator checking but all Housing variables are strings at runtime.

### Type System
- `long` and `double` for numeric work; `var` for Housing's native variable references
- `bool` is a first-class literal but not a `var` type (Housing doesn't have boolean variables)
- Type widening (`long → double`, `long → string`, etc.) should be clarified in the grammar — it happens implicitly in `TypeInferringVisitor` but isn't described in `BNF.bnf`
- The `var [N]` array type appears in the grammar and AST but there is no evidence that runtime array operations are fully implemented

### Control Flow
- **`if / else`** — maps to Hypixel's `CONDITIONAL` action (max 25 per function)
- **`repeat(N)`** — compile-time unrolling, not a runtime loop; `N` must be a constant expression
- **`random { ... }`** — maps to Hypixel's `RANDOM` action; each brace block is an equal-probability branch
- No `while`, `for`, or `do-while` — intentional because Housing has no runtime looping primitive

### Variable Scopes
- `p.name` — player variable (per-player, persists across sessions)
- `g.name` — global variable (shared across all players)
- `t[teamName].name` — team variable (shared within a team)
- Local (stack) variables exist syntactically (`long x;`) but are lowered to global or player variables by the compiler pipeline since Housing has no concept of local scope

### Functions
- `function name(params) { }` — void function; params are lowered to global variables by `FunctionParamLoweringVisitor`
- `function Type name(params) { }` — typed (returning) function; return value is communicated through a generated global variable
- `event EventName { }` — event handler; restricted action set depending on event type
- `command name [self|target] (priority, listed) { }` — slash command handler
- `menu name(rows) { slot N: (item) { } }` — custom inventory menu
- Loop functions are named functions registered with a tick delay

### Expressions
- Standard arithmetic: `+ - * / %`, bitwise ops, shifts
- Comparison: `== != < > <= >=`
- Logical: `&&` (AND), `||` (OR) — Housing maps these to MATCH_ANY flag on `CONDITIONAL`
- Unary: `-`, `!` (not), `++`, `--` (prefix/postfix), `(long)`, `(double)`, `(string)` casts
- Placeholders: `ph.player.health`, `ph.date.unix`, `ph.random.whole(1,100)`, etc. — read-only game state values
- No string interpolation — string values must be `+`-concatenated

### Known Grammar Issues
1. **`name` production excludes digits** — identifiers cannot contain `[0-9]` at any position, which is more restrictive than the Hypixel Housing naming rules
2. **`const_expr` trig functions commented out** — the grammar references math functions that aren't implemented
3. **`bool` type** — `bool` is listed as a `base_type` but `VariableType.BOOL` can't be stored in a Housing variable; scripts that try to assign a bool expression to `p.x` should be a type error
4. **No string escape sequences** — `char` in `BNF.bnf` is defined as "any character except `"` or `\n`". There is no `\n`, `\t`, `\\`, or `\"` escape sequence described
5. **`random` body semantics** — the grammar allows any statements inside each `random` branch, but Hypixel gives each branch equal probability. There is no syntax to weight branches
6. **`cancel` keyword** — semantically valid only inside event handlers, but the grammar permits it anywhere a statement is valid

---

## 8. UI Design

### Strengths
- Consistent dark theme (`0xCC0E0E14` background, `0xEE1A1A2A` header) across all screens
- Transparent background lets the game world show through (immersive, appropriate for an in-game tool)
- Section dividers in `ConfigScreen` provide clear grouping
- Dirty indicator (`●`) and orange filename in `CodeEditorScreen` are clear and minimal
- The info `(i)` button in `CodeEditorScreen` opens help without leaving the editor context
- Footer action bar in `FileBrowserScreen` (select then act) prevents accidental double-tap actions
- Keyboard shortcut Ctrl+S works in the editor
- Hint system in `HomeScreen` provides personality without being intrusive

### Issues and Recommendations

**`FileBrowserScreen`**
- No way to create, rename, or delete files from within the UI
- No path breadcrumb — if scripts can be nested in subdirectories, navigation history is not visible
- No keyboard navigation (arrow keys, Enter to open) — everything requires mouse clicks
- Empty state (no `.nh` files found) shows nothing; a "Create new script" prompt would be more helpful

**`CodeEditorScreen`**
- No find/replace — critical for scripts with repetitive patterns
- No go-to-line — the gutter shows numbers but you can't jump to a specific line
- Error indicator currently only highlights the error token. Long scripts need the error to also be visible in the scrollbar/gutter as a marker when the error line is off-screen
- No syntax highlighting for keywords inside string literals (not a priority, but noted)
- The `Save & Run` button compiles twice: once for the live error check, once during `save()`. The compiled result from `checkErrors()` is thrown away. The button could reuse the last clean compile result

**`ConfigScreen`**
- The theme preview is hardcoded (`renderPreview`). If new syntax token types are added, the preview must be manually updated
- No validation feedback on the `clickDelay` text field (non-numeric input is silently ignored on save)
- The `< Back` button and the red `X` button do the same thing (both call `closeAndSave()`). One of them should be removed

**`HelpScreen`**
- Static document with no search
- Keyboard shortcut list is incomplete (Ctrl+S is mentioned implicitly via "Save" button, but Ctrl+S is not documented anywhere visible)

**`InputDialogScreen`**
- The confirm dialog ("Unsaved changes. Discard?") uses OK/Cancel terminology. A more specific label like "Discard changes" / "Keep editing" would be clearer

**General**
- No loading/progress indicator when running a long script — the UI gives no feedback that the script is executing (the action runner works in the background, but there's no spinner or status line)
- Escape key dismisses suggestions in the editor but also triggers the "unsaved changes" dialog if nothing is focused — the key handling priority could be made clearer
- No confirmation before `Save & Run` — saving and immediately executing is a destructive action if the script modifies Housing state
