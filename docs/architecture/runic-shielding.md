# Runic shielding and the baubles items (TC-M8-02)

Design record for `runic/RunicShielding`, `api/IRunicArmor`,
`api/IVisDiscountGear`, `items/baubles/*` and `wands/{WornGearDiscounts,
VisContainerItem}`. The slot/provider layer they plug into is
[accessories.md](accessories.md); the vis payment they use is
[vis.md](vis.md).

## Original sources

- `thaumcraft/common/items/baubles/{ItemBaubleBlanks, ItemAmuletRunic,
  ItemRingRunic, ItemGirdleRunic, ItemAmuletVis, ItemGirdleHover}.java`
  (decompiled TC 4.2.3.5, `reference/decompiled/`).
- `thaumcraft/common/lib/events/EventHandlerRunic.java` — the runic-shield
  engine all runic gear drives.
- `thaumcraft/api/{IRunicArmor, IVisDiscountGear}.java`,
  `thaumcraft/common/items/wands/WandManager.java:49-111`.
- Config values pinned in `docs/balance-values.md` §1.7
  (`shieldRecharge=2000ms`, `shieldWait=80t`, `shieldCost=50` centi-vis
  Aer+Terra per unit).

## Item table (original metas preserved via `thaumcraft:item_meta`)

| Registered id | Class | Metas → effect |
|---|---|---|
| `itembaubleblanks` | `ItemBaubleBlanks` | 0 amulet / 1 ring / 2 belt blanks (no effect); 3-8 apprentice rings, RING, 1% vis discount toward `getPrimalAspects()[meta-3]` |
| `itemringrunic` | `ItemRingRunic` | RING; charge 1 / 5 / 4 / 4; meta 2 charged (recharge −500 ms), meta 3 revitalizing (Regeneration on depletion) |
| `itemamuletrunic` | `ItemAmuletRunic` | AMULET; charge 8 / 7; meta 1 emergency (refill `min(max, 8n)` on depletion) |
| `itemgirdlerunic` | `ItemGirdleRunic` | BELT; charge 10 / 9; meta 1 kinetic (push burst on depletion) |
| `itemamuletvis` | `ItemAmuletVis` | AMULET; capacity 2500 / 25000 centi-vis; meta 1 gated on `VISAMULET` research; worn-tick transfer to held vis container |
| `itemgirdlehover` | `ItemGirdleHover` | BELT; worn-tick `fallDistance -= 0.33` |

`thaumcraft:focuspouch` (content-mapping.csv:193) is a belt bauble in the
original but is owned by TC-M6-02 (foci + casting, docs/catalog-triage.md) —
its container parent `ItemFocusPouch` is focus work, so it is deliberately
not registered here.

## Runic shield pool (`RunicShielding`)

Runtime-only state keyed by player UUID (the original's HashMaps were never
saved; the pool rebuilds from worn gear on the next scan). Rules preserved
term-for-term from `EventHandlerRunic`:

- Gear scan when dirty or every 40 player ticks; capacity = sum of
  `getFinalCharge` over the four armor slots AND the worn accessory set.
  Armor slots are scanned here so TC-M6-07 armor joins through the same
  `IRunicArmor` contract.
- `getFinalCharge` = base + `thaumcraft:runic_hardening` component (the
  original's `"RS.HARDEN"` NBT; written by the Runic Augmentation flow,
  TC-M6-07).
- Recharge: one unit per `shieldRecharge − chargedRings·500` ms (real-time
  clock), each unit paying Aer+Terra `shieldCost` centi-vis all-or-nothing
  from worn vis containers first, then the main inventory in reverse slot
  order (`WandManager.consumeVisFromInventory` order; the port matches any
  stack with a `thaumcraft:vis_pool` component).
- Absorption with the original's truncating math (`charge > damage` →
  `(int)(charge − damage)`, damage 0; else damage −= charge, pool 0).
  Excluded sources: starve, drown, outOfWorld, wither (SRG fields resolved
  against MCP stable_11 `fields.csv`).
- On-depletion upgrades, each with its own cooldown: kinetic push
  (`newExplosion` r = 1.5 + 0.5n, no damage, 20 s), revitalizing
  Regeneration (240 t, amplifier = ring count, 20 s), emergency refill
  (`min(max, 8n)`, 60 s). Then `rechargeDelay = shieldWait`.
- Upgrade flags come from `RunicUpgradeFlags` (the original instanceof-tested
  the three concrete classes and switched on metas, lines 66-80; the port
  keeps identical per-item results and stays open for armor).

Deferred, cited at call sites: `PacketRunicCharge` HUD sync and
`PacketFXShield` hit FX (TC-M7-04/M2-03), fortress-helm mask effects
(TC-M6-07).

## Vis discount aggregation

`WornGearDiscounts.gearPercentTotal` ports `WandManager.getTotalVisDiscount`
(49-84): worn accessory set + armor slots summed in percent; feeds
`VisCostRules.VisDiscountSource` from TC-M4-02. The Vis Exhaustion term
returns amplifier −1 (no effect) until the potions land with TC-M4-03 —
identical behaviour for players without those effects.

## Vis amulet storage

Charge lives in the shared `thaumcraft:vis_pool` component (same centi-vis
amounts and per-aspect keys as the original's NBTTagInts). The
`wands/VisContainerItem` interface is the original's shared
`ItemWandCasting`/`ItemAmuletVis` duck-type surface; TC-M6-01's wand
implements it unchanged. The vis-relay refill branch (original lines 94-110)
waits for `TileVisRelay` (TC-M6-04 tiles).

## Test coverage

- Unit (20): `RunicShieldingRulesTest` (charge math, scan accumulation,
  interval, absorption boundaries, emergency/kinetic tables, cooldowns),
  `BaubleRulesTest` (slot routing, primal indexing, discount rule, charge
  tables, capacities, transfer cap, fall step).
- Game tests (13, both Curios and no-Curios configs): pool build with
  original capacities, hardening, recharge payment + interval gate,
  absorption + emergency/revitalizing firing, blocked damage sources,
  unequip → pool gone, relog persistence (worn set + vis charge survive,
  pool rebuilds), amulet→wand transfer, research gate, per-stack blank
  slot routing, discount aggregation, hover fall step, rarity/meta parity.
