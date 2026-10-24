# TC-M5-05b — surface structures (totems, shrine mounds, eldritch rings, hilltop stones)

Design record for the structure pass salvaged from the shared-tree WIP.
Original sources: `com.jonnyutah12.thaumcraft.worldgen.WorldGenMound.java`,
`WorldGenEldritch.java`, `WorldGenHilltop.java`, `WorldGenTotem.java`,
`TileEntityTotem.java`, and the `ThaumcraftWorldGenerator.generateSurface`
structure block / `generateTotem` (ThaumcraftWorldGenerator.java:445-487,
112-183).

## Why this rides MagicChunkHooks instead of data-driven worldgen features

The TC-M5-05 ore machinery (data-driven `thaumcraft:tc_ore` feature + biome
modifier) was not reusable here. The original's four structures are one
per-chunk sequence with shared state that placed-feature rarity filters cannot
express without changing the frequencies this card must pin:

1. a chained else-if roll — 1/150 shrine mound, else 1/66 eldritch ring, else
   1/40 hilltop stones. Mutually exclusive and conditional: a mound attempt that
   fails (rock plateau) still consumes the roll and never falls through to the
   ring branch.
2. the totem's `!auraGen` suppression — a chunk that already produced an aura
   node or structure node gets no totem.

Structures also call `NodeGeneration` (they can create aura nodes), which needs
a `ServerLevel` and writes `WorldMagicData` — impossible from a
`worldgen.FeatureInstance` callback. docs/architecture/worldgen.md (TC-M5-01)
already maps the original's per-chunk entry points onto
`MagicChunkHooks`/`WorldMagicData`; this pass is exactly that deferred slot.

Hook order is pinned in code, not by class-load luck: `AuraNodeChunkGen`'s
static initializer registers the node pass and then touches
`StructureChunkGen.ensureHookRegistered()`, whose own static initializer
registers the structure hook second. The totem gate here reads the aura nodes
the earlier pass wrote for the same chunk. (The WIP had the touch inverted; it
also carried an `@EventBusSubscriber` annotation on `StructureChunkGen`, which
fml rejects for a class with no `@SubscribeEvent` method; the tick driver
already lives in `AuraNodeChunkGen`, so there is no behavioural deviation here.)

## Fidelity decisions

* **Gate order is verbatim** from `generateSurface`: overworld only (`field_76574_g
  == 0`; the nether totem call site is dead code, its dimension gate is
  literally `== 0 || == 1`, verified in the original bytecode), dimension
  blacklist (`== -1`), `World_Generation.generate_structures`
  (`Config.genStructure`), the generator-name `startsWith("flat")` guard (see
  BD-15), `newGen` (always true in the modern once-per-chunk pipeline — worldgen.md
  note 7), then the roll chain.
* **Chained else-if roll**: one draw per chunk decides the branch; a failing
  mound does not fall through. `StructureParityTest.rollsMatchOriginalDrawSequence`
  replays the original's raw draw sequence for a seed and asserts the same branch.
* **Per-structure constants** (from the original): mound anchor
  `y = clamp(60 + rand(0..35), 0..120)`, plateau gate ≤ 5 rock cells within
  radius 2, mound radius 4 and ±2 skirt; eldritch ring center
  `y = clamp(50 + rand(20..50), 0..120)`, radius 8..11, ±3 jitter, height
  3..5, clearing radius 3, obsidian replacement chance 1/4; hilltop stone
  count 2..6, `y = clamp(70 + rand(-10..10), 0..120)`, ±5 jitter, totem
  chance 1/6 (16.7%). All pinned by the parity tests.
* **Mound block table**: `MoundStructure`'s 15-cell table is a mechanical
  transcription of the original's `setBlock` call sequence
  (scripts/extract_mound_structure.py, 1441 calls); the test re-parses the
  original source and replays it, last write per cell wins — including
  `tile_stonecosmetic` (101 of 1441 calls), the only TC content name in the
  mound; the port keeps that name as its own content entry (BD-17).
* **Totem**: `TileEntityTotem` semantics are carried in full — `canBeReplaced`
  ground test plus the 8-cell neighbour filter, `hasTotemAt` suppression,
  `findAnchorChunk`/`findAnchorBlock` chunk-anchor normalisation (the
  original's chunk-relative coordinates; anchors resolve via the 3×3 heightmap
  scan; the block search is ±8 cells, negative first, from
  `getHeightAtFaceEdge`), the 3×3 footprint, shaft height `4 + rand(0..2)`,
  stone-cap chance 3/4, and wisp spawn chance 1/10 per shaft cell. The
  original's hardcoded `"thaumcraft:wisp"` entity string is emitted as the
  literal id, with a comment that TC-M6-06 registers the entity (deviation
  list).
* **Structure nodes**: structure-generated aura nodes are written into
  `WorldMagicData` (same storage as TC-M5-02's node runtime), so
  `hasTotemAt`/`findNodeAt` suppress further structures deterministically
  rather than by re-scanning blocks.

## Deviations introduced here

Recorded in docs/BEHAVIORAL_DEVIATIONS.md (BD-15…BD-20) with cause and impact.

## Test evidence

`src/test/java/com/jonnyutah12/thaumcraftclassic/worldgen/StructureParityTest.java`
(4 tests: mound-table equality against a replay of the original source, and
branch-selection/draw-sequence parity for the chained roll) and
`MazeGeneratorTest.java` (1 test; added with the Outer Lands maze, TC-M6-04f).
The first test is skipped by `Assumptions.assumeTrue` when
`reference/decompiled/` is not checked out — the fixture is a porting artifact,
not shipped content.
