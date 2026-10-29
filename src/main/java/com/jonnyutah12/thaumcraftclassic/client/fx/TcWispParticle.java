package com.jonnyutah12.thaumcraftclassic.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The ported wisp particle (TC-M7-02-c-s).
 *
 * <p>One instance is one {@code FXWisp}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXWisp.java),
 * and {@link #tick()} transcribes its {@code onUpdate} (lines 164-212)
 * line-for-line:
 * <ul>
 *   <li>age 0: the {@code tinkle} gate plays {@code random.orb} at volume
 *       0.02 with pitch {@code 0.5 * ((r - r) * 0.6 + 2)} (line 168-173);</li>
 *   <li>{@code age++ >= lifetime} kills (line 175) — the original kept
 *       integrating that final tick, and the port kills first: the position
 *       the original reached is discarded together with the particle, so the
 *       behaviours coincide;</li>
 *   <li>{@code yd -= 0.04 * gravity} (line 179 — the 1.7.10 {@code EntityFX}
 *       base gravity, applied by the FX itself here, not by a base tick),
 *       block push-out (line 180-182), position integration with collision
 *       (line 184);</li>
 *   <li>with a target: drag 0.985 and a 0.2/tick attraction toward
 *       {@code (target.x, target.y + target.height/2, target.z)} clamped to
 *       ±0.2 (lines 185-202; the original divides by the raw distance — the
 *       port guards the divide-by-zero, see the class's BD reference);</li>
 *   <li>without: drag 0.98, plus xz×0.7 while on the ground (lines 203-211).</li>
 * </ul>
 * The render pass applies the scale curve (lines 132-144) — a linear
 * {@code (life - age) / life} fade when {@code shrink}, the triangle pulse
 * {@code age / halfLife, 2 - x} otherwise — and the 0.5 vertex alpha;
 * the quad half-extent is {@code 0.5 * particleScale} (line 144).</p>
 *
 * <p>The 50/25 render-distance cull in the ctor (lines 50-56) is left to
 * 1.21.1's own particle culling (BD-15 precedent).</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcWispParticle extends TextureSheetParticle {

    /** The original's attraction strength toward a wispFX4 target (FXWisp.java:192). */
    private static final double TARGET_PULL = 0.2;

    private final SpriteSet sprites;
    private final TcWispOptions options;
    private final Entity target;
    private final int halfLife;
    private final float moteScale;

    TcWispParticle(final ClientLevel level, final double x, final double y, final double z,
            final TcWispOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.options = options;
        this.target = options.targetEntityId() == TcWispOptions.NO_TARGET
                ? null : level.getEntity(options.targetEntityId());
        this.lifetime = options.lifetime();
        this.halfLife = options.lifetime() / 2;
        this.moteScale = options.scale();
        this.xd = options.motX();
        this.yd = options.motY();
        this.zd = options.motZ();
        this.gravity = (float) options.gravity();
        this.rCol = TcFxPlans.red(options.color());
        this.gCol = TcFxPlans.green(options.color());
        this.bCol = TcFxPlans.blue(options.color());
        // FXWisp renders with vertex alpha 0.5 under the sheet's 0.75 glColor4f.
        this.alpha = 0.5F;
        // The original is a colliding EntityFX (noClip false, FXWisp.java:49);
        // wispFX2 sets noClip = clip (ClientProxy.java:706, field_70145_X).
        this.hasPhysics = !options.clip();
        this.friction = 1.0F;    // the drag lives in tick(): the base tick must not double it
        this.gravity = (float) options.gravity();
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age == 0 && this.options.tinkle() && this.random.nextInt(3) == 0) {
            // FXWisp.java:168-173: world rand, volume 0.02, pitch 0.5*((r-r)*0.6+2).
            final float pitch = 0.5F * ((this.random.nextFloat() - this.random.nextFloat()) * 0.6F + 2.0F);
            this.level.playLocalSound(this.x, this.y, this.z,
                    SoundEvent.createVariableRangeEvent(
                            net.minecraft.resources.ResourceLocation.withDefaultNamespace("random.orb")),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.02F, pitch, true);
        }
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.yd -= 0.04 * this.gravity;
        // FXWisp.java:180-182 pushOutOfBlocks has no 1.21.1 counterpart: the
        // Particle base class is not an Entity and carries no block-collision
        // push-out (BD-16 in docs/BEHAVIORAL_DEVIATIONS.md). The collision of
        // the integration below is the platform's own (hasPhysics).
        this.move(this.xd, this.yd, this.zd);
        if (this.target != null) {
            this.xd *= 0.985;
            this.yd *= 0.985;
            this.zd *= 0.985;
            final double dx = this.target.getX() - this.x;
            final double dy = this.target.getY() + this.target.getBbHeight() / 2.0 - this.y;
            final double dz = this.target.getZ() - this.z;
            final double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d > 1.0E-6) {
                this.xd = clamp02(this.xd + dx / d * TARGET_PULL);
                this.yd = clamp02(this.yd + dy / d * TARGET_PULL);
                this.zd = clamp02(this.zd + dz / d * TARGET_PULL);
            }
        } else {
            this.xd *= 0.98;
            this.yd *= 0.98;
            this.zd *= 0.98;
            if (this.onGround) {
                this.xd *= 0.7;
                this.zd *= 0.7;
            }
        }
        // Render-side scale curve (FXWisp.java:132-144), applied per tick.
        float agescale;
        if (this.options.shrink()) {
            agescale = (float) (this.lifetime - this.age) / this.lifetime;
        } else {
            agescale = (float) this.age / this.halfLife;
            if (agescale > 1.0F) {
                agescale = 2.0F - agescale;
            }
        }
        this.quadSize = 0.5F * this.moteScale * 0.1F * Math.max(0.0F, agescale);
        this.setSpriteFromAge(this.sprites);
    }

    /** {@code MathHelper.clampFloat(v, -0.2, 0.2)} (FXWisp.java:200-202). */
    private static double clamp02(final double v) {
        return Math.max(-TARGET_PULL, Math.min(TARGET_PULL, v));
    }

    /** The original draws its FX sheet fullbright (setBrightness(240)). */
    @Override
    protected int getLightColor(final float partialTick) {
        return 15 << 4 | 15 << 20;
    }

    @Override
    public ParticleRenderType getRenderType() {
        // blendmode 771 (type 5 wisps) rides the additive sheet; blendmode 1
        // rides the translucent one (FXWisp.java:160-162).
        return this.options.additive()
                ? ParticleRenderType.PARTICLE_SHEET_LIT
                : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Provider wired from {@link TcParticleProviders} for the JSON frame list. */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcWispOptions> {

        @Override
        public Particle createParticle(final TcWispOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcWispParticle(level, x, y, z, options, this.sprites);
        }
    }
}
