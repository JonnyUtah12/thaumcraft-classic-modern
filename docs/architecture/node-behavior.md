# Aura node behavior (TC-M5-02)

The live half of the node system: the TileNode tick loop (production,
offline catch-up, stability/orbs, discharge, taint/dark/pure/hungry
behaviour, death spiral), the wand-tap draw + vis payment, the reveal
predicate, and the stabilizer lock — run as a data-driven runtime over the
TC-M5-01 node records, with no node block or tile entity. TC-M5-01
(`docs/architecture/aura-nodes.md`) generates and stores the records; this
card makes them behave.

## Source of truth

- `reference/decompiled/thaumcraft/thaumcraft/common/blocks/entities/TileNode.java`
  - `updateEntity` L105-124: tick order — `count++`, `checkLock`,
    `handleDischarge`, `handleRecharge`, `handleTaintNode`,
    `handleNodeStability`, `handleDarkNode`, `handlePureNode`,
    `handleHungryNode`.
  - `handleRecharge` L660-771: regen-period recompute (L661-674), offline
    catch-up (L688, L285-289), death spiral (L707-748), `wait` pause
    (L760-762), production at `count % regeneration == 0` (L763-769).
  - `handleDischarge` L579-658, `handleTaintNode` L773-801,
    `handleNodeStability` L803-828, `handlePureNode` L830-852,
    `handleDarkNode` L854-910, `handleHungryNode` L912-940.
  - `checkLock` L946-962 + stabilizer detection L949-957
    (`blockStoneDevice` meta 9/10 below the node); lock multipliers L676-682.
  - `onWandRightClick` L385-441: tap size (L390-397), preserve gate
    (L398-401), draw + clamp + leftover payment (L403-427);
    `chooseRandomFilteredFromSource` L195-211.
  - Fields + persistence: `aspects`/`aspectsBase`/`lastActive` L51-60,
    NBT keys L159-160, `getAspectsBase` L164-167, `setAspects` copy
    L171-174, `addToContainer` L177-182, `takeFromContainer` L184-187,
    read path L247-289.
- `reference/.../common/items/wands/ItemWandCasting.java`:
  `getAspectsWithRoom` L222-231, `addRealVis`/`addVis` L352-363,
  `getMaxVis` L82-84.
- `reference/.../client/renderers/tile/TileNodeRenderer.java` L187-197:
  reveal predicate (helmet `IRevealer.showNodes`, else held thaumometer).
- Balance values: docs/balance-values.md §6.2.

## Target-platform design

| Original | Port |
|---|---|
| `TileNode` (block entity at the node block) | `nodes/NodeRuntime` — plain object over the `WorldMagicData.Node` record; no block, no BE. The blockairy host is implicit (record identity) |
| BE ticker driving `updateEntity` | `world/NodeTickDriver` — `ServerTickEvent.Post` hook (same pattern as `FluxTickHandler`/`AuraNodeChunkGen`): every loaded chunk is tracked (`ChunkEvent.Load/Unload`), records are reconciled into runtimes each pass, changed state writes back through `WorldMagicData.replaceNode`, dead nodes drop their record |
| `world.rand` | `LevelRandomBridge(level.getRandom())` — the level's shared RNG bridged to `java.util.Random` (the two methods the engine consumes) |
| `world.getTotalWorldTime()` | `System.currentTimeMillis()` supplier — the 75 ms catch-up math runs in wall-clock ms exactly as the original's |
| `aspectsBase` / `lastActive` NBT keys | `WorldMagicData.Node.aspectsBase()` / `.lastActive()` persisted by the store codec; null `aspectsBase` = "same as aspects" at read time (L171-174), which is what generation writes — generation stays untouched |
| `onWandRightClick` tap | `nodes/NodeWandTap.tap` — research keys `NODETAPPER1/2` + `NODEPRESERVE` read through `ResearchManager`; room filter intersects the node's own primals with pool capacity instead of enumerating the primal registry (same candidate set, registry-free — mirrors the `world/AmbientVis` seam); payment through the TC-M4-02 `WandVis` seam (tap in whole vis, `maxVis` in centi-vis, drain only the amount actually stored) |
| `IRevealer` / `ItemThaumometer` renderer check | `nodes/NodeReveal` — item-keyed registry (`registerRevealer`/`registerThaumometer`) + `canSeeNodes(held, helmet, viewer)` mirroring the original's else-if order; no marker interface needed |
| `blockStoneDevice` meta 9/10 below node | `thaumcraft:node_stabilizer_basic` / `node_stabilizer_advanced` block tags (M6 device blocks opt in; absent tags read as no stabilizer) |
| `isMagicalLog` block-name list | `thaumcraft:magical_log` block tag |
| `BiomeHandler` taint/eerie/magical-forest lookups | taint through the `BiomeAura` registry seam (TC-M5-01); eerie/magical-forest as `NodeTickDriver.registerEerieBiome/registerMagicalForestBiome` sets the biome cards (TC-M5-04+) fill |

## Deliberate adaptations (parity notes)

- **Frozen constants** (unit-tested): regen periods 600/400/900/0 ticks
  (no-modifier/bright/pale/fading — modifier-driven, as original);
  stabilizer lock ×2 basic / ×20 advanced on the period; catch-up window
  `regeneration × 75 ms` wall time, burst capped at the vis-base total;
  death-spiral check every 1200 ticks (one aspect removed per pass,
  `break` after removal, L713-726); stability/taint-conversion gate every
  100 ticks (1/500 taint conversion, unstable→normal at 1/(10000/lock),
  fading→pale at 1/(12500/lock)); biome-effect gates every 50 ticks
  (taint/dark/pure painting, hungry break); lock re-probe every 50 ticks
  (lock change resets the regen period); discharge attempts every
  2/1/3 ticks (default / bright+hungry / pale, PALE skips 50%); hungry
  reach squared-distance 256 with surface clamp + ray trace, breakable
  hardness [0, 5).
- **No node block**: blockairy host, `isWarded` short-circuit, and render
  hooks have no port-side counterpart; `isWarded` reads as false until a
  ward card lands.
- **Entity/device seams are hooks, not no-op stubs**: `spawnAspectOrb`
  (unstable-node primal, L812-818), `breakBlock`/`rayTraceHit`/
  `blockHardness` (hungry), and biome painting are `Context` methods —
  the world driver implements the block-side ones against the live level;
  the orb drop waits for M6 entities. The unstable-node primal *spend*
  and the death-spiral *degradation* are fully modelled.
- **`takeRandomPrimalFromSource`** returns null instead of throwing when
  the node holds no primal (the original's NPE path is unreachable
  through the tick loop); the draw uses the injected level `Random`.
- **Legacy `nodeVisBase` fallback** (TC3 save migration, L168-174 second
  half) is not ported: no TC3 saves exist here.
- **Transient counters** (`count`, `nodeLock`, `regeneration`, `wait`):
  not persisted, exactly as upstream — a chunk reload restarts the regen
  phase; the driver drops runtimes on chunk unload.
- **Dimension gate** for biome painting: nether/end + the TC-M5-01 dim
  blacklist (L831-835, L856-861); the biome-blacklist config has no
  registry yet and reads as allowed.
- **`ChunkPos(int, int)` takes chunk coordinates**: the driver's
  write-back/removal path shifts block coords by 4 first — caught by the
  game test (write-back silently no-op'd before the fix).

## Testing

- `NodeBehaviorMathTest` (31): every frozen constant, the recharge gate +
  catch-up burst math, stability/orb spend + lock-gated conversions,
  discharge transfer + intervals, hungry surface clamp, death-spiral
  degradation/removal order, lock math, tap filter/preserve/leftover.
  Input maps are insertion-ordered (`orderedMap`) wherever a test pins
  which aspect a uniform pick takes — `Map.of` iteration order is
  unspecified and was observed to differ between JVMs.
- `NodeWandTapTest` (9): tap payment through the TC-M4-02 wand seam
  (centi-vis capacity, whole-vis draw), preserve clamp (single-unit
  aspects are never selected, L197-201), full-pool takes nothing.
- `WorldMagicDataNodeBehaviorTest` (4): `aspectsBase`/`lastActive`
  round-trip, lenient load of generated records, `replaceNode` in place.
- `NodeBehaviorGameTest` (4, `runGameTestServer`):
  `driverMaterialisesRuntimeAndTapPersistsToStore` (record→runtime→
  record loop + tap persistence), `productionAndDeathSpiralCycleObservedOverSimulatedTicks`
  (first unit at count 600, base degradation at 1200),
  `emptyNodeDiesAndIsRemovedFromStore` (one aspect per spiral pass,
  record dropped when emptied), `revealGateFollowsTheOriginalSlots`
  (helmet vs held slots against a registered item).
