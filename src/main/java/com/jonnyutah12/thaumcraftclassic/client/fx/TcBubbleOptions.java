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
 * Payload of the ported crucible bubble particle (TC-M7-02-s).
 *
 * <p>The original's shape is {@code FXBubble}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXBubble.java):
 * a coloured bubble that gains {@code bubblespeed} on its Y axis every tick
 * (0.002 default, line 12), wobbles on X/Z, drags every axis by 0.85, counts
 * its life down from {@code age + 2 + 8 / (rand * 0.8 + 0.2)} (line 25) and
 * swaps to the pop frame for its last two ticks (lines 63-65). The
 * {@code crucibleBubble} sender passes {@code age = 1}
 * (ClientProxy.java:682-686) and the aspect colour, so those values ride this
 * record instead of constructor arguments.</p>
 */
public record TcBubbleOptions(int color, float scale, double rise, int lifetime) implements ParticleOptions {

    public static final MapCodec<TcBubbleOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("color").forGetter(TcBubbleOptions::color),
                    Codec.FLOAT.fieldOf("scale").forGetter(TcBubbleOptions::scale),
                    Codec.DOUBLE.fieldOf("rise").forGetter(TcBubbleOptions::rise),
                    Codec.INT.fieldOf("lifetime").forGetter(TcBubbleOptions::lifetime))
                    .apply(instance, TcBubbleOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcBubbleOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, TcBubbleOptions::color,
                    ByteBufCodecs.FLOAT, TcBubbleOptions::scale,
                    ByteBufCodecs.DOUBLE, TcBubbleOptions::rise,
                    ByteBufCodecs.VAR_INT, TcBubbleOptions::lifetime,
                    TcBubbleOptions::new);

    /** The original {@code FXBubble.bubblespeed} default (FxBubble.java:12). */
    public static final double DEFAULT_RISE = 0.002;

    /** The original swaps to the pop frame while {@code lifeLeft <= 2}. */
    public static final int POP_TICKS = 2;

    @Override
    public ParticleType<?> getType() {
        return ModParticles.BUBBLE.get();
    }
}
