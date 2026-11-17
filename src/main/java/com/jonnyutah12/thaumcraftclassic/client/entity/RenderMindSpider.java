package com.jonnyutah12.thaumcraftclassic.client.entity;

import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityMindSpider;
import com.jonnyutah12.thaumcraftclassic.entities.monster.MindSpiderAiLogic;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The Mind Spider renderer — Thaumcraft 4.2.3.5
 * {@code thaumcraft.client.renderers.entity.RenderMindSpider}, transcribed from
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/renderers/entity/RenderMindSpider.java}
 * (117 lines, read whole). Card TC-M7-03-r (kanban {@code t_32366a27}): the mob
 * landed with TC-M6-06-s-c3-b, the renderer lane TC-M7-03-s ({@code
 * t_215d6fd4}) closed <b>without</b> this file, and the registration site the
 * reference uses is {@code ClientProxy.java:450}
 * ({@code registerEntityRenderingHandler(EntityMindSpider.class, new
 * RenderMindSpider())}), which 1.21.1's twin is in {@link ModEntityRenderers}.
 * This is therefore {@code docs/BEHAVIORAL_DEVIATIONS.md} BD-78's real closer.
 *
 * <p><b>The reference's own shape.</b> {@code :17} {@code @SideOnly(CLIENT)},
 * {@code :18} {@code extends RenderLiving}, {@code :19} the texture,
 * {@code :21-24} {@code super(new ModelSpider(), 0.0F)}, {@code :26-28}
 * {@code setSpiderDeathMaxRotation(180.0F)} reached through {@code :110-112},
 * {@code :30-32} the texture getter, {@code :34-53} the three {@code doRender}
 * overloads gated on the viewer, {@code :55-70} the {@code modelRender} override
 * that paints the fade, {@code :72-74} a {@code renderLayers} override that
 * calls {@code super} and nothing, {@code :76-99} {@code setSpiderEyeBrightness}
 * reached through {@code :114-116}, and {@code :101-108} {@code scaleSpider} →
 * {@code glScalef(spiderScaleAmount(), …)}. 1.21.1's twin of {@code
 * RenderLiving} is {@link MobRenderer} — NOT the platform's {@code
 * SpiderRenderer}, which is the twin of 1.7.10 {@code RenderSpider}
 * ({@code RenderTaintSpider.java:20,26,31} carry the {@code RenderSpider}
 * literals: the original copied the spider renderer per mob instead of
 * subclassing it, and this file is one of those copies).</p>
 *
 * <p><b>The 0.3 scale is the renderer's, and only the renderer's
 * ({@code :101-108}).</b> {@code spiderScaleAmount()} is the literal 0.3 on the
 * entity (reference {@code EntityMindSpider.java:67-70}) and the renderer hands
 * it to the matrix. {@link #scale} is 1.21.1's hook, called on the pose the model
 * is drawn with ({@code LivingEntityRenderer.java:102}). The entity's <b>box</b>
 * is already 0.3 x 0.3 ({@code MindSpiderAiLogic.WIDTH/HEIGHT}, from the
 * registered {@code EntityType.Builder.sized(0.3, 0.3)}), and 1.21.1 scales the
 * pose by the {@code minecraft:generic.scale} attribute one line earlier (the
 * entity's {@code getScale()} at {@code LivingEntityRenderer.java:97-98}) and
 * multiplies the shadow radius by it ({@code :282-283}) — a 0.3 attribute would
 * shrink the model twice, which is the double-shrink BD-78 measured and forbids.
 * So the attribute stays at its 1.0 default (the entity registers no
 * {@code generic.scale} row, pinned by a test) and {@link #scale} is the only
 * place the 0.3 is applied.</p>
 *
 * <p><b>The fade of {@code :55-70}.</b> The reference draws the model with the
 * constant vertex colour {@code glColor4f(1,1,1, min(0.1F, age/100.0F))}
 * ({@code :60}) under alpha blending ({@code :62-63}, {@code GL_BLEND} with
 * {@code SRC_ALPHA, ONE}), depth mask off ({@code :61}) and the alpha floor at
 * 1/255 ({@code :64}, restored to 0.1 at {@code :67}). Read as a fade it caps at
 * 10 % and never reaches opaque: an adult is a 10 %-alpha ghost from its tenth
 * tick. 1.21.1 has no renderer colour hook — it draws the model inside {@code
 * LivingEntityRenderer.render} ({@code :127}) with the colour it computes there
 * ({@code -1}, or the 15 % invisible twin {@code 654311423}), and a layer asks
 * for its own colour ({@code EyesLayer.java:57}). The two hooks the platform
 * does give are used: {@code EntityModel#prepareMobModel}, called once per frame
 * immediately before the body draw ({@code LivingEntityRenderer.java:118} →
 * {@code :127}), arms the colour for that draw — {@link FadeModel} — and the
 * render type is asked of the renderer ({@code :124}, {@code :143-150}), so
 * {@link #getRenderType} returns {@link RenderType#entityTranslucent} for the
 * body. That type is the twin of the GL block: translucency, no culling, lightmap
 * and overlay ({@code RenderType.java:153-165}, {@code :850-852}), and its
 * fragment shader tests {@code color.a < 0.1} against the <b>texture</b> alpha
 * <i>before</i> multiplying in the vertex colour ({@code
 * shaders/core/rendertype_entity_translucent.fsh}, read out of the pinned 1.21.1
 * client jar), so a 0.1 body alpha blends rather than being discarded. BD-87
 * carries the deltas: the depth-mask pair ({@code :61}, {@code :69}) is a
 * property of the render type in 1.21.1 and not a per-call state, the alpha
 * floor is 1/10 and not 1/255, and the blend factors are the platform's
 * translucency and not {@code SRC_ALPHA, ONE}.</p>
 *
 * <p><b>The eye pass of {@code :76-99}.</b> The reference binds {@code
 * textures/models/taint_spider_eyes.png} ({@code :80}), sets blending to {@code
 * ONE, ONE} — additive ({@code :84}) — with the alpha test off ({@code :83}),
 * draws at {@code char c0 = '\uf0f0'} full brightness ({@code :91-94}) in opaque
 * white ({@code :95-96}), and returns 1 so vanilla's eye pass is skipped. 1.21.1's
 * twin is an {@link EyesLayer}: {@code RenderType.eyes(location)} is the eye
 * shader under {@code ADDITIVE_TRANSPARENCY} with a colour write mask that never
 * touches depth ({@code RenderType.java:254-272}, {@code :886-888}), and the eye
 * shader has no discard at all ({@code shaders/core/rendertype_eyes.fsh}) — the
 * {@code :83} arm. The brightness is {@code LightTexture.FULL_BRIGHT}
 * ({@code LightTexture.java:20}), the platform's name for the {@code :91}
 * literal, which is one axis brighter than the vanilla layer's {@code 15728640}
 * ({@code EyesLayer.java:57}) — the original's {@code 0xF0F0} is out of 1.7.10's
 * 0..240 lightmap range and clamps to full on both axes. Only the vanilla layer
 * hardcodes the vanilla sheet and takes no texture argument
 * ({@code SpiderEyesLayer.java:13,15}), so the pass is this file's own {@link
 * MindSpiderEyesLayer}, carrying the ARR sheet of {@code :80} and handing the
 * model an explicit white, the {@code :95-96} {@code glColor4f(1,1,1,1)}. The
 * {@code :85-89} depth-mask switch on the spider's hidden flag has no twin (the
 * eye state never writes depth), so a hidden spider's eyes no longer show through
 * walls — BD-87.</p>
 *
 * <p><b>The name gate of {@code :34-53}.</b> All three {@code doRender}
 * overloads carry the same test: draw when {@code getViewer().length() == 0}
 * (nobody mind-controlled it → everybody sees it) <b>or</b> when the viewer
 * equals the client player's name ({@code :35-36}, repeated at {@code :42-43}
 * and {@code :49-50}, where the name comes from {@code
 * this.field_76990_c.field_78734_h.func_70005_c_()} — the render manager's
 * current player, whose 1.21.1 twin is {@code Minecraft.getInstance().player},
 * the route {@code LivingEntityRenderer.java:120} takes). A mind-controlled
 * spider is invisible to every other player. 1.21.1 has one visibility entry
 * point, so the three overloads collapse to {@link #shouldRender} over {@link
 * #drawsForViewer}. Nothing in this port writes {@code viewer} (the
 * mind-control lane is the sibling card {@code t_1a8370db}, and BD-78 already
 * records "viewer has no consumer"), so the empty arm is the live one and every
 * mind spider draws for everyone — the original's behaviour for a spider nobody
 * mind-controlled.</p>
 *
 * <p><b>What has no twin.</b> {@code :23} hands a <b>second</b> {@code
 * ModelSpider} to {@code RenderLiving}, the model-projected shadow; 1.21.1 draws
 * a round shadow of radius 0.0 (the {@code :22} figure), so the shadow is off in
 * both trees and the second model drives nothing. 1.21.1 draws no red model pass
 * for a hurt mob — the hurt flash is the overlay {@code v} bit ({@code
 * LivingEntityRenderer.java:127,152-156}) — where 1.7.10's {@code RenderLiving}
 * recoloured a model pass; the fade here is spent on the body draw only, so the
 * flash is not tinted. {@code :72-74} is a {@code super}-only override, ported
 * as the absence of a pass of its own, and a test pins that this renderer adds
 * exactly one layer. The 1.7.10 geometry is 1.21.1 {@code SpiderModel} under a
 * different skeleton and the reference's own texture path ({@code
 * textures/models/}, not the modern {@code textures/entity/spider/}) proves the
 * sheets do not match, so no part-by-part geometry parity is claimed. All of it
 * is BD-87.</p>
 *
 * <p><b>Textures are referenced by path only (D-004).</b> {@code :19} and {@code
 * :80} are rows 1035/1036 of {@code docs/asset-mapping.csv}
 * ({@code textures/models/taint_spider.png}, {@code
 * textures/models/taint_spider_eyes.png}; ARR overlay, local build only), so the
 * paths resolve on a client only when that overlay is installed — {@code
 * references/asset-json-and-model-gates.md}.</p>
 */
public class RenderMindSpider extends MobRenderer<EntityMindSpider, SpiderModel<EntityMindSpider>> {

    /** {@code :19} — {@code new ResourceLocation("thaumcraft", "textures/models/taint_spider.png")}. */
    public static final ResourceLocation SPIDER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/taint_spider.png");
    /** {@code :80} — {@code UtilsFX.bindTexture("textures/models/taint_spider_eyes.png")}. */
    public static final ResourceLocation SPIDER_EYES_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/taint_spider_eyes.png");

    /** {@code :22} — {@code super(new ModelSpider(), 0.0F)}: no shadow. */
    public static final float SHADOW_RADIUS = 0.0F;
    /** {@code :26-28} via {@code :110-112} — the spider lies flat, not on its
     *  side; 1.21.1's default is 90.0 ({@code LivingEntityRenderer.java:227-229})
     *  and {@code SpiderRenderer.java:25-27} is the platform's own 180.0. */
    public static final float DEATH_FLIP_DEGREES = 180.0F;
    /** {@code :101-104} — {@code glScalef(f1, f1, f1)} with {@code f1 =
     *  spiderScaleAmount() = 0.3F} (reference {@code EntityMindSpider.java:67-70}),
     *  the same 0.3 the registered box carries ({@code MindSpiderAiLogic.SCALE},
     *  kept as one number). */
    public static final float SPIDER_SCALE = MindSpiderAiLogic.SCALE;
    /** {@code :91-94} — {@code char c0 = '\uf0f0'}, out of 1.7.10's lightmap
     *  range and clamped to full bright, which 1.21.1 names
     *  {@code LightTexture.FULL_BRIGHT} (0xF000F0). */
    public static final int EYES_LIGHT = LightTexture.FULL_BRIGHT;

    /** {@code :60} — the alpha cap of {@code Math.min(0.1F, age/100.0F)}. */
    public static final float FADE_ALPHA_CAP = 0.1F;
    /** {@code :60} — the divisor of the same expression. */
    public static final float FADE_AGE_DIVISOR = 100.0F;
    /** {@code :95-96} — the eye pass draws in opaque white. */
    public static final int EYES_COLOR = 0xFFFFFFFF;

    public RenderMindSpider(final EntityRendererProvider.Context context) {
        // :21-24 — the reference bakes its own ModelSpider (the proxy hands the
        // renderer no model, ClientProxy.java:450), so the port self-bakes the
        // platform's spider geometry, the way the golem renderers bake theirs: a
        // provider then needs no client dispatch to build a renderer.
        this(context, SpiderModel.createSpiderBodyLayer().bakeRoot());
    }

    public RenderMindSpider(final EntityRendererProvider.Context context, final ModelPart root) {
        super(context, new FadeModel(root), SHADOW_RADIUS);
        // :76-99 via :114-116 — the eye pass. :72-74 is a super-only no-op, so
        // this renderer adds exactly one layer and draws no pass of its own.
        this.addLayer(new MindSpiderEyesLayer(this));
    }

    /** {@code :101-108} — the 0.3 model scale, this renderer's only size change. */
    @Override
    protected void scale(final EntityMindSpider spider, final PoseStack poseStack, final float partialTick) {
        poseStack.scale(SPIDER_SCALE, SPIDER_SCALE, SPIDER_SCALE);
    }

    /** {@code :26-28} — a dead mind spider is flipped end over end. */
    @Override
    protected float getFlipDegrees(final EntityMindSpider spider) {
        return DEATH_FLIP_DEGREES;
    }

    /** {@code :22} — the 0.0 shadow of the constructor, for every entity. 1.21.1
     *  scales the hook by the entity's own scale ({@code
     *  LivingEntityRenderer.java:282-283}, {@code MobRenderer.java:19-20}) and 0.0
     *  times anything is 0.0, so the shadow stays off. */
    @Override
    protected float getShadowRadius(final EntityMindSpider spider) {
        return SHADOW_RADIUS;
    }

    /** {@code :30-32}. */
    @Override
    public ResourceLocation getTextureLocation(final EntityMindSpider spider) {
        return SPIDER_TEXTURE;
    }

    /**
     * {@code :57-64} — the body is drawn alpha-blended with the depth mask off,
     * so its render type is the translucent one. The renderer is asked for the
     * type ({@code LivingEntityRenderer.java:124,143-150}); the model's own type
     * is the cutout default and {@code Model.renderType} is final ({@code
     * Model.java:13-21}), so this is the seam. The invisible and glowing arms are
     * left to the platform, whose 15 % translucent twin and outline pass 1.7.10
     * reaches through calls this file does not override.
     */
    @Override
    protected RenderType getRenderType(final EntityMindSpider spider, final boolean bodyVisible,
            final boolean translucent, final boolean glowing) {
        return bodyVisible ? RenderType.entityTranslucent(SPIDER_TEXTURE)
                : super.getRenderType(spider, bodyVisible, translucent, glowing);
    }

    /**
     * {@code :34-53} — the three {@code doRender} gates, in 1.21.1's single
     * visibility entry point. The local player's name is read where the
     * reference reads it (its render manager's current player, {@code :49}); the
     * platform's twin is {@code Minecraft.getInstance().player} ({@code
     * LivingEntityRenderer.java:120}) and its name string is the scoreboard name,
     * the form {@code EntityMindSpider#setViewer} (:38-44) is written in.
     */
    @Override
    public boolean shouldRender(final EntityMindSpider spider, final Frustum camera, final double camX,
            final double camY, final double camZ) {
        final LocalPlayer local = Minecraft.getInstance().player;
        return drawsForViewer(spider.getViewer(), local == null ? null : local.getScoreboardName());
    }

    /**
     * The pure rule of {@code :35-36}: an empty viewer draws for everyone, a
     * viewer name draws only for that player. Plain {@code String.equals}, as the
     * original uses. A null local name can only satisfy the empty arm — 1.7.10
     * would have dereferenced a null current player on the second arm, which a
     * pre-world 1.21.1 client never reaches.
     */
    public static boolean drawsForViewer(final String viewer, final String localPlayerName) {
        return viewer.isEmpty() || viewer.equals(localPlayerName);
    }

    /** {@code :60} — the body alpha of the frame, {@code min(0.1, age/100)}. */
    public static float fadeAlpha(final int ageInTicks) {
        return Math.min(FADE_ALPHA_CAP, ageInTicks / FADE_AGE_DIVISOR);
    }

    /**
     * {@code :60} — the same figure as the white vertex colour the original
     * writes. 1.21.1 hands a model an ARGB int and its alpha is the top byte —
     * the platform's own 15 % case is {@code 654311423} = 0x270000FF ({@code
     * LivingEntityRenderer.java:127}) — so the alpha is packed into that byte
     * with the same clamp the house uses for a model tint ({@code
     * RenderGolemBase.damageTint}, {@code :399-402}).
     */
    public static int fadeColor(final int ageInTicks) {
        final int alpha = (int) (255.0F * fadeAlpha(ageInTicks));
        return (Math.max(0, Math.min(255, alpha)) << 24) | 0x00FFFFFF;
    }

    /**
     * The spider model that carries {@code :55-70}'s vertex colour.
     *
     * <p>1.21.1 gives no renderer colour hook, but it calls {@code
     * EntityModel#prepareMobModel(entity, …)} once per frame immediately before
     * the body draw ({@code LivingEntityRenderer.java:118} → {@code :127}), and
     * the body is the only draw of the frame whose colour the renderer chooses:
     * the eye layer asks for its own ({@link MindSpiderEyesLayer}, the {@code
     * :95-96} white). So the fade is armed at prepare time and spent on the body,
     * and every other caller keeps the colour it asked for — one frame, one fade,
     * no leak into the eyes.</p>
     */
    static final class FadeModel extends SpiderModel<EntityMindSpider> {

        private boolean armed;
        private int fadeColor;

        FadeModel(final ModelPart root) {
            super(root);
        }

        @Override
        public void prepareMobModel(final EntityMindSpider spider, final float limbSwing,
                final float limbSwingAmount, final float partialTick) {
            super.prepareMobModel(spider, limbSwing, limbSwingAmount, partialTick);
            // :60 — the alpha is a function of the entity's age (1.7.10
            // `field_70173_aa`), which 1.21.1 keeps as the public `tickCount`
            // (Entity.java:194).
            this.fadeColor = fadeColor(spider.tickCount);
            this.armed = true;
        }

        @Override
        public void renderToBuffer(final PoseStack poseStack, final VertexConsumer buffer, final int packedLight,
                final int packedOverlay, final int color) {
            super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, this.armed ? this.fadeColor : color);
            this.armed = false;
        }
    }

    /**
     * The eye pass of {@code :76-99}. The platform's own layer hardcodes vanilla's
     * eye sheet and takes no texture ({@code SpiderEyesLayer.java:13,15}), so this
     * one carries the ARR sheet of {@code :80} and draws it additive ({@code :84})
     * at full bright ({@code :91-94}) in opaque white ({@code :95-96}) — through
     * the 5-argument {@code renderToBuffer}, so the body's fade cannot be
     * inherited through the 4-argument default, which passes {@code -1}
     * ({@code Model.java:25-27}).
     */
    static final class MindSpiderEyesLayer extends EyesLayer<EntityMindSpider, SpiderModel<EntityMindSpider>> {

        private static final RenderType EYES = RenderType.eyes(SPIDER_EYES_TEXTURE);

        MindSpiderEyesLayer(final RenderLayerParent<EntityMindSpider, SpiderModel<EntityMindSpider>> parent) {
            super(parent);
        }

        @Override
        public RenderType renderType() {
            return EYES;
        }

        @Override
        public void render(final PoseStack poseStack, final MultiBufferSource buffer, final int packedLight,
                final EntityMindSpider spider, final float limbSwing, final float limbSwingAmount,
                final float partialTicks, final float ageInTicks, final float netHeadYaw, final float headPitch) {
            this.getParentModel().renderToBuffer(poseStack, buffer.getBuffer(EYES), EYES_LIGHT,
                    OverlayTexture.NO_OVERLAY, EYES_COLOR);
        }
    }
}
