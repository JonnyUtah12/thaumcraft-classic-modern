# Infusion altar (TC-M4-04)

The port of Thaumcraft 4.2.3.5's infusion altar: the runic matrix tile and
its detection of a valid altar and surrounding pedestals, the instability
economy and its consequence events, the center-item/component consumption
rules, the two recipe kinds as data-driven recipes, and the S2C sync of
matrix state. Pillar/pedestal visuals and the infusion beam effects are
M7-01/M7-03 work; this milestone lands the tiles, the rules and the hooks.

Code lives in `com.jonnyutah12.thaumcraftclassic.tiles`,
`...recipes`, `...aspect` (one helper) and `...blocks` (device substrate):

| File | Role | Original counterpart |
|---|---|---|
| `blocks/BlockDeviceBase` | shared multi-variant device block: one `variant` IntegerProperty, per-variant `DeviceTileFactory` binding, `TickingDeviceTile` ticker dispatch | `common/blocks/BlockDeviceBase` + `createTileEntity` switch |
| `blocks/BlockStoneDevice` | the stone device block (15 variants; altar uses 1/2/3/4) | `common/blocks/BlockStoneDevice.java` |
| `tiles/InfusionMatrixLogic` | pure altar logic: `validLocation`, the `getSurroundings` pedestal/symmetry scan, instability math, the 21-roll event table | `common/tiles/TileInfusionMatrix.java` (lines 300-530 region) |
| `tiles/TileInfusionMatrix` | the runic matrix BE: activation, craft cycle, event application, S2C sync | `common/tiles/TileInfusionMatrix.java` |
| `tiles/TileInfusionPillar` | pillar BE (state carrier for `validLocation`/scan; visuals M7) | `common/tiles/TileInfusionPillar.java` |
| `tiles/TilePedestal` | single-slot display pedestal (`WorldlyContainer`) | `common/tiles/TilePedestal.java` |
| `recipes/InfusionCraftingRecipe` | item→item infusion recipe incl. research gate + output-tag seam | `common/crafting/RecipeInfusionCrafting` |
| `recipes/InfusionEnchantmentRecipe` | enchantment infusion recipe (instability/essentia/XP formulas) | `common/crafting/RecipeInfusionEnchantment` |
| `recipes/M404RecipeSerializers` | recipe types, codecs, stream codecs | `RecipeSerializerImpls` |
| `aspect/EssentiaHandler` | cached 12-block search + drain over `IAspectSource` tiles | `common/lib/events/EssentiaHandler.java` |
| `registration/catalog/M404Infusion` | block/BE registration + (block, variant) → tile bindings | `Thaumcraft.java` init + `BlockStoneDevice.createTileEntity` |
| `network/payload/fx/FXInfusionSourcePayload` | S2C "essentia stream" FX hook (renderer arrives with M7) | `EffectInfusionSource` spawn packets |

## Altar shape and the surroundings scan (pinned by unit tests)

`validLocation` is the original verbatim: a pedestal two below the matrix and
infusion pillars at `centre.east().south()`, `east().north()`,
`west().north()`, `west().south()`.

`InfusionMatrixLogic.scan` ports `getSurroundings` exactly, including the
original's quirks: the column under the matrix is skipped, pedestal search is
bounded to |dx|,|dz| ≤ 8 and 1..10 blocks below the matrix with a per-column
`skip` latch (only the first pedestal in a column counts), stabilisers are
collected over the full ±12/−5..10 box, and the symmetry sum keeps the
original's integer accumulation (`+1` item, `+2` pedestal, `−2` mirrored
pedestal, `−1` mirrored item) plus the float stabiliser term (`+0.1`,
`−0.2` mirrored) truncated with `(int)` at the end. The scan result is
cached on the tile and only recomputed when `checkSurroundings` is set
(original behavior; the tile starts with it set so the first tick after
placement or chunk load scans, and the essentia phase sets it again when the
price is fully paid).

## Instability economy (vs docs/balance-values.md TC-M0-06)

- Start: `instability = symmetry + recipe.instability` — no clamp
  (`craftStartInstability`).
- Every later increment goes through `cappedIncrement` = `min(25, n+1)`.
- Per craft cycle an event fires when `instability > 0 &&
  random.nextInt(500) <= instability` (`eventFires`).
- The event is chosen by `random.nextInt(21)` mapped through
  `eventForRoll` — the original's switch table is reproduced index-by-index
  (eject types 0-5, single/all zap, single/all harm, lightning, warp), with
  the eject type carried in the high byte.
- Stall branches (no matching pedestal item, no XP for enchantment crafts)
  re-add +1 to a random recipe aspect with the original divisors and can
  bump instability with `nextInt(divisor − recipeInstability·2) == 0`.

Events apply the original consequences: eject (drop or destroy + flux goo/gas
above the pedestal when those blocks exist — placed by id lookup so the
later flux card plugs in without a code change), zap (magic damage 4+rand4),
harm (taint 120t / vis_exhaust 2400t effects by id lookup), lightning
(sound; the bolt itself is M7), warp (25% sticky 1, else permanent
1+rand5 via `ResearchManager.applyWarp`).

## Craft cycle rules (center-item + pedestal consumption)

The cycle runs from the BE ticker every `countDelay` ticks (10; 20 while
draining XP for enchantments; the original's values). Per cycle:

1. If the center pedestal no longer matches `recipe.input()`, the craft fails
   (instability reset, CRAFTFAIL sound).
2. Essentia is paid one unit per cycle via `EssentiaHandler.drainEssentia`
   over `IAspectSource` tiles within 12 blocks (jars/tiles arrive with
   M4-06; the gametest uses a probe source).
3. Each pending component: first cycle arms `itemCount = 5` and sends the
   `FXInfusionSourcePayload` hook, then counts down; at 0 the pedestal item
   is replaced by its container item (the original's `getContainerItem`
   rule) and the component is consumed.
4. When the list empties, `craftingFinish` puts the output on the center
   pedestal (`setItemFromInfusion`), applying the output-tag seam (tag
   label/value from the recipe JSON) when present.

Enchantment recipes keep the original formulas: instability from
`calcInstability` (enchant level/weight), essentia scaled by
`getEssentiaMod`, XP levels from `calcXP` drained one level per cycle from
nearby players (with the magic damage tick and FX payload).

## Recipes as data

All 63 crafting + 24 enchantment recipes from the original `ConfigResearch`
infusion entries are shipped as JSON under
`data/thaumcraft/recipe/recipe.*.infusion{,_enchantment}.json`, extracted
1:1 by `scripts/extract_infusion_recipes.py`. Their ingredient ids reference
catalog items owned by later cards (itemshard, resources, etc.); they load
once those ids exist and are structurally guarded by
`InfusionRecipeDataTest` (counts, key uniqueness, research keys vs the
research graph, aspect tags vs the aspect registry).

## S2C sync

Matrix state (active/crafting/instability) syncs through
`getUpdateTag`/`getUpdatePacket` like the original's data NBT; pedestal
items sync the same way (the original relied on container resync). FX use
dedicated payloads (`FXInfusionSourcePayload`) distributed with
`sendToPlayersTrackingChunk`.

## Registration ownership (provisional)

`blockstonedevice` is catalog-owned by TC-M6-04c and `tilepedestal` by
TC-M6-04d, but both cards were abandoned mid-implementation and the altar is
the only consumer of variants 1-4. `M404Infusion` therefore registers the
device block, its block item and the three altar BE types, and binds only
variants 1/2/3/4; other cards bind their own variants through
`DeviceTileFactory` when they land. Handover notes are in the class javadoc
and docs/catalog-triage.md.

## Testing

- `src/test/.../tiles/InfusionMatrixLogicTest.java` — unit: `validLocation`
  on known arrangements, the full `getSurroundings` scan incl. the skip
  latch and truncation quirks, symmetry tables, instability math and the
  21-roll event table vs TC-M0-06.
- `src/test/.../recipes/InfusionRecipeDataTest.java` — unit: the shipped
  recipe JSONs (counts, schema, cross-references).
- `gametest/InfusionAltarGameTest.fullInfusionCraftConsumesPedestalsAndCenter`
  — live server: builds the altar, injects a synthetic recipe into the real
  `RecipeManager` (catalog ingredients are unlanded), activates through the
  empty-hand use seam, and asserts the ticker drains the essentia source,
  consumes both component pedestals and replaces the center item.
