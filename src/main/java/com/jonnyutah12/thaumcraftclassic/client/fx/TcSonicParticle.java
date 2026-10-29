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
 * The ported sonic pulse (TC-M7-02-c-s): one {@code FXSonic}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/other/FXSonic.java).
 *
 * <p>Entity-space FX with no physics: the ctor anchors it at the target's
 * eye height ({@code y = target.y + eyeHeight}, FXSonic.java:37-39) and the
 * render keeps it there. {@code tick()} re-reads the target, advances the
 * 15-frame ripple strip ({@code min(15, (int)(14 * fade) + 1)}, render
 * line 58), and dies at {@code age >= lifetime}. The shell rides the
 * particle billboard at the render's {@code 0.25 * bbHeight} scale (line
 * 67) — the original's OBJ plane, offset {@code 2 * bbHeight + width/2}
 * along the entity's facing (line 66), is the same documented billboard
 * approximation as the shield runes (BD-17).</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcSonicParticle extends TextureSheetParticle {

    /** The 15 ripple frames of the strip (FXSonic.java:57-59). */
    static final int FRAMES = 15;

    private final SpriteSet sprites;
    private final TcSonicOptions options;
    private final int frameCount = FRAMES;

    TcSonicParticle(final ClientLevel level, final TcSonicOptions options,
            final SpriteSet sprites) {
        super(level, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.options = options;
        this.lifetime = options.lifetime();
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = false;                        // noClip true (FXSonic.java:32)
        this.rCol = 0.5F;                               // render line 60: b = 0.5
        this.gCol = 0.5F;
        this.bCol = 0.5F;
        this.alpha = 1.0F;                          // render line 68: alpha 1
        this.setSprite(sprites.get(0, this.frameCount));
    }

    @Override
    public void tick() {
        final Entity target = this.level.getEntity(this.options.targetEntityId());
        if (target == null || this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        // Ctor anchor (FXSonic.java:37-39): the eye height.
        this.setPos(target.getX(), target.getY() + target.getEyeHeight(), target.getZ());
        final float fade = (float) this.age / this.lifetime;
        final int frame = Math.min(this.frameCount - 1, (int) (14.0F * fade));
        this.setSprite(this.sprites.get(frame, this.frameCount));
        this.quadSize = 0.25F * target.getBbHeight();   // render line 67
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

    /** Provider wired from {@link TcParticleProviders} for the ripple frame set. */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcSonicOptions> {

        @Override
        public Particle createParticle(final TcSonicOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcSonicParticle(level, options, this.sprites);
        }
    }
}
