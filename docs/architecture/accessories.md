# Accessory layer (TC-M8-01, decision D-005)

The accessory layer is the port of Thaumcraft 4's bauble semantics to
NeoForge 1.21.1. Per D-005 it is **provider-independent**: core gameplay
code talks to one API (`AccessoryService`), and exactly one backend is
active at runtime — an optional **Curios 9.5.1 bridge** when Curios is
installed (the ATM10 target environment), or a **built-in fallback**
(NeoForge player attachments) when it is not. Both backends enforce the
same Baubles-spec lifecycle contract, so content milestones (TC-M8-02 vis
discount gear, TC-M6-07 runic shielding) behave identically either way.

## Source semantics (what we port)

Citations are against the decompiled reference tree
(`reference/decompiled/`) and the Baubles API repo
(`reference/api-repos/baubles-api/`), MIT, adapted per
`docs/licensing-and-attribution.md`.

| Original | Behavior ported |
| --- | --- |
| `baubles.api.IBauble` (Baubles 1.0.1.10 API) | `getBaubleType` → `AccessoryItem.applicableSlots(ItemStack)`; `onWornTick` / `onEquipped` / `onUnequipped` / `canEquip` / `canUnequip` kept method-for-method with `EntityLivingBase` → `Player` (TC4 baubles are player-only). |
| `baubles.inventory.InventoryBaubles` | 4-slot inventory: index 0 = AMULET, 1–2 = RING, 3 = BELT. `isValidItem` required the item to implement `IBauble` **and** its `BaubleType` to match the target slot; `BaubleType.ALL` was accepted by every slot. Ported as `AccessoryRules.slotAccepts` (an "ALL" item returns the full slot set from `applicableSlots`). |
| `BaubleType` enum | `AccessorySlot` enum: `RING` (capacity 2), `AMULET` (1), `BELT` (1), plus `CHARM` (1) — the single extension slot. TC4's own code never used a fifth slot; CHARM exists because the D-005 scope and the Curios "charm" preset semantics fix it as the generic thaumaturgical charm slot. |
| `BaublesEventHandler` / `PlayerHandler` | Tick driving of `onWornTick` once per worn stack per server tick (`AccessoryLifecycle.onPlayerTick` under the fallback; Curios' own update loop under the bridge). |

## Slot model (frozen contract)

`AccessorySlot` is the single source of truth. These values are
save-compatibility: **never renumber or rename them once shipped.**

| Slot | Capacity | Fallback payload index | Curios slot type id | Curios item tag |
| --- | --- | --- | --- | --- |
| `RING` | 2 | 0, 1 | `thaumcraft.ring` | `curios:thaumcraft.ring` |
| `AMULET` | 1 | 2 | `thaumcraft.amulet` | `curios:thaumcraft.amulet` |
| `BELT` | 1 | 3 | `thaumcraft.belt` | `curios:thaumcraft.belt` |
| `CHARM` | 1 | 4 | `thaumcraft.charm` | `curios:thaumcraft.charm` |

The fallback payload codec field names (`ring_1`, `ring_2`, `amulet`,
`belt`, `charm`) are the same contract. `CuriosSlotDataTest` keeps the
shipped datapack files and `AccessorySlot` consistent;
`AccessorySlotModelTest` pins the numbers.

Curios 9.5.1 registers slot types **from datapack JSON**, so the bridge's
slot registration lives in `src/main/resources/data/`:

- `curios/curios/slots/thaumcraft.<slot>.json` — one per slot, `size`
  matching `AccessorySlot.capacity()`. Verified against the 9.5.1 jar:
  `CuriosSlotManager.apply` only accepts slot files whose **pack namespace
  is `curios`** and derives the slot id from the bare file name — files
  under `data/thaumcraft/curios/slots/` are silently skipped.
- `thaumcraft/curios/entities/baubles.json` — grants exactly our four
  slots to `minecraft:player` (entity grants load from any namespace).
- `curios/tags/item/thaumcraft.<slot>.json` — item routing (the tag is
  `curios:<slot id>` per `CuriosTags.createItemTag`; a missing tag means
  nothing can be worn there).

## Provider SPI

`AccessoryProvider` (package `accessory`) is the internal SPI:
`id`, `canEquip`, `equip`, `unequip`, `worn`, `tickWorn`. Contracts every
provider must satisfy:

- **Server authority** — mutations only on the server.
- `canEquip` implements the full insertion check (slot routing + capacity +
  occupancy + item-side `canEquip` veto) without mutating.
- `equip`/`unequip` fire the `AccessoryItem` lifecycle callbacks **exactly
  once** per insert/removal — synchronously in the fallback; via Curios' own
  update loop under the bridge (the bridge must not fire them itself or
  every callback would double).
- `worn` reflects the current worn set (never contains empties).
- `tickWorn` drives `onWornTick` exactly once per worn stack per tick —
  except the Curios bridge, where Curios' native `curioTick` drives it and
  `tickWorn` is a no-op (double-tick guard).

`AccessoryService.init(modEventBus)` runs once from the mod constructor and
picks the provider: `ModList.isLoaded("curios")` → `CuriosBridge.install`,
otherwise the fallback. Gameplay code only ever calls `AccessoryService`
(`worn`, `wornStacks`, `isWorn`, `findWorn`, `findAllWorn`).

## Isolation rule (guarded)

`accessory/curios` is the **only** package that may reference
`top.theillusivec4.curios` types, and `CuriosBridge` is class-loaded only
on the Curios branch — a no-Curios runtime never touches a Curios type.
`CuriosBridgeIsolationTest` enforces this on compiled bytecode: every
`.class` outside the bridge package must not contain the constant-pool
string `top/theillusivec4/curios` (a byte scan catches every reference
form, including synthetic ones a source grep would miss).

Items never implement `ICurioItem` either: `AccessoryCurioItem` is a
separate adapter object attached via `CuriosApi.registerCurio(Item,
ICurioItem)` at common-setup (iterating `ModItems.ITEMS` entries whose
item implements `AccessoryItem`). Core item classes stay Curios-free.

Adapter mapping (signatures verified with `javap` against the 9.5.1 jar):

| Curios (`ICurioItem`) | Our side |
| --- | --- |
| `canEquip(SlotContext, ItemStack)` | player check + `AccessoryRules.slotAccepts` + `AccessoryItem.canEquip` |
| `canUnequip(SlotContext, ItemStack)` | player check + `AccessoryItem.canUnequip` |
| `onEquip(SlotContext, ItemStack, ItemStack)` | `AccessoryItem.onEquipped` |
| `onUnequip(SlotContext, ItemStack, ItemStack)` | `AccessoryItem.onUnequipped` |
| `curioTick(SlotContext, ItemStack)` | `AccessoryItem.onWornTick` |

`SlotContext.identifier()` is mapped back through
`AccessorySlot.byCuriosSlotId`; unknown slot ids fail validation.

## Fallback provider

A compatibility shim, not a second framework: fixed 5-slot payload
(`AccessoryData`, lenient codec — every field optional, wrong-size
payloads normalized, never a save-bricking throw) stored in the player
attachment `thaumcraft:accessories` (`AccessoryAttachments`). Equipping is
right-click driven (`AccessoryLifecycle.onRightClickItem` — insert the
first free valid slot); there is deliberately **no fallback GUI** (D-005:
the Curios accessory inventory UI is the UI when present; the fallback only
needs to keep TC4 gear functional standalone).

`AccessoryLifecycle` (game events, fallback-active guarded) owns:

- **tick driving** — `PlayerTickEvent.Post` → `provider.tickWorn`;
- **right-click equip** — accessory item + empty target slot → equip;
- **death handling, keepInventory-aware**:
  - `keepInventory=false`: `LivingDropsEvent` adds the worn stacks to the
    death drops and clears the attachment (no `onUnequipped` — matching
    `InventoryBaubles`, which drops without lifecycle callbacks on death);
  - `keepInventory=true`: `PlayerEvent.Clone` (death) copies the payload to
    the respawned player.

The attachment is deliberately **not** `copyOnDeath`: death drops already
move the stacks, and a copy-on-death attachment would duplicate them.
Persistence across disconnect/reconnect rides the normal player-NBT
attachment path (same mechanism as TC-M2-02 warp data).

## Curios bridge

`CuriosProvider` implements the SPI over `CuriosApi.getCuriosInventory`:
`equip` → `setEquippedCurio`, `unequip` → `removeCurio`, `worn` →
`getCurios()` handlers filtered to our four slot ids, `tickWorn` → no-op
(Curios drives `curioTick`). Validation still runs through
`AccessoryRules` first so both providers reject the same insertions;
Curios' own tag check (`curios:thaumcraft.<slot>` item tags) is kept
consistent by shipping those tags for our items.

Death/persistence under the bridge is **Curios-native** (its drop rules
and its own clone handling) — the fallback lifecycle handlers stay inert,
which is what prevents double-equip, double-drop, and duplicate state.

## Testing

- Unit (`src/test/.../accessory/`): slot model + frozen indices
  (`AccessorySlotModelTest`), payload codec leniency and round-trip
  (`AccessoryDataCodecTest`), TC4 validation semantics
  (`AccessoryRulesTest`), adapter negative paths
  (`accessory/curios/AccessoryCurioItemTest`), bytecode isolation
  (`CuriosBridgeIsolationTest`), data-file/slot-model consistency
  (`CuriosSlotDataTest`).
- In-game (`AccessoryGameTest`, registered in `registerGameTests`): runs
  under **both** configurations — CI default (Curios on the runtime
  classpath) and the `game-test-no-curios` job (`-Ptc.noCurios`). It
  asserts provider selection matches the environment, the shared
  equip/worn/unequip contract, capacity + routing + item-veto rejection,
  save/load survival, keepInventory-aware death behavior, and (per config)
  fallback exactly-once callbacks or Curios slot-type registration.
- Probe content: `TestProbeContent` ring / amulet / always-veto ring with
  lifecycle counters, gated by `-Dthaumcraft.testprobe=true` like the
  other probe content.

## Follow-ups

- TC-M8-02 (vis discount gear) and TC-M6-07 (runic shielding) implement
  `AccessoryItem` on their items and consume `AccessoryService` queries —
  no provider code changes expected.
- ATM10 integration battery (TC-M9-01) exercises the bridge against real
  Curios UIs and third-party slot grants.
