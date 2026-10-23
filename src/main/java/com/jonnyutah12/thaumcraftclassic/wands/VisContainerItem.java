package com.jonnyutah12.thaumcraftclassic.wands;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The original's shared vis-container surface (TC-M8-02): the method set
 * {@code ItemWandCasting} and {@code ItemAmuletVis} both implemented in
 * {@code thaumcraft.common.items.wands.ItemWandCasting} /
 * {@code common/items/baubles/ItemAmuletVis.java}, which TC4 code duck-typed
 * between (e.g. the vis amulet's wand-transfer branch,
 * {@code ItemAmuletVis.onWornTick}, calls {@code getAspectsWithRoom},
 * {@code getMaxVis}, {@code getVis}, {@code storeVis} on the held wand).
 *
 * <p>All amounts are centi-vis (0.01 vis), as in the original;
 * {@link #addVis} takes whole vis and converts, matching the original's
 * {@code amount * 100}.</p>
 *
 * <p>Implementors: {@code items.baubles.ItemAmuletVis} (TC-M8-02) and
 * {@code ItemWandCasting} when it lands (TC-M6-01). Distinct from the
 * pedestal-facing {@code IVisContainerItem} (TC-M6-04d), which carries
 * component tags and a void real-vis addVis for the wand pedestal.</p>
 */
public interface VisContainerItem {

    /** Total per-aspect capacity, centi-vis (original getMaxVis). */
    int getMaxVis(ItemStack stack);

    /** Stored charge of one aspect, centi-vis (original getVis). */
    int getVis(ItemStack stack, Aspect aspect);

    /** Absolute write of one aspect's charge, centi-vis (original storeVis). */
    void storeVis(ItemStack stack, Aspect aspect, int amount);

    /** Aspects still below capacity (original getAspectsWithRoom). */
    AspectList getAspectsWithRoom(ItemStack stack);

    /** Every stored aspect and its charge (original getAllVis). */
    AspectList getAllVis(ItemStack stack);

    /**
     * Withdraw many aspects, all-or-nothing (original consumeAllVis
     * signature, ItemAmuletVis.java:151-165; {@code doit=false} only tests).
     */
    boolean consumeAllVis(ItemStack stack, Player player, AspectList centiCosts,
                          boolean doit, boolean crafting);

    /** Add whole vis; returns the amount that did not fit, in whole vis (original addVis). */
    int addVis(ItemStack stack, Aspect aspect, int amount, boolean doit);

    /** Add centi-vis; returns the amount that did not fit, in centi-vis (original addRealVis). */
    int addRealVis(ItemStack stack, Aspect aspect, int centiAmount, boolean doit);
}
