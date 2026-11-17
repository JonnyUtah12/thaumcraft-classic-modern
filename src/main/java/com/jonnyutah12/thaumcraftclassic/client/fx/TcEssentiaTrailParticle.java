package com.jonnyutah12.thaumcraftclassic.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The ported essentia drain trail mote (TC-M7-02-c-s): one
 * {@code FXEssentiaTrail} (reference/decompiled/thaumcraft/thaumcraft/
 * client/fx/particles/FXEssentiaTrail.java).
 *
 * <p>{@code tick()} transcribes {@code onUpdate} (lines 62-97): the mote
 * rises ({@code yd += 0.01 * 0.2} — the trail's gravity rides upward),
 * pushes out of blocks, integrates, drags at 0.985, clamps its velocity to
 * ±0.05 <em>before</em> adding the target pull of
 * {@code 0.01 / min(1, dist)}, shrinks 0.98/tick within 2 blocks, and dies
 * below 0.2 scale. All ctor draws (scale wobble, sin-phased launch velocity
 * keyed on the client {@code count}, lifetime, colour jitter) are
 * pre-drawn by {@link TcFxPlans#essentiaTrailTick}.</p>
 *
 * <p>Render (lines 63-81): the fixed sheet cell 25 ({@code u = 0.5625..0.625},
 * {@code v = 0.0625..0.125} — the trail mote has no animation; the
 * {@code part} local on line 69 is dead code), vertex alpha 0.5 under
 * {@code glColor4f(1,1,1,0.5)}, light 220 (≈fullbright on 1.21.1), and the
 * square billboard's half-extent {@code 0.1 * particleScale * s} with
 * {@code s = sin((age - count)/5)*0.25 + 1}. The 64/32 render-distance cull
 * of lines 50-57 rides the platform's culling (BD-15 precedent).</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcEssentiaTrailParticle extends TextureSheetParticle {

    /** The single fixed frame of the mote (FXEssentiaTrail.java:63-66). */
    static final int FRAMES = 1;

    private final SpriteSet sprites;
    private final TcEssentiaTrailOptions options;
    private float scale;

    TcEssentiaTrailParticle(final ClientLevel level, final double x, final double y, final double z,
            final TcEssentiaTrailOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.options = options;
        this.scale = options.scale();
        this.lifetime = options.lifetime();
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = true;                        // noClip false (FXEssentiaTrail.java:46)
        this.rCol = TcFxPlans.red(options.color());
        this.gCol = TcFxPlans.green(options.color());
        this.bCol = TcFxPlans.blue(options.color());
        this.alpha = 0.5F;
        this.setSpriteFromAge(sprites);
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
        this.yd += TcEssentiaTrailOptions.LIFT;        // FXEssentiaTrail.java:55 (gravity rides up)
        this.move(this.xd, this.yd, this.zd);
        this.xd = clamp005(this.xd * 0.985);           // drag (line 68-70)
        this.yd = clamp005(this.yd * 0.985);
        this.zd = clamp005(this.zd * 0.985);
        this.xd = clamp005(this.xd);                   // clamp ±0.05 (line 71-73)
        this.yd = clamp005(this.yd);
        this.zd = clamp005(this.zd);
        final double dx = this.options.targetX() - this.x;
        final double dy = this.options.targetY() - this.y;
        final double dz = this.options.targetZ() - this.z;
        final double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d < 2.0) {
            this.scale *= 0.98F;
        }
        if (this.scale < 0.2F) {
            this.remove();
            return;
        }
        if (d > 1.0E-6) {
            final double k = 0.01 / Math.min(1.0, d);
            this.xd += dx / d * k;
            this.yd += dy / d * k;
            this.zd += dz / d * k;
        }
        // Render-side scale wobble and fixed frame (FXEssentiaTrail.java:67, 69, 72):
        // s = sin((age - count)/5)*0.25 + 1 and the half-extent is 0.1 * particleScale * s.
        final float wobble = (float) Math.sin((this.age - this.options.count()) / 5.0) * 0.25F + 1.0F;
        this.quadSize = 0.1F * this.scale * wobble;
        this.setSprite(this.sprites.get(0, FRAMES));
    }

    /** {@code MathHelper.clampFloat(v, -0.05, 0.05)} (FXEssentiaTrail.java:71-73). */
    private static double clamp005(final double v) {
        return Math.max(-0.05, Math.min(0.05, v));
    }

    /** The original draws its FX sheet fullbright (setBrightness(240)). */
    @Override
    protected int getLightColor(final float partialTick) {
        return 15 << 4 | 15 << 20;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    /** Provider wired from {@link TcParticleProviders} for the JSON frame list. */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcEssentiaTrailOptions> {

        @Override
        public Particle createParticle(final TcEssentiaTrailOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            final TcEssentiaTrailParticle particle =
                    new TcEssentiaTrailParticle(level, x, y, z, options, this.sprites);
            particle.xd = vx;
            particle.yd = vy;
            particle.zd = vz;
            return particle;
        }
    }
}
