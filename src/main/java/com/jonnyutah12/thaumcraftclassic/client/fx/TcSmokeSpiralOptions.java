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
 * The spawn payload of one {@code thaumcraft:smoke_spiral} mote
 * (TC-M7-02-c-s-f5) — the boiler/smoke spiral of
 * {@code ClientProxy.smokeSpiral} (client/ClientProxy.java:643-649) →
 * {@code FXSmokeSpiral}.
 *
 * <p>The ctor keeps the three constructor arguments that shape the orbit —
 * {@code radius}, the {@code start} angle in degrees and the {@code miny}
 * floor — plus its own lifetime roll {@code 20 + rand(10)} (FXSmokeSpiral.java:22),
 * drawn by {@link TcFxPlans#smokeSpiral}. The colour is not ctor data: the
 * sender sets it from the packet's int after construction
 * ({@code ClientProxy.java:646}, {@code func_70538_b}), so it rides the payload.</p>
 */
public record TcSmokeSpiralOptions(float radius, int startAngle, double floorY,
        int lifetime, int color) implements ParticleOptions {

    public static final MapCodec<TcSmokeSpiralOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.FLOAT.fieldOf("radius").forGetter(TcSmokeSpiralOptions::radius),
                    Codec.INT.fieldOf("start").forGetter(TcSmokeSpiralOptions::startAngle),
                    Codec.DOUBLE.fieldOf("floor_y").forGetter(TcSmokeSpiralOptions::floorY),
                    Codec.INT.fieldOf("lifetime").forGetter(TcSmokeSpiralOptions::lifetime),
                    Codec.INT.fieldOf("color").forGetter(TcSmokeSpiralOptions::color))
                    .apply(instance, TcSmokeSpiralOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcSmokeSpiralOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, TcSmokeSpiralOptions::radius,
                    ByteBufCodecs.VAR_INT, TcSmokeSpiralOptions::startAngle,
                    ByteBufCodecs.DOUBLE, TcSmokeSpiralOptions::floorY,
                    ByteBufCodecs.VAR_INT, TcSmokeSpiralOptions::lifetime,
                    ByteBufCodecs.INT, TcSmokeSpiralOptions::color,
                    TcSmokeSpiralOptions::new);

    /** {@code particleMaxAge = 20 + rand.nextInt(10)} (FXSmokeSpiral.java:22). */
    public static final int LIFETIME_BASE = 20;
    public static final int LIFETIME_SPAN = 10;

    /** The ctor's {@code particleGravity = -0.01F} (FXSmokeSpiral.java:18). */
    public static final float GRAVITY = -0.01F;

    /** {@code setParticleBounds(0.01F, 0.01F)} (FXSmokeSpiral.java:24). */
    public static final float BOUND = 0.01F;

    /** The render's vertex alpha: {@code 0.66F * particleAlpha} (FXSmokeSpiral.java:29, 49). */
    public static final float ALPHA = 0.66F;

    /** The orbit sweep: {@code start + 720 * age/lifetime} degrees (line 33). */
    public static final float SWEEP_DEGREES = 720.0F;

    /** The elevation term: {@code 90 - 180 * age/lifetime} degrees (line 34). */
    public static final float ELEVATION_BASE = 90.0F;
    public static final float ELEVATION_SPAN = 180.0F;

    /** The render's floor: {@code max(y + mY, miny + 0.1F)} (line 46). */
    public static final float FLOOR_LIFT = 0.1F;

    /** The billboard half-extent: {@code 0.15F * particleScale} (line 45). */
    public static final float SIZE_FACTOR = 0.15F;

    /** The frame ramp: {@code 1 + age/lifetime * 4} → 5 frames (line 31). */
    public static final int FRAMES = 5;

    @Override
    public ParticleType<?> getType() {
        return ModParticles.SMOKE_SPIRAL.get();
    }
}
