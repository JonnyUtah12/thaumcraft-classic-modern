package com.jonnyutah12.thaumcraftclassic.wands;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import net.minecraft.world.item.ItemStack;

/**
 * Vis-carrying item contract (content-mapping.csv consumers: tilewandpedestal).
 *
 * <p>Original: the wand pedestal checked {@code instanceof ItemWandCasting} /
 * {@code ItemAmuletVis} directly and called getCap/getRod (component tag
 * strings), getAspectsWithRoom and addVis. The wand and amulet item classes
 * belong to TC-M6-01 / TC-M8-02; this interface carries the same surface so
 * those items implement it when they land. getCap/getRod return the component
 * tag strings directly (the original returned WandCapData/WandRodData objects
 * whose only use here was {@code .getTag()}).</p>
 */
public interface IVisContainerItem {

    /** Aspects the stack can still accept (original getAspectsWithRoom). */
    AspectList getAspectsWithRoom(ItemStack stack);

    /** Add vis to the stack (original addVis(stack, aspect, amount, doUpdate)). */
    void addVis(ItemStack stack, Aspect aspect, int amount, boolean doUpdate);

    /** Cap component tag, e.g. "iron" (original getCap(stack).getTag()). */
    String getCapTag(ItemStack stack);

    /** Rod component tag, e.g. "wood" (original getRod(stack).getTag()). */
    String getRodTag(ItemStack stack);
}
