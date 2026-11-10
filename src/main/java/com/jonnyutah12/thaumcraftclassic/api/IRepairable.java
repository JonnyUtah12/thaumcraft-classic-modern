package com.jonnyutah12.thaumcraftclassic.api;

/**
 * Marker for items the Repair enchantment may mend (TC-M6-05).
 *
 * <p>Original: {@code thaumcraft.api.IRepairable}. The enchantment's
 * behaviour handler only mends stacks whose item implements this interface
 * <em>and</em> that carry object (essentia) tags — the original's
 * {@code isItemStackDamageable() && instanceof IRepairable} gate; the
 * damageable half is carried by the {@code #thaumcraft:enchantments/repair_applicable}
 * item tag the enchantment JSON references.</p>
 */
public interface IRepairable {
}
