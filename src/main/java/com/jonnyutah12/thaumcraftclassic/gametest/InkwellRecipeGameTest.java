package com.jonnyutah12.thaumcraftclassic.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.items.ItemPhial;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M3ResearchContent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * TC-M3-04-s-vi: the Scribing Tools acquisition recipes. The original ships
 * the inkwell with three shapeless rows
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/config/ConfigRecipes.java:3971-3982}):
 *
 * <pre>
 * "Scribe1": itemEssence(1,0) + Items.field_151008_G + "dyeBlack"   - :3971-3975
 * "Scribe2": Items.field_151069_bo + Items.field_151008_G + "dyeBlack" - :3976-3977
 * "Scribe3": itemInkwell(1,32767) + "dyeBlack"                        - :3978-3982
 * </pre>
 *
 * <p>MCP ({@code reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv}):
 * {@code field_151008_G} = feather (:1610), {@code field_151069_bo} =
 * glass_bottle (:1671). The {@code shapelessOreDictRecipe} helper
 * ({@code ConfigRecipes.java:4091-4095}) registers every row into the
 * CraftingManager at mod load, so none of them is research-gated. The port
 * ships Scribe1 as {@code recipe.scribe1.crafting_shapeless.json} (empty
 * phial — {@code thaumcraft:item_meta} 0, D-001/D-015 — + feather + the
 * {@code thaumcraft:oredict.dyeblack} tag) and Scribe2 as
 * {@code recipe.scribe2.crafting_shapeless.json} (glass bottle + feather +
 * the same tag). Scribe3 is the 16-colour recolor row: the port's
 * {@code items/ItemInkwell} declares no subtypes (the original's
 * {@code setHasSubtypes(false)} lineage, BD-19), so there is no colour
 * family to recolor and no row ships — {@code theInkwellHasNoRecolorRowUntilTheMetaFamilyLands}
 * pins that scope.</p>
 *
 * <p>Four probes, in the recipe family's house pattern (the crucible/infusion
 * invariants — see {@code InfusionAltarGameTest.assertEveryLoadableRowLoaded}
 * and {@code KnowledgeFragmentRecipeGameTest}):</p>
 *
 * <ul>
 *   <li>{@link #everyUngatedVanillaCraftingRowIsLoaded}: every shipped
 *       {@code .crafting_*} row whose items all exist must be live in the
 *       {@link RecipeManager} under its own id — the decode gate that keeps
 *       the {@code runServer} smoke job green.</li>
 *   <li>{@link #theEmptyPhialRowCraftsTheInkwell}: a phial carrying
 *       {@code item_meta} 0 + feather + black dye matches the Scribe1 row
 *       and assembles exactly a fresh inkwell (full 100 durability,
 *       unstackable, 0 leftovers).</li>
 *   <li>{@link #theGlassBottleRowCraftsTheInkwellWithInkSac}: the Scribe2
 *       grid crafts with ink sac — 1.7.10's {@code dyeBlack} was dye meta 0,
 *       the ink sac — and with black dye, the modern half of that identity.</li>
 *   <li>{@link #theScribeIngredientsArePrecise}: a filled phial, an
 *       un-component phial, a blue dye grid and a feather-less grid all
 *       craft nothing, with the two positive grids as controls.</li>
 * </ul>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InkwellRecipeGameTest {

    /** {@code data/thaumcraft/recipe/recipe.scribe1.crafting_shapeless.json}. */
    private static final ResourceLocation SCRIBE1 =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "recipe.scribe1.crafting_shapeless");
    /** {@code data/thaumcraft/recipe/recipe.scribe2.crafting_shapeless.json}. */
    private static final ResourceLocation SCRIBE2 =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "recipe.scribe2.crafting_shapeless");
    private InkwellRecipeGameTest() {
    }

    private static final String SHAPE = "minecraft:crafting_shaped";
    private static final String SHAPELESS = "minecraft:crafting_shapeless";

    // -------------------------------------------------------- the data invariant

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void everyUngatedVanillaCraftingRowIsLoaded(final GameTestHelper helper) {
        final Level level = helper.getLevel();
        final var resources = level.getServer().getResourceManager();
        final RecipeManager manager = level.getRecipeManager();

        final Set<String> live = new HashSet<>();
        for (final RecipeHolder<CraftingRecipe> row : manager.getAllRecipesFor(RecipeType.CRAFTING)) {
            live.add(row.id().toString());
        }

        int shipped = 0;
        int gatedOut = 0;
        int liveRows = 0;
        int componentIngredients = 0;
        final List<String> dropped = new ArrayList<>();
        final List<String> badIngredientForm = new ArrayList<>();

        for (final var entry : resources.listResources("recipe", path ->
                path.getNamespace().equals(ThaumcraftClassic.MOD_ID)
                        && path.getPath().contains(".crafting_")).entrySet()) {
            final String qualified = entry.getKey().getPath();
            final String file = qualified.startsWith("recipe/")
                    ? qualified.substring("recipe/".length()) : qualified;
            final String rowId = file.endsWith(".json")
                    ? file.substring(0, file.length() - ".json".length()) : file;
            shipped++;

            final JsonObject json;
            try (var reader = entry.getValue().openAsReader()) {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (final IOException failure) {
                helper.fail("unreadable crafting recipe resource " + entry.getKey()
                        + ": " + failure);
                return;
            }
            final String type = json.get("type").getAsString();
            helper.assertTrue(SHAPE.equals(type) || SHAPELESS.equals(type),
                    "the crafting-family row " + rowId + " names type " + type
                            + " — this probe only covers the vanilla crafting table rows");

            for (final JsonObject ingredient : ingredientsOf(json, type)) {
                if (ingredient.has("components") || ingredient.has("neoforge:components")) {
                    componentIngredients++;
                    if (!"neoforge:components".equals(optString(ingredient, "type"))
                            || !ingredient.has("items") || !ingredient.get("items").isJsonArray()
                            || ingredient.getAsJsonArray("items").isEmpty()) {
                        badIngredientForm.add(rowId);
                    }
                }
            }

            boolean rowGated = false;
            final JsonElement conditions = json.get("neoforge:conditions");
            if (conditions != null && conditions.isJsonArray()) {
                for (final var condition : conditions.getAsJsonArray()) {
                    final JsonObject guard = condition.getAsJsonObject();
                    final String guardType = guard.get("type").getAsString();
                    if ("neoforge:item_exists".equals(guardType)) {
                        if (!BuiltInRegistries.ITEM.containsKey(
                                ResourceLocation.parse(guard.get("item").getAsString()))) {
                            rowGated = true; // legitimately skipped at load
                        }
                    } else {
                        rowGated = true; // a gate this probe cannot evaluate
                    }
                }
            }
            if (rowGated) {
                gatedOut++;
                continue;
            }
            if (live.contains(ThaumcraftClassic.MOD_ID + ":" + rowId)) {
                liveRows++;
            } else {
                dropped.add(rowId);
            }
        }

        helper.assertTrue(badIngredientForm.isEmpty(),
                "rows using a meta-carrying ingredient that is not the registered "
                        + "neoforge:components Ingredient form (items must be a list and the "
                        + "\"type\" key is mandatory): " + badIngredientForm);
        helper.assertTrue(dropped.isEmpty(),
                "rows whose every item exists were dropped by the RecipeManager — a row that "
                        + "cannot decode is logged as 'Parsing error loading recipe' and the "
                        + "runServer smoke job fails on it: " + dropped);
        helper.assertTrue(live.contains(SCRIBE1.toString()),
                "the Scribe1 row " + SCRIBE1 + " gates on items the port registers "
                        + "(thaumcraft:itemessence + minecraft:feather + thaumcraft:iteminkwell), "
                        + "so it must be live in the RecipeManager; live crafting rows: " + live.size());
        helper.assertTrue(live.contains(SCRIBE2.toString()),
                "the Scribe2 row " + SCRIBE2 + " gates on vanilla items and the registered "
                        + "inkwell, so it must be live; live crafting rows: " + live.size());

        ThaumcraftClassic.LOGGER.info(
                "[TC-M3-04-s-vi] Scribing Tools acquisition: {} vanilla crafting row files "
                        + "shipped, {} pass their neoforge:item_exists gate and are live in the "
                        + "RecipeManager, {} gated out, {} ingredients use the registered "
                        + "neoforge:components form, {} live crafting-table recipes in total; "
                        + "{} and {} are live",
                shipped, liveRows, gatedOut, componentIngredients, live.size(), SCRIBE1, SCRIBE2);
        helper.succeed();
    }

    // ------------------------------------------------------------- the crafts

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void theEmptyPhialRowCraftsTheInkwell(final GameTestHelper helper) {
        final Level level = helper.getLevel();
        final RecipeManager manager = level.getRecipeManager();

        final CraftingInput grid = grid(ItemPhial.stack(ItemPhial.META_EMPTY),
                new ItemStack(Items.FEATHER), new ItemStack(Items.BLACK_DYE));
        final Optional<RecipeHolder<CraftingRecipe>> held =
                manager.getRecipeFor(RecipeType.CRAFTING, grid, level);
        helper.assertTrue(held.isPresent(),
                "an empty phial + feather + black dye must match a crafting recipe — the "
                        + "original's ConfigRecipes.java:3971-3975 Scribe1 row crafts nothing");
        final RecipeHolder<CraftingRecipe> row = held.get();
        helper.assertTrue(SCRIBE1.equals(row.id()),
                "the phial grid crafted " + row.id() + " instead of the Scribe1 row "
                        + SCRIBE1 + " — the original's empty phial row is Scribe1");

        final CraftingRecipe recipe = row.value();
        final ItemStack result = recipe.getResultItem(level.registryAccess());
        helper.assertTrue(result.is(M3ResearchContent.ITEM_INKWELL.get()),
                "the Scribe1 result is " + result.getItem() + ", the original's is "
                        + "new ItemStack(ConfigItems.itemInkwell) (ConfigRecipes.java:3972)");
        helper.assertTrue(result.getCount() == 1,
                "the original crafts one inkwell, not " + result.getCount());

        final ItemStack crafted = recipe.assemble(grid, level.registryAccess());
        helper.assertTrue(ItemStack.isSameItemSameComponents(crafted,
                        new ItemStack(M3ResearchContent.ITEM_INKWELL.get())),
                "the crafted inkwell is not a fresh stack: components " + crafted.getComponents());
        helper.assertTrue(crafted.getMaxDamage() == 100 && crafted.getDamageValue() == 0,
                "the crafted inkwell must start at full durability — the original's ctor is "
                        + "setMaxDamage(100) (ItemInkwell.java:23-29), it crafted "
                        + crafted.getDamageValue() + "/" + crafted.getMaxDamage());
        helper.assertTrue(crafted.getMaxStackSize() == 1,
                "the inkwell must stay unstackable (the original's field_77777_bU = 1)");

        int leftovers = 0;
        for (final ItemStack stack : recipe.getRemainingItems(grid)) {
            if (!stack.isEmpty()) {
                leftovers++;
            }
        }
        helper.assertTrue(leftovers == 0,
                "the phial, feather and dye are all consumed, got " + leftovers + " leftover(s)");

        ThaumcraftClassic.LOGGER.info(
                "[TC-M3-04-s-vi] Scribe1 craft: empty phial (item_meta {}) + feather + black "
                        + "dye -> {} x{} at {} durability, 0 leftovers",
                ItemPhial.META_EMPTY, SCRIBE1, result.getCount(),
                crafted.getMaxDamage() - crafted.getDamageValue());
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void theGlassBottleRowCraftsTheInkwellWithInkSac(final GameTestHelper helper) {
        final Level level = helper.getLevel();
        final RecipeManager manager = level.getRecipeManager();

        // 1.7.10's dyeBlack ore-dict entry was dye meta 0 = the INK SAC
        // (Items.dye, meta 0); 1.21.1 splits the identity into black_dye +
        // ink_sac, and the tag carries both, so the squid path crafts exactly
        // as it did in the original.
        for (final var dye : List.of(new ItemStack(Items.INK_SAC), new ItemStack(Items.BLACK_DYE))) {
            final CraftingInput grid = grid(new ItemStack(Items.GLASS_BOTTLE),
                    new ItemStack(Items.FEATHER), dye.copy());
            final Optional<RecipeHolder<CraftingRecipe>> held =
                    manager.getRecipeFor(RecipeType.CRAFTING, grid, level);
            helper.assertTrue(held.isPresent(),
                    "a glass bottle + feather + " + dye.getItem() + " must match a crafting "
                            + "recipe — the original's ConfigRecipes.java:3976-3977 Scribe2 row "
                            + "crafts nothing with this dye");
            helper.assertTrue(SCRIBE2.equals(held.get().id()),
                    "the " + dye.getItem() + " bottle grid crafted " + held.get().id()
                            + " instead of the Scribe2 row " + SCRIBE2);
            final ItemStack crafted = held.get().value()
                    .assemble(grid, level.registryAccess());
            helper.assertTrue(crafted.is(M3ResearchContent.ITEM_INKWELL.get())
                            && crafted.getCount() == 1,
                    "the Scribe2 result must be one fresh inkwell, got " + crafted);
        }

        // The tag resolves live with both modern halves of the dyeBlack identity.
        final var tagKey = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "oredict.dyeblack"));
        final var members = level.registryAccess().lookupOrThrow(Registries.ITEM).get(tagKey);
        helper.assertTrue(members.isPresent() && members.get().size() == 2,
                "thaumcraft:oredict.dyeblack must resolve with both black_dye and ink_sac, "
                        + "members: " + (members.isEmpty() ? "MISSING" : members.get().size()));
        helper.assertTrue(new ItemStack(Items.INK_SAC).is(tagKey),
                "the ink sac (1.7.10's dyeBlack, dye meta 0) is not in the tag");
        helper.assertTrue(new ItemStack(Items.BLACK_DYE).is(tagKey),
                "black dye (the modern dyeBlack) is not in the tag");

        ThaumcraftClassic.LOGGER.info(
                "[TC-M3-04-s-vi] Scribe2 craft: glass bottle + feather + ink sac / black dye "
                        + "-> " + SCRIBE2 + "; thaumcraft:oredict.dyeblack resolves with "
                        + members.get().size() + " members");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void theScribeIngredientsArePrecise(final GameTestHelper helper) {
        final Level level = helper.getLevel();
        final RecipeManager manager = level.getRecipeManager();

        // Positive controls: both shipped grids match, so every refusal below
        // is about the ingredient, not about the harness.
        helper.assertTrue(matches(manager, level, SCRIBE1,
                        grid(ItemPhial.stack(ItemPhial.META_EMPTY),
                                new ItemStack(Items.FEATHER), new ItemStack(Items.BLACK_DYE))),
                "the empty-phial control grid no longer matches the Scribe1 row");
        helper.assertTrue(matches(manager, level, SCRIBE2,
                        grid(new ItemStack(Items.GLASS_BOTTLE),
                                new ItemStack(Items.FEATHER), new ItemStack(Items.BLACK_DYE))),
                "the glass-bottle control grid no longer matches the Scribe2 row");

        // A FILLED phial (meta 1, carrying essentia) must never be eaten by
        // the row: the original's ingredient is ItemStack(itemEssence, 1, 0),
        // the empty phial only.
        helper.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING,
                        grid(ItemPhial.filled(Aspect.getAspect("aqua"), 8),
                                new ItemStack(Items.FEATHER), new ItemStack(Items.BLACK_DYE)),
                        level).isEmpty(),
                "a filled phial (item_meta 1) crafts the inkwell — the ingredient does not pin "
                        + "thaumcraft:item_meta " + ItemPhial.META_EMPTY + " and would eat the "
                        + "phial's essentia contents (the original's ShapelessOreRecipe meta 0)");

        // An un-component phial stack (the creative-tab / give shape, no
        // thaumcraft:item_meta component at all): the port's metas ride the
        // component (BD-20), so this stack matches no Scribe row. The original's
        // meta-0 ingredient matched damage-0 stacks; the port's meta-0 stack is
        // the one ItemPhial.stack(0) / the jar-drain return path builds.
        helper.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING,
                        grid(new ItemStack(
                                        BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(
                                                ThaumcraftClassic.MOD_ID, "itemessence"))),
                                new ItemStack(Items.FEATHER), new ItemStack(Items.BLACK_DYE)),
                        level).isEmpty(),
                "an un-component itemessence stack crafts the inkwell — the ingredient is a "
                        + "bare item, not the registered neoforge:components form, so a FILLED "
                        + "phial would craft too (the BD-20 component split keeps the meta pair "
                        + "precise)");

        // The dye is dyeBlack, not dyeBlue: the original's third ingredient is
        // the ore-dict name, and the port resolves it through the tag.
        helper.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING,
                        grid(ItemPhial.stack(ItemPhial.META_EMPTY),
                                new ItemStack(Items.FEATHER), new ItemStack(Items.BLUE_DYE)),
                        level).isEmpty(),
                "blue dye crafts the inkwell — the dyeBlack tag leaked a non-black dye");

        // No feather: the original's row needs all three ingredients (the
        // shapeless matcher consumes every slot it fires on).
        helper.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING,
                        grid(ItemPhial.stack(ItemPhial.META_EMPTY),
                                ItemStack.EMPTY, new ItemStack(Items.BLACK_DYE)),
                        level).isEmpty(),
                "a phial + dye grid without the feather crafts the inkwell");

        ThaumcraftClassic.LOGGER.info(
                "[TC-M3-04-s-vi] Scribe ingredient precision: the empty-phial and glass-bottle "
                        + "grids craft " + SCRIBE1 + " / " + SCRIBE2 + " while a filled phial, "
                        + "an un-component phial, blue dye and a feather-less grid all refuse");
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    /** The original's rows are shapeless: three ingredients, any layout. */
    private static CraftingInput grid(final ItemStack a, final ItemStack b, final ItemStack c) {
        final List<ItemStack> items = new ArrayList<>();
        items.add(a);
        items.add(b);
        items.add(c);
        while (items.size() < 9) {
            items.add(ItemStack.EMPTY);
        }
        return CraftingInput.of(3, 3, items);
    }

    private static boolean matches(final RecipeManager manager, final Level level,
                                   final ResourceLocation rowId, final CraftingInput grid) {
        final Optional<RecipeHolder<CraftingRecipe>> held =
                manager.getRecipeFor(RecipeType.CRAFTING, grid, level);
        return held.isPresent() && rowId.equals(held.get().id());
    }

    /** Every ingredient object a vanilla crafting row names. */
    private static List<JsonObject> ingredientsOf(final JsonObject json, final String type) {
        final List<JsonObject> out = new ArrayList<>();
        if (SHAPE.equals(type)) {
            for (final var key : json.getAsJsonObject("key").asMap().values()) {
                out.add(key.getAsJsonObject());
            }
        } else {
            for (final var ingredient : json.getAsJsonArray("ingredients")) {
                out.add(ingredient.getAsJsonObject());
            }
        }
        return out;
    }

    private static String optString(final JsonObject object, final String key) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsString() : null;
    }
}
