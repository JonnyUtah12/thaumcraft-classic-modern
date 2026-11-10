package com.jonnyutah12.thaumcraftclassic.items.armor;

import com.jonnyutah12.thaumcraftclassic.api.IRunicArmor;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * Crimson cult plate armor (content-mapping.csv rows 288-290,
 * {@code thaumcraft:item{helmet,chestplate,leggings}cultistplate}).
 *
 * <p>Original: {@code thaumcraft.common.items.armor.ItemCultistPlateArmor}
 * ({@code common/items/armor/ItemCultistPlateArmor.java}) — vanilla
 * {@code ArmorMaterial.IRON} ({@code ConfigItems.java:529-533}), rarity
 * uncommon, vanilla iron-ingot repair, no worn effects beyond the armor
 * stats.</p>
 */
public class ItemCultistPlateArmor extends ArmorItem implements IRunicArmor {

    public ItemCultistPlateArmor(final Holder<ArmorMaterial> material, final Type type) {
        super(material, type, new ItemProperties()
                .durability(type.getDurability(ItemCultistRobeArmor.IRON_MULT))
                .rarity(Rarity.UNCOMMON));
    }

    @Override
    public int getRunicCharge(final ItemStack stack) {
        return 0;
    }
}
