# Changelog

All notable changes to Thaumcraft Classic (Unofficial) are documented here.
Format: Keep a Changelog; versioning: Semantic Versioning (see
`docs/release-process.md`). Project versions are independent of the pinned
Minecraft 1.21.1 / NeoForge 21.1.259 targets.

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
