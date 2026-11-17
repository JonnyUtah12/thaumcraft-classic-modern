package com.jonnyutah12.thaumcraftclassic.items.armor;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.ArmorItem;

/**
 * The thaumic armor tables (TC-M6-07-s), kept as Level-free pure data so they
 * unit-test ({@code ArmorRulesTest}, {@code ArmorMaterialParityTest}) and the
 * live registries can be pinned against them by {@code ArmorEquipmentGameTest}.
 *
 * <h2>Original numbers, verbatim</h2>
 * Both materials are the original's {@code EnumHelper.addArmorMaterial} rows in
 * {@code api/ThaumcraftApi.java:37-38}:
 * <ul>
 *   <li>{@code armorMatThaumium = addArmorMaterial("THAUMIUM", 25, new int[]{2, 6, 5, 2}, 25)}</li>
 *   <li>{@code armorMatSpecial  = addArmorMaterial("SPECIAL",  25, new int[]{1, 3, 2, 1}, 25)}</li>
 * </ul>
 * The 1.7.10 int[] is ordered {helmet, chestplate, leggings, boots} and the
 * durability factor multiplies the per-slot table {11, 16, 15, 13} — the same
 * table 1.21.1 exposes as {@code ArmorItem.Type.getDurability(factor)}
 * ({@code HELMET 11, CHESTPLATE 16, LEGGINGS 15, BOOTS 13}), so
 * {@code addArmorMaterial(name, f, ...)} maps onto
 * {@code .durability(type.getDurability(f))} exactly, piece for piece.
 * 1.7.10 has no armour toughness or knockback resistance, so both are 0.
 *
 * <h2>Equipment effects</h2>
 * The vis discount is the original's {@code IVisDiscountGear} rule: the robe
 * returns {@code armorType == 3 ? 1 : 2} ({@code ItemRobeArmor.java:128-129},
 * 1.7.10 slot index 3 = boots) and the goggles a flat 5
 * ({@code ItemGoggles.java:68-69}). None of the three families implements
 * {@code getRunicCharge} with anything but 0 — that is the un-upgraded state
 * the original ships, and the value runic shielding reads for them
 * (TC-M6-07's runic upgrades live on the fortress/void families).
 */
public final class ArmorSetRules {

    /** {@code armorMatThaumium} durability factor. */
    public static final int THAUMIUM_DURABILITY_FACTOR = 25;
    public static final int THAUMIUM_ENCHANTABILITY = 25;
    public static final int THAUMIUM_DEFENSE_HELMET = 2;
    public static final int THAUMIUM_DEFENSE_CHESTPLATE = 6;
    public static final int THAUMIUM_DEFENSE_LEGGINGS = 5;
    public static final int THAUMIUM_DEFENSE_BOOTS = 2;

    /** {@code armorMatSpecial}: the robe (chest/legs/boots) and the goggles. */
    public static final int SPECIAL_DURABILITY_FACTOR = 25;
    public static final int SPECIAL_ENCHANTABILITY = 25;
    public static final int SPECIAL_DEFENSE_HELMET = 1;
    public static final int SPECIAL_DEFENSE_CHESTPLATE = 3;
    public static final int SPECIAL_DEFENSE_LEGGINGS = 2;
    public static final int SPECIAL_DEFENSE_BOOTS = 1;

    /** {@code ItemGoggles} constructor: {@code func_77656_e(350)} = {@code setMaxDamage(350)} (line 31). */
    public static final int GOGGLES_MAX_DAMAGE = 350;

    /** {@code ItemGoggles.getVisDiscount}: a flat 5 percent. */
    public static final int GOGGLES_VIS_DISCOUNT = 5;

    /**
     * {@code getRunicCharge} for every shipped thaumic armor class
     * (thaumium, robe, goggles): 0. Runic capacity comes from the runic
     * baubles (TC-M8-02) and the runic armour upgrades, not from these.
     */
    public static final int RUNIC_CHARGE_UNUPGRADED = 0;

    /**
     * Anvil repair items, as {@code ItemResource} metas: thaumium armour
     * repairs with the thaumium ingot (meta 2, {@code ItemThaumiumArmor.java:62-63}),
     * the robe with enchanted fabric (meta 7, {@code ItemRobeArmor.java:123-124}),
     * the goggles with a gold ingot ({@code ItemGoggles.java:63-64},
     * {@code Items.field_151043_k} = {@code gold_ingot} per the 1.7.10 MCP
     * field table).
     */
    public static final int REPAIR_META_THAUMIUM_INGOT = 2;
    public static final int REPAIR_META_ENCHANTED_FABRIC = 7;

    /**
     * The robe's robe colour: 1.7.10 {@code ItemRobeArmor.getColor} ({@code func_82814_b}, lines 74-82) returns
     * {@code 6961280} (= {@code 0x6A3880}) when the stack carries no
     * {@code display.color}.
     */
    public static final int ROBE_DEFAULT_COLOR = 0x6A3880;

    /**
     * Original {@code ItemRobeArmor.getVisDiscount}: {@code armorType == 3 ? 1 : 2}
     * — the boots give 1, the chest and leggings 2. There is no robe helmet
     * (the original never registered one), so HELMET is unreachable here.
     */
    public static int robeVisDiscount(final ArmorItem.Type type) {
        return type == ArmorItem.Type.BOOTS ? 1 : 2;
    }

    /** 1.7.10 {@code maxDamage[slot] = durabilityFactor * {11,16,15,13}[slot]}. */
    public static int maxDamage(final ArmorItem.Type type, final int durabilityFactor) {
        return type.getDurability(durabilityFactor);
    }

    /** The 1.7.10 {helmet, chestplate, leggings, boots} array as 1.21.1's map. */
    public static Map<ArmorItem.Type, Integer> defenseMap(
            final int helmet, final int chestplate, final int leggings, final int boots) {
        final Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, helmet);
        defense.put(ArmorItem.Type.CHESTPLATE, chestplate);
        defense.put(ArmorItem.Type.LEGGINGS, leggings);
        defense.put(ArmorItem.Type.BOOTS, boots);
        // 1.21.1 has a fifth (BODY) slot the original never filled; it takes the
        // chestplate value, the same choice vanilla makes for its own materials.
        defense.put(ArmorItem.Type.BODY, chestplate);
        return defense;
    }

    private ArmorSetRules() {
    }
}
