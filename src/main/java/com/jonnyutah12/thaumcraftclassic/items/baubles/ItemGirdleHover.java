package com.jonnyutah12.thaumcraftclassic.items.baubles;

import java.util.Set;

import com.jonnyutah12.thaumcraftclassic.accessory.AccessorySlot;
import com.jonnyutah12.thaumcraftclassic.api.IRunicArmor;
import net.minecraft.world.item.ItemStack;

/**
 * Hover girdle (TC-M8-02): port of
 * {@code thaumcraft.common.items.baubles.ItemGirdleHover}
 * ({@code common/items/baubles/ItemGirdleHover.java}).
 *
 * <p>Single variant (the original registers only meta 0). While worn, it
 * subtracts 0.33 from the wearer's fall distance every tick — the original's
 * {@code onWornTick} (lines 55-58): {@code if (player.fallDistance > 0.0F)
 * player.fallDistance -= 0.33F}. Combined with the hover harness (TC-M6-03
 * armor) this is the original's soft-landing stack; the harness itself is
 * armor work, not bauble work.</p>
 *
 * <p>{@code getRunicCharge} = 0, as in the original (line 70).</p>
 */
public class ItemGirdleHover extends MetaBaubleItem implements IRunicArmor {

    /** The original's per-tick fall-distance reduction (line 57). */
    public static final float FALL_REDUCTION = 0.33F;

    public ItemGirdleHover(final Properties props) {
        super(props);
    }

    public ItemStack stack(final int meta) {
        return metaStack(this, meta);
    }

    /** Pure fall-distance step, unit-testable: only reduces a positive value. */
    public static float nextFallDistance(final float fallDistance) {
        return fallDistance > 0.0F ? fallDistance - FALL_REDUCTION : fallDistance;
    }

    @Override
    public Set<AccessorySlot> applicableSlots(final ItemStack stack) {
        return Set.of(AccessorySlot.BELT);
    }

    @Override
    public void onWornTick(final ItemStack stack, final net.minecraft.world.entity.player.Player wearer) {
        wearer.fallDistance = nextFallDistance(wearer.fallDistance);
    }

    @Override
    public int getRunicCharge(final ItemStack stack) {
        return 0;
    }
}
