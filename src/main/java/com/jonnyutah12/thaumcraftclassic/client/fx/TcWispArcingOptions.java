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
 * The spawn payload of one {@code thaumcraft:wisp_arcing} particle
 * (TC-M7-02-c-s) — the arcing wisp the vis/essentia source streams draw
 * ({@code ClientEffectHooks.sourceStreamFX} → {@code FXWispArcing}).
 *
 * <p>{@code FXWispArcing} is not a physics particle: its ctor stores the
 * <em>full</em> delta to the target as the "motion" field
 * ({@code motion = target - start}, FXWispArcing.java:52-54 — no division by
 * lifetime, unlike {@code FXWisp}'s straight-line ctor) and
 * {@code onUpdate} re-derives the position from it every tick:
 * {@code var1 = 1 - age/life; pos = start + delta * var1} with
 * {@code y += 1 - var2 * var2} for the arc bulge (FXWispArcing.java:93-105).
 * The particle therefore starts at the <em>target</em> (var1 = 1 at age 0)
 * and lands on the start — the stream draws tip-first, and the port keeps
 * that direction exactly. The plan carries the delta and the ctor's
 * lifetime draw; the particle replays the position curve.</p>
 */
public record TcWispArcingOptions(int color, float scale, int lifetime,
        double deltaX, double deltaY, double deltaZ) implements ParticleOptions {

    public static final MapCodec<TcWispArcingOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("color").forGetter(TcWispArcingOptions::color),
                    Codec.FLOAT.fieldOf("scale").forGetter(TcWispArcingOptions::scale),
                    Codec.INT.fieldOf("lifetime").forGetter(TcWispArcingOptions::lifetime),
                    Codec.DOUBLE.fieldOf("delta_x").forGetter(TcWispArcingOptions::deltaX),
                    Codec.DOUBLE.fieldOf("delta_y").forGetter(TcWispArcingOptions::deltaY),
                    Codec.DOUBLE.fieldOf("delta_z").forGetter(TcWispArcingOptions::deltaZ))
                    .apply(instance, TcWispArcingOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcWispArcingOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, TcWispArcingOptions::color,
                    ByteBufCodecs.FLOAT, TcWispArcingOptions::scale,
                    ByteBufCodecs.VAR_INT, TcWispArcingOptions::lifetime,
                    ByteBufCodecs.DOUBLE, TcWispArcingOptions::deltaX,
                    ByteBufCodecs.DOUBLE, TcWispArcingOptions::deltaY,
                    ByteBufCodecs.DOUBLE, TcWispArcingOptions::deltaZ,
                    TcWispArcingOptions::new);

    @Override
    public ParticleType<?> getType() {
        return ModParticles.WISP_ARCING.get();
    }
}
