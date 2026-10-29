package com.jonnyutah12.thaumcraftclassic.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.Entity;

/**
 * The golem-boss impact sprite (TC-M7-02-b-s): the
 * {@code FXBeamGolemBoss.renderImpact} pass
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/beams/FXBeamGolemBoss.java:232-262)
 * ported as its own camera-facing particle, spawned next to each
 * {@link TcGolemBeamParticle} by the plan.
 *
 * <p>The original drew a billboarded quad at the target's interpolated
 * position with the particle-sheet explosion frames — {@code particles.png}
 * cells 80..95 (u = part/16, v = 0.3125, an 8-pixel cell grid), half-extent
 * {@code endMod / 4F * N}, alpha 0.66, additive, brightness 200. The port
 * keeps every number; the 16 frames arrive as the sliced
 * {@code impact_0..15} sprites of {@code particles/beam_impact.json}
 * (build-time slice of the original sheet, D-004). The camera-facing basis
 * the original receives from vanilla {@code RenderGlobal}
 * ({@code f1..f5}) is the 1.21.1 particle render's own camera rotation —
 * the BD-16 seam.</p>
 */
public final class TcBeamImpactParticle extends TextureSheetParticle {

    /** The impact alpha: 0.66 (FXBeamGolemBoss.java:237, 249). */
    static final float ALPHA = 0.66F;

    private final SpriteSet sprites;
    private final TcBeamOptions options;

    TcBeamImpactParticle(final ClientLevel level, final double x, final double y, final double z,
            final TcBeamOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.options = options;
        this.sprites = sprites;
        this.setLifetime(options.age());
        // renderImpact half-extent: endMod / 4F * N, endMod = 1 (FXBeamGolemBoss.java:245).
        this.quadSize = TcGolemBeamParticle.END_MOD / 4.0F * options.size();
        this.alpha = ALPHA;
        this.rCol = TcFxPlans.red(options.color());
        this.gCol = TcFxPlans.green(options.color());
        this.bCol = TcFxPlans.blue(options.color());
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.setSprite(sprites.get(0, 16));
        this.followTarget(level.getEntity(options.targetEntityId()));
    }

    @Override
    public void tick() {
        super.tick();
        // The impact tracks the target every tick (FXBeamGolemBoss.java:95-98,
        // 246-248): the original interpolates prev->current; 1.21.1 particles
        // render at the current position (the parent slice's seam).
        this.followTarget(this.level.getEntity(this.options.targetEntityId()));
        // int part = this.particleAge % 16 (FXBeamGolemBoss.java:240).
        this.setSprite(this.sprites.get(this.age % 16, 16));
    }

    private void followTarget(final Entity target) {
        if (target == null) {
            return;
        }
        this.x = target.getX();
        this.y = TcBeamGeometry.targetAnchorY(target.getBoundingBox().minY, target.getBbWidth());
        this.z = target.getZ();
    }

    @Override
    protected int getLightColor(final float partialTick) {
        return LightTexture.FULL_BRIGHT;   // tessellator setBrightness(200)
    }

    @Override
    public ParticleRenderType getRenderType() {
        // glBlendFunc(770, 1) (FXBeamGolemBoss.java:236) — additive.
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    /** Provider wired from {@link TcParticleProviders} (frames = sliced impact_0..15). */
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcBeamOptions> {

        @Override
        public Particle createParticle(final TcBeamOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcBeamImpactParticle(level, x, y, z, options, this.sprites);
        }
    }
}
