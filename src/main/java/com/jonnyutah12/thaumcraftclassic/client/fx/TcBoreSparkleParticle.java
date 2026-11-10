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
 * The ported infusion draw mote (TC-M7-02-c-s): one {@code FXBoreSparkle}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXBoreSparkle.java).
 *
 * <p>{@code tick()} transcribes {@code onUpdate} (lines 90-116): the mote
 * keeps flying only while its block coords differ from the target's (the
 * floor test, line 94-97 — arrival at the matrix block kills it), pushes
 * out of blocks, integrates, drags at 0.985, and homes with attraction 0.6
 * (0.3 beyond 4 blocks) clamped to ±0.35, shrinking 0.9/tick inside 4
 * blocks. Render: 4-frame strip (particles.png cells 64..67,
 * {@code age % 4}), fullbright, bob {@code sin(age/3)*0.5+1}, vertex alpha
 * 0.75. The 64/32 render-distance cull rides the platform's culling (BD-15).</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcBoreSparkleParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final TcBoreSparkleOptions options;
    private float scale;

    TcBoreSparkleParticle(final ClientLevel level, final double x, final double y, final double z,
            final TcBoreSparkleOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.options = options;
        this.scale = options.scale();
        this.lifetime = options.lifetime();
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = true;                        // noClip false (FXBoreSparkle.java:38)
        this.rCol = TcFxPlans.red(options.color());
        this.gCol = TcFxPlans.green(options.color());
        this.bCol = TcFxPlans.blue(options.color());
        this.alpha = 0.75F;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        // FXBoreSparkle.java:94-97: alive only while age < life AND some axis's
        // block coord still differs from the target's.
        if (this.age++ >= this.lifetime
                || (Math.floor(this.x) == Math.floor(this.options.targetX())
                        && Math.floor(this.y) == Math.floor(this.options.targetY())
                        && Math.floor(this.z) == Math.floor(this.options.targetZ()))) {
            this.remove();
            return;
        }
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.985;
        this.yd *= 0.985;
        this.zd *= 0.985;
        double pull = 0.3;
        final double dx = this.options.targetX() - this.x;
        final double dy = this.options.targetY() - this.y;
        final double dz = this.options.targetZ() - this.z;
        final double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d < 4.0) {
            this.scale *= 0.9F;
            pull = 0.6;
        }
        if (d > 1.0E-6) {
            this.xd = clamp035(this.xd + dx / d * pull);
            this.yd = clamp035(this.yd + dy / d * pull);
            this.zd = clamp035(this.zd + dz / d * pull);
        }
        final float bob = (float) Math.sin(this.age / 3.0) * 0.5F + 1.0F;
        this.quadSize = 0.1F * this.scale * bob;
        this.setSprite(this.sprites.get(this.age % 4, 4));
    }

    /** {@code MathHelper.clampFloat(v, -0.35, 0.35)} (FXBoreSparkle.java:111-113). */
    private static double clamp035(final double v) {
        return Math.max(-0.35, Math.min(0.35, v));
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
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcBoreSparkleOptions> {

        @Override
        public Particle createParticle(final TcBoreSparkleOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            final TcBoreSparkleParticle particle =
                    new TcBoreSparkleParticle(level, x, y, z, options, this.sprites);
            particle.xd = vx;
            particle.yd = vy;
            particle.zd = vz;
            return particle;
        }
    }
}
