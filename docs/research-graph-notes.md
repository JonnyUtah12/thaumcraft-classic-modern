# Research Graph Notes (TC-M3-01)

The complete original research graph — 6 categories and 201 research
entries with pages, triggers, prerequisites, warp and flags — is shipped as
data, not code:

- `src/main/resources/data/thaumcraft/research/categories.json`
- `src/main/resources/data/thaumcraft/research/entries.json`

`ResearchGraphLoader` parses them into `ResearchGraph` (lookup registry over
`ResearchCategoryData` / `ResearchEntryData` / `ResearchPageData`). The
loader is deliberately Minecraft-free so the graph is unit-testable; the
Thaumonomicon UI (TC-M3-03) and progression logic (TC-M3-02) consume this
registry.

## Regenerating the data

    python3 scripts/extract_research_graph.py

The extractor reads `../reference/decompiled/thaumcraft/thaumcraft/common/
config/ConfigResearch.java` (the decompiled original, kept outside the
repository — see `licensing-and-attribution.md`). Upstream 1.7.10 named
this file `thaumcraft/common/lib/research/Research.java`; this
decompilation registers the graph in `ConfigResearch.java`.

Design rule: **every construct the file uses is handled explicitly;
anything unrecognised is reported, never guessed.** The extractor is a
brace/paren-balanced scanner over the registration statements; any
construct it cannot resolve makes the run exit non-zero with a problem
list, so a silent wrong value is not possible.

## Format

Both files carry `formatVersion: 1` and a declared `count`; the loader
refuses a mismatch. Field semantics mirror the original API exactly
(`ResearchItem` constructor + `setX` chain, `ResearchPage` constructors,
`ResearchCategories.registerCategory`). Notable points:

- **Display strings**: `nameKey`/`textKey`/page `textKey` are the original
  `tc.research_name.*` / `tc.research_text.*` / `tc.research_page.*`
  keys; runtime display must resolve them through the game language
  system. The embedded `*EnUs` values are verbatim All Rights Reserved
  quotation from the original `en_US.lang`, kept for verification only.
- **complexity vs complexityRaw**: the original constructor clamped
  complexity to 1..3; 25 stub entries passed 0. `complexity` is the
  effective (clamped) value, `complexityRaw` records the pre-clamp value
  when it differs.
- **Virtual stubs**: `CAP_iron` and `ROD_wood` are virtual auto-unlock
  stubs with no lang name/text in the original; their display stays
  `null` (UNKNOWN) rather than an invented fallback.
- **Recipe references** are keys of the original recipes map
  (`ConfigRecipes.recipes`). Eight recipe objects were constructed inline
  in `ConfigResearch.java` instead of registered — the sceptre's three
  `ShapedArcaneRecipe`s and the five runic-augmentation
  `InfusionRunicAugmentRecipe`s — and are keyed `inline:<Class>#<n>`.
- **Loop-generated keys** (`JarLabel0..47`, `Banner_0..7`,
  `BalancedShard_0..5`, `Clusters`, `PrimalArrow_*`) are resolved by
  parsing the `for` header that generated them in `ConfigRecipes.java`.
- **Array pages**: `recipes` preserves the original array order; the
  original UI shuffles display order at runtime (TC-M3-03 must reproduce
  that shuffle, the data does not).
- **Icon metadata** is resolved to the exact original value, including
  computed ones: `SCEPTRE` uses `(int)(WAND_CAP_THAUMIUM.getCraftCost()
  * WAND_ROD_SILVERWOOD.getCraftCost() * 1.5)` = 81, resolved from the
  `WandCap`/`WandRod` costs in `ConfigItems.java`.
- **Obfuscated vanilla fields** in `new ItemStack(...)` expressions were
  resolved with MCP stable_11 mappings, persisted at
  `../reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv` (outside the
  repository, like the rest of the reference tree; fetched from
  `maven.minecraftforge.net/de/oceanlabs/mcp/mcp_stable/11-1.7.10/`;
  the usual GitHub `minecraft-data` mirrors were unreachable). The
  resolved set is pinned in `ResearchGraphDataTest.ALLOWED_VANILLA_ITEMS`
  — any new one forces a re-review.
- **Recipe list variables**: `ConfigResearch.java` reuses the variable
  name `rc` for different lists in different entries; the extractor
  parses list *definitions* with their source positions and applies them
  in source order.

## Verification (what the tests pin)

`ResearchGraphDataTest` (runs against the repo docs and, when present, the
local `../reference` tree):

- entry keys equal the TC-M0-04 inventory section 8 table exactly
  (201 rows; category, display name and hidden/autoUnlock/special flags
  match row by row);
- every embedded lang string is byte-identical to the original
  `en_US.lang`, and every referenced key exists there (except the two
  UNKNOWN stubs, which must *not* exist);
- every aspect reference resolves against the inventory's 48 aspect tags;
  every item reference resolves against the inventory's registered
  items/blocks or the pinned vanilla allow-list;
- the graph is acyclic and every prerequisite/page-gate reference
  resolves (`ResearchGraph.problems()` must be empty).

`ResearchGraphLoaderTest` loads the shipped resources through the loader
and spot-checks the semantics above (FOCUSHELLBAT hidden+triggers+warp,
JARLABEL clamp + 48-variant array order, SCEPTRE computed meta 81,
CRUCIBLE smelting page, THAUMONOMICON compound page).

## Known gaps / follow-ups

- The original `ResearchPages` helper classes (puzzle pages) are not part
  of the graph data; minigame pages are TC-M3-04.
- Unlock/progression semantics (ResearchManager) are TC-M3-02; the
  Thaumonomicon UI (including array-page shuffle and category page
  layout) is TC-M3-03.
- `ResearchGraph.problems()` also reports duplicate browser positions and
  unresolvable page icon inputs; none exist in the current data.
