package com.jonnyutah12.thaumcraftclassic.client.fx;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * The only place this slice touches the platform spawn API (TC-M7-02-s).
 *
 * <p>The original's client FX handlers call
 * {@code ParticleEngine.instance.addEffect(world, fx)} from inside the packet
 * handler (e.g. {@code PacketFXBlockSparkle.java:40-42}). Here the handler
 * builds a plan ({@link TcFxPlans}, pure code) and this class hands each entry
 * to {@link Level#addParticle(ParticleOptions..., double...)} — a common-layer
 * call that is a no-op on a dedicated server and real on the client, which is
 * what lets the same entry point be driven from the game-test server.</p>
 */
public final class TcEffects {

    private TcEffects() {
    }

    /**
     * The original's {@code PacketFXBlockSparkle} client handler: plan the
     * sparkle burst and spawn it.
     *
     * @return the number of particles the burst requested (0 when the particle
     *         setting gates the whole burst away)
     */
    public static int blockSparkle(final Level level, final BlockPos pos, final int color,
            final int particleSetting, final RandomSource rand) {
        return spawn(level, TcFxPlans.blockSparkle(pos, color, rand, particleSetting));
    }

    /**
     * The original's {@code PacketFXBlockBubble} client handler: plan the
     * crucible bubble burst and spawn it.
     */
    public static int blockBubble(final Level level, final BlockPos pos, final int color,
            final int particleSetting, final RandomSource rand) {
        return spawn(level, TcFxPlans.blockBubble(pos, color, rand, particleSetting));
    }

    /** Spawns every planned particle and returns how many were requested. */
    public static int spawn(final Level level, final List<ParticleSpawn> plan) {
        for (final ParticleSpawn spawn : plan) {
            level.addParticle(spawn.options(), spawn.x(), spawn.y(), spawn.z(),
                    spawn.vx(), spawn.vy(), spawn.vz());
        }
        return plan.size();
    }
}
