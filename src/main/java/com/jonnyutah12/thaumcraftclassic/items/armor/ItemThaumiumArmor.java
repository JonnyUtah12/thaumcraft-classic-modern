package com.jonnyutah12.thaumcraftclassic.items.armor;

import com.jonnyutah12.thaumcraftclassic.api.IRunicArmor;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * Thaumium armor (content-mapping.csv rows 214-217,
 * {@code thaumcraft:item{helmet,chestplate,leggings,boots}thaumium}).
 *
 * <p>Original: {@code thaumcraft.common.items.armor.ItemThaumiumArmor}
 * ({@code common/items/armor/ItemThaumiumArmor.java}) — plain
 * {@code ThaumcraftApi.armorMatThaumium} armor, {@code IRepairable}
 * (thaumium-ingot repair arrives with the itemResource card, TC-M6-04a:
 * the material's repair ingredient is empty until then) and
 * {@code IRunicArmor} returning 0 — kept so the runic engine scans it like
 * the original does. Rarity uncommon (the original's
 * {@code func_77613_e}).</p>
 *
 * <p>Known difference: the original's {@code ISpecialArmor} damage-type
 * ratios and set bonus have no 1.21.1 equivalent hook; the material's
 * defense values are registered as-is and the special-damage pipeline is
 * deferred (docs/feature-tracking.md, TC-M6-07).</p>
 */
public class ItemThaumiumArmor extends ArmorItem implements IRunicArmor {

    /** {@code ThaumcraftApi.armorMatThaumium} durability multiplier
     *  ({@code api/ThaumcraftApi.java:37}); 1.21.1 carries durability on
     *  the item, not the material record. */
    public static final int DURABILITY_MULTIPLIER = 25;

    public ItemThaumiumArmor(final Holder<ArmorMaterial> material, final Type type) {
        super(material, type, new ItemProperties()
                .durability(type.getDurability(DURABILITY_MULTIPLIER))
                .rarity(Rarity.UNCOMMON));
    }

    @Override
    public int getRunicCharge(final ItemStack stack) {
        return 0;
    }
}
