package com.jonnyutah12.thaumcraftclassic.client.fx;

import com.jonnyutah12.thaumcraftclassic.registration.ModParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Payload of the ported ward-hexagram particle (TC-M7-02-c-s-f4).
 *
 * <p>The original is {@code FXBlockWard}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/other/FXBlockWard.java).
 * Its constructor — the whole of its state — is a lifetime roll, a scale roll,
 * a rotation roll, a hit-point jitter, and the hit face, in that order
 * (FXBlockWard.java:21-59). Every one of those rides this record so a spawn
 * reaches the client with the values the 1.7.10 constructor received; the
 * rolls themselves stay plan-side in {@link TcFxPlans} /
 * {@link FxWardRules}, which is what keeps them testable on a dedicated
 * server.</p>
 *
 * <p>The hit face is the original's {@code EnumFacing side} parameter
 * ({@code BlockWarded.addHitEffects} / {@code BlockCosmeticOpaque.addHitEffects}
 * pass {@code hitVec.getDirection()}). It rides as a 1.21.1
 * {@link Direction#get3DDataValue() 3D data value} — the vanilla
 * serialisation convention for a direction, stable 0..5 — because a codec
 * cannot name the six constants without an enum codec and the wire stays
 * one var int.</p>
 *
 * @param side     the hit face as a {@link Direction} 3D data value (0..5)
 * @param rotation {@code rand.nextInt(360)} degrees about the face normal
 *                 (FXBlockWard.java:31)
 * @param scale    {@code 1.4F + nextGaussian() * 0.3F} (FXBlockWard.java:29);
 *                 the draw's quad half-extent is {@code scale / 2}
 * @param lifetime {@code 12 + rand.nextInt(5)} (FXBlockWard.java:27)
 */
public record TcWardOptions(int side, float rotation, float scale, int lifetime)
        implements ParticleOptions {

    public static final MapCodec<TcWardOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("side").forGetter(TcWardOptions::side),
                    Codec.FLOAT.fieldOf("rotation").forGetter(TcWardOptions::rotation),
                    Codec.FLOAT.fieldOf("scale").forGetter(TcWardOptions::scale),
                    Codec.INT.fieldOf("lifetime").forGetter(TcWardOptions::lifetime))
                    .apply(instance, TcWardOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TcWardOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TcWardOptions::side,
                    ByteBufCodecs.FLOAT, TcWardOptions::rotation,
                    ByteBufCodecs.FLOAT, TcWardOptions::scale,
                    ByteBufCodecs.VAR_INT, TcWardOptions::lifetime,
                    TcWardOptions::new);

    /** The hit face, clamped to the six directions (the 3D data convention). */
    public Direction direction() {
        final Direction[] byData = Direction.values();
        return byData[Math.max(0, Math.min(byData.length - 1, this.side))];
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.WARD.get();
    }
}
