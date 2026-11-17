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
 * The spawn payload of one {@code thaumcraft:smoke_trail} mote
 * (TC-M7-02-c-s-f5) — the entity-chasing smoke of
 * {@code ClientProxy.smokeTrailFX} (client/ClientProxy.java:918-941) →
 * {@code FXSmokeTrail}.
 *
 * <p>Smoke trail is <em>entity-space</em>: the ctor keeps the target
 * {@code Entity} and chases it, so the payload carries the target's id and the
 * particle re-resolves it every tick (the BD-49 rule for entity-riding FX —
 * 1.21.1 {@code ParticleOptions} are codec data, not references). The ctor's
 * rolls — the scale {@code rand*0.5+0.5}, the distance-derived lifetime
 * {@code base/2 + rand(base)} with {@code base = max(1, (int)(dist*3))} and the
 * three {@code (rand-rand)*0.1} velocity draws (FXSmokeTrail.java:21-36) — are
 * drawn by {@link TcFxPlans#smokeTrail}.</p>
 */
public record TcSmokeTrailOptions(int targetEntityId, int color, int lifetime,
        float scale) implements ParticleOptions {

    public static final MapCodec<TcSmokeTrailOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("target").forGetter(TcSmokeTrailOptions::targetEntityId),
                    Codec.INT.fieldOf("color").forGetter(TcSmokeTrailOptions::color),
                    Codec.INT.fieldOf("lifetime").forGetter(TcSmokeTrailOptions::lifetime),
                    Codec.FLOAT.fieldOf("scale").forGetter(TcSmokeTrailOptions::scale))
                    .apply(instance, TcSmokeTrailOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcSmokeTrailOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TcSmokeTrailOptions::targetEntityId,
                    ByteBufCodecs.INT, TcSmokeTrailOptions::color,
                    ByteBufCodecs.VAR_INT, TcSmokeTrailOptions::lifetime,
                    ByteBufCodecs.FLOAT, TcSmokeTrailOptions::scale,
                    TcSmokeTrailOptions::new);

    /** The ctor's scale roll: {@code rand*0.5 + 0.5} (FXSmokeTrail.java:21). */
    public static final float SCALE_BASE = 0.5F;
    public static final float SCALE_SPAN = 0.5F;

    /** The lifetime's distance factor: {@code (int)(dist * 3.0F)} (line 27). */
    public static final double LIFETIME_DIST_FACTOR = 3.0;

    /** The ctor's spawn jitter: {@code (rand - rand) * 0.1F} (lines 33-35). */
    public static final double JITTER = 0.1;

    /** The ctor's {@code particleGravity = 0.2F} (line 36). */
    public static final float GRAVITY = 0.2F;

    /** {@code setParticleBounds(0.01F, 0.01F)} — the ctor's default is untouched. */
    public static final float BOUND = 0.1F;

    /** The chase dies at 1 block of the target (onUpdate line 78). */
    public static final double ARRIVAL_DISTANCE = 1.0;

    /** The attraction step {@code d13} and its close-range value (lines 91, 94). */
    public static final double PULL = 0.3;
    public static final double PULL_CLOSE = 0.6;

    /** The shrink window {@code d11 < 4.0} with its 0.9 factor (lines 93-95). */
    public static final double SHRINK_DISTANCE = 4.0;
    public static final float SHRINK_FACTOR = 0.9F;

    /** The motion clamp {@code ±0.35} (lines 101-103). */
    public static final double CLAMP = 0.35;

    /** The per-tick drag 0.985 (lines 84-86). */
    public static final double DRAG = 0.985;

    /** The render's bob: {@code sin(age/3) * 0.33 + 0.66} (line 51). */
    public static final float BOB_SPAN = 0.33F;
    public static final float BOB_BASE = 0.66F;

    /** The render's vertex alpha 0.33 (lines 52, 63). */
    public static final float ALPHA = 0.33F;

    /** The billboard half-extent: {@code 0.3F * particleScale * bob} (line 57). */
    public static final float SIZE_FACTOR = 0.3F;

    /** The frame ramp {@code 1 + age/lifetime * 4} → 5 frames (line 54). */
    public static final int FRAMES = 5;

    /** The view-distance gate 64 / 32 on the fancy-particles setting (lines 38-45). */
    public static final int VIEW_FAR = 64;
    public static final int VIEW_NEAR = 32;

    @Override
    public ParticleType<?> getType() {
        return ModParticles.SMOKE_TRAIL.get();
    }
}
