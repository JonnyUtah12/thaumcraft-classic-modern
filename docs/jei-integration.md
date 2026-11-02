# JEI integration — the optional recipe-browser lane (SPINE-4, t_aebd14cf)

## What this lane is, and what it is not

**It is an integration convenience, not port content.** `docs/porting-decisions.md`
D-011 fenced the "put it in JEI" directive as a *rejected directive about port
mechanics*: the 1.7.10 original has no recipe browser, and adding one adds
nothing to behavioral fidelity. Nothing in this lane may be described as ported
Thaumcraft content, and no fidelity claim in any matrix cites it.

The user-facing request that motivated it — "the items are in JEI" — **is
already satisfied with zero JEI code**: JEI enumerates every registered item
for free, so an item shows up because it is registered and has a model
(item-model cards) and is searchable because it is in a public creative tab
(SPINE-2, `thaumcraft:thaumcraft`). That half of the request ships in those
cards, not here.

**Only custom recipe categories need a plugin.** This card adds exactly four,
one per recipe type the port already implements:

| Category id | Java category | Title key (original) | Title value (verbatim) |
| --- | --- | --- | --- |
| `thaumcraft:arcane_workbench` | `client/jei/ArcaneWorkbenchCategory` | `recipe.type.arcane` | Arcane Workbench |
| `thaumcraft:crucible` | `client/jei/CrucibleCategory` | `recipe.type.crucible` | Crucible |
| `thaumcraft:infusion` | `client/jei/InfusionCategory` | `recipe.type.infusion` | Arcane Infusion |
| `thaumcraft:infusion_enchantment` | `client/jei/InfusionEnchantmentCategory` | `recipe.type.infusionenchantment` | Infusion Enchantment |

The four titles are the **original keys and strings**, not invented labels:
1.7.10's own book heads these same four recipe kinds with `recipe.type.*`
(decompiled `GuiResearchRecipe.java`:644, :969, :1162, :1355) and the values are
`en_US.lang:660-663`. D-016: shipped verbatim; the keys are therefore recorded
as original-shape keys in `LocalizationFileTest.LEGACY_KEYS` as well as in both
lang-audit lists.

### Recorded status (the repo's strict status vocabulary)

`FEATURE_MATRIX.md` carries this lane as **DOCUMENTED DEVIATION**, which the
matrix's own translation table maps to **AD** in `compatibility-matrix.md` and
**"(see BD rows)"** in `feature-tracking.md`; the deviation row is **BD-17** in
`docs/BEHAVIORAL_DEVIATIONS.md` (status ACCEPTED, cause = D-011 item 6). It is
deliberately **not** `VERIFIED` / `IT` / `DONE`: a feature the original never
had cannot carry a parity claim, and `feature-matrix.csv` is regenerated from
the markdown so the CSV carries the same status.

## Code shape

```
recipes/RecipeDisplay.java            browser-independent view of one row (core, no JEI types)
recipes/RecipeViewerLayout.java       panel geometry + slot capacities (core, no JEI types)
client/jei/ThaumcraftJeiPlugin.java   @JeiPlugin entry point — discovered BY JEI
client/jei/ThaumcraftRecipeCategory.java + 4 categories   thin renderers over RecipeDisplay
client/jei/JeiRecipeSource.java       Minecraft-only seam that reads the client RecipeManager
gametest/JeiRecipeCategoryGameTest.java  server-side proof (no JEI class is touched)
```

`client/jei` is the **only** package allowed to reference a `mezz.jei` type. The
direction is enforced on compiled bytecode (`JeiPluginIsolationTest`: a
constant-pool/UTF-8 scan of every `.class` outside the package — the same
technique as `CuriosBridgeIsolationTest`), because the load-safety argument
depends on it: JEI discovers the plugin through `@JeiPlugin`, so with no JEI
installed nothing loads that package and the port's load path is the pre-card
path.

## Dependency shape (the Curios precedent, copied exactly)

| Aspect | Value |
| --- | --- |
| Version pin | `jei_version=19.64.0.454` in `gradle.properties` (single source; asserted equal to the `tc.jeiVersion` system property by `JeiPluginIsolationTest.versionPinIsSingleSource`) |
| Compile | `compileOnly` on `jei-1.21.1-common-api` + `jei-1.21.1-neoforge-api` (permanent; the plugin package compiles against them) |
| Development run | `localRuntime jei-1.21.1-neoforge` behind `if (!project.hasProperty('tc.noJei'))` |
| Tests | `testImplementation` on both API jars, **unconditional**, so the guards run on both legs |
| Published/bundled | **never** — no `implementation`, no `runtimeOnly`, no `api`, no jar in the artifact; `JeiPluginIsolationTest.jeiIsCompileOnlyAndLocalRuntimeOnly` reads `build.gradle` and fails on any other configuration |
| Manifest | `[[dependencies.thaumcraft]] modId="jei" type="optional" side="CLIENT" versionRange="[19.64.0.454,)"` (`JeiPluginIsolationTest.manifestDeclaresJeiOptionalOnTheClient` reads the generated `neoforge.mods.toml`) |
| Repo | `https://maven.blamejared.com/` (JEI's own Maven) |

**The pin is the exact build the target instance ships**, verified on 2026-11-02
against the user's primary install:
`~/.local/share/PrismLauncher/instances/All the Mods 10 - ATM10/minecraft/mods/jei-1.21.1-neoforge-19.64.0.454.jar`
(1.21.1 / NeoForge). The API is pinned to what the plugin will actually meet at
runtime, not to a floating range.

**Licensing (recorded alongside D-011 and D-016):** JEI is third-party software
by MEZ. This project takes it as a compile-time API plus a runtime-optional
dependency and **never redistributes its jar** — the shipped artifact stays
code-only (the alpha.4 artifact policy), and no JEI asset, string or source is
copied into this repository. See `docs/licensing-and-attribution.md`.

## The tolerance contract (the card's definition-of-done clause)

The arcane rows carry a `neoforge:item_exists` gate on items whose owning card
has not registered them, so a row can disappear, and an ingredient can resolve
to **no item at all**. A recipe viewer must draw that row, not throw. The three
live shapes:

1. **Empty ingredient.** A `#tag` ingredient over a tag that is empty at runtime
   survives the item-exists gate with zero stacks → `Entry.resolvable = false`,
   the slot draws empty. Measured on this tree: **103** unresolvable input
   entries across the live rows.
2. **No item output, by design.** Every `thaumcraft:infusion_enchantment` row
   crafts in place (central item + one enchantment level,
   `InfusionEnchantmentRecipe.java`:163-212) → no output slot at all. Measured:
   **24** rows, **24** unresolvable outputs.
3. **The original's NBT-pair infusion output** (golem placer, masks;
   `TileInfusionMatrix.java`:510-519 applies the pair as a data component on the
   infused item, so the craft's result *is* its input). Those rows are gated out
   of the live pack today (`neoforge:item_exists` on items a later card owns),
   so the server-side probe measures `infusion outputs=0`; the shape is pinned
   by the hand-built `RecipeDisplayToleranceTest` and the game-test walk picks
   it up automatically once the items register.

Measured on this tree, the shipped data pack holds **254** recipe JSON files
(the card body's "255 rows" is 254 files on this branch: 109
`thaumcraft:arcane_workbench`, 63 `thaumcraft:infusion`, 54
`thaumcraft:crucible`, 24 `thaumcraft:infusion_enchantment` = **250 custom
rows**, plus 4 `minecraft:crafting_shaped` the port registers through vanilla).
The **live** rows the categories can draw on the game-test server: arcane 41,
crucible 30, infusion 18, enchantment 24 — **113** rows; the rest are gated out
and JEI shows nothing for them, which is the correct behaviour for unported
content. `./gradlew test` runs **394** unit tests (0 failures, 1 skipped) and
`runGameTestServer` runs **127** required game tests.

## Proved, and not proved

Proved on this card, with the numbers above:

- `JeiRecipeCategoryGameTest.everyLiveRecipeRowRenders` — every live row of all
  four types builds a display, every slot fits `RecipeViewerLayout`, unresolvable
  entries are counted and required to exist. It ran **red first** on this card
  with `1 required tests failed: everylivereciperowrenders`, because the
  first draft asserted a tolerance case (an infusion row with a missing output)
  that the live data does not contain; the assertion was re-measured against the
  data, not deleted.
- `JeiRecipeCategoryGameTest.categoryTitlesAreTheOriginalRecipeTypeKeys` —
  exactly four `recipe.type.*` keys shipped, each with the original string.
- `RecipeDisplayToleranceTest` (unit, 10 tests) — the three tolerance shapes
  built by hand, the grid-cell layout, the research-label line, the layout
  capacities.
- `JeiPluginIsolationTest` (unit, 5 tests) — bytecode isolation, the
  compileOnly/localRuntime/never-bundled rule read out of `build.gradle`, the
  single-source version pin, the optional+CLIENT manifest.
- Both load legs: `./gradlew runGameTestServer` (JEI dev jar on `localRuntime`)
  and `./gradlew -Ptc.noJei runGameTestServer` (no JEI jar at all). CI carries
  the second leg as its own job, **`game-test-no-jei`**, beside the existing
  `game-test-no-curios`, each asserting the "required tests passed" line.

**Not proved in this harness, stated plainly:** the pixels. The game-test
harness is a dedicated server; JEI's plugin discovery is client-side
(`RegisterPluginsEvent`/`IModPlugin`), so no probe on this board ever calls
`registerCategories`/`setRecipe`. Slot positions, the arrow, the tooltip text
and the catalyst bindings (which need a booted registry for
`new ItemStack(Catalog…ITEM.get())`) require the ATM10 **client** to observe.
The server-side half proves the data half: if a live row's display builds and
its slots fit the declared panel, that row's JEI page has content.

## Gate flakiness observed on this branch (inherited, not this lane)

The lane's own gates are green (394 unit / 127 game tests, all legs). While
repeating the game-test legs to characterise the branch, **5 runs of
`runGameTestServer` produced 4 green and 1 red, and CI's
`game-test-no-curios` job was red** — every failure in a probe that predates
this card, in a system this card does not touch:

| Probe | Leg | Failure | Mechanism |
| --- | --- | --- | --- |
| `NodeBehaviorGameTest.productionAndDeathSpiralCycleObservedOverSimulatedTicks` | CI `game-test-no-curios` (125-test runs on `main` and on the SPINE-2 parent are green) | `death spiral must degrade the empty aspect's base by exactly one, got 8` | the spiral degrades **every empty-container aspect's** base by one and the `random.nextInt(20)` roll breaks out after the first (`NodeRuntime.java`:413-433). One empty aspect ⇒ base 9, two (the produced unit discharged between 600 and 1200) ⇒ base 8. The draw comes from the **shared level RNG**, whose stream depends on how many other game tests are running in the same tick, so this probe pins a single RNG draw as its expected value. |
| `OreDropGameTest.silkTouchDropsTheBlockWithItsVariant` | local default leg (run 5 of 5) | `a silk-touched v0 ore drops exactly one stack, got 2 [1 minecraft:stone_button, 1 thaumcraft:blockcustomore]` | the drop collection is taken from a region that also contains a template `stone_button`; under some interleavings the button's drop lands in the same collection. Fixture-order sensitive. |

This lane's two probes draw from no level RNG and no block-breaking region, so
they are not the cause; adding game tests shifts the interleaving, which is why
the failure surfaced on the 127-test branch and not on the 125-test parent. Both
probes need determinism work in their own cards (pin the level RNG; scope the
drop capture to the broken block), filed as **t_7bf6a4d0** (node death-spiral)
and **t_20ad0211** (ore silk-touch drop capture). **No probe was loosened on
this card** — an over-strict assertion in
someone else's probe is that card's finding to make, not this one's to paper
over.

## Observing it (the test-copy caveat)

The disposable test copy `~/minecraft mods/atm10-test/mods` holds **0**
browser-mod jars (`ls | grep -icE 'jei|emi|rtroughly'` → 0, measured 2026-11-02),
so the user cannot see this lane — or any JEI behaviour — there at all. To
observe it: install the **same** JEI build the primary instance ships
(`jei-1.21.1-neoforge-19.64.0.454.jar`) into the disposable test copy, or test
in the primary instance. **Never modify the primary instance**
(`~/.local/share/PrismLauncher/instances/All the Mods 10 - ATM10/`) as a side
effect of a card: it is the user's play install and the reference for the API
pin.

## Left out of scope (own cards, not this one)

- **Recipe transfer** (JEI `+/x` ghost recipes, `IRecipeTransferHandler` for the
  workbench grid and the pedestal layout) — needs the machine menus' slot
  contracts, which are GUI cards.
- **Aspect cost icons** in the category panels: the cost lines are text today
  (`RecipeDisplay.AspectCost` carries tag + label + amount); drawing the
  original aspect tag needs the ARR overlay sprites (D-004) and the `drawTag`
  seam from TC-M7-04-m2d.
- **Catalyst bindings beyond the four machines** (the original's
  `ItemResource`/wand routes into the workbench page) — needs the wand item.
- **Per-variant rows** (meta-indexed items show one stack per ingredient, the
  TC-M7-01b follow-up).
