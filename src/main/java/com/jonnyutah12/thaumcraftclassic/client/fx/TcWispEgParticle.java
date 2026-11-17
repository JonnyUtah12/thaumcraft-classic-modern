package com.jonnyutah12.thaumcraftclassic.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The ported EG (essentia-gas) wisp particle (TC-M7-02-c-s-f1).
 *
 * <p>One instance is one {@code FXWispEG}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXWispEG.java):
 * {@code onUpdate} (lines 62-83) rides the target's own motion while airborne,
 * runs {@code pushOutOfBlocks}, integrates its velocity, drags every axis by
 * 0.98 (plus 0.85 on x/z while on ground) and dies at its rolled lifetime.
 * The render pass (lines 49-60) fades the mote by its distance to the camera
 * ({@code 1 - min(1024, distanceSquared)/1024}), scales its alpha by
 * {@code 0.2 * (1 - age/lifetime)}, draws it at a fixed
 * {@code 0.5 * particleScale} half-extent, and steps its frame as
 * {@code age % 13}.</p>
 *
 * <p>Its motion is self-integrated, so the platform drag is neutralised
 * ({@code friction = 1}) and gravity is 0 — the original never sets
 * {@code particleGravity} on this class; the {@code yd = -0.05} initial fall
 * is the whole downward story.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcWispEgParticle extends TextureSheetParticle {

    /** {@code FXWispEG.java:52}: the fade denominator, in squared blocks. */
    static final double FADE_DISTANCE_SQ = 1024.0;
    /** {@code FXWispEG.java:57}: 13 frame indices, wrapping. */
    static final int FRAME_CYCLE = 13;

    private final SpriteSet sprites;
    private final int targetEntityId;

    TcWispEgParticle(final ClientLevel level, final double x, final double y, final double z,
            final double vx, final double vy, final double vz,
            final TcWispEgOptions options, final SpriteSet sprites) {
        // The 4-argument form, matching FXWispEG's super(..., 0,0,0) plus the
        // explicit gaussian fields it assigns right after (FXWispEG.java:21-26).
        super(level, x, y, z);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.sprites = sprites;
        this.targetEntityId = options.targetEntityId();
        this.rCol = TcFxPlans.red(options.color());
        this.gCol = TcFxPlans.green(options.color());
        this.bCol = TcFxPlans.blue(options.color());
        this.lifetime = Math.max(1, options.lifetime());
        // FXWispEG.java:27-28: particleScale *= 0.4, and renderDynamic draws
        // at 0.5 * particleScale (line 51) — a fixed size, no age envelope.
        this.quadSize = 0.5F * FxWispRules.EG_SCALE;
        this.hasPhysics = true;      // the original's ignorePhysics stays false
        this.gravity = 0.0F;
        this.friction = 1.0F;        // the EG mote drags its own velocity
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        // FXWispEG.onUpdate (FXWispEG.java:62-83).
        if (this.targetEntityId != TcWispOptions.NO_TARGET) {
            final Entity target = this.level.getEntity(this.targetEntityId);
            if (target != null && !this.onGround) {
                // The mote rides the target's own velocity (lines 65-68).
                this.setPos(this.x + target.getDeltaMovement().x, this.y,
                        this.z + target.getDeltaMovement().z);
            }
        }
        this.pushOutOfBlocks();
        this.xd *= FxWispRules.FREE_DRAG;
        this.yd *= FxWispRules.FREE_DRAG;
        this.zd *= FxWispRules.FREE_DRAG;
        if (this.onGround) {
            this.xd *= FxWispRules.EG_GROUND_DRAG;
            this.zd *= FxWispRules.EG_GROUND_DRAG;
        }
        // The render pass's alpha (FXWispEG.java:50-57): a camera-distance
        // fade applied twice — once as the GL alpha 0.75*base and once inside
        // the vertex alpha 0.2*agescale*base — so the port multiplies the
        // factors into the single 1.21.1 particle alpha.
        final float base = this.cameraFade();
        final float ageScale = 1.0F - (float) this.age / (float) Math.max(1, this.lifetime);
        this.alpha = 0.75F * base * 0.2F * ageScale * base;
        // FXWispEG.java:59: the frame index is age % 13 over the 13-frame cycle
        // (u = age%13/16, v = 0.1875 -> a 13-cell strip). SpriteSet.get(age,
        // 13) indexes exactly that cycle over the wisp_eg.json frame list.
        this.setSprite(this.sprites.get(this.age % FRAME_CYCLE, FRAME_CYCLE));
    }

    /**
     * The original's own push-out-of-blocks (FXWispEG.java:84, method at
     * 99-183): the EG mote is unconditionally pushed out of a solid non-water
     * cell (its {@code ignorePhysics} is never set), with the same
     * rand*0.05+0.025 outward kick and (rand-rand)*0.1 side jitter as the
     * base wisp — see {@link FxWispPush}.
     */
    private void pushOutOfBlocks() {
        final int direction = FxWispPush.pushDirection(this.level, this.x, this.y, this.z);
        if (direction == FxWispPush.NONE) {
            return;
        }
        final double push = this.random.nextFloat() * 0.05 + 0.025;   // FXWispEG.java:147
        final double jitter = (this.random.nextFloat() - this.random.nextFloat()) * 0.1;
        switch (direction) {                                          // FXWispEG.java:149-177
            case FxWispPush.WEST -> {
                this.xd = -push;
                this.yd = jitter;
                this.zd = jitter;
            }
            case FxWispPush.EAST -> {
                this.xd = push;
                this.yd = jitter;
                this.zd = jitter;
            }
            case FxWispPush.DOWN -> {
                this.yd = -push;
                this.xd = jitter;
                this.zd = jitter;
            }
            case FxWispPush.UP -> {
                this.yd = push;
                this.xd = jitter;
                this.zd = jitter;
            }
            case FxWispPush.NORTH -> {
                this.zd = -push;
                this.xd = jitter;
                this.yd = jitter;
            }
            case FxWispPush.SOUTH -> {
                this.zd = push;
                this.xd = jitter;
                this.yd = jitter;
            }
            default -> {
            }
        }
    }

    /** {@code 1 - min(1024, distanceSquaredToCamera)) / 1024} (FXWispEG.java:51-52). */
    private float cameraFade() {
        final Entity cameraEntity = Minecraft.getInstance().player;
        if (cameraEntity == null) {
            return 1.0F;
        }
        final double distanceSq = cameraEntity.distanceToSqr(this.x, this.y, this.z);
        return (float) (1.0 - Math.min(FADE_DISTANCE_SQ, distanceSq) / FADE_DISTANCE_SQ);
    }

    /** The original draws its FX sheet fullbright (tessellator setBrightness(240)). */
    @Override
    protected int getLightColor(final float partialTick) {
        return 15 << 4 | 15 << 20;
    }

    @Override
    public ParticleRenderType getRenderType() {
        // FXWispEG sets blendmode 771 (FXWispEG.java:43) and
        // getBlendMode (line 85-87) returns the additive layer for it.
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    /** Provider wired from {@link TcParticleProviders} for the JSON frame list. */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcWispEgOptions> {

        @Override
        public Particle createParticle(final TcWispEgOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcWispEgParticle(level, x, y, z, vx, vy, vz, options, this.sprites);
        }
    }
}
