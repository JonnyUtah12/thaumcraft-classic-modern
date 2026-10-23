package com.jonnyutah12.thaumcraftclassic.items.baubles;

import java.util.Set;

import com.jonnyutah12.thaumcraftclassic.accessory.AccessorySlot;
import com.jonnyutah12.thaumcraftclassic.api.IRunicArmor;
import com.jonnyutah12.thaumcraftclassic.runic.RunicShielding;
import com.jonnyutah12.thaumcraftclassic.runic.RunicUpgradeFlags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Runic amulet (TC-M8-02): port of
 * {@code thaumcraft.common.items.baubles.ItemAmuletRunic}
 * ({@code common/items/baubles/ItemAmuletRunic.java}).
 *
 * <p>Original metas preserved: 0 = Amulet of the Avatar (charge 8),
 * 1 = Amulet of the Avatar (Emergency) (charge 7, refills the shield to
 * {@code min(max, 8)} when depleted). Charge table = the original's
 * {@code getRunicCharge} ternary verbatim (line 55). Equipping/unequipping
 * marks the shield pool dirty (lines 67-73).</p>
 */
public class ItemAmuletRunic extends MetaBaubleItem implements IRunicArmor, RunicUpgradeFlags {

    public static final int META_AVATAR = 0;
    public static final int META_EMERGENCY = 1;
    public static final int META_COUNT = 2;

    public ItemAmuletRunic(final Properties props) {
        super(props);
    }

    public ItemStack stack(final int meta) {
        return metaStack(this, meta);
    }

    /** The original's per-meta charge table (ItemAmuletRunic.java:55). */
    public static int chargeForMeta(final int meta) {
        return meta == META_AVATAR ? 8 : 7;
    }

    @Override
    public Set<AccessorySlot> applicableSlots(final ItemStack stack) {
        return Set.of(AccessorySlot.AMULET);
    }

    @Override
    public int getRunicCharge(final ItemStack stack) {
        return chargeForMeta(storedMeta(stack));
    }

    @Override
    public boolean charged(final ItemStack stack) {
        return false;
    }

    @Override
    public boolean kinetic(final ItemStack stack) {
        return false;
    }

    @Override
    public boolean healing(final ItemStack stack) {
        return false;
    }

    @Override
    public boolean emergency(final ItemStack stack) {
        return storedMeta(stack) == META_EMERGENCY;
    }

    @Override
    public void onEquipped(final ItemStack stack, final Player player) {
        RunicShielding.markDirty();
    }

    @Override
    public void onUnequipped(final ItemStack stack, final Player player) {
        RunicShielding.markDirty();
    }

    @Override
    public net.minecraft.network.chat.Component getName(final ItemStack stack) {
        return perMetaName(stack);
    }
}
