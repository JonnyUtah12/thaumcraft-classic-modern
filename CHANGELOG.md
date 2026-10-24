# Changelog

All notable changes to Thaumcraft Classic (Unofficial) are documented here.
Format: Keep a Changelog; versioning: Semantic Versioning (see
`docs/release-process.md`). Project versions are independent of the pinned
Minecraft 1.21.1 / NeoForge 21.1.259 targets.

## [Unreleased]

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
