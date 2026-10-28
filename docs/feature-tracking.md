# Feature Tracking — original feature → task → status

Reconciled against the `thaumcraft-classic-port` board and `origin/main`
HEAD `e48c4fb` on 2026-10-24; re-reconciled against `origin/main`
`3a0eee0` (tag `v0.1.0-alpha.3`) on 2026-10-28 — board counts then: 42 done,
18 todo, 1 ready→running, 1 blocked (see *Systemic blockers* for the
dispatch state). Authoritative parity status lives in
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
| Thaumonomicon GUI | client/gui | TC-M3-03 (t_38e57cb0) → reduced-scope TC-M3-03-s (t_547e7ad8) | BLOCKED (t_38e57cb0 gave up 150/150; no committed code) → **re-dispatched 2026-10-26 as t_547e7ad8**, serial chain position 7, worktree isolation |
| Research minigames | note puzzle classes | TC-M3-04 (t_4ebb83af) → TC-M3-04-s (t_cce3e45d) | BLOCKED → **re-dispatched as t_cce3e45d** (chain position 8, directly after TC-M3-03-s); PARTIAL WIP is on branch `wip-salvage` (`research/NotePuzzle.java`, `HexUtils.java`, `ResearchNoteData.java`), not on the shared tree |
| Aspects | api/aspects | TC-M4-01 (t_0cc0a0ca) | DONE (5ce3dee; docs/aspects.md) |
| Vis core | api/visnet, wands | TC-M4-02 (t_d7272167) | DONE (3314f7f; docs/architecture/vis.md) |
| Arcane workbench core (recipe type + serializer + block/BE + vis) | recipes/, blocks/, tiles/, registration/catalog/CatalogArcaneWorkbench | TC-M4-03-s (t_24af2d37; replaces t_81c79f33) | DONE (this commit; recipes/ArcaneWorkbenchRecipe + SimpleRecipeSerializer + CatalogArcaneWorkbench recipe type/serializer pair registered via DeferredRegister.create(Registries.RECIPE_TYPE/RECIPE_SERIALIZER), blocks/BlockArcaneWorkbench EntityBlock + tiles/TileArcaneWorkbench WorldlyContainer + block entity, one wiring line in ModRegistration.init; 38 of the 109 extracted arcane recipe data files under data/thaumcraft/recipe/ resolve into the live RecipeManager and none logs a RecipeManager parse error (the other 71 carry an unmet neoforge:item_exists gate on an item their owning card has not registered); this row's data was repaired in this commit by TC-M4-03-fix (t_a02e0a31): the 15 rows whose meta-carrying ingredient was the bare {"neoforge:components": {...}} object now carry the registered {"type": "neoforge:components", ...} Ingredient form — that IngredientType is NeoForge's own (NeoForgeMod.DATA_COMPONENT_INGREDIENT_TYPE, NeoForgeMod.java:365), so the earlier claim that they wait on their own card was wrong — and the 16 recipe.112.arcane_shaped.<0..15> banner rows now load because the port registers the thaumcraft:banner_color DataComponentType (registration/ModDataComponents.java) carrying the original's banner NBT "color" byte on the result stack; vis payment runs through TC-M4-02's effectiveCosts→consumeAllVisCrafting all-or-nothing chain (centi-vis pool = real vis x100); ArcaneWorkbenchGameTest 7 probes green: core-registration, shaped direct+mirrored match, shapeless claim-every-item, research-gate flip, exact-pool-empties, short-pool-spends-nothing, assertEveryLoadableArcaneRowLoaded (enumerates the shipped rows through the server's ResourceManager and matches every ungated one against the live instance, its result item, count and thaumcraft:banner_color value); 317 unit + 88/88 game green; gui_arcane_workbench stays on TC-M7-04) |
| Infusion altar core (TC-M4-04-s) | recipes/InfusionCraftingRecipe + InfusionEnchantmentRecipe, tiles/TileInfusionMatrix + TileInfusionPillar + InfusionMatrixLogic, aspect/EssentiaHandler, registration/catalog/CatalogInfusion | TC-M4-04-s (t_35a2c627; replaces t_30961958) | DONE (this commit; CatalogInfusion registers the thaumcraft:infusion + thaumcraft:infusion_enchantment RecipeType/RecipeSerializer pairs and the two tile-entity ids on the shared blockStoneDevice host (variants 1-4: arcane pedestal + matrix + both pillar orientations) through DeviceTileFactory, one wiring line in ModRegistration.init; InfusionMatrixLogic ports validLocation + getSurroundings (25x16x25 scan, +2 per pedestal +1 with an item −2 for the mirrored twin, stabilisers +0.1 each −0.2 mirrored, truncated) and the instability math (symmetry + recipe instability at start, increments capped at 25, event on nextInt(500) <= instability, the 21-sided event table) unit-tested without a Level; TileInfusionMatrix runs the original craft cycle (essentia drain via EssentiaHandler, five-cycle component animation, eject/zap/harm/lightning/warp events, concrete/NBT-tag/enchantment finish paths) and is proven end-to-end by 4 InfusionAltarGameTest probes; 87 extracted recipe rows under data/thaumcraft/recipe/ (63 infusion + 24 infusion_enchantment) now parse — the extractor was fixed to emit the registered neoforge:components ingredient form with its "type" key, to gate every row on neoforge:item_exists for each item it names (an unported item skips the row instead of failing it), and to write 1.21.1 ids (snake_case enchantments, dead_bush/sugar_cane/cobweb/potion, dye meta 0 → ink_sac, wildcard fish → minecraft:fishes); the enchantment field decodes as a ResourceKey and resolves on use, so the two rows naming the TC-M6-05 enchantments load and never match instead of failing the data pack (the dedicated-server smoke gate counts a parse error naming thaumcraft as mod-attributable) — 41 rows resolve into the live RecipeManager today (17 crafting + 24 enchantment), 317 unit + 87/87 game green; the matrix's particles/runes stay on TC-M7-01 and the essentia jars on TC-M4-06) |
| Alchemy/crucible | TileCrucible* | TC-M4-05 (t_cdece7cd) → TC-M4-05-s (t_95373ba3) → probes TC-M4-05-p (t_5fa2e0bb) | **TESTED (t_5fa2e0bb, 2026-10-28)**: the crucible core (from t_95373ba3 — `tiles/TileCrucible.java` + `tiles/CrucibleStateMachine.java` + `recipes/CrucibleRecipe.java` + `registration/catalog/CatalogCrucible.java` recipe type/serializer pair, `thaumcraft:tilecrucible` block entity, 9 oredict item tags, 54 crucible recipe files) now has its behaviour proven by 3 `CrucibleGameTest` probes — `crucibleCoreIsRegistered` (block + `thaumcraft:crucible` type/serializer pair + `tilecrucible` block entity resolve, and the 30 live rows decode through the serializer with their component-carrying catalyst and meta-carrying result intact), `entropicCycleCraftsTheBalancedShard` (a real TileCrucible in a real world: water in, essentia jars thrown through the MERGED `aspect/EssentiaHandler`, ignition, the full entropic cycle, the crafted `itemshard` meta 6 ejected — measured 151 ticks to ignition, pool drained to 0 vis on absorption), and `everyUngatedCrucibleRowIsLoaded` (the data invariant + the research gate; see the row below). Live crucible row count: **30 of the 54 shipped row files** decode into the live `RecipeManager`; 24 gate out on an unmet `neoforge:item_exists`. 28 of the 54 catalysts are the registered `{"type": "neoforge:components", ...}` Ingredient form (the parent row's 22 is stale — it predates the `recipe.0.crucible` rarity expansion), 20 bare `item`, 6 `tag`, **0** bare-`{"neoforge:components": {...}}` form and **0** JSON parse failures. `gui_crucible` stays TC-M7-04, tubes/transport TC-M4-07. |
| Crucible behaviour probes | TileCrucible state machine behaviour, live crucible row count | TC-M4-05-p (t_5fa2e0bb) | **DONE (this commit)** — the parent's non-compiling 611-line `CrucibleGameTest` was rewritten against the merged core (not pasted in) and runs 3 probes in the build. Registration: the type/serializer pair, the `tilecrucible` block entity, and 30 rows decoding through the serializer. Craft cycle: water → reagent → ignition → craft, essentia drawn through the merged `EssentiaHandler`, the result ejected. Data invariant: enumerates the shipped `data/thaumcraft/recipe/*.crucible*` files through the server `ResourceManager`, evaluates every `neoforge:conditions` gate against the live registries, requires every ungated row to be present in the live `RecipeManager` under its own id, requires every component-carrying catalyst to use the registered form (D-015), then seeds the pool and refuses every row gated on research the thrower lacks — **24 of the 30 live rows are research-gated for a CRUCIBLE-only thrower and all 24 are payable by the seeded pool**, so that refusal has teeth. Logged `[TC-M4-05-p] crucible data: 54 row files shipped, 30 pass their gate and are live in the RecipeManager, 24 gated out, 30 resolve in total, 28 use the registered neoforge:components ingredient form, 24 of the live rows gated on unmet research (24 payable)`. Also in this commit: `TileCrucible.setRemoved` no longer parks the server thread on the chunk loader (the removal spill guard read `Level.getBlockState`, which deadlocks `BlockableEventLoop` during `LevelChunk.clearAllBlockEntities` — `runGameTestServer` hung at *Saving worlds*), and the crucible probes claim a 32×32 `gametest_pad_32` structure cell so a neighbouring probe cannot setBlock over the crucible mid-run. 324 unit + **94/94** game green, 94/94 with `-Ptc.noCurios`. |
| Essentia storage | TileSmelter*, jars | TC-M4-06 (t_e9d23e1c) → TC-M4-06-s (t_d9fa0476) | BLOCKED → **re-dispatched as t_d9fa0476** (chain position 2); WIP on `wip-salvage`: `aspect/{EssentiaContainer,EssentiaTags,EssentiaTagDataLoader,IEssentiaContainerItem,IEssentiaTransport}.java`, `tiles/TileMirrorEssentia.java`, `data/thaumcraft/essentia/tags.json`; `aspect/EssentiaHandler.java` and the hungry-chest pair are ALREADY merged (`a9303ea`, TC-M6-04d) — diff, do not duplicate; `TileSmelter*` has no WIP (from scratch). This is the essentia supply path the v0.1.0-alpha.3 notes call the altar "a structure test, not a crafting test" about |
| Essentia transport | tube network | TC-M4-07 (t_2047bca5) | NOT STARTED |
| Aura + nodes | common/world, api/nodes | TC-M5-01 (t_71def828) | DONE (aaca66c; docs/architecture/aura-nodes.md) |
| Node behavior | WorldMagicData tick | TC-M5-02 (t_d00335ab) | DONE (47281ea; docs/architecture/node-behavior.md) |
| Flux | WorldMagicData flux | TC-M5-03 (t_c476f104) | DONE (8395621; docs/architecture/flux.md) |
| Taint | common/taint | TC-M5-04 (t_66c37e97) → TC-M5-04-s (t_f7e25b1c) | BLOCKED → **re-dispatched as t_f7e25b1c** (chain position 3); WIP on `wip-salvage`: `taint/{TaintManager,TaintConversion,TaintBiomes}.java`, `blocks/{BlockTaint,BlockTaintFibres}.java`, `api/entities/ITaintedMob.java`, `data/thaumcraft/damage_type/taint.json`; `potions/PotionFluxTaint.java` belongs to the enchantments/potions card, and the per-chunk store is the merged TC-M5-03 flux world-data store — no second per-chunk store |
| World gen (ores) | worldgen handlers | TC-M5-05 (t_de33def3) | DONE (d1d7d5f; docs/architecture/worldgen.md) |
| World gen (structures) | totems, mounds, eldritch rings, hilltop stones | TC-M5-05b (t_7c49bfdc) | DONE (d4acdb3; StructureParityTest replays original setBlock sequence + roll frequencies; full unit suite green, runGameTestServer 59/59) |
| Wands (rods/caps) | items/wands | TC-M6-01 (t_aaab7bef) | NOT STARTED (VisContainerItem/WandVis seam landed with TC-M8-02, e48c4fb) |
| Foci + casting | focus classes | TC-M6-02 (t_ae471793) | NOT STARTED |
| Focus behaviors/combos | projectile classes | TC-M6-03 (t_a0113d55) | NOT STARTED |
| Block/item catalog | content-mapping.csv | TC-M6-04 (t_12f350af) | DONE (parent triage 97f87a6; a–f below) |
| — catalog: resources/food | content-mapping.csv | TC-M6-04a (t_49d6b3cd; replaces t_cc2524ea) | DONE (this commit; items/ tiles/ blocks/ integrated; CatalogOredictTagsTest 26-row oredict tag guard + CatalogResourcesGameTest 3 probes — meta save round-trips + per-meta name keys + primal-charm stack limit, oredict tags resolve from the CSV contract, mana-pod growth + aspect-seeded bean drops vs the block loot table; full unit suite green, runGameTestServer 62/62) |
| — catalog: decorative blocks | content-mapping.csv | TC-M6-04b (t_c5c8533c; replaces t_dbfabad9) | DONE (this commit; blocks/ + tiles/TileCrystal + registration/catalog/CatalogDecorativeBlocks + data/thaumcraft/loot_table/blocks integrated; CatalogDecorativeBlocksGameTest 3 probes — block+item registry presence and assertBlockPresent for all 12 contract ids, original meta ranges 3/16/8/16/2 exposed as the as-number variant property with value-for-value round-trip, real stair states for the four stairs ids, crystal shard drops per meta + double-slab drops preserving variant through the block_state component; 12 content-mapping rows flipped to done/tested; full unit suite green, runGameTestServer 65/65) |
| — catalog: device blocks/machines | content-mapping.csv | TC-M6-04c (t_49883b51; replaces t_dc6fe98a) | DONE (this commit; blocks/ + tiles/ standalone-machine tiles + registration/catalog/CatalogDevices + blocks/DeviceTileFactory seam integrated; CatalogDevicesGameTest 3 probes - 4 device blocks registered with same-id block items and the original meta ranges 15/15/9/13 carried verbatim as the as-number variant property with value-for-value round-trip, (block, variant) -> tile suppliers resolve only for this card's metas and none for the essentia-transport/banner/node metas, nitor + arcane ear tick through the shared device-block ticker (redstone countdown, Fire-starved burn-out), pressure plate never ticks per the original canUpdate()=false; 14 content-mapping rows flipped to done/tested; full unit suite green, runGameTestServer 68/68; GUIs TC-M7-04 and essentia-transport machines TC-M4-07 deferred) |
| — catalog: furniture/storage/display | content-mapping.csv | TC-M6-04d (t_244a0547; replaces t_a2a41d5b) | DONE (this commit; blocks/ + tiles/ + items/ + menus/ + entities/ + registration/catalog/CatalogFurniture integrated, one wiring line in ModRegistration.init; CatalogFurnitureGameTest 3 probes - all 26 flipped contract rows re-resolve in the registry their csv kind names (block / item / tile_entity / entity_type / menu) with every owned id present, (block, variant) -> tile suppliers resolve for the table family and for the tiles hosted on sibling device blocks (blockStoneDevice 9/11, blockMetalDevice 5/6, blockWoodenDevice 8, blockAiry 4, blockCosmeticSolid 3/15) while the research-table base and arcane workbench metas stay hostless, and the hungry chest's 27 slots + the magic box's item list survive a vanilla saveWithFullMetadata/loadStatic round-trip with slot positions, membership and counts intact; 26 content-mapping rows flipped to done/tested; full unit suite 299 green, runGameTestServer 71/71; tileresearchtable / tilearcaneworkbench, the device GUIs TC-M7-04 and the essentia-transport machines TC-M4-07 deferred) |
| — catalog: trinkets/tools/loot | content-mapping.csv | TC-M6-04e (t_56d498c2; replaces t_bba23f51) | DONE (this commit; loot/ + blocks/BlockLoot + blocks/BlockLootItem + items/ (loot bag, thaumometer, resonator, compass stone, hand mirror, trunk spawner) + items/component/HandMirrorLink + menus/ (hand mirror, trunk) + entities/TrunkHost seam + registration/catalog/CatalogTrinketsLoot integrated, one wiring line in ModRegistration.init; CatalogTrinketsLootGameTest 6 probes - registry + contract (both block ids, all 8 item ids, mirror_link/trunk_upgrade/trunk_inventory component types, both gui ids), urn/crate break-drops follow the original Utils.generateLoot shape (1+rarity..3+rarity per rarity from the Config.initLoot bag tables, silk touch keeps the container with its ITEM_META rarity, pick-block preserves it), treasure bags keep the original 0/1/2 metas on ITEM_META with per-meta name keys and stack limit 16, the trunk spawner is a no-op and unconsumed while TC-M6-06's entity is absent, the hand-mirror menu is the original 1 input slot at (80,24) over 36 player slots on its own container, and the dark-node announcement map prunes past the original 10 s bound inside the original client-only body while the server-side inventoryTick keeps the world.field_72995_K gate; 10 content-mapping rows flipped to done/tested; full unit suite 299 green, runGameTestServer 77/77; the traveling-trunk entity itself TC-M6-06, lodestone icon/model TC-M7-01) |
| — catalog: eldritch realm | content-mapping.csv | TC-M6-04f (t_146ef5b3) | PARTIAL (f252c58 integrates world/outerlands maze cells + MazeGeneratorTest; catalog items + data/thaumcraft/damage_type still WIP in wip-salvage) |
| Enchantments/potions | lib/enchantment, api/potions | TC-M6-05 (t_a8f8a37c) → TC-M6-05-s (t_a758393c) | BLOCKED → **re-dispatched as t_a758393c** (chain position 4); WIP on `wip-salvage`: `enchantment/EnchantmentBehaviours.java`, `registration/ModEnchantments.java`, `data/thaumcraft/enchantment/{ench_haste,ench_repair}.json`, ten `potions/Potion*.java`, `gametest/EnchantmentPotionGameTest.java`; `recipes/InfusionEnchantmentRecipe.java` is ALREADY merged (`a9303ea`) and decodes the enchantment as a `ResourceKey` resolved on use — which is exactly why the two infusion rows naming `thaumcraft:ench_repair` / `ench_haste` load today and never match. Registering those two enchantments turns two shipped rows live; it is this card's highest-value hunk |
| Entities/AI | common/entities (125 classes) | TC-M6-06 (t_cd287131) | NOT STARTED (entities/ dir exists on shared tree, uncommitted, unverified) |
| Armor/equipment | items/armor | TC-M6-07 (t_1ba6e428) → TC-M6-07-s (t_54791c73) | BLOCKED → **re-dispatched as t_54791c73** (chain position 5). WIP is negligible (`api/IRunicArmor.java` only) — treat as from-scratch, not salvage. Runic shielding MECHANICS landed early with TC-M8-02 (`e48c4fb`) and the accessory provider layer + Curios bridge are merged (TC-M8-01/02), so this card must not re-implement slot handling, the shielding maths or the baubles seam; reduced scope = ONE family + its signature effect, the rest to named follow-up cards |
| Localization (en_us.json) | assets/thaumcraft/lang + registration name keys | TC-LOC-s (t_a2a74726) | DONE (this commit; DEFECT B closed — `assets/thaumcraft/lang/` did not exist, so every shipped name rendered as its key. 128 keys: 27 blocks, 15 single-name items, 58 per-meta names over the 9 multi-meta families (ItemResource 19, ItemNugget 15, ItemShard 7, ItemBaubleBlanks 4, ItemRingRunic 4, ItemLootBag 3, ItemAmulet/GirdleRunic 2, ItemAmuletVis 2), 9 container titles (5 menu ids + the 4 `getDisplayName` keys the shipped block entities pass), 1 entity, 15 tooltip/action lines; 118 strings are the original 4.2.3.5 en_US.lang wording (§ codes stripped, `%TYPE`→`%s`), 2 come from the `content-mapping.csv` display_name column, 8 are authored because the original never named them; LocalizationGameTest 2 probes (128/128 registry-derived keys covered, 0 dead keys) + LocalizationFileTest 7 unit; 324 unit + 91/91 game, 91/91 no-Curios. Out of scope here: per-variant labels for the mega-blocks (variants ride a `variant` IntegerProperty and their block items did not override the stack-sensitive name, so the default-variant label showed — closed by TC-M7-01b / t_f40bbc0d, which added the 107 `block.thaumcraft.<id>.<meta>` keys, file now 235), the creative tab label (no tab is registered yet), effects/enchantments (none registered), death messages (no custom damage types), research names/pages (Thaumonomicon UI, TC-M3-03/04)) |
| Per-variant block display names | `tile.<id>.<meta>.name` keys for the multi-variant mega-blocks | TC-M7-01b (t_f40bbc0d) | DONE (this commit; found while verifying TC-LOC-s in the installed test jar — `block.thaumcraft.blockstonedevice` resolved to the meta-0 label ("Alchemical Furnace") for every meta, because 1.21.1 has no per-state block display name and the block items did not override the stack-sensitive name. Fix: `blocks/BlockVariantNames` declares the per-variant name domain (13 mega-block families, 107 (block, meta) pairs, mirroring the original `tile.<id>.<meta>.name` set verbatim including the metas the original left unnamed — blockMetalDevice 4, blockWoodenDevice 3, blockAiry 6-9 — and the slabs' name-keyed originals `tile.blockCosmeticSlabStone.arcane/eldritch.name`), and `blocks/VariantBlockItem` overrides `getDescriptionId(ItemStack)` — the hook `Item.getName(ItemStack)` builds the component from — to read the stack's `variant` value (vanilla `block_state` component first, ported `ITEM_META` component second) and resolve `block.thaumcraft.<id>.<meta>`; component-less stacks keep the plain block key. Wired into all 13 families: the 4 device items become the original's `Block*DeviceItem` classes (thin subclasses), `blocktable`/`blockmirror` get `BlockTableItem`/`BlockMirrorItem` (their `item_class` rows' names), and the 7 per-card item classes extend it (their hand-copied `getPlacementState` variant round-trip now lives in the base). en_us.json grows 128 -> 235 keys; `scripts/build_variant_lang.py` regenerates the 107 from the declaration + the reference lang (original lang stays out of the repo, D-004/licensing). The shared items also apply the stack's variant on placement, so the `blocktable`/`blockmirror` "places the default variant" deferral is closed. LocalizationGameTest 3 probes (235/235 registry-derived keys — the block items now contribute every state of their `variant` property, 0 dead; + `megaBlockVariantsCarryTheirOwnLabels`, which drives the real `getName(ItemStack)` for all 107 pairs in both carriers and fails if a variant resolves to the family's base key: 93 of them differ from it), LocalizationFileTest 9 unit (+ full per-variant set, + original-wording parity in both directions), 326 unit + 92/92 game, 92/92 no-Curios. Out of scope: the per-variant creative-tab entries and the per-variant models (TC-M7-01 — no tab is registered yet, so the base label is what a component-less stack shows), non-English languages) |
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
4. **Remediation IN EFFECT — orchestrator findings, 2026-10-28.**
   - **Incident: the serial chain was frozen, not idle.** The 19
     reduced-scope cards are chained child→child
     (`t_95373ba3 → t_d9fa0476 → t_f7e25b1c → t_a758393c → t_54791c73 →
     t_db7fcc2d → t_547e7ad8 → t_cce3e45d → t_becee16b → t_01f994cf →
     t_21f78517 → t_62ddfe14 → t_26136494 → t_2c24f643 → t_e87a7f7c →
     t_215d6fd4 → t_7eb3e438 → t_d827c350 → t_1476daed`), and the chain
     head `t_35a2c627` (TC-M4-04-s, infusion) sat `blocked` although its
     work was already merged as `a9303ea`. Symptom read as "busy":
     `running=0`, `todo=19`, zero dispatcher spawns. Closed by the
     orchestrator with evidence (317 unit tests 0 failures, 89/89 game,
     89/89 no-Curios, CI run 40697171757 on `v0.1.0-alpha.3`), which
     promoted TC-M4-05-s to `ready` and started the pipeline.
   - **Isolation is the default now:** every chain card runs
     `workspace_kind: worktree` on its own branch under
     `.worktrees/<task>`, so the 2026-10-24 shared-tree `compileJava`
     poisoning cannot recur. Integration stays hunk-wise onto `main`
     per the TC-M5-05 precedent.
   - **Worker model is a real dependency (operator directive
     2026-10-28: use `swift`, not `gemma4`):** the worker model is pinned
     per card to `custom:swift` (alias `swift` → `swift-1.5-iq3_s`,
     `hermes kanban set-model <id> swift --provider custom:swift`) on the
     five nearest chain cards and on `t_a2a74726`. The earlier note here
     that the `custom:coder` endpoint was unreachable is stale — all three
     local endpoints (`swift` 192.168.13.221:8080, `coder`
     192.168.12.88:8080, `gemma4` 192.168.13.164:8033) answer HTTP 200 on
     `/v1/models` as of this pass, so probe before treating an endpoint as
     down. Two measured properties: `swift-1.5-iq3_s` is a **reasoning**
     model (a probe with `max_tokens: 8` returned `finish_reason: length`,
     `content: null`, all of the budget in `reasoning_content`), so a
     tight per-turn completion cap reads as an empty response — that is the
     shape of the "Response Stopped" failures the board recorded as worker
     crashes.
   - **Dispatcher host fault RESOLVED 2026-10-28.** The frozen chain
     turned out to have a second, mechanical cause: every spawned worker
     died ~60 s in with `Kanban owner refused: Kanban execution unknown;
     retained by owner` then `[kanban-worker-exit] rc=1` (raise site
     `hermes_cli/kanban_worker_client.py:79`, the RPC `kanban.run` receipt
     resolving to `unknown`). The gateway owner process predated the
     installed kanban owner code, so each card burned its failure budget
     (failure-limit 2) and self-blocked; the agent cannot clear it because
     the lifecycle guard refuses `hermes gateway restart` from inside the
     gateway. The owner was restarted at 10:45 on 2026-10-28; from then on
     `blocked=0`, spawns land, and the board reads `done=44, running=1`.
     Triage steps are recorded in
     `~/.hermes/skills/kanban-orchestration/references/dispatch-triage.md`.
   - **Crucible integrated 2026-10-28 (TC-M4-05-s, `t_95373ba3` →
     `3dcf3b6` + `4307fe6`).** The interrupted worker's tree was
     integrated hunk-wise: crucible core (tile entity, state machine,
     recipe type/serializer pair, 9 ore-dict tags, 54 gated recipe files)
     compiles and boots green — 324 unit / 0 failures, 91/91 game tests,
     no `thaumcraft` ERROR line over 1404 loaded recipes, CI run
     40711328720 success. Behaviour is **not** proven: the worker's
     611-line game test did not compile and was excluded, so the three
     probes are their own card `t_5fa2e0bb`. One integration fix was needed
     (`TileCrucible.onDataPacket` built a `RegistryFriendlyByteBuf` from
     `HolderLookup.Provider.asRegistryAccess()`, which does not exist →
     `level.registryAccess()`), and the docs flip had to be rebuilt because
     a note containing a comma pushed a `content-mapping.csv` row to 9
     fields and `ContentMappingCsv.read` throws `expected 8 fields, got 9`
     — that is the rule behind this file's plain-text-line-edit convention.
   - **Preflight comments** on `t_95373ba3`, `t_d9fa0476`,
     `t_f7e25b1c`, `t_a758393c`, `t_54791c73`: verified main state, the
     per-area WIP inventory on `wip-salvage`, the merged files that must
     NOT be re-added (`aspect/EssentiaHandler.java`,
     `recipes/InfusionEnchantmentRecipe.java`,
     `blocks/BlockChestHungry.java` + `tiles/TileChestHungry.java`), and
     the recipe ingredient/gating rules the `runServer` smoke gate
     enforces.
   - **Scope discipline for the 150-iteration cap:** each card ships
     its demonstrable hunk as its own commit first and names the rest in
     `left_out_of_scope`, so a partial pass lands value instead of
     burning the budget and auto-blocking.

## Release honesty

v0.1.0-alpha.1 (tag 2026-10-22) is a **pre-release of the foundation
only**: infrastructure, data systems (aspects/vis/research/aura/flux/
nodes/worldgen-ores/accessories) are done and test-verified; the
interaction layer (workbench, infusion, crucible, essentia, taint,
enchantments, armor, entities, Thaumonomicon UI, foci, M7 visual
parity) is NOT DONE. The GitHub release notes say so; do not treat the
tag as gameplay parity.
