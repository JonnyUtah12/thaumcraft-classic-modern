package com.jonnyutah12.thaumcraftclassic.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The ported vis droplet (TC-M7-02-c-s-f5): one {@code FXDrop}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXDrop.java),
 * the coloured drip of {@code ClientProxy.dropletFX}
 * (client/ClientProxy.java:1137-1141).
 *
 * <p>{@code tick()} is {@code onUpdate} (lines 40-85): the drop falls under its
 * own 0.06 gravity, and for the first 40 ticks (its {@code bobTimer}) it crushes
 * its velocity to 0.02 of itself — a mote that hangs, then releases — before
 * the 0.98 drag. The frame is the class's own three indices: 113 while bobbing,
 * 112 once spent, 114 the tick it stands on the ground (with the 0.7 ground
 * drag). The last branch is the death-in-liquid test (lines 74-84): a mote that
 * enters a non-water liquid below its surface level dies.</p>
 *
 * <p>1.21.1 seam (BD-82): the class overrides {@code moveEntity} with a
 * hand-rolled collision walk whose only non-vanilla rule is to ignore the
 * jar block's collision (line 89). The port rides the platform's own physics,
 * so that jar exception is not reproduced; everything else in the walk is the
 * vanilla step the platform already does.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcDropParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private int bobTimer;

    TcDropParticle(final ClientLevel level, final double x, final double y, final double z,
            final TcDropOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.rCol = TcFxPlans.red(options.color());
        this.gCol = TcFxPlans.green(options.color());
        this.bCol = TcFxPlans.blue(options.color());
        this.gravity = TcDropOptions.GRAVITY;
        this.friction = 1.0F;                    // the mote drags its own velocity
        this.bobTimer = TcDropOptions.BOB_TICKS;
        this.lifetime = Math.max(1, options.lifetime());
        this.quadSize = 0.2F * 0.5F;             // the EntityFX default bound
        this.setSprite(sprite(TcDropOptions.CELL_BOBBING));
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        // FXDrop.java:44: the gravity is applied before the bob crush.
        this.yd -= TcDropOptions.GRAVITY;
        if (this.bobTimer-- > 0) {
            this.xd *= TcDropOptions.BOB_CRUSH;
            this.yd *= TcDropOptions.BOB_CRUSH;
            this.zd *= TcDropOptions.BOB_CRUSH;
            this.setSprite(sprite(TcDropOptions.CELL_BOBBING));
        } else {
            this.setSprite(sprite(TcDropOptions.CELL_SPENT));
        }
        this.move(this.xd, this.yd, this.zd);
        this.xd *= TcDropOptions.DRAG;
        this.yd *= TcDropOptions.DRAG;
        this.zd *= TcDropOptions.DRAG;
        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }
        if (this.onGround) {
            this.setSprite(sprite(TcDropOptions.CELL_LANDED));
            this.xd *= TcDropOptions.GROUND_DRAG;
            this.zd *= TcDropOptions.GROUND_DRAG;
        }
        // Lines 74-84: a mote inside a non-water liquid dies at its surface.
        final BlockPos block = BlockPos.containing(this.x, this.y, this.z);
        final FluidState fluid = this.level.getFluidState(block);
        if (!fluid.isEmpty() && !fluid.is(net.minecraft.world.level.material.Fluids.WATER)) {
            final double surface = block.getY() + 1.0
                    - fluid.getOwnHeight();
            if (this.y < surface) {
                this.remove();
            }
        }
    }

    /** The class's own {@code setParticleTextureIndex(cell)} → a frame of the strip. */
    private net.minecraft.client.renderer.texture.TextureAtlasSprite sprite(final int cell) {
        return this.sprites.get(cell - TcDropOptions.FIRST_CELL, TcDropOptions.FRAMES);
    }

    /** The original's flat {@code getBrightnessForRender = 257} (FXDrop.java:33). */
    @Override
    protected int getLightColor(final float partialTick) {
        return TcDropOptions.LIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Provider wired from {@link TcParticleProviders} for the 3-cell strip. */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcDropOptions> {

        @Override
        public Particle createParticle(final TcDropOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcDropParticle(level, x, y, z, options, this.sprites);
        }
    }
}
