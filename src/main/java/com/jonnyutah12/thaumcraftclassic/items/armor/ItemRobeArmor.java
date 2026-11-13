package com.jonnyutah12.thaumcraftclassic.items.armor;

import com.jonnyutah12.thaumcraftclassic.api.IRunicArmor;
import com.jonnyutah12.thaumcraftclassic.api.IVisDiscountGear;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import java.util.List;
import net.minecraft.core.Holder;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.ChatFormatting;

/**
 * Thaumaturge's robe piece (TC-M6-07-s): port of
 * {@code thaumcraft.common.items.armor.ItemRobeArmor}
 * ({@code common/items/armor/ItemRobeArmor.java}), registered for chest, legs
 * and boots — the original's 1.7.10 armourType slots 1, 2 and 3
 * ({@code ConfigItems.java:419-423}: {@code (armorMatSpecial, 1, 1)} chest,
 * {@code (armorMatSpecial, 2, 2)} legs, {@code (armorMatSpecial, 1, 3)} boots; the
 * second argument is the render index, the third the slot). There is
 * no robe helmet — the original never registered one, and the id contract
 * (docs/content-mapping.csv) has no such row.
 *
 * <p>Signature effect: the robe is the original's vis-discount cloth armour.
 * {@code getVisDiscount} (lines 128-129) returns
 * {@code armorType == 3 ? 1 : 2} — boots 1, chest and legs 2 — and
 * {@code wands/WornGearDiscounts} (the port of {@code WandManager.
 * getTotalVisDiscount}) sums it over the four armour slots, so wearing the
 * full robe takes 5 percent off every wand cast. The tooltip line
 * {@code tc.visdiscount} (lines 132-133) is the original's own.</p>
 *
 * <p>The rarity is the {@code minecraft:rarity} component (1.21.1 has no
 * {@code Item.getRarity(ItemStack)} to override): {@code CatalogArmor} passes
 * {@code Rarity.UNCOMMON}, the original's {@code EnumRarity.uncommon}
 * ({@code ItemRobeArmor.java:119-120}).</p>
 *
 * <p>Dyeing: the robe is the one armour family with a dyeable layer, so the
 * colour travels on the stack. 1.7.10 stored a 24-bit RGB int in the
 * {@code display.color} NBT tag; 1.21.1's {@code minecraft:base_color} is a
 * {@code DyeColor} (16 values) and cannot hold a blended colour, so the port
 * carries the int on its own {@code thaumcraft:armor_color} component, read and
 * written by {@link #getColor}/{@link #setColor}/{@link #clearColor} — the port
 * of the original's {@code getColor}, {@code setColor} and {@code clearColor}
 * (lines 74-107), default 6961280.</p>
 *
 * <p>Deferred with its card: the cauldron scrub that strips the colour
 * ({@code onItemUseFirst}, lines 136-143) and {@code RecipesRobeArmorDyes}
 * (line 1 of that file) are the dye-mechanic rows of TC-M6-07b; the
 * robe model/overlay textures are TC-M7-01.</p>
 */
public class ItemRobeArmor extends ArmorItem implements IRunicArmor, IVisDiscountGear {

    public ItemRobeArmor(final Holder<ArmorMaterial> material, final Type type,
                         final Properties properties) {
        super(material, type, properties);
    }

    /** Original lines 40-41: {@code return 0}. */
    @Override
    public int getRunicCharge(final ItemStack stack) {
        return ArmorSetRules.RUNIC_CHARGE_UNUPGRADED;
    }

    /**
     * Original lines 128-129: boots 1 percent, chest and legs 2. The aspect
     * argument is unused in the original (it discounts every aspect), and the
     * tooltip call sites pass {@code null} for it, per the interface contract.
     */
    @Override
    public int getVisDiscount(final ItemStack stack, final Player player, final Aspect aspect) {
        return ArmorSetRules.robeVisDiscount(this.type);
    }

    /** Original lines 123-124: anvil repair is enchanted fabric ({@code itemResource} meta 7). */
    @Override
    public boolean isValidRepairItem(final ItemStack toRepair, final ItemStack repair) {
        return repair.is(CatalogResources.ITEM_RESOURCE.get())
                && ItemResource.storedMeta(repair) == ArmorSetRules.REPAIR_META_ENCHANTED_FABRIC;
    }

    /** The material-level repair rule, item-only (the meta check lives above). */
    public static Ingredient repairIngredient() {
        return Ingredient.of(ItemResource.stack(ArmorSetRules.REPAIR_META_ENCHANTED_FABRIC));
    }

    /**
     * Original {@code func_82814_b} = {@code getColor} (lines 74-82): the stack's {@code display.color}
     * int, default 6961280. The port carries the int on
     * {@code thaumcraft:armor_color} (see {@code ModDataComponents.ARMOR_COLOR}
     * for why 1.21.1's {@code base_color} cannot hold it).
     */
    public int getColor(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ARMOR_COLOR.get(),
                Integer.valueOf(ArmorSetRules.ROBE_DEFAULT_COLOR)).intValue();
    }

    /** Original {@code func_82813_b} = {@code setColor} (lines 94-107), the dye recipe's write. */
    public void setColor(final ItemStack stack, final int color) {
        stack.set(ModDataComponents.ARMOR_COLOR.get(), Integer.valueOf(color));
    }

    /** Original {@code func_82815_c} = {@code clearColor} (lines 84-92), the cauldron scrub's write. */
    public void clearColor(final ItemStack stack) {
        stack.remove(ModDataComponents.ARMOR_COLOR.get());
    }

    /**
     * Original {@code func_77624_a} = {@code addInformation} (lines 132-133):
     * {@code DARK_PURPLE + tc.visdiscount + ": " + getVisDiscount(...) + "%"}.
     * The original passes {@code null} for the player and the aspect.
     */
    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        tooltip.add(Component.translatable("tc.visdiscount")
                .append(": " + getVisDiscount(stack, null, null) + "%")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
