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
 * The ported vis-drain sparkle (TC-M7-02-c-s): one {@code FXVisSparkle}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXVisSparkle.java).
 *
 * <p>{@code tick()} transcribes {@code onUpdate} (lines 68-96): 0.985 drag
 * and a 0.1/tick attraction toward the target clamped to ±0.1 (the drain
 * motes stream slowly in), the 0.95 scale shrink inside 2 blocks, the
 * "arrival" kill at distance &lt; 0.2, and the growth ramp
 * {@code scale = age / sizeMod} for the first 10 ticks. Lifetime is the
 * ctor's 1000-tick cap (the mote's real end is arrival). No-clip
 * ({@code field_70145_X = true}, line 32).</p>
 *
 * <p>Render (lines 45-66): fullbright 16-frame strip (particles.png cells
 * 128..143, {@code part = age % 16}, {@code u = part/16}, {@code v = 0.5}),
 * vertex alpha 0.5, and a square billboard whose half-extent is
 * {@code 0.1 * particleScale * bob} with {@code bob = sin(age/3)*0.3 + 6} —
 * the bob scales the whole spark, not one axis (all four corners use the same
 * {@code var12}). The 64/32 render-distance cull of lines 38-43 rides the
 * platform's culling (BD-15 precedent).</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcVisSparkleParticle extends TextureSheetParticle {

    /** {@code particleMaxAge = 1000} (FXVisSparkle.java:24). */
    static final int LIFETIME = 1000;

    /** The ctor's gaussian velocity multiplier 0.01 (FXVisSparkle.java:26-28). */
    static final double START_VELOCITY = 0.01;

    /** {@code sizeMod} range: 45 + nextInt(15) (FXVisSparkle.java:29). */
    static final int SIZE_MOD_BASE = 45;
    static final int SIZE_MOD_SPAN = 15;

    /** The 16 frames of the strip (FXVisSparkle.java:49-53). */
    static final int FRAMES = 16;

    private final SpriteSet sprites;
    private final TcVisSparkleOptions options;
    private float scale;

    TcVisSparkleParticle(final ClientLevel level, final double x, final double y, final double z,
            final TcVisSparkleOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.options = options;
        this.lifetime = LIFETIME;
        this.scale = 0.0F;                     // particleScale = 0 at spawn (line 20)
        this.gravity = 0.0F;                   // onUpdate integrates itself
        this.friction = 1.0F;
        this.hasPhysics = false;               // the original's noClip
        this.rCol = TcFxPlans.red(options.color());
        this.gCol = TcFxPlans.green(options.color());
        this.bCol = TcFxPlans.blue(options.color());
        this.alpha = 0.5F;                     // tessellator color alpha (line 60)
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
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.985;
        this.yd *= 0.985;
        this.zd *= 0.985;
        final double dx = this.options.targetX() - this.x;
        final double dy = this.options.targetY() - this.y;
        final double dz = this.options.targetZ() - this.z;
        final double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d < 2.0) {
            this.scale *= 0.95F;
        }
        if (d < 0.2) {
            this.remove();                     // FXVisSparkle.java:63-65: lifetime = age
            return;
        }
        if (this.age < 10) {
            this.scale = (float) this.age / this.options.sizeMod();
        }
        if (d > 1.0E-6) {
            this.xd = clamp01(this.xd + dx / d * 0.1);
            this.yd = clamp01(this.yd + dy / d * 0.1);
            this.zd = clamp01(this.zd + dz / d * 0.1);
        }
        // Render-side bob and frame (FXVisSparkle.java:46, 49, 52).
        final float bob = (float) Math.sin(this.age / 3.0) * 0.3F + 6.0F;
        this.quadSize = 0.1F * this.scale * bob;
        this.setSprite(this.sprites.get(this.age % FRAMES, FRAMES));
    }

    /** {@code MathHelper.clampFloat(v, -0.1, 0.1)} (FXVisSparkle.java:94-96). */
    private static double clamp01(final double v) {
        return Math.max(-0.1, Math.min(0.1, v));
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
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcVisSparkleOptions> {

        @Override
        public Particle createParticle(final TcVisSparkleOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            final TcVisSparkleParticle particle =
                    new TcVisSparkleParticle(level, x, y, z, options, this.sprites);
            particle.xd = vx;
            particle.yd = vy;
            particle.zd = vz;
            return particle;
        }
    }
}
