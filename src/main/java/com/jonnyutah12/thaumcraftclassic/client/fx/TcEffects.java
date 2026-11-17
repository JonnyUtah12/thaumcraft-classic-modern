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

    /**
     * The original's ward-flash senders: the ward blocks' hit effects call
     * {@code ClientProxy.blockWard} (ClientProxy.java:980-984) with the block,
     * the hit face and the {@code (hitVec - pos)} triple, which spawns exactly
     * one hexagram on the block centre (TC-M7-02-c-s-f4). The 1.21.1 sender is
     * the client block extension {@link TcWardHitEffects}.
     *
     * @return the number of particles requested (always one; the plan has no
     *         particle-setting gate — the original has none either)
     */
    public static int wardBlock(final Level level, final BlockPos pos,
            final net.minecraft.core.Direction side, final double f, final double f1,
            final double f2, final RandomSource rand) {
        return spawn(level, TcFxPlans.wardBlock(pos, side, f, f1, f2, rand));
    }

    /**
     * The original's {@code PacketFXShield} client handler: the shield runes
     * riding the source entity, aimed at the target, with the
     * {@code -1 / -2 / -3} sentinel branches (PacketFXShield.java:38-79,
     * TC-M7-02-c-s-f4).
     *
     * @param cultist the original's {@code instanceof EntityCultist} exclusion
     *                of the mob branch (FXShieldRunes.java:61); no cultist
     *                entity is landed, so senders pass false
     * @return the number of runes the handler asked for (0 - 2)
     */
    public static int shieldRunes(final Level level, final int sourceEntityId,
            final int targetEntityId, final boolean cultist, final RandomSource rand) {
        return spawn(level, TcFxPlans.shieldRunes(level, sourceEntityId, targetEntityId,
                cultist, rand));
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
