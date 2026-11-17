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
 * The spawn payload of one {@code thaumcraft:bore_sparkle} particle
 * (TC-M7-02-c-s) — the infusion-matrix draw mote ({@code ClientProxy.
 * drawInfusionParticles3/4} → {@code FXBoreSparkle}).
 *
 * <p>{@link TcFxPlans} pre-draws the ctor's randoms in order (the
 * 4-frame-animating white puff's scale {@code rand*0.5+0.5}, the
 * distance-derived lifetime {@code base/2 + rand(base)} with
 * {@code base = max(1, (int)(dist * 3))}, the two gaussian velocity draws)
 * and multiplies the caller's {@code setColor} factors into the ctor's
 * (0.2, 0.6+rand*0.3, 0.2) rgb, exactly as the original ctor +
 * {@code setColor} sequence does.</p>
 */
public record TcBoreSparkleOptions(int color, double targetX, double targetY,
        double targetZ, int lifetime, float scale) implements ParticleOptions {

    public static final MapCodec<TcBoreSparkleOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("color").forGetter(TcBoreSparkleOptions::color),
                    Codec.DOUBLE.fieldOf("target_x").forGetter(TcBoreSparkleOptions::targetX),
                    Codec.DOUBLE.fieldOf("target_y").forGetter(TcBoreSparkleOptions::targetY),
                    Codec.DOUBLE.fieldOf("target_z").forGetter(TcBoreSparkleOptions::targetZ),
                    Codec.INT.fieldOf("lifetime").forGetter(TcBoreSparkleOptions::lifetime),
                    Codec.FLOAT.fieldOf("scale").forGetter(TcBoreSparkleOptions::scale))
                    .apply(instance, TcBoreSparkleOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcBoreSparkleOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, TcBoreSparkleOptions::color,
                    ByteBufCodecs.DOUBLE, TcBoreSparkleOptions::targetX,
                    ByteBufCodecs.DOUBLE, TcBoreSparkleOptions::targetY,
                    ByteBufCodecs.DOUBLE, TcBoreSparkleOptions::targetZ,
                    ByteBufCodecs.VAR_INT, TcBoreSparkleOptions::lifetime,
                    ByteBufCodecs.FLOAT, TcBoreSparkleOptions::scale,
                    TcBoreSparkleOptions::new);

    /** The ctor's {@code particleScale = nextFloat() * 0.5 + 0.5} (FXBoreSparkle.java:19). */
    public static final float SCALE_BASE = 0.5F;
    public static final float SCALE_SPAN = 0.5F;

    /** The ctor's lifetime: {@code base = max(1, (int)(dist * 3))} (line 24-27). */
    public static final double LIFETIME_DIST_FACTOR = 3.0;

    /** The ctor's gaussian velocity multiplier 0.01 (line 29-31). */
    public static final double START_VELOCITY = 0.01;

    @Override
    public ParticleType<?> getType() {
        return ModParticles.BORE_SPARKLE.get();
    }
}
