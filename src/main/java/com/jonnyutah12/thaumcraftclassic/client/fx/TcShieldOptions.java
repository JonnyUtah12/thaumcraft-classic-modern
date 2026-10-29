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
 * The spawn payload of one shield-rune effect (TC-M7-02-c-s) —
 * {@code ClientEffectHooks.fxShield} → {@code FXShieldRunes}. One payload
 * shape serves the two particle types: {@code thaumcraft:shield_runes}
 * rides the hemis frame set (the original's non-mob branch, hemis1..15) and
 * {@code thaumcraft:shield_runes_mob} the ripple frame set (the
 * {@code EntityMob} branch, ripple1..15) — the original picked the
 * hemisphere's texture at render time; 1.21.1 particles bind a frame set at
 * registration, so the branch becomes two types.
 *
 * <p>The payload carries the ctor's entity-riding parameters: the target
 * id, the yaw/pitch the packet handler computed (atan2 toward the defender,
 * the −1/−2/−3 branches' fixed 90°/270°), and the age jitter
 * {@code age + rand.nextInt(age / 2)} (FXShieldRunes.java:31, age base 8 →
 * 8..11), drawn by {@link TcFxPlans#shieldRunes}.</p>
 */
public record TcShieldOptions(int targetEntityId, float yaw, float pitch,
        int lifetime, boolean mob) implements ParticleOptions {

    public static final MapCodec<TcShieldOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("target").forGetter(TcShieldOptions::targetEntityId),
                    Codec.FLOAT.fieldOf("yaw").forGetter(TcShieldOptions::yaw),
                    Codec.FLOAT.fieldOf("pitch").forGetter(TcShieldOptions::pitch),
                    Codec.INT.fieldOf("lifetime").forGetter(TcShieldOptions::lifetime),
                    Codec.BOOL.fieldOf("mob").forGetter(TcShieldOptions::mob))
                    .apply(instance, TcShieldOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcShieldOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TcShieldOptions::targetEntityId,
                    ByteBufCodecs.FLOAT, TcShieldOptions::yaw,
                    ByteBufCodecs.FLOAT, TcShieldOptions::pitch,
                    ByteBufCodecs.VAR_INT, TcShieldOptions::lifetime,
                    ByteBufCodecs.BOOL, TcShieldOptions::mob,
                    TcShieldOptions::new);

    /** The packet handler's base age (PacketFXShield.java:60). */
    public static final int AGE_BASE = 8;

    @Override
    public ParticleType<?> getType() {
        return mob ? ModParticles.SHIELD_RUNES_MOB.get() : ModParticles.SHIELD_RUNES.get();
    }
}
