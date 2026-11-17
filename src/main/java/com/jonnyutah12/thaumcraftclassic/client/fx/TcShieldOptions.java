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
 * Payload of the ported shield-runes particle (TC-M7-02-c-s-f4).
 *
 * <p>The original is {@code FXShieldRunes}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/other/FXShieldRunes.java),
 * spawned by {@code PacketFXShield.onMessage}
 * (common/lib/network/fx/PacketFXShield.java:38-79). What the constructor
 * keeps is the entity it rides, the yaw/pitch it was aimed with, the size, the
 * lifetime roll and the mob branch — all six ride here, and the rolls stay
 * plan-side in {@link TcFxPlans} / {@link FxShieldRules}.</p>
 *
 * <p>The tracked entity is always the packet's <em>source</em>: every
 * {@code new FXShieldRunes(world, p.posX, p.posY, p.posZ, p, 8, yaw, pitch)}
 * hands in {@code p} as the entity (PacketFXShield.java:60-71). The packet's
 * {@code target} is not tracked at all — it only aims the yaw/pitch, and its
 * {@code -1 / -2 / -3} sentinels choose how many runes spawn and at what
 * pitch, so they are consumed plan-side and never reach this record.</p>
 *
 * <p>Two registered types share this record: the hemis form
 * ({@code thaumcraft:shield_runes}) and the mob's ripple form
 * ({@code thaumcraft:shield_runes_mob}), which the original switches on
 * {@code target instanceof EntityMob && !(target instanceof EntityCultist)}
 * inside its draw (FXShieldRunes.java:61-66). A 1.21.1 particle picks its
 * frame list and its tint at construction, so the branch becomes the
 * {@link #type()} switch below — the shape {@code TcWispOptions}'
 * {@code additive} flag documents as the lane's alternative, chosen here
 * because the frame list cannot ride a flag.</p>
 *
 * @param sourceEntityId the entity the runes ride: the packet's source
 *                       (FXShieldRunes.java:36-41, 92-94)
 * @param yaw            the aimed yaw, the original's {@code yaw} field
 *                       ({@code atan2(dz, dx)} degrees minus 90,
 *                       PacketFXShield.java:51)
 * @param pitch          the aimed pitch (the negated {@code atan2(dy, d3)}
 *                       degrees, PacketFXShield.java:52), or the sentinel
 *                       90 / 270 the {@code -1 / -2 / -3} branches pass
 * @param scale          {@code 0.4 * source.width} (FXShieldRunes.java:74)
 * @param lifetime       {@code 8 + rand.nextInt(8 / 2)} (FXShieldRunes.java:31)
 * @param mob            the original's mob branch: ripple frames and the 0.5
 *                       colour (FXShieldRunes.java:61-66)
 */
public record TcShieldOptions(int sourceEntityId, float yaw, float pitch, float scale,
        int lifetime, boolean mob) implements ParticleOptions {

    public static final MapCodec<TcShieldOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("source").forGetter(TcShieldOptions::sourceEntityId),
                    Codec.FLOAT.fieldOf("yaw").forGetter(TcShieldOptions::yaw),
                    Codec.FLOAT.fieldOf("pitch").forGetter(TcShieldOptions::pitch),
                    Codec.FLOAT.fieldOf("scale").forGetter(TcShieldOptions::scale),
                    Codec.INT.fieldOf("lifetime").forGetter(TcShieldOptions::lifetime),
                    Codec.BOOL.fieldOf("mob").forGetter(TcShieldOptions::mob))
                    .apply(instance, TcShieldOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TcShieldOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TcShieldOptions::sourceEntityId,
                    ByteBufCodecs.FLOAT, TcShieldOptions::yaw,
                    ByteBufCodecs.FLOAT, TcShieldOptions::pitch,
                    ByteBufCodecs.FLOAT, TcShieldOptions::scale,
                    ByteBufCodecs.VAR_INT, TcShieldOptions::lifetime,
                    ByteBufCodecs.BOOL, TcShieldOptions::mob,
                    TcShieldOptions::new);

    @Override
    public ParticleType<?> getType() {
        return this.mob ? ModParticles.SHIELD_RUNES_MOB.get() : ModParticles.SHIELD_RUNES.get();
    }
}
