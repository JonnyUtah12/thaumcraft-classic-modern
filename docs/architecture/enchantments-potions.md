# Enchantments, potions and warp (TC-M6-05-s)

Original domain: `EnchantmentHaste`, `EnchantmentRepair`, the nine
`Potion*` classes, `WarpEvents` (Thaumcraft 4.2.3.5, 1.7.10). 18
content-mapping rows: 9 potions, 2 enchantments, 3 items, 2
`infusion_enchantment` recipes, 2 config options.

## 1. Enchantments are data, the behaviour is code

1.21.1 has no `Enchantment` subclass to register: an enchantment is a JSON
payload in the `enchantment` datapack registry (`net.minecraft.world.item.enchantment.Enchantment`)
with no behaviour slots, so the slice splits in two:

* **Data** — `data/thaumcraft/enchantment/ench_haste.json` (weight 3,
  max level 3, `slots: feet`, costs 15/9 · 65/9, anvil 2) and
  `ench_repair.json` (weight 2, max level 2, `slots: any`, costs 20/10 ·
  70/10, anvil 2, `exclusive_set: #thaumcraft:exclusive_set/repair`).
  The original's `isApplicable(ItemStack)` predicate becomes a tag
  (`#thaumcraft:enchantments/haste_applicable`, `…/repair_applicable`) —
  the only structural difference the payload carries, and the
  `EnchantmentDataParityTest` row that pins it.
* **Behaviour** — `enchantment/EnchantmentBehaviours`, subscribed to
  `PlayerTickEvent.Post`. It does not extend `Enchantment`; it reads the
  enchantment levels off the stacks (`EnchantmentHelper.getItemEnchantmentLevel`
  against the `ResourceKey`s in `registration/ModEnchantments`) and applies
  the original's effects.

**Haste** (`EventHandlerEntity.updateSpeed`, lines 290-318) adds a
`MOVEMENT_SPEED` attribute modifier of `level * 0.015F` (`ADD_MULTIPLIED_TOTAL`,
id `thaumcraft:enchantment_haste`), halved while sneaking, and removes the
modifier when no worn boots carry haste. The salvaged WIP gated on
`!player.isCreative()`; the original gates on `!capabilities.isFlying` and
`field_70701_bs.getAttributeBaseValue() > 0` — a creative player on the ground
gets the bonus, a flying survival player does not, and the gate reads the
attribute, never `player.motionX/Z` (`EnchantmentPotionGameTest.hasteGatesMatchOriginal`).

**Repair** (`EnchantmentRepair.onUpdate`) mends `level` damage on the worn
stack, paid from the stack's own vis: it walks `IRepairable` gear, prices the
object tags `(int) Math.sqrt(essentiaAmount * 2) * level` in centi-vis against
`IVisContainerItem.consumeVis`, and pays all-or-nothing before mending
(`doRepair`). Mending itself is vanilla: the payload's `exclusive_set` tag
(`#thaumcraft:exclusive_set/repair` = `thaumcraft:ench_repair` +
`minecraft:mending`) makes the two mutually exclusive, since both repair the
item. `api/IRepairable` / `api/IRepairableExtended` / `wands/IVisContainerItem`
came in with the slice; `registration/TestProbeContent.PROBE_REPAIRABLE` is the
test-only stack that carries both interfaces.

## 2. Potions: 1.7.10 ids became registry paths

`MobEffect` replaces 1.7.10's `Potion` + numeric id table. For each of the
nine effects the port keeps the original's **name** as the registry path
(`thaumcraft:potion.fluxtaint` …, from `setPotionName`), the original's
**colour** as the `MobEffect` constructor argument (verified against
`Config.java:331-339` — flux flu and thaumarhia are both 6702199, 6706551
belongs to flux phage), the original's **bad/good** flag as
`MobEffectCategory.HARMFUL`/`BENEFICIAL`, and `isReady`/`onEffectTick` as
`shouldApplyEffectTickThisTick`/`applyEffectTick`.

| effect | gate (original) | body |
|---|---|---|
| fluxtaint | every `40 >> amplifier` ticks (every tick at amp ≥ 6) | 1 taint damage, skipped while in water or on an `ITaintedMob` |
| visexhaust / infvisexhaust | `% 40` | 4 vis of each aspect from worn gear; amp > 0 spreads itself at amp − 1, amp 0 converts nearby entities to visexhaust |
| unhunger | every tick | `0.025F * (amplifier + 1)` exhaustion |
| thaumarhia | `% 20` | drops one random aspect from every curio slot (no-op while curios is absent — BD-16) |
| sunscorned | `% 40` | 2 damage under an unobstructed sky at day, 6000-tick night vision in between |
| warped/blurred/deathgaze | flag only | read by the warp driver and by the renderer |

The damage paths use `api/damagesource/DamageSourcesThaumcraft` with
`data/thaumcraft/damage_type/taint.json` (1.7.10's `damageEntity(..., Magic)`
is a damage type in 1.21.1). The curio walk keeps the original's
`!(slot.contains("belt") || slot.contains("curio.belt"))` predicate verbatim:
belts are excluded, and the belt branch is the one curios removes.

## 3. Warp: the roll is data, the driver is thin

`warp/WarpLogic` is the pure half of the original's `checkWarpEvent`
(lines 39-225) and `checkDeathGaze` (lines 324-347): the fire gate
`warpCounter > 0 && warp > 0 && rand(100) <= sqrt(warpCounter)`, the roll
`min(100, (warp + warp + warpCounter) / 3)`, the counter rewrite
`(int)(warpCounter - max(5.0, sqrt(warpCounter) * 2.0))` — a **set**, which
drains the counter to 0 and closes the gate until an eldritch grant refills it
— the amplifier `min(3, warp / 15)`, the whole `if (eff …)` chain as
`WarpLogic.outcome(eff, warp)` (durations, amplifier, lang key, grant count,
guardian/spider counts), the death-gaze cone (`isVisibleTo(0.75F, …)`) and
range `min(8 + level * 3, 24)` (the port adds the 1 back for the original's
1-based potion level).

`warp/WarpEvents` (`@EventBusSubscriber`) is the world-facing half:
`EventHandlerEntity.livingTick:235-243` → cadence `tickCount > 0 &&
tickCount % 2000 == 0`, no roll under `TcConfigCatalog.WUSS_MODE` (`Config.wuss`)
or while `thaumcraft:potion.warpward` is active; the death gaze on
`% 10 == 0` with no wuss gate; gear warp through `api/IWarpingGear`; warp
changes through `ResearchManager.applyWarp` (+ `SyncWarpPayload`), chat through
`WarpMessagePayload`, the aspect-pool grant through the
`player/PlayerKnowledgeData` attachment (`grantResearch`:
`1 + random.nextInt(times)` points into a random primal), and the tail decay
`addWarpTemp(-1)` on every event, fired or not.

Left as seams (each is its own card, asserted inert by the probes):

* **Spawns** — the guardian and mind spiders (`WarpEvents.java:226-323`) are
  entity content (TC-M6-06); the mist/spider outcomes still emit their chat
  line, and the counts the original computed are carried in
  `WarpLogic.WarpOutcome.guardians()` and pinned by `WarpLogicTest`.
* **Eldritch research** — the 10/25/50 grants name `@BATHSALTS`,
  `ELDRITCHMINOR`, `ELDRITCHMAJOR`; none exists in the shipped 201-entry graph
  (TC-M6-08), so every grant is graph-gated inert (probe:
  `eldritchResearchSeamIsInert`). The bath salts the hint points at ships here.
* **Fortress mask relief** (`WarpEvents.java:55-60`) — armour, TC-M8.
* **Client FX / sound** (`PacketMiscEvent`, particles) — TC-M7-02/04.
* **`warp.text.hunger.1/2`** — the original's call site is
  `EventHandlerEntity.java:700-702` (the food handler), outside this slice, so
  the keys are deliberately not shipped.

## 4. Tests

* `warp/WarpLogicTest` — the accumulation math: fire gate, roll, the counter
  rewrite (including its negative floor), a multi-event drain to zero, the
  amplifier cap, every branch of the outcome table, the gaze cone, and that
  every lang key the table can emit ships.
* `warp/WarpParityTest` — regex pins over `reference/decompiled`: the 2000 /
  10-tick cadences, the ward + wuss gates, the roll arithmetic as written, the
  effect-duration literals, `Math.min(3, warp / 15)`, `isVisibleTo(0.75F`,
  `Potion.wither`, `addWarpTemp(player, -1)`, `0.015F`; and the 15
  `warp.text.N` strings verbatim against `en_US.lang`.
* `enchantment/EnchantmentDataParityTest` — the two payloads, the
  applicability tags, the mending-exclusivity set, the lang pair.
* `gametest/EnchantmentPotionGameTest` (14 probes) — the speed modifier and its
  gates on a live player, the vis-payment + mending flow, the effect tick
  gates, flux-taint damage through `thaumcraft:taint`, the infectious
  spread/convert, the exhaustion body, the soap's cleansing, the 200-tick bath
  salt lifespan, registry presence of the nine effects / three items / two
  enchantments, the cadence + ward gate + decay, a seeded real warp event
  against the pinned table branch, the gaze cone and its amplifier-scaled
  range, the aspect-pool grant, and the two inert seams.
* `gametest/LocalizationGameTest` now walks the mob-effect and enchantment
  registries too: every registered effect/enchantment must carry
  `effect.thaumcraft.*` / `enchantment.thaumcraft.*`.
