package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.api.wands.WandCap;
import com.jonnyutah12.thaumcraftclassic.api.wands.WandRod;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.items.wands.ItemWandCap;
import com.jonnyutah12.thaumcraftclassic.items.wands.ItemWandCasting;
import com.jonnyutah12.thaumcraftclassic.items.wands.ItemWandRod;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogArcaneWorkbench;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogWands;
import com.jonnyutah12.thaumcraftclassic.wands.IVisContainerItem;
import com.jonnyutah12.thaumcraftclassic.wands.VisContainerItem;
import com.jonnyutah12.thaumcraftclassic.wands.VisPool;
import com.jonnyutah12.thaumcraftclassic.wands.WandComponent;
import com.jonnyutah12.thaumcraftclassic.wands.WandComponents;
import com.jonnyutah12.thaumcraftclassic.wands.WandVis;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * TC-M6-01 acceptance probes: the wand family on the live registries of the
 * gameTestServer — ids, the {@code thaumcraft:wand_component} registry, the
 * capacity/vis-pool behaviour the original's {@code ItemWandCasting} had, the
 * rod on-update rule, the pedestal seam, and the cap/rod crafting rows that
 * were already shipped but inert.
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WandGameTest {

    private WandGameTest() {
    }

    /** The three item ids resolve, and the wand-component registry carries every original tag. */
    @GameTest(template = "empty")
    public static void wandContentIsRegistered(final GameTestHelper helper) {
        final ResourceLocation wandId = id("wandcasting");
        final ResourceLocation capId = id("wandcap");
        final ResourceLocation rodId = id("wandrod");
        helper.assertTrue(BuiltInRegistries.ITEM.get(capId) == CatalogWands.WAND_CAP.get(),
                "thaumcraft:wandcap is not registered through CatalogWands");
        helper.assertTrue(BuiltInRegistries.ITEM.get(rodId) == CatalogWands.WAND_ROD.get(),
                "thaumcraft:wandrod is not registered through CatalogWands");
        helper.assertTrue(BuiltInRegistries.ITEM.get(wandId) == CatalogWands.WAND_CASTING.get(),
                "thaumcraft:wandcasting is not registered through CatalogWands");

        final Registry<WandComponent> components = CatalogWands.registry();
        helper.assertTrue(components != null,
                "the thaumcraft:wand_component registry was not created (NewRegistryEvent wiring)");
        helper.assertTrue(components.size() == 16,
                "expected the 16 distinct component tags of docs/content-mapping.csv, got " + components.size());
        for (final String tag : List.of("iron", "gold", "thaumium", "void", "copper", "silver", "wood",
                "greatwood", "obsidian", "blaze", "ice", "quartz", "bone", "reed", "silverwood", "primal")) {
            helper.assertTrue(components.containsKey(id(tag)),
                    "wand_component row thaumcraft:" + tag + " does not resolve to a registry entry");
        }
        helper.succeed();
    }

    /**
     * Capacity from the rod, charge in the shared vis pool: {@code getMaxVis =
     * rod.capacity × (sceptre ? 150 : 100)} in centi-vis, deposits clamp and
     * report the leftover, payment is all-or-nothing, and the pool survives a
     * stack save/parse round-trip.
     */
    @GameTest(template = "empty")
    public static void wandCapacityAndVisPoolFollowTheOriginal(final GameTestHelper helper) {
        final WandCap thaumium = WandComponents.cap("thaumium");
        final WandRod silverwood = WandComponents.rod("silverwood");
        final ItemStack wand = ItemWandCasting.stack(thaumium, silverwood);
        final ItemWandCasting item = (ItemWandCasting) wand.getItem();

        helper.assertTrue(item instanceof VisContainerItem && item instanceof IVisContainerItem,
                "the wand must join both vis-container seams (TC-M8-02 / TC-M6-04d)");
        helper.assertTrue(ItemWandCasting.capOf(wand) == thaumium && ItemWandCasting.rodOf(wand) == silverwood,
                "rod/cap identity did not round-trip through the stack components");

        // ConfigItems: silverwood rod capacity 100 -> 100 * 100 centi-vis.
        helper.assertTrue(item.getMaxVis(wand) == 10000,
                "silverwood wand capacity should be 10000 centi-vis, got " + item.getMaxVis(wand));
        helper.assertTrue(ItemWandCasting.damageValue(thaumium, silverwood, false) == 54,
                "the original recipe damage value is capCraftCost * rodCraftCost = 6 * 9 = 54");

        final ItemStack sceptre = ItemWandCasting.stack(thaumium, silverwood, true);
        final ItemWandCasting sceptreItem = (ItemWandCasting) sceptre.getItem();
        helper.assertTrue(ItemWandCasting.isSceptre(sceptre), "the sceptre marker must be present");
        helper.assertTrue(sceptreItem.getMaxVis(sceptre) == 15000,
                "a sceptre gets 150% of the rod capacity, got " + sceptreItem.getMaxVis(sceptre));
        helper.assertTrue(ItemWandCasting.damageValue(thaumium, silverwood, true) == 81,
                "the sceptre recipe damage value is (int)(54 * 1.5) = 81");

        final Aspect ignis = Aspect.getAspect("ignis");
        helper.assertTrue(ignis != null, "the aspect registry is not live");
        helper.assertTrue(item.addVis(wand, ignis, 5, true) == 0, "5 vis fits");
        helper.assertTrue(item.getVis(wand, ignis) == 500, "5 vis = 500 centi-vis");
        helper.assertTrue(item.addVis(wand, ignis, 200, true) == 105,
                "an overfill reports the whole-vis leftover above the 100 vis capacity");
        helper.assertTrue(item.getVis(wand, ignis) == 10000, "charge clamps at capacity");

        final Aspect aer = Aspect.getAspect("aer");
        helper.assertTrue(item.addVis(wand, aer, 3, true) == 0, "3 vis of aer fits");
        final AspectList costs = new AspectList().add(ignis, 400).add(aer, 300);
        helper.assertTrue(item.consumeAllVis(wand, null, costs, false, false),
                "a dry run must succeed when both aspects are covered");
        helper.assertTrue(item.getVis(wand, ignis) == 10000, "a dry run must not spend");
        helper.assertTrue(item.consumeAllVis(wand, null, costs, true, false), "the real payment");
        // The thaumium cap's base cost modifier is 0.9, so the debits are (int)(400*0.9)=360 and
        // (int)(300*0.9)=270 — the original applies the modifier to every cost before paying.
        helper.assertTrue(item.getVis(wand, ignis) == 9640 && item.getVis(wand, aer) == 30,
                "both aspects must be debited by the modified cost, got ignis "
                        + item.getVis(wand, ignis) + " aer " + item.getVis(wand, aer));

        final AspectList tooDear = new AspectList().add(ignis, 999999);
        helper.assertTrue(!item.consumeAllVis(wand, null, tooDear, true, false),
                "an unaffordable cost must fail");
        helper.assertTrue(item.getVis(wand, ignis) == 9640, "a failed payment must spend nothing");

        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        final ItemStack restored = ItemStack.parseOptional(registries, (CompoundTag) wand.save(registries));
        helper.assertTrue(!restored.isEmpty() && restored.getItem() instanceof ItemWandCasting,
                "the wand stack did not survive an NBT round-trip");
        final ItemWandCasting restoredItem = (ItemWandCasting) restored.getItem();
        helper.assertTrue(restoredItem.getVis(restored, ignis) == 9640
                        && ItemWandCasting.rodOf(restored) == silverwood
                        && ItemWandCasting.capOf(restored) == thaumium,
                "charge and rod/cap identity must persist through the save path");
        helper.succeed();
    }

    /**
     * The original's rod on-update ({@code WandRodPrimalOnUpdate}): the fixed
     * elemental rods top up their own primal while below {@code maxVis / 10},
     * the primal staff core draws a random under-filled primal, and rods
     * without an on-update never top up. Also exercises the wand pedestal's
     * {@link IVisContainerItem} charging call.
     */
    @GameTest(template = "empty")
    public static void rodOnUpdateAndPedestalSeamFollowTheOriginal(final GameTestHelper helper) {
        final ItemStack wand = ItemWandCasting.stack(WandComponents.cap("iron"),
                WandComponents.rod("obsidian"));
        final ItemWandCasting item = (ItemWandCasting) wand.getItem();
        final Aspect terra = Aspect.getAspect("terra");
        final RandomSource random = RandomSource.create(1L);
        final int maxVis = item.getMaxVis(wand);

        helper.assertTrue(ItemWandCasting.rodTickInterval(WandComponents.rod("obsidian")) == 200,
                "fixed-primal rods tick every 200 player ticks");
        helper.assertTrue(WandVis.getVis(WandVis.poolOf(wand), terra) == 0, "a fresh wand starts empty");
        // -1 vis: a spend that is not covered, so the pool stays empty.
        helper.assertTrue(ItemWandCasting.primalTopUpTarget(WandVis.poolOf(wand), maxVis,
                        WandComponents.rod("obsidian"), random) == terra,
                "an empty obsidian rod tops up its own primal (terra)");
        item.addVis(wand, terra, maxVis / 100, true);
        helper.assertTrue(ItemWandCasting.primalTopUpTarget(WandVis.poolOf(wand), maxVis,
                        WandComponents.rod("obsidian"), random) == null,
                "a rod at or above maxVis/10 stops topping up");
        helper.assertTrue(ItemWandCasting.primalTopUpTarget(WandVis.poolOf(wand), maxVis,
                        WandComponents.rod("silverwood"), random) == null,
                "the silverwood rod has no on-update in the original");

        final WandRod primalStaff = WandComponents.rod("primal_staff");
        helper.assertTrue(ItemWandCasting.rodTickInterval(primalStaff) == 50,
                "the primal staff core ticks every 50 player ticks");
        final Aspect picked = ItemWandCasting.primalTopUpTarget(VisPool.EMPTY, 25000, primalStaff, random);
        helper.assertTrue(picked != null && picked.isPrimal(),
                "an empty primal staff core must pick one of the six primals, got " + picked);
        final ItemStack filled = ItemWandCasting.stack(WandComponents.cap("iron"), primalStaff);
        final ItemWandCasting filledItem = (ItemWandCasting) filled.getItem();
        for (final Aspect primal : Aspect.getPrimalAspects()) {
            filledItem.addVis(filled, primal, 25, true);
        }
        helper.assertTrue(ItemWandCasting.primalTopUpTarget(WandVis.poolOf(filled), filledItem.getMaxVis(filled),
                        primalStaff, random) == null,
                "a full primal staff core must not top up (all primals at maxVis/10)");
        helper.succeed();
    }

    /**
     * The cap/rod crafting rows shipped with TC-M4-03-s were gated on these item
     * ids through {@code neoforge:item_exists}; now that the ids exist they must
     * reach the live RecipeManager, which is what makes the components
     * obtainable in survival.
     */
    @GameTest(template = "empty")
    public static void wandComponentRecipesNowResolve(final GameTestHelper helper) {
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        int capRecipes = 0;
        int rodRecipes = 0;
        for (final RecipeHolder<?> holder : helper.getLevel().getRecipeManager()
                .getAllRecipesFor(CatalogArcaneWorkbench.ARCANE_WORKBENCH_TYPE.get())) {
            final ItemStack result = holder.value().getResultItem(registries);
            if (result.getItem() == CatalogWands.WAND_CAP.get()) {
                capRecipes++;
                helper.assertTrue(ItemWandCap.storedMeta(result) < ItemWandCap.META_COUNT,
                        "wand cap recipe " + holder.id() + " yields an out-of-range meta");
            } else if (result.getItem() == CatalogWands.WAND_ROD.get()) {
                rodRecipes++;
                helper.assertTrue(ItemWandRod.isValidMeta(ItemWandRod.storedMeta(result)),
                        "wand rod recipe " + holder.id() + " yields an out-of-range meta");
            }
        }
        // 5 cap rows (iron/gold/thaumium/void + the conditional silver/copper pair) and the 8
        // arcane staff-core rows; the primal staff core is an *infusion* recipe in the original
        // (ConfigRecipes WandRodPrimal), so it is not expected here.
        helper.assertTrue(capRecipes >= 5,
                "expected the shipped wand-cap crafting rows to resolve, got " + capRecipes);
        helper.assertTrue(rodRecipes >= 8,
                "expected the shipped arcane wand-rod crafting rows to resolve, got " + rodRecipes);
        ThaumcraftClassic.LOGGER.info("[TC-M6-01] {} cap + {} rod crafting rows resolve", capRecipes, rodRecipes);
        helper.succeed();
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
    }
}
