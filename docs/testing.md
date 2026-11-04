# Testing Strategy

## Layers

1. **Unit tests** (`src/test`, JUnit 5, no Minecraft classpath): pure logic —
   aspect math, research graph validity (acyclicity, reachability), recipe
   data matching, cost calculations, persistence codecs, config handling,
   build/metadata wiring. Run by `./gradlew test` (part of `build`, in CI).
2. **Game tests** (`gametest/`, NeoForge `@GameTest`): real-runtime behavior —
   registration, block/entity behavior, crafting flows, vis flows, research
   progression, persistence across save/reload. Run by
   `./gradlew runGameTestServer`; CI asserts the "required tests passed"
   line explicitly (a green exit with zero tests is a failure).
   Dual-configuration matrix (TC-M8-01): the accessory suite runs under both
   provider backends — default (Curios on the runtime classpath) and
   `-Ptc.noCurios` (built-in fallback; separate CI job
   `game-test-no-curios`). `AccessoryGameTest` branches on the active
   provider; the shared contract must hold identically.
   Client-dist boot (payload registration and dist-safe handlers must also
   construct on the client) is smoke-tested manually with
   `xvfb-run ./gradlew runClient`: require the mod init line and
   "Sound engine started" with no errors naming mod classes, then stop it.
3. **In-instance integration** (manual/scripted): the disposable ATM10 8.3
   instance at `~/minecraft mods/atm10-test` (never the user's primary
   instance). Headless client via Xvfb/llvmpipe where no display is needed;
   results with log evidence recorded in `docs/atm10-compatibility.md`.

## Verification stages (from the project brief)

A BUILD → B LOAD → C CONTENT → D GAMEPLAY → E REGRESSION → F ATM10 → G RELEASE.
Each Kanban milestone names the stages it must pass; a task cannot complete
without the evidence for its stages.

## Host constraints (learned on this machine, do not relearn)

- Worker scopes have 4 GiB memory.max; run Minecraft under
  `systemd-run --user --scope` with generous limits.
- ATM10 client needs `-Xmx8G`; the pack's memorysettings mod blocks startup
  below ~5.9 GB heap with a modal dialog.
- Client+server simultaneously exceeds the 14 GiB host — multiplayer-join
  tests need the server stopped or a lighter pack; document honestly.

## Rules

- Never mark a feature tested because it compiles.
- Seeded tests for random processes where practical.
- Regression: identifier stability tests over the content mapping; previously
  fixed bugs get a guarding test.
- **Game test structures share a level and pack densely.** The game test server
  lays every test in a batch out on a grid whose spacing comes from the test's
  *template size*: the `empty` template (1×1×1) packs structures ~6 blocks apart
  in x and ~25 in z, so probes run concurrently in the same 25-block
  neighbourhood. Two failure modes follow, both seen on this repo: a neighbour
  can `setBlock` on top of the cell another probe just built (a crucible probe
  found its `blockMetalDevice` replaced by a neighbouring probe's arcane lamp),
  and a probe whose logic scans a wide box reads the neighbour's blocks
  (`InfusionMatrixLogic.scan` covers 25×16×25, so a neighbour's pedestals
  inflate `symmetry`). Writing MORE probes shifts the grid and moves the
  collision, so a green 91-test run does not protect the 94-test run.
  Two conventions handle it: a probe that owns a machine claims its own padded
  cell (`template = "gametest_pad_32"`,
  `data/thaumcraft/structure/gametest_pad_32.nbt` — places nothing, only
  widens the packing footprint), and a probe whose assertions depend on a world
  scan measures DELTAS over an ambient baseline scan, never absolute totals.
  Prefer both over reordering tests: the layout is not stable across runs.
- A `@GameTest` body that touches a block entity must not park the server
  thread. The game test server shuts down by unloading chunks
  (`LevelChunk.clearAllBlockEntities` → `BlockEntity.setRemoved`), and a
  `setRemoved` that reads `Level.getBlockState`/`getChunk` deadlocks
  `BlockableEventLoop.managedBlock` on the worker performing that unload — the
  server hangs at *Saving worlds* and `runGameTestServer` never exits. Read
  `Level.getBlockStatesIfLoaded` (hasChunksAt-guarded) in removal hooks.

- **A game test gets 100 ticks by default.** The 1.21.1 `@GameTest` budget attribute is `timeoutTicks` (default 100; there is no `seconds`, `ticks`, `boundsSize` or `boundsX`). A probe that measures a multi-tick machine reports `Didn't succeed or fail within 100 ticks` and reads as a hungry bug: the arcane furnace cook is 140 ticks cold, so `theFurnaceSmeltsAnItemAndEjectsTheResult` needs `timeoutTicks = 400`. Raise the budget in the probe, do not shorten the machine.
- **A template `.nbt` must be the cube the probe asks for.** A file under `data/<ns>/structure/` that is a stub fails at structure load with `NbtFormatException: template 24x24x24 is not a cube`, and that kills every probe sharing the batch — probes that never ran read as red content bugs. When `runGameTestServer` reports failures in tests that only place blocks, check the structure loading lines in `run/logs/latest.log` first.
- **1.21.1 `GameTestHelper` API shape, measured against the mapped jar (no guessing):** `destroyBlock(BlockPos)` has no drop flag — for drops use `Level.destroyBlock(pos, true, null)`; `InteractionResult` is the vanilla enum so assert `consumesAction()` (no `isAccepted()`, no `isAccepted()`-style helper); the tick hooks are `runAtTickTime(long, Runnable)`, `runAfterDelay(long, Runnable)`, `succeedOnTickWhen(int, Runnable)` and `onEachTick(Runnable)` with `getTick()` (there is no `runAfterTick`, `runEachTick` or `getTickCount`); `makeMockPlayer(GameType)` gives a `Player` whose inventory is writable.
- **`helper.useBlock` never runs an item's `useOn`.** It calls the block's use path, and a block that returns `PASS_TO_DEFAULT_BLOCK_INTERACTION` for that stack means the item code never executes. Drive the hook the server uses: `stack.useOn(new UseOnContext(level, player, hand, stack, hit))` with a mock player and a `BlockHitResult`. Two 1.21.1 inventory facts come with it: `Inventory.add` reuses the slot a spent stack just vacated, so 'the held stack is now empty' is NOT observable from the hand — assert the inventory count — and a mock player's off-hand stack passed to a tile's `onItemUseFirst` path is written by that path, not by the item.
- **A modded block drops nothing unless it has a block loot table.** 1.21.1 has no implicit self-drop (vanilla's 'drops itself' is a generated table), so a `getDrops` override that defers to `super.getDrops` — the original's 'the block drops itself' case — yields zero items. Ship `data/<ns>/loot_table/blocks/<path>.json` with one pool per variant and a `minecraft:block_state` component so the variant survives the drop (`blockjar.json`, `blockcosmeticslabstone.json`). Give every pool a `block_state_property` condition: an unconditional pool fires for every variant and duplicates the drop.
- **Re-placing identical state does not rebuild the block entity.** `setBlock` with the state that is already there keeps the old tile (its inventory, its fields), so a probe that wants a fresh machine uses a fresh cell, not the cell a previous leg broke. Same reason a probe must not read a tile it placed in an earlier leg of the same test.
- **Never hand a null `Entity` to 1.21.1's `ClipContext`.** The `(Vec3, Vec3, Block, Fluid, Entity)` constructor routes through `CollisionContext.of(entity)` into `EntityCollisionContext`, whose constructor calls `entity.isDescending()` — a guaranteed NPE with null, and inside a world driver that NPE kills the game-test batch that shares the level. A ray with no entity shape wants `CollisionContext.empty()`.

## Standing acceptance gates

**`PlayableAcceptanceGameTest` is the PLAYABLE gate** (TC-M9-01-s, `t_8ea70fda`). The 2026-10-30 intake asked
for three observable behaviours — ores drop items, items appear in JEI, items are searchable in creative. The
three per-card probes (`OreDropGameTest`, `CreativeTabGameTest`, `ItemModelCoverageGameTest`) each went green
while the intake as a whole was unmet, because nothing asserted the three **together**. The last probe of this
class (`theplayableintakecomposesintooneverdict`) runs the ore leg, the tab leg and the name/model leg in one
fresh world and aggregates every failure into a single list, so "is the mod playable?" is a red/green the
suite reports, not a line in a release note. Four rules it encodes:

- **The expected set is derived from the live registries at test time.** Tab membership is compared against
  `BuiltInRegistries.ITEM` filtered by `ModCreativeTabs.isShippedCatalogItem`, and every leg asserts that its
  derived set is non-empty *before* comparing: a leg whose expected set is empty passes by construction, and
  that is the trap a hardcoded expected list sets every time a catalog card registers a new item.
- **Breaks go through the shipped dispatch, never an internal method.** No-tool legs use
  `Level.destroyBlock(pos, true, null)`. Tool legs use `Block.playerDestroy(level, player, pos, state, null,
  tool)` — the call `ServerPlayerGameMode.destroyBlock` makes with the player's main-hand copy — because
  `destroyBlock` hands `dropResources` an `ItemStack.EMPTY` tool, so `match_tool` (silk touch) and the fortune
  term are only observable through the tool-carrying half of the same vanilla dispatch.
- **A drop is identified by identity, not by position.** Structures share one packed level, so a neighbour's
  dropped stacks land in the sweep box (measured: a vanilla `stone_button`): filter to the ids the loot table
  can emit, log the foreign stacks, and never assert on raw box contents.
- **The gate does not depend on JEI, on an asset overlay, or on a client.** JEI lists registered items with no
  plugin code, so the machine-checkable form of "in JEI" is tab membership + a resolving display name + a
  model JSON; the probe reads `en_us.json` and the model tree off the classpath, self-disables nothing, and
  passes with no overlay installed (D-004 code-only release) and with no JEI jar in the test instance.

**Mutation-proof recipe** (a gate that has never been seen failing is not a gate): move
`src/main/resources/data/thaumcraft/loot_table/blocks/blockcustomore.json` out of the tree → `./gradlew
runGameTestServer` **exit 1**, "8 required tests failed" (this gate's 3 ore probes + the 5 `OreDropGameTest`
probes, all zero-drop); put a bare `return;` at the top of `ModCreativeTabs`' `displayItems` lambda → **exit
1**, "4 required tests failed" (this gate's 2 tab probes + `CreativeTabGameTest`'s 2). `git checkout --`
restores both and `diff -q` reports byte-identical.

