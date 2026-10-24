package com.jonnyutah12.thaumcraftclassic.api.entities;

/**
 * Marker for mobs that belong to the taint (TC-M6-05).
 *
 * <p>Original: {@code thaumcraft.api.entities.ITaintedMob}. Taint Poison
 * heals these mobs instead of damaging them, and taint blocks / flux gas
 * skip them entirely. The taint milestone (TC-M5-04) implements this on its
 * mob entities; the entities milestone (TC-M6-06) keeps it for any ported
 * taint creature.</p>
 */
public interface ITaintedMob {
}
