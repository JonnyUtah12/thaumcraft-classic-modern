package com.jonnyutah12.thaumcraftclassic.client.entity;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EnumGolemType;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogGolems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;

/**
 * The golem renderer — Thaumcraft 4.2.3.5
 * {@code thaumcraft.client.renderers.entity.RenderGolemBase}, transcribed from
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/renderers/entity/RenderGolemBase.java}
 * (291 lines). Card TC-M6-08-s-c2.
 *
 * <p><b>Material textures.</b> The eight {@code golem_*.png} sheets
 * ({@code RenderGolemBase.java:43-50}) and the material switch of
 * {@code func_110775_a} ({@code getEntityTexture}, lines 269-290) are ported
 * verbatim; {@link #textureFor(EnumGolemType)} is the switch, and its
 * {@code default} arm returns the player skin, exactly as the original returns
 * {@code AbstractClientPlayer.field_110314_b}.</p>
 *
 * <p><b>The three extra passes of {@code func_77032_a} (lines 68-134).</b>
 * 1.7.10 drives them through {@code RenderLiving.setRenderPasses}: pass 0 is the
 * skin pass (and it draws the core icon + the upgrade strip into it), pass 1 is
 * the decoration layer, pass 2 the damage layer. 1.21.1 has no
 * {@code setRenderPasses} seam, so the two model layers are 1.21.1
 * {@link RenderLayer}s (with the source's own gates: {@code deco.length() > 0 ||
 * advanced} for the decoration, {@code getHealthPercentage() < 1.0F} for the
 * damage) and the two icon passes run in {@link #renderExtraPasses} after
 * {@code super.render}, which is the same place in the frame the original draws
 * them (inside its skin pass).</p>
 *
 * <p><b>Documented deltas, all of them GL-state-to-render-type, none of them
 * geometry:</b></p>
 * <ol>
 *   <li>The decoration layer's {@code GL_BLEND} around the bowtie
 *       ({@code ModelGolemAccessories.java:112-115}) and the head jar
 *       ({@code :139-142}) is a per-part GL state change inside one model draw.
 *       1.21.1's render type is per-draw, so the whole decoration layer uses
 *       {@link RenderType#entityCutoutNoCull} — 1.7.10's alpha-tested default —
 *       and the two blended sub-passes are not reproducible. Recorded as
 *       BEHAVIORAL_DEVIATIONS BD-23.</li>
 *   <li>The upgrade strip's {@code glEnable(GL_BLEND) + glBlendFunc(SRC_ALPHA,
 *       ONE_MINUS_SRC_ALPHA)} (lines 91-92) becomes the layer's
 *       {@link RenderType#entityTranslucent}.</li>
 *   <li>The core icon quad and the upgrade quads sample
 *       {@code itemGolemCore.func_77617_a(core)} /
 *       {@code itemGolemUpgrade.func_77617_a(upgrade)} — the per-metadata item
 *       icon. 1.21.1 has no per-metadata icon API; the icon of an item is the
 *       sprite of its item model, and the golem core / upgrade item model JSONs
 *       are card t_e9d23e1c. The port samples the item model's sprite, so the
 *       quad geometry, UV transposition and blend are exact and the sprite
 *       content lands with that card.</li>
 *   <li>The carried-stack branch of {@code renderCarriedItems} (lines 169-227)
 *       goes through 1.7.10's {@code IItemRenderer}/{@code ItemBlock}/
 *       {@code RenderBlocks.renderBlockAsItem}/{@code getIcon}-renderPass
 *       machinery. 1.21.1 renders every stack — block items included — through
 *       {@code ItemRenderer.renderStatic}, so the branch collapses to one call
 *       with the source's own transform stack kept verbatim.</li>
 *   <li>Core 5 (the bucket) draws the {@code textures/models/bucket.obj} mesh
 *       through {@code AdvancedModelLoader} (lines 61, 233-234). NeoForge 21.1
 *       has no {@code .obj} model loader and the asset is ARR-overlay-only
 *       (D-004), so the bucket mesh is NOT ported. Its fluid quad
 *       ({@code :240-245}) is ported, sampling the carried item's sprite
 *       instead of {@code FluidRegistry.getFluid(item).getIcon()}: 21.1 has no
 *       per-fluid item icon, and the golem's fluid-carry state belongs to
 *       t_03c5d088.</li>
 * </ol>
 *
 * <p><b>The fixed-pipeline GL calls, and where they went.</b> The reference
 * wraps its passes in legacy state: {@code glEnable} of {@code GL_BLEND} (3042)
 * with {@code glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)} (770, 771) for
 * the carried item and the bucket ({@code :139-140}, {@code :230-231}),
 * {@code glDisable} of {@code GL_LIGHTING} (2896) around the item quads
 * ({@code :141}, {@code :232}), {@code glEnable} of {@code GL_RESCALE_NORMAL}
 * (32826) at {@code :154} and {@code glDisable} of {@code GL_TEXTURE_2D} (3553)
 * at {@code :155}. 1.21.1 resolves all of that per {@code RenderType}, not per
 * call site: the item and entity render types this port draws through are
 * already blended, textured and lit, so each call is dropped with its state
 * recorded here — and {@code GL_RESCALE_NORMAL} has no twin at all
 * (normalisation is a property of the lit render types). The two places where a
 * state figure genuinely has no 1.21.1 equivalent — the decoration layer's
 * per-part blend and the 1/255 alpha test — are BD-23 and BD-26.</p>
 *
 * <p>Every constant in this file is quoted from the reference file with its line
 * number next to it. Textures are referenced by path only (D-004).</p>
 */
public class RenderGolemBase extends LivingEntityRenderer<EntityGolemBase, ModelGolem> {

    /** {@code RenderGolemBase.java:48}. */
    public static final ResourceLocation TEXTURE_STRAW = sheet("golem_straw.png");
    /** {@code RenderGolemBase.java:45}. */
    public static final ResourceLocation TEXTURE_WOOD = sheet("golem_wood.png");
    /** {@code RenderGolemBase.java:43}. */
    public static final ResourceLocation TEXTURE_CLAY = sheet("golem_clay.png");
    /** {@code RenderGolemBase.java:44}. */
    public static final ResourceLocation TEXTURE_STONE = sheet("golem_stone.png");
    /** {@code RenderGolemBase.java:47}. */
    public static final ResourceLocation TEXTURE_IRON = sheet("golem_iron.png");
    /** {@code RenderGolemBase.java:46}. */
    public static final ResourceLocation TEXTURE_TALLOW = sheet("golem_tallow.png");
    /** {@code RenderGolemBase.java:49}. */
    public static final ResourceLocation TEXTURE_FLESH = sheet("golem_flesh.png");
    /** {@code RenderGolemBase.java:50}. */
    public static final ResourceLocation TEXTURE_THAUMIUM = sheet("golem_thaumium.png");

    /** {@code UtilsFX.bindTexture("textures/models/golem_decoration.png")} at :120. */
    public static final ResourceLocation TEXTURE_DECORATION =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/golem_decoration.png");
    /** {@code UtilsFX.bindTexture("textures/models/golem_damage.png")} at :126. */
    public static final ResourceLocation TEXTURE_DAMAGE =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/golem_damage.png");
    /** {@code UtilsFX.bindTexture("textures/models/bucket.png")} at :233 (path only; the .obj mesh is not ported). */
    public static final ResourceLocation TEXTURE_BUCKET =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/bucket.png");

    /** The body layer of {@link ModelGolem}, the layer this renderer registers. */
    public static final net.minecraft.client.model.geom.ModelLayerLocation LAYER = ModelGolem.LAYER;

    // ---- the pinned figures of the reference, named so a test can hold them

    /** :75 {@code glScalef(0.175F, 0.175F, 0.175F)} — the core icon's scale. */
    public static final float CORE_ICON_SCALE = 0.175F;
    /** :89 {@code float f6 = 0.08F} — the spacing of the upgrade strip. */
    public static final float UPGRADE_SHIFT = 0.08F;
    /** :97 the strip's y. */
    public static final float UPGRADE_Y = -1.106F;
    /** :97 the strip's z. */
    public static final float UPGRADE_Z = 0.099F;
    /** :98 {@code glScalef(0.1F, 0.1F, 0.1F)}. */
    public static final float UPGRADE_SCALE = 0.1F;
    /** :141 the body-sway amplitude, in degrees. */
    public static final float SWAY_DEGREES = 6.5F;
    /** :141 the sway wave's period. */
    public static final float SWAY_PERIOD = 13.0F;
    /** :140 the sway wave's phase offset. */
    public static final float SWAY_PHASE_OFFSET = 6.0F;

    /** The {@code ModelGolemAccessories} copy of :60. */
    private final ModelGolemAccessories accessories = new ModelGolemAccessories();
    /** The {@code pass = 2} damage copy of :54-58. */
    private final ModelGolem damage;

    public RenderGolemBase(final EntityRendererProvider.Context context) {
        // super(modelBase, 0.25F) — the shadow radius of :53.
        super(context, new ModelGolem(ModelGolem.layer().bakeRoot()), 0.25F);
        this.damage = new ModelGolem(ModelGolem.layer().bakeRoot());
        this.damage.pass = 2; // :56
        // The decoration layer, gated exactly as :119 gates it, and the damage
        // layer, gated as :125 gates it.
        this.addLayer(new AccessoriesLayer(this, this.accessories));
        this.addLayer(new DamageLayer(this, this.damage));
    }

    /** {@code func_110775_a} / {@code getEntityTexture} — the material switch of :269-290. */
    @Override
    public ResourceLocation getTextureLocation(final EntityGolemBase golem) {
        return textureFor(golem.getMaterial());
    }

    /** The switch of :270-289 as a pure function, so a test can pin all nine arms. */
    public static ResourceLocation textureFor(final EnumGolemType material) {
        if (material == EnumGolemType.STRAW) {
            return TEXTURE_STRAW;
        }
        if (material == EnumGolemType.WOOD) {
            return TEXTURE_WOOD;
        }
        if (material == EnumGolemType.CLAY) {
            return TEXTURE_CLAY;
        }
        if (material == EnumGolemType.STONE) {
            return TEXTURE_STONE;
        }
        if (material == EnumGolemType.IRON) {
            return TEXTURE_IRON;
        }
        if (material == EnumGolemType.TALLOW) {
            return TEXTURE_TALLOW;
        }
        if (material == EnumGolemType.FLESH) {
            return TEXTURE_FLESH;
        }
        if (material == EnumGolemType.THAUMIUM) {
            return TEXTURE_THAUMIUM;
        }
        // :288 default -> AbstractClientPlayer.field_110314_b
        return DefaultPlayerSkin.getDefaultTexture();
    }

    /**
     * {@code renderWithSway} (:136-144): once the walk swing is running
     * ({@code limbSwingAmount >= 0.01}, MCP {@code field_70721_aZ}), tilt the
     * whole body 6.5 degrees times a 13-unit triangle wave whose phase is
     * {@code limbSwing - limbSwingAmount * (1 - partialTick) + 6}. In 1.21.1
     * that phase expression is exactly what {@code walkAnimation.position(
     * partialTick)} lerps (MCP {@code field_70754_ba} = limbSwing), and the
     * amplitude is {@code walkAnimation.speed()}.
     */
    @Override
    protected void setupRotations(final EntityGolemBase golem, final PoseStack pose, final float ageInTicks,
            final float rotationPitch, final float rotationYaw, final float partialTick) {
        super.setupRotations(golem, pose, ageInTicks, rotationPitch, rotationYaw, partialTick);
        if (golem.walkAnimation.speed() >= 0.01F) {
            final float phase = golem.walkAnimation.position(partialTick) + SWAY_PHASE_OFFSET; // :140
            pose.mulPose(Axis.ZP.rotationDegrees(
                    SWAY_DEGREES * ModelGolem.wave(phase, SWAY_PERIOD))); // :141-142
        }
    }

    @Override
    public void render(final EntityGolemBase golem, final float entityYaw, final float partialTick,
            final PoseStack pose, final MultiBufferSource buffers, final int packedLight) {
        super.render(golem, entityYaw, partialTick, pose, buffers, packedLight);
        renderExtraPasses(golem, partialTick, pose, buffers, packedLight);
    }

    /**
     * The two icon passes of {@code func_77032_a} pass 0 (:69-117): the core
     * icon (:71-86) and the upgrade strip (:88-117).
     */
    public void renderExtraPasses(final EntityGolemBase golem, final float partialTick, final PoseStack pose,
            final MultiBufferSource buffers, final int packedLight) {
        final String decoration = golem.getDecoration();

        if (golem.getCore() > -1) {
            pose.pushPose();
            pose.mulPose(X_ROT_180()); // :73 glRotatef(180, 1, 0, 0)
            final float[] coreIcon = coreIconTranslation(decoration); // :74
            pose.translate(coreIcon[0], coreIcon[1], coreIcon[2]);
            pose.scale(CORE_ICON_SCALE, CORE_ICON_SCALE, CORE_ICON_SCALE); // :75
            pose.mulPose(Y_ROT_180()); // :76 glRotatef(180, 0, 1, 0)
            final TextureAtlasSprite core = itemSprite(CatalogGolems.ITEM_GOLEM_CORE.get().getDefaultInstance());
            // :83-84 bind the item atlas, then renderIcon(...) at 0.2F depth. The
            // source hands renderIcon (minV, maxV, minU, maxU) into the slots it
            // names (minU, maxU, minV, maxV) — a UV transposition, ported as-is.
            drawIconQuad(pose.last().pose(), buffers.getBuffer(RenderType.entityTranslucent(core.atlasLocation())),
                    transposedUv(core), spritePixelSize(core), spritePixelSize(core), 0.2F, packedLight);
            pose.popPose();
        }

        // :88-92 the strip: one quad per upgrade slot, alpha blended
        final byte[] upgrades = golem.upgrades();
        final float[] slots = upgradeSlots(upgrades.length); // :89, :97
        pose.pushPose();
        for (int a = 0; a < upgrades.length; a++) {
            pose.pushPose();
            pose.mulPose(X_ROT_180()); // :96
            pose.translate(slots[a], UPGRADE_Y, UPGRADE_Z); // :97
            pose.scale(UPGRADE_SCALE, UPGRADE_SCALE, UPGRADE_SCALE); // :98
            final TextureAtlasSprite upgrade = itemSprite(new ItemStack(CatalogGolems.ITEM_GOLEM_UPGRADE.get()));
            // :108-111 the four corners with the same transposition, on a unit quad.
            // 1.7.10 binds the item atlas by hand (:94); 1.21.1's render type binds the
            // sprite's own atlas, which is the item atlas for an item icon.
            drawStripQuad(pose.last().pose(),
                    buffers.getBuffer(RenderType.entityTranslucent(upgrade.atlasLocation())),
                    transposedUv(upgrade), packedLight);
            pose.popPose();
        }
        pose.popPose();
    }

    /**
     * {@code renderCarriedItems} (:146-251), the transform stack kept verbatim.
     * Exposed for the parity test and for the carried-item follow-up.
     */
    public void renderCarriedItems(final EntityGolemBase golem, final float partialTick, final PoseStack pose,
            final MultiBufferSource buffers, final int packedLight) {
        final ItemStack carried = golem.getCarried();

        if (golem.getCore() == 11) {
            // :148-166 the core-11 water tilt: the bucket icon on the right arm
            pose.pushPose();
            final float fs = 0.66F; // :151
            pose.mulPose(Axis.XP.rotationDegrees(
                    5.0F + 90.0F * this.getModel().rightArm().xRot / (float) Math.PI)); // :153
            pose.translate(-0.26875F, 1.6F, -0.53F); // :154
            pose.mulPose(Axis.YN.rotationDegrees(90.0F)); // :155 glRotatef(90, 0, -1, 0)
            pose.mulPose(Axis.ZN.rotationDegrees(30.0F)); // :156 glRotatef(30, 0, 0, -1)
            pose.scale(fs, -fs, fs); // :157 glScalef(fs, -fs, fs)
            final TextureAtlasSprite water = itemSprite(new ItemStack(Items.WATER_BUCKET)); // :158 Items.field_151112_aM
            drawIconQuad(pose.last().pose(),
                    buffers.getBuffer(RenderType.entityTranslucent(water.atlasLocation())),
                    transposedUv(water), spritePixelSize(water), spritePixelSize(water), 0.0625F, packedLight); // :164
            pose.popPose();
        }

        if (carried != null && golem.deathTime == 0 && golem.getCore() != 5) {
            // :169-227. 1.7.10 picks a 3D block renderer, an IItemRenderer, a
            // per-renderPass icon loop, or the flat item renderer; 1.21.1 renders
            // every stack, block items included, through renderStatic.
            pose.pushPose();
            pose.scale(0.4F, 0.4F, 0.4F); // :171
            if (carried.getItem() instanceof net.minecraft.world.item.BlockItem) {
                // :197-198 the is3D arm: translate(0, 2.5, -1.25) + rotate 180 about X
                pose.translate(0.0F, 2.5F, -1.25F);
                pose.mulPose(X_ROT_180());
            } else if (isFilledJar(carried)) {
                // :176-194 the filled-jar arm, including the core-6 fill scale (:177-180)
                pose.translate(0.0F, 2.5F, -1.0F);
                if (golem.getCore() == 6) {
                    final double s = 0.5 + Math.min(64, golem.getCarryLimit()) / 128.0;
                    pose.scale((float) s, (float) s, (float) s);
                }
                pose.translate(-0.5F, 0.0F, -0.8F);
                pose.mulPose(X_ROT_180());
                pose.mulPose(Y_ROT_180());
                pose.mulPose(Axis.ZN.rotationDegrees(335.0F));
                pose.mulPose(Axis.YN.rotationDegrees(50.0F));
            } else {
                // :188-194 the flat-item arm
                pose.translate(-0.5F, 2.5F, -1.25F);
                pose.mulPose(X_ROT_180());
                pose.mulPose(Y_ROT_180());
                pose.mulPose(Axis.ZN.rotationDegrees(335.0F));
                pose.mulPose(Axis.YN.rotationDegrees(50.0F));
            }
            Minecraft.getInstance().getItemRenderer().renderStatic(carried, ItemDisplayContext.FIXED,
                    packedLight, OverlayTexture.NO_OVERLAY, pose, buffers, golem.level(), 0);
            pose.popPose();
        } else if (golem.getCore() == 5) {
            // :228-250 the bucket. The .obj mesh is not ported (see the class
            // javadoc); the transforms and the fluid quad are.
            pose.pushPose();
            pose.scale(0.4F, 0.4F, 0.4F); // :230
            pose.translate(0.0F, 3.0F, -1.1F); // :231
            pose.mulPose(Axis.XP.rotationDegrees(90.0F)); // :232
            if (carried != null) {
                final float max = Math.max(carried.getCount(), golem.getFluidCarryLimit()); // :237
                final float fill = carried.getCount() / max; // :238
                pose.translate(0.0F, 0.0F, 0.2F + 0.8F * fill); // :240
                pose.scale(0.8F, 0.8F, 0.8F); // :241
                final TextureAtlasSprite fluid = itemSprite(carried); // :242 fluid.getIcon()
                final int light = Math.max(packedLight, LightTexture.FULL_BRIGHT); // :243-244
                drawCenteredQuad(pose.last().pose(),
                        buffers.getBuffer(RenderType.entityTranslucent(fluid.atlasLocation())),
                        fluid.getU0(), fluid.getU1(), fluid.getV0(), fluid.getV1(), 1.0F, light); // :245
            }
            pose.popPose();
        }
    }

    /** The 1.7.10 {@code getGolemDecoration().length() > 0 || advanced} gate of :119. */
    static boolean hasDecoration(final EntityGolemBase golem) {
        return showsDecorationFor(golem.getDecoration(), golem.isAdvanced());
    }

    /** The gate of :119 as a pure function of the two figures it reads. */
    public static boolean showsDecorationFor(final String decoration, final boolean advanced) {
        return decoration.length() > 0 || advanced;
    }

    /** The {@code getHealthPercentage() < 1.0F} gate of :125. */
    static boolean showsDamage(final EntityGolemBase golem) {
        return showsDamageFor(golem.getHealthPercentage());
    }

    /** The gate of :125 as a pure function. */
    public static boolean showsDamageFor(final float healthPercentage) {
        return healthPercentage < 1.0F;
    }

    /** :74 the core icon's translation; a {@code P} decoration lifts it 0.03. */
    public static float[] coreIconTranslation(final String decoration) {
        return new float[] {0.0875F, -0.96F, 0.15F + (decoration.contains("P") ? 0.03F : 0.0F)};
    }

    /** :97 the strip's x per slot: {@code -0.05F - shift*(n-1)/2 + shift*a}. */
    public static float[] upgradeSlots(final int count) {
        final float[] slots = new float[count];
        for (int a = 0; a < count; a++) {
            slots[a] = -0.05F - UPGRADE_SHIFT * (count - 1) / 2.0F + UPGRADE_SHIFT * a;
        }
        return slots;
    }

    /**
     * The {@code glColor4f(1, 1, 1, 1 - healthPercentage)} of :128 as a packed
     * ARGB int (1.21.1's vertex colour is ABGR-packed, so white with that alpha
     * is {@code alpha << 24 | 0xFFFFFF}).
     */
    public static int damageTint(final float healthPercentage) {
        final int alpha = (int) (255.0F * (1.0F - healthPercentage));
        return (Math.max(0, Math.min(255, alpha)) << 24) | 0x00FFFFFF;
    }

    /** The {@code .obj} loader's part name, kept for the follow-up card (:234). */
    public static final String BUCKET_MODEL_PART = "Bucket";

    private static ResourceLocation sheet(final String name) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/" + name);
    }

    /** {@code GL11.glRotatef(180, 1, 0, 0)}. */
    private static Quaternionf X_ROT_180() {
        return Axis.XP.rotationDegrees(180.0F);
    }

    /** {@code GL11.glRotatef(180, 0, 1, 0)}. */
    private static Quaternionf Y_ROT_180() {
        return Axis.YP.rotationDegrees(180.0F);
    }

    /** True for the port's filled-jar item, the stand-in for 1.7.10's {@code ItemJarFilled} (:175). */
    private static boolean isFilledJar(final ItemStack stack) {
        return stack.getItem() instanceof com.jonnyutah12.thaumcraftclassic.items.ItemJarFilled;
    }

    /** The item model's sprite for a stack — see delta 3 in the class javadoc. */
    static TextureAtlasSprite itemSprite(final ItemStack stack) {
        final Minecraft minecraft = Minecraft.getInstance();
        final BakedModel model = minecraft.getItemRenderer().getModel(stack, minecraft.level, null, 0);
        return model.getParticleIcon();
    }

    /** A sprite's pixel width/height, the source's {@code icon.getIconWidth()/getIconHeight()} (:84, :164). */
    static int spritePixelSize(final TextureAtlasSprite sprite) {
        return sprite.contents().width();
    }

    /**
     * The source's UV transposition (:79-84, :101-104): it passes
     * {@code (getMinV, getMaxV, getMinU, getMaxU)} into a callee whose parameters
     * MCP names {@code (minU, maxU, minV, maxV)}, so the sampled rectangle is
     * transposed. Returned as {@code {u0, u1, v0, v1}} in the order the quads use.
     */
    public static float[] transposedUv(final TextureAtlasSprite sprite) {
        return transposedUv(sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());
    }

    /** The transposition as a pure function: {@code (u0, u1, v0, v1)} in, {@code (v0, v1, u0, u1)} out. */
    public static float[] transposedUv(final float u0, final float u1, final float v0, final float v1) {
        return new float[] {v0, v1, u0, u1};
    }

    /**
     * The four corners of {@code ItemRenderer.func_78439_a} (:84, :164, :213):
     * {@code (0,0,z) uv00, (w,0,z) uv10, (w,h,z) uv11, (0,h,z) uv01}.
     */
    public static float[] iconQuadCorners(final float[] uv, final float width, final float height, final float z) {
        return new float[] {
                0.0F, 0.0F, z, uv[0], uv[2],
                width, 0.0F, z, uv[1], uv[2],
                width, height, z, uv[1], uv[3],
                0.0F, height, z, uv[0], uv[3],
        };
    }

    /**
     * The four corners of the hand-built upgrade quad (:108-111): a unit quad,
     * with the first corner at {@code (f1, f4)} = (u0, v1) and the walk
     * {@code (u0,v1) (u1,v1) (u1,v0) (u0,v0)}.
     */
    public static float[] stripQuadCorners(final float[] uv) {
        return new float[] {
                0.0F, 0.0F, 0.0F, uv[0], uv[1],
                1.0F, 0.0F, 0.0F, uv[2], uv[1],
                1.0F, 1.0F, 0.0F, uv[2], uv[3],
                0.0F, 1.0F, 0.0F, uv[0], uv[3],
        };
    }

    private static void drawIconQuad(final org.joml.Matrix4f matrix, final VertexConsumer buffer,
            final float[] uv, final float width, final float height, final float z, final int light) {
        final float[] corners = iconQuadCorners(uv, width, height, z);
        quad(matrix, buffer, corners, light);
    }

    private static void drawStripQuad(final org.joml.Matrix4f matrix, final VertexConsumer buffer,
            final float[] uv, final int light) {
        quad(matrix, buffer, stripQuadCorners(uv), light);
    }

    /** {@code UtilsFX.renderQuadCenteredFromIcon} (:245): a size x size quad about the origin. */
    private static void drawCenteredQuad(final org.joml.Matrix4f matrix, final VertexConsumer buffer,
            final float u0, final float u1, final float v0, final float v1, final float size, final int light) {
        final float half = size / 2.0F;
        quad(matrix, buffer, new float[] {
                -half, -half, 0.0F, u0, v0,
                half, -half, 0.0F, u1, v0,
                half, half, 0.0F, u1, v1,
                -half, half, 0.0F, u0, v1,
        }, light);
    }

    private static void quad(final org.joml.Matrix4f matrix, final VertexConsumer buffer,
            final float[] corners, final int light) {
        for (int i = 0; i < 4; i++) {
            final int base = i * 5;
            buffer.addVertex(matrix, corners[base], corners[base + 1], corners[base + 2])
                    .setUv(corners[base + 3], corners[base + 4])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setColor(255, 255, 255, 255);
        }
    }

    /**
     * The decoration model layer: {@code func_77032_a} pass 1 (:119-123) — bind
     * {@code golem_decoration.png}, draw the accessories model, return 1 so the
     * vanilla chain renders it.
     */
    static final class AccessoriesLayer extends RenderLayer<EntityGolemBase, ModelGolem> {
        private final ModelGolemAccessories accessoryModel;

        AccessoriesLayer(final RenderGolemBase parent, final ModelGolemAccessories accessoryModel) {
            super(parent);
            this.accessoryModel = accessoryModel;
        }

        @Override
        public void render(final PoseStack pose, final MultiBufferSource buffers, final int packedLight,
                final EntityGolemBase golem, final float limbSwing, final float limbSwingAmount,
                final float partialTick, final float ageInTicks, final float netHeadYaw, final float headPitch) {
            if (!hasDecoration(golem)) {
                return;
            }
            final ModelGolemAccessories model = this.accessoryModel;
            model.setupAnim(golem, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            model.prepareMobModel(golem, limbSwingAmount, ageInTicks, netHeadYaw);
            pose.pushPose();
            model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE_DECORATION)),
                    packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            pose.popPose();
        }
    }

    /**
     * The damage model layer: {@code func_77032_a} pass 2 (:125-130) — bind
     * {@code golem_damage.png}, draw the {@code pass = 2} copy of the body model,
     * set the alpha to {@code 1 - healthPercentage}. 1.7.10's
     * {@code GL_BLEND}/{@code GL_ALPHA_FUNC 1/255} pair (ModelGolem.java:51-55)
     * is {@link RenderType#entityTranslucent} here.
     */
    static final class DamageLayer extends RenderLayer<EntityGolemBase, ModelGolem> {
        private final ModelGolem damageModel;

        DamageLayer(final RenderGolemBase parent, final ModelGolem damageModel) {
            super(parent);
            this.damageModel = damageModel;
        }

        @Override
        public void render(final PoseStack pose, final MultiBufferSource buffers, final int packedLight,
                final EntityGolemBase golem, final float limbSwing, final float limbSwingAmount,
                final float partialTick, final float ageInTicks, final float netHeadYaw, final float headPitch) {
            if (!showsDamage(golem)) {
                return;
            }
            // The original's second ModelGolem shares the animation of the first
            // (RenderLiving hands both models the same callback), which is what
            // vanilla's copy-layer helper does.
            coloredCutoutModelCopyLayerRender(this.getParentModel(), this.damageModel, TEXTURE_DAMAGE, pose,
                    buffers, packedLight, golem, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch,
                    partialTick, damageTint(golem.getHealthPercentage()));
        }
    }
}
