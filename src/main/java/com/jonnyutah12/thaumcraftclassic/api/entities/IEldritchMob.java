package com.jonnyutah12.thaumcraftclassic.api.entities;

/**
 * Marks a mob as an eldritch mob (original {@code
 * thaumcraft.api.entities.IEldritchMob}, TC-M6-04f-s).
 *
 * <p>The original used it for exactly one behaviour outside the mobs
 * themselves: {@code TileEldritchCrabSpawner} and {@code TileEldritchAltar}
 * counted only entities implementing this interface when deciding whether the
 * box around them was already stocked, so a player's pet never blocked a
 * guardian from spawning. It is a pure marker — the interface declares
 * nothing. The Crimson Cult family that implements it (eldrited guardian,
 * cultist, taintacle, swarm) is TC-M6-06, which is why no mob in this port
 * carries it yet.</p>
 */
public interface IEldritchMob {
}
