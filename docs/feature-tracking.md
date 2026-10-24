# Feature Tracking — original feature → task → status

Reconciled against the `thaumcraft-classic-port` board and `origin/main`
HEAD `e48c4fb` on 2026-10-24. Authoritative parity status lives in
`compatibility-matrix.md`; this file maps each original feature area to the
Kanban task (board `thaumcraft-classic-port`) that implements it.

Statuses (DONE only with test evidence):
- **DONE** — merged to main, unit + game tests green, linked commit.
- **PARTIAL WIP** — code exists on the shared tree, uncommitted and
  unverified; owner task did not complete.
- **BLOCKED** — dispatched worker exhausted its iteration budget
  (150/150) and gave up; task needs re-dispatch, ideally in an isolated
  worktree (see Systemic blockers).
- **NOT STARTED** — no implementation.

| Feature area (original) | Source evidence (TC 4.2.3.5) | Task (id) | Status |
| --- | --- | --- | --- |
| Environment/artifact verification | JARs, instance | TC-M0-01 (t_6ebc7418) | DONE |
| Decompilation workflow | REA + Vineflower | TC-M0-02 (t_22824a88) | DONE |
| API repo reference | api repos @ 1.7.10 | TC-M0-03 (t_cc8fc2e9) | DONE |
| Content + system inventory | decompiled tree | TC-M0-04 (t_83241bc7) | DONE (docs/original-content-inventory.md) |
| Asset inventory/mapping | assets (1079 files) | TC-M0-05 (t_66f308b2) | DONE (0915581) |
| Balance values | decompiled constants | TC-M0-06 (t_b9cd76b8) | DONE (0060616) |
| Build/load foundation | — | TC-M1-01 (t_e81a4a3b) | DONE (8058029, 575c1b6) |
| Registration + id stability | common registration | TC-M2-01 (t_749e3134) | DONE (dc91fe4) |
| Persistence (world/player) | WorldMagicData, PlayerKnowledge | TC-M2-02 (t_0083a616) | DONE (5ead7f1) |
| Networking | common/lib/network (40 classes) | TC-M2-03 (t_cbc91aec) | DONE (7fdcd0a; fa569b7 sound-id fix) |
| Configuration | config class | TC-M2-04 (t_c87a23bc) | DONE (0674bcb) |
| Research graph | ConfigResearch.java (201 entries) | TC-M3-01 (t_eef90e8c) | DONE (6a96dee, 721e5b9, 6201ef5) |
| Research progression | ResearchManager | TC-M3-02 (t_25f6eeb2) | DONE (de193f3, 79c49ba) |
| Thaumonomicon GUI | client/gui | TC-M3-03 (t_38e57cb0) | BLOCKED (worker gave up; no committed code) |
| Research minigames | note puzzle classes | TC-M3-04 (t_4ebb83af) | BLOCKED; PARTIAL WIP (research/NotePuzzle.java, HexUtils.java, ResearchNoteData.java uncommitted) |
| Aspects | api/aspects | TC-M4-01 (t_0cc0a0ca) | DONE (5ce3dee; docs/aspects.md) |
| Vis core | api/visnet, wands | TC-M4-02 (t_d7272167) | DONE (3314f7f; docs/architecture/vis.md) |
| Arcane workbench | RecipeArcaneWorkbench | TC-M4-03 (t_81c79f33) | BLOCKED; PARTIAL WIP (menus/, recipes/, docs/arcane-recipe-inventory.json, scripts/extract_arcane_recipes.py uncommitted) |
| Infusion altar | TileInfusion* | TC-M4-04 (t_30961958) | BLOCKED; PARTIAL WIP (scripts/extract_infusion_recipes.py + tiles/ uncommitted) |
| Alchemy/crucible | TileCrucible* | TC-M4-05 (t_cdece7cd) | BLOCKED; PARTIAL WIP (docs/crucible-recipe-inventory.json, scripts/extract_crucible_recipes.py + tiles/ uncommitted) |
| Essentia storage | TileSmelter*, jars | TC-M4-06 (t_e9d23e1c) | BLOCKED; PARTIAL WIP (tiles/ uncommitted) |
| Essentia transport | tube network | TC-M4-07 (t_2047bca5) | NOT STARTED |
| Aura + nodes | common/world, api/nodes | TC-M5-01 (t_71def828) | DONE (aaca66c; docs/architecture/aura-nodes.md) |
| Node behavior | WorldMagicData tick | TC-M5-02 (t_d00335ab) | DONE (47281ea; docs/architecture/node-behavior.md) |
| Flux | WorldMagicData flux | TC-M5-03 (t_c476f104) | DONE (8395621; docs/architecture/flux.md) |
| Taint | common/taint | TC-M5-04 (t_66c37e97) | BLOCKED; PARTIAL WIP (taint/ uncommitted) |
| World gen (ores) | worldgen handlers | TC-M5-05 (t_de33def3) | DONE (d1d7d5f; docs/architecture/worldgen.md) |
| World gen (structures) | totems, mounds, eldritch rings, hilltop stones | TC-M5-05b (t_7c49bfdc) | DONE (d4acdb3; StructureParityTest replays original setBlock sequence + roll frequencies; full unit suite green, runGameTestServer 59/59) |
| Wands (rods/caps) | items/wands | TC-M6-01 (t_aaab7bef) | NOT STARTED (VisContainerItem/WandVis seam landed with TC-M8-02, e48c4fb) |
| Foci + casting | focus classes | TC-M6-02 (t_ae471793) | NOT STARTED |
| Focus behaviors/combos | projectile classes | TC-M6-03 (t_a0113d55) | NOT STARTED |
| Block/item catalog | content-mapping.csv | TC-M6-04 (t_12f350af) | DONE (parent triage 97f87a6; a–f below) |
| — catalog: resources/food | content-mapping.csv | TC-M6-04a (t_49d6b3cd; replaces t_cc2524ea) | DONE (this commit; items/ tiles/ blocks/ integrated; CatalogOredictTagsTest 26-row oredict tag guard + CatalogResourcesGameTest 3 probes — meta save round-trips + per-meta name keys + primal-charm stack limit, oredict tags resolve from the CSV contract, mana-pod growth + aspect-seeded bean drops vs the block loot table; full unit suite green, runGameTestServer 62/62) |
| — catalog: decorative blocks | content-mapping.csv | TC-M6-04b (t_dbfabad9) | BLOCKED; PARTIAL WIP (blocks/ uncommitted) |
| — catalog: device blocks/machines | content-mapping.csv | TC-M6-04c (t_dc6fe98a) | BLOCKED; PARTIAL WIP (blocks/, tiles/ uncommitted) |
| — catalog: furniture/storage/display | content-mapping.csv | TC-M6-04d (t_a2a41d5b) | BLOCKED; PARTIAL WIP (registration/catalog/ uncommitted) |
| — catalog: trinkets/tools/loot | content-mapping.csv | TC-M6-04e (t_bba23f51) | BLOCKED; PARTIAL WIP (loot/, data/thaumcraft/loot_table/, gametest/CatalogTrinketsLootGameTest.java uncommitted) |
| — catalog: eldritch realm | content-mapping.csv | TC-M6-04f (t_146ef5b3) | PARTIAL (f252c58 integrates world/outerlands maze cells + MazeGeneratorTest; catalog items + data/thaumcraft/damage_type still WIP in wip-salvage) |
| Enchantments/potions | lib/enchantment, api/potions | TC-M6-05 (t_a8f8a37c) | BLOCKED; PARTIAL WIP (enchantment/, potions/, data/thaumcraft/enchantment/, gametest/EnchantmentPotionGameTest.java uncommitted) |
| Entities/AI | common/entities (125 classes) | TC-M6-06 (t_cd287131) | NOT STARTED (entities/ dir exists on shared tree, uncommitted, unverified) |
| Armor/equipment | items/armor | TC-M6-07 (t_1ba6e428) | BLOCKED (runic shielding mechanics landed early with TC-M8-02, e48c4fb; armor items themselves not done) |
| Models/textures | assets + renderers | TC-M7-01 (t_1625375e) | NOT STARTED |
| Particles/beams | client/fx | TC-M7-02 (t_b1249368) | NOT STARTED |
| Entity renderers | renderers/entity | TC-M7-03 (t_6c2e5f60) | NOT STARTED |
| GUI parity | client/gui | TC-M7-04 (t_52cdd02c) | NOT STARTED |
| Sounds | sounds assets | TC-M7-05 (t_32cd081b) | DONE (283a810; 66 events, 3 wired play sites) |
| Accessory layer | Baubles semantics | TC-M8-01 (t_76c2874f) | DONE (c44b219; docs/architecture/accessories.md) |
| Baubles items | items/baubles | TC-M8-02 (t_0ca9e33b) | DONE (e48c4fb; docs/architecture/runic-shielding.md) |
| ATM10 startup | disposable instance | TC-M9-01 (t_bd0c30d2) | DONE (f86a444, ff962ed; docs/atm10-compatibility.md) |
| ATM10 gameplay | disposable instance | TC-M9-02 (t_95c84692) | NOT STARTED (gated on TC-M6-03) |
| Regression | all suites | TC-M10-01 (t_fa8b23a7) | DONE (065dceb; docs/regression-report-2026-10-22.md — point-in-time at c44b219, not current HEAD) |
| Release | release gate | TC-M10-02 (t_596882c9) | DONE (347647f; v0.1.0-alpha.1 pre-release — gameplay systems NOT YET DONE, see table) |

## Systemic blockers (why the BLOCKED rows exist)

1. **Iteration budget exhaustion.** Sixteen tasks ended with
   "Iteration budget exhausted (150/150) — task could not complete".
   These are large build-a-block-and-verify tasks against a full
   Gradle/NeoForge tree; a single worker pass does not fit.
2. **Shared-tree contention.** All workers share
   `~/Thaumcraft-Classic-Modern/thaumcraft-classic` as their workspace
   (`workspace_kind: dir`). Parallel workers left untracked WIP in
   `tiles/`, `menus/`, `recipes/`, etc., which repeatedly broke
   `compileJava` for *other* workers. A poisoned git index (staged
   deletions of live code, incl. the whole accessory layer) was found
   and reset on 2026-10-24; no committed history was lost.
3. **Remediation:** re-dispatch BLOCKED tasks one at a time (or in
   `git worktree` isolation via `workspace_kind: worktree`), each with
   a reduced scope, and integrate hunk-wise per the TC-M5-05 precedent
   (t_833087c1 → d1d7d5f).

## Release honesty

v0.1.0-alpha.1 (tag 2026-10-22) is a **pre-release of the foundation
only**: infrastructure, data systems (aspects/vis/research/aura/flux/
nodes/worldgen-ores/accessories) are done and test-verified; the
interaction layer (workbench, infusion, crucible, essentia, taint,
enchantments, armor, entities, Thaumonomicon UI, foci, M7 visual
parity) is NOT DONE. The GitHub release notes say so; do not treat the
tag as gameplay parity.
