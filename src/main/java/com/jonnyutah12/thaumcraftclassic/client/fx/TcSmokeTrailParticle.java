package com.jonnyutah12.thaumcraftclassic.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The ported smoke trail (TC-M7-02-c-s-f5): one {@code FXSmokeTrail}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXSmokeTrail.java),
 * the smoke ribbon {@code ClientProxy.smokeTrailFX} puts on an entity
 * (client/ClientProxy.java:918-941).
 *
 * <p>Entity-space, so the payload carries the target's id (BD-49): the tick
 * body is {@code onUpdate} (lines 70-107) — while the age is under the lifetime
 * <em>and</em> the mote is still more than a block from the target, drag
 * 0.985, add {@code delta/dist * d13} toward the target's <em>feet</em>
 * ({@code target.posY}, not its middle), with {@code d13 = 0.3} normally and
 * {@code 0.6} — plus a 0.9 scale shrink — inside 4 blocks, and clamp every axis
 * to ±0.35. Failing that test the mote dies, which is the original's
 * {@code else setDead()}.</p>
 *
 * <p>Render (lines 51-63): the billboard is
 * {@code 0.3F·particleScale·(sin(age/3)·0.33 + 0.66)} on a 5-frame strip
 * {@code 1 + age/lifetime·4} (the sliced cells 1..5), alpha 0.33, and the
 * original's {@code getBrightnessForRender} is a flat 90 (line 65) — a dim
 * fullbright, not the block light.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcSmokeTrailParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final int targetEntityId;
    /**
     * The original's {@code particleScale} — the shrinking <em>state</em> the
     * onUpdate multiplies by 0.9 (FXSmokeTrail.java:93), kept apart from
     * {@code quadSize}, which is the render's billboard
     * {@code 0.3F·particleScale·bob} (line 57).
     */
    private float scale;

    TcSmokeTrailParticle(final ClientLevel level, final double x, final double y, final double z,
            final double vx, final double vy, final double vz,
            final TcSmokeTrailOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.targetEntityId = options.targetEntityId();
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.lifetime = Math.max(1, options.lifetime());
        this.gravity = 0.0F;                     // onUpdate integrates itself
        this.friction = 1.0F;
        this.hasPhysics = false;                 // the ctor sets the no-clip flag
        this.rCol = TcFxPlans.red(options.color());
        this.gCol = TcFxPlans.green(options.color());
        this.bCol = TcFxPlans.blue(options.color());
        this.alpha = TcSmokeTrailOptions.ALPHA;  // the render's vertex alpha (line 63)
        this.scale = options.scale();
        this.quadSize = TcSmokeTrailOptions.SIZE_FACTOR * this.scale;
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        // FXSmokeTrail.java:70: the whole body is gated on age < lifetime and
        // distance >= 1.0; failing it is the original's setDead().
        final Entity target = this.level.getEntity(this.targetEntityId);
        final double distance = target == null
                ? 0.0
                : Math.sqrt(target.distanceToSqr(this.x, this.y, this.z));
        if (this.age >= this.lifetime || target == null
                || target.isRemoved() || distance < TcSmokeTrailOptions.ARRIVAL_DISTANCE) {
            this.remove();
            return;
        }
        // Lines 73-76: the move happens first, then the drag.
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
        this.xd *= TcSmokeTrailOptions.DRAG;
        this.yd *= TcSmokeTrailOptions.DRAG;
        this.zd *= TcSmokeTrailOptions.DRAG;
        double dx = target.getX() - this.x;
        double dy = target.getY() - this.y;
        double dz = target.getZ() - this.z;
        double pull = TcSmokeTrailOptions.PULL;
        final double d11 = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d11 < TcSmokeTrailOptions.SHRINK_DISTANCE) {
            this.scale *= TcSmokeTrailOptions.SHRINK_FACTOR;
            pull = TcSmokeTrailOptions.PULL_CLOSE;
        }
        if (d11 > 1.0E-6) {
            dx /= d11;
            dy /= d11;
            dz /= d11;
            this.xd += dx * pull;
            this.yd += dy * pull;
            this.zd += dz * pull;
            this.xd = Mth.clamp(this.xd, -TcSmokeTrailOptions.CLAMP, TcSmokeTrailOptions.CLAMP);
            this.yd = Mth.clamp(this.yd, -TcSmokeTrailOptions.CLAMP, TcSmokeTrailOptions.CLAMP);
            this.zd = Mth.clamp(this.zd, -TcSmokeTrailOptions.CLAMP, TcSmokeTrailOptions.CLAMP);
        }
        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }
        // The render's bob and the 5-frame strip (lines 51-57).
        final float bob = (float) (Math.sin(this.age / 3.0) * TcSmokeTrailOptions.BOB_SPAN
                + TcSmokeTrailOptions.BOB_BASE);
        this.quadSize = TcSmokeTrailOptions.SIZE_FACTOR * this.scale * bob;
        this.setSprite(this.sprites.get(
                Mth.clamp(1 + (int) ((float) this.age / this.lifetime * 4.0F), 1,
                        TcSmokeTrailOptions.FRAMES) - 1,
                TcSmokeTrailOptions.FRAMES));
    }

    /** The original's {@code getBrightnessForRender} is a flat 90 (line 65). */
    @Override
    protected int getLightColor(final float partialTick) {
        return 15 << 4 | 5 << 20;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Provider wired from {@link TcParticleProviders} for the 5-frame strip. */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcSmokeTrailOptions> {

        @Override
        public Particle createParticle(final TcSmokeTrailOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcSmokeTrailParticle(level, x, y, z, vx, vy, vz, options, this.sprites);
        }
    }
}
