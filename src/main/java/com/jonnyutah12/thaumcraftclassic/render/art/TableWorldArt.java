package com.jonnyutah12.thaumcraftclassic.render.art;

import net.minecraft.resources.ResourceLocation;

/**
 * The table's cloth as a data plan (BD-18, t_6563ce50, D-021).
 *
 * <p>1.7.10 drew the table — cloth, legs and crossbar — per frame inside
 * {@code thaumcraft.client.renderers.tile.TileTableRenderer}: it binds the
 * published {@code models/table.png} ({@code :24}), moves the pivot to
 * {@code (x + 0.5, y + 1.0, z + 0.5)}, rotates the model 180° about X
 * ({@code :25-26}) — the boxes are authored hanging from the top, so the flip
 * stands them up — and rotates a further 90° about Y for metadata 1
 * ({@code :27-29}), the pair-placed cloth. It draws this for
 * {@code md < 6} only ({@code :22}): the research-table and deconstruction
 * metas get no cloth. The geometry is the four boxes of
 * {@code ModelTable.java:7-10, 15-37} (top 16x4x16, two 4x8x4 legs, a 16x4x8
 * crossbar) on a 64x32 sheet ({@code :13-14}).</p>
 *
 * <p><b>D-021 route: a static model, no renderer.</b> Nothing in that draw
 * reads live data — the metadata is the only input, and 1.21.1 publishes the
 * metadata as a block state. The shipped {@code
 * models/block/blocktable_cloth.json} carries the same four boxes with the
 * original's UV rectangles, and the {@code blocktable} blockstate names it
 * beside the plain table model for variants 0..5 (the original's
 * {@code md < 6} gate, {@code TileTableRenderer.java:22}), with
 * {@code "y": 90} on variant 1 for the original's second rotation. That is why
 * the table has no {@code BlockEntityRenderer}: a renderer here would re-draw
 * a state-driven shape every frame for free.</p>
 *
 * <p>Server-safe: ints in, sprite id and rotation out.</p>
 */
public final class TableWorldArt {

    /** The cloth's sheet: the original's table texture
     * ({@code TileTableRenderer.java:24}), published as the models/table row. */
    public static final ResourceLocation CLOTH_SPRITE =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "models/table");

    /** The original draws the cloth for metadata 0..5 only ({@code :22}). */
    public static final int CLOTH_LAST_VARIANT = 5;

    /** The pair-placed cloth's extra 90° yaw ({@code TileTableRenderer.java:27-29}). */
    public static final int PAIR_PLACEMENT_YAW = 90;

    /**
     * Does this table variant get the cloth? The original's TER gate is
     * {@code md < 6} ({@code TileTableRenderer.java:22}); variants 14/15 (the
     * deconstruction table and arcane workbench) never draw it.
     */
    public static boolean drawsCloth(final int variant) {
        return variant >= 0 && variant <= CLOTH_LAST_VARIANT;
    }

    /** The cloth's yaw for a variant: 90° on the pair-placed variant 1, else 0. */
    public static int clothYaw(final int variant) {
        return variant == 1 ? PAIR_PLACEMENT_YAW : 0;
    }

    /** What one table variant says to draw: the cloth, its yaw, its sprite. */
    public static Plan plan(final int variant) {
        return new Plan(drawsCloth(variant), clothYaw(variant), CLOTH_SPRITE);
    }

    /** @param cloth does the variant's blockstate key carry the cloth model pair */
    public record Plan(boolean cloth, int yawDeg, ResourceLocation sprite) {
    }

    private TableWorldArt() {
    }
}
