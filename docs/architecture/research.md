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

## Note puzzle (TC-M3-04-s)

The research-note minigame's rules, ported from
`ResearchManager.createNote` / `checkResearchCompletion` / `checkConnections`
and `thaumcraft.common.lib.utils.HexUtils` (TC 4.2.3.5). Pure Java, no
Minecraft types, so a headless server exercises all of it:

| File | Role | Original counterpart |
|---|---|---|
| `research/HexUtils` | axial/cubic hex math: the `NEIGHBOURS` direction table, `getRing`/`generateHexes` (the disc of radius r = `1 + 3r(r+1)` cells), distance, pixel/cubic rounding, the `"q:r"` map key | `common/lib/utils/HexUtils` |
| `research/ResearchNoteData` | the note state: `key`, `color`, `complete`, `copies` and the grid map; hex entry types 0 empty / 1 required / 2 player-placed; codec writes the original `hexgrid` compounds with byte numerics | `common/lib/research/ResearchNoteData` + `ResearchManager.HexEntry` |
| `research/NotePuzzle` | `generate` (radius `1 + min(3, complexity)`, required aspects on the outer ring, `complexity * 2` interior blanks behind the `cc < 2` guard) and `checkCompletion` (the walk that needs both aspects discovered and one a direct component of the other) | `ResearchManager.createNote` / `checkResearchCompletion` |

Fidelity notes that shape the code:

- **Required-aspect order is the original's.** `createNote` assigned
  `rr.tags.getAspects()[count]` around the ring and `AspectList.aspects` is a
  `LinkedHashMap`, so `getAspects()` is the insertion order `ConfigResearch`
  used — the order the TC-M3-01 extractor preserved in `entries.json`. The port
  reads `entry.tags()` directly; this is not an approximation.
- **The blank-removal draw order is behaviour.** The original rebuilt its
  candidate array from the `HashMap` of cells after every removal and drew one
  `nextInt` per attempt, so the port keeps `HashMap`-backed structures and the
  same rebuild. `NotePuzzleOriginalParityTest` replays the decompiled
  `createNote` and demands an identical grid for all 201 shipped entries across
  fixed seeds; a `LinkedHashMap`-ordered port removes different cells and fails.
- **`distributeRingRandomly` consumes one draw it never uses** (the original's
  `int start = random.nextInt(ring.size())`). Keeping the draw is what holds
  every later pick on the original's random stream.
- The note **item** (`thaumcraft:itemresearchnotes`) is its own slice
  (TC-M3-04-s-ii, the section below); the interactive note screen is still
  open (BD-13).

## Research note item (TC-M3-04-s-ii)

The user-facing half of the minigame: `thaumcraft:itemresearchnotes`
(`items/ItemResearchNotes`, original `thaumcraft.common.items.ItemResearchNotes`)
plus the `thaumcraft:research_note` component that carries the note state.

| Piece | Role | Original counterpart |
|---|---|---|
| `registration/catalog/M3ResearchContent` | the card's own holder: registers the id (`stacksTo(1)`, registered rarity rare, `item_meta` 0 default) with one `ModRegistration.init` line | `ConfigItems.java:365-366` |
| `registration/ModDataComponents.RESEARCH_NOTE` | `DataComponentType<ResearchNoteData>`: persistent `ResearchNoteData.CODEC`, network `ResearchNoteData.STREAM_CODEC` | the note's ItemStack NBT, which the original also synced to the client for free with the stack |
| `items/ItemResearchNotes` | `use()` (both arms), `getName` (the meta-64 split on an id-derived key), `appendHoverText` (blank lines, research name/text, warp block clamped at 5) | `ItemResearchNotes.onItemRightClick` / `getItemDisplayName` / `getRarity` / `addInformation` |

Rules the code pins:

- **The arm order is the original's.** The complete-note arm (note present,
  `complete`, entry not yet complete) is tested first, and its `hasRequisites`
  gate answers `tc.researcherror` and returns *before* anything is completed.
  The blank-discovery arm (metas 24/42) only runs when the first gate fails.
- **Completion is a sweep.** `PacketResearchComplete` + `completeResearch` for
  the note's key, then for every sibling that is incomplete and whose
  requisitions the *new* knowledge satisfies. `GOLEMSTRAW` → `COREGATHER` +
  `GOLEMBELL` is the shipped graph's probe for that.
- **The FAIL arm's drop is the original's:** `7 + rand(3)` items of
  `itemresource` meta 9 in one drop at the player, and the blank is consumed.
  The success arm spends the blank *in place* — meta back to 0, the generated
  grid set, no consumption — and is the original's `createNote` call site.
- **The random stream is seeded, not shared** (BD-18): the port draws once from
  `level.getRandom()` and seeds the generator's own `java.util.Random`, so the
  generation rule (radius, ring order, blank count, the discarded start draw)
  stays intact while the picks are per-world reproducible.
- **The meta-64+ discovery family is name/rarity only.** `thaumcraft:item_meta`
  is bounded at 63, so no shipped path can carry such a stack; the rule (the
  `.discovery` suffix, epic rarity) is ported and the builders refuse foreign
  metas, so the family works the moment the carrier is widened.
- The per-meta icons and the glint overlay are item-model concerns and ride the
  item models card; the interactive note screen is its own card.

## The KnowFrag recipe — how a note is acquired (TC-M3-04-s-iv)

`reference/decompiled/thaumcraft/thaumcraft/common/config/ConfigRecipes.java:3860-3865` is the
original's **only** `itemResearchNotes` recipe: `ConfigResearch.recipes.put("KnowFrag",
GameRegistry.addShapedRecipe(new ItemStack(ConfigItems.itemResearchNotes, 1, 42), "KKK","KKK","KKK",
'K', new ItemStack(ConfigItems.itemResource, 1, 9)))`. It ships as a data row —
`data/thaumcraft/recipe/recipe.256.crafting_shaped.json` — under the id
`thaumcraft:recipe.256.crafting_shaped`, the next free number in the recipe-family numbering that
already carries `recipe.264/266/274` (arcane workbench rows) and `recipe.275` (crucible):

- Pattern `KKK/KKK/KKK` — the full 3x3, so the row is craftable at a crafting table and never in the
  2x2 player grid. The original is a `GameRegistry.addShapedRecipe` (vanilla crafting) row, so the
  type stays `minecraft:crafting_shaped`; the port's arcane/infusion/crucible types would move the
  station, which the original does not.
- The ingredient is the registered component form `{"type": "neoforge:components",
  "items": ["thaumcraft:itemresource"], "components": {"thaumcraft:item_meta": 9}}` (BD-04: the
  original's metadata damage is the `thaumcraft:item_meta` component). The `type` key is mandatory —
  a bare `item` ingredient matches every `itemresource` meta and would let amber (3), nitor (6) or
  pure (13) craft notes; the meta precision is pinned by a probe that offers a bare stack, metas
  1/6/13, eight fragments and one stack of nine, and asserts each refuses.
- The result is `thaumcraft:itemresearchnotes` count 1 with `thaumcraft:item_meta: 42` —
  `ItemResearchNotes.META_BLANK_DISCOVERY_B`. The crafted stack carries **no**
  `thaumcraft:research_note` component, matching the original's meta-42 blank, whose
  `ResearchManager.getData` is null, so right-click rolls `findHiddenResearch` (BD-18).
- The row is gated on `neoforge:item_exists` for **both** items. A 1.21.1 `CraftingOutput` resolves
  the result id while the row is parsed, so an ungated row naming an unregistered item is dropped
  with a `Parsing error loading recipe` ERROR — exactly the shape the `runServer` smoke job fails on
  (the crucible row documents the same rule for its output fluid).
- No research gate: the original's KNOWFRAG entry is `setAutoUnlock`, so the recipe is never
  research-locked, and its Thaumonomicon page shows it because the original stored the recipe in
  `ConfigResearch.recipes["KnowFrag"]`. Vanilla crafting rows have nowhere to carry that key, so the
  page display is BD-19 and belongs to the Thaumonomicon/GUI card.

Proven by `KnowledgeFragmentRecipeGameTest` (3 probes: the shipped `.crafting_*` rows all decode and
are live in the `RecipeManager`, the nine-fragment grid crafts **this** row into exactly
`ItemResearchNotes.blankDiscoveryNote(42)` with all slots consumed, and the ingredient is
meta-precise) and by `KnowFragRecipeDataTest` (5 tests, which parse the decompiled registration and
compare it field-for-field with the shipped JSON).

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
- Note puzzle (`research/HexUtilsTest` 9, `NotePuzzleTest` 12,
  `NotePuzzleOriginalParityTest` 3, `ResearchNoteDataTest` 8,
  `ResearchGraphResourceGuardTest` 3; game `gametest/NotePuzzleGameTest` 2):
  the hex table read back out of the decompiled file, the ring/disc closed
  forms, the discarded start draw, the original NBT layout, the radius and
  blank rules, the completion gate, and a replay of the decompiled `createNote`
  over every shipped entry × 8 seeds. The game probes generate over the whole
  shipped 201-entry graph on a server and drive completion through the TC-M3-02
  knowledge record.

- Research note item (`research/ResearchNoteItemRulesTest` 6; game
  `gametest/ResearchNoteItemGameTest` 8): the meta 64 / blank 24+42 /
  warp-clamp-5 / `7 + rand(3)` rules read out of the decompiled item, the
  name/rarity table, the builder family guards, the tooltip label strings
  against the original `en_US.lang` (the original's `%n` becomes a `%s`
  argument), the arm order, and the id-derived display name. The probes drive
  the shipped item through `ItemStack#use` — the call
  `ServerPlayerGameMode#useItem` makes — on a real `ServerPlayer` with a
  declared ad-hoc channel (the mock player is not a `ServerPlayer`, and the
  completion path sends payloads): registration and both component codecs,
  completion + sibling sweep + consumption, the unmet-requisites gate, the FAIL
  arm's 7-9 fragments counted as `ItemEntity` drops, the success arm's stamped
  meta-0 note, the tooltip keys with the warp clamp, and the note surviving its
  NBT (byte-width `hexgrid` rows), wire and stack-save round trips.
- KnowFrag recipe (`recipes/KnowFragRecipeDataTest` 5; game
  `gametest/KnowledgeFragmentRecipeGameTest` 3): the unit side parses the
  decompiled `ConfigRecipes.java` KnowFrag registration (grid rows, the `'K'`
  binding's item/count/meta, the output item/count/meta) and asserts the shipped
  JSON agrees field for field, plus the mandatory `type` key and list-shaped
  `items` of the component Ingredient, the metas read out of `ItemResource` /
  `ItemResearchNotes` and inside the `thaumcraft:item_meta` carrier's 0..63
  bound, the family's `item_exists` gate rule over every shipped
  `minecraft:crafting_*` row, and the content-mapping row's 8-field contract.
  The game side runs on a server: every shipped `.crafting_*` row whose named
  items all exist is live in the `RecipeManager` under its own id (the
  `runServer` smoke failure mode is a row dropped by a parse ERROR), the
  nine-fragment 3x3 matches **this** row and `assemble` returns exactly
  `ItemResearchNotes.blankDiscoveryNote(42)` — meta 42, rare, unstackable, no
  `thaumcraft:research_note` component, all nine slots consumed — and the
  ingredient is meta-precise: a bare `itemresource` stack, metas 1/6/13, eight
  fragments and one stack of nine all refuse, with the nine-fragment grid as the
  positive control.
