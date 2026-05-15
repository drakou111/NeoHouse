# NeoHouse — Context for Claude

> This file is written by Claude, for Claude. If you're reading this, you're a future instance of me who has forgotten the conversation history. Use this to get up to speed fast.
> Feel free to update the context file as you work on this project.
---

A lot more context can be found in README.md.

## What this project is

NeoHouse is a **NeoForge client-side Minecraft mod** that lets players automate the Hypixel Housing minigame by writing scripts in a custom language called **NHScript**. Scripts get compiled into an **action tree** that drives UI automation (clicks, searches, anvil inputs, etc.) to set up Housing items like functions, events, menus, and commands.

---

## Critical: where the real source lives

This repo uses **Stonecutter** (multi-version) + **Architectury** (multi-loader). The layout is:

- **`common/src/main/java/`** — **THE real shared source. Edit this for compiler, actions, factories, utils.** Compiled as `:common` module, included by both `:neoforge` and `:fabric`.
- **`neoforge/src/main/java/`** — NeoForge-specific wiring: `TickDriver`, `ScreenEvents`, `NeoForgePlatform`, `ClientCommandHandler`. Depends on `:common`.
- `fabric/src/main/java/` — Fabric-specific wiring. Depends on `:common`.
- `src/main/java/` (root) — **Stonecutter's VCS canonical template only. NOT compiled.** It's a flat merged view of all classes for Stonecutter version-switching. Editing it does nothing to the actual build.
- `versions/*/build/generated/stonecutter/` — auto-generated from root `src/` by Stonecutter. Build artifacts, NOT compiled. Confirming: `versions/1.21.11/build/classes/java/main/` is empty.

**The active compiled source is `common/src/` + `neoforge/src/`.** Always edit those for the 1.21.11 NeoForge build.

---

## Architecture overview

### Compiler pipeline (`src/.../compiler/`)

```
NHScript source
  → Preprocessor (resolves import "file.nh"; statements)
  → Lexer → Parser → MainNode (AST)
  → [visitor passes] (see NHCompiler.java for order)
  → CodeGenerator → Action tree
```

Key visitors in order:
1. `PlaceholderValidationVisitor` — validates placeholder names
2. `LinkFunctionVisitor` — links function calls to definitions
3. `RepeatUnrollingVisitor` — unrolls `repeat N { }` blocks
4. `DeclarationCollectorVisitor` + `ScopeCheckingVisitor` — scope/variable validation
5. `ReturnCheckVisitor`, `DeadCodeCheckVisitor` — semantic checks
6. `TypeInferringVisitor` — infers and annotates variable types
7. `StringLengthCheckVisitor` — rejects strings that would exceed 32 chars when expanded in Housing format
8. `ReturnContextChecker`, `ReturnPlacementVisitor` — return handling
9. `SimplifyingExpressionsVisitor` — constant folding
10. `FunctionCallLoweringVisitor`, `FunctionParamLoweringVisitor` — inline typed function calls
11. `RandomUnfoldingVisitor` — unfolds `random { }` blocks
12. `ActionCountSplittingVisitor` — splits blocks that exceed Hypixel's 25-action-per-block limit into helper functions, wrapping overflow in null-condition `ConditionalNode` shells
13. `ActionRestrictionVisitor` — validates action/condition placement rules
14. `CodeGenerator` — walks the AST and emits an `Action` tree

### Action tree (`src/.../actions/`)

Actions are executed tick-by-tick via `TickDriver.onClientTick` → `SequenceManager.tick()`. Key types:
- `GroupAction` — runs a list of child actions in sequence
- `SearchAction` — scans a container for an item by name, pages through if not found
- `WaitForAction` — waits for a screen to open or close (event-driven via `ScreenEvents`)
- `AnvilAction` — waits for anvil stability then types a name and clicks the output
- `CreativeGiveAction` — sends a `ServerboundSetCreativeModeSlotPacket` to give an item
- `CheckSlotEmptyAction` — branches on whether a slot has an item

### `requiresCreative()` propagation

Scripts that use items (menus, giveItem, removeItem, dropItem) require creative mode. This is enforced in two layers:

1. **Pre-compile text scan** in `ScriptRunner.sourceNeedsCreativeMode()` — checks for `AddAction` names that have `ITEM_LITERAL` params (giveItem, removeItem, dropItem) and for the menu slot pattern `slot\d+\s*:\s*\(`. Fast early feedback before compilation.
2. **Post-compile action-tree check** in `ScriptRunner` — calls `scriptAction.requiresCreative()` on the compiled action tree. More accurate.
3. **Per-tick runtime check** in `SequenceManager.tick()` — calls `currentAction.requiresCreative()` every tick, stops the script if the player loses creative mid-run.

`requiresCreative()` is defined on `Action` (returns `false` by default) and overridden by:
- `CreativeGiveAction` → always `true`
- `GroupAction` → `true` if any child returns `true`
- `SearchAction` → delegates to `onFound` / `onNotFound`
- `CheckSlotEmptyAction` → delegates to `ifEmpty` / `ifOccupied`

**Important:** Use `mc.player.getAbilities().instabuild`, NOT `mc.player.isCreative()`. On the client, `LocalPlayer.isCreative()` checks the server-reported `GameType` — on Hypixel Housing this is always `CREATIVE` for the owner even after `/gamemode survival`. The `instabuild` ability flag is what actually changes when the owner loses creative access. `ScriptRunner.isPlayerCreative()` and `SequenceManager.tick()` both use `instabuild`.

---

## Multi-loader rule

**Any UI or runtime feature must be implemented for BOTH NeoForge and Fabric.** Shared logic goes in `common/src/`. Loader-specific hooks (events, callbacks) go in `neoforge/src/` and `fabric/src/` respectively.

- NeoForge render hooks: `@EventBusSubscriber` + `@SubscribeEvent` on `RenderGuiEvent.Post`
- Fabric render hooks: `HudRenderCallback.EVENT.register(...)` in `NeoHouseFabric.onInitializeClient()`
- NeoForge tick hooks: `@SubscribeEvent` on `ClientTickEvent.Post` in `TickDriver`
- Fabric tick hooks: `ClientTickEvents.END_CLIENT_TICK` in `TickDriver.register()`

---

## Known gotchas

- **`Action` base class must declare `requiresCreative()`** returning `false`. If it's missing, any call to it is a compile error, the build silently fails, and the old jar runs — making the survival check appear to do nothing.
- **`CodeGenerator.generateConditional`**: null-condition `ConditionalNode`s (created by `ActionCountSplittingVisitor` to wrap overflow actions in a real Housing conditional block) must call `ActionFactory.addActionConditional(ra, false, null, thenAction, null, null)`, NOT inline the thenBlock directly. Inlining defeats the splitting.
- **`WaitForAction` race**: if the screen opens in the same tick as the click that triggers it, `ScreenEvent.Opening` fires before `watch()` is called. Fix: after registering, immediately check `mc.screen` and mark done if the condition is already met.
- **`SearchAction` stability**: don't page-turn based on a simple miss counter. Wait for the non-empty slot count to stabilize over `STABLE_TICKS` ticks first (container data may still be loading from the server).
- **`ScreenEvents` watcher leak**: `waitingActions` is a static set. Always call `ScreenEvents.unwatch(this)` in `WaitForAction.reset()`, and prune completed watchers in `onOpening`/`onClosing` via `removeIf`.
- **Command names are lowercased** at parse time in `Parser.parseCommand()`. This was a deliberate fix — Housing commands are case-insensitive but were being stored with original casing.

---

## Hypixel Housing limits (relevant to compiler)

| Limit | Value |
|---|---|
| Actions per block (VARIABLE context) | 25 |
| Conditions per block | 25 |
| String variable text | 32 chars (expanded) |
| Anvil text | 50 chars |
| Chat message text | 256 chars |
| Sign text | 15 chars |

Variable expansion sizes (for `StringLengthCheckVisitor`):
- `%var.player/name%` → 13 + name.length()
- `%var.global/name%` → 13 + name.length()
- `%var.team/name teamname%` → 12 + name.length() + 1 + teamName.length()
