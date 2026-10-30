package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.client.ContainerLayout;
import com.jonnyutah12.thaumcraftclassic.menus.ArcaneWorkbenchMenu;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
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

import java.util.ArrayList;
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
 *   <li>{@link #componentCarryingRecipePayloadsLoad}: TC-M4-03-s-b acceptance — the
 *       31 instances whose payloads carry a component (the 16 banner results naming
 *       {@code thaumcraft:banner_color} and the 15 with a components-carrying
 *       ingredient) resolve through the live {@link RecipeManager}'s own parse path:
 *       the ingredient form is NeoForge's documented custom-ingredient form
 *       ({"type": "neoforge:components", "items": …, "components": …}, parsed by
 *       {@code Ingredient.MAP_CODEC_NONEMPTY} = {@code CraftingHelper.makeIngredientMapCodec()}),
 *       and {@code thaumcraft:banner_color} is a registered data component (D-015).</li>
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

    /**
     * The instance ids whose generated payloads carry a component: 16 banner results
     * (loop expansion of the original banner family, one per colour) plus 14 shaped and
     * 1 shapeless instance whose ingredient needs a component-carrying ingredient. All
     * 31 have their {@code neoforge:conditions} ids registered, so the only legitimate
     * reason one is absent from the {@link RecipeManager} is a payload the parser rejects.
     */
    private static final List<String> componentCarryingInstanceIds() {
        final List<String> ids = new ArrayList<>();
        for (int colour = 0; colour < 16; colour++) {
            ids.add("recipe.112.arcane_shaped." + colour);
        }
        for (final int row : List.of(113, 120, 121, 122, 124, 125, 127, 128, 130,
                135, 136, 161, 184, 185)) {
            ids.add("recipe." + row + ".arcane_shaped");
        }
        ids.add("recipe.196.arcane_shapeless");
        return ids;
    }

    @GameTest(template = "empty")
    public static void componentCarryingRecipePayloadsLoad(final GameTestHelper helper) {
        final RecipeManager recipes = helper.getLevel().getRecipeManager();
        final List<String> missing = new ArrayList<>();
        for (final String path : componentCarryingInstanceIds()) {
            if (!recipes.byKey(id(path)).isPresent()) {
                missing.add(path);
            }
        }
        helper.assertTrue(missing.isEmpty(),
                "components-carrying recipe payloads rejected by the RecipeManager parse: "
                        + String.join(", ", missing));
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

    /**
     * The menu's slot grid is the original's, in the original's order and at the
     * original's coordinates: result, wand, 3x3 on a 24 px pitch, then the player
     * grid at (16,151) and the hotbar at (16,209) — and every anchor is the same
     * constant {@code client/ContainerLayout} hands the screen, which is what keeps
     * a drawn slot square over the slot the menu actually owns.
     */
    @GameTest(template = "empty")
    public static void menuSlotsAreTheOriginalsOrderAndAnchors(final GameTestHelper helper) {
        final TileArcaneWorkbench bench = benchAt(helper);
        final Player player = playerWithResearch(helper, List.of("WARDEDARCANA"));
        final ArcaneWorkbenchMenu menu =
                new ArcaneWorkbenchMenu(1, player.getInventory(), bench);

        helper.assertTrue(menu.slots.size() == ArcaneWorkbenchMenu.SLOT_COUNT,
                "the original's menu has 2 machine slots + the 3x3 grid + 36 player slots = "
                        + ArcaneWorkbenchMenu.SLOT_COUNT + ", got " + menu.slots.size());
        assertAnchor(helper, menu, ArcaneWorkbenchMenu.SLOT_RESULT,
                ContainerLayout.WB_RESULT_X, ContainerLayout.WB_RESULT_Y, "result");
        assertAnchor(helper, menu, ArcaneWorkbenchMenu.SLOT_WAND,
                ContainerLayout.WB_WAND_X, ContainerLayout.WB_WAND_Y, "wand");
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                // The original adds slots as `var7 + var6*3` (column + row*3).
                assertAnchor(helper, menu, ArcaneWorkbenchMenu.GRID_FIRST + col + row * 3,
                        ContainerLayout.workbenchGridX(col), ContainerLayout.workbenchGridY(row),
                        "grid " + row + "," + col);
            }
        }
        assertAnchor(helper, menu, ArcaneWorkbenchMenu.PLAYER_FIRST,
                ContainerLayout.workbenchPlayerX(0), ContainerLayout.WB_PLAYER_GRID_TOP, "player grid");
        assertAnchor(helper, menu, ArcaneWorkbenchMenu.PLAYER_FIRST + 26,
                ContainerLayout.workbenchPlayerX(8), ContainerLayout.workbenchPlayerY(2), "player grid end");
        assertAnchor(helper, menu, ArcaneWorkbenchMenu.HOTBAR_FIRST,
                ContainerLayout.workbenchPlayerX(0), ContainerLayout.WB_HOTBAR_TOP, "hotbar");
        assertAnchor(helper, menu, ArcaneWorkbenchMenu.SLOT_COUNT - 1,
                ContainerLayout.workbenchPlayerX(8), ContainerLayout.WB_HOTBAR_TOP, "hotbar end");

        helper.assertTrue(menu.slots.get(ArcaneWorkbenchMenu.SLOT_RESULT).container == bench,
                "the result slot must be the tile's slot 9");
        helper.assertTrue(menu.slots.get(ArcaneWorkbenchMenu.SLOT_WAND).container == bench,
                "the wand slot must be the tile's slot 10");
        helper.assertTrue(menu.slots.get(ArcaneWorkbenchMenu.GRID_FIRST).container == bench,
                "the 3x3 slots must be the tile's slots 0..8");
        helper.assertTrue(menu.slots.get(ArcaneWorkbenchMenu.PLAYER_FIRST).container
                        == player.getInventory(),
                "the player slots must be the player's inventory, as in the original");
        helper.succeed();
    }

    private static void assertAnchor(final GameTestHelper helper, final ArcaneWorkbenchMenu menu,
                                     final int index, final int x, final int y, final String what) {
        final Slot slot = menu.slots.get(index);
        helper.assertTrue(slot.x == x && slot.y == y,
                what + " slot anchor: the menu says (" + slot.x + "," + slot.y + "), the original's "
                        + "ContainerArcaneWorkbench and client/ContainerLayout both say (" + x + "," + y + ")");
    }

    /**
     * The container tick is the original's: opening the menu runs
     * {@code updateContainer} — the match, the wand payment, the result — and the
     * vis display the screen draws is the menu's data slots, not a client-side
     * guess. Prices and charges travel in centi-vis (the original's {@code * 100}).
     */
    @GameTest(template = "empty")
    public static void menuCycleFillsTheResultAndSyncsTheVisDisplay(final GameTestHelper helper) {
        final TileArcaneWorkbench bench = benchAt(helper);
        final Player player = playerWithResearch(helper, List.of("WARDEDARCANA"));
        setGrid(bench, grid(new ItemStack(Items.GOLD_NUGGET), new ItemStack(Items.GOLD_NUGGET),
                new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.GOLD_NUGGET)));
        final ItemStack wand = wandWith("aqua", 600, "ordo", 600);
        bench.setItem(10, wand);

        final ArcaneWorkbenchMenu menu =
                new ArcaneWorkbenchMenu(1, player.getInventory(), bench);
        // The server calls broadcastChanges() every tick for the open menu; that is
        // where ArcaneWorkbenchMenu put the original's updateContainer step.
        menu.broadcastChanges();

        assertResult(helper, menu.slots.get(ArcaneWorkbenchMenu.SLOT_RESULT).getItem());
        helper.assertTrue(menu.hasMatch(), "recipe 118 matched, so the menu must report a match");
        final int aqua = primalIndex("aqua");
        final int ordo = primalIndex("ordo");
        helper.assertTrue(menu.getCost(aqua) == 500 && menu.getCost(ordo) == 500,
                "the synced price must be the recipe's 5+5 real vis in centi-vis (the original's * 100), got "
                        + menu.getCost(aqua) + "/" + menu.getCost(ordo));
        helper.assertTrue(menu.getWandCharge(aqua) == 100 && menu.getWandCharge(ordo) == 100,
                "the synced charge must be what the wand has left after paying, got "
                        + menu.getWandCharge(aqua) + "/" + menu.getWandCharge(ordo));
        helper.assertTrue(charge(wand, "aqua") == 100 && charge(wand, "ordo") == 100,
                "the payment the display reports must be the payment the tile took");
        // The original's flag is its GUI's draw-time check (GuiArcaneWorkbench.java:93):
        // "can this wand pay the price NOW", measured AFTER the reveal already spent 500 of
        // 600 — so the original pulsed its tags at low alpha and drew "Insufficient vis"
        // right after a successful craft on a nearly drained wand. The port's flag is the
        // same check on the same beat, so the honest expectation here is TRUE.
        helper.assertTrue(menu.isInsufficientVis(),
                "the pool left after paying (100) is under the price (500) — the original's "
                        + "draw-time check was true in exactly this state");
        helper.assertTrue(primalIndex("aer") == 0 && primalIndex("perditio") == 5,
                "the display's slot order is the original's Aspect.getPrimalAspects() order");

        // The cycle must not run away: the original filled slot 9 only while it was
        // empty, so a second tick with a full result slot spends nothing more.
        menu.broadcastChanges();
        helper.assertTrue(charge(wand, "aqua") == 100,
                "an open menu must not drain the wand once the result is there");
        helper.succeed();
    }

    /**
     * The "Insufficient vis" state, measured where the original measured it — a
     * simulate-only payment attempt — and synced as a flag so the screen never has
     * to read a wand. The all-or-nothing rule keeps the short pool untouched.
     */
    @GameTest(template = "empty")
    public static void menuShortPoolReportsInsufficientVis(final GameTestHelper helper) {
        final TileArcaneWorkbench bench = benchAt(helper);
        final Player player = playerWithResearch(helper, List.of("WARDEDARCANA"));
        setGrid(bench, grid(new ItemStack(Items.GOLD_NUGGET), new ItemStack(Items.GOLD_NUGGET),
                new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.GOLD_NUGGET)));
        final ItemStack wand = wandWith("aqua", 400, "ordo", 400);
        bench.setItem(10, wand);

        final ArcaneWorkbenchMenu menu =
                new ArcaneWorkbenchMenu(1, player.getInventory(), bench);
        menu.broadcastChanges();

        helper.assertTrue(menu.slots.get(ArcaneWorkbenchMenu.SLOT_RESULT).getItem().isEmpty(),
                "a pool one-fifth short must not produce a result");
        helper.assertTrue(menu.hasMatch(), "the recipe still matched; only the price is unpayable");
        helper.assertTrue(menu.isInsufficientVis(),
                "the original's flag: consumeAllVisCrafting(..., false) returns false here");
        helper.assertTrue(menu.getCost(primalIndex("aqua")) == 500
                        && menu.getWandCharge(primalIndex("aqua")) == 400,
                "the screen must be able to show the price next to the charge it is short of");
        helper.assertTrue(charge(wand, "aqua") == 400 && charge(wand, "ordo") == 400,
                "the refused payment must spend nothing");

        // The original's notice gate is `wand != null && cost != null && !canPay`
        // (GuiArcaneWorkbench.java:93): with the wand pulled the GUI drew NO notice,
        // so the synced flag must clear while the price stays live for the tags.
        bench.removeItem(TileArcaneWorkbench.SLOT_WAND, 1);
        menu.broadcastChanges();
        helper.assertTrue(!menu.isInsufficientVis(),
                "no wand in slot 10 means the original showed no \"Insufficient vis\" at all");
        helper.assertTrue(menu.hasMatch() && menu.getCost(primalIndex("aqua")) == 500,
                "the price tags stay live without a wand — the shortage flag is the wand's business");
        bench.setItem(TileArcaneWorkbench.SLOT_WAND, wand);
        menu.broadcastChanges();
        helper.assertTrue(menu.isInsufficientVis(), "putting the short wand back re-arms the flag");

        // No match at all: the flag is clear and the price slots are zero, so the
        // screen draws no tags (the original's `if (amount > 0)` rule).
        bench.clearContent();
        menu.broadcastChanges();
        helper.assertTrue(!menu.hasMatch() && !menu.isInsufficientVis(),
                "an empty grid is not a shortage");
        helper.assertTrue(menu.getCost(primalIndex("aqua")) == 0,
                "no match must sync a zero price, not a stale one");
        helper.succeed();
    }

    /**
     * Lifting the result is {@code SlotCraftingArcaneWorkbench.onCraft}: one item
     * out of every grid cell, and the original's <i>second</i> vis charge. The 1.7.10
     * workbench charged the wand twice — once to reveal the result, once to take it
     * — and the port reproduces that rather than tidying it.
     */
    @GameTest(template = "empty")
    public static void takingTheResultSpendsTheGridAndPaysAgain(final GameTestHelper helper) {
        final TileArcaneWorkbench bench = benchAt(helper);
        final Player player = playerWithResearch(helper, List.of("WARDEDARCANA"));
        final ItemStack twoNuggets = new ItemStack(Items.GOLD_NUGGET, 2);
        final ItemStack twoIngots = new ItemStack(Items.GOLD_INGOT, 2);
        setGrid(bench, grid(twoNuggets, twoNuggets, twoIngots, twoNuggets));
        final ItemStack wand = wandWith("aqua", 1500, "ordo", 1500);
        bench.setItem(10, wand);

        final ArcaneWorkbenchMenu menu =
                new ArcaneWorkbenchMenu(1, player.getInventory(), bench);
        menu.broadcastChanges();
        helper.assertTrue(charge(wand, "aqua") == 1000, "the reveal must have paid 500");

        final Slot result = menu.slots.get(ArcaneWorkbenchMenu.SLOT_RESULT);
        assertResult(helper, result.getItem());
        result.onTake(player, result.getItem().copy());

        helper.assertTrue(bench.getItem(0).getCount() == 1 && bench.getItem(2).getCount() == 1,
                "the original's onCraft loop took exactly one of every grid cell");
        helper.assertTrue(charge(wand, "aqua") == 500 && charge(wand, "ordo") == 500,
                "the take must charge the vis a second time, as the original did");
        helper.succeed();
    }

    /**
     * The two machine slots' rules and the original's shift-click routing, branch
     * for branch. The wand slot's rule is the port's seam for
     * {@code SlotLimitedByWand} (a vis-carrying stack, never a staff), and the 3x3
     * grid stays unreachable by shift-click from the player side — the original's
     * rule, kept.
     */
    @GameTest(template = "empty")
    public static void menuGuardsItsSlotsAndRoutesShiftClicks(final GameTestHelper helper) {
        final TileArcaneWorkbench bench = benchAt(helper);
        final Player player = playerWithResearch(helper, List.of("WARDEDARCANA"));
        // The harness's mock player hands out its inventory by value, so every read and
        // write in this probe goes through ONE captured reference — the same one the menu
        // was built on. Repeated player.getInventory() calls make the probe depend on
        // which copy a line happened to touch.
        final Inventory inv = player.getInventory();
        final ArcaneWorkbenchMenu menu = new ArcaneWorkbenchMenu(1, inv, bench);

        final Slot wandSlot = menu.slots.get(ArcaneWorkbenchMenu.SLOT_WAND);
        helper.assertTrue(!wandSlot.mayPlace(new ItemStack(Items.STONE)),
                "the original's SlotLimitedByWand rejected anything that is not a wand");
        helper.assertTrue(wandSlot.mayPlace(wandWith("aqua", 100, "ordo", 100)),
                "a vis-carrying stack (the port's seam for ItemWandCasting) is accepted");
        helper.assertTrue(wandSlot.getMaxStackSize() == 64,
                "the original's slot declared a 64 stack limit");
        helper.assertTrue(!menu.slots.get(ArcaneWorkbenchMenu.SLOT_RESULT).mayPlace(new ItemStack(Items.GOLD_INGOT)),
                "the result slot is output-only, as SlotCraftingArcaneWorkbench was");

        // Shift-click is the original's ContainerArcaneWorkbench.transferStackInSlot, kept
        // branch for branch at 1.21.1's menu-level seam (Menu#quickMoveStack): a
        // vis-carrying stack in the player's grid goes to the WAND slot — the original's
        // `stack.func_75214_d() instanceof ItemWandCasting` branch — the hotbar shifts to
        // the player grid, the result and the grid shift to the player, and the 3x3 stays
        // unreachable from the player side. The harness player ships with a kit, so the
        // captured inventory is emptied first: this probe is about the routing, not about
        // where the kit happened to sit.
        for (int kitSlot = 0; kitSlot < inv.getContainerSize(); kitSlot++) {
            inv.setItem(kitSlot, ItemStack.EMPTY);
        }

        final ItemStack wand = wandWith("aqua", 100, "ordo", 100);
        inv.setItem(9, wand);
        menu.quickMoveStack(player, ArcaneWorkbenchMenu.PLAYER_FIRST);
        // The shift MOVES the stack, and `wand` is the source slot's object — the move
        // empties it, so the assertion has to read the destination. Measured: the tile's
        // slot 10 and the menu's slot 1 hold the stack, inv slot 9 is air.
        helper.assertTrue(charge(wandSlot.getItem(), "aqua") == 100
                        && charge(wandSlot.getItem(), "ordo") == 100,
                "the player grid's wand shifts into the wand slot with its vis pool intact — "
                        + "the original's `stack.func_75214_d() instanceof ItemWandCasting` branch");
        helper.assertTrue(inv.getItem(9).isEmpty(), "and the grid slot is left empty");

        // A plain item in the hotbar shifts to the player grid.
        inv.setItem(0, new ItemStack(Items.COBBLESTONE));
        menu.quickMoveStack(player, ArcaneWorkbenchMenu.HOTBAR_FIRST);
        helper.assertTrue(inv.getItem(9).is(Items.COBBLESTONE),
                "the hotbar's shift-click must land in the player grid, as the original routed it");

        // The result shifts to the player inventory, scanning back from the hotbar.
        inv.setItem(9, ItemStack.EMPTY);
        inv.setItem(0, ItemStack.EMPTY);
        inv.setItem(18, ItemStack.EMPTY);
        bench.setItem(TileArcaneWorkbench.SLOT_RESULT, new ItemStack(Items.GOLD_INGOT));
        menu.quickMoveStack(player, ArcaneWorkbenchMenu.SLOT_RESULT);
        helper.assertTrue(bench.getItem(TileArcaneWorkbench.SLOT_RESULT).isEmpty(),
                "the result must have left the machine slot");
        boolean goldInPlayer = false;
        for (int playerSlot = 0; playerSlot < inv.getContainerSize(); playerSlot++) {
            if (inv.getItem(playerSlot).is(Items.GOLD_INGOT)) {
                goldInPlayer = true;
            }
        }
        helper.assertTrue(goldInPlayer,
                "and it landed in the player's rows — the original's result branch scans the player "
                        + "inventory backwards from the hotbar, so the exact slot is vanilla's to pick");

        // A grid item shifts to the player: the original's final else-branch.
        bench.setItem(0, new ItemStack(Items.GOLD_NUGGET));
        menu.quickMoveStack(player, ArcaneWorkbenchMenu.GRID_FIRST);
        helper.assertTrue(bench.getItem(0).isEmpty(),
                "the machine-to-player branch is the original's last else");

        helper.succeed();
    }

    /**
     * The block's use interaction is the original's {@code onBlockActivated}: it
     * hands the tile over as a {@link net.minecraft.world.MenuProvider} whose title
     * is the lang key the menu type ships. The open-screen packet itself needs a
     * real {@code ServerPlayer} — the game-test harness's mock player is a plain
     * {@code Player}, so the wire half of this seam is not exercised here; that is
     * the same honest limit the screens carry, stated in docs.
     */
    @GameTest(template = "empty")
    public static void theBenchIsTheMenuProviderTheBlockOpens(final GameTestHelper helper) {
        final TileArcaneWorkbench bench = benchAt(helper);
        final Player player = playerWithResearch(helper, List.of("WARDEDARCANA"));

        helper.assertTrue(bench instanceof net.minecraft.world.MenuProvider,
                "TC-M7-04-m2: the tile must be the MenuProvider BlockArcaneWorkbench opens");
        final var title = bench.getDisplayName().getContents();
        helper.assertTrue(title instanceof net.minecraft.network.chat.contents.TranslatableContents contents
                        && "container.thaumcraft.gui_arcane_workbench".equals(contents.getKey()),
                "the menu title must be the translatable key the gui row ships, got " + title);
        helper.assertTrue(bench.createMenu(7, player.getInventory(), player)
                        instanceof ArcaneWorkbenchMenu,
                "the provider must build the workbench menu the registered MenuType names");
        helper.assertTrue(CatalogArcaneWorkbench.MENU_ARCANE_WORKBENCH.getId().getPath()
                        .equals("gui_arcane_workbench"),
                "the menu type is registered under the content-mapping.csv gui row id");
        helper.succeed();
    }

    /** The index of a primal in {@link Aspect#getPrimalAspects()} — the menu's cost-slot order. */
    private static int primalIndex(final String tag) {
        final List<Aspect> primals = Aspect.getPrimalAspects();
        for (int i = 0; i < primals.size(); i++) {
            // The registry stores the primal names upper-case ({@code "AQUA"}); the
            // probes name them the way the JSON aspect tags do, lower-case.
            if (primals.get(i).getName().equalsIgnoreCase(tag)) {
                return i;
            }
        }
        throw new IllegalArgumentException(tag + " is not a primal aspect");
    }

    private static void assertResult(final GameTestHelper helper, final ItemStack result) {
        helper.assertTrue(!result.isEmpty()
                        && result.is(CatalogFurniture.ARCANE_DOOR_KEY.get())
                        && result.getCount() == 2
                        && result.getOrDefault(ModDataComponents.ITEM_META.get(), -1) == 1,
                "the arcana key result (2x, meta 1) was not produced");
    }
}
