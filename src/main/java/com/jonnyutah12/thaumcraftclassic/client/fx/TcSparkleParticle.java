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
 * The ported generic sparkle particle (TC-M7-02-s).
 *
 * <p>One instance is one {@code FXGeneric}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXGeneric.java):
 * lifetime {@code age + delay} (line 35-39), sprite frame advancing over that
 * lifetime (line 48-56: {@code start + min(num * age/maxAge, num-1)}, which is
 * what {@link TextureSheetParticle#setSpriteFromAge(SpriteSet)} does over the
 * frames of {@code assets/thaumcraft/particles/sparkle.json}), no collision
 * ({@code noClip}, line 24), and the render gate + edge fade of lines 58-67:
 * nothing is drawn before {@code delay}, and the alpha is halved on the first
 * and last tick.</p>
 *
 * <p>Motion: 1.21.1's {@code Particle.tick()} integrates
 * {@code yd -= 0.04 * gravity} then multiplies the velocity by
 * {@code friction} — the same shape the original's vanilla {@code EntityFX}
 * base tick has, so the two knobs carry the original's per-tick drag and
 * gravity directly. {@code FXGeneric} sets no downward gravity and the sender
 * hands an upward drift, so {@code gravity = 0} and {@code friction = 0.96}.
 * The 1.7.10 {@code EntityFX} base class is vanilla code and is not in the
 * reference tree, so that drag/gravity pair is the documented approximation of
 * it (BD-15 in docs/BEHAVIORAL_DEVIATIONS.md); the spawn geometry that IS in
 * the tree is reproduced exactly in {@link TcFxPlans}.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcSparkleParticle extends TextureSheetParticle {

    /** The original's EntityFX per-tick velocity drag (see the class javadoc, BD-15). */
    static final float DRAG = 0.96F;

    private final SpriteSet sprites;
    private final int delayTicks;
    private final float baseAlpha;

    TcSparkleParticle(final ClientLevel level, final double x, final double y, final double z,
            final double vx, final double vy, final double vz,
            final TcSparkleOptions options, final SpriteSet sprites) {
        // The 4-argument form: the 7-argument one re-normalises and randomises
        // the handed velocity, and the original integrates exactly what
        // drawGenericParticles passed.
        super(level, x, y, z);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.sprites = sprites;
        this.delayTicks = options.delay();
        this.baseAlpha = options.alpha();
        this.alpha = options.alpha();
        this.lifetime = options.age() + options.delay();
        // FXGeneric render: half-extent = 0.1F * particleScale, and quadSize IS
        // the half-extent in 1.21.1 (SingleQuadParticle.renderRotatedQuad).
        this.quadSize = 0.1F * options.scale();
        this.rCol = TcFxPlans.red(options.color());
        this.gCol = TcFxPlans.green(options.color());
        this.bCol = TcFxPlans.blue(options.color());
        this.hasPhysics = false;   // the original's noClip
        this.gravity = 0.0F;
        this.friction = DRAG;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
        // FXGeneric.render gate + edge fade (FXGeneric.java:58-67).
        if (this.age < this.delayTicks) {
            this.alpha = 0.0F;
        } else if (this.age <= this.delayTicks + 1 || this.age >= this.lifetime - 1) {
            this.alpha = this.baseAlpha / 2.0F;
        } else {
            this.alpha = this.baseAlpha;
        }
    }

    /** The original draws its FX sheet fullbright (tessellator setBrightness(240)). */
    @Override
    protected int getLightColor(final float partialTick) {
        return 15 << 4 | 15 << 20;
    }

    @Override
    public ParticleRenderType getRenderType() {
        // The sparkle layer blends SRC_ALPHA + ONE (ParticleEngine.java:60-66,
        // glBlendFunc(770, 1)) — the additive sheet.
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    /** Provider wired from {@link TcParticleProviders} for the JSON frame list. */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcSparkleOptions> {

        @Override
        public Particle createParticle(final TcSparkleOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcSparkleParticle(level, x, y, z, vx, vy, vz, options, this.sprites);
        }
    }
}
