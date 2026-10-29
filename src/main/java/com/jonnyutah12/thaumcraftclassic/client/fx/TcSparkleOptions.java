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
 * Payload of the ported generic sparkle particle (TC-M7-02-s).
 *
 * <p>The original spawns this shape through
 * {@code ClientProxy.drawGenericParticles} → {@code FXGeneric}
 * (reference/decompiled/thaumcraft/thaumcraft/client/ClientProxy.java:1222-1250),
 * whose setters take exactly these five values: the RGB colour
 * ({@code func_70538_b}), the alpha (0.9 for every sparkle sender), the sprite
 * scale ({@code setScale}), the age ({@code setMaxAge(max, delay)}) and the
 * render delay. The original carried them as positional arguments to a
 * constructor; 1.21.1 particles are registered data, so the same five values
 * become a codec-backed {@link ParticleOptions} record.</p>
 *
 * <p>The sprite frame range is deliberately NOT part of the payload: the
 * original's {@code startParticle}/{@code numParticles}/{@code particleInc}
 * (112/9/1 for every sparkle call) index a 16×16 cell grid inside
 * {@code textures/misc/particles.png}, and in 1.21.1 the frame list belongs to
 * the particle definition
 * ({@code assets/thaumcraft/particles/sparkle.json}), so the frames come from
 * the particle's {@code SpriteSet}.</p>
 */
public record TcSparkleOptions(int color, float alpha, float scale, int age, int delay) implements ParticleOptions {

    public static final MapCodec<TcSparkleOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("color").forGetter(TcSparkleOptions::color),
                    Codec.FLOAT.fieldOf("alpha").forGetter(TcSparkleOptions::alpha),
                    Codec.FLOAT.fieldOf("scale").forGetter(TcSparkleOptions::scale),
                    Codec.INT.fieldOf("age").forGetter(TcSparkleOptions::age),
                    Codec.INT.fieldOf("delay").forGetter(TcSparkleOptions::delay))
                    .apply(instance, TcSparkleOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcSparkleOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, TcSparkleOptions::color,
                    ByteBufCodecs.FLOAT, TcSparkleOptions::alpha,
                    ByteBufCodecs.FLOAT, TcSparkleOptions::scale,
                    ByteBufCodecs.VAR_INT, TcSparkleOptions::age,
                    ByteBufCodecs.VAR_INT, TcSparkleOptions::delay,
                    TcSparkleOptions::new);

    @Override
    public ParticleType<?> getType() {
        return ModParticles.SPARCLE.get();
    }
}
