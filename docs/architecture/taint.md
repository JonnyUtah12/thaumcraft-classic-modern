# Taint system (TC-M5-04-s)

Port of Thaumcraft 4.2.3.5's taint at shipping size: the spread / conversion /
cleansing rule layer, the two taint blocks, the Tainted Land biome as data, and
the world drivers that run the rules.

Code: `taint/TaintRules.java` (pure arithmetic — every threshold a named
constant), `taint/TaintManager.java` (world entry points, the tick bodies, the
biome write/restore, the guarded-dependency seams), `taint/TaintConversion.java`
(the conversion table), `taint/TaintBiomes.java` (biome ids, the owned-biome
set, the column write + client sync), `blocks/BlockTaint.java`,
`blocks/BlockTaintFibres.java`, `registration/catalog/CatalogTaint.java`,
`world/TaintTickDriver.java`, `world/TaintChunkWatch.java`,
`world/TaintWorldgenHooks.java`.
Tests: `taint/TaintRulesTest` (12 unit tests over the rule matrix),
`taint/TaintRulesParityTest` (46 regex pins against
`reference/decompiled/thaumcraft`, skipped by house convention when
`reference/` is absent), `gametest/TaintGameTest` (8 in-world probes).

## Scope

Shipped here: the rule layer and its tick wiring (crust decay, soil decay, the
crust fall, the log/soil conversion table, the fibres meta ladder, the fibres
death rule, the biome spread), the biome write/restore primitive (the cleansing
operation), the Tainted Land biome as registry data with the original's
colours/climate, the per-column biome sync, the worldgen seed rules, and the
taint-material / biome-change seams.

Left to its own card, each with a working seam here: the taint entities —
falling taint, spores, swarms, the crawlers, the taint mobs and spawn eggs —
(TC-M6-06), the Taint Poison effect and the bottled-taint projectile
(TC-M6-05 / TC-M6-06), the flux goo that crust decays into (TC-M5-03), the
block assets and the client droplet FX (TC-M5-07), and the biome generation
weight, which the biome JSON carries at the original's value (2) while the
terrain-biome-source wiring is the worldgen card's row.

## The rule layer

`TaintRules` takes the world facts it needs as plain booleans/ints, so the
whole contract is decidable without a world: the seed gates, the fibres meta
ladder (`nextInt(10) != 0` → grass → `nextInt(12) < 10` → plant →
`nextInt(3) == 0` → stalk → meta 4), the conversion thresholds (wood + 2
adjacent taint → crust, non-air + 3 → soil), the decay moduli (crust
`nextInt(20)`, soil `nextInt(10)`), the scan box (`nextInt(3) - 1`,
`nextInt(5) - 3`, `nextInt(3) - 1`), the spread gate (target column not
already taint, `nextInt(rate * 5) == 0`, `getAdjacentTaint >= 2`), and the
cleansing/bottle constants.

`TaintManager` holds the tick bodies in the original's order and shape:
`TaintManager.taintBlockTick` and `fibresTick` mirror
`BlockTaint.updateTick` / `BlockTaintFibres.updateTick` line by line, and
`TaintRulesParityTest` pins the decompiled text they were transcribed from.

The original draws all of these from `world.rand` (one shared sequence). The
port passes the level's `RandomSource` for the same reason, and
`TaintGameTest` replays a seed's draw order in a mirror
(`mirroredScanOffset`) to assert the world result matches the mirrored
expectation — that pins each modulus exactly; a statistics band would not.

## Drivers and the one-subscriber rule

The original's taint runs on the random-tick bus. In the port the tick reaches
`taintBlockTick` / `fibresTick` through `world/TaintTickDriver`, which is the
only `BlockEvents.onBlockEventTrigger` subscriber in the tree (see
`docs/architecture/registration.md`: one subscriber per event), dispatching on
`state.is(TAINT_BLOCKS)`. `world/TaintChunkWatch` is the only
`ChunkWatchEvent.Unload` subscriber and re-sends the biome palette a chunk
carries in the Tainted Land set. The `ITaintedMob` interface and
`TaintManager.onTaintMobDeath` are the seam the entity card plugs into.

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

1.7.10 stores one biome byte per chunk column; 1.21.1 stores a 4×4×4 palette
per section. `TaintBiomes.setBiomeAtAndSync` therefore writes **every section**
of the column (the probe asserts the palette from the world floor to its
ceiling), then sends the port's own `PacketBiomeChange` — 1.7.10's
per-column biome packet, and the client applies it through the
`network/ClientEffectHooks.biomeChange` seam built by TC-M7-05.

The original's biome spawns exactly one mob, the Taintacle, gated on
`Config.spawnTaintacle`. The JSON carries no mob spawns: that entity is
TC-M6-06's row, and the spawn entry lands with it.

## Guarded dependencies

Every call site that needs a row this card does not own goes through a
registration check in `TaintManager` — `entityRegistered` (falling taint,
spores, swarms, crawlers), `effectRegistered` (Taint Poison),
`fluxGooRegistered()` (the crust decay target). `TaintManager.guardedDependencies()`
lists them. The consequence, and the reason the probes branch on those checks:
this slice boots, spreads, converts, falls and cleanses with the other rows
absent, and keeps the same behaviour once they land. The crust fall runs the
entity path when `thaumcraft:fallingtaint` is registered, and the original's
block-move path (`world.setBlock(y+1, air)`, `world.setBlock(y2, …, 3)`) while
it is not.

## Deviations

Recorded as BD-15 in `docs/BEHAVIORAL_DEVIATIONS.md`:

- **Biome colour slots.** The original's `func_76731_a(float) = 8144127` — the
  "sky at height" colour that 1.7.10's `RenderSky` mixes in by altitude — has
  no slot in 1.21.1's `BiomeSpecialEffects`, which offers fog, sky, water and
  water-fog (plus the grass/foliage overrides). The client derives the altitude
  gradient from `sky_color` alone. Client-side only; no server behaviour
  changes. UNAVOIDABLE.
- **The falling-crust floor.** The original guards the fall with `y2 >= 0`,
  and in 1.7.10 y=0 *is* the world's bottom layer. 1.21.1's floor is the
  level's build limit, so the port compares against `level.getMinBuildHeight()
  + 1` — the meaning ("the target cell stays inside the world") carries over;
  the literal 0 would freeze the fall for the bottom 64 layers of the
  overworld. One cell of difference at the floor of a 1.7.10-shaped dimension.
- **The `variant` range of `blocktaint`.** The original's block declares four
  metas (0..3) and the name table lists a fourth label, but nothing in the
  decompiled tree writes a meta 3: the aspect table keys 0 and 1, the flesh
  recipe writes 2, the taint blob / spread / decay paths write 0 and 1, and
  the original's own creative tab (`getSubBlocks` → 0..2) stops at flesh. The
  port ships 3 variants, so the property, the name table, the tab and the
  item's tab count all match what the original *does* (the dead fourth label
  is not shipped). A save that somehow held meta 3 would read it as
  unregistered; no content in the original produces it.
- **Tick cadence.** The original's `updateTick` runs on the random-tick bus.
  The port's driver fires every `TaintRules.TICK_INTERVAL = 30` ticks and
  keeps the original's gates inside the body, so the original's own odds stay
  theirs (spread interval 30, crust decay 1/20, soil decay 1/10, spore
  1/200) and the density comes from the scan, as in the original.
- **Numeric-id config options.** `config.biomes.biome_taint` (the original's
  numeric biome id) is not implemented, per BD-07 / D-010: 1.21.1 registries
  are name-keyed. The biome is the registry entry above.
