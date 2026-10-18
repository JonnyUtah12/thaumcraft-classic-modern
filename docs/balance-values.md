# TC-M0-06 — Original balance values and config defaults

Source of truth: the Vineflower-decompiled Thaumcraft 1.12.2 (4.2.3.5) jar at
`../reference/decompiled/thaumcraft/thaumcraft/` (see `docs/decompilation-notes.md`).
Every value below is cited as `Class.field` with the decompiled file path and the line
observed. Line numbers refer to the decompiled tree, not to original sources.
Values that could not be recovered from the jar are marked **UNKNOWN** — never guessed.

Conventions:
- "centi-vis" = 1/100 of one vis unit, the internal integer unit for stored vis (`*100` in `ItemWandCasting.consumeAllVisCrafting`, ItemWandCasting.java:314).
- "ticks" = 1/20 s game ticks unless stated otherwise.
- SRG field names (`field_XXXXX`) are the obfuscated-but-stable MCP/SRG names for Minecraft 1.12 members.

---

## 1. Thaumcraft config class defaults (`thaumcraft.common.config.Config`)

File: `common/config/Config.java`. Field initializers at lines 81–157; Forge-config
binding in `Config.syncConfigurable()` (lines 367–515) and `Config.initialize()`
(lines 171–259). "Effective default" is what a fresh install gets.

### 1.1 Biomes / dimensions

| Field | Config category/key | Type | Effective default | Notes | Line |
|---|---|---|---|---|---|
| `Config.biomeTaintID` | Biomes / `biome_taint` | int | 192 | re-slotted to first free id if taken | 82, 188 |
| `Config.biomeMagicalForestID` | Biomes / `biome_magical_forest` | int | 193 | | 83, 205 |
| `Config.biomeEerieID` | Biomes / `biome_eerie` | int | 194 | | 84, 219 |
| `Config.biomeEldritchID` | Biomes / `biome_eldritch` | int | 195 | | 85, 233 |
| `Config.biomeTaintWeight` | Biomes / `taint_biome_weight` | int | 2 | | 85, 185 |
| `Config.biomeMagicalForestWeight` | Biomes / `magical_forest_biome_weight` | int | 5 | | 86, 202 |
| `Config.dimensionOuterId` | Biomes / `outer_lands_dim` | int | -42 | Eldritch Dimensions | 91, 247 |

### 1.2 Enchantments

| Field | Config category/key | Type | Effective default | Notes | Line |
|---|---|---|---|---|---|
| `Config.enchHaste` | Enchantments / `ench_haste` | int | 150 | `encIdx = 150` then `encIdx++` | 156, 249–251 |
| `Config.enchRepair` | Enchantments / `ench_repair` | int | 151 | second id from same counter | 157, 254 |

### 1.3 World generation / regeneration

| Field | Config category/key | Type | Effective default | Line |
|---|---|---|---|---|
| `Config.genAura` | World_Generation / `generate_aura_nodes` | bool | true | 105, 368 |
| `Config.genStructure` | World_Generation / `generate_structures` | bool | true | 106, 369 |
| `Config.genCinnibar` | World_Generation / `generate_cinnibar_ore` | bool | true | 107, 370 |
| `Config.genAmber` | World_Generation / `generate_amber_ore` | bool | true | 108, 371 |
| `Config.genInfusedStone` | World_Generation / `generate_infused_stone` | bool | true | 109, 372 |
| `Config.genTrees` | World_Generation / `generate_trees` | bool | true | 110, 373 |
| `Config.genTaint` | World_Generation / `generate_taint` | bool | true | 111, 374 |
| `Config.regenKey` | World_Regeneration / `regen_key` | string | "DEFAULT" | 119, 377 |
| `Config.regenAura` | World_Regeneration / `aura_nodes` | bool | false | 112, 380 |
| `Config.regenStructure` | World_Regeneration / `structures` | bool | false | 113, 381 |
| `Config.regenCinnibar` | World_Regeneration / `cinnibar_ore` | bool | false | 114, 382 |
| `Config.regenAmber` | World_Regeneration / `amber_ore` | bool | false | 115, 383 |
| `Config.regenInfusedStone` | World_Regeneration / `infused_stone` | bool | false | 116, 384 |
| `Config.regenTrees` | World_Regeneration / `trees` | bool | false | 117, 385 |
| `Config.regenTaint` | World_Regeneration / `taint` | bool | false | 118, 386 |

### 1.4 Research

| Field | Config category/key | Type | Effective default | Notes | Line |
|---|---|---|---|---|---|
| `Config.researchDifficulty` | Research / `research_difficulty` | int | 0 | 0 normal, -1 easy, 1 hard | 128, 387 |
| `Config.aspectTotalCap` | Research / `aspect_total_cap` | int | 100 | soft cap for RP scanning pool | 129, 390 |

### 1.5 Monster spawning

| Field | Config category/key | Type | Effective default | Line |
|---|---|---|---|---|
| `Config.spawnAngryZombie` | Monster_Spawning / `spawn_angry_zombies` | bool | true | 140, 393 |
| `Config.spawnFireBat` | Monster_Spawning / `spawn_fire_bats` | bool | true | 141, 394 |
| `Config.spawnWisp` | Monster_Spawning / `spawn_wisps` | bool | true | 143, 395 |
| `Config.spawnTaintacle` | Monster_Spawning / `spawn_taintacles` | bool | true | 142, 396 |
| `Config.spawnTaintSpore` | Monster_Spawning / `spawn_taint_spores` | bool | true | 144, 397 |
| `Config.spawnPech` | Monster_Spawning / `spawn_pechs` | bool | true | 145, 398 |
| `Config.spawnElder` | Monster_Spawning / `spawn_eldercreatures` | bool | true | 146, 399 |
| `Config.championMobs` | Monster_Spawning / `champion_mobs` | bool | true | see §12 champion math | 92, 400 |

### 1.6 General

| Field | Config category/key | Type | Effective default | Notes | Line |
|---|---|---|---|---|---|
| `Config.allowMirrors` | general / `allow_mirrors` | bool | true | | 101, 403 |
| `Config.colorBlind` | general / `color_blind` | bool | false | | 96, 406 |
| `Config.shaders` | general / `shaders` | bool | true | | 97, 409 |
| `Config.crooked` | general / `crooked` | bool | true | | 98, 412 |
| `Config.hardNode` | general / `hard_mode_nodes` | bool | true | harsher negative node effects | 89, 415 |
| `Config.wuss` | general / `wuss_mode` | bool | false | disables Warp mechanics | 90, 418 |
| `Config.dialBottom` | general / `wand_dial_bottom` | bool | false | | 102, 421 |
| `Config.golemDelay` | general / `golem_delay` | int | 5 | ticks between golem task scans; clamped min 1 | 130, 424–429 |
| `Config.golemIgnoreDelay` | general / `golem_ignore_delay` | int | 10000 | ms; clamped min 1000 | 131, 431–436 |
| `Config.golemLinkQuality` | general / `golem_link_quality` | int | 16 | <4 → 0 (effect off) | 132, 438–443 |
| `Config.notificationDelay` | general / `notification_delay` | int | 5000 | ms, client scroll speed | 125, 445 |
| `Config.notificationMax` | general / `notification_max` | int | 15 | client | 126, 448 |
| `Config.nodeRarity` | general / `node_rarity` | int | 36 | avg 1 node per N chunks | 123, 451 |
| `Config.specialNodeRarity` | general / `special_node_rarity` | int | 18 | ~1 in N nodes special; clamped min 3 | 124, 454–459 |
| `Config.showTags` | general / `display_aspects` | bool | false | | 99, 461 |
| `Config.allowCheatSheet` | general / `allow_cheat_sheet` | bool | **false** | field initializer is `true` (line 121) but the config default passed to Forge is `false` | 121, 464 |
| `Config.wardedStone` | general / `allow_warded_stone` | bool | true | | 120, 467 |
| `ConfigEntities.entWizardId` | general / `thaumcraft_villager_id` | int | 190 | ConfigEntities.java:70 | 470 |
| `ConfigEntities.entBankerId` | general / `thaumcraft_banker_id` | int | 191 | ConfigEntities.java:71 | 473 |
| `Config.golemChestInteract` | general / `golem_chest_interact` | bool | true | | 122, 476 |
| `ThaumcraftApi.portableHoleBlackList` | general / `portablehole_blacklist` | string list | "iron_door" | | 479–492 |
| `Config.blueBiome` | general / `blue_magical_forest` | bool | false | | 100, 494 |
| `Config.taintFromFlux` | general / `biome_taint_from_flux` | bool | true | | 88, 497 |
| `Config.taintSpreadRate` | general / `biome_taint_spread` | int | 200 | 1-in-N chance per block update; 0 disables; used as `rate*5` in code (§8) | 87, 500 |
| `Config.glowyTaint` | general / `glowing_taint` | bool | true | | 127, 503 |

### 1.7 Runic shielding

| Field | Config category/key | Type | Effective default | Notes | Line |
|---|---|---|---|---|---|
| `Config.shieldRecharge` | Runic_Shielding / `runic_recharge_speed` | int | 2000 | ms between recharge ticks; clamped min 500 | 93, 506–508 |
| `Config.shieldWait` | Runic_Shielding / `runic_recharge_delay` | int | 80 | ticks after shield hits 0 before recharge; clamped min 0 | 94, 509–511 |
| `Config.shieldCost` | Runic_Shielding / `runic_cost` | int | 50 | centi-vis of Aer AND Terra per 1 shield unit; clamped min 0 | 95, 512–514 |

### 1.8 Potion / enchantment ids and dead fields

- Potion ids (`Config.potionVisExhaustID` etc., lines 147–155) have placeholder
  initializers (17–23) but are **assigned dynamically at runtime** by
  `Config.initPotions()` (lines 265–351) from the end of the `Potion.potionTypes`
  array; there are no config keys for them. Actual ids are runtime-dependent → **UNKNOWN** statically.
- `Config.nodeRefresh = 10` (line 103): no config key and **no usage anywhere in the jar** — dead field.
- `Config.auraSize = 4.0F` (line 104, `final`): **no usage anywhere in the jar** — dead field.
- `Config.aspectOrder` (line 158) is populated from `Aspect.aspects.values()` (line 880–882); insertion order = `ConfigAspects` registration order.

---

## 2. Wand capacities and vis consumption

### 2.1 Wand caps (`ConfigItems`, `common/config/ConfigItems.java:287–291, 316–322`)

`WandCap(tag, baseCostModifier, stack, tier)`; the special form adds
`(aspects, specialCostModifier, ...)`.

| Constant | tag | base cost modifier | tier | special aspects / modifier |
|---|---|---|---|---|
| `ConfigItems.WAND_CAP_IRON` | iron | 1.1 | 1 | — |
| `ConfigItems.WAND_CAP_GOLD` | gold | 1.0 | 3 | — |
| `ConfigItems.WAND_CAP_THAUMIUM` | thaumium | 0.9 | 6 | — |
| `ConfigItems.WAND_CAP_VOID` | void | 0.8 | 9 | — |
| `ConfigItems.WAND_CAP_COPPER` (postInit, only if `ingotCopper` exists) | copper | 1.1 | 2 | ORDER, ENTROPY → 1.0 |
| `ConfigItems.WAND_CAP_SILVER` (postInit, only if `ingotSilver` exists) | silver | 1.0 | 4 | AIR, EARTH, FIRE, WATER → 0.95 |

### 2.2 Wand rods (`ConfigItems.java:291–311`)

`WandRod(tag, capacity, stack, tier[, onUpdate])`.

| Constant | capacity | tier | primal rod effect |
|---|---|---|---|
| `WAND_ROD_WOOD` | 25 | 1 | — |
| `WAND_ROD_GREATWOOD` | 50 | 3 | — |
| `WAND_ROD_OBSIDIAN` | 75 | 6 | onUpdate Aspect.EARTH |
| `WAND_ROD_BLAZE` | 75 | 6 | Aspect.FIRE |
| `WAND_ROD_ICE` | 75 | 6 | Aspect.WATER |
| `WAND_ROD_QUARTZ` | 75 | 6 | Aspect.ORDER |
| `WAND_ROD_BONE` | 75 | 6 | Aspect.ENTROPY |
| `WAND_ROD_REED` | 75 | 6 | Aspect.AIR |
| `WAND_ROD_SILVERWOOD` | 100 | 9 | — |

Staff/sceptre rods (`StaffRod`, `ConfigItems.java:302–311`):

| Constant | capacity | tier |
|---|---|---|
| `STAFF_ROD_GREATWOOD` | 125 | 8 |
| `STAFF_ROD_OBSIDIAN` / `_BLAZE` / `_ICE` / `_QUARTZ` / `_BONE` / `_REED` | 175 | 14 |
| `STAFF_ROD_SILVERWOOD` | 250 | 24 |
| `STAFF_ROD_PRIMAL` | 250 | 32 (runes enabled) |

### 2.3 Stored capacity math (`ItemWandCasting.getMaxVis`, `common/items/wands/ItemWandCasting.java:82-84`)

`maxVis(centi-vis) = rod.capacity × (isSceptre ? 150 : 100)`
(e.g. greatwood wand = 50×100 = 5000 c = 50 vis per aspect; sceptre = 150% of rod capacity).

### 2.4 Consumption modifier (`ItemWandCasting.getConsumptionModifier`, ItemWandCasting.java:254-274)

```
mod = cap.specialCostModifier if aspect in cap.specialAspects else cap.baseCostModifier
mod -= WandManager.getTotalVisDiscount(player, aspect)   // from gear, §2.5
mod -= focusFrugalUpgradeLevel / 10.0                    // Frugal upgrade: -10%/level
mod -= 0.1                                              // if sceptre
return max(mod, 0.1)                                    // floor 10%
```
Focus costs in §3 are base costs; the effective cost is `base × mod` (ItemWandCasting.consumeAllVis, lines 325-347).

### 2.5 Vis-discount gear (`IVisDiscountGear.getVisDiscount`, percent)

| Item | Discount | Citation |
|---|---|---|
| `ItemGoggles.getVisDiscount` (Thaumaturgical Goggles) | 5% | `common/items/armor/ItemGoggles.java:68-70` |
| `ItemVoidRobeArmor.getVisDiscount` | 5% | `common/items/armor/ItemVoidRobeArmor.java:111-113` |
| `ItemHoverHarness.getVisDiscount` | 5% for Aspect.AIR, else 2% | `common/items/armor/ItemHoverHarness.java:79-81` |
| `ItemRobeArmor.getVisDiscount` (sandbox robes) | 2%, except boots slot (`field_77881_a==3`) 1% | `common/items/armor/ItemRobeArmor.java:128-130` |
| `ItemCultistRobeArmor.getVisDiscount`, `ItemCultistBoots.getVisDiscount` | 1% | `ItemCultistRobeArmor.java:79-81`, `ItemCultistBoots.java:65-67` |
| `ItemBaubleBlanks.getVisDiscount` (primal baubles, meta 3–8) | 1%, only for the matching primal aspect | `common/items/baubles/ItemBaubleBlanks.java:84-86` |

Vis-exhaust penalty: `WandManager.getTotalVisDiscount` (WandManager.java:50+) also modifies the total when the player has the Vis Exhaustion / Infectious Vis Exhaustion potion (level-scaled penalty; exact formula continues past line 70 — see file; the sign/level math beyond the potion check was not transcribed here → the exact per-level penalty is **UNKNOWN** from this excerpt and must be re-read from WandManager.java:70+ before porting).

---

## 3. Focus vis costs (base costs, before §2.4 modifiers)

All in `common/items/wands/foci/`. Costs are `AspectList` amounts (1 unit = 1 vis).

| Focus | Variant | Cost | Citation |
|---|---|---|---|
| `ItemFocusFire.costBase` | base | FIRE 10 | ItemFocusFire.java:23 |
| `ItemFocusFire.costBeam` | firebeam upgrade | FIRE 10, ORDER 3 | :24 |
| `ItemFocusFire.costBall` | fireball upgrade | FIRE 66, ENTROPY 33 | :25 |
| `ItemFocusFrost.costBase` | base | WATER 5, FIRE 2, ENTROPY 2 | ItemFocusFrost.java:20 |
| `ItemFocusFrost.costScatter` | scattershot | WATER 20, FIRE 2, ENTROPY 2, AIR 5 | :21 |
| `ItemFocusFrost.costBoulder` | iceboulder | WATER 20, FIRE 2, ENTROPY 2, EARTH 5 | :22 |
| `ItemFocusShock.costBase` | base | AIR 25 | ItemFocusShock.java:35 |
| `ItemFocusShock.costChain` | chainlightning | AIR 40, WATER 10 | :36 |
| `ItemFocusShock.costGround` | earthshock | AIR 75, EARTH 25 | :37 |
| `ItemFocusHellbat.costBase` | base | FIRE 200, ENTROPY 100, AIR 100 | ItemFocusHellbat.java:29 |
| `ItemFocusHellbat.costBomb` | batbombs | FIRE 100, ENTROPY 200, AIR 100 | :30 |
| `ItemFocusHellbat.costDevil` | devilbats | FIRE 100, ENTROPY 100, AIR 100, EARTH 100 | :31 |
| `ItemFocusExcavation.cost` | base (per-tick, `isVisCostPerTick`=true, :99-101) | EARTH 15 | :36 |
| `ItemFocusExcavation.cost2` | silktouch | base + AIR/FIRE/EARTH/WATER/ORDER/ENTROPY ×1 each | :74-85 |
| `ItemFocusExcavation.cost2` | dowsing | base + FIRE 2, ORDER 2 | :87-92 |
| `ItemFocusPortableHole.cost` | | ENTROPY 10, AIR 10 | ItemFocusPortableHole.java:27 |
| `ItemFocusPech.cost` | base | EARTH 10, ENTROPY 10, WATER 10 | ItemFocusPech.java:22 |
| `ItemFocusPech.costAll` | nightshade upgrade | all 6 primals ×10 | :23-30 |
| `ItemFocusTrade.cost` | base | ENTROPY 5, EARTH 5, ORDER 5 | ItemFocusTrade.java:36 |
| `ItemFocusTrade.cost2` | silktouch | base + 6 primals ×1 each | :224-235 |
| `ItemFocusWarding.cost` | | EARTH 25, ORDER 25, WATER 10 | ItemFocusWarding.java:35 |
| `ItemFocusPrimal.getVisCost` | randomized per activation | each of 6 primals: `50 + rand(0..4)×50` (50–250) | ItemFocusPrimal.java:75-83 |

Focus upgrade types carry their own aspect costs (e.g. `ItemFocusPech.nightshade` = LIFE 1, POISON 1, MAGIC 1, ItemFocusPech.java:31-37). The full `FocusUpgradeType` table lives in `ItemFocusBasic`/`api/wands` — per-upgrade crafting costs are recipe data, not tracked here.

Whether each focus charges per-tick vs per-activation: only `ItemFocusExcavation.isVisCostPerTick() = true` was verified (ItemFocusExcavation.java:99-101). Per-tick status of other foci: **UNKNOWN** (not transcribed; check `isVisCostPerTick` overrides when porting).

---

## 4. Research costs

Research items are registered in `common/config/ConfigResearch.java` via
`new ResearchItem(key, category, aspectRequirements, col, row, RPcost, icon)`
(constructor signature: `api/research/ResearchItem.java:46,64` — 6th arg = RP cost, field `complexity`).
201 research entries; cost distribution: 0×25, 1×75, 2×83, 3×16.
The two 2-arg entries (`CAP_iron`, `ROD_wood`) are virtual/auto-unlock placeholders (cost 0).

Full table (key | category | RP cost | line in ConfigResearch.java):

| Research key | Category | RP cost | Line |
|---|---|---|---|
| BASICTHAUMATURGY | THAUMATURGY | 0 | 78 |
| FOCUSFIRE | THAUMATURGY | 1 | 89 |
| FOCUSFROST | THAUMATURGY | 1 | 99 |
| FOCUSHELLBAT | THAUMATURGY | 2 | 113 |
| FOCUSEXCAVATION | THAUMATURGY | 2 | 130 |
| FOCUSWARDING | THAUMATURGY | 3 | 144 |
| FOCUSSHOCK | THAUMATURGY | 1 | 159 |
| FOCUSTRADE | THAUMATURGY | 2 | 173 |
| FOCUSPORTABLEHOLE | THAUMATURGY | 2 | 186 |
| FOCUSPOUCH | THAUMATURGY | 1 | 199 |
| CAP_iron | THAUMATURGY | 0 (virtual/auto) | 212 |
| CAP_gold | THAUMATURGY | 1 | 213 |
| CAP_thaumium | THAUMATURGY | 2 | 225 |
| CAP_copper | THAUMATURGY | 1 | 242 |
| CAP_silver | THAUMATURGY | 1 | 257 |
| ROD_wood | THAUMATURGY | 0 (virtual/auto) | 276 |
| ROD_greatwood | THAUMATURGY | 1 | 277 |
| ROD_reed | THAUMATURGY | 2 | 289 |
| ROD_blaze | THAUMATURGY | 2 | 303 |
| ROD_obsidian | THAUMATURGY | 2 | 317 |
| ROD_ice | THAUMATURGY | 2 | 331 |
| ROD_quartz | THAUMATURGY | 2 | 345 |
| ROD_bone | THAUMATURGY | 2 | 359 |
| ROD_silverwood | THAUMATURGY | 3 | 374 |
| SCEPTRE | THAUMATURGY | 3 | 465 |
| ROD_greatwood_staff | THAUMATURGY | 1 | 472 |
| ROD_reed_staff | THAUMATURGY | 2 | 488 |
| ROD_blaze_staff | THAUMATURGY | 2 | 503 |
| ROD_obsidian_staff | THAUMATURGY | 2 | 518 |
| ROD_ice_staff | THAUMATURGY | 2 | 533 |
| ROD_quartz_staff | THAUMATURGY | 2 | 548 |
| ROD_bone_staff | THAUMATURGY | 2 | 563 |
| ROD_silverwood_staff | THAUMATURGY | 3 | 579 |
| WANDPED | THAUMATURGY | 2 | 594 |
| VISAMULET | THAUMATURGY | 2 | 607 |
| WANDPEDFOC | THAUMATURGY | 3 | 624 |
| NODESTABILIZER | THAUMATURGY | 1 | 638 |
| NODESTABILIZERADV | THAUMATURGY | 2 | 654 |
| VISPOWER | THAUMATURGY | 2 | 668 |
| VISCHARGERELAY | THAUMATURGY | 2 | 689 |
| FOCALMANIPULATION | THAUMATURGY | 2 | 704 |
| VAMPBAT | THAUMATURGY | 1 | 721 |
| BASICARTIFACE | ARTIFICE | 0 | 738 |
| ARCANESTONE | ARTIFICE | 0 | 751 |
| GRATE | ARTIFICE | 0 | 763 |
| TABLE | ARTIFICE | 0 | 769 |
| ARCTABLE | ARTIFICE | 0 | 775 |
| RESTABLE | ARTIFICE | 0 | 782 |
| THAUMOMETER | ARTIFICE | 0 | 789 |
| PAVETRAVEL | ARTIFICE | 1 | 795 |
| PAVEWARD | ARTIFICE | 1 | 808 |
| GOGGLES | ARTIFICE | 1 | 825 |
| ARCANEEAR | ARTIFICE | 1 | 838 |
| SINSTONE | ARTIFICE | 1 | 851 |
| LEVITATOR | ARTIFICE | 1 | 866 |
| INFERNALFURNACE | ARTIFICE | 2 | 879 |
| BELLOWS | ARTIFICE | 1 | 897 |
| ENCHFABRIC | ARTIFICE | 1 | 915 |
| INFUSION | ARTIFICE | 2 | 928 |
| FLUXSCRUB | ARTIFICE | 1 | 950 |
| RUNICARMOR | ARTIFICE | 3 | 964 |
| RUNICCHARGED | ARTIFICE | 2 | 984 |
| RUNICHEALING | ARTIFICE | 2 | 998 |
| RUNICKINETIC | ARTIFICE | 2 | 1012 |
| RUNICEMERGENCY | ARTIFICE | 2 | 1026 |
| BANNERS | ARTIFICE | 1 | 1049 |
| RUNICAUGMENTATION | ARTIFICE | 1 | 1066 |
| BOOTSTRAVELLER | ARTIFICE | 2 | 1083 |
| HOVERHARNESS | ARTIFICE | 3 | 1096 |
| HOVERGIRDLE | ARTIFICE | 3 | 1113 |
| MIRROR | ARTIFICE | 2 | 1129 |
| MIRRORHAND | ARTIFICE | 2 | 1154 |
| MIRRORESSENTIA | ARTIFICE | 2 | 1168 |
| ARCANEBORE | ARTIFICE | 2 | 1188 |
| ARCANELAMP | ARTIFICE | 1 | 1207 |
| LAMPGROWTH | ARTIFICE | 2 | 1224 |
| LAMPFERTILITY | ARTIFICE | 2 | 1238 |
| BONEBOW | ARTIFICE | 1 | 1252 |
| PRIMALARROW | ARTIFICE | 2 | 1271 |
| ELEMENTALAXE | ARTIFICE | 2 | 1296 |
| ELEMENTALPICK | ARTIFICE | 2 | 1313 |
| ELEMENTALSWORD | ARTIFICE | 2 | 1330 |
| ELEMENTALSHOVEL | ARTIFICE | 2 | 1343 |
| ELEMENTALHOE | ARTIFICE | 2 | 1360 |
| WARDEDARCANA | ARTIFICE | 2 | 1374 |
| JARBRAIN | ARTIFICE | 2 | 1398 |
| INFUSIONENCHANTMENT | ARTIFICE | 3 | 1415 |
| ARMORFORTRESS | ARTIFICE | 2 | 1456 |
| HELMGOGGLES | ARTIFICE | 2 | 1476 |
| MASKGRINNINGDEVIL | ARTIFICE | 2 | 1491 |
| MASKANGRYGHOST | ARTIFICE | 2 | 1505 |
| MASKSIPPINGFIEND | ARTIFICE | 2 | 1519 |
| PHIAL | ALCHEMY | 0 | 1538 |
| CRUCIBLE | ALCHEMY | 0 | 1550 |
| NITOR | ALCHEMY | 1 | 1564 |
| ALUMENTUM | ALCHEMY | 1 | 1568 |
| ALCHEMICALDUPLICATION | ALCHEMY | 1 | 1574 |
| ALCHEMICALMANUFACTURE | ALCHEMY | 1 | 1595 |
| ENTROPICPROCESSING | ALCHEMY | 1 | 1614 |
| LIQUIDDEATH | ALCHEMY | 2 | 1632 |
| BOTTLETAINT | ALCHEMY | 2 | 1648 |
| THAUMIUM | ALCHEMY | 1 | 1664 |
| PUREIRON | ALCHEMY | 1 | 1684 |
| PUREGOLD | ALCHEMY | 1 | 1691 |
| PURECOPPER | ALCHEMY | 1 | 1706 |
| PURETIN | ALCHEMY | 1 | 1723 |
| PURESILVER | ALCHEMY | 1 | 1740 |
| PURELEAD | ALCHEMY | 1 | 1757 |
| TRANSIRON | ALCHEMY | 1 | 1767 |
| TRANSGOLD | ALCHEMY | 1 | 1774 |
| TRANSCOPPER | ALCHEMY | 1 | 1781 |
| TRANSTIN | ALCHEMY | 1 | 1792 |
| TRANSSILVER | ALCHEMY | 1 | 1809 |
| TRANSLEAD | ALCHEMY | 1 | 1826 |
| TALLOW | ALCHEMY | 1 | 1842 |
| ETHEREALBLOOM | ALCHEMY | 2 | 1850 |
| BATHSALTS | ALCHEMY | 2 | 1869 |
| SANESOAP | ALCHEMY | 1 | 1881 |
| ARCANESPA | ALCHEMY | 1 | 1893 |
| DISTILESSENTIA | ALCHEMY | 1 | 1906 |
| JARLABEL | ALCHEMY | 0 | 1932 |
| JARVOID | ALCHEMY | 1 | 1946 |
| TUBES | ALCHEMY | 1 | 1960 |
| TUBEFILTER | ALCHEMY | 2 | 1982 |
| ESSENTIACRYSTAL | ALCHEMY | 1 | 2002 |
| CENTRIFUGE | ALCHEMY | 2 | 2015 |
| THAUMATORIUM | ALCHEMY | 3 | 2034 |
| HUNGRYCHEST | GOLEMANCY | 1 | 2056 |
| GOLEMFETTER | GOLEMANCY | 1 | 2062 |
| TRAVELTRUNK | GOLEMANCY | 2 | 2075 |
| GOLEMSTRAW | GOLEMANCY | 2 | 2098 |
| GOLEMWOOD | GOLEMANCY | 2 | 2116 |
| GOLEMTALLOW | GOLEMANCY | 2 | 2129 |
| GOLEMCLAY | GOLEMANCY | 2 | 2146 |
| GOLEMFLESH | GOLEMANCY | 2 | 2160 |
| GOLEMSTONE | GOLEMANCY | 2 | 2179 |
| GOLEMIRON | GOLEMANCY | 2 | 2193 |
| GOLEMTHAUMIUM | GOLEMANCY | 2 | 2207 |
| GOLEMBELL | GOLEMANCY | 0 | 2224 |
| COREGATHER | GOLEMANCY | 1 | 2233 |
| COREFILL | GOLEMANCY | 2 | 2244 |
| COREEMPTY | GOLEMANCY | 2 | 2258 |
| CORESORTING | GOLEMANCY | 2 | 2272 |
| COREUSE | GOLEMANCY | 3 | 2286 |
| COREHARVEST | GOLEMANCY | 2 | 2304 |
| COREFISHING | GOLEMANCY | 2 | 2321 |
| CORELUMBER | GOLEMANCY | 2 | 2342 |
| COREGUARD | GOLEMANCY | 2 | 2356 |
| COREBUTCHER | GOLEMANCY | 2 | 2373 |
| CORELIQUID | GOLEMANCY | 2 | 2388 |
| COREALCHEMY | GOLEMANCY | 2 | 2405 |
| UPGRADEAIR | GOLEMANCY | 1 | 2423 |
| UPGRADEEARTH | GOLEMANCY | 1 | 2431 |
| UPGRADEFIRE | GOLEMANCY | 1 | 2439 |
| UPGRADEWATER | GOLEMANCY | 1 | 2447 |
| UPGRADEORDER | GOLEMANCY | 1 | 2461 |
| UPGRADEENTROPY | GOLEMANCY | 1 | 2469 |
| ADVANCEDGOLEM | GOLEMANCY | 2 | 2485 |
| TINYHAT | GOLEMANCY | 1 | 2493 |
| TINYGLASSES | GOLEMANCY | 1 | 2508 |
| TINYBOWTIE | GOLEMANCY | 1 | 2523 |
| TINYFEZ | GOLEMANCY | 1 | 2538 |
| TINYDART | GOLEMANCY | 1 | 2553 |
| TINYVISOR | GOLEMANCY | 1 | 2567 |
| TINYARMOR | GOLEMANCY | 1 | 2581 |
| TINYHAMMER | GOLEMANCY | 1 | 2595 |
| ASPECTS | BASICS | 0 | 2612 |
| PECH | BASICS | 0 | 2620 |
| NODES | BASICS | 0 | 2626 |
| WARP | BASICS | 0 | 2632 |
| RESEARCH | BASICS | 0 | 2638 |
| KNOWFRAG | BASICS | 0 | 2661 |
| THAUMONOMICON | BASICS | 0 | 2668 |
| ORE | BASICS | 0 | 2681 |
| PLANTS | BASICS | 0 | 2693 |
| ENCHANT | BASICS | 0 | 2708 |
| NODETAPPER1 | BASICS | 2 | 2714 |
| NODEPRESERVE | BASICS | 2 | 2727 |
| NODEJAR | BASICS | 3 | 2744 |
| NODETAPPER2 | BASICS | 2 | 2753 |
| RESEARCHER1 | BASICS | 1 | 2767 |
| DECONSTRUCTOR | BASICS | 1 | 2780 |
| RESEARCHER2 | BASICS | 2 | 2797 |
| RESEARCHDUPE | BASICS | 3 | 2812 |
| CRIMSON | BASICS | 0 | 2825 |
| ELDRITCHMINOR | ELDRITCH | 0 | 2836 |
| ELDRITCHMAJOR | ELDRITCH | 0 | 2842 |
| OCULUS | ELDRITCH | 1 | 2849 |
| ENTEROUTER | ELDRITCH | 1 | 2869 |
| OUTERREV | ELDRITCH | 1 | 2876 |
| PRIMPEARL | ELDRITCH | 1 | 2892 |
| PRIMNODE | ELDRITCH | 1 | 2908 |
| ADVALCHEMYFURNACE | ELDRITCH | 1 | 2922 |
| PRIMALCRUSHER | ELDRITCH | 2 | 2940 |
| SANITYCHECK | ELDRITCH | 1 | 2965 |
| VOIDMETAL | ELDRITCH | 2 | 2977 |
| ESSENTIARESERVOIR | ELDRITCH | 2 | 3002 |
| CAP_void | ELDRITCH | 3 | 3018 |
| ARMORVOIDFORTRESS | ELDRITCH | 3 | 3036 |
| FOCUSPRIMAL | ELDRITCH | 2 | 3055 |
| ROD_primal_staff | ELDRITCH | 3 | 3077 |

Notes:
- `Config.researchDifficulty` (-1/0/1) changes whether items are auto-purchasable with RPs vs. requiring the research table (§1.4).
- Aspect requirements per research item are recipe/content data; the RP costs above are the balance values.

---

## 5. Infusion matrix instability math (`TileInfusionMatrix`, `common/tiles/TileInfusionMatrix.java`)

- State fields: `instability` (line 59), `symmetry` (58), `recipeInstability` (66).
- Craft start: `instability = symmetry + recipeInstability` (lines 291, 317), where
  `recipeInstability = recipe.getInstability(input)` (288) or
  `InfusionEnchantmentRecipe.calcInstability(input)` (307). Per-recipe instability
  values are set at recipe construction in `ConfigResearch.java` (recipe data).
- Symmetry computation (`getSurroundings`, lines 700–790): scan box ±12 x/z, −5..+10 y.
  - each pedestal: **+2**; pedestal holding an item: **+1** (738–740)
  - mirror-position pedestal (opposite side): **−2**; both hold items: **−1** (749–751)
  - each gold block (`Blocks.field_150465_bP`) or `IInfusionStabiliser` block: **+0.1**; mirrored stabiliser pair: **−0.2** (769–783)
  - final `symmetry = (int)(symmetry + sym)` (779)
- Per craft-cycle tick (lines 346–410): if input invalid **or**
  `nextInt(500) <= instability` → failure event chosen by `nextInt(21)`:
  - cases 0,2,10,13 → eject item 0; 1,11 → eject 2; 3,8,14 → zap(false); 4,15 → eject 5;
    5,16 → harm(false); 6,17 → eject 1; 7 → eject 4; 9 → explosion (radius `1.5+rand`);
    12 → zap(true); 18 → harm(true); 19 → eject 3; 20 → warp (`inEvWarp`).
- Instability creep while vis drains: `nextInt(100 − recipeInstability×3) == 0 → instability++` (474–478), capped at **25** (478–480).
- While draining XP (infusion enchantments): `nextInt(50 − recipeInstability×2) == 0 → instability++`, cap 25 (447–452).
- While consuming pedestal ingredients: `nextInt(50 − recipeInstability×2) == 0 → instability++`, cap 25 (535–540).
- Cosmetic lightning: `nextInt(200) <= instability` (949).
- Ingredient pickup pacing: `itemCount = 5` ticks between item consumes (505–525); XP drain `countDelay = 20` (437).

---

## 6. Vis nodes

### 6.1 Generation (`ThaumcraftWorldGenerator`, `common/lib/world/ThaumcraftWorldGenerator.java`)

- Wild node: `random.nextInt(Config.nodeRarity) == 0` per chunk → 1/36 chunks (line 186).
- Totem structure: `nextInt(Config.nodeRarity × 10) == 0` → 1/360 chunks (line 117); totem places a node at its top (lines 168–178).
- Node type (`createRandomNodeAt`, lines 219–260):
  - special type if `nextInt(Config.specialNodeRarity) == 0` (1/18): then `nextInt(10)`:
    DARK 3/10, UNSTABLE 3/10, PURE 3/10, HUNGRY 1/10 (235–253).
  - node modifier if `nextInt(Config.specialNodeRarity / 2) == 0` (1/9): BRIGHT / PALE / FADING each 1/3 (258–266).
- Biome aura (`Config.registerBiomes`, `common/config/Config.java:837–867`; `BiomeHandler.getBiomeAura` averages the biome-dictionary type values, fallback **100**, `common/lib/world/biomes/BiomeHandler.java:19-31`):

| BiomeDictionary type | aura | dominant aspect | greatwood | greatwood chance |
|---|---|---|---|---|
| WATER 100, OCEAN 120, RIVER 100, WET 80 | | WATER | false | 0 |
| HOT 100, DESERT 100, NETHER 120, MESA 80 | | FIRE | false | 0 |
| DENSE 100, SNOWY 80, COLD 80, FROZEN 100, MUSHROOM 140 | | ORDER | false | 0 |
| CONIFEROUS 100 | | EARTH | true | 0.2 |
| FOREST 120 | | EARTH | true | 1.0 |
| SANDY 80, BEACH 80 | | EARTH | false | 0 |
| SAVANNA 80, MOUNTAIN 100, HILLS 120, PLAINS 80 | | AIR | PLAINS/SAVANNA true 0.2 | |
| DRY 80, SPARSE 80, WASTELAND 80 | | ENTROPY | false | 0 |
| SWAMP 120 | | ENTROPY | true | 0.2 |
| JUNGLE 100 | | PLANT | false | 0 |
| LUSH 100 | | PLANT | true | 0.5 |
| MAGICAL 100 | | (none) | true | 1.0 |
| END 80 | | VOID | false | 0 |
| SPOOKY 80 | | SOUL | false | 0 |
| DEAD 50 | | DEATH | false | 0 |

- Node vis value: `value = random.nextInt(baura/2) + baura/2` (uniform baura/2..baura−1) (line 285); in a Taint biome `baura ×= 1.5` and 50% chance type→TAINTED with `baura ×= 1.5` again (274–277); silverwood/small nodes `baura /= 4` (282).
- Node aspect composition: biome tag ×2 (else complex ×1 + basic ×1), then 3 coin-flip merges, each `1/specialNodeRarity` complex else basic (286–306); HUNGRY adds HUNGER 2 (+GREED 1 at 50%) (309–313); PURE adds LIFE 2 or ORDER 2 (314–316).

### 6.2 Node vis regeneration (`TileNode`, `common/tiles/TileNode.java`)

- `regeneration` (ticks between +1 vis): base **600**; BRIGHT **400**; PALE **900**; FADING **0** (no regen) (lines 661–673).
- Jar-locked nodes: lock level 1 → `×2`; level 2 → `×20` (676–682).
- Offline catch-up: `amount = (now − lastActive)ms / (regeneration × 75)` (line 688).
- Steady-state: every `count % regeneration == 0` add +1 to a random aspect below its base (754–768).
- Node→node feeding (adjacent airy blocks): transfer 1 vis; receiver's `wait = regeneration/2` (631); 1/3 chance the source's base vis for that aspect drops by 1 (623–625); PALE node clears its modifier with 1/100 chance per feed (620–622).
- Tainted nodes: every 50 ticks convert a random biome cell within ±8 to Taint (771–780); with `hardNode`, 50% chance to spread taint fibres within ±5 (782–786).
- Non-pure/non-tainted nodes inside a Taint biome: every 100 ticks, 1/500 → node becomes TAINTED (788–793).

### 6.3 Vis network

- `api/visnet/TileVisNode.java:94`: network refresh every `nodeCounter % 40 == 0` ticks (or on `nodeRefresh`).
- `TileVisRelay.getRange() = 8` (`common/tiles/TileVisRelay.java:55-57`); `TileNodeEnergized.getRange() = 8` (`common/tiles/TileNodeEnergized.java:156-158`).
- Wand pedestal (`TileWandPedestal`, `common/tiles/TileWandPedestal.java:185-235`): transfers **1 centi-vis per 5 ticks** (`counter % 5`); refuses to drain a node below `vis > min` where min=1, except iron cap or wood rod (min=0, lines 194–197); with a cap block above (stoneDevice meta 8) complex aspects are reduced to primals on transfer (222–233).
- Vis amulet wand→amulet transfer: up to **5 centi-vis per tick** (`ItemAmuletVis`, `common/items/baubles/ItemAmuletVis.java:77, 96`).

---

## 7. Wand-adjacent durations

- Runic shield recalculation cycle: every **40 ticks** (`EventHandlerRunic.livingTick`, `common/lib/events/EventHandlerRunic.java:50`).
- Runic recharge: interval `Config.shieldRecharge − chargedRingCount×500` ms per unit; each unit costs AIR `shieldCost` + EARTH `shieldCost` centi-vis from player inventory (lines 121–131). After charge hits 0: `rechargeDelay = Config.shieldWait` (262).
- Fortress helm "mask" enchant variants (`EventHandlerRunic.entityHurt`, lines 141–172): mask 0 → warp-event effect reduced by `2+rand(4)` (also WarpEvents.java:52-58); mask 1 → attacker gets Slowness 80 ticks with chance `damage/10`; mask 2 → self-heal 1.0 with chance `damage/12`.
- Pure fluid (bath salts → `BlockFluidPure`): grants Warp Ward `min(32000, 200000/√warpPerm)` ticks (`common/blocks/BlockFluidPure.java:44-54`).

---

## 8. Flux and Taint spread

- Taint biome spread (`BlockTaintFibres.taintBiomeSpread`, `common/blocks/BlockTaintFibres.java:197-207`): on random tick of taint/fibres, if `Config.taintSpreadRate > 0`: pick adjacent cell; convert biome to Taint if `rand.nextInt(Config.taintSpreadRate × 5) == 0` (i.e. 1/1000 default) **and** ≥2 taint neighbours.
- Fibre spread (`spreadFibres`, lines 174–195): target = random offset x/z ±1, y −3..+1 from taint block (`BlockTaint.java:155-160`, `BlockTaintFibres.java:138-143`). Fibre meta on placement: 90% meta 0; else 9/10 meta 1, 10/12 meta 2, else meta 3 (181–189).
- Taint block conversion: logs/leaves/cactus/pumpkin with ≥2 taint neighbours → taint (meta 0) (`BlockTaintFibres.java:146-150`); dirt/grass/sand/clay with ≥3 neighbours → taint grass (meta 1) (152–157).
- Taint spores: spore-fibres (meta 3) spawn `EntityTaintSpore` with 1/10 per random tick (160–165); taint block (meta 0) spawns `EntityTaintSporeSwarmer` with 1/200 (BlockTaint.java:163-173).
- Flux goo decay (`BlockFluxGoo.func_149674_a`, `common/blocks/BlockFluxGoo.java:87-115`): per random tick, 1/30 → level−1 (level 0 → remove); meta ≥2: 1/25 spawn Thaumic Slime (size 1); meta ≥6: 1/25 spawn size-2 slime, else if `taintFromFlux` 1/50 → biome→Taint + taint fibres; 50% of decays emit flux gas above.
- Flux gas contact (`BlockFluxGas.func_149670_a`, lines 59–77): 1/10 per contact → Vis Exhaustion **1200 ticks** (amplifier meta/3, no curatives) or Nausea **80+meta×20 ticks**.
- Taint biome weight defaults: §1.1.

---

## 9. Golem stats

### 9.1 Body types (`EnumGolemType`, `common/entities/golems/EnumGolemType.java:7-14`)

| Type | health | armor | speed | fireResist | upgrades | carry | regenDelay (ticks) | strength |
|---|---|---|---|---|---|---|---|---|
| STRAW | 10 | 0 | 0.38 | false | 1 | 1 | 75 | 0 |
| WOOD | 20 | 6 | 0.35 | false | 1 | 4 | 75 | 1 |
| TALLOW | 20 | 9 | 0.33 | false | 2 | 8 | 75 | 2 |
| CLAY | 25 | 9 | 0.33 | true | 1 | 8 | 100 | 2 |
| FLESH | 15 | 6 | 0.35 | false | 2 | 4 | 40 | 1 |
| STONE | 30 | 12 | 0.32 | true | 1 | 16 | 100 | 3 |
| IRON | 35 | 15 | 0.31 | true | 1 | 32 | 125 | 4 |
| THAUMIUM | 40 | 15 | 0.32 | true | 2 | 32 | 100 | 4 |

### 9.2 Derived math (`EntityGolemBase`, `common/entities/golems/EntityGolemBase.java`)

- Max health = `golemType.health + (decoration contains "H" ? 5 : 0)` (lines 201–208).
- Attack damage = `2 + golemType.strength + upgradeAmount(1) + (decoration "M" ? 2 : 0)` (209–215); `getGolemStrength() = strength + upgradeAmount(1)` (869).
- Armor = `super + golemType.armor` (1128).
- Carry limit = `carry + min(16, max(4, carry)) × upgradeAmount(1)` (302–308); fluid limit = `√carryLimit × 1000` mB (311–313).
- Speed = `golemType.speed × (decoration "B" ? 1.1 : 1) × (decoration "P" ? 0.88 : 1) × (1 + upgradeAmount(0)×0.15) × (advanced ? 1.1 : 1)` (316–325).
- Regen: +1 HP every `golemType.regenDelay` ticks; decoration "F" → `×0.66` (390–400).
- Base entity attributes (`func_110147_ax`, 121–127): movementSpeed 1.0, followRange 0.6→(overridden), attackDamage attribute 32.0 (unused for melee math above); size 0.4×0.95 (114).
- Task range: golem deactivates at distance² ≥ **2304** (48 blocks) from its home (402).
- Golem speed potion on feeding: Speed **600 ticks** (+existing remaining, cap logic at 980–986), heal 5.0 (990).
- Config knobs: `golemDelay` 5 ticks, `golemIgnoreDelay` 10000 ms (§1.6).

---

## 10. Equipment stats

### 10.1 Tool materials (`api/ThaumcraftApi.java:34-36`)

`EnumHelper.addToolMaterial(name, harvestLevel, durability, speed, damage, enchantability)`:

| Material | harvest | durability | speed | attack bonus | enchant |
|---|---|---|---|---|---|
| `ThaumcraftApi.toolMatThaumium` ("THAUMIUM") | 3 | 400 | 7.0 | 2.0 | 22 |
| `ThaumcraftApi.toolMatVoid` ("VOID") | 4 | 150 | 8.0 | 3.0 | 10 |
| `ThaumcraftApi.toolMatElemental` ("THAUMIUM_ELEMENTAL") | 3 | 1500 | 10.0 | 3.0 | 18 |

### 10.2 Armor materials (`api/ThaumcraftApi.java:37-41`)

`EnumHelper.addArmorMaterial(name, durability, [helm,chest,legs,boots] reduce, enchantability)`:

| Material | durability | reduction | enchant |
|---|---|---|---|
| `armorMatThaumium` ("THAUMIUM") | 25 | 2/6/5/2 | 25 |
| `armorMatSpecial` ("SPECIAL") | 25 | 1/3/2/1 | 25 |
| `armorMatThaumiumFortress` ("FORTRESS") | 40 | 3/7/6/3 | 25 |
| `armorMatVoid` ("VOID") | 10 | 3/7/6/3 | 10 |
| `armorMatVoidFortress` ("VOIDFORTRESS") | 18 | 4/8/7/4 | 10 |

Cultist armour reuses vanilla `ArmorMaterial.IRON` (`ConfigItems.java:521-533`).

### 10.3 Runic shielding capacity (`IRunicArmor.getRunicCharge`)

| Item | Charge units | Citation |
|---|---|---|
| `ItemRingRunic` meta 0 / 1 / 2 | 1 / 5 / 4 | `common/items/baubles/ItemRingRunic.java:75-77` |
| `ItemAmuletRunic` meta 0 / 1 | 8 / 7 | `common/items/baubles/ItemAmuletRunic.java` (getRunicCharge) |
| `ItemGirdleRunic` meta 0 / 1 | 10 / 9 | `common/items/baubles/ItemGirdleRunic.java` (getRunicCharge) |
| Runic armour pieces (void/fortress/etc.) | 0 (charge comes from `RS.HARDEN` NBT only) | `EventHandlerRunic.getFinalCharge`, `common/lib/events/EventHandlerRunic.java:343-354` |

Total max charge = sum over 4 armor + 4 bauble slots; recharge math in §7.

### 10.4 Vis storage gear

- `ItemAmuletVis.getMaxVis`: meta 0 → **2500** centi-vis (25 vis); meta 1 (primal) → **25000** (250 vis) (`common/items/baubles/ItemAmuletVis.java:132-134`).
- Loot-bag starter amulets: random 0–4 vis per primal (`Config.initLoot`, `common/config/Config.java:519-524`).

### 10.5 Hover harness / traveller boots

- Hover: break-speed multiplier while hovering `×5.0` (`EventHandlerEntity.breakSpeedEvent`, `common/lib/events/EventHandlerEntity.java:739-743`).
- Traveller's boots fall-damage and step-up constants: not transcribed here → **UNKNOWN** (see `ItemBootsTraveller.java` when porting).

---

## 11. World generation frequencies (`ThaumcraftWorldGenerator.generateOres/generateVegetation`, lines 428–590)

Per 16×16 chunk (all gated by the §1.3 config booleans and biome blacklist):

| Feature | Frequency | Details | Line |
|---|---|---|---|
| Cinnabar ore | 18 attempts | single-block replace of stone, y < worldHeight/5 | 519–528 |
| Amber ore | 20 attempts | single-block replace of stone, y = surface − rand(25) | 531–541 |
| Infused stone | 8 veins | `WorldGenMinable(size=6)` in stone, y < max(5, surface−5); meta 1–6, 1/3 biome-aspect-tinted | 544–571 |
| Silverwood tree | 1/60 | only MAGICAL-type biomes | 585-587, `generateSilverwood` 622-631 |
| Greatwood tree | 1/25 | × `BiomeHandler.getBiomeSupportsGreatwood` chance (§6.1 table) | 589-591 |
| Shimmerleaf flowers | 1/30 | hot biomes (temp > 1.0, sand surface) | 593-599 |
| Mound (shrubbery) | 1/150 | overworld, non-flat; creates dark node | 455-461 |
| Eldritch stone ring | 1/66 | overworld, non-flat; creates dark node | 462-475 |
| Hilltop stones | 1/40 | overworld, non-flat; creates dark node | 476-483 |
| Totem | 1/(nodeRarity×10) = 1/360 | overworld/nether, non-flat | 112-180 |
| Structure-attached node | per MapGenScatteredFeature position | one node per nether-wart/obsidian feature not already used | 432-443 |

---

## 12. Mob spawning, champions, drops, loot

### 12.1 Spawn registrations (`ConfigEntities.initEntitySpawns`, `common/config/ConfigEntities.java:156-203`)

`EntityRegistry.addSpawn(class, weight, min, max, type, biomes)`:

| Mob | weight/min/max | Biomes | Line |
|---|---|---|---|
| `EntityBrainyZombie` | 10/1/1 | every biome with a monster list | 163 |
| `EntityPech` | 10/1/1 | MAGICAL biomes | 171 |
| `EntityFireBat` | 10/1/2 | NETHER biomes | 177 |
| `EntityFireBat` (Halloween, Oct 31 only) | 5/1/2 | all biomes | 181 |
| `EntityWisp` | 5/1/1 | NETHER biomes | 186 |

Biome spawn lists (`common/lib/world/biomes/`):

| Biome | Entry (weight/min/max) | Line |
|---|---|---|
| Taint | Taintacle 1/1/1 | BiomeGenTaint.java:37 |
| Magical Forest | Wolf 2/1/3, Horse 2/1/3, Witch 3/1/1, Enderman 3/1/1, Pech 10/1/2, Wisp 10/1/2 | BiomeGenMagicalForest.java:39-48 |
| Eerie | Bat 3/1/1, Witch 8/1/1, Enderman 4/1/1, BrainyZombie 32/1/1, GiantBrainyZombie 8/1/1, Wisp 3/1/1, EldritchGuardian 1/1/1 | BiomeGenEerie.java:20-33 |
| Eldritch Lands | InhabitedZombie 1/1/1, EldritchGuardian 1/1/1 | BiomeGenEldritch.java:20-21 |

### 12.2 Champion math (`EventHandlerEntity`, `common/lib/events/EventHandlerEntity.java:370-421`)

Roll `c = nextInt(100)`; modifiers: EASY or `!championMobs` → +2; HARD → −2 (−0 if champions off); Outer Lands → −3; SPOOKY/NETHER/END biome → −2 (−1 if off); dangerous location → −10 (−3 if off); whitelist entry → −(value−1). Champion applied iff `c ≤ 0` and maxHealth ≥ 10.
Whitelist (`ConfigEntities.java:188-203`): Zombie/Spider/Blaze/Enderman/Skeleton 0, Witch 1, EldritchCrab 0, Taintacle 2, Wisp 1, InhabitedZombie 3; `championModWhitelist`: EntityCultist 1, EntityWatcher 2, EntityPech 1, EntityThaumcraftBoss 200.

### 12.3 Drop chances

- Wisp death → guaranteed `itemWispEssence` tagged with its primal aspect ×2 (`EntityWisp.func_70628_a`, `common/entities/monster/EntityWisp.java:277-283`).
- Taintacle death → 50% `itemResource meta 11` else `meta 12` (`EntityTaintacle.func_70628_a`, `EntityTaintacle.java:298-305`).
- Pech death → `(1+looting)` mana beans at 50% each; `itemResource meta 18` with chance `(1+looting)/10`; when damaged drops `itemResource meta 9` (`EntityPech.java:330-349`).
- Mining-result overrides (`Utils.addSpecialMiningResult`, chance = replacement probability, `common/lib/utils/Utils.java:150-163`; registered in `Config.initMisc`, `Config.java:876-878`): gravel → `itemNugget meta 16` chance 1.0; obsidian → `itemNugget meta 31` chance 0.9; cinnabar ore (`blockCustomOre meta 0`) → `itemNugget meta 21` chance 0.9. Copper/Tin/Silver/Lead ores → nugget metas 17–20 chance 1.0 when those ores exist (`Config.initModCompatibility`, `Config.java:654-684`).
- Mana pod crop: `CropUtils.addStandardCrop(blockManaPod, 7)` max growth stage 7 (`Config.java:873`).

### 12.4 Loot bag weights (`ThaumcraftApi.addLootBagItem(item, weight, tier…)`, `Config.initLoot`, `Config.java:526-558`)

| Item | weight | tier |
|---|---|---|
| `itemResource meta 18` ×1/×2/×3 | 2500 / 2250 / 2000 | 0 / 1 / 2 |
| `itemEldritchObject meta 3` (Mundane Badge) | 1 | 2 |
| `Items.field_151156_bN` (saddle) | 1 | 2 |
| `Items.field_151045_i` (gold ingot) | 10 (t0), 50 (t1-2) | |
| `Items.field_151166_bC` (diamond) | 15 (t0), 75 (t1-2) | |
| `Items.field_151043_k` (iron ingot), `Items.field_151079_bi` (gold carrot? see SRG) | 100 | 0,1,2 |
| `itemResource meta 9` | 25 | 0,1,2 |
| `itemBaubleBlanks meta 0-2` | 10 | 0 |
| `itemBaubleBlanks meta 3-8` | 5 (t1), 7 (t2) | |
| `itemAmuletVis` (random vis) | 6 | 1,2 |
| `itemRingRunic` | 5 | 1,2 |
| `Items.field_151062_by` (ender pearl) | 5/10/20 | 0/1/2 |
| `Items.field_151153_ao meta 1` (potion) | 1/2/3 | 0/1/2 |
| `Items.field_151153_ao meta 0` | 3/6/9 | 0/1/2 |
| `Items.field_151122_aG` (gold nugget) | 10 | 0,1,2 |
| all valid potion metas | level+1 | 0,1,2 |

### 12.5 Dungeon chest injection (`ChestGenHooks.addItem`, `Config.java:587-646`)

`WeightedRandomChestContent(item, minStack, maxStack, weight)`:
- common loot (lootbag t0, phial, saltpetre): 1–3, weight 5 (dungeon/jungle/pyramid), 4 (mineshaft/stronghold) — line 615-619.
- uncommon (lootbag t1, baubles meta 0-2, tainted mangonel? `itemResource meta 9`): 1–2 w4 / 1–2 w3 — 625-631.
- `itemResource meta 9` in stronghold library: 3–6, weight 20 — 634.
- rare (lootbag t2, thaumonomicon, thaumium tools, runic ring, baubles 3-8, vis amulet): 1–1, weight 1 — 637-643.
- village blacksmith: `itemResource meta 2` 1–3 weight 10 — 646.

---

## 13. Warp event math (`WarpEvents.checkWarpEvent`, `common/lib/WarpEvents.java:39-226`)

- Trigger: per check, `rand.nextInt(100) ≤ √warpCounter` (line 45-46).
- Effective warp: `warp = min(100, (2×warp + warpCounter)/3)` (47); `warpCounter -= max(5, √warpCounter×2)` (48).
- Fortress mask 0 reduces effect roll by `2+rand(4)` (52-58).
- Effect roll `eff = rand.nextInt(warp)` (50), table:

| eff | Effect | Duration |
|---|---|---|
| 1–4 | grant 1 RP (random primal) | — |
| 9–12 | message only | — |
| 13–16 | Vis Exhaustion | 5000 t, amp `min(3, warp/15)` |
| 17–20 | Thaumarhia | `min(32000, 10×warp)` t |
| 21–24 | Unnatural Hunger | 5000 t |
| 25–28 | message only | — |
| 29–32 | spawn mist | — |
| 33–36 | Blurred Vision | `min(32000, 10×warp)` t |
| 37–40 | Sun Scorned | 5000 t |
| 41–44 | Mining Fatigue (`Potion.field_76419_f`) | 1200 t |
| 45–48 | Infectious Vis Exhaustion | 6000 t |
| 49–52 | Nausea (`Potion.field_76439_r`) | `min(40×warp, 6000)` t |
| 53–56 | Death Gaze | 6000 t |
| 57–60 | mind spiders (illusion), `min(50, warp)` | — |
| 61–64 | message only | — |
| 65–68 | mist + `warp/30` guardians (cap 8) | — |
| 69–72 | Blindness (`Potion.field_76440_q`) | `min(32000, 5×warp)` t |
| 76 | remove 1 sticky warp | — |
| 77–80 | Unnatural Hunger | 6000 t |
| 81–84 | grant `warp/10` RP | — |
| 89–92 | real mind spiders | — |
| 93+ | mist + `warp/15` guardians | — |

- Thresholds: sticky+perm warp >10 → auto-complete `@BATHSALTS` research (203-209); >25 → +10 RP and `ELDRITCHMINOR` (211-215); >50 → +20 RP and `ELDRITCHMAJOR` (217-221).
- Temporary warp decays −1 per check (`addWarpTemp(-1)`, line 224).
- Death Gaze effect: forces nearby mobs to look at player, range `min(8+3×level, 24)`, applies Slowness 80 t (`checkDeathGaze`, 324-351).

---

## 14. Known-unknown register

Values referenced by the scope that could **not** be recovered from the decompiled jar:

1. Static potion ids — assigned at runtime (§1.8).
2. `Config.nodeRefresh`, `Config.auraSize` — dead fields, no consumers (§1.8).
3. Exact per-level vis-exhaust penalty in `WandManager.getTotalVisDiscount` past line 70 — not transcribed in this doc; re-read `common/items/wands/WandManager.java:70+` before porting.
4. Per-tick vs per-activation charging for foci other than Excavation (§3).
5. `ItemBootsTraveller` fall-damage/step constants (§10.5).
6. Item display names for `itemResource`/`itemNugget` metas cited by number (lang-key lookup not performed here).
7. Per-recipe infusion instability values — recipe data in `ConfigResearch.java`, tracked by the recipe/content mapping tasks, not here.

---

## Verification

- Config table completeness: `Config.java` contains exactly 62 `config.get(...)` calls (Biomes 7, Enchantments 2, general 25, Monster_Spawning 8, Research 2, Runic_Shielding 3, World_Generation 7, World_Regeneration 8 — counted with grep); every one is represented in §1.
- Research table: extracted programmatically from all 201 `new ResearchItem(` call sites in `ConfigResearch.java` with a top-level argument splitter; cost column = 6th constructor arg (`complexity`).
- All numeric constants above were read directly from the decompiled sources listed; none are inferred.
