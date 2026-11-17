package com.jonnyutah12.thaumcraftclassic.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The ported vent puff (TC-M7-02-c-s-f5): one {@code FXVent}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXVent.java),
 * the steam puff {@code ClientProxy.drawVentParticles} throws at a vent
 * (client/ClientProxy.java:1216-1219 and the scaled overload 1254-1258).
 *
 * <p>The puff <em>grows</em> as it dies: {@code onUpdate} (lines 68-86) moves
 * the velocity, adds a constant {@code +0.0025} of lift, drags every axis by
 * 0.85, multiplies the scale by 1.15 while it is under the {@code psm} ceiling,
 * and drags the horizontal axes by 0.7 on the ground. The scale is a state
 * variable, kept apart from {@code quadSize}: the render's billboard is
 * {@code 0.3F·particleScale} (line 101) and its alpha
 * {@code particleAlpha·(psm - scale)/psm} (line 105) — so the puff fades out
 * exactly as it reaches its full size, and the frame is
 * {@code 1 + scale/psm·4}, the sliced cells 1..5.</p>
 *
 * <p>The ctor's 0.02 bounds never reach the quad, and the {@code onGround}
 * branch is dead in the original too: {@code field_70145_X = true} (line 18)
 * makes the 1.7.10 {@code moveEntity} skip collision, which is the only writer
 * of that flag, so the ported {@code hasPhysics = false} keeps it dead for the
 * same reason.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcVentParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    /** The original's {@code particleScale} growth state; {@code psm} is its ceiling. */
    private float scale;
    private final float psm;
    private final float baseAlpha;

    TcVentParticle(final ClientLevel level, final double x, final double y, final double z,
            final double vx, final double vy, final double vz,
            final TcVentOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.scale = options.scale();
        this.psm = options.shrinkCeiling();
        this.baseAlpha = options.alpha();
        this.lifetime = TcVentOptions.LIFETIME;  // the EntityFX default; the ctor leaves it
        this.gravity = 0.0F;                     // onUpdate adds its own lift
        this.friction = 1.0F;
        this.hasPhysics = false;                 // field_70145_X = true (line 18)
        this.rCol = TcFxPlans.red(options.color());
        this.gCol = TcFxPlans.green(options.color());
        this.bCol = TcFxPlans.blue(options.color());
        this.quadSize = TcVentOptions.SIZE_FACTOR * this.scale;
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        // FXVent.java:71-77: the move, the constant lift, then the drag.
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
        this.yd += TcVentOptions.RISE;
        this.xd *= TcVentOptions.DRAG;
        this.yd *= TcVentOptions.DRAG;
        this.zd *= TcVentOptions.DRAG;
        // Lines 78-80: the puff grows until it hits the ceiling.
        if (this.scale < this.psm) {
            this.scale *= TcVentOptions.GROWTH;
        }
        // Lines 81-84: the ground drag (dead with no-clip physics, as in 1.7.10).
        if (this.onGround) {
            this.xd *= TcVentOptions.GROUND_DRAG;
            this.zd *= TcVentOptions.GROUND_DRAG;
        }
        // The render's billboard and alpha (lines 101, 105), and the frame
        // ramp 1 + scale/psm*4 → the sliced cells 1..5 (line 95).
        this.quadSize = TcVentOptions.SIZE_FACTOR * this.scale;
        this.alpha = this.baseAlpha * ((this.psm - this.scale) / this.psm);
        this.setSprite(this.sprites.get(
                Mth.clamp(1 + (int) (this.scale / this.psm * 4.0F), 1, TcVentOptions.FRAMES) - 1,
                TcVentOptions.FRAMES));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Provider wired from {@link TcParticleProviders} for the 5-frame strip. */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcVentOptions> {

        @Override
        public Particle createParticle(final TcVentOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcVentParticle(level, x, y, z, vx, vy, vz, options, this.sprites);
        }
    }
}
