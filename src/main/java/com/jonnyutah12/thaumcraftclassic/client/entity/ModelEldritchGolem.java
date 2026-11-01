package com.jonnyutah12.thaumcraftclassic.client.entity;

import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityEldritchGolem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Eldritch Golem's model (card {@code TC-M6-08-s-c2}): a 1.7.10 → 1.21.1
 * transcription of {@code thaumcraft.client.renderers.models.entities.ModelEldritchGolem}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/client/renderers/models/entities/ModelEldritchGolem.java},
 * 328 lines). Geometry is {@code :48-243} (the 36 {@code ModelRenderer}s and
 * their parent links), rotation angles {@code :291-305}, living animation
 * {@code :307-317}, draw order {@code :245-289}.
 *
 * <p><b>MCP → 1.21.1 mapping</b> (every number below comes from the source
 * line cited, nothing is invented):</p>
 * <pre>
 * new ModelRenderer(this, u, v) + func_78789_a(x,y,z,w,h,d)
 *        → CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d, CubeDeformation.NONE)
 * func_78793_a(x, y, z) (setRotationPoint) → PartPose offset (this model passes no
 *                                        y-offset parameter, so offsets are verbatim)
 * setRotation(model, x, y, z) (:323-327)  → PartPose xRot/yRot/zRot, folded into
 *                                        PartPose.offsetAndRotation
 * field_78795_f / g / h                    → xRot / yRot / zRot
 * field_78809_i = true                     → CubeListBuilder.mirror() before that box
 * func_78792_a(child) (:235-242)           → PartDefinition.addOrReplaceChild on the parent
 * func_78787_b(128, 64) (setTextureSize)   → LayerDefinition.create(mesh, 128, 64)
 * func_78087_a (:291-305)                  → {@link #setupAnim}
 * func_78088_a (:245-289)                  → {@link #setupAnim} (swing math) + {@link #renderToBuffer} (draws)
 * func_78086_a (:307-317)                  → {@link #prepareMobModel}
 * MathHelper.func_76134_b                  → {@link Mth#cos(float)}
 * </pre>
 *
 * <p><b>Texture size (documented delta)</b>: the source is {@code 128x64}
 * ({@code :49-50}) and calls {@code func_78787_b(128, 64)} on every part
 * <i>except</i> {@code Frontcloth0} ({@code :114}), {@code Frontcloth1}
 * ({@code :119}) and {@code Frontcloth2} ({@code :124}), which call
 * {@code func_78787_b(114, 64)} with tex offsets {@code (114, 52)},
 * {@code (114, 39)} and {@code (114, 47)}. 1.21.1 carries one texture size per
 * {@link LayerDefinition}, so the port bakes {@code 128x64} for all 36 parts;
 * the UV scale of those three parts shifts by {@code 128/114} relative to the
 * original, and their boxes keep the source's local-space position untouched.</p>
 *
 * <p><b>Moved code (documented delta)</b>: the cloak / front-cloth swing math of
 * {@code func_78088_a} ({@code :248-255}) ran per frame in the original's
 * <i>render</i> method from its render parameters ({@code f} = limbSwing,
 * {@code f1} = limbSwingAmount). 1.21.1 has no per-frame model render hook, so
 * that block lives in {@link #setupAnim} with the same parameters, in the same
 * order (after the {@code func_78087_a} body, which the original also ran
 * first at {@code :247}).</p>
 *
 * <p><b>Arm swing (documented delta)</b>: {@code func_78086_a} ({@code :307-317})
 * reads {@code golem.getAttackTimer()} — an int tick counter the original's AI
 * sets — and its swinging branch ({@code :310-312})
 * {@code -2.0F + 1.5F * doAbs(i - par3, 10.0F)} is <i>not reproducible</i> in
 * 1.21.1, which has no such counter. The port ships the source's ELSE branch
 * ({@code :313-316}, the walking arm swing) as the original figure and blends
 * toward the attack pose with 1.21.1's built-in
 * {@code LivingEntity#getAttackAnim(float)} — the normalised swing progress
 * that replaces {@code getAttackTimer()}. The walking figure is exact; the
 * attack pose reached at {@code attackAnim == 1} is the documented
 * approximation, and it degrades to the exact original figure whenever
 * {@code attackAnim == 0}. {@code doAbs} ({@code :319-321}) is kept as the
 * public static {@link #wave(float, float)} so the pinned figure stays
 * unit-testable. 1.21.1's {@code prepareMobModel} has no {@code limbSwing}
 * parameter, so the source's {@code par1} ({@code :314-315}) is supplied by
 * {@code ageInTicks}, the phase 1.21.1 hands to that callback.</p>
 *
 * <p><b>Draws</b>: every part is drawn, in the source's draw order
 * ({@code :256-288}). The eight shoulder parts are children of {@code ArmL} /
 * {@code ArmR} ({@code :235-242}) and are drawn by their parent, so they are
 * deliberately <i>not</i> drawn again here. {@code Frontcloth1} is drawn twice
 * in the original ({@code :256} and {@code :275}); the port draws it once.
 * There is no scale in this model (unlike {@code ModelGolem}'s 0.4) — the
 * golem's {@code 2.15F} scale lives in the renderer, so none is applied here.</p>
 *
 * <p><b>Not ported (owning card {@code t_03c5d088}, the behaviour card for
 * {@link EntityEldritchGolem}; that skeleton carries only WIDTH / HEIGHT /
 * TRACKING_RANGE / UPDATE_INTERVAL / attributes, so the state these branches
 * read does not exist in the port and no stand-in is invented)</b>:</p>
 * <ul>
 * <li>the spawn-timer head drop, {@code :293-295}
 * ({@code if (entity instanceof EntityEldritchGolem && ((EntityEldritchGolem)entity).getSpawnTimer() > 0) {
 * this.Head.field_78796_g = 0.0F; this.Head.field_78795_f = ((EntityEldritchGolem)entity).getSpawnTimer() / 2 / (180.0F / (float)Math.PI);}})
 * — {@code setupAnim} ships the source's ELSE branch ({@code :296-301}) verbatim;</li>
 * <li>the head / headless swap, {@code :264-268}
 * ({@code if (entity instanceof EntityEldritchGolem && !((EntityEldritchGolem)entity).isHeadless()) {
 * this.Head.func_78785_a(f5);} else {this.Head2.func_78785_a(f5);}}) — {@code Head} is
 * drawn and {@code Head2} is kept in the layer with {@code visible = false} set
 * in {@code setupAnim}, the safe default for the not-ported branch;</li>
 * <li>the {@code getAttackTimer()} arm swing, {@code :309-312} — see the arm
 * swing note above.</li>
 * </ul>
 *
 * <p>The original's whole-render additive blend
 * ({@code RenderEldritchGolem.java:35-41}) is applied by the renderer's
 * {@link RenderType}, not by this model; the constructor hands
 * {@code SKIN_TEXTURE} (from {@code RenderEldritchGolem.java:18}) to
 * {@code RenderType::entityCutoutNoCull} so the model renders in its own
 * render type. The texture itself is referenced by path only (D-004).</p>
 */
public final class ModelEldritchGolem extends EntityModel<EntityEldritchGolem> {

    /** Layer id for the entity renderer's {@code EntityModelSet}. */
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thaumcraft", "entity/eldritch_golem"), "main");

    /** {@code new ResourceLocation("thaumcraft", "textures/models/eldritch_golem.png")} ({@code RenderEldritchGolem.java:18}). */
    public static final ResourceLocation SKIN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/eldritch_golem.png");

    // ModelEldritchGolem.java:11-46 — the 36 ModelRenderer fields.
    private final ModelPart Frontcloth1;
    private final ModelPart CollarL;
    private final ModelPart Cloak1;
    private final ModelPart CloakCL;
    private final ModelPart CloakCR;
    private final ModelPart Cloak3;
    private final ModelPart Cloak2;
    private final ModelPart Head;
    private final ModelPart Head2;
    private final ModelPart Frontcloth0;
    private final ModelPart CollarB;
    private final ModelPart Torso;
    private final ModelPart CollarR;
    private final ModelPart CollarF;
    private final ModelPart CollarBlack;
    private final ModelPart ShoulderR1;
    private final ModelPart ArmL;
    private final ModelPart ShoulderR;
    private final ModelPart ShoulderR2;
    private final ModelPart ShoulderR0;
    private final ModelPart ArmR;
    private final ModelPart ShoulderL1;
    private final ModelPart ShoulderL0;
    private final ModelPart ShoulderL;
    private final ModelPart ShoulderL2;
    private final ModelPart BackpanelR1;
    private final ModelPart WaistR1;
    private final ModelPart WaistR2;
    private final ModelPart WaistR3;
    private final ModelPart LegR;
    private final ModelPart WaistL1;
    private final ModelPart WaistL2;
    private final ModelPart WaistL3;
    private final ModelPart Frontcloth2;
    private final ModelPart BackpanelL1;
    private final ModelPart LegL;

    public ModelEldritchGolem() {
        // The original's additive-blend render type is the renderer's job; the
        // model itself renders on its own cutout render type.
        super(texture -> RenderType.entityCutoutNoCull(SKIN_TEXTURE));
        final ModelPart root = layer().bakeRoot();
        this.Cloak1 = root.getChild("Cloak1");
        this.Cloak3 = root.getChild("Cloak3");
        this.Cloak2 = root.getChild("Cloak2");
        this.CloakCL = root.getChild("CloakCL");
        this.CloakCR = root.getChild("CloakCR");
        this.Head = root.getChild("Head");
        this.Head2 = root.getChild("Head2");
        this.CollarL = root.getChild("CollarL");
        this.CollarR = root.getChild("CollarR");
        this.CollarB = root.getChild("CollarB");
        this.CollarF = root.getChild("CollarF");
        this.CollarBlack = root.getChild("CollarBlack");
        this.Frontcloth0 = root.getChild("Frontcloth0");
        this.Frontcloth1 = root.getChild("Frontcloth1");
        this.Frontcloth2 = root.getChild("Frontcloth2");
        this.Torso = root.getChild("Torso");
        this.ArmR = root.getChild("ArmR");
        this.ShoulderR = this.ArmR.getChild("ShoulderR");
        this.ShoulderR0 = this.ArmR.getChild("ShoulderR0");
        this.ShoulderR1 = this.ArmR.getChild("ShoulderR1");
        this.ShoulderR2 = this.ArmR.getChild("ShoulderR2");
        this.ArmL = root.getChild("ArmL");
        this.ShoulderL = this.ArmL.getChild("ShoulderL");
        this.ShoulderL0 = this.ArmL.getChild("ShoulderL0");
        this.ShoulderL1 = this.ArmL.getChild("ShoulderL1");
        this.ShoulderL2 = this.ArmL.getChild("ShoulderL2");
        this.BackpanelR1 = root.getChild("BackpanelR1");
        this.WaistR1 = root.getChild("WaistR1");
        this.WaistR2 = root.getChild("WaistR2");
        this.WaistR3 = root.getChild("WaistR3");
        this.LegR = root.getChild("LegR");
        this.WaistL1 = root.getChild("WaistL1");
        this.WaistL2 = root.getChild("WaistL2");
        this.WaistL3 = root.getChild("WaistL3");
        this.BackpanelL1 = root.getChild("BackpanelL1");
        this.LegL = root.getChild("LegL");
    }

    /**
     * The baked geometry: 36 parts in the source's construction order
     * ({@code :48-243}), 1.21.1's single {@code 128x64} texture size
     * ({@code :49-50}, see the class javadoc for the three parts the source
     * set to {@code 114x64}).
     */
    public static LayerDefinition layer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();

        // ModelEldritchGolem.java:51-55
        root.addOrReplaceChild("Cloak1", CubeListBuilder.create().texOffs(0, 47)
                .addBox(-5.0F, 1.5F, 4.0F, 10, 12, 1, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.1396263F, 0.0F, 0.0F));
        // ModelEldritchGolem.java:56-60
        root.addOrReplaceChild("Cloak3", CubeListBuilder.create().texOffs(0, 37)
                .addBox(-5.0F, 17.5F, -0.8F, 10, 4, 1, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.4465716F, 0.0F, 0.0F));
        // ModelEldritchGolem.java:61-65
        root.addOrReplaceChild("Cloak2", CubeListBuilder.create().texOffs(0, 59)
                .addBox(-5.0F, 13.5F, 1.7F, 10, 4, 1, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.3069452F, 0.0F, 0.0F));
        // ModelEldritchGolem.java:66-70
        root.addOrReplaceChild("CloakCL", CubeListBuilder.create().texOffs(0, 43)
                .addBox(3.0F, 0.5F, 2.0F, 2, 1, 3, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.1396263F, 0.0F, 0.0F));
        // ModelEldritchGolem.java:71-75
        root.addOrReplaceChild("CloakCR", CubeListBuilder.create().texOffs(0, 43)
                .addBox(-5.0F, 0.5F, 2.0F, 2, 1, 3, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.1396263F, 0.0F, 0.0F));
        // ModelEldritchGolem.java:76-80
        root.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(47, 12)
                .addBox(-3.5F, -6.0F, -2.5F, 7, 7, 5, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 4.5F, -3.8F, -0.1047198F, 0.0F, 0.0F));
        // ModelEldritchGolem.java:81-85
        root.addOrReplaceChild("Head2", CubeListBuilder.create().texOffs(26, 16)
                .addBox(-2.0F, -2.0F, -2.0F, 4, 4, 4, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, -5.0F, -0.1047198F, 0.0F, 0.0F));
        // ModelEldritchGolem.java:86-90
        root.addOrReplaceChild("CollarL", CubeListBuilder.create().texOffs(75, 50)
                .addBox(3.5F, -0.5F, -7.0F, 1, 4, 10, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.837758F, 0.0F, 0.0F));
        // ModelEldritchGolem.java:91-95
        root.addOrReplaceChild("CollarR", CubeListBuilder.create().texOffs(67, 50)
                .addBox(-4.5F, -0.5F, -7.0F, 1, 4, 10, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.837758F, 0.0F, 0.0F));
        // ModelEldritchGolem.java:96-100
        root.addOrReplaceChild("CollarB", CubeListBuilder.create().texOffs(77, 59)
                .addBox(-3.5F, -0.5F, 2.0F, 7, 4, 1, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.837758F, 0.0F, 0.0F));
        // ModelEldritchGolem.java:101-105
        root.addOrReplaceChild("CollarF", CubeListBuilder.create().texOffs(77, 59)
                .addBox(-3.5F, -0.5F, -7.0F, 7, 4, 1, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.837758F, 0.0F, 0.0F));
        // ModelEldritchGolem.java:106-110
        root.addOrReplaceChild("CollarBlack", CubeListBuilder.create().texOffs(22, 0)
                .addBox(-3.5F, 0.0F, -6.0F, 7, 1, 8, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.837758F, 0.0F, 0.0F));
        // ModelEldritchGolem.java:111-115 (source texture size 114x64 at :114 — see class javadoc)
        root.addOrReplaceChild("Frontcloth0", CubeListBuilder.create().texOffs(114, 52)
                .addBox(-3.0F, 3.2F, -3.5F, 6, 10, 1, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.1745329F, 0.0F, 0.0F));
        // ModelEldritchGolem.java:116-120 (source texture size 114x64 at :119 — see class javadoc)
        root.addOrReplaceChild("Frontcloth1", CubeListBuilder.create().texOffs(114, 39)
                .addBox(-1.0F, 1.5F, -3.5F, 6, 6, 1, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-2.0F, 12.0F, 0.0F, -0.1047198F, 0.0F, 0.0F));
        // ModelEldritchGolem.java:121-125 (source texture size 114x64 at :124 — see class javadoc)
        root.addOrReplaceChild("Frontcloth2", CubeListBuilder.create().texOffs(114, 47)
                .addBox(-1.0F, 8.5F, -1.5F, 6, 3, 1, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-2.0F, 11.0F, 0.0F, -0.3316126F, 0.0F, 0.0F));
        // ModelEldritchGolem.java:126-131 (mirror flag field_78809_i = true at :130)
        root.addOrReplaceChild("Torso", CubeListBuilder.create().texOffs(34, 45).mirror()
                .addBox(-5.0F, 2.5F, -3.0F, 10, 10, 6, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.1745329F, 0.0F, 0.0F));

        // ModelEldritchGolem.java:132-136; its four shoulder children attach at :239-242
        final PartDefinition armR = root.addOrReplaceChild("ArmR", CubeListBuilder.create().texOffs(78, 32)
                .addBox(-3.5F, 1.5F, -2.0F, 4, 13, 5, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-5.0F, 3.0F, -2.0F, 0.0F, 0.0F, 0.1047198F));
        // ModelEldritchGolem.java:141-144 (no func_78793_a, so the offset stays 0,0,0)
        armR.addOrReplaceChild("ShoulderR", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.3F, -1.0F, -3.0F, 4, 5, 7, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.186824F));
        // ModelEldritchGolem.java:149-152 (no func_78793_a, so the offset stays 0,0,0)
        armR.addOrReplaceChild("ShoulderR0", CubeListBuilder.create().texOffs(56, 31)
                .addBox(-4.5F, -1.5F, -2.5F, 5, 6, 6, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        // ModelEldritchGolem.java:137-140 (no func_78793_a, so the offset stays 0,0,0)
        armR.addOrReplaceChild("ShoulderR1", CubeListBuilder.create().texOffs(0, 23)
                .addBox(-3.3F, 4.0F, -2.5F, 1, 2, 6, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.186824F));
        // ModelEldritchGolem.java:145-148 (no func_78793_a, so the offset stays 0,0,0)
        armR.addOrReplaceChild("ShoulderR2", CubeListBuilder.create().texOffs(0, 12)
                .addBox(-2.3F, 4.0F, -3.0F, 2, 3, 7, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.186824F));

        // ModelEldritchGolem.java:153-158 (mirror flag field_78809_i = true at :154);
        // its four shoulder children attach at :235-238
        final PartDefinition armL = root.addOrReplaceChild("ArmL", CubeListBuilder.create().texOffs(78, 32).mirror()
                .addBox(-0.5F, 1.5F, -2.0F, 4, 13, 5, CubeDeformation.NONE),
                PartPose.offsetAndRotation(5.0F, 3.0F, -2.0F, 0.0F, 0.0F, -0.1047198F));
        // ModelEldritchGolem.java:169-173 (mirror at :170; no func_78793_a, so the offset stays 0,0,0)
        armL.addOrReplaceChild("ShoulderL", CubeListBuilder.create().texOffs(0, 0).mirror()
                .addBox(0.3F, -1.0F, -3.0F, 4, 5, 7, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.186824F));
        // ModelEldritchGolem.java:164-168 (mirror at :165; no func_78793_a, so the offset stays 0,0,0)
        armL.addOrReplaceChild("ShoulderL0", CubeListBuilder.create().texOffs(56, 31).mirror()
                .addBox(-0.5F, -1.5F, -2.5F, 5, 6, 6, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        // ModelEldritchGolem.java:159-163 (mirror at :160; no func_78793_a, so the offset stays 0,0,0)
        armL.addOrReplaceChild("ShoulderL1", CubeListBuilder.create().texOffs(0, 23).mirror()
                .addBox(2.3F, 4.0F, -2.5F, 1, 2, 6, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.186824F));
        // ModelEldritchGolem.java:174-178 (mirror at :175; no func_78793_a, so the offset stays 0,0,0)
        armL.addOrReplaceChild("ShoulderL2", CubeListBuilder.create().texOffs(0, 12).mirror()
                .addBox(0.3F, 4.0F, -3.0F, 2, 3, 7, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.186824F));

        // ModelEldritchGolem.java:179-183
        root.addOrReplaceChild("BackpanelR1", CubeListBuilder.create().texOffs(96, 7)
                .addBox(0.0F, 2.5F, -2.5F, 2, 2, 5, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-2.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.1396263F));
        // ModelEldritchGolem.java:184-188
        root.addOrReplaceChild("WaistR1", CubeListBuilder.create().texOffs(96, 14)
                .addBox(-3.0F, -0.5F, -2.5F, 5, 3, 5, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-2.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.1396263F));
        // ModelEldritchGolem.java:189-193
        root.addOrReplaceChild("WaistR2", CubeListBuilder.create().texOffs(116, 13)
                .addBox(-3.0F, 2.5F, -2.5F, 1, 4, 5, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-2.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.1396263F));
        // ModelEldritchGolem.java:194-199 (mirror flag field_78809_i = true at :195)
        root.addOrReplaceChild("WaistR3", CubeListBuilder.create().texOffs(114, 5).mirror()
                .addBox(-2.0F, 2.5F, -2.5F, 2, 3, 5, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-2.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.1396263F));
        // ModelEldritchGolem.java:200-204
        root.addOrReplaceChild("LegR", CubeListBuilder.create().texOffs(79, 19)
                .addBox(-2.5F, 2.5F, -2.0F, 4, 9, 4, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-2.0F, 12.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        // ModelEldritchGolem.java:205-210 (mirror flag field_78809_i = true at :209)
        root.addOrReplaceChild("WaistL1", CubeListBuilder.create().texOffs(96, 14).mirror()
                .addBox(-2.0F, -0.5F, -2.5F, 5, 3, 5, CubeDeformation.NONE),
                PartPose.offsetAndRotation(2.0F, 12.0F, 0.0F, 0.0F, 0.0F, -0.1396263F));
        // ModelEldritchGolem.java:211-216 (mirror flag field_78809_i = true at :215)
        root.addOrReplaceChild("WaistL2", CubeListBuilder.create().texOffs(116, 13).mirror()
                .addBox(2.0F, 2.5F, -2.5F, 1, 4, 5, CubeDeformation.NONE),
                PartPose.offsetAndRotation(2.0F, 12.0F, 0.0F, 0.0F, 0.0F, -0.1396263F));
        // ModelEldritchGolem.java:217-222 (mirror flag field_78809_i = true at :221)
        root.addOrReplaceChild("WaistL3", CubeListBuilder.create().texOffs(114, 5).mirror()
                .addBox(0.0F, 2.5F, -2.5F, 2, 3, 5, CubeDeformation.NONE),
                PartPose.offsetAndRotation(2.0F, 12.0F, 0.0F, 0.0F, 0.0F, -0.1396263F));
        // ModelEldritchGolem.java:223-228 (mirror flag field_78809_i = true at :227)
        root.addOrReplaceChild("BackpanelL1", CubeListBuilder.create().texOffs(96, 7).mirror()
                .addBox(-2.0F, 2.5F, -2.5F, 2, 2, 5, CubeDeformation.NONE),
                PartPose.offsetAndRotation(2.0F, 12.0F, 0.0F, 0.0F, 0.0F, -0.1396263F));
        // ModelEldritchGolem.java:229-234 (mirror flag field_78809_i = true at :233)
        root.addOrReplaceChild("LegL", CubeListBuilder.create().texOffs(79, 19).mirror()
                .addBox(-1.5F, 2.5F, -2.0F, 4, 9, 4, CubeDeformation.NONE),
                PartPose.offsetAndRotation(2.0F, 12.5F, 0.0F, 0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 64);
    }

    /**
     * {@code func_78087_a} ({@code :291-305}) plus the swing block of
     * {@code func_78088_a} ({@code :248-255}); see the class javadoc for the
     * spawn-timer branch ({@code :293-295}) that is not ported.
     */
    @Override
    public void setupAnim(final EntityEldritchGolem golem, final float limbSwing, final float limbSwingAmount,
            final float ageInTicks, final float netHeadYaw, final float headPitch) {
        // The headless state is not ported, so the drawn head is Head and Head2
        // is the not-ported branch's safe default (card t_03c5d088).
        this.Head.visible = true;
        this.Head2.visible = false;

        // ModelEldritchGolem.java:296-300 (the source's else branch, verbatim)
        this.Head.yRot = netHeadYaw / 4.0F / (180.0F / Mth.PI);
        this.Head.xRot = headPitch / 2.0F / (180.0F / Mth.PI);
        this.Head2.yRot = netHeadYaw / (180.0F / Mth.PI);
        this.Head2.xRot = headPitch / (180.0F / Mth.PI);

        // ModelEldritchGolem.java:303-304
        this.LegR.xRot = Mth.cos(limbSwing * 0.4662F) * 1.4F * limbSwingAmount;
        this.LegL.xRot = Mth.cos(limbSwing * 0.4662F + Mth.PI) * 1.4F * limbSwingAmount;

        // ModelEldritchGolem.java:248-255 — moved out of the render method; see class javadoc.
        final float a = Mth.cos(limbSwing * 0.44F) * 1.4F * limbSwingAmount;
        final float b = Mth.cos(limbSwing * 0.44F + Mth.PI) * 1.4F * limbSwingAmount;
        final float c = Math.min(a, b);
        this.Frontcloth1.xRot = c - 0.1047198F;
        this.Frontcloth2.xRot = c - 0.3316126F;
        this.Cloak1.xRot = -c / 3.0F + 0.1396263F;
        this.Cloak2.xRot = -c / 3.0F + 0.3069452F;
        this.Cloak3.xRot = -c / 3.0F + 0.4465716F;
    }

    /**
     * {@code func_78086_a} ({@code :307-317}). The walking figure
     * ({@code :313-316}) is the original; the attack pose blended in by
     * {@code getAttackAnim} replaces the un-reproducible
     * {@code getAttackTimer()} branch ({@code :310-312}, card t_03c5d088) and
     * is the documented approximation.
     */
    @Override
    public void prepareMobModel(final EntityEldritchGolem golem, final float limbSwingAmount,
            final float ageInTicks, final float netHeadYaw) {
        final float attackAnim = golem.getAttackAnim(0.0F);
        // ModelEldritchGolem.java:314-315 — the original walking figure.
        final float walkR = Mth.cos(ageInTicks * 0.4F + Mth.PI) * 2.0F * limbSwingAmount * 0.5F;
        final float walkL = Mth.cos(ageInTicks * 0.4F) * 2.0F * limbSwingAmount * 0.5F;
        // The original's swing pose shape (:311-312) driven by 1.21.1's
        // normalised attack progress instead of the int tick counter.
        final float swing = -2.0F + 1.5F * wave(attackAnim * 10.0F, 10.0F);
        this.ArmR.xRot = walkR + (swing - walkR) * attackAnim;
        this.ArmL.xRot = walkL + (swing - walkL) * attackAnim;
    }

    /**
     * {@code doAbs} ({@code :319-321}), kept public and static so the pinned
     * arm-swing figure is unit-testable.
     *
     * @param value the source's {@code p_78172_1_}
     * @param period the source's {@code p_78172_2_}
     * @return the source's triangle wave in {@code [-1, 1]}
     */
    public static float wave(final float value, final float period) {
        return (Math.abs(value % period - period * 0.5F) - period * 0.25F) / (period * 0.25F);
    }

    /**
     * The draw calls of {@code func_78088_a} ({@code :256-288}), in source
     * order. The eight shoulders are children of {@code ArmL} / {@code ArmR}
     * ({@code :235-242}) and render with them, so they are not drawn here;
     * {@code Frontcloth1} draws once, not twice as at {@code :256} and
     * {@code :275}; {@code Head2} is drawn only as the not-ported headless
     * branch's placeholder and is hidden by {@code setupAnim}.
     */
    @Override
    public void renderToBuffer(final PoseStack poseStack, final VertexConsumer buffer, final int packedLight,
            final int packedOverlay, final int color) {
        this.Frontcloth1.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.CollarL.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.CollarBlack.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.Cloak1.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.CloakCL.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.CloakCR.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.Cloak3.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.Cloak2.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.Head.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.Head2.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.Frontcloth0.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.CollarB.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.Torso.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.CollarR.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.CollarF.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.ArmL.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.ArmR.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.BackpanelR1.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.WaistR1.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.WaistR2.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.WaistR3.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.LegR.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.WaistL1.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.WaistL2.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.WaistL3.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.Frontcloth2.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.BackpanelL1.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.LegL.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
