package com.jonnyutah12.thaumcraftclassic.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The ported vis swarm (TC-M7-02-c-s-f5): one {@code FXSwarm}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXSwarm.java),
 * the attendant swarm {@code ClientProxy.swarmParticleFX} puts on an entity
 * (client/ClientProxy.java:986-1000).
 *
 * <p>Entity-space (BD-20), and the only family that steers with the entity's
 * own rotation ramp: {@code onUpdate} (lines 79-135) adds {@code +pg} to the
 * vertical velocity while the target lives and is not in its hurt-flash,
 * {@code -pg/2} once it dies or is hurt (and fades it out over 50 ticks), then
 * faces the target with a yaw/pitch step of
 * {@code turnSpeed/2 + rand(turnSpeed/2)} — negated when the mote is inside the
 * target's width or the target is hurt — rebuilds the direction from the yaw
 * and pitch, and rewrites the velocity through {@code setHeading(dir, speed,
 * 15.0F)} (lines 118-133, 195-215). Drag 0.985 on every axis.</p>
 *
 * <p>Render (lines 50-77): the 8-frame strip is the sheet's <em>row 4</em> —
 * {@code part = 7 + age % 8} at v = 0.25, i.e. cells 71..78 — the billboard is
 * {@code 0.1F·particleScale·(sin(age/3)·0.25 + 1.0)}, and the fade alpha is
 * {@code (50 - deathtimer) / 50} with the green/blue channels halved once the
 * target is dead, hurt or gone.</p>
 *
 * <p>The buzz sound ({@code "thaumcraft:fly"}, at most 3 concurrent within
 * 8 blocks, 1-in-50 ticks, lines 136-149) is the original's own sound, so it is
 * ported as {@code TcSounds.FLY}; the 1.5 ms slot list is a static.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcSwarmParticle extends TextureSheetParticle {

    /** The original's {@code buzzcount} static (FXSwarm.java:14, 147). */
    private static final java.util.ArrayDeque<Long> BUZZ_SLOTS = new java.util.ArrayDeque<>();

    private final SpriteSet sprites;
    private final int targetEntityId;
    private final float scale0;
    private final float speed;
    private final float turnSpeed;
    /** The sender's per-tick gravity {@code pg} (FXSwarm.java:22, 32), not {@code Particle.gravity}. */
    private final float pg;
    private final int color;
    private int deathtimer;
    private float yaw;
    private float pitch;

    TcSwarmParticle(final ClientLevel level, final double x, final double y, final double z,
            final double vx, final double vy, final double vz,
            final TcSwarmOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.targetEntityId = options.targetEntityId();
        this.scale0 = options.scale();
        this.speed = options.speed();
        this.turnSpeed = options.turnSpeed();
        this.pg = options.gravity();
        this.color = options.color();
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.lifetime = Integer.MAX_VALUE;       // the swarm dies on its target, not on age
        this.gravity = 0.0F;                     // onUpdate applies pg itself
        this.friction = 1.0F;
        this.hasPhysics = false;                 // the ctor sets the no-clip flag
        this.rCol = TcFxPlans.red(this.color);
        this.gCol = TcFxPlans.green(this.color);
        this.bCol = TcFxPlans.blue(this.color);
        this.quadSize = TcSwarmOptions.SIZE_FACTOR * this.scale0;
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.age++;
        final Entity target = this.level.getEntity(this.targetEntityId);
        final boolean lost = target == null || target.isRemoved()
                || (target instanceof LivingEntity living && living.invulnerableTime > 0);
        if (lost) {
            // FXSwarm.java:82-88: the swarm drops and fades when the target is
            // gone, removed, or in its invulnerable frames.
            this.deathtimer++;
            this.yd -= this.pg / 2.0F;
            if (this.deathtimer > TcSwarmOptions.DEATH_FADE_TICKS) {
                this.remove();
                return;
            }
        } else {
            this.yd += this.pg;
        }
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
        this.xd *= TcSwarmOptions.DRAG;
        this.yd *= TcSwarmOptions.DRAG;
        this.zd *= TcSwarmOptions.DRAG;
        if (target != null && !target.isRemoved()) {
            final boolean hurt = target instanceof LivingEntity living && living.hurtTime > 0;
            final double step = this.turnSpeed / 2.0F
                    + this.random.nextInt(Math.max(1, (int) (this.turnSpeed / 2.0F)));
            final double width = target.getDimensions(target.getPose()).width();
            final boolean far = target.distanceToSqr(this.x, this.y, this.z) > width * width;
            // FXSwarm.java:113-127: face the target, steering away when close.
            this.face(target, far ? step : -step, far ? step : -step);
            // Lines 128-133: rebuild the direction from the yaw/pitch and hand
            // it to setHeading, which overwrites the velocity.
            final double dx = -Mth.sin(this.yaw * (float) Math.PI / 180.0F)
                    * Mth.cos(this.pitch * (float) Math.PI / 180.0F);
            final double dz = Mth.cos(this.yaw * (float) Math.PI / 180.0F)
                    * Mth.cos(this.pitch * (float) Math.PI / 180.0F);
            final double dy = -Mth.sin(this.pitch * (float) Math.PI / 180.0F);
            setHeading(dx, dy, dz, this.speed);
        }
        buzz();
        // The 8-frame strip (lines 56-59) and the bob (line 55).
        final float bob = (float) (Math.sin(this.age / 3.0) * TcSwarmOptions.BOB_SPAN
                + TcSwarmOptions.BOB_BASE);
        this.quadSize = TcSwarmOptions.SIZE_FACTOR * this.scale0 * bob;
        final int part = this.age % TcSwarmOptions.FRAMES;
        this.setSprite(this.sprites.get(part, TcSwarmOptions.FRAMES));
        // The fade alpha and the half-bright colour of a lost target (lines 66-71):
        // a living, unhurt target keeps the mote's full green/blue.
        this.alpha = (TcSwarmOptions.DEATH_FADE_TICKS - this.deathtimer)
                / (float) TcSwarmOptions.DEATH_FADE_TICKS;
        final boolean bright = target instanceof LivingEntity living
                && !target.isRemoved() && living.hurtTime <= 0;
        this.gCol = TcFxPlans.green(this.color) / (bright ? 1.0F : 2.0F);
        this.bCol = TcFxPlans.blue(this.color) / (bright ? 1.0F : 2.0F);
    }

    /**
     * The entity-style {@code faceEntity(target, yawDelta, pitchDelta)} of
     * {@code onUpdate} (lines 114-127): point at the target's mid-body, add the
     * handed deltas, wrap to ±180° and clamp the pitch to ±90° (1.7.10
     * {@code Entity.faceEntity} + {@code setRotation}).
     */
    private void face(final Entity target, final double yawDelta, final double pitchDelta) {
        final double dx = target.getX() - this.x;
        final double dy = target.getBoundingBox().getCenter().y - this.y;
        final double dz = target.getZ() - this.z;
        final double horizontal = Math.sqrt(dx * dx + dz * dz);
        this.pitch = Mth.clamp(Mth.wrapDegrees(
                (float) (Math.atan2(dy, horizontal) * 180.0 / Math.PI) + (float) pitchDelta), -90.0F, 90.0F);
        this.yaw = Mth.wrapDegrees(
                (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) + (float) yawDelta);
    }

    /**
     * {@code setHeading} (FXSwarm.java:195-215): normalise the direction, add a
     * per-axis {@code nextGaussian() * ±boolean * 0.0075 * 15.0F} spread, scale
     * by {@code speed} and <em>assign</em> it to the velocity.
     */
    private void setHeading(final double dx, final double dy, final double dz, final float speed) {
        final double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length <= 1.0E-6) {
            return;
        }
        final double nx = dx / length + this.random.nextGaussian()
                * (this.random.nextBoolean() ? 1 : -1)
                * TcSwarmOptions.HEADING_JITTER * TcSwarmOptions.HEADING_SPREAD;
        final double ny = dy / length + this.random.nextGaussian()
                * (this.random.nextBoolean() ? 1 : -1)
                * TcSwarmOptions.HEADING_JITTER * TcSwarmOptions.HEADING_SPREAD;
        final double nz = dz / length + this.random.nextGaussian()
                * (this.random.nextBoolean() ? 1 : -1)
                * TcSwarmOptions.HEADING_JITTER * TcSwarmOptions.HEADING_SPREAD;
        this.xd = nx * speed;
        this.yd = ny * speed;
        this.zd = nz * speed;
    }

    /** The buzz sound slot bookkeeping (FXSwarm.java:136-149). */
    private void buzz() {
        final long now = System.nanoTime();
        BUZZ_SLOTS.removeIf(slot -> slot < now);
        if (BUZZ_SLOTS.size() < TcSwarmOptions.BUZZ_MAX
                && this.random.nextInt(TcSwarmOptions.BUZZ_CHANCE) == 0
                && this.level.getNearestPlayer(this.x, this.y, this.z,
                        TcSwarmOptions.BUZZ_RADIUS, false) != null) {
            this.level.playLocalSound(this.x, this.y, this.z,
                    com.jonnyutah12.thaumcraftclassic.sound.ModSounds.FLY.value(),
                    net.minecraft.sounds.SoundSource.PLAYERS, TcSwarmOptions.BUZZ_VOLUME,
                    TcSwarmOptions.BUZZ_PITCH_BASE
                            + this.random.nextFloat() * TcSwarmOptions.BUZZ_PITCH_SPAN,
                    false);
            BUZZ_SLOTS.add(now + TcSwarmOptions.BUZZ_SLOT_NANOS);
        }
    }

    /** The original's {@code getBrightnessForRender} is a flat 240 (line 65). */
    @Override
    protected int getLightColor(final float partialTick) {
        return 15 << 4 | 15 << 20;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Provider wired from {@link TcParticleProviders} for the 8-frame strip. */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcSwarmOptions> {

        @Override
        public Particle createParticle(final TcSwarmOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcSwarmParticle(level, x, y, z, vx, vy, vz, options, this.sprites);
        }
    }
}
