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
 * Payload of the ported fractal lightning bolt (TC-M7-02-b-s).
 *
 * <p>The original's {@code ClientProxy.nodeBolt} handed the bolt eight
 * positional constructor arguments (start triple, end triple, a seed from
 * {@code world.rand.nextLong()}, duration 10, multiplier 4.0, speed 5 —
 * ClientProxy.java:789-796) and {@code FXLightningBolt} rebuilt the fractal
 * client-side from that seed. 1.21.1 particles are registry data, so the same
 * values become this codec-backed record. The spawn position carries the
 * start triple; the duration/multiplier/speed are sender constants
 * ({@link TcFxPlans#BOLT_DURATION}, {@link TcFxPlans#BOLT_MULTIPLIER},
 * {@link TcFxPlans#BOLT_INCREMENT}) because every in-scope sender passes the
 * same triple. {@code pass} selects the render pass of the original's
 * two-pass {@code getRenderPass() == 2} draw: 0 = the {@code p_large.png}
 * body glow, 1 = the {@code p_small.png} core.</p>
 */
public record TcBoltOptions(double endX, double endY, double endZ, long seed, int color, int pass)
        implements ParticleOptions {

    public static final MapCodec<TcBoltOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.DOUBLE.fieldOf("end_x").forGetter(TcBoltOptions::endX),
                    Codec.DOUBLE.fieldOf("end_y").forGetter(TcBoltOptions::endY),
                    Codec.DOUBLE.fieldOf("end_z").forGetter(TcBoltOptions::endZ),
                    Codec.LONG.fieldOf("seed").forGetter(TcBoltOptions::seed),
                    Codec.INT.fieldOf("color").forGetter(TcBoltOptions::color),
                    Codec.INT.fieldOf("pass").forGetter(TcBoltOptions::pass))
                    .apply(instance, TcBoltOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcBoltOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE, TcBoltOptions::endX,
                    ByteBufCodecs.DOUBLE, TcBoltOptions::endY,
                    ByteBufCodecs.DOUBLE, TcBoltOptions::endZ,
                    ByteBufCodecs.fromCodecWithRegistries(Codec.LONG), TcBoltOptions::seed,
                    ByteBufCodecs.INT, TcBoltOptions::color,
                    ByteBufCodecs.VAR_INT, TcBoltOptions::pass,
                    TcBoltOptions::new);

    @Override
    public ParticleType<?> getType() {
        return ModParticles.BOLT.get();
    }
}
