package com.jonnyutah12.thaumcraftclassic.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The rune decal — the ported {@code thaumcraft:rune} particle, from 1.7.10
 * {@code thaumcraft.client.fx.particles.FXBlockRunes}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXBlockRunes.java).
 *
 * <p><b>Shape (ctor, FXBlockRunes.java:16-37).</b> Lifetime {@code 3 * dur}
 * (line 26), zero motion (line 25), {@code particleScale = 1.0 +
 * nextGaussian()*0.1} (line 36), a {@code rotation} of {@code nextInt(4) * 90}
 * (line 20), the in-plane jitter {@code ofx = rand*0.2} / {@code ofy = -0.3 +
 * rand*0.6} (lines 34-35), the {@code runeIndex} sheet column (line 33) and
 * {@code particleAlpha = 0} (line 37) so the fade envelope starts dark. Gravity
 * starts at 0 (line 24) and is set by the add call's {@code setGravity}
 * (lines 90-92).</p>
 *
 * <p><b>Physics.</b> {@code onUpdate} (lines 69-88) OVERRIDES the base tick and
 * does not call it, so a rune gets no drag, no friction and no collision:
 * {@code yd -= 0.04 * gravity} (line 84) then a plain {@code pos += motion}
 * (lines 85-87). This family is therefore exact where BD-15 had to adapt:
 * 1.21.1's {@code Particle.tick()} runs the same {@code yd -= 0.04 * gravity}
 * and, with {@code friction = 1.0} and {@code hasPhysics = false},
 * {@code move()} is a plain AABB translation with no rounding
 * ({@code Particle.java:178-215}), which is the same arithmetic. Nothing in the
 * BD-15 seam applies here, because the original's base tick is never reached —
 * the reason BD-15 exists is that {@code FXGeneric} DOES call {@code super}.</p>
 *
 * <p><b>Render (lines 40-67).</b> One flat quad, not a billboard:
 * {@code glRotatef(rotation,0,1,0)}, {@code glRotatef(90,0,0,1)},
 * {@code glTranslated(ofx, ofy, -0.51)} (lines 48-50) and four vertices at
 * ±0.5 of the rotated square with edge {@code var12 = 0.3 * particleScale}
 * (lines 55, 60-63). {@link TcPlanarQuad} computes that chain in world axes, so
 * the decal sits on one of four axis-aligned walls; the chain is
 * camera-independent and BD-16 does not apply to it. The render interpolates
 * {@code prevPos -> pos} itself (lines 44-46), which the port mirrors with
 * {@code xo/yo/zo}. The alpha is halved (lines 43, 59,
 * {@code particleAlpha / 2.0F}) and the brightness is baked at
 * {@code setBrightness(240)} (line 58): block light 15, sky light 0, which is
 * the integer 240 in 1.21.1's {@code block << 4 | sky << 20} packing too.</p>
 *
 * <p><b>Texture.</b> The original's uv (lines 51-54) addresses cell
 * {@code runeIndex % 16} of row 6 in the 16x16 grid of
 * {@code textures/misc/particles.png}, with a 0.0624375 (= 0.999/16) inset on
 * each axis. The port ships the 16 cells as 16 registered sprites sliced at
 * build time from those same cells (D-004), so the address becomes
 * {@code sprites.get(frame, 16)} plus the per-sprite atlas rect, with the
 * 0.999 inset preserved. The additive blend of the FX-sheet pass
 * (client/fx/ParticleEngine.java:60-66, {@code glBlendFunc(770, 1)}) is carried
 * by {@link ParticleRenderType#PARTICLE_SHEET_LIT}, the same translation BD-15
 * records for the sparkle layer.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcRuneParticle extends TextureSheetParticle {

    /**
     * The original's baked brightness: {@code tessellator.setBrightness(240)}
     * (FXBlockRunes.java:58) is block light 15 / sky light 0, and 1.21.1 packs
     * the same pair as {@code 15 << 4 | 0 << 20} = 240.
     */
    private static final int BAKED_BRIGHTNESS = 240;

    private final TcRuneOptions options;
    private final TcPlanarQuad quad;

    TcRuneParticle(final ClientLevel level, final double x, final double y, final double z,
            final TcRuneOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.options = options;
        this.quad = TcPlanarQuad.runeQuad(options.yaw(), options.offsetU(), options.offsetV(),
                options.scale());
        this.lifetime = options.lifetime();
        this.gravity = options.gravity();
        this.friction = 1.0F;
        // onUpdate (FXBlockRunes.java:69-88) never touches the world, and the
        // ctor only sizes the box (line 28); FX never collide.
        this.hasPhysics = false;
        // The ctor's particleAlpha = 0 (line 37).
        this.alpha = 0.0F;
        this.setSprite(sprites.get(options.frame(), TcFxPlans.RUNE_FRAMES));
    }

    @Override
    public void tick() {
        // FXBlockRunes.java:73-78 computes the envelope BEFORE the age++ on
        // line 80, so it runs here on the pre-increment age: in over
        // lifetime/5 ticks, then out over the whole lifetime.
        final float fadeIn = this.lifetime / (float) TcFxPlans.RUNE_FADE_IN_DIVISOR;
        this.alpha = this.age <= fadeIn
                ? (fadeIn <= 0.0F ? 1.0F : this.age / fadeIn)
                : (this.lifetime - this.age) / (float) this.lifetime;
        super.tick();
    }

    @Override
    public void render(final VertexConsumer buffer, final Camera camera, final float partialTick) {
        // The render interpolates prevPos -> pos (FXBlockRunes.java:44-46) and
        // translates by the camera, which 1.21.1 does through the vertex
        // offsets (the beam family's translation, TcArcParticle.render).
        final Vec3 cam = camera.getPosition();
        final float ox = (float) (this.xo + (this.x - this.xo) * partialTick - cam.x);
        final float oy = (float) (this.yo + (this.y - this.yo) * partialTick - cam.y);
        final float oz = (float) (this.zo + (this.z - this.zo) * partialTick - cam.z);
        // particleAlpha / 2.0F (FXBlockRunes.java:43, 59).
        final int alpha = (int) (this.alpha / TcFxPlans.RUNE_ALPHA_DIVISOR * 255.0F);
        final int red = (int) (TcFxPlans.red(this.options.color()) * 255.0F);
        final int green = (int) (TcFxPlans.green(this.options.color()) * 255.0F);
        final int blue = (int) (TcFxPlans.blue(this.options.color()) * 255.0F);
        // The original's four vertices in its order (lines 60-63), then the same
        // four reversed: 1.7.10's FX-sheet pass runs with culling disabled
        // (FXArc.java:91 is the same case) and 1.21.1's particle pipelines cull,
        // so a fixed-orientation quad needs both windings.
        emit(buffer, ox, oy, oz, red, green, blue, alpha, -0.5, 0.5);
        emit(buffer, ox, oy, oz, red, green, blue, alpha, 0.5, 0.5);
        emit(buffer, ox, oy, oz, red, green, blue, alpha, 0.5, -0.5);
        emit(buffer, ox, oy, oz, red, green, blue, alpha, -0.5, -0.5);
        emit(buffer, ox, oy, oz, red, green, blue, alpha, -0.5, -0.5);
        emit(buffer, ox, oy, oz, red, green, blue, alpha, 0.5, -0.5);
        emit(buffer, ox, oy, oz, red, green, blue, alpha, 0.5, 0.5);
        emit(buffer, ox, oy, oz, red, green, blue, alpha, -0.5, 0.5);
    }

    /**
     * One vertex of the rune's quad: the corner {@code (s, t)} of the rotated
     * square (in units of the full edge, as the original's
     * {@code ±0.5 * var12} arguments) with the texture coordinate its raw uv
     * picks for it (FXBlockRunes.java:51-54, 60-63).
     */
    private void emit(final VertexConsumer buffer, final float ox, final float oy, final float oz,
            final int red, final int green, final int blue, final int alpha, final double s,
            final double t) {
        final double[] world = this.quad.vertex(ox, oy, oz, s, t);
        // The original's u runs along the quad's v axis and its v along the -u
        // axis: (s=-0.5,t=+0.5) -> (var9,var11), (s=+0.5,t=+0.5) -> (var9,var10),
        // (s=+0.5,t=-0.5) -> (var8,var10), (s=-0.5,t=-0.5) -> (var8,var11),
        // where var8/var9 are the cell's u edges and var10/var11 its v edges
        // (lines 51-54) — i.e. the sprite sampled to 0.999 of its extent.
        final double u = t > 0.0 ? TcFxPlans.RUNE_UV_MAX : 0.0;
        final double v = s < 0.0 ? TcFxPlans.RUNE_UV_MAX : 0.0;
        buffer.addVertex((float) world[0], (float) world[1], (float) world[2])
                .setColor(red, green, blue, alpha)
                .setUv((float) (this.getU0() + (this.getU1() - this.getU0()) * u),
                        (float) (this.getV0() + (this.getV1() - this.getV0()) * v))
                .setLight(BAKED_BRIGHTNESS);
    }

    /**
     * The rune samples no world light: the original bakes brightness 240 into
     * its vertices (FXBlockRunes.java:58), so a rune in a lightless room is as
     * bright as one in daylight.
     */
    @Override
    public int getLightColor(final float partialTick) {
        return BAKED_BRIGHTNESS;
    }

    @Override
    public ParticleRenderType getRenderType() {
        // The FX-sheet pass blends SRC_ALPHA + ONE
        // (client/fx/ParticleEngine.java:60-66, glBlendFunc(770, 1)) — additive,
        // the same layer translation BD-15 records for the sparkle.
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    /** Provider wired from {@link TcParticleProviders} for the JSON frame list. */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcRuneOptions> {

        @Override
        public Particle createParticle(final TcRuneOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            // The plan's motion is zero (FXBlockRunes.java:25), so the platform's
            // velocity arguments are ignored, as the type's declaration says.
            return new TcRuneParticle(level, x, y, z, options, this.sprites);
        }
    }
}
