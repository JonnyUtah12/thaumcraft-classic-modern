# Research progression architecture (TC-M3-02)

The port of Thaumcraft 4.2.3.5's `thaumcraft.common.lib.research.ResearchManager`
progression core. The entry graph itself is the data-driven JSON extracted 1:1
from the decompiled `ConfigResearch.java` (TC-M3-01,
`docs/research-graph-notes.md`); this milestone adds the rules that move a
player through that graph: starting research, prerequisites, unlock triggers,
hidden-research reveal rules, the research-point economy, warp-on-research, and
the S2C sync + `/thaumcraft` command surface.

Code lives in `com.jonnyutah12.thaumcraftclassic.research` (plus command and
network glue):

| File | Role | Original counterpart |
|---|---|---|
| `research/ResearchManager` | pure rule methods over `PlayerKnowledgeData` + thin `ServerPlayer` glue that persists attachments and replays packets | `common/lib/research/ResearchManager` |
| `research/ResearchEvents` | game-bus wiring: join → starting research + primal pool seed; craft → item-warp table + `createClue` | `EventHandlerEntity.playerLoggedIn` tail, `EventHandlerWorld.onCrafting` |
| `command/ThaumcraftCommand` | `/thaumcraft help/list/research/aspect/warp` (op level 2) | `CommandThaumcraft` |
| `network/payload/PlayerCompleteToServerPayload` | C2S Thaumonomicon completion/note request | `PacketPlayerCompleteToServer` |
| `network/payload/ResearchCompletePayload`, `AspectPoolPayload`, `SyncWarpPayload`, `WarpMessagePayload` | S2C knowledge/pool/warp notifications | `PacketResearchComplete`, `PacketAspect`, `PacketWarp` |

Per-player state is the TC-M2-02 attachments (`player/PlayerKnowledgeData`,
`player/PlayerWarpData`); the attachment sync (`ModAttachments`,
`docs/architecture/persistence.md`) is the baseline knowledge S2C channel —
the per-event payloads above replay the original's individual notifications on
top of it.

## Layering: pure rules vs. server glue

Every rule is a static method taking/returning the immutable records with no
Minecraft access (`isResearchComplete`, `doesPlayerHaveRequisites`,
`completeResearchUnsaved`, `warpSplit`, `findHiddenResearch`,
`findMatchingResearch`, `hiddenClueKeys`, `normalizedItemId/EntityName`), so
the original's semantics stay unit-testable without a game runtime. The
`ServerPlayer` methods (`completeResearch`, `onPlayerJoined`, `createClue`,
`handlePlayerCompleteRequest`, `applyWarp`, `applyCraftingWarp`) are thin glue:
read attachment → call the pure rule → `setData` (which triggers the
attachment sync) → send the original's packets.

## Progression rules (parity notes, all pinned by tests)

- **Starting research.** On join, every graph entry flagged `autoUnlock` is
  completed — the original's 22-entry starting set. A player with an empty
  discovered-aspect map (the state of a missing `.thaum` file) gets the six
  primal pools at `15 + rand(5)` each, matching `loadPlayerData`'s
  "Assigning initial aspects" branch.
- **Prerequisites.** `doesPlayerHaveRequisites` gates on `parents` AND
  `parentsHidden`. The original's empty-completed-list quirk is reproduced
  verbatim: with a prerequisite list present and an empty completed list the
  result is false.
- **`@`-keys.** Note/clue unlocks are stored under an `@`-prefixed key, a
  distinct key from the plain one; `isResearchComplete` checks `@`-keys
  verbatim (bypassing the graph-existence check) and they carry no research
  warp — as in the original.
- **Hidden reveal.** `findHiddenResearch` picks among hidden entries that have
  tags, at least one trigger, met prerequisites, and are not yet complete; the
  pick is seeded `dayTime / 10 / 5` exactly like the original (same day →
  same pick). `createClue` matches hidden/lost entries by item, entity, or
  aspect trigger and completes a random match's `@`-key. Item matching ignores
  meta (the original's `areItemStacksEqual(..., true, ...)`); trigger ids are
  normalized to the frozen id contract (D-001). OreDict-equivalent matching is
  deferred to TC-M6 item tags.
- **Aspect-click matching.** `findMatchingResearch` (used by the Thaumonomicon
  aspect search, GUI in TC-M3-03) skips hidden/lost/autoUnlock/virtual/stub
  and secondary-under-current-difficulty entries; in easy mode (`-1`) the
  original's precedence makes every entry secondary, so nothing matches —
  reproduced.
- **Research points.** In 4.2.3.5 the per-player "research points" *are* the
  aspect pool (the original `/thaumcraft aspect` help calls them "aspect
  research points"); there is no per-category point store in this version.
  Completing an entry through the Thaumonomicon deducts the entry's tag
  amounts via the original all-or-nothing `AspectList.reduce` rule: a pool
  below the cost is left untouched, never negative. The constructor
  "complexity" (RP cost, 1..3 clamp) is transcribed into the graph and only
  drives note generation (TC-M3-04). Both the pool arithmetic and the full
  `docs/balance-values.md` §4 cost table (201 entries, distribution
  0×27/1×75/2×83/3×16) are pinned by `ResearchPointMathTest`.
- **Warp on research.** `completeResearch` applies the entry's warp as the
  original split it: `warp > 1` → `warp - warp/2` permanent + `warp/2`
  sticky; `warp == 1` → all permanent; wuss mode suppresses it. Each add
  sets the eldritch counter to the new warp total and sends the original's
  warp sync + message packets.
- **Crafting warp.** `applyCraftingWarp` replays the complete
  `ThaumcraftApi.addWarpToItem` table from the decompiled ConfigResearch
  (8 entries, pinned by test); entries are dormant until their items are
  registered by their content milestones.
- **Completion request.** `handlePlayerCompleteRequest` mirrors
  `PacketPlayerCompleteToServer.onMessage`: already-complete ignored, unmet
  prerequisites answer `tc.researcherror`, type 0 pays tags then completes
  and auto-completes requisition-satisfying siblings, plays `entity.thaumcraft.learn`;
  type 1 (research note) is a TC-M3-04 TODO — the reveal rules it feeds are
  implemented here.

## Triggers wired in-game

`ResearchEvents` runs the join path on `PlayerEvent.PlayerLoggedIn` and the
craft path on `PlayerEvent.ItemCraftedEvent` (warp table + `createClue`).
The original fired `createClue` only from `ScanManager`; the craft hook is
this port's trigger wiring per the TC-M3-02 card, reusing the original
matcher unchanged. Entity/aspect trigger entry points (`createClue(player,
entityName, aspects)`) are ready for the M6 entity and M5 wand wiring.

## Testing

- Unit (`src/test/.../research/`): `ResearchProgressionRulesTest` — starting
  set, prerequisites incl. hidden parents and the empty-list quirk, `@`-key
  semantics, trigger normalization/matching, reveal determinism, difficulty
  precedence, warp split, crafting-warp table. `ResearchPointMathTest` —
  pool spend/refund/all-or-nothing vs `balance-values.md` §4, cost table for
  all 201 entries, complexity clamp. `ThaumcraftCommandTest` — command tree.
- Game (`gametest/ResearchProgressionGameTest`, 6 tests): join auto-unlock
  equals the original 22-entry set + pool seeding; craft trigger reveals
  `@BONEBOW`; FOCUSFIRE purchase pays FIRE 3 + MAGIC 3 out of the pool;
  JARBRAIN warp 3 splits 2 perm + 1 sticky with eldritch counter 3;
  knowledge + warp survive death via `copyOnDeath`; hidden reveal picks a
  valid entry and persists. Death+reload persistence of the stored data is
  covered by the TC-M2-02 persistence gametests.
