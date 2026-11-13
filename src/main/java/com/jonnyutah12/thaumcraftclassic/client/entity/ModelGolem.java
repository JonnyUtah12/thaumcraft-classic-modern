package com.jonnyutah12.thaumcraftclassic.client.entity;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.resources.ResourceLocation;

/**
 * The golem body model — Thaumcraft 4.2.3.5
 * {@code thaumcraft.client.renderers.models.entities.ModelGolem}, transcribed
 * part for part from
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/renderers/models/entities/ModelGolem.java}
 * (166 lines). Card TC-M6-08-s-c2.
 *
 * <p><b>Constructor argument baked.</b> The original takes a {@code boolean p}
 * that only selects the y offset of every rotation point:
 * {@code float f2 = p ? -5.0F : 30.0F} (line 22). The only shipped construction
 * is {@code new ModelGolem(false)} — {@code ClientProxy.java:416} for the
 * renderer, and {@code RenderGolemBase.java:55} for the damage copy — so this
 * port bakes {@code f2 = 30.0F} into every {@link PartPose} (head {@code 0+30},
 * legs {@code 18+30}) and drops the parameter. The {@code p = true} figure
 * ({@code -5.0F}) is dead in the shipped mod; nothing constructs it.</p>
 *
 * <p><b>MCP names (stable_11) used below.</b> {@code func_78793_a} =
 * {@code setRotationPoint} → {@link PartPose#offset(float, float, float)};
 * {@code func_78784_a} = {@code setTextureOffset} →
 * {@code CubeListBuilder.texOffs}; {@code func_78790_a} = {@code addBox(u, v, x,
 * y, z, w, h, d, scale)} → {@code texOffs(u, v).addBox(x, y, z, w, h, d, new
 * CubeDeformation(scale))}; {@code func_78787_b(128, 128)} =
 * {@code setTextureSize} → the one {@link LayerDefinition#create} call;
 * {@code field_78809_i = true} = {@code mirror} →
 * {@code CubeListBuilder.mirror()}, written before the box it mirrors;
 * {@code field_78795_f}/{@code field_78796_g}/{@code field_78808_h} =
 * {@code rotateAngleX}/{@code rotateAngleY}/{@code rotateAngleZ} →
 * {@code ModelPart.xRot/yRot/zRot}.</p>
 *
 * <p><b>The {@code pass} field.</b> The original's public {@code int pass}
 * (line 18) selects a GL state pair inside its render method (lines 51-68:
 * {@code GL_BLEND} + {@code GL_ALPHA_FUNC 1/255} when {@code pass == 2}). 1.21.1
 * has no per-model GL hook, so the two documented deltas are:
 * the field is kept (and pinned by a test) because {@code RenderGolemBase}
 * writes it exactly as the original does ({@code RenderGolemBase.java:56}), and
 * the blend/alpha state is expressed by the render type of the damage
 * {@code RenderLayer} (see {@link RenderGolemBase.DamageLayer}). The geometry of
 * the two passes is identical, exactly as in the original: the second pass is
 * the same six parts against {@code textures/models/golem_damage.png}
 * ({@code RenderGolemBase.java:126}).</p>
 *
 * <p><b>Heal tint.</b> The original's {@code GL11.glColor3f(0.5F + h1, 0.9F +
 * h2, 0.5F + h1)} (lines 81-85) is a GL state change inside the animation
 * callback. {@link #healingTint(int, int)} returns the same colour as the packed
 * ARGB int 1.21.1 hands to {@link #renderToBuffer}, and
 * {@link RenderGolemBase#getColorForLivingEntity} feeds it. The original's
 * unclamped floats (a heal of 10 gives {@code 1.5F}) are clamped to 1.0F, which
 * is what GL does with them.</p>
 *
 * <p>Textures are referenced by path only (D-004); no original PNG is copied.
 * The body sheet is chosen by material in {@link RenderGolemBase}.</p>
 */
public class ModelGolem extends EntityModel<EntityGolemBase> {

    /** The body layer: {@code thaumcraft:entity/golem}, {@code main}. */
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thaumcraft", "entity/golem"), "main");

    /**
     * The damage pass gets its own baked copy of the same geometry, which is
     * what {@code RenderGolemBase.java:54-58} does in 1.7.10
     * ({@code new ModelGolem(false); mg.pass = 2}).
     */
    public static final ModelLayerLocation DAMAGE_LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thaumcraft", "entity/golem_damage"), "main");

    /** The original's {@code public int pass = 0} (line 18). */
    public int pass = 0;

    /** {@code (float)Math.PI}-derived divisor the original repeats inline (lines 94-97, 108-109). */
    private static final float TO_RADIANS = 180.0F / (float) Math.PI;

    /** {@code 0.0F + f2} with the shipped {@code f2 = 30.0F}. */
    private static final float BODY_Y = 30.0F;
    /** {@code 18.0F + f2} with the shipped {@code f2 = 30.0F} (lines 40, 44). */
    private static final float LEG_Y = 48.0F;

    private final ModelPart golemHead;
    private final ModelPart golemBody;
    private final ModelPart golemRightArm;
    private final ModelPart golemLeftArm;
    private final ModelPart golemRightLeg;
    private final ModelPart golemLeftLeg;

    public ModelGolem(final ModelPart root) {
        this.golemHead = root.getChild("golemHead");
        this.golemBody = root.getChild("golemBody");
        this.golemRightArm = root.getChild("golemRightArm");
        this.golemLeftArm = root.getChild("golemLeftArm");
        this.golemRightLeg = root.getChild("golemRightLeg");
        this.golemLeftLeg = root.getChild("golemLeftLeg");
    }

    /**
     * The six parts, transcribed from the constructor (lines 20-46). The
     * texture is 128x128 ({@code var3}/{@code var4}, lines 23-24, applied
     * per part at lines 25, 28, 32, 35, 39, 42) and is stated once here.
     */
    public static LayerDefinition layer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();

        // :26 setRotationPoint(0, 0 + f2, -2); :27 tex(0,0) box(-4,-11,-5.5) 8x9x8 scale f1=0
        root.addOrReplaceChild("golemHead",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -11.0F, -5.5F, 8, 9, 8, CubeDeformation.NONE),
                PartPose.offset(0.0F, BODY_Y, -2.0F));

        // :29 setRotationPoint(0, 0 + f2, 0); :30 tex(0,40) box(-8,-2,-6) 16x12x11; :31 tex(0,70)
        // box(-4.5,10,-3) 9x5x6 with scale f1 + 0.5F — the only non-zero deformation in the model
        root.addOrReplaceChild("golemBody",
                CubeListBuilder.create()
                        .texOffs(0, 40).addBox(-8.0F, -2.0F, -6.0F, 16, 12, 11, CubeDeformation.NONE)
                        .texOffs(0, 70).addBox(-4.5F, 10.0F, -3.0F, 9, 5, 6, new CubeDeformation(0.5F)),
                PartPose.offset(0.0F, BODY_Y, 0.0F));

        // :33 setRotationPoint(0, 0 + f2, 0); :34 tex(60,21) box(-12,-2.5,-3) 4x25x6
        root.addOrReplaceChild("golemRightArm",
                CubeListBuilder.create().texOffs(60, 21).addBox(-12.0F, -2.5F, -3.0F, 4, 25, 6, CubeDeformation.NONE),
                PartPose.offset(0.0F, BODY_Y, 0.0F));

        // :36 field_78809_i = true (mirror); :37 setRotationPoint(0, 0 + f2, 0);
        // :38 tex(60,21) box(8,-2.5,-3) 4x25x6
        root.addOrReplaceChild("golemLeftArm",
                CubeListBuilder.create().mirror()
                        .texOffs(60, 21).addBox(8.0F, -2.5F, -3.0F, 4, 25, 6, CubeDeformation.NONE),
                PartPose.offset(0.0F, BODY_Y, 0.0F));

        // :40 setRotationPoint(-4, 18 + f2, 0); :41 tex(37,0) box(-3.5,-3,-3) 6x16x5
        // The leg constructors of :39/:42 pass a DEFAULT uv of (0, 22), which
        // func_78784_a(37, 0) at :41/:44 overwrites before the only box each leg
        // carries is added — so 22 is dead in the source and 37,0 is the leg's
        // texture region. Recorded because the literal diff between the two trees
        // shows 22 on the reference side only.
        root.addOrReplaceChild("golemRightLeg",
                CubeListBuilder.create().texOffs(37, 0).addBox(-3.5F, -3.0F, -3.0F, 6, 16, 5, CubeDeformation.NONE),
                PartPose.offset(-4.0F, LEG_Y, 0.0F));

        // :43 mirror; :44 tex(37,0) then setRotationPoint(5, 18 + f2, 0); :45 box(-3.5,-3,-3) 6x16x5
        root.addOrReplaceChild("golemLeftLeg",
                CubeListBuilder.create().mirror()
                        .texOffs(37, 0).addBox(-3.5F, -3.0F, -3.0F, 6, 16, 5, CubeDeformation.NONE),
                PartPose.offset(5.0F, LEG_Y, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    /**
     * {@code setRotationAngles} (lines 73-123) verbatim. The {@code en instanceof
     * EntityGolemBase} guard of lines 77-86 is structural in 1.21.1 — the model
     * is parameterised on {@link EntityGolemBase} — so the core/bootup/inactive
     * reads are unconditional and the heal tint moves to
     * {@link RenderGolemBase#getColorForLivingEntity}.
     *
     * @param limbSwing       the source's {@code par1} (the leg-swing phase)
     * @param limbSwingAmount the source's {@code par2} (the leg-swing amplitude)
     * @param netHeadYaw      the source's {@code par4}
     * @param headPitch       the source's {@code par5}
     */
    @Override
    public void setupAnim(final EntityGolemBase golem, final float limbSwing, final float limbSwingAmount,
            final float ageInTicks, final float netHeadYaw, final float headPitch) {
        final int core = golem.getCore();
        final float bootup = golem.getBootup();
        final boolean inactive = golem.isInactive();

        if (core != -1 && !(bootup < 0.0F)) {
            if (inactive) {
                this.golemHead.yRot = 0.0F;
                this.golemHead.xRot = 0.57595867F; // :91 the slumped head of an inactive golem
            } else if (bootup > 0.0F) {
                this.golemHead.yRot = 0.0F;
                this.golemHead.xRot = bootup / TO_RADIANS; // :94 the boot-up ramp
            } else {
                this.golemHead.yRot = netHeadYaw / TO_RADIANS; // :96
                this.golemHead.xRot = headPitch / TO_RADIANS; // :97
            }

            this.golemRightLeg.xRot = -1.5F * wave(limbSwing, 13.0F) * limbSwingAmount; // :100
            this.golemLeftLeg.xRot = 1.5F * wave(limbSwing, 13.0F) * limbSwingAmount; // :101
            this.golemRightLeg.yRot = 0.0F; // :102
            this.golemLeftLeg.yRot = 0.0F; // :103
            this.golemLeftArm.zRot = 0.0F; // :104
            this.golemRightArm.zRot = 0.0F; // :105

            if (core == 6) {
                // :107-109 the "hauling" arm splay: a core-6 golem opens its arms by
                // (1 - (0.5 + min(64, carry) / 128)) * 25 degrees.
                final float s = (1.0F - (0.5F + Math.min(64, golem.getCarryLimit()) / 128.0F)) * 25.0F;
                this.golemLeftArm.zRot = s / TO_RADIANS;
                this.golemRightArm.zRot = -s / TO_RADIANS;
            }
        } else {
            // :112-121 no core, or the boot-up counter is negative: everything zeroed,
            // head slumped at the same 0.57595867F.
            this.golemHead.yRot = 0.0F;
            this.golemHead.xRot = 0.57595867F;
            this.golemRightLeg.xRot = 0.0F;
            this.golemLeftLeg.xRot = 0.0F;
            this.golemRightArm.xRot = 0.0F;
            this.golemLeftArm.xRot = 0.0F;
            this.golemRightLeg.yRot = 0.0F;
            this.golemLeftLeg.yRot = 0.0F;
            this.golemLeftArm.zRot = 0.0F;
            this.golemRightArm.zRot = 0.0F;
        }
    }

    /**
     * {@code func_78086_a} / {@code setLivingAnimations} (lines 125-161)
     * verbatim, mapped to 1.21.1's {@code prepareMobModel(entity,
     * limbSwingAmount, ageInTicks, netHeadYaw)}. The source's {@code par2} and
     * {@code par3} (the walk phase and amplitude, MCP {@code field_70754_ba} =
     * limbSwing and {@code field_70721_aZ} = limbSwingAmount, verified in
     * {@code reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv}) are read
     * from {@code golem.walkAnimation} because 1.21.1's callback does not
     * receive them; {@code par4} (the partial-tick phase) is the
     * {@code netHeadYaw} slot's neighbour in 1.21.1's signature, so the source's
     * partial-tick term is taken as 0 — the swing is then sampled at whole
     * ticks, which is the same figure the source shows on a 0-partial frame.
     */
    @Override
    public void prepareMobModel(final EntityGolemBase golem, final float limbSwingAmount, final float ageInTicks,
            final float netHeadYaw) {
        final int actionTimer = golem.getActionTimer();
        final int leftArm = golem.getLeftArm();
        final int rightArm = golem.getRightArm();
        final boolean bucket = golem.getCore() == 5;

        if (actionTimer > 0) {
            // :142-143 the strike: both arms from the -2.0F + 1.5F wave on a 5-tick period
            final float swing = -2.0F + 1.5F * wave(actionTimer, 5.0F);
            this.golemRightArm.xRot = swing;
            this.golemLeftArm.xRot = swing;
        } else if (leftArm <= 0 && rightArm <= 0) {
            if (golem.getCarried() == null && !bucket) {
                // :146-147 the walk swing, mirrored between the arms
                this.golemRightArm.xRot = (-0.2F + 1.5F * wave(ageInTicks, 13.0F)) * limbSwingAmount;
                this.golemLeftArm.xRot = (-0.2F - 1.5F * wave(ageInTicks, 13.0F)) * limbSwingAmount;
            } else {
                // :149-150 carrying: both arms level at -1.0F
                this.golemRightArm.xRot = -1.0F;
                this.golemLeftArm.xRot = -1.0F;
            }
        } else {
            if (leftArm > 0) {
                this.golemLeftArm.xRot = -2.0F + 1.5F * wave(leftArm, 20.0F); // :154
            }
            if (rightArm > 0) {
                this.golemRightArm.xRot = -2.0F + 1.5F * wave(rightArm, 20.0F); // :158
            }
        }
    }

    /**
     * The original's render method (lines 48-71): the animation callback first,
     * then {@code GL11.glScaled(0.4, 0.4, 0.4)} (line 57) and the six parts in
     * the source's draw order (head, body, right leg, left leg, right arm, left
     * arm, lines 58-63). The {@code pass == 2} GL block (lines 51-55, 65-68) is
     * the render type of {@link RenderGolemBase.DamageLayer}.
     */
    @Override
    public void renderToBuffer(final PoseStack poseStack, final VertexConsumer vertexConsumer,
            final int packedLight, final int packedOverlay, final int color) {
        poseStack.pushPose();
        poseStack.scale(0.4F, 0.4F, 0.4F); // :57
        this.golemHead.render(poseStack, vertexConsumer, packedLight, packedOverlay, color); // :58
        this.golemBody.render(poseStack, vertexConsumer, packedLight, packedOverlay, color); // :59
        this.golemRightLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color); // :60
        this.golemLeftLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color); // :61
        this.golemRightArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color); // :62
        this.golemLeftArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color); // :63
        poseStack.popPose();
    }

    /**
     * The heal tint of lines 81-85 as a packed ARGB int: {@code (0.5F + h1,
     * 0.9F + h2, 0.5F + h1)} with {@code h1 = healing / 10}, {@code h2 =
     * healing / 5}, only on {@code pass == 0} with {@code healing > 0}, else
     * white. Each channel is clamped to 1.0F, which is what the fixed-function
     * pipeline did with the source's over-range floats.
     */
    public static int healingTint(final int healing, final int pass) {
        if (pass != 0 || healing <= 0) {
            return 0xFFFFFFFF;
        }
        final float h1 = healing / 10.0F;
        final float h2 = healing / 5.0F;
        return 0xFF000000 | (clamp255(0.5F + h1) << 16) | (clamp255(0.9F + h2) << 8) | clamp255(0.5F + h1);
    }

    private static int clamp255(final float channel) {
        return (int) (Math.min(1.0F, Math.max(0.0F, channel)) * 255.0F);
    }

    /**
     * The original's {@code func_78172_a} (lines 163-165), unchanged. Public so
     * the pinned figures are unit-testable (the same triangle wave also drives
     * the golem's body sway in {@link RenderGolemBase}).
     */
    public static float wave(final float value, final float period) {
        return (Math.abs(value % period - period * 0.5F) - period * 0.25F) / (period * 0.25F);
    }

    /** Read-only access for tests and for the layers that copy rotations. */
    protected ModelPart head() {
        return this.golemHead;
    }

    /** Read-only access for the {@link RenderGolemBase} carried-item tilt (:153). */
    ModelPart rightArm() {
        return this.golemRightArm;
    }
}
