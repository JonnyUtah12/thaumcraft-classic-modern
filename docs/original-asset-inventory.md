# Original Asset Inventory — Thaumcraft 4.2.3.5 (1.7.10)

Source of truth: `../reference/assets/thaumcraft/` — the asset tree extracted
from `Thaumcraft-1.7.10-4.2.3.5.jar` (SHA-256 verified in
docs/original-baseline.md). Per D-004 these files are All Rights Reserved,
live outside the repository, and are overlaid into the local build; this
document and `docs/asset-mapping.csv` are the committed mapping surface.

Verified totals: **1079 files** under `assets/thaumcraft/` in the JAR.
Every one appears exactly once in `docs/asset-mapping.csv`: **1037 mapped,
42 explicitly marked unused** (list at the end).

## Method

The mapping was derived mechanically from the decompiled sources
(`../reference/decompiled/thaumcraft/`, 859 .java, verified complete by
TC-M0-02) by indexing every asset-path string literal and icon registration
in the code, then resolving it to a file:

| Reference pattern in 1.7.10 code | Resolves to |
| --- | --- |
| `IIconRegister.func_94245_a("thaumcraft:<name>")` from a Block class | `textures/blocks/<name>.png` (Forge block texture map) |
| same call from an Item class | `textures/items/<name>.png` (item texture map) |
| `UtilsFX.bindTexture("textures/...")`, `new ResourceLocation("thaumcraft", "textures/...")`, armor `getArmorTexture` strings | exact path |
| concatenated names (`"thaumcraft:furnace" + a`, `"textures/models/wand_rod_" + tag`, `"textures/aspects/" + tag`) | prefix match against the file tree |
| `sounds.json` event keys + `playSound("thaumcraft:<event>")` call sites | `sounds/<file>.ogg` |
| OBJ `mtllib` | `.mtl` (see model section — not loaded at runtime) |

`used_by` in the CSV lists the decompiled classes that reference the asset,
tagged by kind: `(block) (item) (block-entity) (entity-render) (entity)
(gui) (effect) (model) (wand-component) (aspect-system) (registration)
(proxy) (render-helper) (code)`. The class name identifies the content
(e.g. `BlockCosmeticSolid` = decorative solid blocks, `TileAlembicRenderer`
= alembic, `RenderCultist` = Cultist Kaelonas, `GuiSpa` = Crucible Es /
spa GUI, `FXSonic` = sonic effect). Where a name is built dynamically the
note column records the construction site.

## Inventory by category

| Directory | Files | Mapped | Unused | What it is |
| --- | --- | --- | --- | --- |
| `textures/blocks/` | 262 | 256 | 6 | block icons (incl. 24 `furnace0-26` arcane-furnace faces, 47 `warded_glass_1-47`, `taint_over_*`, `greatwood/silverwood` log sides, `crate_*/urn_*` loot blocks) |
| `textures/items/` | 291 | 278 | 13 | item icons (wand rods/caps, focus icons `focus_*`, golem task cores, baubles, curiosities) |
| `textures/models/` | 216 | 208 | 8 | entity/armor skins (crab, cultist, golem bodies, robes), OBJ models (16), textures bound by TESRs |
| `textures/misc/` | 77 | 69 | 8 | HUD (`hud.png`), research icons (`r_*.png`), particles (`particles*.png`, `wisp*`), portals, parchment/quill |
| `textures/aspects/` | 50 | 50 | 0 | 48 aspect tag icons (built in `Aspect.java` from the tag name) + `_back.png`, `_unknown.png` |
| `textures/gui/` | 23 | 22 | 1 | GUI containers: research book/table, thaumatorium, wand/focal manipulator, golem, spa, focus pouch, hover harness, pech trader, trunks, HUD hexes |
| `textures/foci/` | 21 | 21 | 0 | focus effect icons referenced by `ItemFocus*` classes and `ConfigResearch` |
| `sounds/` | 111 | 110 | 1 | 66 sound events in `sounds.json` (111 .ogg; multi-file events like `bubble1-4`) |
| `lang/` | 24 | 22 | 2 | 22 `.lang` locales (en_US master + 21 translations); README/contributing are not game assets |
| `shader/` | 3 | 0 | 3 | `test*.frag/.vert` — never referenced; TC's post shaders live under `assets/minecraft` (`shaders/post/*.json`) |
| `sounds.json` | 1 | 1 | 0 | sound event registry |
| **Total** | **1079** | **1037** | **42** | |

Animated textures: 101 `.png.mcmeta` files; 98 pair with a used texture
(format unchanged in 1.21.1), 3 are orphaned/paired-with-unused (see unused
list).

## Localization

`lang/en_US.lang` is the master key list; 21 other locales exist
(ca_ES, cs_CZ, de_DE, es_AR/ES/MX/UY/VE, et_EE, fi_FI, fr_FR, it_IT,
ja_JP, ko_KR, nl_NL, pt_BR/PT, ru_RU, sv_SE, zh_CN, zh_TW). 1.21.1 does
not read `.lang`; the build overlay must convert each file to
`translations/<locale>.json` (mechanical key=value conversion). Keys are
consumed by the code via `StatCollector`/`I18n`; the key set itself is
tracked by the content inventory (TC-M0-04), not per asset row.

## Sounds

`sounds.json` defines 66 events over 111 files. The CSV records, per
`.ogg`, the event name and the classes that call `playSound` for it
(entities, tile machines, wand casting, nodes/taint, ambience). Conversion
notes: 1.21.1 drops the per-event `category` field (category is chosen at
the play site) and adds optional `subtitles`; event keys stay identical
under the `thaumcraft` namespace. One event (`urnbreak`) is defined but
never played — `BlockLoot`'s step sound name resolves to the vanilla
`dig.*` namespace, so the ogg is marked unused. TC-M7-05 extends the CSV
with the 1.21.1 trigger sites.

## MODEL-CONVERSION — 1.7.10 → 1.21.1 model format, per model

Thaumcraft 4.2.3.5 predates the JSON block-model system: the JAR contains
**no blockstate/model JSON files**. Its "models" are (a) 16 Wavefront OBJ
files loaded at runtime through Forge 1.7's `WavefrontObject` and rendered
by special renderers that bind textures and render OBJ groups by name, and
(b) code-built entity/armor models (`ModelBase` subclasses in the
decompiled tree — code, tracked by the content tasks, not assets). Every
OBJ is recorded here; none may be silently dropped.

| OBJ | 1.7 loader/renderer | OBJ groups | textures bound by the renderer | 1.21.1 conversion |
| --- | --- | --- | --- | --- |
| `adv_alch_furnace.obj` | `TileAlchemyFurnaceAdvancedRenderer` | Base, Tank | `alch_furnace(_on).png`, `alch_furnace_tank(_on).png` | keep OBJ; BlockEntityRenderer (animated tank) |
| `alembic.obj` | `TileAlembicRenderer` | Pot, Legs, TubeMain, TubeSmall, Panel | `alembic.png`, `label.png`, `Bore.png` | keep OBJ; BER |
| `bucket.obj` | `RenderGolemBase` (golem tool) | Bucket, Bucket_Liquid | `bucket.png` + golem skins | keep OBJ; entity renderer |
| `crabvent.obj` | `TileEldritchCrabSpawnerRenderer` | Vent | `crabvent.png` | keep OBJ; BER |
| `crystalizer.obj` | `TileEssentiaCrystalizerRenderer` | top, base | `crystalizer.png`, `vis_relay.png` | keep OBJ; BER |
| `hemis.obj` | `FXBlockWard`, `FXShieldRunes`, `FXSonic` | GeoSphere001 | `hemis0-15.png`, `ripple1-15.png` (animated frames) | keep OBJ; effect renderer |
| `hoverharness.obj` | `ModelHoverHarness` | Cylinder001 | `hoverharness2.png` | keep OBJ; item/armor renderer |
| `node_stabilizer.obj` | `TileNodeStabilizerRenderer`, `TileNodeConverterRenderer` | lock, piston | `node_stabilizer(_over).png`, `node_converter(_over).png`, `node_bubble.png` | keep OBJ; BER |
| `obelisk_cap.obj` | `TileEldritchCapRenderer`, `TileEldritchObeliskRenderer`, `TileFluxScrubberRenderer` | Cap, Tip | `obelisk_cap(_2).png`, `obelisk_side(_2).png`, `fluxscrubber.png`, `particlefield*.png`, `tunnel.png` | keep OBJ; BER |
| `orb.obj` | `RenderFrostShard` | orb | `blocks/frostshard.png` | keep OBJ; entity renderer |
| `pillar.obj` | `TileInfusionPillarRenderer` | Box001 | `pillar.png` | keep OBJ; BER |
| `reservoir.obj` | `TileEssentiaReservoirRenderer` | Cylinder001 | `reservoir.png` | keep OBJ; BER |
| `scanner.obj` | `ItemThaumometerRenderer` | scanner | `scanner.png`, `scanscreen.png` | keep OBJ; item renderer |
| `thaumatorium.obj` | `TileThaumatoriumRenderer` | Thaumatorium | `thaumatorium.png` | keep OBJ; BER |
| `vcrystal.obj` | `TileEldritchCrystalRenderer` | Crystal, Base | `vcrystal.png`, `blocks/crust.png` | keep OBJ; BER |
| `vis_relay.obj` | `TileVisRelayRenderer`, `TileMagicWorkbenchChargerRenderer`, `TileEssentiaCrystalizerRenderer` | Crystal, RingFloat, RingBase, Support | `vis_relay.png`, `crystalizer.png` | keep OBJ; BER |

Decision: keep the OBJ files as the model source (NeoForge 21.1 still
ships an OBJ loader; group names are preserved by it) and re-implement the
special renderers as `BlockEntityRenderer`/entity renderers. Rationale:
these models are animated and multi-textured per group; a static JSON
block model cannot express them, and converting to JSON would be a silent
visual change. All 16 are carried; none dropped.

`.mtl` files (3): `alembic.mtl`/`thaumatorium.mtl` are referenced by their
OBJ via `mtllib` but Forge 1.7's loader never reads them (materials are
export leftovers; colors in them do not match in-game appearance);
`alchemyblock.mtl` is referenced by nothing; `hemis.obj` references a
missing `hemis.mtl`. All are kept only as model-conversion source, not
runtime assets.

## Unused assets (42) — explicit, with reason

| File | Reason |
| --- | --- |
| `lang/README.md`, `lang/contributing.md` | translation-process notes shipped in the jar; not game assets |
| `shader/test.frag`, `shader/test2.frag`, `shader/test2.vert` | never referenced; TC post shaders live under `assets/minecraft` |
| `sounds/urnbreak.ogg` | event defined in sounds.json but never played; step-sound name resolves to vanilla `dig.*` |
| `textures/blocks/crystalframe.png` | leftover; `BlockCrystal` registers icon `crystal` only |
| `textures/blocks/es_i_1 - Copy.png` | duplicate of `es_i_1.png` (" - Copy" filename) |
| `textures/blocks/flower.png` | leftover from an older Thaumcraft; no flower block in 4.2.3.5 |
| `textures/blocks/redstone_ring.png` | no code reference |
| `textures/blocks/wardedglassrune.png`, `textures/blocks/wardedstone.png` | leftovers; warded look = `BlockWarded` blank + `runeborder` |
| `textures/gui/arcane.png` | leftover; `GuiArcaneWorkbench` uses `gui_arcaneworkbench.png` |
| `textures/items/focus.png`, `focus_reversal.png`, `focus_whatever_orn.png`, `focuspouchbauble.png` | features not present in 4.2.3.5; real focus icons are `focus_<type>` |
| `textures/items/golem_core_bodyguard.png(+.mcmeta)`, `golem_core_patrol.png(+.mcmeta)` | superseded; `ItemGolemCore` registers `golem_core_guard`, no `patrol` task |
| `textures/items/nuggetbeef/chicken/fish/pork.png` | no nugget items in 4.2.3.5 |
| `textures/items/phialoverlay.png` | no code reference; phial = `phial.png` + aspect icon |
| `textures/misc/eldritchajor1/2.png` | no code reference; research icons use `misc/r_eldritchmajor.png` |
| `textures/misc/quill.png` | no code reference; table quill uses `blocks/tablequill.png` |
| `textures/misc/research1-5.png` | no code reference |
| `textures/models/Thaumaturge_eyes.png` | no code reference |
| `textures/models/alchemyblock.mtl` | export leftover; no OBJ references it |
| `textures/models/eldritch_taintacle.png` | unused variant; `RenderTaintacle` uses `taintacle.png` |
| `textures/models/frostshard.png` | unused duplicate; `RenderFrostShard` uses `blocks/frostshard.png` |
| `textures/models/jar_void.png` | no code reference; void jar uses `blocks/jar_side_void/jar_top_void` |
| `textures/models/ripple.png.mcmeta` | orphaned: base `ripple.png` not in the jar |
| `textures/models/runic_goggles.png`, `runicbootstraveler.png` | no code reference; those items return the non-runic skins |

## Cross-checks and downstream contracts

- 1079 files on disk = 1079 CSV rows; each path appears exactly once
  (verified programmatically after generation).
- `new_asset` is the same path (D-004 overlay keeps paths identical, the
  `thaumcraft` namespace is already lowercase); exceptions: `lang/*.lang`
  → generated `translations/<locale>.json`, unused assets → empty.
- TC-M0-04 (content inventory, not yet started): every content entry must
  list its required assets by these exact paths; `used_by` in the CSV is
  the join key (class → content id mapping happens there).
- TC-M7-01 (model/texture integration) consumes the CSV rows + the
  MODEL-CONVERSION table above; TC-M7-05 (sounds) extends the CSV with
  1.21.1 trigger sites.
