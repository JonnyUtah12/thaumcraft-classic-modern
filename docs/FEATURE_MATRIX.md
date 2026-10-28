# FEATURE MATRIX — behavioral fidelity scorecard

Section-19 mandated tracker. Status vocabulary (strict, not
interchangeable): **NOT INVESTIGATED / INVESTIGATED / IN PROGRESS /
IMPLEMENTED / TESTED / VERIFIED / BLOCKED / DOCUMENTED DEVIATION**.

Translation to the other status sets used in this repo:

| This file | `compatibility-matrix.md` | `feature-tracking.md` |
| --- | --- | --- |
| VERIFIED | IT | DONE |
| TESTED | IU | DONE |
| IMPLEMENTED | PARTIAL | (n/a) |
| IN PROGRESS | PARTIAL | BLOCKED (WIP on disk) |
| INVESTIGATED | NI | NOT STARTED |
| BLOCKED | BL | BLOCKED |
| DOCUMENTED DEVIATION | AD | (see BD rows) |

- **VERIFIED** = substantive implementation + passing tests + observable
  behavior compared against the original JAR (4.2.3.5, decompiled).
- **TESTED** = implementation + passing defined tests, behavioral
  comparison not yet recorded.
- **IMPLEMENTED** = substantive code exists; correctness unproven.
- **IN PROGRESS** = WIP present (uncommitted shared-tree) and unverified.
- **INVESTIGATED** = behavior documented, no port yet.
- Counts are the **discovered totals** from
  `docs/original-content-inventory.md` (mechanically derived from the
  decompiled tree). A status at system granularity does not claim every
  listed member of that category is done — the counts column is the
  denominator; the status is the verified/implemented portion.
- Evidence = Kanban card → commit on `main` (board
  `thaumcraft-classic-port`). A row is VERIFIED only with a commit +
  test evidence. Deviations link to `docs/BEHAVIORAL_DEVIATIONS.md` (BD-nn).
- Machine-readable derivative: `docs/feature-matrix.csv`, generated from this
  table joined with `docs/content-mapping.csv` by
  `python3 scripts/build_feature_matrix.py`. Edit the table, then regenerate —
  never hand-edit the CSV.

| System | Discovered (from inventory) | Status | Evidence (card → commit) | Test status | Deviation | Next action |
| --- | --- | --- | --- | --- | --- | --- |
| Aspects + combinations + essentia | 48 aspects; 287 item / 56 oredict / 116 entity essentia rows | VERIFIED | t_0cc0a0ca → 5ce3dee | 4 unit + 5 game (1:1 vs Aspect.java, TC4 combo/reduce/cull + discovery) | none | — |
| Research graph (data) | 6 categories, 201 entries | VERIFIED | t_eef90e8c → 6a96dee/721e5b9/6201ef5 | 87 unit + 18 game (graph 1:1 vs ConfigResearch) | BD-05 | — |
| Research progression/triggers/points | 201 entries, triggers, warp | VERIFIED | t_25f6eeb2 → de193f3/79c49ba | server-authoritative; hidden-parent quirk reproduced | BD-10 | — |
| Thaumonomicon GUI | 17 GUI ids (pages, nav) | BLOCKED | t_38e57cb0 (gave up 150/150) | none | BD-13 | re-dispatch TC-M3-03 in worktree |
| Research minigames | note puzzle set | IN PROGRESS | t_4ebb83af (WIP: research/NotePuzzle, HexUtils, ResearchNoteData) | none (uncommitted) | BD-13 | salvage → t_8aea81b5, re-dispatch |
| Vis core (pool, costs, visnet) | visnet model, cost chain | VERIFIED | t_d7272167 → 3314f7f | 44 unit + game (centi-vis all-or-nothing, selection quirks) | BD-08 | — |
| Aura + nodes | 6 node types, 3 modifiers, 31-row biome aura | VERIFIED | t_71def828 → aaca66c | seeded wild-node gen, biome behavior (unit + game) | — | — |
| Node behavior (tick/modifiers) | 6 types × modifiers | VERIFIED | t_d00335ab → 47281ea | tick/persistence (unit + game) | — | — |
| Flux (decay/diffusion/thresholds) | flux model | VERIFIED | t_c476f104 → 8395621 | unit + game | — | — |
| Taint (spread, biome effects) | taint system | IN PROGRESS | t_66c37e97 (WIP: taint/) | none (uncommitted) | BD-14 | salvage, re-dispatch TC-M5-04 |
| Worldgen — ores (cinnabar/amber/infused) | 16 worldgen features (ores subset) | TESTED | t_833087c1 → d1d7d5f | integrated on `main`; CI run 39886807032 green | BD-12 (resolved) | structure/biome worldgen still open (M5-05b) |
| Worldgen — structures (totem/mound/eldritch/hilltop) | 16 worldgen features (structures) | IN PROGRESS | t_7c49bfdc (WIP: worldgen/Structure*, TotemGen) | none (uncommitted) | — | salvage, re-dispatch TC-M5-05b |
| Arcane workbench | RecipeArcaneWorkbench | IN PROGRESS | t_81c79f33 (WIP: menus/, recipes/, arcane-recipe-inventory.json) | none (uncommitted) | BD-14 | salvage, re-dispatch TC-M4-03 |
| Infusion altar | TileInfusion* | IN PROGRESS | t_30961958 (WIP: extract_infusion_recipes.py, tiles/) | none (uncommitted) | BD-14 | salvage, re-dispatch TC-M4-04 |
| Crucible / alchemy | TileCrucible* | IN PROGRESS | t_cdece7cd (WIP: crucible-recipe-inventory.json, tiles/) | none (uncommitted) | BD-14 | salvage, re-dispatch TC-M4-05 |
| Essentia storage (smelter, jars) | TileSmelter*, jars | IN PROGRESS | t_e9d23e1c (WIP: tiles/) | none (uncommitted) | BD-14 | salvage, re-dispatch TC-M4-06 |
| Essentia transport (tube network) | tube net | INVESTIGATED | TC-M4-07 (not started) | none | — | dispatch TC-M4-07 |
| Wands (rods/caps/staffs) | 24 wand components | INVESTIGATED | TC-M6-01 (VisContainerItem/WandVis seam in e48c4fb) | seam only | — | dispatch TC-M6-01 |
| Foci + casting | focus classes | INVESTIGATED | TC-M6-02 | none | — | dispatch TC-M6-02 |
| Focus behaviors/combos | projectile classes | INVESTIGATED | TC-M6-03 | none | — | dispatch TC-M6-03 |
| Blocks (catalog) | 45 blocks | IN PROGRESS | t_12f350af parent triage (97f87a6); 04d-f WIP (04a/04b/04c/04d integrated - see the slice rows below) | 04b + 04c device-block probes + 04d furniture/storage probes (game) | — | salvage, re-dispatch 04e-f |
| Items (catalog) | 108 items | IN PROGRESS | t_12f350af parent triage; 04e WIP (04a/04d integrated — see Catalog slice rows) | 04a + 04d slice evidence (see rows below) | — | salvage, re-dispatch 04e |
| Tile entities | 73 tiles | IN PROGRESS | WIP tiles/ across M4/M6 (04a mana-pod, 04b crystal tile, 04c device tiles, 04d furniture/storage/display tiles integrated) | mana-pod tile probes on 04a + crystal-tile placement probes on 04b + device-tile host/tick probes on 04c + furniture tile-resolution + storage round-trip probes on 04d (game) | — | salvage per subsystem |
| Catalog — resources/food slice (TC-M6-04a) | 43 content-mapping rows (1 block, 1 tile entity, 11 items incl. 41 metas, 26 oredict tags, 4 recipes) | VERIFIED | t_49d6b3cd (this commit; replaces t_cc2524ea) | 1 unit (26-row oredict tag guard) + 3 game probes (meta save round-trips/name-keys/stack limit, CSV-contract tag resolution, mana-pod growth + aspect-seeded bean drops); full suite 299 unit + 62/62 game green | D-001 id case; meta precision via data_component | remaining rows on TC-M6-04c–f |
| Catalog — decorative/cosmetic blocks slice (TC-M6-04b) | 12 content-mapping block rows (blockCosmeticOpaque/Solid, blockCrystal + TileCrystal, blockCandle, 4 blockStairs*, wood/stone slabs + 2 legacy double-slab ids) | VERIFIED | t_c5c8533c (this commit; replaces t_dbfabad9) | 3 game probes (block+item registry presence for all 12 ids, original meta ranges 3/16/8/16/2 exposed as the as-number variant property with value-for-value round-trip, assertBlockPresent per meta + per stairs family, crystal shard drops per meta and double-slab drops preserving variant through the block_state component via block loot tables); full suite 299 unit + 65/65 game green | D-001 id case; original meta values verbatim as the variant property; devices/furniture deferred to TC-M6-04c/04d | remaining rows on TC-M6-04c–f |
| Catalog — device/standalone-machine blocks slice (TC-M6-04c) | 14 content-mapping rows (4 multi-variant device blocks blockStoneDevice/Metal/Wooden/Airy + 10 tile entities: bellows, sensor, arcane pressure plate, bore + bore base, 3 arcane lamps, the shared ownership base, nitor) | VERIFIED | t_49883b51 (this commit; replaces t_dc6fe98a) | 3 game probes (registry presence of all 4 device blocks + same-id block items, the original meta ranges 15/15/9/13 exposed verbatim as the as-number variant property with value-for-value round-trip and assertBlockPresent per meta, the 14 flipped contract rows re-resolving through the block-entity registry, (block, variant) -> tile suppliers in DeviceTileFactory producing exactly their own tile and hostless metas producing none — the original's createTileEntity returning null, nitor + arcane ear ticking through the shared device-block ticker with redstone-countdown and Fire-starved burn-out asserts, the pressure plate correctly non-ticking per the original's canUpdate() = false); full suite green + 68/68 game green | D-001 id case; original meta values verbatim as the variant property; device GUIs (gui_alchemy_furnace, gui_arcane_bore) TC-M7-04; essentia-transport furnace machines TC-M4-07 | remaining rows on TC-M6-04d-f |
| Catalog — furniture/storage/display slice (TC-M6-04d) | 26 content-mapping rows (6 blocks: blockTable + same-id BlockTableItem, blockChestHungry, blockMagicBox, blockMirror + same-id BlockMirrorItem, blockArcaneDoor, blockLifter; 2 items: ItemArcaneDoor, ArcaneDoorKey; 14 tile entities: table, deconstruction table, hungry chest, magic box, mirror + essentia mirror, arcane door, arcane levitator, pedestal, wand pedestal, spa, banner, item grate, warding stone + fence; 1 entity: specialitemgrate; 3 gui rows: gui_decon_table, gui_magic_box, gui_spa) | VERIFIED | t_244a0547 (this commit; replaces t_a2a41d5b) | 3 game probes (registry + contract: all 26 flipped rows re-resolve in the registry their csv kind names — block / item / tile_entity / entity_type / menu — and every owned id appears in the contract; (block, variant) -> tile suppliers producing their tile for the table family and for the tiles hosted on sibling device blocks (blockStoneDevice 9/11, blockMetalDevice 5/6, blockWoodenDevice 8, blockAiry 4, blockCosmeticSolid 3/15) while the research-table base and the arcane workbench metas produce none; inventory persistence: the hungry chest's 27 slots and the magic box's item list survive the vanilla saveWithFullMetadata/loadStatic round-trip with slot positions, membership and counts intact); full suite 299 unit + 71/71 game green | D-001 id case; original meta values verbatim as the variant property; tileresearchtable / tilearcaneworkbench and the device GUIs TC-M7-04; essentia-transport furnace machines TC-M4-07 | remaining rows on TC-M6-04e-f |
| Enchantments (Haste, Repair) | 2 registered, 3 dead-code | IN PROGRESS | t_a8f8a37c (WIP: enchantment/, data) | none (uncommitted) | BD-07 | salvage, re-dispatch TC-M6-05 |
| Potions / effects | 9 effects | IN PROGRESS | t_a8f8a37c (WIP: potions/) | none (uncommitted) | — | salvage, re-dispatch |
| Entities + AI + spawn | 50 entities, 29 eggs, 5 spawns | INVESTIGATED | TC-M6-06 | none | — | dispatch TC-M6-06 |
| Armor / equipment | items/armor set | INVESTIGATED | TC-M6-07 (runic shield VERIFIED in e48c4fb; armor items not done) | shield mechanics verified | — | dispatch TC-M6-07 |
| Accessory layer (Baubles semantics) | RING×2/AMULET/BELT/CHARM | VERIFIED | t_76c2874f → c44b219 | Curios + no-Curios both green | BD-06 | — |
| Baubles items (focuspouch etc.) | items/baubles set | VERIFIED | t_0ca9e33b → e48c4fb | accessory + runic tests | — | — |
| Sounds | 66 sound events | VERIFIED | t_32cd081b → 283a810 | 66 events 1:1, play-site map, game guard | BD-02, BD-09 | wire remaining play sites (M7) |
| Registration + id stability | full registry | VERIFIED | t_749e3134 → dc91fe4 | id-contract unit + game | BD-01, BD-02, BD-03 | — |
| Persistence substrate | world + player state | TESTED (PARTIAL) | t_0083a616 → 5ead7f1 | 17 unit + 3 game (lenient codecs, copyOnDeath, S2C) | BD-11 | full-state coverage follows features |
| Networking substrate | 37 payloads | TESTED (PARTIAL) | t_cbc91aec → 7fdcd0a | 37 payloads 1:1, both-dist | — | per-feature sync follows features |
| Configuration | 62 options | VERIFIED (with deviation) | t_c87a23bc → 0674bcb | 53/62 ported, clamps at read | BD-07 | — |
| Models / textures / item+block models | 1107 asset files (models subset) | INVESTIGATED | TC-M7-01; asset inventory + local overlay (D-004) | overlay present, playback guarded | — | dispatch TC-M7-01 |
| Particles / beams / fx | client/fx set | INVESTIGATED | TC-M7-02 | none | — | dispatch TC-M7-02 |
| Entity renderers | renderers/entity set | INVESTIGATED | TC-M7-03 | none | — | dispatch TC-M7-03 |
| GUI parity (non-research) | client/gui set | INVESTIGATED | TC-M7-04 | none | — | dispatch TC-M7-04 |
| Build / load foundation | — | VERIFIED | t_e81a4a3b → 8058029/575c1b6 | 5 unit + smoke gametest, CI | — | — |
| ATM10 startup | 496/470 mods | VERIFIED (startup only) | t_bd0c30d2 → f86a444/ff962ed | load in disposable instance | — | — |
| ATM10 gameplay | representative loops | NOT INVESTIGATED | TC-M9-02 (gated on TC-M6-03) | none | — | gated; dispatch after M6 |
| Recipes (all types) | 279 recipes, 42 oredict, 11 wand triggers | IN PROGRESS | recipe inventories (arcane/crucible) WIP; extraction scripts | extraction present, in-game matching none | — | integrate per crafting system |
| Fluids | 4 fluids | INVESTIGATED | content-mapping | none | — | dispatch with catalog |
| Biomes / dimension (Outer Lands) | 4 biomes, 1 dim (-42) | INVESTIGATED | eldritch realm WIP (world/outerlands, damage_type) | none (uncommitted) | — | salvage (04f), dispatch |

## Fidelity scorecard (system granularity — honest denominators)

Counts are systems, not items; the Discovered column above is the
authoritative content-level denominator. No number here counts
uncommitted WIP as VERIFIED.

- **VERIFIED** (impl + tests + original-behavior comparison): **14** —
  aspects, research graph, research progression, vis, aura+nodes, node
  behavior, flux, accessory layer, baubles items, sounds, registration,
  config, build foundation, ATM10 startup.
- **TESTED (partial substrate)**: **3** — persistence, networking, worldgen ores.
- **IN PROGRESS (WIP, uncommitted, unverified)**: **12** — minigames,
  taint, structures, workbench, infusion, crucible, essentia storage,
  blocks, items, tile entities, enchantments/potions, recipes.
- **INVESTIGATED, not ported**: **13** — essentia transport, wands, foci,
  focus behaviors, entities+AI, armor/equipment, models, particles,
  renderers, GUI parity, fluids, biomes/dimension, ATM10 gameplay.
- **BLOCKED (worker gave up, needs re-dispatch)**: **1** — Thaumonomicon GUI.
- **DOCUMENTED DEVIATIONS**: 14 rows in `BEHAVIORAL_DEVIATIONS.md`.

**Honest summary:** the data/infrastructure layer (aspects, research
logic, vis, aura/flux/nodes, registration, config, accessories, sounds,
persistence/networking substrates) is VERIFIED. The interaction layer
(every crafting system, essentia, taint, foci/wands, armor, entities,
Thaumonomicon UI, full visuals, structures, fluids, dimension) is NOT
VERIFIED. This is **Gate 2 (functional alpha)**, not Gate 3. The
v0.1.0-alpha.1 pre-release states this.

## Kanban enforcement

Every IN PROGRESS row is covered by the salvage card `t_8aea81b5` + the
BLOCKED/re-dispatch rows in `feature-tracking.md`. No row may move to
VERIFIED without a commit on `main` + test evidence recorded on its card;
compilation ≠ tested, startup ≠ verified.
