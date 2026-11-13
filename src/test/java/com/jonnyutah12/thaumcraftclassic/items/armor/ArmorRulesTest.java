package com.jonnyutah12.thaumcraftclassic.items.armor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import net.minecraft.world.item.ArmorItem;
import org.junit.jupiter.api.Test;

/**
 * TC-M6-07-s unit evidence: the thaumic armour tables, with no Level and no
 * registry (the house rule for the pure rule classes — the live registry reads
 * are {@code ArmorEquipmentGameTest}'s job).
 *
 * <p>Every number here is the original's; {@code ArmorMaterialParityTest}
 * parses them out of the 1.7.10 sources, so the pairing is: this file pins the
 * shipped table, the parity file pins that table to the original text.</p>
 */
class ArmorRulesTest {

    /**
     * 1.7.10's per-slot durability factors from {@code EnumArmorMaterial}
     * ({@code {11, 16, 15, 13}} for helmet/chest/legs/boots), and 1.21.1's
     * table as {@code ArmorItem.Type.getDurability}: the two must agree piece
     * for piece, which is what makes the 1.7.10 durability factor portable.
     */
    private static final int[] VANILLA_1710_DURABILITY_FACTORS = {11, 16, 15, 13};

    @Test
    void thaumiumDefenceIsTheOriginalThaumiumiumRow() {
        // addArmorMaterial("THAUMIUM", 25, new int[]{2, 6, 5, 2}, 25)
        final Map<ArmorItem.Type, Integer> defense = ArmorSetRules.defenseMap(
                ArmorSetRules.THAUMIUM_DEFENSE_HELMET, ArmorSetRules.THAUMIUM_DEFENSE_CHESTPLATE,
                ArmorSetRules.THAUMIUM_DEFENSE_LEGGINGS, ArmorSetRules.THAUMIUM_DEFENSE_BOOTS);
        assertEquals(2, defense.get(ArmorItem.Type.HELMET));
        assertEquals(6, defense.get(ArmorItem.Type.CHESTPLATE));
        assertEquals(5, defense.get(ArmorItem.Type.LEGGINGS));
        assertEquals(2, defense.get(ArmorItem.Type.BOOTS));
        assertEquals(ArmorSetRules.THAUMIUM_DEFENSE_CHESTPLATE, defense.get(ArmorItem.Type.BODY));
        assertEquals(25, ArmorSetRules.THAUMIUM_ENCHANTABILITY);
        assertEquals(25, ArmorSetRules.THAUMIUM_DURABILITY_FACTOR);
    }

    @Test
    void specialDefenceIsTheOriginalSpecialRow() {
        // addArmorMaterial("SPECIAL", 25, new int[]{1, 3, 2, 1}, 25)
        final Map<ArmorItem.Type, Integer> defense = ArmorSetRules.defenseMap(
                ArmorSetRules.SPECIAL_DEFENSE_HELMET, ArmorSetRules.SPECIAL_DEFENSE_CHESTPLATE,
                ArmorSetRules.SPECIAL_DEFENSE_LEGGINGS, ArmorSetRules.SPECIAL_DEFENSE_BOOTS);
        assertEquals(1, defense.get(ArmorItem.Type.HELMET));
        assertEquals(3, defense.get(ArmorItem.Type.CHESTPLATE));
        assertEquals(2, defense.get(ArmorItem.Type.LEGGINGS));
        assertEquals(1, defense.get(ArmorItem.Type.BOOTS));
        assertEquals(25, ArmorSetRules.SPECIAL_ENCHANTABILITY);
    }

    /**
     * The portability proof: 1.7.10 multiplied its durability factor by
     * {11, 16, 15, 13}; 1.21.1's {@code ArmorItem.Type.getDurability(factor)}
     * carries the same table, so the original's factor ports unchanged and each
     * piece lands on the original's max damage (thaumium/robe: 275/400/375/325).
     */
    @Test
    void durabilityFactorTableMatchesThe1710Table() {
        final ArmorItem.Type[] order = {ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE,
                ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS};
        final int[] thaumiumMaxDamage = {275, 400, 375, 325};
        for (int i = 0; i < order.length; i++) {
            final int factor = VANILLA_1710_DURABILITY_FACTORS[i];
            assertEquals(factor, ArmorSetRules.maxDamage(order[i], 1),
                    order[i] + " must use the original durability factor " + factor);
            assertEquals(factor * ArmorSetRules.THAUMIUM_DURABILITY_FACTOR,
                    ArmorSetRules.maxDamage(order[i], ArmorSetRules.THAUMIUM_DURABILITY_FACTOR),
                    order[i] + " durability must be factor x table value");
            assertEquals(thaumiumMaxDamage[i],
                    ArmorSetRules.maxDamage(order[i], ArmorSetRules.THAUMIUM_DURABILITY_FACTOR),
                    order[i] + " max damage must be the original's " + thaumiumMaxDamage[i]);
        }
    }

    /** {@code ItemGoggles}: {@code setMaxDamage(350)} overrides the material row. */
    @Test
    void gogglesCarryTheirOwnDurability() {
        assertEquals(350, ArmorSetRules.GOGGLES_MAX_DAMAGE);
        assertNotEquals(ArmorSetRules.maxDamage(ArmorItem.Type.HELMET,
                        ArmorSetRules.SPECIAL_DURABILITY_FACTOR),
                ArmorSetRules.GOGGLES_MAX_DAMAGE,
                "the goggles' 350 is an override of the material's 25 x 11 = 275");
    }

    /**
     * The vis-discount contract: robe boots 1, robe chest and legs 2
     * ({@code ItemRobeArmor.getVisDiscount}: {@code armorType == 3 ? 1 : 2}),
     * goggles a flat 5 ({@code ItemGoggles.getVisDiscount}), and the full set
     * sums to the original's 5 for the robe and 10 with the goggles.
     */
    @Test
    void visDiscountsAreTheOriginalAmounts() {
        assertEquals(1, ArmorSetRules.robeVisDiscount(ArmorItem.Type.BOOTS),
                "the original's armoured-boot slot (armorType 3) discounts 1");
        assertEquals(2, ArmorSetRules.robeVisDiscount(ArmorItem.Type.CHESTPLATE));
        assertEquals(2, ArmorSetRules.robeVisDiscount(ArmorItem.Type.LEGGINGS));
        assertEquals(5, ArmorSetRules.GOGGLES_VIS_DISCOUNT);
        assertEquals(5, ArmorSetRules.robeVisDiscount(ArmorItem.Type.CHESTPLATE)
                        + ArmorSetRules.robeVisDiscount(ArmorItem.Type.LEGGINGS)
                        + ArmorSetRules.robeVisDiscount(ArmorItem.Type.BOOTS),
                "a full robe is the original's 5 percent");
        assertEquals(10, ArmorSetRules.GOGGLES_VIS_DISCOUNT
                        + ArmorSetRules.robeVisDiscount(ArmorItem.Type.CHESTPLATE)
                        + ArmorSetRules.robeVisDiscount(ArmorItem.Type.LEGGINGS)
                        + ArmorSetRules.robeVisDiscount(ArmorItem.Type.BOOTS),
                "goggles over a full robe is the original's 10 percent");
    }

    /**
     * Every shipped thaumic armour class is un-upgraded: runic charge 0, which
     * is what {@code runic/RunicShielding}'s armour-slot scan reads for them
     * (the capacity lives on the runic baubles and the runic armour upgrades).
     */
    @Test
    void unUpgradedArmourCarriesZeroRunicCharge() {
        assertEquals(0, ArmorSetRules.RUNIC_CHARGE_UNUPGRADED);
    }

    /** The anvil-repair metas of the original's three families. */
    @Test
    void repairMetasAreTheOriginalItems() {
        // ItemThaumiumArmor: itemResource meta 2 = thaumium ingot
        assertEquals(2, ArmorSetRules.REPAIR_META_THAUMIUM_INGOT);
        // ItemRobeArmor: itemResource meta 7 = enchanted fabric
        assertEquals(7, ArmorSetRules.REPAIR_META_ENCHANTED_FABRIC);
    }

    /** {@code ItemRobeArmor.getColor}'s default: 6961280 = 0x6A3880. */
    @Test
    void robeDefaultColourIsTheOriginalInt() {
        assertEquals(6961280, ArmorSetRules.ROBE_DEFAULT_COLOR);
        assertEquals(0x6A3880, ArmorSetRules.ROBE_DEFAULT_COLOR);
        assertTrue(ArmorSetRules.ROBE_DEFAULT_COLOR >= 0
                        && ArmorSetRules.ROBE_DEFAULT_COLOR <= 0xFFFFFF,
                "the tint must be a 24-bit RGB int, the shape display.color carried");
    }
}
