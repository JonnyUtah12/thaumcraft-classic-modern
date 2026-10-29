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
 * The ported EnderGoo wisp (TC-M7-02-c-s): one {@code FXWispEG}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXWispEG.java).
 *
 * <p>{@code tick()} transcribes its {@code onUpdate} (lines 81-96): while
 * airborne it carries the target's own velocity (lines 85-88 — the original
 * adds {@code target.motionX/motionZ}; 1.21.1's velocity is the same draw),
 * pushes out of blocks, integrates its motion, drags at 0.98 and grinds to
 * 0.85 on the ground (lines 90-91), and dies at {@code age++ >= lifetime}.
 * The ctor draws (gaussian×0.03 lateral, −0.05 fall, 0.4 scale,
 * 40-tick base, additive blend, near-black {@code rand*0.05} colour) are all
 * pre-drawn by {@link TcFxPlans#wispEg} and carried in
 * {@link TcWispEgOptions}.</p>
 *
 * <p>Render (lines 45-71): 13-frame strip indexed {@code age % 13}, quad
 * half-extent {@code 0.5 * 0.04}, vertex alpha {@code 0.2 * (1 - age/life)}
 * plus a 1024-block camera fade — the fade is left to 1.21.1's own particle
 * culling (BD-15 precedent).</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcWispEgParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final int targetId;

    TcWispEgParticle(final ClientLevel level, final double x, final double y, final double z,
            final TcWispEgOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.targetId = options.targetEntityId();
        this.lifetime = options.lifetime();
        this.xd = 0.0;
        this.yd = TcWispEgOptions.FALL;
        this.zd = 0.0;
        this.rCol = TcFxPlans.red(options.color());
        this.gCol = TcFxPlans.green(options.color());
        this.bCol = TcFxPlans.blue(options.color());
        this.quadSize = 0.5F * 0.1F * TcWispEgOptions.SCALE;  // particleScale = 0.1 * 0.4
        this.quadAlpha = 0.2F;                                 // ctor alpha base (agescale applied per tick)
        this.hasPhysics = true;                                // noClip false (FXWispEG.java:27)
        this.friction = 1.0F;
        this.gravity = 0.0F;
        this.setSprite(sprites.get(0, sprites.size()));
    }

    @Override
    public void tick() {
        this.xOld = this.x;
        this.yOld = this.y;
        this.zOld = this.z;
        final Entity target = this.targetId == TcWispOptions.NO_TARGET
                ? null : this.level.getEntity(this.targetId);
        if (target != null && !this.onGround) {
            // FXWispEG.java:85-88: carry the target's motion while airborne.
            this.x += target.getDeltaMovement().x;
            this.z += target.getDeltaMovement().z;
        }
        this.pushOutOfBlocks(this.x, this.y, this.z);
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.98;
        this.yd *= 0.98;
        this.zd *= 0.98;
        if (this.onGround) {
            this.xd *= 0.85;
            this.zd *= 0.85;
        }
        if (this.age++ >= this.lifetime) {
            this.kill();
            return;
        }
        // Render-side age fade + 13-frame strip (FXWispEG.java:47, 53, 59).
        final float agescale = 1.0F - (float) this.age / this.lifetime;
        this.quadAlpha = 0.2F * Math.max(0.0F, agescale);
        this.setSprite(this.sprites.get(this.age % 13, 13));
    }

    /** The original draws its FX sheet fullbright (setBrightness(240)). */
    @Override
    protected int getLightColor(final float partialTick) {
        return 15 << 4 | 15 << 20;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;   // blendmode 771
    }

    /** Provider wired from {@link TcParticleProviders} for the JSON frame list. */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcWispEgOptions> {

        @Override
        public Particle createParticle(final TcWispEgOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            // The ctor's lateral draws land in the spawn velocity (the plan
            // draws them into ParticleSpawn's vx/vz in the original order).
            final TcWispEgParticle particle = new TcWispEgParticle(level, x, y, z, options, this.sprites);
            particle.xd = vx;
            particle.zd = vz;
            return particle;
        }
    }
}
