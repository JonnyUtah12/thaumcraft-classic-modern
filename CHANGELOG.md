# Changelog

All notable changes to Thaumcraft Classic (Unofficial) are documented here.
Format: Keep a Changelog; versioning: Semantic Versioning (see
`docs/release-process.md`). Project versions are independent of the pinned
Minecraft 1.21.1 / NeoForge 21.1.259 targets.

## [Unreleased]

The Thaumonomicon is in the shipped code path. The BOOK chain (BOOK-1 open path,
BOOK-2 tab strip and entry listing generated from the shipped research graph, BOOK-4
per-entry detail panel, the note item with its hex-grid puzzle, and the note-request
arm on node click) landed on `main` at `497e855`; 28 book game probes and 12 book unit
suites measure it, and the merged tree runs unit **594 / 0 failures / 2 skipped** with
**All 218 required tests passed** in both the Curios and `-Ptc.noCurios` modes.
The book's chrome strings are authored; its page layer resolves `tc.research_*`
translation keys that no committed lang file defines, so a code-only build shows raw
keys there. **D-016 is decided** (option 2: generate original display strings at build
time from the player's own extracted assets), which unblocks the text work - see the
decision section. Also landed: the `blockEldritch` meta list is corrected to the shipped
property range (meta 6 is unnamed, there is no meta 11).

**The published alpha.5 jar is unchanged** (built from `7c4ebaf`); everything in this
section reaches players only in the next release cut.

- **The pech trade window opens** (`TC-M7-04-m3-b`, card `t_63e11e8c`): `thaumcraft:gui_pech` (gui_id 1) lands as `PechMenu` + `PechScreen` - the 5-slot trade grid, the player grid 9x3 and inventory 9x3 (36 slots), the dice button at (150,6) with the `PECH_DICE` sound - and the open seam `EntityPech.mobInteract` -> `openTradeGui` -> `Player.openMenu(container, container::getComponents)` (`ContainerPech.java:18-32`, `GuiPechTrade.java:19-24`, `EntityPech:696-702`, `ModGuiHandler:19-23`). Deviation BD-73: the window is the third in the port whose open path passes while its screen-superclass identity deviates (the BD-49/BD-70 idiom, row 78); the dice roll lands with the `ChestGenHooks` loot-table card; the sprite parity with the asset-overlay build is open (TC-M7-03-s); the original's 32-key slot-numbered drop NBT is one non-reserved key (`PechDropOwner`) because 1.21.1 reserves `Owner` for a UUID at ItemEntity save (the measured crash: `IllegalStateException: Missing id for entity!`). The keyed overload `openContainerKeyed` stays a documented no-op with its javadoc's own reason. Suite on the merged tree (`27e16ea4` + docs): `build` exit 0 (979 unit tests / 0 failures / 2 skipped), `runGameTestServer` `All 384 required tests passed` x2, `-Ptc.noCurios runGameTestServer` `All 384 required tests passed` x2; the suite grew 381 -> 384 with the pech window probes.
- **Ward hexagram + shield runes land as particles** (`TC-M7-02-c-s-f4`, card `t_6cfc2f2f`, branch `wt/t_6cfc2f2f-v3`): `FXBlockWard` and `FXShieldRunes` become `thaumcraft:ward`, `thaumcraft:shield_runes` and `thaumcraft:shield_runes_mob`. The hexagram draws the 16 `hemis` frames (copied whole-file from `textures/models`) with the fade-law frame ramp `min(15,(int)(15F*fade))`, the constructor roll order (lifetime `12 + rand(5)`, scale `1.4 + gaussian*0.3`, rotation `nextInt(360)`, per-component jitter `clamp(f − 0.6 + rand*0.2, −0.4, 0.4)` with the normal-axis component zeroed) pinned in client-free `FxWardRules`, and the GL rotation chain baked to a spawn offset measured to push exactly `0.505` along every face normal. The runes ride the packet's **source** entity (entity-id carrier, resolved in the client level per tick), aim at its **target** (`-3` → straight down), honour the handler's `-1` (two runes, pitch 90/270), `-2` (90) and `-3` (270) sentinels, scale with `0.4 * width`, and the mob branch swaps to the `ripple1..15` frames as its own registered type (BD-71); senders `TcFxPlans.wardBlock`/`shieldRunes`, `TcWardHitEffects` (the warded glass meta fires and suppresses vanilla's dig particles, the amber metas fall through), `FXWardBlockPayload`, and the `ClientEffectHooks.fxShield` hook wired to the rune plan; the alpha-255 `FXSheet` branch ships with them. Gates: 3 new `FxParticleGameTest` probes (12 in the file) each proven RED first (namespace 14-pin vs 13, sentinel 2-rune pin vs 3, normal-axis 0-pin vs 0.125, measured scale/lifetime bands vs invented ones — each exit 1, then the suite exit 0), `WardRuneRulesTest` (18 unit), the `ParticleSpriteSlicingTest` ward/shield frame family (whole-file byte identity; `hemis0` belongs to the ward alone). The `hemis.obj` shell draws as an atlas sprite and the `240` light pair collapses to max block light (BD-72); the camera basis is not chased (BD-16). No content-mapping row (fx set, `feature-tracking.md`).

- **Pech + Pech Blast** (`TC-M6-06-s-c3-a`): `thaumcraft:pech` and `thaumcraft:pechblast` land in their own `CatalogPech` holder, salvaged from the WIP preserved at `3af7699` + `d795cb1` after the parent card exhausted twice. The figures are cited line by line - the 0.6 x 1.8 box at the 4-chunk tracking the family rule demands, the 3-tick mob and 20-tick projectile intervals, health 30.0 / attack 6.0 / speed 0.5 with the +2 armour term, the goal table at slots 0 / 1 / 3 / 4 / 5 / 6 / 7 / 8 / 9 (plus 9 / 10 / 11 when tamed), the 8-entry loot array with its 75 % drop roll and 16-stack cap, the 4-slot trade stock at `2 + level / 2` price, the three synced data caps, and the blast's 1.5-motion / 4.0-spread launch on the pech's 20-tick blast-goal cooldown (`EntityAIArrowAttack(this, 0.6, 20, 30, 15.0F)`, :65). What the probes prove: the pech **picks up** loot, follows and attacks when tamed, names itself per type (Forager / Mage / Stalker), carries the original goal table at the original slots, and the blast firing seam of :481 puts **exactly one** blast in the world - owner = the firing pech, motion = the :483 1.5 - while a bow pech looses an arrow and no blast and the ranged goal stays unarmed with no target. Plain statement on the spawn: **the pech does not spawn in a shipped world today** - the gate is live (it rejects the pad biome; overworld / `neoforge:is_magical` / the taint + magical-forest + eerie exclusions / the 16-inflated box with its 4-pech cap all pinned) and the probe counts the shipped biomes carrying `neoforge:is_magical` at **zero**, so the spawn needs the biome cards (rows 961-963) and c6's spawn-table entry (row 347). The blast's flight, its impact and its 500-tick fuse are measured on real server ticks (`empty_24` - the 1x1x1 `empty` template has no ticking area, and the first run of these probes read y-motion 0.0 and tickCount 0). Plain statement of what does not work in a world: the mage form never appears - `thaumcraft:wandcasting` is unregistered - so the blast's damage and knockback are unit-pinned only (BD-68); the trade window never opens (BD-67); the boss flag (BD-69) and the village spawn gate (BD-70) are 1.21.1 gaps. Gate: 918 unit tests / 0 failures / 0 errors (2 skipped) and **All 350 required tests passed** with and without Curios (exit 0 on all four legs: compile, `build`, `runGameTestServer`, `-Ptc.noCurios runGameTestServer`).

- **The research page's recipe body is now drawn, on the unified book screen** (`TC-M3-04-s-iv-b-2`, card `t_79eea20e`): the open half of **BD-64** closes without resurrecting the node-browser screen. The unified `client/ThaumonomiconScreen` detail panel is the host. Its box is split by `research/book/BookDetailPanelLayout` - a plain record, because D-019 rule 2 says an assertion belongs where `runGameTestServer` can reach it and that server never constructs a `Screen` - into the text rows the panel keeps above the body and a region at its bottom, and that record asks `recipes/RecipePageLayout` (the geometry measured off `GuiResearchRecipe#drawCraftingPage:755-946`, unchanged) where the original's page block - 3x3 grid, result, station label - lands inside the region. The block is fitted **down, never up** (scale 1.0 when the panel is wide and tall enough, 0.5 floor): the shipped panel reserves a 100x146 column and the original's 104x156 block scales to fit it, every offset rounding on its own, so items stay native size (BD-17's delta, as on the pane this drew on). `ResearchDetailModel.Detail` gains a `recipeId` component filled from `ResearchRecipeLinks.linkedRowOf(entry)` - the entry answers for its own linked page, guarded so a link naming another entry never paints this one, and the id stays a string: the server-safe layer never touches `net.minecraft.world.item.crafting`. The client resolves it through `RecipePageDisplay#resolve` against the **live `RecipeManager`**, so a row the client was not sent - gated out of the sync - draws no body and the panel's full text column comes back. The grid is vanilla item renders over the vanilla 18px `container/slot` sprite (no ARR art, D-004) under a code-drawn outline, labelled `recipe.type.workbench` / `recipe.type.workbenchshapeless` as the page labelled it. Text yields before the body never does: a text row is dropped only when its box would cross into the body region, the body is clipped to its region, and a panel with no room for the block draws text only. Probes: 1 unit (`ThaumonomiconRecipePageTest#entryDetailCarriesTheRowOfTheEntryThatOwnsThePage` - the KNOWFRAG entry's panel carries `thaumcraft:recipe.256.crafting_shaped`, every other entry's panel carries nothing, and the panel's id is the id the pager puts on the page), 1 contract (`ThaumonomiconDetailPanelContractTest#thePanelReservesARegionAtItsBottomForTheRecipeBody` - the shipped panel splits into 3 text rows + a 100x146 body region and the original's 104x156 block fits the panel's column at scale <= 1), and 3 game probes (`ThaumonomiconRecipePageGameTest#theEntryDetailCarriesTheRowItsPanelDraws` - the entry answers with its own page's row id, live in the manager the server crafts with, exactly one entry in the 201-entry graph having one; `#theBookPanelReservesARegionForTheRecipeBody` - grid, result and label land inside the shipped panel's region and the label fits its column; `#aPanelWithNothingToDrawShowsItsTextBody` - all three no-draw cases (no linked row, an id the live `RecipeManager` does not carry, no room) leave the text column full). The vanilla `container/slot` sprite, the fit-to-pane scale and the code-drawn outline stay BD-17's art deltas; the other 44 `normal_crafting` pages and the port-type page bodies stay open. Measured on the merged tree (this slice over main `2d2bc94`): `compileJava`+`compileTestJava` exit 0, `gradlew build` **885 unit tests / 0 failures / 2 skipped** (`ThaumonomiconRecipePageTest` 10 — 9 + 1 new, `ThaumonomiconDetailPanelContractTest` 7 — 6 + 1 new, `BehavioralDeviationLedgerContractTest` 7, `ThaumonomiconScreenRegistrationTest` green with the client-screen isolation rule intact), `runGameTestServer` **331 game tests complete** with the 3 new probes green in every leg and exit **1** on a single required red, `bossroombuildsthroughtheseam`, an Outer Lands probe byte-identical to main that asserts on absolute coordinates in the shared game-test level: main's tip is 5/5 green at 328 probes on this host AND 2/2 green at 331 with three no-op probes added, so the suite size is exonerated and the trigger is the tick weight this card's probes add to a doorway pass the Outer Lands probe budgets in ticks — a probe-isolation gate (main's own rule from `554ac56`), measured and written up in `docs/testing.md`, with the probe fix handed to a follow-up card. **CI at the landing tip `f4bac9e`: `build` + `game-test-no-curios` + `runServer` all success.** BD ledger 66 rows, ids contiguous, `docs/bd-id-map.csv` untouched (BD-64 was pre-allocated, nothing minted, no renumbering); `content-mapping.csv` 1067 rows, shape identical to main, exactly 1 cell changed (the `recipe.256` note cell), CRLF count 979 unchanged; `docs/feature-matrix.csv` regenerated, regen diff empty after the commit.

- **A knowledge fragment is now craftable into a note, and the book learns which row that is** (`TC-M3-04-s-iv` + `TC-M3-04-s-iv-b`, cards `t_175d2196` / `t_9eefdde6`, heads `05f6495` on `wt/knowfrag-recipe` and `96b247a` on `wt/thaumonomicon-recipe-page`, landed by `t_35921e6e`): `thaumcraft:itemresearchnotes` meta 42 - the blank discovery note that rolls the day's hidden entry - had no survival path, so the note -> book -> research loop depended on creative loot. The original's single `itemResearchNotes` row (`ConfigRecipes.java:3860-3865`, `ConfigResearch.recipes.put("KnowFrag", ...)`) ships as the data row `thaumcraft:recipe.256.crafting_shaped`: `KKK/KKK/KKK` of `thaumcraft:itemresource` pinned to `thaumcraft:item_meta` 9 (the `neoforge:components` ingredient, `type` key mandatory - a bare id would let amber, nitor or pure craft notes) into one `itemresearchnotes` carrying meta 42 with no `thaumcraft:research_note` component, `neoforge:item_exists`-gated on both items, never research-gated because the original's KNOWFRAG entry is `setAutoUnlock`. Pinned by 5 unit (`KnowFragRecipeDataTest`, a field-for-field read of the decompiled registration) + 3 game probes (`KnowledgeFragmentRecipeGameTest`: every shipped `.crafting_*` row live in the `RecipeManager`, the nine-fragment grid assembling exactly `ItemResearchNotes.blankDiscoveryNote(42)`, and a bare / meta-1 / meta-6 / meta-13 / eight-fragment / nine-in-one-stack grid all refusing). `96b247a` then lands the join that puts that row on the entry's page - the original kept the row twice (the crafting table plus `ConfigResearch.recipes["KnowFrag"]`) and a vanilla `minecraft:crafting_*` row has nowhere to carry a research key: `research/book/ResearchRecipeLinks` compiles the link from the content-mapping ledger's `research=KNOWFRAG` column, `BookPageView.recipeId()` carries the row **id** (the server-safe page state never touches `net.minecraft.world.item.crafting`), and `recipes/RecipePageDisplay` resolves it against the **live `RecipeManager`** into the original's picture with the geometry measured off `GuiResearchRecipe#drawCraftingPage:755-946` (`recipes/RecipePageLayout`), under the original's `recipe.type.workbench` / `recipe.type.workbenchshapeless` labels; 9 unit (`ThaumonomiconRecipePageTest`) + 3 game probes (`ThaumonomiconRecipePageGameTest`) pin the join against its three sources and prove 44 of the graph's 45 `normal_crafting` pages carry no body. **THE PAGE-BODY DRAW DID NOT LAND, and the reason is a measurement:** this head painted the grid in `ThaumonomiconScreen#renderCraftingBody` of the research lane's node-browser screen, which the BOOK lane deleted when it merged `ThaumonomiconScreen` into the unified book screen (tab strip + entry listing + TEXT detail panel - no page body to host a 3x3 item grid). Resurrecting a deleted screen is not a landing, so at that landing the row shipped craftable, joined and resolved live while its Thaumonomicon picture was not drawn, and **BD-64** (this head's BD-19, per `docs/bd-id-map.csv`) stayed open for the drawing on top of the geometry and live resolution that did land - the DRAW landed next, at `t_79eea20e`; see the bullet above it. Merged hunk-wise with `main` incumbent: 10 conflicted files (6 docs + `client/ThaumonomiconScreen.java`, `gametest/LocalizationGameTest.java`, `lang/en_us.json`, `localization/LocalizationFileTest.java`); the screen keeps `main`'s book screen, the localization probes keep `main`'s audit shape and gain the two station-label keys (457 -> 459 parsed keys). Measured on the merged tree: Measured on the merged tree (tip `2fefa9b`): 881 unit tests / 0 failures / 2 skipped (`KnowFragRecipeDataTest` 5, `ThaumonomiconRecipePageTest` 9, `InkwellRecipeDataTest` 6, `LocalizationFileTest` 9, the BD ledger contract 7); 328 required game tests pass on `runGameTestServer` and on `-Ptc.noCurios runGameTestServer`; the BD ledger is 65 -> 66 rows, ids contiguous BD-1..BD-66, and `docs/bd-id-map.csv` gains the `wt/inkwell-recipe` BD-20 -> BD-66 row (the KnowFrag / recipe-page BD-19 -> BD-64 allocation was already in that file, so nothing here was renumbered by hand)
- **The Scribing Tools become survival-craftable** (`TC-M3-04-s-vi`, card `t_28fe696f`, head `d0ed716` on `wt/inkwell-recipe`, landed by `t_35921e6e`): `thaumcraft:iteminkwell` was registered, modelled and consumed by three infusion rows but produced by nothing, so the book's note arm - gated on Scribing Tools + paper by the landed `a3d98bc` - was creative-only. The original's `shapelessOreDictRecipe` family (the ore-dict rows the vanilla-crafting extractor skips, `ConfigRecipes.java:3971-3982`) now ships as data: `recipe.scribe1.crafting_shapeless` (empty phial pinned to `thaumcraft:item_meta` 0 + feather + `thaumcraft:oredict.dyeblack`) and `recipe.scribe2.crafting_shapeless` (glass bottle + feather + the same tag - every ingredient vanilla, so this is the unconditional path); Scribe3's 16-colour recolor row waits on the inkwell meta family. `dyeBlack` was 1.7.10 dye meta 0 = ink sac, so the new `thaumcraft:oredict.dyeblack` tag carries `black_dye` + `ink_sac`. Pinned by 6 unit (`InkwellRecipeDataTest`, a field-for-field read of the decompiled Scribe1/2/3 rows) + 4 game probes (`InkwellRecipeGameTest`: both crafts assemble a fresh full-durability inkwell, and filled-phial / un-component-phial / blue-dye / feather-less grids refuse). Merged hunk-wise with `main` incumbent: 6 conflicted files, all docs, 0 of main's rows lost, 0 reverting transitions, one BD row appended as **BD-66** (was BD-20 on the scribing lane). Measured on the merged tree: Measured on the merged tree (tip `2fefa9b`): 881 unit tests / 0 failures / 2 skipped with `InkwellRecipeDataTest` 6/6; 328 required game tests pass on `runGameTestServer` and on `-Ptc.noCurios runGameTestServer`, including the live end-to-end proof this card asks for: `InkwellRecipeGameTest` asserts both rows sit in the `RecipeManager`, that the empty-phial grid assembles a fresh `thaumcraft:iteminkwell` (max damage 100, damage 0, max stack 1, every slot consumed) and that the glass-bottle grid does too, while an `itemresource` meta 9 / feather / feather grid refuses; and `ThaumonomiconNoteRequestGameTest` — landed from `a3d98bc` — passes `ResearchManager.hasScribingInk` on exactly that stack (the probe's crafted stack is `isSameItemSameComponents` to it) plus a paper, and hands the note over. The note -> book -> research chain is therefore survival-reachable: feather + `thaumcraft:oredict.dyeblack` + a bottle, and paper from three canes
- **The Outer Lands land as data, and the game-test leg cannot see them** (`TC-M6-04f-m`, card `t_d62e09c5`, head `50c6b6d` on `wt/eldritch-m`): 14 classes + 3 datapack JSONs — the `thaumcraft:dimension.-42` plane (`dimension_type/outerlands.json` carries the `WorldProviderOuter` flags verbatim: void flat over an empty layer list, the Eldritch biome, ambient 0.15 void fog, frozen noon at time 0, height 128, beds forbidden, no skylight, no natural spawn), the eldritch portal block and tile (the 5-tick player scan with the mount/passenger gate, the original's `bW = 100` cooldown and its in-window refresh rule, `TeleporterThaumcraft.findNearestPortal` targeting, `changeDimension` travel, the `ENTEROUTER` completion seam), and the maze build sweep on the `thaumcraft:labyrinth` `SavedData` (2111 → 38962 placements, 1580 blocks/layer with the 2185-block cross-check, 111 rooms, 4971 floor cells, a 120-tick build budget measured at 9 ticks). Merged hunk-wise against `main` with `main` as incumbent: **8 conflicted files, every one resolved as a merge of both sides, 0 of main's doc rows lost**. Measured on the merged tree: unit **861 / 0 failures / 35 skips**, **All 317 required tests passed** in both the Curios and `-Ptc.noCurios` modes, `en_us.json` **454 → 457** keys, `content-mapping.csv` **427 → 433** `done` (the 6 named rows flipped, the closed kind whitelist untouched, 1048 rows / 1034 unique ids), `feature-matrix.csv` regen diff empty, `research_graph` unchanged at **201 entries / 6 categories**. One red was found on this merge and fixed, not inherited: `everyregisterednamehasanentry` — the slice's new `biome.thaumcraft.` loop makes every registered biome demand a lang entry, and the landed taint biome had none, so `biome.thaumcraft.biome.biomegentaint = "Tainted Land"` (the original's `func_76735_a` at `BiomeGenTaint.java:31`) now ships. **BD-65** records why the dimension half is not gametest-provable (`GameTestServer.create` bakes its world over `new MappedRegistry<>(Registries.LEVEL_STEM).freeze()` + the flat preset, so `runGameTestServer` loads no datapack, mod or vanilla): it rides `OuterLandsParityTest`, which decodes the three shipped JSONs through the real codecs over the live registries, plus the open stage-F in-instance run. Maze features 6..14 (key/nest/library rooms) and the boss entities stay on `t_15530dc0` / TC-M6-06.
- **The BD ledger has one id space again** (`t_9a2c9f91`, D-021): `docs/BEHAVIORAL_DEVIATIONS.md`
  carried one row count on `main` and a different one on every live head — measured **14-44 rows
  across 118 heads** against `main`'s 45, median 18, 111 heads at least 9 rows short, none at
  `main`'s count, and three different rows claiming `BD-19` on three sides of three landings.
  `main`'s 45 rows stay the incumbent, ids and text verbatim, and **19 deviations arrive as
  BD-46 … BD-64** with their source head, sha, old id and landing card named in each row's last
  cell; **17 head rows are rejected** as deviations `main` already owns (`Container screen
  backgrounds and client-visible machine state` → **BD-37**, `Wisp block collision` → **BD-19**).
  `docs/bd-id-map.csv` (generated) is the old→new map every queued landing cites;
  `scripts/bd_ledger.py` runs the rule (`audit` / `emit` / `check` / `revert`); and
  `BehavioralDeviationLedgerContractTest` (7 unit probes) freezes the incumbent rows and the id
  contract — the count never decreases, both counting methods agree, no `BD-NN` citation dangles,
  no merge takes one side of the file wholesale. `BD-13`'s parenthetical moved out of its id cell,
  which is the row the `^| BD-` count saw and a strict parse did not. Measured on the reconciled
  tree: unit **733 / 0 failures / 2 skips**, **All 292 required tests passed** in both the Curios
  and `-Ptc.noCurios` modes, and **`total reverting rows across 121 heads: 0`** (57 of them
  conflicted on this very file).
- **Angry / Furious zombie pair** (`TC-M6-06-s-c2`): `thaumcraft:brainyzombie` and
  `thaumcraft:giantbrainyzombie` land as 1.21.1 `Zombie`s with the originals' figures cited line by
  line — 25.0 health / 5.0 attack with the reinforcement attribute pinned at 0.0 and armour +3 capped at
  20 for the angry zombie, and for the fury the anger ledger (start 1.0, +0.1 per hit to the 2.0 cap
  applied before `super`, 0.002 per tick above 1.0, attack rewritten to `7.0 + (anger - 1) * 5.0` every
  tick, hitbox `(1.2 + anger)` on its own 0.6 x 1.8 base), xp 15, 60.0 / 7.0, the leap goal at slot 2,
  the doubled death drops (2 loops of 6 flips of 2) and the 4-way thaumium / carrot / potato / amber
  `dropAllLoot` replacement. The pair installs the 1.7.10 parent's goal shape and strips the
  post-1.7.10 goals (village patrol, turtle egg, turtle target, door breaking). Gate: 431 unit tests /
  0 failures, 138/138 game tests with and without Curios; BD-28 records the scale-attribute resize and
  BD-29 the brain drop gated on the unregistered zombie-brain item.


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

**TC-M9-01-s turns “the mod is playable” into a gate that fails: `PlayableAcceptanceGameTest` composes the
intake's three asks into one verdict.** All 8 `blockcustomore` variants are placed and broken through the
shipped dispatch only — `level.destroyBlock` with no tool and the `Block.playerDestroy` tool lane with real
enchanted pickaxes — at the original drop formulas (variant 0 self-drop with the variant preserved, variants
1–6 one size-1 `itemshard` stack per unit at `item_meta` = variant − 1 in `1 + rand(2 + fortune)`, variant 7
`itemresource` meta 6 Amber in `1 + rand(fortune + 1)`, the `match_tool` silk-touch branch returning the block
with its variant). The creative tab's contents are compared against a set **derived from the live item
registry at test time** — never a hardcoded list, which is the trap this project keeps hitting — with the
converse asserted too (the `test_probe` / `test_aspect_source` fixtures appear in no public tab), and every
production item must resolve a `Component.translatable` name to text that is not its own key plus a model
JSON on the classpath. `theplayableintakecomposesintooneverdict` runs all of it in one fresh world and reports
**one** verdict: 135/135 required tests, green in both the Curios and `-Ptc.noCurios` configurations and
`./gradlew build` exit 0 (379 unit tests / 0 failures / 1 skipped). The gate is **mutation-proven red
twice** — hide `data/thaumcraft/loot_table/blocks/blockcustomore.json` → exit 1, “8 required tests failed”
with the ore legs red; empty the tab builder's `displayItems` → exit 1, “4 required tests failed” with the tab
legs red — and both files restore byte-identical (`diff -q` silent). The three per-card probes were green
while the intake was unmet; this is the standing gate that closes that gap. JEI needs no code for item
listing, so this is the machine-checkable form of “items are in JEI”; custom recipe categories (SPINE-4) stay
deferred.


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
