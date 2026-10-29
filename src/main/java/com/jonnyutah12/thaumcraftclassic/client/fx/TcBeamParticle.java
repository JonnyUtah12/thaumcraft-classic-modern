package com.jonnyutah12.thaumcraftclassic.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The ported entity-targeted beam (TC-M7-02-b-s): one instance is one
 * reference-tree {@code FXBeam}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/beams/FXBeam.java)
 * drawn as a 1.21.1 particle.
 *
 * <p>The geometry, scroll, spin, pulse ramp and fade are the exact
 * {@link TcBeamGeometry} port of the original's math. The original drew
 * immediate-mode GL quads under a transform chain and a {@code GL_REPEAT}
 * beam texture with culling disabled; the port reproduces the transform chain
 * on a pose stack, cuts the ribbon into per-texture-tile quads because the
 * particle atlas is clamped, and emits each quad twice (both windings) to
 * replace the original's disabled face culling. The beam texture is a
 * full-texture atlas copy sliced at build time from the original
 * {@code textures/misc/beam*.png} (D-004). All of this is the seam recorded
 * as BD-16 in docs/BEHAVIORAL_DEVIATIONS.md.</p>
 *
 * <p>Per-tick behaviour mirrors {@code FXBeam.onUpdate} (FXBeam.java:113-130):
 * the beam follows the live target (eye-height anchor
 * {@code t.posY + getEyeHeight - width/2}) and recomputes its axis; when the
 * target is gone the original forces {@code particleMaxAge = 0}, which the
 * port reaches with {@code setLifetime(0)} at construction.</p>
 */
public final class TcBeamParticle extends TextureSheetParticle {

    /** The original's field default {@code endMod = 1} (FXBeam.java:26) — no sender changes it. */
    static final float END_MOD = 1.0F;
    /** The original's field default {@code rotationspeed = 5} (FXBeam.java:28). */
    static final int ROTATION_SPEED = 5;

    private final SpriteSet sprites;
    private final TcBeamOptions options;
    private final float red;
    private final float green;
    private final float blue;

    private float length;
    private float rotYaw;
    private float rotPitch;
    private float prevYaw;
    private float prevPitch;
    private float prevSize;
    private final PoseStack poseStack = new PoseStack();

    TcBeamParticle(final ClientLevel level, final double x, final double y, final double z,
            final TcBeamOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.options = options;
        this.sprites = sprites;
        this.red = TcFxPlans.red(options.color());
        this.green = TcFxPlans.green(options.color());
        this.blue = TcFxPlans.blue(options.color());
        this.setLifetime(options.age());
        // FXBeam.java:55-64: a dead target forces particleMaxAge = 0.
        final Entity target = level.getEntity(options.targetEntityId());
        if (target == null) {
            this.setLifetime(0);
        }
        this.setSprite(sprites.get(options.type(), 4));
        this.recomputeAxis(target);
        this.prevSize = 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        this.prevYaw = this.rotYaw;
        this.prevPitch = this.rotPitch;
        // FXBeam.onUpdate (FXBeam.java:118-124): follow the live target.
        final Entity target = this.level.getEntity(this.options.targetEntityId());
        if (target != null) {
            this.recomputeAxis(target);
        }
    }

    /** The axis math of {@code FXBeam}'s constructor / onUpdate (FXBeam.java:44-53, 118-124). */
    private void recomputeAxis(final Entity target) {
        if (target == null) {
            return;
        }
        final double tx = target.getX();
        final double ty = target.getY() + target.getEyeHeight() - target.getBbWidth() / 2.0;
        final double tz = target.getZ();
        final TcBeamGeometry.Axis axis = TcBeamGeometry.axis(this.x, this.y, this.z, tx, ty, tz);
        this.length = axis.length();
        this.rotYaw = axis.rotYaw();
        this.rotPitch = axis.rotPitch();
    }

    @Override
    public void render(final VertexConsumer buffer, final Camera camera, final float partialTick) {
        final Vec3 cam = camera.getPosition();
        final Minecraft minecraft = Minecraft.getInstance();

        // FXBeam.java:153-155: the scroll clock is the client player's tick counter.
        final int slide = minecraft.player == null ? 0 : minecraft.player.tickCount;
        final float var12 = TcBeamGeometry.scroll(slide + partialTick, this.options.reverse());
        final float spin = TcBeamGeometry.spin(this.level.getGameTime(), ROTATION_SPEED, partialTick);

        // FXBeam.java:155-165: the pulse size ramp (interpolated from the
        // previous render's size, which the original stores at the end of
        // render) and the end-fade opacity.
        final float size = TcBeamGeometry.size(true, this.age, this.options.size(), this.prevSize, partialTick);
        final float op = TcBeamGeometry.opacity(true, this.options.age(), this.age);
        this.prevSize = size;

        final int alpha = (int) (Math.max(0.0F, Math.min(1.0F, op)) * 255.0F);
        final int light = LightTexture.FULL_BRIGHT;   // tessellator setBrightness(200)

        this.poseStack.setIdentity();
        this.poseStack.translate(this.x - cam.x, this.y - cam.y, this.z - cam.z);
        // The original transform chain (FXBeam.java:213-220).
        this.poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        this.poseStack.mulPose(Axis.ZN.rotationDegrees(
                180.0F + this.prevYaw + (this.rotYaw - this.prevYaw) * partialTick));
        this.poseStack.mulPose(Axis.XP.rotationDegrees(
                this.prevPitch + (this.rotPitch - this.prevPitch) * partialTick));
        this.poseStack.mulPose(Axis.YP.rotationDegrees(spin));

        final float u0 = this.getU0();
        final float u1 = this.getU1();
        final float v0 = this.getV0();
        final float v1 = this.getV1();

        // Three ribbon passes, 60° apart (FXBeam.java:225-243).
        for (int t = 0; t < 3; t++) {
            this.poseStack.mulPose(Axis.YP.rotationDegrees(60.0F));
            final float vStart = -1.0F + var12 + t / 3.0F;
            for (final TcBeamGeometry.RibbonTile tile :
                    TcBeamGeometry.ribbon(this.length, size, this.options.size(), END_MOD, vStart)) {
                final PoseStack.Pose pose = this.poseStack.last();
                TcBeamRibbon.emit(buffer, pose, tile, this.red, this.green, this.blue,
                        u0, u1, v0, v1, alpha, light);
            }
        }
    }

    /** Fancy-graphics read of the original's {@code field_74347_j} gate (FXBeam.java:66-73). */
    public static boolean fancyGraphics() {
        return Minecraft.getInstance().options.graphicsMode().get() != GraphicsStatus.FAST;
    }

    /** The original draws every beam fullbright (tessellator setBrightness(200)). */
    @Override
    protected int getLightColor(final float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        // The packet sends blendmode 1 (SRC_ALPHA, ONE) — the additive sheet.
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    /** Provider wired from {@link TcParticleProviders} (frames = sliced beam0..beam3). */
    public record Provider(SpriteSet sprites)
            implements net.minecraft.client.particle.ParticleProvider<TcBeamOptions> {

        @Override
        public net.minecraft.client.particle.Particle createParticle(final TcBeamOptions options,
                final ClientLevel level, final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcBeamParticle(level, x, y, z, options, this.sprites);
        }
    }
}
