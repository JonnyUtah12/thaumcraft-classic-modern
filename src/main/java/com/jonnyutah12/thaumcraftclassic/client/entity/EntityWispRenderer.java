package com.jonnyutah12.thaumcraftclassic.client.entity;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityWisp;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * The wisp renderer — {@code thaumcraft:wisp} (TC-M7-03-s).
 *
 * <p>Original: {@code thaumcraft.client.renderers.entity.RenderWisp}, the one
 * renderer of the 44 in
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/renderers/entity/}
 * whose entity exists in this tree (see {@code docs/feature-tracking.md},
 * TC-M7-03-s — every other renderer imports an entity the port does not have
 * yet and is filed under its own card).</p>
 *
 * <h2>What the original draws</h2>
 * <p>Not a model: two additively blended, camera-facing quads at
 * {@code (x, y + 0.45, z)}, in this order —</p>
 * <ol>
 *   <li>the <b>body</b>, one frame of the 4x4 animated sheet
 *       {@code textures/misc/wisp.png} (frame {@code tickCount % 16}, scaled by
 *       {@code f10 = 1.0F}) tinted with the wisp's aspect colour, or with the
 *       red hurt flash while {@code hurtTime > 0};</li>
 *   <li>the <b>glow shell</b>, column {@code tickCount % 16} of row 5 of
 *       {@code textures/misc/particles.png} (the original's own
 *       {@code ParticleEngine.particleTexture}), white, scaled by
 *       {@code 0.4F + sin((tickCount + partialTick) / 10F) * 0.1F}.</li>
 * </ol>
 * <p>Both passes are drawn at full brightness ({@code func_78380_c(240)}) and
 * the renderer has no shadow ({@code field_76989_e = 0.0F}). A wisp at zero
 * health draws nothing at all.</p>
 *
 * <p>All the numbers live in {@link WispRenderLogic} so they are pinned by
 * unit tests; this class only turns them into vertices and is loaded on the
 * client only (the registration lives in {@link ModEntityRenderers}, a
 * {@code Dist.CLIENT} mod-bus subscriber, so {@code runServer} and
 * {@code runGameTestServer} never touch it).</p>
 *
 * <h2>Not proved here</h2>
 * <p>Rendered pixels. The quad geometry, the animation frames, the tint and the
 * registration map are asserted; the on-screen result needs a real client and is
 * recorded as {@code not_proven} in {@code docs/FEATURE_MATRIX.md}, exactly as
 * TC-M7-02-s did for its particle sprites.</p>
 */
public class EntityWispRenderer extends EntityRenderer<EntityWisp> {

    /** {@code UtilsFX.bindTexture("textures/misc/wisp.png")} — the 4x4 body sheet. */
    public static final ResourceLocation WISP_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "textures/misc/wisp.png");

    /** {@code ParticleEngine.particleTexture} — the glow-shell atlas. */
    public static final ResourceLocation SHELL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "textures/misc/particles.png");

    public EntityWispRenderer(final EntityRendererProvider.Context context) {
        super(context);
        // The original's RenderWisp constructor sets field_76989_e = 0.0F:
        // a wisp casts no shadow.
        this.shadowRadius = 0.0F;
        this.shadowStrength = 0.0F;
    }

    /** The pass-1 sheet; also what the render dispatcher consults for the
     * entity's "main" texture. */
    @Override
    public ResourceLocation getTextureLocation(final EntityWisp entity) {
        return WISP_TEXTURE;
    }

    @Override
    public void render(final EntityWisp entity, final float entityYaw, final float partialTick,
            final PoseStack pose, final MultiBufferSource buffers, final int packedLight) {
        // renderEntityAt's first line: a dead wisp draws nothing.
        if (entity.getHealth() <= 0.0F) {
            return;
        }

        final Aspect aspect = Aspect.getAspect(entity.getWispType());
        // Aspect.getAspect(...) == null -> the original keeps `new Color(0)`.
        final float[] body = WispRenderLogic.bodyColour(aspect == null ? 0 : aspect.getColor(), entity.hurtTime);
        final int light = WispRenderTypes.LIGHT;
        final int frame = WispRenderLogic.frameIndex(entity.tickCount);

        pose.pushPose();
        // f12 = (float)y + 0.45F, and the quad is built from the camera's own
        // basis vectors (ActiveRenderInfo.f1..f5), which is exactly what the
        // render-dispatcher orientation does in 1.21.1.
        pose.translate(0.0D, WispRenderLogic.VERTICAL_OFFSET, 0.0D);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        final PoseStack.Pose last = pose.last();

        emitQuad(buffers.getBuffer(WispRenderTypes.Body.TYPE), last,
                WispRenderLogic.frameUv(frame), body, WispRenderLogic.BODY_SCALE, light);

        emitQuad(buffers.getBuffer(WispRenderTypes.Shell.TYPE), last,
                WispRenderLogic.shellUv(frame), WispRenderLogic.shellColour(),
                WispRenderLogic.shellScale(entity.tickCount, partialTick), light);

        pose.popPose();
    }

    /**
     * Emits the original's four {@code func_78374_a} vertices: corners and UV
     * assignment come from {@link WispRenderLogic#quad}, scaled about the quad
     * centre (the original multiplies both offset vectors by {@code f10}).
     */
    private static void emitQuad(final VertexConsumer buffer, final PoseStack.Pose pose,
            final WispRenderLogic.Uv uv, final float[] colour, final float scale, final int light) {
        final int red = channel(colour[0]);
        final int green = channel(colour[1]);
        final int blue = channel(colour[2]);
        final int alpha = channel(colour[3]);
        for (final WispRenderLogic.Corner corner : WispRenderLogic.quad(uv)) {
            buffer.addVertex(pose, corner.x() * scale, corner.y() * scale, 0.0F)
                    .setColor(red, green, blue, alpha)
                    .setUv(corner.u(), corner.v())
                    .setLight(light);
        }
    }

    private static int channel(final float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0F)));
    }
}
