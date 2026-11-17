package com.jonnyutah12.thaumcraftclassic.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The ported scorch mote (TC-M7-02-c-s-f5): one {@code FXScorch}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXScorch.java),
 * the fireblast trail {@code UtilsFX.shootFire} throws from the caster's hand
 * (client/lib/UtilsFX.java:365-386).
 *
 * <p>{@code tick()} transcribes {@code onUpdate} (lines 55-98): the velocity is
 * <em>recomputed</em> from the target point every tick —
 * {@code (target - pos) / (distance * 1.25)}, scaled by the remaining-life
 * fraction {@code (lifetime - age) / lifetime}, plus a
 * {@code rand*0.07 - 0.035} kick on each axis. After age 1 the mote stops dead
 * in a solid block and jumps its age by 10 (the "it burned out on impact"
 * branch, lines 84-89). The brightness index climbs 151 → 159 (lines 100-104),
 * which is the 9-frame strip the definition names.</p>
 *
 * <p>Render (lines 106-115): the scale is
 * {@code transferParticleScale * (age/lifetime + 0.25) * 2} and the colour
 * fades black→white in the blue-less channels
 * ({@code r = g = min(1, age*9/lifetime)}, {@code b = 1}) with alpha 0.66; the
 * original's {@code getBrightnessForRender} is a flat 210 (line 49), i.e.
 * fullbright. The 0.1-unit {@code setParticleBounds} is the billboard size,
 * applied as vanilla's {@code 0.1F * particleScale}.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcScorchParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final double targetX;
    private final double targetY;
    private final double targetZ;
    private final float transferScale;

    TcScorchParticle(final ClientLevel level, final double x, final double y, final double z,
            final TcScorchOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.targetX = options.targetX();
        this.targetY = options.targetY();
        this.targetZ = options.targetZ();
        this.transferScale = options.scale();
        this.lifetime = TcScorchOptions.LIFETIME;
        this.gravity = 0.0F;                      // onUpdate integrates itself
        this.friction = 1.0F;
        this.hasPhysics = false;                  // onUpdate moves by hand (no moveEntity call)
        this.alpha = TcScorchOptions.ALPHA * TcScorchOptions.ALPHA;
        // FXScorch.java:108 sets glColor4f(1,1,1,0.66) and line 113 multiplies a
        // second 0.66 into the vertex colour — the mote's alpha is the product.
        this.quadSize = 0.1F * this.transferScale * 0.5F;
        this.setSprite(this.sprites.get(0, TcScorchOptions.FRAMES));
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        // FXScorch.java:55-70: velocity re-derived from the target every tick.
        final double dx = this.targetX - this.x;
        final double dy = this.targetY - this.y;
        final double dz = this.targetZ - this.z;
        final double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        final float life = (float) (this.lifetime - this.age) / this.lifetime;
        this.xd = dx / (distance * 1.25) * life + (this.random.nextFloat() * 0.07F - 0.035F);
        this.yd = dy / (distance * 1.25) * life + (this.random.nextFloat() * 0.07F - 0.035F);
        this.zd = dz / (distance * 1.25) * life + (this.random.nextFloat() * 0.07F - 0.035F);
        // FXScorch.java:82-89: after age 1, a solid block kills the motion and
        // jumps the age by 10 — the mote scorches into the wall.
        if (this.age > 1) {
            final BlockPos block = BlockPos.containing(this.x, this.y + TcScorchOptions.BOUND / 2.0, this.z);
            if (this.level.getBlockState(block).isSolid()) {
                this.xd = 0.0;
                this.yd = 0.0;
                this.zd = 0.0;
                this.age += 10;
            }
        }
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }
        // The brightness ramp (lines 100-104) IS the strip frame: the original
        // sets the texture index, which indexes cells 151..159 of the sheet, so
        // index - FIRST_CELL is the frame of the sliced strip.
        final float fs = (float) this.age / (this.lifetime - 9);
        final int index = fs <= 1.0F
                ? (int) (151.0F + fs * 6.0F) - TcScorchOptions.FIRST_CELL
                : 159 - (this.lifetime - this.age) / 3 - TcScorchOptions.FIRST_CELL;
        this.setSprite(this.sprites.get(Mth.clamp(index, 0, TcScorchOptions.FRAMES - 1),
                TcScorchOptions.FRAMES));
        // Render side-effects the original applies in render (lines 106-114).
        final float ramp = (float) this.age / this.lifetime;
        this.quadSize = 0.1F * this.transferScale * (ramp + 0.25F) * 2.0F;
        final float fade = Math.min(1.0F, this.age * 9.0F / this.lifetime);
        this.rCol = fade;
        this.gCol = fade;
        this.bCol = 1.0F;
    }

    /** The original's {@code getBrightnessForRender} is a flat 210 (line 49). */
    @Override
    protected int getLightColor(final float partialTick) {
        return 15 << 4 | 15 << 20;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Provider wired from {@link TcParticleProviders} for the 9-frame strip. */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcScorchOptions> {

        @Override
        public Particle createParticle(final TcScorchOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcScorchParticle(level, x, y, z, options, this.sprites);
        }
    }
}
