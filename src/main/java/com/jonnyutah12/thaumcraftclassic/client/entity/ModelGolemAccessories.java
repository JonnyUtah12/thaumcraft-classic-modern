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
 * The golem decoration accessories model, ported from
 * {@code thaumcraft.client.renderers.models.entities.ModelGolemAccessories}
 * (reference/decompiled/thaumcraft/thaumcraft/client/renderers/models/entities/ModelGolemAccessories.java),
 * card TC-M6-08-s-c2. The whole 214-line source is transcribed: geometry from the
 * two-arg constructor (lines 34-80), per-decoration draw selection from the render
 * method {@code func_78088_a} (lines 82-153), head pose from {@code setRotationAngles}
 * (lines 155-184), arm/tool animation from {@code func_78086_a} (lines 186-209), and
 * the oscillator {@code func_78172_a} (lines 211-213).
 *
 * <p>The shipped geometry is baked for the one constructor pair the renderer uses,
 * {@code new ModelGolemAccessories(0.0F, 30.0F)} (RenderGolemBase.java:60):
 * {@code par1 = 0} makes every box's {@link CubeDeformation} {@code NONE} (the fez,
 * glasses and every other box take {@code par1}; the hat-rim box at source line 61
 * passes a literal {@code 0.0F}), and {@code par2 = 30} shifts every part's pose Y
 * by +30. The port therefore takes no {@code p}/{@code par} constructor parameters.</p>
 *
 * <p>MCP-name mapping (1.7.10 → 1.21.1):</p>
 * <ul>
 *   <li>{@code func_78793_a} = setRotationPoint(x, y, z) → {@code PartPose.offset(x, y + 30, z)}</li>
 *   <li>{@code func_78784_a} = setTextureOffset(u, v) → {@code CubeListBuilder.texOffs(u, v)}</li>
 *   <li>{@code func_78790_a} = addBox(x, y, z, w, h, d, scale) →
 *       {@code texOffs(u, v).addBox(x, y, z, w, h, d, deformation)} (scale 0 → {@code NONE})</li>
 *   <li>{@code func_78787_b(128, 128)} = setTextureSize → the global
 *       {@code LayerDefinition.create(mesh, 128, 128)}</li>
 *   <li>{@code field_78809_i = true} (mirror) → {@code CubeListBuilder.mirror()} written
 *       before the addBox call on that builder (only golemPlateRight, source line 47)</li>
 *   <li>{@code field_78795_f} = rotateAngleX → {@code ModelPart.xRot};
 *       {@code field_78796_g} = rotateAngleY → {@code ModelPart.yRot}</li>
 *   <li>{@code func_78088_a} (render) → {@code renderToBuffer} + the {@code visible}
 *       flags set in {@code setupAnim}; {@code func_78086_a} (setLivingAnimations) →
 *       {@code prepareMobModel}; {@code setRotationAngles} → {@code setupAnim};
 *       {@code func_78172_a} → {@link #wave(float, float)}</li>
 *   <li>Entity accessors: {@code getGolemDecoration()} → {@code getDecoration()},
 *       {@code advanced} → {@code isAdvanced()}, {@code inactive} → {@code isInactive()},
 *       {@code bootup} → {@code getBootup()}, {@code leftArm}/{@code rightArm} →
 *       {@code getLeftArm()}/{@code getRightArm()}, {@code getCarriedForDisplay()} →
 *       {@code getCarried()} (the port's name for the original's carried/display stack)</li>
 *   <li>Living accessors: MCP {@code field_70754_ba} = limbSwing and
 *       {@code field_70721_aZ} = limbSwingAmount (verified in
 *       reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv) map onto
 *       {@code golem.walkAnimation.position()}/{@code speed()}, because 1.21.1's
 *       {@code prepareMobModel} no longer receives the limb-swing pair.</li>
 * </ul>
 *
 * <p>Documented deltas from the 1.7.10 original:</p>
 * <ol>
 *   <li>The {@code GL11.glEnable(GL_BLEND)}/{@code glBlendFunc(770, 771)} around the
 *       bowtie (source lines 112-115) and the jar (lines 139-142) is a per-part GL
 *       state change in 1.7.10; in 1.21.1 blending is a whole-layer render-type
 *       property chosen by the render layer, not something a model part can toggle
 *       mid-buffer, so no per-part blend code is ported.</li>
 *   <li>All 14 parts share one 128x128 UV sheet (the {@code var3 = var4 = 128} at
 *       source lines 35-36) — the original attached 128x128 sizes to every part
 *       individually; the port states the sheet once via {@code LayerDefinition.create}.</li>
 * </ol>
 *
 * <p>Draw selection: the original picked parts to draw per decoration character in
 * {@code func_78088_a} (lines 87-150); here {@code setupAnim} sets each part's
 * {@code visible} flag from {@code getDecoration()}/{@code isAdvanced()}/{@code getCore()},
 * and {@code renderToBuffer} renders every part — the visible flags ARE the original's
 * draw selection.</p>
 */
public final class ModelGolemAccessories extends EntityModel<EntityGolemBase> {

    /** The accessories layer; the render layer picks the decoration texture. */
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thaumcraft", "entity/golem_accessories"), "main");

    /**
     * The decoration sheet the original bound with
     * {@code UtilsFX.bindTexture("textures/models/golem_decoration.png")}
     * (RenderGolemBase.java:120). Path only — the PNG itself is never copied (D-004).
     */
    public static final ResourceLocation ACCESSORIES_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/golem_decoration.png");

    /** Degrees-to-radians divisor, the original's {@code 180.0F / (float) Math.PI} (source lines 164-167). */
    private static final float TO_RADIANS = 180.0F / (float) Math.PI;

    private final ModelPart golemHeadFez;
    private final ModelPart golemPlate;
    private final ModelPart golemPlateLeft;
    private final ModelPart golemPlateRight;
    private final ModelPart golemHeadHat;
    private final ModelPart golemHeadGlasses;
    private final ModelPart golemVisor;
    private final ModelPart golemHeadHatRim;
    private final ModelPart golemDartgun;
    private final ModelPart golemMace;
    private final ModelPart golemBowtie;
    private final ModelPart golemHeadJar;
    private final ModelPart golemHeadBrain;
    private final ModelPart golemEvilHead;

    /**
     * Cached {@code isAdvanced()} from the last {@code setupAnim}; drives the
     * advanced-only {@code GL11.glTranslatef(0.0F, -0.01F, 0.0F)} lifts of the fez and
     * hat branches (source lines 95 and 103) inside {@code renderToBuffer}, which has
     * no entity parameter.
     */
    private boolean advancedLift;

    /**
     * Builds the 128x128 geometry for the shipped (0.0F, 30.0F) constructor pair —
     * every pose Y is the source's {@code 0.0F + par2} with par2 = 30.
     *
     * @return the accessories layer definition
     */
    public static LayerDefinition layer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();

        // Source line 38-39 — fez, tex (0,94), box (-4.5,-15,-6) 9x7x9, pose (0, 0+par2, -2)
        root.addOrReplaceChild("golemHeadFez",
                CubeListBuilder.create().texOffs(0, 94).addBox(-4.5F, -15.0F, -6.0F, 9, 7, 9, CubeDeformation.NONE),
                PartPose.offset(0.0F, 30.0F, -2.0F));
        // Source line 41-42 — chest plate, tex (32,40), box (-6.5,-1,-7) 13x12x13, pose (0, 0+par2, 0)
        root.addOrReplaceChild("golemPlate",
                CubeListBuilder.create().texOffs(32, 40).addBox(-6.5F, -1.0F, -7.0F, 13, 12, 13, CubeDeformation.NONE),
                PartPose.offset(0.0F, 30.0F, 0.0F));
        // Source line 44-45 — left plate, tex (0,44), box (-8.5,-4,-6.5) 3x6x12, pose (0, 0+par2, 0)
        root.addOrReplaceChild("golemPlateLeft",
                CubeListBuilder.create().texOffs(0, 44).addBox(-8.5F, -4.0F, -6.5F, 3, 6, 12, CubeDeformation.NONE),
                PartPose.offset(0.0F, 30.0F, 0.0F));
        // Source line 47-49 — right plate, the only mirrored part (field_78809_i = true at line 47),
        // tex (0,44), box (5.5,-4,-6.5) 3x6x12, pose (0, 0+par2, 0)
        root.addOrReplaceChild("golemPlateRight",
                CubeListBuilder.create().mirror().texOffs(0, 44)
                        .addBox(5.5F, -4.0F, -6.5F, 3, 6, 12, CubeDeformation.NONE),
                PartPose.offset(0.0F, 30.0F, 0.0F));
        // Source line 51-52 — hat, tex (0,110), box (-4.5,-17,-6) 9x9x9, pose (0, 0+par2, -2)
        root.addOrReplaceChild("golemHeadHat",
                CubeListBuilder.create().texOffs(0, 110).addBox(-4.5F, -17.0F, -6.0F, 9, 9, 9, CubeDeformation.NONE),
                PartPose.offset(0.0F, 30.0F, -2.0F));
        // Source line 54-55 — glasses, tex (0,80), box (-4.5,-8,-6) 9x4x9, pose (0, 0+par2, -2)
        root.addOrReplaceChild("golemHeadGlasses",
                CubeListBuilder.create().texOffs(0, 80).addBox(-4.5F, -8.0F, -6.0F, 9, 4, 9, CubeDeformation.NONE),
                PartPose.offset(0.0F, 30.0F, -2.0F));
        // Source line 57-58 — visor, tex (0,70), box (-5,-8,-6) 10x5x5, pose (0, 0+par2, -2)
        root.addOrReplaceChild("golemVisor",
                CubeListBuilder.create().texOffs(0, 70).addBox(-5.0F, -8.0F, -6.0F, 10, 5, 5, CubeDeformation.NONE),
                PartPose.offset(0.0F, 30.0F, -2.0F));
        // Source line 60-61 — hat rim, tex (36,114), box (-6.5,-9,-8) 13x1x13 with the literal
        // 0.0F scale at line 61 (not par1), pose (0, 0+par2, -2)
        root.addOrReplaceChild("golemHeadHatRim",
                CubeListBuilder.create().texOffs(36, 114).addBox(-6.5F, -9.0F, -8.0F, 13, 1, 13, CubeDeformation.NONE),
                PartPose.offset(0.0F, 30.0F, -2.0F));
        // Source line 63-64 — dartgun, tex (80,80), box (7.9,7.5,-3.5) 6x16x7, pose (0, 0+par2, 0)
        root.addOrReplaceChild("golemDartgun",
                CubeListBuilder.create().texOffs(80, 80).addBox(7.9F, 7.5F, -3.5F, 6, 16, 7, CubeDeformation.NONE),
                PartPose.offset(0.0F, 30.0F, 0.0F));
        // Source line 66-67 — mace, tex (80,26), box (-13,15,-5) 6x8x10, pose (0, 0+par2, 0)
        root.addOrReplaceChild("golemMace",
                CubeListBuilder.create().texOffs(80, 26).addBox(-13.0F, 15.0F, -5.0F, 6, 8, 10, CubeDeformation.NONE),
                PartPose.offset(0.0F, 30.0F, 0.0F));
        // Source line 69-70 — bowtie, tex (0,0), box (-8.5,-2,-6.5) 17x4x12, pose (0, 0+par2, 0)
        root.addOrReplaceChild("golemBowtie",
                CubeListBuilder.create().texOffs(0, 0).addBox(-8.5F, -2.0F, -6.5F, 17, 4, 12, CubeDeformation.NONE),
                PartPose.offset(0.0F, 30.0F, 0.0F));
        // Source line 72-73 — head jar, tex (96,56), box (-4,-15,-5.5) 8x4x8, pose (0, 0+par2, -2)
        root.addOrReplaceChild("golemHeadJar",
                CubeListBuilder.create().texOffs(96, 56).addBox(-4.0F, -15.0F, -5.5F, 8, 4, 8, CubeDeformation.NONE),
                PartPose.offset(0.0F, 30.0F, -2.0F));
        // Source line 75-76 — brain, tex (96,70), box (-3.5,-14,-5) 7x3x7, pose (0, 0+par2, -2)
        root.addOrReplaceChild("golemHeadBrain",
                CubeListBuilder.create().texOffs(96, 70).addBox(-3.5F, -14.0F, -5.0F, 7, 3, 7, CubeDeformation.NONE),
                PartPose.offset(0.0F, 30.0F, -2.0F));
        // Source line 78-79 — evil head, tex (64,65), box (-4,-9,-5.5) 8x7x8, pose (0, 0+par2, -2)
        root.addOrReplaceChild("golemEvilHead",
                CubeListBuilder.create().texOffs(64, 65).addBox(-4.0F, -9.0F, -5.5F, 8, 7, 8, CubeDeformation.NONE),
                PartPose.offset(0.0F, 30.0F, -2.0F));

        // Source line 35-36 (short var3 = 128; short var4 = 128) applied once as func_78787_b(128, 128).
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Binds every part to its mesh child (geometry built by {@link #layer()}). */
    public ModelGolemAccessories() {
        final ModelPart root = layer().bakeRoot();
        this.golemHeadFez = root.getChild("golemHeadFez");
        this.golemPlate = root.getChild("golemPlate");
        this.golemPlateLeft = root.getChild("golemPlateLeft");
        this.golemPlateRight = root.getChild("golemPlateRight");
        this.golemHeadHat = root.getChild("golemHeadHat");
        this.golemHeadGlasses = root.getChild("golemHeadGlasses");
        this.golemVisor = root.getChild("golemVisor");
        this.golemHeadHatRim = root.getChild("golemHeadHatRim");
        this.golemDartgun = root.getChild("golemDartgun");
        this.golemMace = root.getChild("golemMace");
        this.golemBowtie = root.getChild("golemBowtie");
        this.golemHeadJar = root.getChild("golemHeadJar");
        this.golemHeadBrain = root.getChild("golemHeadBrain");
        this.golemEvilHead = root.getChild("golemEvilHead");
    }

    /**
     * Head pose (source {@code setRotationAngles}, lines 155-184) plus the decoration
     * draw-selection flags (from {@code func_78088_a}, lines 87-150 — see the class
     * javadoc). {@code netHeadYaw}/{@code headPitch} are the source's par4/par5
     * (lines 166-167).
     */
    @Override
    public void setupAnim(final EntityGolemBase golem, final float limbSwing, final float limbSwingAmount,
            final float ageInTicks, final float netHeadYaw, final float headPitch) {
        // Source lines 156-168: slumped core-off / booting / inactive head, boot-up ramp,
        // then the live head-follow. 0.57595867F is the literal slumped-pitch constant.
        if (golem.getCore() == -1 || golem.getBootup() < 0.0F) {
            this.golemHeadFez.yRot = 0.0F;
            this.golemHeadFez.xRot = 0.57595867F;
        } else if (golem.isInactive()) {
            this.golemHeadFez.yRot = 0.0F;
            this.golemHeadFez.xRot = 0.57595867F;
        } else if (golem.getBootup() > 0.0F) {
            this.golemHeadFez.yRot = 0.0F;
            this.golemHeadFez.xRot = golem.getBootup() / TO_RADIANS; // source line 164
        } else {
            this.golemHeadFez.yRot = netHeadYaw / TO_RADIANS; // source line 166
            this.golemHeadFez.xRot = headPitch / TO_RADIANS; // source line 167
        }

        // Source lines 170-183: every other head part copies the fez's rotation verbatim.
        this.golemHeadGlasses.yRot = this.golemHeadFez.yRot;
        this.golemHeadGlasses.xRot = this.golemHeadFez.xRot;
        this.golemHeadJar.yRot = this.golemHeadFez.yRot;
        this.golemHeadJar.xRot = this.golemHeadFez.xRot;
        this.golemHeadBrain.yRot = this.golemHeadFez.yRot;
        this.golemHeadBrain.xRot = this.golemHeadFez.xRot;
        this.golemEvilHead.yRot = this.golemHeadFez.yRot;
        this.golemEvilHead.xRot = this.golemHeadFez.xRot;
        this.golemVisor.yRot = this.golemHeadFez.yRot;
        this.golemVisor.xRot = this.golemHeadFez.xRot;
        this.golemHeadHat.yRot = this.golemHeadFez.yRot;
        this.golemHeadHat.xRot = this.golemHeadFez.xRot;
        this.golemHeadHatRim.yRot = this.golemHeadFez.yRot;
        this.golemHeadHatRim.xRot = this.golemHeadFez.xRot;

        // Draw selection (func_78088_a lines 87-150): the visible flags ARE the original's
        // per-decoration-character draw checks; renderToBuffer draws every part.
        final String deco = golem.getDecoration();
        this.golemDartgun.visible = hasDeco(deco, "R"); // line 88
        this.golemHeadFez.visible = hasDeco(deco, "F"); // line 93
        this.golemHeadHat.visible = hasDeco(deco, "H"); // line 101
        this.golemHeadHatRim.visible = hasDeco(deco, "H"); // line 101, drawn with the hat at line 107
        this.golemBowtie.visible = hasDeco(deco, "B"); // line 111
        this.golemPlate.visible = hasDeco(deco, "P"); // line 118
        this.golemPlateLeft.visible = hasDeco(deco, "P"); // line 118
        this.golemPlateRight.visible = hasDeco(deco, "P"); // line 118
        this.golemHeadGlasses.visible = hasDeco(deco, "G"); // line 124
        this.golemVisor.visible = hasDeco(deco, "V"); // line 128
        this.golemMace.visible = hasDeco(deco, "M"); // line 132
        final boolean advanced = golem.isAdvanced(); // en.advanced, lines 136 and 94/102
        this.golemHeadBrain.visible = advanced; // line 136-137
        this.golemHeadJar.visible = advanced; // line 136-141
        this.golemEvilHead.visible = advanced && golem.getCore() >= 0; // lines 136 and 144
        this.advancedLift = advanced;
    }

    /**
     * Dartgun/mace animation (source {@code func_78086_a}, lines 186-209). The source's
     * par2 (limbSwing, MCP field_70754_ba) and par3 (limbSwingAmount, MCP field_70721_aZ)
     * are not parameters of 1.21.1's {@code prepareMobModel}, so they come from
     * {@code golem.walkAnimation.position()}/{@code speed()}; {@code ageInTicks} is the
     * source's par4. The carried-stack test uses the port's {@code getCarried()} for the
     * original's {@code getCarriedForDisplay()} (line 193).
     */
    @Override
    public void prepareMobModel(final EntityGolemBase golem, final float limbSwingAmount,
            final float ageInTicks, final float netHeadYaw) {
        final int actionTimer = golem.getActionTimer(); // source line 188
        final float limbSwing = golem.walkAnimation.position(); // source par2 (field_70754_ba)
        final float swingAmount = golem.walkAnimation.speed(); // source par3 (field_70721_aZ)

        if (actionTimer > 0) {
            // Source lines 190-191: attack swing, wave(actionTimer - ageInTicks, 10.0F)
            final float swing = -2.0F + 1.5F * wave(actionTimer - ageInTicks, 10.0F);
            this.golemDartgun.xRot = swing;
            this.golemMace.xRot = swing;
        } else if (golem.getLeftArm() <= 0 && golem.getRightArm() <= 0) {
            if (golem.getCarried() != null) {
                // Source lines 194-195: carrying something, arms pinned back
                this.golemDartgun.xRot = -1.0F;
                this.golemMace.xRot = -1.0F;
            } else {
                // Source lines 197-198: walk swing, wave(limbSwing, 13.0F) * limbSwingAmount
                this.golemDartgun.xRot = (-0.2F - 1.5F * wave(limbSwing, 13.0F)) * swingAmount;
                this.golemMace.xRot = (-0.2F + 1.5F * wave(limbSwing, 13.0F)) * swingAmount;
            }
        } else {
            // Source lines 201-207: per-arm placed-block timers drive the two tools separately
            if (golem.getLeftArm() > 0) {
                this.golemDartgun.xRot = -2.0F + 1.5F * wave(golem.getLeftArm() - ageInTicks, 10.0F);
            }
            if (golem.getRightArm() > 0) {
                this.golemMace.xRot = -2.0F + 1.5F * wave(golem.getRightArm() - ageInTicks, 10.0F);
            }
        }
    }

    /**
     * Draws every part under the 0.4 model scale (the original's
     * {@code GL11.glScaled(0.4, 0.4, 0.4)} at source line 86); the {@code visible} flags
     * set in {@link #setupAnim} ARE the original's per-decoration draw selection, and
     * {@code setupAnim} runs immediately before every {@code renderToBuffer} call.
     */
    @Override
    public void renderToBuffer(final PoseStack poseStack, final VertexConsumer vertexConsumer,
            final int packedLight, final int packedOverlay, final int color) {
        poseStack.pushPose();
        poseStack.scale(0.4F, 0.4F, 0.4F); // source line 86, GL11.glScaled(0.4, 0.4, 0.4)

        this.golemDartgun.render(poseStack, vertexConsumer, packedLight, packedOverlay, color); // line 89

        // Fez branch, lines 92-99: the advanced golems get the GL11.glTranslatef(0, -0.01, 0)
        // of line 95; in 1.21.1 a per-part GL translate is a pushPose/translate/popPose wrap.
        if (this.advancedLift) {
            poseStack.pushPose();
            poseStack.translate(0.0F, -0.01F, 0.0F); // source line 95
            this.golemHeadFez.render(poseStack, vertexConsumer, packedLight, packedOverlay, color); // line 98
            poseStack.popPose();
        } else {
            this.golemHeadFez.render(poseStack, vertexConsumer, packedLight, packedOverlay, color); // line 98
        }

        // Hat branch, lines 101-108: the same advanced-only lift (line 103) covers the hat
        // and its rim, both drawn inside it (lines 106-107).
        if (this.advancedLift) {
            poseStack.pushPose();
            poseStack.translate(0.0F, -0.01F, 0.0F); // source line 103
            this.golemHeadHat.render(poseStack, vertexConsumer, packedLight, packedOverlay, color); // line 106
            this.golemHeadHatRim.render(poseStack, vertexConsumer, packedLight, packedOverlay, color); // line 107
            poseStack.popPose();
        } else {
            this.golemHeadHat.render(poseStack, vertexConsumer, packedLight, packedOverlay, color); // line 106
            this.golemHeadHatRim.render(poseStack, vertexConsumer, packedLight, packedOverlay, color); // line 107
        }

        // Bowtie (line 114) — the GL_BLEND of lines 112/115 is a whole-layer render type in
        // 1.21.1, not a per-part GL state change (documented delta 1).
        this.golemBowtie.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);

        // Source lines 119-121
        this.golemPlate.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.golemPlateLeft.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.golemPlateRight.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);

        this.golemHeadGlasses.render(poseStack, vertexConsumer, packedLight, packedOverlay, color); // line 125
        this.golemVisor.render(poseStack, vertexConsumer, packedLight, packedOverlay, color); // line 129
        this.golemMace.render(poseStack, vertexConsumer, packedLight, packedOverlay, color); // line 133

        // Source lines 136-149: the brain, jar and evil head are advanced-golem-only.
        this.golemHeadBrain.render(poseStack, vertexConsumer, packedLight, packedOverlay, color); // line 137
        // Jar (line 141) — the GL_BLEND of lines 139-142 is the same render-type delta (delta 1).
        this.golemHeadJar.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        if (this.golemEvilHead.visible) {
            // Source lines 144-148: core >= 0 (already encoded in the visible flag) draws
            // the evil head under GL11.glScaled(1.01, 1.0, 1.01) — a poseStack.scale wrap.
            poseStack.pushPose();
            poseStack.scale(1.01F, 1.0F, 1.01F); // source line 146
            this.golemEvilHead.render(poseStack, vertexConsumer, packedLight, packedOverlay, color); // line 147
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    /**
     * The original's oscillator {@code func_78172_a} (source lines 211-213), public so a
     * unit test can pin it.
     *
     * @param value the driving value (timer, age, or limb swing)
     * @param period the wave period in the same units
     * @return the triangle-wave oscillation in [-1, 1]
     */
    public static float wave(final float value, final float period) {
        return (Math.abs(value % period - period * 0.5F) - period * 0.25F) / (period * 0.25F);
    }

    /** The original's {@code deco != null && deco.contains(tag)} test (e.g. source line 88). */
    private static boolean hasDeco(final String deco, final String tag) {
        return deco != null && deco.contains(tag);
    }
}
