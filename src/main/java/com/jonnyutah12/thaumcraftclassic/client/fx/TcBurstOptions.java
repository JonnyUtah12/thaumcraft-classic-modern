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
 * The spawn payload of one {@code thaumcraft:burst} ring
 * (TC-M7-02-c-s-f5) — the node burst of {@code ClientProxy.burst}
 * (client/ClientProxy.java:747-751), which hands the caller's {@code size}
 * straight into the ctor.
 *
 * <p>{@code FXBurst} (fx/particles/FXBurst.java:12-23) has no draws at all: the
 * ctor multiplies the default scale by {@code size}, sets the 31-tick lifetime,
 * zeroes gravity and velocity, and sets 0.01 bounds. Its render is a
 * 32-frame strip of the <em>node</em> sheet, additive, at full brightness
 * (lines 25-56): {@code u = age % 32 / 32}, {@code v = 0.96875..1.0}, bound to
 * {@code TileNodeRenderer.nodetex}.</p>
 */
public record TcBurstOptions(float size) implements ParticleOptions {

    public static final MapCodec<TcBurstOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.FLOAT.fieldOf("size").forGetter(TcBurstOptions::size))
                    .apply(instance, TcBurstOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcBurstOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, TcBurstOptions::size,
                    TcBurstOptions::new);

    /** {@code particleMaxAge = 31} (FXBurst.java:19). */
    public static final int LIFETIME = 31;

    /** The strip: {@code u = age % 32 / 32} → 32 frames (FXBurst.java:31). */
    public static final int FRAMES = 32;

    /** The render's flat brightness: {@code setBrightness(240)} (FXBurst.java:38). */
    public static final int LIGHT = 240;

    /** The {@code glColor4f(1,1,1,0.75)} of the shell (FXBurst.java:29). */
    public static final float ALPHA = 0.75F;

    @Override
    public ParticleType<?> getType() {
        return ModParticles.BURST.get();
    }
}
