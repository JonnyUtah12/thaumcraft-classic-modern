package com.jonnyutah12.thaumcraftclassic.client.fx;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

/**
 * One requested particle spawn (TC-M7-02-s).
 *
 * <p>The original's FX senders call
 * {@code ParticleEngine.instance.addEffect(world, fx)} directly, so the
 * "spawn request" is implicit in a constructor call. This port separates it:
 * the FX handler builds a plan of these records (pure code, unit- and
 * game-testable on a dedicated server) and hands it to {@link TcEffects},
 * which is the only place that touches {@code Level#addParticle}. That split
 * is what lets the effect's geometry be tested where no client exists —
 * {@code world.field_72995_K} (the original's client-only gate) is a hard wall
 * for a game-test server, so the geometry must not live behind it.</p>
 *
 * @param type    the registered particle type
 * @param options the type's payload
 * @param x       spawn X (world coordinates)
 * @param y       spawn Y
 * @param z       spawn Z
 * @param vx      initial X motion, in the original's motion units
 * @param vy      initial Y motion
 * @param vz      initial Z motion
 */
public record ParticleSpawn(
        ParticleType<?> type, ParticleOptions options,
        double x, double y, double z,
        double vx, double vy, double vz) {
}
