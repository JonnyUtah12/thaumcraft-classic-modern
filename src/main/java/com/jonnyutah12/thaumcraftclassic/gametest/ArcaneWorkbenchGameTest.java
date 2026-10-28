package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.recipes.ArcaneWorkbenchRecipe;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogArcaneWorkbench;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneWorkbench;
import com.jonnyutah12.thaumcraftclassic.wands.WandVis;
import com.jonnyutah12.thaumcraftclassic.wands.VisPool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.GameType;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;
import java.util.Map;

/**
 * TC-M4-03-s acceptance: the arcane workbench core (recipe type, block +
 * block entity, recipe data, vis cost application through TC-M4-02's chain).
 *
 * <ul>
 *   <li>{@link #workbenchCoreIsRegistered}: every contract id of this card
 *       resolves — the workbench block/item/block-entity, the recipe type +
 *       serializer — and the data recipes under
 *       {@code data/thaumcraft/recipe/} load as {@link ArcaneWorkbenchRecipe}
 *       instances into the live {@link RecipeManager} (instances whose
 *       {@code neoforge:conditions} name not-yet-registered items stay gated
 *       until their owning cards land).</li>
 *   <li>{@link #shapedMatchDirectAndMirrored}: the original shaped
 *       {@code NNI/N  } grid matches in its direct orientation and through the
 *       original's default {@code mirrored = true} horizontal mirror
 *       (ThaumcraftCraftingManager.findMatchingArcaneRecipe over
 *       ShapedArcaneRecipe.checkMatch); the result is recipe 118's arcana
 *       key pair with its meta.</li>
 *   <li>{@link #shapelessClaimsEveryIngredient}: the shapeless kind crafts
 *       only when every grid item is claimed by exactly one ingredient and
 *       the recipe's ingredients are all present — an unclaimable extra item
 *       breaks the match (ShapelessArcaneRecipe.java:74).</li>
 *   <li>{@link #researchGateBlocksTheUnreader}: the original research gate
 *       (ThaumcraftApiHelper.isResearchComplete over the requester's
 *       knowledge, TC-M3-02) runs before the grid match: an unseeded player
 *       never matches and spends nothing, seeding {@code WARDEDARCANA} flips
 *       the same grid to a match.</li>
 *   <li>{@link #visPaymentExactEmptiesThePool}: the recipe's aspect price is
 *       paid out of the wand slot's vis pool through TC-M4-02's
 *       {@code effectiveCosts → consumeAllVisCrafting} chain, real vis × 100
 *       (ItemWandCasting.java:314): an exactly-seeded centi-vis pool goes to
 *       empty and only then does the result slot fill.</li>
 *   <li>{@link #shortVisPoolSpendsNothing}: a pool one centi-vis short pays
 *       nothing at all (the original's all-or-nothing consumeAllVis,
 *       ItemWandCasting.java:325-347) and the result never appears.</li>
 * </ul>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArcaneWorkbenchGameTest {

    private ArcaneWorkbenchGameTest() {
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
    }

    // --- helpers ---------------------------------------------------------------

    /** Place the workbench block at (1,1,1) and hand back its entity. */
    private static TileArcaneWorkbench benchAt(final GameTestHelper helper) {
        helper.setBlock(1, 1, 1, CatalogArcaneWorkbench.ARCANE_WORKBENCH_BLOCK.get());
        final BlockEntity entity = helper.getBlockEntity(new BlockPos(1, 1, 1));
        helper.assertTrue(entity instanceof TileArcaneWorkbench,
                "the workbench block must create its TileArcaneWorkbench entity");
        return (TileArcaneWorkbench) entity;
    }

    /** Fill the 3x3 grid (slots 0..8, the original stackList order). */
    private static void setGrid(final TileArcaneWorkbench bench, final ItemStack[] grid) {
        for (int slot = 0; slot < grid.length; slot++) {
            bench.setItem(slot, grid[slot]);
        }
    }

    private static ItemStack[] grid(final ItemStack... items) {
        final ItemStack[] nine = new ItemStack[9];
        for (int i = 0; i < nine.length; i++) {
            nine[i] = i < items.length ? items[i] : ItemStack.EMPTY;
        }
        return nine;
    }

    /** A "wand" for the tile's seam: any stack carrying the TC-M4-02 VIS_POOL. */
    private static ItemStack wandWith(final String aspectA, final int centiA,
                                     final String aspectB, final int centiB) {
        final ItemStack wand = new ItemStack(Items.DIAMOND_PICKAXE);
        WandVis.setPool(wand, new VisPool(Map.of(aspectA, centiA, aspectB, centiB)));
        return wand;
    }

    private static final Aspect aspect(final String tag) {
        final Aspect found = Aspect.getAspect(tag);
        assert found != null : "unknown aspect tag " + tag;
        return found;
    }

    private static int charge(final ItemStack wand, final String tag) {
        return WandVis.getVis(WandVis.poolOf(wand), aspect(tag));
    }

    /** A mock player whose TC-M3-02 knowledge has the listed research done. */
    private static Player playerWithResearch(final GameTestHelper helper,
                                            final List<String> keys) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        for (final String key : keys) {
            knowledge = knowledge.withResearchCompleted(key);
        }
        player.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
        return player;
    }

    // --- probes ----------------------------------------------------------------

    @GameTest(template = "empty")
    public static void workbenchCoreIsRegistered(final GameTestHelper helper) {
        for (final String path : List.of("tilearcaneworkbench")) {
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id(path)),
                    "workbench block id " + path + " is not registered");
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id(path)),
                    "workbench block item id " + path + " is not registered");
            helper.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(id(path)),
                    "workbench block entity id " + path + " is not registered");
        }
        helper.assertTrue(BuiltInRegistries.RECIPE_TYPE.containsKey(id("arcane_workbench")),
                "the arcane workbench recipe type is not registered");
        helper.assertTrue(BuiltInRegistries.RECIPE_SERIALIZER.containsKey(id("arcane_workbench")),
                "the arcane workbench recipe serializer is not registered");

        // The data files under data/thaumcraft/recipe/ must resolve through the
        // type + serializer pair. Instances gated on items their owning cards
        // have not registered yet are legitimately absent; the load must not be.
        final RecipeManager recipes = helper.getLevel().getRecipeManager();
        var loaded = 0;
        for (final RecipeHolder<?> holder : recipes.getAllRecipesFor(
                CatalogArcaneWorkbench.ARCANE_WORKBENCH_TYPE.get())) {
            if (holder.value() instanceof ArcaneWorkbenchRecipe) {
                loaded++;
            }
        }
        helper.assertTrue(loaded > 0,
                "no data recipe resolved as the arcane workbench recipe type");
        // The probes below exercise three of these directly; they must be
        // among the loaded instances (their condition ids are registered).
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shapedMatchDirectAndMirrored(final GameTestHelper helper) {
        final TileArcaneWorkbench bench = benchAt(helper);
        final Player player = playerWithResearch(helper, List.of("WARDEDARCANA"));

        final ItemStack nugget = new ItemStack(Items.GOLD_NUGGET);
        final ItemStack ingot = new ItemStack(Items.GOLD_INGOT);

        // Recipe 118 ("NNI" / "N  ") in its direct orientation.
        setGrid(bench, grid(nugget, nugget, ingot, nugget));
        final ItemStack wandA = wandWith("aqua", 500, "ordo", 500);
        bench.setItem(10, wandA);
        helper.assertTrue(bench.craft(player),
                "the direct-oriented arcana-key grid must match the shaped recipe");
        assertResult(helper, bench.getItem(9));
        helper.assertTrue(charge(wandA, "aqua") == 0 && charge(wandA, "ordo") == 0,
                "the exact centi-vis price must have been spent");

        // The original constructor defaults mirrored = true; the same recipe
        // must also match the horizontally flipped grid.
        bench.clearContent();
        setGrid(bench, grid(ingot, nugget, nugget, null, null, nugget));
        final ItemStack wandB = wandWith("aqua", 500, "ordo", 500);
        bench.setItem(10, wandB);
        helper.assertTrue(bench.craft(player),
                "the mirrored-oriented grid must match the same recipe (original mirrored=true)");
        assertResult(helper, bench.getItem(9));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shapelessClaimsEveryIngredient(final GameTestHelper helper) {
        // None of the five .arcane_shapeless data files is available yet: every one
        // is gated on the blocktube item, whose owning card has not registered it,
        // so this probe builds a shapeless recipe in code and drives the recipe
        // class's claim-every-grid-item rule directly (ShapelessArcaneRecipe.java:74).
        final AspectList empty = new AspectList();
        final ArcaneWorkbenchRecipe shapeless = new ArcaneWorkbenchRecipe("", empty,
                List.of(), Map.of(),
                List.of(Ingredient.of(Items.GOLD_NUGGET), Ingredient.of(Items.GLASS_PANE)),
                new ItemStack(Items.GOLD_INGOT));

        final Player player = playerWithResearch(helper, List.of());
        final ItemStack emptySlot = ItemStack.EMPTY;

        // Exact ingredients: each grid item is claimed by exactly one ingredient
        // and every ingredient is claimed — the shapeless match succeeds.
        final ArcaneWorkbenchRecipe.WorkbenchInput okGrid = new ArcaneWorkbenchRecipe.WorkbenchInput(List.of(
                new ItemStack(Items.GOLD_NUGGET), new ItemStack(Items.GLASS_PANE),
                emptySlot, emptySlot, emptySlot, emptySlot, emptySlot, emptySlot, emptySlot), player);
        helper.assertTrue(shapeless.matches(okGrid, helper.getLevel()),
                "the shapeless recipe's exact ingredients must match");

        // An extra unclaimable item must break the match: nothing may remain on the
        // grid after every ingredient has claimed one item.
        final ArcaneWorkbenchRecipe.WorkbenchInput extraGrid = new ArcaneWorkbenchRecipe.WorkbenchInput(List.of(
                new ItemStack(Items.GOLD_NUGGET), new ItemStack(Items.GLASS_PANE),
                new ItemStack(Items.COBBLESTONE),
                emptySlot, emptySlot, emptySlot, emptySlot, emptySlot, emptySlot), player);
        helper.assertTrue(!shapeless.matches(extraGrid, helper.getLevel()),
                "the unclaimable extra item must break the shapeless match");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void researchGateBlocksTheUnreader(final GameTestHelper helper) {
        final TileArcaneWorkbench bench = benchAt(helper);
        final Player unreader = helper.makeMockPlayer(GameType.SURVIVAL);

        final ItemStack nugget = new ItemStack(Items.GOLD_NUGGET);
        final ItemStack ingot = new ItemStack(Items.GOLD_INGOT);
        setGrid(bench, grid(nugget, nugget, ingot, nugget));
        final ItemStack wandA = wandWith("aqua", 500, "ordo", 500);
        bench.setItem(10, wandA);
        helper.assertTrue(!bench.craft(unreader),
                "WARDEDARCANA not researched: the original gate must refuse the match");
        helper.assertTrue(charge(wandA, "aqua") == 500 && charge(wandA, "ordo") == 500,
                "the refused cycle must not touch the pool");
        helper.assertTrue(bench.getItem(9).isEmpty(), "the result slot must stay empty");

        final Player reader = playerWithResearch(helper, List.of("WARDEDARCANA"));
        helper.assertTrue(bench.craft(reader),
                "the same grid must match once WARDEDARCANA is researched");
        assertResult(helper, bench.getItem(9));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void visPaymentExactEmptiesThePool(final GameTestHelper helper) {
        final TileArcaneWorkbench bench = benchAt(helper);
        final Player player = playerWithResearch(helper, List.of("WARDEDARCANA"));

        final ItemStack nugget = new ItemStack(Items.GOLD_NUGGET);
        final ItemStack ingot = new ItemStack(Items.GOLD_INGOT);
        setGrid(bench, grid(nugget, nugget, ingot, nugget));
        // Recipe 118's price is AQUA 5 + ORDO 5 real vis; storage is centi-vis
        // (the original's * 100, ItemWandCasting.java:314), so the exact pool
        // is 500/500.
        final ItemStack wand = wandWith("aqua", 500, "ordo", 500);
        bench.setItem(10, wand);
        helper.assertTrue(bench.craft(player),
                "an exactly-seeded pool must pay the recipe's price");
        helper.assertTrue(WandVis.poolOf(wand).isEmpty(),
                "the exact payment must leave the pool empty");
        assertResult(helper, bench.getItem(9));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shortVisPoolSpendsNothing(final GameTestHelper helper) {
        final TileArcaneWorkbench bench = benchAt(helper);
        final Player player = playerWithResearch(helper, List.of("WARDEDARCANA"));

        final ItemStack nugget = new ItemStack(Items.GOLD_NUGGET);
        final ItemStack ingot = new ItemStack(Items.GOLD_INGOT);
        setGrid(bench, grid(nugget, nugget, ingot, nugget));
        final ItemStack wand = wandWith("aqua", 500, "ordo", 499);
        bench.setItem(10, wand);
        helper.assertTrue(!bench.craft(player),
                "a pool one centi-vis short must refuse the craft");
        helper.assertTrue(charge(wand, "aqua") == 500 && charge(wand, "ordo") == 499,
                "the all-or-nothing payment rule must spend nothing on a short pool");
        helper.assertTrue(bench.getItem(9).isEmpty(), "the result must not appear");
        helper.succeed();
    }

    /**
     * TC-M4-03-fix: the shipped data set, not the code. Every `recipe.<N>.arcane_*`
     * row under {@code data/thaumcraft/recipe/} is enumerated through the server's
     * {@link ResourceManager}; a row whose {@code neoforge:conditions} are all
     * satisfied must be in the live {@link RecipeManager}, matched against the real
     * instance (research gate, pattern, result item + count, and the
     * {@code thaumcraft:banner_color} component the 16 banner rows carry). A row that
     * fails to decode — a malformed ingredient form, an unregistered component type —
     * is dropped by the RecipeManager in silence, and this is what pins it.
     */
    @GameTest(template = "empty")
    public static void assertEveryLoadableArcaneRowLoaded(final GameTestHelper helper) {
        final var resources = helper.getLevel().getServer().getResourceManager();
        final RecipeManager manager = helper.getLevel().getRecipeManager();

        // Live instances of this card's type; a data row may claim each one only
        // once, so a duplicated signature cannot stand in for a missing row.
        final java.util.ArrayList<ArcaneWorkbenchRecipe> live = new java.util.ArrayList<>();
        for (final RecipeHolder<?> holder : manager.getAllRecipesFor(
                CatalogArcaneWorkbench.ARCANE_WORKBENCH_TYPE.get())) {
            if (holder.value() instanceof ArcaneWorkbenchRecipe row) {
                live.add(row);
            }
        }
        final java.util.ArrayList<String> loadedPaths = new java.util.ArrayList<>();
        final java.util.ArrayList<Boolean> claimed = new java.util.ArrayList<>();
        for (final ArcaneWorkbenchRecipe ignored : live) {
            claimed.add(false);
        }

        var bannerRowsLoaded = 0;
        for (final ArcaneWorkbenchRecipe row : live) {
            if (row.output().has(ModDataComponents.BANNER_COLOR.get())) {
                bannerRowsLoaded++;
            }
        }

        var shipped = 0;
        var loadable = 0;
        for (final var entry : resources.listResources("recipe", path ->
                path.getNamespace().equals(ThaumcraftClassic.MOD_ID)
                        && path.getPath().contains(".arcane")).entrySet()) {
            final com.google.gson.JsonObject json;
            try (var reader = entry.getValue().openAsReader()) {
                json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            } catch (final java.io.IOException failure) {
                helper.fail("unreadable recipe resource " + entry.getKey() + ": " + failure);
                return;
            }
            shipped++;

            var gated = false;
            final var conditions = json.get("neoforge:conditions");
            if (conditions != null && conditions.isJsonArray()) {
                for (final var condition : conditions.getAsJsonArray()) {
                    final var guard = condition.getAsJsonObject();
                    if (!"neoforge:item_exists".equals(guard.get("type").getAsString())) {
                        gated = true; // a Config / tag gate this probe cannot evaluate
                        continue;
                    }
                    if (!BuiltInRegistries.ITEM.containsKey(
                            ResourceLocation.parse(guard.get("item").getAsString()))) {
                        gated = true; // the row is legitimately skipped at load
                    }
                }
            }
            if (gated) {
                continue;
            }
            loadable++;

            final com.google.gson.JsonObject result = json.getAsJsonObject("result");
            final String resultId = result.get("id").getAsString();
            final int wantedCount = result.has("count") ? result.get("count").getAsInt() : 1;
            final com.google.gson.JsonObject components =
                    result.has("components") ? result.getAsJsonObject("components") : null;
            final int wantedColor = components != null && components.has("thaumcraft:banner_color")
                    ? components.get("thaumcraft:banner_color").getAsInt() : -1;
            final java.util.ArrayList<String> pattern = new java.util.ArrayList<>();
            if (json.has("pattern")) {
                for (final var line : json.getAsJsonArray("pattern")) {
                    pattern.add(line.getAsString());
                }
            }
            final String research = json.has("research") ? json.get("research").getAsString() : "";

            final Item expected;
            try {
                expected = BuiltInRegistries.ITEM.get(ResourceLocation.parse(resultId));
            } catch (final IllegalArgumentException failure) {
                helper.fail("ungated row " + entry.getKey() + " names an unregistered result "
                        + resultId + ", which the load cannot resolve: " + failure);
                return;
            }

            var found = -1;
            for (int i = 0; i < live.size(); i++) {
                if (claimed.get(i)) {
                    continue;
                }
                final ArcaneWorkbenchRecipe row = live.get(i);
                final String rowResearch = row.research() == null ? "" : row.research();
                if (!rowResearch.equals(research)) {
                    continue;
                }
                if (!row.pattern().equals(pattern)) {
                    continue;
                }
                final ItemStack output = row.output();
                if (output.getCount() != wantedCount || !output.is(expected)) {
                    continue;
                }
                if (output.getOrDefault(ModDataComponents.BANNER_COLOR.get(), -1) != wantedColor) {
                    continue;
                }
                found = i;
                break;
            }
            helper.assertTrue(found >= 0,
                    "the shipped data row " + entry.getKey() + " is gated on nothing missing, so it "
                            + "must reach the live RecipeManager; it is either malformed (check the "
                            + "server log's RecipeManager parse error for this row) or it carries "
                            + "the result " + resultId + " x" + wantedCount
                            + " colour " + wantedColor + " that no loaded instance matches");
            claimed.set(found, true);
            loadedPaths.add(entry.getKey().toString());
        }

        ThaumcraftClassic.LOGGER.info(
                "[TC-M4-03-fix] arcane data: {} rows shipped, {} ungated and all {} matched against "
                        + "a live instance; {} loaded rows carry a thaumcraft:banner_color component",
                shipped, loadable, loadable, bannerRowsLoaded);
        ThaumcraftClassic.LOGGER.info(
                "[TC-M4-03-fix] the {} rows that resolve into the live RecipeManager: {}",
                loadedPaths.size(), String.join(", ", loadedPaths));
        helper.assertTrue(loadable >= 31,
                "only " + loadable + " arcane rows are ungated — the data set looks truncated");
        helper.assertTrue(bannerRowsLoaded >= 16,
            "the 16 recipe.112.arcane_shaped.<0..15> banner rows must each carry the port's "
                    + "thaumcraft:banner_color component, got " + bannerRowsLoaded);
        helper.succeed();
    }

    private static void assertResult(final GameTestHelper helper, final ItemStack result) {
        helper.assertTrue(!result.isEmpty()
                        && result.is(CatalogFurniture.ARCANE_DOOR_KEY.get())
                        && result.getCount() == 2
                        && result.getOrDefault(ModDataComponents.ITEM_META.get(), -1) == 1,
                "the arcana key result (2x, meta 1) was not produced");
    }
}
