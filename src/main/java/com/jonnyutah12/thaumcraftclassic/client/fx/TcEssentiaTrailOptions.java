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
 * The spawn payload of one {@code thaumcraft:essentia_trail} particle
 * (TC-M7-02-c-s) — the essentia drain trail ({@code ClientProxy.
 * fxEssentiaSource}'s per-tick emitter → {@code FXEssentiaTrail}).
 *
 * <p>{@code FXEssentiaTrail} derives everything from the ctor arguments
 * (start, target, {@code count}, colour, scale): the scale wobble
 * {@code (sin(count/2)*0.1 + 1) * scale}, the sin-driven launch velocity
 * (vx/vz around ±0.015, vy = 0.1 + sin(count/3)*0.01), the
 * {@code base/2 + rand(base)} lifetime with {@code base = max(1, (int)(dist * 30))},
 * and the colour jitter {@code c - m + rand*m} per channel (m = channel*0.2)
 * (FXEssentiaTrail.java:19-45). Every draw lands in
 * {@link TcFxPlans#essentiaTrailTick}; the payload carries the settled
 * values plus {@code count} — the client tick count the original emitted
 * this mote at, whose sine phases are part of the spawn geometry.</p>
 */
public record TcEssentiaTrailOptions(int color, double targetX, double targetY,
        double targetZ, int count, float scale, int lifetime) implements ParticleOptions {

    public static final MapCodec<TcEssentiaTrailOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("color").forGetter(TcEssentiaTrailOptions::color),
                    Codec.DOUBLE.fieldOf("target_x").forGetter(TcEssentiaTrailOptions::targetX),
                    Codec.DOUBLE.fieldOf("target_y").forGetter(TcEssentiaTrailOptions::targetY),
                    Codec.DOUBLE.fieldOf("target_z").forGetter(TcEssentiaTrailOptions::targetZ),
                    Codec.INT.fieldOf("count").forGetter(TcEssentiaTrailOptions::count),
                    Codec.FLOAT.fieldOf("scale").forGetter(TcEssentiaTrailOptions::scale),
                    Codec.INT.fieldOf("lifetime").forGetter(TcEssentiaTrailOptions::lifetime))
                    .apply(instance, TcEssentiaTrailOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcEssentiaTrailOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, TcEssentiaTrailOptions::color,
                    ByteBufCodecs.DOUBLE, TcEssentiaTrailOptions::targetX,
                    ByteBufCodecs.DOUBLE, TcEssentiaTrailOptions::targetY,
                    ByteBufCodecs.DOUBLE, TcEssentiaTrailOptions::targetZ,
                    ByteBufCodecs.VAR_INT, TcEssentiaTrailOptions::count,
                    ByteBufCodecs.FLOAT, TcEssentiaTrailOptions::scale,
                    ByteBufCodecs.VAR_INT, TcEssentiaTrailOptions::lifetime,
                    TcEssentiaTrailOptions::new);

    /** The ctor's lifetime distance factor {@code (int)(dist * 30.0F)} (line 28). */
    public static final double LIFETIME_DIST_FACTOR = 30.0;

    /** The per-tick upward acceleration {@code yd += 0.01 * 0.2} (lines 45, 55). */
    public static final double LIFT = 0.01 * 0.2;

    @Override
    public ParticleType<?> getType() {
        return ModParticles.ESSENTIA_TRAIL.get();
    }
}
