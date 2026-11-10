package com.jonnyutah12.thaumcraftclassic.items.armor;

import com.jonnyutah12.thaumcraftclassic.api.IRunicArmor;
import com.jonnyutah12.thaumcraftclassic.api.IVisDiscountGear;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.DyedItemColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Thaumaturge's robe armor (content-mapping.csv rows 236-238,
 * {@code thaumcraft:item{chestplate,leggings,boots}robe} — the original
 * has no robe helm, {@code common/config/ConfigItems.java:419-423}).
 *
 * <p>Original: {@code thaumcraft.common.items.armor.ItemRobeArmor}
 * ({@code common/items/armor/ItemRobeArmor.java}) —
 * {@code ThaumcraftApi.armorMatSpecial}, rarity uncommon,
 * {@code IVisDiscountGear}: 2% wand-cost discount, 1% for the boots
 * (line 128). Dyeable: the original implemented {@code IDyeableArmorItem}
 * with a cauldron-dye {@code onItemUseFirst} path; 1.21.1 replaced that
 * with the {@code #minecraft:dyeable} item tag + {@code dyed_item_color}
 * component and the vanilla ArmorDyeRecipe (docs/porting-decisions.md; the
 * robe armor textures ship in the ARR overlay, D-004). Default color
 * 6961280 is the original's {@code func_82814_b} fallback.</p>
 */
public class ItemRobeArmor extends ArmorItem implements IRunicArmor, IVisDiscountGear {

    /** {@code ThaumcraftApi.armorMatSpecial} durability multiplier
     *  ({@code api/ThaumcraftApi.java:38}). */
    public static final int DURABILITY_MULTIPLIER = 25;

    /** Original default robe color ({@code func_82814_b} fallback, 6961280). */
    public static final int DEFAULT_COLOR = 0x6A3880;

    public ItemRobeArmor(final Holder<ArmorMaterial> material, final Type type) {
        super(material, type, new ItemProperties()
                .durability(type.getDurability(DURABILITY_MULTIPLIER))
                .rarity(Rarity.UNCOMMON)
                .component(DataComponents.DYED_COLOR, new DyedItemColor(DEFAULT_COLOR)));
    }

    @Override
    public int getRunicCharge(final ItemStack stack) {
        return 0;
    }

    @Override
    public int getVisDiscount(final ItemStack stack, final Player player, final Aspect aspect) {
        // Original: boots (armorType 3) discount 1%, every other piece 2%
        // (ItemRobeArmor.java:128).
        return type() == Type.BOOTS ? 1 : 2;
    }

    @Override
    protected void appendHoverText(final ItemStack stack, final TooltipContext context,
                                  final List<Component> tooltip, final TooltipFlag flag) {
        // Original addInformation (line 133): flat "all aspects" discount.
        tooltip.add(Component.translatable("tc.visdiscount")
                .append(Component.literal(": " + getVisDiscount(stack, null, null) + "%"))
                .withStyle(ChatFormatting.DARK_PURPLE));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
