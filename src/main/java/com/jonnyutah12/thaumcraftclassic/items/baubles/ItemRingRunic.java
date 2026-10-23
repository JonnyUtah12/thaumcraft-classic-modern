package com.jonnyutah12.thaumcraftclassic.items.baubles;

import java.util.Set;

import com.jonnyutah12.thaumcraftclassic.accessory.AccessorySlot;
import com.jonnyutah12.thaumcraftclassic.api.IRunicArmor;
import com.jonnyutah12.thaumcraftclassic.runic.RunicShielding;
import com.jonnyutah12.thaumcraftclassic.runic.RunicUpgradeFlags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * Runic ring (TC-M8-02): port of
 * {@code thaumcraft.common.items.baubles.ItemRingRunic}
 * ({@code common/items/baubles/ItemRingRunic.java}).
 *
 * <p>Original metas preserved: 0 = Ring of Protection (charge 1),
 * 1 = Greater Ring of Protection (charge 5), 2 = Charged Ring (charge 4,
 * shortens the shield recharge by 500 ms), 3 = Revitalizing Ring (charge 4,
 * Regeneration on depletion). The charge table is the original's
 * {@code getRunicCharge} ternary verbatim (line 76) — note meta 1 charges
 * more than 2/3, as in the original. The base capacity the original passed to
 * {@code ItemRunic(5)} is dead there (overridden); the per-meta table is the
 * live rule.</p>
 *
 * <p>Equipping/unequipping marks the shield pool dirty, exactly like the
 * original's {@code isDirty = true} (lines 66-72).</p>
 */
public class ItemRingRunic extends MetaBaubleItem implements IRunicArmor, RunicUpgradeFlags {

    public static final int META_PROTECTION = 0;
    public static final int META_GREATER = 1;
    public static final int META_CHARGED = 2;
    public static final int META_REVITALIZING = 3;
    public static final int META_COUNT = 4;

    public ItemRingRunic(final Properties props) {
        super(props);
    }

    public ItemStack stack(final int meta) {
        return metaStack(this, meta);
    }

    /** The original's per-meta charge table (ItemRingRunic.java:76). */
    public static int chargeForMeta(final int meta) {
        return meta == META_PROTECTION ? 1 : (meta == META_GREATER ? 5 : 4);
    }

    @Override
    public Set<AccessorySlot> applicableSlots(final ItemStack stack) {
        return Set.of(AccessorySlot.RING);
    }

    @Override
    public int getRunicCharge(final ItemStack stack) {
        return chargeForMeta(storedMeta(stack));
    }

    @Override
    public boolean charged(final ItemStack stack) {
        return storedMeta(stack) == META_CHARGED;
    }

    @Override
    public boolean kinetic(final ItemStack stack) {
        return false;
    }

    @Override
    public boolean healing(final ItemStack stack) {
        return storedMeta(stack) == META_REVITALIZING;
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

    @Override
    protected Rarity rarityForMeta(final int meta) {
        // Original: uncommon for meta 0 (the registered default), rare for the
        // upgrades (ItemRingRunic.java:33).
        return meta == META_PROTECTION ? null : Rarity.RARE;
    }
}
