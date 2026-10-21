# Vis system core (TC-M4-02)

The port of Thaumcraft 4.2.3.5's vis economy plumbing: wand/vis-container
charge storage, deposit/withdraw semantics, vis cost application rules, the
vis network container model (node→conduit→device), and the read-side contract
for per-chunk ambient vis. The wand/staff items themselves, the vis-node
tiles, and the discount gear arrive with TC-M6-01/TC-M5-01/TC-M8-02; this
milestone lands the rules those features will call, as pure operations that
are unit-testable without item or tile classes.

Code lives in `com.jonnyutah12.thaumcraftclassic.wands`,
`...visnet`, and one reader in `...world`:

| File | Role | Original counterpart |
|---|---|---|
| `wands/VisPool` | immutable charge payload of the `thaumcraft:vis_pool` data component (aspect tag → centi-vis) | per-aspect `NBTTagInt` keys written by `ItemWandCasting.storeVis/getVis` (`common/items/wands/ItemWandCasting.java:238-249`) |
| `wands/WandVis` | pure pool operations: capacity math, deposit with clamp+leftover, all-or-nothing withdrawal, primal enumeration | `ItemWandCasting` `getMaxVis`/`addVis`/`addRealVis`/`consumeVis`/`consumeAllVis(Crafting)`/`getAllVis`/`getAspectsWithRoom` (lines 82-84, 206-229, 305-375) |
| `wands/VisCostRules` | consumption-modifier chain + truncating cost cast, parameterized over cap profile and discount source | `ItemWandCasting.getConsumptionModifier` (254-274), cost loop of `consumeAllVis` (327-331), `WandManager.getTotalVisDiscount` (`common/items/wands/WandManager.java:50-84`) |
| `visnet/IVisNode` | container contract for network members | `api/visnet/IVisNode` |
| `visnet/VisNodeBase` | shared plumbing: parent-chain consumption, subtree removal, the 40-tick network pass | `api/visnet/TileVisNode` (non-abstract body, lines 31-152) |
| `visnet/VisNetHandler` | per-dimension network forest: source registry, attachment selection, drainer lookup | `api/visnet/VisNetHandler` |
| `visnet/VisNetworkHooks` | clears static networks on `ServerStoppedEvent` | (implicit: original relied on `WeakReference` fade-out) |
| `world/AmbientVis` | per-chunk ambient-vis reader over the M2-02 aura fields | no runtime counterpart in 4.2.3.5 (see below) |
| `registration/ModDataComponents.VIS_POOL` | component registration (persistent + synced) | — |

## Storage: data component, centi-vis, derived capacity

The original stored charge as one `NBTTagInt` per primal aspect tag directly
on the stack (`storeVis`, lines 238-249), in centi-vis — 1/100 of one vis
unit, the original's internal integer unit (`* 100` at line 314/355). The
port keeps the unit exactly and moves the payload into the typed
`thaumcraft:vis_pool` component (`VisPool`, a normalized map that drops
zero entries; absent and 0 read identically on both sides).

Capacity is **not** stored. The original derived it on every read from the
equipped rod: `maxVis = rod.capacity × (isSceptre ? 150 : 100)`
(`getMaxVis`, line 82-84; rod capacity table in `docs/balance-values.md`
§2.2-2.3). `WandVis.maxVis(rodCapacity, sceptre)` ports that math; the rod
and cap components arrive with TC-M6-01 and will feed it.

## Deposit and withdrawal rules (parity notes, all pinned by tests)

- **Deposit clamps and reports the leftover.** `addVis` computes
  `current + amount×100`, clamps to capacity, and returns the overflow back
  in real vis (`leftover/100`, lines 352-363); `addRealVis` is the
  centi-vis form (365-375). Non-primal aspects are refused with leftover 0
  (line 353).
- **Withdrawal is all-or-nothing.** `consumeVis` refuses (returns null —
  the original's `false`) unless the full amount is covered;
  `consumeAllVis` checks every aspect before subtracting any
  (lines 325-347), so a partially-affordable price never partially pays.
- **Crafting prices scale by 100.** Recipe costs are whole vis, storage is
  centi-vis: `consumeAllVisCrafting` multiplies by 100 before paying
  (line 314).
- **The primal row.** `getAllVis` returns all six primal aspects including
  zero entries (206-218) and `getAspectsWithRoom` marks fillable ones with
  amount 1 (220-229) — charging devices iterate these, so the zero rows are
  part of the contract.

## Cost application rules

Effective cost of one action is `(int)(base × modifier)` — the original's
truncating int cast (lines 306, 330); float arithmetic is preserved
term-for-term, including the residual above the 10% floor. The modifier
chain (`getConsumptionModifier`, lines 254-274):

1. cap profile: `specialCostModifier` if the aspect is in the cap's special
   set, else `baseCostModifier` (values: `docs/balance-values.md` §2.1 —
   iron 1.1, gold 1.0, thaumium 0.9, void 0.8; copper/silver specials);
2. minus the worn-gear discount (`VisDiscountSource`, the already-summed
   fraction; TC-M8-02 wires goggles/robes/harness and the Vis Exhaustion
   penalty `-(max(amplifier)+1)×10%` from `WandManager.java:50-84`);
3. minus `frugal/10` per Frugal focus upgrade, skipped while crafting
   (lines 266-268);
4. minus 0.1 for sceptres (lines 270-272);
5. floored at 0.1 (line 273).

The engine takes a `WandCapCost` record and a `VisDiscountSource` function
instead of item classes, so the TC-M0-06 tables are testable now and the
item cards just supply the inputs.

## Vis network container model

The network is a per-dimension forest (`VisNetHandler`): vis node sources
are roots; relays/conduits and devices attach to the closest eligible
member within their own range; consumption walks parent edges up to the
owning source (`VisNodeBase.consumeVis`, `TileVisNode.java:31-40`). The
selection algorithm is preserved including its quirks, each cited:

- **Root preference with mixed units.** Attachment candidates key roots at
  `r − targetRange×2` but descendants at raw `r` (squared distances)
  (`VisNetHandler.java:66-72`) — deliberately root-biased inside a tree;
  pinned by `rootIsPreferredOverItsOwnNearerDescendantTheOriginalQuirk`.
- **Range checks** compare squared distance against `range²` (`inRange`,
  lines 128-131); attunement `-1` matches anything (lines 84-86).
- **Traversal guard**: the static search cache caps at 512 positions and is
  cleared per root (lines 74-78, 133-152).
- **Drainer lookup** (`calculateNearbyNodes`, lines 133-170): per tree the
  member closest to the drainer, each judged by its own range; results are
  cached per drainer position and the whole cache is dropped on any
  topology change (lines 47, 96).
- **Traversal guards on drain**: only live, chunk-loaded descendants are
  walked (`getAllChildren`, lines 172-181); line-of-sight is a pluggable
  hook (`canNodeBeSeen`, lines 176-185 — the tile layer installs the ray
  trace; default transparent).
- **The 40-tick pass** (`networkTick`, `TileVisNode.java:117-152`):
  sources self-register once, members with an invalid parent re-attach,
  broken child edges drop the subtree.

**Parity deviation (documented):** the original held parent/child edges as
`WeakReference`s, so liveness was GC-based. The port uses explicit
references plus `isRemoved()` — deterministic on the target platform — and
therefore must drop the static registries explicitly: `VisNetworkHooks`
clears all networks on `ServerStoppedEvent` so a second world in the same
JVM never inherits the first one's graph. The particle beam
(`generateVisEffect`, lines 36-38) is client visuals and is left to the
M7 visual pass.

## Ambient vis (provisional until TC-M5-01)

Thaumcraft 4.2.3.5 has **no runtime ambient-vis API**: the beta-era
per-chunk aura store was removed, and the only surviving "aura" is the
generation-time biome aura value (`BiomeHandler.getBiomeAura`,
`docs/balance-values.md` §6.1). The port's per-chunk aura fields
(density/purity/aspects) are the M2-02 `WorldMagicData` store that
TC-M5-01's aura generation will fill. `world/AmbientVis` is the stable
read-side contract consumers (recharge devices, thaumometer, M5-02 node
behavior) use, so value semantics never leak into consumer code. Mapping
decision, revisited when TC-M5-01 defines generation: stored aura aspect
amounts are the ambient vis amounts verbatim; density/purity are exposed as
their scalars; a chunk with no record or density ≤ 0 has no ambient vis —
reading never invents values.

## Testing

- `src/test/.../wands/VisPoolMathTest` — capacity tables (§2.2-2.3),
  deposit clamp/leftover, non-primal refusal, all-or-nothing withdrawal,
  crafting ×100, payload normalization + codec/stream-codec round-trips.
- `src/test/.../wands/VisCostRulesTest` — cap table §2.1, the modifier
  chain §2.4, gear aggregation + Vis Exhaustion penalty §2.5, the
  truncating cast, end-to-end effective costs feeding the payment check.
- `src/test/.../visnet/VisNetHandlerTest` — registration, range/attunement
  gating, the root-preference quirk, chain drains, drainer-nearest
  selection, LOS/chunk-loaded guards, removal, the 40-tick pass, cache
  invalidation, per-dimension isolation.
- `src/test/.../world/AmbientVisTest` — no-record/zero-density/unknown-tag
  leniency, verbatim aspect mapping.
- `gametest/VisCoreGameTest.visChargeWithdrawRoundTripPersistsAcrossSave`
  — charge → over-capacity clamp → withdraw → `ItemStack.save` →
  compressed NBT on disk → `parseOptional`, asserting the centi-vis state
  survives the real save path and capacity re-derives after reload.
