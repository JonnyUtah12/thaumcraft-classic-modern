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
 * The ported node burst (TC-M7-02-c-s-f5): one {@code FXBurst}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXBurst.java),
 * the expanding ring {@code ClientProxy.burst} puts on a node
 * (client/ClientProxy.java:747-751).
 *
 * <p>The class is a pure render: {@code onUpdate} (lines 62-66) only ages the
 * mote, and every visual is in the render — the 32-frame node-sheet strip
 * {@code u = age % 32 / 32, v = 0.96875}, the billboard at
 * {@code particleScale = 1 · size}, the additive blend, brightness 240, alpha
 * 0.75. The port reproduces the strip as the frame list, the billboard as
 * {@code quadSize} and the additive blend as the translucent sheet.</p>
 *
 * <p>Art seam (BD-19, shared with the wisp): the strip's source sheet is
 * {@code TileNodeRenderer.nodetex}, which the reference tree does not ship
 * (no {@code nodetex} under reference/assets), so the frames stand in on the
 * Thaumcraft glow sparkle frame; the frame COUNT — 32, indexed by
 * {@code age % 32} — stays the original's, which is what the slicing test and
 * the definition assert.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcBurstParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    TcBurstParticle(final ClientLevel level, final double x, final double y, final double z,
            final TcBurstOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.lifetime = TcBurstOptions.LIFETIME;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = false;                 // field_70145_X = false in the ctor
        this.rCol = 1.0F;                        // FXBurst.java:14-16
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.alpha = TcBurstOptions.ALPHA;
        this.quadSize = options.size() * 0.5F;   // the billboard half-extent (line 34)
        this.setSprite(this.sprites.get(0, TcBurstOptions.FRAMES));
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        // FXBurst.java:62-66: the body is the age, and nothing else.
        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }
        this.setSprite(this.sprites.get(this.age % TcBurstOptions.FRAMES, TcBurstOptions.FRAMES));
    }

    /** The shell's flat {@code setBrightness(240)} (FXBurst.java:38). */
    @Override
    protected int getLightColor(final float partialTick) {
        return 15 << 4 | 15 << 20;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Provider wired from {@link TcParticleProviders} for the 32-frame strip. */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcBurstOptions> {

        @Override
        public Particle createParticle(final TcBurstOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcBurstParticle(level, x, y, z, options, this.sprites);
        }
    }
}
