package com.jonnyutah12.thaumcraftclassic.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Port of {@code thaumcraft.api.IWarpingGear} (Azanor/thaumcraft-api,
 * branch 1.7.10 @ ced30ed, MIT; adapted per
 * docs/licensing-and-attribution.md). Original: {@code api/IWarpingGear.java}
 * — one method, {@code getWarp}.
 *
 * <p>Worn gear that adds to the wearer's warp for the warp-event roll.
 * {@code WarpEvents.getWarpFromGear} ({@code common/lib/WarpEvents.java:349-361})
 * summed {@code EventHandlerRunic.getFinalWarp} over the player's elytra slot,
 * the four armour slots and the four baubles slots, and that helper is exactly
 * "the stack's item implements {@code IWarpingGear}, ask it"
 * ({@code common/lib/events/EventHandlerRunic.java}, {@code getFinalWarp}).
 * {@code warp/WarpEvents#gearWarp} is this port's sum, over the armour slots
 * and {@code accessory/AccessoryService#wornStacks}.</p>
 *
 * <p>The interface lands with TC-M6-05 because the warp roll reads it; the
 * implementors are later cards — the runic armours (TC-M6-07) and the fortress
 * set (TC-M8) carry the original's per-meta warp values. Until one lands the
 * sum is 0, which is the shipped state, not a stub: the roll runs on the
 * player's own warp exactly as the original's does for an unwired player.</p>
 */
public interface IWarpingGear {

    /**
     * Original {@code getWarp(ItemStack, EntityPlayer)}: warp this stack adds
     * to its wearer's warp total for the warp-event roll.
     */
    int getWarp(ItemStack stack, Player player);
}
