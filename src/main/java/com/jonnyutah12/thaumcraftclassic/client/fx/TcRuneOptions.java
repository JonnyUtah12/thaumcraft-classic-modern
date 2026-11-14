package com.jonnyutah12.thaumcraftclassic.client.fx;

import com.jonnyutah12.thaumcraftclassic.registration.ModParticles;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Per-particle state for the rune decal — the ported
 * {@code thaumcraft:rune} particle.
 *
 * <p>Every field is a value 1.7.10 {@code thaumcraft.client.fx.particles.FXBlockRunes}
 * reads out of the 9-arg {@code EffectRenderer.addEffect} call built by
 * {@code ClientProxy.blockRunes} (ClientProxy.java:974-978) or draws in its
 * constructor (FXBlockRunes.java:16-37):</p>
 *
 * <ul>
 *   <li>{@code color} — {@code particleRed/Green/Blue} (lines 21-23) quantised
 *       through {@link TcFxPlans#rgb}; the ctor's red-zero quirk (lines 16-18)
 *       is applied by the plan, not here.</li>
 *   <li>{@code lifetime} — {@code field_70547_e = 3 * dur} (line 26), where
 *       {@code dur} is the add call's {@code dur} argument.</li>
 *   <li>{@code scale} — {@code particleScale = 1.0 + nextGaussian()*0.1} (line 36),
 *       the add call's {@code scale} argument.</li>
 *   <li>{@code frame} — the {@code runeIndex} column rolled at line 33
 *       ({@code (int)(Math.random()*16 + 224)}); the {@code % 16} is what
 *       reaches the texture (line 51), so it is stored resolved.</li>
 *   <li>{@code yaw} — {@code rotation} (line 20), {@code nextInt(4) * 90}, a
 *       multiple of 90° about {@code (0,1,0)} (line 48).</li>
 *   <li>{@code offsetU}/{@code offsetV} — the in-plane jitter {@code ofx}
 *       (line 34) and {@code ofy} (line 35), which the render rotates with the
 *       quad at line 50.</li>
 *   <li>{@code gravity} — the add call's {@code gravity} argument, consumed by
 *       {@code setGravity} (lines 90-92) after the ctor zeroes it (line 24).</li>
 * </ul>
 *
 * <p>The add-call coordinates ride in {@link ParticleSpawn}'s position, with the
 * constructor's {@code +0.5} block-centre offset applied by the plan, so a
 * sender can pass the block coordinates the original senders passed. Motion is
 * always zero: the ctor zeroes {@code motionX/motionY/motionZ} (line 25), so the
 * particle is self-integrated like the beam family and its type declares
 * velocity as irrelevant.</p>
 *
 * <p>The wire view nests the sprite identity (colour, lifetime, scale, frame)
 * in a 4-field core: 1.21.1's {@code StreamCodec.composite} caps at 6 fields,
 * where the original's FX constructor took 8 positional arguments. The flat
 * JSON view is unaffected.</p>
 */
public record TcRuneOptions(int color, int lifetime, float scale, int frame, int yaw,
        float offsetU, float offsetV, float gravity) implements ParticleOptions {

    public static final MapCodec<TcRuneOptions> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(Codec.INT.fieldOf("color").forGetter(TcRuneOptions::color),
                    Codec.INT.fieldOf("lifetime").forGetter(TcRuneOptions::lifetime),
                    Codec.FLOAT.fieldOf("scale").forGetter(TcRuneOptions::scale),
                    Codec.INT.fieldOf("frame").forGetter(TcRuneOptions::frame),
                    Codec.INT.fieldOf("yaw").forGetter(TcRuneOptions::yaw),
                    Codec.FLOAT.fieldOf("offsetU").forGetter(TcRuneOptions::offsetU),
                    Codec.FLOAT.fieldOf("offsetV").forGetter(TcRuneOptions::offsetV),
                    Codec.FLOAT.fieldOf("gravity").forGetter(TcRuneOptions::gravity))
            .apply(instance, TcRuneOptions::new));

    /** The sprite identity half of the payload — see the class javadoc. */
    private record Core(int color, int lifetime, float scale, int frame) {
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, Core> CORE_STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, Core::color,
                    ByteBufCodecs.VAR_INT, Core::lifetime,
                    ByteBufCodecs.FLOAT, Core::scale,
                    ByteBufCodecs.VAR_INT, Core::frame,
                    Core::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, TcRuneOptions> STREAM_CODEC =
            StreamCodec.composite(
                    CORE_STREAM_CODEC, options -> new Core(options.color(), options.lifetime(),
                            options.scale(), options.frame()),
                    ByteBufCodecs.VAR_INT, TcRuneOptions::yaw,
                    ByteBufCodecs.FLOAT, TcRuneOptions::offsetU,
                    ByteBufCodecs.FLOAT, TcRuneOptions::offsetV,
                    ByteBufCodecs.FLOAT, TcRuneOptions::gravity,
                    (core, yaw, offsetU, offsetV, gravity) -> new TcRuneOptions(core.color(),
                            core.lifetime(), core.scale(), core.frame(), yaw, offsetU, offsetV,
                            gravity));

    @Override
    public ParticleType<?> getType() {
        return ModParticles.RUNE.get();
    }
}
