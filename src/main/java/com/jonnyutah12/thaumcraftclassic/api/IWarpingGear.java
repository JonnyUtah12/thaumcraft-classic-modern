package com.jonnyutah12.thaumcraftclassic.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Port of {@code thaumcraft.api.IWarpingGear} (Azanor/thaumcraft-api,
 * branch 1.7.10 @ ced30ed, MIT; adapted per
 * docs/licensing-and-attribution.md). Original: {@code api/IWarpingGear.java}.
 *
 * <p>Worn gear that adds to the wearer's warp total for the warp events:
 * the original summed {@code getWarp} over the current main-hand stack, the
 * four armor slots and the four baubles slots in
 * {@code WarpEvents.getWarpFromGear} ({@code common/lib/WarpEvents.java:353-367}
 * via {@code EventHandlerRunic.getFinalWarp}, {@code :356-363}); the port's
 * aggregation lives in {@code player/WarpEvents}. No shipped item implements
 * it yet — TC-M6-07 runic gear and the foci join through this contract.</p>
 */
public interface IWarpingGear {

    /**
     * Original {@code getWarp(ItemStack, EntityPlayer)}: warp points this
     * worn stack adds to the wearer's warp total (0 when it adds none).
     */
    int getWarp(ItemStack stack, Player player);
}
