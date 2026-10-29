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
 * Payload of the ported golem-boss beam pair (TC-M7-02-b-s).
 *
 * <p>{@code PacketFXBeamPulseGolemBoss} built two {@code FXBeamGolemBoss}
 * beams from the boss's head anchor: colour (0.07, 0.376, 0.325),
 * {@code field_70130_N = 3.0} type 2, and {@code N = 1.5} type 1, both
 * {@code setPulse(true)}, age 20 (PacketFXBeamPulseGolemBoss.java:38-60).
 * The payload carries both entity ids: the particle anchors its start on the
 * boss's head each tick (boss yaw/pitch/eye height, TcBeamGeometry.headAnchor)
 * and its impact sprite on the target, like {@code FXBeamGolemBoss.onUpdate}
 * (FXBeamGolemBoss.java:84-103). Colour arrives packed as 0x00RRGGBB.</p>
 */
public record TcGolemBeamOptions(int bossEntityId, int targetEntityId, int color, float size, int type, int age)
        implements ParticleOptions {

    public static final MapCodec<TcGolemBeamOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("boss").forGetter(TcGolemBeamOptions::bossEntityId),
                    Codec.INT.fieldOf("target").forGetter(TcGolemBeamOptions::targetEntityId),
                    Codec.INT.fieldOf("color").forGetter(TcGolemBeamOptions::color),
                    Codec.FLOAT.fieldOf("size").forGetter(TcGolemBeamOptions::size),
                    Codec.INT.fieldOf("type").forGetter(TcGolemBeamOptions::type),
                    Codec.INT.fieldOf("age").forGetter(TcGolemBeamOptions::age))
                    .apply(instance, TcGolemBeamOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcGolemBeamOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TcGolemBeamOptions::bossEntityId,
                    ByteBufCodecs.VAR_INT, TcGolemBeamOptions::targetEntityId,
                    ByteBufCodecs.INT, TcGolemBeamOptions::color,
                    ByteBufCodecs.FLOAT, TcGolemBeamOptions::size,
                    ByteBufCodecs.VAR_INT, TcGolemBeamOptions::type,
                    ByteBufCodecs.VAR_INT, TcGolemBeamOptions::age,
                    TcGolemBeamOptions::new);

    @Override
    public ParticleType<?> getType() {
        return ModParticles.BEAM_GOLEM_BOSS.get();
    }
}
