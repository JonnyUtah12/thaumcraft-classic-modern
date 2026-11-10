package com.jonnyutah12.thaumcraftclassic.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * An {@link IRepairable} item that mends itself with custom logic when the
 * Repair enchantment fires (TC-M6-05).
 *
 * <p>Original: {@code thaumcraft.api.IRepairableExtended}. When
 * {@link #doRepair(ItemStack, Player, int)} returns true the vis cost is
 * then paid and the stack's damage is reduced by the enchantment level,
 * exactly as in the original {@code EventHandlerEntity.doRepair}.</p>
 */
public interface IRepairableExtended extends IRepairable {

    boolean doRepair(ItemStack stack, Player player, int level);
}
