# Persistence architecture (TC-M2-02)

Foundation for all durable Thaumcraft state. The original Thaumcraft 4.2.3.5
persisted three separate ways: a `WorldSavedValue`-style per-level blob written
through `IChunkProvider`/`WorldSavedMap` hooks (aura, nodes, flux, taint keyed
by `ChunkLoc` strings), raw chunk NBT for the magic-generation marker
(`EventHandlerWorld.chunkSave` wrote a `Thaumcraft`/`regenKey` tag), and
per-player `.dat` files under the world's `Thaumcraft/` folder written by
`ResearchManager.savePlayerData` (research, aspects, scans, warp counters).
On NeoForge 1.21.1 those are replaced by the platform's own persistence
primitives: `SavedData` for per-level state and data attachments for
per-player state.

Code lives in `com.jonnyutah12.thaumcraftclassic.world` and
`...thaumcraftclassic.player`:

| File | Role | Original counterpart |
|---|---|---|
| `world/WorldMagicData` | per-level `SavedData`: per-chunk aura/nodes/flux/taint records + world scalars | `WorldMagicData`/`ThaumcraftSave` blob keyed by `ChunkLoc` |
| `world/MagicChunkHooks` | `ChunkEvent.Load`/`LevelEvent.Unload` hooks; queues chunks whose magic-gen marker is missing for deferred generation | `EventHandlerWorld.chunkSave/chunkLoad`, `Thaumcraft.java` `chunksToGenerate` tick loop |
| `player/PlayerKnowledgeData` | immutable record + lenient codec: research, discovered aspects, scanned objects/entities/phenomena | `PlayerKnowledge` + `ResearchManager.saveResearchNBT/saveAspectNBT/saveScannedNBT` |
| `player/PlayerWarpData` | immutable record + lenient codec: perm/temp/sticky warp, eldritch counter, runic charge | `PlayerKnowledge` warp fields + `ResearchManager.savePlayerData` |
| `player/ModAttachments` | registers both player attachment types (`copyOnDeath`, automatic S2C sync) | `EventHandlerEntity` clone/login plumbing + `PacketNote` knowledge sync |

## Per-level store

- `WorldMagicData` is fetched with `WorldMagicData.get(ServerLevel)`; the
  vanilla `DimensionDataStorage` writes it to `<level>/data/
  thaumcraft_world_magic.dat` on the normal level-save pass. Every mutation
  calls `setDirty()`; there is no separate save tick to schedule.
- The wire format is versioned by a `version` int. Version 1 layout:
  `chunks` list of `{x, z, magicGenDone, auraDensity, auraPurity,
  auraAspects, fluxAir, fluxGround, taint, nodes[]}` plus a `values` compound
  for world scalars. Chunk coordinates are the entry identity; every other
  field is optional and zero-valued fields are not written.
- **Lenient codec contract (all persistence):** unknown keys are ignored,
  missing fields fall back to defaults, and a newer `version` than the reader
  knows is parsed best-effort with a warning instead of refusing the file.
  This is the forward-compatibility rule for every later milestone that
  extends these records.
- TC4's `regenKey` marker cannot live in chunk NBT (NeoForge 21.1 removed the
  raw `ChunkDataEvent` hooks), so the marker is the `magicGenDone` flag in the
  per-chunk record. `ChunkEvent.Load` queues any chunk whose record is missing
  or unflagged; generation hooks run from a server-tick context via
  `MagicChunkHooks.runPendingChunkGen` (TC4's tick-loop parity — never from
  the load callback itself, where the chunk may not be FULL). M5 registers the
  actual node/aura generation hooks.

## Per-player state

- Knowledge and warp ride on NeoForge **data attachments** on `Player`
  (`ModAttachments.PLAYER_KNOWLEDGE` / `PLAYER_WARP`), not side files.
  Consequences, all platform-provided:
  - persistence: attachments serialize into the vanilla player save file under
    the `neoforge:attachments` compound — logout/login needs no TC4-style
    `loadPlayerData`/`savePlayerData` hook;
  - death/respawn: both types are flagged `copyOnDeath`, replacing TC4's
    `PlayerEvent.Clone` copy code;
  - client mirror: both types use automatic S2C sync, replacing the original
    manual knowledge-sync packets (the M3 research layer may additionally use
    the explicit `SyncKnowledgePayload` from TC-M2-03 for bulk updates).
- Records are **immutable**; mutation helpers (`withResearchCompleted`,
  `addWarpTemp`, …) return updated copies, so an attachment value is never
  mutated in place — set a new copy with `setData` and the platform handles
  save/sync/dirty bookkeeping.
- **Parity rule:** field names and NBT tag names are the exact TC4 originals
  (`THAUMCRAFT.RESEARCH`, `THAUMCRAFT.ASPECTS`, `THAUMCRAFT.SCAN.*`,
  `Thaumcraft.eldritch*`, `Thaumcraft.shielding`, entry shape
  `{key}`/`{key, amount:short}`), and warp math keeps the original
  `Math.max(0, current + amount)` clamping. A future importer for legacy
  `.thaum` player files reuses these codecs unchanged.

## Testing

- Unit tests (`src/test`, run with the Minecraft classpath via
  `neoForge.unitTest`): codec round-trips, forward-tolerance (unknown keys,
  missing fields, newer versions), TC4-format parity fixtures, warp clamping.
- Gametests (`gametest/PersistenceGameTest`): the real save/reload path
  (`SavedData.save` flush → fresh `DimensionDataStorage` over a private temp
  directory — the shared `run/world` folder is a collision source across runs
  and sessions, so the round-trip file is hermetic), the player NBT
  save/load path, and the death/respawn copy path (with a dummy
  `ServerGamePacketListenerImpl` on an `EmbeddedChannel`, mirroring
  NeoForge's own `FakePlayer` pattern, since attachment sync sends through
  `player.connection`).
