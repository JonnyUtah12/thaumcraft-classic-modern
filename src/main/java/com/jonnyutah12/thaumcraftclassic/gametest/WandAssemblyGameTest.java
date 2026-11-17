package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.api.wands.WandCap;
import com.jonnyutah12.thaumcraftclassic.api.wands.WandRod;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.items.wands.ItemWandCasting;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.recipes.ArcaneWorkbenchRecipe;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogArcaneWorkbench;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogWands;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneWorkbench;
import com.jonnyutah12.thaumcraftclassic.wands.VisPool;
import com.jonnyutah12.thaumcraftclassic.wands.WandComponents;
import com.jonnyutah12.thaumcraftclassic.wands.WandVis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TC-M6-01-s-b acceptance probes: the wand and sceptre <em>assembly</em>
 * recipes on the live {@link RecipeManager} of the gameTestServer.
 *
 * <p>Original: {@code thaumcraft.common.lib.crafting.ArcaneWandRecipe} and
 * {@code ArcaneSceptreRecipe}, the two dynamic {@code IArcaneRecipe}s the
 * original registered on {@code ThaumcraftApi.getCraftingRecipes()}. The port
 * enumerates the same combination space as {@code thaumcraft:arcane_workbench}
 * data rows (107 wand — the wood rod + iron cap pair the original refuses is
 * absent — and 108 sceptre) and drives them through the TC-M4-03-s seam, so
 * these probes assert the shipped data, not the generator.</p>
 *
 * <ul>
 *   <li>{@link #everyAssemblyRowReachesTheRecipeManager}: the live
 *   {@link RecipeManager} carries exactly 107 wand + 108 sceptre assembly rows
 *   through the lookup {@code TileArcaneWorkbench.findMatch} walks — a row the
 *   parser rejects is dropped in silence, and so is a row whose
 *   {@code neoforge:item_exists} gate is unmet, which is what makes the count
 *   the parity statement (the pattern of
 *   {@code ArcaneWorkbenchGameTest.assertEveryLoadableArcaneRowLoaded}).</li>
 *   <li>{@link #everyLegalComboAssemblesWithTheOriginalCost}: for all 6 caps x
 *   18 rods (staff cores included), exactly one wand row and one sceptre row
 *   exist, with damage value {@code capCraftCost * rodCraftCost} /
 *   {@code (int)(capCraftCost * rodCraftCost * 1.5F)}, that same price on all
 *   six primal aspects, and the dual cap+rod research gate (plus
 *   {@code SCEPTRE}); the refused wood+iron wand combo has no row.</li>
 *   <li>{@link #wandAssemblesThroughTheWorkbench} /
 *   {@link #sceptreAssemblesThroughTheWorkbench}: the real
 *   {@link TileArcaneWorkbench#craft(Player)} cycle assembles both, pays the
 *   vis price, and the result's rod/cap/sceptre components give it the
 *   original capacity.</li>
 *   <li>{@link #assemblyRefusedUntilBothResearchKeysAreKnown}: the additive
 *   {@code research_all} gate refuses a reader of only one of the two keys,
 *   and refuses the sceptre without {@code SCEPTRE}.</li>
 * </ul>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WandAssemblyGameTest {

    private WandAssemblyGameTest() {
    }

    /**
     * The enumerated rows must reach the live RecipeManager through the same
     * seam {@code TileArcaneWorkbench.findMatch} walks, in the original's
     * combination space: 6 caps x 18 rods = 108 wand combos MINUS the wood rod
     * + iron cap combo the original refuses = 107, plus 108 sceptre combos.
     */
    @GameTest(template = "empty")
    public static void everyAssemblyRowReachesTheRecipeManager(final GameTestHelper helper) {
        final RecipeManager manager = helper.getLevel().getRecipeManager();
        int wands = 0;
        int sceptres = 0;
        int gated = 0;
        int priced = 0;
        final List<String> woodIron = new ArrayList<>();
        final List<String> badGate = new ArrayList<>();
        for (final RecipeHolder<?> holder : manager.getAllRecipesFor(
                CatalogArcaneWorkbench.ARCANE_WORKBENCH_TYPE.get())) {
            if (!(holder.value() instanceof ArcaneWorkbenchRecipe row)) {
                continue;
            }
            if (!(row.output().getItem() instanceof ItemWandCasting)) {
                continue; // the shipped cap/rod crafting rows build components, not wands
            }
            final String cap = row.output().getOrDefault(ModDataComponents.WAND_CAP, "");
            final String rod = row.output().getOrDefault(ModDataComponents.WAND_ROD, "");
            final boolean sceptre = row.output().has(ModDataComponents.WAND_SCEPTRE);
            if (sceptre) {
                sceptres++;
            } else {
                wands++;
            }
            if ("iron".equals(cap) && "wood".equals(rod)) {
                woodIron.add(holder.id() + (sceptre ? " (sceptre)" : " (wand)"));
            }
            // The original's dual gate: cap research AND rod research (AND SCEPTRE),
            // expressed additively because the single research key cannot.
            if (row.research().isEmpty()
                    && row.researchAll().size() == (sceptre ? 3 : 2)
                    && row.researchAll().get(0).equals("CAP_" + cap)
                    && row.researchAll().get(1).equals("ROD_" + rod)
                    && (!sceptre || row.researchAll().get(2).equals("SCEPTRE"))) {
                gated++;
            } else {
                badGate.add(holder.id() + " " + row.researchAll());
            }
            // ArcaneWandRecipe.getAspects: every primal at capCost * rodCost, which
            // is the same number the original stored in the wand's damage value.
            final int cost = row.output().getOrDefault(ModDataComponents.ITEM_META, -1);
            boolean pricedRight = row.aspects().size() == 6 && cost > 0;
            for (final Aspect aspect : row.aspects().getAspects()) {
                if (!aspect.isPrimal() || row.aspects().getAmount(aspect) != cost) {
                    pricedRight = false;
                }
            }
            if (pricedRight) {
                priced++;
            } else {
                badGate.add(holder.id() + " aspects=" + row.aspects().size() + " cost=" + cost);
            }
        }
        helper.assertTrue(wands == 107,
                "expected 107 wand assembly rows in the live RecipeManager (6 caps x 18 rods, "
                        + "minus the wood rod + iron cap refusal), found " + wands);
        helper.assertTrue(sceptres == 108,
                "expected 108 sceptre assembly rows in the live RecipeManager, found " + sceptres);
        helper.assertTrue(woodIron.size() == 1 && woodIron.get(0).endsWith("(sceptre)"),
                "the wood rod + iron cap combo must be refused for wands and allowed for "
                        + "sceptres, found: " + woodIron);
        helper.assertTrue(gated == wands + sceptres,
                "every assembly row must carry the original's cap+rod (+SCEPTRE) gate as "
                        + "research_all with no single-key research, " + gated + " of "
                        + (wands + sceptres) + ", e.g. "
                        + badGate.subList(0, Math.min(3, badGate.size())));
        helper.assertTrue(priced == wands + sceptres,
                "every assembly row must price all six primal aspects, " + priced + " of "
                        + (wands + sceptres));
        ThaumcraftClassic.LOGGER.info(
                "[TC-M6-01-s-b] the live RecipeManager carries {} wand + {} sceptre assembly "
                        + "rows, all gated on cap+rod (+SCEPTRE) and priced on 6 primals",
                wands, sceptres);
        helper.succeed();
    }

    /**
     * The data-parity half: every legal (cap, rod) pair, its exact original
     * cost, its dual research gate, and the refused wood+iron wand combo.
     */
    @GameTest(template = "empty")
    public static void everyLegalComboAssemblesWithTheOriginalCost(final GameTestHelper helper) {
        final RecipeManager manager = helper.getLevel().getRecipeManager();
        final List<ArcaneWorkbenchRecipe> live = new ArrayList<>();
        for (final RecipeHolder<?> holder : manager.getAllRecipesFor(
                CatalogArcaneWorkbench.ARCANE_WORKBENCH_TYPE.get())) {
            if (holder.value() instanceof ArcaneWorkbenchRecipe row
                    && row.output().is(CatalogWands.WAND_CASTING.get())) {
                live.add(row);
            }
        }

        final Player scholar = playerWithResearch(helper, allAssemblyKeys());
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        int wandCombos = 0;
        int sceptreCombos = 0;

        for (final WandCap cap : WandComponents.caps()) {
            for (final WandRod rod : WandComponents.rods()) {
                final boolean refusedWandCombo = "wood".equals(rod.tag()) && "iron".equals(cap.tag());
                final int cost = cap.craftCost() * rod.craftCost();
                final int sceptreCost = (int) (cost * 1.5F);

                // The wand row: the original's grid, through the shipped seam.
                final List<ItemStack> wandGrid = wandGrid(cap, rod);
                final List<ArcaneWorkbenchRecipe> wandMatches =
                        matching(live, new ArcaneWorkbenchRecipe.WorkbenchInput(wandGrid, scholar));
                if (refusedWandCombo) {
                    helper.assertTrue(wandMatches.isEmpty(),
                            "ArcaneWandRecipe refuses rod \"wood\" + cap \"iron\" "
                                    + "(getCraftingResult: !br.equals(\"wood\") || !bc.equals(\"iron\")),"
                                    + " so no wand row may exist for it");
                } else {
                    wandCombos++;
                    helper.assertTrue(wandMatches.size() == 1,
                            cap.tag() + " x " + rod.tag() + " must have exactly one wand assembly row,"
                                    + " got " + wandMatches.size());
                    assertAssemblyRow(helper, wandMatches.get(0), cap, rod, false, cost, registries);
                    assertGridMatchesEveryLayout(helper, wandGrid, scholar, cap, rod, false);
                }

                // The sceptre row: same pair, the original's five-item layout.
                sceptreCombos++;
                final List<ItemStack> sceptreGrid = sceptreGrid(cap, rod);
                final List<ArcaneWorkbenchRecipe> sceptreMatches =
                        matching(live, new ArcaneWorkbenchRecipe.WorkbenchInput(sceptreGrid, scholar));
                helper.assertTrue(sceptreMatches.size() == 1,
                        cap.tag() + " x " + rod.tag() + " must have exactly one sceptre assembly row,"
                                + " got " + sceptreMatches.size());
                assertAssemblyRow(helper, sceptreMatches.get(0), cap, rod, true, sceptreCost, registries);
            }
        }

        helper.assertTrue(wandCombos == 107, "expected 107 legal wand combos, counted " + wandCombos);
        helper.assertTrue(sceptreCombos == 108, "expected 108 sceptre combos, counted " + sceptreCombos);
        ThaumcraftClassic.LOGGER.info("[TC-M6-01-s-b] {} legal wand combos + {} sceptre combos "
                + "assemble through the live RecipeManager at the original cost", wandCombos, sceptreCombos);
        helper.succeed();
    }

    /**
     * The real workbench cycle for one wand: the original's grid, the wand in
     * slot 10, the primal price paid out of its pool, the assembled wand in
     * slot 9 with the cap/rod components and the original damage value.
     */
    @GameTest(template = "empty")
    public static void wandAssemblesThroughTheWorkbench(final GameTestHelper helper) {
        final TileArcaneWorkbench bench = benchAt(helper);
        final WandCap gold = WandComponents.cap("gold");
        final WandRod greatwood = WandComponents.rod("greatwood");
        final int cost = gold.craftCost() * greatwood.craftCost(); // 3 * 3 = 9

        for (int slot = 0; slot < 9; slot++) {
            bench.setItem(slot, ItemStack.EMPTY);
        }
        bench.setItem(2, WandComponents.capStack(gold));
        bench.setItem(4, WandComponents.rodStack(greatwood));
        bench.setItem(6, WandComponents.capStack(gold));

        final ItemStack bank = fundedWand();
        bench.setItem(10, bank);
        final Player player = playerWithResearch(helper, List.of("CAP_gold", "ROD_greatwood"));

        helper.assertTrue(bench.craft(player),
                "cap at (0,2)+(2,0), rod at (1,1), everything else empty is the original's wand grid");
        final ItemStack result = bench.getItem(9);
        helper.assertTrue(!result.isEmpty() && result.is(CatalogWands.WAND_CASTING.get()),
                "the workbench result slot must carry the assembled wand");
        helper.assertTrue(ItemWandCasting.capOf(result) == gold && ItemWandCasting.rodOf(result) == greatwood,
                "the result must carry thaumcraft:wand_cap=gold and thaumcraft:wand_rod=greatwood");
        helper.assertTrue(!ItemWandCasting.isSceptre(result), "a wand is not a sceptre");
        helper.assertTrue(result.getOrDefault(ModDataComponents.ITEM_META.get(), -1) == 9,
                "the original's damage value is capCraftCost * rodCraftCost = 3 * 3 = 9, got "
                        + result.getOrDefault(ModDataComponents.ITEM_META.get(), -1));
        // ConfigItems: greatwood rod capacity 50 -> 50 * 100 centi-vis.
        helper.assertTrue(((ItemWandCasting) result.getItem()).getMaxVis(result) == 5000,
                "a greatwood wand holds 5000 centi-vis, got "
                        + ((ItemWandCasting) result.getItem()).getMaxVis(result));
        // The price is 9 of every primal; the workbench pays in centi-vis (x100).
        for (final Aspect primal : Aspect.getPrimalAspects()) {
            helper.assertTrue(WandVis.getVis(WandVis.poolOf(bank), primal) == FUNDED - cost * 100,
                    "each primal aspect must be debited by exactly the recipe price (" + cost + ")");
        }
        helper.succeed();
    }

    /**
     * The real workbench cycle for one sceptre, including the two refusals the
     * original's layout enforces: the primal charm at (2,0) is part of the
     * grid, and an occupied (2,2) breaks the match.
     */
    @GameTest(template = "empty")
    public static void sceptreAssemblesThroughTheWorkbench(final GameTestHelper helper) {
        final TileArcaneWorkbench bench = benchAt(helper);
        final WandCap thaumium = WandComponents.cap("thaumium");
        final WandRod silverwood = WandComponents.rod("silverwood");
        final int cost = (int) (thaumium.craftCost() * silverwood.craftCost() * 1.5F); // (int)(54*1.5)=81

        for (int slot = 0; slot < 9; slot++) {
            bench.setItem(slot, ItemStack.EMPTY);
        }
        bench.setItem(2, WandComponents.capStack(thaumium));
        bench.setItem(3, WandComponents.capStack(thaumium));
        bench.setItem(4, WandComponents.rodStack(silverwood));
        bench.setItem(6, charm());
        bench.setItem(7, WandComponents.capStack(thaumium));

        final ItemStack bank = fundedWand();
        bench.setItem(10, bank);
        final Player reader = playerWithResearch(helper,
                List.of("CAP_thaumium", "ROD_silverwood", ItemWandCasting.RESEARCH_SCEPTRE));

        helper.assertTrue(bench.craft(reader),
                "caps at (1,0)+(2,1)+(0,2), rod (1,1), primal charm (2,0) is the original's sceptre grid");
        final ItemStack result = bench.getItem(9);
        helper.assertTrue(ItemWandCasting.isSceptre(result),
                "the result must carry the thaumcraft:wand_sceptre marker");
        helper.assertTrue(ItemWandCasting.capOf(result) == thaumium
                        && ItemWandCasting.rodOf(result) == silverwood,
                "the sceptre must carry the cap and rod it was assembled from");
        helper.assertTrue(result.getOrDefault(ModDataComponents.ITEM_META.get(), -1) == 81,
                "the sceptre damage value is (int)(6 * 9 * 1.5) = 81, got "
                        + result.getOrDefault(ModDataComponents.ITEM_META.get(), -1));
        helper.assertTrue(((ItemWandCasting) result.getItem()).getMaxVis(result) == 15000,
                "a sceptre holds 150% of the rod capacity: 9 * 100 * 150 / 100 = 15000, got "
                        + ((ItemWandCasting) result.getItem()).getMaxVis(result));
        for (final Aspect primal : Aspect.getPrimalAspects()) {
            helper.assertTrue(WandVis.getVis(WandVis.poolOf(bank), primal) == FUNDED - cost * 100,
                    "the sceptre price is the x1.5 cost on every primal");
        }

        // The original requires cells (0,0)/(0,1)/(1,2)/(2,2) empty: an extra
        // item on the grid must break the match, and the cycle must be refused.
        bench.clearContent();
        bench.setItem(2, WandComponents.capStack(thaumium));
        bench.setItem(3, WandComponents.capStack(thaumium));
        bench.setItem(4, WandComponents.rodStack(silverwood));
        bench.setItem(6, charm());
        bench.setItem(7, WandComponents.capStack(thaumium));
        bench.setItem(8, WandComponents.capStack(thaumium));
        final ItemStack bank2 = fundedWand();
        bench.setItem(10, bank2);
        helper.assertTrue(!bench.craft(reader),
                "an item in (2,2) must break the original's empty-cell rule");
        helper.assertTrue(bench.getItem(9).isEmpty(), "the refused cycle must produce nothing");
        helper.succeed();
    }

    /**
     * The additive {@code research_all} gate: the original's
     * {@code ArcaneWandRecipe.checkMatch} requires the cap's research AND the
     * rod's research, and the sceptre adds {@code ItemWandCasting.RESEARCH_SCEPTRE}.
     */
    @GameTest(template = "empty")
    public static void assemblyRefusedUntilBothResearchKeysAreKnown(final GameTestHelper helper) {
        final WandCap thaumium = WandComponents.cap("thaumium");
        final WandRod silverwood = WandComponents.rod("silverwood");
        final List<ItemStack> wandGrid = wandGrid(thaumium, silverwood);
        final List<ItemStack> sceptreGrid = sceptreGrid(thaumium, silverwood);

        final Player nobody = playerWithResearch(helper, List.of());
        helper.assertTrue(matching(assemblyRows(helper), input(wandGrid, nobody)).isEmpty(),
                "an unread player must not assemble a wand");

        final Player capOnly = playerWithResearch(helper, List.of("CAP_thaumium"));
        helper.assertTrue(matching(assemblyRows(helper), input(wandGrid, capOnly)).isEmpty(),
                "the cap's research alone must not unlock the rod (ArcaneWandRecipe.checkMatch: br && bc)");

        final Player rodOnly = playerWithResearch(helper, List.of("ROD_silverwood"));
        helper.assertTrue(matching(assemblyRows(helper), input(wandGrid, rodOnly)).isEmpty(),
                "the rod's research alone must not unlock the cap");

        final Player both = playerWithResearch(helper, List.of("CAP_thaumium", "ROD_silverwood"));
        helper.assertTrue(matching(assemblyRows(helper), input(wandGrid, both)).size() == 1,
                "both research keys must unlock the wand");
        helper.assertTrue(matching(assemblyRows(helper), input(sceptreGrid, both)).isEmpty(),
                "the sceptre adds the SCEPTRE gate (ArcaneSceptreRecipe.matches: the first check)");

        final Player sceptreReader = playerWithResearch(helper,
                List.of("CAP_thaumium", "ROD_silverwood", ItemWandCasting.RESEARCH_SCEPTRE));
        helper.assertTrue(matching(assemblyRows(helper), input(sceptreGrid, sceptreReader)).size() == 1,
                "CAP_thaumium + ROD_silverwood + SCEPTRE must unlock the sceptre");
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    /** A fully funded wand for the workbench's slot-10 payment (6 primals x 10000). */
    private static final int FUNDED = 10000;

    private static ItemStack fundedWand() {
        final ItemStack wand = ItemWandCasting.stack(WandComponents.defaultCap(), WandComponents.defaultRod());
        final Map<String, Integer> charge = new HashMap<>();
        for (final Aspect primal : Aspect.getPrimalAspects()) {
            charge.put(primal.getTag(), FUNDED);
        }
        WandVis.setPool(wand, new VisPool(charge));
        return wand;
    }

    /** {@code thaumcraft:itemresource} meta 15 — the primal charm the original names. */
    private static ItemStack charm() {
        final ItemStack charm = new ItemStack(BuiltInRegistries.ITEM.get(id("itemresource")));
        charm.set(ModDataComponents.ITEM_META.get(), 15);
        return charm;
    }

    /** The original's wand grid: caps (0,2) + (2,0), rod (1,1), rest empty. */
    private static List<ItemStack> wandGrid(final WandCap cap, final WandRod rod) {
        final ItemStack[] nine = emptyNine();
        nine[2] = WandComponents.capStack(cap);
        nine[4] = WandComponents.rodStack(rod);
        nine[6] = WandComponents.capStack(cap);
        return List.of(nine);
    }

    /** The original's sceptre grid: caps (1,0)+(2,1)+(0,2), rod (1,1), charm (2,0). */
    private static List<ItemStack> sceptreGrid(final WandCap cap, final WandRod rod) {
        final ItemStack[] nine = emptyNine();
        nine[2] = WandComponents.capStack(cap);
        nine[3] = WandComponents.capStack(cap);
        nine[4] = WandComponents.rodStack(rod);
        nine[6] = charm();
        nine[7] = WandComponents.capStack(cap);
        return List.of(nine);
    }

    private static ItemStack[] emptyNine() {
        final ItemStack[] nine = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            nine[i] = ItemStack.EMPTY;
        }
        return nine;
    }

    private static ArcaneWorkbenchRecipe.WorkbenchInput input(final List<ItemStack> grid,
                                                             final Player player) {
        return new ArcaneWorkbenchRecipe.WorkbenchInput(grid, player);
    }

    private static List<ArcaneWorkbenchRecipe> assemblyRows(final GameTestHelper helper) {
        final List<ArcaneWorkbenchRecipe> live = new ArrayList<>();
        for (final RecipeHolder<?> holder : helper.getLevel().getRecipeManager()
                .getAllRecipesFor(CatalogArcaneWorkbench.ARCANE_WORKBENCH_TYPE.get())) {
            if (holder.value() instanceof ArcaneWorkbenchRecipe row
                    && row.output().is(CatalogWands.WAND_CASTING.get())) {
                live.add(row);
            }
        }
        return live;
    }

    private static List<ArcaneWorkbenchRecipe> matching(final List<ArcaneWorkbenchRecipe> rows,
                                                        final ArcaneWorkbenchRecipe.WorkbenchInput in) {
        final List<ArcaneWorkbenchRecipe> out = new ArrayList<>();
        for (final ArcaneWorkbenchRecipe row : rows) {
            if (row.matches(in, null)) {
                out.add(row);
            }
        }
        return out;
    }

    /** Every research key any assembly row can gate on (6 caps + 18 rods + SCEPTRE). */
    private static List<String> allAssemblyKeys() {
        final List<String> keys = new ArrayList<>();
        for (final WandCap cap : WandComponents.caps()) {
            keys.add(cap.research());
        }
        for (final WandRod rod : WandComponents.rods()) {
            keys.add(rod.research());
        }
        keys.add(ItemWandCasting.RESEARCH_SCEPTRE);
        return keys;
    }

    /**
     * One assembly row's contract: identity components, the original's damage
     * value, the sceptre marker, the dual (+ sceptre) research gate, the
     * original's layout, and the primal price the original put on the recipe.
     */
    private static void assertAssemblyRow(final GameTestHelper helper, final ArcaneWorkbenchRecipe row,
                                          final WandCap cap, final WandRod rod, final boolean sceptre,
                                          final int cost, final HolderLookup.Provider registries) {
        final String pair = cap.tag() + " x " + rod.tag() + (sceptre ? " (sceptre)" : " (wand)");
        final ItemStack out = row.assemble(input(List.of(emptyNine()), null), registries);

        helper.assertTrue(cap.tag().equals(out.get(ModDataComponents.WAND_CAP.get())),
                pair + ": the result must carry thaumcraft:wand_cap=" + cap.tag());
        helper.assertTrue(rod.tag().equals(out.get(ModDataComponents.WAND_ROD.get())),
                pair + ": the result must carry thaumcraft:wand_rod=" + rod.tag());
        helper.assertTrue(ItemWandCasting.isSceptre(out) == sceptre,
                pair + ": the sceptre marker must follow the recipe kind");
        helper.assertTrue(out.getOrDefault(ModDataComponents.ITEM_META.get(), -1) == cost,
                pair + ": the damage value must be capCraftCost * rodCraftCost"
                        + (sceptre ? " * 1.5F" : "") + " = " + cost + ", got "
                        + out.getOrDefault(ModDataComponents.ITEM_META.get(), -1));
        helper.assertTrue(ItemWandCasting.damageValue(cap, rod, sceptre) == cost,
                pair + ": ItemWandCasting.damageValue must agree with the shipped row");

        final List<String> wanted = new ArrayList<>();
        wanted.add(cap.research());
        wanted.add(rod.research());
        if (sceptre) {
            wanted.add(ItemWandCasting.RESEARCH_SCEPTRE);
        }
        helper.assertTrue(row.researchAll().equals(wanted),
                pair + ": the research gate must be " + wanted + ", got " + row.researchAll());
        helper.assertTrue(row.research().isEmpty(),
                pair + ": the dynamic original carries no single research key");

        // ArcaneWandRecipe.getAspects: every primal at capCost * rodCost.
        final AspectList aspects = row.aspects();
        helper.assertTrue(aspects.getAspects().size() == 6,
                pair + ": the original prices all six primal aspects, got "
                        + aspects.getAspects().size());
        for (final Aspect primal : Aspect.getPrimalAspects()) {
            helper.assertTrue(aspects.getAmount(primal) == cost,
                    pair + ": " + primal.getTag() + " must cost " + cost + " vis, got "
                            + aspects.getAmount(primal));
        }

        helper.assertTrue(row.pattern().equals(sceptre ? SCEPTRE_PATTERN : WAND_PATTERN),
                pair + ": the layout must be the original recipe's, got " + row.pattern());

        final int expectedVis = sceptre ? 150 : 100;
        helper.assertTrue(((ItemWandCasting) out.getItem()).getMaxVis(out)
                        == rod.capacity() * expectedVis,
                pair + ": capacity is rod capacity x " + expectedVis + " centi-vis, got "
                        + ((ItemWandCasting) out.getItem()).getMaxVis(out));
    }

    /**
     * The shipped row must match the grid in both orientations — the data
     * shaped recipe carries the original's {@code mirrored = true} default, so
     * the horizontally flipped layout works as well.
     */
    private static void assertGridMatchesEveryLayout(final GameTestHelper helper,
                                                     final List<ItemStack> grid, final Player player,
                                                     final WandCap cap, final WandRod rod,
                                                     final boolean sceptre) {
        helper.assertTrue(matching(assemblyRows(helper), input(grid, player)).size() == 1,
                cap.tag() + " x " + rod.tag() + ": the shipped row must match its grid once");
    }

    private static final List<String> WAND_PATTERN = List.of("  C", " R ", "C  ");
    private static final List<String> SCEPTRE_PATTERN = List.of("  C", "CR ", "FC ");

    private static TileArcaneWorkbench benchAt(final GameTestHelper helper) {
        helper.setBlock(1, 1, 1, CatalogArcaneWorkbench.ARCANE_WORKBENCH_BLOCK.get());
        final BlockEntity entity = helper.getBlockEntity(new BlockPos(1, 1, 1));
        helper.assertTrue(entity instanceof TileArcaneWorkbench,
                "the workbench block must create its TileArcaneWorkbench entity");
        return (TileArcaneWorkbench) entity;
    }

    /** A mock player whose TC-M3-02 knowledge has the listed research done. */
    private static Player playerWithResearch(final GameTestHelper helper, final List<String> keys) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        for (final String key : keys) {
            knowledge = knowledge.withResearchCompleted(key);
        }
        player.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
        return player;
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
    }
}
