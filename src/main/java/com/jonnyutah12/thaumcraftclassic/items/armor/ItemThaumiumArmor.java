package com.jonnyutah12.thaumcraftclassic.items.armor;

import com.jonnyutah12.thaumcraftclassic.api.IRunicArmor;
import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

/**
 * Thaumium armour piece (TC-M6-07-s): port of
 * {@code thaumcraft.common.items.armor.ItemThaumiumArmor}
 * ({@code common/items/armor/ItemThaumiumArmor.java}), registered four times
 * for the four slots ({@code ConfigItems.java:375-381},
 * {@code new ItemThaumiumArmor(ThaumcraftApi.armorMatThaumium, 2, slot)} for slots
 * 0..3). The original's class also implements the {@code IRepairable} marker;
 * that interface has no port counterpart (no shipped class implements it), so
 * the anvil rule below is the whole repair contract, as it is for the baubles.
 *
  * <p>1.21.1 has no {@code Item.getRarity(ItemStack)} to override: the rarity is the
 * {@code minecraft:rarity} data component, set from {@code Item.Properties}
 * ({@code CatalogArmor} passes {@code Rarity.UNCOMMON} = the original's
 * {@code EnumRarity.uncommon}, {@code ItemThaumiumArmor.java:58-59}).
 *
 * <p>Original behaviour carried here: {@code getRunicCharge} returns 0
 * (lines 29-30), and
 * anvil repair is the thaumium ingot ({@code itemResource} meta 2, lines 62-63).
 * The class is <em>not</em> {@code IVisDiscountGear}: thaumium armour never
 * discounted vis in the original, so it contributes 0 to
 * {@code wands/WornGearDiscounts}.</p>
 *
 * <p>Deferred with their cards: the per-piece armour model/texture
 * ({@code textures/models/thaumium_1|2.png} → {@code thaumium_layer_1|2.png},
 * TC-M7-01) and the runic upgrade flags, which the fortress/void families
 * carry (TC-M6-07b).</p>
 */
public class ItemThaumiumArmor extends ArmorItem implements IRunicArmor {

    public ItemThaumiumArmor(final Holder<ArmorMaterial> material, final Type type,
                             final Properties properties) {
        super(material, type, properties);
    }

    /** Original lines 29-30: {@code return 0} — un-upgraded armour shields nothing. */
    @Override
    public int getRunicCharge(final ItemStack stack) {
        return ArmorSetRules.RUNIC_CHARGE_UNUPGRADED;
    }

    /**
     * Original lines 62-63: repairable with {@code itemResource} meta 2 (the
     * thaumium ingot). The meta-precise test replaces {@code super}, whose
     * material-level {@code repairIngredient} is item-only in 1.21.1 and would
     * accept any {@code itemResource} meta.
     */
    @Override
    public boolean isValidRepairItem(final ItemStack toRepair, final ItemStack repair) {
        return repair.is(CatalogResources.ITEM_RESOURCE.get())
                && ItemResource.storedMeta(repair) == ArmorSetRules.REPAIR_META_THAUMIUM_INGOT;
    }
}
