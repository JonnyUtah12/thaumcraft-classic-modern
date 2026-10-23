package com.jonnyutah12.thaumcraftclassic.api;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Port of {@code thaumcraft.api.IVisDiscountGear} (Azanor/thaumcraft-api,
 * branch 1.7.10 @ ced30ed, MIT; adapted per
 * docs/licensing-and-attribution.md). Original: {@code api/IVisDiscountGear.java}.
 *
 * <p>Worn gear that lowers the vis cost of casting one aspect, in whole
 * percent. The original summed these over armor slots and the four baubles
 * slots in {@code WandManager.getTotalVisDiscount} ({@code common/items/wands/
 * WandManager.java:50-84}); the port's aggregation lives in
 * {@code wands/WornGearDiscounts} and feeds {@code VisCostRules.
 * VisDiscountSource} (docs/architecture/vis.md).</p>
 */
public interface IVisDiscountGear {

    /**
     * Original {@code getVisDiscount(ItemStack, EntityPlayer, Aspect)}:
     * discount percent for one aspect (0 when the gear does not discount it).
     * The original passed {@code null} for the aspect at tooltip sites to get
     * the flat "all aspects" discount; keep that call shape.
     */
    int getVisDiscount(ItemStack stack, Player player, Aspect aspect);
}
