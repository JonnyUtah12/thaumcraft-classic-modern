package com.jonnyutah12.thaumcraftclassic.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The ported shield runes (TC-M7-02-c-s): one {@code FXShieldRunes}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/other/FXShieldRunes.java).
 *
 * <p>This is entity-space FX: the particle has no physics of its own — it
 * rides its owner. {@code tick()} transcribes the ctor (lines 27-45) and the
 * no-clip entity-tracking render: the position is re-read from the target
 * every tick at {@code (x, (bbMinY + bbMaxY) / 2, z)} (the ctor anchors it
 * there, lines 41-43), the frame advances
 * {@code min(15, (int)(14 * fade) + 1)} with {@code fade = age / lifetime}
 * (render, line 60) over the 15-frame hemis/ripple strip, the alpha fades as
 * {@code min(1, (1 - fade) * 3)} (line 76), and the shell scales with the
 * owner: {@code 0.4 * bbHeight} (line 75).</p>
 *
 * <p>The original renders the frames on an OBJ hemisphere model turned to
 * the packet's yaw/pitch; 1.21.1 has no OBJ particle path, so the frame
 * rides the particle billboard at the same scale (BD row in
 * docs/BEHAVIORAL_DEVIATIONS.md; yaw/pitch are carried in the payload so
 * the geometry survives for a future model render).</p>
 *
 * <p>Target gone → the particle dies (the original NPE-guarded through
 * {@code target}; without a target there is nothing to render).</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcShieldRunesParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final TcShieldOptions options;
    private final int frameCount;

    TcShieldRunesParticle(final ClientLevel level, final TcShieldOptions options,
            final SpriteSet sprites) {
        super(level, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.options = options;
        this.frameCount = sprites.size();
        this.lifetime = options.lifetime();
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = false;                        // noClip true (FXShieldRunes.java:34)
        this.quadAlpha = 1.0F;
        this.setSprite(sprites.get(0, this.frameCount));
    }

    @Override
    public void tick() {
        final Entity target = this.level.getEntity(this.options.targetEntityId());
        if (target == null || this.age++ >= this.lifetime) {
            this.kill();
            return;
        }
        // Ctor anchor (lines 41-43): x/z are the entity's, y the AABB centre.
        this.setPos(target.getX(),
                (target.getBoundingBox().minY + target.getBoundingBox().maxY) / 2.0,
                target.getZ());
        final float fade = (float) this.age / this.lifetime;
        final int frame = Math.min(this.frameCount - 1, (int) (14.0F * fade));
        this.setSprite(this.sprites.get(frame, this.frameCount));
        final float b = this.options.mob() ? 0.5F : 1.0F;   // render line 61-66
        this.rCol = b;
        this.gCol = b;
        this.bCol = b;
        this.quadAlpha = Math.min(1.0F, (1.0F - fade) * 3.0F);
        this.quadSize = 0.4F * target.getBbHeight();        // render line 75
    }

    /** The original draws its shell at light 220 — fullbright on 1.21.1. */
    @Override
    protected int getLightColor(final float partialTick) {
        return 15 << 4 | 15 << 20;
    }

    @Override
    public ParticleRenderType getRenderType() {
        // glBlendFunc(770, 1) (render line 34) — the additive sheet.
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    /** Provider wired from {@link TcParticleProviders} for the hemis frame set. */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcShieldOptions> {

        @Override
        public Particle createParticle(final TcShieldOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcShieldRunesParticle(level, options, this.sprites);
        }
    }
}
