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
   line explicitly (a green exit with zero tests is a failure). The assert
   greps the **per-run** sources — the job's own gradle console output
   (`run/gametest-console.log`, tee'd by the game step) and `run/logs/debug.log`,
   the complete per-run log — and never `run/logs/latest.log`: log4j rolls that
   file on the server **shutdown** path, so the file sitting at that path when
   the assert runs can be the post-rotation one with no summary line in it
   (measured: run 43662060834 on `6a05ba14`, `game-test-no-curios` attempt 1 —
   `All 502 required tests passed :)` and `BUILD SUCCESSFUL in 57s` in the same
   job, no probe failed, assert step RED; the same sha green on the re-run).
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
<a id="attribute-not-filter"></a>
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
  the contract names the item and the mutation surface is the count. Measured a third
  time on a `dropAllLoot` seam: `theritualclosesthelootinggate` — a 40-call mid-ritual
  "nothing appears here" arm over an `inflate(8)` box — went RED on **1 of 4**
  with-Curios legs of tree `258bf0fb` and 0 of 3 of main `7b430bb1`, naming
  `an item appeared on the pad: [1 minecraft:wheat_seeds @(4031868.125, -60.0,
  1.2463378958683364E7)]` (land runner t_5bf13a7d, `v3/leg_curios_1.gradle.log`): a
  draw on the grid origin, the same shape as the `stone_button` and tube readings
  above, and a rate a green streak cannot see. `CultistClericGameTest.plantForeignStack`
  is the twin of `breakForeignBlockInPad` with one difference: the planted stack goes in
  the probe's **own column**, not out in the inflated box — a stack planted out there
  reaches a neighbour's cell and hands the next probe this exact flake.

<a id="vouched-cell"></a>
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
<a id="entity-clock-poll"></a>
- **An arm that waits for an outcome the ENTITY'S CLOCK produces starves on that same vouch wall — and
  `return` is not a poll.** The bullet above reads a per-tick counter; the harder shape is a poll that
  waits for an outcome the entity's own clock produces, e.g. a lifetime clamp (`EntityEldritchOrb#tick`:
  discard once `tickCount > 100`, and the burst writes `tickCount = 100` at :84). The age moves only in
  `ServerLevel#tickNonPassenger` (`tickCount++` then `tick()`), so on an unvouched chunk it never crosses,
  the assert throws on every retry, and the probe dies at the watchdog naming the frozen state — measured
  as `theOrbBurstStrikesTheLivingAndSparesTheUndead` → `age=100 removed=false` (CI run `43634031237` on
  main `046f3d32`, with-Curios `build` job; the parent card measured the same on a clean pad, 1 leg in 11,
  and 36 local legs produced 0, so a leg sweep is not a denominator for this class). The wall reproduces
  on demand: in a throwaway worktree release the pad's FORCED ticket with the harness's own batch-edge call
  (`ServerLevel#setChunkForced(x, z, false)` — what `GameTestRunner#testCompleted` runs over
  `level.getForcedChunks()`). Measured that way on the landing tree: **8 of 10** legs red with that
  byte-exact line; **0 of 10** on the fixed arm, census `ticksDrivenByProbe=1 age=101`. Two shapes close
  it, and the card ships both: put the fixtures on the vouched cell (above), and drive the owed entity
  tick from the probe's clock ONLY on the ticks where the server left the clock still (`tickCount++` then
  `tick()`), counting the driven ticks — the arm ends on the outcome, so nothing is pinned to N real ticks
  (the tick-budget bullet below), `driven = 0` on a vouched pad, and `driven = 1` is the signature of a
  vouch loss; both facts ride the poll's census line AND the assert message, so the wall reports itself
  instead of arriving as a rate.
  The harness fact that turns the tempting shape into a silent pass: in 1.21.1 `succeedWhen(criterion)` is
  `createSequence().thenWaitUntil(criterion).thenSucceed()`, and `GameTestSequence#tick` removes an event
  only when it runs WITHOUT throwing (`tickAndContinue` retries only a `GameTestAssertException`). A
  criterion that RETURNS early consumes the event, runs `thenSucceed`, and passes the probe with its
  asserts NEVER executed. `CultistClericGameTest.theOrbLifetimeClampDiesAtSixteenForPlainAndTwoFortyForRed`
  WAS on that path — its "has not crossed the edge: poll again" `return` arms — so a mutation that deletes
  the golem orb's `discard()` left the 502-probe suite GREEN (`046f3d32` + that mutation, one leg, card
  t_43022b00; again on `89b2a31d`, leg `preteethoff`), while the same mutation on the orb burst probe
  turns that one RED through the poll. It is OFF that path now: every non-final clause throws
  `GameTestAssertException` with the observed age/edge/vouch/driven, and the mutation reddens it by name
  (card t_d29d6aa8). The assert is the poll; never `return` out of a final clause.
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
<a id="removal-age-names-mechanism"></a>
- **A removal is only evidence when its age names the mechanism.** A probe that ends on "the entity is
  gone" passes on ANY removal, and the pad itself hides one: the `empty` template's origin column carries
  the harness's marker pair — `structure_block` in the corner cell and a **full-cube `jigsaw`** one above
  it — so a projectile armed at the corner stands INSIDE a solid block, and `Projectile` answers its
  first MOVING tick with a `BLOCK` hit; `EntityGolemOrb#onHit` (`:63`) answers that with `discard()`.
  Measured with the orb's lifetime clamp mutated off, by a throwaway probe logging every hit, the removal
  reason and the block states (card t_d29d6aa8, leg `diag_1`): `onHit type=BLOCK pos=…`
  `Block{minecraft:jigsaw}` and then `REMOVAL observed: age=162 reason=DISCARDED`. The clamp probe that
  armed its pair on that column went RED on the plain-edge mutation for the WRONG reason (`age=162
  removed=true` on the pair assert) and GREEN on the red-edge mutation — a real removal at the wrong age
  standing in for the clamp. The shape that closes it: put the fixtures in cells the pad leaves AIR and
  ASSERT them air, so a block landing there reports itself as `PROBE BROKEN` instead of arriving as a
  rate, and pin the age AT the removal to `edge+1` — the clamp fires on the first entity tick past the
  edge and a removed entity stops ticking, so `161` plain / `241` red is the clamp's signature and no
  other path writes it. Every edge's mutation then goes RED by name and by the right clause: on the fixed
  arm, clamp off → `PLAIN EDGE … age=461 removed=false`, plain edge off → the same with `ageRed=241`
  beside it, red edge off → `RED EDGE … age=540 removed=false`, edges swapped → `PLAIN EDGE MECHANISM
  plainAge=241 pin=161` — one leg each, four of four RED, each naming the probe (card t_d29d6aa8).
  Rules bullets here are cited **by anchor**: the cited bullet carries `<a id="<kebab>"></a>` on
  the line directly above it and every cite reads `docs/testing.md#<kebab>`, so an insertion
  cannot rot a cite — mint the anchor in the same commit as the first cite of a bullet, lowercase
  kebab. New bullets keep appending at the END of the section (the lane's standing diff-clean
  rule); with anchors no re-point is needed. History of the line-number scheme this replaces:
  main's bullet at `caef6cc7` shifted the `:123` / `:172` / `:192` cites by 8, and this bullet
  moved everything below `:209` by 19 (line-number cites, all converted by card t_867b394f). The
  one-command audit prints every cite with its TARGET line, exits 1 on a missing anchor, and
  marks any remaining `docs/testing.md:NNN` cite `LEGACY[...]`: `python3 scripts/check_docs_cites.py`
  (see "Cite audit" at the end of this file).
- **A route-OUTCOME gate goes red as a rate, on the leg that loads more.** Measured on this tree: the   scribing-ink family landing (`2fefa9b`, over main `104ccce`) ran `runGameTestServer` red ONCE in 4   legs on `combat_golemopensarealdoorandclosesitbehindit` — "the route reaches the far side with doors   openable: `path[1,reached=false]`" — with `All 328 required tests passed :)` on the `-Ptc.noCurios`   leg (rc 0) and on both re-runs of the with-Curios leg (rc 0, rc 0). `GolemCombatAiGameTest.java` is   byte-identical to main's tip (`b3bc00a6`), the landing's 27 paths contain zero golem/door/entity   files, and the red leg printed the landing's own probes passing (`[TC-M3-04-s-vi] Scribe2 craft:`,   `[TC-M3-04-s-iv] KnowFrag craft:`). `reached=false` over a node list that EXISTS is the ground /   tick-order wall above, not the change under test: read the failing probe's NAME before diagnosing,   then re-run the leg before touching code.
- **A tick-budget-coupled probe goes red when a HEAVIER probe joins the suite — the suite's size is not the trigger, the added probes' weight is.** Measured on this tree: `bossroombuildsthroughtheseam` (`gametest/OuterLandsGameTest`, byte-identical to main, 0 diff lines) went red on **10 of 10** `runGameTestServer` legs on the `t_79eea20e` tree (**331** probes) and green on **5 of 5** legs on main's tip (**328**) and on **2 of 2** legs of main's tip with **three NO-OP probes added (also 331, `CtlThreeProbeGameTest`, deleted after the run)** — same host, warm caches, legs run singly and in pairs. So the suite size (328 → 331) is exonerated by the control, and the trigger is what this card's probes *do*: the probe builds the maze at cells (40,40)/(44,44) in the SHARED game-test level and then counts `blockEldritch` and eldritch stairs in the ABSOLUTE box `x 632..664, y ROOM_Y..ROOM_Y+16, z 632..664`, and the doorway pass that fills it is budgeted in ticks — the sweep has to have drained when the assertion runs, so a probe that occupies ticks ahead of it (this card's probes sweep the 201-entry research graph and resolve rows out of the live `RecipeManager`) starves the pass and the leg reports "the boss-room doorway placed no blockEldritch in the cell envelope". `required = false` does not help: the probes still run and still occupy ticks. This is main's own rule from `554ac56` — assert the outcome, not the tick budget — applied to a probe that does not: the fix is for the probe to poll its envelope on ticks with a deadline (or build in a cell window it owns), not for the joining card to thin its probes. Same family as the golem's inherited start cell (`t_67f61f59`). Until that probe is fixed, any leg whose added probes are heavy should expect this red name, and it is a probe-isolation gate, not a product regression: CI at the landing tip `f4bac9e` runs the same suite green (`build` + `game-test-no-curios` + `runServer`).
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
<a id="outcome-not-tick-budget"></a>
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
  conversion — name the precondition in the poll's **first** assert (the vouched-cell rule, docs/testing.md#vouched-cell), so a
  layout change reports the precondition and not a downstream rate; and `GameTestSequence#tickAndContinue`
  swallows **only** `GameTestAssertException`, so a poll body that throws anything else escapes into the
  level tick and takes the batch with it.
  Two shapes are **not** this class and must not be swept: a leg that **feeds its subject its own
  `tick()`** is self-contained and runs inline — no window, no poll (`ZombieMobGameTest.furiousZombieAngerGrowsOnDamageAndRewritesItsDamage`)
  — and a `runAtTickTime` that only **schedules a spawn** with no assertion on a fixed tick carries no
  window at all (the golem probes' `runAtTickTime(6, () -> spawn(…))`, which then gate on the golem's own
  `tickCount`, never on a test-tick phase). Convert the assertion, not the bookkeeping.
  Exemplars: `63b5806` (firebat devil-health delta), `wt/t_f19061fc` (the zombie trio and the furnace cook).
  Quantified on this tree (`ZombieMobGameTest.angryZombieTargetsTheNearestPlayer`, the card that swept it): the villager is acquired on probe
  tick **1-12** over the 8 legs swept at the fixed tree (card t_752fbabd, 585c90d + in-probe prints: 1,1,2,8,9,10,10,12) and **1-28** over the 8
  legs swept at the pre-fix tree (f18e71f + the same prints: 1,2,5,11,13,17,25,28) — 16 legs, `mobTicks == probeTick` on all 16 — against a
  **400**-tick watchdog. The gate is a geometric draw: `NearestAttackableTargetGoal#canUse` passes 1 time in `reducedTickDelay(10)` = **5**
  (`Goal.java:50-52` halves the interval), and `Mob#serverAiStep` runs the full `targetSelector.tick()` only on the `(tickCount + id) % 2` parity
  tick (`Mob.java:779-794`), so acquisition is ~**1/10 per mob tick**. The old `getTick() + 40` window therefore reddened on `0.9^40` = **1.48 %**
  of legs; the converted probe reddens on `0.9^372` = **9.5e-18**. A fixed window on a geometric process is a rate, stated here as a number: the
  conversion is what removed it.
- **A stochastic band arm is mean +- 4 sd of the BINOMIAL SUM, widened to the drop's quantum — and it is never the only arm.**
  A band built from a multiple of the **per-death** sd is the same defect in statistical clothing. At `f18e71f`,
  `angryZombieDeathDropsFollowTheOriginalRolls` asserted `9..21` on a `Binom(30, 1/2)` sum (mean 15, sd 2.74): the width was 3 x the **5-kill** sd
  (1.5) and stayed `+-6` when the kill count doubled to 10, so the span was **+-2.19 sd of the sum** and tripped **1.61 %** of honest legs —
  measured red at 8 flesh. The giant's `87..153` on `2*Binom(120, 1/2)` (mean 120, sd 10.95) was `+-3.01 sd` -> **0.245 %**. With the target
  window's `0.9^40` = 1.48 %, the suite tripped **3.34 %** of legs = 1 red in 30, which is the 2-reds-in-40 the card reported. Both probes were
  green for weeks and red at a rate; neither assert was ever about the mod.
  So: **4 sd of the sum**, widened **outward to the drop's quantum** — a stack of 2 can only leave an even total, so a band edge that is not a
  multiple of the quantum bites on legal draws. `45 +- 4 sd` (30 deaths, 90 flips of 1) -> `[26, 64]`, exact trip **2.97e-5**; `120 +- 4 sd`
  widened by the stack of 2 -> `[76, 164]`, exact **3.24e-5**; the pair **6.21e-5** a leg = 1 red in 16 104 legs.
  Pick the kill count for **power**, and state it as the fraction of legs on which the DEFECT reddens: at 30 deaths a doubled rolls body (mean 90
  against `[26, 64]`) reddens on **99.993 %** of legs, while at 10 deaths the same defect against `[4, 26]` reddens on **81.7 %** and hides on
  **18.3 %**. State the derivation (flips, quantum, mean, sd, band, whole support) **in the failure message** — `assertRollBand` — so the next
  reader computes the trip rate instead of re-measuring it.
  A rate band cannot see a **rescaled stack** (a stack of 2 doubles the mean and the sd together), so pair it with the deterministic arms: the
  **per-death stack cap x deaths** (3 flips -> at most 3 stacks a death, 30 deaths -> 90; the giant's 2 loops x 6 flips plus its one 4-way
  `dropAllLoot` stack -> at most 13 a death, 10 deaths -> 130) and the stack **quantum** (`func_145779_a(rotten_flesh, 1)` is 1, the giant's is 2,
  asserted on every owned stack). Each reddens a rescaled body on 100 % of legs: the `M3b` mutation (giant, one loop with the stack at 4) fired the
  quantum arm ALONE, band 120 and cap 70 green.
  The band only means something over the subset the probe **owns** — see the attribution rule above: per-kill UUID delta (`DeathDrops`), never an
  item-id filter, foreign stacks named and never counted. Prove each arm with its own mutation, and expect the harness to test you: an injected
  census that reported 0 for the rate arm reddened BOTH drops probes — the band's low side, loud. Measured, swept inside the probes so the packing
  grid did not move, 10 kills a leg: the owned census sat at **11-17** flesh (angry) at the fixed tree and **13-20** at the pre-fix tree, and
  **98-142** / **100-130** (giant), against binomial means 15 and 120 — so no vanilla zombie loot table rides a modded id — and every stack in the
  25-block box was owned, **16/16** legs, foreign = 0 (8 legs per tree, the two sweeps above; both ran 10 kills, so the angry mean there is 15,
  not the shipped probe's 45).
<a id="mob-seed-stream-rate-arm"></a>
- **A rate arm drawn one roll per fresh mob is not n independent draws: name the seed source, measure the sum's variance inflation PER ARM, and de-correlate by reading draw j of generator j.**
  A mob's `RandomSource` is seeded by `RandomSupport.generateUniqueSeed()` = `SEED_UNIQUIFIER.updateAndGet(p -> p * 118178349727652981L) ^ System.nanoTime()`, so consecutive spawns differ by the
  **wall-clock cost of a spawn** and each roll is ONE point of a seed rotation whose step is that cost times the LCG multiplier: the leg's count moves as a block, its realised rate rides on
  machine speed, and because that cost differs between the arms CI runs, the distribution is **arm-specific**. Measured on the crab hardspawn arm, 20 legs each, 60 draws (tree `1b6eda86`):
  **18.2 ± 3.14** with-Curios vs **21.7 ± 4.39** no-Curios — variance 0.74× / 1.45× the binomial, realised 0.303 / 0.362 against the shipped `0.33F`, Mann-Whitney p = **0.015**, deviation
  tracking leg wall-time r = +0.30 over legs of 61-151 s. A band on that stream is a band on the runner: the no-Curios arm's tail is what reached 8/60 and breached `9..31`, and widening the
  band on the same shape just moves the wall. Fix the SHAPE: burn `j` throws on the SAME generator before reading draw `j`, so the phase advances with the draw index (equidistributed, Weyl)
  at any spawn cost. The same 40 legs, 360 rolls: sd **8.46** / **9.70** (variance 0.90× / 1.18× the binomial of the sum), arm split p = **0.797**, lag-1 −0.025 / +0.004, transitions
  162.4 / 158.2 and longest run 13.1 / 12.9 inside the independent-stream Monte-Carlo (158.9 / 12.8), load coupling r = −0.095, realised 0.331 / 0.333 — the binomial the band claims.
  Set the band from the **measured** per-leg sd (mean ± 4 sd, widened outward to the quantum), never the nominal, keep the deterministic pair (the guaranteed branch, and the outcome set the
  rate can never hide from), and state the power as the fraction of legs a doubled / halved rule body reddens (**98.8 %** / **99.7 %** at n = 360) plus the blind spot (a 0.25 body reddens on
  **9.9 %**): **widen n for power, never the band** (t_8b63e037).

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

- **A probe must not gate on a one-block-of-margin count that the batch produces.**
  `bossroombuildsthroughtheseam` asserted `eldritch >= 1` over the cell envelope. Measured, swept
  inside the probe so the packing grid did not move: **3** in **12/12** legs at `104ccce`, **0** in
  **14/14** legs at `2d2bc94`, **1** in **8/8** legs on an open-sided fixture — and the same
  envelope's non-air census is bimodal **1646 / 1871**, an exact **7/7** split at one tree. The
  225-cell step is the 2x2 shell forming, and what forms it is state the batch shares:
  `GenCommon.DECORATED_BLOCKS` is a static queue (main's own probes assert it empty), so the
  boss-room probe's outcome is set by its neighbours, not by the boss room — identical probe text,
  0 % red at one tree and 100 % at the next, and 2 red CI draws in 5. Assert a floor with margin
  (the LOW mode, 1646) or a fact the probe's own fixture produces, and keep the knife-edge census
  in the gate print and the failure message, where a shift is a fact and not a coin flip.
- **The boss room's doorway lands on ONE plane, and the LAST OPEN SIDE gets it only while the
  2x2 shell has not formed.** `GenBossRoom.java:141-163` is the original's if-chain (ref L39-64),
  inside the 7x7 doorway sweep at `139-193`: `if (cell.north) …` `if (cell.south) …`
  `if (cell.east) …` `if (cell.west) …` all write into the SAME `xx`/`zz` pair, so one cell lays
  exactly one 7x7 `PAT_DOORWAY` and the last open side in the chain owns that plane. Three arms,
  each measured cell by cell where the chain writes (probe-printed cell 70,70):
  (a) **open-sided feature-2 cell, no formed 2x2 group** — west plane **25** gate/lock variants
  (**7/8**) + **20** `blockAiry` at `x0+3`, `y=ROOM_Y+2..+8`; envelope gate/lock variants **25**,
  every one at `x0+3`, = `PAT_DOORWAY`'s 21 gate + 4 lock + 20 void, exactly; west **49/49**
  non-air with `blockEldritch v7` (gate) and `blockAiry` (the void doorway), north **28**, south
  **24**, east **24** — the other three planes carry only the room's own
  `blockcosmeticsolid`/`blockstairseldritch`. Deterministic.
  (b) **the same open-sided cell INSIDE a formed 2x2 group** — gate **0** / airy **0** on **all
  four** planes in **10/10** legs: the shell's own room build lands on top of the sweep, so the
  doorway is gone from every plane. In `generateRoom` the shell (`Gen2x2.generateRoom`,
  `121-136`) runs BEFORE the sweep (`139-193`) for the cell it is called on, so the covering
  build is a sibling cell's call landing later in the batch — the same queue-driven formation
  behind the **1646 / 1871** split in the rule above. "The last open side gets it" is a fact about
  a cell whose shell has not formed, not a property of an open-sided cell.
  (c) **the shipped `Cell(BOSS_CELL)`** (every side closed — the `Cell` constructor's default,
  **14/14** legs) — the chain leaves `xx=zz=0`, so the sweep parks at the **world column
  `(0,y,0)`**, 7 cells, while the cell's own envelope edge column reads **16 air**: that fixture
  has no doorway in its own envelope at all.
  Assert the doorway on the plane the generator writes it, on a fixture whose sides are open AND
  whose shell has not formed, print the shell mode beside the count, and name the chain in the
  message. History note: the landed fix's own commit-message subject/body (`d1844dc`, twin
  `8e9cd84`, merged as `d057b15`) carry the superseded "the last open side takes it" phrasing —
  read that history with this rule.
- **A probe's pad is its own: clear it, and print what it cleared.** The golem-combat pads are
  packed in a grid, the harness runs one probe at a time, and a probe's mobs are never cleaned up,
  so a sibling probe's mob stands in the pad on the producing tick — measured: a brainy zombie in
  the butcher probe's box in **11 of 12** legs, and in **1 of the 10** legs at `8b4953b` the oldest
  pig was found at the settle tick `alive=false removed=true` at full health. It is not the cull
  (`AINearestButcherTarget` refuses under a pen of 3, and the pen is 1 for the whole staging
  window), and it is not a window. Clear every mob in the template's box that is not the probe's
  own golem and herd, at spawn and at settle, and print them; the animal the probe measures then
  dies only to the probe's own fixture.
<a id="crop-light-propagation-depth"></a>
- **A fixture that leans on a block's LIGHT gate has to pay for its light, and a probe must classify
  the `BlockState` before reading a property that not every state of that block carries.** Measured on
  this tree: `interact_harvestcropscorereapsripewheatandreplantsit` reddened the with-Curios `build`
  job on CI (run `43675329931`, job `132694099034`, tip `e50113cc`) with `Cannot get property
  IntegerProperty{name=age, clazz=class java.lang.Integer, values=[0..7]} as it does not exist in
  Block{minecraft:air}`; the sanctioned same-sha rerun is green on all 3 jobs, the identical line sits
  in the c4a2 legs, and **0 of the 42** red `build` jobs in a 120-run window name it (16 of 16 local
  legs on the landing tree green — 8 with-Curios + 8 no-Curios, 502/502 each, probe never named), so it
  is a rate and the file that crashes is the probe, not the product. The mechanism is the crop's own
  survival gate: `CropBlock#canSurvive` (`CropBlock.java:164-171`) is `hasSufficientLight` =
  `getRawBrightness(pos, 0) >= 8` AND the soil gate, and `BushBlock#updateShape`
  (`BushBlock.java:30-35`) turns the crop into `Block{air}` on the FIRST neighbour update that fails
  it — and the harvest goal's own `setBlock` IS that neighbour update, so the cell the next draw reads
  can be air, and asking `CropBlock.AGE` of it is the disclosed line. The light that gate consults is
  `Level#updateSkyBrightness` (from `ServerLevel#tick`): `skyDarken = (1 − d2)·11` with
  `d2 = 0.5 + 2·clamp(cos(2π·dayTime/24000), −0.25, 0.25)`, so a crop under open sky survives at
  midnight (`dayTime 18000` → skyDarken 5 → raw 10) and pops at dusk (`dayTime 13000` → skyDarken 11 →
  raw 4). The game-test box pays for none of it: every crop cell in the patch reads raw **15**, the
  uncomputed-section value, printed on every leg of an 11-leg sweep — the patch's survival is inherited
  from a number the fixture does not own. The first shape the card shipped — one glowstone at
  `(4,3,4)`, Manhattan ≤ 5 from every crop cell, block channel 15 − Manhattan — measured
  INSUFFICIENT: on the 515-probe grid the drive's FIRST draw found **7 of 8** cells `Block{air}` with
  the farmland intact and raw 10-12 read AFTER the fact (`the crop patch lost a cell to a non-crop
  state at tick 190 … vanished=7 of 8 cells`), and the survivor was exactly the cell at propagation
  depth 2. That is a light engine mid-flush (`ServerChunkCache:573` schedules the flush per chunk tick,
  `ThreadedLevelLightEngine:54-55` is the queue): the SKY channel is applied before the block channel
  has propagated to the far cells, so inside that window a cell reads `max(0, 15 − skyDarken(11)) = 4`
  and `BushBlock#updateShape` pops it on the first neighbour update inside it, permanently. The rule is
  therefore not "pay for light" but **pay for light at propagation depth 2**: a source directly under
  EVERY crop cell (2 steps to the crop through the farmland, block channel 13-15 permanently; BELOW the
  soil, so the golem's walk cells and the crops' headroom are untouched), the crop planted at
  `PLANT_TICK = 40` — after that flush, never into it — the farmland paid wet (`FarmBlock#randomTick`
  turns dry farmland to dirt, `FarmBlock.java:95-104`; the crop above holds it through
  `MAINTAINS_FARMLAND`, `:124-126`), and any cell the window popped before the drive opened is restored
  AND PRINTED (`HARVEST-FIXTURE repaired=N`, with the state / soil / raw it found) so a fixture repair
  is on the record. Measured on that shape: **8 of 8** gate legs green (4 with-Curios + 4 no-Curios,
  515/515 each, `repaired=0`, the census arm never named), and the raw at plant tick is still 15 — the
  uncomputed value — which is why the draw classifies the state first
  (`state.hasProperty(CropBlock.AGE) ? … : VANISHED`): no draw can ask an `IntegerProperty` of
  `Block{air}`, and `getRawBrightness >= 8` is asserted on the drive's first tick in the probe's own
  message. The arm then ends on the OBSERVED reap+replant (`reaped >= 3`, `replanted >= 3`, polled by
  `succeedWhen`, no fixed verdict tick — the `return`-is-not-a-poll rule above) and the two hard arms
  (a cell that stopped being a crop, the drive window closed) throw a plain exception carrying the
  census: per-cell state, soil below, raw, golem state, repaired count. Teeth, RED by name on the fix
  tree: replant disabled (`if (false && broken && …)`) → `the crop patch lost a cell to a non-crop
  state at tick 203 … vanished=1 of 8 cells, repaired=0` (2/2 legs); harvest disabled
  (`final boolean broken = false`) → `the harvest goal never reaped the ripe wheat by the deadline at
  tick 670 … harvested=0, replanted=0, stillRipe=8, vanished=0` (2/2 legs). The crash class is
  reproducible on demand and closed structurally: the SHIPPED probe plus a planted pop
  (`setBlock … Blocks.AIR` at `SETTLE + 40`) went 2/2 RED with the CI line byte-exact (`Cannot get
  property IntegerProperty{name=age, clazz=class java.lang.Integer, values=[0..7]} as it does not exist
  in Block{minecraft:air}`), and the FIXED probe with the SAME pop went 2/2 RED naming the census arm
  (`… at tick 231: harvested=2, replanted=2, …, vanished=1 of 8 cells, repaired=0`) — the age read is
  gone, the pop is named, and that is also why the drive-open repair keeps its teeth: a pop landing
  after the drive opens is a red. Recorded so
  the next reader does not re-chase it: planting the phase clock into the shipped probe
  (`setDayTime(13000)`) went 1 red / 1 green and the red named
  `combat_woundedguardretaliatesanddoesnotrecruititsneighbour`, never the age read, and the probe's own
  raw read stayed 15 — inside the box the section's light is never computed at the read tick, so the
  daylight pop is not armable from the probe side (`setDayTime(18000)` = 4/4 green on the fix tree). The
  daylight gate is the source-level formula; the flush that actually bites inside the box is the queued
  one above, and the crash is closed structurally by the classification.

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

- **A scan-spawn probe driving a scripted `RandomSource` must isolate its pad, and may NOT buy tolerance
  from the stream.** The harness REUSES one grid area across batches: `StructureGridSpawner#onBatchStart`
  rewinds the walk to the batch corner and `StructureUtils#clearSpaceForStructure` clears only BLOCK space,
  and `GameTestInfo#succeed` discards only the entities inside the finished probe's own inflated box (+1).
  Mobs that strolled out of a pad, stacks that skidded out, and every entity of a FAILED probe (a failed
  probe never cleans at all) survive into the next batch, where a 24-tall pad now lands on top of them.
  A stray standing on a scripted landing cell rejects the floor check (`WarpEvents.java:298`), and an
  exact positional stream desyncs on the FIRST unmarked rejection: the spider reads the NEXT spider's
  window, every landing shifts, and the run dies naming "the warp event drew beyond the scripted queue".
  Measured on this tree: `theIllusionBandSpawnsTheWarpCappedHarmlessViewerBoundSwarm` went red 2 of 3 CI
  runs of `3ad8a69e` (502 probes, with-Curios arm only — the no-Curios arm packs a smaller grid that left
  the pad clean) and green on the third re-run; local runs 2/2 green, so the rate lives in batch
  composition and runner timing, not in the product. THE FIX IS THE PAD, NOT THE QUEUE: sweep the pad's
  census box at probe setup (discard non-player entities — the scan is synchronous in one tick, so after
  the sweep nothing enters the pad except the probe's own swarm and fixtures, `WarpEventsGameTest.sweepPad`),
  keep the stream positional, and MARK every rejection the probe intends (`secondAttemptFor`) — the queue
  clock (drained, `consumed == 2 + 6·Σ(landing k + 1)`) then has one satisfying landing pattern and a
  residual overrun is a product anomaly. Measured anti-pattern (same card): widening the supply to 3
  windows per spider to "absorb" rejections made every spider accept the previous spider's leftover
  window on a CLEAN pad — 49 landings at y = 8, spent 99 vs consumed 302 — the mutation sweep caught it;
  a positional stream and per-spawn tolerance are mutually exclusive. Prove the causal lever by PLANTING
  the stray: planted before the sweep the fixed probe is GREEN, planted after the sweep (or on the
  unswept tree) it goes RED naming the queue overrun — the CI signature, on demand.

<a id="sweep-hazard-set-is-scan-set"></a>
- **The sweep's hazard set is the GATE's scan set, and a probe that registers into a level-wide list has
  to tear down on the RED path.** The rule above sweeps non-player entities and LEAVES players, because the
  warp event's scan is a BLOCK scan and no player rejects a cell. A gate that scans ENTITIES has to size
  the set to its own scan: `EntityEldritchCrab#tick`'s mount asks `level.getNearestPlayer(this, -1.0)` —
  distance-UNBOUNDED, target list `level.players()` — so a registered player resident is IN the set, and
  the grid re-gives a pad the coordinates of the pad that leaked it. Measured on this tree:
  `anunshelledCrabInMidAirClimbsOnAPlayerAndPunchesHimOff` went red **1 of 12** full-grid legs
  (`:120-129 — a free mid-air unshelled crab mounts the nearest player`, the parent's disclosed CI line),
  and a planted co-located **registered** `ServerPlayer` reproduces that line **byte-for-byte** on the
  base tree (`P2`) and on the fixed tree (`Q1`), while 10 mid-air mind spiders (`P1`) and a roaming
  unshelled crab (`P3`) leave the probe green (3 legs each) — mob residents are not this gate's hazard,
  player residents are. `EldritchCrabGameTest.sweepPad` deregisters player residents from
  `level.players()` and discards them; the box (2,-4,-2 .. 13,15,13) is provably big enough because the
  mount needs `distanceSquared < 4.0` and the scan keeps the strict minimum. Second half of the rule:
  `level.players().add(player)` is a LEVEL-WIDE registration, the harness's box cleanup cleans entities
  and not the players list, and a probe that throws an assert reaches NO tail code — so a tail-only
  teardown leaks the registered player exactly on the leg that goes red, and the next pad is re-given
  those coordinates: the flake manufactures its own next trigger. That fixture is also a bare-connection
  `ServerPlayer` whose first payload send throws (`Payload thaumcraft:biome_change may not be sent to the
  client!`, measured on the c4b-1 card), and the throw rides the shared level tick into the NEIGHBOUR
  probe. Teardowns of level-wide registrations belong in a `finally` (the crab fixtures ride a list so the
  red path discards them too). Third: `runAfterDelay(N, …)` is `runAtTickTime(getTick() + N)` — the
  TEST sequence's clock, not the entity's — so the line-192 rule ("assert the outcome, never a tick
  budget") applies to it verbatim: `theOrbBurstStrikesTheLivingAndSparesTheUndead` pinned the impact's
  consumption to 2 real ticks and went red on a **clean** pad **1 of 11** legs with `age=100
  removed=false` (the impact landed, the entity's own survival walk had not reached the clamp), the same
  signature as the shipped Energy Orb probe's planted E2b. The consumption is polled
  (`helper.succeedWhen`) and every write of the impact stays asserted synchronously.
  Teeth on the FIXED tree, legs in this card's keep dir: the P2 plant moved AFTER the sweep — a
  resident the sweep cannot remove — reddens the climb probe with the disclosed line (`Q1`); the P1
  spider plant, also post-sweep, leaves climb + orb green (`Q2`); and the tick-budget mutation
  `runAfterDelay(0, …)` on the consumption arm, which fires before any real server tick, reddens the
  burst probe 3 of 3 legs naming `:84 … age=100 removed=false` with the climb probe green 3/3
  (`Q6d`). The 2-real-tick budget's natural rate is 1/11, so three legs of the exact pre-fix budget
  show no teeth (measured: 0 of 3) — the deterministic delay-0 mutation is the teeth leg, the 1/11
  natural rate is the motivation. Plant-script lesson from the failed first pass: plant anchors pin
  the probe's setup text, so a probe refactor silently UNPLANTS the script and the legs run clean —
  a planted leg must print its plant line, and every leg verdict is read, never assumed.

<a id="scan-claim-covers-box-cells"></a>
- **A pad claim's cell set is the GATE's block-arm set: the landing cell and its `:298` cell are 2 of the 5 cells a scan window reads.** The rule at `docs/testing.md#sweep-hazard-set-is-scan-set` says the claim has to cover every arm the gate reads, and the implementation of it shipped 2 of 5: `WarpEventsGameTest.claimScanCells` named each scripted landing cell and its `:298` cell, while `:300` is `noCollision(spider.getBoundingBox())` — `:299` `moveTo`'s the spider to the INTEGER cell corner and `Spider#defineDimensions` makes an adult spider 1.4 wide (the probe's own `EntityMindSpider` says so), so the box straddles x-1..x and z-1..z and reads FOUR cells, three of them outside the claim. Every red on the `a069e53e` lineage lands in one of those three, and each red's own claim line names the terrain the claim DID reach: main run 43748694926 (job 132934321588, `thescanrejectsoccupiedandflooredcellsandspendsitssecondattempt`) cleared `grass_block[snowy=false] @ rel (8, 0, 8)` — window a=0's `:298` cell — six ms before the same probe rejected window a=8's box cell `(7, 1, 9)` on the same block; custody-ref run 43756378800 (job 132941271802, `therealbandspawnsthewarpcappedhostileswarm`) cleared 30 foreign blocks (`dirt`, `grass_block`, `cobblestone`, `mossy_cobblestone`, `cobblestone_stairs`, y 0-1, e.g. `cobblestone_stairs @ rel (9, 0, 18)`) before rejecting box cell `(9, 1, 8)`. So **the probe's own sentence "this intruder arrived after the claim" was the wrong diagnosis**: the terrain wave is inside the pad at claim tick — it is *named* in cells the claim clears and *left standing* in the box cells the claim never named. Read the claim's `cleared N` as the channel census (`cleared 30` / `cleared 1` on the reds, `cleared 0` on a clean handoff). Shape (t_d8944aae): the claim derives its cells from the SAME detached entity the shadow measures with (`blockCellsOfWindow` = `moveTo` + `getBoundingBox()`, the probe's `boxFits`), and asserts `cells == 5 × windows` so a change in the mob's box moves the claim with it and names the box dimensions in the message. Keep it a SETUP claim: the scan is synchronous inside one tick (one draw per tick, `WarpEvents.java:266-272`), so a claim taken at the window's own draw buys nothing the setup claim cannot. Teeth, deterministic — no streak needed: `cobblestone_stairs[facing=east]` planted at rel `(9, 1, 8)` BEFORE the claim (the CI's cell) → shipped tree RED naming rel `(9, 1, 8)` byte-for-byte with the CI line, fixed tree GREEN with the claim line naming the cleared stair in its box-cell census; `grass_block` planted at rel `(7, 1, 9)` in the blocked-scan probe (the 4229b84e cell) → shipped RED naming `(7, 1, 9)`, fixed GREEN; the same plants in the illusion probe's `(7, 1, 20)` → shipped RED, fixed GREEN; and the claim reverted to its 2-cell set on the fixed tree with the plants kept → RED again, which makes the cell set the lever and not the plant's tick. **Census (t_d8944aae): teeth, one leg each, NO-Curios, the three cells planted BEFORE the claim by the plant helper, which prints its plant line on the leg — shipped claim RED naming rel `(9, 1, 8)`, `(7, 1, 9)` and `(7, 1, 20)` byte-for-byte the three reds it reproduces (main runs 43756378800 and 43748694926, and the natural landed-tree leg at 16:49:21), with every claim line in that leg reading `cleared 0`: the plants stood in cells a 2-cell claim never names. The same plants on the fixed claim: GREEN 515/515, each claim line `cleared 1 foreign block(s) — 1 of them sat in a :300 box cell`, naming the plant. Fixed tree with the claim widened only along the landing column (floor + landing + `landing.above()`, 3 of the 5 cells) and the plants kept: RED again on the same three cells — the cell set is the lever, not the plant's tick. Fix arm matrix: NO-Curios 10/10 GREEN (30/30 claim lines `cleared 0` — the channel is drawn by the CI-side terrain: 2 reds in 4 CI samples against 1 in 11 local shipped-tree legs), with-Curios 2/2, post-merge on main `bccc2312` NO-Curios 4/5 + with-Curios 2/2, probe census 515 → 515. The 5th merged leg reddened at 17:52:32 on `theorblifetimeclampdiesatsixteenforplainandtwofortyforred` (`PROBE BROKEN: the orb cells must be AIR — got Block{minecraft:dirt}`): the same terrain wave in the warp ORB probe's cells, a lane this rule does not cover and this fix does not lever.**

- **A sweep is only HALF a scan probe's isolation: the claim has to cover every arm the gate reads, and
  the probe's own fixtures are claimed LAST.** The rule at :390 sweeps non-player ENTITIES, and the
  hazard census that justified it (49 mind spiders at y = 8) is a shape the BLOCK arm cannot produce.
  `cellFits` (`WarpEvents.java:390-392`) is `noCollision` on the 1×1×1 box **plus** the player arm, and
  `:300` reads the spider's own `getBoundingBox()`, so a block inside a scripted cell rejects a window
  the sweep cannot see. Blocks are the half the grid hands over dirty: a 1×1×1 pad strides 6 cells
  (`StructureGridSpawner`: stride = `template width + 5` / `depth + 6`), so sixteen pack inside a
  24-wide pad, their bodies run in the SAME tick, and `clearSpaceForStructure` clears BLOCK space only
  — a neighbour's `setBlock` at its own cell (1, 1, 1) lands on a scripted landing cell HERE. Measured
  on this tree: one `Blocks.STONE` at rel (18, 0, 8) (slot 5's `:298` floor cell) reproduces the CI line
  byte-for-byte on `6a05ba14` in BOTH arms, and the same leg took
  `theScanRejectsOccupiedAndFlooredCellsAndSpendsItsSecondAttempt` red on the identical line with no
  plant in it; the base tree with no plant went red **1 of 8** full-grid legs on the no-Curios arm
  naming `theIllusionBandSpawnsTheWarpCappedHarmlessViewerBoundSwarm … drew beyond the scripted queue
  (bound 18)` — the natural channel, on demand. The layout draw is the ALIGNMENT, not the cell: the
  intruder's position is a function of the grid origin, so no named cell can be excluded — only claimed.
  Shape (t_6119f491, `WarpEventsGameTest`): `claimScanCells` clears and NAMES the blocks of every cell
  this scan can read (each scripted landing cell and its `:298` cell) at setup,
  `attachScanShadow` replays the scan's four arms on every scripted window at the window's own draw —
  so an unmarked rejection fails BY NAME (block state / entity type + id / cell) at the instant it
  lands, instead of ending as the unattributable queue overrun that made this lane unrateable — and
  `assertScanOutcome` asserts the marked rejections are PRESENT, so the probe cannot pass on a pad that
  was handed over clean. Claim first, fixtures after: a claim that runs after the fixtures eats them.

- **A window-shadow over a scripted stream aligns on the DRAW SHAPE, works in ABSOLUTE space, and
  traces its first windows.** The shadow replays the gate's own cell checks on every scripted window
  so an unmarked rejection names its intruder (block state / entity type + id / pad-relative cell)
  instead of surfacing at the tail as "drew beyond the scripted queue" — the shape that made this lane
  unrateable (t_6119f491: 2 CI reds on one lineage, base-tree rate **1/8** measured with no plant in
  the tree). Three shipped versions read the stream plausibly and were wrong, and each failure mode is
  cheap to name. (1) A counted pre-scan offset misses the band roll at `:59-61`, so every window is one
  draw off and the shadow fires on all 502 legs. (2) Pad-relative cells used as world coordinates: a
  game-test pad sits at world coordinates in the MILLIONS, so the arms read the void — the floor arm
  finds the sea-floor stone at absolute (8, 0, 8) and fires on EVERY leg, the entity arm finds nothing,
  and the fixture assert then names a "missing" fixture on every leg. (3) A boundary roll the port
  never draws: the landing spot is the scan's RESULT, and `WarpEvents` draws `:45` (100), `:50` (warp),
  `:57` (4, helm only), `:59-61` (101, band), then per attempt SIX draws shaped (offset `nextInt(18)`,
  sign `nextInt(3)`) per axis (`:292-297`) — arming on a phantom bound-50 roll arms nothing, so the
  shadow silently never runs at all. So: the boundary IS the shape, with the bound asserted at each
  position; every cell goes through `helper.absolutePos(BlockPos.ZERO)`; and the first three windows
  log `key / draws / consumed / verdict`, which is what made the mapping auditable at all. **Prove a
  code path runs by counting its log line against a line known to be emitted on the same leg**
  (measured: 0 `shadow window` lines vs 3 `claimed` lines in the same log) — one grep settles what
  three rounds of static reasoning did not. Teeth, all deterministic on the fixed tree: stone planted
  BEFORE the claim → `All 502 passed` with the claim line naming the clear; the same stone with the
  claim disabled → RED; stone planted AFTER the claim → shadow RED naming `(a=5, k=0) … :298 floor cell
  (18,0,8) … Block{minecraft:stone}`; `SCAN_MARKS {0,1,2}→{0,1}` → shadow RED naming slot 2's own
  fixture window; the fixture stone removed → the positional stream shifts and the landing arm reddens.

- **A scripted ride assert must arm off the mount's own same-tick punch.** The crab's climb gate
  (`EntityEldritchCrab#tick` :120-129) and its punch block (:131-137) run in the SAME `tick()`, and a
  freshly spawned crab has `invulnerableTime == 0` — the punch's cooldown gate is wide open on the
  mount tick, and `dismountsAfterPunch` (`PUNCH_DISMOUNT_CHANCE 0.2`) throws the rider off on ~1/5 of
  scripted ticks, before the assert reads the vehicle. Measured on `cab7305c` (the pad-contamination
  fix tip, 8 full-grid noCurios legs with a per-tick ride trace): EVERY leg ends the tick with
  `invulnerableTime 10..19` (the punch fired on the mount tick); the 2 red legs trace
  `veh=null psg=0 ivt=12 boardingCooldown=60 rise=+0.20` while the 6 green legs trace
  `veh=ServerPlayer psg=1 ivt=10..19 bdCd=0 rise=0.00`. `boardingCooldown=60` is set only by the
  dismount path (`Entity#removePassenger`) — it is the tell that the ride happened and was LOST, not
  rejected. This kills the `Entity#startRiding`-rejection reading from the earlier census: every arm
  of the 21.1.259 gate (`vehicle.couldAcceptPassenger()`, the vehicle-chain cycle,
  `EventHooks.canMountEntity`, `canRide` = `!sneak && boardingCooldown <= 0`, `canAddPassenger` =
  `passengers.isEmpty()`) traces TRUE on the fresh pre-tick fixture state, and the red census
  `yDelta=1.80 / distSqr=3.24` is arithmetic the rejection story cannot produce — the design spawn is
  1.60 above the fixture (`distSqr 2.56`); 1.80/3.24 is 1.60 + the 0.20 dismount hop. A census that
  re-reads state AFTER the tick sequence measures the tick's outcomes, so it must not be read as the
  pre-tick gate state; the arms have to be captured pre-tick to name a rejection. Fix: probe fixture
  hygiene, not assert weakening — arm the crab at its maximum punch cooldown
  (`PUNCH_COOLDOWN_BASE + PUNCH_COOLDOWN_JITTER`) before the scripted mount tick (aiStep decrements
  once inside the tick, the punch block skips) and measure the punch cadence and the 0.2 band on
  their own dedicated arms. Teeth: the mutation `PUNCH_DISMOUNT_CHANCE 0.2 -> 1.0` reddens the
  pre-fix probe on EVERY leg naming `:120-129 — a free mid-air unshelled crab mounts the nearest
  player`, and leaves the armed probe green — the dismount arm, named on demand. The
  layout-draw/red-coordinate axis from the parent card is dead: the event is a `nextFloat()` roll,
  the pad coordinates are uniform in both outcomes, and no grid-origin change can fix a 20 % punch.

- **A consumption poll must carry its own survival drive; a 3-sigma band on a shared-seed stream is not 3 sigma.** Two `EldritchCrabGameTest` arms flaked the with-Curios 502 streak at tip `a29bbc06` (card t_d0cc96e9), each exactly a docs rule wearing a compliant costume. (1) The orb-burst consumption arm was already a `helper.succeedWhen` outcome-poll — and still starved: the harness's level pin is a FINITE `setForcedTicks(1000)` pool, and at a batch edge the test sequence's clock outlives the ENTITY clock, so the orb never receives the one server tick (`tickCount` is incremented by `ServerLevel#tickNonPassenger`, not `Entity#tick`) that walks it from the impact's written age 100 past the `:27-29` `> 100` clamp — the watchdog fired 1 leg in ~14 with `age=100 removed=false`, impact landed, walk never arrived. The poll shape is only half the contract; where the outcome lives on the entity's clock and the harness's pool can die, the probe feeds the subject its survival step IN THE SERVER'S SHAPE (`tickCount++` then `tick()`) on every poll tick and the assert stays on the outcome (`removed`). Teeth: the clamp mutation `> LIFETIME_TICKS` → `> LIFETIME_TICKS * 10` puts the consumption 1000 steps past the 300-tick watchdog and reddens the probe on EVERY leg naming `:84 … age=… removed=false` (measured 2/2, the parent Q6d budget teeth stay intact as the record on the pre-poll shape; the `:84` write→0 mutation walks green past the watchdog on the drive shape — the consumption arm asserts BY the clamp, not at its tick). (2) The hardspawn rate arm `rolled >= 9 && rolled <= 31` is ±3.0 sd of ONE `Binom(60, 0.33)` (mean 19.8, sd 3.64, nominal trip 1.37e-3) — a legal band by the 4-sd rule's arithmetic only at 4 sd — fed by 60 per-mob `RandomSource`s seeded off the shared LCG stream: the correlation inflates the effective sd and it measured 1 red in 17 legs (the red leg drew 8, a 3.24-sd tail under the nominal sum). Same lesson as the 30 % band floor entries above: on correlated streams the NOMINAL binomial is optimistic; widen to 4 sd of the sum (`[5, 35]`, exact 1.75e-5 on the nominal, tolerating the ~3× effective inflation the 1/17 implies) and pair it with the deterministic arm the rate can never hide from — here the observed OUTCOME SET of the 60 draws must be exactly {shelled, unshelled} (the whole support [0, 60] of a 1/3 roll contains both), which a roll rule collapsed to one outcome reddens on 100 % of legs (teeth: `nextFloat() < 0.33F` → `< 0.0F` → `0/60`, measured red naming `:74-75`). **The 1/17 has a mechanism, and it is the STREAM, not the band width (t_8b63e037): each per-mob `RandomSource` carries `uniquefier × 118178349727652981L ^ System.nanoTime()`, so 60 consecutive rolls are one point of 60 near-identical seed rotations — measured arm-specific over 20 legs per arm: 18.2 ± 3.14 with-Curios vs 21.7 ± 4.39 no-Curios, realised 0.303 / 0.362, split p = 0.015, deviation tracking leg wall-time r = +0.30 — and the no-Curios arm's tail is the 8/60 that breached `9..31` on the `game-test-no-curios` leg. `[5, 35]` on that shape widens the wall, not the distribution. The arm now reads draw j of generator j (j throws burned on the SAME generator before `finalizeSpawn`) at 360 rolls — sd 8.46 / 9.70, variance 0.90× / 1.18× the binomial, split p = 0.797, lag-1 ≈ 0, load coupling r = −0.095 — and the band is the MEASURED one: 118.8 ± 4 · 9.70 widened outward = `80..158`, exact trip 9.3e-6, 0 of 40 census legs outside (observed 103..140), power 98.8 % / 99.7 % against a doubled / halved roll body and 9.9 % against a 0.25 body, the declared blind spot. Rule: docs/testing.md#mob-seed-stream-rate-arm.**

- **A draw taken N ticks after a rule WRITES is a tick budget, even when the asserted value is an
  outcome: park the subject, drive ONE tick, and read what that tick wrote.** Measured on
  `combat_returnhomeusestheradiusandteleportsfrombeyond48` (`gametest/GolemCombatAiGameTest`, golem
  lane, 502-probe grid): the probe parks the golem 52 blocks out along +z at `SETTLE+40`, hand-drives
  one `EntityGolemBase.tick()` at `SETTLE+45`, and asserts the golem stands in the home cell's 3x3
  footprint within 3.0 squared of the placer's centre (`AIReturnHome.java:33`'s own gate). Its javadoc
  premise — the 52-block cell is outside the template's ticking area, so only the probe's hand tick can
  fire the rule — is FALSE on most legs: instrumented legs print `restrictDistSqr=2.0` on the PRE line,
  i.e. the shipped rule (`EntityGolemBase.java:1362-1380`, 2304.0 squared) had already fired on an
  ENGINE tick between the park and the draw. The rule writes a `moveTo` into the scan's first
  qualifying triple — the deterministic `(dy=0, dx=-1, dz=-1)` cell, 2.25 squared — and calls
  `navigation.stop()`, but it does not zero the velocity, so the walk phase's carried `deltaMovement`
  keeps moving the golem after its own write. Measured over 14 legs: 12 legs fire on an engine tick and
  draw the drifted state (1.2400 ×4, 1.5560, 2.6461 ×7 — `dmv=(0.0, -0.0784, -0.0042)`), 2 legs never
  got an engine tick and drew exactly 2.2500. The threshold sits in that drift tail: the red this card
  chases drew **3.212850838329566** in the same cell, and the fixed margin on the mode is 0.35. So the
  probe is a rate, and the card's suspected cause — the crab burst arm's `tickCount++`/`tick()` drive
  adding shared-level tick weight — is EXONERATED by planting, not by more streaks: widening that drive
  to 24 orbs driven on every poll tick for the ~50 ticks the pool takes to drain (`[PLANT t_d67dc22f]
  pool=24`, ~+100 extra ticks a leg, a 24x dose of the landed shape) gave **0 target reds in 17 legs**,
  0/14 on the pool=0 control, 0/12 on `eca312f0` (the tree whose cold leg produced the red), 0/10 on the
  untouched base and 0/10 at current main; P(0/17 | the hypothesised 1/5) = 0.023. The tick-weight
  channel is closed for this probe — the coupling is its own 5-tick draw. Fix shape: park + drive ONE
  entity tick + assert what the rule wrote, all inside one tick, and repeat that trial 8 times as the
  deadline the envelope is polled on (far inside `timeoutTicks=400`); every trial asserts, so a product
  that stops teleporting, lands outside the 3x3 footprint, or lands past the home radius reddens on
  trial 1 of every leg — and the trial registrations all come from the probe body, never a nested
  `runAfterDelay`. Teeth (disposable worktree at the tip + this fix, mutations in the PRODUCT, reverted,
  tree removed): `>= 2304.0` → `>= 1.0e18` (the rule never fires) reddens the fixed probe on 2/2 legs
  naming `trial 1/8 of the 48-block rule: the golem stands in the home cell's 3x3 footprint on the tick
  the rule wrote it`; landing at `var2 + dz + 0.0` (the footprint still passes, squared 3.5) reddens it
  on 3/3 legs naming `inside the goal's home radius again: squared distance 3.5`; landing at
  `var1 + dx + 3.5` (`|dx| = 2`) reddens the footprint arm on 2/2 legs. Landing at `var1 + dx + 2.5`
  stays GREEN and is contract-EQUIVALENT — the scan's first triple is `dx=-1`, so +2 is the mirrored
  `+1` corner: `|dx| = 1`, squared 2.25, inside both arms the probe asserts. The probe claims the
  outcome class (home, in the 3x3 footprint, inside the 3.0 radius), not which corner the scan picks; a
  mutation that only mirrors the corner is not a hole. Do not weaken the
  radius arm to dodge the flake: the shipped write is 2.25 and the arm is 3.0, so the arm has margin the
  moment it is read on the tick the write happens. Corollary for probe authors: if a probe parks a
  subject to control *when* a rule fires, verify the park cell is really unticked — print the rule's own
  precondition (`restrictDistSqr`) on the tick before the draw; a precondition already satisfied means
  the hand-driven tick is not the write tick and the draw is a budget.

- **A 1.0-health mob probe has to CLAIM its own mob box, and the channel a pad-sharing claim can NOT
  close is fire.** `theHarmlessFuseDiscardsPast1200TicksAndANormalSpiderNeverExpires`
  (`gametest/MindSpiderMobGameTest`) runs a 1.0-health armed harmless spider and a control spider over
  1300 ticks with a full-health assert every 20 ticks, and went red on the shipped line
  `the armed spider lost health at checkpoint 20 (pad burn/fall) — health 0.0, tickCount=20` at **1/4**
  with-Curios legs on `046f3d32` (t_43022b00 `fixstress#2`) and **1/5** with-Curios legs on `3b5a49ae`
  (t_47951c77 `cens_land_4`) — attributed by probe NAME. `tickCount=20` is ONE damage instance inside
  the probe's first 20 ticks, and the probe owns no damage source: the hazard walks in from the pad the
  grid handed over. Two channels belong in the rule's set because a claim reaches both: a colliding
  block in the mob cell (`LivingEntity#pushEntities` → `makeStuckInBlock`, lethal at 1.0 hp), and a
  foreign entity inside the mob box — the pech-blast pellet flies 12 blocks at 0.5/tick and a 1×1×1 pad
  strides 6 cells (`StructureGridSpawner`: `width + 5` / `depth + 6`), so a neighbour's corridor crosses
  this pad, and its hit is `damage().setExplosion`, 1.5 + knockback, dead in one hit. The second is the
  natural channel, and the claim that PROVES it is a claim that LOGS its catch: on one leg of seven
  landing legs the fuse probe's pad was handed over a `barrier` and a `stone` in the mob cell column
  plus a live `EntityPechBlast['Pech Blast'/12763]` inside its mob box (mid-batch, not a plant probe),
  the claim removed all three and the grid ran 504/504; the other six legs were handed a clean pad
  (t_21b2160d, `teeth2/fixed1.gradle.log`).
  Fire is NOT reachable by a probe-side claim and must NOT be added to one: fire set on a tick takes its
  boundary damage in THAT tick when the ticks are a multiple of 20 (`LivingEntity#baseTick`) and the
  harness's tick runs after the entity pass (t_89eee704's measurement), so no probe-side `clearFire` can
  precede it — measured: a planted `ignite(300)` kills the spider on the ignite tick with the guard in
  place — and fire is not the observed natural channel either: the sampler under the burn/fall red saw
  `remainingFireTicks = 0` on both spiders on every green leg (8 legs × 1201 ticks) and the pad set no
  fire on 24/24 sampled legs.
  Shape (t_21b2160d): `claimMobPad` sweeps the lane's own `mobBox` (±6/7, 10 tall) of foreign entities,
  clears the colliding blocks of the mob cell column, LOGS what the batch handed over, and asserts the
  claim; it runs at setup and `onEachTick` for the probe's own window — the set the gate measures. The
  plinth (the barriers at z+1, z+2) sits outside the column and is untouched; health, fire immunity, the
  checkpoints' `clearFire` and the fuse asserts are untouched — the claim moves contaminants, never the
  subject's state. Teeth: disable the claim and a plant probe (stone in the mob cell + a pech blast
  fired into the box) reddens with the shipped line on 2 of 2 legs (`teeth2/mut1`, `mut2`); with the
  claim in place the same plants are swept. Census (base = no claim, landing = claim, curios arm, 5
  interleaved pairs, full grid, `census2/`): **0/5** base vs **0/5** landing (Fisher p=1.0), pooled with
  the historical base legs **2/14 vs 0/7, p=0.43** — the census ran at loadavg 3.6-4.9 with 2 concurrent
  JVMs, where P(0 reds in 5 legs) is 0.33 at the historical 1/5 rate, so the census CANNOT resolve the
  rate and no rate claim rides on it; the deterministic planted teeth are the causation lever.

- **A box query reads the level's ENTITY list, not `level.players()` — so the rule above (the sweep's
  hazard set is the GATE's scan set) is a rule for gates that scan the player list, and its
  registered-resident arm must NOT be copied into a gate that scans boxes.** Measured on t_21b2160d by
  PLANTING, while testing the opposite hypothesis: a `ServerPlayer` registered through
  `level.players()` (the crab fixture's shape, `EldritchCrabGameTest:672`) planted ON the warp pad is
  reported by that pad's own `getEntitiesOfClass` census **0 times on every leg that planted it**
  (3 legs, `teeth2/fixed1-3.gradle.log`), and the scripted warp probes stay green on all three of those
  legs — `WarpEvents`' `:298`/`:301` arms are `level.getEntities`
  over the entity list, and `Entity#discard` can never run on something the sweep cannot see, so the
  plant cannot produce the queue overrun the lane reports. The player arm is therefore NOT this lane's
  lever: the measured lever for the scripted-queue probe is the BLOCK claim
  (`theScanRejectsOccupiedAndFlooredCellsAndSpendsItsSecondAttempt`, t_6119f491, `claimScanCells`), and
  a probe that sizes its sweep to a gate reading `level.players()` (`getNearestPlayer`) sizes it
  correctly ONLY for that gate. A claim arm that can never fire is not free — it reads as coverage and
  measures nothing. Census for that probe: **0/5** base vs **0/5** landing (both arms carry t_6119f491's
  claim), pooled with the historical base legs **3/37 vs 0/5, p=0.68**.

## Cite audit

Every cite of a Rules bullet — in `src` probes, in this file, anywhere — is an **anchor**:
`docs/testing.md#<kebab>`, with `<a id="<kebab>"></a>` on the line directly above the bullet it
names (lowercase kebab, minted in the same commit as the first cite of that bullet). Anchors do
not move when a bullet is inserted, so cites cannot rot the way line numbers did; the shift
history that minted this scheme lives in the removal-age rule above.

One-command check (exit 1 on a stale cite):

    python3 scripts/check_docs_cites.py

prints one line per cite in `src/` and this file, in the fixed shape

    file:line -> :NNN TARGET[<the cited line>]

where `:NNN` is the CURRENT line the cite resolves to (an anchor resolves to the bullet directly
below its anchor line) and `TARGET[...]` is the text sitting there — a stale cite is the line
whose TARGET is not the rule the citing comment describes. A missing or duplicated anchor prints
`STALE[...]` and exits 1. A deprecated line-number cite prints `LEGACY[...]` and exits 0 until
the last one is converted; no NEW line-number cite is accepted anywhere.

Anchors minted for the cited bullets (t_867b394f): `vouched-cell`, `entity-clock-poll`,
`removal-age-names-mechanism`, `outcome-not-tick-budget`, `sweep-hazard-set-is-scan-set` (t_b33916a2, the mind-spider mob-box claim). Minted by t_8b63e037:
`mob-seed-stream-rate-arm`. Minted by t_d8944aae (the pad-claim box-cell rule):
`scan-claim-covers-box-cells`.
