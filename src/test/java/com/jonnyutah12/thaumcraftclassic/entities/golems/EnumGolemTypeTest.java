package com.jonnyutah12.thaumcraftclassic.entities.golems;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins {@link EnumGolemType} to the decompiled
 * {@code thaumcraft.common.entities.golems.EnumGolemType}: the eight materials
 * in order and every constructor field, value for value. Each assertion below
 * quotes the original enum row it mirrors, so a future edit that "adjusts" a
 * balance figure fails here instead of shipping silently.
 */
class EnumGolemTypeTest {

    @Test
    void hasTheEightMaterialsInSourceOrder() {
        assertEquals(8, EnumGolemType.values().length,
                "EnumGolemType declares exactly eight values (STRAW..THAUMIUM)");
        assertEquals(
                java.util.List.of("STRAW", "WOOD", "TALLOW", "CLAY", "FLESH", "STONE", "IRON", "THAUMIUM"),
                java.util.Arrays.stream(EnumGolemType.values()).map(Enum::name).toList(),
                "the original declares STRAW, WOOD, TALLOW, CLAY, FLESH, STONE, IRON, THAUMIUM in this order");
    }

    /** {@code STRAW(10, 0, 0.38F, false, 1, 1, 75, 0)}. */
    @Test
    void strawRowIsPinned() {
        assertRow(EnumGolemType.STRAW, 10, 0, 0.38F, false, 1, 1, 75, 0);
    }

    /** {@code WOOD(20, 6, 0.35F, false, 1, 4, 75, 1)}. */
    @Test
    void woodRowIsPinned() {
        assertRow(EnumGolemType.WOOD, 20, 6, 0.35F, false, 1, 4, 75, 1);
    }

    /** {@code TALLOW(20, 9, 0.33F, false, 2, 8, 75, 2)}. */
    @Test
    void tallowRowIsPinned() {
        assertRow(EnumGolemType.TALLOW, 20, 9, 0.33F, false, 2, 8, 75, 2);
    }

    /** {@code CLAY(25, 9, 0.33F, true, 1, 8, 100, 2)}. */
    @Test
    void clayRowIsPinned() {
        assertRow(EnumGolemType.CLAY, 25, 9, 0.33F, true, 1, 8, 100, 2);
    }

    /** {@code FLESH(15, 6, 0.35F, false, 2, 4, 40, 1)}. */
    @Test
    void fleshRowIsPinned() {
        assertRow(EnumGolemType.FLESH, 15, 6, 0.35F, false, 2, 4, 40, 1);
    }

    /** {@code STONE(30, 12, 0.32F, true, 1, 16, 100, 3)}. */
    @Test
    void stoneRowIsPinned() {
        assertRow(EnumGolemType.STONE, 30, 12, 0.32F, true, 1, 16, 100, 3);
    }

    /** {@code IRON(35, 15, 0.31F, true, 1, 32, 125, 4)}. */
    @Test
    void ironRowIsPinned() {
        assertRow(EnumGolemType.IRON, 35, 15, 0.31F, true, 1, 32, 125, 4);
    }

    /** {@code THAUMIUM(40, 15, 0.32F, true, 2, 32, 100, 4)}. */
    @Test
    void thaumiumRowIsPinned() {
        assertRow(EnumGolemType.THAUMIUM, 40, 15, 0.32F, true, 2, 32, 100, 4);
    }

    /**
     * Only CLAY, STONE, IRON and THAUMIUM are fire resistant — the four
     * {@code true} rows of the original constructor, and what
     * {@code EntityGolemBase.setupGolem}'s {@code field_70178_ae} sets.
     */
    @Test
    void fireResistanceIsTheClayStoneIronThaumiumSet() {
        assertEquals(
                java.util.List.of(EnumGolemType.CLAY, EnumGolemType.STONE, EnumGolemType.IRON, EnumGolemType.THAUMIUM),
                java.util.Arrays.stream(EnumGolemType.values()).filter(t -> t.fireResist).toList(),
                "the original's fireResist is true for CLAY, STONE, IRON and THAUMIUM only");
    }

    /**
     * The ordinal is the item metadata: {@code ItemGolemPlacer} indexes
     * {@code iconGolem[8]} with the stack's damage value, and
     * {@code ItemGolemCore}/{@code ItemGolemUpgrade}/{@code
     * ItemGolemDecoration} follow the same convention.
     */
    @Test
    void getTypeIsTheOrdinalMapping() {
        for (final EnumGolemType type : EnumGolemType.values()) {
            assertSame(type, EnumGolemType.getType(type.ordinal()),
                    "getType(ordinal) must return the material with that metadata");
        }
    }

    /** The original's {@code codeToTypeMapping.get(i)} is null outside 0..7. */
    @Test
    void getTypeIsNullOutsideTheEightMetas() {
        assertNull(EnumGolemType.getType(-1), "meta -1 is not a material");
        assertNull(EnumGolemType.getType(8), "meta 8 is not a material (the 8 slots are 0..7)");
        assertNull(EnumGolemType.getType(100), "meta 100 is the golem core blank, not a material");
        assertNull(EnumGolemType.getType(32767), "meta 32767 is the original's wildcard, not a material");
    }

    /** The upgrade slot count is {@code upgrades + (advanced ? 1 : 0)} — the original's array sizing. */
    @Test
    void upgradeSlotCountFollowsTheOrdinaryAndAdvancedRule() {
        assertTrue(EnumGolemType.STRAW.upgrades == 1 && EnumGolemType.TALLOW.upgrades == 2,
                "the carry/upgrade columns are the third and fifth constructor arguments");
        assertEquals(1, EnumGolemType.STRAW.upgrades + (false ? 1 : 0));
        assertEquals(3, EnumGolemType.THAUMIUM.upgrades + (true ? 1 : 0));
    }

    private static void assertRow(final EnumGolemType type, final int health, final int armor, final float speed,
                                  final boolean fireResist, final int upgrades, final int carry,
                                  final int regenDelay, final int strength) {
        final String row = type + "(health, armor, speed, fireResist, upgrades, carry, regenDelay, strength)";
        assertEquals(health, type.health, row + " health");
        assertEquals(armor, type.armor, row + " armor");
        assertEquals(speed, type.speed, row + " speed");
        assertEquals(fireResist, type.fireResist, row + " fireResist");
        assertEquals(upgrades, type.upgrades, row + " upgrades");
        assertEquals(carry, type.carry, row + " carry");
        assertEquals(regenDelay, type.regenDelay, row + " regenDelay");
        assertEquals(strength, type.strength, row + " strength");
    }
}
