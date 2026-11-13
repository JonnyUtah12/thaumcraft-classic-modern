package com.jonnyutah12.thaumcraftclassic.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import org.joml.Quaternionf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The ward hexagram (TC-M7-02-c-s-f4) — the port of
 * {@code thaumcraft.client.fx.other.FXBlockWard} (113 lines: constructor
 * 19-47, draw 49-87, tick 89-108).
 *
 * <p>The original is the ward flash: {@code BlockWarded.addHitEffects}
 * (BlockWarded.java:64-70) and {@code BlockCosmeticOpaque.addHitEffects} for
 * metadata 2, warded glass (BlockCosmeticOpaque.java:68-82) call
 * {@code ClientProxy.blockWard(world, x, y, z, side, hitVec - pos)}
 * (ClientProxy.java:980-984), which spawns one of these on the block centre
 * and hands it straight to the particle engine.</p>
 *
 * <h2>What is a billboard and what was not</h2>
 * The original renders the loaded {@code hemis.obj} OBJ hemisphere with the
 * 128x128 {@code hemis<frame>.png} bound (lines 54, 77-80) in a matrix built
 * from {@code glRotatef(90, side.offsetY, -side.offsetX, side.offsetZ)},
 * {@code glRotatef(rotation, 0, 0, 1)}, a {@code ±0.505} push along the face
 * normal and, for a {@code side.offsetZ > 0} hit, an extra
 * {@code glRotatef(180, 0, -1, 0)} (lines 63-70). A 1.21.1 particle is one
 * textured quad, so the port draws that frame art as one quad with the same
 * rotation chain composed on top of the camera basis
 * ({@link FxWardRules#quadRotation}); the OBJ surface itself has no
 * equivalent and is BD-46 in docs/BEHAVIORAL_DEVIATIONS.md, together with the
 * 240 block light's mapping to 1.21.1's fullbright world light. The camera
 * basis is composed, never chased (the BD-16 rule).
 *
 * <p>The {@code ±0.505} push is baked into the spawn position by the plan: the
 * mote has no gravity and no motion (lines 22-23, and the tick's integration
 * of a zero velocity, lines 104-107), so the translate and the position are
 * the same number, and pinning it in the plan is what makes it assertable on a
 * dedicated server.</p>
 *
 * <p>Layer: the original's {@code glBlendFunc(770, 1)} (SRC_ALPHA, ONE) with
 * {@code glDepthMask(false)} (lines 55-57) is 1.21.1's additive
 * {@link ParticleRenderType#PARTICLE_SHEET_LIT}; the draw's quad is white at
 * {@code alpha / 2} (lines 58, 76).</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcWardParticle extends TextureSheetParticle {

    private final TcWardOptions options;
    private final SpriteSet sprites;

    TcWardParticle(final ClientLevel level, final double x, final double y, final double z,
            final TcWardOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.options = options;
        this.sprites = sprites;
        this.lifetime = options.lifetime();
        // The quad corners are +-0.5 * particleScale (FXBlockWard.java:72,
        // 77-80) and 1.21.1's quadSize IS the half-extent
        // (SingleQuadParticle.renderVertex), so the half-extent is scale / 2.
        this.quadSize = (float)(options.scale() / FxWardRules.QUAD_HALF_EXTENT_DIVISOR);
        this.gravity = FxWardRules.GRAVITY;
        this.hasPhysics = false;
        // glColor4f(1, 1, 1, particleAlpha / 2.0F) (line 58): white, and the
        // alpha is EntityFX's default 1.0F, which the constructor never
        // touches (lines 19-47), so the first render draws at half alpha.
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.alpha = FxWardRules.INITIAL_ALPHA / FxWardRules.DRAW_ALPHA_DIVISOR;
    }

    @Override
    public void tick() {
        // The tick computes the alpha on the PRE-increment age (lines 93-98),
        // then runs the shared `age++ >= lifetime` gate (line 100), which
        // Particle.tick() carries, along with the `prev = pos` snapshot
        // (lines 90-92) and the zero-motion integration (lines 104-107).
        this.alpha = FxWardRules.drawAlpha(this.lifetime, this.age);
        super.tick();
    }

    @Override
    public void render(final VertexConsumer buffer, final Camera camera, final float partialTick) {
        // The frame is chosen in the draw, off the fade that carries the
        // partial tick: fade = (age + partialTick) / lifetime, frame =
        // min(15, (int)(15F * fade)), hemis<frame>.png (lines 52-54).
        this.setSprite(this.sprites.get(
                FxWardRules.frame(FxWardRules.fade(this.age, partialTick, this.lifetime)),
                FxWardRules.FRAME_COUNT));
        final Quaternionf rotation = new Quaternionf();
        // Camera basis first (BD-16), then the original's face rotation chain,
        // post-multiplied exactly as the GL matrix stack descends it.
        getFacingCameraMode().setRotation(rotation, camera, partialTick);
        rotation.mul(FxWardRules.quadRotation(this.options.direction(), this.options.rotation()));
        renderRotatedQuad(buffer, camera, rotation, partialTick);
    }

    @Override
    protected int getLightColor(final float partialTick) {
        // tessellator.setBrightness(240) (FXBlockWard.java:75): 240 % 65536 =
        // 240 -> block light at the maximum, 240 / 65536 = 0 -> no sky light.
        return 15 << 4;
    }

    @Override
    public ParticleRenderType getRenderType() {
        // glBlendFunc(GL_SRC_ALPHA, GL_ONE) (FXBlockWard.java:57).
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    /** Provider wired from {@link TcParticleProviders} for the ward.json strip. */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcWardOptions> {

        @Override
        public Particle createParticle(final TcWardOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcWardParticle(level, x, y, z, options, this.sprites);
        }
    }
}
