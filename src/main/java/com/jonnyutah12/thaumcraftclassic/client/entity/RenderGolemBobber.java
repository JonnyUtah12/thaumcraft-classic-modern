package com.jonnyutah12.thaumcraftclassic.client.entity;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBobber;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The golem fishing bobber — Thaumcraft 4.2.3.5
 * {@code thaumcraft.client.renderers.entity.RenderGolemBobber}, transcribed from
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/renderers/entity/RenderGolemBobber.java}
 * (97 lines). Card TC-M6-08-s-c2.
 *
 * <p>The renderer has two parts, exactly as the original: the billboard ball
 * (:19-44) and the line to the thrower (:45-87).</p>
 *
 * <p><b>Ball.</b> The 1.7.10 code translates by the camera-relative position
 * (:20) and scales by 0.5 (:22), counter-rotates by the camera yaw/pitch
 * (:34-35), then emits one unit quad centred on the origin (:38-41) with the UV
 * rectangle of {@code textures/particle/particles.png} tile (b0, b1) = (1, 2),
 * computed as {@code (b * 8 + 0) / 128} / {@code (b * 8 + 8) / 128} (:25-30).
 * 1.21.1 hands the renderer a pose that already sits at the entity, and
 * {@link net.minecraft.client.renderer.entity.EntityRenderDispatcher#cameraOrientation()}
 * is the quaternion twin of the two counter-rotations, so those three GL calls
 * become one {@code mulPose}. The UV arithmetic is ported verbatim into
 * {@link #ballUv(int, int)}, which pins 0.0625 / 0.125 / 0.125 / 0.1875.</p>
 *
 * <p><b>Line.</b> 1.7.10 draws it with {@code glDisable(GL_TEXTURE_2D) +
 * glDisable(GL_LIGHTING)} and a 17-vertex {@code GL_LINE_STRIP} (:73-84), i.e.
 * an untextured, unlit, full-bright white line. The 1.21.1 render type with the
 * same properties is {@link RenderType#lineStrip()}; the 16-segment split and
 * the sag term {@code (t * t + t) * 0.5 + 0.25} of :81 are kept verbatim in
 * {@link #linePoint(double, double, double, float)}. One documented delta: the
 * line has no world-space width in 1.7.10 (GL lines are screen-space), so
 * vanilla 1.21.1's textured 0.03-half-width ribbon is not ported — this stays
 * the 1.7.10 figure, recorded as BEHAVIORAL_DEVIATIONS BD-42.</p>
 *
 * <p>The thrower is the original's public transient
 * {@code EntityGolemBase fisher} ({@code EntityGolemBobber.java:25}), which the
 * port carries as {@link EntityGolemBobber#getFisher()}; the throw site that
 * sets it is the behaviour card t_03c5d088, so with no fisher set the line is
 * simply not drawn, exactly as the {@code fisher != null} guard of :45 does.</p>
 *
 * <p>MCP names: {@code func_76126_a} = {@code MathHelper.sin},
 * {@code func_76134_b} = {@code MathHelper.cos}, {@code func_70047_e} =
 * {@code getEyeHeight()}, {@code field_70127_C}/{@code field_70125_A} = the
 * render-pitch pair ({@code xRotO}/{@code xRot} in 1.21.1),
 * {@code field_70126_B}/{@code field_70177_z} = the render-yaw pair
 * ({@code yRotO}/{@code yRot}), {@code field_70760_ar}/{@code field_70761_aq} =
 * the body-yaw pair ({@code yBodyRotO}/{@code yBodyRot}), and the
 * {@code prevPos}/{@code pos} fields become the lerped {@link Vec3}s of
 * {@link #getOrigin(EntityGolemBobber, float)}.</p>
 */
public class RenderGolemBobber extends EntityRenderer<EntityGolemBobber> {

    /** {@code RenderGolemBobber.java:16} — vanilla's particle sheet, referenced by path only. */
    public static final ResourceLocation PARTICLE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/particle/particles.png");

    /** {@code this.field_76990_c.field_78735_i} / {@code field_78732_j} (:34-35) → one quaternion. */
    public static final int STRIP_SEGMENTS = 16; // :77 byte b2 = 16

    public RenderGolemBobber(final EntityRendererProvider.Context context) {
        super(context);
    }

    /** {@code func_110775_a} (:90-92). */
    @Override
    public ResourceLocation getTextureLocation(final EntityGolemBobber bobber) {
        return PARTICLE_TEXTURE;
    }

    @Override
    public void render(final EntityGolemBobber bobber, final float entityYaw, final float partialTick,
            final PoseStack pose, final MultiBufferSource buffers, final int packedLight) {
        final Vec3 origin = bobber.getPosition(partialTick);

        // ---- the ball, :19-44
        pose.pushPose();
        pose.scale(0.5F, 0.5F, 0.5F); // :22
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation()); // :34-35
        final float[] uv = ballUv(1, 2); // :25-30, b0 = 1, b1 = 2
        quad(pose, buffers.getBuffer(RenderType.entityTranslucent(PARTICLE_TEXTURE)),
                ballCorners(uv), 0.0F, 1.0F, 0.0F, packedLight); // :37 setNormal(0, 1, 0)
        pose.popPose();

        // ---- the line, :45-87
        final EntityGolemBase fisher = bobber.getFisher();
        if (fisher == null) {
            return;
        }
        final Vec3 hand = handPosition(fisher, partialTick);
        // :70-72 the three deltas; the y delta is measured from the ball's top (d8 = y + 0.25, :68)
        final double dx = hand.x - origin.x;
        final double dy = hand.y - (origin.y + 0.25);
        final double dz = hand.z - origin.z;

        pose.pushPose();
        final VertexConsumer line = buffers.getBuffer(RenderType.LINE_STRIP); // :73-74 untextured, unlit
        final Matrix4f matrix = pose.last().pose();
        final Vector3f normal = new Vector3f((float) dx, (float) dy, (float) dz).normalize();
        for (int i = 0; i <= STRIP_SEGMENTS; i++) {
            final float t = (float) i / STRIP_SEGMENTS; // :80
            final float[] point = linePoint(dx, dy, dz, t); // :81
            // POSITION_COLOR_NORMAL: position, colour, normal — the line type has no
            // UV, overlay or light field, which is 1.7.10's full-bright untextured line.
            line.addVertex(matrix, point[0], point[1], point[2])
                    .setColor(255, 255, 255, 255) // :75 glColor white
                    .setNormal(pose.last(), normal.x(), normal.y(), normal.z());
        }
        pose.popPose();
    }

    /**
     * The source's hand offset (:46-66). The first {@code Vec3} block
     * ({@code :48-59}) is written to a temporary the source then overwrites at
     * :64-66, so it is dead code in the original and is not ported; the arm
     * swing of :46-47 still feeds nothing in that path, and the figures that
     * reach the line are the :61-66 body-yaw pair. Kept as comments so the
     * parity is auditable.
     */
    static Vec3 handPosition(final EntityGolemBase fisher, final float partialTick) {
        // :46-47 f9 = fisher.rightArm / 3, f10 = cos(sin(f9) * PI). They feed the
        // (-0.5, 0.03, 0.8) offset block of :48-59, whose result :64-66 overwrites —
        // the block is dead in the shipped code, so it is not ported, and these two
        // figures are recorded here as the source's own.
        // :60 double d6 = fisher.getEyeHeight()
        final double eyeHeight = fisher.getEyeHeight(fisher.getPose());
        // :61 float f11 = lerp(prevRenderYawOffset, renderYawOffset, t) * PI / 180
        final float bodyYaw = (fisher.yBodyRotO + (fisher.yBodyRot - fisher.yBodyRotO) * partialTick)
                * (float) Math.PI / 180.0F;
        final double sinBody = Mth.sin(bodyYaw); // :62 func_76126_a = sin
        final double cosBody = Mth.cos(bodyYaw); // :63 func_76134_b = cos

        final Vec3 lerped = lerp(fisher, partialTick);
        // :64-66 the hand, in world coordinates
        return new Vec3(
                lerped.x - cosBody * 0.25 - sinBody * 0.7,
                lerped.y + eyeHeight - 0.4,
                lerped.z - sinBody * 0.25 + cosBody * 0.7);
    }

    /** The lerped world position of an entity, the {@code prevPos}/{@code pos} pairs of :57-59 and :67-69. */
    private static Vec3 lerp(final EntityGolemBase entity, final float partialTick) {
        return entity.getPosition(partialTick);
    }

    /**
     * The segment position of :81: {@code x = dx * t}, {@code y = dy * (t * t +
     * t) * 0.5 + 0.25}, {@code z = dz * t}.
     */
    public static float[] linePoint(final double dx, final double dy, final double dz, final float t) {
        return new float[] {
                (float) (dx * t),
                (float) (dy * (t * t + t) * 0.5 + 0.25),
                (float) (dz * t),
        };
    }

    /**
     * The UV rectangle of tile (b0, b1) of the 128x128 particle sheet,
     * {@code (b * 8 + 0) / 128} and {@code (b * 8 + 8) / 128} (:27-30). Returned
     * as {@code {f2, f3, f4, f5}} = the source's (u0, u1, vInner, vOuter).
     */
    public static float[] ballUv(final int b0, final int b1) {
        final float f2 = (b0 * 8 + 0) / 128.0F;
        final float f3 = (b0 * 8 + 8) / 128.0F;
        final float f4 = (b1 * 8 + 0) / 128.0F;
        final float f5 = (b1 * 8 + 8) / 128.0F;
        return new float[] {f2, f3, f4, f5};
    }

    /**
     * The four corners of the ball quad (:38-41): a unit square centred on the
     * origin ({@code f7 = f8 = 0.5F}, :33-34) whose UVs are
     * {@code (f2, f5) (f3, f5) (f3, f4) (f2, f4)}.
     */
    public static float[] ballCorners(final float[] uv) {
        final float f7 = 0.5F; // :33
        final float f8 = 0.5F; // :34
        return new float[] {
                0.0F - f7, 0.0F - f8, 0.0F, uv[0], uv[3],
                1.0F - f7, 0.0F - f8, 0.0F, uv[1], uv[3],
                1.0F - f7, 1.0F - f8, 0.0F, uv[1], uv[2],
                0.0F - f7, 1.0F - f8, 0.0F, uv[0], uv[2],
        };
    }

    private static void quad(final PoseStack pose, final VertexConsumer buffer, final float[] corners,
            final float nx, final float ny, final float nz, final int light) {
        final Matrix4f matrix = pose.last().pose();
        for (int i = 0; i < 4; i++) {
            final int base = i * 5;
            buffer.addVertex(matrix, corners[base], corners[base + 1], corners[base + 2])
                    .setUv(corners[base + 3], corners[base + 4])
                    .setNormal(pose.last(), nx, ny, nz)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setColor(255, 255, 255, 255);
        }
    }
}
