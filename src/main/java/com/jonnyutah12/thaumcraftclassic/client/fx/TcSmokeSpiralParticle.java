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
 * The ported smoke spiral (TC-M7-02-c-s-f5): one {@code FXSmokeSpiral}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXSmokeSpiral.java),
 * the boiler plume {@code ClientProxy.smokeSpiral} puts on a stack
 * (client/ClientProxy.java:643-649).
 *
 * <p>Position is pure age math, not physics: {@code onUpdate} (lines 27-37)
 * accumulates a per-tick step of {@code sin(a)·r/20} east, {@code cos(a)·r/20}
 * north and {@code 0.05·(1 - age/lifetime)} up, where
 * {@code a = toRadians(start + 720·age/lifetime)} — the mote sweeps two full
 * turns while it rises, the rise decaying to nothing. The render (lines 39-52)
 * adds the same angle's {@code (sin, 0, cos)} offset to the drawn position,
 * floors the mote at {@code miny + 0.1}, and draws the 5-frame strip
 * {@code 1 + age/lifetime·4} — the sliced cells 1..5.</p>
 *
 * <p>The billboard is hand-sized at {@code 0.15F·particleScale} (line 45) —
 * the ctor's 0.01 bounds are irrelevant — and its alpha is the render's vertex
 * 0.66 multiplied into {@code particleAlpha}. The draw offset and the floor are
 * axis-aligned, so they ride the simulated position rather than a render hook
 * (BD-16: nothing here reads the camera basis).</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcSmokeSpiralParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float radius;
    private final float startAngle;
    private final double floorY;

    TcSmokeSpiralParticle(final ClientLevel level, final double x, final double y, final double z,
            final TcSmokeSpiralOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.radius = options.radius();
        this.startAngle = options.startAngle();
        this.floorY = options.floorY();
        this.lifetime = Math.max(1, options.lifetime());
        this.gravity = 0.0F;                     // onUpdate integrates itself
        this.friction = 1.0F;
        this.hasPhysics = false;
        this.rCol = TcFxPlans.red(options.color());
        this.gCol = TcFxPlans.green(options.color());
        this.bCol = TcFxPlans.blue(options.color());
        this.alpha = TcSmokeSpiralOptions.ALPHA; // the render's vertex alpha (line 49)
        this.quadSize = TcSmokeSpiralOptions.SIZE_FACTOR;
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        final float progress = (float) this.age / this.lifetime;
        final double angle = Math.toRadians(
                this.startAngle + TcSmokeSpiralOptions.SWEEP_DEGREES * progress);
        // FXSmokeSpiral.java:32-36: the step accumulates the position. The
        // render adds this tick's (sin, 0, cos) offset on top (line 41-44) and
        // floors the mote at miny + 0.1 (line 46); both are axis-aligned.
        this.x += Math.sin(angle) * this.radius / 20.0;
        this.y += 0.05 * (1.0 - progress);
        this.z += Math.cos(angle) * this.radius / 20.0;
        if (this.y < this.floorY + TcSmokeSpiralOptions.FLOOR_LIFT) {
            this.y = this.floorY + TcSmokeSpiralOptions.FLOOR_LIFT;
        }
        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }
        // The 5-frame strip: setParticleTextureIndex(1 + age/lifetime * 4).
        this.setSprite(this.sprites.get(
                Mth.clamp(1 + (int) (progress * 4.0F), 1, TcSmokeSpiralOptions.FRAMES) - 1,
                TcSmokeSpiralOptions.FRAMES));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Provider wired from {@link TcParticleProviders} for the 5-frame strip. */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcSmokeSpiralOptions> {

        @Override
        public Particle createParticle(final TcSmokeSpiralOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcSmokeSpiralParticle(level, x, y, z, options, this.sprites);
        }
    }
}
