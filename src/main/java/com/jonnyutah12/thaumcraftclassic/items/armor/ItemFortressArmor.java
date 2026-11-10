package com.jonnyutah12.thaumcraftclassic.items.armor;

import com.jonnyutah12.thaumcraftclassic.api.IRunicArmor;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Thaumium fortress armor (content-mapping.csv rows 265-267,
 * {@code thaumcraft:item{helmet,chestplate,leggings}fortress} — the
 * original has no fortress boots, {@code common/config/ConfigItems.java:483-487}).
 *
 * <p>Original: {@code thaumcraft.common.items.armor.ItemFortressArmor}
 * ({@code common/items/armor/ItemFortressArmor.java}) —
 * {@code ThaumcraftApi.armorMatThaumiumFortress}, rarity rare. The helm
 * carries the goggle attachment ({@code thaumcraft:goggles} component,
 * original NBT boolean "goggles", set by the HELMGOGGLES infusion recipe)
 * and a mask variant ({@code thaumcraft:mask} int 0-2, original NBT
 * "mask", set by the MASK1/MASK2 recipes); both render as the original's
 * tooltip lines (lines 105-113). Goggles reveal nodes through
 * nodes/NodeReveal ({@code IRevealer.showNodes} = hasGoggles, line 172,
 * registered by the M607 catalog holder); mask 1 (angry ghost: wither the
 * attacker) and mask 2 (sipping fiend: leech) run through
 * runic/RunicShielding (EventHandlerRunic.entityHurt parity); mask 0
 * (grinning devil: warp-event mitigation) is consumed by the warp-event
 * card (TC-M6-05).</p>
 *
 * <p>Known difference: the original's {@code ISpecialArmor} damage-type
 * ratios (the "fortress" damage reduction) and set bonus have no 1.21.1
 * equivalent hook; the material's defense values are registered as-is and
 * the special-damage pipeline is deferred (docs/feature-tracking.md,
 * TC-M6-07).</p>
 */
public class ItemFortressArmor extends ArmorItem implements IRunicArmor {

    /** {@code ThaumcraftApi.armorMatThaumiumFortress} durability multiplier
     *  ({@code api/ThaumcraftApi.java:39}). */
    public static final int DURABILITY_MULTIPLIER = 40;

    public ItemFortressArmor(final Holder<ArmorMaterial> material, final Type type) {
        super(material, type, new ItemProperties()
                .durability(type.getDurability(DURABILITY_MULTIPLIER))
                .rarity(Rarity.RARE));
    }

    /** Original {@code hasGoggles(ItemStack)} (ItemFortressArmor.java:172). */
    public static boolean hasGoggles(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.GOGGLES.get(), false);
    }

    /** Original {@code getMask(ItemStack)}: mask variant int, 0 = none. */
    public static int getMask(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.MASK.get(), 0);
    }

    @Override
    public int getRunicCharge(final ItemStack stack) {
        return 0;
    }

    @Override
    protected void appendHoverText(final ItemStack stack, final TooltipContext context,
                                  final List<Component> tooltip, final TooltipFlag flag) {
        if (hasGoggles(stack)) {
            tooltip.add(Component.translatable("item.ItemGoggles.name")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
        final int mask = getMask(stack);
        if (mask > 0) {
            tooltip.add(Component.translatable("item.HelmetFortress.mask." + mask)
                    .withStyle(ChatFormatting.GOLD));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
