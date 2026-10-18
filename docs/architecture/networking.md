# Networking architecture (TC-M2-03)

Foundation for all client/server traffic. The original Thaumcraft 4.2.3.5 used
an FML `SimpleNetworkWrapper` channel (`thaumcraft`) with 40 message classes
under `thaumcraft/common/lib/network/` plus a handler/packet pipeline
(`PacketHandler`, `EventHandlerNetwork`). On NeoForge 1.21.1 the channel is
replaced by the payload system: `RegisterPayloadHandlersEvent` +
`PayloadRegistrar`, one `CustomPacketPayload` record per message with an
explicit `StreamCodec`.

Code lives in `com.jonnyutah12.thaumcraftclassic.network`:

| File | Role | Original counterpart |
|---|---|---|
| `ModNetwork` | registers every payload on both dists; versioned registrar (`"1"`) | `common/lib/network/PacketHandler` |
| `ModNetworkEvents` | pushes the wipe + full knowledge sync on player login | `EventHandlerNetwork.playerLoggedInEvent` |
| `ClientNetworkEvents` | drops the client mirror on client login | `EventHandlerNetwork.clientLoggedIn` |
| `ClientPayloadHandlers` / `ServerPayloadHandlers` | dist-specific handler entry points | the `onMessage` methods inside each packet class |
| `ClientNetworkData` | client mirror of player knowledge (research, scans, aspects, pools, warp, runic charge) | client half of `PlayerKnowledge` + `GuiResearchBrowser.completedResearch` |
| `ClientEffectHooks` / `ServerNetworkHooks` | action sinks for effects and C2S actions; stubs until the owning feature milestone | proxy calls inside the original handlers |
| `NetworkCodecs` | shared codecs (Vec3 as 3 floats) | `ByteBufUtils` helpers |
| `payload/`, `payload/fx/` | 37 payload records, 1:1 with the original message classes | see inventory |

## Protocol and registration

- `event.registrar("1")` — the channel version is bumped whenever any payload
  codec changes incompatibly; NeoForge refuses mismatched connections.
- Every payload id is `thaumcraft:<snake_case>` and must stay unique; the
  gametest asserts namespacing and uniqueness.
- Registration runs identically on both dists (the handshake compares the
  registry). Direction is enforced by `playToClient` / `playToServer`, so a
  handler only ever executes on the dist that receives the payload — this is
  the structural replacement for the original `Side.CLIENT` / `Side.SERVER`
  registration split.
- Handlers hop onto the game thread with `IPayloadContext.enqueueWork` before
  touching game state, matching the original main-thread `onMessage` behavior.
- `NotePayload` is registered bidirectionally: the original `PacketNote` was
  one class registered on both sides with a `-1` query sentinel; the same
  shape is kept (`NOTE_QUERY`).

## Modernizations vs. the original wire format

1. **Sender identity is implicit.** The original C2S packets carried
   `playerid` (and often `dimension`) and re-checked them against the
   connection. NeoForge delivers every C2S payload with its authenticated
   sender (`IPayloadContext.player()`), so those fields and their spoofing
   checks are gone.
2. **Numeric ids become `ResourceLocation`s.** The original encoded 1.7-era
   numeric block/item/biome ids. Payloads that carry such ids
   (`ScannedToServerPayload`, `FXBlockDigPayload`, `BiomeChangePayload`) use
   `ResourceLocation` instead.
3. **Fixed-width fields become varints.** `short`/`byte` amounts widened to
   `int` + `VAR_INT` where the value domain is not protocol-fixed; kind/key
   discriminators keep the original numeric constants as named `int` fields.
4. **Nullable aspect tag.** `PacketAspectPlaceToServer` wrote the literal
   string `"null"`; `AspectPlaceToServerPayload` uses `Optional<String>`.
5. **Config sync is platform-provided.** `PacketConfig` (server → client copy
   of 7 gameplay options) has no payload: NeoForge syncs server-side mod
   configs to clients automatically, and TC-M2-04's `TcConfig` uses that
   mechanism. The original client-side "restore config on disconnect" logic is
   therefore also unnecessary.
6. **Wand essentia amounts ride container sync.** The original research-table
   and wand GUIs read player knowledge through the synced payloads above;
   per-wand stored essentia was (and stays) item NBT synced via vanilla
   container data, not a custom message. `PacketFocusChangeToServer` /
   `PacketItemKeyToServer` cover the wand-state *actions* (focus change,
   misc toggle) and are ported.

## Inventory: every original message class

Directions are from the client's point of view (S2C = server→client).
"Status" is the acceptance contract: **ported** = payload exists and is
registered; **infra** = replaced by platform wiring; **unnecessary** =
documented as not needed.

### common/lib/network (2 classes)

| Original | Status | Replacement |
|---|---|---|
| `PacketHandler` | infra | `ModNetwork.register` (`RegisterPayloadHandlersEvent`) |
| `EventHandlerNetwork` | infra | `ModNetworkEvents` (login sync) + `ClientNetworkEvents` (client reset) |

### common/lib/network/playerdata (16 classes)

| Original | Dir | Payload | Notes |
|---|---|---|---|
| `PacketSyncWipe` | S2C | `SyncWipePayload` | sent first in the login sync sequence |
| `PacketSyncResearch` | S2C | `SyncResearchPayload` | research-book completion state |
| `PacketSyncScannedItems` | S2C | `SyncScannedItemsPayload` | |
| `PacketSyncScannedEntities` | S2C | `SyncScannedEntitiesPayload` | |
| `PacketSyncScannedPhenomena` | S2C | `SyncScannedPhenomenaPayload` | |
| `PacketSyncAspects` | S2C | `SyncAspectsPayload` | nested `AspectAmount(tag, amount)` |
| `PacketSyncWarp` | S2C | `SyncWarpPayload` | `WARP_PERMANENT/STICKY/TEMPORARY` |
| `PacketWarpMessage` | S2C | `WarpMessagePayload` | notification only |
| `PacketAspectPool` | S2C | `AspectPoolPayload` | essentia pool total + gain notification |
| `PacketAspectDiscovery` | S2C | `AspectDiscoveryPayload` | |
| `PacketResearchComplete` | S2C | `ResearchCompletePayload` | drives book/GUI updates |
| `PacketRunicCharge` | S2C | `RunicChargePayload` | |
| `PacketScannedToServer` | C2S | `ScannedToServerPayload` | ids → `ResourceLocation` |
| `PacketAspectCombinationToServer` | C2S | `AspectCombinationToServerPayload` | sender implicit |
| `PacketAspectPlaceToServer` | C2S | `AspectPlaceToServerPayload` | `Optional<String>` aspect |
| `PacketPlayerCompleteToServer` | C2S | `PlayerCompleteToServerPayload` | username/dimension dropped |

### common/lib/network/misc (8 classes)

| Original | Dir | Payload | Notes |
|---|---|---|---|
| `PacketBiomeChange` | S2C | `BiomeChangePayload` | biome id → `ResourceLocation` |
| `PacketConfig` | S2C | — | **unnecessary**: NeoForge syncs server configs automatically (TC-M2-04 `TcConfig`) |
| `PacketMiscEvent` | S2C | `MiscEventPayload` | `EVENT_WARP/MIST/MIST_SHORT` |
| `PacketNote` | S2C+C2S | `NotePayload` | bidirectional, `NOTE_QUERY = -1` sentinel kept |
| `PacketBoreDig` | S2C | `BoreDigPayload` | |
| `PacketFlyToServer` | C2S | `FlyToServerPayload` | hover toggle; sender implicit |
| `PacketFocusChangeToServer` | C2S | `FocusChangeToServerPayload` | wand focus change |
| `PacketItemKeyToServer` | C2S | `ItemKeyToServerPayload` | `KEY_GOLEM_BELL_RESET/KEY_WAND_MISC_TOGGLE` |

### common/lib/network/fx (14 classes, all S2C)

| Original | Payload |
|---|---|
| `PacketFXBlockBubble` | `FXBlockBubblePayload` |
| `PacketFXBlockSparkle` | `FXBlockSparklePayload` |
| `PacketFXBlockArc` | `FXBlockArcPayload` |
| `PacketFXBlockZap` | `FXBlockZapPayload` (Vec3 pos+dir via `NetworkCodecs.VEC3`) |
| `PacketFXBlockDig` | `FXBlockDigPayload` (block/item id → `ResourceLocation`) |
| `PacketFXEssentiaSource` | `FXEssentiaSourcePayload` |
| `PacketFXInfusionSource` | `FXInfusionSourcePayload` |
| `PacketFXShield` | `FXShieldPayload` |
| `PacketFXSonic` | `FXSonicPayload` |
| `PacketFXVisDrain` | `FXVisDrainPayload` |
| `PacketFXWispZap` | `FXWispZapPayload` |
| `PacketFXZap` | `FXZapPayload` |
| `PacketFXBeamPulse` | `FXBeamPulsePayload` |
| `PacketFXBeamPulseGolemBoss` | `FXBeamPulseGolemBossPayload` |

Entity-target FX keep the original entity-network-id encoding
(`level.getEntity(id)` resolves them on the client).

Totals: 40 original classes = 37 ported payloads + 2 infrastructure
replacements + 1 documented unnecessary.

## GUI-sync scope from the milestone brief

- **research / book**: `SyncWipePayload` → `SyncResearchPayload` on login,
  `ResearchCompletePayload` incrementally; the client mirror
  (`ClientNetworkData.completedResearch`) is the data the research-book GUI
  will render when the M4 UI milestone lands.
- **essentia**: `SyncAspectsPayload` (discovered set), `AspectPoolPayload`
  (pool totals + gain toasts), `AspectDiscoveryPayload`.
- **wand state**: `FocusChangeToServerPayload` + `ItemKeyToServerPayload`
  (actions); stored wand essentia stays on item NBT/container sync (see
  modernization note 6).

## Handler bodies

`ClientEffectHooks` and `ServerNetworkHooks` are the seam: payload plumbing is
final, the bodies are stubs that log at debug level and carry a TODO naming
the milestone that implements the real behavior (research M4, tiles M5,
equipment M3, world M6, client FX M4). Feature milestones fill the hooks in
without touching registration or codecs.

## Verification

- `ModNetworkGameTest.payloadIdsAreNamespacedAndUnique` +
  `payloadCodecsRoundTrip` run on the dedicated game-test server
  (`./gradlew runGameTestServer`): proves server-dist class-loading,
  registration, id hygiene, and byte-exact codec round-trips for all 37
  payloads.
- Client dist: `xvfb-run ./gradlew runClient` boots with the payload set
  registered and no class-loading errors (see docs/testing.md for the smoke
  procedure).
