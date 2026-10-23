package com.jonnyutah12.thaumcraftclassic.runic;

import net.minecraft.world.item.ItemStack;

/**
 * Which runic-shield upgrade a worn {@link com.jonnyutah12.thaumcraftclassic.api.IRunicArmor
 * IRunicArmor} stack contributes. The original did not name this: it
 * instanceof-tested the concrete item classes and switched on the damage value
 * inside the scan ({@code EventHandlerRunic.livingTick}, {@code common/lib/
 * events/EventHandlerRunic.java:66-80}) — charged ring meta 2, revitalizing
 * ring meta 3, emergency amulet meta 1, kinetic girdle meta 1. This interface
 * carries the same per-stack facts so the scan stays open for TC-M6-07 armor
 * without concrete-class imports.
 *
 * <p>Effects (all fire only when the pool is driven to zero by an absorbed
 * hit, each with its own cooldown — see {@link RunicShielding}):</p>
 * <ul>
 * <li>{@code charged}: recharge interval shortened by 500 ms per stack;</li>
 * <li>{@code healing}: Regeneration II-class effect, 240 ticks;</li>
 * <li>{@code kinetic}: no-damage push explosion, radius 1.5 + 0.5/stack;</li>
 * <li>{@code emergency}: instant refill to {@code min(max, 8 * count)}.</li>
 * </ul>
 */
public interface RunicUpgradeFlags {

    boolean charged(ItemStack stack);

    boolean kinetic(ItemStack stack);

    boolean healing(ItemStack stack);

    boolean emergency(ItemStack stack);
}
