package com.jonnyutah12.thaumcraftclassic.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;

/**
 * The arc's companion glow sparkle (TC-M7-02-b-s): one instance is the
 * {@code FXSparkle} that {@code ClientProxy.arcLightning} adds at the arc's
 * target point
 * (reference/decompiled/thaumcraft/thaumcraft/client/ClientProxy.java:1302-1306,
 * FXSparkle.java).
 *
 * <p>Exact constants of the type-6 sparkle: lifetime {@code 3 * m = 6} with
 * multiplier {@code m = 2} (frame = {@code (age / 2) % 4}, FXSparkle.java:97,
 * 134-137), half-extent {@code 0.1F * particleScale} where
 * {@code particleScale = 0.1F * 3.0F = 0.3F} (FXSparkle.java:31, 140),
 * shrink on ({@code public boolean shrink = true}, FXSparkle.java:13; the
 * size factor {@code (maxAge - age + 1) / maxAge}, FXSparkle.java:106-108),
 * alpha 0.75 (the render's global glColor4f, FXSparkle.java:133), brightness
 * 240 (FXSparkle.java:149), additive (blendmode stays 1 → render layer 0,
 * FXSparkle.java:120-122), no motion and no collision (velocity zeroed at
 * FXSparkle.java:30; the sender sets {@code noClip}, ClientProxy.java:1304).
 * The colour is the packet's colour draw, applied after the constructor's
 * type-6 draws (the plan keeps the draw order). The four frames arrive as
 * the sliced {@code sparkle_glow_0..3} sprites — cells 32..35 of the
 * original {@code particles.png} (u = part % 4 / 16, v = 0.25, an 8-pixel
 * cell grid), sliced at build time (D-004).</p>
 */
public final class TcGlowSparkleParticle extends TextureSheetParticle {

    /** The sparkle alpha: glColor4f(1,1,1,0.75) (FXSparkle.java:133). */
    static final float ALPHA = 0.75F;
    /** The frame multiplier: {@code part = this.particle + age / multiplier} (FXSparkle.java:134). */
    static final int MULTIPLIER = 2;
    /** The 4-frame loop (FXSparkle.java:135: {@code part % 4}). */
    static final int FRAMES = 4;

    private final SpriteSet sprites;
    private final float baseSize;

    TcGlowSparkleParticle(final ClientLevel level, final double x, final double y, final double z,
            final TcGlowSparkleOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.setLifetime(options.age());
        // Half-extent 0.1F * particleScale, particleScale = 0.1F * scale (FXSparkle.java:31, 140).
        this.baseSize = 0.1F * 0.1F * options.scale();
        this.quadSize = this.baseSize;
        this.alpha = ALPHA;
        this.rCol = TcFxPlans.red(options.color());
        this.gCol = TcFxPlans.green(options.color());
        this.bCol = TcFxPlans.blue(options.color());
        this.gravity = 0.0F;      // setGravity(0.0F) (ClientProxy.java:1303)
        this.hasPhysics = false;  // the sender's noClip (ClientProxy.java:1304)
        this.setSprite(sprites.get(0, FRAMES));
    }

    @Override
    public void tick() {
        super.tick();
        // Frame animation (FXSparkle.java:134-135).
        this.setSprite(this.sprites.get((this.age / MULTIPLIER) % FRAMES, FRAMES));
        // Shrink (FXSparkle.java:106-108).
        this.quadSize = this.baseSize * (this.lifetime - this.age + 1) / this.lifetime;
    }

    @Override
    protected int getLightColor(final float partialTick) {
        return LightTexture.FULL_BRIGHT;   // tessellator setBrightness(240)
    }

    @Override
    public ParticleRenderType getRenderType() {
        // Type 6 keeps blendmode 1 → the additive layer (FXSparkle.java:120-122).
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    /** Provider wired from {@link TcParticleProviders} (frames = sliced sparkle_glow_0..3). */
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcGlowSparkleOptions> {

        @Override
        public Particle createParticle(final TcGlowSparkleOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcGlowSparkleParticle(level, x, y, z, options, this.sprites);
        }
    }
}
