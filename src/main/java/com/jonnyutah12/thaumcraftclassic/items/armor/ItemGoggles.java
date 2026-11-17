package com.jonnyutah12.thaumcraftclassic.items.armor;

import com.jonnyutah12.thaumcraftclassic.api.IGoggles;
import com.jonnyutah12.thaumcraftclassic.api.IRevealer;
import com.jonnyutah12.thaumcraftclassic.api.IRunicArmor;
import com.jonnyutah12.thaumcraftclassic.api.IVisDiscountGear;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.ChatFormatting;

/**
 * Goggles of Revealing (TC-M6-07-s): port of
 * {@code thaumcraft.common.items.armor.ItemGoggles}
 * ({@code common/items/armor/ItemGoggles.java}), registered in the head slot
 * ({@code ConfigItems.java:373},
 * {@code new ItemGoggles(ThaumcraftApi.armorMatSpecial, 4, 0)}).
 *
 * <p>The signature effect is node revealing. The original implements
 * {@code IRevealer.showNodes} and {@code IGoggles.showIngamePopups} as
 * {@code return true} (lines 101-109); the port's consumer of the first is
 * {@code nodes/NodeReveal}, which reads the head slot exactly as the original's
 * node renderer did ({@code TileNodeRenderer.java:187-189}), and the second is
 * the aspect-popup gate the TC-M7-01 visual pass feeds.</p>
 *
 * <p>The rarity is the {@code minecraft:rarity} component (1.21.1 has no
 * {@code Item.getRarity(ItemStack)} to override): {@code CatalogArmor} passes
 * {@code Rarity.RARE}, the original's {@code EnumRarity.rare}
 * ({@code ItemGoggles.java:59-60}).</p>
 *
 * <p>Also carried: the flat 5 percent vis discount
 * ({@code getVisDiscount}, lines 68-69 — the largest single discount in the game),
 * {@code setMaxDamage(350)} (constructor, over the material's per-slot
 * durability), {@code EnumRarity.rare} (lines 59-60), anvil repair with a gold
 * ingot (lines 63-64), and the {@code tc.visdiscount} tooltip line (lines 40-42).</p>
 */
public class ItemGoggles extends ArmorItem
        implements IRunicArmor, IVisDiscountGear, IRevealer, IGoggles {

    public ItemGoggles(final Holder<ArmorMaterial> material, final Type type,
                       final Properties properties) {
        super(material, type, properties);
    }

    /** Original lines 36-37: {@code return 0}. */
    @Override
    public int getRunicCharge(final ItemStack stack) {
        return ArmorSetRules.RUNIC_CHARGE_UNUPGRADED;
    }

    /** Original lines 68-69: a flat 5 percent, for every aspect. */
    @Override
    public int getVisDiscount(final ItemStack stack, final Player player, final Aspect aspect) {
        return ArmorSetRules.GOGGLES_VIS_DISCOUNT;
    }

    /**
     * Original lines 73-74: the renderer's gate. The {@code wearer} is the
     * viewer the original passed ({@code EntityLivingBase}); the answer does
     * not depend on it.
     */
    @Override
    public boolean showNodes(final ItemStack stack, final LivingEntity wearer) {
        return true;
    }

    /** Original lines 78-79: in-world aspect popups. */
    @Override
    public boolean showIngamePopups(final ItemStack stack, final LivingEntity wearer) {
        return true;
    }

    /**
     * Original lines 63-64: repairable with a gold ingot
     * ({@code Items.field_151043_k} = {@code gold_ingot}, line 64). Replaces
     * {@code super}, whose material-level rule is the shared
     * {@code armorMatSpecial} fabric rule — the original's goggles never
     * accepted enchanted fabric.
     */
    @Override
    public boolean isValidRepairItem(final ItemStack toRepair, final ItemStack repair) {
        return repair.is(Items.GOLD_INGOT);
    }

    /** Original lines 40-42: super's lines, then the vis-discount line. */
    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tc.visdiscount")
                .append(": " + getVisDiscount(stack, null, null) + "%")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
