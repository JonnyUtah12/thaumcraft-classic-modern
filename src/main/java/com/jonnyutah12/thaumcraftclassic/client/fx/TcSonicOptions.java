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
 * The spawn payload of one {@code thaumcraft:sonic} effect
 * (TC-M7-02-c-s) — {@code ClientEffectHooks.fxSonic} → {@code FXSonic},
 * the sonicshock/sonicoo pulse that rides a mob's head.
 *
 * <p>The payload carries the ctor's entity-riding state: the target id and
 * the age jitter {@code age + rand.nextInt(age / 2)} (FXSonic.java:29, base
 * 10 from the packet handler → 10..14), drawn by {@link TcFxPlans#sonic}.
 * The ctor's yaw/pitch are read from the entity itself every render
 * (FXSonic.java:26-27) — they are not packet data — so they stay on the
 * particle side; the payload only carries what the packet carries.</p>
 */
public record TcSonicOptions(int targetEntityId, int lifetime) implements ParticleOptions {

    public static final MapCodec<TcSonicOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("target").forGetter(TcSonicOptions::targetEntityId),
                    Codec.INT.fieldOf("lifetime").forGetter(TcSonicOptions::lifetime))
                    .apply(instance, TcSonicOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcSonicOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TcSonicOptions::targetEntityId,
                    ByteBufCodecs.VAR_INT, TcSonicOptions::lifetime,
                    TcSonicOptions::new);

    /** The packet handler's base age (PacketFXSonic.java:36). */
    public static final int AGE_BASE = 10;

    @Override
    public ParticleType<?> getType() {
        return ModParticles.SONIC.get();
    }
}
