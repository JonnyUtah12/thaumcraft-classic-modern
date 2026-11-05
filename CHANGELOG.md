# Changelog

All notable changes to Thaumcraft Classic (Unofficial) are documented here.
Format: Keep a Changelog; versioning: Semantic Versioning (see
`docs/release-process.md`). Project versions are independent of the pinned
Minecraft 1.21.1 / NeoForge 21.1.259 targets.

## [Unreleased]

**The generated research lang is consumed at runtime (BOOK-3a follow-up, t_28ce4d49).**

BOOK-3a's `assets/thaumcraft/lang/research_original.json` was read by nothing: 1.21.1's
`ClientLanguage.loadFrom` formats `lang/%s.json` for the requested locale codes, and NeoForge's
`net.neoforged.fml.i18n.I18nManager.loadTranslations` reads `lang/<locale>.json` off the FML
classpath, so a file named `research_original.json` never entered the language and a research
screen calling `Component.translatable(entry.nameKey())` rendered the raw `tc.research_*` key
even in an overlay build.

- `research/ResearchLexicon` reads the file (path taken from
  `ResearchOriginalLang.GENERATED_RESOURCE_PATH`, so the reader cannot drift from the writer)
  and owns the resolution rules: the generated original, else the live language's own answer
  (how BOOK-3b's 4 authored keys land), else the key unchanged. Minecraft-free by design, so the
  game-test server and a plain JVM probe can build it.
- `client/ResearchOriginalLanguage` (mod-bus `RegisterClientReloadListenersEvent`,
  `Dist.CLIENT`) reads the resource through the `ResourceManager` **first** — the same
  pack-stack route a locale file takes, so a user pack overlays it the same way — and falls
  back to the **classloader** route (`getResourceAsStream`, the route NeoForge's own
  `I18nManager.loadTranslations` uses). The fallback is a measured result, not a hypothesis: on
  this tree the game-test server's `ResourceManager` returns an **empty stack** for
  `thaumcraft:lang/en_us.json` and for `thaumcraft:lang/research_original.json` (the committed
  lang reaches that runtime with 318 keys through the classloader, stack size 0), so a lang
  resource reaches a dedicated server only through the classloader. Then the merge injects the
  merged `Language` with `Language.inject`, the injection vanilla's own `LanguageManager`
  performs, so `TranslatableContents` re-decomposes and **every** translatable route resolves the
  merged map. The wrapper overlays the map and delegates `isDefaultRightToLeft`,
  `getVisualOrder` and `getComponent` to the base language; a game-bus client-tick guard
  re-applies the merge whenever the live language is not the instance it injected (a language
  switch, F3+T, a pack toggle — the reload-stack order between the mod and vanilla listeners is
  not a contract), idempotently and at the cost of one volatile reference read per tick in
  steady state.
- **D-016 is unchanged and stays strict:** the merge injects nothing when the lexicon is empty,
  so a code-only build leaves the client language exactly vanilla's and keeps rendering raw keys;
  the committed `en_us.json` keeps **0** of the research keys, pinned at build time
  (`ResearchLexiconTest.committedLangKeepsZeroOfTheGraphKeys`, 0 of 711 over the 318 committed
  keys) and at runtime (`ResearchOriginalLanguageGameTest.committedLangCarriesNoneOfTheGraphKeys`,
  0 of the 712 the live graph references). Nothing is committed; the generated file stays out of
  `src/main/resources` (pinned).
- Probes: `ResearchLexiconTest` (9: writer→reader round trip, markup verbatim, tolerant parse,
  path contract, merge math, resolution order, both D-016 pins, the classloader route),
  `ResearchOriginalLanguageTest` (6: merge over a synthetic base, real `Language.inject` +
  `Component.translatable` round trip, seeded-lexicon idempotence, empty-lexicon no-op, both
  subscribers `Dist.CLIENT`, and a bytecode scan that keeps the merge out of every server-loaded
  class), `ResearchOriginalLanguageGameTest` (2 headless probes over the live
  `ResourceManager` + classloader + `ResearchManager.graph()`, naming no client class). RED
  baseline `8993885` (both legs exit 1), GREEN `8799ac9`.
- **Measured by the new probes, reported not papered over:** the live graph references **712**
  display keys, the build-time derivation (**every `tc.*` string in `entries.json`**) **711**; the
  generated file carries **707**. The 5-key gap is asserted to be exactly the explicable set,
  derived from shipped data: the 4 keys of the two rows with no original string
  (`CAP_iron`/`ROD_wood`, BOOK-3b's) **plus one key the generator's `GRAPH_KEY_PREFIX` filter
  cannot see** — `VAMPBAT` page 0 carries `textKey = "focus.upgrade.vampirebats.text"`, a
  non-`tc.` key the original lang *does* define (`en_US.lang:617`), so that one hover string
  reaches an overlay build as a raw key. That is a gap in the **generator**, not the consumer;
  filed as a follow-up card. Fidelity: the **199** name keys cross-check against the graph's own
  `nameEnUs` rows — 185 byte-exact, 14 differing only in surrounding whitespace (the original
  lang carries them with a trailing space), 0 differing beyond whitespace.
- BD-30 records the seam: the merge lands on the `Language` instance, so a direct
  `net.minecraft.client.resources.language.I18n.get` (which caches its own language) does not
  see it; the tree has 0 `I18n.` call sites today. BD-22's follow-up column closes the merge
  half; the `<BR>`/`§` page markup stays **BOOK-3b**, and the Thaumonomicon GUI itself
  (pages, nav, minigames) stays unimplemented.
- Gates (JAVA_HOME=`$HOME/opt/jdk21`): `build` exit 0 — unit **485 / 0 failures** (15 of them this
  card's), code-only `build -Ptc.referenceAssets=build/does-not-exist` exit 0 (11 reference-absence
  skips), `runGameTestServer` and `-Ptc.noCurios runGameTestServer` both **All 170 required tests
  passed** (168 before this card + the 2 new probes), fixture checks (`checkAssetPathsOverlayJar`,
  `checkGeneratedResearchLang`) exit 0, no conflict markers.
- **CI corrected two of those readings; both are repaired on this branch.**
  1. The CI game-test legs run with no reference tree, so they take the **code-only** branch of
     `generatedResearchLangResolvesTheShippedGraph` that no local leg ever reached. Its
     precondition hardcoded the research key `BASICS`, which **no row of `entries.json` carries**
     (the shipped keys are `BASICTHAUMATURGY`, `FOCUSFIRE`, …), so CI run 42099194219 failed with
     "the graph itself is shipped" on a probe that, on the same line, measured 712 referenced keys.
     The precondition is now **derived from the shipped JSON** — declared `entryCount` = array
     length = runtime `graph.entries().size()`, and the first shipped row's `key` must resolve in
     the runtime graph — so it is non-vacuous on exactly the build where it is the only branch run.
  2. `checkGeneratedResearchLang` was **red in a clean workspace** (CI runs 42094050934 on
     `wt/book3a` @ `caa7293` and 42099194219 here, byte-identical output). Measured cause:
     `overlayAssetJar` decided its "no overlay lang ⇒ exclude `research_original.json`" rule from a
     `def` evaluated at **configuration time**, and in fixture mode the fixture tree — with the
     `en_US.lang` the generator reads — is created by `makeAssetOverlayFixture` *inside the same
     invocation*, so the flag read false for the whole build and the jar stripped a file it was
     about to package: **25 keys on disk, 0 in the checked jar**. That is the same trap the
     `srcDir` wiring above documents, on the exclusion side. The rule is now evaluated in
     `doFirst`, at task-execution time. Pair measured on this tree from a clean `build/`: without
     the change exit 1 with `jar assets/thaumcraft/lang/research_original.json: 0 keys` and
     `FAIL: 25 graph-referenced keys … are neither committed nor generated`; with it exit 0 with
     `jar …: 25 keys`. The gate's job (validate the artifact that carries the file) is unchanged,
     and the generator's own prefix gap — the 712th key, `focus.upgrade.vampirebats.text` — stays
     filed as **t_6799fd3f**. The `firebatFlagBitsMatchTheOriginalByte` red on that same CI leg is
     the documented flake (t_291f87aa) whose fix sits on the unmerged `wt/flakery` @ `e43937d`.
  3. CI run 42102260672 then went green on the game-test legs and red on the `build` job, exposing
     a **third shape no leg had reached**: the `build` job's game tests run against the **fixture
     overlay**, whose `en_US.lang` values are placeholders that exist to exercise the lang parser
     (an `=` inside a value, a `\n` escape, leading/trailing spaces, `<BR>`/`§` markup) and carry
     **no original string** (D-004). The probe's fidelity leg therefore compared
     `"Fixture name with a trailing space "` against the graph row `"Basic Wand Craft"` — and failed
     correctly, on a build that has nothing to be faithful to; its exact-gap-set leg failed the same
     way, because the fixture lang defines 25 of the 711 keys and no graph row describes the other
     686. The probe now **measures which overlay it is running against** and asserts per shape:
     every mapped block texture is **1x1** under the fixture and **16x16** under the reference tree
     (all 242 of them, and the overlay's textures ship into the mod jar, so a running game can read
     the bytes), which selects the branch. Fixture shape asserts the invariant that holds in every
     build — the generated file never carries a key the generator cannot produce; reference-tree
     shape keeps the fidelity cross-check **and** the exact-gap-set equality. This is a scope split,
     not a weakened pin: the strict pins run on the only shape where they are true, and they are the
     pins `t_6799fd3f` inherits.

**D-016 option 2, generation half (BOOK-3a): the original research display strings are
generated at build time instead of committed.**

The shipped research graph references **711** `tc.*` lang keys — 201 `tc.research_name.*` +
201 `tc.research_text.*` + 309 `tc.research_page.<ENTRY>.<n>` — derived from
`data/thaumcraft/research_graph/entries.json` at build time, never a hardcoded list. A new
build step (`generateOriginalResearchLang`, on the Gradle-free class
`com.jonnyutah12.thaumcraftclassic.tools.ResearchOriginalLang`) converts the **707** of them
that exist in the player's own extracted `../reference/assets/thaumcraft/lang/en_US.lang`
into `assets/thaumcraft/lang/research_original.json` (sorted keys, 2-space indent, UTF-8,
values verbatim). The **4** the original 4.2.3.5 lang never defined —
`tc.research_name/text.CAP_iron` and `tc.research_name/text.ROD_wood`, the same two entries
whose graph rows carry no `nameEnUs`/`textEnUs` — are authored in **BOOK-3b**.

A **code-only** build generates nothing and carries **0 of the 711**, so it renders raw
`tc.research_*` keys: that is the policy-aligned D-016 outcome, and the committed
`en_us.json` (344 keys) is unchanged, with **0** overlap against the 711 pinned by a unit
probe. Also in this commit: the new `checkGeneratedResearchLang` gate (reads the generated
file out of the overlay jar and prints the measured counts; wired into `check` and CI), a
deterministic 25-key `thaumcraft/lang/en_US.lang` in the CI fixture
(`scripts/make_asset_overlay_fixture.py`, fixture 911 → 912 files), 9 unit probes, and
**BD-22** recording that the 1.7.10 page markup rides through verbatim (`<BR>` ×541 over 256
page keys, `§`-colour codes ×370 over 86 page keys, `&` ×1; `#l`/`$#` measured 0 — they do
not exist in the file).

## [0.1.0-alpha.5] - 2026-11-03


The artifact on this release was **replaced on 2026-11-04**: it is built from `7c4ebaf`, not
`5ee1dae`, and the tag moved with it. On top of the alpha.5 spine the shipped jar now carries
the armour slice, the taint root-cause, enchantment registration (`Repair`, `Haste`), item
models for all 63 registered items, the CI particle-sheet fixture fix, and a probe-premise fix
(the pre-taint record is keyed by 4x4 biome cell, so a probe leg must clear it). Gate numbers
on that tree: unit 417 / 0 failures / 2 skipped; `runGameTestServer` All 149 required tests
passed; `-Ptc.noCurios` All 149; asset path contract green in code-only + real overlay +
fixture overlay modes (908 mapped targets, 0 missing). The published jar is code-only: 973
entries, 0 texture entries.

Fifth pre-release — 28 commits past alpha.4. Still a pre-release, not playable
end-to-end; see Known limitations. Per-system status: `docs/FEATURE_MATRIX.md`;
deviations: `docs/BEHAVIORAL_DEVIATIONS.md`.

**Headline: the missing-texture defect is fixed.** The alpha.4 notes recorded
that every block and item rendered the magenta/black missing-texture pattern
*even in an asset-included build*, because the overlay copied the original
1.7.10 plural `textures/blocks/` layout while 1.21.1 resolves the singular
`textures/block/`. That path remap (TC-M7-01-s-a) landed and this is the first
release to carry it. Verified by mutation rather than by a green suite:
commenting the rename map out (`'blocks': 'block'` in `build.gradle`) makes
`OriginalTextureOverlayTest` FAIL (exit 1); restoring it byte-identically returns
the suite to green (exit 0).

**SPINE-3 Item model coverage closes the “items are not visible/searchable” repro: 51
  port-authored `models/item/<id>.json` (52 total) so every registered item renders a
  real icon in inventory, hand, the hotbar, the container grid, the recipe book and the
  creative search index.** Each model names the sprite the **original’s own item class**
  registered as its icon (verified per class against `reference/decompiled/`) — not a
  guessed `thaumcraft:item/<id>` — e.g. `itemcompassstone` → `item/sinister_stone`,
  `blockairy` → `item/taint_over_2` (the original’s `BlockAiryItem` icon),
  `tilearcaneworkbench` → `models/worktable`, `itemessence` → `item/phial` +
  `item/essence`. Every sprite the chains name is a published overlay path: 42 rows of
  `docs/asset-mapping.csv`, of which 4 move the original’s per-nugget icons
  `nuggetbeef.png`, `nuggetchicken.png`, `nuggetpork.png` and `nuggetfish.png`
  (registered for the 4 `ItemNuggetEdible` ids at `ConfigItems.java:399-405`) from
  UNUSED to published, taking the unused-asset inventory 42 → 38 (38 of them textures).
  A new `ItemModelCoverageGameTest` derives its expected item set from the live registry
  (52 shipped items, fixture scaffolding excluded — never a hardcoded list), checks the
  converse (0 dead models), resolves every model chain with explicit namespaces,
  resolves every chain sprite **when a local overlay is installed** (self-disabling
  under D-004’s code-only release, vanilla stand-ins checked against the 1.21.1 client
  jar) and enforces §15 rule 9 across models/lang/tabs. Models are port-authored and
  committed; the original’s textures stay overlay-only, so a code-only jar still
  resolves every model to the placeholder — by design. Per-meta appearance (19/7/15/16
  metas), 3D block-item models, blockstates for the other 51 blocks and the 4 custom
  item renderers are filed as BD-17, not claimed. RED → GREEN: `runGameTestServer` exit
  1 (“51 of 52 registered thaumcraft items have no
  /assets/thaumcraft/models/item/<id>.json”, “51 item model chains do not resolve”)
  before the models landed, exit 0 (“All 130 required tests passed”) after;
  `-Ptc.noCurios` 130/130; `./gradlew build` exit 0 (379 unit / 0 failures / 1 skipped).

## [Unreleased]

Landed on `main` after the `v0.1.0-alpha.5` tag **and inside the replaced
artifact** (built from `7c4ebaf`): the armour slice, the taint root-cause,
enchantment registration, item models for all 63 registered items, and two build
fixes. Landed on `main` **after** that artifact, so it enters the next build: the
wisp FX family (TC-M7-02-c-s-f1c, `wt/fx-f1` @ `74d562e`, merged at `663386f`),
gated on that merge tree — unit **417 / 0 failures / 2 skipped**,
`runGameTestServer` **All 151 required tests passed**, `-Ptc.noCurios` **All 151**,
asset path contract green in overlay mode (248 block + 247 item textures, 908
mapped targets, 0 missing) and code-only mode, fixture overlay-check rc=0 with
**50 particle sprites** sliced (13 wisp sprites added to the 37), CI
`41881766519` success. Not released; every number below is from a gate run on the
merged tree.

Also entering the next build: the **eldritch realm** (TC-M6-04f-s-i, `wt/eld-int` @
`ca933c3`, merged at `6beb7f0`) — its block and item catalog, the realm's pads, 15
content rows flipped, item-model coverage **63 → 68**, and **BD-20** recorded for the
damage-type armour bypass that is not implemented at all. Gates on that merge tree:
unit **424 / 0 failures / 2 skipped**, `runGameTestServer` **All 157 required tests
passed** in both the Curios and `-Ptc.noCurios` modes, asset path contract green
(908 mapped targets, 0 missing).

**Ore drop probes assert on the stacks they own (`c81ca8a`).** The drop scans counted
every item entity in a 2-block box around the broken cell, so once the eldritch pads
changed how the harness packs probe batches, a neighbouring pad's grass seed answered
the cinnabar contract — `got 2 stacks [1 minecraft:wheat_seeds,
1 thaumcraft:blockcustomore]` — in the Curios run, while the same tree passed
`-Ptc.noCurios`. The fixture changed, not the mod. All six scan sites now assert on the
**owned** subset and name the foreign stacks in the failure message. Both axes are
proved by mutating `BlockCustomOre#getDrops`: the ore returning itself twice turns
`cinnabardropsitselfexactlyone` and `silktouchdropstheblockwithitsvariant` red, and the
ore returning one extra shard stack turns `fortunescalestheoriginalcurves` (6 stacks
against a 1..5 bound) and `infusedstonedropsitsprimalshards` (3 against 1..2) red. The
mutated file was restored byte-identically each time, sha256 against HEAD.

- **Firebat mob + the wisp's essence drop** (`TC-M6-06-s-c1`): `thaumcraft:firebat` lands as a hand-flown 1.21.1
  `Mob` - 0.5x0.9 hitbox and fire immunity, 5.0/1.0 attributes with the devil 15.0 and the summoned
  `(devil ? 3 : 2) + damBonus`, the five bat flags as bits of one byte under the original `BatFlags`/`damBonus`
  save keys, an empty goal selector with the original's own target provider and melee body driven from
  `customServerAiStep`, the fire/magic damage refusal, and the hanging/flight/explosion/vampire/spawn-light
  bodies. `EntityWisp.dropCustomDeathLoot` ships the original's `func_70628_a`: one wisp-essence stack carrying
  2 units of the wisp's own aspect, gated on the stored type resolving - and the wisp's eerie-biome pool is
  repaired to the original's `Aspect.<X>.getTag()` values, which is what makes that gate fire. Gate: 416 unit
  tests / 0 failures / 1 skipped, 127/127 game tests with and without Curios. `BD-21` records the firebat's
  unmeasurable vanilla drop roll and the deliberately unwired 1.7.10 `isNoAi` hook.
**Taint — the Ethereal Bloom's cleansing disc (TC-M5-04-s-b2).** The disc
acceptance probe shipped on `wt/taint-b1` reported that 4 of the 16 biome
palette cells under the bloom never cleansed (`MISSING=[2_0, 2_1, 2_2, 2_3]`).
That was the probe's own fixture, not the mod. `RandomSource.create(long)` is
`java.util.Random`, whose LCG amplifies a seed difference of one by only
`0x5DEECE66D` ≈ 2.3e10 inside a 2^48 state, so its **first** `nextInt(8)` moves
about once every 1400 consecutive seeds. Over the seeds 1..2500 that first draw
took four values (4, 5, 6, 7); the bloom draws x's pair first, so the roll
stream spanned x offsets −3..+7 and **never** −5..−7. 69 of the 225 offsets of
the scan box were never drawn, and the cells that received zero rolls were
exactly the four reported ones. The biome write path was cleared by
measurement: 41/41 single-column writes answer for their own column's 4-block
footprint, the paint taints 625/625 columns, and every disc cell that received
a roll fired exactly once.

- `rollSeed` spreads seeds by the golden-ratio constant, and the disc probe now
  also sweeps **every offset of the scan box exactly once** with a scripted
  `RandomSource` on a fresh paint: "the disc is cleansed" no longer depends on
  which offsets a seed sequence happens to draw. The scripted queue is supplied
  x-pair-then-z-pair, which pins the original's draw order.
- `cellKeyOf` now returns the palette cell's full identity (chunk coord +
  index). The index `(coord >> 2) & 3` repeats every chunk and the probe paints
  ±12 columns = two chunks, so an index-only key merged two different cells and
  made the "missing cell" reading ambiguous.
- "Never cleanses beyond the disc" is stated at cell granularity, the
  definition the code already used (`dx > 3 || dz > 3`): a 4×4 cell spans
  columns at |offset| ≥ 8 as its own BD-15 footprint, so a column filter at
  |offset| ≥ 8 would flag a correct cleanse.
- New mutation guard `TaintCleansingRollCoverageTest` (unit, ~50 ms, no world):
  the roll stream must span ±7 on both axes, every disc cell of every bloom
  alignment must receive rolls, and the x band −7..−5 must be drawn. Revert the
  seed spread → 3 of 4 red (`expected: <-7> but was: <-3>`); a fourth test
  pins the `java.util.Random` degeneracy itself.
- Gates on this branch: unit **386 / 0 failures / 1 pre-existing skip**;
  required game tests **All 115 passed** (111 inherited from the taint slice +
  3 cleansing probes + 1 write-location probe). No new behavioral deviation:
  the 4×4-block biome footprint is already BD-15, and the original's
  byte-per-column write shares no palette cell.

Work heading for `0.1.0-alpha.5`, gated per merge on `main`.

- **Enchantments `Repair` and `Haste` are registered** (`TC-M6-05-s-e1c`): the two shipped infusion
  rows `recipe.201.infusion_enchantment.json` / `recipe.202.infusion_enchantment.json` named
  `thaumcraft:ench_repair` and `thaumcraft:ench_haste` with nothing behind those ids, so both rows
  loaded and resolved no enchantment holder — they could never match a pedestal. 1.21.1 keeps
  enchantments in the **datapack** registry `minecraft:enchantment`, so registration is two JSON
  payloads under `data/thaumcraft/enchantment/` plus their `enchantment.thaumcraft.*` labels (the
  original 4.2.3.5 wording: Repair, Haste) and a `ResourceKey` pair on `ModEnchantments` (an id
  holder, deliberately not a `DeferredRegister` — that registry is not in `BuiltInRegistries`, so a
  deferred register for it silently never populates). Gates: `EnchantmentRegistrationGameTest`
  asserts both ids in the live registry, both labels shipped, and both recipe rows resolving a holder;
  `EnchantmentIdsTest` pins the payload path/id, the codec shape, and that no infusion row names a
  thaumcraft enchantment outside the pair. `InfusionAltarGameTest` had pinned the old behaviour as an
  invariant (the 2 rows load and resolve **no** holder); that assertion is flipped to resolvable. **Registration only**: neither payload carries an
  `enchantment_effects` component, so Repair does not mend and Haste does not speed anything up —
  the effect bodies are the next slice (`TC-M6-05-s-e2`).

- **First authored container screen** (`TC-M7-04-s-cr1`): `TrunkScreen` extends 1.21.1
  `AbstractContainerScreen`; `ModClientScreens` registers it on the mod bus through
  `RegisterMenuScreensEvent`, client-only, bound to the trunk menu type. Gate: 372 unit
  tests / 0 failures, 103/103 game tests with and without Curios. This is **1 of N**
  screens: the mod ships menu types with no screen today, and the board's premise that
  screens exist in the wip tree is measured false (0 screen classes in `src`, 0 in
  `wip-salvage`), so the rest are authoring work, not porting.

- **CI green for the first time since the FX merge** (`t_028f9bf8`): every branch was
  red from 2026-10-29 on `:sliceOriginalParticleSprites` — the build-time slicer indexes
  16px cells of the 256x256 `textures/misc/particles.png`, while the CI asset fixture
  wrote 1x1 placeholder PNGs, so the build job's overlay-jar step failed in ~1s with
  `(x + width) is outside of Raster` (main runs 40938034742 / 41145956204 / 41146088447).
  The fixture now writes that one sheet at its real size — every other placeholder stays
  1x1 — and the slicer states the sheet-size contract in its own failure message. Gate:
  CI run 41452841483 (jobs build / game-test-no-curios / runServer) on the code tip
  `79eb001`; locally 379 unit tests / 0 failures / 1 skipped and 116/116 game tests with
  and without Curios.

### Added
- **Ore drops** (SPINE-1): `blockcustomore` shipped with NO loot table, so all
  eight variants dropped nothing, silently. `data/thaumcraft/loot_table/blocks/
  blockcustomore.json` now ports `BlockCustomOre.getDrops` 1:1 — variant 0
  (Cinnabar Ore) drops the ore itself (furnace-smelted to quicksilver later);
  variants 1-6 (infused stone) drop `thaumcraft:itemshard` with
  `thaumcraft:item_meta` = variant-1, count `1 + rand(2+fortune)` as ONE STACK
  PER UNIT; variant 7 (Amber Bearing Stone) drops `thaumcraft:itemresource` with
  `item_meta` 6, count `1 + rand(fortune+1)` via `apply_bonus` /
  `uniform_bonus_count`. Silk touch returns the block with its variant preserved,
  matching 1.7.10's vanilla `harvestBlock` path. The
  `thaumcraft:fortuned_uniform` number provider is registered in
  `ModLootNumberProviders`.
- **Creative tab** (SPINE-2): `ModCreativeTabs` declared a tab registry but
  registered no tab, so nothing was searchable in creative mode. One tab now
  enumerates the live `thaumcraft`-namespace item registry rather than a
  hardcoded list, filters the test-fixture ids, and titles itself with the
  1.21.1 `itemGroup.<namespace>.<id>` convention.

### Verification at this tag
- `./gradlew clean build` exit 0. **379 unit tests, 0 failures, 1 skipped**
  (measured 2026-11-03); **130 required game tests** passed, up from 116 at
  alpha.4. The alpha.4 figure of 368 units was measured on a different tree and
  is superseded.
- **Mutation-proved gates, not just green runs:** hiding
  `blockcustomore.json` makes **5** required tests fail (`runGameTestServer`
  exit 1), and restoring it byte-identically returns green (exit 0).
- Independent `validate-resource-pack.sh --strict` over an asset-included jar:
  all 52 item models are valid JSON and every texture reference resolves. Its one
  ERROR is inherited from the source — `textures/models/ripple.png.mcmeta` has no
  `ripple.png`, and the original tree is identical — and its one WARNING is a
  vanilla texture (`minecraft:item/compass_00`, present in
  `minecraft_1.21.1_client.jar`) that a mod-scoped validator cannot see.

### Known limitations
Not implemented: wands and foci casting, most GUI screens, the Thaumonomicon UI,
essentia transport, taint, enchantment registration, armor, entities, the
infusion matrix's rune rendering, and the traveling trunk (seam only). **Golems
are implemented on branches and are NOT in this build** — they need the shared
entity/AI base (`entities/ai` goals plus a mob-inventory base), neither of which
exists yet.

**Artifact policy:** the attached jar is **code-only** — no original textures,
sounds or models. Stated for accuracy: **232 of the 242 shipped display strings
are verbatim from the original All-Rights-Reserved `en_US.lang`**, which is
committed, so the artifact is free of original *binary* assets but not of
original *display strings*. See **D-016**, still open.

- **Build tooling: the CI asset fixture broke the particle slicer.** The fixture wrote
  every placeholder PNG as 1x1, and `sliceOriginalParticleSprites` crops a 16x16 grid of
  16 px cells out of `textures/misc/particles.png`, so every crop fell outside the raster
  ("(x + width) is outside of Raster", CI run 40938034742). The fixture now generates a
  real 256x256 solid sheet for that one file, the generator is a task input so a stale
  fixture cannot survive, and cells outside the raster degrade to a solid stand-in sprite
  with a logged summary instead of failing the build. Gate: fixture overlay-check
  BUILD SUCCESSFUL (242 `textures/block`, 236 `textures/item`, 904 mapped targets, 0
  missing); merged tree 411 unit tests / 0 failures, 134/134 game tests with and without
  Curios.

- **The merge caught a probe the slice branches passed** (TC-M5-04-s): `runGameTestServer` on the integration tree
  failed 1 required test — `bloomrestoresThePreTaintBiomeFromTheRecord` leg B, “21/22 columns restored to plains;
  the restore answered -6,-6=minecraft:forest”. The pre-taint record is stored per chunk under the 4×4 biome-cell key
  (1.21.1 biomes are 4×4×4 quarts, so 1.7.10's per-column record has no per-column equivalent), which means a record
  written by an earlier leg answers a later leg's restore in the same cell — and which cells collide depends on where
  the game-test pad is dropped, so the same probe passes on one tree and fails on another. Fixture fix, mod code
  untouched: legs B and C clear the record map over their chunks and re-check their premise. BD-18 states the
  granularity as the deviation.

- **The item-model gate caught the integration's own gap** (SPINE-3, extended): on the merged
  tree `runGameTestServer` failed **2 required tests** — `everyitemmodelchainresolves` and
  `everyshippeditemhasamodeljson`, “11 of 63 registered thaumcraft items have no
  `/assets/thaumcraft/models/item/<id>.json`” — because the armour and taint slices registered
  items without models. 11 port-authored models added: 7 armour (`clothboots`/`clothchest`/
  `clothlegs`/`thaumiumboots`/`thaumiumchest`/`thaumiumhelm`/`thaumiumlegs`/`gogglesrevealing`)
  and 3 block items (`greatwoodsapling`/`taint_soil`/`taint_fibres`), each `minecraft:item/generated`
  with a namespaced `layer0` on a published overlay path, each cited in `docs/asset-mapping.csv`.
  Item coverage is now 63/63. Rule for the next slice: an item registration lands with its
  `models/item/<id>.json`, because on this tree a missing icon fails a required test.

### Added
- **Wisp FX family — the last three effects of the M7-02 visual slice**
  (TC-M7-02-c-s-f1c, card `t_7fe21fbf`, branch `wt/fx-f1`, stacked on
  `wt/m7-beams`; not merged to `main`). `thaumcraft:wisp`,
  `thaumcraft:wisp_eg` and `thaumcraft:wisp_arcing` (`FXWisp`/`FXWispEG`/
  `FXWispArcing`) now compile on 1.21.1, register and spawn through the same
  plan/effects seam the sparkle, bubble and beam families use. Nine of the
  port's eleven particle types resolve through codec payload records
  (`TcWispOptions` 9 fields, `TcWispEgOptions` 3, `TcWispArcingOptions` 6),
  the per-tick bodies are the original's own arithmetic (0.985 homing drag
  with the 0.2 seek and ±0.2 clamp, 0.98 free drag, 0.7 ground drag, the
  up-then-down/`shrink` scale envelope, the EG −0.05 fall with its `rand*0.03`
  gaussian spread and 13-frame strip, the arcing anchor+motion re-derived
  position with its arc term), and the senders (`TcFxPlans.wisp`/`wispByType`/
  `wispEg`/`wispArcing`, `ClientEffectHooks.wispFx`/`wispFxType`/`wispFxEg`/
  `sourceStreamFx`) carry the original's `particleCount` gate and 50/25
  render-distance cull. The three original senders are client proxy calls
  (aura nodes, essentia sources, the wisp entity), not packet handlers, so no
  payload routes to them; their callers arrive with their own cards.
- **Measured gates on this branch** (literal, `--no-daemon`): `compileJava
  compileTestJava` exit 0 from a cleared `build/classes/java`; `./gradlew
  test` **371 tests / 0 failures / 0 errors / 1 pre-existing skip** (49
  result files); `runGameTestServer` **"All 118 required tests passed :)"**;
  `-Ptc.noCurios runGameTestServer` **118/118** with `curios … -> MISSING`.
- **Art stand-in recorded as BD-18** (`docs/BEHAVIORAL_DEVIATIONS.md`): the
  original's wisp and arcing-wisp quads sample the **vanilla** `/misc/fx.png`
  sheet, which the reference tree does not ship, so those two draw the sliced
  Thaumcraft glow frame; the EG mote steps its real 13-cell strip. Colour,
  size, scale envelope, motion, lifetime and blend layer are exact.
- **No localization or inventory rows were added, deliberately**: particle
  types carry no translation key in vanilla and the localization probe reds
  the build on any key that maps to no shipped content, so a
  `particle.thaumcraft.*` entry would be a *dead* key (`en_us.json` stays at
  241 keys); `docs/content-mapping.csv` has no FX/particle rows and its
  `kind` whitelist is closed, so the inventory stays at 1048 rows.

## [0.1.0-alpha.4] - 2026-10-28

Fourth pre-release. Still deliberately a pre-release, not playable end-to-end:
wands and foci, all GUI screens, the Thaumonomicon, essentia transport, taint,
enchantment registration, armor, entities, particles and item models remain
unimplemented. Per-system status: `docs/FEATURE_MATRIX.md`; deviations:
`docs/BEHAVIORAL_DEVIATIONS.md`.

**Artifact policy:** the jar attached to this release is **code-only** — no
original Thaumcraft textures, sounds, models or language files. Original assets
are All Rights Reserved and are never redistributed; local builds overlay the
user's own extracted assets (D-004).

**Known limitations, stated accurately.** The alpha.3 notes attributed missing
in-game textures to the code-only artifact policy. That explanation was
incomplete and is corrected here: even an **asset-included** build renders
blocks magenta, because the overlay copies the original 1.7.10 layout
(`textures/blocks/`) while 1.21.1 resolves `textures/block/` (singular). The
path remap is tracked as `TC-M7-01-s-a` and is **not** in this build. The
companion localization defect **is** fixed (see Added).

**Counts in this section, measured on the released tree rather than copied
forward.** Unit tests **368** (0 failures, 1 skipped); required game tests
**103/103**, and the same 103/103 in the no-Curios configuration. The alpha.3
notes' 317 unit / 89 game figures are superseded. `en_us.json` ships **241
keys** — not the 128 the localization card recorded as its own delta, nor 107
alone from the per-variant-names card; those are per-card contributions to the
same file.

**Licensing flag, stated rather than implied.** Of those 241 shipped values,
**231 appear verbatim in the original 4.2.3.5 `en_US.lang`**, which is All
Rights Reserved; only 10 are authored for port-created ids. D-004 says original
assets are never committed or redistributed, and this file is both committed to
the public repository and included in the code-only jar. It was not generated at
build time as its card specified. Recorded as **D-016** with the options; the
exposure predates this release (the commits are already public), so publishing
this jar adds no new copy, but the posture needs an explicit decision.

### Added
- **Essentia extraction and storage** (TC-M4-06-s, merged by TC-M4-06-p,
  `42d94ae`; gates re-measured on the merged tree at `22ba516`): the arcane
  furnace, essentia jars and phials as running content, with 8 game probes
  green — including the void-jar variant and the arcane furnace's self-drop via
  its block loot table. Measured on the merged tree: **103/103 game tests** and
  **368 unit tests**.
- **Localization — `assets/thaumcraft/lang/en_us.json`** (TC-LOC-s, card
  `t_a2a74726`): 128 keys ship for the first time, so the mod's identifiers
  resolve to real words in-game — 27 block display names, 15 item names, 58
  per-meta names across the 9 multi-meta item families (shards, dusts, ingots,
  nuggets, patches, capsules, vials, blocks), 9 container/screen titles
  (`container.thaumcraft.*`, covering the menu types the alpha.3 notes listed
  as "menus with no screens yet"), 1 entity name (`thaumcraft:efrite`), and 15
  tooltip and wand-action strings. 118 keys are verbatim from the original
  4.2.3.5 `en_US.lang`, 2 come from `docs/content-mapping.csv`, and 8 are
  authored for port-created ids (`container.thaumcraft:purifier`,
  `container.thaumcraft:Thaumonomicon`, `item.thaumcraft.thaumium_grapple`,
  `item.thaumcraft.primal_orb`, `item.thaumcraft.creation_pearl`, 2
  `item_category.thaumcraft.*` tab labels, `thaumcraft.slot.mirror_slot`) —
  all declared in a whitelist in the new `LocalizationGameTest`, alongside
  `LocalizationFileTest` (7 unit tests) and a 128-key static audit written to
  `build/lang-audit.txt`. No legacy `§` colour codes. Closes the
  "Thaumcraft identifiers are not localized" known limitation; item groups
  (tabs) and enchantment/mob-effect names remain open on their own cards.
- **Crucible core and gated data** (TC-M4-05-s, card `t_95373ba3`): the
  alchemy crucible now exists as a running block entity — `thaumcraft:tilecrucible`
  with `TileCrucible` + `CrucibleStateMachine`, the `thaumcraft:crucible`
  recipe type and serializer pair in `CatalogCrucible`, one wiring line in
  `ModRegistration.init`, 9 ore-dictionary item tags, and 54 crucible recipe
  files (48 `recipe.N.crucible` rows plus `recipe.0.crucible` expanded to its
  6 rarity variants). Every row carries a `neoforge:item_exists` gate, 22 use
  the registered `{"type": "neoforge:components", ...}` ingredient form, and
  none use the bare form the alpha.3 notes flagged as loading-unsafe (D-015) —
  0 JSON parse failures, and the game-test server boots with no `thaumcraft`
  ERROR line across 1404 loaded recipes. Behaviour is now **proven** by
  TC-M4-05-p (`t_5fa2e0bb`, merge `28fa0fd`): three game probes run in the
  build — the recipe type, serializer and `thaumcraft:tilecrucible` resolve;
  a real-world crucible reaches ignition in 151 ticks and crafts
  `itemshard` meta 6; and the data invariant counts **54 row files, 30
  passing their `neoforge:item_exists` gate and live in the `RecipeManager`,
  24 gated out, 30 resolving**. Gate run on the merged tree: 94/94 game tests
  and 94/94 without Curios. `gui_crucible` remains TC-M7-04 and
  tubes/transport TC-M4-07.
- **Per-variant mega-block display names** (TC-M7-01b, `t_f40bbc0d`, merge
  `5fc5051`): the 10 multi-variant mega-blocks (stone/wooden/metal devices,
  cosmetics, crystals, candles, tables, `blockairy`, custom ores) and their
  slabs now resolve their own label per variant instead of rendering the
  meta-0 name for every variant — 1.21.1 has no per-state block display name,
  so this is a block-item `getName` override reading the `variant` property.
  107 keys added to `en_us.json` (audit: required=235 missing=0 dead=0);
  103 of them are verbatim from the original 4.2.3.5 `tile.<id>.<meta>.name`
  entries, 0 mismatches, and 4 authored (the slab labels, because 1.21.1 slabs
  are separate blocks). Gate run: 95/95 game tests, 95/95 without Curios.

### Development
- Dispatch bookkeeping from the orchestrator pass at `v0.1.0-alpha.3`: the
  19-card serial chain was **frozen, not idle** — its head `t_35a2c627` sat
  `blocked` while its work was already merged as `a9303ea`, so `running=0`
  and 19 cards waited on it. The head was closed with evidence (317 unit
  tests, 89/89 game, 89/89 no-Curios, CI run 40697171757), TC-M4-05-s was
  promoted and runs in its own `git worktree`, and the seven `BLOCKED` rows
  in `docs/feature-tracking.md` / `docs/FEATURE_MATRIX.md` now name the
  reduced-scope card that owns each area, with the WIP inventory recorded
  per area (it lives on the read-only `wip-salvage` branch, not on the
  shared tree).
- `docs/FEATURE_MATRIX.md` carries a **suite at the current tag** bullet so
  per-commit counts quoted inside Evidence cells are not read as current,
  and its structures row is corrected from the stale
  `IN PROGRESS / none (uncommitted)` to VERIFIED: the code is merged
  (`d4acdb3`, a confirmed ancestor of `3a0eee0`) and
  `StructureParityTest` is in `src/test`.
- `GameTestIndex.java` is generated at build time; its committed copy is now
  byte-identical to the generated one (`07fb19d`).
- Localization (TC-LOC-s, card `t_a2a74726`): `assets/thaumcraft/lang/en_us.json`
  now ships, closing the defect where every shipped item, block, menu and
  tooltip rendered as its raw translation key. 128 keys — 27 blocks, 15
  single-name items, 58 per-meta names across the 9 multi-meta item families,
  9 container titles (5 menu ids + the 4 titles the shipped block entities
  pass), 1 entity name and 15 tooltip/action lines; 118 of them carry the
  original Thaumcraft 4.2.3.5 `en_US.lang` wording (legacy `§` colour codes
  stripped, the `%TYPE` token converted to a real `%s` argument). Guarded by
  `LocalizationGameTest` (every registered block/item/menu/entity name is
  covered, and no entry in the file is dead) and `LocalizationFileTest`
  (key shape, placeholders, and per-meta parity against the original lang):
  324 unit + 91/91 game tests, 91/91 without Curios.
  `CatalogTrinketsLootGameTest.treasureBagCarriesOriginalMetaNames` now checks
  the key *and* that it resolves, instead of asserting the raw key renders.

## [0.1.0-alpha.3] - 2026-10-28

Third pre-release. Still not playable end-to-end: the interaction layer
(wand casting, GUI screens, Thaumonomicon UI, crucible/alchemy, essentia
storage, golem, taint, entities, models and renderers) is not implemented.
Per-system status: `docs/FEATURE_MATRIX.md`; deviations:
`docs/BEHAVIORAL_DEVIATIONS.md`.

**Artifact policy:** the jar attached to this release is **code-only** — no
original Thaumcraft textures, sounds, models or language files (All Rights
Reserved, no license grant). Local builds overlay the user's own extracted
assets into the output JAR (see `docs/porting-decisions.md` D-004); a
code-only jar is markedly smaller, which is the signal the overlay did not
apply.

### Added
- **Arcane workbench core** (TC-M4-03-s `64f6ac6`, data repaired by
  TC-M4-03-fix `31ca741`, decision D-015): the `thaumcraft:arcane_workbench`
  recipe type + serializer pair, `BlockArcaneWorkbench` +
  `TileArcaneWorkbench` (`WorldlyContainer`), and vis payment through the
  TC-M4-02 `effectiveCosts → consumeAllVisCrafting` all-or-nothing chain
  (centi-vis pool = real vis × 100). Shaped matching honours the original's
  `mirrored=true` horizontal mirror; shapeless claims every grid item and
  rejects an unclaimable extra; the research gate refuses until the TC-M3-02
  knowledge bit is written. 109 extracted arcane rows ship — 38 resolve into
  the live `RecipeManager`, the other 71 carry an unmet `neoforge:item_exists`
  gate on an item their owning card has not registered, so they skip rather
  than fail. `ArcaneWorkbenchGameTest` proves registration, both match shapes,
  the research flip, exact-pool-empties / short-pool-spends-nothing, and that
  every shipped row whose conditions are satisfied is loaded with its result
  item, count and `thaumcraft:banner_color` value.
- **`thaumcraft:banner_color` data component** (D-015, `9777550`):
  `ModDataComponents.BANNER_COLOR` carries the original's banner NBT `"color"`
  byte on the crafted result stack (`banner.func_77990_d.func_74774_a("color",
  (byte)a)`, `ConfigRecipes.initializeArcaneRecipes`), range 0..15. This took
  our mod's `RecipeManager` parse errors from 31 to 0; the 16
  `recipe.112.arcane_shaped.<0..15>` rows now match with their colour. Nothing
  consumes the component yet — TC-M6-04d owns the banner tile.
- **Infusion altar core** (TC-M4-04-s `a9303ea`): `thaumcraft:infusion` and
  `thaumcraft:infusion_enchantment` recipe type + serializer pairs
  (`CatalogInfusion`), `TileInfusionMatrix` + `TileInfusionPillar` bound to
  `blockStoneDevice` metas 1-4 through `DeviceTileFactory`.
  `InfusionMatrixLogic` ports `validLocation` (complete altar only: pedestal
  two below + four corner pillars), `getSurroundings` (25×16×25 scan, +2 per
  filled pedestal, +1 with an item, −2 for the mirrored twin, stabilisers +0.1
  each / −0.2 mirrored, truncated) and the instability math (symmetry + recipe
  instability at start, increments capped at 25, event on
  `nextInt(500) <= instability`, the 21-sided event table).
  `TileInfusionMatrix` runs the original craft cycle — essentia drain via the
  new `aspect/EssentiaHandler`, five-cycle component animation,
  eject/zap/harm/lightning/warp events, concrete/NBT-tag/enchantment finish
  paths — proven end-to-end by 4 `InfusionAltarGameTest` probes. 87 extracted
  rows ship (63 infusion + 24 infusion_enchantment); 41 resolve today (17
  crafting + 24 enchantment).
- **Catalog — device blocks / standalone machines** (TC-M6-04c `04274d2`):
  14 contract rows — `blockStoneDevice/Metal/Wooden/Airy` with the original
  meta ranges 15/15/9/13 verbatim as the `as-number` variant property, plus 10
  tile entities (bellows, sensor, arcane pressure plate, bore + bore base,
  3 arcane lamps, shared ownership base, nitor). Nitor and arcane ear tick
  through the shared device ticker (redstone countdown, Fire-starved
  burn-out); the pressure plate never ticks per the original
  `canUpdate() = false`.
- **Catalog — furniture / storage / display** (TC-M6-04d `947863a`): 26
  contract rows — tables + deconstruction table, hungry chest, magic box,
  mirror + essentia mirror, arcane door + key, lifter, pedestal, wand
  pedestal, spa, banner, item grate, warding stone + fence, the
  `specialitemgrate` entity type and three menu rows. The hungry chest's 27
  slots and the magic box's item list survive a
  `saveWithFullMetadata`/`loadStatic` round-trip with slot positions,
  membership and counts intact.
- **Catalog — trinkets / tools / loot containers** (TC-M6-04e `57007b4`): 10
  contract rows — `blockLootUrn` / `blockLootCrate` as one `BlockLoot`
  carrying the original 0/1/2 rarity verbatim, treasure bags with the
  original metas on `ITEM_META` and the original stack limit 16, thaumometer,
  resonator, compass stone, hand mirror (menu, 1 input slot at (80,24) over
  36 player slots) and the traveling-trunk spawner seam. Break-drops follow
  the original `Utils.generateLoot` shape (1+rarity..3+rarity from the
  `Config.initLoot` bag tables), with silk touch and pick-block preserving the
  container and its rarity.
- **CI: dedicated-server smoke job** (`c0f0a00`): boots a real dedicated
  server with the mod, scans the complete boot log for any
  `thaumcraft`-attributable `ERROR`/`FATAL`/`Exception` line and asserts the
  mod loaded, so a single non-decoding recipe row turns CI red instead of
  shipping.

### Fixed
- ATM10 startup battery (TC-M9-01): the aggregate research graph files moved
  from `data/thaumcraft/research/` to `data/thaumcraft/research_graph/` —
  Astral Sorcery globs `data/<ns>/research/*.json` across every namespace
  and logged parse errors over our aggregates on every ATM10 startup; the
  old path is also reserved for a future `thaumcraft:research` datapack
  registry. Curios item tags (`curios:thaumcraft.ring/.amulet`) now mark
  their test-probe entries `"required": false`: the probe items are only
  registered under the gametest property, so the shipped tags failed
  vanilla tag validation in production startup.
- The 31 arcane recipe payloads that failed to parse (TC-M4-03-fix): the
  meta-carrying ingredient now uses NeoForge's own registered
  `{"type": "neoforge:components", …}` ingredient form (that `IngredientType`
  is `NeoForgeMod.DATA_COMPONENT_INGREDIENT_TYPE`, `NeoForgeMod.java:365`), so
  the bare `{"neoforge:components": …}` object — which is not an ingredient at
  all — is gone, and the 16 banner rows load because the component id above is
  registered. The earlier claim that those rows waited on their own card was
  wrong and is corrected here.
- Infusion recipe extraction (`scripts/extract_infusion_recipes.py`): rows are
  gated on `neoforge:item_exists` for every item they name (an unported item
  skips the row instead of failing the data pack), ids are 1.21.1
  (snake_case enchantments, `dead_bush` / `sugar_cane` / `cobweb` / `potion`,
  dye meta 0 → `ink_sac`, wildcard fish → `minecraft:fishes`), and the
  enchantment field decodes as a `ResourceKey` resolved on use, so the two
  rows naming the TC-M6-05 enchantments load and never match instead of
  failing the pack.

### Verification
- 317 unit tests, 0 failures; **89** required game tests passed in
  `runGameTestServer`, and the no-Curios configuration
  (`-Ptc.noCurios runGameTestServer`) also 89/89; `./gradlew build` green
  locally and on CI at this tag. (The alpha.2 notes quoted 299 unit / 65 game
  tests — those counts are superseded.)
- Recipe data invariant: of the 109 arcane rows, 38 resolve and none logs a
  `RecipeManager` parse error; of the 87 infusion rows, 41 resolve, and the
  dedicated-server smoke job is green on the boot log.
- Testing note: structures and ores generate in **new chunks** only — load a
  pre-alpha.2 world and they will not appear. Use a fresh world or explore
  ungenerated terrain.

## [0.1.0-alpha.2] - 2026-10-24

Second pre-release. Still deliberately a pre-release, and still not playable
end-to-end: the interaction layer (wands, foci, arcane crafting, infusion,
alchemy, essentia devices, taint, enchantments, armor, entities, GUIs,
Thaumonomicon, particles, models) is not implemented. Per-system status:
`docs/FEATURE_MATRIX.md`; deviations: `docs/BEHAVIORAL_DEVIATIONS.md`.

**Artifact policy:** the jar attached to this release is **code-only** — it
contains no original Thaumcraft textures or sounds. Original assets are All
Rights Reserved (no license grant) and are never redistributed. Local builds
overlay the user's own extracted assets into the output JAR (see
`docs/porting-decisions.md` D-004); a code-only jar is markedly smaller,
which is the signal the overlay did not apply.

### Added
- **Ore world generation** (TC-M5-05, `d1d7d5f`): cinnabar, amber and
  infused stone at the original counts and bands, via the
  `thaumcraft:tc_ore` feature type and a biome modifier. **Corrects the
  alpha.1 notes**, which stated ore world generation was implemented but
  not integrated and therefore absent from that JAR — it is present from
  this release onward.
- **World generation — structures** (TC-M5-05b, `d4acdb3`): totems,
  shrine-mound structure table, eldritch rings and hilltop stones, with
  `StructureParityTest` replaying the original `setBlock` sequence and roll
  frequencies.
- **Eldritch realm — maze cells, first slice** (TC-M6-04f, `f252c58`):
  `MazeGenerator`, outer-lands cell/cell-location model and
  `MazeGeneratorTest`. Catalog items and `damage_type` for this slice remain
  unsalvaged (tracked as TC-M6-04f-s).
- **Content catalog slices** (TC-M6-04a/04b, `9c29cfc`, `ea694a9`):
  resource items, shards, nuggets and food (incl. mana pod); decorative and
  cosmetic blocks.
- **Baubles-slot items + runic shielding** (TC-M8-02, `e48c4fb`): six
  original accessories (apprentice rings, runic ring/amulet/girdle, vis
  amulet, hover girdle) with original metas and costs, and the
  `RunicShielding` pool ported term-for-term from `EventHandlerRunic`
  (charge, hardening, recharge ordering, kinetic push, regeneration,
  emergency, starvation/drowning/void/wither exclusions).
- **Section-19 fidelity tracking** (`bf45efc`, `d89319d`):
  `docs/FEATURE_MATRIX.md` (per-system status with evidence and honest
  denominators), `docs/BEHAVIORAL_DEVIATIONS.md` (BD-01…BD-14 with cause,
  impact, alternatives and next steps), and
  `scripts/build_feature_matrix.py` generating the machine-readable
  `docs/feature-matrix.csv`.

### Fixed
- ATM10 startup battery (TC-M9-01): the aggregate research graph files moved
  from `data/thaumcraft/research/` to `data/thaumcraft/research_graph/` —
  Astral Sorcery globs `data/<ns>/research/*.json` across every namespace
  and logged parse errors over our aggregates on every ATM10 startup; the
  old path is also reserved for a future `thaumcraft:research` datapack
  registry. Curios item tags (`curios:thaumcraft.ring/.amulet`) now mark
  their test-probe entries `"required": false`: the probe items are only
  registered under the gametest property, so the shipped tags failed
  vanilla tag validation in production startup.

### Verification
- 299 unit tests (1 skipped), 0 failures; **65** required game tests passed
  in `runGameTestServer`; `./gradlew build` green locally and on CI at this
  tag. (The alpha.1 notes quoted 216 unit / 39 game tests — those counts are
  superseded.)
- Testing note: structures and ores generate in **new chunks** only — load a
  pre-alpha.2 world and they will not appear. Use a fresh world or explore
  ungenerated terrain.

## [0.1.0-alpha.1] - 2026-10-22

First pre-release. Deliberately a pre-release, not 0.1.0: most gameplay
systems (arcane crafting, infusion, alchemy, essentia devices, wands and
foci, the content catalog, entities, GUIs, taint) are still not
implemented — see Known limitations below and
docs/compatibility-matrix.md for the authoritative status.

### Added
- Accessory provider layer (TC-M8-01): provider SPI (equip/unequip/
  worn-tick/validate) over the original Baubles slot semantics —
  RING×2/AMULET/BELT/CHARM with frozen payload indices — with an optional
  Curios 9.5.1 bridge (data-driven slot types under the `curios` pack
  namespace, `ICurioItem` adapters attached via `CuriosApi.registerCurio`,
  no core class references Curios types — bytecode-guarded) and a
  built-in fallback (player attachment + lenient codec + right-click
  equip + keepInventory-aware death); the accessory game-test suite runs
  green under both configurations (separate `game-test-no-curios` CI
  job). See docs/architecture/accessories.md.
- Aura + node world data (TC-M5-01): `thaumcraft:node_type` /
  `thaumcraft:node_modifier` registries (the 9 frozen content-mapping
  ids), the 31-row biome aura table reproduced row-for-row against the
  original `Config.registerBiomes`, seeded wild-node generation 1:1 with
  the original `createRandomNodeAt` RNG order (golden-seed pin),
  per-chunk aura records and a server-tick driver on the world-data
  chunk pipeline, and NodeLookup query hooks. See
  docs/architecture/aura-nodes.md.
- Flux system (TC-M5-03): per-chunk `fluxAir`/`fluxGround` semantics on
  the world-data store, a generation table pinned to the original sites
  (crucible/infusion/reservoir/explodify/taint/thaumhia plus a
  spell-cast port design constant), the 40960-tick age pass derived from
  the original 1/30-per-random-tick goo decay, gas vent + neighbour
  diffusion, and the slime/taint/gas/goo effect thresholds. See
  docs/architecture/flux.md.
- Vis core (TC-M4-02): vis pool data component (centi-vis,
  all-or-nothing payment, derived capacity), the cost modifier chain
  against the TC-M0-06 tables, the visnet node-conduit-device model with
  the original's selection quirks preserved, and an ambient vis reader.
- Aspect system (TC-M4-01): `thaumcraft:aspect` registry with all 48
  original definitions (48/48 parity vs the decompiled `Aspect.java`),
  data-driven essentia tables (287 item / 56 ore-dict / 116 entity rows
  extracted 1:1 from `ConfigAspects`), the TC4 combination/reduce/cull
  rules, and discovery rules over player knowledge. See docs/aspects.md.
- Research semantics (TC-M3-02): server-authoritative completion with
  aspect-pool payment (all-or-nothing TC4 reduce), the auto-unlock
  starting set, item/entity/aspect triggers with hidden-reveal rules,
  the warp-on-research split, and the `/thaumcraft` command.
- World + player persistence substrate (TC-M2-02): versioned
  `WorldMagicData` SavedData, lenient player-attachment codecs with
  copy-on-death and client sync, and chunk hooks.
- Sound-event registry (TC-M7-05): all 66 original TC sound events with
  a per-event trigger-site map, parity + game-test guards, and the first
  wired play sites. See docs/sound-events.md.
- Research graph data foundation (TC-M3-01): the complete original research
  graph — 6 categories and 201 entries with pages, triggers, prerequisites,
  warp and flags — mechanically extracted from the decompiled original into
  `data/thaumcraft/research_graph/{categories,entries}.json`
  (`scripts/extract_research_graph.py`), with a Minecraft-free model,
  loader and registry (`research/ResearchGraph*`) and tests proving the
  extraction matches the TC-M0-04 inventory entry-for-entry, the lang
  strings are verbatim from the original `en_US.lang`, all aspect/item/
  recipe references resolve, and the prerequisite graph is acyclic
  (see docs/research-graph-notes.md).
- Registration architecture + id-stability harness (TC-M2-01):
  DeferredRegister holders for the whole mod behind one constructor-time
  ordering point, the frozen content-id contract
  (`thaumcraft:` + lowercased original path, decision D-001) mechanically
  derived into docs/content-mapping.csv, and a two-layer id-stability
  harness (unit contract test + game test resolving every implemented
  id against the live registries). See docs/architecture/registration.md.
- Networking payload foundation (TC-M2-03): 37 payloads 1:1 with the
  original message set, registered on both dists.
- Configuration parity (TC-M2-04): the full TC 4.2.3.5 option set as a
  NeoForge config spec. See docs/config-reference.md.
- Project foundation: NeoForge 21.1.259 / MC 1.21.1 Gradle build (ModDevGradle
  2.0.148, committed wrapper), mod metadata with explicit unofficial-port
  disclaimer, unit-test harness (metadata/pin guards), in-game smoke gametest,
  CI (build + gametest server with explicit pass assertion), documentation
  skeleton, and the local original-asset overlay pipeline (assets never
  committed; see docs/licensing-and-attribution.md).

### Fixed
- Accessory game tests under a no-Curios runtime: Curios slot-type JSONs
  are only loaded from the `curios` pack namespace in Curios 9.5.1
  (bytecode-verified), so the slot definitions moved from
  `data/thaumcraft/` to `data/curios/`; the provider check no longer
  class-loads the Curios bridge on a no-Curios runtime.
- Flaky aspect player-scan game test: fresh players draw seeded aspects
  from the name hash, so the assertion now pins the true invariant
  (humanus ≥ 4, total vis exactly 16) instead of a name-dependent split.
- Runic shield sound ids normalized and sound file references
  namespaced in the asset overlay.
- The `thaumcraft:empty` game-test structure template is now shipped, so
  the game-test server starts from a clean install.

### Known limitations
- Not yet playable: the content catalog (blocks/items/machines), wands
  and foci, arcane crafting/infusion/alchemy/essentia devices, taint,
  entities, world-gen structures, and all GUIs (including the
  Thaumonomicon) are not implemented. See docs/compatibility-matrix.md
  for the authoritative status.
- Ore world generation (cinnabar/amber/infused stone, TC-M5-05) is
  implemented and verified but not yet integrated into `main`; it is not
  in CI or in built JARs until that integration lands.
- Original assets are never committed; a code-only JAR is built without
  the local reference tree (decision D-004).
