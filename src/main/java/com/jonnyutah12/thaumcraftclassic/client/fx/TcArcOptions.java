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
 * Payload of the ported block arc (TC-M7-02-b-s).
 *
 * <p>{@code PacketFXBlockZap("arc")} / {@code EntityGolemBuilding} built an
 * {@code FXArc} at the source entity's centre aimed at
 * {@code (pos + 0.5, pos.getY() + 1, pos + 0.5)} with {@code heightGain 0.5}
 * (PacketFXBlockArc.java:53, ClientProxy.java:624-638, FXArc.java:36-39). The
 * arc's point chain is built from a random stream in the original's
 * constructor, so the payload carries the deltas, the height gain and a seed
 * that pins the jitter — same seed, same arc (TcArcGeometry).</p>
 */
public record TcArcOptions(double dx, double dy, double dz, float heightGain, int color, long seed)
        implements ParticleOptions {

    public static final MapCodec<TcArcOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.DOUBLE.fieldOf("dx").forGetter(TcArcOptions::dx),
                    Codec.DOUBLE.fieldOf("dy").forGetter(TcArcOptions::dy),
                    Codec.DOUBLE.fieldOf("dz").forGetter(TcArcOptions::dz),
                    Codec.FLOAT.fieldOf("height_gain").forGetter(TcArcOptions::heightGain),
                    Codec.INT.fieldOf("color").forGetter(TcArcOptions::color),
                    Codec.LONG.fieldOf("seed").forGetter(TcArcOptions::seed))
                    .apply(instance, TcArcOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcArcOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE, TcArcOptions::dx,
                    ByteBufCodecs.DOUBLE, TcArcOptions::dy,
                    ByteBufCodecs.DOUBLE, TcArcOptions::dz,
                    ByteBufCodecs.FLOAT, TcArcOptions::heightGain,
                    ByteBufCodecs.INT, TcArcOptions::color,
                    ByteBufCodecs.fromCodecWithRegistries(Codec.LONG), TcArcOptions::seed,
                    TcArcOptions::new);

    @Override
    public ParticleType<?> getType() {
        return ModParticles.ARC.get();
    }
}
