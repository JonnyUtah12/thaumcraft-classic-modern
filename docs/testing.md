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
  lays every test in a batch out on a walk, not a rectangular grid:
  `StructureGridSpawner` advances the next slot by `template width + 5` in x AND
  `template depth + 6` in z for each structure it places, and `TestGenerator`
  restarts the walk at the batch corner every `maxPerBatch = 40` tests (the batch
  also clears space for the previous batch's box). So a 1×1×1 `empty` neighbour sits
  6 blocks away in BOTH axes, a 24³ `empty_24` neighbour advances 29 in each and its
  cell edge reaches to 5 blocks from the next cell's origin, and probes run
  concurrently in the same 25-block neighbourhood. Measured on this tree (283 tests,
  grid origin 2011583, -60, -4860956): two probes packed next to each other in the
  batch order landed at (+29, +68) and (0, +249) from one another. Two failure modes
  follow, both seen on this repo: a neighbour
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
- **A probe whose verdict is about item entities must attribute them, not just
  filter them.** A scan over a box wider than the probe's cell reads the
  neighbourhood — `mobBox` spans ±12 (25³) and a 1×1×1 `empty` neighbour sits 6
  away, so a foreign stack in the box is the normal case, not the exception.
  Filtering by item identity does NOT fix it where the contract IS the item set
  (a drop-table probe): the stack a defect produces is the probe's own item, so an
  identity filter is blind to exactly the bug it is guarding. Attribute by capture:
  scan the box immediately before and immediately after each action that produces
  entities and keep the UUID delta. The server thread is single-threaded and an
  action is not re-entered, so the delta is that action's output — no clock, no
  position, no draw enters the verdict. Foreign stacks stay in the failure message
  (they are the diagnostic) and are never counted: `ZombieMobGameTest.DeathDrops`,
  `assertOwnedDeathDropsAre`. Measured as CI run 42573819914 (sha 1430745):
  `furiouszombiedeathdropsdoubletherollsandthestack` failed on
  `minecraft:stone_button` at grid origin 12969321, -60, -6539840 while the same sha
  was green in 42573817477 — `GameTestServer#startTests` draws the origin from
  `Level#getRandom`, so an unfiltered pad scan is a draw bet. The item is not
  producible by that probe's drop bodies: `stone_button` appears 0 times in
  `src/main/java`, in no vanilla structure NBT and in no vanilla loot table, so it is
  not terrain loot; the project's only `stone_button` is the ported essentia entry
  `data/thaumcraft/essentia/tags.json` `itemTags[195]` (`machina 1`), which a
  neighbouring machine probe can eject into the box.
  Green is not proof: pair the arm with a standing foreign stack in the box on the
  producing tick (`ZombieMobGameTest.breakForeignBlockInPad`) plus a self-check that
  the fixture is still landing (`aForeignStackInAPadIsNamedAndNotCounted`), and sweep
  the mutation surface (the drop tables) to show the probe goes RED. The
  identity-filtered `ownedBy` pattern (`OreDropGameTest`) is the right tool only where
  the contract names the item and the mutation surface is the count.

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
- **A goal left on a live selector is armed by the ENGINE, and a route gate closes off the ground.**
  `Mob.serverAiStep` ticks the goal selector and `GoalSelector#tick` starts a goal on the
  `(tickCount + id) % 2` parity tick (`GoalSelector.java:74-105`). A probe that hand-drives a goal's
  `canUse` measures its registration priority and then takes the goal OFF the selector: left on it,
  the engine arms it first — on a parity the entity id decides, so the loss is a rate and not a
  constant — and the goal's own `start()` movement is the state the probe goes on to sample. The
  paired wall is the ground: `GroundPathNavigation#canUpdatePath` is
  `onGround || isInLiquid || isPassenger` (`GroundPathNavigation.java:40-43`) and
  `PathNavigation#createPath` answers `null` while it is false (`:136`); that same method answers
  `null` when the target's chunk is not loaded (`:44-49`). Both walls print as the same "no route"
  line, so a gate that reads a route asserts BOTH preconditions where it can read them and names
  them in its own message. Measured on this tree: `combat_golemFleesOnlyACreeperWhoseFuseIsLit` went
  red on ~1 leg in 5 (`-Ptc.noCurios`) with `nodes=null, lastNode=-` across all 480 `canUse` draws,
  and the standing hypothesis — the flee spot reaching a chunk the harness does not vouch — measured
  FALSE: a sweep of the flee domain on the phase ticks of 8 legs found every chunk column loaded
  (36 of 36) on every sample, while `onGround=false` closed 10 of 10 sampled flee routes on all 8
  airborne samples and `onGround=true` left 10 of 10 open on all 18 grounded samples. The engine had
  armed the goal on the tick after the fuse (about one entity id in two) and the goal's own
  `motionY` 0.3 hop (`AIAvoidCreeperSwell.java:74-79`) put the golem airborne and outside its own
  detection box. Fix the PRECONDITION — goal off the selector, preconditions asserted at the fuse
  tick with the `align16` and chunk facts — and keep the detector loud: the mutation that puts the
  golem airborne at the fuse tick turns the probe red naming `canUpdatePath`/`onGround`, and the
  mutation that holds it airborne through the window reproduces the original `nodes=null` red
  verbatim, now reported with `onGround=false, canUpdatePath=false, loaded=true,
  fleeDomainUnloaded=0`. Tick order decides mutations: `onGround` is recomputed in the physics step
  at the END of a tick, so a `setDeltaMovement` applied in the same tick as the probe's draws is
  invisible to those draws — an airborne mutation has to be established one tick early to be seen.
- **A hand-driven `tick()` runs the FULL goal selector, and the goal's gate reads the world.**
  `Entity#tick` → `LivingEntity#tick` → `Mob#serverAiStep` → `goalSelector.tick()`. The
  `(tickCount + id) % 2` parity gate does NOT shelter a hand-driven tick: `tickCount` is advanced
  by the LEVEL (`ServerLevel#tickNonPassenger`), never by `Entity#tick()`, so a probe entity sits
  at `tickCount=0`, the `tickCount > 1` guard is false, and the engine arms a goal on the live
  selector inside the probe's own tick on EVERY draw (measured: 40 ids of both parities in one leg,
  all armed). `GoalSelector#tick` then ticks the goal it has just started in the same call, so the
  goal's `update` writes the state the probe goes on to read — and what closes that state is world
  state: `SwellGoal#tick` feeds `swellUpdateState(target alive, distSqr,
  getSensing().hasLineOfSight(target))`, and the LOS arm is a block clip (`Sensing#hasLineOfSight` →
  `LivingEntity#hasLineOfSight`), so a grid draw that lands the cell in generated terrain writes
  `-1` and the fuse reads 0. Measured on `104ccce` (56 legs, per-leg sampler at the asserted tick):
  `sensingLos=true` 55 legs / 0 reds, `sensingLos=false` (cobblestone in the mob's own eye cell)
  1 leg / 1 red; parity 1 29 legs / 0 reds, parity 0 26 legs / 1 red — **the entity-id parity
  hypothesis measured FALSE**, so a rate is not proof of parity. Fix the PRECONDITION the same way
  as above (measure the priority, `removeGoal`), and stand the world arm in the pad
  (`setBlock` in the eye cell) so both arms of the gate are asserted, not drawn; clear the per-id
  `Sensing` cache (`getSensing().tick()`) when the pad changes, because `Mob#serverAiStep` clears it
  at the top of every AI tick and a stale `unseen` entry reads a block that is no longer there.
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
