package com.jonnyutah12.thaumcraftclassic.client.fx;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * A data-carrying particle type (TC-M7-02-s).
 *
 * <p>1.21.1's {@link ParticleType} is abstract and asks for two views of the
 * payload: a {@link MapCodec} (the particle's JSON/network data shape) and a
 * {@link StreamCodec} (how the spawn reaches the client). Vanilla only ships a
 * concrete type for its argument-less particles ({@code SimpleParticleType}),
 * so every mod particle that carries data needs this three-line subclass. The
 * original needed nothing of the kind: its FX were created on the client from
 * a dedicated {@code PacketFX*} message, so the payload had no registry codec
 * at all — that is exactly the API difference this class absorbs.</p>
 *
 * @param overrideLimiter 1.21.1's per-type pass-through of the particle cap.
 *                        The ported effects pass {@code false}: the original
 *                        gated its FX through
 *                        {@code ClientProxy.particleCount} (the vanilla
 *                        particle setting) itself, and this port keeps that
 *                        gate in {@code TcFxPlans}, so the platform cap is
 *                        honoured on top of it rather than bypassed.
 */
public final class TcParticleType<T extends ParticleOptions> extends ParticleType<T> {

    private final MapCodec<T> codec;
    private final StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec;

    public TcParticleType(final boolean overrideLimiter, final MapCodec<T> codec,
            final StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        super(overrideLimiter);
        this.codec = codec;
        this.streamCodec = streamCodec;
    }

    @Override
    public MapCodec<T> codec() {
        return this.codec;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() {
        return this.streamCodec;
    }
}
