package com.jonnyutah12.thaumcraftclassic.api.entities;

/**
 * TC-M5-04-s: the marker the taint engine consults before it poisons an
 * entity (content-mapping.csv row {@code entity.ITaintedMob}, original
 * {@code thaumcraft.api.ThaumcraftApiHelper.ITaintedMob}).
 *
 * <p>The original taint code asks the API helper first —
 * {@code ThaumcraftApiHelper.isTaintedMob(ent)} — and skips the Taint Poison
 * effect for anything that answers yes: taint creatures are made of taint, so
 * walking the crust heals them rather than poisoning them. The same query
 * gates the bottled-taint splash in {@code EntityBottleTaint.onImpact}
 * (impact lines 45-49) and the biome-decay checks.</p>
 *
 * <p>It ships as an interface because 1.21.1 has no registry-side flag for
 * it: TC-M6-06's taint creatures (taintacle, taint spore, spore swarmer,
 * falling taint, and anything a datapack marks) implement this and the engine
 * needs no edit when they land. {@code TaintManager.applyTaintPoison} is the
 * only consumer, so the gate has one home.</p>
 */
public interface ITaintedMob {
}
