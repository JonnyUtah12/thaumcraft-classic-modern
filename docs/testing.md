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
- **A probe must not argue coverage from a single seeded `RandomSource` stream.**
  `RandomSource.create(long)` is not a fresh generator: `RandomSourceSupport.create`
  → `LegacyRandomSource` → **`java.util.Random`** — `seed = (seed ^ 0x5DEECE66D) &
  (2⁴⁸−1)`, stepped `seed * 0x5DEECE66D + 0xB (mod 2⁴⁸)`, with output taken from the
  **top** bits. A fixed low-entropy seed therefore leaves the stream correlated for a
  long run, so "N seeded rolls" is much weaker evidence than N rolls looks like.
  Measured on this repo: a taint probe asserted 12 of 16 palette cells cleansed from
  2500 seeded draws; the cells in the `−7…−5` band never fired, and the probe read as
  a **product** defect through two cards before measurement root-caused it
  (`8b30a0d`, documented `287e1cf`). The behaviour was correct throughout.
  So: assert the **set of outcomes actually drawn** — never a count, range, frequency
  or variety derived from one stream — and make the failure message name the outcomes
  that were never reached, so the next reader sees which branch is untested
  (`a9f90c3`). Exemplar: `TaintCleansingRollCoverageTest`. The pre-existing good
  pattern is the opposite shape: sweep the seed domain and assert per-seed bounds —
  `NodeGenerationRollTest` (seeds 0–499), `StructureParityTest` (seeds 0–19,999).
  **The asymmetry is the whole point.** The taint case was a **false RED**:
  self-announcing, CI goes red and someone looks. A seeded stream that never reaches
  the branch it claims to cover is a **false GREEN** — it passes forever and nobody
  looks. That is this file's own rule above, *"never mark a feature tested because it
  compiles"*, reached by a mechanism nobody chose.
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
- **A probe that lets the LEVEL tick its mob must place that mob in the chunk the harness vouches.**
  `StructureUtils#prepareTestStructure` calls `forceLoadChunks` on the **template's bounding box**,
  and `GameTestInfo#tick` refuses to start a probe until every one of those chunks answers
  `isPositionEntityTicking` — that box, and only that box, is a guaranteed entity-ticking area.
  `empty` is 1×1×1, so its vouched set is ONE chunk: a mob placed one block off the structure
  origin (the `(1,3,1)` cell) lands in a neighbour the harness never vouched whenever the structure
  block falls on a chunk's last row or column. WHICH draw that is MOVES with the batch, because
  `GameTestServer#startTests` draws the whole grid's origin from `Level#getRandom` inside
  `TEST_POSITION_RANGE` — a uniform draw of 16 alignments per axis, so a `(1,3,1)` cell leaves the
  vouched chunk on 1/16 per axis, ≈12% of draws, and adding probes (which shifts the grid) shifts
  the odds.
  Measured on this tree: the `furiouszombieangerdecaysandtheboxshrinksback` tick-list detector went
  red 2/2 in the **with-Curios** CI job while the same sha was green locally, and a probe with the
  mob pushed one chunk out (`+16`) reported `tickCount 0`, `cellEntityTicking=false`,
  `originChunkInRange=true`, `align16=15,5`. The tick list is the ONLY thing at stake: an entity in
  a hidden section stays in the level's entity lists, so block scans, `getEntitiesOfClass` and
  `getEntities(type, pos, radius)` all still see it — which is why a probe that never ticks looks
  fine on every other arm. Fix the CELL, not the assertion: put the mob on the structure block's own
  (x, z) (`ZombieMobGameTest.MOB_CELL = (0,3,0)`), and assert the cell is entity-ticking BEFORE
  reading a per-tick counter, so a harness/layout change reports the precondition by name instead of
  arriving as a rate that cannot be measured.
- **A mob probe owns the footing its contract rests on; terrain the harness can fill is not a start cell.**
  `GameTestServer#startTests` draws the grid origin from `Level#getRandom`, so the natural ground under a
  probe's mob moves with the batch, and a mob spawned **inside** a solid cube does not merely look odd: it
  takes a damage per tick and trips the mob's own in-wall teleport. `EntityGolemBase#tick` runs
  `if (!level.isClientSide && isInWall())` → `teleportToHome()` → `findSafePosition(...)`, whose 3×3×3
  scan is ordered **dy +1 first, then 0** and dx/dz from −1, and stops at the **first** non-suffocating
  cell. With the spawn cell solid and the surface at y+1, that scan's first hit is the cell **adjacent to
  the probe's target** — the only free cell of the scan — and a goal whose click site is its `tick()` dies
  there: `AIUseItem#tick` right-clicks only at `distanceToSqr(target) <= 4.0`, and `tick()` runs only
  while `canContinueToUse()` holds, which ends on `!navigation.isDone()`; 1.21.1's `Path#isDone()` is
  `nextNodeIndex >= nodeCount`, so a walker spawned next to its target gets a **one-node path** that
  `PathNavigation` marks done on the first navigation tick — 2 of them per probe tick — and the click
  never happens. Measured (t_67f61f59): the red leg on `f18e71f` carries the fingerprint in its **own**
  message (`keep/t752/after_c_3.log`:984) — the golem's **feet at (6,0,4)**, one cell from the probe's marked
  cell `(7,0,4)`, with `home=(5,0,4)` (the spawn cell it was teleported out of), `navDone=true`,
  `tickCount=344`. The probe carves air at exactly `(6,0,4)` and `(7,0,4)`, so on a draw where the pad's
  natural ground fills that row at y=0, the in-wall scan's only free cell **is** the marker's neighbour. On
  `8bcb3d3` the same probe drew AIR at the feet cell in 25/25 sampled legs and went red **0/30** — a terrain
  draw, not a race. Fix the CELL, not the assertion: fill the row the mob walks on
  (`setBlock(x, -1, 4, STONE)` plus AIR at y=0 and y=1), **assert those cells where you set them**, and
  latch the goal's own precondition — the mob starts **outside** its click sphere, the original's literal,
  not a probe-chosen radius — and report it in the poll's first assert. Proven by four mutations, each of
  which goes red naming its own gate: a solid cube at the mob's feet → the footing assert; a spawn at the
  adjacent cell → "activated with the golem INSIDE its own click sphere (`feet=6,0,4 … d2=1.25`)"; the
  product's own `dist <= 4.0` → `dist <= 0.0` → "activated at the marked cell … **never placed** the carried
  block (`starts=106 ticks=4 stops=106`)"; the marked face `UP` → `NORTH` → "**never activated**". The fixed
  probe then passes **12/12** with-Curios and **12/12** no-Curios legs (one of those 24 batches was red on
  an unrelated probe, `combat_butchercullstheoldestanimalonlyinaherdofthree`, whose golem was 12 blocks from
  the cell its probe pins — same family, flagged for its own card).
- **Keep a game test's failure message inside the writable_book page budget.** On a red probe,
  `ReportGameListener#createBook` joins the test name, `-------------------\n` and the failure message into
  **one** `minecraft:writable_book` page placed on a lectern, and that page's codec is
  `WritableBookContent.PAGE_CODEC = Filterable.codec(Codec.string(0, 1024))`. A longer page throws in
  `ItemStack#save` at **chunk save** — `Util: Error saving [1 minecraft:writable_book]` plus a
  `LecternBlockEntity` "exception trying to write state" — so the run reports a serialization stack and the
  diagnosis is gone. Measured (t_67f61f59): the red leg's page was **2734 chars** (`after_c_3.log`:984-997),
  and the payload starts with the probe's own name, which reads like a product encoding bug and is not — it
  is the diagnosis overflowing. A 58-char probe name plus the 20-char separator leaves ~945 chars, so
  `cap(…)` long probe messages at **900** and put the mechanism in the **short** asserts that fire first:
  5 legs built red on purpose with a capped message produced **0** `Error saving [1
  minecraft:writable_book]` lines, against 1 in the uncapped red leg.
- **A probe asserts an OUTCOME, never a tick budget.** `helper.runAtTickTime(getTick() + N, …)` puts the
  harness's scheduler inside the port's contract: the assert fires on the **probe's** clock, while the
  fact it reads is produced by the **LEVEL** — an entity's AI tick, a chunk's block-tick pass — and
  `GameTestInfo#tickInternal` gives no contract about how much of that work lands inside N probe ticks
  (it fires the `runAtTickTimeMap` on `tickCount`, and only then advances the sequences). Nothing in the
  harness says "the mob has ticked by probe tick N"; that number is a guess about the batch, and
  `63b5806` measured the same tree's probe stamps moving by millions of blocks between runs. Measured on
  this repo: `firebatFlagBitsMatchTheOriginalByte` at `+4L` flaked in CI on the same code that passed
  locally, `furiousZombieAngerDecaysAndTheBoxShrinksBack` at `+5L` went red **2/2** in the **with-Curios**
  CI job as `tickCount 0` while the same sha was green locally, and `theFurnaceSmeltsAnItemAndEjectsTheResult`
  asserted that a **140-tick** cook had begun by test tick 8. The shape that cannot lose to a schedule is
  the outcome poll: `helper.succeedWhen(() -> { …asserts… })` — the probe runs the asserts **every tick**
  until they pass, and `timeoutTicks` survives as a **watchdog with large margin**, not as the contract.
  Four harness facts make the shape safe to write, all measured against the mapped sources:
  `GameTestHelper#ensureSingleFinalCheck` throws `IllegalStateException("This test already has final
  clause")` on a **second** `succeedWhen`/`succeedIf`/`succeedOnTickWhen`, so a probe gets exactly **one**
  final clause — latch multi-fact evidence in `onEachTick` and assert the latches inside that one poll;
  a latch must be **monotone and false on tick 0** (set once, never cleared), which is what stops a poll
  from succeeding vacuously on the probe's first tick; `GameTestSequence#tickAndFailIfNotComplete` fails
  the test with the **condition's own assertion message** at the watchdog, so the diagnosis survives the
  conversion — name the precondition in the poll's **first** assert (the vouched-cell check above), so a
  layout change reports the precondition and not a downstream rate; and `GameTestSequence#tickAndContinue`
  swallows **only** `GameTestAssertException`, so a poll body that throws anything else escapes into the
  level tick and takes the batch with it.
  Two shapes are **not** this class and must not be swept: a leg that **feeds its subject its own
  `tick()`** is self-contained and runs inline — no window, no poll (`ZombieMobGameTest.furiousZombieAngerGrowsOnDamageAndRewritesItsDamage`)
  — and a `runAtTickTime` that only **schedules a spawn** with no assertion on a fixed tick carries no
  window at all (the golem probes' `runAtTickTime(6, () -> spawn(…))`, which then gate on the golem's own
  `tickCount`, never on a test-tick phase). Convert the assertion, not the bookkeeping.
  Exemplars: `63b5806` (firebat devil-health delta), `wt/t_f19061fc` (the zombie trio and the furnace cook).
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
