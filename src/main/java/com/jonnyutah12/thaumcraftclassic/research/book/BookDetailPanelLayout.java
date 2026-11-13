package com.jonnyutah12.thaumcraftclassic.research.book;

import com.jonnyutah12.thaumcraftclassic.recipes.RecipePageLayout;

/**
 * The book screen's detail panel box, and where the recipe page body sits inside
 * it (TC-M3-04-s-iv-b-2, closes BD-64).
 *
 * <p><b>Why a plain record (D-019 rule 2).</b> The panel's pixels live in the
 * client window, and {@code runGameTestServer} is a dedicated server that never
 * constructs a {@code Screen}, so the geometry a probe is allowed to assert on
 * has to live outside it. This class imports no Minecraft/NeoForge/Mojang type
 * and no {@code net.minecraft.world.item.crafting} type: it splits a box and
 * delegates to {@link RecipePageLayout}, whose constants are the original's
 * (measured off {@code GuiResearchRecipe#drawCraftingPage},
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiResearchRecipe.java:755-946}).
 * The window draws what this record computes and keeps no box of its own.</p>
 *
 * <p><b>The split.</b> The original's recipe page drew the row's grid as the
 * whole page body: a {@code normal_crafting} page carried no text at all (its
 * renderer is the crafting case of the page dispatch,
 * {@code GuiResearchRecipe.java:263-311}, drawing at {@code 755-946}). The
 * unified book screen has no per-page pager — it lists entries and shows one
 * detail panel that carries the entry's text — so the panel gives the block a
 * region at its bottom and keeps {@link #TEXT_ROWS_KEPT} rows of text above it.
 * The block is fitted with {@link RecipePageLayout#scaleFor}, which is the
 * original's size or smaller (never enlarged, floor
 * {@link RecipePageLayout#MIN_SCALE}), centred in the column. When the region
 * cannot hold the block at that floor, {@link #bodyFits()} is false and the
 * panel shows its text body — the same degradation as an entry with no linked
 * row and as a row the live {@code RecipeManager} does not carry, so the window
 * never paints a clipped grid over entry text.</p>
 *
 * <p><b>The shipped numbers.</b> Fed the book panel's column —
 * {@code ThaumonomiconScreen.DETAIL_WIDTH} wide, from {@code ROWS_TOP - 2} to
 * {@code PANE_HEIGHT - 4}, at {@code DETAIL_PITCH} — the region is 100x146 and
 * the 104x156 block fits at scale ~0.94, the book's column being four pixels
 * narrower than the original's backdrop box.</p>
 *
 * @param left      the panel's text left edge, in window coordinates
 * @param top       the panel's top edge, in window coordinates
 * @param width     the panel's text width
 * @param bottom    the panel's bottom edge (exclusive), in window coordinates
 * @param textPitch the panel's line pitch, so the split tracks the text metrics
 */
public record BookDetailPanelLayout(int left, int top, int width, int bottom, int textPitch) {

    /** The panel's top padding above its first text row (the window's own inset). */
    public static final int TEXT_TOP_PAD = 3;

    /**
     * The rows of entry text the panel keeps when a recipe body draws: the
     * entry's label, its state line, and two more — the lines that name what the
     * row is and whether the player can research it. Port-authored: the original's
     * recipe page had no text to keep, its body was the grid (BD-64).
     */
    public static final int TEXT_ROWS_KEPT = 4;

    /** The gap between the kept text and the top of the body block. */
    public static final int BODY_GAP = 3;

    /** The exclusive bottom of the text column when no body draws (the whole panel). */
    public int textBottom() {
        return bottom;
    }

    /** The exclusive bottom of the text column when a body draws. */
    public int textBottomWithBody() {
        return bodyTop();
    }

    /** The first row the body block may occupy. */
    public int bodyTop() {
        return top + TEXT_TOP_PAD + TEXT_ROWS_KEPT * textPitch + BODY_GAP;
    }

    /** The height the body block has to fit into. */
    public int bodyHeight() {
        return bottom - bodyTop();
    }

    /** The scale the block draws at: the layout's fit of the region, never above 1. */
    public float bodyScale() {
        return RecipePageLayout.scaleFor(width, bodyHeight());
    }

    /** The body block's fitted placement: the original's offsets, centred in the column. */
    public RecipePageLayout bodyLayout() {
        final float scale = bodyScale();
        final int slack = width - Math.round(RecipePageLayout.BLOCK_WIDTH * scale);
        return new RecipePageLayout(left + Math.max(0, slack) / 2, bodyTop(), scale);
    }

    /**
     * True when the region can hold the block at its fitted scale. False means the
     * panel has no room for a picture and shows its text body — the same
     * degradation as a page with no linked row — so the window never draws a block
     * it has to clip.
     */
    public boolean bodyFits() {
        final RecipePageLayout layout = bodyLayout();
        return bodyHeight() >= 0
                && layout.size(RecipePageLayout.BLOCK_WIDTH) <= width
                && layout.size(RecipePageLayout.BLOCK_HEIGHT) <= bodyHeight();
    }
}
