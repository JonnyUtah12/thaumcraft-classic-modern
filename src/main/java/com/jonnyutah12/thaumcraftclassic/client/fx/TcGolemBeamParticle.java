package com.jonnyutah12.thaumcraftclassic.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The ported golem-boss beam (TC-M7-02-b-s): one instance is one
 * {@code FXBeamGolemBoss}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/beams/FXBeamGolemBoss.java)
 * drawn as a 1.21.1 particle.
 *
 * <p>The ribbon body is the same {@link TcBeamGeometry} math as
 * {@link TcBeamParticle}. The start anchor is recomputed every tick from the
 * boss's head (yaw/pitch/eye-height basis, FXBeamGolemBoss.java:39-52,
 * 84-93) and the target anchor is the target's previous-tick x/z and
 * bounding-box bottom plus half its width (FXBeamGolemBoss.java:95-98) — the
 * original reads {@code prevPosX}, {@code boundingBox.minY} and {@code width}.
 * The impact sprite the original draws inside its render pass (its
 * {@code renderImpact}, FXBeamGolemBoss.java:232-262) is the companion
 * {@link TcBeamImpactParticle} the spawn plan emits next to this beam; the
 * boss head anchor and the impact's target anchor are the two halves of the
 * original's two anchors.</p>
 */
public final class TcGolemBeamParticle extends TextureSheetParticle {

    /** The original's field default {@code endMod = 1} (FXBeamGolemBoss.java:26). */
    static final float END_MOD = 1.0F;
    /** The original's field default {@code rotationspeed = 5} (FXBeamGolemBoss.java:28). */
    static final int ROTATION_SPEED = 5;

    private final SpriteSet sprites;
    private final TcGolemBeamOptions options;
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

    TcGolemBeamParticle(final ClientLevel level, final double x, final double y, final double z,
            final TcGolemBeamOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.options = options;
        this.sprites = sprites;
        this.red = TcFxPlans.red(options.color());
        this.green = TcFxPlans.green(options.color());
        this.blue = TcFxPlans.blue(options.color());
        this.setLifetime(options.age());
        // FXBeamGolemBoss.java:41-44: a dead boss forces particleMaxAge = 0.
        final Entity boss = level.getEntity(options.bossEntityId());
        if (boss == null) {
            this.setLifetime(0);
        }
        this.setSprite(sprites.get(options.type(), 4));
        this.anchor(boss);
    }

    @Override
    public void tick() {
        super.tick();
        this.prevYaw = this.rotYaw;
        this.prevPitch = this.rotPitch;
        // FXBeamGolemBoss.onUpdate (FXBeamGolemBoss.java:84-103): re-anchor on
        // the boss's head every tick.
        this.anchor(this.level.getEntity(this.options.bossEntityId()));
    }

    private void anchor(final Entity boss) {
        if (boss == null) {
            return;
        }
        final double[] head = TcBeamGeometry.headAnchor(boss.getX(), boss.getY(), boss.getZ(),
                boss.getYRot(), boss.getXRot(), boss.getEyeHeight());
        this.x = head[0];
        this.y = head[1];
        this.z = head[2];
        final Entity target = this.level.getEntity(this.options.targetEntityId());
        if (target == null) {
            return;
        }
        final TcBeamGeometry.Axis axis = TcBeamGeometry.axis(this.x, this.y, this.z,
                target.xo, TcBeamGeometry.targetAnchorY(target.getBoundingBox().minY, target.getBbWidth()),
                target.zo);
        this.length = axis.length();
        this.rotYaw = axis.rotYaw();
        this.rotPitch = axis.rotPitch();
    }

    @Override
    public void render(final VertexConsumer buffer, final Camera camera, final float partialTick) {
        final Vec3 cam = camera.getPosition();
        final Minecraft minecraft = Minecraft.getInstance();

        final int slide = minecraft.player == null ? 0 : minecraft.player.tickCount;
        final float var12 = TcBeamGeometry.scroll(slide + partialTick, false);
        final float spin = TcBeamGeometry.spin(this.level.getGameTime(), ROTATION_SPEED, partialTick);

        final float size = TcBeamGeometry.size(true, this.age, this.options.size(), this.prevSize, partialTick);
        final float op = TcBeamGeometry.opacity(true, this.options.age(), this.age);
        this.prevSize = size;

        final int alpha = (int) (Math.max(0.0F, Math.min(1.0F, op)) * 255.0F);
        final int light = LightTexture.FULL_BRIGHT;

        this.poseStack.setIdentity();
        this.poseStack.translate(this.x - cam.x, this.y - cam.y, this.z - cam.z);
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

    @Override
    protected int getLightColor(final float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    /** Provider wired from {@link TcParticleProviders} (frames = sliced beam0..beam3). */
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcGolemBeamOptions> {

        @Override
        public Particle createParticle(final TcGolemBeamOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcGolemBeamParticle(level, x, y, z, options, this.sprites);
        }
    }
}
