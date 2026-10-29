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
 * Payload of the ported entity-targeted beam (TC-M7-02-b-s).
 *
 * <p>{@code PacketFXBeamPulse} built an {@code FXBeam} at the source entity's
 * eye position, aimed at a live target entity, with colour from the packet,
 * {@code field_70130_N = 2.5}, type 1, reverse, pulse, age 20
 * (PacketFXBeamPulse.java:38-58). The target travels while the beam lives, so
 * the payload carries the target's entity id and the particle re-reads its
 * position every tick exactly like {@code FXBeam.onUpdate}
 * (FXBeam.java:118-124).</p>
 *
 * <p>{@code endMod = 1} and {@code rotationspeed = 5} are the original's
 * field defaults and every in-scope sender leaves them alone, so they are
 * particle-side constants; {@code pulse} is set by the sender
 * ({@code setPulse(true)}) and carried implicitly by the pulse ramp
 * (docs/BEHAVIORAL_DEVIATIONS.md BD-16).</p>
 */
public record TcBeamOptions(int targetEntityId, int color, float size, int type, boolean reverse, int age)
        implements ParticleOptions {

    public static final MapCodec<TcBeamOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("target").forGetter(TcBeamOptions::targetEntityId),
                    Codec.INT.fieldOf("color").forGetter(TcBeamOptions::color),
                    Codec.FLOAT.fieldOf("size").forGetter(TcBeamOptions::size),
                    Codec.INT.fieldOf("type").forGetter(TcBeamOptions::type),
                    Codec.BOOL.fieldOf("reverse").forGetter(TcBeamOptions::reverse),
                    Codec.INT.fieldOf("age").forGetter(TcBeamOptions::age))
                    .apply(instance, TcBeamOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcBeamOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TcBeamOptions::targetEntityId,
                    ByteBufCodecs.INT, TcBeamOptions::color,
                    ByteBufCodecs.FLOAT, TcBeamOptions::size,
                    ByteBufCodecs.VAR_INT, TcBeamOptions::type,
                    ByteBufCodecs.BOOL, TcBeamOptions::reverse,
                    ByteBufCodecs.VAR_INT, TcBeamOptions::age,
                    TcBeamOptions::new);

    @Override
    public ParticleType<?> getType() {
        return ModParticles.BEAM.get();
    }
}
