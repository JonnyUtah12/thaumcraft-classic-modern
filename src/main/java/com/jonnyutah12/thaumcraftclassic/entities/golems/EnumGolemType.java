package com.jonnyutah12.thaumcraftclassic.entities.golems;

import java.util.HashMap;
import java.util.Map;

/**
 * The original golem materials — Thaumcraft 4.2.3.5
 * {@code thaumcraft.common.entities.golems.EnumGolemType}, transcribed value
 * for value from the decompiled source.
 *
 * <p>The enum is the port's single source of truth for every golem balance
 * figure: {@code EnumGolemType.java} carries the eight constructor rows and
 * nothing here is inferred. Field order in the original constructor is
 * {@code (health, armor, speed, fireResist, upgrades, carry, regenDelay,
 * strength)}, so the rows read:</p>
 *
 * <pre>
 * STRAW   (10,  0, 0.38F, false, 1,  1,  75, 0)
 * WOOD    (20,  6, 0.35F, false, 1,  4,  75, 1)
 * TALLOW  (20,  9, 0.33F, false, 2,  8,  75, 2)
 * CLAY    (25,  9, 0.33F, true,  1,  8, 100, 2)
 * FLESH   (15,  6, 0.35F, false, 2,  4,  40, 1)
 * STONE   (30, 12, 0.32F, true,  1, 16, 100, 3)
 * IRON    (35, 15, 0.31F, true,  1, 32, 125, 4)
 * THAUMIUM(40, 15, 0.32F, true,  2, 32, 100, 4)
 * </pre>
 *
 * <p>The ordinal <em>is</em> the item metadata: {@code ItemGolemPlacer}'s
 * {@code iconGolem[8]} array is indexed by the stack's damage value, and its
 * {@code getUnlocalizedName(ItemStack)} returns
 * {@code <item>.<damageValue>} — the port's per-meta name key. So
 * {@code getType(meta)} is the meta → material contract the placer, the
 * research icons (meta 0..7) and the crucible recipe rows all rely on.</p>
 *
 * <p>{@link #getType(int)} mirrors the original exactly, including its lazily
 * built {@code codeToTypeMapping} and the resulting {@code null} for a meta
 * outside 0..7.</p>
 */
public enum EnumGolemType {

    // health, armor, speed, fireResist, upgrades, carry, regenDelay, strength
    STRAW(10, 0, 0.38F, false, 1, 1, 75, 0),
    WOOD(20, 6, 0.35F, false, 1, 4, 75, 1),
    TALLOW(20, 9, 0.33F, false, 2, 8, 75, 2),
    CLAY(25, 9, 0.33F, true, 1, 8, 100, 2),
    FLESH(15, 6, 0.35F, false, 2, 4, 40, 1),
    STONE(30, 12, 0.32F, true, 1, 16, 100, 3),
    IRON(35, 15, 0.31F, true, 1, 32, 125, 4),
    THAUMIUM(40, 15, 0.32F, true, 2, 32, 100, 4);

    /** Original {@code EnumGolemType.health} — the golem's max health. */
    public final int health;
    /** Original {@code armor} — the golem's armour figure. */
    public final int armor;
    /** Original {@code speed} — the base movement speed. */
    public final float speed;
    /** Original {@code fireResist} — {@code setupGolem} sets {@code isImmuneToFire}. */
    public final boolean fireResist;
    /** Original {@code upgrades} — the number of upgrade slots (advanced golems get one more). */
    public final int upgrades;
    /** Original {@code carry} — the base carry limit. */
    public final int carry;
    /** Original {@code regenDelay} — ticks between the 1.0 self-repair heals. */
    public final int regenDelay;
    /** Original {@code strength} — feeds {@code getGolemStrength()}. */
    public final int strength;

    private static Map<Integer, EnumGolemType> codeToTypeMapping;

    EnumGolemType(final int health, final int armor, final float speed, final boolean fireResist,
                  final int upgrades, final int carry, final int regenDelay, final int strength) {
        this.health = health;
        this.armor = armor;
        this.speed = speed;
        this.fireResist = fireResist;
        this.upgrades = upgrades;
        this.carry = carry;
        this.regenDelay = regenDelay;
        this.strength = strength;
    }

    /** Original {@code EnumGolemType.getType(int)}: the ordinal → material map (null outside 0..7). */
    public static EnumGolemType getType(final int i) {
        if (codeToTypeMapping == null) {
            initMapping();
        }
        return codeToTypeMapping.get(i);
    }

    private static void initMapping() {
        codeToTypeMapping = new HashMap<>();
        for (EnumGolemType s : values()) {
            codeToTypeMapping.put(s.ordinal(), s);
        }
    }
}
