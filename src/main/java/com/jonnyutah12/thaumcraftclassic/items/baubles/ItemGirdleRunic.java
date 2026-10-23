package com.jonnyutah12.thaumcraftclassic.items.baubles;

import java.util.Set;

import com.jonnyutah12.thaumcraftclassic.accessory.AccessorySlot;
import com.jonnyutah12.thaumcraftclassic.api.IRunicArmor;
import com.jonnyutah12.thaumcraftclassic.runic.RunicShielding;
import com.jonnyutah12.thaumcraftclassic.runic.RunicUpgradeFlags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Runic girdle (TC-M8-02): port of
 * {@code thaumcraft.common.items.baubles.ItemGirdleRunic}
 * ({@code common/items/baubles/ItemGirdleRunic.java}).
 *
 * <p>Original metas preserved: 0 = Girdle of the Vis (charge 10),
 * 1 = Girdle of the Vis (Kinetic) (charge 9, no-damage push burst when the
 * shield depletes). Charge table = the original's {@code getRunicCharge}
 * ternary verbatim (line 60). Equipping/unequipping marks the shield pool
 * dirty (lines 72-78).</p>
 */
public class ItemGirdleRunic extends MetaBaubleItem implements IRunicArmor, RunicUpgradeFlags {

    public static final int META_VIS = 0;
    public static final int META_KINETIC = 1;
    public static final int META_COUNT = 2;

    public ItemGirdleRunic(final Properties props) {
        super(props);
    }

    public ItemStack stack(final int meta) {
        return metaStack(this, meta);
    }

    /** The original's per-meta charge table (ItemGirdleRunic.java:60). */
    public static int chargeForMeta(final int meta) {
        return meta == META_VIS ? 10 : 9;
    }

    @Override
    public Set<AccessorySlot> applicableSlots(final ItemStack stack) {
        return Set.of(AccessorySlot.BELT);
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
        return storedMeta(stack) == META_KINETIC;
    }

    @Override
    public boolean healing(final ItemStack stack) {
        return false;
    }

    @Override
    public boolean emergency(final ItemStack stack) {
        return false;
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
