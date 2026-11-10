package com.jonnyutah12.thaumcraftclassic.casting;

/**
 * A castable projectile (TC-M6-03-p): the ember, explosive orb, shock orb, frost
 * shard, pech blast, primal orb and fire bat the focus bodies aim.
 *
 * <p>These are TC-M6-06's content — the seven entity rows in
 * {@code docs/content-mapping.csv} are {@code not_started} and the
 * {@code Entity*} classes named by each original body
 * ({@code ItemFocusFire.java:82-135}, {@code ItemFocusShock.java:114-127},
 * {@code ItemFocusFrost.java:53-85}, {@code ItemFocusPech.java:64-76},
 * {@code ItemFocusPrimal.java:57-67}, {@code ItemFocusHellbat.java:85-136}) do
 * not exist here. This interface is the contract this card codes against: when
 * a TC-M6-06 entity class implements it, the performer launches it with the
 * cast's context and the focus becomes playable with no change to this file.</p>
 *
 * <p>An entity that does not implement it is launched on a best-effort
 * fallback — spawned at the caster's eyes aimed along their view with the
 * spec's speed — so a registered entity gains motion even before the original's
 * per-type launch parameters (nightshade trail, frosty damage, scatter
 * variance, the fire bat's point-blank target) are ported.</p>
 */
public interface FocusProjectile {

    /**
     * Launches this projectile as the cast aims it: the caster, their eye
     * position and view vector, and the effect's parameters (count is handled
     * by the performer — this launches one projectile). The original's entity
     * constructors take {@code (world, player)} and set their own start at the
     * player's eyes; {@code ItemFocusFrost} then applies the spec's speed
     * {@code (1.0F/8.0F)}, {@code ItemFocusPech} the potency, and
     * {@code ItemFocusHellbat} sets the pointed-entity target.
     */
    void launch(CastContext context, SpellEffect.Projectile spec);
}
