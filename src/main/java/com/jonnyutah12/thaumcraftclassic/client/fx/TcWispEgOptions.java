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
 * Payload of the ported EG (essentia-gas) wisp particle (TC-M7-02-c-s-f1).
 *
 * <p>The original's shape is {@code FXWispEG}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXWispEG.java):
 * a dark additive mote that falls out of the sky onto a target
 * ({@code yd = -0.05}, x/z from a {@code rand*0.03} gaussian), rides the
 * target's own motion while airborne, drags by 0.98 (0.85 x/z on ground),
 * fades by distance to the camera and steps through 13 frames of its sheet.
 * {@code ClientProxy.wispFXEG} (ClientProxy.java:720-726) spawns it
 * {@code particleCount(1)} times per call, so the burst count is the shared
 * particle-setting gate.</p>
 *
 * @param color          packed 0x00RRGGBB colour: the original's
 *                       {@code rand*0.05} per channel (FXWispEG.java:44-46)
 * @param lifetime       {@code (int)(40 / (rand*0.3 + 0.7))} (FXWispEG.java:32)
 * @param targetEntityId the entity the mote follows (FXWispEG.java:22), or
 *                       {@link TcWispOptions#NO_TARGET}
 */
public record TcWispEgOptions(int color, int lifetime, int targetEntityId) implements ParticleOptions {

    public static final MapCodec<TcWispEgOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("color").forGetter(TcWispEgOptions::color),
                    Codec.INT.fieldOf("lifetime").forGetter(TcWispEgOptions::lifetime),
                    Codec.INT.fieldOf("target").forGetter(TcWispEgOptions::targetEntityId))
                    .apply(instance, TcWispEgOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcWispEgOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, TcWispEgOptions::color,
                    ByteBufCodecs.VAR_INT, TcWispEgOptions::lifetime,
                    ByteBufCodecs.VAR_INT, TcWispEgOptions::targetEntityId,
                    TcWispEgOptions::new);

    @Override
    public ParticleType<?> getType() {
        return ModParticles.WISP_EG.get();
    }
}
