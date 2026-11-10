package com.jonnyutah12.thaumcraftclassic.items.armor;

import com.jonnyutah12.thaumcraftclassic.api.IRunicArmor;
import com.jonnyutah12.thaumcraftclassic.api.IWarpingGear;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

/**
 * Void armor (content-mapping.csv rows 269-272,
 * {@code thaumcraft:item{helmet,chestplate,leggings,boots}void}).
 *
 * <p>Original: {@code thaumcraft.common.items.armor.ItemVoidArmor}
 * ({@code common/items/armor/ItemVoidArmor.java}) —
 * {@code ThaumcraftApi.armorMatVoid}, rarity uncommon, {@code IWarpingGear}
 * +1 while worn (line 47), and self-repair: one durability point every 20
 * ticks of the wearer while equipped ({@code onArmorTick}) or carried in the
 * main inventory ({@code func_77951_h} update path). The worn-slot half
 * runs through {@link WornArmorTick}; the inventory half through
 * {@code inventoryTick} (same 20-tick cadence and guard as the original).</p>
 */
public class ItemVoidArmor extends ArmorItem implements IRunicArmor, IWarpingGear, WornArmorTick {

    /** {@code ThaumcraftApi.armorMatVoid} durability multiplier
     *  ({@code api/ThaumcraftApi.java:40}). */
    public static final int DURABILITY_MULTIPLIER = 10;

    public ItemVoidArmor(final Holder<ArmorMaterial> material, final Type type) {
        super(material, type, new ItemProperties()
                .durability(type.getDurability(DURABILITY_MULTIPLIER))
                .rarity(Rarity.UNCOMMON));
    }

    @Override
    public int getRunicCharge(final ItemStack stack) {
        return 0;
    }

    @Override
    public int getWarp(final ItemStack stack, final Player player) {
        return 1;
    }

    @Override
    public void onWornTick(final ItemStack stack, final Player wearer) {
        selfRepair(stack, wearer);
    }

    @Override
    public void inventoryTick(final ItemStack stack, final Level level, final Entity entity,
                              final int slotId, final boolean isSelected) {
        if (!level.isClientSide && stack.isDamageableItem() && entity instanceof LivingEntity living) {
            selfRepair(stack, living);
        }
    }

    /** TC4 parity: {@code damageItem(-1)} every 20 ticks of the wearer. */
    static void selfRepair(final ItemStack stack, final LivingEntity wearer) {
        if (wearer.tickCount % 20 == 0 && stack.isDamaged()) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }
}
