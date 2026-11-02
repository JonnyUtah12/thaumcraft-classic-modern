package com.jonnyutah12.thaumcraftclassic.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.recipes.RecipeDisplay;
import com.jonnyutah12.thaumcraftclassic.recipes.RecipeViewerLayout;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogArcaneWorkbench;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogCrucible;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogInfusion;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * SPINE-4 / t_aebd14cf: the four custom recipe categories have rows to show,
 * every live row renders, and a row that resolves to NO item renders too
 * instead of being dropped.
 *
 * <p>This is the server-side half of the JEI lane's proof. A game test may not
 * touch a {@code mezz.jei} type: the required-suite run has no JEI jar on its
 * classpath at all, which is precisely the "the mod loads identically without
 * JEI" state the card demands (JEI discovers the plugin, so with no JEI there
 * is nothing to discover and no {@code mezz.jei} class to link). The probes
 * therefore exercise {@code RecipeDisplay} — the layer the plugin renders
 * verbatim. If a live row's display builds and its slots fit the declared
 * panel, that row's JEI page has content.</p>
 *
 * <p><b>The rows are taken from the live {@link RecipeManager}</b>, per type,
 * the same call the port's own machines make ({@code TileArcaneWorkbench.java}:117,
 * {@code TileCrucible.java}:254, {@code TileInfusionMatrix.java}:213 and :233)
 * — so a row that the data pack gates out is not asserted here, and a row the
 * machine crafts must render. The expected counts are never hardcoded: the
 * probe sums the live rows across the four types and requires the total to be
 * positive, so no category can pass on a page it never populated.</p>
 *
 * <p><b>Tolerance is measured, not assumed.</b> The live pack carries
 * unresolvable ingredients — a {@code #tag} ingredient over a tag that is empty
 * at runtime survives the {@code neoforge:item_exists} gate with zero stacks —
 * and all 24 shipped {@code infusion_enchantment} rows have no item output at
 * all, because that recipe kind crafts in place (central item + one
 * enchantment level, {@code InfusionEnchantmentRecipe.java}:163-212). The probe
 * requires the unresolvable-input count and the enchantment no-output count to
 * be {@code > 0}: if those cases ever disappear from the data, the assertion
 * fails and says to re-measure rather than quietly deleting the teeth. The
 * third tolerance shape — the infusion rows whose output is the original's
 * {@code NBT} pair (golem placer, masks; {@code TileInfusionMatrix.java}:510-519
 * applies that pair as a data component on the infused item, so the craft has
 * no result item) — is gated out of the live pack today by
 * {@code neoforge:item_exists} on items a later card owns, so it is pinned by
 * the hand-built {@code RecipeDisplayToleranceTest} and picked up here
 * automatically once those items register.</p>
 *
 * <p>The four category titles are asserted as the ORIGINAL strings from the
 * shipped lang file — {@code recipe.type.arcane} = "Arcane Workbench",
 * {@code recipe.type.crucible} = "Crucible", {@code recipe.type.infusion} =
 * "Arcane Infusion", {@code recipe.type.infusionenchantment} = "Infusion
 * Enchantment" (reference {@code en_US.lang:660-663}, the keys the decompiled
 * {@code GuiResearchRecipe.java}:644, :969, :1162, :1355 use to head these same
 * four recipe kinds). D-016: verbatim, no invented label. A missing key renders
 * as the key itself in the JEI header, so this probe is the on-screen check.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class JeiRecipeCategoryGameTest {

    /** The four category title keys with the original {@code recipe.type.*} values. */
    static final Map<String, String> CATEGORY_TITLES = titles();

    private static Map<String, String> titles() {
        final Map<String, String> titles = new LinkedHashMap<>();
        titles.put("recipe.type.arcane", "Arcane Workbench");
        titles.put("recipe.type.crucible", "Crucible");
        titles.put("recipe.type.infusion", "Arcane Infusion");
        titles.put("recipe.type.infusionenchantment", "Infusion Enchantment");
        return Map.copyOf(titles);
    }

    private JeiRecipeCategoryGameTest() {
    }

    // ------------------------------------------------------------------ probes

    /**
     * Every live row of the four custom types builds a display, and every slot
     * it needs fits the panel its category declares
     * ({@link RecipeViewerLayout}). An unresolvable slot — the SPINE-4 tolerance
     * case — is counted, logged, and required to exist for the kinds that ship
     * it; it never drops a row.
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void everyLiveRecipeRowRenders(final GameTestHelper helper) {
        final RecipeManager recipes = helper.getLevel().getRecipeManager();

        final Counts arcane = walk(helper, "thaumcraft:arcane_workbench",
                recipes.getAllRecipesFor(CatalogArcaneWorkbench.ARCANE_WORKBENCH_TYPE.get()),
                RecipeViewerLayout.ARCANE_MAX_INPUTS, holder -> RecipeDisplay.of(holder.value()));
        final Counts crucible = walk(helper, "thaumcraft:crucible",
                recipes.getAllRecipesFor(CatalogCrucible.CRUCIBLE_TYPE.get()),
                RecipeViewerLayout.CRUCIBLE_MAX_INPUTS, holder -> RecipeDisplay.of(holder.value()));
        final Counts infusion = walk(helper, "thaumcraft:infusion",
                recipes.getAllRecipesFor(CatalogInfusion.INFUSION_TYPE.get()),
                RecipeViewerLayout.INFUSION_MAX_INPUTS, holder -> RecipeDisplay.of(holder.value()));
        // null registries on purpose: the display must build an enchantment row
        // with no registry access (the unregistered-target branch is the tolerant one).
        final Counts enchantment = walk(helper, "thaumcraft:infusion_enchantment",
                recipes.getAllRecipesFor(CatalogInfusion.INFUSION_ENCHANTMENT_TYPE.get()),
                RecipeViewerLayout.ENCHANTMENT_MAX_INPUTS,
                holder -> RecipeDisplay.of(holder.value(), null));

        final int rows = arcane.rows() + crucible.rows() + infusion.rows() + enchantment.rows();
        final int emptyInputs = arcane.emptyInputs() + crucible.emptyInputs()
                + infusion.emptyInputs() + enchantment.emptyInputs();
        final int emptyOutputs = arcane.emptyOutputs() + crucible.emptyOutputs()
                + infusion.emptyOutputs() + enchantment.emptyOutputs();
        ThaumcraftClassic.LOGGER.info(
                "[SPINE-4] jei lane live rows: arcane={} crucible={} infusion={} enchantment={} (total {});"
                        + " unresolvable inputs={} outputs={} (infusion outputs={}, enchantment outputs={})",
                arcane.rows(), crucible.rows(), infusion.rows(), enchantment.rows(), rows,
                emptyInputs, emptyOutputs, infusion.emptyOutputs(), enchantment.emptyOutputs());

        helper.assertTrue(rows > 0,
                "no custom recipe row is loaded at all — every category page would be empty");
        helper.assertTrue(emptyInputs > 0,
                "the SPINE-4 tolerance case must be live data, not a hand-built mock: " + rows
                        + " live rows must include at least one ingredient that resolves to no item"
                        + " (a #tag over an empty tag survives the item_exists gate with zero"
                        + " stacks). Today the count is " + emptyInputs
                        + "; if it ever reaches 0 the probe is toothless — re-measure, do not delete it");
        helper.assertTrue(enchantment.rows() > 0 && enchantment.emptyOutputs() == enchantment.rows(),
                "every infusion_enchantment row has no item output (it enchants the central item in"
                        + " place, InfusionEnchantmentRecipe.java:163-212); " + enchantment.rows()
                        + " rows vs " + enchantment.emptyOutputs() + " unresolvable outputs means a row"
                        + " grew an output this type cannot craft");
        // The infusion NBT-pair rows (golem placer, masks) are gated out of the
        // live pack today by neoforge:item_exists on the items that owning card
        // has not registered, so infusion.emptyOutputs() reads 0 here (measured
        // 2026-11-02: arcane=41 crucible=30 infusion=18 enchantment=24). When that
        // card lands the rows load and this walk covers them automatically; the
        // display path for that shape is pinned by RecipeDisplayToleranceTest.
        helper.succeed();
    }

    /**
     * The category headers are the original {@code recipe.type.*} keys, shipped
     * with the original values, exactly four of them.
     */
    @GameTest(template = "empty")
    public static void categoryTitlesAreTheOriginalRecipeTypeKeys(final GameTestHelper helper) {
        final JsonObject lang = loadLang();
        for (final Map.Entry<String, String> entry : CATEGORY_TITLES.entrySet()) {
            helper.assertTrue(lang.has(entry.getKey()),
                    "category title key " + entry.getKey() + " is not shipped in"
                            + " assets/thaumcraft/lang/en_us.json — the JEI header renders the key"
                            + " itself, not a label");
            final String value = lang.get(entry.getKey()).getAsString();
            helper.assertTrue(entry.getValue().equals(value),
                    "category title " + entry.getKey() + " = '" + value + "'; the original"
                            + " en_US.lang:660-663 value is '" + entry.getValue() + "' (D-016 verbatim)");
        }
        int recipeTypeKeys = 0;
        for (final String key : lang.keySet()) {
            if (key.startsWith("recipe.type.")) {
                recipeTypeKeys++;
            }
        }
        helper.assertTrue(recipeTypeKeys == CATEGORY_TITLES.size(),
                "the shipped file must carry exactly the four recipe.type.* labels this port has"
                        + " custom recipe types for; found " + recipeTypeKeys + ". The original ships"
                        + " eight (en_US.lang:657-664: workbench, smelting, workbenchshapeless,"
                        + " arcane, crucible, infusion, infusionenchantment, construct) — the other"
                        + " four head vanilla-shaped book pages, which have no custom recipe type");
        ThaumcraftClassic.LOGGER.info("[SPINE-4] jei category titles verified against the original"
                + " recipe.type.* keys ({} keys, {} shipped)", CATEGORY_TITLES.size(), recipeTypeKeys);
        helper.succeed();
    }

    // ----------------------------------------------------------------- helpers

    /** Per-type tallies measured over the live rows, so the log line is auditable. */
    private record Counts(int rows, int emptyInputs, int emptyOutputs) {
    }

    /**
     * Build every row's display and check the tolerance contract on it. A row
     * that throws is reported with its id and kept in the count — the probe must
     * name the row, and a row that renders with nothing in it is legal.
     */
    private static <R extends Recipe<?>> Counts walk(final GameTestHelper helper, final String type,
            final List<RecipeHolder<R>> rows, final int maxInputs,
            final Function<RecipeHolder<R>, RecipeDisplay> build) {
        int emptyInputs = 0;
        int emptyOutputs = 0;
        final List<String> failures = new ArrayList<>();
        for (final RecipeHolder<R> holder : rows) {
            final RecipeDisplay display;
            try {
                display = build.apply(holder);
            } catch (final RuntimeException e) {
                failures.add("row " + holder.id() + " throws while building its display: " + e);
                continue;
            }
            if (display.inputs().size() > maxInputs) {
                failures.add("row " + holder.id() + " needs " + display.inputs().size()
                        + " input slots, over the panel capacity " + maxInputs
                        + " — RecipeViewerLayout must grow before the row ships");
            }
            for (final RecipeDisplay.Entry entry : display.inputs()) {
                if (!entry.resolvable()) {
                    emptyInputs++;
                } else if (entry.stacks().isEmpty()) {
                    failures.add("row " + holder.id() + ": entry " + entry.name()
                            + " is marked resolvable with zero stacks");
                }
            }
            if (!display.output().resolvable()) {
                emptyOutputs++;
            } else if (display.output().stacks().isEmpty()) {
                failures.add("row " + holder.id() + ": output marked resolvable with zero stacks");
            }
        }
        helper.assertTrue(failures.isEmpty(),
                type + ": " + failures.size() + " of " + rows.size() + " live rows fail to render: "
                        + failures);
        return new Counts(rows.size(), emptyInputs, emptyOutputs);
    }

    /**
     * The shipped lang file, off the classpath — the same read
     * {@code LocalizationGameTest.java}:414 and {@code CreativeTabGameTest} use,
     * so the probe sees the resource the shipped jar carries.
     */
    private static JsonObject loadLang() {
        try (InputStream in = JeiRecipeCategoryGameTest.class.getResourceAsStream(
                "/assets/" + ThaumcraftClassic.MOD_ID + "/lang/en_us.json")) {
            if (in == null) {
                throw new IllegalStateException("assets/thaumcraft/lang/en_us.json is not on the classpath");
            }
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
