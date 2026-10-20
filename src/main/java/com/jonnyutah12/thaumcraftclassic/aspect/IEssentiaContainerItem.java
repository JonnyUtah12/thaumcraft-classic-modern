package com.jonnyutah12.thaumcraftclassic.aspect;

import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * Item contract for stacks that carry essentia (original
 * {@code thaumcraft.api.aspects.IEssentiaContainerItem}). The default
 * implementations back the contract with the
 * {@code thaumcraft:essentia_container} data component instead of the
 * original's raw NBT subcompound; the null-when-empty return of
 * {@code getAspects} is preserved because callers branch on it.
 *
 * <p>Implemented by ItemResource (jar label), ItemManaBean, ItemWispEssence
 * and ItemCrystalEssence (TC-M6-04a); the essentia jar/tile consumers
 * (TC-M4-06) implement it the same way.</p>
 */
public interface IEssentiaContainerItem {

    /** Original contract: the stack's aspects, or {@code null} when empty. */
    default AspectList getAspects(final ItemStack itemstack) {
        final EssentiaContainer container = itemstack.get(ModDataComponents.ESSENTIA_CONTAINER.get());
        return container == null || container.isEmpty() ? null : container.toAspectList();
    }

    default void setAspects(final ItemStack itemstack, final AspectList aspects) {
        if (aspects == null || aspects.size() == 0) {
            itemstack.remove(ModDataComponents.ESSENTIA_CONTAINER.get());
        } else {
            itemstack.set(ModDataComponents.ESSENTIA_CONTAINER.get(),
                    EssentiaContainer.fromAspectList(aspects));
        }
    }
}
