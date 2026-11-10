package com.jonnyutah12.thaumcraftclassic.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * TC-M6-07: port of {@code thaumcraft/api/IWarpingGear.java} (Azanor/
 * thaumcraft-api, branch 1.7.10 @ ced30ed, MIT; adapted per
 * docs/licensing-and-attribution.md; {@code EntityPlayer} → {@code Player}
 * per the port's accessory-layer convention). Worn pieces that add
 * temporary warp while equipped: the void set +1 per piece, the
 * void-fortress set +2, the crimson cult set +1 (original {@code getWarp}
 * bodies, {@code common/items/armor/ItemVoidArmor.java:47},
 * {@code ItemVoidRobeArmor.java:66}, {@code ItemCultistRobeArmor.java:59}).
 *
 * <p>TC4 parity: gear warp is while-worn only — there is no sticky-warp
 * persistence on equip. The sum over a player's armor and bauble slots is
 * computed by the warp-event card's own {@code getWarpFromGear} port
 * (TC-M6-05, {@code common/lib/WarpEvents.java:353-365}); the tooltip line
 * ("item.warping", dark purple) is emitted by
 * {@code runic/RunicShielding#onItemTooltip} (EventHandlerRunic
 * {@code tooltipEvent} parity).</p>
 */
public interface IWarpingGear {

    /** Original {@code getWarp(ItemStack, EntityPlayer)}: temporary warp while worn. */
    int getWarp(ItemStack stack, Player player);
}
