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
 * The spawn payload of one {@code thaumcraft:swarm} mote
 * (TC-M7-02-c-s-f5) — the vis-swarm attendant of
 * {@code ClientProxy.swarmParticleFX} (client/ClientProxy.java:986-1000) →
 * {@code FXSwarm}.
 *
 * <p>Entity-space like the smoke trail: the ctor keeps the target and steers
 * toward it with the entity-style yaw/pitch ramp (FXSwarm.java:118-133), so the
 * payload carries the target's id plus the three sender knobs — {@code speed},
 * {@code turnSpeed} and the per-tick gravity {@code pg} — that
 * {@code swarmParticleFX} forwards from its caller (FXSwarm.java:70-75). The
 * ctor's own rolls (scale {@code rand*0.5+1.0}, the three
 * {@code (rand-rand)*0.2} velocity draws) and the sender's colour rolls are
 * drawn by {@link TcFxPlans#swarm}.</p>
 */
public record TcSwarmOptions(int targetEntityId, int color, float scale, float speed,
        float turnSpeed, float gravity) implements ParticleOptions {

    public static final MapCodec<TcSwarmOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("target").forGetter(TcSwarmOptions::targetEntityId),
                    Codec.INT.fieldOf("color").forGetter(TcSwarmOptions::color),
                    Codec.FLOAT.fieldOf("scale").forGetter(TcSwarmOptions::scale),
                    Codec.FLOAT.fieldOf("speed").forGetter(TcSwarmOptions::speed),
                    Codec.FLOAT.fieldOf("turnSpeed").forGetter(TcSwarmOptions::turnSpeed),
                    Codec.FLOAT.fieldOf("gravity").forGetter(TcSwarmOptions::gravity))
                    .apply(instance, TcSwarmOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcSwarmOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TcSwarmOptions::targetEntityId,
                    ByteBufCodecs.INT, TcSwarmOptions::color,
                    ByteBufCodecs.FLOAT, TcSwarmOptions::scale,
                    ByteBufCodecs.FLOAT, TcSwarmOptions::speed,
                    ByteBufCodecs.FLOAT, TcSwarmOptions::turnSpeed,
                    ByteBufCodecs.FLOAT, TcSwarmOptions::gravity,
                    TcSwarmOptions::new);

    /** The ctor's scale roll {@code rand*0.5 + 1.0} (FXSwarm.java:24). */
    public static final float SCALE_BASE = 1.0F;
    public static final float SCALE_SPAN = 0.5F;

    /** The ctor's spawn jitter {@code (rand - rand) * 0.2F} (lines 29-31). */
    public static final double JITTER = 0.2;

    /** The ctor's defaults: speed 0.2, turnSpeed 10.0, gravity 0.1 (lines 21-22, 32). */
    public static final float DEFAULT_SPEED = 0.2F;
    public static final float DEFAULT_TURN_SPEED = 10.0F;
    public static final float DEFAULT_GRAVITY = 0.1F;

    /** The sender's spawn offset: {@code (rand - rand) * 2.0} on every axis (ClientProxy.java:987-990). */
    public static final double SENDER_SPREAD = 2.0;

    /** The sender's colour rolls: r 0.8+rand*0.2, g rand*0.4, b 1.0-rand*0.2 (lines 992-994). */
    public static final float COLOR_R_BASE = 0.8F;
    public static final float COLOR_R_SPAN = 0.2F;
    public static final float COLOR_G_SPAN = 0.4F;
    public static final float COLOR_B_BASE = 1.0F;
    public static final float COLOR_B_SPAN = 0.2F;

    /** The death fade: 50 ticks, alpha {@code (50 - deathtimer) / 50} (lines 66, 95-98). */
    public static final int DEATH_FADE_TICKS = 50;

    /** The per-tick drag 0.985 (lines 103-105). */
    public static final double DRAG = 0.985;

    /** The steer step: {@code turnSpeed/2 + rand((int)(turnSpeed/2))} (lines 116-127). */
    public static final float TURN_STEP_HALF = 2.0F;

    /** The heading spread of {@code setHeading(..., speed, 15.0F)} (line 133). */
    public static final float HEADING_SPREAD = 15.0F;

    /** The gaussian spread factor of {@code setHeading} (FXSwarm.java:205-215). */
    public static final double HEADING_JITTER = 0.0075;

    /** The chase gate: keep steering while farther than the target's width and not hurt (line 113). */
    public static final double CHASE_WIDTH_FACTOR = 1.0;

    /** The bob: {@code sin(age/3) * 0.25 + 1.0} (line 55). */
    public static final float BOB_SPAN = 0.25F;
    public static final float BOB_BASE = 1.0F;

    /** The billboard half-extent {@code 0.1F * particleScale * bob} (line 60). */
    public static final float SIZE_FACTOR = 0.1F;

    /** The 8-frame strip: {@code part = 7 + age % 8} at v = 0.25 (lines 57-59). */
    public static final int FRAMES = 8;
    public static final int FIRST_CELL = 71;

    /** The buzz sound: at most 3 concurrent, 1-in-50 ticks within 8 blocks (lines 140-149). */
    public static final int BUZZ_MAX = 3;
    public static final int BUZZ_CHANCE = 50;
    public static final double BUZZ_RADIUS = 8.0;
    public static final float BUZZ_VOLUME = 0.03F;
    public static final float BUZZ_PITCH_BASE = 0.5F;
    public static final float BUZZ_PITCH_SPAN = 0.4F;
    /** The buzz slot's lifetime: {@code nanoTime() + 1500000L} (line 147). */
    public static final long BUZZ_SLOT_NANOS = 1_500_000L;

    /** The view-distance gate 64 / 32 (lines 34-41). */
    public static final int VIEW_FAR = 64;
    public static final int VIEW_NEAR = 32;

    @Override
    public ParticleType<?> getType() {
        return ModParticles.SWARM.get();
    }
}
