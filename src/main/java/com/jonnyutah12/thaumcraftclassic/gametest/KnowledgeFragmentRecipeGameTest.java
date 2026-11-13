package com.jonnyutah12.thaumcraftclassic.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.items.ItemResearchNotes;
import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M3ResearchContent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
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
 * TC-M3-04-s-iv: the KnowFrag recipe — the note's only survival acquisition
 * path. The original registers exactly one recipe for
 * {@code itemResearchNotes} ({@code ConfigRecipes.java:3860-3865}): a 3x3 of
 * {@code itemResource} meta 9 (knowledge fragments) into one meta-42 blank
 * discovery note. The port ships it as
 * {@code data/thaumcraft/recipe/recipe.256.crafting_shaped.json}, a
 * {@code minecraft:crafting_shaped} row whose meta-carrying ingredient is the
 * registered {@code neoforge:components} Ingredient (D-015) and whose result
 * carries {@code thaumcraft:item_meta: 42}.
 *
 * <p>Three probes, in the family's house pattern (the crucible/infusion/arcane
 * row invariants — see {@code CrucibleGameTest.everyUngatedCrucibleRowIsLoaded}
 * and {@code InfusionAltarGameTest.assertEveryLoadableRowLoaded}):</p>
 *
 * <ul>
 *   <li>{@link #everyUngatedVanillaCraftingRowIsLoaded}: every shipped vanilla
 *       crafting row whose items all exist must be in the live
 *       {@link RecipeManager} under its own id. A row that fails to parse is
 *       dropped with a {@code Parsing error loading recipe} ERROR, and the CI
 *       {@code runServer} smoke job fails on any boot-log line naming
 *       thaumcraft next to an ERROR, so this is the gate that keeps the family
 *       decodeable — not an "is the list non-empty" assertion.</li>
 *   <li>{@link #nineKnowledgeFragmentsCraftTheBlankDiscoveryNote}: a real 3x3
 *       grid of nine fragments matches the KnowFrag row, and its result is the
 *       original's meta-42 blank — the same stack
 *       {@link ItemResearchNotes#blankDiscoveryNote(int)} builds, with an
 *       EMPTY note state (the original's meta-42 blank carries no hexgrid, so
 *       the {@code thaumcraft:research_note} component is absent).</li>
 *   <li>{@link #theFragmentIngredientIsMetaPrecise}: the ingredient is the
 *       component-carrying form, not a bare {@code item}. A bare one would
 *       accept every {@code itemresource} meta, so amber, nitor, a plain
 *       un-component stack, eight fragments, and one stack of nine must all
 *       refuse to craft, while the nine-fragment grid crafts (the positive
 *       control that keeps the negatives from being vacuous).</li>
 * </ul>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KnowledgeFragmentRecipeGameTest {

    /** {@code data/thaumcraft/recipe/recipe.256.crafting_shaped.json}. */
    private static final ResourceLocation ROW_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "recipe.256.crafting_shaped");
    private static final String SHAPE = "minecraft:crafting_shaped";
    private static final String SHAPELESS = "minecraft:crafting_shapeless";

    private KnowledgeFragmentRecipeGameTest() {
    }

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
        final List<String> knowFragFacts = new ArrayList<>();

        for (final var entry : resources.listResources("recipe", path ->
                path.getNamespace().equals(ThaumcraftClassic.MOD_ID)
                        && path.getPath().contains(".crafting_")).entrySet()) {
            // listResources keeps the folder-qualified path with its extension;
            // data/thaumcraft/recipe/<name>.json loads as thaumcraft:<name>.
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

            // D-015: a meta-carrying ingredient is the REGISTERED
            // neoforge:components Ingredient, with "type" present and items as
            // a list. A bare {"neoforge:components": {...}} object is malformed
            // JSON to the Ingredient codec and drops the whole row.
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
            if (ROW_ID.getPath().equals(rowId)) {
                knowFragFacts.add("pattern=" + json.getAsJsonArray("pattern")
                        + " result=" + json.getAsJsonObject("result"));
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
        helper.assertTrue(live.contains(ROW_ID.toString()),
                "the KnowFrag row " + ROW_ID + " gates on items the port registers ("
                        + "thaumcraft:itemresource + thaumcraft:itemresearchnotes), so it must "
                        + "be live in the RecipeManager; live crafting rows: " + live.size());
        helper.assertTrue(!knowFragFacts.isEmpty() && knowFragFacts.get(0).contains("\"KKK\"")
                        && knowFragFacts.get(0).contains("42"),
                "the shipped KnowFrag row drifted from the original's 3x3 K grid into meta 42: "
                        + knowFragFacts);
        // The original's row is a crafting-table row: it must not be reachable
        // in the 2x2 player grid, which is why the port does not shrink it.
        final Optional<RecipeHolder<CraftingRecipe>> row = findKnowFrag(manager);
        helper.assertTrue(row.isPresent() && !row.get().value().canCraftInDimensions(2, 2),
                "the KnowFrag pattern is 3x3, so a 2x2 grid must not be able to craft it");

        ThaumcraftClassic.LOGGER.info(
                "[TC-M3-04-s-iv] crafting-table data: {} vanilla crafting row files shipped, "
                        + "{} pass their neoforge:item_exists gate and are live in the "
                        + "RecipeManager, {} gated out, {} ingredients use the registered "
                        + "neoforge:components form, {} live crafting-table recipes in total; "
                        + "the KnowFrag row {} is live",
                shipped, liveRows, gatedOut, componentIngredients, live.size(), ROW_ID);
        helper.succeed();
    }

    // ------------------------------------------------------------- the craft

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nineKnowledgeFragmentsCraftTheBlankDiscoveryNote(final GameTestHelper helper) {
        final Level level = helper.getLevel();
        final RecipeManager manager = level.getRecipeManager();
        final var registries = level.registryAccess();

        final CraftingInput grid = fragmentGrid(9, ItemResource.META_KNOWLEDGE_FRAGMENT, 1);
        final Optional<RecipeHolder<CraftingRecipe>> held =
                manager.getRecipeFor(RecipeType.CRAFTING, grid, level);
        helper.assertTrue(held.isPresent(),
                "a 3x3 grid of nine knowledge fragments (itemresource meta "
                        + ItemResource.META_KNOWLEDGE_FRAGMENT + ") must match a crafting recipe — "
                        + "the original's ConfigRecipes.java:3860-3865 KnowFrag row crafts nothing");
        final RecipeHolder<CraftingRecipe> row = held.get();
        helper.assertTrue(ROW_ID.equals(row.id()),
                "the nine-fragment grid crafted " + row.id() + " instead of the KnowFrag row "
                        + ROW_ID + " — the original has exactly one note recipe");

        final CraftingRecipe recipe = row.value();
        final ItemStack result = recipe.getResultItem(registries);
        helper.assertTrue(result.is(M3ResearchContent.ITEM_RESEARCH_NOTES.get()),
                "the KnowFrag result is " + result.getItem() + ", the original's is "
                        + "new ItemStack(ConfigItems.itemResearchNotes, 1, 42)");
        helper.assertTrue(result.getCount() == 1,
                "the original crafts one note, not " + result.getCount());
        helper.assertTrue(ItemResearchNotes.storedMeta(result)
                        == ItemResearchNotes.META_BLANK_DISCOVERY_B,
                "the crafted note carries meta " + ItemResearchNotes.storedMeta(result)
                        + ", the original's output is meta "
                        + ItemResearchNotes.META_BLANK_DISCOVERY_B);
        helper.assertTrue(ItemResearchNotes.isBlankDiscoveryMeta(ItemResearchNotes.storedMeta(result)),
                "the crafted note is not one of the original's blank discovery metas 24/42");

        // assemble() is what the crafting table puts in the result slot.
        final ItemStack crafted = recipe.assemble(grid, registries);
        final ItemStack expected = ItemResearchNotes.blankDiscoveryNote(
                ItemResearchNotes.META_BLANK_DISCOVERY_B);
        helper.assertTrue(ItemStack.isSameItemSameComponents(crafted, expected),
                "the crafted blank is not the stack ItemResearchNotes.blankDiscoveryNote(42) "
                        + "builds: crafted " + crafted + " components " + crafted.getComponents()
                        + " vs builder " + expected + " components " + expected.getComponents());

        // The original's meta-42 blank has NO hexgrid: ResearchManager.getData
        // returns null for it, which is why right-click rolls findHiddenResearch
        // instead of finishing a puzzle. The port's note state is the
        // thaumcraft:research_note component, so the crafted blank has none.
        helper.assertTrue(crafted.get(ModDataComponents.RESEARCH_NOTE.get()) == null,
                "the crafted blank carries a research_note state — the original's meta-42 "
                        + "blank has no hexgrid (ResearchManager.getData is null for it)");

        // The note family's rarity (the original's getRarity: rare below meta 64).
        helper.assertTrue(crafted.getRarity() == Rarity.RARE,
                "the crafted blank is " + crafted.getRarity() + ", the original's getRarity "
                        + "returns rare for every meta below 64");
        helper.assertTrue(crafted.getMaxStackSize() == 1,
                "the note must stay unstackable (the original's setMaxStackSize(1))");

        // The grid is consumed: nine fragments in, nothing left over (no
        // crafting-remaining-item on a fragment).
        final var remaining = recipe.getRemainingItems(grid);
        int leftovers = 0;
        for (final ItemStack stack : remaining) {
            if (!stack.isEmpty()) {
                leftovers++;
            }
        }
        helper.assertTrue(leftovers == 0,
                "the nine fragments must be consumed with no leftovers, got " + leftovers);

        ThaumcraftClassic.LOGGER.info(
                "[TC-M3-04-s-iv] KnowFrag craft: nine itemresource meta {} -> {} meta {} "
                        + "(blank discovery note, empty note state, rarity {}, {} crafted "
                        + "component(s) on the stack, 0 leftovers)",
                ItemResource.META_KNOWLEDGE_FRAGMENT, ROW_ID,
                ItemResearchNotes.META_BLANK_DISCOVERY_B, crafted.getRarity(),
                crafted.getComponents().size());
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void theFragmentIngredientIsMetaPrecise(final GameTestHelper helper) {
        final Level level = helper.getLevel();
        final RecipeManager manager = level.getRecipeManager();

        // Positive control first: the same grid the craft probe uses matches,
        // so every refusal below is about the ingredient, not about the grid.
        helper.assertTrue(matches(manager, level,
                        fragmentGrid(9, ItemResource.META_KNOWLEDGE_FRAGMENT, 1)),
                "the nine-fragment control grid no longer matches the KnowFrag row");

        // A stack of the right meta with no thaumcraft:item_meta component: the
        // port's metas ride the component, so a bare item ingredient (the form
        // the vanilla extractor emits) would match this. It must not.
        final List<ItemStack> plain = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            plain.add(new ItemStack(CatalogResources.ITEM_RESOURCE.get()));
        }
        helper.assertTrue(!matches(manager, level, CraftingInput.of(3, 3, plain)),
                "an un-component itemresource stack crafts the note — the ingredient is a bare "
                        + "item, not the registered neoforge:components form, so every "
                        + "itemresource meta would craft notes");

        // Every other itemresource meta must refuse: meta 6 (amber) and meta 1
        // (nitor) are the two metas the OTHER shipped crafting_shaped rows put
        // on a result stack, so they exist and are reachable.
        for (final int meta : List.of(1, 6, ItemResource.META_JAR_LABEL)) {
            helper.assertTrue(!matches(manager, level, fragmentGrid(9, meta, 1)),
                    "nine itemresource meta " + meta + " craft the note — the ingredient does "
                            + "not pin thaumcraft:item_meta 9");
        }

        // Eight fragments of the right meta: the original's row is a full 3x3,
        // so a hole in the grid crafts nothing (no trimmed shape fits a 3x3
        // pattern).
        final List<ItemStack> eight = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            eight.add(i == 0 ? ItemStack.EMPTY
                    : ItemResource.stack(ItemResource.META_KNOWLEDGE_FRAGMENT));
        }
        helper.assertTrue(!matches(manager, level, CraftingInput.of(3, 3, eight)),
                "eight fragments in a 3x3 grid craft the note — the original needs nine");

        // One stack of nine in a single slot: the original's shaped row reads
        // nine grid slots, not one stack of nine.
        final List<ItemStack> oneStack = new ArrayList<>();
        oneStack.add(ItemResource.stack(ItemResource.META_KNOWLEDGE_FRAGMENT).copy());
        oneStack.get(0).setCount(9);
        for (int i = 1; i < 9; i++) {
            oneStack.add(ItemStack.EMPTY);
        }
        helper.assertTrue(!matches(manager, level, CraftingInput.of(3, 3, oneStack)),
                "one stack of nine fragments crafts the note — the row is 3x3 of nine slots");

        ThaumcraftClassic.LOGGER.info(
                "[TC-M3-04-s-iv] KnowFrag ingredient precision: the 3x3 grid of itemresource "
                        + "meta {} matches " + ROW_ID + " while a bare itemresource stack, metas "
                        + "1/6/13, eight fragments and one stack of nine all refuse",
                ItemResource.META_KNOWLEDGE_FRAGMENT);
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    /** A width x height grid (3x3 here) of {@code count} fragments of {@code meta}. */
    private static CraftingInput fragmentGrid(final int slots, final int meta, final int count) {
        final List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < slots; i++) {
            final ItemStack stack = ItemResource.stack(meta);
            stack.setCount(count);
            items.add(stack);
        }
        return CraftingInput.of(3, 3, items);
    }

    private static boolean matches(final RecipeManager manager, final Level level,
                                   final CraftingInput grid) {
        final Optional<RecipeHolder<CraftingRecipe>> held =
                manager.getRecipeFor(RecipeType.CRAFTING, grid, level);
        return held.isPresent() && ROW_ID.equals(held.get().id());
    }

    private static Optional<RecipeHolder<CraftingRecipe>> findKnowFrag(final RecipeManager manager) {
        for (final RecipeHolder<CraftingRecipe> row : manager.getAllRecipesFor(RecipeType.CRAFTING)) {
            if (ROW_ID.equals(row.id())) {
                return Optional.of(row);
            }
        }
        return Optional.empty();
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
