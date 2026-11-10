package com.jonnyutah12.thaumcraftclassic.client.fx;

import com.jonnyutah12.thaumcraftclassic.registration.ModParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The spawn payload of one {@code thaumcraft:vent} puff
 * (TC-M7-02-c-s-f5) — the vent/steam puff of
 * {@code ClientProxy.drawVentParticles} (client/ClientProxy.java:1216-1219 and
 * the scaled overload 1254-1258) → {@code FXVent}.
 *
 * <p>The ctor normalises the throw direction, adds a per-axis
 * {@code nextGaussian() * ±boolean * 0.0075 * spread} jitter, and scales by
 * {@code 0.125} (FXVent.java:52-66 — {@code setHeading(dir, 0.125F, 5.0F)}).
 * Those draws belong to {@link TcFxPlans#vent}, which is the sender's line, so
 * the payload carries the resolved velocity as the spawn's velocity arguments
 * plus the ctor's own scale roll {@code rand*0.1 + 0.05}, the sender's colour,
 * its alpha (the sender's {@code func_82338_g(0.4F)}) and its
 * {@code setScale} factor, which multiplies both the scale and the shrink
 * ceiling {@code psm} (FXVent.java:47-50).</p>
 */
public record TcVentOptions(int color, float scale, float shrinkCeiling,
        float alpha) implements ParticleOptions {

    public static final MapCodec<TcVentOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("color").forGetter(TcVentOptions::color),
                    Codec.FLOAT.fieldOf("scale").forGetter(TcVentOptions::scale),
                    Codec.FLOAT.fieldOf("shrink_ceiling").forGetter(TcVentOptions::shrinkCeiling),
                    Codec.FLOAT.fieldOf("alpha").forGetter(TcVentOptions::alpha))
                    .apply(instance, TcVentOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcVentOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, TcVentOptions::color,
                    ByteBufCodecs.FLOAT, TcVentOptions::scale,
                    ByteBufCodecs.FLOAT, TcVentOptions::shrinkCeiling,
                    ByteBufCodecs.FLOAT, TcVentOptions::alpha,
                    TcVentOptions::new);

    /** {@code particleScale = rand*0.1 + 0.05} (FXVent.java:21). */
    public static final float SCALE_BASE = 0.05F;
    public static final float SCALE_SPAN = 0.1F;

    /** {@code psm} starts at 1.0 and {@code setScale(f)} multiplies both (lines 15, 48-51). */
    public static final float PSM_DEFAULT = 1.0F;

    /** The sender's alpha: {@code func_82338_g(0.4F)} (ClientProxy.java:1217, 1255). */
    public static final float SENDER_ALPHA = 0.4F;

    /** The heading step and spread of {@code setHeading(..., 0.125F, 5.0F)} (line 32). */
    public static final double HEADING_STEP = 0.125;
    public static final double HEADING_SPREAD = 5.0;

    /** The gaussian spread factor of {@code setHeading} (FXVent.java:56-58). */
    public static final double HEADING_JITTER = 0.0075;

    /** {@code setParticleBounds(0.02F, 0.02F)} (line 14). */
    public static final float BOUND = 0.02F;

    /**
     * The lifetime is the 1.7.10 {@code EntityFX} default of 40 ticks — the ctor
     * never touches {@code particleMaxAge}; it only zeroes it when the player is
     * farther than the view gate (FXVent.java:31).
     */
    public static final int LIFETIME = 40;

    /** The per-tick rise {@code motionY += 0.0025} (line 73). */
    public static final double RISE = 0.0025;

    /** The per-tick drag 0.85 (lines 75-77). */
    public static final double DRAG = 0.85;

    /** The growth step {@code scale *= 1.15} while under the ceiling (lines 78-80). */
    public static final double GROWTH = 1.15;

    /** The on-ground horizontal drag 0.7 (lines 81-84). */
    public static final double GROUND_DRAG = 0.7;

    /** The render's billboard factor {@code 0.3F * particleScale} (line 96). */
    public static final float SIZE_FACTOR = 0.3F;

    /** The frame ramp {@code 1 + scale/psm * 4} → 5 frames (line 95). */
    public static final int FRAMES = 5;

    /** The view-distance gate 50 / 25 (lines 34-41). */
    public static final int VIEW_FAR = 50;
    public static final int VIEW_NEAR = 25;

    @Override
    public ParticleType<?> getType() {
        return ModParticles.VENT.get();
    }
}
