package com.jonnyutah12.thaumcraftclassic.items.armor;

import com.jonnyutah12.thaumcraftclassic.api.IRunicArmor;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * Crimson praetor (cult leader) plate armor (content-mapping.csv rows
 * 291-293, {@code thaumcraft:item{helmet,chestplate,leggings}cultistleaderplate}).
 *
 * <p>Original: {@code thaumcraft.common.items.armor.ItemCultistLeaderArmor}
 * ({@code common/items/armor/ItemCultistLeaderArmor.java}) — registered
 * with {@code ThaumcraftApi.armorMatThaumiumFortress}
 * ({@code ConfigItems.java:535-539}), rarity rare, no worn effects beyond
 * the armor stats (repair follows the material: empty until the thaumium
 * ingot lands with TC-M6-04a).</p>
 */
public class ItemCultistLeaderArmor extends ArmorItem implements IRunicArmor {

    public ItemCultistLeaderArmor(final Holder<ArmorMaterial> material, final Type type) {
        super(material, type, new ItemProperties()
                .durability(type.getDurability(ItemFortressArmor.DURABILITY_MULTIPLIER))
                .rarity(Rarity.RARE));
    }

    @Override
    public int getRunicCharge(final ItemStack stack) {
        return 0;
    }
}
