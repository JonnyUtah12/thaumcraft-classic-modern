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
  **The band's literals are the ORIGINAL's roll body x the probe's own kill loop, and re-centring means re-deriving that product on the branch
  that ships the band, not trusting the branch it was written on.** Recomputed at `de3b988c` (main tip at this card's rebase). Three landings moved
  main since the band was written (`6a7f99da` Pech, `10db0007` PLAYABLE, `de3b988c` tube/machine — 38 files, +7117 lines) and **none of them touches
  a zombie file**: `git log 6a7f99da..de3b988c -- .../entities/monster .../gametest/ZombieMobGameTest.java .../monster/BrainyZombieAiLogicTest.java`
  is empty, `EntityBrainyZombie`/`EntityGiantBrainyZombie`/`BrainyZombieAiLogic` carry a single commit `b767568` apiece, and the probes' own last
  touch is the band fix `f759b4d6` — so main's support loop IS the loop the band was authored against, and the arithmetic below is the proof, not an
  assumption. Read off main at that tip: the support loop is `killAndCollectDeathDrops(helper, "brainyzombie", 30)` feeding the band `3 * 30` and
  `killAndCollectDeathDrops(helper, "giantbrainyzombie", 10)` feeding it `2 * 6 * 10` — kill literal and flip literal agree, which is exactly the
  property a rebase has to re-check, because a moved kill loop moves the mean AND the sd:
  `reference/decompiled/.../EntityBrainyZombie.java:44-46` is `for (a = 0; a < 3; a++) if (world.nextBoolean()) drop(rotten_flesh, 1)` x the probe's
  **30** deaths (`killAndCollectDeathDrops(helper, "brainyzombie", 30)`) → flips **90**, quantum **1** → mean `90 · ½ = 45`, sd `√(90 · ¼) · 1 =
  4.7434`, `45 ± 4 sd = [26.03, 63.97]`, widened outward to the quantum → **[26, 64]**, exact trip `P(Binom(90,½) ≤ 25) + P(Binom(90,½) ≥ 65) =
  2.9665e-5` (the probe's shipped `2.97e-5`). `EntityGiantBrainyZombie.java:58-66` is TWO `for (a < 6)` loops of `drop(rotten_flesh, 2)` x **10**
  deaths → flips **120**, quantum **2** → mean **120**, sd `√(120 · ¼) · 2 = 10.9545`, `120 ± 4 sd = [76.18, 163.82]`, widened to the stack of 2 →
  **[76, 164]**, exact trip `P(2·Binom(120,½) ≤ 74) + P(2·Binom(120,½) ≥ 166) = 3.2432e-5` (shipped `3.24e-5`); the pair is **6.2096e-5** a leg =
  1 red in **16 104** legs. The expectations stay the ORIGINAL's literals; `BrainyZombieAiLogic`'s constants are the mutation surface, never the
  expectation (`BrainyZombieAiLogicTest` pins 3 / 1 / 6 / 2 as numbers, and the game probes hard-code `3 * 30` and `2 * 6 * 10`).
  **Predicted rates for the band-regression mutations on this tree** (exact binomial sums, so a leg that survives them is luck, not design):
  halving the angry probe's support (`killAndCollectDeathDrops(helper, "brainyzombie", 30 → 15)`) makes the true body `Binom(45, ½)` — mean **22.5**,
  sd **3.3541** — against the shipped `[26, 64]`, and since 15 deaths can leave at most 45 flesh the HIGH side is unreachable, so the arm reddens on
  `P(Binom(45,½) ≤ 25)` = **81.44 %** of mutated legs, and the cap arm (`≤ 90` stacks) and the quantum arm (every stack `== 1`) stay green on
  **100 %** of them. Halving the furious support product-side (`BrainyZombieAiLogic.FURIOUS_ROLLS_PER_LOOP 6 → 3`) makes the true body
  `2·Binom(60, ½)` — mean **60**, sd **7.746** — against the shipped `[76, 164]`, reddening on `P(2·Binom(60,½) ≤ 74)` = **97.41 %** of legs (which
  is the same number the fury probe's comment calls "lands on 60, well outside"). The band is the FIRST assert in each probe's poll body, so the only
  arms that can mask it are the two inside `killAndCollectDeathDrops` (`… did not kill the mob, so the drop roll never ran` and `the fixture's stone
  button at … did not break`); those must show **0** occurrences in a mutated log for the red to be attributed to the band.

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