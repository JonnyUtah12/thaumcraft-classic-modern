# Flux system (TC-M5-03)

Port of Thaumcraft 4.2.3.5's flux (vis pollution) into the modern tree:
generation from magic use, per-chunk flux state, decay, and the effect
thresholds. Code: `world/FluxModel.java` (pure math), `world/FluxEvents.java`
(server entry points + read API), `world/FluxTickHandler.java` (age pass).
Tests: `world/FluxModelTest` (unit), `gametest/FluxGameTest` (generation,
decay, persistence).

## Parity finding: TC4 has no per-chunk flux store

The milestone brief pointed at TC's `WorldMagicData` flux maps. Verified
against the original jar (class scan of `thaumcraft/`): **Thaumcraft 4.2.3.5
has no `WorldMagicData`, no `FluxAge`/`FluxCache`, and no flux handlers in
`common/lib/events`.** Flux in the original is purely block state:

- `common/blocks/BlockFluxGoo.java` — ground flux (a `BlockFluidFinite` with
  `quantaPerBlock == 8`; the block metadata *is* the flux level, 0..8).
- `common/blocks/BlockFluxGas.java` — air flux (same scale).
- `common/blocks/BlockAiry.java` — the "airy" precursor that detonates into
  flux (`explodify`, lines 336-353).

The per-chunk `fluxAir`/`fluxGround` int fields in `WorldMagicData`
(TC-M2-02, already persisted) are the modern equivalent the plan needs; this
card defines their semantics. Quanta are the unit: a flux block of metadata m
contributes `max(1, m)` quanta (meta 0 is a trace but still flux; a full
column is 8). Air and ground are tracked separately — the chunk-scale analogue
of the original's goo/gas split.

## Generation sites (amounts pinned to the original)

| Source | Quanta | Medium | Original site |
|---|---|---|---|
| Crucible spill | 1 | gas/goo 50/50 | `TileCrucible.java:196-204` (1/4 gate at call site) |
| Infusion crash | 7 | goo (types 1/3) / gas (2/4) | `TileInfusionMatrix.java:639-642` (meta 7) |
| Reservoir overflow | 8 | goo below / gas above | `BlockEssentiaReservoir.java:99-101` (meta 8) |
| Node-converter / airy detonation | 8 × up to 50 placements | side of origin | `BlockAiry.explodify`, `BlockAiry.java:336-353` |
| Taint block decay | 8 | goo | `BlockTaint.java:188-193` (meta = `getQuanta()`) |
| Thaumhia | 1 | goo | `PotionThaumarhia.java:44` (meta 0) |
| **Spell casting** | **1 per vis unit** | air | **port design constant** — TC 4.2.3.5 generates no flux from wand casting (no flux references in `ItemWandCasting`; all flux sites are device/block events). The milestone plan requires casting to pollute; the scale mirrors the crucible's one-spill-one-quantum unit. |

Call-site probability gates (the crucible 1/4 roll) stay with the device code
so `FluxModel` stays pure; the devices themselves are later cards (crucible
TC-M6-01, infusion TC-M6-03, nodes TC-M5-02) and will call
`FluxEvents.addFlux(level, pos, source, units)`.

## Decay + diffusion (the age pass)

`FluxTickHandler` runs one "age pass" per level every
`FluxModel.DECAY_PASS_TICKS = 40_960` game ticks. Derivation (TC-M0-06
§8): a goo block loses one metadata level with probability 1/30 per random
tick (`BlockFluxGoo.java:104-112`), and 1.7.10 random-ticks 3 blocks per
4096-block chunk section per game tick (`randomTickSpeed == 3`), so the
expected game ticks per level lost is `30 × 4096 / 3 = 40,960`.

Per pass, each tracked chunk:

- loses one level per flux column (`columns = ceil(flux/8)`; the original
  decays every goo block independently at the same expected rate);
- ground decay vents air flux: 50% per decayed column
  (`BlockFluxGoo.java:108-111`);
- if holding more than one full column, passes 1 quantum to each cardinal
  neighbour — **port adaptation**: the original has no chunk-level diffusion,
  goo/gas spread by `BlockFluidFinite` fluid flow inside the world; this is
  the chunk-scale analogue.

Chunks are tracked when flux is added or when a chunk with persisted flux
loads (restart rebuild, same pattern as `MagicChunkHooks`); they leave the set
at zero. Unloaded chunks stay tracked but are skipped — the original's random
ticks only touched loaded chunks. The pass counter resets on level unload;
worst case after a restart is one extra pass interval of delay.

## Effect thresholds (block-level rules, pinned for later cards)

`FluxModel` pins the original's threshold rules so the taint/mob/potion
cards consume them instead of re-deriving:

- Thaumic slime spawn: level 2..5 → 1/25 size-1; level ≥6 → 1/25 size-2
  (`BlockFluxGoo.java:88-99`). Slime growth while standing in goo: grows
  while `size < level`, coin flip, consumes one level (`:55-61`).
- Biome→Taint conversion: level ≥6 and `biome_taint_from_flux` (default
  true, `Config.java:88,497-499`): 1/50 (`BlockFluxGoo.java:100-103`).
  The conversion action belongs to TC-M5-04.
- Gas contact: 1/10 per contact → 50/50 Vis Exhaustion 1200t (amp level/3,
  curatives cleared) or Nausea 80+level×20t (`BlockFluxGas.java:59-77`).
- Goo contact: Vis Exhaustion 600t (amp level/3, curatives cleared)
  (`BlockFluxGoo.java:63-65`).

Vis Exhaustion is TC's potion (`PotionVisExhaust`), registered by TC-M6-05;
until then the effect ids in `EffectSpec` are the stable contract. The entity
gates (taint immunity, already-affected check) and level consumption stay
with the caller that applies them.

## Visualization hooks (for TC-M7-02)

`FluxEvents.fluxAir/fluxGround(level, pos)` and
`FluxEvents.levelAt(level, pos)` (0..8, the original's metadata scale) are
the read API for the thaumometer/goggles.

## Wiring

- `ThaumcraftClassic.registerGameTests` registers `FluxGameTest` (+3).
- No new config keys: `biome_taint_from_flux` already exists in the catalog
  (TC-M1-02) and is consumed by `FluxModel.taintConversionRoll`'s caller.
- No new persistence: values live in the existing `WorldMagicData`
  `fluxAir`/`fluxGround` fields (TC-M2-02 codec, unchanged).
