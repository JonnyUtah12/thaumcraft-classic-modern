package com.jonnyutah12.thaumcraftclassic.items.armor;

import com.jonnyutah12.thaumcraftclassic.api.IRunicArmor;
import com.jonnyutah12.thaumcraftclassic.api.IVisDiscountGear;
import com.jonnyutah12.thaumcraftclassic.api.IWarpingGear;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Void-fortress / void thaumaturge armor (content-mapping.csv rows
 * 278-280, {@code thaumcraft:item{helmet,chestplate,leggings}voidfortress}
 * — no boots, {@code common/config/ConfigItems.java:509-513}).
 *
 * <p>Original: {@code thaumcraft.common.items.armor.ItemVoidRobeArmor}
 * ({@code common/items/armor/ItemVoidRobeArmor.java}) — registered with
 * {@code ThaumcraftApi.armorMatVoid} (the original defines
 * {@code armorMatVoidFortress} at {@code api/ThaumcraftApi.java:41} but
 * never uses it; the void-fortress pieces use the plain void material,
 * pinned here), rarity epic. {@code IVisDiscountGear} 5% (line 71
 * tooltip), {@code IWarpingGear} +2 (line 66), self-repair every 20 ticks
 * while worn or carried (same seam as {@link ItemVoidArmor}), and the helm
 * reveals nodes ({@code IRevealer.showNodes} = true on armorType 0 —
 * registered through nodes/NodeReveal by the M607 catalog holder).</p>
 *
 * <p>Known difference: the original's {@code ISpecialArmor} ratios are not
 * ported (see {@link ItemFortressArmor}).</p>
 */
public class ItemVoidRobeArmor extends ArmorItem implements IRunicArmor, IVisDiscountGear,
        IWarpingGear, WornArmorTick {

    public ItemVoidRobeArmor(final Holder<ArmorMaterial> material, final Type type) {
        super(material, type, new ItemProperties()
                .durability(type.getDurability(ItemVoidArmor.DURABILITY_MULTIPLIER))
                .rarity(Rarity.EPIC));
    }

    @Override
    public int getRunicCharge(final ItemStack stack) {
        return 0;
    }

    @Override
    public int getVisDiscount(final ItemStack stack, final Player player, final Aspect aspect) {
        return 5;
    }

    @Override
    public int getWarp(final ItemStack stack, final Player player) {
        return 2;
    }

    @Override
    public void onWornTick(final ItemStack stack, final Player wearer) {
        ItemVoidArmor.selfRepair(stack, wearer);
    }

    @Override
    public void inventoryTick(final ItemStack stack, final Level level, final Entity entity,
                              final int slotId, final boolean isSelected) {
        if (!level.isClientSide && stack.isDamageableItem() && entity instanceof LivingEntity living) {
            ItemVoidArmor.selfRepair(stack, living);
        }
    }

    @Override
    protected void appendHoverText(final ItemStack stack, final TooltipContext context,
                                  final List<Component> tooltip, final TooltipFlag flag) {
        tooltip.add(Component.translatable("tc.visdiscount")
                .append(Component.literal(": " + getVisDiscount(stack, null, null) + "%"))
                .withStyle(ChatFormatting.DARK_PURPLE));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
