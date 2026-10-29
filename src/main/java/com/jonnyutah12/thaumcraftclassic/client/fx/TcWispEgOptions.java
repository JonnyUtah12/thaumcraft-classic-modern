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
 * The spawn payload of one {@code thaumcraft:wisp_eg} particle (TC-M7-02-c-s).
 *
 * <p>{@code FXWispEG} (the EnderGoo wisp the wisp-sender's "eg" branch fires
 * at an entity) draws everything at construction time and carries the target
 * entity's velocity while airborne, so the only value the plan hands the
 * particle is the target's id — everything else (velocity, lifetime, colour,
 * additive blend) is drawn by {@link TcFxPlans#wispEg} in the original's
 * draw order.</p>
 */
public record TcWispEgOptions(int targetEntityId, int lifetime, int color)
        implements ParticleOptions {

    public static final MapCodec<TcWispEgOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("target").forGetter(TcWispEgOptions::targetEntityId),
                    Codec.INT.fieldOf("lifetime").forGetter(TcWispEgOptions::lifetime),
                    Codec.INT.fieldOf("color").forGetter(TcWispEgOptions::color))
                    .apply(instance, TcWispEgOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcWispEgOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TcWispEgOptions::targetEntityId,
                    ByteBufCodecs.VAR_INT, TcWispEgOptions::lifetime,
                    ByteBufCodecs.INT, TcWispEgOptions::color,
                    TcWispEgOptions::new);

    /** {@code FXWispEG.java:26}: {@code (int)(40.0 / (Math.random() * 0.3 + 0.7))}. */
    public static final int LIFETIME_BASE = 40;

    /** The ctor's {@code particleScale *= 0.4} (FXWispEG.java:25). */
    public static final float SCALE = 0.4F;

    /** The ctor's fixed fall speed {@code motionY = -0.05} (FXWispEG.java:23). */
    public static final double FALL = -0.05;

    /** The ctor's lateral {@code nextGaussian() * 0.03} (FXWispEG.java:22,24). */
    public static final double LATERAL = 0.03;

    @Override
    public ParticleType<?> getType() {
        return ModParticles.WISP_EG.get();
    }
}
