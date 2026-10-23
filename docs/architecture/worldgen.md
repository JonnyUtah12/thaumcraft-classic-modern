# World generation (TC-M5-05)

Status: ore generation (cinnabar, amber, infused stone) implemented and
verified (unit + gametests green at this commit). Nodes are TC-M5-01
(t_71def828); the structure generators present in the original (golem
totems, shrine mounds, eldritch stone rings, hilltop stones) are split to
TC-M5-05b; trees, flowers, village houses and the biome-aspect variant
override remain separate rows in docs/catalog-triage.md and are not
covered here.

## Source of truth

`reference/decompiled/thaumcraft/thaumcraft/common/lib/world/ThaumcraftWorldGenerator.java`
`generateOres` (L515-565), reached from `generateSurface` (L428). The original
ran per-chunk `WorldGenMinable`/`setBlock` attempts gated by
`Config.genCinnibar` / `genAmber` / `genInfusedStone` (plus the `regen*`
re-generation variants, which have no modern equivalent — chunk regeneration
re-runs features anyway).

| Ore | attempts/chunk | Y band | vein | host | variant |
|---|---|---|---|---|---|
| cinnabar | 18 | `nextInt(height/5)` = 0..50 absolute | 1 | stone | meta 0 |
| amber | 20 | `heightValue(x,z) - nextInt(25)` = surface-24..surface | 1 | stone | meta 7 |
| infused stone | 8 | `nextInt(max(5, heightValue-5))` = 0..max(5,surface-5) | 6 | stone | meta `nextInt(6)+1`, 1/3 chance biome-aspect override |

## Target-platform design

- **Feature type** `thaumcraft:tc_ore` (`worldgen/TcOreFeature`): two
  placement paths mirroring the original's two shapes. `size == 1` (cinnabar,
  amber — the original was a single `world.setBlock` attempt guarded by
  `isReplaceableOreGen`) replaces the host block in place when it matches a
  rock predicate; the vanilla vein algorithm degenerates at size 1 and places
  nothing, so delegating would silently drop both ores. `size > 1` (infused
  stone, the original `WorldGenMinable(6)`) defers to the vanilla
  `OreFeature` ellipsoid algorithm. In both paths the ore state is picked
  once per vein/attempt (uniform over `config.states`), matching the original
  per-vein metadata roll; vanilla `minecraft:ore` picks per block, which with
  identical host predicates would always place the first state.
- **Placement modifiers** (registered in `worldgen/ModWorldgen`):
  - `thaumcraft:config_gate` — passes the chunk only while the named
    `TcConfigCatalog` option is enabled; read live per chunk like the
    original's per-attempt `Config.*` check. Unknown option names fail at
    datapack load, not silently.
  - `thaumcraft:surface_below` — Y = WORLD_SURFACE heightmap - [min,max]
    offset (amber). `Heightmap.Types.WORLD_SURFACE` is the equivalent of
    1.7.10's `World.getHeightValue`.
  - `thaumcraft:surface_capped_uniform` — Y = [min, max(floor, surface +
    offset)) (infused stone); no vanilla placement expresses a
    bottom-anchored, surface-capped band.
  - Cinnabar uses plain vanilla `count`/`in_square`/`height_range` with an
    absolute 0..50 band.
- **Data**: `data/thaumcraft/worldgen/configured_feature/{ore_cinnabar,
  ore_amber,ore_infused_stone}.json`, `worldgen/placed_feature/*` (counts
  18/20/8), wired into `#minecraft:is_overworld` at step `underground_ores`
  by `data/thaumcraft/neoforge/biome_modifier/tc_ores.json`
  (`neoforge:add_features`, additive).
- **Block**: `thaumcraft:blockcustomore` (`blocks/BlockCustomOre`) keeps the
  original single-id design; the 0-7 metadata becomes a `variant`
  IntegerProperty with the same values (D-001). Physical properties are the
  original's (hardness 5.0, resistance 1.5, pickaxe with no tier floor).
  Assets reproduce the original renderer: cinnabar/amber are single-texture
  cubes; variants 1-6 are the `infusedorestone` base plus the animated
  `infusedore` overlay inset at 0.005, tinted per variant via
  `BlockCustomOreItem.COLORS` (original `BlockCustomOreRenderer` +
  `BlockCustomOreItem.colors`).

## Deliberate adaptations (parity notes)

1. **Cinnabar band stays absolute 0..50** (original `height/5` of height 256)
   instead of scaling to 1.21.1's height 384 — scaling would move the ore
   deeper relative to sea level, changing where players find it.
2. **Deepslate added as host rock.** The original only replaced stone (no
   deepslate existed); without it, attempts below the stone/deepslate
   transition would miss and drop the effective frequency. Attempt counts and
   Y bands are unchanged.
3. **Infused-stone band starts at Y 0**, the original's absolute band, not
   the new world bottom (-64) — same reasoning as (1).
4. **Biome-aspect variant override deferred.** The original re-rolled the
   infused variant 1/3 of the time from `BiomeHandler.getRandomBiomeTag`
   (BiomeDictionary types → aspect). That depends on the biome-behavior
   system (aura/tags), a separate feature-tracking row; until it lands the
   port uses the original's fallback path (`nextInt(6)+1`), which is what
   unregistered biomes produced. Tracked as a follow-up card.
5. **Biome/dimension blacklists are integration hooks** (`addBiomeBlacklist`
   / `addDimBlacklist` are filled by network messages from other mods; empty
   in vanilla TC, so the ore gate is a no-op standalone). They belong to the
   compatibility milestone, not this row.
6. **Ores generate in superflat** — the original's flat-world check gated
   trees/structures/aura, never ores.
7. **`regen*` config options** have no placement-side effect: modern chunk
   regeneration re-runs features; the options remain registered for config
   parity only.

## Testing

- `src/test/.../worldgen/TcWorldGenMathTest.java` — band math + seeded
  distributions (no game classpath).
- `src/test/.../worldgen/WorldGenDataParityTest.java` — the JSONs must keep
  encoding the original counts/bands/variants/option names; blockstate covers
  variants 0-7; ore is in `mineable/pickaxe`.
- `gametest/WorldGenGameTest.java` — (a) placed features decode and are wired
  into plains' `underground_ores` additively (vanilla ores still present);
  (b) seeded runs over a 3x3-chunk stone region reproduce exact attempt
  counts (18/chunk × 9 chunks cinnabar blocks), Y bands, variant domains,
  and per-vein variant diversity — counts are filtered by variant so each
  run's blocks are isolated from earlier runs in the same region; (c)
  toggling `generate_cinnibar_ore` off suppresses the feature and re-enabling
  restores it. The region helper primes `OCEAN_FLOOR_WG` because the
  game-test server never runs the worldgen prime stage and vanilla
  `OreFeature.place` guards on that heightmap.
