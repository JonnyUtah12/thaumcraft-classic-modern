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
 * The ported arcing wisp particle (TC-M7-02-c-s-f1).
 *
 * <p>One instance is one {@code FXWispArcing}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXWispArcing.java),
 * the mote {@code ClientProxy.sourceStreamFX} (ClientProxy.java:752-757) fires
 * from an essentia/aspects tag toward its source. It is not a physics particle
 * at all: {@code onUpdate} (lines 96-110) re-derives the position from the
 * spawn anchor and the delta to the target every tick —</p>
 *
 * <pre>
 *   var1 = 1 - age / lifetime
 *   var2 = 1 - age / (lifetime / 2);  var2 *= var2
 *   x = ax + dx * var1
 *   y = ay + dy * var1 - var2 + 1.0     // the arc: it bulges up over its life
 *   z = az + dz * var1
 * </pre>
 *
 * <p>so the mote flies from the target back toward the anchor as {@code age}
 * grows and lifts by {@code 1 - var2} blocks in the middle. The port keeps
 * that arithmetic exactly, with the platform's integration neutralised
 * ({@code friction = 1}, {@code gravity = 0}, no physics) because the original
 * writes absolute positions. The render pass (lines 60-82) draws the full sheet
 * at {@code 0.5 * particleScale * agescale} with the same
 * up-then-down age envelope {@code FXWisp} uses.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcWispArcingParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final double anchorX;
    private final double anchorY;
    private final double anchorZ;
    private final float moteScale;
    private final int halfLife;

    TcWispArcingParticle(final ClientLevel level, final double x, final double y, final double z,
            final TcWispArcingOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.anchorX = x;
        this.anchorY = y;
        this.anchorZ = z;
        this.moteScale = options.scale();
        this.halfLife = Math.max(1, options.lifetime() / 2);
        this.rCol = TcFxPlans.red(options.color());
        this.gCol = TcFxPlans.green(options.color());
        this.bCol = TcFxPlans.blue(options.color());
        this.alpha = 0.75F;                     // glColor4f(1,1,1,0.75F), line 65
        this.lifetime = Math.max(1, options.lifetime());
        this.quadSize = 0.5F * this.moteScale;  // FXWispArcing.java:66 half-extent
        this.hasPhysics = false;                // positions are written, not moved
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        // FXWispArcing.onUpdate (FXWispArcing.java:96-110).
        final float progress = 1.0F - (float) this.age / (float) Math.max(1, this.lifetime);
        float arc = 1.0F - (float) this.age / (float) this.halfLife;
        arc *= arc;
        this.setPos(this.anchorX + this.optionsDx() * progress,
                this.anchorY + this.optionsDy() * progress - arc + 1.0,
                this.anchorZ + this.optionsDz() * progress);
        float ageScale = (float) this.age / (float) this.halfLife;
        if (ageScale > 1.0F) {
            ageScale = 2.0F - ageScale;
        }
        this.quadSize = 0.5F * this.moteScale * ageScale;
        this.setSpriteFromAge(this.sprites);
    }

    private double optionsDx() {
        return this.delta().x;
    }

    private double optionsDy() {
        return this.delta().y;
    }

    private double optionsDz() {
        return this.delta().z;
    }

    /** The delta carried in the payload; the original parks it in its motion fields. */
    private net.minecraft.world.phys.Vec3 delta() {
        return new net.minecraft.world.phys.Vec3(this.xd, this.yd, this.zd);
    }

    /** The original draws its FX sheet fullbright (tessellator setBrightness(240)). */
    @Override
    protected int getLightColor(final float partialTick) {
        return 15 << 4 | 15 << 20;
    }

    @Override
    public ParticleRenderType getRenderType() {
        // FXWispArcing keeps blendmode 1 (line 15) → the alpha-blended sheet.
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Provider wired from {@link TcParticleProviders} for the JSON frame list. */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcWispArcingOptions> {

        @Override
        public Particle createParticle(final TcWispArcingOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            // The spawn's motion slots carry the delta to the target (the
            // original assigns `x - d` to field_70159_w etc., line 55-58), and
            // the particle never integrates them.
            return new TcWispArcingParticle(level, x, y, z,
                    new TcWispArcingOptions(options.color(), options.scale(), options.lifetime(),
                            vx, vy, vz),
                    this.sprites);
        }
    }
}
