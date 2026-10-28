# Taint system (TC-M5-04-s, half 1)

Port of Thaumcraft 4.2.3.5's taint at shipping size, **half 1**: the spread /
conversion rule layer, the two taint blocks, the Tainted Land biome as data, the
runtime biome-conversion primitive, and the client biome-sync seam.

Code: `taint/TaintRules.java` (pure arithmetic — every threshold a named
constant), `taint/TaintManager.java` (the tick bodies in the original's order,
the biome write/restore, the guarded-dependency seams), `taint/TaintConversion.java`
(the conversion table), `taint/TaintBiomes.java` (biome ids, the owned-biome
set, the column write + client sync), `blocks/BlockTaint.java`,
`blocks/BlockTaintFibres.java`, `registration/catalog/CatalogTaint.java`,
`api/entities/ITaintedMob.java` (the seam the entity card plugs into),
`network/ClientEffectHooks.biomeChange` (the client half of the biome packet,
the TC-M7-05 seam), data: `worldgen/biome/biome.biomegentaint.json`,
`loot_table/blocks/{blocktaint,blocktaintfibres}.json`.

Tests: `taint/TaintRulesTest` (12 unit tests over the rule matrix),
`taint/TaintRulesParityTest` (48 regex pins over the decompiled
`reference/decompiled/thaumcraft` sources, 2 test methods; skipped by house
convention when `reference/` is absent, so CI runs it only where the reference
tree exists), `gametest/TaintGameTest` (8 in-world probes).

## Scope

Shipped here: the rule layer and its tick wiring (crust decay, soil decay, the
crust fall, the log/soil conversion table, the fibres meta ladder, the fibres
death rule, the biome spread across chunk boundaries), the biome write
primitive + per-column biome sync, the Tainted Land biome as registry data with
the original's colours/climate, and the taint-material / biome-change /
`ITaintedMob` seams.

**Half 2 (`t_a85fbd52`, TC-M5-04b) owns taint cleansing, the aura/vis
interaction and restoring the original biome of a tainted column.** The
`restoreBiome` primitive here is the seam it builds on: today it hands a
tainted column back to `minecraft:plains` (1.7.10 never stored the pre-taint
biome either), so "the biome the column had before" is that card's row, not
this one.

Left to its own card, each with a working seam here: the taint entities —
falling taint, spores, swarms, the crawlers, the taint mobs and spawn eggs —
(TC-M6-06), the Taint Poison effect and the bottled-taint projectile
(TC-M6-05 / TC-M6-06), the flux goo that crust decays into (TC-M5-03), the
block assets and the client droplet FX (TC-M5-07), and natural biome placement
at worldgen — the biome JSON carries the original's generation weight (2)
while the terrain-biome-source wiring is the worldgen card's row (TC-M5-05).

## The rule layer

`TaintRules` takes the world facts it needs as plain booleans/ints, so the
whole contract is decidable without a world: the seed gates, the fibres meta
ladder (`nextInt(10) != 0` → grass → `nextInt(12) < 10` → plant → stalk,
meta 3), the conversion thresholds (wood + 2 adjacent taint → crust, non-air
+ 3 → soil), the decay moduli (crust `nextInt(20)`, soil `nextInt(10)`), the
scan box (`nextInt(3) - 1`, `nextInt(5) - 3`, `nextInt(3) - 1`), and the
spread gate (target column not already taint, `nextInt(rate * 5) == 0`,
`getAdjacentTaint >= 2`, `rate > 0`).

`TaintManager` holds the tick bodies in the original's order and shape:
`TaintManager.taintBlockTick` and `fibresTick` mirror
`BlockTaint.func_149674_a` / `BlockTaintFibres.func_149674_a` line by line, and
`TaintRulesParityTest` pins the decompiled text they were transcribed from.

The original draws all of these from `world.rand` (one shared sequence). The
port passes the level's `RandomSource` for the same reason, and
`TaintGameTest` replays a seed's draw order in a mirror
(`mirroredScanOffset`) to assert the world result matches the mirrored
expectation — that pins each modulus exactly; a statistics band would not.

## Tick wiring

The original's taint runs on the 1.7.10 block-tick queue
(`func_149674_a` = `updateTick`). In 1.21.1 that bus is `Block#randomTick`,
and the vanilla tick loop only calls it for a state whose `Properties` carry
`randomTicks()` — so **both taint blocks set `.randomTicks()`** and delegate
straight to `TaintManager` (`BlockTaint.randomTick` → `taintBlockTick`,
`BlockTaintFibres.randomTick` → `fibresTick`). The fibres death rule
(`func_149695_a`) is `neighborChanged`, the step poison (`func_149724_b`) is
`entityInside`.

No event-bus subscriber is involved: the tick reaches the rules through the
block classes, which is why this card adds no `BlockEvents` subscriber (see
`docs/architecture/registration.md`: one subscriber per event). The biome
sync's client half rides the payload handler that TC-M7-05 built
(`network/ClientEffectHooks.biomeChange`).

The probes enter the tick the same way the game does —
`BlockState#randomTick` on the state at the cell, which dispatches on the live
state into `TaintManager` — so every seeded probe pins the block wiring, and
the registration probe asserts that every state of both blocks is marked
random-ticking. Without `Properties#randomTicks()` the rules keep passing in
probes that call `TaintManager` directly while doing nothing in a live world;
that is the failure this assertion exists to catch.

Cadence: 1.21.1 fires random ticks at its own density
(`randomTickRate` × one attempt per 1/3 section per tick, default rate 3);
the original's queue ran `updateTick` on its own tick-priority schedule. The
gates *inside* the body are the original's numbers, so the odds of each
individual rule (decay 1/20 and 1/10, swarmer 1/200, spread `1/(rate*5)`) are
theirs; the arrival rate of ticks per block is the platform's. BD-15.

## Biome

`data/thaumcraft/worldgen/biome/biome.biomegentaint.json` — id
`thaumcraft:biome.biomegentaint`, in the `thaumcraft:owned_biomes` set, so
`restoreBiome` knows which columns are ours to give back. Climate and colours
are the original's numbers: temperature 0.5, downfall 0.5, precipitation on,
fog 7160201, sky 7160201, water 13373832, water fog 329011, grass and foliage
8154503 (see `BiomeGenTaint.java` in the reference tree). The original's
decoration (1-in-8 magic tree, 10 fibre scatter passes, 8 surface-spread
passes) is its own worldgen row; the JSON carries only the palette, effects
and the generation weight.

1.7.10 stores one biome byte per chunk column; 1.21.1 stores a
`PalettedContainer<Holder<Biome>>` per 16³ section, indexed in **quart** space
— the cell of a block coordinate is `(coord >> 2) & 3`, which is exactly the
index `ChunkAccess#getNoiseBiome` reads. `TaintBiomes.setBiomeAt` therefore
writes the `(x >> 2) & 3, 0..3, (z >> 2) & 3` cell of every section of the
chunk. Two consequences, both pinned by probes:

- a column write covers **every section** of the column (the probe samples the
  palette from the world floor to its ceiling), and
- a biome cell is 4 blocks wide, so the write is **4-block granular**: it
  taints the 4×4-block footprint containing the column, and nothing in the
  neighbouring cell (BD-15). Writing `x & 3` — the 1:1 transcription of the
  byte write — lands in the wrong cell for three of every four columns; the
  footprint probe is the test that catches that.

Reads go through `TaintBiomes.biomeAt` (`LevelReader#getNoiseBiome`, i.e. the
chunk palette) and not `Level#getBiome`: `getBiome` goes through
`BiomeManager`, which fiddles the coordinates and caches, so a freshly
converted column can read as the generated biome for tens of seconds. In
1.7.10 the byte read saw the byte the last tick wrote; reading the palette is
what keeps that rule ordering faithful.

`setBiomeAtAndSync` also sends the port's `BiomeChangePayload` (1.7.10's
`PacketBiomeChange`) to the players tracking the column; the client applies
the same palette write, so fog/sky/grass tint follow a runtime conversion.

The original's biome spawns exactly one mob, the Taintacle, gated on
`Config.spawnTaintacle`. The JSON carries no mob spawns: that entity is
TC-M6-06's row, and the spawn entry lands with it.

## Guarded dependencies

Every call site that needs a row this card does not own goes through a
registration check in `TaintManager` — `entityRegistered` (falling taint,
spores, swarms, crawlers), `effectRegistered` (Taint Poison),
`fluxGooRegistered()` (the crust decay target). `TaintManager.guardedDependencies()`
lists them. The consequence, and the reason the probes branch on those checks:
this slice boots, spreads, converts and falls with the other rows absent, and
keeps the same behaviour once they land. The crust fall runs the entity path
when `thaumcraft:fallingtaint` is registered, and the original's block-move
path (`world.setBlock(y+1, air)`, `world.setBlock(y2, …, 3)`) while it is not.

## Deviations

Recorded as BD-15 in `docs/BEHAVIORAL_DEVIATIONS.md`:

- **Biome colour slots.** The original's `func_76731_a(float) = 8144127` — the
  "sky at height" colour that 1.7.10's `RenderSky` mixes in by altitude — has
  no slot in 1.21.1's `BiomeSpecialEffects`, which offers fog, sky, water and
  water-fog (plus the grass/foliage overrides). The client derives the altitude
  gradient from `sky_color` alone. Client-side only; no server behaviour
  changes. UNAVOIDABLE.
- **The biome write is 4-block granular.** 1.7.10's `Utils.setBiomeAt` wrote
  one byte for one 16×16 chunk column; 1.21.1's palette cell is 4×4 blocks, so
  a "column" write taints that footprint. A write narrower than the cell is
  not expressible. Pinned by the footprint probe. UNAVOIDABLE.
- **The palette index is quart-space.** `x & 3` (the literal transcription of
  the byte write) is the offset *inside* a cell; the cell is `(x >> 2) & 3`.
  The port writes the cell the read reads, so the shipped behaviour matches the
  original's intent, not the literal formula.
- **Tick cadence.** The original's `func_149674_a` ran on the 1.7.10 block-tick
  queue; 1.21.1 runs `Block#randomTick` at its own density, gated by
  `Properties#randomTicks()` (set on both taint blocks). The per-rule odds are
  the original's; the per-block tick rate is the platform's.
- **The falling-crust floor.** The original guards the fall with `y2 >= 0`,
  and in 1.7.10 y=0 *is* the world's bottom layer. 1.21.1's floor is the
  level's build limit, so the port compares against `level.getMinBuildHeight()`
  — the meaning ("the target cell stays inside the world") carries over; the
  literal 0 would freeze the fall for the bottom 64 layers of the overworld.
  One cell of difference at the floor of a 1.7.10-shaped dimension.
- **The `variant` range of `blocktaint`.** The original's block declares four
  metas (0..3) and the name table lists a fourth label, but nothing in the
  decompiled tree writes a meta 3: the aspect table keys 0 and 1, the flesh
  recipe writes 2, the taint blob / spread / decay paths write 0 and 1, and
  the original's own creative tab (`getSubBlocks` → 0..2) stops at flesh. The
  port ships 3 variants, so the property, the name table, the tab and the
  item's tab count all match what the original *does* (the dead fourth label
  is not shipped). A save that somehow held meta 3 would read it as
  unregistered; no content in the original produces it.
- **Block sound.** The original's `func_149672_a(new CustomSoundType("gore",
  0.5F, 0.8F))` maps onto a vanilla `SoundType` (`ROOTS`) in the block
  `Properties`: a custom sound type needs volume/pitch events registered, and
  `ModSounds` events are not registered at the time block `Properties` are
  built (registration order). Volume/pitch multipliers have no 1.21.1
  `SoundType` slot. Client-side audio only.
- **Numeric-id config options.** `config.biomes.biome_taint` (the original's
  numeric biome id) is not implemented, per BD-07 / D-010: 1.21.1 registries
  are name-keyed. The biome is the registry entry above.
