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
 * The ported arcing stream wisp (TC-M7-02-c-s): one {@code FXWispArcing}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXWispArcing.java).
 *
 * <p>Not a physics particle. {@code tick()} transcribes {@code onUpdate}
 * (lines 93-105) exactly: with {@code var1 = 1 - age/life} and
 * {@code var2 = 1 - age/(life/2)} squared, the position is
 * {@code start + delta * var1} (y additionally gets {@code -var2 + 1}, the
 * one-block arc bulge that peaks mid-flight). At age 0 the particle sits on
 * the <em>target</em> and it lands on the start — the stream draws
 * tip-first and the port keeps that. The triangle scale pulse
 * ({@code age / halfLife}, {@code 2 - x} past the midpoint, lines 62-69) is
 * applied per tick.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcWispArcingParticle extends TextureSheetParticle {

    private final double startX;
    private final double startY;
    private final double startZ;
    private final TcWispArcingOptions options;
    private final int halfLife;

    TcWispArcingParticle(final ClientLevel level, final double x, final double y, final double z,
            final TcWispArcingOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.startX = x;
        this.startY = y;
        this.startZ = z;
        this.options = options;
        this.lifetime = Math.max(1, options.lifetime());
        this.halfLife = this.lifetime / 2;
        this.rCol = TcFxPlans.red(options.color());
        this.gCol = TcFxPlans.green(options.color());
        this.bCol = TcFxPlans.blue(options.color());
        this.alpha = 0.5F;
        this.hasPhysics = false;   // parametric position: no collision, no motion
        this.friction = 1.0F;
        this.gravity = 0.0F;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        // FXWispArcing.java:96-102, transcribed (note: computed with the
        // pre-increment age, then the post-increment kill check runs).
        double var1 = 1.0 - (double) this.age / this.lifetime;
        double var2 = 1.0 - (double) this.age / (this.lifetime / 2.0);
        var2 *= var2;
        this.x = this.startX + this.options.deltaX() * var1;
        this.y = this.startY + this.options.deltaY() * var1 - var2 + 1.0;
        this.z = this.startZ + this.options.deltaZ() * var1;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        float agescale = (float) this.age / this.halfLife;
        if (agescale > 1.0F) {
            agescale = 2.0F - agescale;
        }
        this.quadSize = 0.5F * 0.1F * this.options.scale() * Math.max(0.0F, agescale);
    }

    /** The original draws its FX sheet fullbright (setBrightness(240)). */
    @Override
    protected int getLightColor(final float partialTick) {
        return 15 << 4 | 15 << 20;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;   // FXWispArcing rides the additive FX sheet
    }

    /** Provider wired from {@link TcParticleProviders} for the JSON frame list. */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcWispArcingOptions> {

        @Override
        public Particle createParticle(final TcWispArcingOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcWispArcingParticle(level, x, y, z, options, this.sprites);
        }
    }
}
