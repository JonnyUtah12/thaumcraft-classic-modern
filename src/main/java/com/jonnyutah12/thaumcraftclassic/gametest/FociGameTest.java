package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryItem;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessorySlot;
import com.jonnyutah12.thaumcraftclassic.api.foci.Focus;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.casting.FocusBehaviors;
import com.jonnyutah12.thaumcraftclassic.casting.WandCasting;
import com.jonnyutah12.thaumcraftclassic.foci.FocusPouchContents;
import com.jonnyutah12.thaumcraftclassic.foci.Focuses;
import com.jonnyutah12.thaumcraftclassic.items.foci.ItemFocus;
import com.jonnyutah12.thaumcraftclassic.items.foci.ItemFocusPouch;
import com.jonnyutah12.thaumcraftclassic.items.wands.ItemWandCasting;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogArcaneWorkbench;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFoci;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogWands;
import com.jonnyutah12.thaumcraftclassic.wands.WandComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Random;
import java.util.Set;

/**
 * TC-M6-02 acceptance probes: the focus family on the live registries of the
 * gameTestServer — ids and identity, the original prices resolved against the
 * real aspect registry, the casting entry point's vis payment/refund contract,
 * and the pouch's storage plus the arcane focus rows that were inert until
 * these ids existed.
 *
 * <p>The casting probe registers its own behaviour for the duration of the
 * probe ({@code FocusBehaviors.register} then {@code unregister} in a
 * {@code finally}) because this slice ships none — per-focus spell bodies are
 * TC-M6-03-s — and an unported focus must never burn a wand's charge.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FociGameTest {

    private FociGameTest() {
    }

    /** The pouch and all ten focus ids resolve through CatalogFoci, with their table identity. */
    @GameTest(template = "empty")
    public static void focusContentIsRegistered(final GameTestHelper helper) {
        final ResourceLocation pouchId = id("focuspouch");
        helper.assertTrue(BuiltInRegistries.ITEM.get(pouchId) == CatalogFoci.FOCUS_POUCH.get(),
                "thaumcraft:focuspouch is not registered through CatalogFoci");
        helper.assertTrue(CatalogFoci.FOCUS_POUCH.get() instanceof AccessoryItem,
                "the original pouch is ItemFocusPouchBauble, so the port item must be an AccessoryItem");
        helper.assertTrue(CatalogFoci.FOCUS_POUCH.get().applicableSlots(new ItemStack(CatalogFoci.FOCUS_POUCH.get()))
                        .equals(Set.of(AccessorySlot.BELT)),
                "ItemFocusPouchBauble.getBaubleType is BaubleType.BELT");

        for (final Focus focus : Focuses.all()) {
            final ResourceLocation itemId = id("focus" + focus.tag());
            helper.assertTrue(BuiltInRegistries.ITEM.get(itemId) == CatalogFoci.item(focus.tag()).get(),
                    itemId + " is not registered through CatalogFoci");
            final ItemStack stack = new ItemStack(CatalogFoci.item(focus.tag()).get());
            helper.assertTrue(stack.getItem() instanceof ItemFocus,
                    itemId + " must be an ItemFocus to be pouchable and castable");
            helper.assertTrue(ItemFocus.focusOf(stack) == focus,
                    itemId + " does not carry its Focus table entry");
            helper.assertTrue(stack.getMaxStackSize() == 1,
                    "ItemFocusBasic set maxStackSize = 1, got " + stack.getMaxStackSize());
        }
        helper.succeed();
    }

    /**
     * The original prices, resolved against the live aspect registry, and the
     * primal focus's rolled price.
     */
    @GameTest(template = "empty")
    public static void focusPricesFollowTheOriginal(final GameTestHelper helper) {
        final Aspect ignis = Aspect.getAspect("ignis");
        final Aspect terra = Aspect.getAspect("terra");
        final Aspect ordo = Aspect.getAspect("ordo");
        final Aspect aqua = Aspect.getAspect("aqua");
        final Aspect perditio = Aspect.getAspect("perditio");
        final Aspect aer = Aspect.getAspect("aer");
        for (final Aspect aspect : new Aspect[] {ignis, terra, ordo, aqua, perditio, aer}) {
            helper.assertTrue(aspect != null, "the aspect registry is not live");
        }

        // ItemFocusFire.getVisCost on a bare stack: FIRE 10 centi-vis.
        final ItemFocus fire = CatalogFoci.item("fire").get();
        final AspectList fireCost = fire.getVisCost(new ItemStack(fire));
        helper.assertTrue(fireCost.getAmount(ignis) == 10 && fireCost.size() == 1,
                "the fire focus costs ignis 10 with no upgrades, got " + fireCost);
        helper.assertTrue(fire.isVisCostPerTick(new ItemStack(fire)),
                "ItemFocusFire.isVisCostPerTick is true");
        helper.assertTrue(fire.getFocusColor(new ItemStack(fire)) == 15028484,
                "ItemFocusFire.getFocusColor is 15028484");

        // ItemFocusWarding.getVisCost: EARTH 25, ORDER 25, WATER 10.
        final ItemFocus warding = CatalogFoci.item("warding").get();
        final AspectList wardingCost = warding.getVisCost(new ItemStack(warding));
        helper.assertTrue(wardingCost.getAmount(terra) == 25
                        && wardingCost.getAmount(ordo) == 25
                        && wardingCost.getAmount(aqua) == 10,
                "the warding focus costs terra/ordo 25 + aqua 10, got " + wardingCost);
        helper.assertTrue(warding.getActivationCooldown(new ItemStack(warding)) == 0,
                "ItemFocusWarding declares no cooldown");

        // ItemFocusPrimal.getVisCost rolls 50 + nextInt(5) * 50 for every primal.
        final ItemFocus primal = CatalogFoci.item("primal").get();
        final AspectList primalCost = primal.getVisCost(new ItemStack(primal));
        helper.assertTrue(primalCost.size() == Aspect.getPrimalAspects().size(),
                "the primal focus prices every primal, got " + primalCost.size());
        for (final Aspect aspect : primalCost.getAspects()) {
            final int amount = primalCost.getAmount(aspect);
            helper.assertTrue(amount >= 50 && amount <= 250 && amount % 50 == 0,
                    "primal " + aspect.getTag() + " rolled " + amount + ", outside 50..250 step 50");
        }
        final AspectList seeded = Focuses.primalCost(new Random(7L));
        helper.assertTrue(seeded.size() == Aspect.getPrimalAspects().size(),
                "Focuses.primalCost must price every primal");
        helper.succeed();
    }

    /**
     * The casting entry point: a focus with no behaviour must not touch the
     * wand, a focus with a behaviour pays the TC-M4-02 modified price, an
     * unaffordable cast spends nothing, and a behaviour that does nothing has
     * its payment refunded.
     */
    @GameTest(template = "empty")
    public static void castingPaysVisThroughTheCostChain(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemWandCasting wandItem = CatalogWands.WAND_CASTING.get();
        final ItemStack wand = ItemWandCasting.stack(WandComponents.cap("iron"), WandComponents.rod("wood"));
        final ItemStack focus = new ItemStack(CatalogFoci.item("fire").get());
        ItemWandCasting.setFocus(wand, focus);
        helper.assertTrue(ItemWandCasting.focusItem(wand).getItem() == focus.getItem(),
                "the wand's focus must round-trip through thaumcraft:wand_focus");

        final Aspect ignis = Aspect.getAspect("ignis");
        wandItem.addVis(wand, ignis, 10, true);

        // 1. No behaviour registered: the cast is unsupported and pays nothing.
        helper.assertTrue(!FocusBehaviors.isRegistered("fire"),
                "this slice ships no focus behaviours; TC-M6-03-s fills the seam");
        WandCasting.CastResult result = WandCasting.cast(wandItem, wand, player, helper.getLevel(),
                InteractionHand.MAIN_HAND);
        helper.assertTrue(result.status() == WandCasting.Status.UNSUPPORTED,
                "a focus with no behaviour must report UNSUPPORTED, got " + result.status());
        helper.assertTrue(wandItem.getVis(wand, ignis) == 1000,
                "an unsupported cast must not spend, got " + wandItem.getVis(wand, ignis));

        int casts = 0;
        try {
            FocusBehaviors.register("fire", context -> {
                return true;
            });
            helper.assertTrue(FocusBehaviors.isRegistered("fire"), "the probe behaviour must register");

            // 2. Supported and affordable: the iron cap's 1.1F modifier makes the
            //    ignis 10 price (int)(10 * 1.1) = 11 centi-vis.
            result = WandCasting.cast(wandItem, wand, player, helper.getLevel(), InteractionHand.MAIN_HAND);
            helper.assertTrue(result.status() == WandCasting.Status.CAST,
                    "a supported, affordable cast must succeed, got " + result.status());
            helper.assertTrue(result.focus() == Focuses.byTag("fire"), "the result must name the cast focus");
            helper.assertTrue(result.cost().getAmount(ignis) == 11,
                    "the iron cap's 1.1F modifier must make ignis 10 cost 11 centi-vis, got "
                            + result.cost().getAmount(ignis));
            helper.assertTrue(wandItem.getVis(wand, ignis) == 1000 - 11,
                    "the price must be debited, got " + wandItem.getVis(wand, ignis));

            // 3. A behaviour that did nothing has its payment refunded.
            FocusBehaviors.register("fire", context -> false);
            result = WandCasting.cast(wandItem, wand, player, helper.getLevel(), InteractionHand.MAIN_HAND);
            helper.assertTrue(result.status() == WandCasting.Status.NO_EFFECT,
                    "a no-effect behaviour must report NO_EFFECT, got " + result.status());
            helper.assertTrue(result.untouched(), "a refunded cast reports nothing paid");
            helper.assertTrue(wandItem.getVis(wand, ignis) == 1000 - 11,
                    "the refund must restore the exact charge, got " + wandItem.getVis(wand, ignis));

            // 4. Unaffordable: the wand is emptied and the cast spends nothing.
            wandItem.storeVis(wand, ignis, 5);
            result = WandCasting.cast(wandItem, wand, player, helper.getLevel(), InteractionHand.MAIN_HAND);
            helper.assertTrue(result.status() == WandCasting.Status.UNPAID,
                    "a wand that cannot pay must report UNPAID, got " + result.status());
            helper.assertTrue(wandItem.getVis(wand, ignis) == 5,
                    "a failed payment must spend nothing, got " + wandItem.getVis(wand, ignis));

            casts++;
        } finally {
            FocusBehaviors.unregister("fire");
        }
        helper.assertTrue(casts == 1, "the casting probe did not run");
        helper.assertTrue(!FocusBehaviors.isRegistered("fire"), "the probe behaviour must be cleaned up");
        helper.succeed();
    }

    /**
     * The pouch: slot storage through the data component, a save round-trip,
     * and the arcane focus crafting rows that were inert until these ids
     * existed.
     */
    @GameTest(template = "empty")
    public static void focusPouchStoresFociAndItsRecipesResolve(final GameTestHelper helper) {
        final ItemFocusPouch pouchItem = CatalogFoci.FOCUS_POUCH.get();
        final ItemStack pouch = new ItemStack(pouchItem);
        helper.assertTrue(ItemFocusPouch.contents(pouch).size() == 0, "a fresh pouch is empty");

        final ItemStack fire = new ItemStack(CatalogFoci.item("fire").get());
        final ItemStack frost = new ItemStack(CatalogFoci.item("frost").get());
        helper.assertTrue(ItemFocusPouch.insert(pouch, fire), "a focus must fit in an empty pouch");
        helper.assertTrue(ItemFocusPouch.insert(pouch, frost), "a second focus must fit");
        helper.assertTrue(!ItemFocusPouch.insert(pouch, pouch), "a nested pouch is not a focus");
        helper.assertTrue(!ItemFocusPouch.insert(pouch, new ItemStack(net.minecraft.world.item.Items.STICK)),
                "a non-focus item is refused (InventoryFocusPouch.isItemValidForSlot)");
        helper.assertTrue(ItemFocusPouch.contents(pouch).size() == 2, "two foci are stored");
        helper.assertTrue(ItemFocusPouch.hasFocus(pouch, "fire") && ItemFocusPouch.hasFocus(pouch, "frost"),
                "both stored foci are findable by tag");
        helper.assertTrue(ItemFocusPouch.contents(pouch).firstFreeSlot() == 2,
                "the lowest empty slot of a two-focus pouch is 2");

        // The array shape of the original getInventory/setInventory: 18 slots.
        final ItemStack[] slots = ItemFocusPouch.getInventory(pouch);
        helper.assertTrue(slots.length == ItemFocusPouch.CAPACITY,
                "getInventory yields the original ItemStack[18], got " + slots.length);

        // Charge-independent save path: the pouch and its contents survive NBT.
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        final ItemStack restored = ItemStack.parseOptional(registries, (CompoundTag) pouch.save(registries));
        helper.assertTrue(!restored.isEmpty() && restored.getItem() == pouchItem,
                "the pouch stack did not survive an NBT round-trip");
        helper.assertTrue(ItemFocusPouch.hasFocus(restored, "fire") && ItemFocusPouch.hasFocus(restored, "frost"),
                "the stored foci must persist through the save path");

        // Extract removes exactly the named focus and leaves the rest.
        final ItemStack extracted = ItemFocusPouch.extract(pouch, "fire").orElse(ItemStack.EMPTY);
        helper.assertTrue(extracted.getItem() == fire.getItem(), "extract must return the named focus");
        helper.assertTrue(!ItemFocusPouch.hasFocus(pouch, "fire") && ItemFocusPouch.hasFocus(pouch, "frost"),
                "extract must remove only the named focus");
        helper.assertTrue(ItemFocusPouch.extract(pouch, "fire").isEmpty(), "extracting twice yields nothing");

        // The seven arcane_shaped focus/pouch rows were gated on these ids.
        int focusRecipes = 0;
        for (final RecipeHolder<?> holder : helper.getLevel().getRecipeManager()
                .getAllRecipesFor(CatalogArcaneWorkbench.ARCANE_WORKBENCH_TYPE.get())) {
            final ItemStack result = holder.value().getResultItem(registries);
            if (result.getItem() instanceof ItemFocus || result.getItem() instanceof ItemFocusPouch) {
                focusRecipes++;
            }
        }
        // recipe.153-159 (FOCUSFIRE, FOCUSFROST, FOCUSSHOCK, FOCUSTRADE,
        // FOCUSEXCAVATION, FOCUSPRIMAL, FOCUSPOUCH).
        helper.assertTrue(focusRecipes >= 7,
                "expected the 7 already-shipped arcane focus/pouch rows to resolve, got " + focusRecipes);
        ThaumcraftClassic.LOGGER.info("[TC-M6-02] {} arcane focus/pouch crafting rows resolve", focusRecipes);
        helper.succeed();
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
    }
}
