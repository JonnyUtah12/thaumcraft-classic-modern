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
 * Payload of the arc's companion glow sparkle (TC-M7-02-b-s).
 *
 * <p>{@code ClientProxy.arcLightning} added an {@code FXSparkle} of type 6
 * (addVelocity = false) at the arc's target point before drawing the arc
 * (ClientProxy.java:627-631). That sparkle is the vanilla particle-sheet
 * sprite, so it ports as a colour-tinted sparkle: {@code particleScale 3.0},
 * {@code particleAlpha 0.75}, multiplier 2 (frame = (age / 2) % 4), shrink on,
 * 4 frames, fullbright 240, normal alpha blend (FXSparkle.java:23-31, 44, 132-158;
 * docs/BEHAVIORAL_DEVIATIONS.md BD-16). The packet's colour draws decide the
 * tint; the spawn plan owns the draws (TcFxPlans.blockArc).</p>
 */
public record TcGlowSparkleOptions(int color, float scale, int age) implements ParticleOptions {

    public static final MapCodec<TcGlowSparkleOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("color").forGetter(TcGlowSparkleOptions::color),
                    Codec.FLOAT.fieldOf("scale").forGetter(TcGlowSparkleOptions::scale),
                    Codec.INT.fieldOf("age").forGetter(TcGlowSparkleOptions::age))
                    .apply(instance, TcGlowSparkleOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcGlowSparkleOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, TcGlowSparkleOptions::color,
                    ByteBufCodecs.FLOAT, TcGlowSparkleOptions::scale,
                    ByteBufCodecs.VAR_INT, TcGlowSparkleOptions::age,
                    TcGlowSparkleOptions::new);

    @Override
    public ParticleType<?> getType() {
        return ModParticles.SPARKLE_GLOW.get();
    }
}
