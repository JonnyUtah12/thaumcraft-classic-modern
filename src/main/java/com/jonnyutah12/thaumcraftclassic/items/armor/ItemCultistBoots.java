package com.jonnyutah12.thaumcraftclassic.items.armor;

import com.jonnyutah12.thaumcraftclassic.api.IRunicArmor;
import com.jonnyutah12.thaumcraftclassic.api.IVisDiscountGear;
import com.jonnyutah12.thaumcraftclassic.api.IWarpingGear;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Crimson cult boots (content-mapping.csv row 287,
 * {@code thaumcraft:itembootscultist}).
 *
 * <p>Original: {@code thaumcraft.common.items.armor.ItemCultistBoots}
 * ({@code common/items/armor/ItemCultistBoots.java}) — vanilla
 * {@code ArmorMaterial.IRON} ({@code ConfigItems.java:527}), rarity
 * uncommon, {@code IWarpingGear} +1 and {@code IVisDiscountGear} 1% while
 * worn, vanilla iron-ingot repair through the material.</p>
 */
public class ItemCultistBoots extends ArmorItem implements IRunicArmor, IWarpingGear,
        IVisDiscountGear {

    public ItemCultistBoots(final Holder<ArmorMaterial> material, final Type type) {
        super(material, type, new ItemProperties()
                .durability(type.getDurability(ItemCultistRobeArmor.IRON_MULT))
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
    public int getVisDiscount(final ItemStack stack, final Player player, final Aspect aspect) {
        return 1;
    }

    @Override
    protected void appendHoverText(final ItemStack stack, final TooltipContext context,
                                  final List<Component> tooltip, final TooltipFlag flag) {
        tooltip.add(Component.translatable("tc.visdiscount")
                .append(Component.literal(": " + getVisDiscount(stack, null, null) + "%"))
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
