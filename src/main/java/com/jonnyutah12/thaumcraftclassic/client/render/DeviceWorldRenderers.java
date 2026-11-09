package com.jonnyutah12.thaumcraftclassic.client.render;

import com.jonnyutah12.thaumcraftclassic.blocks.BlockCrystal;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockMirror;
import com.jonnyutah12.thaumcraftclassic.render.art.CrystalWorldArt;
import com.jonnyutah12.thaumcraftclassic.render.art.JarWorldArt;
import com.jonnyutah12.thaumcraftclassic.render.art.MirrorWorldArt;
import com.jonnyutah12.thaumcraftclassic.render.art.SpaWorldArt;
import com.jonnyutah12.thaumcraftclassic.tiles.TileCrystal;
import com.jonnyutah12.thaumcraftclassic.tiles.TileJarFillable;
import com.jonnyutah12.thaumcraftclassic.tiles.TileMirror;
import com.jonnyutah12.thaumcraftclassic.tiles.TileMirrorEssentia;
import com.jonnyutah12.thaumcraftclassic.tiles.TileSpa;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * The world tile-entity renderers of BD-18 slice 4 (t_6563ce50, D-021).
 *
 * <p>Each renderer is a thin projection of a live tile through a
 * {@code render.art} data plan: it reads the tile every frame, asks the plan
 * what to draw, and emits quads. All art values (heights, tints, angles,
 * offsets, sprite choices) come from the plan classes, which the gametests
 * mutate — so the visible art provably follows the tile data. The plan javadocs
 * carry the 1.7.10 citation for every constant.</p>
 *
 * <p>Render types: the fluid/water use the translucent block blend
 * ({@code RenderType.translucent()}), matching the original's
 * {@code blendFunc(770, 771)}; the label, panes and portal layers use
 * {@code RenderType.translucent()} and are emitted <b>double-sided</b>,
 * because 1.21.1's sprite layers cull back faces while the originals disabled
 * culling around these quads. The original's second portal layer blended
 * additively ({@code blendFunc(1, 1)}, {@code TileMirrorRenderer.java:60});
 * 1.21.1 has no stock additive sprite-sheet render type on the block atlas, so
 * both layers blend like translucency — recorded in BD-18.</p>
 *
 * <p>Light: the plan values are the original's 0-255 floors converted into
 * 1.21.1's packed lightmap ({@link LightTexture}); the label, panes and portal
 * ride on {@link LightTexture#FULL_BRIGHT}, the 1.21.1 form of the original's
 * {@code OpenGlHelper.setLightmapTextureCoords} overrides
 * ({@code TileJarRenderer.java:118-123}, {@code TileCrystalRenderer.java:57-60}).</p>
 */
public final class DeviceWorldRenderers {

    /**
     * A sprite from the block atlas. 1.21.1 exposes the atlas as a lookup
     * function ({@code Minecraft#getTextureAtlas}); the mod's
     * {@code atlases/blocks.json} publishes the {@code models}/{@code aspects}
     * directories and the four single sprites this slice draws, and every sprite
     * id here is a published {@code docs/asset-mapping.csv} row (D-005) — the
     * spa's water is vanilla's own sprite, which the CSV does not govern.
     */
    private static TextureAtlasSprite sprite(final ResourceLocation id) {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(id);
    }

    /** Packed fullbright for the overlay-style layers. */
    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;

    /** Vanilla's default water colour ({@code Biome.DEFAULT_WATER_COLOR}). */
    private static final int VANILLA_WATER_COLOUR = 0x3F76E4;

    // ------------------------------------------------------------------ jar

    /**
     * The warded/void jar: fluid box + label from {@link JarWorldArt.Plan}.
     * The static jar model ships the glass; the TER adds what 1.7.10 drew
     * from the tile every frame. The node-jar scale pulse
     * ({@code TileJarRenderer.java:87-96}) is a {@code TileJarBrain}/{@code
     * TileJarNode} scene — out of this slice (the node tiles have no plan
     * here); recorded in BD-18.
     */
    public static final class JarTer<T extends TileJarFillable> implements BlockEntityRenderer<T> {

        public JarTer(final BlockEntityRendererProvider.Context context) {
            // The plan reads the tile every frame; the context is unused.
        }

        @Override
        public void render(final T tile, final float partialTick, final PoseStack pose,
                final MultiBufferSource buffers, final int packedLight, final int packedOverlay) {
            final JarWorldArt.Plan plan = JarWorldArt.plan(tile);

            if (plan.fluidHeight() > 0.0F) {
                final TextureAtlasSprite fluid = sprite(JarWorldArt.FLUID_SPRITE);
                // The original's fluid brightness is max(200, surrounding light)
                // (TileJarRenderer.java:118-121): the plan's channel is the
                // 200/255 floor (11), raised by the cell's own block light.
                final int blockChannel = Math.max(plan.fluidLight(), LightTexture.block(packedLight) / 16);
                final int light = LightTexture.pack(blockChannel * 16, LightTexture.sky(packedLight) / 16);
                final int rgb = plan.fluidArgb();
                final float top = JarWorldArt.FLUID_BASE_Y + plan.fluidHeight();
                box(pose, buffers.getBuffer(RenderType.translucent()), fluid,
                        JarWorldArt.FLUID_MIN_XZ, JarWorldArt.FLUID_BASE_Y, JarWorldArt.FLUID_MIN_XZ,
                        JarWorldArt.FLUID_MAX_XZ, top, JarWorldArt.FLUID_MAX_XZ,
                        rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, 255, light, packedOverlay);
            }

            final JarWorldArt.Label label = plan.label();
            if (label != null) {
                pose.pushPose();
                pose.translate(0.5, 1.0, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(
                        JarWorldArt.facingRotation(tile.facing)));
                pose.mulPose(Axis.ZP.rotationDegrees(label.tiltDeg()));
                pose.translate(0.0, -0.4, 0.315);
                // The published label quad: size 0.5 centred (TileJarRenderer.java:69-72),
                // fullbright like the original's overlay pass (blit layer).
                quadFace(pose, buffers.getBuffer(RenderType.translucent()),
                        sprite(label.labelSprite()),
                        -0.25F, -0.25F, 0.0F, 0.25F, 0.25F, 0.0F,
                        Direction.SOUTH, FULL_BRIGHT, 0, true);
                // The aspect tag icon centred on it (TileJarRenderer.java:74-82).
                quadFace(pose, buffers.getBuffer(RenderType.translucent()),
                        sprite(label.aspectSprite()),
                        -0.15F, -0.15F, 0.002F, 0.15F, 0.15F, 0.002F,
                        Direction.SOUTH, FULL_BRIGHT, 0, true);
                pose.popPose();
            }
        }
    }

    // -------------------------------------------------------------- crystal

    /**
     * The crystal cluster: six tilted cubes from {@link CrystalWorldArt}
     * seeded by the tile's own coordinates — the same cluster at the same
     * spot always draws the same shards (the mutation the gametest pins:
     * move/rotate the tile, the shard list changes).
     */
    public static final class CrystalTer implements BlockEntityRenderer<TileCrystal> {

        public CrystalTer(final BlockEntityRendererProvider.Context context) {
            // The plan reads the tile every frame; the context is unused.
        }

        @Override
        public void render(final TileCrystal tile, final float partialTick, final PoseStack pose,
                final MultiBufferSource buffers, final int packedLight, final int packedOverlay) {
            final int variant = tile.getBlockState().getValue(BlockCrystal.VARIANT);
            // Variant 7 (strange) is the eldritch host: a separate
            // TileEldritchCrystal + TileEldritchCrystalRenderer in the original
            // (a TileEldritchCrystal the port does not ship), and its
            // colors[md + 1] index runs off the end of the seven-entry array.
            // This renderer declines the scene — recorded in BD-18.
            if (!CrystalWorldArt.drawsCluster(variant)) {
                return;
            }
            // The mixed cluster tints the main cube colors[5] and cycles the
            // surrounding shards (TileCrystalRenderer.java:74-76, 95).
            final var shards = CrystalWorldArt.shards(variant, tile.getBlockPos());
            final float[] placement = CrystalWorldArt.orientationPlacement(tile.getOrientation());

            pose.pushPose();
            pose.translate(placement[0], placement[1], placement[2]);
            pose.mulPose(Axis.XP.rotationDegrees(placement[3]));
            pose.mulPose(Axis.YP.rotationDegrees(placement[4]));
            pose.mulPose(Axis.ZP.rotationDegrees(placement[5]));
            final TextureAtlasSprite crystal = sprite(CrystalWorldArt.CRYSTAL_SPRITE);
            // The original lit each cube (int) (210F * shade) with a per-frame
            // shade pulse (TileCrystalRenderer.java:43, 57); the plan carries
            // the 210/255 floor (12) and the pulse rides on it per frame.
            final float shade = 0.925F + (tile.getLevel() == null
                    ? 0.075F : tile.getLevel().random.nextFloat() * 0.075F);
            final int light = LightTexture.pack(
                    Math.max((int) (CrystalWorldArt.CRYSTAL_LIGHT * shade), 1) * 16,
                    LightTexture.sky(packedLight) / 16);
            final VertexConsumer consumer = buffers.getBuffer(RenderType.translucent());
            for (int shard = 0; shard < shards.size(); shard++) {
                final CrystalWorldArt.Shard cube = shards.get(shard);
                final int rgb = variant == CrystalWorldArt.MIXED
                        ? CrystalWorldArt.mixedShardTint(shard)
                        : CrystalWorldArt.tintFor(variant);
                pose.pushPose();
                pose.mulPose(Axis.YP.rotationDegrees(cube.yawDeg()));
                pose.mulPose(Axis.XP.rotationDegrees(cube.pitchDeg()));
                boxAtOrigin(pose, consumer, crystal, cube.scaleX(), cube.scaleY(), cube.scaleZ(),
                        rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, 255, light, packedOverlay);
                pose.popPose();
            }
            pose.popPose();
        }
    }

    // --------------------------------------------------------------- mirror

    /**
     * The mirror pane + portal from {@link MirrorWorldArt.Plan}: the pane
     * swaps with the live link, the jitter rides {@code TileMirror.instability},
     * and a linked mirror draws the tunnel/particlefield portal box in front
     * of the pane. Both mirror kinds share this renderer: the original
     * registers one {@code TileMirrorRenderer} for both tile classes
     * ({@code ClientProxy.java:496-498}), and the port's
     * {@code TileMirror}/{@code TileMirrorEssentia} are siblings (both extend
     * {@code BlockEntity}, as in the reference), so it binds to
     * {@code BlockEntity} and reads the link per type.
     */
    public static final class MirrorTer implements BlockEntityRenderer<BlockEntity> {

        public MirrorTer(final BlockEntityRendererProvider.Context context) {
            // The plan reads the tile every frame; the context is unused.
        }

        @Override
        public void render(final BlockEntity tile, final float partialTick, final PoseStack pose,
                final MultiBufferSource buffers, final int packedLight, final int packedOverlay) {
            // The original registers this one renderer for BOTH tile classes
            // (ClientProxy.java:496-498), so the port binds it to BlockEntity
            // and reads the link from whichever mirror it was handed.
            // instability is a TileMirror field only (TileMirrorEssentia has no
            // such field in the reference), so an essentia mirror never jitters.
            final boolean linked;
            final int instability;
            if (tile instanceof TileMirror mirror) {
                linked = mirror.isLinkValidSimple();
                instability = mirror.instability;
            } else if (tile instanceof TileMirrorEssentia essentia) {
                linked = essentia.isLinkValidSimple();
                instability = 0;
            } else {
                return;
            }
            final float jitter = tile.getLevel() == null ? 0.0F
                    : MirrorWorldArt.jitter(instability, tile.getLevel().random.nextFloat());
            final MirrorWorldArt.Plan plan = MirrorWorldArt.plan(linked, jitter);
            final Direction facing = BlockMirror.facingFor(tile.getBlockState().getValue(BlockMirror.VARIANT));

            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(facing.getRotation());
            if (linked) {
                // The portal box, centred, inset p = 0.1875; tunnel on the four
                // sides, particlefield top + bottom, fullbright
                // (TileMirrorRenderer.java:41-60).
                final float a = MirrorWorldArt.PORTAL_INSET - 0.5F;
                final float b = 0.5F - MirrorWorldArt.PORTAL_INSET;
                final VertexConsumer tunnel = buffers.getBuffer(RenderType.translucent());
                quadFace(pose, tunnel, sprite(plan.tunnelSprite()), a, a, b, b, b, b,
                        Direction.SOUTH, FULL_BRIGHT, 0, true);
                quadFace(pose, tunnel, sprite(plan.tunnelSprite()), a, a, a, b, b, a,
                        Direction.NORTH, FULL_BRIGHT, 0, true);
                quadFace(pose, tunnel, sprite(plan.tunnelSprite()), b, a, a, b, b, b,
                        Direction.EAST, FULL_BRIGHT, 0, true);
                quadFace(pose, tunnel, sprite(plan.tunnelSprite()), a, a, a, a, b, b,
                        Direction.WEST, FULL_BRIGHT, 0, true);
                final VertexConsumer field = buffers.getBuffer(RenderType.translucent());
                quadFace(pose, field, sprite(plan.fieldSprite()), a, b, a, b, b, b,
                        Direction.UP, FULL_BRIGHT, 0, true);
                quadFace(pose, field, sprite(plan.fieldSprite()), a, a, a, b, a, b,
                        Direction.DOWN, FULL_BRIGHT, 0, true);
            }
            // The pane quad at the 0.02 (+ jitter) standoff from the facing face
            // (TileMirrorRenderer.java:617-629), centred on the cell.
            final VertexConsumer pane = buffers.getBuffer(RenderType.translucent());
            quadFace(pose, pane, sprite(plan.paneSprite()),
                    -0.5F, -0.5F, plan.paneOffset(),
                    0.5F, 0.5F, plan.paneOffset(), Direction.SOUTH,
                    FULL_BRIGHT, packedOverlay, true);
            pose.popPose();
        }
    }

    // ------------------------------------------------------------------ spa

    /**
     * The spa water: NEW art (the 1.7.10 spa drew no water — see
     * {@link SpaWorldArt}); a vanilla {@code water_still} surface on the spa's
     * top face, shown while the tank holds fluid, covering the fill fraction.
     */
    public static final class SpaTer implements BlockEntityRenderer<TileSpa> {

        public SpaTer(final BlockEntityRendererProvider.Context context) {
            // The plan reads the tile every frame; the context is unused.
        }

        @Override
        public void render(final TileSpa tile, final float partialTick, final PoseStack pose,
                final MultiBufferSource buffers, final int packedLight, final int packedOverlay) {
            final SpaWorldArt.Plan plan = SpaWorldArt.plan(tile.getTankAmount(), SpaWorldArt.TANK_CAPACITY);
            if (!plan.visible()) {
                return;
            }
            // Vanilla's own default water colour: the biome water tint is a
            // live-world delta recorded in BD-18, so the port draws the plain
            // vanilla blue (the 1.7.10 spa has no water to tint at all).
            final float inset = SpaWorldArt.WATER_INSET;
            final float far = inset + (1.0F - 2.0F * inset) * plan.uScale();
            quadFace(pose, buffers.getBuffer(RenderType.translucent()),
                    sprite(SpaWorldArt.WATER_SPRITE),
                    inset, 1.0F - 1.0F / 32.0F, inset, far, 1.0F - 1.0F / 32.0F, 1.0F - inset,
                    Direction.UP, packedLight, packedOverlay, false,
                    VANILLA_WATER_COLOUR >> 16 & 0xFF, VANILLA_WATER_COLOUR >> 8 & 0xFF,
                    VANILLA_WATER_COLOUR & 0xFF, 200);
        }
    }

    // ------------------------------------------------------------- geometry

    /**
     * An axis-aligned box with full-sprite UVs on every face. Each face is
     * emitted on its own plane with the outward normal and the winding 1.21.1's
     * {@code ShapeRenderer} uses, so the cull pass keeps the visible sides.
     */
    private static void box(final PoseStack pose, final VertexConsumer consumer, final TextureAtlasSprite sprite,
            final float x0, final float y0, final float z0, final float x1, final float y1, final float z1,
            final int r, final int g, final int b, final int a, final int light, final int overlay) {
        quadFace(pose, consumer, sprite, x0, y0, z0, x1, y1, z0, Direction.SOUTH, light, overlay, false, r, g, b, a);
        quadFace(pose, consumer, sprite, x0, y0, z1, x1, y1, z1, Direction.NORTH, light, overlay, false, r, g, b, a);
        quadFace(pose, consumer, sprite, x1, y0, z0, x1, y1, z1, Direction.EAST, light, overlay, false, r, g, b, a);
        quadFace(pose, consumer, sprite, x0, y0, z0, x0, y1, z1, Direction.WEST, light, overlay, false, r, g, b, a);
        quadFace(pose, consumer, sprite, x0, y1, z0, x1, y1, z1, Direction.UP, light, overlay, false, r, g, b, a);
        quadFace(pose, consumer, sprite, x0, y0, z0, x1, y0, z1, Direction.DOWN, light, overlay, false, r, g, b, a);
    }

    /** A unit cube scaled about the origin (the crystal's centred shard model). */
    private static void boxAtOrigin(final PoseStack pose, final VertexConsumer consumer, final TextureAtlasSprite sprite,
            final float sx, final float sy, final float sz,
            final int r, final int g, final int b, final int a, final int light, final int overlay) {
        box(pose, consumer, sprite, -sx, -sy, -sz, sx, sy, sz, r, g, b, a, light, overlay);
    }

    /**
     * An axis-aligned quad facing {@code normal}, corners given in the current
     * space, white. {@code doubleSided} re-emits the corners reversed with a
     * flipped normal — the 1.21.1 answer to the original's
     * {@code glDisable(GL_CULL_FACE)} around these quads.
     */
    private static void quadFace(final PoseStack pose, final VertexConsumer consumer, final TextureAtlasSprite sprite,
            final float x0, final float y0, final float z0, final float x1, final float y1, final float z1,
            final Direction normal, final int light, final int overlay, final boolean doubleSided) {
        quadFace(pose, consumer, sprite, x0, y0, z0, x1, y1, z1, normal, light, overlay, doubleSided,
                255, 255, 255, 255);
    }

    private static void quadFace(final PoseStack pose, final VertexConsumer consumer, final TextureAtlasSprite sprite,
            final float x0, final float y0, final float z0, final float x1, final float y1, final float z1,
            final Direction normal, final int light, final int overlay, final boolean doubleSided,
            final int r, final int g, final int b, final int a) {
        final Matrix4f matrix = pose.last().pose();
        final float u0 = sprite.getU0();
        final float u1 = sprite.getU1();
        final float v0 = sprite.getV0();
        final float v1 = sprite.getV1();
        // Corners wound counter-clockwise seen from the side `normal` points at
        // (1.21.1 culls back faces), UVs mapped to the sprite rect.
        final float[][] corners = switch (normal) {
            case SOUTH -> new float[][] {{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}};
            case NORTH -> new float[][] {{x1, y0, z1}, {x0, y0, z1}, {x0, y1, z1}, {x1, y1, z1}};
            case EAST -> new float[][] {{x1, y0, z0}, {x1, y0, z1}, {x1, y1, z1}, {x1, y1, z0}};
            case WEST -> new float[][] {{x0, y0, z1}, {x0, y0, z0}, {x0, y1, z0}, {x0, y1, z1}};
            case UP -> new float[][] {{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}};
            case DOWN -> new float[][] {{x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}, {x0, y0, z0}};
        };
        final float[] us = {u0, u1, u1, u0};
        final float[] vs = {v1, v1, v0, v0};
        for (int side = 0; side < (doubleSided ? 2 : 1); side++) {
            final boolean flipped = side == 1;
            for (int corner = 0; corner < 4; corner++) {
                final int at = flipped ? 3 - corner : corner;
                final float[] c = corners[at];
                consumer.addVertex(matrix, c[0], c[1], c[2])
                        .setUv(us[corner], vs[corner])
                        .setColor(r, g, b, a)
                        // 1.21.1's block format carries the lightmap as the UV2
                        // pair (block, sky) and the overlay as its own attribute.
                        .setUv2(light & 0xFFFF, light >> 16 & 0xFFFF)
                        .setOverlay(overlay)
                        .setNormal(pose.last(),
                                flipped ? -normal.getStepX() : normal.getStepX(),
                                flipped ? -normal.getStepY() : normal.getStepY(),
                                flipped ? -normal.getStepZ() : normal.getStepZ());
            }
        }
    }

    private DeviceWorldRenderers() {
    }
}
