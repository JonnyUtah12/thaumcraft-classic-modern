package com.jonnyutah12.thaumcraftclassic.client.fx;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SoundType;

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

    // ---- TC-M7-02-c-s: wisp family, sender motes, entity-space shells ----

    /** {@code ClientProxy.wispFX} (ClientProxy.java:700-703). */
    public static int wispFx(final Level level, final double x, final double y, final double z,
            final float scale, final float red, final float green, final float blue,
            final RandomSource rand) {
        return spawn(level, TcFxPlans.wispFx(x, y, z, scale, red, green, blue, rand));
    }

    /** {@code ClientProxy.wispFX2} (ClientProxy.java:705-710). */
    public static int wispFx2(final Level level, final double x, final double y, final double z,
            final float size, final int type, final boolean shrink, final boolean clip,
            final double gravity, final RandomSource rand) {
        return spawn(level, TcFxPlans.wispFx2(x, y, z, size, type, shrink, clip, gravity, rand));
    }

    /** {@code ClientProxy.wispFX3} (ClientProxy.java:719-724). */
    public static int wispFx3(final Level level, final double x, final double y, final double z,
            final double tx, final double ty, final double tz, final float size, final int type,
            final boolean shrink, final double gravity, final RandomSource rand) {
        return spawn(level, TcFxPlans.wispFx3(x, y, z, tx, ty, tz, size, type, shrink, gravity, rand));
    }

    /** {@code ClientProxy.wispFX4} (ClientProxy.java:726-731). */
    public static int wispFx4(final Level level, final double x, final double y, final double z,
            final int targetEntityId, final int type, final boolean shrink,
            final double gravity, final RandomSource rand) {
        return spawn(level, TcFxPlans.wispFx4(x, y, z, targetEntityId, type, shrink, gravity, rand));
    }

    /** {@code ClientProxy.wispFXEG} (ClientProxy.java:712-716). */
    public static int wispFxEg(final Level level, final double x, final double y, final double z,
            final int targetEntityId, final int particleSetting, final RandomSource rand) {
        return spawn(level, TcFxPlans.wispFxEg(x, y, z, targetEntityId, particleSetting, rand));
    }

    /** {@code ClientProxy.sourceStreamFX} (ClientProxy.java:777-781). */
    public static int sourceStream(final Level level, final double sx, final double sy,
            final double sz, final double tx, final double ty, final double tz,
            final int tagColor, final RandomSource rand) {
        return spawn(level, TcFxPlans.sourceStream(sx, sy, sz, tx, ty, tz, tagColor, rand));
    }

    /** {@code PacketFXVisDrain.onMessage} (PacketFXVisDrain.java:44-58). */
    public static int visDrain(final Level level, final double sx, final double sy, final double sz,
            final double tx, final double ty, final double tz, final int color,
            final RandomSource rand) {
        return spawn(level, TcFxPlans.visDrain(sx, sy, sz, tx, ty, tz, color, rand));
    }

    /**
     * {@code PacketFXBlockDig.onMessage} (PacketFXBlockDig.java:61-116): the
     * crumb burst and the dig-face sound — the original plays the block's
     * step sound at the dig-face centre (volume {@code (v+1)/2}, pitch
     * {@code p*0.8}), and the block's place sound at the target centre with
     * volume {@code (v+3)/4} (the item branch plays dirt's sounds).
     */
    public static int blockDig(final Level level, final BlockPos target, final BlockPos digFrom,
            final BlockState state, final ItemStack stack, final RandomSource rand,
            final int particleSetting) {
        final int spawned = spawn(level, TcFxPlans.blockDig(target, digFrom, state, stack, rand,
                particleSetting));
        final SoundType sound = state == null
                ? net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState().getSoundType()
                : state.getSoundType();
        level.playLocalSound(digFrom.getX() + 0.5, digFrom.getY() + 0.5, digFrom.getZ() + 0.5,
                sound.getBreakSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F, false);
        level.playLocalSound(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 3.0F) / 4.0F, sound.getPitch(), false);
        return spawned;
    }

    /** {@code PacketFXShield.onMessage}: spawn one rune (see {@link TcFxPlans#shieldRune}). */
    public static int shieldRune(final Level level, final int targetEntityId, final float yaw,
            final float pitch, final boolean mob) {
        return spawn(level, TcFxPlans.shieldRune(targetEntityId, yaw, pitch, mob));
    }

    /** {@code PacketFXSonic.onMessage}: spawn one pulse (see {@link TcFxPlans#sonicPulse}). */
    public static int sonicPulse(final Level level, final int targetEntityId) {
        return spawn(level, TcFxPlans.sonicPulse(targetEntityId));
    }

    /**
     * {@code Thaumcraft.proxy.bolt} (PacketFXZap.java:44-62,
     * PacketFXWispZap.java:41-46): the zap / wisp-zap bolt between two
     * entities. The 1.7.10 effect is a fractal line strip drawn by a world
     * renderer ({@code FXLightningBolt.defaultFractal/finalizeBolt}), so it is
     * not a {@code Level#addParticle} hand-off: the geometry travels as a
     * {@link TcBoltPlan} and the beam/bolt lane (TC-M7-02-b-s) renders it.
     * This is the seam the sender calls; it returns the plan unchanged.
     */
    public static TcBoltPlan bolt(final Level level, final TcBoltPlan plan) {
        return plan;
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
