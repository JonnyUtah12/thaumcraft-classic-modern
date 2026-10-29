package com.jonnyutah12.thaumcraftclassic.client.fx;

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
 * The ported wisp particle (TC-M7-02-c-s-f1).
 *
 * <p>One instance is one {@code FXWisp}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXWisp.java):
 * the per-tick body is its {@code onUpdate} (lines 143-173) — integrate the
 * current velocity, then either home on the target (drag 0.985, a 0.2
 * acceleration along the normalised delta to the target's mid-body, then a
 * ±0.2 clamp on every axis) or drift free (drag 0.98, extra 0.7 x/z drag while
 * on ground) — plus the {@code gravity} the senders set
 * ({@code wispFX} uses 0.02) and {@code pushOutOfBlocks} while the original's
 * {@code ignorePhysics} is off.</p>
 *
 * <p>Size is the render quad, not the bounding box: {@code renderDynamic}
 * draws at {@code 0.5F * particleScale} half-extent (FXWisp.java:175-176),
 * and {@code particleScale} is re-derived from {@code moteParticleScale} every
 * render through an age envelope — the up-then-down triangle
 * {@code age / (lifetime / 2)} (folded back by {@code 2 - x} past the peak),
 * or the plain 1→0 ramp when {@code shrink} is set. That envelope is applied
 * per tick here, which is what a 1.21.1 particle does with {@code quadSize}.</p>
 *
 * <p>Motion integration rides {@code Particle.tick()} with {@code friction = 1}
 * (the wisp applies its own drags itself, so the platform must not add one on
 * top) and {@code gravity} handed through from the sender, whose
 * {@code yd -= 0.04 * gravity} is the same expression the 1.7.10 {@code EntityFX}
 * base tick has. The wisp's art is the vanilla {@code /misc/fx.png} sheet in
 * the original, which the reference tree does not ship, so it renders on the
 * Thaumcraft glow frames — see BD-17 in docs/BEHAVIORAL_DEVIATIONS.md.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcWispParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float moteScale;
    private final int halfLife;
    private final boolean shrink;
    private final int targetEntityId;
    private final boolean homing;
    private final boolean additive;

    TcWispParticle(final ClientLevel level, final double x, final double y, final double z,
            final double vx, final double vy, final double vz,
            final TcWispOptions options, final SpriteSet sprites) {
        // The 4-argument form: the 7-argument one re-normalises and randomises
        // the handed velocity, and the original integrates exactly what the
        // sender computed (FXWisp.java:117-125 divides the delta by the
        // lifetime and hands that in as the velocity).
        super(level, x, y, z);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.sprites = sprites;
        this.moteScale = options.scale();
        this.halfLife = Math.max(1, options.lifetime() / 2);
        this.shrink = options.shrink();
        this.targetEntityId = options.targetEntityId();
        this.homing = options.homing();
        this.additive = options.additive();
        this.rCol = TcFxPlans.red(options.color());
        this.gCol = TcFxPlans.green(options.color());
        this.bCol = TcFxPlans.blue(options.color());
        this.alpha = 0.75F;                       // FXWisp.renderDynamic: glColor4f(1,1,1,0.75F)
        this.lifetime = Math.max(1, options.lifetime());
        this.quadSize = 0.5F * this.moteScale;    // FXWisp.java:175 half-extent
        this.hasPhysics = options.clip();         // the original's !ignorePhysics
        this.gravity = options.gravity();
        this.friction = 1.0F;                     // the wisp drags its own velocity
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        // FXWisp.onUpdate (FXWisp.java:143-173): prev-position bookkeeping and
        // the move happen in the base tick; the velocity mutation is what the
        // original does after the move.
        super.tick();
        final Entity target = this.homing ? this.level.getEntity(this.targetEntityId) : null;
        if (this.hasPhysics) {
            // FXWisp.java:180-182 — the original's own pushOutOfBlocks, ported
            // in FxWispPush; the original assigns the outward velocity right
            // before the move, this tick applies it for the next one (BD-17).
            this.pushOutOfBlocks();
        }
        if (target != null) {
            this.xd *= FxWispRules.HOMING_DRAG;
            this.yd *= FxWispRules.HOMING_DRAG;
            this.zd *= FxWispRules.HOMING_DRAG;
            final double dx = target.getX() - this.x;
            final double dy = target.getY() + target.getBbHeight() / 2.0 - this.y;
            final double dz = target.getZ() - this.z;
            final double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > 1.0E-4) {
                // FXWisp.java:155-163. The original divides unguarded and gets
                // NaN at distance 0; a wisp already on its target keeps its
                // velocity instead (the only visible difference, documented in
                // BD-17 — a NaN velocity makes the 1.21.1 particle engine drop
                // the whole spawn).
                this.xd += dx / distance * FxWispRules.SEEK_ACCELERATION;
                this.yd += dy / distance * FxWispRules.SEEK_ACCELERATION;
                this.zd += dz / distance * FxWispRules.SEEK_ACCELERATION;
                this.xd = net.minecraft.util.Mth.clamp(this.xd, -FxWispRules.SEEK_CLAMP, FxWispRules.SEEK_CLAMP);
                this.yd = net.minecraft.util.Mth.clamp(this.yd, -FxWispRules.SEEK_CLAMP, FxWispRules.SEEK_CLAMP);
                this.zd = net.minecraft.util.Mth.clamp(this.zd, -FxWispRules.SEEK_CLAMP, FxWispRules.SEEK_CLAMP);
            }
        } else {
            this.xd *= FxWispRules.FREE_DRAG;
            this.yd *= FxWispRules.FREE_DRAG;
            this.zd *= FxWispRules.FREE_DRAG;
            if (this.onGround) {
                this.xd *= FxWispRules.GROUND_DRAG;
                this.zd *= FxWispRules.GROUND_DRAG;
            }
        }
        // The render-time scale envelope (FXWisp.java:176-183).
        this.quadSize = 0.5F * this.moteScale * this.ageScale();
        this.setSpriteFromAge(this.sprites);
    }

    /**
     * The original's own push-out-of-blocks (FXWisp.java:214-297): inside a
     * solid non-water cell, the mote gets a small outward velocity
     * (rand*0.05+0.025 on the push axis, a (rand-rand)*0.1 jitter on the two
     * others — the original zeroes the other axes to the jitter). 1.21.1's
     * {@code Particle} has no such method; the decision lives in
     * {@link FxWispPush}, and the roll constants are the original's vars.
     */
    private void pushOutOfBlocks() {
        final int direction = FxWispPush.pushDirection(this.level, this.x, this.y, this.z);
        if (direction == FxWispPush.NONE) {
            return;
        }
        final double push = this.random.nextFloat() * 0.05 + 0.025;   // FXWisp.java:262
        final double jitter = (this.random.nextFloat() - this.random.nextFloat()) * 0.1;
        switch (direction) {                                          // FXWisp.java:264-292
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

    /** {@code shrink ? (lifetime-age)/lifetime : min(age/halfLife, 2 - age/halfLife)}. */
    private float ageScale() {
        if (this.shrink) {
            return (float) (this.lifetime - this.age) / (float) this.lifetime;
        }
        float scale = (float) this.age / (float) this.halfLife;
        return scale > 1.0F ? 2.0F - scale : scale;
    }

    /** The original draws its FX sheet fullbright (tessellator setBrightness(240)). */
    @Override
    protected int getLightColor(final float partialTick) {
        return 15 << 4 | 15 << 20;
    }

    @Override
    public ParticleRenderType getRenderType() {
        // FXWisp.getBlendMode (FXWisp.java:139-141) returns 1 (additive) only
        // for the branch that sets blendmode 771; everything else is the
        // alpha-blended sheet.
        return this.additive ? ParticleRenderType.PARTICLE_SHEET_LIT
                : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Provider wired from {@link TcParticleProviders} for the JSON frame list. */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcWispOptions> {

        @Override
        public Particle createParticle(final TcWispOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcWispParticle(level, x, y, z, vx, vy, vz, options, this.sprites);
        }
    }
}
