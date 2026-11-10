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

## Thaumonomicon note request (TC-M3-04-s-v)

The book's third interaction: clicking a node of the graph. The original ran
that click in-process (`GuiResearchBrowser.func_73864_a`,
`GuiResearchBrowser.java:682-731`) and re-ran its own gate server-side in
`PacketPlayerCompleteToServer.onMessage`
(`common/lib/network/playerdata/PacketPlayerCompleteToServer.java:50-100`).
The port keeps the split, and the seam rule the card set: **the book only asks**.

| Piece | Role | Original counterpart |
|---|---|---|
| `research/book/BookNoteRequest` | the click's arm + the hover line, as a pure function of (completed keys, `research_difficulty`, scribing ink, carried note, aspect pool); no client class, so a dedicated server runs the same code | `GuiResearchBrowser.func_73864_a` (:682-731), the tooltip lines (:560, :614-618, :656-658), `canUnlockResearch` (:789-809) |
| `client/ThaumonomiconScreen` | sends the shipped `PlayerCompleteToServerPayload` (type 0 / type 1), draws the hint line in the original's colour and the 3000 ms popup | the click, `popuptime`/`popupmessage` (:186-191, :717-727) |
| `ResearchManager.handlePlayerCompleteRequest` | the one completion authority: pay the tags, `ResearchCompletePayload` + `completeResearch` for the entry and the siblings it unlocks; the note arm returns without completing | `PacketPlayerCompleteToServer.onMessage` (:54-90) |
| `ResearchManager.createResearchNoteForPlayer` | the note arm: `noteSlot` gate, the scribe-tool + paper gate, one durability, `createNote` grid, add-or-drop, `broadcastChanges` | `ResearchManager.java:102-121` |
| `items/ItemInkwell` + `api/IScribeTools` | the shipped ink source marker; stack 1, durability 100 | `common/items/ItemInkwell.java:19-29`, `api/IScribeTools.java` |

Rules the code pins:

- **No second completion path.** The purchase arm completes only through
  `completeResearch` + `ResearchCompletePayload` — the pair
  `ItemResearchNotes.completeWithSiblings` already drives. The note arm never
  completes: it hands over an unsolved generated note and the entry stays
  unknown, which is the original's behaviour and the probe's headline assert.
- **The gates run twice, on both sides, on their own data.** The client reads
  its inventory view and synced knowledge to decide what to ask for; the server
  re-reads its own copy (`isResearchComplete`, `doesPlayerHaveRequisites`,
  `consumeInkFromPlayer`, `carriesPaper`, `noteSlot`), so a spoofed request for
  a locked entry, a note-less entry, or an ink-less player buys nothing and
  spends no durability.
- **The carry check is paper, not an ink sac.** The original's
  `InventoryUtils.isPlayerCarrying(player, new ItemStack(Items.field_151121_aF))`
  and `inventory.hasItem(Items.field_151121_aF)` name the MCP field
  `paper` (`reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv:1723`), and
  the original's own hint line agrees: `tc.research.shortprim` = "You need
  scribing tools and paper to get this research note". Paper is required and
  is never consumed; only one durability comes off the scribe tool.
- **A carried note is free.** `noteSlot` (the original's `getResearchSlot`)
  short-circuits the request: no second note, no ink (the original returns the
  existing stack, `ResearchManager.java:105-107`).
- **The hint is the affordance.** Five states, each with the original's key and
  colour: `tc.researchmissing` (-9416624), `tc.research.hasnote` (16753920),
  `tc.research.getprim` / `tc.research.purchase` (8900331),
  `tc.research.shortprim` / `tc.research.short` (14423100), and the
  `tc.research.popup` line (khaki, wrapped at 150 px) for 3000 ms after a note.

## Scribing Tools acquisition (TC-M3-04-s-vi)

BD-19 left the inkwell survival-unobtainable (BD-20), so the card ports the
original's acquisition rows: `ConfigRecipes.java:3971-3982` registers three
`shapelessOreDictRecipe` rows for `itemInkwell` at mod load - Scribe1 = empty
phial (`itemEssence` meta 0) + feather (`Items.field_151008_G`, MCP fields
CSV :1610) + `dyeBlack`, Scribe2 = glass bottle (`Items.field_151069_bo`,
:1671) + feather + `dyeBlack`, Scribe3 = inkwell (meta 32767 wildcard) +
`dyeBlack` recolor. The helper (`ConfigRecipes.java:4091-4095`) drops every
row straight into the CraftingManager: never research-gated, so the shipped
rows are plain `minecraft:crafting_shapeless` JSON, not arcane/infusion rows.

- **The phial ingredient is component-pinned.** 1.21.1 has no meta-item
  subtypes; the port keeps metas on `thaumcraft:item_meta` (BD-04 / D-001).
  The Scribe1 ingredient is the registered `neoforge:components` Ingredient
  (the `type` key is mandatory, D-015) pinning `item_meta` 0 - the
  jar-drain return `ItemPhial.stack(0)` matches, and a filled (meta 1)
  phial never does: the original's meta-0 `ShapelessOreRecipe` ate only
  empty phials, and a bare-id ingredient would eat essentia the original
  kept (BD-20 alternative (a)). The creative-tab phial carries no component
  and crafts nothing - same as a 1.7.10 creative phial (meta 0 as a
  damage value, matching) with the port's component model, supply arrives
  by draining jars.
- **`dyeBlack` reads the port's oredict tag family.** The 1.7.10 `dyeBlack`
  entry was dye meta 0 - the item named Ink Sac (the squid path). 1.21.1
  splits that identity, so `thaumcraft:oredict.dyeblack` carries
  `minecraft:ink_sac` (the original's item) + `minecraft:black_dye` (the
  modern dye), matching the `oredict.dyeblue` family precedent.
- **Scribe3 ships as NOTHING.** Its ingredient is the inkwell itself with
  the 32767 wildcard, which acts on the 16-colour meta family; the port's
  `ItemInkwell` declares no subtypes, so there is nothing to recolor, and
  a dye-consuming no-op row would invent a cost (BD-20). The recolor row
  lands with the colour family on the item models card (D-004).
- **Gate discipline:** every item each row names carries a
  `neoforge:item_exists` condition (the `neoforge:components` item is in
  its `items` list), so an unported ingredient SKIPS the row instead of
  failing the data pack; both rows and every shipped `.crafting_` row
  decode, are live, and craft on the server (see Testing).

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

- Thaumonomicon note request (`research/BookNoteRequestRulesTest` 13; game
  `gametest/ThaumonomiconNoteRequestGameTest` 8): the prerequisite gate
  (`canUnlockResearch`, drawn and hidden parents, a parent missing from the
  graph does NOT fail it), the purchase/primary split at difficulty -1/0/1,
  the pool-affordability loop, the five hint keys and their colours, the
  popup window and its `%1$s` name argument, and the arm order. The probes
  drive the payload handler on a real `ServerPlayer` with a declared ad-hoc
  channel: Scribing Tools registered as the `IScribeTools` source with the
  original's stack-1/durability-100 constructor, the note arm's single
  durability use and generated unsolved note while the entry stays unknown,
  the carried-note gate, the scribe-tool/paper rejections with no ink spent,
  the requisites rejection of a spoofed locked-entry request, the note
  dropping at the player when the inventory is full, the purchase arm paying
  the tags out of the pool and completing through the shipped authority, and
  the click decision computed over the real 201-entry graph.
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
- Scribing Tools acquisition (`recipes/InkwellRecipeDataTest` 6, game
  `gametest/InkwellRecipeGameTest` 4): the data test reads the decompiled
  Scribe1/2/3 rows out of `ConfigRecipes.java` field-for-field, resolves
  `field_151008_G`/`field_151069_bo` through the MCP fields CSV, pins the
  shipped shapeless shape, the component-pinned meta-0 phial, the
  glass-bottle/feather/dyeBlack ids, the `dyeBlack` tag contents
  (`ink_sac` + `black_dye`), the `neoforge:item_exists` gate on every named
  item, the no-inkwell-as-ingredient rule (Scribe3 deferred), the
  content-mapping row, and the whole shipped `.crafting_` family decoding.
  The game probes prove on the server that every shipped `.crafting_` row
  (this family included) parses and is live in the `RecipeManager`, that
  the phial and glass-bottle grids each assemble a fresh inkwell at full
  100 durability through the vanilla match, that the ink-sac grid crafts
  (the squid path), and that filled-phial, un-component-phial, blue-dye and
  feather-less grids refuse.
