package com.jonnyutah12.thaumcraftclassic.wands;

import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryService;
import com.jonnyutah12.thaumcraftclassic.api.IVisDiscountGear;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Worn-gear vis discount aggregation (TC-M8-02): the port of
 * {@code thaumcraft.common.items.wands.WandManager.getTotalVisDiscount}
 * ({@code common/items/wands/WandManager.java:49-84}), feeding
 * {@link VisCostRules.VisDiscountSource} (docs/architecture/vis.md).
 *
 * <p>Original scan preserved: the four bauble slots (lines 55-60 → the
 * {@link AccessoryService} worn set, TC-M8-01) and the four armor slots
 * (lines 62-66), each {@code IVisDiscountGear.getVisDiscount} summed in
 * percent. Armor slots are scanned here so TC-M6-07 runic/fortress armor
 * joins the same total through the same contract.</p>
 *
 * <p>The Vis Exhaustion / Infectious Vis Exhaustion term (lines 68-81,
 * {@code -(max(amplifier)+1)*10}) arrives with the potions (TC-M4-03);
 * until then the amplifier is -1 ("not under the effect"), which
 * {@link VisCostRules#totalDiscount} treats as no term — the exact
 * singleplayer behaviour without those potions.</p>
 */
public final class WornGearDiscounts {

    /** Percent total from worn gear only (armor slots + accessory slots). */
    public static int gearPercentTotal(final Player player, final Aspect aspect) {
        if (player == null) {
            return 0;
        }
        int total = 0;
        for (final ItemStack worn : AccessoryService.wornStacks(player)) {
            if (!worn.isEmpty() && worn.getItem() instanceof IVisDiscountGear gear) {
                total += gear.getVisDiscount(worn, player, aspect);
            }
        }
        for (final EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
            final ItemStack armor = player.getItemBySlot(slot);
            if (!armor.isEmpty() && armor.getItem() instanceof IVisDiscountGear gear) {
                total += gear.getVisDiscount(armor, player, aspect);
            }
        }
        return total;
    }

    /**
     * The discount source for a specific wearer, in the fraction-of-cost units
     * {@link VisCostRules} consumes (total percent / 100, original line 83).
     */
    public static VisCostRules.VisDiscountSource sourceFor(final Player player) {
        return aspect -> VisCostRules.totalDiscount(
                gearPercentTotal(player, aspect),
                visExhaustAmplifier(player),
                infectiousVisExhaustAmplifier(player));
    }

    /**
     * Highest active Vis Exhaustion amplifier (0-based), or -1 when the effect
     * is not active. The potions land with TC-M4-03; until then this is
     * always -1, matching the original for players without the effects.
     */
    public static int visExhaustAmplifier(final Player player) {
        return -1;
    }

    /** Infectious Vis Exhaustion amplifier, same deferral as {@link #visExhaustAmplifier}. */
    public static int infectiousVisExhaustAmplifier(final Player player) {
        return -1;
    }

    private WornGearDiscounts() {
    }
}
