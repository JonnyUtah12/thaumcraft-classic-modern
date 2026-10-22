# Aura & vis node worldgen (TC-M5-01)

Node types/modifiers as real registries, the biome aura table, the wild-node
roll, and the per-chunk aura records — the world-side half of the vis system
that TC-M4-02 (vis pool/visnet/ambient reader) and TC-M5-02 (node behaviour)
build on.

## Source of truth

- `reference/decompiled/thaumcraft/thaumcraft/common/lib/world/ThaumcraftWorldGenerator.java`
  - `createRandomNodeAt` (L296-425): type/modifier roll tables, taint
    multipliers, value budget, aspect assembly, environment scan, spread merge.
  - `createNodeAt` (L405-418): blockairy placement + TileNode fields.
  - `generateWildNodes` (L185-215): rarity gate, surface scan, vertical jitter.
  - `generateSurface` (L420-470) / `generateNether` (L577-587): dimension and
    config gates.
- `reference/.../common/lib/utils/Utils.java:94-101` — `getFirstUncoveredY`.
- `reference/.../common/config/Config.java:837-867` — `registerBiomes()` aura
  table (31 rows) + TC biome dictionary registrations.
- `reference/.../common/lib/world/biomes/BiomeHandler.java` — `getBiomeAura`
  (average over matched types, fallback 100), `getRandomBiomeTag` (uniform
  pick), `getBiomeSupportsGreatwood`.
- `reference/.../api/nodes/NodeType.java` / `NodeModifier.java` — the 6+3
  constants pinned by docs/content-mapping.csv rows `Thaumcraft:nodetype.*` /
  `Thaumcraft:nodemodifier.*`.
- Balance values: docs/balance-values.md §6.1 (biome aura table), §6.2 (node
  type/modifier behaviour, consumed by TC-M5-02).

## Target-platform design

| Original | Port |
|---|---|
| `api/nodes/NodeType` / `NodeModifier` enums | `nodes/NodeType`, `nodes/NodeModifier` — same constants; short store ids (`"dark"`) for the world store, registry paths (`nodetype.dark`) for the object registries |
| (no registry — TC4 enums were not registered) | `thaumcraft:node_type` / `thaumcraft:node_modifier` custom registries created in `NewRegistryEvent` and filled via `DeferredRegister` (`registration/ModNodeDefs`), same pattern as `ModAspects`; the CSV ids exist as live registry entries (asserted by `AuraNodeGameTest.nodeRegistriesContainTheFrozenIds`) |
| `Config.registerBiomes()` BiomeDictionary table | `worldgen/BiomeAura.TABLE` — 31 rows keyed by the `c:` convention biome tags (the successor of the Forge biome dictionary: `is_forest` ≡ FOREST etc.); aura levels, dominant aspect tags and greatwood flags transcribed row-for-row |
| `BiomeHandler.getBiomeAura/getRandomBiomeTag` | `BiomeAura.getBiomeAura/randomBiomeTag` + pure `averageAura/pickRandomTag` halves for unit tests |
| `ThaumcraftWorldGenerator.createRandomNodeAt` | `worldgen/NodeGeneration.planNode` (pure, seeded) + `createRandomNodeAt` (world side); randomness consumed in the original's exact order |
| `generateWildNodes` | `NodeGeneration.generateWildNodes` — same rarity gate, surface scan (`firstUncoveredY` from y=5), +0..3 jitter with replaceability check, height cap |
| `createNodeAt` (blockairy + TileNode) | `NodeGeneration.createNodeAt` stores a `WorldMagicData.Node` record (type + modifier + intensity + aspects); blockairy/TileNode materialisation is TC-M5-02 |
| `IWorldGenerator` + `EventHandlerWorld` chunk queue | `worldgen/AuraNodeChunkGen` registered as a `MagicChunkHooks.MagicChunkGenHook` (TC-M2-02 pipeline), driven by `ServerTickEvent.Post` — the tick driver M2-02 deferred to M5 |
| `world.getWorldInfo().getWorldType()...startsWith("flat")` | not needed: the original's flat check gates trees/structures, never aura nodes |
| `Utils.getSurroundingVis` / loaded TileNode scans | `nodes/NodeLookup` (find-at / in-chunk / near) — the query hook TC-M7 visuals and TC-M5-02 consume |

Per-chunk aura records: `AuraNodeChunkGen.recordAura` writes the biome's table
aura as `WorldMagicData.setAura(density, purity=100)` for every generated
chunk. TC4 had no per-chunk ambient vis (vis lived only in nodes), so the
aspect map stays empty at generation; TC-M5-03 (flux) writes ambient aspects,
and purity is the pristine baseline those systems lower.

## Deliberate adaptations (parity notes)

1. **BiomeDictionary → `c:` biome tags.** Every dictionary type the original
   registered has a 1:1 convention-tag successor; the mapping is pinned in
   `BiomeAura.TABLE` and guarded by `BiomeAuraTableParityTest`. Unregistered
   tags are ignored rather than throwing (the original's exception fallback);
   for vanilla biomes — all of whose types the original registered — the two
   behaviours agree exactly.
2. **Seeded per-chunk RNG.** The original used the unsalted `world.rand`, so
   node placement depended on chunk load order. The port seeds
   `worldSeed ^ chunkPos.asLong()`, making generation deterministic and testable.
3. **Degenerate-config guards.** `nextInt` bounds are floored at 1 where the
   original would throw (`node_rarity=0`, aura < 2). Unreachable with real
   table values (min table aura is 50).
4. **Deferred hooks ride their milestones.** The scattered-feature (magic
   mushroom) node, silverwood/eerie tree nodes, mound/ring/hilltop/totem
   structure nodes and the Outer Lands maze are the tree/structure/M6
   milestones; the `createRandomNodeAt(silverwood, eerie, small)` signature and
   the `BiomeAura.supportsGreatwood` helper are kept so those cards call them
   unchanged. `NodeGeneration.registerOuterLandsDimension` /
   `addDimBlacklist` keep the original's dimension opt-out surfaces.
5. **Taint-biome identity.** The original compared against its own
   `biomeTaint` instance. The port exposes `BiomeAura.registerTaintBiome`
   (TC-M5-04/M6-04f populate it); until then the ×1.5 multiplier and TAINTED
   roll never fire — what an unmodded world without Taint biomes did.
6. **`World_Regeneration.*` options** stay config-parity-only: the modern
   pipeline generates each chunk exactly once (`magicGenDone` marker), so there
   is no "old chunk" path to re-run (docs/architecture/worldgen.md note 7).
7. **Node records, not block entities.** The store keeps type/modifier/intensity
   + aspects; the blockairy block and TileNode (production, depletion,
   regeneration rates per docs/balance-values.md §6.2) are TC-M5-02's job.
8. **Foliage stand-in.** The original's `isFoliage` matched leaves, grass/fern
   and flowers plus modded custom-foliage blocks; the port matches the vanilla
   half via block tags (`LEAVES`, `SMALL_FLOWERS`, `TALL_FLOWERS`, grass/fern
   blocks). Environment thresholds are the original's (water>100, lava>100 →
   fire+earth, stone>500, foliage>100, 11×11×11 scan).

## Testing

- `nodes/NodeTypeContractTest` — enums match the frozen CSV id rows (6+3),
  short-id round-trip, lenient lookup.
- `worldgen/BiomeAuraTableParityTest` — 31-row table row-for-row against the
  reference, average/fallback/uniform-pick rules, greatwood flags.
- `worldgen/NodeGenerationRollTest` — seeded roll invariants: value budget
  (incl. taint ×1.5/×1.5 and ÷4 paths), roll-table domains, type merges,
  biome-tag base aspect, environment thresholds (strict `>`), spread bounds,
  plus a golden seed pinning the exact RNG-consumption order.
- `gametest/AuraNodeGameTest` — live registries contain the frozen ids; a
  forced seeded wild-node roll stores a valid record (type/modifier/intensity/
  aspect tags all resolvable) and `NodeLookup` finds it; the chunk pass
  records the biome-table aura with pristine purity; the
  `generate_aura_nodes` gate suppresses and resumes generation; the
  server-tick driver drains the chunk queue end-to-end.
