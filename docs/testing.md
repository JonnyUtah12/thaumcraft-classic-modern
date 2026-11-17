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
<a id="harvest-pad-claims-its-light-layer"></a>
- **A probe that measures through a gate has to claim every layer that gate reads, not only the layer it
  asserts, and it has to re-assert its precondition at its own QUIET tick.** Measured on the harvest lane,
  no-Curios arm: `interact_harvestcropscorereapsripewheatandreplantsit` reddened **2 of 2** samples on the
  `2eda098b` lineage (runs `43788415254` job `133035144147`, `43786472558` job `133041135`) with
  `the crop patch sits under CropBlock#hasSufficientLight … at BlockPos{x=5, y=1, z=6} … wheat[age=7],
  below farmland[moisture=7], tick 190`, and the rerun of the first run went green (all 529 required tests
  ×3, 0 sig lines, marker present) — same sha, same lane, red then green: a channel, not a regression. The
  reds landed on the 529-probe census; the greens were the 515 and 517 censuses (`f2d0cbbe`, `b1cead03`), so
  the c5-1 packing shift moved this lane's grid, which is the mechanism the `packing-grid-collision` rule
  names. The lane's own evidence lines say what moved: at `PLANT_TICK = 40` **six of the eight cells read
  `raw=0`** and only the two whose source survived read `raw=13`, and at the gate tick `repaired=5` named
  exactly the cells that then read 7-13 — the glowstone the fixture paid for at y=-1 was gone under six
  cells before the crops went in, and `BushBlock#updateShape` fired on the first neighbour update after
  the drive opened, in the cells the drive-open repair had just restored. The grid writers that rule names
  (`LevelWriter#clearSpaceForStructure`, a sibling's `setBlock`) reach y=-1 (the source layer), y=0 (the
  soil) and y=2 (the crop's headroom); the crop probe claimed none of them.
  The shape, in this tree's words: the fixture names the cells it owns in a struct — source y=-1 under every
  crop cell, soil y=0, crop y=1, headroom y=2, the golem's walk columns — and the pad **CLAIMS** them before
  the fixture writes, **PLANTs** at `PLANT_TICK`, and **SERVICES** on every drive tick, printing every write
  so the census carries `cleared=N` (the clean-handoff number: `cleared=0` on a clean leg) and `relit>0` (a
  source that got eaten mid-drive). SERVICE repairs only PROVEN contamination: a foreign block standing in
  the crop cell, a roof in the headroom, a source that is no longer a source. It never fills an air crop
  cell during the drive, because that is what the probe's own chop looks like, and planting every tick would
  fake the reap the reap arm measures (that arm needs a 7 → 0 transition, which only the goal's replant
  makes). The fixture's precondition assert then runs on the drive's first **quiet** tick — after the pad's
  last write, the lane's own light-settle rule — and at the depth the fixture pays for (`raw >= 13`), not at
  the bare gate threshold: at the gate threshold 8, a cell whose channels have not propagated reads 7, which
  is the value this rule measured mid-flush, so asserting 8 arms the flake the card is about.
  Teeth, no-Curios arm, all measured: SHIPPED probe + a planted wave at tick 200 (the grid's own blocks —
  `dirt` / `cobblestone` in the crop cells, the source replaced at y=-1, `grass_block` over the headroom)
  → RED `the crop patch lost a cell to a non-crop state at tick 201 … vanished=2 of 8 cells, repaired=0;
  … state=Block{minecraft:dirt} … raw=0`; the FIXED probe + the SAME wave → PASS, 529 required tests, with
  the pad's writes printed; the FIXED probe with the `PadMode.SERVICE` call commented out + the SAME wave →
  RED at tick 200, so the per-tick service is the lever, not the plant tick. The same wave at tick 20 —
  before `PLANT_TICK` — is green on both trees: the fixture's own writes overwrite it, which is why the CI
  shape is a mid-window arrival and not a setup miss. That planted wave also reddened a sibling
  (`combat_golemopensarealdoorandclosesitbehindit`) on the fixed tree's leg, which is the packing-grid leak
  in the other direction, caught in the act. Local legs cannot reach the light arm: inside the box the
  section's light is never computed (the census reads `raw=15`, the uncomputed value, on every leg), so
  locally the channel's rate is ~0 (15 legs, 0 reds on three trees) and the CI arm's rate is 2/N — which is
  exactly why the pop's arm cannot be caught after the fact (the crop dies permanently on the first
  neighbour update inside the window) and the precondition assert has to be airtight. The tell for the next
  red on this lane is the pad census: `relit=0` on a light-assert red is the light engine, `relit>0` is the
  wave eating a source and the printed writes name it; a vanish arm naming a cell with a foreign block names
  the contaminant state. Test-side only — the product (a player farm under a slope) is untouched.

<a id="empty-stack-sentinel-save"></a>
- **A `!= null` guard is 1.7.10's empty sentinel: 1.21.1's empty stack is NON-NULL, its `save`
  throws, and the entity-save path catches that by deleting the whole entity.** 1.7.10 stored "no
  carried item" as `null`: the golem's read is
  `ItemStack.func_77949_a(nbt.func_74775_l("ItemCarried"))`
  (`reference/decompiled/thaumcraft/thaumcraft/common/entities/golems/EntityGolemBase.java:643-644`)
  and 1.7.10's `loadItemStackFromNBT` answers null for the empty compound; the port's write guards
  copied that habit (`:588-594` is the `!= null` item arm, `:580-581` the bucket's). 1.21.1's empty
  value is `ItemStack.EMPTY`, which is non-null, and `ItemStack#save` refuses it
  (`ItemStack.java:419-421`, the encode refusal) while `saveOptional` (`:428-430`) writes the empty
  compound; vanilla's own hand-item arm is that same gate-then-write (`Mob.java:394-401`:
  `isEmpty()` → an empty compound, else `save`). `ItemStack#parseOptional` hands that EMPTY back
  for the original's empty compound (`ItemStack.java:299-301`: `tag.isEmpty() ? EMPTY : …`) — so the ORIGINAL'S OWN BYTES,
  read on 1.21.1, turn a field that was null into a field that throws. `FluidStack#save` has the
  same gate (`FluidStack.java:283-285` throws `Cannot encode empty FluidStack`, `:294-296` is the
  gate, `:196` parses that EMPTY back out of a no-key write).
  Two shapes of consequence. **(1) The frame that catches it drops an ENTITY, not a stack.**
  NeoForge's patched `EntityStorage.storeEntities` wraps each entity's `Entity#save` in a catch
  that logs the per-entity "has thrown an exception trying to write state. It will not persist"
  line and never adds the tag to the chunk's `Entities` list: the mob is deleted by a world save,
  silently (the item-component bullet `component-codec-save-path` loses one item; this loses the
  actor). **(2) The FIRST save of a freshly constructed object cannot see it** — the ctor leaves
  the field null (the original's ctor, and `setCarried` on the port), so the throw needs a load in
  front of it. The probe's teeth are therefore the SECOND cycle, save → load → save: a one-cycle
  round-trip
  (`golemStateSurvivesSaveAndReload`, `gametest/GolemRuntimeGameTest`) is green over a total loss,
  and an assert on the live field is green too. A probe must also not read a healing getter before
  that save — the original's `getCarried()` routes an emptied field through `setCarried(null)`
  (decompiled `EntityGolemBase.java:804-810`; the port's copy is `entities/golems/EntityGolemBase.java:737-740`),
  and a probe that asserted on it first measured a golem that can
  never reproduce the census (measured, this card's first leg). Census for the lane:
  `grep -c "has thrown an exception trying to write state"` per retained leg log, split per entity
  type (`entity.thaumcraft.golem` vs `entity.minecraft.item`) — the harness prints
  `All N required tests passed` while the signature rides the shutdown save, so leg exit codes say
  nothing. Three census hygiene rules, all measured on this card's legs. (1) Score the **entity**
  variant: the same phrase is logged by the BlockEntity arm (`A BlockEntity type … has thrown an
  exception trying to write state. It will not persist`), and on a leg where a probe FAILED the
  harness writes that probe's over-length report into a lectern's book, which the 1024-char string
  cap rejects — a red leg's raw phrase count is then inflated by the harness, not by a save loss
  (measured: 1 such hit on a leg red with `interact_returnhomeusesthehomeradius…`, `LecternBlockEntity`,
  `String "interact_returnhome… " is too long: 1105, expected range [0-1024]`). (2) Do NOT paste
  the verbatim phrase into a probe message: the census counts it and the probe inflates its own
  measurement (measured: 1 self-match, this card's first leg). (3) The `Cannot encode empty …`
  strings appear in probe failure text too, so read them as diagnostics, not as the census. The fix shape is to gate the encode
  (`saveOptional`) while keeping the original's bytes (the empty compound, or no key at all) and to
  normalise the read back onto the sentinel; the mutation tooth is restoring the product hunk,
  which reddens the second-cycle probes by name.

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

<a id="deferred-chunkgen-stamps-pad-cells"></a>
- **The producer of a probe cell can be the mod's OWN deferred chunk generation, not a neighbour pad — and for a tile-cast probe the claim must remove the BLOCK ENTITY, not just the block.** The altar/lock/spawner probe (`EldritchRealmGameTest.altarLockAndSpawnerRunTheirCounters`) died with a raw cast failure on a with-Curios leg of `af948d75` (`altarlockandspawnerruntheircounters failed at 7385228, -60, -10213806! class com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchCap cannot be cast to class com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchLock`, leg at 18:41:14): the probe read a `TileEldritchCap` in its LOCK cell — rel `(1, 0, 0)`, a mod block, so the producer is a foreign FIXTURE, and the 1×1×1 pads stride 6 cells with the probe's cells far from any pad edge, so it is not a neighbour template. The ownership capture (t_1dd9381c's shape: a delta WRAPPED on the producing call, ownership logged at the action on every leg) names the producer as the mod's deferred worldgen pass, not a probe at all: **854 of 867 cap-tile constructions sit at PAD coordinates (x/z in the millions, y=-60) across 6 with-Curios legs** of `af948d75`, and the producer stack is `AuraNodeChunkGen.onServerTickPost:75 → MagicChunkHooks.runPendingChunkGen:79 → StructureChunkGen.generateChunk:89 → rollStructures:163 → WorldGenEldritchRingGen.generate:156 → StructureGen.set:125 → Level.setBlock → BlockDeviceBase.newBlockEntity:68 → DeviceTileFactory.create:63`. The harness's pads live in millions-coordinate chunks; the probes force chunk access, the mod's hooks queue that chunk's generation, and a random server tick runs the pending queue and stamps eldritch-ring structures into the pad's own cells. `setBlock` over a standing block entity KEEPS that entity (skill fact, TC-M4-06-p), so when the stamp lands between the probe's plant and its read, the STATE is the probe's and the ENTITY is the ring's, and the cast dies naming the ring's tile. The ring rolls per chunk, so contamination is a rate, not an arm: with-Curios **1/6** on `af948d75` (the 18:41:14 red leg + 5 own trace legs 0/5) and **0/5** on base `bccc2312` (mergedcur2 x2 + 3 legs on this card), NO-Curios **0/5 on each tree** (10 legs) — Fisher 1/6 vs 0/5 p=1.0: the rate is tree-independent; the parent's "shape appears only on that arm" reading (n=1) does not survive its arms, and the main-CI warp-scan reds that named `dirt`/`grass_block`/`cobblestone` (run 43748694926) are the same deferred-terrain channel drawn on the NO-Curios arm. Shape (t_9687e91a): the tile-cast probe claims the cells it CASTS — air + `removeBlockEntity` (air alone leaves the stamped tile standing under the plant; the entity is what the probe reads), the pad's own structure block counted as the grid marker and not a foreign block, every leg logging `claimed the 3 cells … cleared N foreign block(s) [names]` as the channel census (`cleared 0` = clean handoff, a named clear = the deferred pass caught live); casts go through a checked `ownTile` that names the foreign entity and the state, so future contamination reddens with the channel instead of a raw CCE. Cites: `docs/testing.md#sweep-hazard-set-is-scan-set` (the claim covers every arm the gate reads — the tile-cast arm reads block entities, not just blocks) and `docs/testing.md#scan-claim-covers-box-cells` (the cell set is the probe's own arm set; a stride-6 neighbour claim is the wrong set because the producer is the pad's whole chunk). Teeth — `blockeldritch[variant=3]` (the cap) planted at rel `(1, 0, 0)` with the plant printed on the leg: shipped tree RED byte-for-byte the 18:41:14 cast line (leg teethA, plant line on the leg, CCE text identical), fixed tree GREEN 515/515 with the claim naming the planted cap in its cleared census (leg teethB), and fixed tree + plant kept + claim disabled RED again naming `got com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchCap... under state Block{thaumcraft:blockeldritch}[variant=8]` (leg teethC) — the claim is the lever. Fixed-tree with-Curios legs reddened twice on SIBLING lanes: `taintcreeperkeepsitshandrolledfuseandblast` (the disclosed t_21b2160d family) and `combat_butchercullstheoldestanimalonlyinaherdofthree` (`cell ... is air with a solid floor under ... got cell=Block{minecraft:grass_block} floor=Block{minecraft:dirt}` — the terrain side of this same deferred channel standing in a golem-pad cell). Those lanes are named by this rule, claimed by neither fix: the sibling pads need their own claims. Fix arm matrix (`8ea37dd4`): NO-Curios 5/5 and with-Curios 4/5 with THIS probe green on all 10 legs (the 5th with-Curios leg reddened on the guardian lane, `theguardianbreathesforfivehundredticksandburns` reading out-of-water air -20 — water-tank cells, same channel, logged to the sibling card); the claim census read `cleared 65 foreign block(s)` on one green NO-Curios leg that still passed 515/515 — the deferred pass stamped 65 blocks into the cast cells and the claim removed them under green, the channel caught live. CI 3/3 on `8ea37dd4` (run 43807906429), `build` SUCCESSFUL. Probe census 515 → 515 (existing probe, no new rows).**

<a id="deferred-chunkgen-gated-in-gametest"></a>
- **The producer `docs/testing.md#deferred-chunkgen-stamps-pad-cells` names is a HARNESS problem before it
  is a probe problem: gate the block-writing pass on the game-test server, and gate the hook that WRITES,
  not the queue.** Four lanes went red on the same channel (golem pad support cell `grass_block`/`dirt`,
  guardian walkback arm reading `got -20`, warp-orb arming cell `dirt`, taint-creeper LOS arm closed), so
  the lever is one gate, not four claims. Shape (t_764d1e8e): the `gameTestServer` run config arms
  `-Dtc.noChunkGen` (the `tc.*` precedent — `tc.noCurios`, `tc.contentMappingFile`) and `-Ptc.chunkGenOn`
  re-arms the pass for a measurement leg. **Gate the STRUCTURE hook, not the queue:** the first shape
  cleared `MagicChunkHooks.PENDING` inside `runPendingChunkGen`, and on 2/2 with-Curios legs that leg
  verdict was `1 required tests failed: loadedchunksflowthroughthetickdriver` — the lane that asserts
  `data.isMagicGenDone(chunk)` after the driver drains, i.e. the lane whose SUBJECT is the queue; the
  census counted 1454 and 1444 pending chunks dropped on those two legs. `StructureChunkGen.generateChunk`
  is the block-writing hook (the aura hook records vis density and writes no blocks — the stamp shapes the
  producer rule names, `blockeldritch`/`dirt`/`grass_block`/`cobblestone`, all come out of the structure
  family), so gating it removes the producer and keeps the driver lane live: gate ON 10/10 GREEN (5
  NO-Curios + 5 with-Curios, 529/529 each). **A gate has to prove the pass RAN**, else "gate ON and green"
  is indistinguishable from "the pass never ran": the gate counts the chunks it skips and logs the first
  and every 200th (`CHUNKGEN-GATE -Dtc.noChunkGen armed: the deferred structure pass skipped 1400
  chunk(s) …` — 7-8 lines per leg on the grid). **A gate is not a claim**: the planted pair proves the
  difference — the stamp shape planted by the probe (grass_block in the butcher pad's support cell at
  SETTLE-10, dirt in the orb pair's plain arming cell right after its setup claim, cobblestone in the
  guardian's cell at tick 21) reddens the lane WITH THE GATE ARMED, because the gate removes the producer,
  it does not clear a cell; the mid-drive arrival stays covered by the per-probe claims
  (`docs/testing.md#orb-claim-covers-asserted-cells`, `docs/testing.md#scan-claim-covers-box-cells`).
  **Census, the card's question "which producer stamps pad cells (ring vs aura-vein vs terrain)":** the
  structure family funnels every write through THREE sites (`StructureGen.set`, `StructureGen.setAir`,
  `StructureGen.setVine`) — instrument those, register each lane's asserted region at setup, and print the
  producer as the first `worldgen` frame under the helpers plus the pass tag. On the gate-OFF arms
  (pass live, `-Ptc.chunkGenOn`) 8 legs across both Curios arms read **`stamps=0` inside every registered
  region**, and the per-lane sweeps read the region's own fixture census unchanged (`nonAir` identical at
  setup and at the assert tick, e.g. the butcher pad 2160 → 2160 = beacon/stone/glass/barrier/
  structure_block/command_block, the guardian 138 → 141 with the +3 named as `lava`+`blockmetaldevice`
  from the OVERLAPPING neighbour pad, not from the pass). So the natural hit rate of this channel on the
  THREE lanes that exist on main is **0/8 legs on this grid**, the arms cannot separate the gate from its
  control (gate ON 10 green vs gate OFF 10 green, Fisher p=1.0), and the causal leg is the planted pair,
  as `docs/testing.md#scan-claim-covers-box-cells` prescribes. Two disclosures: the fourth lane,
  `taintcreeperkeepsitshandrolledfuseandblast`, has no probe on main (it lives on an in-flight sibling
  branch), so no gate on a main tip can be measured on it; and the reds the producer rule logged came from
  legs of in-flight trees whose pad grid differs from main's, where the sweeps here cannot land.
  **Census (t_764d1e8e): teeth 5 legs, all with-Curios, every leg printing its THREE plant lines — the shipped tree with no gate property at all (the pass live) RED 2/2, the gate tree with the gate ARMED (7-8 `CHUNKGEN-GATE … skipped N chunk(s)` lines on the leg, so the pass ran and was skipped) RED 2/2, and the gate tree re-armed with `-Ptc.chunkGenOn` RED 1/1; every red is `combat_butchercullstheoldestanimalonlyinaherdofthree failed at … the golem's pad: cell … is air with a solid floor under … got cell=Block{minecraft:grass_block} floor=Block{minecraft:stone} (tick 120) — a blocked cell is the block-support precondition closing under the pad` — the card's CI line is the same assert at the same tick with the same `cell=Block{minecraft:grass_block}`, and the FLOOR token is the byte-diff (`floor=Block{minecraft:dirt}` on the CI leg, `stone` on this tree's grid, where the pad's floor is the template's stone). The other two planted shapes did NOT redden their lanes on this grid (cobblestone in the guardian's own cell, dirt in the orb pair's plain arming cell, both planted after the lane's setup claim): the guardian's `got -20` walkback red and the orb clamp steal need an arrival inside the drive window, not a setup stamp, so this card mints no teeth for those two lanes and the per-probe claims stay the lever there. Arms on the gate tree (base `c4fe41ed`, probe census 529): gate ON NO-Curios 5/5 + with-Curios 5/5 GREEN 529/529 with 7-8 gate lines per leg, gate OFF (pass live) NO-Curios 5/5 + with-Curios 5/5 GREEN 529/529 — Fisher 10 vs 10 green, p=1.0, NO separation, which is what the 0/15 census says the natural rate is on this grid; the gate-OFF legs also cost wall time the gate-OFF grid explains (59-181 s per leg vs 58-89 s armed). Producer census legs: 15 (4 + 5 gate-OFF with the v1 tree, 2 gate-ON v1, 4 on the v2 census tree), `stamps=0` in EVERY registered region on EVERY leg. v1-gate red legs: every gate-armed leg on that tree went RED — the 2 completed with-Curios legs name `loadedchunksflowthroughthetickdriver` in the summary block with 1454 and 1444 pending chunks dropped, and 3 NO-Curios legs printed `1 required tests failed` each before the driver was killed, their logs cut before the lane line, so the lane identity in those three is unconfirmed here. `./gradlew --rerun-tasks build` rc 0 (unit XML 1186 tests / 0 failures / 0 errors / 2 skips), probe census 529 → 529 on the card tip (no probe added; the planted/census trees read 533 because main moved under them), cite gate on the landed tip 62 cites / 0 stale / 0 legacy. The card's own harvest-cite redirect is VOID AT LANDING and the merge keeps MAIN's cite text: main minted the kebab those cites already asked for in d253b67c (`harvest-pad-claims-its-light-layer`) and retired the kebab the branch redirects TO in f255dc5a (`harvest-pad-claims-and-services-its-cells`), so landing the redirect would have made 2 cites STALE (measured on the raw auto-merge: 62 cites / 2 stale / rc 1) — the same two fixes, ruled for main's landed pair. CI 43862280324 on the code tip `fe10290c`: `build` (with-Curios game tests), `game-test-no-curios` and `runServer` all success with `-Dtc.noChunkGen=true` armed in the gameTestServer run config — the dedicated-server job proves the gate touches only the harness run config.****

<a id="mob-write-cells-ride-the-int-cast"></a>
- **A mob's block-write cells ride the `(int)` CAST of its raw position, not `blockPosition()`, so a
  probe asserts the cell the BUILDER computes and claims that same list.** 1.7.10 computes a write cell
  as `(int)this.posX + offset` (`EntityCultistPortal.java:144`, `:146`, `:186`); a mob stands in the cell
  CENTRE (`x = cell + 0.5`) and a Java cast truncates toward ZERO, so on the negative half of an axis the
  cast origin sits ONE BLOCK HIGHER than the cell `blockPosition()` reports — the same rule a summon arm
  pins for a minion's home centre (`BossMobGameTest:235`, `EntityCultist.java:135-137`). A probe that
  pins `padCell + offset` asserts a cell the builder never touches, and a builder that computes its
  offsets about `(0,0,0)` writes at the WORLD origin and never touches the pad at all: the Crimson
  Portal's banner wall had BOTH defects at once — the write arm passed a `(0,0,0)` origin into
  `bannerCell()` while the probe asserted `padCell + offset` — so the pad's wall cell read
  `Block{minecraft:air}` on 3/3 arms (with-Curios ×2, noCurios) from the probe's own commit onward, with
  the pad handed over clean (`cleared 0`, `swept 0`) and the probe's census printing no per-cell text
  (c5-2 gate record `gate_record.md`, card t_2f9c86c8). Read the reference's write site before ruling
  which side moves, then fix the BUILDER and the probe's cell list AS A PAIR: the builder passes the
  cast origin, and the probe computes its expected cells by calling the port's own cell expression from
  that same cast and passes THAT list to the claim
  (`docs/testing.md#orb-claim-covers-asserted-cells`) so the claim, the census and the asserts ride one
  list.
<a id="pad-write-lands-only-in-a-loaded-chunk"></a>
- **A world write on a cell whose CHUNK is not loaded is dropped SILENTLY, so a pad's far cells have to
  be armed, claimed and asserted per loaded-CHUNK, not by "is it my origin chunk".** 1.21.1's
  `Level#setBlock` asks the chunk source for a FULL chunk and a server answers null when it is not
  loaded, so the write is never applied and never reported; and a game-test template force-loads only
  its OWN intersecting chunks (`StructureUtils#forceLoadChunks`), so on a packed pad grid a pad's far
  cells ride the NEIGHBOUR templates' forced chunks and the unforced gaps between them. Consequences: a
  claim that writes such a cell is a no-op there (census it and skip it), an assert on it must be stated
  per cell as loaded→written / unloaded→air, and a blanket "outside the origin chunk ⇒ air" is
  OVER-SPECIFIED, because a neighbour's forced chunk does carry the write. Measured on the Crimson
  Portal's banner wall — 6 blocks out from the cast origin, on the stride-6 pad packing — the loaded
  cell set is a function of the pad's offset inside its own chunk, 1..4 of 4 cells across the three
  arms (c5-2 gate record, card t_2f9c86c8).
<a id="follow-up-hurts-clear-the-ten-tick-invuln-window"></a>
- **A scripted follow-up hurt has to clear `LivingEntity#invulnerableTime`, because 21.1.259 arms the
  10-tick window on a hurt the target's immunity REFUSES, and a damageability control behind a
  throwing assert is unmeasured.** Two measured facts from the Crimson Portal's upkeep leg (card
  t_2f9c86c8, 3/3 arms). (1) A refused hurt is NOT inert: the fire-immune statue's refused
  `DamageTypes.DROWN` hurt records its source (`lastSource = DamageSource (drown)`, `damage 0.0`), so a
  refusal reads as `landed == 0.0` PLUS the source's type — `getLastDamageSource() == null` is
  over-specified against the port's design and is not a way to detect one. (2) The same refused hurt
  arms vanilla's 10-tick damage cooldown, so the melee fired on the following line is refused by the
  PLATFORM and lands 0.0: the leg's control (`the statue is damageable: a mob-attack 5.0 …`) reported
  `got 0.0` on 3/3 arms with the statue intact. Clear the window (`mob.invulnerableTime = 0`, this
  file's established reset at `BossMobGameTest:373` and `:561`) and pin BOTH sides — in-window lands
  nothing, cleared lands the armour-scaled hit — so the mechanism is measured, not assumed. (3) Control
  asserts placed AFTER the primary assert never ran on any leg whose primary threw: the pre-fix tree's
  primary threw, so the "controls PASSED" that shipped on the card was not evidence from the gate legs.
  Order a control BEFORE the assert that can throw, or state in the card which arm measured it.

<a id="mob-tick-claim-covers-mob-chunk"></a>
- **A probe that gates on MOB ticks has to claim the mob's CHUNK — and survive that claim's LOAD: every
  probe runs on a 1×1×1 `empty` template, so the harness force-loads the pad corner's chunk only, and a mob
  armed one block off the corner stands in an unticketed chunk in every slot whose corner lands on a chunk
  boundary.** `StructureUtils#forceLoadChunks` (framework:165-170) forces `boundingBox.intersectingChunks()`
  — for `template = "empty"` the ONE chunk holding the pad corner — and `ServerLevel#tickEntities` (:390-395,
  411) ticks an entity only where `getDistanceManager().inEntityTickingRange(entity.chunkPosition())` holds,
  i.e. level-map ticket ≤ `ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING)` = 31 (`ChunkLevel:47-49`
  `isEntityTicking(level) = level <= 31`), and the iteration visits the entities of **LOADED** chunks only.
  `EldritchMobGameTest#spawn` arms every family mob at `pad(1, 3, 1)` = corner + (1, 3, 1), so the mob sits in
  the corner's own chunk when `corner.x mod 16 ≤ 14 && corner.z mod 16 ≤ 14`, in a **4-neighbour** chunk when
  one coordinate crosses (measured `entTick=true`, ticks fine), and in the **DIAGONAL** neighbour when both
  cross, which does not inherit: measured `entTick=false`, **`mobTick=0` from the first sample for the probe's
  whole 400-tick life** with the mob `removed=false inLevel=true`. Both disclosed reds of the walkback lane sit
  on a corner with `x mod 16 = 15` — CI `failed at 1657407, -60, 3623897` (job 133343432981, run 43880111452,
  green on the rerun of the same sha) and local `failed at 10720063, -60, -3829490` (`gate1_leg_noCurios_2`) —
  so the lane's `-20` is **the mob running NO entity ticks** (the air never walks back), not the deferred
  terrain channel: that leg carried 8 `CHUNKGEN-GATE … skipped N chunk(s)` lines (the producer OFF), the
  landing census separated nothing (Fisher 2×2 [[1,5],[0,5]] p=1.0, base `f255dc5a` 5/5 green), and
  t_764d1e8e already disclosed that a cobblestone stamp in the mob's own cell at tick 21 does not redden this
  arm. The producer is a CHUNK TICKET — a harness channel, not terrain. **Shape: per-probe, not a harness
  gate** (the state is chunk-local — the CI red is 1 test of 533 with 533 pads on the grid; a level-wide gate
  cannot put a ticking ticket on a chunk it does not know; forcing every pad's chunks is the grid-load packing
  the standing hazard t_44055375 punishes). The claim pulls the harness's OWN lever — `ServerLevel#setChunkForced`
  (`ForcedChunkManager.addForcedChunk` → `TicketType.FORCED_TICKET` + the level's forced set, which
  `GameTestRunner#runBatch:100-103` clears for the WHOLE level at every batch end, so a setup-time claim cannot
  outlive its batch) — and it also satisfies the `!players.isEmpty() || ForcedChunkManager.hasForcedChunks(level)`
  precondition at `ServerLevel:390`, the 300-tick `emptyTime` wall. NOT `DistanceManager#addTicket(TicketType.FORCED,
  …)`: on an unloaded chunk that ticket is queued as pending and the level map keeps `MAX_TICKET_LEVEL`, so a probe
  reading mob ticks 20 ticks after setup measures `entTick=false` on the chunk it just claimed (measured
  `fix1_leg_4`: `armed=true entTick=false`, 0 of 20). **No probe releases a claim**: the forced set is SHARED with
  the harness and every sibling claim in the batch, so unforcing a chunk a sibling forced freezes that sibling
  (t_44055375); the batch-end level-wide clear is the release. **A rate is not a green**: this lane reddens ~1/5
  no-Curios legs (t_9687e91a's row: 1 of 5 with-Curios; t_c36620d6 measured the CI arm `game-test-no-curios` 40
  green / 5 red over 45 runs), so a streak of greens proves nothing and the teeth are a pair, not a streak.
  **Four measured rules set the shape of the pair.** (1) **The plant has to sit OUTSIDE the batch's forced band,
  and the probe asserts it.** The batch packs pads along `+z`: measured pad-corner chunks x `462695..462702` =
  **eight chunks of x** (z `769533..769646`) on `fixcur3_leg_2`. A plant at (+16,+16) is the 4-neighbour, which
  ticks with the claim OFF (`mut1_leg_2`: `armed=false entTick=true`, 20 of 20); at (+64,+64) = corner + (4, 4)
  chunks it is inside the band plus its one-hop `level+1` propagation, so the mutation arm is a rate — measured
  **claim-OFF GREEN 3 of 4 legs** (`mut3_leg_1..4`), one red. The plant is therefore **512 blocks in `-x`** = 32
  chunks out, 4× outside the band, and the probe fails fast on the premise (`the plant has to arm the mob OUTSIDE
  the batch's forced set`, asserting `mob chunk x <= corner x - 16`). (2) **The claim follows the mob's LIVE
  chunk, sampled every 5 ticks.** A fixture mob armed inside a neighbour pad's geometry is shoved out of it:
  measured mob chunk = corner + (5, 4) for the (+64,+64) plant (`fix3_leg_5`) and corner + (2, 1) for the
  (+16,+16) plant (`fixcur2_leg_2`), and the with-Curios leg `fixcur3_leg_2` ran 16 entity ticks in the head then
  **0 of the last 40** with `entTick=true forcedHere=true` on the chunk captured at spawn — the mob had left it.
  (3) **A self-rescheduling `helper.runAfterDelay` chain must allocate a FRESH lambda at every re-arm.**
  `GameTestInfo` stores delayed tasks in an **`Object2LongMap<Runnable>` keyed on the task OBJECT**
  (framework:34,169-171) and `tickInternal` fires an entry with `entry.getKey().run()` and THEN
  `objectiterator.remove()` (:127-139): a chain that re-arms with the SAME `Runnable` instance has its re-arm
  removed by the very iterator that ran it — it dies after the first sample and the probe ends as
  `GameTestTimeoutException: Didn't succeed or fail within 400 ticks`. Measured on `fix4_leg_1..6`: 6 of 6 legs,
  both arms, one census line at tick 5 and no failure text (the sampler had a `Runnable[]` self-reference);
  `fixcur4_leg_1..3` the same on the with-Curios arm. (4) **A force is a LOAD request, and the tick loop visits
  entities of LOADED chunks only.** `setChunkForced` writes the level map at once (the ticket census reads go
  true immediately); the FULL load it requests has to pass the batch's shared queue. Measured `fix5_leg_3`: 285
  ticks, `entTick=true forcedHere=true`, mob alive, unremoved, in the level's entity list, **0 entity ticks in 57
  samples** — the chunk never reached FULL, so the mob was never in the tick loop at all. So the claim is taken at
  **setup, before the mob exists** (the load queues for the probe's whole life), the census deadline is a LOAD
  deadline (**600 ticks, with the harness `timeoutTicks` behind it at 900**, not in front of it), and
  `MobChunkClaim#loaded`/`#status` sit next to the ticket reads so a red names the LOAD, not the ticket.
  **Teeth (`theGuardianArmedOneChunkOffThePadCornerStillTicks`, the -512 plant, claim at setup, sampling every 5
  ticks, re-claiming the live chunk):** armed passes only on a **40-tick ticking run** — `entTick=true
  forcedHere=true loaded=true` at every sample AND the mob ticking every tick of the run — so the claim's own
  load cost never reddens the armed arm (measured arrival ~77 ticks for a chunk 4 chunks out, `fix3_leg_1`; the
  -512 plant passes at samples 12-23 = ticks 60-115); mutation `-Dtc.noMobChunkClaim` (gradle
  `-Ptc.mobChunkClaimOff`) reads 0 ticks with `entTick=false` and fails with the census at 600, naming the probe.
  **Census (t_f9fe6e81, gated tree `f3294226`, the merge of main `7413157e` with the triage tip `a2f95886`):**
  mutation arm **RED 3 of 3** (`mut7_leg_1..3`: 121 samples over 600 ticks, longest ticking run 0, `entTick=false`
  — with no claim the chunk is not even ticketed); armed **NO-Curios GREEN 3 of 3** (`fix6_leg_1..3`) and
  **with-Curios GREEN 2 of 2** (`fixcur6_leg_1..2`), every armed leg `All 548 required tests passed` with the
  verdict line `plant verdict GREEN … ticking run 40` — **no sibling probe reddened by the batch's 16 claims**;
  probe count **548 runtime**, static `@GameTest` census **633 = main 632 + 1** over the same 83 gametest files,
  no sibling probe retired; the walkback lane probe `theGuardianBreathesForFiveHundredTicksAndBurns` **green on
  every one of the 14 local legs** that carried the fixture claim; `build` **rc 0** — unit **1231 tests / 0 failures / 0 errors / 2 skips** over
  127 in-window XML, gameTest **548/548 on 3 merged-tree armed legs**, **66-88 s/leg armed and 104-122 s/leg on the 600-tick mutation
  legs** at load average **1.9-5.2 on 8 cores**, `build` **12 s**, cite audit **100 cites / 0 stale / 0 legacy rc 0 at the landed tip (origin/main `2cc8df52` measures 87 / 0 / 0 rc 0; the delta's 13 cites resolve on this row)**, zero-residual
  **3 files, 368 insertions / 1 deletion, 0 D rows (build.gradle 10/0, EldritchMobGameTest.java 210/1, MobChunkClaim.java 148/0) + this row, additive** (two java files + `build.gradle`'s claim-off property + this row; the docs commit is Markdown-only
  and its cite audit re-ran at the landed tip). Disclosed on the way, each measured on this card: the v1 (+16,+16)
  and (+64,+64) geometries above; the identity-bug reds (`fix4_leg_1..6`, `fixcur4_leg_1..3`); the cold-load red
  (`fix5_leg_3`) that rule (4) fixes; v3's first armed sample **green 2 of 3 at 540/540** (`fix5_leg_1..2`) and the mutation-arm flag-name trap (`mut6_leg_1..3` read GREEN 3 of 3 because the driver passed `-Ptc.noMobChunkClaim`, the SYSTEM property name, where gradle reads the PROJECT property `tc.mobChunkClaimOff` — an OFF leg that is silently ON; `mut7` with the right flag is the measuring arm); and a
  CI-arm rate measured on this repo's **last 120 runs** — 22 run-level failures, with **3 shas green on one ref and
  red on another at the same sha** (`2eda098b`, `a069e53e`, `4229b84e`), i.e. an unchanged tree reddens, which is
  the same floor t_c36620d6 measured as 40 green / 5 red over 45 runs.

<a id="orb-claim-covers-asserted-cells"></a>
- **A probe that asserts AIR on cells outside its own template box has to TAKE those cells at setup: a
  1×1×1 pad's clear pass reaches the corner cell only, so the assert reports the harness's terrain as a
  product defect.** `docs/testing.md#sweep-hazard-set-is-scan-set` says the claim has to cover every arm
  the gate reads and `docs/testing.md#scan-claim-covers-box-cells` says the cell set is the gate's
  block-arm set; the warp-ORB probe is the case where the set is wider than the pad in the other
  direction. `CultistClericGameTest.theOrbLifetimeClampDiesAtSixteenForPlainAndTwoFortyForRed` arms its
  plain+red pair at rel (1, 4, 1) and (2, 4, 1) — FOUR cells above the corner — and asserts those cells
  are air, because a solid cell is a real removal path (`EntityGolemOrb:63` discards on a block hit,
  measured at age 162). Its template is `empty` (1×1×1), so `StructureUtils#clearSpaceForStructure`
  clears exactly the corner cell, the stride-6 grid packs sixteen of these pads inside a 24-wide
  footprint, and the worldgen terrain wave the grid leaves behind (this lane's reds: `dirt`) stands in
  the arming cells no structure pass reaches. Measured on the landed tree `bccc2312`+`t_d8944aae`,
  NO-Curios leg `merged5#1` at 17:52:32: `PROBE BROKEN: the orb cells must be AIR — got
  Block{minecraft:dirt} at BlockPos{x=-8356951, y=-56, z=-7582238} and Block{minecraft:dirt} at
  BlockPos{x=-8356950, y=-56, z=-7582238} (pad corner BlockPos{x=-8356952, y=-60, z=-7582239})` — 1 red
  of 5 legs, legs 2-5 green, so the assert is a rate, not a defect, and it costs the suite the clamp's
  edge. **Blocks AND entities:** the block is the named hit path, and a foreign mob in an arming cell is
  a removal at age 161, which is exactly the plain clamp's signature age the probe pins (
  `docs/testing.md#removal-age-names-mechanism`), so the mechanism assert cannot separate it from the
  clamp — the claim sweeps non-player entities too. Shape (t_b98f2601): ONE list
  (`CultistClericGameTest.ORB_CELL_OFFSETS`) feeds the air assert, the claim and the `setPos` arms, so
  the claim set cannot drift from the gate's block-arm set; the claim asserts its own size (one cell per
  orb of the pair, naming the offsets) so moving the arming geometry moves the claim and says so by
  name; it runs at setup BEFORE the probe's fixtures (a claim after the fixtures eats them); and it logs
  `cleared N foreign block(s) … swept M foreign entity(ies)` on EVERY leg — `cleared 0` is the pad handed
  over clean, `cleared N` names the wave, which is the channel census the scan lane reads the same way.
  Teeth: plant the CI's `minecraft:dirt` in the arming cells BEFORE the fixtures — shipped probe RED
  byte-for-byte the CI line, fixed probe GREEN on the same injection naming the dirt in its `cleared`
  census, and the claim reverted with the plant kept RED again, which makes the claim the lever and not
  the plant's tick. **Census (t_b98f2601): teeth, NO-Curios, the throwaway plant helper writes the CI's `minecraft:dirt` at rel (1, 4, 1) and (2, 4, 1) FIRST and prints its plant line — shipped tree (`af948d75`) RED 2/2 naming the air assert byte-for-byte the CI line (legs `plantA#1/#2`, e.g. `got Block{minecraft:dirt} at BlockPos{x=-8062799, y=-56, z=8620314} and Block{minecraft:dirt} at BlockPos{x=-8062798, y=-56, z=8620314} (pad corner BlockPos{x=-8062800, y=-60, z=8620313})`), the same plants on the fixed tree GREEN 2/2 with the claim line naming them (`cleared 2 foreign block(s) [Block{minecraft:dirt} @ rel (1, 4, 1) = …, … @ rel (2, 4, 1) = …] swept 0 foreign entity(ies)`, legs `plantB#1/#2`), and the claim reverted with the plants kept RED again on the same cells (leg `plantC#1`) — the claim is the lever, not the plant's tick. Natural-rate control on the shipped tree with a log-only trace of the asserted volume at setup tick: 5/5 GREEN, `nonAir=0` on every leg — the wave is drawn CI/landing-side, matching the 1-in-5 landed-tree sample that opened this card and the 1-in-11 local read. Fix arms: NO-Curios 5/5 + with-Curios 2/2 GREEN, 7/7 claim lines `cleared 0 foreign block(s) … swept 0 foreign entity(ies)` (the pad handed over clean), `./gradlew --rerun-tasks build` rc 0 with the unit XML 1157 tests / 0 failures / 0 errors / 2 skips, probe census 515 → 515 on this card's base `af948d75` (main's census is 529 after the sibling boss-base and pech-drop landings; the 515-probe base legs are the grid this delta does not move). Fisher exact: natural legs 1 red/5 (the parent's `merged5` sample) + 0/5 (this card's trace legs) vs fixed 0/7 → p=0.42; pooled shipped 3/12 vs fixed 0/9 → p=0.23 — no streak arm reaches significance, so the DETERMINISTIC plant pair is the causal leg, as `docs/testing.md#scan-claim-covers-box-cells` prescribes. One disclosed foreign red on `plantA#1`: `combat_golemopensarealdoorandclosesitbehindit`, the route-outcome rate flake the Rules already carry ("A route-OUTCOME gate goes red as a rate, on the leg that loads more") — this delta adds no probes and the census is 515 == 515, so the packing grid is byte-identical.

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
<a id="route-arm-node-trail-names-the-channel"></a>
- **A route arm is a ONE-DRAW assert on a path graph: read the node trail's LAST cell and its
  `reached` flag before attributing the red, and triage the red as the arm's rate, never as a
  streak.** `GolemCombatAiGameTest.combat_returnHomeUsesTheRadiusAndTeleportsFromBeyond48`
  (`:1537-1542`) calls `AIReturnHome#start` and asserts in one draw that the navigation path has
  nodes, `canReach()`, and an end cell within 1.5 squared of the placer's centre; the trail is
  printed by the probe's own `nodeTrail` (`:428-438`). A clean corridor draws
  **`path[10,reached=true]`** ending at rel (12,0,13): 6/6 log-only trace legs on the shipped tree
  print `nonAir=0 nodes=10 reached=true` at the draw tick, no assert added. The CI NO-Curios red drew
  **`path[9,reached=false]`** (land-ref run 43837940910 attempt 1 @ `d06c9b46`) and its trail names
  the channel: node 0 sits at rel (12,0,22) — the cell the probe `setPos`s the golem to at `:1526`, so
  placement is NOT the failure and an assert on the mob's own cell would have passed — the route runs
  `-z` to rel z=19, then LEAVES the probe's floor row (rel y=-1, z=12..70, laid at `:1516-1518`) at
  (13,0,18), (14,0,17), (14,0,16), climbs (14,+1,15) and (14,+2,14) and stops. `PathFinder#findPath`
  calls a target reached at `distanceManhattan(target) <= accuracy` and, when no target is reached,
  reconstructs the **PARTIAL** path off the best open node with `reachesTarget=false` — vanilla
  `world/level/pathfinder/PathFinder.java:81-83`, `:116-120`, `:144`; the repo's `reference/decompiled`
  carries no pathfinder package, so read the vanilla source for it. A node count paired with
  `reached=false` is a graph truncation and its LAST node is the cell where the walkable component
  ended: read the trail, not the assert's first words.
  **Rate, published — this is what closes a route-arm red, and the CI tally needs an ATTEMPT-level
  read.** Arm = `game-test-no-curios`, window 2026-11-16T00:00Z → `4a9ab8df`, every branch, one line
  per run (`ci_lane.out`): **40 successes / 5 failures over 45 completed runs**, and all 5 reds name
  lanes OTHER than this one — the stage machine + CultistPortal summoner pair at `ea9976db` (run
  43886425981, job 133365685741) and 4 reds whose `--log-failed` returned 0 lines (`names []`:
  `86b3c57d`, `17634ba2`, `22f70ceb`, `2eda098b` — a red job log can be UNFETCHABLE and the probe
  name dies with it). This probe's two reds are ABSENT from that list for a tooling reason the next
  sampler has to know: **`gh run view` reports the LATEST attempt**, so run 43837940910 (`d06c9b46`)
  and run 43862872626 (`e3e52643`) both print `nc=success` in the harvest; their red attempts, fixed
  by `gh run rerun --failed` on the SAME sha, are invisible and their job ids cannot be selected
  (`select(.conclusion=="failure")` over 43837940910's jobs prints NOTHING). On the ATTEMPT level the
  arm carries 7 reds (5 final-red + 2 rerun-fixed) and this lane owns 2 of them: the route arm at
  `d06c9b46` (run 43837940910 attempt 1, `path[9,reached=false]`, green on attempt 2, job
  133215632537) and the drift arm at `e3e52643` (run 43862872626, job 133283875938, line `squared
  distance 1.7649174903511837 against a radius of 3.0 squared`, the tail
  `docs/testing.md#outcome-not-tick-budget` carries; green on rerun, job 133287911974): **2 lane reds
  in ~47 arm attempts ≈ 1/24, and 0 of the 5 final-red runs is this probe.** Locally:
  an interleaved 30-leg NO-Curios whole-grid census on this box (10 rounds, base `c4fe41ed` → landing `d06c9b46` → shipped main `4a9ab8df`, one gradle at a time, `ran=1`, `cachehits=0` and the built-in-fallback marker on all 30 legs, `loadavg(1m)` 1.97-6.26 with 0-3 JVMs, legs 59-110 s, `census2/`): this probe is **0/10 base, 0/10 landing, 0/10 shipped = 0/30** (Fisher base-vs-landing p=1.0, pooled with the card's first 6-leg pair p=1.0). The base arm reddened twice on lanes OTHER than this probe — `theguardianbreathesforfivehundredticksandburns` (the water-tank cells `docs/testing.md#deferred-chunkgen-stamps-pad-cells` names) and `interact_harvestgoalsareinertforthewrongcore` — 2/10 vs 0/10 on the landing arm, p=0.47; pooled with that first census (base 2/6 reds naming `therangedattackseamlaunchestheblastwiththeoriginalconstants` and the same guardian, landing 0/6, load 2.1-9.2 with 1-7 JVMs) the base arm sits at **4/16 vs 0/16, p=0.10** — a base-tree floor with no landing-tree delta, and the landing + shipped arms are 20 clean grid legs. The CI pair and the local box are not separable at this N (2 lane reds in 47 CI arm attempts vs 0 in 30 local legs, Fisher p=0.52)
  So a route arm owns no denominator of its own: at the CI's ~1/24 a 10-leg local arm has
  P(0 reds) = 0.65 and a 30-leg census P(0 reds) = 0.28 (Fisher base-vs-landing on THIS probe
  p=1.0), and the "green streak" a land runner wants is P(3 greens | 1/24) = 0.88 — the streak closes
  nothing. The node trail, the interleaved census and the plant pair are what close a route-arm red.
  **Plant pair (deterministic — the causal leg per `docs/testing.md#scan-claim-covers-box-cells`),
  all NO-Curios, every plant prints its own `[C3662]` line on the leg, verdicts by shape not streak.**
  Planted at the CI's mid-window (`pPath-1`, tick 122) on the SHIPPED tree: a single 2-tall column rel
  (12,0..1,13) **GREEN 2/2**, a 3-wide 2-tall wall rel (11..13,0..1,13) **GREEN 2/2**, the eaten floor
  row rel (12,-1,13..18) **GREEN 2/2** at `path[11,reached=true]`, the full trench rel
  (12,-1..-4,13..18) **GREEN 2/2** — a 2-tall step is walked OVER and a void is walked AROUND (the
  drop generator answers OPEN nodes), so "one block in the corridor" and "the floor is gone" are both
  negative controls. What reproduces the lane is the **stride-6 band**: 2-tall walls at rel
  x11..13, dy 0..1, z ∈ {13, 19} (12 blocks — the neighbour stride, two walls so the detour past the
  first one runs out of navigation cost) → shipped tree **RED 3/3** naming this probe, all three on
  the WALK arm: `the goal walked the golem from 10.0 blocks toward home, now 7.372057122171135 /
  7.372057121356423 / 7.372061974048622 away …, tick 160`. A 3-tall seal rel (11..13,0..2,13..18)
  → **RED 2/2** (6.41 / 6.41 away, tick 160); leg 1 ALSO reddened a neighbour,
  `falldistanceisclearedinthefirsttwentyticks` (`it takes no fall damage, got 19.0`) — my seal in its
  pad, a fixture leak of my own making, not a finding about that probe.
  **The lever the pair names is the DRIVE-TICK pass, not the setup claim:** the same band plant on the
  claimed tree with the drive service live → **GREEN 2/2** with
  `ROUTE-CLAIM drive cleared=12 relaid=0 [Block{minecraft:cobblestone} @ rel (11,0,13), …]`; the same
  plant on the same tree with the drive call removed (setup claim only) → **RED 1/1**. Lava, the malus
  shape, rel (11..13,0..1,13..18): drive live **GREEN 2/2**, setup-claim-only **RED 1/1**. A setup
  claim cannot clear terrain that arrives at tick 122.
  **The CI's signature shape reproduced at scale, not at one block:** the region seal rel
  (4..20, 0..2, 13..18) (306 blocks) with the drive service disabled reddened the ROUTE arm with
  **`path[4,reached=false]`** — a 4-node trail off the mob's own cell, the same truncation family as
  the CI's `path[9,reached=false]`. The node count says how far the cut is from the mob, not how much
  terrain moved: a `reached=false` trail is a walkable-component cut. No plant reproduced the CI's
  exact 9-node text locally, so the lane's producer is read from the CI red's own trail, and the
  plants name which shapes can produce that family. **Product tooth:** `AIReturnHome#start` never
  paths (`final boolean path = false;`) → **RED 2/2**: leg 1 names this probe (`outside the home
  radius it does: squared distance 100.25 against a radius of 3.0 squared (:33)`), its
  `interact_` sibling (`the golem did not path back inside the goal's home threshold (feet=…)`) and
  `combat_woundedguardretaliatesanddoesnotrecruititsneighbour`; leg 2 names this probe + the sibling.
  The route arm has teeth, so a green leg is a real draw.
  **Disclose the remedy that failed; do not rebuild it (t_c36620d6).** A corridor claim sized rel
  x4..20 × dy0..2 × z4..30 plus the row under it, run at setup AND on every drive tick, is NOT a fix
  for this probe: the seal ALONE truncates the route — the region plant (306 blocks, tick 119) with
  the drive service disabled reddened the ROUTE arm at `path[4,reached=false]` after
  `ROUTE-CLAIM setup cleared=117` (leg regionC) — and with the drive live the same plant reddened the
  WALK arm (`now 5.254573640… away … tick 160`) behind `setup cleared=76` →
  `drive cleared=309 [Block{minecraft:cobblestone} @ rel (4,0,13), …]` → `walk cleared=0 relaid=0`
  (leg regionB; leg 1 of that batch was a COMPILE red, `rc=1 ran=0` in 11 s, so it is a control and
  not a verdict). A sweep that wide moves the ground the probe's own walk phase stands on and floods
  block writes; **a `cleared=309` on a red leg names the sweep, not the channel.** Any remedy for this
  arm has to be sized to the cells the gate reads AND keep the walk row under the golem standing —
  the `docs/testing.md#scan-claim-covers-box-cells` discipline applied to the row, asserted not swept —
  and it has to be validated against the band plant (RED 3/3) and the 3-tall seal (RED 2/2), not
  against a green streak.
  Triage recipe for a route-arm red, in order: take the CI pair (`gh run rerun --failed` on the same
  sha), run the interleaved base / landing / shipped census with `loadavg` + `pgrep -c java` on every
  leg line, plant the shape the trail's last cell names plus its negative controls, publish both
  denominators with the Fisher exact, and disclose. The triage card owns none of the fix.
<a id="route-claim-covers-the-cells-the-route-gate-reads"></a>
- **A route arm's fix is a claim sized to the cells the gate reads, run as a DRIVE-TICK pass, asserted
  and relayed rather than swept — and the plant pair is its acceptance, never a green streak.** The
  triage rule (`docs/testing.md#route-arm-node-trail-names-the-channel`) demands a remedy sized to the
  gate's cells that keeps the walk row standing. This is the shape that satisfies it, measured on
  t_4c531ae6 (fix `d3cf1a0f` over base `c49132e6`; the probe blob on base is the shipped `4a9ab8df`
  blob plus 4 javadoc lines, so a base leg reproduces the shipped tree).
  **The cell set** — `GolemCombatAiGameTest`'s `CLAIM_DX_*` / `CLAIM_DY_*` / `CLAIM_DZ_*` / `ROW_DZ_*`
  constants and `claimRouteCells`: the route walks the corridor rel x11..13 × dy 0..1 × z 11..23 (the
  probe `setPos`s the golem to rel (12,0,22), `AIReturnHome#start` paths `-z` to rel z=13, and the CI
  red's trail left the corridor at rel (13,0,18)), so the claim CLEARS every non-air block in that box
  and RELAYS the walk row rel (12,-1,12..70) cell by cell, writing only cells that read AIR. The row
  arm is assert-then-relay, never a sweep: on a clean pad it writes nothing (`relaid=0` — that row is
  solid from the template floor plus the neighbour pads), so a clean leg prints only its summary line,
  and the eaten-row / trench plants are the shapes that make it write. It runs at setup, on every
  tick of the drive window (`GameTestHelper#runAfterDelay(long, Runnable)` is
  `runAtTickTime(getTick() + delay, task)` in vanilla `GameTestHelper`), and as the first statement of
  both draw callbacks (next paragraph); the summary at tick 160 reports `passes=162-163`. Every
  drive/setup phase is registered from the probe BODY, never from inside a callback —
  `GameTestInfo` keeps a per-probe `Object2LongMap<Runnable>` of tick actions that the harness iterates
  while running, and a nested registration is the known iterator hazard.
  **The same-tick order is identity-hash order, and that is why the claim also runs INSIDE the draw
  callbacks.** With `SETTLE`=120 the route draw is `pPath`=123 and the walk draw is 160, and the plants
  land at 122 (`pPath-1`). Measured on the drive-pass-only tree (`d3cf1a0f`): the band plant was cleared
  at 122 (`drive tick=122 cleared=12`, the pass ran after the plant in that tick) while the seal plant
  was cleared at 123, the DRAW's own tick (`drive tick=123 cleared=36`) — so on that tree a plant
  landing on the draw tick depended on hash order inside the tick (sealB GREEN 2/2 = a 0.25 read). The
  fix removes the flip: the claim is the FIRST statement of the `pPath` and `pWalk` callbacks, so the
  clear is in sequence before `returnHome.start()` draws and before the walk is measured, with the
  per-tick drive passes as the backstop for terrain arriving between ticks. Measured on the shipped
  claim (`ec6ebcc1`): the band leg prints `ROUTE-CLAIM drive tick=123 cleared=12` (the 123 drive pass,
  running before the draw) and the seal leg prints `ROUTE-CLAIM route tick=123 cleared=36` (the
  in-callback pass) — both GREEN 2/2 — and the summary at tick 160 reads `passes=162-163` (1 setup +
  the drive ticks + the two draw-tick passes; the ±1 is a drive pass landing in the summary's tick).
  **Measured, all NO-Curios whole-grid legs (`ran=1`, `cachehits=0`, the built-in-fallback marker,
  `loadavg(1m)` and `pgrep -c java` on every line — `keep/t_4c531ae6/`, 18 legs on the shipped claim
  `ec6ebcc1`, tags `*D2`):** the stride-6 band plant (rel x11..13, dy 0..1, z ∈ {13,19}, laid at tick
  122 = `pPath-1`) is **RED 2/2 on BASE** naming this probe on the WALK arm — `now 7.372057122818207 …
  away … tick 160`, the shipped tree's value family reproduced (the triage card's 3/3 on `4a9ab8df`) —
  and **GREEN 2/2 on the shipped claim** printing `ROUTE-CLAIM drive tick=123 cleared=12 relaid=0
  [Block{minecraft:cobblestone} @ rel (11,0,13), …]` and `ROUTE-CLAIM summary tick=160 passes=163
  cleared=12 relaid=0`. The same plant with the claim service switched off beyond setup
  (`CLAIM_DRIVE=false`, tag `bandD_setup`) is **RED 2/2**, naming exactly this probe, and its red line
  carries the published number: `the goal walked the golem from 10.0 blocks toward home, now
  7.372061974048622 away … tick 160` — the triage card's own band-plant value, with `passes=1
  cleared=0 relaid=0` proving only the setup claim ran. The same plant with the claim narrowed to rel
  z ≤ 17 (`CLAIM_DZ_TO=5`, tag `narrowzD2`) is **RED 2/2** printing `route tick=123 cleared=6` — it
  cleared the z13 wall and the z19 wall survived to redden the walk arm, so the EXTENT is load-bearing,
  not the block count. The 3-tall seal rel (11..13, 0..2, 13..18) is **RED 2/2 on BASE** (its leg 1 also
  reddened `theguardianbreathesforfivehundredticksandburns`: my plant landing in that probe's pad, the
  fixture leak the triage card measured, disclosed again) and **GREEN 2/2 on the shipped claim** via
  `route tick=123 cleared=36`. The four negative controls — single column rel (12,0..1,13), 3-wide 2-tall
  wall rel (11..13,0..1,13), eaten floor row rel (12,-1,13..18), trench rel (12,-1..-4,13..18) — are
  **GREEN 2/2 each**, with the claim naming what it touched: `cleared=2` (column), `cleared=6` (wall),
  `relaid=6` (eaten row), `relaid=6` (trench): the row arm repairs cells the corridor arm never reaches,
  and a control the claim clears is green *because* it was cleared, not because it was harmless. The
  clean lane is **GREEN 2/2** with `passes=162 cleared=0 relaid=0`.
  **Census (t_4c531ae6, on the LANDING tree `8b071546`):** 12 interleaved pairs (base-first), base
  `c49132e6` (533 probes) vs the landing tree (539: main's c5-2 portal slice adds 6 probes; this card
  adds 0, the probe FILE set is identical to main), all NO-Curios whole-grid legs, one gradle at a time —
  **base 12/12 `PASS533`, landing 11/12 `PASS539`, 0 reds on this probe on either arm**, `ran≥1` +
  `cachehits=0` + the built-in-fallback marker on every one of the 24 legs, `loadavg(1m)` 2.25-5.90 with
  1-4 JVMs, legs 60-159 s (`keep/t_4c531ae6/census2.out`). Every landing leg carries
  `ROUTE-CLAIM summary tick=160 passes=162-163 cleared=0 relaid=0` — the clean-handoff read, the claim
  writing nothing on a pad it was handed clean — and no base leg carries a claim line at all, so the two
  arms are distinguishable per leg. **One landing leg (pair 9) is RED**, naming
  `therangedattackseamlaunchestheblastwiththeoriginalconstants` and
  `wispdropsitsessencewithtwounitsofitsownaspect` — both OUTSIDE this delta (no plant on that leg,
  `cleared=0 relaid=0`, and the probe census + file set equal main's, so the packing grid is
  byte-identical to main), on a leg whose task line printed twice (`ran=2`); base 0/12 red, landing 1/12
  red, Fisher p = 1.0 for that pair. At the lane's published CI rate (1/24) P(0 reds in 12) = 0.60, so
  this census is the no-regression leg for the shared grid, NOT the closure; the plant pair above is the
  causal leg, exactly as the triage rule says. An earlier 20-leg run of the same design on the
  pre-merge fix tree (`d3cf1a0f`, `census_summary.txt`) was 20/20 `PASS533`, 0 reds.
  **Fixture artifacts disclosed, not fixed.** The probe's own floor loop writes
  `new BlockPos(home.getX(), home.getY() - 1, z)` for ABSOLUTE `z` in 12..70 while this pad sits at
  `z ≈ -13,905,236`, so the "floor out to 52 blocks along +z" lands thousands of blocks away; the row
  under the pad is the template floor plus the neighbour pads (the claim's relative row arm reads it as
  solid, `relaid=0`, at setup). The loop is left untouched on purpose: rewriting it moves ground under
  the 48-block park phase, which is a fixture change this card does not own. The triage card's ban
  stands — a sweep of that row moved the walk ground and flooded writes (`drive cleared=309` →
  WALK-arm red), so the row is relayed cell by cell and the claim's size is published in the probe's
  own constants, where the next reader can see what the gate reads.
  **Landed on the MERGED tree `eb0902d0` (parents: main `9315e039` + the claim tip `14fe6574`), so
  the plant pair and the census were re-measured on the 548-probe grid that the Eldritch Golem boss
  repoint and the mob-tick chunk claim shipped on; the census paragraph above is the pre-merge tree
  `8b071546` (533/539 grid) and keeps its own denominators.** Delta vs main is 2 files
  (`docs/testing.md` + this probe file): the `@GameTest(` census is **548 on main and 548 on the
  landing tree** and the probe FILE set is identical (83 files, 0 added/removed), so this card adds
  no probe and moves the shared packing grid by nothing.
  **Plant pair on the LANDING grid** (all NO-Curios whole-grid legs, `ran=1` + `cachehits=0` + the
  built-in-fallback marker on every line, `loadavg(1m)` and `pgrep -c java` published —
  `keep/t_4c531ae6/final_summary.txt`, tags `L_*` = the fix tree, `B_*` = the shipped probe blob
  dropped into the same grid): clean lane GREEN 2/2 `passes=163 cleared=0 relaid=0`; stride-6 band
  plant **GREEN 2/2** with the pass naming the blocks it took (`drive tick=122 cleared=12 relaid=0
  [cobblestone @ rel (11,0,13), …]` on one leg, `route tick=123 cleared=12` on the other — the drive
  pass and the in-callback pass, each ahead of its draw); band + `CLAIM_DRIVE=false` **RED 2/2**
  naming exactly this probe with `passes=1 cleared=0 relaid=0` and the published number (`now
  7.372061973925389 …` / `7.372057122818207 … away … tick 160`); narrowz (`CLAIM_DZ_TO=5`) **RED
  2/2** printing `drive tick=122 cleared=6` (the z13 wall cleared, the z19 wall survived, walk arm
  reddened at `7.372057121429315`) — extent load-bearing on the new grid too; 3-tall seal **GREEN
  2/2** via `route tick=123 cleared=36`; the four negative controls GREEN (column `cleared=2`, wall
  `cleared=6`, eaten row `relaid=6`, trench `relaid=6`). On the SHIPPED probe blob in the same grid
  the band plant is **RED 2/2** (`7.372061974048622` / `7.372057121922323` away — the triage card's
  value family reproduced byte-for-byte on the new grid) and the seal **RED 1/1**
  (`6.413718751500897` away), while the shipped clean lane is GREEN 1/1: the lever is this card's
  claim, on the landing grid, not the pre-merge grid alone.
  **Census on the landing tree:** 10 interleaved pairs (base leg first), base `9315e039` (548 probes)
  vs landing `eb0902d0` (548 probes), all NO-Curios whole-grid, one gradle at a time — **base 8/10,
  landing 10/10, 0 reds naming this probe on either arm**; `ran=1` + `cachehits=0` + the
  built-in-fallback marker on all 20 legs, 65-121 s per leg, `loadavg(1m)` 1.90-3.20 with 1 JVM per
  leg (`keep/t_4c531ae6/final_summary.txt`, per-leg logs `c3_*`). Every landing leg carries
  `ROUTE-CLAIM summary tick=160 passes=162 cleared=0 relaid=0` (the clean-handoff read) and no base
  leg carries a claim line at all, so the two arms are distinguishable per leg. **Both base reds
  (legs 8 and 9) name main's own `theGuardianArmedOneChunkOffThePadCornerStillTicks`** — no plant, no
  claim, `ran=1` — the guardian lane's rate on the BASE control: Fisher two-sided p = 0.47 for base
  2/10 red vs landing 0/10 red, and P(0 reds in 10 | the lane's published CI rate 1/24) = 0.65. This
  census is the no-regression leg for the shared grid, not the closure; the plant pair above is the
  causal leg, exactly as the triage rule says.
  **Fixture artifact disclosed, on the NEW grid (not fixed).** The band-family plants sit at rel
  x11..13 × dy0..1 × z13/19 — inside this probe's own corridor — and on the 548 grid that region is
  also read by main's new `EldritchMobGameTest#theGuardianArmedOneChunkOffThePadCornerStillTicks`
  (the mob-tick chunk claim, `MobChunkClaim`): the `narrowz` leg and one `wall3` control leg reddened
  THAT probe — "over 600 level ticks the guardian ran 0 entity ticks and its longest run of
  consecutive ticks on a ticketed mob chunk is 0 of the required 40" — while this probe stayed green
  on those legs (2 reds among the 22 plant/control legs). The same probe is flaky with NO plant at
  all: 2 of the 10 clean BASE census legs (no plant, no claim) reddened it, so on this grid the
  guardian's own rate is ~2/10 and a plant leg naming it is the neighbour-pad leak the triage card
  measured on the old grid (into `falldistanceisclearedinthefirsttwentyticks`) — plant side effect
  plus that lane's rate, not a channel this delta opens. It belongs to t_f9fe6e81 (landed `9315e039`):
  posted to that lane, not fixed here.
  **Re-measured on the FINAL landing tree `fcd914f8`** (parents: main `762b2340` = the Eldritch Warden
  c5-4 slice, 554 probes + this card's tip `c04f5723`; merge tree `a8f5a673` equals the
  `git merge-tree --write-tree` prediction, 0 D rows, probe FILE set identical, census 554 = 554). The
  probe blob is byte-identical to `eb0902d0`'s (`sha256 d7fb4e20…`), so the code under test did not
  move — only the shared packing grid did (+6 probes from the warden slice). **Plant pair on the 554
  grid** (NO-Curios whole-grid, `ran=1` + `cachehits=0` + marker per leg, `loadavg(1m)` 1.70-5.11 and
  `pgrep -c java` 1-4 published in `keep/t_4c531ae6/final2_summary.txt`, tags `M_*` = the landing
  tree, `S_*` = the shipped probe blob dropped into the same grid): clean lane **GREEN 2/2** `PASS554`
  with `ROUTE-CLAIM summary tick=160 passes=163 cleared=0 relaid=0`; stride-6 band plant — this probe
  **GREEN 2/2**, claim line on both legs; band + `CLAIM_DRIVE=false` **RED 2/2 naming exactly this
  probe** (`cleared=0`, no claim line); narrowz (`CLAIM_DZ_TO=5`) **RED 2/2 naming this probe** with
  the claim live (`cleared=6`); 3-tall seal — this probe **GREEN 2/2** (`route cleared=36`); column
  **GREEN 2/2** (`cleared=2`); eaten floor row **GREEN 2/2** (`relaid=6`). On the SHIPPED blob in the
  same grid: band **RED 2/2** naming this probe, seal **RED 1/1** naming this probe, clean lane GREEN
  1/1. So the acceptance pair holds on 548 and on 554 probes, with the same arms red and the same
  arms green.
  **Census on the 554 grid:** 5 interleaved pairs (base leg first), base `762b2340` (554) vs landing
  `fcd914f8` (554): base 4/5, landing 3/5, **0 reds naming this probe on either arm**; every landing
  leg carries `ROUTE-CLAIM summary tick=160 passes=162-163 cleared=0 relaid=0` and no base leg carries
  a claim line, 66-91 s per leg at 1 JVM.
  **Neighbour lane, published, not owned here (fixture/environment rate).** Main's
  `EldritchMobGameTest#theGuardianArmedOneChunkOffThePadCornerStillTicks` (the mob-tick chunk claim,
  landed by t_f9fe6e81) reddened **12 of the 70 NO-Curios legs this card ran on the 548 and 554 grids**
  — 4 of 42 on the 548 grid (2 clean BASE census legs with no plant and no claim, the `narrowz` leg,
  one 3-wide-wall control leg) and 8 of 28 on the 554 grid (`M_band` leg1, `M_band_setup` leg2,
  `M_seal` leg2, `M_hole` leg2, the shipped-seal leg, and 3 census legs, one of them a BASE leg) — and
  this probe stayed GREEN on every one of those legs; its 7 probe-named reds in this batch are the
  arms above. Rate 1/5.8 on this box, at `loadavg(1m)` 1.70-5.11, 1-4 JVMs. Its message is "over 600
  level ticks the guardian ran 0 entity ticks and its longest run of consecutive ticks on a ticketed
  mob chunk is 0 of the required 40" — a chunk-ticket lane. It fires on the base control as often as
  on this card's arms, and main's own CI runs the same lane green, so this is a box rate, not a
  regression from this delta: posted to the guardian lane (t_f9fe6e81, landed `9315e039`), not fixed
  here.
<a id="component-codec-save-path"></a>
- **A vanilla data component's persistent codec can reject a mod's marker, and the save path
  swallows the ITEM, not the write.** `minecraft:entity_data` and `minecraft:block_entity_data`
  are registered with `CustomData.CODEC_WITH_ID` (`DataComponents.java:172`; `CustomData.java:33-35`
  is `tag.contains("id", 8) ? success : error("Missing id for entity in: " + tag)`), so a compound
  put there with no entity `id` string makes `ItemStack#save` throw
  (`DataComponentUtil.wrapEncodingExceptions` logs `Error saving [15 minecraft:diamond]` and
  **rethrows**) out of `ServerLevel#save:824` — the server logs it, the save keeps going, and that
  stack silently never reaches the region file: a user-visible item loss. Measured here: 186
  occurrences in 53/53 legs (both accessory arms, three trees) from the pech trade-window drop
  stamping `{PechDropOwner:"PechDrop"}` into that component (t_04233450, BD-73's first shape);
  renaming the key does not help, the gate reads the absence of `id`, not the key name.
  Two consequences for probe authors. (1) A probe that asserts the component's VALUE on the live
  stack — the pre-fix `stamped == 5` arm — is GREEN over a total item loss; a write that lands on
  a component the platform cannot serialize needs a SAVE/RELOAD arm on the crashing frames
  (`Entity#saveWithoutId` → `ItemEntity#addAdditionalSaveData:333` → `ItemStack#save:419`) and the
  reload frames (`Entity#load` → `ItemEntity#readAdditionalSaveData:341` → `ItemStack#parse`), and
  the marker must be read back through the CONSUMER's own reader
  (`AIPechItemEntityGoto#pechDropOwnerOf`), never a hand-rolled key, or a marker that survives on a
  key nobody reads passes. (2) This defect is invisible to exit codes: the harness prints
  `All N required tests passed` while the signature rides the shutdown log, so a leg's green says
  nothing — grep the retained per-leg log for `Missing id for entity in` (0 on a clean tree;
  2-12 per leg before the fix).

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
<a id="stride-motion-moves-the-read-cell"></a>
- **A leg that reads a cell derived from the mob's position has to plant its payload where the
  CODE's read lands, not where the geometry says it should - and the read cell is MEASURED on the
  pad, not derived on paper.** The Eldritch Golem's footstep arm (ported verbatim: the motion gate
  `motionX*motionX + motionZ*motionZ > 2.5E-7`, the `nextInt(5) == 0` roll, the row
  `floor(posY - 0.2F - this.height)`) reddened with every term satisfied on paper: motion 0.455
  clears the gate, the row is four cells under a 3.5 box, and the urn in the mob's own column at
  that row stood for all 200 trials - the per-tick draw puts the miss at 0.8^200 = 4.1E-20, so the
  roll is not the explanation (`docs/testing.md#outcome-not-tick-budget` in reverse: the budget was
  sound, the CELL was not). The probe's own per-tick census answers it: 21.1.259's
  `LivingEntity#aiStep` applies the stride motion to the POSITION before the code below
  `super.aiStep()` reads `floor(posX)`, so a mob pinned at its cell centre scores the cell ONE EAST
  of its own column - the logged read cell held `Block{minecraft:stone}` while the placed column
  held `Block{thaumcraft:blocklooturn}[variant=0]`. Shape: plant in the measured cell, keep the two
  cells a WRONG read breaks as decoys (the mob's own column at the same row, and one under the feet
  - the reading that drops the height term), take the cell into the probe's claim set
  (`docs/testing.md#orb-claim-covers-asserted-cells`), and log the read cell on the leg's first tick
  so the geometry stays on the record. This is the
  `docs/testing.md#pad-write-lands-only-in-a-loaded-chunk` discipline with the axis moved: not which
  CHUNK the write lands in, WHICH CELL the code's own arithmetic names. Teeth - the shipped leg put
  the urn in the feet column on an otherwise green tree and reddened 200/200 trials (the red above);
  the fixed leg is green in both arms and its decoy asserts pin the two readings the fix must not
  become (`BossMobGameTest#theGolemCrushesASnowCellAndBreaksLootAheadOfItsStride`).



Anchors minted for the cited bullets (t_867b394f): `vouched-cell`, `entity-clock-poll`,
`removal-age-names-mechanism`, `outcome-not-tick-budget`, `sweep-hazard-set-is-scan-set` (t_b33916a2, the mind-spider mob-box claim). Minted by t_8b63e037:
`mob-seed-stream-rate-arm`. Minted by t_83e5a20d: `empty-stack-sentinel-save` (the golem carry sentinel). Minted by t_d8944aae (the pad-claim box-cell rule):
`scan-claim-covers-box-cells`. Minted later (t_04233450): `component-codec-save-path` (the
item-entity bullet keeps main's `attribute-not-filter`, which this card's probe cites). Minted by t_b98f2601 (the orb-cell claim rule):
`orb-claim-covers-asserted-cells`. Minted by t_9687e91a (the deferred-chunkgen producer rule):
`deferred-chunkgen-stamps-pad-cells`. Minted by t_764d1e8e (the harness gate on that
producer): `deferred-chunkgen-gated-in-gametest`. Minted by t_c36620d6 (the golem route-arm node-trail rule): `route-arm-node-trail-names-the-channel`. Minted by t_d6a23f78 (the stride-cell rule): `stride-motion-moves-the-read-cell` (the footstep arm's read cell is the stride's cell, measured). Minted by t_f9fe6e81 (the mob-tick chunk-claim rule): `mob-tick-claim-covers-mob-chunk`. Minted by t_4c531ae6 (the golem route-arm claim, the fix that card handed over): `route-claim-covers-the-cells-the-route-gate-reads`.
