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
 * The spawn payload of one {@code thaumcraft:vis_sparkle} particle
 * (TC-M7-02-c-s) — the vis-drain mote ({@code ClientEffectHooks.fxVisDrain}
 * → {@code FXVisSparkle}).
 *
 * <p>The payload carries the two values {@code FXVisSparkle} settles before
 * the tick loop: the target point and {@code sizeMod = 45 + rand.nextInt(15)}
 * (FXVisSparkle.java:28), both drawn by {@link TcFxPlans#visDrain} in the
 * ctor's draw order (3 gaussian velocity draws, sizeMod, the green-roll
 * draw that {@code setColor} then overwrites). The ctor's fixed numbers —
 * 1000-tick cap, 0.985 drag, 0.1 attraction clamped to ±0.1, no-clip —
 * live as constants on the particle class.</p>
 */
public record TcVisSparkleOptions(int color, double targetX, double targetY,
        double targetZ, int sizeMod) implements ParticleOptions {

    public static final MapCodec<TcVisSparkleOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("color").forGetter(TcVisSparkleOptions::color),
                    Codec.DOUBLE.fieldOf("target_x").forGetter(TcVisSparkleOptions::targetX),
                    Codec.DOUBLE.fieldOf("target_y").forGetter(TcVisSparkleOptions::targetY),
                    Codec.DOUBLE.fieldOf("target_z").forGetter(TcVisSparkleOptions::targetZ),
                    Codec.INT.fieldOf("size_mod").forGetter(TcVisSparkleOptions::sizeMod))
                    .apply(instance, TcVisSparkleOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcVisSparkleOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, TcVisSparkleOptions::color,
                    ByteBufCodecs.DOUBLE, TcVisSparkleOptions::targetX,
                    ByteBufCodecs.DOUBLE, TcVisSparkleOptions::targetY,
                    ByteBufCodecs.DOUBLE, TcVisSparkleOptions::targetZ,
                    ByteBufCodecs.VAR_INT, TcVisSparkleOptions::sizeMod,
                    TcVisSparkleOptions::new);

    @Override
    public ParticleType<?> getType() {
        return ModParticles.VIS_SPARKLE.get();
    }
}
