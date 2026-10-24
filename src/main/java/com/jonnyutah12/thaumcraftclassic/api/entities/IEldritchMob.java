package com.jonnyutah12.thaumcraftclassic.api.entities;

/**
 * Marker for mobs that count as "eldritch" for TC4 rules (content-mapping.csv
 * row {@code Thaumcraft:IEldritchMob}). In the original this gated the eldritch
 * altar/lock spawn logic and the Outer Lands maze spawners: the eldritch
 * guardian, warden, golem, crab and mind spider all implement it.
 *
 * <p>The entity classes themselves are TC-M6-06; this interface exists so the
 * maze/altar/lock code (TC-M6-04f) can reference the concept without depending
 * on the entity card. Entities that implement it may be spawned by the Outer
 * Lands generators.</p>
 */
public interface IEldritchMob {
}
